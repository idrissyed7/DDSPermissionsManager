package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Page;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator;
import io.micronaut.security.token.jwt.generator.claims.JWTClaimsSetGenerator;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.*;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
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

import static io.micronaut.http.HttpStatus.*;
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
    GroupUserRepository groupUserRepository;

    @Inject
    ApplicationPermissionRepository applicationPermissionRepository;

    @Inject
    ApplicationRepository applicationRepository;

    @Inject
    TopicRepository topicRepository;

    @Inject
    JWTClaimsSetGenerator jwtClaimsSetGenerator;

    @Inject
    JwtTokenGenerator jwtTokenGenerator;

    @Inject
    DbCleanup dbCleanup;

    @Inject
    @Client("/api")
    HttpClient client;

    @Inject
    MockAuthenticationFetcher mockAuthenticationFetcher;

    @Inject
    MockSecretSignature mockJwtSecret;

    @BeforeEach
    void setup() {
        blockingClient = client.toBlocking();
    }

    @Nested
    class WhenAsAdmin {

        private Group testGroup;
        private Topic testTopic;
        private Application applicationOne;

        private Group publicGroup;
        private Application privateApplication;
        private Topic publicTopic;

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test", true));

            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            testGroup = groupRepository.save(new Group("TestGroup"));
            testTopic = topicRepository.save(new Topic("TestTopic", TopicKind.B, testGroup));
            applicationOne = applicationRepository.save(new Application("ApplicationOne", testGroup));

            publicGroup = groupRepository.save(new Group("PublicGroup", "Description", true));
            publicTopic = topicRepository.save(new Topic("TestTopic", TopicKind.B, "topic desc", true, publicGroup));
            privateApplication = applicationRepository.save(new Application("ApplicationOne", publicGroup, "app desc", false));
        }

        @Test
        public void permissionIsDeletedPostTopicDeletion() {

            HttpResponse<?> response;
            HttpRequest<?> request;

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

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationOptional.get().getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            // create topic
            response = createTopic("Topic123", TopicKind.C, primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topicOptional = response.getBody(TopicDTO.class);
            assertTrue(topicOptional.isPresent());
            assertEquals("Topic123", topicOptional.get().getName());

            // create app permission
            response = createApplicationPermission(applicationBindToken, topicOptional.get().getId(), AccessType.READ);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> permissionOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(permissionOptional.isPresent());

            // topic delete
            request = HttpRequest.POST("/topics/delete/"+topicOptional.get().getId(), Map.of());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);

            assertTrue(applicationPermissionRepository.findById(permissionOptional.get().getId()).isEmpty());
        }

        @Test
        public void canListPermissionsNotAMemberOfTopicOrApplicationGroups() {
            Long applicationOneId = applicationOne.getId();
            Long testTopicId = testTopic.getId();
            addReadPermission(applicationOneId, testTopicId);

            HttpRequest<?> request = HttpRequest.GET("/application_permissions/application/" + applicationOneId);
            Page page = blockingClient.retrieve(request,  Page.class);
            assertFalse(page.isEmpty());

            request = HttpRequest.GET("/application_permissions/topic/" + testTopicId);
            page = blockingClient.retrieve(request,  Page.class);
            assertFalse(page.isEmpty());
        }

        @Test
        public void permissionIsDeletedPostApplicationDeletion() {

            HttpResponse<?> response;
            HttpRequest<?> request;

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

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationOptional.get().getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            // create topic
            response = createTopic("Topic123", TopicKind.C, primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topicOptional = response.getBody(TopicDTO.class);
            assertTrue(topicOptional.isPresent());
            assertEquals("Topic123", topicOptional.get().getName());

            // create app permission
            response = createApplicationPermission(applicationBindToken, topicOptional.get().getId(), AccessType.READ);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> permissionOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(permissionOptional.isPresent());

            // application delete
            request = HttpRequest.POST("/applications/delete/"+applicationOptional.get().getId(), Map.of());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);

            assertTrue(applicationPermissionRepository.findById(permissionOptional.get().getId()).isEmpty());
        }

        @Test
        public void canUpdatePermissions() {
            Long applicationOneId = applicationOne.getId();
            Long testTopicId = testTopic.getId();

            addReadPermission(applicationOneId, testTopicId);

            HttpRequest<?> request = HttpRequest.GET("/application_permissions/application/" + applicationOneId);
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
            assertTrue(content.stream().anyMatch((m) -> "READ".equals(m.get("accessType"))));
            assertFalse(content.stream().anyMatch((m) -> "READ_WRITE".equals(m.get("accessType"))));
            assertFalse(content.stream().anyMatch((m) -> "WRITE".equals(m.get("accessType"))));
            int permissionId = (int) content.get(0).get("id");

            updatePermission(permissionId, AccessType.WRITE);

            request = HttpRequest.GET("/application_permissions/application/" + applicationOneId);
            responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
            assertFalse(content.stream().anyMatch((m) -> "READ".equals(m.get("accessType"))));
            assertFalse(content.stream().anyMatch((m) -> "READ_WRITE".equals(m.get("accessType"))));
            assertTrue(content.stream().anyMatch((m) -> "WRITE".equals(m.get("accessType"))));

            request = HttpRequest.DELETE("/application_permissions/" + permissionId);
            HttpResponse<Object> response = blockingClient.exchange(request);
            assertEquals(HttpStatus.NO_CONTENT, response.getStatus());

            request = HttpRequest.GET("/application_permissions/application/" + applicationOneId);
            responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            content = (List<Map>) responseMap.get("content");
            assertNull(content);
        }

        @Test
        public void attemptToAssociateApplicationWithInvalidApplicationJwtToken() {
            HttpResponse<?> response;
            HttpRequest<?> request;

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

            // invalid jwt token
            Map<String, Object> stringObjectMap = jwtClaimsSetGenerator.generateClaimsSet(Map.of("myKey", "myVal"), 5000);
            Optional<String> s = jwtTokenGenerator.generateToken(stringObjectMap);

            request = HttpRequest.POST("/application_permissions/" + topicOptional.get().getId() + "/" + AccessType.READ.name(), Map.of())
                    .header(ApplicationPermissionService.APPLICATION_BIND_TOKEN, s.get());
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request, AccessPermissionDTO.class);
            });
            assertEquals(FORBIDDEN, exception.getStatus());
        }

        @Test
        public void tokenWithInvalidSignatureSecretShouldFail() {
            HttpResponse<?> response;
            HttpRequest<?> request;

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

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationOptional.get().getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            // create topic
            response = createTopic("Topic123", TopicKind.C, primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topicOptional = response.getBody(TopicDTO.class);
            assertTrue(topicOptional.isPresent());
            assertEquals("Topic123", topicOptional.get().getName());

            // change signature secret
            mockJwtSecret.setSecret("thisIsASecretThatIsInvalidAndIsMoreThan256BitsLong");

            // create app permission
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createApplicationPermission(applicationBindToken, topicOptional.get().getId(), AccessType.READ);
            });
            assertEquals(FORBIDDEN, exception.getStatus());
        }

        @Test
        public void canDeleteApplicationPermission() {
            HttpResponse<?> response;
            HttpRequest<?> request;

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

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationOptional.get().getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            // create application permission
            request = HttpRequest.POST("/application_permissions/" + topicOptional.get().getId() + "/" + AccessType.READ.name(), Map.of())
                    .header(ApplicationPermissionService.APPLICATION_BIND_TOKEN, applicationBindToken);
            response = blockingClient.exchange(request, AccessPermissionDTO.class);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> accessPermissionDTOOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(accessPermissionDTOOptional.isPresent());

            // application permissions delete
            request = HttpRequest.DELETE("/application_permissions/"+accessPermissionDTOOptional.get().getId());
            response = blockingClient.exchange(request, HashMap.class);
            assertEquals(NO_CONTENT, response.getStatus());
        }

        @Test
        public void cannotCreateDuplicateEntries() {
            HttpResponse<?> response;
            HttpRequest<?> request;

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

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationOptional.get().getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            // create topic
            response = createTopic("Topic123", TopicKind.C, primaryGroup.getId());
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topicOptional = response.getBody(TopicDTO.class);
            assertTrue(topicOptional.isPresent());
            assertEquals("Topic123", topicOptional.get().getName());

            // create app permission
            response = createApplicationPermission(applicationBindToken, topicOptional.get().getId(), AccessType.READ);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> permissionOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(permissionOptional.isPresent());

            // second create attempt
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createApplicationPermission(applicationBindToken, topicOptional.get().getId(), AccessType.WRITE);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
            Optional<List> body = exception.getResponse().getBody(List.class);
            assertTrue(body.isPresent());
            List<Map> list = body.get();
            assertTrue(list.stream().anyMatch(group -> ResponseStatusCodes.APPLICATION_PERMISSION_ALREADY_EXISTS.equals(group.get("code"))));
        }

        @Test
        public void canViewAllApplicationPermissionsByApplication() {
            HttpResponse response;
            HttpRequest request;

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationOne.getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            response = createApplicationPermission(applicationBindToken, testTopic.getId(), AccessType.READ);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> permissionOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(permissionOptional.isPresent());

            request = HttpRequest.GET("/application_permissions/application/"+applicationOne.getId());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
            assertEquals(permissionOptional.get().getId().intValue(), content.get(0).get("id"));
        }

        @Test
        public void canViewAllApplicationPermissionsByTopic() {
            HttpResponse response;
            HttpRequest request;

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationOne.getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            response = createApplicationPermission(applicationBindToken, testTopic.getId(), AccessType.READ);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> permissionOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(permissionOptional.isPresent());

            request = HttpRequest.GET("/application_permissions/topic/"+testTopic.getId());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
            assertEquals(permissionOptional.get().getId().intValue(), content.get(0).get("id"));
        }

        @Test
        public void canViewAllApplicationPermissionsByTopicIfPublicTopicPrivateGroup() {
            HttpResponse response;
            HttpRequest request;

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + privateApplication.getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            // create permission
            response = createApplicationPermission(applicationBindToken, publicTopic.getId(), AccessType.READ);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> permissionOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(permissionOptional.isPresent());

            request = HttpRequest.GET("/application_permissions/topic/"+publicTopic.getId());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
            assertEquals(permissionOptional.get().getId().intValue(), content.get(0).get("id"));
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

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationId);
            HttpResponse<String> response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            request = HttpRequest.POST("/application_permissions/" + topicId + "/" + accessType.name(), Map.of())
                    .header(ApplicationPermissionService.APPLICATION_BIND_TOKEN, applicationBindToken);
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

        private Group testGroup;
        private Topic testTopic;
        private Application applicationOne;
        private ApplicationPermission applicationPermissionOne;
        private Group testGroupTwo;
        private Topic testTopicTwo;
        private Application applicationTwo;
        private ApplicationPermission applicationPermissionTwo;

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test"));
            User justin = userRepository.save(new User("jjones@test.test"));

            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            testGroup = groupRepository.save(new Group("TestGroup"));
            testTopic = topicRepository.save(new Topic("TestTopic", TopicKind.B, testGroup));
            applicationOne = applicationRepository.save(new Application("ApplicationOne", testGroup));
            applicationPermissionOne = applicationPermissionRepository.save(new ApplicationPermission(applicationOne, testTopic, AccessType.READ_WRITE));
            GroupUser membership = new GroupUser(testGroup, justin);
            membership.setApplicationAdmin(true);
            groupUserRepository.save(membership);

            testGroupTwo = groupRepository.save(new Group("TestGroup1"));
            testTopicTwo = topicRepository.save(new Topic("TestTopic1", TopicKind.C, testGroupTwo));
            applicationTwo = applicationRepository.save(new Application("ApplicationTwo", testGroupTwo));
            applicationPermissionTwo = applicationPermissionRepository.save(new ApplicationPermission(applicationTwo, testTopicTwo, AccessType.WRITE));
        }

        void loginAsApplicationAdmin() {
            mockSecurityService.setServerAuthentication(new ServerAuthentication(
                    "jjones@test.test",
                    Collections.emptyList(),
                    Map.of()
            ));
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        @Test
        public void canDeleteApplicationPermission() {
            HttpResponse<?> response;
            HttpRequest<?> request;

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

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationOptional.get().getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            // create application permission
            request = HttpRequest.POST("/application_permissions/" + topicOptional.get().getId() + "/" + AccessType.READ.name(), Map.of())
                    .header(ApplicationPermissionService.APPLICATION_BIND_TOKEN, applicationBindToken);
            response = blockingClient.exchange(request, AccessPermissionDTO.class);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> accessPermissionDTOOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(accessPermissionDTOOptional.isPresent());

            loginAsApplicationAdmin();

            // application permissions delete
            request = HttpRequest.DELETE("/application_permissions/"+accessPermissionDTOOptional.get().getId());
            response = blockingClient.exchange(request, HashMap.class);
            assertEquals(NO_CONTENT, response.getStatus());
        }

        @Test
        public void canOnlyListPermissionsOfGroupsUserIsAMemberOfByApplication() {
            HttpResponse response;
            HttpRequest request;

            loginAsApplicationAdmin();

            request = HttpRequest.GET("/application_permissions/application/"+applicationOne.getId());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
            assertEquals(applicationPermissionOne.getId().intValue(), content.get(0).get("id"));
        }

        @Test
        public void canOnlyListPermissionsOfGroupsUserIsAMemberOfByTopic() {
            HttpResponse response;
            HttpRequest request;

            loginAsApplicationAdmin();

            request = HttpRequest.GET("/application_permissions/topic/"+testTopic.getId());
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            assertNotNull(responseMap);
            List<Map> content = (List<Map>) responseMap.get("content");
            assertEquals(1, content.size());
            assertEquals(applicationPermissionOne.getId().intValue(), content.get(0).get("id"));
        }
    }

    @Nested
    class WhenAsATopicAdmin {

        private Group testGroup;
        private Topic testTopic;
        private Application applicationOne;
        private ApplicationPermission applicationPermissionOne;
        private Group testGroupTwo;
        private Topic testTopicTwo;
        private Application applicationTwo;
        private ApplicationPermission applicationPermissionTwo;

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test"));
            User justin = userRepository.save(new User("jjones@test.test"));

            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            testGroup = groupRepository.save(new Group("TestGroup"));
            testTopic = topicRepository.save(new Topic("TestTopic", TopicKind.B, testGroup));
            applicationOne = applicationRepository.save(new Application("ApplicationOne", testGroup));
            applicationPermissionOne = applicationPermissionRepository.save(new ApplicationPermission(applicationOne, testTopic, AccessType.READ_WRITE));
            GroupUser membership = new GroupUser(testGroup, justin);
            membership.setTopicAdmin(true);
            groupUserRepository.save(membership);

            testGroupTwo = groupRepository.save(new Group("TestGroup1"));
            testTopicTwo = topicRepository.save(new Topic("TestTopic1", TopicKind.C, testGroupTwo));
            applicationTwo = applicationRepository.save(new Application("ApplicationTwo", testGroupTwo));
            applicationPermissionTwo = applicationPermissionRepository.save(new ApplicationPermission(applicationTwo, testTopicTwo, AccessType.WRITE));
        }

        void loginAsTopicAdmin() {
            mockSecurityService.setServerAuthentication(new ServerAuthentication(
                    "jjones@test.test",
                    Collections.emptyList(),
                    Map.of()
            ));
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        @Test
        public void canDeleteApplicationPermission() {
            HttpResponse<?> response;
            HttpRequest<?> request;

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
            dto.setTopicAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

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

            // generate bind token for application
            request = HttpRequest.GET("/applications/generate_bind_token/" + applicationOptional.get().getId());
            response = blockingClient.exchange(request, String.class);
            assertEquals(OK, response.getStatus());
            Optional<String> optional = response.getBody(String.class);
            assertTrue(optional.isPresent());
            String applicationBindToken = optional.get();

            // create application permission
            request = HttpRequest.POST("/application_permissions/" + topicOptional.get().getId() + "/" + AccessType.READ.name(), Map.of())
                    .header(ApplicationPermissionService.APPLICATION_BIND_TOKEN, applicationBindToken);
            response = blockingClient.exchange(request, AccessPermissionDTO.class);
            assertEquals(CREATED, response.getStatus());
            Optional<AccessPermissionDTO> accessPermissionDTOOptional = response.getBody(AccessPermissionDTO.class);
            assertTrue(accessPermissionDTOOptional.isPresent());

            loginAsTopicAdmin();

            // application permissions delete
            request = HttpRequest.DELETE("/application_permissions/"+accessPermissionDTOOptional.get().getId());
            response = blockingClient.exchange(request, HashMap.class);
            assertEquals(NO_CONTENT, response.getStatus());
        }

        @Test
        public void canListPermissionsOfGroupsUserIsAMemberOf() {
            HttpRequest<?> request;

            loginAsTopicAdmin();

            request = HttpRequest.GET("/application_permissions/application/"+applicationOne.getId());
            Page page = blockingClient.retrieve(request,  Page.class);
            assertFalse(page.isEmpty());
            List content = page.getContent();
            assertNotNull(content);
            assertEquals(1, content.size());
            Map m = (Map) content.get(0);
            assertEquals(applicationPermissionOne.getId().intValue(), m.get("id"));
        }
    }

    @Nested
    class WhenAsNonAdmin {

        private ApplicationPermission applicationPermissionOne;
        private Application publicApplication;
        private Topic publicTopic;
        private Application privateApplication;

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test"));
            userRepository.save(new User("jjones@test.test"));

            Group group = groupRepository.save(new Group("TestGroup"));
            Topic topic = topicRepository.save(new Topic("TestTopic", TopicKind.B, group));
            Application application = applicationRepository.save(new Application("ApplicationOne", group));
            applicationPermissionOne = applicationPermissionRepository.save(new ApplicationPermission(application, topic, AccessType.READ_WRITE));

            Group publicGroup = groupRepository.save(new Group("TestGroup1", "group desc", true));
            publicTopic = topicRepository.save(new Topic("TestTopic1", TopicKind.C, "topic description", true, publicGroup));
            publicApplication = applicationRepository.save(new Application("ApplicationTwo", publicGroup, "topic description", true));
            applicationPermissionRepository.save(new ApplicationPermission(publicApplication, publicTopic, AccessType.READ_WRITE));

            privateApplication = applicationRepository.save(new Application("ApplicationThree", group, "application description", false));
            applicationPermissionRepository.save(new ApplicationPermission(privateApplication, publicTopic, AccessType.READ_WRITE));
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
        public void canViewPublicApplicationPermissions() {
            loginAsNonAdmin();

            HttpRequest<?> request;
            Page page;

            // public permissions with public application
            request = HttpRequest.GET("/application_permissions/application/"+publicApplication.getId());
            page = blockingClient.retrieve(request, Page.class);
            assertFalse(page.isEmpty());
            assertEquals(1, page.getContent().size());
            assertTrue(page.getContent().stream().allMatch(o -> {
                Map map1 = (Map) o;
                String topic = (String) map1.get("topicName");
                String application = (String) map1.get("applicationName");
                return topic.contentEquals("TestTopic1") && application.contentEquals("ApplicationTwo");
            }));

            // public permissions call with private application - should not yield results
            request = HttpRequest.GET("/application_permissions/application/"+privateApplication.getId());
            page = blockingClient.retrieve(request, Page.class);
            assertTrue(page.isEmpty());

            // public permissions call with public topic - should yield one permission (the one with public application association)
            request = HttpRequest.GET("/application_permissions/topic/"+publicTopic.getId());
            page = blockingClient.retrieve(request, Page.class);
            assertFalse(page.isEmpty());
            assertEquals(1, page.getContent().size());
            assertTrue(page.getContent().stream().allMatch(o -> {
                Map map1 = (Map) o;
                String topic = (String) map1.get("topicName");
                String application = (String) map1.get("applicationName");
                return topic.contentEquals("TestTopic1") && application.contentEquals("ApplicationTwo");
            }));
        }

        @Test
        public void cannotDeleteApplicationPermission() {
            HttpResponse<?> response;
            HttpRequest<?> request;

            loginAsNonAdmin();

            // application permissions delete
            request = HttpRequest.DELETE("/application_permissions/"+applicationPermissionOne.getId());
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
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

    private HttpResponse<?> createApplicationPermission(String applicationBindToken, Long topicId, AccessType accessType) {
        HttpRequest<?> request = HttpRequest.POST("/application_permissions/" + topicId + "/" + accessType.name(), Map.of())
                .header(ApplicationPermissionService.APPLICATION_BIND_TOKEN, applicationBindToken);
        return blockingClient.exchange(request, AccessPermissionDTO.class);
    }
}
