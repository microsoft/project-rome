//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.nearshare;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import java.lang.ref.WeakReference;

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.CancellationToken;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesAccountManager;
import com.microsoft.connecteddevices.ConnectedDevicesAccountAddedStatus;
import com.microsoft.connecteddevices.ConnectedDevicesAccessTokenInvalidatedEventArgs;
import com.microsoft.connecteddevices.ConnectedDevicesAccessTokenRequestedEventArgs;
import com.microsoft.connecteddevices.ConnectedDevicesAccountManager;
import com.microsoft.connecteddevices.ConnectedDevicesAddAccountResult;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistration;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationManager;
import com.microsoft.connecteddevices.ConnectedDevicesNotificationRegistrationStateChangedEventArgs;
import com.microsoft.connecteddevices.EventListener;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteSystemConnectionRequest;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;
import com.microsoft.connecteddevices.remotesystems.RemoteSystem;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemAddedEventArgs;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemAuthorizationKind;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemAuthorizationKindFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemDiscoveryType;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemDiscoveryTypeFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemRemovedEventArgs;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemStatusType;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemStatusTypeFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemUpdatedEventArgs;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemWatcher;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemWatcherError;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemWatcherErrorOccurredEventArgs;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareFileProvider;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareHelper;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareSender;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.NearShareStatus;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.ACTION_SEND_MULTIPLE;

