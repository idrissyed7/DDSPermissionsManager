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
import io.unityfoundation.dds.permissions.manager.model.topic.TopicDTO;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import io.unityfoundation.dds.permissions.manager.testing.util.DbCleanup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.*;
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
            HttpRequest<?> request = HttpRequest.POST("/topics/save", topicDTO);
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
            assertEquals(BAD_REQUEST, exception.getStatus());

            topicDTO.setName("     ");
            request = HttpRequest.POST("/topics/save", topicDTO);
            HttpRequest<?> finalRequest1 = request;
            HttpClientResponseException exception1 = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest1);
            });
            assertEquals(BAD_REQUEST, exception1.getStatus());
        }

        @Test
        public void cannotCreateWithNameLessThanThreeCharacters() {

            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            // create topics
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setName("A");
            topicDTO.setKind(TopicKind.B);
            topicDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", topicDTO);

            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(BAD_REQUEST, exception.getStatus());
        }

        @Test
        public void createShouldTrimNameWhitespaces() {
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
        public void cannotUpdateTopic() {
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
            Optional<TopicDTO> topicOptional = response.getBody(TopicDTO.class);
            assertTrue(topicOptional.isPresent());
            assertEquals("Abc123", topicOptional.get().getName());

            // update attempt
            topicDTO = topicOptional.get();
            topicDTO.setName("UpdatedTestTopic2");
            topicDTO.setKind(TopicKind.C);
            request = HttpRequest.POST("/topics/save", topicDTO);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(BAD_REQUEST, thrown.getStatus());
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
            // Theta - Xyz789 & Def456
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

            defDTO.setGroup(theta.getId());
            request = HttpRequest.POST("/topics/save", defDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());

            request = HttpRequest.GET("/topics");
            response = blockingClient.exchange(request, Page.class);
            assertEquals(OK, response.getStatus());
            Optional<Page> topicPage = response.getBody(Page.class);
            assertTrue(topicPage.isPresent());
            List<Map> topics = topicPage.get().getContent();

            // topic names sorted
            List<String> topicNames = topics.stream()
                    .flatMap(map -> Stream.of((String) map.get("name")))
                    .collect(Collectors.toList());
            assertEquals(topicNames.stream().sorted().collect(Collectors.toList()), topicNames);

            // group names should be sorted by topic
            List<String> defTopics = topics.stream().filter(map -> {
                String topicName = (String) map.get("name");
                return topicName.equals("Def456");
            }).flatMap(map -> Stream.of((String) map.get("groupName"))).collect(Collectors.toList());
            assertEquals(defTopics.stream().sorted().collect(Collectors.toList()), defTopics);
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

            List<String> topicNames = topics.stream()
                    .flatMap(map -> Stream.of((String) map.get("name")))
                    .collect(Collectors.toList());
            assertEquals(topicNames.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()), topicNames);
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
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());
        }

        // list - same functionality as member
        // show - same functionality as member

        // create
        @Test
        void canCreateTopic(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

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

            loginAsNonAdmin();

            // create topics
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setName("Abc123");
            topicDTO.setKind(TopicKind.B);
            topicDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", topicDTO);
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }

        // delete
        @Test
        void canDeleteTopic(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

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

            loginAsNonAdmin();

            // delete attempt
            request = HttpRequest.POST("/topics/delete/"+topic.get().getId(), Map.of());
            response = blockingClient.exchange(request);
            assertEquals(OK, response.getStatus());
        }

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

        // create
        @Test
        void cannotCreateTopic(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

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

            loginAsNonAdmin();

            // create topics
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setName("Abc123");
            topicDTO.setKind(TopicKind.B);
            topicDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", topicDTO);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED,exception.getStatus());
        }

        // delete
        @Test
        void cannotDeleteTopic(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

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
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setName("Abc123");
            topicDTO.setKind(TopicKind.B);
            topicDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", topicDTO);
            response = blockingClient.exchange(request, TopicDTO.class);
            assertEquals(OK, response.getStatus());
            Optional<TopicDTO> topic = response.getBody(TopicDTO.class);
            assertTrue(topic.isPresent());

            loginAsNonAdmin();

            // delete attempt
            HttpRequest<?> request2 = HttpRequest.POST("/topics/delete/"+topic.get().getId(), Map.of());
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(request2);
            });
            assertEquals(UNAUTHORIZED,exception.getStatus());
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
    class WhenAsANonGroupMember {

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

        // create
        @Test
        void cannotCreateTopic(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

            Group theta = new Group("Theta");
            HttpRequest<?> request = HttpRequest.POST("/groups/save", theta);
            HttpResponse<?> response = blockingClient.exchange(request, Group.class);
            assertEquals(OK, response.getStatus());
            Optional<Group> thetaOptional = response.getBody(Group.class);
            assertTrue(thetaOptional.isPresent());
            theta = thetaOptional.get();

            loginAsNonAdmin();

            // create topics
            TopicDTO topicDTO = new TopicDTO();
            topicDTO.setName("Abc123");
            topicDTO.setKind(TopicKind.B);
            topicDTO.setGroup(theta.getId());

            request = HttpRequest.POST("/topics/save", topicDTO);
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED,exception.getStatus());
        }

        // delete
        @Test
        void cannotDeleteTopic(){
            mockSecurityService.postConstruct();
            mockAuthenticationFetcher.setAuthentication(mockSecurityService.getAuthentication().get());

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

            loginAsNonAdmin();

            // delete attempt
            request = HttpRequest.POST("/topics/delete/"+topic.get().getId(), Map.of());
            HttpRequest<?> finalRequest = request;
            HttpClientResponseException exception = assertThrowsExactly(HttpClientResponseException.class, () -> {
                blockingClient.exchange(finalRequest);
            });
            assertEquals(UNAUTHORIZED,exception.getStatus());
        }

        // show
        @Test
        void cannotShowTopicWithAssociatedGroup(){
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
            assertEquals(UNAUTHORIZED,exception.getStatus());
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

    private HttpResponse<?> createApplication(String applicationName, Long groupId) {
        ApplicationDTO applicationDTO = new ApplicationDTO();
        applicationDTO.setName(applicationName);
        applicationDTO.setGroup(groupId);

        HttpRequest<?> request = HttpRequest.POST("/applications/save", applicationDTO);
        return blockingClient.exchange(request, ApplicationDTO.class);
    }
}
