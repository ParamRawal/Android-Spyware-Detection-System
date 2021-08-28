package com.example.androidsecurity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AppSettings extends AppCompatActivity {

    RadioGroup usageTime;
    SharedPreferences.Editor editor;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar);                  // 1) Getting UI Components
        usageTime = findViewById(R.id.usage_time);

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));     // 2) Setting back navigation
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);      // 3) Retrieving no. of days stored in SharedPreferences
        int retrievedDays = sharedPref.getInt("days",7);

        RadioButton toBeChecked = null;
        switch (retrievedDays) {
            case 1:
                toBeChecked = findViewById(R.id.one_day);
                break;
            case 7:
                toBeChecked = findViewById(R.id.one_week);
                break;
            case 30:
                toBeChecked = findViewById(R.id.one_month);
                break;
            case 90:
                toBeChecked = findViewById(R.id.three_months);
                break;
            case 180:
                toBeChecked = findViewById(R.id.six_months);
                break;
        }
        toBeChecked.setChecked(true);

        editor = sharedPref.edit();
    }

    /**
     * Function to open Usage Access settings screen, where user can grant or revoke usage access permission
     *
     * @param view - Button
     */
    public void manageUsageAccess(View view) {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    /**
     * Function to set time period for which network data usage is to be calculated
     *
     * @param view - Button
     */
    public void setDataUsageTime(View view) {
        int days = 0;
        switch (usageTime.getCheckedRadioButtonId()) {
            case R.id.one_day:
                days = 1;
                break;
            case R.id.one_week:
                days = 7;
                break;
            case R.id.one_month:
                days = 30;
                break;
            case R.id.three_months:
                days = 90;
                break;
            case R.id.six_months:
                days = 180;
                break;
        }
        editor.putInt("days",days);
        editor.apply();
    }
}
