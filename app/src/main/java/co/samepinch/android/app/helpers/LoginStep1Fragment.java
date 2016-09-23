package co.samepinch.android.app.helpers;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.LoginActivity;
import co.samepinch.android.app.R;
import co.samepinch.android.app.SPApplication;
import co.samepinch.android.data.dto.CountryVO;
import co.samepinch.android.rest.ReqGeneric;
import co.samepinch.android.rest.ReqLogin;
import co.samepinch.android.rest.RespEvaluateUser;
import co.samepinch.android.rest.RestClient;

public class LoginStep1Fragment extends Fragment {
    public static final String TAG = "LoginStep1Fragment";
    private static Map<String, CountryVO> mCountriesView2VOMap;

    static {
        mCountriesView2VOMap = new TreeMap<>(new Comparator() {
            @Override
            public int compare(Object lhs, Object rhs) {
                if (lhs == null || rhs == null) {
                    return 0;
                }
                return lhs.toString().compareToIgnoreCase(rhs.toString());
            }
        });
    }

    @Bind(R.id.ip_phoneoremail)
    EditText mPhoneOrEmailView;
    @Bind(R.id.btn_next)
    TextView mBtnNextView;
    @Bind(R.id.country_selection)
    LinearLayout mCountrySelectionLayout;
    @Bind(R.id.list_country)
    Spinner mCountryListView;
    AccCheckerTask mAccCheckerTask;
    ProgressDialog mProgressDialog;
    private LocalHandler mHandler;
    private ArrayAdapter<String> mCountryListAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check task
        mAccCheckerTask = new AccCheckerTask();

