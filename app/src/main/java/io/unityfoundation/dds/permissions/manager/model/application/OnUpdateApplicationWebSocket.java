package io.unityfoundation.dds.permissions.manager.model.application;

import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.ServerWebSocket;

import java.util.function.Predicate;

@ServerWebSocket("/ws/applications/{applicationId}")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class OnUpdateApplicationWebSocket {

    public static final String APPLICATION_UPDATED = "application_updated";
    public static final String APPLICATION_DELETED = "application_deleted";

    private final WebSocketBroadcaster broadcaster;

    public OnUpdateApplicationWebSocket(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void broadcastResourceEvent(String event, Long applicationId) {
        broadcaster.broadcastSync(event, session ->
                applicationId.equals(session.getUriVariables().get("applicationId", Long.class, null))
        );
    }

    @OnMessage
    public void onMessage(Long applicationId, String message, WebSocketSession session) {
//        broadcaster.broadcastSync(message, isValid(applicationId, session));
    }

    private Predicate<WebSocketSession> isValid(Long applicationId, WebSocketSession session) {
        return s -> s != session &&
                applicationId.equals(s.getUriVariables().get("applicationId", Long.class, null));
    }
}
