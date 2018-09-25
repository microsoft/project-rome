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

using Microsoft.ConnectedDevices.Core;
using Microsoft.IdentityModel.Clients.ActiveDirectory;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using Windows.Foundation;
using Windows.Security.Authentication.Web;
using Windows.Storage;
using Xamarin.Auth;

namespace SDKTemplate
{
    public class MicrosoftAccountProvider : IConnectedDevicesUserAccountProvider
    {
        static readonly string CCSResource = "https://cdpcs.access.microsoft.com";

        static readonly string MsaTokenKey = "MsaToken";

        public event EventHandler SignOutCompleted;
        public ConnectedDevicesUserAccount SignedInAccount { get; set; }
        public string MsaToken { get; set; }
        public UserInfo AadUser { get; set; }

        public MicrosoftAccountProvider()
        {
            if (ApplicationData.Current.LocalSettings.Values.ContainsKey(MsaTokenKey))
            {
                MsaToken = ApplicationData.Current.LocalSettings.Values[MsaTokenKey] as string;
                SignedInAccount = new ConnectedDevicesUserAccount(Guid.NewGuid().ToString(), ConnectedDevicesUserAccountType.MSA);
            }
            else
            {
                var authContext = new AuthenticationContext("https://login.microsoftonline.com/common");
                if (authContext.TokenCache.Count > 0)
                {
                    SignedInAccount = new ConnectedDevicesUserAccount(Guid.NewGuid().ToString(), ConnectedDevicesUserAccountType.AAD);
                }
            }
        }

        public async Task<bool> SignInAad()
        {
            var result = await GetAadTokenForUserAsync(CCSResource);
            if (result.TokenRequestStatus == ConnectedDevicesAccessTokenRequestStatus.Success)
            {
                SignedInAccount = new ConnectedDevicesUserAccount(Guid.NewGuid().ToString(), ConnectedDevicesUserAccountType.AAD);
                return true;
            }
            return false;
        }

        public async Task<bool> SignInMsa()
        {
            string refreshToken = string.Empty;
            if (ApplicationData.Current.LocalSettings.Values.ContainsKey(MsaTokenKey))
            {
                refreshToken = ApplicationData.Current.LocalSettings.Values[MsaTokenKey] as string;
            }

            if (string.IsNullOrEmpty(refreshToken))
            {
                refreshToken = await MSAOAuthHelpers.GetRefreshTokenAsync();
            }

            if (!string.IsNullOrEmpty(refreshToken))
            {
                MsaToken = refreshToken;
                ApplicationData.Current.LocalSettings.Values[MsaTokenKey] = refreshToken;
                SignedInAccount = new ConnectedDevicesUserAccount(Guid.NewGuid().ToString(), ConnectedDevicesUserAccountType.MSA);
                return true;
            }

            return false;
        }

        public void SignOut()
        {
            MsaToken = string.Empty;
            AadUser = null;
            SignedInAccount = null;
            ApplicationData.Current.LocalSettings.Values.Remove(MsaTokenKey);
            new AuthenticationContext("https://login.microsoftonline.com/common").TokenCache.Clear();
            SignOutCompleted?.Invoke(null, new EventArgs());
        }

        IAsyncOperation<ConnectedDevicesAccessTokenResult> IConnectedDevicesUserAccountProvider.GetAccessTokenForUserAccountAsync(string userAccountId, IReadOnlyList<string> scopes)
        {
            Logger.Instance.LogMessage($"Token requested by platform for {userAccountId} and {string.Join(" ", scopes)}");
            if (SignedInAccount.Type == ConnectedDevicesUserAccountType.AAD)
            {
                return GetAadTokenForUserAsync(scopes.First()).AsAsyncOperation();
            }
            else
            {
                return GetMsaTokenForUserAsync(scopes).AsAsyncOperation();
            }
        }

        void IConnectedDevicesUserAccountProvider.OnAccessTokenError(string userAccountId, IReadOnlyList<string> scopes, bool isPermanentError)
        {
            Logger.Instance.LogMessage($"Bad token reported for {userAccountId} isPermanentError: {isPermanentError}");
        }

