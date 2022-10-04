package io.unityfoundation.dds.permissions.manager.model.applicationpermission;


import io.micronaut.core.annotation.NonNull;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;

import javax.persistence.*;

@Entity
@Table(name = "permissions_application_permission")
public class ApplicationPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "permissions_group_application_id", nullable = false) // approach2
    private Application permissionsApplication;

    @NonNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "permissions_group_topic_id", nullable = false) // approach2
    private Topic permissionsTopic;

    @NonNull
    private AccessType accessType;


    public ApplicationPermission() {
    }

    public ApplicationPermission(@NonNull Application application, @NonNull Topic topic, @NonNull AccessType accessType) {
        this.permissionsApplication = application;
        this.permissionsTopic = topic;
        this.accessType = accessType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public Application getPermissionsApplication() {
        return permissionsApplication;
    }

    public void setPermissionsApplication(@NonNull Application permissionsApplication) {
        this.permissionsApplication = permissionsApplication;
    }

    @NonNull
    public Topic getPermissionsTopic() {
        return permissionsTopic;
    }

    public void setPermissionsTopic(@NonNull Topic permissionsTopic) {
        this.permissionsTopic = permissionsTopic;
    }

    public AccessType getAccessType() {
        return this.accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }
}
