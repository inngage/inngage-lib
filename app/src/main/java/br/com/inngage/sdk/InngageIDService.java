package br.com.inngage.sdk;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by viniciusdepaula on 24/06/17.
 */

public class InngageIDService extends FirebaseInstanceIdService {

    private static final String TAG = "inngage-lib";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
    }
}