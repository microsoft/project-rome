//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

/**
 * Updated the TextView when a spinner item is chosen and returns the value of the TextView
 */
class StringStorageItemSelectedListener implements AdapterView.OnItemSelectedListener {
    private TextView mTextView;

    public StringStorageItemSelectedListener(TextView textView) {
        mTextView = textView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mTextView.setText((String)parent.getItemAtPosition(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public String getString() {
        return mTextView.getText().toString();
    }
}