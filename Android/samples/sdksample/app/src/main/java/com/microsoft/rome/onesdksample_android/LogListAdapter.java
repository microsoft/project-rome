//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Adapter for displaying a log of requests and responses
 */
public class LogListAdapter extends ArrayAdapter<String> {
    public LogListAdapter(Context context, ArrayList<String> messages) {
        super(context, 0, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_log_item, parent, false);
        }

        TextView title = (TextView)convertView.findViewById(R.id.title);

        final String message = getItem(position);
        title.setText(message);

        return convertView;
    }
}