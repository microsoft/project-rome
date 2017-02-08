
## Project Rome Connected Devices Plugin for Xamarin on Android

Xamarin plugin to allow access to the Project Rome Connected Device APIs on Android. Discover, launch and message from Android application to Windows devices and applications.

### Blog Post Walkthrough

[Connected Apps and Devices](https://msdn.microsoft.com/windows/uwp/launch-resume/connected-apps-and-devices)


### Setup
* Available on NuGet: https://www.nuget.org/packages/Microsoft.ConnectedDevices.Xamarin.Droid [![NuGet](https://img.shields.io/nuget/v/Microsoft.ConnectedDevices.Xamarin.Droid.svg?label=NuGet)](https://www.nuget.org/packages/Microsoft.ConnectedDevices.Xamarin.Droid/)

**Platform Support**

|Platform|Supported|Version|
| ------------------- | :-----------: | :------------------: |
|Xamarin.Android|Yes|API 19+|


### API Usage

#### Pre-requisites
1. Visual Studio 2015 or 2017 RC with Xamarin or Xamarin Studio
2. Register your applciation and obtain an MSA client ID from 
[https://apps.dev.microsoft.com](https://apps.dev.microsoft.com)

#### Getting Started
Initialize the Connected Devices Platform
```csharp
Platform.FetchAuthCode += Platform_FetchAuthCode;
var result = await Platform.InitializeAsync(this.ApplicationContext, CLIENT_ID);
```

The FetchAuthCode handler is used when the platform needs an authorization code from the user (i.e. form OAuth with Microsoft Account). See the sample for more details.
```csharp
private async void Platform_FetchAuthCode(string oauthUrl)
{
    var authCode = await AuthenticateWithOAuth(oauthUrl);
    Platform.SetAuthCode(token);
}
```

Now, discover devices
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

Finally, connect and launch URIs using LaunchUriAsync
```csharp
private async void RemoteLaunchUri(RemoteSystem remoteSystem, Uri uri)
{
    var launchUriStatus = await RemoteLauncher.LaunchUriAsync(new RemoteSystemConnectionRequest(remoteSystem), uri);
}
```

#### License

The MIT License (MIT)

Copyright (c) Microsoft Corporation

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
