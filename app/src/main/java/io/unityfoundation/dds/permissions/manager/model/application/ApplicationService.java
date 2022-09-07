package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.utils.SecurityService;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final GroupRepository groupRepository;
    private final UserService userService;
    private final GroupUserService groupUserService;
    private final SecurityService securityService;

    public ApplicationService(ApplicationRepository applicationRepository, GroupRepository groupRepository, UserService userService, GroupUserService groupUserService, SecurityService securityService) {
        this.applicationRepository = applicationRepository;
        this.groupRepository = groupRepository;
        this.userService = userService;
        this.groupUserService = groupUserService;
        this.securityService = securityService;
    }

    public Page<Application> findAll(Pageable pageable) {
        return applicationRepository.findAll(pageable);
    }

    @Transactional
    public MutableHttpResponse<Application> save(Application application) throws Exception {

        if (!userService.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(application)) {
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

            Authentication authentication = securityService.getAuthentication().get();
            String userEmail = authentication.getName();
            User user = userService.getUserByEmail(userEmail).get();
            return groupUserService.isUserApplicationAdminOfGroup(applicationGroupId, user.getId());
        }
    }

    public void deleteById(Long id) throws Exception {
        Optional<Application> application = applicationRepository.findById(id);
        if (application.isEmpty()) {
            throw new Exception("Application not found");
        }
        if (!userService.isCurrentUserAdmin() && !isUserApplicationAdminOfGroup(application.get())) {
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
        Group group = groupRepository.findById(application.getPermissionsGroup()).get();

        Map map = Map.of(
                "application_id", application.getId(),
                "application_name", application.getName(),
                "group_name", group.getName()
                );

        return HttpResponse.ok(map);
    }
}
