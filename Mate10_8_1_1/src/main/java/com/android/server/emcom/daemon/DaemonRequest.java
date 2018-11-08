package com.android.server.emcom.daemon;

import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonRequest {
    static final String LOG_TAG = "DaemonRequest";
    private static final int MAX_POOL_SIZE = 4;
    static AtomicInteger sNextSerial = new AtomicInteger(0);
    private static DaemonRequest sPool = null;
    private static int sPoolSize = 0;
    private static Object sPoolSync = new Object();
    static Random sRandom = new Random();
    DaemonRequest mNext;
    Parcel mParcel;
    int mRequest;
    Message mResult;
    int mSerial;

    private DaemonRequest() {
    }

    static DaemonRequest obtain(int request, Message result) {
        if (result == null || result.getTarget() != null) {
            DaemonRequest daemonRequest = null;
            synchronized (sPoolSync) {
                if (sPool != null) {
                    daemonRequest = sPool;
                    sPool = daemonRequest.mNext;
                    daemonRequest.mNext = null;
                    sPoolSize--;
                }
            }
            if (daemonRequest == null) {
                daemonRequest = new DaemonRequest();
            }
            daemonRequest.mSerial = sNextSerial.getAndIncrement();
            daemonRequest.mRequest = request;
            daemonRequest.mResult = result;
            daemonRequest.mParcel = Parcel.obtain();
            daemonRequest.mParcel.writeInt(daemonRequest.mRequest);
            daemonRequest.mParcel.writeInt(daemonRequest.mSerial);
            Log.d(LOG_TAG, " DaemonRequest obtain request " + daemonRequest.mRequest + "  rr.mSerial = " + daemonRequest.mSerial);
            return daemonRequest;
        }
        Log.e(LOG_TAG, "result and targe cannot be null!");
        return null;
    }

    static void resetSerial() {
        sNextSerial.set(sRandom.nextInt());
    }

    void release() {
        synchronized (sPoolSync) {
            if (sPoolSize < 4) {
                this.mNext = sPool;
                sPool = this;
                sPoolSize++;
                this.mResult = null;
            }
        }
    }

    void onError(int error) {
        Log.e(LOG_TAG, " DaemonRequest onError");
        this.mResult = null;
        if (this.mParcel != null) {
            this.mParcel.recycle();
            this.mParcel = null;
        }
    }
}
