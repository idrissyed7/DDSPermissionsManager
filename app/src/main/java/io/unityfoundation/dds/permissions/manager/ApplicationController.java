package io.unityfoundation.dds.permissions.manager;

import freemarker.template.TemplateException;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationService;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OperatorCreationException;

import javax.mail.MessagingException;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Optional;

@Controller("/api/applications")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "application")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Get("{?filter}")
    public Page<ApplicationDTO> index(@Valid Pageable pageable, @Nullable String filter) {
        return applicationService.findAll(pageable, filter);
    }

    @Get("/show/{id}")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApplicationDTO.class))
    )
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse show(Long id) {
        try {
            return applicationService.show(id);
        } catch (AuthenticationException authenticationException) {
            return HttpResponse.unauthorized();
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApplicationDTO.class))
    )
    @ApiResponse(responseCode = "303", description = "Applications already exists. Response body contains original.")
    @ApiResponse(responseCode = "400", description = "Bad Request.")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> save(@Body ApplicationDTO application) {
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
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> delete(Long id) {
        try {
            applicationService.deleteById(id);
        } catch (AuthenticationException authenticationException) {
            return HttpResponse.unauthorized();
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }

        return HttpResponse.seeOther(URI.create("/api/applications"));
    }

    @Get("/search{?filter}")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse search(@Nullable String filter, @Valid Pageable page) {
        return HttpResponse.ok(applicationService.search(filter, page));
    }

    @Get("/generate-passphrase/{application}")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<?> generatePassphrase(@NonNull Long application) {
        try {
            return applicationService.generateCleartextPassphrase(application);
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
    }

    @Get("/identity_ca.pem")
    @Secured("APPLICATION")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<?> getIdentityCACertificate() {
        return applicationService.getIdentityCACertificate();
    }

    @Get("/permissions_ca.pem")
    @Secured("APPLICATION")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<?> getPermissionsCACertificate() {
        return applicationService.getPermissionsCACertificate();
    }

    @Get("/governance.xml.p7s")
    @Secured("APPLICATION")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<?> getGovernanceFile() {
        return applicationService.getGovernanceFile();
    }

    @Post("/key-pair")
    @Secured("APPLICATION")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<?> getPrivateKeyAndClientCertificate(@QueryValue Optional<String> nonce) throws IOException, OperatorCreationException, GeneralSecurityException {
        if (nonce.isEmpty()) {
            return HttpResponse.badRequest();
        }
        return applicationService.getApplicationPrivateKeyAndClientCertificate(nonce.get());
    }

    @Get("/permissions.xml.p7s{?nonce}")
    @Secured("APPLICATION")
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse<?> getPermissionsFile(@Nullable String nonce) throws IOException, OperatorCreationException, GeneralSecurityException, MessagingException, SMIMEException, TemplateException {
        if (nonce == null) {
            return HttpResponse.badRequest();
        }
        return applicationService.getPermissionsFile(nonce);
    }
}
