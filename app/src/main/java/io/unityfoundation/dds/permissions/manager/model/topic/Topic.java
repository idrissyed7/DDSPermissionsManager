package io.unityfoundation.dds.permissions.manager.model.topic;


import io.micronaut.core.annotation.NonNull;

import javax.persistence.*;

@Entity
@Table(name = "permissions_topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private String name;

    public Topic() {
    }

    public Topic(@NonNull String name) {
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
}
