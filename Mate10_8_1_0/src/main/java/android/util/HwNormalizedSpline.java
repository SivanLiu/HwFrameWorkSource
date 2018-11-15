package android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.os.FileUtils;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.System;
import huawei.android.utils.HwEyeProtectionSpline;
import huawei.android.utils.HwEyeProtectionSplineImpl;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class HwNormalizedSpline extends Spline {
    private static final float BRIGHTNESS_WITHDELTA_MAX = 230.0f;
    private static boolean DEBUG = false;
    private static final String HW_LABC_CONFIG_FILE = "LABCConfig.xml";
    private static final String LCD_PANEL_TYPE_PATH = "/sys/class/graphics/fb0/lcd_model";
    private static final String TAG = "HwNormalizedSpline";
    private static final String TOUCH_OEM_INFO_PATH = "/sys/touchscreen/touch_oem_info";
    private static final String XML_EXT = ".xml";
    private static final String XML_NAME_NOEXT = "LABCConfig";
    private static final float maxBrightness = 255.0f;
    private static final float minBrightness = 4.0f;
    private float mAmLux;
    private float mAmLuxOffset;
    private float mAmLuxOffsetSaved;
    private float mAmLuxSaved;
    private boolean mBrightnessCalibrationEnabled;
    private float mCalibrationRatio;
    private boolean mCalibrtionModeBeforeEnable;
    private int mCalibrtionTest;
    List<Point> mCameraBrighnessLinePointsList;
    private boolean mCameraModeEnable;
    private String mConfigFilePath;
    private ContentResolver mContentResolver;
    private boolean mCoverModeNoOffsetEnable;
    private int mCurrentUserId;
    List<Point> mDayBrighnessLinePointsList;
    private boolean mDayModeAlgoEnable;
    private boolean mDayModeEnable;
    private int mDayModeModifyMinBrightness;
    private int mDayModeModifyNumPoint;
    List<Point> mDefaultBrighnessLinePointsList;
    List<Point> mDefaultBrighnessLinePointsListCaliBefore;
    private float mDefaultBrightness;
    private float mDefaultBrightnessFromLux;
    private float mDelta;
    private float mDeltaNew;
    private float mDeltaSaved;
    private final int mDeviceActualBrightnessLevel;
    private int mDeviceActualBrightnessNit;
    private int mDeviceStandardBrightnessNit;
    private HwEyeProtectionSpline mEyeProtectionSpline;
    private boolean mEyeProtectionSplineEnable;
    private boolean mIsReboot;
    private boolean mIsUserChange;
    private boolean mIsUserChangeSaved;
    private float mLastLuxDefaultBrightness;
    private float mLastLuxDefaultBrightnessSaved;
    private int mManualBrightnessMaxLimit;
    private boolean mManualMode;
    private float mOffsetBrightenAlphaLeft;
    private float mOffsetBrightenAlphaRight;
    private float mOffsetBrightenRatioLeft;
    private float mOffsetBrightness_last;
    private float mOffsetBrightness_lastSaved;
    private float mOffsetDarkenAlphaLeft;
    private float mOminLevel;
    List<Point> mOminLevelBrighnessLinePointsList;
    private int mOminLevelCount;
    private boolean mOminLevelCountEnable;
    List<Point> mOminLevelCountLevelPointsList;
    private int mOminLevelCountResetLongSetTime;
    private int mOminLevelCountResetLongSetTimeSaved;
    private int mOminLevelCountResetLongTimeTh;
    private int mOminLevelCountSaved;
    private long mOminLevelCountSetTime;
    private int mOminLevelCountValidLuxTh;
    private int mOminLevelCountValidTimeTh;
    private boolean mOminLevelDayModeEnable;
    private boolean mOminLevelModeEnable;
    private boolean mOminLevelOffsetCountEnable;
    private int mOminLevelValidCount;
    private float mPosBrightness;
    private float mPosBrightnessSaved;
    private float mPowerSavingAmluxThreshold;
    private boolean mPowerSavingBrighnessLineEnable;
    List<Point> mPowerSavingBrighnessLinePointsList;
    private boolean mPowerSavingModeEnable;
    private float mStartLuxDefaultBrightness;
    private float mStartLuxDefaultBrightnessSaved;

    private static class Point {
        float x;
        float y;

        public Point(float inx, float iny) {
            this.x = inx;
            this.y = iny;
        }
    }

    static {
        boolean isLoggable = !Log.HWINFO ? Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false : true;
        DEBUG = isLoggable;
    }

    private HwNormalizedSpline(Context context, int deviceActualBrightnessLevel, int deviceActualBrightnessNit, int deviceStandardBrightnessNit) {
        this.mDelta = 0.0f;
        this.mDeltaNew = 0.0f;
        this.mOffsetBrightenRatioLeft = 1.0f;
        this.mOffsetBrightenAlphaLeft = 1.0f;
        this.mOffsetBrightenAlphaRight = 1.0f;
        this.mOffsetDarkenAlphaLeft = 1.0f;
        this.mPosBrightness = minBrightness;
        this.mIsReboot = false;
        this.mIsUserChange = false;
        this.mOffsetBrightness_last = minBrightness;
        this.mLastLuxDefaultBrightness = minBrightness;
        this.mStartLuxDefaultBrightness = minBrightness;
        this.mConfigFilePath = null;
        this.mCurrentUserId = 0;
        this.mAmLux = -1.0f;
        this.mCoverModeNoOffsetEnable = false;
        this.mCameraModeEnable = false;
        this.mManualMode = false;
        this.mManualBrightnessMaxLimit = 255;
        this.mDayModeEnable = false;
        this.mDayModeAlgoEnable = false;
        this.mDayModeModifyNumPoint = 3;
        this.mDayModeModifyMinBrightness = 6;
        this.mPowerSavingBrighnessLineEnable = false;
        this.mPowerSavingModeEnable = false;
        this.mPowerSavingAmluxThreshold = 25.0f;
        this.mAmLuxOffset = -1.0f;
        this.mAmLuxOffsetSaved = -1.0f;
        this.mCalibrationRatio = 1.0f;
        this.mOminLevelModeEnable = false;
        this.mOminLevelOffsetCountEnable = false;
        this.mOminLevelCountEnable = false;
        this.mOminLevelDayModeEnable = false;
        this.mOminLevelCount = 0;
        this.mOminLevelCountSaved = 0;
        this.mOminLevel = 6.0f;
        this.mOminLevelCountValidLuxTh = 5;
        this.mOminLevelCountValidTimeTh = 60;
        this.mOminLevelCountSetTime = -1;
        this.mOminLevelCountResetLongTimeTh = 20160;
        this.mOminLevelCountResetLongSetTime = -1;
        this.mOminLevelValidCount = 0;
        this.mEyeProtectionSplineEnable = true;
        this.mOminLevelBrighnessLinePointsList = null;
        this.mOminLevelCountLevelPointsList = null;
        this.mDefaultBrighnessLinePointsList = null;
        this.mDefaultBrighnessLinePointsListCaliBefore = null;
        this.mEyeProtectionSpline = null;
        this.mCameraBrighnessLinePointsList = null;
        this.mDayBrighnessLinePointsList = null;
        this.mPowerSavingBrighnessLinePointsList = null;
        this.mIsReboot = true;
        this.mContentResolver = context.getContentResolver();
        this.mDeviceActualBrightnessLevel = deviceActualBrightnessLevel;
        this.mDeviceActualBrightnessNit = deviceActualBrightnessNit;
        this.mDeviceStandardBrightnessNit = deviceStandardBrightnessNit;
        loadCameraDefaultBrightnessLine();
        loadPowerSavingDefaultBrightnessLine();
        loadOminLevelCountLevelPointsList();
        try {
            if (!getConfig()) {
                Slog.e(TAG, "getConfig failed! loadDefaultConfig");
                loadDefaultConfig();
            }
        } catch (IOException e) {
            Slog.e(TAG, "IOException : loadDefaultConfig");
            loadDefaultConfig();
        }
        if (SystemProperties.getInt("ro.config.hw_eyes_protection", 7) != 0) {
            this.mEyeProtectionSpline = new HwEyeProtectionSplineImpl(context);
        }
        if (System.getIntForUser(this.mContentResolver, "screen_brightness_mode", 0, this.mCurrentUserId) == 0) {
            float mPosBrightnessSaved = System.getFloatForUser(this.mContentResolver, "hw_screen_auto_brightness_adj", 0.0f, this.mCurrentUserId) * maxBrightness;
            if (Math.abs(mPosBrightnessSaved) > 1.0E-7f) {
                System.putFloatForUser(this.mContentResolver, "hw_screen_auto_brightness_adj", 0.0f, this.mCurrentUserId);
                Slog.i(TAG, "clear autobrightness offset,orig mPosBrightnessSaved=" + mPosBrightnessSaved);
            }
        }
        updateLinePointsListForCalibration();
        loadOffsetParas();
        if (this.mOminLevelModeEnable) {
            getOminLevelBrighnessLinePoints();
        }
    }

    private File getFactoryXmlFile() {
        String xmlPath = String.format("/xml/lcd/%s_%s%s", new Object[]{XML_NAME_NOEXT, "factory", XML_EXT});
        File xmlFile = HwCfgFilePolicy.getCfgFile(xmlPath, 0);
        if (xmlFile != null) {
            return xmlFile;
        }
        Slog.e(TAG, "get xmlFile :" + xmlPath + " failed!");
        return null;
    }

    private String getLcdPanelName() {
        String panelName = null;
        try {
            panelName = FileUtils.readTextFile(new File(String.format("%s", new Object[]{LCD_PANEL_TYPE_PATH})), 0, null).trim().replace(' ', '_');
        } catch (IOException e) {
            Slog.e(TAG, "Error reading lcd panel name", e);
        }
        return panelName;
    }

    private String getVersionFromTouchOemInfo() {
        String version = null;
        try {
            File file = new File(String.format("%s", new Object[]{TOUCH_OEM_INFO_PATH}));
            if (file.exists()) {
                String touch_oem_info = FileUtils.readTextFile(file, 0, null).trim();
                Slog.i(TAG, "touch_oem_info=" + touch_oem_info);
                String[] versionInfo = touch_oem_info.split(",");
                if (versionInfo.length > 15) {
                    try {
                        int productYear = Integer.parseInt(versionInfo[12]);
                        int productMonth = Integer.parseInt(versionInfo[13]);
                        int productDay = Integer.parseInt(versionInfo[14]);
                        Slog.i(TAG, "lcdversionInfo orig productYear=" + productYear + ",productMonth=" + productMonth + ",productDay=" + productDay);
                        if (productYear < 48 || productYear > 57) {
                            Slog.i(TAG, "lcdversionInfo not valid productYear=" + productYear);
                            return null;
                        }
                        productYear -= 48;
                        if (productMonth >= 48 && productMonth <= 57) {
                            productMonth -= 48;
                        } else if (productMonth < 65 || productMonth > 67) {
                            Slog.i(TAG, "lcdversionInfo not valid productMonth=" + productMonth);
                            return null;
                        } else {
                            productMonth = (productMonth - 65) + 10;
                        }
                        if (productDay >= 48 && productDay <= 57) {
                            productDay -= 48;
                        } else if (productDay < 65 || productDay > 88) {
                            Slog.i(TAG, "lcdversionInfo not valid productDay=" + productDay);
                            return null;
                        } else {
                            productDay = (productDay - 65) + 10;
                        }
                        if (productYear > 8) {
                            version = "vn2";
                        } else if (productYear == 8 && productMonth > 1) {
                            version = "vn2";
                        } else if (productYear == 8 && productMonth == 1 && productDay >= 22) {
                            version = "vn2";
                        } else {
                            Slog.i(TAG, "lcdversionInfo not valid version;productYear=" + productYear + ",productMonth=" + productMonth + ",productDay=" + productDay);
                            return null;
                        }
                        Slog.i(TAG, "lcdversionInfo real vn2,productYear=" + productYear + ",productMonth=" + productMonth + ",productDay=" + productDay);
                    } catch (NumberFormatException e) {
                        Slog.i(TAG, "lcdversionInfo versionfile num is not valid,no need version");
                        return null;
                    }
                }
                Slog.i(TAG, "lcdversionInfo versionfile info length is not valid,no need version");
                return version;
            }
            Slog.i(TAG, "lcdversionInfo versionfile is not exists, no need version,filePath=/sys/touchscreen/touch_oem_info");
            return version;
        } catch (IOException e2) {
            Slog.w(TAG, "Error reading touch_oem_info", e2);
        }
    }

    private File getNormalXmlFile() {
        File xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s_%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, getLcdPanelName(), getVersionFromTouchOemInfo(), SystemProperties.get("ro.config.devicecolor"), XML_EXT}), 0);
        Slog.i(TAG, "screenColor=" + screenColor + ",lcdname=" + lcdname + ",lcdversion=" + lcdversion);
        if (xmlFile == null) {
            xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, lcdversion, XML_EXT}), 0);
            if (xmlFile == null) {
                xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, screenColor, XML_EXT}), 0);
                if (xmlFile == null) {
                    xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, XML_EXT}), 0);
                    if (xmlFile == null) {
                        xmlFile = HwCfgFilePolicy.getCfgFile(String.format("/xml/lcd/%s_%s%s", new Object[]{XML_NAME_NOEXT, screenColor, XML_EXT}), 0);
                        if (xmlFile == null) {
                            String xmlPath = String.format("/xml/lcd/%s", new Object[]{HW_LABC_CONFIG_FILE});
                            xmlFile = HwCfgFilePolicy.getCfgFile(xmlPath, 0);
                            if (xmlFile == null) {
                                Slog.e(TAG, "get xmlFile :" + xmlPath + " failed!");
                                return null;
                            }
                        }
                    }
                }
            }
        }
        return xmlFile;
    }

    private boolean getConfig() throws IOException {
        File xmlFile;
        Throwable th;
        String currentMode = SystemProperties.get("ro.runmode");
        Slog.i(TAG, "currentMode=" + currentMode);
        if (currentMode == null) {
            xmlFile = getNormalXmlFile();
            if (xmlFile == null) {
                return false;
            }
        } else if (currentMode.equals("factory")) {
            xmlFile = getFactoryXmlFile();
            if (xmlFile == null) {
                return false;
            }
        } else if (currentMode.equals("normal")) {
            xmlFile = getNormalXmlFile();
            if (xmlFile == null) {
                return false;
            }
        } else {
            xmlFile = getNormalXmlFile();
            if (xmlFile == null) {
                return false;
            }
        }
        FileInputStream fileInputStream = null;
        try {
            FileInputStream inputStream = new FileInputStream(xmlFile);
            try {
                if (getConfigFromXML(inputStream)) {
                    if (checkConfigLoadedFromXML()) {
                        if (DEBUG) {
                            printConfigFromXML();
                        }
                        initLinePointsList();
                        if (DEBUG) {
                            Slog.i(TAG, "mBrightnessCalibrationEnabled=" + this.mBrightnessCalibrationEnabled + ",mDeviceActualBrightnessNit=" + this.mDeviceActualBrightnessNit + ",mDeviceStandardBrightnessNit=" + this.mDeviceStandardBrightnessNit);
                        }
                        if (this.mBrightnessCalibrationEnabled) {
                            brightnessCalibration(this.mDefaultBrighnessLinePointsList, this.mDeviceActualBrightnessNit, this.mDeviceStandardBrightnessNit);
                        }
                    }
                    this.mConfigFilePath = xmlFile.getAbsolutePath();
                    if (DEBUG) {
                        Slog.i(TAG, "get xmlFile :" + this.mConfigFilePath);
                    }
                    inputStream.close();
                    getDayBrightnessLinePoints();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    return true;
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                fileInputStream = inputStream;
                return false;
            } catch (FileNotFoundException e) {
                fileInputStream = inputStream;
                Slog.e(TAG, "getConfig : FileNotFoundException");
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                return false;
            } catch (IOException e2) {
                fileInputStream = inputStream;
                try {
                    Slog.e(TAG, "getConfig : IOException");
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                fileInputStream = inputStream;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e3) {
            Slog.e(TAG, "getConfig : FileNotFoundException");
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return false;
        } catch (IOException e4) {
            Slog.e(TAG, "getConfig : IOException");
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return false;
        }
    }

    private boolean checkConfigLoadedFromXML() {
        if (this.mDefaultBrightness <= 0.0f) {
            loadDefaultConfig();
            Slog.e(TAG, "LoadXML false for mDefaultBrightness <= 0, LoadDefaultConfig!");
            return false;
        } else if (!checkPointsListIsOK(this.mDefaultBrighnessLinePointsList)) {
            loadDefaultConfig();
            Slog.e(TAG, "checkPointsList mDefaultBrighnessLinePointsList is wrong, LoadDefaultConfig!");
            return false;
        } else if (this.mOffsetBrightenRatioLeft <= 0.0f || this.mOffsetBrightenAlphaLeft < 0.0f || ((double) this.mOffsetBrightenAlphaLeft) > 1.0d) {
            loadDefaultConfig();
            Slog.e(TAG, "LoadXML false, mOffsetBrightenRatioLeft=" + this.mOffsetBrightenRatioLeft + ",mOffsetBrightenAlphaLeft=" + this.mOffsetBrightenAlphaLeft);
            return false;
        } else if (this.mOffsetBrightenAlphaRight < 0.0f || ((double) this.mOffsetBrightenAlphaRight) > 1.0d) {
            loadDefaultConfig();
            Slog.e(TAG, "LoadXML false, mOffsetBrightenAlphaRight=" + this.mOffsetBrightenAlphaRight);
            return false;
        } else if (this.mOffsetDarkenAlphaLeft < 0.0f || ((double) this.mOffsetDarkenAlphaLeft) > 1.0d) {
            loadDefaultConfig();
            Slog.e(TAG, "LoadXML false, mOffsetDarkenAlphaLeft=" + this.mOffsetDarkenAlphaLeft);
            return false;
        } else {
            if (this.mOminLevelModeEnable) {
                if (this.mOminLevelCountValidLuxTh < 0) {
                    loadDefaultConfig();
                    Slog.e(TAG, "LoadXML false, mOminLevelCountValidLuxTh=" + this.mOminLevelCountValidLuxTh);
                    return false;
                } else if (this.mOminLevelCountValidTimeTh < 0) {
                    loadDefaultConfig();
                    Slog.e(TAG, "LoadXML false, mOminLevelCountValidTimeTh=" + this.mOminLevelCountValidTimeTh);
                    return false;
                } else if (!checkPointsListIsOK(this.mOminLevelCountLevelPointsList)) {
                    loadDefaultConfig();
                    Slog.e(TAG, "checkPointsList mOminLevelPointsList is wrong, LoadDefaultConfig!");
                    return false;
                }
            }
            if (DEBUG) {
                Slog.i(TAG, "checkConfigLoadedFromXML success!");
            }
            return true;
        }
    }

    private void initLinePointsList() {
        int listSize = this.mDefaultBrighnessLinePointsList.size();
        for (int i = 0; i < listSize; i++) {
            Point tempPoint = new Point();
            tempPoint.x = ((Point) this.mDefaultBrighnessLinePointsList.get(i)).x;
            tempPoint.y = ((Point) this.mDefaultBrighnessLinePointsList.get(i)).y;
            if (this.mDefaultBrighnessLinePointsListCaliBefore == null) {
                this.mDefaultBrighnessLinePointsListCaliBefore = new ArrayList();
            }
            this.mDefaultBrighnessLinePointsListCaliBefore.add(tempPoint);
        }
        System.putIntForUser(this.mContentResolver, "spline_calibration_test", 0, this.mCurrentUserId);
        if (DEBUG) {
            Slog.i(TAG, "init list_DefaultBrighnessLinePointsBeforeCali");
        }
    }

    private void brightnessCalibration(List<Point> LinePointsList, int actulBrightnessNit, int standardBrightnessNit) {
        List<Point> mLinePointsList = LinePointsList;
        int mActulBrightnessNit = actulBrightnessNit;
        int mStandardBrightnessNit = standardBrightnessNit;
        if (actulBrightnessNit < 400 || actulBrightnessNit > 1000 || standardBrightnessNit > 1000 || standardBrightnessNit <= 0) {
            this.mCalibrationRatio = 1.0f;
            Slog.e(TAG, "error input brightnessNit:mStandardBrightnessNit=" + standardBrightnessNit + ",mActulBrightnessNit=" + actulBrightnessNit);
        } else {
            this.mCalibrationRatio = ((float) standardBrightnessNit) / ((float) actulBrightnessNit);
            if (DEBUG) {
                Slog.i(TAG, "mCalibrationRatio=" + this.mCalibrationRatio + ",mStandardBrightnessNit=" + standardBrightnessNit + ",mActulBrightnessNit=" + actulBrightnessNit);
            }
        }
        int listSize = LinePointsList.size();
        for (int i = 1; i < listSize; i++) {
            Point pointTemp = (Point) LinePointsList.get(i);
            if (pointTemp.y > minBrightness && pointTemp.y < maxBrightness) {
                pointTemp.y *= this.mCalibrationRatio;
                if (pointTemp.y <= minBrightness) {
                    pointTemp.y = minBrightness;
                }
                if (pointTemp.y >= maxBrightness) {
                    pointTemp.y = maxBrightness;
                }
            }
        }
        for (Point temp : LinePointsList) {
            if (DEBUG) {
                Slog.i(TAG, "LoadXMLConfig_NewCalibrationBrighnessLinePoints x = " + temp.x + ", y = " + temp.y);
            }
        }
    }

    private void updateLinePointsListForCalibration() {
        if (this.mBrightnessCalibrationEnabled && Math.abs(this.mCalibrationRatio - 1.0f) > 1.0E-7f) {
            if (this.mPowerSavingBrighnessLineEnable && this.mPowerSavingBrighnessLinePointsList != null) {
                updateNewLinePointsListForCalibration(this.mPowerSavingBrighnessLinePointsList);
                Slog.i(TAG, "update PowerSavingBrighnessLinePointsList for calibration");
                if (DEBUG && this.mPowerSavingBrighnessLinePointsList != null) {
                    for (Point temp : this.mPowerSavingBrighnessLinePointsList) {
                        Slog.d(TAG, "LoadXMLConfig_NewCalibrationPowerSavingPointsList x = " + temp.x + ", y = " + temp.y);
                    }
                }
            }
            if (this.mCameraBrighnessLinePointsList != null) {
                updateNewLinePointsListForCalibration(this.mCameraBrighnessLinePointsList);
                if (DEBUG) {
                    Slog.i(TAG, "update mCameraBrighnessLinePointsList for calibration");
                }
            }
        }
    }

    private void updateNewLinePointsListForCalibration(List<Point> LinePointsList) {
        List<Point> mLinePointsList = LinePointsList;
        int listSize = LinePointsList.size();
        for (int i = 1; i < listSize; i++) {
            Point pointTemp = (Point) LinePointsList.get(i);
            if (pointTemp.y > minBrightness && pointTemp.y < maxBrightness) {
                pointTemp.y *= this.mCalibrationRatio;
                if (pointTemp.y <= minBrightness) {
                    pointTemp.y = minBrightness;
                }
                if (pointTemp.y >= maxBrightness) {
                    pointTemp.y = maxBrightness;
                }
            }
        }
    }

    private boolean checkPointsListIsOK(List<Point> LinePointsList) {
        List<Point> mLinePointsList = LinePointsList;
        if (LinePointsList == null) {
            Slog.e(TAG, "LoadXML false for mLinePointsList == null");
            return false;
        } else if (LinePointsList.size() <= 2 || LinePointsList.size() >= 100) {
            Slog.e(TAG, "LoadXML false for mLinePointsList number is wrong");
            return false;
        } else {
            Point lastPoint = null;
            for (Point tmpPoint : LinePointsList) {
                if (lastPoint == null) {
                    lastPoint = tmpPoint;
                } else if (lastPoint.x >= tmpPoint.x) {
                    loadDefaultConfig();
                    Slog.e(TAG, "LoadXML false for mLinePointsList is wrong");
                    return false;
                } else {
                    lastPoint = tmpPoint;
                }
            }
            return true;
        }
    }

    private boolean checkDayBrightness() {
        if (this.mDefaultBrighnessLinePointsList.size() < this.mDayModeModifyNumPoint) {
            Slog.e(TAG, "mDefaultBrighnessLinePointsList.size < mDayModeModifyNumPoint");
            return true;
        } else if (((Point) this.mDefaultBrighnessLinePointsList.get(this.mDayModeModifyNumPoint - 1)).y >= ((float) this.mDayModeModifyMinBrightness)) {
            return false;
        } else {
            Slog.e(TAG, "temp.y < mDayModeModifyMinBrightness");
            return true;
        }
    }

    private void getDayBrightnessLinePoints() {
        if (this.mDefaultBrighnessLinePointsList != null) {
            float u;
            float v;
            Point temp;
            if (checkDayBrightness()) {
                u = 1.0f;
                v = 0.0f;
                Slog.e(TAG, "error DayBrightnessLinePoints input!");
            } else {
                temp = (Point) this.mDefaultBrighnessLinePointsList.get(this.mDayModeModifyNumPoint - 1);
                u = ((temp.y * 1.0f) - (((float) this.mDayModeModifyMinBrightness) * 1.0f)) / ((temp.y * 1.0f) - minBrightness);
                v = (temp.y * ((((float) this.mDayModeModifyMinBrightness) * 1.0f) - minBrightness)) / ((temp.y * 1.0f) - minBrightness);
                Slog.i(TAG, "DayMode:u=" + u + ", v=" + v);
            }
            if (this.mDayBrighnessLinePointsList == null) {
                this.mDayBrighnessLinePointsList = new ArrayList();
            } else {
                this.mDayBrighnessLinePointsList.clear();
            }
            int cntPoint = 0;
            for (Point temp2 : this.mDefaultBrighnessLinePointsList) {
                cntPoint++;
                if (cntPoint > this.mDayModeModifyNumPoint) {
                    this.mDayBrighnessLinePointsList.add(temp2);
                } else {
                    this.mDayBrighnessLinePointsList.add(new Point(temp2.x, (temp2.y * u) + v));
                }
            }
            for (Point temp22 : this.mDayBrighnessLinePointsList) {
                if (DEBUG) {
                    Slog.i(TAG, "DayMode:DayBrightnessLine: =" + temp22.x + ", y=" + temp22.y);
                }
            }
        }
    }

    private void getOminLevelBrighnessLinePoints() {
        if (this.mOminLevelBrighnessLinePointsList == null) {
            this.mOminLevelBrighnessLinePointsList = new ArrayList();
        } else {
            this.mOminLevelBrighnessLinePointsList.clear();
        }
        if (this.mDayBrighnessLinePointsList != null) {
            for (Point temp : this.mDayBrighnessLinePointsList) {
                this.mOminLevelBrighnessLinePointsList.add(new Point(temp.x, temp.y));
                if (DEBUG) {
                    Slog.d(TAG, "mOminLevelMode:LinePointsList: x=" + temp.x + ", y=" + temp.y);
                }
            }
            updateOminLevelBrighnessLinePoints();
            return;
        }
        Slog.w(TAG, "mOminLevelMode getLineFailed, mDayBrighnessLinePointsList==null");
    }

    public void updateOminLevelBrighnessLinePoints() {
        if (this.mOminLevelBrighnessLinePointsList == null) {
            Slog.w(TAG, "mOminLevelMode mOminLevelBrighnessLinePointsList==null,return");
            return;
        }
        int listsize = this.mOminLevelBrighnessLinePointsList.size();
        int countThMin = getOminLevelCountThMin(this.mOminLevelCountLevelPointsList);
        int countThMax = getOminLevelCountThMax(this.mOminLevelCountLevelPointsList);
        if (listsize >= 2) {
            Point temp = (Point) this.mOminLevelBrighnessLinePointsList.get(0);
            Point temp1 = (Point) this.mOminLevelBrighnessLinePointsList.get(1);
            if (this.mOminLevelCount >= countThMin) {
                temp.y = getOminLevelFromCount(this.mOminLevelCount);
                this.mOminLevelCountEnable = true;
            } else {
                temp.y = getOminLevelThMin(this.mOminLevelCountLevelPointsList);
                this.mOminLevelCountEnable = false;
            }
            if (temp.y > temp1.y) {
                temp.y = temp1.y;
                Slog.w(TAG, "mOminLevelMode updateMinLevel x(0)=" + temp.x + ",y(0)=" + temp.y + ",y(0)==y(1)");
            }
            if (DEBUG && this.mOminLevelCountEnable && this.mOminLevelCount < countThMax) {
                Slog.d(TAG, "mOminLevelMode updateMinLevel x(0)=" + temp.x + ",y(0)=" + temp.y);
            }
        } else {
            Slog.w(TAG, "mOminLevelMode mOminLevelBrighnessLinePointsList==null");
        }
    }

    private float getOminLevelFromCount(int ominLevelCount) {
        int countThMin = getOminLevelCountThMin(this.mOminLevelCountLevelPointsList);
        int countThMax = getOminLevelCountThMax(this.mOminLevelCountLevelPointsList);
        float levelMin = getOminLevelThMin(this.mOminLevelCountLevelPointsList);
        float levelMax = getOminLevelThMax(this.mOminLevelCountLevelPointsList);
        if (ominLevelCount < countThMin) {
            this.mOminLevel = levelMin;
        } else if (ominLevelCount >= countThMax) {
            this.mOminLevel = levelMax;
        } else {
            this.mOminLevel = getOminLevelFromCountInternal(this.mOminLevelCountLevelPointsList, (float) ominLevelCount);
            Slog.i(TAG, "mOminLevelMode ominLevelCount=" + ominLevelCount + ",mOminLevel=" + this.mOminLevel + ",cmin=" + countThMin + ",cmax=" + countThMax);
        }
        return this.mOminLevel;
    }

    private float getOminLevelFromCountInternal(List<Point> linePointsList, float levelCount) {
        List<Point> linePointsListIn = linePointsList;
        float brightnessLevel = minBrightness;
        if (linePointsList == null) {
            Slog.i(TAG, "mOminLevelMode linePointsListIn==null,return minBrightness");
            return minBrightness;
        }
        Point temp1 = null;
        for (Point temp : linePointsList) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (levelCount < temp.x) {
                brightnessLevel = temp1.y;
            } else {
                temp1 = temp;
            }
        }
        return brightnessLevel;
    }

    private void loadDefaultConfig() {
        this.mDefaultBrightness = 100.0f;
        this.mBrightnessCalibrationEnabled = false;
        if (this.mDefaultBrighnessLinePointsList != null) {
            this.mDefaultBrighnessLinePointsList.clear();
        } else {
            this.mDefaultBrighnessLinePointsList = new ArrayList();
        }
        this.mDefaultBrighnessLinePointsList.add(new Point(0.0f, minBrightness));
        this.mDefaultBrighnessLinePointsList.add(new Point(25.0f, 46.5f));
        this.mDefaultBrighnessLinePointsList.add(new Point(1995.0f, 140.7f));
        this.mDefaultBrighnessLinePointsList.add(new Point(4000.0f, maxBrightness));
        this.mDefaultBrighnessLinePointsList.add(new Point(40000.0f, maxBrightness));
        loadCameraDefaultBrightnessLine();
        loadPowerSavingDefaultBrightnessLine();
        loadOminLevelCountLevelPointsList();
        this.mOminLevelModeEnable = false;
        if (DEBUG) {
            printConfigFromXML();
        }
    }

    private void loadCameraDefaultBrightnessLine() {
        if (this.mCameraBrighnessLinePointsList != null) {
            this.mCameraBrighnessLinePointsList.clear();
        } else {
            this.mCameraBrighnessLinePointsList = new ArrayList();
        }
        this.mCameraBrighnessLinePointsList.add(new Point(0.0f, minBrightness));
        this.mCameraBrighnessLinePointsList.add(new Point(25.0f, 46.5f));
        this.mCameraBrighnessLinePointsList.add(new Point(1995.0f, 140.7f));
        this.mCameraBrighnessLinePointsList.add(new Point(4000.0f, maxBrightness));
        this.mCameraBrighnessLinePointsList.add(new Point(40000.0f, maxBrightness));
    }

    private void loadPowerSavingDefaultBrightnessLine() {
        if (this.mPowerSavingBrighnessLinePointsList != null) {
            this.mPowerSavingBrighnessLinePointsList.clear();
        } else {
            this.mPowerSavingBrighnessLinePointsList = new ArrayList();
        }
        this.mPowerSavingBrighnessLinePointsList.add(new Point(0.0f, minBrightness));
        this.mPowerSavingBrighnessLinePointsList.add(new Point(25.0f, 46.5f));
        this.mPowerSavingBrighnessLinePointsList.add(new Point(1995.0f, 140.7f));
        this.mPowerSavingBrighnessLinePointsList.add(new Point(4000.0f, maxBrightness));
        this.mPowerSavingBrighnessLinePointsList.add(new Point(40000.0f, maxBrightness));
    }

    private void loadOminLevelCountLevelPointsList() {
        if (this.mOminLevelCountLevelPointsList != null) {
            this.mOminLevelCountLevelPointsList.clear();
        } else {
            this.mOminLevelCountLevelPointsList = new ArrayList();
        }
        this.mOminLevelCountLevelPointsList.add(new Point(5.0f, 6.0f));
        this.mOminLevelCountLevelPointsList.add(new Point(10.0f, 7.0f));
        this.mOminLevelCountLevelPointsList.add(new Point(20.0f, 8.0f));
    }

    private void printConfigFromXML() {
        Slog.i(TAG, "LoadXMLConfig_DefaultBrightness=" + this.mDefaultBrightness);
        Slog.i(TAG, "LoadXMLConfig_mBrightnessCalibrationEnabled=" + this.mBrightnessCalibrationEnabled + ",mPowerSavingBrighnessLineEnable=" + this.mPowerSavingBrighnessLineEnable + ",mOffsetBrightenRatioLeft=" + this.mOffsetBrightenRatioLeft + ",mOffsetBrightenAlphaLeft=" + this.mOffsetBrightenAlphaLeft + ",mOffsetBrightenAlphaRight=" + this.mOffsetBrightenAlphaRight + ",mOffsetDarkenAlphaLeft=" + this.mOffsetDarkenAlphaLeft + ",mManualMode=" + this.mManualMode + ",mManualBrightnessMaxLimit=" + this.mManualBrightnessMaxLimit);
        Slog.i(TAG, "LoadXMLConfig_mOminLevelMode=" + this.mOminLevelModeEnable + ",mCountEnable=" + this.mOminLevelOffsetCountEnable + ",mDayEn=" + this.mOminLevelDayModeEnable + ",ValidLux=" + this.mOminLevelCountValidLuxTh + ",ValidTime=" + this.mOminLevelCountValidTimeTh + ",mLongTime=" + this.mOminLevelCountResetLongTimeTh + ",EyeEn=" + this.mEyeProtectionSplineEnable);
        for (Point temp : this.mDefaultBrighnessLinePointsList) {
            Slog.i(TAG, "LoadXMLConfig_DefaultBrighnessLinePoints x = " + temp.x + ", y = " + temp.y);
        }
        for (Point temp2 : this.mCameraBrighnessLinePointsList) {
            Slog.i(TAG, "LoadXMLConfig_CameraBrighnessLinePointsList x = " + temp2.x + ", y = " + temp2.y);
        }
        for (Point temp22 : this.mPowerSavingBrighnessLinePointsList) {
            Slog.i(TAG, "LoadXMLConfig_mPowerSavingBrighnessLinePointsList x = " + temp22.x + ", y = " + temp22.y);
        }
        if (this.mOminLevelModeEnable && this.mOminLevelCountLevelPointsList != null) {
            for (Point temp222 : this.mOminLevelCountLevelPointsList) {
                Slog.i(TAG, "LoadXMLConfig_mOminLevelCountLevelPointsList x = " + temp222.x + ", y = " + temp222.y);
            }
        }
    }

    private boolean getConfigFromXML(InputStream inStream) {
        if (DEBUG) {
            Slog.i(TAG, "getConfigFromeXML");
        }
        boolean DefaultBrightnessLoaded = false;
        boolean DefaultBrighnessLinePointsListsLoadStarted = false;
        boolean DefaultBrighnessLinePointsListLoaded = false;
        boolean CameraBrightnessLinePointsListsLoadStarted = false;
        boolean PowerSavingBrightnessLinePointsListsLoadStarted = false;
        boolean OminLevelCountLevelLinePointsListsLoadStarted = false;
        boolean configGroupLoadStarted = false;
        boolean loadFinished = false;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(inStream, "UTF-8");
            int eventType = parser.getEventType();
            while (eventType != 1) {
                String name;
                switch (eventType) {
                    case 2:
                        name = parser.getName();
                        if (!name.equals(XML_NAME_NOEXT)) {
                            if (configGroupLoadStarted) {
                                if (!name.equals("DefaultBrightness")) {
                                    if (!name.equals("BrightnessCalibrationEnabled")) {
                                        if (!name.equals("OffsetBrightenRatioLeft")) {
                                            if (!name.equals("OffsetBrightenAlphaLeft")) {
                                                if (!name.equals("OffsetBrightenAlphaRight")) {
                                                    if (!name.equals("OffsetDarkenAlphaLeft")) {
                                                        if (!name.equals("DefaultBrightnessPoints")) {
                                                            Point currentPoint;
                                                            String s;
                                                            if (!name.equals("Point") || !DefaultBrighnessLinePointsListsLoadStarted) {
                                                                if (!name.equals("CameraBrightnessPoints")) {
                                                                    if (!name.equals("Point") || !CameraBrightnessLinePointsListsLoadStarted) {
                                                                        if (!name.equals("PowerSavingBrightnessPoints")) {
                                                                            if (!name.equals("Point") || !PowerSavingBrightnessLinePointsListsLoadStarted) {
                                                                                if (!name.equals("PowerSavingBrighnessLineEnable")) {
                                                                                    if (!name.equals("ManualMode")) {
                                                                                        if (!name.equals("ManualBrightnessMaxLimit")) {
                                                                                            if (!name.equals("DayModeAlgoEnable")) {
                                                                                                if (!name.equals("DayModeModifyNumPoint")) {
                                                                                                    if (!name.equals("DayModeModifyMinBrightness")) {
                                                                                                        if (!name.equals("OminLevelModeEnable")) {
                                                                                                            if (!name.equals("OminLevelOffsetCountEnable")) {
                                                                                                                if (!name.equals("OminLevelDayModeEnable")) {
                                                                                                                    if (!name.equals("OminLevelCountValidLuxTh")) {
                                                                                                                        if (!name.equals("OminLevelCountValidTimeTh")) {
                                                                                                                            if (!name.equals("OminLevelCountResetLongTimeTh")) {
                                                                                                                                if (!name.equals("EyeProtectionSplineEnable")) {
                                                                                                                                    if (!name.equals("OminLevelCountLevelLinePoints")) {
                                                                                                                                        if (name.equals("Point") && OminLevelCountLevelLinePointsListsLoadStarted) {
                                                                                                                                            currentPoint = new Point();
                                                                                                                                            s = parser.nextText();
                                                                                                                                            currentPoint.x = Float.parseFloat(s.split(",")[0]);
                                                                                                                                            currentPoint.y = Float.parseFloat(s.split(",")[1]);
                                                                                                                                            if (this.mOminLevelCountLevelPointsList == null) {
                                                                                                                                                this.mOminLevelCountLevelPointsList = new ArrayList();
                                                                                                                                            }
                                                                                                                                            this.mOminLevelCountLevelPointsList.add(currentPoint);
                                                                                                                                            break;
                                                                                                                                        }
                                                                                                                                    }
                                                                                                                                    OminLevelCountLevelLinePointsListsLoadStarted = true;
                                                                                                                                    if (this.mOminLevelCountLevelPointsList != null) {
                                                                                                                                        this.mOminLevelCountLevelPointsList.clear();
                                                                                                                                        break;
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                this.mEyeProtectionSplineEnable = Boolean.parseBoolean(parser.nextText());
                                                                                                                                break;
                                                                                                                            }
                                                                                                                            this.mOminLevelCountResetLongTimeTh = Integer.parseInt(parser.nextText());
                                                                                                                            break;
                                                                                                                        }
                                                                                                                        this.mOminLevelCountValidTimeTh = Integer.parseInt(parser.nextText());
                                                                                                                        break;
                                                                                                                    }
                                                                                                                    this.mOminLevelCountValidLuxTh = Integer.parseInt(parser.nextText());
                                                                                                                    break;
                                                                                                                }
                                                                                                                this.mOminLevelDayModeEnable = Boolean.parseBoolean(parser.nextText());
                                                                                                                break;
                                                                                                            }
                                                                                                            this.mOminLevelOffsetCountEnable = Boolean.parseBoolean(parser.nextText());
                                                                                                            break;
                                                                                                        }
                                                                                                        this.mOminLevelModeEnable = Boolean.parseBoolean(parser.nextText());
                                                                                                        break;
                                                                                                    }
                                                                                                    this.mDayModeModifyMinBrightness = Integer.parseInt(parser.nextText());
                                                                                                    break;
                                                                                                }
                                                                                                this.mDayModeModifyNumPoint = Integer.parseInt(parser.nextText());
                                                                                                break;
                                                                                            }
                                                                                            this.mDayModeAlgoEnable = Boolean.parseBoolean(parser.nextText());
                                                                                            break;
                                                                                        } else if (this.mManualMode) {
                                                                                            this.mManualBrightnessMaxLimit = Integer.parseInt(parser.nextText());
                                                                                            break;
                                                                                        }
                                                                                    } else if (Integer.parseInt(parser.nextText()) == 1) {
                                                                                        this.mManualMode = true;
                                                                                        break;
                                                                                    }
                                                                                }
                                                                                this.mPowerSavingBrighnessLineEnable = Boolean.parseBoolean(parser.nextText());
                                                                                break;
                                                                            }
                                                                            currentPoint = new Point();
                                                                            s = parser.nextText();
                                                                            currentPoint.x = Float.parseFloat(s.split(",")[0]);
                                                                            currentPoint.y = Float.parseFloat(s.split(",")[1]);
                                                                            if (this.mPowerSavingBrighnessLinePointsList == null) {
                                                                                this.mPowerSavingBrighnessLinePointsList = new ArrayList();
                                                                            }
                                                                            this.mPowerSavingBrighnessLinePointsList.add(currentPoint);
                                                                            break;
                                                                        }
                                                                        PowerSavingBrightnessLinePointsListsLoadStarted = true;
                                                                        if (this.mPowerSavingBrighnessLinePointsList != null) {
                                                                            this.mPowerSavingBrighnessLinePointsList.clear();
                                                                            break;
                                                                        }
                                                                    }
                                                                    currentPoint = new Point();
                                                                    s = parser.nextText();
                                                                    currentPoint.x = Float.parseFloat(s.split(",")[0]);
                                                                    currentPoint.y = Float.parseFloat(s.split(",")[1]);
                                                                    if (this.mCameraBrighnessLinePointsList == null) {
                                                                        this.mCameraBrighnessLinePointsList = new ArrayList();
                                                                    }
                                                                    this.mCameraBrighnessLinePointsList.add(currentPoint);
                                                                    break;
                                                                }
                                                                CameraBrightnessLinePointsListsLoadStarted = true;
                                                                if (this.mCameraBrighnessLinePointsList != null) {
                                                                    this.mCameraBrighnessLinePointsList.clear();
                                                                    break;
                                                                }
                                                            }
                                                            currentPoint = new Point();
                                                            s = parser.nextText();
                                                            currentPoint.x = Float.parseFloat(s.split(",")[0]);
                                                            currentPoint.y = Float.parseFloat(s.split(",")[1]);
                                                            if (this.mDefaultBrighnessLinePointsList == null) {
                                                                this.mDefaultBrighnessLinePointsList = new ArrayList();
                                                            }
                                                            this.mDefaultBrighnessLinePointsList.add(currentPoint);
                                                            break;
                                                        }
                                                        DefaultBrighnessLinePointsListsLoadStarted = true;
                                                        break;
                                                    }
                                                    this.mOffsetDarkenAlphaLeft = Float.parseFloat(parser.nextText());
                                                    break;
                                                }
                                                this.mOffsetBrightenAlphaRight = Float.parseFloat(parser.nextText());
                                                break;
                                            }
                                            this.mOffsetBrightenAlphaLeft = Float.parseFloat(parser.nextText());
                                            break;
                                        }
                                        this.mOffsetBrightenRatioLeft = Float.parseFloat(parser.nextText());
                                        break;
                                    }
                                    this.mBrightnessCalibrationEnabled = Boolean.parseBoolean(parser.nextText());
                                    break;
                                }
                                this.mDefaultBrightness = Float.parseFloat(parser.nextText());
                                DefaultBrightnessLoaded = true;
                                break;
                            }
                        } else if (this.mDeviceActualBrightnessLevel != 0) {
                            String deviceLevelString = parser.getAttributeValue(null, "level");
                            if (deviceLevelString != null && deviceLevelString.length() != 0) {
                                if (Integer.parseInt(deviceLevelString) == this.mDeviceActualBrightnessLevel) {
                                    if (DEBUG) {
                                        Slog.i(TAG, "actualDeviceLevel = " + this.mDeviceActualBrightnessLevel + ", find matched level in XML, load start");
                                    }
                                    configGroupLoadStarted = true;
                                    break;
                                }
                            }
                            if (DEBUG) {
                                Slog.i(TAG, "actualDeviceLevel = " + this.mDeviceActualBrightnessLevel + ", but can't find level in XML, load start");
                            }
                            configGroupLoadStarted = true;
                            break;
                        } else {
                            if (DEBUG) {
                                Slog.i(TAG, "actualDeviceLevel = 0, load started");
                            }
                            configGroupLoadStarted = true;
                            break;
                        }
                        break;
                    case 3:
                        name = parser.getName();
                        if (name.equals(XML_NAME_NOEXT) && configGroupLoadStarted) {
                            loadFinished = true;
                            break;
                        } else if (configGroupLoadStarted) {
                            if (name.equals("DefaultBrightnessPoints")) {
                                DefaultBrighnessLinePointsListsLoadStarted = false;
                                if (this.mDefaultBrighnessLinePointsList != null) {
                                    DefaultBrighnessLinePointsListLoaded = true;
                                    break;
                                }
                                Slog.e(TAG, "no DefaultBrightnessPoints loaded!");
                                return false;
                            } else if (name.equals("CameraBrightnessPoints")) {
                                CameraBrightnessLinePointsListsLoadStarted = false;
                                if (this.mCameraBrighnessLinePointsList != null) {
                                    break;
                                }
                                Slog.e(TAG, "no CameraBrightnessPoints loaded!");
                                return false;
                            } else if (name.equals("PowerSavingBrightnessPoints")) {
                                PowerSavingBrightnessLinePointsListsLoadStarted = false;
                                if (this.mPowerSavingBrighnessLinePointsList != null) {
                                    break;
                                }
                                Slog.e(TAG, "no PowerSavingBrightnessPoints loaded!");
                                return false;
                            } else if (name.equals("OminLevelCountLevelLinePoints")) {
                                OminLevelCountLevelLinePointsListsLoadStarted = false;
                                if (this.mOminLevelCountLevelPointsList != null) {
                                    break;
                                }
                                Slog.e(TAG, "no OminLevelCountLevelPointsList loaded!");
                                return false;
                            }
                        }
                        break;
                }
                if (!loadFinished) {
                    eventType = parser.next();
                } else if (DefaultBrightnessLoaded || !DefaultBrighnessLinePointsListLoaded) {
                    if (!configGroupLoadStarted) {
                        Slog.e(TAG, "actualDeviceLevel = " + this.mDeviceActualBrightnessLevel + ", can't find matched level in XML, load failed!");
                        return false;
                    }
                    Slog.e(TAG, "getConfigFromeXML false!");
                    return false;
                } else {
                    if (DEBUG) {
                        Slog.i(TAG, "getConfigFromeXML success!");
                    }
                    return true;
                }
            }
            if (DefaultBrightnessLoaded) {
            }
            if (configGroupLoadStarted) {
                Slog.e(TAG, "actualDeviceLevel = " + this.mDeviceActualBrightnessLevel + ", can't find matched level in XML, load failed!");
                return false;
            }
        } catch (XmlPullParserException e) {
            Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
        } catch (IOException e2) {
            Slog.e(TAG, "getConfigFromXML : IOException");
        } catch (NumberFormatException e3) {
            Slog.e(TAG, "getConfigFromXML : NumberFormatException");
        }
        Slog.e(TAG, "getConfigFromeXML false!");
        return false;
    }

    public void updateCurrentUserId(int userId) {
        if (DEBUG) {
            Slog.d(TAG, "save old user's paras and load new user's paras when user change ");
        }
        saveOffsetParas();
        this.mCurrentUserId = userId;
        loadOffsetParas();
    }

    public void loadOffsetParas() {
        boolean z = true;
        this.mPosBrightnessSaved = System.getFloatForUser(this.mContentResolver, "hw_screen_auto_brightness_adj", 0.0f, this.mCurrentUserId) * maxBrightness;
        this.mPosBrightness = this.mPosBrightnessSaved;
        this.mDeltaSaved = System.getFloatForUser(this.mContentResolver, "spline_delta", 0.0f, this.mCurrentUserId);
        this.mDeltaNew = this.mDeltaSaved;
        if (System.getIntForUser(this.mContentResolver, "spline_is_user_change", 0, this.mCurrentUserId) != 1) {
            z = false;
        }
        this.mIsUserChangeSaved = z;
        this.mIsUserChange = this.mIsUserChangeSaved;
        this.mOffsetBrightness_lastSaved = System.getFloatForUser(this.mContentResolver, "spline_offset_brightness_last", minBrightness, this.mCurrentUserId);
        this.mOffsetBrightness_last = this.mOffsetBrightness_lastSaved;
        this.mLastLuxDefaultBrightnessSaved = System.getFloatForUser(this.mContentResolver, "spline_last_lux_default_brightness", minBrightness, this.mCurrentUserId);
        this.mLastLuxDefaultBrightness = this.mLastLuxDefaultBrightnessSaved;
        this.mStartLuxDefaultBrightnessSaved = System.getFloatForUser(this.mContentResolver, "spline_start_lux_default_brightness", minBrightness, this.mCurrentUserId);
        this.mStartLuxDefaultBrightness = this.mStartLuxDefaultBrightnessSaved;
        this.mDelta = this.mPosBrightness - this.mStartLuxDefaultBrightness;
        this.mAmLuxOffset = System.getFloatForUser(this.mContentResolver, "spline_ambient_lux_offset", -1.0f, this.mCurrentUserId);
        if (this.mOminLevelModeEnable) {
            this.mOminLevelCountSaved = System.getIntForUser(this.mContentResolver, "spline_ominlevel_count", 0, this.mCurrentUserId);
            this.mOminLevelCount = this.mOminLevelCountSaved;
            this.mOminLevelCountResetLongSetTimeSaved = System.getIntForUser(this.mContentResolver, "spline_ominlevel_time", 0, this.mCurrentUserId);
            this.mOminLevelCountResetLongSetTime = this.mOminLevelCountResetLongSetTimeSaved;
            Slog.i(TAG, "mOminLevelMode read mOminLevelCount=" + this.mOminLevelCount + ",mOminLevelCountResetLongSetTime=" + this.mOminLevelCountResetLongSetTime);
        }
        if (this.mManualMode && this.mStartLuxDefaultBrightness >= ((float) this.mManualBrightnessMaxLimit) && this.mPosBrightness == ((float) this.mManualBrightnessMaxLimit)) {
            this.mDelta = 0.0f;
            this.mDeltaNew = 0.0f;
            Slog.i(TAG, "updateLevel outdoor no offset set mDelta=0");
        }
        if (DEBUG) {
            Slog.d(TAG, "Read:userId=" + this.mCurrentUserId + ",mPosBrightness=" + this.mPosBrightness + ",mOffsetBrightness_last=" + this.mOffsetBrightness_last + ",mIsUserChange=" + this.mIsUserChange + ",mDeltaNew=" + this.mDeltaNew + ",mDelta=" + this.mDelta + ",mStartLuxDefaultBrightness=" + this.mStartLuxDefaultBrightness + ",mLastLuxDefaultBrightness=" + this.mLastLuxDefaultBrightness + ",mAmLuxOffset=" + this.mAmLuxOffset);
        }
    }

    private void saveOffsetParas() {
        if (((int) (this.mPosBrightness * 10.0f)) != ((int) (this.mPosBrightnessSaved * 10.0f))) {
            System.putFloatForUser(this.mContentResolver, "hw_screen_auto_brightness_adj", this.mPosBrightness / maxBrightness, this.mCurrentUserId);
            this.mPosBrightnessSaved = this.mPosBrightness;
        }
        if (((int) (this.mDeltaNew * 10.0f)) != ((int) (this.mDeltaSaved * 10.0f))) {
            System.putFloatForUser(this.mContentResolver, "spline_delta", this.mDeltaNew, this.mCurrentUserId);
            this.mDeltaSaved = this.mDeltaNew;
        }
        if (this.mIsUserChange != this.mIsUserChangeSaved) {
            System.putIntForUser(this.mContentResolver, "spline_is_user_change", this.mIsUserChange ? 1 : 0, this.mCurrentUserId);
            this.mIsUserChangeSaved = this.mIsUserChange;
        }
        if (((int) (this.mOffsetBrightness_last * 10.0f)) != ((int) (this.mOffsetBrightness_lastSaved * 10.0f))) {
            System.putFloatForUser(this.mContentResolver, "spline_offset_brightness_last", this.mOffsetBrightness_last, this.mCurrentUserId);
            this.mOffsetBrightness_lastSaved = this.mOffsetBrightness_last;
        }
        if (((int) (this.mLastLuxDefaultBrightness * 10.0f)) != ((int) (this.mLastLuxDefaultBrightnessSaved * 10.0f))) {
            System.putFloatForUser(this.mContentResolver, "spline_last_lux_default_brightness", this.mLastLuxDefaultBrightness, this.mCurrentUserId);
            this.mLastLuxDefaultBrightnessSaved = this.mLastLuxDefaultBrightness;
        }
        if (((int) (this.mStartLuxDefaultBrightness * 10.0f)) != ((int) (this.mStartLuxDefaultBrightnessSaved * 10.0f))) {
            System.putFloatForUser(this.mContentResolver, "spline_start_lux_default_brightness", this.mStartLuxDefaultBrightness, this.mCurrentUserId);
            this.mStartLuxDefaultBrightnessSaved = this.mStartLuxDefaultBrightness;
        }
        if (((int) (this.mAmLux * 10.0f)) != ((int) (this.mAmLuxSaved * 10.0f))) {
            System.putFloatForUser(this.mContentResolver, "spline_ambient_lux", this.mAmLux, this.mCurrentUserId);
            this.mAmLuxSaved = this.mAmLux;
        }
        if (((int) (this.mAmLuxOffset * 10.0f)) != ((int) (this.mAmLuxOffsetSaved * 10.0f))) {
            System.putFloatForUser(this.mContentResolver, "spline_ambient_lux_offset", this.mAmLuxOffset, this.mCurrentUserId);
            this.mAmLuxOffsetSaved = this.mAmLuxOffset;
        }
        if (this.mOminLevelCount != this.mOminLevelCountSaved) {
            System.putIntForUser(this.mContentResolver, "spline_ominlevel_count", this.mOminLevelCount, this.mCurrentUserId);
            this.mOminLevelCountSaved = this.mOminLevelCount;
            Slog.i(TAG, "mOminLevelMode saved mOminLevelCount=" + this.mOminLevelCount);
        }
        if (this.mOminLevelCountResetLongSetTime != this.mOminLevelCountResetLongSetTimeSaved) {
            System.putIntForUser(this.mContentResolver, "spline_ominlevel_time", this.mOminLevelCountResetLongSetTime, this.mCurrentUserId);
            this.mOminLevelCountResetLongSetTimeSaved = this.mOminLevelCountResetLongSetTime;
            Slog.i(TAG, "mOminLevelMode saved mOminLevelCountResetLongSetTime=" + this.mOminLevelCountResetLongSetTime);
        }
        if (DEBUG) {
            Slog.d(TAG, "write:userId=" + this.mCurrentUserId + ",mPosBrightness =" + this.mPosBrightness + ",mOffsetBrightness_last=" + this.mOffsetBrightness_last + ",mIsUserChange=" + this.mIsUserChange + ",mDeltaNew=" + this.mDeltaNew + ",mStartLuxDefaultBrightness=" + this.mStartLuxDefaultBrightness + "mLastLuxDefaultBrightness=" + this.mLastLuxDefaultBrightness + ",mAmLux=" + this.mAmLux + ",mAmLuxOffset=" + this.mAmLuxOffset);
        }
    }

    public static HwNormalizedSpline createHwNormalizedSpline(Context context, int deviceActualBrightnessLevel, int deviceActualBrightnessNit, int deviceStandardBrightnessNit) {
        return new HwNormalizedSpline(context, deviceActualBrightnessLevel, deviceActualBrightnessNit, deviceStandardBrightnessNit);
    }

    public String toString() {
        return new StringBuilder().toString();
    }

    public float interpolate(float x) {
        this.mAmLux = x;
        if (this.mPosBrightness == 0.0f) {
            this.mIsReboot = true;
        } else {
            this.mIsReboot = false;
        }
        if (DEBUG) {
            Slog.d(TAG, "interpolate:mPosBrightness=" + this.mPosBrightness + "lux=" + x + ",mIsReboot=" + this.mIsReboot + ",mIsUserChange=" + this.mIsUserChange + ",mDelta=" + this.mDelta);
        }
        float value_interp = getInterpolatedValue(this.mPosBrightness, x) / maxBrightness;
        saveOffsetParas();
        return value_interp;
    }

    public void updateLevelWithLux(float PosBrightness, float lux) {
        if (lux < 0.0f) {
            Slog.e(TAG, "error input lux,lux=" + lux);
            return;
        }
        if (!this.mIsReboot) {
            this.mIsUserChange = true;
        }
        this.mAmLuxOffset = lux;
        if (this.mOminLevelCountEnable && this.mOminLevelModeEnable) {
            if ((this.mDayModeAlgoEnable ? this.mDayModeEnable : false) || (this.mOminLevelDayModeEnable ^ 1) == 0) {
                this.mStartLuxDefaultBrightness = getDefaultBrightnessLevelNew(this.mOminLevelBrighnessLinePointsList, lux);
                if (DEBUG) {
                    Slog.d(TAG, "updateLevel mOminLevelMode:mDefaultBrightness=" + this.mDefaultBrightnessFromLux);
                }
            } else {
                this.mStartLuxDefaultBrightness = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, lux);
            }
        } else if (this.mDayModeAlgoEnable && this.mDayModeEnable) {
            this.mStartLuxDefaultBrightness = getDefaultBrightnessLevelNew(this.mDayBrighnessLinePointsList, lux);
            if (DEBUG) {
                Slog.d(TAG, "updateLevel DayMode: mDefaultBrightnessFromLux =" + this.mDefaultBrightnessFromLux);
            }
        } else {
            this.mStartLuxDefaultBrightness = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, lux);
        }
        this.mPosBrightness = PosBrightness;
        if (this.mManualMode && this.mStartLuxDefaultBrightness >= ((float) this.mManualBrightnessMaxLimit) && this.mPosBrightness == ((float) this.mManualBrightnessMaxLimit)) {
            this.mAmLuxOffset = -1.0f;
            this.mDelta = 0.0f;
            this.mDeltaNew = 0.0f;
            if (DEBUG) {
                Slog.i(TAG, "updateLevel outdoor no offset mDelta=0");
            }
        } else {
            this.mDelta = this.mPosBrightness - this.mStartLuxDefaultBrightness;
            this.mDeltaNew = this.mPosBrightness - this.mStartLuxDefaultBrightness;
        }
        if (this.mPosBrightness == 0.0f) {
            this.mAmLuxOffset = -1.0f;
            this.mDelta = 0.0f;
            this.mDeltaNew = 0.0f;
            this.mOffsetBrightness_last = 0.0f;
            this.mLastLuxDefaultBrightness = 0.0f;
            this.mStartLuxDefaultBrightness = 0.0f;
        }
        if (this.mOminLevelModeEnable) {
            updateOminLevelCount(lux);
        } else {
            this.mOminLevelCountResetLongSetTime = 0;
            this.mOminLevelCount = 0;
        }
        if (DEBUG) {
            Slog.d(TAG, "updateLevel:mDelta=" + this.mDelta + ",mDeltaNew=" + this.mDeltaNew + ",mPosBrightness=" + this.mPosBrightness + ",mStartLuxDefaultBrightness=" + this.mStartLuxDefaultBrightness + ",lux=" + lux);
        }
        saveOffsetParas();
    }

    public void updateOminLevelCount(float lux) {
        int currentMinuteTime = (int) (System.currentTimeMillis() / 60000);
        int deltaMinuteTime = currentMinuteTime - this.mOminLevelCountResetLongSetTime;
        if (deltaMinuteTime >= this.mOminLevelCountResetLongTimeTh || deltaMinuteTime < 0) {
            this.mOminLevelCount = resetOminLevelCount(this.mOminLevelCountLevelPointsList, (float) this.mOminLevelCount);
            this.mOminLevelCountResetLongSetTime = currentMinuteTime;
            if (DEBUG) {
                Slog.d(TAG, "mOminLevelMode reset mOminLevelCount=" + this.mOminLevelCount + ",deltaMinuteTime=" + deltaMinuteTime + ",currenTime=" + currentMinuteTime);
            }
        }
        Object obj = (lux < 0.0f || lux > ((float) this.mOminLevelCountValidLuxTh)) ? null : 1;
        if (obj != null) {
            long currentTime = SystemClock.uptimeMillis();
            float mBrightenDefaultBrightness = getDefaultBrightnessLevelNew(this.mOminLevelBrighnessLinePointsList, lux);
            long deltaTime = currentTime - this.mOminLevelCountSetTime;
            if (deltaTime / 1000 >= ((long) this.mOminLevelCountValidTimeTh)) {
                if (DEBUG) {
                    Slog.d(TAG, "mOminLevelMode deltaTime=" + deltaTime + ",ValidTime");
                }
                if (Math.abs(this.mPosBrightness) < 1.0E-7f) {
                    if (this.mOminLevelCount > 0 && this.mOminLevelOffsetCountEnable) {
                        this.mOminLevelCount--;
                        this.mOminLevelValidCount = 0;
                        this.mOminLevelCountSetTime = currentTime;
                        Slog.i(TAG, "mOminLevelMode resetoffset-- count=" + this.mOminLevelCount);
                    }
                } else if (this.mPosBrightness - mBrightenDefaultBrightness > 0.0f) {
                    if (this.mOminLevelCount < getOminLevelCountThMax(this.mOminLevelCountLevelPointsList)) {
                        this.mOminLevelCount++;
                        this.mOminLevelValidCount = 1;
                        this.mOminLevelCountSetTime = currentTime;
                        Slog.i(TAG, "mOminLevelMode brighten++ count=" + this.mOminLevelCount);
                    }
                } else if (this.mPosBrightness - mBrightenDefaultBrightness < 0.0f && this.mOminLevelCount > 0) {
                    this.mOminLevelCount--;
                    this.mOminLevelValidCount = -1;
                    this.mOminLevelCountSetTime = currentTime;
                    Slog.i(TAG, "mOminLevelMode darken-- count=" + this.mOminLevelCount);
                }
            } else {
                if (DEBUG) {
                    Slog.d(TAG, "mOminLevelMode deltaTime=" + deltaTime);
                }
                if (Math.abs(this.mPosBrightness) < 1.0E-7f) {
                    if (this.mOminLevelCount > 0 && this.mOminLevelValidCount >= 0 && this.mOminLevelOffsetCountEnable) {
                        this.mOminLevelCount--;
                        this.mOminLevelValidCount--;
                        this.mOminLevelCountSetTime = currentTime;
                        Slog.i(TAG, "mOminLevelMode resetoffset-- count=" + this.mOminLevelCount + ",ValidCount=" + this.mOminLevelValidCount);
                    }
                } else if (this.mPosBrightness - mBrightenDefaultBrightness > 0.0f) {
                    if (this.mOminLevelCount < getOminLevelCountThMax(this.mOminLevelCountLevelPointsList) && this.mOminLevelValidCount <= 0) {
                        this.mOminLevelCount++;
                        this.mOminLevelValidCount++;
                        this.mOminLevelCountSetTime = currentTime;
                        Slog.i(TAG, "mOminLevelMode brighten++ count=" + this.mOminLevelCount + ",ValidCount=" + this.mOminLevelValidCount);
                    }
                } else if (this.mPosBrightness - mBrightenDefaultBrightness < 0.0f && this.mOminLevelCount > 0 && this.mOminLevelValidCount >= 0) {
                    this.mOminLevelCount--;
                    this.mOminLevelValidCount--;
                    this.mOminLevelCountSetTime = currentTime;
                    Slog.i(TAG, "mOminLevelMode darken-- count=" + this.mOminLevelCount + ",ValidCount=" + this.mOminLevelValidCount);
                }
            }
            updateOminLevelBrighnessLinePoints();
        }
    }

    private int resetOminLevelCount(List<Point> linePointsList, float levelCount) {
        List<Point> linePointsListIn = linePointsList;
        int ominLevelCount = 0;
        if (linePointsList == null) {
            Slog.e(TAG, "mOminLevelMode linePointsList input error!");
            return 0;
        } else if (levelCount <= ((float) getOminLevelCountThMin(linePointsList))) {
            return 0;
        } else {
            if (levelCount >= ((float) getOminLevelCountThMax(linePointsList))) {
                return getOminLevelCountThMax(linePointsList);
            }
            Point temp1 = null;
            for (Point temp : linePointsList) {
                if (temp1 == null) {
                    temp1 = temp;
                }
                if (levelCount < temp.x) {
                    ominLevelCount = (int) temp1.x;
                } else {
                    temp1 = temp;
                }
            }
            return ominLevelCount;
        }
    }

    private int getOminLevelCountThMin(List<Point> linePointsList) {
        List<Point> linePointsListIn = linePointsList;
        if (linePointsList.size() > 0) {
            return (int) ((Point) linePointsList.get(0)).x;
        }
        return 0;
    }

    private int getOminLevelCountThMax(List<Point> linePointsList) {
        List<Point> linePointsListIn = linePointsList;
        int countMax = 0;
        if (linePointsList == null) {
            return 0;
        }
        int listSize = linePointsList.size();
        if (listSize > 0) {
            countMax = (int) ((Point) linePointsList.get(listSize - 1)).x;
        }
        return countMax;
    }

    private float getOminLevelThMin(List<Point> linePointsList) {
        List<Point> linePointsListIn = linePointsList;
        if (linePointsList.size() > 0) {
            return ((Point) linePointsList.get(0)).y;
        }
        return minBrightness;
    }

    private float getOminLevelThMax(List<Point> linePointsList) {
        List<Point> linePointsListIn = linePointsList;
        float levelMax = minBrightness;
        if (linePointsList == null) {
            return minBrightness;
        }
        int listSize = linePointsList.size();
        if (listSize > 0) {
            levelMax = ((Point) linePointsList.get(listSize - 1)).y;
        }
        return levelMax;
    }

    public float getInterpolatedValue(float PositionBrightness, float lux) {
        float PosBrightness = PositionBrightness;
        if (this.mPowerSavingModeEnable && this.mPowerSavingBrighnessLineEnable && this.mAmLux > this.mPowerSavingAmluxThreshold) {
            this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mPowerSavingBrighnessLinePointsList, lux);
            Slog.i(TAG, "PowerSavingMode defualtbrightness=" + this.mDefaultBrightnessFromLux + ",lux=" + lux + ",mCalibrationRatio=" + this.mCalibrationRatio);
        } else if (this.mCameraModeEnable) {
            this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mCameraBrighnessLinePointsList, lux);
            Slog.i(TAG, "CameraMode defualtbrightness=" + this.mDefaultBrightnessFromLux + ",lux=" + lux);
        } else if (this.mEyeProtectionSpline != null && this.mEyeProtectionSpline.isEyeProtectionMode() && this.mEyeProtectionSplineEnable) {
            this.mDefaultBrightnessFromLux = this.mEyeProtectionSpline.getEyeProtectionBrightnessLevel(lux);
            Slog.i(TAG, "getEyeProtectionBrightnessLevel lux =" + lux + ", mDefaultBrightnessFromLux =" + this.mDefaultBrightnessFromLux);
        } else {
            if (this.mCalibrtionModeBeforeEnable) {
                this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsListCaliBefore, lux);
            } else if (this.mOminLevelCountEnable && this.mOminLevelModeEnable) {
                boolean z;
                if (this.mDayModeAlgoEnable) {
                    z = this.mDayModeEnable;
                } else {
                    z = false;
                }
                if (z || (this.mOminLevelDayModeEnable ^ 1) == 0) {
                    this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mOminLevelBrighnessLinePointsList, lux);
                    Slog.i(TAG, "mOminLevelMode:Day getBrightnessLevel lux =" + lux + ",mDefaultBrightness=" + this.mDefaultBrightnessFromLux + ",mOCount=" + this.mOminLevelCount);
                } else {
                    this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, lux);
                    Slog.i(TAG, "mOminLevelMode:night getBrightnessLevel lux =" + lux + ",mDefaultBrightness=" + this.mDefaultBrightnessFromLux + ",mOCount=" + this.mOminLevelCount);
                }
            } else if (this.mDayModeAlgoEnable && this.mDayModeEnable) {
                this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mDayBrighnessLinePointsList, lux);
                Slog.i(TAG, "DayMode:getBrightnessLevel lux =" + lux + ", mDefaultBrightnessFromLux =" + this.mDefaultBrightnessFromLux);
            } else {
                this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, lux);
            }
            if (DEBUG && this.mEyeProtectionSpline != null && this.mEyeProtectionSpline.isEyeProtectionMode() && (this.mEyeProtectionSplineEnable ^ 1) != 0) {
                Slog.i(TAG, "getEyeProtectionBrightnessLevel");
            }
        }
        if (this.mIsReboot) {
            this.mLastLuxDefaultBrightness = this.mDefaultBrightnessFromLux;
            this.mStartLuxDefaultBrightness = this.mDefaultBrightnessFromLux;
            this.mOffsetBrightness_last = this.mDefaultBrightnessFromLux;
            this.mIsReboot = false;
            this.mIsUserChange = false;
        }
        if (this.mLastLuxDefaultBrightness <= 0.0f && this.mPosBrightness != 0.0f) {
            this.mPosBrightness = 0.0f;
            PosBrightness = 0.0f;
            this.mDelta = 0.0f;
            this.mOffsetBrightness_last = 0.0f;
            this.mLastLuxDefaultBrightness = 0.0f;
            this.mStartLuxDefaultBrightness = 0.0f;
            this.mIsUserChange = false;
            saveOffsetParas();
            if (DEBUG) {
                Slog.d(TAG, "error state for default state");
            }
        }
        float offsetBrightness = this.mDefaultBrightnessFromLux;
        if (PosBrightness == 0.0f || this.mCoverModeNoOffsetEnable) {
            offsetBrightness = this.mDefaultBrightnessFromLux;
            if (this.mCoverModeNoOffsetEnable) {
                this.mCoverModeNoOffsetEnable = false;
                if (DEBUG) {
                    Slog.i(TAG, "set mCoverModeNoOffsetEnable=" + this.mCoverModeNoOffsetEnable);
                }
            }
        } else {
            offsetBrightness = getOffsetBrightnessLevel_new(this.mStartLuxDefaultBrightness, this.mDefaultBrightnessFromLux, PosBrightness);
        }
        if (DEBUG && ((int) offsetBrightness) != ((int) this.mOffsetBrightness_last)) {
            Slog.d(TAG, "offsetBrightness=" + offsetBrightness + ",mOffsetBrightness_last" + this.mOffsetBrightness_last + ",lux=" + lux + ",mPosBrightness=" + this.mPosBrightness + ",mIsUserChange=" + this.mIsUserChange + ",mDelta=" + this.mDelta + ",mDefaultBrightnessFromLux=" + this.mDefaultBrightnessFromLux + ",mStartLuxDefaultBrightness=" + this.mStartLuxDefaultBrightness + "mLastLuxDefaultBrightness=" + this.mLastLuxDefaultBrightness);
        }
        this.mLastLuxDefaultBrightness = this.mDefaultBrightnessFromLux;
        this.mOffsetBrightness_last = offsetBrightness;
        return offsetBrightness;
    }

    public float getDefaultBrightnessLevelNew(List<Point> linePointsList, float lux) {
        List<Point> linePointsListIn = linePointsList;
        int count = 0;
        float brightnessLevel = this.mDefaultBrightness;
        Point temp1 = null;
        for (Point temp : linePointsList) {
            if (count == 0) {
                temp1 = temp;
            }
            if (lux < temp.x) {
                Point temp2 = temp;
                if (temp.x > temp1.x) {
                    return (((temp.y - temp1.y) / (temp.x - temp1.x)) * (lux - temp1.x)) + temp1.y;
                }
                brightnessLevel = this.mDefaultBrightness;
                if (!DEBUG) {
                    return brightnessLevel;
                }
                Slog.i(TAG, "DefaultBrighness_temp1.x <= temp2.x,x" + temp.x + ", y = " + temp.y);
                return brightnessLevel;
            }
            temp1 = temp;
            brightnessLevel = temp.y;
            count++;
        }
        return brightnessLevel;
    }

    float getOffsetBrightnessLevel_new(float brightnessStartOrig, float brightnessEndOrig, float brightnessStartNew) {
        if (this.mIsUserChange) {
            this.mIsUserChange = false;
        }
        float ratio = 1.0f;
        float ratio2 = 1.0f;
        if (brightnessStartOrig < brightnessEndOrig) {
            if (this.mDelta > 0.0f) {
                ratio2 = (((-this.mDelta) * (Math.abs(brightnessStartOrig - brightnessEndOrig) / (this.mDelta + 1.0E-7f))) / ((maxBrightness - brightnessStartOrig) + 1.0E-7f)) + 1.0f;
                if (DEBUG) {
                    Slog.i(TAG, "Orig_ratio2=" + ratio2 + ",mOffsetBrightenAlphaRight=" + this.mOffsetBrightenAlphaRight);
                }
                ratio2 = ((((1.0f - this.mOffsetBrightenAlphaRight) * Math.max(brightnessEndOrig, brightnessStartNew)) + (this.mOffsetBrightenAlphaRight * ((this.mDelta * ratio2) + brightnessEndOrig))) - brightnessEndOrig) / (this.mDelta + 1.0E-7f);
                if (ratio2 < 0.0f) {
                    ratio2 = 0.0f;
                }
            }
            if (this.mDelta < 0.0f) {
                ratio = (((-this.mDelta) * (Math.abs(brightnessStartOrig - brightnessEndOrig) / (this.mDelta - 1.0E-7f))) / ((maxBrightness - brightnessStartOrig) + 1.0E-7f)) + 1.0f;
                if (ratio < 0.0f) {
                    ratio = 0.0f;
                }
            }
        }
        if (brightnessStartOrig > brightnessEndOrig) {
            if (this.mDelta < 0.0f) {
                ratio2 = ((((1.0f - this.mOffsetDarkenAlphaLeft) * Math.min(brightnessEndOrig, brightnessStartNew)) + (this.mOffsetDarkenAlphaLeft * ((this.mDelta * (((this.mDelta * (Math.abs(brightnessStartOrig - brightnessEndOrig) / (this.mDelta - 1.0E-7f))) / ((minBrightness - brightnessStartOrig) - 1.0E-7f)) + 1.0f)) + brightnessEndOrig))) - brightnessEndOrig) / (this.mDelta - 1.0E-7f);
                if (ratio2 < 0.0f) {
                    ratio2 = 0.0f;
                }
            }
            if (this.mDelta > 0.0f) {
                float ratioTmp = (float) Math.pow((double) (brightnessEndOrig / (1.0E-7f + brightnessStartOrig)), (double) this.mOffsetBrightenRatioLeft);
                ratio = ((this.mOffsetBrightenAlphaLeft * brightnessEndOrig) / (1.0E-7f + brightnessStartOrig)) + ((1.0f - this.mOffsetBrightenAlphaLeft) * ratioTmp);
                if (DEBUG) {
                    Slog.d(TAG, "ratio=" + ratio + ",ratioTmp=" + ratioTmp + ",mOffsetBrightenAlphaLeft=" + this.mOffsetBrightenAlphaLeft);
                }
            }
        }
        this.mDeltaNew = (this.mDelta * ratio2) * ratio;
        if (DEBUG) {
            Slog.d(TAG, "mDeltaNew=" + this.mDeltaNew + ",mDelta=" + this.mDelta + ",ratio2=" + ratio2 + ",ratio=" + ratio);
        }
        float brightnessAndDelta = brightnessEndOrig + this.mDeltaNew;
        float offsetBrightnessTemp = brightnessAndDelta > minBrightness ? brightnessAndDelta : minBrightness;
        return offsetBrightnessTemp < maxBrightness ? offsetBrightnessTemp : maxBrightness;
    }

    public float getAmbientValueFromDB() {
        float ambientValue = System.getFloatForUser(this.mContentResolver, "spline_ambient_lux", 100.0f, this.mCurrentUserId);
        if (((int) ambientValue) < 0) {
            Slog.e(TAG, "error inputValue<min,ambientValue=" + ambientValue);
            ambientValue = 0.0f;
        }
        if (((int) ambientValue) <= 40000) {
            return ambientValue;
        }
        Slog.e(TAG, "error inputValue>max,ambientValue=" + ambientValue);
        return 40000.0f;
    }

    public boolean getCalibrationTestEable() {
        int calibrtionTest = System.getIntForUser(this.mContentResolver, "spline_calibration_test", 0, this.mCurrentUserId);
        if (calibrtionTest == 0) {
            this.mCalibrtionModeBeforeEnable = false;
            return false;
        }
        int calibrtionTestLow = calibrtionTest & 65535;
        int calibrtionTestHigh = (calibrtionTest >> 16) & 65535;
        if (calibrtionTestLow != calibrtionTestHigh) {
            Slog.e(TAG, "error db, clear DB,,calibrtionTestLow=" + calibrtionTestLow + ",calibrtionTestHigh=" + calibrtionTestHigh);
            System.putIntForUser(this.mContentResolver, "spline_calibration_test", 0, this.mCurrentUserId);
            this.mCalibrtionModeBeforeEnable = false;
            return false;
        }
        boolean calibrationTestEable;
        int calibrtionModeBeforeEnableInt = (calibrtionTestLow >> 1) & 1;
        int calibrationTestEnableInt = calibrtionTestLow & 1;
        if (calibrtionModeBeforeEnableInt == 1) {
            this.mCalibrtionModeBeforeEnable = true;
        } else {
            this.mCalibrtionModeBeforeEnable = false;
        }
        if (calibrationTestEnableInt == 1) {
            calibrationTestEable = true;
        } else {
            calibrationTestEable = false;
        }
        if (calibrtionTest != this.mCalibrtionTest) {
            this.mCalibrtionTest = calibrtionTest;
            if (DEBUG) {
                Slog.d(TAG, "mCalibrtionTest=" + this.mCalibrtionTest + ",calibrationTestEnableInt=" + calibrationTestEnableInt + ",calibrtionModeBeforeEnableInt=" + calibrtionModeBeforeEnableInt);
            }
        }
        return calibrationTestEable;
    }

    public void setEyeProtectionControlFlag(boolean inControlTime) {
        if (this.mEyeProtectionSpline != null) {
            this.mEyeProtectionSpline.setEyeProtectionControlFlag(inControlTime);
        }
    }

    public void setNoOffsetEnable(boolean noOffsetEnable) {
        this.mCoverModeNoOffsetEnable = noOffsetEnable;
        if (DEBUG) {
            Slog.i(TAG, "LabcCoverMode CoverModeNoOffsetEnable=" + this.mCoverModeNoOffsetEnable);
        }
    }

    public void setCameraModeEnable(boolean cameraModeEnable) {
        this.mCameraModeEnable = cameraModeEnable;
    }

    public void setPowerSavingModeEnable(boolean powerSavingModeEnable) {
        this.mPowerSavingModeEnable = powerSavingModeEnable;
    }

    public float getCurrentDefaultBrightnessNoOffset() {
        return this.mDefaultBrightnessFromLux;
    }

    public float getCurrentAmbientLuxForBrightness() {
        return this.mAmLux;
    }

    public float getCurrentAmbientLuxForOffset() {
        return this.mAmLuxOffset;
    }

    public void setDayModeEnable(boolean dayModeEnable) {
        this.mDayModeEnable = dayModeEnable;
    }

    public void reSetOffsetFromHumanFactor(boolean offsetResetEnable, int minOffsetBrightness, int maxOffsetBrightness) {
        if (offsetResetEnable && Math.abs(this.mPosBrightness) > 1.0E-7f) {
            if (Math.abs(this.mCalibrationRatio - 1.0f) > 1.0E-7f) {
                if (((float) minOffsetBrightness) > minBrightness && ((float) minOffsetBrightness) * this.mCalibrationRatio > minBrightness) {
                    minOffsetBrightness = (int) (((float) minOffsetBrightness) * this.mCalibrationRatio);
                }
                if (((float) maxOffsetBrightness) < maxBrightness && ((float) maxOffsetBrightness) * this.mCalibrationRatio < maxBrightness) {
                    maxOffsetBrightness = (int) (((float) maxOffsetBrightness) * this.mCalibrationRatio);
                }
            }
            if (this.mPosBrightness < ((float) minOffsetBrightness)) {
                this.mPosBrightness = (float) minOffsetBrightness;
                this.mOffsetBrightness_last = (float) minOffsetBrightness;
                this.mDelta = this.mPosBrightness - this.mStartLuxDefaultBrightness;
                if (DEBUG) {
                    Slog.d(TAG, "updateLevel:resetMin mPosBrightness=" + this.mPosBrightness + ",min=" + minOffsetBrightness + ",max=" + maxOffsetBrightness + ",mDelta=" + this.mDelta + ",mAmLuxOffset=" + this.mAmLuxOffset + ",mCalibrationRatio=" + this.mCalibrationRatio);
                }
            }
            if (this.mPosBrightness > ((float) maxOffsetBrightness)) {
                this.mPosBrightness = (float) maxOffsetBrightness;
                this.mOffsetBrightness_last = (float) maxOffsetBrightness;
                this.mDelta = this.mPosBrightness - this.mStartLuxDefaultBrightness;
                if (DEBUG) {
                    Slog.d(TAG, "updateLevel:resetMax mPosBrightness=" + this.mPosBrightness + ",min=" + minOffsetBrightness + ",max=" + maxOffsetBrightness + ",mDelta=" + this.mDelta + ",mAmLuxOffset=" + this.mAmLuxOffset + ",mCalibrationRatio=" + this.mCalibrationRatio);
                }
            }
        }
    }
}
