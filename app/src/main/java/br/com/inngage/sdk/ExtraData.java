package br.com.inngage.sdk;

import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

public class ExtraData {
    static void getDataToIntent(Intent intent, String[] values){
        for (int i = 0; i < InngageConstants.keys.length; i++) {
            if (intent.hasExtra(InngageConstants.keys[i])){
                values[i] = intent.getStringExtra(InngageConstants.keys[i]);
            }
        }
    }

    static void putDataToIntent(String data, Intent intent) throws JSONException {
        JSONObject additionalData = new JSONObject(data);
        for (String key : InngageConstants.keys) {
            if (additionalData.has(key)) {
                if (!additionalData.isNull(key)){
                    if (additionalData.optInt(key, Integer.MIN_VALUE) != Integer.MIN_VALUE) {
                        int intValue = additionalData.getInt(key);
                        intent.putExtra(key, intValue);
                    } else if (additionalData.optBoolean(key, false)) {
                        boolean boolValue = additionalData.getBoolean(key);
                        intent.putExtra(key, boolValue);
                    } else {
                        String stringValue = additionalData.getString(key);
                        intent.putExtra(key, stringValue);
                    }
                }
            }
        }
    }
}
