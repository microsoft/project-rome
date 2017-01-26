## Project Rome Connected Devices Plugin for Xamarin on Android

Xamarin plugin to allow access to the Project Rome Connected Device APIs on Android. Discover, launch and message from Android application to Windows devices and applications.

### Blog Post Walkthrough

[Connected Apps and Devices](https://msdn.microsoft.com/windows/uwp/launch-resume/connected-apps-and-devices)


### Setup
* Available on NuGet: http://www.nuget.org/packages/Microsoft.ConnectedDevices [![NuGet](https://img.shields.io/nuget/v/Microsoft.ConnectedDevices.svg?label=NuGet)](https://www.nuget.org/packages/Microsoft.ConnectedDevices/)

**Platform Support**

|Platform|Supported|Version|
| ------------------- | :-----------: | :------------------: |
|Xamarin.Android|Yes|API 14+|


### API Usage

#### Pre-requisites
1. Visual Studio 2015 or 2017 RC with Xamarin or Xamarin Studio
2. Register your applciation and obtain an MSA client ID from 
[https://apps.dev.microsoft.com](https://apps.dev.microsoft.com)

#### Getting Started
1. Initialize the Connected Devices Platform
```csharp
Platform.FetchAuthCode += Platform_FetchAuthCode;
var result = await Platform.InitializeAsync(this.ApplicationContext, CLIENT_ID);
```

2. The FetchAuthCode handler is used when the platform needs an authorization code from the user (i.e. form OAuth with Microsoft Account). See the sample for more details.
```csharp
private async void Platform_FetchAuthCode(string oauthUrl)
{
    var authCode = await AuthenticateWithOAuth(oauthUrl);
    Platform.SetAuthCode(token);
}
```

3. Now, discover devices
```csharp
private RemoteSystemWatcher _remoteSystemWatcher;
private void DiscoverDevices()
{
    _remoteSystemWatcher = RemoteSystem.CreateWatcher();
    _remoteSystemWatcher.RemoteSystemAdded += (sender, args) =>
    {
        Console.WriteLine("Discovered Device: " + args.P0.DisplayName);
    };
    _remoteSystemWatcher.Start();
}
```

4. Finally, connect and launch URIs using LaunchUriAsync
```csharp
private async void RemoteLaunchUri(RemoteSystem remoteSystem, Uri uri)
{
    var launchUriStatus = await RemoteLauncher.LaunchUriAsync(new RemoteSystemConnectionRequest(remoteSystem), uri);
}
```

#### License
