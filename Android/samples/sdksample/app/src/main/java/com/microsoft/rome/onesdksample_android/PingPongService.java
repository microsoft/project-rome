//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.microsoft.connecteddevices.remotesystems.commanding.AppServiceConnection;
import com.microsoft.connecteddevices.remotesystems.commanding.AppServiceRequest;
import com.microsoft.connecteddevices.remotesystems.commanding.AppServiceRequestReceivedEventArgs;
import com.microsoft.connecteddevices.remotesystems.AppServiceInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the PingPong app service to communicate with remote CDP clients
 */
public class PingPongService extends BaseService {
    // region Member Variables
    private static final String TAG = BaseService.class.getName();

    private static final String APP_SERVICE_NAME = "com.microsoft.test.cdppingpongservice";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    // endregion

    public PingPongService(MainActivity mainActivity) {
        super(mainActivity, new AppServiceInfo(APP_SERVICE_NAME));
    }

    // region Constructor
    /**
     * Respond to an incoming app service request
     * @param connection The app service connection
     * @param args The details of the incoming request
     */
    @Override
    public void onEvent(AppServiceConnection connection, AppServiceRequestReceivedEventArgs args) {
        AppServiceRequest request = args.getRequest();
        Map<String, Object> message = request.getMessage();

        mMainActivity.getHostingFragment().logTrafficMessage(
            "PingPongService received AppService request message with payload " + message.toString());

        Map<String, Object> response = null;

        Object type = message.get("Type");

        if (type instanceof String) {
            switch ((String)type) {
            case "LaunchUri": response = handleLaunchUriRequest(message); break;

            case "ping": response = handlePingRequest(message); break;

            default: response = handleDefaultRequest(message); break;
            }
        } else {
            response = handleDefaultRequest(message);
        }

        request.sendResponseAsync(response);
    }
    // endregion

    /**
     * Handle a AppService implemented "LaunchUri" requests by launching a URI
     */
    private Map<String, Object> handleLaunchUriRequest(Map<String, Object> message) {
        Object uriStr = message.get("Uri");
        if (!(uriStr instanceof String)) {
            uriStr = "http://bing.com";
        }

        // Launch the URI we were given
        Log.i(TAG, "Received app service command to launch URI " + uriStr);
        Uri uri = Uri.parse((String)uriStr);
        Intent launchIntent = new Intent(Intent.ACTION_VIEW, uri);
        mMainActivity.startActivity(launchIntent);

        // Respond to our caller
        Map<String, Object> response = new HashMap<>();
        response.put("Type", "Ok");
        response.put("CreationDate", DATE_FORMAT.format(new Date()));

        return response;
    }

    /**
     * Handle "ping" requests by echoing the payload back to the caller
     */
    private Map<String, Object> handlePingRequest(Map<String, Object> message) {
        // Mirror the request back to the caller, but with the Type field changed to "pong"
        Map<String, Object> response = new HashMap<>();
        response.put("Type", "pong");
        response.put("CreationDate", message.get("CreationDate"));
        response.put("TargetId", message.get("TargetId"));

        Log.i(TAG, "Received ping request");
        return response;
    }

    /**
     * Handle "CCSCommand" and other requests by sending an Ack
     */
    private Map<String, Object> handleDefaultRequest(Map<String, Object> message) {
        Map<String, Object> response = new HashMap<>();
        response.put("Type", "Ack");
        response.put("CreationDate", DATE_FORMAT.format(new Date()));

        Log.i(TAG, "Received generic app service request");
        return response;
    }
}
