package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.unityfoundation.dds.permissions.manager.model.user.Role;

@Controller("/roles")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class RoleController {

    @Get
    public HttpResponse index() {
        return HttpResponse.ok(Role.values());
    }
}
