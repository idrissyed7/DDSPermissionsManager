package io.unityfoundation.dds.permissions.manager;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.unityfoundation.dds.permissions.manager.exception.DPMErrorResponse;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicDTO;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicService;

import javax.validation.Valid;

@Controller("/api/topics")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Tag(name = "topic")
public class TopicController {
    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @ExecuteOn(TaskExecutors.IO)
    @Get("{?filter,group}")
    public Page<TopicDTO> index(@Valid Pageable pageable, @Nullable String filter, @Nullable Long group) {
        return topicService.findAll(pageable, filter, group);
    }

    @Get("/kinds")
    public HttpResponse<TopicKind[]> getKinds() {
        return HttpResponse.ok(TopicKind.values());
    }

    @Post("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TopicDTO.class))
    )
    @ApiResponse(responseCode = "4xx", description = "Bad Request.",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DPMErrorResponse.class)))
    )
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> save(@Body @Valid TopicDTO topic) {
        return topicService.save(topic);
    }

    @Get("/show/{id}")
    @ApiResponse(
            responseCode = "200",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TopicDTO.class))
    )
    @ApiResponse(responseCode = "4xx", description = "Bad Request.",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DPMErrorResponse.class)))
    )
    @ExecuteOn(TaskExecutors.IO)
    public HttpResponse show(Long id) {
        return topicService.show(id);
    }

    @Delete("/delete/{id}")
    @ApiResponse(responseCode = "303", description = "Returns result of /topics")
    @ApiResponse(responseCode = "4xx", description = "Bad Request.",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = DPMErrorResponse.class)))
    )
    @ExecuteOn(TaskExecutors.IO)
    HttpResponse<?> delete(Long id) {
        return topicService.deleteById(id);
    }
}
