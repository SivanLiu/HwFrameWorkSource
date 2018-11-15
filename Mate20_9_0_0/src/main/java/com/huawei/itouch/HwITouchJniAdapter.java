package com.huawei.itouch;

import android.util.Log;

public class HwITouchJniAdapter {
    private static HwITouchJniAdapter mHwITouchJniAdapter = null;

    private native int nativeGetAppType();

    private native void nativeRegisterListener();

    static {
        try {
            System.loadLibrary("itouchmanager");
            Log.d("itouch", "itouch loading JNI succ");
        } catch (UnsatisfiedLinkError e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("itouch LoadLibrary is error ");
            stringBuilder.append(e.toString());
            Log.d("itouch", stringBuilder.toString());
        }
    }

    private HwITouchJniAdapter() {
    }

    public static synchronized HwITouchJniAdapter getInstance() {
        HwITouchJniAdapter hwITouchJniAdapter;
        synchronized (HwITouchJniAdapter.class) {
            if (mHwITouchJniAdapter == null) {
                mHwITouchJniAdapter = new HwITouchJniAdapter();
            }
            hwITouchJniAdapter = mHwITouchJniAdapter;
        }
        return hwITouchJniAdapter;
    }

    public synchronized void registerJniListener() {
        nativeRegisterListener();
    }

    public synchronized int getAppType() {
        return nativeGetAppType();
    }
}
