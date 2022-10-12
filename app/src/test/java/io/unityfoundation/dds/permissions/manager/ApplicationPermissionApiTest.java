package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.AccessPermissionDTO;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.AccessType;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermission;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicDTO;
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

import static io.micronaut.http.HttpStatus.CREATED;
import static io.micronaut.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class ApplicationPermissionApiTest {

    private BlockingHttpClient blockingClient;

    @Inject
    MockSecurityService mockSecurityService;

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

    @Inject
    MockAuthenticationFetcher mockAuthenticationFetcher;

    @BeforeEach
    void setup() {
        blockingClient = client.toBlocking();
    }

    @Nested
    class WhenAsAdmin {

        private Group testGroup;
        private Topic testTopic;
        private Application applicationOne;

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test", true));

            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            testGroup = groupRepository.save(new Group("TestGroup"));
            testTopic = topicRepository.save(new Topic("TestTopic", TopicKind.B, testGroup));
            applicationOne = applicationRepository.save(new Application("ApplicationOne", testGroup));
        }

        @Test
        public void permissionIsDeletedPostTopicDeletion() {

            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create application
            response = createApplication("Application123", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            assertEquals("Application123", applicationOptional.get().getName());

            // create topic
            response = createTopic("Topic123", TopicKind.C, primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topicOptional = response.getBody(TopicDTO.class);
            assertTrue(topicOptional.isPresent());
            assertEquals("Topic123", topicOptional.get().getName());

            // create app permission
            response = createApplicationPermission(applicationOptional.get().getId(), topicOptional.get().getId(), AccessType.READ);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> permissionOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(permissionOptional.isPresent());

            // topic delete
            HttpRequest<?> request = HttpRequest.POST("/topics/delete/"+topicOptional.get().getId(), Map.of());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);

            assertTrue(applicationPermissionRepository.findById(permissionOptional.get().getId()).isEmpty());
        }

        @Test
        public void permissionIsDeletedPostApplicationDeletion() {

            HttpResponse<?> response;

            // create groups
            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());
            Group primaryGroup = primaryOptional.get();

            // create application
            response = createApplication("Application123", primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOptional.isPresent());
            assertEquals("Application123", applicationOptional.get().getName());

            // create topic
            response = createTopic("Topic123", TopicKind.C, primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topicOptional = response.getBody(TopicDTO.class);
            assertTrue(topicOptional.isPresent());
            assertEquals("Topic123", topicOptional.get().getName());

            // create app permission
            response = createApplicationPermission(applicationOptional.get().getId(), topicOptional.get().getId(), AccessType.READ);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> permissionOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(permissionOptional.isPresent());

            // application delete
            HttpRequest<?> request = HttpRequest.POST("/applications/delete/"+applicationOptional.get().getId(), Map.of());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);

            assertTrue(applicationPermissionRepository.findById(permissionOptional.get().getId()).isEmpty());
        }

        @Test
        public void canUpdatePermissions() {
            Long applicationOneId = applicationOne.getId();
            Long testTopicId = testTopic.getId();

            addReadPermission(applicationOneId, testTopicId);

            HttpRequest<?> request = HttpRequest.GET("/application_permissions?applicationId=" + applicationOneId);
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
            assertTrue(content.stream().anyMatch((m) -> "READ".equals(m.get("accessType"))));
            assertFalse(content.stream().anyMatch((m) -> "READ_WRITE".equals(m.get("accessType"))));
            assertFalse(content.stream().anyMatch((m) -> "WRITE".equals(m.get("accessType"))));

            addReadWritePermission(applicationOneId, testTopicId);

            request = HttpRequest.GET("/application_permissions?applicationId=" + applicationOneId);
            responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            content = (List<Map>) responseMap.get("content");
            assertEquals(2, content.size());
            assertTrue(content.stream().anyMatch((m) -> "READ".equals(m.get("accessType"))));
            assertTrue(content.stream().anyMatch((m) -> "READ_WRITE".equals(m.get("accessType"))));
            assertFalse(content.stream().anyMatch((m) -> "WRITE".equals(m.get("accessType"))));
            int permissionId = (int) content.get(0).get("id");

            updatePermission(permissionId, AccessType.WRITE);

            request = HttpRequest.GET("/application_permissions?applicationId=" + applicationOneId);
            responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            content = (List<Map>) responseMap.get("content");
            assertEquals(2, content.size());
            assertFalse(content.stream().anyMatch((m) -> "READ".equals(m.get("accessType"))));
            assertTrue(content.stream().anyMatch((m) -> "READ_WRITE".equals(m.get("accessType"))));
            assertTrue(content.stream().anyMatch((m) -> "WRITE".equals(m.get("accessType"))));

            request = HttpRequest.DELETE("/application_permissions/" + permissionId);
            HttpResponse<Object> response = blockingClient.exchange(request);
            assertEquals(HttpStatus.NO_CONTENT, response.getStatus());

            request = HttpRequest.GET("/application_permissions?applicationId=" + applicationOneId);
            responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
            assertFalse(content.stream().anyMatch((m) -> "READ".equals(m.get("accessType"))));
            assertTrue(content.stream().anyMatch((m) -> "READ_WRITE".equals(m.get("accessType"))));
            assertFalse(content.stream().anyMatch((m) -> "WRITE".equals(m.get("accessType"))));
        }

        private void addReadWritePermission(Long applicationId, Long topicId) {
            addPermission(applicationId, topicId, AccessType.READ_WRITE);
        }

        private void addReadPermission(Long applicationId, Long topicId) {
            addPermission(applicationId, topicId, AccessType.READ);
        }

        private void addPermission(Long applicationId, Long topicId, AccessType accessType) {
            HttpRequest<?> request;
            HashMap<String, Object> responseMap;

            request = HttpRequest.POST("/application_permissions/" + applicationId + "/" + topicId + "/" + accessType.name(), Map.of());
            responseMap = blockingClient.retrieve(request, HashMap.class);

            assertNotNull(responseMap);

            assertEquals(accessType.name(), responseMap.get("accessType"));
            assertEquals(applicationId.intValue(), responseMap.get("applicationId"));
            assertEquals(topicId.intValue(), responseMap.get("topicId"));
        }

        private void updatePermission(int permissionId, AccessType accessType) {
            HttpRequest<?> request;
            HashMap<String, Object> responseMap;

            request = HttpRequest.PUT("/application_permissions/" + permissionId + "/" + accessType.name(), Map.of());
            responseMap = blockingClient.retrieve(request, HashMap.class);

            assertNotNull(responseMap);

            assertEquals(permissionId, responseMap.get("id"));
            assertEquals(accessType.name(), responseMap.get("accessType"));
        }
    }

    @Nested
    class WhenAsAnApplicationAdmin {

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
                    Map.of()
            ));
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }
    }

    @Nested
    class WhenAsNonAdmin {

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
                    Map.of()
            ));
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        @Test
        public void canViewAllApplicationPermissions() {
            loginAsNonAdmin();

            HttpRequest<?> request = HttpRequest.GET("/application_permissions");
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(2, content.size());
        }

        @Test
        public void canViewAllApplicationPermissionsByApplication() {
            loginAsNonAdmin();

            Group group = groupRepository.findByName("TestGroup").get();
            Application application = applicationRepository.findByNameAndPermissionsGroup("ApplicationOne", group).get();

            HttpRequest<?> request = HttpRequest.GET("/application_permissions?application="+application.getId());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
        }

        @Test
        public void canViewAllApplicationPermissionsByTopic() {
            loginAsNonAdmin();

            Topic topic = topicRepository.findByName("TestTopic").get();

            HttpRequest<?> request = HttpRequest.GET("/application_permissions?topic="+topic.getId());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
        }
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

    private HttpResponse<?> createTopic(String topicName, TopicKind topicKind, Long groupId) {
        TopicDTO topicDTO = new TopicDTO();
        topicDTO.setName(topicName);
        topicDTO.setGroup(groupId);
        topicDTO.setKind(topicKind);

        HttpRequest<?> request = HttpRequest.POST("/topics/save", topicDTO);
        return blockingClient.exchange(request, TopicDTO.class);
    }

    private HttpResponse<?> createApplicationPermission(Long applicationId, Long topicId, AccessType accessType) {
        HttpRequest<?> request = HttpRequest.POST("/application_permissions/" + applicationId + "/" + topicId + "/" + accessType.name(), Map.of());
        return blockingClient.exchange(request, AccessPermissionDTO.class);
    }
}
