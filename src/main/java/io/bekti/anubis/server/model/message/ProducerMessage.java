package io.bekti.anubis.server.model.message;

public class ProducerMessage extends BaseMessage {

    private String topic;
    private String key;
    private String value;

    public ProducerMessage() {
        this.event = MessageEvent.PUBLISH;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
