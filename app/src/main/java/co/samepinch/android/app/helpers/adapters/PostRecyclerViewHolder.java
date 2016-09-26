package co.samepinch.android.app.helpers.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.samepinch.android.app.ActivityFragment;
import co.samepinch.android.app.PostDetailActivity;
import co.samepinch.android.app.R;
import co.samepinch.android.app.SPApplication;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.data.dto.Commenter;
import co.samepinch.android.data.dto.Post;
import co.samepinch.android.data.dto.User;

/**
 * Created by imaginationcoder on 7/2/15.
 */
public class PostRecyclerViewHolder extends RecyclerView.ViewHolder {
    static String[] BG_COLORS = SPApplication.getContext().getResources().getStringArray(R.array.post_colors);
    private static AtomicInteger BG_COLOR_INDEX = new AtomicInteger();
    @Bind(R.id.layout_post_item)
    View mLayout;
    @Bind(R.id.avatar_image_vs)
    ViewSwitcher mAvatarImgVS;
    @Bind(R.id.avatar)
    SimpleDraweeView mAvatarView;
    @Bind(R.id.avatar_name)
    TextView mAvatarName;
    @Bind(R.id.wall_post_dot)
    TextView mWallPostDotView;
    @Bind(R.id.wall_post_images)
    SimpleDraweeView mWallPostImages;
    @Bind(R.id.wall_post_content)
    TextView mWallPostContentView;
    @Bind(R.id.wall_tags)
    TextView mWallTags;
    @Bind(R.id.wall_post_commenters)
    LinearLayout mCommentersLayout;
    @Bind(R.id.wall_post_views)
    TextView mWallPostViewsView;
    @Bind(R.id.wall_post_upvote)
    TextView mWallPostUpvoteView;
    @Bind(R.id.wall_commenters_count)
    TextView mCommentersCountView;

    Context mContext;
    Post mPost;

    public PostRecyclerViewHolder(final Context context, View itemView) {
        super(itemView);
        setIsRecyclable(Boolean.TRUE);

        this.mContext = context;
        ButterKnife.bind(this, itemView);

        // post bg color
        BG_COLOR_INDEX.compareAndSet(BG_COLORS.length, 0);
        mLayout.setBackgroundColor(Color.parseColor(BG_COLORS[BG_COLOR_INDEX.getAndIncrement()]));
    }


