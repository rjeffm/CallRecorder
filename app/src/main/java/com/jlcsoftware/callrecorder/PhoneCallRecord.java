package com.jlcsoftware.callrecorder;

import android.graphics.drawable.Drawable;

import com.jlcsoftware.database.CallLog;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jeff on 02-May-16.
 *
 * Info about a phone call recording
 */
public class PhoneCallRecord {

    // Cache of Contact Pictures to minimize image memory use...
    private static Map<String, Drawable> synchronizedMap = Collections.synchronizedMap(new HashMap<String, Drawable>());

    CallLog phoneCall;

    public PhoneCallRecord(CallLog phoneCall) {
        this.phoneCall = phoneCall;
    }

    public void setImage(Drawable photo) {
        synchronizedMap.put(phoneCall.getPhoneNumber(), photo);
    }

    /**
     * Get the Contact image from the cache...
     *
     * @return NULL if there isn't an Image in the cache
     */
    public Drawable getImage() {
        Drawable drawable = synchronizedMap.get(phoneCall.getPhoneNumber());
        return drawable;
    }

    private String contactId;

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getContactId() {
        return contactId;
    }

    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if(null==name){
            return phoneCall.getPhoneNumber();
        }
        return name;
    }

    CallLog getPhoneCall() {
        return phoneCall;
    }
}