/*
 * Wrapper to hold the Rome ConnectedDevicesAccount and RemoteSystemWatcher,
 * as well as additional information required for NearShare.
 * Verification:
 *      1. Discovery: Launch the NearShare App and select check box "Spatially Proximal"
 *      2. At the point, Platform is initialized and started and users can
 *      see list of spatially proximal devices.
 *      3. Send URI: Select a device from the list and click "Send Uri"
 *      4. You will see a toast on the target device with the uri that users can click and launch.
 *      5. Send File(s): Go to photos app or pick any file(s) and select share, pick NearShare app
 *      6. This will open the NearShareApp, continue to select checkbox "Spatially Proximal"
 *      7. Select the device you want to send the file to and click "Send Files" from app
 *      8. Toast will pop up on the target device with options to accept or decline
 *      9. On clicking accept, file transfer with complete and the users can open\save the file.
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    // region Member Variables
    private final static Logger LOG = Logger.getLogger(MainActivity.class.getSimpleName());
    private ConnectedDevicesPlatform mPlatform;
    private RemoteSystemWatcher mRemoteSystemWatcher;
    private DeviceListAdapter mRemoteDeviceListAdapter;
    private RemoteSystem mSelectedRemoteSystem;
    private NearShareSender mNearShareSender;
    private Uri[] mFiles;
    private boolean mWatcherStarted;
    private CheckBox mProximalDiscoveryCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRemoteDeviceListAdapter = new DeviceListAdapter(getApplicationContext());

        requestPermissions();
        initializePlatform();

        initFiles();
        ListView deviceList = (ListView) findViewById(R.id.listRemoteSystems);
        deviceList.setAdapter(mRemoteDeviceListAdapter);
        deviceList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        deviceList.setOnItemClickListener(this);

        findViewById(R.id.btnSendUri)
                .setOnClickListener(v -> {
                    sendUri();
                });

        findViewById(R.id.btnSendFile)
                .setOnClickListener(v -> {
                    sendFile();
                });

        mProximalDiscoveryCheckbox = (CheckBox) findViewById(R.id.chkProximalDiscovery);
        mProximalDiscoveryCheckbox.setOnClickListener(v -> {
            startOrRestartRemoteSystemWatcher();
        });

        mNearShareSender = new NearShareSender();
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

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    /**
     * Request COARSE_LOCATION permission required for nearshare functionality over bluetooth.
     */
    private void requestPermissions() {
        // Request user permission for app to use location services, which is a requirement for Bluetooth.
        Random rng = new Random();
        int permissionRequestCode = rng.nextInt(128);

        int permissionCheck =
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, permissionRequestCode);
        } else {
            LOG.log(Level.INFO, "Requested User Permission To Enable NearShare Prerequisites");
        }
    }

    /**
     * Initialize the platform. This is required before we attempt to use CDP SDK.
     * Steps to start platform:
     * 1. Initialize platform
     * 2. Request Access Token
     * 3. Start Platform
     */
    private void initializePlatform() {
        mPlatform = new ConnectedDevicesPlatform(getApplicationContext());

        ConnectedDevicesAccountManager accountManager = mPlatform.getAccountManager();

        // This subscription isn't necessary for NearShare because it depends on an Anonymous account;
        // but is added in the sample for completeness, in case the app supports other accounts.
        accountManager.accessTokenRequested().subscribe((devicesAccountManager, args) -> onAccessTokenRequested(devicesAccountManager, args));
        accountManager.accessTokenInvalidated().subscribe((devicesAccountManager, args) -> onAccessTokenInvalidated(devicesAccountManager, args));
        // Subscribe to NotificationRegistrationStateChanged event
        mPlatform.getNotificationRegistrationManager().notificationRegistrationStateChanged().subscribe((notificationRegistrationManager, args) -> onNotificationRegistrationStateChanged(notificationRegistrationManager, args));
        mPlatform.start();

        // After platform start, before we can start remotesystem discovery, need to addaccount,
        // NearShare only requires anonymous account, other CDP scenarios may require adding signed in
        // accounts.
        createAndAddAnonymousAccount(mPlatform);
    }

    // region TokenRegistrationCallback
    /**
     * This event is fired when there is a need to request a token. This event should be subscribed and ready to respond before any request is sent out.
     *
     * @param sender ConnectedDevicesAccountManager which is making the request
     * @param args   Contains arguments for the event
     */
    private void onAccessTokenRequested(ConnectedDevicesAccountManager sender, ConnectedDevicesAccessTokenRequestedEventArgs args) {
        LOG.log(Level.INFO, "Token Access Requested");
    }

    /**
     * This event is fired when a token consumer reports a token error. The token provider needs to
     * either refresh their token cache or request a new user login to fix their account setup.
     * If access token in invalidated, refresh token and renew access token.
     *
     * @param sender ConnectedDevicesAccountManager which is making the request
     * @param args   Contains arguments for the event
     */
    private void onAccessTokenInvalidated(ConnectedDevicesAccountManager sender, ConnectedDevicesAccessTokenInvalidatedEventArgs args) {
        LOG.log(Level.INFO, "Token invalidated for account");
    }
    // endregion TokenRegistrationCallback

    /**
     * NearShare just works with anonymous account, signed in accounts are needed when using other CDP
     * features.
     */
    private void createAndAddAnonymousAccount(ConnectedDevicesPlatform platform) {
        ConnectedDevicesAccount account = ConnectedDevicesAccount.getAnonymousAccount();
        platform.getAccountManager().addAccountAsync(account).whenComplete((ConnectedDevicesAddAccountResult result, Throwable throwable) -> {
            if (throwable != null) {
                LOG.log(Level.SEVERE, String.format("AccountManager addAccountAsync returned a throwable: %1$s", throwable.getMessage()));
            } else {
                LOG.log(Level.INFO, "AccountManager : Added account successfully");
            }
        });
    }

    /**
     * Event for when the registration state changes for a given account.
     *
     * @param sender ConnectedDevicesNotificationRegistrationManager which is making the request
     * @param args   Contains arguments for the event
     */
    private void onNotificationRegistrationStateChanged(ConnectedDevicesNotificationRegistrationManager sender, ConnectedDevicesNotificationRegistrationStateChangedEventArgs args) {
        LOG.log(Level.INFO, "NotificationRegistrationStateChanged for account");
    }

    /**
     * This method starts the RemoteSystem discovery process. It sets the corresponding filters
     * to ensure that only spatially proximal devices are listed. It also sets up listeners
     * for important events, such as device added, device updated, and device removed
     */
    private void startOrRestartRemoteSystemWatcher() {
        ArrayList<RemoteSystemFilter> filters = new ArrayList<>();
        if (mProximalDiscoveryCheckbox.isChecked()) {
            filters.add(new RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType.PROXIMAL));
        } else {
            filters.add(new RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType.SPATIALLY_PROXIMAL));
        }

        filters.add(new RemoteSystemStatusTypeFilter(RemoteSystemStatusType.ANY));
        filters.add(new RemoteSystemAuthorizationKindFilter(RemoteSystemAuthorizationKind.ANONYMOUS));

        mRemoteSystemWatcher = new RemoteSystemWatcher(filters);
        final WeakReference<RemoteSystemWatcher> weakRemoteSystemWatcher = new WeakReference<>(mRemoteSystemWatcher);
        weakRemoteSystemWatcher.get().remoteSystemAdded().subscribe(new RemoteSystemAddedListener());
        weakRemoteSystemWatcher.get().remoteSystemUpdated().subscribe(new RemoteSystemUpdatedListener());
        weakRemoteSystemWatcher.get().remoteSystemRemoved().subscribe(new RemoteSystemRemovedListener());
        weakRemoteSystemWatcher.get().errorOccurred().subscribe(new RemoteSystemWatcherErrorOccurredListener());

        // Everytime user toggles checkboc Proximal discovery
        // we restart the watcher with approriate filters to wither do a
        // Proximal or Spatially Proximal discovery. this check is to see if watcher has been previously started
        // if was startetd, we stop it and restart with the new set of filters
        if (mWatcherStarted) {
            weakRemoteSystemWatcher.get().stop();
            mWatcherStarted = false;
            mRemoteDeviceListAdapter.clear();
            mRemoteDeviceListAdapter.notifyDataSetChanged();
        }

        weakRemoteSystemWatcher.get().start();
        mWatcherStarted = true;
    }

    /**
     * Helper Function to initialize the files based on whether user is trying to share
     * single file or multiple files.
     */
    private void initFiles() {
        Intent launchIntent = getIntent();

        if ((ACTION_SEND == launchIntent.getAction()) || (ACTION_SEND_MULTIPLE == launchIntent.getAction())) {
            switch (launchIntent.getAction()) {
                case ACTION_SEND: {
                    mFiles = new Uri[]{launchIntent.getParcelableExtra(Intent.EXTRA_STREAM)};
                    break;
                }
                case ACTION_SEND_MULTIPLE: {
                    ArrayList<Uri> files = launchIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    mFiles = new Uri[files.size()];
                    files.toArray(mFiles);
                    break;
                }
            }
        }
    }

    /**
     * Send URI to the target device using nearshare
     */
    private void sendUri() {
        String uriText = ((EditText) findViewById(R.id.txtUri)).getText().toString();
        if (mSelectedRemoteSystem != null) {
            RemoteSystemConnectionRequest remoteSystemConnectionRequest = new RemoteSystemConnectionRequest(mSelectedRemoteSystem);

            if (mNearShareSender.isNearShareSupported(remoteSystemConnectionRequest)) {
                mNearShareSender.sendUriAsync(remoteSystemConnectionRequest, uriText);
            }
            else
            {
                LOG.log(Level.SEVERE, "NearShare is not supported in this device");
            }
        } else {
            LOG.log(Level.SEVERE, "Please Select a Remote System to SendUri");
        }
    }

    /**
     * Pick Files and Send using NearShare, helper function to pick files and send.
     */
    private AsyncOperation<NearShareStatus> setupAndBeginSendFileAsync() {
        AsyncOperation<NearShareStatus> asyncFileTransferOperation = null;
        if ((null != mFiles) && (null != mSelectedRemoteSystem)) {
            RemoteSystemConnectionRequest remoteSystemConnectionRequest = new RemoteSystemConnectionRequest(mSelectedRemoteSystem);

            if (mNearShareSender.isNearShareSupported(remoteSystemConnectionRequest)) {

                CancellationToken cancellationToken = null;

                findViewById(R.id.btnCancel).setEnabled(true);

                // Call the appropriate api based on the number of files shared to the app.
                if (1 == mFiles.length) {
                    NearShareFileProvider nearShareFileProvider =
                            NearShareHelper.createNearShareFileFromContentUri(mFiles[0], getApplicationContext());

                    asyncFileTransferOperation =
                            mNearShareSender.sendFileAsync(remoteSystemConnectionRequest, nearShareFileProvider);
                } else {
                    NearShareFileProvider[] nearShareFileProviderArray = new NearShareFileProvider[mFiles.length];

                    for (int index = 0; index < mFiles.length; ++index) {
                        nearShareFileProviderArray[index] =
                                NearShareHelper.createNearShareFileFromContentUri(mFiles[index], getApplicationContext());
                    }

                    asyncFileTransferOperation =
                            mNearShareSender.sendFilesAsync(remoteSystemConnectionRequest, nearShareFileProviderArray);

                }
            }
        }
        return asyncFileTransferOperation;
    }

    /**
     * Send file(s) to the target device using nearshare. Select a photo or file and share to the nearshare app.
     */
    private void sendFile() {
        AsyncOperation<NearShareStatus> asyncFileTransferOperation = setupAndBeginSendFileAsync();
        if ((null != mFiles) && (null != mSelectedRemoteSystem)) {
            ((Button) findViewById(R.id.btnCancel))
                    .setOnClickListener(new View.OnClickListener() {
                        private AsyncOperation<NearShareStatus> mAsyncOperation;

                        private View.OnClickListener init(AsyncOperation<NearShareStatus> asyncOperation) {
                            mAsyncOperation = asyncOperation;
                            return this;
                        }

                        /**
                         * Called when a view has been clicked.
                         *
                         * @param v The view that was clicked.
                         */
                        @Override
                        public void onClick(View v) {
                            mAsyncOperation.cancel(true);
                        }
                    }.init(asyncFileTransferOperation));

            asyncFileTransferOperation.whenCompleteAsync(new AsyncOperation.ResultBiConsumer<NearShareStatus, Throwable>() {
                @Override
                public void accept(NearShareStatus nearShareStatus, Throwable throwable) throws Throwable {
                    findViewById(R.id.btnCancel).setEnabled(false);
                    if (null != throwable) {
                        LOG.log(Level.SEVERE, String.format("Exception during file transfer: %1$s", throwable.getMessage()));
                    } else {
                        if (nearShareStatus == NearShareStatus.COMPLETED) {
                            LOG.log(Level.INFO, "File transfer completed");
                        } else {
                            LOG.log(Level.SEVERE, "File transfer failed");
                        }
                    }
                }
            });
        }
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSelectedRemoteSystem = (RemoteSystem)mRemoteDeviceListAdapter.getItem(position);
        mRemoteDeviceListAdapter.setSelectedView(view);
    }

    // region HelperClasses
    private class RemoteSystemAddedListener implements EventListener<RemoteSystemWatcher, RemoteSystemAddedEventArgs> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemAddedEventArgs args) {
            final RemoteSystem remoteSystemParam = args.getRemoteSystem();
            // Calls from the OneSDK are not guaranteed to come back on the given (UI) thread
            // hence explicitly call runOnUiThread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRemoteDeviceListAdapter.addDevice(remoteSystemParam);
                    mRemoteDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class RemoteSystemUpdatedListener implements EventListener<RemoteSystemWatcher, RemoteSystemUpdatedEventArgs> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemUpdatedEventArgs args) {
            LOG.log(Level.INFO, String.format("Updating system: %1$s", args.getRemoteSystem().getDisplayName()));
        }
    }

    private class RemoteSystemRemovedListener implements EventListener<RemoteSystemWatcher, RemoteSystemRemovedEventArgs> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemRemovedEventArgs args) {
            final RemoteSystem remoteSystemParam = args.getRemoteSystem();
            // Calls from the OneSDK are not guaranteed to come back on the given (UI) thread
            // hence explicitly call runOnUiThread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRemoteDeviceListAdapter.removeDevice(remoteSystemParam);
                    mRemoteDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class RemoteSystemWatcherErrorOccurredListener implements EventListener<RemoteSystemWatcher, RemoteSystemWatcherErrorOccurredEventArgs> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemWatcherErrorOccurredEventArgs args) {
            LOG.log(Level.SEVERE, String.format("Discovery error: %1$s", args.getError().toString()));
        }
    }
    // endregion HelperClasses
}