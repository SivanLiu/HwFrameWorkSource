package com.android.server.rms.iaware.cpu;

import android.iawareperf.UniPerf;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.cpu.CPUFeature.CPUFeatureHandler;
import java.util.concurrent.atomic.AtomicBoolean;

class CPUAppStartOnFire {
    private static final int DEFAULT_ONFIRE_DURATION = 10000;
    private static final int MAX_ONFIRE_DURATION = 10000;
    private static final int MIN_ONFIRE_DURATION = 10;
    private static final String TAG = "CPUAppStartOnFire";
    private static CPUAppStartOnFire sInstance;
    private static final Object syncObject = new Object();
    private CPUFeatureHandler mCPUFeatureHandler;
    private AtomicBoolean mIsFeatureEnable;
    private int resetOnFireDelayed;

    private CPUAppStartOnFire() {
        this.mIsFeatureEnable = new AtomicBoolean(false);
        this.resetOnFireDelayed = 10000;
        this.resetOnFireDelayed = new CPUOnFireConfig().getOnFireDuration();
        if (this.resetOnFireDelayed > 10000 || this.resetOnFireDelayed < 10) {
            AwareLog.e(TAG, "set reset on fire delay error! use default value");
            this.resetOnFireDelayed = 10000;
        }
    }

    public static CPUAppStartOnFire getInstance() {
        CPUAppStartOnFire cPUAppStartOnFire;
        synchronized (syncObject) {
            if (sInstance == null) {
                sInstance = new CPUAppStartOnFire();
            }
            cPUAppStartOnFire = sInstance;
        }
        return cPUAppStartOnFire;
    }

    public void enable(CPUFeatureHandler handler) {
        if (this.mIsFeatureEnable.get()) {
            AwareLog.e(TAG, "CPUAppStartOnFire has already enable!");
            return;
        }
        this.mCPUFeatureHandler = handler;
        this.mIsFeatureEnable.set(true);
    }

    public void disable() {
        if (this.mIsFeatureEnable.get()) {
            this.mIsFeatureEnable.set(false);
        } else {
            AwareLog.e(TAG, "CPUAppStartOnFire has already disable!");
        }
    }

    public void setOnFire() {
        if (this.mIsFeatureEnable.get() && this.mCPUFeatureHandler != null) {
            UniPerf.getInstance().uniPerfEvent(4121, "", new int[]{0});
            this.mCPUFeatureHandler.removeMessages(CPUFeature.MSG_RESET_ON_FIRE);
            this.mCPUFeatureHandler.sendEmptyMessageDelayed(CPUFeature.MSG_RESET_ON_FIRE, (long) this.resetOnFireDelayed);
        }
    }

    public void resetOnFire() {
        UniPerf.getInstance().uniPerfEvent(4121, "", new int[]{-1});
    }
}
