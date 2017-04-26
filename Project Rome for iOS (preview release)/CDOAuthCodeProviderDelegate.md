//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

/**
 * @brief A protocol for returning OAuth information.
 */
@protocol CDOAuthCodeProviderDelegate

/**
 * @brief Asynchronously obtains a new OAuth access code.
 * @signInUrl The URI which should be shown in a WebView.
 * @completion Callback which should be invoked when the OAuth access code is obtained or an error occurs.
 * @return An error if there was a failure preparing the async request.
 */
-(nullable NSError*)getAccessCode:(nonnull NSString*) signInUri completion:(nullable void (^)(NSError* _Nullable error, NSString* _Nullable accessCode))completionBlock;

/**
 * @brief The OAuth app id code.
 */
@property (nonatomic, readonly, copy, nonnull) NSString* appId;

@end
