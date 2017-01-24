package com.slim0926.todolist.helpers.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Paint;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.slim0926.todolist.R;
import com.slim0926.todolist.ToDoListContentProvider;
import com.slim0926.todolist.helpers.DatabaseHelper;
import com.slim0926.todolist.helpers.dragdropandswipe.ItemTouchHelperAdapter;
import com.slim0926.todolist.helpers.dragdropandswipe.ItemTouchHelperViewHolder;
import com.slim0926.todolist.helpers.dragdropandswipe.OnStartDragListener;
import com.slim0926.todolist.model.ToDoListItems;
import com.slim0926.todolist.ui.RecyclerListFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by sue on 12/28/16.
 */

public class RecyclerListAdapter extends RecyclerView.Adapter<RecyclerListAdapter.ItemViewHolder>
        implements ItemTouchHelperAdapter {

    private static final String DETAILS_FRAGMENT = "details_fragment";

    private OnStartDragListener mStartDragListener;
    private RecyclerListFragment.OnItemClickedListener mListener;
    private Context mContext;
    private ToDoListItems mItems;
    private int mID;
    private Spinner mSortBySpinner;

    public RecyclerListAdapter(Context context, OnStartDragListener startDragListener, ToDoListItems items,
                               RecyclerListFragment.OnItemClickedListener listener, Spinner sortBySpinner) {

        mContext = context;
        mStartDragListener = startDragListener;
        mItems = items;
        mListener = listener;
        mSortBySpinner = sortBySpinner;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerlist_item, parent, false);
        ItemViewHolder itemViewHolder = new ItemViewHolder(view);
        return itemViewHolder;
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, final int position) {
        holder.mtitleTextView.setText(mItems.getItem(position).getTitle());
        holder.mToDoItemCheckBox.setChecked(false);
        if (mItems.getItem(position).isChecked()) {
            holder.mToDoItemCheckBox.setChecked(true);
        }
        if (holder.mToDoItemCheckBox.isChecked()) {
            holder.mtitleTextView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.mtitleTextView.setPaintFlags(0);
        }

        holder.mToDoItemCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mItems.getItem(position).setChecked(holder.mToDoItemCheckBox.isChecked());
                notifyDataSetChanged();
                updateToDoListItem(position);
            }
        });

        holder.mtitleTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                holder.mtitleTextView.setScaleX(1.0f);
                holder.mtitleTextView.setScaleY(1.0f);
                holder.mtitleTextView.animate().scaleX(1.29f).scaleY(1.29f).start();

                int iD = mItems.getItem(position).getID();
                mListener.onTitleClicked(iD);
            }
        });

        holder.mReorderImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (MotionEventCompat.getActionMasked(motionEvent) == MotionEvent.ACTION_DOWN) {
                    mStartDragListener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems.getSize();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        mItems.moveItem(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);

        return false;
    }

    public void deleteItemFromDB(int position) {
        String selection = DatabaseHelper.COLUMN_ID + " = " + mItems.getItem(position).getID();
        mContext.getContentResolver().delete(ToDoListContentProvider.CONTENT_URI, selection, null);
    }

    public void updateToDoListItem(int position) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ISCHECKED, mItems.getItem(position).isChecked());
        values.put(DatabaseHelper.COLUMN_TITLE, mItems.getItem(position).getTitle());

        String selection = DatabaseHelper.COLUMN_ID + " = " + mItems.getItem(position).getID();
        mContext.getContentResolver().update(ToDoListContentProvider.CONTENT_URI, values, selection, null);
    }

    @Override
    public void onItemDismiss(int position) {
        deleteItemFromDB(position);
        mItems.deleteItem(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }

    @Override
    public void onItemMoved() {
        notifyDataSetChanged();
        mSortBySpinner.setSelection(0);
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

        @BindView(R.id.toDoListItemCheckBox) CheckBox mToDoItemCheckBox;
        public @BindView(R.id.titleTextView) TextView mtitleTextView;
        @BindView(R.id.reorderImageView) ImageView mReorderImageView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void onItemSelected() {

        }

        @Override
        public void onItemClear() {

        }
    }
}
