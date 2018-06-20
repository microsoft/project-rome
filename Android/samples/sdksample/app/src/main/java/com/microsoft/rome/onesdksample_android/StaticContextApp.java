//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.app.Application;
import android.content.Context;

public class StaticContextApp extends Application {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static String getStringValue(int id) {
        return mContext.getString(id);
    }
}
