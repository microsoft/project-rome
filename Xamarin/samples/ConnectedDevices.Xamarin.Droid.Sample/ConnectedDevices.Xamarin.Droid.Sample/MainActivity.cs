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
using System.Collections.Generic;
using System.Linq;
using Android;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Widget;
using Android.Webkit;
using Microsoft.ConnectedDevices;
using Android.Views;
using Android.Content.PM;

namespace ConnectedDevices.Xamarin.Droid.Sample
{
    [Activity(Label = "Connected Devices", MainLauncher = true, Icon = "@drawable/icon")]
    public class MainActivity : ListActivity
    {
        // Use your own client id. Get a client ID from https://apps.dev.microsoft.com/
        private const string CLIENT_ID = Secrets.CLIENT_ID;

        internal Dialog authDialog;
        private WebView webView;
        private RemoteSystemAdapter adapter;

        private List<RemoteSystemKinds> remoteSystemKind = new List<RemoteSystemKinds> { RemoteSystemKinds.Unknown, RemoteSystemKinds.Desktop, RemoteSystemKinds.Holographic, RemoteSystemKinds.Phone, RemoteSystemKinds.Xbox };
        private RemoteSystemDiscoveryType remoteSystemDiscoveryKind = RemoteSystemDiscoveryType.Any;

        private RemoteSystemWatcher remoteSystemWatcher;
        private Button refreshButton;

        private int permissionRequestCode;

        protected override void OnCreate(Bundle bundle)
        {
            base.OnCreate(bundle);

            // Set our view from the "main" layout resource
            this.SetContentView(Resource.Layout.Main);

            refreshButton = FindViewById<Button>(Resource.Id.RefreshButton);
            refreshButton.Enabled = false;
            refreshButton.Click += (object sender, EventArgs e) =>
            {
                RefreshDevices();
            };

            if (string.IsNullOrEmpty(CLIENT_ID))
            {
                Toast.MakeText(this, "CLIENT_ID not set!", ToastLength.Long).Show();
            }

            // Prompt for location permission if it hasn't been granted
            if (CheckSelfPermission(Manifest.Permission.AccessCoarseLocation) == Permission.Granted)
            {
                this.InitializeAsync();
            }
            else
            {
                Random rand = new Random();
                this.permissionRequestCode = rand.Next(128);
                RequestPermissions(new string[] { Manifest.Permission.AccessCoarseLocation }, this.permissionRequestCode);
            }

            this.adapter = new RemoteSystemAdapter(this, new List<RemoteSystem>());
            this.ListAdapter = this.adapter;
        }

        public override async void OnRequestPermissionsResult(int requestCode, string[] permissions, Permission[] grantResults)
        {
            base.OnRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == this.permissionRequestCode)
            {
                // Platform handles if no permission granted for bluetooth, no need to do anything special.
                this.permissionRequestCode = -1;

                this.InitializeAsync();
            }
        }

        protected override void OnListItemClick(ListView l, View v, int position, long id)
        {
            var detailsActivity = new Intent(this, typeof(RemoteSystemActivity));

            RemoteSystem selectedDevice = adapter.GetItem(position);

            ConnectedDevicesApplication.SelectedRemoteSystem = selectedDevice;

            StartActivity(detailsActivity);
        }

        internal async void InitializeAsync()
        {
            Platform.FetchAuthCode += Platform_FetchAuthCode;
            var result = await Platform.InitializeAsync(this.ApplicationContext, CLIENT_ID);

            if (result == true)
            {
                Console.WriteLine("Initialized platform successfully");
                refreshButton.Enabled = true;
                InitializeSpinners();
                RefreshDevices();
            }
            else
            {
                Console.WriteLine("ConnectedDevices Platform initialization failed");
            }
        }

        internal void RefreshDevices()
        {
            adapter.Clear();
            DiscoverDevices();
        }

        private RemoteSystemDiscoveryType StringToDiscoveryType(string str)
        {
            RemoteSystemDiscoveryType kind = RemoteSystemDiscoveryType.Any;

            switch (str)
            {
                case "All":
                    kind = RemoteSystemDiscoveryType.Any;
                    break;
                case "Cloud":
                    kind = RemoteSystemDiscoveryType.Cloud;
                    break;
                case "Proximal":
                    kind = RemoteSystemDiscoveryType.Proximal;
                    break;
            }

            return kind;
        }

