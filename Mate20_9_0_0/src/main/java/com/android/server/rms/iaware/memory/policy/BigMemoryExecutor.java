package com.android.server.rms.iaware.memory.policy;

import android.os.Bundle;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.memory.utils.BigMemoryInfo;
import com.android.server.rms.iaware.memory.utils.EventTracker;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BigMemoryExecutor extends AbsMemoryExecutor {
    private static final String TAG = "AwareMem_BigMemExec";
    private ThreadPoolExecutor mBigMemAppExecutor;
    private long totalKillMem;

    public BigMemoryExecutor() {
        this.mBigMemAppExecutor = null;
        this.totalKillMem = 0;
        this.mBigMemAppExecutor = new ThreadPoolExecutor(0, 1, 10, TimeUnit.SECONDS, new LinkedBlockingQueue(1), new MemThreadFactory("iaware.mem.bigmem"));
    }

    public void disableMemoryRecover() {
        this.mMemState.setStatus(0);
    }

    public void executeMemoryRecover(Bundle extras) {
        if (extras == null) {
            AwareLog.e(TAG, "executeMemoryRecover extras null");
            return;
        }
        long reqMemory = checkBigAppAvailableMemory(extras);
        if (reqMemory <= 0) {
            this.mMemState.setStatus(0);
            return;
        }
        MemoryScenePolicyList memoryScenePolicyList = MemoryExecutorServer.getInstance().getMemoryScenePolicyList();
        if (memoryScenePolicyList == null) {
            AwareLog.e(TAG, "executeMemoryRecover memoryScenePolicyList null");
            return;
        }
        MemoryScenePolicy bigMemoryScenePolicy = memoryScenePolicyList.getMemoryScenePolicy(MemoryConstant.MEM_SCENE_BIGMEM);
        if (bigMemoryScenePolicy == null) {
            this.mMemState.setStatus(0);
            return;
        }
        extras.putLong("reqMem", reqMemory);
        bigMemoryScenePolicy.setExtras(extras);
        MemoryExecutorServer.getInstance().stopMemoryPolicy(true);
        try {
            this.mBigMemAppExecutor.execute(new CalcRunnable(bigMemoryScenePolicy, true));
        } catch (RejectedExecutionException e) {
            AwareLog.e(TAG, "Failed to execute! reject");
        } catch (Exception e2) {
            AwareLog.e(TAG, "Failed to execute! reset");
        }
    }

    private long checkBigAppAvailableMemory(Bundle extras) {
        long reqMem;
        long targetMemKB;
        long targetMemKB2;
        Bundle bundle = extras;
        int event = bundle.getInt("event");
        if (event == 15010) {
            reqMem = bundle.getLong("reqMem", 0);
            targetMemKB = MemoryConstant.getCriticalMemory() + reqMem;
            MemoryConstant.addTotalAPIRequestMemory(reqMem);
            AwareLog.i(TAG, "request mem event");
            targetMemKB2 = targetMemKB;
        } else {
            targetMemKB2 = BigMemoryInfo.getInstance().getAppLaunchRequestMemory(bundle.getString("appName")) * 1024;
            reqMem = targetMemKB2 - MemoryConstant.getCriticalMemory();
            MemoryConstant.addTotalAPIRequestMemory(reqMem);
            this.totalKillMem = 0;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("big mem event target");
            stringBuilder.append(targetMemKB2);
            stringBuilder.append("kb");
            AwareLog.i(str, stringBuilder.toString());
        }
        targetMemKB = MemoryConstant.getTotalAPIRequestMemory();
        String str2;
        StringBuilder stringBuilder2;
        if (targetMemKB > MemoryConstant.getMaxAPIRequestMemory()) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("total request memory has exceed limit:");
            stringBuilder2.append(targetMemKB);
            stringBuilder2.append("kb");
            AwareLog.i(str2, stringBuilder2.toString());
            return -1;
        }
        long availableNow = MemoryReader.getInstance().getMemAvailable();
        long killMem = targetMemKB2 - availableNow;
        String str3 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Avail:");
        stringBuilder3.append(availableNow);
        stringBuilder3.append("kb. prepare:");
        stringBuilder3.append(reqMem);
        stringBuilder3.append("kb. totalReqMem:");
        stringBuilder3.append(targetMemKB);
        stringBuilder3.append("kb");
        AwareLog.i(str3, stringBuilder3.toString());
        if (killMem <= 0) {
            EventTracker instance = EventTracker.getInstance();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("has enough avail mem:");
            stringBuilder3.append(availableNow);
            instance.trackEvent(1001, 0, 0, stringBuilder3.toString());
            return -1;
        }
        this.totalKillMem += killMem;
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("killMem:");
        stringBuilder2.append(killMem);
        stringBuilder2.append("kb. totalKill:");
        stringBuilder2.append(this.totalKillMem);
        stringBuilder2.append("kb");
        AwareLog.i(str2, stringBuilder2.toString());
        return killMem;
    }
}
