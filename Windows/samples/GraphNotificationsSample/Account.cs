//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// This code is licensed under the Microsoft Public License.
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

using Microsoft.ConnectedDevices;
using Microsoft.IdentityModel.Clients.ActiveDirectory;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Runtime.Serialization;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using Windows.Networking.PushNotifications;
using Windows.Security.Authentication.Web;
using Xamarin.Auth;

namespace SDKTemplate
{
    public enum AccountRegistrationState
    {
        InAppCacheAndSdkCache,
        InAppCacheOnly,
        InSdkCacheOnly
    }

    [DataContract]
    public class Account
    {
        [DataMember]
        public String Id { get; set; }

        [DataMember]
        public ConnectedDevicesAccountType Type { get; set; }
        
        [DataMember]
        public String Token { get; set; }

        public AccountRegistrationState RegistrationState { get; set; }

        public UserNotificationsManager UserNotifications { get; set; }

        private ConnectedDevicesPlatform m_platform;

        public Account(ConnectedDevicesPlatform platform, String id, 
            ConnectedDevicesAccountType type, String token, AccountRegistrationState registrationState)
        {
            m_platform = platform;
            Id = id;
            Type = type;
            Token = token;
            RegistrationState = registrationState;

            // Accounts can be in 3 different scenarios:
            // 1: cached account in good standing (initialized in the SDK and our token cache).
            // 2: account missing from the SDK but present in our cache: Add and initialize account.
            // 3: account missing from our cache but present in the SDK. Log the account out async

            // Subcomponents (e.g. UserDataFeed) can only be initialized when an account is in both the app cache 
            // and the SDK cache.
            // For scenario 1, immediately initialize our subcomponents.
            // For scenario 2, subcomponents will be initialized after InitializeAccountAsync registers the account with the SDK.
            // For scenario 3, InitializeAccountAsync will unregister the account and subcomponents will never be initialized.
            if (RegistrationState == AccountRegistrationState.InAppCacheAndSdkCache)
            {
                InitializeSubcomponents();
            }
        }

        public bool EqualsTo(ConnectedDevicesAccount other)
        {
            return ((other.Id == Id) && (other.Type == Type));
        }

        public async Task InitializeAccountAsync()
        {
            if (RegistrationState == AccountRegistrationState.InAppCacheOnly)
            {
                // Scenario 2, add the account to the SDK
                var account = new ConnectedDevicesAccount(Id, Type);
                await m_platform.AccountManager.AddAccountAsync(account);
                RegistrationState = AccountRegistrationState.InAppCacheAndSdkCache;

                InitializeSubcomponents();
                await RegisterAccountWithSdkAsync();
            }
            else if (RegistrationState == AccountRegistrationState.InSdkCacheOnly)
            {
                // Scenario 3, remove the account from the SDK
                var account = new ConnectedDevicesAccount(Id, Type);
                await m_platform.AccountManager.RemoveAccountAsync(account);
            }
        }

        public async Task RegisterAccountWithSdkAsync()
        {
            if (RegistrationState != AccountRegistrationState.InAppCacheAndSdkCache)
            {
                throw new Exception("Account must be in both SDK and App cache before it can be registered");
            }

            var channel = await PushNotificationChannelManager.CreatePushNotificationChannelForApplicationAsync();
            ConnectedDevicesNotificationRegistration registration = new ConnectedDevicesNotificationRegistration();
            registration.Type = ConnectedDevicesNotificationType.WNS;
            registration.Token = channel.Uri;
            var account = new ConnectedDevicesAccount(Id, Type);
            var registerResult = await m_platform.NotificationRegistrationManager.RegisterAsync(account, registration);

            // It would be a good idea for apps to take a look at the different statuses here and perhaps attempt some sort of remediation.
            // For example, web failure may indicate that a web service was temporarily in a bad state and retries may be successful.
            // 
            // NOTE: this approach was chosen rather than using exceptions to help separate "expected" / "retry-able" errors from real 
            // exceptions and keep the error-channel logic clean and simple.
            if (registerResult.Status == ConnectedDevicesNotificationRegistrationStatus.Success)
            {
                await UserNotifications.RegisterAccountWithSdkAsync();
            }
        }

        public async Task LogoutAsync()
        {
            ClearSubcomponents();
            await m_platform.AccountManager.RemoveAccountAsync(new ConnectedDevicesAccount(Id, Type));
            RegistrationState = AccountRegistrationState.InAppCacheOnly;
        }

        private void InitializeSubcomponents()
        {
            if (RegistrationState != AccountRegistrationState.InAppCacheAndSdkCache)
            {
                throw new Exception("Account must be in both SDK and App cache before subcomponents can be initialized");
            }

            var account = new ConnectedDevicesAccount(Id, Type);
            UserNotifications = new UserNotificationsManager(m_platform, account);
        }

        private void ClearSubcomponents()
        {
            UserNotifications.Reset();
            UserNotifications = null;
        }

        public async Task<String> GetAccessTokenAsync(IReadOnlyList<string> scopes)
        {
            if (Type == ConnectedDevicesAccountType.MSA)
            {
                return await MSAOAuthHelpers.GetAccessTokenUsingRefreshTokenAsync(Token, scopes);
            }
            else if (Type == ConnectedDevicesAccountType.AAD)
            {
                var authContext = new AuthenticationContext("https://login.microsoftonline.com/common");

                UserIdentifier aadUserId = new UserIdentifier(Id, UserIdentifierType.UniqueId);
                AuthenticationResult result;
                try
                {
                    result = await authContext.AcquireTokenSilentAsync(scopes[0], Secrets.AAD_CLIENT_ID);
                }
                catch (Exception ex)
                {
                    Logger.Instance.LogMessage($"Token request failed: {ex.Message}");
                    
                    // Token may have expired, try again non-silently
                    result = await authContext.AcquireTokenAsync(scopes[0], Secrets.AAD_CLIENT_ID,
                        new Uri(Secrets.AAD_REDIRECT_URI), new PlatformParameters(PromptBehavior.Auto, true));
                }

                return result.AccessToken;
            }
            else
            {
                throw new Exception("Invalid Account Type");
            }
        }

