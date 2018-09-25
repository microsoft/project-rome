//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// This code is licensed under the MIT License (MIT).
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

using System;
using System.Collections.Generic;
using Windows.UI.Xaml.Controls;

// Important Note for running this sample:
// The sample as-is will not be able to get tokens without having it's app manifest being 
// modified to use the App Identity of a registered Microsoft Store/registered AAD app. 
//
// See 'Related Topics' in the README.md for instructions on how to register an app.

namespace SDKTemplate
{
    public partial class MainPage : Page
    {
        public const string FEATURE_NAME = "Graph Notifications";

        List<Scenario> scenarios = new List<Scenario>
        {
            new Scenario() { Title="Login/Logout", ClassType=typeof(AccountsPage)},
            new Scenario() { Title="Notification History", ClassType=typeof(NotificationsPage)},
            new Scenario() { Title="App Logs", ClassType=typeof(LogsPage)},
        };
    }

    public class Scenario
    {
        public string Title { get; set; }
        public Type ClassType { get; set; }
    }
}
