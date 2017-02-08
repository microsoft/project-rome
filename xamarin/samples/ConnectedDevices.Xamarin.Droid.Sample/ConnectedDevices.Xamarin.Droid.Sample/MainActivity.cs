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
using Android.Content;
using Android.Widget;
using Android.OS;
using Android.Webkit;
using Microsoft.ConnectedDevices;
using System.Collections.Generic;
using System.Linq;
using Android.Views;

namespace ConnectedDevices.Xamarin.Droid.Sample
{
    [Activity(Label = "Connected Devices", MainLauncher = true, Icon = "@drawable/icon")]
    public class MainActivity : ListActivity
    {
        // Use your own client id
        //private const string CLIENT_ID = ""; //get a client ID from https://apps.dev.microsoft.com/
        private const string CLIENT_ID = "4243aabb-d2e3-423f-ad85-de5c284b5b1c";


        private WebView _webView;
        internal Dialog _authDialog;
        RemoteSystemAdapter _adapter;

        private RemoteSystemWatcher _remoteSystemWatcher;

        protected override void OnCreate(Bundle bundle)
        {
            base.OnCreate(bundle);

            // Set our view from the "main" layout resource
            SetContentView(Resource.Layout.Main);

            Button callButton = FindViewById<Button>(Resource.Id.RefreshButton);
            callButton.Click += (object sender, EventArgs e) =>
            {
                RefreshDevices();
            };

            if(string.IsNullOrEmpty(CLIENT_ID))
            {
                Toast.MakeText(this, "CLIENT_ID not set!", ToastLength.Long).Show();
            }

            InitializeAsync();

            _adapter = new RemoteSystemAdapter(this, new List<RemoteSystem>());
            this.ListAdapter = _adapter;
        }

        internal async void InitializeAsync()
        {
            Platform.FetchAuthCode += Platform_FetchAuthCode;
            var result = await Platform.InitializeAsync(this.ApplicationContext, CLIENT_ID);

            if (result == true)
            {
                Console.WriteLine("Initialized platform successfully");
                RefreshDevices();
            }
            else
            {
                Console.WriteLine("ConnectedDevices Platform initialization failed");
            }
        }

        private void Platform_FetchAuthCode(string oauthUrl)
        {
            _authDialog = new Dialog(this);

            var linearLayout = new LinearLayout(_authDialog.Context);
            _webView = new WebView(_authDialog.Context);
            linearLayout.AddView(_webView);
            _authDialog.SetContentView(linearLayout);

            _webView.SetWebChromeClient(new WebChromeClient());
            _webView.Settings.JavaScriptEnabled = true;
            _webView.Settings.DomStorageEnabled = true;
            _webView.LoadUrl(oauthUrl);

            _webView.SetWebViewClient(new MsaWebViewClient(this));
            _authDialog.Show();
            _authDialog.SetCancelable(true);
        }

        internal void RefreshDevices()
        {
            _adapter.Clear();
             DiscoverDevices();
        }

        private void DiscoverDevices()
        {
            _remoteSystemWatcher = RemoteSystem.CreateWatcher();

            _remoteSystemWatcher.RemoteSystemAdded += RemoteSystemWatcherOnRemoteSystemAdded;
            _remoteSystemWatcher.RemoteSystemRemoved += RemoteSystemWatcher_RemoteSystemRemoved;
            _remoteSystemWatcher.RemoteSystemUpdated += RemoteSystemWatcher_RemoteSystemUpdated;

            _remoteSystemWatcher.Start();
        }

        private void RemoteSystemWatcher_RemoteSystemUpdated(RemoteSystemWatcher watcher, RemoteSystemUpdatedEventArgs args)
        {
            RunOnUiThread(() =>
                {
                    _adapter.Remove(_adapter.Items.FirstOrDefault(system => system.Id == args.P0.Id));
                    _adapter.Add(args.P0);
                }
            );
        }

        private void RemoteSystemWatcher_RemoteSystemRemoved(RemoteSystemWatcher watcher, RemoteSystemRemovedEventArgs args)
        {
            RunOnUiThread(() =>
                _adapter.Remove(_adapter.Items.FirstOrDefault(system => system.Id == args.P0))
            );
        }

        private void RemoteSystemWatcherOnRemoteSystemAdded(RemoteSystemWatcher watcher, RemoteSystemAddedEventArgs args)
        {
            RunOnUiThread(() => _adapter.Add(args.P0));
        }

        protected override void OnListItemClick(ListView l, View v, int position, long id)
        {
            var detailsActivity = new Intent(this, typeof(RemoteSystemActivity));

            RemoteSystem selectedDevice = _adapter.GetItem(position);

            ConnectedDevicesApplication.SelectedRemoteSystem = selectedDevice;

            StartActivity(detailsActivity);
        }


    }
    public class RemoteSystemAdapter : ArrayAdapter<RemoteSystem>
    {
        private static readonly Dictionary<RemoteSystemKind, int> RemoteSystemKindImages = new Dictionary<RemoteSystemKind, int>
        {
            { RemoteSystemKind.Desktop, Resource.Drawable.Desktop },
            { RemoteSystemKind.Phone, Resource.Drawable.Phone },
            { RemoteSystemKind.Xbox, Resource.Drawable.Xbox },
            { RemoteSystemKind.Holographic, Resource.Drawable.Hololens },
            { RemoteSystemKind.Hub, Resource.Drawable.SurfaceHub },
            { RemoteSystemKind.Unknown, Resource.Drawable.Unknown}
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
            if (view == null) // no view to re-use, create new
                view = context.LayoutInflater.Inflate(Resource.Layout.RemoteSystemView, null);
            view.FindViewById<TextView>(Resource.Id.Text1).Text = item.DisplayName;
            view.FindViewById<TextView>(Resource.Id.Text2).Text = item.IsAvailableByProximity ? "Proximal" : "Cloud";
            int id = RemoteSystemKindImages.ContainsKey(item.Kind) ? RemoteSystemKindImages[item.Kind] : RemoteSystemKindImages[RemoteSystemKind.Unknown];
            view.FindViewById<ImageView>(Resource.Id.Image).SetImageResource(id);

            return view;
        }
    }

    internal class MsaWebViewClient : WebViewClient
    {
        bool authComplete = false;

        private readonly MainActivity _parentActivity;
        public MsaWebViewClient(MainActivity activity)
        {
            _parentActivity = activity;
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
                _parentActivity._authDialog.Dismiss();
                Platform.SetAuthCode(token);
            }
            else if (url.Contains("error=access_denied"))
            {
                authComplete = true;
                Console.WriteLine("Page finished failed with ACCESS_DENIED_HERE");
                Intent resultIntent = new Intent();
                _parentActivity.SetResult(0, resultIntent);
                _parentActivity._authDialog.Dismiss();
            }
            
        }
    }
    internal class ConnectedDevicesApplication
    {
        public static RemoteSystem SelectedRemoteSystem { get; internal set; }
    }
}

