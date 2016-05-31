package com.jlcsoftware.database;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Jeff on 06-May-16.
 * <p/>
 * Our SQLlite database
 */
public class Database extends SQLiteOpenHelper {

    public static String NAME = "callRecorder";
    public static int VERSION = 1;

    String CREATE_CALL_RECORDS_TABLE = "CREATE TABLE records(_id INTEGER PRIMARY KEY, phone_number TEXT, outgoing INTEGER, start_date_time INTEGER, end_date_time INTEGER, path_to_recording TEXT, keep INTEGER DEFAULT 0, backup_state INTEGER DEFAULT 0 )";
    public static String CALL_RECORDS_TABLE = "records";
    public static String CALL_RECORDS_TABLE_ID = "_id"; // only because of https://developer.android.com/reference/android/widget/CursorAdapter.html
    public static String CALL_RECORDS_TABLE_PHONE_NUMBER = "phone_number";
    public static String CALL_RECORDS_TABLE_OUTGOING = "outgoing";
    public static String CALL_RECORDS_TABLE_START_DATE = "start_date_time";
    public static String CALL_RECORDS_TABLE_END_DATE = "end_date_time";
    public static String CALL_RECORDS_TABLE_RECORDING_PATH = "path_to_recording";
    public static String CALL_RECORDS_TABLE_KEEP = "keep";
    public static String CALL_RECORDS_BACKUP_STATE = "backup_state";

    public static String CREATE_WHITELIST_TABLE = "CREATE TABLE whitelist( _id INTEGER PRIMARY KEY, contact_id TEXT, record INTEGER )";
    public static String WHITELIST_TABLE = "whitelist";
    public static String WHITELIST_TABLE_ID = "_id"; // only because of https://developer.android.com/reference/android/widget/CursorAdapter.html
    public static String WHITELIST_TABLE_CONTACT_ID = "contact_id";
    public static String WHITELIST_TABLE_RECORD = "record";

    private static Database instance;

    public static synchronized Database getInstance(Context context) {
        if (instance == null) {
            instance = new Database(context.getApplicationContext());
        }
        return instance;
    }

    private Database(Context context) {
        super(context, Database.NAME, null, Database.VERSION);
    }

