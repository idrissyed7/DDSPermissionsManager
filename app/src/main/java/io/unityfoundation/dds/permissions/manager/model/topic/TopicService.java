package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.authentication.AuthenticationException;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionService;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Singleton
public class TopicService {

    private final TopicRepository topicRepository;
    private final SecurityUtil securityUtil;
    private final GroupUserService groupUserService;
    private final GroupRepository groupRepository;
    private final ApplicationPermissionService applicationPermissionService;

    public TopicService(TopicRepository topicRepository, SecurityUtil securityUtil, GroupUserService groupUserService, GroupRepository groupRepository, ApplicationPermissionService applicationPermissionService) {
        this.topicRepository = topicRepository;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
        this.groupRepository = groupRepository;
        this.applicationPermissionService = applicationPermissionService;
    }

    public Page<TopicDTO> findAll(Pageable pageable, String filter) {
        return getTopicPage(pageable, filter).map(TopicDTO::new);
    }

    private Page<Topic> getTopicPage(Pageable pageable, String filter) {

        if(!pageable.isSorted()) {
            pageable = pageable.order("name").order("permissionsGroup.name");
        }

        if (securityUtil.isCurrentUserAdmin()) {
            if (filter == null) {
                return topicRepository.findAll(pageable);
            }
            return topicRepository.findAllByNameContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(filter, filter, pageable);
        } else {
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            List<Long> groups = groupUserService.getAllGroupsUserIsAMemberOf(user.getId());

            if (filter == null) {
                return topicRepository.findAllByPermissionsGroupIdIn(groups, pageable);
            }

            List<Long> all = topicRepository.findIdByNameContainsIgnoreCaseOrPermissionsGroupNameContainsIgnoreCase(filter, filter);

            return topicRepository.findAllByIdInAndPermissionsGroupIdIn(all, groups, pageable);
        }
    }

    public MutableHttpResponse<?> save(TopicDTO topicDTO) {

        Optional<Group> groupOptional = groupRepository.findById(topicDTO.getGroup());

        if (groupOptional.isEmpty()) {
            return HttpResponse.notFound("Specified group does not exist");
        }

        Topic topic = new Topic(topicDTO.getName(), topicDTO.getKind());
        Group group = groupOptional.get();
        topic.setPermissionsGroup(group);

        if (!securityUtil.isCurrentUserAdmin() && !isUserTopicAdminOfGroup(topic)) {
            return HttpResponse.unauthorized();
        }

        Optional<Topic> searchTopicByNameAndGroup = topicRepository.findByNameAndPermissionsGroup(
                topic.getName().trim(), topic.getPermissionsGroup());

        if (searchTopicByNameAndGroup.isPresent()) {
            return HttpResponseFactory.INSTANCE.status(HttpStatus.SEE_OTHER, new TopicDTO(searchTopicByNameAndGroup.get()));
        }
        TopicDTO responseTopicDTO = new TopicDTO(topicRepository.save(topic));
        responseTopicDTO.setCanonicalName(computeCanonicalName(responseTopicDTO));
        return HttpResponse.ok(responseTopicDTO);
    }

    public HttpResponse deleteById(Long id) throws AuthenticationException {
        Optional<Topic> optionalTopic = topicRepository.findById(id);
        if (optionalTopic.isEmpty()) {
            return HttpResponse.notFound("Topic not found");
        }

        Topic topic = optionalTopic.get();
        if (!securityUtil.isCurrentUserAdmin() && !isUserTopicAdminOfGroup(topic)) {
            return HttpResponse.unauthorized();
        }

        // TODO - Need to investigate cascade management to eliminate this
        applicationPermissionService.deleteAllByTopic(topic);

        topicRepository.deleteById(id);
        return HttpResponse.seeOther(URI.create("/api/topics"));
    }

    public HttpResponse show(Long id) {
        Optional<Topic> topicOptional = topicRepository.findById(id);
        if (topicOptional.isEmpty()) {
            return HttpResponse.notFound();
        }

        Topic topic = topicOptional.get();
        TopicDTO topicResponseDTO = new TopicDTO(topic);
        topicResponseDTO.setCanonicalName(computeCanonicalName(topicResponseDTO));
        if (!securityUtil.isCurrentUserAdmin() && !isMemberOfTopicGroup(topic.getPermissionsGroup())) {
            return HttpResponse.unauthorized();
        }

        return HttpResponse.ok(topicResponseDTO);
    }

    private boolean isMemberOfTopicGroup(Group group) {
        return groupUserService.isUserMemberOfGroup(group.getId(), securityUtil.getCurrentlyAuthenticatedUser().get().getId());
    }

    private boolean isUserTopicAdminOfGroup(Topic topic) {
        User user = securityUtil.getCurrentlyAuthenticatedUser().get();
        return groupUserService.isUserTopicAdminOfGroup(topic.getPermissionsGroup().getId(), user.getId());
    }

    private String computeCanonicalName(Topic topic) {
        return buildCanonicalName(topic.getKind(), topic.getPermissionsGroup().getId(), topic.getName());
    }

    private String computeCanonicalName(TopicDTO topicDTO) {
        return buildCanonicalName(topicDTO.getKind(), topicDTO.getGroup(), topicDTO.getName());
    }

    private String buildCanonicalName(TopicKind kind, Long groupId, String name) {
        return kind + "." + groupId + "." + name;
    }

    public Optional<Topic> findById(Long topicId) {
        return topicRepository.findById(topicId);
    }
}
