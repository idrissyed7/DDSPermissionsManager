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

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Group permissionsGroup;

    @NonNull
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User permissionsUser;

    @NonNull
    private boolean isGroupAdmin = false;

    @NonNull
    private boolean isTopicAdmin = false;

    @NonNull
    private boolean isApplicationAdmin = false;


    public GroupUser() {
    }

    public GroupUser(@NonNull Group groupId, @NonNull User userId) {
        this.permissionsGroup = groupId;
        this.permissionsUser = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public Group getPermissionsGroup() {
        return permissionsGroup;
    }

    public void setPermissionsGroup(@NonNull Group group) {
        this.permissionsGroup = group;
    }

    @NonNull
    public User getPermissionsUser() {
        return permissionsUser;
    }

    public void setPermissionsUser(@NonNull User user) {
        this.permissionsUser = user;
    }


    public boolean isGroupAdmin() {
        return isGroupAdmin;
    }

    public void setGroupAdmin(boolean groupAdmin) {
        isGroupAdmin = groupAdmin;
    }

    public boolean isTopicAdmin() {
        return isTopicAdmin;
    }

    public void setTopicAdmin(boolean topicAdmin) {
        isTopicAdmin = topicAdmin;
    }

    public boolean isApplicationAdmin() {
        return isApplicationAdmin;
    }

    public void setApplicationAdmin(boolean applicationAdmin) {
        isApplicationAdmin = applicationAdmin;
    }
}
