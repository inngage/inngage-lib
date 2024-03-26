package br.com.inngage.sdk;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;

public class InngageInstanceIDListenerService extends FirebaseMessagingService {
    public void onNewToken(String token) {
        Intent intent = new Intent(this, InngageService.class);
        Log.d(InngageConstants.TAG_FIREBASE, "onTokenRefresh called..");
        startService(intent);
    }
}
