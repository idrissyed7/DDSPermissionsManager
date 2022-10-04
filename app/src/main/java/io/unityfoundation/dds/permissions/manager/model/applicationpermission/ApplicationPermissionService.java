package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationService;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Optional;

@Singleton
public class ApplicationPermissionService {

    private final ApplicationPermissionRepository applicationPermissionRepository;
    private final ApplicationService applicationService;
    private final TopicService topicService;
    private final SecurityUtil securityUtil;
    private final GroupUserService groupUserService;

    public ApplicationPermissionService(ApplicationPermissionRepository applicationPermissionRepository, ApplicationService applicationService, TopicService topicService, SecurityUtil securityUtil, GroupUserService groupUserService) {
        this.applicationPermissionRepository = applicationPermissionRepository;
        this.applicationService = applicationService;
        this.topicService = topicService;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
    }

    public Page<AccessPermissionDTO> findAll(Long applicationId, Long topicId, Pageable pageable) {
        return getApplicationPermissionsPage(applicationId, topicId, pageable).map(applicationPermission -> new AccessPermissionDTO(
                applicationPermission.getId(), applicationPermission.getPermissionsTopic().getId(),
                applicationPermission.getPermissionsApplication().getId(),
                applicationPermission.getAccessType()
        ));
    }

    private Page<ApplicationPermission> getApplicationPermissionsPage(Long applicationId, Long topicId, Pageable pageable) {
        if (applicationId == null && topicId == null) {
            return applicationPermissionRepository.findAll(pageable);
        } else if (applicationId != null && topicId == null) {
            return applicationPermissionRepository.findByPermissionsApplicationId(applicationId, pageable);
        } else if (topicId != null && applicationId == null) {
            return applicationPermissionRepository.findByPermissionsTopicId(topicId, pageable);
        } else {
            return applicationPermissionRepository.findByPermissionsApplicationIdAndPermissionsTopicId(
                    applicationId, topicId, pageable);
        }
    }

    public HttpResponse<AccessPermissionDTO> addAccess(Long applicationId, Long topicId, AccessType access) {
        final HttpResponse response;

        Optional<Application> applicationById = applicationService.findById(applicationId);
        if (applicationById.isEmpty()) {
            response = HttpResponse.notFound();
        } else {
            Optional<Topic> topicById = topicService.findById(topicId);
            if (topicById.isEmpty()) {
                response = HttpResponse.notFound();
            } else {
                Topic topic = topicById.get();

                User user = securityUtil.getCurrentlyAuthenticatedUser().get();
                if (!securityUtil.isCurrentUserAdmin() &&
                        !groupUserService.isUserTopicAdminOfGroup(topic.getPermissionsGroup().getId(), user.getId())) {
                    response = HttpResponse.unauthorized();
                } else {
                    Application application = applicationById.get();
                    response = addAccess(application, topic, access);
                }
            }
        }

        return response;
    }

    public HttpResponse addAccess(Application application, Topic topic, AccessType access) {
        ApplicationPermission newPermission = saveNewPermission(application, topic, access);
        AccessPermissionDTO dto = createDTO(newPermission);
        return HttpResponse.created(dto);
    }

    public ApplicationPermission saveNewPermission(Application application, Topic topic, AccessType access) {
        ApplicationPermission applicationPermission = new ApplicationPermission(application, topic, access);
        return applicationPermissionRepository.save(applicationPermission);
    }

    public AccessPermissionDTO createDTO(ApplicationPermission applicationPermission) {
        Long topicId = applicationPermission.getPermissionsTopic().getId();
        Long applicationId = applicationPermission.getPermissionsApplication().getId();
        AccessType accessType = applicationPermission.getAccessType();
        return new AccessPermissionDTO(applicationPermission.getId(), topicId, applicationId, accessType);
    }

    public HttpResponse deleteById(Long permissionId) {

        Optional<ApplicationPermission> applicationPermissionOptional = applicationPermissionRepository.findById(permissionId);

        if (applicationPermissionOptional.isEmpty()) {
            return HttpResponse.notFound();
        } else {
            Topic topic = applicationPermissionOptional.get().getPermissionsTopic();
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();

            if (!securityUtil.isCurrentUserAdmin() && !groupUserService.isUserTopicAdminOfGroup(topic.getPermissionsGroup().getId(), user.getId())) {
                return HttpResponse.unauthorized();
            }
        }

        applicationPermissionRepository.deleteById(permissionId);
        return HttpResponse.noContent();
    }

    public HttpResponse<AccessPermissionDTO> updateAccess(Long permissionId, AccessType access) {
        if (Arrays.stream(AccessType.values()).noneMatch(accessType -> accessType.equals(access))) {
            return HttpResponse.badRequest();
        }

        Optional<ApplicationPermission> applicationPermissionOptional = applicationPermissionRepository.findById(permissionId);

        if (applicationPermissionOptional.isEmpty()) {
            return HttpResponse.notFound();
        } else {
            Topic topic = applicationPermissionOptional.get().getPermissionsTopic();
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();

            if (!securityUtil.isCurrentUserAdmin() && !groupUserService.isUserTopicAdminOfGroup(topic.getPermissionsGroup().getId(), user.getId())) {
                return HttpResponse.unauthorized();
            }
        }

        ApplicationPermission applicationPermission = applicationPermissionOptional.get();
        applicationPermission.setAccessType(access);

        return HttpResponse.ok(createDTO(applicationPermissionRepository.update(applicationPermission)));
    }

    // approach1
    @Transactional
    public void deleteAllByTopic(Topic topic) {
        applicationPermissionRepository.deleteByPermissionsTopicEquals(topic);
    }
}
