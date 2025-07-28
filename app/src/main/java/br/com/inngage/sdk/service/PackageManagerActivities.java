package br.com.inngage.sdk.service;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.json.JSONException;

import java.util.ArrayList;

public class PackageManagerActivities {

    public static void getAppActivities(Intent intent, PackageManager packageManager, String packageName) {
        try {
            ArrayList<String> activities = getActivityNames(packageManager, packageName);
            for (String activityName : activities) {
                if (activityName.endsWith(".MainActivity")) {
                    intent.setClassName(packageName, activityName);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e("PackageManager", "Erro ao configurar intent para MainActivity", e);
        }
    }

    private static ArrayList<String> getActivityNames(PackageManager packageManager, String packageName) {
        ArrayList<String> activityNames = new ArrayList<>();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            if (packageInfo.activities != null) {
                for (ActivityInfo info : packageInfo.activities) {
                    activityNames.add(info.name);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("PackageManager", "Package não encontrado: " + packageName, e);
        }
        return activityNames;
    }
}
