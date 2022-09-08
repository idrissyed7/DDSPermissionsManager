package io.unityfoundation.dds.permissions.manager.model.group;

public class GroupResponseDTO {
    private Group group;
    private int membershipCount;
    private int topicCount;
    private int applicationCount;

    public GroupResponseDTO() {
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
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
