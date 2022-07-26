package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.core.annotation.NonNull;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "permissions_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private String firstName;

    @NonNull
    private String lastName;

    @NonNull
    private String email;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = Role.class)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private List<Role> roles;

    public User() {
    }

    public User(@NonNull String firstName, @NonNull String lastName, @NonNull String email, @NonNull List<Role> roles) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(@NonNull String firstName) {
        this.firstName = firstName;
    }

    @NonNull
    public String getLastName() {
        return lastName;
    }

    public void setLastName(@NonNull String lastName) {
        this.lastName = lastName;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
}
