package br.com.inngage.sdk.service;

import static br.com.inngage.sdk.InngageConstants.TAG;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import br.com.inngage.sdk.InngageConstants;
import br.com.inngage.sdk.InngagePushConfig;
import br.com.inngage.sdk.InngageUtils;
import br.com.inngage.sdk.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class PushMessagingService extends FirebaseMessagingService {
    InngagePushConfig config = InngagePushConfig.getInstance();
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
    }
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (!remoteMessage.getData().isEmpty()) {
            try{
                JSONObject json = new JSONObject(remoteMessage.getData());
                Log.i(TAG, "Push received: " + json);
                inngageNotification(json);
            } catch (Exception e){
                Log.e(TAG, "Error parsing push data", e);
            }
        }
    }

    private void inngageNotification(JSONObject jsonPushNotification){
        Intent intent = new Intent();
        String targetActivity = InngagePushConfig.getInstance().getTargetActivity();

        if (targetActivity != null && !targetActivity.isEmpty()) {
            intent.setClassName(getPackageName(), targetActivity);
        } else {
            PackageManagerActivities.getAppActivities(intent, getPackageManager(), getPackageName());
        }
        InngageMessagingService.sendData(getApplicationContext(), intent, jsonPushNotification);
        showNotification(intent, jsonPushNotification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    config.getChannelId(), config.getChannelName(), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(config.getChannelDescription());

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private PendingIntent createPendingIntent(Intent intent, int requestCode) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.getActivity(this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getActivity(this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private void showNotification(Intent intent, JSONObject jsonObject) {
        createNotificationChannel();

        String title = jsonObject.optString("title", "Nova notificação");
        String body = jsonObject.optString("body", "");
        String imageUrl = jsonObject.optString("picture", "");

        int uniqueNotificationId = new Random().nextInt(1000000);
        int requestCode = (int) System.currentTimeMillis();

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, config.getChannelId())
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(config.getSmallIcon());

        // Estilo com imagem (BigPicture)
        try {
            if (!imageUrl.isEmpty()) {
                Bitmap image = new InngageUtils().getBitmapfromUrl(imageUrl);
                if (image != null) {
                    builder.setLargeIcon(image);
                    builder.setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(image)
                            .setSummaryText(body));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Erro ao carregar imagem", e);
        }

        PendingIntent pendingIntent = createPendingIntent(intent, requestCode);
        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(uniqueNotificationId, builder.build());
        }
    }
}
