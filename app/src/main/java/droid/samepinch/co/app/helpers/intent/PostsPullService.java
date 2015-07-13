package droid.samepinch.co.app.helpers.intent;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import droid.samepinch.co.app.helpers.AppConstants;
import droid.samepinch.co.app.helpers.module.DaggerStorageComponent;
import droid.samepinch.co.app.helpers.module.DataModule;
import droid.samepinch.co.app.helpers.module.StorageComponent;
import droid.samepinch.co.data.dao.SchemaDots;
import droid.samepinch.co.data.dao.SchemaPosts;
import droid.samepinch.co.data.dao.SchemaTags;
import droid.samepinch.co.data.dto.Post;
import droid.samepinch.co.data.dto.User;
import droid.samepinch.co.rest.ReqPosts;
import droid.samepinch.co.rest.RespPosts;

import static droid.samepinch.co.app.helpers.AppConstants.KV.CLIENT_ID;
import static droid.samepinch.co.app.helpers.AppConstants.KV.CLIENT_SECRET;
import static droid.samepinch.co.app.helpers.AppConstants.KV.GRANT_TYPE;
import static droid.samepinch.co.app.helpers.AppConstants.KV.SCOPE;

/**
 * Created by imaginationcoder on 6/26/15.
 */
public class PostsPullService extends IntentService {
    public static final String LOG_TAG = "PostsPullService";
    private BroadcastNotifier mBroadcaster;


