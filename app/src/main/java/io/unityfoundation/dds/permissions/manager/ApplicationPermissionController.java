// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.AccessPermissionDTO;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionService;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.AccessPermissionBodyDTO;
import org.reactivestreams.Publisher;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import static io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionService.APPLICATION_GRANT_TOKEN;

@Controller("/api/application_permissions")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "application permissions")
public class ApplicationPermissionController {
    private final ApplicationPermissionService applicationPermissionService;

    public ApplicationPermissionController(ApplicationPermissionService applicationPermissionService) {
        this.applicationPermissionService = applicationPermissionService;
    }

    @Get("/topic/{topic}")
    @ExecuteOn(TaskExecutors.IO)
    public Page<AccessPermissionDTO> indexIndexByTopicId(Long topic, @Valid Pageable pageable) {
        return applicationPermissionService.indexByTopicId(topic, pageable);
    }

    @Get("/application/{application}")
    @ExecuteOn(TaskExecutors.IO)
    public Page<AccessPermissionDTO> indexByApplicationId(Long application, @Valid Pageable pageable) {
        return applicationPermissionService.indexByApplicationId(application, pageable);
    }

    @Post("/{topicId}")
    @ExecuteOn(TaskExecutors.IO)
    public Publisher<HttpResponse<AccessPermissionDTO>> addAccess(Long topicId,
                                                                  @NotBlank @Header(APPLICATION_GRANT_TOKEN) String grantToken,
                                                                  @Valid @Body AccessPermissionBodyDTO accessPermissionBodyDTO) {
        return applicationPermissionService.addAccess(grantToken, topicId, accessPermissionBodyDTO);
    }

    @Put("/{permissionId}")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<AccessPermissionDTO> updateAccess(Long permissionId,
                                                          @Valid @Body AccessPermissionBodyDTO accessPermissionBodyDTO) {
        return applicationPermissionService.updateAccess(permissionId, accessPermissionBodyDTO);
    }

    @Delete("/{permissionId}")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse removeAccess(Long permissionId) {
        return applicationPermissionService.deleteById(permissionId);
    }
}
