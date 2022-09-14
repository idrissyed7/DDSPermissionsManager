package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class UserService {
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final GroupUserService groupUserService;

    public UserService(SecurityUtil securityUtil, UserRepository userRepository, GroupUserService groupUserService) {
        this.securityUtil = securityUtil;
        this.userRepository = userRepository;
        this.groupUserService = groupUserService;
    }

    @Transactional
    @NonNull
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Page<User> findAll(Pageable pageable) {
        if (securityUtil.isCurrentUserAdmin()) {
            return userRepository.findAll(pageable);
        }
        List<Long> userIds = getIdsOfUsersWhoShareGroupsWithCurrentUser();
        return userRepository.findAllByIdIn(userIds, pageable);
    }

    public List<Long> getIdsOfUsersWhoShareGroupsWithCurrentUser() {
        return securityUtil.getCurrentlyAuthenticatedUser()
                .map(user -> {
                    Long currentUserId = user.getId();
                    List<Long> currentUsersGroupIds = groupUserService.getAllGroupsUserIsAMemberOf(currentUserId);
                    return currentUsersGroupIds.stream()
                            .map(groupUserService::getUsersOfGroup)
                            .flatMap(List::stream)
                            .map(GroupUser::getPermissionsUser)
                            .map(User::getId)
                            .collect(Collectors.toList());
                }).orElseGet(Collections::emptyList);
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
