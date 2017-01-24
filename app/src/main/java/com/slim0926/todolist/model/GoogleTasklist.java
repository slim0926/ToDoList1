package com.slim0926.todolist.model;

/**
 * Created by sue on 1/16/17.
 */

public class GoogleTasklist {
    private String mTitle;
    private String mID;
    private boolean mIsSelected;

    public GoogleTasklist() {

    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getID() {
        return mID;
    }

    public void setID(String ID) {
        mID = ID;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean selected) {
        mIsSelected = selected;
    }

}
