package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.testing.util.SecurityAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

@Property(name = "micronaut.security.reject-not-found", value = StringUtils.FALSE)
@MicronautTest
class ApplicationPermissionControllerSecuredTest {
    @Test
    void getApiApplicationPermissionsTopicRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        URI uri = UriBuilder.of("/api").path("application_permissions").path("topic").path("foo").build();
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET(uri));
    }

    @Test
    void getApiApplicationPermissionsApplicationRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        URI uri = UriBuilder.of("/api").path("application_permissions").path("application").path("99").build();
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET(uri));
    }
}