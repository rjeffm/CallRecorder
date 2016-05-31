package com.jlcsoftware.database;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by Jeff on 07-May-16.
 *
 * A Whitelist record from our database
 */
public class Whitelist {

    boolean isNew = true;

    private ContentValues content;

    public Whitelist(){
        content = new ContentValues();
        content.put(Database.WHITELIST_TABLE_RECORD, false); // default: do NOT record
    }

    public String getContactId(){
        return content.getAsString(Database.WHITELIST_TABLE_CONTACT_ID);
    }
    public void setContactId(String contactId){
        content.put(Database.WHITELIST_TABLE_CONTACT_ID,contactId);
    }

    public boolean isRecordable(){
        return content.getAsBoolean(Database.WHITELIST_TABLE_RECORD);
    }

    public void setRecordable(boolean enable){
        content.put(Database.WHITELIST_TABLE_RECORD,enable);
    }

    public int getId(){
        return content.getAsInteger(Database.WHITELIST_TABLE_ID);
    }


    public ContentValues getContent(){
        return content;
    }


}
