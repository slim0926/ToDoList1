package com.slim0926.todolist.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by sue on 12/28/16.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE = "ToDoListDatabase";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TODOLIST = "to_do_list";
    public static final String COLUMN_ID = "_ID";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_NOTES = "notes";
    public static final String COLUMN_DUEDATE = "due_date";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_PRIORITY = "priority";
    public static final String COLUMN_ISCHECKED = "is_checked";
    public static final String COLUMN_ROWID = "rowID";


    private SQLiteDatabase mDatabase;

    public DatabaseHelper(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
        mDatabase = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL = " CREATE TABLE " + TABLE_TODOLIST + " ( " +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_NOTES + " TEXT, " +
                COLUMN_DUEDATE + " DATE, " +
                COLUMN_LOCATION + " TEXT, " +
                COLUMN_PRIORITY + " TEXT, " +
                COLUMN_ISCHECKED + " BOOLEAN NOT NULL, " +
                COLUMN_ROWID + " INTEGER);";
        db.execSQL(SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }
}
