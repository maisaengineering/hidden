package co.samepinch.android.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

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
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new LoginStep1Fragment())
                    .commit();
        }
    }

    public void startLoadingAnimation() {
        if (mLoadingAnim != null) {
            try {
                ((ImageView) this.findViewById(R.id.spicon)).startAnimation(mLoadingAnim);
            } catch (Exception e) {
                // muted
            }
        }
    }

    public void endLoadingAnimation() {
        try {
            ((ImageView) this.findViewById(R.id.spicon)).clearAnimation();
        } catch (Exception e) {
            // muted
        }
    }

    public void changeIcon(Drawable aDrawable) {
        try {
            ((ImageView) this.findViewById(R.id.spicon)).setImageDrawable(aDrawable);
        } catch (Exception e) {
            // muted
        }
    }


    public void changeHint(String aHint) {
        try {
            TextView _hintView = ((TextView) this.findViewById(R.id.sphint));
            if(aHint == null){
                _hintView.setVisibility(View.GONE);
            }else{
                _hintView.setVisibility(View.VISIBLE);
                _hintView.setText(aHint);
            }

        } catch (Exception e) {
            // muted
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        BusProvider.INSTANCE.getBus().unregister(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
}
