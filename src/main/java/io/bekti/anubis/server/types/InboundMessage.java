package io.bekti.anubis.server.types;

import org.apache.kafka.common.TopicPartition;

public class InboundMessage {

    private String topic;
    private long offset;
    private String key;
    private String value;
    private TopicPartition topicPartition;

    public InboundMessage(String topic, long offset, String key, String value) {
        this.topic = topic;
        this.offset = offset;
        this.key = key;
        this.value = value;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
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
