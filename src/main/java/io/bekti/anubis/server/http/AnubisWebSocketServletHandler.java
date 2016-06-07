package io.bekti.anubis.server.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bekti.anubis.server.model.dao.Token;
import io.bekti.anubis.server.model.dao.TokenDao;
import io.bekti.anubis.server.model.message.*;
import io.bekti.anubis.server.util.SharedConfiguration;
import io.bekti.anubis.server.worker.ClientThread;
import io.bekti.anubis.server.worker.PingThread;
import io.bekti.anubis.server.worker.WatchDogThread;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class AnubisWebSocketServletHandler {

    private static final Logger log = LoggerFactory.getLogger(AnubisWebSocketServletHandler.class);

    private PingThread pingThread;
    private WatchDogThread watchDogThread;

    public AnubisWebSocketServletHandler() {}

    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("{} connected!", session.getRemoteAddress().getHostString());

        createPingThread(session);
        createWatchDogThread(session);

        boolean authenticateClients = SharedConfiguration.getBoolean("authenticate.clients");

        if (!authenticateClients) {
            createClient(session, new Token());
        }
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        log.debug("Received message from {}: {}", session.getRemoteAddress().getHostString(), message);

        try {
            JsonNode payload = new ObjectMapper().readValue(message, JsonNode.class);
            String event = payload.get("event").asText();

            switch (event) {
                case "auth":
                    AuthMessage authMessage = new ObjectMapper().readValue(message, AuthMessage.class);
                    authenticate(session, authMessage);
                    break;
                case "commit":
                    CommitMessage commitMessage = new ObjectMapper().readValue(message, CommitMessage.class);
                    commit(session, commitMessage);
                    break;
                case "publish":
                    ProducerMessage producerMessage = new ObjectMapper().readValue(message, ProducerMessage.class);
                    publish(session, producerMessage);
                    break;
                case "seek":
                    SeekMessage seekMessage = new ObjectMapper().readValue(message, SeekMessage.class);
                    seek(session, seekMessage);
                    break;
                case "subscribe":
                    SubscribeMessage subscribeMessage = new ObjectMapper().readValue(message, SubscribeMessage.class);
                    subscribe(session, subscribeMessage);
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

        destroyClient(session);
        destroyWatchDogThread();
        destroyPingThread();
    }

    @OnWebSocketFrame
    public void onFrame(Session session, Frame frame) {
        if (frame.getType() == Frame.Type.PONG) {
            log.debug("Got PONG from {}", session.getRemoteAddress().getHostString());

            updateLastPongTimestamp();
        }
    }

    private void createClient(Session session, Token token) {
        ClientThread clientThread = AnubisHttpServer.getClients().get(session);

        if (clientThread != null) return;

        clientThread = new ClientThread(session, token);
        clientThread.start();

        AnubisHttpServer.getClients().put(session, clientThread);
    }

    private void destroyClient(Session session) {
        ClientThread clientThread = AnubisHttpServer.getClients().get(session);

        if (clientThread == null) return;

        clientThread.shutdown();

        try {
            clientThread.join();
            AnubisHttpServer.getClients().remove(session);
        } catch (InterruptedException ignored) {

        }
    }

    private void createPingThread(Session session) {
        destroyPingThread();

        pingThread = new PingThread(session);
        pingThread.start();
    }

    private void destroyPingThread() {
        if (pingThread == null) return;
        if (!pingThread.isRunning()) return;

        pingThread.shutdown();

        try {
            pingThread.join();
            pingThread = null;
        } catch (InterruptedException ignored) {

        }
    }

    private void createWatchDogThread(Session session) {
        destroyWatchDogThread();

        watchDogThread = new WatchDogThread(session);
        watchDogThread.start();
    }

    private void destroyWatchDogThread() {
        if (watchDogThread == null) return;
        if (!watchDogThread.isRunning()) return;

        watchDogThread.shutdown();

        try {
            watchDogThread.join();
            watchDogThread = null;
        } catch (InterruptedException ignored) {

        }
    }

    private void updateLastPongTimestamp() {
        if (watchDogThread == null) return;
        if (!watchDogThread.isRunning()) return;

        watchDogThread.setLastPongTimestamp(System.currentTimeMillis());
    }

    private void authenticate(Session session, AuthMessage authMessage) {
        boolean authenticateClients = SharedConfiguration.getBoolean("authenticate.clients");

        if (!authenticateClients) {
            authMessage.setSuccess(true);
            authMessage.setMessage("Success");
            dispatch(session, authMessage);
            return;
        }

        String jwt = authMessage.getToken();
        String secret = SharedConfiguration.getString("access.token.secret");
        String audience = SharedConfiguration.getString("access.token.audience");

        try {
            Jws<Claims> jws = Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(jwt);
            Claims claims = jws.getBody();

            if (!claims.getAudience().equals(audience)) return;

            String uuid = claims.getId();
            Token token = TokenDao.getByUUID(uuid);

            if (token == null) {
                authMessage.setSuccess(false);
                authMessage.setMessage("Invalid token");
                dispatch(session, authMessage);
                return;
            }

            if (token.isRevoked()) {
                authMessage.setSuccess(false);
                authMessage.setMessage("Token is revoked");
                dispatch(session, authMessage);
                return;
            }

            createClient(session, token);

            authMessage.setSuccess(true);
            authMessage.setMessage("Success");
            dispatch(session, authMessage);
        } catch (Exception e) {
            authMessage.setSuccess(false);
            authMessage.setMessage(e.getMessage());
            dispatch(session, authMessage);

            log.error(e.getMessage(), e);
        }
    }

    private void dispatch(Session session, BaseMessage baseMessage) {
        if (session.isOpen()) {
            try {
                session.getRemote().sendString(baseMessage.toJson());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void subscribe(Session session, SubscribeMessage subscribeMessage) {
        ClientThread clientThread = AnubisHttpServer.getClients().get(session);

        if (clientThread == null) return;

        clientThread.subscribe(subscribeMessage.getTopics(), subscribeMessage.getGroupId());
    }

    private void unsubscribe(Session session) {
        ClientThread clientThread = AnubisHttpServer.getClients().get(session);

        if (clientThread == null) return;

        clientThread.unsubscribe();
    }

    private void publish(Session session, ProducerMessage producerMessage) {
        ClientThread clientThread = AnubisHttpServer.getClients().get(session);

        if (clientThread == null) return;

        clientThread.publish(producerMessage);
    }

    private void commit(Session session, CommitMessage commitMessage) {
        ClientThread clientThread = AnubisHttpServer.getClients().get(session);

        if (clientThread == null) return;

        clientThread.commit(commitMessage);
    }

    private void seek(Session session, SeekMessage seekMessage) {
        ClientThread clientThread = AnubisHttpServer.getClients().get(session);

        if (clientThread == null) return;

        clientThread.seek(seekMessage);
    }

}