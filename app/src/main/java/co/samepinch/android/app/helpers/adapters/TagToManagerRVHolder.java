package co.samepinch.android.app.helpers.adapters;


import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckedTextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;

import co.samepinch.android.app.R;
import co.samepinch.android.app.helpers.Utils;
import co.samepinch.android.data.dao.SchemaTags;

public class TagToManagerRVHolder extends RecyclerView.ViewHolder {
    public static final String TAG = "TagToManagerRVHolder";

    SimpleDraweeView mTagImg;
    CheckedTextView mTagName;

    public TagToManagerRVHolder(View itemView) {
        super(itemView);
        mTagImg = (SimpleDraweeView) itemView.findViewById(R.id.tag_image);
        mTagName = (CheckedTextView) itemView.findViewById(R.id.tag_id);
    }

    void onBindViewHolderImpl(String currUserId, Cursor cursor) {
        int imgIdx = cursor.getColumnIndex(SchemaTags.COLUMN_IMAGE);
        String imgStr = imgIdx > -1 ? cursor.getString(imgIdx) : null;
        if (StringUtils.isNotBlank(imgStr)) {
            Utils.setupLoadingImageHolder(mTagImg, imgStr);
        }
        mTagName.setText(cursor.getString(cursor.getColumnIndex(SchemaTags.COLUMN_NAME)));

        // checked state
        String tagUserId = cursor.getString(cursor.getColumnIndex(SchemaTags.COLUMN_USER_ID));
        mTagName.setChecked(currUserId != null ? StringUtils.equals(currUserId, tagUserId) : false);
    }
}
