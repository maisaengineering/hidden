package co.samepinch.android.app.helpers;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.LoginActivity;
import co.samepinch.android.app.R;

public class LoginStep2Fragment extends Fragment {
    public static final String TAG = "LoginStep1Fragment";


    private LocalHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new LocalHandler(this);

        if(getActivity() instanceof LoginActivity){
            ((LoginActivity) getActivity()).endLoadingAnimation();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_step2, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @OnClick(R.id.btn_prev)
    public void onPrevEvent() {
        getActivity().getSupportFragmentManager().popBackStackImmediate();
    }

    @OnClick(R.id.btn_next)
    public void onNextEvent() {
        Log.v(TAG, "next clicked...");

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private static final class LocalHandler extends Handler {
        private final WeakReference<LoginStep2Fragment> mActivity;

        public LocalHandler(LoginStep2Fragment parent) {
            mActivity = new WeakReference<LoginStep2Fragment>(parent);
        }
    }
//
//    private void handleError(String errMsg) {
//        Snackbar.make(mImgContainer, errMsg, Snackbar.LENGTH_SHORT).show();
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                getActivity().finish();
//            }
//        }, 999);
//    }

}