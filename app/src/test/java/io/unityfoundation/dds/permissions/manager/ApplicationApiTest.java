package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.runtime.http.PageableRequestArgumentBinder;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
@Property(name = "micronaut.security.filter.enabled", value = StringUtils.FALSE)
public class ApplicationApiTest {

    private BlockingHttpClient blockingClient;

    @Inject
    @Client("/")
    HttpClient client;

    @BeforeEach
    void setup() {
        blockingClient = client.toBlocking();
    }

    @Test
    public void testCrudActions() {

        // save
        HttpRequest<?> request = HttpRequest.POST("/applications/save", Map.of("name", "testApp1"));
        HttpResponse<?> response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.POST("/applications/save", Map.of("name", "testApp2"));
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // update
        request = HttpRequest.POST("/applications/save", Map.of("id", 2, "name", "UpdatedTestApp2"));
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // list
        request = HttpRequest.GET("/applications");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> applications = (List<Map>) responseMap.get("content");
        assertEquals(2, applications.size());
        assertEquals(OK, response.getStatus());

        // confirm update
        Map<String, Object> updatedApp = applications.get(1);
        assertEquals("UpdatedTestApp2", updatedApp.get("name"));

        // delete
        request = HttpRequest.POST("/applications/delete/2", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // list to confirm deletion
        request = HttpRequest.GET("/applications");
        HashMap<String, Object> responseMap1 = blockingClient.retrieve(request, HashMap.class);
        List<Map> applications1 = (List<Map>) responseMap1.get("content");
        assertEquals(1, applications1.size());
        assertEquals(OK, response.getStatus());
    }

}
