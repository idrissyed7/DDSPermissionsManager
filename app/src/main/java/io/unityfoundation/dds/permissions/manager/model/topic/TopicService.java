package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.authentication.AuthenticationException;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class TopicService {

    private final TopicRepository topicRepository;
    private final SecurityUtil securityUtil;
    private final GroupUserService groupUserService;
    private final GroupRepository groupRepository;

    public TopicService(TopicRepository topicRepository, SecurityUtil securityUtil, GroupUserService groupUserService, GroupRepository groupRepository) {
        this.topicRepository = topicRepository;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
        this.groupRepository = groupRepository;
    }

    public Page<Topic> findAll(Pageable pageable) {

        if (securityUtil.isCurrentUserAdmin()) {
            return topicRepository.findAll(pageable);
        } else {
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            List<Long> groups = groupUserService.getAllGroupsUserIsAMemberOf(user.getId());
            return topicRepository.findAllByPermissionsGroupIn(groups, pageable);
        }
    }

    public MutableHttpResponse save(Topic topic) throws Exception {
        if (topic.getId() != null) {
            throw new Exception("Update of Topics are not allowed.");
        } else if (!securityUtil.isCurrentUserAdmin() && !isUserTopicAdminOfGroup(topic)) {
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

            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            return groupUserService.isUserTopicAdminOfGroup(topicGroupId, user.getId());
        }
    }

    public void deleteById(Long id) throws Exception {
        Optional<Topic> topic = topicRepository.findById(id);
        if (topic.isEmpty()) {
            throw new Exception("Topic not found");
        }
        if (!securityUtil.isCurrentUserAdmin() && !isUserTopicAdminOfGroup(topic.get())) {
            throw new AuthenticationException("Not authorized");
        }

        topicRepository.deleteById(id);
    }
}
