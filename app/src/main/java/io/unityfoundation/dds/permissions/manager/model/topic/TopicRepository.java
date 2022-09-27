package io.unityfoundation.dds.permissions.manager.model.topic;

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
public interface TopicRepository extends PageableRepository<Topic, Long> {
    Optional<Topic> findByName(@NotNull @NonNull String name);

    Page<Topic> findAllByPermissionsGroupIdIn(List<Long> groupIds, Pageable pageable);

    Page<Topic> findAllByNameContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(String topic, String group, Pageable pageable);

    @Query(value = "select pt.* from permissions_topics as pt " +
            "inner join permissions_group as pg on pt.permissions_group_id = pg.id " +
            "where (pt.name ilike ('%'||:topic||'%') or pg.name ilike ('%'||:group||'%')) and pg.id in :groupIds",
            countQuery = "select count(pt.*) from permissions_topics as pt "  +
                    "inner join permissions_group as pg on pt.permissions_group_id = pg.id " +
                    "where (pt.name ilike ('%'||:topic||'%') or pg.name ilike ('%'||:group||'%')) and pg.id in :groupIds",
            nativeQuery = true )
    Page<Topic> findAllByTopicNameOrGroupNameContainsIgnoreCaseAndGroupIdIn(String topic, String group, List<Long> groupIds, Pageable pageable);

    Optional<Topic> findByNameAndPermissionsGroup(@NotNull @NonNull String name,
                                                  @NotNull @NonNull Group group);
}
