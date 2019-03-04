package com.microsoft.connecteddevices.graphnotifications;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.microsoft.connecteddevices.AsyncOperation;
import com.microsoft.connecteddevices.ConnectedDevicesAccount;
import com.microsoft.connecteddevices.ConnectedDevicesPlatform;
import com.microsoft.connecteddevices.userdata.UserDataFeed;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotification;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationChannel;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationReadState;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationReader;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationStatus;
import com.microsoft.connecteddevices.userdata.usernotifications.UserNotificationUserActionState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;

public class UserNotificationsManager {
    private static final String TAG = UserNotificationsManager.class.getName();

    public static final String CHANNEL_NAME = "GraphNotificationsChannel001";
    public static final String NOTIFICATION_ID = "ID";

    public interface NotificationsUpdatedEventListener {
        void onEvent(EventObject args);
    }

    private ArrayList<NotificationsUpdatedEventListener> mListeners = new ArrayList<>();

    private Context mContext;
    private UserDataFeed mFeed;
    private UserNotificationChannel mChannel;
    private UserNotificationReader mReader;

    private final ArrayList<UserNotification> mHistoricalNotifications = new ArrayList<>();
    private final ArrayList<UserNotification> mNewNotifications = new ArrayList<>();

    public UserNotificationsManager(@NonNull Context context, @NonNull ConnectedDevicesAccount account, @NonNull ConnectedDevicesPlatform platform)
    {
        mContext = context;
        mFeed = UserDataFeed.getForAccount(account, platform, Secrets.APP_HOST_NAME);
        mChannel = new UserNotificationChannel(mFeed);
        mReader = mChannel.createReader();
        mReader.dataChanged().subscribe((reader, aVoid) -> readFromCache(reader));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_NAME, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("GraphNotificationsSample Channel");
            ((NotificationManager)context.getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    public AsyncOperation<Boolean> registerForAccountAsync()
    {
        return mFeed.subscribeToSyncScopesAsync(Arrays.asList(UserNotificationChannel.getSyncScope())).thenApplyAsync((success) -> {
            mFeed.startSync();
            readFromCache(mReader);
            return success;
        });
    }

    public synchronized void addNotificationsUpdatedEventListener(NotificationsUpdatedEventListener listener) {
        mListeners.add(listener);
    }

    public synchronized void removeNotificationsUpdatedEventListener(NotificationsUpdatedEventListener listener) {
        mListeners.remove(listener);
    }

    public List<UserNotification> HistoricalNotifications() {
        return mHistoricalNotifications;
    }

    public boolean HasNewNotifications() {
        return !mNewNotifications.isEmpty();
    }

    public void refresh()
    {
        mFeed.startSync();
        readFromCache(mReader);
    }

    public void activate(UserNotification notification)
    {
        notification.setUserActionState(UserNotificationUserActionState.ACTIVATED);
        notification.saveAsync().whenCompleteAsync((userNotificationUpdateResult, throwable) -> {
            if (throwable == null && userNotificationUpdateResult != null && userNotificationUpdateResult.getSucceeded()) {
                Log.d(TAG, "Successfully activated the notification");
            }
        });
        clearNotification(mContext.getApplicationContext(), notification.getId());
    }

    public void dismiss(UserNotification notification)
    {
        notification.setUserActionState(UserNotificationUserActionState.DISMISSED);
        notification.saveAsync().whenCompleteAsync((userNotificationUpdateResult, throwable) -> {
            if (throwable == null && userNotificationUpdateResult != null && userNotificationUpdateResult.getSucceeded()) {
                Log.d(TAG, "Successfully dismissed the notification");
            }
        });
        clearNotification(mContext.getApplicationContext(), notification.getId());
    }

    public void markRead(UserNotification notification)
    {
        notification.setReadState(UserNotificationReadState.READ);
        notification.saveAsync().whenCompleteAsync((userNotificationUpdateResult, throwable) -> {
            if (throwable == null && userNotificationUpdateResult != null && userNotificationUpdateResult.getSucceeded()) {
                Log.d(TAG, "Successfully marked the notification as read");
            }
        });
    }

    public void delete(UserNotification notification)
    {
        mChannel.deleteUserNotificationAsync(notification.getId()).whenCompleteAsync((userNotificationUpdateResult, throwable) -> {
            if (throwable == null && userNotificationUpdateResult != null && userNotificationUpdateResult.getSucceeded()) {
                Log.d(TAG, "Successfully deleted the notification");
            }
        });
    }

    private void NotifyNotificationsUpdated() {
        Log.d(TAG, "Notifying listeners");
        List<NotificationsUpdatedEventListener> listeners = new ArrayList<>();
        synchronized (this) {
            listeners.addAll(mListeners);
        }
        for (NotificationsUpdatedEventListener listener : listeners) {
            listener.onEvent(new EventObject(this));
        }
    }

    /**
     * Replacement for the java.util.function.Predicate to support pre Java 8 / API 24.
     */
    interface Predicate<T> {
        public boolean test(T t);
    }

    /**
     * Replacement for list.removeIf to support pre Java 8 / API 24.
     * @param list List to search
     * @param predicate Predicate to use against the given list
     * @return True if removed item matching the given predicate, false if none found
     */
    private static <T> boolean removeIf(List<T> list, Predicate<? super T> predicate) {
        for (T item : list) {
            if (predicate.test(item)) {
                list.remove(item);
                return true;
            }
        }

        return false;
    }

    private void readFromCache(final UserNotificationReader reader)
    {
        Log.d(TAG, "Read notifications from cache");
        reader.readBatchAsync(Long.MAX_VALUE).thenAccept(notifications -> {
            synchronized (this) {
                for (final UserNotification notification : notifications) {
                    if (notification.getStatus() == UserNotificationStatus.ACTIVE) {
                        removeIf(mNewNotifications, item -> notification.getId().equals(item.getId()));

                        if (notification.getUserActionState() == UserNotificationUserActionState.NO_INTERACTION) {
                            mNewNotifications.add(notification);
                            if (notification.getReadState() != UserNotificationReadState.READ) {
                                clearNotification(mContext.getApplicationContext(), notification.getId());
                                addNotification(mContext.getApplicationContext(), notification.getContent(), notification.getId());
                            }
                        } else {
                            clearNotification(mContext.getApplicationContext(), notification.getId());
                        }

                        removeIf(mHistoricalNotifications, item -> notification.getId().equals(item.getId()));
                        mHistoricalNotifications.add(0, notification);
                    } else {
                        removeIf(mNewNotifications, item -> notification.getId().equals(item.getId()));
                        removeIf(mHistoricalNotifications, item -> notification.getId().equals(item.getId()));
                        clearNotification(mContext.getApplicationContext(), notification.getId());
                    }
                }
            }

            NotifyNotificationsUpdated();
        });
    }

    static void addNotification(Context ctx, String message, String notificationId) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.putExtra(NOTIFICATION_ID, notificationId);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_NAME)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("New UserNotification!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(ctx).notify(notificationId.hashCode(), builder.build());
    }

    static void clearNotification(Context ctx, String notificationId) {
        ((NotificationManager)ctx.getSystemService(NOTIFICATION_SERVICE)).cancel(notificationId.hashCode());
    }
}
