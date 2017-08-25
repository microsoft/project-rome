//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.romanapp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.connecteddevices.AppServiceConnection;
import com.microsoft.connecteddevices.AppServiceConnectionClosedStatus;
import com.microsoft.connecteddevices.AppServiceConnectionStatus;
import com.microsoft.connecteddevices.AppServiceResponse;
import com.microsoft.connecteddevices.AppServiceRequest;
import com.microsoft.connecteddevices.AppServiceResponseStatus;
import com.microsoft.connecteddevices.ConnectedDevicesException;
import com.microsoft.connecteddevices.IAppServiceConnectionListener;
import com.microsoft.connecteddevices.IAppServiceResponseListener;
import com.microsoft.connecteddevices.IAppServiceResponseStatusListener;
import com.microsoft.connecteddevices.IAppServiceRequestListener;
import com.microsoft.connecteddevices.IRemoteLauncherListener;
import com.microsoft.connecteddevices.RemoteLaunchUriStatus;
import com.microsoft.connecteddevices.RemoteLauncher;
import com.microsoft.connecteddevices.RemoteSystemConnectionRequest;

import static com.microsoft.romanapp.DeviceRecyclerActivity.DEVICE_KEY;
import static com.microsoft.connecteddevices.RemoteLaunchUriStatus.SUCCESS;

