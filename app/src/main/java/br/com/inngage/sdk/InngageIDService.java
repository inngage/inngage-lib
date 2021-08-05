//package br.com.inngage.sdk;
//
//import android.support.annotation.NonNull;
//import android.util.Log;
//
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.iid.FirebaseInstanceId;
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.iid.InstanceIdResult;
//
///**
// * Maintained by Mohamed Ali Nakouri on 11/05/21.24/06/17.
// */
//
//public class InngageIDService extends FirebaseInstanceIdService {
//
//    private static final String TAG = "inngage-lib";
//
//    @Override
//    public void onTokenRefresh() {
//        // Get updated InstanceID token.
//        //String refreshedToken = FirebaseInstanceId.getInstance().getToken();
//        FirebaseInstanceId.getInstance().getInstanceId()
//                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
//                        if (!task.isSuccessful()) {
//                            Log.w(TAG, "getInstanceId failed", task.getException());
//                            return;
//                        }
//
//                        // Get new Instance ID token
//                        String refreshedToken = task.getResult().getToken();
//
//                        // Log and toast
//
//                        if(BuildConfig.DEBUG) {
//
//                            Log.d(TAG, "Refreshed token: " + refreshedToken);
//                        }
//                    }
//                });
//
//
//    }
//}