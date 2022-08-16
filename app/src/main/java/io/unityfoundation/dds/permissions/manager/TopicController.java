package io.unityfoundation.dds.permissions.manager;

import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.rules.SecurityRule;
import io.unityfoundation.dds.permissions.manager.model.topic.Topic;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
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

    @Get("kinds")
    public HttpResponse getKinds() {
        return HttpResponse.ok(TopicKind.values());
    }

    @Get("/create")
    public HttpResponse create() {
        return HttpResponse.ok();
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    HttpResponse<?> save(@Body Topic topic) {
        try {
            return topicService.save(topic);
        } catch (AuthenticationException ae) {
            return HttpResponse.unauthorized();
        } catch (Exception e) {
            return HttpResponse.badRequest(e.getMessage());
        }
    }

    @Post("/delete/{id}")
    HttpResponse<?> delete(Long id) {
        try {
            topicService.deleteById(id);
        } catch (AuthenticationException ae) {
            return HttpResponse.unauthorized();
        } catch (Exception e) {
            HttpResponse.badRequest(e.getMessage());
        }
        return HttpResponse.seeOther(URI.create("/topics"));
    }
}
