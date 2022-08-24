package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.rules.SecurityRule;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationService;

import javax.validation.Valid;
import java.net.URI;

@Controller("/applications")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Get
    public HttpResponse index(@Valid Pageable pageable) {
        return HttpResponse.ok(applicationService.findAll(pageable));
    }

    @Get("/create")
    public HttpResponse create() {
        return HttpResponse.ok();
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    HttpResponse<?> save(@Body Application application) {
        try {
            return applicationService.save(application);
        } catch (AuthenticationException authenticationException) {
            return HttpResponse.unauthorized();
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
    }

    @Post("/delete/{id}")
    HttpResponse<?> delete(Long id) {
        try {
            applicationService.deleteById(id);
        } catch (AuthenticationException authenticationException) {
            return HttpResponse.unauthorized();
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }

        return HttpResponse.seeOther(URI.create("/applications"));
    }
}
