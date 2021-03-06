package co.samepinch.android.app;

import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.IntentPickerSheetView;
import com.squareup.otto.Subscribe;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.TimeUtils;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.adapters.PostDetailsRVAdapter;
import co.samepinch.android.app.helpers.intent.PostDetailsService;
import co.samepinch.android.app.helpers.intent.PostMetaUpdateService;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.data.dao.SchemaComments;
import co.samepinch.android.data.dao.SchemaDots;
import co.samepinch.android.data.dao.SchemaPostDetails;
import co.samepinch.android.data.dto.PostDetails;
import co.samepinch.android.data.dto.User;

import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_UID;

public class PostDetailActivity extends AppCompatActivity {
    public static final String TAG = "PostDetailActivity";

    @Bind(R.id.bottomsheet)
    BottomSheetLayout mBottomsheet;

//    @Bind(R.id.post_dot)
//    ViewGroup mPostDot;

    @Bind(R.id.post_dot_name)
    TextView mPostDotName;

    @Bind(R.id.post_vote_count)
    TextView mPostVoteCount;

    @Bind(R.id.post_views_count)
    TextView mPostViewsCount;

    @Bind(R.id.post_comments_count)
    TextView mPostCommentsCount;

    @Bind(R.id.post_date)
    TextView mPostDate;

    @Bind(R.id.fab)
    FloatingActionButton mFAB;

    @Bind(R.id.recyclerView)
    RecyclerView mRV;

