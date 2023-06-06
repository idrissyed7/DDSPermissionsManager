package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends PageableRepository<Application, Long> {
    @NonNull
    Optional<Application> findByNameAndPermissionsGroup(@NotNull @NonNull String name, @NotNull @NonNull Group group);

    Page<Application> findByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(String application, String applicationDescription, String group, Pageable page);

    Optional<Application> findByNameEquals(@NotBlank String name);

    Page<Application> findById(Long id, Pageable pageable);

    Page<Application> findAllByPermissionsGroupIdIn(List<Long> groups, Pageable pageable);

    List<Long> findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(String application, String applicationDescription, String group);

    Page<Application> findAllByIdInAndPermissionsGroupIdIn(List<Long> all, List<Long> groups, Pageable pageable);

    List<Long> findIdByPermissionsGroupIdIn(List<Long> groups);

    Page<Application> findAllByMakePublicTrue(Pageable pageable);

    List<Application> findTop50ByMakePublicTrue();

    List<Long> findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(String application, String applicationDescription);

    Page<Application> findByMakePublicTrueAndIdIn(List<Long> entityIds, Pageable pageable);

    List<Application> findTop50ByMakePublicTrueAndIdIn(List<Long> entityIds);

    Page<Application> findByIdAndPermissionsGroupId(Long applicationId, Long groupId, Pageable pageable);

    Page<Application> findByIdAndPermissionsGroupIdIn(Long applicationId, List<Long> groups, Pageable pageable);
}