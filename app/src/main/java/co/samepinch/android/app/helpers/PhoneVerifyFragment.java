package co.samepinch.android.app.helpers;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.R;
import co.samepinch.android.data.dto.CountryVO;
import co.samepinch.android.rest.ReqLogin;

public class PhoneVerifyFragment extends Fragment {
    public static final String TAG = "PhoneVerifyFragment";
    public static final String REQ_LOGIN = "reqLogin";
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

    @Bind(R.id.ip_phone)
    TextView mPhoneView;

    @Bind(R.id.list_country)
    Spinner mCountryListView;

    @Bind(R.id.btn_next)
    TextView mBtnNextView;

    public static PhoneVerifyFragment newInstance(ReqLogin reqLogin) {
        PhoneVerifyFragment f = new PhoneVerifyFragment();
        Bundle args = new Bundle();
        args.putSerializable(REQ_LOGIN, reqLogin);

        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.phone_verify, container, false);
        ButterKnife.bind(this, view);

        // country list view
        fillCountryListView();

        return view;
    }

    private void fillCountryListView() {
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService(getContext().TELEPHONY_SERVICE);
        String userCountryCode = StringUtils.upperCase(tm.getNetworkCountryIso());
        String preSelection = null;
        try {
            String countryPhonePrefix;
            for (CountryVO countryVO : Utils.countryList()) {
                countryPhonePrefix = countryVO.getPhonePrefix();
                mCountriesView2VOMap.put(countryVO.getPhonePrefix(), countryVO);
                // track entry
                if (StringUtils.equals(userCountryCode, countryVO.getCode())) {
                    preSelection = countryPhonePrefix;
                }
            }
        } catch (Exception e) {
            // muted
        }

        List<String> _countries = new ArrayList<>(mCountriesView2VOMap.keySet());
        // spinner stuff
        ArrayAdapter<String> mCountryListAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.phoneverify_prefix, _countries);
        mCountryListView.setAdapter(mCountryListAdapter);
        // default setup
        if (StringUtils.isNotBlank(preSelection)) {
            int preSelectionIdx = Collections.binarySearch(_countries, preSelection);
            mCountryListView.setSelection(preSelectionIdx);
        }
    }

    @OnClick(R.id.btn_next)
    public void onNextEvent() {
        String phone = mPhoneView.getText().toString();
        if (StringUtils.isBlank(phone)) {
            mPhoneView.setError(getString(R.string.reqd_login_info));
            return;
        }
        PhonePINVerifyFragment next = PhonePINVerifyFragment.newInstance(phone);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}