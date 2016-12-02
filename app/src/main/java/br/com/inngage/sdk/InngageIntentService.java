package br.com.inngage.sdk;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by viniciusdepaula on 17/05/16.
 */
public class InngageIntentService extends IntentService {

    private static final String TAG = InngageConstants.TAG;

    InngageUtils utils;
    TelephonyManager telephonyManager;
    LocationManager mLocationManager;
    JSONObject jsonBody, jsonObj, jsonCustomField;

    public InngageIntentService() {
        super("InngageIntentService");
    }

    /**
     * Start subscriber registration service.
     *
     * @param context Application context
     * @param appToken Application ID on the Inngage Platform
     * @param identifier Unique user identifier in your application
     */
    public static void startInit(Context context, String appToken, String identifier) {

        Intent intent = new Intent(context, (Class)InngageIntentService.class);
        intent.setAction("br.com.inngage.action.REGISTRATION");

        if(!"".equals(appToken)) {

            intent.putExtra("APP_TOKEN", appToken);

        } else {

            Log.d(TAG, "Verify if the value of APP_TOKEN was informed");
            return;
        }

        if(!"".equals(identifier)) {

            intent.putExtra("IDENTIFIER", identifier);

        } else {

            Log.d(TAG, "Verify if the value of IDENTIFIER was informed");
            return;
        }

        Log.d(TAG, "Starting InngageIntentService");

        context.startService(intent);
    }

    /**
     * Start subscriber registration service.
     *
     * @param context Application context
     * @param appToken Application ID on the Inngage Platform
     * @param identifier Unique user identifier in your application
     * @param customFields JSON Object with custom fields
     */
    public static void startInit(Context context, String appToken, String identifier, JSONObject customFields) {

        Intent intent = new Intent(context, (Class)InngageIntentService.class);
        intent.setAction("br.com.inngage.action.REGISTRATION");

        if(!"".equals(appToken)) {

            intent.putExtra("APP_TOKEN", appToken);

        } else {

            Log.d(TAG, "Verify if the value of APP_TOKEN was informed");
            return;
        }

        if(!"".equals(identifier)) {

            intent.putExtra("IDENTIFIER", identifier);

        } else {

            Log.d(TAG, "Verify if the value of IDENTIFIER was informed");
            return;
        }

        if(customFields.length() != 0) {

            intent.putExtra("CUSTOM_FIELDS", customFields.toString());

        } else {

            Log.d(TAG, "Verify if the value of CUSTOM_FIELDS was informed");
            return;
        }

        Log.d(TAG, "Starting InngageIntentService");

        context.startService(intent);
    }

    /**
     * Start subscriber registration service.
     *
     * @param context Application context
     * @param appToken Application ID on the Inngage Platform
     */
    public static void startInit(Context context, String appToken) {

        Intent intent = new Intent(context, (Class)InngageIntentService.class);

        intent.setAction("br.com.inngage.action.REGISTRATION");

        if(!"".equals(appToken)) {

            intent.putExtra("APP_TOKEN", appToken);

        } else {

            Log.d(TAG, "Verify if the value of APP_TOKEN was informed");
            return;
        }

        Log.d(TAG, "Starting InngageIntentService");

        context.startService(intent);
    }

    /**
     * Start subscriber registration service.
     *
     * @param context Application context
     * @param appToken Application ID on the Inngage Platform
     * @param customFields JSON Object with custom fields
     */
    public static void startInit(Context context, String appToken, JSONObject customFields) {

        Intent intent = new Intent(context, (Class)InngageIntentService.class);

        intent.setAction("br.com.inngage.action.REGISTRATION");

        if(!"".equals(appToken)) {

            intent.putExtra("APP_TOKEN", appToken);

        } else {

            Log.d(TAG, "Verify if the value of APP_TOKEN was informed");
            return;
        }

        if(customFields.length() != 0) {

            intent.putExtra("CUSTOM_FIELDS", customFields.toString());

        } else {

            Log.d(TAG, "Verify if the value of CUSTOM_FIELDS was informed");
            return;
        }

        Log.d(TAG, "Starting InngageIntentService");

        context.startService(intent);
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     *
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {

            String action = intent.getAction();

            if ("br.com.inngage.action.REGISTRATION".equals(action)) {

                Bundle bundle = intent.getExtras();

                String[] intentBundle = new String[3];

                if(bundle.getString("APP_TOKEN") != null) {

                    intentBundle[0] = bundle.getString("APP_TOKEN");
                }
                if(bundle.getString("IDENTIFIER") != null) {

                    intentBundle[1] = bundle.getString("IDENTIFIER");
                }
                if(bundle.getString("CUSTOM_FIELDS") != null) {

                    intentBundle[2] = bundle.getString("CUSTOM_FIELDS");
                }
                this.handleActionSubscribe(intentBundle);

                Log.d(TAG, "Calling handleActionSubscribe");
            }
        }
    }

