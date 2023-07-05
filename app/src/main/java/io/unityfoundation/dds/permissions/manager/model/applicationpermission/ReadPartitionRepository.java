package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;

@Repository
public interface ReadPartitionRepository extends PageableRepository<ReadPartition, Long> {
}
