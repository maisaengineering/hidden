package co.samepinch.android.app.helpers.intent;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.os.Bundle;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

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
import co.samepinch.android.rest.RestClient;

import static co.samepinch.android.app.helpers.AppConstants.API.NOTIFICATIONS;

public class AllNotificationsUpdateService extends IntentService {
    public static final String TAG = "AllNotificationsUpdateService";
    private static String notification_count, last_modified, etag;

    public AllNotificationsUpdateService() {
        super("AllNotificationsUpdateService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Map<String, String> eventBody = new HashMap<>();
        try {
            // get caller data
            Bundle iArgs = intent.getExtras();
            // src
            String src = iArgs.getString(AppConstants.K.SRC.name());
            eventBody.put(AppConstants.K.SRC.name(), src);

            // srcID
            String srcID = iArgs.getString(AppConstants.K.SRC_ID.name());
            eventBody.put(AppConstants.K.SRC_ID.name(), srcID);

            String notifUID = iArgs.getString(AppConstants.K.NOTIF.name());
            eventBody.put(AppConstants.K.NOTIF.name(), notifUID);

            if (StringUtils.isNotBlank(notifUID)) {
                // LOCAL UPDATE
                ArrayList<ContentProviderOperation> delOps = new ArrayList<>();
                delOps.add(ContentProviderOperation.newDelete(SchemaNotifications.CONTENT_URI).withSelection(SchemaNotifications.COLUMN_UID + "=?", new String[]{notifUID}).build());
                getContentResolver().applyBatch(AppConstants.API.CONTENT_AUTHORITY.getValue(), delOps);
            } else {
                return;
            }

            // REMOTE UPDATE
            String notifURI = StringUtils.join(new String[]{NOTIFICATIONS.getValue(), notifUID}, "/");

            // body part of request
            Map<String, String> reqBody = new HashMap<>();
            if (!Utils.isLoggedIn()) {
                // UNIQUE ID FOR THIS INSTALL
                reqBody.put(PARAMS.REF.getValue(), Utils.uniqueID());
            }

            ReqSetBody req = new ReqSetBody();
            req.setToken(Utils.getAppToken(false));
            req.setCmd(PARAMS.COMMAND.getValue());
            req.setBody(reqBody);

            //headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(RestClient.INSTANCE.jsonMediaType());

            HttpEntity<ReqSetBody> payloadEntity = new HttpEntity<>(req, headers);
            RestClient.INSTANCE.handle().exchange(notifURI, HttpMethod.POST, payloadEntity, Resp.class);
        } catch (Exception e) {
            // muted
        } finally {
            BusProvider.INSTANCE.getBus().post(new Events.SingleNotifsUPDATEEvent(eventBody));
        }
    }

    enum PARAMS {
        REF("ref"),
        COMMAND("update"),;
        final String value;

        PARAMS(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
