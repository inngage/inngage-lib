package br.com.inngage.sdk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import com.google.firebase.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InngageUtils {
    private static final String TAG = InngageConstants.TAG_NOTIFY;
    public InngageUtils() {
        super();
    }
    private final ExecutorService service = Executors.newFixedThreadPool(10);
    public void doPost(JSONObject jsonBody, String endpoint, HttpResponseCallback callback){
        service.submit(() -> {
            try {
                String response = performHttpPost(jsonBody, endpoint);
                callback.onResponse(response);
            } catch (IOException e) {
                e.printStackTrace();
                callback.onError("Erro ao fazer a solicitação: " + e.getMessage());
            }
        });
    }
    public String performHttpPost(JSONObject jsonBody, String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(20000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        String message = jsonBody.toString();
        conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

        OutputStream os = conn.getOutputStream();
        os.write(message.getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = new BufferedInputStream(conn.getInputStream());
            String response = convertStreamToString(inputStream);
            inputStream.close();
            return response;
        } else {
            return "Erro ao fazer a solicitação: " + responseCode;
        }
    }
    public String convertStreamToString(java.io.InputStream is) {
        try(Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A")){
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    public static JSONObject convertInputStreamToJSON(InputStream in) throws JSONException {
        BufferedReader streamReader = null;
        try {
            streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuilder responseStrBuilder = new StringBuilder();

        String inputStr;
        try {
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONObject(responseStrBuilder.toString());
    }
    public static void callbackNotification(String notifyID, String appToken, String endpoint) {

        JSONObject jsonBody = new JSONObject();
        jsonBody = createNotificationCallback(notifyID, appToken);
        new InngageUtils().doPost(jsonBody, endpoint, new HttpResponseCallback(){
            @Override
            public void onResponse(String response) { }
            @Override
            public void onError(String errorMessage) { }
        });
    }
    public static JSONObject createNotificationCallback(String notificationId, String appToken) {
        JSONObject jsonBody = new JSONObject();
        JSONObject jsonObj = new JSONObject();

        try {
            jsonBody.put("id", notificationId);
            jsonBody.put("app_token", appToken);
            jsonObj.put("notificationRequest", jsonBody);

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "JSON Request: " + jsonObj.toString());
            }
        } catch (Throwable t) {
            Log.e(InngageConstants.TAG_ERROR, "Error in createNotificationCallbackRequest: " + t);
        }
        return jsonObj;
    }

    public static void handleNotification(
            Context context,
            Intent intent,
            String inngageAppToken,
            String inngageEnvironment) {
        String[] values = new String[InngageConstants.keys.length];
        String[] prefsValues = new String[InngageConstants.keys.length];

        AppPreferences appPreferences = new AppPreferences(context);

        boolean hasNotification = intent.getExtras() != null && !intent.getExtras().isEmpty();

        String endpoint = InngageConstants.INNGAGE_DEV_ENV.equals(inngageEnvironment)
                ? InngageConstants.API_DEV_ENDPOINT
                : InngageConstants.API_PROD_ENDPOINT;

        for (int i = 0; i < InngageConstants.keys.length; i++) {
            prefsValues[i] = appPreferences.getString(values[i], "");
            if (intent.hasExtra(InngageConstants.keys[i])){
                values[i] = intent.getStringExtra(InngageConstants.keys[i]);
            } else {
                values[i] = prefsValues[i];
            }
            for (String key : InngageConstants.keys) {
                appPreferences.putString(key, values[i]);
            }
        }

        String notId = values[0];
        String title = values[2];
        String body = values[3];
        String type = values[4];
        String url = values[5];

        if (hasNotification) {
            callbackNotification(
                    notId,
                    inngageAppToken,
                    endpoint + InngageConstants.PATH_NOTIFICATION_CALLBACK);

            if (isUrlValid(url)) {
                if (type.equals("deep")) {
                    deep(context, url);
                } else if (type.equals("inapp")) {
                    web(url, context);
                }
            }
        }
    }

    public static void handleNotification(
            Context context,
            Intent intent,
            String inngageAppToken,
            String inngageEnvironment,
            boolean blockDeepLink) {
        String[] values = new String[InngageConstants.keys.length];
        String[] prefsValues = new String[InngageConstants.keys.length];

        AppPreferences appPreferences = new AppPreferences(context);

        boolean hasNotification = intent.getExtras() != null && !intent.getExtras().isEmpty();

        String endpoint = InngageConstants.INNGAGE_DEV_ENV.equals(inngageEnvironment)
                ? InngageConstants.API_DEV_ENDPOINT
                : InngageConstants.API_PROD_ENDPOINT;

        for (int i = 0; i < InngageConstants.keys.length; i++) {
            prefsValues[i] = appPreferences.getString(values[i], "");
            if (intent.hasExtra(InngageConstants.keys[i])){
                values[i] = intent.getStringExtra(InngageConstants.keys[i]);
            } else {
                values[i] = prefsValues[i];
            }
            for (String key : InngageConstants.keys) {
                appPreferences.putString(key, values[i]);
            }
        }

        String notId = values[0];
        String title = values[2];
        String body = values[3];
        String type = values[4];
        String url = values[5];

        if (hasNotification) {
            callbackNotification(
                    notId,
                    inngageAppToken,
                    endpoint + InngageConstants.PATH_NOTIFICATION_CALLBACK);

            if (!blockDeepLink) {
                if (isUrlValid(url)) {
                    if (type.equals("deep")) {
                        deep(context, url);
                    } else if (type.equals("inapp")) {
                        web(url, context);
                    }
                }
            }
        }
    }

    private static boolean isUrlValid(String url){
        return url != null && !url.isEmpty();
    }

    public static void web(String url, Context appContext) {
        if (url != null) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.addDefaultShareMenuItem();
            builder.setStartAnimations(appContext, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            builder.setExitAnimations(appContext, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

            builder.setShowTitle(true);
            builder.enableUrlBarHiding();

            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(appContext, Uri.parse(url));
        }
    }

    public static void deep(Context context, String url){
        if (url != null){
            Uri webpage = Uri.parse(url);
            context.startActivity(new Intent(Intent.ACTION_VIEW, webpage));
        }
    }

    private static String getSessionToken() {
        JSONObject jsonResponse = new JSONObject();
        InngageSessionToken sessionToken;
        URL url;
        InputStream in;
        InputStreamReader isw;
        String endpoint = "", token = "", status = "";

        HttpURLConnection urlConnection = null;

        try {
            Log.i(TAG, "Getting the session token");

            String identifier = getMacAddress();
            endpoint = InngageConstants.API_ENDPOINT + "/session/" + identifier;

            url = new URL(endpoint);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = urlConnection.getInputStream();

            jsonResponse = convertInputStreamToJSON(in);

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Server Response: " + jsonResponse);
            }
            if (jsonResponse != null) {
                if (!"".equals(jsonResponse.getString("success"))) {
                    status = jsonResponse.getString("success");
                }
                if ("false".equals(status)) {
                    for (int i = 0; i < 2; i++) {
                        getSessionToken();
                    }
                }
                if (!"".equals(jsonResponse.getString("token"))) {
                    sessionToken = new InngageSessionToken(jsonResponse.getString("token"));
                    token = sessionToken.getToken();

                    Log.i(TAG, "Session token generated: " + sessionToken.getToken());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        return token;
    }
    protected static String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }
                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }
                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                Log.i(TAG, "Getting the device MacAddress by alternative mode: " + res1.toString());
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public Bitmap getBitmapfromUrl(String imageUrl) {
        try {
            if ("".equals(imageUrl)) {
                Log.i(TAG, "Big picture image is null");
                return null;
            } else {
                // This request is synchronous, so it shouldn't be made from main thread
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
