package com.jlcsoftware.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.jlcsoftware.callrecorder.AppPreferences;
import com.jlcsoftware.callrecorder.LocalBroadcastActions;
import com.jlcsoftware.database.CallLog;
import com.jlcsoftware.database.Database;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Clean up disk usage
 */
public class CleanupService extends Service {
    public CleanupService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // not supported
    }


    AtomicBoolean isRunning = new AtomicBoolean();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (!isRunning.get()) {
            isRunning.set(true);

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        AppPreferences instance = AppPreferences.getInstance(getApplicationContext());
                        AppPreferences.OlderThan olderThan = instance.getOlderThan();
                        if (olderThan != AppPreferences.OlderThan.NEVER) {
                            int age = 0;
                            switch (olderThan) {
                                case DAILY:
                                    age = 1;
                                    break;
                                case THREE_DAYS:
                                    age = 3;
                                    break;
                                case WEEKLY:
                                    age = 7;
                                    break;
                                case MONTHLY:
                                    age = 31;
                                    break;
                                case QUARTERLY:
                                    age = 92;
                                    break;
                                case YEARLY:
                                    age = 365;
                                    break;
                            }
                            boolean deleted = false;
                            Calendar now = GregorianCalendar.getInstance();
                            Database callLogDB = Database.getInstance(getApplicationContext());
                            ArrayList<CallLog> allCalls = callLogDB.getAllCalls();
                            for (CallLog call : allCalls) {
                                if (!call.isKept()) {
                                    if (daysBetween(call.getEndTime(), now) >= age) {
                                        callLogDB.removeCall(call.getId());
                                        deleted = true;
                                    }
                                }
                            }
                            if(deleted) LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(LocalBroadcastActions.RECORDING_DELETED_BROADCAST));
                        }
                        // TODO: implement cleanup via disk usage...
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        isRunning.set(false);
                    }
                }
            };
            new Thread(runnable).start();
        }
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public final static String ACTION_CLEAN_UP = "com.jlcsoftware.ACTION_CLEAN_UP";


    public static void sartCleaning(Context context) {
        Intent intent = new Intent(context, CleanupService.class);
        intent.setAction(ACTION_CLEAN_UP);
        context.startService(intent);
    }


    private long daysBetween(Calendar then, Calendar now) {
        long diff = now.getTimeInMillis() - then.getTimeInMillis();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }
}
