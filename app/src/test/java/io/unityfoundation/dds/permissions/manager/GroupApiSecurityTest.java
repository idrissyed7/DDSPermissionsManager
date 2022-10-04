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
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.AccessType;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermission;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupAdminRole;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.testing.util.DbCleanup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class GroupApiSecurityTest {

    private BlockingHttpClient blockingClient;

    @Inject
    MockSecurityService mockSecurityService;

    @Inject
    MockAuthenticationFetcher mockAuthenticationFetcher;

    @Inject
    GroupRepository groupRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    ApplicationPermissionRepository applicationPermissionRepository;

    @Inject
    ApplicationRepository applicationRepository;

    @Inject
    TopicRepository topicRepository;

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
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        @Test
        void canSearchAndShouldYieldAll(){
            HttpResponse response;

            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(Group.class).isPresent());

            response = createGroup("SecondaryGroup");
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(Group.class).isPresent());

            HttpRequest<?> request = HttpRequest.GET("/groups/search/Group");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> optionalPage = response.getBody(Page.class);
            assertTrue(optionalPage.isPresent());
            assertEquals(2, optionalPage.get().getContent().size());

            // search text is null
            request = HttpRequest.GET("/groups/search/");
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest, Page.class);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
        }
    }

    @Nested
    class WhenAsAGroupAdmin {

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
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        @Test
        void canSearchAndShouldYieldOnlyGroupsWhereGroupAdmin() {
            HttpResponse response;
            HttpRequest<?> request;

            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());

            response = createGroup("SecondaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> secondaryOptional = response.getBody(Group.class);
            assertTrue(secondaryOptional.isPresent());

            // add membership
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryOptional.get().getId());
            dto.setEmail("jjones@test.test");
            dto.setGroupAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            dto.setPermissionsGroup(secondaryOptional.get().getId());
            dto.setGroupAdmin(false);
            dto.setApplicationAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // should only see groups that current user is a group admin of
            request = HttpRequest.GET("/groups/search/Group?role="+ GroupAdminRole.GROUP_ADMIN);
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> optionalPage = response.getBody(Page.class);
            assertTrue(optionalPage.isPresent());
            assertEquals(1, optionalPage.get().getContent().size());
            Map groupMap = (Map) optionalPage.get().getContent().get(0);
            assertEquals("PrimaryGroup", groupMap.get("name"));

            // should only see groups that current user is an application admin of
            request = HttpRequest.GET("/groups/search/Group?role="+ GroupAdminRole.APPLICATION_ADMIN);
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            optionalPage = response.getBody(Page.class);
            assertTrue(optionalPage.isPresent());
            assertEquals(1, optionalPage.get().getContent().size());
            groupMap = (Map) optionalPage.get().getContent().get(0);
            assertEquals("SecondaryGroup", groupMap.get("name"));
        }
    }

    @Nested
    class WhenAsARegularUser {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test"));
            userRepository.save(new User("jjones@test.test"));

            Group group = groupRepository.save(new Group("TestGroup"));
            Topic topic = topicRepository.save(new Topic("TestTopic", TopicKind.B, group));
            Application application = applicationRepository.save(new Application("ApplicationOne", group));
            applicationPermissionRepository.save(new ApplicationPermission(application, topic, AccessType.READ_WRITE));

            Group group1 = groupRepository.save(new Group("TestGroup1"));
            Topic topic1 = topicRepository.save(new Topic("TestTopic1", TopicKind.C, group1));
            Application application1 = applicationRepository.save(new Application("ApplicationTwo", group1));
            applicationPermissionRepository.save(new ApplicationPermission(application1, topic1, AccessType.READ_WRITE));
        }

        void loginAsNonAdmin() {
            mockSecurityService.setServerAuthentication(new ServerAuthentication(
                    "jjones@test.test",
                    Collections.emptyList(),
                    Map.of("isAdmin", false)
            ));
        }


    }

    private HttpResponse<?> createGroup(String groupName) {
        Group group = new Group(groupName);
        HttpRequest<?> request = HttpRequest.POST("/groups/save", group);
        return blockingClient.exchange(request, Group.class);
    }
}
