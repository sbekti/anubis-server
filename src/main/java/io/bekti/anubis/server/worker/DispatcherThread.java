package io.bekti.anubis.server.worker;

import io.bekti.anubis.server.model.message.BaseMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DispatcherThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(DispatcherThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private BlockingQueue<BaseMessage> consumerQueue;
    private Session session;

    public DispatcherThread(BlockingQueue<BaseMessage> consumerQueue, Session session) {
        this.consumerQueue = consumerQueue;
        this.session = session;
    }

    @Override
    public void run() {
        running.set(true);

        while (running.get()) {
            BaseMessage baseMessage;

            try {
                baseMessage = consumerQueue.poll(100, TimeUnit.MILLISECONDS);

                if (baseMessage == null) continue;

                if (session.isOpen()) {
                    session.getRemote().sendString(baseMessage.toJson());
                }
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
        }
    }

}
