package br.com.inngage.sdk;

/**
 * Created by viniciusdepaula on 07/09/16.
 */
public interface InngageOnEventListener<T> {

    public void onSuccess(T object);
    public void onFailure(Exception e);
}
