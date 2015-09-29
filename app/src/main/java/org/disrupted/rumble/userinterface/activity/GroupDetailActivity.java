/*
 * Copyright (C) 2014 Disrupted Systems
 * This file is part of Rumble.
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
 * You should have received a copy of the GNU General Public License along
 * with Rumble.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.disrupted.rumble.userinterface.activity;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import org.disrupted.rumble.R;
import org.disrupted.rumble.userinterface.adapter.GroupDetailPagerAdapter;
import org.disrupted.rumble.userinterface.fragments.FragmentContactList;
import org.disrupted.rumble.userinterface.fragments.FragmentStatusList;

/**
 * @author Marlinski
 */
public class GroupDetailActivity extends AppCompatActivity {

    private static final String TAG = "GroupStatusActivity";

    private boolean message_has_focus;
    FragmentStatusList  statusFragment;
    FragmentContactList contactFragment;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getIntent().getExtras();
        String name = args.getString("GroupName");

        setContentView(R.layout.group_detail);
        setTitle(name);

        /* setting up the toolbar */
        Toolbar toolbar = (Toolbar) findViewById(R.id.group_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        /* next we set up the view pager and the tablayout */
        TabLayout tabLayout = (TabLayout) findViewById(R.id.group_tab_layout);
        ViewPager viewPager = (ViewPager) findViewById(R.id.group_viewpager);
        GroupDetailPagerAdapter pagerAdapter = new GroupDetailPagerAdapter(getSupportFragmentManager(), args);
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setSelectedTabIndicatorHeight(10);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id==android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.activity_close_enter, R.anim.activity_close_exit);
    }

}
