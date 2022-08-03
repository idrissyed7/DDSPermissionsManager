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

import java.util.*;

import static io.micronaut.http.HttpStatus.BAD_REQUEST;
import static io.micronaut.http.HttpStatus.OK;
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
        phi.setUsers(Arrays.asList(justin, kevin));

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

        // add member to group
        // To see mocked authentication see MockSecurityService
        long initialMemberCount = groupRepository.findById(1L).get().getUsers().size();

        request = HttpRequest.POST("/groups/add_member/1/3", Map.of());
        response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        request = HttpRequest.GET("/groups");
        responseMap1 = blockingClient.retrieve(request, HashMap.class);
        groups1 = (List<Map>) responseMap1.get("content");
        Map firstGroup = groups1.get(0);
        List<Map> userList = (List<Map>) firstGroup.get("users");
        long postAddUserCount = userList.size();
        assertEquals(initialMemberCount + 1, postAddUserCount);

        // remove member from group
        // To see mocked authentication see MockSecurityService
        request = HttpRequest.POST("/groups/remove_member/1/3", Map.of());
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

        // save group without members
        Group myGroup = new Group("MyGroup");
        HttpRequest<?> request = HttpRequest.POST("/groups/save", myGroup);
        HttpResponse<?> response = blockingClient.exchange(request);
        assertEquals(OK, response.getStatus());

        // save group without members
        Group myGroupDup = new Group("MyGroup");
        request = HttpRequest.POST("/groups/save", myGroupDup);
        HttpRequest<?> finalRequest = request;
        HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
            blockingClient.exchange(finalRequest);
        });
        assertEquals(BAD_REQUEST, thrown.getStatus());

    }
}
