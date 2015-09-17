package co.samepinch.android.app.helpers.adapters;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.flipboard.bottomsheet.BottomSheetLayout;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.samepinch.android.app.ActivityFragment;
import co.samepinch.android.app.R;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.intent.CommentUpdateService;
import co.samepinch.android.app.helpers.module.DaggerStorageComponent;
import co.samepinch.android.app.helpers.module.StorageComponent;
import co.samepinch.android.app.helpers.pubsubs.BusProvider;
import co.samepinch.android.app.helpers.pubsubs.Events;
import co.samepinch.android.data.dto.CommentDetails;
import co.samepinch.android.data.dto.Commenter;
import co.samepinch.android.data.dto.User;
import co.samepinch.android.rest.ReqNoBody;
import co.samepinch.android.rest.Resp;
import co.samepinch.android.rest.RestClient;

/**
 * Created by imaginationcoder on 7/27/15.
 */
public class PostCommentsRVHolder extends PostDetailsRVHolder {

    @Bind(R.id.avatar)
    SimpleDraweeView mAvatar;

    @Bind(R.id.avatar_name)
    TextView mAvatarName;

    @Bind(R.id.avatar_handle)
    TextView mAvatarHandle;

    @Bind(R.id.comment_upvote)
    TextView mCommentUpvote;

    @Bind(R.id.comment_date)
    TextView mCommentDate;

    @Bind(R.id.comment)
    TextView mComment;

    @Bind(R.id.comment_menu)
    ImageView commentMenu;

