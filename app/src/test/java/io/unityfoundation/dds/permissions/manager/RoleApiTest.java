package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "micronaut.security.filter.enabled", value = StringUtils.FALSE)
public class RoleApiTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    public void testRolesIndex() throws Exception {
        HttpResponse e = client.toBlocking().exchange("/roles", String.class);
        String body = (String) e.getBody().get();
        assertEquals(HttpStatus.OK, e.getStatus());
        assertNotNull(body);
    }
}
