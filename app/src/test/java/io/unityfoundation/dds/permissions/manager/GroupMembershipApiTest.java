package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.data.model.Page;
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
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserResponseDTO;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.testing.util.DbCleanup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "micronaut.security.filter.enabled", value = StringUtils.FALSE)
public class GroupMembershipApiTest {
    private BlockingHttpClient blockingClient;

    @Inject
    MockSecurityService mockSecurityService;

    @Inject
    GroupRepository groupRepository;

    @Inject
    UserRepository userRepository;

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
            userRepository.save(new User( "montesm@test.test", true));
            userRepository.save(new User("jjones@test.test"));
            mockSecurityService.postConstruct();
        }

        // create
        @Test
        public void canCreate() {
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }

        @Test
        public void canCreateWithSameEmailDifferentGroup() {
            // group creation
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            // perform test
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            dto.setPermissionsGroup(secondaryGroup.getId());
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

        }

        @Test
        public void cannotCreateWithInvalidEmailFormat() {
            // group creation
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            // perform test
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("pparker@.test.test");
            request = HttpRequest.POST("/group_membership", dto);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(BAD_REQUEST, thrown.getStatus());
            Optional<List> bodyOptional = thrown.getResponse().getBody(List.class);
            assertTrue(bodyOptional.isPresent());
            List<Map> list = bodyOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.INVALID_EMAIL_FORMAT.equals(map.get("code"))));
        }

        @Test
        public void cannotCreateWithSameEmailAndGroupAsExisting() {
            // group creation
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/group_membership", dto);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.GROUP_MEMBERSHIP_ALREADY_EXISTS.equals(map.get("code"))));
        }

        @Test
        public void cannotCreateIfGroupSpecifiedDoesNotExist() {
            GroupUserDTO dto = new GroupUserDTO();
            dto.setEmail("bob.builder@test.test");
            dto.setPermissionsGroup(100l);
            dto.setTopicAdmin(true);
            HttpRequest<?> request = HttpRequest.POST("/group_membership", dto);
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(NOT_FOUND, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.GROUP_NOT_FOUND.equals(map.get("code"))));
        }

        @Test
        public void cannotCreateWithoutGroupSpecified() {
            User bob = userRepository.save(new User( "bob.builder@test.test"));

            GroupUserDTO dto = new GroupUserDTO();
            dto.setEmail(bob.getEmail());
            dto.setTopicAdmin(true);
            HttpRequest<?> request = HttpRequest.POST("/group_membership", dto);
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.GROUP_MEMBERSHIP_REQUIRES_GROUP_ASSOCIATION.equals(map.get("code"))));
        }

        // list
        @Test
        public void canSeeAllMemberships() {
            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/group_membership");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(2, page.getContent().size());
        }

        @Test
        public void membershipsOrderedByEmail() {
            // Groups intentionally created in an order that
            // is not consistent with their sort order
            String firstGroupCreatedName = "ZZZ Group"; // bob and angie and zack
            String secondGroupCreatedName = "MMM Group"; // bob and angie and jill
            String thirdGroupCreatedName = "AAA Group"; // jack and angie

            String jill = "jill@test.test";
            String jack = "jack@test.test";
            String angie = "angie@test.test";
            String bob = "bob@test.test";
            String zack = "zack@test.test";

            createGroupAndMemberships(firstGroupCreatedName, bob, angie, zack);
            createGroupAndMemberships(secondGroupCreatedName, bob, jill, angie);
            createGroupAndMemberships(thirdGroupCreatedName, jack, angie);

            HttpRequest<?> request = HttpRequest.GET("/group_membership");
            HttpResponse<?> response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            List content = page.getContent();
            assertEquals(8, content.size());

            assertExpectedEmailAndGroupName(content, 0, angie, thirdGroupCreatedName);
            assertExpectedEmailAndGroupName(content, 1, angie, secondGroupCreatedName);
            assertExpectedEmailAndGroupName(content, 2, angie, firstGroupCreatedName);

            assertExpectedEmailAndGroupName(content, 3, bob, secondGroupCreatedName);
            assertExpectedEmailAndGroupName(content, 4, bob, firstGroupCreatedName);

            assertExpectedEmailAndGroupName(content, 5, jack, thirdGroupCreatedName);

            assertExpectedEmailAndGroupName(content, 6, jill, secondGroupCreatedName);

            assertExpectedEmailAndGroupName(content, 7, zack, firstGroupCreatedName);
        }

        private void createMemberships(Group group, String... emails) {
            for(String email: emails) {
                GroupUserDTO dto = new GroupUserDTO();
                dto.setPermissionsGroup(group.getId());
                dto.setEmail(email);
                HttpRequest<?> request = HttpRequest.POST("/group_membership", dto);
                HttpResponse<?> response = blockingClient.exchange(request);
                assertEquals(OK, response.getStatus());
            }
        }

        private Group createGroup(String groupName) {
            Group group = new Group(groupName);
            HttpRequest<?> request = HttpRequest.POST("/groups/save", group);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            return response.getBody(Group.class).get();
        }

        private void createGroupAndMemberships(String groupName, String... emails) {
            Group group = createGroup(groupName);
            createMemberships(group, emails);
        }

        void assertExpectedEmailAndGroupName(List content, int index, String expectedEmail, String expectedGroup) {
            Map membership = (Map) content.get(index);

            String email = (String) membership.get("permissionsUserEmail");
            assertEquals(expectedEmail, email);

            String groupName = (String) membership.get("permissionsGroupName");
            assertEquals(expectedGroup, groupName);
        }

        @Test
        public void canSeeAllMembershipsFilteredByGroupNameCaseInsensitive() {
            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/group_membership?filter=secondary");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());
        }

        @Test
        public void canSeeAllMembershipsFilteredByUserEmailCaseInsensitive() {
            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/group_membership?filter=Bob.Builder@UnityFoundation");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());
        }

        @Test
        public void canSeeAllMembershipsFilteredByGroup() {
            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request, GroupUserDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<GroupUserDTO> bobOptional = response.getBody(GroupUserDTO.class);
            assertTrue(bobOptional.isPresent());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request, GroupUserDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<GroupUserDTO> robertOptional = response.getBody(GroupUserDTO.class);
            assertTrue(robertOptional.isPresent());

            // access both members via groups they belong to
            request = HttpRequest.GET("/group_membership?group="+primaryGroup.getId());
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());
            Map map = (Map) page.getContent().get(0);
            assertEquals(map.get("id"), bobOptional.get().getId().intValue());

            request = HttpRequest.GET("/group_membership?group="+secondaryGroup.getId());
            response = blockingClient.exchange(request, Page.class);
            page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());
            map = (Map) page.getContent().get(0);
            assertEquals(map.get("id"), robertOptional.get().getId().intValue());

            // support filter
            request = HttpRequest.GET("/group_membership?filter=Bob.Builder@UnityFoundation&group="+primaryGroup.getId());
            response = blockingClient.exchange(request, Page.class);
            page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());
            map = (Map) page.getContent().get(0);
            assertEquals(map.get("id"), bobOptional.get().getId().intValue());

            // negative (no results)
            request = HttpRequest.GET("/group_membership?group="+-1);
            response = blockingClient.exchange(request, Page.class);
            page = response.getBody(Page.class).get();
            assertEquals(0, page.getContent().size());
        }

        // update
        @Test
        public void canUpdate() {
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            dto.setId(groupUser.getId());
            dto.setGroupAdmin(true);
            dto.setTopicAdmin(true);
            request = HttpRequest.PUT("/group_membership", dto);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            groupUser = response.getBody(GroupUserResponseDTO.class).get();
            assertTrue(groupUser.isGroupAdmin());
            assertTrue(groupUser.isTopicAdmin());
        }

        @Test
        public void cannotAttemptToSaveNewWithUpdateEndpoint() {
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());

            request = HttpRequest.PUT("/group_membership", dto);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.GROUP_MEMBERSHIP_CANNOT_CREATE_WITH_UPDATE.equals(map.get("code"))));
        }

        // delete
        @Test
        public void canDelete() {
            // group creation
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }

        @Test
        public void shouldBeConsideredValid() {
            HttpRequest request = HttpRequest.GET("/group_membership/user_validity");
            HttpResponse response = blockingClient.exchange(request, Map.class);
            assertEquals(OK, response.getStatus());
            Optional<Map> mapOptional = response.getBody(Map.class);
            assertTrue(mapOptional.isPresent());
            Map map = mapOptional.get();
            assertTrue((Boolean) map.get("isAdmin"));
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
                    Map.of("isAdmin", false)
            ));
        }

        @Test
        public void canCreate() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as a group admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setGroupAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // create member with above group
            GroupUserDTO dtoNewUser = new GroupUserDTO();
            dtoNewUser.setPermissionsGroup(primaryGroup.getId());
            dtoNewUser.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dtoNewUser);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUserResponseDTO.class).isPresent());
        }

        @Test
        public void cannotCreateIfNotMemberOfGroup() {
            mockSecurityService.postConstruct();

            // save group without members
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            loginAsNonAdmin();

            GroupUserDTO dtoNewUser = new GroupUserDTO();
            dtoNewUser.setPermissionsGroup(primaryGroup.getId());
            dtoNewUser.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dtoNewUser);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.UNAUTHORIZED.equals(map.get("code"))));
        }

        @Test
        public void cannotCreateIfNonGroupAdminMemberOfGroup() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as a group admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setTopicAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // create application with above group
            GroupUserDTO dtoNewUser = new GroupUserDTO();
            dtoNewUser.setPermissionsGroup(primaryGroup.getId());
            dtoNewUser.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dtoNewUser);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.UNAUTHORIZED.equals(map.get("code"))));
        }

        @Test
        public void canUpdate() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            GroupUserResponseDTO primaryGroupResponse = response.getBody(GroupUserResponseDTO.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroupResponse.getId());
            dto.setEmail(justin.getEmail());
            dto.setGroupAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            loginAsNonAdmin();

            dto.setId(groupUser.getId());
            dto.setGroupAdmin(true);
            dto.setTopicAdmin(true);
            request = HttpRequest.PUT("/group_membership", dto);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            groupUser = response.getBody(GroupUserResponseDTO.class).get();
            assertTrue(groupUser.isGroupAdmin());
            assertTrue(groupUser.isTopicAdmin());
        }

        @Test
        public void cannotUpdateIfNonGroupAdminMemberOfGroup() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setTopicAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            loginAsNonAdmin();

            dto.setId(groupUser.getId());
            dto.setGroupAdmin(true);
            dto.setTopicAdmin(true);
            request = HttpRequest.PUT("/group_membership", dto);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.UNAUTHORIZED.equals(map.get("code"))));
        }

        @Test
        public void canDeleteFromGroup() {
            mockSecurityService.postConstruct();
            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as a group admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setGroupAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create member with above group
            GroupUserDTO dtoNewUser = new GroupUserDTO();
            dtoNewUser.setPermissionsGroup(primaryGroup.getId());
            dtoNewUser.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dtoNewUser);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUserResponseDTO.class).isPresent());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            loginAsNonAdmin();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }

        @Test
        public void deleteUserIfNonAdminAndNoGroupMemberships() {
            mockSecurityService.postConstruct();
            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as a group admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setGroupAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create member with above group
            GroupUserDTO dtoNewUser = new GroupUserDTO();
            dtoNewUser.setPermissionsGroup(primaryGroup.getId());
            dtoNewUser.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dtoNewUser);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUserResponseDTO.class).isPresent());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            request = HttpRequest.GET("/group_membership/user_exists/"+groupUser.getPermissionsUser());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/group_membership/user_exists/"+groupUser.getPermissionsUser());
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(NOT_FOUND, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.USER_NOT_FOUND.equals(map.get("code"))));
        }

        @Test
        public void cannotDeleteIfNotMemberOfGroup() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            // create member with above group
            GroupUserDTO dtoNewUser = new GroupUserDTO();
            dtoNewUser.setPermissionsGroup(primaryGroup.getId());
            dtoNewUser.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dtoNewUser);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUserResponseDTO.class).isPresent());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            loginAsNonAdmin();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.UNAUTHORIZED.equals(map.get("code"))));
        }

        @Test
        public void cannotDeleteIfNonGroupAdminMemberOfGroup() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            User justin = userRepository.findByEmail("jjones@test.test").get();

            // add user to group as a group admin
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail(justin.getEmail());
            dto.setTopicAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create member with above group
            GroupUserDTO dtoNewUser = new GroupUserDTO();
            dtoNewUser.setPermissionsGroup(primaryGroup.getId());
            dtoNewUser.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dtoNewUser);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUserResponseDTO.class).isPresent());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            loginAsNonAdmin();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.UNAUTHORIZED.equals(map.get("code"))));
        }
    }

    @Nested
    class WhenAsAGroupMember {

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
                    Map.of("isAdmin", false)
            ));
        }

        @Test
        public void canSeeMembershipsOfGroupsIAmAMemberOf() {
            mockSecurityService.postConstruct();

            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add non-admin test user
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/group_membership");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(2, page.getContent().size());
        }

        @Test
        public void cannotSeeAllMemberships() {
            mockSecurityService.postConstruct();

            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add non-admin test user
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/group_membership");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(2, page.getContent().size());
        }

        @Test
        public void canSeeCommonGroupMembershipsFilteredByGroupNameCaseInsensitive() {
            mockSecurityService.postConstruct();

            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add non-admin test user
            dto1.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/group_membership?filter=secondary");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(2, page.getContent().size());
        }

        @Test
        public void canSeeCommonGroupMembershipsFilteredByUserEmailCaseInsensitive() {
            mockSecurityService.postConstruct();

            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add non-admin test user
            dto1.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/group_membership?filter=The.GeneralContractor@UnityFoundation");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());
        }

        @Test
        public void canSeeCommonGroupMembershipsFilteredByGroup() {
            mockSecurityService.postConstruct();

            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add non-admin test user
            dto1.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // group search
            request = HttpRequest.GET("/group_membership?group="+secondaryGroup.getId());
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(2, page.getContent().size());

            // filter param support
            request = HttpRequest.GET("/group_membership?filter=The.GeneralContractor@UnityFoundation&group="+secondaryGroup.getId());
            response = blockingClient.exchange(request, Page.class);
            page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());

            // Negative cases
            request = HttpRequest.GET("/group_membership?group="+primaryGroup.getId());
            response = blockingClient.exchange(request, Page.class);
            page = response.getBody(Page.class).get();
            assertTrue(page.getContent().isEmpty());

            request = HttpRequest.GET("/group_membership?filter=bob.builder@unityfoundation&group="+primaryGroup.getId());
            response = blockingClient.exchange(request, Page.class);
            page = response.getBody(Page.class).get();
            assertTrue(page.getContent().isEmpty());
        }


        @Test
        public void cannotSeeOtherGroupMembershipsFilteredByGroupName() {
            mockSecurityService.postConstruct();

            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add non-admin test user
            dto1.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/group_membership?filter=Primary");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(0, page.getContent().size());
        }

        @Test
        public void cannotSeeOtherGroupMembershipsFilteredByUserEmail() {
            mockSecurityService.postConstruct();

            // first group and member
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add non-admin test user
            dto1.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/group_membership?filter=builder@unityfoundation");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(0, page.getContent().size());
        }

        @Test
        public void shouldBeConsideredValid() {
            mockSecurityService.postConstruct();

            HttpRequest request;
            HttpResponse response;

            // other group and member
            Group secondaryGroup = new Group("SecondaryGroup");
            request = HttpRequest.POST("/groups/save", secondaryGroup);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            secondaryGroup = (Group) response.getBody(Group.class).get();

            GroupUserDTO dto1 = new GroupUserDTO();
            dto1.setPermissionsGroup(secondaryGroup.getId());
            dto1.setEmail("robert.the.generalcontractor@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // add non-admin test user
            dto1.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/group_membership/user_validity");
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }

    }

    @Nested
    class WhenAsANonAdmin {

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
                    Map.of("isAdmin", false)
            ));
        }

        @Test
        public void cannotCreate() {
            mockSecurityService.postConstruct();

            // save group without members
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            loginAsNonAdmin();

            GroupUserDTO dtoNewUser = new GroupUserDTO();
            dtoNewUser.setPermissionsGroup(primaryGroup.getId());
            dtoNewUser.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dtoNewUser);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.UNAUTHORIZED.equals(map.get("code"))));
        }

        @Test
        public void cannotUpdate() {
            mockSecurityService.postConstruct();

            // save group without members
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(primaryGroup.getId());
            dto.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            loginAsNonAdmin();

            dto.setId(groupUser.getId());
            dto.setGroupAdmin(true);
            dto.setTopicAdmin(true);
            request = HttpRequest.PUT("/group_membership", dto);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.UNAUTHORIZED.equals(map.get("code"))));
        }

        @Test
        public void cannotDeleteFromGroup() {
            mockSecurityService.postConstruct();

            // create group
            Group primaryGroup = new Group("PrimaryGroup");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", primaryGroup);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            primaryGroup = response.getBody(Group.class).get();

            // create member with above group
            GroupUserDTO dtoNewUser = new GroupUserDTO();
            dtoNewUser.setPermissionsGroup(primaryGroup.getId());
            dtoNewUser.setEmail("bob.builder@test.test");
            request = HttpRequest.POST("/group_membership", dtoNewUser);
            response = blockingClient.exchange(request, GroupUserResponseDTO.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUserResponseDTO.class).isPresent());
            GroupUserResponseDTO groupUser = response.getBody(GroupUserResponseDTO.class).get();

            loginAsNonAdmin();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.UNAUTHORIZED.equals(map.get("code"))));
        }
        @Test
        public void shouldBeConsideredInvalid() {
            loginAsNonAdmin();
            HttpRequest request = HttpRequest.GET("/group_membership/user_validity");
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(NOT_FOUND, exception.getStatus());
            Optional<List> listOptional = exception.getResponse().getBody(List.class);
            assertTrue(listOptional.isPresent());
            List<Map> list = listOptional.get();
            assertTrue(list.stream().anyMatch(map -> ResponseStatusCodes.USER_IS_NOT_VALID.equals(map.get("code"))));
        }
    }
}
