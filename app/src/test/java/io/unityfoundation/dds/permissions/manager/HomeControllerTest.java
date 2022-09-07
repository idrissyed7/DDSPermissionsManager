package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class HomeControllerTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Test
    void apexReturnsOk() {
        assertEquals(HttpStatus.OK, httpClient.toBlocking().exchange("/").getStatus());
    }
}