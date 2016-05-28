package io.bekti.anubis.server.http;

import io.bekti.anubis.server.kafka.KafkaWebSocketClient;
import io.bekti.anubis.server.utils.SharedConfiguration;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebSocketServer extends Thread {

    private static Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    private Server server;
    private AtomicBoolean running = new AtomicBoolean(false);
    private static Map<Session, KafkaWebSocketClient> kafkaClients;

    public WebSocketServer() {
        kafkaClients = new HashMap<>();
    }

    @Override
    public void run() {
        int httpPort = SharedConfiguration.getInteger("ws.server.port");
        int httpsPort = SharedConfiguration.getInteger("wss.server.port");

        log.info("Starting server on port {} (HTTP) and {} (HTTPS)", httpPort, httpsPort);
        running.set(true);

        try {
            server = new Server();
            mountServlets(server);
            setupConnectors(server, httpPort, httpsPort);
            server.start();

            log.info("Successfully started server.");
        } catch (Exception e) {
            log.error("Error starting server.");
            log.error(e.getMessage(), e);

            running.set(false);
        }
    }

    public boolean isRunning() {
        return this.running.get();
    }

    public void shutdown() {
        log.info("Shutting down...");

        if (this.running.get() && this.server != null && !this.server.isStopped()) {
            try {
                this.server.stop();
                log.info("Successfully stopped server.");
            } catch (Exception e) {
                log.error("Error while trying to shutdown server.");
                log.error(e.getMessage(), e);
            }
        }
    }

    private void mountServlets(Server server) {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        ServletHolder loginServletHolder = new ServletHolder("ws", KafkaWebSocketServlet.class);
        context.addServlet(loginServletHolder, "/");

        server.setHandler(context);
    }

    private void setupConnectors(Server server, int httpPort, int httpsPort) {
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(httpPort);

        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(SharedConfiguration.getString("ssl.keystore.path"));
        sslContextFactory.setKeyStorePassword(SharedConfiguration.getString("ssl.keystore.password"));
        sslContextFactory.setKeyManagerPassword(SharedConfiguration.getString("ssl.keymanager.password"));

        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(httpsPort);

        server.setConnectors(new Connector[] { connector, sslConnector });
    }

    public static Map<Session, KafkaWebSocketClient> getKafkaClients() {
        return kafkaClients;
    }

}
