//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.graphnotifications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.connecteddevices.ConnectedDevicesAccountType;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotification;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationReadState;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationUserActionState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    static ConnectedDevicesManager sConnectedDevicesManager;
    static UserNotificationsManager sNotificationsManager;
    static final ArrayList<UserNotification> sActiveNotifications = new ArrayList<>();

    private static CountDownLatch sLatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the tool bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the tabs with adapter to manage the fragments
        TabLayout tabLayout = findViewById(R.id.tabs);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        // Create the ConnectedDevicesManager
        sConnectedDevicesManager = ConnectedDevicesManager.getConnectedDevicesManager(this);

        sLatch = new CountDownLatch(1);

        Intent intent = getIntent();
        if (intent != null) {
            final String id = intent.getStringExtra(UserNotificationsManager.NOTIFICATION_ID);
            if (id != null && !id.isEmpty()) {
                new Thread(() -> {
                    try {
                        sLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    activateNotification(id);
                }).start();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String id = intent.getStringExtra(UserNotificationsManager.NOTIFICATION_ID);
        activateNotification(id);
    }

    private void activateNotification(String id) {
        if (sNotificationsManager != null){
            boolean found = false;
            for (UserNotification notification : sActiveNotifications) {
                if (notification.getId().equals(id)) {
                    sNotificationsManager.activate(notification);
                    found = true;
                    break;
                }
            }

            if (!found) {
                Log.w(TAG, "Attempted to dismiss notification!");
            }
        }
    }

    static class RunnableManager {
        private static Runnable sNotificationsUpdated = null;

        static void setNotificationsUpdated(Runnable runnable) {
            sNotificationsUpdated = runnable;
        }

        static void runNotificationsUpdated() {
            if (sNotificationsUpdated != null) {
                sNotificationsUpdated.run();
            }
        }
    }

    static void setupNotificationsManager(final Activity activity) {
        if (sConnectedDevicesManager.getSignedInAccount() != null) {
            Log.d(TAG, "Setup Notifications manager");
            sNotificationsManager = sConnectedDevicesManager.getSignedInAccount().getNotificationsManager();

            if (sNotificationsManager != null) {
                sNotificationsManager.addNotificationsUpdatedEventListener(args -> {
                    Log.d(TAG, "Notifications available!");

                    if (sNotificationsManager.HasNewNotifications()) {
                        activity.runOnUiThread(() -> Toast.makeText(activity, "Got new notifications", Toast.LENGTH_SHORT).show());
                    }

                    sActiveNotifications.clear();
                    sActiveNotifications.addAll(sNotificationsManager.HistoricalNotifications());

                    if (sLatch.getCount() == 1) {
                        sLatch.countDown();
                    }

                    RunnableManager.runNotificationsUpdated();
                });
                sNotificationsManager.refresh();
            }
        } else {
            activity.runOnUiThread(() -> Toast.makeText(activity, "No signed-in account found!", Toast.LENGTH_SHORT).show());
        }
    }

    public static class LoginFragment extends Fragment {

        enum LoginState {
            LOGIN_PROGRESS,
            LOGGED_IN_MSA,
            LOGGED_IN_AAD,
            LOGGED_OUT
        }

        private LoginState mState = LoginState.LOGGED_OUT;
        private Button mAadButton;
        private Button mMsaButton;

        public LoginFragment() {
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_login, container, false);

            mAadButton = rootView.findViewById(R.id.login_aad_button);
            mAadButton.setOnClickListener(view -> {
                if (getLoginState() != LoginState.LOGGED_IN_AAD) {
                    getActivity().runOnUiThread(() -> updateView(LoginState.LOGIN_PROGRESS));
                    sConnectedDevicesManager.signInAadAsync(getActivity()).whenCompleteAsync((success, throwable) -> {
                        if ((throwable == null) && (success)) {
                            getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_IN_AAD));
                            setupNotificationsManager(getActivity());

                        } else {
                            Log.e(TAG, "AAD login failed!");
                            getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_OUT));
                        }
                    });
                } else {
                    getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_OUT));
                    sConnectedDevicesManager.logout(getActivity());
                    sNotificationsManager = null;
                    sActiveNotifications.clear();
                    RunnableManager.runNotificationsUpdated();
                }
            });

            mMsaButton = rootView.findViewById(R.id.login_msa_button);
            mMsaButton.setOnClickListener(view -> {
                if (getLoginState() != LoginState.LOGGED_IN_MSA) {
                    getActivity().runOnUiThread(() -> updateView(LoginState.LOGIN_PROGRESS));
                    sConnectedDevicesManager.signInMsaAsync(getActivity()).whenCompleteAsync((success, throwable) -> {
                        if ((throwable == null) && (success)) {
                            getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_IN_MSA));
                            setupNotificationsManager(getActivity());
                        } else {
                            Log.e(TAG, "MSA login failed!");
                            getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_OUT));
                        }
                    });
                } else {
                    getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_OUT));
                    sConnectedDevicesManager.logout(getActivity());
                    sNotificationsManager = null;
                    sActiveNotifications.clear();
                    RunnableManager.runNotificationsUpdated();
                }
            });

            LoginState currentState = LoginState.LOGGED_OUT;
            Account signedInAccount = sConnectedDevicesManager.getSignedInAccount();
            if (signedInAccount != null) {
                currentState = signedInAccount.getAccount().getType() == ConnectedDevicesAccountType.AAD ? LoginState.LOGGED_IN_AAD : LoginState.LOGGED_IN_MSA;
                setupNotificationsManager(getActivity());
            }
            updateView(currentState);

            return rootView;
        }

        synchronized LoginState getLoginState()
        {
            return mState;
        }

        synchronized void updateLoginState(LoginState state)
        {
            mState = state;
        }

        void updateView(LoginState state) {
            updateLoginState(state);

            switch (state) {
                case LOGGED_OUT:
                    mAadButton.setEnabled(true);
                    mAadButton.setText(R.string.login_aad);
                    mMsaButton.setEnabled(true);
                    mMsaButton.setText(R.string.login_msa);
                    break;

                case LOGIN_PROGRESS:
                    mAadButton.setEnabled(false);
                    mAadButton.setText(R.string.login_progress);
                    mMsaButton.setEnabled(false);
                    mMsaButton.setText(R.string.login_progress);
                    break;

                case LOGGED_IN_AAD:
                    mAadButton.setEnabled(true);
                    mAadButton.setText(R.string.logout);
                    mMsaButton.setEnabled(false);
                    mMsaButton.setText(R.string.login_msa);
                    break;

                case LOGGED_IN_MSA:
                    mMsaButton.setEnabled(true);
                    mMsaButton.setText(R.string.logout);
                    mAadButton.setEnabled(false);
                    mAadButton.setText(R.string.login_aad);
                    break;
            }
        }
    }

    static class NotificationArrayAdapter extends ArrayAdapter<UserNotification> {
        private final Activity mActivity;

        public NotificationArrayAdapter(Context context, List<UserNotification> items, Activity activity) {
            super(context, R.layout.notifications_list_item, items);
            mActivity = activity;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final UserNotification notification = sActiveNotifications.get(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.notifications_list_item, parent, false);
            }

            final TextView idView = convertView.findViewById(R.id.notification_id);
            idView.setText(notification.getId());

            final TextView contentView = convertView.findViewById(R.id.notification_content);
            contentView.setText(notification.getContent());

            final TextView userActionStateView = convertView.findViewById(R.id.notification_useractionstate);
            userActionStateView.setText(notification.getUserActionState().toString());

            final Button readButton = convertView.findViewById(R.id.notification_read);
            if (notification.getReadState() == UserNotificationReadState.UNREAD) {
                idView.setTextColor(Color.GREEN);
                readButton.setEnabled(true);
                readButton.setOnClickListener(view -> {
                    readButton.setEnabled(false);
                    sNotificationsManager.markRead(notification);
                });
            } else {
                idView.setTextColor(Color.RED);
                readButton.setEnabled(false);
            }

            final Button dismissButton = convertView.findViewById(R.id.notification_dismiss);
            if (notification.getUserActionState() == UserNotificationUserActionState.NO_INTERACTION) {
                dismissButton.setEnabled(true);
                dismissButton.setOnClickListener(view -> {
                    dismissButton.setEnabled(false);
                    sNotificationsManager.dismiss(notification);
                });
            } else {
                dismissButton.setEnabled(false);
            }

            final Button deleteButton = convertView.findViewById(R.id.notification_delete);
            deleteButton.setOnClickListener(view -> {
                sNotificationsManager.delete(notification);
            });

            return convertView;
        }
    }

    public static class NotificationsFragment extends Fragment {
        private Button mRefreshButton;
        private NotificationArrayAdapter mNotificationArrayAdapter;

        public NotificationsFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mNotificationArrayAdapter = new NotificationArrayAdapter(getContext(), sActiveNotifications, getActivity());

            RunnableManager.setNotificationsUpdated(() -> getActivity().runOnUiThread(() -> mNotificationArrayAdapter.notifyDataSetChanged()));

            View rootView = inflater.inflate(R.layout.fragment_notifications, container, false);
            mRefreshButton = rootView.findViewById(R.id.button_refresh);
            mRefreshButton.setOnClickListener(view -> {
                if (sNotificationsManager != null) {
                    sNotificationsManager.refresh();
                }
            });
            ListView listView = rootView.findViewById(R.id.notificationListView);
            listView.setAdapter(mNotificationArrayAdapter);
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    class SectionsPagerAdapter extends FragmentPagerAdapter {
        private LoginFragment mLoginFragment;
        private NotificationsFragment mNotificationFragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position)
            {
                case 0:
                    if (mLoginFragment == null) {
                        mLoginFragment = new LoginFragment();
                    }

                    return mLoginFragment;
                case 1:
                    if (mNotificationFragment == null) {
                        mNotificationFragment = new NotificationsFragment();
                    }

                    return mNotificationFragment;
            }

            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
    }
}
