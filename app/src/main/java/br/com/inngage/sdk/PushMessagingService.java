package br.com.inngage.sdk;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
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

import br.com.inngage.sdk.service.InngageMessagingService;
import br.com.inngage.sdk.service.PackageManagerActivities;

public class PushMessagingService extends FirebaseMessagingService {
    final String CHANNEL = "CH01";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> remoteMessageData = remoteMessage.getData();
            JSONObject remoteMessageJson = new JSONObject(remoteMessageData);

            inngageNotification(remoteMessageJson);
        }
    }

    private void inngageNotification(JSONObject jsonPushNotification){
        Intent intent = new Intent();

        InngageMessagingService.sendData(
                getApplicationContext(),
                intent,
                jsonPushNotification);

        showNotification(intent, jsonPushNotification);
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
                Log.e(InngageConstants.TAG_ERROR, "createNotificationChannel Exception: " + e);
            }
        }
    }

    private void showNotification(Intent intent, JSONObject jsonObject) {
        Random random = new Random();
        int uniqueNotificationId = random.nextInt(1000000);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        int requestId = (int) System.currentTimeMillis();

        createNotificationChannel();

        PackageManagerActivities.getAppActivities(intent, getPackageManager(), jsonObject);

        try{
            Bitmap image = new InngageUtils().getBitmapfromUrl((String) jsonObject.get("picture"));

            if (image != null) {
                builder.setLargeIcon(image);
                builder.setStyle(
                        new NotificationCompat.BigPictureStyle()
                                .bigPicture(image)
                                .setSummaryText((CharSequence) jsonObject.get("body")));
            }

            int id = this.getResources().getIdentifier("ic_notification", "drawable", this.getPackageName());

            if (id != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setSmallIcon(id);
                builder.setColor(ContextCompat.getColor(getApplicationContext(), android.R.color.transparent));
            } else {
                int id2 = this.getResources().getIdentifier("ic_launcher", "mipmap", this.getPackageName());
                builder.setSmallIcon(id2);
            }

            builder.setContentTitle((CharSequence) jsonObject.get("title"));
            builder.setContentText((CharSequence) jsonObject.get("body"));
//            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setPriority(NotificationCompat.PRIORITY_MAX);
            builder.setAutoCancel(true);
            builder.setSound(defaultSoundUri);

            PendingIntent pendingIntent;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(this, requestId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(this, requestId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            builder.setContentIntent(pendingIntent);
            notificationManager.notify(uniqueNotificationId, builder.build());
        } catch (JSONException e){
            Log.e(InngageConstants.TAG_ERROR, "Json data in push notification: " + e);
        }
    }
}
