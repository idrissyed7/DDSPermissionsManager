package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;

import java.util.List;

@Repository
public interface GroupRepository extends PageableRepository<Group, Long> {
    Page<Group> findAllByIdIn(List<Long> groupIds, Pageable pageable);
}
