package br.com.inngage.sdk;

import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

public class ExtraData {
    static void getDataToIntent(Intent intent, String[] values){
        if (intent.hasExtra("inapp_message")){
            for (int i = 0; i < InngageConstants.keys.length; i++) {
                if (intent.hasExtra(InngageConstants.keys[i])){
                    values[i] = intent.getStringExtra(InngageConstants.keys[i]);
                }
            }
        }
    }

    static void putDataToIntent(String data, Intent intent) throws JSONException {
        JSONObject additionalData = new JSONObject(data);
        for (String key : InngageConstants.keys) {
            if (additionalData.has(key)) {
                intent.putExtra(key, additionalData.getString(key));
            }
        }
    }
}
