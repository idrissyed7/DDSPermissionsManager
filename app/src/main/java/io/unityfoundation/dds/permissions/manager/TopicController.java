package io.unityfoundation.dds.permissions.manager;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicRepository;

import java.net.URI;
import java.util.Map;

@Controller("/topics")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class TopicController {
    private final TopicRepository topicRepository;

    public TopicController(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @View("/topics/index")
    @Produces(value = {MediaType.TEXT_HTML})
    @Get
    public HttpResponse index() {
        return HttpResponse.ok(Map.of("topics", topicRepository.findAll()));
    }

    @View("/create")
    @Produces(value = {MediaType.TEXT_HTML})
    @Get("/create")
    public HttpResponse create() {
        return HttpResponse.ok();
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = {MediaType.TEXT_HTML})
    HttpResponse<?> save(@Body Topic topic) {
        topicRepository.save(topic);
        return HttpResponse.seeOther(URI.create("/topics"));
    }

    @Post("/delete/{id}")
    @Produces(value = {MediaType.TEXT_HTML})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    HttpResponse<?> delete(Long id) {
        topicRepository.deleteById(id);
        return HttpResponse.seeOther(URI.create("/topics"));
    }
}
