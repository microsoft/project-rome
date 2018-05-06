//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.os.Bundle;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;

import com.microsoft.connecteddevices.base.EventListener;
import com.microsoft.connecteddevices.discovery.RemoteSystem;
import com.microsoft.connecteddevices.discovery.RemoteSystemFilter;
import com.microsoft.connecteddevices.discovery.RemoteSystemLocalVisibilityKind;
import com.microsoft.connecteddevices.discovery.RemoteSystemLocalVisibilityKindFilter;
import com.microsoft.connecteddevices.discovery.RemoteSystemWatcher;
import com.microsoft.connecteddevices.discovery.RemoteSystemWatcherError;

import java.util.ArrayList;

/**
 *  Discovers remote systems and applications using RemoteSystemWatcher
 *  Discover devices by doing the following:
 *  Step #1: Determine what, if any, filters will be used
 *      These filters can filter for:
 *          Discovery Type (is remote device proximal, cloud, etc)
 *          Status (is the remote device available?)
 *          Device type (is it a desktop?)
 *          Authorization (only for the same user?)
 *          (See onStartWatcherClicked())
 *  Step #2: Create the RemoteSystemWatcher
 *      If you used filters, pass these filters when creating the RemoteSystemWatcher
 *  Step #3: Add the event listeners for when the list of remote devices gets updated
 *  Step #4: Start the RemoteSystemWatcher
 *      Once it has been started, RemoteSystemWatcher will run in the background until it has stopped
 *      You monitor changes to the device list by using the events we assigned earlier
 *      Once you no longer need to discover remote systems, stop the RemoteSystemWatcher.
 *      It is recommended to stop the watcher when it is not needed, to avoid unnecessary network communication and battery drain
 */
public class RemoteSystemWatcherFragment extends BaseFragment {
    // region Member Variables
    private static final String TAG = RemoteSystemWatcherFragment.class.getName();

    // Indices to lookup special values in filter selection spinners
    private static final int sNoFilterIndex = 0;
    private static final int sAllKindsIndex = 1;

    private ExpandableListView mSystemListView;
    private RemoteSystemListAdapter mSystemListAdapter;

    private RemoteSystemWatcher mWatcher = null;
    private ArrayMap<String, RemoteSystem> mSystems;
    // endregion

    // region Overrides
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the fragment view to access UI elements and return to draw the fragment
        View rootView = inflater.inflate(R.layout.fragment_discovery, container, false);

        // Create the ExpandableListView to display the RemoteSystemWatcher results
        mSystems = new ArrayMap<>();
        mSystemListView = rootView.findViewById(R.id.system_list_view);
        mSystemListAdapter = new RemoteSystemListAdapter(getContext(), mSystems);
        mSystemListView.setAdapter(mSystemListAdapter);

        // Create Start/Stop watcher buttons
        rootView.findViewById(R.id.discovery_start).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onStartWatcherClicked();
            }
        });

        return rootView;
    }
    // endregion

    @Override
    public void onPause() {
        super.onPause();
        if (mWatcher == null) {
            return;
        }

        mWatcher.stop();
    }

    private void onStartWatcherClicked() {
        ArrayList<RemoteSystemFilter> filters = new ArrayList<>();
        filters.add(new RemoteSystemLocalVisibilityKindFilter(RemoteSystemLocalVisibilityKind.SHOW_ALL));

        // Stop previous watcher if one exists
        if (mWatcher != null) {
            mWatcher.stop();
        }

        if (filters.isEmpty()) {
            mWatcher = new RemoteSystemWatcher();
        } else {
            mWatcher = new RemoteSystemWatcher(filters.toArray(new RemoteSystemFilter[filters.size()]));
        }

        // Use these events to keep the list of Remote Systems up to date
        mWatcher.addRemoteSystemAddedListener(new RemoteSystemAddedListener());
        mWatcher.addRemoteSystemUpdatedListener(new RemoteSystemUpdatedListener());
        mWatcher.addRemoteSystemRemovedListener(new RemoteSystemRemovedListener());
        mWatcher.addErrorOccurredListener(new RemoteSystemWatcherErrorOccurredListener());

        clearSystems();

        try {
            mWatcher.start();
        } catch (Exception e) { WriteApiException("RemoteSystemWatcher.start", e); }
    }

    /**
     * Adds/updates a system to the backing list of the system list adapter, and notifies the adapter of a
     * data set change on the UI thread.
     *
     * NOTE: The update of the backing list must also happen on the UI thread to avoid a possible
     * race between the data set changing and the UI being refreshed before we raise the data
     * set changed notification.
     */
    private void addOrUpdateSystem(final RemoteSystem remoteSystem) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSystems.put(remoteSystem.getId(), remoteSystem);
                mSystemListAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Removes a system from the backing list of the system list adapter, and notifies the adapter
     * of a data set change on the UI thread.
     *
     * NOTE: The update of the backing list must also happen on the UI thread to avoid a possible
     * race between the data set changing and the UI being refreshed before we raise the data
     * set changed notification.
     */
    private void removeSystem(final String systemId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSystems.remove(systemId);
                mSystemListAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Clears all systems from the backing list of the system list adapter, and notifies the adapter
     * of a data set change on the UI thread.
     *
     * NOTE: The update of the backing list must also happen on the UI thread to avoid a possible
     * race between the data set changing and the UI being refreshed before we raise the data
     * set changed notification.
     */
    private void clearSystems() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSystems.clear();
                mSystemListAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    String getLogTag() {
        return TAG;
    }

    // region RemoteSystemWatcher Listeners
    private class RemoteSystemAddedListener implements EventListener<RemoteSystemWatcher, RemoteSystem> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystem remoteSystem) {
            addOrUpdateSystem(remoteSystem);
        }
    }

    private class RemoteSystemUpdatedListener implements EventListener<RemoteSystemWatcher, RemoteSystem> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystem remoteSystem) {
            addOrUpdateSystem(remoteSystem);
        }
    }

    private class RemoteSystemRemovedListener implements EventListener<RemoteSystemWatcher, RemoteSystem> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystem remoteSystem) {
            removeSystem(remoteSystem.getId());
        }
    }

    private class RemoteSystemWatcherErrorOccurredListener implements EventListener<RemoteSystemWatcher, RemoteSystemWatcherError> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemWatcherError remoteSystemWatcherError) {
        }
    }
    // endregion
}
