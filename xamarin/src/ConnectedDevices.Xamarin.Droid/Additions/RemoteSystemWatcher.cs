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

namespace Microsoft.ConnectedDevices
{

    public partial class RemoteSystemWatcher
    {

        private RemoteSystemDiscovery.Builder _builder = new RemoteSystemDiscovery.Builder();
        private RemoteSystemDiscoveryListener _listener;
        private RemoteSystemDiscovery _watcher;

        //
        // Summary:
        //     The event that is raised when a new remote system (device) is discovered.
        public event OnRemoteSystemAdded RemoteSystemAdded;
        public delegate void OnRemoteSystemAdded(RemoteSystemWatcher watcher, RemoteSystemAddedEventArgs args);
        internal void InvokeRemoteSystemAdded(RemoteSystem remoteSystem)
        {
            RemoteSystemAdded?.Invoke(this, new RemoteSystemAddedEventArgs(remoteSystem));
        }

        //
        // Summary:
        //     The event that is raised when a previously discovered remote system (device)
        //     is no longer visible.
        public event OnRemoteSystemRemoved RemoteSystemRemoved;
        public delegate void OnRemoteSystemRemoved(RemoteSystemWatcher watcher, RemoteSystemRemovedEventArgs args);
        internal void InvokeRemoteSystemRemoved(string remoteSystemId)
        {
            RemoteSystemRemoved?.Invoke(this, new RemoteSystemRemovedEventArgs(remoteSystemId));
        }
        //
        // Summary:
        //     The event that is raised when a previously discovered remote system (device)
        //     changes one of its monitored properties (see the properties of the RemoteSystem
        //     class).
        public event OnRemoteSystemUpdated RemoteSystemUpdated;
        public delegate void OnRemoteSystemUpdated(RemoteSystemWatcher watcher, RemoteSystemUpdatedEventArgs args);
        internal void InvokeRemoteSystemUpdated(RemoteSystem remoteSystem)
        {
            RemoteSystemUpdated?(this, new RemoteSystemUpdatedEventArgs(remoteSystem));
        }
        //
        // Summary:
        //     Starts watching for discoverable remote systems.
        public void Start()
        {
            _listener = new RemoteSystemDiscoveryListener(this);
            _watcher = _builder.SetListener(_listener).Result;
            _watcher.Start();
        }

        //
        // Summary:
        //     Stops watching for discoverable remote systems.

        public void Stop()
        {
            _watcher.Stop();
        }
    }


    internal class RemoteSystemDiscoveryListener : Java.Lang.Object, IRemoteSystemDiscoveryListener
    {
        private RemoteSystemWatcher watcher;

        public RemoteSystemDiscoveryListener(RemoteSystemWatcher w)
        {
            watcher = w;
        }
        public void OnRemoteSystemAdded(Microsoft.ConnectedDevices.RemoteSystem remoteSystem)
        {
            watcher.InvokeRemoteSystemAdded(remoteSystem);
        }

        public void OnRemoteSystemRemoved(string remoteSystemId)
        {
            watcher.InvokeRemoteSystemRemoved(remoteSystemId);
        }

        public void OnRemoteSystemUpdated(Microsoft.ConnectedDevices.RemoteSystem remoteSystem)
        {
            watcher.InvokeRemoteSystemUpdated(remoteSystem);
        }
    }

}