package io.unityfoundation.dds.permissions.manager;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserResponseDTO;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserService;

import javax.validation.Valid;
import java.util.Map;

@Controller("/api/group_membership")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "group membership")
public class GroupMembershipController {

    private final GroupUserService groupUserService;

    public GroupMembershipController(GroupUserService groupUserService) {
        this.groupUserService = groupUserService;
    }

    @Get("{?filter}")
    @ExecuteOn(TaskExecutors.IO)
    public Page<GroupUserResponseDTO> index(@Valid Pageable pageable, @Nullable String filter) {
        return groupUserService.findAll(pageable, filter);
    }

    @Get("/user-validity")
    @ApiResponse(responseCode = "200", description = "Valid User")
    @ApiResponse(responseCode = "404", description = "Invalid User")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse checkIfUserIsValid() {
        return groupUserService.checkUserValidity();
    }

    @Post
    @ExecuteOn(TaskExecutors.IO)
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupUser.class))
    )
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    HttpResponse addMember(@Body GroupUserDTO dto) {
        return groupUserService.addMember(dto);
    }

    @Put
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse updateMember(@Body GroupUserDTO groupUser) {
        return groupUserService.updateMember(groupUser);
    }

    @Delete
    @ExecuteOn(TaskExecutors.IO)
    @ApiResponse(responseCode = "200", description = "Ok")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    HttpResponse removeMember(@Body Map payload) {
        Long id = Long.valueOf((Integer) payload.get("id"));
        return groupUserService.removeMember(id);
    }

    @Get("/user-exists/{id}")
    @ExecuteOn(TaskExecutors.IO)
    @ApiResponse(responseCode = "200", description = "Ok")
    @ApiResponse(responseCode = "404", description = "Not found")
    HttpResponse checkUserExists(Long id) {
        return groupUserService.checkUserExists(id);
    }
}
