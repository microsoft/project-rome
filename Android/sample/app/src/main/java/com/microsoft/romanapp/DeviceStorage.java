//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.romanapp;

import com.microsoft.connecteddevices.RemoteSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public final class DeviceStorage {
    private static final Map<String, RemoteSystem> map = new HashMap<>();

    public static void addDevice(String id, RemoteSystem system) throws RuntimeException {
        if (!map.containsKey(id) && system != null) {
            map.put(id, system);
        } else {
            throw new RuntimeException();
        }
    }

    public static RemoteSystem getDevice(String id) throws NoSuchElementException {
        if (map.containsKey(id)) {
            RemoteSystem device = map.get(id);
            map.remove(id);
            return device;
        }
        throw new NoSuchElementException();
    }
}