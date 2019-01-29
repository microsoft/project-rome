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
import android.util.ArrayMap;
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

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesAccountType;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistration;
import com.microsoft.connecteddevices.EventListener;
import com.microsoft.connecteddevices.signinhelpers.AADSigninHelperAccount;
import com.microsoft.connecteddevices.signinhelpers.MSASigninHelperAccount;
import com.microsoft.connecteddevices.signinhelpers.SigninHelperAccount;
import com.microsoft.connecteddevices.userdata.UserDataFeed;
import com.microsoft.connecteddevices.userdata.UserDataFeedSyncScope;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotification;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationChannel;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationReadState;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationReader;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationReaderOptions;
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
import java.util.Map;
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

    private static SigninHelperAccount sMSAHelperAccount;
    private static SigninHelperAccount sAADHelperAccount;
    private static ConnectedDevicesAccount sLoggedInAccount;
    private static ConnectedDevicesNotificationRegistration sNotificationRegistration;

    private static UserNotificationReader sReader;
    private static CountDownLatch sLatch;

    private static final ArrayList<UserNotification> sNotifications = new ArrayList<>();

    static final String CHANNEL_NAME = "GraphNotificationsChannel001";
    private static final String NOTIFICATION_ID = "ID";

    private enum LoginState {
        LOGGED_IN_MSA,
        LOGGED_IN_AAD,
        LOGGED_OUT
    }

    private static LoginState sState = LoginState.LOGGED_OUT;

    private static synchronized LoginState getAndUpdateLoginState()
    {
        if (sMSAHelperAccount == null || sAADHelperAccount == null || sLoggedInAccount == null) {
            sState = LoginState.LOGGED_OUT;
        } else if (sMSAHelperAccount.isSignedIn() && (sLoggedInAccount.getType() == ConnectedDevicesAccountType.MSA)) {
            sState = LoginState.LOGGED_IN_MSA;
        } else if (sAADHelperAccount.isSignedIn() && (sLoggedInAccount.getType() == ConnectedDevicesAccountType.AAD)) {
            sState = LoginState.LOGGED_IN_AAD;
        } else {
            sState = LoginState.LOGGED_OUT;
        }

        return sState;
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
            channel.setDescription("Graph Notifications Channel");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        if (sMSAHelperAccount == null) {
            final Map<String, String[]> msaScopeOverrides = new ArrayMap<>();
            msaScopeOverrides.put("https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp",
                    new String[] { "https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp",
                            "https://activity.windows.com/Notifications.ReadWrite.CreatedByApp"});
            sMSAHelperAccount = new MSASigninHelperAccount(Secrets.MSA_CLIENT_ID, msaScopeOverrides, getApplicationContext());
        }

        if (sAADHelperAccount == null) {
            sAADHelperAccount = new AADSigninHelperAccount(Secrets.AAD_CLIENT_ID, Secrets.AAD_REDIRECT_URI, getApplicationContext());
        }

        sLatch = new CountDownLatch(1);
        if (PlatformManager.getInstance().getPlatform() == null) {
            PlatformManager.getInstance().createPlatform(getApplicationContext());
        }

        PlatformManager.getInstance().getPlatform().getNotificationRegistrationManager().notificationRegistrationStateChanged().subscribe((connectedDevicesNotificationRegistrationManager, connectedDevicesNotificationRegistrationStateChangedEventArgs) -> {
            Log.i(TAG, "NotificationRegistrationState changed to " + connectedDevicesNotificationRegistrationStateChangedEventArgs.getState().toString());
        });

        tryGetNotificationRegistration();

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

    static void tryGetNotificationRegistration() {
        if (sNotificationRegistration != null) {
            Log.i(TAG, "Already have notification registration");
            return;
        }

        RomeNotificationReceiver receiver = PlatformManager.getInstance().getNotificationReceiver();
        if (receiver != null) {
            receiver.getNotificationRegistrationAsync().whenComplete((connectedDevicesNotificationRegistration, throwable) -> {
                Log.i(TAG, "Got new notification registration");
                sNotificationRegistration = connectedDevicesNotificationRegistration;
            });
        } else {
            Log.i(TAG, "No notification receiver!");
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
                    notification.setUserActionState(UserNotificationUserActionState.ACTIVATED);
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
        boolean firstCreate = true;

        public LoginFragment() {
        }

        public static LoginFragment newInstance() {
            return new LoginFragment();
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mAadButton = rootView.findViewById(R.id.login_aad_button);
            mMsaButton = rootView.findViewById(R.id.login_msa_button);
            LoginState loginState = getAndUpdateLoginState();
            setState(loginState);
            if (firstCreate && (loginState != LoginState.LOGGED_OUT)) {
                firstCreate = false;
                MainActivity.setupChannel(getActivity());
            }

            return rootView;
        }

        void setState(LoginState loginState) {
            switch (loginState) {
                case LOGGED_OUT:
                    mAadButton.setEnabled(true);
                    mAadButton.setText(R.string.login_aad);
                    mAadButton.setOnClickListener(view -> sAADHelperAccount.signIn(getActivity()).whenCompleteAsync((connectedDevicesAccount, throwable) -> {
                        if ((throwable == null) && (connectedDevicesAccount != null)) {
                            sLoggedInAccount = connectedDevicesAccount;
                            PlatformManager.getInstance().getPlatform().getAccountManager().accessTokenRequested().subscribe((accountManager, args)->
                            {
                                sAADHelperAccount.getAccessTokenAsync(args.getRequest().getScopes()).whenCompleteAsync((token, t) -> args.getRequest().completeWithAccessToken(token));
                            });

                            PlatformManager.getInstance().getPlatform().getAccountManager().accessTokenInvalidated().subscribe((connectedDevicesAccountManager, args) -> {
                                // Don't need to do anything here for now
                            });

                            PlatformManager.getInstance().getPlatform().start();

                            tryGetNotificationRegistration();

                            PlatformManager.getInstance().getPlatform().getAccountManager().addAccountAsync(sLoggedInAccount).whenCompleteAsync((connectedDevicesAddAccountResult, throwable12) -> PlatformManager.getInstance().getPlatform().getNotificationRegistrationManager().registerForAccountAsync(sLoggedInAccount, sNotificationRegistration).whenCompleteAsync((aBoolean, throwable1) -> {
                                getActivity().runOnUiThread(()-> setState(getAndUpdateLoginState()));
                                MainActivity.setupChannel(getActivity());
                            }));

                        }
                    }));

                    mMsaButton.setEnabled(true);
                    mMsaButton.setText(R.string.login_msa);
                    mMsaButton.setOnClickListener(view -> sMSAHelperAccount.signIn(getActivity()).whenCompleteAsync((connectedDevicesAccount, throwable) -> {
                        if (throwable == null && connectedDevicesAccount != null) {
                            sLoggedInAccount = connectedDevicesAccount;
                            PlatformManager.getInstance().getPlatform().getAccountManager().accessTokenRequested().subscribe((accountManager, args)-> {
                                sMSAHelperAccount.getAccessTokenAsync(args.getRequest().getScopes()).whenCompleteAsync((token, t) -> {
                                    args.getRequest().completeWithAccessToken(token);
                                });
                            });

                            PlatformManager.getInstance().getPlatform().getAccountManager().accessTokenInvalidated().subscribe((connectedDevicesAccountManager, args) -> {
                                // Don't need to do anything here for now
                            });

                            PlatformManager.getInstance().getPlatform().start();

                            tryGetNotificationRegistration();

                            PlatformManager.getInstance().getPlatform().getAccountManager().addAccountAsync(sLoggedInAccount).whenCompleteAsync((connectedDevicesAddAccountResult, throwable12) -> {
                                PlatformManager.getInstance().getPlatform().getNotificationRegistrationManager().registerForAccountAsync(sLoggedInAccount, sNotificationRegistration).whenCompleteAsync((aBoolean, throwable1) -> {
                                    getActivity().runOnUiThread(()-> setState(getAndUpdateLoginState()));
                                    MainActivity.setupChannel(getActivity());
                                });
                            });
                        }
                    }));
                    break;

                case LOGGED_IN_AAD:
                    mAadButton.setText(R.string.logout);
                    mAadButton.setOnClickListener(view -> PlatformManager.getInstance().getPlatform().getAccountManager().removeAccountAsync(sLoggedInAccount).whenCompleteAsync((connectedDevicesRemoveAccountResult, throwable) -> {
                        sAADHelperAccount.signOut(getActivity()).whenCompleteAsync((connectedDevicesAccount, throwable13) -> {
                            sLoggedInAccount = null;
                            getActivity().runOnUiThread(()-> setState(getAndUpdateLoginState()));
                        });
                    }));
                    mMsaButton.setEnabled(false);
                    break;

                case LOGGED_IN_MSA:
                    mAadButton.setEnabled(false);
                    mMsaButton.setText(R.string.logout);
                    mMsaButton.setOnClickListener(view -> PlatformManager.getInstance().getPlatform().getAccountManager().removeAccountAsync(sLoggedInAccount).whenCompleteAsync((connectedDevicesRemoveAccountResult, throwable) -> {
                        sMSAHelperAccount.signOut(getActivity()).whenCompleteAsync((connectedDevicesAccount, throwable14) -> {
                            sLoggedInAccount = null;
                            getActivity().runOnUiThread(()-> setState(getAndUpdateLoginState()));
                        });
                    }));
                    break;
            }
        }
    }

    static class NotificationArrayAdapter extends ArrayAdapter<UserNotification> {
        private final Activity mActivity;
        NotificationArrayAdapter(Context context, List<UserNotification> items, Activity activity) {
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
                            Log.d(TAG, "Successfully marked notification as read");
                        }
                    });
                });
            } else {
                readButton.setEnabled(false);
            }

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
        NotificationArrayAdapter mNotificationArrayAdapter;
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
                if (getAndUpdateLoginState() != LoginState.LOGGED_OUT) {
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
    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        LoginFragment mLoginFragment;
        NotificationsFragment mNotificationFragment;
        LogFragment mLogFragment;

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
        if (getAndUpdateLoginState() == LoginState.LOGGED_OUT) {
            return;
        }

        UserDataFeed dataFeed = UserDataFeed.getForAccount(sLoggedInAccount, PlatformManager.getInstance().getPlatform(), Secrets.APP_HOST_NAME);
        dataFeed.subscribeToSyncScopesAsync(Arrays.asList(UserNotificationChannel.getSyncScope())).whenCompleteAsync((success, throwable) -> {
            if (success) {
                dataFeed.startSync();
                UserNotificationChannel channel = new UserNotificationChannel(dataFeed);
                UserNotificationReaderOptions options = new UserNotificationReaderOptions();
                sReader = channel.createReaderWithOptions(options);
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
                activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), "Failed to subscribe to sync scopes", Toast.LENGTH_SHORT));
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
