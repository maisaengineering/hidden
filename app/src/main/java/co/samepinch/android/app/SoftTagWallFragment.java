package co.samepinch.android.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.otto.Subscribe;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.adapters.EndlessRecyclerOnScrollListener;
import co.samepinch.android.app.helpers.adapters.PostCursorRecyclerViewAdapter;
import co.samepinch.android.app.helpers.intent.PostsPullService;
import co.samepinch.android.app.helpers.misc.SimpleDividerItemDecoration;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.data.dao.SchemaPosts;

import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_BY;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_KEY;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_POSTS_SEARCH;
import static co.samepinch.android.app.helpers.AppConstants.K;

public class SoftTagWallFragment extends Fragment {
    public static final String TAG = "SoftTagWallFragment";

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;
    RecyclerView.OnScrollListener mRecyclerViewScrollListener;
    private PostCursorRecyclerViewAdapter mViewAdapter;
    private LinearLayoutManager mLayoutManager;
    private LocalHandler mHandler;
    private Activity mActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new LocalHandler(this);
        // call fresh data
        callForRemotePosts(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // register to event bus
        BusProvider.INSTANCE.getBus().register(this);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                reQueryLocal();
                if (mLayoutManager.findFirstVisibleItemPosition() == 0) {
                    callForRemotePosts(Boolean.FALSE);
                }
            }
        });
    }

    private void reQueryLocal() {
        try {
            String tag = getArguments().getString(K.KEY_TAG.name());
            Cursor cursor = mActivity.getContentResolver().query(SchemaPosts.CONTENT_URI, null, SchemaPosts.COLUMN_SRC_SEARCH + "=?", new String[]{tag}, BaseColumns._ID + " DESC");
            if (cursor.getCount() > 0) {
                int beforeIdx = mLayoutManager.findFirstVisibleItemPosition();
                mViewAdapter.changeCursor(cursor);
                mViewAdapter.notifyDataSetChanged();
            } else {
                cursor.close();
            }
        } catch (Exception e) {
            // muted;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.INSTANCE.getBus().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.soft_tags_wall_view, container, false);
        ButterKnife.bind(this, view);

        ((AppCompatActivity) mActivity).setSupportActionBar(mToolbar);
        ((AppCompatActivity) mActivity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) mActivity).getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_arrow);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hack to get click working
                mActivity.onBackPressed();
            }
        });
        // tag name
        String tag = getArguments().getString(K.KEY_TAG.name());
        mToolbar.setTitle(tag);

        // recyclers
        mLayoutManager = new LinearLayoutManager(mActivity);
        setupRecyclerView();

        return view;
    }

    private void setupRecyclerView() {
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        String tag = getArguments().getString(K.KEY_TAG.name());
        Cursor cursor = mActivity.getContentResolver().query(SchemaPosts.CONTENT_URI, null, SchemaPosts.COLUMN_SRC_SEARCH + "=?", new String[]{tag}, BaseColumns._ID + " DESC");
        if (cursor.moveToFirst()) {
            mViewAdapter = new PostCursorRecyclerViewAdapter(mActivity, cursor);
        } else {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            mViewAdapter = new PostCursorRecyclerViewAdapter(mActivity, null);
        }

        mViewAdapter.setHasStableIds(Boolean.TRUE);
        mRecyclerView.setAdapter(mViewAdapter);

        // scroll listerer on recycle view
        mRecyclerViewScrollListener = new EndlessRecyclerOnScrollListener(mLayoutManager, AppConstants.KV.LOAD_MORE.getIntValue()) {
            @Override
            public void onLoadMore(RecyclerView rv, int current_page) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callForRemotePosts(Boolean.TRUE);
                    }
                });
            }

        };
        reInitializeScrollListener(mRecyclerView);

        // STYLE :: DIVIDER
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(mActivity));
    }

    private void callForRemotePosts(boolean isPaginating) {
        String tag = getArguments().getString(K.KEY_TAG.name());

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

//            Utils.PreferencesManager pref = Utils.PreferencesManager.getInstance();
//            Map<String, String> entries = pref.getValueAsMap(AppConstants.API.PREF_POSTS_LIST_USER.getValue());
//            for (Map.Entry<String, String> e : entries.entrySet()) {
//                iArgs.putString(e.getKey(), e.getValue());
//            }
        }

        // context
        iArgs.putString(KEY_BY.getValue(), KEY_POSTS_SEARCH.getValue());
        iArgs.putString(KEY_KEY.getValue(), tag);

        // call for intent
        Intent mServiceIntent =
                new Intent(mActivity, PostsPullService.class);
        mServiceIntent.putExtras(iArgs);
        mActivity.startService(mServiceIntent);
    }

    @Subscribe
    public void onPostsRefreshedEvent(final Events.PostsRefreshedEvent event) {
        Map<String, String> eMData = event.getMetaData();
        if ((eMData = event.getMetaData()) == null || !StringUtils.equalsIgnoreCase(eMData.get(KEY_BY.getValue()), KEY_POSTS_SEARCH.getValue())) {
            return;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String tag = getArguments().getString(K.KEY_TAG.name());
                    Cursor cursor = mActivity.getContentResolver().query(SchemaPosts.CONTENT_URI, null, SchemaPosts.COLUMN_SRC_SEARCH + "=?", new String[]{tag}, BaseColumns._ID + " DESC");
                    if (cursor.getCount() > 0) {
                        mViewAdapter.changeCursor(cursor);
                        mViewAdapter.notifyDataSetChanged();
                    } else {
                        cursor.close();
                    }
                    mViewAdapter.changeCursor(cursor);
                } catch (Exception e) {
                    // muted
                } finally {
                    resetUILoadingState();
                }
            }
        });
    }

    private void resetUILoadingState() {
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
        reInitializeScrollListener(mRecyclerView);
    }


    private void reInitializeScrollListener(RecyclerView rv) {
        try {
            // buggy code
            rv.removeOnScrollListener(mRecyclerViewScrollListener);
            mRecyclerViewScrollListener = new EndlessRecyclerOnScrollListener(mLayoutManager, AppConstants.KV.LOAD_MORE.getIntValue()) {
                @Override
                public void onLoadMore(RecyclerView rv, int current_page) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callForRemotePosts(Boolean.TRUE);
                        }
                    });
                }

            };
            rv.addOnScrollListener(mRecyclerViewScrollListener);
        } catch (Exception e) {
            // muted
//            e.printStackTrace();
        }

    }


    private static final class LocalHandler extends Handler {
        private final WeakReference<SoftTagWallFragment> mActivity;

        public LocalHandler(SoftTagWallFragment parent) {
            mActivity = new WeakReference<SoftTagWallFragment>(parent);
        }
    }
}