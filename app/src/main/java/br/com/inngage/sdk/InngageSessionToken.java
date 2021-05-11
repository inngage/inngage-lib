package br.com.inngage.sdk;

/**
 * Maintained by Mohamed Ali Nakouri on 11/05/21.
 */
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