package io.unityfoundation.dds.permissions.manager.model.topic;

import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.ServerWebSocket;

import java.util.function.Predicate;

@ServerWebSocket("/ws/topics/{topicId}")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class OnUpdateTopicWebSocket {

    public static final String TOPIC_UPDATED = "topic_updated";
    public static final String TOPIC_DELETED = "topic_deleted";

    private final WebSocketBroadcaster broadcaster;

    public OnUpdateTopicWebSocket(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void broadcastResourceEvent(String event, Long topicId) {
        broadcaster.broadcastSync(event, session ->
                topicId.equals(session.getUriVariables().get("topicId", Long.class, null))
        );
    }

    @OnMessage
    public void onMessage(Long topicId, String message, WebSocketSession session) {
//        broadcaster.broadcastSync(message, isValid(topicId, session));
    }

    private Predicate<WebSocketSession> isValid(Long topicId, WebSocketSession session) {
        return s -> s != session &&
                topicId.equals(s.getUriVariables().get("topicId", Long.class, null));
    }
}
