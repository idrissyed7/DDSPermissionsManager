package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.testing.util.SecurityAssertions;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.util.Collections;

@Property(name = "spec.name", value = "AdminControllerSecuredWithRoleTest")
@Property(name = "micronaut.security.reject-not-found", value = StringUtils.FALSE)
@MicronautTest
class AdminControllerSecuredWithRoleTest {
    @Test
    void getApiAdminsRequiresAdminsRole(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertForbidden(httpClient, HttpRequest.GET("/api/admins"));
    }

    @Test
    void postApiAdminsSaveRequiresAdminsRole(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertForbidden(httpClient, HttpRequest.POST("/api/admins/save", Collections.emptyMap()));
    }

    @Test
    void putApiAdminsRemoveAdminRequiresAdminsRole(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertForbidden(httpClient, HttpRequest.PUT("/api/admins/remove_admin/99", Collections.emptyMap()));
    }

    @Requires(property = "spec.name", value = "AdminControllerSecuredWithRoleTest")
    @Singleton
    static class MockAuthenticationFetcher implements AuthenticationFetcher {

        @Override
        public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
            return Publishers.just(Authentication.build("sherlock"));
        }
    }
}