package io.unityfoundation.dds.permissions.manager.testing.util;

import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionRepository;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;

@Singleton
public class DbCleanup {
    private final TopicRepository topicRepository;
    private final GroupRepository groupRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationPermissionRepository applicationPermissionRepository;
    private final GroupUserRepository groupUserRepository;

    public DbCleanup(TopicRepository topicRepository, GroupRepository groupRepository, ApplicationRepository applicationRepository, UserRepository userRepository, ApplicationPermissionRepository applicationPermissionRepository, GroupUserRepository groupUserRepository) {
        this.topicRepository = topicRepository;
        this.groupRepository = groupRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.applicationPermissionRepository = applicationPermissionRepository;
        this.groupUserRepository = groupUserRepository;
    }

    @Transactional
    public void cleanup() {
        groupUserRepository.deleteAll();
        applicationPermissionRepository.deleteAll();
        topicRepository.deleteAll();
        applicationRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }
}