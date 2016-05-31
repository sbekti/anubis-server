package io.bekti.anubis.server.workers;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class WatchDogTimer implements Runnable {

    private Logger log = LoggerFactory.getLogger(WatchDogTimer.class);
    private Session session;
    private AtomicLong lastPongTimestamp;
    private long pingTimeout;

    public WatchDogTimer(Session session, AtomicLong lastPongTimestamp, long pingTimeout) {
        this.session = session;
        this.lastPongTimestamp = lastPongTimestamp;
        this.pingTimeout = pingTimeout;
    }

    @Override
    public void run() {
        log.info("WOOF?");

        long currentTimestamp = System.currentTimeMillis();

        if (currentTimestamp - lastPongTimestamp.get() > pingTimeout) {
            log.info("WOOF!");
            session.close();
        }
    }

}