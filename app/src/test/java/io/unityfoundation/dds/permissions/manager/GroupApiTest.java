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
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    GroupUserRepository groupUserRepository;

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

        // update
        request = HttpRequest.POST("/groups/save", Map.of("id", 2, "name", "Omega"));
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // list
        request = HttpRequest.GET("/groups");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups = (List<Map>) responseMap.get("content");
        assertEquals(initialGroupCount + 1, groups.size());
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
        assertEquals(initialGroupCount, groups1.size());
        assertEquals(OK, response.getStatus());
    }

    @Test
    public void rejectGroupWithSameNameAsAnExistingGroup() {

        // save new group should succeed
        Group myGroup = new Group("MyGroup");
        HttpRequest<?> request = HttpRequest.POST("/groups/save", myGroup);
        HttpResponse<?> response = blockingClient.exchange(request, Group.class);
        assertEquals(OK, response.getStatus());
        myGroup = response.getBody(Group.class).get();

        // Attempt to add a new group with same name, should return the existing group and 303
        Group myGroupDup = new Group("MyGroup");
        request = HttpRequest.POST("/groups/save", myGroupDup);
        response = blockingClient.exchange(request, Group.class);
        assertEquals(SEE_OTHER, response.getStatus());
        myGroupDup = response.getBody(Group.class).get();
        assertEquals(myGroup.getId(), myGroupDup.getId());

        // Attempt to update an existing group with name of another group should yield a bad request with a message
        request = HttpRequest.POST("/groups/save", Map.of("id", 2, "name", "MyGroup"));
        HttpRequest<?> finalRequest1 = request;
        HttpClientResponseException thrown1 = assertThrows(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequest1);
        });
        // Note: Since the client throws an exception for a bad-request response, capturing the response's message is
        // not straightforward.
        assertEquals(BAD_REQUEST, thrown1.getStatus());
    }

    @Test
    public void cannotCreateGroupWithNullNorWhitespace() {
        // save group without members
        Group nullGroup = new Group();
        HttpRequest<?> request = HttpRequest.POST("/groups/save", nullGroup);
        HttpRequest<?> finalRequest = request;
        HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequest);
        });
        assertEquals(BAD_REQUEST, exception.getStatus());

        Group whitespaceGroup = new Group("   ");
        request = HttpRequest.POST("/groups/save", whitespaceGroup);
        HttpRequest<?> finalRequest1 = request;
        HttpClientResponseException exception1 = assertThrowsExactly(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequest1);
        });
        assertEquals(BAD_REQUEST, exception1.getStatus());
    }

    @Test
    public void createShouldTrimWhitespace() {
        // save group without members
        Group gloopGroup = new Group("  GloopGroup ");
        HttpRequest<?> request = HttpRequest.POST("/groups/save", gloopGroup);
        HttpResponse<Group> response = blockingClient.exchange(request, Group.class);
        gloopGroup = response.getBody(Group.class).get();
        assertEquals("GloopGroup", gloopGroup.getName());
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
    public void canSearch() {
        HttpRequest<?> request = HttpRequest.GET("/groups/search/ta");
        List<String> response = blockingClient.retrieve(request, List.class);
        assertTrue(response.size() <= 10);
    }

    @Test
    public void canSearchCaseInsensitive() {
        HttpRequest<?> request = HttpRequest.GET("/groups/search/alpha");
        List<String> response = blockingClient.retrieve(request, List.class);
        assertEquals(1,  response.size());
    }

    @Test
    public void searchDoesNotReturnAnythingIfGroupDoesNotExist() {
        HttpRequest<?> request = HttpRequest.GET("/groups/search/foobarbaz");
        List<String> response = blockingClient.retrieve(request, List.class);
        assertTrue(response.size() == 0);
    }

    @Test
    public void searchReturnsGroupIfExist() {
        HttpRequest<?> request = HttpRequest.GET("/groups/search/Alpha");
        List<String> response = blockingClient.retrieve(request, List.class);
        assertTrue(response.size() == 1);
    }

    @Test
    public void shouldSeeGroupsNamesInAscendingOrderByDefault() {
        HttpRequest<?> request = HttpRequest.GET("/groups");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups = (List<Map>) responseMap.get("content");
        List<String> groupNames = groups.stream()
                .flatMap(map -> Stream.of((String) map.get("name")))
                .collect(Collectors.toList());
        assertEquals(groupNames.stream().sorted().collect(Collectors.toList()), groupNames);
    }

    @Test
    public void shouldRespectGroupsNamesInDescendingOrder() {
        HttpRequest<?> request = HttpRequest.GET("/groups?sort=name,desc");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups = (List<Map>) responseMap.get("content");
        List<String> groupNames = groups.stream()
                .flatMap(map -> Stream.of((String) map.get("name")))
                .collect(Collectors.toList());
        assertEquals(groupNames.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()), groupNames);
    }

    @Test
    public void shouldSeeGroupWithCounts() {
        HttpRequest<?> request = HttpRequest.GET("/groups");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> content = (List<Map>) responseMap.get("content");

        Map alphaGroup = content.get(0);
        assertEquals("Alpha", alphaGroup.get("name"));
        assertEquals(3, alphaGroup.get("membershipCount"));
        assertEquals(1, alphaGroup.get("topicCount"));
        assertEquals(1, alphaGroup.get("applicationCount"));
    }

    @Test
    public void userWithAdminRoleCanSeeGroupsAUserIsAMemberOf() {
        HttpRequest<?> request = HttpRequest.GET("/groups/user/1");
        List responseList = blockingClient.retrieve(request, List.class);
        assertEquals(1, responseList.size());
    }

    @Test
    @Disabled("Topic add and remove cases are covered in TopicApiTest.userWithNonAdminRoleButTopicAdminOfGroupShouldBeAbleCreateUpdateAndDeleteTopics")
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

    // Change 'isAdmin' to false in MockSecurityService in order for the below tests to pass.
    // Why? I can't seem to dynamically change the role of the
    // authenticated user. Need to come up with a better way to mock authentication.
//    @Test
//    public void userWithNonAdminRoleCannotSeeGroupsAUserIsAMemberOf() {
//        HttpRequest<?> request = HttpRequest.GET("/groups/user/1");
//        HttpRequest<?> finalRequest = request;
//        HttpClientResponseException exception1 = assertThrowsExactly(HttpClientResponseException.class, () -> {
//            blockingClient.exchange(finalRequest);
//        });
//        assertEquals(UNAUTHORIZED, exception1.getStatus());
//    }
//
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
