package com.android.server.rms.iaware.memory.action;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.rms.iaware.AwareLog;
import com.android.server.mtm.iaware.appmng.AwareAppMngSortPolicy;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.taskstatus.ProcessCleaner;
import com.android.server.rms.iaware.memory.utils.EventTracker;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.android.server.rms.iaware.memory.utils.PackageTracker;
import com.android.server.rms.iaware.srms.AppCleanupDumpRadar;
import java.util.List;

public class QuickKillAction extends Action {
    private static final String TAG = "AwareMem_QuickKill";
    private final boolean MEM_EMERG_KILL = SystemProperties.getBoolean("persist.iaware.mem_emerg_kill", false);
    private AwareAppMngSortPolicy mPolicy;
    private long mReqMem = 0;

    public QuickKillAction(Context context) {
        super(context);
    }

    public int execute(Bundle extras) {
        if (extras == null) {
            AwareLog.e(TAG, "execQuickKillGroup: null extras");
            return -1;
        }
        this.mReqMem = extras.getLong("reqMem");
        if (this.mReqMem > 0) {
            return execQuickKillGroup(extras, getAwareAppMngProcGroup(null));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("QuickKillAction exit cause reqMem is negative: ");
        stringBuilder.append(this.mReqMem);
        AwareLog.d(str, stringBuilder.toString());
        return -1;
    }

    private List<AwareProcessBlockInfo> getAwareAppMngProcGroup(int memCleanLevel) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("grouplist level=");
        stringBuilder.append(memCleanLevel);
        AwareLog.i(str, stringBuilder.toString());
        this.mPolicy = MemoryUtils.getAppMngSortPolicy(2, 3, memCleanLevel);
        if (this.mPolicy != null) {
            return MemoryUtils.getAppMngProcGroup(this.mPolicy, 2);
        }
        AwareLog.w(TAG, "getAppMngSortPolicy null policy!");
        return null;
    }

    public void reset() {
    }

    private int execKillProcess(Bundle extras, int needKillNum, List<AwareProcessBlockInfo> procGroups) {
        QuickKillAction quickKillAction = this;
        List<AwareProcessBlockInfo> list = procGroups;
        if (list == null) {
            AwareLog.w(TAG, "execKillProcess parameter procGroups NULL");
            return needKillNum;
        }
        int appUid;
        long reqMem = quickKillAction.mReqMem;
        int processName = 1;
        int position = procGroups.size() - 1;
        String processName2 = null;
        Bundle bundle = extras;
        int appUid2 = bundle.getInt("appUid");
        int needKillNum2 = needKillNum;
        long reqMem2 = reqMem;
        int position2 = position;
        while (needKillNum2 > 0 && position2 >= 0) {
            AwareProcessBlockInfo procGroup = (AwareProcessBlockInfo) list.get(position2);
            if (procGroup == null) {
                position2--;
            } else {
                boolean z;
                AwareProcessBlockInfo procGroup2;
                List<AwareProcessInfo> procs = procGroup.getProcessList();
                List<AwareProcessInfo> list2;
                if (procs == null) {
                    list2 = procs;
                    z = processName;
                    appUid = appUid2;
                    procGroup2 = procGroup;
                } else if (procs.size() < processName) {
                    list2 = procs;
                    z = processName;
                    appUid = appUid2;
                    procGroup2 = procGroup;
                } else {
                    AwareProcessInfo currentProcess = (AwareProcessInfo) procs.get(0);
                    if (!(currentProcess == null || currentProcess.mProcInfo == null)) {
                        processName2 = currentProcess.mProcInfo.mProcessName;
                    }
                    String processName3 = processName2;
                    StringBuilder stringBuilder;
                    if (appUid2 == procGroup.mUid) {
                        processName2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("execKillProcess: uid ");
                        stringBuilder.append(procGroup.mUid);
                        stringBuilder.append(processName3);
                        stringBuilder.append(" is launching,should not be killed, position:");
                        stringBuilder.append(position2);
                        AwareLog.i(processName2, stringBuilder.toString());
                        position2--;
                        processName2 = processName3;
                        processName = 1;
                    } else {
                        List<Integer> pids;
                        List<AwareProcessInfo> procs2 = procs;
                        long beginTime = System.currentTimeMillis();
                        List<Integer> pids2 = ProcessCleaner.getInstance(quickKillAction.mContext).killProcessesSameUidExt(procGroup, true, "LowMemQuick");
                        int exeTime = (int) (System.currentTimeMillis() - beginTime);
                        int exeTime2;
                        if (pids2 != null) {
                            int needKillNum3 = needKillNum2 - pids2.size();
                            List<AwareProcessInfo> procs3 = procs2;
                            exeTime2 = exeTime;
                            list2 = procs3;
                            appUid = appUid2;
                            appUid2 = procGroup;
                            pids = pids2;
                            z = true;
                            quickKillAction.insertDumpAndStatisticData(bundle, procGroup, pids2, beginTime, exeTime2, reqMem2);
                            reqMem2 -= MemoryConstant.APP_AVG_USS * ((long) pids.size());
                            EventTracker instance = EventTracker.getInstance();
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("QuickKill,uid:");
                            stringBuilder2.append(appUid2.mUid);
                            stringBuilder2.append(", processName:");
                            stringBuilder2.append(processName3);
                            instance.trackEvent(1002, 0, 0, stringBuilder2.toString());
                            PackageTracker.getInstance().trackKillEvent(appUid2.mUid, list2);
                            needKillNum2 = needKillNum3;
                        } else {
                            pids = pids2;
                            z = true;
                            appUid = appUid2;
                            list2 = procs2;
                            exeTime2 = exeTime;
                            appUid2 = procGroup;
                            EventTracker instance2 = EventTracker.getInstance();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("QuickKill,uid:");
                            stringBuilder.append(appUid2.mUid);
                            stringBuilder.append(", processName:");
                            stringBuilder.append(processName3);
                            stringBuilder.append(" failed!");
                            instance2.trackEvent(1002, 0, 0, stringBuilder.toString());
                        }
                        position2--;
                        processName2 = processName3;
                        pids2 = pids;
                        long j = beginTime;
                        appUid2 = appUid;
                        processName = z;
                        quickKillAction = this;
                        bundle = extras;
                    }
                }
                String str = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("execKillProcess: null process list. uid:");
                stringBuilder3.append(procGroup2.mUid);
                stringBuilder3.append(", position:");
                stringBuilder3.append(position2);
                AwareLog.w(str, stringBuilder3.toString());
                position2--;
                appUid2 = appUid;
                processName = z;
                quickKillAction = this;
                bundle = extras;
            }
        }
        appUid = appUid2;
        AppCleanupDumpRadar.getInstance().reportMemoryData(list, position2);
        return needKillNum2;
    }

