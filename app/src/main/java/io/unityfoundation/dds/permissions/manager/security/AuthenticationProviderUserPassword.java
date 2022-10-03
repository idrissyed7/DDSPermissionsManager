package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationProvider;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.unityfoundation.dds.permissions.manager.DevOrTestCondition;
import io.unityfoundation.dds.permissions.manager.model.user.UserRole;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.List;

@Singleton
@Requires(condition = DevOrTestCondition.class)
public class AuthenticationProviderUserPassword implements AuthenticationProvider {
    @Override
    public Publisher<AuthenticationResponse> authenticate(@Nullable HttpRequest<?> httpRequest,
                                                          AuthenticationRequest<?, ?> authenticationRequest) {
        String identity = (String) authenticationRequest.getIdentity();
        String password = (String) authenticationRequest.getSecret();
        if ( (identity.equals("unity") || identity.equals("unity-admin")) &&
                password.equals("password") ) {

            return Publishers.just(AuthenticationResponse.success((String) authenticationRequest.getIdentity(),
                    (identity.equals("unity-admin") ? List.of(UserRole.ADMIN.toString()) : Collections.emptyList())
            ));
        } else {
            return Publishers.just(AuthenticationResponse.exception());
        }
    }
}
