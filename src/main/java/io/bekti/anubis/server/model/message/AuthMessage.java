package io.bekti.anubis.server.model.message;

public class AuthMessage extends BaseMessage {

    private String token;
    private boolean success;
    private String message;

    public AuthMessage() {
        this.event = MessageEvent.AUTH;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
