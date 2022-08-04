package io.unityfoundation.dds.permissions.manager.model.group;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.annotation.Body;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import io.unityfoundation.dds.permissions.manager.model.user.Role;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.Map;
import java.util.Optional;

@Singleton
public class GroupService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserService userService;
    private final SecurityService securityService;


    public GroupService(UserRepository userRepository, GroupRepository groupRepository, UserService userService, SecurityService securityService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.userService = userService;
        this.securityService = securityService;
    }

    public Page<Group> findAll(Pageable pageable) {
        Authentication authentication = securityService.getAuthentication().get();

        boolean isAdmin = authentication.getRoles().contains(Role.ADMIN.toString());
        if (isAdmin) {
            return groupRepository.findAll(pageable);
        } else {
            String userEmail = authentication.getName();
            User user = userService.getUserByEmail(userEmail).get();
            return groupRepository.findIfMemberOfGroup(user.getId(), pageable);
        }
    }

    public void save(Group group) {
        if (group.getId() == null) {
            groupRepository.save(group);
        } else {
            groupRepository.update(group);
        }
    }

    public void deleteById(Long id) {
        groupRepository.deleteById(id);
    }

    @Transactional
    public boolean addMember(@Body Long groupId, @Body Long candidateId, boolean addAdmin) {
        Optional<Group> groupOptional = groupRepository.findById(groupId);
        Optional<User> userOptional = userRepository.findById(candidateId);
        if (groupOptional.isEmpty() || userOptional.isEmpty()) {
            return false;
        }
        Group group = groupOptional.get();
        User user = userOptional.get();
        if (addAdmin) {
            group.addAdmin(user);
        } else {
            group.addUser(user);
        }
        groupRepository.update(group);
        return true;
    }

    public Optional<Map> getGroupAndCandidates(Long id) {
        Optional<Group> groupOptional = groupRepository.findById(id);
        if (groupOptional.isPresent()) {
            Group group = groupOptional.get();
            Iterable<User> candidateUsers = userService.listUsersNotInGroup(group);
            return Optional.of(Map.of("group", group, "candidateUsers", candidateUsers));
        }
        return Optional.empty();
    }

    public boolean removeMember(Long groupId, Long memberId, boolean addAdmin) {
        Optional<Group> byId = groupRepository.findById(groupId);
        if (byId.isEmpty()) {
            return false;
        }
        Group group = byId.get();
        if (addAdmin) {
            group.removeAdmin(memberId);
        } else {
            group.removeUser(memberId);
        }
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

        boolean isAdmin = authentication.getRoles().contains(Role.ADMIN.toString());
        boolean isGroupAdmin = group.get().getAdmins().stream().anyMatch(groupAdmins -> groupAdmins.getEmail().equals(userEmail));

        return isAdmin || isGroupAdmin;
    }
}
