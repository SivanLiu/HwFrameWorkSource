package android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.System;
import com.huawei.displayengine.DisplayEngineDBManager.AlgorithmESCWKey;
import com.huawei.displayengine.DisplayEngineDBManager.BrightnessCurveKey;
import com.huawei.displayengine.DisplayEngineManager;
import com.huawei.displayengine.IDisplayEngineServiceEx;
import com.huawei.displayengine.IDisplayEngineServiceEx.Stub;
import huawei.android.utils.HwEyeProtectionSpline;
import huawei.android.utils.HwEyeProtectionSplineImpl;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class HwNormalizedSpline extends Spline {
    private static final float BRIGHTNESS_WITHDELTA_MAX = 230.0f;
    private static boolean DEBUG = false;
    private static final boolean HWDEBUG;
    private static final String HW_LABC_CONFIG_FILE = "LABCConfig.xml";
    private static final String LCD_PANEL_TYPE_PATH = "/sys/class/graphics/fb0/lcd_model";
    private static final String TAG = "HwNormalizedSpline";
    private static final String TOUCH_OEM_INFO_PATH = "/sys/touchscreen/touch_oem_info";
    private static final String XML_EXT = ".xml";
    private static final String XML_NAME_NOEXT = "LABCConfig";
    private static final Object mLock = new Object();
    private static final float maxBrightness = 255.0f;
    private static final float minBrightness = 4.0f;
    private float mAmLux;
    private float mAmLuxOffset;
    private float mAmLuxOffsetSaved;
    private float mAmLuxSaved;
    private boolean mBrightnessCalibrationEnabled;
    List<Point> mBrightnessCurveDefault;
    List<Point> mBrightnessCurveDefaultTmp;
    List<Point> mBrightnessCurveHigh;
    List<Point> mBrightnessCurveHighTmp;
    List<Point> mBrightnessCurveLow;
    List<Point> mBrightnessCurveLowTmp;
    List<Point> mBrightnessCurveMiddle;
    List<Point> mBrightnessCurveMiddleTmp;
    private float mBrightnessForLog;
    private float mCalibrationRatio;
    private boolean mCalibrtionModeBeforeEnable;
    private int mCalibrtionTest;
    List<Point> mCameraBrighnessLinePointsList;
    private boolean mCameraModeEnable;
    private ContentResolver mContentResolver;
    private boolean mCoverModeNoOffsetEnable;
    private int mCurrentCurveLevel;
    private int mCurrentUserId;
    private int mCurveLevel;
    private boolean mDarkAdaptEnable;
    private boolean mDarkAdaptLineLocked;
    private DarkAdaptState mDarkAdaptState;
    private DarkAdaptState mDarkAdaptStateDetected;
    private int mDarkAdaptedBrightness0LuxLevel;
    private List<Point> mDarkAdaptedBrightnessPointsList;
    private int mDarkAdaptingBrightness0LuxLevel;
    private List<Point> mDarkAdaptingBrightnessPointsList;
    List<Point> mDayBrighnessLinePointsList;
    private boolean mDayModeAlgoEnable;
    private boolean mDayModeEnable;
    private float mDayModeMinimumBrightness;
    private int mDayModeModifyMinBrightness;
    private int mDayModeModifyNumPoint;
    List<Point> mDefaultBrighnessLinePointsList;
    List<Point> mDefaultBrighnessLinePointsListCaliBefore;
    private float mDefaultBrightness;
    private float mDefaultBrightnessFromLux;
    private float mDelta;
    private float mDeltaNew;
    private float mDeltaSaved;
    private float mDeltaTmp;
    private final int mDeviceActualBrightnessLevel;
    private int mDeviceActualBrightnessNit;
    private int mDeviceStandardBrightnessNit;
    private HwEyeProtectionSpline mEyeProtectionSpline;
    private boolean mEyeProtectionSplineEnable;
    private boolean mGameModeBrightnessEnable;
    List<Point> mGameModeBrightnessLinePointsList;
    private boolean mGameModeEnable;
    private float mGameModeOffsetLux;
    private float mGameModePosBrightness;
    private float mGameModeStartLuxDefaultBrightness;
    private boolean mIsReboot;
    private volatile boolean mIsReset;
    private boolean mIsUserChange;
    private boolean mIsUserChangeSaved;
    private float mLastLuxDefaultBrightness;
    private float mLastLuxDefaultBrightnessSaved;
    private float[] mLuxPonits;
    private DisplayEngineManager mManager;
    private int mManualBrightnessMaxLimit;
    private boolean mManualMode;
    private boolean mNewCurveEnable;
    private boolean mNewCurveEnableTmp;
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
    private boolean mPersonalizedBrightnessCurveEnable;
    private float mPosBrightness;
    private float mPosBrightnessSaved;
    private boolean mPowerOnEanble;
    private float mPowerSavingAmluxThreshold;
    private boolean mPowerSavingBrighnessLineEnable;
    List<Point> mPowerSavingBrighnessLinePointsList;
    private boolean mPowerSavingModeEnable;
    List<Point> mReadingBrighnessLinePointsList;
    private boolean mReadingModeEnable;
    private boolean mRebootNewCurveEnable;
    private int mSceneLevel;
    private float mStartLuxDefaultBrightness;
    private float mStartLuxDefaultBrightnessSaved;
    private boolean mUsePowerSavingModeCurveEnable;
    private float mVehicleModeBrighntess;
    private boolean mVehicleModeBrightnessEnable;
    private boolean mVehicleModeClearOffsetEnable;
    private boolean mVehicleModeEnable;
    private float mVehicleModeLuxThreshold;
    public boolean mVehicleModeQuitForPowerOnEnable;

    public enum BrightnessModeState {
        CameraMode,
        ReadingMode,
        GameMode,
        NewCurveMode,
        PowerSavingMode,
        EyeProtectionMode,
        CalibrtionMode,
        DarkAdaptMode,
        OminLevelMode,
        DayMode,
        DefaultMode
    }

    public enum DarkAdaptState {
        UNADAPTED,
        ADAPTING,
        ADAPTED
    }

    private static class Point {
        float x;
        float y;

        public Point(float inx, float iny) {
            this.x = inx;
            this.y = iny;
        }
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        DEBUG = z;
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
        this.mCurrentUserId = 0;
        this.mAmLux = -1.0f;
        this.mCoverModeNoOffsetEnable = false;
        this.mReadingModeEnable = false;
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
        this.mIsReset = false;
        this.mDayModeMinimumBrightness = minBrightness;
        this.mDefaultBrighnessLinePointsList = null;
        this.mDefaultBrighnessLinePointsListCaliBefore = null;
        this.mEyeProtectionSpline = null;
        this.mCameraBrighnessLinePointsList = null;
        this.mReadingBrighnessLinePointsList = null;
        this.mDayBrighnessLinePointsList = null;
        this.mPowerSavingBrighnessLinePointsList = null;
        this.mBrightnessCurveDefault = new ArrayList();
        this.mBrightnessCurveLow = new ArrayList();
        this.mBrightnessCurveMiddle = new ArrayList();
        this.mBrightnessCurveHigh = new ArrayList();
        this.mBrightnessCurveDefaultTmp = new ArrayList();
        this.mBrightnessCurveLowTmp = new ArrayList();
        this.mBrightnessCurveMiddleTmp = new ArrayList();
        this.mBrightnessCurveHighTmp = new ArrayList();
        this.mRebootNewCurveEnable = true;
        this.mNewCurveEnable = false;
        this.mNewCurveEnableTmp = false;
        this.mCurveLevel = -1;
        this.mPowerOnEanble = false;
        this.mPersonalizedBrightnessCurveEnable = false;
        this.mLuxPonits = new float[]{0.0f, 2.0f, 5.0f, 10.0f, 15.0f, 20.0f, 30.0f, 50.0f, 70.0f, 100.0f, 150.0f, 200.0f, 250.0f, 300.0f, 350.0f, 400.0f, 500.0f, 600.0f, 700.0f, 800.0f, 900.0f, 1000.0f, 1200.0f, 1400.0f, 1800.0f, 2400.0f, 3000.0f, 4000.0f, 5000.0f, 6000.0f, 8000.0f, 10000.0f, 20000.0f, 30000.0f, 40000.0f};
        this.mVehicleModeBrightnessEnable = false;
        this.mVehicleModeClearOffsetEnable = false;
        this.mVehicleModeEnable = false;
        this.mVehicleModeBrighntess = minBrightness;
        this.mVehicleModeLuxThreshold = 0.0f;
        this.mSceneLevel = -1;
        this.mGameModeEnable = false;
        this.mGameModeBrightnessEnable = false;
        this.mDeltaTmp = 0.0f;
        this.mGameModeOffsetLux = -1.0f;
        this.mGameModeBrightnessLinePointsList = new ArrayList();
        this.mGameModeStartLuxDefaultBrightness = -1.0f;
        this.mGameModePosBrightness = 0.0f;
        this.mDarkAdaptState = DarkAdaptState.UNADAPTED;
        this.mDarkAdaptStateDetected = DarkAdaptState.UNADAPTED;
        this.mUsePowerSavingModeCurveEnable = false;
        this.mVehicleModeQuitForPowerOnEnable = false;
        this.mBrightnessForLog = -1.0f;
        this.mCurrentCurveLevel = -1;
        this.mManager = new DisplayEngineManager();
        this.mIsReboot = true;
        this.mContentResolver = context.getContentResolver();
        this.mDeviceActualBrightnessLevel = deviceActualBrightnessLevel;
        this.mDeviceActualBrightnessNit = deviceActualBrightnessNit;
        this.mDeviceStandardBrightnessNit = deviceStandardBrightnessNit;
        loadCameraDefaultBrightnessLine();
        loadReadingDefaultBrightnessLine();
        loadPowerSavingDefaultBrightnessLine();
        loadOminLevelCountLevelPointsList();
        loadGameModeDefaultBrightnessLine();
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("clear autobrightness offset,orig mPosBrightnessSaved=");
                stringBuilder.append(mPosBrightnessSaved);
                Slog.i(str, stringBuilder.toString());
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("get xmlFile :");
        stringBuilder.append(xmlPath);
        stringBuilder.append(" failed!");
        Slog.e(str, stringBuilder.toString());
        return null;
    }

    private String getLcdPanelName() {
        IBinder binder = ServiceManager.getService(DisplayEngineManager.SERVICE_NAME);
        String panelName = null;
        if (binder == null) {
            Slog.i(TAG, "getLcdPanelName() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = Stub.asInterface(binder);
        if (mService == null) {
            Slog.e(TAG, "getLcdPanelName() mService is null!");
            return null;
        }
        byte[] name = new byte[128];
        int ret = 0;
        try {
            int ret2 = mService.getEffect(14, 0, name, name.length);
            if (ret2 != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getLcdPanelName() getEffect failed! ret=");
                stringBuilder.append(ret2);
                Slog.e(str, stringBuilder.toString());
                return null;
            }
            try {
                panelName = new String(name, "UTF-8").trim().replace(' ', '_');
            } catch (UnsupportedEncodingException e) {
                Slog.e(TAG, "Unsupported encoding type!");
            }
            return panelName;
        } catch (RemoteException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getLcdPanelName() RemoteException ");
            stringBuilder2.append(e2);
            Slog.e(str2, stringBuilder2.toString());
            return null;
        }
    }

    private String getVersionFromTouchOemInfo() {
        String version = null;
        try {
            File file = new File(String.format("%s", new Object[]{TOUCH_OEM_INFO_PATH}));
            if (file.exists()) {
                String touch_oem_info = FileUtils.readTextFile(file, 0, null).trim();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("touch_oem_info=");
                stringBuilder.append(touch_oem_info);
                Slog.i(str, stringBuilder.toString());
                String[] versionInfo = touch_oem_info.split(",");
                if (versionInfo.length > 15) {
                    try {
                        int productYear = Integer.parseInt(versionInfo[12]);
                        int productMonth = Integer.parseInt(versionInfo[13]);
                        int productDay = Integer.parseInt(versionInfo[14]);
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("lcdversionInfo orig productYear=");
                        stringBuilder2.append(productYear);
                        stringBuilder2.append(",productMonth=");
                        stringBuilder2.append(productMonth);
                        stringBuilder2.append(",productDay=");
                        stringBuilder2.append(productDay);
                        Slog.i(str2, stringBuilder2.toString());
                        String str3;
                        StringBuilder stringBuilder3;
                        if (productYear < 48 || productYear > 57) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("lcdversionInfo not valid productYear=");
                            stringBuilder3.append(productYear);
                            Slog.i(str3, stringBuilder3.toString());
                            return null;
                        }
                        String version2;
                        productYear -= 48;
                        if (productMonth >= 48 && productMonth <= 57) {
                            productMonth -= 48;
                        } else if (productMonth < 65 || productMonth > 67) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("lcdversionInfo not valid productMonth=");
                            stringBuilder3.append(productMonth);
                            Slog.i(str3, stringBuilder3.toString());
                            return null;
                        } else {
                            productMonth = (productMonth - 65) + 10;
                        }
                        if (productDay >= 48 && productDay <= 57) {
                            productDay -= 48;
                        } else if (productDay < 65 || productDay > 88) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("lcdversionInfo not valid productDay=");
                            stringBuilder3.append(productDay);
                            Slog.i(str3, stringBuilder3.toString());
                            return null;
                        } else {
                            productDay = (productDay - 65) + 10;
                        }
                        if (productYear > 8) {
                            version2 = "vn2";
                        } else if (productYear == 8 && productMonth > 1) {
                            version2 = "vn2";
                        } else if (productYear == 8 && productMonth == 1 && productDay >= 22) {
                            version2 = "vn2";
                        } else {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("lcdversionInfo not valid version;productYear=");
                            stringBuilder3.append(productYear);
                            stringBuilder3.append(",productMonth=");
                            stringBuilder3.append(productMonth);
                            stringBuilder3.append(",productDay=");
                            stringBuilder3.append(productDay);
                            Slog.i(str3, stringBuilder3.toString());
                            return null;
                        }
                        version = version2;
                        version2 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("lcdversionInfo real vn2,productYear=");
                        stringBuilder4.append(productYear);
                        stringBuilder4.append(",productMonth=");
                        stringBuilder4.append(productMonth);
                        stringBuilder4.append(",productDay=");
                        stringBuilder4.append(productDay);
                        Slog.i(version2, stringBuilder4.toString());
                    } catch (NumberFormatException e) {
                        Slog.i(TAG, "lcdversionInfo versionfile num is not valid,no need version");
                        return null;
                    }
                }
                Slog.i(TAG, "lcdversionInfo versionfile info length is not valid,no need version");
            } else {
                Slog.i(TAG, "lcdversionInfo versionfile is not exists, no need version,filePath=/sys/touchscreen/touch_oem_info");
            }
        } catch (IOException e2) {
            Slog.w(TAG, "Error reading touch_oem_info", e2);
        }
        return version;
    }

    private String getVersionFromLCD() {
        IBinder binder = ServiceManager.getService(DisplayEngineManager.SERVICE_NAME);
        String panelVersion = null;
        if (binder == null) {
            Slog.i(TAG, "getLcdPanelName() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = Stub.asInterface(binder);
        if (mService == null) {
            Slog.e(TAG, "getLcdPanelName() mService is null!");
            return null;
        }
        byte[] name = new byte[32];
        String key;
        try {
            int ret = mService.getEffect(14, 3, name, name.length);
            String str;
            StringBuilder stringBuilder;
            if (ret != 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getLcdPanelName() getEffect failed! ret=");
                stringBuilder.append(ret);
                Slog.e(str, stringBuilder.toString());
                return null;
            }
            try {
                str = new String(name, "UTF-8").trim();
                key = "VER:";
                int index = str.indexOf(key);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getVersionFromLCD() index=");
                stringBuilder2.append(index);
                stringBuilder2.append(",lcdVersion=");
                stringBuilder2.append(str);
                Slog.i(str2, stringBuilder2.toString());
                if (index != -1) {
                    panelVersion = str.substring(key.length() + index);
                }
            } catch (UnsupportedEncodingException e) {
                Slog.e(TAG, "Unsupported encoding type!");
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getVersionFromLCD() panelVersion=");
            stringBuilder.append(panelVersion);
            Slog.i(str, stringBuilder.toString());
            return panelVersion;
        } catch (RemoteException e2) {
            key = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getLcdPanelName() RemoteException ");
            stringBuilder3.append(e2);
            Slog.e(key, stringBuilder3.toString());
            return null;
        }
    }

    private File getNormalXmlFile() {
        String lcdname = getLcdPanelName();
        String lcdversion = getVersionFromTouchOemInfo();
        String lcdversionNew = getVersionFromLCD();
        String screenColor = SystemProperties.get("ro.config.devicecolor");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("screenColor=");
        stringBuilder.append(screenColor);
        Slog.i(str, stringBuilder.toString());
        ArrayList<String> xmlPathList = new ArrayList();
        int i = 0;
        if (lcdversion != null) {
            xmlPathList.add(String.format("/xml/lcd/%s_%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, lcdversion, screenColor, XML_EXT}));
            xmlPathList.add(String.format("/xml/lcd/%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, lcdversion, XML_EXT}));
        }
        if (lcdversionNew != null) {
            xmlPathList.add(String.format("/xml/lcd/%s_%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, lcdversionNew, screenColor, XML_EXT}));
            xmlPathList.add(String.format("/xml/lcd/%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, lcdversionNew, XML_EXT}));
        }
        xmlPathList.add(String.format("/xml/lcd/%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, screenColor, XML_EXT}));
        xmlPathList.add(String.format("/xml/lcd/%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, XML_EXT}));
        xmlPathList.add(String.format("/xml/lcd/%s_%s%s", new Object[]{XML_NAME_NOEXT, screenColor, XML_EXT}));
        xmlPathList.add(String.format("/xml/lcd/%s", new Object[]{HW_LABC_CONFIG_FILE}));
        File xmlFile = null;
        int listsize = xmlPathList.size();
        while (true) {
            int i2 = i;
            if (i2 < listsize) {
                xmlFile = HwCfgFilePolicy.getCfgFile((String) xmlPathList.get(i2), 2);
                if (xmlFile != null) {
                    return xmlFile;
                }
                i = i2 + 1;
            } else {
                Slog.e(TAG, "get failed!");
                return xmlFile;
            }
        }
    }

    /* JADX WARNING: Missing block: B:45:0x00d3, code skipped:
            if (r3 == null) goto L_0x00d6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getConfig() throws IOException {
        File xmlFile;
        String currentMode = SystemProperties.get("ro.runmode");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("currentMode=");
        stringBuilder.append(currentMode);
        Slog.i(str, stringBuilder.toString());
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
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(xmlFile);
            if (getConfigFromXML(inputStream)) {
                fillDarkAdaptPointsList();
                if (true == checkConfigLoadedFromXML()) {
                    if (DEBUG) {
                        printConfigFromXML();
                    }
                    initLinePointsList();
                    if (DEBUG) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("mBrightnessCalibrationEnabled=");
                        stringBuilder2.append(this.mBrightnessCalibrationEnabled);
                        stringBuilder2.append(",mDeviceActualBrightnessNit=");
                        stringBuilder2.append(this.mDeviceActualBrightnessNit);
                        stringBuilder2.append(",mDeviceStandardBrightnessNit=");
                        stringBuilder2.append(this.mDeviceStandardBrightnessNit);
                        Slog.i(str2, stringBuilder2.toString());
                    }
                    if (this.mBrightnessCalibrationEnabled) {
                        brightnessCalibration(this.mDefaultBrighnessLinePointsList, this.mDeviceActualBrightnessNit, this.mDeviceStandardBrightnessNit);
                    }
                }
                inputStream.close();
                getDayBrightnessLinePoints();
                updateNewBrightnessCurve();
                inputStream.close();
                return true;
            }
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "getConfig : FileNotFoundException");
        } catch (IOException e2) {
            Slog.e(TAG, "getConfig : IOException");
            if (inputStream != null) {
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        inputStream.close();
        return false;
    }

    private boolean checkConfigLoadedFromXML() {
        String str;
        StringBuilder stringBuilder;
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
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("LoadXML false, mOffsetBrightenRatioLeft=");
            stringBuilder.append(this.mOffsetBrightenRatioLeft);
            stringBuilder.append(",mOffsetBrightenAlphaLeft=");
            stringBuilder.append(this.mOffsetBrightenAlphaLeft);
            Slog.e(str, stringBuilder.toString());
            return false;
        } else if (this.mOffsetBrightenAlphaRight < 0.0f || ((double) this.mOffsetBrightenAlphaRight) > 1.0d) {
            loadDefaultConfig();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("LoadXML false, mOffsetBrightenAlphaRight=");
            stringBuilder.append(this.mOffsetBrightenAlphaRight);
            Slog.e(str, stringBuilder.toString());
            return false;
        } else if (this.mOffsetDarkenAlphaLeft < 0.0f || ((double) this.mOffsetDarkenAlphaLeft) > 1.0d) {
            loadDefaultConfig();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("LoadXML false, mOffsetDarkenAlphaLeft=");
            stringBuilder.append(this.mOffsetDarkenAlphaLeft);
            Slog.e(str, stringBuilder.toString());
            return false;
        } else {
            if (this.mOminLevelModeEnable) {
                if (this.mOminLevelCountValidLuxTh < 0) {
                    loadDefaultConfig();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("LoadXML false, mOminLevelCountValidLuxTh=");
                    stringBuilder.append(this.mOminLevelCountValidLuxTh);
                    Slog.e(str, stringBuilder.toString());
                    return false;
                } else if (this.mOminLevelCountValidTimeTh < 0) {
                    loadDefaultConfig();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("LoadXML false, mOminLevelCountValidTimeTh=");
                    stringBuilder.append(this.mOminLevelCountValidTimeTh);
                    Slog.e(str, stringBuilder.toString());
                    return false;
                } else if (!checkPointsListIsOK(this.mOminLevelCountLevelPointsList)) {
                    loadDefaultConfig();
                    Slog.e(TAG, "checkPointsList mOminLevelPointsList is wrong, LoadDefaultConfig!");
                    return false;
                }
            }
            if (this.mDarkAdaptingBrightnessPointsList != null && !checkPointsListIsOK(this.mDarkAdaptingBrightnessPointsList)) {
                loadDefaultConfig();
                Slog.e(TAG, "checkPointsList mDarkAdaptingBrightnessPointsList is wrong, LoadDefaultConfig!");
                return false;
            } else if (this.mDarkAdaptedBrightnessPointsList != null && !checkPointsListIsOK(this.mDarkAdaptedBrightnessPointsList)) {
                loadDefaultConfig();
                Slog.e(TAG, "checkPointsList mDarkAdaptedBrightnessPointsList is wrong, LoadDefaultConfig!");
                return false;
            } else if (this.mVehicleModeBrighntess < 0.0f || this.mVehicleModeBrighntess > maxBrightness || this.mVehicleModeLuxThreshold < 0.0f) {
                loadDefaultConfig();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("VehicleBrightMode LoadDefaultConfig!,mVehicleModeBrighntess=");
                stringBuilder.append(this.mVehicleModeBrighntess);
                stringBuilder.append(",mVehicleModeLuxThreshold=");
                stringBuilder.append(this.mVehicleModeLuxThreshold);
                Slog.e(str, stringBuilder.toString());
                return false;
            } else if (this.mDayModeMinimumBrightness > maxBrightness) {
                loadDefaultConfig();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DayModeMinimumBrightness LoadDefaultConfig!,mDayModeMinimumBrightness=");
                stringBuilder.append(this.mDayModeMinimumBrightness);
                Slog.e(str, stringBuilder.toString());
                return false;
            } else {
                if (DEBUG) {
                    Slog.i(TAG, "checkConfigLoadedFromXML success!");
                }
                return true;
            }
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
        String str;
        StringBuilder stringBuilder;
        if (mActulBrightnessNit < 400 || mActulBrightnessNit > 1000 || mStandardBrightnessNit > 1000 || mStandardBrightnessNit <= 0) {
            this.mCalibrationRatio = 1.0f;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("error input brightnessNit:mStandardBrightnessNit=");
            stringBuilder.append(mStandardBrightnessNit);
            stringBuilder.append(",mActulBrightnessNit=");
            stringBuilder.append(mActulBrightnessNit);
            Slog.e(str, stringBuilder.toString());
        } else {
            this.mCalibrationRatio = ((float) mStandardBrightnessNit) / ((float) mActulBrightnessNit);
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mCalibrationRatio=");
                stringBuilder.append(this.mCalibrationRatio);
                stringBuilder.append(",mStandardBrightnessNit=");
                stringBuilder.append(mStandardBrightnessNit);
                stringBuilder.append(",mActulBrightnessNit=");
                stringBuilder.append(mActulBrightnessNit);
                Slog.i(str, stringBuilder.toString());
            }
        }
        int listSize = mLinePointsList.size();
        for (int i = 1; i < listSize; i++) {
            Point pointTemp = (Point) mLinePointsList.get(i);
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
        for (Point temp : mLinePointsList) {
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("LoadXMLConfig_NewCalibrationBrighnessLinePoints x = ");
                stringBuilder2.append(temp.x);
                stringBuilder2.append(", y = ");
                stringBuilder2.append(temp.y);
                Slog.i(str2, stringBuilder2.toString());
            }
        }
    }

    private void updateLinePointsListForCalibration() {
        if (this.mBrightnessCalibrationEnabled && Math.abs(this.mCalibrationRatio - 1.0f) > 1.0E-7f) {
            String str;
            StringBuilder stringBuilder;
            if (this.mPowerSavingBrighnessLineEnable && this.mPowerSavingBrighnessLinePointsList != null) {
                updateNewLinePointsListForCalibration(this.mPowerSavingBrighnessLinePointsList);
                Slog.i(TAG, "update PowerSavingBrighnessLinePointsList for calibration");
                if (DEBUG && this.mPowerSavingBrighnessLinePointsList != null) {
                    for (Point temp : this.mPowerSavingBrighnessLinePointsList) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("LoadXMLConfig_NewCalibrationPowerSavingPointsList x = ");
                        stringBuilder.append(temp.x);
                        stringBuilder.append(", y = ");
                        stringBuilder.append(temp.y);
                        Slog.d(str, stringBuilder.toString());
                    }
                }
            }
            if (this.mCameraBrighnessLinePointsList != null) {
                updateNewLinePointsListForCalibration(this.mCameraBrighnessLinePointsList);
                if (DEBUG) {
                    Slog.i(TAG, "update mCameraBrighnessLinePointsList for calibration");
                }
            }
            if (this.mGameModeBrightnessLinePointsList != null) {
                updateNewLinePointsListForCalibration(this.mGameModeBrightnessLinePointsList);
                if (DEBUG) {
                    Slog.i(TAG, "update mGameModeBrightnessLinePointsList for calibration");
                }
                if (DEBUG && this.mGameModeBrightnessLinePointsList != null) {
                    for (Point temp2 : this.mGameModeBrightnessLinePointsList) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("LoadXMLConfig_GameModeBrightnessLinePointsList x = ");
                        stringBuilder.append(temp2.x);
                        stringBuilder.append(", y = ");
                        stringBuilder.append(temp2.y);
                        Slog.d(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    private void updateNewLinePointsListForCalibration(List<Point> LinePointsList) {
        List<Point> mLinePointsList = LinePointsList;
        int listSize = mLinePointsList.size();
        for (int i = 1; i < listSize; i++) {
            Point pointTemp = (Point) mLinePointsList.get(i);
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
        if (mLinePointsList == null) {
            Slog.e(TAG, "LoadXML false for mLinePointsList == null");
            return false;
        } else if (mLinePointsList.size() <= 2 || mLinePointsList.size() >= 100) {
            Slog.e(TAG, "LoadXML false for mLinePointsList number is wrong");
            return false;
        } else {
            Point lastPoint = null;
            for (Point tmpPoint : mLinePointsList) {
                if (lastPoint != null && lastPoint.x >= tmpPoint.x) {
                    loadDefaultConfig();
                    Slog.e(TAG, "LoadXML false for mLinePointsList is wrong");
                    return false;
                }
                lastPoint = tmpPoint;
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DayMode:u=");
                stringBuilder.append(u);
                stringBuilder.append(", v=");
                stringBuilder.append(v);
                Slog.i(str, stringBuilder.toString());
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
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("DayMode:DayBrightnessLine: =");
                    stringBuilder2.append(temp22.x);
                    stringBuilder2.append(", y=");
                    stringBuilder2.append(temp22.y);
                    Slog.i(str2, stringBuilder2.toString());
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
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mOminLevelMode:LinePointsList: x=");
                    stringBuilder.append(temp.x);
                    stringBuilder.append(", y=");
                    stringBuilder.append(temp.y);
                    Slog.d(str, stringBuilder.toString());
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
            String str;
            StringBuilder stringBuilder;
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
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mOminLevelMode updateMinLevel x(0)=");
                stringBuilder.append(temp.x);
                stringBuilder.append(",y(0)=");
                stringBuilder.append(temp.y);
                stringBuilder.append(",y(0)==y(1)");
                Slog.w(str, stringBuilder.toString());
            }
            if (DEBUG && this.mOminLevelCountEnable && this.mOminLevelCount < countThMax) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mOminLevelMode updateMinLevel x(0)=");
                stringBuilder.append(temp.x);
                stringBuilder.append(",y(0)=");
                stringBuilder.append(temp.y);
                Slog.d(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mOminLevelMode ominLevelCount=");
            stringBuilder.append(ominLevelCount);
            stringBuilder.append(",mOminLevel=");
            stringBuilder.append(this.mOminLevel);
            stringBuilder.append(",cmin=");
            stringBuilder.append(countThMin);
            stringBuilder.append(",cmax=");
            stringBuilder.append(countThMax);
            Slog.i(str, stringBuilder.toString());
        }
        return this.mOminLevel;
    }

    private float getOminLevelFromCountInternal(List<Point> linePointsList, float levelCount) {
        List<Point> linePointsListIn = linePointsList;
        float brightnessLevel = minBrightness;
        if (linePointsListIn == null) {
            Slog.i(TAG, "mOminLevelMode linePointsListIn==null,return minBrightness");
            return minBrightness;
        }
        Point temp1 = null;
        for (Point temp : linePointsListIn) {
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
        this.mDarkAdaptingBrightnessPointsList = null;
        this.mDarkAdaptingBrightness0LuxLevel = 0;
        this.mDarkAdaptedBrightnessPointsList = null;
        this.mDarkAdaptedBrightness0LuxLevel = 0;
        this.mDarkAdaptEnable = false;
        this.mDayModeMinimumBrightness = minBrightness;
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

    private void loadReadingDefaultBrightnessLine() {
        if (this.mReadingBrighnessLinePointsList != null) {
            this.mReadingBrighnessLinePointsList.clear();
        } else {
            this.mReadingBrighnessLinePointsList = new ArrayList();
        }
        this.mReadingBrighnessLinePointsList.add(new Point(0.0f, minBrightness));
        this.mReadingBrighnessLinePointsList.add(new Point(25.0f, 46.5f));
        this.mReadingBrighnessLinePointsList.add(new Point(1995.0f, 140.7f));
        this.mReadingBrighnessLinePointsList.add(new Point(4000.0f, maxBrightness));
        this.mReadingBrighnessLinePointsList.add(new Point(40000.0f, maxBrightness));
    }

    private void loadGameModeDefaultBrightnessLine() {
        if (this.mGameModeBrightnessLinePointsList != null) {
            this.mGameModeBrightnessLinePointsList.clear();
        } else {
            this.mGameModeBrightnessLinePointsList = new ArrayList();
        }
        this.mGameModeBrightnessLinePointsList.add(new Point(0.0f, minBrightness));
        this.mGameModeBrightnessLinePointsList.add(new Point(25.0f, 46.5f));
        this.mGameModeBrightnessLinePointsList.add(new Point(1995.0f, 140.7f));
        this.mGameModeBrightnessLinePointsList.add(new Point(4000.0f, maxBrightness));
        this.mGameModeBrightnessLinePointsList.add(new Point(40000.0f, maxBrightness));
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
        String str;
        StringBuilder stringBuilder;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("LoadXMLConfig_DefaultBrightness=");
        stringBuilder2.append(this.mDefaultBrightness);
        Slog.i(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("LoadXMLConfig_mBrightnessCalibrationEnabled=");
        stringBuilder2.append(this.mBrightnessCalibrationEnabled);
        stringBuilder2.append(",mPowerSavingBrighnessLineEnable=");
        stringBuilder2.append(this.mPowerSavingBrighnessLineEnable);
        stringBuilder2.append(",mOffsetBrightenRatioLeft=");
        stringBuilder2.append(this.mOffsetBrightenRatioLeft);
        stringBuilder2.append(",mOffsetBrightenAlphaLeft=");
        stringBuilder2.append(this.mOffsetBrightenAlphaLeft);
        stringBuilder2.append(",mOffsetBrightenAlphaRight=");
        stringBuilder2.append(this.mOffsetBrightenAlphaRight);
        stringBuilder2.append(",mOffsetDarkenAlphaLeft=");
        stringBuilder2.append(this.mOffsetDarkenAlphaLeft);
        stringBuilder2.append(",mManualMode=");
        stringBuilder2.append(this.mManualMode);
        stringBuilder2.append(",mManualBrightnessMaxLimit=");
        stringBuilder2.append(this.mManualBrightnessMaxLimit);
        stringBuilder2.append(",mPersonalizedBrightnessCurveEnable=");
        stringBuilder2.append(this.mPersonalizedBrightnessCurveEnable);
        stringBuilder2.append(",mVehicleModeEnable=");
        stringBuilder2.append(this.mVehicleModeEnable);
        stringBuilder2.append(",mVehicleModeBrighntess=");
        stringBuilder2.append(this.mVehicleModeBrighntess);
        stringBuilder2.append(",mVehicleModeLuxThreshold=");
        stringBuilder2.append(this.mVehicleModeLuxThreshold);
        stringBuilder2.append(",mGameModeEnable=");
        stringBuilder2.append(this.mGameModeEnable);
        Slog.i(str2, stringBuilder2.toString());
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("LoadXMLConfig_mOminLevelMode=");
        stringBuilder2.append(this.mOminLevelModeEnable);
        stringBuilder2.append(",mCountEnable=");
        stringBuilder2.append(this.mOminLevelOffsetCountEnable);
        stringBuilder2.append(",mDayEn=");
        stringBuilder2.append(this.mOminLevelDayModeEnable);
        stringBuilder2.append(",ValidLux=");
        stringBuilder2.append(this.mOminLevelCountValidLuxTh);
        stringBuilder2.append(",ValidTime=");
        stringBuilder2.append(this.mOminLevelCountValidTimeTh);
        stringBuilder2.append(",mLongTime=");
        stringBuilder2.append(this.mOminLevelCountResetLongTimeTh);
        stringBuilder2.append(",EyeEn=");
        stringBuilder2.append(this.mEyeProtectionSplineEnable);
        Slog.i(str2, stringBuilder2.toString());
        for (Point temp : this.mDefaultBrighnessLinePointsList) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("LoadXMLConfig_DefaultBrighnessLinePoints x = ");
            stringBuilder.append(temp.x);
            stringBuilder.append(", y = ");
            stringBuilder.append(temp.y);
            Slog.i(str, stringBuilder.toString());
        }
        for (Point temp2 : this.mCameraBrighnessLinePointsList) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("LoadXMLConfig_CameraBrighnessLinePointsList x = ");
            stringBuilder.append(temp2.x);
            stringBuilder.append(", y = ");
            stringBuilder.append(temp2.y);
            Slog.i(str, stringBuilder.toString());
        }
        for (Point temp22 : this.mReadingBrighnessLinePointsList) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("LoadXMLConfig_ReadingBrighnessLinePointsList x = ");
            stringBuilder.append(temp22.x);
            stringBuilder.append(", y = ");
            stringBuilder.append(temp22.y);
            Slog.i(str, stringBuilder.toString());
        }
        for (Point temp222 : this.mGameModeBrightnessLinePointsList) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("LoadXMLConfig_mGameModeBrightnessLinePointsList x = ");
            stringBuilder.append(temp222.x);
            stringBuilder.append(", y = ");
            stringBuilder.append(temp222.y);
            Slog.i(str, stringBuilder.toString());
        }
        for (Point temp2222 : this.mPowerSavingBrighnessLinePointsList) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("LoadXMLConfig_mPowerSavingBrighnessLinePointsList x = ");
            stringBuilder.append(temp2222.x);
            stringBuilder.append(", y = ");
            stringBuilder.append(temp2222.y);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mOminLevelModeEnable && this.mOminLevelCountLevelPointsList != null) {
            for (Point temp22222 : this.mOminLevelCountLevelPointsList) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LoadXMLConfig_mOminLevelCountLevelPointsList x = ");
                stringBuilder.append(temp22222.x);
                stringBuilder.append(", y = ");
                stringBuilder.append(temp22222.y);
                Slog.i(str, stringBuilder.toString());
            }
        }
        if (this.mDarkAdaptEnable) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("LoadXMLConfig_DarkAdaptingBrightness0LuxLevel = ");
            stringBuilder2.append(this.mDarkAdaptingBrightness0LuxLevel);
            stringBuilder2.append(", Adapted = ");
            stringBuilder2.append(this.mDarkAdaptedBrightness0LuxLevel);
            Slog.i(str2, stringBuilder2.toString());
        }
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("LoadXMLConfig_DayModeMinimumBrightness = ");
        stringBuilder2.append(this.mDayModeMinimumBrightness);
        Slog.i(str2, stringBuilder2.toString());
    }

    /* JADX WARNING: Missing block: B:25:0x0062, code skipped:
            r4 = r23;
     */
    /* JADX WARNING: Missing block: B:335:0x070f, code skipped:
            if (r16 == false) goto L_0x0715;
     */
    /* JADX WARNING: Missing block: B:336:0x0711, code skipped:
            r2 = r20;
     */
    /* JADX WARNING: Missing block: B:338:?, code skipped:
            r0 = r3.next();
     */
    /* JADX WARNING: Missing block: B:339:0x0719, code skipped:
            r2 = r35;
     */
    /* JADX WARNING: Missing block: B:340:0x071d, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:341:0x071e, code skipped:
            r30 = r3;
            r2 = r20;
     */
    /* JADX WARNING: Missing block: B:342:0x0724, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:343:0x0725, code skipped:
            r30 = r3;
            r2 = r20;
     */
    /* JADX WARNING: Missing block: B:344:0x072b, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:345:0x072c, code skipped:
            r30 = r3;
            r2 = r20;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getConfigFromXML(InputStream inStream) {
        boolean DefaultBrightnessLoaded;
        boolean z;
        boolean CameraBrightnessLinePointsListLoaded;
        XmlPullParserException e;
        Object obj;
        IOException e2;
        NumberFormatException e3;
        XmlPullParser parser;
        if (DEBUG) {
            Slog.i(TAG, "getConfigFromeXML");
        }
        boolean DefaultBrighnessLinePointsListLoaded = false;
        boolean CameraBrightnessLinePointsListsLoadStarted = false;
        boolean CameraBrightnessLinePointsListLoaded2 = false;
        boolean ReadingBrightnessLinePointsListsLoadStarted = false;
        boolean GameModeBrightnessLinePointsListsLoadStarted = false;
        boolean PowerSavingBrightnessLinePointsListsLoadStarted = false;
        boolean OminLevelCountLevelLinePointsListsLoadStarted = false;
        boolean configGroupLoadStarted = false;
        boolean loadFinished = false;
        XmlPullParser parser2 = Xml.newPullParser();
        boolean DefaultBrighnessLinePointsListsLoadStarted;
        try {
            DefaultBrightnessLoaded = false;
            DefaultBrighnessLinePointsListsLoadStarted = false;
            boolean DefaultBrighnessLinePointsListsLoadStarted2 = parser2;
            boolean DefaultBrightnessLoaded2;
            try {
                int i;
                String deviceLevelString;
                DefaultBrighnessLinePointsListsLoadStarted2.setInput(inStream, "UTF-8");
                int eventType = DefaultBrighnessLinePointsListsLoadStarted2.getEventType();
                while (eventType != 1) {
                    Object obj2;
                    String name;
                    switch (eventType) {
                        case 2:
                            i = eventType;
                            z = DefaultBrighnessLinePointsListLoaded;
                            try {
                                DefaultBrightnessLoaded2 = DefaultBrighnessLinePointsListsLoadStarted2.getName();
                                try {
                                    if (!DefaultBrightnessLoaded2.equals(XML_NAME_NOEXT)) {
                                        CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                        if (configGroupLoadStarted) {
                                            Point currentPoint;
                                            String s;
                                            if (DefaultBrightnessLoaded2.equals("DefaultBrightness")) {
                                                this.mDefaultBrightness = Float.parseFloat(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                                DefaultBrightnessLoaded = true;
                                            } else if (DefaultBrightnessLoaded2.equals("BrightnessCalibrationEnabled")) {
                                                this.mBrightnessCalibrationEnabled = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OffsetBrightenRatioLeft")) {
                                                this.mOffsetBrightenRatioLeft = Float.parseFloat(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OffsetBrightenAlphaLeft")) {
                                                this.mOffsetBrightenAlphaLeft = Float.parseFloat(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OffsetBrightenAlphaRight")) {
                                                this.mOffsetBrightenAlphaRight = Float.parseFloat(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OffsetDarkenAlphaLeft")) {
                                                this.mOffsetDarkenAlphaLeft = Float.parseFloat(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("PersonalizedBrightnessCurveEnable")) {
                                                this.mPersonalizedBrightnessCurveEnable = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("DayModeMinimumBrightness")) {
                                                this.mDayModeMinimumBrightness = Float.parseFloat(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("VehicleModeEnable")) {
                                                this.mVehicleModeEnable = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("VehicleModeBrighntess")) {
                                                this.mVehicleModeBrighntess = Float.parseFloat(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("VehicleModeLuxThreshold")) {
                                                this.mVehicleModeLuxThreshold = Float.parseFloat(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("GameModeEnable")) {
                                                this.mGameModeEnable = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("DefaultBrightnessPoints")) {
                                                DefaultBrighnessLinePointsListsLoadStarted = true;
                                            } else if (DefaultBrightnessLoaded2.equals("Point") && DefaultBrighnessLinePointsListsLoadStarted) {
                                                currentPoint = new Point();
                                                s = DefaultBrighnessLinePointsListsLoadStarted2.nextText();
                                                currentPoint.x = Float.parseFloat(s.split(",")[0]);
                                                currentPoint.y = Float.parseFloat(s.split(",")[1]);
                                                if (this.mDefaultBrighnessLinePointsList == null) {
                                                    this.mDefaultBrighnessLinePointsList = new ArrayList();
                                                }
                                                this.mDefaultBrighnessLinePointsList.add(currentPoint);
                                            } else if (DefaultBrightnessLoaded2.equals("CameraBrightnessPoints")) {
                                                CameraBrightnessLinePointsListsLoadStarted = true;
                                                if (this.mCameraBrighnessLinePointsList != null) {
                                                    this.mCameraBrighnessLinePointsList.clear();
                                                }
                                            } else if (DefaultBrightnessLoaded2.equals("Point") && CameraBrightnessLinePointsListsLoadStarted) {
                                                currentPoint = new Point();
                                                s = DefaultBrighnessLinePointsListsLoadStarted2.nextText();
                                                currentPoint.x = Float.parseFloat(s.split(",")[0]);
                                                currentPoint.y = Float.parseFloat(s.split(",")[1]);
                                                if (this.mCameraBrighnessLinePointsList == null) {
                                                    this.mCameraBrighnessLinePointsList = new ArrayList();
                                                }
                                                this.mCameraBrighnessLinePointsList.add(currentPoint);
                                            } else if (DefaultBrightnessLoaded2.equals("ReadingBrightnessPoints")) {
                                                ReadingBrightnessLinePointsListsLoadStarted = true;
                                                if (this.mReadingBrighnessLinePointsList != null) {
                                                    this.mReadingBrighnessLinePointsList.clear();
                                                }
                                            } else if (DefaultBrightnessLoaded2.equals("Point") && ReadingBrightnessLinePointsListsLoadStarted) {
                                                currentPoint = new Point();
                                                s = DefaultBrighnessLinePointsListsLoadStarted2.nextText();
                                                currentPoint.x = Float.parseFloat(s.split(",")[0]);
                                                currentPoint.y = Float.parseFloat(s.split(",")[1]);
                                                if (this.mReadingBrighnessLinePointsList == null) {
                                                    this.mReadingBrighnessLinePointsList = new ArrayList();
                                                }
                                                this.mReadingBrighnessLinePointsList.add(currentPoint);
                                            } else if (DefaultBrightnessLoaded2.equals("GameModeBrightnessPoints")) {
                                                GameModeBrightnessLinePointsListsLoadStarted = true;
                                                if (this.mGameModeBrightnessLinePointsList != null) {
                                                    this.mGameModeBrightnessLinePointsList.clear();
                                                }
                                            } else if (DefaultBrightnessLoaded2.equals("Point") && GameModeBrightnessLinePointsListsLoadStarted) {
                                                currentPoint = new Point();
                                                s = DefaultBrighnessLinePointsListsLoadStarted2.nextText();
                                                currentPoint.x = Float.parseFloat(s.split(",")[0]);
                                                currentPoint.y = Float.parseFloat(s.split(",")[1]);
                                                if (this.mGameModeBrightnessLinePointsList == null) {
                                                    this.mGameModeBrightnessLinePointsList = new ArrayList();
                                                }
                                                this.mGameModeBrightnessLinePointsList.add(currentPoint);
                                            } else if (DefaultBrightnessLoaded2.equals("PowerSavingBrightnessPoints")) {
                                                PowerSavingBrightnessLinePointsListsLoadStarted = true;
                                                if (this.mPowerSavingBrighnessLinePointsList != null) {
                                                    this.mPowerSavingBrighnessLinePointsList.clear();
                                                }
                                            } else if (DefaultBrightnessLoaded2.equals("Point") && PowerSavingBrightnessLinePointsListsLoadStarted) {
                                                currentPoint = new Point();
                                                s = DefaultBrighnessLinePointsListsLoadStarted2.nextText();
                                                currentPoint.x = Float.parseFloat(s.split(",")[0]);
                                                currentPoint.y = Float.parseFloat(s.split(",")[1]);
                                                if (this.mPowerSavingBrighnessLinePointsList == null) {
                                                    this.mPowerSavingBrighnessLinePointsList = new ArrayList();
                                                }
                                                this.mPowerSavingBrighnessLinePointsList.add(currentPoint);
                                            } else if (DefaultBrightnessLoaded2.equals("PowerSavingBrighnessLineEnable")) {
                                                this.mPowerSavingBrighnessLineEnable = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("ManualMode")) {
                                                if (Integer.parseInt(DefaultBrighnessLinePointsListsLoadStarted2.nextText()) == 1) {
                                                    this.mManualMode = true;
                                                }
                                            } else if (DefaultBrightnessLoaded2.equals("ManualBrightnessMaxLimit")) {
                                                if (this.mManualMode) {
                                                    this.mManualBrightnessMaxLimit = Integer.parseInt(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                                }
                                            } else if (DefaultBrightnessLoaded2.equals("DayModeAlgoEnable")) {
                                                this.mDayModeAlgoEnable = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("DayModeModifyNumPoint")) {
                                                this.mDayModeModifyNumPoint = Integer.parseInt(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("DayModeModifyMinBrightness")) {
                                                this.mDayModeModifyMinBrightness = Integer.parseInt(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OminLevelModeEnable")) {
                                                this.mOminLevelModeEnable = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OminLevelOffsetCountEnable")) {
                                                this.mOminLevelOffsetCountEnable = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OminLevelDayModeEnable")) {
                                                this.mOminLevelDayModeEnable = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OminLevelCountValidLuxTh")) {
                                                this.mOminLevelCountValidLuxTh = Integer.parseInt(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OminLevelCountValidTimeTh")) {
                                                this.mOminLevelCountValidTimeTh = Integer.parseInt(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OminLevelCountResetLongTimeTh")) {
                                                this.mOminLevelCountResetLongTimeTh = Integer.parseInt(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("EyeProtectionSplineEnable")) {
                                                this.mEyeProtectionSplineEnable = Boolean.parseBoolean(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("OminLevelCountLevelLinePoints")) {
                                                OminLevelCountLevelLinePointsListsLoadStarted = true;
                                                if (this.mOminLevelCountLevelPointsList != null) {
                                                    this.mOminLevelCountLevelPointsList.clear();
                                                }
                                            } else if (DefaultBrightnessLoaded2.equals("Point") && OminLevelCountLevelLinePointsListsLoadStarted) {
                                                currentPoint = new Point();
                                                DefaultBrighnessLinePointsListLoaded = DefaultBrighnessLinePointsListsLoadStarted2.nextText();
                                                currentPoint.x = Float.parseFloat(DefaultBrighnessLinePointsListLoaded.split(",")[0]);
                                                currentPoint.y = Float.parseFloat(DefaultBrighnessLinePointsListLoaded.split(",")[1]);
                                                if (this.mOminLevelCountLevelPointsList == null) {
                                                    this.mOminLevelCountLevelPointsList = new ArrayList();
                                                }
                                                this.mOminLevelCountLevelPointsList.add(currentPoint);
                                            } else if (DefaultBrightnessLoaded2.equals("AdaptingBrightness0LuxLevel")) {
                                                this.mDarkAdaptingBrightness0LuxLevel = Integer.parseInt(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            } else if (DefaultBrightnessLoaded2.equals("AdaptedBrightness0LuxLevel")) {
                                                this.mDarkAdaptedBrightness0LuxLevel = Integer.parseInt(DefaultBrighnessLinePointsListsLoadStarted2.nextText());
                                            }
                                        }
                                    } else if (this.mDeviceActualBrightnessLevel == 0) {
                                        try {
                                            if (DEBUG) {
                                                Slog.i(TAG, "actualDeviceLevel = 0, load started");
                                            }
                                            configGroupLoadStarted = true;
                                            obj2 = DefaultBrightnessLoaded2;
                                            break;
                                        } catch (XmlPullParserException e4) {
                                            e = e4;
                                            obj2 = DefaultBrightnessLoaded2;
                                            obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                            DefaultBrighnessLinePointsListLoaded = z;
                                            Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                                            Slog.e(TAG, "getConfigFromeXML false!");
                                            return false;
                                        } catch (IOException e5) {
                                            e2 = e5;
                                            obj2 = DefaultBrightnessLoaded2;
                                            obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                            DefaultBrighnessLinePointsListLoaded = z;
                                            Slog.e(TAG, "getConfigFromXML : IOException");
                                            Slog.e(TAG, "getConfigFromeXML false!");
                                            return false;
                                        } catch (NumberFormatException e6) {
                                            e3 = e6;
                                            obj2 = DefaultBrightnessLoaded2;
                                            obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                            DefaultBrighnessLinePointsListLoaded = z;
                                            Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                            Slog.e(TAG, "getConfigFromeXML false!");
                                            return false;
                                        }
                                    } else {
                                        StringBuilder stringBuilder;
                                        deviceLevelString = DefaultBrighnessLinePointsListsLoadStarted2.getAttributeValue(null, "level");
                                        if (deviceLevelString == null) {
                                            CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                        } else if (deviceLevelString.length() == 0) {
                                            String str = deviceLevelString;
                                            CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                        } else {
                                            int deviceLevel = Integer.parseInt(deviceLevelString);
                                            if (deviceLevel == this.mDeviceActualBrightnessLevel) {
                                                if (DEBUG) {
                                                    deviceLevelString = TAG;
                                                    stringBuilder = new StringBuilder();
                                                    CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                                    try {
                                                        stringBuilder.append("actualDeviceLevel = ");
                                                        stringBuilder.append(this.mDeviceActualBrightnessLevel);
                                                        stringBuilder.append(", find matched level in XML, load start");
                                                        Slog.i(deviceLevelString, stringBuilder.toString());
                                                    } catch (XmlPullParserException e7) {
                                                        e = e7;
                                                        obj2 = DefaultBrightnessLoaded2;
                                                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                                        DefaultBrighnessLinePointsListLoaded = z;
                                                        CameraBrightnessLinePointsListLoaded2 = CameraBrightnessLinePointsListLoaded;
                                                        Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                                                        Slog.e(TAG, "getConfigFromeXML false!");
                                                        return false;
                                                    } catch (IOException e8) {
                                                        e2 = e8;
                                                        obj2 = DefaultBrightnessLoaded2;
                                                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                                        DefaultBrighnessLinePointsListLoaded = z;
                                                        CameraBrightnessLinePointsListLoaded2 = CameraBrightnessLinePointsListLoaded;
                                                        Slog.e(TAG, "getConfigFromXML : IOException");
                                                        Slog.e(TAG, "getConfigFromeXML false!");
                                                        return false;
                                                    } catch (NumberFormatException e9) {
                                                        e3 = e9;
                                                        obj2 = DefaultBrightnessLoaded2;
                                                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                                        DefaultBrighnessLinePointsListLoaded = z;
                                                        CameraBrightnessLinePointsListLoaded2 = CameraBrightnessLinePointsListLoaded;
                                                        Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                                        Slog.e(TAG, "getConfigFromeXML false!");
                                                        return false;
                                                    }
                                                }
                                                CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                                configGroupLoadStarted = true;
                                            } else {
                                                CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                            }
                                        }
                                        if (DEBUG) {
                                            deviceLevelString = TAG;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("actualDeviceLevel = ");
                                            stringBuilder.append(this.mDeviceActualBrightnessLevel);
                                            stringBuilder.append(", but can't find level in XML, load start");
                                            Slog.i(deviceLevelString, stringBuilder.toString());
                                        }
                                        configGroupLoadStarted = true;
                                    }
                                    name = DefaultBrightnessLoaded2;
                                    DefaultBrighnessLinePointsListLoaded = z;
                                    CameraBrightnessLinePointsListLoaded2 = CameraBrightnessLinePointsListLoaded;
                                    break;
                                } catch (XmlPullParserException e10) {
                                    e = e10;
                                    CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                    obj2 = DefaultBrightnessLoaded2;
                                    parser = DefaultBrighnessLinePointsListsLoadStarted2;
                                    DefaultBrighnessLinePointsListLoaded = z;
                                    Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                                    Slog.e(TAG, "getConfigFromeXML false!");
                                    return false;
                                } catch (IOException e11) {
                                    e2 = e11;
                                    CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                    obj2 = DefaultBrightnessLoaded2;
                                    parser = DefaultBrighnessLinePointsListsLoadStarted2;
                                    DefaultBrighnessLinePointsListLoaded = z;
                                    Slog.e(TAG, "getConfigFromXML : IOException");
                                    Slog.e(TAG, "getConfigFromeXML false!");
                                    return false;
                                } catch (NumberFormatException e12) {
                                    e3 = e12;
                                    CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                    obj2 = DefaultBrightnessLoaded2;
                                    parser = DefaultBrighnessLinePointsListsLoadStarted2;
                                    DefaultBrighnessLinePointsListLoaded = z;
                                    Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                    Slog.e(TAG, "getConfigFromeXML false!");
                                    return false;
                                }
                            } catch (XmlPullParserException e13) {
                                e = e13;
                                CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                parser = DefaultBrighnessLinePointsListsLoadStarted2;
                                DefaultBrighnessLinePointsListLoaded = z;
                                Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                                Slog.e(TAG, "getConfigFromeXML false!");
                                return false;
                            } catch (IOException e14) {
                                e2 = e14;
                                CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                parser = DefaultBrighnessLinePointsListsLoadStarted2;
                                DefaultBrighnessLinePointsListLoaded = z;
                                Slog.e(TAG, "getConfigFromXML : IOException");
                                Slog.e(TAG, "getConfigFromeXML false!");
                                return false;
                            } catch (NumberFormatException e15) {
                                e3 = e15;
                                CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                                parser = DefaultBrighnessLinePointsListsLoadStarted2;
                                DefaultBrighnessLinePointsListLoaded = z;
                                Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                Slog.e(TAG, "getConfigFromeXML false!");
                                return false;
                            }
                            break;
                        case 3:
                            try {
                                String name2 = DefaultBrighnessLinePointsListsLoadStarted2.getName();
                                try {
                                    z = DefaultBrighnessLinePointsListLoaded;
                                    DefaultBrighnessLinePointsListLoaded = name2;
                                    try {
                                        if (!DefaultBrighnessLinePointsListLoaded.equals(XML_NAME_NOEXT) || !configGroupLoadStarted) {
                                            if (!configGroupLoadStarted) {
                                                i = eventType;
                                            } else if (DefaultBrighnessLinePointsListLoaded.equals("DefaultBrightnessPoints")) {
                                                i = eventType;
                                                try {
                                                    if (this.mDefaultBrighnessLinePointsList != 0) {
                                                        DefaultBrighnessLinePointsListsLoadStarted = false;
                                                        obj2 = DefaultBrighnessLinePointsListLoaded;
                                                        DefaultBrighnessLinePointsListLoaded = true;
                                                        break;
                                                    }
                                                    boolean DefaultBrighnessLinePointsListsLoadStarted3 = false;
                                                    try {
                                                        Slog.e(TAG, "no DefaultBrightnessPoints loaded!");
                                                        return false;
                                                    } catch (XmlPullParserException e16) {
                                                        e = e16;
                                                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                                        obj2 = DefaultBrighnessLinePointsListLoaded;
                                                        DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                                        DefaultBrighnessLinePointsListLoaded = z;
                                                        DefaultBrighnessLinePointsListsLoadStarted = DefaultBrighnessLinePointsListsLoadStarted3;
                                                        Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                                                        Slog.e(TAG, "getConfigFromeXML false!");
                                                        return false;
                                                    } catch (IOException e17) {
                                                        e2 = e17;
                                                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                                        obj2 = DefaultBrighnessLinePointsListLoaded;
                                                        DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                                        DefaultBrighnessLinePointsListLoaded = z;
                                                        DefaultBrighnessLinePointsListsLoadStarted = DefaultBrighnessLinePointsListsLoadStarted3;
                                                        Slog.e(TAG, "getConfigFromXML : IOException");
                                                        Slog.e(TAG, "getConfigFromeXML false!");
                                                        return false;
                                                    } catch (NumberFormatException e18) {
                                                        e3 = e18;
                                                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                                        obj2 = DefaultBrighnessLinePointsListLoaded;
                                                        DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                                        DefaultBrighnessLinePointsListLoaded = z;
                                                        DefaultBrighnessLinePointsListsLoadStarted = DefaultBrighnessLinePointsListsLoadStarted3;
                                                        Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                                        Slog.e(TAG, "getConfigFromeXML false!");
                                                        return false;
                                                    }
                                                } catch (XmlPullParserException e19) {
                                                    e = e19;
                                                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                                    obj2 = DefaultBrighnessLinePointsListLoaded;
                                                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                                    DefaultBrighnessLinePointsListLoaded = z;
                                                    DefaultBrighnessLinePointsListsLoadStarted = false;
                                                    Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                                                    Slog.e(TAG, "getConfigFromeXML false!");
                                                    return false;
                                                } catch (IOException e20) {
                                                    e2 = e20;
                                                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                                    obj2 = DefaultBrighnessLinePointsListLoaded;
                                                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                                    DefaultBrighnessLinePointsListLoaded = z;
                                                    DefaultBrighnessLinePointsListsLoadStarted = false;
                                                    Slog.e(TAG, "getConfigFromXML : IOException");
                                                    Slog.e(TAG, "getConfigFromeXML false!");
                                                    return false;
                                                } catch (NumberFormatException e21) {
                                                    e3 = e21;
                                                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                                    obj2 = DefaultBrighnessLinePointsListLoaded;
                                                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                                    DefaultBrighnessLinePointsListLoaded = z;
                                                    DefaultBrighnessLinePointsListsLoadStarted = false;
                                                    Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                                    Slog.e(TAG, "getConfigFromeXML false!");
                                                    return false;
                                                }
                                            } else {
                                                i = eventType;
                                                if (DefaultBrighnessLinePointsListLoaded.equals("CameraBrightnessPoints")) {
                                                    CameraBrightnessLinePointsListsLoadStarted = false;
                                                    if (this.mCameraBrighnessLinePointsList != null) {
                                                        CameraBrightnessLinePointsListLoaded2 = true;
                                                    } else {
                                                        Slog.e(TAG, "no CameraBrightnessPoints loaded!");
                                                        return false;
                                                    }
                                                } else if (DefaultBrighnessLinePointsListLoaded.equals("ReadingBrightnessPoints")) {
                                                    ReadingBrightnessLinePointsListsLoadStarted = false;
                                                    if (this.mReadingBrighnessLinePointsList != null) {
                                                        boolean ReadingBrightnessLinePointsListLoaded = true;
                                                    } else {
                                                        Slog.e(TAG, "no ReadingBrightnessPoints loaded!");
                                                        return false;
                                                    }
                                                } else if (DefaultBrighnessLinePointsListLoaded.equals("GameModeBrightnessPoints")) {
                                                    GameModeBrightnessLinePointsListsLoadStarted = false;
                                                    if (this.mGameModeBrightnessLinePointsList != null) {
                                                        boolean GameModeBrightnessLinePointsListLoaded = true;
                                                    } else {
                                                        Slog.e(TAG, "no GameModeBrightnessPoints loaded!");
                                                        return false;
                                                    }
                                                } else if (DefaultBrighnessLinePointsListLoaded.equals("PowerSavingBrightnessPoints")) {
                                                    PowerSavingBrightnessLinePointsListsLoadStarted = false;
                                                    if (this.mPowerSavingBrighnessLinePointsList != null) {
                                                        boolean PowerSavingBrightnessLinePointsListLoaded = true;
                                                    } else {
                                                        Slog.e(TAG, "no PowerSavingBrightnessPoints loaded!");
                                                        return false;
                                                    }
                                                } else if (DefaultBrighnessLinePointsListLoaded.equals("OminLevelCountLevelLinePoints")) {
                                                    OminLevelCountLevelLinePointsListsLoadStarted = false;
                                                    if (this.mOminLevelCountLevelPointsList != null) {
                                                        boolean OminLevelCountLevelLinePointsListLoaded = true;
                                                    } else {
                                                        Slog.e(TAG, "no OminLevelCountLevelPointsList loaded!");
                                                        return false;
                                                    }
                                                }
                                            }
                                            obj2 = DefaultBrighnessLinePointsListLoaded;
                                            DefaultBrighnessLinePointsListLoaded = z;
                                            break;
                                        }
                                        i = eventType;
                                        loadFinished = true;
                                        name = DefaultBrighnessLinePointsListLoaded;
                                        break;
                                    } catch (XmlPullParserException e22) {
                                        e = e22;
                                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                        obj2 = DefaultBrighnessLinePointsListLoaded;
                                        DefaultBrighnessLinePointsListLoaded = z;
                                        Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                                        Slog.e(TAG, "getConfigFromeXML false!");
                                        return false;
                                    } catch (IOException e23) {
                                        e2 = e23;
                                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                        obj2 = DefaultBrighnessLinePointsListLoaded;
                                        DefaultBrighnessLinePointsListLoaded = z;
                                        Slog.e(TAG, "getConfigFromXML : IOException");
                                        Slog.e(TAG, "getConfigFromeXML false!");
                                        return false;
                                    } catch (NumberFormatException e24) {
                                        e3 = e24;
                                        parser = DefaultBrighnessLinePointsListsLoadStarted2;
                                        obj2 = DefaultBrighnessLinePointsListLoaded;
                                        DefaultBrighnessLinePointsListLoaded = z;
                                        Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                        Slog.e(TAG, "getConfigFromeXML false!");
                                        return false;
                                    }
                                } catch (XmlPullParserException e25) {
                                    e = e25;
                                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                    obj2 = name2;
                                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                    Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                                    Slog.e(TAG, "getConfigFromeXML false!");
                                    return false;
                                } catch (IOException e26) {
                                    e2 = e26;
                                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                    obj2 = name2;
                                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                    Slog.e(TAG, "getConfigFromXML : IOException");
                                    Slog.e(TAG, "getConfigFromeXML false!");
                                    return false;
                                } catch (NumberFormatException e27) {
                                    e3 = e27;
                                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                    obj2 = name2;
                                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                    Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                    Slog.e(TAG, "getConfigFromeXML false!");
                                    return false;
                                }
                            } catch (XmlPullParserException e28) {
                                e = e28;
                                z = DefaultBrighnessLinePointsListLoaded;
                                obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                                Slog.e(TAG, "getConfigFromeXML false!");
                                return false;
                            } catch (IOException e29) {
                                e2 = e29;
                                z = DefaultBrighnessLinePointsListLoaded;
                                obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                Slog.e(TAG, "getConfigFromXML : IOException");
                                Slog.e(TAG, "getConfigFromeXML false!");
                                return false;
                            } catch (NumberFormatException e30) {
                                e3 = e30;
                                z = DefaultBrighnessLinePointsListLoaded;
                                obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                                Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                Slog.e(TAG, "getConfigFromeXML false!");
                                return false;
                            }
                            break;
                        default:
                            break;
                    }
                }
                i = eventType;
                z = DefaultBrighnessLinePointsListLoaded;
                CameraBrightnessLinePointsListLoaded = CameraBrightnessLinePointsListLoaded2;
                DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                boolean DefaultBrightnessLoaded3;
                if (DefaultBrightnessLoaded2 && DefaultBrighnessLinePointsListLoaded) {
                    try {
                        if (DEBUG) {
                            DefaultBrightnessLoaded3 = DefaultBrightnessLoaded2;
                            try {
                                Slog.i(TAG, "getConfigFromeXML success!");
                            } catch (XmlPullParserException e31) {
                                e = e31;
                                obj = DefaultBrighnessLinePointsListsLoadStarted2;
                            } catch (IOException e32) {
                                e2 = e32;
                                obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                Slog.e(TAG, "getConfigFromXML : IOException");
                                Slog.e(TAG, "getConfigFromeXML false!");
                                return false;
                            } catch (NumberFormatException e33) {
                                e3 = e33;
                                obj = DefaultBrighnessLinePointsListsLoadStarted2;
                                Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                                Slog.e(TAG, "getConfigFromeXML false!");
                                return false;
                            }
                        }
                        DefaultBrightnessLoaded3 = DefaultBrightnessLoaded2;
                        return true;
                    } catch (XmlPullParserException e34) {
                        e = e34;
                        DefaultBrightnessLoaded3 = DefaultBrightnessLoaded2;
                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                        Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                        Slog.e(TAG, "getConfigFromeXML false!");
                        return false;
                    } catch (IOException e35) {
                        e2 = e35;
                        DefaultBrightnessLoaded3 = DefaultBrightnessLoaded2;
                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                        Slog.e(TAG, "getConfigFromXML : IOException");
                        Slog.e(TAG, "getConfigFromeXML false!");
                        return false;
                    } catch (NumberFormatException e36) {
                        e3 = e36;
                        DefaultBrightnessLoaded3 = DefaultBrightnessLoaded2;
                        obj = DefaultBrighnessLinePointsListsLoadStarted2;
                        Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                        Slog.e(TAG, "getConfigFromeXML false!");
                        return false;
                    }
                }
                DefaultBrightnessLoaded3 = DefaultBrightnessLoaded2;
                if (configGroupLoadStarted) {
                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded3;
                    Slog.e(TAG, "getConfigFromeXML false!");
                    return false;
                }
                StringBuilder stringBuilder2;
                try {
                    deviceLevelString = TAG;
                    stringBuilder2 = new StringBuilder();
                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                } catch (XmlPullParserException e37) {
                    e = e37;
                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded3;
                    Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                    Slog.e(TAG, "getConfigFromeXML false!");
                    return false;
                } catch (IOException e38) {
                    e2 = e38;
                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded3;
                    Slog.e(TAG, "getConfigFromXML : IOException");
                    Slog.e(TAG, "getConfigFromeXML false!");
                    return false;
                } catch (NumberFormatException e39) {
                    e3 = e39;
                    obj = DefaultBrighnessLinePointsListsLoadStarted2;
                    DefaultBrightnessLoaded2 = DefaultBrightnessLoaded3;
                    Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                    Slog.e(TAG, "getConfigFromeXML false!");
                    return false;
                }
                try {
                    stringBuilder2.append("actualDeviceLevel = ");
                    stringBuilder2.append(this.mDeviceActualBrightnessLevel);
                    stringBuilder2.append(", can't find matched level in XML, load failed!");
                    Slog.e(deviceLevelString, stringBuilder2.toString());
                    return false;
                } catch (XmlPullParserException e40) {
                    e = e40;
                    Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                    Slog.e(TAG, "getConfigFromeXML false!");
                    return false;
                } catch (IOException e41) {
                    e2 = e41;
                    Slog.e(TAG, "getConfigFromXML : IOException");
                    Slog.e(TAG, "getConfigFromeXML false!");
                    return false;
                } catch (NumberFormatException e42) {
                    e3 = e42;
                    Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                    Slog.e(TAG, "getConfigFromeXML false!");
                    return false;
                }
            } catch (XmlPullParserException e43) {
                e = e43;
                obj = DefaultBrighnessLinePointsListsLoadStarted2;
                DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
                Slog.e(TAG, "getConfigFromeXML false!");
                return false;
            } catch (IOException e44) {
                e2 = e44;
                obj = DefaultBrighnessLinePointsListsLoadStarted2;
                DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                Slog.e(TAG, "getConfigFromXML : IOException");
                Slog.e(TAG, "getConfigFromeXML false!");
                return false;
            } catch (NumberFormatException e45) {
                e3 = e45;
                obj = DefaultBrighnessLinePointsListsLoadStarted2;
                DefaultBrightnessLoaded2 = DefaultBrightnessLoaded;
                Slog.e(TAG, "getConfigFromXML : NumberFormatException");
                Slog.e(TAG, "getConfigFromeXML false!");
                return false;
            }
        } catch (XmlPullParserException e46) {
            e = e46;
            DefaultBrightnessLoaded = false;
            DefaultBrighnessLinePointsListsLoadStarted = false;
            parser = parser2;
            Slog.e(TAG, "getConfigFromXML : XmlPullParserException");
            Slog.e(TAG, "getConfigFromeXML false!");
            return false;
        } catch (IOException e47) {
            e2 = e47;
            DefaultBrightnessLoaded = false;
            DefaultBrighnessLinePointsListsLoadStarted = false;
            parser = parser2;
            Slog.e(TAG, "getConfigFromXML : IOException");
            Slog.e(TAG, "getConfigFromeXML false!");
            return false;
        } catch (NumberFormatException e48) {
            e3 = e48;
            DefaultBrightnessLoaded = false;
            DefaultBrighnessLinePointsListsLoadStarted = false;
            parser = parser2;
            Slog.e(TAG, "getConfigFromXML : NumberFormatException");
            Slog.e(TAG, "getConfigFromeXML false!");
            return false;
        }
    }

    public void updateCurrentUserId(int userId) {
        if (DEBUG) {
            Slog.d(TAG, "save old user's paras and load new user's paras when user change ");
        }
        saveOffsetParas();
        this.mCurrentUserId = userId;
        loadOffsetParas();
        unlockDarkAdaptLine();
    }

    public void loadOffsetParas() {
        String str;
        StringBuilder stringBuilder;
        this.mPosBrightnessSaved = System.getFloatForUser(this.mContentResolver, "hw_screen_auto_brightness_adj", 0.0f, this.mCurrentUserId) * maxBrightness;
        this.mPosBrightness = this.mPosBrightnessSaved;
        this.mDeltaSaved = System.getFloatForUser(this.mContentResolver, "spline_delta", 0.0f, this.mCurrentUserId);
        this.mDeltaNew = this.mDeltaSaved;
        boolean z = true;
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
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mOminLevelMode read mOminLevelCount=");
            stringBuilder.append(this.mOminLevelCount);
            stringBuilder.append(",mOminLevelCountResetLongSetTime=");
            stringBuilder.append(this.mOminLevelCountResetLongSetTime);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mManualMode && this.mStartLuxDefaultBrightness >= ((float) this.mManualBrightnessMaxLimit) && this.mPosBrightness == ((float) this.mManualBrightnessMaxLimit)) {
            this.mDelta = 0.0f;
            this.mDeltaNew = 0.0f;
            Slog.i(TAG, "updateLevel outdoor no offset set mDelta=0");
        }
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Read:userId=");
            stringBuilder.append(this.mCurrentUserId);
            stringBuilder.append(",mPosBrightness=");
            stringBuilder.append(this.mPosBrightness);
            stringBuilder.append(",mOffsetBrightness_last=");
            stringBuilder.append(this.mOffsetBrightness_last);
            stringBuilder.append(",mIsUserChange=");
            stringBuilder.append(this.mIsUserChange);
            stringBuilder.append(",mDeltaNew=");
            stringBuilder.append(this.mDeltaNew);
            stringBuilder.append(",mDelta=");
            stringBuilder.append(this.mDelta);
            stringBuilder.append(",mStartLuxDefaultBrightness=");
            stringBuilder.append(this.mStartLuxDefaultBrightness);
            stringBuilder.append(",mLastLuxDefaultBrightness=");
            stringBuilder.append(this.mLastLuxDefaultBrightness);
            stringBuilder.append(",mAmLuxOffset=");
            stringBuilder.append(this.mAmLuxOffset);
            Slog.d(str, stringBuilder.toString());
        }
    }

    private void saveOffsetParas() {
        String str;
        StringBuilder stringBuilder;
        if (((int) (this.mPosBrightness * 10.0f)) != ((int) (this.mPosBrightnessSaved * 10.0f))) {
            System.putFloatForUser(this.mContentResolver, "hw_screen_auto_brightness_adj", this.mPosBrightness / maxBrightness, this.mCurrentUserId);
            this.mPosBrightnessSaved = this.mPosBrightness;
        }
        if (((int) (this.mDeltaNew * 10.0f)) != ((int) (this.mDeltaSaved * 10.0f))) {
            System.putFloatForUser(this.mContentResolver, "spline_delta", this.mDeltaNew, this.mCurrentUserId);
            this.mDeltaSaved = this.mDeltaNew;
        }
        if (this.mIsUserChange != this.mIsUserChangeSaved) {
            ContentResolver contentResolver = this.mContentResolver;
            String str2 = "spline_is_user_change";
            int i = 1;
            if (!this.mIsUserChange) {
                i = 0;
            }
            System.putIntForUser(contentResolver, str2, i, this.mCurrentUserId);
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
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mOminLevelMode saved mOminLevelCount=");
            stringBuilder.append(this.mOminLevelCount);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mOminLevelCountResetLongSetTime != this.mOminLevelCountResetLongSetTimeSaved) {
            System.putIntForUser(this.mContentResolver, "spline_ominlevel_time", this.mOminLevelCountResetLongSetTime, this.mCurrentUserId);
            this.mOminLevelCountResetLongSetTimeSaved = this.mOminLevelCountResetLongSetTime;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mOminLevelMode saved mOminLevelCountResetLongSetTime=");
            stringBuilder.append(this.mOminLevelCountResetLongSetTime);
            Slog.i(str, stringBuilder.toString());
        }
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("write:userId=");
            stringBuilder.append(this.mCurrentUserId);
            stringBuilder.append(",mPosBrightness =");
            stringBuilder.append(this.mPosBrightness);
            stringBuilder.append(",mOffsetBrightness_last=");
            stringBuilder.append(this.mOffsetBrightness_last);
            stringBuilder.append(",mIsUserChange=");
            stringBuilder.append(this.mIsUserChange);
            stringBuilder.append(",mDeltaNew=");
            stringBuilder.append(this.mDeltaNew);
            stringBuilder.append(",mStartLuxDefaultBrightness=");
            stringBuilder.append(this.mStartLuxDefaultBrightness);
            stringBuilder.append("mLastLuxDefaultBrightness=");
            stringBuilder.append(this.mLastLuxDefaultBrightness);
            stringBuilder.append(",mAmLux=");
            stringBuilder.append(this.mAmLux);
            stringBuilder.append(",mAmLuxOffset=");
            stringBuilder.append(this.mAmLuxOffset);
            Slog.d(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("interpolate:mPosBrightness=");
            stringBuilder.append(this.mPosBrightness);
            stringBuilder.append("lux=");
            stringBuilder.append(x);
            stringBuilder.append(",mIsReboot=");
            stringBuilder.append(this.mIsReboot);
            stringBuilder.append(",mIsUserChange=");
            stringBuilder.append(this.mIsUserChange);
            stringBuilder.append(",mDelta=");
            stringBuilder.append(this.mDelta);
            Slog.d(str, stringBuilder.toString());
        }
        float value_interp = getInterpolatedValue(this.mPosBrightness, x) / maxBrightness;
        saveOffsetParas();
        return value_interp;
    }

    public void updateLevelWithLux(float PosBrightness, float lux) {
        String str;
        StringBuilder stringBuilder;
        if (lux < 0.0f) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("error input lux,lux=");
            stringBuilder.append(lux);
            Slog.e(str, stringBuilder.toString());
            return;
        }
        if (!this.mIsReboot) {
            this.mIsUserChange = true;
        }
        this.mAmLuxOffset = lux;
        String str2;
        StringBuilder stringBuilder2;
        if (this.mPersonalizedBrightnessCurveEnable) {
            float defaultBrightness = getCurrentBrightness(lux);
            if (this.mDayModeAlgoEnable && this.mDayModeEnable && getBrightnessMode() == BrightnessModeState.NewCurveMode) {
                float oldBrightness = this.mStartLuxDefaultBrightness;
                this.mStartLuxDefaultBrightness = defaultBrightness > this.mDayModeMinimumBrightness ? defaultBrightness : this.mDayModeMinimumBrightness;
                if (DEBUG && oldBrightness != this.mStartLuxDefaultBrightness) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("updateLevel DayMode: defaultBrightness =");
                    stringBuilder3.append(defaultBrightness);
                    stringBuilder3.append(", mDayModeMinimumBrightness =");
                    stringBuilder3.append(this.mDayModeMinimumBrightness);
                    Slog.d(str3, stringBuilder3.toString());
                }
            } else {
                this.mStartLuxDefaultBrightness = defaultBrightness;
            }
        } else if (this.mDarkAdaptEnable) {
            this.mStartLuxDefaultBrightness = getDefaultBrightnessLevelNew(getCurrentDarkAdaptLine(), lux);
            if (PosBrightness == 0.0f || PosBrightness >= this.mStartLuxDefaultBrightness) {
                this.mDarkAdaptLineLocked = false;
            } else {
                this.mDarkAdaptLineLocked = true;
            }
            if (DEBUG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateLevel DarkAdapt: mDefaultBrightness = ");
                stringBuilder2.append(this.mStartLuxDefaultBrightness);
                stringBuilder2.append(", locked = ");
                stringBuilder2.append(this.mDarkAdaptLineLocked);
                Slog.d(str2, stringBuilder2.toString());
            }
        } else if (this.mOminLevelCountEnable && this.mOminLevelModeEnable) {
            if ((this.mDayModeAlgoEnable && this.mDayModeEnable) || this.mOminLevelDayModeEnable) {
                this.mStartLuxDefaultBrightness = getDefaultBrightnessLevelNew(this.mOminLevelBrighnessLinePointsList, lux);
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateLevel mOminLevelMode:mDefaultBrightness=");
                    stringBuilder2.append(this.mDefaultBrightnessFromLux);
                    Slog.d(str2, stringBuilder2.toString());
                }
            } else {
                this.mStartLuxDefaultBrightness = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, lux);
            }
        } else if (this.mDayModeAlgoEnable && this.mDayModeEnable) {
            this.mStartLuxDefaultBrightness = getDefaultBrightnessLevelNew(this.mDayBrighnessLinePointsList, lux);
            if (DEBUG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateLevel DayMode: mDefaultBrightnessFromLux =");
                stringBuilder2.append(this.mDefaultBrightnessFromLux);
                Slog.d(str2, stringBuilder2.toString());
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
            this.mDarkAdaptLineLocked = false;
            clearGameOffsetDelta();
        }
        if (this.mOminLevelModeEnable) {
            updateOminLevelCount(lux);
        } else {
            this.mOminLevelCountResetLongSetTime = 0;
            this.mOminLevelCount = 0;
        }
        if (this.mVehicleModeEnable && this.mVehicleModeBrightnessEnable && lux < this.mVehicleModeLuxThreshold) {
            this.mVehicleModeClearOffsetEnable = true;
            Slog.i(TAG, "VehicleBrightMode updateLevel in VehicleBrightnessMode");
        }
        if (DEBUG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateLevel:mDelta=");
            stringBuilder.append(this.mDelta);
            stringBuilder.append(",mDeltaNew=");
            stringBuilder.append(this.mDeltaNew);
            stringBuilder.append(",mPosBrightness=");
            stringBuilder.append(this.mPosBrightness);
            stringBuilder.append(",mStartLuxDefaultBrightness=");
            stringBuilder.append(this.mStartLuxDefaultBrightness);
            stringBuilder.append(",lux=");
            stringBuilder.append(lux);
            Slog.d(str, stringBuilder.toString());
        }
        saveOffsetParas();
    }

    public void updateLevelGameWithLux(float PosBrightness, float lux) {
        this.mGameModePosBrightness = PosBrightness;
        if (PosBrightness != 0.0f) {
            this.mGameModeStartLuxDefaultBrightness = getDefaultBrightnessLevelNew(this.mGameModeBrightnessLinePointsList, lux);
            this.mDeltaTmp = PosBrightness - this.mGameModeStartLuxDefaultBrightness;
            this.mGameModeOffsetLux = lux;
        } else {
            clearGameOffsetDelta();
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GameBrightMode updateLevelTmp:mDeltaTmp=");
            stringBuilder.append(this.mDeltaTmp);
            stringBuilder.append(",mGameModePosBrightness=");
            stringBuilder.append(this.mGameModePosBrightness);
            stringBuilder.append(",mGameModeStartLuxDefaultBrightness=");
            stringBuilder.append(this.mGameModeStartLuxDefaultBrightness);
            stringBuilder.append(",lux=");
            stringBuilder.append(lux);
            Slog.d(str, stringBuilder.toString());
        }
    }

    public void setGameCurveLevel(int curveLevel) {
        if (curveLevel == 21) {
            setGameModeEnable(true);
        } else {
            setGameModeEnable(false);
        }
    }

    public void setGameModeEnable(boolean gameModeBrightnessEnable) {
        this.mGameModeBrightnessEnable = gameModeBrightnessEnable;
    }

    public void clearGameOffsetDelta() {
        if (this.mDeltaTmp != 0.0f) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("GameBrightMode updateLevelTmp clearGameOffsetDelta,mDeltaTmp=");
                stringBuilder.append(this.mDeltaTmp);
                stringBuilder.append(",mGameModeOffsetLux=");
                stringBuilder.append(this.mGameModeOffsetLux);
                Slog.d(str, stringBuilder.toString());
            }
            this.mDeltaTmp = 0.0f;
            this.mGameModeOffsetLux = -1.0f;
            this.mGameModePosBrightness = 0.0f;
        }
    }

    public void updateOminLevelCount(float lux) {
        String str;
        int currentMinuteTime = (int) (System.currentTimeMillis() / 60000);
        int deltaMinuteTime = currentMinuteTime - this.mOminLevelCountResetLongSetTime;
        if (deltaMinuteTime >= this.mOminLevelCountResetLongTimeTh || deltaMinuteTime < 0) {
            this.mOminLevelCount = resetOminLevelCount(this.mOminLevelCountLevelPointsList, (float) this.mOminLevelCount);
            this.mOminLevelCountResetLongSetTime = currentMinuteTime;
            if (DEBUG) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mOminLevelMode reset mOminLevelCount=");
                stringBuilder.append(this.mOminLevelCount);
                stringBuilder.append(",deltaMinuteTime=");
                stringBuilder.append(deltaMinuteTime);
                stringBuilder.append(",currenTime=");
                stringBuilder.append(currentMinuteTime);
                Slog.d(str, stringBuilder.toString());
            }
        }
        if (lux >= 0.0f && lux <= ((float) this.mOminLevelCountValidLuxTh)) {
            long currentTime = SystemClock.uptimeMillis();
            float mBrightenDefaultBrightness = getDefaultBrightnessLevelNew(this.mOminLevelBrighnessLinePointsList, lux);
            long deltaTime = currentTime - this.mOminLevelCountSetTime;
            String str2;
            StringBuilder stringBuilder2;
            StringBuilder stringBuilder3;
            if (deltaTime / 1000 >= ((long) this.mOminLevelCountValidTimeTh)) {
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("mOminLevelMode deltaTime=");
                    stringBuilder2.append(deltaTime);
                    stringBuilder2.append(",ValidTime");
                    Slog.d(str2, stringBuilder2.toString());
                }
                if (Math.abs(this.mPosBrightness) < 1.0E-7f) {
                    if (this.mOminLevelCount > 0 && this.mOminLevelOffsetCountEnable) {
                        this.mOminLevelCount--;
                        this.mOminLevelValidCount = 0;
                        this.mOminLevelCountSetTime = currentTime;
                        str = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("mOminLevelMode resetoffset-- count=");
                        stringBuilder3.append(this.mOminLevelCount);
                        Slog.i(str, stringBuilder3.toString());
                    }
                } else if (this.mPosBrightness - mBrightenDefaultBrightness > 0.0f) {
                    if (this.mOminLevelCount < getOminLevelCountThMax(this.mOminLevelCountLevelPointsList)) {
                        this.mOminLevelCount++;
                        this.mOminLevelValidCount = 1;
                        this.mOminLevelCountSetTime = currentTime;
                        str = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("mOminLevelMode brighten++ count=");
                        stringBuilder3.append(this.mOminLevelCount);
                        Slog.i(str, stringBuilder3.toString());
                    }
                } else if (this.mPosBrightness - mBrightenDefaultBrightness < 0.0f && this.mOminLevelCount > 0) {
                    this.mOminLevelCount--;
                    this.mOminLevelValidCount = -1;
                    this.mOminLevelCountSetTime = currentTime;
                    str = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("mOminLevelMode darken-- count=");
                    stringBuilder3.append(this.mOminLevelCount);
                    Slog.i(str, stringBuilder3.toString());
                }
            } else {
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("mOminLevelMode deltaTime=");
                    stringBuilder2.append(deltaTime);
                    Slog.d(str2, stringBuilder2.toString());
                }
                if (Math.abs(this.mPosBrightness) < 1.0E-7f) {
                    if (this.mOminLevelCount > 0 && this.mOminLevelValidCount >= 0 && this.mOminLevelOffsetCountEnable) {
                        this.mOminLevelCount--;
                        this.mOminLevelValidCount--;
                        this.mOminLevelCountSetTime = currentTime;
                        str = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("mOminLevelMode resetoffset-- count=");
                        stringBuilder3.append(this.mOminLevelCount);
                        stringBuilder3.append(",ValidCount=");
                        stringBuilder3.append(this.mOminLevelValidCount);
                        Slog.i(str, stringBuilder3.toString());
                    }
                } else if (this.mPosBrightness - mBrightenDefaultBrightness > 0.0f) {
                    if (this.mOminLevelCount < getOminLevelCountThMax(this.mOminLevelCountLevelPointsList) && this.mOminLevelValidCount <= 0) {
                        this.mOminLevelCount++;
                        this.mOminLevelValidCount++;
                        this.mOminLevelCountSetTime = currentTime;
                        str = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("mOminLevelMode brighten++ count=");
                        stringBuilder3.append(this.mOminLevelCount);
                        stringBuilder3.append(",ValidCount=");
                        stringBuilder3.append(this.mOminLevelValidCount);
                        Slog.i(str, stringBuilder3.toString());
                    }
                } else if (this.mPosBrightness - mBrightenDefaultBrightness < 0.0f && this.mOminLevelCount > 0 && this.mOminLevelValidCount >= 0) {
                    this.mOminLevelCount--;
                    this.mOminLevelValidCount--;
                    this.mOminLevelCountSetTime = currentTime;
                    str = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("mOminLevelMode darken-- count=");
                    stringBuilder3.append(this.mOminLevelCount);
                    stringBuilder3.append(",ValidCount=");
                    stringBuilder3.append(this.mOminLevelValidCount);
                    Slog.i(str, stringBuilder3.toString());
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
            for (Point temp : linePointsListIn) {
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
        if (linePointsListIn.size() > 0) {
            return (int) ((Point) linePointsListIn.get(0)).x;
        }
        return 0;
    }

    private int getOminLevelCountThMax(List<Point> linePointsList) {
        List<Point> linePointsListIn = linePointsList;
        int countMax = 0;
        if (linePointsListIn == null) {
            return 0;
        }
        int listSize = linePointsListIn.size();
        if (listSize > 0) {
            countMax = (int) ((Point) linePointsListIn.get(listSize - 1)).x;
        }
        return countMax;
    }

    private float getOminLevelThMin(List<Point> linePointsList) {
        List<Point> linePointsListIn = linePointsList;
        if (linePointsListIn.size() > 0) {
            return ((Point) linePointsListIn.get(0)).y;
        }
        return minBrightness;
    }

    private float getOminLevelThMax(List<Point> linePointsList) {
        List<Point> linePointsListIn = linePointsList;
        float levelMax = minBrightness;
        if (linePointsListIn == null) {
            return minBrightness;
        }
        int listSize = linePointsListIn.size();
        if (listSize > 0) {
            levelMax = ((Point) linePointsListIn.get(listSize - 1)).y;
        }
        return levelMax;
    }

    public boolean getPowerSavingModeBrightnessChangeEnable(float lux, boolean usePowerSavingModeCurveEnable) {
        boolean powerSavingModeBrightnessChangeEnable = false;
        if (this.mPowerSavingBrighnessLineEnable && lux > this.mPowerSavingAmluxThreshold && this.mUsePowerSavingModeCurveEnable != usePowerSavingModeCurveEnable) {
            float mPowerSavingDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mPowerSavingBrighnessLinePointsList, lux);
            float mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, lux);
            if (((int) mPowerSavingDefaultBrightnessFromLux) != ((int) mDefaultBrightnessFromLux)) {
                powerSavingModeBrightnessChangeEnable = true;
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PowerSavingMode lux=");
                    stringBuilder.append(lux);
                    stringBuilder.append(",usePgEnable=");
                    stringBuilder.append(usePowerSavingModeCurveEnable);
                    stringBuilder.append(",pgBrightness=");
                    stringBuilder.append(mPowerSavingDefaultBrightnessFromLux);
                    stringBuilder.append(",mDefaultBrightness=");
                    stringBuilder.append(mDefaultBrightnessFromLux);
                    Slog.d(str, stringBuilder.toString());
                }
            }
        }
        this.mUsePowerSavingModeCurveEnable = usePowerSavingModeCurveEnable;
        return powerSavingModeBrightnessChangeEnable;
    }

    public void updateNewBrightnessCurve() {
        this.mNewCurveEnable = false;
        if (this.mBrightnessCurveLow.size() > 0) {
            this.mBrightnessCurveLow.clear();
        }
        this.mBrightnessCurveLow = getBrightnessListFromDB("BrightnessCurveLow");
        if (checkBrightnessListIsOK(this.mBrightnessCurveLow)) {
            if (this.mBrightnessCurveMiddle.size() > 0) {
                this.mBrightnessCurveMiddle.clear();
            }
            this.mBrightnessCurveMiddle = getBrightnessListFromDB("BrightnessCurveMiddle");
            if (checkBrightnessListIsOK(this.mBrightnessCurveMiddle)) {
                if (this.mBrightnessCurveHigh.size() > 0) {
                    this.mBrightnessCurveHigh.clear();
                }
                this.mBrightnessCurveHigh = getBrightnessListFromDB("BrightnessCurveHigh");
                if (checkBrightnessListIsOK(this.mBrightnessCurveHigh)) {
                    if (this.mBrightnessCurveDefault.size() > 0) {
                        this.mBrightnessCurveDefault.clear();
                    }
                    this.mBrightnessCurveDefault = getBrightnessListFromDB("BrightnessCurveDefault");
                    if (checkBrightnessListIsOK(this.mBrightnessCurveDefault)) {
                        if (this.mRebootNewCurveEnable) {
                            this.mRebootNewCurveEnable = false;
                            this.mNewCurveEnableTmp = false;
                            this.mNewCurveEnable = true;
                            Slog.i(TAG, "NewCurveMode reboot first updateNewBrightnessCurve success!");
                        }
                        if (this.mNewCurveEnableTmp) {
                            this.mNewCurveEnableTmp = false;
                            this.mNewCurveEnable = true;
                            clearBrightnessOffset();
                            Slog.i(TAG, "NewCurveMode updateNewBrightnessCurve success!");
                        }
                        return;
                    }
                    this.mBrightnessCurveDefault.clear();
                    Slog.w(TAG, "NewCurveMode checkPointsList brightnessList is wrong,tag=BrightnessCurveDefault");
                    return;
                }
                this.mBrightnessCurveHigh.clear();
                Slog.w(TAG, "NewCurveMode checkPointsList brightnessList is wrong,tag=BrightnessCurveHigh");
                return;
            }
            this.mBrightnessCurveMiddle.clear();
            Slog.w(TAG, "NewCurveMode checkPointsList brightnessList is wrong,tag=BrightnessCurveMiddle");
            return;
        }
        this.mBrightnessCurveLow.clear();
        Slog.w(TAG, "NewCurveMode checkPointsList brightnessList is wrong,tag=BrightnessCurveLow");
    }

    public List<Point> getBrightnessListFromDB(String brightnessCurveTag) {
        List<Point> brightnessList = new ArrayList();
        if (this.mManager != null) {
            List<Bundle> records = this.mManager.getAllRecords(brightnessCurveTag, new Bundle());
            if (records != null) {
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < records.size(); i++) {
                    Bundle data = (Bundle) records.get(i);
                    brightnessList.add(new Point(data.getFloat("AmbientLight"), data.getFloat(BrightnessCurveKey.BL)));
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("NewCurveMode brightnessList curve=null,tag=");
                stringBuilder.append(brightnessCurveTag);
                Slog.i(str, stringBuilder.toString());
            }
        }
        return brightnessList;
    }

    private boolean checkBrightnessListIsOK(List<Point> linePointsList) {
        List<Point> linePointsListin = linePointsList;
        if (linePointsListin == null) {
            Slog.e(TAG, "linePointsListin == null");
            return false;
        } else if (linePointsListin.size() <= 2 || linePointsListin.size() >= 100) {
            Slog.e(TAG, "linePointsListin number is wrong");
            return false;
        } else {
            Point lastPoint = null;
            for (Point tmpPoint : linePointsListin) {
                if (lastPoint != null) {
                    String str;
                    StringBuilder stringBuilder;
                    if (lastPoint.x - tmpPoint.x > -1.0E-6f) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("linePointsListin list.y is false, lastPoint.x = ");
                        stringBuilder.append(lastPoint.x);
                        stringBuilder.append(", tmpPoint.x = ");
                        stringBuilder.append(tmpPoint.x);
                        Slog.e(str, stringBuilder.toString());
                        return false;
                    } else if (((int) lastPoint.y) > ((int) tmpPoint.y)) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("linePointsListin check list.y false, lastPoint.y = ");
                        stringBuilder.append(lastPoint.y);
                        stringBuilder.append(", tmpPoint.y = ");
                        stringBuilder.append(tmpPoint.y);
                        Slog.e(str, stringBuilder.toString());
                        return false;
                    }
                }
                lastPoint = tmpPoint;
            }
            return true;
        }
    }

    public void updateNewBrightnessCurveFromTmp() {
        synchronized (mLock) {
            if (this.mNewCurveEnableTmp) {
                this.mNewCurveEnable = false;
                if (this.mBrightnessCurveLow.size() > 0) {
                    this.mBrightnessCurveLow.clear();
                }
                this.mBrightnessCurveLow = cloneList(this.mBrightnessCurveLowTmp);
                if (this.mBrightnessCurveMiddle.size() > 0) {
                    this.mBrightnessCurveMiddle.clear();
                }
                this.mBrightnessCurveMiddle = cloneList(this.mBrightnessCurveMiddleTmp);
                if (this.mBrightnessCurveHigh.size() > 0) {
                    this.mBrightnessCurveHigh.clear();
                }
                this.mBrightnessCurveHigh = cloneList(this.mBrightnessCurveHighTmp);
                if (this.mBrightnessCurveDefault.size() > 0) {
                    this.mBrightnessCurveDefault.clear();
                }
                this.mBrightnessCurveDefault = cloneList(this.mBrightnessCurveDefaultTmp);
                if (this.mNewCurveEnableTmp) {
                    this.mNewCurveEnableTmp = false;
                    this.mNewCurveEnable = true;
                    clearBrightnessOffset();
                    Slog.i(TAG, "NewCurveMode updateNewBrightnessCurve from tmp, success!");
                }
            }
        }
    }

    private List<Point> cloneList(List<Point> list) {
        if (list == null) {
            return null;
        }
        List<Point> newList = new ArrayList();
        for (Point point : list) {
            newList.add(new Point(point.x, point.y));
        }
        return newList;
    }

    public void updateNewBrightnessCurveTmp() {
        this.mNewCurveEnableTmp = false;
        if (this.mPersonalizedBrightnessCurveEnable) {
            if (this.mBrightnessCurveLowTmp.size() > 0) {
                this.mBrightnessCurveLowTmp.clear();
            }
            this.mBrightnessCurveLowTmp = getBrightnessListFromDB("BrightnessCurveLow");
            if (checkBrightnessListIsOK(this.mBrightnessCurveLowTmp)) {
                if (this.mBrightnessCurveMiddleTmp.size() > 0) {
                    this.mBrightnessCurveMiddleTmp.clear();
                }
                this.mBrightnessCurveMiddleTmp = getBrightnessListFromDB("BrightnessCurveMiddle");
                if (checkBrightnessListIsOK(this.mBrightnessCurveMiddleTmp)) {
                    if (this.mBrightnessCurveHighTmp.size() > 0) {
                        this.mBrightnessCurveHighTmp.clear();
                    }
                    this.mBrightnessCurveHighTmp = getBrightnessListFromDB("BrightnessCurveHigh");
                    if (checkBrightnessListIsOK(this.mBrightnessCurveHighTmp)) {
                        if (this.mBrightnessCurveDefaultTmp.size() > 0) {
                            this.mBrightnessCurveDefaultTmp.clear();
                        }
                        this.mBrightnessCurveDefaultTmp = getBrightnessListFromDB("BrightnessCurveDefault");
                        if (checkBrightnessListIsOK(this.mBrightnessCurveDefaultTmp)) {
                            this.mNewCurveEnableTmp = true;
                            if (DEBUG) {
                                Slog.d(TAG, "NewCurveMode updateNewBrightnessCurveTmp success!");
                            }
                            if (!this.mPowerOnEanble) {
                                updateNewBrightnessCurveFromTmp();
                            }
                            return;
                        }
                        this.mBrightnessCurveDefaultTmp.clear();
                        Slog.w(TAG, "NewCurveMode checkPointsList brightnessList is wrong,tag=BrightnessCurveDefault");
                        return;
                    }
                    this.mBrightnessCurveHighTmp.clear();
                    Slog.w(TAG, "NewCurveMode checkPointsList brightnessList is wrong,tag=BrightnessCurveHigh");
                    return;
                }
                this.mBrightnessCurveMiddleTmp.clear();
                Slog.w(TAG, "NewCurveMode checkPointsList brightnessList is wrong,tag=BrightnessCurveMiddle");
                return;
            }
            this.mBrightnessCurveLowTmp.clear();
            Slog.w(TAG, "NewCurveMode checkPointsList brightnessList is wrong,tag=BrightnessCurveLow");
            return;
        }
        Slog.i(TAG, "mPersonalizedBrightnessCurveEnable=false,not updateBrightness");
    }

    public List<Short> getPersonalizedDefaultCurve() {
        if (this.mBrightnessCurveDefaultTmp.isEmpty()) {
            return null;
        }
        List<Short> curveList = new ArrayList();
        for (Point point : this.mBrightnessCurveDefaultTmp) {
            short bright = Math.round(point.y);
            short level = Short.MAX_VALUE;
            if (bright < Short.MAX_VALUE) {
                level = bright;
            }
            curveList.add(Short.valueOf((short) level));
        }
        return curveList;
    }

    public List<Float> getPersonalizedAlgoParam() {
        if (this.mManager == null) {
            return null;
        }
        List<Bundle> records = this.mManager.getAllRecords("AlgorithmESCW", new Bundle());
        if (records == null || records.isEmpty()) {
            return null;
        }
        List<Float> algoParamList = new ArrayList();
        for (Bundle bundle : records) {
            algoParamList.add(Float.valueOf(bundle.getFloat(AlgorithmESCWKey.ESCW)));
        }
        return algoParamList;
    }

    public void setPersonalizedBrightnessCurveLevel(int curveLevel) {
        if (this.mCurveLevel != curveLevel) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NewCurveMode setPersonalizedBrightnessCurveLevel curveLevel=");
            stringBuilder.append(curveLevel);
            Slog.i(str, stringBuilder.toString());
        }
        this.mCurveLevel = curveLevel;
    }

    public void setSceneCurveLevel(int curveLevel) {
        if (this.mVehicleModeEnable) {
            if (curveLevel == 19) {
                this.mVehicleModeBrightnessEnable = true;
                this.mVehicleModeQuitForPowerOnEnable = false;
            } else if (curveLevel == 18) {
                this.mVehicleModeQuitForPowerOnEnable = true;
            }
            if (DEBUG && this.mSceneLevel != curveLevel && (curveLevel == 19 || curveLevel == 18)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("VehicleBrightMode curveLevel=");
                stringBuilder.append(curveLevel);
                stringBuilder.append(",VEnable=");
                stringBuilder.append(this.mVehicleModeBrightnessEnable);
                Slog.d(str, stringBuilder.toString());
            }
            this.mSceneLevel = curveLevel;
        }
    }

    public boolean getVehicleModeQuitForPowerOnEnable() {
        return this.mVehicleModeQuitForPowerOnEnable;
    }

    public boolean getVehicleModeBrightnessEnable() {
        return this.mVehicleModeBrightnessEnable;
    }

    public void setVehicleModeQuitEnable() {
        if (this.mVehicleModeBrightnessEnable) {
            this.mVehicleModeBrightnessEnable = false;
            this.mVehicleModeQuitForPowerOnEnable = false;
            if (this.mVehicleModeClearOffsetEnable) {
                this.mVehicleModeClearOffsetEnable = false;
                clearBrightnessOffset();
                Slog.i(TAG, "VehicleBrightMode clear brightnessOffset");
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("VehicleBrightMode set mVehicleModeBrightnessEnable=");
                stringBuilder.append(this.mVehicleModeBrightnessEnable);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    public boolean getNewCurveEableTmp() {
        return this.mNewCurveEnableTmp;
    }

    public boolean getNewCurveEable() {
        return this.mNewCurveEnable;
    }

    public void setPowerStatus(boolean powerOnEanble) {
        if (this.mPowerOnEanble != powerOnEanble) {
            this.mPowerOnEanble = powerOnEanble;
        }
    }

    public boolean setNewCurveEnable(boolean enable) {
        if (!enable) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NewCurveMode updateNewBrightnessCurveReal starting..,mNewCurveEnable=");
        stringBuilder.append(this.mNewCurveEnable);
        stringBuilder.append(",mNewCurveEnableTmp=");
        stringBuilder.append(this.mNewCurveEnableTmp);
        Slog.i(str, stringBuilder.toString());
        updateNewBrightnessCurveFromTmp();
        return true;
    }

    public void clearBrightnessOffset() {
        if (Math.abs(this.mPosBrightness) > 1.0E-7f) {
            this.mPosBrightness = 0.0f;
            this.mDelta = 0.0f;
            this.mDeltaNew = 0.0f;
            this.mIsUserChange = false;
            this.mAmLuxOffset = -1.0f;
            saveOffsetParas();
            if (DEBUG) {
                Slog.d(TAG, "NewCurveMode clear tmp brighntess offset");
            }
        }
    }

    private float getCurrentBrightness(float lux) {
        List<Point> brightnessList = new ArrayList();
        switch (getBrightnessMode()) {
            case CameraMode:
                brightnessList = this.mCameraBrighnessLinePointsList;
                break;
            case ReadingMode:
                brightnessList = this.mReadingBrighnessLinePointsList;
                break;
            case GameMode:
                brightnessList = this.mGameModeBrightnessLinePointsList;
                break;
            case NewCurveMode:
                brightnessList = getCurrentNewCureLine();
                break;
            case PowerSavingMode:
                brightnessList = this.mPowerSavingBrighnessLinePointsList;
                break;
            case EyeProtectionMode:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("NewCurveMode eye mode=");
                stringBuilder.append(getBrightnessMode());
                Slog.i(str, stringBuilder.toString());
                return this.mEyeProtectionSpline.getEyeProtectionBrightnessLevel(lux);
            case CalibrtionMode:
                brightnessList = this.mDefaultBrighnessLinePointsListCaliBefore;
                break;
            case DarkAdaptMode:
                brightnessList = getCurrentDarkAdaptLine();
                break;
            case OminLevelMode:
                brightnessList = this.mOminLevelBrighnessLinePointsList;
                break;
            case DayMode:
                brightnessList = this.mDayBrighnessLinePointsList;
                break;
            default:
                brightnessList = this.mDefaultBrighnessLinePointsList;
                break;
        }
        if (brightnessList == null || brightnessList.size() == 0) {
            brightnessList = this.mDefaultBrighnessLinePointsList;
            Slog.i(TAG, "NewCurveMode brightnessList null,set mDefaultBrighnessLinePointsList");
        }
        float brightness = getDefaultBrightnessLevelNew(brightnessList, lux);
        if (this.mVehicleModeEnable && this.mVehicleModeBrightnessEnable && lux < this.mVehicleModeLuxThreshold) {
            brightness = brightness > this.mVehicleModeBrighntess ? brightness : this.mVehicleModeBrighntess;
        }
        if (this.mBrightnessForLog != brightness) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("NewCurveMode mode=");
            stringBuilder2.append(getBrightnessMode());
            stringBuilder2.append(",brightness=");
            stringBuilder2.append(getDefaultBrightnessLevelNew(brightnessList, lux));
            stringBuilder2.append(",lux=");
            stringBuilder2.append(lux);
            stringBuilder2.append(",mPis=");
            stringBuilder2.append(this.mPosBrightness);
            stringBuilder2.append(",eyeanble=");
            stringBuilder2.append(this.mEyeProtectionSpline.isEyeProtectionMode());
            stringBuilder2.append(",mVehicleModeEnable=");
            stringBuilder2.append(this.mVehicleModeBrightnessEnable);
            Slog.i(str2, stringBuilder2.toString());
        }
        this.mBrightnessForLog = brightness;
        return brightness;
    }

    private List<Point> getCurrentNewCureLine() {
        if (this.mNewCurveEnable) {
            int currentCurveLevel = getCurrentCurveLevel();
            if (DEBUG && currentCurveLevel != this.mCurrentCurveLevel) {
                String str;
                StringBuilder stringBuilder;
                if (currentCurveLevel == 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("NewCurveMode mBrightnessCurveLow NewCurveMode=");
                    stringBuilder.append(this.mCurveLevel);
                    Slog.i(str, stringBuilder.toString());
                } else if (currentCurveLevel == 1) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("NewCurveMode mBrightnessCurveMiddle NewCurveMode=");
                    stringBuilder.append(this.mCurveLevel);
                    Slog.i(str, stringBuilder.toString());
                } else if (currentCurveLevel == 2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("NewCurveMode mBrightnessCurveHigh NewCurveMode=");
                    stringBuilder.append(this.mCurveLevel);
                    Slog.i(str, stringBuilder.toString());
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("NewCurveMode defualt NewCurveMode=");
                    stringBuilder.append(this.mCurveLevel);
                    Slog.i(str, stringBuilder.toString());
                }
                this.mCurrentCurveLevel = currentCurveLevel;
            }
            if (currentCurveLevel == 0) {
                return this.mBrightnessCurveLow;
            }
            if (currentCurveLevel == 1) {
                return this.mBrightnessCurveMiddle;
            }
            if (currentCurveLevel == 2) {
                return this.mBrightnessCurveHigh;
            }
            return this.mBrightnessCurveDefault;
        }
        Slog.i(TAG, "NewCurveMode NewCurveMode=false,return mDefaultBrighnessLinePointsList");
        return this.mDefaultBrighnessLinePointsList;
    }

    private int getCurrentCurveLevel() {
        return this.mCurveLevel;
    }

    public List<PointF> getCurrentDefaultNewCurveLine() {
        List<PointF> brightnessList = new ArrayList();
        for (int i = 0; i < this.mLuxPonits.length; i++) {
            float brightness;
            if (this.mDayModeAlgoEnable) {
                brightness = getDefaultBrightnessLevelNew(this.mDayBrighnessLinePointsList, this.mLuxPonits[i]);
            } else {
                brightness = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, this.mLuxPonits[i]);
            }
            brightnessList.add(new PointF(this.mLuxPonits[i], brightness));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NewCurveMode getCurrentDefaultNewCurveLine,mDayModeAlgoEnable=");
        stringBuilder.append(this.mDayModeAlgoEnable);
        Slog.i(str, stringBuilder.toString());
        return brightnessList;
    }

    public boolean getPersonalizedBrightnessCurveEnable() {
        if (this.mNewCurveEnable) {
            return this.mPersonalizedBrightnessCurveEnable;
        }
        return false;
    }

    public float getDefaultBrightness(float lux) {
        return getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, lux);
    }

    public float getNewDefaultBrightness(float lux) {
        List<Point> brightnessList = new ArrayList();
        if (getBrightnessMode() == BrightnessModeState.NewCurveMode) {
            brightnessList = this.mBrightnessCurveDefault;
        } else {
            brightnessList = this.mDefaultBrighnessLinePointsList;
        }
        return getDefaultBrightnessLevelNew(brightnessList, lux);
    }

    public float getNewCurrentBrightness(float lux) {
        List<Point> brightnessList = new ArrayList();
        if (getBrightnessMode() == BrightnessModeState.NewCurveMode) {
            brightnessList = getCurrentNewCureLine();
        } else {
            brightnessList = this.mDefaultBrighnessLinePointsList;
        }
        return getDefaultBrightnessLevelNew(brightnessList, lux);
    }

    public BrightnessModeState getBrightnessMode() {
        if (this.mCameraModeEnable) {
            return BrightnessModeState.CameraMode;
        }
        if (this.mReadingModeEnable) {
            return BrightnessModeState.ReadingMode;
        }
        if (this.mGameModeEnable && this.mGameModeBrightnessEnable) {
            return BrightnessModeState.GameMode;
        }
        if (this.mNewCurveEnable) {
            return BrightnessModeState.NewCurveMode;
        }
        if (this.mPowerSavingModeEnable && this.mPowerSavingBrighnessLineEnable && this.mAmLux > this.mPowerSavingAmluxThreshold) {
            return BrightnessModeState.PowerSavingMode;
        }
        if (this.mEyeProtectionSpline != null && this.mEyeProtectionSpline.isEyeProtectionMode() && this.mEyeProtectionSplineEnable) {
            return BrightnessModeState.EyeProtectionMode;
        }
        if (this.mCalibrtionModeBeforeEnable) {
            return BrightnessModeState.CalibrtionMode;
        }
        if (this.mDarkAdaptEnable) {
            return BrightnessModeState.DarkAdaptMode;
        }
        if (this.mOminLevelCountEnable && this.mOminLevelModeEnable) {
            return BrightnessModeState.OminLevelMode;
        }
        if (this.mDayModeAlgoEnable && this.mDayModeEnable) {
            return BrightnessModeState.DayMode;
        }
        return BrightnessModeState.DefaultMode;
    }

    public boolean getPowerSavingBrighnessLineEnable() {
        return this.mPowerSavingBrighnessLineEnable;
    }

    public float getInterpolatedValue(float PositionBrightness, float lux) {
        float defaultBrightness;
        String str;
        StringBuilder stringBuilder;
        float PosBrightness = PositionBrightness;
        boolean inDarkAdaptMode = false;
        String str2;
        StringBuilder stringBuilder2;
        if (this.mPersonalizedBrightnessCurveEnable) {
            defaultBrightness = getCurrentBrightness(lux);
            if (this.mDayModeAlgoEnable && this.mDayModeEnable && getBrightnessMode() == BrightnessModeState.NewCurveMode) {
                float oldBrightness = this.mDefaultBrightnessFromLux;
                this.mDefaultBrightnessFromLux = defaultBrightness > this.mDayModeMinimumBrightness ? defaultBrightness : this.mDayModeMinimumBrightness;
                if (DEBUG && oldBrightness != this.mDefaultBrightnessFromLux) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("getInterpolatedValue DayMode: defaultBrightness =");
                    stringBuilder3.append(defaultBrightness);
                    stringBuilder3.append(", mDayModeMinimumBrightness =");
                    stringBuilder3.append(this.mDayModeMinimumBrightness);
                    Slog.d(str3, stringBuilder3.toString());
                }
            } else {
                this.mDefaultBrightnessFromLux = defaultBrightness;
            }
        } else if (!this.mReadingModeEnable && this.mPowerSavingModeEnable && this.mPowerSavingBrighnessLineEnable && this.mAmLux > this.mPowerSavingAmluxThreshold) {
            this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mPowerSavingBrighnessLinePointsList, lux);
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("PowerSavingMode defualtbrightness=");
            stringBuilder2.append(this.mDefaultBrightnessFromLux);
            stringBuilder2.append(",lux=");
            stringBuilder2.append(lux);
            stringBuilder2.append(",mCalibrationRatio=");
            stringBuilder2.append(this.mCalibrationRatio);
            Slog.i(str2, stringBuilder2.toString());
        } else if (this.mCameraModeEnable) {
            this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mCameraBrighnessLinePointsList, lux);
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CameraMode defualtbrightness=");
            stringBuilder2.append(this.mDefaultBrightnessFromLux);
            stringBuilder2.append(",lux=");
            stringBuilder2.append(lux);
            Slog.i(str2, stringBuilder2.toString());
        } else if (this.mReadingModeEnable) {
            this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mReadingBrighnessLinePointsList, lux);
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("ReadingMode defaultbrightness=");
            stringBuilder2.append(this.mDefaultBrightnessFromLux);
            stringBuilder2.append(",lux=");
            stringBuilder2.append(lux);
            Slog.i(str2, stringBuilder2.toString());
        } else if (this.mGameModeEnable && this.mGameModeBrightnessEnable) {
            this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mGameModeBrightnessLinePointsList, lux);
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("GameBrightMode defaultbrightness=");
            stringBuilder2.append(this.mDefaultBrightnessFromLux);
            stringBuilder2.append(",lux=");
            stringBuilder2.append(lux);
            Slog.i(str2, stringBuilder2.toString());
        } else if (this.mEyeProtectionSpline != null && this.mEyeProtectionSpline.isEyeProtectionMode() && this.mEyeProtectionSplineEnable) {
            this.mDefaultBrightnessFromLux = this.mEyeProtectionSpline.getEyeProtectionBrightnessLevel(lux);
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getEyeProtectionBrightnessLevel lux =");
            stringBuilder2.append(lux);
            stringBuilder2.append(", mDefaultBrightnessFromLux =");
            stringBuilder2.append(this.mDefaultBrightnessFromLux);
            Slog.i(str2, stringBuilder2.toString());
        } else {
            if (this.mCalibrtionModeBeforeEnable) {
                this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsListCaliBefore, lux);
            } else if (this.mDarkAdaptEnable) {
                inDarkAdaptMode = true;
                updateDarkAdaptState();
                this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(getCurrentDarkAdaptLine(), lux);
            } else if (this.mOminLevelCountEnable && this.mOminLevelModeEnable) {
                if ((this.mDayModeAlgoEnable && this.mDayModeEnable) || this.mOminLevelDayModeEnable) {
                    this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mOminLevelBrighnessLinePointsList, lux);
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("mOminLevelMode:Day getBrightnessLevel lux =");
                    stringBuilder2.append(lux);
                    stringBuilder2.append(",mDefaultBrightness=");
                    stringBuilder2.append(this.mDefaultBrightnessFromLux);
                    stringBuilder2.append(",mOCount=");
                    stringBuilder2.append(this.mOminLevelCount);
                    Slog.i(str2, stringBuilder2.toString());
                } else {
                    this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, lux);
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("mOminLevelMode:night getBrightnessLevel lux =");
                    stringBuilder2.append(lux);
                    stringBuilder2.append(",mDefaultBrightness=");
                    stringBuilder2.append(this.mDefaultBrightnessFromLux);
                    stringBuilder2.append(",mOCount=");
                    stringBuilder2.append(this.mOminLevelCount);
                    Slog.i(str2, stringBuilder2.toString());
                }
            } else if (this.mDayModeAlgoEnable && this.mDayModeEnable) {
                this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mDayBrighnessLinePointsList, lux);
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DayMode:getBrightnessLevel lux =");
                stringBuilder2.append(lux);
                stringBuilder2.append(", mDefaultBrightnessFromLux =");
                stringBuilder2.append(this.mDefaultBrightnessFromLux);
                Slog.i(str2, stringBuilder2.toString());
            } else {
                this.mDefaultBrightnessFromLux = getDefaultBrightnessLevelNew(this.mDefaultBrighnessLinePointsList, lux);
            }
            if (DEBUG && this.mEyeProtectionSpline != null && this.mEyeProtectionSpline.isEyeProtectionMode() && !this.mEyeProtectionSplineEnable) {
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
        defaultBrightness = this.mDefaultBrightnessFromLux;
        if (!(this.mGameModeEnable && this.mGameModeBrightnessEnable && this.mDeltaTmp == 0.0f) && ((this.mGameModeBrightnessEnable || PosBrightness != 0.0f) && !this.mCoverModeNoOffsetEnable)) {
            defaultBrightness = (this.mGameModeEnable && this.mGameModeBrightnessEnable) ? getOffsetBrightnessLevel_withDelta(this.mGameModeStartLuxDefaultBrightness, this.mDefaultBrightnessFromLux, this.mGameModePosBrightness, this.mDeltaTmp) : inDarkAdaptMode ? getDarkAdaptOffset(PosBrightness, lux) : getOffsetBrightnessLevel_new(this.mStartLuxDefaultBrightness, this.mDefaultBrightnessFromLux, PosBrightness);
        } else {
            defaultBrightness = this.mDefaultBrightnessFromLux;
            if (this.mCoverModeNoOffsetEnable) {
                this.mCoverModeNoOffsetEnable = false;
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("set mCoverModeNoOffsetEnable=");
                    stringBuilder.append(this.mCoverModeNoOffsetEnable);
                    Slog.i(str, stringBuilder.toString());
                }
            }
        }
        if (DEBUG && ((int) defaultBrightness) != ((int) this.mOffsetBrightness_last)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("offsetBrightness=");
            stringBuilder.append(defaultBrightness);
            stringBuilder.append(",mOffsetBrightness_last");
            stringBuilder.append(this.mOffsetBrightness_last);
            stringBuilder.append(",lux=");
            stringBuilder.append(lux);
            stringBuilder.append(",mPosBrightness=");
            stringBuilder.append(this.mPosBrightness);
            stringBuilder.append(",mIsUserChange=");
            stringBuilder.append(this.mIsUserChange);
            stringBuilder.append(",mDelta=");
            stringBuilder.append(this.mDelta);
            stringBuilder.append(",mDefaultBrightnessFromLux=");
            stringBuilder.append(this.mDefaultBrightnessFromLux);
            stringBuilder.append(",mStartLuxDefaultBrightness=");
            stringBuilder.append(this.mStartLuxDefaultBrightness);
            stringBuilder.append("mLastLuxDefaultBrightness=");
            stringBuilder.append(this.mLastLuxDefaultBrightness);
            Slog.d(str, stringBuilder.toString());
        }
        if (DEBUG && this.mGameModeBrightnessEnable && ((int) defaultBrightness) != ((int) this.mOffsetBrightness_last)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("GameBrightMode mGameModeStartLuxDefaultBrightness=");
            stringBuilder.append(this.mGameModeStartLuxDefaultBrightness);
            stringBuilder.append(",offsetBrightness=");
            stringBuilder.append(defaultBrightness);
            stringBuilder.append(",mDeltaTmp=");
            stringBuilder.append(this.mDeltaTmp);
            stringBuilder.append(",mGameModePosBrightness=");
            stringBuilder.append(this.mGameModePosBrightness);
            stringBuilder.append(",mGameModeOffsetLux=");
            stringBuilder.append(this.mGameModeOffsetLux);
            Slog.i(str, stringBuilder.toString());
        }
        this.mLastLuxDefaultBrightness = this.mDefaultBrightnessFromLux;
        this.mOffsetBrightness_last = defaultBrightness;
        return defaultBrightness;
    }

    public float getDefaultBrightnessLevelNew(List<Point> linePointsList, float lux) {
        List<Point> linePointsListIn = linePointsList;
        int count = 0;
        float brightnessLevel = this.mDefaultBrightness;
        Point temp1 = null;
        for (Point temp : linePointsListIn) {
            if (count == 0) {
                temp1 = temp;
            }
            if (lux < temp.x) {
                Point temp2 = temp;
                if (temp2.x > temp1.x) {
                    return (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (lux - temp1.x)) + temp1.y;
                }
                brightnessLevel = this.mDefaultBrightness;
                if (!DEBUG) {
                    return brightnessLevel;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DefaultBrighness_temp1.x <= temp2.x,x");
                stringBuilder.append(temp.x);
                stringBuilder.append(", y = ");
                stringBuilder.append(temp.y);
                Slog.i(str, stringBuilder.toString());
                return brightnessLevel;
            }
            temp1 = temp;
            brightnessLevel = temp1.y;
            count++;
        }
        return brightnessLevel;
    }

    float getOffsetBrightnessLevel_new(float brightnessStartOrig, float brightnessEndOrig, float brightnessStartNew) {
        return getOffsetBrightnessLevel_withDelta(brightnessStartOrig, brightnessEndOrig, brightnessStartNew, this.mDelta);
    }

    float getOffsetBrightnessLevel_withDelta(float brightnessStartOrig, float brightnessEndOrig, float brightnessStartNew, float delta) {
        float ratio2;
        float ratio22;
        if (this.mIsUserChange) {
            this.mIsUserChange = false;
        }
        float ratio = 1.0f;
        float ratio23 = 1.0f;
        float mDeltaStart = delta;
        if (brightnessStartOrig < brightnessEndOrig) {
            if (mDeltaStart > 0.0f) {
                ratio2 = (((-mDeltaStart) * (Math.abs(brightnessStartOrig - brightnessEndOrig) / (mDeltaStart + 1.0E-7f))) / ((maxBrightness - brightnessStartOrig) + 1.0E-7f)) + 1.0f;
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Orig_ratio2=");
                    stringBuilder.append(ratio2);
                    stringBuilder.append(",mOffsetBrightenAlphaRight=");
                    stringBuilder.append(this.mOffsetBrightenAlphaRight);
                    Slog.i(str, stringBuilder.toString());
                }
                ratio22 = ((((1.0f - this.mOffsetBrightenAlphaRight) * Math.max(brightnessEndOrig, brightnessStartNew)) + (this.mOffsetBrightenAlphaRight * ((mDeltaStart * ratio2) + brightnessEndOrig))) - brightnessEndOrig) / (mDeltaStart + 1.0E-7f);
                if (ratio22 < 0.0f) {
                    ratio23 = 0.0f;
                } else {
                    ratio23 = ratio22;
                }
            }
            if (mDeltaStart < 0.0f) {
                ratio = (((-mDeltaStart) * (Math.abs(brightnessStartOrig - brightnessEndOrig) / (mDeltaStart - 1.0E-7f))) / ((maxBrightness - brightnessStartOrig) + 1.0E-7f)) + 1.0f;
                if (ratio < 0.0f) {
                    ratio = 0.0f;
                }
            }
        }
        int i = (brightnessStartOrig > brightnessEndOrig ? 1 : (brightnessStartOrig == brightnessEndOrig ? 0 : -1));
        ratio22 = minBrightness;
        if (i > 0) {
            if (mDeltaStart < 0.0f) {
                float ratio24 = ((((1.0f - this.mOffsetDarkenAlphaLeft) * Math.min(brightnessEndOrig, brightnessStartNew)) + (this.mOffsetDarkenAlphaLeft * ((mDeltaStart * (((mDeltaStart * (Math.abs(brightnessStartOrig - brightnessEndOrig) / (mDeltaStart - 1.0E-7f))) / ((minBrightness - brightnessStartOrig) - 1.0E-7f)) + 1.0f)) + brightnessEndOrig))) - brightnessEndOrig) / (mDeltaStart - 1.0E-7f);
                if (ratio24 < 0.0f) {
                    ratio23 = 0.0f;
                } else {
                    ratio23 = ratio24;
                }
            }
            if (mDeltaStart > 0.0f) {
                ratio2 = (float) Math.pow((double) (brightnessEndOrig / (brightnessStartOrig + 1.0E-7f)), (double) this.mOffsetBrightenRatioLeft);
                ratio = ((this.mOffsetBrightenAlphaLeft * brightnessEndOrig) / (brightnessStartOrig + 1.0E-7f)) + ((1.0f - this.mOffsetBrightenAlphaLeft) * ratio2);
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ratio=");
                    stringBuilder2.append(ratio);
                    stringBuilder2.append(",ratioTmp=");
                    stringBuilder2.append(ratio2);
                    stringBuilder2.append(",mOffsetBrightenAlphaLeft=");
                    stringBuilder2.append(this.mOffsetBrightenAlphaLeft);
                    Slog.d(str2, stringBuilder2.toString());
                }
            }
        }
        this.mDeltaNew = (mDeltaStart * ratio23) * ratio;
        if (DEBUG) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("mDeltaNew=");
            stringBuilder3.append(this.mDeltaNew);
            stringBuilder3.append(",mDeltaStart=");
            stringBuilder3.append(mDeltaStart);
            stringBuilder3.append(",ratio2=");
            stringBuilder3.append(ratio23);
            stringBuilder3.append(",ratio=");
            stringBuilder3.append(ratio);
            Slog.d(str3, stringBuilder3.toString());
        }
        ratio2 = brightnessEndOrig + this.mDeltaNew;
        if (ratio2 > minBrightness) {
            ratio22 = ratio2;
        }
        float offsetBrightnessTemp = ratio22;
        return offsetBrightnessTemp < maxBrightness ? offsetBrightnessTemp : maxBrightness;
    }

    public float getAmbientValueFromDB() {
        String str;
        StringBuilder stringBuilder;
        float ambientValue = System.getFloatForUser(this.mContentResolver, "spline_ambient_lux", 100.0f, this.mCurrentUserId);
        if (((int) ambientValue) < 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("error inputValue<min,ambientValue=");
            stringBuilder.append(ambientValue);
            Slog.e(str, stringBuilder.toString());
            ambientValue = (float) null;
        }
        if (((int) ambientValue) <= 40000) {
            return ambientValue;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("error inputValue>max,ambientValue=");
        stringBuilder.append(ambientValue);
        Slog.e(str, stringBuilder.toString());
        return (float) 40000;
    }

    public boolean getCalibrationTestEable() {
        boolean calibrationTestEable = false;
        int calibrtionTest = System.getIntForUser(this.mContentResolver, "spline_calibration_test", 0, this.mCurrentUserId);
        if (calibrtionTest == 0) {
            this.mCalibrtionModeBeforeEnable = false;
            return false;
        }
        int calibrtionTestLow = calibrtionTest & 65535;
        int calibrtionTestHigh = 65535 & (calibrtionTest >> 16);
        if (calibrtionTestLow != calibrtionTestHigh) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error db, clear DB,,calibrtionTestLow=");
            stringBuilder.append(calibrtionTestLow);
            stringBuilder.append(",calibrtionTestHigh=");
            stringBuilder.append(calibrtionTestHigh);
            Slog.e(str, stringBuilder.toString());
            System.putIntForUser(this.mContentResolver, "spline_calibration_test", 0, this.mCurrentUserId);
            this.mCalibrtionModeBeforeEnable = false;
            return false;
        }
        int calibrtionModeBeforeEnableInt = (calibrtionTestLow >> 1) & 1;
        int calibrationTestEnableInt = calibrtionTestLow & 1;
        if (calibrtionModeBeforeEnableInt == 1) {
            this.mCalibrtionModeBeforeEnable = true;
        } else {
            this.mCalibrtionModeBeforeEnable = false;
        }
        if (calibrationTestEnableInt == 1) {
            calibrationTestEable = true;
        }
        if (calibrtionTest != this.mCalibrtionTest) {
            this.mCalibrtionTest = calibrtionTest;
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mCalibrtionTest=");
                stringBuilder2.append(this.mCalibrtionTest);
                stringBuilder2.append(",calibrationTestEnableInt=");
                stringBuilder2.append(calibrationTestEnableInt);
                stringBuilder2.append(",calibrtionModeBeforeEnableInt=");
                stringBuilder2.append(calibrtionModeBeforeEnableInt);
                Slog.d(str2, stringBuilder2.toString());
            }
        }
        return calibrationTestEable;
    }

    public void setEyeProtectionControlFlag(boolean inControlTime) {
        if (this.mEyeProtectionSpline != null) {
            this.mEyeProtectionSpline.setEyeProtectionControlFlag(inControlTime);
        }
    }

    public void setReadingModeEnable(boolean readingModeEnable) {
        this.mReadingModeEnable = readingModeEnable;
    }

    public void setNoOffsetEnable(boolean noOffsetEnable) {
        this.mCoverModeNoOffsetEnable = noOffsetEnable;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LabcCoverMode CoverModeNoOffsetEnable=");
            stringBuilder.append(this.mCoverModeNoOffsetEnable);
            Slog.i(str, stringBuilder.toString());
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

    public float getGameModeAmbientLuxForOffset() {
        return this.mGameModeOffsetLux;
    }

    public void setDayModeEnable(boolean dayModeEnable) {
        this.mDayModeEnable = dayModeEnable;
    }

    public void reSetOffsetFromHumanFactor(boolean offsetResetEnable, int minOffsetBrightness, int maxOffsetBrightness) {
        if (offsetResetEnable && Math.abs(this.mPosBrightness) > 1.0E-7f) {
            String str;
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
                this.mIsReset = true;
                if (DEBUG) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLevel:resetMin mPosBrightness=");
                    stringBuilder.append(this.mPosBrightness);
                    stringBuilder.append(",min=");
                    stringBuilder.append(minOffsetBrightness);
                    stringBuilder.append(",max=");
                    stringBuilder.append(maxOffsetBrightness);
                    stringBuilder.append(",mDelta=");
                    stringBuilder.append(this.mDelta);
                    stringBuilder.append(",mAmLuxOffset=");
                    stringBuilder.append(this.mAmLuxOffset);
                    stringBuilder.append(",mCalibrationRatio=");
                    stringBuilder.append(this.mCalibrationRatio);
                    Slog.d(str, stringBuilder.toString());
                }
            }
            if (this.mPosBrightness > ((float) maxOffsetBrightness)) {
                this.mPosBrightness = (float) maxOffsetBrightness;
                this.mOffsetBrightness_last = (float) maxOffsetBrightness;
                this.mDelta = this.mPosBrightness - this.mStartLuxDefaultBrightness;
                this.mIsReset = true;
                if (DEBUG) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateLevel:resetMax mPosBrightness=");
                    stringBuilder2.append(this.mPosBrightness);
                    stringBuilder2.append(",min=");
                    stringBuilder2.append(minOffsetBrightness);
                    stringBuilder2.append(",max=");
                    stringBuilder2.append(maxOffsetBrightness);
                    stringBuilder2.append(",mDelta=");
                    stringBuilder2.append(this.mDelta);
                    stringBuilder2.append(",mAmLuxOffset=");
                    stringBuilder2.append(this.mAmLuxOffset);
                    stringBuilder2.append(",mCalibrationRatio=");
                    stringBuilder2.append(this.mCalibrationRatio);
                    Slog.d(str, stringBuilder2.toString());
                }
            }
        }
    }

    public void resetGameModeOffsetFromHumanFactor(int minOffsetBrightness, int maxOffsetBrightness) {
        if (Math.abs(this.mDeltaTmp) > 1.0E-7f) {
            String str;
            StringBuilder stringBuilder;
            if (Math.abs(this.mCalibrationRatio - 1.0f) > 1.0E-7f) {
                if (((float) minOffsetBrightness) > minBrightness && ((float) minOffsetBrightness) * this.mCalibrationRatio > minBrightness) {
                    minOffsetBrightness = (int) (((float) minOffsetBrightness) * this.mCalibrationRatio);
                }
                if (((float) maxOffsetBrightness) < maxBrightness && ((float) maxOffsetBrightness) * this.mCalibrationRatio < maxBrightness) {
                    maxOffsetBrightness = (int) (((float) maxOffsetBrightness) * this.mCalibrationRatio);
                }
            }
            float positionBrightness = this.mGameModeStartLuxDefaultBrightness + this.mDeltaTmp;
            if (positionBrightness < ((float) minOffsetBrightness)) {
                this.mGameModePosBrightness = (float) minOffsetBrightness;
                this.mDeltaTmp = ((float) minOffsetBrightness) - this.mGameModeStartLuxDefaultBrightness;
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLevel GameMode:resetMin, min=");
                    stringBuilder.append(minOffsetBrightness);
                    stringBuilder.append(",max=");
                    stringBuilder.append(maxOffsetBrightness);
                    stringBuilder.append(",mDeltaTmp=");
                    stringBuilder.append(this.mDeltaTmp);
                    stringBuilder.append(",mGameModeOffsetLux=");
                    stringBuilder.append(this.mGameModeOffsetLux);
                    stringBuilder.append(",mCalibrationRatio=");
                    stringBuilder.append(this.mCalibrationRatio);
                    Slog.d(str, stringBuilder.toString());
                }
            }
            if (positionBrightness > ((float) maxOffsetBrightness)) {
                this.mGameModePosBrightness = (float) maxOffsetBrightness;
                this.mDeltaTmp = ((float) maxOffsetBrightness) - this.mGameModeStartLuxDefaultBrightness;
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateLevel GameMode:resetMax, min=");
                    stringBuilder.append(minOffsetBrightness);
                    stringBuilder.append(",max=");
                    stringBuilder.append(maxOffsetBrightness);
                    stringBuilder.append(",mDeltaTmp=");
                    stringBuilder.append(this.mDeltaTmp);
                    stringBuilder.append(",mGameModeOffsetLux=");
                    stringBuilder.append(this.mGameModeOffsetLux);
                    stringBuilder.append(",mCalibrationRatio=");
                    stringBuilder.append(this.mCalibrationRatio);
                    Slog.d(str, stringBuilder.toString());
                }
            }
        }
    }

    private void fillDarkAdaptPointsList() {
        if (this.mDarkAdaptingBrightness0LuxLevel != 0 && this.mDarkAdaptedBrightness0LuxLevel != 0) {
            if (this.mDarkAdaptedBrightness0LuxLevel > this.mDarkAdaptingBrightness0LuxLevel) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fillDarkAdaptPointsList() error adapted=");
                stringBuilder.append(this.mDarkAdaptedBrightness0LuxLevel);
                stringBuilder.append(" is larger than adapting=");
                stringBuilder.append(this.mDarkAdaptingBrightness0LuxLevel);
                Slog.w(str, stringBuilder.toString());
            } else if (this.mDefaultBrighnessLinePointsList != null) {
                float defaultBrighness0LuxLevel = ((Point) this.mDefaultBrighnessLinePointsList.get(0)).y;
                if (((float) this.mDarkAdaptingBrightness0LuxLevel) > defaultBrighness0LuxLevel) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("fillDarkAdaptPointsList() error adapting=");
                    stringBuilder2.append(this.mDarkAdaptingBrightness0LuxLevel);
                    stringBuilder2.append(" is larger than default=");
                    stringBuilder2.append(defaultBrighness0LuxLevel);
                    Slog.w(str2, stringBuilder2.toString());
                    return;
                }
                this.mDarkAdaptingBrightnessPointsList = cloneListAndReplaceFirstElement(this.mDefaultBrighnessLinePointsList, new Point(0.0f, (float) this.mDarkAdaptingBrightness0LuxLevel));
                this.mDarkAdaptedBrightnessPointsList = cloneListAndReplaceFirstElement(this.mDefaultBrighnessLinePointsList, new Point(0.0f, (float) this.mDarkAdaptedBrightness0LuxLevel));
                this.mDarkAdaptEnable = true;
            }
        }
    }

    private List<Point> cloneListAndReplaceFirstElement(List<Point> list, Point element) {
        List<Point> newList = null;
        if (list == null || element == null) {
            return null;
        }
        for (Point point : list) {
            if (newList == null) {
                newList = new ArrayList();
                newList.add(element);
            } else {
                newList.add(new Point(point.x, point.y));
            }
        }
        return newList;
    }

    private void updateDarkAdaptState() {
        if (!(this.mDarkAdaptLineLocked || this.mDarkAdaptState == this.mDarkAdaptStateDetected)) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateDarkAdaptState() ");
                stringBuilder.append(this.mDarkAdaptState);
                stringBuilder.append(" -> ");
                stringBuilder.append(this.mDarkAdaptStateDetected);
                Slog.i(str, stringBuilder.toString());
            }
            this.mDarkAdaptState = this.mDarkAdaptStateDetected;
        }
    }

    private List<Point> getCurrentDarkAdaptLine() {
        switch (this.mDarkAdaptState) {
            case UNADAPTED:
                return this.mDefaultBrighnessLinePointsList;
            case ADAPTING:
                return this.mDarkAdaptingBrightnessPointsList;
            case ADAPTED:
                return this.mDarkAdaptedBrightnessPointsList;
            default:
                return this.mDefaultBrighnessLinePointsList;
        }
    }

    private float getDarkAdaptOffset(float positionBrightness, float lux) {
        float currentOffset = getOffsetBrightnessLevel_new(this.mStartLuxDefaultBrightness, this.mDefaultBrightnessFromLux, positionBrightness);
        if (this.mDelta >= 0.0f) {
            if (HWDEBUG) {
                Slog.d(TAG, String.format("getDarkAdaptOffset() mDelta = %.1f, current = %.1f", new Object[]{Float.valueOf(this.mDelta), Float.valueOf(currentOffset)}));
            }
            return currentOffset;
        } else if (this.mDarkAdaptLineLocked) {
            if (HWDEBUG) {
                Slog.d(TAG, String.format("getDarkAdaptOffset() locked, current = %.1f", new Object[]{Float.valueOf(currentOffset)}));
            }
            return currentOffset;
        } else {
            float offsetMinLimit;
            switch (this.mDarkAdaptState) {
                case UNADAPTED:
                    offsetMinLimit = getDefaultBrightnessLevelNew(this.mDarkAdaptingBrightnessPointsList, lux);
                    break;
                case ADAPTING:
                    offsetMinLimit = (getDefaultBrightnessLevelNew(this.mDarkAdaptingBrightnessPointsList, lux) + getDefaultBrightnessLevelNew(this.mDarkAdaptedBrightnessPointsList, lux)) / 2.0f;
                    break;
                case ADAPTED:
                    offsetMinLimit = getDefaultBrightnessLevelNew(this.mDarkAdaptedBrightnessPointsList, lux);
                    break;
                default:
                    offsetMinLimit = minBrightness;
                    break;
            }
            if (HWDEBUG) {
                Slog.d(TAG, String.format("getDarkAdaptOffset() %s, current = %.1f, minLimit = %.1f", new Object[]{this.mDarkAdaptState, Float.valueOf(currentOffset), Float.valueOf(offsetMinLimit)}));
            }
            return currentOffset > offsetMinLimit ? currentOffset : offsetMinLimit;
        }
    }

    public void setDarkAdaptState(DarkAdaptState state) {
        if (this.mDarkAdaptEnable && state != null) {
            this.mDarkAdaptStateDetected = state;
        }
    }

    public void unlockDarkAdaptLine() {
        if (this.mDarkAdaptEnable && this.mDarkAdaptLineLocked) {
            this.mDarkAdaptLineLocked = false;
            if (DEBUG) {
                Slog.i(TAG, "unlockDarkAdaptLine()");
            }
        }
    }

    public boolean isDeltaValid() {
        return this.mPosBrightness > 0.0f && !this.mIsReset;
    }

    public void resetUserDragLimitFlag() {
        this.mIsReset = false;
    }
}
