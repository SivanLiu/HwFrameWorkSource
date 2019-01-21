package com.android.server.display;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import com.android.server.display.DisplayEffectMonitor.ParamLogPrinter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

class BrightnessSeekBarMonitor implements MonitorModule {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String MONITOR_TARGET_PACKAGE = "android.uid.systemui";
    private static final long MOVE_INTERVAL_TIME_IN_MS = 2000;
    private static final long MOVE_LIST_INTERVAL_TIME_IN_MS = 15000;
    private static final int MOVE_MERGE_LUX_CHANGE_MIN_THRESHOLD = 5;
    private static final int MOVE_MERGE_LUX_CHANGE_PERCENT = 50;
    private static final String PARAM_BRIGHTNESS = "brightness";
    private static final String PARAM_BRIGHTNESS_MODE = "brightnessMode";
    private static final String PARAM_IS_ENABLE = "isEnable";
    private static final String PARAM_LIGHT_VALUE = "lightValue";
    private static final String PARAM_PACKAGE_NAME = "packageName";
    private static final int POWER_MODE_DEFAULT = 2;
    private static final String POWER_MODE_PROP = "persist.sys.smart_power";
    private static final String TAG = "BrightnessSeekBarMonitor";
    private static final String TYPE_ALGO_DEFAULT_BRIGHTNESS = "algoDefaultBrightness";
    private static final String TYPE_EYE_PROTECT = "eyeProtect";
    private static final String TYPE_TEMP_AUTO_BRIGHTNESS = "tempAutoBrightness";
    private static final String TYPE_TEMP_MANUAL_BRIGHTNESS = "tempManualBrightness";
    private static final Comparator<SeekBarMovement> mComparator = new Comparator<SeekBarMovement>() {
        public int compare(SeekBarMovement a, SeekBarMovement b) {
            return (int) (a.startTime - b.startTime);
        }
    };
    private int mAlgoDefaultBrightness;
    private SeekBarMovement mAutoModeMovement;
    private List<SeekBarMovement> mAutoModeMovementList;
    private final BackLightCommonData mCommonData;
    private boolean mIsEyeProtectEnable;
    private boolean mIsManualModeReceiveAmbientLight;
    private SeekBarMovement mManualModeMovement;
    private List<SeekBarMovement> mManualModeMovementList;
    private final DisplayEffectMonitor mMonitor;
    private SeekBarMovementUploader mSeekBarMovementUploader;
    private String mTempAutoBrightnessPackageNameLast;
    private ParamLogPrinter mTempAutoBrightnessPrinter;
    private String mTempManualBrightnessPackageNameLast;
    private ParamLogPrinter mTempManualBrightnessPrinter;

    private static class SeekBarMovement {
        public int algoDefaultLevel;
        public int endLevel;
        public long endTime;
        public String foregroundPackageName;
        public boolean isAutoMode;
        public boolean powerSave;
        public boolean protectEye;
        public int startLevel;
        public int startLux;
        public long startTime;
        public boolean thermalLimited;

        private SeekBarMovement() {
        }

        /* synthetic */ SeekBarMovement(AnonymousClass1 x0) {
            this();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SeekBarMovement ");
            stringBuilder.append(this.isAutoMode ? "auto" : "manual");
            stringBuilder.append(", ");
            stringBuilder.append(this.startLevel);
            stringBuilder.append(" -> ");
            stringBuilder.append(this.endLevel);
            stringBuilder.append(", lux ");
            stringBuilder.append(this.startLux);
            stringBuilder.append(", last ");
            stringBuilder.append(this.endTime - this.startTime);
            stringBuilder.append("ms, default ");
            stringBuilder.append(this.algoDefaultLevel);
            stringBuilder.append(", app ");
            stringBuilder.append(this.foregroundPackageName);
            stringBuilder.append(", protectEye ");
            stringBuilder.append(this.protectEye);
            stringBuilder.append(", powerSave ");
            stringBuilder.append(this.powerSave);
            stringBuilder.append(", thermalLimited ");
            stringBuilder.append(this.thermalLimited);
            return stringBuilder.toString();
        }
    }

