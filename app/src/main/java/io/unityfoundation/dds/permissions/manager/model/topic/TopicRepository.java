package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;

@Repository
public interface TopicRepository extends PageableRepository<Topic, Long> {
}
