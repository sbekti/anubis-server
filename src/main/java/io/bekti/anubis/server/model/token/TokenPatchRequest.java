package io.bekti.anubis.server.model.token;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TokenPatchRequest {

    private boolean revoked;

    @JsonIgnore
    private boolean revokedSet;

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
        revokedSet = true;
    }

    public boolean isRevokedSet() {
        return revokedSet;
    }

}
