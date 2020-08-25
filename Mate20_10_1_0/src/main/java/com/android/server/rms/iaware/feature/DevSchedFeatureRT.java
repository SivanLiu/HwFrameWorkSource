package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.IDeviceSettingCallback;
import android.rms.iaware.LogIAware;
import android.rms.iaware.NetLocationStrategy;
import android.util.ArrayMap;
import com.android.server.rms.iaware.AwareCallback;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.cpu.CPUCustBaseConfig;
import com.android.server.rms.iaware.dev.BLESchedFeatureRT;
import com.android.server.rms.iaware.dev.DevSchedCallbackManager;
import com.android.server.rms.iaware.dev.DevSchedFeatureBase;
import com.android.server.rms.iaware.dev.FeatureXmlConfigParserRT;
import com.android.server.rms.iaware.dev.LCDSchedFeatureRT;
import com.android.server.rms.iaware.dev.NetLocationSchedFeatureRT;
import com.android.server.rms.iaware.dev.PhoneStatusRecong;
import com.android.server.rms.iaware.dev.ScreenOnWakelockSchedFeatureRT;
import com.huawei.android.app.IHwActivityNotifierEx;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DevSchedFeatureRT extends RFeature {
    private static final String APP_SWITCH_REASON = "appSwitch";
    private static final int BASE_VERSION2 = 2;
    private static final int BASE_VERSION3 = 3;
    public static final String BLE_FEATURE = "ble_iconnect_nearby";
    private static final String DISABLE_VALUE = "0";
    private static final String ENABLE_VALUE = "1";
    public static final String LCD_FEATURE = "LcdCommon";
    public static final String MODEM_FEATURE = "modem";
    private static final int MSG_NAVI_STATUS = 4;
    private static final int MSG_REPORT_DATA = 1;
    private static final int MSG_TOP_APP_SWITCH = 5;
    private static final String TAG = "DevSchedFeatureRT";
    private static final String TOP_APP_INTENT_RESON = "android.intent.extra.REASON";
    private static final String TOP_APP_PID = "toPid";
    private static final String TOP_APP_PKG = "toPackage";
    private static final String TOP_APP_UID = "toUid";
    public static final String WAKELOCK_FEATURE = "ScreenOnWakelock";
    public static final String WIFI_FEATURE = "wifi";
    /* access modifiers changed from: private */
    public static final AtomicBoolean sRunning = new AtomicBoolean(false);
    private static final Map<String, Class<?>> sSubFeatureCustObj = new ArrayMap();
    private static final Map<String, DevSchedFeatureBase> sSubFeatureObjMap = new ArrayMap();
    private static final Map<String, Class<?>> sSubFeaturePlatformObj = new ArrayMap();
    private AwareStateCallback mAwareStateCallback = null;
    private Context mContext = null;
    /* access modifiers changed from: private */
    public DevSchedHandler mDevSchedHandler = null;
    private TopAppCallBack mTopAppCallBack = null;

    static {
        sSubFeatureCustObj.put(LCD_FEATURE, LCDSchedFeatureRT.class);
    }

    public DevSchedFeatureRT(Context context, AwareConstant.FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
        this.mContext = context;
        this.mDevSchedHandler = new DevSchedHandler();
        AwareLog.d(TAG, "create DevSchedFeatureRT success.");
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        AwareLog.d(TAG, "enableFeatureEx realVersion=" + realVersion);
        if (realVersion >= 3) {
            enableIaware3();
        } else if (realVersion >= 2) {
            enableIaware2();
        } else {
            AwareLog.i(TAG, "enableFeatureEx failed, realVersion: " + realVersion);
            return false;
        }
        registerTopAppCallBack();
        sRunning.set(true);
        return true;
    }

    private void enableIaware2() {
        sSubFeatureObjMap.clear();
        createEnabledSubFeature(sSubFeatureCustObj, true);
        createEnabledSubFeature(sSubFeaturePlatformObj, false);
        PhoneStatusRecong.getInstance().init(this.mContext);
        PhoneStatusRecong.getInstance().connectService();
        subscribleEvents();
        registerStateCallback();
        AwareLog.d(TAG, "enableIaware2 SUCCESS");
    }

    private void enableIaware3() {
        sSubFeaturePlatformObj.put(WIFI_FEATURE, NetLocationSchedFeatureRT.class);
        sSubFeaturePlatformObj.put(MODEM_FEATURE, NetLocationSchedFeatureRT.class);
        sSubFeatureCustObj.put(BLE_FEATURE, BLESchedFeatureRT.class);
        sSubFeaturePlatformObj.put(WAKELOCK_FEATURE, ScreenOnWakelockSchedFeatureRT.class);
        enableIaware2();
        AwareLog.d(TAG, "enableIaware3 SUCCESS");
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        AwareLog.d(TAG, "DevSchedFeatureRT, enable.");
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        AwareLog.d(TAG, "DevSchedFeatureRT, disable.");
        sRunning.set(false);
        unregisterStateCallback();
        unSubscribeEvents();
        PhoneStatusRecong.getInstance().disconnectService();
        unregisterTopAppCallBack();
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = data;
        this.mDevSchedHandler.sendMessage(msg);
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean custConfigUpdate() {
        return true;
    }

    private void subscribleEvents() {
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
            this.mIRDataRegister.subscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
        }
    }

    private void unSubscribeEvents() {
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.unSubscribeData(AwareConstant.ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
        }
    }

    private class TopAppCallBack extends IHwActivityNotifierEx {
        private TopAppCallBack() {
        }

        public void call(Bundle extras) {
            if (extras != null && DevSchedFeatureRT.APP_SWITCH_REASON.equals(extras.getString(DevSchedFeatureRT.TOP_APP_INTENT_RESON))) {
                Message msg = Message.obtain();
                msg.what = 5;
                msg.obj = extras;
                DevSchedFeatureRT.this.mDevSchedHandler.sendMessage(msg);
            }
        }
    }

    private void registerTopAppCallBack() {
        if (this.mTopAppCallBack == null) {
            this.mTopAppCallBack = new TopAppCallBack();
            AwareCallback.getInstance().registerActivityNotifier(this.mTopAppCallBack, APP_SWITCH_REASON);
        }
    }

    private void unregisterTopAppCallBack() {
        if (this.mTopAppCallBack != null) {
            AwareCallback.getInstance().unregisterActivityNotifier(this.mTopAppCallBack, APP_SWITCH_REASON);
            this.mTopAppCallBack = null;
        }
    }

    private boolean isConfigEnable(FeatureXmlConfigParserRT.FeatureXmlConfig config) {
        return config == null || !config.subSwitch;
    }

    private void createAndInitFeatureInstance(String subFeature, Class<?> classObj, FeatureXmlConfigParserRT.FeatureXmlConfig config) {
        try {
            Object obj = classObj.getConstructors()[0].newInstance(this.mContext, subFeature);
            if (obj == null) {
                return;
            }
            if (obj instanceof DevSchedFeatureBase) {
                DevSchedFeatureBase subFeatureObj = (DevSchedFeatureBase) obj;
                subFeatureObj.readFeatureConfig(config);
                sSubFeatureObjMap.put(subFeature, subFeatureObj);
            }
        } catch (ArrayIndexOutOfBoundsException | IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException err) {
            AwareLog.e(TAG, "createEnabledSubFeature Exception " + err.getMessage());
        }
    }

    private void createEnabledSubFeature(Map<String, Class<?>> featureMap, boolean isCust) {
        String subFeature;
        Class<?> classObj;
        if (!featureMap.isEmpty()) {
            for (Map.Entry<String, Class<?>> entry : featureMap.entrySet()) {
                if (!(entry == null || (subFeature = entry.getKey()) == null)) {
                    FeatureXmlConfigParserRT.FeatureXmlConfig config = FeatureXmlConfigParserRT.parseFeatureXmlConfig(subFeature, !isCust);
                    if (!isConfigEnable(config) && (classObj = entry.getValue()) != null) {
                        createAndInitFeatureInstance(subFeature, classObj, config);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public class DevSchedHandler extends Handler {
        private DevSchedHandler() {
        }

        public void handleMessage(Message msg) {
            if (!DevSchedFeatureRT.sRunning.get()) {
                AwareLog.i(DevSchedFeatureRT.TAG, "DevSchedHandler, feature sRunning is false, return!");
            } else if (msg == null) {
                AwareLog.e(DevSchedFeatureRT.TAG, "DevSchedHandler, msg is null, error!");
            } else {
                Object dataObj = msg.obj;
                int i = msg.what;
                if (i != 1) {
                    if (i == 4) {
                        DevSchedFeatureRT.this.handleNaviStatus(msg.arg1);
                    } else if (i != 5) {
                        AwareLog.i(DevSchedFeatureRT.TAG, "DevSchedHandler, default branch, msg.what is " + msg.what);
                    } else if (dataObj instanceof Bundle) {
                        DevSchedFeatureRT.this.handleTopAppSwitch((Bundle) dataObj);
                    }
                } else if (dataObj instanceof CollectData) {
                    DevSchedFeatureRT.this.handleReportData((CollectData) dataObj);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleReportData(CollectData data) {
        if (data != null) {
            data.getTimeStamp();
            AwareConstant.ResourceType type = AwareConstant.ResourceType.getResourceType(data.getResId());
            if (type == AwareConstant.ResourceType.RESOURCE_SCREEN_ON || type == AwareConstant.ResourceType.RESOURCE_SCREEN_OFF) {
                DevSchedFeatureBase.setScreenState(type == AwareConstant.ResourceType.RESOURCE_SCREEN_ON ? DevSchedFeatureBase.ScreenState.ScreenOn : DevSchedFeatureBase.ScreenState.ScreenOff);
                for (DevSchedFeatureBase subFeatureObj : sSubFeatureObjMap.values()) {
                    if (subFeatureObj != null) {
                        subFeatureObj.handleScreenStateChange(type == AwareConstant.ResourceType.RESOURCE_SCREEN_ON ? DevSchedFeatureBase.ScreenState.ScreenOn : DevSchedFeatureBase.ScreenState.ScreenOff);
                    }
                }
                return;
            }
            AwareLog.d(TAG, "no register event");
        }
    }

    /* access modifiers changed from: private */
    public void handleNaviStatus(int eventType) {
        boolean isInNavi = true;
        if (eventType != 1) {
            isInNavi = false;
        }
        for (DevSchedFeatureBase subFeatureObj : sSubFeatureObjMap.values()) {
            if (subFeatureObj != null) {
                subFeatureObj.handleNaviStatus(isInNavi);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleTopAppSwitch(Bundle bundle) {
        if (bundle != null) {
            int pid = bundle.getInt(TOP_APP_PID, -1);
            int uid = bundle.getInt(TOP_APP_UID, -1);
            String pkgName = bundle.getString(TOP_APP_PKG);
            if (pid < 0 || uid < 0 || pkgName == null) {
                AwareLog.w(TAG, "handleTopAppSwitch pid or uid or pkgName invalid!");
                return;
            }
            reportTopAppToAwareNrt(pid, uid, pkgName);
            for (DevSchedFeatureBase subFeatureObj : sSubFeatureObjMap.values()) {
                if (subFeatureObj != null) {
                    subFeatureObj.handleTopAppChange(pid, uid, pkgName);
                }
            }
        }
    }

    private void reportTopAppToAwareNrt(int pid, int uid, String pkgName) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(String.valueOf(pid));
        stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(String.valueOf(uid));
        stringBuffer.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        stringBuffer.append(pkgName);
        LogIAware.report(2100, stringBuffer.toString());
    }

    public static NetLocationStrategy getNetLocationStrategy(String pkgName, int uid, int type) {
        String feature;
        if (!sRunning.get()) {
            AwareLog.i(TAG, "getNetLocationStrategy, sRunning is false, return!");
            return null;
        }
        if (type == 1) {
            feature = WIFI_FEATURE;
        } else if (type != 2) {
            AwareLog.i(TAG, "getNetLocationStrategy, Wrong Location type, return!");
            return null;
        } else {
            feature = MODEM_FEATURE;
        }
        DevSchedFeatureBase netLocationFeature = sSubFeatureObjMap.get(feature);
        if (netLocationFeature == null) {
            AwareLog.i(TAG, "netLocationFeature is null. feature:" + feature);
            return null;
        } else if (netLocationFeature instanceof NetLocationSchedFeatureRT) {
            return ((NetLocationSchedFeatureRT) netLocationFeature).getNetLocationStrategy(pkgName, uid);
        } else {
            AwareLog.i(TAG, "netLocationFeature is not obj of NetLocationSchedFeatureRT, wifiFeature : " + netLocationFeature.getClass().getName());
            return null;
        }
    }

    public static boolean isAwarePreventWakelockScreenOn(String pkgName, String tag) {
        if (!sRunning.get()) {
            AwareLog.i(TAG, "isAwarePreventWakelockScreenOn, sRunning is false, return!");
            return false;
        }
        DevSchedFeatureBase wakelockFeature = sSubFeatureObjMap.get(WAKELOCK_FEATURE);
        if (wakelockFeature == null) {
            AwareLog.i(TAG, "wakelockFeature is null.");
            return false;
        } else if (wakelockFeature instanceof ScreenOnWakelockSchedFeatureRT) {
            return ((ScreenOnWakelockSchedFeatureRT) wakelockFeature).isAwarePreventWakelockScreenOn(pkgName, tag);
        } else {
            AwareLog.i(TAG, "wakelockFeature is not obj of ScreenOnWakelockSchedFeatureRT, wakelockFeature : " + wakelockFeature.getClass().getName());
            return false;
        }
    }

    public static int getAppTypeForLCD(String pkgName) {
        if (!sRunning.get()) {
            AwareLog.i(TAG, "getAppTypeForLCD, sRunning is false, return!");
            return 255;
        }
        DevSchedFeatureBase lcdFeature = sSubFeatureObjMap.get(LCD_FEATURE);
        if (lcdFeature == null) {
            AwareLog.i(TAG, "lcdFeature is null.");
            return 255;
        } else if (lcdFeature instanceof LCDSchedFeatureRT) {
            return ((LCDSchedFeatureRT) lcdFeature).getAppType(pkgName);
        } else {
            AwareLog.i(TAG, "lcdFeature is not obj of LCDSchedFeatureRT, lcdFeature : " + lcdFeature.getClass().getName());
            return 255;
        }
    }

    private void registerStateCallback() {
        if (this.mAwareStateCallback == null) {
            this.mAwareStateCallback = new AwareStateCallback();
            AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 3);
        }
    }

    private void unregisterStateCallback() {
        if (this.mAwareStateCallback != null) {
            AwareAppKeyBackgroup.getInstance().unregisterStateCallback(this.mAwareStateCallback, 3);
            this.mAwareStateCallback = null;
        }
    }

    private class AwareStateCallback implements AwareAppKeyBackgroup.IAwareStateCallback {
        private AwareStateCallback() {
        }

        @Override // com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup.IAwareStateCallback
        public void onStateChanged(int stateType, int eventType, int pid, int uid) {
            if (!DevSchedFeatureRT.sRunning.get() || DevSchedFeatureRT.this.mDevSchedHandler == null) {
                AwareLog.i(DevSchedFeatureRT.TAG, "DevSchedHandler, feature sRunning is false, return!");
            } else if (stateType == 3) {
                Message msg = Message.obtain();
                msg.what = 4;
                msg.arg1 = eventType;
                DevSchedFeatureRT.this.mDevSchedHandler.sendMessage(msg);
            }
        }
    }

    public static void registerDevModeMethod(int deviceId, IDeviceSettingCallback callback, Bundle args) {
        if (!sRunning.get()) {
            AwareLog.i(TAG, "registerDevModeMethod, sRunning is false, return!");
        } else {
            DevSchedCallbackManager.getInstance().registerDevModeMethod(deviceId, callback, args);
        }
    }

    public static void unregisterDevModeMethod(int deviceId, IDeviceSettingCallback callback, Bundle args) {
        if (!sRunning.get()) {
            AwareLog.i(TAG, "unregisterDevModeMethod, sRunning is false, return!");
        } else {
            DevSchedCallbackManager.getInstance().unregisterDevModeMethod(deviceId, callback, args);
        }
    }

    public static boolean isDeviceIdAvailable(int deviceId) {
        if (!sRunning.get()) {
            AwareLog.i(TAG, "isDeviceIdAvailable, sRunning is false, return!");
            return false;
        } else if (getDeviceObjById(deviceId) == null) {
            return false;
        } else {
            return true;
        }
    }

    public static void sendCurrentDeviceMode(int deviceId) {
        if (!sRunning.get()) {
            AwareLog.i(TAG, "sendCurrentDeviceMode, sRunning is false, return!");
            return;
        }
        DevSchedFeatureBase deviceObj = getDeviceObjById(deviceId);
        if (deviceObj == null) {
            AwareLog.i(TAG, "sendCurrentDeviceMode, no deviceObj for deviceId:" + deviceId);
            return;
        }
        deviceObj.sendCurrentDeviceMode();
    }

    private static DevSchedFeatureBase getDeviceObjById(int deviceId) {
        if (deviceId == -1) {
            return null;
        }
        for (DevSchedFeatureBase subFeatureObj : sSubFeatureObjMap.values()) {
            if (subFeatureObj != null && subFeatureObj.getDeviceId() == deviceId) {
                return subFeatureObj;
            }
        }
        return null;
    }
}
