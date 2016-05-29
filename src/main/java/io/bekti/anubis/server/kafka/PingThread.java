package io.bekti.anubis.server.kafka;

import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PingThread extends Thread {

    private static Logger log = LoggerFactory.getLogger(DispatcherThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private Session session;
    private KafkaWebSocketClient client;

    public PingThread(Session session, KafkaWebSocketClient client) {
        this.session = session;
        this.client = client;
    }

    @Override
    public void run() {
        running.set(true);

        JSONObject payload = new JSONObject();
        payload.put("event", "ping");

        while (running.get()) {
            try {
                if (session.isOpen()) {
                    session.getRemote().sendString(payload.toString());
                } else {
                    client.shutdown();
                }

                Thread.sleep(5000);
            } catch (IOException ioe) {
                client.shutdown();
            } catch (InterruptedException ignored) {

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void shutdown() {
        if (running.get()) {
            running.set(false);
            Thread.currentThread().interrupt();
        }
    }

}
