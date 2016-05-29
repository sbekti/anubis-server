package io.bekti.anubis.server.kafka;

import io.bekti.anubis.server.types.InboundMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DispatcherThread extends Thread {

    private static Logger log = LoggerFactory.getLogger(DispatcherThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private BlockingQueue<InboundMessage> inboundQueue;
    private Session session;

    public DispatcherThread(BlockingQueue<InboundMessage> inboundQueue, Session session) {
        this.inboundQueue = inboundQueue;
        this.session = session;
    }

    @Override
    public void run() {
        running.set(true);

        while (running.get()) {
            InboundMessage inboundMessage;

            try {
                inboundMessage = inboundQueue.poll(100, TimeUnit.MILLISECONDS);

                if (inboundMessage == null) continue;

                if (session.isOpen()) {
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("topic", inboundMessage.getTopic());
                    jsonResponse.put("offset", inboundMessage.getOffset());
                    jsonResponse.put("key", inboundMessage.getKey());
                    jsonResponse.put("value", inboundMessage.getValue());

                    session.getRemote().sendString(jsonResponse.toString());
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
