package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;

@Repository
public interface ApplicationRepository extends PageableRepository<Application, Long> {
}
