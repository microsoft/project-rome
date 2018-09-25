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
using Microsoft.ConnectedDevices.UserData;
using Microsoft.ConnectedDevices.UserNotifications;
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Threading.Tasks;
using Windows.Data.Xml.Dom;
using Windows.Foundation;
using Windows.Storage;

namespace SDKTemplate
{
    public class GraphNotificationProvider : IConnectedDevicesNotificationProvider
    {
        private ConnectedDevicesPlatform m_platform;
        private UserDataFeed m_feed;
        private UserNotificationReader m_reader;
        private UserNotificationChannel m_channel;
        private List<UserNotification> m_newNotifications = new List<UserNotification>();
        private List<UserNotification> m_historicalNotifications = new List<UserNotification>();
        private MicrosoftAccountProvider m_accoutProvider;
        private string m_pushUri;

        static readonly string PushUriKey = "PushUri";

        public event EventHandler CacheUpdated;

        public bool NewNotifications
        {
            get
            {
                return m_newNotifications.Count > 0;
            }
        }

        public IReadOnlyList<UserNotification> HistoricalNotifications
        {
            get
            {
                return m_historicalNotifications.AsReadOnly();
            }
        }

        public GraphNotificationProvider(MicrosoftAccountProvider accountProvider, string pushUri)
        {
            m_pushUri = pushUri;
            if (string.IsNullOrEmpty(pushUri) && ApplicationData.Current.LocalSettings.Values.ContainsKey(PushUriKey))
            {
                m_pushUri = ApplicationData.Current.LocalSettings.Values[PushUriKey] as string;
            }
            m_accoutProvider = accountProvider;
            accountProvider.SignOutCompleted += (s, e) => Reset();
        }

        private async Task<string> GetNotificationRegistration()
        {
            return m_pushUri;
        }

        IAsyncOperation<string> IConnectedDevicesNotificationProvider.GetNotificationRegistrationAsync()
        {
            Logger.Instance.LogMessage($"Push registration requested by platform");
            return GetNotificationRegistration().AsAsyncOperation();
        }

        event TypedEventHandler<IConnectedDevicesNotificationProvider, ConnectedDevicesNotificationRegistrationUpdatedEventArgs> IConnectedDevicesNotificationProvider.RegistrationUpdated
        {
            add { return new EventRegistrationToken(); }
            remove { }
        }

        public async void Refresh()
        {
            await SetupChannel();
            if (m_reader != null)
            {
                Logger.Instance.LogMessage("Read cached notifications");
                ReadNotifications(m_reader);
            }

            Logger.Instance.LogMessage("Request another sync");
            m_feed?.StartSync();
        }

        public async void ReceiveNotification(string content)
        {
            await SetupChannel();
            m_platform.ReceiveNotification(content);
        }

        public async void Activate(string id, bool dismiss)
        {
            await SetupChannel();
            var notification = m_historicalNotifications.Find((n) => { return (n.Id == id); });
            if (notification != null)
            {
                notification.UserActionState = dismiss ? UserNotificationUserActionState.Dismissed : UserNotificationUserActionState.Activated;
                await notification.SaveAsync();
                RemoveToastNotification(notification.Id);
                Logger.Instance.LogMessage($"{notification.Id} is now DISMISSED");
            }
        }

        public async void MarkRead(string id)
        {
            var notification = m_historicalNotifications.Find((n) => { return (n.Id == id); });
            if (notification != null)
            {
                notification.ReadState = UserNotificationReadState.Read;
                await notification.SaveAsync();
                Logger.Instance.LogMessage($"{notification.Id} is now READ");
            }
        }

        public async void Delete(string id)
        {
            var notification = m_historicalNotifications.Find((n) => { return (n.Id == id); });
            if (notification != null)
            {
                await m_channel?.DeleteUserNotificationAsync(notification.Id);
                Logger.Instance.LogMessage($"{notification.Id} is now DELETED");
            }
        }

        public async void Reset()
        {
            if (m_platform != null)
            {
                Logger.Instance.LogMessage("Shutting down platform");
                await m_platform.ShutdownAsync();
                m_platform = null;
                m_feed = null;
                m_newNotifications.Clear();
                m_historicalNotifications.Clear();
            }

            CacheUpdated?.Invoke(this, new EventArgs());
        }

        private async Task SetupChannel()
        {
            var account = m_accoutProvider.SignedInAccount;
            if (account != null && m_platform == null)
            {
                m_platform = new ConnectedDevicesPlatform(m_accoutProvider, this);
            }

            if (m_feed == null)
            {
                // Need to run UserDataFeed creation on a background thread
                // because MSA/AAD token request might need to show UI.
                await Task.Run(() =>
                {
                    lock (this)
                    {
                        if (account != null && m_feed == null)
                        {
                            try
                            {
                                m_feed = new UserDataFeed(account, m_platform, "graphnotifications.sample.windows.com");
                                m_feed.SyncStatusChanged += Feed_SyncStatusChanged;
                                m_feed.AddSyncScopes(new List<IUserDataFeedSyncScope>
                                {
                                    UserNotificationChannel.SyncScope
                                });

                                m_channel = new UserNotificationChannel(m_feed);
                                m_reader = m_channel.CreateReader();
                                m_reader.DataChanged += Reader_DataChanged;

                                Logger.Instance.LogMessage($"Setup feed for {account.Id} {account.Type}");
                            }
                            catch (Exception ex)
                            {
                                Logger.Instance.LogMessage($"Failed to setup UserNotificationChannel {ex.Message}");
                                m_feed = null;
                            }
                        }
                    }
                });
            }
        }

        private async void ReadNotifications(UserNotificationReader reader)
        {
            var notifications = await reader.ReadBatchAsync(UInt32.MaxValue);
            Logger.Instance.LogMessage($"Read {notifications.Count} notifications");

            foreach (var notification in notifications)
            {
                //Logger.Instance.LogMessage($"UserNotification: {notification.Id} Status: {notification.Status} ReadState: {notification.ReadState} UserActionState: {notification.UserActionState}");

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

        private void Feed_SyncStatusChanged(UserDataFeed sender, object args)
        {
            Logger.Instance.LogMessage($"SyncStatus is {sender.SyncStatus.ToString()}");
        }

        private void Reader_DataChanged(UserNotificationReader sender, object args)
        {
            Logger.Instance.LogMessage("New notification available");
            ReadNotifications(sender);
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

        // Raise a new toast with UserNotification.Id as tag
        private void ShowToastNotification(Windows.UI.Notifications.ToastNotification toast)
        {
            var toastNotifier = Windows.UI.Notifications.ToastNotificationManager.CreateToastNotifier();
            toast.Activated += (s, e) => Activate(s.Tag, false);
            toastNotifier.Show(toast);
        }

        // Remove a toast with UserNotification.Id as tag
        private void RemoveToastNotification(string notificationId)
        {
            Windows.UI.Notifications.ToastNotificationManager.History.Remove(notificationId);
        }
    }
}
