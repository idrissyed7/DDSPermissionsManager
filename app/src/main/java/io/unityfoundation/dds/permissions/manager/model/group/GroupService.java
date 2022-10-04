package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpResponseFactory;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.authentication.AuthenticationException;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.security.SecurityUtil;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class GroupService {

    private final GroupRepository groupRepository;
    private final SecurityUtil securityUtil;
    private final GroupUserService groupUserService;


    public GroupService(GroupRepository groupRepository, SecurityUtil securityUtil,
                        GroupUserService groupUserService) {
        this.groupRepository = groupRepository;
        this.securityUtil = securityUtil;
        this.groupUserService = groupUserService;
    }

    public Page<GroupDTO> findAll(Pageable pageable, String filter) {
        return getGroupPage(pageable, filter).map(group -> {
            GroupDTO groupsResponseDTO = new GroupDTO();
            groupsResponseDTO.setGroupFields(group);
            groupsResponseDTO.setTopics(group.getTopics().stream().map(Topic::getId).collect(Collectors.toSet()));
            groupsResponseDTO.setApplications(group.getApplications().stream().map(Application::getId).collect(Collectors.toSet()));
            groupsResponseDTO.setTopicCount(group.getTopics().size());
            groupsResponseDTO.setApplicationCount(group.getApplications().size());
            groupsResponseDTO.setMembershipCount(groupUserService.getMembershipCountByGroup(group));

            return groupsResponseDTO;
        });
    }

    private Page<Group> getGroupPage(Pageable pageable, String filter) {
        if (!pageable.isSorted()) {
            pageable = pageable.order(Sort.Order.asc("name"));
        }

        if (securityUtil.isCurrentUserAdmin()) {
            if (filter == null) {
                return groupRepository.findAll(pageable);
            }

            return groupRepository.findAllByNameContainsIgnoreCase(filter, pageable);
        } else {
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            List<Long> groupsList = groupUserService.getAllGroupsUserIsAMemberOf(user.getId());

            if (filter == null) {
                return groupRepository.findAllByIdIn(groupsList, pageable);
            }

            return groupRepository.findAllByIdInAndNameContainsIgnoreCase(groupsList, filter, pageable);
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

    public List<Map<String, Object>> getGroupsUserIsAMemberOf(Long userId) {
        if (!securityUtil.isCurrentUserAdmin()) {
            throw new AuthenticationException("Not authorized");
        }

        return groupUserService.getAllPermissionsPerGroupUserIsMemberOf(userId);
    }

    public Page<GroupSearchDTO> search(String filter, GroupAdminRole role, Pageable pageable) {

        return getGroupSearchPage(filter, role, pageable).map(group -> {
            GroupSearchDTO groupsResponseDTO = new GroupSearchDTO();
            groupsResponseDTO.setGroupFields(group);

            return groupsResponseDTO;
        });
    }

    private Page<Group> getGroupSearchPage(String filter, GroupAdminRole role, Pageable pageable) {
        if (!pageable.isSorted()) {
            pageable = pageable.order(Sort.Order.asc("permissionsGroup.name"));
        }

        if (securityUtil.isCurrentUserAdmin()) {
            return groupRepository.findAllByNameContainsIgnoreCase(filter, pageable);
        } else {
            // search based on context
            User user = securityUtil.getCurrentlyAuthenticatedUser().get();
            return groupUserService.getAllGroupsUserIsAnAdminOf(user, filter, pageable, role);
        }
    }
}
