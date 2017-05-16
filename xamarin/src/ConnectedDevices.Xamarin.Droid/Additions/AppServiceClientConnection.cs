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

namespace Microsoft.ConnectedDevices
{
    public partial class AppServiceClientConnection
    {
        private static TaskCompletionSource<AppServiceClientConnectionStatus> tcsConnectionListener = null;
        private static TaskCompletionSource<AppServiceClientResponse> tcsResponseListener = null;

        public AppServiceClientConnection(
            string appServiceName,
            string appIdentifier,
            RemoteSystemConnectionRequest request) :
            this(appServiceName, appIdentifier, request, GetAppServiceClientConnectionListener(), GetAppServiceResponseListener())
        {
        }

        public Task<AppServiceClientConnectionStatus> OpenRemoteAsync()
        {
            try
            {
                OpenRemoteAsynchronous();
            }
            catch (Exception e)
            {
                tcsConnectionListener.TrySetException(e);
            }

            return tcsConnectionListener.Task;
        }

        public Task<AppServiceClientResponse> SendMessageAsync(Android.OS.Bundle message)
        {
            try
            {
                SendMessageAsynchronous(message);
            }
            catch (Exception e)
            {
                tcsResponseListener.TrySetException(e);
            }

            return tcsResponseListener.Task;
        }

        private static AppServiceClientConnectionListener GetAppServiceClientConnectionListener()
        {
            tcsConnectionListener = new TaskCompletionSource<AppServiceClientConnectionStatus>();
            var connectionListener = new AppServiceClientConnectionListener();
            connectionListener.ConnectedStatus += (r) =>
            {
                tcsConnectionListener.TrySetResult(r);
            };

            return connectionListener;
        }

        private static AppServiceResponseListener GetAppServiceResponseListener()
        {
            tcsResponseListener = new TaskCompletionSource<AppServiceClientResponse>();
            var responseListener = new AppServiceResponseListener();
            responseListener.Response += (r) =>
            {
                tcsResponseListener.TrySetResult(r);
            };

            return responseListener;
        }

        internal class AppServiceClientConnectionListener : Java.Lang.Object, IAppServiceClientConnectionListener
        {
            public event Action<AppServiceClientConnectionStatus> ConnectedStatus;

            public void OnSuccess()
            {
                this.ConnectedStatus?.Invoke(AppServiceClientConnectionStatus.Success);
            }

            public void OnError(AppServiceClientConnectionStatus status)
            {
                this.ConnectedStatus?.Invoke(status);
            }

            public void OnClosed(AppServiceClientConnectionClosedStatus status) {}
        }

        internal class AppServiceResponseListener : Java.Lang.Object, IAppServiceResponseListener
        {
            public event Action<AppServiceClientResponse> Response;

            public void ResponseReceived(AppServiceClientResponse response)
            {
                this.Response?.Invoke(response);
            }
        }
    }
}