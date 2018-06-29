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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.base.CancellationToken;
import com.microsoft.connecteddevices.base.EventListener;
import com.microsoft.connecteddevices.commanding.RemoteSystemConnectionRequest;
import com.microsoft.connecteddevices.core.Platform;
import com.microsoft.connecteddevices.discovery.RemoteSystem;
import com.microsoft.connecteddevices.discovery.RemoteSystemAuthorizationKind;
import com.microsoft.connecteddevices.discovery.RemoteSystemAuthorizationKindFilter;
import com.microsoft.connecteddevices.discovery.RemoteSystemDiscoveryType;
import com.microsoft.connecteddevices.discovery.RemoteSystemDiscoveryTypeFilter;
import com.microsoft.connecteddevices.discovery.RemoteSystemFilter;
import com.microsoft.connecteddevices.discovery.RemoteSystemStatusType;
import com.microsoft.connecteddevices.discovery.RemoteSystemStatusTypeFilter;
import com.microsoft.connecteddevices.discovery.RemoteSystemWatcher;
import com.microsoft.connecteddevices.discovery.RemoteSystemWatcherError;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.ACTION_SEND_MULTIPLE;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private final static Logger LOG = Logger.getLogger(MainActivity.class.getSimpleName());

    private Platform mPlatform;
    private RemoteSystemWatcher mRemoteSystemWatcher;
    private DeviceListAdapter mRemoteDeviceListAdapter;
    private RemoteSystem mSelectedRemoteSystem = null;
    private NearShareSender mNearShareSender = null;
    private Uri[] mFiles;
    private boolean mWatcherStarted = false;
    private CheckBox mProximalDiscoveryCheckbox = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRemoteDeviceListAdapter = new DeviceListAdapter(getApplicationContext());

        initializePlatform();
        requestPermissions();
        startDiscovery();

        Intent launchIntent = getIntent();

        if ((ACTION_SEND == launchIntent.getAction()) || (ACTION_SEND_MULTIPLE == launchIntent.getAction())) {
            switch (launchIntent.getAction()) {
            case ACTION_SEND: {
                mFiles = new Uri[] { launchIntent.getParcelableExtra(Intent.EXTRA_STREAM) };
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

        ListView deviceList = (ListView)findViewById(R.id.listRemoteSystems);
        deviceList.setAdapter(mRemoteDeviceListAdapter);

        deviceList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        deviceList.setOnItemClickListener(this);

        findViewById(R.id.btnSendUri)
            .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendUri();
                }
            });

        findViewById(R.id.btnSendFile)
            .setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendFile();
                }
            });

        mProximalDiscoveryCheckbox = (CheckBox)findViewById(R.id.chkProximalDiscovery);

        mProximalDiscoveryCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiscovery();
            }
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

    /**
     * Initialize the platform. This is required before we attempt to use CDP SDK.
     */

    private void initializePlatform() {
        mPlatform = new Platform(getApplicationContext(), null, null);
    }

    /**
     * This method starts the RemoteSystem discovery process. It sets the corresponding filters
     * to ensure that only spatially proximal devices are listed. It also sets up listenrs
     * for important events, such as device added, device updated, and device removed
     */

    private void startDiscovery() {
        try {
            ArrayList<RemoteSystemFilter> filters = new ArrayList<>();

            if (mProximalDiscoveryCheckbox.isChecked()) {
                filters.add(new RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType.PROXIMAL));
            } else {
                filters.add(new RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType.SPATIALLY_PROXIMAL));
            }

            filters.add(new RemoteSystemStatusTypeFilter(RemoteSystemStatusType.ANY));
            filters.add(new RemoteSystemAuthorizationKindFilter(RemoteSystemAuthorizationKind.ANONYMOUS));

            mRemoteSystemWatcher = new RemoteSystemWatcher(filters.toArray(new RemoteSystemFilter[filters.size()]));

            mRemoteSystemWatcher.addRemoteSystemAddedListener(new RemoteSystemAddedListener());
            mRemoteSystemWatcher.addRemoteSystemUpdatedListener(new RemoteSystemUpdatedListener());
            mRemoteSystemWatcher.addRemoteSystemRemovedListener(new RemoteSystemRemovedListener());
            mRemoteSystemWatcher.addErrorOccurredListener(new RemoteSystemErrorListener());

            if (mWatcherStarted) {
                mRemoteSystemWatcher.stop();
                mWatcherStarted = false;
                mRemoteDeviceListAdapter.clear();
                mRemoteDeviceListAdapter.notifyDataSetChanged();
            }

            mRemoteSystemWatcher.start();
            mWatcherStarted = true;
        } catch (Exception exception) { LOG.log(Level.SEVERE, String.format("Discovery failed: %1$s", exception.getMessage())); }
    }

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
                this, new String[] { android.Manifest.permission.ACCESS_COARSE_LOCATION }, permissionRequestCode);
        } else {
            LOG.log(Level.SEVERE, "ACCESS_COARSE_LOCATION permission denied");
        }
    }

    /**
     * Send URI to the target device using nearshare
     */

    private void sendUri() {
        String uriText = ((EditText)findViewById(R.id.txtUri)).getText().toString();
        RemoteSystemConnectionRequest remoteSystemConnectionRequest = new RemoteSystemConnectionRequest(mSelectedRemoteSystem);

        if (mNearShareSender.isNearShareSupported(remoteSystemConnectionRequest)) {
            mNearShareSender.sendUriAsync(remoteSystemConnectionRequest, uriText);
        }
    }

    /**
     * Send file(s) to the target device using nearshare. This functionality requires the nearshare app to be used as a share target. The
     * operation starts with
     * an app such as the photos app. Select a photo and share to the nearshare app.
     */

    private void sendFile() {
        if ((null != mFiles) && (null != mSelectedRemoteSystem)) {
            RemoteSystemConnectionRequest remoteSystemConnectionRequest = new RemoteSystemConnectionRequest(mSelectedRemoteSystem);

            if (mNearShareSender.isNearShareSupported(remoteSystemConnectionRequest)) {
                AsyncOperation<NearShareStatus> asyncFileTransferOperation = null;
                ProgressCallback progressCallback = new NearShareProgressCallback();
                CancellationToken cancellationToken = null;

                findViewById(R.id.btnCancel).setEnabled(true);

                // Call the appropriate api based on the number of files shared to the app.
                if (1 == mFiles.length) {
                    NearShareFileProvider nearShareFileProvider =
                        NearShareHelper.createNearShareFileFromContentUri(mFiles[0], getApplicationContext());

                    asyncFileTransferOperation =
                        mNearShareSender.sendFileAsync(remoteSystemConnectionRequest, nearShareFileProvider, progressCallback);
                } else {
                    NearShareFileProvider[] nearShareFileProviderArray = new NearShareFileProvider[mFiles.length];

                    for (int index = 0; index < mFiles.length; ++index) {
                        nearShareFileProviderArray[index] =
                            NearShareHelper.createNearShareFileFromContentUri(mFiles[index], getApplicationContext());
                    }

                    asyncFileTransferOperation =
                        mNearShareSender.sendFilesAsync(remoteSystemConnectionRequest, nearShareFileProviderArray, progressCallback);
                }

                ((Button)findViewById(R.id.btnCancel))
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

    private class RemoteSystemAddedListener implements EventListener<RemoteSystemWatcher, RemoteSystem> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystem remoteSystem) {
            final RemoteSystem remoteSystemParam = remoteSystem;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRemoteDeviceListAdapter.addDevice(remoteSystemParam);
                    mRemoteDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class RemoteSystemUpdatedListener implements EventListener<RemoteSystemWatcher, RemoteSystem> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystem remoteSystem) {
            LOG.log(Level.INFO, String.format("Updating system: %1$s", remoteSystem.getDisplayName()));
        }
    }

    private class RemoteSystemRemovedListener implements EventListener<RemoteSystemWatcher, RemoteSystem> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystem remoteSystem) {
            final RemoteSystem remoteSystemParam = remoteSystem;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRemoteDeviceListAdapter.removeDevice(remoteSystemParam);
                    mRemoteDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private class RemoteSystemErrorListener implements EventListener<RemoteSystemWatcher, RemoteSystemWatcherError> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemWatcherError remoteSystemWatcherError) {
            LOG.log(Level.INFO, String.format("Discovery error: %1$s", remoteSystemWatcherError.toString()));
        }
    }
    // region HelperClasses
}
