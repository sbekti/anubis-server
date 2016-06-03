package io.bekti.anubis.server.workers;

import io.bekti.anubis.server.messages.PingMessage;
import io.bekti.anubis.server.utils.SharedConfiguration;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class PingThread extends Thread {

    private static Logger log = LoggerFactory.getLogger(DispatcherThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private Session session;
    private MainWorkerThread client;

    public PingThread(Session session, MainWorkerThread client) {
        this.session = session;
        this.client = client;
    }

    @Override
    public void run() {
        running.set(true);

        long pingInterval = SharedConfiguration.getLong("ping.interval.ms");

        while (running.get()) {
            try {
                if (session.isOpen()) {
                    ByteBuffer pingPayload = generatePingPayload();
                    session.getRemote().sendPing(pingPayload);
                } else {
                    client.shutdown();
                }

                Thread.sleep(pingInterval);
            } catch (InterruptedException ignored) {

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                client.shutdown();
            }
        }
    }

    private ByteBuffer generatePingPayload() {
        PingMessage pingMessage = new PingMessage();
        pingMessage.setWatchDogTimeout(SharedConfiguration.getLong("watchdog.timeout.ms"));

        ByteBuffer payload = ByteBuffer.wrap(pingMessage.toJson().getBytes());
        return payload;
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
