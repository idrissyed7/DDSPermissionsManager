package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Page;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.user.AdminDTO;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.testing.util.DbCleanup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

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

        @Test
        public void shouldSeeAdminsInAscendingOrderByDefault() {

            AdminDTO homer = new AdminDTO("hsimpson@foobar.com");
            HttpRequest<?> request = HttpRequest.POST("/admins/save", homer);
            HttpResponse<?> response = blockingClient.exchange(request, AdminDTO.class);
            assertEquals(OK, response.getStatus());

            AdminDTO bart = new AdminDTO("bsimpson@foobar.com");
            request = HttpRequest.POST("/admins/save", bart);
            response = blockingClient.exchange(request, AdminDTO.class);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/admins");
            Page<Map> responsePage = blockingClient.retrieve(request, Page.class);
            List<Map> admins = responsePage.getContent();
            List<String> adminEmails = admins.stream().map(map -> (String) map.get("email")).collect(Collectors.toList());
            assertEquals(adminEmails.stream().sorted().collect(Collectors.toList()), adminEmails);
        }

        @Test
        public void shouldRespectAdminEmailsInDescendingOrder() {

            AdminDTO bart = new AdminDTO("bsimpson@foobar.com");
            HttpRequest<?> request = HttpRequest.POST("/admins/save", bart);
            HttpResponse<?> response = blockingClient.exchange(request, AdminDTO.class);
            assertEquals(OK, response.getStatus());

            AdminDTO homer = new AdminDTO("hsimpson@foobar.com");
            request = HttpRequest.POST("/admins/save", homer);
            response = blockingClient.exchange(request, AdminDTO.class);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/admins?sort=email,desc");
            Page<Map> responsePage = blockingClient.retrieve(request, Page.class);
            List<Map> admins = responsePage.getContent();
            List<String> groupNames = admins.stream().flatMap(map -> Stream.of((String) map.get("email"))).collect(Collectors.toList());
            assertEquals(groupNames.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()), groupNames);
        }
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
        @Test
        void cannotListAdmins(){
            loginAsNonAdmin();
            HttpRequest<?>request = HttpRequest.GET("/admins");
            HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(FORBIDDEN, thrown.getStatus());
        }
        // delete
    }


}
