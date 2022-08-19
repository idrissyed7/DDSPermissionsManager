package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationService;

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
    @ApiResponse(responseCode = "303", description = "Returns result of /applications")
    HttpResponse<?> save(@Body Application application) {
        applicationService.save(application);
        return HttpResponse.seeOther(URI.create("/applications/"));
    }

    @Post("/delete/{id}")
    @ApiResponse(responseCode = "303", description = "Returns result of /applications")
    HttpResponse<?> delete(Long id) {
        applicationService.deleteById(id);
        return HttpResponse.seeOther(URI.create("/applications"));
    }
}
