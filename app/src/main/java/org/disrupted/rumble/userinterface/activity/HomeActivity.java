/*
 * Copyright (C) 2014 Disrupted Systems
 *
 * This file is part of Rumble.
 *
 * Rumble is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rumble is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Rumble.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.disrupted.rumble.userinterface.activity;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import org.disrupted.rumble.R;
import org.disrupted.rumble.app.RumbleApplication;
import org.disrupted.rumble.database.ChatMessageDatabase;
import org.disrupted.rumble.database.DatabaseExecutor;
import org.disrupted.rumble.database.DatabaseFactory;
import org.disrupted.rumble.database.PushStatusDatabase;
import org.disrupted.rumble.database.events.ChatMessageInsertedEvent;
import org.disrupted.rumble.database.events.ChatMessageUpdatedEvent;
import org.disrupted.rumble.database.events.StatusDatabaseEvent;
import org.disrupted.rumble.network.linklayer.bluetooth.BluetoothUtil;
import org.disrupted.rumble.userinterface.adapter.HomePagerAdapter;
import org.disrupted.rumble.userinterface.fragments.FragmentChatMessageList;
import org.disrupted.rumble.userinterface.fragments.FragmentNavigationDrawer;
import org.disrupted.rumble.userinterface.fragments.FragmentNetworkDrawer;
import org.disrupted.rumble.userinterface.fragments.FragmentStatusList;

import de.greenrobot.event.EventBus;

/**
 * @author Marlinski
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private CharSequence mTitle;

    private FragmentNavigationDrawer mNavigationDrawerFragment;
    private FragmentNetworkDrawer mNetworkDrawerFragment;
    public  SlidingMenu slidingMenu;

    private Fragment statusFragment;
    private Fragment chatFragment;
    private View notifStatus;
    private View notifChat;
    private boolean chatHasFocus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.home_layout);

        mTitle = getTitle();

        /* sliding menu with both right and left drawer */
        slidingMenu = new SlidingMenu(this);
        slidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        slidingMenu.setShadowDrawable(R.drawable.shadow);
        slidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        slidingMenu.setFadeDegree(0.35f);
        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        slidingMenu.setMode(SlidingMenu.LEFT_RIGHT);
        slidingMenu.setMenu(R.layout.navigation_frame);
        slidingMenu.setSecondaryMenu(R.layout.network_frame);
        slidingMenu.setSecondaryShadowDrawable(R.drawable.shadowright);

        if (savedInstanceState == null) {
            mNavigationDrawerFragment = new FragmentNavigationDrawer();
            mNetworkDrawerFragment    = new FragmentNetworkDrawer();
            this.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navigation_drawer_frame, mNavigationDrawerFragment).commit();
            this.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.network_drawer_frame, mNetworkDrawerFragment).commit();
        } else {
            mNavigationDrawerFragment = (FragmentNavigationDrawer) this.getSupportFragmentManager().findFragmentById(R.id.navigation_drawer_frame);
            mNetworkDrawerFragment = (FragmentNetworkDrawer) this.getSupportFragmentManager().findFragmentById(R.id.network_drawer_frame);
        }
        slidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);

        /* populate the container
        statusFragment = new FragmentStatusList();
        chatFragment = new FragmentChatMessageList();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, statusFragment)
                .commit();
        chatHasFocus = false;

        notifStatus = renderTabView(this, R.drawable.ic_world);
        notifChat   = renderTabView(this, R.drawable.ic_forum_white_24dp);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setCustomView(notifStatus));
        tabLayout.addTab(tabLayout.newTab().setCustomView(notifChat));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Fragment fragment = chatHasFocus ? statusFragment : chatFragment;
                if(chatHasFocus) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container, fragment)
                            .commit();
                    chatHasFocus = false;
                } else {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container, fragment)
                            .commit();
                    chatHasFocus = true;
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                Fragment fragment = chatHasFocus ? chatFragment : statusFragment;
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .remove(fragment)
                        .commit();
            }
        });
        tabLayout.setSelectedTabIndicatorHeight(10);
        */

        TabLayout tabLayout = (TabLayout) findViewById(R.id.home_tab_layout);
        ViewPager viewPager = (ViewPager) findViewById(R.id.home_viewpager);
        HomePagerAdapter pagerAdapter = new HomePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        // little hack to set the icons instead of text
        notifStatus = renderTabView(this, R.drawable.ic_world);
        notifChat   = renderTabView(this, R.drawable.ic_forum_white_24dp);
        tabLayout.getTabAt(0).setCustomView(notifStatus);
        tabLayout.getTabAt(1).setCustomView(notifChat);
        tabLayout.setSelectedTabIndicatorHeight(10);

        // for notification
        refreshStatusNotifications();
        refreshChatNotifications();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            slidingMenu.toggle();
            return true;
        }
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(slidingMenu.isMenuShowing()) {
                slidingMenu.toggle();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    /*
     * Receive Bluetooth Enable/Disable
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothUtil.REQUEST_ENABLE_BT:
            case BluetoothUtil.REQUEST_ENABLE_DISCOVERABLE:
                mNetworkDrawerFragment.manageBTCode(requestCode, resultCode, data);
                break;
        }
    }

    public View renderTabView(Context context, int iconResource) {
        RelativeLayout view = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.badge_tab_layout, null);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        ((ImageView)view.findViewById(R.id.tab_icon)).setImageResource(iconResource);
        ((TextView)view.findViewById(R.id.tab_badge)).setVisibility(View.INVISIBLE);
        return view;
    }

    public void refreshStatusNotifications() {
        PushStatusDatabase.StatusQueryOption statusQueryOption = new PushStatusDatabase.StatusQueryOption();
        statusQueryOption.filterFlags = PushStatusDatabase.StatusQueryOption.FILTER_READ;
        statusQueryOption.read = false;
        statusQueryOption.query_result = PushStatusDatabase.StatusQueryOption.QUERY_RESULT.COUNT;
        DatabaseFactory.getPushStatusDatabase(RumbleApplication.getContext()).getStatuses(statusQueryOption, onRefreshStatuses);
    }
    DatabaseExecutor.ReadableQueryCallback onRefreshStatuses = new DatabaseExecutor.ReadableQueryCallback() {
        @Override
        public void onReadableQueryFinished(Object object) {
            final Integer count = (Integer)object;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView view = (TextView) notifStatus.findViewById(R.id.tab_badge);
                    if (count > 0) {
                        view.setText(count.toString());
                        view.setVisibility(View.VISIBLE);
                    } else {
                        view.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
    };

    public void refreshChatNotifications() {
        ChatMessageDatabase.ChatMessageQueryOption messageQueryOptions = new ChatMessageDatabase.ChatMessageQueryOption();
        messageQueryOptions.filterFlags = ChatMessageDatabase.ChatMessageQueryOption.FILTER_READ;
        messageQueryOptions.read = false;
        messageQueryOptions.query_result = ChatMessageDatabase.ChatMessageQueryOption.QUERY_RESULT.COUNT;
        DatabaseFactory.getChatMessageDatabase(RumbleApplication.getContext()).getChatMessage(messageQueryOptions, onRefreshChatMessages);
    }
    DatabaseExecutor.ReadableQueryCallback onRefreshChatMessages = new DatabaseExecutor.ReadableQueryCallback() {
        @Override
        public void onReadableQueryFinished(Object object) {
            final Integer count = (Integer)object;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView view = (TextView)notifChat.findViewById(R.id.tab_badge);
                    if (count > 0) {
                        view.setText(count.toString());
                        view.setVisibility(View.VISIBLE);
                    } else {
                        view.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
    };

    public boolean isChatHasFocus() {
        return chatHasFocus;
    }

    /*
     * Handling Events coming from outside the activity
     */
    public void onEvent(StatusDatabaseEvent event) {
        refreshStatusNotifications();
    }
    public void onEvent(ChatMessageInsertedEvent event) {
        refreshChatNotifications();
    }
    public void onEvent(ChatMessageUpdatedEvent event) {
        refreshChatNotifications();
    }

}
