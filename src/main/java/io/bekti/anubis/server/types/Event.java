package io.bekti.anubis.server.types;

import com.google.gson.annotations.SerializedName;

public enum Event {

    @SerializedName("commit")
    COMMIT,

    @SerializedName("message")
    MESSAGE,

    @SerializedName("ping")
    PING,

    @SerializedName("publish")
    PUBLISH,

    @SerializedName("rebalance")
    REBALANCE,

    @SerializedName("seek")
    SEEK,

    @SerializedName("subscribe")
    SUBSCRIBE,

}
