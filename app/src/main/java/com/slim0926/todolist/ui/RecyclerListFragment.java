package com.slim0926.todolist.ui;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.slim0926.todolist.R;

import com.slim0926.todolist.ToDoListContentProvider;
import com.slim0926.todolist.model.ToDoListItem;
import com.slim0926.todolist.model.ToDoListItems;
import com.slim0926.todolist.helpers.DatabaseHelper;
import com.slim0926.todolist.helpers.adapters.RecyclerListAdapter;
import com.slim0926.todolist.helpers.dragdropandswipe.OnStartDragListener;
import com.slim0926.todolist.helpers.dragdropandswipe.SimpleItemTouchHelperCallback;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by sue on 12/28/16.
 */

public class RecyclerListFragment extends Fragment implements OnStartDragListener {

    private ItemTouchHelper mItemTouchHelper;
    protected RecyclerListAdapter mAdapter;

    @BindView(R.id.addImageButton) ImageButton mAddButton;
    @BindView(R.id.sortBySpinner) Spinner mSortBySpinner;
    @BindView(R.id.menuImageButton) ImageButton mMenuButton;

    public interface OnItemClickedListener {
        void onToDoListItemAddButtonClicked();
        void onTitleClicked(int position);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // OnItemClickedListener listener = (OnItemClickedListener) getActivity();
        View view = inflater.inflate(R.layout.fragment_recyclerlist, container, false);
        ButterKnife.bind(this, view);

        final String[] sortByArray = new String[] {"None", "Title", "Due Date", "Priority"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item,
                sortByArray);
        mSortBySpinner.setAdapter(adapter);

        final OnItemClickedListener listener = (OnItemClickedListener) getActivity();

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.toDoListRecyclerView);
        mAdapter = new RecyclerListAdapter(getActivity(), this, ((MainActivity) getActivity()).mItems,
                listener, mSortBySpinner);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        //final OnItemClickedListener mainActivity = (OnItemClickedListener) getActivity();
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onToDoListItemAddButtonClicked();
            }
        });

        mSortBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        Collections.sort(((MainActivity)getActivity()).mItems.getToDoList(), new ToDoListItem.ItemTitleComparator());
                        mAdapter.notifyDataSetChanged();
                        break;
                    case 2:
                        Collections.sort(((MainActivity)getActivity()).mItems.getToDoList(), new ToDoListItem.ItemDuedateComparator());
                        mAdapter.notifyDataSetChanged();
                        break;
                    case 3:
                        Collections.sort(((MainActivity)getActivity()).mItems.getToDoList(), new ToDoListItem.ItemPriorityComparator());
                        mAdapter.notifyDataSetChanged();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)getActivity()).showPopup(view);
            }
        });

        return view;
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void onPause() {
        super.onPause();

        ToDoListItems items = ((MainActivity)getActivity()).mItems;
        if (items != null) {
            for (int i = 0; i < items.getSize(); i++) {
                //Toast.makeText(getActivity(), items.getItem(i).getRowID() + " rowID", Toast.LENGTH_SHORT).show();
                items.getItem(i).setRowID(i);
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_ROWID, items.getItem(i).getRowID());
                //Toast.makeText(getActivity(), items.getItem(i).getRowID() + " rowID After " + items.getItem(i).getTitle(), Toast.LENGTH_SHORT).show();
                //Toast.makeText(getActivity(), items.getItem(i).getID() + " ID", Toast.LENGTH_SHORT).show();
                String selection = DatabaseHelper.COLUMN_ID + " = " + items.getItem(i).getID();
                //Toast.makeText(getActivity(), "ID = " + items.getItem(i).getID(), Toast.LENGTH_SHORT).show();
                getActivity().getContentResolver().update(ToDoListContentProvider.CONTENT_URI, values, selection, null);
            }
        }
    }
}
