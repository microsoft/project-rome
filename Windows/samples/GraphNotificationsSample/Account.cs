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
using Microsoft.ConnectedDevices.UserData;
using Microsoft.ConnectedDevices.UserData.UserNotifications;
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
using Windows.Data.Xml.Dom;
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

    public class UserNotificationsManager
    {
        private UserDataFeed m_feed;
        private UserNotificationReader m_reader;
        private UserNotificationChannel m_channel;

        public event EventHandler CacheUpdated;
        private List<UserNotification> m_newNotifications = new List<UserNotification>();
        public bool NewNotifications
        {
            get
            {
                return m_newNotifications.Count > 0;
            }
        }

        private List<UserNotification> m_historicalNotifications = new List<UserNotification>();
        public IReadOnlyList<UserNotification> HistoricalNotifications
        {
            get
            {
                return m_historicalNotifications.AsReadOnly();
            }
        }

        public UserNotificationsManager(ConnectedDevicesPlatform platform, ConnectedDevicesAccount account)
        {
            m_feed = UserDataFeed.GetForAccount(account, platform, Secrets.APP_HOST_NAME);
            m_feed.SyncStatusChanged += Feed_SyncStatusChanged;

            m_channel = new UserNotificationChannel(m_feed);
            m_reader = m_channel.CreateReader();
            m_reader.DataChanged += Reader_DataChanged;
            Logger.Instance.LogMessage($"Setup feed for {account.Id} {account.Type}");
        }

        public async Task RegisterAccountWithSdkAsync()
        {
            var scopes = new List<IUserDataFeedSyncScope> { UserNotificationChannel.SyncScope };
            bool registered = await m_feed.SubscribeToSyncScopesAsync(scopes);
            if (!registered)
            {
                throw new Exception("Subscribe failed");
            }
        }

        private void Feed_SyncStatusChanged(UserDataFeed sender, object args)
        {
            Logger.Instance.LogMessage($"SyncStatus is {sender.SyncStatus.ToString()}");
        }

        private async void Reader_DataChanged(UserNotificationReader sender, object args)
        {
            Logger.Instance.LogMessage("New notification available");
            await ReadNotificationsAsync(sender);
        }

        public async Task RefreshAsync()
        {
            Logger.Instance.LogMessage("Read cached notifications");
            await ReadNotificationsAsync(m_reader);

            Logger.Instance.LogMessage("Request another sync");
            m_feed.StartSync();
        }

        private async Task ReadNotificationsAsync(UserNotificationReader reader)
        {
            var notifications = await reader.ReadBatchAsync(UInt32.MaxValue);
            Logger.Instance.LogMessage($"Read {notifications.Count} notifications");

            foreach (var notification in notifications)
            {
                if (notification.Status == UserNotificationStatus.Active)
                {
                    m_newNotifications.RemoveAll((n) => { return (n.Id == notification.Id); });
                    if (notification.UserActionState == UserNotificationUserActionState.NoInteraction)
                    {
                        // Brand new notification, add to new
                        m_newNotifications.Add(notification);
                        Logger.Instance.LogMessage($"UserNotification not interacted: {notification.Id}");
                        if (!string.IsNullOrEmpty(notification.Content) && notification.ReadState != UserNotificationReadState.Read)
                        {
                            RemoveToastNotification(notification.Id);
                            ShowToastNotification(BuildToastNotification(notification.Id, notification.Content));
                        }
                    }
                    else
                    {
                        RemoveToastNotification(notification.Id);
                    }

                    m_historicalNotifications.RemoveAll((n) => { return (n.Id == notification.Id); });
                    m_historicalNotifications.Insert(0, notification);
                }
                else
                {
                    // Historical notification is marked as deleted, remove from display
                    m_newNotifications.RemoveAll((n) => { return (n.Id == notification.Id); });
                    m_historicalNotifications.RemoveAll((n) => { return (n.Id == notification.Id); });
                    RemoveToastNotification(notification.Id);
                }
            }

            CacheUpdated?.Invoke(this, new EventArgs());
        }

        public async Task ActivateAsync(string id, bool dismiss)
        {
            var notification = m_historicalNotifications.Find((n) => { return (n.Id == id); });
            if (notification != null)
            {
                notification.UserActionState = dismiss ? UserNotificationUserActionState.Dismissed : UserNotificationUserActionState.Activated;
                await notification.SaveAsync();
                RemoveToastNotification(notification.Id);
                Logger.Instance.LogMessage($"{notification.Id} is now DISMISSED");
            }
        }

        public async Task MarkReadAsync(string id)
        {
            var notification = m_historicalNotifications.Find((n) => { return (n.Id == id); });
            if (notification != null)
            {
                notification.ReadState = UserNotificationReadState.Read;
                await notification.SaveAsync();
                Logger.Instance.LogMessage($"{notification.Id} is now READ");
            }
        }

        public async Task DeleteAsync(string id)
        {
            var notification = m_historicalNotifications.Find((n) => { return (n.Id == id); });
            if (notification != null)
            {
                await m_channel.DeleteUserNotificationAsync(notification.Id);
                Logger.Instance.LogMessage($"{notification.Id} is now DELETED");
            }
        }

        // Raise a new toast with UserNotification.Id as tag
        private void ShowToastNotification(Windows.UI.Notifications.ToastNotification toast)
        {
            var toastNotifier = Windows.UI.Notifications.ToastNotificationManager.CreateToastNotifier();
            toast.Activated += async (s, e) => await ActivateAsync(s.Tag, false);
            toastNotifier.Show(toast);
        }

        // Remove a toast with UserNotification.Id as tag
        private void RemoveToastNotification(string notificationId)
        {
            Windows.UI.Notifications.ToastNotificationManager.History.Remove(notificationId);
        }

        public static Windows.UI.Notifications.ToastNotification BuildToastNotification(string notificationId, string notificationContent)
        {
            XmlDocument toastXml = Windows.UI.Notifications.ToastNotificationManager.GetTemplateContent(Windows.UI.Notifications.ToastTemplateType.ToastText02);
            XmlNodeList toastNodeList = toastXml.GetElementsByTagName("text");
            toastNodeList.Item(0).AppendChild(toastXml.CreateTextNode(notificationId));
            toastNodeList.Item(1).AppendChild(toastXml.CreateTextNode(notificationContent));
            IXmlNode toastNode = toastXml.SelectSingleNode("/toast");
            ((XmlElement)toastNode).SetAttribute("launch", "{\"type\":\"toast\",\"notificationId\":\"" + notificationId + "\"}");
            XmlElement audio = toastXml.CreateElement("audio");
            audio.SetAttribute("src", "ms-winsoundevent:Notification.SMS");
            return new Windows.UI.Notifications.ToastNotification(toastXml)
            {
                Tag = notificationId
            };
        }

        public void Reset()
        {
            Logger.Instance.LogMessage("Resetting the feed");
            m_feed = null;
            m_newNotifications.Clear();
            m_historicalNotifications.Clear();

            CacheUpdated?.Invoke(this, new EventArgs());
        }
    }

    [DataContract]
    public class Account
    {
        public AccountRegistrationState RegistrationState { get; set; }

        [DataMember]
        public String Token { get; set; }

        [DataMember]
        public String Id { get; set; }

        [DataMember]
        public ConnectedDevicesAccountType Type { get; set; }
        private ConnectedDevicesPlatform m_platform;
        public UserNotificationsManager UserNotifications { get; set; }

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
            await m_platform.NotificationRegistrationManager.RegisterForAccountAsync(account, registration);

            await UserNotifications.RegisterAccountWithSdkAsync();
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
