package io.bekti.anubis.server.model.kafka;

public class KafkaPartition {

    private String topic;
    private int id;

    public KafkaPartition() {}

    public KafkaPartition(String topic, int id) {
        this.topic = topic;
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
