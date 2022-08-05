package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.user.Role;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "micronaut.security.filter.enabled", value = StringUtils.FALSE)
//@Property(name = "micronaut.security.enabled", value= StringUtils.FALSE)
public class GroupApiTest {

    private BlockingHttpClient blockingClient;

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    GroupRepository groupRepository;

    @Inject
    TopicRepository topicRepository;

    @BeforeEach
    void setup() {
        blockingClient = client.toBlocking();
    }

    @Test
    public void testCrudActions() {

        long initialGroupCount = groupRepository.count();

        // save group without members
        Group theta = new Group("Theta");
        HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
        HttpResponse<?> response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // save group with members
        Group phi = new Group("Phi");
        User justin = new User("Justin", "Jones", "jjones@test.test", List.of(Role.ADMIN));
        User kevin = new User("Kevin", "Kaminsky", "kkaminsky@test.test", List.of(Role.ADMIN));
        phi.setUsers(Set.of(justin, kevin));

        request = HttpRequest.POST("/groups/save", phi);
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // update
        request = HttpRequest.POST("/groups/save", Map.of("id", 2, "name", "Omega"));
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // list
        request = HttpRequest.GET("/groups");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups = (List<Map>) responseMap.get("content");
        assertEquals(initialGroupCount + 2, groups.size());
        assertEquals(OK, response.getStatus());

        // show + confirm update
        request = HttpRequest.GET("/groups/2");
        HashMap<String, Object> showGroupResponse = blockingClient.retrieve(request, HashMap.class);
        Map getShowGroup = (Map) showGroupResponse.get("group");
        assertEquals("Omega", getShowGroup.get("name"));

        // delete
        request = HttpRequest.POST("/groups/delete/2", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // list + confirm deletion
        request = HttpRequest.GET("/groups");
        HashMap<String, Object> responseMap1 = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups1 = (List<Map>) responseMap1.get("content");
        assertEquals(initialGroupCount + 1, groups1.size());
        assertEquals(OK, response.getStatus());

        long initialMemberCount = groupRepository.findById(1L).get().getUsers().size();

        // adding an existing should not add to user group
        // To see mocked authentication see MockSecurityService
        request = HttpRequest.POST("/groups/add_member/1/3", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.GET("/groups");
        responseMap1 = blockingClient.retrieve(request, HashMap.class);
        groups1 = (List<Map>) responseMap1.get("content");
        Map firstGroup = groups1.get(0);
        List<Map> userList = (List<Map>) firstGroup.get("users");
        assertEquals(initialMemberCount, userList.size());

        // add new member to group
        // To see mocked authentication see MockSecurityService
        request = HttpRequest.POST("/groups/add_member/1/4", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.GET("/groups");
        responseMap1 = blockingClient.retrieve(request, HashMap.class);
        groups1 = (List<Map>) responseMap1.get("content");
        firstGroup = groups1.get(0);
        userList = (List<Map>) firstGroup.get("users");
        long postAddUserCount = userList.size();
        assertEquals(initialMemberCount + 1, postAddUserCount);

        // remove new member from group
        // To see mocked authentication see MockSecurityService
        request = HttpRequest.POST("/groups/remove_member/1/4", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.GET("/groups");
        responseMap1 = blockingClient.retrieve(request, HashMap.class);
        groups1 = (List<Map>) responseMap1.get("content");
        firstGroup = groups1.get(0);
        userList = (List<Map>) firstGroup.get("users");
        assertEquals(postAddUserCount - 1, userList.size());
    }

    @Test
    public void userWithAdminRoleShouldSeeAllGroups() {

        long initialGroupCount = groupRepository.count();

        HttpRequest<?> request = HttpRequest.GET("/groups?sort=name,desc");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups = (List<Map>) responseMap.get("content");
        assertEquals(initialGroupCount, groups.size());
    }

    @Test
    public void testGroupTopicAddAndRemove() {

        long initialTopicCount = topicRepository.count();

        Topic testTopic1 = new Topic("testTopic1", TopicKind.B);
        Topic testTopic2 = new Topic("testTopic2", TopicKind.C);

        // create two topics and add to group
        HttpRequest<?> request = HttpRequest.POST("/topics/save", testTopic1);
        HttpResponse<?> response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.POST("/topics/save", testTopic2);
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // topics with same name can exist in topics table
        request = HttpRequest.POST("/topics/save", testTopic1);
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.GET("/topics");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> topics = (List<Map>) responseMap.get("content");
        assertEquals(OK, response.getStatus());
        assertEquals(initialTopicCount + 3, topics.size());

        // add topics to group (positive)
        Group groupA = groupRepository.findById(1l).get();
        List<Map> testTopic1FromResponse = topics.stream().filter(t -> t.get("name").equals("testTopic1")).collect(Collectors.toList());
        List<Map> testTopic2FromResponse = topics.stream().filter(t -> t.get("name").equals("testTopic2")).collect(Collectors.toList());
        Integer savedTopic1Id = (Integer) testTopic1FromResponse.get(0).get("id");
        Integer savedTopic1IdDup = (Integer) testTopic1FromResponse.get(1).get("id");
        Integer savedTopic2Id = (Integer) testTopic2FromResponse.get(0).get("id");

        request = HttpRequest.POST("/groups/add_topic/1/"+savedTopic1Id, Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.POST("/groups/add_topic/1/"+savedTopic2Id, Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.GET("/groups");
        HashMap responseMap1 = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups1 = (List<Map>) responseMap1.get("content");
        Map firstGroup = groups1.get(0);
        List<Map> topicList = (List<Map>) firstGroup.get("topics");
        long postTopicAddToGroupCount = topicList.size();
        assertEquals(2, postTopicAddToGroupCount);


        // adding topic with same name to a group should fail (negative)
        // ensure same number of topics and no duplicate entries
        request = HttpRequest.POST("/groups/add_topic/1/"+savedTopic1Id, Map.of());
        HttpRequest<?> finalRequest = request;
        HttpClientResponseException exception1 = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequest);
        });
        assertEquals(BAD_REQUEST, exception1.getStatus());

        request = HttpRequest.GET("/groups");
        responseMap1 = blockingClient.retrieve(request, HashMap.class);
        groups1 = (List<Map>) responseMap1.get("content");
        firstGroup = groups1.get(0);
        topicList = (List<Map>) firstGroup.get("topics");
        assertEquals(2, topicList.size());

        // removing a topic should succeed
        request = HttpRequest.POST("/groups/remove_topic/1/"+savedTopic2Id, Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // total number of topics should be the same as post-addition of topics less one.
        request = HttpRequest.GET("/groups");
        responseMap1 = blockingClient.retrieve(request, HashMap.class);
        groups1 = (List<Map>) responseMap1.get("content");
        firstGroup = groups1.get(0);
        topicList = (List<Map>) firstGroup.get("topics");
        assertEquals(postTopicAddToGroupCount - 1, topicList.size());
    }

    // Change 'ADMIN' to 'GROUP_ADMIN' in MockSecurityService in order for the below tests to pass.
    // Why? I can't seem to dynamically change the role of the
    // authenticated user. Need to come up with a better way to mock authentication.
//    @Test
//    public void userWithNonAdminRoleShouldNotSeeAllGroups() {
//
//        long initialGroupCount = groupRepository.count();
//
//        HttpRequest<?> request = HttpRequest.GET("/groups");
//        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
//        List<Map> groups = (List<Map>) responseMap.get("content");
//        assertNotEquals(initialGroupCount, groups.size());
//    }
//
//    @Test
//    public void userWithNonAdminRoleShouldNotBeAbleToCallGroupCrud() {
//        // create
//        Group theta = new Group("Theta");
//        HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
//        HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
//            blockingClient.exchange(request);
//        });
//        assertEquals(UNAUTHORIZED,exception.getStatus());
//
//        // update
//        HttpRequest<?> request1 = HttpRequest.POST("/groups/save", Map.of("id", 2, "name", "Omega"));
//        HttpClientResponseException exception1 = assertThrowsExactly(HttpClientResponseException.class, () -> {
//            blockingClient.exchange(request1);
//        });
//        assertEquals(UNAUTHORIZED,exception1.getStatus());
//
//        // delete
//        HttpRequest<?> request2 = HttpRequest.POST("/groups/delete/2", Map.of());
//        HttpClientResponseException exception2 = assertThrowsExactly(HttpClientResponseException.class, () -> {
//            blockingClient.exchange(request2);
//        });
//        assertEquals(UNAUTHORIZED,exception2.getStatus());
//    }
}
