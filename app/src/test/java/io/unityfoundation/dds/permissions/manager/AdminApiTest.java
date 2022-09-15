package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.ClientAuthentication;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.user.AdminDTO;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.testing.util.DbCleanup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
public class AdminApiTest {

    private BlockingHttpClient blockingClient;

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    MockSecurityService mockSecurityService;

    @Inject
    MockAuthenticationFetcher mockAuthenticationFetcher;

    @Inject
    DbCleanup dbCleanup;

    @BeforeEach
    void setup() {
        blockingClient = client.toBlocking();
    }

    @Nested
    class WhenAsAdmin {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        // create
        @Test
        void canAddAdmin(){
            AdminDTO justin = new AdminDTO("jjones@foobar.com");
            HttpRequest<?> request = HttpRequest.POST("/admins/save", justin);
            HttpResponse<?> response = blockingClient.exchange(request, AdminDTO.class);
            assertEquals(OK, response.getStatus());
        }

        @Test
        void adminsWithDuplicateEntriesShouldNotExist(){
            AdminDTO justin = new AdminDTO("jjones@foobar.com");
            HttpRequest<?> request = HttpRequest.POST("/admins/save", justin);
            HttpResponse<?> response = blockingClient.exchange(request, AdminDTO.class);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/admins/save", justin);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest, AdminDTO.class);
            });
            assertEquals(BAD_REQUEST, thrown.getStatus());
        }

        @Test
        public void userWithInvalidEmailFormatShallNotPersist() {
            User john = new User("pparker@.test.test", true);
            HttpRequest<?> request = HttpRequest.POST("/admins/save", john);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(BAD_REQUEST, thrown.getStatus());
        }

        // update
        // list
        // delete
    }

    @Nested
    class WhenAsNonAdmin {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test"));
            userRepository.save(new User("jjones@test.test"));
        }

        void loginAsNonAdmin() {
            mockSecurityService.setServerAuthentication(new ServerAuthentication(
                    "jjones@test.test",
                    Collections.emptyList(),
                    Map.of("isAdmin", false)
            ));
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        @Test
        void cannotAddAdmin(){
            loginAsNonAdmin();
            AdminDTO justin = new AdminDTO("jjones@foobar.com");
            HttpRequest<?> request = HttpRequest.POST("/admins/save", justin);
            HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(FORBIDDEN, thrown.getStatus());
        }

        // create
        // update
        // list
        // delete
    }


}