    private String mPostId;
    private PostDetails mPostDetails;
    private PostDetailsRVAdapter mViewAdapter;
    private Menu mMenu;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == AppConstants.KV.REQUEST_EDIT_POST.getIntValue()) {
                if (data != null && data.getBooleanExtra("deleted", false)) {
                    this.finish();
                    return;
                }

                Intent refresh = new Intent(this, PostDetailActivity.class);
                refresh.putExtras(getIntent());
                startActivity(refresh);
                this.finish();
            } else if (requestCode == AppConstants.KV.REQUEST_ADD_COMMENT.getIntValue()) {
                Intent intent = getIntent();
                intent.putExtra("isScrollDown", true);
                Intent refresh = new Intent(this, PostDetailActivity.class);
                refresh.putExtras(intent);
                startActivity(refresh);
                this.finish();
            } else if (requestCode == AppConstants.KV.REQUEST_EDIT_COMMENT.getIntValue()) {
                ((MergeCursor) mViewAdapter.getCursor()).requery();
                mViewAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postdetail);
        ButterKnife.bind(this);

        // get caller data
        Bundle iArgs = getIntent().getExtras();
        mPostId = iArgs.getString(AppConstants.K.POST.name());

        // set title
        CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        toolbarLayout.setTitle(AppConstants.K.POST.name());

        // setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get the ActionBar here to configure the way it behaves.
        final ActionBar ab = getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.back_arrow); // set a custom icon for the default home button
        ab.setDisplayShowHomeEnabled(true); // show or hide the default home button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowCustomEnabled(false); // enable overriding the default toolbar layout
        ab.setDisplayShowTitleEnabled(false);


        // query for post details
        Cursor currPost = getContentResolver().query(SchemaPostDetails.CONTENT_URI, null, SchemaPostDetails.COLUMN_UID + "=?", new String[]{mPostId}, null);
        // query for post comments
        Cursor currComments = getContentResolver().query(SchemaComments.CONTENT_URI, null, SchemaComments.COLUMN_POST_DETAILS + "=?", new String[]{mPostId}, SchemaComments.COLUMN_CREATED_AT + " DESC");
        MergeCursor mergeCursor = new MergeCursor(new Cursor[]{currPost, currComments});

        // setup meta-data
        setUpMetadata();

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mViewAdapter = new PostDetailsRVAdapter(PostDetailActivity.this, mergeCursor);
        mViewAdapter.setHasStableIds(true);
        // recycler view setup
        mRV.setHasFixedSize(true);
        mRV.setLayoutManager(mLayoutManager);

        mRV.setAdapter(mViewAdapter);
        mRV.setItemAnimator(new DefaultItemAnimator());

        // prepare to refresh post details
        Bundle iServiceArgs = new Bundle();
        iServiceArgs.putString(KEY_UID.getValue(), mPostId);

        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TARGET
                Bundle args = new Bundle();
                args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_COMMENT.name());
                // data
                args.putString(AppConstants.K.POST.name(), mPostId);

                // intent
                Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
                intent.putExtras(args);
                startActivityForResult(intent, AppConstants.KV.REQUEST_ADD_COMMENT.getIntValue());
            }
        });

        // call for intent
        Intent detailsIntent =
                new Intent(getApplicationContext(), PostDetailsService.class);
        detailsIntent.putExtras(iArgs);
        startService(detailsIntent);
    }

    private void setUpMetadata() {
        Cursor currPost = getContentResolver().query(SchemaPostDetails.CONTENT_URI, null, SchemaPostDetails.COLUMN_UID + "=?", new String[]{mPostId}, null);
        try {
            if (!currPost.moveToFirst()) {
                return;
            }

            mPostDetails = Utils.cursorToPostDetailsEntity(currPost);
            if (mPostDetails == null) {
                return;
            }

            // post date
            mPostDate.setText(TimeUtils.toHumanRelativePeriod(mPostDetails.getCreatedAt()));

            // views count
            if (mPostDetails.getViews() == null) {
                mPostViewsCount.setText("-o-");
            } else {
                mPostViewsCount.setText(String.valueOf(mPostDetails.getViews()));
            }

            // vote count
            if (mPostDetails.getUpvoteCount() == null) {
                mPostVoteCount.setText("-o-");
            } else {
                mPostVoteCount.setText(String.valueOf(mPostDetails.getUpvoteCount()));
            }
            // comment count
            if (mPostDetails.getCommentCount() == null) {
                mPostCommentsCount.setText("-o-");
            } else {
                mPostCommentsCount.setText(String.valueOf(mPostDetails.getCommentCount()));
            }

            String ownerUid = null;
            int ownerUidIndex;
            if ((ownerUidIndex = currPost.getColumnIndex(SchemaPostDetails.COLUMN_OWNER)) != -1) {
                ownerUid = currPost.getString(ownerUidIndex);
            }
            // blank check
            if (StringUtils.isBlank(ownerUid)) {
                return;
            }
            // get user info
            Cursor currDot = getContentResolver().query(SchemaDots.CONTENT_URI, null, SchemaDots.COLUMN_UID + "=?", new String[]{ownerUid}, null);
            if (currDot.moveToFirst()) {
                final User user = Utils.cursorToUserEntity(currDot);

                String dotName = null;
                if (StringUtils.isBlank(user.getPrefName())) {
                    String fName = StringUtils.defaultString(user.getFname(), StringUtils.EMPTY);
                    String lName = StringUtils.defaultString(user.getLname(), StringUtils.EMPTY);
                    dotName = StringUtils.join(new String[]{fName, lName}, StringUtils.SPACE);
                } else {
                    dotName = user.getPrefName();
                }
                if (StringUtils.isBlank(dotName)) {
                    String pinchHandle = String.format(getApplicationContext().getString(R.string.pinch_handle), user.getPinchHandle());
                    dotName = pinchHandle;
                }

                mPostDotName.setText(dotName);
                mPostDotName.setOnClickListener(null);
                if (mPostDetails.getAnonymous() == null || !mPostDetails.getAnonymous()) {
                    // onclick take to dot view
                    mPostDotName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showPostUser(user.getUid());
                        }
                    });
                }
            }

            currDot.close();
        } finally {
            if (currPost != null && !currPost.isClosed()) {
                currPost.close();
            }
        }
    }

    public void showPostUser(String postUserId) {
        Bundle args = new Bundle();
        args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_DOTWALL.name());
        // data
        args.putString(AppConstants.K.KEY_DOT.name(), postUserId);

        // intent
        Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
        intent.putExtras(args);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;

            case R.id.menuitem_post_edit_id:
                doEditIt(item);
                return true;

            case R.id.menuitem_post_share_id:
                doShareIt(item);
                return true;

            case R.id.menuitem_post_menu_id:
                handleMenuSelection(item);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mMenu = menu;
        getMenuInflater().inflate(R.menu.post_detail_menu, menu);
        List<String> permissions = mPostDetails == null ? null : mPostDetails.getPermissions();
        if (permissions != null && permissions.contains("edit")) {
            // self
            menu.findItem(R.id.menuitem_post_edit_id).setVisible(Boolean.TRUE);
        }

        // open share dialog?
        try {
            if (getIntent().getExtras().getBoolean(AppConstants.K.DO_SHARE.name(), false)) {
                onOptionsItemSelected(mMenu.findItem(R.id.menuitem_post_share_id));
            }
        } catch (Exception e) {
            // muted
        }

        return true;
    }

    public void doShareIt(MenuItem item) {
        if (StringUtils.isBlank(mPostDetails.getUrl())) {
            //TODO may be ask user to file a bug?
            return;
        }

        try {

            final Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, mPostDetails.getUrl());
            shareIntent.setType("text/plain");
            IntentPickerSheetView intentPickerSheet = new IntentPickerSheetView(PostDetailActivity.this, shareIntent, "Share with...", new IntentPickerSheetView.OnIntentPickedListener() {
                @Override
                public void onIntentPicked(IntentPickerSheetView.ActivityInfo activityInfo) {
                    if(mBottomsheet.isSheetShowing()){
                        mBottomsheet.dismissSheet();
                    }
                    startActivity(activityInfo.getConcreteIntent(shareIntent));
                }
            });
            mBottomsheet.showWithSheetView(intentPickerSheet);
        } catch (Exception e) {
            // muted
        }
    }

    public void handleMenuSelection(MenuItem item) {
        // prepare menu options
        View menu = LayoutInflater.from(mBottomsheet.getContext()).inflate(R.layout.bs_menu, mBottomsheet, false);
        LinearLayout layout = (LinearLayout) menu.findViewById(R.id.layout_menu_list);
        if (mPostDetails.getUpvoted() != null && mPostDetails.getUpvoted()) {
            TextView downVoteView = (TextView) LayoutInflater.from(mBottomsheet.getContext()).inflate(R.layout.bs_raw_downvote, null);
            layout.addView(downVoteView);
            new MenuItemClickListener(downVoteView, "undoVoting", mPostId, mBottomsheet);
        } else {
            TextView voteView = (TextView) LayoutInflater.from(mBottomsheet.getContext()).inflate(R.layout.bs_raw_upvote, null);
            layout.addView(voteView);
            new MenuItemClickListener(voteView, "upvote", mPostId, mBottomsheet);
        }


        List<String> permissions = mPostDetails.getPermissions();
        if (permissions != null && permissions.contains("flag")) {
            TextView flagView = (TextView) LayoutInflater.from(mBottomsheet.getContext()).inflate(R.layout.bs_raw_flag, null);
            layout.addView(flagView);
            flagView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    new MaterialDialog.Builder(v.getContext())
                            .title(R.string.flag_title)
                            .items(R.array.flag_choice_arr)
                            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence reason) {
                                    Bundle body = new Bundle();
                                    body.putString("reason", String.valueOf(reason));
                                    new MenuItemClickListener(v, "flag", mPostId, mBottomsheet).callRemote(body);

                                    return true;
                                }
                            })
                            .negativeText(R.string.flag_btn_cancel)
                            .positiveText(R.string.flag_btn_choose)
                            .show();
                }
            });
        }
        mBottomsheet.showWithSheetView(menu);
    }


    private void doLogin() {
        Intent intent = new Intent(PostDetailActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    public void doEditIt(MenuItem item) {
        // TARGET
        Bundle args = new Bundle();
        args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_EDIT_POST.name());
        // data
        args.putString(AppConstants.K.POST.name(), mPostId);

        // intent
        Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
        intent.putExtras(args);
        startActivityForResult(intent, AppConstants.KV.REQUEST_EDIT_POST.getIntValue());
    }

    @Override
    public void onResume() {
        super.onResume();
        // register to event bus
        BusProvider.INSTANCE.getBus().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.INSTANCE.getBus().unregister(this);
    }

    @Subscribe
    public void onPostDetailsRefreshEvent(Events.PostDetailsRefreshEvent event) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // meta-data update
//                    Cursor currPost = getContentResolver().query(SchemaPostDetails.CONTENT_URI, null, SchemaPostDetails.COLUMN_UID + "=?", new String[]{mPostId}, null);
                    setUpMetadata();

                    ((MergeCursor) mViewAdapter.getCursor()).requery();
                    mViewAdapter.notifyDataSetChanged();
//                    currPost.close();
                    invalidateOptionsMenu();
                } catch (Exception e) {
                    // muted
                }
            }
        });
    }

    @Subscribe
    public void onPostDetailsRefreshFailEvent(final Events.PostDetailsRefreshFailEvent event) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (event != null && event.getMetaData() != null) {
                        Snackbar.make(mBottomsheet, event.getMetaData().get(AppConstants.K.MESSAGE.name()), Snackbar.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    // muted
                }
            }
        });
    }

    @Subscribe
    public void onPostMetaUpdateServiceSuccessEvent(final Events.PostMetaUpdateServiceSuccessEvent event) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (event != null && event.getMetaData() != null) {
                        Snackbar.make(mBottomsheet, event.getMetaData().get(AppConstants.K.MESSAGE.name()), Snackbar.LENGTH_SHORT).show();
                    }

                    setUpMetadata();
                    Bundle iArgs = getIntent().getExtras();
                    // call for intent
                    Intent detailsIntent =
                            new Intent(getApplicationContext(), PostDetailsService.class);
                    detailsIntent.putExtras(iArgs);
                    startService(detailsIntent);
                } catch (Exception e) {
                    // muted
                }
            }
        });
    }

    @Subscribe
    public void PostMetaUpdateServiceFailEvent(final Events.PostMetaUpdateServiceSuccessEvent event) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (event != null && event.getMetaData() != null) {
                        Snackbar.make(mBottomsheet, event.getMetaData().get(AppConstants.K.MESSAGE.name()), Snackbar.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    // muted
                }
            }
        });
    }

    @Subscribe
    public void onCommentDetailsRefreshEvent(final Events.CommentDetailsRefreshEvent event) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String eventMsg;
                    if (event.getMetaData() != null && (eventMsg = event.getMetaData().get(AppConstants.K.MESSAGE.name())) != null) {
                        Snackbar.make(mBottomsheet, eventMsg, Snackbar.LENGTH_SHORT).show();
                    }
                    ((MergeCursor) mViewAdapter.getCursor()).requery();
                    mViewAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    // muted
                }
            }
        });
    }


    @Subscribe
    public void onCommentDetailsEditEvent(final Events.CommentDetailsEditEvent event) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // TARGET
                    Bundle args = new Bundle();
                    args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_COMMENT.name());
                    // data
                    args.putString(AppConstants.K.POST.name(), mPostId);
                    args.putString(AppConstants.K.COMMENT.name(), event.getMetaData().get(AppConstants.K.COMMENT.name()));

                    // intent
                    Intent intent = new Intent(getApplicationContext(), ActivityFragment.class);
                    intent.putExtras(args);
                    startActivityForResult(intent, AppConstants.KV.REQUEST_EDIT_COMMENT.getIntValue());
                } catch (Exception e) {
                    // muted
                }
            }
        });
    }

    private class MenuItemClickListener implements View.OnClickListener {
        private final View view;
        private final String command;
        private final String postUID;
        private final BottomSheetLayout bottomSheet;

        public MenuItemClickListener(View source, String command, String postUID, BottomSheetLayout bs) {
            this.command = command;
            this.postUID = postUID;
            this.view = source;
            this.bottomSheet = bs;
            this.view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            callRemote(null);
        }

        public void callRemote(Bundle body) {
            bottomSheet.dismissSheet();
            if (Utils.isLoggedIn()) {
                Bundle iArgs = new Bundle();
                iArgs.putString(AppConstants.K.POST.name(), postUID);
                iArgs.putString(AppConstants.K.COMMAND.name(), command);
                if (body != null) {
                    iArgs.putBundle(AppConstants.K.BODY.name(), body);
                }
                // call for intent
                Intent intent =
                        new Intent(view.getContext(), PostMetaUpdateService.class);
                intent.putExtras(iArgs);
                view.getContext().startService(intent);
            } else {
                doLogin();
            }
        }
    }
}