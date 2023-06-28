package io.unityfoundation.dds.permissions.manager.model.applicationpermission;

import io.micronaut.core.annotation.Introspected;
import io.unityfoundation.dds.permissions.manager.model.EntityDTO;

@Introspected
public class AccessPermissionDTO implements EntityDTO {
    private final Long topicId;
    private final String topicName;
    private final String topicCanonicalName;
    private final String topicGroup;
    private final Long applicationId;
    private final String applicationName;
    private final String applicationGroupName;
    private final AccessType accessType;
    private final Long id;

    public AccessPermissionDTO(Long id, Long topicId, String topicName, String topicCanonicalName, String topicGroup, Long applicationId, String applicationName, String applicationGroupName, AccessType accessType) {
        this.id = id;
        this.topicId = topicId;
        this.topicName = topicName;
        this.topicCanonicalName = topicCanonicalName;
        this.topicGroup = topicGroup;
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

    public String getTopicGroup() {
        return topicGroup;
    }

    public String getTopicCanonicalName() {
        return topicCanonicalName;
    }
}
