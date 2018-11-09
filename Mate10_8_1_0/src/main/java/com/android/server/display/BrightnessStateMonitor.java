package com.android.server.display;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.BackLightCommonData.BrightnessMode;
import com.android.server.display.BackLightMonitorManager.MsgType;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;

class BrightnessStateMonitor implements MonitorModule {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String PARAM_BRIGHTNESS = "brightness";
    private static final String PARAM_IS_THERMAL_LIMITED = "isThermalLimited";
    private static final String PARAM_POWER_STATE = "powerState";
    private static final String PARAM_SCENE = "scene";
    private static final long STATE_TIMER_FIRST_DELAY_MILLIS = 60000;
    private static final long STATE_TIMER_PERIOD_MILLIS = 600000;
    private static final long STATE_UPLOAD_MIN_INTERVAL_MILLIS = 599000;
    private static final String TAG = "BrightnessStateMonitor";
    private static final String TYPE_ALGO_DISCOUNT_BRIGHTNESS = "algoDiscountBrightness";
    private static final String TYPE_POWER_STATE_UPDATE = "powerStateUpdate";
    private static final String TYPE_SCENE_RECOGNITION = "sceneRecognition";
    private static final String TYPE_STATE_TIMER = "stateTimer";
    private static final String TYPE_THERMAL_LIMIT = "thermalLimit";
    private int mAlgoDiscountBrightness;
    private final BackLightCommonData mCommonData;
    private final boolean mDebugMode;
    private boolean mIsThermalLimited;
    private final BackLightMonitorManager mManager;
    private final DisplayEffectMonitor mMonitor;
    private PowerState mPowerState = PowerState.OFF;
    private Scene mScene = Scene.OTHERS;
    private long mStateUploadTime;

    private enum PowerState {
        OFF,
        ON,
        DIM,
        VR
    }

    private enum Scene {
        OTHERS,
        GAME,
        VIDEO
    }

    static {
        boolean z = true;
        boolean isLoggable = !Log.HWLog ? Log.HWModuleLog ? Log.isLoggable(TAG, 3) : false : true;
        HWDEBUG = isLoggable;
        if (!Log.HWINFO) {
            z = Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false;
        }
        HWFLOW = z;
    }

    public BrightnessStateMonitor(DisplayEffectMonitor monitor, BackLightMonitorManager manager) {
        this.mMonitor = monitor;
        this.mManager = manager;
        this.mCommonData = manager.getBackLightCommonData();
        this.mDebugMode = SystemProperties.getBoolean("persist.display.monitor.debug", false);
    }

    public boolean isParamOwner(String paramType) {
        if (paramType.equals(TYPE_POWER_STATE_UPDATE) || paramType.equals(TYPE_THERMAL_LIMIT) || paramType.equals(TYPE_STATE_TIMER) || paramType.equals(TYPE_SCENE_RECOGNITION) || paramType.equals(TYPE_ALGO_DISCOUNT_BRIGHTNESS)) {
            return true;
        }
        return false;
    }

    public void sendMonitorParam(ArrayMap<String, Object> params) {
        if (params == null || ((params.get(MonitorModule.PARAM_TYPE) instanceof String) ^ 1) != 0) {
            Slog.e(TAG, "sendMonitorParam() input params format error!");
            return;
        }
        String paramType = (String) params.get(MonitorModule.PARAM_TYPE);
        if (paramType.equals(TYPE_POWER_STATE_UPDATE)) {
            powerStateUpdate(params);
        } else if (paramType.equals(TYPE_THERMAL_LIMIT)) {
            thermalLimit(params);
        } else if (paramType.equals(TYPE_STATE_TIMER)) {
            stateTimer();
        } else if (paramType.equals(TYPE_SCENE_RECOGNITION)) {
            sceneRecognition(params);
        } else if (paramType.equals(TYPE_ALGO_DISCOUNT_BRIGHTNESS)) {
            algoDiscountBrightness(params);
        } else {
            Slog.e(TAG, "sendMonitorParam() undefine paramType: " + paramType);
        }
    }

