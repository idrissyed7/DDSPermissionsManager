package io.unityfoundation.dds.permissions.manager.testing.util;

import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
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

    public DbCleanup(TopicRepository topicRepository, GroupRepository groupRepository, ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.groupRepository = groupRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void cleanup() {
        groupRepository.deleteAll();
        topicRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
    }
}