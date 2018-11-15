package com.android.server.display;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.BackLightCommonData.BrightnessMode;
import com.android.server.display.BackLightCommonData.Scene;
import com.android.server.display.BackLightMonitorManager.MsgType;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;

class BrightnessStateMonitor implements MonitorModule {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String PARAM_BRIGHTNESS = "brightness";
    private static final String PARAM_IS_THERMAL_LIMITED = "isLimited";
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
    private final BackLightMonitorManager mManager;
    private final DisplayEffectMonitor mMonitor;
    private PowerState mPowerState = PowerState.OFF;
    private Scene mScene;
    private long mStateUploadTime;

    private enum PowerState {
        OFF,
        ON,
        DIM,
        VR
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public BrightnessStateMonitor(DisplayEffectMonitor monitor, BackLightMonitorManager manager) {
        this.mMonitor = monitor;
        this.mManager = manager;
        this.mCommonData = manager.getBackLightCommonData();
        this.mDebugMode = SystemProperties.getBoolean("persist.display.monitor.debug", false);
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isParamOwner(String paramType) {
        boolean z;
        switch (paramType.hashCode()) {
            case -663211403:
                if (paramType.equals(TYPE_POWER_STATE_UPDATE)) {
                    z = false;
                    break;
                }
            case -238058188:
                if (paramType.equals(TYPE_STATE_TIMER)) {
                    z = true;
                    break;
                }
            case 871202699:
                if (paramType.equals(TYPE_SCENE_RECOGNITION)) {
                    z = true;
                    break;
                }
            case 1567901733:
                if (paramType.equals(TYPE_ALGO_DISCOUNT_BRIGHTNESS)) {
                    z = true;
                    break;
                }
            case 1671621220:
                if (paramType.equals(TYPE_THERMAL_LIMIT)) {
                    z = true;
                    break;
                }
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
            case true:
            case true:
            case true:
            case true:
                return true;
            default:
                return false;
        }
    }

    public void sendMonitorParam(ArrayMap<String, Object> params) {
        if (params == null || !(params.get(MonitorModule.PARAM_TYPE) instanceof String)) {
            Slog.e(TAG, "sendMonitorParam() input params format error!");
            return;
        }
        String paramType = (String) params.get(MonitorModule.PARAM_TYPE);
        Object obj = -1;
        switch (paramType.hashCode()) {
            case -663211403:
                if (paramType.equals(TYPE_POWER_STATE_UPDATE)) {
                    obj = null;
                    break;
                }
                break;
            case -238058188:
                if (paramType.equals(TYPE_STATE_TIMER)) {
                    obj = 2;
                    break;
                }
                break;
            case 871202699:
                if (paramType.equals(TYPE_SCENE_RECOGNITION)) {
                    obj = 3;
                    break;
                }
                break;
            case 1567901733:
                if (paramType.equals(TYPE_ALGO_DISCOUNT_BRIGHTNESS)) {
                    obj = 4;
                    break;
                }
                break;
            case 1671621220:
                if (paramType.equals(TYPE_THERMAL_LIMIT)) {
                    obj = 1;
                    break;
                }
                break;
        }
        switch (obj) {
            case null:
                powerStateUpdate(params);
                break;
            case 1:
                thermalLimit(params);
                break;
            case 2:
                stateTimer();
                break;
            case 3:
                sceneRecognition(params);
                break;
            case 4:
                algoDiscountBrightness(params);
                break;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendMonitorParam() undefine paramType: ");
                stringBuilder.append(paramType);
                Slog.e(str, stringBuilder.toString());
                break;
        }
    }

    public void triggerUploadTimer() {
    }

    /* JADX WARNING: Missing block: B:21:0x0063, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void powerStateUpdate(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && powerStateUpdateCheckParamValid(params)) {
            String powerStateName = (String) params.get(PARAM_POWER_STATE);
            String str;
            StringBuilder stringBuilder;
            try {
                PowerState newState = PowerState.valueOf(powerStateName);
                if (this.mPowerState != newState) {
                    if (HWDEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("powerStateUpdate ");
                        stringBuilder.append(newState);
                        Slog.d(str, stringBuilder.toString());
                    }
                    powerStateProcess(newState);
                    this.mPowerState = newState;
                }
            } catch (IllegalArgumentException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("powerStateUpdate() input error! powerState=");
                stringBuilder.append(powerStateName);
                Slog.e(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setStateTimer delay ");
            stringBuilder.append(delayMillis / 1000);
            stringBuilder.append(" s");
            Slog.d(str, stringBuilder.toString());
        }
        ArrayMap<String, Object> params = new ArrayMap();
        params.put(MonitorModule.PARAM_TYPE, TYPE_STATE_TIMER);
        this.mManager.sendMonitorMsgDelayed(MsgType.STATE_TIMER, params, delayMillis);
    }

    /* JADX WARNING: Missing block: B:11:0x0045, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void thermalLimit(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && thermalLimitCheckParamValid(params)) {
            boolean isThermalLimited = ((Boolean) params.get(PARAM_IS_THERMAL_LIMITED)).booleanValue();
            this.mCommonData.setThermalLimited(isThermalLimited);
            if (HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("thermalLimit ");
                stringBuilder.append(isThermalLimited);
                Slog.i(str, stringBuilder.toString());
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
        Slog.e(TAG, "thermalLimitCheckParamValid() can't get param: isLimited");
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
        if (this.mScene == null || this.mCommonData.getBrightnessMode() != BrightnessMode.AUTO || this.mPowerState != PowerState.ON || this.mCommonData.isThermalLimited()) {
            return false;
        }
        if (this.mScene == Scene.VIDEO || !this.mCommonData.isWindowManagerBrightnessMode()) {
            return true;
        }
        return false;
    }

    private void stateUpload(Scene scene, int ambient, int backlight) {
        EventStream stream = IMonitor.openEventStream(932010105);
        stream.setParam((short) 0, (byte) scene.ordinal());
        int i = 32767;
        stream.setParam((short) 1, (short) (ambient < 32767 ? ambient : 32767));
        if (backlight < 32767) {
            i = backlight;
        }
        stream.setParam((short) 2, (short) i);
        IMonitor.sendEvent(stream);
        IMonitor.closeEventStream(stream);
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stateUpload() scene=");
            stringBuilder.append(scene);
            stringBuilder.append(", ambient=");
            stringBuilder.append(ambient);
            stringBuilder.append(", backlight=");
            stringBuilder.append(backlight);
            Slog.i(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:17:0x005d, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void sceneRecognition(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && sceneRecognitionCheckParamValid(params)) {
            String sceneName = (String) params.get("scene");
            try {
                this.mScene = Scene.valueOf(sceneName);
                if (HWDEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sceneRecognition ");
                    stringBuilder.append(this.mScene);
                    Slog.d(str, stringBuilder.toString());
                }
            } catch (IllegalArgumentException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sceneRecognition() input error! scene=");
                stringBuilder2.append(sceneName);
                Slog.e(str2, stringBuilder2.toString());
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

    /* JADX WARNING: Missing block: B:11:0x0043, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void algoDiscountBrightness(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && algoDiscountBrightnessCheckParamValid(params)) {
            this.mAlgoDiscountBrightness = ((Integer) params.get(PARAM_BRIGHTNESS)).intValue();
            if (HWDEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mAlgoDiscountBrightness ");
                stringBuilder.append(this.mAlgoDiscountBrightness);
                Slog.d(str, stringBuilder.toString());
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
