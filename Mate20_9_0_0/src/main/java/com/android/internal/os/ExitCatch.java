package com.android.internal.os;

import android.util.Log;
import java.io.FileOutputStream;
import java.io.IOException;

public final class ExitCatch {
    private static final boolean DEBUG = false;
    public static final int EXIT_CATCH_ABORT_FLAG = 6;
    public static final int KILL_CATCH_FLAG = 1;
    private static final String TAG = "RMS.ExitCatch";

    public static boolean enable(int pid, int flags) {
        String buf = new StringBuilder();
        buf.append("/proc/");
        buf.append(pid);
        buf.append("/unexpected_die_catch");
        boolean ret = writeFile(buf.toString(), String.valueOf(flags));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("now ExitCatch is enable in pid =");
        stringBuilder.append(pid);
        stringBuilder.append(" ret = ");
        stringBuilder.append(ret);
        Log.w(str, stringBuilder.toString());
        return ret;
    }

    public static boolean disable(int pid) {
        String buf = new StringBuilder();
        buf.append("/proc/");
        buf.append(pid);
        buf.append("/unexpected_die_catch");
        boolean ret = writeFile(buf.toString(), "0");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("now ExitCatch is disable in pid =");
        stringBuilder.append(pid);
        stringBuilder.append(" ret = ");
        stringBuilder.append(ret);
        Log.w(str, stringBuilder.toString());
        return ret;
    }

    private static final boolean writeFile(String path, String data) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(data.getBytes());
            try {
                fos.close();
            } catch (IOException e) {
                Log.w(TAG, "find IOException.");
            }
            return true;
        } catch (IOException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to write ");
            stringBuilder.append(path);
            stringBuilder.append(" msg=");
            stringBuilder.append(e2.getMessage());
            Log.w(str, stringBuilder.toString());
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e3) {
                    Log.w(TAG, "find IOException.");
                }
            }
            return false;
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e4) {
                    Log.w(TAG, "find IOException.");
                }
            }
            return true;
        }
    }
}
