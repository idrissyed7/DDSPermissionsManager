package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.authentication.AuthenticationException;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.Optional;

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

    public Page<Application> findAll(Pageable pageable) {
        return applicationRepository.findAll(pageable);
    }

    @Transactional
    public MutableHttpResponse<Application> save(Application application) throws Exception {

        if (!securityUtil.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(application)) {
            throw new AuthenticationException("Not authorized");
        }

        Optional<Application> searchApplicationByNameAndGroup = applicationRepository.findByNameAndPermissionsGroup(
                application.getName(), application.getPermissionsGroup());

        if (searchApplicationByNameAndGroup.isPresent()) {
            return HttpResponseFactory.INSTANCE.status(HttpStatus.SEE_OTHER, searchApplicationByNameAndGroup.get());
        }

        if (application.getId() == null) {
            return HttpResponse.ok(applicationRepository.save(application));
        } else {
            return HttpResponse.ok(applicationRepository.update(application));
        }
    }

    public boolean isUserApplicationAdminOfGroup(Application application) throws Exception {
        Long applicationGroupId = application.getPermissionsGroup();
        if (applicationGroupId == null) {
            throw new Exception("Cannot save Application without specifying a Group.");
        } else {
            if (groupRepository.findById(applicationGroupId).isEmpty()) {
                throw new Exception("Specified group does not exist.");
            }

            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            return groupUserService.isUserApplicationAdminOfGroup(applicationGroupId, user.getId());
        }
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
}
