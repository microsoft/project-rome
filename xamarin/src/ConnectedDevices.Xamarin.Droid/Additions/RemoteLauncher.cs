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
    public partial class RemoteLauncher
    {
        public static Task<RemoteLaunchUriStatus> LaunchUriAsync(RemoteSystemConnectionRequest connectionRequest, Uri uri)
        {
            var tcs = new TaskCompletionSource<RemoteLaunchUriStatus>();

            RemoteLauncher _launcher = new RemoteLauncher();

            try
            { 
                var launchUriListener = new LaunchURIListener();
                launchUriListener.LaunchCompleted += (r) =>
                {
                    tcs.TrySetResult(r);
                };
                _launcher.LaunchUriAsync(connectionRequest, uri.OriginalString, launchUriListener);
            }
            catch (Exception e)
            {
                tcs.TrySetException(e);
            }

            return tcs.Task;

        }
    }

    internal class LaunchURIListener : Java.Lang.Object, IRemoteLauncherListener
    {
        public void OnCompleted(RemoteLaunchUriStatus p0)
        {
            LaunchCompleted?.Invoke(p0);
        }

        public event Action<RemoteLaunchUriStatus> LaunchCompleted;

    }
}