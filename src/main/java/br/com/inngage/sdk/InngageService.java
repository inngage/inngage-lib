package br.com.inngage.sdk;

import static br.com.inngage.sdk.IPreferenceConstants.PREF_ANON_IDENTIFIER;
import static br.com.inngage.sdk.IPreferenceConstants.PREF_DEVICE_UUID;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.android.gms.location.*;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.BuildConfig;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class InngageService extends ListenableWorker {
    private static final String TAG = InngageConstants.TAG;
    /** Upper bound for a single location fetch; on expiry we subscribe without coordinates. */
    private static final long LOCATION_TIMEOUT_MS = 5000L;
    JSONObject jsonBody, jsonObj, jsonCustomField;
    AppPreferences appPreferences;
    static String appFireToken = "";
    /**
     * Environment ("dev"/"prod") remembered from the most recent subscribe call so
     * {@link #sendEvent} can target the matching backend. Defaults to prod, which
     * preserves the previous always-prod behavior until subscribe runs.
     */
    private static volatile String sEnvironment = InngageConstants.INNGAGE_PROD_ENV;

    public InngageService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        String appToken = getInputData().getString(InngageConstants.EXTRA_TOKEN);
        String identifier = getInputData().getString(InngageConstants.EXTRA_IDENTIFIER);
        String env = getInputData().getString(InngageConstants.EXTRA_ENV);
        String provider = getInputData().getString(InngageConstants.EXTRA_PROV);
        String customFields = getInputData().getString(InngageConstants.EXTRA_CUSTOM_FIELD);
        String email = getInputData().getString(InngageConstants.EXTRA_EMAIL);
        String phoneNumber = getInputData().getString(InngageConstants.EXTRA_PHONE);
        boolean requestGeoLocator = getInputData().getBoolean(InngageConstants.GEOLOCATION, false);

        String[] intentBundle = new String[7];
        intentBundle[0] = appToken;
        intentBundle[1] = identifier;
        intentBundle[2] = env;
        intentBundle[3] = provider;
        intentBundle[4] = customFields;
        intentBundle[5] = email;
        intentBundle[6] = phoneNumber;

        return CallbackToFutureAdapter.getFuture(completer -> {
            handleActionSubscriber(intentBundle, requestGeoLocator, completer);
            return "Inngage Geo-location registration";
        });
    }

    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider) {
        startInit(context, appToken, env, provider, null, null, null, null, false);
    }

    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier) {
        startInit(context, appToken, env, provider, identifier, null, null, null, false);
    }

    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier,
            JSONObject customFields) {
        startInit(context, appToken, env, provider, identifier, customFields, null, null, false);
    }
    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier,
            JSONObject customFields,
            String email) {
        startInit(context, appToken, env, provider, identifier, customFields, email, null, false);
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
        startInit(context, appToken, env, provider, identifier, customFields, email, phoneNumber, false);
    }
    public static void subscribe(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier,
            JSONObject customFields,
            String email,
            String phoneNumber,
            boolean requestGeoLocator){
        startInit(context, appToken, env, provider, identifier, customFields, email, phoneNumber, requestGeoLocator);
    }
    public static void startInit(
            Context context,
            String appToken,
            String env,
            String provider,
            String identifier,
            JSONObject customFields,
            String email,
            String phoneNumber,
            boolean requestGeoLocator) {
        try {
            Data.Builder dataBuilder = new Data.Builder();

            validateInputParameters(appToken, env, provider, identifier, customFields);

            // Remember the environment so sendEvent() targets the same backend.
            sEnvironment = env;

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
            if (requestGeoLocator){
                dataBuilder.putBoolean(InngageConstants.GEOLOCATION, true);
            }

            Data inputData = dataBuilder.build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(InngageService.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(context).enqueue(workRequest);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    private static void validateInputParameters(String appToken, String env, String provider, String identifier, JSONObject customFields) {
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

        if (!ValidateProperties.validateCustomField(customFields)) {
            throw new IllegalArgumentException(InngageConstants.INVALID_CUSTOM_FIELD);
        }
    }

    public static void sendEvent(String appToken, String identifier, String eventName) {
        doSendEvent(appToken, identifier, eventName, null, false, 0f, null);
    }

    public static void sendEvent(String appToken, String identifier, String eventName, JSONObject eventValues) {
        doSendEvent(appToken, identifier, eventName, eventValues, false, 0f, null);
    }

    public static void sendEvent(String appToken, String identifier, String eventName,
                                 boolean conversionEvent, float conversionValue, String conversionId) {
        doSendEvent(appToken, identifier, eventName, null, conversionEvent, conversionValue, conversionId);
    }

    public static void sendEvent(String appToken, String identifier, String eventName,
                                 boolean conversionEvent) {
        doSendEvent(appToken, identifier, eventName, null, conversionEvent, 0f, null);
    }

    public static void sendEvent(String appToken, String identifier, String eventName,
                                 boolean conversionEvent, float conversionValue) {
        doSendEvent(appToken, identifier, eventName, null, conversionEvent, conversionValue, null);
    }

    public static void sendEvent(String appToken, String identifier, String eventName, JSONObject eventValues,
                                 boolean conversionEvent, float conversionValue, String conversionId) {
        doSendEvent(appToken, identifier, eventName, eventValues, conversionEvent, conversionValue, conversionId);
    }

    public static void sendEvent(String appToken, String identifier, String eventName, JSONObject eventValues,
                                 boolean conversionEvent, float conversionValue) {
        doSendEvent(appToken, identifier, eventName, eventValues, conversionEvent, conversionValue, null);
    }

    public static void sendEvent(String appToken, String identifier, String eventName, JSONObject eventValues,
                                 boolean conversionEvent) {
        doSendEvent(appToken, identifier, eventName, eventValues, conversionEvent, 0f, null);
    }

    private static void doSendEvent(String appToken, String identifier, String eventName,
                                    JSONObject eventValues, boolean conversionEvent,
                                    float conversionValue, String conversionId) {
        InngageUtils utils = new InngageUtils();
        JSONObject jsonBody = new JSONObject();
        JSONObject jsonObj = new JSONObject();

        try {
            jsonBody.put("app_token", appToken);
            jsonBody.put("identifier", identifier);
            jsonBody.put("event_name", eventName);
            jsonBody.put("event_values", eventValues != null ? eventValues : "");
            if (conversionEvent) {
                jsonBody.put("conversion_event", true);
            }
            if (conversionValue != 0f){
                jsonBody.put("conversion_value", conversionValue);
            }
            if (conversionId != null) {
                jsonBody.put("conversion_id", conversionId);
            }
            jsonObj.put("newEventRequest", jsonBody);

            String endpoint = InngageConstants.INNGAGE_DEV_ENV.equals(sEnvironment)
                    ? InngageConstants.EVENT_ENDPOINT_DEV
                    : InngageConstants.EVENT_ENDPOINT_PROD;

            utils.doPost(jsonObj, endpoint, new HttpResponseCallback() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "sendEvent response: " + response);
                }
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "sendEvent error: " + errorMessage);
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Error in sendEvent \n" + e);
        }
    }

    private void handleActionSubscriber(
            String[] intentBundle,
            boolean requestGeoLocator,
            CallbackToFutureAdapter.Completer<Result> completer) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Calling handleActionSubscriber");
        }

        String provider = intentBundle[3];

        if (provider == null) {
            completer.set(Result.failure());
            return;
        }

        if (InngageConstants.FCM_PLATFORM.equals(provider)) {
            try {
                // Use a background executor so the entire geo + network chain never
                // touches the main thread, eliminating ANR risk.
                java.util.concurrent.Executor bgExecutor =
                        java.util.concurrent.Executors.newSingleThreadExecutor();

                FirebaseMessaging.getInstance().getToken()
                        .addOnCompleteListener(bgExecutor, task -> {
                            String fcmToken = task.isSuccessful() ? task.getResult() : null;
                            // registration must never be empty/null — without it the
                            // subscriber cannot receive push, so we don't send at all.
                            if (fcmToken != null && !fcmToken.trim().isEmpty()) {
                                Log.d(TAG, "FCM Token: " + fcmToken);
                                // Single entry point — no risk of double completer.set()
                                getGeoLocationAndSend(fcmToken, intentBundle, requestGeoLocator, completer);
                            } else {
                                Log.e(TAG, "FCM token vazio ou indisponível; inscrição não será enviada.");
                                completer.set(Result.failure());
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "Failed to get FCM token", e);
                completer.set(Result.failure());
            }
        } else {
            Log.d(TAG, "No valid provider found");
            completer.set(Result.failure());
        }
        // Removed the erroneous second call to getGeoLocationAndSend that used
        // token[0] — that check always evaluated to false (async) but could race
        // in rare cases where getToken() returned synchronously from cache,
        // causing completer.set() to be called twice and throwing IllegalStateException.
    }

    private void getGeoLocationAndSend(
            String token,
            String[] intentBundle,
            boolean requestGeoLocator,
            CallbackToFutureAdapter.Completer<Result> completer) {
        if (!requestGeoLocator) {
            sendRegistrationToServer(token, intentBundle, false, completer);
            return;
        }

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permissão de localização não concedida; seguindo sem coordenadas.");
            InngageLocationHolder.lat = null;
            InngageLocationHolder.lon = null;
            sendRegistrationToServer(token, intentBundle, true, completer);
            return;
        }

        // Everything below runs off the main thread (bgExecutor + a scheduled
        // timeout), so it never risks an ANR. The timeout also guarantees the
        // flow never hangs waiting for a GPS fix that may never arrive.
        final java.util.concurrent.Executor bgExecutor =
                java.util.concurrent.Executors.newSingleThreadExecutor();
        final java.util.concurrent.ScheduledExecutorService timeoutExec =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

        final FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(getApplicationContext());

        // Single-fire guard: cached fix, fresh fix, error or timeout — whichever
        // finishes first sends the subscription exactly once and tears down the
        // rest, so completer.set() is never called twice or never at all.
        final AtomicBoolean sent = new AtomicBoolean(false);
        final LocationCallback[] pendingCallback = new LocationCallback[1];
        final android.os.HandlerThread[] pendingThread = new android.os.HandlerThread[1];

        final Runnable proceed = () -> {
            if (!sent.compareAndSet(false, true)) {
                return;
            }
            timeoutExec.shutdownNow();
            if (pendingCallback[0] != null) {
                fusedLocationClient.removeLocationUpdates(pendingCallback[0]);
                pendingCallback[0] = null;
            }
            if (pendingThread[0] != null) {
                pendingThread[0].quitSafely();
                pendingThread[0] = null;
            }
            sendRegistrationToServer(token, intentBundle, true, completer);
        };

        // Global upper bound: on expiry, subscribe without lat/long.
        timeoutExec.schedule(() -> {
            if (sent.get()) {
                return;
            }
            Log.w(TAG, "Tempo limite de localização atingido; seguindo sem coordenadas.");
            InngageLocationHolder.lat = null;
            InngageLocationHolder.lon = null;
            proceed.run();
        }, LOCATION_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(bgExecutor, location -> {
                        if (sent.get()) {
                            return;
                        }
                        if (location != null) {
                            InngageLocationHolder.lat = location.getLatitude();
                            InngageLocationHolder.lon = location.getLongitude();
                            Log.d(TAG, "Localização cache: " + InngageLocationHolder.lat + ", " + InngageLocationHolder.lon);
                            proceed.run();
                        } else {
                            requestNewLocation(fusedLocationClient, pendingCallback, pendingThread, sent, proceed);
                        }
                    })
                    .addOnFailureListener(bgExecutor, e -> {
                        if (sent.get()) {
                            return;
                        }
                        Log.e(TAG, "Erro ao obter localização", e);
                        InngageLocationHolder.lat = null;
                        InngageLocationHolder.lon = null;
                        proceed.run();
                    });
        } catch (Exception e) {
            // e.g. SecurityException / Play Services problems — never fail subscribe.
            Log.e(TAG, "Falha ao solicitar localização", e);
            InngageLocationHolder.lat = null;
            InngageLocationHolder.lon = null;
            proceed.run();
        }
    }

    // Permission is verified in getGeoLocationAndSend before this runs, and the
    // request is wrapped in try/catch for a possible SecurityException.
    @SuppressLint("MissingPermission")
    private void requestNewLocation(
            FusedLocationProviderClient fusedLocationClient,
            LocationCallback[] pendingCallback,
            android.os.HandlerThread[] pendingThread,
            AtomicBoolean sent,
            Runnable proceed) {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500)
                .setNumUpdates(1);

        android.os.HandlerThread handlerThread = new android.os.HandlerThread("InngageLocationThread");
        handlerThread.start();

        LocationCallback callback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (sent.get()) {
                    return;
                }
                Location loc = locationResult.getLastLocation();
                if (loc != null) {
                    InngageLocationHolder.lat = loc.getLatitude();
                    InngageLocationHolder.lon = loc.getLongitude();
                    Log.d(TAG, "Localização ativa: " + InngageLocationHolder.lat + ", " + InngageLocationHolder.lon);
                } else {
                    Log.w(TAG, "Localização ativa ainda veio nula.");
                    InngageLocationHolder.lat = null;
                    InngageLocationHolder.lon = null;
                }
                proceed.run();
            }
        };
        pendingCallback[0] = callback;
        pendingThread[0] = handlerThread;

        // If the timeout already fired while we were setting up, tear down now.
        if (sent.get()) {
            fusedLocationClient.removeLocationUpdates(callback);
            handlerThread.quitSafely();
            return;
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, handlerThread.getLooper());
        } catch (Exception e) {
            // If requestLocationUpdates itself throws, don't leak the thread and don't hang.
            Log.e(TAG, "Erro ao solicitar atualização de localização", e);
            InngageLocationHolder.lat = null;
            InngageLocationHolder.lon = null;
            proceed.run();
        }
    }

    static private class InngageLocationHolder {
        static private Double lat = null;
        static private Double lon = null;
    }

    private void sendRegistrationToServer(
            String token,
            String[] intentBundle,
            boolean requestGeoLocator,
            CallbackToFutureAdapter.Completer<Result> completer) {

        // Defensive: registration (FCM token) is required and must never be sent
        // empty/null. Aborts here as a safety net in case an empty token slips through.
        if (token == null || token.trim().isEmpty()) {
            Log.e(TAG, "Registration (FCM token) vazio; inscrição abortada.");
            completer.set(Result.failure());
            return;
        }

        InngageUtils utils = new InngageUtils();
        jsonBody = createSubscriberRequest(token, intentBundle, requestGeoLocator);
        String endpoint = InngageConstants.INNGAGE_DEV_ENV.equals(intentBundle[2])
                ? InngageConstants.API_DEV_ENDPOINT
                : InngageConstants.API_PROD_ENDPOINT;

        utils.doPost(jsonObj, endpoint + InngageConstants.PATH_SUBSCRIPTION, new HttpResponseCallback() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Inscrição enviada com sucesso.");
                completer.set(Result.success());
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Erro ao enviar inscrição: " + errorMessage);
                completer.set(Result.failure());
            }
        });
    }

    public JSONObject createSubscriberRequest(String regId, String[] intentBundle, boolean requestGeoLocator) {
        jsonBody = new JSONObject();
        jsonObj = new JSONObject();
        AppInfo app = getAppInfo();

        try {
            String identifier;
            if (intentBundle[1] != null && !intentBundle[1].trim().isEmpty()) {
                identifier = intentBundle[1];
            } else {
                // No identifier supplied to subscribe(): never send it empty — use a
                // stable anonymous id so this device maps to a single subscriber.
                identifier = getOrCreateAnonymousId();
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

            if (requestGeoLocator && InngageLocationHolder.lat != null && InngageLocationHolder.lon != null) {
                jsonBody.put("lat", String.valueOf(InngageLocationHolder.lat));
                jsonBody.put("long", String.valueOf(InngageLocationHolder.lon));
            }

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
            Log.e(TAG, "Error in createSubscriptionRequest \n" + e);
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
            Log.d(TAG, "Failed to get app info: ", e);
        }
        return new AppInfo(installationDate, updateDate, versionName);
    }

    /**
     * Returns a stable, non-empty anonymous identifier for use when the consumer
     * calls subscribe() without one. Reused across calls (persisted) so the device
     * always maps to the same subscriber instead of creating a new one each time.
     *
     * Prefers {@code Settings.Secure.ANDROID_ID} (stable across reinstalls for the
     * same app signing key, requires no permission); falls back to a random
     * {@link UUID} when ANDROID_ID is missing or the known-buggy value. A true
     * hardware id (IMEI/serial) is intentionally not used — it is unavailable to
     * regular apps on Android 10+.
     */
    private String getOrCreateAnonymousId() {
        AppPreferences prefs = new AppPreferences(getApplicationContext());
        String stored = prefs.getString(PREF_ANON_IDENTIFIER, "");
        if (stored != null && !stored.trim().isEmpty()) {
            return stored;
        }

        String androidId = Settings.Secure.getString(
                getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String anonId;
        if (androidId != null && !androidId.trim().isEmpty()
                && !"9774d56d682e549c".equals(androidId)) {
            anonId = androidId;
        } else {
            anonId = UUID.randomUUID().toString();
        }

        prefs.putString(PREF_ANON_IDENTIFIER, anonId);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Anonymous identifier gerado/recuperado: " + anonId);
        }
        return anonId;
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
    @android.annotation.SuppressLint("MissingPermission")
    private String getDeviceImei() {
        String deviceid = "";
        try {
            TelephonyManager telephonyManager = (TelephonyManager) this.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(TAG, "Getting the device IMEI: " + deviceid);
                    deviceid = telephonyManager.getImei();
                } else {
                    deviceid = telephonyManager.getDeviceId();
                    Log.d(TAG, "Getting the device IMEI: " + deviceid);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
