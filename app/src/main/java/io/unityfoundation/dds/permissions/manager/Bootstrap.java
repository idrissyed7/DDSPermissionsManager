package io.unityfoundation.dds.permissions.manager;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.user.Role;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.List;

@Singleton
public class Bootstrap {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public Bootstrap(UserRepository userRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @EventListener
    public void devData(ServerStartupEvent event) {
        User justin = userRepository
                .save(new User("Justin", "Wilson", "jwilson@test.test", List.of(Role.ADMIN)));
        User kevin = userRepository
                .save(new User("Kevin", "Stanley", "kstanley@test.test", List.of(Role.ADMIN)));
        userRepository.save(new User("Jeff", "Brown", "jeff@test.test", List.of(Role.ADMIN)));
        userRepository.save(new User("Julian", "Gracia", "jgracia@test.test", List.of(Role.ADMIN)));
        userRepository.save(new User("Daniel", "Bellone", "belloned@test.test", List.of(Role.ADMIN)));
        userRepository.save(new User("Max", "Montes", "montesm@test.test", List.of(Role.ADMIN)));

        Group alphaGroup = groupRepository.save(new Group("Alpha"));
        alphaGroup.setUsers(Arrays.asList(justin, kevin));
        alphaGroup.setAdmins(List.of(justin));
        groupRepository.update(alphaGroup);

        groupRepository.save(new Group("Beta"));
        groupRepository.save(new Group("Gamma"));
        groupRepository.save(new Group("Delta"));
    }
}
