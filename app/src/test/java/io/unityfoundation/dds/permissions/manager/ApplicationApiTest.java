package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.testing.util.DbCleanup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.*;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "micronaut.security.filter.enabled", value = StringUtils.FALSE)
public class ApplicationApiTest {

    private BlockingHttpClient blockingClient;

    @Inject
    MockSecurityService mockSecurityService;

    @Inject
    GroupRepository groupRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    DbCleanup dbCleanup;

    @Inject
    @Client("/")
    HttpClient client;

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
        }

        @Test
        public void cannotCreateWithoutGroupSpecified() {
            Application application = new Application();
            application.setName("TestApplication");
            HttpRequest<?> request = HttpRequest.POST("/applications/save", application);
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
        }

        @Test
        public void canCreateApplicationWithGroupSpecified() {
            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            HttpRequest<?> request = HttpRequest.POST("/applications/save", application);
            HttpResponse<?> response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }

        @Test
        public void cannotCreateApplicationWithSameNameAsAnotherInSameGroup() {
            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            Application applicationOne = new Application();
            applicationOne.setName("ApplicationOne");
            applicationOne.setPermissionsGroup(primaryGroup.getId());
            HttpRequest<?> request = HttpRequest.POST("/applications/save", applicationOne);
            HttpResponse<?> response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationOne = response.getBody(Application.class).get();

            request = HttpRequest.POST("/applications/save", applicationOne);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(SEE_OTHER, response.getStatus());
            Application application = response.getBody(Application.class).get();

            assertEquals(applicationOne.getId(), application.getId());
        }

        @Test
        public void canCreateApplicationWithSameNameInAnotherGroup() {
            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));
            Group secondaryGroup = groupRepository.save(new Group("SecondaryGroup"));

            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            HttpRequest<?> request = HttpRequest.POST("/applications/save", application);
            HttpResponse<?> response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            application.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }

        @Test
        public void canViewAllApplications() {
            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));
            Group secondaryGroup = groupRepository.save(new Group("SecondaryGroup"));

            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            HttpRequest<?> request = HttpRequest.POST("/applications/save", application);
            HttpResponse<?> response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            application.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/applications");
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            List<Map> applications = (List<Map>) responseMap.get("content");
            assertEquals(2, applications.size());
        }

        @Test
        public void canUpdateApplicationName() {
            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            HttpRequest<?> request = HttpRequest.POST("/applications/save", application);
            HttpResponse<?> response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();

            application.setName("TestApplicationUpdate");
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();

            assertEquals("TestApplicationUpdate", application.getName());
        }

        @Test
        public void canUpdateApplicationGroup() {
            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));
            Group secondaryGroup = groupRepository.save(new Group("SecondaryGroup"));

            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            HttpRequest<?> request = HttpRequest.POST("/applications/save", application);
            HttpResponse<?> response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();

            application.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();

            assertEquals(secondaryGroup.getId(), application.getPermissionsGroup());
        }


        @Test
        public void cannotUpdateGroupIfApplicationWithSameNameExistsInTargetGroup() {
            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));
            Group secondaryGroup = groupRepository.save(new Group("SecondaryGroup"));

            Application applicationOne = new Application();
            applicationOne.setName("TestApplication");
            applicationOne.setPermissionsGroup(primaryGroup.getId());
            HttpRequest<?> request = HttpRequest.POST("/applications/save", applicationOne);
            HttpResponse<?> response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationOne = response.getBody(Application.class).get();

            Application applicationTwo = new Application();
            applicationTwo.setName("TestApplication");
            applicationTwo.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationTwo);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationTwo = response.getBody(Application.class).get(); // 2

            applicationOne.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationOne);
            response = blockingClient.exchange(request, Application.class);

            assertEquals(SEE_OTHER, response.getStatus());
            Application application = response.getBody(Application.class).get(); // 4

            // 2, 4
            assertEquals(applicationTwo.getId(), application.getId());
        }

        @Test
        public void cannotUpdateApplicationNameIfOneAlreadyExistsInSameGroup() {
            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            Application applicationOne = new Application();
            applicationOne.setName("ApplicationOne");
            applicationOne.setPermissionsGroup(primaryGroup.getId());
            HttpRequest<?> request = HttpRequest.POST("/applications/save", applicationOne);
            HttpResponse<?> response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationOne = response.getBody(Application.class).get();

            Application applicationTwo = new Application();
            applicationTwo.setName("ApplicationTwo");
            applicationTwo.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationTwo);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationTwo = response.getBody(Application.class).get();

            applicationTwo.setName(applicationOne.getName());
            request = HttpRequest.POST("/applications/save", applicationTwo);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(SEE_OTHER, response.getStatus());
            Application application = response.getBody(Application.class).get();

            assertEquals(applicationOne.getId(), application.getId());
        }

        @Test
        public void canDeleteFromGroup() {
            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            Application application = new Application();
            application.setName("ApplicationDelete");
            application.setPermissionsGroup(primaryGroup.getId());
            HttpRequest<?> request = HttpRequest.POST("/applications/save", application);
            HttpResponse<?> response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();


            request = HttpRequest.POST("/applications/delete/" + application.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }
    }

    @Nested
    class WhenAsAnApplicationAdmin {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("Max", "Montes", "montesm@test.test"));
            userRepository.save(new User("Justin", "Jones", "jjones@test.test"));
        }

        void loginAsNonAdmin() {
            mockSecurityService.setServerAuthentication(new ServerAuthentication(
                    "jjones@test.test",
                    Collections.emptyList(),
                    Map.of("isAdmin", false)
            ));
        }

        @Test
        public void cannotCreateWithoutGroupSpecified() {
            loginAsNonAdmin();
            Application application = new Application();
            application.setName("TestApplication");
            HttpRequest<?> request = HttpRequest.POST("/applications/save", application);
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
        }

        @Test
        public void cannotCreateIfNotMemberOfGroup() {
//            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            mockSecurityService.postConstruct();  // create Application as Admin

            // save group without members
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

//            createUser();
            loginAsNonAdmin();

            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }

        @Test
        public void canCreate() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as an application admin
            request = HttpRequest.POST("/groups/add_member/"+primaryGroup.getId()+"/"+justin.getId(),
                    Map.of("isApplicationAdmin", true));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // create application with above group
            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(Application.class).isPresent());
        }

        @Test
        public void cannotCreateIfNonApplicationAdminMemberOfGroup() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as an application admin
            request = HttpRequest.POST("/groups/add_member/"+primaryGroup.getId()+"/"+justin.getId(),
                    Map.of("isTopicAdmin", true));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // create application with above group
            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }


        @Test
        @Disabled // out of scope for UFP-526; need requirements
        public void canViewApplicationsAsNonAdminOfGroup() {
        }

        @Test
        @Disabled // out of scope for UFP-526; need requirements
        public void canViewApplicationsAsApplicationAdminOfGroup() {
        }

        @Test
        public void canUpdateApplicationName() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as an application admin
            request = HttpRequest.POST("/groups/add_member/"+primaryGroup.getId()+"/"+justin.getId(),
                    Map.of("isApplicationAdmin", true));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add application to group
            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(Application.class).isPresent());
            application = response.getBody(Application.class).get();

            loginAsNonAdmin();

            application.setName("TestApplicationUpdate");
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();

            assertEquals("TestApplicationUpdate", application.getName());
        }

        @Test
        public void canUpdateApplicationGroupIfTargetGroupApplicationAdmin() {
            mockSecurityService.postConstruct();

            // create two groups
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to groups as an application admin
            request = HttpRequest.POST("/groups/add_member/"+primaryGroup.getId()+"/"+justin.getId(),
                    Map.of("isApplicationAdmin", true));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/groups/add_member/"+secondaryGroup.getId()+"/"+justin.getId(),
                    Map.of("isApplicationAdmin", true));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add application to group
            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(Application.class).isPresent());
            application = response.getBody(Application.class).get();

            loginAsNonAdmin();

            // change the application to the other group
            application.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();

            assertEquals(secondaryGroup.getId(), application.getPermissionsGroup());
        }

        @Test
        public void cannotUpdateApplicationGroupIfNotApplicationAdminOfTargetGroup() {
            mockSecurityService.postConstruct();

            // create two groups
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to groups as an application admin
            request = HttpRequest.POST("/groups/add_member/"+primaryGroup.getId()+"/"+justin.getId(),
                    Map.of("isApplicationAdmin", true));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/groups/add_member/"+secondaryGroup.getId()+"/"+justin.getId(),
                    Map.of("isTopicAdmin", true));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add application to group
            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(Application.class).isPresent());
            application = response.getBody(Application.class).get();

            loginAsNonAdmin();

            // change application.group to the other group
            application.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }

        @Test
        public void canDeleteFromGroup() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as an application admin
            request = HttpRequest.POST("/groups/add_member/"+primaryGroup.getId()+"/"+justin.getId(),
                    Map.of("isApplicationAdmin", true));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add application to group
            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(Application.class).isPresent());
            application = response.getBody(Application.class).get();

            loginAsNonAdmin();

            request = HttpRequest.POST("/applications/delete/" + application.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }
    }

    @Nested
    class WhenAsNonAdmin {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("Max", "Montes", "montesm@test.test"));
            userRepository.save(new User("Justin", "Jones", "jjones@test.test"));
        }

        void loginAsNonAdmin() {
            mockSecurityService.setServerAuthentication(new ServerAuthentication(
                    "jjones@test.test",
                    Collections.emptyList(),
                    Map.of("isAdmin", false)
            ));
        }

        @Test
        public void cannotCreateWithoutGroupSpecified() {
            loginAsNonAdmin();
            Application application = new Application();
            application.setName("TestApplication");
            HttpRequest<?> request = HttpRequest.POST("/applications/save", application);
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
        }

        @Test
        public void cannotCreateApplicationWithGroupSpecified() {
//            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            mockSecurityService.postConstruct();  // create Application as Admin

            // save group without members
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            loginAsNonAdmin();

            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }

        @Test
        public void cannotCreateApplicationWithSameNameAsAnotherInSameGroup() {
//            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            mockSecurityService.postConstruct();

            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Application applicationOne = new Application();
            applicationOne.setName("ApplicationOne");
            applicationOne.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationOne);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationOne = response.getBody(Application.class).get();

            loginAsNonAdmin();

            request = HttpRequest.POST("/applications/save", applicationOne);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<Application> application = exception.getResponse().getBody(Application.class);
            assertTrue(application.isEmpty());
        }

        @Test
        @Disabled // out of scope for UFP-526; need requirements
        public void cannotViewAllApplications() {

            mockSecurityService.postConstruct();

            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Application applicationOne = new Application();
            applicationOne.setName("ApplicationOne");
            applicationOne.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationOne);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationOne = response.getBody(Application.class).get();

            loginAsNonAdmin();

            request = HttpRequest.GET("/applications");
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            List<Map> applications = (List<Map>) responseMap.get("content");
            assertEquals(0, applications.size());
        }

        @Test
        public void cannotUpdateApplicationName() {
//            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            mockSecurityService.postConstruct();

            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();

            loginAsNonAdmin();

            application.setName("TestApplicationUpdate");
            request = HttpRequest.POST("/applications/save", application);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<Application> applicationUpdateAttempt = exception.getResponse().getBody(Application.class);
            assertTrue(applicationUpdateAttempt.isEmpty());
        }

        @Test
        public void cannotUpdateApplicationGroup() {
//            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));
//            Group secondaryGroup = groupRepository.save(new Group("SecondaryGroup"));

            mockSecurityService.postConstruct();  // create Application as Admin

            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            Application application = new Application();
            application.setName("TestApplication");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();

            loginAsNonAdmin(); // Attempt to create duplicate as non admin

            application.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<Application> applicationUpdateAttempt = exception.getResponse().getBody(Application.class);
            assertTrue(applicationUpdateAttempt.isEmpty());
        }

        @Test
        public void cannotUpdateGroupIfApplicationWithSameNameExistsInTargetGroup() {
//            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));
//            Group secondaryGroup = groupRepository.save(new Group("SecondaryGroup"));

            mockSecurityService.postConstruct();

            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            Application applicationOne = new Application();
            applicationOne.setName("TestApplication");
            applicationOne.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationOne);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationOne = response.getBody(Application.class).get();

            Application applicationTwo = new Application();
            applicationTwo.setName("TestApplication");
            applicationTwo.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationTwo);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationTwo = response.getBody(Application.class).get(); // 2

            loginAsNonAdmin();

            applicationOne.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationOne);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<Application> applicationUpdateAttempt = exception.getResponse().getBody(Application.class);
            assertTrue(applicationUpdateAttempt.isEmpty());
        }

        @Test
        public void cannotUpdateApplicationNameIfOneAlreadyExistsInSameGroup() {
//            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            mockSecurityService.postConstruct();

            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Application applicationOne = new Application();
            applicationOne.setName("ApplicationOne");
            applicationOne.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationOne);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationOne = response.getBody(Application.class).get();

            Application applicationTwo = new Application();
            applicationTwo.setName("ApplicationTwo");
            applicationTwo.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", applicationTwo);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            applicationTwo = response.getBody(Application.class).get();

            loginAsNonAdmin(); // Attempt to create duplicate as non admin

            applicationTwo.setName(applicationOne.getName());
            request = HttpRequest.POST("/applications/save", applicationTwo);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<Application> applicationUpdateAttempt = exception.getResponse().getBody(Application.class);
            assertTrue(applicationUpdateAttempt.isEmpty());
        }

        @Test
        public void cannotDeleteFromGroup() {
//            Group primaryGroup = groupRepository.save(new Group("PrimaryGroup"));

            mockSecurityService.postConstruct();  // create Application as Admin

            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Application application = new Application();
            application.setName("ApplicationDelete");
            application.setPermissionsGroup(primaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, Application.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(Application.class).get();

            loginAsNonAdmin(); // Attempt to create duplicate as non admin

            request = HttpRequest.POST("/applications/delete/" + application.getId(), Map.of());
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }
    }
}
