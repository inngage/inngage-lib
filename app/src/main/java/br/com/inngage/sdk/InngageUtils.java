package br.com.inngage.sdk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.support.customtabs.CustomTabsIntent;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
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

import static br.com.inngage.sdk.InngageConstants.API_DEV_ENDPOINT;
import static br.com.inngage.sdk.InngageConstants.API_PROD_ENDPOINT;
import static br.com.inngage.sdk.InngageConstants.INNGAGE_DEV_ENV;
import static br.com.inngage.sdk.InngageConstants.PATH_NOTIFICATION_CALLBACK;

/**
 * Created by viniciusdepaula on 07/07/16.
 */
public class InngageUtils {

    private static final String TAG = InngageConstants.TAG;

    public InngageUtils() {

        super();
    }

    JSONObject jsonBody, jsonObj;

    public void doPost(JSONObject jsonBody, String endpoint) {

        if(BuildConfig.DEBUG) {

            Log.d(TAG, "API Endpoint: " + endpoint);
        }

        OutputStream os = null;
        InputStream is = null;
        HttpURLConnection conn = null;
        int readTimeout = 10000;
        int connectTimeout = 20000;

        try {

            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            String message = jsonBody.toString();
            conn.setReadTimeout(readTimeout);
            conn.setConnectTimeout(connectTimeout);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(message.getBytes().length);

            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }

            conn.connect();
            os = new BufferedOutputStream(conn.getOutputStream());
            os.write(message.getBytes());
            os.flush();
            is = conn.getInputStream();

            if (BuildConfig.DEBUG) {

                Log.d(TAG, "Server Response:" + convertStreamToString(is));

            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (os != null) {

                    os.close();
                }

            } catch (IOException e) {

                e.printStackTrace();
            }
            try {

                if (is != null) {

                    is.close();
                }
            } catch (IOException e) {

                e.printStackTrace();
            }
            if (conn != null) {

                conn.disconnect();
            }
        }
    }

    public String convertStreamToString(java.io.InputStream is) {

        try {

            return new java.util.Scanner(is).useDelimiter("\\A").next();

        } catch (java.util.NoSuchElementException e) {

            return "";
        }
    }

    public static JSONObject convertInputStremToJSON(InputStream in) throws JSONException {

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

    public JSONObject createLocationRequest(String deviceID, double lat, double lon) {

        jsonBody = new JSONObject();
        jsonObj = new JSONObject();

        try {

            jsonBody.put("uuid", deviceID);
            jsonBody.put("lat", lat);
            jsonBody.put("lon", lon);
            jsonObj.put("registerGeolocationRequest", jsonBody);

            if(BuildConfig.DEBUG) {

                Log.d(TAG, "JSON Request: " + jsonObj.toString());
            }

        } catch (Throwable t) {

            Log.d(TAG, "Error in createLocationRequest: " + t);
        }
        return jsonObj;
    }

    public JSONObject createLocationRequest(String deviceID, double lat, double lon, String appToken) {

        jsonBody = new JSONObject();
        jsonObj = new JSONObject();

        try {

            jsonBody.put("uuid", deviceID);
            jsonBody.put("lat", lat);
            jsonBody.put("lon", lon);
            jsonBody.put("app_token", appToken);
            jsonObj.put("registerGeolocationRequest", jsonBody);

            if(BuildConfig.DEBUG) {

                Log.d(TAG, "JSON Request: " + jsonObj.toString());
            }

        } catch (Throwable t) {

            Log.d(TAG, "Error in createLocationRequest: " + t);
        }
        return jsonObj;
    }

    public static void callbackNotification(String notifyID, String appToken) {

        JSONObject jsonBody = new JSONObject();
        jsonBody = createNotificationCallback(notifyID, appToken);
        new InngageUtils().doPost(jsonBody, InngageConstants.NOTIFICATION_CALLBACK);
    }

    public static void callbackNotification(String notifyID, String appToken, String endpoint) {

        JSONObject jsonBody = new JSONObject();
        jsonBody = createNotificationCallback(notifyID, appToken);
        new InngageUtils().doPost(jsonBody, endpoint);
    }

    public static JSONObject createNotificationCallback(String notificationId, String appToken) {

        JSONObject jsonBody = new JSONObject();
        JSONObject jsonObj = new JSONObject();

        try {

            jsonBody.put("id", notificationId);
            jsonBody.put("app_token", appToken);
            jsonObj.put("notificationRequest", jsonBody);

            if(BuildConfig.DEBUG) {

                Log.d(TAG, "JSON Request: " + jsonObj.toString());
            }

        } catch (Throwable t) {

            Log.d(TAG, "Error in createNotificationCallbackRequest: " + t);
        }
        return jsonObj;
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

            Log.d(TAG, "Getting the session token");

            String identifier = getMacAddress();
            endpoint = InngageConstants.API_ENDPOINT + "/session/" + identifier;

            url = new URL(endpoint);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = urlConnection.getInputStream();

            jsonResponse = convertInputStremToJSON(in);

            if(BuildConfig.DEBUG) {

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

                    Log.d(TAG, "Session token generated: " + sessionToken.getToken());
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
                Log.d(TAG, "Getting the device MacAddress by alternative mode: " + res1.toString());

                return res1.toString();
            }
        } catch (Exception ex) {

            ex.printStackTrace();
        }
        return "";
    }

    //// TODO: 18/04/17  implementar passagem da url e iniciar web view
    public static void showDialog(String title,
                                  String body,
                                  final String notifyID,
                                  final String appToken,
                                  Context appContext) {

        //String endpoint = INNGAGE_DEV_ENV.equals(intentBundle[2]) ? API_DEV_ENDPOINT : API_PROD_ENDPOINT;

        if ("".equals(title)) {

            title = getApplicationName(appContext);
        }

                callbackNotification(notifyID, appToken);

        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);
        builder.setTitle(title);
        builder.setMessage(body);
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Log.d(TAG, "Button OK pressed by the user");

                    }
                });
        builder.show();
    }



    public static void showDialogwithLink(String title,
                                  String body,
                                  final String notifyID,
                                  final String appToken,
                                  final String environment,
                                  final String url,
                                  Context appContext) {

        String endpoint = INNGAGE_DEV_ENV.equals(environment) ? API_DEV_ENDPOINT : API_PROD_ENDPOINT;

        if ("".equals(title)) {

            title = getApplicationName(appContext);
        }

        callbackNotification(notifyID, appToken, endpoint + PATH_NOTIFICATION_CALLBACK);

        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);
        builder.setTitle(title);
        builder.setMessage(body+"   "+url);
        builder.setPositiveButton("Veja mais",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Log.d(TAG, "Button OK pressed by the user");


                    }
                });
        builder.setNegativeButton("Fechar",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Log.d(TAG, "Button Fechar pressed by the user");



                    }
                });
        builder.show();

    }


    public static void showDialog(String title,
                                  String body,
                                  final String notifyID,
                                  final String appToken,
                                  final String environment,
                                  Context appContext) {

        String endpoint = INNGAGE_DEV_ENV.equals(environment) ? API_DEV_ENDPOINT : API_PROD_ENDPOINT;

        if ("".equals(title)) {

            title = getApplicationName(appContext);
        }

        callbackNotification(notifyID, appToken, endpoint + PATH_NOTIFICATION_CALLBACK);

        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);
        builder.setTitle(title);
        builder.setMessage(body);
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Log.d(TAG, "Button OK pressed by the user");

                    }
                });
        builder.show();
    }

    /**
     * Get the application name.
     *
     * @param context Application context.
     */
    public static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    public static String decodeIdentifier(String base64) {

        String identifier = "";
        byte[] data;

        try {

            data = Base64.decode(base64, Base64.DEFAULT);
            identifier = new String(data, "UTF-8");

        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        }
        return identifier;
    }

    public static String encodeIdentifier(String identifier) {

        String base64 = "";
        byte[] data;

        try {

            data = identifier.getBytes("UTF-8");
            base64 = Base64.encodeToString(data, Base64.DEFAULT);

        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        }
        return base64;
    }

    /*
    * To get a Bitmap image from the URL received
    * @imageUrl image URL
    */
    public Bitmap getBitmapfromUrl(String imageUrl) {

        try {

            if("".equals(imageUrl)) {

                Log.d(TAG, "Big picture image is null");
                return null;

            } else {

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

    /**
     * @return true If device has Android Marshmallow or above version
     */
    public static boolean hasM() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}