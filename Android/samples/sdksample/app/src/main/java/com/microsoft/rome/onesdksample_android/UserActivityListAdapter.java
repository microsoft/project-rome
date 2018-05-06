//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.microsoft.connecteddevices.useractivities.UserActivity;
import com.microsoft.connecteddevices.useractivities.UserActivitySessionHistoryItem;

import java.util.List;

/**
 * Adapter for displaying activities in an expandable list
 */
public class UserActivityListAdapter extends ArrayAdapter<UserActivitySessionHistoryItem> {

    public UserActivityListAdapter(Context context, List<UserActivitySessionHistoryItem> items) {
        super(context, R.layout.useractivity_list_item, items);
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {

        UserActivitySessionHistoryItem history = getItem(position);

        // Create view if the given one cannot be reused
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.useractivity_list_item, parent, false);
        }

        if (history != null) {
            UserActivity activity = history.getUserActivity();
            TextView id = (TextView)convertView.findViewById(R.id.activity_id);
            id.setText(activity.getActivityId().trim());
            TextView displayText = (TextView)convertView.findViewById(R.id.activity_displaytext);
            displayText.setText(activity.getVisualElements().getDisplayText());
            TextView activationUri = (TextView)convertView.findViewById(R.id.activity_activationuri);
            activationUri.setText(activity.getActivationUri());
            TextView activationIconUri = (TextView)convertView.findViewById(R.id.activity_activationiconuri);
            activationIconUri.setText(activity.getVisualElements().getAttribution().getIconUri());
            TextView start = (TextView)convertView.findViewById(R.id.activity_start);
            start.setText(history.getStartTime().toString());
            TextView end = (TextView)convertView.findViewById(R.id.activity_end);
            end.setText(history.getEndTime().toString());
        }

        return convertView;
    }
}
