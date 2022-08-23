package io.unityfoundation.dds.permissions.manager.model.groupuser;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupUserRepository extends PageableRepository<GroupUser, Long> {

    @Query(value = "select * from permissions_group_user " +
            "where permissions_group_id = :groupId and permissions_user_id = :userId and is_group_admin = true",
            countQuery = "select DISTINCT count(*) from permissions_group_user " +
                    "where permissions_group_id = :groupId and permissions_user_id = :userId and is_group_admin = true",
            nativeQuery = true )
    Optional<GroupUser> findByPermissionsGroupAndPermissionsUserAndGroupAdminTrue(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    @Query(value = "select * from permissions_group_user " +
            "where permissions_group_id = :groupId and permissions_user_id = :userId and is_topic_admin = true",
            countQuery = "select DISTINCT count(*) from permissions_group_user " +
                    "where permissions_group_id = :groupId and permissions_user_id = :userId and is_topic_admin = true",
            nativeQuery = true )
    Optional<GroupUser> findByPermissionsGroupIdAndPermissionsUserIdAndTopicAdminTrue(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    void deleteAllByPermissionsUserId(@NotNull @NonNull Long userId);
    void deleteAllByPermissionsGroupIdAndPermissionsUserId(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    List<GroupUser> findAllByPermissionsUserId(@NotNull @NonNull Long userId);

    Optional<GroupUser> findByPermissionsGroupIdAndPermissionsUserId(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    List<GroupUser> findAllByPermissionsGroupId(@NotNull @NonNull Long groupId);
}
