package br.com.inngage.sdk;

import androidx.annotation.DrawableRes;

public class InngagePushConfig {

    private static InngagePushConfig instance;

    private String channelId = "default_channel_id";
    private String channelName = "Notificações";
    private String channelDescription = "Canal padrão de notificações";
    private String targetActivityClassName;
    private int smallIconRes = android.R.drawable.ic_dialog_info; // fallback
    private int notificationColor = -1;


    private InngagePushConfig() {}

    public static InngagePushConfig getInstance() {
        if (instance == null) {
            instance = new InngagePushConfig();
        }
        return instance;
    }

    public InngagePushConfig setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public InngagePushConfig setChannelName(String name) {
        this.channelName = name;
        return this;
    }

    public InngagePushConfig setChannelDescription(String desc) {
        this.channelDescription = desc;
        return this;
    }

    public InngagePushConfig setSmallIcon(@DrawableRes int iconRes) {
        this.smallIconRes = iconRes;
        return this;
    }

    public InngagePushConfig setNotificationColor(int color) {
        this.notificationColor = color;
        return this;
    }

    public InngagePushConfig setTargetActivity(String className) {
        this.targetActivityClassName = className;
        return this;
    }

    public String getTargetActivity() {
        return targetActivityClassName;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getChannelDescription() {
        return channelDescription;
    }

    public int getSmallIcon() {
        return smallIconRes;
    }

    public int getNotificationColor() {
        return notificationColor;
    }
}
