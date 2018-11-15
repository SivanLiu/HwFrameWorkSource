package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.IDeviceSettingCallback;
import android.rms.iaware.NetLocationStrategy;
import android.util.ArrayMap;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup.IAwareStateCallback;
import com.android.server.rms.iaware.dev.BLESchedFeatureRT;
import com.android.server.rms.iaware.dev.DevSchedCallbackManager;
import com.android.server.rms.iaware.dev.DevSchedFeatureBase;
import com.android.server.rms.iaware.dev.DevSchedFeatureBase.ScreenState;
import com.android.server.rms.iaware.dev.DevXmlConfig;
import com.android.server.rms.iaware.dev.GpsSchedFeatureRT;
import com.android.server.rms.iaware.dev.LCDSchedFeatureRT;
import com.android.server.rms.iaware.dev.NetLocationSchedFeatureRT;
import com.android.server.rms.iaware.dev.PhoneStatusRecong;
import com.android.server.rms.iaware.dev.ScreenOnWakelockSchedFeatureRT;
import com.android.server.rms.iaware.memory.data.content.AttrSegments;
import com.android.server.rms.iaware.memory.data.content.AttrSegments.Builder;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public class DevSchedFeatureRT extends RFeature {
    private static final int BASE_VERSION2 = 2;
    private static final int BASE_VERSION3 = 3;
    private static final String BLE_FEATURE = "ble";
    private static final String DISABLE_VALUE = "0";
    private static final int DUMP_WIFI_ARG_LENGTH = 4;
    public static final int DUMP_WIFI_ARG_LENGTH_STEP = 2;
    private static final String ENABLE_VALUE = "1";
    private static final String GPS_FEATURE = "gps";
    private static final String LCD_FEATURE = "lcd";
    private static final String MODEM_FEATURE = "modem";
    private static final int MSG_NAVI_STATUS = 4;
    private static final int MSG_REPORT_DATA = 1;
    private static final int MSG_UPDATE_CONFIG = 2;
    private static final String TAG = "DevSchedFeatureRT";
    private static final String WAKELOCK_FEATURE = "wakelock";
    private static final String WIFI_DUMPSYS_PARAM = "--test-dev-wifi";
    private static final String WIFI_FEATURE = "wifi";
    private static final AtomicBoolean mRunning = new AtomicBoolean(false);
    private static final Map<String, Class<?>> mSubFeatureObj = new ArrayMap();
    private static final Map<String, DevSchedFeatureBase> mSubFeatureObjMap = new ArrayMap();
    private AwareStateCallback mAwareStateCallback = null;
    private Context mContext = null;
    private DevSchedHandler mDevSchedHandler = null;
    private final Map<String, String> mSubFeatureSwitch = new ArrayMap();

    private class DevSchedHandler extends Handler {
        private DevSchedHandler() {
        }

        public void handleMessage(Message msg) {
            if (!DevSchedFeatureRT.mRunning.get()) {
                AwareLog.i(DevSchedFeatureRT.TAG, "DevSchedHandler, feature mRunning is false, return!");
            } else if (msg == null) {
                AwareLog.e(DevSchedFeatureRT.TAG, "DevSchedHandler, msg is null, error!");
            } else {
                int i = msg.what;
                if (i != 4) {
                    switch (i) {
                        case 1:
                            if (msg.obj instanceof CollectData) {
                                DevSchedFeatureRT.this.handlerReportData(msg.obj);
                                break;
                            }
                            return;
                        case 2:
                            DevSchedFeatureRT.this.handlerUpdateCustConfig();
                            break;
                        default:
                            String str = DevSchedFeatureRT.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("DevSchedHandler, default branch, msg.what is ");
                            stringBuilder.append(msg.what);
                            AwareLog.i(str, stringBuilder.toString());
                            return;
                    }
                }
                DevSchedFeatureRT.this.handlerNaviStatus(msg.arg1);
            }
        }
    }

    private class AwareStateCallback implements IAwareStateCallback {
        private AwareStateCallback() {
        }

        public void onStateChanged(int stateType, int eventType, int pid, int uid) {
            if (!DevSchedFeatureRT.mRunning.get() || DevSchedFeatureRT.this.mDevSchedHandler == null) {
                AwareLog.i(DevSchedFeatureRT.TAG, "DevSchedHandler, feature mRunning is false, return!");
            } else if (stateType == 3) {
                Message msg = Message.obtain();
                msg.what = 4;
                msg.arg1 = eventType;
                DevSchedFeatureRT.this.mDevSchedHandler.sendMessage(msg);
            }
        }
    }

    static {
        mSubFeatureObj.put(GPS_FEATURE, GpsSchedFeatureRT.class);
        mSubFeatureObj.put(LCD_FEATURE, LCDSchedFeatureRT.class);
    }

    public DevSchedFeatureRT(Context context, FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
        this.mContext = context;
        this.mDevSchedHandler = new DevSchedHandler();
        AwareLog.d(TAG, "create DevSchedFeatureRT success.");
    }

    public boolean enableFeatureEx(int realVersion) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableFeatureEx realVersion=");
        stringBuilder.append(realVersion);
        AwareLog.d(str, stringBuilder.toString());
        if (realVersion >= 3) {
            enableIAware3();
        } else if (realVersion >= 2) {
            enableIAware2();
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("enableFeatureEx failed, realVersion: ");
            stringBuilder.append(realVersion);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        }
        mRunning.set(true);
        return true;
    }

    private void enableIAware2() {
        DevXmlConfig.loadSubFeatureSwitch(this.mSubFeatureSwitch);
        createEnabledSubFeature(mSubFeatureObjMap);
        PhoneStatusRecong.getInstance().connectService(this.mContext);
        subscribleEvents();
        registerStateCallback();
        AwareLog.d(TAG, "enableIAware2 SUCCESS");
    }

    private void enableIAware3() {
        mSubFeatureObj.put(WIFI_FEATURE, NetLocationSchedFeatureRT.class);
        mSubFeatureObj.put(MODEM_FEATURE, NetLocationSchedFeatureRT.class);
        mSubFeatureObj.put(BLE_FEATURE, BLESchedFeatureRT.class);
        mSubFeatureObj.put(WAKELOCK_FEATURE, ScreenOnWakelockSchedFeatureRT.class);
        enableIAware2();
        AwareLog.d(TAG, "enableIAware3 SUCCESS");
    }

    public boolean enable() {
        AwareLog.d(TAG, "DevSchedFeatureRT, enable.");
        return false;
    }

    public boolean disable() {
        AwareLog.d(TAG, "DevSchedFeatureRT, disable.");
        mRunning.set(false);
        unregisterStateCallback();
        unSubscribeEvents();
        PhoneStatusRecong.getInstance().disconnectService();
        return true;
    }

    public boolean reportData(CollectData data) {
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = data;
        this.mDevSchedHandler.sendMessage(msg);
        return true;
    }

    public boolean custConfigUpdate() {
        this.mDevSchedHandler.sendEmptyMessage(2);
        return true;
    }

    private void subscribleEvents() {
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.subscribeData(ResourceType.RES_APP, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
            this.mIRDataRegister.subscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
        }
    }

    private void unSubscribeEvents() {
        if (this.mIRDataRegister != null) {
            this.mIRDataRegister.unSubscribeData(ResourceType.RES_APP, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_ON, this.mFeatureType);
            this.mIRDataRegister.unSubscribeData(ResourceType.RESOURCE_SCREEN_OFF, this.mFeatureType);
        }
    }

    private void createEnabledSubFeature(Map<String, DevSchedFeatureBase> subFeatureObjMap) {
        if (subFeatureObjMap != null) {
            subFeatureObjMap.clear();
            if (mSubFeatureObj.size() != 0) {
                for (Entry<String, Class<?>> entry : mSubFeatureObj.entrySet()) {
                    if (entry != null) {
                        String subFeature = (String) entry.getKey();
                        if (subFeature != null) {
                            if (isSubFeatureEnable(subFeature)) {
                                Class<?> classObj = (Class) entry.getValue();
                                if (classObj != null) {
                                    try {
                                        subFeatureObjMap.put(subFeature, (DevSchedFeatureBase) classObj.getConstructors()[0].newInstance(new Object[]{this.mContext, subFeature}));
                                    } catch (IllegalArgumentException e) {
                                        AwareLog.e(TAG, "createEnabledSubFeature IllegalArgumentException");
                                    } catch (IllegalAccessException e2) {
                                        AwareLog.e(TAG, "createEnabledSubFeature IllegalAccessException");
                                    } catch (InstantiationException e3) {
                                        AwareLog.e(TAG, "createEnabledSubFeature InstantiationException");
                                    } catch (InvocationTargetException e4) {
                                        AwareLog.e(TAG, "createEnabledSubFeature InvocationTargetException");
                                    } catch (ArrayIndexOutOfBoundsException e5) {
                                        AwareLog.e(TAG, "createEnabledSubFeature ArrayIndexOutOfBoundsException");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isSubFeatureEnable(String subFeature) {
        if (subFeature == null || subFeature.isEmpty()) {
            return false;
        }
        if ("1".equals((String) this.mSubFeatureSwitch.get(subFeature))) {
            return true;
        }
        return false;
    }

    private void handlerReportData(CollectData data) {
        if (data != null) {
            long timestamp = data.getTimeStamp();
            ResourceType type = ResourceType.getResourceType(data.getResId());
            if (type == ResourceType.RES_APP) {
                String eventData = data.getData();
                Builder builder = new Builder();
                builder.addCollectData(eventData);
                AttrSegments attrSegments = builder.build();
                if (attrSegments.isValid()) {
                    for (DevSchedFeatureBase subFeatureObj : mSubFeatureObjMap.values()) {
                        if (subFeatureObj != null) {
                            subFeatureObj.handleResAppData(timestamp, attrSegments.getEvent().intValue(), attrSegments);
                        }
                    }
                } else {
                    AwareLog.e(TAG, "Invalid collectData, or event");
                }
            } else if (type == ResourceType.RESOURCE_SCREEN_ON || type == ResourceType.RESOURCE_SCREEN_OFF) {
                DevSchedFeatureBase.setScreenState(type == ResourceType.RESOURCE_SCREEN_ON ? ScreenState.ScreenOn : ScreenState.ScreenOff);
                for (DevSchedFeatureBase subFeatureObj2 : mSubFeatureObjMap.values()) {
                    if (subFeatureObj2 != null) {
                        subFeatureObj2.handScreenStateChange(type == ResourceType.RESOURCE_SCREEN_ON ? ScreenState.ScreenOn : ScreenState.ScreenOff);
                    }
                }
            }
        }
    }

    private void handlerUpdateCustConfig() {
        for (DevSchedFeatureBase subFeatureObj : mSubFeatureObjMap.values()) {
            if (subFeatureObj != null) {
                subFeatureObj.handleUpdateCustConfig();
            }
        }
    }

    private void handlerNaviStatus(int eventType) {
        boolean isInNavi = true;
        if (1 != eventType) {
            isInNavi = false;
        }
        for (DevSchedFeatureBase subFeatureObj : mSubFeatureObjMap.values()) {
            if (subFeatureObj != null) {
                subFeatureObj.handlerNaviStatus(isInNavi);
            }
        }
    }

    public static NetLocationStrategy getNetLocationStrategy(String pkgName, int uid, int type) {
        if (mRunning.get()) {
            String feature;
            switch (type) {
                case 1:
                    feature = WIFI_FEATURE;
                    break;
                case 2:
                    feature = MODEM_FEATURE;
                    break;
                default:
                    AwareLog.i(TAG, "getNetLocationStrategy, Wrong Location type, return!");
                    return null;
            }
            DevSchedFeatureBase netLocationFeature = (DevSchedFeatureBase) mSubFeatureObjMap.get(feature);
            String str;
            StringBuilder stringBuilder;
            if (netLocationFeature == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("netLocationFeature is null. feature:");
                stringBuilder.append(feature);
                AwareLog.i(str, stringBuilder.toString());
                return null;
            } else if (netLocationFeature instanceof NetLocationSchedFeatureRT) {
                return ((NetLocationSchedFeatureRT) netLocationFeature).getNetLocationStrategy(pkgName, uid);
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("netLocationFeature is not obj of NetLocationSchedFeatureRT, wifiFeature : ");
                stringBuilder.append(netLocationFeature.getClass().getName());
                AwareLog.i(str, stringBuilder.toString());
                return null;
            }
        }
        AwareLog.i(TAG, "getNetLocationStrategy, mRunning is false, return!");
        return null;
    }

    public static boolean isAwarePreventWakelockScreenOn(String pkgName, String tag) {
        if (mRunning.get()) {
            DevSchedFeatureBase wakelockFeature = (DevSchedFeatureBase) mSubFeatureObjMap.get(WAKELOCK_FEATURE);
            if (wakelockFeature == null) {
                AwareLog.i(TAG, "wakelockFeature is null.");
                return false;
            } else if (wakelockFeature instanceof ScreenOnWakelockSchedFeatureRT) {
                return ((ScreenOnWakelockSchedFeatureRT) wakelockFeature).isAwarePreventWakelockScreenOn(pkgName, tag);
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("wakelockFeature is not obj of ScreenOnWakelockSchedFeatureRT, wakelockFeature : ");
                stringBuilder.append(wakelockFeature.getClass().getName());
                AwareLog.i(str, stringBuilder.toString());
                return false;
            }
        }
        AwareLog.i(TAG, "isAwarePreventWakelockScreenOn, mRunning is false, return!");
        return false;
    }

    public static int getAppTypeForLCD(String pkgName) {
        if (mRunning.get()) {
            DevSchedFeatureBase lcdFeature = (DevSchedFeatureBase) mSubFeatureObjMap.get(LCD_FEATURE);
            if (lcdFeature == null) {
                AwareLog.i(TAG, "lcdFeature is null.");
                return 255;
            } else if (lcdFeature instanceof LCDSchedFeatureRT) {
                return ((LCDSchedFeatureRT) lcdFeature).getAppType(pkgName);
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("lcdFeature is not obj of LCDSchedFeatureRT, lcdFeature : ");
                stringBuilder.append(lcdFeature.getClass().getName());
                AwareLog.i(str, stringBuilder.toString());
                return 255;
            }
        }
        AwareLog.i(TAG, "getAppTypeForLCD, mRunning is false, return!");
        return 255;
    }

    public static final boolean doDumpsys(RFeature feature, FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mRunning.get()) {
            boolean isValidParam = args != null && args.length > 0 && feature != null && (feature instanceof DevSchedFeatureRT);
            if (!isValidParam) {
                return false;
            }
            if (WIFI_DUMPSYS_PARAM.equals(args[0])) {
                doDumpsysWifi(args);
            }
            return true;
        }
        AwareLog.i(TAG, "doDumpsys, mRunning is false, return!");
        return false;
    }

    private static void doDumpsysWifi(String[] args) {
        if (args == null) {
            return;
        }
        if (args.length == 4 || args.length == 6) {
            PhoneStatusRecong.getInstance().doDumpsys(args);
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

    public static void registerDevModeMethod(int deviceId, IDeviceSettingCallback callback, Bundle args) {
        if (mRunning.get()) {
            DevSchedCallbackManager.getInstance().registerDevModeMethod(deviceId, callback, args);
        } else {
            AwareLog.i(TAG, "registerDevModeMethod, mRunning is false, return!");
        }
    }

    public static void unregisterDevModeMethod(int deviceId, IDeviceSettingCallback callback, Bundle args) {
        if (mRunning.get()) {
            DevSchedCallbackManager.getInstance().unregisterDevModeMethod(deviceId, callback, args);
        } else {
            AwareLog.i(TAG, "unregisterDevModeMethod, mRunning is false, return!");
        }
    }

    public static boolean checkDeviceIdAvailable(int deviceId) {
        if (!mRunning.get()) {
            AwareLog.i(TAG, "checkDeviceIdAvailable, mRunning is false, return!");
            return false;
        } else if (getDeviceObjById(deviceId) == null) {
            return false;
        } else {
            return true;
        }
    }

    public static void sendCurrentDeviceMode(int deviceId) {
        if (mRunning.get()) {
            DevSchedFeatureBase deviceObj = getDeviceObjById(deviceId);
            if (deviceObj == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendCurrentDeviceMode, no deviceObj for deviceId:");
                stringBuilder.append(deviceId);
                AwareLog.i(str, stringBuilder.toString());
                return;
            }
            deviceObj.sendCurrentDeviceMode();
            return;
        }
        AwareLog.i(TAG, "sendCurrentDeviceMode, mRunning is false, return!");
    }

    private static DevSchedFeatureBase getDeviceObjById(int deviceId) {
        if (-1 == deviceId) {
            return null;
        }
        for (DevSchedFeatureBase subFeatureObj : mSubFeatureObjMap.values()) {
            if (subFeatureObj != null) {
                if (subFeatureObj.getDeviceId() == deviceId) {
                    return subFeatureObj;
                }
            }
        }
        return null;
    }
}
