package io.bekti.anubis.server.worker;

import io.bekti.anubis.server.model.message.PingMessage;
import io.bekti.anubis.server.util.ConfigUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class PingThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(PingThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private Session session;

    public PingThread(Session session) {
        this.session = session;
    }

    @Override
    public void run() {
        running.set(true);

        long pingInterval = ConfigUtils.getLong("ping.interval.ms");

        while (running.get()) {
            try {
                if (session.isOpen()) {
                    ByteBuffer pingPayload = generatePingPayload();
                    session.getRemote().sendPing(pingPayload);
                }

                Thread.sleep(pingInterval);
            } catch (InterruptedException ignored) {

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private ByteBuffer generatePingPayload() {
        long watchDogTimeout = ConfigUtils.getLong("watchdog.timeout.ms");

        PingMessage pingMessage = new PingMessage();
        pingMessage.setWatchDogTimeout(watchDogTimeout);

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
