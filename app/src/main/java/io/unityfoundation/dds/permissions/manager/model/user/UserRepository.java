package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;

import java.util.List;

@Repository
public interface UserRepository extends PageableRepository<User, Long> {
    Iterable<User> findAllByIdNotInList(List ids);
}
