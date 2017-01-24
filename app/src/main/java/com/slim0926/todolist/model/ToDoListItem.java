package com.slim0926.todolist.model;

import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by sue on 12/31/16.
 */

public class ToDoListItem {
    private int mID;
    private String mTitle;
    private String mNotes;
    private String mDueDate;
    private String mLocation;
    private String mPriority;
    private boolean mIsChecked;
    private int mRowID;

    public int getID() {
        return mID;
    }

    public void setID(int ID) {
        mID = ID;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getNotes() {
        return mNotes;
    }

    public void setNotes(String notes) {
        mNotes = notes;
    }

    public String getDueDate() {
        return mDueDate;
    }

    public void setDueDate(String dueDate) {
        mDueDate = dueDate;
    }

    public Date getDueDateInDateFormat() {
        SimpleDateFormat formatter = new SimpleDateFormat("M/d/yyyy");
        Date dueDate = new Date();
        try {
            dueDate = formatter.parse(mDueDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dueDate;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public String getPriority() {
        return mPriority;
    }

    public void setPriority(String priority) {
        mPriority = priority;
    }

    public int getPriorityNum() {
        int priorityNum = 0;
        switch (mPriority) {
            case "None":
                priorityNum = 3;
                break;
            case "High":
                priorityNum = 0;
                break;
            case "Medium":
                priorityNum = 1;
                break;
            case "Low":
                priorityNum = 2;
                break;
        }
        return priorityNum;
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    public void setChecked(boolean checked) {
        mIsChecked = checked;
    }

    public int getRowID() {
        return mRowID;
    }

    public void setRowID(int rowID) {
        mRowID = rowID;
    }

    public static class ItemPriorityComparator implements Comparator<ToDoListItem> {

        @Override
        public int compare(ToDoListItem item1, ToDoListItem item2) {
            return item1.getPriorityNum() - item2.getPriorityNum();
        }
    }

    public static class ItemDuedateComparator implements Comparator<ToDoListItem> {

        @Override
        public int compare(ToDoListItem item1, ToDoListItem item2) {
            return item1.getDueDateInDateFormat().compareTo(item2.getDueDateInDateFormat());
        }
    }

    public static class ItemTitleComparator implements Comparator<ToDoListItem> {

        @Override
        public int compare(ToDoListItem item1, ToDoListItem item2) {
            return item1.mTitle.compareTo(item2.mTitle);
        }
    }
}
