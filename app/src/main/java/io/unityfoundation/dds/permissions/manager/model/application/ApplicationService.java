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
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final GroupRepository groupRepository;
    private final SecurityUtil securityUtil;
    private final GroupUserService groupUserService;

    public ApplicationService(ApplicationRepository applicationRepository, GroupRepository groupRepository, SecurityUtil securityUtil, GroupUserService groupUserService) {
        this.applicationRepository = applicationRepository;
        this.groupRepository = groupRepository;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
    }

    public Page<ApplicationDTO> findAll(Pageable pageable) {
        return applicationRepository.findAll(pageable).map(ApplicationDTO::new);
    }

    @Transactional
    public MutableHttpResponse<?> save(ApplicationDTO applicationDTO) throws Exception {

        if (applicationDTO.getGroup() == null) {
            throw new Exception("Application must be associated to a group.");
        }

        Optional<Group> groupOptional = groupRepository.findById(applicationDTO.getGroup());

        if (groupOptional.isEmpty()) {
            return HttpResponse.notFound("Specified group does not exist");
        }

        Application application = new Application(applicationDTO.getName());
        application.setId(applicationDTO.getId());
        Group group = groupOptional.get();
        application.setPermissionsGroup(group);

        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(application)) {
            throw new AuthenticationException("Not authorized");
        }

        Optional<Application> searchApplicationByNameAndGroup = applicationRepository.findByNameAndPermissionsGroup(
                application.getName(), application.getPermissionsGroup());

        if (searchApplicationByNameAndGroup.isPresent()) {
            return HttpResponseFactory.INSTANCE.status(HttpStatus.SEE_OTHER, new ApplicationDTO(searchApplicationByNameAndGroup.get()));
        }

        if (application.getId() == null) {
            return HttpResponse.ok(new ApplicationDTO(applicationRepository.save(application)));
        } else {
            return HttpResponse.ok(new ApplicationDTO(applicationRepository.update(application)));
        }
    }

    public boolean isUserApplicationAdminOfGroup(Application application) throws Exception {
        Group applicationGroupId = application.getPermissionsGroup();
        if (applicationGroupId == null) {
            throw new Exception("Cannot save Application without specifying a Group.");
        }
        if (groupRepository.findById(applicationGroupId.getId()).isEmpty()) {
            throw new Exception("Specified group does not exist.");
        }

        return securityUtil.getCurrentlyAuthenticatedUser()
                .map(user -> groupUserService.isUserApplicationAdminOfGroup(applicationGroupId.getId(), user.getId()))
                .orElse(false);
    }

    public void deleteById(Long id) throws Exception {
        Optional<Application> application = applicationRepository.findById(id);
        if (application.isEmpty()) {
            throw new Exception("Application not found");
        }
        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(application.get())) {
            throw new AuthenticationException("Not authorized");
        }

        applicationRepository.deleteById(id);
    }

    public HttpResponse show(Long id) {
        Optional<Application> applicationOptional = applicationRepository.findById(id);
        if (applicationOptional.isEmpty()) {
            return HttpResponse.notFound();
        }
        Application application = applicationOptional.get();
        ApplicationDTO applicationDTO = new ApplicationDTO(application);

        return HttpResponse.ok(applicationDTO);
    }

    public List<ApplicationDTO> search(String filter, int max) {
        Iterable<Application> results = applicationRepository.searchByApplicationNameAndGroupName(filter);
        return toDtos(results);
    }

    public List<ApplicationDTO> toDtos(Iterable<Application> results) {
        return StreamSupport.stream(results.spliterator(), false)
                .map(ApplicationDTO::new)
                .collect(Collectors.toList());
    }
}
