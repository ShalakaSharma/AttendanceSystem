package com.example.shalaka.attendancesystem;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Shalaka on 4/15/2017.
 */

public class Scheduler extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;
    public static final String ACTION = "com.codepath.example.servicesdemo.alarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(getClass().getSimpleName(),  "onReceive() called");
        boolean result = false;
        result = isMyServiceRunning(BackgroundService.class, context);
        Log.i(getClass().getSimpleName(),  "" + result);
        if (!result) {
            Intent i = new Intent(context, BackgroundService.class);
            context.startService(i);
        }
    }

    public boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        Log.i(getClass().getSimpleName(),  "isMyServiceRunning called");
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}