    private static class SeekBarMovementUploader {
        private static final int HOURS_PER_DAY = 24;
        private static final byte MODE_AUTO = (byte) 1;
        private static final byte MODE_MANUAL = (byte) 2;
        private static final int UPLOAD_TIMES_LIMIT_PER_DAY = 30;
        private static final int UPLOAD_TIMES_LIMIT_PER_HOUR = 10;
        private int mHourCount;
        private List<SeekBarMovementUploadData> mUploadList = new ArrayList();
        private int mUploadTimesPerDay;

        private static class SeekBarMovementUploadData {
            public short adjustEndLevel;
            public short adjustStartLevel;
            public short algoCalcDefaultLevel;
            public short ambientLightLux;
            public String foregroundPackageName;
            public byte mode;
            public boolean powerSave;
            public boolean protectEye;
            public boolean thermalLimited;

            private SeekBarMovementUploadData() {
            }

            /* synthetic */ SeekBarMovementUploadData(AnonymousClass1 x0) {
                this();
            }

            public String toString() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SeekBarMovementUploadData mode ");
                stringBuilder.append(this.mode);
                stringBuilder.append(", ");
                stringBuilder.append(this.adjustStartLevel);
                stringBuilder.append(" -> ");
                stringBuilder.append(this.adjustEndLevel);
                stringBuilder.append(", ambientLightLux ");
                stringBuilder.append(this.ambientLightLux);
                stringBuilder.append(", algoCalcDefaultLevel ");
                stringBuilder.append(this.algoCalcDefaultLevel);
                stringBuilder.append(", app ");
                stringBuilder.append(this.foregroundPackageName);
                stringBuilder.append(", protectEye ");
                stringBuilder.append(this.protectEye);
                stringBuilder.append(", powerSave ");
                stringBuilder.append(this.powerSave);
                stringBuilder.append(", thermalLimited ");
                stringBuilder.append(this.thermalLimited);
                return stringBuilder.toString();
            }
        }

        public void addData(SeekBarMovement movement) {
            if (this.mUploadList.size() < 10 && this.mUploadTimesPerDay < 30) {
                byte b = (byte) 1;
                this.mUploadTimesPerDay++;
                SeekBarMovementUploadData data = new SeekBarMovementUploadData();
                if (!movement.isAutoMode) {
                    b = MODE_MANUAL;
                }
                data.mode = b;
                data.adjustStartLevel = (short) movement.startLevel;
                data.adjustEndLevel = (short) movement.endLevel;
                int i = 32767;
                if (movement.startLux < 32767) {
                    i = movement.startLux;
                }
                data.ambientLightLux = (short) i;
                data.algoCalcDefaultLevel = (short) movement.algoDefaultLevel;
                data.foregroundPackageName = movement.foregroundPackageName;
                data.protectEye = movement.protectEye;
                data.powerSave = movement.powerSave;
                data.thermalLimited = movement.thermalLimited;
                this.mUploadList.add(data);
            }
        }

        public void upload() {
            if (!this.mUploadList.isEmpty()) {
                for (SeekBarMovementUploadData data : this.mUploadList) {
                    EventStream stream = IMonitor.openEventStream(932010102);
                    stream.setParam((short) 0, data.mode);
                    stream.setParam((short) 1, data.adjustStartLevel);
                    stream.setParam((short) 2, data.adjustEndLevel);
                    stream.setParam((short) 3, data.ambientLightLux);
                    stream.setParam((short) 4, data.algoCalcDefaultLevel);
                    stream.setParam((short) 5, data.foregroundPackageName);
                    stream.setParam((short) 6, Boolean.valueOf(data.protectEye));
                    stream.setParam((short) 7, Boolean.valueOf(data.powerSave));
                    stream.setParam((short) 8, Boolean.valueOf(data.thermalLimited));
                    IMonitor.sendEvent(stream);
                    IMonitor.closeEventStream(stream);
                    if (BrightnessSeekBarMonitor.HWFLOW) {
                        String str = BrightnessSeekBarMonitor.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("SeekBarMovementUploader.upload() ");
                        stringBuilder.append(data);
                        Slog.i(str, stringBuilder.toString());
                    }
                }
                this.mUploadList.clear();
            }
        }

