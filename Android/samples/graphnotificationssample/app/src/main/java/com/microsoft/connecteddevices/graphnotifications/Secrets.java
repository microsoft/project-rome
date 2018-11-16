//
// Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.graphnotifications;

class Secrets {
// These come from the converged app registration portal at apps.dev.microsoft.com
    // MSA_CLIENT_ID:           Id of this app's registration in the MSA portal
    // AAD_CLIENT_ID:           Id of this app's registration in the Azure portal
    // AAD_REDIRECT_URI:        A Uri that this app is registered with in the Azure portal.
    //                          AAD is supposed to use this Uri to call the app back after login (currently not true, external requirement)
    //                          And this app is supposed to be able to handle this Uri (currently not true)
    // APP_HOST_NAME            Cross-device domain of this app's registration
    static final String MSA_CLIENT_ID = "<<MSA client ID goes here>>";
    static final String AAD_CLIENT_ID = "<<AAD client ID goes here>>";
    static final String AAD_REDIRECT_URI = "<<AAD redirect URI goes here>>";
    static final String APP_HOST_NAME = "<<App cross-device domain goes here>>";

    // Your client's Firebase Cloud Messaging Sender Id
    static final String FCM_SENDER_ID = "<<FCM sender ID goes here>>";
}
