package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.StaticIpConfiguration;
import android.net.ip.IpClient;
import android.net.ip.IpClient.ProvisioningConfiguration;
import android.net.wifi.HwInnerNetworkManagerImpl;
import android.net.wifi.ParcelUtil;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiDetectConfInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiSsid;
import android.net.wifi.wifipro.HwNetworkAgent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import android.util.LruCache;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.IState;
import com.android.server.hidata.mplink.HwMpLinkContentAware;
import com.android.server.os.GetUDIDNative;
import com.android.server.wifi.ABS.HwABSDetectorService;
import com.android.server.wifi.ABS.HwABSUtils;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifi.MSS.HwMSSArbitrager;
import com.android.server.wifi.MSS.HwMSSHandler;
import com.android.server.wifi.WifiNative.SignalPollResult;
import com.android.server.wifi.WifiNative.TxPacketCounters;
import com.android.server.wifi.WifiStateMachine.ObtainingIpState;
import com.android.server.wifi.util.StringUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.android.server.wifi.util.WifiCommonUtils;
import com.android.server.wifi.wifipro.HwAutoConnectManager;
import com.android.server.wifi.wifipro.HwDualBandManager;
import com.android.server.wifi.wifipro.WifiHandover;
import com.android.server.wifi.wifipro.WifiProConfigStore;
import com.android.server.wifi.wifipro.WifiProStateMachine;
import com.android.server.wifi.wifipro.WifiproUtils;
import com.android.server.wifi.wifipro.hwintelligencewifi.HwIntelligenceWiFiManager;
import com.android.server.wifi.wifipro.wifiscangenie.WifiScanGenieController;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONException;
import org.json.JSONObject;

