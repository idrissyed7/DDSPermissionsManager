package io.unityfoundation.dds.permissions.manager;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import jakarta.inject.Singleton;

@Singleton
public class Bootstrap {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;

    public Bootstrap(UserRepository userRepository, GroupRepository groupRepository,
            GroupUserRepository groupUserRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupUserRepository = groupUserRepository;
    }

    @EventListener
    public void devData(ServerStartupEvent event) {
        User justin = userRepository.save(new User("jwilson@test.test", true));
        User kevin = userRepository.save(new User("kstanley@test.test"));
        User max = userRepository.save(new User("montesm@test.test"));
        userRepository.save(new User("jeff@test.test"));
        userRepository.save(new User("jgracia@test.test"));
        userRepository.save(new User("belloned@test.test", true));

        Group alphaGroup = groupRepository.save(new Group("Alpha"));

        GroupUser alphaJustin = new GroupUser(alphaGroup, justin);
        GroupUser alphaKevin = new GroupUser(alphaGroup, kevin);
        GroupUser alphaMax = new GroupUser(alphaGroup, max);

        Topic topic = new Topic("TestTopic123", TopicKind.B, alphaGroup.getId());
        Application application = new Application("TestApplication123", alphaGroup.getId());
        alphaGroup.addTopic(topic);
        alphaGroup.addApplication(application);
        groupRepository.update(alphaGroup);

        groupUserRepository.save(alphaJustin);
        groupUserRepository.save(alphaKevin);
        groupUserRepository.save(alphaMax);

        groupRepository.save(new Group("Beta"));
        groupRepository.save(new Group("Gamma"));
        groupRepository.save(new Group("Delta"));
    }
}
