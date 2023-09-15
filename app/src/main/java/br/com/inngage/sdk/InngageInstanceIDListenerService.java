package br.com.inngage.sdk;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;

public class InngageInstanceIDListenerService extends FirebaseMessagingService {
    private static final String TAG = "inngage-lib";
    public void onNewToken(String token) {
        Intent intent = new Intent(this, InngageIntentService.class);
        Log.d(TAG, "onTokenRefresh called..");
        startService(intent);
    }
}
