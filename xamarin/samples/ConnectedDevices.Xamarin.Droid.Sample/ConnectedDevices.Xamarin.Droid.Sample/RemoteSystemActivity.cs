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
using System.Globalization;
using Android.App;
using Android.OS;
using Android.Widget;
using Microsoft.ConnectedDevices;

namespace ConnectedDevices.Xamarin.Droid.Sample
{
    [Activity(Label = "Remote System Details")]
    public class RemoteSystemActivity : Activity
    {
        private const string AppService = ""; // Fill in your app service name
        private const string AppIdentifier = ""; // Fill in your app identifier

        private string id;
        private AppServiceClientConnection appServiceClientConnection;

        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);

            this.SetContentView(Resource.Layout.RemoteSystem);

            var remoteSystem = ConnectedDevicesApplication.SelectedRemoteSystem;
            if (remoteSystem != null)
            {
                FindViewById<TextView>(Resource.Id.detailed_name_text).Text = remoteSystem.DisplayName;
                FindViewById<TextView>(Resource.Id.detailed_id_text).Text = remoteSystem.Id;
                FindViewById<TextView>(Resource.Id.detailed_kind_text).Text = (string)remoteSystem.Kind;
                FindViewById<TextView>(Resource.Id.detailed_proximity_text).Text = remoteSystem.IsAvailableByProximity ? "True" : "False";

                SetPingText(this as Activity, string.Empty);

                Button launchBtn = FindViewById<Button>(Resource.Id.detailed_launch_button);
                launchBtn.Click += delegate
                {
                    EditText textBox = (EditText)FindViewById(Resource.Id.detailed_url_text);
                    string uri = textBox.Text;

                    Console.WriteLine("Launching URI");
                    this.RemoteLaunchUriAsync(ConnectedDevicesApplication.SelectedRemoteSystem, new Uri(uri));
                };

                Button connectBtn = FindViewById<Button>(Resource.Id.detailed_connect_button);
                connectBtn.Click += delegate
                {
                    Console.WriteLine("Attempting connection to AppService");
                    this.ConnectAppService(AppService, AppIdentifier, new RemoteSystemConnectionRequest(ConnectedDevicesApplication.SelectedRemoteSystem));
                };

                Button pingBtn = FindViewById<Button>(Resource.Id.detailed_ping_button);
                pingBtn.Click += delegate
                {
                    Console.WriteLine("Sending ping message using AppServices");
                    this.SendPingMessage();
                };
            }
        }

        private static void SetPingText(Activity activity, string text)
        {
            activity.FindViewById<TextView>(Resource.Id.detailed_ping_text).Text = text;
        }

        private async void RemoteLaunchUriAsync(RemoteSystem remoteSystem, Uri uri)
        {
            var launchUriStatus = await RemoteLauncher.LaunchUriAsync(new RemoteSystemConnectionRequest(remoteSystem), uri);

            if (launchUriStatus != RemoteLaunchUriStatus.Success)
            {
                Console.WriteLine("Failed to Launch!");
            }
        }

        private async void ConnectAppService(string appService, string appIdentifier, RemoteSystemConnectionRequest connectionRequest)
        {
            this.appServiceClientConnection = new AppServiceClientConnection(appService, appIdentifier, connectionRequest);
            this.id = connectionRequest.RemoteSystem.Id;

            try
            {
                var status = await this.appServiceClientConnection.OpenRemoteAsync();
                Console.WriteLine("App Service connection returned with status " + status.ToString());
            }
            catch (ConnectedDevicesException e)
            {
                Console.WriteLine("Failed during attempt to create AppServices connection");
                e.PrintStackTrace();
            }
        }

        private async void SendPingMessage()
        {
            Bundle message = new Bundle();
            message.PutString("Type", "ping");
            message.PutString("CreationDate", DateTime.Now.ToString(CultureInfo.InvariantCulture));
            message.PutString("TargetId", this.id);

            try
            {
                var response = await this.appServiceClientConnection.SendMessageAsync(message);
                AppServiceResponseStatus status = response.Status;

                if (status == AppServiceResponseStatus.Success)
                {
                    Bundle bundle = response.Message;
                    string type = bundle.GetString("Type");
                    DateTime creationDate = DateTime.Parse(bundle.GetString("CreationDate"));
                    string targetId = bundle.GetString("TargetId");

                    DateTime nowDate = DateTime.Now;
                    int diff = nowDate.Subtract(creationDate).Milliseconds;

                    this.RunOnUiThread(() =>
                    {
                        SetPingText(this as Activity, diff.ToString());
                    });
                }
            }
            catch (ConnectedDevicesException e)
            {
                Console.WriteLine("Failed to send message using AppServices");
                e.PrintStackTrace();
            }
        }
    }
}