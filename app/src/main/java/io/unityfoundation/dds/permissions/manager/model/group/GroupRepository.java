package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Repository
public interface GroupRepository extends PageableRepository<Group, Long> {
    @Join(value = "users", type = Join.Type.LEFT_FETCH)
    Optional<Group> findById(@NotNull @NonNull Long id);

    @Query(value = "select DISTINCT g.* from permissions_group g " +
            "left join permissions_group_members gm on g.id = gm.group_id " +
            "left join permissions_group_admins ga on g.id = gm.group_id " +
            "where gm.user_id = :userId or ga.user_id = :userId",
            countQuery = "select DISTINCT count(g.*) from permissions_group g " +
                    "left join permissions_group_members gm on g.id = gm.group_id " +
                    "left join permissions_group_admins ga on g.id = gm.group_id " +
                    "where gm.user_id = :userId or ga.user_id = :userId",
            nativeQuery = true )
    Page<Group> findIfMemberOfGroup(Long userId, Pageable pageable);
}
