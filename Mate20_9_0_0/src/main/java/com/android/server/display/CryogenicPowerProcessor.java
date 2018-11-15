package com.android.server.display;

import android.os.Bundle;
import android.os.Handler;
import android.os.HwBrightnessProcessor;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.HwBrightnessXmlLoader.Data;

public class CryogenicPowerProcessor extends HwBrightnessProcessor {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static String TAG = "CryogenicPowerProcessor";
    private HwNormalizedAutomaticBrightnessController mAutomaticBrightnessController;
    private final Data mData;
    private Handler mHandler;
    private Object mLockHandle;
    private int mMaxBrightness = 0;
    private Runnable mMaxBrightnessEffectivenessCheckingRunnable;
    private long mUpdateTime = 0;

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public CryogenicPowerProcessor(AutomaticBrightnessController controller) {
        this.mAutomaticBrightnessController = (HwNormalizedAutomaticBrightnessController) controller;
        this.mData = HwBrightnessXmlLoader.getData();
        this.mLockHandle = new Object();
        this.mHandler = new Handler();
        this.mMaxBrightnessEffectivenessCheckingRunnable = new Runnable() {
            public void run() {
                CryogenicPowerProcessor.this.handleMaxBrightnessEffectivenessChecking();
            }
        };
        if (this.mAutomaticBrightnessController != null) {
            this.mAutomaticBrightnessController.registerCryogenicProcessor(this);
        }
    }

    public void onScreenOff() {
        synchronized (this.mLockHandle) {
            if (this.mMaxBrightness > 0) {
                this.mUpdateTime = SystemClock.elapsedRealtime();
                if (HWFLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onScreenOff() mUpdateTime=");
                    stringBuilder.append(this.mUpdateTime);
                    Slog.d(str, stringBuilder.toString());
                }
                if (!queueMaxBrightnessEffectivenessChecking(this.mData.cryogenicMaxBrightnessTimeOut)) {
                    Slog.e(TAG, "Failed to call queueMaxBrightnessEffectivenessChecking()");
                }
            }
        }
    }

    private void handleMaxBrightnessEffectivenessChecking() {
        if (this.mAutomaticBrightnessController == null) {
            Slog.e(TAG, "mAutomaticBrightnessController=null");
            return;
        }
        if (!this.mAutomaticBrightnessController.getScreenStatus()) {
            synchronized (this.mLockHandle) {
                long time = SystemClock.elapsedRealtime() - this.mUpdateTime;
                if (time > this.mData.cryogenicMaxBrightnessTimeOut) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("The time of max brightness updating from cryogenic is over ");
                    stringBuilder.append(time);
                    stringBuilder.append("ms, and the value becomes invalid!");
                    Slog.i(str, stringBuilder.toString());
                    this.mAutomaticBrightnessController.setMaxBrightnessFromCryogenic(0);
                } else {
                    queueMaxBrightnessEffectivenessChecking(this.mData.cryogenicMaxBrightnessTimeOut - time);
                }
            }
        }
    }

    private boolean queueMaxBrightnessEffectivenessChecking(long delayMillis) {
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("queueMaxBrightnessEffectivenessChecking() delay=");
            stringBuilder.append(delayMillis);
            stringBuilder.append("ms");
            Slog.d(str, stringBuilder.toString());
        }
        this.mHandler.removeCallbacks(this.mMaxBrightnessEffectivenessCheckingRunnable);
        return this.mHandler.postDelayed(this.mMaxBrightnessEffectivenessCheckingRunnable, delayMillis);
    }

    private boolean checkMaxBrightnessEffectiveness(int maxBrightness) {
        boolean ret;
        synchronized (this.mLockHandle) {
            this.mUpdateTime = SystemClock.elapsedRealtime();
            if (HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkMaxBrightnessEffectiveness() mUpdateTime=");
                stringBuilder.append(this.mUpdateTime);
                Slog.d(str, stringBuilder.toString());
            }
            if (this.mAutomaticBrightnessController.getScreenStatus()) {
                ret = true;
            } else {
                ret = queueMaxBrightnessEffectivenessChecking(this.mData.cryogenicMaxBrightnessTimeOut);
            }
            if (this.mMaxBrightness != maxBrightness) {
                this.mAutomaticBrightnessController.setMaxBrightnessFromCryogenic(maxBrightness);
            }
        }
        return ret;
    }

    public boolean setData(Bundle data, int[] retValue) {
        int i = -1;
        retValue[0] = -1;
        String str;
        StringBuilder stringBuilder;
        if (!this.mData.cryogenicEnable) {
            Slog.w(TAG, "Cryogenic is disable!");
            return true;
        } else if (this.mAutomaticBrightnessController == null) {
            Slog.e(TAG, "mAutomaticBrightnessController=null");
            return true;
        } else if (data == null || retValue.length <= 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setData() invalid input: data=");
            stringBuilder.append(data);
            stringBuilder.append(",retValue.length=");
            stringBuilder.append(retValue.length);
            Slog.e(str, stringBuilder.toString());
            return true;
        } else {
            int maxBrightness = data.getInt("MaxBrightness", 0);
            if (maxBrightness < 0 || maxBrightness > 255) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("setData() invalid input: maxBrightness=");
                stringBuilder.append(maxBrightness);
                Slog.e(str, stringBuilder.toString());
                return true;
            }
            if (checkMaxBrightnessEffectiveness(maxBrightness)) {
                i = 0;
            }
            retValue[0] = i;
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Cryogenic set maxBrightness=");
            stringBuilder2.append(maxBrightness);
            stringBuilder2.append(retValue[0] == 0 ? " success" : " failed!");
            Slog.i(str, stringBuilder2.toString());
            return true;
        }
    }
}
