package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends PageableRepository<Application, Long> {
    @NonNull
    Optional<Application> findByNameAndPermissionsGroup(@NotNull @NonNull String name,
                                                        @NotNull @NonNull Group group);
}
