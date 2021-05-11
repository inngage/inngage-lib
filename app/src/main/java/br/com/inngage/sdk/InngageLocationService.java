//package br.com.inngage.sdk;
//
//import android.Manifest;
//import android.app.AlertDialog;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.location.Criteria;
//import android.location.Location;
//
//import android.location.LocationManager;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v4.content.PermissionChecker;
//import android.util.Log;
//import android.widget.Toast;
//
////import androidx.core.content.ContextCompat;
////import androidx.core.content.PermissionChecker;
//
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesUtil;
//import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationListener;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.android.gms.tasks.Task;
//import com.nabinbhandari.android.permissions.PermissionHandler;
//import com.nabinbhandari.android.permissions.Permissions;
//
//import org.json.JSONObject;
//
//import java.text.DateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//
//import static br.com.inngage.sdk.IPreferenceConstants.PREF_APP_TOKEN;
//import static br.com.inngage.sdk.IPreferenceConstants.PREF_DISPLACEMENT;
//import static br.com.inngage.sdk.IPreferenceConstants.PREF_DISTANCE;
//import static br.com.inngage.sdk.IPreferenceConstants.PREF_PRIORITY_ACCURACY;
//import static br.com.inngage.sdk.IPreferenceConstants.PREF_UPDATE_INTERVAL;
//import static br.com.inngage.sdk.InngageConstants.API_DEV_ENDPOINT;
//import static br.com.inngage.sdk.InngageConstants.API_PROD_ENDPOINT;
//import static br.com.inngage.sdk.InngageConstants.INNGAGE_DEV_ENV;
//import static br.com.inngage.sdk.InngageConstants.PATH_GEOLOCATION;
//import static br.com.inngage.sdk.InngageConstants.UNABLE_FIND_LOCATION;
//
///**
// * Maintained by Mohamed Ali Nakouri on 11/05/21.29/10/17.
// */
////public class InngageLocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ILocationConstants, IPreferenceConstants {
//public class InngageLocationService extends Service implements  ILocationConstants, IPreferenceConstants  {
//
//    private static final String TAG = InngageConstants.TAG;
//
//    /**
//     * Provides the entry point to Google Play services.
//     */
//    //protected GoogleApiClient mGoogleApiClient;
//
//    /**
//     * Stores parameters for requests to the FusedLocationProviderApi.
//     */
//    //protected LocationRequest mLocationRequest;
//
//    /**
//     * Represents a geographical location.
//     */
//    protected Location mCurrentLocation;
//
//    private String mLatitudeLabel;
//    private String mLongitudeLabel;
//    private String mLastUpdateTimeLabel;
//    private String mDistance;
//    private int updateInterval;
//    private int priorityAccuracy;
//    private int displacement;
//    private String appToken;
//    private String deviceUUID;
//    private String inngageEnvironment;
//    JSONObject jsonBody, jsonObj;
//
//    /**
//     * Time when the location was updated represented as a String.
//     */
//    protected String mLastUpdateTime;
//
//    private Location oldLocation;
//
//    private Location newLocation;
//    private Location location;
//    private Location lat;
//    private Location lon;
//
//    private AppPreferences appPreferences;
//
//    /**
//     * Total distance covered
//     */
//    private float distance;
//
//    FusedLocationProviderClient fusedLocationProviderClient ;
//    private LocationRequest locationRequest;
//
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//
//
//
//
//        appPreferences = new AppPreferences(this);
//
//        oldLocation = new Location("Point A");
//        newLocation = new Location("Point B");
//        mLatitudeLabel = "Latitudde";
//        mLongitudeLabel = "Longitude";
//        mLastUpdateTimeLabel = "Uptime";
//        mDistance = "Distance";
//        mLastUpdateTime = "";
//        distance = appPreferences.getFloat(PREF_DISTANCE, 0);
//
//        if (BuildConfig.DEBUG) {
//
//            Log.d(TAG, "onCreate Distance: " + distance);
//        }
//
//    }
//    public void  requestLocationUpdate(int updateInterval, int priorityAccuracy, int displacement) {
//                if(BuildConfig.DEBUG) {
//
//                    Log.d(TAG, "------------> Creating location request");
//                                         }
//        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED)
//        {
//        fusedLocationProviderClient = new FusedLocationProviderClient(this);
//        locationRequest = new LocationRequest();
//        locationRequest.setPriority(priorityAccuracy);
//        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
//        locationRequest.setInterval(updateInterval);
//        locationRequest.setSmallestDisplacement(displacement);
//            try {
//
//            if(BuildConfig.DEBUG) {
//
//                Log.d(TAG, "startLocationUpdates - requestLocationUpdates");
//            }
//        fusedLocationProviderClient.requestLocationUpdates(locationRequest,new LocationCallback(){
//
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                super.onLocationResult(locationResult);
//                mCurrentLocation = locationResult.getLastLocation();
//                Log.d(TAG,"--------Lat : "+ locationResult.getLastLocation().getLatitude());
//                Log.d(TAG,"--------Lon : "+ locationResult.getLastLocation().getLongitude());
//                onLocationChanged();
//            }
//        },getMainLooper());
//            } catch (SecurityException ex) {
//
//            ex.printStackTrace();
//        }
//
//        } else callPermissions();
//    }
//    public void callPermissions(){
//        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
//        Permissions.check(getBaseContext(), permissions, "Location Permissions are required", new Permissions.Options().setSettingsDialogTitle("Warning !"
//        ).setRationaleDialogTitle("Info"), new PermissionHandler() {
//            @Override
//            public void onGranted() {
//                requestLocationUpdate(updateInterval, priorityAccuracy, displacement);
//            }
//
//            @Override
//            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
//                super.onDenied(context, deniedPermissions);
//                callPermissions();
//            }
//        });
//    }
//
//       @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//
//        if (intent != null) {
//
//            Bundle extras = intent.getExtras();
//
//            if (extras != null) {
//
//                if (extras.containsKey("UPDATE_INTERVAL")) {
//
//                    updateInterval = extras.getInt("UPDATE_INTERVAL", 60000);
//                    appPreferences.putInt(PREF_UPDATE_INTERVAL, updateInterval);
//                }
//                if (extras.containsKey("PRIORITY_ACCURACY")) {
//
//                    priorityAccuracy = extras.getInt("PRIORITY_ACCURACY", 104);
//                    appPreferences.putInt(PREF_PRIORITY_ACCURACY, priorityAccuracy);
//                }
//                if (extras.containsKey("DISPLACEMENT")) {
//
//                    displacement = extras.getInt("DISPLACEMENT", 250);
//                    appPreferences.putInt(PREF_DISPLACEMENT, displacement);
//                }
//                if (extras.containsKey("APP_TOKEN")) {
//
//                    appToken = extras.getString("APP_TOKEN");
//                    appPreferences.putString(PREF_APP_TOKEN, appToken);
//                }
//            }
//
//        } else {
//
//            Log.d(TAG, "No extras loaded, shared pref will be used");
//            updateInterval = appPreferences.getInt(PREF_UPDATE_INTERVAL, 60000);
//            priorityAccuracy = appPreferences.getInt(PREF_PRIORITY_ACCURACY, 104);
//            displacement = appPreferences.getInt(PREF_DISPLACEMENT, 250);
//            appToken = appPreferences.getString(PREF_APP_TOKEN, "");
//        }
//
//        if(BuildConfig.DEBUG) {
//
//            Log.d(TAG, "Location parameters loaded (updateInterval: " + updateInterval + " priorityAccuracy: " + priorityAccuracy + " displacement: " + displacement + ")");
//        }
//
//        requestLocationUpdate(updateInterval, priorityAccuracy, displacement);
//
//
//
//
//
//        return Service.START_STICKY;
//    }
//
// //   @Override
////    public int onStartCommand(Intent intent, int flags, int startId) {
////
////        if (intent != null) {
////
////            Bundle extras = intent.getExtras();
////
////            if (extras != null) {
////
////                if (extras.containsKey("UPDATE_INTERVAL")) {
////
////                    updateInterval = extras.getInt("UPDATE_INTERVAL", 60000);
////                    appPreferences.putInt(PREF_UPDATE_INTERVAL, updateInterval);
////                }
////                if (extras.containsKey("PRIORITY_ACCURACY")) {
////
////                    priorityAccuracy = extras.getInt("PRIORITY_ACCURACY", 104);
////                    appPreferences.putInt(PREF_PRIORITY_ACCURACY, priorityAccuracy);
////                }
////                if (extras.containsKey("DISPLACEMENT")) {
////
////                    displacement = extras.getInt("DISPLACEMENT", 250);
////                    appPreferences.putInt(PREF_DISPLACEMENT, displacement);
////                }
////                if (extras.containsKey("APP_TOKEN")) {
////
////                    appToken = extras.getString("APP_TOKEN");
////                    appPreferences.putString(PREF_APP_TOKEN, appToken);
////                }
////            }
////
////        } else {
////
////            Log.d(TAG, "No extras loaded, shared pref will be used");
////            updateInterval = appPreferences.getInt(PREF_UPDATE_INTERVAL, 60000);
////            priorityAccuracy = appPreferences.getInt(PREF_PRIORITY_ACCURACY, 104);
////            displacement = appPreferences.getInt(PREF_DISPLACEMENT, 250);
////            appToken = appPreferences.getString(PREF_APP_TOKEN, "");
////        }
////
////        if(BuildConfig.DEBUG) {
////
////            Log.d(TAG, "Location parameters loaded (updateInterval: " + updateInterval + " priorityAccuracy: " + priorityAccuracy + " displacement: " + displacement + ")");
////        }
////
////        //buildGoogleApiClient(updateInterval, priorityAccuracy, displacement);
////
////        mGoogleApiClient.connect();
////
////        if (mGoogleApiClient.isConnected()) {
////
////            if(BuildConfig.DEBUG) {
////
////                Log.d(TAG, "mGoogleApiClient isConnected");
////            }
////            //startLocationUpdates();
////            Log.d(TAG, "startLocationUpdates STARTED");
////        }
////        else
////        {
////            Log.d(TAG, "startLocationUpdates  DIDN'T START");
////
////        }
////
////        return Service.START_STICKY;
////    }
//
////    /**
////     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
////     * LocationServices API.
////     */
////    protected synchronized void buildGoogleApiClient(int updateInterval, int priorityAccuracy, int displacement) {
////        Log.d(TAG, "---Entering buildGoogleApiClient----");
////        if(BuildConfig.DEBUG) {
////
////            Log.d(TAG, "Building Google API Client");
////        }
////        Log.d(TAG, "---Starting to Build Google API Client");
////        try
////        {
////            mGoogleApiClient = new GoogleApiClient.Builder(this)
////                    .addConnectionCallbacks(this)
////                    .addOnConnectionFailedListener(this)
////                    .addApi(LocationServices.API)
////                    .build();
////        }
////        catch (Exception ex) {
////
////            ex.printStackTrace();
////        }
////        /*mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
////                .addApi(LocationServices.API)
////                .addConnectionCallbacks(this)
////                .addOnConnectionFailedListener(this)
////                .build();*/
////
////
////        createLocationRequest(updateInterval, priorityAccuracy, displacement);
////    }
////
////    protected void createLocationRequest(int updateInterval, int priorityAccuracy, int displacement) {
////
////        if(BuildConfig.DEBUG) {
////
////            Log.d(TAG, "Creating location request");
////        }
////
////        mLocationRequest = new LocationRequest();
////
////        mLocationRequest.setInterval(updateInterval);
////
////        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
////
////        mLocationRequest.setPriority(priorityAccuracy);
////
////        mLocationRequest.setSmallestDisplacement(displacement);
////    }
////
////    /**
////     * Requests location updates from the FusedLocationApi.
////     */
////    protected void startLocationUpdates() {
////
////        try {
////
////            if(BuildConfig.DEBUG) {
////
////                Log.d(TAG, "startLocationUpdates - requestLocationUpdates");
////            }
////
////            LocationServices.FusedLocationApi.requestLocationUpdates(
////                    mGoogleApiClient,
////                    mLocationRequest,
////                    this);
////
////
////
////        } catch (SecurityException ex) {
////
////            ex.printStackTrace();
////        }
////    }
////
//    /**
//     * Updates the latitude, the longitude, and the last location time in the UI.
//     */
//    private void updateUI() {
//
//        InngageUtils utils = new InngageUtils();
//
//        if (null != mCurrentLocation) {
//
//            StringBuilder sbLocationData = new StringBuilder();
//            sbLocationData.append(mLatitudeLabel)
//                    .append(" ")
//                    .append(mCurrentLocation.getLatitude())
//                    .append("\n")
//                    .append(mLongitudeLabel)
//                    .append(" ")
//                    .append(mCurrentLocation.getLongitude())
//                    .append("\n")
//                    .append(mLastUpdateTimeLabel)
//                    .append(" ")
//                    .append(mLastUpdateTime)
//                    .append("\n")
//                    .append(mDistance)
//                    .append(" ")
//                    .append(getUpdatedDistance())
//                    .append(" meters");
//            /*
//             * update preference with latest value of distance
//             */
//            appPreferences.putFloat(PREF_DISTANCE, distance);
//
//            if(BuildConfig.DEBUG) {
//
//                Log.d(TAG, "Location Data:\n" + sbLocationData.toString());
//            }
//
//            appToken = appPreferences.getString(PREF_APP_TOKEN, "");
//            deviceUUID = appPreferences.getString(PREF_DEVICE_UUID, "");
//            inngageEnvironment = appPreferences.getString(PREF_INNGAGE_ENV, "");
//
//            if("".equals(deviceUUID)) {
//
//                Log.d(TAG, "Device UUID not found");
//            }
//
//            if(!"".equals(deviceUUID) && !"".equals(appToken)) {
//
//                jsonBody = utils.createLocationRequest(
//                        deviceUUID,
//                        mCurrentLocation.getLatitude(),
//                        mCurrentLocation.getLongitude(),
//                        appToken);
//
//                String endpoint = INNGAGE_DEV_ENV.equals(inngageEnvironment) ? API_DEV_ENDPOINT : API_PROD_ENDPOINT;
//
//                utils.doPost(jsonBody, endpoint + PATH_GEOLOCATION);
//            }
//
//        } else {
//
//            Toast.makeText(this, UNABLE_FIND_LOCATION, Toast.LENGTH_SHORT).show();
//        }
//    }
////
////    /**
////     * Removes location updates from the FusedLocationApi.
////     */
////    protected void stopLocationUpdates() {
////
////        Log.d(TAG, "stopLocationUpdates()");
////
////        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
////
////    }
////
////    @Override
////    public void onDestroy() {
////
////        Log.d(TAG, "onDestroy()");
////
////        appPreferences.putFloat(PREF_DISTANCE, distance);
////
////        stopLocationUpdates();
////
////        mGoogleApiClient.disconnect();
////
////        Log.d(TAG, "onDestroy Distance " + distance);
////
////        super.onDestroy();
////    }
////
////    /**
////     * Runs when a GoogleApiClient object successfully connects.
////     */
////    @Override
////    public void onConnected(Bundle connectionHint) throws SecurityException {
////
////        if(BuildConfig.DEBUG) {
////
////            Log.i(TAG, "Connected to GoogleApiClient");
////        }
////
////        if (mCurrentLocation == null) {
////            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
////            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
////
////        }
////
////        if (mGoogleApiClient != null) {
////            mGoogleApiClient.connect();
////            startLocationUpdates();
////        }
////        else
////        {
////            Log.d(TAG, "mGoogleApiClient houa lmochkol");
////            AlertDialog.Builder builder = new AlertDialog.Builder(this);
////            builder.setMessage("ma connectech")
////                    .setTitle("mGoogleApiClient");
////
////            AlertDialog alert =builder.create();
////            alert.show();
////        }
////
////    }
////
////    /**
////     * Callback that fires when the location changes.
////     */
////    @Override
//    public void onLocationChanged() {
//
//        if(BuildConfig.DEBUG) {
//
//            Log.i(TAG, "onLocationChanged()");
//        }
//
////        mCurrentLocation = location;
////        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
//        updateUI();
//
//    }
////
////    @Override
////    public void onConnectionSuspended(int cause) {
////
////        Log.i(TAG, "onConnectionSuspended()");
////
////        mGoogleApiClient.connect();
////    }
////
////    @Override
////    public void onConnectionFailed(ConnectionResult result) {
////
////        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
////
////    }
////
//    private float getUpdatedDistance() {
//
//        /**
//         * There is 68% chance that user is with in 100m from this location.
//         * So neglect location updates with poor accuracy
//         */
//
//
//        if (mCurrentLocation.getAccuracy() > ACCURACY_THRESHOLD) {
//
//            return distance;
//        }
//
//
//        if (oldLocation.getLatitude() == 0 && oldLocation.getLongitude() == 0) {
//
//            oldLocation.setLatitude(mCurrentLocation.getLatitude());
//            oldLocation.setLongitude(mCurrentLocation.getLongitude());
//            newLocation.setLatitude(mCurrentLocation.getLatitude());
//            newLocation.setLongitude(mCurrentLocation.getLongitude());
//            return distance;
//
//        } else {
//
//            oldLocation.setLatitude(newLocation.getLatitude());
//            oldLocation.setLongitude(newLocation.getLongitude());
//
//            newLocation.setLatitude(mCurrentLocation.getLatitude());
//            newLocation.setLongitude(mCurrentLocation.getLongitude());
//
//        }
//
//        /**
//         * Calculate distance between last two geo locations
//         */
//        distance += newLocation.distanceTo(oldLocation);
//
//        return distance;
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        throw new UnsupportedOperationException("Not yet implemented");
//    }
//
//
//
//}