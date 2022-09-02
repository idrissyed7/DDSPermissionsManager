package io.unityfoundation.dds.permissions.manager.model.group;


import io.micronaut.core.annotation.NonNull;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions_group")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @NotBlank
    @Column(unique = true)
    private String name;

    @ManyToMany(targetEntity = Topic.class, cascade = CascadeType.ALL)
    @JoinTable(name="permissions_group_topics",
            joinColumns=
            @JoinColumn(name="group_id", referencedColumnName="id"),
            inverseJoinColumns=
            @JoinColumn(name="topic_id", referencedColumnName="id")
    )
    @LazyCollection(LazyCollectionOption.FALSE)
    private Set<Topic> topics = new HashSet<>();

    @ManyToMany(targetEntity = Application.class, cascade = CascadeType.ALL)
    @JoinTable(name="permissions_group_applications",
            joinColumns=
            @JoinColumn(name="group_id", referencedColumnName="id"),
            inverseJoinColumns=
            @JoinColumn(name="application_id", referencedColumnName="id")
    )
    @LazyCollection(LazyCollectionOption.FALSE)
    private Set<Application> applications = new HashSet<>();

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

    public Set<Topic> getTopics() {
        if (topics == null) return null;
        return Collections.unmodifiableSet(topics);
    }

    public void setTopics(Set<Topic> topics) {
        this.topics = topics;
    }

    public boolean removeTopic(Long topicId) {
        return topics.removeIf(topic -> topicId != null && topicId.equals(topic.getId()));
    }

    public void addTopic(Topic topic) {
        topics.add(topic);
    }

    public Set<Application> getApplications() {
        if (applications == null) return null;
        return Collections.unmodifiableSet(applications);
    }

    public void setApplications(Set<Application> applications) {
        this.applications = applications;
    }

    public boolean removeApplication(Long applicationId) {
        return topics.removeIf(application -> applicationId != null && applicationId.equals(application.getId()));
    }

    public void addApplication(Application application) {
        applications.add(application);
    }

    @PrePersist
    void encodePassword() {
        this.name = this.name.trim();
    }
}
