// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.model.groupuser;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class GroupUserResponseDTO {

    private Long id;
    private Long permissionsUser;
    private String permissionsUserEmail;
    private long permissionsGroup;
    private String permissionsGroupName;
    private boolean isGroupAdmin = false;
    private boolean isTopicAdmin = false;
    private boolean isApplicationAdmin = false;

    public GroupUserResponseDTO() {
    }

    public GroupUserResponseDTO(GroupUser member) {
        this.id = member.getId();
        this.permissionsUser = member.getPermissionsUser().getId();
        this.permissionsUserEmail = member.getPermissionsUser().getEmail();
        this.permissionsGroup = member.getPermissionsGroup().getId();
        this.permissionsGroupName = member.getPermissionsGroup().getName();
        this.isGroupAdmin = member.isGroupAdmin();
        this.isTopicAdmin = member.isTopicAdmin();
        this.isApplicationAdmin = member.isApplicationAdmin();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPermissionsUser() {
        return permissionsUser;
    }

    public void setPermissionsUser(Long permissionsUser) {
        this.permissionsUser = permissionsUser;
    }

    public long getPermissionsGroup() {
        return permissionsGroup;
    }

    public void setPermissionsGroup(long permissionsGroup) {
        this.permissionsGroup = permissionsGroup;
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

    public String getPermissionsUserEmail() {
        return permissionsUserEmail;
    }

    public void setPermissionsUserEmail(String permissionsUserEmail) {
        this.permissionsUserEmail = permissionsUserEmail;
    }

    public String getPermissionsGroupName() {
        return permissionsGroupName;
    }

    public void setPermissionsGroupName(String permissionsGroupName) {
        this.permissionsGroupName = permissionsGroupName;
    }
}
