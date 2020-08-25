package com.android.server;

import android.common.HwFrameworkFactory;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.usb.UsbManager;
import android.hdm.HwDeviceManager;
import android.net.ConnectivityManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.booster.IHwCommBoosterCallback;
import android.net.booster.IHwCommBoosterServiceManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.HwInnerTelephonyManagerImpl;
import android.telephony.HwTelephonyManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.connectivity.NetdEventListenerService;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.connectivity.Tethering;
import com.android.server.connectivity.tethering.TetheringConfiguration;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.intellicom.networkslice.css.NetworkSlicesHandler;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.android.app.admin.DeviceVpnManager;
import com.huawei.deliver.info.HwDeliverInfo;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import huawei.android.net.IConnectivityExManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class HwConnectivityManagerImpl implements HwConnectivityManager {
    private static final int ADD_NETWORK_ACCESS_LIST = 0;
    private static final int BINARY = 2;
    private static final int BLACK_LIST = 1;
    private static final String BOOT_PERMISSION = "android.permission.RECEIVE_BOOT_COMPLETED";
    private static final String CHR_BROADCAST_PERMISSION = "com.huawei.android.permission.GET_CHR_DATA";
    private static final int CODE_SET_NETWORK_ACCESS_LIST = 1106;
    public static final int CURRENT_CONNECT_TO_CELLULAR = 2;
    public static final int CURRENT_CONNECT_TO_WLAN = 1;
    private static final int DATA_SEND_TO_HWCM_DNS_COLLECT = 10;
    private static final boolean DBG = true;
    private static final String DESCRIPTOR_NETWORKMANAGEMENT_SERVICE = "android.os.INetworkManagementService";
    private static final String DISABLE_VPN = "disable-vpn";
    private static final int DNS_BIG_LATENCY = 2000;
    private static final int DNS_ERROR_IPV6_TIMEOUT = 15;
    private static final String DNS_EVENT_KEY_LANENCY = "latency";
    private static final String DNS_EVENT_KEY_NETID = "netid";
    private static final String DNS_EVENT_KEY_RETURNCODE = "returnCode";
    public static final String DNS_EVENT_KEY_UID = "uid";
    private static final int DNS_FAIL_REPORT_COUNT = 6;
    private static final int DNS_FAIL_REPORT_TIMESPAN = 45000;
    private static final int DNS_FAIL_REPORT_TIME_INTERVAL = 600000;
    private static final int DNS_FAIL_SAMPLING_RATIO = 8;
    private static final int DNS_LATENCY_1000 = 1000;
    private static final int DNS_LATENCY_150 = 150;
    private static final int DNS_LATENCY_20 = 20;
    private static final int DNS_LATENCY_500 = 500;
    private static final int DNS_OVER2000_REPORT_COUNT = 6;
    private static final int DNS_OVER2000_REPORT_TIMESPAN = 45000;
    private static final int DNS_OVER2000_REPORT_TIME_INTERVAL = 600000;
    private static final int DNS_OVER2000_SAMPLING_RATIO = 2;
    private static final int DNS_REPORT_COUNTING_INTERVAL = 100;
    private static final int DNS_REQUEST_MIN_DELAY_TIME = 8;
    private static int DNS_STAT_ENUM_ENDC = 1;
    private static int DNS_STAT_ENUM_LTE = 0;
    private static int DNS_STAT_ENUM_NRSA = 2;
    private static final int DNS_SUCCESS = 0;
    private static final int DNS_TIME_MIN = 10;
    private static final int DSQOE_START_DNS_MONITOR = 0;
    private static final int DSQOE_STOP_DNS_MONITOR = 1;
    private static final int EXIST_BLACK_AND_DOMAIN_NETWORK_POLICY_FLAG = 3;
    private static final int EXIST_BLACK_BUT_NOT_DOMAIN_NETWORK_POLICY_FLAG = 2;
    private static final int EXIST_WHITE_AND_DOMAIN_NETWORK_POLICY_FLAG = 1;
    private static final int EXIST_WHITE_BUT_NOT_DOMAIN_NETWORK_POLICY_FLAG = 0;
    protected static final boolean HWFLOW = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String HW_SYSTEM_SERVER_START = "com.huawei.systemserver.START";
    private static final boolean INIT_PDN_WIFI = SystemProperties.getBoolean("ro.config.forbid_roam_dun_wifi", false);
    private static final String INTENT_DS_DNS_STATISTICS = "com.intent.action.dns_statistics";
    private static final String INTENT_DS_WEB_STAT_REPORT = "com.android.intent.action.web_stat_report";
    private static final String INTENT_WIFI_DNS_STATISTICS = "com.intent.action.wifi_dns_statistics";
    private static final boolean IS_CHINA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final boolean IS_DOCOMO = (SystemProperties.get("ro.config.hw_opta", "").equals("341") && SystemProperties.get("ro.config.hw_optb", "").equals("392"));
    private static final boolean IS_NR_SLICES_SUPPORTED = HwFrameworkFactory.getHwInnerTelephonyManager().isNrSlicesSupported();
    private static final String KEY_NETWORK_POLICY_FLAG = "network_policy";
    private static final String LOG_TAG = "HwConnectivityManagerImpl";
    static final Uri MSIM_TELEPHONY_CARRIERS_URI = Uri.parse("content://telephony/carriers/subId");
    private static final int NETWORK_POLICY_NOT_SET = -1;
    public static final int NOT_CONNECT_TO_NETWORK = 0;
    private static final int NSA_STATE0 = 0;
    private static final int NSA_STATE1 = 1;
    private static final int NSA_STATE2 = 2;
    private static final int NSA_STATE3 = 3;
    private static final int NSA_STATE4 = 4;
    private static final int NSA_STATE5 = 5;
    private static final int NSA_STATE6 = 6;
    private static final int ON_DNS_EVENT_MSG = 0;
    private static final int ON_SET_IP_TABLES_MSG = 1;
    private static final String P2P_TETHER_IFAC = "p2p-wlan0-";
    private static final String P2P_TETHER_IFAC_110x = "p2p-p2p0-";
    private static final String P2P_TETHER_IFAC_QCOM = "p2p0";
    protected static final String PROPERTY_BTHOTSPOT_ON = "sys.isbthotspoton";
    protected static final String PROPERTY_USBTETHERING_ON = "sys.isusbtetheringon";
    protected static final String PROPERTY_WIFIHOTSPOT_ON = "sys.iswifihotspoton";
    private static final int PS_AP_SLOW_DNS_BIG_LATENCY = 6;
    private static final int PS_AP_SLOW_DNS_FAIL = 5;
    private static final String SECURE_VPN = "secure-vpn";
    public static final String SET_IP_TABLES_KEY_HOST_NAME = "hostName";
    public static final String SET_IP_TABLES_KEY_IP_COUNT = "ipCount";
    public static final String SET_IP_TABLES_KEY_IP_LIST = "ipList";
    private static final int SET_NETWORK_ACCESS_LIST = 1;
    /* access modifiers changed from: private */
    public static final String TAG = null;
    private static final String VALID_PKGNAME_DNS = "com.android.server.dns";
    private static final boolean VDBG = false;
    private static final int WHITE_LIST = 0;
    private static final String WIFI_AP_MANUAL_CONNECT = "wifi_ap_manual_connect";
    /* access modifiers changed from: private */
    public static int cellNetId = 0;
    private static HwConnectivityManager mInstance = new HwConnectivityManagerImpl();
    private static List<String> sNetworkListCacheOfDns = new ArrayList();
    /* access modifiers changed from: private */
    public static int wifiNetId = 0;
    private DnsQueryStat cellEndcDnsStat = new DnsQueryStat("cell");
    private DnsQueryStat cellLteDnsStat = new DnsQueryStat("cell");
    private DnsQueryStat cellNrSaDnsStat = new DnsQueryStat("cell");
    /* access modifiers changed from: private */
    public ConnectivityManager mConnMgr = null;
    /* access modifiers changed from: private */
    public int mConnectedType = 0;
    private final BroadcastReceiver mConnectivityChangeReceiver = new BroadcastReceiver() {
        /* class com.android.server.HwConnectivityManagerImpl.AnonymousClass3 */

        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && (action = intent.getAction()) != null) {
                if (SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE.equals(action)) {
                    ConnectivityManager unused = HwConnectivityManagerImpl.this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
                    if (HwConnectivityManagerImpl.this.mConnMgr != null) {
                        HwConnectivityManagerImpl hwConnectivityManagerImpl = HwConnectivityManagerImpl.this;
                        NetworkInfo unused2 = hwConnectivityManagerImpl.mNetworkInfoWlan = hwConnectivityManagerImpl.mConnMgr.getNetworkInfo(1);
                        HwConnectivityManagerImpl hwConnectivityManagerImpl2 = HwConnectivityManagerImpl.this;
                        NetworkInfo unused3 = hwConnectivityManagerImpl2.mNetworkInfoMobile = hwConnectivityManagerImpl2.mConnMgr.getNetworkInfo(0);
                        if (!(HwConnectivityManagerImpl.this.mNetworkInfoWlan == null || HwConnectivityManagerImpl.this.mNetworkInfoMobile == null)) {
                            if (HwConnectivityManagerImpl.this.mNetworkInfoWlan.isConnected()) {
                                int unused4 = HwConnectivityManagerImpl.this.mConnectedType = 1;
                            } else if (HwConnectivityManagerImpl.this.mNetworkInfoMobile.isConnected()) {
                                int unused5 = HwConnectivityManagerImpl.this.mConnectedType = 2;
                            } else {
                                int unused6 = HwConnectivityManagerImpl.this.mConnectedType = 0;
                            }
                        }
                        Network[] networks = HwConnectivityManagerImpl.this.mConnMgr.getAllNetworks();
                        for (Network network : networks) {
                            NetworkInfo networkInfo = HwConnectivityManagerImpl.this.mConnMgr.getNetworkInfo(network);
                            if (networkInfo != null) {
                                if (networkInfo.getType() == 1) {
                                    int unused7 = HwConnectivityManagerImpl.wifiNetId = network.netId;
                                }
                                if (networkInfo.getType() == 0) {
                                    int unused8 = HwConnectivityManagerImpl.cellNetId = network.netId;
                                }
                            }
                        }
                        HwConnectivityManagerImpl.this.clearInvalidPrivateDnsNetworkInfo();
                    }
                } else if (HwConnectivityManagerImpl.HW_SYSTEM_SERVER_START.equals(action)) {
                    Log.d(HwConnectivityManagerImpl.LOG_TAG, "BroadcastReceiver booster");
                    HwConnectivityManagerImpl.this.initCommBoosterManager();
                }
            }
        }
    };
    private Context mContex = null;
    /* access modifiers changed from: private */
    public DataServiceQoeDnsMonitor mDataServiceQoeDnsMonitor = null;
    private Handler mDnsEventHandler = null;
    private LinkedList<Date> mDnsFailQ = new LinkedList<>();
    private LinkedList<Date> mDnsOver2000Q = new LinkedList<>();
    /* access modifiers changed from: private */
    public IHwCommBoosterServiceManager mHwCommBoosterServiceManager = null;
    private HwConnectivityService mHwConnectivityService = null;
    private IHwCommBoosterCallback mIHwCommBoosterCallback = new IHwCommBoosterCallback.Stub() {
        /* class com.android.server.HwConnectivityManagerImpl.AnonymousClass1 */

        public void callBack(int type, Bundle data) throws RemoteException {
            if (data == null) {
                Log.e(HwConnectivityManagerImpl.LOG_TAG, "data null");
            } else if (HwConnectivityManagerImpl.this.mDataServiceQoeDnsMonitor == null) {
                Log.e(HwConnectivityManagerImpl.LOG_TAG, "mDataServiceQoeDnsMonitor null");
            } else if (type == 10) {
                int action = data.getInt(AwarenessConstants.DATA_ACTION_TYPE, -1);
                int timer = data.getInt("timer", 0);
                Log.d(HwConnectivityManagerImpl.LOG_TAG, "startappqoednscollection action =" + action + "timer =" + timer);
                if (action == 1) {
                    HwConnectivityManagerImpl.this.mDataServiceQoeDnsMonitor.stopDnsMonitor();
                }
                if (action == 0) {
                    int unused = HwConnectivityManagerImpl.this.mDataServiceQoeDnsMonitor.mTimer = timer;
                    HwConnectivityManagerImpl.this.mDataServiceQoeDnsMonitor.startDnsMonitor();
                }
            } else {
                Log.e(HwConnectivityManagerImpl.LOG_TAG, "appqoe_call back type null");
            }
        }
    };
    /* access modifiers changed from: private */
    public NetworkInfo mNetworkInfoMobile = null;
    /* access modifiers changed from: private */
    public NetworkInfo mNetworkInfoWlan = null;
    private boolean mSendDnsFailFlag = false;
    private boolean mSendDnsOver2000Flag = false;
    private NetworkSlicesHandler mSlicesHandler;
    private final DeviceVpnManager mVpnManager = new DeviceVpnManager();
    private DnsQueryStat wifiDnsStat = new DnsQueryStat(DevSchedFeatureRT.WIFI_FEATURE);

    private class DnsQueryStat {
        /* access modifiers changed from: private */
        public String intentType = "";
        /* access modifiers changed from: private */
        public int mDnsCount = 0;
        /* access modifiers changed from: private */
        public int mDnsFailCount = 0;
        /* access modifiers changed from: private */
        public int mDnsIpv6Timeout = 0;
        /* access modifiers changed from: private */
        public int mDnsResponse1000Count = 0;
        /* access modifiers changed from: private */
        public int mDnsResponse150Count = 0;
        /* access modifiers changed from: private */
        public int mDnsResponse2000Count = 0;
        /* access modifiers changed from: private */
        public int mDnsResponse20Count = 0;
        /* access modifiers changed from: private */
        public int mDnsResponse500Count = 0;
        /* access modifiers changed from: private */
        public int mDnsResponseOver2000Count = 0;
        /* access modifiers changed from: private */
        public int mDnsResponseTotalTime = 0;
        /* access modifiers changed from: private */
        public int mDnsStatEnum = 0;
        /* access modifiers changed from: private */
        public int mHicureDnsFailCount = 0;

        static /* synthetic */ int access$1808(DnsQueryStat x0) {
            int i = x0.mDnsCount;
            x0.mDnsCount = i + 1;
            return i;
        }

        static /* synthetic */ int access$2112(DnsQueryStat x0, int x1) {
            int i = x0.mDnsResponseTotalTime + x1;
            x0.mDnsResponseTotalTime = i;
            return i;
        }

        static /* synthetic */ int access$2208(DnsQueryStat x0) {
            int i = x0.mDnsResponse20Count;
            x0.mDnsResponse20Count = i + 1;
            return i;
        }

        static /* synthetic */ int access$2308(DnsQueryStat x0) {
            int i = x0.mDnsResponse150Count;
            x0.mDnsResponse150Count = i + 1;
            return i;
        }

        static /* synthetic */ int access$2408(DnsQueryStat x0) {
            int i = x0.mDnsResponse500Count;
            x0.mDnsResponse500Count = i + 1;
            return i;
        }

        static /* synthetic */ int access$2508(DnsQueryStat x0) {
            int i = x0.mDnsResponse1000Count;
            x0.mDnsResponse1000Count = i + 1;
            return i;
        }

        static /* synthetic */ int access$2608(DnsQueryStat x0) {
            int i = x0.mDnsResponse2000Count;
            x0.mDnsResponse2000Count = i + 1;
            return i;
        }

        static /* synthetic */ int access$2708(DnsQueryStat x0) {
            int i = x0.mDnsResponseOver2000Count;
            x0.mDnsResponseOver2000Count = i + 1;
            return i;
        }

        static /* synthetic */ int access$2808(DnsQueryStat x0) {
            int i = x0.mDnsIpv6Timeout;
            x0.mDnsIpv6Timeout = i + 1;
            return i;
        }

        static /* synthetic */ int access$2908(DnsQueryStat x0) {
            int i = x0.mDnsFailCount;
            x0.mDnsFailCount = i + 1;
            return i;
        }

        static /* synthetic */ int access$3008(DnsQueryStat x0) {
            int i = x0.mHicureDnsFailCount;
            x0.mHicureDnsFailCount = i + 1;
            return i;
        }

        DnsQueryStat(String connectedType) {
            if (connectedType.equals("cell")) {
                this.intentType = HwConnectivityManagerImpl.INTENT_DS_DNS_STATISTICS;
            } else {
                this.intentType = HwConnectivityManagerImpl.INTENT_WIFI_DNS_STATISTICS;
            }
        }

        /* access modifiers changed from: private */
        public void resetAll() {
            this.mDnsCount = 0;
            this.mDnsFailCount = 0;
            this.mDnsIpv6Timeout = 0;
            this.mDnsResponse20Count = 0;
            this.mDnsResponse150Count = 0;
            this.mDnsResponse500Count = 0;
            this.mDnsResponse1000Count = 0;
            this.mDnsResponse2000Count = 0;
            this.mDnsResponseOver2000Count = 0;
            this.mDnsResponseTotalTime = 0;
        }
    }

    public ConnectivityService createHwConnectivityService(Context context, INetworkManagementService netd, INetworkStatsService statsService, INetworkPolicyManager policyManager) {
        this.mContex = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE);
        intentFilter.addAction(HW_SYSTEM_SERVER_START);
        this.mContex.registerReceiver(this.mConnectivityChangeReceiver, intentFilter, BOOT_PERMISSION, null);
        initDnsEventHandler();
        this.mDataServiceQoeDnsMonitor = new DataServiceQoeDnsMonitor();
        this.mDataServiceQoeDnsMonitor.init();
        this.mHwConnectivityService = new HwConnectivityService(context, netd, statsService, policyManager);
        if (IS_NR_SLICES_SUPPORTED) {
            this.mSlicesHandler = new NetworkSlicesHandler();
        }
        return this.mHwConnectivityService;
    }

    public static HwConnectivityManager getDefault() {
        return mInstance;
    }

    public void setPushServicePowerNormalMode() {
    }

    public boolean setPushServicePowerSaveMode(NetworkInfo networkInfo) {
        return true;
    }

    public void setTetheringProp(Tethering tetheringService, boolean tethering, boolean usb, String ifaceName) {
        Log.d(TAG, "enter setTetheringProp");
        String prop = tethering ? "true" : "false";
        try {
            TetheringConfiguration cfg = tetheringService.getTetheringConfiguration();
            if (usb) {
                SystemProperties.set(PROPERTY_USBTETHERING_ON, prop);
                String str = TAG;
                Log.d(str, "set PROPERTY_USBTETHERING_ON: " + prop);
            } else if (cfg != null && cfg.isWifi(ifaceName)) {
                SystemProperties.set(PROPERTY_WIFIHOTSPOT_ON, prop);
                String str2 = TAG;
                Log.d(str2, "set iswifihotspoton = " + prop);
            } else if (cfg != null && cfg.isBluetooth(ifaceName)) {
                SystemProperties.set(PROPERTY_BTHOTSPOT_ON, prop);
                String str3 = TAG;
                Log.d(str3, "set isbthotspoton = " + prop);
            }
        } catch (RuntimeException e) {
            String str4 = TAG;
            Log.e(str4, "when setTetheringProp ,error =" + e + "  ifaceNmae =" + ifaceName);
        }
    }

    private boolean isFromDocomo(Context context) {
        if (IS_DOCOMO && !TextUtils.isEmpty(Settings.Global.getString(context.getContentResolver(), "tether_dun_apn"))) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:43:0x016f, code lost:
        if (r9 == null) goto L_0x019d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x0171, code lost:
        r9.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x0194, code lost:
        if (0 == 0) goto L_0x019d;
     */
    public boolean checkDunExisted(Context mContext) {
        String operator;
        SystemProperties.getBoolean("ro.config.enable.gdun", false);
        if (this.mHwConnectivityService == null) {
            Log.d(TAG, "mHwConnectivityService == null ,return false");
            return false;
        }
        Log.d(TAG, "isSystemBootComplete =" + this.mHwConnectivityService.isSystemBootComplete());
        if (!this.mHwConnectivityService.isSystemBootComplete()) {
            return false;
        }
        if (isFromDocomo(mContext)) {
            return true;
        }
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        int type = TelephonyManager.getDefault().getCurrentPhoneType(subId);
        Log.d(TAG, " type:" + type + " subId = " + subId);
        if (type == 1) {
            String operator2 = TelephonyManager.getDefault().getSimOperator(subId);
            Log.d(TAG, " operator:" + operator2);
            operator = operator2;
        } else {
            operator = HwInnerTelephonyManagerImpl.getDefault().getOperatorNumeric();
        }
        if (operator != null) {
            String[] projection = {HwSecDiagnoseConstant.ANTIMAL_APK_TYPE, "proxy", "port"};
            String selection = "numeric = '" + operator + "' and carrier_enabled = 1";
            Cursor cursor = null;
            try {
                if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                    Uri uri = Uri.withAppendedPath(MSIM_TELEPHONY_CARRIERS_URI, Long.toString((long) subId));
                    cursor = mContext.getContentResolver().query(uri, projection, selection, null, null);
                    if (HWFLOW) {
                        Log.d(TAG, "Read DB '" + uri);
                    }
                } else {
                    cursor = mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI, projection, selection, null, null);
                    if (HWFLOW) {
                        Log.d(TAG, "Read DB '" + Telephony.Carriers.CONTENT_URI);
                    }
                }
                if (cursor != null && cursor.moveToFirst()) {
                    while (!cursor.getString(cursor.getColumnIndexOrThrow(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE)).contains("dun")) {
                        if (!cursor.moveToNext()) {
                        }
                    }
                    if (!INIT_PDN_WIFI || TelephonyManager.getDefault() == null || !TelephonyManager.getDefault().isNetworkRoaming()) {
                        cursor.close();
                        return true;
                    }
                    cursor.close();
                    return false;
                }
            } catch (Exception e) {
                Log.d(TAG, "Read DB '" + Telephony.Carriers.CONTENT_URI + "' failed");
            } catch (Throwable th) {
                if (0 != 0) {
                    cursor.close();
                }
                throw th;
            }
        }
        return false;
    }

    public boolean setUsbFunctionForTethering(Context context, UsbManager usbManager, boolean enable) {
        if (!HwDeliverInfo.isIOTVersion() || !SystemProperties.getBoolean("ro.config.persist_usb_tethering", false)) {
            return false;
        }
        String str = TAG;
        Log.d(str, "tethering setCurrentFunction rndis,serial " + enable);
        if (enable) {
            if (usbManager != null) {
                usbManager.setCurrentFunction("rndis,serial", false);
            }
            Settings.Secure.putInt(context.getContentResolver(), "usb_tethering_on", 1);
        } else {
            Settings.Secure.putInt(context.getContentResolver(), "usb_tethering_on", 0);
        }
        return true;
    }

    public void captivePortalCheckCompleted(Context context, boolean isCaptivePortal) {
        if (!isCaptivePortal && 1 == Settings.System.getInt(context.getContentResolver(), WIFI_AP_MANUAL_CONNECT, 0)) {
            Settings.System.putInt(context.getContentResolver(), WIFI_AP_MANUAL_CONNECT, 0);
            Log.d(LOG_TAG, "not portal ap manual connect");
        }
    }

    public void startBrowserOnClickNotification(Context context, String url) {
        Log.d(LOG_TAG, "startBrowserOnClickNotification url: " + url);
        if (url != null) {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
            intent.setFlags(272629760);
            intent.putExtra(WifiProCommonUtils.BROWSER_LAUNCH_FROM, WifiProCommonUtils.BROWSER_LAUNCHED_BY_WIFI_PORTAL);
            try {
                if (IS_CHINA) {
                    if (Utils.isPackageInstalled("com.huawei.browser", context)) {
                        intent.setClassName("com.huawei.browser", "com.huawei.browser.Main");
                    } else if (Utils.isPackageInstalled("com.android.browser", context)) {
                        intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
                    } else {
                        Log.d(LOG_TAG, "hwbrowser not exist..");
                    }
                }
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.d(LOG_TAG, "browser not exist..");
                if (intent.getComponent() != null) {
                    intent.setComponent(null);
                    try {
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e2) {
                        Log.e(LOG_TAG, "browser failed");
                    }
                }
            }
        }
    }

    public Network getNetworkForTypeWifi() {
        HwConnectivityService hwConnectivityService = this.mHwConnectivityService;
        if (hwConnectivityService != null) {
            return hwConnectivityService.getNetworkForTypeWifi();
        }
        return null;
    }

    public boolean isP2pTether(String iface) {
        if (iface == null) {
            return false;
        }
        if (iface.startsWith(P2P_TETHER_IFAC) || iface.startsWith(P2P_TETHER_IFAC_110x) || iface.startsWith(P2P_TETHER_IFAC_QCOM)) {
            return true;
        }
        return false;
    }

    public void stopP2pTether(Context context) {
        if (context != null) {
            WifiP2pManager.Channel channel = null;
            WifiP2pManager.ActionListener mWifiP2pBridgeCreateListener = new WifiP2pManager.ActionListener() {
                /* class com.android.server.HwConnectivityManagerImpl.AnonymousClass2 */

                public void onSuccess() {
                    Log.d(HwConnectivityManagerImpl.TAG, " Stop p2p tether success");
                }

                public void onFailure(int reason) {
                    String access$500 = HwConnectivityManagerImpl.TAG;
                    Log.e(access$500, " Stop p2p tether fail:" + reason);
                }
            };
            WifiP2pManager wifiP2pManager = (WifiP2pManager) context.getSystemService("wifip2p");
            if (wifiP2pManager != null) {
                channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
            }
            if (channel != null) {
                wifiP2pManager.removeGroup(channel, mWifiP2pBridgeCreateListener);
            }
        }
    }

    /* JADX INFO: finally extract failed */
    public boolean isVpnDisabled() {
        long ident = Binder.clearCallingIdentity();
        try {
            boolean allow = this.mVpnManager.isVpnDisabled((ComponentName) null);
            Binder.restoreCallingIdentity(ident);
            String str = TAG;
            Log.d(str, "isVpnDisabled and result is " + allow);
            return allow;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    /* JADX INFO: finally extract failed */
    public boolean isInsecureVpnDisabled() {
        long ident = Binder.clearCallingIdentity();
        try {
            boolean allow = this.mVpnManager.isInsecureVpnDisabled((ComponentName) null);
            Binder.restoreCallingIdentity(ident);
            String str = TAG;
            Log.d(str, "isInsecureVpnDisabled and result is " + allow);
            return allow;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private DnsQueryStat getCellNrDnsStat(int nsaState) {
        switch (nsaState) {
            case 0:
            case 6:
                return this.cellNrSaDnsStat;
            case 1:
            case 2:
            case 3:
            case 4:
                return this.cellLteDnsStat;
            case 5:
                return this.cellEndcDnsStat;
            default:
                return null;
        }
    }

    private boolean isNsaState(int state) {
        if (2 > state || state > 5) {
            return false;
        }
        return true;
    }

    private boolean isInService(int state) {
        if (state == 0) {
            return true;
        }
        return false;
    }

    private DnsQueryStat getCellDnsStat() {
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        int networkType = TelephonyManager.getDefault().getNetworkType(subId);
        ServiceState ss = TelephonyManager.getDefault().getServiceStateForSubscriber(subId);
        int nsaState = 0;
        if (ss != null) {
            nsaState = ss.getNsaState();
            if (isNsaState(nsaState) && isInService(ss.getDataRegState())) {
                networkType = ss.getConfigRadioTechnology();
            }
        }
        int unused = this.cellLteDnsStat.mDnsStatEnum = DNS_STAT_ENUM_LTE;
        int unused2 = this.cellEndcDnsStat.mDnsStatEnum = DNS_STAT_ENUM_ENDC;
        int unused3 = this.cellNrSaDnsStat.mDnsStatEnum = DNS_STAT_ENUM_NRSA;
        if (networkType == 13 || networkType == 19) {
            return this.cellLteDnsStat;
        }
        if (networkType != 20) {
            return null;
        }
        return getCellNrDnsStat(nsaState);
    }

    private DnsQueryStat getDnsStat(int netId) {
        if (netId != cellNetId) {
            return this.wifiDnsStat;
        }
        return getCellDnsStat();
    }

    private void initDnsEventHandler() {
        Looper mainLooper;
        Context context = this.mContex;
        if (context != null && (mainLooper = context.getMainLooper()) != null) {
            this.mDnsEventHandler = new DnsEventHandler(mainLooper);
        }
    }

    private class DnsEventHandler extends Handler {
        public DnsEventHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 0) {
                if (i == 1) {
                    Bundle bundleData = msg.getData();
                    if (bundleData == null) {
                        Log.e(HwConnectivityManagerImpl.LOG_TAG, "set ip tables data is null");
                        return;
                    }
                    String hostName = bundleData.getString(HwConnectivityManagerImpl.SET_IP_TABLES_KEY_HOST_NAME);
                    List<String> ipAddresses = new ArrayList<>();
                    try {
                        ipAddresses = bundleData.getStringArrayList(HwConnectivityManagerImpl.SET_IP_TABLES_KEY_IP_LIST);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Log.e(HwConnectivityManagerImpl.TAG, "ArrayIndexOutOfBoundsException");
                    }
                    HwConnectivityManagerImpl.this.setIpRulesOfDnsEventAction(hostName, ipAddresses, bundleData.getInt(HwConnectivityManagerImpl.SET_IP_TABLES_KEY_IP_COUNT));
                }
            } else if (msg.obj instanceof Context) {
                Context context = (Context) msg.obj;
                Bundle data = msg.getData();
                if (data == null) {
                    Log.e(HwConnectivityManagerImpl.LOG_TAG, "DNS event data is null");
                    return;
                }
                int returnCode = data.getInt(HwConnectivityManagerImpl.DNS_EVENT_KEY_RETURNCODE);
                int latencyMs = data.getInt(HwConnectivityManagerImpl.DNS_EVENT_KEY_LANENCY);
                int netId = data.getInt(HwConnectivityManagerImpl.DNS_EVENT_KEY_NETID);
                NetdEventListenerService.updateUidDnsFailCount(data.getInt("uid"));
                HwConnectivityManagerImpl.this.onDnsEventForNrSlice(returnCode, data);
                HwConnectivityManagerImpl.this.onDnsEventAction(context, returnCode, latencyMs, netId);
            }
        }
    }

    /* access modifiers changed from: private */
    public void onDnsEventForNrSlice(int returnCode, Bundle data) {
        NetworkSlicesHandler networkSlicesHandler;
        if (IS_NR_SLICES_SUPPORTED && returnCode == 0 && (networkSlicesHandler = this.mSlicesHandler) != null) {
            Message msg = networkSlicesHandler.obtainMessage(1);
            msg.setData(data);
            msg.sendToTarget();
        }
    }

    public void onDnsEvent(Context context, Bundle bundle) {
        Handler handler = this.mDnsEventHandler;
        if (handler != null) {
            Message setIpTablesMsg = handler.obtainMessage(1);
            setIpTablesMsg.setData(bundle);
            this.mDnsEventHandler.sendMessageAtFrontOfQueue(setIpTablesMsg);
            Message onDnsEventmsg = this.mDnsEventHandler.obtainMessage(0, context);
            onDnsEventmsg.setData(bundle);
            this.mDnsEventHandler.sendMessage(onDnsEventmsg);
        }
    }

    /* access modifiers changed from: private */
    public void onDnsEventAction(Context context, int returnCode, int latencyMs, int netId) {
        DnsQueryStat mDnsStat;
        if ((netId == wifiNetId || netId == cellNetId) && (mDnsStat = getDnsStat(netId)) != null) {
            if (returnCode == 0) {
                onDnsReportSuccProcess(context, mDnsStat, latencyMs, netId);
            } else {
                onDnsReportFailProcess(context, mDnsStat, returnCode, latencyMs, netId);
            }
            DnsQueryStat.access$1808(mDnsStat);
            if (mDnsStat.mDnsCount == 100) {
                sendIntentDnsEvent(context, netId, mDnsStat);
            }
            HwConnectivityService hwConnectivityService = this.mHwConnectivityService;
            if (hwConnectivityService != null) {
                hwConnectivityService.recordPrivateDnsEvent(context, returnCode, latencyMs, netId);
            }
        }
    }

    /* access modifiers changed from: private */
    public void setIpRulesOfDnsEventAction(String hostName, List<String> ipAddresses, int ipAddressesCount) {
        int networkPolicyFlag;
        if (ipAddresses != null && !ipAddresses.isEmpty() && (networkPolicyFlag = getNetworkPolicyFlag()) != -1) {
            if (networkPolicyFlag == 1 || networkPolicyFlag == 0) {
                setWhiteRulesToIptables(hostName, ipAddresses, networkPolicyFlag);
            } else if (networkPolicyFlag == 2 || networkPolicyFlag == 3) {
                setBlackRulesToIptables(hostName, ipAddresses, networkPolicyFlag);
            } else {
                Log.d(LOG_TAG, "hasNetworkPolicyList error");
            }
        }
    }

    public void clearIpCacheOfDnsEvent() {
        sNetworkListCacheOfDns.clear();
    }

    private void onDnsReportSuccProcess(Context context, DnsQueryStat dnsStat, int latencyMs, int netId) {
        DataServiceQoeDnsMonitor dataServiceQoeDnsMonitor = this.mDataServiceQoeDnsMonitor;
        if (dataServiceQoeDnsMonitor != null) {
            DataServiceQoeDnsMonitor.access$1908(dataServiceQoeDnsMonitor);
            DataServiceQoeDnsMonitor.access$2012(this.mDataServiceQoeDnsMonitor, latencyMs);
            DnsQueryStat.access$2112(dnsStat, latencyMs);
            if (latencyMs > 8 && netId == cellNetId) {
                if (!this.mSendDnsOver2000Flag && this.mDnsOver2000Q.size() > 0) {
                    this.mDnsOver2000Q.clear();
                }
                if (!this.mSendDnsFailFlag && this.mDnsFailQ.size() > 0) {
                    this.mDnsFailQ.clear();
                }
            }
            if (latencyMs > 2000) {
                DnsQueryStat.access$2708(dnsStat);
                sendIntentPsSlowDnsOver2000(context, latencyMs, netId);
            } else if (latencyMs <= 20) {
                DnsQueryStat.access$2208(dnsStat);
            } else if (latencyMs <= 150) {
                DnsQueryStat.access$2308(dnsStat);
            } else if (latencyMs <= 500) {
                DnsQueryStat.access$2408(dnsStat);
            } else if (latencyMs <= 1000) {
                DnsQueryStat.access$2508(dnsStat);
            } else {
                DnsQueryStat.access$2608(dnsStat);
            }
        }
    }

    private void onDnsReportFailProcess(Context context, DnsQueryStat dnsStat, int returnCode, int latencyMs, int netId) {
        if (this.mDataServiceQoeDnsMonitor != null) {
            if (returnCode == 15) {
                DnsQueryStat.access$2808(dnsStat);
            }
            DnsQueryStat.access$2908(dnsStat);
            DnsQueryStat.access$3008(dnsStat);
            DataServiceQoeDnsMonitor.access$3108(this.mDataServiceQoeDnsMonitor);
            sendIntentPsSlowDnsFail(context, returnCode, latencyMs, netId);
            if (netId == cellNetId) {
                SystemProperties.set("hw.hicure.dns_fail_count", "" + dnsStat.mHicureDnsFailCount);
                return;
            }
            SystemProperties.set("hw.wifipro.dns_fail_count", "" + dnsStat.mDnsFailCount);
        }
    }

    public boolean isBypassPrivateDns(int netId) {
        HwConnectivityService hwConnectivityService = this.mHwConnectivityService;
        if (hwConnectivityService != null) {
            return hwConnectivityService.isBypassPrivateDns(netId);
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void clearInvalidPrivateDnsNetworkInfo() {
        HwConnectivityService hwConnectivityService = this.mHwConnectivityService;
        if (hwConnectivityService != null) {
            hwConnectivityService.clearInvalidPrivateDnsNetworkInfo();
        }
    }

    private void sendIntentPsSlowDnsFail(Context context, int returnCode, int latencyMs, int netId) {
        if (netId == cellNetId) {
            Date now = new Date();
            Log.d(LOG_TAG, " sendIntentPsSlowDnsFail mSendDnsFailFlag = " + this.mSendDnsFailFlag + "mDnsFailQ.size() = " + this.mDnsFailQ.size());
            if (!this.mSendDnsFailFlag) {
                this.mDnsFailQ.addLast(now);
                if (this.mDnsFailQ.size() == 6) {
                    if (now.getTime() - this.mDnsFailQ.getFirst().getTime() <= 45000) {
                        this.mSendDnsFailFlag = true;
                        if (this.mConnectedType == 2) {
                            Intent chrIntent = new Intent(INTENT_DS_WEB_STAT_REPORT);
                            chrIntent.putExtra("ReportType", 5);
                            chrIntent.putExtra("WebFailCode", returnCode);
                            chrIntent.putExtra("WebDelay", latencyMs);
                            context.sendBroadcast(chrIntent, CHR_BROADCAST_PERMISSION);
                            Log.d(LOG_TAG, " sendIntentPsSlowDnsFail");
                        }
                    }
                    this.mDnsFailQ.removeFirst();
                }
            } else if (this.mDnsFailQ.isEmpty() || now.getTime() - this.mDnsFailQ.getLast().getTime() > 600000) {
                Log.d(LOG_TAG, " sendIntentPsSlowDnsFail reset mSendDnsFailFlag");
                this.mSendDnsFailFlag = false;
                this.mDnsFailQ.clear();
                this.mDnsFailQ.addLast(now);
            }
        }
    }

    private void sendIntentPsSlowDnsOver2000(Context context, int delay, int netId) {
        if (netId == cellNetId) {
            Date now = new Date();
            Log.d(LOG_TAG, " sendIntentPsSlowDnsOver2000 mSendDnsOver2000Flag = " + this.mSendDnsOver2000Flag + "mDnsOver2000Q.size() = " + this.mDnsOver2000Q.size());
            if (!this.mSendDnsOver2000Flag) {
                this.mDnsOver2000Q.addLast(now);
                if (this.mDnsOver2000Q.size() == 6) {
                    if (now.getTime() - this.mDnsOver2000Q.getFirst().getTime() <= 45000) {
                        this.mSendDnsOver2000Flag = true;
                        if (this.mConnectedType == 2) {
                            Intent chrIntent = new Intent(INTENT_DS_WEB_STAT_REPORT);
                            chrIntent.putExtra("ReportType", 6);
                            chrIntent.putExtra("WebDelay", delay);
                            context.sendBroadcast(chrIntent, CHR_BROADCAST_PERMISSION);
                            Log.d(LOG_TAG, " sendIntentPsSlowDnsOver2000");
                        }
                    }
                    this.mDnsOver2000Q.removeFirst();
                }
            } else if (this.mDnsOver2000Q.isEmpty() || now.getTime() - this.mDnsOver2000Q.getLast().getTime() > 600000) {
                Log.d(LOG_TAG, " sendIntentPsSlowDnsOver2000 reset mSendDnsOver2000Flag");
                this.mSendDnsOver2000Flag = false;
                this.mDnsOver2000Q.clear();
                this.mDnsOver2000Q.addLast(now);
            }
        }
    }

    private void sendIntentDnsEvent(Context context, int netId, DnsQueryStat resDnsStat) {
        Log.d(LOG_TAG, "sendIntentDnsEvent connectType = " + this.mConnectedType);
        Log.d(LOG_TAG, "sendIntentDnsEvent mDnsCount:" + resDnsStat.mDnsCount + "mDnsIpv6Timeout:" + resDnsStat.mDnsIpv6Timeout + "mDnsResponseTotalTime:" + resDnsStat.mDnsResponseTotalTime + "mDnsFailCount:" + resDnsStat.mDnsFailCount + "mDnsResponse20Count:" + resDnsStat.mDnsResponse20Count + "mDnsResponse150Count:" + resDnsStat.mDnsResponse150Count + "mDnsResponse500Count:" + resDnsStat.mDnsResponse500Count + "mDnsResponse1000Count:" + resDnsStat.mDnsResponse1000Count + "mDnsResponse2000Count:" + resDnsStat.mDnsResponse2000Count + "mDnsResponseOver2000Count:" + resDnsStat.mDnsResponseOver2000Count);
        Bundle extras = new Bundle();
        extras.putInt("dnsCount", resDnsStat.mDnsCount);
        extras.putInt("dnsIpv6Timeout", resDnsStat.mDnsIpv6Timeout);
        extras.putInt("dnsResponseTotalTime", resDnsStat.mDnsResponseTotalTime);
        extras.putInt("dnsFailCount", resDnsStat.mDnsFailCount);
        extras.putInt("dnsResponse20Count", resDnsStat.mDnsResponse20Count);
        extras.putInt("dnsResponse150Count", resDnsStat.mDnsResponse150Count);
        extras.putInt("dnsResponse500Count", resDnsStat.mDnsResponse500Count);
        extras.putInt("dnsResponse1000Count", resDnsStat.mDnsResponse1000Count);
        extras.putInt("dnsResponse2000Count", resDnsStat.mDnsResponse2000Count);
        extras.putInt("dnsResponseOver2000Count", resDnsStat.mDnsResponseOver2000Count);
        extras.putInt("dnsStatEnum", resDnsStat.mDnsStatEnum);
        Intent intent = new Intent(resDnsStat.intentType);
        intent.putExtras(extras);
        resDnsStat.resetAll();
        context.sendBroadcast(intent, CHR_BROADCAST_PERMISSION);
    }

    public boolean needCaptivePortalCheck(NetworkAgentInfo nai, Context context) {
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (!SubscriptionManager.isUsableSubIdValue(subId) || context == null || TelephonyManager.getDefault() == null) {
            Log.e(LOG_TAG, "needCaptivePortal: subId =" + subId + " is Invalid, or context is null,return false.");
            return false;
        }
        int deviceProvisioned = Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0);
        if (deviceProvisioned != 0) {
            Log.e(LOG_TAG, "needCaptivePortal: deviceProvisioned=" + deviceProvisioned + ", return false.");
            return false;
        } else if (nai == null || nai.networkInfo == null || nai.networkInfo.getType() != 0) {
            Log.e(LOG_TAG, "needCaptivePortal: NetworkAgentInfo is not Mobile Type,return false.");
            return false;
        } else {
            String simOperator = TelephonyManager.getDefault().getSimOperator(subId);
            if (TextUtils.isEmpty(simOperator)) {
                return false;
            }
            String plmnConfig = Settings.System.getString(context.getContentResolver(), "need_captive_portal_by_hplmn");
            if (TextUtils.isEmpty(plmnConfig)) {
                return false;
            }
            for (String plmn : plmnConfig.split(",")) {
                if (simOperator.equals(plmn)) {
                    Log.d(LOG_TAG, "needCaptivePortalCheck return true for simOperator=" + simOperator);
                    return true;
                }
            }
            return false;
        }
    }

    public void informModemTetherStatusToChangeGRO(int enable, String faceName) {
        HwTelephonyManager.getDefault().informModemTetherStatusToChangeGRO(enable, faceName);
    }

    public boolean isApIpv4AddressFixed() {
        Log.d(LOG_TAG, "Get isApIpv4AddressFixed");
        IConnectivityExManager sService = IConnectivityExManager.Stub.asInterface(ServiceManager.getService("hwConnectivityExService"));
        if (sService == null) {
            return false;
        }
        try {
            return sService.isApIpv4AddressFixed();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "RemoteException.");
            return false;
        }
    }

    public void setApIpv4AddressFixed(boolean isFixed) {
        Log.d(LOG_TAG, "setApIpv4AddressFixed: " + isFixed);
        IConnectivityExManager sService = IConnectivityExManager.Stub.asInterface(ServiceManager.getService("hwConnectivityExService"));
        if (sService != null) {
            try {
                sService.setApIpv4AddressFixed(isFixed);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "RemoteException.");
            }
        }
    }

    /* access modifiers changed from: private */
    public class DataServiceQoeDnsMonitor {
        private static final int EVENT_DNS_MONITOR_TIMER = 1;
        private static final int HWCM_DNS_INFO = 801;
        private int mDnsFailCount;
        private int mDnsLatencyMs;
        private int mDnsSucceedCount;
        private MyHandler mHandler;
        private HandlerThread mHandlerThread;
        /* access modifiers changed from: private */
        public int mTimer;

        static /* synthetic */ int access$1908(DataServiceQoeDnsMonitor x0) {
            int i = x0.mDnsSucceedCount;
            x0.mDnsSucceedCount = i + 1;
            return i;
        }

        static /* synthetic */ int access$2012(DataServiceQoeDnsMonitor x0, int x1) {
            int i = x0.mDnsLatencyMs + x1;
            x0.mDnsLatencyMs = i;
            return i;
        }

        static /* synthetic */ int access$3108(DataServiceQoeDnsMonitor x0) {
            int i = x0.mDnsFailCount;
            x0.mDnsFailCount = i + 1;
            return i;
        }

        private DataServiceQoeDnsMonitor() {
            this.mHandler = null;
            this.mHandlerThread = null;
            this.mTimer = 0;
            this.mDnsSucceedCount = 0;
            this.mDnsFailCount = 0;
            this.mDnsLatencyMs = 0;
        }

        public void init() {
            this.mHandlerThread = new HandlerThread("DataServiceQoeDnsMonitorTh");
            this.mHandlerThread.start();
            this.mHandler = new MyHandler(this.mHandlerThread.getLooper());
        }

        private class MyHandler extends Handler {
            public MyHandler(Looper looper) {
                super(looper);
            }

            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    DataServiceQoeDnsMonitor.this.reportBoosterDnsPara();
                    DataServiceQoeDnsMonitor.this.resetDnsMonitorTimer();
                }
            }
        }

        /* access modifiers changed from: private */
        public void reportBoosterDnsPara() {
            if (HwConnectivityManagerImpl.this.mHwCommBoosterServiceManager == null) {
                Log.e(HwConnectivityManagerImpl.LOG_TAG, "reportBoosterDnsPara:mHwCommBoosterServiceManager is null");
                return;
            }
            Bundle data = new Bundle();
            data.putInt("mDnsFailCount", this.mDnsFailCount);
            data.putInt("mDnsSucceedCount", this.mDnsSucceedCount);
            data.putInt("mDnsLatencyMs", this.mDnsLatencyMs);
            int ret = HwConnectivityManagerImpl.this.mHwCommBoosterServiceManager.reportBoosterPara(HwConnectivityManagerImpl.VALID_PKGNAME_DNS, 801, data);
            this.mDnsFailCount = 0;
            this.mDnsLatencyMs = 0;
            this.mDnsSucceedCount = 0;
            if (ret == -1) {
                Log.d(HwConnectivityManagerImpl.LOG_TAG, "reportBoosterDnsPara fail");
            }
        }

        /* access modifiers changed from: private */
        public void startDnsMonitor() {
            if (this.mHandler.hasMessages(1)) {
                this.mHandler.removeMessages(1);
                Log.e(HwConnectivityManagerImpl.LOG_TAG, "startDnsMonitor: message exist");
            }
            this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 1), (long) this.mTimer);
        }

        /* access modifiers changed from: private */
        public void stopDnsMonitor() {
            if (this.mHandler.hasMessages(1)) {
                this.mHandler.removeMessages(1);
            }
        }

        /* access modifiers changed from: private */
        public void resetDnsMonitorTimer() {
            if (this.mTimer < 0) {
                Log.e(HwConnectivityManagerImpl.LOG_TAG, "resetDnsMonitorTimer error");
                return;
            }
            this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 1), (long) this.mTimer);
        }
    }

    /* access modifiers changed from: private */
    public void initCommBoosterManager() {
        this.mHwCommBoosterServiceManager = HwFrameworkFactory.getHwCommBoosterServiceManager();
        IHwCommBoosterServiceManager iHwCommBoosterServiceManager = this.mHwCommBoosterServiceManager;
        if (iHwCommBoosterServiceManager == null) {
            Log.e(LOG_TAG, "registerBoosterCallback:getHwCommBoosterServiceManager fail");
        } else if (iHwCommBoosterServiceManager.registerCallBack(VALID_PKGNAME_DNS, this.mIHwCommBoosterCallback) != 0) {
            Log.e(LOG_TAG, "registerBoosterCallback:registerBoosterCallback fail");
            this.mHwCommBoosterServiceManager = null;
        } else {
            Log.d(LOG_TAG, "initCommBoosterManager completed");
        }
    }

    private List<String> deleteSettedIpList(List<String> ipAddressesList, List<String> settedIpList) {
        if (settedIpList.isEmpty()) {
            return ipAddressesList;
        }
        for (String settedIp : settedIpList) {
            ipAddressesList.remove(settedIp);
        }
        return ipAddressesList;
    }

    private void setOrAddNetworkAccessList(List<String> addrList, int whiteOrBlack, int setOrAdd) {
        Log.d(LOG_TAG, "setOrAddWhiteRulesToIptables ");
        IBinder binder = ServiceManager.getService("network_management");
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        if (binder != null) {
            try {
                data.writeInterfaceToken(DESCRIPTOR_NETWORKMANAGEMENT_SERVICE);
                data.writeStringList(addrList);
                data.writeInt(whiteOrBlack);
                data.writeInt(setOrAdd);
                binder.transact(1106, data, reply, 0);
                reply.readException();
            } catch (RemoteException localRemoteException) {
                Log.e(TAG, "operate NetworkAccessList error", localRemoteException);
            } catch (Throwable th) {
                reply.recycle();
                data.recycle();
                throw th;
            }
        }
        reply.recycle();
        data.recycle();
    }

    private boolean isHostNameMatchesPolicyList(String hostName, List<String> hostNamePolicyList) {
        if (hostNamePolicyList == null || hostNamePolicyList.isEmpty()) {
            return false;
        }
        for (String hostNamePolicy : hostNamePolicyList) {
            if (isHostnameMatches(hostName, hostNamePolicy)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHostnameMatches(String hostName, String hostNamePolicy) {
        int index;
        if (TextUtils.isEmpty(hostName) || TextUtils.isEmpty(hostNamePolicy) || hostNamePolicy.length() > hostName.length() || (index = hostName.indexOf(hostNamePolicy)) != hostName.length() - hostNamePolicy.length() || (index != 0 && hostName.charAt(index - 1) != '.')) {
            return false;
        }
        return true;
    }

    private void setWhiteRulesToIptables(String hostName, List<String> ipAddressesList, int networkPolicyFlag) {
        Log.d(LOG_TAG, "setWhiteRulesToIptables ");
        if (networkPolicyFlag == 1 && isHostNameMatchesPolicyList(hostName, HwDeviceManager.getList(64))) {
            deleteSettedIpList(ipAddressesList, sNetworkListCacheOfDns);
            if (!ipAddressesList.isEmpty()) {
                setOrAddNetworkAccessList(ipAddressesList, 0, 0);
                sNetworkListCacheOfDns.addAll(ipAddressesList);
            }
        }
    }

    private void setBlackRulesToIptables(String hostName, List<String> ipAddressesList, int networkPolicyFlag) {
        Log.d(LOG_TAG, "setBlackRulesToIptables ");
        if (networkPolicyFlag == 3 && isHostNameMatchesPolicyList(hostName, HwDeviceManager.getList(65))) {
            List<String> blackIpPolicyList = HwDeviceManager.getList(63);
            if ((blackIpPolicyList == null || blackIpPolicyList.isEmpty()) && sNetworkListCacheOfDns.isEmpty()) {
                setOrAddNetworkAccessList(ipAddressesList, 1, 1);
                sNetworkListCacheOfDns.addAll(ipAddressesList);
                return;
            }
            deleteSettedIpList(ipAddressesList, sNetworkListCacheOfDns);
            if (!ipAddressesList.isEmpty()) {
                setOrAddNetworkAccessList(ipAddressesList, 1, 0);
                sNetworkListCacheOfDns.addAll(ipAddressesList);
            }
        }
    }

    private int getNetworkPolicyFlag() {
        String flagStr = Settings.System.getString(this.mContex.getContentResolver(), KEY_NETWORK_POLICY_FLAG);
        if (TextUtils.isEmpty(flagStr)) {
            Log.d(LOG_TAG, "getNetworkPolicyFlag flagStr null");
            return -1;
        }
        try {
            return Integer.parseInt(flagStr, 2);
        } catch (NumberFormatException e) {
            Log.e(LOG_TAG, "getNetworkPolicyFlag parseInt error flagStr = " + flagStr);
            return -1;
        }
    }
}
