package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Status;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

@Controller
public class HomeController {
    @Get
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Status(HttpStatus.OK)
    public void home() {
    }
}
