package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class GroupSearchDTO {

    private Long id;
    private String name;

    public GroupSearchDTO() {
    }

    public void setGroupFields(Group group) {
        this.id = group.getId();
        this.name = group.getName();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
