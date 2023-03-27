package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends PageableRepository<Group, Long> {
    Optional<Group> findByName(@NotNull @NonNull String name);
    Page<Group> findAllByIdIn(List<Long> groupIds, Pageable pageable);
    Page<Group> findAllByNameContainsIgnoreCase(String filter, Pageable pageable);
    Page<Group> findAllByIdInAndNameContainsIgnoreCase(List<Long> groupIds, String filter, Pageable pageable);
    Page<Group> findAllByNameContainsIgnoreCaseAndMakePublicTrue(String filter, Pageable pageable);
    List<Group> findTop50ByNameContainsIgnoreCaseAndMakePublicTrue(String query);
    Page<Group> findAllByMakePublicTrue(Pageable pageable);
    List<Group> findTop50ByMakePublicTrue();
}
