package co.samepinch.android.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ToxicBakery.viewpager.transforms.DepthPageTransformer;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.IntentPickerSheetView;

import org.apache.commons.lang3.StringUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.RootActivity;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.adapters.SPFragmentPagerAdapter;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private static final int INTENT_LOGIN = 0;

    @Bind(R.id.bottomsheet)
    BottomSheetLayout mBottomsheet;

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

    protected static void doSpreadIt(final Activity activity, final BottomSheetLayout bs) {
        final String subject = activity.getString(R.string.share_subject);
        final String body = activity.getString(R.string.share_body);
        // prepare menu options
        View menu = LayoutInflater.from(activity).inflate(R.layout.bs_menu, bs, false);
        final LinearLayout layout = (LinearLayout) menu.findViewById(R.id.layout_menu_list);
        // email
        TextView viaEmail = (TextView) LayoutInflater.from(activity).inflate(R.layout.bs_raw_email, null);
        viaEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bs.isSheetShowing()) {
                    bs.dismissSheet();
                }
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(Intent.EXTRA_TEXT, body);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    activity.startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(activity, SPApplication.getContext().getString(R.string.msg_no_email), Toast.LENGTH_SHORT).show();
                }
            }
        });
        layout.addView(viaEmail);

        // others
        TextView viaOther = (TextView) LayoutInflater.from(activity).inflate(R.layout.bs_raw_via_other, null);
        viaOther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // sms
                final Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                shareIntent.putExtra(Intent.EXTRA_TITLE, subject);
                shareIntent.putExtra(Intent.EXTRA_TEXT, body);

                shareIntent.setType("text/plain");
                IntentPickerSheetView sheetView = new IntentPickerSheetView(activity, shareIntent, StringUtils.EMPTY, new IntentPickerSheetView.OnIntentPickedListener() {
                    @Override
                    public void onIntentPicked(IntentPickerSheetView.ActivityInfo activityInfo) {
                        if (bs.isSheetShowing()) {
                            bs.dismissSheet();
                        }
                        activity.startActivity(activityInfo.getConcreteIntent(shareIntent));
                    }
                });
                layout.removeAllViews();
                layout.addView(sheetView);
            }
        });
        layout.addView(viaOther);
        bs.showWithSheetView(menu);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        conditionallyLogin();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conditionallyLogin();

        setContentView(R.layout.activity_main);
        ButterKnife.bind(MainActivity.this);
        BusProvider.INSTANCE.getBus().register(this);
        setupDrawerContent(mNavigationView);

        setSupportActionBar(mToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);

        // navigation view
        View headerView = LayoutInflater.from(SPApplication.getContext()).inflate(R.layout.main_tab_header, null);
        // login handler setup
        setupLoginHandler(headerView.findViewById(R.id.nav_login));

        // settings handler setup
        setupSettingsHandler(headerView.findViewById(R.id.nav_settings));

        // in-app notifications handler setup
        setupInAppNotifsHandler(headerView.findViewById(R.id.nav_notif));

        // attach view
        ab.setDisplayShowCustomEnabled(true);
        ab.setCustomView(headerView);


        SPFragmentPagerAdapter adapterViewPager = new SPFragmentPagerAdapter(getSupportFragmentManager());
        adapterViewPager.setCount(1);
        mViewPager.setAdapter(adapterViewPager);
        mViewPager.setPageTransformer(false, new DepthPageTransformer());

        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabsFromPagerAdapter(adapterViewPager);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            tab.setCustomView(SPFragmentPagerAdapter.getTabView(getApplicationContext(), i));
        }
    }

    private void conditionallyLogin() {
        if (Utils.isLoggedIn()) {
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

    private void setupLoginHandler(View view) {
        view.setVisibility(View.VISIBLE);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivityForResult(loginIntent, INTENT_LOGIN);
            }
        });
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
        if (requestCode == INTENT_LOGIN) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.menuitem_sign_in_id:
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivityForResult(loginIntent, INTENT_LOGIN);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BusProvider.INSTANCE.getBus().unregister(this);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        setupDrawerNavListener();
    }

    private void setupDrawerNavListener() {
        mNavigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        Bundle args = new Bundle();
                        switch (menuItem.getItemId()) {
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
                            case R.id.nav_spread_it:
                                doSpreadIt(MainActivity.this, mBottomsheet);
                                break;
                            case R.id.nav_rate_it:
                                Intent iRate = new Intent(Intent.ACTION_VIEW);
                                iRate.setData(Uri.parse(AppConstants.API.GPLAY_LINK.getValue()));
                                startActivity(iRate);
                                break;
                            case R.id.nav_feedback:
                                doFeedback();
                                break;
                            case R.id.nav_sign_in:
                                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivityForResult(loginIntent, INTENT_LOGIN);
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

    private void doFeedback() {
        final String subject = SPApplication.getContext().getString(R.string.feedback_subject);
        final String to = SPApplication.getContext().getString(R.string.feedback_to);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, SPApplication.getContext().getString(R.string.msg_no_email), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.fab)
    public void onClickFAB() {
        Toast.makeText(MainActivity.this, SPApplication.getContext().getString(R.string.msg_login_req), Toast.LENGTH_LONG).show();

        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivityForResult(loginIntent, INTENT_LOGIN);
    }

    @OnClick(R.id.wall_notice)
    public void onClickNotice() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivityForResult(loginIntent, INTENT_LOGIN);
    }
}
