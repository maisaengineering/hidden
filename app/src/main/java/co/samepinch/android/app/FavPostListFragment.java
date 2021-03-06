package co.samepinch.android.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import co.samepinch.android.app.helpers.misc.FragmentLifecycle;
import co.samepinch.android.app.helpers.misc.SimpleDividerItemDecoration;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.data.dao.SchemaPosts;

import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_BY;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_POSTS_FAV;

public class FavPostListFragment extends Fragment implements FragmentLifecycle {
    public static final String TAG = "FavPostListFragment";
    public static final String ARG_PAGE = "ARG_PAGE";

    @Bind(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mRefreshLayout;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    PostCursorRecyclerViewAdapter mViewAdapter;
    LinearLayoutManager mLayoutManager;

    private LocalHandler mHandler;

    public static FavPostListFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);

        FavPostListFragment fragment = new FavPostListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new LocalHandler(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // register to event bus
        BusProvider.INSTANCE.getBus().register(this);

        // refresh
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mViewAdapter != null) {
                    Cursor cursor = getActivity().getContentResolver().query(SchemaPosts.CONTENT_URI, null, SchemaPosts.COLUMN_SRC_FAV + "=?", new String[]{"1"}, BaseColumns._ID  + " ASC");
                    if (cursor.getCount() > 0) {
                        mViewAdapter.changeCursor(cursor);
                    } else {
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                        callForRemotePosts(false);
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.INSTANCE.getBus().unregister(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.posts_wall, container, false);
        ButterKnife.bind(this, view);

        // clear session data
        Utils.PreferencesManager.getInstance().remove(AppConstants.API.PREF_POSTS_LIST_FAV.getValue());

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(mLayoutManager, AppConstants.KV.LOAD_MORE.getIntValue()) {
            @Override
            public void onLoadMore(RecyclerView rv, int current_page) {
                callForRemotePosts(Boolean.TRUE);
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                callForRemotePosts(false);
            }
        });
        setupRecyclerView();

        // refresh
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callForRemotePosts(false);
            }
        });

        return view;
    }

    private void setupRecyclerView() {
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        Cursor cursor = getActivity().getContentResolver().query(SchemaPosts.CONTENT_URI, null, SchemaPosts.COLUMN_SRC_FAV + "=?", new String[]{"1"}, BaseColumns._ID  + " ASC");
        mViewAdapter = new PostCursorRecyclerViewAdapter(getActivity(), cursor);
        mViewAdapter.setHasStableIds(Boolean.TRUE);
        mRecyclerView.setAdapter(mViewAdapter);

        // STYLE :: DIVIDER
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
    }

    private void callForRemotePosts(boolean isPaginating) {
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
            Map<String, String> entries = pref.getValueAsMap(AppConstants.API.PREF_POSTS_LIST_FAV.getValue());
            for (Map.Entry<String, String> e : entries.entrySet()) {
                iArgs.putString(e.getKey(), e.getValue());
            }
        } else {
            iArgs.putBoolean(AppConstants.KV.LOAD_MORE.getKey(), Boolean.TRUE);
        }

        // context
        iArgs.putString(KEY_BY.getValue(), KEY_POSTS_FAV.getValue());
        
        // call for intent
        Intent mServiceIntent =
                new Intent(getActivity(), PostsPullService.class);
        mServiceIntent.putExtras(iArgs);
        getActivity().startService(mServiceIntent);
    }

    @Subscribe
    public void onPostsRefreshedEvent(final Events.PostsRefreshedEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> eMData = event.getMetaData();
                    if ((eMData = event.getMetaData()) == null || !StringUtils.equalsIgnoreCase(eMData.get(KEY_BY.getValue()), KEY_POSTS_FAV.getValue())) {
                        return;
                    }

                    Utils.PreferencesManager pref = Utils.PreferencesManager.getInstance();
                    pref.setValue(AppConstants.API.PREF_POSTS_LIST_FAV.getValue(), event.getMetaData());

                    // refresh complete view
                    Cursor cursor = getActivity().getContentResolver().query(SchemaPosts.CONTENT_URI, null, SchemaPosts.COLUMN_SRC_FAV + "=?", new String[]{"1"}, BaseColumns._ID  + " ASC");
                    mViewAdapter.changeCursor(cursor);
                } catch (Exception e) {
                    // muted
                }finally {
                    if (mRefreshLayout.isRefreshing()) {
                        mRefreshLayout.setRefreshing(false);
                        mRecyclerView.clearOnScrollListeners();
                        mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(mLayoutManager, AppConstants.KV.LOAD_MORE.getIntValue()) {
                            @Override
                            public void onLoadMore(RecyclerView rv, int current_page) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        callForRemotePosts(true);
                                    }
                                });
                            }
                        });
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

    @Override
    public void onPauseFragment() {
        if (mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onResumeFragment() {
    }

    @Override
    public void onRefreshFragment() {
    }


    private static final class LocalHandler extends Handler {
        private final WeakReference<FavPostListFragment> mActivity;

        public LocalHandler(FavPostListFragment parent) {
            mActivity = new WeakReference<FavPostListFragment>(parent);
        }
    }
}
