package io.unityfoundation.dds.permissions.manager.security;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.security.oauth2.endpoint.authorization.request.AuthorizationRequest;
import io.micronaut.security.oauth2.endpoint.authorization.request.DefaultAuthorizationRedirectHandler;
import jakarta.inject.Singleton;

import java.util.Map;

@Requires(beans = HostedDomain.class)
@Singleton
@Replaces(DefaultAuthorizationRedirectHandler.class)
public class DefaultAuthorizationRedirectHandlerReplacement extends DefaultAuthorizationRedirectHandler {

    private final HostedDomain hostedDomain;

    public DefaultAuthorizationRedirectHandlerReplacement(HostedDomain hostedDomain) {
        this.hostedDomain = hostedDomain;
    }

    @Override
    protected Map<String, Object> instantiateParameters(AuthorizationRequest authorizationRequest, MutableHttpResponse response) {
        Map<String, Object> m = super.instantiateParameters(authorizationRequest, response);
        m.put(HostedDomain.CLAIM_HD, hostedDomain.getUrl());
        return m;
    }
}
