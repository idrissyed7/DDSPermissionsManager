package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Repository
public interface GroupRepository extends CrudRepository<Group, Long> {
    @Join(value = "users", type = Join.Type.LEFT_FETCH)
    Optional<Group> findById(@NotNull @NonNull Long id);
}
