package com.huawei.android.pushagent.a.a;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class c {
    private static String a = "";
    private static String b = "hwpush";
    private static String c = "PushLog";
    private static c d = null;

    private c() {
    }

    public static synchronized c a() {
        c cVar;
        synchronized (c.class) {
            if (d == null) {
                d = new c();
            }
            cVar = d;
        }
        return cVar;
    }

    public static String a(Throwable th) {
        return Log.getStackTraceString(th);
    }

    private synchronized void a(int i, String str, String str2, Throwable th, int i2) {
        Object -l_6_R;
        try {
            if (a(i)) {
                String str3 = "[" + Thread.currentThread().getName() + "-" + Thread.currentThread().getId() + "]" + str2;
                -l_6_R = new Throwable().getStackTrace();
                str3 = -l_6_R.length <= i2 ? str3 + "(" + a + "/unknown source)" : str3 + "(" + a + "/" + -l_6_R[i2].getFileName() + ":" + -l_6_R[i2].getLineNumber() + ")";
                if (th != null) {
                    str3 = str3 + '\n' + a(th);
                }
                Log.println(i, c, str3);
            }
        } catch (Object -l_6_R2) {
            Log.e("PushLogSC2907", "call writeLog cause:" + -l_6_R2.toString(), -l_6_R2);
        }
    }

    public static synchronized void a(Context context) {
        synchronized (c.class) {
            if (d == null) {
                a();
            }
            if (TextUtils.isEmpty(a)) {
                Object -l_1_R = context.getPackageName();
                if (-l_1_R != null) {
                    Object -l_2_R = -l_1_R.split("\\.");
                    if (-l_2_R.length > 0) {
                        a = -l_2_R[-l_2_R.length - 1];
                    }
                }
                c = b(context);
                return;
            }
        }
    }

    public static void a(String str, String str2) {
        a().a(3, str, str2, null, 2);
    }

    public static void a(String str, String str2, Throwable th) {
        a().a(3, str, str2, th, 2);
    }

    public static void a(String str, String str2, Object... objArr) {
        try {
            a().a(3, str, String.format(str2, objArr), null, 2);
        } catch (Object -l_3_R) {
            Log.e("PushLogSC2907", "call writeLog cause:" + -l_3_R.toString(), -l_3_R);
        }
    }

    private static boolean a(int i) {
        return Log.isLoggable(b, i);
    }

    public static String b(Context context) {
        Object -l_1_R = "PushLogSC2907";
        if (context == null) {
            return -l_1_R;
        }
        if ("com.huawei.android.pushagent".equals(context.getPackageName())) {
            -l_1_R = -l_1_R.replace("SC", "AC");
        } else {
            if ("android".equals(context.getPackageName())) {
                -l_1_R = -l_1_R.replace("SC", "");
            } else if (!TextUtils.isEmpty(a)) {
                -l_1_R = -l_1_R + "_" + a;
            }
        }
        return -l_1_R;
    }

    public static void b(String str, String str2) {
        a().a(4, str, str2, null, 2);
    }

    public static void b(String str, String str2, Throwable th) {
        a().a(4, str, str2, th, 2);
    }

    public static void b(String str, String str2, Object... objArr) {
        try {
            a().a(2, str, String.format(str2, objArr), null, 2);
        } catch (Object -l_3_R) {
            Log.e("PushLogSC2907", "call writeLog cause:" + -l_3_R.toString(), -l_3_R);
        }
    }

    public static void c(String str, String str2) {
        a().a(5, str, str2, null, 2);
    }

    public static void c(String str, String str2, Throwable th) {
        a().a(5, str, str2, th, 2);
    }

    public static void d(String str, String str2) {
        a().a(6, str, str2, null, 2);
    }

    public static void d(String str, String str2, Throwable th) {
        a().a(6, str, str2, th, 2);
    }

    public static void e(String str, String str2) {
        a().a(2, str, str2, null, 2);
    }

    public static void e(String str, String str2, Throwable th) {
        a().a(2, str, str2, th, 2);
    }
}
