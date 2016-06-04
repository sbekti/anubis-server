package io.bekti.anubis.server.ws;

import io.bekti.anubis.server.messages.CommitMessage;
import io.bekti.anubis.server.messages.ProducerMessage;
import io.bekti.anubis.server.messages.SeekMessage;
import io.bekti.anubis.server.messages.SubscribeMessage;
import io.bekti.anubis.server.utils.SharedConfiguration;
import io.bekti.anubis.server.workers.MainWorkerThread;
import io.bekti.anubis.server.workers.WatchDogTimer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@WebSocket
public class AnubisWebSocketServletHandler {

    private Logger log = LoggerFactory.getLogger(AnubisWebSocketServletHandler.class);

    private ScheduledThreadPoolExecutor watchDogTimer = new ScheduledThreadPoolExecutor(1);
    private AtomicLong lastPongTimestamp = new AtomicLong();

    public AnubisWebSocketServletHandler() {}

    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("{} connected!", session.getRemoteAddress().getHostString());

        createWorker(session);
        createWatchDogTimer(session);
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        log.debug("Received message from {}: {}", session.getRemoteAddress().getHostString(), message);

        try {
            JsonObject payload = new Gson().fromJson(message, JsonObject.class);
            String event = payload.get("event").getAsString();

            switch (event) {
                case "subscribe":
                    SubscribeMessage subscribeMessage = new Gson().fromJson(message, SubscribeMessage.class);
                    subscribe(session, subscribeMessage);
                    break;
                case "publish":
                    ProducerMessage producerMessage = new Gson().fromJson(message, ProducerMessage.class);
                    publish(session, producerMessage);
                    break;
                case "commit":
                    CommitMessage commitMessage = new Gson().fromJson(message, CommitMessage.class);
                    commit(session, commitMessage);
                    break;
                case "seek":
                    SeekMessage seekMessage = new Gson().fromJson(message, SeekMessage.class);
                    seek(session, seekMessage);
                    break;
                case "unsubscribe":
                    unsubscribe(session);
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        log.info("{} closed: {} ({})", session.getRemoteAddress().getHostString(), reason, status);

        closeWorker(session);
        destroyWatchDogTimer();
    }

    @OnWebSocketFrame
    public void onFrame(Session session, Frame frame) {
        if (frame.getType() == Frame.Type.PONG) {
            log.debug("Got PONG from {}", session.getRemoteAddress().getHostString());
            lastPongTimestamp.set(System.currentTimeMillis());
        }
    }

    private void createWorker(Session session) {
        closeWorker(session);

        MainWorkerThread worker = new MainWorkerThread(session);
        worker.start();

        AnubisWebSocketServer.getWorkers().put(session, worker);
    }

    private void subscribe(Session session, SubscribeMessage subscribeMessage) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            worker.subscribe(subscribeMessage.getTopics(), subscribeMessage.getGroupId());
        }
    }

    private void unsubscribe(Session session) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            worker.unsubscribe();
        }
    }

    private void publish(Session session, ProducerMessage producerMessage) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            worker.publish(producerMessage);
        }
    }

    private void commit(Session session, CommitMessage commitMessage) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            worker.commit(commitMessage);
        }
    }

    private void seek(Session session, SeekMessage seekMessage) {
        MainWorkerThread worker = AnubisWebSocketServer.getWorkers().get(session);

        if (worker != null) {
            worker.seek(seekMessage);
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

    private void createWatchDogTimer(Session session) {
        long watchDogTimeout = SharedConfiguration.getLong("watchdog.timeout.ms");

        lastPongTimestamp.set(System.currentTimeMillis());

        watchDogTimer.scheduleAtFixedRate(
                new WatchDogTimer(session, lastPongTimestamp, watchDogTimeout),
                watchDogTimeout,
                watchDogTimeout,
                TimeUnit.MILLISECONDS
        );
    }

    private void destroyWatchDogTimer() {
        watchDogTimer.shutdown();
    }

}