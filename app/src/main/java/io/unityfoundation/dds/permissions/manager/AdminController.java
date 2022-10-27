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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.user.AdminDTO;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;

import javax.validation.Valid;

@Controller("/api/admins")
@Secured("ADMIN")
@Tag(name = "admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @Get("{?filter}")
    @ExecuteOn(TaskExecutors.IO)
    public Page<AdminDTO> index(@Valid Pageable pageable, @Nullable String filter) {
        return userService.findAll(pageable, filter);
    }

    @Post("/save")
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDTO.class))
    )
    @ApiResponse(responseCode = "400", description = "Bad Request")
    HttpResponse<?> save(@Body AdminDTO adminDTO) {
        try {
            return HttpResponse.ok(userService.save(adminDTO));
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
    }

    @Put("/remove-admin/{id}")
    @ExecuteOn(TaskExecutors.IO)
    @ApiResponse(responseCode = "404", description = "User cannot be found.")
    HttpResponse<?> removeAdminPrivilege(Long id) {
        if (!userService.removeAdminPrivilegeById(id)) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok();
    }
}
