package io.unityfoundation.dds.permissions.manager.model.user;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.http.HttpResponse;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

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
    public HttpResponse save(AdminDTO adminDTO) {

        if (adminDTO.getId() == null) {
            Optional<User> userSearchByEmail = userRepository.findByEmail(adminDTO.getEmail());
            if (userSearchByEmail.isPresent()) {
                return HttpResponse.badRequest("User with same email already exists.");
            }

            User user = generateAdminFromDTO(adminDTO);
            if (user.isAdmin()) {
                LOG.info(user.getEmail() + " is now a super admin");
            } else {
                LOG.info(user.getEmail() + " is no longer a super admin");
            }
            return HttpResponse.ok(userRepository.save(user));
        } else {
            Optional<User> existingUserOptional = userRepository.findById(adminDTO.getId());
            if (existingUserOptional.isEmpty()) {
                return HttpResponse.notFound("User with given id does not exist.");
            }

            return HttpResponse.ok(userRepository.update(generateAdminFromDTO(adminDTO)));
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

    @Transactional
    public boolean removeAdminPrivilegeById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        LOG.info(user.getEmail() + " is no longer a super admin");

        if (user.isAdmin() && groupUserService.countMembershipsByUserId(id) == 0) {
            userRepository.delete(user);
        } else {
            user.setAdmin(false);
            userRepository.update(user);
        }

        return true;
    }
}
