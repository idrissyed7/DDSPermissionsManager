package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.Optional;

@Singleton
public class UserService {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupUserService groupUserService;

    public UserService(UserRepository userRepository, GroupRepository groupRepository, GroupUserService groupUserService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupUserService = groupUserService;
    }

    @Transactional
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void save(User user) throws Exception {

        if (user.getId() == null) {
            Optional<User> userSearchByEmail = userRepository.findByEmail(user.getEmail());
            if (userSearchByEmail.isPresent()) {
                throw new Exception("User with same email already exists");
            }

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
        groupUserService.removeUserFromAllGroups(userId);
    }
}
