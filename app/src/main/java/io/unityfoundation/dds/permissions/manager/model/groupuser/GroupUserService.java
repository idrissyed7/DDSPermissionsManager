package io.unityfoundation.dds.permissions.manager.model.groupuser;

import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class GroupUserService {

    private final GroupUserRepository groupUserRepository;


    public GroupUserService(GroupUserRepository groupUserRepository) {
        this.groupUserRepository = groupUserRepository;
    }

    public void removeUserFromAllGroups(Long userId) {
        groupUserRepository.deleteAllByPermissionsUser(userId);
    }

    public boolean isUserGroupAdminOfGroup(Long groupId, Long userId) {
        Optional<GroupUser> groupUser = groupUserRepository.findByPermissionsGroupAndPermissionsUserAndGroupAdminTrue(groupId, userId);
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

    public Map<Long, Map> getAllPermissionsPerGroupUserIsMemberOf(Long id) {
        HashMap<Long, Map> result = new HashMap();
        List<GroupUser> groupUserList = groupUserRepository.findAllByPermissionsUser(id);
        groupUserList.forEach(groupUser -> {
          result.put(groupUser.getPermissionsGroup(), Map.of(
                  "isGroupAdmin", groupUser.isGroupAdmin(),
                  "isTopicAdmin", groupUser.isTopicAdmin(),
                  "isApplicationAdmin", groupUser.isApplicationAdmin()
          ));
        });
        return result;
    }
}
