package co.samepinch.android.app.helpers.intent;

import android.animation.TypeConverter;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.AccessToken;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Arrays;

import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.module.DaggerStorageComponent;
import co.samepinch.android.app.helpers.module.StorageComponent;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.rest.ReqLogin;
import co.samepinch.android.rest.RespLogin;
import co.samepinch.android.rest.RestClient;

import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_EMAIL;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_PASSWORD;

/**
 * Created by imaginationcoder on 6/26/15.
 */
public class FBAuthService extends IntentService {
    public static final String LOG_TAG = "AuthService";

    public FBAuthService() {
        super("AuthService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        StorageComponent component = DaggerStorageComponent.create();
        ReqLogin loginReq = component.provideReqLogin();

        //headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        try {
            JSONObject userObj = new JSONObject(intent.getStringExtra("user"));
            String uid = userObj.getString("id");
            String fName = userObj.getString("first_name");
            String lName = userObj.getString("last_name");
            String email = userObj.getString("email");

            String name = userObj.getString("email");
            String pinchHandle = StringUtils.strip(name, "");


            Bundle iArgs = intent.getExtras();
            // set base args
            loginReq.setToken(Utils.getAppToken(false));
            loginReq.setCmd("signIn");
            // set context args
            loginReq.setEmail(iArgs.getString("user"));


            HttpEntity<ReqLogin> payloadEntity;
            ResponseEntity<RespLogin> resp = null;
            try {
                // call remote
                payloadEntity = new HttpEntity<>(loginReq.build(), headers);
                resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.USERS.getValue(), HttpMethod.POST, payloadEntity, RespLogin.class);

            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    // try resetting token?
                    loginReq.setToken(Utils.getAppToken(true));
                    payloadEntity = new HttpEntity<>(loginReq.build(), headers);
                    resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.POSTS.getValue(), HttpMethod.POST, payloadEntity, RespLogin.class);
                } else {
                    throw new IllegalStateException("un-known response code.", e);
                }
            }

            Utils.PreferencesManager.getInstance().setValue(AppConstants.API.PREF_AUTH_USER.getValue(), resp.getBody().getBody());
            BusProvider.INSTANCE.getBus().post(new Events.AuthSuccessEvent(null));
        } catch (Exception e) {
//            e.printStackTrace();
            // get rid of auth session
            Utils.PreferencesManager.getInstance().remove(AppConstants.API.PREF_AUTH_USER.getValue());
            BusProvider.INSTANCE.getBus().post(new Events.AuthFailEvent(null));
        }
    }
}
