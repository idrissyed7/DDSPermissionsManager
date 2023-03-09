package io.unityfoundation.dds.permissions.manager.search;

import io.unityfoundation.dds.permissions.manager.model.application.ApplicationDTO;
import io.unityfoundation.dds.permissions.manager.model.group.SimpleGroupDTO;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicDTO;

import java.util.List;

public class SearchResponseDTO {
    private List<SimpleGroupDTO> groups;
    private List<ApplicationDTO> applications;
    private List<TopicDTO> topics;
}
