package droid.samepinch.co.app.helpers.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import droid.samepinch.co.app.R;

/**
 * Created by imaginationcoder on 7/27/15.
 */
public class PostDetailsRVAdapter extends CursorRecyclerViewAdapter<PostDetailsRVHolder> {
    private final Context context;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;


    public PostDetailsRVAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }else if(isPositionFooter(position)){
            return TYPE_FOOTER;
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
                viewHolder = new PostCommentsRVHolder(v);
                break;

            case TYPE_FOOTER:
                v = LayoutInflater.from(context)
                        .inflate(R.layout.post_details_comment_add, parent, false);
                viewHolder = new PostCommentAddRVHolder(v);
                break;

            default:
                Thread.dumpStack();
                throw new IllegalStateException("un-known viewType=" + viewType);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(PostDetailsRVHolder viewHolder, Cursor cursor) {
        viewHolder.onBindViewHolderImpl(cursor);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    private boolean isPositionFooter(int position) {
        return position == (getCursor().getCount()-1);
    }

}
