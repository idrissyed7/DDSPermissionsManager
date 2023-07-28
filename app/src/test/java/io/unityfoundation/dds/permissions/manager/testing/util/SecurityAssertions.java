// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.testing.util;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SecurityAssertions {
    private SecurityAssertions() {

    }

    public static void assertUnauthorized(HttpClient httpClient, HttpRequest<?> request) throws IOException {
        assertStatus(httpClient, request, HttpStatus.UNAUTHORIZED);
    }

    public static void assertForbidden(HttpClient httpClient, MutableHttpRequest<Object> request) throws IOException {
        assertStatus(httpClient, request, HttpStatus.FORBIDDEN);
    }

    public static void assertBadRequest(HttpClient httpClient, MutableHttpRequest<Object> request) throws IOException {
        assertStatus(httpClient, request, HttpStatus.BAD_REQUEST);
    }

    private static void assertStatus(HttpClient httpClient, HttpRequest<?> request, HttpStatus expectedStatus) throws IOException {
        BlockingHttpClient client = httpClient.toBlocking();
        Executable e = () -> client.exchange(request);
        HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, e);
        assertEquals(expectedStatus, thrown.getStatus());
        client.close();
    }
}
