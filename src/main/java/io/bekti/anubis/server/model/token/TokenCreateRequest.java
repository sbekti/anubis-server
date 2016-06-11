package io.bekti.anubis.server.model.token;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;

public class TokenCreateRequest {

    @NotEmpty
    private String name;

    @Min(0)
    private long expiry;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

}
