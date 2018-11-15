package com.android.server.rms.iaware.memory.policy;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.rms.iaware.memory.data.handle.DataInputHandle;
import com.android.server.rms.iaware.memory.utils.BigDataStore;
import com.android.server.rms.iaware.memory.utils.CpuReader;
import com.android.server.rms.iaware.memory.utils.EventTracker;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import com.android.server.rms.iaware.memory.utils.MemoryUtils;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IdleMemoryExecutor extends AbsMemoryExecutor {
    private static final int CONTINUOUS_LOW_MEM_COUNT_THRESHOLD = 3;
    private static final int MSG_CYCLE_RECYCLE = 11;
    private static final int PROTECTLRU_MIN_TIME_GAP = 600000;
    private static final int SYSTEM_MEMORY_STATE_BASE = 0;
    private static final int SYSTEM_MEMORY_STATE_LOW = 1;
    private static final int SYSTEM_MEMORY_STATE_NORMAL = 0;
    private static final String TAG = "AwareMem_IdleMemExec";
    private BigDataStore mBigDataStore;
    private ThreadPoolExecutor mIdleMemAppExecutor;
    private ActionFlag mLastExecutorActionFlag;
    private MemHandler mMemHandler;
    private int mProtectCacheFlag;
    private long mProtectCacheTimestamp;
    private long mSysCpuOverLoadCnt;
    private LimitedSizeQueue<Integer> mSysLowMemStateTracker;

    private enum ActionFlag {
        ACTION_NONE,
        ACTION_RECLAIM,
        ACTION_KILL
    }

    private static final class CpuResult {
        boolean mBusy;
        int mCpuLoad;
        String mTraceInfo;

        private CpuResult() {
            this.mCpuLoad = 0;
            this.mBusy = true;
            this.mTraceInfo = null;
        }
    }

    private static final class LimitedSizeQueue<K> extends LinkedList<K> {
        private static final long serialVersionUID = 6928859904407185256L;
        private int maxSize;

        public LimitedSizeQueue(int size) {
            this.maxSize = size;
        }

        public boolean add(K k) {
            boolean added = super.add(k);
            while (added && size() > this.maxSize) {
                super.remove();
            }
            return added;
        }
    }

    private final class MemHandler extends Handler {
        public MemHandler(Looper looper) {
            super(looper);
        }

        public void removeAllMessage() {
            removeMessages(11);
        }

        public Message getMessage(int what, int arg1, Object obj) {
            return obtainMessage(what, arg1, 0, obj);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 11) {
                IdleMemoryExecutor.this.handleRecycleMsg(msg);
            }
        }
    }

    private static final class MemoryResult {
        long mAvailable;
        boolean mEmergency;
        long mRequest;
        String mScene;
        String mTraceInfo;

        private MemoryResult() {
            this.mRequest = 0;
            this.mAvailable = 0;
            this.mEmergency = false;
            this.mScene = null;
            this.mTraceInfo = null;
        }
    }

    public IdleMemoryExecutor() {
        this.mIdleMemAppExecutor = null;
        this.mBigDataStore = BigDataStore.getInstance();
        this.mSysLowMemStateTracker = null;
        this.mLastExecutorActionFlag = ActionFlag.ACTION_NONE;
        this.mIdleMemAppExecutor = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue(1), new MemThreadFactory("iaware.mem.default"));
        this.mSysCpuOverLoadCnt = 0;
        this.mProtectCacheFlag = 0;
        this.mProtectCacheTimestamp = SystemClock.uptimeMillis();
        this.mSysLowMemStateTracker = new LimitedSizeQueue(3);
        for (int i = 0; i < 3; i++) {
            this.mSysLowMemStateTracker.add(Integer.valueOf(0));
        }
    }

    public void disableMemoryRecover() {
        this.mMemState.setStatus(0);
        if (this.mMemHandler != null) {
            this.mMemHandler.removeAllMessage();
        }
    }

    public void stopMemoryRecover() {
        this.mStopAction.set(true);
        disableMemoryRecover();
    }

    public void executeMemoryRecover(Bundle extras) {
        int event = extras.getInt("event");
        if (this.mMemHandler != null) {
            this.mMemHandler.sendMessage(this.mMemHandler.getMessage(11, event, extras));
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("executeMemoryRecover event=");
            stringBuilder.append(event);
            AwareLog.d(str, stringBuilder.toString());
        }
    }

    public void setMemHandlerThread(HandlerThread handlerThread) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setHandler: object=");
        stringBuilder.append(handlerThread);
        AwareLog.d(str, stringBuilder.toString());
        this.mMemHandler = new MemHandler(handlerThread.getLooper());
    }

    private void executeIdleMemory(int event, Bundle extras) {
        this.mStopAction.set(false);
        MemoryScenePolicy idleMemoryScenePolicy = createPolicyByMemCpu(extras);
        if (idleMemoryScenePolicy == null) {
            this.mMemState.setStatus(0);
            return;
        }
        idleMemoryScenePolicy.setExtras(extras);
        try {
            this.mIdleMemAppExecutor.execute(new CalcRunnable(idleMemoryScenePolicy, false));
        } catch (RejectedExecutionException e) {
            AwareLog.e(TAG, "Failed to execute! reject");
        } catch (Exception e2) {
            AwareLog.e(TAG, "Failed to execute! reset");
        }
    }

    private MemoryScenePolicy createPolicyByMemCpu(Bundle extras) {
        Bundle bundle = extras;
        MemoryScenePolicyList memoryScenePolicyList = MemoryExecutorServer.getInstance().getMemoryScenePolicyList();
        if (memoryScenePolicyList == null || bundle == null) {
            AwareLog.e(TAG, "createPolicyByMemCpu memoryScenePolicyList null");
            return null;
        }
        MemoryResult memResult = calculateMemStatus();
        long beginTime = System.currentTimeMillis();
        updateSysMemStates(memResult.mAvailable);
        if (!isProtectCache() && isContLowMemState()) {
            setProtectCache(1);
        } else if (!isUnprotectCache() && isContNormalMemState() && isProtectLruTimeOk()) {
            setProtectCache(0);
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bybass set protect cache. States: ");
            stringBuilder.append(this.mProtectCacheFlag);
            stringBuilder.append(", ");
            stringBuilder.append(Arrays.toString(this.mSysLowMemStateTracker.toArray()));
            AwareLog.d(str, stringBuilder.toString());
        }
        long exeTime = System.currentTimeMillis() - beginTime;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setProtectCache time ");
        stringBuilder2.append(exeTime);
        AwareLog.d(str2, stringBuilder2.toString());
        updateExtraFreeKbytes(memResult.mAvailable);
        if (memResult.mScene == null) {
            EventTracker.getInstance().trackEvent(1001, 0, 0, memResult.mTraceInfo);
            return null;
        }
        MemoryScenePolicy policy = memoryScenePolicyList.getMemoryScenePolicy(memResult.mScene);
        if (policy == null || !policy.canBeExecuted()) {
            String reason = policy == null ? "null policy" : "policy can not execute";
            String traceInfo = new StringBuilder();
            traceInfo.append(memResult.mTraceInfo);
            traceInfo.append(" but ");
            traceInfo.append(reason);
            EventTracker.getInstance().trackEvent(1001, 0, 0, traceInfo.toString());
            return null;
        }
        if (!memResult.mEmergency) {
            CpuResult cpuResult = calculateCPUStatus(policy, memResult.mAvailable);
            bundle.putInt("cpuLoad", cpuResult.mCpuLoad);
            bundle.putBoolean("cpuBusy", cpuResult.mBusy);
            if (cpuResult.mBusy) {
                EventTracker.getInstance().trackEvent(1001, 0, 0, cpuResult.mTraceInfo);
                this.mSysCpuOverLoadCnt++;
                return null;
            }
        }
        this.mSysCpuOverLoadCnt = 0;
        bundle.putLong("reqMem", memResult.mRequest);
        policy.setExtras(bundle);
        EventTracker.getInstance().trackEvent(1003, 0, 0, memResult.mTraceInfo);
        return policy;
    }

    private void updateExtraFreeKbytes(long availableRam) {
        if (availableRam > 0) {
            if (availableRam >= MemoryConstant.getIdleMemory()) {
                MemoryUtils.writeExtraFreeKbytes(MemoryConstant.DEFAULT_EXTRA_FREE_KBYTES);
            } else {
                MemoryUtils.writeExtraFreeKbytes(MemoryConstant.getConfigExtraFreeKbytes());
            }
        }
    }

    private MemoryResult calculateMemStatus() {
        MemoryResult memResult = new MemoryResult();
        if (canRecoverExecute("calculateMemStatus")) {
            boolean z = true;
            this.mMemState.setStatus(1);
            long availableRam = MemoryReader.getInstance().getMemAvailable();
            String str;
            if (availableRam <= 0) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("calculateMemStatus read availableRam err!");
                stringBuilder.append(availableRam);
                AwareLog.e(str, stringBuilder.toString());
                memResult.mTraceInfo = "read availableRam err";
                return memResult;
            }
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("calculateMemStatus memory availableRam=");
            stringBuilder2.append(availableRam);
            AwareLog.d(str, stringBuilder2.toString());
            memResult.mAvailable = availableRam;
            StringBuilder stringBuilder3;
            if (availableRam >= MemoryConstant.getIdleMemory()) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("memory enough ");
                stringBuilder3.append(availableRam);
                memResult.mTraceInfo = stringBuilder3.toString();
                this.mLastExecutorActionFlag = ActionFlag.ACTION_NONE;
                setBelowThresholdTime();
            } else if (availableRam >= MemoryConstant.getCriticalMemory()) {
                memResult.mRequest = MemoryConstant.getIdleMemory() - availableRam;
                memResult.mScene = MemoryConstant.MEM_SCENE_IDLE;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("memory low ");
                stringBuilder3.append(availableRam);
                memResult.mTraceInfo = stringBuilder3.toString();
                this.mLastExecutorActionFlag = ActionFlag.ACTION_RECLAIM;
                setBelowThresholdTime();
            } else if (availableRam >= MemoryConstant.getCriticalMemory() - MemoryConstant.getReclaimKillGapMemory()) {
                if (this.mLastExecutorActionFlag != ActionFlag.ACTION_KILL) {
                    memResult.mRequest = MemoryConstant.getIdleMemory() - availableRam;
                    memResult.mScene = MemoryConstant.MEM_SCENE_IDLE;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("memory in Idle gap ");
                    stringBuilder3.append(availableRam);
                    memResult.mTraceInfo = stringBuilder3.toString();
                } else {
                    memResult.mRequest = MemoryConstant.getCriticalMemory() - availableRam;
                    memResult.mScene = MemoryConstant.MEM_SCENE_DEFAULT;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("memory in Critical gap ");
                    stringBuilder3.append(availableRam);
                    memResult.mTraceInfo = stringBuilder3.toString();
                }
                this.mLastExecutorActionFlag = ActionFlag.ACTION_KILL;
                setLowMemoryManageCount();
            } else {
                setLowMemoryManageCount();
                this.mLastExecutorActionFlag = ActionFlag.ACTION_KILL;
                memResult.mRequest = MemoryConstant.getCriticalMemory() - availableRam;
                memResult.mScene = MemoryConstant.MEM_SCENE_DEFAULT;
                if (availableRam > MemoryConstant.getEmergencyMemory()) {
                    z = false;
                }
                memResult.mEmergency = z;
                memResult.mTraceInfo = memResult.mEmergency ? "memory emergemcy " : "memory critical ";
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(memResult.mTraceInfo);
                stringBuilder3.append(availableRam);
                memResult.mTraceInfo = stringBuilder3.toString();
            }
            return memResult;
        }
        memResult.mTraceInfo = "DME not running or interrupted";
        return memResult;
    }

    private CpuResult calculateCPUStatus(MemoryScenePolicy policy, long mem) {
        CpuResult cpuResult = new CpuResult();
        boolean z = true;
        if (canRecoverExecute("calculateCPUStatus")) {
            this.mMemState.setStatus(2);
            long cpuLoad = CpuReader.getInstance().getCpuPercent();
            String str;
            if (cpuLoad < 0) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("calculateCPUStatus faild to read cpuload=");
                stringBuilder.append(cpuLoad);
                AwareLog.e(str, stringBuilder.toString());
                cpuResult.mTraceInfo = "read cpuload err";
                cpuResult.mBusy = true;
                return cpuResult;
            }
            cpuResult.mCpuLoad = (int) cpuLoad;
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("calculateCPUStatus cpu pressure ");
            stringBuilder2.append(cpuLoad);
            AwareLog.d(str, stringBuilder2.toString());
            StringBuilder stringBuilder3;
            if (cpuLoad < policy.getCpuPressure(mem, MemoryConstant.getIdleThresHold(), this.mSysCpuOverLoadCnt)) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("cpuload ");
                stringBuilder3.append(cpuLoad);
                cpuResult.mTraceInfo = stringBuilder3.toString();
                cpuResult.mBusy = false;
                return cpuResult;
            }
            if (cpuLoad < policy.getCpuPressure(mem, MemoryConstant.getNormalThresHold(), this.mSysCpuOverLoadCnt)) {
                if (DataInputHandle.getInstance().getActiveStatus() == 2) {
                    z = false;
                }
                cpuResult.mBusy = z;
                if (cpuResult.mBusy) {
                    str = "phone in active state";
                } else {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("cpuload ");
                    stringBuilder3.append(cpuLoad);
                    str = stringBuilder3.toString();
                }
                cpuResult.mTraceInfo = str;
            } else {
                cpuResult.mBusy = true;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("cpuload high ");
                stringBuilder3.append(cpuLoad);
                cpuResult.mTraceInfo = stringBuilder3.toString();
            }
            return cpuResult;
        }
        cpuResult.mTraceInfo = "DME not running or interrupted";
        cpuResult.mBusy = true;
        return cpuResult;
    }

    private boolean canRecoverExecute(String functionName) {
        if (!this.mStopAction.get()) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(functionName);
        stringBuilder.append(" iaware not running, action=");
        stringBuilder.append(this.mStopAction.get());
        AwareLog.w(str, stringBuilder.toString());
        return false;
    }

    private void handleRecycleMsg(Message msg) {
        int event = msg.arg1;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MemHandler event=");
        stringBuilder.append(event);
        AwareLog.d(str, stringBuilder.toString());
        Bundle extras = msg.obj;
        if (extras == null) {
            AwareLog.d(TAG, "MemHandler extras is null");
            return;
        }
        long timestamp = extras.getLong("timeStamp");
        if (this.mMemState.getStatus() != 0) {
            EventTracker instance = EventTracker.getInstance();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mem ");
            stringBuilder2.append(this.mMemState.toString());
            instance.trackEvent(1001, event, timestamp, stringBuilder2.toString());
        } else if (MemoryExecutorServer.getInstance().getBigMemAppLaunching()) {
            AwareLog.d(TAG, "MemHandler big memory app is running");
        } else {
            if (event > 0 && timestamp > 0) {
                EventTracker.getInstance().trackEvent(1000, event, timestamp, null);
            }
            executeIdleMemory(event, extras);
        }
    }

    private void setBelowThresholdTime() {
        if (AwareConstant.CURRENT_USER_TYPE == 3) {
            long belowThresholdTimeEnd = SystemClock.elapsedRealtime();
            if (this.mBigDataStore.belowThresholdTimeBegin > 0 && belowThresholdTimeEnd - this.mBigDataStore.belowThresholdTimeBegin > 0) {
                BigDataStore bigDataStore = this.mBigDataStore;
                bigDataStore.belowThresholdTime += belowThresholdTimeEnd - this.mBigDataStore.belowThresholdTimeBegin;
                this.mBigDataStore.belowThresholdTimeBegin = 0;
            }
        }
    }

    private void setLowMemoryManageCount() {
        if (AwareConstant.CURRENT_USER_TYPE == 3) {
            if (this.mBigDataStore.belowThresholdTimeBegin == 0) {
                this.mBigDataStore.belowThresholdTimeBegin = SystemClock.elapsedRealtime();
            }
            BigDataStore bigDataStore = this.mBigDataStore;
            bigDataStore.lowMemoryManageCount++;
        }
    }

    private void updateSysMemStates(long memAvailKB) {
        if (memAvailKB >= 0) {
            int state;
            if (memAvailKB < MemoryConstant.getCriticalMemory()) {
                state = 1;
            } else {
                state = 0;
            }
            this.mSysLowMemStateTracker.add(Integer.valueOf(state));
        }
    }

    private boolean isContLowMemState() {
        for (int i = 0; i < 3; i++) {
            if (((Integer) this.mSysLowMemStateTracker.get(i)).intValue() != 1) {
                return false;
            }
        }
        return true;
    }

    private boolean isContNormalMemState() {
        for (int i = 0; i < 3; i++) {
            if (((Integer) this.mSysLowMemStateTracker.get(i)).intValue() != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isProtectCache() {
        return this.mProtectCacheFlag == 1;
    }

    private boolean isUnprotectCache() {
        return this.mProtectCacheFlag == 0;
    }

    public void setProtectCacheFlag(int state) {
        this.mProtectCacheFlag = state;
    }

    private void setProtectCache(int state) {
        String str;
        StringBuilder stringBuilder;
        if (state == 1 || state == 0) {
            this.mProtectCacheFlag = state;
            this.mProtectCacheTimestamp = SystemClock.uptimeMillis();
            MemoryUtils.dynamicSetProtectLru(state);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setProtectCache state: ");
            stringBuilder.append(state);
            stringBuilder.append(". States: ");
            stringBuilder.append(this.mProtectCacheFlag);
            stringBuilder.append(", ");
            stringBuilder.append(Arrays.toString(this.mSysLowMemStateTracker.toArray()));
            AwareLog.d(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setProtectCache invalid state: ");
        stringBuilder.append(state);
        stringBuilder.append(". States: ");
        stringBuilder.append(this.mProtectCacheFlag);
        stringBuilder.append(", ");
        stringBuilder.append(Arrays.toString(this.mSysLowMemStateTracker.toArray()));
        AwareLog.d(str, stringBuilder.toString());
    }

    private boolean isProtectLruTimeOk() {
        long now = SystemClock.uptimeMillis();
        boolean ret = now > this.mProtectCacheTimestamp && now - this.mProtectCacheTimestamp >= AwareAppMngSort.PREVIOUS_APP_DIRCACTIVITY_DECAYTIME;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isProtectLruTimeOk: ");
        stringBuilder.append(ret);
        stringBuilder.append(". States: ");
        stringBuilder.append(this.mProtectCacheFlag);
        stringBuilder.append(", ");
        stringBuilder.append(Arrays.toString(this.mSysLowMemStateTracker.toArray()));
        AwareLog.d(str, stringBuilder.toString());
        return ret;
    }
}
