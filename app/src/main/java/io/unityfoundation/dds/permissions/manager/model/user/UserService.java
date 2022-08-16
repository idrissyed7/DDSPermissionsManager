package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Singleton
public class UserService {
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupUserService groupUserService;

    public UserService(SecurityService securityService, UserRepository userRepository, GroupRepository groupRepository, GroupUserService groupUserService) {
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupUserService = groupUserService;
    }

    @Transactional
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * @param pageable
     * @return If the currently authenticated user is a super-admin then
     * return a list of all Users for the tiven pageable.
     * If the currently authenticated user is not a super-admin, then this
     * will return a list of Users who are members of Groups that the
     * currently authenticated user is also a member of, for the given
     * pageable.
     */
    public Page<User> findAll(Pageable pageable) {
        if (isCurrentUserAdmin()) {
            return userRepository.findAll(pageable);
        } else {
            Long userId = getCurrentlyAuthenticatedUser().getId();
            List<Long> groupsList = groupUserService.getAllGroupsUserIsAMemberOf(userId);
            return userRepository.findAllByIdIn(groupsList, pageable);
        }
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

    public boolean isCurrentUserAdmin() {
        Authentication authentication = securityService.getAuthentication().get();
        return Optional.of((Boolean) authentication.getAttributes().get("isAdmin")).orElse(false);
    }

    public User getCurrentlyAuthenticatedUser() {
        Authentication authentication = securityService.getAuthentication().get();
        String userEmail = authentication.getName();
        return getUserByEmail(userEmail).get();
    }
}
