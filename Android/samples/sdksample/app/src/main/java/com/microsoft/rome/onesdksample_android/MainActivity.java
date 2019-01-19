//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.rome.onesdksample_android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.content.Context;

import java.util.ArrayList;
/**
 * Most importantly in MainActivity is the platform initialization, happening in init()
 *
 *
 */
public class MainActivity extends AppCompatActivity {
    // region Member Variables
    private static final String TAG = MainActivity.class.getName();

    public static final String DEVICE_RELAY = "Device Relay";
    public static final String USER_ACTIVITIES = "User Activities";
    public static final String HOSTING = "Hosting";
    public static final String LAUNCH = "Launch";
    public static final String SDK_SELECT = "SDK Select";

    private DrawerLayout mNavigationDrawer;
    private ListView mNavigationList;

    private ArrayList<NavigationPage> mPages;
    private ArrayList<NavigationPage> mSubPages;
    private CharSequence mCurrentTitle;
    private CharSequence mNavigationDrawerTitle;

    private RemoteSystemWatcherFragment mRemoteSystemWatcherFragment;
    private NavigationPage mActiveFragment;
    private LaunchFragment mLaunchFragment;
    private ModuleSelectFragment mModuleSelectFragment;
    private UserActivityFragment mUserActivityFragment;
    private HostingFragment mHostingFragment;
    private ConnectedDevicesManager mConnectedDevicesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRemoteSystemWatcherFragment = new RemoteSystemWatcherFragment();
        mLaunchFragment = new LaunchFragment();
        mModuleSelectFragment = new ModuleSelectFragment();
        mUserActivityFragment = new UserActivityFragment();
        mHostingFragment = new HostingFragment();

        mPages = new ArrayList<>();
        mPages.add(new NavigationPage(DEVICE_RELAY, mRemoteSystemWatcherFragment));
        mPages.add(new NavigationPage(USER_ACTIVITIES, mUserActivityFragment));
        mPages.add(new NavigationPage(HOSTING, mHostingFragment));

        mSubPages = new ArrayList<>();
        mSubPages.add(new NavigationPage(LAUNCH, mLaunchFragment));
        mSubPages.add(new NavigationPage(SDK_SELECT, mModuleSelectFragment));

        // We add (which also attaches) all fragments in the onCreate and hide them. This ensures the
        // fragments are kept alive while navigating the application, saving their state (required for the
        // LogFragment, keeping the AppServiceConnection alive, etc.). This also is also significantly
        // less expensive to than tearing down -> reinitializing fragments while navigating the app.
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        for (NavigationPage page : mPages) {
            fragmentTransaction.add(R.id.navigation_frame, page.getFragment());
            fragmentTransaction.hide(page.getFragment());
        }

        // NavigationPage pages that won't be present in the navigation drawer (automatic redirects, etc)
        for (NavigationPage page : mSubPages) {
            fragmentTransaction.add(R.id.navigation_frame, page.getFragment());
            fragmentTransaction.hide(page.getFragment());
        }

        // Commit the changes to fragmentTransaction. We can now log messages.
        fragmentTransaction.commit();

        // Action bar titles for when drawer is open/collapsed.
        mCurrentTitle = getTitle();
        mNavigationDrawerTitle = mCurrentTitle;

        // Enable action bar home button for toggling the navigation drawer.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Setup views.
        mNavigationDrawer = findViewById(R.id.navigation_drawer);
        mNavigationList = findViewById(R.id.navigation_page_list);

