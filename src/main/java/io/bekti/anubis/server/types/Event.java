package io.bekti.anubis.server.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Event {

    @JsonProperty("assign")
    ASSIGN,

    @JsonProperty("commit")
    COMMIT,

    @JsonProperty("message")
    MESSAGE,

    @JsonProperty("ping")
    PING,

    @JsonProperty("publish")
    PUBLISH,

    @JsonProperty("revoke")
    REVOKE,

    @JsonProperty("seek")
    SEEK,

    @JsonProperty("subscribe")
    SUBSCRIBE

}
