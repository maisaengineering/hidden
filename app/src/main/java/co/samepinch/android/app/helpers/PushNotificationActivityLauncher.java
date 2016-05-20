package co.samepinch.android.app.helpers;

import android.content.Intent;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

import co.samepinch.android.app.R;
import co.samepinch.android.app.helpers.misc.SPParsePushBroadcastReceiver;
import co.samepinch.android.data.dto.PushNotification;

public class PushNotificationActivityLauncher extends InternalActivityLauncher {
    // Group|Post|Comment|Follower|Vote
    public static final int LAUNCH_TARGET_ACTIVITY = 108;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty);
        try {
            // get caller data
            PushNotification notification = SPParsePushBroadcastReceiver.getAppPushNotification(getIntent());
            PushNotification.Context notificationContext = notification == null ? null : notification.getContext();
            // empty check
            if (notification == null || notificationContext == null) {
                Intent intent = new Intent(PushNotificationActivityLauncher.this, RootActivity.class);
                startActivity(intent);
            }

            // grab target action
            String actionType = StringUtils.defaultString(notificationContext.getType(), TYPE_ROOT);
            actionType = actionType.toUpperCase(Locale.getDefault());

            // args uid
            String uid = notification.getContext().getUid();
            launch(actionType, uid);
        } catch (Exception e) {
            Intent intent = new Intent(PushNotificationActivityLauncher.this, RootActivity.class);
            startActivity(intent);
        }
    }

    @Override
    int getActivityId() {
        return LAUNCH_TARGET_ACTIVITY;
    }
}