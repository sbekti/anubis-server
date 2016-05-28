package io.bekti.anubis.server.http;

import io.bekti.anubis.server.kafka.KafkaWebSocketClient;
import io.bekti.anubis.server.types.OutboundMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebSocket
public class KafkaWebSocketServletHandler {

    private Logger log = LoggerFactory.getLogger(KafkaWebSocketServletHandler.class);

    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException {
        log.info("Received message: {}", message);

        try {
            JSONObject body = new JSONObject(message);
            String action = body.getString("action");

            switch (action) {
                case "connect":
                    createKafkaClient(session, body);
                    break;
                case "publish":
                    publishMessage(session, body);
                    break;
                case "seek":
                    seekConsumer(session, body);
                    break;
                case "disconnect":
                    closeKafkaClient(session);
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        log.info("{} connected!", session.getRemoteAddress().getHostString());
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        log.info("{} closed!", session.getRemoteAddress().getHostString());

        closeKafkaClient(session);
    }

    private void createKafkaClient(Session session, JSONObject body) {
        closeKafkaClient(session);

        String groupId = body.getString("groupId");
        JSONArray jsonTopicsArray = body.getJSONArray("topics");

        List<String> topics = new ArrayList<>();

        for (int i = 0; i < jsonTopicsArray.length(); ++i) {
            topics.add(jsonTopicsArray.getString(i));
        }

        KafkaWebSocketClient client = new KafkaWebSocketClient(groupId, topics, session);
        client.start();

        WebSocketServer.getKafkaClients().put(session, client);
    }

    private void publishMessage(Session session, JSONObject body) {
        KafkaWebSocketClient client = WebSocketServer.getKafkaClients().get(session);

        if (client != null) {
            String topic = body.getString("topic");
            String key = body.has("key") ? body.getString("key") : null;
            String value = body.getString("value");
            client.enqueueOutboundMessage(new OutboundMessage(topic, key, value));
        }
    }

    private void seekConsumer(Session session, JSONObject body) {
        KafkaWebSocketClient client = WebSocketServer.getKafkaClients().get(session);

        if (client != null) {
            String topic = body.getString("topic");
            String offset = body.getString("offset");
            client.requestSeek(topic, offset);
        }
    }

    private void closeKafkaClient(Session session) {
        KafkaWebSocketClient client = WebSocketServer.getKafkaClients().get(session);

        if (client != null) {
            client.shutdown();

            try {
                client.join();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}