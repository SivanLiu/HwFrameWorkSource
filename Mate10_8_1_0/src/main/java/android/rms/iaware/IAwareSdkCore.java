package android.rms.iaware;

import android.os.Parcel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class IAwareSdkCore {
    private static final int ASYNC_LOADER_FINISHED = 2;
    private static final int ASYNC_LOADER_IDLE = 0;
    private static final int ASYNC_LOADER_WORKING = 1;
    static final int FIRST_ASYNC_CALL_TRANSACTION = 10001;
    static final int FIRST_SYNC_CALL_TRANSACTION = 1;
    static final int LAST_ASYNC_CALL_TRANSACTION = 16777215;
    static final int LAST_SYNC_CALL_TRANSACTION = 10000;
    private static final int NATIVE_LOADED = 1;
    private static final int NATIVE_LOAD_FAILED = -1;
    private static final int NATIVE_UNLOADED = 0;
    private static String TAG = "iAwareSdkCore";
    private static int mAsyncLoaderStatus = 0;
    private static Object mLoadLock = new Object();
    private static AtomicInteger mNativeStatus = new AtomicInteger(0);
    private static List<AsyncTask> mTasks = new ArrayList();

    private static class AsyncLoader implements Runnable {
        private AsyncLoader() {
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            Throwable th;
            AwareLog.d(IAwareSdkCore.TAG, "Load jni library in the new thread");
            boolean loaded = IAwareSdkCore.loadJniLibrary();
            Iterable iterable = null;
            synchronized (IAwareSdkCore.mTasks) {
                try {
                    IAwareSdkCore.mAsyncLoaderStatus = 2;
                    if (loaded) {
                        List<AsyncTask> tasks = new ArrayList();
                        try {
                            tasks.addAll(IAwareSdkCore.mTasks);
                            iterable = tasks;
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                    IAwareSdkCore.mTasks.clear();
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        }
    }

    private static class AsyncTask {
        private int mCode;
        private Parcel mData;
        private int mExtraInfo;

        public AsyncTask(int code, Parcel data, int extraInfo) {
            this.mCode = code;
            if (data != null) {
                this.mData = Parcel.obtain();
                this.mData.appendFrom(data, 0, data.dataSize());
            } else {
                this.mData = null;
            }
            this.mExtraInfo = extraInfo;
        }

        public void run() {
            Parcel reply = Parcel.obtain();
            if (!IAwareSdkCore.nativeHandleEvent(this.mCode, this.mData, reply, this.mExtraInfo)) {
                AwareLog.v(IAwareSdkCore.TAG, "Failed to handle event");
            }
            if (this.mData != null) {
                this.mData.recycle();
            }
            reply.recycle();
        }
    }

    private static native boolean nativeHandleEvent(int i, Parcel parcel, Parcel parcel2, int i2);

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean loadJniLibrary() {
        boolean z = true;
        synchronized (mLoadLock) {
            int nativeStatus = mNativeStatus.get();
            if (nativeStatus == 0) {
                try {
                    AwareLog.d(TAG, "Load libiAwareSdk_jni.so");
                    System.loadLibrary("iAwareSdk_jni");
                    mNativeStatus.set(1);
                    return true;
                } catch (UnsatisfiedLinkError e) {
                    AwareLog.e(TAG, "ERROR: Could not load libiAwareSdk_jni.so");
                    mNativeStatus.set(-1);
                    return false;
                }
            }
        }
    }

    private static boolean isAsync(int code) {
        return code >= 10001 && code <= 16777215;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean asyncHandleEvent(int code, Parcel data, Parcel reply, int extraInfo) {
        synchronized (mTasks) {
            if (mAsyncLoaderStatus == 0) {
                mAsyncLoaderStatus = 1;
                new Thread(new AsyncLoader()).start();
            }
            if (mAsyncLoaderStatus == 1) {
                mTasks.add(new AsyncTask(code, data, extraInfo));
                return true;
            }
        }
    }

    public static boolean handleEvent(int code, Parcel data, Parcel reply, int extraInfo) {
        int nativeStatus = mNativeStatus.get();
        if (nativeStatus == 1) {
            return nativeHandleEvent(code, data, reply, extraInfo);
        }
        if (nativeStatus == -1) {
            return false;
        }
        if (isAsync(code)) {
            return asyncHandleEvent(code, data, reply, extraInfo);
        }
        if (loadJniLibrary()) {
            return nativeHandleEvent(code, data, reply, extraInfo);
        }
        return false;
    }

    public static boolean handleEvent(int code, Parcel data, Parcel reply) {
        return handleEvent(code, data, reply, -1);
    }
}
