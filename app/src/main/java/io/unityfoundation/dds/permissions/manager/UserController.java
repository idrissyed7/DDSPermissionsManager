package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;

import javax.validation.Valid;
import java.net.URI;

@Controller("/users")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @ExecuteOn(TaskExecutors.IO)
    @Get
    public HttpResponse<Page<User>> index(@Valid Pageable pageable) {
        return HttpResponse.ok(userService.findAll(pageable));
    }

    @ExecuteOn(TaskExecutors.IO)
    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "303", description = "Returns result of /users")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    HttpResponse<?> save(@Body User user) {
        try {
            userService.save(user);
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
        return HttpResponse.seeOther(URI.create("/users/"));
    }

    @ExecuteOn(TaskExecutors.IO)
    @Post("/delete/{id}")
    @ApiResponse(responseCode = "303", description = "Returns result of /topics")
    HttpResponse<?> delete(Long id) {
        userService.deleteById(id);
        return HttpResponse.seeOther(URI.create("/users"));
    }
}
