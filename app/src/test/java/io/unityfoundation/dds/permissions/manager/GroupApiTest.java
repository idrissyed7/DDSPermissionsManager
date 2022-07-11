package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
public class GroupApiTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    public void testAccessingSecureEndpointWithoutCredentials() throws Exception {
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange("/groups").status();
        });
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }
}
