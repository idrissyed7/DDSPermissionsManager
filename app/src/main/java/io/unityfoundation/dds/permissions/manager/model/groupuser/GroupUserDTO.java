package io.unityfoundation.dds.permissions.manager.model.groupuser;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Introspected
public class GroupUserDTO {

    private Long id;

    @NotBlank
    @Email
    private String email;
    private long permissionsGroup;
    private boolean isGroupAdmin = false;
    private boolean isTopicAdmin = false;
    private boolean isApplicationAdmin = false;

    public GroupUserDTO() {
    }

    public GroupUserDTO(GroupUser member) {
        this.id = member.getId();
        this.email = member.getPermissionsUser().getEmail();
        this.permissionsGroup = member.getPermissionsGroup().getId();
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}
