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
import com.microsoft.connecteddevices.useractivities.UserActivityAttribution;
import com.microsoft.connecteddevices.useractivities.UserActivitySessionHistoryItem;

import java.util.Date;
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

            TextView id = convertView.findViewById(R.id.activity_id);
            id.setText(activity.getActivityId().trim());

            TextView displayText = convertView.findViewById(R.id.activity_displaytext);
            displayText.setText(activity.getVisualElements().getDisplayText());

            TextView activationUri = convertView.findViewById(R.id.activity_activationuri);
            activationUri.setText(activity.getActivationUri());

            String iconUri = "";
            UserActivityAttribution attribution = activity.getVisualElements().getAttribution();
            if (attribution != null) {
                iconUri = attribution.getIconUri();
            }
            TextView activationIconUri = convertView.findViewById(R.id.activity_activationiconuri);
            activationIconUri.setText(iconUri);

            TextView start = convertView.findViewById(R.id.activity_start);
            start.setText(history.getStartTime().toString());

            String endTimeValue = "";
            Date endTime = history.getEndTime();
            if (endTime != null) {
                endTimeValue = endTime.toString();
            }
            TextView end = convertView.findViewById(R.id.activity_end);
            end.setText(endTimeValue);
        }

        return convertView;
    }
}
