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
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.gesture.GestureNavConst;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
    private List<String> mApsColorPlusPkgList = new ArrayList();
    private final HwApsManagerService mApsService;
    private int mApsSupportValue = 0;
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
            int i = msg.what;
            if (i == 300) {
                HwApsManagerServiceConfig.this.saveApsAppInfo();
            } else if (i == HwApsManagerServiceConfig.APS_MSG_FORCESTOP_APK) {
                HwApsManagerServiceConfig.this.stopApsCompatPackages();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x00ab A:{SYNTHETIC, Splitter: B:14:0x00ab} */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x00a3  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwApsManagerServiceConfig(HwApsManagerService service, Handler handler) {
        mLowResolutionMode = service.getLowResolutionSwitchState();
        this.mApsSupportValue = SystemProperties.getInt("sys.aps.support", 0);
        this.mFile = new AtomicFile(new File(mHwApsPackagesListPath));
        this.mHandler = new ApsHandler(handler.getLooper());
        this.mApsService = service;
        String str = null;
        FileInputStream fis = null;
        try {
            int eventType;
            fis = this.mFile.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            int eventType2 = parser.getEventType();
            while (true) {
                eventType = eventType2;
                int i = 2;
                if (eventType != 2 && eventType != 1) {
                    eventType2 = parser.next();
                } else if (eventType != 1) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                        }
                    }
                    return;
                } else {
                    if ("hwaps-compat-packages".equals(parser.getName())) {
                        eventType = parser.next();
                        while (true) {
                            if (eventType == i) {
                                String tagName = parser.getName();
                                if (parser.getDepth() == i && "pkg".equals(tagName)) {
                                    String pkg = parser.getAttributeValue(str, "name");
                                    String resolutionratio = parser.getAttributeValue(str, "resolutionratio");
                                    String framerate = parser.getAttributeValue(str, "framerate");
                                    String maxframerate = parser.getAttributeValue(str, "maxframerate");
                                    String texturepercent = parser.getAttributeValue(str, "texturepercent");
                                    String brightnesspercent = parser.getAttributeValue(str, "brightnesspercent");
                                    int framerateInt = parser.getAttributeValue(str, "switchable");
                                    float resolutionratioFloat = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                                    int framerateInt2 = 0;
                                    int maxframerateInt = 0;
                                    int texturepercentInt = 0;
                                    int brightnesspercentInt = 0;
                                    boolean switchableBoolean = true;
                                    try {
                                        resolutionratioFloat = Float.parseFloat(resolutionratio);
                                        framerateInt2 = Integer.parseInt(framerate);
                                        maxframerateInt = Integer.parseInt(maxframerate);
                                        texturepercentInt = Integer.parseInt(texturepercent);
                                        brightnesspercentInt = Integer.parseInt(brightnesspercent);
                                        try {
                                            switchableBoolean = Boolean.parseBoolean(framerateInt);
                                        } catch (NumberFormatException e2) {
                                        }
                                    } catch (NumberFormatException e3) {
                                        Object obj = framerateInt;
                                    }
                                    this.mPackages.put(pkg, new ApsAppInfo(pkg, resolutionratioFloat, framerateInt2, maxframerateInt, texturepercentInt, brightnesspercentInt, switchableBoolean));
                                }
                            }
                            eventType = parser.next();
                            if (eventType == 1) {
                                break;
                            }
                            str = null;
                            i = 2;
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e4) {
                        }
                    }
                    return;
                }
            }
            if (eventType != 1) {
            }
        } catch (XmlPullParserException e5) {
            Slog.w(TAG, "Error reading hwaps-compat-packages", e5);
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e6) {
            if (fis != null) {
                Slog.w(TAG, "Error reading hwaps-compat-packages", e6);
            }
            if (fis != null) {
                fis.close();
            }
        } catch (Throwable th) {
            Throwable th2 = th;
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e7) {
                }
            }
        }
    }

    public void stopApsCompatPackages() {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null) {
            for (String pkgName : this.mPackages.keySet()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stopApsCompatPackages pkgName = ");
                stringBuilder.append(pkgName);
                Slog.i(str, stringBuilder.toString());
                if (pkgName != null) {
                    ApsAppInfo info = (ApsAppInfo) this.mPackages.get(pkgName);
                    if (info != null && info.getSwitchable()) {
                        try {
                            am.forceStopPackage(pkgName, -1);
                        } catch (RemoteException e) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Failed to kill aps package of ");
                            stringBuilder2.append(pkgName);
                            Slog.e(str2, stringBuilder2.toString());
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
                if (info.getSwitchable()) {
                    if (info.getSwitchable()) {
                        int i = mLowResolutionMode;
                        HwApsManagerService hwApsManagerService = this.mApsService;
                        if (i != 1) {
                        }
                    }
                }
                try {
                    am.forceStopPackage(pkgName, -1);
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to kill aps package of ");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(" when stop all low resolution apps.");
                    Slog.e(str, stringBuilder.toString());
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
        String str;
        StringBuilder stringBuilder;
        if (callback != null) {
            this.mPkgnameCallbackMap.put(pkgName, callback);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("registerCallbackLocked success, pkgName:");
            stringBuilder.append(pkgName);
            stringBuilder.append(", callback_count:");
            stringBuilder.append(this.mPkgnameCallbackMap.size());
            Slog.i(str, stringBuilder.toString());
            doCallbackAtFirstRegisterLocked(pkgName, callback);
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("unregisterCallback from service, pkg:");
            stringBuilder.append(pkgName);
            Slog.i(str, stringBuilder.toString());
            this.mPkgnameCallbackMap.remove(pkgName);
        }
        return true;
    }

    public int notifyApsManagerServiceCallback(String pkgName, int apsCallbackCode, int data) {
        IApsManagerServiceCallback callback = (IApsManagerServiceCallback) this.mPkgnameCallbackMap.get(pkgName);
        String str;
        StringBuilder stringBuilder;
        if (callback == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notifyApsManagerServiceCallback, pkgName:");
            stringBuilder.append(pkgName);
            stringBuilder.append(" , callback is not found.");
            Slog.d(str, stringBuilder.toString());
            return -1;
        }
        try {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("notifyApsManagerServiceCallback, pkgName:");
            stringBuilder.append(pkgName);
            stringBuilder.append(", apsCallbackCode:");
            stringBuilder.append(apsCallbackCode);
            stringBuilder.append(", data:");
            stringBuilder.append(data);
            Slog.d(str, stringBuilder.toString());
            callback.doCallback(apsCallbackCode, data);
            return 0;
        } catch (RemoteException ex) {
            this.mPkgnameCallbackMap.remove(pkgName);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("notifyApsManagerServiceCallback,ex:");
            stringBuilder2.append(ex);
            stringBuilder2.append(", remove ");
            stringBuilder2.append(pkgName);
            stringBuilder2.append(" from mPkgnameCallbackMap.");
            Slog.w(str2, stringBuilder2.toString());
            return -5;
        }
    }

    public void doCallbackAtFirstRegisterLocked(String pkgName, IApsManagerServiceCallback callback) {
        String str;
        StringBuilder stringBuilder;
        try {
            int data;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("doCallbackAtFirstRegisterLocked, start ! pkgName:");
            stringBuilder2.append(pkgName);
            Slog.i(str2, stringBuilder2.toString());
            if (this.mNewFbSkipSwitchMap.get(pkgName) != null) {
                data = ((Boolean) this.mNewFbSkipSwitchMap.get(pkgName)).booleanValue();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("doCallbackAtFirstRegisterLocked, callback:4, data: ");
                stringBuilder.append(data);
                stringBuilder.append(" , from new config.");
                Slog.i(str, stringBuilder.toString());
                callback.doCallback(4, data);
            }
            if (this.mNewHighpToLowpSwitchMap.get(pkgName) != null) {
                data = ((Boolean) this.mNewHighpToLowpSwitchMap.get(pkgName)).booleanValue();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("doCallbackAtFirstRegisterLocked, callback:5, data: ");
                stringBuilder.append(data);
                stringBuilder.append(" , from new config.");
                Slog.i(str, stringBuilder.toString());
                callback.doCallback(5, data);
            }
            if (this.mNewShadowMapSwitchMap.get(pkgName) != null) {
                data = ((Integer) this.mNewShadowMapSwitchMap.get(pkgName)).intValue();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("doCallbackAtFirstRegisterLocked, callback:6, data: ");
                stringBuilder.append(data);
                stringBuilder.append(" , from new config.");
                Slog.i(str, stringBuilder.toString());
                callback.doCallback(6, data);
            }
            if (this.mNewMipMapSwitchMap.get(pkgName) != null) {
                data = ((Integer) this.mNewMipMapSwitchMap.get(pkgName)).intValue();
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("doCallbackAtFirstRegisterLocked, callback:7, data: ");
                stringBuilder.append(data);
                stringBuilder.append(" , from new config.");
                Slog.i(str, stringBuilder.toString());
                callback.doCallback(7, data);
            }
            if (this.mNewResolutionRatioMap.get(pkgName) != null) {
                data = (int) (((Float) this.mNewResolutionRatioMap.get(pkgName)).floatValue() * 100000.0f);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("doCallbackAtFirstRegisterLocked, callback:1, data: ");
                stringBuilder.append(data);
                Slog.i(str, stringBuilder.toString());
                callback.doCallback(1, data);
            }
            if (this.newFpsMap.get(pkgName) != null) {
                data = ((Integer) this.newFpsMap.get(pkgName)).intValue();
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("doCallbackAtFirstRegisterLocked, callback:0, data: ");
                stringBuilder3.append(data);
                stringBuilder3.append(" , from new config.");
                Slog.i(str3, stringBuilder3.toString());
                callback.doCallback(0, data);
                return;
            }
            ApsAppInfo apsInfo = (ApsAppInfo) this.mPackages.get(pkgName);
            if (apsInfo != null) {
                int data2 = apsInfo.getFrameRatio();
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("doCallbackAtFirstRegisterLocked, callback:0, data: ");
                stringBuilder4.append(data2);
                stringBuilder4.append(" , from database config.");
                Slog.i(str4, stringBuilder4.toString());
                callback.doCallback(0, data2);
            }
        } catch (RemoteException ex) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("doCallbackAtFirstRegisterLocked, pkgName: ");
            stringBuilder.append(pkgName);
            stringBuilder.append(", ex:");
            stringBuilder.append(ex);
            Slog.w(str, stringBuilder.toString());
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setLowResolutionModeLocked, lowReosulotionMode = ");
        stringBuilder.append(lowResolutionMode);
        Slog.i(str, stringBuilder.toString());
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

    public int setMaxFpsLocked(String pkgName, int fps) {
        return -1;
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, setTextureLocked, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return -6;
        }
    }

    public int setFbSkipLocked(String pkgName, boolean onoff) {
        try {
            this.mNewFbSkipSwitchMap.put(pkgName, Boolean.valueOf(onoff));
            return 0;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, setFbSkipLocked, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return -6;
        }
    }

    public int setHighpToLowpLocked(String pkgName, boolean onoff) {
        try {
            this.mNewHighpToLowpSwitchMap.put(pkgName, Boolean.valueOf(onoff));
            return 0;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, setHighpToLowpLocked, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return -6;
        }
    }

    public int setShadowMapLocked(String pkgName, int status) {
        try {
            this.mNewShadowMapSwitchMap.put(pkgName, Integer.valueOf(status));
            return 0;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, setShadowMapLocked, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return -6;
        }
    }

    public int setMipMapLocked(String pkgName, int status) {
        try {
            this.mNewMipMapSwitchMap.put(pkgName, Integer.valueOf(status));
            return 0;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, setMipMapLocked, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, getFbSkip, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, getHighpToLowp, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, getShadowMap, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, getMipMap, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return 0;
        }
    }

    public int setDynamicResolutionRatioLocked(String pkgName, float ratio) {
        String str;
        StringBuilder stringBuilder;
        if (ratio <= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO || ratio > 1.0f) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setDynamicResolutionRatioLocked, pkg:");
            stringBuilder.append(pkgName);
            stringBuilder.append(", ratio:");
            stringBuilder.append(ratio);
            stringBuilder.append(", return APS_ERRNO_RUNAS_CONFIG]");
            Slog.i(str, stringBuilder.toString());
            return -4;
        }
        if (ratio == 1.0f) {
            try {
                this.mNewResolutionRatioMap.remove(pkgName);
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("APS, HwApsManagerServiceConfig, setDynamicResolutionRatioLocked, exception:");
                stringBuilder2.append(e);
                Slog.e(str2, stringBuilder2.toString());
                return -6;
            }
        }
        this.mNewResolutionRatioMap.put(pkgName, Float.valueOf(ratio));
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("APSLog, setDynamicResolutionRatioLocked, pkg:");
        stringBuilder.append(pkgName);
        stringBuilder.append(", ratio:");
        stringBuilder.append(ratio);
        stringBuilder.append(", retCode:0");
        Slog.i(str, stringBuilder.toString());
        return 0;
    }

    public float getDynamicResolutionRatioLocked(String pkgName) {
        try {
            if (this.mNewResolutionRatioMap.get(pkgName) == null) {
                return 1.0f;
            }
            return ((Float) this.mNewResolutionRatioMap.get(pkgName)).floatValue();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, getDynamicResolutionRatioLocked, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return 1.0f;
        }
    }

    public int setDynamicFpsLocked(String pkgName, int fps) {
        String str;
        StringBuilder stringBuilder;
        try {
            ApsAppInfo apsInfo = (ApsAppInfo) this.mPackages.get(pkgName);
            if (apsInfo == null || fps <= apsInfo.getFrameRatio()) {
                if (fps == -1) {
                    this.newFpsMap.remove(pkgName);
                } else {
                    this.newFpsMap.put(pkgName, Integer.valueOf(fps));
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("APSLog, setDynamicFpsLocked: pkg:");
                stringBuilder.append(pkgName);
                stringBuilder.append(",fps:");
                stringBuilder.append(fps);
                stringBuilder.append(",retCode:0 ");
                Slog.i(str, stringBuilder.toString());
                return 0;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("APSLog, setDynamicFpsLocked: pkg:");
            stringBuilder.append(pkgName);
            stringBuilder.append(",fps:");
            stringBuilder.append(fps);
            stringBuilder.append(",retCode:-4->APS_ERRNO_RUNAS_CONFIG, config fps:");
            stringBuilder.append(apsInfo.getFrameRatio());
            Slog.i(str, stringBuilder.toString());
            return -4;
        } catch (Exception e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, setDynamicFPSLocked, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return -6;
        }
    }

    public int getDynamicFpsLocked(String pkgName) {
        String str;
        Exception e;
        if (pkgName == null) {
            try {
                Slog.e(TAG, "getResolutionLocked input invalid param!");
                return -1;
            } catch (Exception e2) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("APS, HwApsManagerServiceConfig, getDynamicFPSLocked, exception:");
                stringBuilder.append(e2);
                Slog.e(str, stringBuilder.toString());
                return -6;
            }
        }
        e2 = -1;
        str = GET_FPS_SOURCE_UNKOWN;
        ApsAppInfo apsInfo = (ApsAppInfo) this.mPackages.get(pkgName);
        if (this.newFpsMap.containsKey(pkgName)) {
            e2 = ((Integer) this.newFpsMap.get(pkgName)).intValue();
            str = GET_FPS_SOURCE_SERVICE;
        } else if (apsInfo != null) {
            e2 = apsInfo.getFrameRatio();
            str = GET_FPS_SOURCE_CONFIG;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("APSLog -> getFps:[from:");
        stringBuilder2.append(str);
        stringBuilder2.append(",pkgName:");
        stringBuilder2.append(pkgName);
        stringBuilder2.append(",fps:");
        stringBuilder2.append(e2);
        stringBuilder2.append("]");
        Slog.i(str2, stringBuilder2.toString());
        return e2;
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
        } else if ((this.mApsSupportValue & 32768) == 0) {
            Log.i(TAG, "HwApsManagerServiceConfig.getResoutionLocked, application low resolution is not supported.");
            return -1.0f;
        } else if (this.mApsService.mInCarMode) {
            return -1.0f;
        } else {
            ApsAppInfo info = (ApsAppInfo) this.mPackages.get(pkgName);
            if (HwVRUtils.isVRMode() && HwVRUtils.isVRLowPowerApp(pkgName)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("return VR App pkgName = ");
                stringBuilder.append(pkgName);
                stringBuilder.append(" Resolution = ");
                stringBuilder.append(VR_APP_RATIO);
                Slog.w(str, stringBuilder.toString());
                return VR_APP_RATIO;
            } else if (info == null || (info.getSwitchable() && mLowResolutionMode == 0)) {
                return -1.0f;
            } else {
                int defaultWidth = SystemProperties.getInt("persist.sys.aps.defaultWidth", 0);
                int curWidth = SystemProperties.getInt("persist.sys.rog.width", 0);
                float ratio = info.getResolutionRatio();
                if (!(defaultWidth == 0 || curWidth == 0 || defaultWidth == curWidth || HwVRUtils.isVRLowPowerApp(pkgName))) {
                    ratio = ratio >= ((float) curWidth) / ((float) defaultWidth) ? -1.0f : (((float) defaultWidth) * ratio) / ((float) curWidth);
                }
                return ratio;
            }
        }
    }

    public int getTextureLocked(String pkgName) {
        if (pkgName == null) {
            try {
                Slog.e(TAG, "getTextureLocked input invalid param!");
                return -1;
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("APS, HwApsManagerServiceConfig, getTextureLocked, exception:");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
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
            Slog.e(TAG, "getFpsLocked input invalid param!");
            return -1;
        }
        ApsAppInfo info = (ApsAppInfo) this.mPackages.get(pkgName);
        if (info == null) {
            return -1;
        }
        return info.getFrameRatio();
    }

    public int getMaxFpsLocked(String pkgName) {
        if (pkgName == null) {
            Slog.e(TAG, "getMaxFpsLocked input invalid param!");
            return -1;
        }
        ApsAppInfo info = (ApsAppInfo) this.mPackages.get(pkgName);
        if (info == null) {
            return -1;
        }
        return info.getMaxFrameRatio();
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
                    ApsAppInfo apsAppInfo = (ApsAppInfo) this.mPackages.get(pkgName);
                }
            }
        }
        return false;
    }

    public boolean isSupportApsColorPlusLocked(String pkgName) {
        return (pkgName == null || this.mApsColorPlusPkgList == null || !this.mApsColorPlusPkgList.contains(pkgName)) ? false : true;
    }

    public int setColorPlusPkgListLocked(List<String> pkgList) {
        try {
            this.mApsColorPlusPkgList = Collections.synchronizedList(pkgList);
            return 0;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("APS, HwApsManagerServiceConfig, setColorPlusPkgListLocked, exception:");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            e.printStackTrace();
            return -6;
        }
    }

    private void scheduleWrite() {
        this.mHandler.removeMessages(300);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(300), MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
    }

    /* JADX WARNING: Removed duplicated region for block: B:36:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x00e4  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void saveApsAppInfo() {
        HashMap<String, ApsAppInfo> pkgs;
        IOException e;
        synchronized (this.mApsService) {
            pkgs = new HashMap(this.mPackages);
        }
        String str = null;
        FileOutputStream fos = null;
        HashMap<String, ApsAppInfo> pkgs2;
        try {
            fos = this.mFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            out.startTag(null, "hwaps-compat-packages");
            for (Entry<String, ApsAppInfo> entry : pkgs.entrySet()) {
                String pkg = (String) entry.getKey();
                ApsAppInfo apsInfo = (ApsAppInfo) entry.getValue();
                if (apsInfo == null) {
                    pkgs2 = pkgs;
                } else {
                    out.startTag(str, "pkg");
                    out.attribute(str, "name", pkg);
                    float rr = apsInfo.getResolutionRatio();
                    int fr = apsInfo.getFrameRatio();
                    int maxfr = apsInfo.getMaxFrameRatio();
                    int tp = apsInfo.getTexturePercent();
                    int bp = apsInfo.getBrightnessPercent();
                    boolean sa = apsInfo.getSwitchable();
                    pkgs2 = pkgs;
                    try {
                        out.attribute(null, "resolutionratio", Float.toString(rr));
                        out.attribute(null, "framerate", Integer.toString(fr));
                        out.attribute(null, "maxframerate", Integer.toString(maxfr));
                        out.attribute(null, "texturepercent", Integer.toString(tp));
                        out.attribute(null, "brightnesspercent", Integer.toString(bp));
                        out.attribute(null, "switchable", Boolean.toString(sa));
                        out.endTag(null, "pkg");
                    } catch (IOException e2) {
                        e = e2;
                        Slog.e(TAG, "Error writing hwaps compat packages", e);
                        if (fos == null) {
                        }
                    }
                }
                pkgs = pkgs2;
                str = null;
            }
            out.endTag(null, "hwaps-compat-packages");
            out.endDocument();
            this.mFile.finishWrite(fos);
        } catch (IOException e3) {
            e = e3;
            pkgs2 = pkgs;
            Slog.e(TAG, "Error writing hwaps compat packages", e);
            if (fos == null) {
                this.mFile.failWrite(fos);
            }
        }
    }
}
