package com.huawei.hwwifiproservice;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.wifi.HwHiLog;
import com.android.server.wifipro.WifiProCommonUtils;

public class HwWifiProFeatureControl {
    private static final boolean DEFAULT_WIFI_PRO_ENABLED = false;
    private static final String KEY_WIFI_PRO_PROPERTY = "ro.config.hw_wifipro_feature";
    private static final int MSG_FIRST_PROBE_RESULT = 1;
    private static final long RAM_LIMIT_OF_DEVICE = 1073741824;
    private static final String TAG = "HwWifiProFeatureControl";
    private static final int WIFIPRO_11V_ROAMING_CTL = 11;
    private static final int WIFIPRO_DISTANCE_OPEN_CTL = 64;
    private static final int WIFIPRO_DUAL_BAND_CTL = 7;
    private static final int WIFIPRO_FEATURE_NOT_CONFIG = 65535;
    private static final int WIFIPRO_FULL_FEATURE_CTL = 1023;
    private static final int WIFIPRO_LITE_CTL = 384;
    private static final int WIFIPRO_N0_FEATURE = 0;
    private static final int WIFIPRO_OPEN_EVALUTE_CTL = 17;
    private static final int WIFIPRO_PORTAL_CTL = 128;
    private static final String WIFIPRO_PROPERTY = "hw_wifipro_enable";
    private static final int WIFIPRO_SCANGENIE_CTL = 512;
    private static final int WIFIPRO_SELFCURE_CTL = 256;
    private static final int WIFIPRO_TO_CELLULAR_CTL = 33;
    private static final int WIFIPRO_TO_WIFI_CTL = 3;
    private static final int WIFIPRO_WIFI_HANDOVER_CTL = 1;
    private static HwWifiProFeatureControl mHwWifiProFeatureControl = null;
    private static int mWifiProFeatureCtl = 0;
    protected static boolean sWifiProDualBandCtrl = false;
    protected static boolean sWifiProFixedLocationOpenCtrl = false;
    protected static boolean sWifiProHandoverCtrl = false;
    protected static boolean sWifiProOpenApEvaluateCtrl = false;
    protected static boolean sWifiProPortalCtrl = false;
    protected static boolean sWifiProRoamingCtrl = false;
    protected static boolean sWifiProScanGenieCtrl = false;
    protected static boolean sWifiProSelfCureCtrl = false;
    protected static boolean sWifiProToCellularCtrl = false;
    protected static boolean sWifiProToWifiCtrl = false;
    /* access modifiers changed from: private */
    public ContentResolver mContentResolver = null;
    private Context mContext = null;
    private WifiConfiguration mCurrentWifiConfig;
    private HwIntelligenceWiFiManager mHwIntelligenceWiFiManager = null;
    private IntentFilter mIntentFilter = null;
    /* access modifiers changed from: private */
    public boolean mIsWiFiProEnabled = false;
    private WifiProHandler mWifiProHandler;
    private WifiProStateMachine mWifiProStateMachine = null;
    private Messenger mWifiStateMachineMessenger = null;

    private HwWifiProFeatureControl(Context context) {
        this.mContext = context;
        this.mIsWiFiProEnabled = WifiProCommonUtils.isWifiProSwitchOn(context);
        this.mContentResolver = context.getContentResolver();
    }

    public void init() {
        HwHiLog.i(TAG, false, "Enter init.", new Object[0]);
        autoConfigWifiProLiteProperty();
        HandlerThread handlerThread = new HandlerThread("wifipro_feature_control_handler_thread");
        handlerThread.start();
        this.mWifiProHandler = new WifiProHandler(handlerThread.getLooper());
        if (SystemProperties.getInt(KEY_WIFI_PRO_PROPERTY, (int) WIFIPRO_FEATURE_NOT_CONFIG) != WIFIPRO_FEATURE_NOT_CONFIG) {
            mWifiProFeatureCtl = SystemProperties.getInt(KEY_WIFI_PRO_PROPERTY, (int) WIFIPRO_FEATURE_NOT_CONFIG);
        } else if (WifiProCommonUtils.isWifiProPropertyEnabled(this.mContext)) {
            mWifiProFeatureCtl = WIFIPRO_FULL_FEATURE_CTL;
        } else if (WifiProCommonUtils.isWifiProLitePropertyEnabled(this.mContext)) {
            mWifiProFeatureCtl = WIFIPRO_LITE_CTL;
        } else {
            mWifiProFeatureCtl = 0;
        }
        parseWifiProProperty(mWifiProFeatureCtl);
    }

