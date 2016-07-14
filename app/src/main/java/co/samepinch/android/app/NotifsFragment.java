package co.samepinch.android.app;

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

import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.samepinch.android.app.helpers.adapters.NotifsRVAdapter;
import co.samepinch.android.app.helpers.misc.FragmentLifecycle;
import co.samepinch.android.app.helpers.misc.SimpleDividerItemDecoration;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
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
                        //callForRemotePosts(false);
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
        View view = inflater.inflate(R.layout.notifs_wall, container, false);
        ButterKnife.bind(this, view);

        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);

        ActionBar ab = ((AppCompatActivity) getActivity()).getSupportActionBar();
        ab.setDisplayShowCustomEnabled(true);

//        ab.setHomeButtonEnabled(false);
//        ab.setHomeAsUpIndicator(R.drawable.back_arrow);
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        View headerView = LayoutInflater.from(SPApplication.getContext()).inflate(R.layout.notifs_header, null);
        ab.setCustomView(headerView, LP_AB_MATCH_CTR);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hack to get click working
                getActivity().onBackPressed();
            }
        });

        mLayoutManager = new LinearLayoutManager(getActivity());
        setupRecyclerView();

        return view;
    }

    private void setupRecyclerView() {
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        Cursor cursor = getActivity().getContentResolver().query(SchemaNotifications.CONTENT_URI, null, null, null, BaseColumns._ID + " ASC");
        if(cursor.moveToFirst()){
            mViewAdapter = new NotifsRVAdapter(getActivity(), cursor);
        }else{
            if(cursor !=null && !cursor.isClosed()){
                cursor.close();
            }
            mViewAdapter = new NotifsRVAdapter(getActivity(), null);
        }

        mViewAdapter.setHasStableIds(Boolean.TRUE);
        mRecyclerView.setAdapter(mViewAdapter);

        // STYLE :: DIVIDER
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
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
        private final WeakReference<NotifsFragment> mActivity;

        public LocalHandler(NotifsFragment parent) {
            mActivity = new WeakReference<NotifsFragment>(parent);
        }
    }
}
