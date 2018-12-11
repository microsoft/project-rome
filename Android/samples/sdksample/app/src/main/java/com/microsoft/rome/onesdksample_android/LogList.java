//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.support.v4.app.Fragment;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Handles the logic of a Log ListView, providing the interface for adding logging.
 */
public class LogList {
    // region Member Variables
    private Fragment mFragment;
    private ListView mListView;
    private LogListAdapter mLogListAdapter;
    // endregion

    // region Overrides
    public LogList(Fragment fragment, ListView listView) {
        mFragment = fragment;
        mListView = listView;

        mLogListAdapter = new LogListAdapter(fragment.getContext(), new ArrayList<String>());
        mListView.setAdapter(mLogListAdapter);
    }
    // endregion

    public void logTraffic(final String message) {
        mFragment.getActivity().runOnUiThread(() -> mLogListAdapter.add(message));
    }
}