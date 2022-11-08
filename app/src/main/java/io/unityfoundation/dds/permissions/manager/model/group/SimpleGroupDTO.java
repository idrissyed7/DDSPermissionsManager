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

    public SimpleGroupDTO() {
    }

    public SimpleGroupDTO(Long id, String name) {
        this.id = id;
        this.name = name;
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
}
