package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends PageableRepository<Group, Long> {
    Page<Group> findAllByIdIn(List<Long> groupIds, Pageable pageable);
}
