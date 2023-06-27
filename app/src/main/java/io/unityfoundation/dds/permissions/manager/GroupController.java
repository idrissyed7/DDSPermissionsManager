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
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.exception.DPMErrorResponse;
import io.unityfoundation.dds.permissions.manager.model.group.*;

import javax.validation.Valid;

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
    public Page<DetailedGroupDTO> index(@Valid Pageable pageable, @Nullable String filter) {
        return groupService.findAll(pageable, filter);
    }

    @Get("/search/{text}{?role}")
    @ExecuteOn(TaskExecutors.IO)
    public Page<SimpleGroupDTO> search(@NonNull String text, @Nullable GroupAdminRole role, @Valid Pageable pageable) {
        return groupService.search(text, role, pageable);
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Group.class))
    )
    @ApiResponse(responseCode = "4xx", description = "Bad Request.",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DPMErrorResponse.class)))
    )
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> save(@Body @Valid SimpleGroupDTO group) {
        return groupService.save(group);
    }


    @Delete("/delete/{id}")
    @ApiResponse(responseCode = "303", description = "Returns result of /groups")
    @ApiResponse(responseCode = "4xx", description = "Bad Request.",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DPMErrorResponse.class)))
    )
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> delete(Long id) {
        return groupService.deleteById(id);
    }
}
