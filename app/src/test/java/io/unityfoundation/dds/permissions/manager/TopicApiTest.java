package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "micronaut.security.filter.enabled", value = StringUtils.FALSE)
public class TopicApiTest {

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
        HttpRequest<?> request = HttpRequest.POST("/topics/save", Map.of("name", "testTopic1"));
        HttpResponse<?> response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.POST("/topics/save", Map.of("name", "testTopic2"));
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // update attempt should fail
        request = HttpRequest.POST("/topics/save", Map.of("id", 2, "name", "UpdatedTestTopic2"));
        HttpRequest<?> finalRequest = request;
        HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequest);
        });
        assertEquals(BAD_REQUEST, thrown.getStatus());

        // list
        request = HttpRequest.GET("/topics");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> topics = (List<Map>) responseMap.get("content");
        assertEquals(2, topics.size());
        assertEquals(OK, response.getStatus());

        // confirm update failed
        Map<String, Object> updatedTopic = topics.get(1);
        assertNotEquals("UpdatedTestTopic2", updatedTopic.get("name"));

        // delete
        request = HttpRequest.POST("/topics/delete/2", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // list to confirm deletion
        request = HttpRequest.GET("/topics");
        HashMap<String, Object> responseMap1 = blockingClient.retrieve(request, HashMap.class);
        List<Map> topics1 = (List<Map>) responseMap1.get("content");
        assertEquals(1, topics1.size());
        assertEquals(OK, response.getStatus());
    }
}
