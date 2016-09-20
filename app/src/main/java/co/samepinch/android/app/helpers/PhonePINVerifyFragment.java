package co.samepinch.android.app.helpers;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.R;
import co.samepinch.android.app.SPApplication;
import co.samepinch.android.rest.ReqGeneric;
import co.samepinch.android.rest.Resp;
import co.samepinch.android.rest.RestClient;

public class PhonePINVerifyFragment extends Fragment {
    public static final String TAG = "PhonePINVerifyFragment";
    public static final String REQ_PHONE = "reqPhone";
    public static final String REQ_COUNTRY_CODE = "reqCountryCode";
    public static final String CTR_FORMAT = "%02d:%02d";

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

    public static PhonePINVerifyFragment newInstance(String phone, String countryCOde) {
        PhonePINVerifyFragment f = new PhonePINVerifyFragment();
        Bundle args = new Bundle();
        args.putString(REQ_PHONE, phone);
        args.putString(REQ_COUNTRY_CODE, countryCOde);

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
        mHandler = new LocalHandler(this);

        // 2minutes timer
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

        Bundle args = getArguments();
        new ConfirmPhoneNumberTask().execute(args.getString(REQ_PHONE, StringUtils.EMPTY), args.getString(REQ_COUNTRY_CODE, StringUtils.EMPTY));
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
        } else {
            new VerifyPINTask().execute(phonePIN);
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

    private class VerifyPINTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... args0) {
            try {
                ReqGeneric<Map<String, String>> req = new ReqGeneric<>();
                // set base args
                req.setToken(Utils.getNonBlankAppToken());
                req.setCmd("verifyAccount");

                // body
                Map<String, String> body = new HashMap<>();
                body.put("code", args0[0]);
                req.setBody(body);

                //headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(RestClient.INSTANCE.jsonMediaType());
                HttpEntity<ReqGeneric<Map<String, String>>> payloadEntity = new HttpEntity<>(req, headers);
                ResponseEntity<Resp> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.CHECK_USER.getValue(), HttpMethod.POST, payloadEntity, Resp.class);
                return resp.getBody().getStatus() == 200;
            } catch (Exception e) {
                // muted
                return null;
            }
        }

        @Override
        protected void onPostExecute(Boolean status) {
            if (status == null || !status.booleanValue()) {
                Toast.makeText(getContext(), SPApplication.getContext().getText(R.string.phoneverify_general_err), Toast.LENGTH_SHORT).show();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPhonePINView.setText(StringUtils.EMPTY);
                    }
                }, 2000);
            } else {
                Toast.makeText(getContext(), SPApplication.getContext().getText(R.string.phoneverify_seccess), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ConfirmPhoneNumberTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... args0) {
            try {
                ReqGeneric<Map<String, String>> req = new ReqGeneric<>();
                // set base args
                req.setToken(Utils.getNonBlankAppToken());
                req.setCmd("confirmPhno");

                // body
                Map<String, String> body = new HashMap<>();
                body.put("phno", args0[0]);
                body.put("country", args0[1]);
                req.setBody(body);

                //headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(RestClient.INSTANCE.jsonMediaType());
                HttpEntity<ReqGeneric<Map<String, String>>> payloadEntity = new HttpEntity<>(req, headers);
                ResponseEntity<Resp> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.CHECK_USER.getValue(), HttpMethod.POST, payloadEntity, Resp.class);
                return resp.getBody().getStatus() == 200;
            } catch (Exception e) {
                // muted
            }

            return null;
        }

        @Override
        protected void onPostExecute(Boolean status) {
            if (status == null || !status.booleanValue()) {
                Toast.makeText(getContext(), SPApplication.getContext().getText(R.string.phoneverify_general_err), Toast.LENGTH_SHORT).show();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().getSupportFragmentManager().popBackStackImmediate();
                    }
                }, 2000);
            }
        }
    }
}