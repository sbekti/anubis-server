package io.bekti.anubis.server.messages;

import io.bekti.anubis.server.types.Event;

public class PingMessage extends BaseMessage {

    private long watchDogTimeout;

    public PingMessage() {
        this.event = Event.PING;
    }

    public long getWatchDogTimeout() {
        return watchDogTimeout;
    }

    public void setWatchDogTimeout(long watchDogTimeout) {
        this.watchDogTimeout = watchDogTimeout;
    }

}
