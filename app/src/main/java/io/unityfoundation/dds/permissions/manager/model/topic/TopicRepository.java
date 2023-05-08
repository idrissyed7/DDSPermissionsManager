package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.core.annotation.NonNull;
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

    Page<Topic> findAllByPermissionsGroupIdIn(List<Long> groupIds, Pageable pageable);

    Page<Topic> findAllByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(String topic, String topicDescription, String group, Pageable pageable);

    List<Long> findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(String topic,String topicDescription, String group);

    Page<Topic> findAllByIdInAndPermissionsGroupIdIn(List<Long> all, List<Long> groups, Pageable pageable);

    Optional<Topic> findByNameAndPermissionsGroup(@NotNull @NonNull String name,
                                                  @NotNull @NonNull Group group);

    List<Long> findIdByPermissionsGroupIdIn(List<Long> groups);

    Page<Topic> findAllByMakePublicTrue(Pageable pageable);

    List<Topic> findTop50ByMakePublicTrue();

    List<Long> findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(String topic, String topicDescription);

    Page<Topic> findByMakePublicTrueAndIdIn(List<Long> entityIds, Pageable pageable);

    List<Topic> findTop50ByMakePublicTrueAndIdIn(List<Long> secondEntityIds);
}
