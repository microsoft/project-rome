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
    public partial class AppServiceRequest
    {
        private TaskCompletionSource<AppServiceResponseStatus> tcsResponseStatusListener = null;
    
        public Task<AppServiceResponseStatus> SendResponseAsync(Android.OS.Bundle message)
        {
            try
            {
                SendResponseAsynchronous(message, GetAppServiceResponseStatusListener());
            }
            catch (Exception e)
            {
                tcsResponseStatusListener.TrySetException(e);
            }
    
            return tcsResponseStatusListener.Task;
        }
    
        private AppServiceResponseStatusListener GetAppServiceResponseStatusListener()
        {
            AppServiceResponseStatusListener.OnResponseReceived onResponseReceived = (r) =>
            {
                tcsResponseStatusListener.TrySetResult(r);
            };
    
            tcsResponseStatusListener = new TaskCompletionSource<AppServiceResponseStatus>();
            var responseStatusListener = new AppServiceResponseStatusListener(onResponseReceived);
            return responseStatusListener;
        }
    
        internal class AppServiceResponseStatusListener : Java.Lang.Object, IAppServiceResponseStatusListener
        {
            public delegate void OnResponseReceived(AppServiceResponseStatus status);
    
            private OnResponseReceived onResponseReceived;
    
            public AppServiceResponseStatusListener(OnResponseReceived onReceived)
            {
                this.onResponseReceived = onReceived;
            }
    
            public void StatusReceived(AppServiceResponseStatus status)
            {
                this.onResponseReceived(status);
            }
        }
    }
}