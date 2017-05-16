//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.romanapp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.connecteddevices.AppServiceClientConnectionClosedStatus;
import com.microsoft.connecteddevices.AppServiceClientConnection;
import com.microsoft.connecteddevices.AppServiceClientConnectionStatus;
import com.microsoft.connecteddevices.AppServiceClientResponse;
import com.microsoft.connecteddevices.AppServiceResponseStatus;
import com.microsoft.connecteddevices.ConnectedDevicesException;
import com.microsoft.connecteddevices.IAppServiceClientConnectionListener;
import com.microsoft.connecteddevices.IAppServiceResponseListener;
import com.microsoft.connecteddevices.IRemoteLauncherListener;
import com.microsoft.connecteddevices.Platform;
import com.microsoft.connecteddevices.RemoteLaunchUriStatus;
import com.microsoft.connecteddevices.RemoteLauncher;
import com.microsoft.connecteddevices.RemoteSystemConnectionRequest;

import static com.microsoft.romanapp.DeviceRecyclerActivity.DEVICE_KEY;
import static com.microsoft.connecteddevices.RemoteLaunchUriStatus.SUCCESS;

public class DeviceActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = DeviceActivity.class.getName();

    private static final String APP_SERVICE = ""; // Fill in your app service name
    private static final String APP_IDENTIFIER = ""; // Fill in your app identifier

    private static final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";
    private static final String TIME_STAMP_FORMAT = "HH:mm:ss.SSS";

    private TextView _pingText;
    private Device device;
    private TextView _launchLog;
    private EditText _launchUriEditText;

    private Button _sendPingButton;
    private Button _connectButton;
    private String _id = null;

    private AppServiceClientConnection _appServiceClientConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_view);

        _launchLog = (TextView) findViewById(R.id.launch_log);
        _launchUriEditText = (EditText) findViewById(R.id.launch_uri_edit_text);

        Intent intent = this.getIntent();
        device = intent.getParcelableExtra(DEVICE_KEY);
        if (device == null) {
            Log.e(TAG, "Could not find \"device\" in bundle");
            finish();
            return;
        }

        TextView deviceName = (TextView) findViewById(R.id.device_name);
        deviceName.setText(device.getName());
        TextView deviceType = (TextView) findViewById(R.id.device_type);
        deviceType.setText(device.getType());

        Spinner uriSpinner = (Spinner) findViewById(R.id.launch_uri_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.uri_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        uriSpinner.setAdapter(adapter);
        uriSpinner.setOnItemSelectedListener(this);
        uriSpinner.setSelection(0);

        _pingText = (TextView) findViewById(R.id.ping_value);

        Button launchUriButton = (Button) findViewById(R.id.launch_uri_btn);
        launchUriButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLaunchClick();
            }
        });

        _connectButton = (Button) findViewById(R.id.open_connection_btn);
        _connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onConnectClick();
            }
        });

        _sendPingButton = (Button) findViewById(R.id.send_ping_btn);
        _sendPingButton.setEnabled(false);
        _sendPingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onSendPingClick();
            }
        });
    }

    public void onConnectClick() {
        if (device.getSystem() != null) {
            _connectButton.setEnabled(false);
            logMessage("Waiting for connection response");
            connectAppService(new RemoteSystemConnectionRequest(device.getSystem()));
        }
    }

    public void onSendPingClick() {
        logMessage("Waiting for ping response");

        Bundle message = new Bundle();
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        message.putString("Type", "ping");
        message.putString("CreationDate",  df.format(new Date()));
        message.putString("TargetId", _id);

        try {
            _appServiceClientConnection.sendMessageAsync(message);
        } catch (ConnectedDevicesException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Platform.resume();
    }

    @Override
    public void onPause() {
        Platform.suspend();
        super.onPause();
    }

    public void onLaunchClick() {
        if (device.getSystem() != null) {
            logMessage("Launching Uri");
            launchUri(new RemoteSystemConnectionRequest(device.getSystem()));
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String url = (String) parent.getItemAtPosition(pos);
        _launchUriEditText.setText(url);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private void connectAppService(RemoteSystemConnectionRequest connectionRequest) {
        _appServiceClientConnection = new AppServiceClientConnection(APP_SERVICE, APP_IDENTIFIER, connectionRequest,
                new AppServiceClientConnectionListener(),
                new IAppServiceResponseListener() {
                    @Override
                    public void responseReceived(AppServiceClientResponse response) {
                        AppServiceResponseStatus status = response.getStatus();

                        if (status == AppServiceResponseStatus.SUCCESS)
                        {
                            Bundle bundle = response.getMessage();
                            Log.i(TAG, "Received successful AppService response");
                            logMessage("Received successful AppService response");

                            String dateStr = bundle.getString("CreationDate");

                            DateFormat df = new SimpleDateFormat(DATE_FORMAT);
                            try {
                                Date startDate = df.parse(dateStr);
                                Date nowDate = new Date();
                                long diff = nowDate.getTime() - startDate.getTime();
                                runOnUiThread(new SetPingText(Long.toString(diff)));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                        else
                        {
                            Log.e(TAG, "IAppServiceResponseListener.responseReceived status != SUCCESS");
                            logMessage("Did not receive successful AppService response");
                        }
                    }
                });

        _id = connectionRequest.getRemoteSystem().getId();

        try {
            _appServiceClientConnection.openRemoteAsync();
        } catch (ConnectedDevicesException e) {
            e.printStackTrace();
        }
    }

    private class SetPingText implements Runnable {
        String _str;
        SetPingText(String s) { _str = s; }
        public void run() {
            _pingText.setText(_str);
        }
    }

    private void launchUri(RemoteSystemConnectionRequest connectionRequest) {
        try {
            String url = _launchUriEditText.getText().toString();
            logMessage("Launching URI: " + url + " on " + connectionRequest.getRemoteSystem().getDisplayName());
            new RemoteLauncher().LaunchUriAsync(connectionRequest, url,
                    new IRemoteLauncherListener() {
                        @Override
                        public void onCompleted(RemoteLaunchUriStatus status) {
                            String message;
                            if (status == SUCCESS)
                            {
                                message = "Launch succeeded";
                                Log.i(TAG, message);
                            }
                            else
                            {
                                message = "Launch failed with status " + status.toString();
                                Log.e(TAG, message);
                            }
                            logMessage(message);
                            DeviceActivity.this.runOnUiThread(new PrintToast(message));
                        }
                    });
        } catch (ConnectedDevicesException e) {
            e.printStackTrace();
        }
    }

    private void logMessage(final String message) {
        DeviceActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (_launchLog == null) {
                    return;
                }
                CharSequence prevText = _launchLog.getText();
                String newText = prevText + "\n" + message + " ["+getTimeStamp()+"]";
                _launchLog.setText(newText);
            }
        });
    }

    private class PrintToast implements Runnable {
        String message;
        PrintToast(String s) { message = s; }
        public void run() {
            if (message != null) {
                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                toast.show();
            }
        };
    }

    private String getTimeStamp() {
        DateFormat df = new SimpleDateFormat(TIME_STAMP_FORMAT);
        return df.format(new Date());
    }

    private class AppServiceClientConnectionListener implements IAppServiceClientConnectionListener {

        @Override
        public void onSuccess() {
            Log.i(TAG, "AppServiceClientConnectionListener onSuccess");
            logMessage("AppService connection opened");
            _sendPingButton.setEnabled(true);
            _connectButton.setEnabled(true);
        }

        @Override
        public void onError(AppServiceClientConnectionStatus status) {
            Log.e(TAG, "AppServiceClientConnectionListener onError status [" + status.toString()+"]");
            logMessage("AppService connection error");
            _sendPingButton.setEnabled(false);
            _connectButton.setEnabled(true);
        }

        @Override
        public void onClosed(AppServiceClientConnectionClosedStatus status) {
            Log.i(TAG, "AppServiceClientConnectionListener onClosed status [" + status.toString()+"]");
            logMessage("AppService connection closed");
            _sendPingButton.setEnabled(false);
            _connectButton.setEnabled(true);
        }
    }
}