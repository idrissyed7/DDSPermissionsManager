package io.unityfoundation.dds.permissions.manager;

import io.micronaut.core.annotation.NonNull;
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
import io.unityfoundation.dds.permissions.manager.model.group.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Controller("/api/groups")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "group")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @Get("{?filter}")
    @ExecuteOn(TaskExecutors.IO)
    public Page<GroupDTO> index(@Valid Pageable pageable, @Nullable String filter) {
        return groupService.findAll(pageable, filter);
    }

    @Get("/search/{text}{?role}")
    @ExecuteOn(TaskExecutors.IO)
    public Page<GroupSearchDTO> search(@NonNull String text, @Nullable GroupAdminRole role, @Valid Pageable pageable) {
        return groupService.search(text, role, pageable);
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
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
        return HttpResponse.seeOther(URI.create("/api/groups"));
    }
}
