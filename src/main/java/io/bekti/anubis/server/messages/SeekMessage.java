package io.bekti.anubis.server.messages;

import io.bekti.anubis.server.types.Event;

public class SeekMessage extends BaseMessage {

    private String topic;
    private String offset;

    public SeekMessage() {
        this.event = Event.SEEK;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

}