    public void triggerUploadTimer() {
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void powerStateUpdate(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && powerStateUpdateCheckParamValid(params)) {
            String powerStateName = (String) params.get(PARAM_POWER_STATE);
            try {
                PowerState newState = PowerState.valueOf(powerStateName);
                if (this.mPowerState != newState) {
                    if (HWDEBUG) {
                        Slog.d(TAG, "powerStateUpdate " + newState);
                    }
                    powerStateProcess(newState);
                    this.mPowerState = newState;
                }
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "powerStateUpdate() input error! powerState=" + powerStateName);
            }
        }
    }

    private boolean powerStateUpdateCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (params.get(PARAM_POWER_STATE) instanceof String) {
            return true;
        }
        Slog.e(TAG, "powerStateUpdateCheckParamValid() can't get param: powerState");
        return false;
    }

    private void powerStateProcess(PowerState newState) {
        if (newState == PowerState.OFF) {
            if (HWDEBUG) {
                Slog.d(TAG, "powerStateProcess removeMonitorMsg()");
            }
            this.mManager.removeMonitorMsg(MsgType.STATE_TIMER);
        }
        if (newState == PowerState.ON && this.mPowerState == PowerState.OFF) {
            setStateTimer(60000);
        }
    }

    private void setStateTimer(long delayMillis) {
        if (this.mDebugMode) {
            delayMillis /= 10;
        }
        if (HWDEBUG) {
            Slog.d(TAG, "setStateTimer delay " + (delayMillis / 1000) + " s");
        }
        ArrayMap<String, Object> params = new ArrayMap();
        params.put(MonitorModule.PARAM_TYPE, TYPE_STATE_TIMER);
        this.mManager.sendMonitorMsgDelayed(MsgType.STATE_TIMER, params, delayMillis);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void thermalLimit(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && thermalLimitCheckParamValid(params)) {
            this.mIsThermalLimited = ((Boolean) params.get(PARAM_IS_THERMAL_LIMITED)).booleanValue();
            if (HWFLOW) {
                Slog.i(TAG, "thermalLimit " + this.mIsThermalLimited);
            }
        }
    }

    private boolean thermalLimitCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (params.get(PARAM_IS_THERMAL_LIMITED) instanceof Boolean) {
            return true;
        }
        Slog.e(TAG, "thermalLimitCheckParamValid() can't get param: isThermalLimited");
        return false;
    }

    private void stateTimer() {
        if (this.mPowerState != PowerState.OFF) {
            setStateTimer(600000);
        }
        stateProcess();
    }

    private void stateProcess() {
        if (needUploadState()) {
            long currentTime = SystemClock.elapsedRealtime();
            long minIntervalMillis = this.mDebugMode ? 59900 : STATE_UPLOAD_MIN_INTERVAL_MILLIS;
            if (this.mStateUploadTime == 0 || currentTime - this.mStateUploadTime >= minIntervalMillis) {
                this.mStateUploadTime = currentTime;
                stateUpload(this.mScene, this.mCommonData.getSmoothAmbientLight(), this.mAlgoDiscountBrightness);
            }
        }
    }

    private boolean needUploadState() {
        if (this.mCommonData.getBrightnessMode() != BrightnessMode.AUTO || this.mPowerState != PowerState.ON || this.mIsThermalLimited) {
            return false;
        }
        if (this.mScene == Scene.VIDEO || !this.mCommonData.isWindowManagerBrightnessMode()) {
            return true;
        }
        return false;
    }

    private void stateUpload(Scene scene, int ambient, int backlight) {
        int i = 32767;
        EventStream stream = IMonitor.openEventStream(932010105);
        stream.setParam((short) 0, (byte) scene.ordinal());
        stream.setParam((short) 1, (short) (ambient < 32767 ? ambient : 32767));
        if (backlight < 32767) {
            i = backlight;
        }
        stream.setParam((short) 2, (short) i);
        IMonitor.sendEvent(stream);
        IMonitor.closeEventStream(stream);
        if (HWFLOW) {
            Slog.i(TAG, "stateUpload() scene=" + scene + ", ambient=" + ambient + ", backlight=" + backlight);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void sceneRecognition(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && sceneRecognitionCheckParamValid(params)) {
            String sceneName = (String) params.get("scene");
            try {
                this.mScene = Scene.valueOf(sceneName);
                if (HWDEBUG) {
                    Slog.d(TAG, "sceneRecognition " + this.mScene);
                }
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "sceneRecognition() input error! scene=" + sceneName);
            }
        }
    }

    private boolean sceneRecognitionCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (params.get("scene") instanceof String) {
            return true;
        }
        Slog.e(TAG, "sceneRecognitionCheckParamValid() can't get param: scene");
        return false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void algoDiscountBrightness(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && algoDiscountBrightnessCheckParamValid(params)) {
            this.mAlgoDiscountBrightness = ((Integer) params.get(PARAM_BRIGHTNESS)).intValue();
            if (HWDEBUG) {
                Slog.d(TAG, "mAlgoDiscountBrightness " + this.mAlgoDiscountBrightness);
            }
        }
    }

    private boolean algoDiscountBrightnessCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (params.get(PARAM_BRIGHTNESS) instanceof Integer) {
            return true;
        }
        Slog.e(TAG, "algoDiscountBrightnessCheckParamValid() can't get param: brightness");
        return false;
    }
}
