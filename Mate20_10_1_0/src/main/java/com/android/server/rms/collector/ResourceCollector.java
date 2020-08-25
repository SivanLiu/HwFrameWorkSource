package com.android.server.rms.collector;

import android.os.Debug;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ResourceCollector {
    private static final int END_MARK = 5;
    private static final String TAG = "RMS.ResourceCollector";
    private static AtomicBoolean mIsValidPssFast = new AtomicBoolean(false);
    private static AtomicInteger mPssFastRefs = new AtomicInteger(0);

    public static final native String getBuddyInfo();

    public static native String getIonInfo();

    public static native int getMemInfo(long[] jArr);

    private static native long getPssFast(int i, long[] jArr, long[] jArr2);

    public static native int getSumIon();

    public static final native int killProcessGroupForQuickKill(int i, int i2);

    public static native void setThreadVip(int i, int i2, boolean z);

    private ResourceCollector() {
    }

    public static long getPss(int pid, long[] outUssSwapPss, long[] outMemtrack) {
        long pss = isValidPssFast() ? getPssFast(pid, outUssSwapPss, outMemtrack) : 0;
        if (pss <= 0) {
            if (mPssFastRefs.get() < 5) {
                mPssFastRefs.addAndGet(1);
            }
            return Debug.getPss(pid, outUssSwapPss, outMemtrack);
        }
        mPssFastRefs.set(5);
        mIsValidPssFast.set(true);
        return pss;
    }

    private static boolean isValidPssFast() {
        if (mPssFastRefs.get() == 5) {
            return mIsValidPssFast.get();
        }
        return true;
    }
}