        private List<RemoteSystemKinds> StringToSystemKind(string str)
        {
            List<RemoteSystemKinds> kinds = new List<RemoteSystemKinds> { RemoteSystemKinds.Unknown };

            switch (str)
            {
                case "All":
                    kinds = new List<RemoteSystemKinds> { RemoteSystemKinds.Unknown, RemoteSystemKinds.Desktop, RemoteSystemKinds.Holographic, RemoteSystemKinds.Phone, RemoteSystemKinds.Xbox };
                    break;
                case "Unknown":
                    kinds = new List<RemoteSystemKinds> { RemoteSystemKinds.Unknown };
                    break;
                case "Desktop":
                    kinds = new List<RemoteSystemKinds> { RemoteSystemKinds.Desktop };
                    break;
                case "Holographic":
                    kinds = new List<RemoteSystemKinds> { RemoteSystemKinds.Holographic };
                    break;
                case "Phone":
                    kinds = new List<RemoteSystemKinds> { RemoteSystemKinds.Phone };
                    break;
                case "Xbox":
                    kinds = new List<RemoteSystemKinds> { RemoteSystemKinds.Xbox };
                    break;
            }

            return kinds;
        }

        private void InitializeSpinners()
        {
            Spinner discoveryTypeSpinner = FindViewById<Spinner>(Resource.Id.discovery_type_filter_spinner);
            discoveryTypeSpinner.ItemSelected += new EventHandler<AdapterView.ItemSelectedEventArgs>(DiscoveryTypeItemSelected);
            var discoveryTypeAdapter = ArrayAdapter.CreateFromResource(this, Resource.Array.discovery_type_filter_array, Android.Resource.Layout.SimpleSpinnerItem);
            discoveryTypeAdapter.SetDropDownViewResource(Android.Resource.Layout.SimpleSpinnerDropDownItem);
            discoveryTypeSpinner.Adapter = discoveryTypeAdapter;
            discoveryTypeSpinner.SetSelection(0);

            Spinner systemKindSpinner = FindViewById<Spinner>(Resource.Id.system_kind_filter_spinner);
            systemKindSpinner.ItemSelected += new EventHandler<AdapterView.ItemSelectedEventArgs>(SystemKindItemSelected);
            var systemKindAdapter = ArrayAdapter.CreateFromResource(this, Resource.Array.system_kind_filter_array, Android.Resource.Layout.SimpleSpinnerItem);
            systemKindAdapter.SetDropDownViewResource(Android.Resource.Layout.SimpleSpinnerDropDownItem);
            systemKindSpinner.Adapter = systemKindAdapter;
            systemKindSpinner.SetSelection(0);
        }

        private void DiscoveryTypeItemSelected(object sender, AdapterView.ItemSelectedEventArgs e)
        {
            Spinner spinner = (Spinner)sender;
            string discoveryTypeStr = spinner.GetItemAtPosition(e.Position).ToString();
            remoteSystemDiscoveryKind = StringToDiscoveryType(discoveryTypeStr);
            RefreshDevices();
        }

        private void SystemKindItemSelected(object sender, AdapterView.ItemSelectedEventArgs e)
        {
            Spinner spinner = (Spinner)sender;
            string systemKindStr = spinner.GetItemAtPosition(e.Position).ToString();
            remoteSystemKind = StringToSystemKind(systemKindStr);
            RefreshDevices();
        }       

        private void Platform_FetchAuthCode(string oauthUrl)
        {
            authDialog = new Dialog(this);

            var linearLayout = new LinearLayout(authDialog.Context);
            webView = new WebView(authDialog.Context);
            linearLayout.AddView(webView);
            authDialog.SetContentView(linearLayout);

            webView.SetWebChromeClient(new WebChromeClient());
            webView.Settings.JavaScriptEnabled = true;
            webView.Settings.DomStorageEnabled = true;
            webView.LoadUrl(oauthUrl);

            webView.SetWebViewClient(new MsaWebViewClient(this));
            authDialog.Show();
            authDialog.SetCancelable(true);
        }

