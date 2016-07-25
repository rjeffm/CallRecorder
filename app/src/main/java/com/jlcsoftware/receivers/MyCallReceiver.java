package com.jlcsoftware.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.jlcsoftware.callrecorder.AppPreferences;
import com.jlcsoftware.listeners.PhoneListener;


/**
 * Handle the Phone call related BroadcastActions
 * <action android:name="android.intent.action.PHONE_STATE" />
 * <action android:name="android.intent.action.NEW_OUTGOING_CALL" />
 */

public class MyCallReceiver extends BroadcastReceiver {

    public MyCallReceiver() {
    }

    static TelephonyManager manager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("JLCreativeCallRecorder", "MyCallReceiver.onReceive ");

        if (!AppPreferences.getInstance(context).isRecordingEnabled()) {
            removeListener();
            return;
        }

        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
            if (!AppPreferences.getInstance(context).isRecordingOutgoingEnabled()) {
                removeListener();
                return;
            }
            PhoneListener.getInstance(context).setOutgoing(intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
        } else {
            if (!AppPreferences.getInstance(context).isRecordingIncomingEnabled()) {
                removeListener();
                return;
            }
        }

        // Start Listening to the call....
        if (null == manager) {
            manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        }
        if (null != manager)
            manager.listen(PhoneListener.getInstance(context), PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void removeListener() {
        if (null != manager) {
            if (PhoneListener.hasInstance())
                manager.listen(PhoneListener.getInstance(null), PhoneStateListener.LISTEN_NONE);
        }
    }

}