public class HwWifiStateMachine extends WifiStateMachine {
    public static final int AP_CAP_CACHE_COUNT = 1000;
    public static final String AP_CAP_KEY = "AP_CAP";
    private static final String ASSOCIATION_REJECT_STATUS_CODE = "wifi_association_reject_status_code";
    private static final String BRCM_CHIP_4359 = "bcm4359";
    public static final String BSSID_KEY = "BSSID";
    public static final int CMD_AP_STARTED_GET_STA_LIST = 131104;
    public static final int CMD_AP_STARTED_SET_DISASSOCIATE_STA = 131106;
    public static final int CMD_AP_STARTED_SET_MAC_FILTER = 131105;
    static final int CMD_GET_CHANNEL_LIST_5G = 131572;
    public static final int CMD_SCREEN_OFF_SCAN = 131578;
    public static final int CMD_STOP_WIFI_REPEATER = 131577;
    public static final int CMD_UPDATE_WIFIPRO_CONFIGURATIONS = 131672;
    private static final String DBKEY_HOTSPOT20_VALUE = "hw_wifi_hotspot2_on";
    private static final int DEFAULT_ARP_DETECT_TIME = 5;
    private static final int DEFAULT_ARP_TIMEOUT_MS = 1000;
    private static final int DHCP_RESULT_CACHE_SIZE = 50;
    public static final int ENTERPRISE_HOTSPOT_THRESHOLD = 4;
    private static final String HIGEO_PACKAGE_NAME = "com.huawei.lbs";
    private static final int HIGEO_STATE_DEFAULT_MODE = 0;
    private static final int HIGEO_STATE_WIFI_SCAN_MODE = 1;
    private static final String HUAWEI_SETTINGS = "com.android.settings.Settings$WifiSettingsActivity";
    public static final int PM_LOWPWR = 7;
    public static final int PM_NORMAL = 6;
    public static final int SCAN_ONLY_CONNECT_MODE = 100;
    private static final String SOFTAP_IFACE = "wlan0";
    private static final int SUCCESS = 1;
    public static final String SUPPLICANT_WAPI_EVENT = "android.net.wifi.supplicant.WAPI_EVENT";
    private static final String TAG = "HwWifiStateMachine";
    private static final long TIMEOUT_CONTROL_SCAN_ASSOCIATED = 5000;
    private static final long TIMEOUT_CONTROL_SCAN_ASSOCIATING = 2000;
    public static final String TX_MCS_SET = "TX_MCS_SET";
    private static final String USB_SUPPLY = "/sys/class/power_supply/USB/online";
    private static final String USB_SUPPLY_QCOM = "/sys/class/power_supply/usb/online";
    public static final int WAPI_AUTHENTICATION_FAILURE_EVENT = 147474;
    public static final int WAPI_CERTIFICATION_FAILURE_EVENT = 147475;
    public static final int WAPI_EVENT_AUTH_FAIL_CODE = 16;
    public static final int WAPI_EVENT_CERT_FAIL_CODE = 17;
    private static final String WIFI_EVALUATE_TAG = "wifipro_recommending_access_points";
    private static final int WIFI_GLOBAL_SCAN_CTRL_FOUL_INTERVAL = 5000;
    private static final int WIFI_GLOBAL_SCAN_CTRL_FREED_INTERVAL = 10000;
    private static final int WIFI_LINK_DETECT_CNT = 3;
    private static final int WIFI_MAX_FOUL_TIMES = 5;
    private static final int WIFI_MAX_FREED_TIMES = 5;
    private static final long WIFI_SCAN_BLACKLIST_REMOVE_INTERVAL = 7200000;
    private static final String WIFI_SCAN_CONNECTED_LIMITED_WHITE_PACKAGENAME = "wifi_scan_connected_limited_white_packagename";
    private static final String WIFI_SCAN_INTERVAL_WHITE_WLAN_CONNECTED = "wifi_scan_interval_white_wlan_connected";
    private static final String WIFI_SCAN_INTERVAL_WLAN_CLOSE = "wifi_scan_interval_wlan_close";
    private static final long WIFI_SCAN_INTERVAL_WLAN_CLOSE_DEFAULT = 20000;
    private static final String WIFI_SCAN_INTERVAL_WLAN_NOT_CONNECTED = "wifi_scan_interval_wlan_not_connected";
    private static final long WIFI_SCAN_INTERVAL_WLAN_NOT_CONNECTED_DEFAULT = 10000;
    private static final long WIFI_SCAN_INTERVAL_WLAN_WHITE_CONNECTED_DEFAULT = 10000;
    private static final long WIFI_SCAN_OVER_INTERVAL_MAX_COUNT = 10;
    private static final long WIFI_SCAN_RESULT_DELAY_TIME_DEFAULT = 300;
    private static final String WIFI_SCAN_WHITE_PACKAGENAME = "wifi_scan_white_packagename";
    private static final int WIFI_START_EVALUATE_TAG = 1;
    private static final int WIFI_STOP_EVALUATE_TAG = 0;
    private static int mFrequency = 0;
    private static WifiNativeUtils wifiNativeUtils = ((WifiNativeUtils) EasyInvokeFactory.getInvokeUtils(WifiNativeUtils.class));
    private static WifiStateMachineUtils wifiStateMachineUtils = ((WifiStateMachineUtils) EasyInvokeFactory.getInvokeUtils(WifiStateMachineUtils.class));
    private boolean isInGlobalScanCtrl = false;
    private long lastConnectTime = -1;
    private HashMap<String, String> lastDhcps = new HashMap();
    private long lastScanResultTimestamp = 0;
    private ActivityManager mActivityManager;
    private int mBQEUid;
    private BroadcastReceiver mBcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int PluggedType;
            HwWifiStateMachine hwWifiStateMachine;
            String action = intent.getAction();
            String chipName = SystemProperties.get("ro.connectivity.sub_chiptype", "");
            WifiInfo wifiInfo = HwWifiStateMachine.wifiStateMachineUtils.getWifiInfo(HwWifiStateMachine.this);
            boolean isMobileAP = HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(HwWifiStateMachine.this.myContext);
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                PluggedType = intent.getIntExtra("plugged", 0);
                if (PluggedType == 2 || PluggedType == 5) {
                    HwWifiStateMachine.this.mIsScanCtrlPluggedin = true;
                } else if (!HwWifiStateMachine.this.getChargingState()) {
                    HwWifiStateMachine.this.mIsScanCtrlPluggedin = false;
                }
                hwWifiStateMachine = HwWifiStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mBcastReceiver: PluggedType = ");
                stringBuilder.append(PluggedType);
                stringBuilder.append(" mIsScanCtrlPluggedin = ");
                stringBuilder.append(HwWifiStateMachine.this.mIsScanCtrlPluggedin);
                hwWifiStateMachine.logd(stringBuilder.toString());
            } else if (!(!"android.intent.action.SCREEN_OFF".equals(action) || wifiInfo == null || wifiInfo.getNetworkId() == -1 || HwWifiStateMachine.this.getChargingState() || HwWifiStateMachine.this.isWifiRepeaterStarted())) {
                Log.d(HwWifiStateMachine.TAG, "SCREEN_OFF, startFilteringMulticastPackets");
                HwWifiStateMachine.this.setScreenOffMulticastFilter(true);
            }
            if (HwWifiStateMachine.BRCM_CHIP_4359.equals(chipName)) {
                StringBuilder stringBuilder2;
                if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                    PluggedType = intent.getIntExtra("plugged", 0);
                    if (PluggedType == 2 || PluggedType == 5) {
                        HwWifiStateMachine.this.mIsChargePluggedin = true;
                    } else {
                        HwWifiStateMachine.this.mIsChargePluggedin = false;
                        HwWifiStateMachine.this.mIsAllowedManualPwrBoost = 0;
                    }
                } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (networkInfo != null) {
                        switch (AnonymousClass6.$SwitchMap$android$net$NetworkInfo$DetailedState[networkInfo.getDetailedState().ordinal()]) {
                            case 1:
                                HwWifiStateMachine.this.logd("setpmlock:CONNECTED");
                                HwWifiStateMachine.this.mWifiConnectState = true;
                                if (wifiInfo != null) {
                                    HwWifiStateMachine.this.mSsid = wifiInfo.getSSID();
                                }
                                HwWifiStateMachine.this.setLowPwrMode(HwWifiStateMachine.this.mWifiConnectState, HwWifiStateMachine.this.mSsid, isMobileAP, HwWifiStateMachine.this.mScreenState);
                                break;
                            case 2:
                                HwWifiStateMachine.this.logd("setpmlock:DISCONNECTED");
                                HwWifiStateMachine.this.mWifiConnectState = false;
                                HwWifiStateMachine.this.setLowPwrMode(HwWifiStateMachine.this.mWifiConnectState, null, isMobileAP, HwWifiStateMachine.this.mScreenState);
                                break;
                        }
                    }
                } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                    hwWifiStateMachine = HwWifiStateMachine.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setpmlock:action = ");
                    stringBuilder2.append(action);
                    hwWifiStateMachine.logd(stringBuilder2.toString());
                } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    hwWifiStateMachine = HwWifiStateMachine.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setpmlock:action = ");
                    stringBuilder2.append(action);
                    hwWifiStateMachine.logd(stringBuilder2.toString());
                }
            }
        }
    };
    private ConnectivityManager mConnMgr = null;
    public boolean mCurrNetworkHistoryInserted = false;
    private int mCurrentConfigNetId = -1;
    private String mCurrentConfigurationKey = null;
    private boolean mCurrentPwrBoostStat = false;
    private boolean mDelayWifiScoreBySelfCureOrSwitch = false;
    private Queue<IState> mDestStates = null;
    private final LruCache<String, DhcpResults> mDhcpResultCache = new LruCache(50);
    private int mFoulTimes = 0;
    private int mFreedTimes = 0;
    private HiLinkController mHiLinkController = null;
    private HwInnerNetworkManagerImpl mHwInnerNetworkManagerImpl;
    private HwSoftApManager mHwSoftApManager;
    private HwWifiCHRService mHwWifiCHRService;
    public int mIsAllowedManualPwrBoost = 0;
    private boolean mIsChargePluggedin = false;
    private boolean mIsFinishLinkDetect = false;
    private boolean mIsScanCtrlPluggedin = false;
    private long mLastScanTimestamp = 0;
    private int mLastTxPktCnt = 0;
    private HashMap<String, Integer> mPidBlackList = new HashMap();
    private long mPidBlackListInteval = 0;
    private HashMap<String, Integer> mPidConnectedBlackList = new HashMap();
    private HashMap<Integer, Long> mPidLastScanSuccTimestamp = new HashMap();
    private HashMap<Integer, Long> mPidLastScanTimestamp = new HashMap();
    private HashMap<Integer, Integer> mPidWifiScanCount = new HashMap();
    private int mPwrBoostOffcnt = 0;
    private int mPwrBoostOncnt = 0;
    private AtomicBoolean mRenewDhcpSelfCuring = new AtomicBoolean(false);
    private int mScreenOffScanToken = 0;
    private boolean mScreenState = true;
    public WifiConfiguration mSelectedConfig = null;
    private DetailedState mSelfCureNetworkLastState = DetailedState.IDLE;
    private int mSelfCureWifiConnectRetry = 0;
    private int mSelfCureWifiLastState = -1;
    private String mSsid = null;
    private long mTimeLastCtrlScanDuringObtainingIp = 0;
    private long mTimeOutScanControlForAssoc = 0;
    private long mTimeStampScanControlForAssoc = 0;
    public boolean mUserCloseWifiWhenSelfCure = false;
    private WifiSsid mWiFiProRoamingSSID = null;
    public boolean mWifiAlwaysOnBeforeCure = false;
    public boolean mWifiBackgroundConnected = false;
    private boolean mWifiConnectState = false;
    private WifiDetectConfInfo mWifiDetectConfInfo = new WifiDetectConfInfo();
    private int mWifiDetectperiod = -1;
    private long mWifiEnabledTimeStamp = 0;
    private int mWifiSelfCureState = 0;
    private AtomicBoolean mWifiSelfCuring = new AtomicBoolean(false);
    private AtomicBoolean mWifiSoftSwitchRunning = new AtomicBoolean(false);
    public boolean mWifiSwitchOnGoing = false;
    private HashMap<String, Boolean> mapApCapChr = new HashMap();
    private HwMSSArbitrager mssArbi = null;
    private Context myContext;
    private final Object selectConfigLock = new Object();
    private boolean usingStaticIpConfig = false;
    private int wifiConnectedBackgroundReason = 0;
    private WifiEapUIManager wifiEapUIManager;

    /* renamed from: com.android.server.wifi.HwWifiStateMachine$6 */
    static /* synthetic */ class AnonymousClass6 {
        static final /* synthetic */ int[] $SwitchMap$android$net$NetworkInfo$DetailedState = new int[DetailedState.values().length];

        static {
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.DISCONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private class FilterScanRunnable implements Runnable {
        List<ScanDetail> lstScanRet = null;

        public FilterScanRunnable(List<ScanDetail> lstScan) {
            this.lstScanRet = lstScan;
        }

        /* JADX WARNING: Missing block: B:31:0x00ff, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            String strCurBssid = HwWifiStateMachine.this.getCurrentBSSID();
            if (strCurBssid != null && !strCurBssid.isEmpty() && !HwWifiStateMachine.this.mapApCapChr.containsKey(strCurBssid) && this.lstScanRet != null && this.lstScanRet.size() != 0) {
                for (ScanDetail scanned : this.lstScanRet) {
                    if (scanned != null) {
                        String strBssid = scanned.getBSSIDString();
                        if (strBssid == null) {
                            continue;
                        } else if (!strBssid.isEmpty()) {
                            if (strCurBssid.equals(strBssid)) {
                                int stream1 = scanned.getNetworkDetail().getStream1();
                                int stream2 = scanned.getNetworkDetail().getStream2();
                                int stream3 = scanned.getNetworkDetail().getStream3();
                                int stream4 = scanned.getNetworkDetail().getStream4();
                                int txMcsSet = scanned.getNetworkDetail().getTxMcsSet();
                                int value = ((scanned.getNetworkDetail().getPrimaryFreq() / 1000) * 10) + Math.abs(((stream1 + stream2) + stream3) + stream4);
                                String strJSON = new StringBuilder();
                                strJSON.append("{BSSID:\"");
                                strJSON.append(strCurBssid);
                                strJSON.append("\",");
                                strJSON.append(HwWifiStateMachine.AP_CAP_KEY);
                                strJSON.append(":");
                                strJSON.append(value);
                                strJSON.append(",");
                                strJSON.append(HwWifiStateMachine.TX_MCS_SET);
                                strJSON.append(":");
                                strJSON.append(txMcsSet);
                                strJSON.append("}");
                                HwWifiStateMachine.this.mHwWifiCHRService.updateWifiException(213, strJSON.toString());
                                HwWifiStateMachine.this.mapApCapChr.put(strCurBssid, Boolean.valueOf(true));
                                if (HwWifiStateMachine.this.mapApCapChr.size() > 1000) {
                                    HwWifiStateMachine.this.mapApCapChr.clear();
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private class pwrBoostHandler extends Handler {
        private static final int PWR_BOOST_END_MSG = 1;
        private static final int PWR_BOOST_MANUAL_DISABLE = 0;
        private static final int PWR_BOOST_MANUAL_ENABLE = 1;
        private static final int PWR_BOOST_START_MSG = 0;

        pwrBoostHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (!HwWifiStateMachine.this.mWifiConnectState) {
                        HwWifiStateMachine.this.clearPwrBoostChrStatus();
                        return;
                    } else if (!HwWifiStateMachine.this.mIsChargePluggedin || !HwWifiStateMachine.this.mIsFinishLinkDetect) {
                        if (HwWifiStateMachine.this.mIsChargePluggedin && !HwWifiStateMachine.this.mIsFinishLinkDetect) {
                            HwWifiStateMachine.this.mIsAllowedManualPwrBoost = 1;
                            if (!HwWifiStateMachine.this.mCurrentPwrBoostStat) {
                                WifiInjector.getInstance().getWifiNative().setPwrBoost(1);
                                HwWifiStateMachine.this.mCurrentPwrBoostStat = true;
                                HwWifiStateMachine.this.linkMeasureAndStatic(HwWifiStateMachine.this.mCurrentPwrBoostStat);
                                HwWifiStateMachine.this.mPwrBoostOncnt = HwWifiStateMachine.this.mPwrBoostOncnt + 1;
                            } else if (HwWifiStateMachine.this.mCurrentPwrBoostStat) {
                                WifiInjector.getInstance().getWifiNative().setPwrBoost(0);
                                HwWifiStateMachine.this.mCurrentPwrBoostStat = false;
                                HwWifiStateMachine.this.linkMeasureAndStatic(HwWifiStateMachine.this.mCurrentPwrBoostStat);
                                HwWifiStateMachine.this.mPwrBoostOffcnt = HwWifiStateMachine.this.mPwrBoostOffcnt + 1;
                            }
                        }
                        if (HwWifiStateMachine.this.mPwrBoostOncnt >= 3 && HwWifiStateMachine.this.mPwrBoostOffcnt >= 3) {
                            HwWifiStateMachine.this.mIsFinishLinkDetect = true;
                            break;
                        } else {
                            HwWifiStateMachine.this.mIsFinishLinkDetect = false;
                            break;
                        }
                    } else {
                        HwWifiStateMachine.this.mIsAllowedManualPwrBoost = 0;
                        return;
                    }
                    break;
                case 1:
                    HwWifiStateMachine.this.clearPwrBoostChrStatus();
                    break;
            }
        }
    }

    public HwWifiStateMachine(Context context, FrameworkFacade facade, Looper looper, UserManager userManager, WifiInjector wifiInjector, BackupManagerProxy backupManagerProxy, WifiCountryCode countryCode, WifiNative wifiNative, WrongPasswordNotifier wrongPasswordNotifier, SarManager sarManager) {
        Context context2 = context;
        super(context, facade, looper, userManager, wifiInjector, backupManagerProxy, countryCode, wifiNative, wrongPasswordNotifier, sarManager);
        this.myContext = context2;
        this.mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
        this.mBQEUid = 1000;
        this.mHwInnerNetworkManagerImpl = (HwInnerNetworkManagerImpl) HwFrameworkFactory.getHwInnerNetworkManager();
        registerReceiverInWifiPro(context2);
        registerForWifiEvaluateChanges();
        this.mssArbi = HwMSSArbitrager.getInstance(context2);
        if (WifiRadioPowerController.isRadioPowerEnabled()) {
            WifiRadioPowerController.setInstance(context2, this, wifiStateMachineUtils.getWifiNative(this), (HwInnerNetworkManagerImpl) HwFrameworkFactory.getHwInnerNetworkManager());
        }
        if (context2.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            registerForPasspointChanges();
        }
        this.mHiLinkController = new HiLinkController(context2, this);
        this.mActivityManager = (ActivityManager) context2.getSystemService("activity");
        this.mDestStates = new LinkedList();
        pwrBoostRegisterBcastReceiver();
        if (PreconfiguredNetworkManager.IS_R1) {
            this.wifiEapUIManager = new WifiEapUIManager(context2);
        }
        wifiStateMachineUtils.getWifiConfigManager(this).setSupportWapiType();
        this.mConnMgr = (ConnectivityManager) context2.getSystemService("connectivity");
    }

    public String getWpaSuppConfig() {
        log("WiFIStateMachine  getWpaSuppConfig InterfaceName ");
        if (this.myContext.checkCallingPermission("com.huawei.permission.ACCESS_AP_INFORMATION") == 0) {
            return WifiInjector.getInstance().getWifiNative().getWpaSuppConfig();
        }
        log("getWpaSuppConfig(): permissin deny");
        return null;
    }

    protected void enableAllNetworksByMode() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableAllNetworks mOperationalMode: ");
        stringBuilder.append(wifiStateMachineUtils.getOperationalMode(this));
        log(stringBuilder.toString());
        if (wifiStateMachineUtils.getOperationalMode(this) != 100) {
            WifiConfigStoreUtils.enableAllNetworks(wifiStateMachineUtils.getWifiConfigManager(this));
        }
    }

    protected void handleNetworkDisconnect() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handle network disconnect mOperationalMode: ");
        stringBuilder.append(wifiStateMachineUtils.getOperationalMode(this));
        log(stringBuilder.toString());
        if (wifiStateMachineUtils.getOperationalMode(this) == 100) {
            HwDisableLastNetwork();
        }
        log("handleNetworkDisconnect,resetWifiProManualConnect");
        resetWifiProManualConnect();
        super.handleNetworkDisconnect();
    }

    protected void loadAndEnableAllNetworksByMode() {
        if (wifiStateMachineUtils.getOperationalMode(this) == 100) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("supplicant connection mOperationalMode: ");
            stringBuilder.append(wifiStateMachineUtils.getOperationalMode(this));
            log(stringBuilder.toString());
            WifiConfigStoreUtils.loadConfiguredNetworks(wifiStateMachineUtils.getWifiConfigManager(this));
            WifiConfigStoreUtils.disableAllNetworks(wifiStateMachineUtils.getWifiConfigManager(this));
        } else {
            WifiConfigStoreUtils.loadAndEnableAllNetworks(wifiStateMachineUtils.getWifiConfigManager(this));
        }
        if (isWifiSelfCuring()) {
            updateNetworkId();
        }
    }

    private void HwDisableLastNetwork() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwDisableLastNetwork, currentState:");
        stringBuilder.append(getCurrentState());
        stringBuilder.append(", mLastNetworkId:");
        stringBuilder.append(wifiStateMachineUtils.getLastNetworkId(this));
        log(stringBuilder.toString());
    }

    protected boolean processScanModeSetMode(Message message, int mLastOperationMode) {
        if (message.arg1 != 100) {
            return false;
        }
        log("SCAN_ONLY_CONNECT_MODE, do not enable all networks here.");
        if (mLastOperationMode == 3) {
            wifiStateMachineUtils.setWifiState(this, 3);
            WifiConfigStoreUtils.loadConfiguredNetworks(wifiStateMachineUtils.getWifiConfigManager(this));
            AsyncChannel mWifiP2pChannel = wifiStateMachineUtils.getWifiP2pChannel(this);
            if (mWifiP2pChannel == null) {
                wifiStateMachineUtils.getAdditionalWifiServiceInterfaces(this);
                log("mWifiP2pChannel retry init");
            }
            if (mWifiP2pChannel != null) {
                mWifiP2pChannel.sendMessage(131203);
            } else {
                log("mWifiP2pChannel is null");
            }
        }
        wifiStateMachineUtils.setOperationalMode(this, 100);
        transitionTo(wifiStateMachineUtils.getDisconnectedState(this));
        return true;
    }

    protected boolean processConnectModeSetMode(Message message) {
        if (wifiStateMachineUtils.getOperationalMode(this) != 100 || message.arg2 != 0) {
            return false;
        }
        log("CMD_ENABLE_NETWORK command is ignored.");
        wifiStateMachineUtils.replyToMessage((WifiStateMachine) this, message, message.what, 1);
        return true;
    }

    protected boolean processL2ConnectedSetMode(Message message) {
        wifiStateMachineUtils.setOperationalMode(this, message.arg1);
        if (wifiStateMachineUtils.getOperationalMode(this) == 100) {
            if (!wifiStateMachineUtils.getNetworkInfo(this).isConnected()) {
                sendMessage(131145);
            }
            disableAllNetworksExceptLastConnected();
            return true;
        } else if (wifiStateMachineUtils.getOperationalMode(this) != 1) {
            return false;
        } else {
            WifiConfigStoreUtils.enableAllNetworks(wifiStateMachineUtils.getWifiConfigManager(this));
            return true;
        }
    }

    protected boolean processDisconnectedSetMode(Message message) {
        wifiStateMachineUtils.setOperationalMode(this, message.arg1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set operation mode mOperationalMode: ");
        stringBuilder.append(wifiStateMachineUtils.getOperationalMode(this));
        log(stringBuilder.toString());
        if (wifiStateMachineUtils.getOperationalMode(this) == 100) {
            WifiConfigStoreUtils.disableAllNetworks(wifiStateMachineUtils.getWifiConfigManager(this));
            return true;
        } else if (wifiStateMachineUtils.getOperationalMode(this) != 1) {
            return false;
        } else {
            WifiConfigStoreUtils.enableAllNetworks(wifiStateMachineUtils.getWifiConfigManager(this));
            wifiStateMachineUtils.getWifiNative(this).reconnect(wifiStateMachineUtils.getInterfaceName(this));
            return true;
        }
    }

    protected void enterConnectedStateByMode() {
        if (wifiStateMachineUtils.getOperationalMode(this) == 100) {
            log("wifi connected. disable other networks.");
            disableAllNetworksExceptLastConnected();
        }
    }

    protected boolean enterDriverStartedStateByMode() {
        if (wifiStateMachineUtils.getOperationalMode(this) != 100) {
            return false;
        }
        log("SCAN_ONLY_CONNECT_MODE, disable all networks.");
        wifiStateMachineUtils.getWifiNative(this).disconnect(wifiStateMachineUtils.getInterfaceName(this));
        WifiConfigStoreUtils.disableAllNetworks(wifiStateMachineUtils.getWifiConfigManager(this));
        transitionTo(wifiStateMachineUtils.getDisconnectedState(this));
        return true;
    }

    private void disableAllNetworksExceptLastConnected() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disable all networks except last connected. currentState:");
        stringBuilder.append(getCurrentState());
        stringBuilder.append(", mLastNetworkId:");
        stringBuilder.append(wifiStateMachineUtils.getLastNetworkId(this));
        log(stringBuilder.toString());
        for (WifiConfiguration network : WifiConfigStoreUtils.getConfiguredNetworks(wifiStateMachineUtils.getWifiConfigManager(this))) {
            if (network.networkId != wifiStateMachineUtils.getLastNetworkId(this)) {
                int i = network.status;
            }
        }
    }

    public void log(String message) {
        Log.d(TAG, message);
    }

    public boolean isScanAndManualConnectMode() {
        return wifiStateMachineUtils.getOperationalMode(this) == 100;
    }

    protected boolean processConnectModeAutoConnectByMode() {
        if (wifiStateMachineUtils.getOperationalMode(this) != 100) {
            return false;
        }
        log("CMD_AUTO_CONNECT command is ignored..");
        return true;
    }

    protected void recordAssociationRejectStatusCode(int statusCode) {
        System.putInt(this.myContext.getContentResolver(), ASSOCIATION_REJECT_STATUS_CODE, statusCode);
    }

    protected void startScreenOffScan() {
        int configNetworksSize = wifiStateMachineUtils.getWifiConfigManager(this).getSavedNetworks().size();
        if (!wifiStateMachineUtils.getScreenOn(this) && configNetworksSize > 0) {
            logd("begin scan when screen off");
            int i = this.mScreenOffScanToken + 1;
            this.mScreenOffScanToken = i;
            sendMessageDelayed(obtainMessage(CMD_SCREEN_OFF_SCAN, i, 0), wifiStateMachineUtils.getSupplicantScanIntervalMs(this));
        }
    }

    protected boolean processScreenOffScan(Message message) {
        if (CMD_SCREEN_OFF_SCAN != message.what) {
            return false;
        }
        if (message.arg1 == this.mScreenOffScanToken) {
            startScreenOffScan();
        }
        return true;
    }

    protected void makeHwDefaultIPTable(DhcpResults dhcpResults) {
        synchronized (this.mDhcpResultCache) {
            String key = wifiStateMachineUtils.getWifiInfo(this).getBSSID();
            if (key == null) {
                Log.w(TAG, "makeHwDefaultIPTable key is null!");
                return;
            }
            if (((DhcpResults) this.mDhcpResultCache.get(key)) != null) {
                log("make default IP configuration map, remove old rec.");
                this.mDhcpResultCache.remove(key);
            }
            boolean isPublicESS = false;
            int count = 0;
            String ssid = "";
            String capabilities = "";
            List<ScanResult> scanList = new ArrayList();
            if (WifiInjector.getInstance().getWifiStateMachineHandler().runWithScissors(new -$$Lambda$HwWifiStateMachine$iSDo2643LM7D37HI0i8CX3IwIOM(this, scanList), WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT)) {
                StringBuilder stringBuilder;
                try {
                    for (ScanResult result : scanList) {
                        if (key.equals(result.BSSID)) {
                            ssid = result.SSID;
                            capabilities = result.capabilities;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("ESS: SSID:");
                            stringBuilder.append(ssid);
                            stringBuilder.append(",capabilities:");
                            stringBuilder.append(capabilities);
                            log(stringBuilder.toString());
                            break;
                        }
                    }
                    for (ScanResult result2 : scanList) {
                        if (ssid.equals(result2.SSID) && capabilities.equals(result2.capabilities)) {
                            count++;
                            if (count >= 3) {
                                isPublicESS = true;
                            }
                        }
                    }
                } catch (Exception e) {
                }
                if (isPublicESS) {
                    log("current network is public ESS, dont make default IP");
                    return;
                }
                this.mDhcpResultCache.put(key, new DhcpResults(dhcpResults));
                stringBuilder = new StringBuilder();
                stringBuilder.append("make default IP configuration map, add rec for ");
                stringBuilder.append(StringUtil.safeDisplayBssid(key));
                log(stringBuilder.toString());
                return;
            }
            Log.e(TAG, "Failed to post runnable to fetch scan results");
        }
    }

    protected boolean handleHwDefaultIPConfiguration() {
        boolean isCurrentNetworkWEPSecurity = false;
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (!(config == null || config.wepKeys == null)) {
            int idx = config.wepTxKeyIndex;
            boolean z = idx >= 0 && idx < config.wepKeys.length && config.wepKeys[idx] != null;
            isCurrentNetworkWEPSecurity = z;
        }
        if (isCurrentNetworkWEPSecurity) {
            log("current network is WEP, dot set default IP configuration");
            return false;
        }
        String key = wifiStateMachineUtils.getWifiInfo(this).getBSSID();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("try to set default IP configuration for ");
        stringBuilder.append(key);
        log(stringBuilder.toString());
        if (key == null) {
            Log.w(TAG, "handleHwDefaultIPConfiguration key is null!");
            return false;
        }
        DhcpResults dhcpResult = (DhcpResults) this.mDhcpResultCache.get(key);
        if (dhcpResult == null) {
            log("set default IP configuration failed for no rec found");
            return false;
        }
        DhcpResults dhcpResults = new DhcpResults(dhcpResult);
        InterfaceConfiguration ifcg = new InterfaceConfiguration();
        try {
            ifcg.setLinkAddress(dhcpResults.ipAddress);
            ifcg.setInterfaceUp();
            wifiStateMachineUtils.handleIPv4Success(this, dhcpResults);
            log("set default IP configuration succeeded");
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("set default IP configuration failed for err: ");
            stringBuilder2.append(e);
            loge(stringBuilder2.toString());
            return false;
        }
    }

    public DhcpResults getCachedDhcpResultsForCurrentConfig() {
        boolean isCurrentNetworkWEPSecurity = false;
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (!(config == null || config.wepKeys == null)) {
            int idx = config.wepTxKeyIndex;
            boolean z = idx >= 0 && idx < config.wepKeys.length && config.wepKeys[idx] != null;
            isCurrentNetworkWEPSecurity = z;
        }
        if (isCurrentNetworkWEPSecurity) {
            log("current network is WEP, dot set default IP configuration");
            return null;
        }
        String key = wifiStateMachineUtils.getWifiInfo(this).getBSSID();
        int currRssi = wifiStateMachineUtils.getWifiInfo(this).getRssi();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("try to set default IP configuration currRssi = ");
        stringBuilder.append(currRssi);
        Log.d(str, stringBuilder.toString());
        if (key != null && currRssi >= -75) {
            return (DhcpResults) this.mDhcpResultCache.get(key);
        }
        Log.w(TAG, "getCachedDhcpResultsForCurrentConfig key is null!");
        return null;
    }

    protected boolean hasMeteredHintForWi(Inet4Address ip) {
        boolean isIphone = false;
        boolean isWindowsPhone = false;
        if (SystemProperties.get("dhcp.wlan0.vendorInfo", "").startsWith("hostname:") && ip != null && ip.toString().startsWith("/172.20.10.")) {
            Log.d(TAG, "isiphone = true");
            isIphone = true;
        }
        if (SystemProperties.get("dhcp.wlan0.domain", "").equals("mshome.net")) {
            Log.d(TAG, "isWindowsPhone = true");
            isWindowsPhone = true;
        }
        return isIphone || isWindowsPhone;
    }

    public int[] syncGetApChannelListFor5G(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_CHANNEL_LIST_5G);
        int[] channels = null;
        if (resultMsg.obj != null) {
            channels = (int[]) resultMsg.obj;
        }
        resultMsg.recycle();
        return channels;
    }

    public void setLocalMacAddressFromMacfile() {
        String str;
        StringBuilder stringBuilder;
        String ret = "02:00:00:00:00:00";
        String oriMacString = GetUDIDNative.getWifiMacAddress();
        if (oriMacString == null || oriMacString.length() != 12) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("MacString: ");
            stringBuilder.append(oriMacString);
            stringBuilder.append(" from UDIDNative is unvalid. Use default MAC address");
            Log.e(str, stringBuilder.toString());
        } else {
            StringBuilder macBuilder = new StringBuilder();
            for (int i = 0; i < oriMacString.length(); i += 2) {
                macBuilder.append(oriMacString.substring(i, i + 2));
                if (i + 2 < oriMacString.length() - 1) {
                    macBuilder.append(":");
                }
            }
            try {
                ret = MacAddress.fromString(macBuilder.toString()).toString();
            } catch (IllegalArgumentException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Formatted MacString is unvalid, message");
                stringBuilder2.append(e.getMessage());
                stringBuilder2.append("Use default MAC address");
                Log.e(str2, stringBuilder2.toString());
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setLocalMacAddress: ");
        stringBuilder.append(ParcelUtil.safeDisplayMac(ret));
        Log.i(str, stringBuilder.toString());
        wifiStateMachineUtils.getWifiInfo(this).setMacAddress(ret);
    }

    public void setVoWifiDetectMode(WifiDetectConfInfo info) {
        if (info != null && !this.mWifiDetectConfInfo.isEqual(info)) {
            this.mWifiDetectConfInfo = info;
            sendMessage(131772, info);
        }
    }

    protected void processSetVoWifiDetectMode(Message msg) {
        StringBuilder stringBuilder;
        WifiDetectConfInfo info = msg.obj;
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("set VoWifi Detect Mode ");
        stringBuilder2.append(info);
        Log.d(str, stringBuilder2.toString());
        boolean ret = false;
        if (info != null) {
            WifiNative wifiNative;
            if (info.mWifiDetectMode == 1) {
                wifiNative = WifiInjector.getInstance().getWifiNative();
                stringBuilder = new StringBuilder();
                stringBuilder.append("LOW_THRESHOLD ");
                stringBuilder.append(info.mThreshold);
                ret = wifiNative.voWifiDetectSet(stringBuilder.toString());
            } else if (info.mWifiDetectMode == 2) {
                wifiNative = WifiInjector.getInstance().getWifiNative();
                stringBuilder = new StringBuilder();
                stringBuilder.append("HIGH_THRESHOLD ");
                stringBuilder.append(info.mThreshold);
                ret = wifiNative.voWifiDetectSet(stringBuilder.toString());
            } else {
                wifiNative = WifiInjector.getInstance().getWifiNative();
                stringBuilder = new StringBuilder();
                stringBuilder.append("MODE ");
                stringBuilder.append(info.mWifiDetectMode);
                ret = wifiNative.voWifiDetectSet(stringBuilder.toString());
            }
            if (ret) {
                wifiNative = WifiInjector.getInstance().getWifiNative();
                stringBuilder = new StringBuilder();
                stringBuilder.append("TRIGGER_COUNT ");
                stringBuilder.append(info.mEnvalueCount);
                if (wifiNative.voWifiDetectSet(stringBuilder.toString())) {
                    wifiNative = WifiInjector.getInstance().getWifiNative();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MODE ");
                    stringBuilder.append(info.mWifiDetectMode);
                    ret = wifiNative.voWifiDetectSet(stringBuilder.toString());
                }
            }
        }
        String str2;
        if (ret) {
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("done set  VoWifi Detect Mode ");
            stringBuilder.append(info);
            Log.d(str2, stringBuilder.toString());
            return;
        }
        str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failed to set VoWifi Detect Mode ");
        stringBuilder.append(info);
        Log.d(str2, stringBuilder.toString());
    }

    public WifiDetectConfInfo getVoWifiDetectMode() {
        return this.mWifiDetectConfInfo;
    }

    public void setVoWifiDetectPeriod(int period) {
        if (period != this.mWifiDetectperiod) {
            this.mWifiDetectperiod = period;
            sendMessage(131773, period);
        }
    }

    protected void processSetVoWifiDetectPeriod(Message msg) {
        int period = msg.arg1;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set VoWifiDetect Period ");
        stringBuilder.append(period);
        Log.d(str, stringBuilder.toString());
        boolean ret = WifiInjector.getInstance().getWifiNative();
        stringBuilder = new StringBuilder();
        stringBuilder.append("PERIOD ");
        stringBuilder.append(period);
        String str2;
        StringBuilder stringBuilder2;
        if (ret.voWifiDetectSet(stringBuilder.toString())) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("done set set VoWifiDetect  Period");
            stringBuilder2.append(period);
            Log.d(str2, stringBuilder2.toString());
            return;
        }
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("set VoWifiDetect Period");
        stringBuilder2.append(period);
        Log.d(str2, stringBuilder2.toString());
    }

    public int getVoWifiDetectPeriod() {
        return this.mWifiDetectperiod;
    }

    public boolean syncGetSupportedVoWifiDetect(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(131774);
        boolean supportedVoWifiDetect = resultMsg.arg1 == 0;
        resultMsg.recycle();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("syncGetSupportedVoWifiDetect ");
        stringBuilder.append(supportedVoWifiDetect);
        Log.e(str, stringBuilder.toString());
        return supportedVoWifiDetect;
    }

    protected void processIsSupportVoWifiDetect(Message msg) {
        wifiStateMachineUtils.replyToMessage((WifiStateMachine) this, msg, msg.what, WifiInjector.getInstance().getWifiNative().isSupportVoWifiDetect() ? 0 : -1);
    }

    protected void processStatistics(int event) {
        if (event == 0) {
            this.lastConnectTime = System.currentTimeMillis();
            Flog.bdReport(this.myContext, 200);
        } else if (1 == event) {
            Flog.bdReport(this.myContext, HwSelfCureUtils.RESET_LEVEL_LOW_1_DNS);
            if (-1 != this.lastConnectTime) {
                JSONObject eventMsg = new JSONObject();
                try {
                    eventMsg.put("duration", (System.currentTimeMillis() - this.lastConnectTime) / 1000);
                } catch (JSONException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("processStatistics put error.");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
                Flog.bdReport(this.myContext, HwSelfCureUtils.RESET_LEVEL_LOW_2_RENEW_DHCP, eventMsg);
                this.lastConnectTime = -1;
            }
        }
    }

    public byte[] fetchWifiSignalInfoForVoWiFi() {
        String macStr;
        byte[] macBytes;
        ByteBuffer rawByteBuffer = ByteBuffer.allocate(52);
        rawByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int linkSpeed = -1;
        int linkSpeed2 = -1;
        int rssi = -1;
        SignalPollResult signalInfo = WifiInjector.getInstance().getWifiNative().signalPoll(wifiStateMachineUtils.getInterfaceName(this));
        if (signalInfo != null) {
            rssi = signalInfo.currentRssi;
            linkSpeed = signalInfo.txBitrate;
            linkSpeed2 = signalInfo.associationFrequency;
        }
        int frequency = linkSpeed2;
        linkSpeed2 = linkSpeed;
        RssiPacketCountInfo info = new RssiPacketCountInfo();
        TxPacketCounters txPacketCounters = WifiInjector.getInstance().getWifiNative().getTxPacketCounters(wifiStateMachineUtils.getInterfaceName(this));
        long nativeTxGood = 0;
        long nativeTxBad = 0;
        if (txPacketCounters != null) {
            info.txgood = txPacketCounters.txSucceeded;
            nativeTxGood = (long) txPacketCounters.txSucceeded;
            info.txbad = txPacketCounters.txFailed;
            nativeTxBad = (long) txPacketCounters.txFailed;
        }
        rawByteBuffer.putInt(rssi);
        rawByteBuffer.putInt(0);
        int noise = 0;
        int bler = (int) ((((double) info.txbad) / ((double) (info.txgood + info.txbad))) * 100.0d);
        rawByteBuffer.putInt(bler);
        int dpktcnt = info.txgood - this.mLastTxPktCnt;
        this.mLastTxPktCnt = info.txgood;
        rawByteBuffer.putInt(dpktcnt);
        rawByteBuffer.putInt(convertToAccessType(linkSpeed2, frequency));
        rawByteBuffer.putInt(0);
        rawByteBuffer.putLong(nativeTxGood);
        rawByteBuffer.putLong(nativeTxBad);
        String bssid = wifiStateMachineUtils.getWifiInfo(this).getBSSID();
        String macStr2 = "ffffffffffff";
        if (TextUtils.isEmpty(bssid)) {
            macStr = macStr2;
        } else {
            macStr = bssid.replace(":", "");
        }
        byte[] macBytes2 = new byte[16];
        try {
            macBytes = macStr.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            macBytes = macBytes2;
        }
        rawByteBuffer.put(macBytes);
        macStr2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("rssi=");
        stringBuilder.append(rssi);
        stringBuilder.append(",nativeTxBad=");
        stringBuilder.append(nativeTxBad);
        stringBuilder.append(", nativeTxGood=");
        stringBuilder.append(nativeTxGood);
        stringBuilder.append(", dpktcnt=");
        stringBuilder.append(dpktcnt);
        stringBuilder.append(", linkSpeed=");
        stringBuilder.append(linkSpeed2);
        stringBuilder.append(", frequency=");
        stringBuilder.append(frequency);
        stringBuilder.append(", noise=");
        stringBuilder.append(noise);
        stringBuilder.append(", mac=");
        stringBuilder.append(macStr.length() >= 6 ? macStr.substring(0, 6) : "ffffff");
        Log.d(macStr2, stringBuilder.toString());
        return rawByteBuffer.array();
    }

    private static int convertToAccessType(int linkSpeed, int frequency) {
        return 0;
    }

    private void closeInputStream(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
            }
        }
    }

    private void registerReceiverInWifiPro(Context context) {
        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                WifiConfiguration newConfig = (WifiConfiguration) intent.getParcelableExtra("new_wifi_config");
                WifiConfiguration currentConfig = HwWifiStateMachine.this.getCurrentWifiConfiguration();
                WifiConfigManager wifiConfigManager = HwWifiStateMachine.wifiStateMachineUtils.getWifiConfigManager(HwWifiStateMachine.this);
                if (newConfig != null && currentConfig != null && wifiConfigManager != null) {
                    HwWifiStateMachine hwWifiStateMachine = HwWifiStateMachine.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("sync update network history, internetHistory = ");
                    stringBuilder.append(newConfig.internetHistory);
                    hwWifiStateMachine.log(stringBuilder.toString());
                    currentConfig.noInternetAccess = newConfig.noInternetAccess;
                    currentConfig.validatedInternetAccess = newConfig.validatedInternetAccess;
                    currentConfig.numNoInternetAccessReports = newConfig.numNoInternetAccessReports;
                    currentConfig.portalNetwork = newConfig.portalNetwork;
                    currentConfig.portalCheckStatus = newConfig.portalCheckStatus;
                    currentConfig.internetHistory = newConfig.internetHistory;
                    currentConfig.lastHasInternetTimestamp = newConfig.lastHasInternetTimestamp;
                    wifiConfigManager.updateInternetInfoByWifiPro(currentConfig);
                    wifiConfigManager.saveToStore(true);
                }
            }
        }, new IntentFilter("com.huawei.wifipro.ACTION_UPDATE_CONFIG_HISTORY"), "com.huawei.wifipro.permission.RECV.NETWORK_CHECKER", null);
        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int switchType = intent.getIntExtra(WifiHandover.WIFI_HANDOVER_NETWORK_SWITCHTYPE, 1);
                WifiConfiguration changeConfig = (WifiConfiguration) intent.getParcelableExtra(WifiHandover.WIFI_HANDOVER_NETWORK_WIFICONFIG);
                HwWifiStateMachine hwWifiStateMachine = HwWifiStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ACTION_REQUEST_DUAL_BAND_WIFI_HANDOVER, switchType = ");
                stringBuilder.append(switchType);
                hwWifiStateMachine.log(stringBuilder.toString());
                if (!HwWifiStateMachine.this.mWifiSwitchOnGoing && changeConfig != null) {
                    if (switchType == 1) {
                        HwWifiStateMachine.this.requestWifiSoftSwitch();
                        HwWifiStateMachine.this.startConnectToUserSelectNetwork(changeConfig.networkId, Binder.getCallingUid(), changeConfig.BSSID);
                    } else {
                        ScanResult roamScanResult = new ScanResult();
                        roamScanResult.BSSID = changeConfig.BSSID;
                        HwWifiStateMachine.this.startRoamToNetwork(changeConfig.networkId, roamScanResult);
                        HwWifiStateMachine.this.log("roamScanResult, call startRoamToNetwork");
                    }
                    HwWifiStateMachine.this.mWifiSwitchOnGoing = true;
                }
            }
        }, new IntentFilter(WifiHandover.ACTION_REQUEST_DUAL_BAND_WIFI_HANDOVER), WifiHandover.WIFI_HANDOVER_RECV_PERMISSION, null);
    }

    public void startWifi2WifiRequest() {
        this.mWifiSwitchOnGoing = true;
    }

    public boolean isWifiProEnabled() {
        return WifiProCommonUtils.isWifiProSwitchOn(this.myContext);
    }

    public int resetScoreByInetAccess(int score) {
        NetworkInfo networkInfo = wifiStateMachineUtils.getNetworkInfo(this);
        if (isWifiProEnabled() && networkInfo != null && networkInfo.getDetailedState() == DetailedState.VERIFYING_POOR_LINK) {
            return 0;
        }
        return score;
    }

    public void getConfiguredNetworks(Message message) {
        wifiStateMachineUtils.replyToMessage((WifiStateMachine) this, message, message.what, wifiStateMachineUtils.getWifiConfigManager(this).getSavedNetworks());
    }

    public void saveConnectingNetwork(WifiConfiguration config, int netId, boolean autoJoin) {
        synchronized (this.selectConfigLock) {
            if (config == null && netId != -1) {
                config = wifiStateMachineUtils.getWifiConfigManager(this).getConfiguredNetwork(netId);
            }
            this.mSelectedConfig = config;
            if (HwAutoConnectManager.getInstance() != null) {
                HwAutoConnectManager.getInstance().releaseBlackListBssid(config, autoJoin);
            }
        }
    }

    public void reportPortalNetworkStatus() {
        unwantedNetwork(3);
    }

    public boolean ignoreEnterConnectedState() {
        NetworkInfo networkInfo = wifiStateMachineUtils.getNetworkInfo(this);
        if (!isWifiProEnabled() || networkInfo == null || networkInfo.getDetailedState() != DetailedState.VERIFYING_POOR_LINK) {
            return false;
        }
        log("L2ConnectedState, case CMD_IP_CONFIGURATION_SUCCESSFUL, ignore to enter CONNECTED State");
        return true;
    }

    public void wifiNetworkExplicitlyUnselected() {
        WifiInfo wifiInfo = wifiStateMachineUtils.getWifiInfo(this);
        HwNetworkAgent networkAgent = wifiStateMachineUtils.getNetworkAgent(this);
        if (wifiInfo != null) {
            wifiInfo.score = 40;
        }
        if (networkAgent != null) {
            networkAgent.sendNetworkScore(40);
        }
    }

    public void wifiNetworkExplicitlySelected() {
        WifiInfo wifiInfo = wifiStateMachineUtils.getWifiInfo(this);
        HwNetworkAgent networkAgent = wifiStateMachineUtils.getNetworkAgent(this);
        if (wifiInfo != null) {
            wifiInfo.score = 60;
        }
        if (networkAgent != null) {
            networkAgent.sendNetworkScore(60);
        }
    }

    public void handleConnectedInWifiPro() {
        WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this);
        handleWiFiConnectedByScanGenie(wifiConfigManager);
        if (this.mWifiSwitchOnGoing) {
            WifiConfiguration config;
            String bssid = null;
            String ssid = null;
            String configKey = null;
            synchronized (this.selectConfigLock) {
                if (this.mSelectedConfig != null) {
                    config = this.mSelectedConfig;
                } else {
                    config = wifiConfigManager.getConfiguredNetwork(wifiStateMachineUtils.getLastNetworkId(this));
                }
            }
            if (config != null) {
                bssid = wifiStateMachineUtils.getWifiInfo(this).getBSSID();
                ssid = config.SSID;
                configKey = config.configKey();
            }
            sendWifiHandoverCompletedBroadcast(0, bssid, ssid, configKey);
        }
        int lastNetworkId = wifiStateMachineUtils.getLastNetworkId(this);
        WifiConfiguration connectedConfig = wifiConfigManager.getConfiguredNetwork(lastNetworkId);
        if (connectedConfig != null) {
            if (connectedConfig.portalNetwork) {
                Bundle data = new Bundle();
                data.putBoolean("protalflag", connectedConfig.portalNetwork);
                this.mHwWifiCHRService.uploadDFTEvent(3, data);
            }
            for (WifiConfiguration config2 : wifiConfigManager.getSavedNetworks()) {
                if (config2.getNetworkSelectionStatus().getConnectChoice() != null) {
                    wifiConfigManager.clearNetworkConnectChoice(config2.networkId);
                }
            }
            if (connectedConfig.portalCheckStatus == 1) {
                log("handleConnectedInWifiPro reset HAS_INTERNET to INTERNET_UNKNOWN!!");
                connectedConfig.portalCheckStatus = 0;
            }
            if (connectedConfig.internetRecoveryStatus == 5) {
                log("handleConnectedInWifiPro reset RECOVERED to INTERNET_UNKNOWN!!");
                connectedConfig.internetRecoveryStatus = 3;
            }
            wifiConfigManager.updateInternetInfoByWifiPro(connectedConfig);
            if (!(connectedConfig == null || !isWifiProEvaluatingAP() || this.usingStaticIpConfig || connectedConfig.SSID == null || connectedConfig.SSID.equals("<unknown ssid>"))) {
                String strDhcpResults = WifiProCommonUtils.dhcpResults2String(wifiStateMachineUtils.getDhcpResults(this), WifiProCommonUtils.getCurrentCellId());
                if (!(strDhcpResults == null || connectedConfig.configKey() == null)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleConnectedInWifiPro, lastDhcpResults = ");
                    stringBuilder.append(strDhcpResults);
                    stringBuilder.append(", ssid = ");
                    stringBuilder.append(connectedConfig.SSID);
                    log(stringBuilder.toString());
                    this.lastDhcps.put(connectedConfig.configKey(), strDhcpResults);
                }
            }
        }
        if (!isWifiProEvaluatingAP()) {
            try {
                this.mHwInnerNetworkManagerImpl.setWifiproFirewallEnable(false);
            } catch (Exception e) {
                log("wifi connected, Disable WifiproFirewall again");
            }
        }
        synchronized (this.selectConfigLock) {
            this.mSelectedConfig = null;
        }
        this.usingStaticIpConfig = false;
        resetSelfCureCandidateLostCnt();
        wifiConfigManager.resetNetworkConnFailedInfo(lastNetworkId);
        wifiConfigManager.updateRssiDiscNonLocally(lastNetworkId, false, 0, 0);
        if (this.mWifiSoftSwitchRunning.get()) {
            log("wifi connected, reset mWifiSoftSwitchRunning and SCE state");
            this.mWifiSoftSwitchRunning.set(false);
            WifiProCommonUtils.setWifiSelfCureStatus(0);
        }
        removeMessages(131897);
    }

    public void handleDisconnectedInWifiPro() {
        WifiScanGenieController.createWifiScanGenieControllerImpl(this.myContext).handleWiFiDisconnected();
        this.mCurrNetworkHistoryInserted = false;
        if (this.wifiConnectedBackgroundReason == 2 || this.wifiConnectedBackgroundReason == 3) {
            WifiProCommonUtils.setBackgroundConnTag(this.myContext, false);
        }
        this.wifiConnectedBackgroundReason = 0;
        if (HwAutoConnectManager.getInstance() != null) {
            HwAutoConnectManager.getInstance().notifyNetworkDisconnected();
        }
        synchronized (this.selectConfigLock) {
            this.mSelectedConfig = null;
        }
        this.usingStaticIpConfig = false;
        this.mRenewDhcpSelfCuring.set(false);
        this.mDelayWifiScoreBySelfCureOrSwitch = false;
    }

    public void handleUnwantedNetworkInWifiPro(WifiConfiguration config, int unwantedType) {
        if (config != null) {
            boolean updated = false;
            if (unwantedType == wifiStateMachineUtils.getUnwantedValidationFailed(this)) {
                config.noInternetAccess = true;
                config.validatedInternetAccess = false;
                config.portalNetwork = WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 102);
                if (this.mCurrNetworkHistoryInserted) {
                    config.internetHistory = WifiProCommonUtils.updateWifiConfigHistory(config.internetHistory, 0);
                } else {
                    config.internetHistory = WifiProCommonUtils.insertWifiConfigHistory(config.internetHistory, 0);
                    this.mCurrNetworkHistoryInserted = true;
                }
                updated = true;
            } else if (unwantedType == 3) {
                config.portalNetwork = true;
                config.noInternetAccess = false;
                config.validatedInternetAccess = true;
                if (this.mCurrNetworkHistoryInserted) {
                    config.internetHistory = WifiProCommonUtils.updateWifiConfigHistory(config.internetHistory, 2);
                } else {
                    config.internetHistory = WifiProCommonUtils.insertWifiConfigHistory(config.internetHistory, 2);
                    this.mCurrNetworkHistoryInserted = true;
                    Bundle data = new Bundle();
                    data.putBoolean("protalflag", config.portalNetwork);
                    this.mHwWifiCHRService.uploadDFTEvent(3, data);
                }
                updated = true;
            }
            if (updated) {
                WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this);
                wifiConfigManager.updateInternetInfoByWifiPro(config);
                wifiConfigManager.saveToStore(false);
            }
            this.mDelayWifiScoreBySelfCureOrSwitch = false;
        }
    }

    public void handleValidNetworkInWifiPro(WifiConfiguration config) {
        if (config != null) {
            String strDhcpResults = WifiProCommonUtils.dhcpResults2String(wifiStateMachineUtils.getDhcpResults(this), -1);
            if (strDhcpResults != null) {
                config.lastDhcpResults = strDhcpResults;
                if (!isWifiProEvaluatingAP()) {
                    HwSelfCureEngine.getInstance(this.myContext, this).notifyDhcpResultsInternetOk(strDhcpResults);
                }
            }
            if (!config.portalNetwork || !this.mCurrNetworkHistoryInserted) {
                config.noInternetAccess = false;
                if (this.mCurrNetworkHistoryInserted) {
                    config.internetHistory = WifiProCommonUtils.updateWifiConfigHistory(config.internetHistory, 1);
                } else {
                    config.internetHistory = WifiProCommonUtils.insertWifiConfigHistory(config.internetHistory, 1);
                    this.mCurrNetworkHistoryInserted = true;
                }
                config.lastHasInternetTimestamp = System.currentTimeMillis();
                WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this);
                wifiConfigManager.updateInternetInfoByWifiPro(config);
                wifiConfigManager.saveToStore(false);
                this.mDelayWifiScoreBySelfCureOrSwitch = false;
            }
        }
    }

    public void startRoamToNetwork(int networkId, ScanResult scanResult) {
        super.startRoamToNetwork(networkId, scanResult);
        this.mCurrNetworkHistoryInserted = false;
    }

    public void handleConnectFailedInWifiPro(int netId, int disableReason) {
        if (this.mWifiSwitchOnGoing && disableReason >= 2 && disableReason <= 4) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleConnectFailedInWifiPro, netId = ");
            stringBuilder.append(netId);
            stringBuilder.append(", disableReason = ");
            stringBuilder.append(disableReason);
            log(stringBuilder.toString());
            String failedBssid = null;
            String failedSsid = null;
            int status = -6;
            if (disableReason != 2) {
                status = -7;
            }
            synchronized (this.selectConfigLock) {
                if (this.mSelectedConfig != null) {
                    failedBssid = this.mSelectedConfig.BSSID;
                    failedSsid = this.mSelectedConfig.SSID;
                }
            }
            sendWifiHandoverCompletedBroadcast(status, failedBssid, failedSsid, null);
        }
    }

    private void sendWifiHandoverCompletedBroadcast(int statusCode, String bssid, String ssid, String configKey) {
        if (this.mWifiSwitchOnGoing) {
            this.mWifiSwitchOnGoing = false;
            synchronized (this.selectConfigLock) {
                this.mSelectedConfig = null;
            }
            Intent intent = new Intent();
            if (WifiProStateMachine.getWifiProStateMachineImpl().getNetwoksHandoverType() == 1) {
                intent.setAction(WifiHandover.ACTION_RESPONSE_WIFI_2_WIFI);
            } else {
                intent.setAction(WifiHandover.ACTION_RESPONSE_DUAL_BAND_WIFI_HANDOVER);
            }
            intent.putExtra(WifiHandover.WIFI_HANDOVER_COMPLETED_STATUS, statusCode);
            intent.putExtra(WifiHandover.WIFI_HANDOVER_NETWORK_BSSID, bssid);
            intent.putExtra(WifiHandover.WIFI_HANDOVER_NETWORK_SSID, ssid);
            intent.putExtra(WifiHandover.WIFI_HANDOVER_NETWORK_CONFIGKYE, configKey);
            this.myContext.sendBroadcastAsUser(intent, UserHandle.ALL, WifiHandover.WIFI_HANDOVER_RECV_PERMISSION);
        }
    }

    public void updateWifiproWifiConfiguration(Message message) {
        if (message != null) {
            WifiConfiguration config = message.obj;
            boolean z = true;
            if (message.arg1 != 1) {
                z = false;
            }
            boolean uiOnly = z;
            if (config != null && config.networkId != -1) {
                WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this);
                wifiConfigManager.updateWifiConfigByWifiPro(config, uiOnly);
                if (config.configKey() != null && config.wifiProNoInternetAccess) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateWifiproWifiConfiguration, noInternetReason = ");
                    stringBuilder.append(config.wifiProNoInternetReason);
                    stringBuilder.append(", ssid = ");
                    stringBuilder.append(config.SSID);
                    log(stringBuilder.toString());
                    this.lastDhcps.remove(config.configKey());
                }
                wifiConfigManager.saveToStore(false);
            }
        }
    }

    public void notifyWifiConnFailedInfo(int netId, String bssid, int rssi, int reason, WifiConnectivityManager wcm) {
        if (netId == -1) {
            return;
        }
        if (reason == 3 || reason == 2 || reason == 4) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateNetworkConnFailedInfo, netId = ");
            stringBuilder.append(netId);
            stringBuilder.append(", rssi = ");
            stringBuilder.append(rssi);
            stringBuilder.append(", reason = ");
            stringBuilder.append(reason);
            log(stringBuilder.toString());
            WifiConfigManager configManager = wifiStateMachineUtils.getWifiConfigManager(this);
            WifiConfiguration selectedConfig = configManager.getConfiguredNetwork(netId);
            if (reason == 4) {
                configManager.updateNetworkConnFailedInfo(netId, rssi, reason);
            } else {
                if (selectedConfig != null) {
                    ScanResult scanResult = selectedConfig.getNetworkSelectionStatus().getCandidate();
                    if (scanResult != null) {
                        rssi = scanResult.level;
                    }
                }
                configManager.updateNetworkConnFailedInfo(netId, rssi, reason);
            }
            if (HwAutoConnectManager.getInstance() != null) {
                HwAutoConnectManager.getInstance().notifyWifiConnFailedInfo(selectedConfig, bssid, rssi, reason, wcm);
            }
        }
    }

    public void notifyNetworkUserConnect(boolean isUserConnect) {
        WifiProStateMachine mWifiProStateMachine = WifiProStateMachine.getWifiProStateMachineImpl();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyNetworkUserConnect : ");
        stringBuilder.append(isUserConnect);
        log(stringBuilder.toString());
        if (mWifiProStateMachine != null) {
            mWifiProStateMachine.notifyNetworkUserConnect(isUserConnect);
        }
    }

    public void handleDisconnectedReason(WifiConfiguration config, int rssi, int local, int reason) {
        if (config != null && local == 0 && rssi != -127) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleDisconnectedReason, rssi = ");
            stringBuilder.append(rssi);
            stringBuilder.append(", reason = ");
            stringBuilder.append(reason);
            stringBuilder.append(", ssid = ");
            stringBuilder.append(config.SSID);
            log(stringBuilder.toString());
            if (reason == 0 || reason == 3 || reason == 8) {
                wifiStateMachineUtils.getWifiConfigManager(this).updateRssiDiscNonLocally(config.networkId, true, rssi, System.currentTimeMillis());
            }
        }
    }

    public void setWiFiProScanResultList(List<ScanResult> list) {
        if (isWifiProEnabled()) {
            HwIntelligenceWiFiManager.setWiFiProScanResultList(list);
        }
    }

    /* JADX WARNING: Missing block: B:33:0x00bd, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:41:0x00e2, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isWifiProEvaluatingAP() {
        boolean z = true;
        if (this.wifiConnectedBackgroundReason == 2) {
            log("isWifiProEvaluatingAP, WIFI_BACKGROUND_PORTAL_CHECKING");
            return true;
        }
        if (isWifiProEnabled()) {
            WifiConfiguration connectedConfig = wifiStateMachineUtils.getWifiConfigManager(this).getConfiguredNetwork(wifiStateMachineUtils.getLastNetworkId(this));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isWifiProEvaluatingAP, connectedConfig = ");
            stringBuilder.append(connectedConfig != null ? connectedConfig.SSID : null);
            log(stringBuilder.toString());
            StringBuilder stringBuilder2;
            if (this.wifiConnectedBackgroundReason == 2 || this.wifiConnectedBackgroundReason == 3) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("isWifiProEvaluatingAP, wifi connected at background matched, reason = ");
                stringBuilder2.append(this.wifiConnectedBackgroundReason);
                log(stringBuilder2.toString());
                return true;
            } else if (connectedConfig != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("isWifiProEvaluatingAP, isTempCreated = ");
                stringBuilder2.append(connectedConfig.isTempCreated);
                stringBuilder2.append(", evaluating = ");
                stringBuilder2.append(WifiProStateMachine.isWifiEvaluating());
                stringBuilder2.append(", wifiConnectedBackgroundReason = ");
                stringBuilder2.append(this.wifiConnectedBackgroundReason);
                log(stringBuilder2.toString());
                if (WifiProStateMachine.isWifiEvaluating() && connectedConfig.isTempCreated) {
                    this.wifiConnectedBackgroundReason = 1;
                    return true;
                }
            } else {
                synchronized (this.selectConfigLock) {
                    if (this.mSelectedConfig != null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("isWifiProEvaluatingAP = ");
                        stringBuilder.append(WifiProStateMachine.isWifiEvaluating());
                        stringBuilder.append(", mSelectedConfig isTempCreated = ");
                        stringBuilder.append(this.mSelectedConfig.isTempCreated);
                        log(stringBuilder.toString());
                        if (!WifiProStateMachine.isWifiEvaluating() || !this.mSelectedConfig.isTempCreated) {
                            z = false;
                        }
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("==connectedConfig&mSelectedConfig are null, backgroundReason = ");
                        stringBuilder.append(this.wifiConnectedBackgroundReason);
                        log(stringBuilder.toString());
                        if (!WifiProStateMachine.isWifiEvaluating()) {
                            if (this.wifiConnectedBackgroundReason < 1) {
                                z = false;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void updateScanDetail(ScanDetail scanDetail) {
        ScanResult sc = scanDetail.getScanResult();
        if (sc != null) {
            if (isWifiProEnabled()) {
                WifiProConfigStore.updateScanDetailByWifiPro(sc);
            } else {
                sc.internetAccessType = 0;
                sc.networkQosLevel = 0;
                sc.networkQosScore = 0;
            }
        }
    }

    public void updateScanDetailByWifiPro(List<ScanDetail> scanResults) {
        if (scanResults != null) {
            for (ScanDetail scanDetail : scanResults) {
                updateScanDetail(scanDetail);
            }
        }
    }

    public void tryUseStaticIpForFastConnecting(int lastNid) {
        if (isWifiProEnabled() && lastNid != -1 && isWifiProEvaluatingAP()) {
            synchronized (this.selectConfigLock) {
                if (!(this.mSelectedConfig == null || this.mSelectedConfig.configKey() == null)) {
                    WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this);
                    this.mSelectedConfig.lastDhcpResults = (String) this.lastDhcps.get(this.mSelectedConfig.configKey());
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("tryUseStaticIpForFastConnecting, lastDhcpResults = ");
                    stringBuilder.append(this.mSelectedConfig.lastDhcpResults);
                    log(stringBuilder.toString());
                    if (this.mSelectedConfig.lastDhcpResults != null && this.mSelectedConfig.lastDhcpResults.length() > 0 && this.mSelectedConfig.getStaticIpConfiguration() == null && wifiConfigManager.tryUseStaticIpForFastConnecting(lastNid)) {
                        this.usingStaticIpConfig = true;
                    }
                }
            }
        }
    }

    public void updateNetworkConcurrently() {
        DetailedState state = DetailedState.CONNECTED;
        NetworkInfo networkInfo = wifiStateMachineUtils.getNetworkInfo(this);
        WifiInfo wifiInfo = wifiStateMachineUtils.getWifiInfo(this);
        WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this);
        int lastNetworkId = wifiStateMachineUtils.getLastNetworkId(this);
        HwNetworkAgent networkAgent = wifiStateMachineUtils.getNetworkAgent(this);
        if (!(networkInfo.getExtraInfo() == null || wifiInfo.getSSID() == null || wifiInfo.getSSID().equals("<unknown ssid>"))) {
            networkInfo.setExtraInfo(wifiInfo.getSSID());
        }
        if (state != networkInfo.getDetailedState()) {
            networkInfo.setDetailedState(state, null, wifiInfo.getSSID());
            if (networkAgent != null) {
                networkAgent.updateNetworkConcurrently(networkInfo);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateNetworkConcurrently, lastNetworkId = ");
        stringBuilder.append(lastNetworkId);
        log(stringBuilder.toString());
        if (this.wifiConnectedBackgroundReason == 2 || this.wifiConnectedBackgroundReason == 3) {
            HwSelfCureEngine.getInstance(this.myContext, this).notifyWifiConnectedBackground();
            WifiScanGenieController.createWifiScanGenieControllerImpl(this.myContext).notifyWifiConnectedBackground();
        }
    }

    public void triggerRoamingNetworkMonitor(boolean autoRoaming) {
        if (autoRoaming) {
            NetworkInfo networkInfo = wifiStateMachineUtils.getNetworkInfo(this);
            HwNetworkAgent networkAgent = wifiStateMachineUtils.getNetworkAgent(this);
            if (networkAgent != null) {
                networkAgent.triggerRoamingNetworkMonitor(networkInfo);
            }
        }
    }

    public boolean isDualbandScanning() {
        HwDualBandManager mHwDualBandManager = HwDualBandManager.getInstance();
        if (mHwDualBandManager != null) {
            return mHwDualBandManager.isDualbandScanning();
        }
        return false;
    }

    public void triggerInvalidlinkNetworkMonitor() {
        NetworkInfo networkInfo = wifiStateMachineUtils.getNetworkInfo(this);
        HwNetworkAgent networkAgent = wifiStateMachineUtils.getNetworkAgent(this);
        if (networkAgent != null) {
            networkAgent.triggerInvalidlinkNetworkMonitor(networkInfo);
        }
    }

    public void notifyWifiConnectedBackgroundReady() {
        Intent intent;
        if (this.wifiConnectedBackgroundReason == 1) {
            log("notifyWifiConnectedBackgroundReady, ACTION_NOTIFY_WIFI_CONNECTED_CONCURRENTLY sent");
            intent = new Intent(WifiproUtils.ACTION_NOTIFY_WIFI_CONNECTED_CONCURRENTLY);
            intent.setFlags(67108864);
            this.myContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else if (this.wifiConnectedBackgroundReason == 2) {
            log("notifyWifiConnectedBackgroundReady, WIFI_BACKGROUND_PORTAL_CHECKING sent");
            intent = new Intent(WifiproUtils.ACTION_NOTIFY_PORTAL_CONNECTED_BACKGROUND);
            intent.setFlags(67108864);
            this.myContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else if (this.wifiConnectedBackgroundReason == 3) {
            log("notifyWifiConnectedBackgroundReady, ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND sent");
            intent = new Intent(WifiproUtils.ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND);
            intent.setFlags(67108864);
            this.myContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    public void setWifiBackgroundReason(int status) {
        if (status == 0) {
            this.wifiConnectedBackgroundReason = 2;
            WifiProCommonUtils.setBackgroundConnTag(this.myContext, true);
        } else if (status == 1) {
            this.wifiConnectedBackgroundReason = 0;
        } else if (status == 3) {
            this.wifiConnectedBackgroundReason = 3;
            WifiProCommonUtils.setBackgroundConnTag(this.myContext, true);
        } else if (status == 5) {
            this.wifiConnectedBackgroundReason = 0;
        } else if (status == 6) {
            this.wifiConnectedBackgroundReason = 0;
        }
    }

    public void updateWifiBackgroudStatus(int msgType) {
        if (msgType == 2) {
            WifiProCommonUtils.setBackgroundConnTag(this.myContext, false);
            this.wifiConnectedBackgroundReason = 0;
        }
    }

    public boolean isWiFiProSwitchOnGoing() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isWiFiProSwitchOnGoing,mWifiSwitchOnGoing = ");
        stringBuilder.append(this.mWifiSwitchOnGoing);
        log(stringBuilder.toString());
        return this.mWifiSwitchOnGoing;
    }

    public void resetWifiproEvaluateConfig(WifiInfo mWifiInfo, int netId) {
        if (isWifiProEvaluatingAP() && mWifiInfo != null && mWifiInfo.getNetworkId() == netId) {
            int lastNetworkId = wifiStateMachineUtils.getLastNetworkId(this);
            WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this);
            WifiConfiguration connectedConfig = wifiConfigManager.getConfiguredNetwork(lastNetworkId);
            synchronized (this.selectConfigLock) {
                if (connectedConfig == null) {
                    try {
                        connectedConfig = this.mSelectedConfig;
                    } catch (Throwable th) {
                        while (true) {
                        }
                    }
                }
            }
            if (connectedConfig != null) {
                connectedConfig.isTempCreated = false;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("resetWifiproEvaluateConfig,ssid = ");
                stringBuilder.append(connectedConfig.SSID);
                log(stringBuilder.toString());
                wifiConfigManager.updateWifiConfigByWifiPro(connectedConfig, true);
            }
        }
    }

    public boolean ignoreNetworkStateChange(NetworkInfo networkInfo) {
        if (networkInfo == null) {
            return false;
        }
        if ((!isWifiProEvaluatingAP() || (networkInfo.getDetailedState() != DetailedState.CONNECTING && networkInfo.getDetailedState() != DetailedState.SCANNING && networkInfo.getDetailedState() != DetailedState.AUTHENTICATING && networkInfo.getDetailedState() != DetailedState.OBTAINING_IPADDR && networkInfo.getDetailedState() != DetailedState.CONNECTED)) && !selfCureIgnoreNetworkStateChange(networkInfo) && !softSwitchIgnoreNetworkStateChanged(networkInfo)) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ignoreNetworkStateChange, DetailedState = ");
        stringBuilder.append(networkInfo.getDetailedState());
        Log.d("WiFi_PRO", stringBuilder.toString());
        if (networkInfo.getDetailedState() == DetailedState.CONNECTING && ((this.mWifiSoftSwitchRunning.get() || isWifiSelfCureByReset()) && !isMobileNetworkActive())) {
            this.mDelayWifiScoreBySelfCureOrSwitch = true;
        }
        return true;
    }

    public boolean selfCureIgnoreNetworkStateChange(NetworkInfo networkInfo) {
        if ((!isWifiSelfCuring() || !this.mWifiBackgroundConnected) && ((!isWifiSelfCuring() || this.mWifiBackgroundConnected || networkInfo.getDetailedState() == DetailedState.CONNECTED) && (!isRenewDhcpSelfCuring() || networkInfo.getDetailedState() == DetailedState.DISCONNECTED))) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("selfCureIgnoreNetworkStateChange, detailedState = ");
        stringBuilder.append(networkInfo.getDetailedState());
        Log.d("HwSelfCureEngine", stringBuilder.toString());
        return true;
    }

    private boolean selfCureIgnoreSuppStateChange(SupplicantState state) {
        if (!isWifiSelfCuring() && !isRenewDhcpSelfCuring() && !this.mWifiSoftSwitchRunning.get()) {
            return false;
        }
        if (state == SupplicantState.ASSOCIATING && ((this.mWifiSoftSwitchRunning.get() || isWifiSelfCureByReset()) && !isMobileNetworkActive())) {
            this.mDelayWifiScoreBySelfCureOrSwitch = true;
        }
        return true;
    }

    private boolean isWifiSelfCureByReset() {
        return 102 == WifiProCommonUtils.getSelfCuringState();
    }

    private boolean isMobileNetworkActive() {
        if (this.mConnMgr == null) {
            this.mConnMgr = (ConnectivityManager) this.myContext.getSystemService("connectivity");
        }
        boolean z = false;
        if (this.mConnMgr == null) {
            return false;
        }
        NetworkInfo activeNetInfo = this.mConnMgr.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getType() == 0) {
            z = true;
        }
        return z;
    }

    public boolean softSwitchIgnoreNetworkStateChanged(NetworkInfo networkInfo) {
        if (!this.mWifiSoftSwitchRunning.get() || networkInfo.getDetailedState() == DetailedState.CONNECTED) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("softSwitchIgnoreNetworkStateChanged, detailedState = ");
        stringBuilder.append(networkInfo.getDetailedState());
        Log.d("WIFIPRO", stringBuilder.toString());
        if (networkInfo.getDetailedState() == DetailedState.DISCONNECTED) {
            HwSelfCureEngine.getInstance(this.myContext, this).notifyWifiDisconnected();
            HwWifiConnectivityMonitor.getInstance().notifyWifiDisconnected();
            HwMSSHandler.getInstance().notifyWifiDisconnected();
        }
        return true;
    }

    public boolean ignoreSupplicantStateChange(SupplicantState state) {
        if (state == SupplicantState.ASSOCIATING) {
            this.mTimeStampScanControlForAssoc = System.currentTimeMillis();
            this.mTimeOutScanControlForAssoc = TIMEOUT_CONTROL_SCAN_ASSOCIATING;
        } else if (state == SupplicantState.ASSOCIATED) {
            this.mTimeStampScanControlForAssoc = System.currentTimeMillis();
            this.mTimeOutScanControlForAssoc = TIMEOUT_CONTROL_SCAN_ASSOCIATED;
        } else if (!(state == SupplicantState.FOUR_WAY_HANDSHAKE || state == SupplicantState.AUTHENTICATING || state == SupplicantState.GROUP_HANDSHAKE)) {
            this.mTimeStampScanControlForAssoc = System.currentTimeMillis();
            this.mTimeOutScanControlForAssoc = 0;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update the timeout parameter for the scan control, timeout = ");
        stringBuilder.append(this.mTimeOutScanControlForAssoc);
        stringBuilder.append(", state = ");
        stringBuilder.append(state);
        Log.d(str, stringBuilder.toString());
        if ((!isWifiProEvaluatingAP() || (state != SupplicantState.SCANNING && state != SupplicantState.ASSOCIATING && state != SupplicantState.AUTHENTICATING && state != SupplicantState.ASSOCIATED && state != SupplicantState.FOUR_WAY_HANDSHAKE && state != SupplicantState.AUTHENTICATING && state != SupplicantState.GROUP_HANDSHAKE && state != SupplicantState.COMPLETED)) && !selfCureIgnoreSuppStateChange(state)) {
            return false;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("ignoreSupplicantStateChange, state = ");
        stringBuilder.append(state);
        Log.d("WiFi_PRO", stringBuilder.toString());
        return true;
    }

    private boolean ctrlScanForConnection() {
        long now = System.currentTimeMillis();
        if (now - this.mTimeStampScanControlForAssoc <= this.mTimeOutScanControlForAssoc) {
            return true;
        }
        if (!ObtainingIpState.class.equals(getCurrentState().getClass())) {
            this.mTimeLastCtrlScanDuringObtainingIp = 0;
            return false;
        } else if (this.mTimeLastCtrlScanDuringObtainingIp == 0) {
            this.mTimeLastCtrlScanDuringObtainingIp = now;
            return true;
        } else if (now - this.mTimeLastCtrlScanDuringObtainingIp <= TIMEOUT_CONTROL_SCAN_ASSOCIATED) {
            return true;
        } else {
            return false;
        }
    }

    private void resetWifiProManualConnect() {
        System.putInt(this.myContext.getContentResolver(), "wifipro_manual_connect_ap", 0);
    }

    private int getAppUid(String processName) {
        try {
            ApplicationInfo ai = this.myContext.getPackageManager().getApplicationInfo(processName, 1);
            if (ai != null) {
                return ai.uid;
            }
            return 1000;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return 1000;
        }
    }

    private void registerForWifiEvaluateChanges() {
        this.myContext.getContentResolver().registerContentObserver(Secure.getUriFor(WIFI_EVALUATE_TAG), false, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                HwWifiStateMachine hwWifiStateMachine;
                StringBuilder stringBuilder;
                int tag = Secure.getInt(HwWifiStateMachine.this.myContext.getContentResolver(), HwWifiStateMachine.WIFI_EVALUATE_TAG, 0);
                if (HwWifiStateMachine.this.mBQEUid == 1000) {
                    HwWifiStateMachine.this.mBQEUid = HwWifiStateMachine.this.getAppUid("com.huawei.wifiprobqeservice");
                }
                HwWifiStateMachine hwWifiStateMachine2 = HwWifiStateMachine.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("**wifipro tag is chenge, setWifiproFirewallEnable**,tag =");
                stringBuilder2.append(tag);
                hwWifiStateMachine2.logd(stringBuilder2.toString());
                if (tag == 1) {
                    try {
                        HwWifiStateMachine.this.mHwInnerNetworkManagerImpl.setWifiproFirewallEnable(true);
                        if (HwWifiStateMachine.this.mBQEUid != 1000) {
                            HwWifiStateMachine.this.mHwInnerNetworkManagerImpl.setWifiproFirewallWhitelist(HwWifiStateMachine.this.mBQEUid);
                        }
                        HwWifiStateMachine.this.mHwInnerNetworkManagerImpl.setWifiproFirewallWhitelist(1000);
                        HwWifiStateMachine.this.mHwInnerNetworkManagerImpl.setWifiproFirewallDrop();
                    } catch (Exception e) {
                        hwWifiStateMachine = HwWifiStateMachine.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("**setWifiproCmdEnable,Error Exception :");
                        stringBuilder.append(e);
                        hwWifiStateMachine.loge(stringBuilder.toString());
                    }
                } else if (tag == 0) {
                    try {
                        HwWifiStateMachine.this.mHwInnerNetworkManagerImpl.setWifiproFirewallEnable(false);
                    } catch (Exception e2) {
                        hwWifiStateMachine = HwWifiStateMachine.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("**Disable WifiproCmdEnable***Error Exception ");
                        stringBuilder.append(e2);
                        hwWifiStateMachine.loge(stringBuilder.toString());
                    }
                }
            }
        });
    }

    private void registerForPasspointChanges() {
        this.myContext.getContentResolver().registerContentObserver(Global.getUriFor(DBKEY_HOTSPOT20_VALUE), false, new ContentObserver(getHandler()) {
            public void onChange(boolean selfChange) {
                if (Global.getInt(HwWifiStateMachine.this.myContext.getContentResolver(), HwWifiStateMachine.DBKEY_HOTSPOT20_VALUE, 1) == 0) {
                    WifiConfiguration config = HwWifiStateMachine.this.getCurrentWifiConfiguration();
                    if (config != null && config.isPasspoint()) {
                        HwWifiStateMachine.this.disconnectCommand();
                    }
                }
            }
        });
    }

    private void handleWiFiConnectedByScanGenie(WifiConfigManager wifiConfigManager) {
        Log.d(TAG, "handleWiFiConnectedByScanGenie");
        if (HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(this.myContext)) {
            Log.d(TAG, "this is mobile ap,ScanGenie ignor it");
            return;
        }
        WifiConfiguration currentWifiConfig = getCurrentWifiConfiguration();
        if (!(currentWifiConfig == null || currentWifiConfig.isTempCreated)) {
            Log.d(TAG, "mWifiScanGenieController.handleWiFiConnected");
            WifiScanGenieController.createWifiScanGenieControllerImpl(this.myContext).handleWiFiConnected(currentWifiConfig, false);
        }
    }

    public void notifyWifiScanResultsAvailable(boolean success) {
        HwSelfCureEngine.getInstance(this.myContext, this).notifyWifiScanResultsAvailable(success);
    }

    public void notifyWifiRoamingStarted() {
        HwWifiConnectivityMonitor.getInstance(this.myContext, this).notifyWifiRoamingStarted();
    }

    public void notifyWifiRoamingCompleted(String newBssid) {
        if (newBssid != null) {
            HwQoEService mHwQoEService = HwQoEService.getInstance();
            if (mHwQoEService != null) {
                mHwQoEService.notifyNetworkRoaming();
            }
            HwSelfCureEngine.getInstance(this.myContext, this).notifyWifiRoamingCompleted(newBssid);
            HwWifiConnectivityMonitor.getInstance(this.myContext, this).notifyWifiRoamingCompleted();
            WifiScanGenieController.createWifiScanGenieControllerImpl(this.myContext).notifyNetworkRoamingCompleted(newBssid);
        }
    }

    public void notifyEnableSameNetworkId(int netId) {
        if (HwAutoConnectManager.getInstance() != null) {
            HwAutoConnectManager.getInstance().notifyEnableSameNetworkId(netId);
        }
    }

    public boolean isWlanSettingsActivity() {
        List<RunningTaskInfo> runningTaskInfos = this.mActivityManager.getRunningTasks(1);
        if (!(runningTaskInfos == null || runningTaskInfos.isEmpty())) {
            ComponentName cn = ((RunningTaskInfo) runningTaskInfos.get(0)).topActivity;
            if (cn == null || cn.getClassName() == null || !cn.getClassName().startsWith(HUAWEI_SETTINGS)) {
                return false;
            }
            return true;
        }
        return false;
    }

    public void requestUpdateDnsServers(ArrayList<String> dnses) {
        if (dnses != null && !dnses.isEmpty()) {
            sendMessage(131882, dnses);
        }
    }

    public void sendUpdateDnsServersRequest(Message msg, LinkProperties lp) {
        if (msg != null && msg.obj != null) {
            ArrayList<String> dnsesStr = msg.obj;
            ArrayList<InetAddress> dnses = new ArrayList();
            int i = 0;
            while (i < dnsesStr.size()) {
                try {
                    dnses.add(Inet4Address.getByName((String) dnsesStr.get(i)));
                    i++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!dnses.isEmpty()) {
                HwNetworkAgent networkAgent = wifiStateMachineUtils.getNetworkAgent(this);
                LinkProperties newLp = new LinkProperties(lp);
                newLp.setDnsServers(dnses);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendUpdateDnsServersRequest, renew dns server newLp is: ");
                stringBuilder.append(newLp);
                logd(stringBuilder.toString());
                if (networkAgent != null) {
                    networkAgent.sendLinkProperties(newLp);
                }
            }
        }
    }

    public void requestRenewDhcp() {
        this.mRenewDhcpSelfCuring.set(true);
        sendMessage(131883);
    }

    public void handleInvalidIpAddr() {
        sendMessage(131895);
    }

    public void startSelfCureReconnect() {
        resetSelfCureParam();
        if (saveCurrentConfig()) {
            this.mWifiSelfCuring.set(true);
            resetSelfCureCandidateLostCnt();
            WifiProCommonUtils.setWifiSelfCureStatus(103);
            checkWifiBackgroundStatus();
            setSelfCureWifiTimeOut(5);
        }
    }

    public void handleNoInternetIp() {
        sendMessage(131898);
    }

    public void setForceDhcpDiscovery(IpClient ipClient) {
        if ((this.mRenewDhcpSelfCuring.get() || this.mWifiSelfCuring.get()) && ipClient != null) {
            logd("setForceDhcpDiscovery, force dhcp discovery for sce background cure internet.");
            ipClient.setForceDhcpDiscovery();
        }
    }

    public void resetIpConfigStatus() {
        this.mRenewDhcpSelfCuring.set(false);
    }

    public boolean isRenewDhcpSelfCuring() {
        return this.mRenewDhcpSelfCuring.get();
    }

    public void requestUseStaticIpConfig(StaticIpConfiguration staticIpConfig) {
        sendMessage(131884, staticIpConfig);
    }

    public void handleStaticIpConfig(IpClient ipClient, WifiNative wifiNative, StaticIpConfiguration config) {
        if (ipClient != null && wifiNative != null && config != null) {
            ProvisioningConfiguration prov = IpClient.buildProvisioningConfiguration().withStaticConfiguration(config).withoutIpReachabilityMonitor().withApfCapabilities(wifiNative.getApfCapabilities(wifiStateMachineUtils.getInterfaceName(this))).build();
            logd("handleStaticIpConfig, startProvisioning");
            ipClient.startProvisioning(prov);
        }
    }

    public void notifyIpConfigCompleted() {
        HwSelfCureEngine.getInstance(this.myContext, this).notifyIpConfigCompleted();
    }

    public int getWifiApTypeFromMpLink() {
        return HwMpLinkContentAware.getInstance(this.myContext).getWifiApTypeAndSendMsg(getCurrentWifiConfiguration());
    }

    public boolean notifyIpConfigLostAndFixedBySce(WifiConfiguration config) {
        return HwSelfCureEngine.getInstance(this.myContext, this).notifyIpConfigLostAndHandle(config);
    }

    public void requestResetWifi() {
        sendMessage(131887);
    }

    public void requestReassocLink() {
        sendMessage(131886);
    }

    public void startSelfCureWifiReset() {
        resetSelfCureParam();
        if (saveCurrentConfig()) {
            this.mWifiSelfCuring.set(true);
            resetSelfCureCandidateLostCnt();
            WifiProCommonUtils.setWifiSelfCureStatus(102);
            checkWifiBackgroundStatus();
            selfCureWifiDisable();
            return;
        }
        stopSelfCureDelay(1, 0);
    }

    public void startSelfCureWifiReassoc() {
        resetSelfCureParam();
        if (saveCurrentConfig()) {
            this.mWifiSelfCuring.set(true);
            resetSelfCureCandidateLostCnt();
            WifiProCommonUtils.setWifiSelfCureStatus(101);
            checkWifiBackgroundStatus();
            reassociateCommand();
            setSelfCureWifiTimeOut(4);
            return;
        }
        stopSelfCureDelay(1, 0);
    }

    public void requestWifiSoftSwitch() {
        this.mWifiSoftSwitchRunning.set(true);
        WifiProCommonUtils.setWifiSelfCureStatus(104);
        sendMessageDelayed(131897, -4, 0, 15000);
    }

    private boolean saveCurrentConfig() {
        WifiConfiguration currentConfiguration = getCurrentWifiConfiguration();
        if (currentConfiguration == null) {
            stopSelfCureDelay(1, 0);
            return false;
        }
        this.mCurrentConfigurationKey = currentConfiguration.configKey();
        this.mCurrentConfigNetId = currentConfiguration.networkId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("saveCurrentConfig >> configKey=");
        stringBuilder.append(this.mCurrentConfigurationKey);
        stringBuilder.append(" netid=");
        stringBuilder.append(this.mCurrentConfigNetId);
        logd(stringBuilder.toString());
        return true;
    }

    private void updateNetworkId() {
        WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this);
        if (wifiConfigManager != null) {
            WifiConfiguration wifiConfig = wifiConfigManager.getConfiguredNetwork(this.mCurrentConfigurationKey);
            if (wifiConfig != null) {
                this.mCurrentConfigNetId = wifiConfig.networkId;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateNetworkId >> configKey=");
        stringBuilder.append(this.mCurrentConfigurationKey);
        stringBuilder.append(" netid=");
        stringBuilder.append(this.mCurrentConfigNetId);
        logd(stringBuilder.toString());
    }

    private void resetSelfCureParam() {
        logd("ENTER: resetSelfCureParam");
        this.mWifiSelfCuring.set(false);
        WifiProCommonUtils.setWifiSelfCureStatus(0);
        this.mWifiAlwaysOnBeforeCure = false;
        this.mWifiBackgroundConnected = false;
        this.mCurrentConfigurationKey = null;
        this.mSelfCureWifiLastState = -1;
        this.mUserCloseWifiWhenSelfCure = false;
        this.mSelfCureNetworkLastState = DetailedState.IDLE;
        this.mSelfCureWifiConnectRetry = 0;
        removeMessages(131888);
        removeMessages(131889);
        removeMessages(131890);
        removeMessages(131891);
    }

    private void checkWifiBackgroundStatus() {
        NetworkInfo networkInfo = wifiStateMachineUtils.getNetworkInfo(this);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkWifiBackgroundStatus: detailstate=");
        stringBuilder.append(networkInfo.getDetailedState());
        stringBuilder.append(" isMobileDataInactive=");
        stringBuilder.append(WifiProCommonUtils.isMobileDataInactive(this.myContext));
        logd(stringBuilder.toString());
        boolean z = (networkInfo == null || networkInfo.getDetailedState() != DetailedState.VERIFYING_POOR_LINK || WifiProCommonUtils.isMobileDataInactive(this.myContext)) ? false : true;
        setWifiBackgroundStatus(z);
    }

    public void setWifiBackgroundStatus(boolean background) {
        if (isWifiSelfCuring()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setWifiBackgroundStatus: ");
            stringBuilder.append(background);
            stringBuilder.append(" wifiBackgroundConnected=");
            stringBuilder.append(this.mWifiBackgroundConnected);
            logd(stringBuilder.toString());
            this.mWifiBackgroundConnected = background;
        }
    }

    private void selfCureWifiDisable() {
        HwSelfCureEngine.getInstance(this.myContext, this).requestChangeWifiStatus(false);
        setSelfCureWifiTimeOut(1);
    }

    private void selfCureWifiEnable() {
        HwSelfCureEngine.getInstance(this.myContext, this).requestChangeWifiStatus(true);
        setSelfCureWifiTimeOut(2);
    }

    private void setSelfCureWifiTimeOut(int wifiSelfCureState) {
        this.mWifiSelfCureState = wifiSelfCureState;
        switch (this.mWifiSelfCureState) {
            case 1:
                logd("selfCureWifiResetCheck send delay messgae CMD_SELFCURE_WIFI_OFF_TIMEOUT 2000");
                sendMessageDelayed(131888, -1, 0, TIMEOUT_CONTROL_SCAN_ASSOCIATING);
                break;
            case 2:
                logd("selfCureWifiResetCheck send delay messgae CMD_SELFCURE_WIFI_ON_TIMEOUT 3000");
                sendMessageDelayed(131889, -1, 0, 3000);
                break;
            case 3:
                int i;
                if (((PowerManager) this.myContext.getSystemService("power")).isScreenOn()) {
                    i = 15000;
                } else {
                    i = 30000;
                }
                long delayedMs = (long) i;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("selfCureWifiResetCheck send delay messgae CMD_SELFCURE_WIFI_CONNECT_TIMEOUT ");
                stringBuilder.append(delayedMs);
                logd(stringBuilder.toString());
                sendMessageDelayed(131890, -1, 0, delayedMs);
                break;
            case 4:
                logd("selfCureWifiResetCheck send delay messgae SCE_WIFI_REASSOC_STATE 12000");
                sendMessageDelayed(131891, -1, 0, 12000);
                break;
            case 5:
                logd("selfCureWifiResetCheck send delay messgae SCE_WIFI_RECONNECT_STATE 15000");
                sendMessageDelayed(131896, -1, 0, 15000);
                break;
            default:
                return;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:93:0x01ef  */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x01dd  */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x0197  */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x0120  */
    /* JADX WARNING: Missing block: B:60:0x00e9, code skipped:
            if (r17 == 101) goto L_0x00eb;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean checkSelfCureWifiResult(int event) {
        int wifiState = syncGetWifiState();
        if (wifiState == 2) {
            this.mWifiEnabledTimeStamp = System.currentTimeMillis();
        }
        int i = -1;
        if (wifiState == 0) {
            if (!isWifiSelfCuring() && wifiStateMachineUtils.getScreenOn(this)) {
                WifiConfigManager wifiConfigMgr = wifiStateMachineUtils.getWifiConfigManager(this);
                for (WifiConfiguration config : wifiConfigMgr.getConfiguredNetworks()) {
                    if (config.portalCheckStatus != 0) {
                        config.portalCheckStatus = 0;
                        wifiConfigMgr.updateInternetInfoByWifiPro(config);
                    }
                }
                List<ScanResult> scanResults = new ArrayList();
                ScanRequestProxy scanProxy = WifiInjector.getInstance().getScanRequestProxy();
                if (scanProxy != null) {
                    synchronized (scanProxy) {
                        for (ScanResult result : scanProxy.getScanResults()) {
                            scanResults.add(new ScanResult(result));
                        }
                    }
                    if (scanResults.size() > 0) {
                        setWiFiProScanResultList(scanResults);
                    }
                }
            }
            if (!(!isWifiSelfCuring() || this.mUserCloseWifiWhenSelfCure || isWifiSelfCureByReset())) {
                logd("checkSelfCureWifiResult, user close wifi during reassoc or reconnect self-cure going.");
                this.mUserCloseWifiWhenSelfCure = true;
                removeMessages(131891);
                removeMessages(131896);
                exitWifiSelfCure(1, -1);
                return false;
            }
        }
        int i2;
        StringBuilder stringBuilder;
        if (this.mWifiSoftSwitchRunning.get() && wifiState == 0) {
            logd("checkSelfCureWifiResult, WifiSoftSwitchRunning, WIFI_STATE_DISABLING.");
            removeMessages(131897);
            sendMessage(131897, -4, 0);
            return false;
        } else if (!isWifiSelfCuring() || this.mUserCloseWifiWhenSelfCure || wifiState == 4) {
            i2 = event;
            stringBuilder = new StringBuilder();
            stringBuilder.append("userCloseWifiWhenSelfCure = ");
            stringBuilder.append(this.mUserCloseWifiWhenSelfCure);
            stringBuilder.append(", wifiState = ");
            stringBuilder.append(wifiState);
            logd(stringBuilder.toString());
            return false;
        } else {
            boolean ret = true;
            if (this.mSelfCureWifiLastState > wifiState && this.mWifiSelfCureState != 1) {
                i2 = event;
            } else if (wifiState != 0 || this.mSelfCureWifiLastState != wifiState) {
                i2 = event;
                this.mSelfCureWifiLastState = wifiState;
                NetworkInfo networkInfo;
                boolean connSucc;
                switch (this.mWifiSelfCureState) {
                    case 1:
                        if (wifiState == 1) {
                            removeMessages(131888);
                            logd("wifi disabled > CMD_SCE_WIFI_OFF_TIMEOUT msg removed");
                            notifySelfCureComplete(true, 0);
                            break;
                        }
                        break;
                    case 2:
                        if (wifiState == 3) {
                            removeMessages(131889);
                            logd("wifi enabled > CMD_SCE_WIFI_ON_TIMEOUT msg removed");
                            notifySelfCureComplete(true, 0);
                            break;
                        }
                        break;
                    case 3:
                        networkInfo = wifiStateMachineUtils.getNetworkInfo(this);
                        if ((!isDuplicateNetworkState(networkInfo) && networkInfo.getDetailedState() == DetailedState.CONNECTED) || networkInfo.getDetailedState() == DetailedState.VERIFYING_POOR_LINK) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("wifi connect > CMD_SCE_WIFI_CONNECT_TIMEOUT msg removed state=");
                            stringBuilder.append(networkInfo.getDetailedState());
                            logd(stringBuilder.toString());
                            removeMessages(131890);
                            connSucc = isWifiConnectToSameAP();
                            if (connSucc) {
                                i = 0;
                            }
                            notifySelfCureComplete(connSucc, i);
                            break;
                        }
                    case 4:
                    case 5:
                        networkInfo = wifiStateMachineUtils.getNetworkInfo(this);
                        StringBuilder stringBuilder2;
                        if ((isDuplicateNetworkState(networkInfo) || networkInfo.getDetailedState() != DetailedState.CONNECTED) && networkInfo.getDetailedState() != DetailedState.VERIFYING_POOR_LINK) {
                            if (!isDuplicateNetworkState(networkInfo) && networkInfo.getDetailedState() == DetailedState.DISCONNECTED) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("wifi reassociate/reconnect > CMD_SCE_WIFI_REASSOC_TIMEOUT msg removed state=");
                                stringBuilder2.append(networkInfo.getDetailedState());
                                logd(stringBuilder2.toString());
                                removeMessages(131891);
                                removeMessages(131896);
                                notifySelfCureComplete(false, -1);
                                break;
                            }
                        }
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("wifi reassociate/reconnect > CMD_SCE_WIFI_REASSOC_TIMEOUT msg removed state=");
                        stringBuilder2.append(networkInfo.getDetailedState());
                        logd(stringBuilder2.toString());
                        removeMessages(131891);
                        removeMessages(131896);
                        connSucc = isWifiConnectToSameAP();
                        if (connSucc) {
                            i = 0;
                        }
                        notifySelfCureComplete(connSucc, i);
                        break;
                        break;
                }
                return ret;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("last state =");
            stringBuilder3.append(this.mSelfCureWifiLastState);
            stringBuilder3.append(", current state=");
            stringBuilder3.append(wifiState);
            stringBuilder3.append(", user may toggle wifi! stop selfcure");
            logd(stringBuilder3.toString());
            exitWifiSelfCure(1, -1);
            this.mUserCloseWifiWhenSelfCure = true;
            ret = false;
            this.mSelfCureWifiLastState = wifiState;
            switch (this.mWifiSelfCureState) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                case 5:
                    break;
            }
            return ret;
        }
    }

    private boolean isDuplicateNetworkState(NetworkInfo networkInfo) {
        boolean ret = false;
        if (networkInfo != null && this.mSelfCureNetworkLastState == networkInfo.getDetailedState()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("duplicate network state non-change ");
            stringBuilder.append(networkInfo.getDetailedState());
            log(stringBuilder.toString());
            ret = true;
        }
        this.mSelfCureNetworkLastState = networkInfo.getDetailedState();
        return ret;
    }

    private boolean isWifiConnectToSameAP() {
        WifiConfiguration wifiConfig = getCurrentWifiConfiguration();
        if (this.mCurrentConfigurationKey == null || wifiConfig == null || wifiConfig.configKey() == null || !this.mCurrentConfigurationKey.equals(wifiConfig.configKey())) {
            return false;
        }
        return true;
    }

    public boolean isBssidDisabled(String bssid) {
        return false;
    }

    public void resetSelfCureCandidateLostCnt() {
        WifiInjector.getInstance().getSavedNetworkEvaluator().resetSelfCureCandidateLostCnt();
    }

    public boolean isWifiSelfCuring() {
        return this.mWifiSelfCuring.get();
    }

    public int getSelfCureNetworkId() {
        return this.mCurrentConfigNetId;
    }

    public long getWifiEnabledTimeStamp() {
        return this.mWifiEnabledTimeStamp;
    }

    public boolean reportWifiScoreDelayed() {
        return this.mDelayWifiScoreBySelfCureOrSwitch;
    }

    public void notifySelfCureComplete(boolean success, int reasonCode) {
        if (!success && reasonCode == -4) {
            Log.d("WIFIPRO", "notifySelfCureComplete SOFT_CONNECT_FAILED, timeout happend");
            this.mWifiSoftSwitchRunning.set(false);
            WifiProCommonUtils.setWifiSelfCureStatus(0);
            stopSelfCureDelay(-4, 0);
        } else if (isWifiSelfCuring()) {
            if (success) {
                handleSelfCureNormal();
            } else {
                handleSelfCureException(reasonCode);
            }
        } else {
            logd("notifySelfCureComplete: not Curing!");
            stopSelfCureDelay(1, 0);
        }
    }

    public void notifySelfCureNetworkLost() {
        if (hasMessages(131890)) {
            logd("notifySelfCureNetworkLost, Stop Reset");
            removeMessages(131890);
            sendMessage(131890, -2, 0);
        } else if (hasMessages(131891)) {
            logd("notifySelfCureNetworkLost, Stop Reassociate");
            removeMessages(131891);
            sendMessage(131891, -2, 0);
        } else {
            logd("notifySelfCureNetworkLost, No delay message found.");
        }
    }

    private void handleSelfCureNormal() {
        switch (this.mWifiSelfCureState) {
            case 1:
                logd("handleSelfCureNormal, wifi off OK! -> wifi on");
                selfCureWifiEnable();
                break;
            case 2:
                logd("handleSelfCureNormal, wifi on OK! -> wifi connect");
                setSelfCureWifiTimeOut(3);
                if (HwABSUtils.getABSEnable()) {
                    HwABSDetectorService service = HwABSDetectorService.getInstance();
                    if (service != null) {
                        service.notifySelEngineEnableWiFi();
                        break;
                    }
                }
                break;
            case 3:
            case 4:
            case 5:
                logd("handleSelfCureNormal, wifi connect/reassoc/reconnect OK!");
                if (this.mWifiBackgroundConnected) {
                    logd("handleSelfCureNormal, wifiBackgroundConnected, wifiNetworkExplicitlyUnselected");
                    wifiNetworkExplicitlyUnselected();
                }
                stopSelfCureDelay(0, 500);
                break;
            default:
                return;
        }
    }

    private void handleSelfCureException(int reasonCode) {
        switch (this.mWifiSelfCureState) {
            case 1:
                stopSelfCureDelay(-1, 0);
                logd("handleSelfCureException, wifi off fail! -> wifi off");
                HwSelfCureEngine.getInstance(this.myContext, this).requestChangeWifiStatus(false);
                break;
            case 2:
                stopSelfCureDelay(-1, 0);
                logd("handleSelfCureException, wifi on fail! -> wifi on");
                HwSelfCureEngine.getInstance(this.myContext, this).requestChangeWifiStatus(true);
                break;
            case 3:
            case 4:
            case 5:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleSelfCureException, wifi connect/reassoc/reconnect failed! retry = ");
                stringBuilder.append(this.mSelfCureWifiConnectRetry);
                stringBuilder.append(", reason = ");
                stringBuilder.append(reasonCode);
                logd(stringBuilder.toString());
                if (this.mSelfCureWifiConnectRetry < 1 && reasonCode != -2) {
                    this.mSelfCureWifiConnectRetry++;
                    startConnectToUserSelectNetwork(this.mCurrentConfigNetId, Binder.getCallingUid(), null);
                    setSelfCureWifiTimeOut(3);
                    break;
                }
                stopSelfCureDelay(reasonCode == -2 ? -2 : -1, 0);
                if (!this.mWifiBackgroundConnected) {
                    if (reasonCode != -2) {
                        startConnectToUserSelectNetwork(this.mCurrentConfigNetId, Binder.getCallingUid(), null);
                    }
                    this.mCurrentConfigNetId = -1;
                    break;
                }
                disconnectCommand();
                break;
                break;
            default:
                return;
        }
    }

    public void stopSelfCureWifi(int status) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopSelfCureWifi, status =");
        stringBuilder.append(status);
        log(stringBuilder.toString());
        if (status == -4) {
            log("notify soft connect time out failed.");
            sendWifiHandoverCompletedBroadcast(-6, null, null, null);
            sendMessage(131893);
        } else if (isWifiSelfCuring()) {
            NetworkInfo networkInfo = wifiStateMachineUtils.getNetworkInfo(this);
            if (this.mWifiBackgroundConnected && networkInfo != null && networkInfo.getDetailedState() == DetailedState.CONNECTED) {
                logd("stopSelfCureWifi,  CONNECTED => POOR_LINK_DETECTED");
                sendMessage(131873);
            }
            HwSelfCureEngine.getInstance(this.myContext, this).notifySefCureCompleted(status);
            resetSelfCureParam();
            sendMessage(131893);
        }
    }

    public void stopSelfCureDelay(int status, int delay) {
        if (hasMessages(131892)) {
            removeMessages(131892);
        }
        sendMessageDelayed(obtainMessage(131892, status, 0), (long) delay);
    }

    public void exitWifiSelfCure(int exitedType, int networkId) {
        if (isWifiSelfCuring()) {
            if (networkId == -1 || networkId == getSelfCureNetworkId()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exitWifiSelfCure, CONNECT_NETWORK/FORGET_NETWORK/CLOSE_WIFI stop SCE, type = ");
                stringBuilder.append(exitedType);
                logd(stringBuilder.toString());
                WifiProCommonUtils.setWifiSelfCureStatus(0);
                HwSelfCureEngine.getInstance(this.myContext, this).notifyWifiDisconnected();
                int status = 1;
                if (exitedType == 151553 || exitedType == 151556) {
                    status = -3;
                } else {
                    boolean scanAlwaysAvailable = true;
                    if (exitedType == 1) {
                        if (Global.getInt(this.myContext.getContentResolver(), "wifi_scan_always_enabled", 0) != 1) {
                            scanAlwaysAvailable = false;
                        }
                        if (hasMessages(131891) && scanAlwaysAvailable && getCurrentState() == wifiStateMachineUtils.getDisconnectedState(this)) {
                            status = -3;
                        }
                    }
                }
                stopSelfCureDelay(status, 0);
            } else {
                logd("exitWifiSelfCure, user forget other network, do nothing.");
            }
        }
    }

    @Deprecated
    public List<String> syncGetApLinkedStaList(AsyncChannel channel) {
        log("HwWiFIStateMachine syncGetApLinkedStaList");
        Message resultMsg = channel.sendMessageSynchronously(CMD_AP_STARTED_GET_STA_LIST);
        List<String> ret = resultMsg.obj;
        resultMsg.recycle();
        return ret;
    }

    @Deprecated
    public void handleSetSoftapMacFilter(String macFilter) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwWifiStateMachine handleSetSoftapMacFilter is called, macFilter =");
        stringBuilder.append(macFilter);
        log(stringBuilder.toString());
        WifiInjector.getInstance().getWifiNative().setSoftapMacFltrHw(macFilter);
    }

    @Deprecated
    public void handleSetSoftapDisassociateSta(String mac) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwWifiStateMachine handleSetSoftapDisassociateSta is called, mac =");
        stringBuilder.append(mac);
        log(stringBuilder.toString());
        WifiInjector.getInstance().getWifiNative().disassociateSoftapStaHw(mac);
    }

    public boolean handleWapiFailureEvent(Message message, SupplicantStateTracker mSupplicantStateTracker) {
        StringBuilder stringBuilder;
        if (147474 == message.what) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Handling WAPI_EVENT, msg [");
            stringBuilder.append(message.what);
            stringBuilder.append("]");
            log(stringBuilder.toString());
            Intent intent = new Intent(SUPPLICANT_WAPI_EVENT);
            intent.putExtra("wapi_string", 16);
            this.myContext.sendBroadcast(intent);
            mSupplicantStateTracker.sendMessage(147474);
            return true;
        } else if (147475 != message.what) {
            return false;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Handling WAPI_EVENT, msg [");
            stringBuilder.append(message.what);
            stringBuilder.append("]");
            log(stringBuilder.toString());
            Intent intent2 = new Intent(SUPPLICANT_WAPI_EVENT);
            intent2.putExtra("wapi_string", 17);
            this.myContext.sendBroadcast(intent2);
            return true;
        }
    }

    public void handleStopWifiRepeater(AsyncChannel wifiP2pChannel) {
        wifiP2pChannel.sendMessage(CMD_STOP_WIFI_REPEATER);
    }

    public boolean isWifiRepeaterStarted() {
        return 1 == Global.getInt(this.myContext.getContentResolver(), "wifi_repeater_on", 0) || 6 == Global.getInt(this.myContext.getContentResolver(), "wifi_repeater_on", 0);
    }

    public void setWifiRepeaterStoped() {
        Global.putInt(this.myContext.getContentResolver(), "wifi_repeater_on", 0);
    }

    public void triggerUpdateAPInfo() {
        if (HWFLOW) {
            Log.d(TAG, "triggerUpdateAPInfo");
        }
        new Thread(new FilterScanRunnable(getScanResultsListNoCopyUnsync())).start();
    }

    public void sendStaFrequency(int frequency) {
        if (mFrequency != frequency && frequency >= 5180) {
            mFrequency = frequency;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendStaFrequency ");
            stringBuilder.append(mFrequency);
            log(stringBuilder.toString());
            Intent intent = new Intent("android.net.wifi.p2p.STA_FREQUENCY_CREATED");
            intent.putExtra("freq", String.valueOf(frequency));
            this.myContext.sendBroadcast(intent);
        }
    }

    public boolean isHiLinkActive() {
        if (this.mHiLinkController != null) {
            return this.mHiLinkController.isHiLinkActive();
        }
        return super.isHiLinkActive();
    }

    public void enableHiLinkHandshake(boolean uiEnable, String bssid) {
        if (uiEnable) {
            clearRandomMacOui();
            this.mIsRandomMacCleared = true;
        } else if (this.mIsRandomMacCleared) {
            setRandomMacOui();
            this.mIsRandomMacCleared = false;
        }
        this.mHiLinkController.enableHiLinkHandshake(uiEnable, bssid);
        WifiInjector.getInstance().getWifiNative().enableHiLinkHandshake(uiEnable, bssid);
    }

    public void sendWpsOkcStartedBroadcast() {
        this.mHiLinkController.sendWpsOkcStartedBroadcast();
    }

    public NetworkUpdateResult saveWpsOkcConfiguration(int connectionNetId, String connectionBssid) {
        List<ScanResult> scanResults = new ArrayList();
        if (!WifiInjector.getInstance().getWifiStateMachineHandler().runWithScissors(new -$$Lambda$HwWifiStateMachine$L5we5jgkU-zLvi8d8SVTYbk_Z1s(this, scanResults), WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT)) {
            Log.e(TAG, "Failed to post runnable to fetch scan results");
        }
        return this.mHiLinkController.saveWpsOkcConfiguration(connectionNetId, connectionBssid, scanResults);
    }

    public void handleAntenaPreempted() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getName());
        stringBuilder.append("EVENT_ANT_CORE_ROB");
        log(stringBuilder.toString());
        String ACTION_WIFI_ANTENNA_PREEMPTED = HwABSUtils.ACTION_WIFI_ANTENNA_PREEMPTED;
        String HUAWEI_BUSSINESS_PERMISSION = HwABSUtils.HUAWEI_BUSSINESS_PERMISSION;
        this.myContext.sendBroadcastAsUser(new Intent(ACTION_WIFI_ANTENNA_PREEMPTED), UserHandle.ALL, HUAWEI_BUSSINESS_PERMISSION);
    }

    public void handleDualbandHandoverFailed(int disableReason) {
        if (this.mWifiSwitchOnGoing && disableReason == 3 && WifiProStateMachine.getWifiProStateMachineImpl().getNetwoksHandoverType() == 4) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleDualbandHandoverFailed, disableReason = ");
            stringBuilder.append(disableReason);
            log(stringBuilder.toString());
            String failedBssid = null;
            String failedSsid = null;
            synchronized (this.selectConfigLock) {
                if (this.mSelectedConfig != null) {
                    failedBssid = this.mSelectedConfig.BSSID;
                    failedSsid = this.mSelectedConfig.SSID;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleDualbandHandoverFailed, sendWifiHandoverCompletedBroadcast, status = ");
            stringBuilder2.append(-7);
            log(stringBuilder2.toString());
            sendWifiHandoverCompletedBroadcast(-7, failedBssid, failedSsid, null);
        }
    }

    public void setWiFiProRoamingSSID(WifiSsid SSID) {
        this.mWiFiProRoamingSSID = SSID;
    }

    public WifiSsid getWiFiProRoamingSSID() {
        return this.mWiFiProRoamingSSID;
    }

    public boolean isEnterpriseHotspot(WifiConfiguration config) {
        if (config != null) {
            String currentSsid = config.SSID;
            String configKey = config.configKey();
            if (TextUtils.isEmpty(currentSsid) || TextUtils.isEmpty(configKey)) {
                return false;
            }
            List<ScanResult> scanResults = new ArrayList();
            if (WifiInjector.getInstance().getWifiStateMachineHandler().runWithScissors(new -$$Lambda$HwWifiStateMachine$Rtaw3MiMjSAm9A80baumon7OXAI(this, scanResults), WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT)) {
                int foundCounter = 0;
                for (int i = 0; i < scanResults.size(); i++) {
                    ScanResult nextResult = (ScanResult) scanResults.get(i);
                    String scanSsid = new StringBuilder();
                    scanSsid.append("\"");
                    scanSsid.append(nextResult.SSID);
                    scanSsid.append("\"");
                    scanSsid = scanSsid.toString();
                    String capabilities = nextResult.capabilities;
                    if (currentSsid.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(capabilities, configKey)) {
                        foundCounter++;
                        if (foundCounter >= 4) {
                            return true;
                        }
                    }
                }
            } else {
                Log.e(TAG, "Failed to post runnable to fetch scan results");
                return false;
            }
        }
        return false;
    }

    public String getConnectionRawPsk() {
        log("getConnectionRawPsk.");
        if (this.myContext.checkCallingPermission("com.huawei.permission.ACCESS_AP_INFORMATION") != 0) {
            log("getConnectionRawPsk: permissin denied.");
            return null;
        } else if (-1 != wifiStateMachineUtils.getWifiInfo(this).getNetworkId()) {
            String ret = WifiInjector.getInstance().getWifiNative().getConnectionRawPsk();
            log("getConnectionRawPsk: OK");
            return ret;
        } else {
            log("getConnectionRawPsk: netId is invalid.");
            return null;
        }
    }

    protected void notifyWlanChannelNumber(int channel) {
        if (channel > 13) {
            channel = 0;
        }
        WifiCommonUtils.notifyDeviceState("WLAN", String.valueOf(channel), "");
    }

    protected void notifyWlanState(String state) {
        WifiCommonUtils.notifyDeviceState("WLAN", state, "");
    }

    private long getScanInterval() {
        long scanInterval;
        if (wifiStateMachineUtils.getOperationalMode(this) == 3) {
            scanInterval = Global.getLong(this.myContext.getContentResolver(), WIFI_SCAN_INTERVAL_WLAN_CLOSE, WIFI_SCAN_INTERVAL_WLAN_CLOSE_DEFAULT);
        } else if (wifiStateMachineUtils.getNetworkInfo(this).isConnected()) {
            scanInterval = Global.getLong(this.myContext.getContentResolver(), WIFI_SCAN_INTERVAL_WHITE_WLAN_CONNECTED, 10000);
        } else {
            scanInterval = Global.getLong(this.myContext.getContentResolver(), WIFI_SCAN_INTERVAL_WLAN_NOT_CONNECTED, 10000);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("the wifi_scan interval is:");
        stringBuilder.append(scanInterval);
        logd(stringBuilder.toString());
        return scanInterval;
    }

    public synchronized boolean allowWifiScanRequest(int pid) {
        int i = pid;
        synchronized (this) {
            if (ctrlScanForConnection()) {
                return true;
            }
            RunningAppProcessInfo appProcessInfo = getAppProcessInfoByPid(pid);
            if (!(i <= 0 || appProcessInfo == null || appProcessInfo.pkgList == null)) {
                if (!this.mIsScanCtrlPluggedin) {
                    if (isGlobalScanCtrl(appProcessInfo)) {
                        logd("isGlobalScanCtrl contrl scan ");
                        sendMessageDelayed(CMD_SCREEN_OFF_SCAN, WIFI_SCAN_RESULT_DELAY_TIME_DEFAULT);
                        return true;
                    }
                    wifiScanBlackListLearning(appProcessInfo);
                    long scanInterval = getScanInterval();
                    if (isWifiScanBlacklisted(appProcessInfo, scanInterval)) {
                        long now = System.currentTimeMillis();
                        long appLastScanRequestTimestamp = 0;
                        if (this.mPidLastScanSuccTimestamp.containsKey(Integer.valueOf(pid))) {
                            appLastScanRequestTimestamp = ((Long) this.mPidLastScanSuccTimestamp.get(Integer.valueOf(pid))).longValue();
                        }
                        if (this.lastScanResultTimestamp == 0 || (now - this.lastScanResultTimestamp >= scanInterval && now - appLastScanRequestTimestamp >= scanInterval)) {
                            this.mPidLastScanSuccTimestamp.put(Integer.valueOf(pid), Long.valueOf(now));
                        } else {
                            if (now - this.lastScanResultTimestamp < 0) {
                                logd("wifi_scan the last scan time is jump!!!");
                                this.lastScanResultTimestamp = now;
                            }
                            sendMessageDelayed(CMD_SCREEN_OFF_SCAN, WIFI_SCAN_RESULT_DELAY_TIME_DEFAULT);
                            return true;
                        }
                    }
                    updateGlobalScanTimes();
                    return false;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("wifi_scan pid[");
            stringBuilder.append(i);
            stringBuilder.append("] is not correct or is charging. mIsScanCtrlPluggedin = ");
            stringBuilder.append(this.mIsScanCtrlPluggedin);
            stringBuilder.append(" isInGlobalScanCtrl = ");
            stringBuilder.append(this.isInGlobalScanCtrl);
            logd(stringBuilder.toString());
            return false;
        }
    }

    public boolean isRSDBSupported() {
        return WifiInjector.getInstance().getWifiNative().isSupportRsdbByDriver();
    }

    protected void handleSimAbsent(WifiConfiguration config) {
        WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this);
        if (PreconfiguredNetworkManager.IS_R1 && config.enterpriseConfig != null && TelephonyUtil.isSimEapMethod(config.enterpriseConfig.getEapMethod()) && PreconfiguredNetworkManager.getInstance().isPreconfiguredNetwork(config.SSID)) {
            wifiConfigManager.disableNetwork(config.networkId, 1000);
            this.wifiEapUIManager.showDialog(Resources.getSystem().getString(33686183), Resources.getSystem().getString(33686181));
        }
    }

    protected void handleEapErrorcodeReport(int networkId, String ssid, int errorCode) {
        if (PreconfiguredNetworkManager.IS_R1 && PreconfiguredNetworkManager.getInstance().isPreconfiguredNetwork(ssid)) {
            wifiStateMachineUtils.getWifiConfigManager(this).updateNetworkSelectionStatus(networkId, 15);
            this.wifiEapUIManager.showDialog(errorCode);
        }
    }

    private void wifiScanBlackListLearning(RunningAppProcessInfo appProcessInfo) {
        long now = System.currentTimeMillis();
        long scanInterval = getScanInterval();
        int pid = appProcessInfo.pid;
        clearDeadPidCache();
        if (this.mPidLastScanTimestamp.containsKey(Integer.valueOf(pid))) {
            if (!this.mPidWifiScanCount.containsKey(Integer.valueOf(pid))) {
                this.mPidWifiScanCount.put(Integer.valueOf(pid), Integer.valueOf(0));
            }
            long tmpLastScanRequestTimestamp = ((Long) this.mPidLastScanTimestamp.get(Integer.valueOf(pid))).longValue();
            this.mPidLastScanTimestamp.put(Integer.valueOf(pid), Long.valueOf(now));
            if (tmpLastScanRequestTimestamp != 0 && now >= tmpLastScanRequestTimestamp) {
                if (isWifiScanInBlacklistCache(pid) || now - tmpLastScanRequestTimestamp >= scanInterval) {
                    if (isWifiScanInBlacklistCache(pid) && now - tmpLastScanRequestTimestamp > WIFI_SCAN_BLACKLIST_REMOVE_INTERVAL) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("wifi_scan blacklist cache remove pid:");
                        stringBuilder.append(pid);
                        logd(stringBuilder.toString());
                        removeWifiScanBlacklistCache(pid);
                    }
                    this.mPidWifiScanCount.put(Integer.valueOf(pid), Integer.valueOf(0));
                    return;
                }
                int count = ((Integer) this.mPidWifiScanCount.get(Integer.valueOf(pid))).intValue() + 1;
                this.mPidWifiScanCount.put(Integer.valueOf(pid), Integer.valueOf(count));
                if (((long) count) >= WIFI_SCAN_OVER_INTERVAL_MAX_COUNT) {
                    this.mPidLastScanTimestamp.remove(Integer.valueOf(pid));
                    this.mPidWifiScanCount.remove(Integer.valueOf(pid));
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("pid:");
                    stringBuilder2.append(pid);
                    stringBuilder2.append(" wifi_scan interval is frequent");
                    logd(stringBuilder2.toString());
                    if (!isWifiScanWhitelisted(appProcessInfo)) {
                        addWifiScanBlacklistCache(appProcessInfo);
                    }
                }
                return;
            }
            return;
        }
        this.mPidLastScanTimestamp.put(Integer.valueOf(pid), Long.valueOf(now));
        this.mPidWifiScanCount.put(Integer.valueOf(pid), Integer.valueOf(0));
    }

    private boolean isWifiScanInBlacklistCache(int pid) {
        StringBuilder stringBuilder;
        for (Entry<String, Integer> entry : this.mPidBlackList.entrySet()) {
            if (pid == ((Integer) entry.getValue()).intValue()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("pid:");
                stringBuilder.append(pid);
                stringBuilder.append(" in wifi_scan cache blacklist, appname=");
                stringBuilder.append((String) entry.getKey());
                logd(stringBuilder.toString());
                return true;
            }
        }
        for (Entry<String, Integer> entry2 : this.mPidConnectedBlackList.entrySet()) {
            if (pid == ((Integer) entry2.getValue()).intValue()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("pid:");
                stringBuilder.append(pid);
                stringBuilder.append(" in wifi_scan connected cache blacklist, appname=");
                stringBuilder.append((String) entry2.getKey());
                logd(stringBuilder.toString());
                return true;
            }
        }
        return false;
    }

    private void removeWifiScanBlacklistCache(int pid) {
        Entry<String, Integer> entry;
        String key;
        StringBuilder stringBuilder;
        this.mPidLastScanSuccTimestamp.remove(Integer.valueOf(pid));
        this.mPidLastScanTimestamp.remove(Integer.valueOf(pid));
        this.mPidWifiScanCount.remove(Integer.valueOf(pid));
        Iterator iter = this.mPidBlackList.entrySet().iterator();
        while (iter.hasNext()) {
            entry = (Entry) iter.next();
            if (pid == ((Integer) entry.getValue()).intValue()) {
                key = (String) entry.getKey();
                stringBuilder = new StringBuilder();
                stringBuilder.append("pid:");
                stringBuilder.append(pid);
                stringBuilder.append(" remove from wifi_scan cache blacklist success, appname=");
                stringBuilder.append(key);
                logd(stringBuilder.toString());
                iter.remove();
                break;
            }
        }
        iter = this.mPidConnectedBlackList.entrySet().iterator();
        while (iter.hasNext()) {
            entry = (Entry) iter.next();
            if (pid == ((Integer) entry.getValue()).intValue()) {
                key = (String) entry.getKey();
                stringBuilder = new StringBuilder();
                stringBuilder.append("pid:");
                stringBuilder.append(pid);
                stringBuilder.append(" remove from wifi_scan connected cache blacklist success, appname=");
                stringBuilder.append(key);
                logd(stringBuilder.toString());
                iter.remove();
                return;
            }
        }
    }

    private void addWifiScanBlacklistCache(RunningAppProcessInfo appProcessInfo) {
        int pid = appProcessInfo.pid;
        String appName = appProcessInfo.pkgList[0];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pid:");
        stringBuilder.append(pid);
        stringBuilder.append(" add to wifi_scan connected limited blacklist");
        logd(stringBuilder.toString());
        if (wifiStateMachineUtils.getNetworkInfo(this).isConnected()) {
            this.mPidConnectedBlackList.put(appName, Integer.valueOf(pid));
        } else {
            this.mPidBlackList.put(appName, Integer.valueOf(pid));
        }
    }

    private boolean isWifiScanBlacklisted(RunningAppProcessInfo appProcessInfo, long scanInterval) {
        if (isPackagesNamesMatched(appProcessInfo.pkgList, this.myContext.getResources().getStringArray(33816587), null)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("config blacklist wifi_scan name:callingPkgNames[pid=");
            stringBuilder.append(appProcessInfo.pid);
            stringBuilder.append("]=");
            stringBuilder.append(appProcessInfo.processName);
            logd(stringBuilder.toString());
            return true;
        }
        if (wifiStateMachineUtils.getNetworkInfo(this).isConnected()) {
            this.mPidBlackList.clear();
        } else {
            this.mPidConnectedBlackList.clear();
        }
        if (!(isWifiScanConnectedLimitedWhitelisted(appProcessInfo) || this.mPidBlackListInteval <= 0 || this.mPidBlackListInteval == scanInterval)) {
            logd("wifi_scan blacklist clear because the interval is change");
            this.mPidBlackList.clear();
            this.mPidBlackListInteval = 0;
        }
        return isWifiScanInBlacklistCache(appProcessInfo.pid);
    }

    private boolean isPackagesNamesMatched(String[] callingPkgNames, String[] whitePkgs, String whiteDbPkgs) {
        int whitePkgsLength = 0;
        if (whitePkgs != null) {
            whitePkgsLength = whitePkgs.length;
        }
        if (callingPkgNames == null || (whiteDbPkgs == null && whitePkgsLength == 0)) {
            logd("wifi_scan input PkgNames are not correct");
            return false;
        }
        int j;
        StringBuilder stringBuilder;
        for (j = 0; j < whitePkgsLength; j++) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("config--list:");
            stringBuilder.append(whitePkgs[j]);
            logd(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("config--db:");
        stringBuilder.append(whiteDbPkgs);
        logd(stringBuilder.toString());
        int i = 0;
        while (i < callingPkgNames.length) {
            StringBuilder stringBuilder2;
            for (j = 0; j < whitePkgsLength; j++) {
                if (callingPkgNames[i].equals(whitePkgs[j])) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("config white wifi_scan name:callingPkgNames[");
                    stringBuilder2.append(Integer.toString(i));
                    stringBuilder2.append("]=");
                    stringBuilder2.append(callingPkgNames[i]);
                    logd(stringBuilder2.toString());
                    return true;
                }
            }
            if (whiteDbPkgs == null || !TextUtils.delimitedStringContains(whiteDbPkgs, ',', callingPkgNames[i])) {
                i++;
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("db white wifi_scan name:callingPkgNames[");
                stringBuilder2.append(Integer.toString(i));
                stringBuilder2.append("]=");
                stringBuilder2.append(callingPkgNames[i]);
                logd(stringBuilder2.toString());
                return true;
            }
        }
        return false;
    }

    private boolean isWifiScanConnectedLimitedWhitelisted(RunningAppProcessInfo appProcessInfo) {
        String[] callingPkgNames = appProcessInfo.pkgList;
        String[] whitePkgs = this.myContext.getResources().getStringArray(33816588);
        String whiteDbPkgs = Global.getString(this.myContext.getContentResolver(), WIFI_SCAN_CONNECTED_LIMITED_WHITE_PACKAGENAME);
        if (appProcessInfo.uid == 1000) {
            return true;
        }
        if (!isPackagesNamesMatched(callingPkgNames, whitePkgs, whiteDbPkgs)) {
            return false;
        }
        logd("wifi_scan pkgname is in connected whitelist pkgs");
        return true;
    }

    private boolean isWifiScanWhitelisted(RunningAppProcessInfo appProcessInfo) {
        if (isPackagesNamesMatched(appProcessInfo.pkgList, this.myContext.getResources().getStringArray(33816589), Global.getString(this.myContext.getContentResolver(), WIFI_SCAN_WHITE_PACKAGENAME))) {
            logd("wifi_scan pkgname is in whitelist pkgs");
            return true;
        } else if (Binder.getCallingUid() == 1000 || Binder.getCallingUid() == HwArpVerifier.MSG_DUMP_LOG) {
            return true;
        } else {
            return false;
        }
    }

    public synchronized void updateLastScanRequestTimestamp() {
        this.lastScanResultTimestamp = System.currentTimeMillis();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wifi_scan update lastScanResultTimestamp=");
        stringBuilder.append(this.lastScanResultTimestamp);
        logd(stringBuilder.toString());
    }

    private RunningAppProcessInfo getAppProcessInfoByPid(int pid) {
        List<RunningAppProcessInfo> appProcessList = ((ActivityManager) this.myContext.getSystemService("activity")).getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == pid) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PkgInfo--uid=");
                stringBuilder.append(appProcess.uid);
                stringBuilder.append(", processName=");
                stringBuilder.append(appProcess.processName);
                stringBuilder.append(",pid=");
                stringBuilder.append(pid);
                logd(stringBuilder.toString());
                return appProcess;
            }
        }
        return null;
    }

    private void clearDeadPidCache() {
        List<RunningAppProcessInfo> appProcessList = ((ActivityManager) this.myContext.getSystemService("activity")).getRunningAppProcesses();
        ArrayList<Integer> tmpPidSet = new ArrayList();
        Iterator iter = this.mPidLastScanTimestamp.entrySet().iterator();
        if (appProcessList != null) {
            for (RunningAppProcessInfo appProcess : appProcessList) {
                tmpPidSet.add(Integer.valueOf(appProcess.pid));
            }
            while (iter.hasNext()) {
                Integer key = (Integer) ((Entry) iter.next()).getKey();
                if (!tmpPidSet.contains(key)) {
                    iter.remove();
                    this.mPidWifiScanCount.remove(key);
                    this.mPidLastScanSuccTimestamp.remove(key);
                }
            }
        }
    }

    public void transitionToCallback(IState destState) {
        if (this.mDestStates != null) {
            this.mDestStates.offer(destState);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("transition to ");
        stringBuilder.append(destState.getClass().getSimpleName());
        stringBuilder.append(" begining.");
        Log.i(str, stringBuilder.toString());
    }

    protected void onPostHandleMessage(Message msg) {
        if (this.mDestStates != null) {
            IState destState = (IState) this.mDestStates.poll();
            if (destState != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("transition to ");
                stringBuilder.append(destState.getClass().getSimpleName());
                stringBuilder.append(" finished.");
                Log.i(str, stringBuilder.toString());
            }
        }
    }

    private void setLowPwrMode(boolean isConnected, String ssid, boolean isMobileAP, boolean isScreenOn) {
        String hwSsid = "\"Huawei-Employee\"";
        String cloneSsid = "CloudClone";
        boolean isHwSsid = false;
        boolean isCloneSsid = false;
        if (ssid != null) {
            isHwSsid = ssid.equals(hwSsid);
            isCloneSsid = ssid.contains(cloneSsid);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setpmlock:isConnected: ");
        stringBuilder.append(isConnected);
        stringBuilder.append(" ssid:");
        stringBuilder.append(ssid);
        stringBuilder.append(" isMobileAP:");
        stringBuilder.append(isMobileAP);
        stringBuilder.append(" isAndroidMobileAP:");
        stringBuilder.append(isAndroidMobileAP());
        logd(stringBuilder.toString());
        if (isConnected && (isHwSsid || ((isMobileAP && isAndroidMobileAP() && !isCloneSsid) || !isScreenOn || this.mssArbi.matchAllowMSSApkList()))) {
            WifiInjector.getInstance().getWifiNative().gameKOGAdjustSpeed(0, 7);
        } else {
            WifiInjector.getInstance().getWifiNative().gameKOGAdjustSpeed(0, 6);
        }
    }

    private void pwrBoostRegisterBcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addCategory("android.net.wifi.STATE_CHANGE@hwBrExpand@WifiNetStatus=WIFICON|WifiNetStatus=WIFIDSCON");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.myContext.registerReceiver(this.mBcastReceiver, filter);
    }

    private void linkMeasureAndStatic(boolean enable) {
        long arpRtt = 0;
        int arpCnt = 0;
        HwArpVerifier mArpVerifier = HwArpVerifier.getDefault();
        int dltTxGoodCnt = 0;
        int dltTxBadCnt = 0;
        TxPacketCounters txPacketCounters = WifiInjector.getInstance().getWifiNative().getTxPacketCounters(wifiStateMachineUtils.getInterfaceName(this));
        if (txPacketCounters != null) {
            int i;
            int lastTxGoodCnt = txPacketCounters.txSucceeded;
            int lastTxGoodCnt2 = txPacketCounters.txFailed;
            int lastTxBadCnt = wifiStateMachineUtils.getWifiInfo(this).txRetries;
            if (mArpVerifier != null) {
                for (i = 0; i < 5; i++) {
                    long ret = mArpVerifier.getGateWayArpRTT(1000);
                    if (ret != -1) {
                        arpRtt += ret;
                        arpCnt++;
                    }
                }
            }
            txPacketCounters = WifiInjector.getInstance().getWifiNative().getTxPacketCounters(wifiStateMachineUtils.getInterfaceName(this));
            if (txPacketCounters != null) {
                i = txPacketCounters.txSucceeded - lastTxGoodCnt;
                int dltTxBadCnt2 = txPacketCounters.txFailed - lastTxGoodCnt2;
                txPacketCounters = wifiStateMachineUtils.getWifiInfo(this).txRetries - lastTxBadCnt;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pwr:dltTxGoodCnt:");
                stringBuilder.append(i);
                stringBuilder.append(" dltTxBadCnt:");
                stringBuilder.append(dltTxBadCnt2);
                stringBuilder.append(" dltTxRetries:");
                stringBuilder.append(txPacketCounters);
                stringBuilder.append(" arpRtt:");
                stringBuilder.append(arpRtt);
                stringBuilder.append(" arpCnt:");
                stringBuilder.append(arpCnt);
                stringBuilder.append(" enable:");
                stringBuilder.append(enable);
                logd(stringBuilder.toString());
                this.mHwWifiCHRService.txPwrBoostChrStatic(Boolean.valueOf(enable), (int) arpRtt, arpCnt, i, dltTxBadCnt2, (int) txPacketCounters);
                return;
            }
            int i2 = lastTxGoodCnt;
        }
    }

    public int isAllowedManualWifiPwrBoost() {
        return this.mIsAllowedManualPwrBoost;
    }

    public boolean isWifiConnectivityManagerEnabled() {
        return this.mWifiConnectivityManager != null && this.mWifiConnectivityManager.isWifiConnectivityManagerEnabled();
    }

    private void clearPwrBoostChrStatus() {
        this.mCurrentPwrBoostStat = false;
        this.mIsFinishLinkDetect = false;
        this.mPwrBoostOncnt = 0;
        this.mPwrBoostOffcnt = 0;
    }

    private boolean isGlobalScanCtrl(RunningAppProcessInfo appProcessInfo) {
        logd("isGlobalScanCtrl begin ");
        if (!isWifiScanWhitelisted(appProcessInfo)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("wifi_scan return isInGlobalScanCtrl = ");
            stringBuilder.append(this.isInGlobalScanCtrl);
            logd(stringBuilder.toString());
            if (!this.isInGlobalScanCtrl || System.currentTimeMillis() - this.mLastScanTimestamp > TIMEOUT_CONTROL_SCAN_ASSOCIATED) {
                return false;
            }
            return true;
        }
        return false;
    }

    private void updateGlobalScanTimes() {
        long now = System.currentTimeMillis();
        long scanInterval = now - this.mLastScanTimestamp;
        this.mLastScanTimestamp = now;
        if (scanInterval > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("wifi_scan interval = ");
            stringBuilder.append(scanInterval);
            stringBuilder.append(" mFoulTimes = ");
            stringBuilder.append(this.mFoulTimes);
            stringBuilder.append(" mFreedTimes = ");
            stringBuilder.append(this.mFreedTimes);
            stringBuilder.append(" isInGlobalScanCtrl = ");
            stringBuilder.append(this.isInGlobalScanCtrl);
            logd(stringBuilder.toString());
            if (this.isInGlobalScanCtrl) {
                if (scanInterval > 10000) {
                    this.mFreedTimes++;
                } else {
                    this.mFreedTimes = 0;
                }
                if (this.mFreedTimes >= 5) {
                    this.mFoulTimes = 0;
                    this.mFreedTimes = 0;
                    this.isInGlobalScanCtrl = false;
                }
            } else {
                if (scanInterval < TIMEOUT_CONTROL_SCAN_ASSOCIATED) {
                    this.mFoulTimes++;
                } else {
                    this.mFoulTimes = 0;
                }
                if (this.mFoulTimes >= 5) {
                    this.mFoulTimes = 0;
                    this.mFreedTimes = 0;
                    this.isInGlobalScanCtrl = true;
                }
            }
        }
    }

    public boolean getChargingState() {
        String flag = "1";
        String usb = HwArpVerifier.readFileByChars(USB_SUPPLY);
        if (usb.length() == 0) {
            usb = HwArpVerifier.readFileByChars(USB_SUPPLY_QCOM);
        }
        if (flag.equals(usb.trim())) {
            return true;
        }
        logd("getChargingState return false");
        return false;
    }

    void registHwSoftApManager(HwSoftApManager hwSoftApManager) {
        this.mHwSoftApManager = hwSoftApManager;
        log("HwSoftApManager registed");
    }

    void clearHwSoftApManager() {
        log("Clear HwSoftApManager");
        if (this.mHwSoftApManager != null) {
            this.mHwSoftApManager.clearCallbacksAndMessages();
        }
        this.mHwSoftApManager = null;
    }

    public List<String> getApLinkedStaList() {
        if (this.mHwSoftApManager != null) {
            return this.mHwSoftApManager.getApLinkedStaList();
        }
        Log.w(TAG, "getApLinkedStaList called when mHwSoftApManager is not registed");
        return Collections.emptyList();
    }

    public int[] getSoftApChannelListFor5G() {
        HwSoftApManager hwSoftApManager = this.mHwSoftApManager;
        return HwSoftApManager.getSoftApChannelListFor5G();
    }

    public void setSoftapDisassociateSta(String mac) {
        if (this.mHwSoftApManager != null) {
            this.mHwSoftApManager.setSoftApDisassociateSta(mac);
        } else {
            Log.w(TAG, "setSoftapDisassociateSta called when mHwSoftApManager is not registed");
        }
    }

    public void setSoftapMacFilter(String macFilter) {
        if (this.mHwSoftApManager != null) {
            this.mHwSoftApManager.setSoftapMacFilter(macFilter);
        } else {
            Log.w(TAG, "setSoftapMacFilter called when mHwSoftApManager is not registed");
        }
    }

    public boolean isAndroidMobileAP() {
        String androidMobileIpAddress = "192.168.43.";
        String ipAddress = "";
        WifiInfo wifiInfo = getWifiInfo();
        if (wifiInfo != null) {
            ipAddress = intIpToStringIp(wifiInfo.getIpAddress());
        }
        if (ipAddress == null || !ipAddress.startsWith(androidMobileIpAddress)) {
            return false;
        }
        return true;
    }

    private String intIpToStringIp(int ip) {
        return String.format("%d.%d.%d.%d", new Object[]{Integer.valueOf(ip & 255), Integer.valueOf((ip >> 8) & 255), Integer.valueOf((ip >> 16) & 255), Integer.valueOf((ip >> 24) & 255)});
    }

    public void startPacketKeepalive(Message msg) {
        KeepalivePacketData data = msg.obj;
        if (data != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startPacketKeepalive msg.arg1 = ");
            stringBuilder.append(msg.arg1);
            stringBuilder.append(" msg.arg2 =");
            stringBuilder.append(msg.arg2);
            stringBuilder.append(" srcPort = ");
            stringBuilder.append(data.srcPort);
            stringBuilder.append(" dstPort = ");
            stringBuilder.append(data.dstPort);
            Log.e(str, stringBuilder.toString());
        } else {
            Log.e(TAG, "startPacketKeepalive data == null");
        }
        sendMessage(131232, msg.arg1, msg.arg2, msg.obj);
    }

    public void stopPacketKeepalive(Message msg) {
        KeepalivePacketData data = msg.obj;
        if (data != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopPacketKeepalive msg.arg1 = ");
            stringBuilder.append(msg.arg1);
            stringBuilder.append(" msg.arg2 =");
            stringBuilder.append(msg.arg2);
            stringBuilder.append(" srcPort = ");
            stringBuilder.append(data.srcPort);
            stringBuilder.append(" dstPort = ");
            stringBuilder.append(data.dstPort);
            Log.e(str, stringBuilder.toString());
        } else {
            Log.e(TAG, "stopPacketKeepalive data == null");
        }
        sendMessage(131233, msg.arg1, msg.arg2, msg.obj);
    }
}
