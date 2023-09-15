package br.com.inngage.sdk;

public interface HttpResponseCallback {
    void onResponse(String response);
    void onError(String errorMessage);
}
