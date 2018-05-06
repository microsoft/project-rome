//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.microsoft.connecteddevices.base.AsyncOperation;
import com.microsoft.connecteddevices.core.Platform;
import com.microsoft.connecteddevices.core.PlatformCreationResult;
import com.microsoft.connecteddevices.hosting.ApplicationRegistration;
import com.microsoft.connecteddevices.hosting.ApplicationRegistrationBuilder;
import com.microsoft.connecteddevices.sampleaccountproviders.MSAAccountProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Most importantly in MainActivity is the platform initialization, happening in init()
 */
public class PlatformBroker {
    // region Member Variables
    private static final String TAG = PlatformBroker.class.getName();
    private static final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";
    private static final String TIMESTAMP_KEY = "TIMESTAMP_KEY";
    private static final String PACKAGE_KEY = "PACKAGE_KEY";
    private static final String PACKAGE_VALUE = "com.microsoft.rome.onesdksample_android";
    // endregion

    public static AsyncOperation<PlatformCreationResult> start(MainActivity mainActivity) {
        // Register the builder to application with attributes and hosting providers .
        ApplicationRegistrationBuilder builder = new ApplicationRegistrationBuilder();
        builder.addAttribute(TIMESTAMP_KEY, getInitialRegistrationDateTime(mainActivity));
        builder.addAttribute(PACKAGE_KEY, PACKAGE_VALUE);

        // We add 2 AppService providers.
        builder.addAppServiceProvider(new PingPongService(mainActivity));
        builder.addAppServiceProvider(new EchoService(mainActivity));
        // We set the only LaunchUri provider.
        builder.setLaunchUriProvider(new SimpleLaunchHandler(mainActivity));

        GcmNotificationProvider gcmNotificationProvider = new GcmNotificationProvider(mainActivity);
        ApplicationRegistration applicationRegistration = builder.buildRegistration();
        MSAAccountProvider signInHelper = AccountProviderBroker.getSignInHelper();

        // Instantiate Platform using the UserAccountProvider the sign in helper provides
        return Platform.createInstanceAsync(mainActivity, gcmNotificationProvider, applicationRegistration, signInHelper);
    }

    private static String getInitialRegistrationDateTime(Activity activity) {
        SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
        // Check that the SharedPreferences has the timestamp. This should be true after the first clean install -> Platform init.
        if (preferences.contains(TIMESTAMP_KEY)) {
            // You must provide a default value. Since we check that key exists we should never get a empty string.
            String timestamp = preferences.getString(TIMESTAMP_KEY, "");
            if (timestamp.isEmpty()) {
                throw new RuntimeException("Failed to get TimeStamp after verifying it exists");
            }
            return timestamp;
        }

        // Create the initial timestamp for RemoteSystemApplication registration and store it in SharedPreferences
        String timestamp = new SimpleDateFormat(DATE_FORMAT, Locale.US).format(new Date());
        preferences.edit().putString(TIMESTAMP_KEY, timestamp).apply();

        return timestamp;
    }
}
