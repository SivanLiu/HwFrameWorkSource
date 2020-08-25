package com.huawei.hwwifiproservice;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import com.android.server.wifi.hwUtil.ScanResultRecords;
import com.android.server.wifi.hwUtil.StringUtilEx;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HwNetworkPropertyChecker {
    private static final String BUNDLE_FLAG_MAIN_DETECT_MS = "mainDetectMs";
    public static final String BUNDLE_FLAG_REDIRECT_URL = "redirectUrl";
    public static final String BUNDLE_FLAG_USED_SERVER = "usedServer";
    public static final String BUNDLE_PROBE_RESPONSE_CODE = "probeResponseCode";
    public static final String BUNDLE_PROBE_RESPONSE_TIME = "probeResponseTime";
    private static final int HAS_INTERNET_CODE = 204;
    private static final int HTTP_SERVER_OPT_FAILED = 599;
    public static final int HW_MAX_RETRIES = 3;
    private static final int LAC_UNKNOWN = -1;
    private static final int MSG_DNS_RESP_RCVD = 104;
    private static final int MSG_HTML_DOWNLOADED_RCVD = 105;
    private static final int MSG_HTTP_RESP_RCVD = 103;
    private static final int MSG_HTTP_RESP_TIMEOUT = 101;
    private static final int MSG_NETWORK_DISCONNECTED = 102;
    public static final int NETWORK_PROPERTY_INTERNET = 5;
    public static final int NETWORK_PROPERTY_NO_INTERNET = -1;
    public static final int NETWORK_PROPERTY_PORTAL = 6;
    private static final int NO_INTERNET_CODE = 599;
    private static final int PORTAL_CODE = 302;
    private static final String PRODUCT_LOCALE_CN = "CN";
    public static final String SERVER_HICLOUD = "hicloud";
    public static final String TAG = "HwNetworkPropertyChecker";
    private BroadcastReceiver mBroadcastReceiver;
    private CellLocation mCellLocation;
    /* access modifiers changed from: private */
    public boolean mCheckerInitialized;
    /* access modifiers changed from: private */
    public int mCheckingCounter;
    protected Context mContext;
    private boolean mControllerNotified;
    /* access modifiers changed from: private */
    public WifiConfiguration mCurrentWifiConfig;
    private long mDetectTime;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public final Object mHtmlDownloadWaitingLock = new Object();
    /* access modifiers changed from: private */
    public final Object mHttpRespWaitingLock = new Object();
    /* access modifiers changed from: private */
    public int mHttpResponseCode;
    private NetworkMonitor mHwNetworkMonitor;
    protected boolean mIgnoreRxCounter;
    private boolean mInOversea;
    private IntentFilter mIntentFilter;
    /* access modifiers changed from: private */
    public boolean mLastDetectTimeout = false;
    /* access modifiers changed from: private */
    public int mMaxAttempts;
    /* access modifiers changed from: private */
    public boolean mMobileHotspot;
    private Network mNetwork;
    /* access modifiers changed from: private */
    public AtomicBoolean mNetworkDisconnected = new AtomicBoolean(true);
    private Network mNetworkInfo;
    private String mPortalDetectStatisticsInfo = null;
    /* access modifiers changed from: private */
    public String mPortalRedirectedUrl = "";
    /* access modifiers changed from: private */
    public int mRawHttpRespCode;
    /* access modifiers changed from: private */
    public String mRawRedirectedHostName;
    /* access modifiers changed from: private */
    public final Object mRxCounterWaitLock = new Object();
    protected int mTcpRxCounter;
    private TelephonyManager mTelManager;
    /* access modifiers changed from: private */
    public String mUsedServer = null;
    protected WifiManager mWifiManager;
    /* access modifiers changed from: private */
    public boolean rxCounterRespRcvd;

    public static class StarndardPortalInfo {
        public String currentSsid = "";
        public int lac = -1;
        public long timestamp = 0;
    }

    public HwNetworkPropertyChecker(Context context, WifiManager wifiManager, TelephonyManager telManager, boolean enabled, Network agent, boolean needRxBroadcast) {
        this.mContext = context;
        this.mWifiManager = wifiManager;
        this.mTelManager = telManager;
        this.mNetworkInfo = agent;
        this.mNetwork = null;
        this.mCurrentWifiConfig = null;
        this.mInOversea = false;
        this.mCheckerInitialized = false;
        this.mHttpResponseCode = 599;
        this.mIgnoreRxCounter = false;
        this.mControllerNotified = false;
        this.mCheckingCounter = 0;
        this.mMaxAttempts = 3;
        this.mTcpRxCounter = 0;
        if (this.mWifiManager == null) {
            this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        }
        init(needRxBroadcast);
        this.mHwNetworkMonitor = new NetworkMonitor(context);
    }

    private void init(boolean needRxBroadcast) {
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        if (needRxBroadcast) {
            this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_RESPONSE_TCP_RX_COUNTER);
        }
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* class com.huawei.hwwifiproservice.HwNetworkPropertyChecker.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                int i = 0;
                if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (info != null && NetworkInfo.DetailedState.DISCONNECTED == info.getDetailedState() && !HwNetworkPropertyChecker.this.mNetworkDisconnected.get()) {
                        HwHiLog.d(HwNetworkPropertyChecker.TAG, false, "NETWORK_STATE_CHANGED_ACTION, network is connected --> disconnected.", new Object[0]);
                        HwNetworkPropertyChecker.this.mNetworkDisconnected.set(true);
                        HwNetworkPropertyChecker.this.mHandler.sendMessage(Message.obtain(HwNetworkPropertyChecker.this.mHandler, 102, 0, 0));
                    }
                } else if (WifiProCommonDefs.ACTION_RESPONSE_TCP_RX_COUNTER.equals(intent.getAction())) {
                    int rx = intent.getIntExtra(WifiProCommonDefs.EXTRA_FLAG_TCP_RX_COUNTER, 0);
                    HwNetworkPropertyChecker hwNetworkPropertyChecker = HwNetworkPropertyChecker.this;
                    if (rx > 0) {
                        i = rx;
                    }
                    hwNetworkPropertyChecker.mTcpRxCounter = i;
                    synchronized (HwNetworkPropertyChecker.this.mRxCounterWaitLock) {
                        boolean unused = HwNetworkPropertyChecker.this.rxCounterRespRcvd = true;
                        HwNetworkPropertyChecker.this.mRxCounterWaitLock.notifyAll();
                    }
                }
            }
        };
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter, WifiProCommonDefs.NETWORK_CHECKER_RECV_PERMISSION, null);
        this.mHandler = new Handler(Looper.getMainLooper()) {
            /* class com.huawei.hwwifiproservice.HwNetworkPropertyChecker.AnonymousClass2 */

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 101:
                        int unused = HwNetworkPropertyChecker.this.mHttpResponseCode = 599;
                        boolean reconfirmDetect = msg.arg1 == 1;
                        if (!reconfirmDetect && HwNetworkPropertyChecker.this.mCheckingCounter < HwNetworkPropertyChecker.this.mMaxAttempts) {
                            boolean unused2 = HwNetworkPropertyChecker.this.mLastDetectTimeout = true;
                        }
                        int mainHttpCode = HwNetworkPropertyChecker.this.mLastDetectTimeout ? 600 : msg.arg2;
                        Bundle bundle = (Bundle) msg.obj;
                        if (bundle != null) {
                            long mainDetectMs = bundle.getLong(HwNetworkPropertyChecker.BUNDLE_FLAG_MAIN_DETECT_MS);
                            String usedServer = bundle.getString(HwNetworkPropertyChecker.BUNDLE_FLAG_USED_SERVER);
                            HwNetworkPropertyChecker hwNetworkPropertyChecker = HwNetworkPropertyChecker.this;
                            hwNetworkPropertyChecker.updatePortalDetectionStatistics(reconfirmDetect, WifiProCommonUtils.RESP_CODE_TIMEOUT, -1, usedServer, mainHttpCode, mainDetectMs, hwNetworkPropertyChecker.mCurrentWifiConfig);
                            if (reconfirmDetect) {
                                int unused3 = HwNetworkPropertyChecker.this.mHttpResponseCode = WifiProCommonUtils.RESP_CODE_TIMEOUT;
                            }
                            synchronized (HwNetworkPropertyChecker.this.mHttpRespWaitingLock) {
                                HwNetworkPropertyChecker.this.mHttpRespWaitingLock.notifyAll();
                            }
                            break;
                        } else {
                            HwHiLog.d(HwNetworkPropertyChecker.TAG, false, "MSG_HTTP_RESP_TIMEOUT get Bundle fail,Bundle is null", new Object[0]);
                            break;
                        }
                    case 102:
                        if (HwNetworkPropertyChecker.this.mHandler.hasMessages(101)) {
                            HwHiLog.d(HwNetworkPropertyChecker.TAG, false, "MSG_HTTP_RESP_TIMEOUT msg removed because of disconnected.", new Object[0]);
                            HwNetworkPropertyChecker.this.mHandler.removeMessages(101);
                        }
                        int unused4 = HwNetworkPropertyChecker.this.mHttpResponseCode = 599;
                        int unused5 = HwNetworkPropertyChecker.this.mRawHttpRespCode = 599;
                        boolean unused6 = HwNetworkPropertyChecker.this.mMobileHotspot = false;
                        boolean unused7 = HwNetworkPropertyChecker.this.mCheckerInitialized = false;
                        String unused8 = HwNetworkPropertyChecker.this.mRawRedirectedHostName = null;
                        String unused9 = HwNetworkPropertyChecker.this.mPortalRedirectedUrl = "";
                        String unused10 = HwNetworkPropertyChecker.this.mUsedServer = null;
                        synchronized (HwNetworkPropertyChecker.this.mHttpRespWaitingLock) {
                            HwNetworkPropertyChecker.this.mHttpRespWaitingLock.notifyAll();
                        }
                        break;
                    case 103:
                        if (HwNetworkPropertyChecker.this.mHandler.hasMessages(101)) {
                            HwHiLog.d(HwNetworkPropertyChecker.TAG, false, "MSG_HTTP_RESP_TIMEOUT msg removed because of HTTP response received.", new Object[0]);
                            HwNetworkPropertyChecker.this.mHandler.removeMessages(101);
                        }
                        synchronized (HwNetworkPropertyChecker.this.mHttpRespWaitingLock) {
                            HwNetworkPropertyChecker.this.mHttpRespWaitingLock.notifyAll();
                        }
                        break;
                    case 104:
                        if (HwNetworkPropertyChecker.this.mHandler.hasMessages(101)) {
                            HwHiLog.d(HwNetworkPropertyChecker.TAG, false, "MSG_HTTP_RESP_TIMEOUT msg removed because of DNS response received.", new Object[0]);
                            HwNetworkPropertyChecker.this.mHandler.removeMessages(101);
                        }
                        synchronized (HwNetworkPropertyChecker.this.mHttpRespWaitingLock) {
                            HwNetworkPropertyChecker.this.mHttpRespWaitingLock.notifyAll();
                        }
                        break;
                    case 105:
                        if (HwNetworkPropertyChecker.this.mHandler.hasMessages(105)) {
                            HwHiLog.d(HwNetworkPropertyChecker.TAG, false, "MSG_HTML_DOWNLOADED_RCVD msg removed because of html downloaded.", new Object[0]);
                            HwNetworkPropertyChecker.this.mHandler.removeMessages(105);
                        }
                        synchronized (HwNetworkPropertyChecker.this.mHtmlDownloadWaitingLock) {
                            HwNetworkPropertyChecker.this.mHtmlDownloadWaitingLock.notifyAll();
                        }
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    private static class OneAddressPerFamilyNetwork extends Network {
        public OneAddressPerFamilyNetwork(Network network) {
            super(network);
        }
    }

    private void initCurrWifiConfig() {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
            if (configNetworks != null && wifiInfo != null) {
                for (int i = 0; i < configNetworks.size(); i++) {
                    WifiConfiguration config = configNetworks.get(i);
                    if (config.networkId == wifiInfo.getNetworkId()) {
                        this.mCurrentWifiConfig = config;
                        HwHiLog.d(TAG, false, "initialize, current rssi = %{public}d,network = %{public}s", new Object[]{Integer.valueOf(wifiInfo.getRssi()), StringUtilEx.safeDisplaySsid(config.getPrintableSsid())});
                        this.mNetworkDisconnected.set(false);
                        return;
                    }
                }
            }
        }
    }

    private Network getNetworkForTypeWifi() {
        Bundle bundle = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 14, null);
        if (bundle != null) {
            return (Network) bundle.getParcelable(NetworkMonitor.KEY_NETWORK_NAME);
        }
        return null;
    }

    private void initialize(boolean reconfirm) {
        if (!this.mCheckerInitialized) {
            if (this.mWifiManager == null) {
                this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            }
            if (this.mTelManager == null) {
                this.mTelManager = (TelephonyManager) this.mContext.getSystemService("phone");
            }
            if (this.mNetwork == null) {
                Network network = this.mNetworkInfo;
                if (network != null) {
                    this.mNetwork = new OneAddressPerFamilyNetwork(network);
                } else {
                    Network activeNetwork = getNetworkForTypeWifi();
                    if (activeNetwork != null) {
                        this.mNetwork = new OneAddressPerFamilyNetwork(activeNetwork);
                    }
                }
            }
            initCurrWifiConfig();
            String operator = this.mTelManager.getNetworkOperator();
            if (operator == null || operator.length() == 0) {
                if (!PRODUCT_LOCALE_CN.equalsIgnoreCase(WifiProCommonUtils.getProductLocale())) {
                    HwHiLog.d(TAG, false, "initialize, can't get network operator!", new Object[0]);
                    this.mInOversea = true;
                }
            } else if (!operator.startsWith(WifiProCommonUtils.COUNTRY_CODE_CN)) {
                this.mInOversea = true;
            }
            this.mMobileHotspot = HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(this.mContext);
            this.mCheckerInitialized = true;
            WifiConfiguration wifiConfiguration = this.mCurrentWifiConfig;
            if (wifiConfiguration != null) {
                HwHiLog.d(TAG, false, "initialize, AP's network history = %{public}s, operator = %{public}s, mInOversea = %{public}s", new Object[]{wifiConfiguration.internetHistory, operator, String.valueOf(this.mInOversea)});
            }
        }
        if (reconfirm) {
            this.mNetwork = getNetworkForTypeWifi();
            initCurrWifiConfig();
        }
        this.mIgnoreRxCounter = false;
    }

    private void sendCheckResultWhenConnected(int finalRespCode, String action, String flag, int property) {
        Intent intent = new Intent(action);
        intent.setFlags(67108864);
        intent.putExtra(flag, property);
        if (WifiProCommonDefs.EXTRA_FLAG_NETWORK_PROPERTY.equals(flag)) {
            boolean firstDetected = true;
            if (property == 6) {
                if (!TextUtils.isEmpty(this.mRawRedirectedHostName)) {
                    intent.putExtra(WifiProCommonDefs.EXTRA_RAW_REDIRECTED_HOST, this.mRawRedirectedHostName);
                }
                intent.putExtra(WifiProCommonDefs.EXTRA_STANDARD_PORTAL_NETWORK, true);
            }
            String str = this.mPortalDetectStatisticsInfo;
            if (str != null) {
                intent.putExtra(WifiProCommonUtils.KEY_PORTAL_DETECT_STAT_INFO, str);
            }
            WifiConfiguration wifiConfiguration = this.mCurrentWifiConfig;
            if (wifiConfiguration == null || !WifiProCommonUtils.matchedRequestByHistory(wifiConfiguration.internetHistory, 103)) {
                firstDetected = false;
            }
            intent.putExtra(WifiProCommonUtils.KEY_PORTAL_HTTP_RESP_CODE, finalRespCode);
            intent.putExtra(WifiProCommonUtils.KEY_PORTAL_FIRST_DETECT, firstDetected);
            intent.putExtra(WifiProCommonUtils.KEY_PORTAL_REDIRECTED_URL, this.mPortalRedirectedUrl);
            WifiConfiguration wifiConfiguration2 = this.mCurrentWifiConfig;
            intent.putExtra(WifiProCommonUtils.KEY_PORTAL_CONFIG_KEY, wifiConfiguration2 != null ? wifiConfiguration2.configKey() : "");
        }
        this.mContext.sendBroadcast(intent, WifiProCommonDefs.NETWORK_CHECKER_RECV_PERMISSION);
    }

    public int isCaptivePortal(boolean reconfirm) {
        return isCaptivePortal(reconfirm, false, false);
    }

    public long getDetectTime() {
        return this.mDetectTime;
    }

    public int isCaptivePortal(boolean reconfirm, boolean portalNetwork, boolean wifiBackground) {
        HwHiLog.d(TAG, false, "=====ENTER: ===isCaptivePortal, reconfirm = %{public}s, counter = %{public}d", new Object[]{String.valueOf(reconfirm), Integer.valueOf(this.mCheckingCounter)});
        int httpResponseCode = 599;
        initialize(reconfirm);
        this.mCheckingCounter++;
        if (!this.mNetworkDisconnected.get()) {
            Bundle result = this.mHwNetworkMonitor.getProbeResponse();
            httpResponseCode = result.getInt(BUNDLE_PROBE_RESPONSE_CODE, 599);
            this.mUsedServer = result.getString(BUNDLE_FLAG_USED_SERVER, "");
            this.mDetectTime = result.getLong(BUNDLE_PROBE_RESPONSE_TIME, 0);
            this.mPortalRedirectedUrl = result.getString(BUNDLE_FLAG_REDIRECT_URL, "");
            if ((isHomeRouter() || isHilinkRouter()) && WifiProCommonUtils.isRedirectedRespCodeByGoogle(httpResponseCode)) {
                HwHiLog.d(TAG, false, "redirected by home router, do not update history", new Object[0]);
            } else {
                updateStarndardPortalRecord(httpResponseCode, reconfirm);
                updateWifiConfigHistory(httpResponseCode, reconfirm);
            }
        }
        HwHiLog.d(TAG, false, "=====LEAVE: ===isCaptivePortal, httpResponseCode = %{public}d, ovs= %{public}s, reconfirm = %{public}s", new Object[]{Integer.valueOf(httpResponseCode), String.valueOf(this.mInOversea), String.valueOf(reconfirm)});
        WifiProChrUploadManager uploadManager = WifiProChrUploadManager.getInstance(this.mContext);
        if (httpResponseCode == 204) {
            uploadManager.addChrSsidCntStat("activeDetecEvent", "hasInternet");
        } else if (httpResponseCode == 599) {
            uploadManager.addChrSsidCntStat("activeDetecEvent", "noInternet");
        } else if (httpResponseCode == 302) {
            uploadManager.addChrSsidCntStat("activeDetecEvent", "portal");
        } else {
            HwHiLog.w(TAG, false, "httpResponseCode is not in active detect event", new Object[0]);
        }
        return httpResponseCode;
    }

    private boolean isHomeRouter() {
        String[] redirectedUrls = this.mContext.getResources().getStringArray(33816596);
        if (redirectedUrls == null || redirectedUrls.length == 0) {
            return false;
        }
        int length = redirectedUrls.length;
        int i = 0;
        while (i < length) {
            String url = redirectedUrls[i];
            String str = this.mPortalRedirectedUrl;
            if (str == null || !str.contains(url)) {
                i++;
            } else {
                HwHiLog.d(TAG, false, "home router, because the redirect url matches the keyword", new Object[0]);
                return true;
            }
        }
        return false;
    }

    private boolean isHilinkRouter() {
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        if (wifiInfo == null || wifiInfo.getBSSID() == null || ScanResultRecords.getDefault().getHiLinkAp(wifiInfo.getBSSID()) != 1) {
            return false;
        }
        return true;
    }

    private int getCurrentLac() {
        if (this.mTelManager == null) {
            this.mTelManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        this.mCellLocation = this.mTelManager.getCellLocation();
        int simstatus = this.mTelManager.getSimState();
        CellLocation cellLocation = this.mCellLocation;
        if (cellLocation == null || simstatus == 1) {
            return -1;
        }
        if (cellLocation instanceof GsmCellLocation) {
            return ((GsmCellLocation) cellLocation).getLac();
        }
        if (cellLocation instanceof CdmaCellLocation) {
            return ((CdmaCellLocation) cellLocation).getNetworkId();
        }
        HwHiLog.w(TAG, false, "CellLocation PhoneType Unknown.", new Object[0]);
        return -1;
    }

    private void updateStarndardPortalRecord(int respCode, boolean reconfirm) {
        PortalDataBaseManager database;
        if (!reconfirm && WifiProCommonUtils.isRedirectedRespCode(respCode) && WifiProCommonUtils.isOpenType(this.mCurrentWifiConfig) && (database = PortalDataBaseManager.getInstance(this.mContext)) != null && this.mCurrentWifiConfig != null) {
            StarndardPortalInfo portalInfo = new StarndardPortalInfo();
            portalInfo.currentSsid = this.mCurrentWifiConfig.SSID;
            portalInfo.lac = getCurrentLac();
            portalInfo.timestamp = System.currentTimeMillis();
            HwHiLog.d(TAG, false, "updateStarndardPortalRecord, ssid = %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(this.mCurrentWifiConfig.SSID)});
            database.updateStandardPortalTable(portalInfo);
        }
    }

    public String getCaptiveUsedServer() {
        if (TextUtils.isEmpty(this.mUsedServer)) {
            return this.mHwNetworkMonitor.getCaptivePortalServerHttpUrl();
        }
        return this.mUsedServer;
    }

    public String getPortalRedirectedUrl() {
        return this.mPortalRedirectedUrl;
    }

    private void updateWifiConfigHistory(int respCode, boolean reconfirm) {
        WifiConfiguration wifiConfiguration;
        if (reconfirm && WifiProCommonUtils.httpReachableOrRedirected(respCode) && (wifiConfiguration = this.mCurrentWifiConfig) != null) {
            String internetHistory = wifiConfiguration.internetHistory;
            boolean z = false;
            if (internetHistory == null || internetHistory.lastIndexOf("/") == -1) {
                HwHiLog.w(TAG, false, "updateWifiConfigHistory, inputed arg is invalid, internetHistory = %{public}s", new Object[]{internetHistory});
                return;
            }
            String status = internetHistory.substring(0, 1);
            if (status != null && status.equals("0")) {
                int newStatus = respCode == 204 ? 1 : 2;
                String internetHistory2 = String.valueOf(newStatus) + "/" + internetHistory.substring(internetHistory.indexOf("/") + 1);
                WifiConfiguration wifiConfiguration2 = this.mCurrentWifiConfig;
                wifiConfiguration2.noInternetAccess = false;
                wifiConfiguration2.validatedInternetAccess = !wifiConfiguration2.noInternetAccess;
                if (newStatus == 1) {
                    WifiConfiguration wifiConfiguration3 = this.mCurrentWifiConfig;
                    wifiConfiguration3.numNoInternetAccessReports = 0;
                    wifiConfiguration3.lastHasInternetTimestamp = System.currentTimeMillis();
                }
                WifiConfiguration wifiConfiguration4 = this.mCurrentWifiConfig;
                if (wifiConfiguration4.portalNetwork || respCode != 204) {
                    z = true;
                }
                wifiConfiguration4.portalNetwork = z;
                this.mCurrentWifiConfig.internetHistory = internetHistory2;
                if (newStatus == 1) {
                    sendCheckResultWhenConnected(204, WifiProCommonDefs.ACTION_NETWORK_CONDITIONS_MEASURED, WifiProCommonDefs.EXTRA_IS_INTERNET_READY, 1);
                }
                Intent intent = new Intent(WifiProCommonDefs.ACTION_UPDATE_CONFIG_HISTORY);
                intent.putExtra(WifiProCommonDefs.EXTRA_FLAG_NEW_WIFI_CONFIG, new WifiConfiguration(this.mCurrentWifiConfig));
                this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, WifiProCommonDefs.NETWORK_CHECKER_RECV_PERMISSION);
            } else if (status != null && status.equals("2") && respCode == 204) {
                this.mCurrentWifiConfig.lastHasInternetTimestamp = System.currentTimeMillis();
                this.mCurrentWifiConfig.portalCheckStatus = 0;
                Intent intent2 = new Intent(WifiProCommonDefs.ACTION_UPDATE_CONFIG_HISTORY);
                intent2.putExtra(WifiProCommonDefs.EXTRA_FLAG_NEW_WIFI_CONFIG, new WifiConfiguration(this.mCurrentWifiConfig));
                this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL, WifiProCommonDefs.NETWORK_CHECKER_RECV_PERMISSION);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updatePortalDetectionStatistics(boolean reconfirm, int currHttpCode, long currDetectMs, String server, int mainHttpCode, long mainHttpMs, WifiConfiguration config) {
        long currDetectMs2;
        long mainHttpMs2;
        if (!reconfirm && !this.mControllerNotified && config != null && server != null) {
            boolean mainServer = server.contains(SERVER_HICLOUD);
            if (!mainServer || this.mCheckingCounter >= this.mMaxAttempts || !WifiProCommonUtils.unreachableRespCode(currHttpCode)) {
                boolean firstConnected = WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 103);
                boolean currentPortalCode = WifiProCommonUtils.isRedirectedRespCodeByGoogle(currHttpCode);
                if (firstConnected || config.portalNetwork || currentPortalCode) {
                    if (currDetectMs <= 0) {
                        currDetectMs2 = -1;
                    } else {
                        currDetectMs2 = currDetectMs;
                    }
                    if (mainHttpMs <= 0) {
                        mainHttpMs2 = -1;
                    } else {
                        mainHttpMs2 = mainHttpMs;
                    }
                    int mainCode = mainServer ? currHttpCode : mainHttpCode;
                    int backupCode = mainServer ? -1 : currHttpCode;
                    long mainDetectMs = mainServer ? currDetectMs2 : mainHttpMs2;
                    long backupDetectMs = mainServer ? -1 : currDetectMs2;
                    this.mPortalDetectStatisticsInfo = (firstConnected ? 1 : 0) + "|" + (this.mInOversea ? 1 : 0) + "|" + mainCode + "|" + backupCode + "|" + mainDetectMs + "|" + backupDetectMs;
                    HwHiLog.d(TAG, false, "updatePortalDetectionStatistics, portalDetectStatisticsInfo = %{public}s", new Object[]{this.mPortalDetectStatisticsInfo});
                }
            }
        }
    }

    public int getRawHttpRespCode() {
        return this.mRawHttpRespCode;
    }

    public void setRawRedirectedHostName(String hostName) {
        HwHiLog.i(TAG, false, "setRawRedirectedHostName", new Object[0]);
        this.mRawRedirectedHostName = hostName;
    }

    public void resetCheckerStatus() {
        this.mCheckingCounter = 0;
        this.mCheckerInitialized = false;
        this.mInOversea = false;
        this.mControllerNotified = false;
        this.mIgnoreRxCounter = false;
        this.mLastDetectTimeout = false;
        synchronized (this.mRxCounterWaitLock) {
            this.rxCounterRespRcvd = false;
        }
    }

    public void release() {
        HwHiLog.i(TAG, false, "release, checking counter = %{public}d", new Object[]{Integer.valueOf(this.mCheckingCounter)});
        BroadcastReceiver broadcastReceiver = this.mBroadcastReceiver;
        if (broadcastReceiver != null) {
            this.mContext.unregisterReceiver(broadcastReceiver);
            this.mBroadcastReceiver = null;
        }
        this.mNetworkDisconnected.set(true);
        this.mLastDetectTimeout = false;
    }
}
