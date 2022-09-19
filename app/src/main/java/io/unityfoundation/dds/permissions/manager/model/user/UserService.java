package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
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

    public Page<User> findAll(Pageable pageable, String filter) {
        if (!pageable.isSorted()) {
            pageable = pageable.order(Sort.Order.asc("email"));
        }

        if (filter == null) {
            return userRepository.findByAdminTrue(pageable);
        }

        return userRepository.findByAdminTrueAndEmailContainsIgnoreCase(filter, pageable);
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
    public User save(AdminDTO adminDTO) throws Exception {

        if (adminDTO.getId() == null) {
            Optional<User> userSearchByEmail = userRepository.findByEmail(adminDTO.getEmail());
            if (userSearchByEmail.isPresent()) {
                throw new Exception("User with same email already exists.");
            }

            return userRepository.save(generateAdminFromDTO(adminDTO));
        } else {
            Optional<User> existingUserOptional = userRepository.findById(adminDTO.getId());
            if (existingUserOptional.isEmpty()) {
                throw new Exception("User with given id does not exist.");
            }

            return userRepository.update(generateAdminFromDTO(adminDTO));
        }
    }

    private User generateAdminFromDTO(AdminDTO adminDTO) {
        User user = new User();
        user.setId(adminDTO.getId());
        user.setEmail(adminDTO.getEmail());
        user.setAdmin(true);
        return user;
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