        public static async Task<AuthenticationResult> GetAadTokenAsync(string scope)
        {
            var authContext = new AuthenticationContext("https://login.microsoftonline.com/common");
            return await authContext.AcquireTokenAsync(scope, Secrets.AAD_CLIENT_ID, new Uri(Secrets.AAD_REDIRECT_URI), 
                new PlatformParameters(PromptBehavior.Auto, true));
        }
    }

    public class MSAOAuthHelpers
    {
        static readonly string ProdAuthorizeUrl = "https://login.live.com/oauth20_authorize.srf";
        static readonly string ProdRedirectUrl = "https://login.microsoftonline.com/common/oauth2/nativeclient";
        static readonly string ProdAccessTokenUrl = "https://login.live.com/oauth20_token.srf";

        static readonly string OfflineAccessScope = "wl.offline_access";
        static readonly string WNSScope = "wns.connect";
        static readonly string DdsScope = "dds.register dds.read";
        static readonly string CCSScope = "ccs.ReadWrite";
        static readonly string UserActivitiesScope = "https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp";
        static readonly string UserNotificationsScope = "https://activity.windows.com/Notifications.ReadWrite.CreatedByApp";

        static Random Randomizer = new Random((int)DateTime.Now.Ticks);
        static SHA256 HashProvider = SHA256.Create();

        static async Task<IDictionary<string, string>> RequestAccessTokenAsync(string accessTokenUrl, IDictionary<string, string> queryValues)
        {
            // mc++ changed protected to public for extension methods RefreshToken (Adrian Stevens) 
            var content = new FormUrlEncodedContent(queryValues);

            HttpClient client = new HttpClient();
            HttpResponseMessage response = await client.PostAsync(accessTokenUrl, content).ConfigureAwait(false);
            string text = await response.Content.ReadAsStringAsync().ConfigureAwait(false);

            // Parse the response
            IDictionary<string, string> data = text.Contains("{") ? WebEx.JsonDecode(text) : WebEx.FormDecode(text);
            if (data.ContainsKey("error"))
            {
                throw new AuthException(data["error_description"]);
            }

            return data;
        }

        public static async Task<string> GetRefreshTokenAsync()
        {
            byte[] buffer = new byte[32];
            Randomizer.NextBytes(buffer);
            var codeVerifier = Convert.ToBase64String(buffer).Replace('+', '-').Replace('/', '_').Replace("=", "");

            byte[] hash = HashProvider.ComputeHash(Encoding.UTF8.GetBytes(codeVerifier));
            var codeChallenge = Convert.ToBase64String(hash).Replace('+', '-').Replace('/', '_').Replace("=", "");

            var redirectUri = new Uri(ProdRedirectUrl);

            string scope = $"{OfflineAccessScope} {WNSScope} {CCSScope} {UserNotificationsScope} {UserActivitiesScope} {DdsScope}";
            var startUri = new Uri($"{ProdAuthorizeUrl}?client_id={Secrets.MSA_CLIENT_ID}&response_type=code&code_challenge_method=S256&code_challenge={codeChallenge}&redirect_uri={ProdRedirectUrl}&scope={scope}");

            var webAuthenticationResult = await WebAuthenticationBroker.AuthenticateAsync(
                WebAuthenticationOptions.None,
                startUri,
                redirectUri);

            if (webAuthenticationResult.ResponseStatus == WebAuthenticationStatus.Success)
            {
                var codeResponseUri = new Uri(webAuthenticationResult.ResponseData);
                IDictionary<string, string> queryParams = WebEx.FormDecode(codeResponseUri.Query);
                if (!queryParams.ContainsKey("code"))
                {
                    return string.Empty;
                }

                string authCode = queryParams["code"];
                Dictionary<string, string> refreshTokenQuery = new Dictionary<string, string>
                {
                    { "client_id", Secrets.MSA_CLIENT_ID },
                    { "redirect_uri", redirectUri.AbsoluteUri },
                    { "grant_type", "authorization_code" },
                    { "code", authCode },
                    { "code_verifier", codeVerifier },
                    { "scope", WNSScope }
                };

                IDictionary<string, string> refreshTokenResponse = await RequestAccessTokenAsync(ProdAccessTokenUrl, refreshTokenQuery);
                if (refreshTokenResponse.ContainsKey("refresh_token"))
                {
                    return refreshTokenResponse["refresh_token"];
                }
            }

            return string.Empty;
        }

        public static async Task<string> GetAccessTokenUsingRefreshTokenAsync(string refreshToken, IReadOnlyList<string> scopes)
        {
            Dictionary<string, string> accessTokenQuery = new Dictionary<string, string>
            {
                { "client_id", Secrets.MSA_CLIENT_ID },
                { "redirect_uri", ProdRedirectUrl },
                { "grant_type", "refresh_token" },
                { "refresh_token", refreshToken },
                { "scope", string.Join(" ", scopes.ToArray()) },
            };

            IDictionary<string, string> accessTokenResponse = await RequestAccessTokenAsync(ProdAccessTokenUrl, accessTokenQuery);
            if (accessTokenResponse == null || !accessTokenResponse.ContainsKey("access_token"))
            {
                throw new Exception("Unable to fetch access_token!");
            }

            return accessTokenResponse["access_token"];
        }
    }
}
