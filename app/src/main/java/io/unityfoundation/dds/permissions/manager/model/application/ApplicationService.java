package io.unityfoundation.dds.permissions.manager.model.application;

import freemarker.template.TemplateException;
import io.micronaut.context.annotation.Property;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.AccessType;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermission;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionService;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRole;
import io.unityfoundation.dds.permissions.manager.security.ApplicationSecretsClient;
import io.unityfoundation.dds.permissions.manager.security.BCryptPasswordEncoderService;
import io.unityfoundation.dds.permissions.manager.security.PassphraseGenerator;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.Store;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.security.auth.x500.X500Principal;
import javax.transaction.Transactional;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class ApplicationService {

    @Property(name = "permissions-manager.application.client-certificate.time-expiry")
    protected Optional<Long> certExpiry;
    @Property(name = "permissions-manager.application.permissions-file.time-expiry")
    protected Optional<Long> permissionExpiry;
    @Property(name = "permissions-manager.application.permissions-file.domain")
    protected Optional<Long> permissionDomain;
    private final ApplicationRepository applicationRepository;
    private final GroupRepository groupRepository;
    private final SecurityUtil securityUtil;
    private final GroupUserService groupUserService;
    private final ApplicationPermissionService applicationPermissionService;
    private final PassphraseGenerator passphraseGenerator;
    private final BCryptPasswordEncoderService passwordEncoderService;
    private final ApplicationSecretsClient applicationSecretsClient;
    private final TemplateService templateService;

    public ApplicationService(ApplicationRepository applicationRepository, GroupRepository groupRepository,
                              ApplicationPermissionService applicationPermissionService,
                              SecurityUtil securityUtil, GroupUserService groupUserService,
                              PassphraseGenerator passphraseGenerator,
                              BCryptPasswordEncoderService passwordEncoderService, ApplicationSecretsClient applicationSecretsClient, TemplateService templateService) {
        this.applicationRepository = applicationRepository;
        this.groupRepository = groupRepository;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
        this.applicationPermissionService = applicationPermissionService;
        this.passphraseGenerator = passphraseGenerator;
        this.passwordEncoderService = passwordEncoderService;
        this.applicationSecretsClient = applicationSecretsClient;
        this.templateService = templateService;
    }

    public Page<ApplicationDTO> findAll(Pageable pageable, String filter) {
        return getApplicationPage(pageable, filter).map(ApplicationDTO::new);
    }

    private Page<Application> getApplicationPage(Pageable pageable, String filter) {

        if(!pageable.isSorted()) {
            pageable = pageable.order("name").order("permissionsGroup.name");
        }

        if (securityUtil.isCurrentUserAdmin()) {
            if (filter == null) {
                return applicationRepository.findAll(pageable);
            }

            return applicationRepository.findByNameContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(filter, filter, pageable);
        } else {
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            List<Long> groups = groupUserService.getAllGroupsUserIsAMemberOf(user.getId());

            if (filter == null) {
                return applicationRepository.findAllByPermissionsGroupIdIn(groups, pageable);
            }
            List<Long> all = applicationRepository.findIdByNameContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(filter, filter);

            return applicationRepository.findAllByIdInAndPermissionsGroupIdIn(all, groups, pageable);
        }
    }

    @Transactional
    public MutableHttpResponse<?> save(ApplicationDTO applicationDTO) throws Exception {

        if (applicationDTO.getGroup() == null) {
            throw new Exception("Application must be associated to a group.");
        } else if (applicationDTO.getName() == null) {
            throw new Exception("Name cannot be empty.");
        }

        Optional<Group> groupOptional = groupRepository.findById(applicationDTO.getGroup());

        if (groupOptional.isEmpty()) {
            return HttpResponse.notFound("Specified group does not exist");
        }

        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(groupOptional.get())) {
            throw new AuthenticationException("Not authorized");
        }

        // check if application with same name exists in group
        Optional<Application> searchApplicationByNameAndGroup = applicationRepository.findByNameAndPermissionsGroup(
                applicationDTO.getName().trim(), groupOptional.get());

        Application application;
        if (applicationDTO.getId() != null) { // update

            Optional<Application> applicationOptional = applicationRepository.findById(applicationDTO.getId());
            if (applicationOptional.isEmpty()) {
                return HttpResponse.notFound("Specified application does not exist");
            } else if (!Objects.equals(applicationOptional.get().getPermissionsGroup().getId(), applicationDTO.getGroup())) {
                throw new Exception("Cannot change application's group association");
            } else if (searchApplicationByNameAndGroup.isPresent() &&
                    searchApplicationByNameAndGroup.get().getId() != applicationOptional.get().getId()) {
                // attempt to update an existing application with same name/group combo as another
                return HttpResponseFactory.INSTANCE.status(HttpStatus.SEE_OTHER, new ApplicationDTO(searchApplicationByNameAndGroup.get()));
            }

            application = applicationOptional.get();
            application.setName(applicationDTO.getName());

            return HttpResponse.ok(new ApplicationDTO(applicationRepository.update(application)));
        } else { // new

            if (searchApplicationByNameAndGroup.isPresent()) {
                return HttpResponseFactory.INSTANCE.status(HttpStatus.SEE_OTHER, new ApplicationDTO(searchApplicationByNameAndGroup.get()));
            }

            application = new Application(applicationDTO.getName());
            application.setId(applicationDTO.getId());
            Group group = groupOptional.get();
            application.setPermissionsGroup(group);
            return HttpResponse.ok(new ApplicationDTO(applicationRepository.save(application)));
        }
    }

    public boolean isUserApplicationAdminOfGroup(Group group) throws Exception {
        if (group == null) {
            throw new Exception("Cannot save Application without specifying a Group.");
        }
        if (groupRepository.findById(group.getId()).isEmpty()) {
            throw new Exception("Specified group does not exist.");
        }

        return securityUtil.getCurrentlyAuthenticatedUser()
                .map(user -> groupUserService.isUserApplicationAdminOfGroup(group.getId(), user.getId()))
                .orElse(false);
    }

    public void deleteById(Long id) throws Exception {
        Optional<Application> applicationOptional = applicationRepository.findById(id);
        if (applicationOptional.isEmpty()) {
            throw new Exception("Application not found");
        }
        Application application = applicationOptional.get();
        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(application.getPermissionsGroup())) {
            throw new AuthenticationException("Not authorized");
        }

        // TODO - Need to investigate cascade management to eliminate this
        applicationPermissionService.deleteAllByApplication(application);

        applicationRepository.deleteById(id);
    }

    public HttpResponse show(Long id) throws Exception {
        Optional<Application> applicationOptional = applicationRepository.findById(id);
        if (applicationOptional.isEmpty()) {
            return HttpResponse.notFound();
        }

        if (!securityUtil.isCurrentUserAdmin() &&
                !groupUserService.isUserMemberOfGroup(
                        applicationOptional.get().getPermissionsGroup().getId(),
                        securityUtil.getCurrentlyAuthenticatedUser().get().getId())
        ){
            throw new AuthenticationException("Not authorized");
        }
        Application application = applicationOptional.get();
        ApplicationDTO applicationDTO = new ApplicationDTO(application);

        return HttpResponse.ok(applicationDTO);
    }

    public List<ApplicationDTO> search(String filter, Pageable page) {
        Page<Application> results = applicationRepository.findByNameContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(filter, filter, page);
        return toDtos(results);
    }

    public List<ApplicationDTO> toDtos(Iterable<Application> results) {
        return StreamSupport.stream(results.spliterator(), false)
                .map(ApplicationDTO::new)
                .collect(Collectors.toList());
    }
    
    public Optional<Application> findById(Long applicationId) {
        return applicationRepository.findById(applicationId);
    }

    public MutableHttpResponse<Object> generateCleartextPassphrase(Long applicationId) throws Exception {
        Optional<Application> applicationOptional = applicationRepository.findById(applicationId);
        if (applicationOptional.isEmpty()) {
            return HttpResponse.notFound();
        }
        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(applicationOptional.get().getPermissionsGroup())) {
            throw new AuthenticationException("Not authorized");
        }

        String clearTextPassphrase = passphraseGenerator.generatePassphrase();

        Application application = applicationOptional.get();
        application.setEncryptedPassword(passwordEncoderService.encode(clearTextPassphrase));
        applicationRepository.update(application);

        return HttpResponse.ok(clearTextPassphrase);
    }

    public AuthenticationResponse passwordMatches(Long applicationId, String rawPassword) {
        Optional<Application> applicationOptional = applicationRepository.findById(applicationId);
        if (applicationOptional.isEmpty()) {
            return AuthenticationResponse.failure("Application not found.");
        }
        Application application = applicationOptional.get();

        if (passwordEncoderService.matches(rawPassword, application.getEncryptedPassword())) {
            return AuthenticationResponse.success(application.getId().toString(), List.of(UserRole.APPLICATION.toString()));
        } else {
            return AuthenticationResponse.failure("Invalid passphrase.");
        }
    }

    public HttpResponse<?> getIdentityCACertificate() {
        Optional<String> identityCACert = applicationSecretsClient.getIdentityCACert();
        if (identityCACert.isPresent()) {
            String cert = identityCACert.get();

            return HttpResponse.ok(cert);
        }
        return HttpResponse.notFound();
    }

    public HttpResponse<?> getPermissionsCACertificate() {
        Optional<String> permissionsCACert = applicationSecretsClient.getPermissionsCACert();
        if (permissionsCACert.isPresent()) {
            String cert = permissionsCACert.get();

            return HttpResponse.ok(cert);
        }
        return HttpResponse.notFound();
    }

    public HttpResponse<?> getGovernanceFile() {
        Optional<String> governanceFile = applicationSecretsClient.getGovernanceFile();
        if (governanceFile.isPresent()) {
            String cert = governanceFile.get();

            return HttpResponse.ok(cert);
        }
        return HttpResponse.notFound();
    }

    public HttpResponse<?> getApplicationPrivateKeyAndClientCertificate(String nonce) throws IOException, OperatorCreationException, GeneralSecurityException {

        Optional<String> identityCACert = applicationSecretsClient.getIdentityCACert();
        Optional<String> identityCAKey = applicationSecretsClient.getIdentityCAKey();
        Optional<Application> applicationOptional = securityUtil.getCurrentlyAuthenticatedApplication();

        if (applicationOptional.isPresent() && identityCACert.isPresent() && identityCAKey.isPresent()) {
            Application application = applicationOptional.get();

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(256);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            X509Certificate x509Certificate = makeV3Certificate(
                    readCertificate(identityCACert.get()),
                    readPrivateKey(identityCAKey.get().trim()),
                    keyPair.getPublic(),
                    application,
                    nonce
            );

            Map creds = Map.of(
                    "private", objectToPEMString(keyPair.getPrivate()),
                    "public", objectToPEMString(x509Certificate)
            );
            return HttpResponse.ok(creds);
        }

        return HttpResponse.notFound();
    }

    public HttpResponse<?> getPermissionsFile(String nonce) throws IOException, GeneralSecurityException, MessagingException, SMIMEException, OperatorCreationException, TemplateException {
        Optional<String> permissionsCAKey = applicationSecretsClient.getPermissionsCAKey();
        Optional<String> permissionsCACert = applicationSecretsClient.getPermissionsCACert();
        Optional<Application> applicationOptional = securityUtil.getCurrentlyAuthenticatedApplication();

        if (applicationOptional.isPresent() && permissionsCAKey.isPresent() && permissionsCACert.isPresent()) {
            //openssl smime -sign -in permissions.ftlx -text -out permissions.ftlx.p7s -signer permissions_ca.pem -inkey permissions_ca_key.pem
            String permissionsXml = generatePermissionsXml(applicationOptional.get(), nonce);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setText(permissionsXml);
            MimeMultipart signedMultipart = createSignedMultipart(
                    readPrivateKey(permissionsCAKey.get()),
                    readCertificate(permissionsCACert.get()),
                    mimeBodyPart);

            String result;
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                signedMultipart.writeTo(byteArrayOutputStream);
                result = byteArrayOutputStream.toString();
            }

            return HttpResponse.ok(result);
        }

        return HttpResponse.notFound();
    }

    public static MimeMultipart createSignedMultipart(
            PrivateKey signingKey, X509Certificate signingCert, MimeBodyPart message)
            throws GeneralSecurityException, OperatorCreationException, SMIMEException {
        List<X509Certificate> certList = new ArrayList<X509Certificate>();
        certList.add(signingCert);
        Store certs = new JcaCertStore(certList);
        ASN1EncodableVector signedAttrs = generateSignedAttributes();
        signedAttrs.add(new Attribute(CMSAttributes.signingTime, new DERSet(new Time(new Date()))));
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder()
                .setSignedAttributeGenerator(new AttributeTable(signedAttrs))
                .build("SHA1WITHECDSA", signingKey, signingCert));
        gen.addCertificates(certs);
        return gen.generate(message);
    }

    private static ASN1EncodableVector generateSignedAttributes() {
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        caps.addCapability(SMIMECapability.aES128_CBC);
        caps.addCapability(SMIMECapability.aES192_CBC);
        caps.addCapability(SMIMECapability.aES256_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        return signedAttrs;
    }

    private String generatePermissionsXml(Application application, String nonce) throws TemplateException, IOException {
        Map<String, Object> dataModel = buildTemplateDataModel(nonce, application);

        return templateService.mergeDataAndTemplate(dataModel);
    }

    private Map<String, Object> buildTemplateDataModel(String nonce, Application application) {
        HashMap<String, Object> dataModel = new HashMap<>();
        dataModel.put("subject", buildSubject(application, nonce));
        dataModel.put("applicationId", application.getId());

        Long expiry = permissionExpiry.isPresent() ? permissionExpiry.get(): 30;
        String validStart = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String validEnd = ZonedDateTime.now(ZoneOffset.UTC).plusDays(expiry).format(DateTimeFormatter.ISO_INSTANT);
        dataModel.put("validStart", validStart);
        dataModel.put("validEnd", validEnd);

        Long domain = permissionDomain.isPresent() ? permissionDomain.get(): 1;
        dataModel.put("domain", domain);

        List<ApplicationPermission> applicationPermissions = applicationPermissionService.findAllByApplication(application);
        Map<AccessType, List<ApplicationPermission>> accessTypeListMap =
                applicationPermissions.stream().collect(Collectors.groupingBy(ApplicationPermission::getAccessType));

        // list of canonical names for each publish-subscribe sections
        List<String> subscribeList = new ArrayList<>();
        List<String> publishList = new ArrayList<>();

        // read
        addCanonicalNamesToList(subscribeList, accessTypeListMap.get(AccessType.READ));

        // write
        addCanonicalNamesToList(publishList, accessTypeListMap.get(AccessType.WRITE));

        // read+write
        addCanonicalNamesToList(subscribeList, accessTypeListMap.get(AccessType.READ_WRITE));
        addCanonicalNamesToList(publishList, accessTypeListMap.get(AccessType.READ_WRITE));

        dataModel.put("subscribeList", subscribeList);
        dataModel.put("publishList", publishList);

        return dataModel;
    }

    private void addCanonicalNamesToList(List<String> list, List<ApplicationPermission> applicationPermissions) {
        if (applicationPermissions != null) {
            list.addAll(applicationPermissions.stream()
                    .map(applicationPermission -> buildCanonicalName(
                            applicationPermission.getPermissionsApplication(),
                            applicationPermission))
                    .collect(Collectors.toList()));
        }
    }

    private static String buildCanonicalName(Application application, ApplicationPermission applicationPermission) {
        return applicationPermission.getPermissionsTopic().getKind() + "." +
                application.getPermissionsGroup().getId() + "." +
                applicationPermission.getPermissionsTopic().getName();
    }

    public X509Certificate makeV3Certificate(
            X509Certificate caCertificate, PrivateKey caPrivateKey, PublicKey eePublicKey, Application application, String nonce)
            throws GeneralSecurityException, CertIOException, OperatorCreationException {

        Long expiry = certExpiry.isPresent() ? certExpiry.get(): 365;

        X509v3CertificateBuilder v3CertBldr = new JcaX509v3CertificateBuilder(
                caCertificate.getSubjectX500Principal(), // issuer
                BigInteger.valueOf(System.currentTimeMillis()) // serial number
                        .multiply(BigInteger.valueOf(10)),
                new Date(System.currentTimeMillis() - 1000L * 5), // start time
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(expiry)), // expiry time
                new X500Principal(buildSubject(application, nonce)), // subject
                eePublicKey); // subject public key

        // extensions
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        v3CertBldr.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(eePublicKey));
        v3CertBldr.addExtension(
                Extension.authorityKeyIdentifier,
                false,
                extUtils.createAuthorityKeyIdentifier(caCertificate));
        v3CertBldr.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false));
        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WITHECDSA");

        return new JcaX509CertificateConverter().getCertificate(v3CertBldr.build(signerBuilder.build(caPrivateKey)));
    }

    private static String buildSubject(Application application, String nonce) {
        X500NameBuilder nameBuilder = new X500NameBuilder();
        nameBuilder.addRDN(BCStyle.CN, application.getId() + "_" + nonce);
        nameBuilder.addRDN(BCStyle.GIVENNAME, String.valueOf(application.getId()));
        nameBuilder.addRDN(BCStyle.SURNAME, String.valueOf(application.getPermissionsGroup().getId()));
        return nameBuilder.build().toString();
    }

    public static String objectToPEMString(Object certificate) throws IOException {
        StringWriter sWrt = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt);
        pemWriter.writeObject(certificate);
        pemWriter.close();
        return sWrt.toString();
    }

    public static X509Certificate readCertificate(String pemEncoding) throws IOException, CertificateException {
        PEMParser parser = new PEMParser(new StringReader(pemEncoding));
        X509CertificateHolder certHolder = (X509CertificateHolder) parser.readObject();
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    public static PrivateKey readPrivateKey(String pemEncoding) throws IOException {
        PEMParser parser = new PEMParser(new StringReader(pemEncoding));
        PEMKeyPair pemKeyPair = (PEMKeyPair) parser.readObject();
        return new JcaPEMKeyConverter().getPrivateKey(pemKeyPair.getPrivateKeyInfo());
    }

    private static String getContentHash(String cert) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytesOfMessage = cert.getBytes("UTF-8");

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(bytesOfMessage);
        return DatatypeConverter.printHexBinary(digest).toUpperCase();
    }
}
