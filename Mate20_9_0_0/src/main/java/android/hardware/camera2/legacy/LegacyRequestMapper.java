package android.hardware.camera2.legacy;

import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Key;
import android.hardware.camera2.legacy.ParameterUtils.ZoomData;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.utils.ListUtils;
import android.hardware.camera2.utils.ParamsUtils;
import android.location.Location;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class LegacyRequestMapper {
    private static final boolean DEBUG = false;
    private static final byte DEFAULT_JPEG_QUALITY = (byte) 85;
    private static final String TAG = "LegacyRequestMapper";

    public static void convertRequestMetadata(LegacyRequest legacyRequest) {
        String str;
        StringBuilder stringBuilder;
        String str2;
        StringBuilder stringBuilder2;
        String modeToSet;
        LegacyRequest legacyRequest2 = legacyRequest;
        CameraCharacteristics characteristics = legacyRequest2.characteristics;
        CaptureRequest request = legacyRequest2.captureRequest;
        Size previewSize = legacyRequest2.previewSize;
        Parameters params = legacyRequest2.parameters;
        Rect activeArray = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        ZoomData zoomData = ParameterUtils.convertScalerCropRegion(activeArray, (Rect) request.get(CaptureRequest.SCALER_CROP_REGION), previewSize, params);
        if (params.isZoomSupported()) {
            params.setZoom(zoomData.zoomIndex);
        }
        int i = 1;
        int aberrationMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, Integer.valueOf(1))).intValue();
        if (!(aberrationMode == 1 || aberrationMode == 2)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("convertRequestToMetadata - Ignoring unsupported colorCorrection.aberrationMode = ");
            stringBuilder.append(aberrationMode);
            Log.w(str, stringBuilder.toString());
        }
        Integer antiBandingMode = (Integer) request.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE);
        if (antiBandingMode != null) {
            str = convertAeAntiBandingModeToLegacy(antiBandingMode.intValue());
        } else {
            str = (String) ListUtils.listSelectFirstFrom(params.getSupportedAntibanding(), new String[]{"auto", "off", Parameters.ANTIBANDING_50HZ, Parameters.ANTIBANDING_60HZ});
        }
        if (str != null) {
            params.setAntibanding(str);
        }
        MeteringRectangle[] aeRegions = (MeteringRectangle[]) request.get(CaptureRequest.CONTROL_AE_REGIONS);
        if (request.get(CaptureRequest.CONTROL_AWB_REGIONS) != null) {
            Log.w(TAG, "convertRequestMetadata - control.awbRegions setting is not supported, ignoring value");
        }
        int maxNumMeteringAreas = params.getMaxNumMeteringAreas();
        List<Area> meteringAreaList = convertMeteringRegionsToLegacy(activeArray, zoomData, aeRegions, maxNumMeteringAreas, "AE");
        if (maxNumMeteringAreas > 0) {
            params.setMeteringAreas(meteringAreaList);
        }
        aeRegions = (MeteringRectangle[]) request.get(CaptureRequest.CONTROL_AF_REGIONS);
        maxNumMeteringAreas = params.getMaxNumFocusAreas();
        meteringAreaList = convertMeteringRegionsToLegacy(activeArray, zoomData, aeRegions, maxNumMeteringAreas, "AF");
        if (maxNumMeteringAreas > 0) {
            params.setFocusAreas(meteringAreaList);
        }
        Range<Integer> aeFpsRange = (Range) request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
        maxNumMeteringAreas = 0;
        if (aeFpsRange != null) {
            int[] legacyFps = convertAeFpsRangeToLegacy(aeFpsRange);
            int[] rangeToApply = null;
            for (int[] range : params.getSupportedPreviewFpsRange()) {
                i = ((int) Math.ceil(((double) range[i]) / 1000.0d)) * 1000;
                if (legacyFps[0] == ((int) Math.floor(((double) range[maxNumMeteringAreas]) / 1000.0d)) * 1000 && legacyFps[1] == i) {
                    rangeToApply = range;
                    break;
                } else {
                    i = 1;
                    maxNumMeteringAreas = 0;
                }
            }
            if (rangeToApply != null) {
                params.setPreviewFpsRange(rangeToApply[0], rangeToApply[1]);
            } else {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unsupported FPS range set [");
                stringBuilder2.append(legacyFps[0]);
                stringBuilder2.append(",");
                stringBuilder2.append(legacyFps[1]);
                stringBuilder2.append("]");
                Log.w(str2, stringBuilder2.toString());
            }
        }
        Range<Integer> compensationRange = (Range) characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        int compensation = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.valueOf(0))).intValue();
        if (!compensationRange.contains(Integer.valueOf(compensation))) {
            Log.w(TAG, "convertRequestMetadata - control.aeExposureCompensation is out of range, ignoring value");
            compensation = 0;
        }
        params.setExposureCompensation(compensation);
        Boolean aeLock = (Boolean) getIfSupported(request, CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(false), params.isAutoExposureLockSupported(), Boolean.valueOf(false));
        if (aeLock != null) {
            params.setAutoExposureLock(aeLock.booleanValue());
        }
        mapAeAndFlashMode(request, params);
        str2 = LegacyMetadataMapper.convertAfModeToLegacy(((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(0))).intValue(), params.getSupportedFocusModes());
        if (str2 != null) {
            params.setFocusMode(str2);
        }
        Integer awbMode = (Integer) getIfSupported(request, CaptureRequest.CONTROL_AWB_MODE, Integer.valueOf(1), params.getSupportedWhiteBalance() != null, Integer.valueOf(1));
        if (awbMode != null) {
            params.setWhiteBalance(convertAwbModeToLegacy(awbMode.intValue()));
        }
        aeLock = (Boolean) getIfSupported(request, CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(false), params.isAutoWhiteBalanceLockSupported(), Boolean.valueOf(false));
        if (aeLock != null) {
            params.setAutoWhiteBalanceLock(aeLock.booleanValue());
        }
        i = filterSupportedCaptureIntent(((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_CAPTURE_INTENT, Integer.valueOf(1))).intValue());
        boolean z = i == 3 || i == 4;
        params.setRecordingHint(z);
        awbMode = (Integer) getIfSupported(request, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, Integer.valueOf(0), params.isVideoStabilizationSupported(), Integer.valueOf(0));
        if (awbMode != null) {
            params.setVideoStabilization(awbMode.intValue() == 1);
        }
        boolean infinityFocusSupported = ListUtils.listContains(params.getSupportedFocusModes(), Parameters.FOCUS_MODE_INFINITY);
        Float focusDistance = (Float) getIfSupported(request, CaptureRequest.LENS_FOCUS_DISTANCE, Float.valueOf(0.0f), infinityFocusSupported, Float.valueOf(0.0f));
        if (focusDistance == null || focusDistance.floatValue() != 0.0f) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("convertRequestToMetadata - Ignoring android.lens.focusDistance ");
            stringBuilder.append(infinityFocusSupported);
            stringBuilder.append(", only 0.0f is supported");
            Log.w(str, stringBuilder.toString());
        }
        if (params.getSupportedSceneModes() != null) {
            i = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_MODE, Integer.valueOf(1))).intValue();
            switch (i) {
                case 1:
                    modeToSet = "auto";
                    break;
                case 2:
                    compensation = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_SCENE_MODE, Integer.valueOf(0))).intValue();
                    str = LegacyMetadataMapper.convertSceneModeToLegacy(compensation);
                    if (str == null) {
                        modeToSet = "auto";
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Skipping unknown requested scene mode: ");
                        stringBuilder3.append(compensation);
                        Log.w(str3, stringBuilder3.toString());
                        break;
                    }
                    modeToSet = str;
                    break;
                default:
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Control mode ");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" is unsupported, defaulting to AUTO");
                    Log.w(str2, stringBuilder2.toString());
                    modeToSet = "auto";
                    break;
            }
            params.setSceneMode(modeToSet);
        }
        if (params.getSupportedColorEffects() != null) {
            i = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_EFFECT_MODE, Integer.valueOf(0))).intValue();
            str2 = LegacyMetadataMapper.convertEffectModeToLegacy(i);
            if (str2 != null) {
                params.setColorEffect(str2);
            } else {
                params.setColorEffect("none");
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping unknown requested effect mode: ");
                stringBuilder.append(i);
                Log.w(str, stringBuilder.toString());
            }
        }
        i = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.SENSOR_TEST_PATTERN_MODE, Integer.valueOf(0))).intValue();
        if (i != 0) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("convertRequestToMetadata - ignoring sensor.testPatternMode ");
            stringBuilder2.append(i);
            stringBuilder2.append("; only OFF is supported");
            Log.w(str2, stringBuilder2.toString());
        }
        Location location = (Location) request.get(CaptureRequest.JPEG_GPS_LOCATION);
        if (location == null) {
            params.removeGpsData();
        } else if (checkForCompleteGpsData(location)) {
            params.setGpsAltitude(location.getAltitude());
            params.setGpsLatitude(location.getLatitude());
            params.setGpsLongitude(location.getLongitude());
            params.setGpsProcessingMethod(location.getProvider().toUpperCase());
            params.setGpsTimestamp(location.getTime());
        } else {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Incomplete GPS parameters provided in location ");
            stringBuilder2.append(location);
            Log.w(str2, stringBuilder2.toString());
        }
        awbMode = (Integer) request.get(CaptureRequest.JPEG_ORIENTATION);
        params.setRotation(((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(awbMode == null ? 0 : awbMode.intValue()))).intValue());
        params.setJpegQuality(((Byte) ParamsUtils.getOrDefault(request, CaptureRequest.JPEG_QUALITY, Byte.valueOf(DEFAULT_JPEG_QUALITY))).byteValue() & 255);
        params.setJpegThumbnailQuality(((Byte) ParamsUtils.getOrDefault(request, CaptureRequest.JPEG_THUMBNAIL_QUALITY, Byte.valueOf(DEFAULT_JPEG_QUALITY))).byteValue() & 255);
        List<Camera.Size> sizes = params.getSupportedJpegThumbnailSizes();
        if (sizes != null && sizes.size() > 0) {
            Size s = (Size) request.get(CaptureRequest.JPEG_THUMBNAIL_SIZE);
            boolean invalidSize = (s == null || ParameterUtils.containsSize(sizes, s.getWidth(), s.getHeight())) ? false : true;
            if (invalidSize) {
                modeToSet = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Invalid JPEG thumbnail size set ");
                stringBuilder4.append(s);
                stringBuilder4.append(", skipping thumbnail...");
                Log.w(modeToSet, stringBuilder4.toString());
            }
            if (s == null || invalidSize) {
                params.setJpegThumbnailSize(0, 0);
            } else {
                params.setJpegThumbnailSize(s.getWidth(), s.getHeight());
            }
        }
        i = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.NOISE_REDUCTION_MODE, Integer.valueOf(1))).intValue();
        if (i != 1 && i != 2) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("convertRequestToMetadata - Ignoring unsupported noiseReduction.mode = ");
            stringBuilder2.append(i);
            Log.w(str2, stringBuilder2.toString());
        }
    }

    private static boolean checkForCompleteGpsData(Location location) {
        return (location == null || location.getProvider() == null || location.getTime() == 0) ? false : true;
    }

    static int filterSupportedCaptureIntent(int captureIntent) {
        String str;
        StringBuilder stringBuilder;
        switch (captureIntent) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return captureIntent;
            case 5:
            case 6:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported control.captureIntent value ");
                stringBuilder.append(1);
                stringBuilder.append("; default to PREVIEW");
                Log.w(str, stringBuilder.toString());
                break;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown control.captureIntent value ");
        stringBuilder.append(1);
        stringBuilder.append("; default to PREVIEW");
        Log.w(str, stringBuilder.toString());
        return 1;
    }

    private static List<Area> convertMeteringRegionsToLegacy(Rect activeArray, ZoomData zoomData, MeteringRectangle[] meteringRegions, int maxNumMeteringAreas, String regionName) {
        int i = 0;
        if (meteringRegions != null && maxNumMeteringAreas > 0) {
            List<MeteringRectangle> meteringRectangleList = new ArrayList();
            for (MeteringRectangle rect : meteringRegions) {
                if (rect.getMeteringWeight() != 0) {
                    meteringRectangleList.add(rect);
                }
            }
            if (meteringRectangleList.size() == 0) {
                Log.w(TAG, "Only received metering rectangles with weight 0.");
                return Arrays.asList(new Area[]{ParameterUtils.CAMERA_AREA_DEFAULT});
            }
            int countMeteringAreas = Math.min(maxNumMeteringAreas, meteringRectangleList.size());
            List<Area> meteringAreaList = new ArrayList(countMeteringAreas);
            while (i < countMeteringAreas) {
                meteringAreaList.add(ParameterUtils.convertMeteringRectangleToLegacy(activeArray, (MeteringRectangle) meteringRectangleList.get(i), zoomData).meteringArea);
                i++;
            }
            if (maxNumMeteringAreas < meteringRectangleList.size()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("convertMeteringRegionsToLegacy - Too many requested ");
                stringBuilder.append(regionName);
                stringBuilder.append(" regions, ignoring all beyond the first ");
                stringBuilder.append(maxNumMeteringAreas);
                Log.w(str, stringBuilder.toString());
            }
            return meteringAreaList;
        } else if (maxNumMeteringAreas <= 0) {
            return null;
        } else {
            return Arrays.asList(new Area[]{ParameterUtils.CAMERA_AREA_DEFAULT});
        }
    }

    private static void mapAeAndFlashMode(CaptureRequest r, Parameters p) {
        int flashMode = ((Integer) ParamsUtils.getOrDefault(r, CaptureRequest.FLASH_MODE, Integer.valueOf(0))).intValue();
        int aeMode = ((Integer) ParamsUtils.getOrDefault(r, CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(1))).intValue();
        List<String> supportedFlashModes = p.getSupportedFlashModes();
        String flashModeSetting = null;
        if (ListUtils.listContains(supportedFlashModes, "off")) {
            flashModeSetting = "off";
        }
        if (aeMode == 1) {
            if (flashMode == 2) {
                if (ListUtils.listContains(supportedFlashModes, Parameters.FLASH_MODE_TORCH)) {
                    flashModeSetting = Parameters.FLASH_MODE_TORCH;
                } else {
                    Log.w(TAG, "mapAeAndFlashMode - Ignore flash.mode == TORCH;camera does not support it");
                }
            } else if (flashMode == 1) {
                if (ListUtils.listContains(supportedFlashModes, Parameters.FLASH_MODE_ON)) {
                    flashModeSetting = Parameters.FLASH_MODE_ON;
                } else {
                    Log.w(TAG, "mapAeAndFlashMode - Ignore flash.mode == SINGLE;camera does not support it");
                }
            }
        } else if (aeMode == 3) {
            if (ListUtils.listContains(supportedFlashModes, Parameters.FLASH_MODE_ON)) {
                flashModeSetting = Parameters.FLASH_MODE_ON;
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignore control.aeMode == ON_ALWAYS_FLASH;camera does not support it");
            }
        } else if (aeMode == 2) {
            if (ListUtils.listContains(supportedFlashModes, "auto")) {
                flashModeSetting = "auto";
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignore control.aeMode == ON_AUTO_FLASH;camera does not support it");
            }
        } else if (aeMode == 4) {
            if (ListUtils.listContains(supportedFlashModes, Parameters.FLASH_MODE_RED_EYE)) {
                flashModeSetting = Parameters.FLASH_MODE_RED_EYE;
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignore control.aeMode == ON_AUTO_FLASH_REDEYE;camera does not support it");
            }
        }
        if (flashModeSetting != null) {
            p.setFlashMode(flashModeSetting);
        }
    }

    private static String convertAeAntiBandingModeToLegacy(int mode) {
        switch (mode) {
            case 0:
                return "off";
            case 1:
                return Parameters.ANTIBANDING_50HZ;
            case 2:
                return Parameters.ANTIBANDING_60HZ;
            case 3:
                return "auto";
            default:
                return null;
        }
    }

    private static int[] convertAeFpsRangeToLegacy(Range<Integer> fpsRange) {
        return new int[]{((Integer) fpsRange.getLower()).intValue() * 1000, ((Integer) fpsRange.getUpper()).intValue() * 1000};
    }

    private static String convertAwbModeToLegacy(int mode) {
        switch (mode) {
            case 1:
                return "auto";
            case 2:
                return Parameters.WHITE_BALANCE_INCANDESCENT;
            case 3:
                return Parameters.WHITE_BALANCE_FLUORESCENT;
            case 4:
                return Parameters.WHITE_BALANCE_WARM_FLUORESCENT;
            case 5:
                return Parameters.WHITE_BALANCE_DAYLIGHT;
            case 6:
                return Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT;
            case 7:
                return Parameters.WHITE_BALANCE_TWILIGHT;
            case 8:
                return Parameters.WHITE_BALANCE_SHADE;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("convertAwbModeToLegacy - unrecognized control.awbMode");
                stringBuilder.append(mode);
                Log.w(str, stringBuilder.toString());
                return "auto";
        }
    }

    private static <T> T getIfSupported(CaptureRequest r, Key<T> key, T defaultValue, boolean isSupported, T allowedValue) {
        T val = ParamsUtils.getOrDefault(r, key, defaultValue);
        if (isSupported) {
            return val;
        }
        if (!Objects.equals(val, allowedValue)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(key.getName());
            stringBuilder.append(" is not supported; ignoring requested value ");
            stringBuilder.append(val);
            Log.w(str, stringBuilder.toString());
        }
        return null;
    }
}
