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

using System;
using Windows.UI.Core;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Navigation;

namespace SDKTemplate
{
    public sealed partial class LogsPage : Page
    {
        private MainPage rootPage;

        public LogsPage()
        {
            this.InitializeComponent();
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            rootPage = MainPage.Current;

            var accountProvider = ((App)Application.Current).AccountProvider;
            if (accountProvider.SignedInAccount != null)
            {
                Description.Text = $"{accountProvider.SignedInAccount.Type} user ";
                if (accountProvider.AadUser != null)
                {
                    Description.Text += accountProvider.AadUser.DisplayableId;
                }
            }

            LogView.Text = Logger.Instance.AppLogs;
            Logger.Instance.LogUpdated += LogsUpdated;
        }

        protected override void OnNavigatedFrom(NavigationEventArgs e)
        {
            Logger.Instance.LogUpdated -= LogsUpdated;
        }

        private async void LogsUpdated(object sender, string message)
        {
            await Dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                LogView.Text = message + Environment.NewLine + LogView.Text;
            });
        }

        private void Button_Clear(object sender, RoutedEventArgs e)
        {
            LogView.Text = string.Empty;
            Logger.Instance.AppLogs = string.Empty;
            rootPage.NotifyUser("Logs cleared", NotifyType.ErrorMessage);
        }
    }
}
