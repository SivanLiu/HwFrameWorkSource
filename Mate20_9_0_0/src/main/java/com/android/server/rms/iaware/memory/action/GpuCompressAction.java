package com.android.server.rms.iaware.memory.action;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.AwareAppMngSortPolicy;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.rms.iaware.feature.MemoryFeature2;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import com.huawei.android.app.HwActivityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GpuCompressAction extends Action {
    private static final int MAX_PID_COUNT_GMC = 5;
    private static final int MIN_GMC_UID = 10000;
    private static final String TAG = "AwareMem_GMC";
    private static Map<Integer, GmcStat> hasGMCMap = new ArrayMap();
    private int mGmcCount = 0;

    private static class GmcStat {
        public int mPid = 0;
        public long mTime = 0;

        public GmcStat(int pid) {
            this.mPid = pid;
            this.mTime = SystemClock.elapsedRealtime();
        }
    }

    public GpuCompressAction(Context context) {
        super(context);
    }

    public static void removeUidFromGMCMap(int uid) {
        int pid = 0;
        long time = 0;
        synchronized (hasGMCMap) {
            if (hasGMCMap.containsKey(Integer.valueOf(uid))) {
                GmcStat gs = (GmcStat) hasGMCMap.get(Integer.valueOf(uid));
                pid = gs.mPid;
                time = gs.mTime;
                hasGMCMap.remove(Integer.valueOf(uid));
            }
        }
        if (pid > 0) {
            MemoryUtils.setReclaimGPUMemory(false, pid);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("decompress gpu memory, uid:");
            stringBuilder.append(uid);
            stringBuilder.append(" proc:");
            stringBuilder.append(pid);
            stringBuilder.append(" time:");
            stringBuilder.append(time);
            AwareLog.d(str, stringBuilder.toString());
        }
    }

    public static void doGmc(int uid) {
        long startTime = SystemClock.elapsedRealtime();
        if (uid >= 10000) {
            List<String> pids = HwActivityManager.getPidWithUiFromUid(uid);
            String str;
            StringBuilder stringBuilder;
            if (pids.size() == 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("this uid:");
                stringBuilder.append(uid);
                stringBuilder.append(" has no UI.");
                AwareLog.d(str, stringBuilder.toString());
                return;
            }
            for (String pid : pids) {
                try {
                    int intPid = Integer.parseInt(pid);
                    synchronized (hasGMCMap) {
                        if (hasGMCMap.containsKey(Integer.valueOf(uid))) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("this uid ");
                            stringBuilder2.append(uid);
                            stringBuilder2.append(" has compressed gpu memory.");
                            AwareLog.d(str2, stringBuilder2.toString());
                        } else {
                            hasGMCMap.put(Integer.valueOf(uid), new GmcStat(intPid));
                            MemoryUtils.setReclaimGPUMemory(true, intPid);
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("compress gpu memory from hiber req, uid:");
                            stringBuilder3.append(uid);
                            stringBuilder3.append(" pid:");
                            stringBuilder3.append(pid);
                            AwareLog.d(str3, stringBuilder3.toString());
                        }
                    }
                } catch (NumberFormatException e) {
                }
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("do gmc uid:");
            stringBuilder.append(uid);
            stringBuilder.append(" used time:");
            stringBuilder.append(SystemClock.elapsedRealtime() - startTime);
            AwareLog.d(str, stringBuilder.toString());
        }
    }

    private void generatePidList(List<AwareProcessBlockInfo> procsGroups, List<AwareProcessInfo> waitForCompressPidList) {
        if (procsGroups != null && waitForCompressPidList != null) {
            for (AwareProcessBlockInfo blockInfo : procsGroups) {
                if (blockInfo != null) {
                    if (blockInfo.mUid >= 10000) {
                        List<AwareProcessInfo> processList = blockInfo.getProcessList();
                        if (processList != null) {
                            synchronized (hasGMCMap) {
                                if (hasGMCMap.containsKey(Integer.valueOf(blockInfo.mUid))) {
                                } else {
                                    for (AwareProcessInfo proc : processList) {
                                        if (proc != null) {
                                            if (proc.mProcInfo != null) {
                                                if (proc.mHasShownUi && proc.mProcInfo.mCurAdj > 0) {
                                                    waitForCompressPidList.add(proc);
                                                    hasGMCMap.put(Integer.valueOf(blockInfo.mUid), new GmcStat(proc.mPid));
                                                    this.mGmcCount--;
                                                    if (this.mGmcCount <= 0) {
                                                        return;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        List<AwareProcessInfo> list = processList;
                    }
                }
            }
        }
    }

    public int execute(Bundle extras) {
        if (extras == null) {
            AwareLog.w(TAG, "gmc not action for extras null");
            return -1;
        } else if (!MemoryFeature2.isUpMemoryFeature.get() || MemoryConstant.getConfigGmcSwitch() == 0) {
            AwareLog.d(TAG, "gmc can not action for not iaware2.0 or gmc function is close");
            return 0;
        } else {
            long availableRam = MemoryReader.getInstance().getMemAvailable();
            if (availableRam > MemoryConstant.getGpuMemoryLimit()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("gmc can not action for available mem:");
                stringBuilder.append(availableRam);
                stringBuilder.append(" > gmc limit:");
                stringBuilder.append(MemoryConstant.getGpuMemoryLimit());
                AwareLog.d(str, stringBuilder.toString());
                return 0;
            }
            long start = SystemClock.elapsedRealtime();
            this.mGmcCount = 5;
            AwareAppMngSortPolicy policy = MemoryUtils.getAppMngSortPolicy(1, 3);
            if (policy == null) {
                AwareLog.w(TAG, "getAppMngSortPolicy null policy!");
                return -1;
            }
            List<AwareProcessInfo> waitForCompressPidList = new ArrayList();
            generatePidList(MemoryUtils.getAppMngProcGroup(policy, 2), waitForCompressPidList);
            if (this.mGmcCount > 0) {
                generatePidList(MemoryUtils.getAppMngProcGroup(policy, 1), waitForCompressPidList);
            }
            doCompress(waitForCompressPidList);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("compress gpu memory use time:");
            stringBuilder2.append(SystemClock.elapsedRealtime() - start);
            AwareLog.d(str2, stringBuilder2.toString());
            return 0;
        }
    }

    private boolean doCompress(List<AwareProcessInfo> waitForCompressPidList) {
        boolean ret = false;
        if (waitForCompressPidList == null) {
            return false;
        }
        for (AwareProcessInfo pinfo : waitForCompressPidList) {
            if (pinfo != null) {
                if (pinfo.mProcInfo != null) {
                    if (pinfo.mProcInfo.mCurAdj > 0) {
                        ret = true;
                        MemoryUtils.setReclaimGPUMemory(true, pinfo.mPid);
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("compress gpu memory, pid:");
                        stringBuilder.append(pinfo.mPid);
                        stringBuilder.append(" proc:");
                        stringBuilder.append(pinfo.mProcInfo.mProcessName);
                        AwareLog.d(str, stringBuilder.toString());
                    }
                }
            }
        }
        return ret;
    }

    public void reset() {
    }
}
