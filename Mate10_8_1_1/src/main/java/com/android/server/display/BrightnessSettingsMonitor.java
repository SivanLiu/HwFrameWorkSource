package com.android.server.display;

import android.util.ArrayMap;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.BackLightCommonData.BrightnessMode;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import com.android.server.display.DisplayEffectMonitor.ParamLogPrinter;
import huawei.com.android.server.fingerprint.FingerViewController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class BrightnessSettingsMonitor implements MonitorModule {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String PARAM_BRIGHTNESS = "brightness";
    private static final String PARAM_BRIGHTNESS_MODE = "brightnessMode";
    private static final String PARAM_PACKAGE_NAME = "packageName";
    private static final String TAG = "BrightnessSettingsMonitor";
    private static final String TYPE_BRIGHTNESS_MODE = "brightnessMode";
    private static final String TYPE_MANUAL_BRIGHTNESS = "manualBrightness";
    private static final String TYPE_WINDOW_MANAGER_BRIGHTNESS = "windowManagerBrightness";
    private BrightnessMode mBrightnessModeBackup;
    private BrightnessMode mBrightnessModeLast;
    private String mBrightnessModePackageNameLast;
    private List<String> mBrightnessModePackageNameUploadedList = new ArrayList();
    private List<String> mBrightnessModePackageNameWhiteList = new ArrayList(Arrays.asList(new String[]{FingerViewController.PKGNAME_OF_KEYGUARD, "com.android.settings"}));
    private final BackLightCommonData mCommonData;
    private String mManualBrightnessPackageNameLast;
    private ParamLogPrinter mManualBrightnessPrinter;
    private final DisplayEffectMonitor mMonitor;
    private String mWindowManagerBrightnessPackageNameLast = "android";
    private ParamLogPrinter mWindowManagerBrightnessPrinter;

    static {
        boolean z = true;
        boolean isLoggable = !Log.HWLog ? Log.HWModuleLog ? Log.isLoggable(TAG, 3) : false : true;
        HWDEBUG = isLoggable;
        if (!Log.HWINFO) {
            z = Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false;
        }
        HWFLOW = z;
    }

    public BrightnessSettingsMonitor(DisplayEffectMonitor monitor, BackLightMonitorManager manager) {
        this.mMonitor = monitor;
        this.mCommonData = manager.getBackLightCommonData();
        DisplayEffectMonitor displayEffectMonitor = this.mMonitor;
        displayEffectMonitor.getClass();
        this.mWindowManagerBrightnessPrinter = new ParamLogPrinter(TYPE_WINDOW_MANAGER_BRIGHTNESS, TAG);
        displayEffectMonitor = this.mMonitor;
        displayEffectMonitor.getClass();
        this.mManualBrightnessPrinter = new ParamLogPrinter(TYPE_MANUAL_BRIGHTNESS, TAG);
    }

    public boolean isParamOwner(String paramType) {
        if (paramType.equals(TYPE_WINDOW_MANAGER_BRIGHTNESS) || paramType.equals(TYPE_MANUAL_BRIGHTNESS) || paramType.equals("brightnessMode")) {
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
        if (paramType.equals(TYPE_WINDOW_MANAGER_BRIGHTNESS)) {
            windowManagerBrightness(params);
        } else if (paramType.equals(TYPE_MANUAL_BRIGHTNESS)) {
            manualBrightness(params);
        } else if (paramType.equals("brightnessMode")) {
            brightnessMode(params);
        } else {
            Slog.e(TAG, "sendMonitorParam() undefine paramType: " + paramType);
        }
    }

    public void triggerUploadTimer() {
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void windowManagerBrightness(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && windowManagerBrightnessCheckParamValid(params)) {
            windowManagerBrightnessPrint(((Integer) params.get(PARAM_BRIGHTNESS)).intValue(), (String) params.get("packageName"));
        }
    }

    private boolean windowManagerBrightnessCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (!(params.get(PARAM_BRIGHTNESS) instanceof Integer)) {
            Slog.e(TAG, "windowManagerBrightnessCheckParamValid() can't get param: brightness");
            return false;
        } else if (params.get("packageName") instanceof String) {
            return true;
        } else {
            Slog.e(TAG, "windowManagerBrightnessCheckParamValid() can't get param: packageName");
            return false;
        }
    }

    private void windowManagerBrightnessPrint(int brightness, String packageName) {
        if (this.mWindowManagerBrightnessPackageNameLast.equals(packageName)) {
            if (HWDEBUG) {
                Slog.d(TAG, "windowManagerBrightnessPrint() update brightness=" + brightness + ", packageName=" + packageName);
            }
            this.mWindowManagerBrightnessPrinter.updateParam(brightness, packageName);
        } else if (brightness == -255 && packageName.equals("android")) {
            if (HWDEBUG) {
                Slog.d(TAG, "windowManagerBrightnessPrint() brightness reset to normal");
            }
            this.mWindowManagerBrightnessPrinter.resetParam(brightness, packageName);
            this.mWindowManagerBrightnessPackageNameLast = packageName;
            this.mCommonData.setWindowManagerBrightnessMode(false);
        } else {
            if (HWDEBUG) {
                Slog.d(TAG, "windowManagerBrightnessPrint() start brightness=" + brightness + ", packageName=" + packageName);
            }
            if (this.mWindowManagerBrightnessPackageNameLast.equals("android")) {
                this.mWindowManagerBrightnessPrinter.updateParam(brightness, packageName);
            } else {
                this.mWindowManagerBrightnessPrinter.changeName(brightness, packageName);
            }
            this.mWindowManagerBrightnessPackageNameLast = packageName;
            this.mCommonData.setWindowManagerBrightnessMode(true);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void manualBrightness(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && manualBrightnessCheckParamValid(params)) {
            manualBrightnessPrint(((Integer) params.get(PARAM_BRIGHTNESS)).intValue(), (String) params.get("packageName"));
        }
    }

    private boolean manualBrightnessCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (!(params.get(PARAM_BRIGHTNESS) instanceof Integer)) {
            Slog.e(TAG, "manualBrightnessCheckParamValid() can't get param: brightness");
            return false;
        } else if (params.get("packageName") instanceof String) {
            return true;
        } else {
            Slog.e(TAG, "manualBrightnessCheckParamValid() can't get param: packageName");
            return false;
        }
    }

    private void manualBrightnessPrint(int brightness, String packageName) {
        if (this.mManualBrightnessPackageNameLast == null) {
            this.mManualBrightnessPackageNameLast = packageName;
        }
        if (this.mManualBrightnessPackageNameLast.equals(packageName)) {
            this.mManualBrightnessPrinter.updateParam(brightness, packageName);
            return;
        }
        this.mManualBrightnessPrinter.changeName(brightness, packageName);
        this.mManualBrightnessPackageNameLast = packageName;
    }

    private void brightnessMode(ArrayMap<String, Object> params) {
        if (!this.mCommonData.isCommercialVersion() && brightnessModeCheckParamValid(params)) {
            String modeName = (String) params.get("brightnessMode");
            String packageName = (String) params.get("packageName");
            try {
                BrightnessMode newMode = BrightnessMode.valueOf(modeName);
                brightnessModePrint(newMode, packageName);
                brightnessModeCheckRecovered(packageName);
                this.mBrightnessModeLast = newMode;
                this.mCommonData.setBrightnessMode(newMode);
                this.mBrightnessModePackageNameLast = packageName;
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "brightnessMode() input error! brightnessMode=" + modeName);
            }
        }
    }

    private boolean brightnessModeCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (!(params.get("brightnessMode") instanceof String)) {
            Slog.e(TAG, "brightnessModeCheckParamValid() can't get param: brightnessMode");
            return false;
        } else if (params.get("packageName") instanceof String) {
            return true;
        } else {
            Slog.e(TAG, "brightnessModeCheckParamValid() can't get param: packageName");
            return false;
        }
    }

    private void brightnessModePrint(BrightnessMode mode, String packageName) {
        if (this.mBrightnessModeLast == null || this.mBrightnessModePackageNameLast == null) {
            if (HWFLOW) {
                Slog.i(TAG, "brightnessMode init " + mode + " by " + packageName);
            }
            return;
        }
        boolean modeChanged = this.mBrightnessModeLast != mode;
        boolean packageNameChanged = this.mBrightnessModePackageNameLast.equals(packageName) ^ 1;
        if (modeChanged && packageNameChanged) {
            if (HWFLOW) {
                Slog.i(TAG, "brightnessMode " + this.mBrightnessModeLast + " by " + this.mBrightnessModePackageNameLast + " -> " + mode + " by " + packageName);
            }
        } else if (modeChanged) {
            if (HWFLOW) {
                Slog.i(TAG, "brightnessMode " + this.mBrightnessModeLast + " -> " + mode + " by " + packageName);
            }
        } else if (packageNameChanged && HWFLOW) {
            Slog.i(TAG, "brightnessMode " + this.mBrightnessModeLast + " by " + this.mBrightnessModePackageNameLast + " -> " + packageName);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void brightnessModeCheckRecovered(String packageName) {
        if (this.mBrightnessModeLast != null && this.mBrightnessModePackageNameLast != null && !this.mBrightnessModePackageNameLast.equals(packageName)) {
            if (!(this.mBrightnessModeBackup == null || this.mBrightnessModeBackup == this.mBrightnessModeLast)) {
                brightnessModeUploadPackageNameIfNeed(this.mBrightnessModePackageNameLast);
            }
            this.mBrightnessModeBackup = this.mBrightnessModeLast;
        }
    }

    private void brightnessModeUploadPackageNameIfNeed(String packageName) {
        if (this.mBrightnessModePackageNameWhiteList.contains(packageName)) {
            if (HWDEBUG) {
                Slog.d(TAG, "brightnessModeUploadPackageNameIfNeed " + packageName + " is in white list");
            }
        } else if (this.mBrightnessModePackageNameUploadedList.contains(packageName)) {
            if (HWFLOW) {
                Slog.i(TAG, "brightnessModeUploadPackageNameIfNeed " + packageName + " already uploaded");
            }
        } else {
            if (!this.mMonitor.isAppForeground(packageName)) {
                this.mBrightnessModePackageNameUploadedList.add(packageName);
                brightnessModeUploadPackageName(packageName);
            } else if (HWFLOW) {
                Slog.i(TAG, "brightnessModeUploadPackageNameIfNeed " + packageName + " is still in foreground");
            }
        }
    }

    private void brightnessModeUploadPackageName(String packageName) {
        EventStream stream = IMonitor.openEventStream(932010103);
        stream.setParam((short) 0, packageName);
        IMonitor.sendEvent(stream);
        IMonitor.closeEventStream(stream);
        if (HWFLOW) {
            Slog.i(TAG, "brightnessModeUploadPackageName() " + packageName);
        }
    }
}
