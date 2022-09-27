package io.unityfoundation.dds.permissions.manager.model.topic;


import io.micronaut.core.annotation.NonNull;
import io.unityfoundation.dds.permissions.manager.model.group.Group;

import javax.persistence.*;

@Entity
@Table(name = "permissions_topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private String name;

    @NonNull
    private TopicKind kind;

    @ManyToOne
    @JoinColumn(name = "permissions_group_id", nullable = false)
    private Group permissionsGroup;

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
}
