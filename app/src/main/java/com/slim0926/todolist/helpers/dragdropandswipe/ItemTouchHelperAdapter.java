package com.slim0926.todolist.helpers.dragdropandswipe;

/**
 * Created by sue on 12/28/16.
 */
public interface ItemTouchHelperAdapter {
    boolean onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);

    void onItemMoved();

}
