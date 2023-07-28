// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.core.annotation.Introspected;
import io.unityfoundation.dds.permissions.manager.model.EntityDTO;

import java.util.Set;

@Introspected
public class DetailedGroupDTO implements EntityDTO {

    private Long id;
    private String name;
    private String description;
    private Boolean isPublic;
    private Set<Long> topics;
    private Set<Long> applications;
    private int membershipCount;
    private int topicCount;
    private int applicationCount;

    public DetailedGroupDTO() {
    }

    public void setGroupFields(Group group) {
        this.id = group.getId();
        this.name = group.getName();
        this.isPublic = group.getMakePublic();
        this.description = group.getDescription();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getPublic() {
        return isPublic;
    }

    public void setPublic(Boolean aPublic) {
        isPublic = aPublic;
    }
}
