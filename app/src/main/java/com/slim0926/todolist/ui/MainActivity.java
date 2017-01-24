package com.slim0926.todolist.ui;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
//import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.tasks.TasksScopes;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.api.services.tasks.model.Tasks;

import com.slim0926.todolist.R;
import com.slim0926.todolist.ToDoListContentProvider;
import com.slim0926.todolist.helpers.DateValidator;
import com.slim0926.todolist.helpers.adapters.GoogleTasklistAdapter;
import com.slim0926.todolist.model.GoogleTasklist;
import com.slim0926.todolist.model.ToDoListItem;
import com.slim0926.todolist.model.ToDoListItems;
import com.slim0926.todolist.helpers.DatabaseHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


import butterknife.OnClick;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements RecyclerListFragment.OnItemClickedListener,
        DetailsFragment.OnToDoListItemSaveInterface, EasyPermissions.PermissionCallbacks {

    private static final String RECYCLERLIST_FRAGMENT = "recyclerlist_fragment";
    private static final String DETAILS_FRAGMENT = "details_fragment";
    private static final String ITEM_ID = "item_id";
    private static final int YEARS_TO_BE_ADDED = 365000;
    private static final String GOOGLETASKLIST_DIALOG = "googletasklist_dialog";

    protected ToDoListItems mItems;
    private DetailsFragment mDetailsFragment;
    private RecyclerListFragment mRecyclerListFragment;
    private boolean mIsUpdate = false;

    // For connecting to Google Tasks
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "account_name";
    private static final String[] SCOPES = {TasksScopes.TASKS};

    GoogleAccountCredential mCredential;
    MakeRequestTask mMakeRequestTask;
    GoogleTasklist[] mGoogleTasklists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mItems = new ToDoListItems();
        updateItems();

        mRecyclerListFragment = (RecyclerListFragment) getSupportFragmentManager().findFragmentByTag(RECYCLERLIST_FRAGMENT);
        if (mRecyclerListFragment == null) {
            mRecyclerListFragment = new RecyclerListFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.slide_up_and_in, R.anim.slide_up_and_out);
            fragmentTransaction.add(R.id.placeHolder, mRecyclerListFragment, RECYCLERLIST_FRAGMENT);
            fragmentTransaction.commit();
        }

        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }

    @Override
    public void onToDoListItemAddButtonClicked() {
        mDetailsFragment = new DetailsFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_up_and_in, R.anim.slide_up_and_out);
        fragmentTransaction.replace(R.id.placeHolder, mDetailsFragment, DETAILS_FRAGMENT);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onTitleClicked(int iD) {
        mDetailsFragment = new DetailsFragment();

        Bundle args = new Bundle();
        args.putInt(ITEM_ID, iD);
        mDetailsFragment.setArguments(args);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_up_and_in, R.anim.slide_up_and_out);
        fragmentTransaction.replace(R.id.placeHolder, mDetailsFragment, DETAILS_FRAGMENT);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void addItemToDatabase(String title, String notes, String dueDate, String location,
                                   String priority, boolean checked) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TITLE, title);
        values.put(DatabaseHelper.COLUMN_NOTES, notes);
        values.put(DatabaseHelper.COLUMN_DUEDATE, dueDate);
        values.put(DatabaseHelper.COLUMN_LOCATION, location);
        values.put(DatabaseHelper.COLUMN_PRIORITY, priority);
        values.put(DatabaseHelper.COLUMN_ISCHECKED, checked);
        getContentResolver().insert(ToDoListContentProvider.CONTENT_URI, values);
    }

    public void updateItems() {
        Cursor cursor = getContentResolver().query(ToDoListContentProvider.CONTENT_URI, null, null, null, DatabaseHelper.COLUMN_ROWID);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String iD = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE));
                String notes = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTES));
                String dueDate = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DUEDATE));
                String location = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_LOCATION));
                String priority = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PRIORITY));
                String isChecked = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ISCHECKED));
                String rowID = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ROWID));
                //Toast.makeText(this, rowID + " rowID", Toast.LENGTH_SHORT).show();
                //int intRowID = Integer.parseInt(rowID);

                boolean isCheckedBool = false;
                if (!isChecked.equals("0")) {
                    isCheckedBool = true;
                }
                ToDoListItem item = createOneItem(title, notes, dueDate, location, priority, isCheckedBool);
                item.setID(Integer.parseInt(iD));
                //item.setRowID(intRowID);
                mItems.addItem(item);

            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    @Override
    public void onToDoListItemSaveButtonClicked() {
        DateValidator dateValidator = new DateValidator();

        if (mDetailsFragment.mDetailsTitleEdit.getText().toString().equals("")) {
            Toast.makeText(this, "You must enter a title. Try again.", Toast.LENGTH_LONG).show();
        } else if (!dateValidator.validate(mDetailsFragment.mDueDateEdit.getText().toString())
                && !mDetailsFragment.mDueDateEdit.getText().toString().equals("")) {
            Toast.makeText(this, "You must enter a date in the following format: 1/5/2017 " +
                            "or if you don't want to specify a date, you can just leave it blank.",
                    Toast.LENGTH_LONG).show();

        } else {
            String title = mDetailsFragment.mDetailsTitleEdit.getText().toString();
            String notes = mDetailsFragment.mNotesEdit.getText().toString();
            String dueDate = "";
            if (mDetailsFragment.mDueDateEdit.getText().toString().equals("")) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d/yyyy");
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, YEARS_TO_BE_ADDED);
                dueDate = simpleDateFormat.format(calendar.getTime());
            } else {
                dueDate = mDetailsFragment.mDueDateEdit.getText().toString();
            }
            String location = mDetailsFragment.mAddressEdit.getText().toString();
            String priority = mDetailsFragment.mPrioritySpinner.getSelectedItem().toString();
            boolean isChecked = mDetailsFragment.mDetailsCheckBox.isChecked();

            int pos = 0;
            for (int i = 0; i < mItems.getSize(); i++) {
                if (mItems.getItem(i).getTitle().equals(title)) {
                    mIsUpdate = true;
                    pos = i;
                }
            }

            if (mIsUpdate) {
                mItems.getItem(pos).setNotes(notes);
                mItems.getItem(pos).setDueDate(dueDate);
                mItems.getItem(pos).setLocation(location);
                mItems.getItem(pos).setPriority(priority);
                mItems.getItem(pos).setChecked(isChecked);
                int iD = mItems.getItem(pos).getID();

                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_NOTES, notes);
                values.put(DatabaseHelper.COLUMN_DUEDATE, dueDate);
                values.put(DatabaseHelper.COLUMN_LOCATION, location);
                values.put(DatabaseHelper.COLUMN_PRIORITY, priority);
                values.put(DatabaseHelper.COLUMN_ISCHECKED, isChecked);

                String selection = DatabaseHelper.COLUMN_ID + " = " + iD;
                getContentResolver().update(ToDoListContentProvider.CONTENT_URI, values, selection, null);
            } else {
                //int size = mItems.getSize();
                addItem(title, notes, dueDate, location, priority, isChecked);
                //Toast.makeText(this, "mItems.size: " + mItems.getSize(), Toast.LENGTH_SHORT).show();
            }
            onBackPressed();
        }
    }

    private void addItem(String title, String notes, String dueDate, String location, String priority, boolean isChecked) {
        addItemToDatabase(title, notes, dueDate, location, priority, isChecked);
        ToDoListItem item = createOneItem(title, notes, dueDate, location, priority, isChecked);

        String iD = "";
        Cursor cursor = getContentResolver().query(ToDoListContentProvider.CONTENT_URI, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            cursor.moveToLast();
            iD = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
            cursor.close();
        }

        item.setID(Integer.parseInt(iD));
        mItems.addItem(item);
    }

    private ToDoListItem createOneItem(String title, String notes, String dueDate, String location, String priority,
                                       boolean isChecked) {
        ToDoListItem item = new ToDoListItem();
        item.setTitle(title);
        item.setNotes(notes);
        item.setDueDate(dueDate);
        item.setLocation(location);
        item.setPriority(priority);
        item.setChecked(isChecked);
        //item.setRowID(rowID);
        return item;
    }

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.custom_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String menuItem = item.getTitle().toString();
                if (menuItem.equals("CLEAR CHECKED ITEMS")) {
                    for (int i = mItems.getSize() - 1; i >= 0; i--) {
                        if (mItems.getItem(i).isChecked()) {
                            mRecyclerListFragment.mAdapter.onItemDismiss(i);
                        }
                    }
                } else if (menuItem.equals("CONNECT TO GOOGLE TASKS")) {
//                    GoogleTasksFragment dialog = GoogleTasksFragment.newInstance();
//                    dialog.show(getSupportFragmentManager(), GOOGLETASKLIST_DIALOG);
                    //Toast.makeText(MainActivity.this, "I am here!", Toast.LENGTH_SHORT).show();
                    getResultsFromApi();
                }

                return true;
            }

        });
        MenuItem[] items = {popup.getMenu().getItem(0), popup.getMenu().getItem(1)};
        int i = 0;

        for (MenuItem menuItem : items) {
            SpannableString spanString = new SpannableString(popup.getMenu().getItem(i).getTitle().toString());
            int end = spanString.length();
            spanString.setSpan(new RelativeSizeSpan(0.8f), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            menuItem.setTitle(spanString);
            i++;
        }

        popup.show();
    }

    //////////////////////////////////
    // For connecting to Google Tasks
    //////////////////////////////////
    protected void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
        } else {
            mMakeRequestTask = new MainActivity.MakeRequestTask(mCredential);
            //mMakeRequestTask.execute();
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "This app requires Google Play Services." +
                                    " Please install Google Play Services on your device and relaunc this app.",
                            Toast.LENGTH_LONG).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    private void showGooglePlayServicesAvailabilityErrorDialog(int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    private class MakeRequestTask //extends AsyncTask<Void, Void, ToDoListItem[]>
            implements GoogleTasksFragment.OnItemClickedListener
    {
        private static final int GET_TASKLISTS_COMPLETE = 0;
        private static final int GET_TASKS_COMPLETE = 1;

        private com.google.api.services.tasks.Tasks mService = null;
        private Exception mLastError = null;
        private String mTasklistID;
        GoogleTasksFragment mDialog;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.tasks.Tasks.Builder(transport, jsonFactory, credential)
                    .setApplicationName("ToDoList")
                    .build();
            doBackgroundTasklists();
        }

//        @Override
//        protected ToDoListItem[] doInBackground(Void... voids) {
//            try {
//                return getDataFromApi();
//            } catch (Exception e) {
//                mLastError = e;
//                mLastError.printStackTrace();
//                cancel(true);
//                return null;
//            }
//        }

        private void doBackgroundTasklists() {
            Thread backgroundThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    TaskLists result = null;
                    try {
                        result = mService.tasklists().list().setMaxResults(10L).execute();


                    } catch (IOException e) {

                    }
                    List<TaskList> taskLists = result.getItems();
                    GoogleTasklist[] googleTasklists = new GoogleTasklist[taskLists.size()];
                    if (taskLists != null) {
                        int i = 0;
                        for (TaskList tasklist : taskLists) {
                            googleTasklists[i] = new GoogleTasklist();
                            googleTasklists[i].setTitle(tasklist.getTitle());
                            googleTasklists[i].setID(tasklist.getId());
                            i++;
                        }
                    }
                    mGoogleTasklists = Arrays.copyOf(googleTasklists, googleTasklists.length);
                    mDialog = GoogleTasksFragment.newInstance();
                    mDialog.show(getSupportFragmentManager(), GOOGLETASKLIST_DIALOG);
                }
            });
            backgroundThread.start();