    public void onBindViewHolderImpl(final Cursor cursor) {
        mPost = Utils.cursorToPostEntity(cursor);

        final User user = mPost.getOwner();
        if (Utils.isValidUri(user.getPhoto())) {
            Utils.setupLoadingImageHolder(mAvatarView, user.getPhoto());
            mAvatarImgVS.setDisplayedChild(0);
        } else {
            String name = StringUtils.join(StringUtils.substring(user.getFname(), 0, 1), StringUtils.substring(user.getLname(), 0, 1));
            if (StringUtils.isBlank(name)) {
                name = (StringUtils.substring(user.getPinchHandle(), 0, 1));
            }
            mAvatarName.setText(name);
            mAvatarImgVS.setDisplayedChild(1);
        }
        String fName = StringUtils.defaultString(user.getFname()).toLowerCase(Locale.getDefault());
        String lName = StringUtils.defaultString(user.getLname()).toLowerCase(Locale.getDefault());
        String name = StringUtils.join(new String[]{StringUtils.capitalize(fName), StringUtils.capitalize(lName)}, " ");
        mWallPostDotView.setText(StringUtils.defaultString(name, "anonymous"));
//        mWallPinchHandleView.setText(String.format(mContext.getString(R.string.pinch_handle), user.getPinchHandle()));
        if (mPost == null) {
            return;
        }
        Integer viewsCnt = mPost.getViews();
        Integer voteCnt = mPost.getUpvoteCount();
        Integer commentCnt = mPost.getCommentCount();
        mWallPostViewsView.setText(StringUtils.defaultString(viewsCnt == null ? null : viewsCnt.toString(), StringUtils.EMPTY));
        mWallPostUpvoteView.setText(StringUtils.defaultString(voteCnt == null ? null : voteCnt.toString(), StringUtils.EMPTY));
        String commentsCnt = StringUtils.defaultString(commentCnt == null ? null : commentCnt.toString(), StringUtils.EMPTY);
//        if (StringUtils.isNotBlank(commentsCnt) && commentsCnt != "0") {
//            mCommentersCount.setText(commentsCnt);
//        }


//        mWallPostDateView.setText(StringUtils.defaultString(TimeUtils.toHumanRelativePeriod(mPost.getCreatedAt()), StringUtils.EMPTY));
        mWallPostContentView.setText(StringUtils.defaultString(mPost.getWallContent(), StringUtils.EMPTY));

        // hide needed ones
//        int commentViewsCount = mWallPostCommentersLayout.getChildCount();
//        for (int i = 0; i < commentViewsCount; i++) {
//            View child = mWallPostCommentersLayout.getChildAt(i);
//            if (child instanceof SimpleDraweeView) {
//                child.setVisibility(View.GONE);
//            }
//        }

        // clear all
        mCommentersLayout.removeAllViews();
        List<Commenter> _commenters = mPost.getCommenters();
        if (_commenters != null && _commenters.size() > 0) {
            // set count
            mCommentersCountView.setText(Integer.toString(_commenters.size()));

            // set images stuff
            String anonyImg = Utils.PreferencesManager.getInstance().getValue(AppConstants.API.PREF_ANONYMOUS_IMG.getValue());
            String _commenterImg;
            int _commenterViewIdx = 0;
            for (Commenter _commenter : _commenters) {
                if (_commenter.getAnonymous() != null && _commenter.getAnonymous().booleanValue()) {
                    _commenterImg = anonyImg;
                } else if (Utils.isValidUri(_commenter.getPhoto())) {
                    _commenterImg = _commenter.getPhoto();
                } else {
                    continue;
                }

                // add to parent
                LayoutInflater.from(mContext).inflate(R.layout.img_commenter, mCommentersLayout);
                // get recently added
                SimpleDraweeView _commenterView = (SimpleDraweeView) mCommentersLayout.getChildAt(_commenterViewIdx);
                Utils.setupLoadingImageHolder(_commenterView, _commenterImg);
                _commenterViewIdx += 1;
            }
        }else{
            mCommentersCountView.setText(R.string.q_mark);
        }

        // count
//        LayoutInflater.from(mContext).inflate(R.layout.text_commenters_count, mCommentersLayout);

        if (CollectionUtils.isEmpty(mPost.getWallImages())) {
            mWallPostImages.setVisibility(View.GONE);
        } else {
            Utils.setupLoadingImageHolder(mWallPostImages, mPost.getWallImages().get(0));
            mWallPostImages.setVisibility(View.VISIBLE);
        }

        if (StringUtils.isNotBlank(mPost.getTagsForDB())) {
            Utils.markTags(mContext, mWallTags, mPost.getTagsForDB().split(","));
        }
    }

    @OnClick({R.id.avatar, R.id.avatar_name, R.id.wall_post_dot})
    public void doOpenDotWall() {
        if (mPost.getAnonymous() != null && mPost.getAnonymous().booleanValue()) {
            return;
        }

        Bundle args = new Bundle();
        args.putString(AppConstants.K.KEY_DOT.name(), mPost.getOwner().getUid());
        args.putString(AppConstants.K.TARGET_FRAGMENT.name(), AppConstants.K.FRAGMENT_DOTWALL.name());

        Intent intent = new Intent(mContext, ActivityFragment.class);
        intent.putExtras(args);

        mContext.startActivity(intent);
    }

    @OnClick({R.id.wall_post_content, R.id.wall_post_images, R.id.layout_post_item})
    public void doOpenPost() {
        Bundle iArgs = new Bundle();
        iArgs.putString(AppConstants.K.POST.name(), mPost.getUid());

        Intent intent = new Intent(mContext, PostDetailActivity.class);
        intent.putExtras(iArgs);

        mContext.startActivity(intent);
    }
}
