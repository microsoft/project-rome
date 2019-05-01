/*
 * Copyright(c) Microsoft.All rights reserved.Licensed under the MIT license.
 * See LICENSE in the source repository root for complete license information. 
*/

var _site = (function (site) {

    var msal = site.msalConfig;

    /**
     * Callback method from sign-in: if no errors, show the user details
     * @param {string} errorDesc - If error occur, the error message
     * @param {object} token - The token received from login
     * @param {object} error - The error string
     * @param {string} tokenType - The token type: For loginRedirect, tokenType = "id_token". For acquireTokenRedirect, tokenType:"access_token".
     */
    var loginCallback = function (errorDesc, token, error, tokenType) {
        if (errorDesc) {
            showError(error);
        } else {
            site.login();
        }
    };

    var loggerCallback = function (logLevel, message, piiLoggingEnabled) {
        console.log(message);
    };

    var logger = new Msal.Logger(loggerCallback, { level: Msal.LogLevel.Verbose, correlationId: '12345' });

    // Initialize application
    var userAgentApplication = new Msal.UserAgentApplication(msal.clientId, null, loginCallback, {
        logger: logger,
        cacheLocation: 'localStorage'
    });

    var $enable_obo = $('#enable_obo').prop("checked");
    var $oboAccessToken = null;
    var $accessToken = null;

    /*
     * Step1: Client side logic
     * User clicks the Sign in button and presents their username and password. 
     * We call GetAccessToken method to get relevant credentials from IDS (Identity Services)
     */
    site.login = site.login || function (shouldForcePrompt) {
        var user = userAgentApplication.getUser();
        if (!user && !shouldForcePrompt) {
            return;
        }
        if (!user) {
            GetAccessToken();
        } else {
            // If user is already signed in, display the user info
            delete user.idToken;
            $('#userinfo').text(JSON.stringify(user, null, 4));

            // Now Call Graph API to show the user profile information:            
            // In order to call the Graph API, an access token needs to be acquired.
            // Try to acquire the token used to query Graph API silently first:
            userAgentApplication.acquireTokenSilent(site.appServiceScopes)
                .then(gotToken, function (error) {
                    // If the acquireTokenSilent() method fails, then acquire the token interactively via acquireTokenRedirect().
                    // In this case, the browser will redirect user back to the Azure Active Directory v2 Endpoint so the user 
                    // can reenter the current username/ password and/ or give consent to new permissions your application is requesting.
                    // After authentication/ authorization completes, this page will be reloaded again and callGraphApi() will be executed on page load.
                    // Then, acquireTokenSilent will then get the token silently, the Graph API call results will be made and results will be displayed in the page.
                    if (error && shouldForcePrompt) {
                        showError(error);
                        userAgentApplication.acquireTokenRedirect(site.appServiceScopes);
                    }
                });
        }
    };

    var showError = function (error) {
        $('code#response').text(JSON.stringify(error, null, 2));
    };

    /*
     * Step2: Client Side Logic
     * Using the MSAL Library, we exchange user credentials for an Access Token (AT). Since the OBO flow is not yet enabled ($enable_obo = false),
     * the "scopes" or "resources" for the login request are Graph Notifications related.
     * Scopes/Resources for the request: "https://graph.microsoft.com/User.Read", "Notifications.ReadWrite.CreatedByApp"
     */
     /*
      * Step4: Client Side Logic
      * Since the OBO flow is now enabled ($enable_obo = true), the user's Access Token (AT) is now requested with an 
      * additional App Service specific scope: "api://AppServiceId/User.Read"
      * This new AT will have cumulative resources/scopes (Graph Notifications + A.S.)
      * Please note: this is NOT the OBO token. This is user's AT that contains user's consent to let the A.S. post on its behalf to graph
      * Once this new AT is received, the client should send this to the App Service so that it can use it to get the OBO token.
      */
    function GetAccessToken() {
        var scopes = $enable_obo ? site.appServiceScopes : site.graphNotificationScopes;
        userAgentApplication.loginPopup(scopes)
            .then(function (idToken) {
                //Login Success
                userAgentApplication.acquireTokenSilent(scopes)
                    .then(gotToken, function (error) {
                        //AcquireToken Failure, send an interactive request.
                        userAgentApplication.acquireTokenPopup(scopes)
                            .then(gotToken, function (error) {
                                showError(error);
                            });
                    });
            }, function (error) {
                showError(error);
            });
    }

    var gotToken = function (token) {
        if ($enable_obo) {
            $oboAccessToken = token;
        } else {
            $accessToken = token;
        }
        var user = userAgentApplication.getUser();
        $('#userinfo').text(JSON.stringify(user, null, 4));

        $('#access_token').text(token);
        $('#signin').hide();
        $('#signout').show();
    };

    /*
     * Step5: User Action
     * This method is invoked when we click on the Submit button to create a new notification.
     * This triggers the relevant method in OboController.
     */
    site.createNotificationsViaObo = site.createNotificationsViaObo || function ($payload) {
        var url = location.origin + "/api/obo/";
        $.ajax({
            url: url,
            type: 'post',
            data: $payload,
            headers: {
                "Authorization": "bearer " + $oboAccessToken,
                "ClientId": site.appServiceId
            },
            dataType: 'json',
            contentType: 'application/json',
            complete: function (jqXHR) { showResponse(jqXHR); }
        });
    };

    var showResponse = function (jqXHR) {
        var headers = jqXHR.getAllResponseHeaders();
        headers = jqXHR.status + " " + jqXHR.statusText + "\n" + headers;
        $('code#response').text(headers + "\n" + JSON.stringify(jqXHR.responseJSON, null, 2));
    };

    $(function () {

        site.login(false);

        $('#createRequest').val(JSON.stringify(site.sampleNotificationRequest, null, 2));
        $('#claims').text(JSON.stringify(site.graphNotificationScopes, null, 2));

        $('.btn#put').off('click').on('click', function (e) {
            e.preventDefault(true);
            if (!$accessToken) {
                alert('Please sign in first! Thanks!');
                return
            }
            if (!$enable_obo) {
                alert('Please enable OBO Flow! Thanks!');
                return
            }

            var $json = $('#createRequest').val();
            if ($enable_obo) {
                site.createNotificationsViaObo($json);
            }
        });

        $('.btn#get').off('click').on('click', function (e) {
            e.preventDefault(true);
            if (!$accessToken) {
                alert('Please sign in first! Thanks!');
                return
            }
        });

        /*
         * Step3: Client Side Logic
         * When the "Enable OBO" button is clicked (this is equivalent to asking user if they want to enable Graph Notifications in the app),
         * we request the AT again with more resources/scopes
         */
        $('#enable_obo').off('click').on('click', function (e) {
            if (!$accessToken) {
                alert('Please sign in first! Thanks!');
                $('#enable_obo').attr('checked', false);
                return
            }

            $enable_obo = this.checked;

            // set the sample notification payload if obo is enabled.
            var createRequest = site.sampleNotificationRequest;
            $('#createRequest').val(JSON.stringify(createRequest, null, 2));

            if (($enable_obo && $oboAccessToken == null) ||
                $accessToken == null) { 
                //1. Logged-in through Sign-in button and enabling OBO switch.
                //2. Enable obo switch at first without clicking Sign-in button initally.
                GetAccessToken();
            }
            else if ($oboAccessToken != null && $enable_obo) {
                //set the OBO accessToken for graph which previously acquired
                $('#access_token').text($oboAccessToken);
            }
            else {
                //set the accessToken which previously acquired
                $('#access_token').text($accessToken);
            }
        });

        $('.btn#signin').off('click').on('click', function (e) {
            site.login(true);
        });

        $('.btn#signout').off('click').on('click', function (e) {
            if (userAgentApplication) {
                userAgentApplication.logout();
                userAgentApplication = null;
            }
        });

        if ($accessToken) {
            $('.btn#signin').hide();
            $('.btn#signout').show();
        } else {
            $('.btn#signout').hide();
            $('.btn#signin').show();
        }
    });

    return site;
})(_site || {});

window._site = _site;
