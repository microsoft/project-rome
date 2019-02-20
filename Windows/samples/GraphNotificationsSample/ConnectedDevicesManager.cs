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
using System.IO;
using System.Linq;
using System.Runtime.Serialization.Json;
using System.Text;
using System.Threading.Tasks;
using Windows.Storage;

namespace SDKTemplate
{
    public class ConnectedDevicesManager
    {
        private ConnectedDevicesPlatform m_platform;

        private List<Account> m_accounts = new List<Account>();
        public IReadOnlyList<Account> Accounts
        {
            get
            {
                return m_accounts.Where((x) => x.RegistrationState == AccountRegistrationState.InAppCacheAndSdkCache).ToList();
            }
        }
        public event EventHandler AccountsChanged;

        static readonly string AccountsKey = "Accounts";
        static readonly string CCSResource = "https://cdpcs.access.microsoft.com";

        // This is a singleton object which holds onto the app's ConnectedDevicesPlatform and 
        // handles account management. This is accessed via App.Current.ConnectedDevicesManager
        public ConnectedDevicesManager()
        {
            // Construct and initialize a platform. All we are doing here is hooking up event handlers before
            // calling ConnectedDevicesPlatform.Start(). After Start() is called events may begin to fire.
            m_platform = new ConnectedDevicesPlatform();
            m_platform.AccountManager.AccessTokenRequested += AccountManager_AccessTokenRequestedAsync;
            m_platform.AccountManager.AccessTokenInvalidated += AccountManager_AccessTokenInvalidated;
            m_platform.NotificationRegistrationManager.NotificationRegistrationStateChanged += NotificationRegistrationManager_NotificationRegistrationStateChanged;
            m_platform.Start();

            // Pull the accounts from our app's cache and synchronize the list with the apps cached by 
            // ConnectedDevicesPlatform.AccountManager.
            DeserializeAccounts();

            // Finally initialize the accounts. This will refresh registrations when needed, add missing accounts,
            // and remove stale accounts from the ConnectedDevicesPlatform.AccountManager.
            Task.Run(() => InitializeAccountsAsync());
        }

        private void DeserializeAccounts()
        {
            // Add all our cached accounts.
            var sdkCachedAccounts = m_platform.AccountManager.Accounts.ToList();
            var appCachedAccounts = ApplicationData.Current.LocalSettings.Values[AccountsKey] as string;
            if (!String.IsNullOrEmpty(appCachedAccounts))
            {
                DeserializeAppCachedAccounts(appCachedAccounts, sdkCachedAccounts);
            }

            // Add the remaining SDK only accounts (these need to be removed from the SDK)
            foreach (var sdkCachedAccount in sdkCachedAccounts)
            {
                m_accounts.Add(new Account(m_platform, sdkCachedAccount.Id, sdkCachedAccount.Type, null, AccountRegistrationState.InSdkCacheOnly));
            }
        }

        private void DeserializeAppCachedAccounts(String jsonCachedAccounts, List<ConnectedDevicesAccount> sdkCachedAccounts)
        {
            MemoryStream stream = new MemoryStream(Encoding.UTF8.GetBytes(jsonCachedAccounts));
            DataContractJsonSerializer serializer = new DataContractJsonSerializer(m_accounts.GetType());
            List <Account> appCachedAccounts = serializer.ReadObject(stream) as List<Account>;

            var authContext = new AuthenticationContext("https://login.microsoftonline.com/common");
            var adalCachedItems = authContext.TokenCache.ReadItems();
            foreach (var account in appCachedAccounts)
            {
                if (account.Type == ConnectedDevicesAccountType.AAD)
                {
                    // AAD accounts are also cached in ADAL, which is where the actual token logic lives.
                    // If the account isn't available in our ADAL cache then it's not usable. Ideally this
                    // shouldn't happen.
                    var adalCachedItem = adalCachedItems.FirstOrDefault((x) => x.UniqueId == account.Id);
                    if (adalCachedItem == null)
                    {
                        continue;
                    }
                }

                // Check if the account is also present in ConnectedDevicesPlatform.AccountManager.
                AccountRegistrationState registrationState;
                var sdkAccount = sdkCachedAccounts.Find((x) => account.EqualsTo(x));
                if (sdkAccount == null)
                {
                    // Account not found in the SDK cache. Later when Account.InitializeAsync runs this will 
                    // add the account to the SDK cache and perform registration.
                    registrationState = AccountRegistrationState.InAppCacheOnly;
                }
                else
                {
                    // Account found in the SDK cache, remove it from the list of sdkCachedAccounts. After 
                    // all the appCachedAccounts have been processed any accounts remaining in sdkCachedAccounts
                    // are only in the SDK cache, and should be removed.
                    registrationState = AccountRegistrationState.InAppCacheAndSdkCache;
                    sdkCachedAccounts.RemoveAll((x) => account.EqualsTo(x));
                }

                m_accounts.Add(new Account(m_platform, account.Id, account.Type, account.Token, registrationState));
            }
        }

