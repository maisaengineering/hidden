package co.samepinch.android.app.helpers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.samepinch.android.app.R;

public class Web2AppActivityLauncher extends InternalActivityLauncher {
    public static final String TAG = "Web2AppActivityLauncher";
    public static final int LAUNCH_TARGET_ACTIVITY = 109;
    private static final Pattern PTRN_QUERY_PARAM = Pattern.compile("type=(.*?)&uid=(.*?)($|&)");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty);

        try {
            Intent intent = getIntent();
            Uri data = intent.getData();

            Matcher qMatcher = PTRN_QUERY_PARAM.matcher(data.toString());
            String actionType = null, uid = null;
            if (qMatcher.find()) {
                actionType = qMatcher.group(1);
                uid = qMatcher.group(2);
            }
            launch(actionType, uid);
        } catch (Exception e) {
            Intent intent = new Intent(Web2AppActivityLauncher.this, RootActivity.class);
            startActivity(intent);
        }
    }

    @Override
    int getActivityId() {
        return LAUNCH_TARGET_ACTIVITY;
    }
}