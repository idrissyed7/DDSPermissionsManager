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

    @Query(value = "select count(*) from permissions_group_user " +
            "where permissions_group = :groupId and permissions_user = :userId and is_group_admin = true",
            countQuery = "select DISTINCT count(*) from permissions_group_user " +
                    "where permissions_group = :groupId and permissions_user = :userId and is_group_admin = true",
            nativeQuery = true )
    int countByPermissionsGroupAndPermissionsUserAndGroupAdminTrue(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    @Query(value = "select count(*) from permissions_group_user " +
            "where permissions_group = :groupId and permissions_user = :userId and is_topic_admin = true",
            countQuery = "select DISTINCT count(*) from permissions_group_user " +
                    "where permissions_group = :groupId and permissions_user = :userId and is_topic_admin = true",
            nativeQuery = true )
    int countByPermissionsGroupAndPermissionsUserAndTopicAdminTrue(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    @Query(value = "select COUNT(*) from permissions_group_user " +
            "where permissions_group = :groupId and permissions_user = :userId and is_application_admin = true",
            countQuery = "select DISTINCT count(*) from permissions_group_user " +
                    "where permissions_group = :groupId and permissions_user = :userId and is_application_admin = true",
            nativeQuery = true )
    int countByPermissionsGroupAndPermissionsUserAndApplicationAdminTrue(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    void deleteAllByPermissionsUser(@NotNull @NonNull Long userId);
    void deleteAllByPermissionsGroupAndPermissionsUser(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    List<GroupUser> findAllByPermissionsUser(@NotNull @NonNull Long userId);

    Optional<GroupUser> findByPermissionsGroupAndPermissionsUser(@NotNull @NonNull Long groupId, @NotNull @NonNull Long userId);

    List<GroupUser> findAllByPermissionsGroup(@NotNull @NonNull Long groupId);
}
