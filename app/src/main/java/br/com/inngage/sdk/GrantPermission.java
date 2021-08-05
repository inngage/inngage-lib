package br.com.inngage.sdk;

public abstract class GrantPermission {
    protected abstract void call(int requestCode, String permissions[], int[] grantResults);
}