public class DeviceActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = DeviceActivity.class.getName();

    // Event log colors
    private static final int APP_SERVICE_CONNECTION_COLOR = (0xff) << 24 | (0x52) << 16 | (0x6d) << 8 | (0x9b);
    private static final int APP_SERVICE_REQUEST_COLOR = (0xff) << 24 | (0x01) << 16 | (0x00) << 8 | (0x72);
    private static final int APP_SERVICE_RESPONSE_COLOR = (0xff) << 24 | (0x61) << 16 | (0x00) << 8 | (0x8e);
    private static final int LAUNCH_URI_COLOR = (0xff) << 24 | (0x72) << 16 | (0x5d) << 8 | (0x00);
    private static final int SUCCESS_COLOR = (0xff) << 24 | (0x00) << 16 | (0x8e) << 8 | (0x17);
    private static final int FAILURE_COLOR = (0xff) << 24 | (0x8e) << 16 | (0x00) << 8 | (0x00);

    private static final String APP_SERVICE = "com.microsoft.test.cdppingpongservice";
    private static final String APP_IDENTIFIER = "5085ShawnHenry.RomanTestApp_jsjw7knzsgcce";
    private static final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";
    private static final String TIME_STAMP_FORMAT = "HH:mm:ss.SSS";

    private TextView _pingText;
    private Device device;
    private TextView _launchLog;
    private EditText _launchUriEditText;
    private Button _sendPingButton;
    private Button _connectButton;
    private String _id;

    private long _messageId = 0;
    private AppServiceConnection _appServiceConnection;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public void onConnectClick() {
        if (device.getSystem() != null) {
            _connectButton.setEnabled(false);
            LogMessage(LogLevel.Info, "Waiting for connection response", APP_SERVICE_CONNECTION_COLOR);
            connectAppService(new RemoteSystemConnectionRequest(device.getSystem()));
        }
    }

    public void onSendPingClick() {
        final long messageId = ++_messageId;

        LogMessage(LogLevel.Verbose, "Waiting for ping response for message [" + Long.toString(messageId) + "]", APP_SERVICE_REQUEST_COLOR);

        Bundle message = CreatePingMessage();

        try {
            _appServiceConnection.sendMessageAsync(message, new IAppServiceResponseListener() {
                @Override
                public void responseReceived(AppServiceResponse response) {
                    AppServiceResponseStatus status = response.getStatus();
                    if (status == AppServiceResponseStatus.SUCCESS)
                    {
                        Bundle bundle = response.getMessage();
                        LogMessage(LogLevel.Info, "Received successful AppService response to message [" + Long.toString(messageId) + "]", SUCCESS_COLOR);
                        String dateStr = bundle.getString("CreationDate");
                        DateFormat df = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
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
                        LogMessage(LogLevel.Error, "Did not receive successful AppService response", FAILURE_COLOR);
                    }
                }
            });
        } catch (ConnectedDevicesException e) {
            LogMessage(LogLevel.Error, "Failed to send Ping request through AppServices", FAILURE_COLOR);
            e.printStackTrace();
        }
    }

    private Bundle CreatePingMessage() {
        Bundle bundle = new Bundle();
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        bundle.putString("Type", "ping");
        bundle.putString("CreationDate",  df.format(new Date()));
        bundle.putString("TargetId", _id);

        return bundle;
    }

    private Bundle CreatePongMessage(Bundle bundle) {
        bundle.putString("Type", "pong");
        return bundle;
    }

    public void onLaunchClick() {
        if (device.getSystem() != null) {
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
        _appServiceConnection = new AppServiceConnection(APP_SERVICE, APP_IDENTIFIER, connectionRequest,
                new AppServiceConnectionListener(),
                new IAppServiceRequestListener() {
                    @Override
                    public void requestReceived(AppServiceRequest request) {
                        LogMessage(LogLevel.Info, "Received AppService request. Sending response.", APP_SERVICE_RESPONSE_COLOR);

                        Bundle message = CreatePongMessage(request.getMessage());
                        request.sendResponseAsync(message, new IAppServiceResponseStatusListener() {
                            @Override
                            public void statusReceived(AppServiceResponseStatus status) {
                                if (status == AppServiceResponseStatus.SUCCESS) {
                                    LogMessage(LogLevel.Info, "Successfully sent response.", SUCCESS_COLOR);
                                } else {
                                    LogMessage(LogLevel.Info, "Failed to send response.", FAILURE_COLOR);
                                }
                            }
                        });
                    }
                });

        _id = connectionRequest.getRemoteSystem().getId();

        try {
            _appServiceConnection.openRemoteAsync();
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
            Uri uri = Uri.parse(_launchUriEditText.getText().toString());
            LogMessage(LogLevel.Info, "Launching URI on " + connectionRequest.getRemoteSystem().getDisplayName(), LAUNCH_URI_COLOR);
            RemoteLauncher.LaunchUriAsync(connectionRequest, uri,
                    new IRemoteLauncherListener() {
                        @Override
                        public void onCompleted(RemoteLaunchUriStatus status) {
                            String message;
                            if (status == SUCCESS)
                            {
                                message = "Launch succeeded";
                                LogMessage(LogLevel.Info, message, SUCCESS_COLOR);
                            }
                            else
                            {
                                message = "Launch failed with status " + status.toString();
                                LogMessage(LogLevel.Error, message, FAILURE_COLOR);
                            }
                            DeviceActivity.this.runOnUiThread(new PrintToast(message));
                        }
                    });
        } catch (ConnectedDevicesException e) {
            LogMessage(LogLevel.Error, "Failed to launch URI. Error: " + e.getMessage(), FAILURE_COLOR);
        } catch (android.net.ParseException e) {
            LogMessage(LogLevel.Error, "Failed to parse provided URI. Error: " + e.getMessage(), FAILURE_COLOR);
        }
    }

    private enum LogLevel {
        Error(0),
        Warning(1),
        Info(2),
        Verbose(3);

        public final int id;

        LogLevel(int id) {
            this.id = id;
        }
    }

    private void LogMessage(final LogLevel level, final String message, final int color) {
        final String levelStr;

        switch (level) {
            case Error:
                Log.e(TAG, message);
                levelStr = "Error";
                break;
            case Warning:
                Log.w(TAG, message);
                levelStr = "Warning";
                break;
            case Info:
                Log.i(TAG, message);
                levelStr = "Info";
                break;
            case Verbose:
                Log.v(TAG, message);
                levelStr = "Verbose";
                break;
            default:
                levelStr = "Unknown";
        }

        DeviceActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (_launchLog == null) {
                    return;
                }

                String newText = "\n" + "[" + getTimeStamp() + "] " + message;

                Spannable insertText = new SpannableString(newText);
                insertText.setSpan(new ForegroundColorSpan(color), 0, newText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                _launchLog.append(insertText);
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
        }
    }

    private String getTimeStamp() {
        DateFormat df = new SimpleDateFormat(TIME_STAMP_FORMAT, Locale.getDefault());
        return df.format(new Date());
    }

    private class AppServiceConnectionListener implements IAppServiceConnectionListener {

        @Override
        public void onSuccess() {
            LogMessage(LogLevel.Info, "AppService connection opened", SUCCESS_COLOR);

            DeviceActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _sendPingButton.setEnabled(true);
                    _connectButton.setEnabled(true);
                }
            });
        }

        @Override
        public void onError(AppServiceConnectionStatus status) {
            if (status == AppServiceConnectionStatus.APPSERVICE_UNAVAILABLE)
            {
                LogMessage(LogLevel.Warning, "AppService connection was lost. Attempting to reconnect", FAILURE_COLOR);
                // Since we tried to connect before sending a message, we lost the appservice connection and so need to reconnect
                try {
                    _appServiceConnection.openRemoteAsync();
                } catch (ConnectedDevicesException e) {
                    e.printStackTrace();
                }
            }
            else if (status == AppServiceConnectionStatus.APP_NOT_INSTALLED)
            {
                LogMessage(LogLevel.Warning, "RomanApp is not installed on the target machine.", FAILURE_COLOR);
            }
            else
            {
                LogMessage(LogLevel.Error, "AppService connection error [" + status.toString() + "]", FAILURE_COLOR);
            }
            DeviceActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _sendPingButton.setEnabled(false);
                    _connectButton.setEnabled(true);
                }
            });
        }

        @Override
        public void onClosed(AppServiceConnectionClosedStatus status) {
            LogMessage(LogLevel.Error, "AppService connection closed [" + status.toString() + "]", FAILURE_COLOR);
            DeviceActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _sendPingButton.setEnabled(false);
                    _connectButton.setEnabled(true);
                }
            });
        }
    }
}