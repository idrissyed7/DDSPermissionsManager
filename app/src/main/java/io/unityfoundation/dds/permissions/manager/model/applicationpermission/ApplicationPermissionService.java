package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Singleton;

@Singleton
public class ApplicationPermissionService {

    private final ApplicationPermissionRepository applicationPermissionRepository;


    public ApplicationPermissionService(ApplicationPermissionRepository applicationPermissionRepository) {
        this.applicationPermissionRepository = applicationPermissionRepository;
    }

    public Page<AccessPermissionDTO> findAll(Long applicationId, Long topicId, Pageable pageable) {
        return getApplicationPermissionsPage(applicationId, topicId, pageable).map(applicationPermission -> new AccessPermissionDTO(
                applicationPermission.getPermissionsTopic().getId(),
                applicationPermission.getPermissionsApplication().getId(),
                applicationPermission.getAccessType()
        ));
    }

    private Page<ApplicationPermission> getApplicationPermissionsPage(Long applicationId, Long topicId, Pageable pageable) {
        if (applicationId == null && topicId == null) {
            return applicationPermissionRepository.findAll(pageable);
        } else if (applicationId != null && topicId == null)  {
            return applicationPermissionRepository.findByPermissionsApplicationId(applicationId, pageable);
        } else if (topicId != null && applicationId == null)  {
            return applicationPermissionRepository.findByPermissionsTopicId(topicId, pageable);
        } else {
            return applicationPermissionRepository.findByPermissionsApplicationIdAndPermissionsTopicId(
                    applicationId, topicId, pageable);
        }
    }
}
