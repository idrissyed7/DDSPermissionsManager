package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.util.StringUtils;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUser;
import io.unityfoundation.dds.permissions.manager.model.groupuser.GroupUserRepository;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import io.unityfoundation.dds.permissions.manager.model.user.User;
import io.unityfoundation.dds.permissions.manager.model.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Requires(property = "dpm.bootstrap.data.enabled", value = StringUtils.TRUE)
@ConfigurationProperties("dpm.bootstrap")
public class Bootstrap {

    @MapFormat(transformation = MapFormat.MapTransformation.NESTED)
    private Map<String, Object> data;

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    public Bootstrap(UserRepository userRepository, GroupRepository groupRepository,
            GroupUserRepository groupUserRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupUserRepository = groupUserRepository;
    }

    @EventListener
    public void devData(ServerStartupEvent event) {
        if(data != null) {
            if(data.containsKey("admin-users")) {
                ((List<String>) data.get("admin-users")).stream().forEach(email -> {
                        LOG.info(email + " is now a super admin");
                        userRepository.save(new User(email, true));
                    });
            }
            if(data.containsKey("non-admin-users")) {
                ((List<String>) data.get("non-admin-users")).stream().forEach(email -> userRepository.save(new User(email)));
            }

            if(data.containsKey("groups")) {
                ((List<Map<String, ?>>) data.get("groups")).stream().forEach(groupMap -> {
                    String groupName = (String) groupMap.get("name");
                    Group group = groupRepository.save(new Group(groupName));

                    if (groupMap.containsKey("users")) {
                        List<Map> users = (List<Map>) groupMap.get("users");
                        users.stream().forEach((Map user) -> {
                            String email = (String) user.get("email");
                            GroupUser groupUser = new GroupUser(group, userRepository.findByEmail(email).get());

                            if(user.containsKey("admin-flags")) {
                                List<String> adminFlags = (List<String>) user.get("admin-flags");
                                groupUser.setGroupAdmin(adminFlags.contains("group"));
                                groupUser.setApplicationAdmin(adminFlags.contains("application"));
                                groupUser.setTopicAdmin(adminFlags.contains("topic"));
                            }

                            groupUserRepository.save(groupUser);
                        });
                    }

                    if (groupMap.containsKey("topics")) {
                        List<Map<String, String>> topics = (List<Map<String, String>>) groupMap.get("topics");
                        topics.stream().forEach(topicMap -> {
                            String name = topicMap.get("name");
                            TopicKind kind = TopicKind.valueOf(topicMap.get("kind"));
                            group.addTopic(new Topic(name, kind, group));
                        });
                    }

                    if (groupMap.containsKey("applications")) {
                        List<String> applications = (List<String>) groupMap.get("applications");
                        applications.stream().forEach(applicationName -> {
                            group.addApplication(new Application(applicationName, group));
                        });
                    }

                    groupRepository.update(group);
                });
            }
        }
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
