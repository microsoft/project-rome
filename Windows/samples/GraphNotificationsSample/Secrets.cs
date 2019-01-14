namespace SDKTemplate
{
    public class Secrets
    {
        // Get the following from Application Registration Portal (apps.dev.microsoft.com)
        // MSA_CLIENT_ID:           Application Id for MSA
        // AAD_CLIENT_ID:           Application Id for AAD
        // AAD_REDIRECT_URI:        Redirect Uri for AAD
        // APP_HOST_NAME            Cross-device domain for UserDataFeed

        // These values are specific to this app, and can't be used by your app. You will need to register
        // your app and get your own secrets, these are ours.
        public static readonly string MSA_CLIENT_ID = "<<MSA client ID goes here>>";
        public static readonly string AAD_CLIENT_ID = "<<AAD client ID goes here>>";
        public static readonly string AAD_REDIRECT_URI = "<<AAD redirect URI goes here>>";
        public static readonly string APP_HOST_NAME = "<<App cross-device domain goes here>>";
    }
}