    private int execQuickKillGroup(Bundle extras, List<AwareProcessBlockInfo> procGroups) {
        int needKillNum = (int) ((this.mReqMem / MemoryConstant.APP_AVG_USS) + 4);
        if (this.mContext == null) {
            AwareLog.e(TAG, "execQuickKillGroup: mContext = NULL!");
            return -1;
        } else if (procGroups == null || procGroups.size() < 1) {
            AwareLog.d(TAG, "getProcessActionGroup:null!");
            return -1;
        } else {
            List<AwareProcessBlockInfo> procGroupsLevel1;
            String str;
            StringBuilder stringBuilder;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getProcessActionGroup:");
            stringBuilder2.append(procGroups.size());
            stringBuilder2.append(" needKillNum:");
            stringBuilder2.append(needKillNum);
            AwareLog.d(str2, stringBuilder2.toString());
            needKillNum = execKillProcess(extras, needKillNum, procGroups);
            if (needKillNum > 0) {
                AwareLog.d(TAG, "get getProcessActionGroup1");
                procGroupsLevel1 = getAwareAppMngProcGroup(1);
                if (procGroupsLevel1 != null) {
                    needKillNum = execKillProcess(extras, needKillNum, procGroupsLevel1);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getProcessActionGroup1:");
                    stringBuilder.append(procGroupsLevel1.size());
                    stringBuilder.append(" needKillNum:");
                    stringBuilder.append(needKillNum);
                    AwareLog.d(str, stringBuilder.toString());
                }
            }
            if (this.MEM_EMERG_KILL && needKillNum > 0) {
                AwareLog.d(TAG, "get getProcessActionGroup2");
                procGroupsLevel1 = getAwareAppMngProcGroup(2);
                if (procGroupsLevel1 != null) {
                    needKillNum = execKillProcess(extras, needKillNum, procGroupsLevel1);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getProcessActionGroup2:");
                    stringBuilder.append(procGroupsLevel1.size());
                    stringBuilder.append(" needKillNum:");
                    stringBuilder.append(needKillNum);
                    AwareLog.d(str, stringBuilder.toString());
                }
            }
            if (this.MEM_EMERG_KILL && needKillNum > 0) {
                AwareLog.d(TAG, "get getProcessActionGroup3");
                procGroupsLevel1 = getAwareAppMngProcGroup(3);
                if (procGroupsLevel1 != null) {
                    needKillNum = execKillProcess(extras, needKillNum, procGroupsLevel1);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getProcessActionGroup3:");
                    stringBuilder.append(procGroupsLevel1.size());
                    stringBuilder.append(" needKillNum:");
                    stringBuilder.append(needKillNum);
                    AwareLog.d(str, stringBuilder.toString());
                }
            }
            return 0;
        }
    }

    private void insertDumpAndStatisticData(Bundle extras, AwareProcessBlockInfo procGroup, List<Integer> pids, long beginTime, int exeTime, long reqMem) {
        Bundle bundle = extras;
        AwareProcessBlockInfo awareProcessBlockInfo = procGroup;
        List<Integer> list = pids;
        if (bundle == null || awareProcessBlockInfo == null || list == null) {
            int i = exeTime;
            AwareLog.e(TAG, "insertDumpAndStatisticData: null procGroups");
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
        StringBuilder operationSB = new StringBuilder("QuickKill [");
        operationSB.append(awareProcessBlockInfo.mUid);
        operationSB.append(",");
        for (AwareProcessInfo info : procs) {
            if (!(info.mProcInfo == null || info.mProcInfo.mPackageName == null)) {
                operationSB.append(info.mProcInfo.mPackageName);
                operationSB.append(",");
            }
            bundle = extras;
            awareProcessBlockInfo = procGroup;
        }
        operationSB.append(list);
        operationSB.append("]");
        EventTracker.getInstance().insertDumpData(beginTime, operationSB.toString(), exeTime, reasonSB.toString());
        EventTracker.getInstance().insertStatisticData("QuickKill", exeTime, effect);
    }
}
