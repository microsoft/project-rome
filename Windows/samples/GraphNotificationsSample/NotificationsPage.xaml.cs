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

using Microsoft.ConnectedDevices.UserData.UserNotifications;
using System;
using System.Collections.ObjectModel;
using System.Threading.Tasks;
using Windows.UI;
using Windows.UI.Core;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;

namespace SDKTemplate
{
    class NotificationListItem
    {
        public string Id { get; set; }
        public string Content { get; set; }
        public bool UnreadState { get; set; }
        public string UserActionState { get; set; }
        public string Priority { get; set; }
        public string ExpirationTime { get; set; }
        public string ChangeTime { get; set; }
    }

    public class BoolColorConverter : IValueConverter
    {
        object IValueConverter.Convert(object value, Type targetType, object parameter, string language)
        {
            return new SolidColorBrush(((bool)value) ? Colors.Green : Colors.Red);
        }

        object IValueConverter.ConvertBack(object value, Type targetType, object parameter, string language)
        {
            throw new NotImplementedException();
        }
    }

    public partial class NotificationsPage : Page
    {
        private MainPage rootPage;
        private ObservableCollection<NotificationListItem> m_activeNotifications = new ObservableCollection<NotificationListItem>();
        private Account m_account;
        private UserNotificationsManager m_userNotificationManager;

        public NotificationsPage()
        {
            InitializeComponent();
            UnreadView.ItemsSource = m_activeNotifications;
        }

        protected override async void OnNavigatedTo(NavigationEventArgs e)
        {
            rootPage = MainPage.Current;
            await RefreshAsync();
        }

        private async Task RefreshAsync()
        {
            m_account = null;
            m_userNotificationManager = null;
            m_activeNotifications.Clear();

            // The ConnectedDevices SDK does not support multi-user currently. When this support becomes available 
            // the user would be sent via NavigationEventArgs. For now, just grab the first one if it exists.
            var connectedDevicesManager = ((App)Application.Current).ConnectedDevicesManager;
            if ((connectedDevicesManager.Accounts.Count > 0))
            {
                m_account = connectedDevicesManager.Accounts[0];
                if (m_account.UserNotifications != null)
                {
                    m_userNotificationManager = connectedDevicesManager.Accounts[0].UserNotifications;
                }
            }

            RefreshButton.IsEnabled = (m_userNotificationManager != null);
            if (m_account != null)
            {
                Description.Text = $"{m_account.Type} user ";
            }

            if (m_userNotificationManager != null)
            {
                if (m_userNotificationManager.CurrentSubscription != null)
                {
                    TextBox_SubscriptionId.Text = m_userNotificationManager.CurrentSubscription.UserNotificationSubscriptionId;
                }
                m_userNotificationManager.CacheUpdated += Cache_CacheUpdated;
                await m_userNotificationManager.RefreshAsync();
            }
        }

        protected override void OnNavigatedFrom(NavigationEventArgs e)
        {
            if (m_userNotificationManager != null)
            {
                m_userNotificationManager.CacheUpdated -= Cache_CacheUpdated;
            }
        }

        private async void Cache_CacheUpdated(object sender, EventArgs e)
        {
            await Dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                RefreshNotifications();
            });
        }

        private void RefreshNotifications()
        {
            m_activeNotifications.Clear();

            foreach (UserNotification notification in m_userNotificationManager.HistoricalNotifications)
            {
                m_activeNotifications.Add(new NotificationListItem()
                {
                    Id = notification.Id,
                    Content = $"  Content:{notification.Content}",
                    UnreadState = notification.ReadState == UserNotificationReadState.Unread,
                    UserActionState = notification.UserActionState.ToString(),
                    Priority = $"  Priority: {notification.Priority.ToString()}",
                    ExpirationTime = $"  Expiry: {notification.ExpirationTime.ToLocalTime().ToString()}",
                    ChangeTime = $"  Last Updated: {notification.ChangeTime.ToLocalTime().ToString()}",
                });
            }

            if (m_userNotificationManager.NewNotifications)
            {
                rootPage.NotifyUser("History is up-to-date. New notifications available", NotifyType.StatusMessage);
            }
            else
            {
                rootPage.NotifyUser("History is up-to-date", NotifyType.StatusMessage);
            }
        }

        private async void Button_Refresh(object sender, RoutedEventArgs e)
        {
            rootPage.NotifyUser("Updating history", NotifyType.StatusMessage);
            await m_userNotificationManager.RefreshAsync();
        }

        private async void Button_MarkRead(object sender, RoutedEventArgs e)
        {
            var item = ((Grid)((Border)((Button)sender).Parent).Parent).DataContext as NotificationListItem;
            await m_userNotificationManager.MarkReadAsync(item.Id);
        }

        private async void Button_Delete(object sender, RoutedEventArgs e)
        {
            var item = ((Grid)((Border)((Button)sender).Parent).Parent).DataContext as NotificationListItem;
            await m_userNotificationManager.DeleteAsync(item.Id);
        }
    }
}