package com.huawei.android.pushagent.utils.a;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class b {
    private static b r = null;
    private static String s = "";

    private b() {
    }

    private static synchronized b af() {
        b bVar;
        synchronized (b.class) {
            if (r == null) {
                r = new b();
            }
            bVar = r;
        }
        return bVar;
    }

    public static void ad(Context context) {
        if (r == null) {
            af();
        }
        if (TextUtils.isEmpty(s)) {
            String packageName = context.getPackageName();
            if (packageName != null) {
                String[] split = packageName.split("\\.");
                if (split != null && split.length > 0) {
                    s = split[split.length - 1];
                }
            }
        }
    }

    public static void x(String str, String str2) {
        af().ai(3, str, str2, null, 2);
    }

    public static void ae(String str, String str2, Throwable th) {
        af().ai(3, str, str2, th, 2);
    }

    public static void z(String str, String str2) {
        af().ai(4, str, str2, null, 2);
    }

    public static void ab(String str, String str2) {
        af().ai(5, str, str2, null, 2);
    }

    public static void ac(String str, String str2, Throwable th) {
        af().ai(5, str, str2, th, 2);
    }

    public static void y(String str, String str2) {
        af().ai(6, str, str2, null, 2);
    }

    public static void aa(String str, String str2, Throwable th) {
        af().ai(6, str, str2, th, 2);
    }

    public static String ag(Throwable th) {
        return Log.getStackTraceString(th);
    }

    private synchronized void ai(int i, String str, String str2, Throwable th, int i2) {
        try {
            if (ah(i)) {
                String str3 = "[" + Thread.currentThread().getName() + "-" + Thread.currentThread().getId() + "]" + str2;
                StackTraceElement[] stackTrace = new Throwable().getStackTrace();
                if (stackTrace.length > i2) {
                    str3 = str3 + "(" + s + "/" + stackTrace[i2].getFileName() + ":" + stackTrace[i2].getLineNumber() + ")";
                } else {
                    str3 = str3 + "(" + s + "/unknown source)";
                }
                if (th != null) {
                    str3 = str3 + '\n' + ag(th);
                }
                Log.println(i, str, str3);
            }
        } catch (Throwable e) {
            Log.e("PushLog2976", "call writeLog cause:" + e.toString(), e);
        }
    }

    private static boolean ah(int i) {
        try {
            return Log.isLoggable("hwpush", i);
        } catch (Exception e) {
            return false;
        }
    }
}
