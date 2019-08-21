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
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Windows.Data.Xml.Dom;

namespace SDKTemplate
{
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

        public UserDataFeedSubscription CurrentSubscription { get; private set; } = null;

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
            var scopes = new List<UserDataFeedSyncScope> { UserNotificationChannel.SyncScope };
            UserDataFeedSubscribeResult result = await m_feed.SubscribeToSyncScopesWithResultAsync(scopes);
            if (result.Status != UserDataFeedSubscribeStatus.Success)
            {
                throw new Exception($"GraphNotificationsSample failed to subscribe for notifications, status: {result.Status}");
            }
            else
            {
                // Save the last good subscription
                CurrentSubscription = result.Subscription;
                Logger.Instance.LogMessage($"GraphNotificationsSample subscribed with {result.Subscription.UserNotificationSubscriptionId} valid till {result.Subscription.ExpirationTime}");

                // This App should send "subscription.UserNotificationSubscriptionId" to its appservice.
                // Appservice can use UserNotificationSubscriptionId to POST new notification
                // to https://graph.microsoft.com/beta/me/notifications without OaAuth tokens.
            }
        }

        private void Feed_SyncStatusChanged(UserDataFeed sender, object args)
        {
            Logger.Instance.LogMessage($"SyncStatus is {sender.SyncStatus}");
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
}
