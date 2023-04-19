package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.unityfoundation.dds.permissions.manager.model.group.Group;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Entity
@Table(name = "permissions_application")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @NotBlank
    @Size(min = 3)
    private String name;

    @Nullable
    private String encryptedPassword;

    @Size(max = 4000)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition="BOOLEAN DEFAULT false")
    private Boolean makePublic;

    @ManyToOne
    @JoinColumn(name = "permissions_group_id", nullable = false)
    private Group permissionsGroup;

    public Application() {
    }

    public Application(@NonNull String name, String description, Boolean isPublic) {
        this.name = name;
        this.description = description;
        this.makePublic = isPublic;
    }

    public Application(@NonNull String name, @NonNull Group permissionsGroup) {
        this.name = name;
        this.permissionsGroup = permissionsGroup;
    }

    public Application(@NonNull String name, @NonNull Group permissionsGroup, String description, Boolean isPublic) {
        this.name = name;
        this.description = description;
        this.makePublic = isPublic;
        this.permissionsGroup = permissionsGroup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public Group getPermissionsGroup() {
        return permissionsGroup;
    }

    public void setPermissionsGroup(Group permissionsGroup) {
        this.permissionsGroup = permissionsGroup;
    }

    @Nullable
    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(@Nullable String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    @PrePersist
    void trimName() {
        this.name = this.name.trim();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public Boolean getMakePublic() {
        return makePublic;
    }

    public void setMakePublic(Boolean isPublic) {
        makePublic = isPublic;
    }
}
