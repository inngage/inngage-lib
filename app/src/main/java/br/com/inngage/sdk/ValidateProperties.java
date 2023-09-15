package br.com.inngage.sdk;

import org.json.JSONObject;

import java.util.Arrays;

public class ValidateProperties {
    public static boolean validateAppToken(String appToken) {
        if ("".equals(appToken) || appToken.length() < 8) {
            return false;
        }
        return true;
    }
    public static boolean validateEnvironment(String env) {
        String[] environments = {InngageConstants.INNGAGE_DEV_ENV, InngageConstants.INNGAGE_PROD_ENV};

        if ("".equals(env) || !Arrays.asList(environments).contains(env)) {
            return false;
        }
        return true;
    }
    public static boolean validateProvider(String provider) {
        String[] providers = {InngageConstants.FCM_PLATFORM, InngageConstants.GCM_PLATFORM};

        if ("".equals(provider) || !Arrays.asList(providers).contains(provider)) {
            return false;
        }
        return true;
    }
    public static boolean validateIdentifier(String identifier) {
        if ("".equals(identifier)) {
            return false;
        }
        return true;
    }
    public static boolean validateCustomField(JSONObject customFields) {
        if (customFields.length() == 0) {
            return false;
        }
        return true;
    }
}
