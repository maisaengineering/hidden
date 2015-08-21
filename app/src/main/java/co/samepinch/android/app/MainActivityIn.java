/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.samepinch.android.app;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.squareup.otto.Subscribe;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.adapters.SPFragmentPagerAdapter;
import co.samepinch.android.app.helpers.intent.PostsPullService;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.app.helpers.widget.SIMView;

import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_FNAME;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_LNAME;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_PHOTO;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_PINCH_HANDLE;

/**
 * TODO
 */
public class MainActivityIn extends AppCompatActivity {
    public static final String TAG = "MainActivityIn";
    private static final int INTENT_LOGOUT = 1;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @Bind(R.id.nav_view)
    NavigationView mNavigationView;

    @Bind(R.id.tabs)
    TabLayout mTabLayout;

    @Bind(R.id.viewpager)
    ViewPager mViewPager;

    @Bind(R.id.nav_header_switch)
    ViewSwitcher mHeaderSwitch;

    @Bind(R.id.nav_header_img)
    SIMView mNavHeaderImg;

    @Bind(R.id.nav_header_name)
    TextView mNavHeaderName;

    @Bind(R.id.nav_header_summary)
    TextView mNavHeaderSummary;

    SPFragmentPagerAdapter adapterViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_in);

        ButterKnife.bind(MainActivityIn.this);
        BusProvider.INSTANCE.getBus().register(this);

        setSupportActionBar(mToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.ic_menu);
        ab.setDisplayHomeAsUpEnabled(true);

        Map<String, String> userInfo = Utils.PreferencesManager.getInstance().getValueAsMap(AppConstants.API.PREF_AUTH_USER.getValue());
        if (StringUtils.isNotBlank(userInfo.get(KEY_PHOTO.getValue()))) {
            mNavHeaderImg.populateImageViewWithAdjustedAspect(userInfo.get(KEY_PHOTO.getValue()));
        } else {
            String fName = userInfo.get(KEY_FNAME.getValue());
            String lName = userInfo.get(KEY_LNAME.getValue());
            String initials = StringUtils.join(StringUtils.substring(fName, 0, 1), StringUtils.substring(lName, 0, 1));
            mNavHeaderName.setText(initials);
            mHeaderSwitch.showNext();
        }

        String pinchHandle = String.format(getString(R.string.pinch_handle), userInfo.get(KEY_PINCH_HANDLE.getValue()));
        mNavHeaderSummary.setText(pinchHandle);

        setupDrawerContent(mNavigationView);
        setupViewPager(mViewPager);

        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabsFromPagerAdapter(adapterViewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            tab.setCustomView(SPFragmentPagerAdapter.getTabView(getApplicationContext(), i));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_LOGOUT) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                if (!this.isFinishing()) {
                    finish();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menuitem_sign_in_id).setVisible(false);
        menu.findItem(R.id.menuitem_sign_out_id).setVisible(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.menuitem_sign_out_id:
                Intent logOutIntent = new Intent(getApplicationContext(), LogoutActivity.class);
                startActivityForResult(logOutIntent, INTENT_LOGOUT);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        BusProvider.INSTANCE.getBus().unregister(this);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapterViewPager = new SPFragmentPagerAdapter(getSupportFragmentManager());
        adapterViewPager.setCount(2);
        viewPager.setAdapter(adapterViewPager);
    }

    private void setupDrawerContent(NavigationView navigationView) {

//        SIMView imgView = new SIMView(getApplicationContext());
//        imgView.populateImageView(userInfo.get("photo"));
//        navigationView.addHeaderView(imgView);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });
    }

    @OnClick(R.id.fab)
    public void onClickFAB() {
        Bundle iArgs = new Bundle();
        Utils.PreferencesManager pref = Utils.PreferencesManager.getInstance();
        Map<String, String> pPosts = pref.getValueAsMap(AppConstants.API.PREF_POSTS_LIST.getValue());
        for (Map.Entry<String, String> e : pPosts.entrySet()) {
            iArgs.putString(e.getKey(), e.getValue());
        }

        // call for intent
        Intent mServiceIntent =
                new Intent(getApplicationContext(), PostsPullService.class);
        mServiceIntent.putExtras(iArgs);
        startService(mServiceIntent);
    }

    @Subscribe
    public void onPostsRefreshedEvent(Events.PostsRefreshedEvent event) {
        Snackbar.make(this.findViewById(R.id.fab), "refreshed", Snackbar.LENGTH_LONG).show();
    }
}
