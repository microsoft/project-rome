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

using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Navigation;

namespace SDKTemplate
{
    enum LoginState
    {
        LOGIN_PROGRESS,
        LOGGED_IN_MSA,
        LOGGED_IN_AAD,
        LOGGED_OUT
    }

    public sealed partial class AccountsPage : Page
    {
        private MainPage m_rootPage;
        private ConnectedDevicesManager m_connectedDevicesManager;
        private LoginState m_state = LoginState.LOGGED_OUT;

        public AccountsPage()
        {
            this.InitializeComponent();
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            m_rootPage = MainPage.Current;
            m_connectedDevicesManager = ((App)Application.Current).ConnectedDevicesManager;
            m_connectedDevicesManager.AccountsChanged += ConnectedDevicesManager_AccountsChanged;

            UpdateView(GetCurrentLoginState());
        }

        private void ConnectedDevicesManager_AccountsChanged(object sender, System.EventArgs e)
        {
            UpdateView(GetCurrentLoginState());
        }

        private async void Button_LoginMSA(object sender, RoutedEventArgs e)
        {
            if (m_state == LoginState.LOGGED_OUT)
            {
                UpdateView(LoginState.LOGIN_PROGRESS);
                bool success = await m_connectedDevicesManager.SignInMsaAsync();
                if (!success)
                {
                    m_rootPage.NotifyUser("MSA login failed!", NotifyType.ErrorMessage);
                    UpdateView(LoginState.LOGGED_OUT);
                }
                else
                {
                    m_rootPage.NotifyUser("MSA login successful", NotifyType.StatusMessage);
                    UpdateView(LoginState.LOGGED_IN_MSA);
                }
            }
            else
            {
                LogoutCurrentAccount();
            }
        }

        private async void Button_LoginAAD(object sender, RoutedEventArgs e)
        {
            if (m_state == LoginState.LOGGED_OUT)
            {
                UpdateView(LoginState.LOGIN_PROGRESS);
                bool success = await m_connectedDevicesManager.SignInAadAsync();
                if (!success)
                {
                    m_rootPage.NotifyUser("AAD login failed!", NotifyType.ErrorMessage);
                    UpdateView(LoginState.LOGGED_OUT);
                }
                else
                {
                    m_rootPage.NotifyUser("AAD login successful", NotifyType.StatusMessage);
                    UpdateView(LoginState.LOGGED_IN_AAD);
                }
            }
            else
            {
                LogoutCurrentAccount();
            }
        }

        private async void LogoutCurrentAccount()
        {
            UpdateView(LoginState.LOGGED_OUT);
            var account = m_connectedDevicesManager.Accounts[0];
            await m_connectedDevicesManager.LogoutAsync(account);
            m_rootPage.NotifyUser("Logged out", NotifyType.ErrorMessage);
        }

        private LoginState GetCurrentLoginState()
        {
            LoginState currentState = LoginState.LOGGED_OUT;
            if (m_connectedDevicesManager.Accounts.Count > 0)
            {
                currentState = m_connectedDevicesManager.Accounts[0].Type == Microsoft.ConnectedDevices.ConnectedDevicesAccountType.AAD ? LoginState.LOGGED_IN_AAD : LoginState.LOGGED_IN_MSA;
            }
            return currentState;
        }

        private void UpdateView(LoginState state)
        {
            m_state = state;

            switch (state)
            {
                case LoginState.LOGGED_OUT:
                    AadButton.IsEnabled = true;
                    AadButton.Content = "Login with AAD";
                    MsaButton.IsEnabled = true;
                    MsaButton.Content = "Login with MSA";
                    break;

                case LoginState.LOGIN_PROGRESS:
                    AadButton.IsEnabled = false;
                    AadButton.Content = "Logging In";
                    MsaButton.IsEnabled = false;
                    MsaButton.Content = "Logging In";
                    break;

                case LoginState.LOGGED_IN_AAD:
                    AadButton.IsEnabled = true;
                    AadButton.Content = "Log Out";
                    MsaButton.IsEnabled = false;
                    MsaButton.Content = "Login with MSA";
                    break;

                case LoginState.LOGGED_IN_MSA:
                    MsaButton.IsEnabled = true;
                    MsaButton.Content = "Log Out";
                    AadButton.IsEnabled = false;
                    AadButton.Content = "Login with AAD";
                    break;
            }
        }

    }
}
