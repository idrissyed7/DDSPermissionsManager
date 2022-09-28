package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.format.MapFormat;
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

import java.util.List;
import java.util.Map;

@Requires(condition = DevDataCondition.class)
@ConfigurationProperties("bootstrap")
public class Bootstrap {

    @MapFormat(transformation = MapFormat.MapTransformation.NESTED)
    private Map<String, Object> data;

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;

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
                ((List<String>) data.get("admin-users")).stream().forEach(email -> userRepository.save(new User(email, true)));
            }
            if(data.containsKey("non-admin-users")) {
                ((List<String>) data.get("non-admin-users")).stream().forEach(email -> userRepository.save(new User(email)));
            }

            if(data.containsKey("groups")) {
                ((List<Map<String, ?>>) data.get("groups")).stream().forEach(groupMap -> {
                    String groupName = (String) groupMap.get("name");
                    Group group = groupRepository.save(new Group(groupName));

                    if (groupMap.containsKey("users")) {
                        List<String> users = (List<String>) groupMap.get("users");
                        users.stream().forEach(email -> {
                            groupUserRepository.save(new GroupUser(group, userRepository.findByEmail(email).get()));
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
