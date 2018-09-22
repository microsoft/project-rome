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
        private MicrosoftAccountProvider accountProvider;

        public AccountsPage()
        {
            this.InitializeComponent();
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            rootPage = MainPage.Current;
            accountProvider = ((App)Application.Current).AccountProvider;
            UpdateUI();
        }

        private void UpdateUI()
        {
            MsaButton.IsEnabled = (accountProvider.SignedInAccount == null);
            AadButton.IsEnabled = (accountProvider.SignedInAccount == null);
            LogoutButton.IsEnabled = (accountProvider.SignedInAccount != null);
            LogoutButton.Content = "Logout";

            if (accountProvider.SignedInAccount != null)
            {
                Description.Text = $"{accountProvider.SignedInAccount.Type} user ";
                if (accountProvider.AadUser != null)
                {
                    Description.Text += accountProvider.AadUser.DisplayableId;
                }

                LogoutButton.Content = $"Logout - {accountProvider.SignedInAccount.Type}";
            }
        }

        private async void Button_LoginMSA(object sender, RoutedEventArgs e)
        {
            if (accountProvider.SignedInAccount == null)
            {
                ((Button)sender).IsEnabled = false;

                bool success = await accountProvider.SignInMsa();
                if (!success)
                {
                    rootPage.NotifyUser("MSA login failed!", NotifyType.ErrorMessage);
                }
                else
                {
                    rootPage.NotifyUser("MSA login successful", NotifyType.StatusMessage);
                }

                UpdateUI();
            }
        }

        private async void Button_LoginAAD(object sender, RoutedEventArgs e)
        {
            if (accountProvider.SignedInAccount == null)
            {
                ((Button)sender).IsEnabled = false;

                bool success = await accountProvider.SignInAad();
                if (!success)
                {
                    rootPage.NotifyUser("AAD login failed!", NotifyType.ErrorMessage);
                }
                else
                {
                    rootPage.NotifyUser("AAD login successful", NotifyType.StatusMessage);
                }

                UpdateUI();
            }
        }

        private async void Button_Logout(object sender, RoutedEventArgs e)
        {
            ((Button)sender).IsEnabled = false;

            accountProvider.SignOut();

            rootPage.NotifyUser("Logout successful", NotifyType.ErrorMessage);

            UpdateUI();
        }
    }
}
