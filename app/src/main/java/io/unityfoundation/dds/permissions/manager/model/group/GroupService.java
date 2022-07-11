package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.http.annotation.Body;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.Optional;

@Singleton
public class GroupService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;


    public GroupService(UserRepository userRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional
    public boolean addMember(@Body Long groupId, @Body Long candidateId) {
        Optional<Group> groupOptional = groupRepository.findById(groupId);
        Optional<User> userOptional = userRepository.findById(candidateId);
        if (groupOptional.isEmpty() || userOptional.isEmpty()) {
            return false;
        }
        Group group = groupOptional.get();
        User user = userOptional.get();
        group.addUser(user);
        groupRepository.update(group);
        return true;
    }

}
