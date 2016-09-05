package co.samepinch.android.app.helpers.intent;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.data.dao.SchemaNotifications;
import co.samepinch.android.rest.ReqSetBody;
import co.samepinch.android.rest.Resp;
import co.samepinch.android.rest.RespAllNotifs;
import co.samepinch.android.rest.RestClient;

import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_UID;

public class AllNotificationsService extends IntentService {
    public static final String TAG = "AllNotificationsService";
    private static String notification_count, last_modified, etag;

    public AllNotificationsService() {
        super("AllNotificationsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Map<String, String> eventBody = new HashMap<>();
        try {
            Map<String, String> reqBody = new HashMap<>();
//            reqBody.put("last_modified", "");
            Map<String, String> prefParams = Utils.PreferencesManager.getInstance().getValueAsMap(AppConstants.API.PREF_NOTIFS_METADATA.getValue());
            if (prefParams == null || prefParams.isEmpty()) {
                reqBody.put(PARAMS.STEP.getValue(), PARAMS.STEP_DFLT.getValue());
            } else {
                reqBody.put(PARAMS.STEP.getValue(), PARAMS.STEP_NEXT.getValue());
                reqBody.putAll(prefParams);
            }

            ReqSetBody req = new ReqSetBody();
            req.setToken(Utils.getAppToken(false));
            req.setCmd(PARAMS.COMMAND.getValue());
            req.setBody(reqBody);
            if (!Utils.isLoggedIn()) {
                // UNIQUE ID FOR THIS INSTALL
                reqBody.put(PARAMS.REF.getValue(), Utils.uniqueID());
            }

            //headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(RestClient.INSTANCE.jsonMediaType());

            HttpEntity<ReqSetBody> payloadEntity = new HttpEntity<>(req, headers);
            ResponseEntity<RespAllNotifs> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.NOTIFICATIONS.getValue(), HttpMethod.POST, payloadEntity, RespAllNotifs.class);
            ArrayList<ContentProviderOperation> ops = parseResponse(resp.getBody());

            // if new, clear local
            if (reqBody.get(PARAMS.STEP.getValue()) == PARAMS.STEP_DFLT.getValue()) {
                ArrayList<ContentProviderOperation> delOps = new ArrayList<>();
                delOps.add(ContentProviderOperation.newDelete(SchemaNotifications.CONTENT_URI).build());
                getContentResolver().applyBatch(AppConstants.API.CONTENT_AUTHORITY.getValue(), delOps);
            }
            if (ops != null && ops.size() > 0) {
                ArrayList<ContentProviderOperation> dbOps = new ArrayList<>();
                dbOps.addAll(ops);
                getContentResolver().applyBatch(AppConstants.API.CONTENT_AUTHORITY.getValue(), dbOps);
            }

            eventBody.put(PARAMS.NOTIFS_COUNT.getValue(), resp.getBody().getBody().getNotifsCount());
            eventBody.put(PARAMS.LAST_MODIFIED.getValue(), resp.getBody().getBody().getLastModified());
            eventBody.put(PARAMS.ETAG.getValue(), resp.getBody().getBody().getEtag());
            Utils.PreferencesManager.getInstance().setValue(AppConstants.API.PREF_NOTIFS_METADATA.getValue(), eventBody);
            BusProvider.INSTANCE.getBus().post(new Events.AllNotifsUPDATEEvent(eventBody));
        } catch (Exception e) {
            // muted
            Resp resp = Utils.parseAsRespSilently(e);
            Map<String, String> eventData = new HashMap<>();
            String msg = resp == null || resp.getMessage() == null ? AppConstants.APP_INTENT.KEY_MSG_GENERIC_ERR.getValue() : resp.getMessage();
            eventData.put(AppConstants.K.MESSAGE.name(), msg);
            BusProvider.INSTANCE.getBus().post(new Events.AllNotifsERROREvent(eventBody));
        } finally {
            BusProvider.INSTANCE.getBus().post(new Events.AllNotifsRefreshedEvent(eventBody));
        }
    }

    @NonNull
    private ArrayList<ContentProviderOperation> parseResponse(RespAllNotifs respData) {
        if (respData == null || respData.getBody() == null || respData.getBody().getNotifications() == null) {
            return null;
        }

        Map<String, String> userInfo = Utils.PreferencesManager.getInstance().getValueAsMap(AppConstants.API.PREF_AUTH_USER.getValue());
        String currUserId = userInfo.get(KEY_UID.getValue());

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (RespAllNotifs.Notification notification : respData.getBody().getNotifications()) {
            ops.add(ContentProviderOperation.newInsert(SchemaNotifications.CONTENT_URI)
                    .withValue(SchemaNotifications.COLUMN_UID, notification.getUid())
                    .withValue(SchemaNotifications.COLUMN_MSG, notification.getMessage())
                    .withValue(SchemaNotifications.COLUMN_SRC, notification.getSource())
                    .withValue(SchemaNotifications.COLUMN_SRC_ID, notification.getSourceId())
                    .withValue(SchemaNotifications.COLUMN_VIEWED, notification.getViewed())
                    .build());
        }

        return ops;
    }

    enum PARAMS {
        NOTIFS_COUNT("notification_count"),
        LAST_MODIFIED("last_modified"),
        ETAG("etag"),
        STEP_DFLT("new"),
        STEP_NEXT("next"),
        STEP("step"),
        COMMAND("all"),
        REF("ref");

        final String value;

        PARAMS(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
