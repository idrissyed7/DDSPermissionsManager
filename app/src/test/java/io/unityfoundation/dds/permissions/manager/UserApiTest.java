package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.user.Role;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
@Property(name = "micronaut.security.filter.enabled", value = StringUtils.FALSE)
public class UserApiTest {

    private BlockingHttpClient blockingClient;

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    void setup() {
        blockingClient = client.toBlocking();
    }

    @Test
    public void testCrudActions() {

        long initialUserCount = userRepository.count();

        // save
        User justin = new User("Justin", "Jones", "jjones@test.test", List.of(Role.ADMIN));
        HttpRequest<?> request = HttpRequest.POST("/users/save", justin);
        HttpResponse<?> response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        User kevin = new User("Kevin", "Kaminsky", "kkaminsky@test.test", List.of(Role.ADMIN));
        request = HttpRequest.POST("/users/save", kevin);
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // update
        request = HttpRequest.POST("/users/save", Map.of("id", 2, "firstName", "Michael"));
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // list
        request = HttpRequest.GET("/users");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> users = (List<Map>) responseMap.get("content");
        assertEquals(initialUserCount + 2, users.size());
        assertEquals(OK, response.getStatus());

        // confirm update
        Map<String, Object> updatedApp = users.get(1);
        assertEquals("Michael", updatedApp.get("firstName"));

        // delete
        request = HttpRequest.POST("/users/delete/2", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // list to confirm deletion
        request = HttpRequest.GET("/users");
        HashMap<String, Object> responseMap1 = blockingClient.retrieve(request, HashMap.class);
        List<Map> users1 = (List<Map>) responseMap1.get("content");
        assertEquals(initialUserCount + 1, users1.size());
        assertEquals(OK, response.getStatus());
    }
}
