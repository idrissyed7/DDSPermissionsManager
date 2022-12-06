package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class AccessPermissionDTO {
    private final Long topicId;
    private final String topicName;
    private final Long applicationId;
    private final String applicationName;
    private final String applicationGroupName;
    private final AccessType accessType;
    private final Long id;

    public AccessPermissionDTO(Long id, Long topicId, String topicName, Long applicationId, String applicationName, String applicationGroupName, AccessType accessType) {
        this.id = id;
        this.topicId = topicId;
        this.topicName = topicName;
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.applicationGroupName = applicationGroupName;
        this.accessType = accessType;
    }

    public Long getTopicId() {
        return topicId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public Long getId() {
        return id;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getApplicationGroupName() {
        return applicationGroupName;
    }
}
