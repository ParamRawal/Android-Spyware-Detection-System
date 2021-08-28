package com.example.androidsecurity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder>{

    private static final String TAG = "AppListAdapter";
    private Context mContext;
    private ArrayList<AppDetails> apps;

    public AppListAdapter(Context mContext, ArrayList<AppDetails> apps) {
        this.mContext = mContext;
        this.apps = apps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: onCreateViewHolder called");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;  
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: "+apps.get(position).appName);
        holder.appIcon.setImageDrawable(apps.get(position).appIcon);
        holder.appName.setText(apps.get(position).appName);
        holder.appDescription.setText(apps.get(position).appDesc);

        holder.appItemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext,AppDetailsActivity.class);
                intent.putExtra("AppName",apps.get(position).appName);
                intent.putExtra("PackageName", apps.get(position).packageName);
                intent.putExtra("Permissions",apps.get(position).permissionList);
                intent.putExtra("isSystemApp",apps.get(position).isSystemApp);
                intent.putExtra("Details", apps.get(position).appDesc);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: called");
        return apps.size();
    }

    public void filteredList(ArrayList<AppDetails> filteredApps) {
        this.apps = filteredApps;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView appDescription;
        RelativeLayout appItemLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, "ViewHolder: constructor called");
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appDescription = itemView.findViewById(R.id.app_description);
            appItemLayout = itemView.findViewById(R.id.app_item_layout);
        }
    }
}
