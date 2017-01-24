package com.slim0926.todolist;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteAbortException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.slim0926.todolist.helpers.DatabaseHelper;

/**
 * Created by sue on 12/28/16.
 */

public class ToDoListContentProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://com.slim0926.todolist.provider/" +
            DatabaseHelper.TABLE_TODOLIST);
    private Context mContext;
    private SQLiteDatabase mDatabase;

    @Override
    public boolean onCreate() {
        mContext = getContext();
        DatabaseHelper databaseHelper = new DatabaseHelper(mContext);
        mDatabase = databaseHelper.getWritableDatabase();
        return mDatabase != null;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DatabaseHelper.TABLE_TODOLIST);
        return builder.query(mDatabase, columns, selection, selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        long rowId = mDatabase.insert(DatabaseHelper.TABLE_TODOLIST, null, contentValues);
        if (rowId > -1) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
            mContext.getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        throw new SQLiteAbortException("Insert failed for Uri: " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        count = mDatabase.delete(DatabaseHelper.TABLE_TODOLIST, selection, selectionArgs);
        mContext.getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        int count = 0;
        count = mDatabase.update(DatabaseHelper.TABLE_TODOLIST, contentValues, selection, selectionArgs);
        mContext.getContentResolver().notifyChange(uri, null);
        return count;
    }
}
