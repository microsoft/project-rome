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
    public sealed partial class AccountsPage : Page
    {
        private MainPage rootPage;
        private ConnectedDevicesManager connectedDevicesManager;

        public AccountsPage()
        {
            this.InitializeComponent();
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            rootPage = MainPage.Current;
            connectedDevicesManager = ((App)Application.Current).ConnectedDevicesManager;
            connectedDevicesManager.AccountsChanged += ConnectedDevicesManager_AccountsChanged;
            UpdateUI();
        }

        private void ConnectedDevicesManager_AccountsChanged(object sender, System.EventArgs e)
        {
            UpdateUI();
        }

        private void UpdateUI()
        {
            // The ConnectedDevices SDK does not support multi-user currently. When this support becomes available 
            // these buttons would always be enabled.
            bool hasAccount = connectedDevicesManager.Accounts.Count > 0;
            MsaButton.IsEnabled = !hasAccount;
            AadButton.IsEnabled = !hasAccount;
        }

        private async void Button_LoginMSA(object sender, RoutedEventArgs e)
        {
            bool success = await connectedDevicesManager.SignInMsaAsync();
            if (!success)
            {
                rootPage.NotifyUser("MSA login failed!", NotifyType.ErrorMessage);
            }
            else
            {
                rootPage.NotifyUser("MSA login successful", NotifyType.StatusMessage);
            }
        }

        private async void Button_LoginAAD(object sender, RoutedEventArgs e)
        {
            ((Button)sender).IsEnabled = false;

            bool success = await connectedDevicesManager.SignInAadAsync();
            if (!success)
            {
                rootPage.NotifyUser("AAD login failed!", NotifyType.ErrorMessage);
            }
            else
            {
                rootPage.NotifyUser("AAD login successful", NotifyType.StatusMessage);
            }
        }
    }
}
