package com.android.server.rms.iaware.memory.action;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.rms.iaware.AwareLog;
import com.android.server.mtm.iaware.appmng.AwareAppMngSortPolicy;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.taskstatus.ProcessCleaner;
import com.android.server.rms.iaware.appmng.AppMngConfig;
import com.android.server.rms.iaware.memory.utils.EventTracker;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.android.server.rms.iaware.memory.utils.PackageTracker;
import com.android.server.rms.iaware.srms.AppCleanupDumpRadar;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KillAction extends Action {
    private static final int MAX_PROCESS_KILL_COUNT = 5;
    private static final long MIN_LEVEL2_CLEAN_INTERVAL = 300000;
    private static final String TAG = "AwareMem_Kill";
    private final boolean MEM_EMERG_KILL = SystemProperties.getBoolean("persist.iaware.mem_emerg_kill", false);
    private int mInvaildKillCount = 0;
    private long mLastExecTime = 0;
    private int mLastKillZeroCount = 0;
    private long mLastLevel2ExecTime = 0;

    public KillAction(Context context) {
        super(context);
    }

    public int execute(Bundle extras) {
        Bundle bundle = extras;
        if (bundle == null) {
            AwareLog.e(TAG, "null extras!");
            return -1;
        }
        int maxKillCount = 5;
        long availableRam = MemoryReader.getInstance().getMemAvailable();
        if (availableRam <= 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("execute faild to read availableRam =");
            stringBuilder.append(availableRam);
            AwareLog.e(str, stringBuilder.toString());
            return -1;
        }
        boolean invaildKill = false;
        long emergWater = MemoryConstant.getEmergencyMemory();
        long normalWater = MemoryConstant.getMiddleWater();
        if (availableRam < normalWater) {
            maxKillCount = (int) (((long) 5) + (1 + ((normalWater - availableRam) / MemoryConstant.APP_AVG_USS)));
            invaildKill = true;
        }
        int memCleanLevel = 0;
        if (getInvaildKillCount() > 3 && AppMngConfig.getKillMoreFlag()) {
            memCleanLevel = 1;
        }
        AwareAppMngSortPolicy policy = getAwareAppMngSortPolicy(memCleanLevel);
        if (policy == null) {
            AwareLog.w(TAG, "getAppMngSortPolicy null policy!");
            return -1;
        } else if (this.mInterrupt.get()) {
            this.mLastKillZeroCount++;
            this.mInvaildKillCount++;
            return -1;
        } else {
            boolean invaildKill2;
            int maxKillCount2;
            int killedCount = 0;
            List<AwareProcessBlockInfo> procGroups = MemoryUtils.getAppMngProcGroup(policy, 2);
            int maxKillCount3 = maxKillCount;
            long now = SystemClock.elapsedRealtime();
            if (procGroups == null || procGroups.isEmpty()) {
                invaildKill2 = invaildKill;
                maxKillCount2 = maxKillCount3;
                long j = normalWater;
                AwareLog.w(TAG, "empty group list!");
                this.mLastKillZeroCount++;
            } else {
                updateGroupList(procGroups, false);
                maxKillCount2 = maxKillCount3;
                killedCount = execKillGroup(bundle, procGroups, maxKillCount2);
                this.mLastExecTime = now;
                if (killedCount > 0) {
                    invaildKill2 = invaildKill;
                    this.mLastKillZeroCount = false;
                    if (killedCount >= 5) {
                        invaildKill2 = false;
                    }
                } else {
                    invaildKill2 = invaildKill;
                    this.mLastKillZeroCount++;
                }
            }
            if (invaildKill2) {
                this.mInvaildKillCount++;
            } else {
                this.mInvaildKillCount = 0;
            }
            if (this.MEM_EMERG_KILL && availableRam <= emergWater && maxKillCount2 - killedCount > 0 && now - this.mLastLevel2ExecTime > 300000) {
                killedCount += excuteEmergKill(bundle, maxKillCount2 - killedCount);
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("execute: killing count=");
            stringBuilder2.append(killedCount);
            AwareLog.i(str2, stringBuilder2.toString());
            return killedCount > 0 ? 0 : -1;
        }
    }

    private int excuteEmergKill(Bundle extras, int maxKillCount) {
        AwareAppMngSortPolicy policy = getAwareAppMngSortPolicy(2);
        if (policy == null) {
            AwareLog.w(TAG, "getAppMngSortPolicy level2 null policy!");
            return 0;
        }
        int killedCount = 0;
        List<AwareProcessBlockInfo> procGroups = MemoryUtils.getAppMngProcGroup(policy, 2);
        if (!(procGroups == null || procGroups.isEmpty())) {
            updateGroupList(procGroups, true);
            killedCount = execKillGroup(extras, procGroups, maxKillCount);
            this.mLastExecTime = SystemClock.elapsedRealtime();
            this.mLastLevel2ExecTime = SystemClock.elapsedRealtime();
            if (killedCount > 0) {
                this.mLastKillZeroCount = 0;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("execute: killing emerg count=");
        stringBuilder.append(killedCount);
        AwareLog.i(str, stringBuilder.toString());
        return killedCount;
    }

    private AwareAppMngSortPolicy getAwareAppMngSortPolicy(int memCleanLevel) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("request grouplist level=");
        stringBuilder.append(memCleanLevel);
        AwareLog.i(str, stringBuilder.toString());
        return MemoryUtils.getAppMngSortPolicy(2, 3, memCleanLevel);
    }

    public int getInvaildKillCount() {
        return this.mInvaildKillCount;
    }

    public void reset() {
        this.mLastExecTime = 0;
        this.mLastKillZeroCount = 0;
        this.mInvaildKillCount = 0;
    }

    public boolean canBeExecuted() {
        long availableRam = MemoryReader.getInstance().getMemAvailable();
        String str;
        StringBuilder stringBuilder;
        if (availableRam <= 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("canBeExecuted read availableRam err!");
            stringBuilder.append(availableRam);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        }
        long maxStep = 3 * (MemoryConstant.getIdleMemory() - MemoryConstant.getEmergencyMemory());
        if (maxStep <= 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Idle <= Emergency Memory! getIdleMemory=");
            stringBuilder.append(MemoryConstant.getIdleMemory());
            stringBuilder.append(",getEmergencyMemory=");
            stringBuilder.append(MemoryConstant.getEmergencyMemory());
            AwareLog.w(str, stringBuilder.toString());
            return false;
        }
        long curStep = availableRam - MemoryConstant.getEmergencyMemory();
        long interval = ((curStep * curStep) * MemoryConstant.MIN_INTERVAL_OP_TIMEOUT) / ((100 * maxStep) * 1024);
        if (interval <= SystemClock.elapsedRealtime() - this.mLastExecTime) {
            return true;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("canBeExecuted waiting next operation, real interval=");
        stringBuilder2.append(interval);
        AwareLog.i(str2, stringBuilder2.toString());
        return false;
    }

    public int getLastExecFailCount() {
        return this.mLastKillZeroCount;
    }

    private int execKillGroup(Bundle extras, List<AwareProcessBlockInfo> procGroups, int maxKillCount) {
        int i;
        KillAction killAction = this;
        Bundle bundle = extras;
        List<AwareProcessBlockInfo> list = procGroups;
        int i2 = 0;
        if (!(bundle == null || list == null)) {
            int i3 = 1;
            if (procGroups.size() >= 1 && killAction.mContext != null) {
                int position;
                int exeTime;
                int exeTime2 = 0;
                int killedPids = 0;
                long reqMem = bundle.getLong("reqMem");
                int position2 = procGroups.size() - 1;
                while (reqMem > 0 && position2 >= 0) {
                    if (killAction.mInterrupt.get()) {
                        AwareLog.w(TAG, "execKillGroup: mInterrupt, return");
                        i = maxKillCount;
                        position = position2;
                        break;
                    }
                    AwareProcessBlockInfo procGroup = (AwareProcessBlockInfo) list.get(position2);
                    if (procGroup == null) {
                        AwareLog.w(TAG, "execKillGroup: null procGroup");
                        position2--;
                    } else {
                        AwareProcessBlockInfo procGroup2;
                        List<AwareProcessInfo> procs = procGroup.getProcessList();
                        List<AwareProcessInfo> list2;
                        if (procs == null) {
                            i = maxKillCount;
                            list2 = procs;
                            position = position2;
                            procGroup2 = procGroup;
                        } else if (procs.size() < i3) {
                            i = maxKillCount;
                            list2 = procs;
                            position = position2;
                            procGroup2 = procGroup;
                        } else {
                            List<Integer> pids;
                            List<Integer> list3;
                            long j;
                            AwareProcessInfo currentProcess = (AwareProcessInfo) procs.get(i2);
                            String processName = currentProcess.mProcInfo != null ? currentProcess.mProcInfo.mProcessName : null;
                            long beginTime = System.currentTimeMillis();
                            List<Integer> pids2 = ProcessCleaner.getInstance(killAction.mContext).killProcessesSameUidExt(procGroup, killAction.mInterrupt, false, false, "LowMem");
                            String processName2 = processName;
                            i2 = (int) (System.currentTimeMillis() - beginTime);
                            if (pids2 != null) {
                                i3 = killedPids + pids2.size();
                                list2 = procs;
                                pids = pids2;
                                killedPids = i2;
                                position = position2;
                                exeTime = i2;
                                i2 = procGroup;
                                killAction.insertDumpAndStatisticData(bundle, procGroup, pids2, beginTime, killedPids, reqMem);
                                reqMem -= MemoryConstant.APP_AVG_USS * ((long) pids.size());
                                EventTracker instance = EventTracker.getInstance();
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("uid:");
                                stringBuilder.append(i2.mUid);
                                stringBuilder.append(", proc:");
                                stringBuilder.append(processName2);
                                instance.trackEvent(1002, 0, 0, stringBuilder.toString());
                                PackageTracker.getInstance().trackKillEvent(i2.mUid, list2);
                                if (i3 >= maxKillCount) {
                                    String str = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("execKillGroup: Had killed process count=");
                                    stringBuilder2.append(i3);
                                    stringBuilder2.append(", return");
                                    AwareLog.i(str, stringBuilder2.toString());
                                    list3 = pids;
                                    j = beginTime;
                                    break;
                                }
                                killedPids = i3;
                            } else {
                                pids = pids2;
                                list2 = procs;
                                position = position2;
                                exeTime = i2;
                                String processName3 = processName2;
                                currentProcess = maxKillCount;
                                procGroup2 = procGroup;
                                EventTracker instance2 = EventTracker.getInstance();
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("uid:");
                                stringBuilder3.append(procGroup2.mUid);
                                stringBuilder3.append(", proc:");
                                stringBuilder3.append(processName3);
                                stringBuilder3.append(" fail ");
                                instance2.trackEvent(1002, 0, 0, stringBuilder3.toString());
                            }
                            position2 = position - 1;
                            list3 = pids;
                            j = beginTime;
                            exeTime2 = exeTime;
                            killAction = this;
                            bundle = extras;
                            i2 = 0;
                            i3 = 1;
                        }
                        String str2 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("execKillGroup: null process list. uid:");
                        stringBuilder4.append(procGroup2.mUid);
                        stringBuilder4.append(", position:");
                        stringBuilder4.append(position);
                        AwareLog.w(str2, stringBuilder4.toString());
                        position2 = position - 1;
                        killAction = this;
                        bundle = extras;
                        i2 = 0;
                        i3 = 1;
                    }
                }
                i = maxKillCount;
                position = position2;
                exeTime = exeTime2;
                i3 = killedPids;
                AppCleanupDumpRadar.getInstance().reportMemoryData(list, position);
                return i3;
            }
        }
        i = maxKillCount;
        AwareLog.w(TAG, "execKillGroup: null procGroups");
        return 0;
    }

    private void updateGroupList(List<AwareProcessBlockInfo> procGroups, boolean retained) {
        if (PackageTracker.getInstance().isEnabled() && procGroups != null) {
            List<AwareProcessBlockInfo> highFrequencyProcGroups = new ArrayList();
            int oldSize = procGroups.size();
            int i = procGroups.size();
            while (true) {
                i--;
                if (i < 0) {
                    break;
                }
                List<AwareProcessInfo> processList = ((AwareProcessBlockInfo) procGroups.get(i)).mProcessList;
                int uid = ((AwareProcessBlockInfo) procGroups.get(i)).mUid;
                for (String packageName : getPackageList(processList)) {
                    String str;
                    StringBuilder stringBuilder;
                    if (uid < 10000 && processList.get(0) != null && ((AwareProcessInfo) processList.get(0)).mProcInfo != null) {
                        String firstProcessname = ((AwareProcessInfo) processList.get(0)).mProcInfo.mProcessName;
                        if (PackageTracker.getInstance().isProcessCriticalFrequency(packageName, firstProcessname, uid)) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("updateGroupList process: not kill critical frequent=");
                            stringBuilder.append(firstProcessname);
                            stringBuilder.append(", uid=");
                            stringBuilder.append(uid);
                            AwareLog.w(str, stringBuilder.toString());
                            if (retained) {
                                highFrequencyProcGroups.add((AwareProcessBlockInfo) procGroups.get(i));
                            }
                            procGroups.remove(i);
                        } else if (PackageTracker.getInstance().isProcessHighFrequency(packageName, firstProcessname, uid)) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("updateGroupList process: move high frequent=");
                            stringBuilder.append(firstProcessname);
                            stringBuilder.append(", uid=");
                            stringBuilder.append(uid);
                            AwareLog.w(str, stringBuilder.toString());
                            highFrequencyProcGroups.add((AwareProcessBlockInfo) procGroups.get(i));
                            procGroups.remove(i);
                            break;
                        }
                    } else if (uid < 10000) {
                        continue;
                    } else if (PackageTracker.getInstance().isPackageCriticalFrequency(packageName, uid)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateGroupList package: not kill critical frequent=");
                        stringBuilder.append(packageName);
                        stringBuilder.append(", uid=");
                        stringBuilder.append(uid);
                        AwareLog.w(str, stringBuilder.toString());
                        if (retained) {
                            highFrequencyProcGroups.add((AwareProcessBlockInfo) procGroups.get(i));
                        }
                        procGroups.remove(i);
                    } else if (PackageTracker.getInstance().isPackageHighFrequency(packageName, uid)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateGroupList package: move high frequent=");
                        stringBuilder.append(packageName);
                        stringBuilder.append(", uid=");
                        stringBuilder.append(uid);
                        AwareLog.w(str, stringBuilder.toString());
                        highFrequencyProcGroups.add((AwareProcessBlockInfo) procGroups.get(i));
                        procGroups.remove(i);
                        break;
                    }
                }
            }
            for (AwareProcessBlockInfo info : highFrequencyProcGroups) {
                procGroups.add(0, info);
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateGroupList: oldSize=");
            stringBuilder2.append(oldSize);
            stringBuilder2.append(", newSize=");
            stringBuilder2.append(procGroups.size());
            AwareLog.d(str2, stringBuilder2.toString());
        }
    }

    @SuppressLint({"PreferForInArrayList"})
    private List<String> getPackageList(List<AwareProcessInfo> procInfoList) {
        List<String> packageList = new ArrayList();
        if (procInfoList == null) {
            return packageList;
        }
        for (AwareProcessInfo info : procInfoList) {
            if (info.mProcInfo != null) {
                if (info.mProcInfo.mPackageName != null) {
                    Iterator it = info.mProcInfo.mPackageName.iterator();
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

    private void insertDumpAndStatisticData(Bundle extras, AwareProcessBlockInfo procGroup, List<Integer> pids, long beginTime, int exeTime, long reqMem) {
        Bundle bundle = extras;
        AwareProcessBlockInfo awareProcessBlockInfo = procGroup;
        List<Integer> list = pids;
        if (bundle == null || awareProcessBlockInfo == null || list == null) {
            int i = exeTime;
            AwareLog.w(TAG, "insertDumpAndStatisticData: null procGroups");
            return;
        }
        List<AwareProcessInfo> procs = procGroup.getProcessList();
        String appName = bundle.getString("appName");
        int event = bundle.getInt("event");
        long timeStamp = bundle.getLong("timeStamp");
        int cpuLoad = bundle.getInt("cpuLoad");
        boolean cpuBusy = bundle.getBoolean("cpuBusy");
        int effect = (20480 * pids.size()) / 1024;
        StringBuilder reasonSB = new StringBuilder("[");
        reasonSB.append(appName);
        reasonSB.append(",");
        reasonSB.append(event);
        reasonSB.append(",");
        reasonSB.append(timeStamp);
        reasonSB.append("],[");
        reasonSB.append(cpuLoad);
        reasonSB.append(",");
        reasonSB.append(cpuBusy);
        reasonSB.append("],[");
        reasonSB.append(reqMem / 1024);
        reasonSB.append(",");
        reasonSB.append(effect);
        reasonSB = reasonSB.append("]");
        StringBuilder operationSB = new StringBuilder("Kill [");
        operationSB.append(awareProcessBlockInfo.mUid);
        operationSB.append(",");
        List<String> packages = getPackageList(procs);
        operationSB.append(packages);
        operationSB.append(",");
        operationSB.append(list);
        operationSB.append("]");
        EventTracker.getInstance().insertDumpData(beginTime, operationSB.toString(), exeTime, reasonSB.toString());
        EventTracker.getInstance().insertStatisticData("Kill", exeTime, effect);
    }
}
