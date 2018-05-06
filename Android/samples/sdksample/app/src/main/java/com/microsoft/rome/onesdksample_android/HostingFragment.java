//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Display all received AppService messages and LaunchUri commands
 */
public class HostingFragment extends BaseFragment {
    // region Member Variables
    private static final String TAG = HostingFragment.class.getName();
    private LogList mLogList;
    // endregion

    // region Overrides
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_hosting, container, false);

        ListView hostingLogListView = (ListView)rootView.findViewById(R.id.hosting_log);
        mLogList = new LogList(this, hostingLogListView);

        return rootView;
    }

    @Override
    String getLogTag() {
        return TAG;
    }
    // endregion

    public void logTrafficMessage(final String message) {
        mLogList.logTraffic(message);
    }
}
