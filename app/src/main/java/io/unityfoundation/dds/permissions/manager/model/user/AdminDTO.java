// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Introspected
public class AdminDTO {
    private Long id;
    @NotBlank
    @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")
    private String email;

    public AdminDTO(String email) {
        this.email = email;
    }

    public AdminDTO(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
