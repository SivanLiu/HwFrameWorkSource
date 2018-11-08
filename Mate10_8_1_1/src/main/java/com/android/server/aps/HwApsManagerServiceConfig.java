package com.android.server.aps;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.aps.ApsAppInfo;
import android.aps.IApsManagerServiceCallback;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.AtomicFile;
import android.util.HwVRUtils;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.gesture.GestureNavConst;
import com.android.server.location.HwGpsPowerTracker;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class HwApsManagerServiceConfig {
    private static final int APS_MSG_FORCESTOP_APK = 310;
    private static final int APS_MSG_WRITE = 300;
    private static final String GET_FPS_SOURCE_CONFIG = "Aps Config";
    private static final String GET_FPS_SOURCE_SERVICE = "Aps Service";
    private static final String GET_FPS_SOURCE_UNKOWN = "unkown source";
    private static final String TAG = "HwApsManagerConfig";
    private static final float VR_APP_RATIO = 0.625f;
    private static final String mHwApsPackagesListPath = "/data/system/hwaps-packages-compat.xml";
    private static int mLowResolutionMode = 0;
    private final HwApsManagerService mApsService;
    private final AtomicFile mFile;
    private final ApsHandler mHandler;
    private final ConcurrentHashMap<String, Boolean> mNewFbSkipSwitchMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, Boolean> mNewHighpToLowpSwitchMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, Integer> mNewMipMapSwitchMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, Float> mNewResolutionRatioMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, Integer> mNewShadowMapSwitchMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, Integer> mNewTextureQualityMap = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, ApsAppInfo> mPackages = new ConcurrentHashMap();
    private ConcurrentHashMap<String, IApsManagerServiceCallback> mPkgnameCallbackMap = new ConcurrentHashMap();
    private ConcurrentHashMap<String, Integer> newFpsMap = new ConcurrentHashMap();

    private final class ApsHandler extends Handler {
        public ApsHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 300:
                    HwApsManagerServiceConfig.this.saveApsAppInfo();
                    return;
                case HwApsManagerServiceConfig.APS_MSG_FORCESTOP_APK /*310*/:
                    HwApsManagerServiceConfig.this.stopApsCompatPackages();
                    return;
                default:
                    return;
            }
        }
    }

    public HwApsManagerServiceConfig(HwApsManagerService service, Handler handler) {
        mLowResolutionMode = service.getLowResolutionSwitchState();
        this.mFile = new AtomicFile(new File(mHwApsPackagesListPath));
        this.mHandler = new ApsHandler(handler.getLooper());
        this.mApsService = service;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = this.mFile.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
            int eventType = parser.getEventType();
            while (eventType != 2 && eventType != 1) {
                eventType = parser.next();
            }
            if (eventType == 1) {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                    }
                }
                return;
            }
            if ("hwaps-compat-packages".equals(parser.getName())) {
                eventType = parser.next();
                do {
                    if (eventType == 2) {
                        String tagName = parser.getName();
                        if (parser.getDepth() == 2 && HwGpsPowerTracker.DEL_PKG.equals(tagName)) {
                            String pkg = parser.getAttributeValue(null, "name");
                            String resolutionratio = parser.getAttributeValue(null, "resolutionratio");
                            String framerate = parser.getAttributeValue(null, "framerate");
                            String texturepercent = parser.getAttributeValue(null, "texturepercent");
                            String brightnesspercent = parser.getAttributeValue(null, "brightnesspercent");
                            String switchable = parser.getAttributeValue(null, "switchable");
                            float f = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                            int i = 0;
                            int i2 = 0;
                            int i3 = 0;
                            boolean switchableBoolean = true;
                            try {
                                f = Float.parseFloat(resolutionratio);
                                i = Integer.parseInt(framerate);
                                i2 = Integer.parseInt(texturepercent);
                                i3 = Integer.parseInt(brightnesspercent);
                                switchableBoolean = Boolean.parseBoolean(switchable);
                            } catch (NumberFormatException e2) {
                            }
                            this.mPackages.put(pkg, new ApsAppInfo(pkg, f, i, i2, i3, switchableBoolean));
                        }
                    }
                    eventType = parser.next();
                } while (eventType != 1);
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e3) {
                }
            }
        } catch (XmlPullParserException e4) {
            Slog.w(TAG, "Error reading hwaps-compat-packages", e4);
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e5) {
                }
            }
        } catch (IOException e6) {
            if (fileInputStream != null) {
                Slog.w(TAG, "Error reading hwaps-compat-packages", e6);
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e7) {
                }
            }
        } catch (Throwable th) {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e8) {
                }
            }
        }
    }

    public void stopApsCompatPackages() {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            for (String pkgName : this.mPackages.keySet()) {
                Slog.i(TAG, "stopApsCompatPackages pkgName = " + pkgName);
                if (pkgName != null) {
                    ApsAppInfo info = (ApsAppInfo) this.mPackages.get(pkgName);
                    if (info != null && info.getSwitchable()) {
                        try {
                            am.forceStopPackage(pkgName, -1);
                        } catch (RemoteException e) {
                            Slog.e(TAG, "Failed to kill aps package of " + pkgName);
                        }
                    }
                }
            }
        }
    }

    void stopAllAppsInLowResolution() {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            for (Entry<String, ApsAppInfo> entry : this.mPackages.entrySet()) {
                String pkgName = (String) entry.getKey();
                ApsAppInfo info = (ApsAppInfo) entry.getValue();
                if (!info.getSwitchable() || (info.getSwitchable() && mLowResolutionMode == 1)) {
                    try {
                        am.forceStopPackage(pkgName, -1);
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to kill aps package of " + pkgName + " when stop all low resolution apps.");
                    }
                }
            }
        }
    }

    public boolean findPackageInListLocked(String pkgName) {
        if (((ApsAppInfo) this.mPackages.get(pkgName)) == null) {
            return false;
        }
        return true;
    }

    public boolean registerCallbackLocked(String pkgName, IApsManagerServiceCallback callback) {
        if (pkgName == null || pkgName.isEmpty()) {
            return false;
        }
        if (callback != null) {
            this.mPkgnameCallbackMap.put(pkgName, callback);
            Slog.i(TAG, "registerCallbackLocked success, pkgName:" + pkgName + ", callback_count:" + this.mPkgnameCallbackMap.size());
            doCallbackAtFirstRegisterLocked(pkgName, callback);
        } else {
            Slog.i(TAG, "unregisterCallback from service, pkg:" + pkgName);
            this.mPkgnameCallbackMap.remove(pkgName);
        }
        return true;
    }

    public int notifyApsManagerServiceCallback(String pkgName, int apsCallbackCode, int data) {
        IApsManagerServiceCallback callback = (IApsManagerServiceCallback) this.mPkgnameCallbackMap.get(pkgName);
        if (callback == null) {
            Slog.d(TAG, "notifyApsManagerServiceCallback, pkgName:" + pkgName + " , callback is not found.");
            return -1;
        }
        try {
            Slog.d(TAG, "notifyApsManagerServiceCallback, pkgName:" + pkgName + ", apsCallbackCode:" + apsCallbackCode + ", data:" + data);
            callback.doCallback(apsCallbackCode, data);
            return 0;
        } catch (RemoteException ex) {
            this.mPkgnameCallbackMap.remove(pkgName);
            Slog.w(TAG, "notifyApsManagerServiceCallback,ex:" + ex + ", remove " + pkgName + " from mPkgnameCallbackMap.");
            return -5;
        }
    }

    public void doCallbackAtFirstRegisterLocked(String pkgName, IApsManagerServiceCallback callback) {
        try {
            int data;
            Slog.i(TAG, "doCallbackAtFirstRegisterLocked, start ! pkgName:" + pkgName);
            if (this.mNewFbSkipSwitchMap.get(pkgName) != null) {
                data = ((Boolean) this.mNewFbSkipSwitchMap.get(pkgName)).booleanValue() ? 1 : 0;
                Slog.i(TAG, "doCallbackAtFirstRegisterLocked, callback:4, data: " + data + " , from new config.");
                callback.doCallback(4, data);
            }
            if (this.mNewHighpToLowpSwitchMap.get(pkgName) != null) {
                data = ((Boolean) this.mNewHighpToLowpSwitchMap.get(pkgName)).booleanValue() ? 1 : 0;
                Slog.i(TAG, "doCallbackAtFirstRegisterLocked, callback:5, data: " + data + " , from new config.");
                callback.doCallback(5, data);
            }
            if (this.mNewShadowMapSwitchMap.get(pkgName) != null) {
                data = ((Integer) this.mNewShadowMapSwitchMap.get(pkgName)).intValue();
                Slog.i(TAG, "doCallbackAtFirstRegisterLocked, callback:6, data: " + data + " , from new config.");
                callback.doCallback(6, data);
            }
            if (this.mNewMipMapSwitchMap.get(pkgName) != null) {
                data = ((Integer) this.mNewMipMapSwitchMap.get(pkgName)).intValue();
                Slog.i(TAG, "doCallbackAtFirstRegisterLocked, callback:7, data: " + data + " , from new config.");
                callback.doCallback(7, data);
            }
            if (this.mNewResolutionRatioMap.get(pkgName) != null) {
                data = (int) (((Float) this.mNewResolutionRatioMap.get(pkgName)).floatValue() * 100000.0f);
                Slog.i(TAG, "doCallbackAtFirstRegisterLocked, callback:1, data: " + data);
                callback.doCallback(1, data);
            }
            if (this.newFpsMap.get(pkgName) != null) {
                data = ((Integer) this.newFpsMap.get(pkgName)).intValue();
                Slog.i(TAG, "doCallbackAtFirstRegisterLocked, callback:0, data: " + data + " , from new config.");
                callback.doCallback(0, data);
                return;
            }
            ApsAppInfo apsInfo = (ApsAppInfo) this.mPackages.get(pkgName);
            if (apsInfo != null) {
                data = apsInfo.getFrameRatio();
                Slog.i(TAG, "doCallbackAtFirstRegisterLocked, callback:0, data: " + data + " , from database config.");
                callback.doCallback(0, data);
            }
        } catch (RemoteException ex) {
            Slog.w(TAG, "doCallbackAtFirstRegisterLocked, pkgName: " + pkgName + ", ex:" + ex);
        }
    }

    public int setResolutionLocked(String pkgName, float ratio, boolean switchable) {
        ApsAppInfo apsInfo = (ApsAppInfo) this.mPackages.get(pkgName);
        if (apsInfo == null) {
            apsInfo = new ApsAppInfo(pkgName, ratio, 60, 100, 100, switchable);
        } else {
            apsInfo.setResolutionRatio(ratio, switchable);
        }
        this.mPackages.put(pkgName, apsInfo);
        scheduleWrite();
        return 0;
    }

    public int setLowResolutionModeLocked(int lowResolutionMode) {
        Slog.i(TAG, "setLowResolutionModeLocked, lowReosulotionMode = " + lowResolutionMode);
        mLowResolutionMode = lowResolutionMode;
        this.mHandler.removeMessages(APS_MSG_FORCESTOP_APK);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(APS_MSG_FORCESTOP_APK));
        return 0;
    }

    public int setFpsLocked(String pkgName, int fps) {
        ApsAppInfo apsInfo = (ApsAppInfo) this.mPackages.get(pkgName);
        if (apsInfo == null) {
            Slog.w(TAG, "setFpsLocked can not get ApsAppInfo. We will create a new one");
            apsInfo = new ApsAppInfo(pkgName, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, fps, 100, 100, true);
        } else {
            apsInfo.setFps(fps);
        }
        this.mPackages.put(pkgName, apsInfo);
        scheduleWrite();
        return 0;
    }

    public int setBrightnessLocked(String pkgName, int ratioPercent) {
        return -1;
    }

    public int setTextureLocked(String pkgName, int texture) {
        if (texture < 5 || texture > 100) {
            return -6;
        }
        try {
            this.mNewTextureQualityMap.put(pkgName, Integer.valueOf(texture));
            return 0;
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, setTextureLocked, exception:" + e);
            return -6;
        }
    }

    public int setFbSkipLocked(String pkgName, boolean onoff) {
        try {
            this.mNewFbSkipSwitchMap.put(pkgName, Boolean.valueOf(onoff));
            return 0;
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, setFbSkipLocked, exception:" + e);
            return -6;
        }
    }

    public int setHighpToLowpLocked(String pkgName, boolean onoff) {
        try {
            this.mNewHighpToLowpSwitchMap.put(pkgName, Boolean.valueOf(onoff));
            return 0;
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, setHighpToLowpLocked, exception:" + e);
            return -6;
        }
    }

    public int setShadowMapLocked(String pkgName, int status) {
        try {
            this.mNewShadowMapSwitchMap.put(pkgName, Integer.valueOf(status));
            return 0;
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, setShadowMapLocked, exception:" + e);
            return -6;
        }
    }

    public int setMipMapLocked(String pkgName, int status) {
        try {
            this.mNewMipMapSwitchMap.put(pkgName, Integer.valueOf(status));
            return 0;
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, setMipMapLocked, exception:" + e);
            return -6;
        }
    }

    public boolean getFbSkipLocked(String pkgName) {
        try {
            if (this.mNewFbSkipSwitchMap.get(pkgName) == null) {
                return false;
            }
            return ((Boolean) this.mNewFbSkipSwitchMap.get(pkgName)).booleanValue();
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, getFbSkip, exception:" + e);
            return false;
        }
    }

    public boolean getHighpToLowpLocked(String pkgName) {
        try {
            if (this.mNewHighpToLowpSwitchMap.get(pkgName) == null) {
                return false;
            }
            return ((Boolean) this.mNewHighpToLowpSwitchMap.get(pkgName)).booleanValue();
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, getHighpToLowp, exception:" + e);
            return false;
        }
    }

    public int getShadowMapLocked(String pkgName) {
        try {
            if (this.mNewShadowMapSwitchMap.get(pkgName) == null) {
                return 0;
            }
            return ((Integer) this.mNewShadowMapSwitchMap.get(pkgName)).intValue();
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, getShadowMap, exception:" + e);
            return 0;
        }
    }

    public int getMipMapLocked(String pkgName) {
        try {
            if (this.mNewMipMapSwitchMap.get(pkgName) == null) {
                return 0;
            }
            return ((Integer) this.mNewMipMapSwitchMap.get(pkgName)).intValue();
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, getMipMap, exception:" + e);
            return 0;
        }
    }

    public int setDynamicResolutionRatioLocked(String pkgName, float ratio) {
        if (ratio <= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || ratio > 1.0f) {
            try {
                Slog.i(TAG, "setDynamicResolutionRatioLocked, pkg:" + pkgName + ", ratio:" + ratio + ", return APS_ERRNO_RUNAS_CONFIG]");
                return -4;
            } catch (Exception e) {
                Slog.e(TAG, "APS, HwApsManagerServiceConfig, setDynamicResolutionRatioLocked, exception:" + e);
                return -6;
            }
        }
        if (ratio == 1.0f) {
            this.mNewResolutionRatioMap.remove(pkgName);
        } else {
            this.mNewResolutionRatioMap.put(pkgName, Float.valueOf(ratio));
        }
        Slog.i(TAG, "APSLog, setDynamicResolutionRatioLocked, pkg:" + pkgName + ", ratio:" + ratio + ", retCode:0");
        return 0;
    }

    public float getDynamicResolutionRatioLocked(String pkgName) {
        try {
            if (this.mNewResolutionRatioMap.get(pkgName) == null) {
                return 1.0f;
            }
            return ((Float) this.mNewResolutionRatioMap.get(pkgName)).floatValue();
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, getDynamicResolutionRatioLocked, exception:" + e);
            return 1.0f;
        }
    }

    public int setDynamicFpsLocked(String pkgName, int fps) {
        try {
            ApsAppInfo apsInfo = (ApsAppInfo) this.mPackages.get(pkgName);
            if (apsInfo == null || fps <= apsInfo.getFrameRatio()) {
                if (fps == -1) {
                    this.newFpsMap.remove(pkgName);
                } else {
                    this.newFpsMap.put(pkgName, Integer.valueOf(fps));
                }
                Slog.i(TAG, "APSLog, setDynamicFpsLocked: pkg:" + pkgName + ",fps:" + fps + ",retCode:0 ");
                return 0;
            }
            Slog.i(TAG, "APSLog, setDynamicFpsLocked: pkg:" + pkgName + ",fps:" + fps + ",retCode:-4->APS_ERRNO_RUNAS_CONFIG, config fps:" + apsInfo.getFrameRatio());
            return -4;
        } catch (Exception e) {
            Slog.e(TAG, "APS, HwApsManagerServiceConfig, setDynamicFPSLocked, exception:" + e);
            return -6;
        }
    }

    public int getDynamicFpsLocked(String pkgName) {
        if (pkgName == null) {
            try {
                Slog.e(TAG, "getResolutionLocked input invalid param!");
                return -1;
            } catch (Exception e) {
                Slog.e(TAG, "APS, HwApsManagerServiceConfig, getDynamicFPSLocked, exception:" + e);
                return -6;
            }
        }
        int retFps = -1;
        String fpsSourceForPrintLog = GET_FPS_SOURCE_UNKOWN;
        ApsAppInfo apsInfo = (ApsAppInfo) this.mPackages.get(pkgName);
        if (this.newFpsMap.containsKey(pkgName)) {
            retFps = ((Integer) this.newFpsMap.get(pkgName)).intValue();
            fpsSourceForPrintLog = GET_FPS_SOURCE_SERVICE;
        } else if (apsInfo != null) {
            retFps = apsInfo.getFrameRatio();
            fpsSourceForPrintLog = GET_FPS_SOURCE_CONFIG;
        }
        Slog.i(TAG, "APSLog -> getFps:[from:" + fpsSourceForPrintLog + ",pkgName:" + pkgName + ",fps:" + retFps + "]");
        return retFps;
    }

    public int setPackageApsInfoLocked(String pkgName, ApsAppInfo info) {
        if (info == null) {
            return -1;
        }
        this.mPackages.put(pkgName, new ApsAppInfo(info));
        scheduleWrite();
        return 0;
    }

    public ApsAppInfo getPackageApsInfoLocked(String pkgName) {
        if (pkgName != null) {
            return (ApsAppInfo) this.mPackages.get(pkgName);
        }
        Slog.e(TAG, "getPackageApsInfoLocked input invalid param!");
        return null;
    }

    public float getResolutionLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "getResolutionLocked input invalid param!");
            return -1.0f;
        } else if (this.mApsService.mInCarMode) {
            return -1.0f;
        } else {
            ApsAppInfo info = (ApsAppInfo) this.mPackages.get(pkgName);
            if (HwVRUtils.isVRMode() && HwVRUtils.isVRLowPowerApp(pkgName)) {
                return VR_APP_RATIO;
            }
            if (info == null || (info.getSwitchable() && mLowResolutionMode == 0)) {
                return -1.0f;
            }
            int defaultWidth = SystemProperties.getInt("persist.sys.aps.defaultWidth", 0);
            int curWidth = SystemProperties.getInt("persist.sys.rog.width", 0);
            float ratio = info.getResolutionRatio();
            if (!(defaultWidth == 0 || curWidth == 0 || defaultWidth == curWidth || (HwVRUtils.isVRLowPowerApp(pkgName) ^ 1) == 0)) {
                ratio = ratio >= ((float) curWidth) / ((float) defaultWidth) ? -1.0f : (((float) defaultWidth) * ratio) / ((float) curWidth);
            }
            return ratio;
        }
    }

    public int getTextureLocked(String pkgName) {
        if (pkgName == null) {
            try {
                Slog.e(TAG, "getTextureLocked input invalid param!");
                return -1;
            } catch (Exception e) {
                Slog.e(TAG, "APS, HwApsManagerServiceConfig, getTextureLocked, exception:" + e);
                return -1;
            }
        } else if (this.mNewTextureQualityMap.get(pkgName) != null) {
            return ((Integer) this.mNewTextureQualityMap.get(pkgName)).intValue();
        } else {
            ApsAppInfo apsAppInfo = (ApsAppInfo) this.mPackages.get(pkgName);
            if (apsAppInfo == null) {
                return -1;
            }
            return apsAppInfo.getTexturePercent();
        }
    }

    public int getFpsLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "getResolutionLocked input invalid param!");
            return -1;
        }
        ApsAppInfo info = (ApsAppInfo) this.mPackages.get(pkgName);
        if (info == null) {
            return -1;
        }
        return info.getFrameRatio();
    }

    public int getBrightnessLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "getResolutionLocked input invalid param!");
            return -1;
        }
        ApsAppInfo info = (ApsAppInfo) this.mPackages.get(pkgName);
        if (info == null) {
            return -1;
        }
        return info.getBrightnessPercent();
    }

    public boolean deletePackageApsInfoLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "deletePackageApsInfoLocked input invalid param!");
            return false;
        } else if (((ApsAppInfo) this.mPackages.get(pkgName)) == null) {
            return false;
        } else {
            this.mPackages.remove(pkgName);
            scheduleWrite();
            return true;
        }
    }

    public List<ApsAppInfo> getAllPackagesApsInfoLocked() {
        List<ApsAppInfo> apsAppInfolist = new ArrayList();
        for (Entry<String, ApsAppInfo> entry : this.mPackages.entrySet()) {
            apsAppInfolist.add((ApsAppInfo) entry.getValue());
        }
        return apsAppInfolist;
    }

    public List<String> getAllApsPackagesLocked() {
        List<String> apsPackageNameList = new ArrayList();
        for (String pkgName : this.mPackages.keySet()) {
            apsPackageNameList.add(pkgName);
        }
        return apsPackageNameList;
    }

    public boolean updateApsInfoLocked(List<ApsAppInfo> infos) {
        for (ApsAppInfo apsInfoUpdate : infos) {
            if (apsInfoUpdate == null) {
                Slog.e(TAG, "updateApsInfoLocked error, can not find apsInfo.");
                return false;
            }
            String pkgNameUpdate = apsInfoUpdate.getBasePackageName();
            for (String pkgName : this.mPackages.keySet()) {
                if (pkgName.equals(pkgNameUpdate)) {
                    ApsAppInfo apsInfo = (ApsAppInfo) this.mPackages.get(pkgName);
                }
            }
        }
        return false;
    }

    private void scheduleWrite() {
        this.mHandler.removeMessages(300);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(300), MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
    }

    void saveApsAppInfo() {
        synchronized (this.mApsService) {
            HashMap<String, ApsAppInfo> pkgs = new HashMap(this.mPackages);
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = this.mFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fileOutputStream, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag(null, "hwaps-compat-packages");
            for (Entry<String, ApsAppInfo> entry : pkgs.entrySet()) {
                String pkg = (String) entry.getKey();
                ApsAppInfo apsInfo = (ApsAppInfo) entry.getValue();
                if (apsInfo != null) {
                    out.startTag(null, HwGpsPowerTracker.DEL_PKG);
                    out.attribute(null, "name", pkg);
                    float rr = apsInfo.getResolutionRatio();
                    int fr = apsInfo.getFrameRatio();
                    int tp = apsInfo.getTexturePercent();
                    int bp = apsInfo.getBrightnessPercent();
                    boolean sa = apsInfo.getSwitchable();
                    out.attribute(null, "resolutionratio", Float.toString(rr));
                    out.attribute(null, "framerate", Integer.toString(fr));
                    out.attribute(null, "texturepercent", Integer.toString(tp));
                    out.attribute(null, "brightnesspercent", Integer.toString(bp));
                    out.attribute(null, "switchable", Boolean.toString(sa));
                    out.endTag(null, HwGpsPowerTracker.DEL_PKG);
                }
            }
            out.endTag(null, "hwaps-compat-packages");
            out.endDocument();
            this.mFile.finishWrite(fileOutputStream);
        } catch (IOException e) {
            Slog.e(TAG, "Error writing hwaps compat packages", e);
            if (fileOutputStream != null) {
                this.mFile.failWrite(fileOutputStream);
            }
        }
    }
}
