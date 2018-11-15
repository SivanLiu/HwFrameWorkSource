package huawei.com.android.server.policy.recsys;

import android.text.TextUtils;
import android.util.Log;

public class HwLog {
    private static final String APPLICATION_NAME = "HwRecSys_";
    private static final boolean HWLOG = true;

    public static void v(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(APPLICATION_NAME);
        stringBuilder.append(tag);
        Log.v(stringBuilder.toString(), TextUtils.isEmpty(msg) ? "no msg" : msg);
    }

    public static void d(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(APPLICATION_NAME);
        stringBuilder.append(tag);
        Log.d(stringBuilder.toString(), TextUtils.isEmpty(msg) ? "no msg" : msg);
    }

    public static void e(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(APPLICATION_NAME);
        stringBuilder.append(tag);
        Log.e(stringBuilder.toString(), TextUtils.isEmpty(msg) ? "no msg" : msg);
    }

    public static void i(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(APPLICATION_NAME);
        stringBuilder.append(tag);
        Log.i(stringBuilder.toString(), TextUtils.isEmpty(msg) ? "no msg" : msg);
    }

    public static void i(String tag, String msg, Exception ex) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(APPLICATION_NAME);
        stringBuilder.append(tag);
        Log.i(stringBuilder.toString(), TextUtils.isEmpty(msg) ? "no msg" : msg, ex);
    }

    public static void w(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(APPLICATION_NAME);
        stringBuilder.append(tag);
        Log.w(stringBuilder.toString(), TextUtils.isEmpty(msg) ? "no msg" : msg);
    }
}
