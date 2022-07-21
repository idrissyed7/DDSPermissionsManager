package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicService;

import javax.validation.Valid;
import java.net.URI;

@Controller("/topics")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class TopicController {
    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @Get
    public HttpResponse index(@Valid Pageable pageable) {
        return HttpResponse.ok(topicService.findAll(pageable));
    }

    @Get("/create")
    public HttpResponse create() {
        return HttpResponse.ok();
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    HttpResponse<?> save(@Body Topic topic) {
        topicService.save(topic);
        return HttpResponse.seeOther(URI.create("/topics"));
    }

    @Post("/delete/{id}")
    HttpResponse<?> delete(Long id) {
        topicService.deleteById(id);
        return HttpResponse.seeOther(URI.create("/topics"));
    }
}
