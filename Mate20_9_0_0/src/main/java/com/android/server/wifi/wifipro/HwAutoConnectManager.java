package com.android.server.wifi.wifipro;

import android.app.KeyguardManager;
import android.app.Notification.Builder;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.contentsensor.IActivityObserver;
import android.contentsensor.IActivityObserver.Stub;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.SettingsEx.Systemex;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.Utils;
import com.android.server.am.HwActivityManagerService;
import com.android.server.wifi.HwPortalExceptionManager;
import com.android.server.wifi.HwSelfCureEngine;
import com.android.server.wifi.HwSelfCureUtils;
import com.android.server.wifi.HwWifiStateMachine;
import com.android.server.wifi.LAA.HwLaaController;
import com.android.server.wifi.LAA.HwLaaUtils;
import com.android.server.wifi.SavedNetworkEvaluator;
import com.android.server.wifi.WifiConnectivityManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifipro.WifiProCommonUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public class HwAutoConnectManager {
    private static final int AUTO_JOIN_DISABLED_NETWORK_THRESHOLD = 2;
    private static final String[] BROWSERS_PRE_INSTALLED = new String[]{HwPortalExceptionManager.BROWSER_PACKET_NAME, "com.android.browser", "com.android.chrome"};
    private static final String COUNTRY_CODE_CN = "460";
    private static final long DELAYED_TIME_HTTP_RECHECK = 10000;
    private static final long DELAYED_TIME_LAUNCH_BROWSER = 500;
    private static final long DELAYED_TIME_STATUS_BAR = 500;
    private static final long DELAYED_TIME_SWITCH_WIFI = 2000;
    private static final long DELAYED_TIME_WIFI_ICON = 200;
    public static final String KEY_HUAWEI_EMPLOYEE = "\"Huawei-Employee\"WPA_EAP";
    private static final int MAX_PORTAL_HTTP_TIMES = 3;
    private static final int MAX_START_BROWSER_TIME = 120;
    private static final int MSG_BLACKLIST_BSSID_TIMEOUT = 118;
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
    private static final int MSG_RSSI_CHANGED_PORTAL_NETWORK = 114;
    private static final int MSG_SWITCH_WIFI_FOREGROUND = 113;
    private static final int MSG_USER_ENTER_WLAN_SETTINGS = 119;
    private static final int MSG_WIFI_CLOSED = 110;
    private static final int MSG_WIFI_DISABLED_RCVD = 117;
    private static final String PORTAL_STATUS_BAR_TAG = "wifipro_portal_status_bar";
    private static final int START_BROWSER_TIMEOUT = -100;
    private static final String TAG = "HwAutoConnectManager";
    private static HwAutoConnectManager mHwAutoConnectManager = null;
    private IActivityObserver mActivityObserver = new Stub() {
        public void activityResumed(int pid, int uid, ComponentName componentName) throws RemoteException {
            try {
                if ("com.android.settings.Settings$WifiSettingsActivity".equals(componentName.getClassName())) {
                    HwAutoConnectManager.this.mHandler.sendMessage(HwAutoConnectManager.this.mHandler.obtainMessage(119));
                    HwSelfCureEngine.getInstance().notifyUserEnterWlanSettings();
                }
                if (WifiProCommonUtils.isInMonitorList(componentName.getPackageName(), HwAutoConnectManager.BROWSERS_PRE_INSTALLED)) {
                    HwAutoConnectManager.this.mHandler.sendMessage(HwAutoConnectManager.this.mHandler.obtainMessage(122));
                }
                synchronized (HwAutoConnectManager.this.mCurrentTopUidLock) {
                    HwAutoConnectManager.this.mCurrentTopUid = uid;
                    HwAutoConnectManager.this.mCurrentPackageName = componentName.getPackageName();
                }
            } catch (Exception e) {
                HwAutoConnectManager.this.LOGW("IActivityObserver Exception rcvd.");
            }
        }

        public void activityPaused(int pid, int uid, ComponentName componentName) throws RemoteException {
        }
    };
    private Object mAutoConnectFilterLock = new Object();
    private ArrayList<String> mAutoJoinBlacklistBssid = new ArrayList();
    private int mAutoJoinDisabledNetworkCnt = 0;
    private AtomicBoolean mBackGroundRunning = new AtomicBoolean(false);
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private String mCurrentAutoJoinTargetBssid = null;
    private String mCurrentBlacklistConfigKey = null;
    private WifiConfiguration mCurrentCheckWifiConfig = null;
    private String mCurrentPackageName = "";
    private int mCurrentTopUid = -1;
    private Object mCurrentTopUidLock = new Object();
    private boolean mFirstDetected = false;
    private Handler mHandler;
    private IntentFilter mIntentFilter;
    private KeyguardManager mKeyguardManager;
    private boolean mLaaDiabledRequest = false;
    private Object mNetworkCheckLock = new Object();
    private long mNetworkConnectedTime = -1;
    private HwNetworkPropertyRechecker mNetworkPropertyRechecker;
    private WifiConfiguration mPopUpNotifyWifiConfig = null;
    private int mPopUpWifiRssi = WifiHandover.INVALID_RSSI;
    private Builder mPortalBuilder = null;
    private Object mPortalDatabaseLock = new Object();
    private int mPortalNotificationId = -1;
    private String mPortalRedirectedUrl = null;
    private int mPortalRespCode = 599;
    private Map<String, ArrayList<String>> mPortalUnauthDatabase = new HashMap();
    private String mPortalUsedUrl = null;
    private PowerManager mPowerManager;
    private SavedNetworkEvaluator mSavedNetworkEvaluator;
    private WifiInjector mWifiInjector;
    private WifiManager mWifiManager;
    private WifiProUIDisplayManager mWifiProUIDisplayManager;
    private WifiStateMachine mWifiStateMachine;

    private class NetworkCheckThread extends Thread {
        private int checkCounter;
        private boolean portalNetwork;

        public NetworkCheckThread(boolean portal, int counter) {
            this.portalNetwork = portal;
            this.checkCounter = counter;
        }

        public void run() {
            synchronized (HwAutoConnectManager.this.mNetworkCheckLock) {
                HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, this.portalNetwork ? 101 : 104, this.checkCounter, HwAutoConnectManager.this.mNetworkPropertyRechecker.syncRequestNetworkCheck(this.portalNetwork, false, true)));
            }
        }
    }

    public HwAutoConnectManager(Context context, NetworkQosMonitor networkQosMonitor) {
        this.mContext = context;
        this.mWifiInjector = WifiInjector.getInstance();
        this.mWifiStateMachine = this.mWifiInjector.getWifiStateMachine();
        this.mSavedNetworkEvaluator = this.mWifiInjector.getSavedNetworkEvaluator();
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mKeyguardManager = (KeyguardManager) context.getSystemService("keyguard");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mNetworkPropertyRechecker = networkQosMonitor.getNetworkPropertyRechecker();
        this.mWifiProUIDisplayManager = networkQosMonitor.getWifiProUIDisplayManager();
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

    public void init() {
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction(WifiproUtils.ACTION_NOTIFY_PORTAL_CONNECTED_BACKGROUND);
        this.mIntentFilter.addAction(WifiproUtils.ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND);
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mIntentFilter.addAction("com.huawei.wifipro.action.ACTION_PORTAL_USED_BY_USER");
        this.mIntentFilter.addAction("com.huawei.wifipro.action.ACTION_PORTAL_CANCELED_BY_USER");
        this.mIntentFilter.addAction(WifiproUtils.ACTION_NOTIFY_PORTAL_OUT_OF_RANGE);
        this.mIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        HandlerThread handlerThread = new HandlerThread("wifipro_auto_conn_manager_handler_thread");
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                Message message = msg;
                long j = 0;
                String str = null;
                boolean z = false;
                HwAutoConnectManager hwAutoConnectManager;
                StringBuilder stringBuilder;
                HwAutoConnectManager hwAutoConnectManager2;
                int maxRssi;
                switch (message.what) {
                    case 101:
                        if (HwAutoConnectManager.this.mBackGroundRunning.get()) {
                            HwAutoConnectManager.this.handleRespCodeForPortalCheck(message.arg1, message.arg2);
                            break;
                        }
                        break;
                    case 102:
                        if (WifiProCommonUtils.isWifiConnected(HwAutoConnectManager.this.mWifiManager)) {
                            HwAutoConnectManager.this.LOGW("MSG_DISCONNECT_NETWORK msg handled");
                            HwAutoConnectManager.this.mWifiManager.disconnect();
                        }
                        HwAutoConnectManager.this.mBackGroundRunning.set(false);
                        HwAutoConnectManager.this.mCurrentCheckWifiConfig = null;
                        HwAutoConnectManager.this.mPortalRedirectedUrl = null;
                        HwAutoConnectManager.this.mPortalRespCode = 599;
                        HwAutoConnectManager.this.mFirstDetected = false;
                        HwSelfCureEngine.getInstance().notifyWifiDisconnected();
                        HwAutoConnectManager.this.mHandler.removeMessages(112);
                        break;
                    case 103:
                        HwAutoConnectManager.this.LOGD("MSG_RECV_NETWORK_DISCONNECTED");
                        HwAutoConnectManager.this.mBackGroundRunning.set(false);
                        HwAutoConnectManager.this.mCurrentCheckWifiConfig = null;
                        HwAutoConnectManager.this.mPortalRedirectedUrl = null;
                        HwAutoConnectManager.this.mPortalRespCode = 599;
                        HwAutoConnectManager.this.mFirstDetected = false;
                        HwAutoConnectManager.this.mNetworkConnectedTime = -1;
                        HwAutoConnectManager.this.removeDelayedMessage(103);
                        HwAutoConnectManager.this.mHandler.removeMessages(112);
                        HwAutoConnectManager.this.mHandler.removeMessages(121);
                        HwPortalExceptionManager.getInstance(HwAutoConnectManager.this.mContext).notifyNetworkDisconnected();
                        Secure.putInt(HwAutoConnectManager.this.mContext.getContentResolver(), "HW_WIFI_PORTAL_FLAG", 0);
                        if (HwAutoConnectManager.this.mLaaDiabledRequest) {
                            HwAutoConnectManager.this.mLaaDiabledRequest = false;
                            if (HwLaaUtils.isLaaPlusEnable() && HwLaaController.getInstrance() != null) {
                                HwLaaController.getInstrance().setLAAEnabled(true, 4);
                                break;
                            }
                        }
                        break;
                    case 104:
                        if (HwAutoConnectManager.this.mBackGroundRunning.get()) {
                            HwAutoConnectManager.this.handleRespCodeForNoInternetCheck(message.arg1, message.arg2);
                            break;
                        }
                        break;
                    case 106:
                        hwAutoConnectManager = HwAutoConnectManager.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("MSG_PORTAL_STATUS_BAR, update = ");
                        stringBuilder.append(message.obj);
                        hwAutoConnectManager.LOGD(stringBuilder.toString());
                        HwAutoConnectManager.this.showPortalStatusBar(((Boolean) message.obj).booleanValue());
                        break;
                    case 107:
                        HwAutoConnectManager.this.LOGD("###MSG_PORTAL_SELECTED");
                        if (HwAutoConnectManager.this.mPopUpNotifyWifiConfig != null) {
                            if (!HwAutoConnectManager.this.mBackGroundRunning.get() || (HwAutoConnectManager.this.mCurrentCheckWifiConfig != null && (HwAutoConnectManager.this.mCurrentCheckWifiConfig.configKey() == null || !HwAutoConnectManager.this.mCurrentCheckWifiConfig.configKey().equals(HwAutoConnectManager.this.mPopUpNotifyWifiConfig.configKey())))) {
                                HwAutoConnectManager.this.LOGD("MSG_PORTAL_SELECTED, to connect the notification portal network.");
                                HwAutoConnectManager.this.mWifiManager.connect(HwAutoConnectManager.this.mPopUpNotifyWifiConfig, null);
                            } else {
                                HwAutoConnectManager.this.LOGD("MSG_PORTAL_SELECTED, to switch the portal network foreground.");
                                HwAutoConnectManager.this.switchWifiForeground();
                                HwAutoConnectManager.this.mHandler.sendMessageDelayed(Message.obtain(HwAutoConnectManager.this.mHandler, 111, HwAutoConnectManager.this.mPopUpNotifyWifiConfig.configKey()), 500);
                            }
                            HwAutoConnectManager.this.notifyPortalStatusChanged(false, HwAutoConnectManager.this.mPopUpNotifyWifiConfig.configKey(), HwAutoConnectManager.this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp > 0);
                            WifiProStatisticsManager.getInstance().increasePortalRefusedButUserTouchCnt();
                            HwAutoConnectManager.this.mPortalNotificationId = -1;
                            HwAutoConnectManager.this.mPopUpNotifyWifiConfig = null;
                            HwAutoConnectManager.this.mPopUpWifiRssi = WifiHandover.INVALID_RSSI;
                            HwAutoConnectManager.this.mBackGroundRunning.set(false);
                            synchronized (HwAutoConnectManager.this.mPortalDatabaseLock) {
                                HwAutoConnectManager.this.mPortalUnauthDatabase.clear();
                            }
                            break;
                        }
                        break;
                    case 108:
                        if (HwAutoConnectManager.this.mPopUpNotifyWifiConfig != null) {
                            HwAutoConnectManager.this.notifyPortalStatusChanged(false, HwAutoConnectManager.this.mPopUpNotifyWifiConfig.configKey(), HwAutoConnectManager.this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp > 0);
                            HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 102));
                            HwAutoConnectManager.this.mPortalNotificationId = -1;
                            HwAutoConnectManager.this.mPopUpNotifyWifiConfig = null;
                            HwAutoConnectManager.this.mPopUpWifiRssi = WifiHandover.INVALID_RSSI;
                            break;
                        }
                        break;
                    case 109:
                        HwAutoConnectManager.this.handlePortalOutOfRange();
                        break;
                    case 111:
                        HwAutoConnectManager.this.launchBrowserForPortalLogin((String) message.obj);
                        break;
                    case 112:
                        if (HwAutoConnectManager.this.mBackGroundRunning.get()) {
                            if (message.arg2 == 1) {
                                HwAutoConnectManager.this.mCurrentCheckWifiConfig = WifiProCommonUtils.getCurrentWifiConfig(HwAutoConnectManager.this.mWifiManager);
                                hwAutoConnectManager = HwAutoConnectManager.this;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("MSG_HTTP_RECHECK, current request network = ");
                                if (HwAutoConnectManager.this.mCurrentCheckWifiConfig != null) {
                                    str = HwAutoConnectManager.this.mCurrentCheckWifiConfig.configKey();
                                }
                                stringBuilder.append(str);
                                hwAutoConnectManager.LOGD(stringBuilder.toString());
                            }
                            hwAutoConnectManager2 = HwAutoConnectManager.this;
                            if (message.arg1 == 101) {
                                z = true;
                            }
                            new NetworkCheckThread(z, message.arg2).start();
                            break;
                        }
                        break;
                    case 113:
                        HwAutoConnectManager.this.mWifiStateMachine.sendMessage(131874, 2);
                        break;
                    case 114:
                        maxRssi = message.arg1;
                        String configKey = message.obj;
                        if (!(configKey == null || HwAutoConnectManager.this.mPopUpNotifyWifiConfig == null || !configKey.equals(HwAutoConnectManager.this.mPopUpNotifyWifiConfig.configKey()))) {
                            HwAutoConnectManager.this.mPopUpWifiRssi = maxRssi;
                            HwAutoConnectManager.this.updateUnauthPortalDatabase(configKey, maxRssi);
                            break;
                        }
                    case 115:
                        hwAutoConnectManager = HwAutoConnectManager.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("###MSG_ENABLE_SAME_NETWORK_ID, nid = ");
                        stringBuilder.append(message.arg1);
                        hwAutoConnectManager.LOGD(stringBuilder.toString());
                        if (HwAutoConnectManager.this.mBackGroundRunning.get()) {
                            HwAutoConnectManager.this.switchWifiForeground();
                        }
                        HwAutoConnectManager.this.mBackGroundRunning.set(false);
                        break;
                    case 116:
                        HwAutoConnectManager.this.LOGD("##MSG_RECV_NETWORK_CONNECTED");
                        WifiConfiguration current = WifiProCommonUtils.getCurrentWifiConfig(HwAutoConnectManager.this.mWifiManager);
                        synchronized (HwAutoConnectManager.this.mAutoConnectFilterLock) {
                            if (current != null) {
                                try {
                                    if (!(current.configKey() == null || !current.configKey().equals(HwAutoConnectManager.this.mCurrentBlacklistConfigKey) || current.getNetworkSelectionStatus().isNetworkEnabled())) {
                                        HwAutoConnectManager hwAutoConnectManager3 = HwAutoConnectManager.this;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("##enableNetwork currentBlacklistConfigKey networkId = ");
                                        stringBuilder2.append(current.networkId);
                                        hwAutoConnectManager3.LOGD(stringBuilder2.toString());
                                        HwAutoConnectManager.this.mWifiManager.enableNetwork(current.networkId, false);
                                    }
                                } finally {
                                    break;
                                }
                            }
                            HwAutoConnectManager.this.mCurrentAutoJoinTargetBssid = null;
                            HwAutoConnectManager.this.mCurrentBlacklistConfigKey = null;
                            HwAutoConnectManager.this.mAutoJoinBlacklistBssid.clear();
                            HwAutoConnectManager.this.mAutoJoinDisabledNetworkCnt = 0;
                        }
                        HwAutoConnectManager.this.mHandler.removeMessages(118);
                        HwAutoConnectManager.this.mNetworkConnectedTime = System.currentTimeMillis();
                        HwAutoConnectManager.this.handlePortalOutOfRange();
                        break;
                    case 117:
                        synchronized (HwAutoConnectManager.this.mAutoConnectFilterLock) {
                            HwAutoConnectManager.this.mCurrentBlacklistConfigKey = null;
                            HwAutoConnectManager.this.mCurrentAutoJoinTargetBssid = null;
                            HwAutoConnectManager.this.mAutoJoinDisabledNetworkCnt = 0;
                            HwAutoConnectManager.this.mAutoJoinBlacklistBssid.clear();
                        }
                        HwAutoConnectManager.this.mHandler.removeMessages(118);
                        HwAutoConnectManager.this.handlePortalOutOfRange();
                        break;
                    case 118:
                        HwAutoConnectManager.this.LOGD("###MSG_BLACKLIST_BSSID_TIMEOUT");
                        synchronized (HwAutoConnectManager.this.mAutoConnectFilterLock) {
                            HwAutoConnectManager.this.mCurrentBlacklistConfigKey = null;
                            HwAutoConnectManager.this.mAutoJoinBlacklistBssid.clear();
                            HwAutoConnectManager.this.mAutoJoinDisabledNetworkCnt = 0;
                        }
                        break;
                    case 119:
                        HwAutoConnectManager.this.LOGD("###MSG_USER_ENTER_WLAN_SETTINGS");
                        if (HwAutoConnectManager.this.mBackGroundRunning.get()) {
                            HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 102));
                        }
                        HwAutoConnectManager.this.mHandler.removeMessages(118);
                        synchronized (HwAutoConnectManager.this.mAutoConnectFilterLock) {
                            HwAutoConnectManager.this.mCurrentBlacklistConfigKey = null;
                            HwAutoConnectManager.this.mAutoJoinBlacklistBssid.clear();
                            HwAutoConnectManager.this.mAutoJoinDisabledNetworkCnt = 0;
                        }
                        break;
                    case 120:
                        HwAutoConnectManager.this.LOGD("###MSG_PORTAL_NETWORK_CONNECTED");
                        if (!WifiProCommonUtils.isInMonitorList(WifiProCommonUtils.getPackageName(HwAutoConnectManager.this.mContext, WifiProCommonUtils.getForegroundAppUid(HwAutoConnectManager.this.mContext)), HwAutoConnectManager.BROWSERS_PRE_INSTALLED)) {
                            HwAutoConnectManager.this.mHandler.removeMessages(121);
                            HwAutoConnectManager.this.mHandler.sendMessageDelayed(Message.obtain(HwAutoConnectManager.this.mHandler, 121), 5000);
                            break;
                        }
                        HwAutoConnectManager.this.LOGD("###BROWSERS_PRE_INSTALLED launched!!!");
                        break;
                    case 121:
                        HwAutoConnectManager.this.LOGD("###MSG_PORTAL_BROWSER_LAUNCHED_TIMEOUT");
                        HwAutoConnectManager.this.handleBrowserLaunchedTimeout(-100);
                        break;
                    case 122:
                        if (HwAutoConnectManager.this.mHandler.hasMessages(121)) {
                            HwAutoConnectManager.this.LOGD("###MSG_PORTAL_BROWSER_LAUNCHED");
                            HwAutoConnectManager.this.mHandler.removeMessages(121);
                            if (HwAutoConnectManager.this.mNetworkConnectedTime > 0) {
                                j = (System.currentTimeMillis() - HwAutoConnectManager.this.mNetworkConnectedTime) / 1000;
                            }
                            maxRssi = (int) j;
                            hwAutoConnectManager2 = HwAutoConnectManager.this;
                            int i = 120;
                            if (maxRssi <= 120) {
                                i = maxRssi;
                            }
                            hwAutoConnectManager2.handleBrowserLaunchedTimeout(i);
                            break;
                        }
                        break;
                }
                super.handleMessage(msg);
            }
        };
        registerUserBroadcastReceiver();
    }

    private void registerUserBroadcastReceiver() {
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (WifiproUtils.ACTION_NOTIFY_PORTAL_CONNECTED_BACKGROUND.equals(intent.getAction())) {
                    HwAutoConnectManager.this.LOGD("ACTION_NOTIFY_PORTAL_CONNECTED_BACKGROUND received.");
                    HwAutoConnectManager.this.mBackGroundRunning.set(true);
                    HwAutoConnectManager.this.mHandler.removeMessages(107);
                    if (!HwAutoConnectManager.this.mLaaDiabledRequest && WifiProCommonUtils.isWifi5GConnected(HwAutoConnectManager.this.mWifiManager)) {
                        HwAutoConnectManager.this.mLaaDiabledRequest = true;
                        if (HwLaaUtils.isLaaPlusEnable() && HwLaaController.getInstrance() != null) {
                            HwLaaController.getInstrance().setLAAEnabled(false, 4);
                        }
                    }
                    HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 112, 101, 1));
                } else if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (info != null && info.getDetailedState() == DetailedState.DISCONNECTED) {
                        HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 103));
                    } else if (info != null && info.getDetailedState() == DetailedState.CONNECTED) {
                        HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 116));
                    }
                } else if (WifiproUtils.ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND.equals(intent.getAction())) {
                    HwAutoConnectManager.this.LOGD("ACTION_NOTIFY_NO_INTERNET_CONNECTED_BACKGROUND received.");
                    HwAutoConnectManager.this.mBackGroundRunning.set(true);
                    HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 112, 104, 1));
                } else if ("com.huawei.wifipro.action.ACTION_PORTAL_USED_BY_USER".equals(intent.getAction())) {
                    HwAutoConnectManager.this.mHandler.sendMessageDelayed(Message.obtain(HwAutoConnectManager.this.mHandler, 107), HwAutoConnectManager.DELAYED_TIME_SWITCH_WIFI);
                } else if ("com.huawei.wifipro.action.ACTION_PORTAL_CANCELED_BY_USER".equals(intent.getAction())) {
                    HwAutoConnectManager.this.mHandler.sendMessage(Message.obtain(HwAutoConnectManager.this.mHandler, 108));
                } else if (WifiproUtils.ACTION_NOTIFY_PORTAL_OUT_OF_RANGE.equals(intent.getAction())) {
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
        HwActivityManagerService hwActivityManger = HwActivityManagerService.self();
        if (hwActivityManger != null) {
            hwActivityManger.registerActivityObserver(this.mActivityObserver);
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0077, code skipped:
            r0 = com.android.server.wifi.HwWifiServiceFactory.getHwWifiCHRService();
     */
    /* JADX WARNING: Missing block: B:22:0x007b, code skipped:
            if (r0 == null) goto L_0x0092;
     */
    /* JADX WARNING: Missing block: B:23:0x007d, code skipped:
            r1 = new android.os.Bundle();
            r1.putInt("Server", r5);
            r0.uploadDFTEvent(909002061, r1);
            LOGD("###handleBrowserLaunchedTimeout");
     */
    /* JADX WARNING: Missing block: B:24:0x0092, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleBrowserLaunchedTimeout(int deltaTimeSec) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("###handleBrowserLaunchedTimeout, deltaTimeSec = ");
        stringBuilder.append(deltaTimeSec);
        LOGD(stringBuilder.toString());
        if (1 != Global.getInt(this.mContext.getContentResolver(), "hw_disable_portal", 0)) {
            if ("CMCC".equalsIgnoreCase(SystemProperties.get("ro.config.operators", ""))) {
                if ("CMCC".equals(WifiProCommonUtils.getCurrentSsid(this.mWifiManager))) {
                    return;
                }
            }
            if (Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0 || !"true".equals(Systemex.getString(this.mContext.getContentResolver(), "wifi.challenge.required"))) {
                synchronized (this.mCurrentTopUidLock) {
                    if ("com.huawei.hiskytone".equals(this.mCurrentPackageName)) {
                    }
                }
            }
        }
    }

    private void handlePortalOutOfRange() {
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

    private void showPortalStatusBar(boolean updated) {
        if (this.mPopUpNotifyWifiConfig != null && !TextUtils.isEmpty(this.mPopUpNotifyWifiConfig.SSID) && !this.mPopUpNotifyWifiConfig.SSID.equals("<unknown ssid>")) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("showPortalStatusBar, portal network = ");
            stringBuilder.append(this.mPopUpNotifyWifiConfig.configKey());
            LOGD(stringBuilder.toString());
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cancelPortalNotifyStatusBar, nid = ");
            stringBuilder.append(this.mPortalNotificationId);
            stringBuilder.append(", ssid = ");
            stringBuilder.append(this.mPopUpNotifyWifiConfig.configKey());
            LOGD(stringBuilder.toString());
            this.mWifiProUIDisplayManager.cancelPortalNotificationStatusBar(PORTAL_STATUS_BAR_TAG, this.mPortalNotificationId);
            notifyPortalStatusChanged(false, this.mPopUpNotifyWifiConfig.configKey(), this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp > 0);
        }
    }

    private void removeDelayedMessage(int reason) {
        if (reason == 103) {
            if (this.mHandler.hasMessages(101)) {
                LOGW("MSG_CHECK_PORTAL_NETWORK msg removed");
                this.mHandler.removeMessages(101);
            }
            if (this.mHandler.hasMessages(111)) {
                LOGW("MSG_LAUNCH_BROWSER msg removed");
                this.mHandler.removeMessages(111);
            }
            if (this.mHandler.hasMessages(106)) {
                LOGW("MSG_PORTAL_STATUS_BAR msg removed");
                this.mHandler.removeMessages(106);
            }
            if (this.mHandler.hasMessages(113)) {
                this.mHandler.removeMessages(113);
            }
        }
    }

    private void handleRespCodeForPortalCheck(int counter, int respCode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleRespCodeForPortalCheck, counter = ");
        stringBuilder.append(counter);
        stringBuilder.append(", respCode = ");
        stringBuilder.append(respCode);
        LOGD(stringBuilder.toString());
        if (respCode == HwSelfCureUtils.RESET_LEVEL_MIDDLE_REASSOC) {
            if (this.mCurrentCheckWifiConfig != null) {
                this.mCurrentCheckWifiConfig.portalCheckStatus = 1;
                this.mCurrentCheckWifiConfig.noInternetAccess = false;
                this.mCurrentCheckWifiConfig.validatedInternetAccess = true;
                this.mCurrentCheckWifiConfig.wifiProNoInternetAccess = false;
                this.mCurrentCheckWifiConfig.lastHasInternetTimestamp = System.currentTimeMillis();
                updateWifiConfig(this.mCurrentCheckWifiConfig);
                synchronized (this.mPortalDatabaseLock) {
                    this.mPortalUnauthDatabase.clear();
                }
                this.mCurrentCheckWifiConfig = null;
                switchWifiForeground();
            }
        } else if (WifiProCommonUtils.isRedirectedRespCode(respCode)) {
            boolean empty = false;
            if (this.mCurrentCheckWifiConfig != null) {
                empty = WifiProCommonUtils.matchedRequestByHistory(this.mCurrentCheckWifiConfig.internetHistory, 103);
                if (empty) {
                    this.mCurrentCheckWifiConfig.portalCheckStatus = 2;
                    this.mCurrentCheckWifiConfig.portalNetwork = true;
                    this.mCurrentCheckWifiConfig.noInternetAccess = false;
                    this.mCurrentCheckWifiConfig.wifiProNoInternetAccess = true;
                    this.mCurrentCheckWifiConfig.wifiProNoInternetReason = 1;
                    this.mCurrentCheckWifiConfig.internetHistory = WifiProCommonUtils.insertWifiConfigHistory(this.mCurrentCheckWifiConfig.internetHistory, 2);
                    updateWifiConfig(this.mCurrentCheckWifiConfig);
                }
            }
            if (this.mNetworkPropertyRechecker != null) {
                this.mPortalUsedUrl = this.mNetworkPropertyRechecker.getCaptiveUsedServer();
                this.mPortalRedirectedUrl = this.mNetworkPropertyRechecker.getPortalRedirectedUrl();
                this.mPortalRespCode = this.mNetworkPropertyRechecker.getRawHttpRespCode();
                this.mFirstDetected = empty;
            }
            if (this.mPopUpNotifyWifiConfig == null && this.mCurrentCheckWifiConfig != null) {
                this.mPopUpNotifyWifiConfig = this.mCurrentCheckWifiConfig;
                this.mPopUpWifiRssi = WifiProCommonUtils.getCurrentRssi(this.mWifiManager);
                this.mHandler.sendMessage(Message.obtain(this.mHandler, 106, Boolean.valueOf(false)));
            }
            if (counter < 3) {
                this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 112, 101, counter + 1), DELAYED_TIME_HTTP_RECHECK);
            } else {
                if (this.mCurrentCheckWifiConfig != null) {
                    saveCurrentUnauthPortalBssid();
                    if (!(this.mPopUpNotifyWifiConfig == null || this.mPopUpNotifyWifiConfig.configKey() == null || this.mPopUpNotifyWifiConfig.configKey().equals(this.mCurrentCheckWifiConfig.configKey()))) {
                        int currCheckNetworkRssi = WifiProCommonUtils.getCurrentRssi(this.mWifiManager);
                        if ((this.mPopUpNotifyWifiConfig.lastHasInternetTimestamp == 0 && currCheckNetworkRssi > -80) || (currCheckNetworkRssi >= -75 && currCheckNetworkRssi - this.mPopUpWifiRssi >= 8)) {
                            this.mPopUpNotifyWifiConfig = this.mCurrentCheckWifiConfig;
                            this.mPopUpWifiRssi = currCheckNetworkRssi;
                            this.mHandler.sendMessage(Message.obtain(this.mHandler, 106, Boolean.valueOf(true)));
                        }
                    }
                }
                this.mHandler.sendMessage(Message.obtain(this.mHandler, 102));
            }
        } else if (WifiProCommonUtils.unreachableRespCode(respCode)) {
            if (counter < 3) {
                this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 112, 101, counter + 1), DELAYED_TIME_HTTP_RECHECK);
            } else if (this.mCurrentCheckWifiConfig != null) {
                if (WifiProCommonUtils.matchedRequestByHistory(this.mCurrentCheckWifiConfig.internetHistory, 103)) {
                    this.mCurrentCheckWifiConfig.noInternetAccess = true;
                    this.mCurrentCheckWifiConfig.validatedInternetAccess = false;
                    this.mCurrentCheckWifiConfig.wifiProNoInternetAccess = true;
                    this.mCurrentCheckWifiConfig.wifiProNoInternetReason = 0;
                    this.mCurrentCheckWifiConfig.wifiProNoHandoverNetwork = false;
                    this.mCurrentCheckWifiConfig.internetHistory = WifiProCommonUtils.insertWifiConfigHistory(this.mCurrentCheckWifiConfig.internetHistory, 0);
                    updateWifiConfig(this.mCurrentCheckWifiConfig);
                } else {
                    saveCurrentUnauthPortalBssid();
                    if (this.mPopUpNotifyWifiConfig == null) {
                        this.mPopUpNotifyWifiConfig = this.mCurrentCheckWifiConfig;
                        this.mPopUpWifiRssi = WifiProCommonUtils.getCurrentRssi(this.mWifiManager);
                        this.mHandler.sendMessage(Message.obtain(this.mHandler, 106, Boolean.valueOf(false)));
                    }
                }
                this.mHandler.sendMessage(Message.obtain(this.mHandler, 102));
            }
        }
    }

    private void handleRespCodeForNoInternetCheck(int counter, int respCode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleRespCodeForNoInternetCheck, counter = ");
        stringBuilder.append(counter);
        stringBuilder.append(", resp = ");
        stringBuilder.append(respCode);
        LOGD(stringBuilder.toString());
        if (this.mCurrentCheckWifiConfig != null) {
            this.mCurrentCheckWifiConfig.internetRecoveryCheckTimestamp = System.currentTimeMillis();
            this.mCurrentCheckWifiConfig.internetRecoveryStatus = respCode == HwSelfCureUtils.RESET_LEVEL_MIDDLE_REASSOC ? 5 : 4;
            if (this.mCurrentCheckWifiConfig.internetRecoveryStatus == 5) {
                this.mCurrentCheckWifiConfig.lastHasInternetTimestamp = System.currentTimeMillis();
                this.mCurrentCheckWifiConfig.noInternetAccess = false;
                this.mCurrentCheckWifiConfig.validatedInternetAccess = true;
                this.mCurrentCheckWifiConfig.internetHistory = WifiProCommonUtils.insertWifiConfigHistory(this.mCurrentCheckWifiConfig.internetHistory, 1);
            }
            updateWifiConfig(this.mCurrentCheckWifiConfig);
            this.mHandler.sendMessage(Message.obtain(this.mHandler, 102));
        }
    }

    private void saveCurrentUnauthPortalBssid() {
        synchronized (this.mPortalDatabaseLock) {
            if (this.mPortalUnauthDatabase.containsKey(this.mCurrentCheckWifiConfig.configKey())) {
                ((ArrayList) this.mPortalUnauthDatabase.get(this.mCurrentCheckWifiConfig.configKey())).add(WifiProCommonUtils.getCurrentBssid(this.mWifiManager));
            } else {
                ArrayList<String> unauthBssidsList = new ArrayList();
                unauthBssidsList.add(WifiProCommonUtils.getCurrentBssid(this.mWifiManager));
                this.mPortalUnauthDatabase.put(this.mCurrentCheckWifiConfig.configKey(), unauthBssidsList);
            }
        }
    }

    private void updateUnauthPortalDatabase(String configKey, int maxRssi) {
        if (maxRssi <= -80) {
            synchronized (this.mPortalDatabaseLock) {
                String currKey;
                ArrayList<String> releasedConfigKeys = new ArrayList();
                for (Entry entry : this.mPortalUnauthDatabase.entrySet()) {
                    currKey = (String) entry.getKey();
                    if (!(currKey == null || configKey == null || currKey.equals(configKey))) {
                        releasedConfigKeys.add(currKey);
                    }
                }
                for (int i = 0; i < releasedConfigKeys.size(); i++) {
                    currKey = (String) releasedConfigKeys.get(i);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateUnauthPortalDatabase, key = ");
                    stringBuilder.append(currKey);
                    LOGD(stringBuilder.toString());
                    this.mPortalUnauthDatabase.remove(currKey);
                }
            }
        }
    }

    private void switchWifiForeground() {
        this.mWifiStateMachine.sendMessage(131873);
        this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 113), DELAYED_TIME_WIFI_ICON);
    }

    private void notifyPortalStatusChanged(boolean popUp, String configKey, boolean hasInternetAccess) {
        if (this.mSavedNetworkEvaluator != null) {
            this.mSavedNetworkEvaluator.portalNotifyChanged(popUp, configKey, hasInternetAccess);
        }
    }

    public void notifyNetworkDisconnected() {
        if (this.mBackGroundRunning.get()) {
            HwSelfCureEngine.getInstance().notifyWifiDisconnected();
            this.mHandler.sendMessage(Message.obtain(this.mHandler, 103));
        }
    }

    public void notifyEnableSameNetworkId(int netId) {
        this.mHandler.sendMessage(Message.obtain(this.mHandler, 115, netId, 0));
    }

    public void updatePopUpNetworkRssi(String configKey, int maxRssi) {
        this.mHandler.sendMessage(Message.obtain(this.mHandler, 114, maxRssi, 0, configKey));
    }

    public void notifyPortalNetworkConnected() {
        this.mHandler.sendMessage(Message.obtain(this.mHandler, 120));
    }

    public boolean allowCheckPortalNetwork(String configKey, String bssid) {
        synchronized (this.mPortalDatabaseLock) {
            for (Entry entry : this.mPortalUnauthDatabase.entrySet()) {
                String currKey = (String) entry.getKey();
                if (!(currKey == null || configKey == null || !currKey.equals(configKey))) {
                    ArrayList<String> unauthBssids = (ArrayList) this.mPortalUnauthDatabase.get(currKey);
                    if (!(unauthBssids == null || bssid == null)) {
                        if (unauthBssids.size() >= 3) {
                            return false;
                        }
                        for (int i = 0; i < unauthBssids.size(); i++) {
                            if (bssid.equals((String) unauthBssids.get(i))) {
                                return false;
                            }
                        }
                        continue;
                    }
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
            Message msg = Message.obtain();
            msg.what = HwWifiStateMachine.CMD_UPDATE_WIFIPRO_CONFIGURATIONS;
            msg.obj = config;
            this.mWifiStateMachine.sendMessage(msg);
        }
    }

    private boolean useOperatorOverSea() {
        String operator = TelephonyManager.getDefault().getNetworkOperator();
        if (operator == null || operator.length() <= 0) {
            if ("CN".equalsIgnoreCase(WifiProCommonUtils.getProductLocale())) {
                return false;
            }
        } else if (operator.startsWith(COUNTRY_CODE_CN)) {
            return false;
        }
        return true;
    }

    public void saveAutoJoinTargetBssid(WifiConfiguration config, String targetBssid) {
        synchronized (this.mAutoConnectFilterLock) {
            this.mCurrentAutoJoinTargetBssid = targetBssid;
            if (!(config == null || config.getNetworkSelectionStatus().isNetworkEnabled())) {
                this.mAutoJoinDisabledNetworkCnt++;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("saveAutoJoinTargetBssid, autoJoinDisabedNetworkCnt = ");
            stringBuilder.append(this.mAutoJoinDisabledNetworkCnt);
            LOGD(stringBuilder.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x0049 A:{Catch:{ all -> 0x001b }} */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0020 A:{Catch:{ all -> 0x001b }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isAutoJoinAllowedSetTargetBssid(WifiConfiguration config, String targetBssid) {
        synchronized (this.mAutoConnectFilterLock) {
            boolean matchedBlacklistSsid;
            if (config != null) {
                try {
                    if (config.configKey() != null && config.configKey().equals(this.mCurrentBlacklistConfigKey)) {
                        matchedBlacklistSsid = true;
                        if (matchedBlacklistSsid) {
                            this.mCurrentAutoJoinTargetBssid = targetBssid;
                            boolean matchedHuaweiEmployee = (config == null || config.configKey() == null || !config.configKey().equals(KEY_HUAWEI_EMPLOYEE)) ? false : true;
                            if (matchedHuaweiEmployee) {
                                return true;
                            }
                            return false;
                        }
                        this.mCurrentAutoJoinTargetBssid = targetBssid;
                        if (!config.getNetworkSelectionStatus().isNetworkEnabled()) {
                            this.mAutoJoinDisabledNetworkCnt++;
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("isAutoJoinAllowedSetTargetBssid, autoJoinDisabedNetworkCnt = ");
                        stringBuilder.append(this.mAutoJoinDisabledNetworkCnt);
                        LOGD(stringBuilder.toString());
                        return true;
                    }
                } finally {
                }
            }
            matchedBlacklistSsid = false;
            if (matchedBlacklistSsid) {
            }
        }
    }

    public boolean isBssidMatchedBlacklist(String bssid) {
        synchronized (this.mAutoConnectFilterLock) {
            if (WifiProCommonUtils.isInMonitorList(bssid, (String[]) this.mAutoJoinBlacklistBssid.toArray(new String[0]))) {
                return true;
            }
            return false;
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

    public void notifyWifiConnFailedInfo(WifiConfiguration config, String bssid, int rssi, int reason, WifiConnectivityManager wcm) {
        if (config != null && config.configKey() != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyWifiConnFailedInfo, rssi = ");
            stringBuilder.append(rssi);
            stringBuilder.append(", reason = ");
            stringBuilder.append(reason);
            LOGD(stringBuilder.toString());
            synchronized (this.mAutoConnectFilterLock) {
                if (reason == 3 || reason == 2) {
                    if (!(this.mCurrentBlacklistConfigKey == null || this.mCurrentBlacklistConfigKey.equals(config.configKey()))) {
                        this.mAutoJoinDisabledNetworkCnt = 0;
                        this.mAutoJoinBlacklistBssid.clear();
                    }
                    this.mCurrentBlacklistConfigKey = config.configKey();
                    String currBssid = bssid != null ? bssid : this.mCurrentAutoJoinTargetBssid;
                    if (!(currBssid == null || WifiProCommonUtils.isInMonitorList(currBssid, (String[]) this.mAutoJoinBlacklistBssid.toArray(new String[0])))) {
                        this.mAutoJoinBlacklistBssid.add(currBssid);
                    }
                    this.mHandler.removeMessages(118);
                    this.mHandler.sendMessageDelayed(Message.obtain(this.mHandler, 118), 240000);
                    NetworkSelectionStatus status = config.getNetworkSelectionStatus();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("notifyWifiConnFailedInfo, isNetworkEnabled = ");
                    stringBuilder2.append(status.isNetworkEnabled());
                    stringBuilder2.append(", cnt = ");
                    stringBuilder2.append(this.mAutoJoinDisabledNetworkCnt);
                    LOGD(stringBuilder2.toString());
                    if (!(status.isNetworkEnabled() || this.mAutoJoinDisabledNetworkCnt > 2 || wcm == null)) {
                        LOGD("notifyWifiConnFailedInfo, start scan immediately for auto join other bssid again!!!");
                        wcm.handleConnectionStateChanged(2);
                    }
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

    private void launchBrowserForPortalLogin(String configKey) {
        try {
            URL url = new URL("http://connectivitycheck.platform.hicloud.com/generate_204");
            if (!TextUtils.isEmpty(this.mPortalUsedUrl) && this.mPortalUsedUrl.startsWith("http")) {
                Log.d(TAG, "launchBrowserForPortalLogin: use the portal url from the settings");
                url = new URL(this.mPortalUsedUrl);
            } else if (useOperatorOverSea()) {
                url = new URL("http://connectivitycheck.gstatic.com/generate_204");
            }
            String packageName = "com.android.browser";
            String className = "com.android.browser.BrowserActivity";
            if (Utils.isPackageInstalled(HwPortalExceptionManager.BROWSER_PACKET_NAME, this.mContext)) {
                packageName = HwPortalExceptionManager.BROWSER_PACKET_NAME;
                className = "com.huawei.browser.Main";
            }
            Secure.putInt(this.mContext.getContentResolver(), "HW_WIFI_PORTAL_FLAG", 1);
            HwPortalExceptionManager.getInstance(this.mContext).notifyPortalConnectedInfo(configKey, this.mFirstDetected, this.mPortalRespCode, this.mPortalRedirectedUrl);
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url.toString()));
            intent.setFlags(272629760);
            intent.putExtra("launch_from", "wifi_portal");
            try {
                intent.setClassName(packageName, className);
                this.mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                try {
                    Intent intentOthers = new Intent("android.intent.action.VIEW", Uri.parse(url.toString()));
                    intentOthers.setFlags(272629760);
                    this.mContext.startActivity(intentOthers);
                } catch (ActivityNotFoundException e3) {
                    Log.e(TAG, "startActivity failed, message", e3);
                }
            }
        } catch (MalformedURLException e2) {
            Log.e(TAG, "launchBrowserForPortalLogin, MalformedURLException!");
        }
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

    public void LOGD(String msg) {
        Log.d(TAG, msg);
    }

    public void LOGW(String msg) {
        Log.w(TAG, msg);
    }
}
