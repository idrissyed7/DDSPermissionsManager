package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import jakarta.inject.Singleton;

@Singleton
public class ApplicationPermissionService {

    private final ApplicationPermissionRepository applicationPermissionRepository;


    public ApplicationPermissionService(ApplicationPermissionRepository applicationPermissionRepository) {
        this.applicationPermissionRepository = applicationPermissionRepository;
    }

}