        public void hourlyTrigger() {
            int i = this.mHourCount + 1;
            this.mHourCount = i;
            if (i >= 24) {
                this.mHourCount = 0;
                this.mUploadTimesPerDay = 0;
            }
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

    public BrightnessSeekBarMonitor(DisplayEffectMonitor monitor, BackLightMonitorManager manager) {
        this.mMonitor = monitor;
        this.mCommonData = manager.getBackLightCommonData();
        if (!this.mCommonData.isCommercialVersion()) {
            this.mAutoModeMovementList = new ArrayList();
            this.mManualModeMovementList = new ArrayList();
            DisplayEffectMonitor displayEffectMonitor = this.mMonitor;
            Objects.requireNonNull(displayEffectMonitor);
            this.mTempManualBrightnessPrinter = new ParamLogPrinter(TYPE_TEMP_MANUAL_BRIGHTNESS, TAG);
            displayEffectMonitor = this.mMonitor;
            Objects.requireNonNull(displayEffectMonitor);
            this.mTempAutoBrightnessPrinter = new ParamLogPrinter(TYPE_TEMP_AUTO_BRIGHTNESS, TAG);
            this.mSeekBarMovementUploader = new SeekBarMovementUploader();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0049 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004a A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0049 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004a A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0049 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004a A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0049 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004a A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isParamOwner(String paramType) {
        boolean z;
        int hashCode = paramType.hashCode();
        if (hashCode == -1659644065) {
            if (paramType.equals(TYPE_ALGO_DEFAULT_BRIGHTNESS)) {
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
        } else if (hashCode == -1040827554) {
            if (paramType.equals(TYPE_EYE_PROTECT)) {
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
        } else if (hashCode == 1149740116) {
            if (paramType.equals(TYPE_TEMP_AUTO_BRIGHTNESS)) {
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
        } else if (hashCode == 1651298475 && paramType.equals(TYPE_TEMP_MANUAL_BRIGHTNESS)) {
            z = false;
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
        if (hashCode != -1659644065) {
            if (hashCode != -1040827554) {
                if (hashCode != 1149740116) {
                    if (hashCode == 1651298475 && paramType.equals(TYPE_TEMP_MANUAL_BRIGHTNESS)) {
                        obj = null;
                    }
                } else if (paramType.equals(TYPE_TEMP_AUTO_BRIGHTNESS)) {
                    obj = 1;
                }
            } else if (paramType.equals(TYPE_EYE_PROTECT)) {
                obj = 3;
            }
        } else if (paramType.equals(TYPE_ALGO_DEFAULT_BRIGHTNESS)) {
            obj = 2;
        }
        switch (obj) {
            case null:
                tempManualBrightness(params);
                break;
            case 1:
                tempAutoBrightness(params);
                break;
            case 2:
                algoDefaultBrightness(params);
                break;
            case 3:
                eyeProtect(params);
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
        seekBarMovementUploader();
    }

    /* JADX WARNING: Missing block: B:11:0x003c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void tempManualBrightness(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && tempManualBrightnessCheckParamValid(params)) {
            int brightness = ((Integer) params.get(PARAM_BRIGHTNESS)).intValue();
            String packageName = (String) params.get("packageName");
            tempManualBrightnessPrint(brightness, packageName);
            if (packageName.equals(MONITOR_TARGET_PACKAGE)) {
                recordSeekBarMovement(false, brightness);
            }
        }
    }

    private boolean tempManualBrightnessCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (!(params.get(PARAM_BRIGHTNESS) instanceof Integer)) {
            Slog.e(TAG, "tempManualBrightnessCheckParamValid() can't get param: brightness");
            return false;
        } else if (params.get("packageName") instanceof String) {
            return true;
        } else {
            Slog.e(TAG, "tempManualBrightnessCheckParamValid() can't get param: packageName");
            return false;
        }
    }

    private void tempManualBrightnessPrint(int brightness, String packageName) {
        if (this.mTempManualBrightnessPackageNameLast == null) {
            this.mTempManualBrightnessPackageNameLast = packageName;
        }
        if (this.mTempManualBrightnessPackageNameLast.equals(packageName)) {
            this.mTempManualBrightnessPrinter.updateParam(brightness, packageName);
            return;
        }
        this.mTempManualBrightnessPrinter.changeName(brightness, packageName);
        this.mTempManualBrightnessPackageNameLast = packageName;
    }

    /* JADX WARNING: Missing block: B:13:0x003f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void tempAutoBrightness(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && tempAutoBrightnessCheckParamValid(params)) {
            int brightness = ((Integer) params.get(PARAM_BRIGHTNESS)).intValue();
            String packageName = (String) params.get("packageName");
            tempAutoBrightnessPrint(brightness, packageName);
            if (brightness != -1 && packageName.equals(MONITOR_TARGET_PACKAGE)) {
                recordSeekBarMovement(true, brightness);
            }
        }
    }

    private boolean tempAutoBrightnessCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (!(params.get(PARAM_BRIGHTNESS) instanceof Integer)) {
            Slog.e(TAG, "tempAutoBrightnessCheckParamValid() can't get param: brightness");
            return false;
        } else if (params.get("packageName") instanceof String) {
            return true;
        } else {
            Slog.e(TAG, "tempAutoBrightnessCheckParamValid() can't get param: packageName");
            return false;
        }
    }

    private void tempAutoBrightnessPrint(int brightness, String packageName) {
        if (this.mTempAutoBrightnessPackageNameLast == null) {
            this.mTempAutoBrightnessPackageNameLast = packageName;
        }
        if (!this.mTempAutoBrightnessPackageNameLast.equals(packageName)) {
            this.mTempAutoBrightnessPrinter.changeName(brightness, packageName);
            this.mTempAutoBrightnessPackageNameLast = packageName;
        } else if (brightness == -1) {
            this.mTempAutoBrightnessPrinter.resetParam(brightness, packageName);
        } else {
            this.mTempAutoBrightnessPrinter.updateParam(brightness, packageName);
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0067, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void algoDefaultBrightness(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion() && algoDefaultBrightnessCheckParamValid(params)) {
            this.mCommonData.setSmoothAmbientLight(((Integer) params.get(PARAM_LIGHT_VALUE)).intValue());
            this.mAlgoDefaultBrightness = ((Integer) params.get(PARAM_BRIGHTNESS)).intValue();
            String mode = (String) params.get(PARAM_BRIGHTNESS_MODE);
            if (HWDEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("algoDefaultBrightness() mAlgoDefaultBrightness=");
                stringBuilder.append(this.mAlgoDefaultBrightness);
                Slog.d(str, stringBuilder.toString());
            }
            if (mode.equals("MANUAL")) {
                this.mIsManualModeReceiveAmbientLight = true;
            }
        }
    }

    private boolean algoDefaultBrightnessCheckParamValid(ArrayMap<String, Object> params) {
        if (params == null) {
            return false;
        }
        if (!(params.get(PARAM_LIGHT_VALUE) instanceof Integer)) {
            Slog.e(TAG, "algoDefaultBrightnessCheckParamValid() can't get param: lightValue");
            return false;
        } else if (!(params.get(PARAM_BRIGHTNESS) instanceof Integer)) {
            Slog.e(TAG, "algoDefaultBrightnessCheckParamValid() can't get param: brightness");
            return false;
        } else if (params.get(PARAM_BRIGHTNESS_MODE) instanceof String) {
            return true;
        } else {
            Slog.e(TAG, "algoDefaultBrightnessCheckParamValid() can't get param: brightnessMode");
            return false;
        }
    }

    private void eyeProtect(ArrayMap<String, Object> params) {
        if (this.mCommonData.isProductEnable() && !this.mCommonData.isCommercialVersion()) {
            Object enable = params.get(PARAM_IS_ENABLE);
            if (enable instanceof Boolean) {
                this.mIsEyeProtectEnable = ((Boolean) enable).booleanValue();
                if (HWDEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("eyeProtect() mIsEyeProtectEnable=");
                    stringBuilder.append(this.mIsEyeProtectEnable);
                    Slog.d(str, stringBuilder.toString());
                }
                return;
            }
            Slog.e(TAG, "eyeProtect() can't get param: isEnable");
        }
    }

    private boolean isPowerSaveMode() {
        return 2 != SystemProperties.getInt(POWER_MODE_PROP, 2);
    }

    private void recordSeekBarMovement(boolean isAutoMode, int level) {
        if ((isAutoMode || this.mIsManualModeReceiveAmbientLight) && !this.mCommonData.isWindowManagerBrightnessMode()) {
            long currentTime = SystemClock.elapsedRealtime();
            SeekBarMovement movement = isAutoMode ? this.mAutoModeMovement : this.mManualModeMovement;
            if (movement == null) {
                recordSeekBarMovementStart(new SeekBarMovement(), isAutoMode, level, currentTime);
                return;
            }
            if (movement.endTime == 0) {
                if (currentTime - movement.startTime > MOVE_INTERVAL_TIME_IN_MS) {
                    recordSeekBarMovementStart(movement, isAutoMode, level, currentTime);
                } else {
                    movement.endTime = currentTime;
                    movement.endLevel = level;
                }
            } else if (currentTime - movement.endTime > MOVE_INTERVAL_TIME_IN_MS) {
                recordSeekBarMovementEnd(movement, isAutoMode);
                recordSeekBarMovementStart(new SeekBarMovement(), isAutoMode, level, currentTime);
            } else {
                movement.endTime = currentTime;
                movement.endLevel = level;
            }
        }
    }

    private void recordSeekBarMovementStart(SeekBarMovement movement, boolean isAutoMode, int level, long time) {
        movement.isAutoMode = isAutoMode;
        movement.startTime = time;
        movement.startLevel = level;
        movement.startLux = this.mCommonData.getSmoothAmbientLight();
        movement.algoDefaultLevel = isAutoMode ? this.mAlgoDefaultBrightness : 0;
        movement.foregroundPackageName = this.mMonitor.getCurrentTopAppName();
        movement.protectEye = this.mIsEyeProtectEnable;
        movement.powerSave = isPowerSaveMode();
        movement.thermalLimited = this.mCommonData.isThermalLimited();
        if (isAutoMode) {
            this.mAutoModeMovement = movement;
        } else {
            this.mManualModeMovement = movement;
        }
    }

    private void recordSeekBarMovementEnd(SeekBarMovement movement, boolean isAutoMode) {
        Object movement2;
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("recordSeekBarMovementEnd() ");
            stringBuilder.append(isAutoMode ? "AUTO" : "MANUAL");
            stringBuilder.append(" ");
            stringBuilder.append(movement2);
            Slog.i(str, stringBuilder.toString());
        }
        if (movement2.startLevel == movement2.endLevel) {
            movement2 = null;
        }
        if (isAutoMode) {
            if (movement2 != null) {
                this.mAutoModeMovementList.add(movement2);
            }
            this.mAutoModeMovement = null;
            return;
        }
        if (movement2 != null) {
            this.mManualModeMovementList.add(movement2);
        }
        this.mManualModeMovement = null;
    }

    private void seekBarMovementUploader() {
        checkUnfinishedMovement(true);
        checkUnfinishedMovement(false);
        checkMovementListInterval(true);
        checkMovementListInterval(false);
        uploadMovementList();
        this.mSeekBarMovementUploader.hourlyTrigger();
    }

    private void checkUnfinishedMovement(boolean isAutoMode) {
        SeekBarMovement movement = isAutoMode ? this.mAutoModeMovement : this.mManualModeMovement;
        if (movement != null) {
            long currentTime = SystemClock.elapsedRealtime();
            if (movement.endTime != 0 && currentTime - movement.endTime > MOVE_INTERVAL_TIME_IN_MS) {
                recordSeekBarMovementEnd(movement, isAutoMode);
            }
        }
    }

    private void checkMovementListInterval(boolean isAutoMode) {
        List<SeekBarMovement> movementList = isAutoMode ? this.mAutoModeMovementList : this.mManualModeMovementList;
        if (movementList.size() > 1) {
            if (HWDEBUG) {
                for (SeekBarMovement movement : movementList) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("checkMovementListInterval() before ");
                    stringBuilder.append(movement);
                    Slog.d(str, stringBuilder.toString());
                }
            }
            boolean isMerged = false;
            SeekBarMovement movement2 = null;
            SeekBarMovement now = null;
            Iterator<SeekBarMovement> it = movementList.iterator();
            while (it.hasNext()) {
                if (movement2 == null) {
                    movement2 = (SeekBarMovement) it.next();
                } else if (now != null) {
                    movement2 = now;
                }
                now = (SeekBarMovement) it.next();
                if (isMerge2Movement(movement2, now)) {
                    if (HWDEBUG) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("checkMovementListInterval() merge ");
                        stringBuilder2.append(movement2);
                        stringBuilder2.append(" and ");
                        stringBuilder2.append(now);
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    movement2.endTime = now.endTime;
                    movement2.endLevel = now.endLevel;
                    it.remove();
                    now = null;
                    isMerged = true;
                }
            }
            if (isMerged && HWDEBUG) {
                for (SeekBarMovement movement3 : movementList) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("checkMovementListInterval() after ");
                    stringBuilder3.append(movement3);
                    Slog.d(str3, stringBuilder3.toString());
                }
            }
        }
    }

    private boolean isMerge2Movement(SeekBarMovement pre, SeekBarMovement now) {
        if (now.startTime - pre.endTime > MOVE_LIST_INTERVAL_TIME_IN_MS) {
            return false;
        }
        int deltaLevel = Math.abs(now.startLux - pre.startLux);
        int deltaLevelThresholdTmp = (pre.startLux * 50) / 100;
        int deltaLevelThreshold = 5;
        if (deltaLevelThresholdTmp > 5) {
            deltaLevelThreshold = deltaLevelThresholdTmp;
        }
        if (deltaLevel >= deltaLevelThreshold) {
            return false;
        }
        return true;
    }

    private void uploadMovementList() {
        if (!this.mAutoModeMovementList.isEmpty() || !this.mManualModeMovementList.isEmpty()) {
            List<SeekBarMovement> allMovementList = new ArrayList();
            allMovementList.addAll(this.mAutoModeMovementList);
            allMovementList.addAll(this.mManualModeMovementList);
            Collections.sort(allMovementList, mComparator);
            for (SeekBarMovement movement : allMovementList) {
                if (movement.startLevel != movement.endLevel) {
                    this.mSeekBarMovementUploader.addData(movement);
                }
            }
            this.mSeekBarMovementUploader.upload();
            this.mAutoModeMovementList.clear();
            this.mManualModeMovementList.clear();
        }
    }
}
