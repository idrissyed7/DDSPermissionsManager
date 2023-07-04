package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;

import java.util.Collection;
import java.util.List;

@Repository
public interface PartitionRepository extends PageableRepository<Partition, Long> {
}
