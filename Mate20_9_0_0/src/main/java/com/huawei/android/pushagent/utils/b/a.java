package com.huawei.android.pushagent.utils.b;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class a {
    private static a gr = null;
    private static String gs = "";

    private a() {
    }

    private static synchronized a sz() {
        a aVar;
        synchronized (a.class) {
            if (gr == null) {
                gr = new a();
            }
            aVar = gr;
        }
        return aVar;
    }

    public static void tb(Context context) {
        if (gr == null) {
            sz();
        }
        if (TextUtils.isEmpty(gs)) {
            String packageName = context.getPackageName();
            if (packageName != null) {
                String[] split = packageName.split("\\.");
                if (split != null && split.length > 0) {
                    gs = split[split.length - 1];
                }
            }
        }
    }

    public static void st(String str, String str2) {
        sz().te(3, str, str2, null, 2);
    }

    public static void sy(String str, String str2, Throwable th) {
        sz().te(3, str, str2, th, 2);
    }

    public static void sv(String str, String str2) {
        sz().te(4, str, str2, null, 2);
    }

    public static void sx(String str, String str2) {
        sz().te(5, str, str2, null, 2);
    }

    public static void td(String str, String str2, Throwable th) {
        sz().te(5, str, str2, th, 2);
    }

    public static void su(String str, String str2) {
        sz().te(6, str, str2, null, 2);
    }

    public static void sw(String str, String str2, Throwable th) {
        sz().te(6, str, str2, th, 2);
    }

    public static String ta(Throwable th) {
        return Log.getStackTraceString(th);
    }

    private synchronized void te(int i, String str, String str2, Throwable th, int i2) {
        try {
            if (tc(i)) {
                String str3 = "[" + Thread.currentThread().getName() + "-" + Thread.currentThread().getId() + "]" + str2;
                StackTraceElement[] stackTrace = new Throwable().getStackTrace();
                if (stackTrace.length > i2) {
                    str3 = str3 + "(" + gs + "/" + stackTrace[i2].getFileName() + ":" + stackTrace[i2].getLineNumber() + ")";
                } else {
                    str3 = str3 + "(" + gs + "/unknown source)";
                }
                if (th != null) {
                    str3 = str3 + 10 + ta(th);
                }
                Log.println(i, str, str3);
            } else {
                return;
            }
        } catch (Exception e) {
            Log.e("PushLog3414", "call writeLog cause:" + e.toString(), e);
        }
        return;
    }

    private static boolean tc(int i) {
        try {
            return Log.isLoggable("hwpush", i);
        } catch (Exception e) {
            return false;
        }
    }
}
