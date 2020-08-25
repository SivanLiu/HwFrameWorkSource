package com.huawei.hwwifiproservice;

import android.app.KeyguardManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.SettingsEx;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.HwPortalExceptionManager;
import com.android.server.wifi.HwQoE.HwQoEUtils;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifi.hwUtil.WifiCommonUtils;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.android.app.ActivityManagerEx;
import com.huawei.android.app.IHwActivityNotifierEx;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class HwAutoConnectManager {
    private static final int ADJUST_RSSI_GOOD_PERIOD_COUNT = 8;
    private static final int AUTO_JOIN_DISABLED_NETWORK_THRESHOLD = 2;
    /* access modifiers changed from: private */
    public static final String[] BROWSERS_PRE_INSTALLED = {HwPortalExceptionManager.BROWSER_PACKET_NAME, "com.android.browser", "com.android.chrome"};
    private static final int CHECK_NETWORK_RSSI_LEVEL = -80;
    public static final int CMD_UPDATE_WIFIPRO_CONFIGURATIONS = 131672;
    private static final String COUNTRY_CODE_CN = "460";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36";
    private static final long DELAYED_TIME_HTTP_RECHECK = 10000;
    private static final long DELAYED_TIME_HTTP_RECHECK_FAST = 2000;
    private static final long DELAYED_TIME_LAUNCH_BROWSER = 500;
    private static final long DELAYED_TIME_RETRY_LAUNCH_BROWSER = 500;
    private static final long DELAYED_TIME_STATUS_BAR = 500;
    private static final long DELAYED_TIME_SWITCH_WIFI = 2000;
    private static final long DELAYED_TIME_WIFI_ICON = 200;
    private static final long DELAY_DURATION = 5000;
    public static final int GOOD_LINK_DETECTED = 131874;
    private static final int INITIAL_CNT = -1;
    private static final String KEYWORD_MESSAGE_ARG = "messageArg";
    private static final String KEYWORD_MESSAGE_OBJ = "messageObj";
    private static final String KEYWORD_MESSAGE_WHAT = "messageWhat";
    public static final String KEY_HUAWEI_EMPLOYEE = "\"Huawei-Employee\"WPA_EAP";
    public static final String KEY_HUAWEI_EMPLOYEE_FT = "\"Huawei-Employee\"FT_EAP";
    private static final int LAUNCH_BROWSER_MAX_RETRY_COUNT = 4;
    private static final int MAX_PORTAL_HTTP_TIMES = 3;
    private static final int MAX_START_BROWSER_TIME = 120;
    private static final int MSG_BLACKLIST_BSSID_TIMEOUT = 118;
    private static final int MSG_CHECK_PORTAL_AUTH_RESULT = 124;
    private static final int MSG_CHECK_PORTAL_NETWORK = 101;
    private static final int MSG_DISCONNECT_NETWORK = 102;
    private static final int MSG_ENABLE_SAME_NETWORK_ID = 115;
    private static final int MSG_HTTP_RECHECK = 112;
    private static final int MSG_LAUNCH_BROWSER = 111;
    private static final int MSG_NO_INTERNET_RECOVERY_CHECK = 104;
    private static final int MSG_PORTAL_BROWSER_LAUNCHED = 122;
    private static final int MSG_PORTAL_BROWSER_LAUNCHED_TIMEOUT = 121;
    private static final int MSG_PORTAL_CANCELED = 108;
    private static final int MSG_PORTAL_NETWORK_CONNECTED = 120;
    private static final int MSG_PORTAL_OUT_OF_RANGE = 109;
    private static final int MSG_PORTAL_SELECTED = 107;
    private static final int MSG_PORTAL_STATUS_BAR = 106;
    private static final int MSG_RECV_NETWORK_CONNECTED = 116;
    private static final int MSG_RECV_NETWORK_DISCONNECTED = 103;
    private static final int MSG_REQUEST_CHECK_PORTAL_AUTH = 123;
    private static final int MSG_RSSI_CHANGED_PORTAL_NETWORK = 114;
    private static final int MSG_SWITCH_WIFI_FOREGROUND = 113;
    private static final int MSG_USER_ENTER_WLAN_SETTINGS = 119;
    private static final int MSG_WIFI_CLOSED = 110;
    private static final int MSG_WIFI_DISABLED_RCVD = 117;
    private static final int NO_INTERNET_CODE = 599;
    public static final int POOR_LINK_DETECTED = 131873;
    private static final int POOR_LINK_RSSI_THRESHOLD = -75;
    private static final int POP_UP_WIFI_RSSI = -200;
    private static final String PORTAL_STATUS_BAR_TAG = "wifipro_portal_status_bar";
    private static final int START_BROWSER_TIMEOUT = -100;
    private static final String TAG = "HwAutoConnectManager";
    private static final String WIFI_SETTINGS_ACTIVITY_DEFAULT = "com.android.settings.Settings$WifiSettingsActivity";
    private static final String WIFI_SETTINGS_ACTIVITY_TV = "com.huawei.homevision.settings.network.NetworkActivity";
    private static HwAutoConnectManager mHwAutoConnectManager = null;
    private IHwActivityNotifierEx mActivityNotifierEx = new IHwActivityNotifierEx() {
        /* class com.huawei.hwwifiproservice.HwAutoConnectManager.AnonymousClass3 */

        public void call(Bundle extras) {
            HwQoeQualityMonitor hwQoeQualityMonitor;
            if (extras == null) {
                HwAutoConnectManager.this.logE("extras == null");
                return;
            }
            Object tempComp = extras.getParcelable("comp");
            ComponentName componentName = null;
            if (tempComp instanceof ComponentName) {
                componentName = (ComponentName) tempComp;
            }
            int uid = extras.getInt("uid");
            if ("onResume".equals(extras.getString("state")) && componentName != null && uid != -1) {
                if ("com.android.settings.Settings$WifiSettingsActivity".equals(componentName.getClassName()) || (WifiCommonUtils.IS_TV && HwAutoConnectManager.WIFI_SETTINGS_ACTIVITY_TV.equals(componentName.getClassName()))) {
                    HwAutoConnectManager.this.mHandler.sendMessage(HwAutoConnectManager.this.mHandler.obtainMessage(119));
                    if (HwSelfCureEngine.getInstance() != null) {
                        HwSelfCureEngine.getInstance().notifyUserEnterWlanSettings();
                    }
                }
                if (WifiProCommonUtils.isInMonitorList(componentName.getPackageName(), HwAutoConnectManager.BROWSERS_PRE_INSTALLED)) {
                    HwAutoConnectManager.this.mHandler.sendMessage(HwAutoConnectManager.this.mHandler.obtainMessage(122));
                }
                int oldUid = HwAutoConnectManager.this.mCurrentTopUid;
                String oldPackageName = HwAutoConnectManager.this.mCurrentPackageName;
                synchronized (HwAutoConnectManager.this.mCurrentTopUidLock) {
                    int unused = HwAutoConnectManager.this.mCurrentTopUid = uid;
                    String unused2 = HwAutoConnectManager.this.mCurrentPackageName = componentName.getPackageName();
                }
                if (HwAutoConnectManager.this.mCurrentPackageName != null && !HwAutoConnectManager.this.mCurrentPackageName.equals(oldPackageName) && HwAutoConnectManager.this.mCurrentTopUid != oldUid && (hwQoeQualityMonitor = HwQoeQualityMonitor.getInstance()) != null) {
                    hwQoeQualityMonitor.notifyAppChanged();
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public final Object mAutoConnectFilterLock = new Object();
    /* access modifiers changed from: private */
    public ArrayList<String> mAutoJoinBlacklistBssid = new ArrayList<>();
    /* access modifiers changed from: private */
    public int mAutoJoinDisabledNetworkCnt = 0;
    /* access modifiers changed from: private */
    public AtomicBoolean mBackGroundRunning = new AtomicBoolean(false);
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private String mCurrentAutoJoinTargetBssid = null;
    /* access modifiers changed from: private */
    public String mCurrentBlacklistConfigKey = null;
    private WifiConfiguration mCurrentCheckWifiConfig = null;
    /* access modifiers changed from: private */
    public String mCurrentPackageName = "";
    /* access modifiers changed from: private */
    public int mCurrentTopUid = -1;
    /* access modifiers changed from: private */
    public final Object mCurrentTopUidLock = new Object();
    private boolean mFirstDetected = false;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public HwNetworkPropertyChecker mHwNetworkPropertychecker = null;
    private IntentFilter mIntentFilter;
    private KeyguardManager mKeyguardManager;
    /* access modifiers changed from: private */
    public boolean mLaaDiabledRequest = false;
    private int mLaunchBrowserRetryCount = 0;
    /* access modifiers changed from: private */
    public final Object mNetworkCheckLock = new Object();
    private long mNetworkConnectedTime = -1;
    private WifiConfiguration mPopUpNotifyWifiConfig = null;
    private int mPopUpWifiRssi = -200;
    private Notification.Builder mPortalBuilder = null;
    private final Object mPortalDatabaseLock = new Object();
    private int mPortalNotificationId = -1;
    private String mPortalRedirectedUrl = null;
    private int mPortalRespCode = 599;
    private Map<String, ArrayList<String>> mPortalUnauthDatabase = new HashMap();
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    private WifiProUIDisplayManager mWifiProUIDisplayManager;

    public HwAutoConnectManager(Context context, NetworkQosMonitor networkQosMonitor) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mKeyguardManager = (KeyguardManager) context.getSystemService("keyguard");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (networkQosMonitor != null) {
            this.mHwNetworkPropertychecker = networkQosMonitor.getNetworkPropertyChecker();
            this.mWifiProUIDisplayManager = networkQosMonitor.getWifiProUIDisplayManager();
        } else {
            this.mWifiProUIDisplayManager = WifiProUIDisplayManager.createInstance(context, null);
            this.mHwNetworkPropertychecker = new HwNetworkPropertyChecker(this.mContext, null, null, true, null, false);
        }
        Log.d(TAG, "HwAutoConnectManager init Complete! ");
    }

    public static synchronized HwAutoConnectManager getInstance(Context context, NetworkQosMonitor networkQosMonitor) {
        HwAutoConnectManager hwAutoConnectManager;
        synchronized (HwAutoConnectManager.class) {
            if (mHwAutoConnectManager == null) {
                mHwAutoConnectManager = new HwAutoConnectManager(context, networkQosMonitor);
            }
            hwAutoConnectManager = mHwAutoConnectManager;
        }
        return hwAutoConnectManager;
    }

    public static synchronized HwAutoConnectManager getInstance() {
        HwAutoConnectManager hwAutoConnectManager;
        synchronized (HwAutoConnectManager.class) {
            hwAutoConnectManager = mHwAutoConnectManager;
        }
        return hwAutoConnectManager;
    }

    public void init(Looper looperIn) {
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("com.huawei.wifipro.action.ACTION_NOTIFY_PORTAL_CONNECTED_BACKGROUND");
        this.mIntentFilter.addAction("com.huawei.wifipro.action.ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND");
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_PORTAL_USED_BY_USER);
        this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_PORTAL_CANCELED_BY_USER);
        this.mIntentFilter.addAction("com.huawei.wifipro.action.ACTION_NOTIFY_PORTAL_OUT_OF_RANGE");
        this.mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        Looper looper = looperIn;
        if (looper == null) {
            HandlerThread handlerThread = new HandlerThread("wifipro_auto_conn_manager_handler_thread");
            handlerThread.start();
            looper = handlerThread.getLooper();
        }
        this.mHandler = new Handler(looper) {
            /* class com.huawei.hwwifiproservice.HwAutoConnectManager.AnonymousClass1 */

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 101:
                        if (HwAutoConnectManager.this.mBackGroundRunning.get()) {
                            HwAutoConnectManager.this.handleRespCodeForPortalCheck(msg.arg1, msg.arg2);
                            break;
                        }
                        break;
                    case 102:
                        HwAutoConnectManager.this.handleMsgDisconnectNetwork(msg);
                        break;
                    case 103:
                        HwAutoConnectManager.this.handleMsgRecvNetworkDisconnected(msg);
                        break;
                    case 104:
                        if (HwAutoConnectManager.this.mBackGroundRunning.get()) {
                            HwAutoConnectManager.this.handleRespCodeForNoInternetCheck(msg.arg1, msg.arg2);
                            break;
                        }
                        break;
                    case 106:
                        HwAutoConnectManager hwAutoConnectManager = HwAutoConnectManager.this;
                        hwAutoConnectManager.logI("MSG_PORTAL_STATUS_BAR, update = " + msg.obj);
                        HwAutoConnectManager.this.showPortalStatusBar(((Boolean) msg.obj).booleanValue());
                        break;
                    case 107:
                        HwAutoConnectManager.this.handleMsgPortalSelected(msg);
                        break;
                    case 108:
                        HwAutoConnectManager.this.handleMsgPortalCanceled(msg);
                        break;
                    case 109:
                        HwAutoConnectManager.this.handlePortalOutOfRange();
                        break;
                    case 111:
                        HwAutoConnectManager.this.handleMsgLaunchBrowser(msg);
                        break;
                    case 112:
                        HwAutoConnectManager.this.handleMsgHttpRecheck(msg);
                        break;
                    case 113:
                        HwAutoConnectManager.this.notifyGoodLinkDetected();
                        break;
                    case 114:
                        HwAutoConnectManager.this.handleRssiChangedPortalNetwork(msg);
                        break;
                    case 115:
                        HwAutoConnectManager hwAutoConnectManager2 = HwAutoConnectManager.this;
                        hwAutoConnectManager2.logI("###MSG_ENABLE_SAME_NETWORK_ID, nid = " + msg.arg1);
                        if (HwAutoConnectManager.this.mBackGroundRunning.get()) {
                            HwAutoConnectManager.this.switchWifiForeground();
                        }
                        HwAutoConnectManager.this.mBackGroundRunning.set(false);
                        break;
                    case 116:
                        HwAutoConnectManager.this.handleMsgRecvNetworkConnected(msg);
                        break;
                    case 117:
                        HwAutoConnectManager.this.handleMsgWifiDisabledRcvd(msg);
                        break;
                    case 118:
                        HwAutoConnectManager.this.logI("###MSG_BLACKLIST_BSSID_TIMEOUT");
                        synchronized (HwAutoConnectManager.this.mAutoConnectFilterLock) {
                            String unused = HwAutoConnectManager.this.mCurrentBlacklistConfigKey = null;
                            HwAutoConnectManager.this.mAutoJoinBlacklistBssid.clear();
                            int unused2 = HwAutoConnectManager.this.mAutoJoinDisabledNetworkCnt = 0;
                        }
                        break;
                    case 119:
                        HwAutoConnectManager.this.handleMsgUserEnterWlanSettings(msg);
                        break;
                    case HwQoEUtils.QOE_MSG_CAMERA_ON:
                        HwAutoConnectManager.this.handlePortalNetworkConnected(msg);
                        break;
                    case 121:
                        HwAutoConnectManager.this.logI("###MSG_PORTAL_BROWSER_LAUNCHED_TIMEOUT");
                        HwAutoConnectManager.this.handleBrowserLaunchedTimeout(-100);
                        break;
                    case 122:
                        HwAutoConnectManager.this.handlePortalBrowserLaunched(msg);
                        break;
                    case 123:
                        new NetworkCheckThread(124, 1).start();
                        break;
                    case 124:
                        if (WifiProStateMachine.getWifiProStateMachineImpl() != null) {
                            WifiProStateMachine.getWifiProStateMachineImpl().onPortalAuthCheckResult(msg.arg2);
                            break;
                        }
                        break;
                }
                super.handleMessage(msg);
            }
        };
        registerUserBroadcastReceiver();
    }

    /* access modifiers changed from: private */
    public void handleMsgUserEnterWlanSettings(Message msg) {
        logI("###MSG_USER_ENTER_WLAN_SETTINGS");
        if (this.mBackGroundRunning.get()) {
            Handler handler = this.mHandler;
            handler.sendMessage(Message.obtain(handler, 102));
        }
        this.mHandler.removeMessages(118);
        synchronized (this.mAutoConnectFilterLock) {
            this.mCurrentBlacklistConfigKey = null;
            this.mAutoJoinBlacklistBssid.clear();
            this.mAutoJoinDisabledNetworkCnt = 0;
        }
    }

    /* access modifiers changed from: private */
    public void handleMsgWifiDisabledRcvd(Message msg) {
        synchronized (this.mAutoConnectFilterLock) {
            this.mCurrentBlacklistConfigKey = null;
            this.mCurrentAutoJoinTargetBssid = null;
            this.mAutoJoinDisabledNetworkCnt = 0;
            this.mAutoJoinBlacklistBssid.clear();
        }
        this.mHandler.removeMessages(118);
        handlePortalOutOfRange();
    }

    /* access modifiers changed from: private */
    public void handleMsgDisconnectNetwork(Message msg) {
        if (WifiProCommonUtils.isWifiConnected(this.mWifiManager)) {
            logW("MSG_DISCONNECT_NETWORK msg handled");
            this.mWifiManager.disconnect();
        }
        this.mBackGroundRunning.set(false);
        this.mCurrentCheckWifiConfig = null;
        this.mPortalRedirectedUrl = null;
        this.mPortalRespCode = 599;
        this.mFirstDetected = false;
        if (HwSelfCureEngine.getInstance() != null) {
            HwSelfCureEngine.getInstance().notifyWifiDisconnected();
        }
        this.mHandler.removeMessages(112);
    }

    /* access modifiers changed from: private */
    public void handleMsgRecvNetworkConnected(Message msg) {
        logI("##MSG_RECV_NETWORK_CONNECTED");
        WifiConfiguration current = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        synchronized (this.mAutoConnectFilterLock) {
            if (current != null) {
                if (current.configKey() != null && current.configKey().equals(this.mCurrentBlacklistConfigKey) && !current.getNetworkSelectionStatus().isNetworkEnabled()) {
                    logI("##enableNetwork currentBlacklistConfigKey networkId = " + current.networkId);
                    this.mWifiManager.enableNetwork(current.networkId, false);
                }
            }
            this.mCurrentAutoJoinTargetBssid = null;
            this.mCurrentBlacklistConfigKey = null;
            this.mAutoJoinBlacklistBssid.clear();
            this.mAutoJoinDisabledNetworkCnt = 0;
        }
        this.mHandler.removeMessages(118);
        this.mNetworkConnectedTime = System.currentTimeMillis();
        handlePortalOutOfRange();
    }

    /* access modifiers changed from: private */
    public void handleMsgRecvNetworkDisconnected(Message msg) {
        logI("MSG_RECV_NETWORK_DISCONNECTED");
        this.mBackGroundRunning.set(false);
        this.mCurrentCheckWifiConfig = null;
        this.mPortalRedirectedUrl = null;
        this.mPortalRespCode = 599;
        this.mFirstDetected = false;
        this.mNetworkConnectedTime = -1;
        removeDelayedMessage(103);
        this.mHandler.removeMessages(112);
        this.mHandler.removeMessages(121);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 42, null);
        Settings.Secure.putInt(this.mContext.getContentResolver(), WifiProCommonUtils.PORTAL_NETWORK_FLAG, 0);
        if (this.mLaaDiabledRequest) {
            this.mLaaDiabledRequest = false;
            Bundle data = new Bundle();
            data.putBoolean("is24gConnected", true);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 31, data);
        }
    }

    /* access modifiers changed from: private */
    public void handleMsgPortalSelected(Message msg) {
        WifiConfiguration wifiConfiguration;
        logI("###MSG_PORTAL_SELECTED");
        if (this.mPopUpNotifyWifiConfig != null) {
            if (!this.mBackGroundRunning.get() || (wifiConfiguration = this.mCurrentCheckWifiConfig) == null || wifiConfiguration.configKey() == null || !this.mCurrentCheckWifiConfig.configKey().equals(this.mPopUpNotifyWifiConfig.configKey())) {
                logI("MSG_PORTAL_SELECTED, to connect the notification portal network.");
                this.mWifiManager.connect(this.mPopUpNotifyWifiConfig, null);
                Settings.Secure.putInt(this.mContext.getContentResolver(), WifiProCommonUtils.PORTAL_NETWORK_FLAG, 2);
            } else {
                logI("MSG_PORTAL_SELECTED, to switch the portal network foreground.");
                switchWifiForeground();
                this.mLaunchBrowserRetryCount = 0;
                Handler handler = this.mHandler;
                handler.sendMessageDelayed(Message.obtain(handler, 111, this.mPopUpNotifyWifiConfig), 500);
            }
            notifyPortalStatusChanged(false, this.mPopUpNotifyWifiConfig.configKey(), this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp > 0);
            WifiProStatisticsManager.getInstance().increasePortalRefusedButUserTouchCnt();
            this.mPortalNotificationId = -1;
            this.mPopUpNotifyWifiConfig = null;
            this.mPopUpWifiRssi = -200;
            this.mBackGroundRunning.set(false);
            synchronized (this.mPortalDatabaseLock) {
                this.mPortalUnauthDatabase.clear();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleMsgPortalCanceled(Message msg) {
        WifiConfiguration wifiConfiguration = this.mPopUpNotifyWifiConfig;
        if (wifiConfiguration != null) {
            notifyPortalStatusChanged(false, wifiConfiguration.configKey(), this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp > 0);
            Handler handler = this.mHandler;
            handler.sendMessage(Message.obtain(handler, 102));
            this.mPortalNotificationId = -1;
            this.mPopUpNotifyWifiConfig = null;
            this.mPopUpWifiRssi = -200;
        }
    }

    /* access modifiers changed from: private */
    public void handleMsgLaunchBrowser(Message msg) {
        if (msg.obj != null && (msg.obj instanceof WifiConfiguration)) {
            if (WifiProCommonUtils.isWifiConnectedActive(this.mContext)) {
                launchBrowserForPortalLogin(((WifiConfiguration) msg.obj).configKey());
                return;
            }
            logI("Delay launchBrowserForPortalLogin");
            int i = this.mLaunchBrowserRetryCount;
            if (i < 4) {
                this.mLaunchBrowserRetryCount = i + 1;
                Handler handler = this.mHandler;
                handler.sendMessageDelayed(Message.obtain(handler, 111, msg.obj), 500);
                return;
            }
            logE("LaunchBrowserFail");
            this.mWifiManager.connect((WifiConfiguration) msg.obj, null);
        }
    }

    /* access modifiers changed from: private */
    public void handleMsgHttpRecheck(Message msg) {
        if (this.mBackGroundRunning.get()) {
            if (msg.arg2 == 1) {
                this.mCurrentCheckWifiConfig = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
                StringBuilder sb = new StringBuilder();
                sb.append("MSG_HTTP_RECHECK, current request network = ");
                WifiConfiguration wifiConfiguration = this.mCurrentCheckWifiConfig;
                sb.append(wifiConfiguration != null ? StringUtilEx.safeDisplaySsid(wifiConfiguration.getPrintableSsid()) : null);
                logI(sb.toString());
            }
            new NetworkCheckThread(msg.arg1, msg.arg2).start();
        }
    }

    /* access modifiers changed from: private */
    public void handleRssiChangedPortalNetwork(Message msg) {
        WifiConfiguration wifiConfiguration;
        int maxRssi = msg.arg1;
        String configKey = (String) msg.obj;
        if (configKey != null && (wifiConfiguration = this.mPopUpNotifyWifiConfig) != null && configKey.equals(wifiConfiguration.configKey())) {
            this.mPopUpWifiRssi = maxRssi;
            updateUnauthPortalDatabase(configKey, maxRssi);
        }
    }

    /* access modifiers changed from: private */
    public void handlePortalNetworkConnected(Message msg) {
        logI("###MSG_PORTAL_NETWORK_CONNECTED");
        if (!WifiProCommonUtils.isInMonitorList(WifiProCommonUtils.getPackageName(this.mContext, WifiProCommonUtils.getForegroundAppUid(this.mContext)), BROWSERS_PRE_INSTALLED)) {
            this.mHandler.removeMessages(121);
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(Message.obtain(handler, 121), DELAY_DURATION);
        }
    }

    /* access modifiers changed from: private */
    public void handlePortalBrowserLaunched(Message msg) {
        if (this.mHandler.hasMessages(121)) {
            logI("###MSG_PORTAL_BROWSER_LAUNCHED");
            this.mHandler.removeMessages(121);
            long j = 0;
            if (this.mNetworkConnectedTime > 0) {
                j = (System.currentTimeMillis() - this.mNetworkConnectedTime) / 1000;
            }
            int deltaTimeSec = (int) j;
            int i = HwQoEUtils.QOE_MSG_CAMERA_ON;
            if (deltaTimeSec <= 120) {
                i = deltaTimeSec;
            }
            handleBrowserLaunchedTimeout(i);
        }
    }

    private void registerUserBroadcastReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* class com.huawei.hwwifiproservice.HwAutoConnectManager.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                if ("com.huawei.wifipro.action.ACTION_NOTIFY_PORTAL_CONNECTED_BACKGROUND".equals(intent.getAction())) {
                    HwAutoConnectManager.this.logI("ACTION_NOTIFY_PORTAL_CONNECTED_BACKGROUND received.");
                    HwAutoConnectManager.this.mBackGroundRunning.set(true);
                    HwAutoConnectManager.this.mHandler.removeMessages(107);
                    if (!HwAutoConnectManager.this.mLaaDiabledRequest && WifiProCommonUtils.isWifi5GConnected(HwAutoConnectManager.this.mWifiManager)) {
                        boolean unused = HwAutoConnectManager.this.mLaaDiabledRequest = true;
                        Bundle data = new Bundle();
                        data.putBoolean("is24gConnected", false);
                        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 31, data);
                    }
                    HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 112, 101, 1));
                } else if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                    Object tempInfo = intent.getParcelableExtra("networkInfo");
                    NetworkInfo info = null;
                    if (tempInfo instanceof NetworkInfo) {
                        info = (NetworkInfo) tempInfo;
                    } else {
                        HwAutoConnectManager.this.logE("onReceive:tempInfo is not match the class");
                    }
                    if (info != null && info.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {
                        HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 103));
                    } else if (info != null && info.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                        HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 116));
                    }
                } else if ("com.huawei.wifipro.action.ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND".equals(intent.getAction())) {
                    HwAutoConnectManager.this.logI("ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND received.");
                    HwAutoConnectManager.this.mBackGroundRunning.set(true);
                    HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 112, 104, 1));
                } else if (WifiProCommonDefs.ACTION_PORTAL_USED_BY_USER.equals(intent.getAction())) {
                    HwAutoConnectManager.this.mHandler.sendMessageDelayed(Message.obtain(HwAutoConnectManager.this.mHandler, 107), 2000);
                } else if (WifiProCommonDefs.ACTION_PORTAL_CANCELED_BY_USER.equals(intent.getAction())) {
                    HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 108));
                } else if ("com.huawei.wifipro.action.ACTION_NOTIFY_PORTAL_OUT_OF_RANGE".equals(intent.getAction())) {
                    HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 109));
                } else if (!"android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction())) {
                } else {
                    if (!HwAutoConnectManager.this.mWifiManager.isWifiEnabled() && HwAutoConnectManager.this.mPowerManager.isScreenOn()) {
                        HwAutoConnectManager.this.mHandler.sendMessageDelayed(Message.obtain(HwAutoConnectManager.this.mHandler, 117), HwAutoConnectManager.DELAYED_TIME_WIFI_ICON);
                    } else if (HwAutoConnectManager.this.mWifiManager.isWifiEnabled() && HwAutoConnectManager.this.mPowerManager.isScreenOn()) {
                        HwAutoConnectManager.this.mHandler.removeMessages(117);
                    }
                }
            }
        };
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter, WifiproUtils.PERMISSION_RECV_WIFI_CONNECTED_CONCURRENTLY, null);
        ActivityManagerEx.registerHwActivityNotifier(this.mActivityNotifierEx, "activityLifeState");
    }

    /* access modifiers changed from: private */
    public void handleBrowserLaunchedTimeout(int deltaTimeSec) {
        logI("###handleBrowserLaunchedTimeout, deltaTimeSec = " + deltaTimeSec);
        if (1 != Settings.Global.getInt(this.mContext.getContentResolver(), "hw_disable_portal", 0)) {
            if ("CMCC".equalsIgnoreCase(SystemProperties.get("ro.config.operators", "")) && "CMCC".equals(WifiProCommonUtils.getCurrentSsid(this.mWifiManager))) {
                return;
            }
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0 || !"true".equals(SettingsEx.Systemex.getString(this.mContext.getContentResolver(), "wifi.challenge.required"))) {
                synchronized (this.mCurrentTopUidLock) {
                    if (!"com.huawei.hiskytone".equals(this.mCurrentPackageName)) {
                        Bundle data = new Bundle();
                        data.putInt("Server", deltaTimeSec);
                        data.putInt("eventId", WifiProCommonUtils.ID_UPDATE_PORTAL_POPUP_BROWSER_FAILED);
                        uploadChrEvent(data);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handlePortalOutOfRange() {
        cancelPortalNotifyStatusBar();
        this.mHandler.removeMessages(112);
        this.mBackGroundRunning.set(false);
        this.mPortalNotificationId = -1;
        this.mPopUpNotifyWifiConfig = null;
        this.mCurrentCheckWifiConfig = null;
        synchronized (this.mPortalDatabaseLock) {
            this.mPortalUnauthDatabase.clear();
        }
    }

    /* access modifiers changed from: private */
    public void showPortalStatusBar(boolean updated) {
        WifiConfiguration wifiConfiguration = this.mPopUpNotifyWifiConfig;
        if (wifiConfiguration != null && !TextUtils.isEmpty(wifiConfiguration.SSID) && !this.mPopUpNotifyWifiConfig.SSID.equals("<unknown ssid>")) {
            logI("showPortalStatusBar, portal network = " + this.mPopUpNotifyWifiConfig.configKey());
            boolean z = false;
            if (!updated && this.mPortalNotificationId == -1) {
                this.mPortalNotificationId = new SecureRandom().nextInt(100000);
                this.mPortalBuilder = this.mWifiProUIDisplayManager.showPortalNotificationStatusBar(this.mPopUpNotifyWifiConfig.SSID, PORTAL_STATUS_BAR_TAG, this.mPortalNotificationId, null);
                String configKey = this.mPopUpNotifyWifiConfig.configKey();
                if (this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp > 0) {
                    z = true;
                }
                notifyPortalStatusChanged(true, configKey, z);
                WifiProStatisticsManager.getInstance().increasePortalNoAutoConnCnt();
            } else if (updated && this.mPortalNotificationId != -1) {
                this.mPortalBuilder = this.mWifiProUIDisplayManager.showPortalNotificationStatusBar(this.mPopUpNotifyWifiConfig.SSID, PORTAL_STATUS_BAR_TAG, this.mPortalNotificationId, this.mPortalBuilder);
                String configKey2 = this.mPopUpNotifyWifiConfig.configKey();
                if (this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp > 0) {
                    z = true;
                }
                notifyPortalStatusChanged(true, configKey2, z);
            }
        }
    }

    private void cancelPortalNotifyStatusBar() {
        if (this.mPortalNotificationId != -1 && this.mPopUpNotifyWifiConfig != null) {
            logI("cancelPortalNotifyStatusBar, nid = " + this.mPortalNotificationId + ", ssid = " + this.mPopUpNotifyWifiConfig.configKey());
            this.mWifiProUIDisplayManager.cancelPortalNotificationStatusBar(PORTAL_STATUS_BAR_TAG, this.mPortalNotificationId);
            notifyPortalStatusChanged(false, this.mPopUpNotifyWifiConfig.configKey(), this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp > 0);
        }
    }

    private void removeDelayedMessage(int reason) {
        if (reason == 103) {
            if (this.mHandler.hasMessages(101)) {
                logW("MSG_CHECK_PORTAL_NETWORK msg removed");
                this.mHandler.removeMessages(101);
            }
            if (this.mHandler.hasMessages(111)) {
                logW("MSG_LAUNCH_BROWSER msg removed");
                this.mHandler.removeMessages(111);
            }
            if (this.mHandler.hasMessages(106)) {
                logW("MSG_PORTAL_STATUS_BAR msg removed");
                this.mHandler.removeMessages(106);
            }
            if (this.mHandler.hasMessages(113)) {
                this.mHandler.removeMessages(113);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleRespCodeForPortalCheck(int checkCounter, int respCode) {
        WifiConfiguration wifiConfiguration;
        logI("handleRespCodeForPortalCheck, counter = " + checkCounter + ", respCode = " + respCode);
        if (respCode == 204) {
            if (checkCounter < 3) {
                Handler handler = this.mHandler;
                handler.sendMessageDelayed(Message.obtain(handler, 112, 101, checkCounter + 1), 2000);
                return;
            }
            WifiConfiguration wifiConfiguration2 = this.mCurrentCheckWifiConfig;
            if (wifiConfiguration2 != null) {
                wifiConfiguration2.portalCheckStatus = 1;
                wifiConfiguration2.noInternetAccess = false;
                wifiConfiguration2.validatedInternetAccess = true;
                wifiConfiguration2.wifiProNoInternetAccess = false;
                wifiConfiguration2.lastHasInternetTimestamp = System.currentTimeMillis();
                updateWifiConfig(this.mCurrentCheckWifiConfig);
                synchronized (this.mPortalDatabaseLock) {
                    this.mPortalUnauthDatabase.clear();
                }
                this.mCurrentCheckWifiConfig = null;
                switchWifiForeground();
            }
        } else if (WifiProCommonUtils.isRedirectedRespCode(respCode)) {
            boolean empty = false;
            WifiConfiguration wifiConfiguration3 = this.mCurrentCheckWifiConfig;
            if (wifiConfiguration3 != null && (empty = WifiProCommonUtils.matchedRequestByHistory(wifiConfiguration3.internetHistory, 103))) {
                WifiConfiguration wifiConfiguration4 = this.mCurrentCheckWifiConfig;
                wifiConfiguration4.portalCheckStatus = 2;
                wifiConfiguration4.portalNetwork = true;
                wifiConfiguration4.noInternetAccess = false;
                wifiConfiguration4.wifiProNoInternetAccess = true;
                wifiConfiguration4.wifiProNoInternetReason = 1;
                wifiConfiguration4.internetHistory = WifiProCommonUtils.insertWifiConfigHistory(wifiConfiguration4.internetHistory, 2);
                updateWifiConfig(this.mCurrentCheckWifiConfig);
            }
            HwNetworkPropertyChecker hwNetworkPropertyChecker = this.mHwNetworkPropertychecker;
            if (hwNetworkPropertyChecker != null) {
                this.mPortalRedirectedUrl = hwNetworkPropertyChecker.getPortalRedirectedUrl();
                this.mPortalRespCode = this.mHwNetworkPropertychecker.getRawHttpRespCode();
                this.mFirstDetected = empty;
            }
            if (this.mPopUpNotifyWifiConfig == null && (wifiConfiguration = this.mCurrentCheckWifiConfig) != null) {
                this.mPopUpNotifyWifiConfig = wifiConfiguration;
                this.mPopUpWifiRssi = WifiProCommonUtils.getCurrentRssi(this.mWifiManager);
                Handler handler2 = this.mHandler;
                handler2.sendMessage(Message.obtain(handler2, 106, false));
            }
            if (checkCounter < 3) {
                Handler handler3 = this.mHandler;
                handler3.sendMessageDelayed(Message.obtain(handler3, 112, 101, checkCounter + 1), DELAYED_TIME_HTTP_RECHECK);
                return;
            }
            if (this.mCurrentCheckWifiConfig != null) {
                saveCurrentUnauthPortalBssid();
                WifiConfiguration wifiConfiguration5 = this.mPopUpNotifyWifiConfig;
                if (!(wifiConfiguration5 == null || wifiConfiguration5.configKey() == null || this.mPopUpNotifyWifiConfig.configKey().equals(this.mCurrentCheckWifiConfig.configKey()))) {
                    int currCheckNetworkRssi = WifiProCommonUtils.getCurrentRssi(this.mWifiManager);
                    if ((this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp == 0 && currCheckNetworkRssi > CHECK_NETWORK_RSSI_LEVEL) || (currCheckNetworkRssi >= -75 && currCheckNetworkRssi - this.mPopUpWifiRssi >= 8)) {
                        this.mPopUpNotifyWifiConfig = this.mCurrentCheckWifiConfig;
                        this.mPopUpWifiRssi = currCheckNetworkRssi;
                        Handler handler4 = this.mHandler;
                        handler4.sendMessage(Message.obtain(handler4, 106, true));
                    }
                }
            }
            Handler handler5 = this.mHandler;
            handler5.sendMessage(Message.obtain(handler5, 102));
        } else if (!WifiProCommonUtils.unreachableRespCode(respCode)) {
        } else {
            if (checkCounter < 3) {
                Handler handler6 = this.mHandler;
                handler6.sendMessageDelayed(Message.obtain(handler6, 112, 101, checkCounter + 1), DELAYED_TIME_HTTP_RECHECK);
                return;
            }
            WifiConfiguration wifiConfiguration6 = this.mCurrentCheckWifiConfig;
            if (wifiConfiguration6 != null) {
                if (WifiProCommonUtils.matchedRequestByHistory(wifiConfiguration6.internetHistory, 103)) {
                    WifiConfiguration wifiConfiguration7 = this.mCurrentCheckWifiConfig;
                    wifiConfiguration7.noInternetAccess = true;
                    wifiConfiguration7.validatedInternetAccess = false;
                    wifiConfiguration7.wifiProNoInternetAccess = true;
                    wifiConfiguration7.wifiProNoInternetReason = 0;
                    wifiConfiguration7.wifiProNoHandoverNetwork = false;
                    wifiConfiguration7.internetHistory = WifiProCommonUtils.insertWifiConfigHistory(wifiConfiguration7.internetHistory, 0);
                    updateWifiConfig(this.mCurrentCheckWifiConfig);
                } else {
                    saveCurrentUnauthPortalBssid();
                    if (this.mPopUpNotifyWifiConfig == null) {
                        this.mPopUpNotifyWifiConfig = this.mCurrentCheckWifiConfig;
                        this.mPopUpWifiRssi = WifiProCommonUtils.getCurrentRssi(this.mWifiManager);
                        Handler handler7 = this.mHandler;
                        handler7.sendMessage(Message.obtain(handler7, 106, false));
                    }
                }
                Handler handler8 = this.mHandler;
                handler8.sendMessage(Message.obtain(handler8, 102));
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleRespCodeForNoInternetCheck(int counter, int respCode) {
        logI("handleRespCodeForNoInternetCheck, counter = " + counter + ", resp = " + respCode);
        WifiConfiguration wifiConfiguration = this.mCurrentCheckWifiConfig;
        if (wifiConfiguration != null) {
            wifiConfiguration.internetRecoveryCheckTimestamp = System.currentTimeMillis();
            this.mCurrentCheckWifiConfig.internetRecoveryStatus = respCode == 204 ? 5 : 4;
            if (this.mCurrentCheckWifiConfig.internetRecoveryStatus == 5) {
                this.mCurrentCheckWifiConfig.lastHasInternetTimestamp = System.currentTimeMillis();
                WifiConfiguration wifiConfiguration2 = this.mCurrentCheckWifiConfig;
                wifiConfiguration2.noInternetAccess = false;
                wifiConfiguration2.validatedInternetAccess = true;
                wifiConfiguration2.internetHistory = WifiProCommonUtils.insertWifiConfigHistory(wifiConfiguration2.internetHistory, 1);
            }
            updateWifiConfig(this.mCurrentCheckWifiConfig);
            Handler handler = this.mHandler;
            handler.sendMessage(Message.obtain(handler, 102));
        }
    }

    private void saveCurrentUnauthPortalBssid() {
        synchronized (this.mPortalDatabaseLock) {
            if (!this.mPortalUnauthDatabase.containsKey(this.mCurrentCheckWifiConfig.configKey())) {
                ArrayList<String> unauthBssidsList = new ArrayList<>();
                unauthBssidsList.add(WifiProCommonUtils.getCurrentBssid(this.mWifiManager));
                this.mPortalUnauthDatabase.put(this.mCurrentCheckWifiConfig.configKey(), unauthBssidsList);
            } else {
                this.mPortalUnauthDatabase.get(this.mCurrentCheckWifiConfig.configKey()).add(WifiProCommonUtils.getCurrentBssid(this.mWifiManager));
            }
        }
    }

    private void updateUnauthPortalDatabase(String configKey, int maxRssi) {
        if (maxRssi <= CHECK_NETWORK_RSSI_LEVEL) {
            synchronized (this.mPortalDatabaseLock) {
                ArrayList<String> releasedConfigKeys = new ArrayList<>();
                for (Map.Entry<String, ArrayList<String>> entry : this.mPortalUnauthDatabase.entrySet()) {
                    String currKey = entry.getKey();
                    if (!(currKey == null || configKey == null || currKey.equals(configKey))) {
                        releasedConfigKeys.add(currKey);
                    }
                }
                Iterator<String> it = releasedConfigKeys.iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    logI("updateUnauthPortalDatabase, key = " + key);
                    this.mPortalUnauthDatabase.remove(key);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void switchWifiForeground() {
        notifyPoorLinkDetected();
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(Message.obtain(handler, 113), DELAYED_TIME_WIFI_ICON);
    }

    private void uploadChrEvent(Bundle bundle) {
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, bundle);
    }

    private void notifyPoorLinkDetected() {
        Bundle data = new Bundle();
        data.putInt(KEYWORD_MESSAGE_WHAT, POOR_LINK_DETECTED);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 28, data);
    }

    /* access modifiers changed from: private */
    public void notifyGoodLinkDetected() {
        Bundle data = new Bundle();
        data.putInt(KEYWORD_MESSAGE_WHAT, GOOD_LINK_DETECTED);
        data.putInt(KEYWORD_MESSAGE_ARG, 2);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 28, data);
    }

    private void notifyPortalStatusChanged(boolean popUp, String configKey, boolean hasInternetAccess) {
        Bundle data = new Bundle();
        data.putBoolean("popUp", popUp);
        data.putString("configKey", configKey);
        data.putBoolean("hasInternetAccess", hasInternetAccess);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 43, data);
    }

    public void notifyNetworkDisconnected() {
        if (this.mBackGroundRunning.get()) {
            if (HwSelfCureEngine.getInstance() != null) {
                HwSelfCureEngine.getInstance().notifyWifiDisconnected();
            }
            Handler handler = this.mHandler;
            handler.sendMessage(Message.obtain(handler, 103));
        }
    }

    public void notifyEnableSameNetworkId(int netId) {
        Handler handler = this.mHandler;
        handler.sendMessage(Message.obtain(handler, 115, netId, 0));
    }

    public void updatePopUpNetworkRssi(String configKey, int maxRssi) {
        Handler handler = this.mHandler;
        handler.sendMessage(Message.obtain(handler, 114, maxRssi, 0, configKey));
    }

    public void notifyPortalNetworkConnected() {
        Handler handler = this.mHandler;
        handler.sendMessage(Message.obtain(handler, (int) HwQoEUtils.QOE_MSG_CAMERA_ON));
    }

    public boolean allowCheckPortalNetwork(String configKey, String bssid) {
        ArrayList<String> unauthBssids;
        synchronized (this.mPortalDatabaseLock) {
            for (Map.Entry<String, ArrayList<String>> entry : this.mPortalUnauthDatabase.entrySet()) {
                String currKey = entry.getKey();
                if (!(currKey == null || configKey == null || !currKey.equals(configKey) || (unauthBssids = this.mPortalUnauthDatabase.get(currKey)) == null || bssid == null)) {
                    if (unauthBssids.size() >= 3) {
                        return false;
                    }
                    Iterator<String> it = unauthBssids.iterator();
                    while (it.hasNext()) {
                        if (bssid.equals(it.next())) {
                            return false;
                        }
                    }
                    continue;
                }
            }
            return true;
        }
    }

    public boolean isPortalNotifyOn() {
        return this.mPortalNotificationId != -1;
    }

    private void updateWifiConfig(WifiConfiguration config) {
        if (config != null) {
            Bundle data = new Bundle();
            data.putInt(KEYWORD_MESSAGE_WHAT, 131672);
            data.putParcelable(KEYWORD_MESSAGE_OBJ, config);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 28, data);
        }
    }

    private boolean useOperatorOverSea() {
        String operator = TelephonyManager.getDefault().getNetworkOperator();
        if (operator == null || operator.length() <= 0) {
            if ("CN".equalsIgnoreCase(WifiProCommonUtils.getProductLocale())) {
                return false;
            }
            return true;
        } else if (operator.startsWith("460")) {
            return false;
        } else {
            return true;
        }
    }

    public void saveAutoJoinTargetBssid(WifiConfiguration config, String targetBssid) {
        synchronized (this.mAutoConnectFilterLock) {
            this.mCurrentAutoJoinTargetBssid = targetBssid;
            if (config != null && !config.getNetworkSelectionStatus().isNetworkEnabled()) {
                this.mAutoJoinDisabledNetworkCnt++;
            }
            logI("saveAutoJoinTargetBssid, autoJoinDisabedNetworkCnt = " + this.mAutoJoinDisabledNetworkCnt);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x001f A[Catch:{ all -> 0x001a }] */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0047 A[Catch:{ all -> 0x001a }] */
    public boolean isAutoJoinAllowedSetTargetBssid(WifiConfiguration config, String targetBssid) {
        boolean matchedBlacklistSsid;
        synchronized (this.mAutoConnectFilterLock) {
            if (config != null) {
                try {
                    if (config.configKey() != null && config.configKey().equals(this.mCurrentBlacklistConfigKey)) {
                        matchedBlacklistSsid = true;
                        if (!matchedBlacklistSsid) {
                            this.mCurrentAutoJoinTargetBssid = targetBssid;
                            if (!config.getNetworkSelectionStatus().isNetworkEnabled()) {
                                this.mAutoJoinDisabledNetworkCnt++;
                            }
                            logI("isAutoJoinAllowedSetTargetBssid, autoJoinDisabedNetworkCnt = " + this.mAutoJoinDisabledNetworkCnt);
                        } else {
                            this.mCurrentAutoJoinTargetBssid = targetBssid;
                            logI("Set the target BSSID = " + StringUtilEx.safeDisplayBssid(targetBssid));
                        }
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            matchedBlacklistSsid = false;
            if (!matchedBlacklistSsid) {
            }
        }
        return true;
    }

    public boolean isBssidMatchedBlacklist(String bssid) {
        synchronized (this.mAutoConnectFilterLock) {
            if (!WifiProCommonUtils.isInMonitorList(bssid, (String[]) this.mAutoJoinBlacklistBssid.toArray(new String[0]))) {
                return false;
            }
            logI("In blacklist bssid " + StringUtilEx.safeDisplayBssid(bssid));
            return true;
        }
    }

    public void releaseBlackListBssid(WifiConfiguration config, boolean autoJoin) {
        if (!autoJoin && config != null && config.configKey() != null) {
            synchronized (this.mAutoConnectFilterLock) {
                if (config.configKey().equals(this.mCurrentBlacklistConfigKey)) {
                    this.mHandler.removeMessages(118);
                    this.mCurrentBlacklistConfigKey = null;
                    this.mAutoJoinBlacklistBssid.clear();
                    this.mCurrentAutoJoinTargetBssid = null;
                    this.mAutoJoinDisabledNetworkCnt = 0;
                }
            }
        }
    }

    public void notifyWifiConnFailedInfo(WifiConfiguration config, String bssid, int rssi, int reason) {
        if (config != null && config.configKey() != null) {
            logI("notifyWifiConnFailedInfo, rssi = " + rssi + ", reason = " + reason);
            synchronized (this.mAutoConnectFilterLock) {
                if (reason == 3 || reason == 2) {
                    if (this.mCurrentBlacklistConfigKey != null && !this.mCurrentBlacklistConfigKey.equals(config.configKey())) {
                        this.mAutoJoinDisabledNetworkCnt = 0;
                        this.mAutoJoinBlacklistBssid.clear();
                    }
                    this.mCurrentBlacklistConfigKey = config.configKey();
                    String currBssid = bssid != null ? bssid : this.mCurrentAutoJoinTargetBssid;
                    if (currBssid != null && !WifiProCommonUtils.isInMonitorList(currBssid, (String[]) this.mAutoJoinBlacklistBssid.toArray(new String[0]))) {
                        this.mAutoJoinBlacklistBssid.add(currBssid);
                        logI("add to four minutes blacklist bssid: " + StringUtilEx.safeDisplayBssid(currBssid) + " reason " + reason);
                    }
                    this.mHandler.removeMessages(118);
                    this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 118), 240000);
                    WifiConfiguration.NetworkSelectionStatus status = config.getNetworkSelectionStatus();
                    logI("notifyWifiConnFailedInfo, isNetworkEnabled = " + status.isNetworkEnabled() + ", cnt = " + this.mAutoJoinDisabledNetworkCnt);
                }
            }
        }
    }

    public boolean allowAutoJoinDisabledNetworkAgain(WifiConfiguration config) {
        boolean z = false;
        if (config == null || config.configKey() == null) {
            return false;
        }
        synchronized (this.mAutoConnectFilterLock) {
            if (config.configKey().equals(this.mCurrentBlacklistConfigKey) && this.mAutoJoinDisabledNetworkCnt < 2) {
                z = true;
            }
        }
        return z;
    }

    public void launchBrowserForPortalLogin(String configKey) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), WifiProCommonUtils.PORTAL_NETWORK_FLAG, 1);
        Bundle data = new Bundle();
        data.putString("configKey", configKey);
        data.putString("redirectedUrl", this.mPortalRedirectedUrl);
        data.putBoolean("firstDetected", this.mFirstDetected);
        data.putInt("respCode", this.mPortalRespCode);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 5, data);
        Bundle bundle = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 14, null);
        Network network = null;
        if (bundle != null) {
            network = (Network) bundle.getParcelable(NetworkMonitor.KEY_NETWORK_NAME);
        }
        if (network == null) {
            Log.e(TAG, "Webview network null");
            return;
        }
        Bundle appExtras = new Bundle();
        appExtras.putParcelable("android.net.extra.NETWORK", network);
        appExtras.putString("android.net.extra.CAPTIVE_PORTAL_URL", this.mHwNetworkPropertychecker.getCaptiveUsedServer());
        appExtras.putString("android.net.extra.CAPTIVE_PORTAL_USER_AGENT", getCaptivePortalUserAgent(this.mContext));
        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).startCaptivePortalApp(network, appExtras);
    }

    private static String getCaptivePortalUserAgent(Context context) {
        String value = Settings.Global.getString(context.getContentResolver(), "captive_portal_user_agent");
        return value != null ? value : DEFAULT_USER_AGENT;
    }

    private class NetworkCheckThread extends Thread {
        private int checkCounter;
        private int msg;

        public NetworkCheckThread(int msg2, int counter) {
            this.msg = msg2;
            this.checkCounter = counter;
        }

        public void run() {
            synchronized (HwAutoConnectManager.this.mNetworkCheckLock) {
                HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, this.msg, this.checkCounter, HwAutoConnectManager.this.mHwNetworkPropertychecker.isCaptivePortal(this.msg != 104, false, true)));
            }
        }
    }

    public void checkPortalAuthExpiration() {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(123));
    }

    public int getCurrentTopUid() {
        int i;
        synchronized (this.mCurrentTopUidLock) {
            i = this.mCurrentTopUid;
        }
        return i;
    }

    public String getCurrentPackageName() {
        String str;
        synchronized (this.mCurrentTopUidLock) {
            str = this.mCurrentPackageName;
        }
        return str;
    }

    public void logD(String msg) {
        Log.d(TAG, msg);
    }

    public void logW(String msg) {
        Log.w(TAG, msg);
    }

    public void logE(String msg) {
        Log.e(TAG, msg);
    }

    /* access modifiers changed from: private */
    public void logI(String msg) {
        Log.i(TAG, msg);
    }
}
