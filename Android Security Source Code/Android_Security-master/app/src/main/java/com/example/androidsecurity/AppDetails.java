package com.example.androidsecurity;

import android.graphics.drawable.Drawable;

public class AppDetails {

    String appName;
    String appDesc;
    Drawable appIcon;
    String permissionList;
    int permissionCount;
    String packageName;
    boolean isSystemApp;
    double appSize;
    double dataUsed;

    /**
     * AppDetails object constructor
     * The AppDetails object stores the characteristics/details of that particular app
     * @param appName         - App Name
     * @param appDesc         - App description - App size & Network usage details
     * @param appIcon         - App Icon
     * @param permissionList  - List of granted permissions
     * @param permissionCount - No. of permissions granted
     * @param isSystemApp     - boolean variable denoting if app is a system app or not
     * @param packageName     - Package name of the app
     * @param appSize         - App Size in MB (App Size + Cache size)
     * @param dataUsed        - Network Data used in MB (WiFi data usage + Mobile data usage)
     */
    public AppDetails(String appName, String appDesc, Drawable appIcon, String permissionList,
                      int permissionCount, boolean isSystemApp, String packageName, double appSize, double dataUsed) {
        this.appName = appName;
        this.appDesc = appDesc;
        this.appIcon = appIcon;
        this.permissionList = permissionList;
        this.permissionCount = permissionCount;
        this.isSystemApp = isSystemApp;
        this.packageName = packageName;
        this.appSize = appSize;
        this.dataUsed = dataUsed;
    }
}
