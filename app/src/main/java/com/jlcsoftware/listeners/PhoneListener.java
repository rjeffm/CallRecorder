package com.jlcsoftware.listeners;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.jlcsoftware.database.CallLog;
import com.jlcsoftware.database.Database;
import com.jlcsoftware.receivers.MyCallReceiver;
import com.jlcsoftware.services.RecordCallService;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Jeff on 01-May-16.
 * <p/>
 * The logic is a little odd here...
 * <p/>
 * When a incoming call comes in, we get a CALL_STATE_RINGING that provides the incoming number and all is easy and good...
 * on the other hand, a Outgoing call generates a ACTION_NEW_OUTGOING_CALL with the phone number, then an a CALL_STATE_IDLE and then a
 * CALL_STATE_OFFHOOK when the call connects - we never get the outgoing number in the PhoneState Change
 * <p/>
 */
public class PhoneListener extends PhoneStateListener {

    private static PhoneListener instance = null;

    /**
     * Must be called once on app startup
     *
     * @param context - application context
     * @return
     */
    public static PhoneListener getInstance(Context context) {
        if (instance == null) {
            instance = new PhoneListener(context);
        }
        return instance;
    }

    public static boolean hasInstance() {
        return null != instance;
    }

    private final Context context;
    private CallLog phoneCall;

    private PhoneListener(Context context) {
        this.context = context;
    }

    AtomicBoolean isRecording = new AtomicBoolean();
    AtomicBoolean isWhitelisted = new AtomicBoolean();


    /**
     * Set the outgoing phone number
     * <p/>
     * Called by {@link MyCallReceiver}  since that is where the phone number is available in a outgoing call
     *
     * @param phoneNumber
     */
    public void setOutgoing(String phoneNumber) {
        if (null == phoneCall)
            phoneCall = new CallLog();
        phoneCall.setPhoneNumber(phoneNumber);
        phoneCall.setOutgoing();
        // called here so as not to miss recording part of the conversation in TelephonyManager.CALL_STATE_OFFHOOK
        isWhitelisted.set(Database.isWhitelisted(context, phoneCall.getPhoneNumber()));
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);

        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE: // Idle... no call
                if (isRecording.get()) {
                    RecordCallService.stopRecording(context);
                    phoneCall = null;
                    isRecording.set(false);
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK: // Call answered
                if (isWhitelisted.get()) {
                    isWhitelisted.set(false);
                    return;
                }
                if (!isRecording.get()) {
                    isRecording.set(true);
                    // start: Probably not ever usefull
                    if (null == phoneCall)
                        phoneCall = new CallLog();
                    if (!incomingNumber.isEmpty()) {
                        phoneCall.setPhoneNumber(incomingNumber);
                    }
                    // end: Probably not ever usefull
                    RecordCallService.sartRecording(context, phoneCall);
                }
                break;
            case TelephonyManager.CALL_STATE_RINGING: // Phone ringing
                // DO NOT try RECORDING here! Leads to VERY poor quality recordings
                // I think something is not fully settled with the Incoming phone call when we get CALL_STATE_RINGING
                // a "SystemClock.sleep(1000);" in the code will allow the incoming call to stabilize and produce a good recording...(as proof of above)
                if (null == phoneCall)
                    phoneCall = new CallLog();
                if (!incomingNumber.isEmpty()) {
                    phoneCall.setPhoneNumber(incomingNumber);
                    // called here so as not to miss recording part of the conversation in TelephonyManager.CALL_STATE_OFFHOOK
                    isWhitelisted.set(Database.isWhitelisted(context, phoneCall.getPhoneNumber()));
                }
                break;
        }

    }
}