        private void DiscoverDevices()
        {
            if (remoteSystemWatcher != null)
            {
                remoteSystemWatcher.Stop();
            }

            var filters = new List<IRemoteSystemFilter> { new RemoteSystemKindFilter(remoteSystemKind), new RemoteSystemDiscoveryTypeFilter(remoteSystemDiscoveryKind) };
            remoteSystemWatcher = RemoteSystem.CreateWatcher(filters);

            remoteSystemWatcher.RemoteSystemAdded += RemoteSystemWatcherOnRemoteSystemAdded;
            remoteSystemWatcher.RemoteSystemRemoved += RemoteSystemWatcher_RemoteSystemRemoved;
            remoteSystemWatcher.RemoteSystemUpdated += RemoteSystemWatcher_RemoteSystemUpdated;

            remoteSystemWatcher.Start();
        }

        private void RemoteSystemWatcher_RemoteSystemUpdated(RemoteSystemWatcher watcher, RemoteSystemUpdatedEventArgs args)
        {
            RunOnUiThread(() =>
                {
                    adapter.Remove(adapter.Items.FirstOrDefault(system => system.Id == args.P0.Id));
                    adapter.Add(args.P0);
                }
            );
        }

        private void RemoteSystemWatcher_RemoteSystemRemoved(RemoteSystemWatcher watcher, RemoteSystemRemovedEventArgs args)
        {
            RunOnUiThread(() =>
                adapter.Remove(adapter.Items.FirstOrDefault(system => system.Id == args.P0))
            );
        }

        private void RemoteSystemWatcherOnRemoteSystemAdded(RemoteSystemWatcher watcher, RemoteSystemAddedEventArgs args)
        {
            RunOnUiThread(() => adapter.Add(args.P0));
        }
    }

    public class RemoteSystemAdapter : ArrayAdapter<RemoteSystem>
    {
        private static readonly Dictionary<RemoteSystemKinds, int> RemoteSystemKindImages = new Dictionary<RemoteSystemKinds, int>
        {
            { RemoteSystemKinds.Desktop, Resource.Drawable.Desktop },
            { RemoteSystemKinds.Phone, Resource.Drawable.Phone },
            { RemoteSystemKinds.Xbox, Resource.Drawable.Xbox },
            { RemoteSystemKinds.Holographic, Resource.Drawable.Hololens },
            { RemoteSystemKinds.Hub, Resource.Drawable.SurfaceHub },
            { RemoteSystemKinds.Unknown, Resource.Drawable.Unknown}
        };

        public List<RemoteSystem> Items { get; }
        Activity context;
        public RemoteSystemAdapter(Activity context, List<RemoteSystem> items)
        : base(context, Resource.Layout.RemoteSystemView, items)
        {
            this.context = context;
            this.Items = items;
        }

        public override View GetView(int position, View convertView, ViewGroup parent)
        {
            var item = this.GetItem(position);

            View view = convertView;
            // no view to re-use, create new
            if (view == null)
            {
                view = context.LayoutInflater.Inflate(Resource.Layout.RemoteSystemView, null);
            }

            view.FindViewById<TextView>(Resource.Id.Text1).Text = item.DisplayName;
            view.FindViewById<TextView>(Resource.Id.Text2).Text = item.IsAvailableByProximity ? "Proximal" : "Cloud";
            int id = RemoteSystemKindImages.ContainsKey(item.Kind) ? RemoteSystemKindImages[item.Kind] : RemoteSystemKindImages[RemoteSystemKinds.Unknown];
            view.FindViewById<ImageView>(Resource.Id.Image).SetImageResource(id);

            return view;
        }
    }

    internal class MsaWebViewClient : WebViewClient
    {
        bool authComplete = false;

        private readonly MainActivity parentActivity;
        public MsaWebViewClient(MainActivity activity)
        {
            this.parentActivity = activity;
        }

        public override void OnPageFinished(WebView view, string url)
        {
            base.OnPageFinished(view, url);
            if (url.Contains("?code=") && !authComplete)
            {
                authComplete = true;
                Console.WriteLine("Page finished successfully");

                var uri = Android.Net.Uri.Parse(url);
                string token = uri.GetQueryParameter("code");
                this.parentActivity.authDialog.Dismiss();
                Platform.SetAuthCode(token);
            }
            else if (url.Contains("error=access_denied"))
            {
                authComplete = true;
                Console.WriteLine("Page finished failed with ACCESS_DENIED_HERE");
                Intent resultIntent = new Intent();
                this.parentActivity.SetResult(0, resultIntent);
                this.parentActivity.authDialog.Dismiss();
            }
        }
    }
    internal class ConnectedDevicesApplication
    {
        public static RemoteSystem SelectedRemoteSystem { get; internal set; }
    }
}
