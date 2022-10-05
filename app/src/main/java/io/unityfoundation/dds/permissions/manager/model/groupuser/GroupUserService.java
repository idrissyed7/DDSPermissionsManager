package io.unityfoundation.dds.permissions.manager.model.groupuser;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupAdminRole;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class GroupUserService {

    private final GroupUserRepository groupUserRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;


    public GroupUserService(GroupUserRepository groupUserRepository, GroupRepository groupRepository, UserRepository userRepository, SecurityUtil securityUtil) {
        this.groupUserRepository = groupUserRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.securityUtil = securityUtil;
    }

    public Page<GroupUserResponseDTO> findAll(Pageable pageable, String filter) {
        return getGroupMembers(pageable, filter).map(GroupUserResponseDTO::new);
    }

    private Page<GroupUser> getGroupMembers(Pageable pageable, String filter) {
        if(!pageable.isSorted()) {
            pageable = pageable.order("permissionsUser.email").order("permissionsGroup.name");
        }
        if (securityUtil.isCurrentUserAdmin()) {
            if (filter == null) {
                return groupUserRepository.findAll(pageable);
            } else {
                return groupUserRepository.findAllByPermissionsGroupNameContainsOrPermissionsUserEmailContains(filter, filter, pageable);
            }
        } else {
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            List<Long> groupsList = getAllGroupsUserIsAMemberOf(user.getId());

            if (filter == null) {
                return groupUserRepository.findAllByPermissionsGroupIdIn(groupsList, pageable);
            } else {
                List<Long> all = groupUserRepository.findIdByPermissionsGroupNameContainsOrPermissionsUserEmailContains(filter, filter);

                return groupUserRepository.findAllByIdInAndPermissionsGroupIdIn(all, groupsList, pageable);

                // more efficient if Or-And query worked
//                return groupUserRepository.findAllByPermissionsGroupNameContainsOrPermissionsUserEmailContainsAndPermissionsGroupIdIn(filter, filter, groupsList, pageable);
            }
        }
    }

    public Optional<GroupUser> findById(Long id) {
        return groupUserRepository.findById(id);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void removeUserFromAllGroups(Long userId) {
        groupUserRepository.deleteAllByPermissionsUserId(userId);
    }

    public boolean isUserGroupAdminOfGroup(Long groupId, Long userId) {
        int groupUserCount = groupUserRepository.countByPermissionsGroupIdAndPermissionsUserIdAndGroupAdminTrue(groupId, userId);
        return groupUserCount > 0;
    }

    public boolean isUserTopicAdminOfGroup(Long groupId, Long userId) {
        int groupUserCount =  groupUserRepository.countByPermissionsGroupIdAndPermissionsUserIdAndTopicAdminTrue(groupId, userId);
        return groupUserCount > 0;
    }

    public boolean isUserApplicationAdminOfGroup(Long groupId, Long userId) {
        int groupUserCount = groupUserRepository.countByPermissionsGroupIdAndPermissionsUserIdAndApplicationAdminTrue(groupId, userId);
        return groupUserCount > 0;
    }

    public List<Long> getAllGroupsUserIsAMemberOf(Long userId) {
        return groupUserRepository.findAllByPermissionsUserId(userId).stream().map(GroupUser::getPermissionsGroup).map(Group::getId).collect(Collectors.toList());
    }

    public boolean isUserMemberOfGroup(Long groupId, Long userId) {
        return groupUserRepository.findByPermissionsGroupIdAndPermissionsUserId(groupId, userId).isPresent();
    }

    public List<GroupUser> getUsersOfGroup(Long groupId) {
        return groupUserRepository.findAllByPermissionsGroupId(groupId);
    }

    public int countMembershipsByUserId(Long id) {
        return groupUserRepository.countByPermissionsUserId(id);
    }

    public boolean isAdminOrGroupAdmin(Long groupId) {
        Optional<Group> group = groupRepository.findById(groupId);
        Optional<User> user = securityUtil.getCurrentlyAuthenticatedUser();

        if (group.isEmpty() || user.isEmpty()) {
            return false;
        }

        return user.get().isAdmin() || isUserGroupAdminOfGroup(group.get().getId(), user.get().getId());
    }

    @Transactional
    public MutableHttpResponse<?> addMember(@Body GroupUserDTO groupUserDTO) {
        Optional<Group> groupOptional = groupRepository.findById(groupUserDTO.getPermissionsGroup());
        if (groupOptional.isEmpty()) {
            return HttpResponse.notFound("Specified group not found");
        }

        Optional<User> userOptional = userRepository.findByEmail(groupUserDTO.getEmail());
        if (userOptional.isPresent()) {
            if (isUserMemberOfGroup(groupUserDTO.getPermissionsGroup(), userOptional.get().getId())) {
                return HttpResponse.badRequest("Please use update endpoint");
            } else {
                return HttpResponse.ok(new GroupUserResponseDTO(saveFromDTO(userOptional.get(), groupUserDTO)));
            }
        } else {
            User newUser = userRepository.save(new User(groupUserDTO.getEmail()));
            return HttpResponse.ok(new GroupUserResponseDTO(saveFromDTO(newUser, groupUserDTO)));
        }
    }

    @Transactional
    public MutableHttpResponse<?> updateMember(@Body GroupUserDTO groupUser) {
        if (groupUser.getId() == null) {
            return HttpResponse.badRequest("Please use save endpoint instead");
        }

        if (!isAdminOrGroupAdmin(groupUser.getPermissionsGroup())) {
            return HttpResponse.unauthorized();
        }

        Optional<GroupUser> groupUserOptional = groupUserRepository.findById(groupUser.getId());
        if (groupUserOptional.isEmpty()) {
            return HttpResponse.notFound("Member not found");
        }

        return HttpResponse.ok(new GroupUserResponseDTO(updateFromDTO(groupUserOptional.get(), groupUser)));
    }

    private GroupUser saveFromDTO(User user, GroupUserDTO groupUserDTO) {
        GroupUser groupUser = new GroupUser(groupRepository.findById(groupUserDTO.getPermissionsGroup()).get(), user);
        groupUser.setGroupAdmin(groupUserDTO.isGroupAdmin());
        groupUser.setTopicAdmin(groupUserDTO.isTopicAdmin());
        groupUser.setApplicationAdmin(groupUserDTO.isApplicationAdmin());

        return groupUserRepository.save(groupUser);
    }
    private GroupUser updateFromDTO(GroupUser groupUser, GroupUserDTO groupUserDTO) {
        groupUser.setGroupAdmin(groupUserDTO.isGroupAdmin());
        groupUser.setTopicAdmin(groupUserDTO.isTopicAdmin());
        groupUser.setApplicationAdmin(groupUserDTO.isApplicationAdmin());

        return groupUserRepository.update(groupUser);
    }

    public boolean removeMember(Long id) {
        Optional<GroupUser> groupUserOptional = groupUserRepository.findById(id);

        if (groupUserOptional.isEmpty()) {
            return true;
        }

        User user = groupUserOptional.get().getPermissionsUser();
        groupUserRepository.deleteById(id);

        int countByPermissionsUser = groupUserRepository.countByPermissionsUserId(user.getId());
        if (!user.isAdmin() && countByPermissionsUser == 0) {
            userRepository.delete(user);
        }

        return true;
    }

    public List<Map<String, Object>> getAllPermissionsPerGroupUserIsMemberOf(Long id) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<GroupUser> groupUserList = groupUserRepository.findAllByPermissionsUserId(id);
        groupUserList.forEach(groupUser -> {
            Group group = groupUser.getPermissionsGroup();
            if (group != null) {
                result.add(
                        Map.of(
                                "groupId", group.getId(),
                                "groupName", group.getName(),
                                "isGroupAdmin", groupUser.isGroupAdmin(),
                                "isTopicAdmin", groupUser.isTopicAdmin(),
                                "isApplicationAdmin", groupUser.isApplicationAdmin()
                        )
                );
            }
        });

        return result;
    }

    public Page<Group> getAllGroupsUserIsAnAdminOf(User user, String filter, Pageable pageable, GroupAdminRole role) {

        switch (role) {
            case GROUP_ADMIN:
                return groupUserRepository.findPermissionsGroupByPermissionsUserEqualsAndPermissionsGroupNameContainsIgnoreCaseAndGroupAdminTrue(user, filter, pageable);
            case APPLICATION_ADMIN:
                return groupUserRepository.findPermissionsGroupByPermissionsUserEqualsAndPermissionsGroupNameContainsIgnoreCaseAndApplicationAdminTrue(user, filter, pageable);
            case TOPIC_ADMIN:
                return groupUserRepository.findPermissionsGroupByPermissionsUserEqualsAndPermissionsGroupNameContainsIgnoreCaseAndTopicAdminTrue(user, filter, pageable);
        }
        return (Page<Group>) Page.EMPTY;
    }

    public int getMembershipCountByGroup(Group group) {
        return groupUserRepository.countByPermissionsGroup(group);
    }

    public HttpResponse checkUserValidity() {

        if (!securityUtil.isCurrentUserAdmin() &&
                countMembershipsByUserId(securityUtil.getCurrentlyAuthenticatedUser().get().getId()) == 0) {
            return HttpResponse.notFound();
        }

        return HttpResponse.ok();
    }
}
