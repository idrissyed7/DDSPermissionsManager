package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.oauth2.endpoint.authorization.state.State;
import io.micronaut.security.oauth2.endpoint.token.response.DefaultOpenIdAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserService;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Replaces(DefaultOpenIdAuthenticationMapper.class)
@Singleton
@Named("google")
public class PermissionsManagerAuthenticationMapper implements OpenIdAuthenticationMapper {

    private final UserService userService;

    public PermissionsManagerAuthenticationMapper(UserService userService) {
        this.userService = userService;
    }

    @Override
    public AuthenticationResponse createAuthenticationResponse(String providerName,
                                                               OpenIdTokenResponse tokenResponse,
                                                               OpenIdClaims openIdClaims,
                                                               State state) {

        String email = openIdClaims.getEmail();
        Optional<User> user = userService.getUserByEmail(email);
        if (email == null || user.isEmpty()) {
            return AuthenticationResponse.failure(AuthenticationFailureReason.USER_NOT_FOUND);
        }

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("isAdmin", user.get().isAdmin());
        attributes.put("name", openIdClaims.getName());

        return AuthenticationResponse.success( Objects.requireNonNull(openIdClaims.getEmail()),
                Collections.emptyList(),
                attributes);
    }
}