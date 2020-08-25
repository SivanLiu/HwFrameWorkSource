package com.android.server.rms.iaware.cpu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.provider.Settings;
import android.rms.iaware.AwareLog;
import com.android.server.gesture.GestureNavConst;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import java.util.concurrent.atomic.AtomicBoolean;

class CPUPowerMode {
    private static final String ACTION_ENTER_SUPER_SAVE_MODE = "huawei.intent.action.HWSYSTEMMANAGER_CHANGE_POWERMODE";
    private static final String ACTION_POWER_MODE_CHANGE = "huawei.intent.action.POWER_MODE_CHANGED_ACTION";
    private static final String ACTION_QUIT_SUPER_SAVE_MODE = "huawei.intent.action.HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE";
    private static final int EXTRA_NONSAVE_STATE = 2;
    private static final int EXTRA_PERFORMANCE_STATE = 3;
    private static final int EXTRA_SAVE_STATE = 1;
    private static final int NON_SAVE_POWER_MODE = 1;
    private static final int PERFORMANCE_POWER_MODE = 3;
    private static final int SAVE_POWER_MODE = 2;
    private static final int SETTING_NONSAVE_POWER_MODE = 1;
    private static final int SETTING_SAVE_POWER_MODE = 4;
    private static final Object SLOCK = new Object();
    private static final int SUPER_SAVE_POWER_MODE = 4;
    private static final String SYSTEM_MANAGER_PERMISSION = "com.huawei.systemmanager.permission.ACCESS_INTERFACE";
    private static final String TAG = "CPUPowerMode";
    private static CPUPowerMode sInstance;
    /* access modifiers changed from: private */
    public static AtomicBoolean sIsFeatureEnable = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public static AtomicBoolean sIsSaveMode = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public static AtomicBoolean sIsSuperSaveMode = new AtomicBoolean(false);
    private Context mContext;
    /* access modifiers changed from: private */
    public int mPowerMode = 1;
    private PowerStateChangeReceiver mPowerStateChangeReceiver;
    /* access modifiers changed from: private */
    public int mTempPowerMode = 1;

    public void enable(Context context) {
        if (sIsFeatureEnable.get()) {
            AwareLog.e(TAG, "CPUPowerMode has already enabled!");
            return;
        }
        sIsFeatureEnable.set(true);
        this.mContext = context;
        registerPowerStateReceiver();
        int powermode = Settings.System.getInt(this.mContext.getContentResolver(), "SmartModeStatus", 1);
        if (powermode == 1) {
            this.mPowerMode = 1;
            sIsSaveMode.set(false);
        } else if (powermode == 4) {
            this.mPowerMode = 2;
            sIsSaveMode.set(true);
        } else {
            AwareLog.d(TAG, "enable powermode not need to process");
        }
        if (!isSuperPowerSave()) {
            sIsSuperSaveMode.set(false);
        } else {
            sIsSuperSaveMode.set(true);
        }
        SystemProperties.set("persist.sys.performance", isPerformanceMode() ? "true" : "false");
    }

    public void disable() {
        if (!sIsFeatureEnable.get()) {
            AwareLog.e(TAG, "CPUPowerMode has already disabled!");
            return;
        }
        sIsFeatureEnable.set(false);
        unregisterPowerStateReceiver();
    }

    private CPUPowerMode() {
    }

    public static CPUPowerMode getInstance() {
        CPUPowerMode cPUPowerMode;
        synchronized (SLOCK) {
            if (sInstance == null) {
                sInstance = new CPUPowerMode();
            }
            cPUPowerMode = sInstance;
        }
        return cPUPowerMode;
    }

    public boolean isSuperPowerSave() {
        return "true".equals(SystemProperties.get(GestureNavConst.KEY_SUPER_SAVE_MODE, "false"));
    }

    private void registerPowerStateReceiver() {
        if (this.mContext != null && this.mPowerStateChangeReceiver == null) {
            this.mPowerStateChangeReceiver = new PowerStateChangeReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_POWER_MODE_CHANGE);
            filter.addAction(ACTION_ENTER_SUPER_SAVE_MODE);
            this.mContext.registerReceiver(this.mPowerStateChangeReceiver, filter);
            IntentFilter filterSuper = new IntentFilter();
            filterSuper.addAction(ACTION_QUIT_SUPER_SAVE_MODE);
            this.mContext.registerReceiver(this.mPowerStateChangeReceiver, filterSuper, "com.huawei.systemmanager.permission.ACCESS_INTERFACE", null);
        }
    }

    private void unregisterPowerStateReceiver() {
        PowerStateChangeReceiver powerStateChangeReceiver;
        Context context = this.mContext;
        if (context != null && (powerStateChangeReceiver = this.mPowerStateChangeReceiver) != null) {
            context.unregisterReceiver(powerStateChangeReceiver);
            this.mPowerStateChangeReceiver = null;
        }
    }

    private class PowerStateChangeReceiver extends BroadcastReceiver {
        private PowerStateChangeReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                AwareLog.e(CPUPowerMode.TAG, "PowerStateChangeReceiver onReceive intent null!");
            } else if (!CPUPowerMode.sIsFeatureEnable.get()) {
                AwareLog.e(CPUPowerMode.TAG, "PowerStateChangeReceiver disable, return!");
            } else {
                String action = intent.getAction();
                if (CPUPowerMode.ACTION_POWER_MODE_CHANGE.equals(action)) {
                    int powerMode = intent.getIntExtra(SceneRecogFeature.DATA_STATE, 0);
                    if (powerMode == 1) {
                        int unused = CPUPowerMode.this.mPowerMode = 2;
                        CPUPowerMode.sIsSaveMode.set(true);
                    } else if (powerMode == 2) {
                        int unused2 = CPUPowerMode.this.mPowerMode = 1;
                        CPUPowerMode.sIsSaveMode.set(false);
                    } else if (powerMode == 3) {
                        int unused3 = CPUPowerMode.this.mPowerMode = 3;
                    } else {
                        AwareLog.d(CPUPowerMode.TAG, "unknow powerMode " + powerMode);
                    }
                } else if (CPUPowerMode.ACTION_ENTER_SUPER_SAVE_MODE.equals(action)) {
                    CPUPowerMode cPUPowerMode = CPUPowerMode.this;
                    int unused4 = cPUPowerMode.mTempPowerMode = cPUPowerMode.mPowerMode;
                    int unused5 = CPUPowerMode.this.mPowerMode = 4;
                    CPUPowerMode.sIsSuperSaveMode.set(true);
                } else if (CPUPowerMode.ACTION_QUIT_SUPER_SAVE_MODE.equals(action)) {
                    CPUPowerMode cPUPowerMode2 = CPUPowerMode.this;
                    int unused6 = cPUPowerMode2.mPowerMode = cPUPowerMode2.mTempPowerMode;
                    CPUPowerMode.sIsSuperSaveMode.set(false);
                } else {
                    AwareLog.d(CPUPowerMode.TAG, "onReceive invaild action");
                }
                SystemProperties.set("persist.sys.performance", CPUPowerMode.isPerformanceMode() ? "true" : "false");
                CPUHighLoadManager.getInstance().notifyPowerStateChanged(CPUPowerMode.isPowerModePerformance(CPUPowerMode.this.mPowerMode));
            }
        }
    }

    public static boolean isPerformanceMode() {
        return !sIsSaveMode.get() && !sIsSuperSaveMode.get();
    }

    public static boolean isPowerModePerformance(int mode) {
        return mode == 3;
    }
}
