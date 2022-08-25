package io.unityfoundation.dds.permissions.manager.model.groupuser;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
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

    public Optional<GroupUser> findById(Long id) {
        return groupUserRepository.findById(id);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void removeUserFromAllGroups(Long userId) {
        groupUserRepository.deleteAllByPermissionsUser(userId);
    }

    public boolean isUserGroupAdminOfGroup(Long groupId, Long userId) {
        int groupUserCount = groupUserRepository.countByPermissionsGroupAndPermissionsUserAndGroupAdminTrue(groupId, userId);
        return groupUserCount > 0;
    }

    public boolean isUserTopicAdminOfGroup(Long groupId, Long userId) {
        int groupUserCount =  groupUserRepository.countByPermissionsGroupAndPermissionsUserAndTopicAdminTrue(groupId, userId);
        return groupUserCount > 0;
    }

    public boolean isUserApplicationAdminOfGroup(Long groupId, Long userId) {
        int groupUserCount = groupUserRepository.countByPermissionsGroupAndPermissionsUserAndApplicationAdminTrue(groupId, userId);
        return groupUserCount > 0;
    }

    public List<Long> getAllGroupsUserIsAMemberOf(Long userId) {
        return groupUserRepository.findAllByPermissionsUser(userId).stream().map(GroupUser::getPermissionsGroup).collect(Collectors.toList());
    }

    public boolean isUserMemberOfGroup(Long groupId, Long userId) {
        return groupUserRepository.findByPermissionsGroupAndPermissionsUser(groupId, userId).isPresent();
    }

    public List<GroupUser> getUsersOfGroup(Long groupId) {
        return groupUserRepository.findAllByPermissionsGroup(groupId);
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
                return HttpResponse.ok(saveFromDTO(userOptional.get(), groupUserDTO));
            }
        } else {
            User newUser = userRepository.save(new User(groupUserDTO.getEmail()));
            return HttpResponse.ok(saveFromDTO(newUser, groupUserDTO));
        }
    }

    @Transactional
    public MutableHttpResponse<?> updateMember(@Body GroupUser groupUser) {
        if (groupUser.getId() == null) {
            return HttpResponse.badRequest("Please use save endpoint instead");
        }
        return HttpResponse.ok(groupUserRepository.update(groupUser));
    }

    private GroupUser saveFromDTO(User user, GroupUserDTO groupUserDTO) {
        GroupUser groupUser = new GroupUser(groupUserDTO.getPermissionsGroup(), user.getId());
        groupUser.setGroupAdmin(groupUserDTO.isGroupAdmin());
        groupUser.setTopicAdmin(groupUserDTO.isTopicAdmin());
        groupUser.setApplicationAdmin(groupUserDTO.isApplicationAdmin());

        return groupUserRepository.save(groupUser);
    }

    public boolean removeMember(Long id) {
        groupUserRepository.deleteById(id);

        return true;
    }

    public List<Map<String, Object>> getAllPermissionsPerGroupUserIsMemberOf(Long id) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<GroupUser> groupUserList = groupUserRepository.findAllByPermissionsUser(id);
        groupUserList.forEach(groupUser -> {
            Optional<Group> optionalGroup = groupRepository.findById(groupUser.getPermissionsGroup());
            optionalGroup.ifPresent(group -> result.add(
                    Map.of(
                            "groupId", group.getId(),
                            "groupName", group.getName(),
                            "isGroupAdmin", groupUser.isGroupAdmin(),
                            "isTopicAdmin", groupUser.isTopicAdmin(),
                            "isApplicationAdmin", groupUser.isApplicationAdmin()
                    )
            ));
        });

        return result;
    }
}
