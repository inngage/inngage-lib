package br.com.inngage.sdk;

public final class InngageConstants {

    public static final String PLATFORM = "android";
    public static final String SDK = "4.2.0";
    public static final String TAG = "inngage-lib";
    // Endpoints
    public static final String API_ENDPOINT = "https://apid.inngage.com.br/v1";
    public static final String API_DEV_ENDPOINT = "https://apid.inngage.com.br/v1";
    public static final String API_PROD_ENDPOINT = "https://api.inngage.com.br/v1";
    public static final String API_PROD_ENDPOINT_V3 = "https://api.inngage.com.br/v1";
    // Paths
    public static final String PATH_SUBSCRIPTION = "/subscription/";
    public static final String PATH_GEOLOCATION = "/geolocation/";
    public static final String PATH_NOTIFICATION_CALLBACK = "/notification/";
    // Log tag
    public static final String TAG_FIREBASE = "Inngage-FirebaseService";
    public static final String TAG_NOTIFY = "Inngage-Notify";
    public static final String TAG_INAPP = "Inngage-InApp";
    public static final String TAG_ERROR = "Inngage-Notify-Error";

    public static final String SUBSCRIPTION = "SUBSCRIPTION";
    public static final String GEOLOCATION = "GEOLOCATION";
    public static final String NOTIFICATION_CALLBACK = "NOTIFICATION_CALLBACK";

    public static final String INNGAGE_DEV_ENV = "dev";
    public static final String INNGAGE_PROD_ENV = "prod";
    public static final String GCM_PLATFORM = "GCM";
    public static final String FCM_PLATFORM = "FCM";
    public static final String ACTION_REGISTRATION = "br.com.inngage.action.REGISTRATION";

    // Extras
    public static final String EXTRA_PROV = "PROVIDER";
    public static final String EXTRA_ENV = "ENVIRONMENT";
    public static final String EXTRA_TOKEN = "APP_TOKEN";
    public static final String EXTRA_IDENTIFIER = "IDENTIFIER";
    public static final String EXTRA_CUSTOM_FIELD = "CUSTOM_FIELDS";
    public static final String EXTRA_EMAIL = "EMAIL";
    public static final String EXTRA_PHONE = "PHONE_NUMBER";

    // Messages
    public static final String INVALID_APP_TOKEN = "Verify if the value of APP_TOKEN was informed";
    public static final String INVALID_ENVIRONMENT = "Verify if the value of ENVIRONMENT was informed";
    public static final String INVALID_PROVIDER = "Verify if the value of PROVIDER was informed";
    public static final String INVALID_IDENTIFIER = "Verify if the value of IDENTIFIER was informed";
    public static final String INVALID_CUSTOM_FIELD = "Verify if the value of CUSTOM_FIELD was informed";
    public static final String INVALID_UPDATE_INTERVAL = "Error starting location service: verify if the value of updateInterval is a integer";
    public static final String INVALID_PRIORITY_ACCURACY = "Error starting location service: verify if the value of updateInterval is valid (100, 102, 104 or 105)";
    public static final String INVALID_DISPLACEMENT = "Error starting location service: verify if the value of displacement is valid";
    public static final String INVALID_APP_TOKEN_LENGHT = "Verify if the value of APP_TOKEN is correct";
    public static final String UNABLE_FIND_LOCATION = "Não foi possível obter a sua localização";

    // NOTIFICATIONS
    static final String notId = "notId";
    static final String id = "id";
    static final String title = "title";
    static final String body = "body";
    static final String type = "type";
    static final String url = "url";
    static final String picture = "picture";
    static final String actPackage = "act_pkg";

    public static String[] keys = {
            notId,
            id,
            title,
            body,
            type,
            url,
            picture,
            actPackage,
    };
}