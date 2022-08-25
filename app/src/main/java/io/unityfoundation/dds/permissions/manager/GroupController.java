package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.rules.SecurityRule;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupService;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller("/groups")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class GroupController {

    private final GroupService groupService;
    private final GroupUserService groupUserService;

    public GroupController(GroupService groupService, GroupUserService groupUserService) {
        this.groupService = groupService;
        this.groupUserService = groupUserService;
    }

    @Get
    public HttpResponse index(@Valid Pageable pageable) {
        return HttpResponse.ok(groupService.findAll(pageable));
    }

    @Get("/create")
    public HttpResponse create() {
        return HttpResponse.ok();
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    HttpResponse<?> save(@Body Group group) {
        try {
            return groupService.save(group);
        } catch (AuthenticationException authenticationException) {
            return HttpResponse.unauthorized();
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
    }


    @Post("/delete/{id}")
    HttpResponse<?> delete(Long id) {
        try {
            groupService.deleteById(id);
        } catch (AuthenticationException ae) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.seeOther(URI.create("/groups"));
    }

    @Get("/{id}")
    HttpResponse show(Long id) {
        Optional<Map> groupOptional = groupService.getGroupDetails(id);
        if (groupOptional.isPresent()) {
            Map payload = groupOptional.get();
            return HttpResponse.ok(payload);
        }
        return HttpResponse.notFound();
    }

    @Get("/{id}/members")
    HttpResponse showMembers(Long id) {
        List<Map> groupMembers = groupService.getGroupMembers(id);
        if (!groupMembers.isEmpty()) {
            return HttpResponse.ok(groupMembers);
        }
        return HttpResponse.notFound();
    }

    @Get("/user/{id}/")
    HttpResponse showGroupsUserIsAMemberOf(Long id) {
        try {
            return HttpResponse.ok(groupService.getGroupsUserIsAMemberOf(id));
        } catch (AuthenticationException ae) {
            return HttpResponse.unauthorized();
        }
    }

    @Post("/remove_topic/{groupId}/{topicId}")
    HttpResponse removeTopic(Long groupId, Long topicId) {

        if (groupUserService.isAdminOrGroupAdmin(groupId)) {
            if (groupService.removeTopic(groupId, topicId)) {
                return HttpResponse.seeOther(URI.create("/groups/" + groupId));
            }
        } else {
            return HttpResponse.unauthorized();
        }

        return HttpResponse.notFound();
    }

    @Post("/add_topic/{groupId}/{topicId}")
    HttpResponse addTopic(Long groupId, Long topicId) {

        if (groupUserService.isAdminOrGroupAdmin(groupId)) {
            try {
                if (groupService.addTopic(groupId, topicId)) {
                    return HttpResponse.seeOther(URI.create("/groups/" + groupId));
                }
            } catch (Exception e) {
                return HttpResponse.badRequest(e.getMessage());
            }
        } else {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.notFound();
    }

}
