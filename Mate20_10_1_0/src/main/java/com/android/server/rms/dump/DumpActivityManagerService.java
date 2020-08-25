package com.android.server.rms.dump;

import android.content.Context;
import android.util.Log;
import com.android.server.am.HwActivityManagerService;
import java.io.PrintWriter;

public final class DumpActivityManagerService {
    private static final String TAG = "DumpActivityManagerService";

    public static void lockAms(Context context, PrintWriter pw, String[] args) {
        pw.println("--dump-ActivityManagerService");
        new Thread(new Runnable() {
            /* class com.android.server.rms.dump.DumpActivityManagerService.AnonymousClass1 */

            public void run() {
                synchronized (HwActivityManagerService.self()) {
                    Log.d(DumpActivityManagerService.TAG, "new thread:--dump-ActivityManagerService");
                    try {
                        Thread.currentThread();
                        Thread.sleep(20000);
                    } catch (InterruptedException e) {
                        Log.d(DumpActivityManagerService.TAG, "InterruptedException");
                    }
                }
            }
        }).start();
    }
}
