package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationProvider;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationService;
import io.unityfoundation.dds.permissions.manager.model.user.UserRole;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.List;

@Singleton
public class AuthenticationProviderUserPassword implements AuthenticationProvider {

    @Nullable
    @Property(name = "permissions-manager.test.username")
    protected String testUsername;

    @Nullable
    @Property(name = "permissions-manager.test.password")
    protected String testPassword;

    @Nullable
    @Property(name = "permissions-manager.test.email")
    protected String testEmail;

    @Property(name = "permissions-manager.test.is-admin", defaultValue = "false")
    protected boolean testUserIsAdmin;

    private final Environment environment;
    private final ApplicationService applicationService;

    public AuthenticationProviderUserPassword(Environment environment, ApplicationService applicationService) {
        this.environment = environment;
        this.applicationService = applicationService;
    }

    @Override
    public Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest,
                                                          AuthenticationRequest<?, ?> authenticationRequest) {

        String identity = (String) authenticationRequest.getIdentity();
        String password = (String) authenticationRequest.getSecret();

        if ( testUsername != null && testPassword != null && testEmail != null &&
                identity.equals(testUsername) && password.equals(testPassword) &&
                (environment.getActiveNames().contains("dev") || environment.getActiveNames().contains("test")) ) {

            return Publishers.just(AuthenticationResponse.success(testEmail));
        } else {
            // application login
            try {
                return Publishers.just(applicationService.passwordMatches(Long.valueOf(identity), password));
            } catch (NumberFormatException numberFormatException) {
                Publishers.just(AuthenticationResponse.exception("Incorrect Application Id format."));
            }
        }

        return Publishers.just(AuthenticationResponse.failure());
    }
}
