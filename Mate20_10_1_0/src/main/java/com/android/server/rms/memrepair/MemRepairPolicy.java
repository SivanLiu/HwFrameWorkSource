package com.android.server.rms.memrepair;

import android.os.Debug;
import android.rms.iaware.AwareLog;
import android.rms.iaware.memrepair.MemRepairPkgInfo;
import android.rms.iaware.memrepair.MemRepairProcInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.internal.util.MemInfoReader;
import com.android.server.location.HwLogRecordManager;
import com.android.server.mtm.iaware.appmng.AwareAppMngSortPolicy;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.taskstatus.ProcessCleaner;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.utils.SparseSet;
import com.android.server.pm.auth.DevicePublicKeyLoader;
import com.android.server.rms.collector.ResourceCollector;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.android.server.rms.memrepair.MemRepairAlgorithm;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MemRepairPolicy {
    public static final int DEFAULT_DVALUE_FLOAT_PERCENT = 5;
    private static final long ION_REPAIR_INTERVAL = 3600000;
    private static final long KB2MB_FACTOR = 1024;
    public static final int MAX_DVALUE_FLOAT_PERCENT = 30;
    public static final int MAX_MEM_THRES_COUNT = 20;
    public static final int MAX_PROCESS_THRES_COUNT = 1;
    public static final int MEM_THRES_ITEM_COUNT = 3;
    public static final int MEM_THRES_MAX_FLOAT_IDX = 1;
    public static final int MEM_THRES_MIN_FLOAT_IDX = 0;
    public static final int MEM_THRES_PERCENTAGE_IDX = 2;
    public static final int MIN_DVALUE_FLOAT_PERCENT = 1;
    private static final int NORMALIZATION_THREASHOLD = 5120;
    public static final int PROCESS_THRES_BG_IDX = 1;
    public static final int PROCESS_THRES_EMERG = 0;
    public static final int PROCESS_THRES_FG_IDX = 0;
    public static final int PROCESS_THRES_ITEM_COUNT = 2;
    public static final int SCENE_TYPE_LOW_MEMORY = 2;
    public static final int SCENE_TYPE_MIDNIGHT = 1;
    public static final int SCENE_TYPE_SCREENOFF = 0;
    private static final String TAG = "AwareMem_MRPolicy";
    public static final int TYPE_EMERG_BG_THRESHOLD = 4;
    public static final int TYPE_EMERG_FG_THRESHOLD = 2;
    public static final int TYPE_EMERG_ION_THRESHOLD = 32;
    public static final int TYPE_EMERG_TOTAL_PSS_THRESHOLD = 16;
    public static final int TYPE_FOREGROUND = 8;
    public static final int TYPE_MEM_GROW_UP = 1;
    public static final int TYPE_NONE = 0;
    private static final long VSS_DECENT_PERCENT = 1;
    private static final long VSS_INCREASE_SIZE = 10;
    public static final int VSS_INTERVAL_SIZE = 3;
    private static final long VSS_MAX_THRESHOLD = 4096;
    private static final long VSS_NORMALIZATION_SIZE = 50;
    public static final int VSS_PARAMETER_SIZE = 5;
    private static final int VSS_TYPE_DROP = 1;
    private static final int VSS_TYPE_NO_TREND = 4;
    private static final int VSS_TYPE_PERCENT = 2;
    private static final int VSS_TYPE_THRESHOLD = 5;
    private static final int VSS_TYPE_TREND = 3;
    private static MemRepairPolicy sMemRepairPolicy;
    private AlgoCallback mAlgoCallback = new AlgoCallback();
    private long[][] mDefaultProcThresHolds = null;
    private int mDvalueFloatPercent = 5;
    private long[][] mFloatThresHolds = null;
    private final Object mIonLock = new Object();
    private long mIonRepairLastTime = 0;
    private long mIonThreshold = 0;
    private boolean mIsMemRepairIonFeature = false;
    private long[][] mLowMemProcThresHolds = null;
    private int mMaxCollectCount = 50;
    private int mMinCollectCount = 6;
    private long[][] mProcThresHolds = null;
    private long mVssInitPercent = 0;
    private long mVssInitStepValue = 0;
    private long[] mVssIntervals = null;
    private long mVssThreshold = 0;

    private MemRepairPolicy() {
    }

    public static MemRepairPolicy getInstance() {
        MemRepairPolicy memRepairPolicy;
        synchronized (MemRepairPolicy.class) {
            if (sMemRepairPolicy == null) {
                sMemRepairPolicy = new MemRepairPolicy();
            }
            memRepairPolicy = sMemRepairPolicy;
        }
        return memRepairPolicy;
    }

    public void updateCollectCount(int fgCollectCount, int bgCollectCount) {
        AwareLog.d(TAG, "updateCollectCount fgCount=" + fgCollectCount + ",bgCount=" + bgCollectCount);
        int minCount = fgCollectCount < bgCollectCount ? bgCollectCount : fgCollectCount;
        int i = 6;
        if (minCount >= 6) {
            i = minCount;
        }
        this.mMinCollectCount = i;
        this.mMaxCollectCount = this.mMinCollectCount * 2;
        int i2 = this.mMaxCollectCount;
        if (i2 >= 100) {
            i2 = 100;
        }
        this.mMaxCollectCount = i2;
    }

    public void updateDvalueFloatPercent(int percent) {
        if (percent < 1 || percent > 30) {
            AwareLog.w(TAG, "updateDvalueFloatPercent percent=" + percent);
            return;
        }
        this.mDvalueFloatPercent = percent;
        AwareLog.d(TAG, "updateDvalueFloatPercent=" + percent);
    }

    public void updateFloatThresHold(long[][] floatThresHolds) {
        if (floatThresHolds == null || floatThresHolds.length < 1 || floatThresHolds.length > 20) {
            AwareLog.w(TAG, "updateFloatThresHold error params");
        } else if (floatThresHolds[0] == null || floatThresHolds[0].length != 3) {
            AwareLog.w(TAG, "updateFloatThresHold error params");
        } else {
            this.mFloatThresHolds = (long[][]) Array.newInstance(long.class, floatThresHolds.length, floatThresHolds[0].length);
            for (int i = 0; i < floatThresHolds.length; i++) {
                if (floatThresHolds[i] == null || floatThresHolds[i].length != 3) {
                    AwareLog.w(TAG, "updateFloatThresHold error params=" + Arrays.toString(floatThresHolds[i]));
                    this.mFloatThresHolds = null;
                    return;
                }
                System.arraycopy(floatThresHolds[i], 0, this.mFloatThresHolds[i], 0, floatThresHolds[0].length);
                AwareLog.d(TAG, "updateFloatThresHold thresHolds=" + Arrays.toString(this.mFloatThresHolds[i]));
            }
        }
    }

    public void updateProcThresHold(long[][] procThresHolds, boolean isCust) {
        AwareLog.d(TAG, "enter updateProcThresHold...isCust=" + isCust);
        if (procThresHolds == null || procThresHolds.length != 1 || procThresHolds[0] == null || procThresHolds[0].length != 2) {
            AwareLog.w(TAG, "updateProcThresHold error params");
        } else if (isCust) {
            this.mLowMemProcThresHolds = (long[][]) Array.newInstance(long.class, procThresHolds.length, procThresHolds[0].length);
            updateThresHolds(procThresHolds, this.mLowMemProcThresHolds);
        } else {
            this.mDefaultProcThresHolds = (long[][]) Array.newInstance(long.class, procThresHolds.length, procThresHolds[0].length);
            updateThresHolds(procThresHolds, this.mDefaultProcThresHolds);
            this.mProcThresHolds = this.mDefaultProcThresHolds;
        }
    }

    private void updateThresHolds(long[][] procThresHolds, long[][] need2ProcThresHolds) {
        for (int i = 0; i < procThresHolds.length; i++) {
            if (procThresHolds[i] == null || procThresHolds[i].length != 2) {
                AwareLog.w(TAG, "updateProcThresHold error params=" + Arrays.toString(procThresHolds[i]));
                return;
            }
            System.arraycopy(procThresHolds[i], 0, need2ProcThresHolds[i], 0, procThresHolds[0].length);
            AwareLog.d(TAG, "updateProcThresHold thresHolds=" + Arrays.toString(need2ProcThresHolds[i]));
        }
    }

    private void changeValue(boolean isLowMem) {
        AwareLog.d(TAG, "isLowMem=" + isLowMem);
        long[][] jArr = this.mLowMemProcThresHolds;
        if (jArr != null && isLowMem) {
            this.mProcThresHolds = jArr;
            AwareLog.d(TAG, "updateProcThresHold thresHolds=" + Arrays.toString(this.mProcThresHolds[0]));
        }
        long[][] jArr2 = this.mDefaultProcThresHolds;
        if (jArr2 != null && !isLowMem) {
            this.mProcThresHolds = jArr2;
            AwareLog.d(TAG, "updateProcThresHold thresHolds=" + Arrays.toString(this.mProcThresHolds[0]));
        }
    }

    public List<MemRepairPkgInfo> getMemRepairPolicy(int sceneType) {
        if (sceneType == 1) {
            SystemAppMemRepairMng.getInstance().reportData(20027);
        }
        if (sceneType != 0 && sceneType != 1 && sceneType != 2) {
            AwareLog.i(TAG, "getMemRepairPolicy invalid param=" + sceneType);
            return null;
        } else if (sceneType == 2 && this.mLowMemProcThresHolds == null) {
            AwareLog.i(TAG, "mLowMemProcThresHolds is null , sceneType=" + sceneType);
            return null;
        } else {
            long availableRam = MemoryReader.getInstance().getMemAvailable();
            if (availableRam <= 0) {
                AwareLog.e(TAG, "execute faild to read availableRam =" + availableRam);
                return null;
            }
            if (availableRam <= MemoryConstant.getCriticalMemory()) {
                changeValue(true);
            } else {
                changeValue(false);
            }
            if (this.mProcThresHolds != null && this.mFloatThresHolds != null) {
                return mergeIonRepair(mergePssAndVss(sceneType), sceneType);
            }
            AwareLog.i(TAG, "getMemRepairPolicy null thresholds!");
            return mergeIonRepair(null, sceneType);
        }
    }

    private ArrayMap<String, Integer> getIonPidRepair() {
        int maxIonPid = -1;
        ArrayMap<String, Integer> ionRepairMap = new ArrayMap<>();
        try {
            String ionString = ResourceCollector.getIonInfo();
            if (ionString == null) {
                AwareLog.d(TAG, "getIonPidRepair the ion info is null");
                return ionRepairMap;
            }
            String[] ionStrings = ionString.split(System.getProperty("line.separator"));
            int maxIon = 0;
            int len = ionStrings.length;
            for (int j = 1; j < len; j++) {
                String[] ionPidSize = ionStrings[j].trim().replaceAll(" +", " ").split(" ");
                if (ionPidSize.length != 2) {
                    return ionRepairMap;
                }
                int pid = Integer.parseInt(ionPidSize[0]);
                int ionValue = Integer.parseInt(ionPidSize[1]);
                if (maxIon < ionValue) {
                    maxIon = ionValue;
                    maxIonPid = pid;
                }
            }
            ionRepairMap.put(DevicePublicKeyLoader.KEY, Integer.valueOf(maxIonPid));
            ionRepairMap.put("value", Integer.valueOf(maxIon));
            return ionRepairMap;
        } catch (NumberFormatException exception) {
            AwareLog.w(TAG, "get the ion info error " + exception.getStackTrace());
        }
    }

    private List<MemRepairPkgInfo> mergeIonRepair(List<MemRepairPkgInfo> memRepairPkgInfos, int sceneType) {
        MemRepairPkgInfo memRepairPkgInfoByIon;
        if (!checkIonRepair(sceneType)) {
            return memRepairPkgInfos;
        }
        ArrayMap<String, Integer> ionRepairMap = getIonPidRepair();
        Integer ionPid = ionRepairMap.get(DevicePublicKeyLoader.KEY);
        if (ionPid == null || ionPid.intValue() == -1) {
            AwareLog.d(TAG, "mergeIonRepair can't find the ion pid");
            return memRepairPkgInfos;
        }
        AwareLog.d(TAG, "mergeIonRepair the max ion pid =" + ionPid);
        List<MemRepairPkgInfo> memRepairPkgInfosTemp = memRepairPkgInfos;
        if (memRepairPkgInfosTemp == null) {
            memRepairPkgInfosTemp = new ArrayList<>();
        }
        if (checkIonProcInVssOrPssRepair(memRepairPkgInfosTemp, ionPid.intValue())) {
            AwareLog.d(TAG, "mergeIonRepair vss or pss memRepair has the same pid");
            return memRepairPkgInfosTemp;
        }
        AwareAppMngSortPolicy policy = MemoryUtils.getAppMngSortPolicyForMemRepair(sceneType);
        if (policy == null) {
            AwareLog.d(TAG, "mergeIonRepair null policy!");
            return memRepairPkgInfosTemp;
        }
        List<AwareProcessBlockInfo> awareProcessBlockInfos = MemoryUtils.getAppMngProcGroup(policy, 2);
        if (!(awareProcessBlockInfos == null || (memRepairPkgInfoByIon = getMemRepairPkgInfoByIon(ionRepairMap, awareProcessBlockInfos, sceneType)) == null)) {
            AwareLog.i(TAG, "mergeIonRepair memRepairPkgInfoByIon=" + memRepairPkgInfoByIon.toString());
            memRepairPkgInfosTemp.add(memRepairPkgInfoByIon);
        }
        return memRepairPkgInfosTemp;
    }

    private boolean checkIonProcInVssOrPssRepair(List<MemRepairPkgInfo> memRepairPkgInfosTemp, int ionPidRepair) {
        for (MemRepairPkgInfo memRepairPkgInfo : memRepairPkgInfosTemp) {
            if (memRepairPkgInfo != null) {
                for (MemRepairProcInfo memRepairProcInfo : memRepairPkgInfo.getProcessList()) {
                    if (memRepairProcInfo != null && memRepairProcInfo.getPid() == ionPidRepair) {
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0059, code lost:
        r4 = com.android.server.rms.collector.ResourceCollector.getSumIon();
        android.rms.iaware.AwareLog.d(com.android.server.rms.memrepair.MemRepairPolicy.TAG, "mergeIonRepair total ion = " + r4 + ",mIonThreshold = " + r9.mIonThreshold);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0083, code lost:
        if (((long) r4) >= r9.mIonThreshold) goto L_0x0086;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0085, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0086, code lost:
        return true;
     */
    private boolean checkIonRepair(int sceneType) {
        if (sceneType != 0 && sceneType != 1) {
            AwareLog.w(TAG, "mergeIonRepair invalid param=" + sceneType);
            return false;
        } else if (!this.mIsMemRepairIonFeature) {
            AwareLog.d(TAG, "mergeIonRepair switch=off");
            return false;
        } else {
            long currentTime = System.currentTimeMillis();
            synchronized (this.mIonLock) {
                long interval = currentTime - this.mIonRepairLastTime;
                if (interval < 3600000) {
                    AwareLog.d(TAG, "mergeIonRepair  since last ion repair it is " + interval);
                    return false;
                }
                this.mIonRepairLastTime = currentTime;
            }
        }
    }

    private MemRepairPkgInfo getMemRepairPkgInfoByIon(ArrayMap<String, Integer> ionRepairMap, List<AwareProcessBlockInfo> awareProcessBlockInfos, int sceneType) {
        for (AwareProcessBlockInfo blockInfo : awareProcessBlockInfos) {
            List<AwareProcessInfo> procList = checkAndGetProcList(blockInfo, true);
            if (procList == null) {
                AwareLog.w(TAG, "getMemRepairPkgInfoByIon procList is null");
            } else if (checkProcListNeedIonRepair(procList, ionRepairMap.get(DevicePublicKeyLoader.KEY).intValue())) {
                MemRepairPkgInfo memRepairPkgInfo = new MemRepairPkgInfo(blockInfo.procPackageName);
                memRepairPkgInfo.updateThresHoldType(32);
                for (Iterator<AwareProcessInfo> it = procList.iterator(); it.hasNext(); it = it) {
                    AwareProcessInfo awareProcessInfo = it.next();
                    matchVisibleApp(sceneType, awareProcessInfo);
                    int mem = 0;
                    if (ionRepairMap.get(DevicePublicKeyLoader.KEY).intValue() == awareProcessInfo.procPid) {
                        mem = ionRepairMap.get("value").intValue();
                    }
                    MemRepairProcInfo memRepairProcInfo = MemRepairProcInfo.createMemRepairProcInfo(blockInfo.procUid, awareProcessInfo.procPid, awareProcessInfo.procProcInfo.mProcessName, (long) mem, true);
                    updateMemRepairProc(memRepairProcInfo, awareProcessInfo);
                    memRepairProcInfo.updateThresHoldType(32);
                    if (getForeGroundApp().contains(awareProcessInfo.procPid)) {
                        memRepairPkgInfo.updateThresHoldType(8);
                        AwareLog.d(TAG, "getMemRepairPkgInfoByIon  type=8");
                    }
                    memRepairPkgInfo.addProcInfo(memRepairProcInfo);
                }
                return memRepairPkgInfo;
            }
        }
        return null;
    }

    private boolean checkProcListNeedIonRepair(List<AwareProcessInfo> procList, int maxIonPid) {
        for (AwareProcessInfo awareProcessInfo : procList) {
            if (!checkProcInfo(awareProcessInfo)) {
                AwareLog.d(TAG, "checkNeedIonRepair awareProcessInfo is null");
            } else if (awareProcessInfo.procPid == maxIonPid) {
                AwareLog.i(TAG, "checkNeedIonRepair  pid=" + maxIonPid);
                return true;
            }
        }
        return false;
    }

    private List<MemRepairPkgInfo> getMemRepairPkgInfo(AwareAppMngSortPolicy policy, int sceneType, boolean isPss) {
        List<MemRepairPkgInfo> mrPkgList;
        List<MemRepairProcInfo> procList = processMemRepairProcInfoListWithSumPss(getMemRepairProcList(isPss), isPss, policy);
        if (procList == null || (mrPkgList = matchAndBuildPkgList(sceneType, policy, procList, isPss)) == null || mrPkgList.size() <= 0) {
            return null;
        }
        return mrPkgList;
    }

    private List<MemRepairProcInfo> processMemRepairProcInfoListWithSumPss(List<MemRepairProcInfo> procList, boolean isPss, AwareAppMngSortPolicy policy) {
        if (!MemoryConstant.isConfigMemRepairBySumPssSwitch() || !isPss) {
            AwareLog.i(TAG, "mem repair sum pss switch off or is not pss!");
            return procList;
        }
        List<MemRepairProcInfo> sumProcPssLeakList = getSumProcPssLeak(policy);
        if (procList == null) {
            return sumProcPssLeakList;
        }
        if (!sumProcPssLeakList.isEmpty()) {
            return mergeMemRepairProcInfoList(procList, sumProcPssLeakList);
        }
        AwareLog.i(TAG, "get sum proc pss leak result is null");
        return procList;
    }

    private List<MemRepairPkgInfo> mergePssAndVss(int sceneType) {
        AwareAppMngSortPolicy pssPolicy = MemoryUtils.getAppMngSortPolicyForMemRepair(sceneType);
        if (pssPolicy == null) {
            AwareLog.i(TAG, "getMemRepairPolicy null policy!");
            return null;
        }
        List<MemRepairPkgInfo> pssList = getMemRepairPkgInfo(pssPolicy, sceneType, true);
        AwareLog.d(TAG, "vss para: " + this.mVssThreshold + "|" + this.mVssInitPercent + "|" + this.mVssInitStepValue + "|" + Arrays.toString(this.mVssIntervals));
        if (this.mVssThreshold <= 0 || this.mVssInitPercent <= 0 || this.mVssInitStepValue <= 0 || this.mVssIntervals == null) {
            AwareLog.w(TAG, "Get vss parameters failed");
            return pssList;
        }
        AwareAppMngSortPolicy vssPolicy = MemoryUtils.getAppSortPolicyForMemRepairVss(sceneType, pssPolicy);
        if (vssPolicy == null) {
            AwareLog.i(TAG, "getMemRepairVssPolicy null policy!");
            return null;
        }
        List<MemRepairPkgInfo> vssList = getMemRepairPkgInfo(vssPolicy, sceneType, false);
        if (pssList == null) {
            return vssList;
        }
        if (vssList == null) {
            return pssList;
        }
        pssList.addAll(vssList);
        return pssList;
    }

    private void printMemRepairProcInfo(String state, ProcStateData procStateData, String entryKey, List<Long> procMemList, long[] memSets) {
        AwareLog.i(TAG, state + procStateData.getProcName() + "|" + entryKey + ",procState=" + procStateData.getState() + ",isPss=" + procStateData.isPss() + ",size=" + procMemList.size() + ",procMemList=" + Arrays.toString(procMemList.toArray()) + ",memSets=" + Arrays.toString(memSets) + ",initMem=" + procStateData.getInitMem() + ",minMem=" + procStateData.getMinMem() + ",maxMem=" + procStateData.getMaxMem());
    }

    private List<MemRepairProcInfo> getMemRepairProcList(boolean isPss) {
        MemRepairAlgorithm.MemRepairHolder holder;
        int normThreshold;
        Map<String, List<ProcStateData>> memMap = getMemMap(isPss);
        if (memMap.size() < 1) {
            AwareLog.d(TAG, "isPss:" + isPss + " ,zero memMap size");
            return null;
        }
        AwareLog.d(TAG, "isPss:" + isPss + " ,memMap size=" + memMap.size());
        int ret = 0;
        long[] memSets = new long[this.mMaxCollectCount];
        List<MemRepairProcInfo> procList = new ArrayList<>();
        int normThreshold2 = getNormThreshold(isPss);
        MemRepairAlgorithm.MemRepairHolder holder2 = new MemRepairAlgorithm.MemRepairHolder(normThreshold2, this.mMinCollectCount, this.mMaxCollectCount);
        holder2.updateFloatPercent(this.mDvalueFloatPercent);
        for (Map.Entry<String, List<ProcStateData>> entry : memMap.entrySet()) {
            Iterator<ProcStateData> it = entry.getValue().iterator();
            int ret2 = ret;
            MemRepairProcInfo procInfo = null;
            while (true) {
                if (!it.hasNext()) {
                    holder = holder2;
                    normThreshold = normThreshold2;
                    break;
                }
                ProcStateData procStateData = it.next();
                if (checkProcStateData(procStateData)) {
                    int procState = procStateData.getState();
                    List<Long> procMemList = procStateData.getStateMemList();
                    boolean isForegroundState = ProcStateStatisData.getInstance().isForgroundState(procState);
                    long emergMem = matchEmergProc(procStateData, isForegroundState);
                    if (emergMem > 0) {
                        holder = holder2;
                        normThreshold = normThreshold2;
                        procInfo = getNewProcInfo(procList, procInfo, procStateData, entry.getKey(), emergMem, isForegroundState ? 2 : 4, isPss);
                        if (procInfo != null) {
                            AwareLog.i(TAG, "proc emergency:" + procStateData.getProcName() + "|" + entry.getKey() + "|" + isPss);
                            break;
                        }
                        normThreshold2 = normThreshold;
                        holder2 = holder;
                    } else {
                        int ret3 = applyMemRepairAlgorithm(holder2, procStateData, procMemList, memSets, isPss);
                        if (ret3 != 1) {
                            printMemRepairProcInfo("memory ok:", procStateData, entry.getKey(), procMemList, memSets);
                            ret2 = ret3;
                            normThreshold2 = normThreshold2;
                            holder2 = holder2;
                        } else {
                            ret2 = ret3;
                            MemRepairProcInfo procInfo2 = getNewProcInfo(procList, procInfo, procStateData, entry.getKey(), procStateData.getLastMem(), 1, isPss);
                            if (procInfo2 != null) {
                                procInfo2.addMemSets(memSets, procMemList.size(), procStateData.getState(), procStateData.getMergeCount());
                                printMemRepairProcInfo("memory increase:", procStateData, entry.getKey(), procMemList, memSets);
                            }
                            procInfo = procInfo2;
                            normThreshold2 = normThreshold2;
                            holder2 = holder2;
                        }
                    }
                }
            }
            ret = ret2;
            normThreshold2 = normThreshold;
            holder2 = holder;
        }
        return procList;
    }

    private boolean checkProcStateData(ProcStateData procStateData) {
        if (procStateData != null && procStateData.getStateMemList().size() >= 1 && ProcStateStatisData.getInstance().isValidProcState(procStateData.getState())) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x0032  */
    private int applyMemRepairAlgorithm(MemRepairAlgorithm.MemRepairHolder holder, ProcStateData procStateData, List<Long> procMemList, long[] memSets, boolean isPss) {
        boolean canPredictIncrease;
        int setsCount = procMemList.size();
        int minCount = ProcStateStatisData.getInstance().getMinCount(procStateData.getState());
        long minMem = procStateData.getMinMem();
        long maxMem = procStateData.getMaxMem();
        long initMem = procStateData.getInitMem();
        boolean z = false;
        int ret = 1;
        if (setsCount >= minCount) {
            if (setsCount <= memSets.length) {
                canPredictIncrease = true;
                if (isPss) {
                    int pid = procStateData.getPid();
                    int type = getVssIntervalType(initMem);
                    boolean z2 = type == 2;
                    if (type == 3) {
                        z = true;
                    }
                    boolean needNext = z2 | z;
                    boolean isRuleSatisfied = estimateVssInterval(type, initMem, maxMem, pid);
                    AwareLog.d(TAG, "pid:" + pid + " |type:" + type + " |predictIncrease:" + canPredictIncrease + " |needNext:" + needNext + " |isRuleSatisfied:" + isRuleSatisfied);
                    if (!isRuleSatisfied || !needNext) {
                        if (needNext) {
                            return 6;
                        }
                        if (!isRuleSatisfied) {
                            ret = 6;
                        }
                        return ret;
                    } else if (!canPredictIncrease) {
                        return 6;
                    } else {
                        return memIncreaseJudge(holder, procMemList, memSets, minCount, setsCount);
                    }
                } else if (canPredictIncrease && estimateMinMaxMemory(minMem, maxMem)) {
                    return memIncreaseJudge(holder, procMemList, memSets, minCount, setsCount);
                } else {
                    return 6;
                }
            }
        }
        canPredictIncrease = false;
        if (isPss) {
        }
    }

    private int memIncreaseJudge(MemRepairAlgorithm.MemRepairHolder holder, List<Long> procMemList, long[] memSets, int minCount, int setsCount) {
        long[] memSets2 = getAndUpdateMemSets(procMemList, memSets);
        holder.updateCollectCount(minCount, minCount * 2);
        holder.updateSrcValue(memSets2, setsCount);
        return MemRepairAlgorithm.translateMemRepair(holder, this.mAlgoCallback, null);
    }

    private Map<String, List<ProcStateData>> getMemMap(boolean isPss) {
        if (isPss) {
            return ProcStateStatisData.getInstance().getPssListMap();
        }
        return ProcStateStatisData.getInstance().getVssListMap();
    }

    private int getNormThreshold(boolean isPss) {
        return isPss ? 5120 : 15360;
    }

    private long matchEmergProc(ProcStateData procStateData, boolean isForeground) {
        if (procStateData.isPss()) {
            return matchEmergProcPss(procStateData, isForeground);
        }
        return matchEmergProcVss(procStateData);
    }

    private long matchEmergProcPss(ProcStateData procStateData, boolean isForeground) {
        if (matchEmergPssThreshold((procStateData.getLastMem() * 3) / 2, isForeground) == 0) {
            return 0;
        }
        long pss = getPssByPid(procStateData.getPid());
        AwareLog.i(TAG, "matchEmergProc:" + procStateData.getProcName() + ",lastPss=" + procStateData.getLastMem() + ",curPss=" + pss);
        if (matchEmergPssThreshold(pss, isForeground) == 0) {
            return 0;
        }
        return pss;
    }

    private long matchEmergProcVss(ProcStateData procStateData) {
        long val = getVssByPid(procStateData.getPid());
        if (val <= this.mVssThreshold) {
            return 0;
        }
        AwareLog.i(TAG, "matchEmergProc:" + procStateData + ",curVss" + val + "|" + this.mVssThreshold);
        return val;
    }

    private long[] getAndUpdateMemSets(List<Long> procPssList, long[] memSets) {
        int setsCount = procPssList.size();
        long[] tmpSets = memSets;
        if (setsCount > tmpSets.length) {
            tmpSets = new long[setsCount];
        }
        Arrays.fill(tmpSets, 0L);
        for (int i = 0; i < setsCount; i++) {
            tmpSets[i] = procPssList.get(i).longValue();
        }
        return tmpSets;
    }

    private boolean estimateVssInterval(int type, long initMem, long maxMem, int pid) {
        if (!isParametersValid(initMem, maxMem, pid)) {
            return false;
        }
        AwareLog.d(TAG, "estimateVssInterval: " + type + "|" + initMem + "|" + maxMem + "|" + pid);
        if (type == 1) {
            AwareLog.d(TAG, "VSS_TYPE_DROP: " + false);
            return false;
        } else if (type == 2) {
            long thresPercent = this.mVssInitPercent - ((initMem - this.mVssIntervals[0]) / 51200);
            long realPercent = ((maxMem - initMem) * 100) / initMem;
            boolean result = thresPercent > 0 && realPercent >= thresPercent;
            AwareLog.d(TAG, "VSS_TYPE_PERCENT: " + result + "|" + thresPercent + "|" + realPercent);
            return result;
        } else if (type == 3) {
            long[] jArr = this.mVssIntervals;
            long thresValue = ((initMem - jArr[1]) / 5) + jArr[2];
            long thresValue2 = this.mVssThreshold;
            if (thresValue <= thresValue2) {
                thresValue2 = thresValue;
            }
            boolean result2 = checkIsInRange(thresValue2, maxMem);
            AwareLog.d(TAG, "VSS_TYPE_TREND: " + result2 + "|" + thresValue2 + "|" + maxMem);
            return result2;
        } else if (type == 4) {
            long thresValue3 = ((this.mVssInitStepValue * KB2MB_FACTOR) - ((initMem - this.mVssIntervals[2]) / 5)) + initMem;
            long thresValue4 = this.mVssThreshold;
            if (thresValue3 <= thresValue4) {
                thresValue4 = thresValue3;
            }
            boolean result3 = checkIsInRange(thresValue4, maxMem);
            AwareLog.d(TAG, "VSS_TYPE_NO_TREND: " + result3 + "|" + thresValue4 + "|" + maxMem);
            return result3;
        } else if (type != 5) {
            AwareLog.d(TAG, "VSS_TYPE_ERROR: " + type + "|" + initMem + "|" + maxMem + "|" + pid);
            return false;
        } else {
            long curPss = getVssByPid(pid);
            boolean result4 = curPss > this.mVssThreshold;
            AwareLog.d(TAG, "VSS_TYPE_THRESHOLD: " + result4 + "|" + curPss);
            return result4;
        }
    }

    private boolean isParametersValid(long initMem, long maxMem, int pid) {
        return initMem > 0 && pid > 0 && maxMem >= initMem;
    }

    private boolean checkIsInRange(long thresValue, long maxMem) {
        return 0 < thresValue && thresValue < maxMem;
    }

    private int getVssIntervalType(long initMem) {
        long[] jArr;
        if (initMem <= 0 || (jArr = this.mVssIntervals) == null) {
            return 0;
        }
        if (initMem < jArr[0]) {
            return 1;
        }
        if (initMem < jArr[1]) {
            return 2;
        }
        if (initMem < jArr[2]) {
            return 3;
        }
        if (initMem < this.mVssThreshold) {
            return 4;
        }
        if (initMem < Long.MAX_VALUE) {
            return 5;
        }
        return 0;
    }

    private boolean estimateMinMaxMemory(long minMem, long maxMem) {
        if (minMem < 1 || maxMem < 1 || minMem >= maxMem) {
            AwareLog.i(TAG, "Error min/max Mem!! minMem=" + minMem + ",maxMem=" + maxMem);
            return false;
        } else if (this.mFloatThresHolds == null) {
            AwareLog.w(TAG, "estimateMinMaxMemory: why null thresHolds!!");
            return false;
        } else {
            long diff = maxMem - minMem;
            long multi = (100 * diff) / minMem;
            int i = 0;
            while (true) {
                long[][] jArr = this.mFloatThresHolds;
                if (i >= jArr.length) {
                    return false;
                }
                if (diff < jArr[i][0] || diff >= jArr[i][1]) {
                    i++;
                } else if (multi >= jArr[i][2]) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private MemRepairProcInfo getNewProcInfo(List<MemRepairProcInfo> procList, MemRepairProcInfo procInfo, ProcStateData procStateData, String procKey, long pss, int type, boolean isPss) {
        MemRepairProcInfo procInfoTemp = procInfo;
        if (procInfoTemp == null) {
            procInfoTemp = createMemProcInfo(procStateData, procKey, pss, type, isPss);
            if (procInfoTemp == null) {
                AwareLog.i(TAG, "null procInfo:" + procStateData.getProcName() + "|" + procKey);
                return null;
            }
            procList.add(procInfoTemp);
        } else {
            procInfoTemp.updateThresHoldType(type);
        }
        return procInfoTemp;
    }

    private MemRepairProcInfo createMemProcInfo(ProcStateData procStateData, String procKey, long mem, int type, boolean isPss) {
        int uid;
        int pid;
        if (TextUtils.isEmpty(procKey)) {
            return null;
        }
        String procName = procStateData.getProcName();
        if (TextUtils.isEmpty(procName)) {
            return null;
        }
        String[] procKeys = procKey.split(HwLogRecordManager.VERTICAL_ESC_SEPARATE);
        if (procKeys.length != 2) {
            return null;
        }
        int uid2 = -1;
        try {
            uid2 = Integer.parseInt(procKeys[0]);
            pid = Integer.parseInt(procKeys[1]);
            uid = uid2;
        } catch (NumberFormatException e) {
            AwareLog.w(TAG, "Failed parsing process=" + procName);
            uid = uid2;
            pid = 0;
        }
        if (pid >= 1) {
            if (uid >= 0) {
                MemRepairProcInfo procInfo = MemRepairProcInfo.createMemRepairProcInfo(uid, pid, procName, mem, isPss);
                procInfo.updateThresHoldType(type);
                return procInfo;
            }
        }
        return null;
    }

    private List<MemRepairPkgInfo> matchAndBuildPkgList(int sceneType, AwareAppMngSortPolicy policy, List<MemRepairProcInfo> mrProcInfoList, boolean isPss) {
        List<AwareProcessBlockInfo> forbidStopList = MemoryUtils.getAppMngProcGroup(policy, 0);
        List<AwareProcessBlockInfo> shortageStopList = MemoryUtils.getAppMngProcGroup(policy, 1);
        List<AwareProcessBlockInfo> allowStopList = MemoryUtils.getAppMngProcGroup(policy, 2);
        List<List<AwareProcessBlockInfo>> appMngLists = new ArrayList<>();
        appMngLists.add(forbidStopList);
        appMngLists.add(shortageStopList);
        appMngLists.add(allowStopList);
        ArrayMap<String, MemRepairPkgInfo> mrPkgMap = new ArrayMap<>();
        matchMemLeakPkgList(sceneType, mrPkgMap, mrProcInfoList, appMngLists);
        AwareLog.d(TAG, "all pkg list=" + mrPkgMap.toString());
        buildMemLeakPkgList(sceneType, mrPkgMap, mrProcInfoList, appMngLists, isPss);
        if (mrPkgMap.size() < 1) {
            return null;
        }
        List<MemRepairPkgInfo> mrPkgList = new ArrayList<>();
        mrPkgList.addAll(0, mrPkgMap.values());
        return mrPkgList;
    }

    private void matchMemLeakPkgList(int sceneType, ArrayMap<String, MemRepairPkgInfo> emergPkgMap, List<MemRepairProcInfo> mrProcInfoList, List<List<AwareProcessBlockInfo>> appMngLists) {
        if (mrProcInfoList != null && mrProcInfoList.size() >= 1 && appMngLists != null) {
            for (List<AwareProcessBlockInfo> pkgList : appMngLists) {
                if (pkgList != null && !pkgList.isEmpty()) {
                    for (MemRepairProcInfo mrProcInfo : mrProcInfoList) {
                        matchMemLeakPkgInfo(sceneType, emergPkgMap, mrProcInfo, pkgList);
                    }
                }
            }
        }
    }

    private void buildMemLeakPkgList(int sceneType, ArrayMap<String, MemRepairPkgInfo> mrPkgMap, List<MemRepairProcInfo> mrProcList, List<List<AwareProcessBlockInfo>> appMngLists, boolean isPss) {
        if (mrPkgMap != null && mrPkgMap.size() >= 1 && appMngLists != null) {
            ArrayMap<Integer, MemRepairProcInfo> mrProcMap = new ArrayMap<>();
            for (MemRepairProcInfo mrProcInfo : mrProcList) {
                mrProcMap.put(Integer.valueOf(mrProcInfo.getPid()), mrProcInfo);
            }
            for (List<AwareProcessBlockInfo> pkgList : appMngLists) {
                if (pkgList != null && !pkgList.isEmpty()) {
                    buildMemLeakPkgInfo(sceneType, mrPkgMap, mrProcMap, pkgList, isPss);
                }
            }
        }
    }

    private void matchMemLeakPkgInfo(int sceneType, ArrayMap<String, MemRepairPkgInfo> mrPkgMap, MemRepairProcInfo mrProcInfo, List<AwareProcessBlockInfo> pkgList) {
        String pkgName;
        for (AwareProcessBlockInfo blockInfo : pkgList) {
            List<AwareProcessInfo> procList = checkAndGetProcList(blockInfo, mrProcInfo.isPss());
            if (procList != null) {
                SparseSet forePids = getForeGroundApp();
                for (AwareProcessInfo procInfo : procList) {
                    if (checkProcInfo(procInfo) && procInfo.procPid == mrProcInfo.getPid()) {
                        String mrProcName = mrProcInfo.getProcName();
                        if (!TextUtils.isEmpty(mrProcName)) {
                            if (mrProcName.equals(procInfo.procProcInfo.mProcessName)) {
                                if (procInfo.procProcInfo.mPackageName.size() > 0) {
                                    pkgName = (String) procInfo.procProcInfo.mPackageName.get(0);
                                } else {
                                    pkgName = null;
                                }
                                if (TextUtils.isEmpty(pkgName)) {
                                    AwareLog.i(TAG, "matchMemLeakPkgInfo:null pkgName! Process=" + procInfo.procProcInfo.mProcessName);
                                    return;
                                }
                                MemRepairPkgInfo mrPkgInfo = updatePackageInfo(mrPkgMap, getPkgkey(blockInfo.procUid, pkgName), pkgName, forePids, procInfo);
                                matchVisibleApp(sceneType, procInfo);
                                updateThresHoldType(mrProcInfo, procInfo);
                                updateMemRepairProc(mrProcInfo, procInfo);
                                mrPkgInfo.addProcInfo(mrProcInfo);
                                AwareLog.d(TAG, "matchMemLeakPkgInfo:" + mrPkgInfo.toString());
                                return;
                            }
                        }
                        AwareLog.i(TAG, "matchMemLeakPkgInfo:diff procName! Process=" + procInfo.procProcInfo.mProcessName);
                        return;
                    }
                }
            }
        }
    }

    private void updateThresHoldType(MemRepairProcInfo mrProcInfo, AwareProcessInfo procInfo) {
        if (mrProcInfo.getThresHoldType() != 16) {
            mrProcInfo.updateThresHoldType(matchEmergThreshold(mrProcInfo.getMem(), procInfo.isForegroundApp(), mrProcInfo.isPss()));
        }
    }

    private MemRepairPkgInfo updatePackageInfo(ArrayMap<String, MemRepairPkgInfo> mrPkgMap, String key, String pkgName, SparseSet forePids, AwareProcessInfo procInfo) {
        MemRepairPkgInfo mrPkgInfo = mrPkgMap.get(key);
        if (mrPkgInfo == null) {
            mrPkgInfo = new MemRepairPkgInfo(pkgName);
            mrPkgMap.put(key, mrPkgInfo);
        }
        if (forePids.contains(procInfo.procPid)) {
            mrPkgInfo.updateThresHoldType(8);
        }
        return mrPkgInfo;
    }

    private List<AwareProcessBlockInfo> getAwareProcessBlockInfos(AwareAppMngSortPolicy policy) {
        Collection<? extends AwareProcessBlockInfo> forbidStopList = MemoryUtils.getAppMngProcGroup(policy, 0);
        Collection<? extends AwareProcessBlockInfo> allowStopList = MemoryUtils.getAppMngProcGroup(policy, 2);
        List<AwareProcessBlockInfo> totalBlockInfoList = new ArrayList<>();
        totalBlockInfoList.addAll(forbidStopList);
        totalBlockInfoList.addAll(allowStopList);
        return totalBlockInfoList;
    }

    private List<MemRepairProcInfo> getSumProcPssLeak(AwareAppMngSortPolicy policy) {
        MemRepairPolicy memRepairPolicy = this;
        List<MemRepairProcInfo> memRepairProcInfoList = new ArrayList<>();
        List<AwareProcessBlockInfo> totalList = getAwareProcessBlockInfos(policy);
        if (totalList.isEmpty()) {
            AwareLog.d(TAG, "totalList is empty");
            return memRepairProcInfoList;
        }
        Map<String, List<ProcStateData>> pssMap = ProcStateStatisData.getInstance().getPssListMap();
        if (pssMap.isEmpty()) {
            AwareLog.d(TAG, "zero pssMap size");
            return memRepairProcInfoList;
        }
        long[][] jArr = memRepairPolicy.mProcThresHolds;
        if (jArr != null) {
            boolean z = true;
            if (jArr.length >= 1) {
                char c = 0;
                if (jArr[0].length < 2) {
                    AwareLog.d(TAG, "the threshold is invalid");
                    return memRepairProcInfoList;
                }
                for (AwareProcessBlockInfo blockInfo : totalList) {
                    List<AwareProcessInfo> procList = memRepairPolicy.checkAndGetProcList(blockInfo, z);
                    if (procList == null) {
                        AwareLog.d(TAG, "procList is null");
                    } else {
                        MemRepairTotalSum memRepairTotalSum = new MemRepairTotalSum(0, 1);
                        memRepairPolicy.calculateTotalPss(procList, memRepairTotalSum, pssMap);
                        long totalPss = memRepairTotalSum.getTotalPss();
                        int state = memRepairTotalSum.getState();
                        if (memRepairPolicy.mProcThresHolds[c][state] <= totalPss) {
                            AwareLog.i(TAG, "totalPss=" + totalPss + ";state=" + state + ";limit=" + memRepairPolicy.mProcThresHolds[c][state]);
                            for (AwareProcessInfo procInfo : procList) {
                                if (!memRepairPolicy.checkProcInfo(procInfo)) {
                                    AwareLog.d(TAG, "procInfo is null");
                                } else {
                                    MemRepairProcInfo memRepairProcInfo = MemRepairProcInfo.createMemRepairProcInfo(blockInfo.procUid, procInfo.procPid, ((ProcessInfo) procInfo.procProcInfo).mProcessName, ProcStateStatisData.getInstance().getProcPss(blockInfo.procUid, procInfo.procPid), true);
                                    memRepairProcInfo.updateThresHoldType(16);
                                    memRepairProcInfoList.add(memRepairProcInfo);
                                    memRepairPolicy = this;
                                    totalList = totalList;
                                    pssMap = pssMap;
                                }
                            }
                            memRepairPolicy = this;
                            c = 0;
                            z = true;
                        }
                    }
                }
                return memRepairProcInfoList;
            }
        }
        AwareLog.d(TAG, "the threshold is invalid");
        return memRepairProcInfoList;
    }

    private void calculateTotalPss(List<AwareProcessInfo> procList, MemRepairTotalSum memRepairTotalSum, Map<String, List<ProcStateData>> pssMap) {
        long totalPss = memRepairTotalSum.getTotalPss();
        int state = memRepairTotalSum.getState();
        for (AwareProcessInfo procInfo : procList) {
            if (!checkProcInfo(procInfo)) {
                AwareLog.d(TAG, "procInfo is null");
            } else {
                List<ProcStateData> procStateDataList = pssMap.get(procInfo.procProcInfo.mUid + "|" + procInfo.procProcInfo.mPid);
                if (procStateDataList == null) {
                    AwareLog.d(TAG, "procStateDataList is null");
                } else {
                    boolean isForegroundApp = procInfo.isForegroundApp();
                    int thresIdx = 1;
                    int i = 0;
                    if (procInfo.procProcInfo.mCurAdj == 0) {
                        thresIdx = 0;
                    }
                    if (!isForegroundApp) {
                        i = state;
                    }
                    state = i;
                    for (ProcStateData procStateData : procStateDataList) {
                        if (checkProcStateData(procStateData) && procStateData.getState() == thresIdx) {
                            totalPss += procStateData.getLastMem();
                        }
                    }
                }
            }
        }
        memRepairTotalSum.setTotalPss(totalPss);
        memRepairTotalSum.setState(state);
    }

    private static class MemRepairTotalSum {
        private int state;
        private long totalPss;

        MemRepairTotalSum(long totalPss2, int state2) {
            this.totalPss = totalPss2;
            this.state = state2;
        }

        public long getTotalPss() {
            return this.totalPss;
        }

        public void setTotalPss(long totalPss2) {
            this.totalPss = totalPss2;
        }

        public int getState() {
            return this.state;
        }

        public void setState(int state2) {
            this.state = state2;
        }
    }

    private List<MemRepairProcInfo> mergeMemRepairProcInfoList(List<MemRepairProcInfo> pkgList, List<MemRepairProcInfo> sumProcPssLeakList) {
        AwareLog.d(TAG, "before merge pkgList=" + pkgList);
        for (MemRepairProcInfo mrProcInfo : sumProcPssLeakList) {
            if (!checkInleakPkgInfo(mrProcInfo, pkgList)) {
                pkgList.add(mrProcInfo);
            }
        }
        AwareLog.d(TAG, "after merge pkgList=" + pkgList);
        return pkgList;
    }

    private boolean checkInleakPkgInfo(MemRepairProcInfo mrProcInfo, List<MemRepairProcInfo> pkgList) {
        if (mrProcInfo == null || pkgList == null || pkgList.size() < 1) {
            return false;
        }
        for (MemRepairProcInfo pkgInfo : pkgList) {
            if (mrProcInfo.getPid() == pkgInfo.getPid()) {
                AwareLog.d(TAG, "pid=" + mrProcInfo.getPid());
                return true;
            }
        }
        return false;
    }

    private void buildMemLeakPkgInfo(int sceneType, ArrayMap<String, MemRepairPkgInfo> mrPkgMap, ArrayMap<Integer, MemRepairProcInfo> mrProcMap, List<AwareProcessBlockInfo> pkgList, boolean isPss) {
        String pkgName;
        Iterator<AwareProcessBlockInfo> it = pkgList.iterator();
        while (it.hasNext()) {
            AwareProcessBlockInfo blockInfo = it.next();
            List<AwareProcessInfo> procList = checkAndGetProcList(blockInfo, isPss);
            if (procList != null) {
                SparseSet forePids = getForeGroundApp();
                for (AwareProcessInfo procInfo : procList) {
                    if (checkProcInfo(procInfo) && mrProcMap.get(Integer.valueOf(procInfo.procPid)) == null) {
                        if (procInfo.procProcInfo.mPackageName.size() > 0) {
                            pkgName = (String) procInfo.procProcInfo.mPackageName.get(0);
                        } else {
                            pkgName = null;
                        }
                        if (TextUtils.isEmpty(pkgName)) {
                            AwareLog.i(TAG, "buildMemLeakPkgInfo null pkgName?! Process=" + procInfo.procProcInfo.mProcessName);
                        } else {
                            MemRepairPkgInfo mrPkgInfo = mrPkgMap.get(getPkgkey(blockInfo.procUid, pkgName));
                            if (mrPkgInfo != null) {
                                if (forePids.contains(procInfo.procPid)) {
                                    mrPkgInfo.updateThresHoldType(8);
                                }
                                MemRepairProcInfo mrProcInfo = MemRepairProcInfo.createMemRepairProcInfo(procInfo.procProcInfo.mUid, procInfo.procProcInfo.mPid, procInfo.procProcInfo.mProcessName, 0, isPss);
                                matchVisibleApp(sceneType, procInfo);
                                mrProcInfo.updateThresHoldType(0);
                                updateMemRepairProc(mrProcInfo, procInfo);
                                mrPkgInfo.addProcInfo(mrProcInfo);
                                AwareLog.d(TAG, "buildMemLeakPkgInfo:" + mrPkgInfo.toString());
                                it = it;
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateMemRepairProc(MemRepairProcInfo info, AwareProcessInfo apInfo) {
        info.updateAppMngInfo(apInfo.procCleanType == ProcessCleaner.CleanType.NONE ? 0 : 1, apInfo.isAwareProtected(), apInfo.getProcessStatus(), apInfo.procProcInfo.mCurAdj);
    }

    private List<AwareProcessInfo> checkAndGetProcList(AwareProcessBlockInfo blockInfo, boolean isPss) {
        List<AwareProcessInfo> procList;
        if (blockInfo == null || (procList = blockInfo.getProcessList()) == null || procList.size() < 1) {
            return null;
        }
        if (isPss || !blockInfo.procReason.contains("TYPE:9")) {
            return procList;
        }
        return null;
    }

    private boolean checkProcInfo(AwareProcessInfo procInfo) {
        if (procInfo == null || procInfo.procProcInfo == null || TextUtils.isEmpty(procInfo.procProcInfo.mProcessName)) {
            return false;
        }
        return true;
    }

    private SparseSet getForeGroundApp() {
        SparseSet forePids = new SparseSet();
        AwareAppAssociate.getInstance().getForeGroundApp(forePids);
        return forePids;
    }

    private String getPkgkey(int uid, String pkgName) {
        return uid + "|" + pkgName;
    }

    private int matchEmergThreshold(long val, boolean isForeground, boolean isPss) {
        return isPss ? matchEmergPssThreshold(val, isForeground) : matchEmergVssThreshold(val, isForeground);
    }

    private int matchEmergPssThreshold(long pss, boolean isForeground) {
        long[][] jArr = this.mProcThresHolds;
        if (jArr == null) {
            AwareLog.w(TAG, "matchEmergThreshold: why null thresHolds!!");
            return 0;
        }
        if (jArr[0][!isForeground] > pss) {
            return 0;
        }
        return isForeground ? 2 : 4;
    }

    private int matchEmergVssThreshold(long vss, boolean isForeground) {
        if (this.mVssThreshold > vss) {
            return 0;
        }
        return isForeground ? 2 : 4;
    }

    private void matchVisibleApp(int sceneType, AwareProcessInfo procInfo) {
        if (sceneType == 1 && procInfo.isVisibleApp(100)) {
            procInfo.procCleanType = ProcessCleaner.CleanType.KILL_ALLOW_START;
        }
    }

    private long getPssByPid(int pid) {
        if (pid > 0) {
            return Debug.getPss(pid, null, null);
        }
        return 0;
    }

    public void updateVssThreshold(long vssThreshold) {
        if (0 < vssThreshold && vssThreshold < 4096) {
            this.mVssThreshold = KB2MB_FACTOR * vssThreshold;
        }
        AwareLog.d(TAG, "updateVssThreshold: " + this.mVssThreshold);
    }

    public void updateVssParameters(long[] fields) {
        if (fields != null && fields.length == 5) {
            long dropThreshold = 0;
            this.mVssInitPercent = (0 >= fields[0] || fields[0] >= 100) ? 0 : fields[0];
            this.mVssInitStepValue = fields[1] > 0 ? fields[1] : 0;
            this.mVssIntervals = createVssIntervals();
            long lastValue = 0;
            int i = 0;
            while (true) {
                long[] jArr = this.mVssIntervals;
                if (i < jArr.length) {
                    long current = fields[i + 2] * KB2MB_FACTOR;
                    if (current <= lastValue) {
                        this.mVssIntervals = null;
                        return;
                    }
                    jArr[i] = current;
                    lastValue = current;
                    i++;
                } else if (lastValue > this.mVssThreshold) {
                    this.mVssIntervals = null;
                    return;
                } else {
                    AwareLog.d(TAG, "updateVssParameters: " + this.mVssInitPercent + "|" + this.mVssInitStepValue + "|" + Arrays.toString(this.mVssIntervals));
                    if (fields[2] > 0) {
                        dropThreshold = fields[2];
                    }
                    ProcStateStatisData.getInstance().updateDropThreshold(KB2MB_FACTOR * dropThreshold);
                    return;
                }
            }
        }
    }

    private long[] createVssIntervals() {
        if (this.mVssIntervals == null) {
            this.mVssIntervals = new long[3];
        }
        return this.mVssIntervals;
    }

    private long getVssByPid(int pid) {
        if (pid > 0) {
            return MemoryCollector.getVss(pid);
        }
        return 0;
    }

    private static final class AlgoCallback implements MemRepairAlgorithm.EstimateCallback {
        private AlgoCallback() {
        }

        @Override // com.android.server.rms.memrepair.MemRepairAlgorithm.EstimateCallback
        public int estimateLinear(Object user, MemRepairAlgorithm.CallbackData outData) {
            if (outData == null) {
                return 5;
            }
            if (outData.isIncreased()) {
                AwareLog.d(MemRepairPolicy.TAG, "leak:state=" + outData.mDvalueState + ", dvalues=" + Arrays.toString(outData.mDvalues));
                return 1;
            }
            AwareLog.d(MemRepairPolicy.TAG, "continue:state=" + outData.mDvalueState + ", dvalues=" + Arrays.toString(outData.mDvalues));
            return 3;
        }
    }

    public void updateIonThreshold(int ionThreshold) {
        AwareLog.d(TAG, "updateIonThreshold ionThreshold=" + ionThreshold);
        MemInfoReader memInfoReader = new MemInfoReader();
        memInfoReader.readMemInfo();
        long totalMemMb = memInfoReader.getTotalSize() / 1048576;
        if (ionThreshold < 1 || ((long) ionThreshold) > totalMemMb) {
            AwareLog.w(TAG, "updateIonThreshold ionThreshold is too small or too big");
        } else {
            this.mIonThreshold = ((long) ionThreshold) * 1048576;
        }
    }

    public boolean checkIonConfigParas() {
        if (this.mIonThreshold == 0) {
            return false;
        }
        return true;
    }

    public void updateIonSwitch(int switchValue) {
        AwareLog.d(TAG, "updateIonSwitch switch=" + switchValue);
        boolean z = true;
        if (switchValue != 1) {
            z = false;
        }
        this.mIsMemRepairIonFeature = z;
    }
}
