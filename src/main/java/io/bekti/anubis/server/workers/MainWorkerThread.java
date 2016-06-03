package io.bekti.anubis.server.workers;

import io.bekti.anubis.server.messages.*;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWorkerThread extends Thread {

    private static Logger log = LoggerFactory.getLogger(MainWorkerThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private Session session;
    private DispatcherThread dispatcherThread;
    private ProducerThread producerThread;
    private ConsumerThread consumerThread;
    private PingThread pingThread;

    private BlockingQueue<BaseMessage> consumerQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<ProducerMessage> producerQueue = new LinkedBlockingQueue<>();

    public MainWorkerThread(Session session) {
        this.session = session;
    }

    @Override
    public void run() {
        log.info("Starting threads...");
        running.set(true);

        dispatcherThread = new DispatcherThread(consumerQueue, session);
        dispatcherThread.start();

        producerThread = new ProducerThread(producerQueue);
        producerThread.start();

        pingThread = new PingThread(session, this);
        pingThread.start();

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

                if (pingThread.isRunning()) {
                    pingThread.shutdown();
                    pingThread.join();
                }
            } catch (InterruptedException ignored) {

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void publish(ProducerMessage producerMessage) {
        try {
            producerQueue.put(producerMessage);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void subscribe(List<String> topics, String groupId) {
        try {
            if (consumerThread != null && consumerThread.isRunning()) {
                consumerThread.shutdown();
                consumerThread.join();
            }

            consumerThread = new ConsumerThread(topics, groupId, consumerQueue);
            consumerThread.start();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void unsubscribe() {
        try {
            if (consumerThread != null && consumerThread.isRunning()) {
                consumerThread.shutdown();
                consumerThread.join();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void commit(CommitMessage commitMessage) {
        if (consumerThread != null && consumerThread.isRunning()) {
            consumerThread.commit(commitMessage);
        }
    }

    public void seek(SeekMessage seekMessage) {
        if (consumerThread != null && consumerThread.isRunning()) {
            consumerThread.seek(seekMessage);
        }
    }

}
