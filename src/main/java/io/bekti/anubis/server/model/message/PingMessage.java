package io.bekti.anubis.server.model.message;

public class PingMessage extends BaseMessage {

    private long watchDogTimeout;

    public PingMessage() {
        this.event = MessageEvent.PING;
    }

    public long getWatchDogTimeout() {
        return watchDogTimeout;
    }

    public void setWatchDogTimeout(long watchDogTimeout) {
        this.watchDogTimeout = watchDogTimeout;
    }

}
