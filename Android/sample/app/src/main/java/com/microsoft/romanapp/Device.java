//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.romanapp;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.microsoft.connecteddevices.RemoteSystem;

import java.util.NoSuchElementException;

public class Device implements Parcelable {
    private String name;
    private String type;
    private String id;
    private boolean isAvailableByProximity;
    private RemoteSystem system = null;

    Device(RemoteSystem system) {
        this.system = system;
        id = system.getId();
        name = system.getDisplayName();
        type = system.getKind().toString();
        isAvailableByProximity = system.isAvailableByProximity();
    }

    protected Device(Parcel in) {
        id = in.readString();

        try {
            system = DeviceStorage.getDevice(id);
            name = system.getDisplayName();
            type = system.getKind().toString();
            isAvailableByProximity = system.isAvailableByProximity();
        } catch (NoSuchElementException e) {
            Log.e("Device", "Device was not found in storage");
            e.printStackTrace();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        // The system must be stored since it is not parcelable
        try {
            DeviceStorage.addDevice(id, system);
        } catch (RuntimeException e) {
            Log.e("Device", "Device already has been added to storage");
            e.printStackTrace();
        }
    }

    public String getName() { return name; }

    public String getType() { return type; }

    public RemoteSystem getSystem() { return system; }

    public boolean getIsAvailableByProximity() { return isAvailableByProximity; }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };
}
