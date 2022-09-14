package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.security.authentication.AuthenticationException;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class GroupService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final SecurityUtil securityUtil;
    private final GroupUserService groupUserService;
    private final TopicRepository topicRepository;


    public GroupService(UserRepository userRepository, GroupRepository groupRepository, SecurityUtil securityUtil,
                        GroupUserService groupUserService, TopicRepository topicRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
        this.topicRepository = topicRepository;
    }

    public Page<GroupResponseDTO> findAll(Pageable pageable) {
        return getGroupPage(pageable).map(group -> {
            GroupResponseDTO groupsResponseDTO = new GroupResponseDTO();
            groupsResponseDTO.setGroupFields(group);
            groupsResponseDTO.setTopicCount(group.getTopics().size());
            groupsResponseDTO.setApplicationCount(group.getApplications().size());
            groupsResponseDTO.setMembershipCount(groupUserService.getMembershipCountByGroup(group));

            return groupsResponseDTO;
        });
    }

    private Page<Group> getGroupPage(Pageable pageable) {
        if (!pageable.isSorted()) {
            pageable = pageable.order(Sort.Order.asc("name"));
        }

        if (securityUtil.isCurrentUserAdmin()) {
            return groupRepository.findAll(pageable);
        } else {
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            List<Long> groupsList = groupUserService.getAllGroupsUserIsAMemberOf(user.getId());
            return groupRepository.findAllByIdIn(groupsList, pageable);
        }
    }

    public MutableHttpResponse<Group> save(Group group) throws Exception {
        if (!securityUtil.isCurrentUserAdmin()) {
            throw new AuthenticationException("Not authorized");
        }

        Optional<Group> searchGroupByName = groupRepository.findByName(group.getName().trim());

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
        if (!securityUtil.isCurrentUserAdmin()) {
            throw new AuthenticationException("Not authorized");
        }

        groupRepository.deleteById(id);
    }

    public Optional<Map> getGroupDetails(Long id) {
        Optional<Group> groupOptional = groupRepository.findById(id);
        if (groupOptional.isPresent()) {
            Group group = groupOptional.get();
            return Optional.of(Map.of("group", group));
        }
        return Optional.empty();
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

    public List<Map<String, Object>> getGroupsUserIsAMemberOf(Long userId) {
        if (!securityUtil.isCurrentUserAdmin()) {
            throw new AuthenticationException("Not authorized");
        }

        return groupUserService.getAllPermissionsPerGroupUserIsMemberOf(userId);
    }

    public List<Group> searchByNameContains(String searchText) {
        return groupRepository.findTop10ByNameContainsIgnoreCase(searchText);
    }
}
