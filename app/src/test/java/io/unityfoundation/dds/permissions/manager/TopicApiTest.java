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
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "micronaut.security.filter.enabled", value = StringUtils.FALSE)
@Disabled
public class TopicApiTest {

    private BlockingHttpClient blockingClient;

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TopicRepository topicRepository;

    @Inject
    GroupRepository groupRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    GroupUserRepository groupUserRepository;

    @Inject
    MockSecurityService mockSecurityService;

    @BeforeEach
    void setup() {
        blockingClient = client.toBlocking();
    }

    @AfterEach
    void afterEach() {
        mockSecurityService.postConstruct();
    }


    @Test
    public void testCrudActions() {

        // save
        HttpRequest<?> request = HttpRequest.POST("/topics/save", Map.of("name", "testTopic1"));
        HttpResponse<?> response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());
        Topic topic = response.getBody(Topic.class).get();
        long topic1Id = topic.getId();

        request = HttpRequest.POST("/topics/save", Map.of("name", "testTopic2", "kind", 'C'));
        response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());
        topic = response.getBody(Topic.class).get();
        long topic2Id = topic.getId();

        // topics with same name can exist site-wide
        request = HttpRequest.POST("/topics/save", Map.of("name", "testTopic2", "kind", 'B'));
        response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());
        topic = response.getBody(Topic.class).get();
        assertNotEquals(topic2Id, topic.getId());

        // update attempt should fail
        request = HttpRequest.POST("/topics/save", Map.of("id", topic2Id, "name", "UpdatedTestTopic2"));
        HttpRequest<?> finalRequest = request;
        HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequest);
        });
        assertEquals(BAD_REQUEST, thrown.getStatus());

        // list
        request = HttpRequest.GET("/topics");
        response = blockingClient.exchange(request, HashMap.class);
        List<Map> topics = (List<Map>) response.getBody(HashMap.class).get().get("content");
        assertEquals(3, topics.size());
        assertEquals(OK, response.getStatus());

        // confirm update failed
        Map<String, Object> updatedTopic = topics.get(1);
        assertNotEquals("UpdatedTestTopic2", updatedTopic.get("name"));

        // delete
        request = HttpRequest.POST("/topics/delete/2", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // list to confirm deletion
        request = HttpRequest.GET("/topics");
        response = blockingClient.exchange(request, HashMap.class);
        List<Map> topics1 = (List<Map>) response.getBody(HashMap.class).get().get("content");
        assertEquals(2, topics1.size());
        assertEquals(OK, response.getStatus());
    }

    @Test
    public void userWithAdminRoleShouldSeeAllTopics() {

        long initialGroupCount = topicRepository.count();

        HttpRequest<?> request = HttpRequest.POST("/topics/save", Map.of("name", "testTopic1"));
        HttpResponse<?> response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());
        Topic topic = response.getBody(Topic.class).get();

        request = HttpRequest.GET("/topics");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups = (List<Map>) responseMap.get("content");
        assertEquals(initialGroupCount + 1, groups.size());
    }

    @Test
    public void userWithNonAdminRoleShouldNotSeeAllTopics() {

        // create topics
        HttpRequest<?> request = HttpRequest.POST("/topics/save", Map.of("name", "Foo"));
        HttpResponse<?> response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.POST("/topics/save", Map.of("name", "Bar"));
        response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.POST("/topics/save", Map.of("name", "BedsAvailable"));
        response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());

        Topic bedsAvailable = response.getBody(Topic.class).get();

        long initialTopicCount = topicRepository.count();

        // group
        Group cityGovernment = new Group("CityGovernment");
        request = HttpRequest.POST("/groups/save", cityGovernment);
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        cityGovernment = groupRepository.findByName("CityGovernment").get();

        // associate topic and group
        request = HttpRequest.POST("/groups/add_topic/"+cityGovernment.getId()+"/"+bedsAvailable.getId(), Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // get user - see MockSecurityService - set isAdmin to false
        User user = userRepository.findByEmail("jgracia@test.test").get();

        // add user to group
        request = HttpRequest.POST("/groups/add_member/"+cityGovernment.getId()+"/"+user.getId(), Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());


        mockSecurityService.setServerAuthentication(new ServerAuthentication(
                "jgracia@test.test", Collections.emptyList(), Map.of("isAdmin", false)));

        // group member who is not an admin should see 1 topic
        request = HttpRequest.GET("/topics");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> topics = (List<Map>) responseMap.get("content");
        assertEquals(1, topics.size());
        assertNotEquals(initialTopicCount, topics.size());
    }

    @Test
    public void userWithNonAdminRoleShouldNotBeAbleCreateUpdateNorDeleteTopic() {

        HttpRequest<?> request = HttpRequest.POST("/topics/save", Map.of("name", "Foo"));
        HttpResponse<?> response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());

        Topic createdTopic = response.getBody(Topic.class).get();

        mockSecurityService.setServerAuthentication(new ServerAuthentication(
                "jgracia@test.test", Collections.emptyList(), Map.of("isAdmin", false)));

        // create attempt
        Topic topic = new Topic("Theta", TopicKind.B);
        request = HttpRequest.POST("/topics/save", topic);
        HttpRequest<?> finalRequest = request;
        HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequest);
        });
        assertEquals(UNAUTHORIZED,exception.getStatus());

        // update not possible
        HttpRequest<?> request1 = HttpRequest.POST("/topics/save", Map.of("id", createdTopic.getId(), "name", "Omega"));
        HttpClientResponseException exception1 = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(request1);
        });
        assertEquals(BAD_REQUEST, exception1.getStatus());

        // delete attempt
        HttpRequest<?> request2 = HttpRequest.POST("/groups/delete/"+createdTopic.getId(), Map.of());
        HttpClientResponseException exception2 = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(request2);
        });
        assertEquals(UNAUTHORIZED,exception2.getStatus());
    }

    @Test
    public void userWithNonAdminRoleButTopicAdminOfGroupShouldBeAbleCreateUpdateAndDeleteTopics() {

        // group
        Group cityGovernment = new Group("CityGovernment");
        HttpRequest<?> request = HttpRequest.POST("/groups/save", cityGovernment);
        HttpResponse<?> response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        cityGovernment = groupRepository.findByName("CityGovernment").get();

        // get user - see MockSecurityService - set isAdmin to false
        User user = userRepository.findByEmail("jgracia@test.test").get();

        // add TopicAdmin user to group
        request = HttpRequest.POST("/groups/add_member/"+cityGovernment.getId()+"/"+user.getId(), Map.of("isTopicAdmin", true));
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());


        // create different group with different topic
        Group otherGroup = new Group("OtherGroup");
        request = HttpRequest.POST("/groups/save", otherGroup);
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());
        otherGroup = groupRepository.findByName("OtherGroup").get();
        request = HttpRequest.POST("/topics/save", Map.of("name", "OtherTopic"));
        response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());
        Topic otherTopic = response.getBody(Topic.class).get();
        request = HttpRequest.POST("/groups/add_topic/"+otherGroup.getId()+"/"+otherTopic.getId(), Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        mockSecurityService.setServerAuthentication(new ServerAuthentication(
                "jgracia@test.test", Collections.emptyList(), Map.of("isAdmin", false)));

        // create topic with declared group should succeed if user is topic-admin of group
        Topic theta = new Topic("Theta", TopicKind.B);
        theta.setPermissionsGroup(cityGovernment.getId());
        request = HttpRequest.POST("/topics/save", theta);
        response = blockingClient.exchange(request, Topic.class);
        assertEquals(OK, response.getStatus());
        Topic createdTopic = response.getBody(Topic.class).get();

        // attempt to create a topic in another group that user is not a topic admin of
        Topic otherTheta = new Topic("OtherTheta", TopicKind.B);
        theta.setPermissionsGroup(otherGroup.getId());
        request = HttpRequest.POST("/topics/save", otherTheta);
        HttpRequest<?> finalRequestOtherTheta = request;
        HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequestOtherTheta);
        });
        assertEquals(UNAUTHORIZED, exception.getStatus());

        // create topic with group not declared should fail
        request = HttpRequest.POST("/topics/save", new Topic("Zeta", TopicKind.C));
        HttpRequest<?> finalRequestZeta = request;
        HttpClientResponseException exception1 = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequestZeta);
        });
        assertEquals(UNAUTHORIZED, exception1.getStatus());

        // update not possible
        HttpRequest<?> request1 = HttpRequest.POST("/topics/save", Map.of("id", createdTopic.getId(), "name", "Omega"));
        HttpClientResponseException exception2 = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(request1);
        });
        assertEquals(BAD_REQUEST, exception2.getStatus());

        // delete group topic should succeed if topic admin of associated group
        request = HttpRequest.POST("/topics/delete/"+createdTopic.getId(), Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // attempt to delete a topic in another group that user is not a topic admin of should fail
        HttpRequest<?> request2 = HttpRequest.POST("/topics/delete/"+otherTopic.getId(), Map.of());
        HttpClientResponseException exception3 = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(request2);
        });
        assertEquals(UNAUTHORIZED, exception3.getStatus());
    }
}
