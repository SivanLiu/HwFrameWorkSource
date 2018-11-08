package com.huawei.android.pushselfshow.d;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.c.a;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import tmsdk.common.module.intelli_sms.SmsCheckResult;

public class d {
    private static int a = 0;
    private static HashMap b = new HashMap();

    public static Notification a(Context context, a aVar, int i, int i2, int i3) {
        int -l_8_I;
        Bitmap -l_9_R;
        Notification -l_5_R = new Notification();
        -l_5_R.icon = b.a(context, aVar);
        int -l_6_I = context.getApplicationInfo().labelRes;
        -l_5_R.tickerText = aVar.n();
        -l_5_R.when = System.currentTimeMillis();
        -l_5_R.flags |= 16;
        -l_5_R.defaults |= 1;
        Object -l_7_R = a(context, aVar, i, i2);
        -l_5_R.contentIntent = -l_7_R;
        -l_5_R.deleteIntent = b(context, aVar, i, i3);
        if (aVar.p() != null) {
            if (!"".equals(aVar.p())) {
                -l_5_R.setLatestEventInfo(context, aVar.p(), aVar.n(), -l_7_R);
                -l_8_I = context.getResources().getIdentifier("icon", "id", "android");
                -l_9_R = b.b(context, aVar);
                if (!(-l_8_I == 0 || -l_5_R.contentView == null || -l_9_R == null)) {
                    -l_5_R.contentView.setImageViewBitmap(-l_8_I, -l_9_R);
                }
                return f.a(context, -l_5_R, i, aVar, -l_9_R);
            }
        }
        -l_5_R.setLatestEventInfo(context, context.getResources().getString(-l_6_I), aVar.n(), -l_7_R);
        -l_8_I = context.getResources().getIdentifier("icon", "id", "android");
        -l_9_R = b.b(context, aVar);
        -l_5_R.contentView.setImageViewBitmap(-l_8_I, -l_9_R);
        return f.a(context, -l_5_R, i, aVar, -l_9_R);
    }

    private static PendingIntent a(Context context, a aVar, int i, int i2) {
        Object -l_4_R = new Intent("com.huawei.intent.action.PUSH");
        String str = "selfshow_token";
        str = "extra_encrypt_data";
        -l_4_R.putExtra("selfshow_info", aVar.c()).putExtra(str, aVar.d()).putExtra("selfshow_event_id", "1").putExtra(str, com.huawei.android.pushselfshow.utils.a.m(context)).putExtra("selfshow_notify_id", i).setPackage(context.getPackageName()).setFlags(268435456);
        return PendingIntent.getBroadcast(context, i2, -l_4_R, 134217728);
    }

    private static void a(Context context, Builder builder, a aVar) {
        if ("com.huawei.android.pushagent".equals(context.getPackageName())) {
            Object -l_3_R = new Bundle();
            Object -l_4_R = aVar.k();
            if (!TextUtils.isEmpty(-l_4_R)) {
                -l_3_R.putString("hw_origin_sender_package_name", -l_4_R);
                builder.setExtras(-l_3_R);
            }
        }
    }

