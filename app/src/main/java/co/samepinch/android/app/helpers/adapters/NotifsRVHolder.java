package co.samepinch.android.app.helpers.adapters;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import co.samepinch.android.app.R;
import co.samepinch.android.app.helpers.AppConstants;
import co.samepinch.android.app.helpers.intent.AllNotificationsUpdateService;
import co.samepinch.android.data.dao.SchemaNotifications;

public class NotifsRVHolder extends RecyclerView.ViewHolder {
    private final Context mContext;

    public NotifsRVHolder(View itemView) {
        super(itemView);
        mContext = itemView.getContext();
        setIsRecyclable(true);
    }

    void onBindViewHolderImpl(final Cursor cursor) {
        final TextView mMsg = (TextView) itemView.findViewById(R.id.notif_msg);
        String viewed = cursor.getString(cursor.getColumnIndex(SchemaNotifications.COLUMN_VIEWED));
        if(StringUtils.isNotBlank(viewed) && Boolean.getBoolean(viewed) == true){
            mMsg.setEnabled(Boolean.FALSE);
        }

        final String uid = cursor.getString(cursor.getColumnIndex(SchemaNotifications.COLUMN_UID));
        final String src = cursor.getString(cursor.getColumnIndex(SchemaNotifications.COLUMN_SRC));
        final String srcID = cursor.getString(cursor.getColumnIndex(SchemaNotifications.COLUMN_SRC_ID));
        mMsg.setTag(uid);

        mMsg.setText(cursor.getString(cursor.getColumnIndex(SchemaNotifications.COLUMN_MSG)));
        mMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMsg.setEnabled(Boolean.FALSE);

                Bundle iArgs = new Bundle();
                iArgs.putString(AppConstants.K.NOTIF.name(), uid);
                iArgs.putString(AppConstants.K.SRC.name(), src);
                iArgs.putString(AppConstants.K.SRC_ID.name(), srcID);
                // sync remote service
                Intent dataSyncIntent =
                        new Intent(mContext, AllNotificationsUpdateService.class);
                dataSyncIntent.putExtras(iArgs);

                // invoke both
                mContext.startService(dataSyncIntent);
            }
        });
    }

}
