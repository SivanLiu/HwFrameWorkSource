package tmsdkobf;

import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class mb {
    public static void a(String str, String str2, Throwable th) {
        if (isEnable()) {
            if (str2 == null) {
                str2 = "(null)";
            }
            Log.println(4, str, str2);
        }
    }

    public static void b(String str, String str2, Throwable th) {
        if (isEnable()) {
            if (str2 == null) {
                str2 = "(null)";
            }
            Log.println(5, str, str2);
        }
    }

    private static String c(Object obj) {
        return obj != null ? !(obj instanceof String) ? !(obj instanceof Throwable) ? obj.toString() : getStackTraceString((Throwable) obj) : (String) obj : null;
    }

    public static void c(String str, String str2, Throwable th) {
        if (isEnable()) {
            if (str2 == null) {
                str2 = "(null)";
            }
            Log.println(6, str, str2);
        }
    }

    public static void d(String str, Object obj) {
        d(str, c(obj));
    }

    public static void d(String str, String str2) {
        if (isEnable()) {
            Log.d(str, str2);
        }
    }

    public static void e(String str, Object obj) {
        o(str, c(obj));
    }

    public static String getStackTraceString(Throwable th) {
        if (th == null) {
            return "(Null stack trace)";
        }
        Object -l_1_R = new StringWriter();
        Object -l_2_R = new PrintWriter(-l_1_R);
        th.printStackTrace(-l_2_R);
        -l_2_R.flush();
        Object -l_3_R = -l_1_R.toString();
        -l_2_R.close();
        return -l_3_R;
    }

    public static boolean isEnable() {
        return false;
    }

    public static void n(String str, String str2) {
        if (isEnable()) {
            Log.i(str, str2);
        }
    }

    public static void o(String str, String str2) {
        if (isEnable()) {
            Log.e(str, str2);
        }
    }

    public static void r(String str, String str2) {
        if (isEnable()) {
            Log.v(str, str2);
        }
    }

    public static void s(String str, String str2) {
        if (isEnable()) {
            Log.w(str, str2);
        }
    }
}
