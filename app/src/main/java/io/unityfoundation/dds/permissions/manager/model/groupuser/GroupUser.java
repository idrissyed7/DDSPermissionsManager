package io.unityfoundation.dds.permissions.manager.model.groupuser;

import io.micronaut.core.annotation.NonNull;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.user.User;

import javax.persistence.*;

@Entity
@Table(name = "permissions_group_user")
public class GroupUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    private Group permissionsGroup;

    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    private User permissionsUser;

    @NonNull
    private boolean groupAdmin = false;

    @NonNull
    private boolean topicAdmin = false;

    @NonNull
    private boolean applicationAdmin = false;

    public GroupUser() {
    }

    public GroupUser(@NonNull Group group, @NonNull User user) {
        this.permissionsGroup = group;
        this.permissionsUser = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Group getPermissionsGroup() {
        return permissionsGroup;
    }

    public void setPermissionsGroup(Group permissionsGroup) {
        this.permissionsGroup = permissionsGroup;
    }

    public User getPermissionsUser() {
        return permissionsUser;
    }

    public void setPermissionsUser(User permissionsUser) {
        this.permissionsUser = permissionsUser;
    }

    public boolean isGroupAdmin() {
        return groupAdmin;
    }

    public void setGroupAdmin(boolean groupAdmin) {
        this.groupAdmin = groupAdmin;
    }

    public boolean isTopicAdmin() {
        return topicAdmin;
    }

    public void setTopicAdmin(boolean topicAdmin) {
        this.topicAdmin = topicAdmin;
    }

    public boolean isApplicationAdmin() {
        return applicationAdmin;
    }

    public void setApplicationAdmin(boolean applicationAdmin) {
        this.applicationAdmin = applicationAdmin;
    }
}
