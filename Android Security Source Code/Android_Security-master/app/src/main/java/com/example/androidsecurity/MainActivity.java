package com.example.androidsecurity;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private Button getAppList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);               // 1) Getting UI Components
        getAppList = findViewById(R.id.get_app_list);
        getAppList.setEnabled(false);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Android Security");

        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);           // 2) Checking if usage access permission has been granted
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());

        if (mode != AppOpsManager.MODE_ALLOWED) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Permission");
            alertDialog.setMessage("Please give the permission for usage access. (Used to calculate storage space and data usage)");

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Give Permission", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    getAppList.setEnabled(true);
                }
            });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alertDialog.show();
        } else {
            getAppList.setEnabled(true);
        }
    }

    /**
     * Function to open app settings
     *
     * @param view - Settings Button
     */
    public void openSettings(View view) {
        startActivity(new Intent(this, AppSettings.class));
    }

    /**
     * Function to take user to AppListActivity, where app list is generated and displayed
     *
     * @param view - Get App List Button
     */
    public void getAppList(View view) {
        startActivity(new Intent(this, AppListActivity.class));
    }
}