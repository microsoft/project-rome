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

    public partial class RemoteSystem
    {
        public static void RequestAccessAsync() {}

        public static RemoteSystemWatcher CreateWatcher()
        {
            return new RemoteSystemWatcher(null); 
        }

        public static RemoteSystemWatcher CreateWatcher(List<IRemoteSystemFilter> filters)
        {
            return new RemoteSystemWatcher(filters); 
        }
    }
}