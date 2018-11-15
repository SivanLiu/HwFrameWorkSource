package com.android.server.wm;

import android.os.IBinder;
import java.util.HashMap;
import java.util.Map;

public class HwStartWindowRecord {
    private static HwStartWindowRecord sInstance;
    private Map<String, IBinder> mStartWindowApps = new HashMap();

    public static synchronized HwStartWindowRecord getInstance() {
        HwStartWindowRecord hwStartWindowRecord;
        synchronized (HwStartWindowRecord.class) {
            if (sInstance == null) {
                sInstance = new HwStartWindowRecord();
            }
            hwStartWindowRecord = sInstance;
        }
        return hwStartWindowRecord;
    }

    private HwStartWindowRecord() {
    }

    public void updateStartWindowApp(String packageName, IBinder token) {
        synchronized (this.mStartWindowApps) {
            this.mStartWindowApps.put(packageName, token);
        }
    }

    public boolean checkStartWindowApp(String packageName) {
        synchronized (this.mStartWindowApps) {
            if (this.mStartWindowApps.get(packageName) != null) {
                return true;
            }
            return false;
        }
    }

    public IBinder getTransferFromStartWindowApp(String packageName) {
        IBinder iBinder;
        synchronized (this.mStartWindowApps) {
            iBinder = (IBinder) this.mStartWindowApps.get(packageName);
        }
        return iBinder;
    }

    public void resetStartWindowApp(String packageName) {
        synchronized (this.mStartWindowApps) {
            this.mStartWindowApps.put(packageName, null);
        }
    }

    public boolean isStartWindowApp(String packageName) {
        boolean containsKey;
        synchronized (this.mStartWindowApps) {
            containsKey = this.mStartWindowApps.containsKey(packageName);
        }
        return containsKey;
    }

    public void removeStartWindowApp(String packageName) {
        synchronized (this.mStartWindowApps) {
            this.mStartWindowApps.remove(packageName);
        }
    }

    public void clearStartWindowApp() {
        synchronized (this.mStartWindowApps) {
            this.mStartWindowApps.clear();
        }
    }
}
