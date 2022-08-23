package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.utils.SecurityService;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class GroupService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserService userService;
    private final SecurityService securityService;
    private final GroupUserService groupUserService;
    private final TopicRepository topicRepository;


    public GroupService(UserRepository userRepository, GroupRepository groupRepository, UserService userService,
                        SecurityService securityService, GroupUserService groupUserService, TopicRepository topicRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.userService = userService;
        this.securityService = securityService;
        this.groupUserService = groupUserService;
        this.topicRepository = topicRepository;
    }

    public Page<Group> findAll(Pageable pageable) {
        Authentication authentication = securityService.getAuthentication().get();

        if (userService.isCurrentUserAdmin()) {
            return groupRepository.findAll(pageable);
        } else {
            String userEmail = authentication.getName();
            User user = userService.getUserByEmail(userEmail).get();
            List<Long> groupsList = groupUserService.getAllGroupsUserIsAMemberOf(user.getId());
            return groupRepository.findAllByIdIn(groupsList, pageable);
        }
    }

    public MutableHttpResponse<Group> save(Group group) throws Exception {
        if (!userService.isCurrentUserAdmin()) {
            throw new AuthenticationException("Not authorized");
        }

        Optional<Group> searchGroupByName = groupRepository.findByName(group.getName());

        if (group.getId() == null) {
            if (searchGroupByName.isPresent()) {
                return HttpResponseFactory.INSTANCE.status(HttpStatus.SEE_OTHER, searchGroupByName.get());
            }
            return HttpResponse.ok(groupRepository.save(group));
        } else {
            if (searchGroupByName.isPresent()) {
                throw new Exception("Group with same name already exists");
            }
            return HttpResponse.ok(groupRepository.update(group));
        }
    }

    public void deleteById(Long id) {
        if (!userService.isCurrentUserAdmin()) {
            throw new AuthenticationException("Not authorized");
        }

        groupRepository.deleteById(id);
    }

    @Transactional
    public boolean addMember(@Body Long groupId, @Body Long candidateId, Map userRolesMap) {
        Optional<Group> groupOptional = groupRepository.findById(groupId);
        Optional<User> userOptional = userRepository.findById(candidateId);
        if (groupOptional.isEmpty() || userOptional.isEmpty()) {
            return false;
        }
        Group group = groupOptional.get();
        User user = userOptional.get();

        // ignore duplicate add attempt
        if (groupUserService.isUserMemberOfGroup(group.getId(), user.getId())) {
            return true;
        }

        GroupUser groupUser = new GroupUser(group, user);
        if (userRolesMap != null) {
            groupUser.setGroupAdmin(Optional.ofNullable((Boolean) userRolesMap.get("isGroupAdmin")).orElse(false));
            groupUser.setTopicAdmin(Optional.ofNullable((Boolean) userRolesMap.get("isTopicAdmin")).orElse(false));
            groupUser.setApplicationAdmin(Optional.ofNullable((Boolean) userRolesMap.get("isApplicationAdmin")).orElse(false));
        }
        groupUserService.save(groupUser);

        return true;
    }

    public Optional<Map> getGroupDetails(Long id) {
        Optional<Group> groupOptional = groupRepository.findById(id);
        if (groupOptional.isPresent()) {
            Group group = groupOptional.get();
            return Optional.of(Map.of("group", group));
        }
        return Optional.empty();
    }

    public boolean removeMember(Long groupId, Long memberId) {
        Optional<Group> byId = groupRepository.findById(groupId);
        if (byId.isEmpty()) {
            return false;
        }

        groupUserService.removeMemberFromGroup(groupId, memberId);

        return true;
    }

    @Transactional
    public boolean addTopic(@Body Long groupId, @Body Long topicId) throws Exception {
        Optional<Group> groupOptional = groupRepository.findById(groupId);
        Optional<Topic> topicOptional = topicRepository.findById(topicId);
        if (groupOptional.isEmpty() || topicOptional.isEmpty()) {
            return false;
        }

        Group group = groupOptional.get();
        Topic topic = topicOptional.get();

        // if a topic in the group with the same name exists, throw an exception
        boolean topicExistsInGroup = group.getTopics().stream().anyMatch(groupTopic -> groupTopic.getName().equals(topic.getName()));

        if (topicExistsInGroup) {
            throw new Exception("Topic " + topic.getName() + " already exists in Group " + group.getName() + ".");
        }

        topic.setPermissionsGroup(groupId);
        topicRepository.update(topic);
        group.addTopic(topic);
        groupRepository.update(group);
        return true;
    }

    @Transactional
    public boolean removeTopic(Long groupId, Long topicId) {
        Optional<Group> byId = groupRepository.findById(groupId);
        if (byId.isEmpty()) {
            return false;
        }
        Group group = byId.get();
        group.removeTopic(topicId);
        groupRepository.update(group);
        return true;
    }

    public boolean isAdminOrGroupAdmin(Long groupId) {

        Optional<Group> group = groupRepository.findById(groupId);

        if (group.isEmpty()) {
            return false;
        }

        Authentication authentication = securityService.getAuthentication().get();
        String userEmail = authentication.getName();
        Long userId = userService.getUserByEmail(userEmail).get().getId();

        boolean isGroupAdmin = groupUserService.isUserGroupAdminOfGroup(group.get().getId(), userId);

        return userService.isCurrentUserAdmin() || isGroupAdmin;
    }

    public List<Map> getGroupMembers(Long groupId) {
        List<GroupUser> groupUsers = groupUserService.getUsersOfGroup(groupId);
        List<Map> result = new ArrayList<>();
        for (GroupUser groupUser : groupUsers) {
            result.add(Map.of("member", groupUser.getPermissionsUser(),
                    "permissions", groupUser));
        }
        return result;
    }

    public List<Map<String, Object>> getGroupsUserIsAMemberOf(Long userId) {
        if (!userService.isCurrentUserAdmin()) {
            throw new AuthenticationException("Not authorized");
        }

        return groupUserService.getAllPermissionsPerGroupUserIsMemberOf(userId);
    }
}
