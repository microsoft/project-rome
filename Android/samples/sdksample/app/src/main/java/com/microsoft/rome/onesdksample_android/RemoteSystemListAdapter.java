//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Context;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.microsoft.connecteddevices.discovery.RemoteSystem;
import com.microsoft.connecteddevices.discovery.RemoteSystemApp;

import java.util.List;

/**
 * Adapter for displaying systems and their applications in an expandable list
 */
public class RemoteSystemListAdapter extends BaseExpandableListAdapter {
    private final Context mContext;
    private final ArrayMap<String, RemoteSystem> mSystems;

    public RemoteSystemListAdapter(Context context, ArrayMap<String, RemoteSystem> systems) {
        mContext = context;
        mSystems = systems;
    }
    // region Overrides
    @Override
    public int getGroupCount() {
        return mSystems.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        RemoteSystem system = mSystems.valueAt(groupPosition);
        if (system == null) {
            return 0;
        }

        return system.getApps().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mSystems;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        RemoteSystem system = mSystems.get(groupPosition);
        if (system == null) {
            return null;
        }

        return system.getApps().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        // Corresponding indices in system list may change as systems are added/removed
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        // Create view if the given one cannot be reused
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.remote_system_list_item, null);
        }

        TextView name = (TextView)convertView.findViewById(R.id.system_name);
        TextView id = (TextView)convertView.findViewById(R.id.system_id);
        TextView status = (TextView)convertView.findViewById(R.id.system_status);
        TextView kind = (TextView)convertView.findViewById(R.id.system_kind);
        TextView make = (TextView)convertView.findViewById(R.id.system_make_model);
        TextView apps = (TextView)convertView.findViewById(R.id.system_applications);

        RemoteSystem system = mSystems.valueAt(groupPosition);
        name.setText(system.getDisplayName());
        id.setText(system.getId().trim());
        status.setText(system.getStatus().toString());
        kind.setText(system.getKind());
        make.setText(String.format("%s %s", system.getManufacturerDisplayName(), system.getModelDisplayName()));
        apps.setText(Integer.toString(getChildrenCount(groupPosition)));

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, final ViewGroup parent) {
        // Create view if the given one cannot be reused
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.remote_application_list_item, null);
        }

        TextView name = (TextView)convertView.findViewById(R.id.application_name);
        TextView id = (TextView)convertView.findViewById(R.id.application_id);
        TextView proximal = (TextView)convertView.findViewById(R.id.application_proximal);
        TextView spatial = (TextView)convertView.findViewById(R.id.application_spatial);

        final RemoteSystemApp app = getRemoteSystemApp(groupPosition, childPosition);
        name.setText(app.getDisplayName());
        id.setText(app.getId().trim());
        proximal.setText(Boolean.toString(app.getIsAvailableByProximity()));
        spatial.setText(Boolean.toString(app.getIsAvailableBySpatialProximity()));

        // When a remote application is selected, pass that remote system to the Launch Fragment
        // and navigate to Launch Fragment
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity)mContext;
                // Set the selected RemoteSystemApp then navigate to that page
                mainActivity.getLaunchFragment().setRemoteSystemApp(app);
                mainActivity.navigateToPage(MainActivity.LAUNCH);
            }
        });

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
    // endregion

    public RemoteSystemApp getRemoteSystemApp(int groupPosition, int childPosition) {
        return mSystems.valueAt(groupPosition).getApps().get(childPosition);
    }
}
