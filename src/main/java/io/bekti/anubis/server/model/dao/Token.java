package io.bekti.anubis.server.model.dao;

import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "access_tokens", indexes = { @Index(name = "tokenUUIDIndex", columnList = "uuid", unique = true) })
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private int id;

    @NotBlank
    @Column(name = "uuid", length = 64, unique = true)
    private String uuid;

    @NotBlank
    @Column(name = "token", length = 512)
    private String token;

    @NotNull
    @Column(name = "revoked")
    private boolean revoked;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

}