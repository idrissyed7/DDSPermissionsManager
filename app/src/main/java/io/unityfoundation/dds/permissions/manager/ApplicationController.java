package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationService;
import io.unityfoundation.dds.permissions.manager.model.group.Group;

import javax.validation.Valid;
import java.net.URI;

@Controller("/applications")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "application")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Get
    public HttpResponse<Page<Application>> index(@Valid Pageable pageable) {
        return HttpResponse.ok(applicationService.findAll(pageable));
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Application.class))
    )
    @ApiResponse(responseCode = "303", description = "Applications already exists. Response body contains original.")
    @ApiResponse(responseCode = "400", description = "Bad Request.")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
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
    @ApiResponse(responseCode = "303", description = "Returns result of /applications")
    @ApiResponse(responseCode = "400", description = "Bad Request.")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
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
