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
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
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
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.micronaut.http.HttpStatus.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class TopicApiTest {

    private BlockingHttpClient blockingClient;

    @Inject
    @Client("/api")
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
        void cannotCreateOnItsOwnWithoutAGroupAssociation(){
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setName("testTopic1");
            topicDTO.setKind(TopicKind.B);
            HttpRequest<?> request = HttpRequest.POST("/topics/save", topicDTO);;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
        }

        @Test
        void canCreateWithGroupAssociation(){
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            // create topics
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setName("Abc123");
            topicDTO.setKind(TopicKind.B);
            topicDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", topicDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topic = response.getBody(TopicDTO.class);
            assertTrue(topic.isPresent());
        }

        @Test
        public void cannotCreateGroupWithNullNorWhitespace() {

            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            // create topics
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setKind(TopicKind.B);
            topicDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", topicDTO);

            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());;

            topicDTO.setName("     ");
            request = HttpRequest.POST("/topics/save", topicDTO);
            HttpRequest<?> finalRequest1 = request;
            HttpClientResponseException exception1 = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest1);
            });
            assertEquals(BAD_REQUEST, exception1.getStatus());
        }

        @Test
        public void createShouldTrimWhitespace() {
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            // create topics
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setName("   Abc123  ");
            topicDTO.setKind(TopicKind.B);
            topicDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", topicDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topic = response.getBody(TopicDTO.class);
            assertTrue(topic.isPresent());
            assertEquals("Abc123", topic.get().getName());
        }

        @Test
        public void cannotCreateTopicWithSameNameInGroup() {
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            // create topics
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setName("Abc123");
            topicDTO.setKind(TopicKind.B);
            topicDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", topicDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topic = response.getBody(TopicDTO.class);
            assertTrue(topic.isPresent());
            assertEquals("Abc123", topic.get().getName());

            // confirm it is topic with kind B and not C
            topicDTO.setKind(TopicKind.C);
            request = HttpRequest.POST("/topics/save", topicDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(SEE_OTHER, response.getStatus());
            topic = response.getBody(TopicDTO.class);
            assertTrue(topic.isPresent());
            assertEquals(TopicKind.B, topic.get().getKind());
        }

        //show
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
            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> xyzTopicOptional = response.getBody(TopicDTO.class);
            assertTrue(xyzTopicOptional.isPresent());
            TopicDTO xyzTopic = xyzTopicOptional.get();

            // show topic
            request = HttpRequest.GET("/topics/show/"+xyzTopic.getId());
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topicShowResponse = response.getBody(TopicDTO.class);
            assertTrue(topicShowResponse.isPresent());
            assertNotNull(topicShowResponse.get().getId());
            assertNotNull(topicShowResponse.get().getGroup());
            assertNotNull(topicShowResponse.get().getGroupName());
        }

        // list all topics from all groups
        @Test
        void canListAllTopicsAndTopicsWithSameNameCanExistSitewide(){
            // Group - Topics
            // ---
            // Green - Xyz789
            // Yellow - Abc123 & Xyz789

            // create groups
            Group green = new Group("Green");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", green);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> greenOptional = response.getBody(Group.class);
            assertTrue(greenOptional.isPresent());
            green = greenOptional.get();

            Group yellow = new Group("Yellow");
            request = HttpRequest.POST("/groups/save", yellow);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> yellowOptional = response.getBody(Group.class);
            assertTrue(yellowOptional.isPresent());
            yellow = yellowOptional.get();

            // create topics
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(yellow.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(green.getId());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // site-wide test
            xyzDTO.setGroup(yellow.getId());
            request = HttpRequest.POST("/topics/save", xyzDTO);
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
            // Group - Topics
            // ---
            // Theta - Xyz789
            // Zeta - Abc123

            // create groups
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            Group zeta = new Group("Zeta");
            request = HttpRequest.POST("/groups/save", zeta);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> zetaOptional = response.getBody(Group.class);
            assertTrue(zetaOptional.isPresent());
            zeta = zetaOptional.get();

            // create topics
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(zeta.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", xyzDTO);
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

        @Test
        void canListAllTopicsNameInAscendingOrderByDefault(){
            // Group - Topics
            // ---
            // Theta - Xyz789
            // Zeta - Abc123 & Def456

            // create groups
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            Group zeta = new Group("Zeta");
            request = HttpRequest.POST("/groups/save", zeta);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> zetaOptional = response.getBody(Group.class);
            assertTrue(zetaOptional.isPresent());
            zeta = zetaOptional.get();

            // create topics
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(zeta.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            TopicDTO defDTO = new TopicDTO();
            defDTO.setName("Def456");
            defDTO.setKind(TopicKind.C);
            defDTO.setGroup(zeta.getId());

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", defDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/topics");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            List<Map> topics = topicPage.get().getContent();

            List<String> groupNames = topics.stream()
                    .flatMap(map -> Stream.of((String) map.get("groupName")))
                    .collect(Collectors.toList());
            assertEquals(groupNames.stream().sorted().collect(Collectors.toList()), groupNames);

            List<String> zetaTopics = topics.stream().filter(map -> {
                String groupName = (String) map.get("groupName");
                return groupName.equals("Zeta");
            }).flatMap(map -> Stream.of((String) map.get("groupName"))).collect(Collectors.toList());
            assertEquals(zetaTopics.stream().sorted().collect(Collectors.toList()), zetaTopics);
        }

        @Test
        void canListAllTopicsNameInDescendingOrder(){
            // Group - Topics
            // ---
            // Theta - Xyz789
            // Zeta - Abc123 & Def456

            // create groups
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            Group zeta = new Group("Zeta");
            request = HttpRequest.POST("/groups/save", zeta);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> zetaOptional = response.getBody(Group.class);
            assertTrue(zetaOptional.isPresent());
            zeta = zetaOptional.get();

            // create topics
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(zeta.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            TopicDTO defDTO = new TopicDTO();
            defDTO.setName("Def456");
            defDTO.setKind(TopicKind.C);
            defDTO.setGroup(zeta.getId());

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", defDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/topics?sort=name,desc");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            List<Map> topics = topicPage.get().getContent();

            List<String> zetaTopics = topics.stream().filter(map -> {
                String groupName = (String) map.get("groupName");
                return groupName.equals("Zeta");
            }).flatMap(map -> Stream.of((String) map.get("groupName"))).collect(Collectors.toList());
            assertEquals(zetaTopics.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()), zetaTopics);
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

        @Test
        void cannotDeleteAnApplication(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

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
            dto.setTopicAdmin(true);
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create application
            response = createApplication("ApplicationOne", theta.getId());
            assertEquals(OK, response.getStatus());
            Optional<ApplicationDTO> applicationOneOptional = response.getBody(ApplicationDTO.class);
            assertTrue(applicationOneOptional.isPresent());
            ApplicationDTO applicationOne = applicationOneOptional.get();

            loginAsNonAdmin();

            request = HttpRequest.POST("/applications/delete/"+applicationOne.getId(), Map.of());
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
            // Zeta - Abc123 - None

            // create groups
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            Group zeta = new Group("Zeta");
            request = HttpRequest.POST("/groups/save", zeta);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> zetaOptional = response.getBody(Group.class);
            assertTrue(zetaOptional.isPresent());
            zeta = zetaOptional.get();

            // add member to group
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(theta.getId());
            dto.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create topics
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(zeta.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> xyzTopicOptional = response.getBody(TopicDTO.class);
            assertTrue(xyzTopicOptional.isPresent());
            TopicDTO xyzTopic = xyzTopicOptional.get();

            loginAsNonAdmin();

            request = HttpRequest.GET("/topics/show/"+xyzTopic.getId());
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topicResponseOptional = response.getBody(TopicDTO.class);
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
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(omega.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> abcTopicOptional = response.getBody(TopicDTO.class);
            assertTrue(abcTopicOptional.isPresent());
            TopicDTO abcTopic = abcTopicOptional.get();

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> xyzTopicOptional = response.getBody(TopicDTO.class);
            assertTrue(xyzTopicOptional.isPresent());
            TopicDTO xyzTopic = xyzTopicOptional.get();

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
            // Zeta - Abc123

            // create groups
            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            Group zeta = new Group("Zeta");
            request = HttpRequest.POST("/groups/save", zeta);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> zetaOptional = response.getBody(Group.class);
            assertTrue(zetaOptional.isPresent());
            zeta = zetaOptional.get();

            // add member to group
            GroupUserDTO dto = new GroupUserDTO();
            dto.setPermissionsGroup(theta.getId());
            dto.setEmail("jjones@test.test");
            request = HttpRequest.POST("/group_membership", dto);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            // create topics
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(zeta.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> xyzTopicOptional = response.getBody(TopicDTO.class);
            assertTrue(xyzTopicOptional.isPresent());
            TopicDTO xyzTopic = xyzTopicOptional.get();

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
            // Zeta - Abc123

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

            // other group
            Group zeta = new Group("Zeta");
            request = HttpRequest.POST("/groups/save", zeta);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> zetaOptional = response.getBody(Group.class);
            assertTrue(zetaOptional.isPresent());
            zeta = zetaOptional.get();

            // create topics
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(zeta.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> xyzTopicOptional = response.getBody(TopicDTO.class);
            assertTrue(xyzTopicOptional.isPresent());
            TopicDTO xyzTopic = xyzTopicOptional.get();

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
            // Zeta - Abc123

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

            // other group
            Group zeta = new Group("Zeta");
            request = HttpRequest.POST("/groups/save", zeta);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> zetaOptional = response.getBody(Group.class);
            assertTrue(zetaOptional.isPresent());
            zeta = zetaOptional.get();

            // create topics
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(zeta.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> xyzTopicOptional = response.getBody(TopicDTO.class);
            assertTrue(xyzTopicOptional.isPresent());
            TopicDTO xyzTopic = xyzTopicOptional.get();

            loginAsNonAdmin();

            request = HttpRequest.GET("/topics");
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
            // Zeta - Abc123

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

            // other group
            Group zeta = new Group("Zeta");
            request = HttpRequest.POST("/groups/save", zeta);
            response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> zetaOptional = response.getBody(Group.class);
            assertTrue(zetaOptional.isPresent());
            zeta = zetaOptional.get();

            // create topics
            TopicDTO abcDTO = new TopicDTO();
            abcDTO.setName("Abc123");
            abcDTO.setKind(TopicKind.B);
            abcDTO.setGroup(zeta.getId());

            TopicDTO xyzDTO = new TopicDTO();
            xyzDTO.setName("Xyz789");
            xyzDTO.setKind(TopicKind.B);
            xyzDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", abcDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.POST("/topics/save", xyzDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> xyzTopicOptional = response.getBody(TopicDTO.class);
            assertTrue(xyzTopicOptional.isPresent());
            TopicDTO xyzTopic = xyzTopicOptional.get();

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

            // update attempt should fail // todo
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
            theta.setPermissionsGroup(cityGovernment);
            request = HttpRequest.POST("/topics/save", theta);
            response = blockingClient.exchange(request, Topic.class);
            assertEquals(OK, response.getStatus());
            Topic createdTopic = response.getBody(Topic.class).get();

            // attempt to create a topic in another group that user is not a topic admin of
            Topic otherTheta = new Topic("OtherTheta", TopicKind.B);
            theta.setPermissionsGroup(otherGroup);
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

    private HttpResponse<?> createApplication(String applicationName, Long groupId) {
        ApplicationDTO applicationDTO = new ApplicationDTO();
        applicationDTO.setName(applicationName);
        applicationDTO.setGroup(groupId);

        HttpRequest<?> request = HttpRequest.POST("/applications/save", applicationDTO);
        return blockingClient.exchange(request, ApplicationDTO.class);
    }
}
