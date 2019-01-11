package com.microsoft.connecteddevices.graphnotifications;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
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
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationUserActionState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private static ArrayList<UserNotification> sNewNotifications = new ArrayList<>();
    private static final ArrayList<UserNotification> sHistoricalNotifications = new ArrayList<>();

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

        if (PlatformManager.getInstance().getPlatform() == null) {
            PlatformManager.getInstance().createPlatform(getApplicationContext());
        }

        PlatformManager.getInstance().getPlatform().getNotificationRegistrationManager().notificationRegistrationStateChanged().subscribe((connectedDevicesNotificationRegistrationManager, connectedDevicesNotificationRegistrationStateChangedEventArgs) -> {
            Log.i(TAG, "NotificationRegistrationState changed to " + connectedDevicesNotificationRegistrationStateChangedEventArgs.getState().toString());
        });

        tryGetNotificationRegistration();
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static class RunnableManager {
        private static Runnable sNewNotificationsUpdated;
        private static Runnable sHistoryUpdated;

        static void setNewNotificationsUpdated(Runnable runnable) {
            sNewNotificationsUpdated = runnable;
        }

        static void setHistoryUpdated(Runnable runnable) {
            sHistoryUpdated = runnable;
        }

        static Runnable getNewNotificationsUpdated() {
            return sNewNotificationsUpdated;
        }

        static Runnable getHistoryUpdated() {
            return sHistoryUpdated;
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
        NotificationArrayAdapter(Context context, List<UserNotification> items) {
            super(context, R.layout.notifications_list_item, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final UserNotification notification = sNewNotifications.get(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.notifications_list_item, parent, false);
            }

            TextView idView = convertView.findViewById(R.id.notification_id);
            idView.setText(notification.getId());

            TextView textView = convertView.findViewById(R.id.notification_text);
            String content = notification.getContent();
            textView.setText(content);

            convertView.setOnClickListener(view -> {
                notification.setUserActionState(UserNotificationUserActionState.DISMISSED);
                notification.saveAsync();
            });

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
            mNotificationArrayAdapter = new NotificationArrayAdapter(getContext(), sNewNotifications);
            RunnableManager.setNewNotificationsUpdated(() -> {
                Toast.makeText(getContext(), "Got a new notification!", Toast.LENGTH_SHORT).show();
                mNotificationArrayAdapter.notifyDataSetChanged();
            });
            View rootView = inflater.inflate(R.layout.fragment_notifications, container, false);
            ListView listView = rootView.findViewById(R.id.notificationListView);
            listView.setAdapter(mNotificationArrayAdapter);
            return rootView;
        }
    }

    static class HistoryArrayAdapter extends ArrayAdapter<UserNotification> {
        HistoryArrayAdapter(Context context, List<UserNotification> items) {
            super(context, R.layout.notifications_list_item, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final UserNotification notification = sHistoricalNotifications.get(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.notifications_list_item, parent, false);
            }

            TextView idView = convertView.findViewById(R.id.notification_id);
            idView.setText(notification.getId());

            TextView textView = convertView.findViewById(R.id.notification_text);
            textView.setText(notification.getContent());

            convertView.setOnClickListener(view -> {
                notification.setReadState(UserNotificationReadState.READ);
                notification.saveAsync();
            });

            return convertView;
        }
    }

    public static class HistoryFragment extends Fragment {
        private HistoryArrayAdapter mHistoryArrayAdapter;
        public HistoryFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static HistoryFragment newInstance() {
            return new HistoryFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_history, container, false);
            mHistoryArrayAdapter = new HistoryArrayAdapter(getContext(), sHistoricalNotifications);
            RunnableManager.setHistoryUpdated(() -> mHistoryArrayAdapter.notifyDataSetChanged());
            ListView listView = rootView.findViewById(R.id.historyListView);
            listView.setAdapter(mHistoryArrayAdapter);
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        LoginFragment mLoginFragment;
        NotificationsFragment mNotificationFragment;
        HistoryFragment mHistoryFragment;

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
                    if (mHistoryFragment == null) {
                        mHistoryFragment = HistoryFragment.newInstance();
                    }

                    return mHistoryFragment;
            }

            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }

    static void setupChannel(final Activity activity) {
        new Thread(() -> {
            if (getAndUpdateLoginState() == LoginState.LOGGED_OUT){
                return;
            }

            ConnectedDevicesAccount account = sLoggedInAccount;
            ArrayList<UserDataFeedSyncScope> scopes = new ArrayList<>();
            scopes.add(UserNotificationChannel.getSyncScope());
            UserDataFeed dataFeed = UserDataFeed.getForAccount(account, PlatformManager.getInstance().getPlatform(), Secrets.APP_HOST_NAME);
            dataFeed.subscribeToSyncScopesAsync(scopes);
            UserNotificationChannel channel = new UserNotificationChannel(dataFeed);
            UserNotificationReaderOptions options = new UserNotificationReaderOptions();
            sReader = channel.createReaderWithOptions(options);
            sReader.readBatchAsync(Long.MAX_VALUE).thenAccept(userNotifications -> {
                synchronized (sHistoricalNotifications) {
                    for (UserNotification notification : userNotifications) {
                        if (notification.getReadState() == UserNotificationReadState.UNREAD) {
                            sHistoricalNotifications.add(notification);
                        }
                    }
                }

                if (RunnableManager.getHistoryUpdated() != null) {
                    activity.runOnUiThread(RunnableManager.getHistoryUpdated());
                }
            });

            sReader.dataChanged().subscribe((userNotificationReader, args) -> userNotificationReader.readBatchAsync(Long.MAX_VALUE).thenAccept(new AsyncOperation.ResultConsumer<List<UserNotification>>() {
                @Override
                public void accept(List<UserNotification> userNotifications) throws Throwable {
                    boolean updatedNew = false;
                    boolean updatedHistorical = false;
                    synchronized (sHistoricalNotifications) {
                        for (final UserNotification notification : userNotifications) {
                            if ((notification.getStatus() == UserNotificationStatus.ACTIVE) && (notification.getReadState() == UserNotificationReadState.UNREAD)) {
                                switch (notification.getUserActionState()) {
                                    case NO_INTERACTION:
                                        // Brand new notification
                                        for (int i = 0; i < sNewNotifications.size(); i++) {
                                            if (sNewNotifications.get(i).getId().equals(notification.getId())) {
                                                sNewNotifications.remove(i);
                                                break;
                                            }
                                        }

                                        sNewNotifications.add(notification);
                                        updatedNew = true;
                                        break;
                                    case DISMISSED:
                                        // Existing notification we dismissed, move from new -> history
                                        for (int i = 0; i < sNewNotifications.size(); i++) {
                                            if (sNewNotifications.get(i).getId().equals(notification.getId())) {
                                                sNewNotifications.remove(i);
                                                updatedNew = true;
                                                break;
                                            }
                                        }

                                        for (int i = 0; i < sHistoricalNotifications.size(); i++) {
                                            if (sHistoricalNotifications.get(i).getId().equals(notification.getId())) {
                                                sHistoricalNotifications.remove(i);
                                                break;
                                            }
                                        }

                                        sHistoricalNotifications.add(notification);
                                        updatedHistorical = true;
                                        break;
                                    default:
                                        Log.e(TAG, "Somehow got a notification with user action state " + notification.getUserActionState());
                                        // Something unexpected happened, just ignore for future flexibility
                                }
                            } else {
                                // historical item has been updated, should only happen if marked as read
                                for (int i = 0; i < sHistoricalNotifications.size(); i++) {
                                    if (sHistoricalNotifications.get(i).getId().equals(notification.getId())) {
                                        sHistoricalNotifications.remove(i);
                                        updatedHistorical = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (updatedNew && (RunnableManager.getNewNotificationsUpdated() != null)) {
                        activity.runOnUiThread(RunnableManager.getNewNotificationsUpdated());
                    }

                    if (updatedHistorical && (RunnableManager.getHistoryUpdated() != null)) {
                        activity.runOnUiThread(RunnableManager.getHistoryUpdated());
                    }
                }
            }));
        }).start();
    }
}
