package com.huawei.android.pushagent.utils.f;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class c {
    private static c bb = null;
    private static String bc = "";

    private c() {
    }

    private static synchronized c ex() {
        c cVar;
        synchronized (c.class) {
            if (bb == null) {
                bb = new c();
            }
            cVar = bb;
        }
        return cVar;
    }

    public static void eu(Context context) {
        if (bb == null) {
            ex();
        }
        if (TextUtils.isEmpty(bc)) {
            String packageName = context.getPackageName();
            if (packageName != null) {
                String[] split = packageName.split("\\.");
                if (split != null && split.length > 0) {
                    bc = split[split.length - 1];
                }
            }
        }
    }

    public static void er(String str, String str2) {
        ex().ez(3, str, str2, null, 2);
    }

    public static void et(String str, String str2, Throwable th) {
        ex().ez(3, str, str2, th, 2);
    }

    public static void ep(String str, String str2) {
        ex().ez(4, str, str2, null, 2);
    }

    public static void eo(String str, String str2) {
        ex().ez(5, str, str2, null, 2);
    }

    public static void ev(String str, String str2, Throwable th) {
        ex().ez(5, str, str2, th, 2);
    }

    public static void eq(String str, String str2) {
        ex().ez(6, str, str2, null, 2);
    }

    public static void es(String str, String str2, Throwable th) {
        ex().ez(6, str, str2, th, 2);
    }

    public static String ew(Throwable th) {
        return Log.getStackTraceString(th);
    }

    private synchronized void ez(int i, String str, String str2, Throwable th, int i2) {
        try {
            if (ey(i)) {
                String str3 = "[" + Thread.currentThread().getName() + "-" + Thread.currentThread().getId() + "]" + str2;
                StackTraceElement[] stackTrace = new Throwable().getStackTrace();
                if (stackTrace.length > i2) {
                    str3 = str3 + "(" + bc + "/" + stackTrace[i2].getFileName() + ":" + stackTrace[i2].getLineNumber() + ")";
                } else {
                    str3 = str3 + "(" + bc + "/unknown source)";
                }
                if (th != null) {
                    str3 = str3 + 10 + ew(th);
                }
                Log.println(i, str, str3);
            } else {
                return;
            }
        } catch (Throwable e) {
            Log.e("PushLog3413", "call writeLog cause:" + e.toString(), e);
        }
        return;
    }

    private static boolean ey(int i) {
        try {
            return Log.isLoggable("hwpush", i);
        } catch (Exception e) {
            return false;
        }
    }
}
