package io.bekti.anubis.server.worker;

import io.bekti.anubis.server.model.client.ClientInfo;
import io.bekti.anubis.server.model.dao.Token;
import io.bekti.anubis.server.model.message.*;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(ClientThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private Session session;
    private ClientInfo clientInfo;

    private DispatcherThread dispatcherThread;
    private ProducerThread producerThread;
    private ConsumerThread consumerThread;

    private BlockingQueue<BaseMessage> consumerQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<ProducerMessage> producerQueue = new LinkedBlockingQueue<>();

    public ClientThread(Session session, Token token) {
        this.session = session;

        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setHostString(session.getRemoteAddress().getHostString());
        clientInfo.setToken(token);

        this.clientInfo = clientInfo;
    }

    @Override
    public void run() {
        log.info("Starting threads...");
        running.set(true);

        dispatcherThread = new DispatcherThread(consumerQueue, session);
        dispatcherThread.start();

        producerThread = new ProducerThread(producerQueue);
        producerThread.start();

        log.debug("Entering main client loop...");
    }

    public boolean isRunning() {
        return running.get();
    }

    public void shutdown() {
        if (running.get()) {
            running.set(false);

            try {
                if (dispatcherThread.isRunning()) {
                    dispatcherThread.shutdown();
                    dispatcherThread.join();
                }

                if (producerThread.isRunning()) {
                    producerThread.shutdown();
                    producerThread.join();
                }

                if (consumerThread != null && consumerThread.isRunning()) {
                    consumerThread.shutdown();
                    consumerThread.join();
                }
            } catch (InterruptedException ignored) {

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void publish(ProducerMessage producerMessage) {
        producerQueue.add(producerMessage);
    }

    public void subscribe(List<String> topics, String groupId) {
        try {
            unsubscribe();

            consumerThread = new ConsumerThread(topics, groupId, consumerQueue);
            consumerThread.start();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void unsubscribe() {
        try {
            if (consumerThread == null) return;
            if (!consumerThread.isRunning()) return;

            consumerThread.shutdown();
            consumerThread.join();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void commit(CommitMessage commitMessage) {
        if (consumerThread == null) return;
        if (!consumerThread.isRunning()) return;

        consumerThread.commit(commitMessage);
    }

    public void seek(SeekMessage seekMessage) {
        if (consumerThread == null) return;
        if (!consumerThread.isRunning()) return;

        consumerThread.seek(seekMessage);
    }

}
