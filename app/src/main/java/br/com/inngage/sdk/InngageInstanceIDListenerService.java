package br.com.inngage.sdk;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

public class InngageInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = "inngage-lib";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {

        Intent intent = new Intent(this, InngageIntentService.class);
        Log.d(TAG, "onTokenRefresh called..");
        startService(intent);
    }
}
