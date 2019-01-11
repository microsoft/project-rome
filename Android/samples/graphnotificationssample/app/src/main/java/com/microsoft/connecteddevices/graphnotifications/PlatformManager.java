package com.microsoft.connecteddevices.graphnotifications;

import android.content.Context;
import android.util.Log;

import com.microsoft.connecteddevices.ConnectedDevicesPlatform;

final class PlatformManager {
    private RomeNotificationReceiver mNotificationReceiver;
    private ConnectedDevicesPlatform mPlatform;
    private static PlatformManager sInstance;

    private static final String TAG = PlatformManager.class.getName();

    public static synchronized PlatformManager getInstance()
    {
        if (sInstance == null) {
            sInstance = new PlatformManager();
        }

        return sInstance;
    }

    public synchronized ConnectedDevicesPlatform createPlatform(Context context) {
        if (mPlatform != null) {
            return mPlatform;
        }

        try {
            mPlatform = new ConnectedDevicesPlatform(context);
        } catch (Exception e) {
            return null;
        }

        // Subscribe to ConnectedDevicesNotificationRegistrationManager's event for when the registration state changes for a given account.
        mPlatform.getNotificationRegistrationManager().notificationRegistrationStateChanged().subscribe(
                (notificationRegistrationManager, args) -> {
                    // TODO: Future - Identity-V3: give this to the signin helpers? Not exactly sure how to handle this...
                });

        return mPlatform;
    }

    public synchronized void startPlatform() {
        // Ensure we have created the Platform since there will not be an object to call start on otherwise.
        if (mPlatform == null) {
            return;
        }

        try {
            mPlatform.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start platform with exception: " + e.getMessage());
        }
    }

    public synchronized ConnectedDevicesPlatform getPlatform() {
        return mPlatform;
    }

    public synchronized void createNotificationReceiver(Context context) {
        mNotificationReceiver = new RomeNotificationReceiver(context);
    }

    public synchronized RomeNotificationReceiver getNotificationReceiver() {
        return mNotificationReceiver;
    }
}
