package com.huawei.android.app;

import android.common.HwFrameworkFactory;
import android.os.Process;
import android.util.Log;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;

public class Xcollie {
    public static final int FLAG_BACKTRACE = 2;
    public static final int FLAG_DEFAULT = -1;
    public static final int FLAG_LOG = 1;
    public static final int FLAG_NOOP = 0;
    public static final int FLAG_RECOVERY = 4;
    private static final int INVALID_ID = -1;
    private static final String TAG = "Xcollie-Java";
    private String mName;
    private long mNativeContext;
    private String mPackageName;
    private int mPid;
    private int mTid;
    private int mXid;

    private native void nativeEnd(int i);

    private native int nativeStart(String str, int i, int i2);

    private native int nativeUpdate(int i, int i2);

    static {
        try {
            System.loadLibrary("xcollie_jni");
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Loading xcollie_jni libarary failed >>>>>");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    public Xcollie(String name) {
        this.mPid = 0;
        this.mTid = 0;
        this.mXid = -1;
        this.mPackageName = null;
        this.mName = null;
        this.mNativeContext = 0;
        this.mPid = Process.myPid();
        this.mTid = Process.myTid();
        this.mName = name;
    }

    public Xcollie(String packageName, String name) {
        this.mPid = 0;
        this.mTid = 0;
        this.mXid = -1;
        this.mPackageName = null;
        this.mName = null;
        this.mNativeContext = 0;
        this.mPid = Process.myPid();
        this.mTid = Process.myTid();
        this.mPackageName = packageName;
        this.mName = name;
    }

    public int startTimer(int timeout, int flag) {
        try {
            if (this.mXid == -1) {
                if (timeout > 0) {
                    int id = nativeStart(this.mName, timeout, flag);
                    if (id == -1) {
                        return -1;
                    }
                    this.mXid = id;
                    return 0;
                }
            }
            return -1;
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Loading xcollie_jni libarary failed >>>>>");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public int updateTimer(int timeout) {
        try {
            if (this.mXid != -1) {
                if (timeout > 0) {
                    int id = nativeUpdate(this.mXid, timeout);
                    if (id == -1) {
                        return -1;
                    }
                    this.mXid = id;
                    return 0;
                }
            }
            return -1;
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Loading xcollie_jni libarary failed >>>>>");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public void endTimer() {
        try {
            if (this.mXid != -1) {
                nativeEnd(this.mXid);
                this.mXid = -1;
            }
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Loading xcollie_jni libarary failed >>>>>");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    public void notifyTimeout() {
    }

    private void notifyTimeoutEx() {
        String header = new StringBuilder();
        header.append("timeout pid: ");
        header.append(this.mPid);
        header.append("\ntimeout tid: ");
        header.append(this.mTid);
        header.append("\ntimeout packageName: ");
        header.append(this.mPackageName);
        header.append("\ntimeout name: ");
        header.append(this.mName);
        header.append("\n>>> ");
        header.append(this.mPackageName);
        header.append(" <<<\n\n");
        header = header.toString();
        String stack = new StringBuilder();
        stack.append("tid = ");
        stack.append(this.mTid);
        stack.append("\n");
        stack.append(Log.getStackTraceString(new Throwable()));
        stack.append("\n");
        stack = stack.toString();
        Log.e(TAG, header);
        Log.e(TAG, stack);
        IZrHung mAppEyeXcollie = HwFrameworkFactory.getZrHung("appeye_xcollie");
        if (mAppEyeXcollie != null) {
            ZrHungData args = new ZrHungData();
            args.putInt("pid", this.mPid);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(header);
            stringBuilder.append(stack);
            args.putString("stackTrace", stringBuilder.toString());
            mAppEyeXcollie.sendEvent(args);
        }
        notifyTimeout();
    }
}
