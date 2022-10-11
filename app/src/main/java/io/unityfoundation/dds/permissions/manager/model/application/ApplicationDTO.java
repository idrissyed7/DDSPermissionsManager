package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class ApplicationDTO {

    private Long id;
    private String name;
    private Long group;
    private String groupName;

    public ApplicationDTO() {
    }

    public ApplicationDTO(Application app) {
        this.id = app.getId();
        this.name = app.getName();
        this.group = app.getPermissionsGroup().getId();
        this.groupName = app.getPermissionsGroup().getName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getGroup() {
        return group;
    }

    public void setGroup(Long group) {
        this.group = group;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