        IReadOnlyList<ConnectedDevicesUserAccount> IConnectedDevicesUserAccountProvider.UserAccounts
        {
            get
            {
                var accounts = new List<ConnectedDevicesUserAccount>();
                var account = SignedInAccount;
                if (account != null)
                {
                    accounts.Add(account);
                }
                return accounts;
            }
        }

        event TypedEventHandler<IConnectedDevicesUserAccountProvider, object> IConnectedDevicesUserAccountProvider.UserAccountChanged
        {
            add { return new EventRegistrationToken(); }
            remove { }
        }

        private async Task<ConnectedDevicesAccessTokenResult> GetAadTokenForUserAsync(string audience)
        {
            try
            {
                var authContext = new AuthenticationContext("https://login.microsoftonline.com/common");
                AuthenticationResult result = await authContext.AcquireTokenAsync(
                        audience, Secrets.AAD_CLIENT_ID, new Uri(Secrets.AAD_REDIRECT_URI), new PlatformParameters(PromptBehavior.Auto, true));
                if (AadUser == null)
                {
                    AadUser = result.UserInfo;
                    Logger.Instance.LogMessage($"SignIn done for {AadUser.DisplayableId}");
                }
                Logger.Instance.LogMessage($"AAD Token : {result.AccessToken}");
                return new ConnectedDevicesAccessTokenResult(result.AccessToken, ConnectedDevicesAccessTokenRequestStatus.Success);
            }
            catch (Exception ex)
            {
                Logger.Instance.LogMessage($"AAD Token request failed: {ex.Message}");
                return new ConnectedDevicesAccessTokenResult(string.Empty, ConnectedDevicesAccessTokenRequestStatus.TransientError);
            }
        }

        private async Task<ConnectedDevicesAccessTokenResult> GetMsaTokenForUserAsync(IReadOnlyList<string> scopes)
        {
            try
            {
                string accessToken = await MSAOAuthHelpers.GetAccessTokenUsingRefreshTokenAsync(MsaToken, scopes);
                Logger.Instance.LogMessage($"MSA Token : {accessToken}");
                return new ConnectedDevicesAccessTokenResult(accessToken, ConnectedDevicesAccessTokenRequestStatus.Success);
            }
            catch (Exception ex)
            {
                Logger.Instance.LogMessage($"MSA Token request failed: {ex.Message}");
                return new ConnectedDevicesAccessTokenResult(string.Empty, ConnectedDevicesAccessTokenRequestStatus.TransientError);
            }
        }
    }

    public class MSAOAuthHelpers
    {
        static readonly string ProdAuthorizeUrl = "https://login.live.com/oauth20_authorize.srf";
        static readonly string ProdRedirectUrl = "https://login.microsoftonline.com/common/oauth2/nativeclient";
        static readonly string ProdAccessTokenUrl = "https://login.live.com/oauth20_token.srf";

        static readonly string OfflineAccessScope = "wl.offline_access";
        static readonly string CCSScope = "ccs.ReadWrite";
        static readonly string UserActivitiesScope = "https://activity.windows.com/UserActivity.ReadWrite.CreatedByApp";
        static readonly string UserNotificationsScope = "https://activity.windows.com/Notifications.ReadWrite.CreatedByApp";

        static Random Randomizer = new Random((int)DateTime.Now.Ticks);
        static SHA256 HashProvider = SHA256.Create();

        static async Task<IDictionary<string, string>> RequestAccessTokenAsync(string accessTokenUrl, IDictionary<string, string> queryValues)
        {
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

            string scope = $"{OfflineAccessScope} {CCSScope} {UserNotificationsScope} {UserActivitiesScope}";
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
                    { "client_id", ProdClientId },
                    { "redirect_uri", redirectUri.AbsoluteUri },
                    { "grant_type", "authorization_code" },
                    { "code", authCode },
                    { "code_verifier", codeVerifier },
                    { "scope", CCSScope }
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
                { "client_id", ProdClientId },
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
