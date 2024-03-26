package br.com.inngage.sdk;

import static br.com.inngage.sdk.IPreferenceConstants.PREF_DEVICE_UUID;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.BuildConfig;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InngageService extends Worker {
    private static final String TAG = InngageConstants.TAG_NOTIFY;
    JSONObject jsonBody, jsonObj, jsonCustomField;
    AppPreferences appPreferences;
    static String appFireToken = "";

    public InngageService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String appToken = getInputData().getString(InngageConstants.EXTRA_TOKEN);
            String identifier = getInputData().getString(InngageConstants.EXTRA_IDENTIFIER);
            String env = getInputData().getString(InngageConstants.EXTRA_ENV);
            String provider = getInputData().getString(InngageConstants.EXTRA_PROV);
            String customFields = getInputData().getString(InngageConstants.EXTRA_CUSTOM_FIELD);
            String email = getInputData().getString(InngageConstants.EXTRA_EMAIL);
            String phoneNumber = getInputData().getString(InngageConstants.EXTRA_PHONE);

            String[] intentBundle = new String[7];
            intentBundle[0] = appToken;
            intentBundle[1] = identifier;
            intentBundle[2] = env;
            intentBundle[3] = provider;
            intentBundle[4] = customFields;
            intentBundle[5] = email;
            intentBundle[6] = phoneNumber;

            handleActionSubscriber(intentBundle);

            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider) {
        startInit(context, appToken, env, provider, null, null, null, null);
    }

    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier) {
        startInit(context, appToken, env, provider, identifier, null, null, null);
    }

    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier,
            JSONObject customFields) {
        startInit(context, appToken, env, provider, identifier, customFields, null, null);
    }
    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier,
            JSONObject customFields,
            String email) {
        startInit(context, appToken, env, provider, identifier, customFields, email, null);
    }
    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier,
            JSONObject customFields,
            String email,
            String phoneNumber) {
        startInit(context, appToken, env, provider, identifier, customFields, email, phoneNumber);
    }
    public static void startInit(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier,
            JSONObject customFields,
            String email,
            String phoneNumber) {
        try {
            Data.Builder dataBuilder = new Data.Builder();

            validateInputParameters(appToken, env, provider, identifier);

            dataBuilder.putString(InngageConstants.EXTRA_TOKEN, appToken)
                    .putString(InngageConstants.EXTRA_ENV, env)
                    .putString(InngageConstants.EXTRA_PROV, provider);

            if (identifier != null)
                dataBuilder.putString(InngageConstants.EXTRA_IDENTIFIER, identifier);
            if (customFields != null)
                dataBuilder.putString(InngageConstants.EXTRA_CUSTOM_FIELD, customFields.toString());
            if (email != null)
                dataBuilder.putString(InngageConstants.EXTRA_EMAIL, email);
            if (phoneNumber != null)
                dataBuilder.putString(InngageConstants.EXTRA_PHONE, phoneNumber);

            Data inputData = dataBuilder.build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(InngageService.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(context).enqueue(workRequest);
        } catch (IllegalArgumentException e) {
            Log.e(InngageConstants.TAG_ERROR, e.getMessage());
        }
    }

    private static void validateInputParameters(String appToken, String env, String provider, String identifier) {
        if (!ValidateProperties.validateAppToken(appToken)) {
            throw new IllegalArgumentException(InngageConstants.INVALID_APP_TOKEN);
        }

        if (!ValidateProperties.validateIdentifier(identifier)){
            throw new IllegalArgumentException(InngageConstants.INVALID_IDENTIFIER);
        }

        if (!ValidateProperties.validateEnvironment(env)) {
            throw new IllegalArgumentException(InngageConstants.INVALID_ENVIRONMENT);
        }

        if (!ValidateProperties.validateProvider(provider)) {
            throw new IllegalArgumentException(InngageConstants.INVALID_PROVIDER);
        }
    }

    public static void sendEvent(String appToken, String identifier, String eventName) {
        InngageUtils utils = new InngageUtils();
        JSONObject jsonBody = new JSONObject();
        JSONObject jsonObj = new JSONObject();

        try {
            jsonBody.put("app_token", appToken);
            jsonBody.put("identifier", identifier);
            jsonBody.put("event_name", eventName);
            jsonBody.put("event_values", "");
            jsonObj.put("newEventRequest", jsonBody);

            utils.doPost(jsonObj, InngageConstants.API_PROD_ENDPOINT + "/events/newEvent/", new HttpResponseCallback() {
                @Override
                public void onResponse(String response) { }
                @Override
                public void onError(String errorMessage) { }
            });
        } catch (JSONException e) {
            Log.e(InngageConstants.TAG_ERROR, "Error in createSubscriptionRequest \n" + e);
        }
    }
    public static void sendEvent(String appToken, String identifier, String eventName, JSONObject eventValues) {
        InngageUtils utils = new InngageUtils();
        JSONObject jsonBody = new JSONObject();
        JSONObject jsonObj = new JSONObject();

        try {
            jsonBody.put("app_token", appToken);
            jsonBody.put("identifier", identifier);
            jsonBody.put("event_name", eventName);
            jsonBody.put("event_values", eventValues);
            jsonObj.put("newEventRequest", jsonBody);

            utils.doPost(jsonObj, InngageConstants.API_PROD_ENDPOINT + "/events/newEvent/", new HttpResponseCallback() {
                @Override
                public void onResponse(String response) { }
                @Override
                public void onError(String errorMessage) { }
            });
        } catch (JSONException e) {
            Log.e(InngageConstants.TAG_ERROR, "Error in createSubscriptionRequest \n" + e);
        }
    }

    private void handleActionSubscriber(String[] intentBundle) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Calling handleActionSubscriber");
        }
        final String[] token = {""};
        String provider = intentBundle[3];

        if (provider == null)
            return;

        if (InngageConstants.FCM_PLATFORM.equals(provider)) {
            try {
                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                String fcmToken = task.getResult();
                                token[0] = fcmToken;
                                Log.i(TAG, "FCM Token: " + token[0]);
                                sendRegistrationToServer(token[0], intentBundle);
                            } else {
                                Log.e(InngageConstants.TAG_ERROR, "Failed to get FCM token");
                            }
                        });
            } catch (Exception e) {
                Log.e(InngageConstants.TAG_ERROR, "Failed to get FCM token", e);
                return;
            }
        } else {
            Log.e(InngageConstants.TAG_ERROR, "No valid provider found");
            return;
        }

        if (!token[0].isEmpty()) {
            Log.i(TAG, "Token: " + token[0]);
            sendRegistrationToServer(token[0], intentBundle);
        }
    }

    private void sendRegistrationToServer(String token, String[] intentBundle) {
        InngageUtils utils = new InngageUtils();
        jsonBody = createSubscriberRequest(token, intentBundle);
        String endpoint = InngageConstants.INNGAGE_DEV_ENV.equals(intentBundle[2]) ? InngageConstants.API_DEV_ENDPOINT : InngageConstants.API_PROD_ENDPOINT;

        utils.doPost(jsonObj, endpoint + InngageConstants.PATH_SUBSCRIPTION, new HttpResponseCallback() {
            @Override
            public void onResponse(String response) { }
            @Override
            public void onError(String errorMessage) { }
        });
    }

    public JSONObject createSubscriberRequest(String regId, String[] intentBundle) {
        jsonBody = new JSONObject();
        jsonObj = new JSONObject();
        AppInfo app = getAppInfo();

        try {
            String identifier = "";
            if (intentBundle[1] != null) {
                identifier = intentBundle[1];
            } else {
                identifier = getDeviceId();
            }
            String _MODEL = Build.MODEL;
            String _MANUFACTURER = Build.MANUFACTURER;
            String _LOCALE = getApplicationContext().getResources().getConfiguration().locale.getDisplayCountry();
            String _LANGUAGE = getApplicationContext().getResources().getConfiguration().locale.getDisplayLanguage();
            String _RELEASE = Build.VERSION.RELEASE;

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

            // Check if api level is more than 19
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (NotificationsUtils.isNotificationEnabled(getApplicationContext())) {
                    jsonBody.put("opt_in", "1");
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Push notifications is enabled");
                    }
                } else {
                    jsonBody.put("opt_in", "0");
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Push notifications is disabled");
                    }
                }
            } else {
                if (NotificationManagerCompat.from(getApplicationContext()).areNotificationsEnabled()) {
                    jsonBody.put("opt_in", "1");
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Push notifications is enabled");
                    }
                } else {
                    jsonBody.put("opt_in", "0");
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Push notifications is disabled");
                    }
                }
            }
            if (intentBundle[4] != null) {
                jsonCustomField = new JSONObject(intentBundle[4]);
                jsonBody.put("custom_field", jsonCustomField);
            }
            jsonObj.put("registerSubscriberRequest", jsonBody);

            if (intentBundle[5] != null) {
                jsonBody.put("email", intentBundle[5]);
            }
            if (intentBundle[6] != null) {
                jsonBody.put("phone", intentBundle[6]);
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "JSON Request: " + jsonObj.toString());
            }
        } catch (JSONException e) {
            Log.e(InngageConstants.TAG_ERROR, "Error in createSubscriptionRequest \n" + e);
        }
        return jsonObj;
    }

    public AppInfo getAppInfo() {

        String packageName = getApplicationContext().getPackageName();
        String updateDate = "";
        String installationDate = "";
        String versionName = "";

        try {
            final PackageManager pm = this.getApplicationContext().getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            installationDate = dateFormat.format(new Date(packageInfo.firstInstallTime));
            updateDate = dateFormat.format(new Date(packageInfo.lastUpdateTime));
            versionName = packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(InngageConstants.TAG_ERROR, "Failed to get app info: ", e);
        }
        return new AppInfo(installationDate, updateDate, versionName);
    }

    private String getDeviceId() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Trying to get the device ID");
        }
        String deviceId = "";

        try {
            if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Permission to ACCESS_COARSE_LOCATION was granted, getMacAddress will be used Android API");
                }
                deviceId = getMacAddress();
                if ("02:00:00:00:00:00".equals(deviceId)) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Device UUID returned is 02:00:00:00:00:00 alternative will be used");
                    }
                    deviceId = InngageUtils.getMacAddress();
                }
            } else if (!"".equals(InngageUtils.getMacAddress())) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "No permission to ACCESS_COARSE_LOCATION was granted, getMacAddress will be used alternative mode");
                }
                deviceId = InngageUtils.getMacAddress();
            } else if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Permission to READ_PHONE_STATE was granted, device IMEI will be used");
                }
                deviceId = getDeviceImei();
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "No permissions granted, ANDROID_ID will be used");
                }
                deviceId = Settings.Secure.getString(this.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            }
            appPreferences = new AppPreferences(this.getApplicationContext());
            appPreferences.putString(PREF_DEVICE_UUID, deviceId);
            if (BuildConfig.DEBUG) {

                Log.d(TAG, "Device UUID: " + deviceId);
            }
        } catch (Exception e) {
            deviceId = appFireToken;
        }
        return deviceId;
    }

    private String getMacAddress() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Getting the device MacAddress by Android API: " + info.getMacAddress());
        }
        return info.getMacAddress();
    }
    private String getDeviceImei() {
        String deviceid = "";
        try {
            TelephonyManager telephonyManager = (TelephonyManager) this.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.i(TAG, "Getting the device IMEI: " + deviceid);
                    deviceid = telephonyManager.getImei();
                } else {
                    deviceid = telephonyManager.getDeviceId();
                    Log.i(TAG, "Getting the device IMEI: " + deviceid);
                }
                return deviceid;
            } else {
                return appFireToken;
            }
        } catch (Exception e) {
            return appFireToken;
        }
    }
}

class NotificationsUtils {
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
    public static boolean isNotificationEnabled(Context context) {
        AppOpsManager mAppOps = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        }

        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        Class appOpsClass = null; /* Context.APP_OPS_MANAGER */

        try {
            // Check if api level is more than 19
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                appOpsClass = Class.forName(AppOpsManager.class.getName());
                Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE, String.class);
                Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
                int value = (int) opPostNotificationValue.get(Integer.class);

                return ((int) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
}
