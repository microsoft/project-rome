//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.core.UserAccount;
import com.microsoft.connecteddevices.useractivities.UserActivity;
import com.microsoft.connecteddevices.useractivities.UserActivityChannel;
import com.microsoft.connecteddevices.useractivities.UserActivitySession;
import com.microsoft.connecteddevices.useractivities.UserActivitySessionHistoryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

/**
 * Creates, publishes, and reads User Activities
 * Create the UserActivityChannel
 *      To initialize the activity feed, the user must be signed in & the platform must be initialized
 *      In this app, the initializeUserActivityFeed() method is called after platform initialization in MainActivity
 *      Once platform init is complete, initializeUserActivityFeed() is called to initialize the activity feed
 *      In this app, the UserActivityChannel is created in getUserActivityChannel()
 *      A UserActivityChannel is created for the signed in account by passing the UserAccount to the constructor
 * Create a UserActivity
 *      UserActivities are created by calling the async method UserActivityChannel.getOrCreateUserActivityAsync and passing an activity ID
 *      If no activity ID is provided, an IllegalArgumentException is thrown.
 *      In this app, this is done in the createUserActivity method
 * Save (publish) a UserActivity
 *      First, set the DisplayText and ActivationURI properties of the Activity
 *      DisplayText will be shown on other devices when viewing the UserActivity (i.e. in Windows Timeline)
 *      The ActivationUri will determine what action is taken when the UserActivity is activated (i.e. when it is selected in Timeline)
 *      Call UserActivity.saveAsync() to save & publish the UserActivity
 * Creating a UserActivitySession
 *      If you have an existing activity and wish to amend additional information (some new engagement, changed page, etc), you can do so by
 * using a UserActivitySession
 *      Create a new session by calling UserActivity.createSession()
 *      Once you've created a session, you can make any desired changes to the properties of the UserActivity
 *      When you've made the changes, call UserActivitySession.close();
 *      Think of UserActivitySessions as a way to create a UserActivityHistory. Rather than create a new UserActivity every time a user
 * moves to a new page, you can simply create a new session for each page
 * Reading UserActivities
 *      To read UserActivities you call UserActivityChannel.getRecentUserActivitiesAsync and pass in the number of unique activities you
 * want to read
 *      i.e. passing in 5 will return the 5 most recent UserActivities your app has access to
 */
public class UserActivityFragment extends BaseFragment implements View.OnClickListener {
    private static final String TAG = UserActivityFragment.class.getName();

    // private Button mAfcInitButton;
    private TextView mActivityStatus;
    private Button mNewButton;
    private Button mStartButton;
    private Button mReadButton;
    private TextView mActivityId;
    private EditText mDisplayText;
    private EditText mActivationUri;
    private EditText mActivityIconUri;
    private ListView mListView;
    private UserActivityListAdapter mListAdapter;
    private ArrayList<UserActivitySessionHistoryItem> mHistoryItems;
    private UserActivity mActivity;
    private UserActivitySession mActivitySession;
    private UserActivityChannel mActivityChannel;

    @Nullable
    private UserActivityChannel getUserActivityChannel() {
        UserAccount[] accounts = AccountProviderBroker.getSignInHelper().getUserAccounts();
        if (accounts.length <= 0) {
            setStatus(R.string.status_activities_signin_required);
            return null;
        }

        setStatus(R.string.status_activities_get_channel);
        UserActivityChannel channel = null;
        try {
            // Step #1
            // create a UserActivityChannel for the signed in account
            channel = new UserActivityChannel(accounts[0]);
            setStatus(R.string.status_activities_get_channel_success);
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(R.string.status_activities_get_channel_failed);
        }
        return channel;
    }

    private String createActivityId() {
        return UUID.randomUUID().toString();
    }

    @Nullable
    private UserActivity createUserActivity(UserActivityChannel channel, String activityId) {
        UserActivity activity = null;
        AsyncOperation<UserActivity> activityOperation = channel.getOrCreateUserActivityAsync(activityId);

        try {
            activity = activityOperation.get();
            setStatus(R.string.status_activities_create_activity_success);
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(R.string.status_activities_create_activity_failed);
        }
        return activity;
    }

    /**
     * Initializes the UserActivityFeed.
     */
    public void initializeUserActivityFeed() {
        setStatus(R.string.status_activities_initialize);
        mActivityChannel = getUserActivityChannel();
        setStatus(R.string.status_activities_initialize_complete);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_useractivity, container, false);

        // Initialize the UI elements & event Listeners
        mActivityId = rootView.findViewById(R.id.activityId);
        mDisplayText = rootView.findViewById(R.id.activityDisplayText);
        mActivationUri = rootView.findViewById(R.id.activityUri);
        mActivityIconUri = rootView.findViewById(R.id.activityIconUri);
        mActivityStatus = rootView.findViewById(R.id.activityStatus);

