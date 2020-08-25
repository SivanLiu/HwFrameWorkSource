package com.android.server.fsm;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.LocalServices;
import com.huawei.android.fsm.HwFoldScreenManagerInternal;

public final class MagnetometerWakeupManager extends WakeupManager {
    private static final int EXT_HALL_DATA_LENGTH = 2;
    private static final int HALL_FOLDED_THRESHOLD = 0;
    private static final int HALL_THRESHOLD = 1;
    private static final int LOW_TEMP_VALUE_INDEX = 2;
    private static final int LOW_TEMP_VALUE_LENGTH = 3;
    private static final int MAGN_HALL_TYPE = 2;
    private static final int SENSOR_RATE = 100000;
    private static final int SENSOR_TYPE_HALL = 65557;
    private static final String TAG = "Fsm_MagnetometerWakeupManager";
    private static MagnetometerWakeupManager mInstance = null;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public HwFoldScreenManagerInternal mFoldScreenManagerService;
    /* access modifiers changed from: private */
    public int mHallData = 1;
    SensorEventListener mMagnetometerListener = new SensorEventListener() {
        /* class com.android.server.fsm.MagnetometerWakeupManager.AnonymousClass1 */

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != MagnetometerWakeupManager.SENSOR_TYPE_HALL) {
                Slog.e("Fsm_MagnetometerWakeupManager", "Type is not Hall Sensor");
            } else if (event.values.length < 2) {
                Slog.e("Fsm_MagnetometerWakeupManager", "event value lenght is less than 2");
            } else {
                int unused = MagnetometerWakeupManager.this.mHallData = (int) event.values[1];
                if (MagnetometerWakeupManager.this.mPowerManager == null) {
                    MagnetometerWakeupManager magnetometerWakeupManager = MagnetometerWakeupManager.this;
                    PowerManager unused2 = magnetometerWakeupManager.mPowerManager = (PowerManager) magnetometerWakeupManager.mContext.getSystemService("power");
                }
                if (!MagnetometerWakeupManager.this.mPowerManager.isScreenOn()) {
                    int type = (int) event.values[0];
                    if (type == 2 && MagnetometerWakeupManager.this.mHallData == 1) {
                        Bundle extra = new Bundle();
                        extra.putInt("uid", 1000);
                        extra.putString("opPackageName", MagnetometerWakeupManager.this.mContext.getOpPackageName());
                        extra.putInt("reason", 7);
                        extra.putString("details", "magnetic.wakeUp");
                        if (MagnetometerWakeupManager.this.mFoldScreenManagerService == null) {
                            HwFoldScreenManagerInternal unused3 = MagnetometerWakeupManager.this.mFoldScreenManagerService = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
                        }
                        MagnetometerWakeupManager.this.mFoldScreenManagerService.prepareWakeup(4, extra);
                    }
                    if (type == 2 && MagnetometerWakeupManager.this.mHallData == 0) {
                        Slog.d("Fsm_MagnetometerWakeupManager", "handleDrawWindow to folded status");
                        if (MagnetometerWakeupManager.this.mFoldScreenManagerService == null) {
                            HwFoldScreenManagerInternal unused4 = MagnetometerWakeupManager.this.mFoldScreenManagerService = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
                        }
                        MagnetometerWakeupManager.this.mFoldScreenManagerService.handleDrawWindow();
                    }
                    if (event.values.length >= 3) {
                        int lowTempWarningValue = (int) event.values[2];
                        Slog.i("Fsm_MagnetometerWakeupManager", "hall changed, lowTempWarningValue : " + lowTempWarningValue);
                        if (MagnetometerWakeupManager.this.mFoldScreenManagerService == null) {
                            HwFoldScreenManagerInternal unused5 = MagnetometerWakeupManager.this.mFoldScreenManagerService = (HwFoldScreenManagerInternal) LocalServices.getService(HwFoldScreenManagerInternal.class);
                        }
                        MagnetometerWakeupManager.this.mFoldScreenManagerService.notifyLowTempWarning(lowTempWarningValue);
                    }
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    private PowerManagerInternal mPowerManagerInternal;
    private SensorManager mSensorManager;

    MagnetometerWakeupManager(Context context) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
    }

    public static synchronized MagnetometerWakeupManager getInstance(Context context) {
        MagnetometerWakeupManager magnetometerWakeupManager;
        synchronized (MagnetometerWakeupManager.class) {
            if (mInstance == null) {
                mInstance = new MagnetometerWakeupManager(context);
            }
            magnetometerWakeupManager = mInstance;
        }
        return magnetometerWakeupManager;
    }

    /* access modifiers changed from: protected */
    public int getHallData() {
        return this.mHallData;
    }

    public void initSensorListener() {
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        SensorManager sensorManager = this.mSensorManager;
        if (sensorManager == null) {
            Slog.e("Fsm_MagnetometerWakeupManager", "connect SENSOR_SERVICE fail");
            return;
        }
        boolean ret = this.mSensorManager.registerListener(this.mMagnetometerListener, sensorManager.getDefaultSensor(SENSOR_TYPE_HALL), SENSOR_RATE);
        Slog.d("Fsm_MagnetometerWakeupManager", "register Hall Sensor result:" + ret);
    }

    @Override // com.android.server.fsm.WakeupManager
    public void wakeup() {
        Slog.d("Fsm_MagnetometerWakeupManager", "Wakeup in MagnetometerWakeupManager");
        if (this.mPowerManagerInternal == null) {
            this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        }
        this.mPowerManagerInternal.powerWakeup(SystemClock.uptimeMillis(), this.mReason, this.mDetails, this.mUid, this.mOpPackageName, this.mUid);
    }
}
