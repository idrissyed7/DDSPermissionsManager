package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Introspected
public class SimpleGroupDTO {

    private Long id;
    @NotBlank
    @Size(min = 3)
    private String name;
    @Size(max = 4000)
    private String description;
    private Boolean isPublic;

    public SimpleGroupDTO() {
    }

    public SimpleGroupDTO(Long id, String name, String description, Boolean isPublic) {
        this.id = id;
        this.name = name;
        this.isPublic = isPublic;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }
}
