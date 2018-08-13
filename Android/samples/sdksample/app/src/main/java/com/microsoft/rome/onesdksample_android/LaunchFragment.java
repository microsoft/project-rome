//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.commanding.AppServiceConnection;
import com.microsoft.connecteddevices.commanding.AppServiceConnectionStatus;
import com.microsoft.connecteddevices.commanding.AppServiceResponse;
import com.microsoft.connecteddevices.commanding.AppServiceResponseStatus;
import com.microsoft.connecteddevices.commanding.RemoteLaunchUriStatus;
import com.microsoft.connecteddevices.commanding.RemoteLauncher;
import com.microsoft.connecteddevices.commanding.RemoteSystemConnectionRequest;
import com.microsoft.connecteddevices.discovery.AppServiceDescription;
import com.microsoft.connecteddevices.discovery.RemoteSystemApp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays the functionality for sending LaunchUri commands to RemoteSystems + RemoteApplications.
 * For app service messaging, an app service must be available from another device
 * For this sample, use the UWP test app found here - http://aka.ms/romeapp
 * Messaging can done in 4 steps:
 * Step #1:  Establish an app service connection
 *      App Service name is set in OnCreateView
 *      App Service Connection is established by passing an AppServiceConnectionRequest to AppServiceConnection.openRemoteAsync (see
 * openAppServiceConnection())
 * Step #2:  Create a message to send
 *      Build the message payload: onAppServiceMessageSelected
 * Step #3:  Send a message using the app service connection
 *      Send the message payload: sendMessage
 *      Using the AppServiceConnection, call sendMessageAsync
 *      Set the handler for receiving an app service response, in this case handleAppServiceResponse
 * Step #4:  Get a message response
 *      The handler we set earlier (handleAppServiceResponse) will be called when an app service response is received
 *      Retrieve the message response: appServiceResponse.getMessage()
 */
public class LaunchFragment extends BaseFragment {
    // region Member Variables
    // Launch URI variables
    private static final String TAG = LaunchFragment.class.getName();

    private AtomicInteger mMessageIdLaunch = new AtomicInteger(0);
    private TextView mRemoteSystemsText;
    private RemoteSystemAppWrapper mRemoteSystemApp;
    private StringStorageItemSelectedListener mUriListener;

    private String launchString;

    // App Services variables
    private static final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";

    private LogList mLogList;
    private AppServiceConnection connection = null;
    private AtomicInteger mMessageIdAppServices = new AtomicInteger(0);
    private Map<String, Object> mMessagePayload = null;
    private String mPackageIdentifier;
    private String mAppServiceName;
    // endregion

