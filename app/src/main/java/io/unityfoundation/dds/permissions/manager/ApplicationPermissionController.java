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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.AccessType;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermission;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionService;

import javax.validation.Valid;

@Controller("/api/application_permissions")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "application permissions")
public class ApplicationPermissionController {
    private final ApplicationPermissionService applicationPermissionService;

    public ApplicationPermissionController(ApplicationPermissionService applicationPermissionService) {
        this.applicationPermissionService = applicationPermissionService;
    }

    @Get("{?application,topic")
    @ExecuteOn(TaskExecutors.IO)
    public Page<ApplicationPermission> index(@Nullable Long application, @Nullable Long topic, @Valid Pageable pageable) {
        return applicationPermissionService.findAll(application, topic, pageable);
    }

    @Get("access_types")
    public AccessType[] getAccessTypes() {
        return AccessType.values();
    }
}
