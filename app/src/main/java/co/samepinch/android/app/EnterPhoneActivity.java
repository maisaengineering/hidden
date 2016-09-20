package co.samepinch.android.app;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import co.samepinch.android.app.helpers.PhoneVerifyFragment;

public class EnterPhoneActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_phone);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new PhoneVerifyFragment())
                    .commit();
        }

        ActionBar ab = getSupportActionBar();
        //ab.setDisplayShowTitleEnabled(Boolean.FALSE);
    }
}
