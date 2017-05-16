//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

using System.Collections.Generic;

namespace Microsoft.ConnectedDevices
{
    public partial class RemoteSystemWatcher
    {
        private RemoteSystemDiscovery.Builder builder = new RemoteSystemDiscovery.Builder();
        private RemoteSystemDiscoveryListener listener;
        private List<IRemoteSystemFilter> filters;

        private RemoteSystemDiscovery watcher;

        public RemoteSystemWatcher(List<IRemoteSystemFilter> filters)
        {
            this.filters = filters;
        }

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

        //
        // Summary:
        //     Starts watching for discoverable remote systems.
        public void Start()
        {
            this.listener = new RemoteSystemDiscoveryListener(this);
            this.builder = this.builder.SetListener(this.listener);

            if (filters != null)
            {
                foreach (var filter in this.filters)
                {
                    this.builder = this.builder.Filter(filter);
                }
            }

            this.watcher = this.builder.Result;
            this.watcher.Start();
        }

        //
        // Summary:
        //     Stops watching for discoverable remote systems.

        public void Stop()
        {
            this.watcher.Stop();
        }

        internal void InvokeRemoteSystemUpdated(RemoteSystem remoteSystem)
        {
            this.RemoteSystemUpdated?.Invoke(this, new RemoteSystemUpdatedEventArgs(remoteSystem));
        }
    }

    internal class RemoteSystemDiscoveryListener : Java.Lang.Object, IRemoteSystemDiscoveryListener
    {
        private RemoteSystemWatcher watcher;

        public RemoteSystemDiscoveryListener(RemoteSystemWatcher w)
        {
            this.watcher = w;
        }

        public void OnRemoteSystemAdded(Microsoft.ConnectedDevices.RemoteSystem remoteSystem)
        {
            this.watcher.InvokeRemoteSystemAdded(remoteSystem);
        }

        public void OnRemoteSystemRemoved(string remoteSystemId)
        {
            this.watcher.InvokeRemoteSystemRemoved(remoteSystemId);
        }

        public void OnRemoteSystemUpdated(Microsoft.ConnectedDevices.RemoteSystem remoteSystem)
        {
            this.watcher.InvokeRemoteSystemUpdated(remoteSystem);
        }
    }
}