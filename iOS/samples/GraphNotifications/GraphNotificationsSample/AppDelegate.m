
#import "AppDelegate.h"
#import "NotificationsManager.h"

void uncaughtExceptionHandler(NSException* uncaughtException)
{
    NSLog(@"Uncaught exception: %@", uncaughtException.description);
}

@interface AppDelegate ()
@property (nonatomic) MCDConnectedDevicesNotificationRegistration* notificationRegistration;
@property (nonatomic) MCDConnectedDevicesAccount* pendingAccount;
@property (nonatomic) void (^pendingCallback)(BOOL,NSError*);
- (void)createNotificationRegistrationWithToken:(NSString* _Nonnull)deviceToken;
- (BOOL)processNotification:(NSDictionary* _Nonnull)userInfo;
@end

@implementation AppDelegate

- (void)initializePlatform
{
    if (!self.platform)
    {
        self.platform = [MCDConnectedDevicesPlatform new];
    }
}

- (void)startPlatform
{
    [self.platform start];
}

- (void)registerNotificationsForAccount:(MCDConnectedDevicesAccount*)account callback:(void(^)(BOOL,NSError*))callback
{
    @try
    {
        if (self.notificationRegistration)
        {
            NSLog(@"GraphNotifications Registering notifications with registration with token %@ and appId %@", self.notificationRegistration.token, self.notificationRegistration.appId);
            [self.platform.notificationRegistrationManager registerForAccountAsync:account registration:self.notificationRegistration callback:callback];
            self.pendingAccount = nil;
            self.pendingCallback = nil;
            NSLog(@"GraphNotifications Successfully registered notification");
        }
        else
        {
            NSLog(@"GraphNotifications Do not have notification registration yet, pending registration for account %@", account.accountId);
            self.pendingAccount = account;
            self.pendingCallback = callback;
        }
    }
    @catch(NSException* e)
    {
        NSLog(@"GraphNotifications Failed to register with %@", e.description);
    }
}

- (void)createNotificationRegistrationWithToken:(NSString* _Nonnull)deviceToken
{
    _notificationRegistration = [[MCDConnectedDevicesNotificationRegistration alloc] init];
    _notificationRegistration.type = MCDNotificationTypeAPN;
    _notificationRegistration.appId = [[NSBundle mainBundle] bundleIdentifier];
    _notificationRegistration.appDisplayName = (NSString*)[[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
    _notificationRegistration.token = deviceToken;
    NSLog(@"GraphNotifications Successfully created notification registration!");
    if (self.pendingAccount)
    {
        [self registerNotificationsForAccount:self.pendingAccount callback:self.pendingCallback];
    }
}

- (BOOL)processNotification:(NSDictionary* _Nonnull)userInfo
{
    @try
    {
        if ([NSJSONSerialization isValidJSONObject:userInfo])
        {
            id romeData = userInfo[@"rome-data"];
            if ([romeData isKindOfClass:NSDictionary.class])
            {
                userInfo = romeData;
            }

            // Forward the notification to CDP.
            NSError* error;
            NSData* data = [NSJSONSerialization dataWithJSONObject:userInfo options:0 error:&error];
            if (data != nil && error == nil)
            {
                NSString* byteString = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
                MCDConnectedDevicesProcessNotificationOperation* result = [self.platform processNotification:byteString];
                return result.connectedDevicesNotification;
            }
        }
        else
        {
            NSLog(@"Notification was not valid json! %@", userInfo);
        }
    }
    @catch (NSException* e)
    {
        NSLog(@"GraphNotifications Error. Processing notification failed.");
    }

    return NO;
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    NSSetUncaughtExceptionHandler(&uncaughtExceptionHandler);
    [self initializePlatform];
    [NotificationsManager startWithPlatform:self.platform];
    [self.platform.notificationRegistrationManager.notificationRegistrationStateChanged subscribe:^(__unused MCDConnectedDevicesNotificationRegistrationManager * _Nonnull manager, MCDConnectedDevicesNotificationRegistrationStateChangedEventArgs * _Nonnull args)
    {
        NSLog(@"GraphNotifications NotificationRegistrationState changed to %ld", args.state);
        if ((args.state == MCDConnectedDevicesNotificationRegistrationStateExpired) || (args.state == MCDConnectedDevicesNotificationRegistrationStateExpiring))
        {
            [[NotificationsManager sharedInstance] refresh];
        }
    }];

    // Set up Notifications
    NSDictionary* userInfo = launchOptions[UIApplicationLaunchOptionsRemoteNotificationKey];

    if (!userInfo)
    {
        // User launch app by tapping the App icon normal launch
        NSLog(@"GraphNotifications launching without user info");
        UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
        [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert | UNAuthorizationOptionSound | UNAuthorizationOptionBadge) completionHandler:^(BOOL granted, NSError * _Nullable error)
        {
            NSLog(@"GraphNotifications granted notification: %d error: %@", granted, error);
        }];

        // User launch app by tapping the App icon normal launch
        [application registerForRemoteNotifications];
        center.delegate = self;
    }
    else
    {
        @try
        {
            // app run in background and received the push notification, app is launched by user tapping the alert view
            [self processNotification:userInfo];
        }
        @catch(NSException* exception)
        {
            NSLog(@"GraphNotifications Failed start up notification with exception %@", exception);
        }
    }
    return YES;
}

- (void)application:(__unused UIApplication*)application didFailToRegisterForRemoteNotificationsWithError:(nonnull NSError *)error
{
    NSLog(@"GraphNotifications Failed to register with %@", error);
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
}


- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}


- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
}


- (void)applicationDidBecomeActive:(UIApplication *)application
{
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}


- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

- (void)application:(UIApplication*)application
didRegisterUserNotificationSettings:(__unused UIUserNotificationSettings*)notificationSettings
{
    // actually registerForRemoteNotifications after registerUserNotificationSettings is finished
    [application registerForRemoteNotifications];
    [UNUserNotificationCenter currentNotificationCenter].delegate = self;
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
    NSLog(@"GraphNotifications APNs token: %@", deviceTokenStr);

    @try
    {
        [self createNotificationRegistrationWithToken:deviceTokenStr];
    }
    @catch (NSException* exception)
    {
        NSLog(@"GraphNotifications Failed to update notification registration with exception %@", exception);
    }
}

- (void)application:(__unused UIApplication*)application didReceiveRemoteNotification:(nonnull NSDictionary*)userInfo
{
    // app run in foreground and received the push notification, pump notification into CDPPlatform
    NSLog(@"GraphNotifications Received remote notification...");
    [userInfo enumerateKeysAndObjectsUsingBlock:^(
                                                  id _Nonnull key, id _Nonnull obj, __unused BOOL* _Nonnull stop) { NSLog(@"%@: %@", key, obj); }];
    @try
    {
        if (![self processNotification:userInfo])
        {
            NSLog(@"GraphNotifications Received notification was not for Rome");
        }
    }
    @catch(NSException* exception) {
        NSLog(@"GraphNotifications Failed to receive notification with exception %@", exception);
    }
}

- (void)userNotificationCenter:(__unused UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler
{
    completionHandler(UNNotificationPresentationOptionAlert | UNNotificationPresentationOptionBadge | UNNotificationPresentationOptionSound);
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void (^)(void))completionHandler
{
    [[NotificationsManager sharedInstance] dismissNotificationWithId:response.notification.request.identifier];
    completionHandler();
}

@end
