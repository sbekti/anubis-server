package io.bekti.anubis.server.model.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MessageEvent {

    @JsonProperty("assign")
    ASSIGN,

    @JsonProperty("auth")
    AUTH,

    @JsonProperty("commit")
    COMMIT,

    @JsonProperty("message")
    MESSAGE,

    @JsonProperty("ping")
    PING,

    @JsonProperty("provision")
    PROVISION,

    @JsonProperty("publish")
    PUBLISH,

    @JsonProperty("revoke")
    REVOKE,

    @JsonProperty("seek")
    SEEK,

    @JsonProperty("subscribe")
    SUBSCRIBE

}
