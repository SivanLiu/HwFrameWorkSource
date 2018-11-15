package com.android.server.rms.iaware.cpu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.rms.iaware.AwareLog;
import java.util.concurrent.atomic.AtomicBoolean;

class CPUPowerMode {
    private static final String ACTION_ENTER_SUPER_SAVE_MODE = "huawei.intent.action.HWSYSTEMMANAGER_CHANGE_POWERMODE";
    private static final String ACTION_POWER_MODE_CHANGE = "huawei.intent.action.POWER_MODE_CHANGED_ACTION";
    private static final String ACTION_QUIT_SUPER_SAVE_MODE = "huawei.intent.action.HWSYSTEMMANAGER_SHUTDOWN_LIMIT_POWERMODE";
    private static final int EXTRA_NONSAVE_STATE = 2;
    private static final int EXTRA_SAVE_STATE = 1;
    private static final int NON_SAVE_POWER_MODE = 1;
    private static final int SAVE_POWER_MODE = 2;
    private static final int SETTING_NONSAVE_POWER_MODE = 1;
    private static final int SETTING_SAVE_POWER_MODE = 4;
    private static final int SUPER_SAVE_POWER_MODE = 3;
    private static final String SYSTEM_MANAGER_PERMISSION = "com.huawei.systemmanager.permission.ACCESS_INTERFACE";
    private static final String TAG = "CPUPowerMode";
    private static AtomicBoolean mIsFeatureEnable = new AtomicBoolean(false);
    private static AtomicBoolean mIsSaveMode = new AtomicBoolean(false);
    private static AtomicBoolean mIsSuperSaveMode = new AtomicBoolean(false);
    private static CPUPowerMode sInstance;
    private Context mContext;
    private int mPowerMode = 1;
    private PowerStateChangeReceiver mPowerStateChangeReceiver;
    private int mTempPowerMode = 1;

    private class PowerStateChangeReceiver extends BroadcastReceiver {
        private PowerStateChangeReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                AwareLog.e(CPUPowerMode.TAG, "PowerStateChangeReceiver onReceive intent null!");
            } else if (CPUPowerMode.mIsFeatureEnable.get()) {
                String action = intent.getAction();
                if (CPUPowerMode.ACTION_POWER_MODE_CHANGE.equals(action)) {
                    int powerMode = intent.getIntExtra("state", 0);
                    if (powerMode == 1) {
                        CPUPowerMode.this.mPowerMode = 2;
                        CPUPowerMode.mIsSaveMode.set(true);
                    } else if (powerMode == 2) {
                        CPUPowerMode.this.mPowerMode = 1;
                        CPUPowerMode.mIsSaveMode.set(false);
                    }
                } else if (CPUPowerMode.ACTION_ENTER_SUPER_SAVE_MODE.equals(action)) {
                    CPUPowerMode.this.mTempPowerMode = CPUPowerMode.this.mPowerMode;
                    CPUPowerMode.this.mPowerMode = 3;
                    CPUPowerMode.mIsSuperSaveMode.set(true);
                } else if (CPUPowerMode.ACTION_QUIT_SUPER_SAVE_MODE.equals(action)) {
                    CPUPowerMode.this.mPowerMode = CPUPowerMode.this.mTempPowerMode;
                    CPUPowerMode.mIsSuperSaveMode.set(false);
                }
                SystemProperties.set("persist.sys.performance", CPUPowerMode.isPerformanceMode() ? "true" : "false");
            } else {
                AwareLog.e(CPUPowerMode.TAG, "PowerStateChangeReceiver disable, return!");
            }
        }
    }

    public void enable(Context context) {
        if (mIsFeatureEnable.get()) {
            AwareLog.e(TAG, "CPUPowerMode has already enabled!");
            return;
        }
        mIsFeatureEnable.set(true);
        this.mContext = context;
        registerPowerStateReceiver();
        int powermode = System.getInt(this.mContext.getContentResolver(), "SmartModeStatus", 1);
        if (powermode == 1) {
            this.mPowerMode = 1;
            mIsSaveMode.set(false);
        } else if (powermode == 4) {
            this.mPowerMode = 2;
            mIsSaveMode.set(true);
        }
        if (isSuperPowerSave()) {
            mIsSuperSaveMode.set(true);
        } else {
            mIsSuperSaveMode.set(false);
        }
        SystemProperties.set("persist.sys.performance", isPerformanceMode() ? "true" : "false");
    }

    public void disable() {
        if (mIsFeatureEnable.get()) {
            mIsFeatureEnable.set(false);
            unregisterPowerStateReceiver();
            return;
        }
        AwareLog.e(TAG, "CPUPowerMode has already disabled!");
    }

    public static synchronized CPUPowerMode getInstance() {
        CPUPowerMode cPUPowerMode;
        synchronized (CPUPowerMode.class) {
            if (sInstance == null) {
                sInstance = new CPUPowerMode();
            }
            cPUPowerMode = sInstance;
        }
        return cPUPowerMode;
    }

    public boolean isSuperPowerSave() {
        return "true".equals(SystemProperties.get("sys.super_power_save", "false"));
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
            this.mContext.registerReceiver(this.mPowerStateChangeReceiver, filterSuper, SYSTEM_MANAGER_PERMISSION, null);
        }
    }

    private void unregisterPowerStateReceiver() {
        if (!(this.mContext == null || this.mPowerStateChangeReceiver == null)) {
            this.mContext.unregisterReceiver(this.mPowerStateChangeReceiver);
            this.mPowerStateChangeReceiver = null;
        }
    }

    public static boolean isPerformanceMode() {
        return (mIsSaveMode.get() || mIsSuperSaveMode.get()) ? false : true;
    }
}
