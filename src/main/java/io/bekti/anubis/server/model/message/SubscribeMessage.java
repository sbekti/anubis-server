package io.bekti.anubis.server.model.message;

import java.util.List;

public class SubscribeMessage extends BaseMessage {

    private List<String> topics;
    private String groupId;

    public SubscribeMessage() {
        this.event = MessageEvent.SUBSCRIBE;
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
