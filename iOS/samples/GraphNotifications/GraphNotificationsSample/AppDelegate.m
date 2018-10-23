
#import "AppDelegate.h"
#import "NotificationProvider.h"


void uncaughtExceptionHandler(NSException* uncaughtException)
{
    NSLog(@"Uncaught exception: %@", uncaughtException.description);
}

@interface AppDelegate ()

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    NSSetUncaughtExceptionHandler(&uncaughtExceptionHandler);
    
    // Set up Notifications
    NSDictionary* userInfo = launchOptions[UIApplicationLaunchOptionsRemoteNotificationKey];
    
    if (!userInfo)
    {
        // User launch app by tapping the App icon normal launch
        [application
         registerUserNotificationSettings:[UIUserNotificationSettings settingsForTypes:UIUserNotificationTypeAlert categories:nil]];
    }
    else
    {
        @try
        {
            // app run in background and received the push notification, app is launched by user tapping the alert view
            [MCDNotificationReceiver receiveNotification:userInfo];
        }
        @catch(NSException* exception) {
            NSLog(@"Failed start up notification with exception %@", exception);
        }
    }
    return YES;
}


- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
}


- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}


- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
}


- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}


- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

- (void)application:(UIApplication*)application
didRegisterUserNotificationSettings:(__unused UIUserNotificationSettings*)notificationSettings
{
    // actually registerForRemoteNotifications after registerUserNotificationSettings is finished
    [application registerForRemoteNotifications];
}

- (void)application:(__unused UIApplication*)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken
{
    // when registerForRemoteNotifications, retrieve the deviceToken, convert it to HEX encoded NSString
    NSMutableString* deviceTokenStr = [NSMutableString stringWithCapacity:deviceToken.length * 2];
    const unsigned char* byteBuffer = deviceToken.bytes;
    for (NSUInteger i = 0; i < deviceToken.length; ++i)
    {
        [deviceTokenStr appendFormat:@"%02X", (unsigned int)byteBuffer[i]];
    }
    NSLog(@"APNs token: %@", deviceTokenStr);
    
    @try
    {
        // invoke notificationProvider with new notification registration
        [NotificationProvider
         updateNotificationRegistration:[[MCDNotificationRegistration alloc]
                                         initWithNotificationType:MCDNotificationTypeAPN
                                         token:deviceTokenStr
                                         appId:[[NSBundle mainBundle] bundleIdentifier]
                                         appDisplayName:(NSString*)[[NSBundle mainBundle]
                                                                    objectForInfoDictionaryKey:@"CFBundleDisplayName"]]];
    }
    @catch (NSException* exception) {
        NSLog(@"Failed to update notification registration with exception %@", exception);
    }
}

- (void)application:(__unused UIApplication*)application didReceiveRemoteNotification:(nonnull NSDictionary*)userInfo
{
    // app run in foreground and received the push notification, pump notification into CDPPlatform
    NSLog(@"Received remote notification...");
    [userInfo enumerateKeysAndObjectsUsingBlock:^(
                                                  id _Nonnull key, id _Nonnull obj, __unused BOOL* _Nonnull stop) { NSLog(@"%@: %@", key, obj); }];
    @try
    {
        if (![MCDNotificationReceiver receiveNotification:userInfo])
        {
            NSLog(@"Received notification was not for Rome");
        }
    }
    @catch(NSException* exception) {
        NSLog(@"Failed to receive notification with exception %@", exception);
    }
}


@end
