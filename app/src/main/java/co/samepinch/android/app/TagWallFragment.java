package co.samepinch.android.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.squareup.otto.Subscribe;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.adapters.EndlessRecyclerOnScrollListener;
import co.samepinch.android.app.helpers.adapters.PostCursorRecyclerViewAdapter;
import co.samepinch.android.app.helpers.intent.PostsPullService;
import co.samepinch.android.app.helpers.intent.TagDetailsService;
import co.samepinch.android.app.helpers.intent.TagsPullService;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.data.dao.SchemaPosts;
import co.samepinch.android.data.dao.SchemaTags;

import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_BY;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_KEY;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_NAME;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_POSTS_TAG;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_TAGS_PULL_FAV;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_TAGS_PULL_TYPE;
import static co.samepinch.android.app.helpers.AppConstants.K;

public class TagWallFragment extends Fragment {
    public static final String TAG = "TagWallFragment";

    @Bind(R.id.fab)
    FloatingActionButton mFab;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.collapsing_toolbar)
    CollapsingToolbarLayout mToolbarLayout;

    @Bind(R.id.backdrop)
    SimpleDraweeView mBackdrop;

    @Bind(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mRefreshLayout;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @Bind(R.id.tag_wall_name)
    TextView mTagName;

    @Bind(R.id.tag_wall_followers_count)
    TextView mTagFollowersCnt;

    @Bind(R.id.tag_wall_posts_count)
    TextView mTagPostsCnt;

    private PostCursorRecyclerViewAdapter mViewAdapter;
    private LinearLayoutManager mLayoutManager;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.KV.REQUEST_EDIT_TAG.getIntValue()) {
            if (resultCode == Activity.RESULT_OK) {
                callForRemoteTagData();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.tags_wall_view, container, false);
        ButterKnife.bind(this, view);

        // clear session data
        Utils.PreferencesManager.getInstance().remove(AppConstants.API.PREF_POSTS_LIST_TAG.getValue());

        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hack to get click working
                getActivity().onBackPressed();
            }
        });
        mToolbarLayout.setExpandedTitleTextAppearance(R.style.TransparentText);

        // tag name
        String tag = getArguments().getString(K.KEY_TAG.name());
        mToolbarLayout.setTitle(tag);
        mTagName.setText(tag);

        // recyclers
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(mLayoutManager, 5) {
            @Override
            public void onLoadMore(RecyclerView rv, int current_page) {
                callForRemotePosts(true);
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                callForRemotePosts(false);
            }
        });

        Cursor cursor = getActivity().getContentResolver().query(SchemaTags.CONTENT_URI, null, SchemaTags.COLUMN_NAME + "=?", new String[]{tag}, null);
        if(cursor.getCount() == 0){
            ContentValues values = new ContentValues();
            values.put(SchemaTags.COLUMN_NAME, tag);
            getActivity().getContentResolver().insert(SchemaTags.CONTENT_URI, values);
        }
        if(cursor !=null && !cursor.isClosed()){
            cursor.close();
        }

        setUpMetaData();
        setupRecyclerView();

        // refresh
        callForRemoteTagData();
        callForRemotePosts(false);

        // call for intent
        Intent tagRefreshIntent =
                new Intent(getActivity().getApplicationContext(), TagsPullService.class);
        tagRefreshIntent.putExtra(KEY_TAGS_PULL_TYPE.getValue(), KEY_TAGS_PULL_FAV.getValue());
        getActivity().startService(tagRefreshIntent);

        return view;
    }

    private void setUpMetaData() {
        String tag = getArguments().getString(K.KEY_TAG.name());
        Cursor cursor = null;
        try {
            cursor = getActivity().getContentResolver().query(SchemaTags.CONTENT_URI, null, SchemaTags.COLUMN_NAME + "=?", new String[]{tag}, null);
            if (!cursor.moveToFirst()) {
                return;
            }

            // followers
            int followersCntIdx = cursor.getColumnIndex(SchemaTags.COLUMN_FOLLOWERS_COUNT);
            String followersCnt = followersCntIdx > -1 ? cursor.getString(followersCntIdx) : null;
            mTagFollowersCnt.setText(followersCnt);

            // followers
            int postsCntIdx = cursor.getColumnIndex(SchemaTags.COLUMN_POSTS_COUNT);
            String postsCnt = postsCntIdx > -1 ? cursor.getString(postsCntIdx) : null;
            mTagPostsCnt.setText(postsCnt);


            // image
            int imgIdx = cursor.getColumnIndex(SchemaTags.COLUMN_IMAGE);
            String imgStr = imgIdx > -1 ? cursor.getString(imgIdx) : null;
            if (StringUtils.isNotBlank(imgStr)) {
                Utils.setupLoadingImageHolder(mBackdrop, imgStr);
            }

            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Utils.isLoggedIn()) {
                        // params
                        String tag = getArguments().getString(K.KEY_TAG.name());
                        Map<String, String> userInfo = Utils.PreferencesManager.getInstance().getValueAsMap(AppConstants.API.PREF_AUTH_USER.getValue());
                        String currUserId = userInfo.get(AppConstants.APP_INTENT.KEY_UID.getValue());

                        Bundle args = new Bundle();
                        args.putString(AppConstants.APP_INTENT.KEY_TAG.getValue(), tag);
                        args.putString(AppConstants.APP_INTENT.KEY_UID.getValue(), currUserId);
                        // target
                        args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_MANAGE_A_TAG.name());

                        // intent
                        Intent intent = new Intent(getActivity().getApplicationContext(), ActivityFragment.class);
                        intent.putExtras(args);
                        startActivityForResult(intent, AppConstants.KV.REQUEST_EDIT_TAG.getIntValue());
                    } else {
                        doLogin();
                    }
                }
            });
        } catch (Exception e) {
            // muted
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void doLogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void setupRecyclerView() {
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        String tag = getArguments().getString(K.KEY_TAG.name());
        Cursor cursor = getActivity().getContentResolver().query(SchemaPosts.CONTENT_URI, null, "tags LIKE ?", new String[]{"%" + tag + "%"}, null);
        mViewAdapter = new PostCursorRecyclerViewAdapter(getActivity(), cursor);
        mRecyclerView.setAdapter(mViewAdapter);
    }

    private void callForRemoteTagData() {
        final String tag = getArguments().getString(K.KEY_TAG.name());

        // construct context from preferences if any?
        Bundle iArgs = new Bundle();
        iArgs.putString(KEY_NAME.getValue(), tag);

        // call for intent
        Intent intent =
                new Intent(getActivity(), TagDetailsService.class);
        intent.putExtras(iArgs);
        getActivity().startService(intent);
    }

    private void callForRemotePosts(boolean isPaginating) {
        final String tag = getArguments().getString(K.KEY_TAG.name());

        // construct context from preferences if any?
        Bundle iArgs = new Bundle();
        if (isPaginating) {
            Object _state = mRecyclerView.getTag();
            // prevent unnecessary traffic
            if (_state != null && (_state instanceof Utils.State)) {
                if (((Utils.State) _state).isPendingLoadMore()) {
                    return;
                }
            }

            Utils.State state = new Utils.State();
            state.setPendingLoadMore(true);
            mRecyclerView.setTag(state);

            Utils.PreferencesManager pref = Utils.PreferencesManager.getInstance();
            Map<String, String> entries = pref.getValueAsMap(AppConstants.API.PREF_POSTS_LIST_TAG.getValue());
            for (Map.Entry<String, String> e : entries.entrySet()) {
                iArgs.putString(e.getKey(), e.getValue().toString());
            }
        }

        // context
        iArgs.putString(KEY_BY.getValue(), KEY_POSTS_TAG.getValue());
        iArgs.putString(KEY_KEY.getValue(), tag.replaceFirst("#", ""));

        // call for intent
        Intent mServiceIntent =
                new Intent(getActivity(), PostsPullService.class);
        mServiceIntent.putExtras(iArgs);
        getActivity().startService(mServiceIntent);
    }


    @Subscribe
    public void onTagRefreshedEvent(Events.TagRefreshedEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUpMetaData();
            }
        });
    }

    @Subscribe
    public void onPostsRefreshedEvent(final Events.PostsRefreshedEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> eMData = event.getMetaData();
                    if ((eMData = event.getMetaData()) == null || !StringUtils.equalsIgnoreCase(eMData.get(KEY_BY.getValue()), KEY_POSTS_TAG.getValue())) {
                        return;
                    }

                    Utils.PreferencesManager pref = Utils.PreferencesManager.getInstance();
                    pref.setValue(AppConstants.API.PREF_POSTS_LIST_TAG.getValue(), event.getMetaData());

                    String tag = getArguments().getString(K.KEY_TAG.name());
                    Cursor cursor = getActivity().getContentResolver().query(SchemaPosts.CONTENT_URI, null, "tags LIKE ?", new String[]{"%" + tag + "%"}, null);
                    mViewAdapter.changeCursor(cursor);
                } catch (Exception e) {
                    // muted
                }finally {
                    if (mRefreshLayout.isRefreshing()) {
                        mRefreshLayout.setRefreshing(false);
                    }

                    Object _state = mRecyclerView.getTag();
                    // prevent unnecessary traffic
                    if (_state != null && (_state instanceof Utils.State)) {
                        ((Utils.State) _state).setPendingLoadMore(false);
                    } else {
                        Utils.State state = new Utils.State();
                        state.setPendingLoadMore(false);
                        _state = state;
                    }
                    mRecyclerView.setTag(_state);
                }
            }
        });
    }

    @Subscribe
    public void onTagsRefreshedEvent(Events.TagsRefreshedEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
//                    Cursor cursor = getActivity().getContentResolver().query(SchemaTags.CONTENT_URI, null, null, null, SchemaTags.COLUMN_NAME + " ASC");
//                    mTagsToManageRVAdapter.changeCursor(cursor);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        });
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
        if (mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(false);
        }
    }
}