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
using Android.App;
using Android.OS;
using Android.Widget;
using Microsoft.ConnectedDevices;

namespace ConnectedDevices.Xamarin.Droid.Sample
{
    [Activity(Label = "Remote System Details")]
    public class RemoteSystemActivity : Activity
    {
        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);

            SetContentView(Resource.Layout.RemoteSystem);

            var remoteSystem = ConnectedDevicesApplication.SelectedRemoteSystem;
            if (remoteSystem != null)
            {
                FindViewById<TextView>(Resource.Id.detailed_name_text).Text = remoteSystem.DisplayName;
                FindViewById<TextView>(Resource.Id.detailed_id_text).Text = remoteSystem.Id;
                FindViewById<TextView>(Resource.Id.detailed_kind_text).Text = (string)remoteSystem.Kind;
                FindViewById<TextView>(Resource.Id.detailed_proximity_text).Text = remoteSystem.IsAvailableByProximity ? "True" : "False";

                Button button = FindViewById<Button>(Resource.Id.detailed_launch_button);
                button.Click += delegate
                {
                    EditText textBox = (EditText)FindViewById(Resource.Id.detailed_url_text);
                    string uri = textBox.Text;

                    Console.WriteLine("Launching URI");
                    RemoteLaunchUriAsync(ConnectedDevicesApplication.SelectedRemoteSystem, new Uri(uri));
                };

            }
        }

        private async void RemoteLaunchUriAsync(RemoteSystem remoteSystem, Uri uri)
        {
            var launchUriStatus = await RemoteLauncher.LaunchUriAsync(new RemoteSystemConnectionRequest(remoteSystem), uri);

            if (launchUriStatus != RemoteLaunchUriStatus.Success)
            {
                Console.WriteLine("Failed to Launch!");
            }
        }
    }
}