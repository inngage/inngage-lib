package br.com.inngage.sdk;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class InngageInstanceIDListenerService extends FirebaseInstanceIdService {
    private static final String TAG = "inngage-lib";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    // [START refresh_token]
    public void onNewToken(String token) {
        Intent intent = new Intent(this, InngageIntentService.class);
        Log.d(TAG, "onTokenRefresh called..");
        startService(intent);
    }
}
