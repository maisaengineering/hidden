package co.samepinch.android.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.facebook.drawee.generic.RoundingParams;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.Postprocessor;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.gson.Gson;
import com.squareup.otto.Subscribe;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.ImageUtils;
import co.samepinch.android.app.helpers.RootActivity;
import co.samepinch.android.app.helpers.SmartFragmentStatePagerAdapter;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.intent.AllNotificationsService;
import co.samepinch.android.app.helpers.intent.DotDetailsService;
import co.samepinch.android.app.helpers.misc.FragmentLifecycle;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.app.helpers.widget.SIMView;
import co.samepinch.android.data.dao.SchemaDots;
import co.samepinch.android.data.dto.User;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivityIn extends AppCompatActivity {
    public static final String TAG = "MainActivityIn";
    public static final String DFLT_ZERO = "0";
    private static final int INTENT_LOGOUT = 1;
    private static final int TAB_ITEM_COUNT = 2;
    @Bind(R.id.bottomsheet)
    BottomSheetLayout mBottomsheet;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    /**
     * BODY
     */
    @Bind(R.id.tabs)
    TabLayout mTabLayout;

    @Bind(R.id.viewpager)
    ViewPager mViewPager;

    /**
     * NAVIGATION RELATED
     */
    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @Bind(R.id.nav_view)
    NavigationView mNavigationView;

    @Bind(R.id.wall_notice)
    TextView mWallNotice;

    NavViews nv;
    private LocalHandler mHandler;
    private User mUser;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // logout if needed
        conditionallyLogout();

        setContentView(R.layout.activity_main_in);
        ButterKnife.bind(MainActivityIn.this);
        BusProvider.INSTANCE.getBus().register(this);

        mNavigationView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                nv = new NavViews();
                ButterKnife.bind(nv, mNavigationView);
                mNavigationView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setupDrawerContent(mUser, true);
            }
        });

        setSupportActionBar(mToolbar);
        ActionBar ab = getSupportActionBar();
