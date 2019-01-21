package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemClock;
import android.os.WorkSource;
import android.os.WorkSource.WorkChain;
import android.telephony.Rlog;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RILRequest {
    static final String LOG_TAG = "RilRequest";
    private static final int MAX_POOL_SIZE = 4;
    static AtomicInteger sNextSerial = new AtomicInteger(0);
    private static RILRequest sPool = null;
    private static int sPoolSize = 0;
    private static Object sPoolSync = new Object();
    static Random sRandom = new Random();
    String mClientId;
    RILRequest mNext;
    int mRequest;
    Message mResult;
    int mSerial;
    long mStartTimeMs;
    int mWakeLockType;
    WorkSource mWorkSource;

    public int getSerial() {
        return this.mSerial;
    }

    public int getRequest() {
        return this.mRequest;
    }

    public Message getResult() {
        return this.mResult;
    }

    private static RILRequest obtain(int request, Message result) {
        RILRequest rr = null;
        synchronized (sPoolSync) {
            if (sPool != null) {
                rr = sPool;
                sPool = rr.mNext;
                rr.mNext = null;
                sPoolSize--;
            }
        }
        if (rr == null) {
            rr = new RILRequest();
        }
        rr.mSerial = sNextSerial.getAndIncrement();
        rr.mRequest = request;
        rr.mResult = result;
        rr.mWakeLockType = -1;
        rr.mWorkSource = null;
        rr.mStartTimeMs = SystemClock.elapsedRealtime();
        if (result == null || result.getTarget() != null) {
            return rr;
        }
        throw new NullPointerException("Message target must not be null");
    }

    public static RILRequest obtain(int request, Message result, WorkSource workSource) {
        RILRequest rr = obtain(request, result);
        if (workSource != null) {
            rr.mWorkSource = workSource;
            rr.mClientId = rr.getWorkSourceClientId();
        } else {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("null workSource ");
            stringBuilder.append(request);
            Rlog.e(str, stringBuilder.toString());
        }
        return rr;
    }

    public String getWorkSourceClientId() {
        if (this.mWorkSource == null || this.mWorkSource.isEmpty()) {
            return null;
        }
        if (this.mWorkSource.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mWorkSource.get(0));
            stringBuilder.append(":");
            stringBuilder.append(this.mWorkSource.getName(0));
            return stringBuilder.toString();
        }
        ArrayList<WorkChain> workChains = this.mWorkSource.getWorkChains();
        if (workChains == null || workChains.isEmpty()) {
            return null;
        }
        WorkChain workChain = (WorkChain) workChains.get(0);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(workChain.getAttributionUid());
        stringBuilder2.append(":");
        stringBuilder2.append(workChain.getTags()[0]);
        return stringBuilder2.toString();
    }

    void release() {
        synchronized (sPoolSync) {
            if (sPoolSize < 4) {
                this.mNext = sPool;
                sPool = this;
                sPoolSize++;
                this.mResult = null;
                if (this.mWakeLockType != -1 && this.mWakeLockType == 0) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("RILRequest releasing with held wake lock: ");
                    stringBuilder.append(serialString());
                    Rlog.e(str, stringBuilder.toString());
                }
            }
        }
    }

    private RILRequest() {
    }

    static void resetSerial() {
        sNextSerial.set(sRandom.nextInt());
    }

    String serialString() {
        StringBuilder sb = new StringBuilder(8);
        String sn = Long.toString((((long) this.mSerial) - -2147483648L) % 10000);
        sb.append('[');
        int s = sn.length();
        for (int i = 0; i < 4 - s; i++) {
            sb.append('0');
        }
        sb.append(sn);
        sb.append(']');
        return sb.toString();
    }

    void onError(int error, Object ret) {
        CommandException ex = CommandException.fromRilErrno(error);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(serialString());
        stringBuilder.append("< ");
        stringBuilder.append(RIL.requestToString(this.mRequest));
        stringBuilder.append(" error: ");
        stringBuilder.append(ex);
        stringBuilder.append(" ret=");
        stringBuilder.append(RIL.retToString(this.mRequest, ret));
        Rlog.d(str, stringBuilder.toString());
        if (this.mResult != null) {
            AsyncResult.forMessage(this.mResult, ret, ex);
            this.mResult.sendToTarget();
        }
    }
}
