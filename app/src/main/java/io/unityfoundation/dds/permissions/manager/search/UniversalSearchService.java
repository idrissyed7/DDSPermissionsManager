// Copyright 2023 DDS Permissions Manager Authors
package io.unityfoundation.dds.permissions.manager.search;

import io.micronaut.core.util.StringUtils;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.unityfoundation.dds.permissions.manager.model.DPMEntity;
import io.unityfoundation.dds.permissions.manager.model.application.Application;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.model.application.ApplicationRepository;
import io.unityfoundation.dds.permissions.manager.model.group.Group;
import io.unityfoundation.dds.permissions.manager.model.group.GroupRepository;
import io.unityfoundation.dds.permissions.manager.model.group.SimpleGroupDTO;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicDTO;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class UniversalSearchService {

    private final GroupRepository groupRepository;
    private final TopicRepository topicRepository;
    private final ApplicationRepository applicationRepository;

    public UniversalSearchService(GroupRepository groupRepository, TopicRepository topicRepository, ApplicationRepository applicationRepository) {
        this.groupRepository = groupRepository;
        this.topicRepository = topicRepository;
        this.applicationRepository = applicationRepository;
    }


    public Page<SearchResponseDTO> search(UniversalSearchParams searchParams) {

        boolean searchGroups = Boolean.TRUE.equals(searchParams.getGroups());
        boolean searchTopics = Boolean.TRUE.equals(searchParams.getTopics());
        boolean searchApplications = Boolean.TRUE.equals(searchParams.getApplications());
        String query = searchParams.getQuery();
        List<Long> entityIds;

        Pageable searchParamsPageable = searchParams.getPageable();
        int pageCount = Math.max(searchParamsPageable.getNumber(), 1);
        int pageSize = 10;
        if (searchParamsPageable.getSize() > 0) {
            pageSize = searchParamsPageable.getSize();
        }
        int start = (pageCount - 1) * pageSize;
        int end = pageCount * pageSize;
        if (searchGroups && !searchTopics && !searchApplications) {
            // just groups
            Page<Group> groups;
            if (!StringUtils.hasText(query)) {
                groups = groupRepository.findAllByMakePublicTrue(searchParamsPageable);
            } else {
                entityIds = groupRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                groups = groupRepository.findByMakePublicTrueAndIdIn(entityIds, searchParamsPageable);
            }
            return groups.map(group -> new SearchResponseDTO(DPMEntity.GROUP, new SimpleGroupDTO(group.getId(), group.getName(), group.getDescription(), group.getMakePublic())));
        } else if (!searchGroups && searchTopics && !searchApplications) {
            // just topics
            Page<Topic> topics;
            if (!StringUtils.hasText(query)) {
                topics = topicRepository.findAllByMakePublicTrue(searchParamsPageable);
            } else {
                entityIds = topicRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                topics = topicRepository.findByMakePublicTrueAndIdIn(entityIds, searchParamsPageable);
            }
            return topics.map(topic -> new SearchResponseDTO(DPMEntity.TOPIC, new TopicDTO(topic)));
        } else if (!searchGroups && !searchTopics && searchApplications) {
            // just applications
            Page<Application> applications;
            if (!StringUtils.hasText(query)) {
                applications = applicationRepository.findAllByMakePublicTrue(searchParamsPageable);
            } else {
                entityIds = applicationRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                applications = applicationRepository.findByMakePublicTrueAndIdIn(entityIds, searchParamsPageable);
            }
            return applications.map(application -> new SearchResponseDTO(DPMEntity.APPLICATION, new ApplicationDTO(application)));
        } else if (searchGroups && searchTopics && !searchApplications) {
            // groups and topics

            // the passed in pageable applies to the *combined* resultset (limited to 50 per resource)
            List<Group> groupsTop50;
            List<Topic> topicsTop50;
            if (!StringUtils.hasText(query)) {
                groupsTop50 = groupRepository.findTop50ByMakePublicTrue();
                topicsTop50 = topicRepository.findTop50ByMakePublicTrue();
            } else {
                entityIds = groupRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                groupsTop50 = groupRepository.findTop50ByMakePublicTrueAndIdIn(entityIds);
                entityIds = topicRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                topicsTop50 = topicRepository.findTop50ByMakePublicTrueAndIdIn(entityIds);
            }
            List<SearchResponseDTO> combined = Stream.concat(
                    groupsTop50.stream().map(group -> new SearchResponseDTO(DPMEntity.GROUP, new SimpleGroupDTO(group.getId(), group.getName(), group.getDescription(), group.getMakePublic()))),
                    topicsTop50.stream().map(topic -> new SearchResponseDTO(DPMEntity.TOPIC, new TopicDTO(topic)))
            ).collect(Collectors.toList());

            end = Math.min(end, combined.size());

            return Page.of(combined.subList(start, end), searchParamsPageable, combined.size());
        } else if (searchGroups && !searchTopics && searchApplications) {
            // groups and applications

            List<Group> groupsTop50;
            List<Application> applicationsTop50;
            if (!StringUtils.hasText(query)) {
                groupsTop50 = groupRepository.findTop50ByMakePublicTrue();
                applicationsTop50 = applicationRepository.findTop50ByMakePublicTrue();
            } else {
                entityIds = groupRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                groupsTop50 = groupRepository.findTop50ByMakePublicTrueAndIdIn(entityIds);
                entityIds = applicationRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                applicationsTop50 = applicationRepository.findTop50ByMakePublicTrueAndIdIn(entityIds);
            }
            List<SearchResponseDTO> combined = Stream.concat(
                    groupsTop50.stream().map(group -> new SearchResponseDTO(DPMEntity.GROUP, new SimpleGroupDTO(group.getId(), group.getName(), group.getDescription(), group.getMakePublic()))),
                    applicationsTop50.stream().map(application -> new SearchResponseDTO(DPMEntity.APPLICATION, new ApplicationDTO(application)))
            ).collect(Collectors.toList());

            end = Math.min(end, combined.size());

            return Page.of(combined.subList(start, end), searchParamsPageable, combined.size());
        } else if (!searchGroups && searchTopics && searchApplications) {
            // topics and applications
            List<Topic> topicsTop50;
            List<Application> applicationsTop50;
            if (!StringUtils.hasText(query)) {
                topicsTop50 = topicRepository.findTop50ByMakePublicTrue();
                applicationsTop50 = applicationRepository.findTop50ByMakePublicTrue();
            } else {
                entityIds = topicRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                topicsTop50 = topicRepository.findTop50ByMakePublicTrueAndIdIn(entityIds);
                entityIds = applicationRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                applicationsTop50 = applicationRepository.findTop50ByMakePublicTrueAndIdIn(entityIds);
            }
            List<SearchResponseDTO> combined = Stream.concat(
                    topicsTop50.stream().map(topic -> new SearchResponseDTO(DPMEntity.TOPIC, new TopicDTO(topic))),
                    applicationsTop50.stream().map(application -> new SearchResponseDTO(DPMEntity.APPLICATION, new ApplicationDTO(application)))
            ).collect(Collectors.toList());

            end = Math.min(end, combined.size());

            return Page.of(combined.subList(start, end), searchParamsPageable, combined.size());
        } else {
            // query all three entities in a pageable manner...
            List<Group> groupsTop50;
            List<Topic> topicsTop50;
            List<Application> applicationsTop50;
            if (!StringUtils.hasText(query)) {
                groupsTop50 = groupRepository.findTop50ByMakePublicTrue();
                topicsTop50 = topicRepository.findTop50ByMakePublicTrue();
                applicationsTop50 = applicationRepository.findTop50ByMakePublicTrue();
            } else {
                entityIds = groupRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                groupsTop50 = groupRepository.findTop50ByMakePublicTrueAndIdIn(entityIds);
                entityIds = topicRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                topicsTop50 = topicRepository.findTop50ByMakePublicTrueAndIdIn(entityIds);
                entityIds = applicationRepository.findIdByNameContainsIgnoreCaseOrDescriptionContainsIgnoreCase(query, query);
                applicationsTop50 = applicationRepository.findTop50ByMakePublicTrueAndIdIn(entityIds);
            }

            List<SearchResponseDTO> combined = Stream.concat(
                    groupsTop50.stream().map(group -> new SearchResponseDTO(DPMEntity.GROUP, new SimpleGroupDTO(group.getId(), group.getName(), group.getDescription(), group.getMakePublic()))),
                    Stream.concat(
                        topicsTop50.stream().map(topic -> new SearchResponseDTO(DPMEntity.TOPIC, new TopicDTO(topic))),
                        applicationsTop50.stream().map(application -> new SearchResponseDTO(DPMEntity.APPLICATION, new ApplicationDTO(application))))
            ).collect(Collectors.toList());

            end = Math.min(end, combined.size());

            return Page.of(combined.subList(start, end), searchParamsPageable, combined.size());
        }
    }
}
