var _site = (function (site) {

    //Step0: Update Config: Please enter client id from Azure Portal
    site.appServiceId = "PleaseEnterValue";

    //Step0: Update Config: Please enter targetHostName 
    //TargetHostName must match Domain Name registered in Dev Portal with your client Id
    site.targetHostName = "PleaseEnterValue";

    site.appServiceScopes = ["api://" + site.appServiceId + "/User.Read"]; //Scopes for the App Service
    site.graphNotificationScopes = ["https://graph.microsoft.com/User.Read", "https://graph.microsoft.com/Notifications.ReadWrite.CreatedByApp"]; //Scopes for Graph Notifications API

    // Graph notification sample request payload
    site.sampleNotificationRequest = {
        "targetHostName": site.targetHostName, 
        "appNotificationId": "sampleRawNotification",
        "payload": {
            "rawContent": "Aren't Notifications the best??!!"
        },
        "targetPolicy": {
            "platformTypes": [
                "windows"
            ]
        },
        "priority": "High",
        "displayTimeToLive": "60"
    };

    site.msalConfig = {
        clientId: site.appServiceId,
        redirectUri: location.origin,
        tenant: "common",
        authority: "https://login.microsoftonline.com/{0}/v2.0"
    };

    return site;
})(_site || {});

window._site = _site;
