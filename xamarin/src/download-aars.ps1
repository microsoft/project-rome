$version = "0.1.0"

"Downloading connecteddevices-core-armv7-externalRelease.aar..."
Invoke-WebRequest https://projectrome.bintray.com/maven/com/microsoft/connecteddevices/connecteddevices-core-armv7/$version/connecteddevices-core-armv7-$version-externalRelease.aar -OutFile ConnectedDevices.Xamarin.Droid.Core\Jars\connecteddevices-core-armv7-externalRelease.aar

"Downloading connecteddevices-sdk-armv7-externalRelease.aar..."
Invoke-WebRequest https://projectrome.bintray.com/maven/com/microsoft/connecteddevices/connecteddevices-sdk-armv7/$version/connecteddevices-sdk-armv7-$version-externalRelease.aar -OutFile ConnectedDevices.Xamarin.Droid\Jars\connecteddevices-sdk-armv7-externalRelease.aar