package com.example.androidsecurity;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.NetworkStatsManager;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class AppListActivity extends AppCompatActivity {

    private static final String TAG = "AppListActivity";
    private static ArrayList<AppDetails> apps;
    private AppListAdapter adapter;
    private PackageManager pm;
    private static Handler mainHandler;
    private NetworkStatsManager nsm;
    private StorageStatsManager ssm;
    private UserHandle uh;
    private File dir;
    private CacheOperations cacheOp;
    private boolean isDataInCache;
    private SharedPreferences.Editor editor;
    private String scanDate;

    //UI Components
    private SearchView search;
    private RadioGroup appType;
    private RadioGroup sortBy;
    private RadioGroup largestOrSmallest;
    private Button filterBtn;
    private ProgressBar initProgress;
    private ImageButton refreshBtn;
    private TextView scanDateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        search = findViewById(R.id.search_view);               //Getting UI Components for access
        refreshBtn = findViewById(R.id.refresh_btn);
        appType = findViewById(R.id.app_type);
        sortBy = findViewById(R.id.sort_by);
        largestOrSmallest = findViewById(R.id.largest_smallest);
        filterBtn = findViewById(R.id.filter_btn);
        initProgress = findViewById(R.id.progress);
        scanDateView = findViewById(R.id.scan_date);

        dir = getFilesDir();
        SharedPreferences sharedPref = getSharedPreferences("Cache", Context.MODE_PRIVATE);
        scanDate = sharedPref.getString("scanDate","");
        isDataInCache = sharedPref.getBoolean("isDataInCache",false);
        editor = sharedPref.edit();

        cacheOp = new CacheOperations(dir);
        mainHandler = new MyHandler();
        pm = getPackageManager();

        refreshBtn.setOnClickListener(new View.OnClickListener() {              //Setting listener for refresh button
            @Override
            public void onClick(View v) {
                AlertDialog alertDialog = new AlertDialog.Builder(AppListActivity.this).create();
                alertDialog.setTitle("Refresh operation");
                alertDialog.setMessage("Are you sure you wish to refresh the app list?");

                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        initOperation();
                    }
                });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        });

        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);                           //Checking if permission has been granted
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());

        if(mode != AppOpsManager.MODE_ALLOWED) {
            AlertDialog alertDialog = new AlertDialog.Builder(AppListActivity.this).create();
            alertDialog.setTitle("Permission");
            alertDialog.setMessage("Please give the permission for usage access, to see storage space and data usage statistics\n\nThe permission can be granted through app settings.");
            alertDialog.show();
        } else {
            if(isDataInCache) {
                refreshBtn.setEnabled(false);                   // 1) Disabling UI Components as app details retrieval is taking place
                filterBtn.setEnabled(false);
                search.setIconifiedByDefault(true);
                initProgress.setVisibility(View.VISIBLE);

                LoadCache lc = new LoadCache();
                Thread lcThread = new Thread(lc);
                lcThread.start();
            }
            else {
                initOperation();
            }
        }

        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {        //Setting listener for search view
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchFilter(newText);
                return true;
            }
        });
    }

    // Getting data and displaying data

    /**
     * Function to refresh the app list with current data
     */
    private void initOperation() {
        refreshBtn.setEnabled(false);                   // 1) Disabling UI Components as app details retrieval is taking place
        initProgress.setVisibility(View.VISIBLE);
        filterBtn.setEnabled(false);
        search.setIconifiedByDefault(true);

        pm = this.getPackageManager();                          // 2) Getting necessary parameters to retrieve app details
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ssm = (StorageStatsManager) getSystemService(Context.STORAGE_STATS_SERVICE);
            uh = android.os.Process.myUserHandle();
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nsm = (NetworkStatsManager) getSystemService(Context.NETWORK_STATS_SERVICE);
        }
        SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        int days = sharedPref.getInt("days", 7);

        Operations op = new Operations(mainHandler, this, pm, days, ssm, nsm, uh, dir);     // 3) Starting app details retrieval operation
        Thread opTh = new Thread(op);
        opTh.start();
    }

    /**
     * Function to set the RecyclerView with the data obtained by refreshing, or from cache
     */
    private void initAppList() {
        RecyclerView recyclerView = findViewById(R.id.app_list);                    // 1) Setting app details in the recycler view
        adapter = new AppListAdapter(this, apps);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        initProgress.setVisibility(View.GONE);                                      // 2) Enabling UI Components as app details retrieval is complete
        filterBtn.setEnabled(true);
        search.setIconifiedByDefault(false);
        refreshBtn.setEnabled(true);
        scanDateView.setText("Data as of : " + scanDate);
    }

    /**
     * Function to update the apps object with app icons, if app list is loaded from cache
     */
    private void getAppIcons() {
        String packageName;
        ApplicationInfo ai;
        Drawable icon;
        for (AppDetails ad : apps) {
            packageName = ad.packageName;
            try {
                ai = pm.getApplicationInfo(packageName, 0);
                icon = pm.getApplicationIcon(ai);
                ad.appIcon = icon;
            } catch (PackageManager.NameNotFoundException e) {
                ad.appIcon = getDrawable(R.drawable.ic_launcher_foreground);
                e.printStackTrace();
            }
        }
    }

    private void permissionCountFilter(int sortBy) {
        if (sortBy == 0) {                                          // Sort by smallest on top
            Collections.sort(apps, new Comparator<AppDetails>() {
                @Override
                public int compare(AppDetails o1, AppDetails o2) {
                    return Double.compare(o1.permissionCount, o2.permissionCount);
                }
            });
        } else if (sortBy == 1) {                                   // Sort by largest on top
            Collections.sort(apps, new Comparator<AppDetails>() {
                @Override
                public int compare(AppDetails o1, AppDetails o2) {
                    return (Double.compare(o2.permissionCount, o1.permissionCount));
                }
            });
        }
    }

    void appTypeFilter(int type) {
        ArrayList<AppDetails> filteredApps = new ArrayList<>();
        if (type == 0) {                        // To filter out installed apps
            for (AppDetails a : apps) {
                if (!a.isSystemApp)
                    filteredApps.add(a);
            }
        } else if (type == 1) {                 // To filter out system apps
            for (AppDetails a : apps) {
                if (a.isSystemApp)
                    filteredApps.add(a);
            }
        } else if (type == 2) {                 // To filter all apps (both system and installed)
            filteredApps = apps;
        }

        adapter.filteredList(filteredApps);
    }

    //Open settings
    public void openSettings(View view) {
        startActivity(new Intent(this, AppSettings.class));
    }

    // Search and Filters
    private void searchFilter(String query) {
        ArrayList<AppDetails> filteredApps = new ArrayList<>();
        for (AppDetails a : apps) {
            if (a.appName.toLowerCase().contains(query.toLowerCase()))
                filteredApps.add(a);
        }
        adapter.filteredList(filteredApps);
    }

    void appSizeFilter(int sortBy) {
        if (sortBy == 0) {                                          // Sort by smallest on top
            Collections.sort(apps, new Comparator<AppDetails>() {

                @Override
                public int compare(AppDetails o1, AppDetails o2) {
                    return Double.compare(o1.appSize, o2.appSize);
                }
            });
        } else if (sortBy == 1) {                                   // Sort by largest on top
            Collections.sort(apps, new Comparator<AppDetails>() {

                @Override
                public int compare(AppDetails o1, AppDetails o2) {
                    return Double.compare(o2.appSize, o1.appSize);
                }
            });
        }

        adapter.filteredList(apps);
    }

    void dataUsedFilter(int sortBy) {
        if (sortBy == 0) {                                          // Sort by smallest on top
            Collections.sort(apps, new Comparator<AppDetails>() {
                @Override
                public int compare(AppDetails o1, AppDetails o2) {
                    return Double.compare(o1.dataUsed, o2.dataUsed);
                }
            });
        } else if (sortBy == 1) {                                    // Sort by largest on top
            Collections.sort(apps, new Comparator<AppDetails>() {
                @Override
                public int compare(AppDetails o1, AppDetails o2) {
                    return (Double.compare(o2.dataUsed, o1.dataUsed));
                }
            });
        }

        adapter.filteredList(apps);
    }

    /**
     * Function to apply filters as per input by user
     *
     * @param view - Filter button
     */
    public void applyFilters(View view) {
        int smallestOrLargest = 0;

        switch (largestOrSmallest.getCheckedRadioButtonId()) {
            case R.id.smallest:
                smallestOrLargest = 0;
                break;
            case R.id.largest:
                smallestOrLargest = 1;
                break;
        }

        switch (sortBy.getCheckedRadioButtonId()) {
            case R.id.option_data_used:
                dataUsedFilter(smallestOrLargest);
                break;
            case R.id.option_app_size:
                appSizeFilter(smallestOrLargest);
                break;
            case R.id.option_permissions:
                permissionCountFilter(smallestOrLargest);
                break;
        }

        switch (appType.getCheckedRadioButtonId()) {
            case R.id.radio_installed:
                appTypeFilter(0);
                break;
            case R.id.radio_system:
                appTypeFilter(1);
                break;
            case R.id.radio_all:
                appTypeFilter(2);
                break;
        }
    }

    /**
     * Handler to get app list from message sent from Operations thread,
     */
    private class MyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            apps = (ArrayList<AppDetails>) msg.obj;
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Date dt = new Date();
            scanDate = formatter.format(dt);
            editor.putString("scanDate", scanDate);
            editor.putBoolean("isDataInCache", true);
            editor.apply();
            initAppList();
        }
    }

    /**
     * Inner thread class to load app list from cache
     */
    private class LoadCache implements Runnable {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            apps = cacheOp.readData();
            getAppIcons();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initAppList();
                }
            });
        }
    }

    //Toggle filter sections
    public void showSortBy(View view) {
        if (sortBy.getVisibility() == View.VISIBLE) {
            sortBy.setVisibility(View.GONE);
            largestOrSmallest.setVisibility(View.GONE);
            appType.setVisibility(View.GONE);
        } else {
            sortBy.setVisibility(View.VISIBLE);
            largestOrSmallest.setVisibility(View.GONE);
            appType.setVisibility(View.GONE);
        }
    }

    public void showAppTypes(View view) {
        if(appType.getVisibility() == View.VISIBLE) {
            sortBy.setVisibility(View.GONE);
            largestOrSmallest.setVisibility(View.GONE);
            appType.setVisibility(View.GONE);
        }
        else {
            appType.setVisibility(View.VISIBLE);
            largestOrSmallest.setVisibility(View.GONE);
            sortBy.setVisibility(View.GONE);
        }
    }

    public void showLargestSmallest(View view) {
        if(largestOrSmallest.getVisibility() == View.VISIBLE) {
            sortBy.setVisibility(View.GONE);
            largestOrSmallest.setVisibility(View.GONE);
            appType.setVisibility(View.GONE);
        }
        else {
            largestOrSmallest.setVisibility(View.VISIBLE);
            sortBy.setVisibility(View.GONE);
            appType.setVisibility(View.GONE);
        }
    }
}