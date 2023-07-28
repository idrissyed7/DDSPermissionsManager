// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.DateCreated;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Entity
class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @NotBlank
    String username;

    @NonNull
    @NotBlank
    String refreshToken;

    @NonNull
    @NotNull
    Boolean revoked;

    @DateCreated
    @NonNull
    @NotNull
    Instant dateCreated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @NonNull
    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(@NonNull Boolean revoked) {
        this.revoked = revoked;
    }

    @NonNull
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(@NonNull String refreshToken) {
        this.refreshToken = refreshToken;
    }
}