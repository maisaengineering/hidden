package co.samepinch.android.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.squareup.otto.Subscribe;

import co.samepinch.android.app.helpers.LoginStep1Fragment;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.intent.ParseSyncService;
import co.samepinch.android.app.helpers.intent.SignOutService;
import co.samepinch.android.app.helpers.pubsubs.Events;

import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_APP_ACCESS_STATE;

public class LoginActivity extends AppCompatActivity {
    public static final String TAG = "LoginActivity";
    ProgressDialog progressDialog;
    Animation mLoadingAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // local values
        mLoadingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_around_center_point_2);

//        ButterKnife.bind(LoginActivity.this);
//        BusProvider.INSTANCE.getBus().register(this);

//        progressDialog = new ProgressDialog(LoginActivity.this,
//                R.style.dialog);
//        progressDialog.setCancelable(Boolean.FALSE);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new LoginStep1Fragment())
                    .commit();
        }
    }

    public void startLoadingAnimation() {
        if(mLoadingAnim !=null){
            ((ImageView) this.findViewById(R.id.spicon)).startAnimation(mLoadingAnim);
        }
    }

    public void endLoadingAnimation() {
        ((ImageView) this.findViewById(R.id.spicon)).clearAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        BusProvider.INSTANCE.getBus().unregister(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == AppConstants.KV.REQUEST_SIGNUP.getIntValue()) {
//            if (resultCode == RESULT_OK) {
//                setResult(RESULT_OK);
//                finish();
//            }
//        }
//
//        if (RESULT_OK == resultCode) {
//            try {
//                Utils.clearDB(getContentResolver());
//                finish();
//            } catch (Exception e) {
//                // muted
//                // signout
//                Intent mServiceIntent =
//                        new Intent(this, SignOutService.class);
//                SPApplication.getContext().startService(mServiceIntent);
//            }
//        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Subscribe
    public void onAuthSuccessEvent(final Events.AuthSuccessEvent event) {
        Log.v(TAG, "onAuthSuccessEvent");

        // call for intent
        Intent intent =
                new Intent(SPApplication.getContext(), ParseSyncService.class);
        Bundle iArgs = new Bundle();
        iArgs.putInt(KEY_APP_ACCESS_STATE.getValue(), 1);
        intent.putExtras(iArgs);
        SPApplication.getContext().startService(intent);

        Log.v(TAG, "onAuthSuccessEvent");
        LoginActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Utils.clearDB(getContentResolver());
                    Utils.dismissSilently(progressDialog);
                    setResult(Activity.RESULT_OK);
                    finish();
                } catch (Exception e) {
                    // muted
                    // signout
                    Intent mServiceIntent =
                            new Intent(getApplicationContext(), SignOutService.class);
                    SPApplication.getContext().startService(mServiceIntent);
                }
            }
        });
    }

//    @Subscribe
//    public void onAuthFailEvent(final Events.AuthFailEvent event) {
//        final Map<String, String> eventData = event.getMetaData();
//        if (eventData == null) {
//            return;
//        }
//
//        if (StringUtils.equals(eventData.get(AppConstants.K.provider.name()), AppConstants.K.via_email_password.name())) {
//            LoginActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Utils.dismissSilently(progressDialog);
////                    mLoginButton.setEnabled(Boolean.TRUE);
//
//                    if (eventData.containsKey(AppConstants.K.MESSAGE.name())) {
//                        Snackbar.make(findViewById(R.id.login_layout), eventData.get(AppConstants.K.MESSAGE.name()), Snackbar.LENGTH_LONG).show();
//                    } else {
//                        Snackbar.make(findViewById(R.id.login_layout), "try again", Snackbar.LENGTH_SHORT).show();
//                    }
//                }
//            });
//        }
//    }
}
