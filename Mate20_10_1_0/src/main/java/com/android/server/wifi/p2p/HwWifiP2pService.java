package com.android.server.wifi.p2p;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hdm.HwDeviceManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.IWifiActionListener;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import android.util.Pair;
import android.util.wifi.HwHiLog;
import android.util.wifi.HwHiSLog;
import android.widget.Toast;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifi.HwQoE.HwQoEUtils;
import com.android.server.wifi.HwSoftApManager;
import com.android.server.wifi.HwWifiCHRHilink;
import com.android.server.wifi.HwWifiCHRService;
import com.android.server.wifi.HwWifiCHRServiceImpl;
import com.android.server.wifi.HwWifiService;
import com.android.server.wifi.HwWifiStateMachine;
import com.android.server.wifi.MSS.HwMSSUtils;
import com.android.server.wifi.SoftApChannelXmlParse;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNativeUtils;
import com.android.server.wifi.WifiRepeater;
import com.android.server.wifi.WifiRepeaterConfigStore;
import com.android.server.wifi.WifiRepeaterController;
import com.android.server.wifi.hwUtil.HwApConfigUtilEx;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifi.hwUtil.WifiCommonUtils;
import com.android.server.wifi.p2p.HwWifiP2pService;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import com.huawei.android.pc.HwPCManagerEx;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;

