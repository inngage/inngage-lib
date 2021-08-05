package br.com.inngage.sdk;

/**
 * Maintained by Mohamed Ali Nakouri on 11/05/21.
 */
public interface InngageOnEventListener<T> {

    public void onSuccess(T object);
    public void onFailure(Exception e);
}