    public static HwWifiProFeatureControl getInstance() {
        return mHwWifiProFeatureControl;
    }

    public static HwWifiProFeatureControl getInstance(Context context) {
        if (mHwWifiProFeatureControl == null) {
            mHwWifiProFeatureControl = new HwWifiProFeatureControl(context);
        }
        return mHwWifiProFeatureControl;
    }

    private void registerForSettingsChanges() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(WifiProCommonUtils.KEY_WIFI_PRO_SWITCH), false, new ContentObserver(null) {
            /* class com.huawei.hwwifiproservice.HwWifiProFeatureControl.AnonymousClass1 */

            public void onChange(boolean selfChange) {
                HwWifiProFeatureControl hwWifiProFeatureControl = HwWifiProFeatureControl.this;
                boolean unused = hwWifiProFeatureControl.mIsWiFiProEnabled = HwWifiProFeatureControl.getSettingsSystemBoolean(hwWifiProFeatureControl.mContentResolver, WifiProCommonUtils.KEY_WIFI_PRO_SWITCH, false);
                HwHiLog.d(HwWifiProFeatureControl.TAG, false, "Wifi pro setting has changed,WiFiProEnabled == %{public}s", new Object[]{String.valueOf(HwWifiProFeatureControl.this.mIsWiFiProEnabled)});
                if (HwWifiProFeatureControl.this.mIsWiFiProEnabled) {
                    HwWifiProFeatureControl.this.starWifiProFeature();
                } else {
                    HwWifiProFeatureControl.this.stopWifiProFeature();
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public static boolean getSettingsSystemBoolean(ContentResolver cr, String name, boolean def) {
        return Settings.System.getInt(cr, name, def ? 1 : 0) == 1;
    }

    private void startWifiProStateMachine() {
        if (this.mWifiStateMachineMessenger == null) {
            HwHiLog.i(TAG, false, "mWifiStateMachineMessenger is null", new Object[0]);
            return;
        }
        HwHiLog.i(TAG, false, "createWifiProStateMachine", new Object[0]);
        this.mWifiProStateMachine = WifiProStateMachine.createWifiProStateMachine(this.mContext, this.mWifiStateMachineMessenger, this.mWifiProHandler.getLooper());
    }

    private int parseWifiProProperty(int flag) {
        HwHiLog.i(TAG, false, "feature flag= %{public}d", new Object[]{Integer.valueOf(flag)});
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 78, new Bundle());
        if (result == null) {
            HwHiLog.d(TAG, false, "parseWifiProProperty fail,Bundle is null", new Object[0]);
            return 0;
        }
        this.mWifiStateMachineMessenger = (Messenger) result.getParcelable("WifiStateMachineMessenger");
        if (isFeatureEnabled(1)) {
            sWifiProHandoverCtrl = true;
        } else if (WifiProCommonUtils.isWifiProLitePropertyEnabled(this.mContext)) {
            HwHiLog.i(TAG, false, "startWifiProLite", new Object[0]);
            HwWifiproLiteStateMachine.getInstance(this.mContext, this.mWifiStateMachineMessenger, this.mWifiProHandler.getLooper()).setup();
        }
        if (sWifiProHandoverCtrl) {
            if (isFeatureEnabled(7)) {
                sWifiProDualBandCtrl = true;
            }
            if (isFeatureEnabled(11)) {
                sWifiProRoamingCtrl = true;
            }
            if (isFeatureEnabled(3)) {
                sWifiProToWifiCtrl = true;
            }
            if (isFeatureEnabled(17)) {
                sWifiProOpenApEvaluateCtrl = true;
            }
            if (isFeatureEnabled(33)) {
                sWifiProToCellularCtrl = true;
            }
            HwHiLog.i(TAG, false, "startWifiProStateMachine", new Object[0]);
            startWifiProStateMachine();
        }
        if (isFeatureEnabled(64)) {
            HwHiLog.i(TAG, false, "startDistanceOpen", new Object[0]);
            sWifiProFixedLocationOpenCtrl = true;
            startDistanceOpen();
            registerForSettingsChanges();
        }
        if (isFeatureEnabled(128)) {
            HwHiLog.i(TAG, false, "startPortal", new Object[0]);
            sWifiProPortalCtrl = true;
            startPortal();
        }
        if (isFeatureEnabled(256)) {
            HwHiLog.i(TAG, false, "startSelfCure", new Object[0]);
            sWifiProSelfCureCtrl = true;
            startSelfCure();
        }
        if (isFeatureEnabled(512)) {
            HwHiLog.i(TAG, false, "startWifiScanGenie", new Object[0]);
            sWifiProScanGenieCtrl = true;
            startWifiScanGenie();
        }
        HwHiLog.i(TAG, false, "%{public}s", new Object[]{"sWifiProScanGenieCtrl=" + sWifiProScanGenieCtrl + ", sWifiProToWifiCtrl=" + sWifiProToWifiCtrl + ", sWifiProDualBandCtrl=" + sWifiProDualBandCtrl + ", sWifiProRoamingCtrl=" + sWifiProRoamingCtrl + ", sWifiProOpenApEvaluateCtrl=" + sWifiProOpenApEvaluateCtrl + ", sWifiProToCellularCtrl=" + sWifiProToCellularCtrl + ", sWifiProFixedLocationOpenCtrl=" + sWifiProFixedLocationOpenCtrl + ", sWifiProPortalCtrl=" + sWifiProPortalCtrl + ", sWifiProSelfCureCtrl=" + sWifiProSelfCureCtrl + ", sWifiProScanGenieCtrl=" + sWifiProScanGenieCtrl});
        return 0;
    }

    private boolean isFeatureEnabled(int subFeature) {
        return (mWifiProFeatureCtl & subFeature) == subFeature;
    }

    /* access modifiers changed from: private */
    public void starWifiProFeature() {
        HwIntelligenceWiFiManager hwIntelligenceWiFiManager = this.mHwIntelligenceWiFiManager;
        if (hwIntelligenceWiFiManager != null) {
            hwIntelligenceWiFiManager.start();
        }
    }

    /* access modifiers changed from: private */
    public void stopWifiProFeature() {
        HwIntelligenceWiFiManager hwIntelligenceWiFiManager = this.mHwIntelligenceWiFiManager;
        if (hwIntelligenceWiFiManager != null) {
            hwIntelligenceWiFiManager.stop();
        }
    }

    public static boolean isSelfCureOngoing() {
        if (HwSelfCureEngine.getInstance() != null) {
            return HwSelfCureEngine.getInstance().isSelfCureOngoing();
        }
        return false;
    }

    public static void notifyInternetAccessRecovery() {
        if (HwSelfCureEngine.getInstance() != null) {
            HwSelfCureEngine.getInstance().notifyInternetAccessRecovery();
        }
    }

    public static void notifyInternetFailureDetected(boolean forceNoHttpCheck) {
        if (HwSelfCureEngine.getInstance() != null) {
            HwSelfCureEngine.getInstance().notifyInternetFailureDetected(forceNoHttpCheck);
        }
    }

    private void startWifiScanGenie() {
        WifiScanGenieController.createWifiScanGenieControllerImpl(this.mContext);
    }

    private void startPortal() {
        if (this.mWifiProStateMachine != null) {
            HwAutoConnectManager.getInstance(this.mContext, null).init(this.mWifiProHandler.getLooper());
        } else {
            HwAutoConnectManager.getInstance(this.mContext, null).init(null);
        }
        HwHiLog.d(TAG, false, "System Create WifiProStateMachine begin to initialize portal database.", new Object[0]);
        PortalDataBaseManager.getInstance(this.mContext);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 29, new Bundle());
    }

    private void startDistanceOpen() {
        this.mHwIntelligenceWiFiManager = HwIntelligenceWiFiManager.createInstance(this.mContext);
        if (this.mIsWiFiProEnabled) {
            this.mHwIntelligenceWiFiManager.start();
        }
    }

    private void startSelfCure() {
        HwHiLog.i(TAG, false, "start create HwSelfCureEngine", new Object[0]);
        HwSelfCureEngine.getInstance(this.mContext).setup();
    }

    public void notifyFirstConnectProbeResult(int respCode) {
        WifiProHandler wifiProHandler = this.mWifiProHandler;
        wifiProHandler.sendMessage(Message.obtain(wifiProHandler, 1, respCode, 0));
    }

    private class WifiProHandler extends Handler {
        WifiProHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                int respCode = msg.arg1;
                HwHiLog.i(HwWifiProFeatureControl.TAG, false, "MSG_FIRST_PROBE_RESULT = %{public}d", new Object[]{Integer.valueOf(respCode)});
                HwWifiProFeatureControl.this.notifyProbeResultByBroadcast(respCode);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyProbeResultByBroadcast(int finalRespCode) {
        WifiConfiguration wifiConfiguration;
        this.mCurrentWifiConfig = WifiProCommonUtils.getCurrentWifiConfig((WifiManager) this.mContext.getSystemService("wifi"));
        if (WifiProCommonUtils.httpReachableOrRedirected(finalRespCode)) {
            int property = finalRespCode == 204 ? 5 : 6;
            sendCheckResultWhenConnected(finalRespCode, WifiProCommonDefs.ACTION_NETWOR_PROPERTY_NOTIFICATION, WifiProCommonDefs.EXTRA_FLAG_NETWORK_PROPERTY, property);
            if (property == 5 && (wifiConfiguration = this.mCurrentWifiConfig) != null && WifiProCommonUtils.matchedRequestByHistory(wifiConfiguration.internetHistory, 102)) {
                property = 6;
            }
            sendCheckResultWhenConnected(finalRespCode, WifiProCommonDefs.ACTION_NETWORK_CONDITIONS_MEASURED, WifiProCommonDefs.EXTRA_IS_INTERNET_READY, property);
        } else if (finalRespCode == 599) {
            sendCheckResultWhenConnected(finalRespCode, WifiProCommonDefs.ACTION_NETWOR_PROPERTY_NOTIFICATION, WifiProCommonDefs.EXTRA_FLAG_NETWORK_PROPERTY, -1);
            sendCheckResultWhenConnected(finalRespCode, WifiProCommonDefs.ACTION_NETWORK_CONDITIONS_MEASURED, WifiProCommonDefs.EXTRA_IS_INTERNET_READY, -1);
        } else {
            HwHiLog.i(TAG, false, "the first probe result %{public}d, is abnormal.", new Object[]{Integer.valueOf(finalRespCode)});
        }
    }

    private void sendCheckResultWhenConnected(int finalRespCode, String action, String flag, int property) {
        Intent intent = new Intent(action);
        intent.setFlags(67108864);
        intent.putExtra(flag, property);
        if (WifiProCommonDefs.EXTRA_FLAG_NETWORK_PROPERTY.equals(flag)) {
            boolean isFirstDetected = true;
            if (property == 6) {
                intent.putExtra(WifiProCommonDefs.EXTRA_STANDARD_PORTAL_NETWORK, true);
            }
            WifiConfiguration wifiConfiguration = this.mCurrentWifiConfig;
            if (wifiConfiguration == null || !WifiProCommonUtils.matchedRequestByHistory(wifiConfiguration.internetHistory, 103)) {
                isFirstDetected = false;
            }
            intent.putExtra(WifiProCommonUtils.KEY_PORTAL_HTTP_RESP_CODE, finalRespCode);
            intent.putExtra(WifiProCommonUtils.KEY_PORTAL_FIRST_DETECT, isFirstDetected);
            String str = "";
            intent.putExtra(WifiProCommonUtils.KEY_PORTAL_REDIRECTED_URL, str);
            WifiConfiguration wifiConfiguration2 = this.mCurrentWifiConfig;
            if (wifiConfiguration2 != null) {
                str = wifiConfiguration2.configKey();
            }
            intent.putExtra(WifiProCommonUtils.KEY_PORTAL_CONFIG_KEY, str);
        }
        this.mContext.sendBroadcast(intent, WifiProCommonDefs.NETWORK_CHECKER_RECV_PERMISSION);
    }

    private void autoConfigWifiProLiteProperty() {
        ActivityManager activityManager;
        String wifiProProperty;
        Context context = this.mContext;
        if (context != null && (activityManager = (ActivityManager) context.getSystemService("activity")) != null && (wifiProProperty = Settings.Global.getString(this.mContext.getContentResolver(), WIFIPRO_PROPERTY)) != null && "true".equalsIgnoreCase(wifiProProperty)) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            if (memoryInfo.totalMem > 0 && memoryInfo.totalMem < RAM_LIMIT_OF_DEVICE) {
                Settings.Global.putString(this.mContext.getContentResolver(), WIFIPRO_PROPERTY, "lite");
            }
        }
    }
}