        mNewButton = rootView.findViewById(R.id.activityNewButton);
        mNewButton.setOnClickListener(this);

        mStartButton = rootView.findViewById(R.id.activityStartStopButton);
        mStartButton.setOnClickListener(this);

        mReadButton = rootView.findViewById(R.id.activityReadButton);
        mReadButton.setOnClickListener(this);

        mHistoryItems = new ArrayList<>();
        mListView = rootView.findViewById(R.id.activityListView);
        mListAdapter = new UserActivityListAdapter(getContext(), mHistoryItems);
        mListView.setAdapter(mListAdapter);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        // New activity button clicked
        /*
        Clicking the New button will generate default properties for the activity, create the activity,
        and publish it.
         */
        if (mNewButton.equals(v)) {
            mActivity = null;
            final String newId = createActivityId();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivityId.setText(newId);
                    mDisplayText.setText(R.string.default_activity_display_text);
                    mActivationUri.setText(R.string.default_activity_uri);
                    mActivityIconUri.setText(R.string.default_activity_icon_uri);
                    mStartButton.setText(R.string.button_start_activity_session);
                }
            });

            setStatus(R.string.status_activities_create_activity);

            // ActivityChannel has not been initialized
            if (mActivityChannel == null) {
                setStatus(R.string.status_activities_channel_null);
                return;
            }
            // Step #2
            // Create the UserActivity
            mActivity = createUserActivity(mActivityChannel, mActivityId.getText().toString());

            // Step #3
            // Save (publish) the UserActivity
            // set the properties of the UserActivity
            // Display Text will be shown when the UserActivity is viewed on other devices
            mActivity.getVisualElements().setDisplayText(mDisplayText.getText().toString());
            mActivity.getVisualElements().getAttribution().setIconUri(mActivityIconUri.getText().toString());
            // ActivationURI will determine what is launched when your UserActivity is activated from other devices
            mActivity.setActivationUri(mActivationUri.getText().toString());

            // Saves & publishes the activity
            AsyncOperation<Void> operation = mActivity.saveAsync();
            operation.whenCompleteAsync(new AsyncOperation.ResultBiConsumer<Void, Throwable>() {
                @Override
                public void accept(Void aVoid, Throwable throwable) throws Throwable {
                    if (throwable != null) {
                        setStatus(R.string.status_activities_save_activity_failed);
                    } else {
                        setStatus(R.string.status_activities_save_activity_success);
                    }
                }
            });
        }
        // Start activity button clicked
        else if (mStartButton.equals(v)) {
            if (mActivity != null) {
                String title = mStartButton.getText().toString();
                if (title.equals(getString(R.string.button_start_activity_session))) {
                    mActivitySession = mActivity.createSession();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mStartButton.setText(R.string.button_stop_activity_session);
                        }
                    });

                    setStatus(R.string.status_activities_session_start);
                } else {
                    mActivitySession.close();
                    mActivitySession = null;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mStartButton.setText(R.string.button_start_activity_session);
                        }
                    });

                    setStatus(R.string.status_activities_session_stop);
                }
            } else {
                setStatus(R.string.status_activities_session_activity_required);
            }
        }
        // Read activity button clicked
        else if (mReadButton.equals(v)) {
            mHistoryItems.clear();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListAdapter.notifyDataSetChanged();
                }
            });

            if (mActivityChannel == null) {
                setStatus(R.string.status_activities_channel_null);
                return;
            }
            setStatus(R.string.status_activities_read_activity);

            // Async method to read activities. Last (most recent) 5 activities will be returned
            AsyncOperation<UserActivitySessionHistoryItem[]> operation = mActivityChannel.getRecentUserActivitiesAsync(5);
            operation.whenCompleteAsync(new AsyncOperation.ResultBiConsumer<UserActivitySessionHistoryItem[], Throwable>() {
                @Override
                public void accept(UserActivitySessionHistoryItem[] result, Throwable throwable) throws Throwable {
                    if (throwable != null) {
                        setStatus(R.string.status_activities_read_activity_failed);
                        throwable.printStackTrace();
                    } else {
                        Collections.addAll(mHistoryItems, result);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mListAdapter.notifyDataSetChanged();
                            }
                        });

                        setStatus(R.string.status_activities_read_activity_success);
                    }
                }
            });
        }
    }

    @Override
    String getLogTag() {
        return TAG;
    }

    void setStatus(int resourceId) {
        final String text = getString(resourceId);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivityStatus.setText(text);
            }
        });
        Log.d(TAG, text);
    }
}
