package io.bekti.anubis.server.model.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseMessage {

    private static final Logger log = LoggerFactory.getLogger(BaseMessage.class);

    protected MessageEvent event;

    public MessageEvent getEvent() {
        return event;
    }

    public void setEvent(MessageEvent event) {
        this.event = event;
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
