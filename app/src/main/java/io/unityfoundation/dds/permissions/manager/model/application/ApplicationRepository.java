package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends PageableRepository<Application, Long> {
    @NonNull
    Optional<Application> findByNameAndPermissionsGroup(@NotNull @NonNull String name,
                                                        @NotNull @NonNull Group group);

    @Query(value = "FROM Application a where a.name like :filter or a.permissionsGroup.name like :filter",
    countQuery = "select count(*) FROM Application a where a.name like :filter or a.permissionsGroup.name like :filter")
    Page<Application> searchByApplicationNameAndGroupName(String filter, Pageable page);
}