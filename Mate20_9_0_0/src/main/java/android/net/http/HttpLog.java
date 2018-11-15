package android.net.http;

import android.os.SystemClock;
import android.util.Log;

class HttpLog {
    private static final boolean DEBUG = false;
    private static final String LOGTAG = "http";
    static final boolean LOGV = false;

    HttpLog() {
    }

    static void v(String logMe) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(SystemClock.uptimeMillis());
        stringBuilder.append(" ");
        stringBuilder.append(Thread.currentThread().getName());
        stringBuilder.append(" ");
        stringBuilder.append(logMe);
        Log.v("http", stringBuilder.toString());
    }

    static void e(String logMe) {
        Log.e("http", logMe);
    }
}
