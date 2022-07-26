package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
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
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void save(User user) {
        if (user.getId() == null) {
            userRepository.save(user);
        } else {
            userRepository.update(user);
        }
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
