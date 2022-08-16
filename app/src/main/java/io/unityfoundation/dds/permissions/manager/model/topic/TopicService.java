package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.utils.SecurityService;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class TopicService {

    private final TopicRepository topicRepository;
    private final SecurityService securityService;
    private final UserService userService;
    private final GroupUserService groupUserService;
    private final GroupRepository groupRepository;

    public TopicService(TopicRepository topicRepository, SecurityService securityService, UserService userService, GroupUserService groupUserService, GroupRepository groupRepository) {
        this.topicRepository = topicRepository;
        this.securityService = securityService;
        this.userService = userService;
        this.groupUserService = groupUserService;
        this.groupRepository = groupRepository;
    }

    public Page<Topic> findAll(Pageable pageable) {
        Authentication authentication = securityService.getAuthentication().get();

        if (isCurrentUserAdmin()) {
            return topicRepository.findAll(pageable);
        } else {
            String userEmail = authentication.getName();
            User user = userService.getUserByEmail(userEmail).get();
            List<Long> groups = groupUserService.getAllGroupsUserIsAMemberOf(user.getId());
            return topicRepository.findAllByPermissionsGroupIn(groups, pageable);
        }
    }

    public MutableHttpResponse save(Topic topic) throws Exception {
        if (topic.getId() != null) {
            throw new Exception("Update of Topics are not allowed.");
        } else if (!isCurrentUserAdmin() && !isUserTopicAdminOfGroup(topic)) {
            throw new AuthenticationException("Not authorized");
        }

        return HttpResponse.ok(topicRepository.save(topic));
    }

    private boolean isUserTopicAdminOfGroup(Topic topic) throws Exception {
        Long topicGroupId = topic.getPermissionsGroup();
        if (topicGroupId == null) {
            return false;
        } else {
            if (groupRepository.findById(topicGroupId).isEmpty()) {
                throw new Exception("Specified group does not exist.");
            }
            Authentication authentication = securityService.getAuthentication().get();
            String userEmail = authentication.getName();
            User user = userService.getUserByEmail(userEmail).get();
            return groupUserService.isUserTopicAdminOfGroup(topicGroupId, user.getId());
        }
    }

    public void deleteById(Long id) throws Exception {
        Optional<Topic> topic = topicRepository.findById(id);
        if (topic.isEmpty()) {
            throw new Exception("Topic not found");
        }
        if (!isCurrentUserAdmin() && !isUserTopicAdminOfGroup(topic.get())) {
            throw new AuthenticationException("Not authorized");
        }

        topicRepository.deleteById(id);
    }

    public boolean isCurrentUserAdmin() {
        Authentication authentication = securityService.getAuthentication().get();
        return Optional.of((Boolean) authentication.getAttributes().get("isAdmin")).orElse(false);
    }
}
