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
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.testing.util.DbCleanup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

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
    @Client("/")
    HttpClient client;

    @BeforeEach
    void setup() {
        blockingClient = client.toBlocking();
    }

    //    We should take the user email, GroupID, and flags indicating group admin, application admin, and topic admins.
    @Nested
    class WhenAsAdmin {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User( "montesm@test.test", true));
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
            assertEquals(UNAUTHORIZED, exception.getStatus());
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
            assertEquals(UNAUTHORIZED, exception.getStatus());
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
        public void canSeeAllMembershipsFilteredByGroupName() {
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

            request = HttpRequest.GET("/group_membership?group=Secondary");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());
        }

        @Test
        public void canSeeAllMembershipsFilteredByUserEmail() {
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

            request = HttpRequest.GET("/group_membership?email=bob.builder@unityfoundation");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());
        }

        // update

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
            response = blockingClient.exchange(request, GroupUser.class);
            assertEquals(OK, response.getStatus());
            GroupUser groupUser = response.getBody(GroupUser.class).get();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }
    }

    @Nested
    class WhenAsAGroupAdmin {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User( "montesm@test.test", true));
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
            response = blockingClient.exchange(request, GroupUser.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUser.class).isPresent());
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
            response = blockingClient.exchange(request, GroupUser.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUser.class).isPresent());
            GroupUser groupUser = response.getBody(GroupUser.class).get();

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
            response = blockingClient.exchange(request, GroupUser.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUser.class).isPresent());
            GroupUser groupUser = response.getBody(GroupUser.class).get();

            assertTrue(userRepository.findByEmail("bob.builder@test.test").isPresent());

            loginAsNonAdmin();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            assertTrue(userRepository.findByEmail("bob.builder@test.test").isEmpty());
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
            response = blockingClient.exchange(request, GroupUser.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUser.class).isPresent());
            GroupUser groupUser = response.getBody(GroupUser.class).get();

            loginAsNonAdmin();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
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
            response = blockingClient.exchange(request, GroupUser.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUser.class).isPresent());
            GroupUser groupUser = response.getBody(GroupUser.class).get();

            loginAsNonAdmin();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
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
            userRepository.save(new User( "jjones@test.test"));
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
        public void canSeeCommonGroupMembershipsFilteredByGroupName() {
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

            request = HttpRequest.GET("/group_membership?group=Secondary");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(2, page.getContent().size());
        }

        @Test
        public void canSeeCommonGroupMembershipsFilteredByUserEmail() {
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

            request = HttpRequest.GET("/group_membership?email=the.generalcontractor@unityfoundation");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(1, page.getContent().size());
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

            request = HttpRequest.GET("/group_membership?group=Primary");
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

            request = HttpRequest.GET("/group_membership?email=builder@unityfoundation");
            response = blockingClient.exchange(request, Page.class);
            Page page = response.getBody(Page.class).get();
            assertEquals(0, page.getContent().size());
        }

    }

    @Nested
    class WhenAsANonAdmin {

        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test", true));
            userRepository.save(new User( "jjones@test.test"));
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
            response = blockingClient.exchange(request, GroupUser.class);
            assertEquals(OK, response.getStatus());
            assertTrue(response.getBody(GroupUser.class).isPresent());
            GroupUser groupUser = response.getBody(GroupUser.class).get();

            loginAsNonAdmin();

            // delete
            request = HttpRequest.DELETE("/group_membership", Map.of("id", groupUser.getId()));
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }
    }
}