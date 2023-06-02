package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.testing.util.SecurityAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

@Property(name = "micronaut.security.reject-not-found", value = StringUtils.FALSE)
@MicronautTest
class AdminControllerSecuredTest {
    @Test
    void getApiAdminsRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/admins"));
    }

    @Test
    void postApiAdminsSaveRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.POST("/api/admins/save", Collections.emptyMap()));
    }

    @Test
    void putApiAdminsRemoveAdminRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.PUT("/api/admins/remove_admin/99", Collections.emptyMap()));
    }

}