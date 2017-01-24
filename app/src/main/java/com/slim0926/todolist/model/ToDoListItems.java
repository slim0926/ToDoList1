package com.slim0926.todolist.model;

import com.slim0926.todolist.model.ToDoListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sue on 12/31/16.
 */

public class ToDoListItems {
    private List<ToDoListItem> mToDoList;

    public ToDoListItems() {
        mToDoList = new ArrayList<>();
    }

    public List<ToDoListItem> getToDoList() {
        return mToDoList;
    }

    public void setToDoList(List<ToDoListItem> toDoList) {
        mToDoList = toDoList;
    }

    public void addItem(ToDoListItem item) {
        mToDoList.add(item);
    }

    public ToDoListItem getItem(int index) {
        return mToDoList.get(index);
    }

    public void deleteItem(int position) {
        mToDoList.remove(position);
    }

    public void moveItem(int oldPosition, int newPosition) {
        Collections.swap(mToDoList, oldPosition, newPosition);
    }

    public int getSize() {
        return mToDoList.size();
    }
}