    // region Overrides
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_launch, container, false);

        mRemoteSystemsText = (TextView)rootView.findViewById(R.id.remoteSystemText);
        if (mRemoteSystemApp != null) {
            mRemoteSystemsText.setText("App: " + mRemoteSystemApp.getDisplayName());
        } else {
            mRemoteSystemsText.setText("Error: no remote system found");
        }

        launchString = getString(R.string.input_launch_uri_default_value);

        Button launchBtn = (Button)rootView.findViewById(R.id.launch_uri_btn);
        launchBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onLaunchUriButtonClicked();
            }
        });
        launchBtn.setText("Launch " + launchString);

        // Grab the default values for the AppService connection information
        // This is the remote app service you want to connect to (in this case, Rome App on Windows)
        mPackageIdentifier = getActivity().getString(R.string.appservice_identifier_romanapp);
        mAppServiceName = getActivity().getString(R.string.appservice_name_romanapp);

        // Set the on-click event for create new AppService message button
        rootView.findViewById(R.id.appservice_new_btn)
            .setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onNewConnectionButtonClicked();
                }
            });

        // Set the on-click event for send AppService message button
        rootView.findViewById(R.id.appservice_message_btn)
            .setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onMessageButtonClicked();
                }
            });

        ListView appServiceLogListView = (ListView)rootView.findViewById(R.id.appservice_log);
        // The ScrollView overrides touch inputs over its children. This results in the
        // inability to use a ListView's scroll feature, so once there are more items in
        // the View size, you cannot scroll through them. So we will disable the ScrollView's
        // touch capability over its child ListView.
        appServiceLogListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        mLogList = new LogList(this, appServiceLogListView);

        return rootView;
    }

    @Override
    String getLogTag() {
        return TAG;
    }
    // endregion

    /**
     * This method is called when a remote system is selected from another fragment
     * (i.e. the RemoteSystemsWatcher fragment)
     * @param remoteSystemApplication
     */
    public void setRemoteSystemApp(RemoteSystemApp remoteSystemApplication) {
        mRemoteSystemApp = new RemoteSystemAppWrapper(remoteSystemApplication);
        mRemoteSystemsText.setText("App: " + mRemoteSystemApp.getDisplayName());
    }

    /**
     * Launch the specified URI to the currently selected SystemApplicationWrapper on a non-UI
     * thread as to not block user interaction, a result of the delay between API calls.
     */
    private void onLaunchUriButtonClicked() {
        launchUri(launchString, mRemoteSystemApp, mMessageIdLaunch.incrementAndGet());
    }

    /**
     * Responsible for calling into the Rome API to launch the given URI and provides
     * the logic to handle the RemoteLaunchUriStatus response.
     * @param uri URI to launch
     * @param system The RemoteSystemAppWrapper target for the launch URI request
     */
    private void launchUri(final String uri, final RemoteSystemAppWrapper system, final long messageId) {
        RemoteLauncher remoteLauncher = new RemoteLauncher();
        AsyncOperation<RemoteLaunchUriStatus> resultOperation =
            remoteLauncher.launchUriAsync(system.getRemoteSystemConnectionRequest(), uri);
        resultOperation.whenCompleteAsync(new AsyncOperation.ResultBiConsumer<RemoteLaunchUriStatus, Throwable>() {
            @Override
            public void accept(RemoteLaunchUriStatus status, Throwable throwable) throws Throwable {
                if (throwable != null) {
                    mLogList.logTraffic(String.format("Failed to launch uri [%s] because of exception [%s]", uri, throwable));
                } else {
                    if (status == RemoteLaunchUriStatus.SUCCESS) {
                        mLogList.logTraffic("Launch URI Success");
                    } else {
                        mLogList.logTraffic(String.format("Failed to launch uri [%s] because of exception [%s]", uri, status));
                    }
                }
            }
        });
    }

    /**
     * Creates an AppService connection with the current identifier and service name and opens the
     * app service connection.
     */
    private void onNewConnectionButtonClicked() {
        connection = new AppServiceConnection();
        connection.setAppServiceDescription(new AppServiceDescription(mAppServiceName, mPackageIdentifier));

        if (mRemoteSystemApp == null) {
            return;
        }

        openAppServiceConnection();
    }

    /**
     * Send the current message payload through all selected AppService connections on a non-UI
     * thread as to not block user interaction, a result of the delay between API calls.
     */
    private void onMessageButtonClicked() {
        mMessagePayload = new HashMap<>();
        // Add the required fields of RomanApp on Windows
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        mMessagePayload.put("Type", "ping");
        mMessagePayload.put("CreationDate", df.format(new Date()));
        mMessagePayload.put("TargetId", "check if this field needs to be included");

        sendMessage(connection, mMessagePayload);
    }

    /**
     * Step #1:  Establish an app service connection
     * Opens the given AppService connection using the connection request. Once the connection is
     * opened, it adds the listeners for request received and close. Catches all exceptions and
     * prints them to show behavior of API surface exceptions.
     */
    private void openAppServiceConnection() {
        String title = String.format("Outbound open connection request");
        mLogList.logTraffic(title);

        RemoteSystemConnectionRequest connectionRequest = mRemoteSystemApp.getRemoteSystemConnectionRequest();

        /*Will asynchronously open the app service connection using the given connection request
        When this is done, we log the traffic in the UI for visibility to the user (for sample purposes)
        We can check the status of the connection to determine whether or not it was successful, and show some UI if it wasn't (in this case
        it is part of the list item)
        AppServiceConnectionStatus can be as follows:
        SUCCESS(0),
        APP_NOT_INSTALLED(1),
        APP_UNAVAILABLE(2),
        APPSERVICE_UNAVAILABLE(3),
        UNKNOWN(4),
        REMOTE_SYSTEM_UNAVAILABLE(5),
        REMOTE_SYSTEM_NOT_SUPPORTEDBYAPP(6),
        NOT_AUTHORIZED(7)
        */
        connection.openRemoteAsync(connectionRequest)
            .thenAcceptAsync(new AsyncOperation.ResultConsumer<AppServiceConnectionStatus>() {
                @Override
                public void accept(AppServiceConnectionStatus appServiceConnectionStatus) throws Throwable {
                    String title = "Inbound open connection response";
                    mLogList.logTraffic(title);

                    if (appServiceConnectionStatus != AppServiceConnectionStatus.SUCCESS) {
                        Log.d("LaunchFragment", "App Service Connection failed: " + appServiceConnectionStatus.toString());
                        return;
                    }
                }
            })
            .exceptionally(new AsyncOperation.ResultFunction<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) throws Throwable {
                    WriteApiException("AsyncOperation<AppServiceConnectionStatus>.get", throwable);
                    return null;
                }
            });
    }

    /**
     * Step #3:  Send a message using the app service connection
     * Send the given Map object through the given AppService connection. Uses an internal messageId
     * for logging purposes.
     * @param connection AppSerivce connection to send the Map payload
     * @param message Payload to be translated to a ValueSet
     */
    private void sendMessage(final AppServiceConnection connection, Map<String, Object> message) {
        final long messageId = mMessageIdAppServices.incrementAndGet();

        connection.sendMessageAsync(message)
            .thenAcceptAsync(new AsyncOperation.ResultConsumer<AppServiceResponse>() {
                @Override
                public void accept(AppServiceResponse appServiceResponse) throws Throwable {
                    String title = "Inbound message response";
                    mLogList.logTraffic(title);

                    handleAppServiceResponse(appServiceResponse, messageId);
                }
            })
            .exceptionally(new AsyncOperation.ResultFunction<Throwable, Void>() {
                @Override
                public Void apply(Throwable throwable) throws Throwable {
                    WriteApiException("SendMessageAsync AsyncOperation<AppServiceResponse>.get", throwable);
                    return null;
                }
            });

        // Construct the traffic log message and log it
        String title = "Outbound message request to " + connection.getAppServiceDescription().getName();
        mLogList.logTraffic(title);
    }

    /**
     * Step #4:  Send the message and get a response
     * Provides the logic for how to handle the response to an AppService request message. Looks for
     * a field containing the date to calculate the round-trip time of the message.
     * @param appServiceResponse
     * @param messageId
     */
    private void handleAppServiceResponse(AppServiceResponse appServiceResponse, long messageId) {
        AppServiceResponseStatus status = appServiceResponse.getStatus();
        if (status == AppServiceResponseStatus.SUCCESS) {
            Map<String, Object> response;

            response = appServiceResponse.getMessage();

            String dateStr = (String)response.get("CreationDate");
            DateFormat df = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            try {
                Date startDate = df.parse(dateStr);
                Date nowDate = new Date();
                long diff = nowDate.getTime() - startDate.getTime();

            } catch (ParseException e) { e.printStackTrace(); }
        }
    }
}
