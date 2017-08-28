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
using System.Collections.Generic;

namespace Microsoft.ConnectedDevices
{
    public partial class AppServiceConnection
    {
        private AppServiceRequestListener appServiceRequestListener = null;
        private AppServiceConnectionInternal appServiceConnection = null;
        private AppServiceConnectionListener connectionListener = null;

        //
        // Summary:
        //     The event that is raised when an AppService request is received.
        public event OnRequestReceived RequestReceived;

        public delegate void OnRequestReceived(AppServiceRequest request);

        internal void InvokeRequestReceived(AppServiceRequest request)
        {
            RequestReceived?.Invoke(request);
        }

        public AppServiceConnection(string appServiceName, string appIdentifier, RemoteSystemConnectionRequest request)
        {
            appServiceConnection = new AppServiceConnectionInternal(appServiceName, appIdentifier, request, GetAppServiceConnectionListener(), GetAppServiceRequestListener(this));
        }

        public Task<AppServiceConnectionStatus> OpenRemoteAsync()
        {
            TaskCompletionSource<AppServiceConnectionStatus> tcsConnectionListener = new TaskCompletionSource<AppServiceConnectionStatus>();
            // We don't "need" to remove the lambda once it's done since previous 
            // tcsConnectionListener's will gracefully fail when called multiple times
            connectionListener.ConnectedStatus += (r) =>
            {
                tcsConnectionListener.TrySetResult(r);
            };

            try
            {
                appServiceConnection.OpenRemoteAsynchronous();
            }
            catch (Exception e)
            {
                tcsConnectionListener.TrySetException(e);
            }

            return tcsConnectionListener.Task;
        }

        public Task<AppServiceResponse> SendMessageAsync(Android.OS.Bundle message)
        {
            TaskCompletionSource<AppServiceResponse> tcsResponseListener = new TaskCompletionSource<AppServiceResponse>();
            AppServiceResponseListener.OnResponseReceived onResponseReceived = (r) =>
            {
                tcsResponseListener.TrySetResult(r);
            };

            var responseListener = new AppServiceResponseListener(onResponseReceived);

            try
            {
                appServiceConnection.SendMessageAsynchronous(message, responseListener);
            }
            catch (Exception e)
            {
                tcsResponseListener.TrySetException(e);
            }

            return tcsResponseListener.Task;
        }

        private AppServiceConnectionListener GetAppServiceConnectionListener()
        {
            connectionListener = new AppServiceConnectionListener();
            return connectionListener;
        }

        private AppServiceRequestListener GetAppServiceRequestListener(AppServiceConnection c)
        {
            appServiceRequestListener = new AppServiceRequestListener(c);
            return appServiceRequestListener;
        }

        internal class AppServiceConnectionListener : Java.Lang.Object, IAppServiceConnectionListener
        {
            public event Action<AppServiceConnectionStatus> ConnectedStatus;

            public void OnSuccess()
            {
                this.ConnectedStatus?.Invoke(AppServiceConnectionStatus.Success);
            }

            public void OnError(AppServiceConnectionStatus status)
            {
                this.ConnectedStatus?.Invoke(status);
            }

            public void OnClosed(AppServiceConnectionClosedStatus status) {}
        }

        internal class AppServiceResponseListener : Java.Lang.Object, IAppServiceResponseListener
        {
            public delegate void OnResponseReceived(AppServiceResponse status);
    
            private OnResponseReceived onResponseReceived;

            public AppServiceResponseListener(OnResponseReceived onReceived)
            {
                this.onResponseReceived = onReceived;
            }

            public void ResponseReceived(AppServiceResponse response)
            {
                this.onResponseReceived(response);
            }
        }

        internal class AppServiceRequestListener : Java.Lang.Object, IAppServiceRequestListener
        {
            private AppServiceConnection connection;
    
            public AppServiceRequestListener(AppServiceConnection c)
            {
                this.connection = c;
            }
    
            public void RequestReceived(AppServiceRequest request)
            {
                this.connection.InvokeRequestReceived(request);
            }
        }
    }
}