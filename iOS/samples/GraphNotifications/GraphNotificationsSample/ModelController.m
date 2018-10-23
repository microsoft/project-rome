#import "ModelController.h"
#import "LoginViewController.h"



@interface ModelController ()

@end

@implementation ModelController

- (instancetype)init {
    self = [super init];
    return self;
}

- (UIViewController *)viewControllerAtIndex:(NSUInteger)index storyboard:(UIStoryboard *)storyboard {
    
    switch(index) {
        case 0: {
            LoginViewController *dataViewController = [storyboard instantiateViewControllerWithIdentifier:@"LoginViewController"];
            return dataViewController;
        }
        default:
            return nil;
    }
}


- (NSUInteger)indexOfViewController:(UIViewController *)viewController {
    if ([viewController isKindOfClass:[LoginViewController class]]) {
        return 0;
    }
    
    return NSNotFound;
}


#pragma mark - Page View Controller Data Source

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerBeforeViewController:(UIViewController *)viewController
{
    NSUInteger index = [self indexOfViewController:viewController];
    if ((index == 0) || (index == NSNotFound)) {
        return nil;
    }
    
    return [self viewControllerAtIndex:--index storyboard:viewController.storyboard];
}

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerAfterViewController:(UIViewController *)viewController
{
    NSUInteger index = [self indexOfViewController:viewController];
    if (index == NSNotFound || index >= 2) {
        return nil;
    }
    
    return [self viewControllerAtIndex:++index storyboard:viewController.storyboard];
}

@end
