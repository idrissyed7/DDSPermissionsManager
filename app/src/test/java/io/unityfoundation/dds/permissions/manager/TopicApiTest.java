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
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserDTO;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicShowResponseDTO;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.testing.util.DbCleanup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
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
    DbCleanup dbCleanup;

    @Inject
    MockSecurityService mockSecurityService;

    @Inject
    MockAuthenticationFetcher mockAuthenticationFetcher;

    @BeforeEach
    void setup() {
        blockingClient = client.toBlocking();
    }

    @Nested
    class WhenAsAdmin {
        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test", true));
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        //create
        @Test
        void canCreate(){
            HttpRequest<?> request = HttpRequest.POST("/topics/save", Map.of("name", "testTopic1"));
            HttpResponse<?> response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> topic = response.getBody(Topic.class);
            assertTrue(topic.isPresent());
        }

        //show
        @Test
        void canShowTopicNotAssociatedToAGroup(){
            HttpRequest<?> request = HttpRequest.POST("/topics/save", Map.of("name", "testTopic1"));
            HttpResponse<?> response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> topic = response.getBody(Topic.class);
            assertTrue(topic.isPresent());

            request = HttpRequest.GET("/topics/show/"+topic.get().getId());
            response = blockingClient.exchange(request, TopicShowResponseDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicShowResponseDTO> topicShowResponse = response.getBody(TopicShowResponseDTO.class);
            assertTrue(topicShowResponse.isPresent());
            assertNotNull(topicShowResponse.get().getId());
        }

        @Test
        void canShowTopicAssociatedToAGroup(){
            // create group
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            // create topic
            request = HttpRequest.POST("/topics/save", Map.of("name", "testTopic1"));
            response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> topicOptional = response.getBody(Topic.class);
            assertTrue(topicOptional.isPresent());

            // add topic to group
            request = HttpRequest.POST("/groups/add_topic/"+theta.getId()+"/"+topicOptional.get().getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // show topic
            request = HttpRequest.GET("/topics/show/"+topicOptional.get().getId());
            response = blockingClient.exchange(request, TopicShowResponseDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicShowResponseDTO> topicShowResponse = response.getBody(TopicShowResponseDTO.class);
            assertTrue(topicShowResponse.isPresent());
            assertNotNull(topicShowResponse.get().getId());
            assertNotNull(topicShowResponse.get().getGroupId());
            assertNotNull(topicShowResponse.get().getGroupName());
        }

        // list all topics from all groups
        @Test
        void canListAllTopics(){
            Topic testTopic1 = new Topic("Abc123", TopicKind.B);
            Topic testTopic2 = new Topic("Xyz789", TopicKind.C);

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
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            assertEquals(3, topicPage.get().getContent().size());
        }

        @Test
        void canListAllTopicsWithFilter(){
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            Topic testTopic1 = new Topic("Abc123", TopicKind.B);
            Topic testTopic2 = new Topic("Xyz789", TopicKind.C);

            // create two topics and add to group
            request = HttpRequest.POST("/topics/save", testTopic1);
            response = blockingClient.exchange(request);
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
            assertEquals(3, topics.size());

            List<Map> testTopic1FromResponse = topics.stream().filter(t -> t.get("name").equals("Abc123")).collect(Collectors.toList());
            List<Map> testTopic2FromResponse = topics.stream().filter(t -> t.get("name").equals("Xyz789")).collect(Collectors.toList());
            Integer savedTopic1Id = (Integer) testTopic1FromResponse.get(0).get("id");
            Integer savedTopic1IdDup = (Integer) testTopic1FromResponse.get(1).get("id");
            Integer savedTopic2Id = (Integer) testTopic2FromResponse.get(0).get("id");

            request = HttpRequest.POST("/groups/add_topic/"+theta.getId()+"/"+savedTopic2Id, Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // support case-insensitive
            request = HttpRequest.GET("/topics?filter=xyz");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            assertEquals(1, topicPage.get().getContent().size());

            // group search
            request = HttpRequest.GET("/topics?filter=heta");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            assertEquals(1, topicPage.get().getContent().size());
        }

        //delete
    }

    @Nested
    class WhenAsATopicAdmin {

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

        // list - same functionality as member
        // show - same functionality as member
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
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        // show
        @Test
        void canShowTopicWithAssociatedGroup(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            // Group - Topics - Members
            // ---
            // Theta - Xyz789 - jjones
            // None - Abc123 - None

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
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create topics
            Topic testTopic1 = new Topic("Abc123", TopicKind.B);
            Topic testTopic2 = new Topic("Xyz789", TopicKind.C);

            request = HttpRequest.POST("/topics/save", testTopic1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", testTopic2);
            response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> xyzTopicOptional = response.getBody(Topic.class);
            assertTrue(xyzTopicOptional.isPresent());
            Topic xyzTopic = xyzTopicOptional.get();

            // add to group
            request = HttpRequest.POST("/groups/add_topic/"+theta.getId()+"/"+xyzTopic.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/topics/show/"+xyzTopic.getId());
            response = blockingClient.exchange(request, TopicShowResponseDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicShowResponseDTO> topicResponseOptional = response.getBody(TopicShowResponseDTO.class);
            assertTrue(topicResponseOptional.isPresent());
            assertEquals("Xyz789", topicResponseOptional.get().getName());
            assertEquals("Theta", topicResponseOptional.get().getGroupName());
        }

        @Test
        void cannotShowTopicIfTopicBelongsToAGroupIAmNotAMemberOf(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            // Group - Topics - Members
            // ---
            // Theta - Xyz789 - jjones
            // Omega - Abc123 - None

            // create groups
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            Group omega = new Group("Omega");
            request = HttpRequest.POST("/groups/save", omega);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> omegaOptional = response.getBody(Group.class);
            assertTrue(omegaOptional.isPresent());
            omega = omegaOptional.get();

            // add member to group
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(theta.getId());
            dto.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create topics
            Topic testTopic1 = new Topic("Abc123", TopicKind.B);
            Topic testTopic2 = new Topic("Xyz789", TopicKind.C);

            request = HttpRequest.POST("/topics/save", testTopic1);
            response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> abcTopicOptional = response.getBody(Topic.class);
            assertTrue(abcTopicOptional.isPresent());
            Topic abcTopic = abcTopicOptional.get();

            request = HttpRequest.POST("/topics/save", testTopic2);
            response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> xyzTopicOptional = response.getBody(Topic.class);
            assertTrue(xyzTopicOptional.isPresent());
            Topic xyzTopic = xyzTopicOptional.get();

            // add topics to group
            request = HttpRequest.POST("/groups/add_topic/"+theta.getId()+"/"+xyzTopic.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/groups/add_topic/"+omega.getId()+"/"+abcTopic.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/topics/show/"+abcTopic.getId());
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }

        // list
        @Test
        void canListAllTopicsLimitedToMembership(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            // Group - Topics
            // ---
            // Theta - Xyz789
            // None - Abc123

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
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create topics
            Topic testTopic1 = new Topic("Abc123", TopicKind.B);
            Topic testTopic2 = new Topic("Xyz789", TopicKind.C);

            request = HttpRequest.POST("/topics/save", testTopic1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", testTopic2);
            response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> xyzTopicOptional = response.getBody(Topic.class);
            assertTrue(xyzTopicOptional.isPresent());
            Topic xyzTopic = xyzTopicOptional.get();

            // add to group
            request = HttpRequest.POST("/groups/add_topic/"+theta.getId()+"/"+xyzTopic.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/topics");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            assertEquals(1, topicPage.get().getContent().size());
            Map expectedTopic = (Map) topicPage.get().getContent().get(0);
            assertEquals("Xyz789", expectedTopic.get("name"));
        }

        @Test
        void canListTopicsWithFilterLimitedToGroupMembership(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            // Group - Topics
            // ---
            // Theta - Xyz789
            // None - Abc123

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
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create topics
            Topic testTopic1 = new Topic("Abc123", TopicKind.B);
            Topic testTopic2 = new Topic("Xyz789", TopicKind.C);

            request = HttpRequest.POST("/topics/save", testTopic1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", testTopic2);
            response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> xyzTopicOptional = response.getBody(Topic.class);
            assertTrue(xyzTopicOptional.isPresent());
            Topic xyzTopic = xyzTopicOptional.get();

            // add to group
            request = HttpRequest.POST("/groups/add_topic/"+theta.getId()+"/"+xyzTopic.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            // support case-insensitive
            request = HttpRequest.GET("/topics?filter=xyz");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            assertEquals(1, topicPage.get().getContent().size());
            Map expectedTopic = (Map) topicPage.get().getContent().get(0);
            assertEquals("Xyz789", expectedTopic.get("name"));

            // Negative case
            request = HttpRequest.GET("/topics?filter=abc");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            assertEquals(0, topicPage.get().getContent().size());

            // group search
            request = HttpRequest.GET("/topics?filter=heta");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            assertEquals(1, topicPage.get().getContent().size());
            expectedTopic = (Map) topicPage.get().getContent().get(0);
            assertEquals("Xyz789", expectedTopic.get("name"));
        }
    }

    @Nested
    class WhenAsAUnauthenticatedUser {
        @BeforeEach
        void setup() {
            dbCleanup.cleanup();
            userRepository.save(new User("montesm@test.test", true));
        }

        void loginAsNonAdmin() {
            mockSecurityService.setServerAuthentication(null);
            mockAuthenticationFetcher.setAuthentication(null);
        }

        @Test
        void cannotListAllTopics(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            // Group - Topics
            // ---
            // Theta - Xyz789
            // None - Abc123

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
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create topics
            Topic testTopic1 = new Topic("Abc123", TopicKind.B);
            Topic testTopic2 = new Topic("Xyz789", TopicKind.C);

            request = HttpRequest.POST("/topics/save", testTopic1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", testTopic2);
            response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> xyzTopicOptional = response.getBody(Topic.class);
            assertTrue(xyzTopicOptional.isPresent());
            Topic xyzTopic = xyzTopicOptional.get();

            // add to group
            request = HttpRequest.POST("/groups/add_topic/"+theta.getId()+"/"+xyzTopic.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/topics");
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }

        @Test
        void cannotShowATopicWithoutGroupAssociation(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            // create topic
            Topic testTopic1 = new Topic("Abc123", TopicKind.B);

            HttpRequest<?> request = HttpRequest.POST("/topics/save", testTopic1);
            HttpResponse<?> response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> topicOptional = response.getBody(Topic.class);
            assertTrue(topicOptional.isPresent());

            loginAsNonAdmin();

            request = HttpRequest.GET("/topics/show/"+topicOptional.get().getId());
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }

        @Test
        void cannotShowATopicWithGroupAssociation(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            // Group - Topics
            // ---
            // Theta - Xyz789
            // None - Abc123

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
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create topics
            Topic testTopic1 = new Topic("Abc123", TopicKind.B);
            Topic testTopic2 = new Topic("Xyz789", TopicKind.C);

            request = HttpRequest.POST("/topics/save", testTopic1);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", testTopic2);
            response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Optional<Topic> xyzTopicOptional = response.getBody(Topic.class);
            assertTrue(xyzTopicOptional.isPresent());
            Topic xyzTopic = xyzTopicOptional.get();

            // add to group
            request = HttpRequest.POST("/groups/add_topic/"+theta.getId()+"/"+xyzTopic.getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            loginAsNonAdmin();

            request = HttpRequest.GET("/topics/show/"+xyzTopic.getId());
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @Disabled("Need to re-implement")
    class LegacyTests {
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
}
