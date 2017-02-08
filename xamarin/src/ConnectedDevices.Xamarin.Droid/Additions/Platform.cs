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

        public static event Action<string> FetchAuthCode;
        private static Platform.IAuthCodeHandler _handler;

        public static Task<bool> InitializeAsync(Context context, string clientID)
        {
            var tcs = new TaskCompletionSource<bool>(); 

            var platformInitializer = new PlatformInitializer();
            var _authCodeProvider = new AuthCodeProvider(clientID);

            platformInitializer.InitializationCompleted += (r) =>
            {
                tcs.TrySetResult(r);
            };
            Platform.Initialize(context, _authCodeProvider, platformInitializer);

            return tcs.Task;
        }

        internal static void InvokeFetchAuthCode(string oauthUrl, Platform.IAuthCodeHandler handler)
        {
            _handler = handler;
            FetchAuthCode?.Invoke(oauthUrl);
        }

        public static void SetAuthCode(string authCode)
        {
            _handler?.OnAuthCodeFetched(authCode);
        }
    }

    internal class PlatformInitializer : Java.Lang.Object, IPlatformInitializationHandler
    {
        public void OnDone(bool completed)
        {
            InitializationCompleted?.Invoke(completed);
        }

        public event Action<bool> InitializationCompleted;
    }

    internal class AuthCodeProvider : Java.Lang.Object, IAuthCodeProvider
    {
        public AuthCodeProvider(string clientId)
        {
            ClientId = clientId;
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