    @Override
    public synchronized void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_CALL_RECORDS_TABLE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            db.execSQL(CREATE_WHITELIST_TABLE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
        }
    }


    private CallLog getCallLogFrom(Cursor cursor) {
        CallLog phoneCall = new CallLog();
        phoneCall.isNew = false;

        // String[] columnNames = cursor.getColumnNames();

        int index = cursor.getColumnIndex(CALL_RECORDS_TABLE_ID);
        phoneCall.getContent().put(CALL_RECORDS_TABLE_ID, cursor.getInt(index));

        index = cursor.getColumnIndex(CALL_RECORDS_TABLE_PHONE_NUMBER);
        phoneCall.getContent().put(CALL_RECORDS_TABLE_PHONE_NUMBER, cursor.getString(index));

        index = cursor.getColumnIndex(CALL_RECORDS_TABLE_OUTGOING);
        phoneCall.getContent().put(CALL_RECORDS_TABLE_OUTGOING, cursor.getInt(index));

        index = cursor.getColumnIndex(CALL_RECORDS_TABLE_START_DATE);
        phoneCall.getContent().put(CALL_RECORDS_TABLE_START_DATE, cursor.getLong(index));

        index = cursor.getColumnIndex(CALL_RECORDS_TABLE_END_DATE);
        phoneCall.getContent().put(CALL_RECORDS_TABLE_END_DATE, cursor.getLong(index));

        index = cursor.getColumnIndex(CALL_RECORDS_TABLE_RECORDING_PATH);
        phoneCall.getContent().put(CALL_RECORDS_TABLE_RECORDING_PATH, cursor.getString(index));

        index = cursor.getColumnIndex(CALL_RECORDS_TABLE_KEEP);
        phoneCall.getContent().put(CALL_RECORDS_TABLE_KEEP, cursor.getInt(index));

        index = cursor.getColumnIndex(CALL_RECORDS_BACKUP_STATE);
        phoneCall.getContent().put(CALL_RECORDS_BACKUP_STATE, cursor.getInt(index));

        return phoneCall;
    }


    public synchronized ArrayList<CallLog> getAllCalls() {
        ArrayList<CallLog> array_list = new ArrayList<CallLog>();

        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from " + Database.CALL_RECORDS_TABLE, null);
            cursor.moveToFirst();
            while (cursor.isAfterLast() == false) {
                CallLog phoneCall = getCallLogFrom(cursor);
                array_list.add(phoneCall);
                cursor.moveToNext();
            }
            return array_list;
        } finally {
            db.close();
        }
    }


    public synchronized ArrayList<CallLog> getAllCalls(boolean outgoing) {
        ArrayList<CallLog> array_list = new ArrayList<CallLog>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            Cursor cursor = db.rawQuery("select * from " + Database.CALL_RECORDS_TABLE + " where " + Database.CALL_RECORDS_TABLE_OUTGOING + "=" + (outgoing ? "1" : "0"), null);
            cursor.moveToFirst();
            while (cursor.isAfterLast() == false) {
                CallLog phoneCall = getCallLogFrom(cursor);
                array_list.add(phoneCall);
                cursor.moveToNext();
            }
            return array_list;
        } finally {
            db.close();
        }
    }

    public synchronized boolean addCall(CallLog phoneCall) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            if (phoneCall.isNew) {
                long rowId = db.insert(Database.CALL_RECORDS_TABLE, null, phoneCall.getContent());
                // rowID is and Alias for _ID  see: http://www.sqlite.org/autoinc.html
                phoneCall.getContent().put(Database.CALL_RECORDS_TABLE_ID, rowId);
            } else {
                db.update(Database.CALL_RECORDS_TABLE, phoneCall.getContent(), CALL_RECORDS_TABLE_ID + "=" + phoneCall.getId(), null);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
    }

    public synchronized boolean updateCall(CallLog phoneCall) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.update(Database.CALL_RECORDS_TABLE, phoneCall.getContent(), "id = ?", new String[]{Integer.toString(phoneCall.getId())});
            return true;
        } finally {
            db.close();
        }
    }

    public synchronized int count() {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            int numRows = (int) DatabaseUtils.queryNumEntries(db, Database.CALL_RECORDS_TABLE);
            return numRows;
        } finally {
            db.close();
        }
    }

    public synchronized CallLog getCall(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from " + Database.CALL_RECORDS_TABLE + " where " + Database.CALL_RECORDS_TABLE_ID + "=" + id, null);
            if (!cursor.moveToFirst()) return null; // does not exist
            return getCallLogFrom(cursor);
        } finally {
            db.close();
        }
    }

    public synchronized void removeCall(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from " + Database.CALL_RECORDS_TABLE + " where " + Database.CALL_RECORDS_TABLE_ID + "=" + id, null);
            if (!cursor.moveToFirst()) return; // doesn't exist
            CallLog call = getCallLogFrom(cursor);
            String path = call.getPathToRecording();
            try {
                if (null != path)
                    new File(path).delete();
            } catch (Exception e) {

            }
            db.execSQL("Delete from " + Database.CALL_RECORDS_TABLE + " where " + Database.CALL_RECORDS_TABLE_ID + "=" + id);
        } finally {
            db.close();
        }
    }


    public synchronized void removeAllCalls(boolean includeKept) {
        final ArrayList<CallLog> allCalls = getAllCalls();
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            for (CallLog call : allCalls) {
                if (includeKept || !call.isKept()) {
                    try {
                        new File(call.getPathToRecording()).delete();
                    } catch (Exception e) {
                    }
                    try {
                        db.execSQL("Delete from " + Database.CALL_RECORDS_TABLE + " where " + Database.CALL_RECORDS_TABLE_ID + "=" + call.getId());
                    } catch (Exception e) {
                    }
                }
            }
            // db.delete(Database.CALL_RECORDS_TABLE, null, null);
        } finally {
            db.close();
        }
    }


    private synchronized Whitelist getWhitelistFrom(Cursor cursor) {
        Whitelist whitelist = new Whitelist();

        int index = cursor.getColumnIndex(Database.WHITELIST_TABLE_ID);
        whitelist.getContent().put(Database.WHITELIST_TABLE_ID, cursor.getInt(index));

        index = cursor.getColumnIndex(Database.WHITELIST_TABLE_CONTACT_ID);
        whitelist.getContent().put(Database.WHITELIST_TABLE_CONTACT_ID, cursor.getString(index));

        index = cursor.getColumnIndex(Database.WHITELIST_TABLE_RECORD);
        whitelist.getContent().put(Database.WHITELIST_TABLE_RECORD, cursor.getString(index));

        return whitelist;
    }


    public synchronized ArrayList<Whitelist> getAllWhitelist() {
        ArrayList<Whitelist> array_list = new ArrayList<Whitelist>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from " + Database.WHITELIST_TABLE, null);
            String[] columns = cursor.getColumnNames();

            cursor.moveToFirst();
            while (cursor.isAfterLast() == false) {
                Whitelist contact = getWhitelistFrom(cursor);
                array_list.add(contact);
                cursor.moveToNext();
            }
            return array_list;
        } finally {
            db.close();
        }
    }

    public synchronized Whitelist getContact(String contactId) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from " + Database.WHITELIST_TABLE + " where " + Database.WHITELIST_TABLE_CONTACT_ID + " = " + contactId + "", null);
            if (!cursor.moveToFirst()) return null; // does not exist
            return getWhitelistFrom(cursor);
        } finally {
            db.close();
        }
    }


    public synchronized boolean addWhitelist(Whitelist contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.insert(Database.WHITELIST_TABLE, null, contact.getContent());
            return true;
        } finally {
            db.close();
        }
    }


    public synchronized void removeWhiteList(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            db.execSQL("Delete from " + Database.WHITELIST_TABLE + " where " + Database.WHITELIST_TABLE_ID + "=" + id + "");
        } finally {
            db.close();
        }
    }

    public synchronized static boolean isWhitelisted(Context context, String phoneNumber) {
        // define the columns the query should return
        String[] projection = new String[]{ContactsContract.PhoneLookup._ID};
        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        // query time
        Cursor cursor = context.getContentResolver().query(contactUri, projection, null, null, null);

        if (cursor.moveToFirst()) {
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
            // Found the callers... now check the whitelist
            Whitelist whitelist = getInstance(context).getContact(contactId);
            return (null != whitelist);
        }
        return false;
    }


}
