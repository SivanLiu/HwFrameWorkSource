package com.android.server.rms.memrepair;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IMWThirdpartyCallback;
import android.os.Message;
import android.os.SystemClock;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.server.hidata.appqoe.HwAPPQoEUserAction;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.rms.iaware.memory.data.handle.DataAppHandle;
import com.android.server.rms.iaware.memory.policy.AbsMemoryExecutor;
import com.android.server.rms.iaware.memory.policy.SystemTrimPolicy;
import com.huawei.android.app.HwActivityTaskManager;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SystemAppMemRepairMng {
    private static final long KEEP_ALIVE_TIME = 30;
    private static final long MAX_EXCUTE_INTERVAL = 1440;
    private static final long MAX_THRESHOLD = 1048576;
    private static final long MILLISECOND_OF_MINUTE = 60000;
    private static final long MIN_EXCUTE_INTERVAL = 1;
    private static final long MIN_THRESHOLD = 0;
    private static final int MSG_LOW_MEMORY = 103;
    private static final int MSG_MID_NIGHT = 101;
    private static final int MSG_SCREEN_OFF = 102;
    private static final int MSG_SYSTEM_MAMANGER_CLEAN = 104;
    public static final int POLICY_KILL = 1;
    public static final int POLICY_TRIM = 0;
    private static final int SCENE_TYPE_LOW_MEMORY = 4;
    private static final int SCENE_TYPE_MID_NIGHT = 1;
    private static final int SCENE_TYPE_MIN = 1;
    private static final int SCENE_TYPE_SCREEN_OFF = 2;
    private static final int SCENE_TYPE_SYSTEM_MANAGER_CLEAN = 8;
    private static final String TAG = "SystemAppMemRepairMng";
    private static SystemAppMemRepairMng mInstance = null;
    /* access modifiers changed from: private */
    public static long mLastExecuteTime = 0;
    private static final Object mLock = new Object();
    /* access modifiers changed from: private */
    public final Map<MemRepairScene, SystemAppMemRepairDefaultAction> mActions;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public final AtomicBoolean mEnable;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public final AtomicBoolean mInterrupted;
    /* access modifiers changed from: private */
    public long mInterval;
    /* access modifiers changed from: private */
    public AtomicBoolean mIsMultiWindow;
    /* access modifiers changed from: private */
    public ThreadPoolExecutor mMemPrepairExecutor;
    private SysMemCallBackHandler mSysMemCallBackHandler;

    public enum Policy {
        TRIM("on-trim"),
        KILL("kill-9");
        
        private String mDescription;

        private Policy(String description) {
            this.mDescription = description;
        }

        public String description() {
            return this.mDescription;
        }
    }

    public enum MemRepairScene {
        NONE(HwAPPQoEUserAction.DEFAULT_CHIP_TYPE),
        MID_NIGHT("mid-night"),
        SCREEN_OFF("screen-off"),
        LOW_MEMORY("low-memory"),
        SYSTEM_MANAGER_CLEAN("system_manager_clean");
        
        private String mDescription;

        private MemRepairScene(String description) {
            this.mDescription = description;
        }

        public String description() {
            return this.mDescription;
        }
    }

    private SystemAppMemRepairMng() {
        this.mActions = new ArrayMap();
        this.mEnable = new AtomicBoolean(false);
        this.mInterrupted = new AtomicBoolean(false);
        this.mInterval = 10;
        this.mIsMultiWindow = new AtomicBoolean(false);
        this.mHandler = null;
        this.mContext = null;
        this.mMemPrepairExecutor = null;
        this.mSysMemCallBackHandler = new SysMemCallBackHandler(this, null);
        this.mHandler = new SystemMemRepairHandler();
        this.mMemPrepairExecutor = new ThreadPoolExecutor(0, 1, (long) KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue(1), new AbsMemoryExecutor.MemThreadFactory("iaware.mem.prepair"));
        synchronized (this.mActions) {
            this.mActions.put(MemRepairScene.LOW_MEMORY, new SystemAppMemRepairDefaultAction(MemRepairScene.LOW_MEMORY.description()));
            this.mActions.put(MemRepairScene.MID_NIGHT, new SystemAppMemRepairDefaultAction(MemRepairScene.MID_NIGHT.description()));
            this.mActions.put(MemRepairScene.SCREEN_OFF, new SystemAppMemRepairDefaultAction(MemRepairScene.SCREEN_OFF.description()));
            this.mActions.put(MemRepairScene.SYSTEM_MANAGER_CLEAN, new SystemAppMemRepairDefaultAction(MemRepairScene.SYSTEM_MANAGER_CLEAN.description()));
        }
    }

    public static SystemAppMemRepairMng getInstance() {
        SystemAppMemRepairMng systemAppMemRepairMng;
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new SystemAppMemRepairMng();
            }
            systemAppMemRepairMng = mInstance;
        }
        return systemAppMemRepairMng;
    }

    public void enable() {
        if (!this.mEnable.get()) {
            SystemTrimPolicy.getInstance().enable();
            MultiTaskManagerService mMtmService = MultiTaskManagerService.self();
            if (mMtmService != null) {
                this.mContext = mMtmService.context();
            }
            if (this.mContext != null) {
                this.mEnable.set(true);
            } else {
                AwareLog.w(TAG, "Get MtmServices failed, set feature disable");
            }
            HwActivityTaskManager.registerThirdPartyCallBack(this.mSysMemCallBackHandler);
        }
    }

    public void disable() {
        if (this.mEnable.get()) {
            SystemTrimPolicy.getInstance().disable();
            this.mEnable.set(false);
            HwActivityTaskManager.unregisterThirdPartyCallBack(this.mSysMemCallBackHandler);
        }
    }

    public void setTriggerInterval(long interval) {
        if (interval <= MAX_EXCUTE_INTERVAL && interval >= 1) {
            this.mInterval = interval;
            AwareLog.d(TAG, "mInterval is : " + this.mInterval + "min");
        }
    }

    public void interrupt(boolean interrupt) {
        this.mInterrupted.set(interrupt);
    }

    public void configSystemAppThreshold(String packageName, long threshold, int policy, int scene) {
        if (packageName == null || policy < 0) {
            AwareLog.i(TAG, "packageName or policy is invalid");
        } else if (0 >= threshold || 1048576 < threshold) {
            AwareLog.i(TAG, "threshold is invalid");
        } else if (policy == Policy.TRIM.ordinal()) {
            configTrimPolicyThreshold(packageName, threshold);
        } else if (policy == Policy.KILL.ordinal()) {
            configKillPolicyThreshold(packageName, threshold, policy, scene);
        } else {
            AwareLog.i(TAG, "policy is invalid");
        }
    }

    public void reportData(int eventId) {
        Message msg = this.mHandler.obtainMessage();
        msg.what = -1;
        if (eventId == 20011) {
            this.mInterrupted.set(true);
        } else if (eventId == 20027) {
            msg.what = 101;
        } else if (eventId == 20029) {
            msg.what = 103;
        } else if (eventId == 20031) {
            msg.what = 104;
        } else if (eventId == 90011) {
            msg.what = 102;
            this.mInterrupted.set(true);
        }
        if (msg.what != -1) {
            this.mHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void resetinterrupt() {
        if (this.mInterrupted.get()) {
            interrupt(false);
        }
    }

    private void configTrimPolicyThreshold(String packageName, long threshold) {
        SystemTrimPolicy.getInstance().updateProcThreshold(packageName, threshold);
    }

    private void configKillPolicyThreshold(String packageName, long threshold, int policy, int sceneValue) {
        if (sceneValue < 0) {
            AwareLog.i(TAG, "scene is invalid");
            return;
        }
        AwareLog.d(TAG, "configKillPolicyThreshold:packageName=" + packageName + ",threshold=" + threshold + ",policy=" + policy + ",sceneValue=" + sceneValue);
        synchronized (this.mActions) {
            if ((sceneValue & 1) != 0) {
                try {
                    this.mActions.get(MemRepairScene.MID_NIGHT).updateThreshold(packageName, threshold);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((sceneValue & 2) != 0) {
                this.mActions.get(MemRepairScene.SCREEN_OFF).updateThreshold(packageName, threshold);
            }
            if ((sceneValue & 4) != 0) {
                this.mActions.get(MemRepairScene.LOW_MEMORY).updateThreshold(packageName, threshold);
            }
            if ((sceneValue & 8) != 0) {
                this.mActions.get(MemRepairScene.SYSTEM_MANAGER_CLEAN).updateThreshold(packageName, threshold);
            }
        }
    }

    private class SystemMemRepairHandler extends Handler {
        SystemMemRepairHandler() {
        }

        public void handleMessage(Message msg) {
            long interval = SystemClock.elapsedRealtime() - SystemAppMemRepairMng.mLastExecuteTime;
            AwareLog.i(SystemAppMemRepairMng.TAG, "SysMemMng ReportData Msg.what = " + msg.what);
            if (SystemAppMemRepairMng.mLastExecuteTime != 0 && interval < SystemAppMemRepairMng.this.mInterval * 60000) {
                AwareLog.d(SystemAppMemRepairMng.TAG, "last excute was at " + SystemAppMemRepairMng.mLastExecuteTime + " ms  currentInterval is " + interval + " ms  ConfigInterval is " + SystemAppMemRepairMng.this.mInterval + " mins ");
            } else if (SystemAppMemRepairMng.this.mIsMultiWindow.get()) {
                AwareLog.d(SystemAppMemRepairMng.TAG, "trigger abort cause in multiWindow mode");
            } else {
                MemRepairScene scene = null;
                switch (msg.what) {
                    case 101:
                        scene = MemRepairScene.MID_NIGHT;
                        break;
                    case 102:
                        scene = MemRepairScene.SCREEN_OFF;
                        break;
                    case 103:
                        scene = MemRepairScene.LOW_MEMORY;
                        break;
                    case 104:
                        scene = MemRepairScene.SYSTEM_MANAGER_CLEAN;
                        break;
                }
                if (scene != null) {
                    AwareLog.i(SystemAppMemRepairMng.TAG, "handleMessage now,scene = " + scene);
                    SystemAppMemRepairMng.this.resetinterrupt();
                    SystemAppMemRepairMng.this.mMemPrepairExecutor.execute(new MemRepairRunnable(scene));
                    long unused = SystemAppMemRepairMng.mLastExecuteTime = SystemClock.elapsedRealtime();
                }
            }
        }
    }

    private final class MemRepairRunnable implements Runnable {
        private MemRepairScene mScene;

        MemRepairRunnable(MemRepairScene scene) {
            this.mScene = scene;
        }

        public void run() {
            SystemAppMemRepairDefaultAction action;
            Bundle extras = DataAppHandle.getInstance().createBundleFromAppInfo();
            synchronized (SystemAppMemRepairMng.this.mActions) {
                action = (SystemAppMemRepairDefaultAction) SystemAppMemRepairMng.this.mActions.get(this.mScene);
            }
            if (SystemAppMemRepairMng.this.mEnable.get()) {
                if (action != null) {
                    action.excute(SystemAppMemRepairMng.this.mContext, extras, SystemAppMemRepairMng.this.mInterrupted);
                }
                if (AnonymousClass1.$SwitchMap$com$android$server$rms$memrepair$SystemAppMemRepairMng$MemRepairScene[this.mScene.ordinal()] == 1) {
                    NativeAppMemRepair.getInstance().doMemRepair(SystemAppMemRepairMng.this.mInterrupted);
                }
                SystemAppMemRepairMng.this.resetinterrupt();
                return;
            }
            AwareLog.i(SystemAppMemRepairMng.TAG, "System app Mem prepair feature is disable");
        }
    }

    /* renamed from: com.android.server.rms.memrepair.SystemAppMemRepairMng$1  reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$rms$memrepair$SystemAppMemRepairMng$MemRepairScene = new int[MemRepairScene.values().length];

        static {
            try {
                $SwitchMap$com$android$server$rms$memrepair$SystemAppMemRepairMng$MemRepairScene[MemRepairScene.MID_NIGHT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    private class SysMemCallBackHandler extends IMWThirdpartyCallback.Stub {
        private SysMemCallBackHandler() {
        }

        /* synthetic */ SysMemCallBackHandler(SystemAppMemRepairMng x0, AnonymousClass1 x1) {
            this();
        }

        public void onModeChanged(boolean aMWStatus) {
            SystemAppMemRepairMng.this.mIsMultiWindow.set(aMWStatus);
            AwareLog.d(SystemAppMemRepairMng.TAG, "MultiWindowMode is " + SystemAppMemRepairMng.this.mIsMultiWindow.get());
        }

        public void onZoneChanged() {
        }

        public void onSizeChanged() {
        }
    }
}
