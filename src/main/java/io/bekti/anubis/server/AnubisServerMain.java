package io.bekti.anubis.server;

import io.bekti.anubis.server.ws.AnubisWebSocketServer;
import io.bekti.anubis.server.utils.SharedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnubisServerMain {

    private static Logger log = LoggerFactory.getLogger(AnubisServerMain.class);

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();
        SharedConfiguration.loadFromFile(System.getProperty("config"));

        AnubisWebSocketServer server = new AnubisWebSocketServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Shutting down...");

            try {
                server.shutdown();
                server.join();
                mainThread.join();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }));
    }

}
