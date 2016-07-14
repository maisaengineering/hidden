package co.samepinch.android.app.helpers.adapters;


import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import co.samepinch.android.app.R;
import co.samepinch.android.data.dao.SchemaNotifications;

public class NotifsRVHolder extends RecyclerView.ViewHolder {
    TextView mMsg;

    public NotifsRVHolder(View itemView) {
        super(itemView);
        mMsg = (TextView) itemView.findViewById(R.id.notif_msg);
        setIsRecyclable(false);
    }

    void onBindViewHolderImpl(Cursor cursor) {
//        int imgIdx = cursor.getColumnIndex(SchemaTags.COLUMN_IMAGE);
//        String imgStr = imgIdx > -1 ? cursor.getString(imgIdx) : null;
//        if (StringUtils.isNotBlank(imgStr)) {
//            Utils.setupLoadingImageHolder(mTagImage, imgStr);
//        }
        mMsg.setText(cursor.getString(cursor.getColumnIndex(SchemaNotifications.COLUMN_MSG)));
    }

}
