package com.android.server.rms.iaware.memory.action;

import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.mtm.iaware.appmng.AwareAppMngSortPolicy;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.net.HwNetworkStatsService;
import com.android.server.rms.iaware.hiber.AppHibernateTask;
import com.android.server.rms.iaware.hiber.bean.HiberAppInfo;
import com.android.server.rms.iaware.memory.utils.EventTracker;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import java.util.ArrayList;
import java.util.List;

public class ReclaimAction extends Action {
    private static final int BASE_RECLAIM_GAP = 2000;
    private static final int DEFAULT_RECLAIM_RATE = 30;
    private static final int MAX_RECLAIM_CNT = 3;
    private static final int MAX_RECLAIM_GAP = 1800000;
    private static final int MAX_RECLAIM_TIME = 1000;
    private static final int MIN_RECLAIM_UID = 10000;
    private static final String TAG = "AwareMem_Reclaim";
    private int mEmptyLoopCount = 0;
    private long mLastReclaimTime = 0;
    private long mReclaimGap = 2000;

    private static class ReclaimState {
        public long mBeginTime;
        public String mReasonCommon;
        public int mReclaimedProc;
        public long mRequestMemory;

        ReclaimState(Bundle extras) {
            String appName = extras.getString("appName");
            if (appName != null && appName.length() > 64) {
                appName = appName.substring(0, 63);
            }
            int event = extras.getInt("event");
            long timeStamp = extras.getLong("timeStamp");
            int cpuLoad = extras.getInt("cpuLoad");
            boolean cpuBusy = extras.getBoolean("cpuBusy");
            StringBuffer buffer = new StringBuffer(128);
            buffer.append("[");
            buffer.append(appName);
            buffer.append(",");
            buffer.append(event);
            buffer.append(",");
            buffer.append(timeStamp);
            buffer.append("],[");
            buffer.append(cpuLoad);
            buffer.append(",");
            buffer.append(cpuBusy);
            buffer.append("],[");
            this.mReasonCommon = buffer.toString();
            this.mRequestMemory = extras.getLong("reqMem");
            this.mReclaimedProc = 0;
            this.mBeginTime = System.currentTimeMillis();
        }
    }

    public ReclaimAction(Context context) {
        super(context);
    }

    public boolean reqInterrupt(Bundle extras) {
        this.mInterrupt.set(true);
        return true;
    }

