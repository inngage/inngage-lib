package br.com.inngage.sdk;

public final class InngageConstants {
    public static final String PLATFORM = "android";
    public static final String SDK = "1";
    // Endpoints
    public static final String API_ENDPOINT = "https://apid.inngage.com.br/v1";
    public static final String API_DEV_ENDPOINT = "https://apid.inngage.com.br/v1";
    public static final String API_PROD_ENDPOINT = "https://api.inngage.com.br/v1";
    // Paths
    public static final String PATH_SUBSCRIPTION = "/subscription/";
    public static final String PATH_GEOLOCATION = "/geolocation/";
    public static final String PATH_NOTIFICATION_CALLBACK = "/notification/";
    // Log tag
    public static final String TAG = "inngage-lib";
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

    // In App
    static final String IN_APP_MESSAGE = "inapp_message";
    static final String TITLE_IN_APP = "title";
    static final String BODY_IN_APP = "body";
    static final String TITLE_FONT_COLOR = "title_font_color";
    static final String BODY_FONT_COLOR = "body_font_color";
    static final String BACKGROUND_COLOR = "background_color";
    static final String BTN_LEFT_TXT_COLOR = "btn_left_txt_color";
    static final String BTN_LEFT_BG_COLOR = "btn_left_bg_color";
    static final String BTN_RIGHT_TXT_COLOR = "btn_right_txt_color";
    static final String BTN_RIGHT_BG_COLOR = "btn_right_bg_color";
    static final String BACKGROUND_IMAGE = "background_image";
    static final String BTN_LEFT_TXT = "btn_left_txt";
    static final String BTN_LEFT_ACTION_TYPE = "btn_left_action_type";
    static final String BTN_LEFT_ACTION_LINK = "btn_left_action_link";
    static final String BTN_RIGHT_TXT = "btn_right_txt";
    static final String BTN_RIGHT_ACTION_TYPE = "btn_right_action-type";
    static final String BTN_RIGHT_ACTION_LINK = "btn_right_action_link";
    static final String RICH_CONTENT = "rich_content";
    static final String IMPRESSION = "inpression";
    static final String BACKGROUND_IMG_ACTION_TYPE = "bg_img_action_type";
    static final String BACKGROUND_IMG_ACTION_LINK = "bg_img_action_link";

    public static String[] keys = {
            TITLE_IN_APP,
            BODY_IN_APP,
            IN_APP_MESSAGE,
            TITLE_FONT_COLOR,
            BODY_FONT_COLOR,
            BACKGROUND_COLOR,
            BTN_LEFT_TXT_COLOR,
            BTN_LEFT_BG_COLOR,
            BTN_RIGHT_TXT_COLOR,
            BTN_RIGHT_BG_COLOR,
            BACKGROUND_IMAGE,
            BTN_LEFT_TXT,
            BTN_LEFT_ACTION_TYPE,
            BTN_LEFT_ACTION_LINK,
            BTN_RIGHT_TXT,
            BTN_RIGHT_ACTION_TYPE,
            BTN_RIGHT_ACTION_LINK,
            RICH_CONTENT,
            IMPRESSION,
            BACKGROUND_IMG_ACTION_TYPE,
            BACKGROUND_IMG_ACTION_LINK
    };
}