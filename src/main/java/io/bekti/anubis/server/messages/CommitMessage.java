package io.bekti.anubis.server.messages;

import io.bekti.anubis.server.types.Event;

public class CommitMessage extends BaseMessage {

    private String topic;
    private int partition;
    private long offset;

    public CommitMessage() {
        this.event = Event.COMMIT;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

}
