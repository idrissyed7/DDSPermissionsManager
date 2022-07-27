package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

@Controller
public class HomeController {

    @Get
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse home() {
        return HttpResponse.ok();
    }
}
