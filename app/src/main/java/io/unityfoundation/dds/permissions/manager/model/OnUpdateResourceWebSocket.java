package io.unityfoundation.dds.permissions.manager.model;

import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;

import java.util.function.Predicate;

@ServerWebSocket("/resource/{topic}/{username}")
@Secured(SecurityRule.IS_ANONYMOUS)
public class OnUpdateResourceWebSocket {

    private final WebSocketBroadcaster broadcaster;

    public OnUpdateResourceWebSocket(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }


    @OnOpen // (2)
    public void onOpen(String topic, String username, WebSocketSession session) {
        String msg = "[" + username + "] Joined!";
        broadcaster.broadcastSync(msg, isValid(topic, session));
    }

    @OnMessage // (3)
    public void onMessage(String topic, String username,
                          String message, WebSocketSession session) {
        String msg = "[" + username + "] " + message;
        broadcaster.broadcastSync(msg, isValid(topic, session)); // (4)
    }

    @OnClose // (5)
    public void onClose(String topic, String username, WebSocketSession session) {
        String msg = "[" + username + "] Disconnected!";
        broadcaster.broadcastSync(msg, isValid(topic, session));
    }

    private Predicate<WebSocketSession> isValid(String topic, WebSocketSession session) {
        return s -> s != session &&
                topic.equalsIgnoreCase(s.getUriVariables().get("topic", String.class, null));
    }
}
