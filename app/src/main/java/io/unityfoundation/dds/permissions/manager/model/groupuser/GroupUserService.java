package io.unityfoundation.dds.permissions.manager.model.groupuser;

import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class GroupUserService {

    private final GroupUserRepository groupUserRepository;
    private final GroupRepository groupRepository;


    public GroupUserService(GroupUserRepository groupUserRepository, GroupRepository groupRepository) {
        this.groupUserRepository = groupUserRepository;
        this.groupRepository = groupRepository;
    }

    public void removeUserFromAllGroups(Long userId) {
        groupUserRepository.deleteAllByPermissionsUser(userId);
    }

    public boolean isUserGroupAdminOfGroup(Long groupId, Long userId) {
        Optional<GroupUser> groupUser = groupUserRepository.findByPermissionsGroupAndPermissionsUserAndGroupAdminTrue(groupId, userId);
        return groupUser.isPresent();
    }

    public boolean isUserTopicAdminOfGroup(Long groupId, Long userId) {
        Optional<GroupUser> groupUser = groupUserRepository.findByPermissionsGroupAndPermissionsUserAndTopicAdminTrue(groupId, userId);
        return groupUser.isPresent();
    }

    public boolean isUserApplicationAdminOfGroup(Long groupId, Long userId) {
        Optional<GroupUser> groupUser = groupUserRepository.findByPermissionsGroupAndPermissionsUserAndApplicationAdminTrue(groupId, userId);
        return groupUser.isPresent();
    }

    public void removeMemberFromGroup(Long groupId, Long memberId) {
        groupUserRepository.deleteAllByPermissionsGroupAndPermissionsUser(groupId, memberId);
    }

    public GroupUser save(GroupUser groupUser) {
        if (groupUser.getId() == null) {
            return groupUserRepository.save(groupUser);
        } else {
            return groupUserRepository.update(groupUser);
        }
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
