package io.bekti.anubis.server.messages;

import io.bekti.anubis.server.models.KafkaPartition;
import io.bekti.anubis.server.types.Event;

import java.util.List;

public class RevokeMessage extends BaseMessage {

    private List<KafkaPartition> partitions;

    public RevokeMessage() {
        this.event = Event.REVOKE;
    }

    public void setPartitions(List<KafkaPartition> partitions) {
        this.partitions = partitions;
    }

    public List<KafkaPartition> getPartitions() {
        return this.partitions;
    }

}