//        ab.setHomeAsUpIndicator(R.drawable.menu_blue);
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);

        View headerView = LayoutInflater.from(SPApplication.getContext()).inflate(R.layout.main_tab_header, null);
        // settings handler setup
        setupSettingsHandler(headerView.findViewById(R.id.nav_settings));
        // in-app notifications handler setup
        setupInAppNotifsHandler(headerView.findViewById(R.id.nav_notif));

        // attach view
        ab.setDisplayShowCustomEnabled(true);
        ab.setCustomView(headerView);

        setupWindowAnimations((ImageView) headerView.findViewById(R.id.home));

        // handler
        mHandler = new LocalHandler(this);
        setupViewPager();

        //update user details
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Bundle iArgs = new Bundle();
                iArgs.putString(AppConstants.K.DOT.name(), mUser.getUid());
                Intent intent =
                        new Intent(getApplicationContext(), DotDetailsService.class);
                intent.putExtras(iArgs);
                startService(intent);
            }
        }, 99);

        //update user details
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // refresh user notifications
                Intent intentNotifs =
                        new Intent(getApplicationContext(), AllNotificationsService.class);
                startService(intentNotifs);
            }
        }, 100);
    }

    private void conditionallyLogout() {
        try {
            if (Utils.isLoggedIn()) {
                String userStr = Utils.PreferencesManager.getInstance().getValue(AppConstants.API.PREF_AUTH_USER.getValue());
                Gson gson = new Gson();
                mUser = gson.fromJson(userStr, User.class);
            } else {
                throw new IllegalStateException("failed to load user.");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() == null ? "" : e.getMessage(), e);
            Intent intent = new Intent(this, RootActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            if (!this.isFinishing()) {
                this.finish();
            }
        }
    }

    private void showNotice() {
        try {

            String userStr = Utils.PreferencesManager.getInstance().getValue(AppConstants.API.PREF_AUTH_USER.getValue());
            Gson gson = new Gson();
            User _user = gson.fromJson(userStr, User.class);

            View.OnClickListener clickListener = null;
            Integer textToShow = null;
            // user reminders
            if (_user.getVerified() == null || !_user.getVerified()) {
                textToShow = R.string.notice_update_phone;
                clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getApplicationContext(), EnterPhoneActivity.class);
                        startActivity(intent);
                    }
                };
            } else if (StringUtils.isBlank(_user.getEmail())) {
                textToShow = R.string.notice_update_email;
                clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle();
                        // target
                        args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_DOTEDIT.name());

                        // intent
                        Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
                        intent.putExtras(args);
                        startActivity(intent);
                    }
                };
            }
            if (clickListener != null && textToShow != null) {
                mWallNotice.animate()
                        .alpha(1.0f)
                        .setDuration(2500)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                mWallNotice.setVisibility(View.VISIBLE);
                                mWallNotice.animate().setListener(null);
                            }
                        });
                mWallNotice.setOnClickListener(clickListener);
                mWallNotice.setText(textToShow);
            }

        } catch (Exception e) {
            // muted
        }
    }

    private void setupSettingsHandler(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }


    private void setupInAppNotifsHandler(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                // target
                args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_NOTIFS.name());

                // intent
                Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtras(args);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_LOGOUT) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(this, RootActivity.class);
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

    private void setupViewPager() {
        final TabItemAdapter pagerAdapter = new TabItemAdapter(getSupportFragmentManager(), TAB_ITEM_COUNT);
        mViewPager.setAdapter(pagerAdapter);

//        mViewPager.setPageTransformer(false, new FlipHorizontalTransformer());
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                FragmentLifecycle active = (FragmentLifecycle) pagerAdapter.getItem(position);
                active.onResumeFragment();

                FragmentLifecycle passive;
                for (int i = 0; i < pagerAdapter.getCount(); i++) {
                    if (i == position) {
                        // skip
                        continue;
                    }
                    // rest call onPause even though redundant for already hidden
                    passive = (FragmentLifecycle) pagerAdapter.getItem(i);
                    passive.onPauseFragment();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        // setup tabs
        mTabLayout.setupWithViewPager(mViewPager);
        // title tabs
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            tab.setCustomView(getCustomTabView(i));
        }

    }

    public View getCustomTabView(int position) {
        View v = LayoutInflater.from(SPApplication.getContext()).inflate(R.layout.custom_tab_main, null);
        ViewSwitcher vs = (ViewSwitcher) v.findViewById(R.id.tab_main_switch);
        vs.setDisplayedChild(position);
        return v;
    }

    private void setupDrawerContent(final User user, boolean init) {
        if (nv == null) {
            Intent intent = new Intent(MainActivityIn.this, MainActivityIn.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        String fName = user.getFname();
        String lName = user.getLname();
        // tag map
        if (StringUtils.isBlank(user.getPhoto())) {
            nv.mVS.setDisplayedChild(1);
            String initials = StringUtils.join(StringUtils.substring(fName, 0, 1), StringUtils.substring(lName, 0, 1));
            initials = StringUtils.isNotBlank(initials) ? initials : StringUtils.substring(user.getPinchHandle(), 0, 1);

            nv.mDotImageText.setText(initials);
            Bitmap blurredBitmap = ImageUtils.blurredDfltBitmap();
            nv.mBackdrop.setImageBitmap(blurredBitmap);
        } else {
            nv.mVS.setDisplayedChild(0);
            Postprocessor postprocessor = new BasePostprocessor() {
                @Override
                public String getName() {
                    return "redMeshPostprocessor";
                }

                @Override
                public void process(final Bitmap bitmap) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            nv.mBackdrop.setImageBitmap(null);
                            Bitmap blurredBitmap = ImageUtils.blur(getApplicationContext(), bitmap);
                            nv.mBackdrop.setImageBitmap(blurredBitmap);
                        }
                    });

                }
            };

            RoundingParams roundingParams = RoundingParams.asCircle();
            roundingParams.setBorder(R.color.light_blue_500, 1.0f);

            nv.mDotImage.setRoundingParams(roundingParams);
            nv.mDotImage.populateImageViewWithAdjustedAspect(user.getPhoto(), postprocessor);
            nv.mBackdrop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle args = new Bundle();
                    // target
                    args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_DOTEDIT.name());

                    // intent
                    Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
                    intent.putExtras(args);
                    startActivityForResult(intent, AppConstants.KV.REQUEST_EDIT_DOT.getIntValue());
                }
            });
        }

        nv.mDotName.setText(StringUtils.join(new String[]{fName, lName}, " "));
        if (user.getFollowersCount() != null) {
            nv.mDotFollowersCnt.setText(Long.toString(user.getFollowersCount()));
        } else {
            nv.mDotFollowersCnt.setText(DFLT_ZERO);
        }

        if (StringUtils.isNotBlank(user.getSummary())) {
            nv.mDotAbout.setText(user.getSummary());
        } else {
            nv.mDotAbout.setVisibility(View.GONE);
        }

        if (user.getPostsCount() != null) {
            nv.mDotPostsCnt.setText(Long.toString(user.getPostsCount()));
        } else {
            nv.mDotPostsCnt.setText(DFLT_ZERO);
        }

        if (Utils.isValidUri(user.getBlog())) {
            nv.mDotBlogWrapper.setVisibility(View.VISIBLE);
            nv.mDotBlog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.getBlog()));
                    startActivity(intent);
                }
            });
        } else {
            nv.mDotBlogWrapper.setVisibility(View.GONE);
        }

        if (init) {
            nv.mDotEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle args = new Bundle();
                    // target
                    args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_DOTEDIT.name());

                    // intent
                    Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
                    intent.putExtras(args);
                    startActivityForResult(intent, AppConstants.KV.REQUEST_EDIT_DOT.getIntValue());
                }
            });

            setupDrawerNavListener();
        }
    }

    private void setupDrawerNavListener() {
        mNavigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        Bundle args = new Bundle();
                        switch (menuItem.getItemId()) {
                            case R.id.nav_wall:
//                                mViewPager.getAdapter().notifyDataSetChanged();
//                                mViewPager.setCurrentItem(0);
                                break;
                            case R.id.nav_post:
                                args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_CREATE_POST.name());
                                break;
                            case R.id.nav_tags:
                                args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_MANAGE_TAGS.name());
                                break;
                            case R.id.nav_settings:
                                Map<String, String> userInfo = Utils.PreferencesManager.getInstance().getValueAsMap(AppConstants.API.PREF_AUTH_USER.getValue());
                                String userId = userInfo.get(AppConstants.APP_INTENT.KEY_UID.getValue());
                                String userSettings = String.format(AppConstants.API.URL_USER_SETTINGS.getValue(), userId);
                                args.putString(AppConstants.K.REMOTE_URL.name(), userSettings);
                                args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_WEBVIEW.name());
                                break;
                            case R.id.nav_rules:
                                args.putString(AppConstants.K.REMOTE_URL.name(), AppConstants.API.URL_RULES.getValue());
                                args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_WEBVIEW.name());
                                break;
                            case R.id.nav_sys_status:
                                args.putString(AppConstants.K.REMOTE_URL.name(), AppConstants.API.URL_SYS_STATUS.getValue());
                                args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_WEBVIEW.name());
                                break;
                            case R.id.nav_t_n_c:
                                args.putString(AppConstants.K.REMOTE_URL.name(), AppConstants.API.URL_TERMS_COND.getValue());
                                args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_WEBVIEW.name());
                                break;
                            case R.id.nav_rate_it:
                                Intent iRate = new Intent(Intent.ACTION_VIEW);
                                iRate.setData(Uri.parse(AppConstants.API.GPLAY_LINK.getValue()));
                                startActivity(iRate);
                                break;
                            case R.id.nav_feedback:
                                doFeedback();
                                break;
                            case R.id.nav_spread_it:
                                MainActivity.doSpreadIt(MainActivityIn.this, mBottomsheet);
                                break;

                            case R.id.nav_sign_out:
                                Intent logOutIntent = new Intent(getApplicationContext(), LogoutActivity.class);
                                startActivityForResult(logOutIntent, INTENT_LOGOUT);
                                break;
                            default:
                                break;
                        }
                        if (!args.isEmpty()) {
                            // intent
                            Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtras(args);
                            startActivity(intent);
                        }
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        conditionallyLogout();

        // notice clean up
        mWallNotice.clearAnimation();
        mWallNotice.setAlpha(0.0f);

        mNavigationView.getMenu().getItem(0).setChecked(true);
        //update user details
        Bundle iArgs = new Bundle();
        iArgs.putString(AppConstants.K.DOT.name(), mUser.getUid());
        Intent intent =
                new Intent(getApplicationContext(), DotDetailsService.class);
        intent.putExtras(iArgs);
        startService(intent);

        String shouldRefresh = Utils.PreferencesManager.getInstance().getValue(AppConstants.APP_INTENT.KEY_FRESH_WALL_FLAG.getValue());
        if (shouldRefresh != null && Boolean.valueOf(shouldRefresh).booleanValue()) {

            // drawer
            String userStr = Utils.PreferencesManager.getInstance().getValue(AppConstants.API.PREF_AUTH_USER.getValue());
            Gson gson = new Gson();
            setupDrawerContent(gson.fromJson(userStr, User.class), false);

            // body
//            TabItemAdapter adapter = (TabItemAdapter) mViewPager.getAdapter();
//            adapter.notifyDataSetChanged();

            // invalidate menu too?
            invalidateOptionsMenu();
        }
        // remove if there is one
        Utils.PreferencesManager.getInstance().remove(AppConstants.APP_INTENT.KEY_FRESH_WALL_FLAG.getValue());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewPager.clearOnPageChangeListeners();
        BusProvider.INSTANCE.getBus().unregister(this);
    }

    private void doFeedback() {
        final String subject = getApplicationContext().getString(R.string.feedback_subject);
        final String to = getApplicationContext().getString(R.string.feedback_to);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivityIn.this, SPApplication.getContext().getString(R.string.msg_no_email), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.fab)
    public void onClickFAB() {
        Bundle args = new Bundle();
        args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_CREATE_POST.name());

        // intent
        Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtras(args);
        startActivity(intent);
    }

    @Subscribe
    public void onDotDetailsRefreshEvent(final Events.DotDetailsRefreshEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Cursor cursor = null;
                    try {
                        cursor = getContentResolver().query(SchemaDots.CONTENT_URI, null, SchemaDots.COLUMN_UID + "=?", new String[]{mUser.getUid()}, null);
                        if (cursor.moveToFirst()) {
                            User updatedUser = Utils.cursorToUserEntity(cursor);
                            if(Utils.updateAuthUserToPref(updatedUser)){
                                invalidateOptionsMenu();
                                setupDrawerContent(updatedUser, false);
                            }
                        }
                    } catch (Exception e) {
                        //e.printStackTrace();
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                    showNotice();
                } catch (Exception e) {
                    // muted
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupWindowAnimations(ImageView imageView) {
        try {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            imageView.startAnimation(animation);
        } catch (Throwable te) {
            // muted
        }
    }

    static class NavViews {
        @Bind(R.id.dot_wall_switch)
        ViewSwitcher mVS;

        @Bind(R.id.backdrop)
        ImageView mBackdrop;

        @Bind(R.id.dot_wall_image)
        SIMView mDotImage;

        @Bind(R.id.dot_wall_image_txt)
        TextView mDotImageText;

        @Bind(R.id.dot_wall_name)
        TextView mDotName;

//        @Bind(R.id.dot_wall_handle)
//        TextView mDotHandle;

        @Bind(R.id.dot_wall_about)
        TextView mDotAbout;

        @Bind(R.id.dot_wall_followers_count)
        TextView mDotFollowersCnt;

        @Bind(R.id.dot_wall_posts_count)
        TextView mDotPostsCnt;

        @Bind(R.id.dot_wall_blog_wrapper)
        LinearLayout mDotBlogWrapper;

        @Bind(R.id.dot_wall_blog)
        TextView mDotBlog;

        @Bind(R.id.dot_wall_edit)
        TextView mDotEdit;
    }

    private static final class LocalHandler extends Handler {
        private final WeakReference<MainActivityIn> mActivity;

        public LocalHandler(MainActivityIn parent) {
            mActivity = new WeakReference<MainActivityIn>(parent);
        }
    }

    public static class TabItemAdapter extends SmartFragmentStatePagerAdapter {
        private final int itemCount;

        public TabItemAdapter(FragmentManager fm, int itemCount) {
            super(fm);
            this.itemCount = itemCount;
        }

        @Override
        public int getCount() {
            return itemCount;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = getRegisteredFragment(position);
            if (fragment == null) {
                switch (position) {
                    case 0:
                        fragment = PostListFragment.newInstance(position);
                        break;
                    case 1:
                        fragment = FavPostListFragment.newInstance(position);
                        break;
                    default:
                        throw new IllegalStateException("non-bound position index: " + position);
                }
            }
            return fragment;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }
    }
}
