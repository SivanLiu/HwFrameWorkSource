package com.android.server.display;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.HwBrightnessProcessor;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.display.IHwDisplayPowerControllerEx;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.policy.WindowManagerPolicy;

public final class HwDisplayPowerControllerEx implements IHwDisplayPowerControllerEx {
    private static final int DOZE_MODE = 1;
    private static final boolean HWDEBUG = (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3)));
    private static final String HW_SCREEN_OFF_FOR_POSITIVE = "hw.intent.action.HW_SCREEN_OFF_FOR_POSITIVE";
    private static final String HW_SCREEN_OFF_FOR_POSITIVE_PERMISSION = "com.huawei.permission.HW_SCREEN_OFF_FOR_POSITIVE";
    private static final String KEY_POSITIVE = "key_positive";
    private static final int NORMAL_MODE = 2;
    private static final int SCENE = 22;
    private static final String TAG = "HwDisplayPowerControllerEx";
    private AutomaticBrightnessController mAutomaticBrightnessController = null;
    private Light mBackLight = null;
    /* access modifiers changed from: private */
    public final IHwDisplayPowerControllerEx.Callbacks mCallbacks;
    private final Context mContext;
    private HwDisplayBrightnessProcessor mHwDisplayBrightnessProcessor = null;
    private final LightsManager mLights;
    private ManualBrightnessController mManualBrightnessController = null;
    /* access modifiers changed from: private */
    public boolean mPendingTpKeep = false;
    private boolean mProxPendingByPhone = false;
    private boolean mProxPositive = false;
    private boolean mProximityTop = SystemProperties.getBoolean("ro.config.proximity_top", false);
    /* access modifiers changed from: private */
    public boolean mTpKeep = false;
    private TpKeepChange mTpKeepChange;
    private final WindowManagerPolicy mWindowManagerPolicy;

    public HwDisplayPowerControllerEx(Context context, IHwDisplayPowerControllerEx.Callbacks callbacks) {
        ManualBrightnessController manualBrightnessController;
        this.mCallbacks = callbacks;
        this.mContext = context;
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mLights = (LightsManager) LocalServices.getService(LightsManager.class);
        LightsManager lightsManager = this.mLights;
        if (lightsManager != null) {
            this.mBackLight = lightsManager.getLight(0);
        }
        IHwDisplayPowerControllerEx.Callbacks callbacks2 = this.mCallbacks;
        if (callbacks2 != null) {
            this.mAutomaticBrightnessController = callbacks2.getAutomaticBrightnessController();
            this.mManualBrightnessController = this.mCallbacks.getManualBrightnessController();
        }
        if (this.mAutomaticBrightnessController == null) {
            Slog.w(TAG, "init mAutomaticBrightnessController failed");
        }
        if (this.mManualBrightnessController == null) {
            Slog.w(TAG, "init mManualBrightnessController failed");
        }
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null && (manualBrightnessController = this.mManualBrightnessController) != null) {
            this.mHwDisplayBrightnessProcessor = new HwDisplayBrightnessProcessor(automaticBrightnessController, manualBrightnessController);
        }
    }

    private final class TpKeepChange implements WindowManagerPolicy.TpKeepListener {
        private TpKeepChange() {
        }

        public void setTpKeep(boolean keep) {
            boolean unused = HwDisplayPowerControllerEx.this.mTpKeep = keep;
            if (HwDisplayPowerControllerEx.this.mPendingTpKeep != keep && HwDisplayPowerControllerEx.this.mCallbacks != null) {
                HwDisplayPowerControllerEx.this.mCallbacks.onTpKeepStateChanged(keep);
                boolean unused2 = HwDisplayPowerControllerEx.this.mPendingTpKeep = keep;
            }
        }
    }

    public void initTpKeepParamters() {
        if (this.mProximityTop) {
            this.mTpKeepChange = new TpKeepChange();
            this.mWindowManagerPolicy.setTpKeepListener(this.mTpKeepChange);
        }
    }

    public boolean getTpKeep() {
        return this.mTpKeep;
    }

    public void setTPDozeMode(boolean useProximitySensor) {
        if (this.mProximityTop && useProximitySensor != this.mProxPendingByPhone) {
            if (useProximitySensor) {
                this.mWindowManagerPolicy.setTPDozeMode(22, 1);
            } else {
                this.mWindowManagerPolicy.setTPDozeMode(22, 2);
            }
            this.mProxPendingByPhone = useProximitySensor;
        }
    }

    public void sendProximityBroadcast(boolean positive) {
        if (this.mContext == null) {
            Slog.e(TAG, "mContext is null, can not sendProximityBroadcast.");
            return;
        }
        Intent intent = new Intent(HW_SCREEN_OFF_FOR_POSITIVE);
        intent.putExtra(KEY_POSITIVE, positive);
        Slog.d(TAG, "sendProximityBroadcast: hw.intent.action.HW_SCREEN_OFF_FOR_POSITIVE");
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, HW_SCREEN_OFF_FOR_POSITIVE_PERMISSION);
    }

    public boolean setHwBrightnessData(String name, Bundle data, int[] result) {
        HwDisplayBrightnessProcessor hwDisplayBrightnessProcessor;
        HwBrightnessProcessor processor;
        Light light = this.mBackLight;
        if (light == null) {
            Slog.e(TAG, "setHwBrightnessData with mBackLight null");
            return false;
        }
        boolean ret = light.setHwBrightnessData(name, data, result);
        if (!(ret || (hwDisplayBrightnessProcessor = this.mHwDisplayBrightnessProcessor) == null || (processor = hwDisplayBrightnessProcessor.getProcessor(name)) == null)) {
            ret = processor.setData(data, result);
        }
        if (HWDEBUG) {
            Slog.d(TAG, "setHwBrightnessData-name=" + name + ",ret=" + ret);
        }
        return ret;
    }

    public boolean getHwBrightnessData(String name, Bundle data, int[] result) {
        HwDisplayBrightnessProcessor hwDisplayBrightnessProcessor;
        HwBrightnessProcessor processor;
        Light light = this.mBackLight;
        if (light == null) {
            Slog.e(TAG, "getHwBrightnessData with mBackLight null");
            return false;
        }
        boolean ret = light.getHwBrightnessData(name, data, result);
        if (!(ret || (hwDisplayBrightnessProcessor = this.mHwDisplayBrightnessProcessor) == null || (processor = hwDisplayBrightnessProcessor.getProcessor(name)) == null)) {
            ret = processor.getData(data, result);
        }
        if (HWDEBUG) {
            Slog.d(TAG, "getHwBrightnessData-name=" + name + ",ret=" + ret);
        }
        return ret;
    }
}
