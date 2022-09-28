package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.core.annotation.Introspected;

@Introspected
public class AccessPermissionDTO {
    private final Long topicId;
    private final Long applicationId;
    private final AccessType accessType;

    public AccessPermissionDTO(Long topicId, Long applicationId, AccessType accessType) {
        this.topicId = topicId;
        this.applicationId = applicationId;
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
}