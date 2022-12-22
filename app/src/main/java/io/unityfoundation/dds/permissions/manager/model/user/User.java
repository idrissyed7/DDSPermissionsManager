package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.core.annotation.NonNull;

import javax.persistence.*;
import javax.validation.constraints.Email;

@Entity
@Table(name = "permissions_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")
    @Column(unique = true)
    private String email;

    private boolean admin = false;

    public User() {
    }

    public User(@NonNull String email) {
        this.email = email;
    }

    public User(@NonNull String email, @NonNull boolean admin) {
        this.email = email;
        this.admin = admin;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
