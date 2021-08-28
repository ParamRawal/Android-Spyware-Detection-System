package com.example.androidsecurity;

import android.annotation.SuppressLint;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.os.storage.StorageManager.UUID_DEFAULT;

public class Operations implements Runnable {

    Handler mainHandler;
    private List<PackageInfo> packages;
    private ArrayList<AppDetails> apps;
    private PackageManager pm;
    private int days;
    private Context context;
    private List<PermissionGroupInfo> permissionGroups;
    private ArrayList<PermissionInfo> permissionInfo = new ArrayList<>();
    private StorageStatsManager ssm;
    private NetworkStatsManager nsm;
    private UserHandle uh;
    private File dir;
    private ArrayList<String> permissionNames;

    public Operations(Handler mainHandler, Context context, PackageManager pm, int days,
                      StorageStatsManager ssm, NetworkStatsManager nsm, UserHandle uh, File dir) {
        this.mainHandler = mainHandler;
        this.context = context;
        this.pm = pm;
        this.days = days;
        this.ssm = ssm;
        this.nsm = nsm;
        this.uh = uh;
        this.dir = dir;
    }

    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        retrievePermissions();
        getAppDetails();
    }

    /**
     * Function to get all the possible permissions in the device
     */
    private void retrievePermissions() {
        permissionGroups = pm.getAllPermissionGroups(0);
        permissionNames = new ArrayList<>();
        List<PermissionInfo> permissionsByGroup = null;
        String permissionGroupName;

        for (PermissionGroupInfo pg : permissionGroups) {
            permissionGroupName = pg.name;
            try {
                permissionsByGroup = pm.queryPermissionsByGroup(permissionGroupName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (permissionsByGroup != null)
                permissionInfo.addAll(permissionsByGroup);
        }

        for (PermissionInfo pi : permissionInfo) {
            permissionNames.add(pi.name);
        }
    }

    /**
     * Function to retrieve app details
     */
    @SuppressLint("DefaultLocale")
    private void getAppDetails() {
        packages = pm.getInstalledPackages(PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS);

        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -(days));
        Date newDate = cal.getTime();
        long startTime = cal.getTimeInMillis();

        apps = new ArrayList<>();

        for (PackageInfo p : packages) {
            String packageName = p.packageName;
            ApplicationInfo a = null;
            try {
                a = pm.getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String appName = pm.getApplicationLabel(a).toString();
            Drawable appIcon = pm.getApplicationIcon(a);

            //App Size & Data Usage
            double wifiData = 0, mobileData = 0;
            double[] networkDataUsage = new double[] {0,0};
            double[] appSizeDetails = new double[] {0,0};

            try {
                networkDataUsage = getNetworkDataUsage(a, startTime, endTime);
                appSizeDetails = getAppSize(packageName, a);
            } catch(SecurityException e) {
                e.printStackTrace();
            }

            wifiData = networkDataUsage[0];
            mobileData = networkDataUsage[1];
            String appInfo = String.format("App Size : %.2f MB \nCache Size : %.2f MB", appSizeDetails[0], appSizeDetails[1]);
            appInfo += String.format("\nWifi Data:  %.2f MB \nMobile data: %.2f MB", wifiData, mobileData);

            //Permissions
            int permissionCount = 0;
            String permissionList = "";
            if (permissionInfo != null) {
                for (String permName : permissionNames) {
                    if (permName != null)
                        if (pm.checkPermission(permName, p.packageName) == PackageManager.PERMISSION_GRANTED) {
                            String name = permName;
                            int index = name.indexOf("permission") + 11;
                            permissionList += name.substring(index) + "\n";
                            permissionCount++;
                        }
                }
            }

            boolean isSystemApp = false;
            if ((a.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                isSystemApp = true;
            }

            double appSize = appSizeDetails[0] + appSizeDetails[1];
            double dataUsed = wifiData + mobileData;

            apps.add(new AppDetails(appName, appInfo, appIcon, permissionList, permissionCount, isSystemApp, p.packageName, appSize, dataUsed));
        }

        CacheOperations cacheOp = new CacheOperations(dir);
        cacheOp.writeData(apps);
        cacheOp.writePermissions(permissionNames);

        Message message = mainHandler.obtainMessage();
        message.obj = apps;
        mainHandler.sendMessage(message);
    }

    /**
     * Function to calculate app size and cache size of application
     *
     * @param packageName - Package name of the application
     * @param a           - ApplicationInfo object of the application
     * @return - returns array of app size and cache size (in MB)
     */
    @SuppressLint("DefaultLocale")
    private double[] getAppSize(String packageName, ApplicationInfo a) {
        double appSize = 0, cacheSize = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            UUID uuid = UUID_DEFAULT;

            try {
                final StorageStats storageStats = ssm.queryStatsForPackage(uuid, packageName, uh);
                appSize = storageStats.getAppBytes();
                cacheSize = storageStats.getCacheBytes();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            appSize = appSize / (1048576);
            cacheSize = cacheSize / (1048576);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            //StorageStatsManager does not work for Android Version N (Android 7) and below
            appSize = cacheSize = 0;
        }
        return new double[]{appSize, cacheSize};
    }

    /**
     * Function to calculate network data usage between a specified period of time
     *
     * @param a         - ApplicationInfo object of the application
     * @param startTime - Start time of specified period of time, in milliseconds
     * @param endTime   - End time of specified period of time, in milliseconds
     * @return - array of wifi data used and mobile data used (in MB) for specified period of time
     */
    private double[] getNetworkDataUsage(ApplicationInfo a, long startTime, long endTime) {
        double wifiData = 0, mobileData = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int appUid = a.uid;

            final NetworkStats nwStatsWifi = nsm.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, null, startTime, endTime, appUid);
            final NetworkStats nwStatsMobile = nsm.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime, appUid);
            NetworkStats.Bucket bucketWifi = new NetworkStats.Bucket();
            NetworkStats.Bucket bucketMobile = new NetworkStats.Bucket();

            double receivedData = 0, sentData = 0;

            while (nwStatsWifi.hasNextBucket()) {
                nwStatsWifi.getNextBucket(bucketWifi);
                receivedData = receivedData + bucketWifi.getRxBytes();
                sentData = sentData + bucketWifi.getTxBytes();
            }
            wifiData = receivedData + sentData;
            receivedData = sentData = 0;

            while (nwStatsMobile.hasNextBucket()) {
                nwStatsMobile.getNextBucket(bucketMobile);
                receivedData = receivedData + bucketWifi.getRxBytes();
                sentData = sentData + bucketWifi.getTxBytes();
            }

            mobileData = receivedData + sentData;
        }

        wifiData = wifiData / (1048576);
        mobileData = mobileData / 1048576;

        return new double[]{wifiData, mobileData};
    }
}