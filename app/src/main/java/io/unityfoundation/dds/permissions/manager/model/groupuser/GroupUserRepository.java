package io.unityfoundation.dds.permissions.manager.model.groupuser;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupUserRepository extends PageableRepository<GroupUser, Long> {

    @Query(value = "select count(*) from permissions_group_user " +
            "where permissions_group_id = :groupId and permissions_user_id = :userId and is_group_admin = true",
            countQuery = "select DISTINCT count(*) from permissions_group_user " +
                    "where permissions_group_id = :groupId and permissions_user_id = :userId and is_group_admin = true",
            nativeQuery = true )
    int countByPermissionsGroupIdAndPermissionsUserIdAndGroupAdminTrue(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    @Query(value = "select count(*) from permissions_group_user " +
            "where permissions_group_id = :groupId and permissions_user_id = :userId and is_topic_admin = true",
            countQuery = "select DISTINCT count(*) from permissions_group_user " +
                    "where permissions_group_id = :groupId and permissions_user_id = :userId and is_topic_admin = true",
            nativeQuery = true )
    int countByPermissionsGroupIdAndPermissionsUserIdAndTopicAdminTrue(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    @Query(value = "select COUNT(*) from permissions_group_user " +
            "where permissions_group_id = :groupId and permissions_user_id = :userId and is_application_admin = true",
            countQuery = "select DISTINCT count(*) from permissions_group_user " +
                    "where permissions_group_id = :groupId and permissions_user_id = :userId and is_application_admin = true",
            nativeQuery = true )
    int countByPermissionsGroupIdAndPermissionsUserIdAndApplicationAdminTrue(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    void deleteAllByPermissionsUserId(@NotNull @NonNull Long userId);

    List<GroupUser> findAllByPermissionsUserId(@NotNull @NonNull Long userId);

    Page<GroupUser> findAllByPermissionsGroupIdIn(@NotNull @NonNull List<Long> groupIds, Pageable pageable);

    Page<GroupUser> findAllByPermissionsGroupNameContainsAndPermissionsUserEmailContains(@NotNull @NonNull String name, @NotNull @NonNull String email, Pageable pageable);

    List<Group> findPermissionsGroupByPermissionsGroupNameContainsOrPermissionsUserEmailContains(@NotNull @NonNull String name, @NotNull @NonNull String email);

    int countByPermissionsUserId(@NotNull @NonNull Long userId);

    Optional<GroupUser> findByPermissionsGroupIdAndPermissionsUserId(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    List<GroupUser> findAllByPermissionsGroupId(@NotNull @NonNull Long groupId);

    Page<GroupUser> findAllByPermissionsGroupNameContains(String groupName, Pageable pageable);

    Page<GroupUser> findAllByPermissionsUserEmailContains(String userEmail, Pageable pageable);

    List<Group> findPermissionsGroupByPermissionsGroupNameContains(String groupName);

    Page<GroupUser> findAllByPermissionsUserEmailContainsAndPermissionsGroupIdIn(String userEmail, List<Long> groupsList, Pageable pageable);

    int countByPermissionsGroup(Group group);
}
