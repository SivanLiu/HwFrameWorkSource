package com.android.server.mtm.taskstatus;

import android.app.ActivityManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.rms.iaware.appmng.AwareAppMngDFX;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.huawei.android.app.HwActivityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessCleaner {
    private static final int CLEAN_PID_NOTIFICATION = 2;
    private static final int CLEAN_UID_NOTIFICATION = 1;
    private static final int PROTECTED_APP_NUM_FROM_MDM = 3;
    private static final String TAG = "ProcessCleaner";
    private static ProcessCleaner mProcessCleaner = null;
    private ActivityManager mActivityManager;
    Handler mHandler;
    private HwActivityManagerService mHwAMS;
    private ArrayList<String> mMDMProtectedList;
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
        FORCESTOP_ALARM("force-stop-alarm");
        
        String mDescription;

        private CleanType(String description) {
            this.mDescription = description;
        }

        public String description() {
            return this.mDescription;
        }
    }

    private ProcessCleaner(Context context) {
        this.mHwAMS = null;
        this.mProcInfoCollector = null;
        this.mActivityManager = null;
        this.mMDMProtectedList = new ArrayList();
        this.mHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        ProcessCleaner.this.cleanPackageNotifications((List) msg.obj, msg.arg1);
                        return;
                    case 2:
                        ProcessCleaner.this.cleanNotificationWithPid((List) msg.obj, msg.arg1, msg.arg2);
                        return;
                    default:
                        return;
                }
            }
        };
        this.mProcInfoCollector = ProcessInfoCollector.getInstance();
        this.mHwAMS = HwActivityManagerService.self();
        if (this.mHwAMS == null) {
            Slog.e(TAG, "init failed to get HwAMS handler");
        }
        this.mActivityManager = (ActivityManager) context.getSystemService("activity");
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

    public int uniformClean(AwareProcessBlockInfo procGroup, Bundle extras, String reason) {
        int killedCount = 0;
        if (procGroup == null) {
            return 0;
        }
        if (!(procGroup.mProcessList == null || procGroup.mProcessList.isEmpty())) {
            switch (procGroup.mCleanType) {
                case KILL_ALLOW_START:
                case KILL_FORBID_START:
                    for (AwareProcessInfo awareProc : procGroup.mProcessList) {
                        if (killProcess(awareProc.mPid, procGroup.mCleanType == CleanType.KILL_ALLOW_START, reason)) {
                            killedCount++;
                        }
                    }
                    break;
                case REMOVETASK:
                    int result = removetask(procGroup);
                    if (result > 0) {
                        killedCount = 0 + result;
                        break;
                    }
                    break;
                case FORCESTOP_REMOVETASK:
                    if (forcestopAppsAsUser((AwareProcessInfo) procGroup.mProcessList.get(0), reason)) {
                        killedCount = 0 + procGroup.mProcessList.size();
                    }
                    removetask(procGroup);
                    break;
                case FORCESTOP:
                    if (forcestopAppsAsUser((AwareProcessInfo) procGroup.mProcessList.get(0), reason)) {
                        killedCount = 0 + procGroup.mProcessList.size();
                        break;
                    }
                    break;
                case FORCESTOP_ALARM:
                    List<Integer> killedPid = killProcessesSameUidExt(procGroup, null, false, false, reason);
                    if (killedPid != null) {
                        killedCount = 0 + killedPid.size();
                        break;
                    }
                    break;
            }
        }
        return killedCount;
    }

    private Map<String, List<String>> getAlarmTags(int uid, List<String> packageList) {
        Map<String, List<String>> map = null;
        if (packageList == null || packageList.isEmpty()) {
            return null;
        }
        Map<String, List<String>> tags = new ArrayMap();
        boolean clearAll = true;
        for (String pkg : packageList) {
            List<String> list = AwareIntelligentRecg.getInstance().getAllInvalidAlarmTags(uid, pkg);
            if (list != null) {
                clearAll = false;
                tags.put(pkg, list);
            }
        }
        if (!clearAll) {
            map = tags;
        }
        return map;
    }

    public boolean killProcess(int pid, boolean restartservice) {
        return killProcess(pid, restartservice, "null");
    }

    public boolean killProcess(int pid, boolean restartservice, String reason) {
        long start = SystemClock.elapsedRealtime();
        if (this.mProcInfoCollector.INFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("process cleaner kill process: pid is ");
            stringBuilder.append(pid);
            stringBuilder.append(", restart service :");
            stringBuilder.append(restartservice);
            Slog.d(str, stringBuilder.toString());
        }
        ProcessInfo temp = this.mProcInfoCollector.getProcessInfo(pid);
        if (temp == null) {
            Slog.e(TAG, "process cleaner kill process: process info is null ");
            return false;
        } else if (this.mHwAMS == null) {
            Slog.e(TAG, "process cleaner kill process: mHwAMS is null ");
            return false;
        } else if (HwActivityManager.killProcessRecordFromMTM(temp, restartservice, reason)) {
            this.mProcInfoCollector.recordKilledProcess(temp);
            long end = SystemClock.elapsedRealtime();
            if (this.mProcInfoCollector.INFO) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("process cleaner kill process: pid is ");
                stringBuilder2.append(pid);
                stringBuilder2.append(",last time :");
                stringBuilder2.append(end - start);
                Slog.d(str2, stringBuilder2.toString());
            }
            return true;
        } else {
            Slog.e(TAG, "process cleaner kill process: failed to kill ");
            return false;
        }
    }

    public int removetask(AwareProcessBlockInfo procGroup) {
        if (procGroup == null) {
            return 0;
        }
        if (this.mHwAMS == null) {
            AwareLog.e(TAG, "process cleaner kill process: mHwAMS is null ");
            return 0;
        } else if (procGroup.mProcessList == null) {
            return 0;
        } else {
            HashSet<Integer> taskIdSet = new HashSet();
            boolean success = false;
            for (AwareProcessInfo awareProc : procGroup.mProcessList) {
                if (awareProc != null) {
                    taskIdSet.add(Integer.valueOf(awareProc.mTaskId));
                }
            }
            Iterator it = taskIdSet.iterator();
            while (it.hasNext()) {
                Integer taskId = (Integer) it.next();
                if (taskId.intValue() != -1) {
                    if (this.mHwAMS.removeTask(taskId.intValue())) {
                        success = true;
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("fail to removeTask: ");
                        stringBuilder.append(taskId);
                        AwareLog.e(str, stringBuilder.toString());
                    }
                }
            }
            if (success) {
                return procGroup.mProcessList.size();
            }
            return 0;
        }
    }

    public List<Integer> killProcessesSameUidExt(AwareProcessBlockInfo procGroup, boolean quickKillAction, String reason) {
        return killProcessesSameUidExt(procGroup, null, true, quickKillAction, reason);
    }

    public List<Integer> killProcessesSameUidExt(AwareProcessBlockInfo procGroup, AtomicBoolean interrupt, boolean isAsynchronous, boolean quickKillAction, String reason) {
        List<AwareProcessInfo> dfxDataList;
        Throwable pidCantStop;
        List<AwareProcessInfo> list;
        boolean isCleanAllRes;
        boolean hasPerceptAlarm;
        List<AwareProcessInfo> pidsWithNotification;
        boolean hasPerceptAlarm2;
        boolean isCleanAllRes2;
        AwareProcessBlockInfo awareProcessBlockInfo = procGroup;
        String str = reason;
        if (awareProcessBlockInfo == null) {
            return null;
        }
        StringBuilder stringBuilder;
        int targetUid = awareProcessBlockInfo.mUid;
        boolean resCleanAllow = awareProcessBlockInfo.mResCleanAllow;
        if (resCleanAllow) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("iAwareF[");
            stringBuilder.append(str);
            stringBuilder.append("]");
            str = stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("iAwareK[");
            stringBuilder.append(str);
            stringBuilder.append("]");
            str = stringBuilder.toString();
        }
        String reason2 = str;
        List<AwareProcessInfo> procInfoAllStopList = procGroup.getProcessList();
        boolean z;
        int i;
        if (targetUid == 0) {
            z = quickKillAction;
            i = targetUid;
        } else if (procInfoAllStopList == null) {
            z = quickKillAction;
            i = targetUid;
        } else if (this.mHwAMS == null) {
            Slog.e(TAG, "[aware_mem] Why mHwAMS is null!!");
            return null;
        } else if (checkPkgInProtectedListFromMDM(awareProcessBlockInfo.mPackageName)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("[aware_mem] ");
            stringBuilder.append(awareProcessBlockInfo.mPackageName);
            stringBuilder.append(" protected by MDM");
            Slog.d(str, stringBuilder.toString());
            return null;
        } else {
            List<Integer> killList = new ArrayList();
            List<String> packageList = getPackageList(procInfoAllStopList);
            List<AwareProcessInfo> pidsWithNotification2 = getPidsWithNotification(procInfoAllStopList);
            if (this.mProcInfoCollector.INFO) {
                Slog.d(TAG, "[aware_mem] start process cleaner kill process start");
            }
            AwareAppMngSort appMngSort = AwareAppMngSort.getInstance();
            StringBuilder stringBuilder2;
            if (awareProcessBlockInfo.mIsNativeForceStop || appMngSort == null || !appMngSort.isProcessBlockPidChanged(awareProcessBlockInfo)) {
                boolean isCleanAllRes3 = resCleanAllow;
                boolean isCleanUidActivity = false;
                boolean needCheckAlarm = appMngSort != null ? appMngSort.needCheckAlarm(awareProcessBlockInfo) : true;
                Map<String, List<String>> alarmTagMap = null;
                if (!awareProcessBlockInfo.mIsNativeForceStop) {
                    alarmTagMap = getAlarmTags(targetUid, packageList);
                }
                Map<String, List<String>> alarmTagMap2 = alarmTagMap;
                boolean hasPerceptAlarm3 = AwareIntelligentRecg.getInstance().hasPerceptAlarm(targetUid, packageList);
                List<AwareProcessInfo> pidsWithNotification3 = pidsWithNotification2;
                HwActivityManagerService targetUid2 = this.mHwAMS;
                synchronized (targetUid2) {
                    boolean z2;
                    AwareAppMngSort awareAppMngSort;
                    HwActivityManagerService hwActivityManagerService;
                    Map<String, List<String>> map;
                    List<AwareProcessInfo> dfxDataList2;
                    ArraySet<Integer> pidCantStop2;
                    AwareProcessInfo info;
                    int targetUid3;
                    List<AwareProcessInfo> list2;
                    if (resCleanAllow) {
                        try {
                            dfxDataList = null;
                            try {
                                Slog.d(TAG, "[aware_mem] start process cleaner setPackageStoppedState");
                                this.mHwAMS.setPackageStoppedState(packageList, true, targetUid);
                            } catch (Throwable th) {
                                pidCantStop = th;
                                z = quickKillAction;
                                z2 = false;
                                isCleanUidActivity = hasPerceptAlarm3;
                                awareAppMngSort = appMngSort;
                                hwActivityManagerService = targetUid2;
                                map = alarmTagMap2;
                                i = targetUid;
                                list = pidsWithNotification3;
                                targetUid = dfxDataList;
                            }
                        } catch (Throwable th2) {
                            pidCantStop = th2;
                            dfxDataList2 = quickKillAction;
                            z2 = false;
                            isCleanUidActivity = hasPerceptAlarm3;
                            awareAppMngSort = appMngSort;
                            hwActivityManagerService = targetUid2;
                            map = alarmTagMap2;
                            i = targetUid;
                            list = pidsWithNotification3;
                            targetUid = null;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th3) {
                                    pidCantStop = th3;
                                }
                            }
                            throw pidCantStop;
                        }
                    }
                    dfxDataList = null;
                    try {
                        pidCantStop2 = new ArraySet();
                        if (!awareProcessBlockInfo.mIsNativeForceStop) {
                            try {
                                Iterator it = procInfoAllStopList.iterator();
                                while (it.hasNext()) {
                                    Iterator it2;
                                    AwareProcessInfo info2 = (AwareProcessInfo) it.next();
                                    if (appMngSort != null) {
                                        it2 = it;
                                        info = info2;
                                        if (info != null) {
                                            isCleanAllRes = isCleanAllRes3;
                                            try {
                                                if (info.mProcInfo) {
                                                    z2 = isCleanUidActivity;
                                                    try {
                                                        hasPerceptAlarm = hasPerceptAlarm3;
                                                        try {
                                                            map = alarmTagMap2;
                                                        } catch (Throwable th4) {
                                                            pidCantStop = th4;
                                                            map = alarmTagMap2;
                                                            z = quickKillAction;
                                                            awareAppMngSort = appMngSort;
                                                            hwActivityManagerService = targetUid2;
                                                            i = targetUid;
                                                            list = pidsWithNotification3;
                                                            targetUid = dfxDataList;
                                                            isCleanAllRes3 = isCleanAllRes;
                                                            isCleanUidActivity = hasPerceptAlarm;
                                                            while (true) {
                                                                break;
                                                            }
                                                            throw pidCantStop;
                                                        }
                                                    } catch (Throwable th5) {
                                                        pidCantStop = th5;
                                                        map = alarmTagMap2;
                                                        z = quickKillAction;
                                                        isCleanUidActivity = hasPerceptAlarm3;
                                                        awareAppMngSort = appMngSort;
                                                        hwActivityManagerService = targetUid2;
                                                        i = targetUid;
                                                        list = pidsWithNotification3;
                                                        targetUid = dfxDataList;
                                                        isCleanAllRes3 = isCleanAllRes;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw pidCantStop;
                                                    }
                                                    try {
                                                        targetUid3 = targetUid;
                                                        try {
                                                            if (appMngSort.isGroupBeHigher(info.mPid, info.mProcInfo.mUid, info.mProcInfo.mProcessName, info.mProcInfo.mPackageName, info.mMemGroup)) {
                                                                pidCantStop2.add(Integer.valueOf(info.mPid));
                                                            }
                                                        } catch (Throwable th6) {
                                                            pidCantStop = th6;
                                                            z = quickKillAction;
                                                            awareAppMngSort = appMngSort;
                                                            hwActivityManagerService = targetUid2;
                                                            list = pidsWithNotification3;
                                                            list2 = dfxDataList;
                                                            while (true) {
                                                                break;
                                                            }
                                                            throw pidCantStop;
                                                        }
                                                    } catch (Throwable th7) {
                                                        pidCantStop = th7;
                                                        z = quickKillAction;
                                                        awareAppMngSort = appMngSort;
                                                        hwActivityManagerService = targetUid2;
                                                        i = targetUid;
                                                        list = pidsWithNotification3;
                                                        targetUid = dfxDataList;
                                                        isCleanAllRes3 = isCleanAllRes;
                                                        isCleanUidActivity = hasPerceptAlarm;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw pidCantStop;
                                                    }
                                                }
                                                z2 = isCleanUidActivity;
                                                hasPerceptAlarm = hasPerceptAlarm3;
                                                map = alarmTagMap2;
                                                targetUid3 = targetUid;
                                                it = it2;
                                                isCleanAllRes3 = isCleanAllRes;
                                                isCleanUidActivity = z2;
                                                hasPerceptAlarm3 = hasPerceptAlarm;
                                                alarmTagMap2 = map;
                                                targetUid = targetUid3;
                                            } catch (Throwable th8) {
                                                pidCantStop = th8;
                                                z2 = isCleanUidActivity;
                                                map = alarmTagMap2;
                                                z = quickKillAction;
                                                isCleanUidActivity = hasPerceptAlarm3;
                                                awareAppMngSort = appMngSort;
                                                hwActivityManagerService = targetUid2;
                                                i = targetUid;
                                                list = pidsWithNotification3;
                                                targetUid = dfxDataList;
                                                isCleanAllRes3 = isCleanAllRes;
                                                while (true) {
                                                    break;
                                                }
                                                throw pidCantStop;
                                            }
                                        }
                                    }
                                    it2 = it;
                                    isCleanAllRes = isCleanAllRes3;
                                    z2 = isCleanUidActivity;
                                    hasPerceptAlarm = hasPerceptAlarm3;
                                    map = alarmTagMap2;
                                    targetUid3 = targetUid;
                                    it = it2;
                                    isCleanAllRes3 = isCleanAllRes;
                                    isCleanUidActivity = z2;
                                    hasPerceptAlarm3 = hasPerceptAlarm;
                                    alarmTagMap2 = map;
                                    targetUid = targetUid3;
                                }
                            } catch (Throwable th9) {
                                pidCantStop = th9;
                                isCleanAllRes = isCleanAllRes3;
                                z2 = false;
                                map = alarmTagMap2;
                                z = quickKillAction;
                                isCleanUidActivity = hasPerceptAlarm3;
                                awareAppMngSort = appMngSort;
                                hwActivityManagerService = targetUid2;
                                i = targetUid;
                                list = pidsWithNotification3;
                                targetUid = dfxDataList;
                                while (true) {
                                    break;
                                }
                                throw pidCantStop;
                            }
                        }
                        isCleanAllRes = isCleanAllRes3;
                        z2 = isCleanUidActivity;
                        hasPerceptAlarm = hasPerceptAlarm3;
                        map = alarmTagMap2;
                        targetUid3 = targetUid;
                    } catch (Throwable th10) {
                        pidCantStop = th10;
                        z = quickKillAction;
                        isCleanAllRes = isCleanAllRes3;
                        z2 = false;
                        awareAppMngSort = appMngSort;
                        hwActivityManagerService = targetUid2;
                        map = alarmTagMap2;
                        i = targetUid;
                        list = pidsWithNotification3;
                        targetUid = dfxDataList;
                        while (true) {
                            break;
                        }
                        throw pidCantStop;
                    }
                    try {
                        ArraySet<Integer> pidCantStop3;
                        int i2;
                        Object obj;
                        String killHint;
                        boolean z3;
                        Iterator pidsWithNotification4 = procInfoAllStopList.iterator();
                        list2 = dfxDataList;
                        while (pidsWithNotification4.hasNext()) {
                            try {
                                try {
                                    Iterator it3;
                                    String str2;
                                    info = (AwareProcessInfo) pidsWithNotification4.next();
                                    if (interrupt != null) {
                                        try {
                                            if (interrupt.get()) {
                                                pidCantStop3 = pidCantStop2;
                                                i2 = 3;
                                                awareAppMngSort = appMngSort;
                                                hwActivityManagerService = targetUid2;
                                                isCleanUidActivity = false;
                                                pidsWithNotification = pidsWithNotification3;
                                                hasPerceptAlarm2 = hasPerceptAlarm;
                                                i = targetUid3;
                                                obj = 1;
                                                break;
                                            }
                                        } catch (Throwable th11) {
                                            pidCantStop = th11;
                                            z = quickKillAction;
                                            hwActivityManagerService = targetUid2;
                                            while (true) {
                                                break;
                                            }
                                            throw pidCantStop;
                                        }
                                    }
                                    boolean killResult = false;
                                    if (awareProcessBlockInfo.mIsNativeForceStop) {
                                        AwareProcessInfo info3;
                                        try {
                                            info3 = info;
                                            hasPerceptAlarm2 = hasPerceptAlarm;
                                            awareAppMngSort = appMngSort;
                                            hwActivityManagerService = targetUid2;
                                            it3 = pidsWithNotification4;
                                            pidsWithNotification = pidsWithNotification3;
                                        } catch (Throwable th12) {
                                            pidCantStop = th12;
                                            awareAppMngSort = appMngSort;
                                            hwActivityManagerService = targetUid2;
                                            z = quickKillAction;
                                            list = pidsWithNotification3;
                                            isCleanAllRes3 = isCleanAllRes;
                                            isCleanUidActivity = hasPerceptAlarm;
                                            i = targetUid3;
                                            while (true) {
                                                break;
                                            }
                                            throw pidCantStop;
                                        }
                                        try {
                                            killProcessSameUid(info.mPid, info.getRestartFlag(), isAsynchronous, reason2, true);
                                            killResult = true;
                                            pidCantStop3 = pidCantStop2;
                                            pidCantStop2 = info3;
                                        } catch (Throwable th13) {
                                            pidCantStop = th13;
                                            z = quickKillAction;
                                            list = pidsWithNotification;
                                            isCleanAllRes3 = isCleanAllRes;
                                            i = targetUid3;
                                            isCleanUidActivity = hasPerceptAlarm2;
                                            while (true) {
                                                break;
                                            }
                                            throw pidCantStop;
                                        }
                                    }
                                    awareAppMngSort = appMngSort;
                                    hwActivityManagerService = targetUid2;
                                    it3 = pidsWithNotification4;
                                    pidsWithNotification = pidsWithNotification3;
                                    hasPerceptAlarm2 = hasPerceptAlarm;
                                    AwareProcessInfo info4 = info;
                                    try {
                                        if (pidCantStop2.contains(Integer.valueOf(info4.mPid))) {
                                            pidCantStop3 = pidCantStop2;
                                            pidCantStop2 = info4;
                                        } else {
                                            pidCantStop3 = pidCantStop2;
                                            pidCantStop2 = info4;
                                            killResult = killProcessSameUid(info4.mPid, info4.getRestartFlag(), isAsynchronous, reason2, null);
                                        }
                                    } catch (Throwable th14) {
                                        pidCantStop = th14;
                                        i = targetUid3;
                                        z = quickKillAction;
                                        list = pidsWithNotification;
                                        isCleanAllRes3 = isCleanAllRes;
                                        isCleanUidActivity = hasPerceptAlarm2;
                                        while (true) {
                                            break;
                                        }
                                        throw pidCantStop;
                                    }
                                    if (killResult) {
                                        killList.add(Integer.valueOf(pidCantStop2.mPid));
                                        if (resCleanAllow || pidCantStop2.mProcInfo == null || !pidsWithNotification.contains(pidCantStop2) || pidCantStop2.mRestartFlag) {
                                            i = targetUid3;
                                        } else {
                                            Message msg = this.mHandler.obtainMessage(2);
                                            msg.obj = packageList;
                                            i = targetUid3;
                                            try {
                                                msg.arg1 = i;
                                                msg.arg2 = pidCantStop2.mPid;
                                                this.mHandler.sendMessageDelayed(msg, 200);
                                                str2 = TAG;
                                                StringBuilder stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("[aware_mem] clean notification ");
                                                stringBuilder3.append(pidCantStop2.mProcInfo.mProcessName);
                                                Slog.d(str2, stringBuilder3.toString());
                                            } catch (Throwable th15) {
                                                pidCantStop = th15;
                                                z = quickKillAction;
                                                isCleanUidActivity = hasPerceptAlarm2;
                                                while (true) {
                                                    break;
                                                }
                                                throw pidCantStop;
                                            }
                                        }
                                        isCleanUidActivity = (!resCleanAllow && pidCantStop2.mHasShownUi && this.mHwAMS.numOfPidWithActivity(i) == 0) ? true : z2;
                                        try {
                                            if (AwareConstant.CURRENT_USER_TYPE == 3) {
                                                if (list2 == null) {
                                                    dfxDataList2 = new ArrayList();
                                                } else {
                                                    dfxDataList2 = list2;
                                                }
                                                try {
                                                    dfxDataList2.add(pidCantStop2);
                                                    list2 = dfxDataList2;
                                                } catch (Throwable th16) {
                                                    pidCantStop = th16;
                                                    list2 = dfxDataList2;
                                                    z2 = isCleanUidActivity;
                                                    list = pidsWithNotification;
                                                    isCleanAllRes3 = isCleanAllRes;
                                                    isCleanUidActivity = hasPerceptAlarm2;
                                                    dfxDataList2 = quickKillAction;
                                                    while (true) {
                                                        break;
                                                    }
                                                    throw pidCantStop;
                                                }
                                            }
                                            z2 = isCleanUidActivity;
                                        } catch (Throwable th17) {
                                            pidCantStop = th17;
                                            z = quickKillAction;
                                            z2 = isCleanUidActivity;
                                            isCleanUidActivity = hasPerceptAlarm2;
                                            while (true) {
                                                break;
                                            }
                                            throw pidCantStop;
                                        }
                                    }
                                    i = targetUid3;
                                    isCleanAllRes = false;
                                    if (pidCantStop2.mProcInfo != null) {
                                        killHint = killResult ? "success " : "fail ";
                                        str2 = TAG;
                                        isCleanUidActivity = new StringBuilder();
                                        isCleanUidActivity.append("[aware_mem] process cleaner ");
                                        isCleanUidActivity.append(killHint);
                                        isCleanUidActivity.append("pid:");
                                        isCleanUidActivity.append(pidCantStop2.mPid);
                                        isCleanUidActivity.append(",uid:");
                                        isCleanUidActivity.append(pidCantStop2.mProcInfo.mUid);
                                        isCleanUidActivity.append(",");
                                        isCleanUidActivity.append(pidCantStop2.mProcInfo.mProcessName);
                                        isCleanUidActivity.append(",");
                                        isCleanUidActivity.append(pidCantStop2.mProcInfo.mPackageName);
                                        isCleanUidActivity.append(",mHasShownUi:");
                                        isCleanUidActivity.append(pidCantStop2.mHasShownUi);
                                        isCleanUidActivity.append(",");
                                        isCleanUidActivity.append(awareProcessBlockInfo.mSubTypeStr);
                                        isCleanUidActivity.append(",class:");
                                        isCleanUidActivity.append(awareProcessBlockInfo.mClassRate);
                                        isCleanUidActivity.append(",");
                                        isCleanUidActivity.append(awareProcessBlockInfo.mSubClassRate);
                                        isCleanUidActivity.append(",");
                                        isCleanUidActivity.append(pidCantStop2.mClassRate);
                                        isCleanUidActivity.append(",");
                                        isCleanUidActivity.append(pidCantStop2.mSubClassRate);
                                        isCleanUidActivity.append(",adj:");
                                        isCleanUidActivity.append(pidCantStop2.mProcInfo.mCurAdj);
                                        isCleanUidActivity.append(killResult ? " is killed" : "");
                                        Slog.d(str2, isCleanUidActivity.toString());
                                    }
                                    targetUid3 = i;
                                    pidsWithNotification3 = pidsWithNotification;
                                    appMngSort = awareAppMngSort;
                                    targetUid2 = hwActivityManagerService;
                                    hasPerceptAlarm = hasPerceptAlarm2;
                                    pidsWithNotification4 = it3;
                                    pidCantStop2 = pidCantStop3;
                                } catch (Throwable th18) {
                                    pidCantStop = th18;
                                    awareAppMngSort = appMngSort;
                                    hwActivityManagerService = targetUid2;
                                    i = targetUid3;
                                    z = quickKillAction;
                                    list = pidsWithNotification3;
                                    isCleanAllRes3 = isCleanAllRes;
                                    isCleanUidActivity = hasPerceptAlarm;
                                    while (true) {
                                        break;
                                    }
                                    throw pidCantStop;
                                }
                            } catch (Throwable th19) {
                                pidCantStop = th19;
                                z = quickKillAction;
                                awareAppMngSort = appMngSort;
                                hwActivityManagerService = targetUid2;
                                list = pidsWithNotification3;
                                isCleanUidActivity = hasPerceptAlarm;
                                i = targetUid3;
                                while (true) {
                                    break;
                                }
                                throw pidCantStop;
                            }
                        }
                        pidCantStop3 = pidCantStop2;
                        i2 = 3;
                        awareAppMngSort = appMngSort;
                        hwActivityManagerService = targetUid2;
                        pidsWithNotification = pidsWithNotification3;
                        hasPerceptAlarm2 = hasPerceptAlarm;
                        i = targetUid3;
                        obj = 1;
                        isCleanUidActivity = isCleanAllRes;
                        ArraySet<Integer> arraySet;
                        if (isCleanUidActivity) {
                            boolean z4 = false;
                            if (needCheckAlarm) {
                                try {
                                    z4 = this.mHwAMS.isPkgHasAlarm(packageList, i);
                                } catch (Throwable th20) {
                                    pidCantStop = th20;
                                    z = quickKillAction;
                                    isCleanAllRes3 = isCleanUidActivity;
                                    list = pidsWithNotification;
                                    isCleanUidActivity = hasPerceptAlarm2;
                                    while (true) {
                                        break;
                                    }
                                    throw pidCantStop;
                                }
                            }
                            if (z4) {
                                str = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("[aware_mem] is alarm ");
                                stringBuilder2.append(packageList);
                                Slog.d(str, stringBuilder2.toString());
                                this.mHwAMS.setPackageStoppedState(packageList, false, i);
                                isCleanAllRes2 = isCleanUidActivity;
                                list = pidsWithNotification;
                                isCleanUidActivity = hasPerceptAlarm2;
                                arraySet = pidCantStop3;
                            } else {
                                try {
                                    Object obj2 = obj;
                                    list = pidsWithNotification;
                                    z3 = false;
                                    isCleanAllRes2 = isCleanUidActivity;
                                    try {
                                        z4 = HwActivityManager.cleanPackageRes(packageList, map, i, awareProcessBlockInfo.mCleanAlarm, awareProcessBlockInfo.mIsNativeForceStop, hasPerceptAlarm2);
                                        killHint = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("[aware_mem] start process cleaner cleanPackageRes, clnAlarm:");
                                        stringBuilder2.append(awareProcessBlockInfo.mCleanAlarm);
                                        stringBuilder2.append(", hasPerceptAlarm:");
                                        isCleanUidActivity = hasPerceptAlarm2;
                                        stringBuilder2.append(isCleanUidActivity);
                                        stringBuilder2.append(", isNative:");
                                        stringBuilder2.append(awareProcessBlockInfo.mIsNativeForceStop);
                                        stringBuilder2.append(", cleanResult: ");
                                        stringBuilder2.append(z4);
                                        Slog.d(killHint, stringBuilder2.toString());
                                        if (!awareProcessBlockInfo.mIsNativeForceStop && isCleanUidActivity) {
                                            this.mHwAMS.setPackageStoppedState(packageList, z3, i);
                                        }
                                    } catch (Throwable th21) {
                                        pidCantStop = th21;
                                        z = quickKillAction;
                                        isCleanAllRes3 = isCleanAllRes2;
                                        while (true) {
                                            break;
                                        }
                                        throw pidCantStop;
                                    }
                                } catch (Throwable th22) {
                                    pidCantStop = th22;
                                    isCleanAllRes2 = isCleanUidActivity;
                                    list = pidsWithNotification;
                                    isCleanUidActivity = hasPerceptAlarm2;
                                    z = quickKillAction;
                                    isCleanAllRes3 = isCleanAllRes2;
                                    while (true) {
                                        break;
                                    }
                                    throw pidCantStop;
                                }
                            }
                        }
                        isCleanAllRes2 = isCleanUidActivity;
                        list = pidsWithNotification;
                        arraySet = pidCantStop3;
                        z3 = false;
                        if (resCleanAllow) {
                            Slog.d(TAG, "[aware_mem] start process cleaner reset PackageStoppedState");
                            this.mHwAMS.setPackageStoppedState(packageList, z3, i);
                        }
                        if (z2) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("[aware_mem] clean uid activity:");
                            stringBuilder.append(i);
                            Slog.d(str, stringBuilder.toString());
                            this.mHwAMS.cleanActivityByUid(packageList, i);
                        }
                        try {
                            z3 = isCleanAllRes2;
                            if (z3) {
                                Message msg2 = this.mHandler.obtainMessage(1);
                                msg2.obj = packageList;
                                msg2.arg1 = i;
                                this.mHandler.sendMessageDelayed(msg2, 200);
                            }
                            if (this.mProcInfoCollector.INFO) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("[aware_mem] process cleaner kill pids:");
                                stringBuilder.append(killList.toString());
                                Slog.d(str, stringBuilder.toString());
                            }
                            if (AwareConstant.CURRENT_USER_TYPE == 3) {
                                AwareAppMngDFX.getInstance().trackeKillInfo(list2, z3, quickKillAction);
                            } else {
                                z = quickKillAction;
                            }
                            return killList.size() > 0 ? killList : null;
                        } catch (Throwable th23) {
                            pidCantStop = th23;
                            z = quickKillAction;
                            Map<String, List<String>> map2 = isCleanAllRes2;
                            while (true) {
                                break;
                            }
                            throw pidCantStop;
                        }
                    } catch (Throwable th24) {
                        pidCantStop = th24;
                        z = quickKillAction;
                        awareAppMngSort = appMngSort;
                        hwActivityManagerService = targetUid2;
                        list = pidsWithNotification3;
                        isCleanUidActivity = hasPerceptAlarm;
                        i = targetUid3;
                        targetUid = dfxDataList;
                        while (true) {
                            break;
                        }
                        throw pidCantStop;
                    }
                }
            }
            if (this.mProcInfoCollector.INFO) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[aware_mem] new process has started in block, uid: ");
                stringBuilder2.append(targetUid);
                Slog.d(str, stringBuilder2.toString());
            }
            return null;
        }
        return null;
    }

    public boolean forcestopAppsAsUser(AwareProcessInfo awareProc, String reason) {
        if (awareProc == null) {
            return false;
        }
        ProcessInfo temp = awareProc.mProcInfo;
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
            ThreadLocal threadLocal = this.mHwAMS.mLocalStopReason;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iAwareF[");
            stringBuilder.append(reason);
            stringBuilder.append("]");
            threadLocal.set(stringBuilder.toString());
            this.mHwAMS.forceStopPackage(packagename, userId);
            return true;
        }
        AwareLog.e(TAG, "forcestopAppsAsUser process: mActivityManager is null ");
        return false;
    }

    public boolean forcestopApps(int pid) {
        long start = SystemClock.elapsedRealtime();
        if (this.mProcInfoCollector.INFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("forcestopApps kill process: pid is ");
            stringBuilder.append(pid);
            Slog.d(str, stringBuilder.toString());
        }
        ProcessInfo temp = this.mProcInfoCollector.getProcessInfo(pid);
        String str2;
        if (temp == null) {
            Slog.e(TAG, "forcestopApps kill process: process info is null ");
            return false;
        } else if (temp.mCurSchedGroup != 0) {
            str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("forcestopApps kill process: process ");
            stringBuilder2.append(temp.mProcessName);
            stringBuilder2.append(" is not in BG");
            Slog.e(str2, stringBuilder2.toString());
            return false;
        } else {
            str2 = (String) temp.mPackageName.get(0);
            if (str2 == null || str2.equals(" ")) {
                Slog.e(TAG, "forcestopApps kill process: packagename == null");
                return false;
            } else if (this.mActivityManager != null) {
                this.mActivityManager.forceStopPackage(str2);
                this.mProcInfoCollector.recordKilledProcess(temp);
                long end = SystemClock.elapsedRealtime();
                if (this.mProcInfoCollector.INFO) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("pforcestopApps kill process: pid is ");
                    stringBuilder3.append(pid);
                    stringBuilder3.append(",last time :");
                    stringBuilder3.append(end - start);
                    Slog.d(str3, stringBuilder3.toString());
                }
                return true;
            } else {
                Slog.e(TAG, "forcestopApps process: mActivityManager is null ");
                return false;
            }
        }
    }

    private List<String> getPackageList(List<AwareProcessInfo> procInfoAllStopList) {
        List<String> packageList = new ArrayList();
        for (AwareProcessInfo curPIAllStop : procInfoAllStopList) {
            if (curPIAllStop.mProcInfo != null) {
                if (curPIAllStop.mProcInfo.mPackageName != null) {
                    Iterator it = curPIAllStop.mProcInfo.mPackageName.iterator();
                    while (it.hasNext()) {
                        String packageName = (String) it.next();
                        if (!packageList.contains(packageName)) {
                            packageList.add(packageName);
                        }
                    }
                }
            }
        }
        return packageList;
    }

    private List<AwareProcessInfo> getPidsWithNotification(List<AwareProcessInfo> procInfoAllStopList) {
        List<AwareProcessInfo> pidsWithNotification = new ArrayList();
        for (AwareProcessInfo info : procInfoAllStopList) {
            if (hasNotification(info.mPid)) {
                pidsWithNotification.add(info);
            }
        }
        return pidsWithNotification;
    }

    public boolean killProcessSameUid(int pid, boolean restartservice, boolean isAsynchronous, String reason, boolean isNative) {
        long start = SystemClock.elapsedRealtime();
        if (this.mProcInfoCollector.INFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[aware_mem] process cleaner kill process: pid is ");
            stringBuilder.append(pid);
            stringBuilder.append(", restart service :");
            stringBuilder.append(restartservice);
            Slog.d(str, stringBuilder.toString());
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
        } else if (!HwActivityManager.killProcessRecordFromIAware(temp, restartservice, isAsynchronous, reason)) {
            return false;
        }
        this.mProcInfoCollector.recordKilledProcess(temp);
        long end = SystemClock.elapsedRealtime();
        if (this.mProcInfoCollector.INFO) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[aware_mem] process cleaner kill process: pid is ");
            stringBuilder2.append(pid);
            stringBuilder2.append(",last time :");
            stringBuilder2.append(end - start);
            Slog.d(str2, stringBuilder2.toString());
        }
        return true;
    }

    public void setProtectedListFromMDM(List<String> protectedList) {
        if (protectedList == null) {
            Slog.e(TAG, "[aware_mem] Set MDM protected list error");
            return;
        }
        ArrayList<String> tempList = new ArrayList();
        if (protectedList.size() < 3) {
            tempList.addAll(protectedList);
        } else {
            for (int i = 0; i < 3; i++) {
                tempList.add((String) protectedList.get(i));
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[aware_mem] Only 3 apps will be protected from MDM.");
            stringBuilder.append(tempList.toString());
            Slog.d(str, stringBuilder.toString());
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
        ArrayList<String> tempList = new ArrayList();
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

    private void cleanPackageNotifications(List<String> packageList, int targetUid) {
        if (packageList != null) {
            INotificationManager service = NotificationManager.getService();
            if (service != null) {
                int userId = UserHandle.getUserId(targetUid);
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cleanupPackageNotifications, userId=");
                    stringBuilder.append(userId);
                    stringBuilder.append("|");
                    stringBuilder.append(packageList);
                    Slog.v(str, stringBuilder.toString());
                    for (String packageName : packageList) {
                        service.cancelAllNotifications(packageName, userId);
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to talk to notification manager. Woe!");
                }
            }
        }
    }

    private void cleanNotificationWithPid(List<String> packageList, int targetUid, int pid) {
        if (packageList != null) {
            INotificationManager service = NotificationManager.getService();
            if (service != null) {
                try {
                    StatusBarNotification[] notifications = service.getActiveNotifications("android");
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
    }

    private boolean hasNotification(int pid) {
        if (pid < 0) {
            return false;
        }
        INotificationManager service = NotificationManager.getService();
        if (service == null) {
            return false;
        }
        try {
            StatusBarNotification[] notifications = service.getActiveNotifications("android");
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
}
