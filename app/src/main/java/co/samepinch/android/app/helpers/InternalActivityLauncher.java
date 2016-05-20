package co.samepinch.android.app.helpers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import co.samepinch.android.app.ActivityFragment;
import co.samepinch.android.app.PostDetailActivity;

/**
 * Created by imaginationcoder on 5/20/16.
 */
public abstract class InternalActivityLauncher extends AppCompatActivity {
    public static final String TYPE_DOT = "FOLLOWER";
    public static final String TYPE_ADD_POST = "ADDPOST";
    public static final String TYPE_POST = "POST";
    public static final String TYPE_GROUP = "GROUP";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_VOTE = "VOTE";
    public static final String TYPE_SHARE = "SHARE";
    public static final String TYPE_ADMIN = "ADMIN";
    public static final String TYPE_ROOT = "";

    public void launch(String actionType, String uid) {
        Intent intent;
        try {
            // target activity args
            Bundle iArgs = new Bundle();
            Class<?> targetActivity = null;
            switch (actionType.toUpperCase()) {
                case TYPE_DOT:
                    iArgs.putString(AppConstants.K.KEY_DOT.name(), uid);
                    iArgs.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_DOTWALL.name());
                    targetActivity = ActivityFragment.class;
                    break;

                case TYPE_POST:
                case TYPE_COMMENT:
                case TYPE_VOTE:
                    iArgs.putString(AppConstants.K.POST.name(), uid);
                    targetActivity = PostDetailActivity.class;
                    break;

                case TYPE_ADD_POST:
                    if (Utils.isLoggedIn()) {
                        iArgs.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_CREATE_POST.name());
                        targetActivity = ActivityFragment.class;
                    } else {
                        targetActivity = RootActivity.class;
                    }
                    break;

                case TYPE_SHARE:
                    iArgs.putString(AppConstants.K.POST.name(), uid);
                    iArgs.putBoolean(AppConstants.K.DO_SHARE.name(), true);
                    targetActivity = PostDetailActivity.class;
                    break;

                case TYPE_GROUP:
                    iArgs.putString(AppConstants.K.KEY_TAG.name(), uid);
                    iArgs.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_TAGWALL.name());
                    targetActivity = ActivityFragment.class;
                    break;

                case TYPE_ADMIN:
                    // store preferences
                    Utils.PreferencesManager.getInstance().setValue(AppConstants.APP_INTENT.KEY_ADMIN_COMMAND.getValue(), uid);
                    targetActivity = RootActivity.class;
                    break;
                default:
                    targetActivity = RootActivity.class;
                    break;
            }

            intent = new Intent(getApplicationContext(), targetActivity);
            intent.putExtras(iArgs);
            startActivityForResult(intent, getActivityId());
        } catch (Exception e) {
            Intent eIntent = new Intent(this, RootActivity.class);
            startActivity(eIntent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == getActivityId()) {
            Intent intent = new Intent(this, RootActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    abstract int getActivityId();
}
