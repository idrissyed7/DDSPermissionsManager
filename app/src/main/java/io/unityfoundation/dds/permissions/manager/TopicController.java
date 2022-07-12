package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;

@Controller("/topics")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class TopicController {

    @View("/topics/index")
    @Produces(value = {MediaType.TEXT_HTML})
    @Get
    public HttpResponse home() {
        return HttpResponse.ok();
    }
}
