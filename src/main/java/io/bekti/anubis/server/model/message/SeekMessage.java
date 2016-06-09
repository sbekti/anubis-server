package io.bekti.anubis.server.model.message;

public class SeekMessage extends BaseMessage {

    private String topic;
    private String offset;

    public SeekMessage() {
        this.event = MessageEvent.SEEK;
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
