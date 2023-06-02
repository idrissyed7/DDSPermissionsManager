package io.unityfoundation.dds.permissions.manager;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import org.reactivestreams.Publisher;

public class AuthenticationFetcherReplacement implements AuthenticationFetcher {


    private Authentication authentication;

    @Override
    public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
        return Publishers.just(this.authentication);
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }
}