package com.android.server.location;

import android.content.Context;
import android.os.WorkSource;
import android.util.ArraySet;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GpsFreezeProc implements IGpsFreezeProc {
    private static final int DEFAULT_SIZE = 16;
    private static final Object LOCK = new Object();
    private static String TAG = "GpsFreezeProc";
    private static final long WHITELIST_READ_INTERVAL = 600000;
    private static volatile GpsFreezeProc mGpsFreezeProc;
    private List<String> backgroundThrottlePackageWhitelists = new ArrayList(16);
    private long disableListUpdateTimetamp = 0;
    private final HashMap<Integer, ArraySet<String>> mAllPkgWhiteList = new HashMap<>(16);
    private ArrayList<GpsFreezeListener> mFreezeListenerList = new ArrayList<>(16);
    private HashMap<String, Integer> mFreezeProcesses = new HashMap<>(16);
    private HwLbsConfigManager mHwLbsConfigManager;

    private GpsFreezeProc() {
    }

    public void initHwLbsConfigManager(Context context) {
        this.mHwLbsConfigManager = HwLbsConfigManager.getInstance(context);
    }

    public static GpsFreezeProc getInstance() {
        if (mGpsFreezeProc == null) {
            mGpsFreezeProc = new GpsFreezeProc();
        }
        return mGpsFreezeProc;
    }

    public void addFreezeProcess(String packageName, int uid) {
        LBSLog.i(TAG, false, "addFreezeProcess enter", new Object[0]);
        synchronized (LOCK) {
            this.mFreezeProcesses.put(packageName, Integer.valueOf(uid));
        }
        LBSLog.i(TAG, false, "addFreezeProcess packageName:%{public}s", packageName);
        int size = this.mFreezeListenerList.size();
        for (int i = 0; i < size; i++) {
            this.mFreezeListenerList.get(i).onFreezeProChange(packageName);
        }
    }

    public void removeFreezeProcess(String packageName, int uid) {
        LBSLog.i(TAG, false, "removeFreezeProcess enter", new Object[0]);
        synchronized (LOCK) {
            if (uid == 0) {
                if ("".equals(packageName)) {
                    this.mFreezeProcesses.clear();
                }
            }
            this.mFreezeProcesses.remove(packageName);
        }
        LBSLog.i(TAG, false, "removeFreezeProcess packageName:%{public}s", packageName);
        int size = this.mFreezeListenerList.size();
        for (int i = 0; i < size; i++) {
            this.mFreezeListenerList.get(i).onFreezeProChange(packageName);
        }
    }

    public boolean isFreeze(String packageName) {
        boolean containsKey;
        synchronized (LOCK) {
            LBSLog.d(TAG, false, "isFreeze %{public}s mFreezeProcesses %{public}s", packageName, this.mFreezeProcesses);
            containsKey = this.mFreezeProcesses.containsKey(packageName);
        }
        return containsKey;
    }

    public boolean isInPackageWhiteListByType(int type, String packageName) {
        synchronized (this.mAllPkgWhiteList) {
            if (!this.mAllPkgWhiteList.containsKey(Integer.valueOf(type))) {
                return false;
            }
            return this.mAllPkgWhiteList.get(Integer.valueOf(type)).contains(packageName);
        }
    }

    public void refreshPackageWhitelist(int type, List<String> packageLists) {
        ArraySet<String> packageWhiteLists;
        if (packageLists == null) {
            LBSLog.e(TAG, false, "refreshPackageWhitelist packageLists is null", new Object[0]);
            return;
        }
        synchronized (this.mAllPkgWhiteList) {
            if (this.mAllPkgWhiteList.containsKey(Integer.valueOf(type))) {
                packageWhiteLists = this.mAllPkgWhiteList.get(Integer.valueOf(type));
            } else {
                packageWhiteLists = new ArraySet<>();
            }
            packageWhiteLists.clear();
            packageWhiteLists.addAll(packageLists);
            this.mAllPkgWhiteList.put(Integer.valueOf(type), packageWhiteLists);
        }
        LBSLog.i(TAG, false, "refreshPackageWhitelists pkgs-count:%{public}d ,type = %{public}d", Integer.valueOf(packageLists.size()), Integer.valueOf(type));
        int size = packageLists.size();
        for (int i = 0; i < size; i++) {
            LBSLog.i(TAG, false, "pkgs:%{public}s", packageLists.get(i));
        }
        int size2 = this.mFreezeListenerList.size();
        for (int i2 = 0; i2 < size2; i2++) {
            this.mFreezeListenerList.get(i2).onWhiteListChange(type, packageLists);
        }
    }

    public ArraySet<String> getPackageWhiteList(int type) {
        ArraySet<String> packageLists;
        synchronized (this.mAllPkgWhiteList) {
            if (this.mAllPkgWhiteList.containsKey(Integer.valueOf(type))) {
                packageLists = this.mAllPkgWhiteList.get(Integer.valueOf(type));
            } else {
                packageLists = new ArraySet<>();
            }
            if (type == 1) {
                updateBackgroundThrottleWhiteList();
                for (String packageName : this.backgroundThrottlePackageWhitelists) {
                    if (!packageLists.contains(packageName)) {
                        packageLists.add(packageName);
                    }
                }
            }
        }
        return packageLists;
    }

    public void registerFreezeListener(GpsFreezeListener freezeListener) {
        this.mFreezeListenerList.add(freezeListener);
    }

    public boolean shouldFreeze(WorkSource workSource) {
        boolean shouldFreeze = true;
        int size = workSource.size();
        for (int i = 0; i < size; i++) {
            if (!getInstance().isFreeze(workSource.getName(i))) {
                shouldFreeze = false;
            }
        }
        if (shouldFreeze) {
            LBSLog.i(TAG, false, "should freeze gps", new Object[0]);
        }
        return shouldFreeze;
    }

    private void updateBackgroundThrottleWhiteList() {
        if (this.mHwLbsConfigManager != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.disableListUpdateTimetamp > 600000) {
                this.disableListUpdateTimetamp = currentTime;
                this.backgroundThrottlePackageWhitelists.clear();
                this.backgroundThrottlePackageWhitelists.addAll(this.mHwLbsConfigManager.getListForFeature(LbsConfigContent.CONFIG_BACKGROUNG_THROTTLE_WHITELIST));
                LBSLog.i(TAG, false, "updateWhitelists backgroundThrottlePackageWhitelists %{public}s", this.backgroundThrottlePackageWhitelists);
            }
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("Location Freeze Proc:");
        synchronized (LOCK) {
            Iterator<String> it = this.mFreezeProcesses.keySet().iterator();
            while (it.hasNext()) {
                pw.println("   " + it.next());
            }
        }
        synchronized (this.mAllPkgWhiteList) {
            for (Map.Entry<Integer, ArraySet<String>> entry : this.mAllPkgWhiteList.entrySet()) {
                ArraySet<String> packageLists = entry.getValue();
                pw.println(" type =" + entry.getKey());
                Iterator<String> it2 = packageLists.iterator();
                while (it2.hasNext()) {
                    pw.println("   " + it2.next());
                }
            }
        }
    }
}
