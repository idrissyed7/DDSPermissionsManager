package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Introspected
public class ApplicationDTO {

    private Long id;
    @NotBlank
    @Size(min = 3)
    private String name;
    @Size(max = 4000)
    private String description;
    @NotNull
    private Long group;
    private String groupName;

    public ApplicationDTO() {
    }

    public ApplicationDTO(Application app) {
        this.id = app.getId();
        this.name = app.getName();
        this.description = app.getDescription();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
