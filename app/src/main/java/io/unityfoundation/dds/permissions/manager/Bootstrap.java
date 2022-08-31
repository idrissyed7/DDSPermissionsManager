package io.unityfoundation.dds.permissions.manager;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.AccessType;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermission;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import jakarta.inject.Singleton;

@Singleton
public class Bootstrap {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final TopicRepository topicRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationPermissionRepository applicationPermissionRepository;

    public Bootstrap(UserRepository userRepository, GroupRepository groupRepository,
            GroupUserRepository groupUserRepository, ApplicationPermissionRepository applicationPermissionRepository,
            TopicRepository topicRepository, ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupUserRepository = groupUserRepository;
        this.applicationPermissionRepository = applicationPermissionRepository;
        this.topicRepository = topicRepository;
        this.applicationRepository = applicationRepository;
    }

    @EventListener
    public void devData(ServerStartupEvent event) {
        User justin = userRepository.save(new User("Justin", "Wilson", "jwilson@test.test", true));
        User kevin = userRepository.save(new User("Kevin", "Stanley", "kstanley@test.test"));
        User max = userRepository.save(new User("Max", "Montes", "montesm@test.test"));
        userRepository.save(new User("Jeff", "Brown", "jeff@test.test"));
        userRepository.save(new User("Julian", "Gracia", "jgracia@test.test"));
        userRepository.save(new User("Justin", "Jones", "jjones@test.test"));
        User daniel = userRepository.save(new User("Daniel", "Bellone", "belloned@test.test"));

        Group alphaGroup = groupRepository.save(new Group("Alpha"));
        Group betaGroup = groupRepository.save(new Group("Beta"));

        GroupUser alphaJustin = new GroupUser(alphaGroup.getId(), justin.getId());
        GroupUser alphaKevin = new GroupUser(alphaGroup.getId(), kevin.getId());
        GroupUser alphaMax = new GroupUser(alphaGroup.getId(), max.getId());
        GroupUser alphaDaniel = new GroupUser(alphaGroup.getId(), daniel.getId());

        GroupUser betaJustin = new GroupUser(betaGroup.getId(), justin.getId());
        GroupUser betaKevin = new GroupUser(betaGroup.getId(), kevin.getId());
        GroupUser betaMax = new GroupUser(betaGroup.getId(), max.getId());
        GroupUser betaDaniel = new GroupUser(betaGroup.getId(), daniel.getId());

        groupUserRepository.save(alphaJustin);
        groupUserRepository.save(alphaKevin);
        groupUserRepository.save(alphaMax);
        groupUserRepository.save(alphaDaniel);

        groupUserRepository.save(betaJustin);
        groupUserRepository.save(betaKevin);
        groupUserRepository.save(betaMax);
        groupUserRepository.save(betaDaniel);

        Topic topic1 = topicRepository.save(new Topic("Topic1", TopicKind.B, alphaGroup.getId()));
        Topic topic2 = topicRepository.save(new Topic("Topic2", TopicKind.C, alphaGroup.getId()));
        Topic topic3 = topicRepository.save(new Topic("Topic3", TopicKind.C, alphaGroup.getId()));

        Application application1 = applicationRepository.save(new Application("ApplicationOne", alphaGroup.getId()));

        applicationPermissionRepository.save(new ApplicationPermission(application1, topic1, AccessType.READ_WRITE));
        applicationPermissionRepository.save(new ApplicationPermission(application1, topic2, AccessType.READ_WRITE));
        applicationPermissionRepository.save(new ApplicationPermission(application1, topic3, AccessType.READ_WRITE));

        // Application application2 = applicationRepository.save(new
        // Application("ApplicationTwo", betaGroup.getId()));
        // applicationPermissionRepository.save(new ApplicationPermission(application2,
        // topic2, AccessType.READ_WRITE));

        groupRepository.save(new Group("Gamma"));
        groupRepository.save(new Group("Delta"));
    }
}
