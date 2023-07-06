package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.testing.util.SecurityAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

@Property(name = "micronaut.security.reject-not-found", value = StringUtils.FALSE)
@MicronautTest
class ApplicationControllerSecuredTest {
    @Test
    void getApiApplicationsPermissionsJsonRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications/permissions.json"));
    }

    @Test
    void getApiApplicationsShowRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications/show/99"));
    }

    @Test
    void getApiApplicationsGenerateBinTokenRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications/generate_grant_token//99").accept(MediaType.TEXT_PLAIN));
    }

    @Test
    void getApiApplicationsGeneratePassphraseRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications/generate_passphrase/99"));
    }

    @Test
    void postApiApplicationsDeleteRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.DELETE("/api/applications/99", Collections.emptyMap()));
    }

    @Test
    void postApiApplicationsSaveRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        ApplicationDTO applicationDTO = new ApplicationDTO();
        applicationDTO.setName("foo");
        applicationDTO.setGroup(99L);
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.POST("/api/applications/save", applicationDTO));
    }

    @Test
    void getApiApplicationsRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications"));
    }

    @Test
    void getApiApplicationsIdentyCaRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications/identity_ca.pem"));
    }

    @Test
    void getApiApplicationsPermissionsCaRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications/permissions_ca.pem"));
    }

    @Test
    void getApiApplicationsGovernanceXmlP7sRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications//governance.xml.p7s"));
    }

    @Test
    void getApiApplicationsKeyPairRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications/key_pair"));
    }

    @Test
    void getApiApplicationsPermissionsXmlP7sRequiresAuthentication(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/applications/permissions.xml.p7s"));
    }
}