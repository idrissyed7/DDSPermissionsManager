package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Singleton;

@Singleton
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public Page<Topic> findAll(Pageable pageable) {
        return topicRepository.findAll(pageable);
    }

    public void save(Topic topic) throws Exception {
        if (topic.getId() == null) {
            topicRepository.save(topic);
        } else {
            throw new Exception("Update of Topics are not allowed.");
        }
    }

    public void deleteById(Long id) {
        topicRepository.deleteById(id);
    }
}
