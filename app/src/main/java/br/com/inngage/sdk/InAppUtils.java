package br.com.inngage.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;

public class InAppUtils {
    private String TAG = "in-app";
    private Intent intentMain;
    AppPreferences appPreferences;

    public void getIntentFromService(Context context, Intent intent) {
        if (appPreferences == null) {
            appPreferences = new AppPreferences(context);
        }

        if (intentMain == null) {
            intentMain = new Intent(context, InApp.class);
        }

        ArrayList<String> list = dataInApp(intent);

        if (!list.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("keyInApp", list);
            intentMain.putExtras(bundle);

            for (int i = 0; i < list.size(); i++) {
                String valor = list.get(i);
                if ("".equals(valor)) {
                    list.set(i, null);
                }
            }

            String listString = TextUtils.join(", ", list);

            if (listString != null && !listString.isEmpty()) {
                appPreferences.putString("key-intent", listString);
            }

            startInApp(context);
        }
    }

    public ArrayList<String> dataInApp(Intent intent){
        String[] values = new String[InngageConstants.keys.length];
        ArrayList<String> arrayValues = new ArrayList<>();

        if (intent.hasExtra("additional_data")){
            try{
                String additionalDataString = intent.getStringExtra("additional_data");
                ExtraData.putDataToIntent(additionalDataString, intent);
            } catch(JSONException e){
                e.printStackTrace();
            }
            ExtraData.getDataToIntent(intent, values);
        } else {
            ExtraData.getDataToIntent(intent, values);
        }

        for(int i = 0; i < values.length; i++){
            arrayValues.add(values[i]);
        }
        return arrayValues;
    }

    public void startInApp(Context context) {
        if (appPreferences == null) {
            appPreferences = new AppPreferences(context);
        }

        if (appPreferences != null) {
            String stringList = appPreferences.getString("key-intent", null);

            if (intentMain != null) {
                startActivityInApp(context);
            } else if (stringList != null) {
                ArrayList<String> list = new ArrayList<>(Arrays.asList(stringList.split(", ")));

                for (int i = 0; i < list.size(); i++) {
                    String valor = list.get(i);
                    if ("null".equals(valor)) {
                        list.set(i, null);
                    }
                }

                intentMain = new Intent(context, InApp.class);
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("keyInApp", list);
                intentMain.putExtras(bundle);
                startActivityInApp(context);
            } else {
                Log.e(TAG, "Error");
            }
        }
    }

    private void startActivityInApp(Context context){
        intentMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intentMain);
    }
}