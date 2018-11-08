package com.huawei.android.pushselfshow.a;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import com.huawei.android.pushagent.a.a.c;
import java.util.HashMap;

public class a {
    private static final Object a = new Object();
    private static Context b;
    private static String c;
    private static String d;
    private static HashMap e = new HashMap();
    private static final HandlerThread f = new HandlerThread("push-badge-work");
    private static final Handler g = new Handler(f.getLooper());
    private static Runnable h = new b();

    static {
        f.start();
    }

    private static Bundle a(Context context, String str, String str2, String str3, int i) {
        Bundle bundle = null;
        Object -l_6_R = new Bundle();
        -l_6_R.putString("basepackage", "com.huawei.android.pushagent");
        -l_6_R.putString("package", str2);
        -l_6_R.putString("class", str3);
        -l_6_R.putInt("badgenumber", i);
        try {
            bundle = context.getContentResolver().call(Uri.parse("content://com.huawei.android.launcher.settings/badge/"), str, null, -l_6_R);
            c.b("PushSelfShowLog", "callLauncherMethod:" + str + " sucess");
            return bundle;
        } catch (Object -l_7_R) {
            c.d("PushSelfShowLog", -l_7_R.toString(), -l_7_R);
            return bundle;
        }
    }

    private static void a(Context context, String str, String str2) {
        b = context;
        c = str;
        d = str2;
    }

    public static synchronized void a(Context context, String str, String str2, int i) {
        synchronized (a.class) {
            c.b("PushSelfShowLog", "refresh");
            a(context, str, str2);
            try {
                a(str, i);
                g.removeCallbacks(h);
                g.postDelayed(h, 600);
            } catch (Object -l_4_R) {
                c.d("PushSelfShowLog", -l_4_R.toString());
            }
        }
    }

    private static synchronized void a(String str, int i) {
        synchronized (a.class) {
            int -l_2_I = 0;
            if (e.containsKey(str)) {
                -l_2_I = ((Integer) e.get(str)).intValue();
            }
            c.b("PushSelfShowLog", "existnum " + -l_2_I + ",new num " + i);
            e.put(str, Integer.valueOf(-l_2_I + i));
        }
    }

    private static synchronized void b(String str) {
        synchronized (a.class) {
            c.b("PushSelfShowLog", "resetCachedNum " + str);
            e.remove(str);
        }
    }

    private static int d(Context context, String str, String str2, int i) {
        synchronized (a) {
            Object -l_5_R = a(context, "getbadgeNumber", str, str2, i);
            if (-l_5_R == null) {
                c.b("PushSelfShowLog", "get current exist badgenumber failed");
                return 0;
            }
            int -l_6_I = -l_5_R.getInt("badgenumber");
            c.b("PushSelfShowLog", "current exist badgenumber:" + -l_6_I);
            return -l_6_I;
        }
    }

    private static void e(Context context, String str, String str2, int i) {
        synchronized (a) {
            if (a(context, "change_badge", str, str2, i) == null) {
                c.b("PushSelfShowLog", "refreashBadgeNum failed");
            } else {
                c.b("PushSelfShowLog", "refreashBadgeNum success");
            }
        }
    }
}
