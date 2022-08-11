package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
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

    public MutableHttpResponse save(Topic topic) throws Exception {
        if (topic.getId() == null) {
            return HttpResponse.ok(topicRepository.save(topic));
        } else {
            throw new Exception("Update of Topics are not allowed.");
        }
    }

    public void deleteById(Long id) {
        topicRepository.deleteById(id);
    }
}
