# Platform class
This class handles the process of connecting the application to Microsoft's Remote Systems service. This must be handled before any remote device discovery is attempted.

## Syntax
`public final class Platform`

## Public methods

### initialize
Establishes the Remote Systems platform for use by the app.

`public static void initialize(Context context, final ITokenProvider tokenProvider, final IPlatformInitializationHandler initializationHandler)`

**Parameters**
*context* - the **Context** for the current application
*tokenProvider* - an implementation of [**ITokenProvider**](ITokenProvider.md) that can deliver the required authentication information for this app
*initializationHandler* - an implementation of [**IPlatformInitializationHandler**](IPlatformInitializationHandler.md) that determines what is done when this method succeeds or fails

### shutdown
Closes the Remote Systems platform.

`public static void shutdown()`

### suspend
Suspends the Remote Systems platform. Recommended to invoke in Activity.onPause(). Safe to call even if platform is not yet initialized.

`public static void suspend()`

### resume
Resumes the Remote Systems platform from suspension. Recommended to invoke in Activity.onPause(). Safe to call even if platform is not yet initialized.

`public static void resume()`