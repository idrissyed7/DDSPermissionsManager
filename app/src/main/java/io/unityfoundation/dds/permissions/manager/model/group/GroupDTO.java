package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.core.annotation.Introspected;

import java.util.Set;

@Introspected
public class GroupDTO {

    private Long id;
    private String name;
    private Set<Long> topics;
    private Set<Long> applications;
    private int membershipCount;
    private int topicCount;
    private int applicationCount;

    public GroupDTO() {
    }

    public void setGroupFields(Group group) {
        this.id = group.getId();
        this.name = group.getName();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Long> getTopics() {
        return topics;
    }

    public void setTopics(Set<Long> topics) {
        this.topics = topics;
    }

    public Set<Long> getApplications() {
        return applications;
    }

    public void setApplications(Set<Long> applications) {
        this.applications = applications;
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