        // Setup navigation.
        mNavigationList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_navigation_list_item, mPages));
        mNavigationList.setOnItemClickListener(new NavigationItemClickListener());

        // Update title when drawer is opened/closed.
        ActionBarDrawerToggle actionBarDrawerToggle =
            new ActionBarDrawerToggle(this, mNavigationDrawer, R.string.drawer_open, R.string.drawer_close) {
                public void onDrawerClosed(View view) {
                    getSupportActionBar().setTitle(mCurrentTitle);
                }

                public void onDrawerOpened(View drawerView) {
                    getSupportActionBar().setTitle(mNavigationDrawerTitle);
                }
            };
        mNavigationDrawer.addDrawerListener(actionBarDrawerToggle);

        // Create the ConnectedDevicesManager
        mConnectedDevicesManager = ConnectedDevicesManager.getOrInitializeConnectedDevicesManager((Context)this);

        // Sign the user in, which may or may not require UI interaction
        mConnectedDevicesManager.signInMsa(this).thenAcceptAsync((Void v) -> {
            // Initialize the UserActivity Feed
            // TODO: Enable this once race is solved
            // getUserActivityFragment().initializeUserActivityFeed();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            toggleNavigationDrawer();
        }

        return super.onOptionsItemSelected(item);
    }
    // endregion

    /**
     * Get the current selected fragment visible to the user
     * @return The current selected fragment visible to the user
     */
    public Fragment getCurrentFragment() {
        return mActiveFragment.getFragment();
    }

    public RemoteSystemWatcherFragment getDiscoverFragment() {
        return mRemoteSystemWatcherFragment;
    }

    public LaunchFragment getLaunchFragment() {
        return mLaunchFragment;
    }

    public ModuleSelectFragment getModuleSelectFragment() {
        return mModuleSelectFragment;
    }

    public UserActivityFragment getUserActivityFragment() {
        return mUserActivityFragment;
    }

    public HostingFragment getHostingFragment() {
        return mHostingFragment;
    }

    // region Navigation
    public void navigateToPage(String page) {
        navigateToPage(getNativationPage((page)));
    }

    /**
     * Replaces the current fragment, if any, loaded in the navigation frame with the fragment
     * representing the selected page.
     *
     * @param position Index of the page to navigate to
     */
    private void navigateToPage(int position) {
        navigateToPage(mPages.get(position));

        // Update the navigation drawer.
        mNavigationList.setItemChecked(position, true);
        mNavigationDrawer.closeDrawer(mNavigationList);
    }

    private void navigateToPage(NavigationPage page) {
        Log.d(TAG, "Navigate to page [" + page.getTitle() + "]");

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        // Hide the current visible fragment.
        if (mActiveFragment != null) {
            fragmentTransaction.hide(mActiveFragment.getFragment());
        }

        // Load the fragment for the selected page.
        fragmentTransaction.show(page.getFragment());
        fragmentTransaction.commit();

        // Update the Active Fragment to point to the selected page.
        mActiveFragment = page;

        // Update page title.
        setTitle(page.getTitle());
    }

    private NavigationPage getNativationPage(String string) {
        Log.d(TAG, "Finding page [" + string + "]");

        switch (string) {
        case DEVICE_RELAY: return mPages.get(0);
        case USER_ACTIVITIES: return mPages.get(1);
        case HOSTING: return mPages.get(2);
        case LAUNCH: return mSubPages.get(0);
        case SDK_SELECT: return mSubPages.get(1);
        default: throw new IllegalArgumentException("String [" + string + "] was not found as a page");
        }
    }

    /**
     * Opens/closes the navigation drawer based on current state
     */
    private void toggleNavigationDrawer() {
        if (mNavigationDrawer.isDrawerOpen(mNavigationList)) {
            mNavigationDrawer.closeDrawer(mNavigationList);
        } else {
            mNavigationDrawer.openDrawer(mNavigationList);
        }
    }

    private void raiseToast(final String message) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    /**
     * Listener for handling navigation to the selected page on click.
     */
    private class NavigationItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            navigateToPage(position);
        }
    }

    /**
     * Basic wrapper representing a page to navigate to within the app.
     */
    private class NavigationPage {
        private CharSequence mTitle;
        private Fragment mFragment;

        NavigationPage(CharSequence title, Fragment fragment) {
            mTitle = title;
            mFragment = fragment;
        }

        CharSequence getTitle() {
            return mTitle;
        }

        Fragment getFragment() {
            return mFragment;
        }

        @Override
        public String toString() {
            return mTitle.toString();
        }
    }
    // endregion
}