        // progress dialog properties
        mProgressDialog = new ProgressDialog(getActivity(),
                R.style.dialog);
        mProgressDialog.setCancelable(Boolean.TRUE);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                try {
                    if (mAccCheckerTask != null) {
                        mAccCheckerTask.cancel(Boolean.TRUE);
                    }
                } catch (Exception e) {
                    // muted
                }
            }
        });

        // handler
        mHandler = new LocalHandler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_step1, container, false);
        ButterKnife.bind(this, view);

        new DisplayCountryTask().execute();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // update holding activity graphic
        if (getActivity() instanceof LoginActivity) {
            try{
                ((LoginActivity) getActivity()).changeIcon(getResources().getDrawable(R.drawable.icon));
                ((LoginActivity) getActivity()).changeHint(getResources().getString(R.string.must_login));
            }catch(Exception e){
                // muted
            }

        }
    }

    @OnClick(R.id.btn_next)
    public void onNextEvent(View btnView) {
        String emailOrPhone = mPhoneOrEmailView.getText().toString();
        if (StringUtils.isBlank(emailOrPhone)) {
            mPhoneOrEmailView.setError(getString(R.string.reqd_login_info));
            return;
        }

        mAccCheckerTask = new AccCheckerTask();
        CountryVO _selectedCountry = mCountriesView2VOMap.get(mCountryListView.getSelectedItem());


        String lookupInfo = emailOrPhone;
        if (StringUtils.isNotBlank(lookupInfo) && StringUtils.isNumericSpace(lookupInfo) && !lookupInfo.startsWith("+")) {
            lookupInfo = String.format("%s %s", _selectedCountry.getPhonePrefix(), lookupInfo);
        }
        Utils.showDialog(mProgressDialog, String.format(getString(R.string.dialog_acc_locating), lookupInfo));

        ReqLogin loginForm = new ReqLogin();
        loginForm.setAuthKey(emailOrPhone);
        loginForm.setCountry(_selectedCountry == null ? null : _selectedCountry.getCode());

        mAccCheckerTask.execute(loginForm);
    }

    private void continueToNext(ReqLogin reqLogin) {
        if (getActivity() instanceof LoginActivity) {
            ((LoginActivity) getActivity()).startLoadingAnimation();
        }

        LoginStep2Fragment step2 = LoginStep2Fragment.newInstance(reqLogin);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Transition exit = TransitionInflater.from(getContext()).inflateTransition(R.transition.slide_right);
            Transition enter = TransitionInflater.from(getContext()).inflateTransition(R.transition.slide_left);
            step2.setSharedElementEnterTransition(enter);
            step2.setEnterTransition(enter);
            setExitTransition(exit);
            step2.setSharedElementReturnTransition(exit);
        }
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .addSharedElement(mBtnNextView, "btn_next")
                .replace(R.id.container, step2)
                .addToBackStack(null)
                .commit();
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

    /**
     * Account Checking task
     */
    private class AccCheckerTask extends AsyncTask<ReqLogin, Integer, ReqLogin> {
        @Override
        protected ReqLogin doInBackground(ReqLogin... reqLogin) {
            //publishProgress(2);
            try {
                ReqGeneric<Map<String, String>> req = new ReqGeneric<>();
                // set base args
                req.setToken(Utils.getNonBlankAppToken());
                req.setCmd("evaluateUser");
                // just consider single post for now
                Map<String, String> body = new HashMap<>();
                body.put("auth_key", reqLogin[0].getAuthKey());
                body.put("country", reqLogin[0].getCountry());
                req.setBody(body);

                //headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(RestClient.INSTANCE.jsonMediaType());
                HttpEntity<ReqGeneric<Map<String, String>>> payloadEntity = new HttpEntity<>(req, headers);
                ResponseEntity<RespEvaluateUser> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.CHECK_USER.getValue(), HttpMethod.POST, payloadEntity, RespEvaluateUser.class);
                if (resp != null && resp.getBody() != null) {
                    reqLogin[0].setIsNewUser(Boolean.toString(resp.getBody().getBody().isNewUser()));
                    return reqLogin[0];
                }
            } catch (Exception e) {
                // muted
            }
            publishProgress(-1);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            switch (values[0]) {
                case 0:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_notfound));
                    break;
                case 1:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_found));
                    break;
                case -1:
                    Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_lookuperr));
                    break;
                default:
                    Utils.dismissSilently(mProgressDialog);
            }
        }

        @Override
        protected void onPostExecute(final ReqLogin reqLogin) {
            Utils.dismissSilently(mProgressDialog);
            if (reqLogin == null) {
                return;
            }
            // new or old?
            if (Boolean.valueOf(reqLogin.isNewUser())) {
                Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_notfound));
            } else {
                Utils.showDialog(mProgressDialog, getString(R.string.dialog_acc_found));
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Utils.dismissSilently(mProgressDialog);
                    continueToNext(reqLogin);
                }
            }, 2000);

        }
    }

    /**
     * Country pull-up task
     */
    private class DisplayCountryTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            TelephonyManager tm = (TelephonyManager) getContext().getSystemService(getContext().TELEPHONY_SERVICE);
            String userCountryCode = StringUtils.upperCase(tm.getNetworkCountryIso());
            String preSelection = null;
            try {
                String countryName;
                for (CountryVO countryVO : Utils.countryList()) {
                    countryName = countryVO.getName().split("(?<=\\G.{19})")[0];
                    countryName = String.format("%s (%s)", countryName.split(",")[0], StringUtils.defaultIfBlank(countryVO.getCode(), "--"));
                    mCountriesView2VOMap.put(countryName, countryVO);
                    // track entry
                    if (StringUtils.equals(userCountryCode, countryVO.getCode())) {
                        preSelection = countryName;
                    }
                }
            } catch (Exception e) {
                // muted
            }

            List<String> _countries = new ArrayList<>(mCountriesView2VOMap.keySet());
            // spinner stuff
            mCountryListAdapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.spinner_text, _countries);
            mCountryListAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown);
            mCountryListView.setAdapter(mCountryListAdapter);
            // default setup
            if (StringUtils.isNotBlank(preSelection)) {
                int preSelectionIdx = Collections.binarySearch(_countries, preSelection);
                mCountryListView.setSelection(preSelectionIdx);
            }
            // animation stuff
            Animation animFadeIn = AnimationUtils.loadAnimation(SPApplication.getContext(), R.anim.fade_in);
            mCountrySelectionLayout.setAnimation(animFadeIn);
            mCountrySelectionLayout.setVisibility(View.VISIBLE);
        }
    }
}