package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;

@Introspected
public class TopicDTO {

    private Long id;
    @NonNull
    private String name;
    private TopicKind kind;
    private Long group;
    private String groupName;

    public TopicDTO() {
    }

    public TopicDTO(Topic topic) {
        this.id = topic.getId();
        this.name = topic.getName();
        this.kind = topic.getKind();
        this.group = topic.getPermissionsGroup().getId();
        this.groupName = topic.getPermissionsGroup().getName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TopicKind getKind() {
        return kind;
    }

    public void setKind(TopicKind kind) {
        this.kind = kind;
    }

    public Long getGroup() {
        return group;
    }

    public void setGroup(Long group) {
        this.group = group;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
