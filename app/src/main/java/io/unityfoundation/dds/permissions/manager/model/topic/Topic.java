package io.unityfoundation.dds.permissions.manager.model.topic;


import io.micronaut.core.annotation.NonNull;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermission;
import io.unityfoundation.dds.permissions.manager.model.group.Group;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions_topic")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @NotBlank
    private String name;

    @NonNull
    private TopicKind kind;

    @ManyToOne
    @JoinColumn(name = "permissions_group_id", nullable = false)
    private Group permissionsGroup;

    // approach2
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "permissionsTopic")
    private Set<ApplicationPermission> applicationPermissions = new HashSet<>();

    public Topic() {
    }

    public Topic(@NonNull String name, @NonNull TopicKind kind) {
        this.name = name;
        this.kind = kind;
    }

    public Topic(@NonNull String name, @NonNull TopicKind kind, Group permissionsGroup) {
        this.name = name;
        this.kind = kind;
        this.permissionsGroup = permissionsGroup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public TopicKind getKind() {
        return kind;
    }

    public void setKind(@NonNull TopicKind kind) {
        this.kind = kind;
    }

    public Group getPermissionsGroup() {
        return permissionsGroup;
    }

    public void setPermissionsGroup(Group permissionsGroup) {
        this.permissionsGroup = permissionsGroup;
    }

    @PrePersist
    void trimName() {
        this.name = this.name.trim();
    }

    public Set<ApplicationPermission> getApplicationPermissions() {
        return applicationPermissions;
    }

    public void setApplicationPermissions(Set<ApplicationPermission> permissions) {
        this.applicationPermissions = permissions;
    }
}
