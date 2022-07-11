package io.unityfoundation.dds.permissions.manager.model.group;


import io.micronaut.core.annotation.NonNull;
import io.unityfoundation.dds.permissions.manager.model.user.User;

import javax.persistence.*;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "permissions_group")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private String name;

    @ManyToMany(targetEntity = User.class, cascade = CascadeType.ALL)
    private List<User> users;

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

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public boolean removeUser(Long userId) {
        return users.removeIf(user -> userId != null && userId.equals(user.getId()));
    }

    public void addUser(User user) {
        users.add(user);
    }
}
