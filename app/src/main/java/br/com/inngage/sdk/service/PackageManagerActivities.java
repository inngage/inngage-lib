package br.com.inngage.sdk.service;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import br.com.inngage.sdk.InngageConstants;

public class PackageManagerActivities {
    public static void getAppActivities(
            Intent intent,
            PackageManager packageManager,
            JSONObject jsonObject){
        String activityPackage = "";
        try{
            ArrayList<String> activities = PackageManagerActivities.activitiesName(
                    packageManager,
                    (String) jsonObject.get("act_pkg"));
            for (String activityName : activities) {
                if (activityName.equals(activityPackage + ".MainActivity")) {
                    intent.setClassName(activityPackage, activityName);
                }
            }
        } catch (JSONException e){
            Log.e(InngageConstants.TAG_ERROR, "Json exception: " + e);
        }
    }
    public static ArrayList<String> activitiesName(
            PackageManager packageManager,
            String packageName){
        ArrayList<String> activitiesNames = new ArrayList<>();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_ACTIVITIES);
            ActivityInfo[] activities = packageInfo.activities;
            if (activities != null) {
                for (ActivityInfo activityInfo : activities) {
                    String activityName = activityInfo.name;
                    activitiesNames.add(activityName);
                }
            }
        } catch (PackageManager.NameNotFoundException e){
            Log.e(InngageConstants.TAG_ERROR, "Package exception: " + e);
        }
        return activitiesNames;
    }
}
