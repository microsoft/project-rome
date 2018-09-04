//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.microsoft.connecteddevices.base.EventListener;
import com.microsoft.connecteddevices.commanding.IRemoteSystemAppRegistration;
import com.microsoft.connecteddevices.commanding.RemoteSystemAppRegistrationStatus;
import com.microsoft.connecteddevices.core.NotificationProvider;
import com.microsoft.connecteddevices.core.Platform;
import com.microsoft.connecteddevices.core.UserAccount;
import com.microsoft.connecteddevices.core.UserAccountProvider;
import com.microsoft.connecteddevices.hosting.AppServiceProvider;
import com.microsoft.connecteddevices.hosting.LaunchUriProvider;
import com.microsoft.connecteddevices.hosting.RemoteSystemAppRegistrationBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Most importantly in MainActivity is the platform initialization, happening in init()
 */
public class PlatformBroker {
    // region Member Variables
    private static final String TAG = PlatformBroker.class.getName();

    public static final String DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";
    public static final String TIMESTAMP_KEY = "TIMESTAMP_KEY";
    public static final String PACKAGE_KEY = "PACKAGE_KEY";
    public static final String GUID_KEY = "GUID_KEY";
    public static final String PACKAGE_VALUE = "com.microsoft.oneRomanApp";

    private static Platform sPlatform;

    private PlatformBroker() { }

    public static synchronized Platform getPlatform() {
        return sPlatform;
    }

    public static synchronized Platform createPlatform(Context context, UserAccountProvider accountProvider, NotificationProvider notificationProvider) {
        sPlatform = new Platform(context, accountProvider, notificationProvider);
        return sPlatform;
    }

    public static synchronized Platform getOrCreatePlatform(Context context, UserAccountProvider accountProvider, NotificationProvider notificationProvider) {
        Platform platform = getPlatform();

        if (platform == null) {
            platform = createPlatform(context, accountProvider, notificationProvider);
        }

        return platform;
    }

    public static void register(Context context, ArrayList<AppServiceProvider> appServiceProviders, LaunchUriProvider launchUriProvider, EventListener<UserAccount, RemoteSystemAppRegistrationStatus> listener) {
        // Initialize the platform with all possible services
        RemoteSystemAppRegistrationBuilder builder = new RemoteSystemAppRegistrationBuilder();
        builder.addAttribute(TIMESTAMP_KEY, getInitialRegistrationDateTime(context));
        builder.addAttribute(PACKAGE_KEY, PACKAGE_VALUE);

        // Add the given AppService and LaunchUri Providers to the registration builder
        if (appServiceProviders != null) {
            for (AppServiceProvider provider : appServiceProviders) {
                builder.addAppServiceProvider(provider);
            }
        }
        if (launchUriProvider != null) {
            builder.setLaunchUriProvider(launchUriProvider);
        }

        IRemoteSystemAppRegistration registration = builder.buildRegistration();
        // Add an EventListener to handle registration completion
        registration.addRemoteSystemAppRegistrationStatusChangedListener(listener);
        registration.save();
    }

    /**
     * Grab the initial registration date-time if one is found, otherwise generate a new one.
     * @param context
     * @return Datetime to insert into the RemoteSystemAppRegistrationBuilder
     */
    private static String getInitialRegistrationDateTime(final Context context) {
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

        String timestamp;
        // Check that the SharedPreferences has the timestamp. This should be true after the first clean install -> Platform init.
        if (preferences.contains(TIMESTAMP_KEY)) {
            // The `getString` API requires a default value. Since we check that key exists we should never get the default value of empty
            // string.
            timestamp = preferences.getString(TIMESTAMP_KEY, "");
            if (timestamp.isEmpty()) {
                Log.e(TAG, "getInitialRegistrationDateTime failed to get the TimeStamp although the key exists");
                throw new RuntimeException("Failed to get TimeStamp after verifying it exists");
            }
        } else {
            // Create the initial timestamp for RemoteSystemApp registration and store it in SharedPreferences
            timestamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
            preferences.edit().putString(TIMESTAMP_KEY, timestamp).apply();
        }

        return timestamp;
    }
}