    private List<AwareProcessInfo> generateCompressList(List<AwareProcessBlockInfo> procsGroups) {
        List<AwareProcessInfo> list = null;
        if (procsGroups == null || procsGroups.isEmpty()) {
            AwareLog.w(TAG, "generateCompressList procsGroups error!");
            return null;
        }
        List<AwareProcessInfo> compressList = new ArrayList();
        ArrayMap<Integer, HiberAppInfo> historyMap = AppHibernateTask.getInstance().getRelaimedRecord();
        for (AwareProcessBlockInfo blockInfo : procsGroups) {
            if (blockInfo != null) {
                if (blockInfo.mUid >= 10000) {
                    List<AwareProcessInfo> processList = blockInfo.getProcessList();
                    if (processList != null) {
                        for (AwareProcessInfo proc : processList) {
                            if (!(proc == null || proc.mProcInfo == null)) {
                                if (Process.myPid() != proc.mPid) {
                                    ProcessInfo currentProcInfo = proc.mProcInfo;
                                    if (currentProcInfo.mProcessName != null) {
                                        if (!currentProcInfo.mProcessName.contains("launcher")) {
                                            if (currentProcInfo.mPackageName != null) {
                                                if (1 == currentProcInfo.mPackageName.size()) {
                                                    HiberAppInfo historyHiberAppInfo = historyMap == null ? null : (HiberAppInfo) historyMap.get(Integer.valueOf(currentProcInfo.mPid));
                                                    if (historyHiberAppInfo == null || currentProcInfo.mUid != historyHiberAppInfo.mUid || !currentProcInfo.mProcessName.equals(historyHiberAppInfo.mProcessName) || SystemClock.uptimeMillis() - historyHiberAppInfo.mReclaimTime >= AwareAppMngSort.PREVIOUS_APP_DIRCACTIVITY_DECAYTIME) {
                                                        compressList.add(proc);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!compressList.isEmpty()) {
            list = compressList;
        }
        return list;
    }

    public int execute(Bundle extras) {
        if (extras == null) {
            AwareLog.w(TAG, "null extras!");
            return -1;
        }
        ReclaimState state = new ReclaimState(extras);
        AwareAppMngSortPolicy policy = MemoryUtils.getAppMngSortPolicy(1, 3);
        if (policy == null) {
            AwareLog.w(TAG, "getAppMngSortPolicy null policy!");
            return -1;
        }
        if (3 == reclaimProcessGroup(policy, 2, state)) {
            reclaimProcessGroup(policy, 1, state);
        }
        if (this.mInterrupt.get()) {
            return 0;
        }
        updateReclaimGap(state.mReclaimedProc);
        this.mLastReclaimTime = SystemClock.elapsedRealtime();
        this.mEmptyLoopCount = state.mReclaimedProc == 0 ? this.mEmptyLoopCount + 1 : 0;
        return 0;
    }

    private int reclaimProcessGroup(AwareAppMngSortPolicy policy, int groupId, ReclaimState state) {
        String str;
        ReclaimAction reclaimAction = this;
        ReclaimState reclaimState = state;
        int ret = 0;
        List<AwareProcessBlockInfo> procsGroups = MemoryUtils.getAppMngProcGroup(policy, groupId);
        List<AwareProcessInfo> compressList = reclaimAction.generateCompressList(procsGroups);
        List<AwareProcessInfo> list;
        List<AwareProcessBlockInfo> list2;
        if (compressList == null) {
            list = compressList;
        } else if (compressList.isEmpty()) {
            list2 = procsGroups;
            list = compressList;
        } else {
            int ret2;
            for (AwareProcessInfo proc : compressList) {
                if (reclaimAction.mInterrupt.get()) {
                    AwareLog.d(TAG, "Interrupted, return");
                    return -1;
                }
                if (proc == null) {
                    ret2 = ret;
                    list2 = procsGroups;
                    list = compressList;
                } else if (proc.mProcInfo == null) {
                    ret2 = ret;
                    list2 = procsGroups;
                    list = compressList;
                } else {
                    String procName = proc.mProcInfo.mProcessName;
                    long uss = MemoryReader.getPssForPid(proc.mPid);
                    if (uss <= 0) {
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        ret2 = ret;
                        stringBuilder.append("getPssForPid error skip! procName=");
                        stringBuilder.append(procName);
                        AwareLog.w(str2, stringBuilder.toString());
                        ret = ret2;
                    } else {
                        long beginTime = System.currentTimeMillis();
                        ret = AppHibernateTask.getInstance().reclaimApp(proc);
                        long endTime = System.currentTimeMillis();
                        int ret3;
                        if (ret < 0) {
                            str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            ret3 = ret;
                            stringBuilder2.append("call hiber reclaimApp error skip! procName=");
                            stringBuilder2.append(procName);
                            AwareLog.d(str, stringBuilder2.toString());
                            ret = ret3;
                        } else {
                            ret3 = ret;
                            str = new StringBuilder();
                            str.append("Reclaim [");
                            str.append(proc.mPid);
                            str.append(",");
                            str.append(procName);
                            str.append("]");
                            str = str.toString();
                            long effect = (70 * uss) / 100;
                            ret = new StringBuilder();
                            ret.append(reclaimState.mReasonCommon);
                            list2 = procsGroups;
                            ret.append(reclaimState.mRequestMemory / 1024);
                            ret.append(",");
                            ret.append(effect / 1024);
                            ret.append("]");
                            ret = ret.toString();
                            int exeTime = (int) (endTime - beginTime);
                            EventTracker.getInstance().insertDumpData(beginTime, str, exeTime, ret);
                            list = compressList;
                            EventTracker.getInstance().insertStatisticData("Reclaim", exeTime, (int) (effect / 1024));
                            reclaimState.mReclaimedProc++;
                            reclaimState.mRequestMemory -= effect;
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("reclaimed ");
                            stringBuilder3.append(procName);
                            stringBuilder3.append("(");
                            stringBuilder3.append(proc.mPid);
                            stringBuilder3.append(") get ");
                            stringBuilder3.append(effect);
                            stringBuilder3.append(" kb memory");
                            AwareLog.d(str3, stringBuilder3.toString());
                            if (1000 <= endTime - reclaimState.mBeginTime || reclaimState.mRequestMemory <= 0 || 3 <= reclaimState.mReclaimedProc) {
                                return 0;
                            }
                            String str4 = str;
                            Object obj = ret;
                            ret = ret3;
                            procsGroups = list2;
                            compressList = list;
                        }
                    }
                    reclaimAction = this;
                }
                AwareLog.w(TAG, "proc error skip!");
                ret = ret2;
                procsGroups = list2;
                compressList = list;
                reclaimAction = this;
            }
            ret2 = ret;
            list2 = procsGroups;
            list = compressList;
            return 3;
        }
        str = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("reclaim process group ");
        stringBuilder4.append(groupId);
        stringBuilder4.append(" err!");
        AwareLog.i(str, stringBuilder4.toString());
        return 3;
    }

    public void reset() {
        this.mLastReclaimTime = 0;
        this.mEmptyLoopCount = 0;
        this.mReclaimGap = 2000;
    }

    public boolean canBeExecuted() {
        if (!AppHibernateTask.getInstance().isAppHiberEnabled()) {
            AwareLog.i(TAG, "canBeExecuted hibernation is not running!");
            this.mReclaimGap = HwNetworkStatsService.UPLOAD_INTERVAL;
            return false;
        } else if (SystemClock.elapsedRealtime() - this.mLastReclaimTime < this.mReclaimGap) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("canBeExecuted waiting next operation, interval=");
            stringBuilder.append(this.mReclaimGap);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        } else if (MemoryReader.isZramOK()) {
            return true;
        } else {
            AwareLog.i(TAG, "canBeExecuted no zram space!");
            updateReclaimGap(0);
            return false;
        }
    }

    public int getLastExecFailCount() {
        return this.mEmptyLoopCount;
    }

    private void updateReclaimGap(int nReclaimed) {
        long j;
        long j2;
        if (nReclaimed == 0) {
            this.mReclaimGap *= 2;
            j = this.mReclaimGap;
            j2 = HwNetworkStatsService.UPLOAD_INTERVAL;
            if (j <= HwNetworkStatsService.UPLOAD_INTERVAL) {
                j2 = this.mReclaimGap;
            }
            this.mReclaimGap = j2;
        } else {
            j = MemoryReader.getInstance().getMemAvailable();
            j2 = 0;
            if (j > 0) {
                long maxStep = MemoryConstant.getIdleMemory() - MemoryConstant.getCriticalMemory();
                if (maxStep <= 0) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Idle <= Emergency Memory! getIdleMemory=");
                    stringBuilder.append(MemoryConstant.getIdleMemory());
                    stringBuilder.append(",getEmergencyMemory=");
                    stringBuilder.append(MemoryConstant.getEmergencyMemory());
                    AwareLog.w(str, stringBuilder.toString());
                    return;
                }
                long interval = (8000 * (j - MemoryConstant.getCriticalMemory())) / maxStep;
                if (interval >= 0) {
                    j2 = interval;
                }
                this.mReclaimGap = 2000 + j2;
            } else {
                this.mReclaimGap = 2000;
            }
        }
    }
}
