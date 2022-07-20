package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

@Controller("/applications")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class ApplicationController {

    @Get
    public HttpResponse index() {
        return HttpResponse.ok();
    }
}
