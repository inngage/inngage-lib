package br.com.inngage.sdk;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class PushMessagingService extends FirebaseMessagingService {
    private static final String TAG = "inngage-lib";
    final String CHANNEL = "CH01";
    Random random = new Random();
    int notifyID = random.nextInt(9999 - 1000) + 1000;
    String body, title = null;
    JSONObject jsonObject;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            jsonObject = parseRemoteMessageToJson(remoteMessage);
            Log.d(TAG, "Push received from " + remoteMessage.getFrom());
            startInngage(jsonObject);
        }
    }

    private JSONObject parseRemoteMessageToJson(RemoteMessage remoteMessage) {
        Map<String, String> params = remoteMessage.getData();
        jsonObject = new JSONObject(params);
        Log.d("Notification JSON:", jsonObject.toString());
        return jsonObject;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Your_channel";
            String description = "Your_channel_desc";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            try {
                notificationManager.createNotificationChannel(channel);
            } catch (Exception e) {
                Log.d(TAG, "createNotificationChannel Exception: " + e);
            }
        }
    }

    private void startInngage(JSONObject jsonObject){
        Intent intent = new Intent();
        if (!jsonObject.isNull("additional_data")) {
            Log.d(TAG, "Data JSON InApp:" + jsonObject);
            getInAppData(jsonObject, intent);
            new InAppUtils().getIntentFromService(getApplicationContext(), intent);
        } else {
            showNotification(jsonObject);
        }
    }

    private void showNotification(JSONObject jsonObject) {
        Log.d(TAG, "Starting process to showing notification");
        PendingIntent pendingIntent;
        String bigPicture = "", activityPackage = "";

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
            if (!jsonObject.isNull("type")) {
                intent.putExtra("type", jsonObject.getString("type"));
            }
            if (!jsonObject.isNull("picture")) {
                bigPicture = jsonObject.getString("picture");
                Log.d(TAG, "We Have a IMAGE : " + bigPicture);
                intent.putExtra("big_picture", bigPicture);
            }
            if (!jsonObject.isNull("act_pkg")){
                activityPackage = jsonObject.getString("act_pkg");
                intent.putExtra("act_pkg", activityPackage);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting JSON field \n" + e);
        }

        try {
            ArrayList<String> activities = packageManagerActivities(activityPackage);
            for (String activityName : activities) {
                if (activityName.equals(activityPackage + ".MainActivity")) {
                    intent.setClassName(activityPackage, activityName);
                }
            }
            Log.d(TAG, "Adding Flags to the Pending Intent ");
            int requestID = (int) System.currentTimeMillis();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(this, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(this, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            Log.d(TAG, "pending intent : " + pendingIntent.toString());

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            createNotificationChannel();
            Log.d(TAG, "Notification Channel Created : " + CHANNEL);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL);
            //builder.setSmallIcon(R.mipmap.ic_launcher);
            int id = this.getResources().getIdentifier("ic_notification", "drawable", this.getPackageName());

            if (id != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setSmallIcon(id);
                builder.setColor(ContextCompat.getColor(getApplicationContext(), android.R.color.transparent));
            } else {
                int id2 = this.getResources().getIdentifier("ic_launcher", "mipmap", this.getPackageName());
                builder.setSmallIcon(id2);
            }
            if (!"".equals(bigPicture) && bigPicture != null) {
                InngageUtils utils = new InngageUtils();
                Bitmap image = utils.getBitmapfromUrl(bigPicture);

                if (image != null) {
                    builder.setLargeIcon(image);
                    builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(image).setSummaryText(body));

                    Log.d(TAG, "Notification has BigPictureStyle");
                }
            }

            builder.setContentTitle(title);
            builder.setContentText(body);
            builder.setPriority(NotificationCompat.PRIORITY_MAX);
            builder.setAutoCancel(true);
            builder.setSound(defaultSoundUri);
            builder.setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            notificationManagerCompat.notify(notifyID, builder.build());
        } catch (Exception e) {
            Log.d(TAG, "Push intent open error");
        }
    }

    private ArrayList<String> packageManagerActivities(String packageName){
        PackageManager packageManager = getPackageManager();
        ArrayList<String> activitiesNames = new ArrayList<>();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            ActivityInfo[] activities = packageInfo.activities;
            if (activities != null) {
                for (ActivityInfo activityInfo : activities) {
                    String activityName = activityInfo.name;
                    activitiesNames.add(activityName);
                }
            }
        } catch (PackageManager.NameNotFoundException e){
            Log.d(TAG, "" + e);
        }
        return activitiesNames;
    }

    private void getInAppData(JSONObject jsonObject, Intent intent) {
        try {
            String additionalDataString = jsonObject.getString("additional_data");
            ExtraData.putDataToIntent(additionalDataString, intent);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
