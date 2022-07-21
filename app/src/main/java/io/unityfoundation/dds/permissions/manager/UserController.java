package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;

import javax.validation.Valid;
import java.net.URI;

@Controller("/users")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Get
    public HttpResponse index(@Valid Pageable pageable) {
        return HttpResponse.ok(userRepository.findAll(pageable));
    }

    @Get("/create")
    public HttpResponse create() {
        return HttpResponse.ok();
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    HttpResponse<?> save(@Body User user) {
        userRepository.save(user);
        return HttpResponse.seeOther(URI.create("/users/"));
    }

    @Post("/delete/{id}")
    HttpResponse<?> delete(Long id) {
        userService.deleteById(id);
        return HttpResponse.seeOther(URI.create("/users"));
    }
}
