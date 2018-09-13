//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.util.Log;

import com.microsoft.connecteddevices.commanding.AppServiceConnection;
import com.microsoft.connecteddevices.commanding.AppServiceRequest;
import com.microsoft.connecteddevices.commanding.AppServiceRequestReceivedEventArgs;
import com.microsoft.connecteddevices.discovery.AppServiceInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides the PingPong app service to communicate with remote CDP clients
 */
public class EchoService extends BaseService {
    // region Member Variables
    private static final String TAG = BaseService.class.getName();
    private static final String APP_SERVICE_NAME = "com.microsoft.test.echo";
    // endregion

    public EchoService(MainActivity mainActivity) {
        super(mainActivity, new AppServiceInfo(APP_SERVICE_NAME));
    }

    /**
     * Respond to incoming requests by echoing the request back to the caller
     */
    @Override
    public void onEvent(AppServiceConnection connection, AppServiceRequestReceivedEventArgs args) {
        AppServiceRequest request = args.getRequest();
        Map<String, Object> message = request.getMessage();

        mMainActivity.getHostingFragment().logTrafficMessage(
            "EchoService received AppService request message with payload " + message.toString());

        Map<String, Object> response = new HashMap<>();

        // Echo back whatever was sent to us
        Object payload = message.get("request");
        Log.i(TAG, "Echoing app service payload: " + payload);
        response.put("response", payload);

        request.sendResponseAsync(response);
    }
}
