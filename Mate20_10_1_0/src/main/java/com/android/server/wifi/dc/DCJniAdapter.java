package com.android.server.wifi.dc;

import android.text.TextUtils;
import android.util.wifi.HwHiLog;

public class DCJniAdapter {
    private static final String TAG = "DCJniAdapter";
    private static DCJniAdapter mDCJniAdapter = null;

    private native boolean nativeStartOrStopHiP2p(String str, String str2, boolean z);

    private DCJniAdapter() {
    }

    public static synchronized DCJniAdapter getInstance() {
        DCJniAdapter dCJniAdapter;
        synchronized (DCJniAdapter.class) {
            if (mDCJniAdapter == null) {
                mDCJniAdapter = new DCJniAdapter();
            }
            dCJniAdapter = mDCJniAdapter;
        }
        return dCJniAdapter;
    }

    public synchronized boolean startOrStopHiP2p(String masterIfac, String slaveIfac, boolean enable) {
        if (!TextUtils.isEmpty(masterIfac)) {
            if (!TextUtils.isEmpty(slaveIfac)) {
                return nativeStartOrStopHiP2p(masterIfac, slaveIfac, enable);
            }
        }
        HwHiLog.e(TAG, false, "masterIfac or slaveIfac is null", new Object[0]);
        return false;
    }
}
