package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Introspected
public class AdminDTO {
    private Long id;
    @NotBlank
    @Email
    private String email;

    public AdminDTO(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
