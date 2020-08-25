package com.android.server.mtm.taskstatus;

import android.app.ActivityManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.app.ProcessMap;
import com.android.server.ServiceThread;
import com.android.server.am.HwActivityManagerService;
import com.android.server.am.ProcessRecord;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.appmng.AwareAppMngDFX;
import com.android.server.rms.iaware.appmng.AwareAppUseDataManager;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.cpu.CPUCustBaseConfig;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.huawei.android.app.HwActivityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessCleaner {
    private static final int CLEAN_PID_ACTIVITY = 4;
    private static final int CLEAN_PID_NOTIFICATION = 2;
    private static final int CLEAN_UID_NOTIFICATION = 1;
    private static final int PROTECTED_APP_NUM_FROM_MDM = 10;
    private static final String TAG = "ProcessCleaner";
    private static ProcessCleaner mProcessCleaner = null;
    private ActivityManager mActivityManager;
    private CleanHandler mCleanHandler;
    private HandlerThread mCleanThread;
    protected AtomicBoolean mCleaning;
    Handler mHandler;
    private final HwActivityManagerService mHwAMS;
    private final ArrayList<String> mMDMProtectedList;
    private final ProcessMap<ProcessFastKillInfo> mProcCleanMap;
    private ProcessInfoCollector mProcInfoCollector;

    public enum CleanType {
        NONE("do-nothing"),
        COMPACT("compact"),
        REMOVETASK("removetask"),
        KILL_ALLOW_START("kill-allow-start"),
        KILL_FORBID_START("kill-forbid-start"),
        KILL_DELAY_START("kill-delay-start"),
        FORCESTOP("force-stop"),
        FORCESTOP_REMOVETASK("force-stop-removetask"),
        FREEZE_NOMAL("freeze-nomal"),
        FREEZE_UP_DOWNLOAD("freeze-up-download"),
        IOLIMIT("iolimit"),
        FORCESTOP_ALARM("force-stop-alarm"),
        CPULIMIT("cpulimit");
        
        String mDescription;

        private CleanType(String description) {
            this.mDescription = description;
        }

        public String description() {
            return this.mDescription;
        }
    }

    private final class CleanHandler extends Handler {
        private CleanHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            ProcessFastKillInfo procHold;
            ProcessRecord proc;
            if (msg.what == 4 && (procHold = (ProcessFastKillInfo) msg.obj) != null && (proc = procHold.mApp) != null) {
                IBinder thread = procHold.mAppThread != null ? procHold.mAppThread.asBinder() : null;
                boolean isNative = true;
                if (msg.arg2 != 1) {
                    isNative = false;
                }
                HwActivityManager.cleanProcessResourceFast(proc.processName, procHold.mPid, thread, procHold.mAllowRestart, isNative);
                ProcessCleaner.this.removeProcessFastKillLocked(proc.processName, procHold.mUid);
                AwareLog.d(ProcessCleaner.TAG, "fast kill clean proc: " + proc + ", pid: " + procHold.mPid);
            }
        }
    }

    private ProcessCleaner(Context context) {
        this.mHwAMS = HwActivityManagerService.self();
        this.mProcInfoCollector = null;
        this.mActivityManager = null;
        this.mMDMProtectedList = new ArrayList<>();
        this.mProcCleanMap = new ProcessMap<>();
        this.mHandler = new Handler(Looper.getMainLooper()) {
            /* class com.android.server.mtm.taskstatus.ProcessCleaner.AnonymousClass1 */

            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i == 1) {
                    ProcessCleaner.this.cleanPackageNotifications((List) msg.obj, msg.arg1);
                } else if (i == 2) {
                    ProcessCleaner.this.cleanNotificationWithPid((List) msg.obj, msg.arg1, msg.arg2);
                }
            }
        };
        this.mCleanHandler = null;
        this.mCleanThread = null;
        this.mCleaning = new AtomicBoolean(false);
        this.mProcInfoCollector = ProcessInfoCollector.getInstance();
        if (this.mHwAMS == null) {
            Slog.e(TAG, "init failed to get HwAMS handler");
        }
        this.mActivityManager = (ActivityManager) context.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        this.mCleanThread = new ServiceThread("iaware.clean", -2, false);
        this.mCleanThread.start();
        Looper loop = this.mCleanThread.getLooper();
        if (loop != null) {
            this.mCleanHandler = new CleanHandler(loop);
        }
    }

    public static synchronized ProcessCleaner getInstance(Context context) {
        ProcessCleaner processCleaner;
        synchronized (ProcessCleaner.class) {
            if (mProcessCleaner == null) {
                mProcessCleaner = new ProcessCleaner(context);
            }
            processCleaner = mProcessCleaner;
        }
        return processCleaner;
    }

    public static synchronized ProcessCleaner getInstance() {
        ProcessCleaner processCleaner;
        synchronized (ProcessCleaner.class) {
            processCleaner = mProcessCleaner;
        }
        return processCleaner;
    }

    public int uniformClean(AwareProcessBlockInfo procGroup, Bundle extras, String reason) {
        int killedCount = 0;
        if (procGroup == null || procGroup.procProcessList == null || procGroup.procProcessList.isEmpty()) {
            return 0;
        }
        switch (procGroup.procCleanType) {
            case KILL_ALLOW_START:
            case KILL_FORBID_START:
                for (AwareProcessInfo awareProc : procGroup.procProcessList) {
                    if (killProcess(awareProc.procPid, procGroup.procCleanType == CleanType.KILL_ALLOW_START, reason)) {
                        killedCount++;
                    }
                }
                return killedCount;
            case REMOVETASK:
                int result = removetask(procGroup);
                if (result > 0) {
                    return 0 + result;
                }
                return 0;
            case FORCESTOP_REMOVETASK:
                if (forcestopAppsAsUser(procGroup.procProcessList.get(0), reason)) {
                    killedCount = 0 + procGroup.procProcessList.size();
                }
                removetask(procGroup);
                return killedCount;
            case FORCESTOP:
                if (forcestopAppsAsUser(procGroup.procProcessList.get(0), reason)) {
                    return 0 + procGroup.procProcessList.size();
                }
                return 0;
            case FORCESTOP_ALARM:
                List<Integer> killedPid = killProcessesSameUidExt(procGroup, null, false, false, reason, true);
                if (killedPid != null) {
                    return 0 + killedPid.size();
                }
                return 0;
            default:
                return 0;
        }
    }

    private Map<String, List<String>> getAlarmTags(int uid, List<String> packageList) {
        if (packageList == null || packageList.isEmpty()) {
            return null;
        }
        Map<String, List<String>> tags = new ArrayMap<>();
        boolean clearAll = true;
        for (String pkg : packageList) {
            List<String> list = AwareIntelligentRecg.getInstance().getAllInvalidAlarmTags(uid, pkg);
            if (list != null) {
                clearAll = false;
                tags.put(pkg, list);
            }
        }
        if (clearAll) {
            return null;
        }
        return tags;
    }

    public boolean killProcess(int pid, boolean restartservice) {
        return killProcess(pid, restartservice, "null");
    }

    public boolean killProcess(int pid, boolean restartservice, String reason) {
        long start = SystemClock.elapsedRealtime();
        if (this.mProcInfoCollector.INFO) {
            Slog.d(TAG, "process cleaner kill process: pid is " + pid + ", restart service :" + restartservice);
        }
        ProcessInfo temp = this.mProcInfoCollector.getProcessInfo(pid);
        if (temp == null) {
            Slog.e(TAG, "process cleaner kill process: process info is null ");
            return false;
        } else if (this.mHwAMS == null) {
            Slog.e(TAG, "process cleaner kill process: mHwAMS is null ");
            return false;
        } else if (!HwActivityManager.killProcessRecordFromMTM(temp, restartservice, reason)) {
            Slog.e(TAG, "process cleaner kill process: failed to kill ");
            return false;
        } else {
            this.mProcInfoCollector.recordKilledProcess(temp);
            long end = SystemClock.elapsedRealtime();
            if (!this.mProcInfoCollector.INFO) {
                return true;
            }
            Slog.d(TAG, "process cleaner kill process: pid is " + pid + ",last time :" + (end - start));
            return true;
        }
    }

    public int removetask(AwareProcessBlockInfo procGroup) {
        if (procGroup == null) {
            return 0;
        }
        if (this.mHwAMS == null) {
            AwareLog.e(TAG, "process cleaner kill process: mHwAMS is null ");
            return 0;
        } else if (procGroup.procProcessList == null) {
            return 0;
        } else {
            HashSet<Integer> taskIdSet = new HashSet<>();
            boolean success = false;
            for (AwareProcessInfo awareProc : procGroup.procProcessList) {
                if (awareProc != null) {
                    taskIdSet.add(Integer.valueOf(awareProc.procTaskId));
                }
            }
            Iterator<Integer> it = taskIdSet.iterator();
            while (it.hasNext()) {
                Integer taskId = it.next();
                if (taskId.intValue() != -1) {
                    if (this.mHwAMS.removeTask(taskId.intValue())) {
                        success = true;
                    } else {
                        AwareLog.e(TAG, "fail to removeTask: " + taskId);
                    }
                }
            }
            if (success) {
                return procGroup.procProcessList.size();
            }
            return 0;
        }
    }

    public List<Integer> killProcessesSameUidExt(AwareProcessBlockInfo procGroup, boolean quickKillAction, String reason, boolean needCheckAdj) {
        return killProcessesSameUidExt(procGroup, null, true, quickKillAction, reason, needCheckAdj);
    }

    private String getReason(String reason, boolean resCleanAllow, long lastUseTime) {
        if (resCleanAllow) {
            return "iAwareF[" + reason + "] " + lastUseTime + "ms";
        }
        return "iAwareK[" + reason + "] " + lastUseTime + "ms";
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:214:0x0585 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX INFO: Multiple debug info for r12v19 'info'  com.android.server.mtm.iaware.appmng.AwareProcessInfo: [D('hasPerceptAlarm' boolean), D('info' com.android.server.mtm.iaware.appmng.AwareProcessInfo)] */
    /* JADX WARN: Type inference failed for: r15v3 */
    /* JADX WARN: Type inference failed for: r14v2, types: [java.util.List] */
    /* JADX WARN: Type inference failed for: r0v15, types: [com.android.server.rms.iaware.appmng.AwareAppMngDFX] */
    /* JADX WARN: Type inference failed for: r15v6 */
    /* JADX WARN: Type inference failed for: r14v7 */
    /* JADX WARN: Type inference failed for: r1v47, types: [java.util.List] */
    /* JADX WARN: Type inference failed for: r1v49 */
    /* JADX WARN: Type inference failed for: r1v51, types: [java.util.ArrayList] */
    /* JADX WARN: Type inference failed for: r14v8 */
    /* JADX WARN: Type inference failed for: r14v9 */
    /* JADX WARN: Type inference failed for: r14v10 */
    /* JADX WARN: Type inference failed for: r14v11 */
    /* JADX WARN: Type inference failed for: r14v12 */
    /* JADX WARNING: Code restructure failed: missing block: B:237:0x063e, code lost:
        if (r15 == false) goto L_0x0652;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:238:0x0640, code lost:
        r0 = r36.mHandler.obtainMessage(1);
        r0.obj = r12;
        r0.arg1 = r7;
        r36.mHandler.sendMessageDelayed(r0, 200);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:240:0x0656, code lost:
        if (r36.mProcInfoCollector.INFO == false) goto L_0x0672;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:241:0x0658, code lost:
        android.util.Slog.d(com.android.server.mtm.taskstatus.ProcessCleaner.TAG, "[aware_mem] process cleaner kill pids:" + r10.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:243:0x0675, code lost:
        if (android.rms.iaware.AwareConstant.CURRENT_USER_TYPE != 3) goto L_0x0681;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:244:0x0677, code lost:
        com.android.server.rms.iaware.appmng.AwareAppMngDFX.getInstance().trackeKillInfo(r14, r15, r40);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:247:0x0687, code lost:
        if (r10.size() <= 0) goto L_0x068c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:249:0x068c, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:274:?, code lost:
        return r10;
     */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x025c A[SYNTHETIC, Splitter:B:102:0x025c] */
    /* JADX WARNING: Removed duplicated region for block: B:149:0x0392 A[Catch:{ all -> 0x037a, all -> 0x03a1 }] */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x03ba  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x03e9  */
    /* JADX WARNING: Removed duplicated region for block: B:205:0x0540  */
    /* JADX WARNING: Removed duplicated region for block: B:229:0x05fb A[Catch:{ all -> 0x05de, all -> 0x0615 }] */
    /* JADX WARNING: Removed duplicated region for block: B:235:0x0622 A[Catch:{ all -> 0x05de, all -> 0x0615 }] */
    /* JADX WARNING: Removed duplicated region for block: B:267:0x052b A[SYNTHETIC] */
    /* JADX WARNING: Unknown variable types count: 3 */
    public List<Integer> killProcessesSameUidExt(AwareProcessBlockInfo procGroup, AtomicBoolean interrupt, boolean isAsynchronous, boolean quickKillAction, String reason, boolean needCheckAdj) {
        Map<String, List<String>> alarmTagMap;
        String str;
        List<Integer> killList;
        List<String> packageList;
        List<AwareProcessInfo> pidsWithNotification;
        boolean isCleanUidActivity;
        boolean isCleanAllRes;
        Iterator<AwareProcessInfo> it;
        String str2;
        boolean hasPerceptAlarm;
        List<AwareProcessInfo> procInfoAllStopList;
        List<String> packageList2;
        String str3;
        int targetUid;
        List<Integer> killList2;
        boolean isCleanAllRes2;
        ?? r14;
        boolean killResult;
        String str4;
        boolean hasPerceptAlarm2;
        List<AwareProcessInfo> procInfoAllStopList2;
        int targetUid2;
        AwareAppMngSort appMngSort;
        List<AwareProcessInfo> procInfoAllStopList3;
        AwareProcessInfo info;
        List<Integer> killList3;
        List<String> packageList3;
        int targetUid3;
        String str5;
        List<String> packageList4;
        String killHint;
        boolean isCleanUidActivity2;
        ?? r1;
        List<Integer> killList4;
        List<String> packageList5;
        List<AwareProcessInfo> pidsWithNotification2;
        boolean isCleanUidActivity3;
        Iterator<AwareProcessInfo> it2;
        boolean isCleanAllRes3;
        AwareProcessBlockInfo awareProcessBlockInfo = procGroup;
        if (awareProcessBlockInfo == null) {
            return null;
        }
        int targetUid4 = awareProcessBlockInfo.procUid;
        boolean resCleanAllow = awareProcessBlockInfo.procResCleanAllow;
        String reason2 = getReason(reason, resCleanAllow, AwareAppUseDataManager.getInstance().getLastUseTime(awareProcessBlockInfo.procPackageName));
        List<AwareProcessInfo> pidsWithNotification3 = procGroup.getProcessList();
        if (targetUid4 == 0) {
            return null;
        }
        if (pidsWithNotification3 == null) {
            return null;
        }
        if (this.mHwAMS == null) {
            Slog.e(TAG, "[aware_mem] Why mHwAMS is null!!");
            return null;
        } else if (checkPkgInProtectedListFromMDM(awareProcessBlockInfo.procPackageName)) {
            Slog.d(TAG, "[aware_mem] " + awareProcessBlockInfo.procPackageName + " protected by MDM");
            return null;
        } else {
            List<Integer> killList5 = new ArrayList<>();
            List<String> packageList6 = getPackageList(pidsWithNotification3);
            List<AwareProcessInfo> pidsWithNotification4 = getPidsWithNotification(pidsWithNotification3);
            if (this.mProcInfoCollector.INFO) {
                Slog.d(TAG, "[aware_mem] start process cleaner kill process start");
            }
            AwareAppMngSort appMngSort2 = AwareAppMngSort.getInstance();
            if (awareProcessBlockInfo.procIsNativeForceStop || appMngSort2 == null || !appMngSort2.isProcessBlockPidChanged(awareProcessBlockInfo)) {
                boolean isCleanAllRes4 = resCleanAllow;
                boolean isCleanUidActivity4 = false;
                boolean needCheckAlarm = appMngSort2 != null ? appMngSort2.needCheckAlarm(awareProcessBlockInfo) : true;
                if (!awareProcessBlockInfo.procIsNativeForceStop) {
                    alarmTagMap = getAlarmTags(targetUid4, packageList6);
                } else {
                    alarmTagMap = null;
                }
                boolean hasPerceptAlarm3 = AwareIntelligentRecg.getInstance().hasPerceptAlarm(targetUid4, packageList6);
                synchronized (this.mHwAMS) {
                    if (resCleanAllow) {
                        str = null;
                        try {
                            Slog.d(TAG, "[aware_mem] start process cleaner setPackageStoppedState");
                            this.mHwAMS.setPackageStoppedState(packageList6, true, targetUid4);
                        } catch (Throwable th) {
                            th = th;
                        }
                    } else {
                        str = null;
                    }
                    try {
                        ArraySet<Integer> pidCantStop = new ArraySet<>();
                        if (needCheckAdj) {
                            try {
                                if (!awareProcessBlockInfo.procIsNativeForceStop) {
                                    Iterator<AwareProcessInfo> it3 = pidsWithNotification3.iterator();
                                    while (it3.hasNext()) {
                                        AwareProcessInfo info2 = it3.next();
                                        if (appMngSort2 != null) {
                                            it2 = it3;
                                            if (info2 != null) {
                                                isCleanAllRes3 = isCleanAllRes4;
                                                try {
                                                    if (info2.procProcInfo != null) {
                                                        isCleanUidActivity3 = isCleanUidActivity4;
                                                        try {
                                                            pidsWithNotification2 = pidsWithNotification4;
                                                            try {
                                                                packageList5 = packageList6;
                                                                try {
                                                                    killList4 = killList5;
                                                                    try {
                                                                        if (appMngSort2.isGroupBeHigher(info2.procPid, info2.procProcInfo.mUid, ((ProcessInfo) info2.procProcInfo).mProcessName, ((ProcessInfo) info2.procProcInfo).mPackageName, info2.procMemGroup)) {
                                                                            pidCantStop.add(Integer.valueOf(info2.procPid));
                                                                        }
                                                                    } catch (Throwable th2) {
                                                                        th = th2;
                                                                        while (true) {
                                                                            try {
                                                                                break;
                                                                            } catch (Throwable th3) {
                                                                                th = th3;
                                                                            }
                                                                        }
                                                                        throw th;
                                                                    }
                                                                } catch (Throwable th4) {
                                                                    th = th4;
                                                                    while (true) {
                                                                        break;
                                                                    }
                                                                    throw th;
                                                                }
                                                            } catch (Throwable th5) {
                                                                th = th5;
                                                                while (true) {
                                                                    break;
                                                                }
                                                                throw th;
                                                            }
                                                        } catch (Throwable th6) {
                                                            th = th6;
                                                            while (true) {
                                                                break;
                                                            }
                                                            throw th;
                                                        }
                                                    } else {
                                                        isCleanUidActivity3 = isCleanUidActivity4;
                                                        pidsWithNotification2 = pidsWithNotification4;
                                                        packageList5 = packageList6;
                                                        killList4 = killList5;
                                                    }
                                                } catch (Throwable th7) {
                                                    th = th7;
                                                    while (true) {
                                                        break;
                                                    }
                                                    throw th;
                                                }
                                            } else {
                                                isCleanAllRes3 = isCleanAllRes4;
                                                isCleanUidActivity3 = isCleanUidActivity4;
                                                pidsWithNotification2 = pidsWithNotification4;
                                                packageList5 = packageList6;
                                                killList4 = killList5;
                                            }
                                        } else {
                                            it2 = it3;
                                            isCleanUidActivity3 = isCleanUidActivity4;
                                            pidsWithNotification2 = pidsWithNotification4;
                                            packageList5 = packageList6;
                                            killList4 = killList5;
                                            isCleanAllRes3 = isCleanAllRes4;
                                        }
                                        isCleanAllRes4 = isCleanAllRes3;
                                        it3 = it2;
                                        isCleanUidActivity4 = isCleanUidActivity3;
                                        pidsWithNotification4 = pidsWithNotification2;
                                        packageList6 = packageList5;
                                        killList5 = killList4;
                                    }
                                    isCleanAllRes = isCleanAllRes4;
                                    isCleanUidActivity = isCleanUidActivity4;
                                    pidsWithNotification = pidsWithNotification4;
                                    packageList = packageList6;
                                    killList = killList5;
                                    sortProcListWithStable(pidsWithNotification3);
                                    it = pidsWithNotification3.iterator();
                                    str2 = str;
                                    while (true) {
                                        try {
                                            if (it.hasNext()) {
                                                hasPerceptAlarm = hasPerceptAlarm3;
                                                procInfoAllStopList = pidsWithNotification;
                                                packageList2 = packageList;
                                                str3 = str2;
                                                targetUid = targetUid4;
                                                killList2 = killList;
                                                isCleanAllRes2 = isCleanAllRes;
                                                break;
                                            }
                                            try {
                                                AwareProcessInfo info3 = it.next();
                                                if (interrupt != null) {
                                                    try {
                                                        if (interrupt.get()) {
                                                            hasPerceptAlarm = hasPerceptAlarm3;
                                                            procInfoAllStopList = pidsWithNotification;
                                                            packageList2 = packageList;
                                                            str3 = str2;
                                                            targetUid = targetUid4;
                                                            killList2 = killList;
                                                            isCleanAllRes2 = false;
                                                            break;
                                                        }
                                                    } catch (Throwable th8) {
                                                        th = th8;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                }
                                                killResult = false;
                                                if (awareProcessBlockInfo.procIsNativeForceStop) {
                                                    try {
                                                        appMngSort = appMngSort2;
                                                        hasPerceptAlarm2 = hasPerceptAlarm3;
                                                        procInfoAllStopList2 = pidsWithNotification3;
                                                        procInfoAllStopList3 = pidsWithNotification;
                                                        info = info3;
                                                        packageList3 = packageList;
                                                        str4 = str2;
                                                        targetUid2 = targetUid4;
                                                        killList3 = killList;
                                                    } catch (Throwable th9) {
                                                        th = th9;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                    try {
                                                        killProcessSameUid(info3.procPid, info3.getRestartFlag(), isAsynchronous, reason2, true, needCheckAdj);
                                                        killResult = true;
                                                    } catch (Throwable th10) {
                                                        th = th10;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                } else {
                                                    appMngSort = appMngSort2;
                                                    str4 = str2;
                                                    targetUid2 = targetUid4;
                                                    hasPerceptAlarm2 = hasPerceptAlarm3;
                                                    procInfoAllStopList2 = pidsWithNotification3;
                                                    procInfoAllStopList3 = pidsWithNotification;
                                                    packageList3 = packageList;
                                                    killList3 = killList;
                                                    info = info3;
                                                    try {
                                                        if (!pidCantStop.contains(Integer.valueOf(info.procPid))) {
                                                            killResult = killProcessSameUid(info.procPid, info.getRestartFlag(), isAsynchronous, reason2, false, needCheckAdj);
                                                        }
                                                    } catch (Throwable th11) {
                                                        th = th11;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                }
                                                if (killResult) {
                                                    try {
                                                        killList3.add(Integer.valueOf(info.procPid));
                                                        if (!resCleanAllow) {
                                                            try {
                                                                if (info.procProcInfo != null) {
                                                                    if (!procInfoAllStopList3.contains(info) || info.procRestartFlag) {
                                                                        targetUid3 = targetUid2;
                                                                        if (!resCleanAllow || !info.procHasShownUi || this.mHwAMS.numOfPidWithActivity(targetUid3) != 0) {
                                                                            isCleanUidActivity2 = isCleanUidActivity;
                                                                        } else {
                                                                            isCleanUidActivity2 = true;
                                                                        }
                                                                        if (AwareConstant.CURRENT_USER_TYPE == 3) {
                                                                            if (str4 == null) {
                                                                                try {
                                                                                    r1 = new ArrayList();
                                                                                } catch (Throwable th12) {
                                                                                    th = th12;
                                                                                }
                                                                            } else {
                                                                                r1 = str4;
                                                                            }
                                                                            try {
                                                                                r1.add(info);
                                                                                str5 = r1;
                                                                                isCleanUidActivity = isCleanUidActivity2;
                                                                            } catch (Throwable th13) {
                                                                                th = th13;
                                                                                while (true) {
                                                                                    break;
                                                                                }
                                                                                throw th;
                                                                            }
                                                                        } else {
                                                                            str5 = str4;
                                                                            isCleanUidActivity = isCleanUidActivity2;
                                                                        }
                                                                    } else {
                                                                        Message msg = this.mHandler.obtainMessage(2);
                                                                        msg.obj = packageList3;
                                                                        targetUid3 = targetUid2;
                                                                        msg.arg1 = targetUid3;
                                                                        msg.arg2 = info.procPid;
                                                                        this.mHandler.sendMessageDelayed(msg, 200);
                                                                        Slog.d(TAG, "[aware_mem] clean notification " + info.procProcInfo.mProcessName);
                                                                        if (!resCleanAllow) {
                                                                        }
                                                                        isCleanUidActivity2 = isCleanUidActivity;
                                                                        if (AwareConstant.CURRENT_USER_TYPE == 3) {
                                                                        }
                                                                    }
                                                                }
                                                            } catch (Throwable th14) {
                                                                th = th14;
                                                                while (true) {
                                                                    break;
                                                                }
                                                                throw th;
                                                            }
                                                        }
                                                        targetUid3 = targetUid2;
                                                        if (!resCleanAllow) {
                                                        }
                                                        isCleanUidActivity2 = isCleanUidActivity;
                                                        try {
                                                            if (AwareConstant.CURRENT_USER_TYPE == 3) {
                                                            }
                                                        } catch (Throwable th15) {
                                                            th = th15;
                                                            while (true) {
                                                                break;
                                                            }
                                                            throw th;
                                                        }
                                                    } catch (Throwable th16) {
                                                        th = th16;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                } else {
                                                    targetUid3 = targetUid2;
                                                    str5 = str4;
                                                    isCleanAllRes = false;
                                                }
                                            } catch (Throwable th17) {
                                                th = th17;
                                                while (true) {
                                                    break;
                                                }
                                                throw th;
                                            }
                                            try {
                                                if (info.procProcInfo != null) {
                                                    if (killResult) {
                                                        killHint = "success ";
                                                    } else {
                                                        killHint = "fail ";
                                                    }
                                                    StringBuilder sb = new StringBuilder();
                                                    sb.append("[aware_mem] process cleaner ");
                                                    sb.append(killHint);
                                                    sb.append("pid:");
                                                    sb.append(info.procPid);
                                                    sb.append(",uid:");
                                                    sb.append(info.procProcInfo.mUid);
                                                    sb.append(",");
                                                    sb.append(info.procProcInfo.mProcessName);
                                                    sb.append(",");
                                                    sb.append(info.procProcInfo.mPackageName);
                                                    sb.append(",mHasShownUi:");
                                                    sb.append(info.procHasShownUi);
                                                    sb.append(",");
                                                    packageList4 = packageList3;
                                                    awareProcessBlockInfo = procGroup;
                                                    try {
                                                        sb.append(awareProcessBlockInfo.procSubTypeStr);
                                                        sb.append(",class:");
                                                        sb.append(awareProcessBlockInfo.procClassRate);
                                                        sb.append(",");
                                                        sb.append(awareProcessBlockInfo.procSubClassRate);
                                                        sb.append(",");
                                                        sb.append(info.procClassRate);
                                                        sb.append(",");
                                                        sb.append(info.procSubClassRate);
                                                        sb.append(",adj:");
                                                        sb.append(info.procProcInfo.mCurAdj);
                                                        sb.append(killResult ? " is killed" : "");
                                                        Slog.d(TAG, sb.toString());
                                                    } catch (Throwable th18) {
                                                        th = th18;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                } else {
                                                    packageList4 = packageList3;
                                                    awareProcessBlockInfo = procGroup;
                                                }
                                                packageList = packageList4;
                                                killList = killList3;
                                                pidsWithNotification = procInfoAllStopList3;
                                                appMngSort2 = appMngSort;
                                                pidsWithNotification3 = procInfoAllStopList2;
                                                hasPerceptAlarm3 = hasPerceptAlarm2;
                                                targetUid4 = targetUid3;
                                                str2 = str5;
                                            } catch (Throwable th19) {
                                                th = th19;
                                                while (true) {
                                                    break;
                                                }
                                                throw th;
                                            }
                                        } catch (Throwable th20) {
                                            th = th20;
                                            while (true) {
                                                break;
                                            }
                                            throw th;
                                        }
                                    }
                                    if (!isCleanAllRes2) {
                                        boolean isAlarmFlag = false;
                                        if (needCheckAlarm) {
                                            try {
                                                isAlarmFlag = this.mHwAMS.isPkgHasAlarm(packageList2, targetUid);
                                            } catch (Throwable th21) {
                                                th = th21;
                                                while (true) {
                                                    break;
                                                }
                                                throw th;
                                            }
                                        }
                                        if (isAlarmFlag) {
                                            Slog.d(TAG, "[aware_mem] is alarm " + packageList2);
                                            this.mHwAMS.setPackageStoppedState(packageList2, false, targetUid);
                                            r14 = str3;
                                            procInfoAllStopList = isCleanAllRes2;
                                        } else {
                                            try {
                                                reason2 = str3;
                                                procInfoAllStopList = isCleanAllRes2;
                                                try {
                                                    boolean cleanResult = HwActivityManager.cleanPackageRes(packageList2, alarmTagMap, targetUid, awareProcessBlockInfo.procCleanAlarm, awareProcessBlockInfo.procIsNativeForceStop, hasPerceptAlarm);
                                                    StringBuilder sb2 = new StringBuilder();
                                                    sb2.append("[aware_mem] start process cleaner cleanPackageRes, clnAlarm:");
                                                    sb2.append(awareProcessBlockInfo.procCleanAlarm);
                                                    sb2.append(", hasPerceptAlarm:");
                                                    sb2.append(hasPerceptAlarm);
                                                    sb2.append(", isNative:");
                                                    sb2.append(awareProcessBlockInfo.procIsNativeForceStop);
                                                    sb2.append(", cleanResult: ");
                                                    sb2.append(cleanResult);
                                                    Slog.d(TAG, sb2.toString());
                                                    r14 = reason2;
                                                    r14 = reason2;
                                                    if (!awareProcessBlockInfo.procIsNativeForceStop && hasPerceptAlarm) {
                                                        this.mHwAMS.setPackageStoppedState(packageList2, false, targetUid);
                                                        r14 = reason2;
                                                    }
                                                } catch (Throwable th22) {
                                                    th = th22;
                                                    while (true) {
                                                        break;
                                                    }
                                                    throw th;
                                                }
                                            } catch (Throwable th23) {
                                                th = th23;
                                                while (true) {
                                                    break;
                                                }
                                                throw th;
                                            }
                                        }
                                    } else {
                                        String reason3 = str3;
                                        procInfoAllStopList = isCleanAllRes2;
                                        r14 = reason3;
                                        if (resCleanAllow) {
                                            Slog.d(TAG, "[aware_mem] start process cleaner reset PackageStoppedState");
                                            this.mHwAMS.setPackageStoppedState(packageList2, false, targetUid);
                                            r14 = reason3;
                                        }
                                    }
                                    if (isCleanUidActivity) {
                                        Slog.d(TAG, "[aware_mem] clean uid activity:" + targetUid);
                                        this.mHwAMS.cleanActivityByUid(packageList2, targetUid);
                                    }
                                }
                            } catch (Throwable th24) {
                                th = th24;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        }
                        isCleanAllRes = isCleanAllRes4;
                        isCleanUidActivity = false;
                        pidsWithNotification = pidsWithNotification4;
                        packageList = packageList6;
                        killList = killList5;
                    } catch (Throwable th25) {
                        th = th25;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                    try {
                        sortProcListWithStable(pidsWithNotification3);
                        it = pidsWithNotification3.iterator();
                        str2 = str;
                        while (true) {
                            if (it.hasNext()) {
                            }
                            packageList = packageList4;
                            killList = killList3;
                            pidsWithNotification = procInfoAllStopList3;
                            appMngSort2 = appMngSort;
                            pidsWithNotification3 = procInfoAllStopList2;
                            hasPerceptAlarm3 = hasPerceptAlarm2;
                            targetUid4 = targetUid3;
                            str2 = str5;
                        }
                        if (!isCleanAllRes2) {
                        }
                        if (isCleanUidActivity) {
                        }
                    } catch (Throwable th26) {
                        th = th26;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                }
            } else {
                if (this.mProcInfoCollector.INFO) {
                    Slog.d(TAG, "[aware_mem] new process has started in block, uid: " + targetUid4);
                }
                return null;
            }
        }
    }

    public void beginKillFast() {
        this.mCleaning.set(true);
    }

    public void endKillFast() {
        this.mCleaning.set(false);
    }

    /* JADX INFO: Multiple debug info for r11v5 'info'  com.android.server.mtm.iaware.appmng.AwareProcessInfo: [D('info' com.android.server.mtm.iaware.appmng.AwareProcessInfo), D('procInfoAllStopList' java.util.List<com.android.server.mtm.iaware.appmng.AwareProcessInfo>)] */
    /* JADX INFO: Multiple debug info for r14v8 'pidCantStop'  android.util.ArraySet<java.lang.Integer>: [D('packageList' java.util.List<java.lang.String>), D('pidCantStop' android.util.ArraySet<java.lang.Integer>)] */
    /* JADX WARNING: Code restructure failed: missing block: B:97:0x037a, code lost:
        r0 = r16;
     */
    public List<Integer> killProcessesSameUidFast(AwareProcessBlockInfo procGroup, AtomicBoolean interrupt, boolean isAsynchronous, boolean quickKillAction, String reason, boolean needCheckAdj) {
        int targetUid;
        List<String> packageList;
        String str;
        List<AwareProcessInfo> dfxDataList;
        ArraySet<Integer> pidCantStop;
        boolean resCleanAllow;
        int i;
        List<AwareProcessInfo> dfxDataList2;
        List<String> packageList2;
        int targetUid2;
        String str2;
        boolean isCleanAllRes;
        List<AwareProcessInfo> dfxDataList3;
        List<AwareProcessInfo> procInfoAllStopList;
        ProcessRecord app;
        int appUid;
        Iterator<AwareProcessInfo> it;
        AwareProcessInfo info;
        String appProcName;
        boolean killResult;
        boolean resCleanAllow2;
        int targetUid3;
        String str3;
        List<String> packageList3;
        List<AwareProcessInfo> dfxDataList4;
        AwareAppMngSort appMngSort;
        int targetUid4;
        List<String> packageList4;
        String str4;
        List<AwareProcessInfo> dfxDataList5;
        ArraySet<Integer> pidCantStop2;
        boolean resCleanAllow3;
        if (procGroup == null) {
            return null;
        }
        int targetUid5 = procGroup.procUid;
        List<AwareProcessInfo> procInfoAllStopList2 = procGroup.getProcessList();
        if (targetUid5 == 0) {
            return null;
        }
        if (procInfoAllStopList2 == null) {
            return null;
        }
        HwActivityManagerService hwActivityManagerService = this.mHwAMS;
        String str5 = TAG;
        if (hwActivityManagerService == null) {
            AwareLog.e(str5, "[aware_mem] Why mHwAMS is null!");
            return null;
        } else if (checkPkgInProtectedListFromMDM(procGroup.procPackageName)) {
            AwareLog.d(str5, "[aware_mem] " + procGroup.procPackageName + " protected by MDM");
            return null;
        } else {
            List<Integer> killList = new ArrayList<>();
            List<AwareProcessInfo> dfxDataList6 = null;
            List<String> packageList5 = getPackageList(procInfoAllStopList2);
            List<AwareProcessInfo> pidsWithNotification = getPidsWithNotification(procInfoAllStopList2);
            AwareAppMngSort appMngSort2 = AwareAppMngSort.getInstance();
            if (procGroup.procIsNativeForceStop || appMngSort2 == null || !appMngSort2.isProcessBlockPidChanged(procGroup)) {
                boolean resCleanAllow4 = procGroup.procResCleanAllow;
                boolean isCleanAllRes2 = resCleanAllow4;
                ArraySet<Integer> pidCantStop3 = new ArraySet<>();
                if (!needCheckAdj || procGroup.procIsNativeForceStop) {
                    dfxDataList = null;
                    resCleanAllow = resCleanAllow4;
                    targetUid = targetUid5;
                    str = str5;
                    packageList = packageList5;
                    pidCantStop = pidCantStop3;
                } else {
                    for (AwareProcessInfo info2 : procInfoAllStopList2) {
                        if (appMngSort2 == null || info2 == null || info2.procProcInfo == null) {
                            dfxDataList5 = dfxDataList6;
                            resCleanAllow3 = resCleanAllow4;
                            appMngSort = appMngSort2;
                            targetUid4 = targetUid5;
                            str4 = str5;
                            packageList4 = packageList5;
                            pidCantStop2 = pidCantStop3;
                        } else {
                            dfxDataList5 = dfxDataList6;
                            str4 = str5;
                            targetUid4 = targetUid5;
                            packageList4 = packageList5;
                            pidCantStop2 = pidCantStop3;
                            resCleanAllow3 = resCleanAllow4;
                            appMngSort = appMngSort2;
                            if (appMngSort2.isGroupBeHigher(info2.procPid, info2.procProcInfo.mUid, info2.procProcInfo.mProcessName, ((ProcessInfo) info2.procProcInfo).mPackageName, info2.procMemGroup)) {
                                pidCantStop2.add(Integer.valueOf(info2.procPid));
                            }
                        }
                        resCleanAllow4 = resCleanAllow3;
                        pidCantStop3 = pidCantStop2;
                        dfxDataList6 = dfxDataList5;
                        str5 = str4;
                        packageList5 = packageList4;
                        targetUid5 = targetUid4;
                        appMngSort2 = appMngSort;
                    }
                    dfxDataList = dfxDataList6;
                    resCleanAllow = resCleanAllow4;
                    targetUid = targetUid5;
                    str = str5;
                    packageList = packageList5;
                    pidCantStop = pidCantStop3;
                }
                sortProcListWithStable(procInfoAllStopList2);
                Iterator<AwareProcessInfo> it2 = procInfoAllStopList2.iterator();
                List<AwareProcessInfo> dfxDataList7 = dfxDataList;
                while (true) {
                    if (!it2.hasNext()) {
                        i = 3;
                        dfxDataList2 = dfxDataList7;
                        packageList2 = packageList;
                        targetUid2 = targetUid;
                        str2 = str;
                        break;
                    }
                    AwareProcessInfo info3 = it2.next();
                    if (info3 != null) {
                        if (interrupt != null && interrupt.get()) {
                            i = 3;
                            dfxDataList2 = dfxDataList7;
                            isCleanAllRes = false;
                            packageList2 = packageList;
                            targetUid2 = targetUid;
                            str2 = str;
                            break;
                        }
                        ProcessRecord app2 = this.mHwAMS.getProcessRecordLocked(info3.procPid);
                        if (app2 != null) {
                            int appUid2 = app2.uid;
                            String appProcName2 = app2.processName;
                            if (procGroup.procIsNativeForceStop) {
                                appUid = appUid2;
                                app = app2;
                                it = it2;
                                appProcName = appProcName2;
                                procInfoAllStopList = procInfoAllStopList2;
                                dfxDataList3 = dfxDataList7;
                                info = info3;
                                killResult = killProcessFast(info3.procPid, info3.getRestartFlag(), isAsynchronous, reason, true, needCheckAdj);
                            } else {
                                appUid = appUid2;
                                app = app2;
                                it = it2;
                                procInfoAllStopList = procInfoAllStopList2;
                                dfxDataList3 = dfxDataList7;
                                info = info3;
                                appProcName = appProcName2;
                                if (!pidCantStop.contains(Integer.valueOf(info.procPid))) {
                                    killResult = killProcessFast(info.procPid, info.getRestartFlag(), isAsynchronous, reason, false, needCheckAdj);
                                } else {
                                    killResult = false;
                                }
                            }
                            if (killResult) {
                                ProcessFastKillInfo procHold = new ProcessFastKillInfo(app, appUid, info.procPid, app.thread, info.getRestartFlag());
                                addProcessFastKillLocked(procHold, appProcName, appUid);
                                killList.add(Integer.valueOf(info.procPid));
                                if (resCleanAllow || info.procProcInfo == null) {
                                    packageList3 = packageList;
                                    targetUid3 = targetUid;
                                    resCleanAllow2 = resCleanAllow;
                                    str3 = str;
                                } else if (!pidsWithNotification.contains(info) || info.procRestartFlag) {
                                    packageList3 = packageList;
                                    targetUid3 = targetUid;
                                    resCleanAllow2 = resCleanAllow;
                                    str3 = str;
                                } else {
                                    Message msg = this.mHandler.obtainMessage(2);
                                    packageList3 = packageList;
                                    msg.obj = packageList3;
                                    targetUid3 = targetUid;
                                    msg.arg1 = targetUid3;
                                    msg.arg2 = info.procPid;
                                    resCleanAllow2 = resCleanAllow;
                                    this.mHandler.sendMessageDelayed(msg, 200);
                                    str3 = str;
                                    AwareLog.d(str3, "[aware_mem] clean notification " + info.procProcInfo.mProcessName);
                                }
                                Message msg2 = this.mCleanHandler.obtainMessage(4);
                                msg2.obj = procHold;
                                msg2.arg1 = targetUid3;
                                if (!info.procHasShownUi || info.procStableValue > 1) {
                                    msg2.arg2 = 0;
                                    this.mCleanHandler.sendMessage(msg2);
                                } else {
                                    msg2.arg2 = procGroup.procIsNativeForceStop ? 1 : 0;
                                    this.mCleanHandler.sendMessageAtFrontOfQueue(msg2);
                                }
                                if (AwareConstant.CURRENT_USER_TYPE == 3) {
                                    if (dfxDataList3 == null) {
                                        dfxDataList4 = new ArrayList<>();
                                    } else {
                                        dfxDataList4 = dfxDataList3;
                                    }
                                    dfxDataList4.add(info);
                                    dfxDataList3 = dfxDataList4;
                                }
                            } else {
                                packageList3 = packageList;
                                targetUid3 = targetUid;
                                resCleanAllow2 = resCleanAllow;
                                str3 = str;
                                AwareLog.w(str3, "not clean res, killResult:" + killResult + ", app:" + app + ", pid:" + info.procPid);
                                isCleanAllRes2 = false;
                            }
                            if (info.procProcInfo != null) {
                                String killHint = killResult ? "success " : "fail ";
                                StringBuilder sb = new StringBuilder();
                                sb.append("[aware_mem] fast kill ");
                                sb.append(killHint);
                                sb.append("pid:");
                                sb.append(info.procPid);
                                sb.append(",uid:");
                                sb.append(info.procProcInfo.mUid);
                                sb.append(",");
                                sb.append(info.procProcInfo.mProcessName);
                                sb.append(",");
                                sb.append(info.procProcInfo.mPackageName);
                                sb.append(",mHasShownUi:");
                                sb.append(info.procHasShownUi);
                                sb.append(",");
                                sb.append(procGroup.procSubTypeStr);
                                sb.append(",class:");
                                sb.append(procGroup.procClassRate);
                                sb.append(",");
                                sb.append(procGroup.procSubClassRate);
                                sb.append(",");
                                sb.append(info.procClassRate);
                                sb.append(",");
                                sb.append(info.procSubClassRate);
                                sb.append(",adj:");
                                sb.append(info.procProcInfo.mCurAdj);
                                sb.append(killResult ? " is killed" : "");
                                AwareLog.d(str3, sb.toString());
                            }
                            str = str3;
                            targetUid = targetUid3;
                            it2 = it;
                            resCleanAllow = resCleanAllow2;
                            procInfoAllStopList2 = procInfoAllStopList;
                            dfxDataList7 = dfxDataList3;
                            packageList = packageList3;
                        }
                    } else {
                        i = 3;
                        dfxDataList2 = dfxDataList7;
                        packageList2 = packageList;
                        targetUid2 = targetUid;
                        str2 = str;
                        break;
                    }
                }
                if (isCleanAllRes) {
                    Message msg3 = this.mHandler.obtainMessage(1);
                    msg3.obj = packageList2;
                    msg3.arg1 = targetUid2;
                    this.mHandler.sendMessageDelayed(msg3, 200);
                    AwareLog.d(str2, "[aware_mem] clean uid notification:" + targetUid2 + ", pkg: " + procGroup.procPackageName);
                }
                if (AwareConstant.CURRENT_USER_TYPE == i) {
                    AwareAppMngDFX.getInstance().trackeKillInfo(dfxDataList2, isCleanAllRes, quickKillAction);
                }
                if (killList.size() > 0) {
                    return killList;
                }
                return null;
            }
            AwareLog.d(str5, "[aware_mem] new process has started in block, uid: " + targetUid5);
            return null;
        }
    }

    public boolean forcestopAppsAsUser(AwareProcessInfo awareProc, String reason) {
        if (awareProc == null) {
            return false;
        }
        ProcessInfo temp = awareProc.procProcInfo;
        if (temp == null) {
            AwareLog.e(TAG, "forcestopAppsAsUser kill package: package info is null ");
            return false;
        }
        String packagename = (String) temp.mPackageName.get(0);
        if (packagename == null || packagename.equals(" ")) {
            AwareLog.e(TAG, "forcestopAppsAsUser kill package: packagename == null");
            return false;
        }
        int userId = UserHandle.getUserId(temp.mUid);
        if (this.mHwAMS != null) {
            long lastUseTime = AwareAppUseDataManager.getInstance().getLastUseTime(packagename);
            ThreadLocal threadLocal = this.mHwAMS.mLocalStopReason;
            threadLocal.set("iAwareF[" + reason + "] " + lastUseTime + "ms");
            this.mHwAMS.forceStopPackage(packagename, userId);
            return true;
        }
        AwareLog.e(TAG, "forcestopAppsAsUser process: mActivityManager is null ");
        return false;
    }

    public boolean forcestopApps(int pid) {
        long start = SystemClock.elapsedRealtime();
        if (this.mProcInfoCollector.INFO) {
            Slog.d(TAG, "forcestopApps kill process: pid is " + pid);
        }
        ProcessInfo temp = this.mProcInfoCollector.getProcessInfo(pid);
        if (temp == null) {
            Slog.e(TAG, "forcestopApps kill process: process info is null ");
            return false;
        } else if (temp.mCurSchedGroup != 0) {
            Slog.e(TAG, "forcestopApps kill process: process " + temp.mProcessName + " is not in BG");
            return false;
        } else {
            String packagename = (String) temp.mPackageName.get(0);
            if (packagename == null || packagename.equals(" ")) {
                Slog.e(TAG, "forcestopApps kill process: packagename == null");
                return false;
            }
            ActivityManager activityManager = this.mActivityManager;
            if (activityManager != null) {
                activityManager.forceStopPackage(packagename);
                this.mProcInfoCollector.recordKilledProcess(temp);
                long end = SystemClock.elapsedRealtime();
                if (!this.mProcInfoCollector.INFO) {
                    return true;
                }
                Slog.d(TAG, "pforcestopApps kill process: pid is " + pid + ",last time :" + (end - start));
                return true;
            }
            Slog.e(TAG, "forcestopApps process: mActivityManager is null ");
            return false;
        }
    }

    private List<String> getPackageList(List<AwareProcessInfo> procInfoAllStopList) {
        List<String> packageList = new ArrayList<>();
        for (AwareProcessInfo curPIAllStop : procInfoAllStopList) {
            if (!(curPIAllStop.procProcInfo == null || curPIAllStop.procProcInfo.mPackageName == null)) {
                Iterator it = curPIAllStop.procProcInfo.mPackageName.iterator();
                while (it.hasNext()) {
                    String packageName = (String) it.next();
                    if (!packageList.contains(packageName)) {
                        packageList.add(packageName);
                    }
                }
            }
        }
        return packageList;
    }

    private List<AwareProcessInfo> getPidsWithNotification(List<AwareProcessInfo> procInfoAllStopList) {
        List<AwareProcessInfo> pidsWithNotification = new ArrayList<>();
        for (AwareProcessInfo info : procInfoAllStopList) {
            if (hasNotification(info.procPid)) {
                pidsWithNotification.add(info);
            }
        }
        return pidsWithNotification;
    }

    public boolean killProcessSameUid(int pid, boolean restartservice, boolean isAsynchronous, String reason, boolean isNative, boolean needCheckAdj) {
        long start = SystemClock.elapsedRealtime();
        if (this.mProcInfoCollector.INFO) {
            Slog.d(TAG, "[aware_mem] process cleaner kill process: pid is " + pid + ", restart service :" + restartservice);
        }
        ProcessInfo temp = this.mProcInfoCollector.getProcessInfo(pid);
        if (temp == null) {
            Slog.e(TAG, "[aware_mem] process cleaner kill process: process info is null ");
            return false;
        }
        if (isNative) {
            if (!HwActivityManager.killProcessRecordFromIAwareNative(temp, restartservice, isAsynchronous, reason)) {
                return false;
            }
        } else if (!HwActivityManager.killProcessRecordFromIAware(temp, restartservice, isAsynchronous, reason, needCheckAdj)) {
            return false;
        }
        this.mProcInfoCollector.recordKilledProcess(temp);
        long end = SystemClock.elapsedRealtime();
        if (!this.mProcInfoCollector.INFO) {
            return true;
        }
        Slog.d(TAG, "[aware_mem] process cleaner kill process: pid is " + pid + ",last time :" + (end - start));
        return true;
    }

    private boolean killProcessFast(int pid, boolean restartservice, boolean isAsynchronous, String reason, boolean isNative, boolean needCheckAdj) {
        ProcessInfo proc = this.mProcInfoCollector.getProcessInfo(pid);
        if (proc == null) {
            AwareLog.e(TAG, "[aware_mem] fast kill process: process info is null ");
            return false;
        }
        long lastUseTime = -1;
        if (proc.mPackageName != null && proc.mPackageName.size() > 0) {
            lastUseTime = AwareAppUseDataManager.getInstance().getLastUseTime((String) proc.mPackageName.get(0));
        }
        String reason2 = reason + CPUCustBaseConfig.CPUCONFIG_INVALID_STR + lastUseTime;
        if (isNative) {
            if (!HwActivityManager.killNativeProcessRecordFast(proc.mProcessName, proc.mPid, proc.mUid, restartservice, isAsynchronous, reason2)) {
                return false;
            }
        } else if (!HwActivityManager.killProcessRecordFast(proc.mProcessName, proc.mPid, proc.mUid, restartservice, isAsynchronous, reason2, needCheckAdj)) {
            return false;
        }
        this.mProcInfoCollector.recordKilledProcess(proc);
        AwareLog.d(TAG, "[aware_mem] fast kill proc: " + proc.mProcessName + ", pid: " + pid + ", restart: " + restartservice);
        return true;
    }

    public void setProtectedListFromMDM(List<String> protectedList) {
        if (protectedList == null) {
            Slog.e(TAG, "[aware_mem] Set MDM protected list error");
            return;
        }
        ArrayList<String> tempList = new ArrayList<>();
        if (protectedList.size() < 10) {
            tempList.addAll(protectedList);
        } else {
            for (int i = 0; i < 10; i++) {
                tempList.add(protectedList.get(i));
            }
            Slog.d(TAG, "[aware_mem] Only 10 apps will be protected from MDM." + tempList.toString());
        }
        synchronized (this.mMDMProtectedList) {
            this.mMDMProtectedList.clear();
            this.mMDMProtectedList.addAll(tempList);
        }
    }

    public void removeProtectedListFromMDM() {
        synchronized (this.mMDMProtectedList) {
            this.mMDMProtectedList.clear();
        }
        Slog.d(TAG, "[aware_mem] Remove MDM protected list");
    }

    public ArrayList<String> getProtectedListFromMDM() {
        ArrayList<String> tempList = new ArrayList<>();
        synchronized (this.mMDMProtectedList) {
            tempList.addAll(this.mMDMProtectedList);
        }
        return tempList;
    }

    private boolean checkPkgInProtectedListFromMDM(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        synchronized (this.mMDMProtectedList) {
            if (this.mMDMProtectedList.contains(pkgName)) {
                return true;
            }
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void cleanPackageNotifications(List<String> packageList, int targetUid) {
        INotificationManager service;
        if (packageList != null && (service = NotificationManager.getService()) != null) {
            int userId = UserHandle.getUserId(targetUid);
            try {
                Slog.v(TAG, "cleanupPackageNotifications, userId=" + userId + "|" + packageList);
                for (String packageName : packageList) {
                    service.cancelAllNotifications(packageName, userId);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to talk to notification manager. Woe!");
            }
        }
    }

    /* access modifiers changed from: private */
    public void cleanNotificationWithPid(List<String> packageList, int targetUid, int pid) {
        INotificationManager service;
        if (packageList != null && (service = NotificationManager.getService()) != null) {
            try {
                StatusBarNotification[] notifications = service.getActiveNotifications(AppStartupDataMgr.HWPUSH_PKGNAME);
                int userId = UserHandle.getUserId(targetUid);
                if (notifications != null) {
                    for (StatusBarNotification notification : notifications) {
                        if (notification.getInitialPid() == pid) {
                            for (String packageName : packageList) {
                                service.cancelNotificationWithTag(packageName, notification.getTag(), notification.getId(), userId);
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to talk to notification manager. Woe!");
            }
        }
    }

    private boolean hasNotification(int pid) {
        INotificationManager service;
        if (pid < 0 || (service = NotificationManager.getService()) == null) {
            return false;
        }
        try {
            StatusBarNotification[] notifications = service.getActiveNotifications(AppStartupDataMgr.HWPUSH_PKGNAME);
            if (notifications == null) {
                return false;
            }
            for (StatusBarNotification notification : notifications) {
                if (notification.getInitialPid() == pid) {
                    return true;
                }
            }
            return false;
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to talk to notification manager. Woe!");
        }
    }

    public boolean isProcessFastKillLocked(String procName, int uid) {
        boolean z;
        synchronized (this.mProcCleanMap) {
            z = ((ProcessFastKillInfo) this.mProcCleanMap.get(procName, uid)) != null;
        }
        return z;
    }

    private final void addProcessFastKillLocked(ProcessFastKillInfo app, String procName, int uid) {
        if (app != null) {
            synchronized (this.mProcCleanMap) {
                this.mProcCleanMap.put(procName, uid, app);
            }
        }
    }

    /* access modifiers changed from: private */
    public final void removeProcessFastKillLocked(String procName, int uid) {
        synchronized (this.mProcCleanMap) {
            this.mProcCleanMap.remove(procName, uid);
        }
    }

    private void sortProcListWithStable(List<AwareProcessInfo> procList) {
        AwareAppAssociate.getInstance();
        if (AwareAppAssociate.isEnabled() && procList.size() >= 2) {
            this.mHwAMS.sortProcListWithStable(procList);
        }
    }
}