    public PostsPullService() {
        super(PostsPullService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mBroadcaster = new BroadcastNotifier(this);
        mBroadcaster.broadcastIntentWithState(AppConstants.APP_INTENT.REFRESH_ACTION_STARTED);

        String appToken = getAppToken();
        if (appToken == null) {
            mBroadcaster.broadcastIntentWithState(AppConstants.APP_INTENT.REFRESH_ACTION_FAILED);
            return;
        }

        StorageComponent component = DaggerStorageComponent.create();
        ReqPosts postsReq = component.provideReqPosts();

        postsReq.setToken(appToken);
        postsReq.setCmd("all");
        postsReq.setPostCount(10);
        postsReq.setLastModified(null);
        postsReq.setStep(1);
        postsReq.setEtag("");

        try {
            //headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            // call
            HttpEntity<ReqPosts> payloadEntity = new HttpEntity<>(postsReq.build(), headers);
            ResponseEntity<RespPosts> resp = component.provideRestTemplate().exchange(AppConstants.API.POSTS.getValue(), HttpMethod.POST, payloadEntity, RespPosts.class);
            RespPosts respData = resp.getBody();

            List<Post> postsToInsert = respData.getBody().getPosts();
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            // anonymous dot construction
            User dfltAnonyDot = component.provideAnonymousDot();
            String anonyImg = respData.getBody().getAnonymousImage();
            dfltAnonyDot.setPhoto(anonyImg);

            for (Post post : postsToInsert) {
                User postOwner = post.getOwner();
                // DOTS
                if (post.getAnonymous()) {
//                    dfltAnonyDot.setPhoto(anonyImg);
                    // TODO
                    ops.add(ContentProviderOperation.newInsert(SchemaDots.CONTENT_URI)
                            .withValue(SchemaDots.COLUMN_UID, dfltAnonyDot.getUid())
                            .withValue(SchemaDots.COLUMN_FNAME, dfltAnonyDot.getFname())
                                    .withValue(SchemaDots.COLUMN_LNAME, dfltAnonyDot.getLname())
                                    .withValue(SchemaDots.COLUMN_PREF_NAME, dfltAnonyDot.getPrefName())
//                                    .withValue(SchemaDots.COLUMN_PINCH_HANDLE, "anonymous")
                                    .withValue(SchemaDots.COLUMN_PHOTO_URL, dfltAnonyDot.getPhoto()).build());
                } else {
                    ops.add(ContentProviderOperation.newInsert(SchemaDots.CONTENT_URI)
                            .withValue(SchemaDots.COLUMN_UID, postOwner.getUid())
                            .withValue(SchemaDots.COLUMN_FNAME, postOwner.getFname())
                            .withValue(SchemaDots.COLUMN_LNAME, postOwner.getLname())
                            .withValue(SchemaDots.COLUMN_PREF_NAME, postOwner.getPrefName())
                            .withValue(SchemaDots.COLUMN_PINCH_HANDLE, postOwner.getPinchHandle())
                            .withValue(SchemaDots.COLUMN_PHOTO_URL, postOwner.getPhoto()).build());
                }
                // POSTS
                ops.add(ContentProviderOperation.newInsert(SchemaPosts.CONTENT_URI)
                        .withValue(SchemaPosts.COLUMN_UID, post.getUid())
                        .withValue(SchemaPosts.COLUMN_CONTENT, post.getContent())
                        .withValue(SchemaPosts.COLUMN_COMMENT_COUNT, post.getCommentCount())
                        .withValue(SchemaPosts.COLUMN_UPVOTE_COUNT, post.getUpvoteCount())
                        .withValue(SchemaPosts.COLUMN_VIEWS, post.getViews())
                        .withValue(SchemaPosts.COLUMN_ANONYMOUS, post.getAnonymous())
                        .withValue(SchemaPosts.COLUMN_CREATED_AT, post.getCreatedAt().getTime())
                        .withValue(SchemaPosts.COLUMN_COMMENTERS, post.getCommentersForDB())
                        .withValue(SchemaPosts.COLUMN_OWNER, (post.getAnonymous() ? dfltAnonyDot.getUid() : postOwner.getUid()))
                        .withValue(SchemaPosts.COLUMN_TAGS, post.getTagsForDB()).build());
                // TAGS
                for (String tag : post.getTags()) {
                    ops.add(ContentProviderOperation.newInsert(SchemaTags.CONTENT_URI)
                            .withValue(SchemaTags.COLUMN_NAME, tag)
                            .withValue(SchemaTags.COLUMN_PHOTO_URL, "http://bigtheme.ir/wp-content/uploads/2015/06/sample.jpg")
                            .build());
                }
            }

            ContentProviderResult[] result = getContentResolver().
                    applyBatch(AppConstants.API.CONTENT_AUTHORITY.getValue(), ops);
            mBroadcaster.broadcastIntentWithState(AppConstants.APP_INTENT.REFRESH_ACTION_COMPLETE);
        } catch (Exception e) {
            e.printStackTrace();
            mBroadcaster.broadcastIntentWithState(AppConstants.APP_INTENT.REFRESH_ACTION_FAILED);
        }
    }

    public String getAppToken() {
        String accessToken = AppConstants.API.ACCESS_TOKEN.getValue();
        SharedPreferences settings = getSharedPreferences(AppConstants.API.SHARED_PREFS_NAME.getValue(), 0);
        String token = settings.getString(accessToken, null);
        if (token != null) {
            //return token;
        }
        mBroadcaster.broadcastIntentWithState(AppConstants.APP_INTENT.AUTHENTICATING_CLIENT);

        try {
            //time to fetch a token
            final Map<String, String> payload = new HashMap<>();
            payload.put(CLIENT_ID.getKey(), CLIENT_ID.getValue());
            payload.put(CLIENT_SECRET.getKey(), CLIENT_SECRET.getValue());
            payload.put(SCOPE.getKey(), SCOPE.getValue());
            payload.put(GRANT_TYPE.getKey(), GRANT_TYPE.getValue());

            StorageComponent component = DaggerStorageComponent.create();
            ResponseEntity<Map> response = component.provideRestTemplate().postForEntity(AppConstants.API.CLIENTAUTH.getValue(), payload, Map.class);
            Map<String, String> responseEntity = response.getBody();
            // populate to shared prefs
            SharedPreferences.Editor editor = settings.edit();
            for (Map.Entry<String, String> entry : responseEntity.entrySet()) {
                editor.putString(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            editor.commit();

            return settings.getString(accessToken, null);
        } catch (RuntimeException e) {
            // muted
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

}