        private async Task InitializeAccountsAsync()
        {
            foreach (var account in m_accounts)
            {
                await account.InitializeAccountAsync();
            }

            // All accounts which can be in a good state should be. Remove any accounts which aren't
            m_accounts.RemoveAll((x) => x.RegistrationState != AccountRegistrationState.InAppCacheAndSdkCache);
            AccountListChanged();
        }

        private void AccountListChanged()
        {
            AccountsChanged.Invoke(this, new EventArgs());
            SerializeAccountsToCache();
        }

        private void SerializeAccountsToCache()
        {
            using (MemoryStream stream = new MemoryStream())
            {
                DataContractJsonSerializer serializer = new DataContractJsonSerializer(m_accounts.GetType());
                serializer.WriteObject(stream, m_accounts);

                byte[] json = stream.ToArray();
                ApplicationData.Current.LocalSettings.Values[AccountsKey] = Encoding.UTF8.GetString(json, 0, json.Length);
            }
        }

        private void AccountManager_AccessTokenInvalidated(ConnectedDevicesAccountManager sender, ConnectedDevicesAccessTokenInvalidatedEventArgs args)
        {
            Logger.Instance.LogMessage($"Token Invalidated. AccountId: {args.Account.Id}, AccountType: {args.Account.Id}, scopes: {string.Join(" ", args.Scopes)}");
        }

        private async void NotificationRegistrationManager_NotificationRegistrationStateChanged(ConnectedDevicesNotificationRegistrationManager sender, ConnectedDevicesNotificationRegistrationStateChangedEventArgs args)
        {
            if ((args.State == ConnectedDevicesNotificationRegistrationState.Expired) || (args.State == ConnectedDevicesNotificationRegistrationState.Expiring))
            {
                var account = m_accounts.Find((x) => x.EqualsTo(args.Account));
                if (account != null)
                {
                    await account.RegisterAccountWithSdkAsync();
                }
            }
        }

        private async void AccountManager_AccessTokenRequestedAsync(ConnectedDevicesAccountManager sender, ConnectedDevicesAccessTokenRequestedEventArgs args)
        {
            Logger.Instance.LogMessage($"Token requested by platform for {args.Request.Account.Id} and {string.Join(" ", args.Request.Scopes)}");

            var account = m_accounts.Find((x) => x.EqualsTo(args.Request.Account));
            if (account != null)
            {
                try
                {
                    var accessToken = await account.GetAccessTokenAsync(args.Request.Scopes);
                    Logger.Instance.LogMessage($"Token : {accessToken}");
                    args.Request.CompleteWithAccessToken(accessToken);
                }
                catch (Exception ex)
                {
                    Logger.Instance.LogMessage($"Token request failed: {ex.Message}");
                    args.Request.CompleteWithErrorMessage(ex.Message);
                }
            }
        }

        public async Task<bool> SignInAadAsync()
        {
            try
            {
                var authResult = await Account.GetAadTokenAsync(CCSResource);
                var account = new Account(m_platform, authResult.UserInfo.UniqueId, 
                    ConnectedDevicesAccountType.AAD, authResult.AccessToken, AccountRegistrationState.InAppCacheOnly);
                m_accounts.Add(account);
                await account.InitializeAccountAsync();

                AccountListChanged();
                return true;
            }
            catch
            {
                return false;
            }
        }

        public async Task<bool> SignInMsaAsync()
        {
            string refreshToken = await MSAOAuthHelpers.GetRefreshTokenAsync();
            if (!string.IsNullOrEmpty(refreshToken))
            {
                var account = new Account(m_platform, Guid.NewGuid().ToString(), 
                    ConnectedDevicesAccountType.MSA, refreshToken, AccountRegistrationState.InAppCacheOnly);
                m_accounts.Add(account);
                await account.InitializeAccountAsync();

                AccountListChanged();
                return true;
            }

            return false;
        }

        public async Task LogoutAsync(Account account)
        {
            // First log the account out from the ConnectedDevices SDK. The SDK may call back for access tokens to perform
            // unregistration with services
            await account.LogoutAsync();

            // Next remove the account locally
            m_accounts.RemoveAll((x) => x.Id == account.Id);
            if (account.Type == ConnectedDevicesAccountType.AAD)
            {
                var authContext = new AuthenticationContext("https://login.microsoftonline.com/common");
                var cacheItems = authContext.TokenCache.ReadItems();
                var cacheItem = cacheItems.FirstOrDefault((x) => x.UniqueId == account.Id);
                if (cacheItem != null)
                {
                    authContext.TokenCache.DeleteItem(cacheItem);
                }
            }

            AccountListChanged();
        }

        public async Task ReceiveNotificationAsync(string content)
        {
            ConnectedDevicesNotification notification = ConnectedDevicesNotification.TryParse(content);
            if (notification != null)
            {
                await m_platform.ProcessNotificationAsync(notification);
            }
        }

        public async Task RefreshAsync()
        {
            foreach (var account in m_accounts)
            {
                await account.UserNotifications?.RefreshAsync();
            }
        }

        public async Task ActivateAsync(string id, bool dismiss)
        {
            foreach (var account in m_accounts)
            {
                await account.UserNotifications?.ActivateAsync(id, dismiss);
            }
        }
    }
}
