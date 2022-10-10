package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;

import java.util.Optional;

@Repository
public interface ApplicationPermissionRepository extends PageableRepository<ApplicationPermission, Long> {
    Page<ApplicationPermission> findByPermissionsApplicationId(Long applicationId, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsTopicId(Long topicId, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationIdAndPermissionsTopicId(Long applicationId, Long topicId, Pageable pageable);
    Optional<ApplicationPermission> findByPermissionsApplicationIdAndPermissionsTopicIdAndAccessType(Long applicationId, Long topicId, AccessType accessType);
}
