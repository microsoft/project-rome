//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

using System;
using System.Threading.Tasks;
using Android.Content;

namespace Microsoft.ConnectedDevices
{
    public partial class Platform
    {
        private static Platform.IAuthCodeHandler _handler;
        public static event Action<string> FetchAuthCode;

        public static Task<bool> InitializeAsync(Context context, string clientID)
        {
            var tcs = new TaskCompletionSource<bool>(); 

            var platformInitializer = new PlatformInitializer();
            var authCodeProvider = new AuthCodeProvider(clientID);

            platformInitializer.InitializationCompleted += (r) =>
            {
                tcs.TrySetResult(r);
            };
            Platform.Initialize(context, authCodeProvider, platformInitializer);

            return tcs.Task;
        }

        public static void SetAuthCode(string authCode)
        {
            _handler?.OnAuthCodeFetched(authCode);
        }

        internal static void InvokeFetchAuthCode(string oauthUrl, Platform.IAuthCodeHandler handler)
        {
            _handler = handler;
            FetchAuthCode?.Invoke(oauthUrl);
        }

        internal class PlatformInitializer : Java.Lang.Object, IPlatformInitializationHandler
        {
            public event Action<bool> InitializationCompleted;

            public void OnDone()
            {
                this.InitializationCompleted?.Invoke(true);
            }

            public void OnError(PlatformInitializationStatus status)
            {
                this.InitializationCompleted?.Invoke(false);
            }
        }

        internal class AuthCodeProvider : Java.Lang.Object, IAuthCodeProvider
        {
            public AuthCodeProvider(string clientId)
            {
                this.ClientId = clientId;
            }

            public string ClientId { get; private set; }

            public void FetchAuthCodeAsync(string oauthUrl, Platform.IAuthCodeHandler handler)
            {
                // Platform needs app to get an MSA auth_code.
                // Need to sign in user via OAuth for given url.
                Platform.InvokeFetchAuthCode(oauthUrl, handler);
            }
        }
    }
}