    private void handleActionSubscribe(String[] intentBundle) {

        try {

            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            Log.d(TAG, "GCM Registration Token: " + token);
            sendRegistrationToServer(token, intentBundle);

        } catch (Exception e) {

            Log.d(TAG, "Failed to complete registration: ", e);
        }
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token, String[] intentBundle) {

        jsonBody = createSubscriberRequest(token, intentBundle);
        utils = new InngageUtils();
        utils.doPost(jsonBody, InngageConstants.SUBSCRIPTION);

        Location location = getLastKnownLocation();

        if (location != null) {

            jsonBody = utils.createLocationRequest(getDeviceId(), location.getLatitude(), location.getLongitude());
            utils.doPost(jsonBody, InngageConstants.SUBSCRIPTION);
        }
    }

    public JSONObject createSubscriberRequest(String regId, String[] intentBundle) {

        jsonBody = new JSONObject();
        jsonObj = new JSONObject();

        AppInfo app = getAppInfo();

        try {

            String identifier = "";
            telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

            if (intentBundle[1] != null) {

                identifier = intentBundle[1];

            } else {

                identifier = getDeviceId();
            }

            String _MODEL = android.os.Build.MODEL;
            String _MANUFACTURER = android.os.Build.MANUFACTURER;
            String _LOCALE = getApplicationContext().getResources().getConfiguration().locale.getDisplayCountry();
            String _LANGUAGE = getApplicationContext().getResources().getConfiguration().locale.getDisplayLanguage();
            String _RELEASE = android.os.Build.VERSION.RELEASE;

            /*String encod = InngageUtils.encodeIdentifier(identifier);

            Log.d(TAG, "Encoding identifier: " + encod);

            Log.d(TAG, "Encoding identifier: " + InngageUtils.decodeIdentifier(encod));*/

            jsonBody.put("identifier", identifier);
            jsonBody.put("registration", regId);
            jsonBody.put("platform", InngageConstants.PLATFORM);
            jsonBody.put("sdk", InngageConstants.SDK);
            jsonBody.put("app_token", intentBundle[0]);
            jsonBody.put("device_model", _MODEL);
            jsonBody.put("device_manufacturer", _MANUFACTURER);
            jsonBody.put("os_locale", _LOCALE);
            jsonBody.put("os_language", _LANGUAGE);
            jsonBody.put("os_version", _RELEASE);
            jsonBody.put("app_version", app.getVersionName());
            jsonBody.put("app_installed_in", app.getInstallationDate());
            jsonBody.put("app_updated_in", app.getUpdateDate());
            jsonBody.put("uuid", getDeviceId());
            if (intentBundle[2] != null) {
                jsonCustomField = new JSONObject(intentBundle[2]);
                jsonBody.put("custom_field", jsonCustomField);
            }
            jsonObj.put("registerSubscriberRequest", jsonBody);

            Log.d(TAG, "JSON Request: " + jsonObj.toString());

        } catch (Throwable t) {

            Log.d(TAG, "Error in createSubscriptionRequest: " + t);
        }
        return jsonObj;
    }

    public AppInfo getAppInfo() {

        String packageName = getApplicationContext().getPackageName();
        String updateDate = "";
        String installationDate = "";
        String versionName = "";

        try {

            final PackageManager pm = InngageIntentService.this.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            installationDate = dateFormat.format(new Date(packageInfo.firstInstallTime));
            updateDate = dateFormat.format(new Date(packageInfo.lastUpdateTime));
            versionName = packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {

            Log.d(TAG, "Failed to get app info: ", e);
        }
        return new AppInfo(installationDate, updateDate, versionName);
    }

    private Location getLastKnownLocation() {

        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;

        for (String provider : providers) {

            try {

                Location l = mLocationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            } catch (SecurityException e) {
                Log.d(TAG, "No permissions to get the user Location");
            }
        }
        return bestLocation;
    }

    private String getDeviceId() {

        Log.d(TAG, "Trying to get the device ID");

        String deviceId = "";

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "No permission to ACCESS_COARSE_LOCATION , getMacAddress will be used alternative mode");

            deviceId = InngageUtils.getMacAddress();

        }
        else {

            Log.d(TAG, "Permission ACCESS_COARSE_LOCATION granted, getMacAddress will be used Android API");

            deviceId = getMacAddress();

        }
        return deviceId;
    }

    private String getMacAddress() {

        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();

        Log.d(TAG, "Getting the device MacAddress by Android API: " + info.getMacAddress());

        return info.getMacAddress();
    }
}

final class AppInfo {

    private final String installationDate;
    private final String updateDate;
    private final String versionName;

    public AppInfo(String installationDate, String updateDate, String versionName) {

        this.installationDate = installationDate;
        this.updateDate = updateDate;
        this.versionName = versionName;
    }

    public String getInstallationDate() {
        return installationDate;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public String getVersionName() {
        return versionName;
    }
}

final class InngageConstants {

    public static final String PLATFORM = "android";
    public static final String SDK = "1";
    public static final String API_ENDPOINT = "https://api.inngage.com.br/v1";
    public static final String TAG = "inngage-lib";
    public static final String SUBSCRIPTION = "SUBSCRIPTION";
    public static final String GEOLOCATION = "GEOLOCATION";
    public static final String NOTIFICATION_CALLBACK = "NOTIFICATION_CALLBACK";

}

class MyInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = "InngageFramework";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {

        Intent intent = new Intent(this, InngageIntentService.class);
        Log.d(TAG, "onTokenRefresh called..");
        startService(intent);
    }
}