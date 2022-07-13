package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface TopicRepository extends CrudRepository<Topic, Long> {
}
