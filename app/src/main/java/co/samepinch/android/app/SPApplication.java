package co.samepinch.android.app;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.facebook.FacebookSdk;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.karumi.dexter.Dexter;
import com.parse.Parse;
import com.parse.ParseInstallation;

import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by cbenjaram on 8/6/15.
 */
public class SPApplication extends MultiDexApplication {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Dexter.initialize(this);

        // others
        Utils.PreferencesManager.initializeInstance(mContext);
        Fresco.initialize(mContext);
        FacebookSdk.sdkInitialize(mContext);

        // parse hash
        Parse.initialize(this, AppConstants.API.PARSE_APPLICATION_ID.getValue(), AppConstants.API.PARSE_CLIENT_KEY.getValue());
        ParseInstallation.getCurrentInstallation().saveInBackground();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
    }

    public static Context getContext() {
        return mContext;
    }
}
