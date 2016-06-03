package io.bekti.anubis.server.messages;

import io.bekti.anubis.server.types.Event;
import java.util.List;

public class SubscribeMessage extends BaseMessage {

    private List<String> topics;
    private String groupId;

    public SubscribeMessage() {
        this.event = Event.SUBSCRIBE;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

}
