package io.bekti.anubis.server.http;

import io.bekti.anubis.server.model.exceptionmapper.NotFoundExceptionMapper;
import io.bekti.anubis.server.model.exceptionmapper.RuntimeExceptionMapper;
import io.bekti.anubis.server.model.exceptionmapper.WebApplicationExceptionMapper;
import io.bekti.anubis.server.worker.ClientThread;
import io.bekti.anubis.server.util.ConfigUtils;
import io.bekti.anubis.server.http.rest.AuthFilter;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnubisHttpServer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(AnubisHttpServer.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private Server server;
    private static Map<Session, ClientThread> clients = new HashMap<>();

    public AnubisHttpServer() {}

    @Override
    public void run() {
        int httpPort = ConfigUtils.getInteger("ws.server.port");
        int httpsPort = ConfigUtils.getInteger("wss.server.port");

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

        if (running.get() && server != null && !server.isStopped()) {
            try {
                for (Map.Entry<Session, ClientThread> pair : clients.entrySet()) {
                    ClientThread workerThread = pair.getValue();
                    workerThread.shutdown();
                    workerThread.join();
                }

                server.stop();
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

        ServletHolder webSocketServlet = new ServletHolder(new AnubisWebSocketServlet());
        context.addServlet(webSocketServlet, "/");

        ResourceConfig resourceConfig = new ResourceConfig()
                .packages("io.bekti.anubis.server.http.rest")
                .register(JacksonFeature.class)
                .register(AuthFilter.class)
                .register(WebApplicationExceptionMapper.class)
                .register(RuntimeExceptionMapper.class)
                .register(NotFoundExceptionMapper.class);

        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(resourceConfig));
        jerseyServlet.setInitOrder(0);
        context.addServlet(jerseyServlet, "/api/v1/*");

        server.setHandler(context);
    }

    private void setupConnectors(Server server, int httpPort, int httpsPort) {
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(httpPort);

        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(ConfigUtils.getString("ssl.keystore.path"));
        sslContextFactory.setKeyStorePassword(ConfigUtils.getString("ssl.keystore.password"));
        sslContextFactory.setKeyManagerPassword(ConfigUtils.getString("ssl.keymanager.password"));

        ServerConnector sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(https));
        sslConnector.setPort(httpsPort);

        server.setConnectors(new Connector[] { connector, sslConnector });
    }

    public static Map<Session, ClientThread> getClients() {
        return clients;
    }

}
