package br.com.inngage.sdk;


import com.google.android.gms.location.LocationRequest;

/**
 * Maintained by Mohamed Ali Nakouri on 11/05/21.
 */

interface ILocationConstants {

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    int UPDATE_INTERVAL_IN_MILLISECONDS = 12000;

    /**
     * If accuracy is lesser than 100m , discard it
     */
    int ACCURACY_THRESHOLD = 100;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    int DISPLACEMENT = 200;

    int PRIORITY_ACCURACY = LocationRequest.PRIORITY_LOW_POWER;

    /**
     * Broadcast Receiver Action to update location
     */
    String LOACTION_ACTION = "br.com.inngage.tutorialfcm.LOCATION_ACTION";

    /**
     * Message key for data with in the broadcast
     */
    String LOCATION_MESSAGE = "br.com.inngage.tutorialfcm.LOCATION_DATA";


    /***
     * Request code while asking for permissions
     */
    int PERMISSION_ACCESS_LOCATION_CODE = 99;

}

interface IPreferenceConstants {

    String PREF_DISTANCE = "distance";
    String PREF_UPDATE_INTERVAL = "updateInterval";
    String PREF_PRIORITY_ACCURACY = "priorityAccuracy";
    String PREF_DISPLACEMENT = "displacement";
    String PREF_APP_TOKEN = "appToken";
    String PREF_DEVICE_UUID = "deviceUUID";
    String PREF_INNGAGE_ENV = "inngageEnvironment";

}