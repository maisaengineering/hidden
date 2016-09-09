package co.samepinch.android.app.helpers.intent;

import android.app.IntentService;
import android.content.Intent;

import org.springframework.http.ResponseEntity;

import java.util.Map;

import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.rest.RestClient;

public class IPCheckerService extends IntentService {
    public static final String TAG = "IPCheckerService";

    public IPCheckerService() {
        super("IPCheckerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            ResponseEntity<Map> resp = RestClient.INSTANCE.handle().getForEntity(AppConstants.API.IPCHECK_HOST.getValue(), Map.class);
            BusProvider.INSTANCE.getBus().post(new Events.IPCheckerServiceSuccess((Map<String, String>) resp));
        } catch (Exception e) {
            BusProvider.INSTANCE.getBus().post(new Events.IPCheckerServiceErr(null));
        }
    }
}
