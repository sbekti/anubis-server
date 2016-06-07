package io.bekti.anubis.server;

import io.bekti.anubis.server.http.AnubisHttpServer;
import io.bekti.anubis.server.util.DatabaseUtils;
import io.bekti.anubis.server.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnubisServerMain {

    private static final Logger log = LoggerFactory.getLogger(AnubisServerMain.class);

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();
        ConfigUtils.loadFromClassPath();

        DatabaseUtils.initAdminUser();
        DatabaseUtils.startH2Console();

        AnubisHttpServer server = new AnubisHttpServer();
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
