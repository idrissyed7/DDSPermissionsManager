package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    Iterable<User> findAllByIdNotInList(List ids);
}
