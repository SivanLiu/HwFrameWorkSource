package com.android.server.display;

import android.graphics.PointF;
import android.os.Bundle;
import android.os.HwBrightnessProcessor;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;

public class HwDisplayBrightnessProcessor extends HwBrightnessProcessor {
    private static final int FAILED_RETURN_VALUE = -1;
    /* access modifiers changed from: private */
    public static final boolean HWDEBUG = (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3)));
    /* access modifiers changed from: private */
    public static final boolean HWFLOW;
    private static final ArrayMap<String, HwBrightnessProcessor> HW_BRIGHTNESS_PROCESSORS = new ArrayMap<>();
    private static final int SUCCESS_RETURN_VALUE = 0;
    /* access modifiers changed from: private */
    public static String TAG = "HwDisplayBrightnessProcessor";

    static {
        boolean z = false;
        if (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4))) {
            z = true;
        }
        HWFLOW = z;
    }

    public HwDisplayBrightnessProcessor(AutomaticBrightnessController autoController, ManualBrightnessController manualController) {
        loadHwBrightnessProcessors(autoController, manualController);
    }

    private static void loadHwBrightnessProcessors(AutomaticBrightnessController autoController, ManualBrightnessController manualController) {
        HW_BRIGHTNESS_PROCESSORS.put("Cryogenic", new CryogenicPowerProcessor(autoController));
        HW_BRIGHTNESS_PROCESSORS.put("SceneRecognition", new SceneRecognitionProcessor(autoController));
        HW_BRIGHTNESS_PROCESSORS.put("PersonalizedBrightnessCurveLevel", new PersonalizedBrightnessCurveLevelProcessor(autoController));
        HW_BRIGHTNESS_PROCESSORS.put("PersonalizedBrightness", new PersonalizedBrightnessProcessor(autoController));
        HW_BRIGHTNESS_PROCESSORS.put("QRCodeBrighten", new QrCodeBrightenProcessor(autoController));
        HW_BRIGHTNESS_PROCESSORS.put("ThermalMaxBrightnessNit", new ThermalMaxBrightnessNitProcessor(autoController, manualController));
        HW_BRIGHTNESS_PROCESSORS.put("CurrentBrightnessNit", new CurrentBrightnessNitProcessor(autoController));
        HW_BRIGHTNESS_PROCESSORS.put("AmbientLuxBrightness", new AmbientLuxBrightnessProcessor(autoController, manualController));
        HW_BRIGHTNESS_PROCESSORS.put("FrontCameraApp", new FrontCameraAppProcessor(autoController, manualController));
        HW_BRIGHTNESS_PROCESSORS.put("GameDiableAutoBrightness", new GameDiableAutoBrightnessProcessor(autoController));
        Slog.i(TAG, "loadHwBrightnessProcessors");
    }

    public HwBrightnessProcessor getProcessor(String processName) {
        return HW_BRIGHTNESS_PROCESSORS.get(processName);
    }

    private static final class SceneRecognitionProcessor extends HwBrightnessProcessor {
        private AutomaticBrightnessController mAutomaticBrightnessController;

        public SceneRecognitionProcessor(AutomaticBrightnessController controller) {
            this.mAutomaticBrightnessController = controller;
        }

        public boolean getData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController == null) {
                retValue[0] = -1;
                return false;
            }
            automaticBrightnessController.getUserDragInfo(data);
            if (HwDisplayBrightnessProcessor.HWDEBUG) {
                Slog.i(HwDisplayBrightnessProcessor.TAG, "getUserDragInfo");
            }
            retValue[0] = 0;
            return true;
        }
    }

    private static final class PersonalizedBrightnessCurveLevelProcessor extends HwBrightnessProcessor {
        private AutomaticBrightnessController mAutomaticBrightnessController;

        public PersonalizedBrightnessCurveLevelProcessor(AutomaticBrightnessController controller) {
            this.mAutomaticBrightnessController = controller;
        }

        public boolean setData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            if (this.mAutomaticBrightnessController == null) {
                retValue[0] = -1;
                return false;
            } else if (data == null) {
                retValue[0] = -1;
                return false;
            } else {
                int topApkLevel = data.getInt("TopApkLevel", 0);
                this.mAutomaticBrightnessController.setPersonalizedBrightnessCurveLevel(topApkLevel);
                if (HwDisplayBrightnessProcessor.HWDEBUG) {
                    String access$100 = HwDisplayBrightnessProcessor.TAG;
                    Slog.i(access$100, "setPersonalizedBrightnessCurveLevel=" + topApkLevel);
                }
                retValue[0] = 0;
                return true;
            }
        }
    }

    private static final class PersonalizedBrightnessProcessor extends HwBrightnessProcessor {
        private String TAG = "PersonalizedBrightnessProcessor";
        private AutomaticBrightnessController mAutomaticBrightnessController;

        public PersonalizedBrightnessProcessor(AutomaticBrightnessController controller) {
            this.mAutomaticBrightnessController = controller;
        }

        public boolean setData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            if (this.mAutomaticBrightnessController == null) {
                retValue[0] = -1;
                return false;
            } else if (data == null) {
                retValue[0] = -1;
                return false;
            } else {
                int isCurveUpdate = data.getInt("CurveUpdateFlag", 0);
                if (HwDisplayBrightnessProcessor.HWDEBUG) {
                    String str = this.TAG;
                    Slog.i(str, "isCurveUpdate = " + isCurveUpdate);
                }
                if (isCurveUpdate == 1) {
                    this.mAutomaticBrightnessController.updateNewBrightnessCurveTmp();
                }
                retValue[0] = 0;
                return true;
            }
        }

        public boolean getData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController == null) {
                retValue[0] = -1;
                return false;
            }
            List<PointF> defaultCurve = automaticBrightnessController.getCurrentDefaultNewCurveLine();
            if (defaultCurve == null) {
                retValue[0] = -1;
                return false;
            }
            if (defaultCurve instanceof ArrayList) {
                data.putParcelableArrayList("DefaultCurve", (ArrayList) defaultCurve);
            }
            if (HwDisplayBrightnessProcessor.HWDEBUG) {
                Slog.i(this.TAG, "defaultCurve ");
            }
            retValue[0] = 0;
            return true;
        }
    }

    private static final class QrCodeBrightenProcessor extends HwBrightnessProcessor {
        private String TAG = "QrCodeBrightenProcessor";
        private AutomaticBrightnessController mAutomaticBrightnessController;

        public QrCodeBrightenProcessor(AutomaticBrightnessController controller) {
            this.mAutomaticBrightnessController = controller;
        }

        public boolean setData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            if (this.mAutomaticBrightnessController == null) {
                retValue[0] = -1;
                return false;
            } else if (data == null) {
                retValue[0] = -1;
                return false;
            } else {
                boolean isVideoPlay = data.getBoolean("IsVideoPlay", false);
                if (HwDisplayBrightnessProcessor.HWFLOW) {
                    String str = this.TAG;
                    Slog.i(str, "IsVideoPlay = " + isVideoPlay);
                }
                this.mAutomaticBrightnessController.setVideoPlayStatus(isVideoPlay);
                retValue[0] = 0;
                return true;
            }
        }
    }

    private static final class ThermalMaxBrightnessNitProcessor extends HwBrightnessProcessor {
        private final String TAG = "ThermalMaxBrightnessNitProcessor";
        private AutomaticBrightnessController mAutomaticBrightnessController;
        private ManualBrightnessController mManualBrightnessController;

        public ThermalMaxBrightnessNitProcessor(AutomaticBrightnessController autoController, ManualBrightnessController manualController) {
            this.mAutomaticBrightnessController = autoController;
            this.mManualBrightnessController = manualController;
        }

        public boolean setData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            if (this.mAutomaticBrightnessController == null || this.mManualBrightnessController == null) {
                retValue[0] = -1;
                return false;
            } else if (data == null) {
                retValue[0] = -1;
                return false;
            } else {
                int maxBrightnessNit = data.getInt("MaxBrightnessNit", 0);
                if (HwDisplayBrightnessProcessor.HWFLOW) {
                    Slog.i("ThermalMaxBrightnessNitProcessor", "MaxBrightnessNitFromThermal,maxNit = " + maxBrightnessNit);
                }
                this.mAutomaticBrightnessController.setMaxBrightnessNitFromThermal(maxBrightnessNit);
                this.mManualBrightnessController.setMaxBrightnessNitFromThermal(maxBrightnessNit);
                retValue[0] = 0;
                return true;
            }
        }
    }

    private static final class CurrentBrightnessNitProcessor extends HwBrightnessProcessor {
        private final String TAG = "CurrentBrightnessNitProcessor";
        private AutomaticBrightnessController mAutomaticBrightnessController;

        public CurrentBrightnessNitProcessor(AutomaticBrightnessController autoController) {
            this.mAutomaticBrightnessController = autoController;
        }

        public boolean getData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController == null) {
                retValue[0] = -1;
                return false;
            }
            int currentBrightnessNit = automaticBrightnessController.getCurrentBrightnessNit();
            int maxBrightnessNit = this.mAutomaticBrightnessController.getMaxBrightnessNit();
            int minBrightnessNit = this.mAutomaticBrightnessController.getMinBrightnessNit();
            data.putInt("CurrentBrightnessNit", currentBrightnessNit);
            data.putInt("MaxBrightnessNit", maxBrightnessNit);
            data.putInt("MinBrightnessNit", minBrightnessNit);
            if (HwDisplayBrightnessProcessor.HWDEBUG) {
                Slog.i("CurrentBrightnessNitProcessor", "currentBrightnessNit=" + currentBrightnessNit + ",maxBrightnessNit=" + maxBrightnessNit + ",minBrightnessNit=" + minBrightnessNit);
            }
            retValue[0] = 0;
            return true;
        }
    }

    private static final class AmbientLuxBrightnessProcessor extends HwBrightnessProcessor {
        private final String TAG = "AmbientLuxBrightnessProcessor";
        private AutomaticBrightnessController mAutomaticBrightnessController;
        private ManualBrightnessController mManualBrightnessController;

        public AmbientLuxBrightnessProcessor(AutomaticBrightnessController autoController, ManualBrightnessController manualController) {
            this.mAutomaticBrightnessController = autoController;
            this.mManualBrightnessController = manualController;
        }

        public boolean getData(Bundle data, int[] retValue) {
            int brightnessLevel;
            int ambientLux;
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController == null || this.mManualBrightnessController == null) {
                retValue[0] = -1;
                return false;
            }
            boolean autoModeEnable = automaticBrightnessController.getAutoBrightnessEnable();
            if (autoModeEnable) {
                ambientLux = this.mAutomaticBrightnessController.getAmbientLux();
                brightnessLevel = this.mAutomaticBrightnessController.getBrightnessLevel(ambientLux);
            } else {
                ambientLux = this.mManualBrightnessController.getAmbientLux();
                brightnessLevel = this.mManualBrightnessController.getBrightnessLevel(ambientLux);
            }
            data.putInt("AmbientLux", ambientLux);
            data.putInt("BrightnessLevel", brightnessLevel);
            if (HwDisplayBrightnessProcessor.HWFLOW) {
                Slog.i("AmbientLuxBrightnessProcessor", "ambientLux=" + ambientLux + ",brightnessLevel= " + brightnessLevel + ",autoModeEnable=" + autoModeEnable);
            }
            retValue[0] = 0;
            return true;
        }
    }

    private static final class FrontCameraAppProcessor extends HwBrightnessProcessor {
        private final String TAG = "FrontCameraAppProcessor";
        private AutomaticBrightnessController mAutomaticBrightnessController;
        private ManualBrightnessController mManualBrightnessController;

        public FrontCameraAppProcessor(AutomaticBrightnessController autoController, ManualBrightnessController manualController) {
            this.mAutomaticBrightnessController = autoController;
            this.mManualBrightnessController = manualController;
        }

        public boolean setData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            if (this.mAutomaticBrightnessController == null || this.mManualBrightnessController == null) {
                retValue[0] = -1;
                return false;
            } else if (data == null) {
                retValue[0] = -1;
                return false;
            } else {
                boolean frontCameraAppEnableState = data.getBoolean("FrontCameraAppEnableState", false);
                if (HwDisplayBrightnessProcessor.HWFLOW) {
                    Slog.i("FrontCameraAppProcessor", "frontCameraAppEnableState= " + frontCameraAppEnableState);
                }
                this.mAutomaticBrightnessController.setFrontCameraAppEnableState(frontCameraAppEnableState);
                this.mManualBrightnessController.setFrontCameraAppEnableState(frontCameraAppEnableState);
                retValue[0] = 0;
                return true;
            }
        }
    }

    private static final class GameDiableAutoBrightnessProcessor extends HwBrightnessProcessor {
        private static final String TAG = "GameDiableAutoBrightnessProcessor";
        private AutomaticBrightnessController mAutomaticBrightnessController;

        public GameDiableAutoBrightnessProcessor(AutomaticBrightnessController autoController) {
            this.mAutomaticBrightnessController = autoController;
        }

        public boolean getData(Bundle data, int[] retValue) {
            if (retValue == null || retValue.length == 0) {
                return false;
            }
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController == null) {
                retValue[0] = -1;
                return false;
            } else if (data == null) {
                retValue[0] = -1;
                return false;
            } else {
                boolean modeEnable = automaticBrightnessController.getGameDisableAutoBrightnessModeStatus();
                data.putBoolean("GameDisableAutoBrightnessModeEnable", modeEnable);
                if (HwDisplayBrightnessProcessor.HWFLOW) {
                    Slog.i(TAG, "GameDisableAutoBrightnessModeEnable=" + modeEnable);
                }
                retValue[0] = 0;
                return true;
            }
        }
    }
}
