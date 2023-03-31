package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Page;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.DPMEntity;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.applicationpermission.*;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.group.SimpleGroupDTO;
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
import org.junit.jupiter.api.Test;

import java.util.*;

import static io.micronaut.http.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class UniversalSearchApiTest {

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
        dbCleanup.cleanup();
        mockSecurityService.postConstruct();
        mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

        userRepository.save(new User("montesm@test.test", true));
        User jjones = userRepository.save(new User("jjones@test.test"));
        User eclair = userRepository.save(new User("eclair@test.test"));

        Group groupOne = groupRepository.save(new Group("GroupOne", "GroupOne", true));
        Topic topicOne = topicRepository.save(new Topic("TopicOne", TopicKind.B, "TopicOne", true, groupOne));
        Topic topicOne1 = topicRepository.save(new Topic("TopicOne!", TopicKind.C, "TopicOne!", true, groupOne));
        Application applicationOne = applicationRepository.save(new Application("ApplicationOne", groupOne, "ApplicationOne", true));
        applicationPermissionRepository.save(new ApplicationPermission(applicationOne, topicOne, AccessType.READ_WRITE));
        groupUserRepository.save(new GroupUser(groupOne, jjones));
        groupUserRepository.save(new GroupUser(groupOne, eclair));

        Group groupTwo = groupRepository.save(new Group("GroupTwo", "GroupTwo", true));
        Topic topicTwo = topicRepository.save(new Topic("TopicTwo", TopicKind.C, "TopicTwo", true, groupTwo));
        Application applicationTwo = applicationRepository.save(new Application("ApplicationTwo", groupTwo, "ApplicationTwo", true));
        Application applicationTwo1 = applicationRepository.save(new Application("ApplicationTwo1", groupTwo, "ApplicationTwo1", true));
        applicationPermissionRepository.save(new ApplicationPermission(applicationTwo, topicTwo, AccessType.READ_WRITE));
        groupUserRepository.save(new GroupUser(groupTwo, eclair));
    }

    @Test
    void canQuery() {
        HttpRequest request;
        HttpResponse response;

        // string s = "o"
        request = HttpRequest.GET("/search?query=o");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> searchPage = response.getBody(Page.class);
        assertTrue(searchPage.isPresent());
        List<Map> searchResults = searchPage.get().getContent();
        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.stream().allMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.GROUP.name()) ||
                    type.equals(DPMEntity.APPLICATION.name()) ||
                    type.equals(DPMEntity.TOPIC.name());
        }));

        // group
        request = HttpRequest.GET("/search?query=o&groups=true");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> groupsPage = response.getBody(Page.class);
        assertTrue(groupsPage.isPresent());
        List<Map> groups = groupsPage.get().getContent();
        assertFalse(groups.isEmpty());
        assertTrue(groups.stream().allMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.GROUP.name());
        }));

        // topics and applications
        request = HttpRequest.GET("/search?query=o&topics=true&applications=true");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> appTopicsPage = response.getBody(Page.class);
        assertTrue(appTopicsPage.isPresent());
        List<Map> appsTopics = appTopicsPage.get().getContent();
        assertFalse(appsTopics.isEmpty());
        assertTrue(appsTopics.stream().noneMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.GROUP.name());
        }));

        // all
        request = HttpRequest.GET("/search?query=o&topics=true&applications=true&groups=true");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> allPage = response.getBody(Page.class);
        assertTrue(allPage.isPresent());
        List<Map> all = allPage.get().getContent();
        assertFalse(all.isEmpty());
        assertTrue(all.stream().allMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.GROUP.name()) ||
                    type.equals(DPMEntity.APPLICATION.name()) ||
                    type.equals(DPMEntity.TOPIC.name());
        }));
    }

    @Test
    void emptyOrNullQueryShouldReturnAll() {
        HttpRequest request;
        HttpResponse response;

        // none
        request = HttpRequest.GET("/search");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> allPage = response.getBody(Page.class);
        assertTrue(allPage.isPresent());
        List<Map> all = allPage.get().getContent();
        assertFalse(all.isEmpty());
        assertTrue(all.stream().allMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.GROUP.name()) ||
                    type.equals(DPMEntity.APPLICATION.name()) ||
                    type.equals(DPMEntity.TOPIC.name());
        }));

        // groups
        request = HttpRequest.GET("/search?groups=true");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> groupsPage = response.getBody(Page.class);
        assertTrue(groupsPage.isPresent());
        List<Map> groups = groupsPage.get().getContent();
        assertFalse(groups.isEmpty());
        assertTrue(groups.stream().allMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.GROUP.name());
        }));

        // topics
        request = HttpRequest.GET("/search?topics=true");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> topicsPage = response.getBody(Page.class);
        assertTrue(topicsPage.isPresent());
        List<Map> topics = topicsPage.get().getContent();
        assertFalse(topics.isEmpty());
        assertTrue(topics.stream().allMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.TOPIC.name());
        }));

        // applications
        request = HttpRequest.GET("/search?applications=true");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> appsPage = response.getBody(Page.class);
        assertTrue(appsPage.isPresent());
        List<Map> apps = appsPage.get().getContent();
        assertFalse(apps.isEmpty());
        assertTrue(apps.stream().allMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.APPLICATION.name());
        }));
    }

    @Test
    void canPairEntityQueries() {
        HttpRequest request;
        HttpResponse response;

        // groups and topics
        request = HttpRequest.GET("/search?query=o&groups=true&topics=true");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> groupsTopicsPage = response.getBody(Page.class);
        assertTrue(groupsTopicsPage.isPresent());
        List<Map> groupsTopics = groupsTopicsPage.get().getContent();
        assertFalse(groupsTopics.isEmpty());
        assertTrue(groupsTopics.stream().noneMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.APPLICATION.name());
        }));

        // topics and applications
        request = HttpRequest.GET("/search?query=o&topics=true&applications=true");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> appTopicsPage = response.getBody(Page.class);
        assertTrue(appTopicsPage.isPresent());
        List<Map> appsTopics = appTopicsPage.get().getContent();
        assertFalse(appsTopics.isEmpty());
        assertTrue(appsTopics.stream().noneMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.GROUP.name());
        }));

        // groups and applications
        request = HttpRequest.GET("/search?query=o&groups=true&applications=true");
        response = blockingClient.exchange(request, Page.class);
        assertEquals(OK, response.getStatus());
        Optional<Page> groupsAppsPage = response.getBody(Page.class);
        assertTrue(groupsAppsPage.isPresent());
        List<Map> groupsApps = groupsAppsPage.get().getContent();
        assertFalse(groupsApps.isEmpty());
        assertTrue(groupsApps.stream().noneMatch(map -> {
            String type = (String) map.get("type");
            return type.equals(DPMEntity.TOPIC.name());
        }));
    }


    private HttpResponse<?> createGroup(String groupName) {
        SimpleGroupDTO group = new SimpleGroupDTO();
        group.setName(groupName);
        HttpRequest<?> request = HttpRequest.POST("/groups/save", group);
        return blockingClient.exchange(request, SimpleGroupDTO.class);
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
        topicDTO.setKind(topicKind);
        topicDTO.setGroup(groupId);

        HttpRequest<?> request = HttpRequest.POST("/topics/save", topicDTO);
        return blockingClient.exchange(request, TopicDTO.class);
    }

    private HttpResponse<?> createNonAdminGroupMembership(String email, Long groupId) {
        return createGroupMembership(email, groupId, false, false, false);
    }

    private HttpResponse<?> createGroupMembership(String email, Long groupId, boolean groupAdmin, boolean topicAdmin, boolean applicationAdmin) {
        GroupUserDTO dto = new GroupUserDTO();
        dto.setPermissionsGroup(groupId);
        dto.setEmail(email);
        dto.setGroupAdmin(groupAdmin);
        dto.setTopicAdmin(topicAdmin);
        dto.setApplicationAdmin(applicationAdmin);
        HttpRequest<?> request = HttpRequest.POST("/group_membership", dto);
        return  blockingClient.exchange(request, GroupUserDTO.class);
    }

    private HttpResponse<?> createApplicationPermission(Long applicationId, Long topicId, AccessType accessType) {
        HttpRequest<?> request;

        // generate bind token for application
        request = HttpRequest.GET("/applications/generate_bind_token/" + applicationId);
        HttpResponse<String> response = blockingClient.exchange(request, String.class);
        assertEquals(OK, response.getStatus());
        Optional<String> optional = response.getBody(String.class);
        assertTrue(optional.isPresent());
        String applicationBindToken = optional.get();

        request = HttpRequest.POST("/application_permissions/" + topicId + "/" + accessType.name(), Map.of())
                .header(ApplicationPermissionService.APPLICATION_BIND_TOKEN, applicationBindToken);
        return blockingClient.exchange(request, AccessPermissionDTO.class);
    }

}
