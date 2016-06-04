package io.bekti.anubis.server.types;

import com.google.gson.annotations.SerializedName;

public enum Event {

    @SerializedName("assign")
    ASSIGN,

    @SerializedName("commit")
    COMMIT,

    @SerializedName("message")
    MESSAGE,

    @SerializedName("ping")
    PING,

    @SerializedName("publish")
    PUBLISH,

    @SerializedName("revoke")
    REVOKE,

    @SerializedName("seek")
    SEEK,

    @SerializedName("subscribe")
    SUBSCRIBE

}
