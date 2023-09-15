package br.com.inngage.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;
import java.util.Set;

public class AppPreferences {
    private SharedPreferences mPreferences;

    public AppPreferences(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    public AppPreferences(Context context, String name, int mode) {
        mPreferences = context.getSharedPreferences(name, mode);
    }
    public void setDefaultValues(Context context, int resId, boolean readAgain) {
        PreferenceManager.setDefaultValues(context, resId, readAgain);
    }
    public void putString(String key, String value) {
        try {
            SharedPreferences.Editor prefEdit = mPreferences.edit();
            prefEdit.putString(key, value);
            prefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String getString(String key, String defValue) throws ClassCastException {
        return mPreferences.getString(key, defValue);
    }
    public void putInt(String key, int value) {
        try {
            SharedPreferences.Editor prefEdit = mPreferences.edit();
            prefEdit.putInt(key, value);
            prefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public int getInt(String key, int defValue) throws ClassCastException {
        return mPreferences.getInt(key, defValue);
    }
    public void putLong(String key, long value) {
        try {
            SharedPreferences.Editor prefEdit = mPreferences.edit();
            prefEdit.putLong(key, value);
            prefEdit.apply();
        } catch (Exception e) {

            e.printStackTrace();

        }
    }
    public long getLong(String key, long defValue) throws ClassCastException {
        return mPreferences.getLong(key, defValue);
    }
    public void putBoolean(String key, Boolean value) {
        try {
            SharedPreferences.Editor prefEdit = mPreferences.edit();
            prefEdit.putBoolean(key, value);
            prefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean getBoolean(String key, boolean defValue) throws ClassCastException {
        return mPreferences.getBoolean(key, defValue);
    }
    public void putFloat(String key, float value) {
        try {
            SharedPreferences.Editor prefEdit = mPreferences.edit();
            prefEdit.putFloat(key, value);
            prefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public float getFloat(String key, float defValue) throws ClassCastException {
        return mPreferences.getFloat(key, defValue);
    }
    public Map<String, ?> getAll() throws NullPointerException {
        return mPreferences.getAll();
    }
    public Boolean contains(String key) {
        return mPreferences.contains(key);
    }
    public void clearPreferences() {
        try {
            SharedPreferences.Editor prefEdit = mPreferences.edit();
            prefEdit.clear();
            prefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void removeKey(String key) {
        try {
            SharedPreferences.Editor prefEdit = mPreferences.edit();
            prefEdit.remove(key);
            prefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void putStringSet(String key, Set<String> set) {
        try {
            SharedPreferences.Editor prefEdit = mPreferences.edit();
            prefEdit.putStringSet(key, set);
            prefEdit.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Set<String> getStringSet(String key, Set<String> set) {
        return mPreferences.getStringSet(key, set);
    }
}
