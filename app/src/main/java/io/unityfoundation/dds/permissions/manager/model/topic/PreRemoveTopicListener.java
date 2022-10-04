package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.data.annotation.event.PreRemove;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionService;
import jakarta.inject.Singleton;

// approach1
@Singleton
public class PreRemoveTopicListener {

    private final ApplicationPermissionService applicationPermissionService;

    public PreRemoveTopicListener(ApplicationPermissionService applicationPermissionService) {
        this.applicationPermissionService = applicationPermissionService;
    }

    @PreRemove
    void preTopicRemove(Topic topic) {
        // does not execute
        applicationPermissionService.deleteAllByTopic(topic);
    }
}