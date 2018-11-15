package com.android.server.display;

import android.util.ArrayMap;
import android.util.HwLog;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import huawei.com.android.server.policy.stylus.StylusGestureSettings;
import java.util.Arrays;

class AmbientLightMonitor implements MonitorModule {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String PARAM_BRIGHTNESS_MODE = "brightnessMode";
    private static final String PARAM_COLOR_TEMP_VALUE = "colorTempValue";
    private static final String PARAM_DURATION_IN_MS = "durationInMs";
    private static final String PARAM_LIGHT_VALUE = "lightValue";
    private static final String PARAM_RAW_LIGHT_VALUE = "rawLightValue";
    private static final String TAG = "AmbientLightMonitor";
    private static final String TYPE_AMBIENT_COLOR_TEMP_COLLECTION = "ambientColorTempCollection";
    private static final String TYPE_AMBIENT_LIGHT_COLLECTION = "ambientLightCollection";
    private AmbientLightCollectionData mAmbientLightDataInAPP = new AmbientLightCollectionData(Mode.APP);
    private AmbientLightCollectionData mAmbientLightDataInAuto = new AmbientLightCollectionData(Mode.AUTO);
    private AmbientLightCollectionData mAmbientLightDataInFRONT = new AmbientLightCollectionData(Mode.FRONT);
    private AmbientLightCollectionData mAmbientLightDataInManual = new AmbientLightCollectionData(Mode.MANUAL);
    private ColorTempCollector mColorTempCollector;
    private final BackLightCommonData mCommonData;

    private static class AmbientLightCollectionData {
        private static final int UPLOAD_INTERVAL_TIME_IN_MS = 60000;
        private int lux1000To5000TimeInMs;
        private int lux200To500TimeInMs;
        private int lux500To1000TimeInMs;
        private int lux50To200TimeInMs;
        private int luxAbove5000TimeInMs;
        private int luxBelow50TimeInMs;
        private final Mode mMode;
        private int mTotalTimeInMs;

        public enum Mode {
            AUTO,
            MANUAL,
            APP,
            FRONT
        }

        public AmbientLightCollectionData(Mode mode) {
            this.mMode = mode;
        }

