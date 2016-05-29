package io.bekti.anubis.server.types;

public class SeekRequest {

    private String topic;
    private String offset;

    public SeekRequest(String topic, String offset) {
        this.topic = topic;
        this.offset = offset;
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
