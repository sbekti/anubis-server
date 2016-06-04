package io.bekti.anubis.server.messages;

import io.bekti.anubis.server.models.KafkaPartition;
import io.bekti.anubis.server.types.Event;

import java.util.List;

public class AssignMessage extends BaseMessage {

    private List<KafkaPartition> partitions;

    public AssignMessage() {
        this.event = Event.ASSIGN;
    }

    public void setPartitions(List<KafkaPartition> partitions) {
        this.partitions = partitions;
    }

    public List<KafkaPartition> getPartitions() {
        return this.partitions;
    }

}