public class HwWifiP2pService extends WifiP2pServiceImpl {
    private static final String ACTION_DEVICE_DELAY_IDLE = "com.android.server.wifi.p2p.action.DEVICE_DELAY_IDLE";
    /* access modifiers changed from: private */
    public static final String[] AP_NO_DHCP_WHITE_PACKAGE_NAME_LIST = new String[0];
    private static final int BAND_ERROR = -1;
    private static final int BASE = 143360;
    private static final String[] BLACKLIST_P2P_FIND = {"com.hp.android.printservice"};
    private static final String CARRY_DATA_MIRACAST = "1";
    private static final int CHANNEL_ERROR = -1;
    public static final int CMD_BATTERY_CHANGED = 143469;
    public static final int CMD_DEVICE_DELAY_IDLE = 143465;
    public static final int CMD_LINKSPEED_POLL = 143470;
    public static final int CMD_REQUEST_REPEATER_CONFIG = 143463;
    public static final int CMD_RESPONSE_REPEATER_CONFIG = 143464;
    public static final int CMD_SCREEN_OFF = 143467;
    public static final int CMD_SCREEN_ON = 143466;
    public static final int CMD_SELFCURE_GO_CREATE_FAIL = 143471;
    public static final int CMD_SET_REPEATER_CONFIG = 143461;
    public static final int CMD_SET_REPEATER_CONFIG_COMPLETED = 143462;
    private static final int CMD_TYPE_SET = 2;
    public static final int CMD_USER_PRESENT = 143468;
    private static final int CODE_DISABLE_P2P_GC_DHCP = 1006;
    private static final int CODE_GET_GROUP_CONFIG_INFO = 1005;
    private static final int CODE_GET_WIFI_REPEATER_CONFIG = 1001;
    private static final int CODE_REQUEST_DFS_STATUS = 1007;
    private static final int CODE_SET_WIFI_REPEATER_CONFIG = 1002;
    private static final int CODE_UPDATE_DFS_STATUS = 1008;
    private static final int CODE_WIFI_MAGICLINK_CONFIG_IP = 1003;
    private static final int CODE_WIFI_MAGICLINK_RELEASE_IP = 1004;
    /* access modifiers changed from: private */
    public static final int[] COMMON_CHANNELS_2G = {1, 6, 11};
    private static final int CONNECT_FAILURE = -1;
    private static final int CONNECT_SUCCESS = 0;
    private static final int DATA_TYPE_HOMEVISION_SINK_P2P_IE = 3;
    private static final int DATA_TYPE_P2P_BUSINESS = 1;
    private static final int DATA_TYPE_SET_LISTEN_MODE = 4;
    private static final boolean DBG = true;
    private static final long DEFAULT_IDLE_MS = 1800000;
    private static final long DEFAULT_LOW_DATA_TRAFFIC_LINE = 102400;
    private static final long DELAY_IDLE_MS = 60000;
    private static final String DESCRIPTOR = "android.net.wifi.p2p.IWifiP2pManager";
    private static final String[] DISABLE_DHCP_WHITE_PACKAGE_NAME_LIST = new String[0];
    private static final int DISABLE_P2P_GC_DHCP_WAIT_TIME_MS = 10000;
    private static final String[] DISABLE_P2P_RANDOM_MAC_WHITE_PACKAGE_NAME_LIST = {"com.huawei.android.airsharing", "com.huawei.android.mirrorshare"};
    private static final String EXTRA_CLIENT_INFO = "macInfo";
    private static final String EXTRA_CURRENT_TIME = "currentTime";
    private static final String EXTRA_STA_COUNT = "staCount";
    private static final String HUAWEI_WIFI_DEVICE_DELAY_IDLE = "huawei.android.permission.WIFI_DEVICE_DELAY_IDLE";
    /* access modifiers changed from: private */
    public static final boolean HWDBG;
    private static final boolean HWLOGW_E = true;
    private static long INTERVAL_DISALLOW_P2P_FIND = 130000;
    private static final boolean IS_TABLET_WINDOWS_CAST_ENABLED = "tablet".equals(SystemProperties.get("ro.build.characteristics", "default"));
    private static final boolean IS_TV = "tv".equals(SystemProperties.get("ro.build.characteristics", "default"));
    private static final boolean IS_WINDOWS_CAST_ENABLED = SystemProperties.getBoolean("ro.config.hw_emui_cast_mode", false);
    private static final int LEGACYGO_FLAG_INDEX = 4;
    private static final int LINKSPEED_ESTIMATE_TIMES = 4;
    private static final int LINKSPEED_POLL_INTERVAL = 1000;
    private static final int MAGICLINK_CONNECT_AP_DHCP = 1;
    private static final int MAGICLINK_CONNECT_AP_NODHCP = 2;
    private static final String MAGICLINK_CONNECT_GC_AP_MODE = "1";
    private static final String MAGICLINK_CONNECT_GC_GO_MODE = "0";
    private static final int MAGICLINK_CONNECT_GO_NODHCP = 0;
    private static final String MAGICLINK_CREATE_GROUP_160M_FLAG = "w";
    private static final int MAX_P2P_CREATE_GO_FAIL_NUM = 2;
    private static final Boolean NO_REINVOCATION = false;
    private static final String ONEHOP_LISTEN_MODE = "1";
    private static final int P2P_BAND_2G = 0;
    private static final int P2P_BAND_5G = 1;
    private static final int P2P_CHOOSE_CHANNEL_RANDOM = 0;
    private static final int P2P_DEVICE_OF_MIRACAST = 7;
    private static final String PERMISSION_DISABLE_P2P_GC_DHCP = "huawei.android.permission.WIFI_DISABLE_P2P_GC_DHCP";
    private static final String PERMISSION_DISABLE_P2P_RANDOM_MAC = "huawei.android.permission.WIFI_DISABLE_P2P_RANDOM_MAC";
    private static final String PERMISSION_SET_SINK_CONFIG = "huawei.android.permission.HW_SIGNATURE_OR_SYSTEM";
    /* access modifiers changed from: private */
    public static final Boolean RELOAD = true;
    private static final String[] REQUEST_DFS_STATUS_WHITE_PACKAGE_LIST = {"com.huawei.nearby", "com.huawei.android.instantshare"};
    private static final int RUNNING_TASK_NUM_MAX = 1;
    private static final int SEGMENT_LENGTH_MIN = 2;
    private static final String SERVER_ADDRESS_WIFI_BRIDGE = "192.168.43.1";
    private static final String SERVER_ADDRESS_WIFI_BRIDGE_OTHER = "192.168.50.1";
    private static final String[] SET_HWSINK_CONFIG_WHITE_PACKAGE_NAME_LIST = {"com.hisilicon.miracast"};
    private static final String SPLIT_DOT = ",";
    private static final String SPLIT_EQUAL = "=";
    private static final int SYSTEM_UID_VALUE_MAX = 10000;
    private static final String TAG = "HwWifiP2pService";
    /* access modifiers changed from: private */
    public static final Boolean TRY_REINVOCATION = true;
    private static final int WHITELIST_DURATION_MS = 15000;
    private static final int WIFI_DISABLE_P2P_GC_DHCP_FOREVER = 2;
    private static final int WIFI_DISABLE_P2P_GC_DHCP_NONE = 0;
    private static final int WIFI_DISABLE_P2P_GC_DHCP_ONCE = 1;
    private static final int WIFI_REPEATER_CLIENT_JOIN = 0;
    private static final String WIFI_REPEATER_CLIENT_JOIN_ACTION = "com.huawei.wifi.action.WIFI_REPEATER_CLIENT_JOIN";
    private static final int WIFI_REPEATER_CLIENT_LEAVE = 1;
    private static final String WIFI_REPEATER_CLIENT_LEAVE_ACTION = "com.huawei.wifi.action.WIFI_REPEATER_CLIENT_LEAVE";
    private static final int WIFI_REPEATER_MAX_CLIENT = 4;
    private static WifiNativeUtils wifiNativeUtils = EasyInvokeFactory.getInvokeUtils(WifiNativeUtils.class);
    /* access modifiers changed from: private */
    public static WifiP2pServiceUtils wifiP2pServiceUtils = EasyInvokeFactory.getInvokeUtils(WifiP2pServiceUtils.class);
    /* access modifiers changed from: private */
    public AlarmManager mAlarmManager;
    private final BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
        /* class com.android.server.wifi.p2p.HwWifiP2pService.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                HwHiSLog.d(HwWifiP2pService.TAG, false, "onReceive, action:%{public}s", new Object[]{action});
                if (action.equals(HwWifiP2pService.ACTION_DEVICE_DELAY_IDLE)) {
                    HwWifiP2pService.this.mP2pStateMachine.sendMessage((int) HwWifiP2pService.CMD_DEVICE_DELAY_IDLE);
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public String mConfigInfo;
    /* access modifiers changed from: private */
    public Context mContext;
    private String mCurDisbleDhcpPackageName = "";
    /* access modifiers changed from: private */
    public PendingIntent mDefaultIdleIntent;
    /* access modifiers changed from: private */
    public PendingIntent mDelayIdleIntent;
    private HashMap<String, Integer> mDisbleGcDhcpList = new HashMap<>();
    private long mDisbleP2pGcDhcpTime = -10000;
    /* access modifiers changed from: private */
    public HwDfsMonitor mHwDfsMonitor;
    private HwP2pStateMachine mHwP2pStateMachine = null;
    HwWifiCHRService mHwWifiCHRService;
    private String mInterface = "";
    /* access modifiers changed from: private */
    public boolean mIsUsingHwShare = false;
    private boolean mIsWifiRepeaterTetherStarted = false;
    /* access modifiers changed from: private */
    public volatile int mLastLinkSpeed = 0;
    /* access modifiers changed from: private */
    public long mLastRxBytes = 0;
    /* access modifiers changed from: private */
    public long mLastTxBytes = 0;
    /* access modifiers changed from: private */
    public boolean mLegacyGO = false;
    /* access modifiers changed from: private */
    public int mLinkSpeedCounter = 0;
    /* access modifiers changed from: private */
    public int mLinkSpeedPollToken = 0;
    /* access modifiers changed from: private */
    public int[] mLinkSpeedWeights;
    /* access modifiers changed from: private */
    public int[] mLinkSpeeds = new int[4];
    private final Object mLock = new Object();
    private int mMacFilterStaCount = 0;
    private String mMacFilterStr = "";
    /* access modifiers changed from: private */
    public boolean mMagicLinkDeviceFlag = false;
    NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, "WIFI", "");
    /* access modifiers changed from: private */
    public int mP2pCreateGoFailTimes = 0;
    private List<P2pFindProcessInfo> mP2pFindProcessInfoList = null;
    NetworkInfo mP2pNetworkInfo = new NetworkInfo(13, 0, "WIFI_P2P", "");
    private PowerManager mPowerManager = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* class com.android.server.wifi.p2p.HwWifiP2pService.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            NetworkInfo p2pNetworkInfo;
            String action = intent.getAction();
            HwHiSLog.d(HwWifiP2pService.TAG, false, "onReceive, action:%{public}s", new Object[]{action});
            if (action != null) {
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    HwWifiP2pService.this.mP2pStateMachine.sendMessage((int) HwWifiP2pService.CMD_SCREEN_ON);
                } else if (action.equals("android.intent.action.USER_PRESENT")) {
                    HwWifiP2pService.this.mP2pStateMachine.sendMessage((int) HwWifiP2pService.CMD_USER_PRESENT);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    HwWifiP2pService.this.mP2pStateMachine.sendMessage((int) HwWifiP2pService.CMD_SCREEN_OFF);
                } else if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (networkInfo != null) {
                        HwWifiP2pService.this.mNetworkInfo = networkInfo;
                    }
                } else if (action.equals("android.net.wifi.p2p.CONNECTION_STATE_CHANGE") && (p2pNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo")) != null) {
                    HwWifiP2pService.this.mP2pNetworkInfo = p2pNetworkInfo;
                }
            }
        }
    };
    private String mTetherInterfaceName;
    /* access modifiers changed from: private */
    public List<Pair<String, Long>> mValidDeivceList = new ArrayList();
    /* access modifiers changed from: private */
    public Handler mWifiP2pDataTrafficHandler;
    /* access modifiers changed from: private */
    public WifiRepeater mWifiRepeater;
    private long mWifiRepeaterBeginWorkTime = 0;
    private Collection<WifiP2pDevice> mWifiRepeaterClientList = new ArrayList();
    /* access modifiers changed from: private */
    public AsyncChannel mWifiRepeaterConfigChannel;
    private WifiRepeaterConfigStore mWifiRepeaterConfigStore;
    /* access modifiers changed from: private */
    public boolean mWifiRepeaterEnabled = false;
    private long mWifiRepeaterEndWorkTime = 0;
    private int mWifiRepeaterFreq = 0;
    HandlerThread wifip2pThread = new HandlerThread("WifiP2pService");

    static /* synthetic */ int access$1608(HwWifiP2pService x0) {
        int i = x0.mLinkSpeedCounter;
        x0.mLinkSpeedCounter = i + 1;
        return i;
    }

    static /* synthetic */ int access$1704(HwWifiP2pService x0) {
        int i = x0.mLinkSpeedPollToken + 1;
        x0.mLinkSpeedPollToken = i;
        return i;
    }

    static {
        boolean z = true;
        if (!Log.HWLog && (!Log.HWModuleLog || !Log.isLoggable(TAG, 3))) {
            z = false;
        }
        HWDBG = z;
    }

    public HwWifiP2pService(Context context, WifiInjector wifiInjector) {
        super(context);
        this.mContext = context;
        if (this.mP2pStateMachine instanceof HwP2pStateMachine) {
            this.mHwP2pStateMachine = this.mP2pStateMachine;
        }
        this.wifip2pThread.start();
        this.mWifiP2pDataTrafficHandler = new WifiP2pDataTrafficHandler(this.wifip2pThread.getLooper());
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mDefaultIdleIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_DEVICE_DELAY_IDLE, (Uri) null), 0);
        this.mDelayIdleIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_DEVICE_DELAY_IDLE, (Uri) null), 0);
        registerForBroadcasts();
        this.mWifiRepeater = new WifiRepeaterController(this.mContext, getP2pStateMachineMessenger());
        this.mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
        this.mLinkSpeedWeights = new int[]{15, 20, 30, 35};
        this.mP2pFindProcessInfoList = new ArrayList();
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mHwDfsMonitor = HwDfsMonitor.createHwDfsMonitor(this.mContext);
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        this.mContext.registerReceiver(this.mAlarmReceiver, new IntentFilter(ACTION_DEVICE_DELAY_IDLE), HUAWEI_WIFI_DEVICE_DELAY_IDLE, null);
    }

    public boolean isWifiRepeaterStarted() {
        return 1 == Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_repeater_on", 0);
    }

    /* access modifiers changed from: private */
    public boolean shouldDisconnectWifiP2p() {
        if (!this.mWifiRepeaterEnabled) {
            return true;
        }
        HwHiSLog.i(TAG, false, "WifiRepeater is open.", new Object[0]);
        return false;
    }

    private class WifiP2pDataTrafficHandler extends Handler {
        private static final int MSG_UPDATA_DATA_TAFFIC = 0;

        WifiP2pDataTrafficHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                HwWifiP2pService.this.handleUpdataDateTraffic();
            }
        }
    }

    private boolean checkP2pDataTrafficLine() {
        WifiP2pGroup mWifiP2pGroup = wifiP2pServiceUtils.getmGroup(this.mP2pStateMachine);
        if (mWifiP2pGroup == null) {
            HwHiSLog.d(TAG, false, "WifiP2pGroup is null.", new Object[0]);
            return true;
        }
        this.mInterface = mWifiP2pGroup.getInterface();
        HwHiSLog.d(TAG, false, "mInterface: %{public}s", new Object[]{this.mInterface});
        long txBytes = TrafficStats.getTxBytes(this.mInterface);
        long rxBytes = TrafficStats.getRxBytes(this.mInterface);
        long txSpeed = txBytes - this.mLastTxBytes;
        long rxSpeed = rxBytes - this.mLastRxBytes;
        HwHiSLog.d(TAG, false, " txBytes:%{public}s rxBytes:%{public}s txSpeed:%{public}s rxSpeed:%{public}s mLowDataTrafficLine:%{public}s DELAY_IDLE_MS:%{public}s", new Object[]{String.valueOf(txBytes), String.valueOf(rxBytes), String.valueOf(txSpeed), String.valueOf(rxSpeed), String.valueOf((long) DEFAULT_LOW_DATA_TRAFFIC_LINE), String.valueOf((long) DELAY_IDLE_MS)});
        if (this.mLastTxBytes == 0 && this.mLastRxBytes == 0) {
            this.mLastTxBytes = txBytes;
            this.mLastRxBytes = rxBytes;
            return false;
        }
        this.mLastTxBytes = txBytes;
        this.mLastRxBytes = rxBytes;
        return txSpeed + rxSpeed < DEFAULT_LOW_DATA_TRAFFIC_LINE;
    }

    /* access modifiers changed from: private */
    public void handleUpdataDateTraffic() {
        HwHiSLog.d(TAG, false, "handleUpdataDateTraffic", new Object[0]);
        if (!this.mP2pNetworkInfo.isConnected()) {
            HwHiSLog.d(TAG, false, "p2p is disconnected.", new Object[0]);
        } else if (!checkP2pDataTrafficLine() || isPCManagerRunning()) {
            this.mAlarmManager.setExact(0, System.currentTimeMillis() + DELAY_IDLE_MS, this.mDelayIdleIntent);
        } else {
            HwHiSLog.w(TAG, false, "remove group, disconnect wifi p2p", new Object[0]);
            this.mP2pStateMachine.sendMessage(139280);
        }
    }

    private boolean isPCManagerRunning() {
        if (IS_WINDOWS_CAST_ENABLED && HwPCManagerEx.isInWindowsCastMode()) {
            return true;
        }
        if (!IS_TABLET_WINDOWS_CAST_ENABLED || !HwPCManagerEx.isInSinkWindowsCastMode()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public Object getHwP2pStateMachine(String name, Looper looper, boolean p2pSupported) {
        return new HwP2pStateMachine(name, looper, p2pSupported);
    }

    /* access modifiers changed from: protected */
    public boolean handleDefaultStateMessage(Message message) {
        HwP2pStateMachine hwP2pStateMachine = this.mHwP2pStateMachine;
        if (hwP2pStateMachine != null) {
            return hwP2pStateMachine.handleDefaultStateMessage(message);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean handleP2pNotSupportedStateMessage(Message message) {
        HwP2pStateMachine hwP2pStateMachine = this.mHwP2pStateMachine;
        if (hwP2pStateMachine != null) {
            return hwP2pStateMachine.handleP2pNotSupportedStateMessage(message);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean handleInactiveStateMessage(Message message) {
        HwP2pStateMachine hwP2pStateMachine = this.mHwP2pStateMachine;
        if (hwP2pStateMachine != null) {
            return hwP2pStateMachine.handleInactiveStateMessage(message);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean handleP2pEnabledStateExMessage(Message message) {
        HwP2pStateMachine hwP2pStateMachine = this.mHwP2pStateMachine;
        if (hwP2pStateMachine != null) {
            return hwP2pStateMachine.handleP2pEnabledStateExMessage(message);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean handleGroupNegotiationStateExMessage(Message message) {
        HwP2pStateMachine hwP2pStateMachine = this.mHwP2pStateMachine;
        if (hwP2pStateMachine != null) {
            return hwP2pStateMachine.handleGroupNegotiationStateExMessage(message);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean handleGroupCreatedStateExMessage(Message message) {
        HwP2pStateMachine hwP2pStateMachine = this.mHwP2pStateMachine;
        if (hwP2pStateMachine != null) {
            return hwP2pStateMachine.handleGroupCreatedStateExMessage(message);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean handleOngoingGroupRemovalStateExMessage(Message message) {
        HwP2pStateMachine hwP2pStateMachine = this.mHwP2pStateMachine;
        if (hwP2pStateMachine != null) {
            return hwP2pStateMachine.handleOngoingGroupRemovalStateExMessage(message);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void sendGroupConfigInfo(WifiP2pGroup mGroup) {
        this.mConfigInfo = mGroup.getNetworkName() + "\n" + mGroup.getOwner().deviceAddress + "\n" + mGroup.getPassphrase() + "\n" + mGroup.getFrequency();
        this.mContext.sendBroadcastAsUser(new Intent("android.net.wifi.p2p.CONFIG_INFO"), UserHandle.ALL, "com.huawei.instantshare.permission.ACCESS_INSTANTSHARE");
    }

    /* access modifiers changed from: private */
    public void sendInterfaceCreatedBroadcast(String ifName) {
        logd("sending interface created broadcast", new Object[0]);
        Intent intent = new Intent("android.net.wifi.p2p.INTERFACE_CREATED");
        intent.putExtra("p2pInterfaceName", ifName);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.instantshare.permission.ACCESS_INSTANTSHARE");
    }

    /* access modifiers changed from: private */
    public void sendNetworkConnectedBroadcast(String bssid) {
        logd("sending network connected broadcast", new Object[0]);
        Intent intent = new Intent("android.net.wifi.p2p.NETWORK_CONNECTED_ACTION");
        intent.putExtra("bssid", bssid);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.instantshare.permission.ACCESS_INSTANTSHARE");
    }

    /* access modifiers changed from: private */
    public void sendNetworkDisconnectedBroadcast(String bssid) {
        logd("sending network disconnected broadcast", new Object[0]);
        Intent intent = new Intent("android.net.wifi.p2p.NETWORK_DISCONNECTED_ACTION");
        intent.putExtra("bssid", bssid);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.instantshare.permission.ACCESS_INSTANTSHARE");
    }

    /* access modifiers changed from: private */
    public void sendLinkSpeedChangedBroadcast() {
        logd("sending linkspeed changed broadcast " + this.mLastLinkSpeed, new Object[0]);
        Intent intent = new Intent("com.huawei.net.wifi.p2p.LINK_SPEED");
        intent.putExtra("linkSpeed", this.mLastLinkSpeed);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.wfd.permission.ACCESS_P2P_LINKSPEED");
    }

    /* access modifiers changed from: private */
    public void sendHwP2pDeviceExInfoBroadcast(byte[] info) {
        logd("sending HwP2pDeviceExInfo broadcast ", new Object[0]);
        Intent intent = new Intent("com.huawei.net.wifi.p2p.peers.hw.extend.info");
        intent.putExtra("exinfo", info);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.wfd.permission.ACCESS_P2P_LINKSPEED");
    }

    /* access modifiers changed from: protected */
    public void handleTetheringDhcpRange(String[] tetheringDhcpRanges) {
        for (int i = tetheringDhcpRanges.length - 1; i >= 0; i--) {
            if ("192.168.49.2".equals(tetheringDhcpRanges[i])) {
                tetheringDhcpRanges[i] = "192.168.49.101";
                return;
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean handleClientHwMessage(Message message) {
        switch (message.what) {
            case 141264:
            case 141265:
            case 141266:
            case 141267:
            case 141268:
            case 141269:
            case 141270:
            case 141271:
            case 141272:
            case 141273:
                this.mP2pStateMachine.sendMessage(message);
                return true;
            default:
                HwHiSLog.d(TAG, false, "ClientHandler.handleMessage ignoring msg=%{public}s", new Object[]{message.toString()});
                return false;
        }
    }

    /* access modifiers changed from: package-private */
    public class HwP2pStateMachine extends WifiP2pServiceImpl.P2pStateMachine {
        private static final int DEFAULT_GROUP_OWNER_INTENT = 6;
        private Message mCreatPskGroupMsg;

        HwP2pStateMachine(String name, Looper looper, boolean p2pSupported) {
            super(HwWifiP2pService.this, name, looper, p2pSupported);
        }

        public boolean handleDefaultStateMessage(Message message) {
            switch (message.what) {
                case HwWifiStateMachine.CMD_STOP_WIFI_REPEATER:
                    if (HwWifiP2pService.this.getWifiRepeaterEnabled()) {
                        sendMessage(139280);
                        break;
                    }
                    break;
                case 141264:
                    String addDeviceAddress = message.getData().getString("avlidDevice");
                    HwWifiP2pService.this.logd("add p2p deivce valid addDeviceAddress = %{private}s", addDeviceAddress);
                    addP2PValidDevice(addDeviceAddress);
                    break;
                case 141265:
                    String removeDeviceAddress = message.getData().getString("avlidDevice");
                    HwWifiP2pService.this.logd("remove p2p valid deivce removeDeviceAddress = %{private}s", removeDeviceAddress);
                    removeP2PValidDevice(removeDeviceAddress);
                    break;
                case 141266:
                    HwWifiP2pService.this.logd("clear p2p valid deivce", new Object[0]);
                    clearP2PValidDevice();
                    break;
                case 141268:
                case 141270:
                    HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139278, 2);
                    break;
                case 141269:
                    HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139272, 2);
                    break;
                case 141271:
                    HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139281, 2);
                    if (HwWifiP2pService.this.getWifiRepeaterEnabled()) {
                        HwWifiP2pService.this.stopWifiRepeater(HwWifiP2pService.wifiP2pServiceUtils.getmGroup(HwWifiP2pService.this.mP2pStateMachine));
                        break;
                    }
                    break;
                case HwWifiP2pService.CMD_DEVICE_DELAY_IDLE /*{ENCODED_INT: 143465}*/:
                    HwWifiP2pService.this.mWifiP2pDataTrafficHandler.sendMessage(Message.obtain(HwWifiP2pService.this.mWifiP2pDataTrafficHandler, 0));
                    break;
                case HwWifiP2pService.CMD_SCREEN_ON /*{ENCODED_INT: 143466}*/:
                    HwHiSLog.d(HwWifiP2pService.TAG, false, "cancel alarm.", new Object[0]);
                    HwWifiP2pService.this.mAlarmManager.cancel(HwWifiP2pService.this.mDefaultIdleIntent);
                    HwWifiP2pService.this.mAlarmManager.cancel(HwWifiP2pService.this.mDelayIdleIntent);
                    break;
                case HwWifiP2pService.CMD_SCREEN_OFF /*{ENCODED_INT: 143467}*/:
                    long unused = HwWifiP2pService.this.mLastTxBytes = 0;
                    long unused2 = HwWifiP2pService.this.mLastRxBytes = 0;
                    if (HwWifiP2pService.this.shouldDisconnectWifiP2p()) {
                        if (HwWifiP2pService.this.mNetworkInfo.getDetailedState() != NetworkInfo.DetailedState.CONNECTED || !HwWifiP2pService.this.mP2pNetworkInfo.isConnected()) {
                            if (HwWifiP2pService.this.mP2pNetworkInfo.isConnected()) {
                                HwHiSLog.d(HwWifiP2pService.TAG, false, "start to removeP2PGroup.", new Object[0]);
                                HwWifiP2pService.this.handleUpdataDateTraffic();
                                break;
                            }
                        } else {
                            WifiP2pGroup wifiP2pGroup = HwWifiP2pService.wifiP2pServiceUtils.getmGroup(HwWifiP2pService.this.mP2pStateMachine);
                            long delayTimeMs = HwWifiP2pService.DEFAULT_IDLE_MS;
                            if (wifiP2pGroup != null && wifiP2pGroup.isGroupOwner() && wifiP2pGroup.getClientList().size() == 0) {
                                delayTimeMs = HwQoEService.GAME_RTT_NOTIFY_INTERVAL;
                            }
                            HwHiSLog.d(HwWifiP2pService.TAG, false, "set default idle timer: %{public}s ms", new Object[]{String.valueOf(delayTimeMs)});
                            HwWifiP2pService.this.mAlarmManager.set(0, System.currentTimeMillis() + delayTimeMs, HwWifiP2pService.this.mDefaultIdleIntent);
                            break;
                        }
                    }
                    break;
                case 147459:
                    HwWifiP2pService.this.sendNetworkConnectedBroadcast((String) message.obj);
                    break;
                case 147460:
                    HwWifiP2pService.this.sendNetworkDisconnectedBroadcast((String) message.obj);
                    break;
                case 147558:
                    updatePersistentNetworks(true);
                    break;
                case 147577:
                    break;
                default:
                    HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                    hwWifiP2pService.loge("Unhandled message " + message, new Object[0]);
                    return false;
            }
            return true;
        }

        public boolean handleP2pEnabledStateExMessage(Message message) {
            int i = message.what;
            if (i == 143471) {
                selfcureP2pGoCreateFail();
                return true;
            } else if (i != 147577) {
                HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                hwWifiP2pService.loge("Unhandled message " + message, new Object[0]);
                return false;
            } else {
                HwWifiP2pService.this.sendHwP2pDeviceExInfoBroadcast((byte[]) message.obj);
                return true;
            }
        }

        public boolean handleOngoingGroupRemovalStateExMessage(Message message) {
            int i = message.what;
            if (i != 141268) {
                if (i != 141271) {
                    HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                    hwWifiP2pService.loge("Unhandled message " + message, new Object[0]);
                    return false;
                }
                replyToMessage(message, 139282);
                return true;
            } else if (HwWifiP2pService.this.getWifiRepeaterEnabled()) {
                return true;
            } else {
                deferMessage(message);
                return true;
            }
        }

        public boolean handleGroupNegotiationStateExMessage(Message message) {
            if (message.what != 141271) {
                HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                hwWifiP2pService.loge("Unhandled message " + message, new Object[0]);
                return false;
            }
            logd(getName() + " MAGICLINK_REMOVE_GC_GROUP");
            String unused = HwWifiP2pService.this.mConfigInfo = "";
            if (!(message.obj instanceof Bundle)) {
                return true;
            }
            String p2pInterface = ((Bundle) message.obj).getString("iface");
            logd(getName() + "p2pInterface :" + p2pInterface);
            if (p2pInterface == null || p2pInterface.equals("")) {
                HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139281, 0);
                transitionTo(HwWifiP2pService.wifiP2pServiceUtils.getmInactiveState(HwWifiP2pService.this.mP2pStateMachine));
                return true;
            }
            logd(getName() + " MAGICLINK_REMOVE_GC_GROUP,p2pInterface !=null,now remove it");
            if (this.mWifiNative.p2pGroupRemove(p2pInterface)) {
                replyToMessage(message, 139282);
                HwWifiP2pService.wifiP2pServiceUtils.sendP2pConnectionChangedBroadcast(HwWifiP2pService.this.mP2pStateMachine);
            } else {
                HwWifiP2pService.wifiP2pServiceUtils.handleGroupRemoved(HwWifiP2pService.this.mP2pStateMachine);
                HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139281, 0);
            }
            transitionTo(HwWifiP2pService.wifiP2pServiceUtils.getmInactiveState(HwWifiP2pService.this.mP2pStateMachine));
            return true;
        }

        public boolean handleGroupCreatedStateExMessage(Message message) {
            int i = message.what;
            if (i == 141271) {
                logd(getName() + " MAGICLINK_REMOVE_GC_GROUP");
                HwWifiP2pService.wifiP2pServiceUtils.enableBTCoex(HwWifiP2pService.this.mP2pStateMachine);
                String unused = HwWifiP2pService.this.mConfigInfo = "";
                if (this.mWifiNative.p2pGroupRemove(HwWifiP2pService.wifiP2pServiceUtils.getmGroup(HwWifiP2pService.this.mP2pStateMachine).getInterface())) {
                    transitionTo(HwWifiP2pService.wifiP2pServiceUtils.getmOngoingGroupRemovalState(HwWifiP2pService.this.mP2pStateMachine));
                    replyToMessage(message, 139282);
                } else {
                    HwWifiP2pService.wifiP2pServiceUtils.handleGroupRemoved(HwWifiP2pService.this.mP2pStateMachine);
                    transitionTo(HwWifiP2pService.wifiP2pServiceUtils.getmInactiveState(HwWifiP2pService.this.mP2pStateMachine));
                    HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139281, 0);
                }
                if (HwWifiP2pService.this.getWifiRepeaterEnabled()) {
                    HwWifiP2pService.this.stopWifiRepeater(HwWifiP2pService.wifiP2pServiceUtils.getmGroup(HwWifiP2pService.this.mP2pStateMachine));
                }
            } else if (i == 143374) {
                logd(" SET_MIRACAST_MODE: " + message.arg1);
                if (1 == message.arg1) {
                    int unused2 = HwWifiP2pService.this.mLastLinkSpeed = -1;
                    int unused3 = HwWifiP2pService.this.mLinkSpeedCounter = 0;
                    int unused4 = HwWifiP2pService.this.mLinkSpeedPollToken = 0;
                    sendMessage(HwWifiP2pService.CMD_LINKSPEED_POLL, HwWifiP2pService.this.mLinkSpeedPollToken);
                }
                return false;
            } else if (i != 143470) {
                HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                hwWifiP2pService.loge("Unhandled message when=" + message.getWhen() + " what=" + message.what + " arg1=" + message.arg1 + " arg2=" + message.arg2, new Object[0]);
                return false;
            } else if (HwWifiP2pService.this.mLinkSpeedPollToken == message.arg1) {
                String ifname = HwWifiP2pService.wifiP2pServiceUtils.getmGroup(HwWifiP2pService.this.mP2pStateMachine).getInterface();
                int linkSpeed = SystemProperties.getInt("wfd.config.linkspeed", 0);
                if (linkSpeed == 0) {
                    linkSpeed = this.mWifiNative.mHwWifiP2pNativeEx.getP2pLinkSpeed(ifname);
                }
                logd("ifname: " + ifname + ", get linkspeed from wpa: " + linkSpeed + ", mLinkSpeed " + linkSpeed);
                if (HwWifiP2pService.this.mLinkSpeedCounter < 4) {
                    HwWifiP2pService.this.mLinkSpeeds[HwWifiP2pService.access$1608(HwWifiP2pService.this)] = linkSpeed;
                }
                if (HwWifiP2pService.this.mLinkSpeedCounter >= 4) {
                    int avarageLinkSpeed = 0;
                    for (int i2 = 0; i2 < 4; i2++) {
                        avarageLinkSpeed += HwWifiP2pService.this.mLinkSpeeds[i2] * HwWifiP2pService.this.mLinkSpeedWeights[i2];
                    }
                    int avarageLinkSpeed2 = avarageLinkSpeed / 100;
                    if (HwWifiP2pService.this.mLastLinkSpeed != avarageLinkSpeed2) {
                        int unused5 = HwWifiP2pService.this.mLastLinkSpeed = avarageLinkSpeed2;
                        HwWifiP2pService.this.sendLinkSpeedChangedBroadcast();
                    }
                    int unused6 = HwWifiP2pService.this.mLinkSpeedCounter = 0;
                }
                sendMessageDelayed(HwWifiP2pService.CMD_LINKSPEED_POLL, HwWifiP2pService.access$1704(HwWifiP2pService.this), 1000);
            }
            return true;
        }

        public boolean handleP2pNotSupportedStateMessage(Message message) {
            if (message.what != 141268) {
                HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                hwWifiP2pService.loge("Unhandled message " + message, new Object[0]);
                return false;
            }
            HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139278, 1);
            return true;
        }

        public boolean handleInactiveStateMessage(Message message) {
            boolean mret;
            switch (message.what) {
                case 141267:
                    HwWifiP2pService.this.removeDisableP2pGcDhcp(true);
                    WifiP2pConfig beam_config = (WifiP2pConfig) message.obj;
                    HwWifiP2pService.wifiP2pServiceUtils.setAutonomousGroup(HwWifiP2pService.this, false);
                    HwWifiP2pService.this.updateGroupCapability(this.mPeers, beam_config.deviceAddress, this.mWifiNative.getGroupCapability(beam_config.deviceAddress));
                    if (beam_connect(beam_config, HwWifiP2pService.TRY_REINVOCATION.booleanValue()) != -1) {
                        HwWifiP2pService.this.updateStatus(this.mPeers, this.mSavedPeerConfig.deviceAddress, 1);
                        sendPeersChangedBroadcast();
                        replyToMessage(message, 139273);
                        transitionTo(this.mGroupNegotiationState);
                        break;
                    } else {
                        replyToMessage(message, 139272);
                        break;
                    }
                case 141268:
                    if (!HwWifiP2pService.this.mWifiRepeater.isEncryptionTypeTetheringAllowed()) {
                        HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, this.mCreatPskGroupMsg, 139278, 0);
                        HwWifiP2pService.this.setWifiRepeaterState(5);
                        break;
                    } else {
                        HwWifiP2pService.this.setWifiRepeaterState(3);
                        HwWifiP2pService.wifiP2pServiceUtils.setAutonomousGroup(HwWifiP2pService.this, true);
                        if (HwWifiP2pService.this.mWifiRepeaterConfigChannel != null) {
                            this.mCreatPskGroupMsg = message;
                            WifiConfiguration userconfig = (WifiConfiguration) message.obj;
                            if (userconfig != null) {
                                HwWifiP2pService.this.mWifiRepeaterConfigChannel.sendMessage((int) HwWifiP2pService.CMD_SET_REPEATER_CONFIG, userconfig);
                                creatGroupForRepeater(userconfig);
                                break;
                            } else {
                                HwWifiP2pService.this.mWifiRepeaterConfigChannel.sendMessage((int) HwWifiP2pService.CMD_REQUEST_REPEATER_CONFIG);
                                break;
                            }
                        }
                    }
                    break;
                case 141269:
                    if (message.obj instanceof Bundle) {
                        HwWifiP2pService.this.setCurDisbleDhcpPackageName(message.sendingUid);
                        if (!sendMagiclinkConnectCommand(((Bundle) message.obj).getString("cfg"), message.sendingUid)) {
                            HwHiSLog.e(HwWifiP2pService.TAG, false, "MAGICLINK_CONNECT fail", new Object[0]);
                            HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139272, 0);
                            return true;
                        }
                    }
                    break;
                case 141270:
                    if (message.obj instanceof Bundle) {
                        resetP2pChannelSet();
                        HwWifiP2pService.wifiP2pServiceUtils.setAutonomousGroup(HwWifiP2pService.this, true);
                        int mnetId = message.arg1;
                        String freq = checkDfsWhenMagiclinkCreateGroup(((Bundle) message.obj).getString("freq"));
                        HwHiSLog.i(HwWifiP2pService.TAG, false, "MAGICLINK_CREATE_GROUP freq=%{public}s", new Object[]{freq});
                        if (mnetId == -2) {
                            int mnetId2 = this.mGroups.getNetworkId(HwWifiP2pService.wifiP2pServiceUtils.getmThisDevice(HwWifiP2pService.this).deviceAddress);
                            if (mnetId2 != -1) {
                                mret = this.mWifiNative.mHwWifiP2pNativeEx.magiclinkGroupAdd(mnetId2, freq);
                            } else {
                                mret = this.mWifiNative.mHwWifiP2pNativeEx.magiclinkGroupAdd(true, freq);
                            }
                        } else {
                            mret = this.mWifiNative.mHwWifiP2pNativeEx.magiclinkGroupAdd(false, freq);
                        }
                        if (mret) {
                            replyToMessage(message, 139279);
                            transitionTo(this.mGroupNegotiationState);
                        } else {
                            HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, message, 139278, 0);
                        }
                        HwWifiP2pService.this.updateP2pGoCreateStatus(mret);
                        break;
                    }
                    break;
                case 141272:
                    HwHiSLog.i(HwWifiP2pService.TAG, false, "receive disable p2p random mac message", new Object[0]);
                    if (HwWifiP2pService.this.ableToDisableP2pRandomMac(message.sendingUid)) {
                        HwHiSLog.i(HwWifiP2pService.TAG, false, "deliverP2pData to disable p2p random mac", new Object[0]);
                        this.mWifiNative.mHwWifiP2pNativeEx.deliverP2pData(2, 1, "1");
                        break;
                    }
                    break;
                case 141273:
                    if (HwWifiP2pService.this.isAbleToSetSinkConfig(message.sendingUid)) {
                        if (message.obj instanceof Bundle) {
                            String sinkConfig = ((Bundle) message.obj).getString("sinkConfig", "");
                            logd("HwWifiP2pService: setHwSinkConfig");
                            this.mWifiNative.mHwWifiP2pNativeEx.deliverP2pData(2, 3, sinkConfig);
                            break;
                        } else {
                            loge("SET_HWSINKCONFIG fail, message invalid");
                            return true;
                        }
                    }
                    break;
                case HwWifiP2pService.CMD_RESPONSE_REPEATER_CONFIG /*{ENCODED_INT: 143464}*/:
                    WifiConfiguration config = (WifiConfiguration) message.obj;
                    if (config == null) {
                        HwWifiP2pService.this.loge("wifi repeater config is null!", new Object[0]);
                        break;
                    } else {
                        creatGroupForRepeater(config);
                        break;
                    }
                case 147557:
                    HwWifiP2pService.this.sendInterfaceCreatedBroadcast((String) message.obj);
                    HwWifiP2pService hwWifiP2pService = HwWifiP2pService.this;
                    boolean unused = hwWifiP2pService.mMagicLinkDeviceFlag = !hwWifiP2pService.mLegacyGO;
                    transitionTo(this.mGroupNegotiationState);
                    break;
                default:
                    HwWifiP2pService hwWifiP2pService2 = HwWifiP2pService.this;
                    hwWifiP2pService2.loge("Unhandled message when=" + message.getWhen() + " what=" + message.what + " arg1=" + message.arg1 + " arg2=" + message.arg2, new Object[0]);
                    return false;
            }
            return true;
        }

        private boolean hasDisableDhcpPermission(int uid) {
            WifiInjector wifiInjector = WifiInjector.getInstance();
            if (wifiInjector == null || wifiInjector.getWifiPermissionsWrapper().getUidPermission(HwWifiP2pService.PERMISSION_DISABLE_P2P_GC_DHCP, uid) == -1) {
                return false;
            }
            return true;
        }

        private boolean isInApNoDhcpWhiteList(int uid) {
            if (uid == 1000) {
                return true;
            }
            String packageName = HwWifiP2pService.this.mContext.getPackageManager().getNameForUid(uid);
            for (String whitePackageName : HwWifiP2pService.AP_NO_DHCP_WHITE_PACKAGE_NAME_LIST) {
                if (whitePackageName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        private String convertMagiclinkConnectMode(String connectMode, int uid) {
            if (TextUtils.isEmpty(connectMode)) {
                return "";
            }
            try {
                int mode = Integer.parseInt(connectMode);
                if (mode == 0) {
                    boolean unused = HwWifiP2pService.this.mLegacyGO = false;
                    return HwWifiP2pService.MAGICLINK_CONNECT_GC_GO_MODE;
                } else if (mode == 1) {
                    boolean unused2 = HwWifiP2pService.this.mLegacyGO = true;
                    return "1";
                } else if (mode != 2) {
                    boolean unused3 = HwWifiP2pService.this.mLegacyGO = false;
                    return connectMode;
                } else if (!hasDisableDhcpPermission(uid) || !isInApNoDhcpWhiteList(uid)) {
                    HwHiSLog.e(HwWifiP2pService.TAG, false, "uid %{public}d do not have permission or not in white list", new Object[]{Integer.valueOf(uid)});
                    return "";
                } else {
                    boolean unused4 = HwWifiP2pService.this.mLegacyGO = false;
                    return "1";
                }
            } catch (NumberFormatException e) {
                HwHiSLog.e(HwWifiP2pService.TAG, false, "connectMode parseInt fail", new Object[0]);
                return "";
            }
        }

        private boolean sendMagiclinkConnectCommand(String info, int uid) {
            if (TextUtils.isEmpty(info)) {
                return false;
            }
            String[] tokens = info.split("\n");
            if (tokens.length < 4) {
                return false;
            }
            StringBuffer buf = new StringBuffer();
            buf.append("P\"" + tokens[0] + "\"\n" + tokens[1] + "\n\"" + tokens[2] + "\"\n" + tokens[3]);
            for (int i = 4; i < tokens.length; i++) {
                if (i == 4) {
                    HwHiSLog.i(HwWifiP2pService.TAG, false, "LegacyGO flag = %{public}s", new Object[]{tokens[i]});
                    String p2pMode = convertMagiclinkConnectMode(tokens[i], uid);
                    if (TextUtils.isEmpty(p2pMode)) {
                        return false;
                    }
                    buf.append("\n" + p2pMode);
                } else {
                    buf.append("\n" + tokens[i]);
                }
            }
            resetP2pChannelSet();
            this.mWifiNative.mHwWifiP2pNativeEx.magiclinkConnect(buf.toString());
            return true;
        }

        private void creatGroupForRepeater(WifiConfiguration config) {
            boolean unused = HwWifiP2pService.this.mWifiRepeaterEnabled = true;
            config.apChannel = HwWifiP2pService.this.mWifiRepeater.retrieveDownstreamChannel();
            config.apBand = HwWifiP2pService.this.mWifiRepeater.retrieveDownstreamBand();
            if (config.apChannel == -1 || config.apBand == -1) {
                HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, this.mCreatPskGroupMsg, 139278, 0);
                HwWifiP2pService.this.setWifiRepeaterState(5);
                boolean unused2 = HwWifiP2pService.this.mWifiRepeaterEnabled = false;
                return;
            }
            if (HwWifiP2pService.this.isWifiConnected()) {
                StringBuilder sb = new StringBuilder("WifiRepeater=y");
                sb.append("\nssid=");
                sb.append(config.SSID);
                sb.append("\npsk=");
                sb.append(new SensitiveArg(config.preSharedKey));
                sb.append("\nchannel=");
                sb.append(config.apChannel);
                sb.append("\nband=");
                StringBuilder repeater_conf = sb.append(config.apBand);
                resetP2pChannelSet();
                boolean ret = this.mWifiNative.mHwWifiP2pNativeEx.addP2pRptGroup(repeater_conf.toString());
                if (!ret) {
                    boolean unused3 = HwWifiP2pService.this.mWifiRepeaterEnabled = false;
                }
                HwWifiP2pService.this.updateP2pGoCreateStatus(ret);
            } else {
                boolean unused4 = HwWifiP2pService.this.mWifiRepeaterEnabled = false;
                HwWifiP2pService.this.loge("wifirpt: isWifiConnected = false", new Object[0]);
            }
            if (HwWifiP2pService.this.mWifiRepeaterEnabled) {
                Settings.Global.putInt(HwWifiP2pService.this.mContext.getContentResolver(), "wifi_repeater_on", 6);
                replyToMessage(this.mCreatPskGroupMsg, 139279);
                transitionTo(this.mGroupNegotiationState);
                if (HwWifiP2pService.HWDBG) {
                    HwWifiP2pService.this.logd("wifirpt: CREATE_GROUP_PSK SUCCEEDED, now transitionTo GroupNegotiationState", new Object[0]);
                    return;
                }
                return;
            }
            HwWifiP2pService.wifiP2pServiceUtils.replyToMessage(HwWifiP2pService.this.mP2pStateMachine, this.mCreatPskGroupMsg, 139278, 0);
            HwWifiP2pService.this.setWifiRepeaterState(5);
            HwWifiP2pService.this.loge("wifirpt: CREATE_GROUP_PSK FAILED, remain at this state.", new Object[0]);
        }

        private synchronized void addP2PValidDevice(String deviceAddress) {
            if (deviceAddress != null) {
                Iterator<Pair<String, Long>> iter = HwWifiP2pService.this.mValidDeivceList.iterator();
                while (iter.hasNext()) {
                    if (((String) iter.next().first).equals(deviceAddress)) {
                        iter.remove();
                    }
                }
                HwWifiP2pService.this.mValidDeivceList.add(new Pair(deviceAddress, Long.valueOf(SystemClock.elapsedRealtime())));
            }
        }

        private synchronized void removeP2PValidDevice(String deviceAddress) {
            if (HwWifiP2pService.this.mValidDeivceList != null) {
                Iterator<Pair<String, Long>> iter = HwWifiP2pService.this.mValidDeivceList.iterator();
                while (iter.hasNext()) {
                    if (((String) iter.next().first).equals(deviceAddress)) {
                        iter.remove();
                    }
                }
            }
        }

        private void cleanupValidDevicelist() {
            long curTime = SystemClock.elapsedRealtime();
            Iterator<Pair<String, Long>> iter = HwWifiP2pService.this.mValidDeivceList.iterator();
            while (iter.hasNext()) {
                if (curTime - ((Long) iter.next().second).longValue() > 15000) {
                    iter.remove();
                }
            }
        }

        private synchronized boolean isP2PValidDevice(String deviceAddress) {
            cleanupValidDevicelist();
            for (Pair<String, Long> entry : HwWifiP2pService.this.mValidDeivceList) {
                if (((String) entry.first).equals(deviceAddress)) {
                    return true;
                }
            }
            return false;
        }

        private synchronized void clearP2PValidDevice() {
            HwWifiP2pService.this.mValidDeivceList.clear();
        }

        private int beam_connect(WifiP2pConfig config, boolean tryInvocation) {
            int netId;
            if (config == null) {
                HwWifiP2pService.this.loge("config is null", new Object[0]);
                return -1;
            }
            this.mSavedPeerConfig = config;
            WifiP2pDevice dev = this.mPeers.get(config.deviceAddress);
            if (dev == null) {
                HwWifiP2pService.this.loge("target device not found ", new Object[0]);
                return -1;
            }
            boolean join = dev.isGroupOwner();
            String ssid = this.mWifiNative.p2pGetSsid(dev.deviceAddress);
            HwWifiP2pService.this.logd("target ssid is %{public}s join:%{public}s", StringUtilEx.safeDisplaySsid(ssid), String.valueOf(join));
            if (join && dev.isGroupLimit()) {
                HwWifiP2pService.this.logd("target device reaches group limit.", new Object[0]);
                join = false;
            } else if (join && (netId = HwWifiP2pService.this.getNetworkId(this.mGroups, dev.deviceAddress, ssid)) >= 0) {
                if (!this.mWifiNative.p2pGroupAdd(netId)) {
                    HwWifiP2pService.this.updateP2pGoCreateStatus(false);
                    return -1;
                }
                HwWifiP2pService.this.updateP2pGoCreateStatus(true);
                return 0;
            }
            if (join || !dev.isDeviceLimit()) {
                if (!join && tryInvocation && dev.isInvitationCapable()) {
                    int netId2 = -2;
                    if (config.netId < 0) {
                        netId2 = HwWifiP2pService.this.getNetworkId(this.mGroups, dev.deviceAddress);
                    } else if (config.deviceAddress.equals(HwWifiP2pService.this.getOwnerAddr(this.mGroups, config.netId))) {
                        netId2 = config.netId;
                    }
                    if (netId2 < 0) {
                        netId2 = getNetworkIdFromClientList(dev.deviceAddress);
                    }
                    HwWifiP2pService.this.logd("netId related with %{private}s = %{public}d", dev.deviceAddress, Integer.valueOf(netId2));
                    if (netId2 >= 0) {
                        if (this.mWifiNative.p2pReinvoke(netId2, dev.deviceAddress)) {
                            this.mSavedPeerConfig.netId = netId2;
                            return 0;
                        }
                        HwWifiP2pService.this.loge("p2pReinvoke() failed, update networks", new Object[0]);
                        updatePersistentNetworks(HwWifiP2pService.RELOAD.booleanValue());
                    }
                }
                this.mWifiNative.p2pStopFind();
                p2pBeamConnectWithPinDisplay(config);
                return 0;
            }
            HwWifiP2pService.this.loge("target device reaches the device limit.", new Object[0]);
            return -1;
        }

        private void p2pBeamConnectWithPinDisplay(WifiP2pConfig config) {
            WifiP2pDevice dev = this.mPeers.get(config.deviceAddress);
            if (dev == null) {
                HwWifiP2pService.this.loge("target device is not found ", new Object[0]);
                return;
            }
            String pin = this.mWifiNative.p2pConnect(config, dev.isGroupOwner());
            try {
                Integer.parseInt(pin);
                notifyInvitationSent(pin, config.deviceAddress);
            } catch (NumberFormatException e) {
            }
        }

        private void sendPeersChangedBroadcast() {
            Intent intent = new Intent("android.net.wifi.p2p.PEERS_CHANGED");
            intent.putExtra("wifiP2pDeviceList", new WifiP2pDeviceList(this.mPeers));
            intent.addFlags(67108864);
            HwWifiP2pService.this.mContext.sendBroadcast(intent, "android.permission.ACCESS_WIFI_STATE");
        }

        /* access modifiers changed from: private */
        public void sendP2pConnectionStateBroadcast(int state) {
            HwWifiP2pService.this.logd("sending p2p connection state broadcast and state = %{public}d", Integer.valueOf(state));
            Intent intent = new Intent("android.net.wifi.p2p.CONNECT_STATE_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("extraState", state);
            if (this.mSavedPeerConfig == null || state != 2) {
                HwWifiP2pService.this.loge("GroupCreatedState:mSavedConnectConfig is null", new Object[0]);
            } else {
                String opposeInterfaceAddressString = this.mSavedPeerConfig.deviceAddress;
                String conDeviceName = null;
                intent.putExtra("interfaceAddress", opposeInterfaceAddressString);
                Iterator<WifiP2pDevice> it = this.mPeers.getDeviceList().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    WifiP2pDevice d = it.next();
                    if (d.deviceAddress != null && d.deviceAddress.equals(this.mSavedPeerConfig.deviceAddress)) {
                        conDeviceName = d.deviceName;
                        break;
                    }
                }
                intent.putExtra("oppDeviceName", conDeviceName);
                HwWifiP2pService.this.logd("oppDeviceName = %{public}s", conDeviceName);
                HwWifiP2pService.this.logd("opposeInterfaceAddressString = %{private}s", opposeInterfaceAddressString);
            }
            HwWifiP2pService.this.mContext.sendBroadcast(intent, "android.permission.ACCESS_WIFI_STATE");
        }

        public boolean autoAcceptConnection() {
            if (!isP2PValidDevice(this.mSavedPeerConfig.deviceAddress) && !isP2PValidDevice(getDeviceName(this.mSavedPeerConfig.deviceAddress))) {
                return false;
            }
            HwWifiP2pService.this.logd("notifyInvitationReceived is a valid device", new Object[0]);
            removeP2PValidDevice(this.mSavedPeerConfig.deviceAddress);
            sendMessage(HwWifiP2pService.wifiP2pServiceUtils.getPeerConnectionUserAccept(HwWifiP2pService.this));
            return true;
        }

        private String getDeviceName(String deviceAddress) {
            WifiP2pDevice d = this.mPeers.get(deviceAddress);
            if (d != null) {
                return d.deviceName;
            }
            return deviceAddress;
        }

        public String p2pBeamConnect(WifiP2pConfig config, boolean joinExistingGroup) {
            if (config == null) {
                return null;
            }
            List<String> args = new ArrayList<>();
            WpsInfo wps = config.wps;
            args.add(config.deviceAddress);
            int i = wps.setup;
            if (i == 0) {
                args.add("pbc");
            } else if (i == 1) {
                if (TextUtils.isEmpty(wps.pin)) {
                    args.add("pin");
                } else {
                    args.add(wps.pin);
                }
                args.add("display");
            } else if (i == 2) {
                args.add(wps.pin);
                args.add("keypad");
            } else if (i == 3) {
                args.add(wps.pin);
                args.add("label");
            }
            if (config.netId == -2) {
                args.add("persistent");
            }
            if (joinExistingGroup) {
                args.add("join");
            } else {
                int groupOwnerIntent = config.groupOwnerIntent;
                if (groupOwnerIntent < 0 || groupOwnerIntent > 15) {
                    groupOwnerIntent = 6;
                }
                args.add("go_intent=" + groupOwnerIntent);
            }
            args.add("beam");
            StringBuffer command = new StringBuffer("P2P_CONNECT ");
            for (String s : args) {
                command.append(s);
                command.append(" ");
            }
            return HwWifiCHRHilink.WEB_DELAY_NEEDUPLOAD;
        }

        private void selfcureP2pGoCreateFail() {
            loge("selfcureP2pGoCreatedFail times >= 2, then reset wifi");
            WifiInjector wifiInjector = WifiInjector.getInstance();
            int unused = HwWifiP2pService.this.mP2pCreateGoFailTimes = 0;
            wifiInjector.getSelfRecovery().trigger(1);
        }

        private void resetP2pChannelSet() {
            loge("resetP2pChannelSet");
            this.mWifiNative.p2pSetChannel(0, 0);
        }

        private int getRandom2gChannel() {
            int result = HwWifiP2pService.COMMON_CHANNELS_2G[new SecureRandom().nextInt(HwWifiP2pService.COMMON_CHANNELS_2G.length)];
            HwHiSLog.d(HwWifiP2pService.TAG, false, "getRandom2gChannel: %{public}d", new Object[]{Integer.valueOf(result)});
            return result;
        }

        private int getUsable5gChannel() {
            WifiInjector wifiInjector = WifiInjector.getInstance();
            String countryCode = "";
            if (wifiInjector != null) {
                countryCode = wifiInjector.getWifiCountryCode().getCountryCodeSentToDriver();
            }
            if (TextUtils.isEmpty(countryCode)) {
                return WifiCommonUtils.convertChannelToFrequency(getRandom2gChannel());
            }
            if ("CN".equals(countryCode)) {
                return 5180;
            }
            int selectedChannel = HwApConfigUtilEx.getSelected5GChannel(SoftApChannelXmlParse.convertChannelListToFrequency(HwSoftApManager.getChannelListFor5GWithoutIndoor()));
            if (selectedChannel == -1) {
                selectedChannel = getRandom2gChannel();
            }
            return WifiCommonUtils.convertChannelToFrequency(selectedChannel);
        }

        private int getFreqWhenSetDfsChannel(int frequency) {
            WifiManager wifiManager = (WifiManager) HwWifiP2pService.this.mContext.getSystemService("wifi");
            WifiInfo wifiInfo = null;
            if (wifiManager != null) {
                wifiInfo = wifiManager.getConnectionInfo();
            }
            if (!HwWifiP2pService.this.isWifiConnected() || wifiInfo == null) {
                return getUsable5gChannel();
            }
            if (HwWifiP2pService.this.mHwDfsMonitor != null && wifiInfo.getFrequency() == frequency) {
                HwWifiP2pService.this.mHwDfsMonitor.closeGoCac(0);
            }
            return wifiInfo.getFrequency();
        }

        private String checkDfsWhenMagiclinkCreateGroup(String freq) {
            HwHiSLog.i(HwWifiP2pService.TAG, false, "checkDfsWhenMagiclinkCreateGroup freq=%{public}s", new Object[]{freq});
            String sourceFreq = freq;
            String freqBw80M = freq;
            boolean is160M = false;
            if (!TextUtils.isEmpty(sourceFreq) && sourceFreq.endsWith(HwWifiP2pService.MAGICLINK_CREATE_GROUP_160M_FLAG)) {
                freqBw80M = sourceFreq.replace(HwWifiP2pService.MAGICLINK_CREATE_GROUP_160M_FLAG, "");
                is160M = true;
            }
            try {
                int frequency = Integer.parseInt(freqBw80M);
                WifiInjector wifiInjector = WifiInjector.getInstance();
                boolean isDfsChannel = false;
                if (wifiInjector != null) {
                    isDfsChannel = wifiInjector.getWifiNative().mHwWifiNativeEx.isDfsChannel(frequency);
                }
                if (!HwWifiP2pService.this.mIsUsingHwShare || (!is160M && !isDfsChannel)) {
                    if (HwWifiP2pService.this.mHwDfsMonitor != null && isDfsChannel) {
                        HwWifiP2pService.this.mHwDfsMonitor.closeGoCac(0);
                    }
                    boolean unused = HwWifiP2pService.this.mIsUsingHwShare = false;
                    return is160M ? freqBw80M : sourceFreq;
                }
                boolean unused2 = HwWifiP2pService.this.mIsUsingHwShare = false;
                if (is160M && HwMSSUtils.is1105() && !HwWifiP2pService.this.isWifiConnected() && frequency >= 5500) {
                    frequency = getUsable5gChannel();
                    sourceFreq = String.valueOf(frequency) + HwWifiP2pService.MAGICLINK_CREATE_GROUP_160M_FLAG;
                }
                if (HwWifiP2pService.this.mHwDfsMonitor != null && HwWifiP2pService.this.mHwDfsMonitor.isDfsUsable(frequency)) {
                    HwWifiP2pService.this.mHwDfsMonitor.closeGoCac(0);
                    return isDfsChannel ? freqBw80M : sourceFreq;
                } else if (isDfsChannel) {
                    return String.valueOf(getFreqWhenSetDfsChannel(frequency));
                } else {
                    return freqBw80M;
                }
            } catch (NumberFormatException e) {
                HwHiSLog.e(HwWifiP2pService.TAG, false, "freq parseInt fail", new Object[0]);
                return sourceFreq;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void sendP2pConnectingStateBroadcast() {
        logd(" mHwP2pStateMachine = " + this.mHwP2pStateMachine + " this = " + this, new Object[0]);
        this.mHwP2pStateMachine.sendP2pConnectionStateBroadcast(1);
    }

    /* access modifiers changed from: protected */
    public void sendP2pFailStateBroadcast() {
        HwWifiCHRService hwWifiCHRService;
        logd(" mHwP2pStateMachine = " + this.mHwP2pStateMachine + " this = " + this, new Object[0]);
        this.mHwP2pStateMachine.sendP2pConnectionStateBroadcast(3);
        if (this.mIsWifiRepeaterTetherStarted && (hwWifiCHRService = this.mHwWifiCHRService) != null) {
            hwWifiCHRService.addRepeaterConnFailedCount(1);
        }
    }

    /* access modifiers changed from: protected */
    public void sendP2pConnectedStateBroadcast() {
        logd(" mHwP2pStateMachine = " + this.mHwP2pStateMachine + " this = " + this, new Object[0]);
        this.mHwP2pStateMachine.sendP2pConnectionStateBroadcast(2);
    }

    /* access modifiers changed from: protected */
    public void clearValidDeivceList() {
        this.mValidDeivceList.clear();
    }

    /* access modifiers changed from: protected */
    public boolean autoAcceptConnection() {
        logd(" mHwP2pStateMachine = " + this.mHwP2pStateMachine + " this = " + this, new Object[0]);
        return this.mHwP2pStateMachine.autoAcceptConnection();
    }

    /* access modifiers changed from: private */
    public void loge(String string, Object... args) {
        HwHiSLog.e(TAG, false, string, args);
    }

    /* access modifiers changed from: private */
    public void logd(String string, Object... args) {
        HwHiSLog.d(TAG, false, string, args);
    }

    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) this.mContext.getSystemService("connectivity");
    }

    /* access modifiers changed from: private */
    public boolean isWifiConnected() {
        NetworkInfo networkInfo = this.mNetworkInfo;
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        return false;
    }

    private int convertFrequencyToChannelNumber(int frequency) {
        if (frequency >= 2412 && frequency <= 2484) {
            return ((frequency - 2412) / 5) + 1;
        }
        if (frequency < 5170 || frequency > 5825) {
            return 0;
        }
        return ((frequency - 5170) / 5) + 34;
    }

    public static class SensitiveArg {
        private final Object mArg;

        public SensitiveArg(Object arg) {
            this.mArg = arg;
        }

        public String toString() {
            return String.valueOf(this.mArg);
        }
    }

    /* access modifiers changed from: protected */
    public boolean startWifiRepeater(WifiP2pGroup group) {
        this.mTetherInterfaceName = group.getInterface();
        if (HWDBG) {
            logd("start wifi repeater, ifaceName=" + this.mTetherInterfaceName + ", mWifiRepeaterEnabled=" + this.mWifiRepeaterEnabled + ", isWifiConnected=" + isWifiConnected(), new Object[0]);
        }
        this.mWifiRepeaterFreq = group.getFrequency();
        if (isWifiConnected()) {
            int resultCode = getConnectivityManager().tether(this.mTetherInterfaceName);
            if (HWDBG) {
                logd("ConnectivityManager.tether resultCode = " + resultCode, new Object[0]);
            }
            if (resultCode == 0) {
                this.mWifiRepeater.handleP2pTethered(group);
                this.mIsWifiRepeaterTetherStarted = true;
                setWifiRepeaterState(1);
                HwWifiCHRService hwWifiCHRService = this.mHwWifiCHRService;
                if (hwWifiCHRService != null) {
                    hwWifiCHRService.addWifiRepeaterOpenedCount(1);
                    this.mHwWifiCHRService.setWifiRepeaterStatus(true);
                }
                return true;
            }
        }
        setWifiRepeaterState(5);
        HwWifiCHRService hwWifiCHRService2 = this.mHwWifiCHRService;
        if (hwWifiCHRService2 != null) {
            hwWifiCHRService2.updateRepeaterOpenOrCloseError((int) HwQoEUtils.QOE_MSG_SCREEN_OFF, 1, "REPEATER_OPEN_OR_CLOSE_FAILED_UNKNOWN");
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public String getWifiRepeaterServerAddress() {
        DhcpInfo dhcpInfo;
        WifiManager mWM = (WifiManager) this.mContext.getSystemService("wifi");
        int defaultAddress = NetworkUtils.inetAddressToInt((Inet4Address) NetworkUtils.numericToInetAddress(SERVER_ADDRESS_WIFI_BRIDGE));
        if (mWM == null || (dhcpInfo = mWM.getDhcpInfo()) == null || (dhcpInfo.gateway & 16777215) != (16777215 & defaultAddress)) {
            if (HWDBG) {
                logd("getWifiRepeaterServerAddress use SERVER_ADDRESS_WIFI_BRIDGE", new Object[0]);
            }
            return SERVER_ADDRESS_WIFI_BRIDGE;
        } else if (!HWDBG) {
            return SERVER_ADDRESS_WIFI_BRIDGE_OTHER;
        } else {
            logd("getWifiRepeaterServerAddress use SERVER_ADDRESS_WIFI_BRIDGE_OTHER", new Object[0]);
            return SERVER_ADDRESS_WIFI_BRIDGE_OTHER;
        }
    }

    public WifiRepeater getWifiRepeater() {
        return this.mWifiRepeater;
    }

    public void notifyRptGroupRemoved() {
        this.mWifiRepeater.handleP2pUntethered();
    }

    public int getWifiRepeaterFreq() {
        return this.mWifiRepeaterFreq;
    }

    public int getWifiRepeaterChannel() {
        return convertFrequencyToChannelNumber(this.mWifiRepeaterFreq);
    }

    public boolean getWifiRepeaterTetherStarted() {
        return this.mIsWifiRepeaterTetherStarted;
    }

    public void handleClientConnect(WifiP2pGroup group) {
        if (this.mIsWifiRepeaterTetherStarted && group != null) {
            if (group.getClientList().size() >= 1) {
                DecisionUtil.bindService(this.mContext);
                HwHiLog.d(TAG, false, "bindService", new Object[0]);
            }
            this.mWifiRepeater.handleClientListChanged(group);
            if (HwWifiService.SHOULD_NETWORK_SHARING_INTEGRATION) {
                handleClientChangedAction(group, 0);
            }
            if (this.mWifiRepeaterBeginWorkTime == 0 && group.getClientList().size() == 1) {
                this.mWifiRepeaterBeginWorkTime = SystemClock.elapsedRealtime();
            }
            this.mHwWifiCHRService.setRepeaterMaxClientCount(group.getClientList().size() > 0 ? group.getClientList().size() : 0);
        }
    }

    public void handleClientDisconnect(WifiP2pGroup group) {
        if (group != null && this.mIsWifiRepeaterTetherStarted) {
            this.mWifiRepeater.handleClientListChanged(group);
            if (HwWifiService.SHOULD_NETWORK_SHARING_INTEGRATION) {
                handleClientChangedAction(group, 1);
            }
            if (group.getClientList().size() == 0 && this.mHwWifiCHRService != null) {
                this.mWifiRepeaterEndWorkTime = SystemClock.elapsedRealtime();
                this.mHwWifiCHRService.setWifiRepeaterWorkingTime((this.mWifiRepeaterEndWorkTime - this.mWifiRepeaterBeginWorkTime) / 1000);
                this.mWifiRepeaterEndWorkTime = 0;
                this.mWifiRepeaterBeginWorkTime = 0;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void stopWifiRepeater(WifiP2pGroup group) {
        HwWifiCHRService hwWifiCHRService;
        setWifiRepeaterState(2);
        this.mWifiRepeaterEnabled = false;
        this.mWifiRepeaterEndWorkTime = SystemClock.elapsedRealtime();
        if (!(group == null || group.getClientList().size() <= 0 || (hwWifiCHRService = this.mHwWifiCHRService) == null)) {
            hwWifiCHRService.setWifiRepeaterWorkingTime((this.mWifiRepeaterEndWorkTime - this.mWifiRepeaterBeginWorkTime) / 1000);
        }
        this.mWifiRepeaterBeginWorkTime = 0;
        this.mWifiRepeaterEndWorkTime = 0;
        this.mWifiRepeaterFreq = 0;
        HwWifiCHRService hwWifiCHRService2 = this.mHwWifiCHRService;
        if (hwWifiCHRService2 != null) {
            hwWifiCHRService2.setWifiRepeaterFreq(this.mWifiRepeaterFreq);
        }
        if (this.mIsWifiRepeaterTetherStarted) {
            int resultCode = getConnectivityManager().untether(this.mTetherInterfaceName);
            if (HWDBG) {
                logd("ConnectivityManager.untether resultCode = " + resultCode, new Object[0]);
            }
            if (resultCode == 0) {
                this.mIsWifiRepeaterTetherStarted = false;
                setWifiRepeaterState(0);
                HwWifiCHRService hwWifiCHRService3 = this.mHwWifiCHRService;
                if (hwWifiCHRService3 != null) {
                    hwWifiCHRService3.setWifiRepeaterStatus(false);
                }
                this.mWifiRepeaterClientList.clear();
                return;
            }
            loge("Untether initiate failed!", new Object[0]);
            setWifiRepeaterState(4);
            HwWifiCHRService hwWifiCHRService4 = this.mHwWifiCHRService;
            if (hwWifiCHRService4 != null) {
                hwWifiCHRService4.updateRepeaterOpenOrCloseError((int) HwQoEUtils.QOE_MSG_SCREEN_OFF, 0, "REPEATER_OPEN_OR_CLOSE_FAILED_UNKNOWN");
                return;
            }
            return;
        }
        setWifiRepeaterState(0);
        this.mWifiRepeaterClientList.clear();
    }

    public void setWifiRepeaterState(int state) {
        Context context = this.mContext;
        if (context != null) {
            Settings.Global.putInt(context.getContentResolver(), "wifi_repeater_on", state);
            Intent intent = new Intent("com.huawei.android.net.wifi.p2p.action.WIFI_RPT_STATE_CHANGED");
            intent.putExtra("wifi_rpt_state", state);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /* access modifiers changed from: protected */
    public boolean getWifiRepeaterEnabled() {
        return this.mWifiRepeaterEnabled;
    }

    /* access modifiers changed from: protected */
    public void initWifiRepeaterConfig() {
        if (this.mWifiRepeaterConfigChannel == null) {
            this.mWifiRepeaterConfigChannel = new AsyncChannel();
            this.mWifiRepeaterConfigStore = WifiRepeaterConfigStore.makeWifiRepeaterConfigStore(this.mP2pStateMachine.getHandler());
            this.mWifiRepeaterConfigStore.loadRepeaterConfiguration();
            this.mWifiRepeaterConfigChannel.connectSync(this.mContext, this.mP2pStateMachine.getHandler(), this.mWifiRepeaterConfigStore.getMessenger());
        }
    }

    public void setWifiRepeaterConfiguration(WifiConfiguration config) {
        AsyncChannel asyncChannel = this.mWifiRepeaterConfigChannel;
        if (asyncChannel != null && config != null) {
            asyncChannel.sendMessage((int) CMD_SET_REPEATER_CONFIG, config);
        }
    }

    public WifiConfiguration syncGetWifiRepeaterConfiguration() {
        Message resultMsg;
        AsyncChannel asyncChannel = this.mWifiRepeaterConfigChannel;
        if (asyncChannel == null || (resultMsg = asyncChannel.sendMessageSynchronously((int) CMD_REQUEST_REPEATER_CONFIG)) == null) {
            return null;
        }
        WifiConfiguration ret = (WifiConfiguration) resultMsg.obj;
        resultMsg.recycle();
        return ret;
    }

    private void handleClientChangedAction(WifiP2pGroup group, int clientAction) {
        int curLinkedCliectCount = group.getClientList().size();
        if (curLinkedCliectCount > 4) {
            HwHiLog.d(TAG, false, "LinkedCliectCount over flow, need synchronize. CliectCount=%{public}d", new Object[]{Integer.valueOf(curLinkedCliectCount)});
            return;
        }
        String action = null;
        long currentTime = System.currentTimeMillis();
        String deviceAddress = null;
        Collection<WifiP2pDevice> curClientList = new ArrayList<>(group.getClientList());
        if (clientAction == 0) {
            action = WIFI_REPEATER_CLIENT_JOIN_ACTION;
            curClientList.removeAll(this.mWifiRepeaterClientList);
            if (curClientList.isEmpty()) {
                HwHiLog.d(TAG, false, "no new client join", new Object[0]);
                return;
            }
            deviceAddress = new ArrayList<>(curClientList).get(0).deviceAddress;
        } else if (clientAction == 1) {
            action = WIFI_REPEATER_CLIENT_LEAVE_ACTION;
            this.mWifiRepeaterClientList.removeAll(curClientList);
            if (this.mWifiRepeaterClientList.isEmpty()) {
                HwHiLog.d(TAG, false, "no client leave", new Object[0]);
                return;
            }
            deviceAddress = new ArrayList<>(this.mWifiRepeaterClientList).get(0).deviceAddress;
        }
        this.mWifiRepeaterClientList.clear();
        this.mWifiRepeaterClientList.addAll(group.getClientList());
        if (deviceAddress != null && action != null) {
            String.format("MAC=%s TIME=%d STACNT=%d", StringUtilEx.safeDisplayBssid(deviceAddress), Long.valueOf(currentTime), Integer.valueOf(curLinkedCliectCount));
            HwHiLog.d(TAG, false, "Send broadcast: %{public}s, extraInfo: MAC=%{private}s TIME=%{public}d STACNT=%{public}d", new Object[]{action, StringUtilEx.safeDisplayBssid(deviceAddress), Long.valueOf(currentTime), Integer.valueOf(curLinkedCliectCount)});
            Intent intent = new Intent(action);
            intent.addFlags(16777216);
            intent.putExtra(EXTRA_CLIENT_INFO, deviceAddress);
            intent.putExtra(EXTRA_CURRENT_TIME, currentTime);
            intent.putExtra(EXTRA_STA_COUNT, curLinkedCliectCount);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.ACCESS_WIFI_STATE");
        }
    }

    public List<String> getRepeaterLinkedClientList() {
        if (!this.mIsWifiRepeaterTetherStarted) {
            HwHiLog.d(TAG, false, "mWifiRepeaterClientList is null or empty", new Object[0]);
            return Collections.emptyList();
        }
        WifiP2pGroup wifiP2pGroup = wifiP2pServiceUtils.getmGroup(this.mP2pStateMachine);
        if (wifiP2pGroup == null) {
            return Collections.emptyList();
        }
        Collection<WifiP2pDevice> curClientList = new ArrayList<>(wifiP2pGroup.getClientList());
        List<String> dhcpList = HwSoftApManager.readSoftapStaDhcpInfo();
        List<String> infoList = new ArrayList<>();
        for (WifiP2pDevice p2pDevice : curClientList) {
            infoList.add(HwSoftApManager.getApLinkedStaInfo(p2pDevice.deviceAddress, dhcpList));
        }
        HwHiLog.d(TAG, false, "linkedClientInfo: info size=%{public}d", new Object[]{Integer.valueOf(infoList.size())});
        return infoList;
    }

    public void setWifiRepeaterDisassociateSta(String mac) {
        if (!this.mIsWifiRepeaterTetherStarted || TextUtils.isEmpty(mac)) {
            HwHiLog.d(TAG, false, "setWifiRepeaterDisassociateSta called when WifiRepeaterTether is not Started", new Object[0]);
        } else if (TextUtils.isEmpty(this.mTetherInterfaceName)) {
            HwHiLog.d(TAG, false, "setWifiRepeaterDisassociateSta mInterface is empty", new Object[0]);
        } else if (!WifiInjector.getInstance().getWifiNative().mHwWifiNativeEx.disassociateWifiRepeaterStationHw(this.mTetherInterfaceName, mac)) {
            HwHiLog.e(TAG, false, "Failed to setWifiRepeaterDisassociateSta", new Object[0]);
        }
    }

    public void setWifiRepeaterMacFilter(String macFilter) {
        if (!this.mIsWifiRepeaterTetherStarted || TextUtils.isEmpty(this.mTetherInterfaceName) || TextUtils.isEmpty(macFilter)) {
            HwHiLog.d(TAG, false, "setWifiRepeaterMacFilter called when WifiRepeaterTether is not Started", new Object[0]);
        } else if (!WifiInjector.getInstance().getWifiNative().mHwWifiNativeEx.setWifiRepeaterMacFilterHw(this.mTetherInterfaceName, macFilter)) {
            HwHiLog.e(TAG, false, "Failed to setWifiRepeaterMacFilter", new Object[0]);
        } else {
            if (this.mHwWifiCHRService != null && !this.mMacFilterStr.equals(macFilter)) {
                String[] macFilterStrs = macFilter.split(SPLIT_DOT);
                if (macFilterStrs.length < 2) {
                    HwHiLog.e(TAG, false, "length of macFilterStrs is not enough ", new Object[0]);
                    return;
                }
                String[] macFilterCountStrs = macFilterStrs[1].split(SPLIT_EQUAL);
                if (macFilterCountStrs.length < 2) {
                    HwHiLog.e(TAG, false, "length of macFilterCntStrs is not enough ", new Object[0]);
                    return;
                }
                try {
                    int macFilterCount = Integer.parseInt(macFilterCountStrs[1]);
                    HwHiLog.d(TAG, false, "setWifiRepeaterMacFilter count = %{public}d", new Object[]{Integer.valueOf(macFilterCount)});
                    if (macFilterCount > 0 && macFilterCount >= this.mMacFilterStaCount) {
                        Bundle data = new Bundle();
                        data.putInt("repeaterBlacklistCnt", 1);
                        this.mHwWifiCHRService.uploadDFTEvent(14, data);
                    }
                    this.mMacFilterStaCount = macFilterCount;
                } catch (NumberFormatException e) {
                    HwHiLog.e(TAG, false, "Exception happens", new Object[0]);
                    return;
                }
            }
            this.mMacFilterStr = macFilter;
        }
    }

    public boolean hasP2pService() {
        WifiP2pGroup wifiP2pGroup = wifiP2pServiceUtils.getmGroup(this.mP2pStateMachine);
        if (wifiP2pGroup == null) {
            return false;
        }
        String p2pGroupInterface = wifiP2pGroup.getInterface();
        if (TextUtils.isEmpty(p2pGroupInterface)) {
            return false;
        }
        try {
            NetworkInterface p2pInterface = NetworkInterface.getByName(p2pGroupInterface);
            if (p2pInterface != null && !p2pInterface.isLoopback()) {
                if (p2pInterface.isUp()) {
                    Enumeration<InetAddress> addrs = p2pInterface.getInetAddresses();
                    if (addrs == null || !addrs.hasMoreElements()) {
                        return false;
                    }
                    return true;
                }
            }
            return false;
        } catch (SocketException e) {
            HwHiLog.e(TAG, false, "SocketException exception when get p2pInterface", new Object[0]);
            return false;
        }
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", "WifiService");
    }

    private void enforceDisableDhcpPermission() {
        this.mContext.enforceCallingOrSelfPermission(PERMISSION_DISABLE_P2P_GC_DHCP, TAG);
    }

    private boolean isInRequsetDfsStatusWhiteList(String packageName) {
        for (String whitePackageName : REQUEST_DFS_STATUS_WHITE_PACKAGE_LIST) {
            if (whitePackageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean requsetDfsStatus(Parcel data, Parcel reply) {
        if (!wifiP2pServiceUtils.checkSignMatchOrIsSystemApp(this.mContext)) {
            HwHiLog.e(TAG, false, "REQUEST_DFS_STATUS SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
            reply.writeNoException();
            reply.writeInt(0);
            return false;
        }
        data.enforceInterface(DESCRIPTOR);
        enforceAccessPermission();
        String packageName = data.readString();
        HwHiLog.i(TAG, false, "REQUEST_DFS_STATUS packageName=%{public}s", new Object[]{packageName});
        if (!isInRequsetDfsStatusWhiteList(packageName)) {
            reply.writeNoException();
            reply.writeInt(0);
            return false;
        }
        this.mIsUsingHwShare = true;
        int frequency = data.readInt();
        int bandWidth = data.readInt();
        IWifiActionListener actionListener = IWifiActionListener.Stub.asInterface(data.readStrongBinder());
        int result = 0;
        HwDfsMonitor hwDfsMonitor = this.mHwDfsMonitor;
        if (hwDfsMonitor != null) {
            result = hwDfsMonitor.requestDfsStatus(frequency, bandWidth, actionListener);
        }
        reply.writeNoException();
        reply.writeInt(result);
        return true;
    }

    private boolean updateDfsStatus(Parcel data, Parcel reply) {
        if (!wifiP2pServiceUtils.checkSignMatchOrIsSystemApp(this.mContext)) {
            HwHiLog.e(TAG, false, "UPDATE_DFS_STATUS SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
            reply.writeNoException();
            reply.writeInt(0);
            return false;
        }
        data.enforceInterface(DESCRIPTOR);
        enforceAccessPermission();
        String packageName = data.readString();
        HwHiLog.i(TAG, false, "UPDATE_DFS_STATUS packageName=%{public}s", new Object[]{packageName});
        if (!isInRequsetDfsStatusWhiteList(packageName)) {
            reply.writeNoException();
            reply.writeInt(0);
            return false;
        }
        int transferResult = data.readInt();
        int transferRate = data.readInt();
        HwDfsMonitor hwDfsMonitor = this.mHwDfsMonitor;
        if (hwDfsMonitor != null) {
            hwDfsMonitor.updateDfsStatus(transferResult, transferRate);
        }
        reply.writeNoException();
        reply.writeInt(1);
        return true;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        WifiConfiguration _arg0;
        String _arg2;
        String _arg1;
        String _arg02;
        String _arg03;
        switch (code) {
            case CODE_GET_WIFI_REPEATER_CONFIG /*{ENCODED_INT: 1001}*/:
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                HwHiSLog.d(TAG, false, "GetWifiRepeaterConfiguration ", new Object[0]);
                WifiConfiguration _result = syncGetWifiRepeaterConfiguration();
                reply.writeNoException();
                if (_result != null) {
                    reply.writeInt(1);
                    _result.writeToParcel(reply, 1);
                } else {
                    reply.writeInt(0);
                }
                return true;
            case CODE_SET_WIFI_REPEATER_CONFIG /*{ENCODED_INT: 1002}*/:
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                HwHiSLog.d(TAG, false, "setWifiRepeaterConfiguration ", new Object[0]);
                if (data.readInt() != 0) {
                    _arg0 = (WifiConfiguration) WifiConfiguration.CREATOR.createFromParcel(data);
                } else {
                    _arg0 = null;
                }
                setWifiRepeaterConfiguration(_arg0);
                reply.writeNoException();
                return true;
            case CODE_WIFI_MAGICLINK_CONFIG_IP /*{ENCODED_INT: 1003}*/:
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                HwHiSLog.d(TAG, false, "configIPAddr ", new Object[0]);
                if (data.readInt() != 0) {
                    _arg02 = data.readString();
                    _arg1 = data.readString();
                    _arg2 = data.readString();
                } else {
                    _arg02 = null;
                    _arg1 = null;
                    _arg2 = null;
                }
                configIPAddr(_arg02, _arg1, _arg2);
                reply.writeNoException();
                return true;
            case CODE_WIFI_MAGICLINK_RELEASE_IP /*{ENCODED_INT: 1004}*/:
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                HwHiSLog.d(TAG, false, "setWifiRepeaterConfiguration ", new Object[0]);
                if (data.readInt() != 0) {
                    _arg03 = data.readString();
                } else {
                    _arg03 = null;
                }
                releaseIPAddr(_arg03);
                reply.writeNoException();
                return true;
            case CODE_GET_GROUP_CONFIG_INFO /*{ENCODED_INT: 1005}*/:
                if (!wifiP2pServiceUtils.checkSignMatchOrIsSystemApp(this.mContext)) {
                    HwHiLog.e(TAG, false, "WifiP2pService  CODE_GET_GROUP_CONFIG_INFO  SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                    reply.writeInt(0);
                    reply.writeNoException();
                    return false;
                }
                String temp = this.mConfigInfo;
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                reply.writeNoException();
                if (temp == null) {
                    reply.writeInt(0);
                    return true;
                }
                reply.writeInt(1);
                reply.writeString(temp);
                return true;
            case CODE_DISABLE_P2P_GC_DHCP /*{ENCODED_INT: 1006}*/:
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                enforceDisableDhcpPermission();
                HwHiSLog.i(TAG, false, "disableP2pGcDhcp", new Object[0]);
                boolean isSuccess = disableP2pGcDhcp(data.readString(), Binder.getCallingUid(), data.readInt());
                reply.writeNoException();
                reply.writeBooleanArray(new boolean[]{isSuccess});
                return true;
            case CODE_REQUEST_DFS_STATUS /*{ENCODED_INT: 1007}*/:
                return requsetDfsStatus(data, reply);
            case CODE_UPDATE_DFS_STATUS /*{ENCODED_INT: 1008}*/:
                return updateDfsStatus(data, reply);
            default:
                return HwWifiP2pService.super.onTransact(code, data, reply, flags);
        }
    }

    /* access modifiers changed from: protected */
    public boolean processMessageForP2pCollision(Message msg, State state) {
        int i;
        boolean mIsP2pCollision = false;
        if (state instanceof WifiP2pServiceImpl.P2pStateMachine.DefaultState) {
            switch (msg.what) {
                case 139265:
                case 139268:
                case 139271:
                case 139274:
                case 139277:
                case 139315:
                case 139318:
                case 139321:
                    if (this.mWifiRepeaterEnabled) {
                        showUserToastIfP2pCollision();
                        mIsP2pCollision = true;
                        break;
                    }
                    break;
            }
        }
        if (state instanceof WifiP2pServiceImpl.P2pStateMachine.P2pEnabledState) {
            switch (msg.what) {
                case 139265:
                case 139318:
                case 139329:
                case 139332:
                case 139335:
                    if (this.mWifiRepeaterEnabled) {
                        showUserToastIfP2pCollision();
                        mIsP2pCollision = true;
                        break;
                    }
                    break;
            }
        }
        if (state instanceof WifiP2pServiceImpl.P2pStateMachine.InactiveState) {
            switch (msg.what) {
                case 139265:
                case 139274:
                case 139277:
                case 139329:
                case 139332:
                case 139335:
                    if (this.mWifiRepeaterEnabled) {
                        showUserToastIfP2pCollision();
                        mIsP2pCollision = true;
                        break;
                    }
                    break;
            }
        }
        if ((state instanceof WifiP2pServiceImpl.P2pStateMachine.GroupCreatingState) && (((i = msg.what) == 139265 || i == 139274) && this.mWifiRepeaterEnabled)) {
            showUserToastIfP2pCollision();
            mIsP2pCollision = true;
        }
        if (!(state instanceof WifiP2pServiceImpl.P2pStateMachine.GroupCreatedState)) {
            return mIsP2pCollision;
        }
        int i2 = msg.what;
        if (i2 != 139265) {
            if (i2 != 139271 || !this.mWifiRepeaterEnabled) {
                return mIsP2pCollision;
            }
            showUserToastIfP2pCollision();
            return true;
        } else if (!this.mWifiRepeaterEnabled) {
            return mIsP2pCollision;
        } else {
            if (shouldShowP2pCollisionToast(msg)) {
                showUserToastIfP2pCollision();
            }
            return true;
        }
    }

    private boolean shouldShowP2pCollisionToast(Message msg) {
        int uid = msg.sendingUid;
        String currentPkgName = getCurrentPkgName();
        String[] callingPkgNames = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (callingPkgNames == null || callingPkgNames.length == 0 || currentPkgName == null) {
            return false;
        }
        boolean isForeground = false;
        int length = callingPkgNames.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            } else if (callingPkgNames[i].equals(currentPkgName)) {
                isForeground = true;
                break;
            } else {
                i++;
            }
        }
        HwHiLog.d(TAG, false, "P2pCollision uid=%{public}d, currentPkgName=%{public}s, isForeground=%{public}s", new Object[]{Integer.valueOf(uid), currentPkgName, Boolean.valueOf(isForeground)});
        return isForeground;
    }

    private String getCurrentPkgName() {
        try {
            List<ActivityManager.RunningTaskInfo> tasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
            if (tasks == null || tasks.isEmpty()) {
                return null;
            }
            return tasks.get(0).topActivity.getPackageName();
        } catch (SecurityException e) {
            HwHiLog.d(TAG, false, "SecurityException: get current package name error", new Object[0]);
            return null;
        }
    }

    private void showUserToastIfP2pCollision() {
        Toast.makeText(this.mContext, 33685839, 0).show();
    }

    /* access modifiers changed from: protected */
    public boolean getMagicLinkDeviceFlag() {
        return this.mMagicLinkDeviceFlag;
    }

    /* access modifiers changed from: protected */
    public void setmMagicLinkDeviceFlag(boolean magicLinkDeviceFlag) {
        this.mMagicLinkDeviceFlag = magicLinkDeviceFlag;
        if (!this.mMagicLinkDeviceFlag) {
            this.mLegacyGO = false;
        }
    }

    /* access modifiers changed from: protected */
    public void notifyP2pChannelNumber(int channel) {
        if (channel > 13) {
            channel = 0;
        }
        WifiCommonUtils.notifyDeviceState("WLAN-P2P", String.valueOf(channel), "");
    }

    /* access modifiers changed from: protected */
    public void notifyP2pState(String state) {
        WifiCommonUtils.notifyDeviceState("WLAN-P2P", state, "");
    }

    private boolean configIPAddr(String ifName, String ipAddr, String gateway) {
        HwHiSLog.d(TAG, false, "configIPAddr: %{public}s %{private}s", new Object[]{ifName, StringUtilEx.safeDisplayIpAddress(ipAddr)});
        try {
            this.mNwService.enableIpv6(ifName);
            InterfaceConfiguration ifcg = new InterfaceConfiguration();
            ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(ipAddr), 24));
            ifcg.setInterfaceUp();
            this.mNwService.setInterfaceConfig(ifName, ifcg);
            RouteInfo connectedRoute = new RouteInfo(new LinkAddress((Inet4Address) NetworkUtils.numericToInetAddress(ipAddr), 24), null, ifName);
            List<RouteInfo> routes = new ArrayList<>(3);
            routes.add(connectedRoute);
            routes.add(new RouteInfo(null, NetworkUtils.numericToInetAddress(gateway), ifName));
            HwHiLog.e(TAG, false, "add new RouteInfo() gateway:%{private}s iface:%{public}s", new Object[]{StringUtilEx.safeDisplayIpAddress(gateway), ifName});
            this.mNwService.addInterfaceToLocalNetwork(ifName, routes);
        } catch (Exception e) {
            HwHiLog.e(TAG, false, "configIPAddr fail", new Object[0]);
        }
        HwHiSLog.d(TAG, false, "configIPAddr: %{public}s %{private}s* ok", new Object[]{ifName, StringUtilEx.safeDisplayIpAddress(ipAddr)});
        return true;
    }

    private boolean releaseIPAddr(String ifName) {
        if (ifName == null) {
            return false;
        }
        try {
            this.mNwService.disableIpv6(ifName);
            this.mNwService.clearInterfaceAddresses(ifName);
            return true;
        } catch (Exception e) {
            HwHiLog.e(TAG, false, "Failed to clear addresses or disable IPv6", new Object[0]);
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public int addScanChannelInTimeout(int channelID, int timeout) {
        int ret = (channelID << 16) + (timeout & 255);
        logd("discover time " + ret, new Object[0]);
        return ret;
    }

    /* access modifiers changed from: protected */
    public boolean allowP2pFind(int uid) {
        boolean allow;
        if (Process.isCoreUid(uid)) {
            return true;
        }
        boolean isBlackApp = isInBlacklistForP2pFind(uid);
        if (isScreenOn()) {
            if (isBlackApp) {
                allow = allowP2pFindByTime(uid);
            } else {
                allow = true;
            }
        } else if (isBlackApp) {
            allow = false;
        } else {
            allow = allowP2pFindByTime(uid);
        }
        if (!allow) {
            HwHiLog.d(TAG, false, "p2p find disallowed, uid:%{public}d", new Object[]{Integer.valueOf(uid)});
        }
        return allow;
    }

    /* access modifiers changed from: protected */
    public synchronized void handleP2pStopFind(int uid) {
        if (this.mP2pFindProcessInfoList != null) {
            if (uid < 0) {
                this.mP2pFindProcessInfoList.clear();
            }
            Iterator<P2pFindProcessInfo> it = this.mP2pFindProcessInfoList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                P2pFindProcessInfo p2pInfo = it.next();
                if (uid == p2pInfo.mUid) {
                    this.mP2pFindProcessInfoList.remove(p2pInfo);
                    break;
                }
            }
        }
    }

    private boolean isScreenOn() {
        PowerManager powerManager = this.mPowerManager;
        if (powerManager == null || powerManager.isScreenOn()) {
            return true;
        }
        return false;
    }

    private boolean isInBlacklistForP2pFind(int uid) {
        PackageManager pkgMgr;
        Context context = this.mContext;
        if (context == null || (pkgMgr = context.getPackageManager()) == null) {
            return false;
        }
        String pkgName = pkgMgr.getNameForUid(uid);
        for (String black : BLACKLIST_P2P_FIND) {
            if (black.equals(pkgName)) {
                HwHiLog.d(TAG, false, "p2p-find blacklist: %{public}s", new Object[]{pkgName});
                return true;
            }
        }
        return false;
    }

    private synchronized boolean allowP2pFindByTime(int uid) {
        if (this.mP2pFindProcessInfoList == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        this.mP2pFindProcessInfoList.clear();
        this.mP2pFindProcessInfoList.addAll((List) this.mP2pFindProcessInfoList.stream().filter(new Predicate(now) {
            /* class com.android.server.wifi.p2p.$$Lambda$HwWifiP2pService$mTGzbNymvkAXSXqBrM8otRFbQ0 */
            private final /* synthetic */ long f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return HwWifiP2pService.lambda$allowP2pFindByTime$0(this.f$0, (P2pFindProcessInfo) obj);
            }
        }).collect(Collectors.toList()));
        for (P2pFindProcessInfo p2pInfo : this.mP2pFindProcessInfoList) {
            if (uid == p2pInfo.mUid) {
                return false;
            }
        }
        this.mP2pFindProcessInfoList.add(new P2pFindProcessInfo(uid, now));
        return true;
    }

    static /* synthetic */ boolean lambda$allowP2pFindByTime$0(long now, P2pFindProcessInfo P2pFindProcessInfo2) {
        return now - P2pFindProcessInfo2.mLastP2pFindTimestamp <= INTERVAL_DISALLOW_P2P_FIND;
    }

    /* access modifiers changed from: private */
    public class P2pFindProcessInfo {
        public long mLastP2pFindTimestamp;
        public int mUid;

        public P2pFindProcessInfo(int uid, long p2pFindTimestamp) {
            this.mUid = uid;
            this.mLastP2pFindTimestamp = p2pFindTimestamp;
        }
    }

    /* access modifiers changed from: protected */
    public void processStatistics(Context mContext2, int eventID, int choice) {
        JSONObject eventMsg = new JSONObject();
        try {
            eventMsg.put("choice", choice);
        } catch (JSONException e) {
            loge("processStatistics put error." + e, new Object[0]);
        }
        Flog.bdReport(mContext2, eventID, eventMsg);
    }

    /* access modifiers changed from: protected */
    public boolean isMiracastDevice(String deviceType) {
        if (deviceType == null) {
            return false;
        }
        String[] tokens = deviceType.split("-");
        try {
            if (tokens.length > 0 && Integer.parseInt(tokens[0]) == 7) {
                logd("As connecting miracast device ,set go_intent = 14 to let it works as GO ", new Object[0]);
                return true;
            }
        } catch (NumberFormatException e) {
            loge("isMiracastDevice: " + e, new Object[0]);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean wifiIsConnected() {
        WifiManager wifiMgr;
        NetworkInfo wifiInfo;
        Context context = this.mContext;
        if (context == null || (wifiMgr = (WifiManager) context.getSystemService("wifi")) == null || wifiMgr.getWifiState() != 3 || (wifiInfo = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getNetworkInfo(1)) == null) {
            return false;
        }
        logd("wifiIsConnected: " + wifiInfo.isConnected(), new Object[0]);
        return wifiInfo.isConnected();
    }

    /* access modifiers changed from: protected */
    public void sendReinvokePGBroadcast(int netId) {
        Intent intent = new Intent("com.huawei.net.wifi.p2p.REINVOKE_PERSISTENT_GROUP_ACTION");
        intent.putExtra("com.huawei.net.wifi.p2p.EXTRA_REINVOKE_NETID", netId);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.permission.REINVOKE_PERSISTENT");
    }

    /* access modifiers changed from: protected */
    public String getCustomDeviceName(String deviceName) {
        String deviceName2;
        if (!SystemProperties.getBoolean("ro.config.hw_wifi_bt_name", false) || !TextUtils.isEmpty(deviceName)) {
            return deviceName;
        }
        StringBuilder sb = new StringBuilder();
        String uuidStr = UUID.randomUUID().toString();
        String marketing_name = SystemProperties.get("ro.config.marketing_name");
        if (!TextUtils.isEmpty(marketing_name)) {
            sb.append(marketing_name);
            sb.append("_");
            sb.append(uuidStr.substring(24, 28).toUpperCase(Locale.US));
            deviceName2 = sb.toString();
        } else {
            sb.append("HUAWEI ");
            sb.append(Build.PRODUCT);
            sb.append("_");
            sb.append(uuidStr.substring(24, 28).toUpperCase(Locale.US));
            deviceName2 = sb.toString();
        }
        Settings.Global.putString(this.mContext.getContentResolver(), "wifi_p2p_device_name", deviceName2);
        return deviceName2;
    }

    /* access modifiers changed from: protected */
    public String getSsidPostFix(String deviceName) {
        String ssidPostFix = deviceName;
        if (ssidPostFix == null) {
            return ssidPostFix;
        }
        byte[] ssidPostFixBytes = ssidPostFix.getBytes();
        while (ssidPostFixBytes.length > 22) {
            ssidPostFix = ssidPostFix.substring(0, ssidPostFix.length() - 1);
            ssidPostFixBytes = ssidPostFix.getBytes();
        }
        if (ssidPostFixBytes.length != 14) {
            return ssidPostFix;
        }
        return ssidPostFix + " ";
    }

    /* access modifiers changed from: protected */
    public boolean isWifiP2pForbidden(int msgWhat) {
        boolean isConnect = msgWhat == 139271;
        boolean isDiscoverPeers = msgWhat == 139265;
        boolean isRequestPeers = msgWhat == 139283;
        if (!HwDeviceManager.disallowOp(45) || (!isConnect && !isDiscoverPeers && !isRequestPeers)) {
            return false;
        }
        HwHiSLog.d(TAG, false, "wifiP2P function is forbidden,msg.what = %{public}d", new Object[]{Integer.valueOf(msgWhat)});
        Context context = this.mContext;
        Toast.makeText(context, context.getResources().getString(33686008), 0).show();
        return true;
    }

    private boolean isInDisableDhcpWhiteList(String packageName) {
        for (String whitePackageName : DISABLE_DHCP_WHITE_PACKAGE_NAME_LIST) {
            if (whitePackageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void setCurDisbleDhcpPackageName(int uid) {
        if (!TextUtils.isEmpty(this.mCurDisbleDhcpPackageName) && uid > 10000) {
            if (!this.mCurDisbleDhcpPackageName.equals(this.mContext.getPackageManager().getNameForUid(uid))) {
                this.mCurDisbleDhcpPackageName = "";
            }
        }
    }

    public boolean disableP2pGcDhcp(String packageName, int uid, int type) {
        if (type != 1) {
            HwHiSLog.e(TAG, false, "disableP2pGcDhcp: type is none", new Object[0]);
            return false;
        }
        String realPackageName = packageName;
        if (uid > 10000) {
            String appName = this.mContext.getPackageManager().getNameForUid(uid);
            if (appName == null) {
                HwHiSLog.e(TAG, false, "disableP2pGcDhcp: appName is null", new Object[0]);
                return false;
            }
            realPackageName = appName;
        }
        this.mCurDisbleDhcpPackageName = realPackageName;
        HwHiSLog.i(TAG, false, "disableP2pGcDhcp: appName disable p2p Gc Dhcp %{public}s", new Object[]{this.mCurDisbleDhcpPackageName});
        if (!isInDisableDhcpWhiteList(realPackageName)) {
            HwHiSLog.e(TAG, false, "disableP2pGcDhcp: appName is not in white list", new Object[0]);
            return false;
        }
        this.mDisbleP2pGcDhcpTime = SystemClock.elapsedRealtime();
        synchronized (this.mLock) {
            this.mDisbleGcDhcpList.put(realPackageName, Integer.valueOf(type));
        }
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0027, code lost:
        if ((android.os.SystemClock.elapsedRealtime() - r9.mDisbleP2pGcDhcpTime) <= 10000) goto L_0x0036;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0029, code lost:
        android.util.wifi.HwHiSLog.i(com.android.server.wifi.p2p.HwWifiP2pService.TAG, false, "called shouldDisableP2pGcDhcp after disableP2pGcDhcp too long time", new java.lang.Object[0]);
        removeDisableP2pGcDhcp(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0035, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0036, code lost:
        r6 = r9.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0038, code lost:
        monitor-enter(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x003f, code lost:
        if (r9.mDisbleGcDhcpList.containsKey(r0) == false) goto L_0x004c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0041, code lost:
        android.util.wifi.HwHiSLog.i(com.android.server.wifi.p2p.HwWifiP2pService.TAG, false, "should Disable P2p Gc Dhcp", new java.lang.Object[0]);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x004a, code lost:
        monitor-exit(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x004b, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x004c, code lost:
        monitor-exit(r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x004d, code lost:
        return false;
     */
    public boolean shouldDisableP2pGcDhcp() {
        String packageName = this.mCurDisbleDhcpPackageName;
        synchronized (this.mLock) {
            if (this.mDisbleGcDhcpList != null && !this.mDisbleGcDhcpList.isEmpty()) {
                if (TextUtils.isEmpty(packageName)) {
                }
            }
            HwHiSLog.e(TAG, false, "shouldDisableP2pGcDhcp: mDisbleGcDhcpList is null, do not need DisableP2pGcDhcp", new Object[0]);
            return false;
        }
    }

    public void removeDisableP2pGcDhcp(boolean shouldRemoveAll) {
        String packageName = this.mCurDisbleDhcpPackageName;
        synchronized (this.mLock) {
            if (this.mDisbleGcDhcpList == null) {
                HwHiSLog.e(TAG, false, "removeDisableP2pGcDhcp: mDisbleGcDhcpList is null", new Object[0]);
                return;
            }
            if (shouldRemoveAll) {
                this.mDisbleGcDhcpList.clear();
            } else if (!TextUtils.isEmpty(packageName) && this.mDisbleGcDhcpList.containsKey(packageName) && this.mDisbleGcDhcpList.get(packageName).equals(1)) {
                this.mDisbleGcDhcpList.remove(packageName);
                HwHiSLog.i(TAG, false, "removeDisableP2pGcDhcp enter", new Object[0]);
            }
            this.mCurDisbleDhcpPackageName = "";
            this.mDisbleP2pGcDhcpTime = -10000;
        }
    }

    private boolean hasDisableP2pRandomMacPermission(int uid) {
        WifiInjector wifiInjector = WifiInjector.getInstance();
        if (wifiInjector == null) {
            return false;
        }
        WifiPermissionsWrapper wifiPermissionsWrapper = wifiInjector.getWifiPermissionsWrapper();
        if (wifiPermissionsWrapper == null) {
            HwHiSLog.i(TAG, false, "wifiPermissionsWrapper is null when check disable p2p random mac permission", new Object[0]);
            return false;
        }
        boolean disableP2pRandomMacPermission = wifiPermissionsWrapper.getUidPermission(PERMISSION_DISABLE_P2P_RANDOM_MAC, uid) != -1;
        boolean accessWifiStatePermission = wifiPermissionsWrapper.getUidPermission("android.permission.ACCESS_WIFI_STATE", uid) != -1;
        if (!disableP2pRandomMacPermission || !accessWifiStatePermission) {
            return false;
        }
        return true;
    }

    private boolean isInDisableP2pRandomMacWhiteList(int uid, String packageName) {
        if (uid == 1000) {
            return true;
        }
        for (String whitePackageName : DISABLE_P2P_RANDOM_MAC_WHITE_PACKAGE_NAME_LIST) {
            if (whitePackageName.equals(packageName)) {
                return true;
            }
        }
        HwHiSLog.i(TAG, false, "not in white list to disable p2p random mac", new Object[0]);
        return false;
    }

    /* access modifiers changed from: private */
    public synchronized boolean ableToDisableP2pRandomMac(int uid) {
        if (this.mContext == null) {
            return false;
        }
        try {
            if (!this.mContext.getResources().getBoolean(17891592)) {
                HwHiSLog.i(TAG, false, "not support feature: P2P MAC randomization", new Object[0]);
                return false;
            }
            PackageManager pkgMgr = this.mContext.getPackageManager();
            if (pkgMgr == null) {
                return false;
            }
            String pkgName = pkgMgr.getNameForUid(uid);
            if (pkgName == null) {
                HwHiSLog.i(TAG, false, "pkgName is null when get name for uid in ableToDisableP2pRandomMac", new Object[0]);
                return false;
            } else if (hasDisableP2pRandomMacPermission(uid) && isInDisableP2pRandomMacWhiteList(uid, pkgName)) {
                return true;
            } else {
                HwHiSLog.i(TAG, false, "no permission or not in white list to disable p2p random mac", new Object[0]);
                return false;
            }
        } catch (Resources.NotFoundException e) {
            HwHiSLog.w(TAG, false, "not found config_wifi_p2p_mac_randomization_supported", new Object[0]);
            return false;
        }
    }

    /* access modifiers changed from: private */
    public boolean isAbleToSetSinkConfig(int uid) {
        PackageManager pkgMgr;
        if (!IS_TV) {
            loge("setSinkConfig only works on TV.", new Object[0]);
            return false;
        }
        Context context = this.mContext;
        if (context == null || (pkgMgr = context.getPackageManager()) == null) {
            return false;
        }
        String pkgName = pkgMgr.getNameForUid(uid);
        if (pkgName == null) {
            loge("calling package name is null.", new Object[0]);
            return false;
        } else if (isInSetSinkConfigWhiteList(uid, pkgName) && hasSetSinkConfigPermission(uid)) {
            return true;
        } else {
            loge("calling package is not in whitelist or no permission.", new Object[0]);
            return false;
        }
    }

    private boolean isInSetSinkConfigWhiteList(int uid, String packageName) {
        if (uid == 1000) {
            return true;
        }
        for (String whitePackageName : SET_HWSINK_CONFIG_WHITE_PACKAGE_NAME_LIST) {
            if (whitePackageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSetSinkConfigPermission(int uid) {
        WifiPermissionsWrapper wifiPermissionsWrapper;
        WifiInjector wifiInjector = WifiInjector.getInstance();
        if (wifiInjector == null || (wifiPermissionsWrapper = wifiInjector.getWifiPermissionsWrapper()) == null || wifiPermissionsWrapper.getUidPermission(PERMISSION_SET_SINK_CONFIG, uid) == -1) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void updateP2pGoCreateStatus(boolean isSuccessed) {
        loge("updateP2pGoCreateStatus for create GO failure", new Object[0]);
        if (isSuccessed) {
            this.mP2pCreateGoFailTimes = 0;
            return;
        }
        this.mP2pCreateGoFailTimes++;
        if (this.mP2pCreateGoFailTimes >= 2) {
            this.mP2pCreateGoFailTimes = 0;
            this.mP2pStateMachine.sendMessage((int) CMD_SELFCURE_GO_CREATE_FAIL);
        }
    }

    public void setWifiP2pListenMode() {
        WifiInjector wifiInjector = WifiInjector.getInstance();
        if (wifiInjector != null) {
            WifiP2pNative wifiNative = wifiInjector.getWifiP2pNative();
            if (wifiNative == null || wifiNative.mHwWifiP2pNativeEx == null) {
                loge("setWifiP2pListenMode wifiP2pNative is null", new Object[0]);
                return;
            }
            logd("setWifiP2pListenMode", new Object[0]);
            wifiNative.mHwWifiP2pNativeEx.deliverP2pData(2, 4, "1");
        }
    }
}
