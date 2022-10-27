package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class AdminDTO {
    private Long id;
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
