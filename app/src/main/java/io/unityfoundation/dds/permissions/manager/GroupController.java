package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.group.GroupService;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Controller("/groups")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class GroupController {

    private final GroupRepository groupRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final GroupService groupService;

    public GroupController(GroupRepository groupRepository, UserService userService, UserRepository userRepository, GroupService groupService) {
        this.groupRepository = groupRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    @View("/groups/index")
    @Produces(value = {MediaType.TEXT_HTML})
    @Get
    public HttpResponse index() {
        return HttpResponse.ok(Map.of("groups", groupRepository.findAll()));
    }

    @View("/groups/create")
    @Produces(value = {MediaType.TEXT_HTML})
    @Get("/create")
    public HttpResponse create() {
        return HttpResponse.ok();
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = {MediaType.TEXT_HTML})
    HttpResponse<?> save(@Body Group group) {
        groupRepository.save(group);
        return HttpResponse.seeOther(URI.create("/groups/"));
    }


    @Post("/delete/{id}")
    @Produces(value = {MediaType.TEXT_HTML})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    HttpResponse<?> delete(Long id) {
        groupRepository.deleteById(id);
        return HttpResponse.seeOther(URI.create("/groups"));
    }

    @Get("/{id}")
    @View("/groups/show")
    @Produces(value = {MediaType.TEXT_HTML})
    HttpResponse show(Long id) {
        Optional<Group> groupOptional = groupRepository.findById(id);
        if (groupOptional.isPresent()) {
            Group group = groupOptional.get();
            Iterable<User> candidateUsers = userService.listUsersNotInGroup(group);
            return HttpResponse.ok(Map.of("group", group, "candidateUsers", candidateUsers));
        }
        return HttpResponse.notFound();
    }

    @Post("/remove_member/{groupId}/{memberId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = {MediaType.TEXT_HTML})
    HttpResponse removeMemeber(Long groupId, Long memberId) {
        Optional<Group> byId = groupRepository.findById(groupId);
        if (byId.isEmpty()) {
            return HttpResponse.notFound();
        }
        Group group = byId.get();
        group.removeUser(memberId);
        groupRepository.update(group);
        return HttpResponse.seeOther(URI.create("/groups/" + groupId));
    }

    @Post("/add_member/{groupId}/{candidateId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = {MediaType.TEXT_HTML})
    HttpResponse addMember(Long groupId, Long candidateId) {
        if (groupService.addMember(groupId, candidateId)) {
            return HttpResponse.seeOther(URI.create("/groups/" + groupId));
        }
        return HttpResponse.notFound();
    }

}
