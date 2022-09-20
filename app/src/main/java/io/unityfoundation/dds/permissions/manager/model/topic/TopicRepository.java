package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends PageableRepository<Topic, Long> {
    Optional<Topic> findByName(@NotNull @NonNull String name);

    Page<Topic> findAllByPermissionsGroupIn(List<Long> groupIds, Pageable pageable);

    @Query(value = "select pt.* from permissions_topics as pt " +
            "inner join permissions_group_topics as pgt on pt.id = pgt.topic_id " +
            "inner join permissions_group as pg on pgt.group_id = pg.id " +
            "where pt.name ilike ('%'||:topic||'%') or pg.name ilike ('%'||:group||'%')",
            countQuery = "select count(pt.*) from permissions_topics as pt "  +
                    "inner join permissions_group_topics as pgt on pt.id = pgt.topic_id " +
                    "inner join permissions_group as pg on pgt.group_id = pg.id " +
                    "where pt.name ilike ('%'||:topic||'%') or pg.name ilike ('%'||:group||'%')",
            nativeQuery = true )
    Page<Topic> findAllByTopicNameAndGroupNameContainsIgnoreCase(String topic, String group, Pageable pageable);

    @Query(value = "select pt.* from permissions_topics as pt " +
            "inner join permissions_group_topics as pgt on pt.id = pgt.topic_id " +
            "inner join permissions_group as pg on pgt.group_id = pg.id " +
            "where (pt.name ilike ('%'||:topic||'%') or pg.name ilike ('%'||:group||'%')) and pg.id in :groupIds",
            countQuery = "select count(pt.*) from permissions_topics as pt "  +
                    "inner join permissions_group_topics as pgt on pt.id = pgt.topic_id " +
                    "inner join permissions_group as pg on pgt.group_id = pg.id " +
                    "where (pt.name ilike ('%'||:topic||'%') or pg.name ilike ('%'||:group||'%')) and pg.id in :groupIds",
            nativeQuery = true )
    Page<Topic> findAllByTopicNameAndGroupNameContainsIgnoreCaseAndGroupIdIn(String topic, String group, List<Long> groupIds, Pageable pageable);
}
