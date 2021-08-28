package com.example.androidsecurity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AppDetailsActivity extends AppCompatActivity {

    PackageManager pm;
    String packageName;
    private Toolbar toolbar;
    private TextView name;
    private ImageView icon;
    private TextView permissions;
    private TextView description;
    private Button uninstall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_details);

        toolbar = findViewById(R.id.toolbar);               // 1) Getting UI Components
        name = findViewById(R.id.app_name);
        icon = findViewById(R.id.app_icon);
        permissions = findViewById(R.id.app_permissions);
        description  = findViewById(R.id.app_description);
        uninstall = findViewById(R.id.uninstall);

        pm = this.getPackageManager();                      // 2) Initializing fields

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));     // 3) Setting back navigation
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getIncomingIntent();                // 4) Getting intent from previous screen (AppListActivity)
    }

    private void getIncomingIntent() {
        if(getIntent().hasExtra("PackageName") && getIntent().hasExtra("AppName")
                && getIntent().hasExtra("Permissions") && getIntent().hasExtra("isSystemApp")
                && getIntent().hasExtra("Details")) {
            String appName = getIntent().getStringExtra("AppName");
            String details = getIntent().getStringExtra("Details");
            packageName = getIntent().getStringExtra("PackageName");
            String permissionList = getIntent().getStringExtra("Permissions");
            boolean isSystemApp = getIntent().getBooleanExtra("isSystemApp", false);

            ApplicationInfo appInfo = null;
            try {
                appInfo = pm.getApplicationInfo(packageName,0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Drawable appIcon;
            if (appInfo == null) {
                appIcon = getDrawable(R.drawable.ic_launcher_foreground);
            } else {
                appIcon = pm.getApplicationIcon(appInfo);
            }
            setData(appName, appIcon, permissionList, details, isSystemApp);
        }
    }

    /**
     * Function to set/display received data on the screen
     *
     * @param appName        - Name of app
     * @param appIcon        - App icon
     * @param permissionList - List of permissions granted to app
     * @param details        - App size & Network usage details
     * @param isSystemApp    - boolean variable denoting if app is a system app or not
     */
    private void setData(String appName, Drawable appIcon, String permissionList, String details, boolean isSystemApp) {
        permissions.setText(permissionList);
        name.setText(appName);
        icon.setImageDrawable(appIcon);
        description.setText(details);
        if (isSystemApp) uninstall.setEnabled(false);
    }

    /**
     * Function to open the settings screen of the particular app, to manage its permissions
     *
     * @param view - Manage Permissions Button
     */
    public void openSettings(View view) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", packageName, null);
            intent.setData(uri);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to uninstall the particular app
     *
     * @param view - Uninstall Button
     */
    public void uninstall(View view) {
        try {
            Uri uri = Uri.fromParts("package", packageName, null);
            Intent intent = new Intent(Intent.ACTION_DELETE, uri);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}