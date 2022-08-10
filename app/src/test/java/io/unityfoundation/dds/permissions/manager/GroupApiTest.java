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
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

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

    @Inject
    GroupUserRepository groupUserRepository;

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

        long initialAlphaGroupMemberCount = groupUserRepository.findAllByPermissionsGroup(1l).size();

        // adding an existing member should not add to user group
        // To see mocked authentication see MockSecurityService
        request = HttpRequest.POST("/groups/add_member/1/3", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.GET("/groups/1/members");
        List<Map> responseList = blockingClient.retrieve(request, List.class);
        assertEquals(initialAlphaGroupMemberCount, responseList.size());

        // add new member to group
        // To see mocked authentication see MockSecurityService
        request = HttpRequest.POST("/groups/add_member/1/4", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.GET("/groups/1/members");
        responseList = blockingClient.retrieve(request, List.class);
        long postAddUserCount = responseList.size();
        assertEquals(initialAlphaGroupMemberCount + 1, postAddUserCount);

        // remove new member from group
        // To see mocked authentication see MockSecurityService
        request = HttpRequest.POST("/groups/remove_member/1/4", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.GET("/groups/1/members");
        responseList = blockingClient.retrieve(request, List.class);
        assertEquals(postAddUserCount - 1, responseList.size());
    }

    @Test
    public void userWithAdminRoleShouldSeeAllGroups() {

        long initialGroupCount = groupRepository.count();

        HttpRequest<?> request = HttpRequest.GET("/groups?sort=name,desc");
        HashMap<String, Object> responseMap = blockingClient.retrieve(request, HashMap.class);
        List<Map> groups = (List<Map>) responseMap.get("content");
        assertEquals(initialGroupCount, groups.size());
    }

    // Change 'isAdmin' to false in MockSecurityService in order for the below tests to pass.
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
