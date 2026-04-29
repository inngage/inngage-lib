package br.com.inngage.sdk.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.inngage.sdk.InAppConstants;
import br.com.inngage.sdk.InAppUtils;
import br.com.inngage.sdk.InngageConstants;

public class InngageMessagingService {
    static String inAppData = "additional_data";
    static String notData = "notId";
    public static void sendData(
            Context context,
            Intent intent,
            JSONObject jsonObject){
        try {
            if(jsonObject.has(inAppData)){
                String additionalDataString = jsonObject.getString(inAppData);
                IntentDataManager.putDataToIntent(
                        intent,
                        additionalDataString,
                        InAppConstants.keys);
                new InAppUtils().getIntentFromService(context, intent);
            }
            if(jsonObject.has(notData))
                IntentDataManager.putDataToIntent(
                        intent,
                        String.valueOf(jsonObject),
                        InngageConstants.keys);
        } catch(JSONException e){
            Log.e(InngageConstants.TAG_ERROR, "Error Inngage Data: " + e);
        }
    }
}
