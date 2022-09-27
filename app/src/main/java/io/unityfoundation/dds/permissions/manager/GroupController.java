package io.unityfoundation.dds.permissions.manager;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupService;
import io.unityfoundation.dds.permissions.manager.model.group.GroupResponseDTO;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;

import javax.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Controller("/api/groups")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "group")
public class GroupController {

    private final GroupService groupService;
    private final GroupUserService groupUserService;

    public GroupController(GroupService groupService, GroupUserService groupUserService) {
        this.groupService = groupService;
        this.groupUserService = groupUserService;
    }

    @Get("{?filter}")
    @ExecuteOn(TaskExecutors.IO)
    public Page<GroupResponseDTO> index(@Valid Pageable pageable, @Nullable String filter) {
        return groupService.findAll(pageable, filter);
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Group.class))
    )
    @ApiResponse(responseCode = "303", description = "Group already exists. Response body contains original.")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ExecuteOn(TaskExecutors.IO)
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
    @ApiResponse(responseCode = "303", description = "Returns result of /groups")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> delete(Long id) {
        try {
            groupService.deleteById(id);
        } catch (AuthenticationException ae) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.seeOther(URI.create("/api/groups"));
    }

    @Get("/{id}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", description = "Not Found - Returned if the given group cannot be found.")
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse show(Long id) {
        Optional<Map> groupOptional = groupService.getGroupDetails(id);
        if (groupOptional.isPresent()) {
            Map payload = groupOptional.get();
            return HttpResponse.ok(payload);
        }
        return HttpResponse.notFound();
    }

    @Get("/user/{id}/")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json"),
            description = "Returns a list of dictionaries where each dictionary is of the form: \n" +
                    "\"groupId\", boolean,\n" +
                    "\"groupName\", boolean,\n" +
                    "\"isGroupAdmin\", boolean,\n" +
                    "\"isTopicAdmin\", boolean,\n" +
                    "\"isApplicationAdmin\", boolean"
    )
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> showGroupsUserIsAMemberOf(Long id) {
        try {
            return HttpResponse.ok(groupService.getGroupsUserIsAMemberOf(id));
        } catch (AuthenticationException ae) {
            return HttpResponse.unauthorized();
        }
    }

    @Post("/remove_topic/{groupId}/{topicId}")
    @ApiResponse(responseCode = "303", description = "Returns result of /groups")
    @ApiResponse(responseCode = "404", description = "Not Found - Topic or Group not found.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> removeTopic(Long groupId, Long topicId) {

        if (groupUserService.isAdminOrGroupAdmin(groupId)) {
            if (groupService.removeTopic(groupId, topicId)) {
                return HttpResponse.seeOther(URI.create("/api/groups/" + groupId));
            }
        } else {
            return HttpResponse.unauthorized();
        }

        return HttpResponse.notFound();
    }

    @Post("/add_topic/{groupId}/{topicId}")
    @ApiResponse(responseCode = "303", description = "Returns result of /groups")
    @ApiResponse(responseCode = "400", description = "Bad Request - Topic already exists.")
    @ApiResponse(responseCode = "404", description = "Not Found - Topic or Group not found.")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> addTopic(Long groupId, Long topicId) {

        if (groupUserService.isAdminOrGroupAdmin(groupId)) {
            try {
                if (groupService.addTopic(groupId, topicId)) {
                    return HttpResponse.seeOther(URI.create("/api/groups/" + groupId));
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
