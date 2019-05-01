# Graph Notifications Sample App Service

## Terminology
 
* Sample App/Client App => Windows App/iOS App/Android App/Web App where user signs in 
* Sample API/App Service => Service that wants to post a notification
* Azure AD => Identity Service
* AFS/WNS => Internal Microsoft Services to support graph notifications scenario

## Summary

When posting a notification for a user, an App Service (A.S.) like Outlook, needs a way to identify the user. The current Graph Notifications code supports this through **user credentails**. The client app where the user signs in (like Outlook app on Windows/iOS/Android), can send the Access Token (AT) over to the App Service which can then be used to send notifications to the User. However, the AT expires in an hour, and the client app would need to keep sending this AT every hour. For a more permanent solution, we can use the [On Behalf-Of (OBO) flow](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-on-behalf-of-flow). In this flow, the user sends in the AT which is then exchanged by the A.S. for an OBO token from Identity Service (IDS) using its own credentails.

## Sample

This is the sample code for an App Service that can create a notification for a user using OBO flow described above.

In the usual flow, user credentials are sent from client app to the App Service. In this sample, the user signs in directly into the App Service to provide their credentials. This part of the code is labeled as "client side code" in the js files to clarify how code should be structured (Please pay special attention to the scopees in authentication requests for the user). The App Service Specific code is in the Obocontroller). 

## Setting up the sample

* Clone the code from the repo

* Please update the following credentials in the code:
  * appServiceClientId (in siteConfig.js): This is the App Service Id registered in Azure Portal
  * appServiceClientSecret (in Web.config): This is the App Service Secret registered in Azure Portal
  * TargetHostName: This is the Domain name that was verified and associated with the App Service Id in Dev Portal

* Deploy locally using IIS Express in VS
 
## Explanation of demo flow: 

### Scenario: Getting User's AT with the right Scopes. (This logic will exist in the client app)
 
* Open demo website and Click on "Sign In with Microsoft"
  * Login popup will open. (Make sure popup blocker is disabled). (Ref Code: site.login in site.js)
  * Use AAD for login. Consent popup will open for Graph authorizations scopes. (Ref Code:  site.graphNotificationScopes in siteConfig.js) to to grant right permissions (add right scopes)
 
* In the UI, Enable the “Access resources via On-Behalf-of” switch for getting right permissions/scope for OBO flow. 
  * Consent popup will open to authorize App Service scopes. (Ref Code: site.appServiceScopes in siteConfig.js)
  * Access token will be generated with cumulative scopes.
 
### Scenario: Exchange User's AT for OBO AT/RT and hit Graph Endpoint
 
* Hit the Submit Button. (Ref Code:  site.createNotificationsViaObo in site.js). This will do the following: 
  * Send request to AAD to exchange User's AT for OBO token (Ref Code:  PostAsync -> GetOboTokenAsync in OboController.cs). 
  * Send request payload to Graph endpoint to target notification with the OBO Token

You should only need client credentials (in the form of Access Token) only once and don’t need to persist this anywhere. The App Service will use the client AT to get OBO Access Tokens (AT) and Refresh Tokens (RT) as described above. The App Service will need to maintain a list of these AT/RT pair per user. AT expires every 60 minutes, and a RT can be used to retrieve a new AT.

This sounds cumbersome, and we agree. OBO flow is not the long term recommended solution for user identification for Graph Notifications scenario and has been provided as a means to unblock E2E scenario for the immediate future.

An updated version of SDK and API will be available soon which will eliminate the need for user auth completely and instead use the concept of User identifier (UNSId). This User Identifier will be obtained during subscription under the covers and needs to be sent to the App Service only once. The App Service will only need to maintain User and UNSId mapping going forward.

### Other Useful Links:
 
* [Token lifetimes](https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-configurable-token-lifetimes#configurable-token-lifetime-properties)
 
* [Register an App with Azure Active Directory (AAD) V2 endpoint](https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-v2-register-an-app)
 
* [Exchange On-Behalf-Of token](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-on-behalf-of-flow)
 
* [MSA Users](https://docs.microsoft.com/en-us/azure/active-directory/develop/azure-ad-endpoint-comparison)
 
* How to enable Cross-device experience for notifications:
  * [Guide for Android](https://docs.microsoft.com/en-us/windows/project-rome/notifications/how-to-guide-for-android)
  * [Guide for iOS](https://docs.microsoft.com/en-us/windows/project-rome/notifications/how-to-guide-for-ios)
  * [Guide for Windows](https://docs.microsoft.com/en-us/windows/project-rome/notifications/how-to-guide-for-windows)
 
