package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.*;
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
import java.net.URI;
import java.util.List;
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
    public MutableHttpResponse<?> save(ApplicationDTO applicationDTO) {
        Optional<Group> groupOptional = groupRepository.findById(applicationDTO.getGroup());

        if (groupOptional.isEmpty()) {
            return HttpResponse.notFound("Specified group does not exist");
        }

        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(groupOptional.get())) {
            return HttpResponse.unauthorized();
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
                return HttpResponse.badRequest("Cannot change application's group association");
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

    public boolean isUserApplicationAdminOfGroup(Group group) {
        return securityUtil.getCurrentlyAuthenticatedUser()
                .map(user -> groupUserService.isUserApplicationAdminOfGroup(group.getId(), user.getId()))
                .orElse(false);
    }

    public HttpResponse deleteById(Long id) {

        Optional<Application> applicationOptional = applicationRepository.findById(id);
        if (applicationOptional.isEmpty()) {
            return HttpResponse.notFound("Application not found");
        }

        Application application = applicationOptional.get();

        Optional<Group> groupOptional = groupRepository.findById(application.getPermissionsGroup().getId());
        if (groupOptional.isEmpty()) {
            return HttpResponse.notFound("Group does not exist");
        }

        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(application.getPermissionsGroup())) {
            return HttpResponse.unauthorized();
        }

        // TODO - Need to investigate cascade management to eliminate this
        applicationPermissionService.deleteAllByApplication(application);

        applicationRepository.deleteById(id);
        return HttpResponse.seeOther(URI.create("/api/applications"));
    }

    public HttpResponse show(Long id) {
        Optional<Application> applicationOptional = applicationRepository.findById(id);
        if (applicationOptional.isEmpty()) {
            return HttpResponse.notFound();
        }

        if (!securityUtil.isCurrentUserAdmin() &&
                !groupUserService.isUserMemberOfGroup(
                        applicationOptional.get().getPermissionsGroup().getId(),
                        securityUtil.getCurrentlyAuthenticatedUser().get().getId())
        ){
            return HttpResponse.unauthorized();
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

    public MutableHttpResponse<Object> generateCleartextPassphrase(Long applicationId) {
        Optional<Application> applicationOptional = applicationRepository.findById(applicationId);
        if (applicationOptional.isEmpty()) {
            return HttpResponse.notFound("Application not found");
        }

        Application application = applicationOptional.get();

        Optional<Group> groupOptional = groupRepository.findById(application.getPermissionsGroup().getId());
        if (groupOptional.isEmpty()) {
            return HttpResponse.notFound("Group does not exist");
        }

        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(application.getPermissionsGroup())) {
            return HttpResponse.unauthorized();
        }

        String clearTextPassphrase = passphraseGenerator.generatePassphrase();

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
}
