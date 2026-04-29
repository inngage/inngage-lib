package br.com.inngage.sdk;

public class InngageSessionToken {
    public InngageSessionToken(String token) {
        this.token = token;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    private String token;
}