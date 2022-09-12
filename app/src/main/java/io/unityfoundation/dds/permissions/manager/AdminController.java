package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.user.AdminDTO;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;

import javax.validation.Valid;
import java.net.URI;

@Controller("/admins")
@Secured("ADMIN")
@Tag(name = "admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // todo pending update with upcoming story
    @Get
    public HttpResponse<Page<User>> index(@Valid Pageable pageable) {
        return HttpResponse.ok(userService.findAll(pageable));
    }

    // todo (UPDATE) pending update with upcoming story
    @Post("/save")
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

    // todo pending update with upcoming story
    @Post("/delete/{id}")
    @ApiResponse(responseCode = "303", description = "Returns result of /users")
    HttpResponse<?> delete(Long id) {
        userService.deleteById(id);
        return HttpResponse.seeOther(URI.create("/users"));
    }
}
