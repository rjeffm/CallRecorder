package com.jlcsoftware.database;

import android.content.ContentValues;
import android.content.Context;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Jeff on 01-May-16.
 * <p>
 * A Call Log record from our database
 */
public class CallLog {

    static final int VERSION = 1;

    boolean isNew = true;

    public ContentValues getContent() {
        return content;
    }

    private ContentValues content;

    public CallLog() {
        content = new ContentValues();
        content.put(Database.CALL_RECORDS_TABLE_OUTGOING, false); // default "outgoing" false (therefore incoming)
        content.put(Database.CALL_RECORDS_TABLE_KEEP, false); // default "keep" false (therefore allow automatic deletion)
    }

    public CallLog(ContentValues content) {
        this.content = content;
    }

    public String getPhoneNumber() {
        String phone = content.getAsString(Database.CALL_RECORDS_TABLE_PHONE_NUMBER);
        if(null==phone) return "";
        return phone;
    }

    public void setPhoneNumber(String phoneNumer) {
        content.put(Database.CALL_RECORDS_TABLE_PHONE_NUMBER, phoneNumer);
    }

    public int getId() {
        return content.getAsInteger(Database.CALL_RECORDS_TABLE_ID);
    }

    public boolean isOutgoing() {
        return content.getAsBoolean(Database.CALL_RECORDS_TABLE_OUTGOING); // Outgoing is "true" Incoming is "false"
    }

    public void setOutgoing() {
        content.put(Database.CALL_RECORDS_TABLE_OUTGOING, true);
    }


    public Calendar getStartTime() {
        Long time = content.getAsLong(Database.CALL_RECORDS_TABLE_START_DATE);
        Calendar cal = GregorianCalendar.getInstance();
        if (null != time) cal.setTimeInMillis(time);
        return cal;
    }

    public void setSartTime(Calendar cal) {
        content.put(Database.CALL_RECORDS_TABLE_START_DATE, cal.getTimeInMillis());
    }

    public Calendar getEndTime() {
        Long time = content.getAsLong(Database.CALL_RECORDS_TABLE_END_DATE);
        Calendar cal = GregorianCalendar.getInstance();
        if (null != time) cal.setTimeInMillis(time);
        return cal;
    }


    public void setEndTime(Calendar cal) {
        content.put(Database.CALL_RECORDS_TABLE_END_DATE, cal.getTimeInMillis());
    }

    public String getPathToRecording() {
        String path = content.getAsString(Database.CALL_RECORDS_TABLE_RECORDING_PATH);
        if(path==null) return "";
        return path;
    }

    public void setPathToRecording(String path) {
        content.put(Database.CALL_RECORDS_TABLE_RECORDING_PATH, path);
    }

    public boolean isKept() {
        Boolean asBoolean = content.getAsBoolean(Database.CALL_RECORDS_TABLE_KEEP);
        if(null==asBoolean) return false;
        return asBoolean;
    }

    public void setKept(boolean keep){
        content.put(Database.CALL_RECORDS_TABLE_KEEP, keep);
    }

    public void save(Context context) {
        Database.getInstance(context).addCall(this);
    }


}
