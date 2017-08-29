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
        // Your app service name
        private const string AppService = Secrets.AppService;
        // Your app identifier
        private const string AppIdentifier = Secrets.AppIdentifier;

        private string id;
        private AppServiceConnection appServiceConnection;

        private TextView launchLog;
        private EditText textBox;
        private Button pingBtn;

        private long messageId = 0;

        protected override void OnCreate(Bundle savedInstanceState)
        {
            base.OnCreate(savedInstanceState);

            this.SetContentView(Resource.Layout.RemoteSystem);

            var remoteSystem = ConnectedDevicesApplication.SelectedRemoteSystem;
            if (remoteSystem != null)
            {
                FindViewById<TextView>(Resource.Id.device_name).Text = remoteSystem.DisplayName;
                FindViewById<TextView>(Resource.Id.device_type).Text = (string)remoteSystem.Kind;

                SetPingText(this as Activity, string.Empty);

                this.launchLog = (TextView)FindViewById(Resource.Id.launch_log);
                this.textBox = (EditText)FindViewById(Resource.Id.launch_uri_edit_text);

                InitializeButtons();
                InitializeSpinner();
            }
        }

        private static void SetPingText(Activity activity, string text)
        {
            activity.FindViewById<TextView>(Resource.Id.ping_value).Text = text;
        }

        private void InitializeButtons()
        {
            Button launchBtn = FindViewById<Button>(Resource.Id.launch_uri_btn);
            launchBtn.Click += delegate
            {
                string uri = this.textBox.Text;

                Console.WriteLine("Launching URI");
                this.RemoteLaunchUriAsync(ConnectedDevicesApplication.SelectedRemoteSystem, new Uri(uri));
            };

            Button connectBtn = FindViewById<Button>(Resource.Id.open_connection_btn);
            connectBtn.Click += delegate
            {
                Console.WriteLine("Attempting connection to AppService");
                this.ConnectAppService(AppService, AppIdentifier, new RemoteSystemConnectionRequest(ConnectedDevicesApplication.SelectedRemoteSystem));
            };

            this.pingBtn = FindViewById<Button>(Resource.Id.send_ping_btn);
            this.pingBtn.Click += delegate
            {
                Console.WriteLine("Sending ping message using AppServices");
                this.SendPingMessage();
            };
        }

        private void InitializeSpinner()
        {
            Spinner uriSpinner = FindViewById<Spinner>(Resource.Id.launch_uri_spinner);
            uriSpinner.ItemSelected += new EventHandler<AdapterView.ItemSelectedEventArgs>(spinner_ItemSelected);
            var adapter = ArrayAdapter.CreateFromResource(this, Resource.Array.uri_array, Android.Resource.Layout.SimpleSpinnerItem);
            adapter.SetDropDownViewResource(Android.Resource.Layout.SimpleSpinnerDropDownItem);
            uriSpinner.Adapter = adapter;
            uriSpinner.SetSelection(0);
        }

        private void spinner_ItemSelected(object sender, AdapterView.ItemSelectedEventArgs e)
        {
            if (this.textBox == null)
            {
                return;
            }

            Spinner spinner = (Spinner)sender;
            string url = spinner.GetItemAtPosition(e.Position).ToString();
            this.textBox.SetText(url.ToCharArray(), 0, url.Length);
        }

        private async void RemoteLaunchUriAsync(RemoteSystem remoteSystem, Uri uri)
        {
            this.LogMessage("Launching URI: " + uri + " on " + remoteSystem.DisplayName);
            var launchUriStatus = await RemoteLauncher.LaunchUriAsync(new RemoteSystemConnectionRequest(remoteSystem), uri);

            if (launchUriStatus == RemoteLaunchUriStatus.Success)
            {
                this.LogMessage("Launch succeeded");
            }
            else
            {
                this.LogMessage("Launch failed due to [" + launchUriStatus.ToString() + "]");
            }
        }

        private async void ConnectAppService(string appService, string appIdentifier, RemoteSystemConnectionRequest connectionRequest)
        {
            this.appServiceConnection = new AppServiceConnection(appService, appIdentifier, connectionRequest);
            this.appServiceConnection.RequestReceived += AppServiceConnectionRequestReceived;

            this.id = connectionRequest.RemoteSystem.Id;

            try
            {
                this.LogMessage("Sending AppServices connection request. Waiting for connection response");
                var status = await this.appServiceConnection.OpenRemoteAsync();
                
                if (status == AppServiceConnectionStatus.Success)
                {
                    this.LogMessage("App Service connection successful!");
                    this.pingBtn.Enabled = true;
                }
                else
                {
                    this.LogMessage("App Service connection failed, returning status " + status.ToString());
                }
            }
            catch (ConnectedDevicesException e)
            {
                Console.WriteLine("Failed during attempt to create AppServices connection");
                e.PrintStackTrace();
            }
        }

        private Bundle CreatePingMessage()
        {
            Bundle bundle = new Bundle();
            bundle.PutString("Type", "ping");
            bundle.PutString("CreationDate", DateTime.Now.ToString(CultureInfo.InvariantCulture));
            bundle.PutString("TargetId", this.id);
    
            return bundle;
        }

        private Bundle CreatePongMessage(Bundle bundle)
        {
            bundle.PutString("Type", "pong");
            return bundle;
        }

        private async void AppServiceConnectionRequestReceived(AppServiceRequest request)
        {
            this.LogMessage("Received AppService request. Sending response.");

            var status = await request.SendResponseAsync(this.CreatePongMessage(request.Message));
            if (status == AppServiceResponseStatus.Success)
            {
                this.LogMessage("Successfully sent response.");
            }
            else
            {
                this.LogMessage("Failed to send response.");
            }
        }

        private async void SendPingMessage()
        {
            long id = ++this.messageId;

            try
            {
                this.LogMessage("Sending AppServices message [" + id.ToString() + "]. Waiting for ping response");
                var response = await this.appServiceConnection.SendMessageAsync(this.CreatePingMessage());
                AppServiceResponseStatus status = response.Status;

                if (status == AppServiceResponseStatus.Success)
                {
                    this.LogMessage("Received successful AppService response to message [" + id.ToString() + "]");

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
                else
                {
                    this.LogMessage("Did not receive successful AppService response");
                }
            }
            catch (ConnectedDevicesException e)
            {
                Console.WriteLine("Failed to send message using AppServices");
                e.PrintStackTrace();
            }
        }

        private void LogMessage(string message)
        {
            if (this.launchLog == null)
            {
                return;
            }

            Console.WriteLine(message);

            String newText = "\n" + message + " [" + this.GetTimeStamp() + "]";
            // UI elements can only be modified on the UI thread.
            this.RunOnUiThread(() =>
            {
                this.launchLog.Append(newText);
            });
        }

        private string GetTimeStamp()
        {
            return DateTime.Now.ToString("HH:mm:ss.fff");
        }
    }
}