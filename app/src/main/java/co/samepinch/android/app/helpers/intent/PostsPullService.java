package co.samepinch.android.app.helpers.intent;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Intent;
import android.os.Bundle;
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

import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.module.DaggerStorageComponent;
import co.samepinch.android.app.helpers.module.StorageComponent;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.data.dao.SchemaDots;
import co.samepinch.android.data.dao.SchemaPosts;
import co.samepinch.android.data.dao.SchemaTags;
import co.samepinch.android.data.dto.Post;
import co.samepinch.android.data.dto.User;
import co.samepinch.android.rest.ReqPosts;
import co.samepinch.android.rest.RespPosts;
import co.samepinch.android.rest.RestClient;

import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_BY;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_ETAG;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_KEY;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_LAST_MODIFIED;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_POST_COUNT;
import static co.samepinch.android.app.helpers.AppConstants.APP_INTENT.KEY_STEP;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Created by imaginationcoder on 6/26/15.
 */
public class PostsPullService extends IntentService {
    public static final String TAG = "PostsPullService";

    public PostsPullService() {
        super("PostsPullService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            StorageComponent component = DaggerStorageComponent.create();
            ReqPosts postsReq = component.provideReqPosts();

            // get caller data
            Bundle iArgs = intent.getExtras();
            // set base args
            postsReq.setToken(Utils.getNonBlankAppToken());
            postsReq.setCmd("filter");
            // set context args
            postsReq.setPostCount(iArgs.getString(KEY_POST_COUNT.getValue(), "99"));
            postsReq.setLastModified(iArgs.getString(KEY_LAST_MODIFIED.getValue(), EMPTY));
            postsReq.setKey(iArgs.getString(KEY_STEP.getValue(), EMPTY));
            postsReq.setEtag(iArgs.getString(KEY_ETAG.getValue(), EMPTY));
            postsReq.setKey(iArgs.getString(KEY_KEY.getValue(), EMPTY));
            postsReq.setKey(iArgs.getString(KEY_BY.getValue(), EMPTY));

            //headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<ReqPosts> payloadEntity = new HttpEntity<>(postsReq.build(), headers);
            ResponseEntity<RespPosts> resp = RestClient.INSTANCE.handle().exchange(AppConstants.API.POSTS_WITH_FILTER.getValue(), HttpMethod.POST, payloadEntity, RespPosts.class);
            ArrayList<ContentProviderOperation> ops = parseResponse(iArgs, resp.getBody());
            ContentProviderResult[] result = getContentResolver().
                    applyBatch(AppConstants.API.CONTENT_AUTHORITY.getValue(), ops);

            // event data
            Map<String, String> metaData = new HashMap<>();
            RespPosts.Body respBody = resp.getBody().getBody();
            metaData.put(KEY_LAST_MODIFIED.getValue(), respBody.getLastModifiedStr());
            metaData.put(KEY_ETAG.getValue(), respBody.getEtag());
            metaData.put(KEY_POST_COUNT.getValue(), String.valueOf(respBody.getPostCount()));
//            metaData.put(KEY_BY.getValue(), iArgs.getString(KEY_BY.getValue()));
            // publish success event
            BusProvider.INSTANCE.getBus().post(new Events.PostsRefreshedEvent(metaData));
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "error refreshing posts...", e);
        }
    }


    private ArrayList<ContentProviderOperation> parseResponse(Bundle reqArgs, RespPosts respData) {
        List<Post> postsToInsert = respData.getBody().getPosts();
        if (postsToInsert == null) {
            return null;
        }

        // anonymous dot construction
        StorageComponent component = DaggerStorageComponent.create();
        User anonyOwner = component.provideAnonymousDot();

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        User postOwner;
        for (Post post : postsToInsert) {
            postOwner = post.getOwner();
            // dot
            appendDOTOps(postOwner, anonyOwner, ops);
            // post
            appendPostOps(post, anonyOwner, ops);
            // tags
            appendTagOps(post, ops);
        }
        return ops;

    }


    //    @NonNull
