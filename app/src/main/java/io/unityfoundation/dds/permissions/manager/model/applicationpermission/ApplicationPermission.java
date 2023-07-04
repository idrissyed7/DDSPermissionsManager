package io.unityfoundation.dds.permissions.manager.model.applicationpermission;


import io.micronaut.core.annotation.NonNull;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions_application_permission")
public class ApplicationPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Application permissionsApplication;

    @NonNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Topic permissionsTopic;

    @NonNull
    private AccessType accessType;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "applicationPermission")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Partition> partitions = new HashSet<>();


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

    public Set<Partition> getPartitions() {
        if (partitions == null) return null;
        return Collections.unmodifiableSet(partitions);
    }

    public void setPartitions(Set<Partition> partitions) {
        this.partitions = partitions;
    }

    public boolean removePartition(Long partitionId) {
        return partitions.removeIf(partition -> partitionId != null && partitionId.equals(partition.getId()));
    }
}
