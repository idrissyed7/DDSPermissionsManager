package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Body;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.Map;
import java.util.Optional;

@Singleton
public class GroupService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final GroupRepository groupRepository;


    public GroupService(UserRepository userRepository, GroupRepository groupRepository, UserService userService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.userService = userService;
    }

    public Page<Group> findAll(Pageable pageable) {
        return groupRepository.findAll(pageable);
    }

    public void save(Group group) {
        if (group.getId() == null) {
            groupRepository.save(group);
        } else {
            groupRepository.update(group);
        }
    }

    public void deleteById(Long id) {
        groupRepository.deleteById(id);
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

    public Optional<Map> getGroupAndCandidates(Long id) {
        Optional<Group> groupOptional = groupRepository.findById(id);
        if (groupOptional.isPresent()) {
            Group group = groupOptional.get();
            Iterable<User> candidateUsers = userService.listUsersNotInGroup(group);
            return Optional.of(Map.of("group", group, "candidateUsers", candidateUsers));
        }
        return Optional.empty();
    }

    public boolean removeMember(Long groupId, Long memberId) {
        Optional<Group> byId = groupRepository.findById(groupId);
        if (byId.isEmpty()) {
            return false;
        }
        Group group = byId.get();
        group.removeUser(memberId);
        groupRepository.update(group);

        return true;
    }
}
