package br.com.inngage.sdk;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Random;

public class PushMessagingService extends FirebaseMessagingService {

    private static final String TAG = "inngage-lib";
    String body, title = null;
    Random random = new Random();
    int notifyID = random.nextInt(9999 - 1000) + 1000;
    JSONObject jsonObject;

    /**
     * Get the push notification event
     *
     * @param  remoteMessage  Remote message from push notification
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage.getData().size() > 0) {

            jsonObject = parseRemoteMessageToJson(remoteMessage);
            Log.d(TAG, "Push received from " + remoteMessage.getFrom());
            showNotification(jsonObject);
        }
    }

    private JSONObject parseRemoteMessageToJson(RemoteMessage remoteMessage) {

        Map<String, String> params = remoteMessage.getData();
        jsonObject = new JSONObject(params);
        Log.d("Notification JSON:", jsonObject.toString());
        return jsonObject;
    }

    /**
     * Show the push notification
     *
     * @param  jsonObject The message title
     */
    private void showNotification(JSONObject jsonObject) {

        Log.d(TAG, "Starting process to showing notification");

        String activityClass = "", activityPackage = "";

        Intent intent = new Intent();

        try {

            if (!jsonObject.isNull("id")) {

                intent.putExtra("EXTRA_NOTIFICATION_ID", jsonObject.getString("id"));
            }
            if (!jsonObject.isNull("title")) {

                title = jsonObject.getString("title");
                intent.putExtra("EXTRA_TITLE", title);
            }
            if (!jsonObject.isNull("body")) {

                body = jsonObject.getString("body");
                intent.putExtra("EXTRA_BODY", body);
            }
            if (!jsonObject.isNull("url")) {

                intent.putExtra("EXTRA_URL", jsonObject.getString("url"));
            }
            if (!jsonObject.isNull("act_class")) {

                activityClass = jsonObject.getString("act_class");
                intent.putExtra("act_class", activityClass);
            }
            if (!jsonObject.isNull("act_pkg")) {

                activityPackage = jsonObject.getString("act_pkg");
                intent.putExtra("act_pkg", activityPackage);
            }
            if (!jsonObject.isNull("inngage_data")) {

                JSONArray dataArray = new JSONArray(jsonObject.getString("inngage_data"));
                intent.putExtra("EXTRA_DATA", dataArray.toString());
            }

        } catch (JSONException e) {

            Log.e(TAG, "Error getting JSON field \n" +e);
        }
        if("".equals(activityClass)) {

            Log.e(TAG, "The activity class name not found in message, make sure the setting has been made in Inngage Platform: Configuration > Platform");
            return;
        }
        if("".equals(activityPackage)) {

            Log.e(TAG, "The package name of the application not found in message, make sure the setting has been made in Inngage Platform: Configuration > Platform");
            return;
        }

        intent.setClassName(activityPackage, activityPackage + "." + activityClass);
        Log.d(TAG, "Redirecting user to " + activityPackage + "." + activityClass);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[] { 700, 700})
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notifyID, notificationBuilder.build());
    }
}