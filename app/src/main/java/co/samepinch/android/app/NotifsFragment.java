package co.samepinch.android.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.NotifsActivityLauncher;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.adapters.EndlessRecyclerOnScrollListener;
import co.samepinch.android.app.helpers.adapters.NotifsRVAdapter;
import co.samepinch.android.app.helpers.intent.AllNotificationsService;
import co.samepinch.android.app.helpers.misc.FragmentLifecycle;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.data.dao.SchemaNotifications;

public class NotifsFragment extends Fragment implements FragmentLifecycle {
    public static final String TAG = "NotifsFragment";
    public static final String ARG_PAGE = "ARG_PAGE";
    static ActionBar.LayoutParams LP_AB_MATCH_CTR = new ActionBar.LayoutParams(android.app.ActionBar.LayoutParams.MATCH_PARENT, android.app.ActionBar.LayoutParams.MATCH_PARENT, Gravity.CENTER);

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mRefreshLayout;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @Bind(R.id.notifs_view_switcher)
    ViewSwitcher mNotifsVS;

    LinearLayoutManager mLayoutManager;
    NotifsRVAdapter mViewAdapter;

    private LocalHandler mHandler;

    public static NotifsFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        NotifsFragment fragment = new NotifsFragment();
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
                    Cursor cursor = getActivity().getContentResolver().query(SchemaNotifications.CONTENT_URI, null, null, null, BaseColumns._ID + " ASC");
                    if (cursor.getCount() > 0) {
                        int beforeIdx = mLayoutManager.findFirstVisibleItemPosition();
                        mViewAdapter.changeCursor(cursor);
                        try {
                            int afterIdx = mLayoutManager.findFirstVisibleItemPosition();
                            int total = mLayoutManager.getItemCount();
                            if (beforeIdx <= total && beforeIdx != afterIdx) {
                                mLayoutManager.scrollToPosition(beforeIdx);
                            }
                        } catch (Exception e) {
                            // muted
                        }

                    } else {
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                        callForRemoteNotifs(Boolean.FALSE);
                    }
                }
            }
        });

        mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(mLayoutManager, 5) {
            @Override
            public void onLoadMore(RecyclerView rv, int current_page) {
                callForRemoteNotifs(Boolean.TRUE);
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
        View view = inflater.inflate(R.layout.notifs_wall, container, false);
        ButterKnife.bind(this, view);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);

        ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
        ab.setDisplayShowCustomEnabled(true);

        ab.setDisplayHomeAsUpEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        View headerView = LayoutInflater.from(SPApplication.getContext()).inflate(R.layout.notifs_header, null);
        ab.setCustomView(headerView, LP_AB_MATCH_CTR);

        headerView.findViewById(R.id.nav_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hack to get click working
                getActivity().onBackPressed();
            }
        });

        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                callForRemoteNotifs(Boolean.FALSE);
            }
        });

        setupRecyclerView();

        // refresh data
        Utils.PreferencesManager.getInstance().remove(AppConstants.API.PREF_NOTIFS_METADATA.getValue());
        callForRemoteNotifs(Boolean.FALSE);

        return view;
    }

    private void setupRecyclerView() {
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        Cursor cursor = getActivity().getContentResolver().query(SchemaNotifications.CONTENT_URI, null, null, null, BaseColumns._ID + " ASC");
        if (cursor.moveToFirst()) {
            mNotifsVS.setDisplayedChild(0);
            mViewAdapter = new NotifsRVAdapter(getActivity(), cursor);
        } else {
            mViewAdapter = new NotifsRVAdapter(getActivity(), null);
            if (cursor != null && cursor.getColumnCount() == 0) {
                mNotifsVS.setDisplayedChild(1);
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        }

        mViewAdapter.setHasStableIds(Boolean.TRUE);
        mRecyclerView.setAdapter(mViewAdapter);
    }

    public void callForRemoteNotifs(boolean isPaginating) {
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
            iArgs.putBoolean(AppConstants.KV.LOAD_MORE.getKey(), Boolean.TRUE);
        } else {
            mRecyclerView.setTag("");
        }

        // refresh user notifications
        Intent intentNotifs =
                new Intent(SPApplication.getContext(), AllNotificationsService.class);
        intentNotifs.putExtras(iArgs);
        SPApplication.getContext().startService(intentNotifs);
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

    @Subscribe
    public void onAllNotifsRefreshedEvent(final Events.AllNotifsRefreshedEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Cursor cursor = getActivity().getContentResolver().query(SchemaNotifications.CONTENT_URI, null, null, null, BaseColumns._ID + " ASC");
                    if (cursor.moveToFirst()) {
                        mNotifsVS.setDisplayedChild(0);

                        mViewAdapter.changeCursor(cursor);
                        mRecyclerView.invalidate();
                    } else {
                        if (cursor != null && cursor.getCount() == 0) {
                            mNotifsVS.setDisplayedChild(1);
                            if (!cursor.isClosed()) {
                                cursor.close();
                            }
                        }
                    }
                } catch (Exception e) {
                    // muted
                } finally {
                    mRefreshLayout.setRefreshing(false);
                    mRecyclerView.setTag("");
                }
            }
        });
    }

    @Subscribe
    public void onAllNotifsUPDATEEvent(final Events.AllNotifsUPDATEEvent event) {
        this.onAllNotifsRefreshedEvent(null);
    }

    @Subscribe
    public void onAllNotifsERROREvent(final Events.AllNotifsERROREvent event) {
        mRefreshLayout.setRefreshing(false);
        mRecyclerView.setTag("");
    }

    @Subscribe
    public void onSingleNotifsUPDATEEvent(final Events.SingleNotifsUPDATEEvent event) {
        Map<String, String> eventData = null;
        if (event == null || (eventData = event.getMetaData()) == null) {
            return;
        }
        String src = eventData.get(AppConstants.K.SRC.name());
        String srcID = eventData.get(AppConstants.K.SRC_ID.name());

        // target activity
        Bundle iArgs = new Bundle();
        iArgs.putString(AppConstants.K.ACTION_TYPE.name(), src);
        iArgs.putString(AppConstants.K.ID.name(), srcID);
        Intent actionIntent =
                new Intent(getContext(), NotifsActivityLauncher.class);
        actionIntent.putExtras(iArgs);
        getContext().startActivity(actionIntent);
    }

    private static final class LocalHandler extends Handler {
        private final WeakReference<NotifsFragment> mActivity;

        public LocalHandler(NotifsFragment parent) {
            mActivity = new WeakReference<NotifsFragment>(parent);
        }
    }
}
