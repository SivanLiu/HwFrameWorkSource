package com.android.server.display;

import android.util.ArrayMap;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.BackLightCommonData.BrightnessMode;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import com.android.server.display.DisplayEffectMonitor.ParamLogPrinter;
import java.util.List;
import java.util.Objects;

class BrightnessSettingsMonitor implements MonitorModule {
    private static final int CURVE_MAX_SIZE = 38;
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String PARAM_BRIGHTNESS = "brightness";
    private static final String PARAM_BRIGHTNESS_MODE = "brightnessMode";
    private static final String PARAM_PACKAGE_NAME = "packageName";
    private static final String PARAM_PERSONALIZED_CURVE = "personalizedCurve";
    private static final String PARAM_PERSONALIZED_PARAM = "personalizedParam";
    private static final int PARAM_SIZE = 9;
    private static final String TAG = "BrightnessSettingsMonitor";
    private static final String TYPE_BRIGHTNESS_MODE = "brightnessMode";
    private static final String TYPE_MANUAL_BRIGHTNESS = "manualBrightness";
    private static final String TYPE_PERSONALIZED_CURVE_AND_PARAM = "personalizedCurveAndParam";
    private static final String TYPE_WINDOW_MANAGER_BRIGHTNESS = "windowManagerBrightness";
    private BrightnessMode mBrightnessModeLast;
    private final BackLightCommonData mCommonData;
    private ParamLogPrinter mManualBrightnessPrinter;
    private final DisplayEffectMonitor mMonitor;
    private String mWindowManagerBrightnessPackageNameLast = "android";
    private ParamLogPrinter mWindowManagerBrightnessPrinter;

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public BrightnessSettingsMonitor(DisplayEffectMonitor monitor, BackLightMonitorManager manager) {
        this.mMonitor = monitor;
        this.mCommonData = manager.getBackLightCommonData();
        if (!this.mCommonData.isCommercialVersion()) {
            DisplayEffectMonitor displayEffectMonitor = this.mMonitor;
            Objects.requireNonNull(displayEffectMonitor);
            this.mWindowManagerBrightnessPrinter = new ParamLogPrinter(TYPE_WINDOW_MANAGER_BRIGHTNESS, TAG);
            displayEffectMonitor = this.mMonitor;
            Objects.requireNonNull(displayEffectMonitor);
            this.mManualBrightnessPrinter = new ParamLogPrinter(TYPE_MANUAL_BRIGHTNESS, TAG);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0049 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0049 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0049 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0049 A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isParamOwner(String paramType) {
        boolean z;
        int hashCode = paramType.hashCode();
        if (hashCode == -212802098) {
            if (paramType.equals(TYPE_WINDOW_MANAGER_BRIGHTNESS)) {
                z = false;
                switch (z) {
                    case false:
                    case true:
                    case true:
                    case true:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == -138212075) {
            if (paramType.equals(TYPE_PERSONALIZED_CURVE_AND_PARAM)) {
                z = true;
                switch (z) {
                    case false:
                    case true:
                    case true:
                    case true:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 1984317844) {
            if (paramType.equals("brightnessMode")) {
                z = true;
                switch (z) {
                    case false:
                    case true:
                    case true:
                    case true:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == 2125230263 && paramType.equals(TYPE_MANUAL_BRIGHTNESS)) {
            z = true;
            switch (z) {
                case false:
                case true:
                case true:
                case true:
                    return true;
                default:
                    return false;
            }
        }
        z = true;
        switch (z) {
            case false:
            case true:
            case true:
            case true:
                break;
            default:
                break;
        }
    }

    public void sendMonitorParam(ArrayMap<String, Object> params) {
        if (params == null || !(params.get(MonitorModule.PARAM_TYPE) instanceof String)) {
            Slog.e(TAG, "sendMonitorParam() input params format error!");
            return;
        }
        String paramType = (String) params.get(MonitorModule.PARAM_TYPE);
        Object obj = -1;
        int hashCode = paramType.hashCode();
        if (hashCode != -212802098) {
            if (hashCode != -138212075) {
                if (hashCode != 1984317844) {
                    if (hashCode == 2125230263 && paramType.equals(TYPE_MANUAL_BRIGHTNESS)) {
                        obj = 1;
                    }
                } else if (paramType.equals("brightnessMode")) {
                    obj = 2;
                }
            } else if (paramType.equals(TYPE_PERSONALIZED_CURVE_AND_PARAM)) {
                obj = 3;
            }
        } else if (paramType.equals(TYPE_WINDOW_MANAGER_BRIGHTNESS)) {
            obj = null;
        }
        switch (obj) {
            case null:
                windowManagerBrightness(params);
                break;
            case 1:
                manualBrightness(params);
                break;
            case 2:
                brightnessMode(params);
                break;
            case 3:
                personalizedCurveAndParam(params);
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

    /* JADX WARNING: Missing block: B:9:0x0030, code skipped:
            return;
     */
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
        String str;
        StringBuilder stringBuilder;
        if (this.mWindowManagerBrightnessPackageNameLast.equals(packageName)) {
            if (HWDEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("windowManagerBrightnessPrint() update brightness=");
                stringBuilder.append(brightness);
                stringBuilder.append(", packageName=");
                stringBuilder.append(packageName);
                Slog.d(str, stringBuilder.toString());
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
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("windowManagerBrightnessPrint() start brightness=");
                stringBuilder.append(brightness);
                stringBuilder.append(", packageName=");
                stringBuilder.append(packageName);
                Slog.d(str, stringBuilder.toString());
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

    /* JADX WARNING: Missing block: B:9:0x002d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void manualBrightness(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && manualBrightnessCheckParamValid(params)) {
            this.mManualBrightnessPrinter.updateParam(((Integer) params.get(PARAM_BRIGHTNESS)).intValue(), "unknow");
        }
    }

    private boolean manualBrightnessCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (params.get(PARAM_BRIGHTNESS) instanceof Integer) {
            return true;
        }
        Slog.e(TAG, "manualBrightnessCheckParamValid() can't get param: brightness");
        return false;
    }

    private void brightnessMode(ArrayMap<String, Object> params) {
        if (!this.mCommonData.isCommercialVersion() && brightnessModeCheckParamValid(params)) {
            String modeName = (String) params.get("brightnessMode");
            try {
                BrightnessMode newMode = BrightnessMode.valueOf(modeName);
                brightnessModePrint(newMode);
                this.mBrightnessModeLast = newMode;
                this.mCommonData.setBrightnessMode(newMode);
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("brightnessMode() input error! brightnessMode=");
                stringBuilder.append(modeName);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    private boolean brightnessModeCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (params.get("brightnessMode") instanceof String) {
            return true;
        }
        Slog.e(TAG, "brightnessModeCheckParamValid() can't get param: brightnessMode");
        return false;
    }

    private void brightnessModePrint(BrightnessMode mode) {
        String str;
        StringBuilder stringBuilder;
        if (this.mBrightnessModeLast == null) {
            if (HWFLOW) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("brightnessMode init ");
                stringBuilder.append(mode);
                Slog.i(str, stringBuilder.toString());
            }
            return;
        }
        if (this.mBrightnessModeLast != mode && HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("brightnessMode ");
            stringBuilder.append(this.mBrightnessModeLast);
            stringBuilder.append(" -> ");
            stringBuilder.append(mode);
            Slog.i(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:9:0x002c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void personalizedCurveAndParam(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && personalizedCurveAndParamCheckParamValid(params)) {
            personalizedCurveAndParamUpload((List) params.get(PARAM_PERSONALIZED_CURVE), (List) params.get(PARAM_PERSONALIZED_PARAM));
        }
    }

    private boolean personalizedCurveAndParamCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (params.get(PARAM_PERSONALIZED_CURVE) instanceof List) {
            List<Short> curveList = (List) params.get(PARAM_PERSONALIZED_CURVE);
            int curveListSize = curveList.size();
            String str;
            if (curveListSize == 0 || curveListSize > 38) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("personalizedCurveAndParamCheckParamValid() curveList size error: ");
                stringBuilder.append(curveListSize);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
            try {
                for (Object obj : curveList) {
                    if (!(obj instanceof Short)) {
                        str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("personalizedCurveAndParamCheckParamValid() curveList type error obj = ");
                        stringBuilder2.append(obj);
                        Slog.e(str, stringBuilder2.toString());
                        return false;
                    }
                }
                if (params.get(PARAM_PERSONALIZED_PARAM) instanceof List) {
                    List<Float> paramList = (List) params.get(PARAM_PERSONALIZED_PARAM);
                    int paramListSize = paramList.size();
                    String str2;
                    if (paramListSize != 9) {
                        str2 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("personalizedCurveAndParamCheckParamValid() paramList size error: ");
                        stringBuilder3.append(paramListSize);
                        Slog.e(str2, stringBuilder3.toString());
                        return false;
                    }
                    try {
                        for (Object obj2 : paramList) {
                            if (!(obj2 instanceof Float)) {
                                str2 = TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("personalizedCurveAndParamCheckParamValid() paramList type error obj = ");
                                stringBuilder4.append(obj2);
                                Slog.e(str2, stringBuilder4.toString());
                                return false;
                            }
                        }
                        return true;
                    } catch (ClassCastException e) {
                        Slog.e(TAG, "personalizedCurveAndParamCheckParamValid() paramList type error");
                        return false;
                    }
                }
                Slog.e(TAG, "personalizedCurveAndParamCheckParamValid() can't get param: personalizedParam");
                return false;
            } catch (ClassCastException e2) {
                Slog.e(TAG, "personalizedCurveAndParamCheckParamValid() curveList type error");
                return false;
            }
        }
        Slog.e(TAG, "personalizedCurveAndParamCheckParamValid() can't get param: personalizedCurve");
        return false;
    }

    private void personalizedCurveAndParamUpload(List<Short> curveList, List<Float> paramList) {
        EventStream stream = IMonitor.openEventStream(932010700);
        int i = 0;
        for (Short level : curveList) {
            Object[] objArr = new Object[1];
            i++;
            objArr[0] = Integer.valueOf(i);
            stream.setParam(String.format("B%02d", objArr), level.shortValue());
        }
        try {
            stream.setParam("P01EnvAdapt", ((Float) paramList.get(0)).floatValue());
            stream.setParam("P02ContAdapt", ((Float) paramList.get(1)).floatValue());
            stream.setParam("P03VisualAcuity", ((Float) paramList.get(2)).floatValue());
            stream.setParam("P04RecogTime", ((Float) paramList.get(3)).floatValue());
            stream.setParam("P05Tolerance", ((Float) paramList.get(4)).floatValue());
            stream.setParam("P06Reflect", ((Float) paramList.get(7)).floatValue());
            stream.setParam("P07Power", ((Float) paramList.get(8)).floatValue());
            IMonitor.sendEvent(stream);
            IMonitor.closeEventStream(stream);
            if (HWFLOW) {
                Slog.i(TAG, "personalizedCurveAndParamUpload done");
            }
        } catch (IndexOutOfBoundsException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("personalizedCurveAndParamUpload() IndexOutOfBoundsException paramList.size()=");
            stringBuilder.append(paramList.size());
            Slog.e(str, stringBuilder.toString());
            IMonitor.closeEventStream(stream);
        }
    }
}
