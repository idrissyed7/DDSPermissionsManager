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
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.AccessType;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermission;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.ApplicationPermissionRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupAdminRole;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class GroupApiTest {

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
    GroupUserRepository groupUserRepository;

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

            User jjones = userRepository.save(new User("jjones@test.test"));
            User eclair = userRepository.save(new User("eclair@test.test"));

            Group groupOne = groupRepository.save(new Group("GroupOne"));
            Topic topicOne = topicRepository.save(new Topic("TopicOne", TopicKind.B, groupOne));
            Topic topicOne1 = topicRepository.save(new Topic("TopicOne!", TopicKind.C, groupOne));
            Application applicationOne = applicationRepository.save(new Application("ApplicationOne", groupOne));
            applicationPermissionRepository.save(new ApplicationPermission(applicationOne, topicOne, AccessType.READ_WRITE));
            groupUserRepository.save(new GroupUser(groupOne, jjones));
            groupUserRepository.save(new GroupUser(groupOne, eclair));

            Group groupTwo = groupRepository.save(new Group("GroupTwo"));
            Topic topicTwo = topicRepository.save(new Topic("TopicTwo", TopicKind.C, groupTwo));
            Application applicationTwo = applicationRepository.save(new Application("ApplicationTwo", groupTwo));
            Application applicationTwo1 = applicationRepository.save(new Application("ApplicationTwo1", groupTwo));
            applicationPermissionRepository.save(new ApplicationPermission(applicationTwo, topicTwo, AccessType.READ_WRITE));
            groupUserRepository.save(new GroupUser(groupTwo, eclair));
        }

        // create
        @Test
        void canCreateGroup(){
            HttpResponse<?> response = createGroup("Theta");
            assertEquals(OK, response.getStatus());
        }

        @Test
        public void cannotCreateGroupWithSameNameAsAnExistingGroup() {
            HttpResponse<?> response = createGroup("Theta");
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            Group theta = thetaOptional.get();

            response = createGroup("Theta");
            assertEquals(SEE_OTHER, response.getStatus());
            Optional<Group> groupDuplicate = response.getBody(Group.class);
            assertTrue(groupDuplicate.isPresent());
            assertEquals(theta.getId(), groupDuplicate.get().getId());
        }

        @Test
        public void cannotCreateGroupWithNullNorWhitespace() {
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createGroup(null);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());

            HttpClientResponseException exception1 = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createGroup("   ");
            });
            assertEquals(BAD_REQUEST, exception1.getStatus());
        }

        @Test
        public void createShouldTrimWhitespace() {
            HttpResponse<?> response = createGroup("   Theta  ");
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            Group theta = thetaOptional.get();
            assertEquals("Theta", theta.getName());
        }

        // update
        @Test
        void canUpdateGroup(){
            HttpResponse<?> response = createGroup("Theta");
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            Group theta = thetaOptional.get();

            theta.setName("ThetaTestUpdate");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaTestUpdateOptional = response.getBody(Group.class);
            assertTrue(thetaTestUpdateOptional.isPresent());
            assertEquals("ThetaTestUpdate", thetaTestUpdateOptional.get().getName());
        }

        @Test
        public void cannotUpdateGroupWithSameNameAsAnExistingGroup() {
            HttpResponse<?> response;
            HttpRequest<?> request;

            response = createGroup("Theta");
            assertEquals(OK, response.getStatus());

            response = createGroup("Beta");
            assertEquals(OK, response.getStatus());
            Optional<Group> betaOptional = response.getBody(Group.class);
            assertTrue(betaOptional.isPresent());
            Group beta = betaOptional.get();

            beta.setName("Theta");
            request = HttpRequest.POST("/groups/save", beta);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(BAD_REQUEST, thrown.getStatus());
        }

        // list
        @Test
        void canListAllGroups(){
            HttpResponse<?> response;
            HttpRequest<?> request;

            response = createGroup("Theta");
            assertEquals(OK, response.getStatus());

            response = createGroup("Beta");
            assertEquals(OK, response.getStatus());

            response = createGroup("Zeta");
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/groups");
            Page page = blockingClient.retrieve(request, Page.class);
            assertEquals(5, page.getContent().size());
        }

        @Test
        void canListWithFilterOnAllGroupsAndCaseInsensitive(){
            HttpResponse<?> response;
            HttpRequest<?> request;

            response = createGroup("Theta");
            assertEquals(OK, response.getStatus());

            response = createGroup("Beta");
            assertEquals(OK, response.getStatus());

            response = createGroup("SecondGroup");
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/groups?filter=EtA");
            Page page = blockingClient.retrieve(request, Page.class);
            assertEquals(2, page.getContent().size());
        }

        @Test
        void canListWithFilterYieldsEmptyResultIfGroupByNameDoesNotExist(){
            HttpResponse<?> response;
            HttpRequest<?> request;

            response = createGroup("Theta");
            assertEquals(OK, response.getStatus());

            response = createGroup("Beta");
            assertEquals(OK, response.getStatus());

            response = createGroup("SecondGroup");
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/groups?filter=foobarbaz");
            Page page = blockingClient.retrieve(request, Page.class);
            assertEquals(0, page.getContent().size());
        }

        @Test
        public void listYieldsGroupsNamesInAscendingOrderByDefault() {
            HttpResponse<?> response;
            HttpRequest<?> request;

            response = createGroup("Theta");
            assertEquals(OK, response.getStatus());

            response = createGroup("Beta");
            assertEquals(OK, response.getStatus());

            response = createGroup("SecondGroup");
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/groups");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> groupsPage = response.getBody(Page.class);
            assertTrue(groupsPage.isPresent());
            List<Map> groups = groupsPage.get().getContent();

            List<String> groupNames = groups.stream()
                    .flatMap(map -> Stream.of((String) map.get("name")))
                    .collect(Collectors.toList());
            assertEquals(groupNames.stream().sorted().collect(Collectors.toList()), groupNames);
        }

        @Test
        public void listShouldRespectRequestedGroupsNamesInDescendingOrder() {
            HttpResponse<?> response;
            HttpRequest<?> request;

            response = createGroup("Theta");
            assertEquals(OK, response.getStatus());

            response = createGroup("Beta");
            assertEquals(OK, response.getStatus());

            response = createGroup("SecondGroup");
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/groups?sort=name,desc");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> groupsPage = response.getBody(Page.class);
            assertTrue(groupsPage.isPresent());
            List<Map> groups = groupsPage.get().getContent();

            List<String> groupNames = groups.stream()
                    .flatMap(map -> Stream.of((String) map.get("name")))
                    .collect(Collectors.toList());
            assertEquals(groupNames.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()), groupNames);
        }

        @Test
        void listGroupsWithCounts() {
            HttpRequest<?> request = HttpRequest.GET("/groups");
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            List<Map> content = (List<Map>) responseMap.get("content");

            Map groupOne = content.stream().filter(map -> {
                String groupName = (String) map.get("name");
                return groupName.equals("GroupOne");
            }).collect(Collectors.toList()).get(0);

            assertEquals("GroupOne", groupOne.get("name"));
            assertEquals(2, groupOne.get("membershipCount"));
            assertEquals(2, groupOne.get("topicCount"));
            assertEquals(1, groupOne.get("applicationCount"));
        }

        // delete
        @Test
        void canDeleteGroup(){
            HttpResponse response = createGroup("Beta");
            Optional<Group> betaOptional = response.getBody(Group.class);
            assertTrue(betaOptional.isPresent());
            Group beta = betaOptional.get();

            HttpRequest request = HttpRequest.POST("/groups/delete/"+beta.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }

        // search
        @Test
        void canSearchAndShouldYieldAll(){
            HttpResponse response;

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
            assertEquals(NOT_FOUND, exception.getStatus());
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
                    Map.of()
            ));
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        // cannot create, update, delete
        @Test
        void cannotCreateGroup() {
            loginAsNonAdmin();
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                createGroup("Theta");
            });
            assertEquals(UNAUTHORIZED,exception.getStatus());
        }

        @Test
        void cannotUpdateGroup() {
            HttpResponse response;
            HttpRequest<?> request;

            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());

            loginAsNonAdmin();

            Group group = primaryOptional.get();
            group.setName("Omega");
            request = HttpRequest.POST("/groups/save", group);
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }

        @Test
        void cannotDeleteGroup() {
            HttpResponse response;
            HttpRequest<?> request;

            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            response = createGroup("PrimaryGroup");
            assertEquals(OK, response.getStatus());
            Optional<Group> primaryOptional = response.getBody(Group.class);
            assertTrue(primaryOptional.isPresent());

            loginAsNonAdmin();

            Group group = primaryOptional.get();
            request = HttpRequest.POST("/groups/delete/"+group.getId(), Map.of());
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
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

        @Test
        void cannotDeleteAnApplication(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            // create groups
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            // add member to group
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(theta.getId());
            dto.setEmail("jjones@test.test");
            dto.setGroupAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create application
            response = createApplication("ApplicationOne", theta.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOneOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOneOptional.isPresent());
            ApplicationDTO applicationOne = applicationOneOptional.get();

            loginAsNonAdmin();

            request = HttpRequest.POST("/applications/delete/"+applicationOne.getId(), Map.of());
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    class WhenAsAGroupMember {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test", true));
            User user = userRepository.save(new User("jjones@test.test"));

            Group group = groupRepository.save(new Group("GroupOne"));
            Topic topic = topicRepository.save(new Topic("TopicOne", TopicKind.B, group));
            Application application = applicationRepository.save(new Application("ApplicationOne", group));
            applicationPermissionRepository.save(new ApplicationPermission(application, topic, AccessType.READ_WRITE));
            groupUserRepository.save(new GroupUser(group, user));

            Group group1 = groupRepository.save(new Group("GroupTwo"));
            Topic topic1 = topicRepository.save(new Topic("TopicTwo", TopicKind.C, group1));
            Application application1 = applicationRepository.save(new Application("ApplicationTwo", group1));
            applicationPermissionRepository.save(new ApplicationPermission(application1, topic1, AccessType.READ_WRITE));

            loginAsNonAdmin();
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
        void canOnlySeeGroupsIAmAMemberOf() {
            HttpRequest<?> request = HttpRequest.GET("/groups");
            HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
            List<Map> groups = (List<Map>) responseMap.get("content");
            assertEquals(1, groups.size());
            Map group = groups.get(0);
            assertEquals("GroupOne", group.get("name"));
        }
    }

    @Nested
    class WhenAsANonGroupMember {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test", true));
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

    private HttpResponse<?> createApplication(String applicationName, Long groupId) {
        ApplicationDTO applicationDTO = new ApplicationDTO();
        applicationDTO.setName(applicationName);
        applicationDTO.setGroup(groupId);

        HttpRequest<?> request = HttpRequest.POST("/applications/save", applicationDTO);
        return blockingClient.exchange(request, ApplicationDTO.class);
    }
}