    public static void a(Context context, Intent intent, long j, int i) {
        try {
            c.a("PushSelfShowLog", "enter setDelayAlarm(intent:" + intent.toURI() + " interval:" + j + "ms, context:" + context);
            ((AlarmManager) context.getSystemService("alarm")).set(0, System.currentTimeMillis() + j, PendingIntent.getBroadcast(context, i, intent, 134217728));
        } catch (Throwable -l_5_R) {
            c.a("PushSelfShowLog", "set DelayAlarm error", -l_5_R);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized void a(Context context, a aVar) {
        synchronized (d.class) {
            if (!(context == null || aVar == null)) {
                try {
                    int -l_2_I;
                    int -l_3_I;
                    int -l_4_I;
                    int -l_5_I;
                    c.a("PushSelfShowLog", " showNotification , the msg id = " + aVar.a());
                    com.huawei.android.pushselfshow.utils.a.a(2, (int) SmsCheckResult.ESCT_180);
                    if (a == 0) {
                        a = (context.getPackageName() + new Date().toString()).hashCode();
                    }
                    if (TextUtils.isEmpty(aVar.e())) {
                        -l_2_I = a + 1;
                        a = -l_2_I;
                        -l_3_I = a + 1;
                        a = -l_3_I;
                        -l_4_I = a + 1;
                        a = -l_4_I;
                        -l_5_I = a + 1;
                        a = -l_5_I;
                    } else {
                        -l_2_I = (aVar.k() + aVar.e()).hashCode();
                        -l_3_I = a + 1;
                        a = -l_3_I;
                        -l_4_I = a + 1;
                        a = -l_4_I;
                        -l_5_I = a + 1;
                        a = -l_5_I;
                    }
                    c.a("PushSelfShowLog", "notifyId:" + -l_2_I + ",openNotifyId:" + -l_3_I + ",delNotifyId:" + -l_4_I + ",alarmNotifyId:" + -l_5_I);
                    Object -l_6_R = !com.huawei.android.pushselfshow.utils.a.b() ? a(context, aVar, -l_2_I, -l_3_I, -l_4_I) : b(context, aVar, -l_2_I, -l_3_I, -l_4_I);
                    NotificationManager -l_7_R = (NotificationManager) context.getSystemService("notification");
                    if (!(-l_7_R == null || -l_6_R == null)) {
                        -l_7_R.notify(-l_2_I, -l_6_R);
                        if (aVar.f() > 0) {
                            Intent -l_8_R = new Intent("com.huawei.intent.action.PUSH");
                            String str = "extra_encrypt_data";
                            -l_8_R.putExtra("selfshow_info", aVar.c()).putExtra("selfshow_token", aVar.d()).putExtra("selfshow_event_id", "-1").putExtra(str, com.huawei.android.pushselfshow.utils.a.m(context)).putExtra("selfshow_notify_id", -l_2_I).setPackage(context.getPackageName()).setFlags(32);
                            a(context, -l_8_R, (long) aVar.f(), -l_5_I);
                            c.a("PushSelfShowLog", "setDelayAlarm alarmNotityId" + -l_5_I + " and intent is " + -l_8_R.toURI());
                        }
                        if (!"com.huawei.android.pushagent".equals(context.getPackageName()) || TextUtils.isEmpty(aVar.M()) || TextUtils.isEmpty(aVar.k()) || aVar.L() == 0) {
                            c.a("PushSelfShowLog", "badgeClassName is null or permission denied ");
                        } else {
                            c.a("PushSelfShowLog", "need to refresh badge number. package name is " + aVar.k());
                            com.huawei.android.pushselfshow.a.a.a(context, aVar.k(), aVar.M(), aVar.L());
                        }
                        com.huawei.android.pushselfshow.utils.a.a(context, "0", aVar, -l_2_I);
                    }
                } catch (Object -l_2_R) {
                    c.d("PushSelfShowLog", "showNotification error " + -l_2_R.toString());
                }
            }
        }
    }

    public static void a(String str) {
        if (TextUtils.isEmpty(str)) {
            c.d("PushSelfShowLog", "enter clearGroupCount, key is empty");
            return;
        }
        if (b.containsKey(str)) {
            b.remove(str);
            c.a("PushSelfShowLog", "after remove, groupMap.size is:" + b.get(str));
        }
    }

    public static Notification b(Context context, a aVar, int i, int i2, int i3) {
        Bitmap -l_8_R;
        Builder -l_5_R = new Builder(context);
        b.a(context, -l_5_R, aVar);
        int -l_6_I = context.getApplicationInfo().labelRes;
        -l_5_R.setTicker(aVar.n());
        -l_5_R.setWhen(System.currentTimeMillis());
        -l_5_R.setAutoCancel(true);
        -l_5_R.setDefaults(1);
        Object -l_7_R = aVar.k() + aVar.e();
        if (!TextUtils.isEmpty(aVar.e())) {
            c.a("PushSelfShowLog", "groupMap key is " + -l_7_R);
            if (b.containsKey(-l_7_R)) {
                b.put(-l_7_R, Integer.valueOf(((Integer) b.get(-l_7_R)).intValue() + 1));
                c.a("PushSelfShowLog", "groupMap.size:" + b.get(-l_7_R));
            } else {
                b.put(-l_7_R, Integer.valueOf(1));
            }
        }
        if (aVar.p() != null) {
            if (!"".equals(aVar.p())) {
                -l_5_R.setContentTitle(aVar.p());
                if (TextUtils.isEmpty(aVar.e()) || ((Integer) b.get(-l_7_R)).intValue() == 1) {
                    -l_5_R.setContentText(aVar.n());
                } else {
                    -l_5_R.setContentText(context.getResources().getQuantityString(com.huawei.android.pushselfshow.utils.d.b(context, "hwpush_message_hint"), -l_8_I, new Object[]{Integer.valueOf(((Integer) b.get(-l_7_R)).intValue())}));
                }
                -l_5_R.setContentIntent(a(context, aVar, i, i2));
                -l_5_R.setDeleteIntent(b(context, aVar, i, i3));
                -l_8_R = b.b(context, aVar);
                if (-l_8_R != null) {
                    -l_5_R.setLargeIcon(-l_8_R);
                }
                a(context, -l_5_R, aVar);
                b(context, -l_5_R, aVar);
                return f.a(context, -l_5_R, i, aVar, -l_8_R) != null ? -l_5_R.getNotification() : null;
            }
        }
        -l_5_R.setContentTitle(context.getResources().getString(-l_6_I));
        if (TextUtils.isEmpty(aVar.e())) {
            -l_5_R.setContentText(context.getResources().getQuantityString(com.huawei.android.pushselfshow.utils.d.b(context, "hwpush_message_hint"), -l_8_I, new Object[]{Integer.valueOf(((Integer) b.get(-l_7_R)).intValue())}));
            -l_5_R.setContentIntent(a(context, aVar, i, i2));
            -l_5_R.setDeleteIntent(b(context, aVar, i, i3));
            -l_8_R = b.b(context, aVar);
            if (-l_8_R != null) {
                -l_5_R.setLargeIcon(-l_8_R);
            }
            a(context, -l_5_R, aVar);
            b(context, -l_5_R, aVar);
            if (f.a(context, -l_5_R, i, aVar, -l_8_R) != null) {
            }
        }
        -l_5_R.setContentText(aVar.n());
        -l_5_R.setContentIntent(a(context, aVar, i, i2));
        -l_5_R.setDeleteIntent(b(context, aVar, i, i3));
        -l_8_R = b.b(context, aVar);
        if (-l_8_R != null) {
            -l_5_R.setLargeIcon(-l_8_R);
        }
        a(context, -l_5_R, aVar);
        b(context, -l_5_R, aVar);
        if (f.a(context, -l_5_R, i, aVar, -l_8_R) != null) {
        }
    }

    private static PendingIntent b(Context context, a aVar, int i, int i2) {
        Object -l_4_R = new Intent("com.huawei.intent.action.PUSH");
        String str = "selfshow_token";
        str = "extra_encrypt_data";
        -l_4_R.putExtra("selfshow_info", aVar.c()).putExtra(str, aVar.d()).putExtra("selfshow_event_id", "2").putExtra("selfshow_notify_id", i).setPackage(context.getPackageName()).putExtra(str, com.huawei.android.pushselfshow.utils.a.m(context)).setFlags(268435456);
        return PendingIntent.getBroadcast(context, i2, -l_4_R, 134217728);
    }

    private static void b(Context context, Builder builder, a aVar) {
        if (com.huawei.android.pushagent.a.a.a.a() >= 11 && com.huawei.android.pushselfshow.utils.a.f(context)) {
            Object -l_3_R = new Bundle();
            String -l_4_R = aVar.k();
            c.b("PushSelfShowLog", "the package name of notification is:" + -l_4_R);
            if (!TextUtils.isEmpty(-l_4_R)) {
                Object -l_5_R = com.huawei.android.pushselfshow.utils.a.a(context, -l_4_R);
                c.b("PushSelfShowLog", "the app name is:" + -l_5_R);
                if (-l_5_R != null) {
                    -l_3_R.putCharSequence("android.extraAppName", -l_5_R);
                }
            }
            builder.setExtras(-l_3_R);
        }
    }

    public static void b(String str) {
        if (TextUtils.isEmpty(str)) {
            c.d("PushSelfShowLog", "enter clearAllGroupCount, pkgname is empty");
            return;
        }
        Object -l_1_R = new LinkedList();
        for (String -l_4_R : b.keySet()) {
            c.a("PushSelfShowLog", "clearAllGroupCount, group is:" + -l_4_R);
            if (-l_4_R.contains(str)) {
                -l_1_R.add(-l_4_R);
            }
        }
        Object -l_3_R = -l_1_R.iterator();
        while (-l_3_R.hasNext()) {
            b.remove((String) -l_3_R.next());
        }
    }
}
