package br.com.inngage.sdk.service;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.inngage.sdk.InAppConstants;
import br.com.inngage.sdk.InngageConstants;

public class IntentDataManager {
    public static void getDataFromIntent(Intent intent, String[] values){
        if(intent.hasExtra("inapp_message"))
            for (int i = 0; i < InAppConstants.keys.length; i++) {
                if (intent.hasExtra(InAppConstants.keys[i])){
                    values[i] = intent.getStringExtra(InAppConstants.keys[i]);
                }
            }
        if(!intent.hasExtra("inapp_message"))
            for (int i = 0; i < InngageConstants.keys.length; i++) {
                if (intent.hasExtra(InngageConstants.keys[i])){
                    values[i] = intent.getStringExtra(InngageConstants.keys[i]);
                }
            }
    }

    public static void putDataToIntent(Intent intent, String data, String[] keys){
        try{
            JSONObject jsonData = new JSONObject(data);
            for (String key : keys) {
                if (jsonData.has(key)) {
                    if (!jsonData.isNull(key)){
                        if (jsonData.optInt(key, Integer.MIN_VALUE) != Integer.MIN_VALUE) {
                            int intValue = jsonData.getInt(key);
                            intent.putExtra(key, intValue);
                        } else if (jsonData.optBoolean(key, false)) {
                            boolean boolValue = jsonData.getBoolean(key);
                            intent.putExtra(key, boolValue);
                        } else {
                            String stringValue = jsonData.getString(key);
                            intent.putExtra(key, stringValue);
                        }
                    }
                }
            }
        } catch (JSONException e){
            Log.e(InngageConstants.TAG_ERROR, "" + e);
        }
    }
}
