package co.samepinch.android.app.helpers;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.LoginActivity;
import co.samepinch.android.app.R;
import co.samepinch.android.app.SPApplication;
import co.samepinch.android.data.dto.User;
import co.samepinch.android.rest.ReqGeneric;
import co.samepinch.android.rest.ReqLogin;
import co.samepinch.android.rest.Resp;
import co.samepinch.android.rest.RespLogin;
import co.samepinch.android.rest.RestClient;

public class LoginStep2Fragment extends Fragment {
    public static final String TAG = "LoginStep2Fragment";
    public static final String REQ_LOGIN = "reqLogin";

    @Bind(R.id.ip_password)
    TextView mPasswordView;

    @Bind(R.id.btn_next)
    TextView mBtnNextView;

    @Bind(R.id.btn_pass_reset)
    TextView mBtnPassResetView;

    private ProgressDialog mProgressDialog;
    private LocalHandler mHandler;

    public static LoginStep2Fragment newInstance(ReqLogin reqLogin) {
        LoginStep2Fragment f = new LoginStep2Fragment();
        Bundle args = new Bundle();
        args.putSerializable(REQ_LOGIN, reqLogin);

        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new LocalHandler(this);
        if (getActivity() instanceof LoginActivity) {
            ((LoginActivity) getActivity()).endLoadingAnimation();
        }


        // progress dialog properties
        mProgressDialog = new ProgressDialog(getActivity(),
                R.style.dialog);
        mProgressDialog.setCancelable(Boolean.FALSE);
    }

