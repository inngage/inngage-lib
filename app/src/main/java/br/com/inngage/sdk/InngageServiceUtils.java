//package br.com.inngage.sdk;
//
//import android.content.Context;
//import android.content.Intent;
//import android.util.Log;
//
//import java.util.Arrays;
//import java.util.List;
//
//import static br.com.inngage.sdk.InngageConstants.INVALID_APP_TOKEN_LENGHT;
//import static br.com.inngage.sdk.InngageConstants.INVALID_DISPLACEMENT;
//import static br.com.inngage.sdk.InngageConstants.INVALID_PRIORITY_ACCURACY;
//import static br.com.inngage.sdk.InngageConstants.INVALID_UPDATE_INTERVAL;
//
///**
// * Maintained by Mohamed Ali Nakouri on 11/05/21.04/11/17.
// */
//
//public class InngageServiceUtils {
//
//    private static Context context;
//    //public Context context;
//
//    private static final String TAG = InngageConstants.TAG;
//
//    public InngageServiceUtils(Context context) {
//
//        this.context = context;
//    }
//
//    public InngageServiceUtils() {
//
//        super();
//    }
//
//    public static void startService(int updateInterval, int priorityAccuracy, int displacement, String appToken) {
//
//        Integer[] acceptPriorities = new Integer[]{100, 102, 104, 105};
//        List<Integer> list = Arrays.asList(acceptPriorities);
//
//        if (updateInterval < 0) {
//            Log.e(TAG, INVALID_UPDATE_INTERVAL);
//            return;
//        }
//        if (!list.contains(priorityAccuracy)) {
//            Log.e(TAG, INVALID_PRIORITY_ACCURACY);
//            return;
//        }
//        if (displacement < 0) {
//            Log.e(TAG, INVALID_DISPLACEMENT);
//            return;
//        }
//        if (appToken.length() < 30) {
//            Log.e(TAG, INVALID_APP_TOKEN_LENGHT);
//            return;
//        }
//        Intent intent = new Intent(context, InngageLocationService.class);
//        intent.putExtra("UPDATE_INTERVAL", updateInterval);
//        intent.putExtra("PRIORITY_ACCURACY", priorityAccuracy);
//        intent.putExtra("DISPLACEMENT", displacement);
//        intent.putExtra("APP_TOKEN", appToken);
//        context.startService(intent);
//    }
//
//    public void startService() {
//
//        Intent intent = new Intent(context, InngageLocationService.class);
//        intent.putExtra("UPDATE_INTERVAL", 30000);
//        intent.putExtra("PRIORITY_ACCURACY", 104);
//        intent.putExtra("DISPLACEMENT", 1000);
//        context.startService(intent);
//    }
//}