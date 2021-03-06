package org.azavea.otm.ui;

import org.azavea.otm.App;
import org.azavea.otm.R;


import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class TabLayout extends OTMActionBarActivity {

    private static final String SELECTED_TAB = "TAB";

    private static final String MAIN_MAP = "MainMapActivity";
    private static final String PROFILE = "ProfileDisplay";
    private static final String LISTS = "ListDisplay";
    private static final String ABOUT = "AboutDisplay";
    private Menu menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.addTab(
                actionBar.newTab()
                        .setText(R.string.tab_map)
                        .setTag(MAIN_MAP)
                        .setTabListener(new TabListener<>(this, MAIN_MAP, MainMapFragment.class))
        );

        actionBar.addTab(
                actionBar.newTab()
                        .setText(R.string.tab_profile)
                        .setTabListener(new TabListener<>(this, PROFILE, ProfileDisplay.class))
        );

        actionBar.addTab(
                actionBar.newTab()
                        .setText(R.string.tab_lists)
                        .setTabListener(new TabListener<>(this, LISTS, ListDisplay.class))
        );

        actionBar.addTab(
                actionBar.newTab()
                        .setText(R.string.tab_about)
                        .setTabListener(new TabListener<>(this, ABOUT, AboutDisplay.class))
        );

        if (savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt(SELECTED_TAB));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        // A bit of an annoyance, the TabLayout Activity gets the backpress events
        // and must delegate them back down to the MainMapActivity Fragment
        // If we need to support handling back presses differently on each tab,
        // we should probably make an Interface and call whatever the current tab is
        ActionBar actionBar = getActionBar();
        if (actionBar.getSelectedTab().getTag() == MAIN_MAP) {
            final FragmentManager manager = TabLayout.this.getFragmentManager();
            MainMapFragment mainMap = (MainMapFragment) manager.findFragmentByTag(MAIN_MAP);
            if (mainMap.shouldHandleBackPress()) {
                mainMap.onBackPressed();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(SELECTED_TAB, getActionBar().getSelectedNavigationIndex());
    }

    public class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment tabFragment;
        private final Activity host;
        private final String tag;
        private final Class<T> tabClass;

        /**
         * Constructor used each time a new tab is created.
         *
         * @param host The host Activity, used to instantiate the fragment
         * @param tag  The identifier tag for the fragment
         * @param clz  The fragment's Class, used to instantiate the fragment
         */
        public TabListener(Activity host, String tag, Class<T> clz) {
            this.host = host;
            this.tag = tag;
            tabClass = clz;

            final FragmentManager manager = TabLayout.this.getFragmentManager();
            tabFragment = manager.findFragmentByTag(tag);
            if (tabFragment != null && !tabFragment.isHidden()) {
                manager.beginTransaction().hide(tabFragment).commit();
            }
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            // Check if the fragment is already initialized
            if (tabFragment == null) {
                // If not, instantiate and add it to the activity
                tabFragment = Fragment.instantiate(host, tabClass.getName());
                ft.add(android.R.id.content, tabFragment, tag);
            } else {
                // If it exists, simply attach it in order to show it
                ft.show(tabFragment);
            }
            App.getAppInstance().sendFragmentView(tabFragment, TabLayout.this);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (tabFragment != null) {
                // Detach the fragment, because another one is being attached
                ft.hide(tabFragment);
            }
            // Hide the soft keyboard if it is up
            View currentView = getCurrentFocus();
            if (currentView != null) {
                InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(currentView.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
            if (menu != null) {
                menu.clear();
            }
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }
}
