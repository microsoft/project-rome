//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.support.v4.app.Fragment;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Base Activity containing common helpers
 */
abstract class BaseFragment extends Fragment {
    private static final String TIME_STAMP_FORMAT = "HH:mm:ss.SSS";

    /**
     * Gets the log tag to use for this class
     *
     * @return Log tag for the class
     */
    abstract String getLogTag();

    /**
     * Gets the activity instance of this fragment cast as MainActivity to make usage less cumbersome and verbose
     *
     * @return The activity this fragment belongs to, cast as MainActivity
     */
    MainActivity getMainActivity() {
        return (MainActivity)getActivity();
    }

    /**
     * Simple function to get the current time in the form specified by the TIME_STAMP_FORMAT
     * @return Current time as string
     */
    String getTimeStamp() {
        DateFormat df = new SimpleDateFormat(TIME_STAMP_FORMAT, Locale.getDefault());
        return df.format(new Date());
    }

    /**
     * Pipes the message through logging in the Log Fragment + logcat
     * Calls logMessage on the owning Activity to send down to the Log Fragment
     *
     * @param level LogLevel to determine the level in Logcat
     * @param message String message to be displayed
     */
    void logMessage(final int level, final String message) {
        MainActivity mainActivity = getMainActivity();
        if (mainActivity == null) {
        } else {
            logMessage(getLogTag(), level, message);
        }
    }

    private void logMessage(final String tag, final int level, final String message) {
        switch (level) {
        case Log.ERROR: Log.e(tag, message); break;
        case Log.WARN: Log.w(tag, message); break;
        case Log.INFO: Log.i(tag, message); break;
        case Log.VERBOSE: Log.v(tag, message); break;
        default: Log.e(tag, "Unknown Log level on " + message);
        }

        if (getMainActivity() == null) {
            Log.e(
                getLogTag(), "The following message was not written to the log since this fragment is not yet attached to MainActivity [" +
                                 message + "]");
            return;
        }
    }

    /**
     * A shared format to handle all exceptions thrown by ConnectedDevices APIs.
     * @param api Function/Class name called which threw
     * @param e The throwable exception object
     */
    void WriteApiException(String api, Throwable e) {
        e.printStackTrace();
    }
}
