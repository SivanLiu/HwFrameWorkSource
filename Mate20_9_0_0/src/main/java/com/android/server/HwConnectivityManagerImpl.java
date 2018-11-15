package com.android.server;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.provider.Telephony.Carriers;
import android.telephony.HwInnerTelephonyManagerImpl;
import android.telephony.HwTelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.connectivity.NetworkMonitor;
import com.android.server.connectivity.Tethering;
import com.android.server.connectivity.tethering.TetheringConfiguration;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.android.app.admin.DeviceVpnManager;
import com.huawei.deliver.info.HwDeliverInfo;
import huawei.android.net.IConnectivityExManager;
import huawei.android.net.IConnectivityExManager.Stub;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

public class HwConnectivityManagerImpl implements HwConnectivityManager {
    private static final String CHR_BROADCAST_PERMISSION = "com.huawei.android.permission.GET_CHR_DATA";
    private static final String COUNTRY_CODE_CN = "460";
    public static final int CURRENT_CONNECT_TO_CELLULAR = 2;
    public static final int CURRENT_CONNECT_TO_WLAN = 1;
    private static final boolean DBG = true;
    private static final String DISABLE_VPN = "disable-vpn";
    private static final int DNS_BIG_LATENCY = 2000;
    private static final int DNS_ERROR_IPV6_TIMEOUT = 15;
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
    private static final int DNS_SUCCESS = 0;
    private static final int DNS_TIME_MIN = 10;
    protected static final boolean HWFLOW;
    private static final boolean INIT_PDN_WIFI = SystemProperties.getBoolean("ro.config.forbid_roam_dun_wifi", false);
    private static final String INTENT_DS_DNS_STATISTICS = "com.intent.action.dns_statistics";
    private static final String INTENT_DS_WEB_STAT_REPORT = "com.android.intent.action.web_stat_report";
    private static final String INTENT_WIFI_DNS_STATISTICS = "com.intent.action.wifi_dns_statistics";
    private static final boolean IS_CHINA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final boolean IS_DOCOMO;
    private static final String LOG_TAG = "HwConnectivityManagerImpl";
    static final Uri MSIM_TELEPHONY_CARRIERS_URI = Uri.parse("content://telephony/carriers/subId");
    public static final int NOT_CONNECT_TO_NETWORK = 0;
    private static final String P2P_TETHER_IFAC = "p2p-wlan0-";
    private static final String P2P_TETHER_IFAC_110x = "p2p-p2p0-";
    private static final String P2P_TETHER_IFAC_QCOM = "p2p0";
    protected static final String PROPERTY_BTHOTSPOT_ON = "sys.isbthotspoton";
    protected static final String PROPERTY_USBTETHERING_ON = "sys.isusbtetheringon";
    protected static final String PROPERTY_WIFIHOTSPOT_ON = "sys.iswifihotspoton";
    private static final int PS_AP_SLOW_DNS_BIG_LATENCY = 6;
    private static final int PS_AP_SLOW_DNS_FAIL = 5;
    private static final String SECURE_VPN = "secure-vpn";
    private static final String TAG = null;
    private static final boolean VDBG = false;
    private static final String WIFI_AP_MANUAL_CONNECT = "wifi_ap_manual_connect";
    private static HwConnectivityManager mInstance = new HwConnectivityManagerImpl();
    private ConnectivityManager mConnMgr = null;
    private int mConnectedType = 0;
    private final BroadcastReceiver mConnectivityChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null && "android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                    HwConnectivityManagerImpl.this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
                    if (HwConnectivityManagerImpl.this.mConnMgr != null) {
                        HwConnectivityManagerImpl.this.mNetworkInfoWlan = HwConnectivityManagerImpl.this.mConnMgr.getNetworkInfo(1);
                        HwConnectivityManagerImpl.this.mNetworkInfoMobile = HwConnectivityManagerImpl.this.mConnMgr.getNetworkInfo(0);
                        if (!(HwConnectivityManagerImpl.this.mNetworkInfoWlan == null || HwConnectivityManagerImpl.this.mNetworkInfoMobile == null)) {
                            if (HwConnectivityManagerImpl.this.mNetworkInfoWlan.isConnected()) {
                                HwConnectivityManagerImpl.this.mConnectedType = 1;
                                HwConnectivityManagerImpl.this.sendIntentDnsEvent(context, HwConnectivityManagerImpl.this.mLastConnectedType);
                                HwConnectivityManagerImpl.this.mLastConnectedType = HwConnectivityManagerImpl.this.mConnectedType;
                            } else if (HwConnectivityManagerImpl.this.mNetworkInfoMobile.isConnected()) {
                                HwConnectivityManagerImpl.this.mConnectedType = 2;
                                HwConnectivityManagerImpl.this.sendIntentDnsEvent(context, HwConnectivityManagerImpl.this.mLastConnectedType);
                                HwConnectivityManagerImpl.this.mLastConnectedType = HwConnectivityManagerImpl.this.mConnectedType;
                            } else {
                                HwConnectivityManagerImpl.this.mConnectedType = 0;
                                HwConnectivityManagerImpl.this.sendIntentDnsEvent(context, HwConnectivityManagerImpl.this.mLastConnectedType);
                                HwConnectivityManagerImpl.this.mLastConnectedType = HwConnectivityManagerImpl.this.mConnectedType;
                            }
                        }
                        HwConnectivityManagerImpl.this.clearInvalidPrivateDnsNetworkInfo();
                    }
                }
            }
        }
    };
    private Context mContex = null;
    private int mDnsCount = 0;
    private int mDnsFailCount = 0;
    private LinkedList<Date> mDnsFailQ = new LinkedList();
    private int mDnsIpv6Timeout = 0;
    private LinkedList<Date> mDnsOver2000Q = new LinkedList();
    private int mDnsResponse1000Count = 0;
    private int mDnsResponse150Count = 0;
    private int mDnsResponse2000Count = 0;
    private int mDnsResponse20Count = 0;
    private int mDnsResponse500Count = 0;
    private int mDnsResponseOver2000Count = 0;
    private int mDnsResponseTotalTime = 0;
    private HwConnectivityService mHwConnectivityService = null;
    private int mLastConnectedType = 0;
    private NetworkInfo mNetworkInfoMobile = null;
    private NetworkInfo mNetworkInfoWlan = null;
    private boolean mSendDnsFailFlag = false;
    private boolean mSendDnsOver2000Flag = false;
    private final DeviceVpnManager mVpnManager = new DeviceVpnManager();

    static {
        boolean z = true;
        boolean z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z2;
        if (!(SystemProperties.get("ro.config.hw_opta", "").equals("341") && SystemProperties.get("ro.config.hw_optb", "").equals("392"))) {
            z = false;
        }
        IS_DOCOMO = z;
    }

    public ConnectivityService createHwConnectivityService(Context context, INetworkManagementService netd, INetworkStatsService statsService, INetworkPolicyManager policyManager) {
        this.mContex = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContex.registerReceiver(this.mConnectivityChangeReceiver, intentFilter);
        this.mHwConnectivityService = new HwConnectivityService(context, netd, statsService, policyManager);
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
        String str;
        StringBuilder stringBuilder;
        try {
            TetheringConfiguration cfg = tetheringService.getTetheringConfiguration();
            if (usb) {
                SystemProperties.set(PROPERTY_USBTETHERING_ON, prop);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("set PROPERTY_USBTETHERING_ON: ");
                stringBuilder.append(prop);
                Log.d(str, stringBuilder.toString());
            } else if (cfg != null && cfg.isWifi(ifaceName)) {
                SystemProperties.set(PROPERTY_WIFIHOTSPOT_ON, prop);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("set iswifihotspoton = ");
                stringBuilder.append(prop);
                Log.d(str, stringBuilder.toString());
            } else if (cfg != null && cfg.isBluetooth(ifaceName)) {
                SystemProperties.set(PROPERTY_BTHOTSPOT_ON, prop);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("set isbthotspoton = ");
                stringBuilder.append(prop);
                Log.d(str, stringBuilder.toString());
            }
        } catch (RuntimeException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("when setTetheringProp ,error =");
            stringBuilder.append(e);
            stringBuilder.append("  ifaceNmae =");
            stringBuilder.append(ifaceName);
            Log.e(str, stringBuilder.toString());
        }
    }

    private boolean isFromDocomo(Context context) {
        if (IS_DOCOMO && !TextUtils.isEmpty(Global.getString(context.getContentResolver(), "tether_dun_apn"))) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:46:0x0169, code:
            if (r8 == null) goto L_0x019b;
     */
    /* JADX WARNING: Missing block: B:47:0x016b, code:
            r8.close();
     */
    /* JADX WARNING: Missing block: B:52:0x0192, code:
            if (r8 == null) goto L_0x019b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean checkDunExisted(Context mContext) {
        SystemProperties.getBoolean("ro.config.enable.gdun", false);
        if (this.mHwConnectivityService == null) {
            Log.d(TAG, "mHwConnectivityService == null ,return false");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSystemBootComplete =");
        stringBuilder.append(this.mHwConnectivityService.isSystemBootComplete());
        Log.d(str, stringBuilder.toString());
        if (!this.mHwConnectivityService.isSystemBootComplete()) {
            return false;
        }
        if (isFromDocomo(mContext)) {
            return true;
        }
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        int type = TelephonyManager.getDefault().getCurrentPhoneType(subId);
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" type:");
        stringBuilder2.append(type);
        stringBuilder2.append(" subId = ");
        stringBuilder2.append(subId);
        Log.d(str2, stringBuilder2.toString());
        if (type == 1) {
            str = TelephonyManager.getDefault().getSimOperator(subId);
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" operator:");
            stringBuilder2.append(str);
            Log.d(str2, stringBuilder2.toString());
        } else {
            str = HwInnerTelephonyManagerImpl.getDefault().getOperatorNumeric();
        }
        str2 = str;
        if (str2 != null) {
            String[] projection = new String[]{HwSecDiagnoseConstant.ANTIMAL_APK_TYPE, "proxy", "port"};
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("numeric = '");
            stringBuilder3.append(str2);
            stringBuilder3.append("' and carrier_enabled = 1");
            String selection = stringBuilder3.toString();
            Cursor cursor = null;
            StringBuilder stringBuilder4;
            try {
                if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                    Uri uri = Uri.withAppendedPath(MSIM_TELEPHONY_CARRIERS_URI, Long.toString((long) subId));
                    cursor = mContext.getContentResolver().query(uri, projection, selection, null, null);
                    if (HWFLOW) {
                        String str3 = TAG;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Read DB '");
                        stringBuilder5.append(uri);
                        Log.d(str3, stringBuilder5.toString());
                    }
                } else {
                    cursor = mContext.getContentResolver().query(Carriers.CONTENT_URI, projection, selection, null, null);
                    if (HWFLOW) {
                        str = TAG;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Read DB '");
                        stringBuilder4.append(Carriers.CONTENT_URI);
                        Log.d(str, stringBuilder4.toString());
                    }
                }
                if (cursor != null && cursor.moveToFirst()) {
                    while (!cursor.getString(cursor.getColumnIndexOrThrow(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE)).contains("dun")) {
                        if (!cursor.moveToNext()) {
                        }
                    }
                    if (INIT_PDN_WIFI && TelephonyManager.getDefault() != null && TelephonyManager.getDefault().isNetworkRoaming()) {
                        if (cursor != null) {
                            cursor.close();
                        }
                        return false;
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    return true;
                }
            } catch (Exception e) {
                String str4 = TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Read DB '");
                stringBuilder4.append(Carriers.CONTENT_URI);
                stringBuilder4.append("' failed: ");
                stringBuilder4.append(e);
                Log.d(str4, stringBuilder4.toString());
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return false;
    }

    public boolean setUsbFunctionForTethering(Context context, UsbManager usbManager, boolean enable) {
        if (!HwDeliverInfo.isIOTVersion() || !SystemProperties.getBoolean("ro.config.persist_usb_tethering", false)) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("tethering setCurrentFunction rndis,serial ");
        stringBuilder.append(enable);
        Log.d(str, stringBuilder.toString());
        if (enable) {
            if (usbManager != null) {
                usbManager.setCurrentFunction("rndis,serial", false);
            }
            Secure.putInt(context.getContentResolver(), "usb_tethering_on", 1);
        } else {
            Secure.putInt(context.getContentResolver(), "usb_tethering_on", 0);
        }
        return true;
    }

    public void captivePortalCheckCompleted(Context context, boolean isCaptivePortal) {
        if (!isCaptivePortal && 1 == System.getInt(context.getContentResolver(), WIFI_AP_MANUAL_CONNECT, 0)) {
            System.putInt(context.getContentResolver(), WIFI_AP_MANUAL_CONNECT, 0);
            Log.d(LOG_TAG, "not portal ap manual connect");
        }
    }

    public void startBrowserOnClickNotification(Context context, String url) {
        String operator;
        Notification notification = new Notification();
        String usedUrl = Global.getString(context.getContentResolver(), "captive_portal_server");
        if (!TextUtils.isEmpty(usedUrl) && usedUrl.startsWith("http")) {
            Log.d(LOG_TAG, "startBrowserOnClickNotification: use the portal url from the settings");
            url = usedUrl;
        } else if (IS_CHINA) {
            operator = TelephonyManager.getDefault().getNetworkOperator();
            if (!(operator == null || operator.length() == 0 || !operator.startsWith("460"))) {
                url = HwNetworkPropertyChecker.CHINA_MAINLAND_BACKUP_SERVER;
            }
        }
        operator = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startBrowserOnClickNotification url: ");
        stringBuilder.append(url);
        Log.d(operator, stringBuilder.toString());
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
        intent.setFlags(272629760);
        notification.contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
        try {
            String packageName = "com.android.browser";
            String className = "com.android.browser.BrowserActivity";
            if (Utils.isPackageInstalled("com.huawei.browser", context)) {
                packageName = "com.huawei.browser";
                className = "com.huawei.browser.Main";
            }
            intent.setClassName(packageName, className);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                Log.d(LOG_TAG, "default browser not exist..");
                notification.contentIntent.send();
            } catch (CanceledException e2) {
                String str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Sending contentIntent failed: ");
                stringBuilder2.append(e2);
                Log.e(str, stringBuilder2.toString());
            }
        }
    }

    public NetworkMonitor createHwNetworkMonitor(Context context, Handler handler, NetworkAgentInfo nai, NetworkRequest defaultRequest) {
        return new HwNetworkMonitor(context, handler, nai, defaultRequest);
    }

    public Network getNetworkForTypeWifi() {
        if (this.mHwConnectivityService != null) {
            return this.mHwConnectivityService.getNetworkForTypeWifi();
        }
        return null;
    }

    public boolean isP2pTether(String iface) {
        boolean z = false;
        if (iface == null) {
            return false;
        }
        if (iface.startsWith(P2P_TETHER_IFAC) || iface.startsWith(P2P_TETHER_IFAC_110x) || iface.startsWith(P2P_TETHER_IFAC_QCOM)) {
            z = true;
        }
        return z;
    }

    public void stopP2pTether(Context context) {
        if (context != null) {
            Channel channel = null;
            ActionListener mWifiP2pBridgeCreateListener = new ActionListener() {
                public void onSuccess() {
                    Log.d(HwConnectivityManagerImpl.TAG, " Stop p2p tether success");
                }

                public void onFailure(int reason) {
                    String access$000 = HwConnectivityManagerImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" Stop p2p tether fail:");
                    stringBuilder.append(reason);
                    Log.e(access$000, stringBuilder.toString());
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

    public boolean isVpnDisabled() {
        boolean allow = this.mVpnManager.isVpnDisabled(null);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isVpnDisabled and result is ");
        stringBuilder.append(allow);
        Log.d(str, stringBuilder.toString());
        return allow;
    }

    public boolean isInsecureVpnDisabled() {
        boolean allow = this.mVpnManager.isInsecureVpnDisabled(null);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isInsecureVpnDisabled and result is ");
        stringBuilder.append(allow);
        Log.d(str, stringBuilder.toString());
        return allow;
    }

    public void onDnsEvent(Context context, int returnCode, int latencyMs, int netId) {
        this.mDnsCount++;
        if (15 == returnCode) {
            this.mDnsIpv6Timeout++;
        }
        if (returnCode == 0) {
            this.mDnsResponseTotalTime += latencyMs;
            if (!this.mSendDnsFailFlag && this.mDnsFailQ.size() > 0) {
                this.mDnsFailQ.clear();
            }
            if (latencyMs > 2000) {
                this.mDnsResponseOver2000Count++;
                sendIntentPsSlowDnsOver2000(context, latencyMs);
            } else {
                if (!this.mSendDnsOver2000Flag && this.mDnsOver2000Q.size() > 0) {
                    this.mDnsOver2000Q.clear();
                }
                if (latencyMs <= 20) {
                    this.mDnsResponse20Count++;
                } else if (latencyMs <= 150) {
                    this.mDnsResponse150Count++;
                } else if (latencyMs <= 500) {
                    this.mDnsResponse500Count++;
                } else if (latencyMs <= 1000) {
                    this.mDnsResponse1000Count++;
                } else if (latencyMs <= 2000) {
                    this.mDnsResponse2000Count++;
                }
            }
        } else {
            this.mDnsFailCount++;
            sendIntentPsSlowDnsFail(context, returnCode);
        }
        if (100 == this.mDnsCount) {
            sendIntentDnsEvent(context, this.mLastConnectedType);
        }
        if (this.mHwConnectivityService != null) {
            this.mHwConnectivityService.recordPrivateDnsEvent(context, returnCode, latencyMs, netId);
        }
    }

    public boolean isBypassPrivateDns(int netId) {
        if (this.mHwConnectivityService != null) {
            return this.mHwConnectivityService.isBypassPrivateDns(netId);
        }
        return false;
    }

    private void clearInvalidPrivateDnsNetworkInfo() {
        if (this.mHwConnectivityService != null) {
            this.mHwConnectivityService.clearInvalidPrivateDnsNetworkInfo();
        }
    }

    private boolean randomSampling(int samplingRatio) {
        if (samplingRatio > 0 && new Random().nextInt(samplingRatio) != 0) {
            return false;
        }
        return true;
    }

    private void sendIntentPsSlowDnsFail(Context context, int returnCode) {
        Date now = new Date();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" sendIntentPsSlowDnsFail mSendDnsFailFlag = ");
        stringBuilder.append(this.mSendDnsFailFlag);
        stringBuilder.append("mDnsFailQ.size() = ");
        stringBuilder.append(this.mDnsFailQ.size());
        Log.d(str, stringBuilder.toString());
        if (!this.mSendDnsFailFlag) {
            this.mDnsFailQ.addLast(now);
            if (this.mDnsFailQ.size() == 6) {
                if (now.getTime() - ((Date) this.mDnsFailQ.getFirst()).getTime() <= 45000 && randomSampling(8)) {
                    this.mSendDnsFailFlag = true;
                    if (this.mConnectedType == 2) {
                        Intent chrIntent = new Intent(INTENT_DS_WEB_STAT_REPORT);
                        chrIntent.putExtra("ReportType", 5);
                        chrIntent.putExtra("WebFailCode", returnCode);
                        context.sendBroadcast(chrIntent, CHR_BROADCAST_PERMISSION);
                        Log.d(LOG_TAG, " sendIntentPsSlowDnsFail");
                    }
                }
                this.mDnsFailQ.removeFirst();
            }
        } else if (this.mDnsFailQ.isEmpty() || now.getTime() - ((Date) this.mDnsFailQ.getLast()).getTime() > AwareAppMngSort.PREVIOUS_APP_DIRCACTIVITY_DECAYTIME) {
            Log.d(LOG_TAG, " sendIntentPsSlowDnsFail reset mSendDnsFailFlag");
            this.mSendDnsFailFlag = false;
            this.mDnsFailQ.clear();
            this.mDnsFailQ.addLast(now);
        }
    }

    private void sendIntentPsSlowDnsOver2000(Context context, int delay) {
        Date now = new Date();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" sendIntentPsSlowDnsOver2000 mSendDnsOver2000Flag = ");
        stringBuilder.append(this.mSendDnsOver2000Flag);
        stringBuilder.append("mDnsOver2000Q.size() = ");
        stringBuilder.append(this.mDnsOver2000Q.size());
        Log.d(str, stringBuilder.toString());
        if (!this.mSendDnsOver2000Flag) {
            this.mDnsOver2000Q.addLast(now);
            if (this.mDnsOver2000Q.size() == 6) {
                if (now.getTime() - ((Date) this.mDnsOver2000Q.getFirst()).getTime() <= 45000 && randomSampling(2)) {
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
        } else if (this.mDnsOver2000Q.isEmpty() || now.getTime() - ((Date) this.mDnsOver2000Q.getLast()).getTime() > AwareAppMngSort.PREVIOUS_APP_DIRCACTIVITY_DECAYTIME) {
            Log.d(LOG_TAG, " sendIntentPsSlowDnsOver2000 reset mSendDnsOver2000Flag");
            this.mSendDnsOver2000Flag = false;
            this.mDnsOver2000Q.clear();
            this.mDnsOver2000Q.addLast(now);
        }
    }

    private void sendIntentDnsEvent(Context context, int connectType) {
        if (connectType == 0 || this.mDnsCount == 0) {
            Log.d(LOG_TAG, " not connect to network or DNS count is 0, return");
            return;
        }
        Intent intent;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendIntentDnsEvent connectType ");
        stringBuilder.append(connectType);
        Log.d(str, stringBuilder.toString());
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("sendIntentDnsEvent mDnsCount:");
        stringBuilder.append(this.mDnsCount);
        stringBuilder.append(" mDnsIpv6Timeout:");
        stringBuilder.append(this.mDnsIpv6Timeout);
        stringBuilder.append(" mDnsResponseTotalTime:");
        stringBuilder.append(this.mDnsResponseTotalTime);
        stringBuilder.append(" mDnsFailCount:");
        stringBuilder.append(this.mDnsFailCount);
        stringBuilder.append(" mDnsResponse20Count:");
        stringBuilder.append(this.mDnsResponse20Count);
        stringBuilder.append(" mDnsResponse150Count:");
        stringBuilder.append(this.mDnsResponse150Count);
        stringBuilder.append(" mDnsResponse500Count:");
        stringBuilder.append(this.mDnsResponse500Count);
        stringBuilder.append(" mDnsResponse1000Count:");
        stringBuilder.append(this.mDnsResponse1000Count);
        stringBuilder.append(" mDnsResponse2000Count:");
        stringBuilder.append(this.mDnsResponse2000Count);
        stringBuilder.append(" mDnsResponseOver2000Count:");
        stringBuilder.append(this.mDnsResponseOver2000Count);
        Log.d(str, stringBuilder.toString());
        if (connectType == 2) {
            intent = new Intent(INTENT_DS_DNS_STATISTICS);
        } else {
            intent = new Intent(INTENT_WIFI_DNS_STATISTICS);
        }
        Bundle extras = new Bundle();
        extras.putInt("dnsCount", this.mDnsCount);
        extras.putInt("dnsIpv6Timeout", this.mDnsIpv6Timeout);
        extras.putInt("dnsResponseTotalTime", this.mDnsResponseTotalTime);
        extras.putInt("dnsFailCount", this.mDnsFailCount);
        extras.putInt("dnsResponse20Count", this.mDnsResponse20Count);
        extras.putInt("dnsResponse150Count", this.mDnsResponse150Count);
        extras.putInt("dnsResponse500Count", this.mDnsResponse500Count);
        extras.putInt("dnsResponse1000Count", this.mDnsResponse1000Count);
        extras.putInt("dnsResponse2000Count", this.mDnsResponse2000Count);
        extras.putInt("dnsResponseOver2000Count", this.mDnsResponseOver2000Count);
        intent.putExtras(extras);
        this.mDnsCount = 0;
        this.mDnsIpv6Timeout = 0;
        this.mDnsResponseTotalTime = 0;
        this.mDnsFailCount = 0;
        this.mDnsResponse20Count = 0;
        this.mDnsResponse150Count = 0;
        this.mDnsResponse500Count = 0;
        this.mDnsResponse1000Count = 0;
        this.mDnsResponse2000Count = 0;
        this.mDnsResponseOver2000Count = 0;
        context.sendBroadcast(intent, CHR_BROADCAST_PERMISSION);
    }

    public boolean needCaptivePortalCheck(NetworkAgentInfo nai, Context context) {
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (!SubscriptionManager.isUsableSubIdValue(subId) || context == null || TelephonyManager.getDefault() == null) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("needCaptivePortal: subId =");
            stringBuilder.append(subId);
            stringBuilder.append(" is Invalid, or context is null,return false.");
            Log.e(str, stringBuilder.toString());
            return false;
        }
        int deviceProvisioned = Global.getInt(context.getContentResolver(), "device_provisioned", 0);
        String str2;
        if (deviceProvisioned != 0) {
            str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("needCaptivePortal: deviceProvisioned=");
            stringBuilder2.append(deviceProvisioned);
            stringBuilder2.append(", return false.");
            Log.e(str2, stringBuilder2.toString());
            return false;
        } else if (nai == null || nai.networkInfo == null || nai.networkInfo.getType() != 0) {
            Log.e(LOG_TAG, "needCaptivePortal: NetworkAgentInfo is not Mobile Type,return false.");
            return false;
        } else {
            str2 = TelephonyManager.getDefault().getSimOperator(subId);
            if (TextUtils.isEmpty(str2)) {
                return false;
            }
            String plmnConfig = System.getString(context.getContentResolver(), "need_captive_portal_by_hplmn");
            if (TextUtils.isEmpty(plmnConfig)) {
                return false;
            }
            for (String plmn : plmnConfig.split(",")) {
                if (str2.equals(plmn)) {
                    String str3 = LOG_TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("needCaptivePortalCheck return true for simOperator=");
                    stringBuilder3.append(str2);
                    Log.d(str3, stringBuilder3.toString());
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
        IConnectivityExManager sService = Stub.asInterface(ServiceManager.getService("hwConnectivityExService"));
        if (sService == null) {
            return false;
        }
        try {
            return sService.isApIpv4AddressFixed();
        } catch (RemoteException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteException");
            stringBuilder.append(e.getMessage());
            Log.d(str, stringBuilder.toString());
            return false;
        }
    }

    public void setApIpv4AddressFixed(boolean isFixed) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setApIpv4AddressFixed: ");
        stringBuilder.append(isFixed);
        Log.d(str, stringBuilder.toString());
        IConnectivityExManager sService = Stub.asInterface(ServiceManager.getService("hwConnectivityExService"));
        if (sService != null) {
            try {
                sService.setApIpv4AddressFixed(isFixed);
            } catch (RemoteException e) {
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("RemoteException");
                stringBuilder2.append(e.getMessage());
                Log.d(str2, stringBuilder2.toString());
            }
        }
    }
}
