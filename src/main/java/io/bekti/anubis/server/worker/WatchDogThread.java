package io.bekti.anubis.server.worker;

import io.bekti.anubis.server.util.ConfigUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class WatchDogThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(WatchDogThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private Session session;
    private AtomicLong lastPongTimestamp = new AtomicLong();

    public WatchDogThread(Session session) {
        this.session = session;
    }

    @Override
    public void run() {
        running.set(true);

        long watchDogTimeout = ConfigUtils.getLong("watchdog.timeout.ms");

        while (running.get()) {
            try {
                Thread.sleep(watchDogTimeout);

                log.debug("WOOF {}?", session.getRemoteAddress().getHostString());

                long currentTimestamp = System.currentTimeMillis();

                if (currentTimestamp - lastPongTimestamp.get() > watchDogTimeout) {
                    log.debug("WOOF {}!", session.getRemoteAddress().getHostString());

                    try {
                        session.disconnect();
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } catch (InterruptedException ignored) {

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void setLastPongTimestamp(long timestamp) {
        lastPongTimestamp.set(timestamp);
    }

    public long getLastPongTimestamp() {
        return lastPongTimestamp.get();
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
