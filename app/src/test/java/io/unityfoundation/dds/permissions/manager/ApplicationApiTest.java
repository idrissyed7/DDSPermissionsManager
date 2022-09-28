package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.data.model.Page;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.testing.util.DbCleanup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.*;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments={RunApplication.ENVIRONMENT_DEV_DATA})
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
    @Client("/api")
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
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createApplication("TestApplication", null);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
        }

        @Test
        public void canCreateApplicationWithGroupSpecified() {
            HttpResponse<?> response;

            // create group
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create application
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
        }

        @Test
        public void cannotCreateApplicationWithSameNameAsAnotherInSameGroup() {
            HttpResponse<?> response;

            // create group
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create application
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());

            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(SEE_OTHER, response.getStatus());
            ApplicationDTO application = response.getBody(ApplicationDTO.class).get();

            assertEquals(applicationOptional.get().getId(), application.getId());
        }

        @Test
        public void canCreateApplicationWithSameNameInAnotherGroup() {
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            response = createGroup("SecondaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> secondaryOptional = response.getBody(Group.class);
            assertTrue(secondaryOptional.isPresent());
            Group secondaryGroup = secondaryOptional.get();

            // create application
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());

            response = createApplication("TestApplication", secondaryGroup.getId());
            assertEquals(OK, response.getStatus());
        }

        @Test
        public void canViewAllApplications() {
            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            response = createGroup("SecondaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> secondaryOptional = response.getBody(Group.class);
            assertTrue(secondaryOptional.isPresent());
            Group secondaryGroup = secondaryOptional.get();

            // create applications
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());

            response = createApplication("TestApplication", secondaryGroup.getId());
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/applications");
            Page page = blockingClient.retrieve(request, Page.class);
            assertEquals(2, page.getContent().size());
        }

        @Test
        public void canUpdateApplicationName() {
            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create applications
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            ApplicationDTO application = applicationOptional.get();

            application.setName("TestApplicationUpdate");
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, ApplicationDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> updatedApplicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(updatedApplicationOptional.isPresent());
            ApplicationDTO updatedApplication = updatedApplicationOptional.get();

            assertEquals("TestApplicationUpdate", updatedApplication.getName());
        }

        @Test
        public void canUpdateApplicationGroup() {
            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            response = createGroup("SecondaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> secondaryOptional = response.getBody(Group.class);
            assertTrue(secondaryOptional.isPresent());
            Group secondaryGroup = secondaryOptional.get();

            // create applications
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            ApplicationDTO application = applicationOptional.get();

            application.setGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, ApplicationDTO.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(ApplicationDTO.class).get();

            assertEquals(secondaryGroup.getId(), application.getGroup());
        }

        @Test
        public void cannotUpdateApplicationNameIfOneAlreadyExistsInSameGroup() {
            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create applications
            response = createApplication("ApplicationOne", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOneOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOneOptional.isPresent());
            ApplicationDTO applicationOne = applicationOneOptional.get();

            response = createApplication("ApplicationTwo", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationTwoOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationTwoOptional.isPresent());
            ApplicationDTO applicationTwo = applicationTwoOptional.get();

            // update attempt
            applicationTwo.setName(applicationOne.getName());
            request = HttpRequest.POST("/applications/save", applicationTwo);
            response = blockingClient.exchange(request, ApplicationDTO.class);
            assertEquals(SEE_OTHER, response.getStatus());
            ApplicationDTO application = response.getBody(ApplicationDTO.class).get();

            assertEquals(applicationOne.getId(), application.getId());
        }

        @Test
        public void canDeleteFromGroup() {
            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create application
            response = createApplication("ApplicationOne", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOneOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOneOptional.isPresent());
            ApplicationDTO applicationOne = applicationOneOptional.get();

            // delete
            request = HttpRequest.POST("/applications/delete/" + applicationOne.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }
    }

    @Nested
    class WhenAsAnApplicationAdmin {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test", true));
            userRepository.save(new User("jjones@test.test"));
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
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createApplication("TestApplication", null);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
        }

        @Test
        public void cannotCreateIfNotMemberOfGroup() {
            HttpResponse<?> response;

            mockSecurityService.postConstruct();  // create Application as Admin

            // save group without members
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryGroupOptional = response.getBody(Group.class);
            assertTrue(primaryGroupOptional.isPresent());
            Group primaryGroup = primaryGroupOptional.get();

            loginAsNonAdmin();

            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createApplication("TestApplication", primaryGroup.getId());
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }

        @Test
        public void canCreate() {
            mockSecurityService.postConstruct();

            // create group
            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as an application admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setApplicationAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // create application with above group
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(ApplicationDTO.class).isPresent());
        }

        @Test
        public void cannotCreateIfNonApplicationAdminMemberOfGroup() {
            mockSecurityService.postConstruct();

            // create group
            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as an application admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setTopicAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // create application with above group
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createApplication("TestApplication", primaryGroup.getId());
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }


        @Test
        @Disabled(value = "out of scope for UFP-526; need requirements")
        public void canViewApplicationsAsNonAdminOfGroup() {
        }

        @Test
        @Disabled(value = "out of scope for UFP-526; need requirements")
        public void canViewApplicationsAsApplicationAdminOfGroup() {
        }

        @Test
        public void canUpdateApplicationName() {
            mockSecurityService.postConstruct();

            // create group
            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as an application admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setApplicationAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add application to group
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            ApplicationDTO application = applicationOptional.get();

            loginAsNonAdmin();

            application.setName("TestApplicationUpdate");
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, ApplicationDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationUpdateOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationUpdateOptional.isPresent());
            ApplicationDTO applicationUpdate = applicationUpdateOptional.get();

            assertEquals("TestApplicationUpdate", applicationUpdate.getName());
        }

        @Test
        public void canUpdateApplicationGroupIfTargetGroupApplicationAdmin() {
            mockSecurityService.postConstruct();

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create two groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Group primaryGroup = response.getBody(Group.class).get();

            response = createGroup("SecondaryGroup");
            assertEquals(OK, response.getStatus());
            Group secondaryGroup = response.getBody(Group.class).get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to groups as an application admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setApplicationAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            dto.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add application to group
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(ApplicationDTO.class).isPresent());
            ApplicationDTO application = response.getBody(ApplicationDTO.class).get();

            loginAsNonAdmin();

            // change the application to the other group
            application.setGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/applications/save", application);
            response = blockingClient.exchange(request, ApplicationDTO.class);
            assertEquals(OK, response.getStatus());
            application = response.getBody(ApplicationDTO.class).get();

            assertEquals(secondaryGroup.getId(), application.getGroup());
        }

        @Test
        public void cannotUpdateApplicationGroupIfNotApplicationAdminOfTargetGroup() {
            mockSecurityService.postConstruct();

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create two groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Group primaryGroup = response.getBody(Group.class).get();

            response = createGroup("SecondaryGroup");
            assertEquals(OK, response.getStatus());
            Group secondaryGroup = response.getBody(Group.class).get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to groups as an application admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setApplicationAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            dto.setPermissionsGroup(secondaryGroup.getId());
            dto.setApplicationAdmin(false);
            dto.setTopicAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add application to group
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(ApplicationDTO.class).isPresent());
            ApplicationDTO application = response.getBody(ApplicationDTO.class).get();

            loginAsNonAdmin();

            // change application.group to the other group
            application.setGroup(secondaryGroup.getId());
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

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create group
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Group primaryGroup = response.getBody(Group.class).get();

            // get user
            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as an application admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setApplicationAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add application to group
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(ApplicationDTO.class).isPresent());
            ApplicationDTO application = response.getBody(ApplicationDTO.class).get();

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
            userRepository.save(new User("montesm@test.test"));
            userRepository.save(new User("jjones@test.test"));
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
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createApplication("TestApplication", null);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
        }

        @Test
        public void cannotCreateApplicationWithGroupSpecified() {
            mockSecurityService.postConstruct();  // create Application as Admin

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            loginAsNonAdmin();

            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createApplication("TestApplication", primaryGroup.getId());
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }

        @Test
        public void cannotCreateApplicationWithSameNameAsAnotherInSameGroup() {

            mockSecurityService.postConstruct();

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create applications
            response = createApplication("ApplicationOne", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            ApplicationDTO applicationOne = applicationOptional.get();

            loginAsNonAdmin();

            request = HttpRequest.POST("/applications/save", applicationOne);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<ApplicationDTO> application = exception.getResponse().getBody(ApplicationDTO.class);
            assertTrue(application.isEmpty());
        }

        @Test
        @Disabled(value = "out of scope for UFP-526; need requirements")
        public void cannotViewAllApplications() {

            mockSecurityService.postConstruct();

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create applications
            response = createApplication("ApplicationOne", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            ApplicationDTO applicationOne = applicationOptional.get();

            loginAsNonAdmin();

            request = HttpRequest.GET("/applications");
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            List<Map> applications = (List<Map>) responseMap.get("content");
            assertEquals(0, applications.size());
        }

        @Test
        public void canViewApplicationDetails() {

            mockSecurityService.postConstruct();

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create applications
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            ApplicationDTO applicationOne = applicationOptional.get();

            loginAsNonAdmin();

            request = HttpRequest.GET("/applications/show/"+applicationOne.getId());
            ApplicationDTO applicationDTO = blockingClient.retrieve(request, ApplicationDTO.class);
            assertNotNull(applicationDTO);
        }

        @Test
        public void cannotUpdateApplicationName() {
            mockSecurityService.postConstruct();

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create applications
            response = createApplication("TestApplication", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            ApplicationDTO application = applicationOptional.get();

            loginAsNonAdmin();

            application.setName("TestApplicationUpdate");
            request = HttpRequest.POST("/applications/save", application);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<ApplicationDTO> applicationUpdateAttempt = exception.getResponse().getBody(ApplicationDTO.class);
            assertTrue(applicationUpdateAttempt.isEmpty());
        }

        @Test
        public void cannotUpdateApplicationNameIfOneAlreadyExistsInSameGroup() {
            mockSecurityService.postConstruct();

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create group
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create application
            response = createApplication("ApplicationOne", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOneOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOneOptional.isPresent());
            ApplicationDTO applicationOne = applicationOneOptional.get();

            response = createApplication("ApplicationTwo", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationTwoOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationTwoOptional.isPresent());
            ApplicationDTO applicationTwo = applicationTwoOptional.get();

            loginAsNonAdmin();

            applicationTwo.setName(applicationOne.getName());
            request = HttpRequest.POST("/applications/save", applicationTwo);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<ApplicationDTO> applicationUpdateAttempt = exception.getResponse().getBody(ApplicationDTO.class);
            assertTrue(applicationUpdateAttempt.isEmpty());
        }

        @Test
        public void cannotDeleteFromGroup() {
            mockSecurityService.postConstruct();

            HttpRequest<?> request;
            HttpResponse<?> response;

            // create group
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create application
            response = createApplication("ApplicationDelete", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            ApplicationDTO application = applicationOptional.get();

            loginAsNonAdmin();

            request = HttpRequest.POST("/applications/delete/" + application.getId(), Map.of());
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }
    }

    @Test
    public void testApplicationFilter() {
        HttpRequest<Object> request = HttpRequest.GET("/applications/search?filter=Application");
        List results = blockingClient.retrieve(request, List.class);
        assertEquals(2, results.size());

        request = HttpRequest.GET("/applications/search?filter=Application&size=1");
        results = blockingClient.retrieve(request, List.class);
        assertEquals(1, results.size());

        request = HttpRequest.GET("/applications/search?filter=Alpha");
        results = blockingClient.retrieve(request, List.class);
        assertEquals(2, results.size());

        request = HttpRequest.GET("/applications/search?filter=Alpha&size=1");
        results = blockingClient.retrieve(request, List.class);
        assertEquals(1, results.size());

        request = HttpRequest.GET("/applications/search?filter=Two");
        results = blockingClient.retrieve(request, List.class);
        assertEquals(1, results.size());
    }

    private HttpResponse<?> createGroup(String groupName) {
        Group group = new Group(groupName);
        HttpRequest<?> request = HttpRequest.POST("/groups/save", group);
        HttpResponse<?> response;
        return blockingClient.exchange(request, Group.class);
    }

    private HttpResponse<?> createApplication(String applicationName, Long groupId) {
        ApplicationDTO applicationDTO = new ApplicationDTO();
        applicationDTO.setName(applicationName);
        applicationDTO.setGroup(groupId);

        HttpRequest<?> request = HttpRequest.POST("/applications/save", applicationDTO);
        return blockingClient.exchange(request, ApplicationDTO.class);
    }
}
