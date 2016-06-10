package io.bekti.anubis.server.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserPatchRequest {

    private String name;

    @JsonIgnore
    private boolean nameSet;

    private String password;

    @JsonIgnore
    private boolean passwordSet;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        nameSet = true;
    }

    public boolean isNameSet() {
        return nameSet;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        passwordSet = true;
    }

    public boolean isPasswordSet() {
        return passwordSet;
    }

}
