package co.samepinch.android.app.helpers.intent;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
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

    public AllNotificationsService() {
        super("AllNotificationsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Map<String, String> reqBody = new HashMap<>();
//            reqBody.put("last_modified", "");
            reqBody.put("step", "new");
            // new or next
//            reqBody.put("etag", "");


            ReqSetBody req = new ReqSetBody();
            req.setToken(Utils.getAppToken(false));
            req.setCmd("all");

            req.setBody(reqBody);
            //headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(RestClient.INSTANCE.jsonMediaType());

            HttpEntity<ReqSetBody> payloadEntity = new HttpEntity<>(req, headers);
            ResponseEntity<RespAllNotifs> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.NOTIFICATIONS.getValue(), HttpMethod.POST, payloadEntity, RespAllNotifs.class);
            ArrayList<ContentProviderOperation> ops = parseResponse(resp.getBody());
            if (ops != null && ops.size() > 0) {
                ContentProviderResult[] result = getContentResolver().
                        applyBatch(AppConstants.API.CONTENT_AUTHORITY.getValue(), ops);
            }
        } catch (Exception e) {
            // muted
            Resp resp = Utils.parseAsRespSilently(e);
            Map<String, String> eventData = new HashMap<>();
            String msg = resp == null || resp.getMessage() == null ? AppConstants.APP_INTENT.KEY_MSG_GENERIC_ERR.getValue() : resp.getMessage();
            eventData.put(AppConstants.K.MESSAGE.name(), msg);
        }finally{
            BusProvider.INSTANCE.getBus().post(new Events.AllNotifsRefreshedEvent(null));
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
        for(int i=2; i-->0;){
            for (RespAllNotifs.Notification notification : respData.getBody().getNotifications()) {
                ops.add(ContentProviderOperation.newInsert(SchemaNotifications.CONTENT_URI)
                        .withValue(SchemaNotifications.COLUMN_UID, notification.getUid() + System.currentTimeMillis())
                        .withValue(SchemaNotifications.COLUMN_MSG, notification.getMessage())
                        .withValue(SchemaNotifications.COLUMN_SRC, notification.getSource())
                        .withValue(SchemaNotifications.COLUMN_SRC_ID, notification.getSourceId())
                        .withValue(SchemaNotifications.COLUMN_VIEWED, notification.getViewed())
                        .build());
            }
        }
        return ops;
    }
}
