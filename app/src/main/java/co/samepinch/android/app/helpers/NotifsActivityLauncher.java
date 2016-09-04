package co.samepinch.android.app.helpers;

import android.content.Intent;
import android.os.Bundle;

import java.util.Locale;

import co.samepinch.android.app.R;

public class NotifsActivityLauncher extends InternalActivityLauncher {
    // Group|Post|Comment|Follower|Vote
    public static final int LAUNCH_TARGET_ACTIVITY = 208;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty);
        try {
            // get caller data
            Intent intent = getIntent();
            Bundle input = intent.getExtras();
            String actionType = input.getString(AppConstants.K.ACTION_TYPE.name(), "");
            actionType = actionType.toUpperCase(Locale.getDefault());

            String uid = input.getString(AppConstants.K.ID.name());
            launch(actionType, uid);
        } catch (Exception e) {
            Intent intent = new Intent(NotifsActivityLauncher.this, RootActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent output = new Intent();
        setResult(RESULT_OK, output);
        finish();
    }

    @Override
    int getActivityId() {
        return LAUNCH_TARGET_ACTIVITY;
    }
}