//    private ArrayList<ContentProviderOperation> parseResponse(Bundle reqArgs, RespPosts respData) {
//        List<Post> postsToInsert = respData.getBody().getPosts();
//        if (postsToInsert == null) {
//            return null;
//        }
//
//        // anonymous dot construction
//        StorageComponent component = DaggerStorageComponent.create();
//        User anonyOwner = component.provideAnonymousDot();
//
//        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
//
//
//        // anonymous dot construction
//        StorageComponent component = DaggerStorageComponent.create();
//        User dfltAnonyDot = component.provideAnonymousDot();
//        String anonyImg = respData.getBody().getAnonymousImage();
//        dfltAnonyDot.setPhoto(anonyImg);
//
//        for (Post post : postsToInsert) {
//            User postOwner = post.getOwner();
//            // DOT
//            appendDOTOps(post.getOwner(), anonyOwner, ops);
//
//            // DOTS
//            if (post.getAnonymous()) {
//                // TODO
//                ops.add(ContentProviderOperation.newInsert(SchemaDots.CONTENT_URI)
//                        .withValue(SchemaDots.COLUMN_UID, dfltAnonyDot.getUid())
//                        .withValue(SchemaDots.COLUMN_FNAME, dfltAnonyDot.getFname())
//                        .withValue(SchemaDots.COLUMN_LNAME, dfltAnonyDot.getLname())
//                        .withValue(SchemaDots.COLUMN_PREF_NAME, dfltAnonyDot.getPrefName())
//                        .withValue(SchemaDots.COLUMN_PINCH_HANDLE, dfltAnonyDot.getPinchHandle())
//                        .withValue(SchemaDots.COLUMN_PHOTO_URL, dfltAnonyDot.getPhoto()).build());
//            } else {
//                ops.add(ContentProviderOperation.newInsert(SchemaDots.CONTENT_URI)
//                        .withValue(SchemaDots.COLUMN_UID, postOwner.getUid())
//                        .withValue(SchemaDots.COLUMN_FNAME, postOwner.getFname())
//                        .withValue(SchemaDots.COLUMN_LNAME, postOwner.getLname())
//                        .withValue(SchemaDots.COLUMN_PREF_NAME, postOwner.getPrefName())
//                        .withValue(SchemaDots.COLUMN_PINCH_HANDLE, postOwner.getPinchHandle())
//                        .withValue(SchemaDots.COLUMN_PHOTO_URL, postOwner.getPhoto()).build());
//            }
//            // POSTS
//            ops.add(ContentProviderOperation.newInsert(SchemaPosts.CONTENT_URI)
//                    .withValue(SchemaPosts.COLUMN_UID, post.getUid())
//                    .withValue(SchemaPosts.COLUMN_CONTENT, post.getContent())
//                    .withValue(SchemaPosts.COLUMN_IMAGES, post.getImagesForDB())
//                    .withValue(SchemaPosts.COLUMN_COMMENT_COUNT, post.getCommentCount())
//                    .withValue(SchemaPosts.COLUMN_UPVOTE_COUNT, post.getUpvoteCount())
//                    .withValue(SchemaPosts.COLUMN_VIEWS, post.getViews())
//                    .withValue(SchemaPosts.COLUMN_ANONYMOUS, post.getAnonymous())
//                    .withValue(SchemaPosts.COLUMN_CREATED_AT, post.getCreatedAt().getTime())
//                    .withValue(SchemaPosts.COLUMN_COMMENTERS, post.getCommentersForDB())
//                    .withValue(SchemaPosts.COLUMN_COMMENTERS, post.getCommentersForDB())
//                    .withValue(SchemaPosts.COLUMN_OWNER, (post.getAnonymous() ? dfltAnonyDot.getUid() : postOwner.getUid()))
//                    .withValue(SchemaPosts.COLUMN_TAGS, post.getTagsForDB()).build());
//            // TAGS
//            for (String tag : post.getTags()) {
//                ops.add(ContentProviderOperation.newInsert(SchemaTags.CONTENT_URI)
//                        .withValue(SchemaTags.COLUMN_NAME, tag)
//                        .build());
//            }
//        }
//        return ops;
//    }
//
//
    private static void appendDOTOps(User postOwner, User anonyOwner, ArrayList<ContentProviderOperation> ops) {
        if (postOwner == null) {
            ops.add(ContentProviderOperation.newInsert(SchemaDots.CONTENT_URI)
                    .withValue(SchemaDots.COLUMN_UID, anonyOwner.getUid())
                    .withValue(SchemaDots.COLUMN_FNAME, anonyOwner.getFname())
                    .withValue(SchemaDots.COLUMN_LNAME, anonyOwner.getLname())
                    .withValue(SchemaDots.COLUMN_PREF_NAME, anonyOwner.getPrefName())
                    .withValue(SchemaDots.COLUMN_PINCH_HANDLE, anonyOwner.getPinchHandle())
                    .withValue(SchemaDots.COLUMN_PHOTO_URL, anonyOwner.getPhoto()).build());
        } else {
            ops.add(ContentProviderOperation.newInsert(SchemaDots.CONTENT_URI)
                    .withValue(SchemaDots.COLUMN_UID, postOwner.getUid())
                    .withValue(SchemaDots.COLUMN_FNAME, postOwner.getFname())
                    .withValue(SchemaDots.COLUMN_LNAME, postOwner.getLname())
                    .withValue(SchemaDots.COLUMN_PREF_NAME, postOwner.getPrefName())
                    .withValue(SchemaDots.COLUMN_PINCH_HANDLE, postOwner.getPinchHandle())
                    .withValue(SchemaDots.COLUMN_PHOTO_URL, postOwner.getPhoto()).build());
        }
    }

    private static void appendPostOps(Post post, User anonyOwner, ArrayList<ContentProviderOperation> ops) {
        ops.add(ContentProviderOperation.newInsert(SchemaPosts.CONTENT_URI)
                .withValue(SchemaPosts.COLUMN_UID, post.getUid())
                .withValue(SchemaPosts.COLUMN_CONTENT, post.getContent())
                .withValue(SchemaPosts.COLUMN_IMAGES, post.getImagesForDB())
                .withValue(SchemaPosts.COLUMN_COMMENT_COUNT, post.getCommentCount())
                .withValue(SchemaPosts.COLUMN_UPVOTE_COUNT, post.getUpvoteCount())
                .withValue(SchemaPosts.COLUMN_VIEWS, post.getViews())
                .withValue(SchemaPosts.COLUMN_ANONYMOUS, post.getAnonymous())
                .withValue(SchemaPosts.COLUMN_CREATED_AT, post.getCreatedAt().getTime())
                .withValue(SchemaPosts.COLUMN_COMMENTERS, post.getCommentersForDB())
                .withValue(SchemaPosts.COLUMN_OWNER, post.getUid())
                .withValue(SchemaPosts.COLUMN_TAGS, post.getTagsForDB()).build());
    }

    private static void appendTagOps(Post post, ArrayList<ContentProviderOperation> ops) {
        for (String tag : post.getTags()) {
            ops.add(ContentProviderOperation.newInsert(SchemaTags.CONTENT_URI)
                    .withValue(SchemaTags.COLUMN_NAME, tag)
                    .build());
        }
    }

}
