package io.unityfoundation.dds.permissions.manager.model.group;


import io.micronaut.core.annotation.NonNull;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.Collections;
import java.util.Set;

@Entity
@Table(name = "permissions_group")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Column(unique = true)
    private String name;

    @ManyToMany(targetEntity = User.class, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JoinTable(name="permissions_group_members",
            joinColumns=
            @JoinColumn(name="group_id", referencedColumnName="id"),
            inverseJoinColumns=
            @JoinColumn(name="user_id", referencedColumnName="id")
    )
    private Set<User> users;

    @ManyToMany(targetEntity = User.class, cascade = CascadeType.ALL)
    @JoinTable(name="permissions_group_admins",
            joinColumns=
            @JoinColumn(name="group_id", referencedColumnName="id"),
            inverseJoinColumns=
            @JoinColumn(name="user_id", referencedColumnName="id")
    )
    @LazyCollection(LazyCollectionOption.FALSE)
    private Set<User> admins;

    public Group() {
    }

    public Group(@NonNull String name) {
        this.name = name;
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

    public Set<User> getUsers() {
        if (users == null) return null;
        return Collections.unmodifiableSet(users);
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public boolean removeUser(Long userId) {
        return users.removeIf(user -> userId != null && userId.equals(user.getId()));
    }

    public void addUser(User user) {
        users.add(user);
    }

    public Set<User> getAdmins() {
        if (admins == null) return null;
        return Collections.unmodifiableSet(admins);
    }

    public void setAdmins(Set<User> admins) {
        this.admins = admins;
    }

    public boolean removeAdmin(Long userId) {
        return admins.removeIf(user -> userId != null && userId.equals(user.getId()));
    }

    public void addAdmin(User user) {
        admins.add(user);
    }
}
