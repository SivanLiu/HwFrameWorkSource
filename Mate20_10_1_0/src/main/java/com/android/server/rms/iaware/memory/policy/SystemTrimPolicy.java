package com.android.server.rms.iaware.memory.policy;

import android.rms.iaware.AwareLog;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.rms.collector.ResourceCollector;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SystemTrimPolicy {
    private static final AtomicBoolean IS_FEATURE_ENABLE = new AtomicBoolean(false);
    private static final Object LOCK = new Object();
    private static final String TAG = "AwareMem_SysTrim";
    private static SystemTrimPolicy sSystemTrimPolicy = null;
    private final Map<String, Long> mProcThresholds = new ArrayMap();

    private SystemTrimPolicy() {
    }

    public static SystemTrimPolicy getInstance() {
        SystemTrimPolicy systemTrimPolicy;
        synchronized (LOCK) {
            if (sSystemTrimPolicy == null) {
                sSystemTrimPolicy = new SystemTrimPolicy();
            }
            systemTrimPolicy = sSystemTrimPolicy;
        }
        return systemTrimPolicy;
    }

    public void enable() {
        if (IS_FEATURE_ENABLE.get()) {
            AwareLog.d(TAG, "SystemTrimPolicy has already enable!");
        } else {
            IS_FEATURE_ENABLE.set(true);
        }
    }

    public void disable() {
        if (!IS_FEATURE_ENABLE.get()) {
            AwareLog.d(TAG, "SystemTrimPolicy has already disable!");
        } else {
            IS_FEATURE_ENABLE.set(false);
        }
    }

    public void updateProcThreshold(String packageName, long threshold) {
        AwareLog.d(TAG, "set sys app for trim, name=" + packageName + ", threshold=" + threshold);
        synchronized (this.mProcThresholds) {
            this.mProcThresholds.put(packageName, Long.valueOf(threshold));
        }
    }

    public List<AwareProcessInfo> getProcNeedTrim(List<AwareProcessInfo> procsGroups) {
        return getProcOverThreshold(procsGroups, this.mProcThresholds);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x002e, code lost:
        if (r4 == null) goto L_0x003c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0034, code lost:
        if (r4.isEmpty() == false) goto L_0x0037;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x003b, code lost:
        return calcOverThresholdPackages(r4, r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x003c, code lost:
        android.rms.iaware.AwareLog.w(com.android.server.rms.iaware.memory.policy.SystemTrimPolicy.TAG, "no proc need to trim/kill");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0044, code lost:
        return r0;
     */
    public List<AwareProcessInfo> getProcOverThreshold(List<AwareProcessInfo> procsGroups, Map<String, Long> thresholdCached) {
        List<AwareProcessInfo> procInfos = new ArrayList<>();
        AwareLog.d(TAG, "getProcOverThreshold enter");
        if (!IS_FEATURE_ENABLE.get()) {
            AwareLog.d(TAG, "feature has already disable!");
            return procInfos;
        }
        synchronized (thresholdCached) {
            if (thresholdCached.isEmpty()) {
                AwareLog.w(TAG, "no app proc need to trim/kill");
                return procInfos;
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00cf, code lost:
        r0 = th;
     */
    private List<AwareProcessInfo> calcOverThresholdPackages(List<AwareProcessInfo> procsGroups, Map<String, Long> thresholdCached) {
        Set<AwareProcessInfo> allProcInfos = new ArraySet<>();
        List<AwareProcessInfo> procInfos = new ArrayList<>();
        Set<String> apps = new ArraySet<>();
        Map<String, Long> memCaches = new ArrayMap<>();
        for (AwareProcessInfo procInfo : procsGroups) {
            if (procInfo != null) {
                if (procInfo.procProcInfo == null) {
                    continue;
                } else if (procInfo.procProcInfo.mPackageName != null) {
                    if (procInfo.procProcInfo.mPackageName.isEmpty()) {
                        continue;
                    } else {
                        String packageName = (String) procInfo.procProcInfo.mPackageName.get(0);
                        if (TextUtils.isEmpty(packageName)) {
                            continue;
                        } else {
                            String key = procInfo.procProcInfo.mUid + packageName;
                            synchronized (thresholdCached) {
                                if (thresholdCached.containsKey(packageName)) {
                                    long threshold = thresholdCached.get(packageName).longValue();
                                    allProcInfos.add(procInfo);
                                    if (apps.contains(key)) {
                                        AwareLog.d(TAG, "the proc which need to trim/kill is in the list, pkg=" + packageName);
                                    } else {
                                        long currentPss = getPss(procInfo.procProcInfo);
                                        if (memCaches.containsKey(key)) {
                                            currentPss += memCaches.get(key).longValue();
                                        }
                                        memCaches.put(key, Long.valueOf(currentPss));
                                        if (currentPss > threshold) {
                                            apps.add(key);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (AwareProcessInfo proc : allProcInfos) {
            String packageName2 = (String) proc.procProcInfo.mPackageName.get(0);
            if (apps.contains(proc.procProcInfo.mUid + packageName2)) {
                procInfos.add(proc);
                AwareLog.d(TAG, "the proc:" + packageName2 + " is over threshold !");
            }
        }
        return procInfos;
        while (true) {
        }
    }

    private long getPss(ProcessInfo procInfo) {
        if (procInfo == null) {
            return 0;
        }
        long pss = 0;
        int pid = procInfo.mPid;
        int uid = procInfo.mUid;
        if (checkPidValid(pid, uid)) {
            pss = ResourceCollector.getPss(pid, null, null);
        }
        AwareLog.d(TAG, "the proc , pid=" + pid + ", uid=" + uid + ", pss=" + pss + ", procName:" + procInfo.mProcessName);
        return pss;
    }

    private boolean checkPidValid(int pid, int uid) {
        if (new File("/acct/uid_" + uid + "/pid_" + pid).exists()) {
            return true;
        }
        try {
            Object temp = Files.getAttribute(Paths.get("/proc/" + pid + "/status/", new String[0]), "unix:uid", new LinkOption[0]);
            int realUid = -1;
            if (temp instanceof Integer) {
                realUid = ((Integer) temp).intValue();
            }
            if (realUid == uid) {
                return true;
            }
            AwareLog.w(TAG, "read uid " + realUid + " of " + pid + " is not match");
            return false;
        } catch (IOException | IllegalArgumentException | SecurityException | UnsupportedOperationException e) {
            AwareLog.w(TAG, "read status of " + pid + " failed");
            return false;
        }
    }
}
