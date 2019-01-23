//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ModuleSelectFragment extends BaseFragment {

    private static final String TAG = ModuleSelectFragment.class.getName();

    // region Overrides
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_module_select, container, false);

        // Initialize buttons
        rootView.findViewById(R.id.device_relay_button)
            .setOnClickListener(v -> getMainActivity().navigateToPage((MainActivity.DEVICE_RELAY)));

        rootView.findViewById(R.id.activities_button)
            .setOnClickListener(v -> getMainActivity().navigateToPage((MainActivity.USER_ACTIVITIES)));

        rootView.findViewById(R.id.hosting_button)
            .setOnClickListener(v -> getMainActivity().navigateToPage((MainActivity.HOSTING)));

        return rootView;
    }
    // endregion

    @Override
    String getLogTag() {
        return TAG;
    }
}
