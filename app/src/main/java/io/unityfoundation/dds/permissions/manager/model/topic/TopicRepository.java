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
    Optional<Topic> findByName(@NotNull @NonNull String name);

    Page<Topic> findAllByPermissionsGroupIn(List<Long> groupIds, Pageable pageable);
}
