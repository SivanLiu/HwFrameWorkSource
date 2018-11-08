package com.android.server.mtm.iaware.appmng.appclean;

import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import android.app.mtm.iaware.appmng.AppMngConstant.CleanReason;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.rule.ListItem;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.AppMngTag;
import com.android.server.mtm.taskstatus.ProcessCleaner.CleanType;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.rms.iaware.srms.AppCleanupDumpRadar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class CleanSource {
    private static final Comparator<AwareProcessBlockInfo> BLOCK_BY_PACKAGE = new Comparator<AwareProcessBlockInfo>() {
        public int compare(AwareProcessBlockInfo arg0, AwareProcessBlockInfo arg1) {
            int i = -1;
            if (arg0 == null) {
                return arg1 == null ? 0 : -1;
            } else {
                if (arg1 == null) {
                    return 1;
                }
                if (arg0.mPackageName == null) {
                    return arg1.mPackageName == null ? 0 : -1;
                } else {
                    if (arg1.mPackageName == null) {
                        return 1;
                    }
                    if (!arg1.mPackageName.equals(arg0.mPackageName)) {
                        return arg0.mPackageName.compareTo(arg1.mPackageName);
                    }
                    if (arg0.mUid == arg1.mUid) {
                        return 0;
                    }
                    if (arg0.mUid >= arg1.mUid) {
                        i = 1;
                    }
                    return i;
                }
            }
        }
    };
    protected static final int FAKE_ADJ = 10001;
    protected static final int FAKE_PID = 99999;
    protected static final int FAKE_UID = 99999;
    private static final String TAG = "CleanSource";

    public void clean() {
    }

    public static List<AwareProcessBlockInfo> mergeBlockForMemory(List<AwareProcessBlockInfo> rawBlock, HashMap<String, ListItem> processList) {
        return mergeBlockInternal(rawBlock, true, null, processList);
    }

    public static List<AwareProcessBlockInfo> mergeBlock(List<AwareProcessBlockInfo> rawBlock) {
        return mergeBlockInternal(rawBlock, false, null, null);
    }

    public static List<AwareProcessBlockInfo> mergeBlock(List<AwareProcessBlockInfo> rawBlock, List<CleanType> priority) {
        return mergeBlockInternal(rawBlock, false, priority, null);
    }

    private static void setProperFlag(AwareProcessBlockInfo curBlock, boolean forMemory) {
        if (CleanType.FORCESTOP.equals(curBlock.mCleanType) || CleanType.FORCESTOP_REMOVETASK.equals(curBlock.mCleanType)) {
            curBlock.mResCleanAllow = true;
        } else if (CleanType.FORCESTOP_ALARM.equals(curBlock.mCleanType)) {
            curBlock.mResCleanAllow = true;
            curBlock.mCleanAlarm = true;
            curBlock.mIsNativeForceStop = false;
        } else if (forMemory && CleanType.KILL_ALLOW_START.equals(curBlock.mCleanType) && curBlock.mProcessList != null) {
            curBlock.mResCleanAllow = false;
            curBlock.mIsNativeForceStop = false;
            for (AwareProcessInfo procInfo : curBlock.mProcessList) {
                if (procInfo != null) {
                    procInfo.mRestartFlag = true;
                }
            }
        }
    }

    private static List<AwareProcessBlockInfo> mergeBlockInternal(List<AwareProcessBlockInfo> rawBlock, boolean forMemory, List<CleanType> priority, HashMap<String, ListItem> processList) {
        if (rawBlock == null || rawBlock.isEmpty()) {
            return rawBlock;
        }
        Collections.sort(rawBlock, BLOCK_BY_PACKAGE);
        List<AwareProcessBlockInfo> cleanBlock = new ArrayList();
        List<AwareProcessBlockInfo> protectedBlock = new ArrayList();
        List<AwareProcessBlockInfo> tempBlockInfoList = new ArrayList();
        AwareProcessBlockInfo lastBlock = null;
        Iterator<AwareProcessBlockInfo> iterator = rawBlock.iterator();
        while (iterator.hasNext()) {
            AwareProcessBlockInfo curBlock = (AwareProcessBlockInfo) iterator.next();
            iterator.remove();
            if (curBlock == null) {
                AwareLog.e(TAG, "bad decide result curBlock == null");
                return null;
            }
            setProperFlag(curBlock, forMemory);
            if (lastBlock == null) {
                if (!CleanType.NONE.equals(curBlock.mCleanType)) {
                    checkProcInProcesslist(curBlock, processList, tempBlockInfoList);
                }
                lastBlock = curBlock;
            } else if (lastBlock.mPackageName != null && lastBlock.mPackageName.equals(curBlock.mPackageName) && UserHandle.getUserId(curBlock.mUid) == UserHandle.getUserId(lastBlock.mUid)) {
                if (!(CleanType.NONE.equals(lastBlock.mCleanType) || (CleanType.NONE.equals(curBlock.mCleanType) ^ 1) == 0)) {
                    checkProcInProcesslist(curBlock, processList, tempBlockInfoList);
                }
                mergeBlockInfo(lastBlock, curBlock, priority);
            } else {
                handleBlockWithProcessList(lastBlock, cleanBlock, protectedBlock, tempBlockInfoList, processList, priority, forMemory);
                mergeBlockToList(lastBlock, cleanBlock, protectedBlock, forMemory);
                tempBlockInfoList.clear();
                if (!CleanType.NONE.equals(curBlock.mCleanType)) {
                    checkProcInProcesslist(curBlock, processList, tempBlockInfoList);
                }
                lastBlock = curBlock;
            }
        }
        handleBlockWithProcessList(lastBlock, cleanBlock, protectedBlock, tempBlockInfoList, processList, priority, forMemory);
        mergeBlockToList(lastBlock, cleanBlock, protectedBlock, forMemory);
        rawBlock.addAll(protectedBlock);
        return cleanBlock;
    }

    private static void checkProcInProcesslist(AwareProcessBlockInfo block, HashMap<String, ListItem> processList, List<AwareProcessBlockInfo> blockInfoList) {
        if (block != null && block.mProcessList != null && processList != null && blockInfoList != null) {
            Iterator<AwareProcessInfo> iterator = block.mProcessList.iterator();
            while (iterator.hasNext()) {
                AwareProcessInfo procInfo = (AwareProcessInfo) iterator.next();
                if (procInfo == null || procInfo.mProcInfo == null || procInfo.mProcInfo.mProcessName == null) {
                    iterator.remove();
                } else {
                    ListItem item = (ListItem) processList.get(procInfo.mProcInfo.mProcessName);
                    if (item != null && item.getPolicy() < block.mCleanType.ordinal()) {
                        blockInfoList.add(new AwareProcessBlockInfo(block.mReason, block.mUid, procInfo, block.mCleanType.ordinal(), block.mDetailedReason));
                        iterator.remove();
                    }
                }
            }
            if (block.mProcessList.isEmpty()) {
                block.mProcessList = null;
            }
        }
    }

    private static void handleBlockWithProcessList(AwareProcessBlockInfo lastBlock, List<AwareProcessBlockInfo> cleanBlock, List<AwareProcessBlockInfo> protectedBlock, List<AwareProcessBlockInfo> blockInfoList, HashMap<String, ListItem> processList, List<CleanType> priority, boolean forMemory) {
        if (lastBlock != null && blockInfoList != null && cleanBlock != null && processList != null && protectedBlock != null) {
            if (CleanType.NONE.equals(lastBlock.mCleanType)) {
                for (AwareProcessBlockInfo block : blockInfoList) {
                    mergeBlockInfo(lastBlock, block, priority);
                }
                return;
            }
            boolean isChanged = mergeBlockWithProcessList(lastBlock, cleanBlock, protectedBlock, blockInfoList, processList, priority, forMemory);
            if (lastBlock.mProcessList != null) {
                if (isChanged && lastBlock.mCleanType.ordinal() > CleanType.KILL_ALLOW_START.ordinal()) {
                    String newReason = AppMngTag.POLICY.getDesc().toUpperCase() + ":" + CleanType.KILL_ALLOW_START.ordinal();
                    lastBlock.mCleanType = CleanType.KILL_ALLOW_START;
                    lastBlock.mReason = lastBlock.mReason.replaceFirst(AppMngTag.POLICY.getDesc().toUpperCase() + ":\\d+", newReason) + "," + CleanReason.POLICY_DEGRADE.getCode();
                    lastBlock.mDetailedReason.put(AppMngTag.POLICY.getDesc(), Integer.valueOf(CleanType.KILL_ALLOW_START.ordinal()));
                    setProperFlag(lastBlock, forMemory);
                    AwareLog.d(TAG, lastBlock.toString());
                }
            }
        }
    }

    private static boolean mergeBlockWithProcessList(AwareProcessBlockInfo lastBlock, List<AwareProcessBlockInfo> cleanBlock, List<AwareProcessBlockInfo> protectedBlock, List<AwareProcessBlockInfo> blockInfoList, HashMap<String, ListItem> processList, List<CleanType> priority, boolean forMemory) {
        boolean isChanged = false;
        Iterator<AwareProcessBlockInfo> iterator = blockInfoList.iterator();
        while (iterator.hasNext()) {
            AwareProcessBlockInfo block = (AwareProcessBlockInfo) iterator.next();
            AwareProcessInfo procInfo = (AwareProcessInfo) block.mProcessList.get(0);
            ListItem item = (ListItem) processList.get(procInfo.mProcInfo.mProcessName);
            if (item == null || (lastBlock.mProcessList != null && item.getPolicy() >= lastBlock.mCleanType.ordinal())) {
                mergeBlockInfo(lastBlock, block, priority);
            } else {
                isChanged = true;
                HashMap<String, Integer> detailedReason = new HashMap();
                detailedReason.put(AppMngTag.POLICY.getDesc(), Integer.valueOf(item.getPolicy()));
                detailedReason.put("spec", Integer.valueOf(CleanReason.PROCESSLIST.ordinal()));
                AwareProcessBlockInfo tempBlock = new AwareProcessBlockInfo(CleanReason.PROCESSLIST.getCode(), procInfo.mProcInfo.mUid, procInfo, item.getPolicy(), detailedReason);
                tempBlock.mPackageName = procInfo.mProcInfo.mProcessName;
                setProperFlag(tempBlock, forMemory);
                mergeBlockToList(tempBlock, cleanBlock, protectedBlock, forMemory);
                AwareLog.d(TAG, tempBlock.toString());
            }
            iterator.remove();
        }
        return isChanged;
    }

    private static void mergeBlockToList(AwareProcessBlockInfo procBlock, List<AwareProcessBlockInfo> cleanBlock, List<AwareProcessBlockInfo> protectedBlock, boolean forMemory) {
        if (procBlock != null && cleanBlock != null && protectedBlock != null) {
            if (forMemory && (CleanType.NONE.equals(procBlock.mCleanType) ^ 1) == 0) {
                protectedBlock.add(procBlock);
            } else {
                procBlock.mUpdateTime = SystemClock.elapsedRealtime();
                cleanBlock.add(procBlock);
            }
        }
    }

    private static void mergeBlockInfo(AwareProcessBlockInfo lastBlock, AwareProcessBlockInfo curBlock, List<CleanType> priority) {
        if (curBlock.mProcessList != null) {
            if (lastBlock.mProcessList == null) {
                lastBlock.mProcessList = new ArrayList();
            }
            if (curBlock.mProcessList != null) {
                lastBlock.mProcessList.addAll(curBlock.mProcessList);
                if (lastBlock.mMinAdj > curBlock.mMinAdj) {
                    lastBlock.mMinAdj = curBlock.mMinAdj;
                }
            }
            if (curBlock.mCleanType == null) {
                curBlock.mCleanType = CleanType.NONE;
            }
            if (lastBlock.mCleanType == null) {
                lastBlock.mCleanType = CleanType.NONE;
            }
            if (!lastBlock.mCleanType.equals(curBlock.mCleanType)) {
                if (priority != null) {
                    if (priority.indexOf(lastBlock.mCleanType) < priority.indexOf(curBlock.mCleanType)) {
                        lastBlock.mCleanType = curBlock.mCleanType;
                        lastBlock.mResCleanAllow = curBlock.mResCleanAllow;
                        lastBlock.mReason = curBlock.mReason;
                        lastBlock.mDetailedReason = curBlock.mDetailedReason;
                    }
                } else if (lastBlock.mCleanType.ordinal() > curBlock.mCleanType.ordinal()) {
                    lastBlock.mCleanType = curBlock.mCleanType;
                    lastBlock.mResCleanAllow = curBlock.mResCleanAllow;
                    lastBlock.mReason = curBlock.mReason;
                    lastBlock.mDetailedReason = curBlock.mDetailedReason;
                }
            }
        }
    }

    protected static AwareProcessInfo getDeadAwareProcInfo(String packageName, int userId) {
        int packageUid = 99999;
        try {
            IPackageManager pm = Stub.asInterface(ServiceManager.getService("package"));
            if (pm != null) {
                packageUid = pm.getPackageUid(packageName, 8192, userId);
            }
        } catch (RemoteException e) {
            AwareLog.e(TAG, "Failed to get PackageManagerService!");
        }
        ProcessInfo fakeProcInfo = new ProcessInfo(99999, packageUid);
        fakeProcInfo.mCurAdj = 10001;
        fakeProcInfo.mPackageName.add(packageName);
        return new AwareProcessInfo(99999, fakeProcInfo);
    }

    protected void uploadToBigData(AppCleanSource source, AwareProcessBlockInfo info) {
        if (info != null) {
            AppCleanupDumpRadar.getInstance().updateCleanData(info.mPackageName, source, info.mDetailedReason);
        }
    }

    public void updateHistory(AppCleanSource source, AwareProcessBlockInfo info) {
        if (info != null) {
            StringBuilder history = new StringBuilder();
            history.append("pkg = ");
            history.append(info.mPackageName);
            history.append(", uid = ");
            history.append(info.mUid);
            history.append(", reason = ");
            history.append(info.mReason);
            history.append(", type = ");
            history.append(info.mCleanType);
            DecisionMaker.getInstance().updateHistory(source, history.toString());
        }
    }
}
