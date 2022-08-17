package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.repository.PageableRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends PageableRepository<User, Long> {
    Iterable<User> findAllByIdNotInList(List ids);
    Optional<User> findByEmail(String email);
    Page<User> findAllByIdIn(List<Long> groupIds, Pageable pageable);
}
