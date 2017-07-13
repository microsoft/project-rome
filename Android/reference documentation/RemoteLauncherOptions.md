# RemoteLauncherOptions class
This class specifies the options used to launch the default app for URI on a remote device.

## Syntax
`public class RemoteLauncherOptions`

## Public constructors

### RemoteLauncherOptions
Initializes an instance of the RemoteLauncherOptions class.

`RemoteLauncherOptions(List<String> preferredAppIds, String fallbackUri)`

#### Parameters  
*preferredAppIds* - a list of global identifiers of apps that should be used to launch the URI on the remote device. The first string on the list should correspond to the preferred application.  
*fallbackUri* - The URI of the web site to open in the event that the original URI cannot be handled by the intended app

## Public methods

### getFallbackUri
Returns the URI of the web site to open in the event that the original URI cannot be handled by the intended app.

`public String getFallbackUri()`

#### return value  
The fallback URI String

### getPreferredAppIds
Returns the list of global identifiers of apps that should be used to launch the URI on the remote device.

`public List<String> getPreferredAppIds()`

#### return value  
The list of preferred app identifiers
