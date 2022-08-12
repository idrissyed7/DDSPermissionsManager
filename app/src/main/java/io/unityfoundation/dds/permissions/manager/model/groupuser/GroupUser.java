package io.unityfoundation.dds.permissions.manager.model.groupuser;


import io.micronaut.core.annotation.NonNull;

import javax.persistence.*;

@Entity
@Table(name = "permissions_group_user")
public class GroupUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private Long permissionsGroup;

    @NonNull
    private Long permissionsUser;

    @NonNull
    private boolean isGroupAdmin = false;

    @NonNull
    private boolean isTopicAdmin = false;

    @NonNull
    private boolean isApplicationAdmin = false;


    public GroupUser() {
    }

    public GroupUser(@NonNull Long groupId, @NonNull Long userId) {
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
    public Long getPermissionsGroup() {
        return permissionsGroup;
    }

    public void setPermissionsGroup(@NonNull Long groupId) {
        this.permissionsGroup = groupId;
    }

    @NonNull
    public Long getPermissionsUser() {
        return permissionsUser;
    }

    public void setPermissionsUser(@NonNull Long userId) {
        this.permissionsUser = userId;
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
