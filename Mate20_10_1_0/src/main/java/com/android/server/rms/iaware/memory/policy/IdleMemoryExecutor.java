package com.android.server.rms.iaware.memory.policy;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.memory.policy.AbsMemoryExecutor;
import com.android.server.rms.iaware.memory.utils.BigDataStore;
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
    private static final long THREAD_POOL_ALIVE_TIME = 30;
    private static final String UNIT_KB = "KB";
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

    public IdleMemoryExecutor() {
        this.mIdleMemAppExecutor = null;
        this.mBigDataStore = BigDataStore.getInstance();
        this.mSysLowMemStateTracker = null;
        this.mLastExecutorActionFlag = ActionFlag.ACTION_NONE;
        this.mIdleMemAppExecutor = new ThreadPoolExecutor(0, 1, (long) THREAD_POOL_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue(1), new MemThreadFactory("iaware.mem.default"));
        this.mProtectCacheFlag = 0;
        this.mProtectCacheTimestamp = SystemClock.uptimeMillis();
        this.mSysLowMemStateTracker = new LimitedSizeQueue<>(3);
        for (int i = 0; i < 3; i++) {
            this.mSysLowMemStateTracker.add(0);
        }
    }

    @Override // com.android.server.rms.iaware.memory.policy.AbsMemoryExecutor
    public void disableMemoryRecover() {
        this.mMemState.setStatus(0);
        MemHandler memHandler = this.mMemHandler;
        if (memHandler != null) {
            memHandler.removeAllMessage();
        }
    }

    @Override // com.android.server.rms.iaware.memory.policy.AbsMemoryExecutor
    public void stopMemoryRecover() {
        this.mStopAction.set(true);
        disableMemoryRecover();
    }

    @Override // com.android.server.rms.iaware.memory.policy.AbsMemoryExecutor
    public void executeMemoryRecover(Bundle extras) {
        int event = extras.getInt("event");
        MemHandler memHandler = this.mMemHandler;
        if (memHandler != null) {
            memHandler.sendMessage(memHandler.getMessage(11, event, extras));
            AwareLog.d(TAG, "executeMemoryRecover event=" + event);
        }
    }

    @Override // com.android.server.rms.iaware.memory.policy.AbsMemoryExecutor
    public void setMemHandlerThread(HandlerThread handlerThread) {
        AwareLog.d(TAG, "setHandler: object=" + handlerThread);
        this.mMemHandler = new MemHandler(handlerThread.getLooper());
    }

    private void executeIdleMemory(int event, Bundle extras) {
        this.mStopAction.set(false);
        MemoryScenePolicy idleMemoryScenePolicy = createPolicyByMemCpu(extras);
        if (idleMemoryScenePolicy == null) {
            this.mMemState.setStatus(0);
            return;
        }
        try {
            this.mIdleMemAppExecutor.execute(new CalcRunnable(idleMemoryScenePolicy, false));
        } catch (RejectedExecutionException e) {
            AwareLog.e(TAG, "Failed to execute! reject");
        } catch (Exception e2) {
            AwareLog.e(TAG, "Failed to execute! reset");
        }
    }

    private MemoryScenePolicy createPolicyByMemCpu(Bundle extras) {
        MemoryScenePolicyList memoryScenePolicyList = MemoryExecutorServer.getInstance().getMemoryScenePolicyList();
        if (memoryScenePolicyList == null || extras == null) {
            AwareLog.w(TAG, "createPolicyByMemCpu memoryScenePolicyList null");
            return null;
        }
        boolean immediated = extras.getBoolean("immediate", false);
        MemoryResult memResult = calculateMemStatus(immediated);
        long beginTime = System.currentTimeMillis();
        updateSysMemStates(memResult.mAvailable);
        if (!isProtectCache() && isContLowMemState()) {
            setProtectCache(1);
        } else if (isUnprotectCache() || !isContNormalMemState() || !isProtectLruTimeOk()) {
            AwareLog.d(TAG, "Bybass set protect cache. States: " + this.mProtectCacheFlag + ", " + Arrays.toString(this.mSysLowMemStateTracker.toArray()));
        } else {
            setProtectCache(0);
        }
        AwareLog.d(TAG, "setProtectCache time " + (System.currentTimeMillis() - beginTime));
        updateExtraFreeKbytes(memResult.mAvailable);
        if (memResult.mScene == null) {
            EventTracker.getInstance().trackEvent(1001, 0, 0, memResult.mTraceInfo);
            return null;
        }
        MemoryScenePolicy policy = memoryScenePolicyList.getMemoryScenePolicy(memResult.mScene);
        if (policy == null || (!immediated && !policy.canBeExecuted())) {
            String reason = policy == null ? "null policy" : "policy can not execute";
            EventTracker.getInstance().trackEvent(1001, 0, 0, memResult.mTraceInfo + " but " + reason);
            return null;
        }
        extras.putBoolean("emergency", memResult.mIsEmergent);
        extras.putLong("available", memResult.mAvailable);
        extras.putLong("reqMem", memResult.mRequest);
        policy.setExtras(extras);
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

    private MemoryResult calculateMemStatus(boolean immediated) {
        MemoryResult memResult = new MemoryResult();
        if (!canRecoverExecute("calculateMemStatus")) {
            memResult.mTraceInfo = "DME not running or interrupted";
            return memResult;
        }
        this.mMemState.setStatus(1);
        long availableRam = MemoryReader.getInstance().getMemAvailable();
        if (availableRam <= 0) {
            AwareLog.e(TAG, "calculateMemStatus read availableRam err!" + availableRam);
            memResult.mTraceInfo = "read availableRam err";
            return memResult;
        }
        AwareLog.d(TAG, "calculateMemStatus memory availableRam=" + availableRam);
        memResult.mAvailable = availableRam;
        updateMemStatus(memResult, availableRam, immediated);
        return memResult;
    }

    private void updateMemStatus(MemoryResult memResult, long availableRam, boolean immediated) {
        boolean z = true;
        if (immediated) {
            this.mLastExecutorActionFlag = ActionFlag.ACTION_KILL;
            memResult.mRequest = MemoryConstant.getCriticalMemory() - availableRam;
            memResult.mScene = MemoryConstant.MEM_SCENE_DEFAULT;
            memResult.mIsEmergent = true;
            memResult.mTraceInfo = "app emergency ";
            memResult.mTraceInfo += availableRam;
        } else if (availableRam >= MemoryConstant.getIdleMemory()) {
            memResult.mTraceInfo = "memory enough " + availableRam + UNIT_KB;
            this.mLastExecutorActionFlag = ActionFlag.ACTION_NONE;
            setBelowThresholdTime();
        } else if (availableRam >= MemoryConstant.getCriticalMemory()) {
            memResult.mRequest = MemoryConstant.getIdleMemory() - availableRam;
            memResult.mScene = MemoryConstant.MEM_SCENE_IDLE;
            memResult.mTraceInfo = "memory idle " + availableRam + UNIT_KB;
            this.mLastExecutorActionFlag = ActionFlag.ACTION_RECLAIM;
            setBelowThresholdTime();
        } else if (availableRam >= MemoryConstant.getCriticalMemory() - MemoryConstant.getReclaimKillGapMemory()) {
            boolean canKernelCompress = MemoryReader.canKernelCompress();
            boolean isBigMemCriticalMemory = MemoryConstant.isBigMemCriticalMemory();
            if (!MemoryConstant.isKernCompressEnable() || !canKernelCompress || isBigMemCriticalMemory) {
                memResult.mRequest = MemoryConstant.getCriticalMemory() - availableRam;
                memResult.mScene = MemoryConstant.MEM_SCENE_DEFAULT;
                memResult.mTraceInfo = "memory in critical gap " + availableRam + UNIT_KB;
                this.mLastExecutorActionFlag = ActionFlag.ACTION_KILL;
            } else {
                memResult.mRequest = MemoryConstant.getIdleMemory() - availableRam;
                memResult.mScene = MemoryConstant.MEM_SCENE_IDLE;
                memResult.mTraceInfo = "memory in idle gap " + availableRam + UNIT_KB;
                this.mLastExecutorActionFlag = ActionFlag.ACTION_RECLAIM;
                MemoryUtils.rccCompress(MemoryConstant.getCriticalMemory() - availableRam);
            }
            setLowMemoryManageCount();
        } else {
            setLowMemoryManageCount();
            this.mLastExecutorActionFlag = ActionFlag.ACTION_KILL;
            memResult.mRequest = MemoryConstant.getCriticalMemory() - availableRam;
            memResult.mScene = MemoryConstant.MEM_SCENE_DEFAULT;
            if (availableRam > MemoryConstant.getEmergencyMemory()) {
                z = false;
            }
            memResult.mIsEmergent = z;
            memResult.mTraceInfo = memResult.mIsEmergent ? "memory emergency " : "memory critical ";
            memResult.mTraceInfo += availableRam + UNIT_KB;
        }
    }

    private static final class MemoryResult {
        long mAvailable;
        boolean mIsEmergent;
        long mRequest;
        String mScene;
        String mTraceInfo;

        private MemoryResult() {
            this.mRequest = 0;
            this.mAvailable = 0;
            this.mIsEmergent = false;
            this.mScene = null;
            this.mTraceInfo = null;
        }
    }

    private final class MemHandler extends Handler {
        MemHandler(Looper looper) {
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

    /* access modifiers changed from: private */
    public void handleRecycleMsg(Message msg) {
        int event = msg.arg1;
        AwareLog.d(TAG, "MemHandler event=" + event);
        if (msg.obj instanceof Bundle) {
            Bundle extras = (Bundle) msg.obj;
            long timestamp = extras.getLong("timeStamp");
            if (this.mMemState.getStatus() != 0) {
                EventTracker instance = EventTracker.getInstance();
                instance.trackEvent(1001, event, timestamp, "mem " + this.mMemState.toString());
            } else if (MemoryExecutorServer.getInstance().getBigMemAppLaunching()) {
                AwareLog.d(TAG, "MemHandler big memory app is running");
            } else {
                if (event > 0 && timestamp > 0) {
                    EventTracker.getInstance().trackEvent(1000, event, timestamp, null);
                }
                executeIdleMemory(event, extras);
            }
        } else {
            AwareLog.d(TAG, "MemHandler extras is null");
        }
    }

    private void setBelowThresholdTime() {
        if (AwareConstant.CURRENT_USER_TYPE == 3) {
            long belowThresholdTimeEnd = SystemClock.elapsedRealtime();
            if (this.mBigDataStore.belowThresholdTimeBegin > 0 && belowThresholdTimeEnd - this.mBigDataStore.belowThresholdTimeBegin > 0) {
                this.mBigDataStore.belowThresholdTime += belowThresholdTimeEnd - this.mBigDataStore.belowThresholdTimeBegin;
                this.mBigDataStore.belowThresholdTimeBegin = 0;
            }
        }
    }

    private void setLowMemoryManageCount() {
        if (AwareConstant.CURRENT_USER_TYPE == 3) {
            if (this.mBigDataStore.belowThresholdTimeBegin == 0) {
                this.mBigDataStore.belowThresholdTimeBegin = SystemClock.elapsedRealtime();
            }
            this.mBigDataStore.lowMemoryManageCount++;
        }
    }

    private static final class LimitedSizeQueue<K> extends LinkedList<K> {
        private static final long serialVersionUID = 6928859904407185256L;
        private int maxSize;

        LimitedSizeQueue(int size) {
            this.maxSize = size;
        }

        @Override // java.util.AbstractCollection, java.util.List, java.util.Collection, java.util.AbstractList, java.util.Queue, java.util.LinkedList, java.util.Deque
        public boolean add(K k) {
            boolean added = super.add(k);
            while (added && size() > this.maxSize) {
                super.remove();
            }
            return added;
        }
    }

    private void updateSysMemStates(long memAvailKb) {
        int state;
        if (memAvailKb >= 0) {
            if (memAvailKb < MemoryConstant.getCriticalMemory()) {
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

    @Override // com.android.server.rms.iaware.memory.policy.AbsMemoryExecutor
    public int getProtectCacheFlag() {
        return this.mProtectCacheFlag;
    }

    @Override // com.android.server.rms.iaware.memory.policy.AbsMemoryExecutor
    public void setProtectCacheFlag(int state) {
        this.mProtectCacheFlag = state;
    }

    private void setProtectCache(int state) {
        if (state == 1 || state == 0) {
            this.mProtectCacheFlag = state;
            this.mProtectCacheTimestamp = SystemClock.uptimeMillis();
            MemoryUtils.dynamicSetProtectLru(state);
            AwareLog.d(TAG, "setProtectCache state: " + state + ". States: " + this.mProtectCacheFlag + ", " + Arrays.toString(this.mSysLowMemStateTracker.toArray()));
            return;
        }
        AwareLog.d(TAG, "setProtectCache invalid state: " + state + ". States: " + this.mProtectCacheFlag + ", " + Arrays.toString(this.mSysLowMemStateTracker.toArray()));
    }

    private boolean isProtectLruTimeOk() {
        long now = SystemClock.uptimeMillis();
        long j = this.mProtectCacheTimestamp;
        boolean ret = now > j && now - j >= 600000;
        AwareLog.d(TAG, "isProtectLruTimeOk: " + ret + ". States: " + this.mProtectCacheFlag + ", " + Arrays.toString(this.mSysLowMemStateTracker.toArray()));
        return ret;
    }
}