//            try {
//                backgroundThread.join();
//            } catch (InterruptedException e) {
//
//            }
        }



        private void doDownloadTasks() {
            Thread backgroundThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mTasklistID = mDialog.mAdapter.mTasklistID;
//                    if (mTasklistID == null || mTasklistID.equals("")) {
//                    } else {

                        Tasks tasks = null;
                        try {
                            tasks = mService.tasks().list(mTasklistID).execute();
                        } catch (IOException e) {

                        }

                        boolean isDuplicate = false;
                        for (Task task : tasks.getItems()) {
                            for (ToDoListItem item : mItems.getToDoList()) {
                                if (task.getTitle().equals(item.getTitle())) {
                                    isDuplicate = true;
                                }
                            }
                            if (!task.getTitle().equals("") && !isDuplicate) {

                                String title = task.getTitle();
                                String notes = "";
                                String dueDate = "";

                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d/yyyy");
                                Calendar calendar = Calendar.getInstance();
                                calendar.add(Calendar.DATE, YEARS_TO_BE_ADDED);
                                dueDate = simpleDateFormat.format(calendar.getTime());

                                String location = "";
                                String priority = "None";
                                boolean isChecked = false;

                                addItem(title, notes, dueDate, location, priority, isChecked);
                            }
                            isDuplicate = false;
                        }
                    //}
                    //ToDoListItem[] listItems = mItems.getToDoList().toArray(new ToDoListItem[mItems.getSize()]);
                }
            });
            backgroundThread.start();
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Log.e("Download", e.getMessage());
            }
        }

