package io.bekti.anubis.server.model.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bekti.anubis.server.model.dao.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientInfo {

    private static final Logger log = LoggerFactory.getLogger(ClientInfo.class);

    private String hostString;
    private Token token;

    public ClientInfo() {}

    public String getHostString() {
        return hostString;
    }

    public void setHostString(String hostString) {
        this.hostString = hostString;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = null;

        try {
            json = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return json;
    }

}
