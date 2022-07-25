package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import jakarta.inject.Singleton;

@Singleton
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public Page<Application> findAll(Pageable pageable) {
        return applicationRepository.findAll(pageable);
    }

    public void save(Application application) {
        applicationRepository.save(application);
    }

    public void deleteById(Long id) {
        applicationRepository.deleteById(id);
    }
}
