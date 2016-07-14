package co.samepinch.android.app.helpers.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import co.samepinch.android.app.R;
import co.samepinch.android.data.dao.SchemaNotifications;

/**
 * Created by imaginationcoder on 7/27/15.
 */
public class NotifsRVAdapter extends CursorRecyclerViewAdapter<NotifsRVHolder> {
    private final Context context;

    public NotifsRVAdapter(Context context, Cursor cursor) {
        super(cursor);
        this.context = context;
    }

    @Override
    public NotifsRVHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.notif_element, parent, false);
        NotifsRVHolder viewHolder = new NotifsRVHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolderCursor(final NotifsRVHolder viewHolder, Cursor cursor) {
        viewHolder.onBindViewHolderImpl(cursor);
        String srcId = cursor.getString(cursor.getColumnIndex(SchemaNotifications.COLUMN_SRC_ID));
    }


    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    public interface ItemEventListener<T> {
        void onChange(T t);
    }
}