        public void collect(int lux, int durationInMs) {
            if (lux < 0 || durationInMs <= 0 || durationInMs >= 5000) {
                String str = AmbientLightMonitor.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AmbientLightCollectionData.collect() error!input params out of range: lux=");
                stringBuilder.append(lux);
                stringBuilder.append(", durationInMs=");
                stringBuilder.append(durationInMs);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            if (AmbientLightMonitor.HWDEBUG) {
                String str2 = AmbientLightMonitor.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("collect lux=");
                stringBuilder2.append(lux);
                stringBuilder2.append(", durationInMs=");
                stringBuilder2.append(durationInMs);
                stringBuilder2.append(", mode=");
                stringBuilder2.append(this.mMode);
                Slog.d(str2, stringBuilder2.toString());
            }
            if (lux <= 50) {
                this.luxBelow50TimeInMs += durationInMs;
            } else if (lux <= 200) {
                this.lux50To200TimeInMs += durationInMs;
            } else if (lux <= 500) {
                this.lux200To500TimeInMs += durationInMs;
            } else if (lux <= 1000) {
                this.lux500To1000TimeInMs += durationInMs;
            } else if (lux <= 5000) {
                this.lux1000To5000TimeInMs += durationInMs;
            } else {
                this.luxAbove5000TimeInMs += durationInMs;
            }
            this.mTotalTimeInMs += durationInMs;
            if (this.mTotalTimeInMs >= 60000) {
                upload();
                clean();
            }
        }

        private void upload() {
            String str;
            StringBuilder stringBuilder;
            if (AmbientLightMonitor.HWDEBUG) {
                str = AmbientLightMonitor.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("upload ");
                stringBuilder.append(this);
                Slog.d(str, stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DUBAI_TAG_");
            stringBuilder2.append(this.mMode);
            stringBuilder2.append("_AMBIENT_LIGHT");
            str = stringBuilder2.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append("d0=");
            stringBuilder.append(this.luxBelow50TimeInMs);
            stringBuilder.append(" d1=");
            stringBuilder.append(this.lux50To200TimeInMs);
            stringBuilder.append(" d2=");
            stringBuilder.append(this.lux200To500TimeInMs);
            stringBuilder.append(" d3=");
            stringBuilder.append(this.lux500To1000TimeInMs);
            stringBuilder.append(" d4=");
            stringBuilder.append(this.lux1000To5000TimeInMs);
            stringBuilder.append(" d5=");
            stringBuilder.append(this.luxAbove5000TimeInMs);
            HwLog.dubaie(str, stringBuilder.toString());
        }

        private void clean() {
            this.mTotalTimeInMs = 0;
            this.luxBelow50TimeInMs = 0;
            this.lux50To200TimeInMs = 0;
            this.lux200To500TimeInMs = 0;
            this.lux500To1000TimeInMs = 0;
            this.lux1000To5000TimeInMs = 0;
            this.luxAbove5000TimeInMs = 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AmbientLightCollectionData ");
            stringBuilder.append(this.mMode);
            stringBuilder.append(", lux <50:");
            stringBuilder.append(this.luxBelow50TimeInMs / 1000);
            stringBuilder.append("s, 50~200:");
            stringBuilder.append(this.lux50To200TimeInMs / 1000);
            stringBuilder.append("s, 200~500:");
            stringBuilder.append(this.lux200To500TimeInMs / 1000);
            stringBuilder.append("s, 500~1000:");
            stringBuilder.append(this.lux500To1000TimeInMs / 1000);
            stringBuilder.append("s, 1000~5000:");
            stringBuilder.append(this.lux1000To5000TimeInMs / 1000);
            stringBuilder.append("s, >5000:");
            stringBuilder.append(this.luxAbove5000TimeInMs / 1000);
            stringBuilder.append(StylusGestureSettings.STYLUS_GESTURE_S_SUFFIX);
            return stringBuilder.toString();
        }
    }

    private static class ColorTempCollector {
        private static final int COLOR_TEMP_MAX = 8000;
        private static final int COLOR_TEMP_MIN = 2500;
        private static final int COLOR_TEMP_MIN2 = 4000;
        private static final int LEVEL_NUM = 7;
        private static final int UPLOAD_UNIT_IN_MS = 60000;
        private int[] timeInMs;

        private ColorTempCollector() {
            this.timeInMs = new int[7];
        }

        public void collect(int colorTemp, int durationInMs) {
            if (colorTemp > 0 && durationInMs > 0 && durationInMs < 5000) {
                int index;
                if (colorTemp < COLOR_TEMP_MIN) {
                    index = 0;
                } else if (colorTemp < 4000) {
                    index = 1;
                } else if (colorTemp >= COLOR_TEMP_MAX) {
                    index = 6;
                } else {
                    index = ((colorTemp - 4000) / 1000) + 2;
                }
                int[] iArr = this.timeInMs;
                iArr[index] = iArr[index] + durationInMs;
                if (AmbientLightMonitor.HWDEBUG) {
                    String str = AmbientLightMonitor.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ColorTempCollector.collect() colorTemp=");
                    stringBuilder.append(colorTemp);
                    stringBuilder.append(", index=");
                    stringBuilder.append(index);
                    stringBuilder.append(", durationInMs=");
                    stringBuilder.append(durationInMs);
                    Slog.d(str, stringBuilder.toString());
                }
            }
        }

        public void upload() {
            if (isEmpty()) {
                clean();
                return;
            }
            byte[] timeInMinute = new byte[7];
            int index = 0;
            for (int index2 = 0; index2 < 7; index2++) {
                int minute = this.timeInMs[index2] / 60000;
                byte b = Byte.MAX_VALUE;
                if (minute < 127) {
                    b = (byte) minute;
                }
                timeInMinute[index2] = b;
            }
            if (AmbientLightMonitor.HWFLOW) {
                String str = AmbientLightMonitor.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ColorTempCollector.upload() ");
                stringBuilder.append(Arrays.toString(timeInMinute));
                Slog.i(str, stringBuilder.toString());
            }
            EventStream stream = IMonitor.openEventStream(932040100);
            while (index < 7) {
                if (timeInMinute[index] > (byte) 0) {
                    stream.setParam((short) index, timeInMinute[index]);
                }
                index++;
            }
            IMonitor.sendEvent(stream);
            IMonitor.closeEventStream(stream);
            clean();
        }

        private boolean isEmpty() {
            for (int time : this.timeInMs) {
                if (time >= 60000) {
                    return false;
                }
            }
            return true;
        }

        private void clean() {
            Arrays.fill(this.timeInMs, 0);
        }
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

    public AmbientLightMonitor(DisplayEffectMonitor monitor, BackLightMonitorManager manager) {
        this.mCommonData = manager.getBackLightCommonData();
        if (!this.mCommonData.isCommercialVersion()) {
            this.mColorTempCollector = new ColorTempCollector();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0029 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x002a A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0029 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x002a A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isParamOwner(String paramType) {
        boolean z;
        int hashCode = paramType.hashCode();
        if (hashCode == -1209196900) {
            if (paramType.equals(TYPE_AMBIENT_LIGHT_COLLECTION)) {
                z = false;
                switch (z) {
                    case false:
                    case true:
                        break;
                    default:
                        break;
                }
            }
        } else if (hashCode == -313342435 && paramType.equals(TYPE_AMBIENT_COLOR_TEMP_COLLECTION)) {
            z = true;
            switch (z) {
                case false:
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
        if (hashCode != -1209196900) {
            if (hashCode == -313342435 && paramType.equals(TYPE_AMBIENT_COLOR_TEMP_COLLECTION)) {
                obj = 1;
            }
        } else if (paramType.equals(TYPE_AMBIENT_LIGHT_COLLECTION)) {
            obj = null;
        }
        switch (obj) {
            case null:
                ambientLightCollection(params);
                break;
            case 1:
                ambientColorTempCollection(params);
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
        if (this.mColorTempCollector != null) {
            this.mColorTempCollector.upload();
        }
    }

    private void ambientLightCollection(ArrayMap<String, Object> params) {
        if (ambientLightCollectionCheckParamValid(params)) {
            int lightValue = ((Integer) params.get(PARAM_LIGHT_VALUE)).intValue();
            int durationInMs = ((Integer) params.get(PARAM_DURATION_IN_MS)).intValue();
            String mode = (String) params.get(PARAM_BRIGHTNESS_MODE);
            if (this.mCommonData.isWindowManagerBrightnessMode()) {
                this.mAmbientLightDataInAPP.collect(lightValue, durationInMs);
            } else if (mode.equals("AUTO")) {
                this.mAmbientLightDataInAuto.collect(lightValue, durationInMs);
                if (params.get(PARAM_RAW_LIGHT_VALUE) instanceof Integer) {
                    this.mAmbientLightDataInFRONT.collect(((Integer) params.get(PARAM_RAW_LIGHT_VALUE)).intValue(), durationInMs);
                }
            } else {
                this.mAmbientLightDataInManual.collect(lightValue, durationInMs);
            }
        }
    }

    private boolean ambientLightCollectionCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (!(params.get(PARAM_LIGHT_VALUE) instanceof Integer)) {
            Slog.e(TAG, "ambientLightCollectionCheckParamValid() can't get param: lightValue");
            return false;
        } else if (!(params.get(PARAM_DURATION_IN_MS) instanceof Integer)) {
            Slog.e(TAG, "ambientLightCollectionCheckParamValid() can't get param: durationInMs");
            return false;
        } else if (params.get(PARAM_BRIGHTNESS_MODE) instanceof String) {
            String mode = (String) params.get(PARAM_BRIGHTNESS_MODE);
            if (mode.equals("AUTO") || mode.equals("MANUAL")) {
                return true;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ambientLightCollectionCheckParamValid() brightnessMode value error: ");
            stringBuilder.append(mode);
            Slog.e(str, stringBuilder.toString());
            return false;
        } else {
            Slog.e(TAG, "ambientLightCollectionCheckParamValid() can't get param: brightnessMode");
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0036, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void ambientColorTempCollection(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && ambientColorTempCollectionCheckParamValid(params)) {
            this.mColorTempCollector.collect(((Integer) params.get(PARAM_COLOR_TEMP_VALUE)).intValue(), ((Integer) params.get(PARAM_DURATION_IN_MS)).intValue());
        }
    }

    private boolean ambientColorTempCollectionCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (!(params.get(PARAM_COLOR_TEMP_VALUE) instanceof Integer)) {
            Slog.e(TAG, "ambientColorTempCollectionCheckParamValid() can't get param: colorTempValue");
            return false;
        } else if (params.get(PARAM_DURATION_IN_MS) instanceof Integer) {
            return true;
        } else {
            Slog.e(TAG, "ambientColorTempCollectionCheckParamValid() can't get param: durationInMs");
            return false;
        }
    }
}