    @Override
    public void onResume() {
        super.onResume();

        // update holding activity graphic
        if (getActivity() instanceof LoginActivity) {
            try{
                ((LoginActivity) getActivity()).changeIcon(getResources().getDrawable(R.drawable.ic_lock_outline_black_100dp));
                ((LoginActivity) getActivity()).changeHint(null);

            }catch (Exception e){
                // muted
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_step2, container, false);
        ButterKnife.bind(this, view);

        ReqLogin reqLogin;
        try {
            reqLogin = (ReqLogin) getArguments().get(REQ_LOGIN);
        } catch (Exception e) {
            // muted
            reqLogin = null;
        }
        if (reqLogin == null) {
            onPrevEvent();
            return view;
        }

        if (Boolean.valueOf(reqLogin.isNewUser())) {
            mPasswordView.setHint(getString(R.string.hint_choose_password));
        } else {
            mPasswordView.setHint(getString(R.string.hint_enter_password));
        }

        return view;
    }

    @OnClick(R.id.btn_prev)
    public void onPrevEvent() {
        getActivity().getSupportFragmentManager().popBackStackImmediate();
    }

    @OnClick(R.id.btn_pass_reset)
    public void onPassReset() {
        mBtnPassResetView.setEnabled(Boolean.FALSE);
        ReqLogin reqLogin = (ReqLogin) getArguments().get(REQ_LOGIN);
        new ResetPassword().execute(reqLogin);
    }


    @OnClick(R.id.btn_next)
    public void onNextEvent() {
        String password = mPasswordView.getText().toString();
        if (StringUtils.isBlank(password)) {
            mPasswordView.setError(getString(R.string.reqd_login_info));
            return;
        }
        ReqLogin reqLogin = (ReqLogin) getArguments().get(REQ_LOGIN);
        reqLogin.setPassword(password);
        new AccCreateOrSignInTask().execute(reqLogin);
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

    private class AccCreateOrSignInTask extends AsyncTask<ReqLogin, Integer, User> {
        @Override
        protected User doInBackground(ReqLogin... params) {
            publishProgress(0);
            boolean isNewUser = Boolean.TRUE;
            try {
                ReqLogin reqLogin = params[0];

                ReqGeneric<Map<String, String>> req = new ReqGeneric<>();
                // set base args
                req.setToken(Utils.getAppToken(false));
                isNewUser = Boolean.valueOf(reqLogin.isNewUser());
                if (isNewUser) {
                    req.setCmd("create");
                } else {
                    req.setCmd("signIn");
                }
                // body
                Map<String, String> body = new HashMap<>();
                body.put("auth_key", reqLogin.getAuthKey());
                body.put("password", reqLogin.getPassword());
                body.put("country", reqLogin.getCountry());
                req.setBody(body);

                //headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(RestClient.INSTANCE.jsonMediaType());
                HttpEntity<ReqGeneric<Map<String, String>>> payloadEntity = new HttpEntity<>(req, headers);
                ResponseEntity<RespLogin> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.CHECK_USER.getValue(), HttpMethod.POST, payloadEntity, RespLogin.class);
                if (resp != null && resp.getBody() != null && resp.getBody().getBody() != null) {
                    if (isNewUser) {
                        publishProgress(1);
                    } else {
                        publishProgress(2);
                    }
                    return resp.getBody().getBody();
                }
            } catch (Exception e) {
                // muted
                Resp resp = Utils.parseAsRespSilently(e);
                if (resp != null && resp.getStatus() == 400) {
                    publishProgress(-2);
                    return null;
                }
            }

            publishProgress(-1);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Utils.dismissSilently(mProgressDialog);
            switch (values[0]) {
                case 0:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_creating_sing_in_wait));
                    break;
                case -1:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_creating_err));
                    break;
                case -2:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_signin_err));
                    break;
                case 1:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_creating_success));
                    break;
                case 2:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_signin_success));
                    break;
                default:
                    Utils.dismissSilently(mProgressDialog);
            }
        }

        @Override
        protected void onPostExecute(final User user) {
            if (user == null) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // get rid of any logins
                        Utils.PreferencesManager.getInstance().remove(AppConstants.API.PREF_AUTH_PROVIDER.getValue());
                        Utils.PreferencesManager.getInstance().remove(AppConstants.API.PREF_AUTH_USER.getValue());
                        Utils.dismissSilently(mProgressDialog);

                        // reset button
                        mBtnPassResetView.setEnabled(Boolean.TRUE);
                        ReqLogin reqLogin = (ReqLogin) getArguments().get(REQ_LOGIN);
                        if (!Boolean.valueOf(reqLogin.isNewUser())) {
                            mBtnPassResetView.setVisibility(View.VISIBLE);
                        }
                    }
                }, 1000);
            } else {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Utils.dismissSilently(mProgressDialog);
                        Gson gson = new Gson();
                        String userStr = gson.toJson(user);
                        Utils.PreferencesManager.getInstance().setValue(AppConstants.API.PREF_AUTH_PROVIDER.getValue(), AppConstants.K.via_email_password.name());
                        Utils.PreferencesManager.getInstance().setValue(AppConstants.API.PREF_AUTH_USER.getValue(), userStr);
//                    LoginStep1Fragment.
//                    getActivity().finish();
                        Fragment next;
                        if (StringUtils.isNotBlank(user.getPhno()) && user.getVerified() != null && !user.getVerified()) {
                            next = PhonePINVerifyFragment.newInstance(user.getPhno(), user.getCountry(), Boolean.FALSE);
                        } else {
                            getActivity().finish();
                            return;
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Transition exit = TransitionInflater.from(getContext()).inflateTransition(R.transition.slide_right);
                            Transition enter = TransitionInflater.from(getContext()).inflateTransition(R.transition.slide_left);
                            next.setSharedElementEnterTransition(enter);
                            next.setEnterTransition(enter);
                            setExitTransition(exit);
                            next.setSharedElementReturnTransition(exit);
                        }
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .addSharedElement(mBtnNextView, "btn_next")
                                .replace(R.id.container, next)
                                .addToBackStack(null)
                                .commit();
                    }
                }, 2000);
            }
        }
    }

    private class ResetPassword extends AsyncTask<ReqLogin, Integer, Resp> {
        @Override
        protected Resp doInBackground(ReqLogin... args0) {
            publishProgress(0);
            if (args0 == null || args0.length < 1) {
                return null;
            }

            try {
                ReqLogin reqLogin = args0[0];

                ReqGeneric<Map<String, String>> req = new ReqGeneric<>();
                // set base args
                req.setToken(Utils.getAppToken(false));
                req.setCmd("forgot_password");
                // body
                Map<String, String> body = new HashMap<>();
                body.put("auth_key", reqLogin.getAuthKey());
                body.put("country", reqLogin.getCountry());
                req.setBody(body);

                //headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(RestClient.INSTANCE.jsonMediaType());
                HttpEntity<ReqGeneric<Map<String, String>>> payloadEntity = new HttpEntity<>(req, headers);
                ResponseEntity<Resp> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.CHECK_USER.getValue(), HttpMethod.POST, payloadEntity, Resp.class);
                return resp.getBody();
            } catch (Exception e) {
                // muted
            }

            publishProgress(-1);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Utils.dismissSilently(mProgressDialog);
            switch (values[0]) {
                case 0:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_pass_reset_wait));
                    break;
                case -1:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_pass_general_err));
                    break;
                default:
                    Utils.dismissSilently(mProgressDialog);
            }
        }

        @Override
        protected void onPostExecute(final Resp aResp) {
            if (aResp != null && aResp.getStatus() == 200) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Utils.dismissSilently(mProgressDialog);
                        Toast.makeText(SPApplication.getContext(), aResp.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }, 1);
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Utils.dismissSilently(mProgressDialog);
                    if (aResp != null && aResp.getStatus() == 200) {
                        mBtnPassResetView.setVisibility(View.GONE);
                    } else {
                        mBtnPassResetView.setEnabled(Boolean.TRUE);
                        mBtnPassResetView.setVisibility(View.VISIBLE);
                    }
                }
            }, 2000);
        }
    }

}