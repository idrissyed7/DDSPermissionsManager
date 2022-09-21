package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class TopicShowResponseDTO {
    private Long id;

    private String name;

    private TopicKind kind;

    private Long groupId;

    private String groupName;

    public TopicShowResponseDTO() {
    }

    public TopicShowResponseDTO(Topic topic) {
        this.id = topic.getId();
        this.name = topic.getName();
        this.kind = topic.getKind();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TopicKind getKind() {
        return kind;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
