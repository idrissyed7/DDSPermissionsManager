package io.unityfoundation.dds.permissions.manager.model.user;

import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class UserService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public UserService(UserRepository userRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional
    public void deleteById(Long id) {
        removeUserFromGroups(id);
        userRepository.deleteById(id);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void removeUserFromGroups(Long userId) {
        // TODO fix this...
        groupRepository.findAll().forEach(group -> {
            if (group.removeUser(userId)) {
                groupRepository.update(group);
            }
        });
    }

    public Iterable<User> listUsersNotInGroup(Group group) {
        List<Long> ids = group.getUsers().stream().map(User::getId).collect(Collectors.toList());
        return userRepository.findAllByIdNotInList(ids);
    }
}
