package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;

import java.util.Map;

@Controller
public class HomeController {

    @Get
    @View("/index")
    @Produces(value = {MediaType.TEXT_HTML})
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse home() {
        return HttpResponse.ok(Map.of("something", "Something"));
    }
}
