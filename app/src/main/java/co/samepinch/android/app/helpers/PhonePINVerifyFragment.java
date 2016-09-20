package co.samepinch.android.app.helpers;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.apache.commons.lang3.StringUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.R;

public class PhonePINVerifyFragment extends Fragment {
    public static final String TAG = "PhonePINVerifyFragment";
    public static final String REQ_PHONE = "reqPhone";
    private static final String CTR_FORMAT = "%02d:%02d";
    @Bind(R.id.ip_phone_pin)
    TextView mPhonePINView;
    @Bind(R.id.btn_resend)
    TextView mResendView;
    @Bind(R.id.btn_resend_ctr)
    TextView mBtnResendCTRView;
    @Bind(R.id.resend_vs)
    ViewSwitcher mResendVSView;
    private LocalHandler mHandler;
    private ProgressDialog mProgressDialog;

    public static PhonePINVerifyFragment newInstance(String phone) {
        PhonePINVerifyFragment f = new PhonePINVerifyFragment();
        Bundle args = new Bundle();
        args.putSerializable(REQ_PHONE, phone);

        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new LocalHandler(this);
        // progress dialog properties
        mProgressDialog = new ProgressDialog(getActivity(),
                R.style.dialog);
        mProgressDialog.setCancelable(Boolean.FALSE);

        new CountDownTimer(120000, 1000) {
            public void onTick(long elapsed) {
                try {
                    String elapsedStr = String.format(CTR_FORMAT,
                            TimeUnit.MILLISECONDS.toMinutes(elapsed) - TimeUnit.HOURS.toMinutes(
                                    TimeUnit.MILLISECONDS.toHours(elapsed)),
                            TimeUnit.MILLISECONDS.toSeconds(elapsed) - TimeUnit.MINUTES.toSeconds(
                                    TimeUnit.MILLISECONDS.toMinutes(elapsed)));
                    mBtnResendCTRView.setText(elapsedStr);
                } catch (Exception e) {
                    // muted
                }

            }

            public void onFinish() {
                try {
                    mBtnResendCTRView.setText(R.string.empty);
                    mResendVSView.setDisplayedChild(1);
                } catch (Exception e) {
                    // muted
                }
            }
        }.start();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.phone_pin_verify, container, false);
        ButterKnife.bind(this, view);
        return view;
    }


    @OnClick(R.id.btn_later)
    public void onLaterEvent() {
        getActivity().finish();
    }


    @OnClick(R.id.btn_resend)
    public void onResendEvent() {
        getActivity().getSupportFragmentManager().popBackStackImmediate();
    }

    @OnClick(R.id.btn_next)
    public void onNextEvent() {
        String phonePIN = mPhonePINView.getText().toString();
        if (StringUtils.isBlank(phonePIN)) {
            mPhonePINView.setError(getString(R.string.reqd_login_info));
            return;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    private static final class LocalHandler extends Handler {
        private final WeakReference<PhonePINVerifyFragment> mActivity;

        public LocalHandler(PhonePINVerifyFragment parent) {
            mActivity = new WeakReference<PhonePINVerifyFragment>(parent);
        }
    }
}