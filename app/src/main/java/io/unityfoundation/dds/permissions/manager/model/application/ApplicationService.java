package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.*;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthenticationResponse;
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

import javax.transaction.Transactional;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final GroupRepository groupRepository;
    private final SecurityUtil securityUtil;
    private final GroupUserService groupUserService;
    private final ApplicationPermissionService applicationPermissionService;
    private final PassphraseGenerator passphraseGenerator;
    private final BCryptPasswordEncoderService passwordEncoderService;
    private final ApplicationSecretsClient applicationSecretsClient;

    public ApplicationService(ApplicationRepository applicationRepository, GroupRepository groupRepository,
                              ApplicationPermissionService applicationPermissionService,
                              SecurityUtil securityUtil, GroupUserService groupUserService,
                              PassphraseGenerator passphraseGenerator,
                              BCryptPasswordEncoderService passwordEncoderService, ApplicationSecretsClient applicationSecretsClient) {
        this.applicationRepository = applicationRepository;
        this.groupRepository = groupRepository;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
        this.applicationPermissionService = applicationPermissionService;
        this.passphraseGenerator = passphraseGenerator;
        this.passwordEncoderService = passwordEncoderService;
        this.applicationSecretsClient = applicationSecretsClient;
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

    public HttpResponse<?> getIdentityCACertificate() throws IOException, NoSuchAlgorithmException {
        Optional<String> identityCACert = applicationSecretsClient.getIdentityCACert();
        if (identityCACert.isPresent()) {
            String cert = identityCACert.get();
            String hash = getContentHash(cert);

            File file = File.createTempFile("identity_ca", "pem");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(cert);
            writer.close();

            return HttpResponse.ok(file).header("CONTENT-HASH", hash);
        }
        return HttpResponse.notFound();
    }

    public HttpResponse<?> getPermissionsCACertificate() throws IOException, NoSuchAlgorithmException {
        Optional<String> permissionsCACert = applicationSecretsClient.getPermissionsCACert();
        if (permissionsCACert.isPresent()) {
            String cert = permissionsCACert.get();
            String hash = getContentHash(cert);

            File file = File.createTempFile("permissions_ca", "pem");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(cert);
            writer.close();

            return HttpResponse.ok(file).header("CONTENT-HASH", hash);
        }
        return HttpResponse.notFound();
    }

    public HttpResponse<?> getGovernanceFile() throws IOException, NoSuchAlgorithmException {
        Optional<String> governanceFile = applicationSecretsClient.getGovernanceFile();
        if (governanceFile.isPresent()) {
            String cert = governanceFile.get();
            String hash = getContentHash(cert);

            File file = File.createTempFile("governance_xml_p7s", "txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(cert);
            writer.close();

            return HttpResponse.ok(file).header("CONTENT-HASH", hash);
        }
        return HttpResponse.notFound();
    }

    public HttpResponse<?> getApplicationFileHashes() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Optional<String> identityCACert = applicationSecretsClient.getIdentityCACert();
        Optional<String> permissionsCACert = applicationSecretsClient.getPermissionsCACert();
        Optional<String> governanceFile = applicationSecretsClient.getGovernanceFile();

        if (identityCACert.isPresent() && permissionsCACert.isPresent() && governanceFile.isPresent()){
            Map response = Map.of(
                    "identity_ca.pem", getContentHash(identityCACert.get()),
                    "permissions_ca.pem", getContentHash(permissionsCACert.get()),
                    "governance_xml_p7s.txt", getContentHash(governanceFile.get())
            );

            return HttpResponse.ok(response);
        }
        return HttpResponse.notFound();
    }

    private static String getContentHash(String cert) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytesOfMessage = cert.getBytes("UTF-8");

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(bytesOfMessage);
        return DatatypeConverter.printHexBinary(digest).toUpperCase();
    }
}
