package com.android.server.rms.memrepair;

import android.os.Debug;
import android.rms.iaware.AwareLog;
import android.rms.iaware.memrepair.MemRepairPkgInfo;
import android.rms.iaware.memrepair.MemRepairProcInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.server.location.HwLogRecordManager;
import com.android.server.mtm.iaware.appmng.AwareAppMngSortPolicy;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.taskstatus.ProcessCleaner.CleanType;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.android.server.rms.memrepair.MemRepairAlgorithm.CallbackData;
import com.android.server.rms.memrepair.MemRepairAlgorithm.MRCallback;
import com.android.server.rms.memrepair.MemRepairAlgorithm.MemRepairHolder;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MemRepairPolicy {
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
    private static final int PROCESS_THRES_EMERG = 0;
    public static final int PROCESS_THRES_FG_IDX = 0;
    public static final int PROCESS_THRES_ITEM_COUNT = 2;
    private static final int SCENE_TYPE_MIDNIGHT = 1;
    private static final int SCENE_TYPE_SCREENOFF = 0;
    private static final String TAG = "AwareMem_MRPolicy";
    private static final int TYPE_EMERG_BG_THRESHOLD = 4;
    private static final int TYPE_EMERG_FG_THRESHOLD = 2;
    private static final int TYPE_MEM_GROW_UP = 1;
    private static final int TYPE_NONE = 0;
    private static MemRepairPolicy mMemRepairPolicy;
    private int mDValueFloatPercent = 5;
    private long[][] mFloatThresHolds = null;
    private AlgoCallback mMRCallback = new AlgoCallback();
    private int mMaxCollectCount = 50;
    private int mMinCollectCount = 6;
    private long[][] mProcThresHolds = null;

    private static final class AlgoCallback implements MRCallback {
        private AlgoCallback() {
        }

        public int estimateLinear(Object user, CallbackData outData) {
            if (outData == null) {
                return 5;
            }
            String str;
            StringBuilder stringBuilder;
            if (outData.isIncreased()) {
                str = MemRepairPolicy.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("leak:state=");
                stringBuilder.append(outData.mDValueState);
                stringBuilder.append(", dvalues=");
                stringBuilder.append(Arrays.toString(outData.mDValues));
                AwareLog.d(str, stringBuilder.toString());
                return 1;
            }
            str = MemRepairPolicy.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("continue:state=");
            stringBuilder.append(outData.mDValueState);
            stringBuilder.append(", dvalues=");
            stringBuilder.append(Arrays.toString(outData.mDValues));
            AwareLog.d(str, stringBuilder.toString());
            return 3;
        }
    }

    private MemRepairPolicy() {
    }

    public static MemRepairPolicy getInstance() {
        MemRepairPolicy memRepairPolicy;
        synchronized (MemRepairPolicy.class) {
            if (mMemRepairPolicy == null) {
                mMemRepairPolicy = new MemRepairPolicy();
            }
            memRepairPolicy = mMemRepairPolicy;
        }
        return memRepairPolicy;
    }

    public void updateCollectCount(int fgCollectCount, int bgCollectCount) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCollectCount fgCount=");
        stringBuilder.append(fgCollectCount);
        stringBuilder.append(",bgCount=");
        stringBuilder.append(bgCollectCount);
        AwareLog.d(str, stringBuilder.toString());
        int minCount = fgCollectCount < bgCollectCount ? bgCollectCount : fgCollectCount;
        int i = 6;
        if (minCount >= 6) {
            i = minCount;
        }
        this.mMinCollectCount = i;
        this.mMaxCollectCount = this.mMinCollectCount * 2;
        int i2 = 100;
        if (this.mMaxCollectCount < 100) {
            i2 = this.mMaxCollectCount;
        }
        this.mMaxCollectCount = i2;
    }

    public void updateDValueFloatPercent(int percent) {
        String str;
        StringBuilder stringBuilder;
        if (percent < 1 || percent > 30) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateDValueFloatPercent percent=");
            stringBuilder.append(percent);
            AwareLog.w(str, stringBuilder.toString());
            return;
        }
        this.mDValueFloatPercent = percent;
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("updateDValueFloatPercent=");
        stringBuilder.append(percent);
        AwareLog.d(str, stringBuilder.toString());
    }

    public void updateFloatThresHold(long[][] floatThresHolds) {
        if (floatThresHolds == null || floatThresHolds.length < 1 || floatThresHolds.length > 20 || floatThresHolds[0] == null || floatThresHolds[0].length != 3) {
            AwareLog.w(TAG, "updateFloatThresHold error params");
            return;
        }
        this.mFloatThresHolds = (long[][]) Array.newInstance(long.class, new int[]{floatThresHolds.length, floatThresHolds[0].length});
        int i = 0;
        while (i < floatThresHolds.length) {
            if (floatThresHolds[i] == null || floatThresHolds[i].length != 3) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateFloatThresHold error params=");
                stringBuilder.append(Arrays.toString(floatThresHolds[i]));
                AwareLog.w(str, stringBuilder.toString());
                this.mFloatThresHolds = null;
                break;
            }
            System.arraycopy(floatThresHolds[i], 0, this.mFloatThresHolds[i], 0, floatThresHolds[0].length);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateFloatThresHold thresHolds=");
            stringBuilder2.append(Arrays.toString(this.mFloatThresHolds[i]));
            AwareLog.d(str2, stringBuilder2.toString());
            i++;
        }
    }

    public void updateProcThresHold(long[][] procThresHolds) {
        if (procThresHolds == null || procThresHolds.length != 1 || procThresHolds[0] == null || procThresHolds[0].length != 2) {
            AwareLog.w(TAG, "updateProcThresHold error params");
            return;
        }
        this.mProcThresHolds = (long[][]) Array.newInstance(long.class, new int[]{procThresHolds.length, procThresHolds[0].length});
        int i = 0;
        while (i < procThresHolds.length) {
            if (procThresHolds[i] == null || procThresHolds[i].length != 2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateProcThresHold error params=");
                stringBuilder.append(Arrays.toString(procThresHolds[i]));
                AwareLog.w(str, stringBuilder.toString());
                this.mProcThresHolds = null;
                break;
            }
            System.arraycopy(procThresHolds[i], 0, this.mProcThresHolds[i], 0, procThresHolds[0].length);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateProcThresHold thresHolds=");
            stringBuilder2.append(Arrays.toString(this.mProcThresHolds[i]));
            AwareLog.d(str2, stringBuilder2.toString());
            i++;
        }
    }

    public List<MemRepairPkgInfo> getMemRepairPolicy(int sceneType) {
        List<MemRepairPkgInfo> list = null;
        String str;
        if (sceneType != 0 && sceneType != 1) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMemRepairPolicy invalid param=");
            stringBuilder.append(sceneType);
            AwareLog.i(str, stringBuilder.toString());
            return null;
        } else if (this.mProcThresHolds == null || this.mFloatThresHolds == null) {
            AwareLog.i(TAG, "getMemRepairPolicy null thresholds!");
            return null;
        } else {
            AwareAppMngSortPolicy policy = MemoryUtils.getAppMngSortPolicyForMemRepair(sceneType);
            if (policy == null) {
                AwareLog.i(TAG, "getMemRepairPolicy null policy!");
                return null;
            }
            List<MemRepairProcInfo> procList = getMemRepairProcList();
            if (procList == null || procList.size() < 1) {
                AwareLog.i(TAG, "getMemRepairPolicy null procList!");
                return null;
            }
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getMemRepairPolicy procList size=");
            stringBuilder2.append(procList.size());
            AwareLog.d(str, stringBuilder2.toString());
            List<MemRepairPkgInfo> mrPkgList = matchAndBuildPkgList(sceneType, policy, procList);
            if (mrPkgList != null && mrPkgList.size() > 0) {
                list = mrPkgList;
            }
            return list;
        }
    }

    private List<MemRepairProcInfo> getMemRepairProcList() {
        MemRepairPolicy setsCount = this;
        Map<String, List<ProcStateData>> pssMap = ProcStateStatisData.getInstance().getPssListMap();
        if (pssMap.size() < 1) {
            AwareLog.d(TAG, "zero pssMap size");
            return null;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pssMap size=");
        stringBuilder.append(pssMap.size());
        AwareLog.d(str, stringBuilder.toString());
        int ret = 0;
        long minMem = 0;
        long maxMem = 0;
        int setsCount2 = 0;
        long[] memSets = new long[setsCount.mMaxCollectCount];
        List<MemRepairProcInfo> procList = new ArrayList();
        MemRepairHolder holder = new MemRepairHolder(5120, setsCount.mMinCollectCount, setsCount.mMaxCollectCount);
        holder.updateFloatPercent(setsCount.mDValueFloatPercent);
        Iterator memSets2 = pssMap.entrySet().iterator();
        while (memSets2.hasNext()) {
            Map<String, List<ProcStateData>> map;
            List<MemRepairProcInfo> procList2;
            Iterator it;
            long[] jArr;
            Object obj;
            Entry<String, List<ProcStateData>> entry = (Entry) memSets2.next();
            Iterator procState = ((List) entry.getValue()).iterator();
            int ret2 = ret;
            long minMem2 = minMem;
            long maxMem2 = maxMem;
            int setsCount3 = setsCount2;
            MemRepairProcInfo procInfo = null;
            long[] memSets3 = memSets;
            while (procState.hasNext()) {
                Iterator it2;
                ProcStateData procStateData = (ProcStateData) procState.next();
                if (procStateData != null) {
                    int procState2 = procStateData.getState();
                    List<Long> procPssList = procStateData.getStatePssList();
                    if (procPssList == null) {
                        map = pssMap;
                        it2 = procState;
                        procList2 = procList;
                        it = memSets2;
                        jArr = memSets3;
                    } else if (ProcStateStatisData.getInstance().isValidProcState(procState2)) {
                        boolean isForgroundState = ProcStateStatisData.getInstance().isForgroundState(procState2);
                        long emergPss = setsCount.matchEmergProc(procStateData, isForgroundState);
                        ProcStateData pssMap2;
                        if (emergPss > 0) {
                            map = pssMap;
                            it2 = procState;
                            pssMap2 = procStateData;
                            it = memSets2;
                            jArr = memSets3;
                            procInfo = setsCount.getNewProcInfo(procList, procInfo, procStateData, (String) entry.getKey(), emergPss, isForgroundState ? 2 : 4);
                            if (procInfo != null) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("proc emergency:");
                                stringBuilder.append(pssMap2.getProcName());
                                stringBuilder.append("|");
                                stringBuilder.append((String) entry.getKey());
                                AwareLog.i(str, stringBuilder.toString());
                                procList2 = procList;
                                obj = 1;
                                break;
                            }
                            memSets3 = jArr;
                            pssMap = map;
                            procState = it2;
                            memSets2 = it;
                        } else {
                            List<Long> procPssList2 = procPssList;
                            map = pssMap;
                            it2 = procState;
                            it = memSets2;
                            pssMap2 = procStateData;
                            int procState3 = procState2;
                            jArr = memSets3;
                            memSets3 = ProcStateStatisData.getInstance().getMinCount(procState3);
                            long[] jArr2;
                            if (procPssList2.size() < memSets3) {
                                jArr2 = memSets3;
                                procList2 = procList;
                            } else if (procPssList2.size() > jArr.length) {
                                jArr2 = memSets3;
                                procList2 = procList;
                            } else {
                                setsCount2 = procPssList2.size();
                                jArr = setsCount.getAndUpdateMemSets(procPssList2, jArr);
                                maxMem = pssMap2.getMinPss();
                                minMem = pssMap2.getMaxPss();
                                if (setsCount.estimateMinMaxMemory(maxMem, minMem)) {
                                    procList2 = procList;
                                    holder.updateCollectCount(memSets3, memSets3 * 2);
                                    holder.updateSrcValue(jArr, setsCount2);
                                    procState2 = MemRepairAlgorithm.translateMemRepair(holder, setsCount.mMRCallback, null);
                                    if (procState2 != 1) {
                                        str = TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        int setsCount4 = setsCount2;
                                        stringBuilder2.append("memory ok:");
                                        stringBuilder2.append(pssMap2.getProcName());
                                        stringBuilder2.append("|");
                                        stringBuilder2.append((String) entry.getKey());
                                        stringBuilder2.append(",procState=");
                                        stringBuilder2.append(procState3);
                                        stringBuilder2.append(",size=");
                                        stringBuilder2.append(procPssList2.size());
                                        stringBuilder2.append(",pssSets=");
                                        stringBuilder2.append(Arrays.toString(jArr));
                                        stringBuilder2.append(",minMem=");
                                        stringBuilder2.append(maxMem);
                                        stringBuilder2.append(",maxMem=");
                                        stringBuilder2.append(minMem);
                                        AwareLog.d(str, stringBuilder2.toString());
                                        maxMem2 = minMem;
                                        minMem2 = maxMem;
                                        ret2 = procState2;
                                        memSets3 = jArr;
                                        pssMap = map;
                                        procState = it2;
                                        memSets2 = it;
                                        procList = procList2;
                                        setsCount3 = setsCount4;
                                    } else {
                                        long maxMem3;
                                        long maxMem4 = minMem;
                                        long minMem3 = maxMem;
                                        int ret3 = procState2;
                                        int setsCount5 = setsCount2;
                                        int minCount = memSets3;
                                        MemRepairProcInfo procInfo2 = setsCount.getNewProcInfo(procList2, procInfo, pssMap2, (String) entry.getKey(), pssMap2.getLastPss(), 1);
                                        if (procInfo2 == null) {
                                            maxMem3 = maxMem4;
                                            maxMem = minMem3;
                                        } else {
                                            procInfo2.addPssSets(jArr, setsCount5, pssMap2.getState(), pssMap2.getMergeCount());
                                            String str2 = TAG;
                                            StringBuilder stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("memory increase:");
                                            stringBuilder3.append(pssMap2.getProcName());
                                            stringBuilder3.append("|");
                                            stringBuilder3.append((String) entry.getKey());
                                            stringBuilder3.append(",procState=");
                                            stringBuilder3.append(procState3);
                                            stringBuilder3.append(",size=");
                                            stringBuilder3.append(procPssList2.size());
                                            stringBuilder3.append(",pssSets=");
                                            stringBuilder3.append(Arrays.toString(jArr));
                                            stringBuilder3.append(",minMem=");
                                            maxMem = minMem3;
                                            stringBuilder3.append(maxMem);
                                            stringBuilder3.append(",maxMem=");
                                            maxMem3 = maxMem4;
                                            stringBuilder3.append(maxMem3);
                                            AwareLog.i(str2, stringBuilder3.toString());
                                        }
                                        procInfo = procInfo2;
                                        maxMem2 = maxMem3;
                                        setsCount3 = setsCount5;
                                        memSets3 = jArr;
                                        ret2 = ret3;
                                        pssMap = map;
                                        procState = it2;
                                        memSets2 = it;
                                        procList = procList2;
                                        setsCount = this;
                                        minMem2 = maxMem;
                                    }
                                } else {
                                    str = TAG;
                                    StringBuilder stringBuilder4 = new StringBuilder();
                                    procList2 = procList;
                                    stringBuilder4.append("less min/max-mem:");
                                    stringBuilder4.append(pssMap2.getProcName());
                                    stringBuilder4.append("|");
                                    stringBuilder4.append((String) entry.getKey());
                                    stringBuilder4.append(",procState=");
                                    stringBuilder4.append(procState3);
                                    stringBuilder4.append(",size=");
                                    stringBuilder4.append(procPssList2.size());
                                    stringBuilder4.append(",pssSets=");
                                    stringBuilder4.append(Arrays.toString(jArr));
                                    stringBuilder4.append(",minMem=");
                                    stringBuilder4.append(maxMem);
                                    stringBuilder4.append(",maxMem=");
                                    stringBuilder4.append(minMem);
                                    AwareLog.d(str, stringBuilder4.toString());
                                    maxMem2 = minMem;
                                    minMem2 = maxMem;
                                    setsCount3 = setsCount2;
                                    memSets3 = jArr;
                                    pssMap = map;
                                    procState = it2;
                                    memSets2 = it;
                                    procList = procList2;
                                }
                            }
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("less/more size:");
                            stringBuilder.append(pssMap2.getProcName());
                            stringBuilder.append("|");
                            stringBuilder.append((String) entry.getKey());
                            stringBuilder.append(",procState=");
                            stringBuilder.append(procState3);
                            stringBuilder.append(",size=");
                            stringBuilder.append(procPssList2.size());
                            AwareLog.d(str, stringBuilder.toString());
                        }
                    }
                    memSets3 = jArr;
                    pssMap = map;
                    procState = it2;
                    memSets2 = it;
                    procList = procList2;
                    setsCount = this;
                }
                map = pssMap;
                it2 = procState;
                procList2 = procList;
                it = memSets2;
                jArr = memSets3;
                memSets3 = jArr;
                pssMap = map;
                procState = it2;
                memSets2 = it;
                procList = procList2;
                setsCount = this;
            }
            map = pssMap;
            procList2 = procList;
            it = memSets2;
            obj = 1;
            jArr = memSets3;
            memSets = jArr;
            setsCount2 = setsCount3;
            minMem = minMem2;
            maxMem = maxMem2;
            ret = ret2;
            Object obj2 = obj;
            pssMap = map;
            memSets2 = it;
            procList = procList2;
            setsCount = this;
        }
        return procList;
    }

    private long matchEmergProc(ProcStateData procStateData, boolean isForeground) {
        if (matchEmergThreshold((procStateData.getLastPss() * 3) / 2, isForeground) == 0) {
            return 0;
        }
        long pss = getPssByPid(procStateData.getPid());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("matchEmergProc:");
        stringBuilder.append(procStateData.getProcName());
        stringBuilder.append(",lastPss=");
        stringBuilder.append(procStateData.getLastPss());
        stringBuilder.append(",curPss=");
        stringBuilder.append(pss);
        AwareLog.i(str, stringBuilder.toString());
        if (matchEmergThreshold(pss, isForeground) == 0) {
            return 0;
        }
        return pss;
    }

    private long[] getAndUpdateMemSets(List<Long> procPssList, long[] memSets) {
        int setsCount = procPssList.size();
        if (setsCount > memSets.length) {
            memSets = new long[setsCount];
        }
        Arrays.fill(memSets, 0);
        for (int i = 0; i < setsCount; i++) {
            memSets[i] = ((Long) procPssList.get(i)).longValue();
        }
        return memSets;
    }

    private boolean estimateMinMaxMemory(long minMem, long maxMem) {
        if (minMem < 1 || maxMem < 1 || minMem >= maxMem) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error min/max Mem!! minMem=");
            stringBuilder.append(minMem);
            stringBuilder.append(",maxMem=");
            stringBuilder.append(maxMem);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        } else if (this.mFloatThresHolds == null) {
            AwareLog.w(TAG, "estimateMinMaxMemory: why null thresHolds!!");
            return false;
        } else {
            long diff = maxMem - minMem;
            long multi = (100 * diff) / minMem;
            boolean estimated = false;
            int i = 0;
            while (i < this.mFloatThresHolds.length) {
                if (diff < this.mFloatThresHolds[i][0] || diff >= this.mFloatThresHolds[i][1]) {
                    i++;
                } else {
                    if (multi >= this.mFloatThresHolds[i][2]) {
                        estimated = true;
                    }
                    return estimated;
                }
            }
            return estimated;
        }
    }

    private MemRepairProcInfo getNewProcInfo(List<MemRepairProcInfo> procList, MemRepairProcInfo procInfo, ProcStateData procStateData, String procKey, long pss, int type) {
        if (procInfo == null) {
            procInfo = createMRProcInfo(procStateData, procKey, pss, type);
            if (procInfo == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("null procInfo:");
                stringBuilder.append(procStateData.getProcName());
                stringBuilder.append("|");
                stringBuilder.append(procKey);
                AwareLog.i(str, stringBuilder.toString());
                return null;
            }
            procList.add(procInfo);
        } else {
            procInfo.updateThresHoldType(type);
        }
        return procInfo;
    }

    private MemRepairProcInfo createMRProcInfo(ProcStateData procStateData, String procKey, long pss, int type) {
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
        int pid;
        int uid;
        int uid2 = -1;
        int pid2 = 0;
        try {
            uid2 = Integer.parseInt(procKeys[0]);
            pid = Integer.parseInt(procKeys[1]);
            uid = uid2;
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed parsing process=");
            stringBuilder.append(procName);
            AwareLog.w(str, stringBuilder.toString());
            uid = uid2;
            pid = pid2;
        }
        if (pid < 1 || uid < 0) {
            uid2 = type;
            return null;
        }
        MemRepairProcInfo memRepairProcInfo = new MemRepairProcInfo(uid, pid, procName, pss);
        memRepairProcInfo.updateThresHoldType(type);
        return memRepairProcInfo;
    }

    private List<MemRepairPkgInfo> matchAndBuildPkgList(int sceneType, AwareAppMngSortPolicy policy, List<MemRepairProcInfo> mrProcInfoList) {
        AwareAppMngSortPolicy awareAppMngSortPolicy = policy;
        List<AwareProcessBlockInfo> forbidStopList = MemoryUtils.getAppMngProcGroup(awareAppMngSortPolicy, 0);
        List<AwareProcessBlockInfo> shortageStopList = MemoryUtils.getAppMngProcGroup(awareAppMngSortPolicy, 1);
        List<AwareProcessBlockInfo> allowStopList = MemoryUtils.getAppMngProcGroup(awareAppMngSortPolicy, 2);
        ArrayMap<String, MemRepairPkgInfo> mrPkgMap = new ArrayMap();
        List<MemRepairProcInfo> list = mrProcInfoList;
        List<AwareProcessBlockInfo> list2 = forbidStopList;
        List<AwareProcessBlockInfo> list3 = shortageStopList;
        List<AwareProcessBlockInfo> list4 = allowStopList;
        matchMemLeakPkgList(sceneType, mrPkgMap, list, list2, list3, list4);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("all pkg list=");
        stringBuilder.append(mrPkgMap.toString());
        AwareLog.d(str, stringBuilder.toString());
        buildMemLeakPkgList(sceneType, mrPkgMap, list, list2, list3, list4);
        if (mrPkgMap.size() < 1) {
            return null;
        }
        List<MemRepairPkgInfo> mrPkgList = new ArrayList();
        mrPkgList.addAll(0, mrPkgMap.values());
        return mrPkgList;
    }

    private void matchMemLeakPkgList(int sceneType, ArrayMap<String, MemRepairPkgInfo> emergPkgMap, List<MemRepairProcInfo> mrProcInfoList, List<AwareProcessBlockInfo> forbidStopList, List<AwareProcessBlockInfo> shortageStopList, List<AwareProcessBlockInfo> allowStopList) {
        if (mrProcInfoList != null && mrProcInfoList.size() >= 1) {
            appMngList = new Object[3];
            int i = 0;
            appMngList[0] = forbidStopList;
            appMngList[1] = shortageStopList;
            appMngList[2] = allowStopList;
            while (true) {
                int i2 = i;
                if (i2 < appMngList.length) {
                    List<AwareProcessBlockInfo> pkgList = appMngList[i2];
                    if (!(pkgList == null || pkgList.isEmpty())) {
                        for (MemRepairProcInfo mrProcInfo : mrProcInfoList) {
                            matchMemLeakPkgInfo(sceneType, emergPkgMap, mrProcInfo, pkgList);
                        }
                    }
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private void buildMemLeakPkgList(int sceneType, ArrayMap<String, MemRepairPkgInfo> mrPkgMap, List<MemRepairProcInfo> mrProcList, List<AwareProcessBlockInfo> forbidStopList, List<AwareProcessBlockInfo> shortageStopList, List<AwareProcessBlockInfo> allowStopList) {
        if (mrPkgMap != null && mrPkgMap.size() >= 1) {
            appMngList = new Object[3];
            int i = 0;
            appMngList[0] = forbidStopList;
            appMngList[1] = shortageStopList;
            appMngList[2] = allowStopList;
            ArrayMap<Integer, MemRepairProcInfo> mrProcMap = new ArrayMap();
            for (MemRepairProcInfo mrProcInfo : mrProcList) {
                mrProcMap.put(Integer.valueOf(mrProcInfo.getPid()), mrProcInfo);
            }
            while (i < appMngList.length) {
                List<AwareProcessBlockInfo> pkgList = appMngList[i];
                if (!(pkgList == null || pkgList.isEmpty())) {
                    buildMemLeakPkgInfo(sceneType, mrPkgMap, mrProcMap, pkgList);
                }
                i++;
            }
        }
    }

    private void matchMemLeakPkgInfo(int sceneType, ArrayMap<String, MemRepairPkgInfo> mrPkgMap, MemRepairProcInfo mrProcInfo, List<AwareProcessBlockInfo> pkgList) {
        int i;
        ArrayMap<String, MemRepairPkgInfo> arrayMap = mrPkgMap;
        MemRepairProcInfo memRepairProcInfo = mrProcInfo;
        for (AwareProcessBlockInfo blockInfo : pkgList) {
            List<AwareProcessInfo> procList = checkAndGetProcList(blockInfo);
            if (procList != null) {
                for (AwareProcessInfo procInfo : procList) {
                    if (checkProcInfo(procInfo)) {
                        if (procInfo.mPid == mrProcInfo.getPid()) {
                            String mrProcName = mrProcInfo.getProcName();
                            String str;
                            if (TextUtils.isEmpty(mrProcName) || !mrProcName.equals(procInfo.mProcInfo.mProcessName)) {
                                i = sceneType;
                                str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("matchMemLeakPkgInfo:while diff procName?! Process=");
                                stringBuilder.append(procInfo.mProcInfo.mProcessName);
                                AwareLog.i(str, stringBuilder.toString());
                                return;
                            }
                            str = null;
                            int i2 = 0;
                            if (procInfo.mProcInfo.mPackageName.size() > 0) {
                                str = (String) procInfo.mProcInfo.mPackageName.get(0);
                            }
                            String str2;
                            if (TextUtils.isEmpty(str)) {
                                str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("matchMemLeakPkgInfo:while null pkgName?! Process=");
                                stringBuilder2.append(procInfo.mProcInfo.mProcessName);
                                AwareLog.i(str2, stringBuilder2.toString());
                                return;
                            }
                            str2 = getPkgkey(blockInfo.mUid, str);
                            MemRepairPkgInfo mrPkgInfo = (MemRepairPkgInfo) arrayMap.get(str2);
                            if (mrPkgInfo == null) {
                                mrPkgInfo = new MemRepairPkgInfo(str);
                                arrayMap.put(str2, mrPkgInfo);
                            }
                            matchVisibleApp(sceneType, procInfo);
                            memRepairProcInfo.updateThresHoldType(matchEmergThreshold(mrProcInfo.getPss(), procInfo.isForegroundApp()));
                            if (procInfo.mCleanType != CleanType.NONE) {
                                i2 = 1;
                            }
                            memRepairProcInfo.updateAppMngInfo(i2, procInfo.isAwareProtected(), procInfo.getProcessStatus(), procInfo.mProcInfo.mCurAdj);
                            mrPkgInfo.addProcInfo(memRepairProcInfo);
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("matchMemLeakPkgInfo:");
                            stringBuilder3.append(mrPkgInfo.toString());
                            AwareLog.d(str3, stringBuilder3.toString());
                            return;
                        }
                    }
                }
                i = sceneType;
            }
        }
        i = sceneType;
    }

    private void buildMemLeakPkgInfo(int sceneType, ArrayMap<String, MemRepairPkgInfo> mrPkgMap, ArrayMap<Integer, MemRepairProcInfo> mrProcMap, List<AwareProcessBlockInfo> pkgList) {
        int i;
        ArrayMap<Integer, MemRepairProcInfo> arrayMap;
        ArrayMap<String, MemRepairPkgInfo> arrayMap2;
        MemRepairPolicy memRepairPolicy = this;
        for (AwareProcessBlockInfo blockInfo : pkgList) {
            List<AwareProcessInfo> procList = memRepairPolicy.checkAndGetProcList(blockInfo);
            if (procList != null) {
                for (AwareProcessInfo procInfo : procList) {
                    if (memRepairPolicy.checkProcInfo(procInfo)) {
                        if (mrProcMap.get(Integer.valueOf(procInfo.mPid)) == null) {
                            String pkgName = null;
                            int i2 = 0;
                            if (procInfo.mProcInfo.mPackageName.size() > 0) {
                                pkgName = (String) procInfo.mProcInfo.mPackageName.get(0);
                            }
                            StringBuilder stringBuilder;
                            if (TextUtils.isEmpty(pkgName)) {
                                String str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("buildMemLeakPkgInfo null pkgName?! Process=");
                                stringBuilder.append(procInfo.mProcInfo.mProcessName);
                                AwareLog.i(str, stringBuilder.toString());
                            } else {
                                MemRepairPkgInfo mrPkgInfo = (MemRepairPkgInfo) mrPkgMap.get(memRepairPolicy.getPkgkey(blockInfo.mUid, pkgName));
                                if (mrPkgInfo == null) {
                                    i = sceneType;
                                } else {
                                    MemRepairProcInfo mrProcInfo = new MemRepairProcInfo(procInfo.mProcInfo.mUid, procInfo.mProcInfo.mPid, procInfo.mProcInfo.mProcessName, 0);
                                    memRepairPolicy.matchVisibleApp(sceneType, procInfo);
                                    mrProcInfo.updateThresHoldType(0);
                                    if (procInfo.mCleanType != CleanType.NONE) {
                                        i2 = 1;
                                    }
                                    mrProcInfo.updateAppMngInfo(i2, procInfo.isAwareProtected(), procInfo.getProcessStatus(), procInfo.mProcInfo.mCurAdj);
                                    mrPkgInfo.addProcInfo(mrProcInfo);
                                    String str2 = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("buildMemLeakPkgInfo:");
                                    stringBuilder.append(mrPkgInfo.toString());
                                    AwareLog.d(str2, stringBuilder.toString());
                                }
                                memRepairPolicy = this;
                            }
                        }
                    } else {
                        arrayMap = mrProcMap;
                    }
                }
                i = sceneType;
                arrayMap2 = mrPkgMap;
                arrayMap = mrProcMap;
                memRepairPolicy = this;
            }
        }
        i = sceneType;
        arrayMap2 = mrPkgMap;
        arrayMap = mrProcMap;
    }

    private List<AwareProcessInfo> checkAndGetProcList(AwareProcessBlockInfo blockInfo) {
        if (blockInfo == null) {
            return null;
        }
        List<AwareProcessInfo> procList = blockInfo.getProcessList();
        if (procList == null || procList.size() < 1) {
            return null;
        }
        return procList;
    }

    private boolean checkProcInfo(AwareProcessInfo procInfo) {
        if (procInfo == null || procInfo.mProcInfo == null || TextUtils.isEmpty(procInfo.mProcInfo.mProcessName)) {
            return false;
        }
        return true;
    }

    private String getPkgkey(int uid, String pkgName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(uid);
        stringBuilder.append("|");
        stringBuilder.append(pkgName);
        return stringBuilder.toString();
    }

    private int matchEmergThreshold(long pss, boolean isForeground) {
        if (this.mProcThresHolds == null) {
            AwareLog.w(TAG, "matchEmergThreshold: why null thresHolds!!");
            return 0;
        }
        if (this.mProcThresHolds[0][isForeground ^ 1] > pss) {
            return 0;
        }
        return isForeground ? 2 : 4;
    }

    private void matchVisibleApp(int sceneType, AwareProcessInfo procInfo) {
        if (sceneType == 1 && procInfo.isVisibleApp(100)) {
            procInfo.mCleanType = CleanType.KILL_ALLOW_START;
        }
    }

    private long getPssByPid(int pid) {
        if (pid > 0) {
            return Debug.getPss(pid, null, null);
        }
        return 0;
    }
}
