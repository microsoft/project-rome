using Android.App;
using Android.Widget;
using Android.OS;
using Microsoft.ConnectedDevices;
using Android.Webkit;
using System;
using Android.Content;
using System.Collections.Generic;
using Android.Views;
using System.Linq;

namespace RomeCastRemote
{
	[Activity(Label = "RomeCast Remote Control", MainLauncher = true, Icon = "@mipmap/icon")]
	public class MainActivity : Activity
	{
		// Use your own client id
		private const string CLIENT_ID = ""; //get a client ID from https://apps.dev.microsoft.com/

		private WebView _webView;
		internal Dialog _authDialog;

		private RemoteSystemWatcher _remoteSystemWatcher;
		ArrayAdapter<String> _adapter;
		List<RemoteSystem> _remoteSystems = new List<RemoteSystem>();
		RemoteSystem _selectedDevice;

		protected override async void OnCreate(Bundle savedInstanceState)
		{
			base.OnCreate(savedInstanceState);

			// Set our view from the "main" layout resource
			SetContentView(Resource.Layout.Main);

			Platform.FetchAuthCode += Platform_FetchAuthCode;
			var result = await Platform.InitializeAsync(this.ApplicationContext, CLIENT_ID);

            DiscoverDevices();

            Spinner spinner = FindViewById<Spinner>(Resource.Id.spinner);

			spinner.ItemSelected += new EventHandler<AdapterView.ItemSelectedEventArgs>(spinner_ItemSelected);

			_adapter = new ArrayAdapter<string>(this, Android.Resource.Layout.SimpleSpinnerItem, new List<string>()); //selected item will look like a spinner set from XML
			_adapter.SetDropDownViewResource(Android.Resource.Layout.SimpleSpinnerDropDownItem); 
			spinner.Adapter = _adapter;

            FindViewById<Button>(Resource.Id.next).Click += (object sender, EventArgs e) => RemoteLaunchUriAsync(_selectedDevice, new Uri("romecast://command?prev"));
            FindViewById<Button>(Resource.Id.play).Click += (object sender, EventArgs e) => RemoteLaunchUriAsync(_selectedDevice, new Uri("romecast://command?play"));
            FindViewById<Button>(Resource.Id.pause).Click += (object sender, EventArgs e) => RemoteLaunchUriAsync(_selectedDevice, new Uri("romecast://command?pause"));
            FindViewById<Button>(Resource.Id.next).Click += (object sender, EventArgs e) => RemoteLaunchUriAsync(_selectedDevice, new Uri("romecast://command?next"));



        }

        private void spinner_ItemSelected(object sender, AdapterView.ItemSelectedEventArgs e)
		{
			Spinner spinner = (Spinner)sender;

			var deviceName = (string) spinner.GetItemAtPosition(e.Position);
			_selectedDevice = _remoteSystems.FirstOrDefault(d => d.DisplayName == deviceName) ;
		}

        private async void RemoteLaunchUriAsync(RemoteSystem remoteSystem, Uri uri)
        {
            if (remoteSystem != null)
            {
                var launchUriStatus = await RemoteLauncher.LaunchUriAsync(new RemoteSystemConnectionRequest(remoteSystem), uri);

                if (launchUriStatus != RemoteLaunchUriStatus.Success)
                {
                    Console.WriteLine("Failed to Launch!");
                }
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
					_remoteSystems.RemoveAll(system => system.Id == args.P0.Id);
					_remoteSystems.Add(args.P0);
				}
			);
		}

		private void RemoteSystemWatcher_RemoteSystemRemoved(RemoteSystemWatcher watcher, RemoteSystemRemovedEventArgs args)
		{
			_remoteSystems.RemoveAll(system => system.Id == args.P0);
			
			RunOnUiThread(() => _adapter.Remove(args.P0));
		}

		private void RemoteSystemWatcherOnRemoteSystemAdded(RemoteSystemWatcher watcher, RemoteSystemAddedEventArgs args)
		{
			_remoteSystems.Add(args.P0);

			RunOnUiThread(() => _adapter.Add(args.P0.DisplayName));
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
}

