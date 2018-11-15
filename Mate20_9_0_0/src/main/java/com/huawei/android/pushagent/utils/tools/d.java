package com.huawei.android.pushagent.utils.tools;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import com.huawei.android.pushagent.utils.f.c;

public class d {
    private static String TAG = "PushLog3413";

    public static void cw(Context context, Intent intent, long j) {
        c.ep(TAG, "enter AlarmTools:setExactAlarm(intent:" + intent + " interval:" + j + "ms");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        long currentTimeMillis = System.currentTimeMillis() + j;
        c.ep(TAG, "setExactAlarm expectTriggerTime:" + currentTimeMillis);
        intent.putExtra("expectTriggerTime", currentTimeMillis);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, 134217728);
        if (VERSION.SDK_INT >= 19) {
            alarmManager.setExact(0, currentTimeMillis, broadcast);
        } else {
            alarmManager.set(0, currentTimeMillis, broadcast);
        }
    }

    public static void da(Context context, Intent intent, long j, long j2) {
        c.er(TAG, "enter AlarmTools:setExactWindowAlarm(intent:" + intent + ",interval:" + j + "ms" + ",window:" + j2);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, 134217728);
        if (VERSION.SDK_INT >= 19) {
            alarmManager.setWindow(0, System.currentTimeMillis() + j, j2, broadcast);
        } else {
            c.eq(TAG, "fail to setExactWindowAlarm");
        }
    }

    public static void cx(Context context, Intent intent, long j) {
        c.er(TAG, "enter AlarmTools:setInexactAlarm(intent:" + intent + " interval:" + j + "ms");
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, 134217728);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        if (alarmManager != null) {
            alarmManager.set(1, System.currentTimeMillis() + j, broadcast);
        } else {
            c.eq(TAG, "fail to setInexactAlarm");
        }
    }

    public static void cz(Context context, Intent intent, long j) {
        c.er(TAG, "enter AlarmTools:setDelayNotifyService(intent:" + intent + " interval:" + j + ")");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        PendingIntent service = PendingIntent.getService(context, 0, intent, 0);
        if (VERSION.SDK_INT >= 19) {
            alarmManager.setExact(0, System.currentTimeMillis() + j, service);
        } else {
            alarmManager.set(0, System.currentTimeMillis() + j, service);
        }
    }

    public static void cy(Context context, String str) {
        c.er(TAG, "enter cancelAlarm(Action=" + str);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        Intent intent = new Intent(str);
        intent.setPackage(context.getPackageName());
        alarmManager.cancel(PendingIntent.getBroadcast(context, 0, intent, 0));
    }
}
