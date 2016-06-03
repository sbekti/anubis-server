package io.bekti.anubis.server.messages;

import io.bekti.anubis.server.types.Event;
import com.google.gson.Gson;

public class BaseMessage {

    protected Event event;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

}
