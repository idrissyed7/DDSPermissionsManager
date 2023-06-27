package io.unityfoundation.dds.permissions.manager;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicDTO;
import io.unityfoundation.dds.permissions.manager.model.topic.TopicKind;
import io.unityfoundation.dds.permissions.manager.testing.util.SecurityAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

@MicronautTest
@Property(name = "micronaut.security.reject-not-found", value = StringUtils.FALSE)
class TopicControllerSecuredTest {

    @Test
    void getApiTopicsShowIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/topics/show/99"));
    }

    @Test
    void getApiTopicsKindsIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/topics/kinds"));
    }

    @Test
    void getApiTopicsIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.GET("/api/topics"));
    }

    @Test
    void postApiTopicsDeleteIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.DELETE("/api/topics/99", Collections.emptyMap()));
    }

    @Test
    void postApiTopicsSaveIsSecured(@Client("/") HttpClient httpClient) throws IOException {
        TopicDTO topicDTO = new TopicDTO();
        topicDTO.setName("testTopic1");
        topicDTO.setKind(TopicKind.B);
        SecurityAssertions.assertUnauthorized(httpClient, HttpRequest.POST("/api/topics/save", topicDTO));
    }


}