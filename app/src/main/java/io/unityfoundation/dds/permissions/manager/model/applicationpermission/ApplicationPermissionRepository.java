package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;

import java.util.Collection;

@Repository
public interface ApplicationPermissionRepository extends PageableRepository<ApplicationPermission, Long> {
    Page<ApplicationPermission> findByPermissionsApplicationId(Long applicationId, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsTopicId(Long topicId, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationIdAndPermissionsTopicId(Long applicationId, Long topicId, Pageable pageable);
    void deleteByPermissionsTopicEquals(Topic permissionsTopic);
    void deleteByPermissionsApplicationEquals(Application permissionsApplication);
    void deleteByPermissionsApplicationIdIn(Collection<Long> permissionsApplications);
    void deleteByPermissionsTopicIdIn(Collection<Long> permissionsTopics);
}