//        private Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case GET_TASKLISTS_COMPLETE:
//                    doBackgroundTasks();
//                    break;
//                case GET_TASKS_COMPLETE:
//
//                    break;
//            }
//        }
//    }

//        private ToDoListItem[] getDataFromApi() throws IOException {
//            //List<String> taskListInfo = new ArrayList<String>();
//            TaskLists result = mService.tasklists().list().setMaxResults(10L).execute();
//            List<TaskList> taskLists = result.getItems();
//            final GoogleTasklist[] googleTasklists = new GoogleTasklist[taskLists.size()];
//            if (taskLists != null) {
//                int i = 0;
//                for (TaskList tasklist : taskLists) {
//                    googleTasklists[i] = new GoogleTasklist();
//                    googleTasklists[i].setTitle(tasklist.getTitle());
//                    googleTasklists[i].setID(tasklist.getId());
//                    i++;
//                }
//            }
//
//            mGoogleTasklists = Arrays.copyOf(googleTasklists, googleTasklists.length);
//            GoogleTasksFragment dialog = GoogleTasksFragment.newInstance();
//            dialog.show(getSupportFragmentManager(), GOOGLETASKLIST_DIALOG);
//
//            dialog.mDownloadTaskListButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    for (GoogleTasklist tasklist : mGoogleTasklists) {
//                        if (tasklist.isSelected()) {
//                            mTasklistID = tasklist.getID();
//                        }
//                    }
//                }
//            });
//
//            Tasks tasks = mService.tasks().list(mTasklistID).execute();
//
//            for (Task task : tasks.getItems()) {
//                String title = task.getTitle();
//                String notes = "";
//                String dueDate = "";
//
//                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d/yyyy");
//                Calendar calendar = Calendar.getInstance();
//                calendar.add(Calendar.DATE, YEARS_TO_BE_ADDED);
//                dueDate = simpleDateFormat.format(calendar.getTime());
//
//                String location = "";
//                String priority = "None";
//                boolean isChecked = false;
//
//                addItemToDatabase(title, notes, dueDate, location, priority, isChecked);
//                ToDoListItem item = createOneItem(title, notes, dueDate, location, priority, isChecked);
//
//                String iD = "";
//                Cursor cursor = getContentResolver().query(ToDoListContentProvider.CONTENT_URI, null, null, null, null);
//                if (cursor != null && cursor.moveToFirst()) {
//                    cursor.moveToLast();
//                    iD = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID));
//                    cursor.close();
//                }
//
//                item.setID(Integer.parseInt(iD));
//                mItems.addItem(item);
//
//            }
//            ToDoListItem[] listItems = mItems.getToDoList().toArray(new ToDoListItem[mItems.getSize()]);
//            return listItems;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }
//
//        @Override
//        protected void onPostExecute(ToDoListItem[] items) {
//            // super.onPostExecute(tasklists);
//            if (items == null || items.length == 0) {
//                Toast.makeText(MainActivity.this, "No results returned.", Toast.LENGTH_SHORT).show();
//            } else {
//                GoogleTasklistAdapter adapter = new GoogleTasklistAdapter(MainActivity.this, tasklists);
//                mGoogleTasksRecyclerview.setAdapter(adapter);
//
//                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(GoogleTasksActivity.this);
//                mGoogleTasksRecyclerview.setLayoutManager(layoutManager);
//
//                mGoogleTasksRecyclerview.setHasFixedSize(true);
//                onBackPressed();
//            }
//        }
//
//        @Override
//        protected void onCancelled() {
//            if (mLastError != null) {
//                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
//                } else if (mLastError instanceof UserRecoverableAuthIOException) {
//                    startActivityForResult(
//                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                            REQUEST_AUTHORIZATION);
//                } else {
//                    Toast.makeText(MainActivity.this, "The following error occurred:\n" + mLastError.getMessage(),
//                            Toast.LENGTH_LONG).show();
//                }
//            } else {
//                Toast.makeText(MainActivity.this, "Request cancelled.", Toast.LENGTH_SHORT).show();
//            }
//        }

        public void onDownloadTasksDone() {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.placeHolder, new RecyclerListFragment())
                    .commit();
        }

        @Override
        public void onDownloadButtonClicked() throws IOException {
            doDownloadTasks();

                onDownloadTasksDone();
                mDialog.dismiss();

        }

        @Override
        public void onUploadButtonClicked() {
            doUploadTasks();
            mDialog.dismiss();
        }

        private void doUploadTasks() {
            Thread backgroundThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mTasklistID = mDialog.mAdapter.mTasklistID;
                    Tasks tasks = null;
                    try {
                        tasks = mService.tasks().list(mTasklistID).execute();
                    } catch (IOException e) {

                    }
                    boolean isDuplicate = false;
                    for (ToDoListItem item : mItems.getToDoList()) {
                        for (Task googleTask : tasks.getItems()) {
                            if (item.getTitle().equals(googleTask.getTitle())) {
                                isDuplicate = true;
                            }
                        }
                        if (!isDuplicate) {

                            Task task = new Task();
                            task.setTitle(item.getTitle());
                            try {
                                mService.tasks().insert(mTasklistID, task).execute();
                            } catch (IOException e) {

                            }
                        }
                        isDuplicate = false;
                    }
                }
            });
            backgroundThread.start();
            try {
                backgroundThread.join();
            } catch (InterruptedException e) {
                Log.e("Upload", e.getMessage());
            }
        }
    }

}
