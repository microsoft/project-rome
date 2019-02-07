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

    private static ConnectedDevicesManager mConnectedDevicesManager;
    private static UserNotificationChannel sChannel;
    private static UserNotificationReader sReader;

    private static final ArrayList<UserNotification> sNotifications = new ArrayList<>();

    private static CountDownLatch sLatch;

    private static final String CHANNEL_NAME = "GraphNotificationsChannel001";
    private static final String NOTIFICATION_ID = "ID";

    private enum LoginState {
        LOGGED_IN_MSA,
        LOGGED_IN_AAD,
        LOGGED_OUT
    }

    private static LoginState mState = LoginState.LOGGED_OUT;

    private static synchronized LoginState getLoginState()
    {
        return mState;
    }

    private static synchronized void updateLoginState(LoginState state)
    {
        mState = state;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_NAME, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("GraphNotificationsSample Channel");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        // Create the ConnectedDevicesManager
        mConnectedDevicesManager = ConnectedDevicesManager.getConnectedDevicesManager((Context)this);

        sLatch = new CountDownLatch(1);

        Intent intent = getIntent();
        if (intent != null) {
            final String id = intent.getStringExtra(NOTIFICATION_ID);
            if (id != null && id.equals("")) {
                new Thread(() -> {
                    try {
                        sLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    dismissNotification(id);
                }).start();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String id = intent.getStringExtra(NOTIFICATION_ID);
        dismissNotification(id);
    }

    private void dismissNotification(String id) {
        synchronized (sNotifications) {
            boolean found = false;
            for (UserNotification notification : sNotifications) {
                if (notification.getId().equals(id)) {
                    notification.setUserActionState(UserNotificationUserActionState.DISMISSED);
                    notification.saveAsync();
                    found = true;
                    break;
                }
            }

            if (!found) {
                Log.w(TAG, "Attempted to dismiss missing notification!");
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static class RunnableManager {
        private static Runnable sNotificationsUpdated;

        static void setNotificationsUpdated(Runnable runnable) {
            sNotificationsUpdated = runnable;
        }

        static Runnable getNotificationsUpdated() {
            return sNotificationsUpdated;
        }
    }

    public static class LoginFragment extends Fragment {
        private Button mAadButton;
        private Button mMsaButton;

        public LoginFragment() {
        }

        public static LoginFragment newInstance() {
            return new LoginFragment();
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_login, container, false);

            mAadButton = rootView.findViewById(R.id.login_aad_button);
            mAadButton.setOnClickListener(view -> mConnectedDevicesManager.signInAadAsync(getActivity()).whenCompleteAsync((success, throwable) -> {
                if ((throwable == null) && (success)) {
                    if (getLoginState() != LoginState.LOGGED_IN_AAD) {
                        getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_IN_AAD));
                        MainActivity.setupChannel(getActivity());
                    } else {
                        getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_OUT));
                        mConnectedDevicesManager.logout(getActivity());
                    }
                } else {
                    Log.d(TAG, "AAD login failed!");
                }
            }));

            mMsaButton = rootView.findViewById(R.id.login_msa_button);
            mMsaButton.setOnClickListener(view -> mConnectedDevicesManager.signInMsaAsync(getActivity()).whenCompleteAsync((success, throwable) -> {
                if ((throwable == null) && (success)) {
                    if (getLoginState() != LoginState.LOGGED_IN_AAD) {
                        getActivity().runOnUiThread(()-> updateView(LoginState.LOGGED_IN_MSA));
                        MainActivity.setupChannel(getActivity());
                    } else {
                        getActivity().runOnUiThread(() -> updateView(LoginState.LOGGED_OUT));
                        mConnectedDevicesManager.logout(getActivity());
                    }
                }
                else {
                    Log.d(TAG, "MSA login failed!");
                }
            }));

            updateView(getLoginState());

            return rootView;
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

    public static class NotificationArrayAdapter extends ArrayAdapter<UserNotification> {
        private final Activity mActivity;

        public NotificationArrayAdapter(Context context, List<UserNotification> items, Activity activity) {
            super(context, R.layout.notifications_list_item, items);
            mActivity = activity;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final UserNotification notification = sNotifications.get(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.notifications_list_item, parent, false);
            }

            TextView idView = convertView.findViewById(R.id.notification_id);
            idView.setText(notification.getId());

            TextView textView = convertView.findViewById(R.id.notification_text);
            String content = notification.getContent();
            textView.setText(content);

            TextView userActionStateView = convertView.findViewById(R.id.notification_useractionstate);
            userActionStateView.setText((notification.getUserActionState() == UserNotificationUserActionState.NO_INTERACTION)
                    ? "NO_INTERACTION" : "ACTIVATED");

            final Button readButton = convertView.findViewById(R.id.notification_read);
            if (notification.getReadState() == UserNotificationReadState.UNREAD) {
                readButton.setEnabled(true);
                readButton.setOnClickListener(view -> {
                    readButton.setEnabled(false);
                    notification.setReadState(UserNotificationReadState.READ);
                    notification.saveAsync().whenCompleteAsync((userNotificationUpdateResult, throwable) -> {
                        if (throwable == null && userNotificationUpdateResult != null && userNotificationUpdateResult.getSucceeded()) {
                            Log.d(TAG, "Successfully marked the notification as read");
                        }
                    });
                });
            } else {
                readButton.setEnabled(false);
            }

            final Button deleteButton = convertView.findViewById(R.id.notification_delete);
            deleteButton.setOnClickListener(view -> {
                sChannel.deleteUserNotificationAsync(notification.getId()).whenCompleteAsync((userNotificationUpdateResult, throwable) -> {
                    if (throwable == null && userNotificationUpdateResult != null && userNotificationUpdateResult.getSucceeded()) {
                        Log.d(TAG, "Successfully deleted the notification");
                    }
                });
            });

            if (notification.getUserActionState() == UserNotificationUserActionState.NO_INTERACTION) {
                convertView.setOnClickListener(view -> {
                    clearNotification(mActivity, notification.getId());
                    notification.setUserActionState(UserNotificationUserActionState.ACTIVATED);
                    notification.saveAsync();
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

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static NotificationsFragment newInstance() {
            return new NotificationsFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mNotificationArrayAdapter = new NotificationArrayAdapter(getContext(), sNotifications, getActivity());
            RunnableManager.setNotificationsUpdated(() -> {
                if (getLoginState() != LoginState.LOGGED_OUT) {
                    Toast.makeText(getContext(), "Got a new notification update!", Toast.LENGTH_SHORT).show();
                }

                mNotificationArrayAdapter.notifyDataSetChanged();
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

        public static LogFragment newInstance() {
            return new LogFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            mRootView = inflater.inflate(R.layout.fragment_log, container, false);
            mTextView = mRootView.findViewById(R.id.log_text);
            mLogFile = new File(getActivity().getApplicationContext().getExternalFilesDir(null), "CDPTraces.log");
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

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position)
            {
                case 0:
                    if (mLoginFragment == null) {
                        mLoginFragment = LoginFragment.newInstance();
                    }

                    return mLoginFragment;
                case 1:
                    if (mNotificationFragment == null) {
                        mNotificationFragment = NotificationsFragment.newInstance();
                    }

                    return mNotificationFragment;
                case 2:
                    if  (mLogFragment == null) {
                        mLogFragment = LogFragment.newInstance();
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

    static void handleNotifications(final List<UserNotification> userNotifications, final Activity activity) {
        activity.runOnUiThread(() -> {
            synchronized (sNotifications) {
                for (final UserNotification notification : userNotifications) {
                    for (int i = 0; i < sNotifications.size(); i++) {
                        if (sNotifications.get(i).getId().equals(notification.getId())) {
                            sNotifications.remove(i);
                            break;
                        }
                    }

                    if (notification.getStatus() == UserNotificationStatus.ACTIVE) {
                        sNotifications.add(0, notification);

                        if (notification.getUserActionState() == UserNotificationUserActionState.NO_INTERACTION && notification.getReadState() == UserNotificationReadState.UNREAD) {
                            addNotification(activity, notification.getContent(), notification.getId());
                        } else {
                            clearNotification(activity, notification.getId());
                        }
                    } else {
                        clearNotification(activity, notification.getId());
                    }
                }

                if (RunnableManager.getNotificationsUpdated() != null) {
                    RunnableManager.getNotificationsUpdated().run();
                }
            }
        });
    }

    static void setupChannel(final Activity activity) {
        UserDataFeed dataFeed = UserDataFeed.getForAccount(mConnectedDevicesManager.getSignedInAccount().getAccount(), mConnectedDevicesManager.getPlatform(), Secrets.APP_HOST_NAME);
        dataFeed.subscribeToSyncScopesAsync(Arrays.asList(UserNotificationChannel.getSyncScope())).whenCompleteAsync((success, throwable) -> {
            if (success) {
                dataFeed.startSync();

                sChannel = new UserNotificationChannel(dataFeed);

                sReader = sChannel.createReader();
                sReader.readBatchAsync(Long.MAX_VALUE).thenAccept(userNotifications -> {
                    handleNotifications(userNotifications, activity);
                    if (sLatch.getCount() == 1) {
                        sLatch.countDown();
                    }
                });
                sReader.dataChanged().subscribe((userNotificationReader, aVoid) -> userNotificationReader.readBatchAsync(Long.MAX_VALUE).thenAccept(userNotifications -> {
                    handleNotifications(userNotifications, activity);
                }));
            } else {
                activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), "Failed to setup USerDataFeed for notifications", Toast.LENGTH_SHORT));
            }
        });
    }

    static void addNotification(Activity activity, String message, String notificationId) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, MainActivity.CHANNEL_NAME)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("New MSGraph Notification!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(activity).notify(notificationId.hashCode(), builder.build());
    }

    static void clearNotification(Activity activity, String notificationId) {
        ((NotificationManager)activity.getSystemService(NOTIFICATION_SERVICE)).cancel(notificationId.hashCode());
    }

}
