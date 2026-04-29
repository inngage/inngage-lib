package br.com.inngage.sdk;

public final class AppInfo {
    private final String installationDate;
    private final String updateDate;
    private final String versionName;

    public AppInfo(String installationDate, String updateDate, String versionName) {

        this.installationDate = installationDate;
        this.updateDate = updateDate;
        this.versionName = versionName;
    }

    public String getInstallationDate() {
        return installationDate;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public String getVersionName() {
        return versionName;
    }
}
