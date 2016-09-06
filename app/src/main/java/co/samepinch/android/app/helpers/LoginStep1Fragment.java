package co.samepinch.android.app.helpers;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.R;

public class LoginStep1Fragment extends Fragment {
    public static final String TAG = "LoginStep1Fragment";

    @Bind(R.id.image_container)
    FrameLayout mImgContainer;

    private LocalHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new LocalHandler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.imageview, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @OnClick(R.id.close)
    public void onCloseEvent() {
        getActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private static final class LocalHandler extends Handler {
        private final WeakReference<LoginStep1Fragment> mActivity;

        public LocalHandler(LoginStep1Fragment parent) {
            mActivity = new WeakReference<LoginStep1Fragment>(parent);
        }
    }

    private void handleError(String errMsg) {
        Snackbar.make(mImgContainer, errMsg, Snackbar.LENGTH_SHORT).show();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getActivity().finish();
            }
        }, 999);
    }

}