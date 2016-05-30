package io.bekti.anubis.server.ws;

import io.bekti.anubis.server.workers.MainWorkerThread;
import io.bekti.anubis.server.types.OutboundMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@WebSocket
public class AnubisWebSocketServletHandler {

    private Logger log = LoggerFactory.getLogger(AnubisWebSocketServletHandler.class);

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        log.info("Received message: {}", message);

        try {
            JSONObject body = new JSONObject(message);
            String action = body.getString("event");

            switch (action) {
                case "subscribe":
                    subscribe(session, body);
                    break;
                case "publish":
                    publish(session, body);
                    break;
                case "commit":
                    commit(session, body);
                    break;
                case "seek":
                    seek(session, body);
                    break;
                case "unsubscribe":
                    unsubscribe(session);
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("{} connected!", session.getRemoteAddress().getHostString());

        createWorker(session);
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        log.info("{} closed!", session.getRemoteAddress().getHostString());

        closeWorker(session);
    }

    private void createWorker(Session session) {
        closeWorker(session);

        MainWorkerThread worker = new MainWorkerThread(session);
        worker.start();

        AnubisWebSocketServer.getWorkers().put(session, worker);
    }

    private void subscribe(Session session, JSONObject body) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            String groupId = body.getString("groupId");
            JSONArray jsonTopicsArray = body.getJSONArray("topics");

            List<String> topics = new ArrayList<>();

            for (int i = 0; i < jsonTopicsArray.length(); ++i) {
                topics.add(jsonTopicsArray.getString(i));
            }

            worker.subscribe(topics, groupId);
        }
    }

    private void unsubscribe(Session session) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            worker.unsubscribe();
        }
    }

    private void publish(Session session, JSONObject body) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            String topic = body.getString("topic");
            String key = body.has("key") ? body.getString("key") : null;
            String value = body.getString("value");
            worker.enqueueOutboundMessage(new OutboundMessage(topic, key, value));
        }
    }

    private void commit(Session session, JSONObject body) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            String topic = body.getString("topic");
            int partition = body.getInt("partition");
            long offset = body.getLong("offset");
            worker.commit(topic, partition, offset);
        }
    }

    private void seek(Session session, JSONObject body) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            String topic = body.getString("topic");

            String offset;

            try {
                offset = body.getString("offset");
            } catch (JSONException e) {
                offset = String.valueOf(body.getLong("offset"));
            }

            worker.seek(topic, offset);
        }
    }

    private void closeWorker(Session session) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            worker.shutdown();

            try {
                worker.join();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}