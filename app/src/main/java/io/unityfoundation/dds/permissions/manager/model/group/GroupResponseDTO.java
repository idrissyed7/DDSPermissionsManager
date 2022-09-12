package io.unityfoundation.dds.permissions.manager.model.group;

import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;

import java.util.Set;

public class GroupResponseDTO {

    private Long id;
    private String name;
    private Set<Topic> topics;
    private Set<Application> applications;
    private int membershipCount;
    private int topicCount;
    private int applicationCount;

    public GroupResponseDTO() {
    }

    public void setGroupFields(Group group) {
        this.id = group.getId();
        this.name = group.getName();
        this.topics = group.getTopics();
        this.applications = group.getApplications();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Topic> getTopics() {
        return topics;
    }

    public Set<Application> getApplications() {
        return applications;
    }

    public long getMembershipCount() {
        return membershipCount;
    }

    public void setMembershipCount(int membershipCount) {
        this.membershipCount = membershipCount;
    }

    public long getTopicCount() {
        return topicCount;
    }

    public void setTopicCount(int topicCount) {
        this.topicCount = topicCount;
    }

    public long getApplicationCount() {
        return applicationCount;
    }

    public void setApplicationCount(int applicationCount) {
        this.applicationCount = applicationCount;
    }
}
