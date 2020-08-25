package com.android.server.rms.iaware.cpu;

import android.iawareperf.UniPerf;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.cpu.CPUFeature;
import java.util.concurrent.atomic.AtomicBoolean;

public class CPUAppStartOnFire {
    private static final Object LOCK = new Object();
    private static final int RESET_ON_FIRE_DELAYED = 10000;
    private static final String TAG = "CPUAppStartOnFire";
    private static CPUAppStartOnFire sInstance;
    private CPUFeature.CPUFeatureHandler mCpuFeatureHandler;
    private AtomicBoolean mIsFeatureEnable = new AtomicBoolean(false);

    private CPUAppStartOnFire() {
    }

    public static CPUAppStartOnFire getInstance() {
        CPUAppStartOnFire cPUAppStartOnFire;
        synchronized (LOCK) {
            if (sInstance == null) {
                sInstance = new CPUAppStartOnFire();
            }
            cPUAppStartOnFire = sInstance;
        }
        return cPUAppStartOnFire;
    }

    public void enable(CPUFeature.CPUFeatureHandler handler) {
        if (this.mIsFeatureEnable.get()) {
            AwareLog.e(TAG, "CPUAppStartOnFire has already enable!");
            return;
        }
        this.mCpuFeatureHandler = handler;
        this.mIsFeatureEnable.set(true);
    }

    public void disable() {
        if (!this.mIsFeatureEnable.get()) {
            AwareLog.e(TAG, "CPUAppStartOnFire has already disable!");
        } else {
            this.mIsFeatureEnable.set(false);
        }
    }

    public void setOnFire() {
        if (this.mIsFeatureEnable.get() && this.mCpuFeatureHandler != null) {
            UniPerf.getInstance().uniPerfEvent(4121, "", new int[]{0});
            this.mCpuFeatureHandler.removeMessages(CPUFeature.MSG_RESET_ON_FIRE);
            this.mCpuFeatureHandler.sendEmptyMessageDelayed(CPUFeature.MSG_RESET_ON_FIRE, 10000);
        }
    }

    public void resetOnFire() {
        UniPerf.getInstance().uniPerfEvent(4121, "", new int[]{-1});
    }
}
