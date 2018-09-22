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

using Microsoft.ConnectedDevices.UserNotifications;
using System;
using System.Collections.ObjectModel;
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
        private ObservableCollection<NotificationListItem> activeNotifications = new ObservableCollection<NotificationListItem>();
        private GraphNotificationProvider notificationCache;

        public NotificationsPage()
        {
            InitializeComponent();

            UnreadView.ItemsSource = activeNotifications;
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            rootPage = MainPage.Current;
            var accountProvider = ((App)Application.Current).AccountProvider;
            RefreshButton.IsEnabled = (accountProvider.SignedInAccount != null);
            if (accountProvider.SignedInAccount != null)
            {
                Description.Text = $"{accountProvider.SignedInAccount.Type} user ";
                if (accountProvider.AadUser != null)
                {
                    Description.Text += accountProvider.AadUser.DisplayableId;
                }

                notificationCache = ((App)Application.Current).NotificationProvider;
                notificationCache.CacheUpdated += Cache_CacheUpdated;
                notificationCache.Refresh();
            }
        }

        protected override void OnNavigatedFrom(NavigationEventArgs e)
        {
            if (notificationCache != null)
            {
                notificationCache.CacheUpdated -= Cache_CacheUpdated;
            }
        }

        private async void Cache_CacheUpdated(object sender, EventArgs e)
        {
            await Dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                activeNotifications.Clear();
                foreach (UserNotification notification in notificationCache.HistoricalNotifications)
                {
                    activeNotifications.Add(new NotificationListItem()
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

                if (notificationCache.NewNotifications)
                {
                    rootPage.NotifyUser("History is up-to-date. New notifications available", NotifyType.StatusMessage);
                }
                else
                {
                    rootPage.NotifyUser("History is up-to-date", NotifyType.StatusMessage);
                }
            });
        }

        private void Button_Refresh(object sender, RoutedEventArgs e)
        {
            rootPage.NotifyUser("Updating history", NotifyType.StatusMessage);
            notificationCache.Refresh();
        }

        private void Button_MarkRead(object sender, RoutedEventArgs e)
        {
            var item = ((Grid)((Border)((Button)sender).Parent).Parent).DataContext as NotificationListItem;
            notificationCache.MarkRead(item.Id);
        }

        private void Button_Delete(object sender, RoutedEventArgs e)
        {
            var item = ((Grid)((Border)((Button)sender).Parent).Parent).DataContext as NotificationListItem;
            notificationCache.Delete(item.Id);
        }
    }
}