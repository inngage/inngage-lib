package br.com.inngage.sdk;

/**
 * Created by viniciusdepaula on 07/09/16.
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