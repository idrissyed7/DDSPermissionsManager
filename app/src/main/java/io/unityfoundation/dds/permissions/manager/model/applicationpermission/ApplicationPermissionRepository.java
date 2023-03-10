package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApplicationPermissionRepository extends PageableRepository<ApplicationPermission, Long> {
    boolean existsByPermissionsApplicationAndPermissionsTopic(Application permissionsApplication, Topic permissionsTopic);
    Page<ApplicationPermission> findByPermissionsApplicationId(Long applicationId, Pageable pageable);
    List<ApplicationPermission> findByPermissionsApplication(Application permissionsApplication);
    Page<ApplicationPermission> findByPermissionsTopicId(Long topicId, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationIdAndPermissionsTopicId(Long applicationId, Long topicId, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationIdInOrPermissionsTopicIdIn(Collection<Long> permissionsTopic_id, Collection<Long> permissionsApplication_id, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationIdAndPermissionsApplicationIdIn(Long applicationId, List<Long> groupsApplications, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsTopicIdAndPermissionsTopicIdIn(Long topicId, List<Long> groupsTopics, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationIdAndPermissionsTopicIdAndPermissionsApplicationIdInAndPermissionsTopicIdIn(Long applicationId, Long topicId, List<Long> groupsApplications, List<Long> groupsTopics, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationMakePublicTrueAndPermissionsTopicMakePublicTrue(Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationMakePublicTrueAndPermissionsTopicMakePublicTrueAndPermissionsApplicationId(Long applicationId, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationMakePublicTrueAndPermissionsTopicMakePublicTrueAndPermissionsTopicId(Long topicId, Pageable pageable);
    Page<ApplicationPermission> findByPermissionsApplicationMakePublicTrueAndPermissionsTopicMakePublicTrueAndPermissionsApplicationIdAndPermissionsTopicId(Long applicationId, Long topicId, Pageable pageable);
    void deleteByPermissionsTopicEquals(Topic permissionsTopic);
    void deleteByPermissionsApplicationEquals(Application permissionsApplication);
    void deleteByPermissionsApplicationIdIn(Collection<Long> permissionsApplications);
    void deleteByPermissionsTopicIdIn(Collection<Long> permissionsTopics);
}
