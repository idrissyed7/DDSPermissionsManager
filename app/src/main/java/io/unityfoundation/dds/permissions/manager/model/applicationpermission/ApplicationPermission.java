package io.unityfoundation.dds.permissions.manager.model.applicationpermission;


import io.micronaut.core.annotation.NonNull;

import javax.persistence.*;

@Entity
@Table(name = "permissions_application_permission")
public class ApplicationPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private Long permissionsApplication;

    @NonNull
    private Long permissionsTopic;

    @NonNull
    private AccessType accessType;


    public ApplicationPermission() {
    }

    public ApplicationPermission(@NonNull Long groupId, @NonNull Long userId) {
        this.permissionsApplication = groupId;
        this.permissionsTopic = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public Long getPermissionsApplication() {
        return permissionsApplication;
    }

    public void setPermissionsApplication(@NonNull Long groupId) {
        this.permissionsApplication = groupId;
    }

    @NonNull
    public Long getPermissionsTopic() {
        return permissionsTopic;
    }

    public void setPermissionsTopic(@NonNull Long userId) {
        this.permissionsTopic = userId;
    }

    public AccessType getAccessType() {
        return this.accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }
}
