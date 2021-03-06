package co.samepinch.android.app.helpers.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.atomic.AtomicInteger;

import co.samepinch.android.app.R;
import co.samepinch.android.app.SPApplication;

public class PostDetailsRVAdapter extends CursorRecyclerViewAdapter<PostDetailsRVHolder> {
    static String[] BG_COLORS = SPApplication.getContext().getResources().getStringArray(R.array.post_colors);
    private static AtomicInteger BG_COLOR_INDEX = new AtomicInteger();

    private final Context context;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;


    public PostDetailsRVAdapter(Context context, Cursor cursor) {
        super(cursor);
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }
        return 1;
    }

    @Override
    public PostDetailsRVHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        PostDetailsRVHolder viewHolder = null;
        switch (viewType) {
            case TYPE_HEADER:
                v = LayoutInflater.from(context)
                        .inflate(R.layout.post_details_content, parent, false);
                viewHolder = new PostContentRVHolder(v);
                break;

            case TYPE_ITEM:
                v = LayoutInflater.from(context)
                        .inflate(R.layout.post_comment_item, parent, false);
                viewHolder = new PostCommentsRVHolder(context, v);
                break;

            default:
                throw new IllegalStateException("un-known viewType=" + viewType);
        }

        // post bg color
//        BG_COLOR_INDEX.compareAndSet(BG_COLORS.length, 0);
//        v.setBackgroundColor(Color.parseColor(BG_COLORS[BG_COLOR_INDEX.getAndIncrement()]));

        return viewHolder;
    }

    @Override
    public void onBindViewHolderCursor(PostDetailsRVHolder viewHolder, Cursor cursor) {
        viewHolder.onBindViewHolderImpl(cursor);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

}
