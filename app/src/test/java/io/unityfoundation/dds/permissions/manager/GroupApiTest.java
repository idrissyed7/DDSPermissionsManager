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
import io.unityfoundation.dds.permissions.manager.model.user.Role;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.*;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.OK;
import static io.micronaut.http.HttpStatus.UNAUTHORIZED;
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
    public void rejectGroupWithSameNameAsAnExistingGroup() {

        // save new group should succeed
        Group myGroup = new Group("MyGroup");
        HttpRequest<?> request = HttpRequest.POST("/groups/save", myGroup);
        HttpResponse<?> response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // Attempt to add a new group with same name, should return the same as index
        Group myGroupDup = new Group("MyGroup");
        request = HttpRequest.POST("/groups/save", myGroupDup);
        Map groupResponse = blockingClient.retrieve(request, Map.class);
        assertNotNull(groupResponse);

        // Attempt to update an existing group with name of another group should yield a bad request with a message
        request = HttpRequest.POST("/groups/save", Map.of("id", 2, "name", "MyGroup"));
        HttpRequest<?> finalRequest1 = request;
        HttpClientResponseException thrown1 = assertThrows(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequest1);
        });
        // Note: Since the client throws an exception for a bad-request response, capturing the response's message is
        // not straightforward. The message returned is 'Duplicate column mapping name'
        assertEquals(BAD_REQUEST, thrown1.getStatus());

    }

    @Test
    public void userWithAdminRoleShouldSeeAllGroups() {

        long initialGroupCount = groupRepository.count();

        HttpRequest<?> request = HttpRequest.GET("/groups?sort=name,desc");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups = (List<Map>) responseMap.get("content");
        assertEquals(initialGroupCount, groups.size());
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
