package co.samepinch.android.app.helpers.intent;

import android.app.IntentService;
import android.content.Intent;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.rest.ReqSetBody;
import co.samepinch.android.rest.Resp;
import co.samepinch.android.rest.RestClient;

/**
 * Created by imaginationcoder on 6/26/15.
 */
public class SignOutService extends IntentService {
    public static final String LOG_TAG = "SignOutService";

    public SignOutService() {
        super("SignOutService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Map<String, String> eventData = new HashMap<>();
        eventData.put(AppConstants.K.provider.name(), AppConstants.API.PREF_AUTH_PROVIDER.getValue());

        // local logout first
        logOutLocally();

        // remote logout
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(RestClient.INSTANCE.jsonMediaType());
        try {
            ReqSetBody logoutReq = new ReqSetBody();
            logoutReq.setToken(Utils.getNonBlankAppToken());

            logoutReq.setCmd("signOut");
            Map<String, String> body = new HashMap<>();
            body.put(AppConstants.KV.PLATFORM.getKey(), AppConstants.KV.PLATFORM.getValue());
            logoutReq.setBody(body);

            HttpEntity<ReqSetBody> payloadEntity;

            // call remote
            payloadEntity = new HttpEntity<>(logoutReq, headers);
            ResponseEntity<Resp> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.USERS.getValue(), HttpMethod.POST, payloadEntity, Resp.class);
            BusProvider.INSTANCE.getBus().post(new Events.AuthOutEvent(eventData));
        } catch (Exception e) {
            Resp resp = Utils.parseAsRespSilently(e);
            if (resp != null) {
                eventData.put(AppConstants.K.MESSAGE.name(), resp.getMessage());
            }

            BusProvider.INSTANCE.getBus().post(new Events.AuthOutFailEvent(eventData));
        }

        // clear pref state
        Utils.PreferencesManager.getInstance().remove(AppConstants.API.PREF_AUTH_PROVIDER.getValue());
        Utils.PreferencesManager.getInstance().remove(AppConstants.API.PREF_AUTH_USER.getValue());
        Utils.PreferencesManager.getInstance().remove(AppConstants.API.ACCESS_TOKEN.getValue());
    }

    public void logOutLocally() {
        String currProvider = Utils.PreferencesManager.getInstance().getValue(AppConstants.API.PREF_AUTH_PROVIDER.getValue());
    }
}
