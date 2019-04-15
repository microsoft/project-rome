//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.graphnotifications;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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

import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesAccountType;
import com.microsoft.connecteddevices.userdata.UserDataFeed;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotification;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationChannel;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationReadState;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationReader;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationStatus;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationUpdateResult;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationUserActionState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Request a feed sync, all channels will get updated
        if (id == R.id.action_refresh) {
            if (sNotificationsManager != null){
                sNotificationsManager.refresh();
            }
        }

        return super.onOptionsItemSelected(item);
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
            sNotificationsManager.addNotificationsUpdatedEventListener(args -> {
                Log.d(TAG, "Notifications available!");

                if (sNotificationsManager.HasNewNotifications()) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Got new notifications", Toast.LENGTH_SHORT).show();
                    });
                }

                sActiveNotifications.clear();
                sActiveNotifications.addAll(sNotificationsManager.HistoricalNotifications());

                if (sLatch.getCount() == 1) {
                    sLatch.countDown();
                }

                RunnableManager.runNotificationsUpdated();
            });
            sNotificationsManager.refresh();
        } else {
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "No signed-in account found!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    public static class LoginFragment extends Fragment {
        private Button mAadButton;
        private Button mMsaButton;

        enum LoginState {
            LOGGED_IN_MSA,
            LOGGED_IN_AAD,
            LOGGED_OUT
        }

        private LoginState mState = LoginState.LOGGED_OUT;

        public LoginFragment() {
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_login, container, false);

            mAadButton = rootView.findViewById(R.id.login_aad_button);
            mAadButton.setOnClickListener(view -> {
                if (getLoginState() != LoginState.LOGGED_IN_AAD) {
                    sConnectedDevicesManager.signInAadAsync(getActivity()).whenCompleteAsync((success, throwable) -> {
                        if ((throwable == null) && (success)) {
                            getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_IN_AAD));
                            setupNotificationsManager(getActivity());

                        } else {
                            Log.e(TAG, "AAD login failed!");
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
                    sConnectedDevicesManager.signInMsaAsync(getActivity()).whenCompleteAsync((success, throwable) -> {
                        if ((throwable == null) && (success)) {
                            getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_IN_MSA));
                            setupNotificationsManager(getActivity());
                        } else {
                            Log.e(TAG, "MSA login failed!");
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
            if (sConnectedDevicesManager.getSignedInAccount() != null) {
                currentState = sConnectedDevicesManager.getSignedInAccount().getAccount().getType() == ConnectedDevicesAccountType.AAD ? LoginState.LOGGED_IN_AAD : LoginState.LOGGED_IN_MSA;
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

                case LOGGED_IN_AAD:
                    mAadButton.setText(R.string.logout);
                    mMsaButton.setEnabled(false);
                    break;

                case LOGGED_IN_MSA:
                    mAadButton.setEnabled(false);
                    mMsaButton.setText(R.string.logout);
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

            final Button deleteButton = convertView.findViewById(R.id.notification_delete);
            deleteButton.setOnClickListener(view -> {
                sNotificationsManager.delete(notification);
            });

            if (notification.getUserActionState() == UserNotificationUserActionState.NO_INTERACTION) {
                convertView.setOnClickListener(view -> {
                    sNotificationsManager.activate(notification);
                });
            } else {
                convertView.setOnClickListener(null);
            }

            return convertView;
        }
    }

    public static class NotificationsFragment extends Fragment {
        private NotificationArrayAdapter mNotificationArrayAdapter;

        public NotificationsFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mNotificationArrayAdapter = new NotificationArrayAdapter(getContext(), sActiveNotifications, getActivity());

            RunnableManager.setNotificationsUpdated(() -> {
                getActivity().runOnUiThread(() -> {
                    mNotificationArrayAdapter.notifyDataSetChanged();
                });
            });

            View rootView = inflater.inflate(R.layout.fragment_notifications, container, false);
            ListView listView = rootView.findViewById(R.id.notificationListView);
            listView.setAdapter(mNotificationArrayAdapter);
            return rootView;
        }
    }

    public static class LogFragment extends Fragment {
        private View mRootView;
        private TextView mTextView;
        private File mLogFile;
        private FileReader mReader;
        private StringBuilder mLog = new StringBuilder();
        boolean mStopReading = false;

        public LogFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mRootView = inflater.inflate(R.layout.fragment_log, container, false);
            mTextView = mRootView.findViewById(R.id.log_text);
            mLogFile = new File(getActivity().getApplicationContext().getFilesDir(), "CDPTraces.log");
            try {
                mReader = new FileReader(mLogFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            startReading();
            return mRootView;
        }

        void startReading() {
            new Thread(() -> {
                while (!mStopReading) {
                    boolean stop = false;
                    char[] buff = new char[2048];
                    while (!stop) {
                        int readResult = -1;
                        try {
                            readResult = mReader.read(buff, 0, 2048);
                        } catch (IOException e) {
                            e.printStackTrace();
                            stop = true;
                        }

                        if (readResult == -1) {
                            stop = true;
                        } else {
                            mLog.append(buff, 0, readResult);
                        }
                    }

                    updateText();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        void updateText() {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    char[] endLog = new char[10000];
                    int length = mLog.length();
                    if (length > 10000) {
                        mLog.getChars(length - 10000, length, endLog, 0);
                    } else {
                        mLog.getChars(0, length, endLog, 0);
                    }
                    mTextView.setText(new String(endLog));
                    mRootView.invalidate();
                });
            } else {
                mStopReading = true;
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    class SectionsPagerAdapter extends FragmentPagerAdapter {
        private LoginFragment mLoginFragment;
        private NotificationsFragment mNotificationFragment;
        private LogFragment mLogFragment;

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
                case 2:
                    if  (mLogFragment == null) {
                        mLogFragment = new LogFragment();
                    }

                    return mLogFragment;
            }

            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}