    public PostCommentsRVHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    void onBindViewHolderImpl(Cursor cursor) {
        final CommentDetails commentDetails = Utils.cursorToCommentDetails(cursor);
        String fName, lName, photo, handle;
        View.OnClickListener dotClick = null;
        if (commentDetails.getAnonymous()) {
            StorageComponent component = DaggerStorageComponent.create();
            User anonyOwner = component.provideAnonymousDot();
            fName = anonyOwner.getFname();
            lName = anonyOwner.getLname();
            photo = anonyOwner.getPhoto();
            handle = anonyOwner.getPinchHandle();
        } else {
            final Commenter commenter = commentDetails.getCommenter();
            fName = commenter.getFname();
            lName = commenter.getLname();
            photo = commenter.getPhoto();
            handle = commenter.getPinchHandle();
            dotClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TARGET
                    Bundle args = new Bundle();
                    args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_DOTWALL.name());
                    // data
                    args.putString(AppConstants.K.KEY_DOT.name(), commenter.getUid());

                    // intent
                    Intent intent = new Intent(mView.getContext(), ActivityFragment.class);
                    intent.putExtras(args);

                    mView.getContext().startActivity(intent);
                }
            };
        }

        if (StringUtils.isBlank(photo)) {
            mAvatarName.setVisibility(View.VISIBLE);
            mAvatar.setVisibility(View.INVISIBLE);

            String name = StringUtils.join(StringUtils.substring(fName, 0, 1), StringUtils.substring(lName, 0, 1));
            mAvatarName.setText(name);

            if (dotClick != null) {
                mAvatarName.setOnClickListener(dotClick);
            }

        } else {
            mAvatar.setVisibility(View.VISIBLE);
            mAvatarName.setVisibility(View.GONE);
            Utils.setupLoadingImageHolder(mAvatar, photo);

            if (dotClick != null) {
                mAvatar.setOnClickListener(dotClick);
            }
        }

        // setup handle
        String pinchHandle = String.format(mView.getContext().getString(R.string.pinch_handle), handle);
        mAvatarHandle.setText(pinchHandle);

        // setup counts
        mCommentUpvote.setText(StringUtils.defaultString(Integer.toString(commentDetails.getUpvoteCount()), "0"));

        // setup date
        mCommentDate.setText(Utils.dateToString(commentDetails.getCreatedAt()));

        // setup comment
        mComment.setText(commentDetails.getText());

        final List<String> permissions = commentDetails.getPermissions();
        final String commentUID = commentDetails.getUid();
        commentMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BottomSheetLayout bs = (BottomSheetLayout) mView.getRootView().findViewById(R.id.bottomsheet);
                // prepare menu options
                View menu = LayoutInflater.from(mView.getContext()).inflate(R.layout.bs_menu, bs, false);
                LinearLayout layout = (LinearLayout) menu.findViewById(R.id.layout_menu_list);
                boolean addDiv = false;
                if (commentDetails.getUpvoted() != null && commentDetails.getUpvoted()) {
                    if (addDiv) {
//                        View divider = LayoutInflater.from(mView.getContext()).inflate(R.layout.raw_divider, null);
//                        layout.addView(divider);
                    }

                    TextView downVoteView = (TextView) LayoutInflater.from(mView.getContext()).inflate(R.layout.bs_raw_downvote, null);
                    layout.addView(downVoteView);
                    new MenuItemClickListener(downVoteView, "undoVoting", commentUID, bs);
                    addDiv = true;
                } else {
                    if (addDiv) {
//                        View divider = LayoutInflater.from(mView.getContext()).inflate(R.layout.raw_divider, null);
//                        layout.addView(divider);
                    }

                    TextView voteView = (TextView) LayoutInflater.from(mView.getContext()).inflate(R.layout.bs_raw_upvote, null);
                    layout.addView(voteView);
                    new MenuItemClickListener(voteView, "upvote", commentUID, bs);
                    addDiv = true;
                }

                if (permissions.contains("edit")) {
                    if (addDiv) {
//                        View divider = LayoutInflater.from(mView.getContext()).inflate(R.layout.raw_divider, null);
//                        layout.addView(divider);
                    }

                    final TextView editView = (TextView) LayoutInflater.from(mView.getContext()).inflate(R.layout.bs_raw_edit, null);
                    editView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Map<String, String> metaData = new HashMap<>();
                            metaData.put(AppConstants.K.COMMENT.name(), commentDetails.getUid());
                            BusProvider.INSTANCE.getBus().post(new Events.CommentDetailsEditEvent(metaData));
                            bs.dismissSheet();
                        }
                    });
                    layout.addView(editView);
                    addDiv = true;
                }

                if (permissions.contains("flag")) {
                    if (addDiv) {
//                        View divider = LayoutInflater.from(mView.getContext()).inflate(R.layout.raw_divider, null);
//                        layout.addView(divider);
                    }

                    TextView flagView = (TextView) LayoutInflater.from(mView.getContext()).inflate(R.layout.bs_raw_flag, null);
                    layout.addView(flagView);
                    new MenuItemClickListener(flagView, "flag", commentUID, bs);
                    addDiv = true;
                }

                if (permissions.contains("un-flag")) {
                    if (addDiv) {
//                        View divider = LayoutInflater.from(mView.getContext()).inflate(R.layout.raw_divider, null);
//                        layout.addView(divider);
                    }

                    TextView unFlagView = (TextView) LayoutInflater.from(mView.getContext()).inflate(R.layout.bs_raw_unflag, null);
                    layout.addView(unFlagView);
                    addDiv = true;
                }


                bs.showWithSheetView(menu);
            }
        });
    }

    private class CommentActionTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... args) {
            if (args == null || args.length < 2) {
                return Boolean.FALSE;
            }

            try {
                ReqNoBody req = new ReqNoBody();
                req.setToken(Utils.getNonBlankAppToken());
                req.setCmd(args[1]);

                //headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

                HttpEntity<ReqNoBody> payloadEntity = new HttpEntity<>(req, headers);
                String commentUrl = StringUtils.join(new String[]{AppConstants.API.COMMENTS.getValue(), args[0]}, "/");

                ResponseEntity<Resp> resp = RestClient.INSTANCE.handle().exchange(commentUrl, HttpMethod.POST, payloadEntity, Resp.class);
                return resp.getBody().getStatus() == 200;
            } catch (Exception e) {
                // muted
                Resp resp = Utils.parseAsRespSilently(e);
            }
            return Boolean.FALSE;
        }

        @Override
        protected void onPostExecute(Boolean result) {
//            Utils.dismissSilently(progressDialog);
//            if (result != null && result.booleanValue()) {
//                Snackbar.make(getView(), "commented successfully.", Snackbar.LENGTH_SHORT).show();
//
//                Intent resultIntent = new Intent();
//                getActivity().setResult(Activity.RESULT_OK, resultIntent);
//                getActivity().finish();
//            } else {
//                Snackbar.make(getView(), "error commenting. try again...", Snackbar.LENGTH_SHORT).show();
//            }
        }
    }

    private static class MenuItemClickListener implements View.OnClickListener {
        private final View view;
        private final String command;
        private final String commentUID;
        private final BottomSheetLayout bottomSheet;

        public MenuItemClickListener(View source, String command, String commentUID, BottomSheetLayout bs) {
            this.command = command;
            this.commentUID = commentUID;
            this.view = source;
            this.bottomSheet = bs;
            this.view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            bottomSheet.dismissSheet();
            Bundle iArgs = new Bundle();
            iArgs.putString(AppConstants.K.COMMENT.name(), commentUID);
            iArgs.putString(AppConstants.K.COMMAND.name(), command);

            // call for intent
            Intent intent =
                    new Intent(view.getContext(), CommentUpdateService.class);
            intent.putExtras(iArgs);
            view.getContext().startService(intent);
        }
    }
}
