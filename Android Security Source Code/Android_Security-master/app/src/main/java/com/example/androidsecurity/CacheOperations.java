package com.example.androidsecurity;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class CacheOperations {

    private File dir;

    public CacheOperations(File dir) {
        this.dir = dir;
    }

    /**
     * Function to store the list of permissions on device, in a file in cache
     *
     * @param permissionNames - ArrayList of permission names, in String
     */
    public void writePermissions(ArrayList<String> permissionNames) {
        String permissionsList = "";

        for (String p : permissionNames) {
            permissionsList = permissionsList + p + "\n";
        }

        File file = new File(dir, "permissions.txt");

        try {
            FileWriter fw = new FileWriter(file);
            fw.write(permissionsList);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to store the list of apps on phone, with their details, in cache
     *
     * @param apps - ArrayList of AppDetails objects
     */
    public void writeData(ArrayList<AppDetails> apps) {
        String header = "appName,appDesc,permissionList,permissionCount,isSystemApp,packageName,appSize,dataUsed";
        String data = "";
        String appName, appDesc, permissionList, permissionCount, packageName, isSystemApp, appSize, dataUsed;

        for (AppDetails ad : apps) {
            appName = ad.appName;
            appDesc = ad.appDesc;
            permissionList = ad.permissionList;
            permissionCount = Integer.toString(ad.permissionCount);
            packageName = ad.packageName;
            isSystemApp = Boolean.toString(ad.isSystemApp);
            appSize = Double.toString(ad.appSize);
            dataUsed = Double.toString(ad.dataUsed);

            appDesc = appDesc.replaceAll("\n", ";");
            permissionList = permissionList.replaceAll("\n", ";");

            data += appName + "," + appDesc + "," + permissionList + "," + permissionCount + "," + isSystemApp +
                    "," + packageName + "," + appSize + "," + dataUsed + "\n";
        }

        File file = new File(dir,"appList.txt");

        try {
            FileWriter fw = new FileWriter(file);
            fw.write(data);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected ArrayList<AppDetails> readData() {
        File file = new File(dir,"appList.txt");
        ArrayList<String> appRows = new ArrayList<>();

        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String s;
            while((s = br.readLine()) != null) {
                s = s.replaceAll("\n","");
                appRows.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ArrayList<AppDetails> apps = new ArrayList<>();
        String[] cells;
        String appName;
        String appDesc;
        String permissionList;
        int permissionCount;
        String packageName;
        boolean isSystemApp;
        double appSize;
        double dataUsed;

        for(String row:appRows) {
            Log.d(TAG, "readData: the row is: "+row);
            cells = row.split(",");

            appName = cells[0];
            Log.d(TAG, "readData: BEFORE appDesc = "+cells[1]);
            appDesc = cells[1].replaceAll(";","\n");
            Log.d(TAG, "readData: AFTER appDesc = "+appDesc);
            Log.d(TAG, "readData: BEFORE permList = "+cells[2]);
            permissionList = cells[2].replaceAll(";","\n");
            Log.d(TAG, "readData:  AFTER permList  = "+permissionList);
            permissionCount = Integer.parseInt(cells[3]);
            isSystemApp = Boolean.parseBoolean(cells[4]);
            packageName = cells[5];
            appSize = Double.parseDouble(cells[6]);
            dataUsed = Double.parseDouble(cells[7]);

            apps.add(new AppDetails(appName,appDesc, null,permissionList,permissionCount,isSystemApp,packageName,appSize,dataUsed));
        }
        return apps;
    }

    protected ArrayList<String> readPermissions() {
        //Read from file
        File file = new File(dir,"permissions.txt");
        ArrayList<String> permissionList = new ArrayList<>();
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String s;
            while((s = br.readLine()) != null) {
                s = s.replaceAll("\n","");
                permissionList.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return permissionList;
    }
}