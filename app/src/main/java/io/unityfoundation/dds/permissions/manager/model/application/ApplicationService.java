package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.authentication.AuthenticationException;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.security.BCryptPasswordEncoderService;
import io.unityfoundation.dds.permissions.manager.security.PassphraseGenerator;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
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
    private final PassphraseGenerator passphraseGenerator;
    private final BCryptPasswordEncoderService passwordEncoderService;

    public ApplicationService(ApplicationRepository applicationRepository, GroupRepository groupRepository,
                              SecurityUtil securityUtil, GroupUserService groupUserService, PassphraseGenerator passphraseGenerator,
                              BCryptPasswordEncoderService passwordEncoderService) {
        this.applicationRepository = applicationRepository;
        this.groupRepository = groupRepository;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
        this.passphraseGenerator = passphraseGenerator;
        this.passwordEncoderService = passwordEncoderService;
    }

    public Page<ApplicationDTO> findAll(Pageable pageable, String filter) {
        return getApplicationPage(pageable, filter).map(ApplicationDTO::new);
    }

    private Page<Application> getApplicationPage(Pageable pageable, String filter) {

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
            if (application.getEncryptedPassword() == null && applicationDTO.getPassword() != null) {
                application.setEncryptedPassword(passwordEncoderService.encode(applicationDTO.getPassword()));
            }

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

        // todo place below logic

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
        Optional<Application> application = applicationRepository.findById(id);
        if (application.isEmpty()) {
            throw new Exception("Application not found");
        }
        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(application.get().getPermissionsGroup())) {
            throw new AuthenticationException("Not authorized");
        }

        applicationRepository.deleteById(id);
    }

    public HttpResponse show(Long id) throws Exception {
        Optional<Application> applicationOptional = applicationRepository.findById(id);
        if (applicationOptional.isEmpty()) {
            return HttpResponse.notFound();
        }
        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(applicationOptional.get().getPermissionsGroup())) {
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

    public MutableHttpResponse<Object> generateCleartextPassphrase(Long application) throws Exception {
        Optional<Application> applicationOptional = applicationRepository.findById(application);
        if (applicationOptional.isEmpty()) {
            return HttpResponse.notFound();
        }
        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(applicationOptional.get().getPermissionsGroup())) {
            throw new AuthenticationException("Not authorized");
        }

        return HttpResponse.ok(passphraseGenerator.generatePassphrase());
    }

    // delete
    public MutableHttpResponse<Object> passwordMatches(Long application, String rawPassword) throws Exception {
        Optional<Application> applicationOptional = applicationRepository.findById(application);
        if (applicationOptional.isEmpty()) {
            return HttpResponse.notFound();
        }
        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(applicationOptional.get().getPermissionsGroup())) {
            throw new AuthenticationException("Not authorized");
        }

        if (passwordEncoderService.matches(rawPassword, applicationOptional.get().getEncryptedPassword())) {
            return HttpResponse.ok();
        } else {
            return HttpResponse.notFound();
        }
    }
}
