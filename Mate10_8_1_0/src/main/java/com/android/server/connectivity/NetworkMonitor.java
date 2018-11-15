package com.android.server.connectivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.NetworkEvent;
import android.net.metrics.ValidationProbeEvent;
import android.net.util.Stopwatch;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.LocalLog.ReadOnlyLocalLog;
import android.util.Log;
import com.android.internal.util.State;
import com.android.server.AbsNetworkMonitor;
import com.android.server.HwConnectivityManager;
import com.android.server.HwServiceFactory;
import com.android.server.am.ProcessList;
import com.android.server.display.DisplayTransformManager;
import com.android.server.location.LocationFudger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NetworkMonitor extends AbsNetworkMonitor {
    public static final String ACTION_NETWORK_CONDITIONS_MEASURED = "android.net.conn.NETWORK_CONDITIONS_MEASURED";
    private static final boolean ADD_CRICKET_WIFI_MANAGER = SystemProperties.getBoolean("ro.config.cricket_wifi_manager", false);
    private static final String BAKUP_SERVER = "www.baidu.com";
    private static final String BAKUP_SERV_PAGE = "/";
    private static final int BASE = 532480;
    private static final int BLAME_FOR_EVALUATION_ATTEMPTS = 5;
    private static final int CAPTIVE_PORTAL_REEVALUATE_DELAY_MS = 600000;
    private static final int CMD_CAPTIVE_PORTAL_APP_FINISHED = 532489;
    private static final int CMD_CAPTIVE_PORTAL_RECHECK = 532492;
    public static final int CMD_FORCE_REEVALUATION = 532488;
    public static final int CMD_LAUNCH_CAPTIVE_PORTAL_APP = 532491;
    public static final int CMD_NETWORK_CONNECTED = 532481;
    public static final int CMD_NETWORK_DISCONNECTED = 532487;
    private static final int CMD_REEVALUATE = 532486;
    private static final String COUNTRY_CODE_CN = "460";
    private static final boolean DBG = true;
    private static final String DEFAULT_FALLBACK_URL = "http://www.google.com/gen_204";
    private static final String DEFAULT_HTTPS_URL = "https://www.google.com/generate_204";
    private static final String DEFAULT_HTTP_URL = "http://connectivitycheck.gstatic.com/generate_204";
    private static final String DEFAULT_OTHER_FALLBACK_URLS = "http://play.googleapis.com/generate_204";
    private static final String DEFAULT_SERV_PAGE = "/generate_204";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.32 Safari/537.36";
    public static final int EVENT_NETWORK_TESTED = 532482;
    public static final int EVENT_PROVISIONING_NOTIFICATION = 532490;
    public static final String EXTRA_BSSID = "extra_bssid";
    public static final String EXTRA_CELL_ID = "extra_cellid";
    public static final String EXTRA_CONNECTIVITY_TYPE = "extra_connectivity_type";
    public static final String EXTRA_IS_CAPTIVE_PORTAL = "extra_is_captive_portal";
    public static final String EXTRA_NETWORK_TYPE = "extra_network_type";
    public static final String EXTRA_REQUEST_TIMESTAMP_MS = "extra_request_timestamp_ms";
    public static final String EXTRA_RESPONSE_RECEIVED = "extra_response_received";
    public static final String EXTRA_RESPONSE_TIMESTAMP_MS = "extra_response_timestamp_ms";
    public static final String EXTRA_SSID = "extra_ssid";
    private static final int IGNORE_REEVALUATE_ATTEMPTS = 5;
    private static final int INITIAL_REEVALUATE_DELAY_MS = 1000;
    private static final int INVALID_UID = -1;
    private static final boolean IS_CHINA_AREA = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    private static final int MAX_REEVALUATE_DELAY_MS = 600000;
    public static final int NETWORK_TEST_RESULT_INVALID = 1;
    public static final int NETWORK_TEST_RESULT_VALID = 0;
    private static final String PERMISSION_ACCESS_NETWORK_CONDITIONS = "android.permission.ACCESS_NETWORK_CONDITIONS";
    private static final int PROBE_TIMEOUT_MS = 3000;
    private static final String SERVER_BAIDU = "baidu";
    private static final int SOCKET_TIMEOUT_MS = 10000;
    private static final String TAG = NetworkMonitor.class.getSimpleName();
    private static final boolean VDBG = false;
    private boolean httpReachable;
    private final AlarmManager mAlarmManager;
    private final URL[] mCaptivePortalFallbackUrls;
    private final URL mCaptivePortalHttpUrl;
    private final URL mCaptivePortalHttpsUrl;
    private final State mCaptivePortalState;
    private final String mCaptivePortalUserAgent;
    private final Handler mConnectivityServiceHandler;
    private final Context mContext;
    private final NetworkRequest mDefaultRequest;
    private final State mDefaultState;
    private boolean mDontDisplaySigninNotification;
    private final State mEvaluatingState;
    private final Stopwatch mEvaluationTimer;
    protected boolean mIsCaptivePortalCheckEnabled;
    private CaptivePortalProbeResult mLastPortalProbeResult;
    private CustomIntentReceiver mLaunchCaptivePortalAppBroadcastReceiver;
    private final State mMaybeNotifyState;
    private final IpConnectivityLog mMetricsLog;
    private final int mNetId;
    private final Network mNetwork;
    private final NetworkAgentInfo mNetworkAgentInfo;
    private int mNextFallbackUrlIndex;
    private int mReevaluateToken;
    private final TelephonyManager mTelephonyManager;
    private int mUidResponsibleForReeval;
    private String mUrlHeadFieldLocation;
    private boolean mUseHttps;
    private boolean mUserDoesNotWant;
    private final State mValidatedState;
    private int mValidations;
    private final WifiManager mWifiManager;
    public boolean systemReady;
    private final LocalLog validationLogs;

    /* renamed from: com.android.server.connectivity.NetworkMonitor$1ProbeThread */
    final class AnonymousClass1ProbeThread extends Thread {
        private final boolean mIsHttps;
        private volatile CaptivePortalProbeResult mResult = CaptivePortalProbeResult.FAILED;
        final /* synthetic */ URL val$httpUrl;
        final /* synthetic */ URL val$httpsUrl;
        final /* synthetic */ CountDownLatch val$latch;
        final /* synthetic */ ProxyInfo val$proxy;

        public AnonymousClass1ProbeThread(boolean isHttps, ProxyInfo proxyInfo, URL url, URL url2, CountDownLatch countDownLatch) {
            this.val$proxy = proxyInfo;
            this.val$httpsUrl = url;
            this.val$httpUrl = url2;
            this.val$latch = countDownLatch;
            this.mIsHttps = isHttps;
        }

        public CaptivePortalProbeResult result() {
            return this.mResult;
        }

        public void run() {
            if (this.mIsHttps) {
                this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpsUrl, 2);
            } else {
                this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrl, 1);
            }
            if ((this.mIsHttps && this.mResult.isSuccessful()) || (!this.mIsHttps && this.mResult.isPortal())) {
                while (this.val$latch.getCount() > 0) {
                    this.val$latch.countDown();
                }
            }
            this.val$latch.countDown();
        }
    }

    public static final class CaptivePortalProbeResult {
        static final CaptivePortalProbeResult FAILED = new CaptivePortalProbeResult(FAILED_CODE);
        static final int FAILED_CODE = 599;
        static final CaptivePortalProbeResult SUCCESS = new CaptivePortalProbeResult(SUCCESS_CODE);
        static final int SUCCESS_CODE = 204;
        final String detectUrl;
        int mHttpResponseCode;
        final String redirectUrl;

        public CaptivePortalProbeResult(int httpResponseCode, String redirectUrl, String detectUrl) {
            this.mHttpResponseCode = httpResponseCode;
            this.redirectUrl = redirectUrl;
            this.detectUrl = detectUrl;
        }

        public CaptivePortalProbeResult(int httpResponseCode) {
            this(httpResponseCode, null, null);
        }

        boolean isSuccessful() {
            return this.mHttpResponseCode == SUCCESS_CODE;
        }

        boolean isPortal() {
            return !isSuccessful() && this.mHttpResponseCode >= DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE && this.mHttpResponseCode <= 399;
        }

        boolean isFailed() {
            return !isSuccessful() ? isPortal() ^ 1 : false;
        }
    }

    private class CaptivePortalState extends State {
        private static final String ACTION_LAUNCH_CAPTIVE_PORTAL_APP = "android.net.netmon.launchCaptivePortalApp";

        private CaptivePortalState() {
        }

        public void enter() {
            NetworkMonitor.this.maybeLogEvaluationResult(NetworkMonitor.this.networkEventType(NetworkMonitor.this.validationStage(), EvaluationResult.CAPTIVE_PORTAL));
            if (!NetworkMonitor.this.mDontDisplaySigninNotification) {
                if (NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver == null) {
                    NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver = new CustomIntentReceiver(ACTION_LAUNCH_CAPTIVE_PORTAL_APP, new Random().nextInt(), NetworkMonitor.CMD_LAUNCH_CAPTIVE_PORTAL_APP);
                }
                NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION, 1, NetworkMonitor.this.mNetId, NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver.getPendingIntent()));
                NetworkMonitor.this.sendMessageDelayed(NetworkMonitor.CMD_CAPTIVE_PORTAL_RECHECK, 0, LocationFudger.FASTEST_INTERVAL_MS);
                NetworkMonitor networkMonitor = NetworkMonitor.this;
                networkMonitor.mValidations = networkMonitor.mValidations + 1;
            }
        }

        public void exit() {
            NetworkMonitor.this.removeMessages(NetworkMonitor.CMD_CAPTIVE_PORTAL_RECHECK);
        }
    }

    private class CustomIntentReceiver extends BroadcastReceiver {
        private final String mAction;
        private final int mToken;
        private final int mWhat;

        CustomIntentReceiver(String action, int token, int what) {
            this.mToken = token;
            this.mWhat = what;
            this.mAction = action + "_" + NetworkMonitor.this.mNetId + "_" + token;
            NetworkMonitor.this.mContext.registerReceiver(this, new IntentFilter(this.mAction));
        }

        public PendingIntent getPendingIntent() {
            Intent intent = new Intent(this.mAction);
            intent.setPackage(NetworkMonitor.this.mContext.getPackageName());
            return PendingIntent.getBroadcast(NetworkMonitor.this.mContext, 0, intent, 0);
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(this.mAction)) {
                NetworkMonitor.this.sendMessage(NetworkMonitor.this.obtainMessage(this.mWhat, this.mToken));
            }
        }
    }

    private class DefaultState extends State {
        private DefaultState() {
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_NETWORK_CONNECTED /*532481*/:
                    NetworkMonitor.this.logNetworkEvent(1);
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                    return true;
                case NetworkMonitor.CMD_NETWORK_DISCONNECTED /*532487*/:
                    NetworkMonitor.this.logNetworkEvent(7);
                    if (NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver != null) {
                        NetworkMonitor.this.mContext.unregisterReceiver(NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver);
                        NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver = null;
                    }
                    NetworkMonitor.this.releaseNetworkPropertyChecker();
                    NetworkMonitor.this.httpReachable = false;
                    NetworkMonitor.this.quit();
                    return true;
                case NetworkMonitor.CMD_FORCE_REEVALUATION /*532488*/:
                case NetworkMonitor.CMD_CAPTIVE_PORTAL_RECHECK /*532492*/:
                    NetworkMonitor.this.log("Forcing reevaluation for UID " + message.arg1);
                    NetworkMonitor.this.mUidResponsibleForReeval = message.arg1;
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                    return true;
                case NetworkMonitor.CMD_CAPTIVE_PORTAL_APP_FINISHED /*532489*/:
                    NetworkMonitor.this.log("CaptivePortal App responded with " + message.arg1);
                    NetworkMonitor.this.mUseHttps = false;
                    switch (message.arg1) {
                        case 0:
                            NetworkMonitor.this.sendMessage(NetworkMonitor.CMD_FORCE_REEVALUATION, 0, 0);
                            break;
                        case 1:
                            NetworkMonitor.this.mDontDisplaySigninNotification = true;
                            NetworkMonitor.this.mUserDoesNotWant = true;
                            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 1, NetworkMonitor.this.mNetId, null));
                            NetworkMonitor.this.mUidResponsibleForReeval = 0;
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                            break;
                        case 2:
                            NetworkMonitor.this.mDontDisplaySigninNotification = true;
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                            break;
                    }
                    return true;
                case AbsNetworkMonitor.CMD_NETWORK_ROAMING_CONNECTED /*532581*/:
                    NetworkMonitor.this.log("DefaultState receive CMD_NETWORK_ROAMING_CONNECTED");
                    NetworkMonitor.this.resetNetworkMonitor();
                    NetworkMonitor.this.httpReachable = false;
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mEvaluatingState);
                    return true;
                default:
                    return true;
            }
        }
    }

    private class EvaluatingState extends State {
        private int mAttempts;
        private int mReevaluateDelayMs;

        private EvaluatingState() {
        }

        public void enter() {
            if (!NetworkMonitor.this.mEvaluationTimer.isStarted()) {
                NetworkMonitor.this.mEvaluationTimer.start();
            }
            NetworkMonitor networkMonitor = NetworkMonitor.this;
            NetworkMonitor networkMonitor2 = NetworkMonitor.this;
            networkMonitor.sendMessage(NetworkMonitor.CMD_REEVALUATE, networkMonitor2.mReevaluateToken = networkMonitor2.mReevaluateToken + 1, 0);
            if (NetworkMonitor.this.mUidResponsibleForReeval != -1) {
                TrafficStats.setThreadStatsUid(NetworkMonitor.this.mUidResponsibleForReeval);
                NetworkMonitor.this.mUidResponsibleForReeval = -1;
            }
            this.mReevaluateDelayMs = 1000;
            this.mAttempts = 0;
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_REEVALUATE /*532486*/:
                    if (message.arg1 != NetworkMonitor.this.mReevaluateToken || NetworkMonitor.this.mUserDoesNotWant) {
                        return true;
                    }
                    if (NetworkMonitor.this.mDefaultRequest.networkCapabilities.satisfiedByNetworkCapabilities(NetworkMonitor.this.mNetworkAgentInfo.networkCapabilities)) {
                        this.mAttempts++;
                        if (NetworkMonitor.this.mNetworkAgentInfo.networkInfo.getType() == 0) {
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                            return true;
                        }
                        NetworkMonitor networkMonitor;
                        CaptivePortalProbeResult probeResult = new CaptivePortalProbeResult(599);
                        if (!NetworkMonitor.this.isWifiProEnabled() || !NetworkMonitor.this.mIsCaptivePortalCheckEnabled) {
                            probeResult = NetworkMonitor.this.isCaptivePortal(NetworkMonitor.getCaptivePortalServerHttpUrl(NetworkMonitor.this.mContext), NetworkMonitor.DEFAULT_SERV_PAGE);
                            if (probeResult.mHttpResponseCode < DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE || probeResult.mHttpResponseCode > 399) {
                                String operator = NetworkMonitor.this.mTelephonyManager.getNetworkOperator();
                                NetworkMonitor.this.log("IS_CHINA_AREA =" + NetworkMonitor.IS_CHINA_AREA + ", operator =" + operator);
                                if (!(operator == null || operator.length() == 0 || !operator.startsWith(NetworkMonitor.COUNTRY_CODE_CN)) || NetworkMonitor.IS_CHINA_AREA) {
                                    NetworkMonitor.this.log("NetworkMonitor isCaptivePortal transit to link baidu");
                                    probeResult = NetworkMonitor.this.isCaptivePortal(NetworkMonitor.BAKUP_SERVER, NetworkMonitor.BAKUP_SERV_PAGE);
                                    if (probeResult.mHttpResponseCode >= DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE && probeResult.mHttpResponseCode <= 399 && probeResult.mHttpResponseCode != 301 && probeResult.mHttpResponseCode != 302) {
                                        probeResult.mHttpResponseCode = 204;
                                    } else if (probeResult.mHttpResponseCode == 301 || probeResult.mHttpResponseCode == 302) {
                                        NetworkMonitor.this.log("mUrlHeadFieldLocation" + NetworkMonitor.this.mUrlHeadFieldLocation);
                                        String host = NetworkMonitor.this.parseHostByLocation(NetworkMonitor.this.mUrlHeadFieldLocation);
                                        if (host != null && host.contains(NetworkMonitor.SERVER_BAIDU)) {
                                            NetworkMonitor.this.log("host contains baidu ,change httpResponseCode to 204");
                                            probeResult.mHttpResponseCode = 204;
                                        }
                                    }
                                }
                            }
                        } else if (NetworkMonitor.this.isCheckCompletedByWifiPro() && NetworkMonitor.this.httpReachable) {
                            return true;
                        } else {
                            probeResult.mHttpResponseCode = NetworkMonitor.this.getRespCodeByWifiPro();
                            if (probeResult.mHttpResponseCode != 599) {
                                boolean z;
                                networkMonitor = NetworkMonitor.this;
                                if (probeResult.mHttpResponseCode != 204) {
                                    z = true;
                                } else {
                                    z = false;
                                }
                                networkMonitor.sendNetworkConditionsBroadcast(true, z, NetworkMonitor.this.getReqTimestamp(), NetworkMonitor.this.getRespTimestamp());
                            }
                        }
                        HwConnectivityManager hwConnectivityManager = HwServiceFactory.getHwConnectivityManager();
                        Context -get5 = NetworkMonitor.this.mContext;
                        boolean z2 = (probeResult.mHttpResponseCode == 204 || probeResult.mHttpResponseCode == 599) ? false : true;
                        hwConnectivityManager.captivePortalCheckCompleted(-get5, z2);
                        if (probeResult.isSuccessful()) {
                            NetworkMonitor.this.httpReachable = true;
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                        } else if (!probeResult.isPortal() || (NetworkMonitor.ADD_CRICKET_WIFI_MANAGER ^ 1) == 0) {
                            networkMonitor = NetworkMonitor.this;
                            NetworkMonitor networkMonitor2 = NetworkMonitor.this;
                            Message msg = networkMonitor.obtainMessage(NetworkMonitor.CMD_REEVALUATE, networkMonitor2.mReevaluateToken = networkMonitor2.mReevaluateToken + 1, 0);
                            if (!NetworkMonitor.this.isWifiProEnabled() || (NetworkMonitor.this.isCheckCompletedByWifiPro() ^ 1) == 0) {
                                NetworkMonitor.this.sendMessageDelayed(msg, (long) this.mReevaluateDelayMs);
                                NetworkMonitor.this.logNetworkEvent(3);
                                NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 1, NetworkMonitor.this.mNetId, probeResult.redirectUrl));
                                if (this.mAttempts >= 5) {
                                    TrafficStats.clearThreadStatsUid();
                                }
                                this.mReevaluateDelayMs *= 2;
                                if (this.mReevaluateDelayMs > ProcessList.PSS_ALL_INTERVAL) {
                                    this.mReevaluateDelayMs = ProcessList.PSS_ALL_INTERVAL;
                                }
                            } else {
                                this.mReevaluateDelayMs *= 2;
                                NetworkMonitor.this.sendMessageDelayed(msg, (long) NetworkMonitor.this.resetReevaluateDelayMs(this.mReevaluateDelayMs));
                                return true;
                            }
                        } else {
                            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 1, NetworkMonitor.this.mNetId, probeResult.redirectUrl));
                            NetworkMonitor.this.mLastPortalProbeResult = probeResult;
                            if (NetworkMonitor.this.isWifiProEnabled()) {
                                NetworkMonitor.this.reportPortalNetwork(NetworkMonitor.this.mConnectivityServiceHandler, NetworkMonitor.this.mNetId, probeResult.redirectUrl);
                                NetworkMonitor.this.httpReachable = true;
                                NetworkMonitor.this.transitionTo(NetworkMonitor.this.mCaptivePortalState);
                                return true;
                            }
                            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 1, NetworkMonitor.this.mNetId, probeResult.redirectUrl));
                            NetworkMonitor.this.transitionTo(NetworkMonitor.this.mCaptivePortalState);
                        }
                        return true;
                    }
                    NetworkMonitor.this.validationLog("Network would not satisfy default request, not validating");
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                    return true;
                case NetworkMonitor.CMD_FORCE_REEVALUATION /*532488*/:
                    return this.mAttempts < 5;
                default:
                    return false;
            }
        }

        public void exit() {
            TrafficStats.clearThreadStatsUid();
        }
    }

    enum EvaluationResult {
        VALIDATED(true),
        CAPTIVE_PORTAL(false);
        
        final boolean isValidated;

        private EvaluationResult(boolean isValidated) {
            this.isValidated = isValidated;
        }
    }

    private class MaybeNotifyState extends State {
        private MaybeNotifyState() {
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_LAUNCH_CAPTIVE_PORTAL_APP /*532491*/:
                    try {
                        HwServiceFactory.getHwConnectivityManager().startBrowserOnClickNotification(NetworkMonitor.this.mContext, new URL(NetworkMonitor.getCaptivePortalServerHttpUrl(NetworkMonitor.this.mContext)).toString());
                    } catch (MalformedURLException e) {
                        NetworkMonitor.this.log("MalformedURLException " + e);
                    }
                    return true;
                default:
                    return false;
            }
        }

        public void exit() {
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_PROVISIONING_NOTIFICATION, 0, NetworkMonitor.this.mNetId, null));
        }
    }

    private static class OneAddressPerFamilyNetwork extends Network {
        public OneAddressPerFamilyNetwork(Network network) {
            super(network);
        }

        public InetAddress[] getAllByName(String host) throws UnknownHostException {
            List<InetAddress> addrs = Arrays.asList(super.getAllByName(host));
            LinkedHashMap<Class, InetAddress> addressByFamily = new LinkedHashMap();
            addressByFamily.put(((InetAddress) addrs.get(0)).getClass(), (InetAddress) addrs.get(0));
            Collections.shuffle(addrs);
            for (InetAddress addr : addrs) {
                addressByFamily.put(addr.getClass(), addr);
            }
            return (InetAddress[]) addressByFamily.values().toArray(new InetAddress[addressByFamily.size()]);
        }
    }

    private class ValidatedState extends State {
        private ValidatedState() {
        }

        public void enter() {
            NetworkMonitor.this.maybeLogEvaluationResult(NetworkMonitor.this.networkEventType(NetworkMonitor.this.validationStage(), EvaluationResult.VALIDATED));
            NetworkMonitor.this.mConnectivityServiceHandler.sendMessage(NetworkMonitor.this.obtainMessage(NetworkMonitor.EVENT_NETWORK_TESTED, 0, NetworkMonitor.this.mNetId, null));
            NetworkMonitor networkMonitor = NetworkMonitor.this;
            networkMonitor.mValidations = networkMonitor.mValidations + 1;
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case NetworkMonitor.CMD_NETWORK_CONNECTED /*532481*/:
                    NetworkMonitor.this.transitionTo(NetworkMonitor.this.mValidatedState);
                    return true;
                default:
                    return false;
            }
        }
    }

    enum ValidationStage {
        FIRST_VALIDATION(true),
        REVALIDATION(false);
        
        final boolean isFirstValidation;

        private ValidationStage(boolean isFirstValidation) {
            this.isFirstValidation = isFirstValidation;
        }
    }

    protected com.android.server.connectivity.NetworkMonitor.CaptivePortalProbeResult sendHttpProbe(java.net.URL r21, int r22) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x015b in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r20 = this;
        r14 = 0;
        r5 = 599; // 0x257 float:8.4E-43 double:2.96E-321;
        r8 = 0;
        r15 = new android.net.util.Stopwatch;
        r15.<init>();
        r7 = r15.start();
        r15 = -190; // 0xffffffffffffff42 float:NaN double:NaN;
        r6 = android.net.TrafficStats.getAndSetThreadStatsTag(r15);
        r0 = r20;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r0.mNetwork;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r21;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.openConnection(r0);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r15;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = (java.net.HttpURLConnection) r0;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r14 = r0;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = 3;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r22;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        if (r0 != r15) goto L_0x00f3;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x0026:
        r15 = 1;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x0027:
        r14.setInstanceFollowRedirects(r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r14.setConnectTimeout(r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r14.setReadTimeout(r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = 0;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r14.setUseCaches(r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r20;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r0.mCaptivePortalUserAgent;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        if (r15 == 0) goto L_0x004a;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x003e:
        r15 = "User-Agent";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r20;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r0.mCaptivePortalUserAgent;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = r0;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r14.setRequestProperty(r15, r16);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x004a:
        r15 = r14.getRequestProperties();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r9 = r15.toString();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r10 = android.os.SystemClock.elapsedRealtime();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r5 = r14.getResponseCode();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = "location";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r8 = r14.getHeaderField(r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r12 = android.os.SystemClock.elapsedRealtime();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = "Location";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r14.getHeaderField(r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r20;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0.mUrlHeadFieldLocation = r15;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15.<init>();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = "time=";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = r12 - r10;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = "ms";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = " ret=";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r5);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = " request=";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r9);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = " headers=";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = r14.getHeaderFields();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r20;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r1 = r22;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r2 = r21;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0.validationLog(r1, r2, r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        if (r5 != r15) goto L_0x00d2;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x00bf:
        r15 = 3;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r22;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        if (r0 != r15) goto L_0x00f6;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x00c4:
        r15 = "PAC fetch 200 response interpreted as 204 response.";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r20;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r1 = r22;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r2 = r21;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0.validationLog(r1, r2, r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r5 = 204; // 0xcc float:2.86E-43 double:1.01E-321;
    L_0x00d2:
        if (r14 == 0) goto L_0x00d7;
    L_0x00d4:
        r14.disconnect();
    L_0x00d7:
        android.net.TrafficStats.setThreadStatsTag(r6);
    L_0x00da:
        r16 = r7.stop();
        r0 = r20;
        r1 = r16;
        r3 = r22;
        r0.logValidationProbe(r1, r3, r5);
        r15 = new com.android.server.connectivity.NetworkMonitor$CaptivePortalProbeResult;
        r16 = r21.toString();
        r0 = r16;
        r15.<init>(r5, r8, r0);
        return r15;
    L_0x00f3:
        r15 = 0;
        goto L_0x0027;
    L_0x00f6:
        r16 = r14.getContentLengthLong();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r18 = 0;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = (r16 > r18 ? 1 : (r16 == r18 ? 0 : -1));	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        if (r15 != 0) goto L_0x010f;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x0100:
        r15 = "200 response with Content-length=0 interpreted as 204 response.";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r20;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r1 = r22;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r2 = r21;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0.validationLog(r1, r2, r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r5 = 204; // 0xcc float:2.86E-43 double:1.01E-321;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        goto L_0x00d2;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x010f:
        r16 = r14.getContentLengthLong();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r18 = -1;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = (r16 > r18 ? 1 : (r16 == r18 ? 0 : -1));	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        if (r15 != 0) goto L_0x00d2;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x0119:
        r15 = r14.getInputStream();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.read();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = -1;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r16;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        if (r15 != r0) goto L_0x00d2;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
    L_0x0127:
        r15 = "Empty 200 response interpreted as 204 response.";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r20;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r1 = r22;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r2 = r21;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0.validationLog(r1, r2, r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r5 = 204; // 0xcc float:2.86E-43 double:1.01E-321;
        goto L_0x00d2;
    L_0x0136:
        r4 = move-exception;
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15.<init>();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r16 = "Probe failed with exception ";	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.append(r4);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0 = r20;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r1 = r22;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r2 = r21;	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r0.validationLog(r1, r2, r15);	 Catch:{ IOException -> 0x0136, all -> 0x0160 }
        r15 = 599; // 0x257 float:8.4E-43 double:2.96E-321;
        if (r14 == 0) goto L_0x015b;
    L_0x0158:
        r14.disconnect();
    L_0x015b:
        android.net.TrafficStats.setThreadStatsTag(r6);
        goto L_0x00da;
    L_0x0160:
        r15 = move-exception;
        if (r14 == 0) goto L_0x0166;
    L_0x0163:
        r14.disconnect();
    L_0x0166:
        android.net.TrafficStats.setThreadStatsTag(r6);
        throw r15;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.connectivity.NetworkMonitor.sendHttpProbe(java.net.URL, int):com.android.server.connectivity.NetworkMonitor$CaptivePortalProbeResult");
    }

    public NetworkMonitor(Context context, Handler handler, NetworkAgentInfo networkAgentInfo, NetworkRequest defaultRequest) {
        this(context, handler, networkAgentInfo, defaultRequest, new IpConnectivityLog());
    }

    protected NetworkMonitor(Context context, Handler handler, NetworkAgentInfo networkAgentInfo, NetworkRequest defaultRequest, IpConnectivityLog logger) {
        boolean z = true;
        super(TAG + networkAgentInfo.name());
        this.mReevaluateToken = 0;
        this.mUidResponsibleForReeval = -1;
        this.mValidations = 0;
        this.httpReachable = false;
        this.mUserDoesNotWant = false;
        this.mDontDisplaySigninNotification = false;
        this.systemReady = false;
        this.mDefaultState = new DefaultState();
        this.mValidatedState = new ValidatedState();
        this.mMaybeNotifyState = new MaybeNotifyState();
        this.mEvaluatingState = new EvaluatingState();
        this.mCaptivePortalState = new CaptivePortalState();
        this.mLaunchCaptivePortalAppBroadcastReceiver = null;
        this.validationLogs = new LocalLog(20);
        this.mUrlHeadFieldLocation = null;
        this.mEvaluationTimer = new Stopwatch();
        this.mLastPortalProbeResult = CaptivePortalProbeResult.FAILED;
        this.mNextFallbackUrlIndex = 0;
        this.mContext = context;
        this.mMetricsLog = logger;
        this.mConnectivityServiceHandler = handler;
        this.mNetworkAgentInfo = networkAgentInfo;
        this.mNetwork = new OneAddressPerFamilyNetwork(networkAgentInfo.network);
        this.mNetId = this.mNetwork.netId;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mDefaultRequest = defaultRequest;
        addState(this.mDefaultState);
        addState(this.mValidatedState, this.mDefaultState);
        addState(this.mMaybeNotifyState, this.mDefaultState);
        addState(this.mEvaluatingState, this.mMaybeNotifyState);
        addState(this.mCaptivePortalState, this.mMaybeNotifyState);
        setInitialState(this.mDefaultState);
        this.mIsCaptivePortalCheckEnabled = Global.getInt(this.mContext.getContentResolver(), "captive_portal_mode", 1) != 0;
        if (Global.getInt(this.mContext.getContentResolver(), "captive_portal_use_https", 1) != 1) {
            z = false;
        }
        this.mUseHttps = z;
        this.mCaptivePortalUserAgent = getCaptivePortalUserAgent(context);
        this.mCaptivePortalHttpsUrl = makeURL(getCaptivePortalServerHttpsUrl(context));
        this.mCaptivePortalHttpUrl = makeURL(getCaptivePortalServerHttpUrl(context));
        this.mCaptivePortalFallbackUrls = makeCaptivePortalFallbackUrls(context);
        start();
    }

    protected void log(String s) {
        Log.d(TAG + BAKUP_SERV_PAGE + this.mNetworkAgentInfo.name(), s);
    }

    private void validationLog(int probeType, Object url, String msg) {
        validationLog(String.format("%s %s %s", new Object[]{ValidationProbeEvent.getProbeName(probeType), url, msg}));
    }

    private void validationLog(String s) {
        log(s);
        this.validationLogs.log(s);
    }

    public ReadOnlyLocalLog getValidationLogs() {
        return this.validationLogs.readOnlyLocalLog();
    }

    private ValidationStage validationStage() {
        return this.mValidations == 0 ? ValidationStage.FIRST_VALIDATION : ValidationStage.REVALIDATION;
    }

    private static String getCaptivePortalServerHttpsUrl(Context context) {
        return getSetting(context, "captive_portal_https_url", DEFAULT_HTTPS_URL);
    }

    public static String getCaptivePortalServerHttpUrl(Context context) {
        return getSetting(context, "captive_portal_http_url", DEFAULT_HTTP_URL);
    }

    private URL[] makeCaptivePortalFallbackUrls(Context context) {
        String separator = ",";
        String joinedUrls = getSetting(context, "captive_portal_fallback_url", DEFAULT_FALLBACK_URL) + separator + getSetting(context, "captive_portal_other_fallback_urls", DEFAULT_OTHER_FALLBACK_URLS);
        List<URL> urls = new ArrayList();
        for (String s : joinedUrls.split(separator)) {
            URL u = makeURL(s);
            if (u != null) {
                urls.add(u);
            }
        }
        if (urls.isEmpty()) {
            Log.e(TAG, String.format("could not create any url from %s", new Object[]{joinedUrls}));
        }
        return (URL[]) urls.toArray(new URL[urls.size()]);
    }

    private static String getCaptivePortalUserAgent(Context context) {
        return getSetting(context, "captive_portal_user_agent", DEFAULT_USER_AGENT);
    }

    private static String getSetting(Context context, String symbol, String defaultValue) {
        String value = Global.getString(context.getContentResolver(), symbol);
        return value != null ? value : defaultValue;
    }

    private URL nextFallbackUrl() {
        if (this.mCaptivePortalFallbackUrls.length == 0) {
            return null;
        }
        int idx = Math.abs(this.mNextFallbackUrlIndex) % this.mCaptivePortalFallbackUrls.length;
        this.mNextFallbackUrlIndex += new Random().nextInt();
        return this.mCaptivePortalFallbackUrls[idx];
    }

    protected CaptivePortalProbeResult isCaptivePortal(String urlString) {
        if (this.mIsCaptivePortalCheckEnabled) {
            URL url = null;
            URL httpsUrl = this.mCaptivePortalHttpsUrl;
            URL httpUrl = this.mCaptivePortalHttpUrl;
            if (urlString != null && urlString.contains(BAKUP_SERVER)) {
                try {
                    URL httpUrl2 = new URL(urlString);
                    try {
                        httpsUrl = new URL(urlString);
                        httpUrl = httpUrl2;
                    } catch (MalformedURLException e) {
                        httpUrl = httpUrl2;
                        validationLog("Bad validation URL");
                        return CaptivePortalProbeResult.FAILED;
                    }
                } catch (MalformedURLException e2) {
                    validationLog("Bad validation URL");
                    return CaptivePortalProbeResult.FAILED;
                }
            }
            ProxyInfo proxyInfo = this.mNetworkAgentInfo.linkProperties.getHttpProxy();
            if (!(proxyInfo == null || (Uri.EMPTY.equals(proxyInfo.getPacFileUrl()) ^ 1) == 0)) {
                url = makeURL(proxyInfo.getPacFileUrl().toString());
                if (url == null) {
                    return CaptivePortalProbeResult.FAILED;
                }
            }
            if (url == null && (httpUrl == null || httpsUrl == null)) {
                return CaptivePortalProbeResult.FAILED;
            }
            CaptivePortalProbeResult result;
            long startTime = SystemClock.elapsedRealtime();
            if (url != null) {
                result = sendDnsAndHttpProbes(null, url, 3);
            } else if (this.mUseHttps) {
                result = sendParallelHttpProbes(proxyInfo, httpsUrl, httpUrl);
            } else {
                result = sendDnsAndHttpProbes(proxyInfo, httpUrl, 1);
            }
            sendNetworkConditionsBroadcast(true, result.isPortal(), startTime, SystemClock.elapsedRealtime());
            return result;
        }
        validationLog("Validation disabled.");
        return CaptivePortalProbeResult.SUCCESS;
    }

    protected CaptivePortalProbeResult isCaptivePortal() {
        return isCaptivePortal(getCaptivePortalServerHttpsUrl(this.mContext));
    }

    protected CaptivePortalProbeResult isCaptivePortal(String server_url, String page) {
        if (!(!server_url.startsWith("http://") ? server_url.startsWith("https://") : true)) {
            server_url = "http://" + server_url;
        }
        if (!server_url.endsWith(page)) {
            server_url = server_url + page;
        }
        return isCaptivePortal(server_url);
    }

    private CaptivePortalProbeResult sendDnsAndHttpProbes(ProxyInfo proxy, URL url, int probeType) {
        sendDnsProbe(proxy != null ? proxy.getHost() : url.getHost());
        return sendHttpProbe(url, probeType);
    }

    private void sendDnsProbe(String host) {
        if (!TextUtils.isEmpty(host)) {
            int result;
            String connectInfo;
            String name = ValidationProbeEvent.getProbeName(0);
            Stopwatch watch = new Stopwatch().start();
            try {
                InetAddress[] addresses = this.mNetwork.getAllByName(host);
                StringBuffer buffer = new StringBuffer();
                for (InetAddress address : addresses) {
                    buffer.append(',').append(address.getHostAddress());
                }
                result = 1;
                connectInfo = "OK " + buffer.substring(1);
            } catch (UnknownHostException e) {
                result = 0;
                connectInfo = "FAIL";
            }
            validationLog(0, host, String.format("%dms %s", new Object[]{Long.valueOf(watch.stop()), connectInfo}));
            logValidationProbe(latency, 0, result);
        }
    }

    private CaptivePortalProbeResult sendParallelHttpProbes(ProxyInfo proxy, URL httpsUrl, URL httpUrl) {
        CountDownLatch latch = new CountDownLatch(2);
        AnonymousClass1ProbeThread httpsProbe = new AnonymousClass1ProbeThread(true, proxy, httpsUrl, httpUrl, latch);
        AnonymousClass1ProbeThread httpProbe = new AnonymousClass1ProbeThread(false, proxy, httpsUrl, httpUrl, latch);
        try {
            httpsProbe.start();
            httpProbe.start();
            latch.await(3000, TimeUnit.MILLISECONDS);
            CaptivePortalProbeResult httpsResult = httpsProbe.result();
            CaptivePortalProbeResult httpResult = httpProbe.result();
            if (httpResult.isPortal()) {
                return httpResult;
            }
            if (httpsResult.isPortal() || httpsResult.isSuccessful()) {
                return httpsResult;
            }
            URL fallbackUrl = nextFallbackUrl();
            if (fallbackUrl != null) {
                CaptivePortalProbeResult result = sendHttpProbe(fallbackUrl, 4);
                if (result.isPortal()) {
                    return result;
                }
            }
            try {
                httpProbe.join();
                if (httpProbe.result().isPortal()) {
                    return httpProbe.result();
                }
                httpsProbe.join();
                return httpsProbe.result();
            } catch (InterruptedException e) {
                validationLog("Error: http or https probe wait interrupted!");
                return CaptivePortalProbeResult.FAILED;
            }
        } catch (InterruptedException e2) {
            validationLog("Error: probes wait interrupted!");
            return CaptivePortalProbeResult.FAILED;
        }
    }

    private URL makeURL(String url) {
        if (url != null) {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                validationLog("Bad URL: " + url);
            }
        }
        return null;
    }

    private void sendNetworkConditionsBroadcast(boolean responseReceived, boolean isCaptivePortal, long requestTimestampMs, long responseTimestampMs) {
        if (Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) != 0 && this.systemReady) {
            Intent latencyBroadcast = new Intent(ACTION_NETWORK_CONDITIONS_MEASURED);
            switch (this.mNetworkAgentInfo.networkInfo.getType()) {
                case 0:
                    latencyBroadcast.putExtra(EXTRA_NETWORK_TYPE, this.mTelephonyManager.getNetworkType());
                    List<CellInfo> info = this.mTelephonyManager.getAllCellInfo();
                    if (info != null) {
                        int numRegisteredCellInfo = 0;
                        for (CellInfo cellInfo : info) {
                            if (cellInfo.isRegistered()) {
                                numRegisteredCellInfo++;
                                if (numRegisteredCellInfo <= 1) {
                                    if (cellInfo instanceof CellInfoCdma) {
                                        latencyBroadcast.putExtra(EXTRA_CELL_ID, ((CellInfoCdma) cellInfo).getCellIdentity());
                                    } else if (cellInfo instanceof CellInfoGsm) {
                                        latencyBroadcast.putExtra(EXTRA_CELL_ID, ((CellInfoGsm) cellInfo).getCellIdentity());
                                    } else if (cellInfo instanceof CellInfoLte) {
                                        latencyBroadcast.putExtra(EXTRA_CELL_ID, ((CellInfoLte) cellInfo).getCellIdentity());
                                    } else if (cellInfo instanceof CellInfoWcdma) {
                                        latencyBroadcast.putExtra(EXTRA_CELL_ID, ((CellInfoWcdma) cellInfo).getCellIdentity());
                                    } else {
                                        return;
                                    }
                                }
                                return;
                            }
                        }
                        break;
                    }
                    return;
                case 1:
                    WifiInfo currentWifiInfo = this.mWifiManager.getConnectionInfo();
                    if (currentWifiInfo != null) {
                        latencyBroadcast.putExtra(EXTRA_SSID, currentWifiInfo.getSSID());
                        latencyBroadcast.putExtra(EXTRA_BSSID, currentWifiInfo.getBSSID());
                        break;
                    }
                    return;
                default:
                    return;
            }
            latencyBroadcast.putExtra(EXTRA_CONNECTIVITY_TYPE, this.mNetworkAgentInfo.networkInfo.getType());
            latencyBroadcast.putExtra(EXTRA_RESPONSE_RECEIVED, responseReceived);
            latencyBroadcast.putExtra(EXTRA_REQUEST_TIMESTAMP_MS, requestTimestampMs);
            if (responseReceived) {
                latencyBroadcast.putExtra(EXTRA_IS_CAPTIVE_PORTAL, isCaptivePortal);
                latencyBroadcast.putExtra(EXTRA_RESPONSE_TIMESTAMP_MS, responseTimestampMs);
            }
            this.mContext.sendBroadcastAsUser(latencyBroadcast, UserHandle.CURRENT, PERMISSION_ACCESS_NETWORK_CONDITIONS);
        }
    }

    private void logNetworkEvent(int evtype) {
        this.mMetricsLog.log(new NetworkEvent(this.mNetId, evtype));
    }

    private int networkEventType(ValidationStage s, EvaluationResult r) {
        if (s.isFirstValidation) {
            if (r.isValidated) {
                return 8;
            }
            return 10;
        } else if (r.isValidated) {
            return 9;
        } else {
            return 11;
        }
    }

    private void maybeLogEvaluationResult(int evtype) {
        if (this.mEvaluationTimer.isRunning()) {
            this.mMetricsLog.log(new NetworkEvent(this.mNetId, evtype, this.mEvaluationTimer.stop()));
            this.mEvaluationTimer.reset();
        }
    }

    private void logValidationProbe(long durationMs, int probeType, int probeResult) {
        int[] transports = this.mNetworkAgentInfo.networkCapabilities.getTransportTypes();
        boolean isFirstValidation = validationStage().isFirstValidation;
        ValidationProbeEvent ev = new ValidationProbeEvent();
        ev.probeType = ValidationProbeEvent.makeProbeType(probeType, isFirstValidation);
        ev.returnCode = probeResult;
        ev.durationMs = durationMs;
        this.mMetricsLog.log(this.mNetId, transports, ev);
    }

    private String parseHostByLocation(String location) {
        if (location != null) {
            int start = 0;
            if (location.startsWith("http://")) {
                start = 7;
            } else if (location.startsWith("https://")) {
                start = 8;
            }
            int end = location.indexOf(BAKUP_SERV_PAGE, start);
            if (end == -1) {
                end = location.length();
            }
            if (start <= end && end <= location.length()) {
                return location.substring(start, end);
            }
        }
        return null;
    }
}
