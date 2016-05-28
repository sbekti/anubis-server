package io.bekti.anubis.server;

import io.bekti.anubis.server.http.WebSocketServer;
import io.bekti.anubis.server.utils.SharedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnubisServerMain {

    private static Logger log = LoggerFactory.getLogger(AnubisServerMain.class);

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();
        SharedConfiguration.loadFromFile(System.getProperty("config"));

        WebSocketServer webSocketServer = new WebSocketServer();
        webSocketServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Shutting down...");

            try {
                webSocketServer.shutdown();
                webSocketServer.join();
                mainThread.join();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }));
    }

}
