package io.bekti.anubis.server.model.token;

public class TokenRequest {

    private long expiry;

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

}
