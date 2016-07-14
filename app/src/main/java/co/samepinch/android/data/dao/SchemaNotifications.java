package co.samepinch.android.data.dao;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import co.samepinch.android.app.helpers.AppConstants;

/**
 * Created by imaginationcoder on 7/4/15.
 */
public interface SchemaNotifications extends BaseColumns {
    // DB related stuff
    String TABLE_NAME = "notifications";
    String COLUMN_UID = "uid";
    String COLUMN_MSG = "message";

    String COLUMN_SRC = "source";
    String COLUMN_SRC_ID = "sourceId";
    String COLUMN_VIEWED = "viewed";

    String TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME
            + " ("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_UID + " TEXT NOT NULL UNIQUE, "
            + COLUMN_MSG + " TEXT, "
            + COLUMN_SRC + " TEXT, "
            + COLUMN_SRC_ID + " TEXT, "
            + COLUMN_VIEWED + " TEXT"
            + ")";

    String TABLE_DROP = "DROP TABLE IF EXISTS "
            + TABLE_NAME;

    String[] TAG_COLUMNS = new String[]{
            _ID, COLUMN_UID, COLUMN_MSG, COLUMN_SRC, COLUMN_SRC_ID, COLUMN_VIEWED
    };

    // provider related stuff
    String PATH_NOTIFICATIONS = "notifications";
    String CONTENT_AUTHORITY = AppConstants.API.CONTENT_AUTHORITY.getValue();
    String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTIFICATIONS;
    String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTIFICATIONS;

    Uri CONTENT_URI = Uri.withAppendedPath(SPContentProvider.CONTENT_URI, TABLE_NAME);
}
