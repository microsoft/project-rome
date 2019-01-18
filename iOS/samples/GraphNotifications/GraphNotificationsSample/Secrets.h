//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

#pragma once

#import <Foundation/Foundation.h>

// These come from the converged app registration portal at apps.dev.microsoft.com
// MSA_CLIENT_ID:           Id of this app's registration in the MSA portal
// AAD_CLIENT_ID:           Id of this app's registration in the Azure portal
// AAD_REDIRECT_URI:        A Uri that this app is registered with in the Azure portal.
//                          AAD is supposed to use this Uri to call the app back after login (currently not true, external requirement)
//                          And this app is supposed to be able to handle this Uri (currently not true)
// APP_HOST_NAME            Cross-device domain of this app's registration
static NSString* const MSA_CLIENT_ID = @"<<MSA client ID goes here>>";
static NSString* const AAD_CLIENT_ID = @"<<AAD client ID goes here>>";
static NSString* const AAD_REDIRECT_URI = @"<<AAD redirect URI goes here>>";
static NSString* const APP_HOST_NAME = @"<<App cross-device domain goes here>>";
