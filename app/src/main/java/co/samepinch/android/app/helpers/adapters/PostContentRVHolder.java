package co.samepinch.android.app.helpers.adapters;


import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.samepinch.android.app.R;
import co.samepinch.android.app.SPApplication;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.app.helpers.widget.SIMView;
import co.samepinch.android.data.dto.PostDetails;

/**
 * Created by imaginationcoder on 7/27/15.
 */
public class PostContentRVHolder extends PostDetailsRVHolder {

    @Bind(R.id.post_details_content)
    ViewGroup mViewGroup;

    @Bind(R.id.post_details_tags)
    TextView mTagsView;

    public PostContentRVHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        setIsRecyclable(false);
    }

    void onBindViewHolderImpl(Cursor cursor) {
        mViewGroup.removeAllViews();
        // do nothing in base
        PostDetails details = Utils.cursorToPostDetailsEntity(cursor);
        if (details != null) {
            List<String> imageKArr = Utils.getImageValues(details.getContent());
            String rightContent = details.getContent();

            Map<String, String> imageKV = details.getImages();
            ViewGroup.LayoutParams imageWHParams = null;

            LayoutInflater inflater = (LayoutInflater) SPApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            for (String imgK : imageKArr) {
                // get left of image
                String leftContent = StringUtils.substringBefore(rightContent, imgK).replaceAll("::", "");

                // grab right remaining chunk
                rightContent = StringUtils.substringAfter(rightContent, imgK).replaceAll("::", "");
                if (StringUtils.isNotBlank(leftContent)) {
                    TextView tView = (TextView) inflater.inflate(R.layout.post_textview, null);
//                     tView.setText(leftContent);
                    Utils.markSoftTagsWithinText(mView.getContext(), tView, leftContent);
                    // LEFT TEXT VIEW
                    addToView(tView);
                }


                imageWHParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500);
                SIMView imgView = new SIMView(mView.getContext());
                String imgUrl = imageKV.get(imgK);
                imgView.setLayoutParams(imageWHParams);
                imgView.setAspectRatio(1.33f);
                imgView.populateImageViewWithAdjustedAspect(imgUrl);
                // IMAGE
                addToView(imgView);
            }


            if (StringUtils.isNotBlank(rightContent)) {
                TextView tView = (TextView) inflater.inflate(R.layout.post_textview, null);
//                tView.setText(rightContent);
                // RIGHT TEXT VIEW
                Utils.markSoftTagsWithinText(mView.getContext(), tView, rightContent);
                addToView(tView);
            }

            // add tags at the end
            String tags = details.getTagsForDB();
            if (StringUtils.isNotBlank(tags)) {
                Utils.markTags(mView.getContext(), mTagsView, tags.split(","));
            } else {
                // still hold space
                mView.setVisibility(View.INVISIBLE);
            }
        }
    }

    void addToView(View v) {
        mViewGroup.addView(v);
    }
}
