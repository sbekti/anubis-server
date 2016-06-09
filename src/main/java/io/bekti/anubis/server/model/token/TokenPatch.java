package io.bekti.anubis.server.model.token;

public class TokenPatch {

    private boolean revoked;

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

}
