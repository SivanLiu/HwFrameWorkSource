package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.IpConfiguration.IpAssignment;
import android.net.KeepalivePacketData;
import android.net.KeepalivePacketData.InvalidPacketException;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkMisc;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.dhcp.DhcpClient;
import android.net.ip.IpClient;
import android.net.ip.IpClient.Callback;
import android.net.ip.IpClient.ProvisioningConfiguration;
import android.net.wifi.IClientInterface;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.WifiScanner.ScanListener;
import android.net.wifi.WpsInfo;
import android.net.wifi.WpsResult;
import android.net.wifi.WpsResult.Status;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.p2p.IWifiP2pManager;
import android.net.wifi.wifipro.HwNetworkAgent;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.system.OsConstants;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.app.IBatteryStats.Stub;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.server.wifi.ClientModeManager.Listener;
import com.android.server.wifi.WifiBackupRestore.SupplicantBackupMigration;
import com.android.server.wifi.WifiMulticastLockManager.FilterController;
import com.android.server.wifi.WifiNative.SignalPollResult;
import com.android.server.wifi.WifiNative.TxPacketCounters;
import com.android.server.wifi.WifiNative.WifiRssiEventHandler;
import com.android.server.wifi.hotspot2.AnqpEvent;
import com.android.server.wifi.hotspot2.IconEvent;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.NetworkDetail.Ant;
import com.android.server.wifi.hotspot2.PasspointManager;
import com.android.server.wifi.hotspot2.WnmData;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.scanner.ChannelHelper;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.StringUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.android.server.wifi.util.TelephonyUtil.SimAuthRequestData;
import com.android.server.wifi.util.TelephonyUtil.SimAuthResponseData;
import com.android.server.wifi.util.WifiCommonUtils;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import com.android.server.wifi.wificond.NativeMssResult;
import huawei.cust.HwCustUtils;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WifiStateMachine extends AbsWifiStateMachine {
    private static final long ALLOW_SEND_HILINK_SCAN_RESULTS_BROADCAST_INTERVAL_MS = 3000;
    static final int BASE = 131072;
    private static final String BSSID_TO_CONNECT = "bssid_to_connect";
    static final int CMD_ACCEPT_UNVALIDATED = 131225;
    static final int CMD_ADD_OR_UPDATE_NETWORK = 131124;
    static final int CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG = 131178;
    static final int CMD_ASSOCIATED_BSSID = 131219;
    static final int CMD_BLUETOOTH_ADAPTER_STATE_CHANGE = 131103;
    static final int CMD_BOOT_COMPLETED = 131206;
    public static final int CMD_CHANGE_TO_AP_P2P_CONNECT = 131574;
    public static final int CMD_CHANGE_TO_STA_P2P_CONNECT = 131573;
    static final int CMD_CONFIG_ND_OFFLOAD = 131276;
    private static final int CMD_DIAGS_CONNECT_TIMEOUT = 131324;
    static final int CMD_DISABLE_EPHEMERAL_NETWORK = 131170;
    public static final int CMD_DISABLE_P2P_REQ = 131204;
    public static final int CMD_DISABLE_P2P_RSP = 131205;
    static final int CMD_DISABLE_P2P_WATCHDOG_TIMER = 131184;
    static final int CMD_DISCONNECT = 131145;
    static final int CMD_DISCONNECTING_WATCHDOG_TIMER = 131168;
    static final int CMD_ENABLE_NETWORK = 131126;
    public static final int CMD_ENABLE_P2P = 131203;
    static final int CMD_ENABLE_RSSI_POLL = 131154;
    static final int CMD_ENABLE_TDLS = 131164;
    static final int CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER = 131238;
    static final int CMD_GET_ALL_MATCHING_CONFIGS = 131240;
    static final int CMD_GET_CHANNEL_LIST_5G = 131572;
    static final int CMD_GET_CONFIGURED_NETWORKS = 131131;
    static final int CMD_GET_LINK_LAYER_STATS = 131135;
    static final int CMD_GET_MATCHING_CONFIG = 131171;
    static final int CMD_GET_MATCHING_OSU_PROVIDERS = 131181;
    static final int CMD_GET_PASSPOINT_CONFIGS = 131180;
    static final int CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS = 131134;
    static final int CMD_GET_SUPPORTED_FEATURES = 131133;
    static final int CMD_GET_SUPPORT_VOWIFI_DETECT = 131774;
    static final int CMD_INITIALIZE = 131207;
    static final int CMD_INSTALL_PACKET_FILTER = 131274;
    static final int CMD_IPV4_PROVISIONING_FAILURE = 131273;
    static final int CMD_IPV4_PROVISIONING_SUCCESS = 131272;
    static final int CMD_IP_CONFIGURATION_LOST = 131211;
    static final int CMD_IP_CONFIGURATION_SUCCESSFUL = 131210;
    static final int CMD_IP_REACHABILITY_LOST = 131221;
    static final int CMD_MATCH_PROVIDER_NETWORK = 131177;
    static final int CMD_NETWORK_STATUS = 131220;
    static final int CMD_PNO_PERIODIC_SCAN = 131575;
    static final int CMD_QUERY_OSU_ICON = 131176;
    static final int CMD_READ_PACKET_FILTER = 131280;
    static final int CMD_REASSOCIATE = 131147;
    static final int CMD_RECONNECT = 131146;
    static final int CMD_RELOAD_TLS_AND_RECONNECT = 131214;
    static final int CMD_REMOVE_APP_CONFIGURATIONS = 131169;
    static final int CMD_REMOVE_NETWORK = 131125;
    static final int CMD_REMOVE_PASSPOINT_CONFIG = 131179;
    static final int CMD_REMOVE_USER_CONFIGURATIONS = 131224;
    static final int CMD_RESET_SIM_NETWORKS = 131173;
    static final int CMD_RESET_SUPPLICANT_STATE = 131183;
    static final int CMD_ROAM_WATCHDOG_TIMER = 131166;
    static final int CMD_RSSI_POLL = 131155;
    static final int CMD_RSSI_THRESHOLD_BREACHED = 131236;
    public static final int CMD_SCE_HANDLE_IP_INVALID = 131895;
    public static final int CMD_SCE_HANDLE_IP_NO_INTERNET = 131898;
    public static final int CMD_SCE_NOTIFY_WIFI_DISABLED = 131894;
    public static final int CMD_SCE_RESTORE = 131893;
    public static final int CMD_SCE_STOP_SELF_CURE = 131892;
    public static final int CMD_SCE_WIFI_CONNECT_TIMEOUT = 131890;
    public static final int CMD_SCE_WIFI_OFF_TIMEOUT = 131888;
    public static final int CMD_SCE_WIFI_ON_TIMEOUT = 131889;
    public static final int CMD_SCE_WIFI_REASSOC_TIMEOUT = 131891;
    public static final int CMD_SCE_WIFI_RECONNECT_TIMEOUT = 131896;
    static final int CMD_SCREEN_STATE_CHANGED = 131167;
    static final int CMD_SET_DETECTMODE_CONF = 131772;
    static final int CMD_SET_DETECT_PERIOD = 131773;
    static final int CMD_SET_FALLBACK_PACKET_FILTERING = 131275;
    static final int CMD_SET_HIGH_PERF_MODE = 131149;
    static final int CMD_SET_OPERATIONAL_MODE = 131144;
    static final int CMD_SET_SUSPEND_OPT_ENABLED = 131158;
    static final int CMD_START_CONNECT = 131215;
    static final int CMD_START_IP_PACKET_OFFLOAD = 131232;
    static final int CMD_START_ROAM = 131217;
    static final int CMD_START_RSSI_MONITORING_OFFLOAD = 131234;
    private static final int CMD_START_SUBSCRIPTION_PROVISIONING = 131326;
    static final int CMD_STATIC_IP_FAILURE = 131088;
    static final int CMD_STATIC_IP_SUCCESS = 131087;
    static final int CMD_STOP_IP_PACKET_OFFLOAD = 131233;
    static final int CMD_STOP_RSSI_MONITORING_OFFLOAD = 131235;
    public static final int CMD_STOP_WIFI_REPEATER = 131577;
    static final int CMD_TARGET_BSSID = 131213;
    static final int CMD_TEST_NETWORK_DISCONNECT = 131161;
    static final int CMD_UNWANTED_NETWORK = 131216;
    static final int CMD_UPDATE_LINKPROPERTIES = 131212;
    public static final int CMD_UPDATE_WIFIPRO_CONFIGURATIONS = 131672;
    static final int CMD_USER_STOP = 131279;
    static final int CMD_USER_SWITCH = 131277;
    static final int CMD_USER_UNLOCK = 131278;
    public static final int CMD_WIFI_SCAN_REJECT_SEND_SCAN_RESULT = 131578;
    static final int CMD_WPS_PIN_RETRY = 131576;
    private static final String CONNECT_FROM_USER = "connect_from_user";
    public static final int CONNECT_MODE = 1;
    private static final int CONNECT_REQUEST_DELAY_MSECS = 50;
    private static boolean DBG = HWFLOW;
    private static final int DEFAULT_MTU = 1500;
    private static final int DEFAULT_POLL_RSSI_INTERVAL_MSECS = 3000;
    private static final int DEFAULT_WIFI_AP_CHANNEL = 0;
    private static final int DEFAULT_WIFI_AP_MAXSCB = 8;
    private static final long DIAGS_CONNECT_TIMEOUT_MILLIS = 60000;
    public static final int DISABLED_MODE = 4;
    static final int DISABLE_P2P_GUARD_TIMER_MSEC = 2000;
    static final int DISCONNECTING_GUARD_TIMER_MSEC = 5000;
    private static final int DRIVER_STARTED = 1;
    private static final int DRIVER_STOPPED = 2;
    private static final boolean ENABLE_DHCP_AFTER_ROAM = SystemProperties.getBoolean("ro.config.roam_force_dhcp", false);
    private static final String EXTRA_OSU_ICON_QUERY_BSSID = "BSSID";
    private static final String EXTRA_OSU_ICON_QUERY_FILENAME = "FILENAME";
    private static final String EXTRA_OSU_PROVIDER = "OsuProvider";
    private static final int FAILURE = -1;
    public static final int GOOD_LINK_DETECTED = 131874;
    private static final String GOOGLE_OUI = "DA-A1-19";
    protected static final boolean HWFLOW;
    private static boolean HWLOGW_E = true;
    private static final int IMSI_RECONNECT_LIMIT = 3;
    public static final int INVALID_LINK_DETECTED = 131875;
    private static final long LAST_AUTH_FAILURE_GAP = 100;
    @VisibleForTesting
    public static final int LAST_SELECTED_NETWORK_EXPIRATION_AGE_MILLIS = 30000;
    private static final int LINK_FLAPPING_DEBOUNCE_MSEC = 4000;
    private static final String LOGD_LEVEL_DEBUG = "D";
    private static final String LOGD_LEVEL_VERBOSE = "V";
    private static final int MESSAGE_HANDLING_STATUS_DEFERRED = -4;
    private static final int MESSAGE_HANDLING_STATUS_DISCARD = -5;
    private static final int MESSAGE_HANDLING_STATUS_FAIL = -2;
    private static final int MESSAGE_HANDLING_STATUS_HANDLING_ERROR = -7;
    private static final int MESSAGE_HANDLING_STATUS_LOOPED = -6;
    private static final int MESSAGE_HANDLING_STATUS_OBSOLETE = -3;
    private static final int MESSAGE_HANDLING_STATUS_OK = 1;
    private static final int MESSAGE_HANDLING_STATUS_PROCESSED = 2;
    private static final int MESSAGE_HANDLING_STATUS_REFUSED = -1;
    private static final int MESSAGE_HANDLING_STATUS_UNKNOWN = 0;
    private static final String NETWORKTYPE = "WIFI";
    private static final String NETWORKTYPE_UNTRUSTED = "WIFI_UT";
    private static final int NETWORK_STATUS_UNWANTED_DISABLE_AUTOJOIN = 2;
    private static final int NETWORK_STATUS_UNWANTED_DISCONNECT = 0;
    private static final int NETWORK_STATUS_UNWANTED_VALIDATION_FAILED = 1;
    private static final int NET_ID_NONE = -1;
    @VisibleForTesting
    public static final short NUM_LOG_RECS_NORMAL = (short) 100;
    @VisibleForTesting
    public static final short NUM_LOG_RECS_VERBOSE = (short) 3000;
    @VisibleForTesting
    public static final short NUM_LOG_RECS_VERBOSE_LOW_MEMORY = (short) 200;
    private static final int ONE_HOUR_MILLI = 3600000;
    private static boolean PDBG = HWFLOW;
    public static final int POOR_LINK_DETECTED = 131873;
    static final int ROAM_GUARD_TIMER_MSEC = 15000;
    public static final int SCAN_ONLY_MODE = 2;
    public static final int SCAN_ONLY_WITH_WIFI_OFF_MODE = 3;
    public static final int SCE_EVENT_CONN_CHANGED = 103;
    public static final int SCE_EVENT_NET_INFO_CHANGED = 102;
    public static final int SCE_EVENT_WIFI_STATE_CHANGED = 101;
    public static final int SCE_REQUEST_REASSOC_WIFI = 131886;
    public static final int SCE_REQUEST_RENEW_DHCP = 131883;
    public static final int SCE_REQUEST_RESET_WIFI = 131887;
    public static final int SCE_REQUEST_SET_STATIC_IP = 131884;
    public static final int SCE_REQUEST_UPDATE_DNS_SERVER = 131882;
    public static final int SCE_START_SET_STATIC_IP = 131885;
    private static final String SOFTAP_IFACE = "wlan0";
    private static final int SUCCESS = 1;
    public static final String SUPPLICANT_BSSID_ANY = "any";
    private static final int SUPPLICANT_RESTART_INTERVAL_MSECS = 5000;
    private static final int SUPPLICANT_RESTART_TRIES = 5;
    private static final int SUSPEND_DUE_TO_DHCP = 1;
    private static final int SUSPEND_DUE_TO_HIGH_PERF = 2;
    private static final int SUSPEND_DUE_TO_SCREEN = 4;
    private static final String SYSTEM_PROPERTY_LOG_CONTROL_WIFIHAL = "log.tag.WifiHAL";
    private static final String TAG = "WifiStateMachine";
    private static boolean USE_PAUSE_SCANS = false;
    private static boolean VDBG = false;
    private static boolean VVDBG = false;
    public static final int WIFIPRO_SOFT_CONNECT_TIMEOUT = 131897;
    private static final String WIFI_DRIVER_CHANGE_ACTION = "huawei.intent.action.WIFI_DRIVER_CHANGE";
    private static final String WIFI_DRIVER_CHANGE_PERMISSION = "com.huawei.powergenie.receiverPermission";
    private static final String WIFI_DRIVER_STATE = "wifi_driver_state";
    public static final WorkSource WIFI_WORK_SOURCE = new WorkSource(1010);
    private static final int WPS_PIN_RETRY_INTERVAL_MSECS = 50000;
    private static boolean mLogMessages = HWFLOW;
    private static final Class[] sMessageClasses = new Class[]{AsyncChannel.class, WifiStateMachine.class, DhcpClient.class};
    private static int sScanAlarmIntentCount = 0;
    private static final SparseArray<String> sSmToString = MessageUtils.findMessageNames(sMessageClasses);
    private boolean didBlackListBSSID = false;
    int disconnectingWatchdogCount = 0;
    private boolean isBootCompleted = false;
    private long lastConnectAttemptTimestamp = 0;
    private long lastLinkLayerStatsUpdate = 0;
    private long lastOntimeReportTimeStamp = 0;
    private Set<Integer> lastScanFreqs = null;
    private long lastScreenStateChangeTimeStamp = 0;
    private final BackupManagerProxy mBackupManagerProxy;
    private final IBatteryStats mBatteryStats;
    private boolean mBluetoothConnectionActive = false;
    private final BuildProperties mBuildProperties;
    private IClientInterface mClientInterface;
    private Listener mClientModeCallback = null;
    private final Clock mClock;
    private ConnectivityManager mCm;
    private State mConnectModeState = new ConnectModeState();
    private State mConnectedState = new ConnectedState();
    private long mConnectingStartTimestamp = 0;
    @GuardedBy("mWifiReqCountLock")
    private int mConnectionReqCount = 0;
    private Context mContext;
    private final WifiCountryCode mCountryCode;
    private int mCurrentAssociateNetworkId = -1;
    HwCustWifiStateMachineReference mCust = ((HwCustWifiStateMachineReference) HwCustUtils.createObj(HwCustWifiStateMachineReference.class, new Object[0]));
    private State mDefaultState = new DefaultState();
    private final NetworkCapabilities mDfltNetworkCapabilities;
    private DhcpResults mDhcpResults;
    private final Object mDhcpResultsLock = new Object();
    private long mDiagsConnectionStartMillis = -1;
    int mDisableP2pWatchdogCount = 0;
    private State mDisconnectedState = new DisconnectedState();
    private long mDisconnectedTimeStamp = 0;
    private State mDisconnectingState = new DisconnectingState();
    private AtomicBoolean mEnableConnectedMacRandomization = new AtomicBoolean(false);
    private boolean mEnableRssiPolling = false;
    private FrameworkFacade mFacade;
    private int mFeatureSet = 0;
    private HwMSSHandlerManager mHwMssHandler;
    private HwWifiCHRService mHwWifiCHRService;
    private String mInterfaceName;
    private IpClient mIpClient;
    private boolean mIpReachabilityDisconnectEnabled = true;
    private boolean mIsAutoRoaming = false;
    private boolean mIsFactoryFirstEnter = true;
    private boolean mIsImsiAvailable = true;
    public boolean mIsRandomMacCleared = false;
    private boolean mIsRealReboot = false;
    private boolean mIsRunning = false;
    private State mL2ConnectedState = new L2ConnectedState();
    private long mLastAllowSendHiLinkScanResultsBroadcastTime = 0;
    private long mLastAuthFailureTimestamp = Long.MIN_VALUE;
    private String mLastBssid;
    private volatile WifiConfiguration mLastConnectConfig = null;
    private long mLastDriverRoamAttempt = 0;
    private int mLastNetworkId;
    private final WorkSource mLastRunningWifiUids = new WorkSource();
    private int mLastSignalLevel = -1;
    private LinkProperties mLinkProperties;
    private final McastLockManagerFilterController mMcastLockManagerFilterController;
    private boolean mModeChange = false;
    private WifiNetworkAgent mNetworkAgent;
    private final NetworkCapabilities mNetworkCapabilitiesFilter = new NetworkCapabilities();
    private WifiNetworkFactory mNetworkFactory;
    private NetworkInfo mNetworkInfo;
    private final NetworkMisc mNetworkMisc = new NetworkMisc();
    private INetworkManagementService mNwService;
    private State mObtainingIpState = new ObtainingIpState();
    private int mOnTime = 0;
    private int mOnTimeLastReport = 0;
    private int mOnTimeScreenStateChange = 0;
    private int mOperationalMode = 4;
    private final AtomicBoolean mP2pConnected = new AtomicBoolean(false);
    private final boolean mP2pSupported;
    private final PasspointManager mPasspointManager;
    private int mPeriodicScanToken = 0;
    private volatile int mPollRssiIntervalMsecs = DEFAULT_POLL_RSSI_INTERVAL_MSECS;
    private final String mPrimaryDeviceType;
    private final PropertyService mPropertyService;
    private AsyncChannel mReplyChannel = new AsyncChannel();
    private boolean mReportedRunning = false;
    private int mRoamFailCount = 0;
    private State mRoamingState = new RoamingState();
    private int mRssiPollToken = 0;
    private byte[] mRssiRanges;
    int mRunningBeaconCount = 0;
    private final WorkSource mRunningWifiUids = new WorkSource();
    private int mRxTime = 0;
    private int mRxTimeLastReport = 0;
    private final SarManager mSarManager;
    private ScanRequestProxy mScanRequestProxy;
    private boolean mScreenOn = false;
    private long mSupplicantScanIntervalMs;
    private SupplicantStateTracker mSupplicantStateTracker;
    private int mSuspendOptNeedsDisabled = 0;
    private WakeLock mSuspendWakeLock;
    private int mTargetNetworkId = -1;
    private String mTargetRoamBSSID = "any";
    private final String mTcpBufferSizes;
    private TelephonyManager mTelephonyManager;
    private boolean mTemporarilyDisconnectWifi = false;
    private String mTls12ConfKey = null;
    private int mTrackEapAuthFailCount = 0;
    private int mTxTime = 0;
    private int mTxTimeLastReport = 0;
    private UntrustedWifiNetworkFactory mUntrustedNetworkFactory;
    @GuardedBy("mWifiReqCountLock")
    private int mUntrustedReqCount = 0;
    private AtomicBoolean mUserWantsSuspendOpt = new AtomicBoolean(true);
    private boolean mVerboseLoggingEnabled = false;
    private int mVerboseLoggingLevel = 0;
    private WakeLock mWakeLock;
    private final AtomicInteger mWifiApState = new AtomicInteger(1);
    private WifiConfigManager mWifiConfigManager;
    protected WifiConnectivityManager mWifiConnectivityManager;
    private BaseWifiDiagnostics mWifiDiagnostics;
    private final ExtendedWifiInfo mWifiInfo;
    private WifiInjector mWifiInjector;
    private WifiMetrics mWifiMetrics;
    private WifiMonitor mWifiMonitor;
    private WifiNative mWifiNative;
    private AsyncChannel mWifiP2pChannel;
    private WifiP2pServiceImpl mWifiP2pServiceImpl;
    private WifiPermissionsUtil mWifiPermissionsUtil;
    private final WifiPermissionsWrapper mWifiPermissionsWrapper;
    private WifiRepeater mWifiRepeater;
    private final Object mWifiReqCountLock = new Object();
    private final WifiScoreReport mWifiScoreReport;
    private WifiSettingsStore mWifiSettingStore;
    private final AtomicInteger mWifiState = new AtomicInteger(1);
    public WifiStateMachineHisiExt mWifiStateMachineHisiExt = null;
    private WifiStateTracker mWifiStateTracker;
    private State mWpsRunningState = new WpsRunningState();
    private final WrongPasswordNotifier mWrongPasswordNotifier;
    private int messageHandlingStatus = 0;
    int roamWatchdogCount = 0;
    private WifiConfiguration targetWificonfiguration = null;
    private boolean testNetworkDisconnect = false;
    private int testNetworkDisconnectCounter = 0;
    private DataUploader uploader;

    class ConnectModeState extends State {
        ConnectModeState() {
        }

        public void enter() {
            String str = WifiStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("entering ConnectModeState: ifaceName = ");
            stringBuilder.append(WifiStateMachine.this.mInterfaceName);
            Log.d(str, stringBuilder.toString());
            WifiStateMachine.this.mOperationalMode = 1;
            WifiStateMachine.this.setupClientMode();
            if (!WifiStateMachine.this.mWifiNative.removeAllNetworks(WifiStateMachine.this.mInterfaceName)) {
                WifiStateMachine.this.loge("Failed to remove networks on entering connect mode");
            }
            WifiStateMachine.this.mScanRequestProxy.enableScanningForHiddenNetworks(true);
            WifiStateMachine.this.mWifiInfo.reset();
            WifiStateMachine.this.mWifiInfo.setSupplicantState(SupplicantState.DISCONNECTED);
            WifiStateMachine.this.mWifiInjector.getWakeupController().reset();
            WifiStateMachine.this.mNetworkInfo.setIsAvailable(true);
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.mNetworkAgent.sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
            }
            WifiStateMachine.this.setNetworkDetailedState(DetailedState.DISCONNECTED);
            WifiStateMachine.this.mWifiConnectivityManager.setWifiEnabled(true);
            WifiStateMachine.this.mWifiMetrics.setWifiState(2);
            WifiStateMachine.this.p2pSendMessage(WifiStateMachine.CMD_ENABLE_P2P);
            WifiStateMachine.this.mSarManager.setClientWifiState(3);
        }

        public void exit() {
            WifiStateMachine.this.mOperationalMode = 4;
            WifiStateMachine.this.mNetworkInfo.setIsAvailable(false);
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.mNetworkAgent.sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
            }
            WifiStateMachine.this.mWifiConnectivityManager.setWifiEnabled(false);
            WifiStateMachine.this.mWifiMetrics.setWifiState(1);
            WifiStateMachine.this.mSarManager.setClientWifiState(1);
            if (!WifiStateMachine.this.mWifiNative.removeAllNetworks(WifiStateMachine.this.mInterfaceName)) {
                WifiStateMachine.this.loge("Failed to remove networks on exiting connect mode");
            }
            WifiStateMachine.this.mScanRequestProxy.enableScanningForHiddenNetworks(false);
            WifiStateMachine.this.mScanRequestProxy.clearScanResults();
            WifiStateMachine.this.mWifiInfo.reset();
            WifiStateMachine.this.mWifiInfo.setSupplicantState(SupplicantState.DISCONNECTED);
            WifiStateMachine.this.stopClientMode();
        }

        /* JADX WARNING: Missing block: B:170:0x05d6, code skipped:
            if (r9.equals(r10.toString()) != false) goto L_0x05d8;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(Message message) {
            Message message2 = message;
            WifiStateMachine.this.logStateAndMessage(message2, this);
            if (WifiStateMachine.this.handleWapiFailureEvent(message2, WifiStateMachine.this.mSupplicantStateTracker)) {
                return true;
            }
            int i = -1;
            int i2 = 2;
            boolean res = false;
            int netId;
            int netId2;
            WifiConfiguration config;
            boolean ok;
            WifiStateMachine wifiStateMachine;
            int i3;
            String remoteAddress;
            Set<Integer> removedNetworkIds;
            WifiConfiguration config2;
            boolean connectFromUser;
            StringBuilder stringBuilder;
            int netId3;
            WifiStateMachine wifiStateMachine2;
            StringBuilder stringBuilder2;
            int res2;
            NetworkUpdateResult networkUpdateResult;
            WifiConfiguration config3;
            WifiStateMachine wifiStateMachine3;
            StringBuilder stringBuilder3;
            String anonymousIdentity;
            String str;
            switch (message2.what) {
                case WifiStateMachine.CMD_BLUETOOTH_ADAPTER_STATE_CHANGE /*131103*/:
                    WifiStateMachine wifiStateMachine4 = WifiStateMachine.this;
                    if (message2.arg1 != 0) {
                        res = true;
                    }
                    wifiStateMachine4.mBluetoothConnectionActive = res;
                    WifiStateMachine.this.mWifiNative.setBluetoothCoexistenceScanMode(WifiStateMachine.this.mInterfaceName, WifiStateMachine.this.mBluetoothConnectionActive);
                    break;
                case WifiStateMachine.CMD_REMOVE_NETWORK /*131125*/:
                    if (!WifiStateMachine.this.deleteNetworkConfigAndSendReply(message2, false)) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        break;
                    }
                    netId = message2.arg1;
                    if (netId == WifiStateMachine.this.mTargetNetworkId || netId == WifiStateMachine.this.mLastNetworkId) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        break;
                    }
                case WifiStateMachine.CMD_ENABLE_NETWORK /*131126*/:
                    boolean disableOthers = message2.arg2 == 1;
                    netId2 = message2.arg1;
                    config = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetwork(netId2);
                    if (disableOthers) {
                        ok = WifiStateMachine.this.connectToUserSelectNetwork(netId2, message2.sendingUid, false);
                        WifiStateMachine.this.saveConnectingNetwork(config, netId2, false);
                    } else if (!WifiStateMachine.this.processConnectModeSetMode(message2)) {
                        ok = WifiStateMachine.this.mWifiConfigManager.enableNetwork(netId2, false, message2.sendingUid);
                    }
                    res = ok;
                    if (!res) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    }
                    wifiStateMachine = WifiStateMachine.this;
                    i3 = message2.what;
                    if (res) {
                        i = 1;
                    }
                    wifiStateMachine.replyToMessage(message2, i3, i);
                    break;
                case WifiStateMachine.CMD_GET_LINK_LAYER_STATS /*131135*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, (Object) WifiStateMachine.this.getWifiLinkLayerStats());
                    break;
                case WifiStateMachine.CMD_RECONNECT /*131146*/:
                    WifiStateMachine.this.mWifiConnectivityManager.forceConnectivityScan(message2.obj);
                    break;
                case WifiStateMachine.CMD_REASSOCIATE /*131147*/:
                    WifiStateMachine.this.lastConnectAttemptTimestamp = WifiStateMachine.this.mClock.getWallClockMillis();
                    WifiStateMachine.this.log("ConnectModeState, case CMD_REASSOCIATE, do reassociate");
                    WifiStateMachine.this.mWifiNative.reassociate(WifiStateMachine.this.mInterfaceName);
                    break;
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE /*131149*/:
                    if (message2.arg1 != 1) {
                        WifiStateMachine.this.setSuspendOptimizationsNative(2, true);
                        break;
                    }
                    WifiStateMachine.this.setSuspendOptimizationsNative(2, false);
                    break;
                case WifiStateMachine.CMD_SET_SUSPEND_OPT_ENABLED /*131158*/:
                    if (message2.arg1 != 1) {
                        WifiStateMachine.this.setSuspendOptimizationsNative(4, false);
                        break;
                    }
                    WifiStateMachine.this.setSuspendOptimizationsNative(4, true);
                    if (message2.arg2 == 1) {
                        WifiStateMachine.this.mSuspendWakeLock.release();
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_ENABLE_TDLS /*131164*/:
                    if (message2.obj != null) {
                        remoteAddress = message2.obj;
                        if (message2.arg1 == 1) {
                            res = true;
                        }
                        WifiStateMachine.this.mWifiNative.startTdls(WifiStateMachine.this.mInterfaceName, remoteAddress, res);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_REMOVE_APP_CONFIGURATIONS /*131169*/:
                    removedNetworkIds = WifiStateMachine.this.mWifiConfigManager.removeNetworksForApp((ApplicationInfo) message2.obj);
                    if (removedNetworkIds.contains(Integer.valueOf(WifiStateMachine.this.mTargetNetworkId)) || removedNetworkIds.contains(Integer.valueOf(WifiStateMachine.this.mLastNetworkId))) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        break;
                    }
                case WifiStateMachine.CMD_DISABLE_EPHEMERAL_NETWORK /*131170*/:
                    config2 = WifiStateMachine.this.mWifiConfigManager.disableEphemeralNetwork((String) message2.obj);
                    if (config2 != null && (config2.networkId == WifiStateMachine.this.mTargetNetworkId || config2.networkId == WifiStateMachine.this.mLastNetworkId)) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        break;
                    }
                case WifiStateMachine.CMD_GET_MATCHING_CONFIG /*131171*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, (Object) WifiStateMachine.this.mPasspointManager.getMatchingWifiConfig((ScanResult) message2.obj));
                    break;
                case WifiStateMachine.CMD_RESET_SIM_NETWORKS /*131173*/:
                    if (message2.arg1 == 1) {
                        Log.d(WifiStateMachine.TAG, "enable EAP-SIM/AKA/AKA' networks since SIM was loaded");
                        WifiStateMachine.this.mWifiConfigManager.enableSimNetworks();
                    }
                    WifiStateMachine.this.log("resetting EAP-SIM/AKA/AKA' networks since SIM was changed");
                    WifiConfigManager access$800 = WifiStateMachine.this.mWifiConfigManager;
                    if (message2.arg1 == 1) {
                        res = true;
                    }
                    access$800.resetSimNetworks(res);
                    break;
                case WifiStateMachine.CMD_QUERY_OSU_ICON /*131176*/:
                    WifiStateMachine.this.mPasspointManager.queryPasspointIcon(((Bundle) message2.obj).getLong("BSSID"), ((Bundle) message2.obj).getString(WifiStateMachine.EXTRA_OSU_ICON_QUERY_FILENAME));
                    break;
                case WifiStateMachine.CMD_MATCH_PROVIDER_NETWORK /*131177*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, 0);
                    break;
                case WifiStateMachine.CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG /*131178*/:
                    PasspointConfiguration passpointConfig = message2.obj;
                    if (!WifiStateMachine.this.mPasspointManager.addOrUpdateProvider(passpointConfig, message2.arg1)) {
                        WifiStateMachine.this.replyToMessage(message2, message2.what, -1);
                        break;
                    }
                    String fqdn = passpointConfig.getHomeSp().getFqdn();
                    if (WifiStateMachine.this.isProviderOwnedNetwork(WifiStateMachine.this.mTargetNetworkId, fqdn) || WifiStateMachine.this.isProviderOwnedNetwork(WifiStateMachine.this.mLastNetworkId, fqdn)) {
                        WifiStateMachine.this.logd("Disconnect from current network since its provider is updated");
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    }
                    WifiStateMachine.this.replyToMessage(message2, message2.what, 1);
                    break;
                case WifiStateMachine.CMD_REMOVE_PASSPOINT_CONFIG /*131179*/:
                    remoteAddress = message2.obj;
                    if (!WifiStateMachine.this.mPasspointManager.removeProvider(remoteAddress)) {
                        WifiStateMachine.this.replyToMessage(message2, message2.what, -1);
                        break;
                    }
                    if (WifiStateMachine.this.isProviderOwnedNetwork(WifiStateMachine.this.mTargetNetworkId, remoteAddress) || WifiStateMachine.this.isProviderOwnedNetwork(WifiStateMachine.this.mLastNetworkId, remoteAddress)) {
                        WifiStateMachine.this.logd("Disconnect from current network since its provider is removed");
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                    }
                    WifiStateMachine.this.replyToMessage(message2, message2.what, 1);
                    break;
                case WifiStateMachine.CMD_GET_MATCHING_OSU_PROVIDERS /*131181*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, (Object) WifiStateMachine.this.mPasspointManager.getMatchingOsuProviders((ScanResult) message2.obj));
                    break;
                case WifiStateMachine.CMD_ENABLE_P2P /*131203*/:
                    WifiStateMachine.this.p2pSendMessage(WifiStateMachine.CMD_ENABLE_P2P);
                    break;
                case WifiStateMachine.CMD_TARGET_BSSID /*131213*/:
                    if (message2.obj != null) {
                        WifiStateMachine.this.mTargetRoamBSSID = (String) message2.obj;
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_RELOAD_TLS_AND_RECONNECT /*131214*/:
                    config2 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if ((config2 == null || config2.cloudSecurityCheck == 0) && (config2 == null || (config2.allowedKeyManagement.get(2) && config2.allowedKeyManagement.get(3)))) {
                        WifiStateMachine.this.log("currentWifiConfiguration is EAP type or no currentWifiConfiguration");
                        if (WifiStateMachine.this.mWifiConfigManager.needsUnlockedKeyStore() && !WifiStateMachine.this.isConnected()) {
                            WifiStateMachine.this.logd("Reconnecting to give a chance to un-connected TLS networks");
                            WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                            WifiStateMachine.this.lastConnectAttemptTimestamp = WifiStateMachine.this.mClock.getWallClockMillis();
                            WifiStateMachine.this.mWifiNative.reconnect(WifiStateMachine.this.mInterfaceName);
                            break;
                        }
                    }
                case WifiStateMachine.CMD_START_CONNECT /*131215*/:
                    if (!WifiStateMachine.this.isHiLinkActive()) {
                        Bundle bundle = message2.obj;
                        connectFromUser = bundle.getBoolean(WifiStateMachine.CONNECT_FROM_USER);
                        remoteAddress = WifiStateMachine.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("connectFromUser =");
                        stringBuilder.append(connectFromUser);
                        Log.d(remoteAddress, stringBuilder.toString());
                        if (!WifiStateMachine.this.attemptAutoConnect() && !connectFromUser) {
                            WifiStateMachine.this.logd("SupplicantState is TransientState, refuse auto connect");
                            break;
                        }
                        netId3 = message2.arg1;
                        i3 = message2.arg2;
                        String bssid = bundle.getString(WifiStateMachine.BSSID_TO_CONNECT);
                        synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                            if (!WifiStateMachine.this.hasConnectionRequests()) {
                                if (WifiStateMachine.this.mNetworkAgent != null) {
                                    if (!(connectFromUser || WifiStateMachine.this.mWifiPermissionsUtil.checkNetworkSettingsPermission(i3))) {
                                        WifiStateMachine.this.loge("CMD_START_CONNECT but no requests and connected, but app does not have sufficient permissions, bailing");
                                        break;
                                    }
                                }
                                WifiStateMachine.this.loge("CMD_START_CONNECT but no requests and not connected, bailing");
                                break;
                            }
                            config2 = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetworkWithoutMasking(netId3);
                            wifiStateMachine2 = WifiStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("CMD_START_CONNECT sup state ");
                            stringBuilder2.append(WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName());
                            stringBuilder2.append(" my state ");
                            stringBuilder2.append(WifiStateMachine.this.getCurrentState().getName());
                            stringBuilder2.append(" nid=");
                            stringBuilder2.append(Integer.toString(netId3));
                            stringBuilder2.append(" roam=");
                            stringBuilder2.append(Boolean.toString(WifiStateMachine.this.mIsAutoRoaming));
                            wifiStateMachine2.logd(stringBuilder2.toString());
                            if (config2 != null) {
                                if (!(!connectFromUser || bssid == null || !bssid.equals("any") || config2.BSSID == null || isApInScanList(config2.BSSID))) {
                                    WifiStateMachine.this.logd("bssid not match, connect with ssid");
                                    config2.BSSID = null;
                                }
                                if (!WifiCommonUtils.doesNotWifiConnectRejectByCust(config2.getNetworkSelectionStatus(), config2.SSID, WifiStateMachine.this.mContext)) {
                                    if (!HwWifiServiceFactory.getHwWifiDevicePolicy().isWifiRestricted(config2, false)) {
                                        if (!WifiStateMachine.this.mWifiConfigManager.isSimPresent()) {
                                            WifiStateMachine.this.handleSimAbsent(config2);
                                        }
                                        if (WifiStateMachine.this.isEnterpriseHotspot(config2)) {
                                            wifiStateMachine2 = WifiStateMachine.this;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append(config2.SSID);
                                            stringBuilder2.append("is enterprise hotspot ");
                                            wifiStateMachine2.logd(stringBuilder2.toString());
                                            WifiStateMachine.this.mTargetRoamBSSID = "any";
                                        }
                                        WifiStateMachine.this.mTargetNetworkId = netId3;
                                        WifiStateMachine.this.setTargetBssid(config2, bssid);
                                        if (WifiStateMachine.this.mEnableConnectedMacRandomization.get()) {
                                            WifiStateMachine.this.configureRandomizedMacAddress(config2);
                                        }
                                        String currentMacAddress = WifiStateMachine.this.mWifiNative.getMacAddress(WifiStateMachine.this.mInterfaceName);
                                        WifiStateMachine.this.mWifiInfo.setMacAddress(currentMacAddress);
                                        String str2 = WifiStateMachine.TAG;
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Connecting with ");
                                        stringBuilder4.append(currentMacAddress);
                                        stringBuilder4.append(" as the mac address");
                                        Log.i(str2, stringBuilder4.toString());
                                        WifiStateMachine.this.reportConnectionAttemptStart(config2, WifiStateMachine.this.mTargetRoamBSSID, 5);
                                        WifiStateMachine.this.saveConnectingNetwork(config2, netId3, true);
                                        if (!WifiStateMachine.this.mWifiNative.connectToNetwork(WifiStateMachine.this.mInterfaceName, config2)) {
                                            WifiStateMachine wifiStateMachine5 = WifiStateMachine.this;
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("CMD_START_CONNECT Failed to start connection to network ");
                                            stringBuilder4.append(config2);
                                            wifiStateMachine5.loge(stringBuilder4.toString());
                                            WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                                            WifiStateMachine.this.replyToMessage(message2, 151554, 0);
                                            break;
                                        }
                                        WifiStateMachine.this.mWifiMetrics.logStaEvent(11, config2);
                                        WifiStateMachine.this.lastConnectAttemptTimestamp = WifiStateMachine.this.mClock.getWallClockMillis();
                                        WifiStateMachine.this.targetWificonfiguration = config2;
                                        WifiStateMachine.this.mIsAutoRoaming = false;
                                        if (WifiStateMachine.this.getCurrentState() != WifiStateMachine.this.mDisconnectedState) {
                                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                                            break;
                                        }
                                    }
                                    Log.w(WifiStateMachine.TAG, "CMD_START_CONNECT: MDM deny connect to restricted network!");
                                    break;
                                }
                                Log.d(WifiStateMachine.TAG, "break CMD_START_CONNECT with WifiConnectRejectByCust");
                                break;
                            }
                            WifiStateMachine.this.loge("CMD_START_CONNECT and no config, bail out...");
                            break;
                        }
                    }
                    Log.d(WifiStateMachine.TAG, "HiLink is active, refuse auto connect");
                    break;
                    break;
                case WifiStateMachine.CMD_START_ROAM /*131217*/:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID /*131219*/:
                    remoteAddress = message2.obj;
                    if (remoteAddress != null) {
                        ScanDetailCache scanDetailCache = WifiStateMachine.this.mWifiConfigManager.getScanDetailCacheForNetwork(WifiStateMachine.this.mTargetNetworkId);
                        if (scanDetailCache != null) {
                            WifiStateMachine.this.mWifiMetrics.setConnectionScanDetail(scanDetailCache.getScanDetail(remoteAddress));
                        }
                    }
                    return false;
                case WifiStateMachine.CMD_REMOVE_USER_CONFIGURATIONS /*131224*/:
                    removedNetworkIds = WifiStateMachine.this.mWifiConfigManager.removeNetworksForUser(Integer.valueOf(message2.arg1).intValue());
                    if (removedNetworkIds.contains(Integer.valueOf(WifiStateMachine.this.mTargetNetworkId)) || removedNetworkIds.contains(Integer.valueOf(WifiStateMachine.this.mLastNetworkId))) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        break;
                    }
                case WifiStateMachine.CMD_STOP_IP_PACKET_OFFLOAD /*131233*/:
                    netId = message2.arg1;
                    netId2 = WifiStateMachine.this.stopWifiIPPacketOffload(netId);
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(netId, netId2);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER /*131238*/:
                    WifiConnectivityManager wifiConnectivityManager = WifiStateMachine.this.mWifiConnectivityManager;
                    if (message2.arg1 == 1) {
                        res = true;
                    }
                    wifiConnectivityManager.enable(res);
                    break;
                case WifiStateMachine.CMD_GET_ALL_MATCHING_CONFIGS /*131240*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, (Object) WifiStateMachine.this.mPasspointManager.getAllMatchingWifiConfigs((ScanResult) message2.obj));
                    break;
                case WifiStateMachine.CMD_CONFIG_ND_OFFLOAD /*131276*/:
                    if (message2.arg1 > 0) {
                        res = true;
                    }
                    WifiStateMachine.this.mWifiNative.configureNeighborDiscoveryOffload(WifiStateMachine.this.mInterfaceName, res);
                    break;
                case WifiStateMachine.CMD_START_SUBSCRIPTION_PROVISIONING /*131326*/:
                    if (WifiStateMachine.this.mPasspointManager.startSubscriptionProvisioning(message2.arg1, (OsuProvider) message.getData().getParcelable(WifiStateMachine.EXTRA_OSU_PROVIDER), message2.obj)) {
                        res2 = 1;
                    }
                    WifiStateMachine.this.replyToMessage(message2, message2.what, res2);
                    break;
                case WifiStateMachine.CMD_UPDATE_WIFIPRO_CONFIGURATIONS /*131672*/:
                    WifiStateMachine.this.updateWifiproWifiConfiguration(message2);
                    break;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST /*143372*/:
                    if (message2.arg1 != 1) {
                        WifiStateMachine.this.log("ConnectModeState, case WifiP2pService.DISCONNECT_WIFI_REQUEST, do reconnect");
                        WifiStateMachine.this.mWifiNative.reconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.mTemporarilyDisconnectWifi = false;
                        break;
                    }
                    WifiStateMachine.this.log("ConnectModeState, case WifiP2pService.DISCONNECT_WIFI_REQUEST, do disconnect");
                    WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 5);
                    WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                    WifiStateMachine.this.mTemporarilyDisconnectWifi = true;
                    break;
                case WifiMonitor.NETWORK_CONNECTION_EVENT /*147459*/:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("Network connection established");
                    }
                    WifiStateMachine.this.mLastNetworkId = message2.arg1;
                    WifiStateMachine.this.mWifiConfigManager.clearRecentFailureReason(WifiStateMachine.this.mLastNetworkId);
                    if (WifiStateMachine.this.mHwWifiCHRService != null) {
                        WifiStateMachine.this.mHwWifiCHRService.updateWIFIConfiguraionByConfig(WifiStateMachine.this.getCurrentWifiConfiguration());
                    }
                    WifiStateMachine.this.mLastBssid = (String) message2.obj;
                    if (WifiStateMachine.this.mLastNetworkId == -1) {
                        networkUpdateResult = WifiStateMachine.this.saveWpsOkcConfiguration(WifiStateMachine.this.mLastNetworkId, WifiStateMachine.this.mLastBssid);
                        if (networkUpdateResult != null) {
                            WifiStateMachine.this.mLastNetworkId = networkUpdateResult.getNetworkId();
                        }
                    }
                    netId = message2.arg2;
                    config3 = WifiStateMachine.this.getCurrentWifiConfiguration();
                    WifiStateMachine.this.setLastConnectConfig(config3);
                    if (config3 == null) {
                        wifiStateMachine3 = WifiStateMachine.this;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Connected to unknown networkId ");
                        stringBuilder5.append(WifiStateMachine.this.mLastNetworkId);
                        stringBuilder5.append(", disconnecting...");
                        wifiStateMachine3.logw(stringBuilder5.toString());
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        break;
                    }
                    WifiStateMachine.this.mWifiInfo.setBSSID(WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                    WifiStateMachine.this.mWifiInfo.setMacAddress(WifiStateMachine.this.mWifiNative.getMacAddress(WifiStateMachine.this.mInterfaceName));
                    ScanDetailCache scanDetailCache2 = WifiStateMachine.this.mWifiConfigManager.getScanDetailCacheForNetwork(config3.networkId);
                    if (!(scanDetailCache2 == null || WifiStateMachine.this.mLastBssid == null)) {
                        ScanResult scanResult = scanDetailCache2.getScanResult(WifiStateMachine.this.mLastBssid);
                        if (scanResult != null) {
                            WifiStateMachine.this.mWifiInfo.setFrequency(scanResult.frequency);
                        }
                    }
                    WifiStateMachine.this.mWifiConnectivityManager.trackBssid(WifiStateMachine.this.mLastBssid, true, netId);
                    DataUploader access$9100 = WifiStateMachine.this.uploader;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("{RT:6,SPEED:");
                    stringBuilder3.append(WifiStateMachine.this.mWifiInfo.getLinkSpeed());
                    stringBuilder3.append("}");
                    access$9100.e(54, stringBuilder3.toString());
                    if (!(config3.isTempCreated || config3.enterpriseConfig == null || !TelephonyUtil.isSimEapMethod(config3.enterpriseConfig.getEapMethod()))) {
                        anonymousIdentity = WifiStateMachine.this.mWifiNative.getEapAnonymousIdentity(WifiStateMachine.this.mInterfaceName);
                        if (anonymousIdentity != null) {
                            config3.enterpriseConfig.setAnonymousIdentity(anonymousIdentity);
                        } else {
                            Log.d(WifiStateMachine.TAG, "Failed to get updated anonymous identity from supplicant, reset it in WifiConfiguration.");
                            config3.enterpriseConfig.setAnonymousIdentity(null);
                        }
                        WifiStateMachine.this.mWifiConfigManager.addOrUpdateNetwork(config3, 1010);
                    }
                    WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.log("ConnectModeState, case WifiMonitor.NETWORK_CONNECTION_EVENT, go to mObtainingIpState");
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("ConnectModeState: Network connection lost ");
                    }
                    if (WifiStateMachine.this.disassociatedReason(message2.arg2)) {
                        remoteAddress = WifiStateMachine.TAG;
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("DISABLED_DISASSOC_REASON for network ");
                        stringBuilder6.append(WifiStateMachine.this.mTargetNetworkId);
                        stringBuilder6.append(" is ");
                        stringBuilder6.append(message2.arg2);
                        Log.d(remoteAddress, stringBuilder6.toString());
                        WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.mTargetNetworkId, 16);
                    }
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT /*147462*/:
                    SupplicantState state = WifiStateMachine.this.handleSupplicantStateChange(message2);
                    if (state != SupplicantState.INTERFACE_DISABLED) {
                        if (state == SupplicantState.DISCONNECTED && WifiStateMachine.this.mNetworkInfo.getState() != NetworkInfo.State.DISCONNECTED) {
                            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                                WifiStateMachine.this.log("Missed CTRL-EVENT-DISCONNECTED, disconnect");
                            }
                            WifiStateMachine.this.handleNetworkDisconnect();
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                        }
                        if (state == SupplicantState.COMPLETED) {
                            WifiStateMachine.this.mIpClient.confirmConfiguration();
                            WifiStateMachine.this.mWifiScoreReport.noteIpCheck();
                        }
                        StateChangeResult stateChangeResult = message2.obj;
                        if (stateChangeResult != null) {
                            res2 = stateChangeResult.networkId;
                            config = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetwork(res2);
                            if (config != null && config.getNetworkSelectionStatus().isNetworkEnabled() && config.getNetworkSelectionStatus().getDisableReasonCounter(3) > 0 && WifiStateMachine.this.mClock.getElapsedSinceBootMillis() - WifiStateMachine.this.mLastAuthFailureTimestamp < WifiStateMachine.LAST_AUTH_FAILURE_GAP && !WifiStateMachine.this.isConnected() && state == SupplicantState.DISCONNECTED) {
                                str = WifiStateMachine.TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("start an immediate connection for network ");
                                stringBuilder3.append(res2);
                                Log.d(str, stringBuilder3.toString());
                                WifiStateMachine.this.startConnectToNetwork(res2, 1010, "any");
                                break;
                            }
                        }
                    }
                    return false;
                    break;
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT /*147463*/:
                    WifiStateMachine.this.mWifiDiagnostics.captureBugReportData(2);
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.AUTHENTICATION_FAILURE_EVENT);
                    netId = 3;
                    netId2 = message2.arg1;
                    if (WifiStateMachine.this.isPermanentWrongPasswordFailure(WifiStateMachine.this.mTargetNetworkId, netId2)) {
                        netId = 13;
                    } else if (netId2 == 3) {
                        WifiStateMachine.this.handleEapAuthFailure(WifiStateMachine.this.mTargetNetworkId, message2.arg2);
                    }
                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.mTargetNetworkId, netId);
                    WifiStateMachine.this.mWifiConfigManager.clearRecentFailureReason(WifiStateMachine.this.mTargetNetworkId);
                    WifiStateMachine.this.mLastAuthFailureTimestamp = WifiStateMachine.this.mClock.getElapsedSinceBootMillis();
                    WifiStateMachine.this.notifyWifiConnFailedInfo(WifiStateMachine.this.mTargetNetworkId, null, WifiServiceHisiExt.MIN_RSSI, 3, WifiStateMachine.this.mWifiConnectivityManager);
                    WifiStateMachine.this.reportConnectionAttemptEnd(3, 1);
                    if (WifiStateMachine.this.mCust != null && WifiStateMachine.this.mCust.isShowWifiAuthenticationFailurerNotification()) {
                        WifiStateMachine.this.mCust.handleWifiAuthenticationFailureEvent(WifiStateMachine.this.mContext, WifiStateMachine.this);
                    }
                    WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), WifiStateMachine.this.mTargetRoamBSSID, 2);
                    break;
                case WifiMonitor.SUP_REQUEST_IDENTITY /*147471*/:
                    netId = message2.arg2;
                    connectFromUser = false;
                    if (WifiStateMachine.this.targetWificonfiguration != null && WifiStateMachine.this.targetWificonfiguration.networkId == netId && TelephonyUtil.isSimConfig(WifiStateMachine.this.targetWificonfiguration)) {
                        Pair<String, String> identityPair = TelephonyUtil.getSimIdentity(WifiStateMachine.this.getTelephonyManager(), new TelephonyUtil(), WifiStateMachine.this.targetWificonfiguration);
                        if (identityPair == null || identityPair.first == null) {
                            Log.e(WifiStateMachine.TAG, "Unable to retrieve identity from Telephony");
                        } else {
                            connectFromUser = WifiStateMachine.this.mWifiNative.simIdentityResponse(WifiStateMachine.this.mInterfaceName, netId, (String) identityPair.first, (String) identityPair.second);
                        }
                    }
                    if (!connectFromUser) {
                        str = message2.obj;
                        if (!(WifiStateMachine.this.targetWificonfiguration == null || str == null || WifiStateMachine.this.targetWificonfiguration.SSID == null)) {
                            if (!WifiStateMachine.this.targetWificonfiguration.SSID.equals(str)) {
                                String str3 = WifiStateMachine.this.targetWificonfiguration.SSID;
                                StringBuilder stringBuilder7 = new StringBuilder();
                                stringBuilder7.append("\"");
                                stringBuilder7.append(str);
                                stringBuilder7.append("\"");
                                break;
                            }
                            if (WifiStateMachine.this.mTrackEapAuthFailCount >= 3) {
                                Log.d(WifiStateMachine.TAG, "updateNetworkSelectionStatus(DISABLED_AUTHENTICATION_NO_CREDENTIALS)");
                                WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.targetWificonfiguration.networkId, 9);
                                WifiStateMachine.this.mTrackEapAuthFailCount = 0;
                            } else if (WifiStateMachine.this.mIsImsiAvailable) {
                                WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.targetWificonfiguration.networkId, 0);
                                WifiStateMachine.this.mTrackEapAuthFailCount = WifiStateMachine.this.mTrackEapAuthFailCount + 1;
                                String str4 = WifiStateMachine.TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("sim is not ready and retry mTrackEapAuthFailCount ");
                                stringBuilder3.append(WifiStateMachine.this.mTrackEapAuthFailCount);
                                Log.d(str4, stringBuilder3.toString());
                            } else {
                                Log.d(WifiStateMachine.TAG, "sim is not available,updateNetworkSelectionStatus(DISABLED_AUTHENTICATION_NO_CREDENTIALS)");
                                WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.targetWificonfiguration.networkId, 9);
                                WifiStateMachine.this.mTrackEapAuthFailCount = 0;
                            }
                        }
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 2);
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        break;
                    }
                    break;
                case WifiMonitor.SUP_REQUEST_SIM_AUTH /*147472*/:
                    WifiStateMachine.this.logd("Received SUP_REQUEST_SIM_AUTH");
                    SimAuthRequestData requestData = message2.obj;
                    if (requestData != null) {
                        if (requestData.protocol != 4) {
                            if (requestData.protocol == 5 || requestData.protocol == 6) {
                                WifiStateMachine.this.handle3GAuthRequest(requestData);
                                break;
                            }
                        }
                        WifiStateMachine.this.handleGsmAuthRequest(requestData);
                        break;
                    }
                    WifiStateMachine.this.loge("Invalid sim auth request");
                    break;
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT /*147499*/:
                    WifiStateMachine.this.mWifiDiagnostics.captureBugReportData(1);
                    WifiStateMachine.this.didBlackListBSSID = false;
                    remoteAddress = message2.obj;
                    boolean timedOut = message2.arg1 > 0;
                    netId3 = message2.arg2;
                    String str5 = WifiStateMachine.TAG;
                    StringBuilder stringBuilder8 = new StringBuilder();
                    stringBuilder8.append("Assocation Rejection event: bssid=");
                    stringBuilder8.append(remoteAddress);
                    stringBuilder8.append(" reason code=");
                    stringBuilder8.append(netId3);
                    stringBuilder8.append(" timedOut=");
                    stringBuilder8.append(Boolean.toString(timedOut));
                    Log.d(str5, stringBuilder8.toString());
                    if (remoteAddress == null || TextUtils.isEmpty(remoteAddress)) {
                        remoteAddress = WifiStateMachine.this.mTargetRoamBSSID;
                    }
                    if (remoteAddress != null) {
                        WifiStateMachine.this.didBlackListBSSID = WifiStateMachine.this.mWifiConnectivityManager.trackBssid(remoteAddress, false, netId3);
                    }
                    WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(WifiStateMachine.this.mTargetNetworkId, 2);
                    WifiStateMachine.this.mWifiConfigManager.setRecentFailureAssociationStatus(WifiStateMachine.this.mTargetNetworkId, netId3);
                    WifiStateMachine.this.notifyWifiConnFailedInfo(WifiStateMachine.this.mTargetNetworkId, remoteAddress, WifiServiceHisiExt.MIN_RSSI, 2, WifiStateMachine.this.mWifiConnectivityManager);
                    WifiStateMachine.this.recordAssociationRejectStatusCode(message2.arg2);
                    WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.ASSOCIATION_REJECTION_EVENT);
                    wifiStateMachine3 = WifiStateMachine.this;
                    if (timedOut) {
                        i2 = 11;
                    }
                    wifiStateMachine3.reportConnectionAttemptEnd(i2, 1);
                    WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), remoteAddress, 1);
                    break;
                case WifiMonitor.ANQP_DONE_EVENT /*147500*/:
                    WifiStateMachine.this.mPasspointManager.notifyANQPDone((AnqpEvent) message2.obj);
                    break;
                case WifiMonitor.AUTHENTICATION_TIMEOUT_EVENT /*147501*/:
                    if (WifiStateMachine.this.mWifiInfo != null && WifiStateMachine.this.mWifiInfo.getSupplicantState() == SupplicantState.ASSOCIATED) {
                        WifiStateMachine.this.loge("auth timeout in associated state, handle as associate reject event");
                        WifiStateMachine.this.mSupplicantStateTracker.sendMessage(WifiMonitor.ASSOCIATION_REJECTION_EVENT);
                        break;
                    }
                case WifiMonitor.RX_HS20_ANQP_ICON_EVENT /*147509*/:
                    WifiStateMachine.this.mPasspointManager.notifyIconDone((IconEvent) message2.obj);
                    break;
                case WifiMonitor.HS20_REMEDIATION_EVENT /*147517*/:
                    WifiStateMachine.this.mPasspointManager.receivedWnmFrame((WnmData) message2.obj);
                    break;
                case WifiMonitor.WPS_START_OKC_EVENT /*147656*/:
                    WifiStateMachine.this.sendWpsOkcStartedBroadcast();
                    if (!WifiStateMachine.this.mWifiNative.removeAllNetworks(WifiStateMachine.this.mInterfaceName)) {
                        WifiStateMachine.this.loge("Failed to remove networks before HiLink OKC");
                    }
                    remoteAddress = message2.obj;
                    if (!TextUtils.isEmpty(remoteAddress)) {
                        WifiStateMachine.this.mWifiNative.startWpsPbc(WifiStateMachine.this.mInterfaceName, remoteAddress);
                        break;
                    }
                    break;
                case WifiMonitor.EAP_ERRORCODE_REPORT_EVENT /*147956*/:
                    if (WifiStateMachine.this.targetWificonfiguration != null && WifiStateMachine.this.targetWificonfiguration.networkId == message2.arg1) {
                        WifiStateMachine.this.handleEapErrorcodeReport(message2.arg1, (String) message2.obj, message2.arg2);
                        break;
                    }
                case 151553:
                    netId = message2.arg1;
                    config3 = message2.obj;
                    anonymousIdentity = "*";
                    ok = false;
                    boolean forceReconnect = false;
                    if (config3 != null) {
                        anonymousIdentity = config3.preSharedKey;
                        if (WifiStateMachine.this.mNetworkInfo != null && WifiStateMachine.this.mNetworkInfo.isConnectedOrConnecting() && config3.isTempCreated && WifiStateMachine.this.isWifiProEvaluatingAP()) {
                            WifiStateMachine.this.logd("CONNECT_NETWORK user connect network, stop background evaluating and force reconnect");
                            config3.isTempCreated = false;
                            forceReconnect = true;
                        }
                        NetworkUpdateResult result = WifiStateMachine.this.mWifiConfigManager.addOrUpdateNetwork(config3, message2.sendingUid);
                        if (!result.isSuccess()) {
                            WifiStateMachine wifiStateMachine6 = WifiStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("CONNECT_NETWORK adding/updating config=");
                            stringBuilder2.append(config3);
                            stringBuilder2.append(" failed");
                            wifiStateMachine6.loge(stringBuilder2.toString());
                            WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                            WifiStateMachine.this.replyToMessage(message2, 151554, 0);
                            break;
                        }
                        netId = result.getNetworkId();
                        ok = result.hasCredentialChanged();
                    }
                    if (WifiStateMachine.this.mHwWifiCHRService != null) {
                        WifiConfiguration newWifiConfig = new WifiConfiguration(config3);
                        newWifiConfig.preSharedKey = anonymousIdentity;
                        WifiStateMachine.this.mHwWifiCHRService.connectFromUserByConfig(newWifiConfig);
                    }
                    WifiStateMachine.this.saveConnectingNetwork(config3, netId, false);
                    WifiStateMachine.this.exitWifiSelfCure(151553, -1);
                    if (!(WifiStateMachine.this.mLastNetworkId == -1 || WifiStateMachine.this.mLastNetworkId != netId || WifiStateMachine.this.getCurrentState() == WifiStateMachine.this.mDisconnectedState)) {
                        WifiStateMachine.this.logd("disconnect old");
                        forceReconnect = true;
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    }
                    wifiStateMachine2 = WifiStateMachine.this;
                    i2 = message2.sendingUid;
                    boolean z = ok || forceReconnect;
                    if (!wifiStateMachine2.connectToUserSelectNetwork(netId, i2, z)) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        if (-1 != netId && config3 == null) {
                            config3 = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetwork(netId);
                        }
                        if (config3 != null && HwWifiServiceFactory.getHwWifiDevicePolicy().isWifiRestricted(config3, false)) {
                            WifiStateMachine.this.replyToMessage(message2, 151554, 1000);
                            break;
                        }
                        WifiStateMachine.this.replyToMessage(message2, 151554, 9);
                        break;
                    }
                    WifiStateMachine.this.mWifiMetrics.logStaEvent(13, config3);
                    WifiStateMachine.this.broadcastWifiCredentialChanged(0, config3);
                    WifiStateMachine.this.replyToMessage(message2, 151555);
                    break;
                    break;
                case 151556:
                    if (WifiStateMachine.this.deleteNetworkConfigAndSendReply(message2, true)) {
                        netId = message2.arg1;
                        WifiStateMachine.this.exitWifiSelfCure(151556, netId);
                        if (netId == WifiStateMachine.this.mTargetNetworkId || netId == WifiStateMachine.this.mLastNetworkId) {
                            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                            break;
                        }
                    }
                    break;
                case 151559:
                    networkUpdateResult = WifiStateMachine.this.saveNetworkConfigAndSendReply(message2);
                    netId2 = networkUpdateResult.getNetworkId();
                    if (networkUpdateResult.isSuccess() && WifiStateMachine.this.mWifiInfo.getNetworkId() == netId2) {
                        if (!networkUpdateResult.hasCredentialChanged()) {
                            if (networkUpdateResult.hasProxyChanged()) {
                                WifiStateMachine.this.log("Reconfiguring proxy on connection");
                                WifiStateMachine.this.mIpClient.setHttpProxy(WifiStateMachine.this.getProxyProperties());
                            }
                            if (networkUpdateResult.hasIpChanged()) {
                                Log.d(WifiStateMachine.TAG, "Reconfiguring IP if current state == mConnectedState");
                                if (!WifiStateMachine.this.isConnected()) {
                                    Log.d(WifiStateMachine.TAG, "ignore reconfiguring IP because current state != mConnectedState");
                                    break;
                                }
                                WifiStateMachine.this.log("Reconfiguring IP on connection");
                                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                                break;
                            }
                        }
                        WifiConfiguration config4 = message2.obj;
                        WifiStateMachine wifiStateMachine7 = WifiStateMachine.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("SAVE_NETWORK credential changed for config=");
                        stringBuilder.append(config4.configKey());
                        stringBuilder.append(", Reconnecting.");
                        wifiStateMachine7.logi(stringBuilder.toString());
                        WifiStateMachine.this.startConnectToNetwork(netId2, message2.sendingUid, "any");
                        break;
                    }
                    break;
                case 151562:
                    WpsInfo wpsInfo = message2.obj;
                    if (wpsInfo != null) {
                        WpsResult wpsResult = new WpsResult();
                        if (!WifiStateMachine.this.mWifiNative.removeAllNetworks(WifiStateMachine.this.mInterfaceName)) {
                            WifiStateMachine.this.loge("Failed to remove networks before WPS");
                        }
                        switch (wpsInfo.setup) {
                            case 0:
                                WifiStateMachine.this.clearRandomMacOui();
                                WifiStateMachine.this.mIsRandomMacCleared = true;
                                if (!WifiStateMachine.this.mWifiNative.startWpsPbc(WifiStateMachine.this.mInterfaceName, wpsInfo.BSSID)) {
                                    Log.e(WifiStateMachine.TAG, "Failed to start WPS push button configuration");
                                    wpsResult.status = Status.FAILURE;
                                    break;
                                }
                                wpsResult.status = Status.SUCCESS;
                                break;
                            case 1:
                                wpsResult.pin = WifiStateMachine.this.mWifiNative.startWpsPinDisplay(WifiStateMachine.this.mInterfaceName, wpsInfo.BSSID);
                                if (!TextUtils.isEmpty(wpsResult.pin)) {
                                    wpsResult.status = Status.SUCCESS;
                                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_WPS_PIN_RETRY, wpsResult);
                                    break;
                                }
                                Log.e(WifiStateMachine.TAG, "Failed to start WPS pin method configuration");
                                wpsResult.status = Status.FAILURE;
                                break;
                            case 2:
                                if (!WifiStateMachine.this.mWifiNative.startWpsRegistrar(WifiStateMachine.this.mInterfaceName, wpsInfo.BSSID, wpsInfo.pin)) {
                                    Log.e(WifiStateMachine.TAG, "Failed to start WPS push button configuration");
                                    wpsResult.status = Status.FAILURE;
                                    break;
                                }
                                wpsResult.status = Status.SUCCESS;
                                break;
                            default:
                                wpsResult = new WpsResult(Status.FAILURE);
                                WifiStateMachine.this.loge("Invalid setup for WPS");
                                break;
                        }
                        if (wpsResult.status != Status.SUCCESS) {
                            wifiStateMachine = WifiStateMachine.this;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Failed to start WPS with config ");
                            stringBuilder3.append(wpsInfo.toString());
                            wifiStateMachine.loge(stringBuilder3.toString());
                            WifiStateMachine.this.replyToMessage(message2, 151564, 0);
                            break;
                        }
                        WifiStateMachine.this.replyToMessage(message2, 151563, (Object) wpsResult);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mWpsRunningState);
                        break;
                    }
                    WifiStateMachine.this.loge("Cannot start WPS with null WpsInfo object");
                    WifiStateMachine.this.replyToMessage(message2, 151564, 0);
                    break;
                case 151569:
                    netId = message2.arg1;
                    if (!WifiStateMachine.this.mWifiConfigManager.disableNetwork(netId, message2.sendingUid)) {
                        WifiStateMachine.this.loge("Failed to disable network");
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                        WifiStateMachine.this.replyToMessage(message2, 151570, 0);
                        break;
                    }
                    WifiStateMachine.this.replyToMessage(message2, 151571);
                    if (netId == WifiStateMachine.this.mTargetNetworkId || netId == WifiStateMachine.this.mLastNetworkId) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        break;
                    }
                default:
                    return false;
            }
            return true;
        }

        private boolean isApInScanList(String bssid) {
            ScanRequestProxy scanProxy = WifiInjector.getInstance().getScanRequestProxy();
            if (!(scanProxy == null || bssid == null)) {
                synchronized (scanProxy) {
                    List<ScanResult> cachedScanResults = scanProxy.getScanResults();
                    if (cachedScanResults != null) {
                        for (ScanResult result : cachedScanResults) {
                            if (result != null && result.BSSID != null && bssid.equals(result.BSSID)) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }

    class ConnectedState extends State {
        private Message mSourceMessage = null;

        ConnectedState() {
        }

        public void enter() {
            WifiStateMachine wifiStateMachine = WifiStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WifiStateMachine: enter Connected state");
            stringBuilder.append(getName());
            wifiStateMachine.logd(stringBuilder.toString());
            WifiStateMachine.this.processStatistics(0);
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                wifiStateMachine = WifiStateMachine.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Enter ConnectedState  mScreenOn=");
                stringBuilder2.append(WifiStateMachine.this.mScreenOn);
                wifiStateMachine.log(stringBuilder2.toString());
            }
            WifiStateMachine.this.triggerRoamingNetworkMonitor(WifiStateMachine.this.mIsAutoRoaming);
            WifiStateMachine.this.handleConnectedInWifiPro();
            if (WifiStateMachine.this.mWifiRepeater != null) {
                WifiStateMachine.this.mWifiRepeater.handleWifiConnect(WifiStateMachine.this.mWifiInfo, WifiStateMachine.this.mWifiConfigManager.getConfiguredNetwork(WifiStateMachine.this.mWifiInfo.getNetworkId()));
            }
            if (WifiStateMachine.this.mWifiConnectivityManager != null) {
                WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(1);
            }
            WifiStateMachine.this.registerConnected();
            WifiStateMachine.this.lastConnectAttemptTimestamp = 0;
            WifiStateMachine.this.targetWificonfiguration = null;
            WifiStateMachine.this.mIsAutoRoaming = false;
            if (WifiStateMachine.this.testNetworkDisconnect) {
                WifiStateMachine.this.testNetworkDisconnectCounter = WifiStateMachine.this.testNetworkDisconnectCounter + 1;
                wifiStateMachine = WifiStateMachine.this;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("ConnectedState Enter start disconnect test ");
                stringBuilder3.append(WifiStateMachine.this.testNetworkDisconnectCounter);
                wifiStateMachine.logd(stringBuilder3.toString());
                WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT, WifiStateMachine.this.testNetworkDisconnectCounter, 0), 15000);
            }
            WifiStateMachine.this.mLastDriverRoamAttempt = 0;
            WifiStateMachine.this.mTargetNetworkId = -1;
            WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().connectedStateTransition(true);
            WifiStateMachine.this.triggerUpdateAPInfo();
            WifiStateMachine.this.mWifiStateTracker.updateState(3);
            WifiStateMachine.this.notifyWlanChannelNumber(WifiCommonUtils.convertFrequencyToChannelNumber(WifiStateMachine.this.mWifiInfo.getFrequency()));
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.mNetworkAgent.sendWifiApType(WifiStateMachine.this.getWifiApTypeFromMpLink());
            }
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            boolean z = false;
            String str;
            WifiConfiguration config;
            int netId;
            switch (message.what) {
                case WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT /*131161*/:
                    if (message.arg1 == WifiStateMachine.this.testNetworkDisconnectCounter) {
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_UNWANTED_NETWORK /*131216*/:
                    if (message.arg1 == 0) {
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 3);
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    } else if (message.arg1 == 2 || message.arg1 == 1 || message.arg1 == 3) {
                        String str2 = WifiStateMachine.TAG;
                        if (message.arg1 == 2) {
                            str = "NETWORK_STATUS_UNWANTED_DISABLE_AUTOJOIN";
                        } else {
                            str = "NETWORK_STATUS_UNWANTED_VALIDATION_FAILED";
                        }
                        Log.d(str2, str);
                        config = WifiStateMachine.this.getCurrentWifiConfiguration();
                        if (config != null) {
                            if (message.arg1 == 2) {
                                WifiStateMachine.this.mWifiConfigManager.setNetworkValidatedInternetAccess(config.networkId, false);
                                Log.d(WifiStateMachine.TAG, "updateNetworkSelectionStatus(DISABLED_NO_INTERNET)");
                                WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(config.networkId, 10);
                            } else {
                                WifiStateMachine.this.mWifiConfigManager.incrementNetworkNoInternetAccessReports(config.networkId);
                            }
                            WifiStateMachine.this.handleUnwantedNetworkInWifiPro(config, message.arg1);
                        }
                    }
                    return true;
                case WifiStateMachine.CMD_START_ROAM /*131217*/:
                    WifiStateMachine.this.mLastDriverRoamAttempt = 0;
                    netId = message.arg1;
                    ScanResult candidate = message.obj;
                    String bssid = "any";
                    if (candidate != null) {
                        bssid = candidate.BSSID;
                    }
                    config = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetworkWithoutMasking(netId);
                    if (config != null) {
                        WifiStateMachine.this.setTargetBssid(config, bssid);
                        WifiStateMachine.this.mTargetNetworkId = netId;
                        WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("CMD_START_ROAM sup state ");
                        stringBuilder.append(WifiStateMachine.this.mSupplicantStateTracker.getSupplicantStateName());
                        stringBuilder.append(" my state ");
                        stringBuilder.append(WifiStateMachine.this.getCurrentState().getName());
                        stringBuilder.append(" nid=");
                        stringBuilder.append(Integer.toString(netId));
                        stringBuilder.append(" config ");
                        stringBuilder.append(config.configKey());
                        stringBuilder.append(" targetRoamBSSID ");
                        stringBuilder.append(WifiStateMachine.this.mTargetRoamBSSID);
                        wifiStateMachine.logd(stringBuilder.toString());
                        WifiStateMachine.this.reportConnectionAttemptStart(config, WifiStateMachine.this.mTargetRoamBSSID, 3);
                        if (!WifiStateMachine.this.mWifiNative.roamToNetwork(WifiStateMachine.this.mInterfaceName, config)) {
                            WifiStateMachine wifiStateMachine2 = WifiStateMachine.this;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("CMD_START_ROAM Failed to start roaming to network ");
                            stringBuilder2.append(config);
                            wifiStateMachine2.loge(stringBuilder2.toString());
                            WifiStateMachine.this.reportConnectionAttemptEnd(5, 1);
                            WifiStateMachine.this.replyToMessage(message, 151554, 0);
                            WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                            break;
                        }
                        WifiStateMachine.this.lastConnectAttemptTimestamp = WifiStateMachine.this.mClock.getWallClockMillis();
                        WifiStateMachine.this.targetWificonfiguration = config;
                        WifiStateMachine.this.mIsAutoRoaming = true;
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(12, config);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mRoamingState);
                        break;
                    }
                    WifiStateMachine.this.loge("CMD_START_ROAM and no config, bail out...");
                    break;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID /*131219*/:
                    WifiStateMachine.this.mLastDriverRoamAttempt = WifiStateMachine.this.mClock.getWallClockMillis();
                    WifiStateMachine.this.notifyWifiRoamingStarted();
                    return false;
                case WifiStateMachine.CMD_NETWORK_STATUS /*131220*/:
                    if (message.arg1 == 1) {
                        config = WifiStateMachine.this.getCurrentWifiConfiguration();
                        if (config != null) {
                            WifiStateMachine.this.handleValidNetworkInWifiPro(config);
                            WifiStateMachine.this.mWifiConfigManager.updateNetworkSelectionStatus(config.networkId, 0);
                            WifiStateMachine.this.mWifiConfigManager.setNetworkValidatedInternetAccess(config.networkId, true);
                        }
                    }
                    return true;
                case WifiStateMachine.CMD_ACCEPT_UNVALIDATED /*131225*/:
                    if (message.arg1 != 0) {
                        z = true;
                    }
                    WifiStateMachine.this.mWifiConfigManager.setNetworkNoInternetAccessExpected(WifiStateMachine.this.mLastNetworkId, z);
                    return true;
                case WifiStateMachine.CMD_START_IP_PACKET_OFFLOAD /*131232*/:
                    netId = message.arg1;
                    KeepalivePacketData pkt = message.obj;
                    int result = WifiStateMachine.this.startWifiIPPacketOffload(netId, pkt, message.arg2);
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(netId, result);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_SET_DETECTMODE_CONF /*131772*/:
                    WifiStateMachine.this.processSetVoWifiDetectMode(message);
                    break;
                case WifiStateMachine.CMD_SET_DETECT_PERIOD /*131773*/:
                    WifiStateMachine.this.processSetVoWifiDetectPeriod(message);
                    break;
                case WifiStateMachine.POOR_LINK_DETECTED /*131873*/:
                    WifiStateMachine.this.logd("handle WifiStateMachine: POOR_LINK_DETECTED");
                    WifiStateMachine.this.wifiNetworkExplicitlyUnselected();
                    WifiStateMachine.this.setNetworkDetailedState(DetailedState.VERIFYING_POOR_LINK);
                    WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    break;
                case WifiStateMachine.GOOD_LINK_DETECTED /*131874*/:
                    WifiStateMachine.this.logd("handle WifiStateMachine: GOOD_LINK_DETECTED");
                    WifiStateMachine.this.updateWifiBackgroudStatus(message.arg1);
                    WifiStateMachine.this.wifiNetworkExplicitlySelected();
                    WifiStateMachine.this.setWifiBackgroundStatus(false);
                    WifiStateMachine.this.sendConnectedState();
                    break;
                case WifiStateMachine.INVALID_LINK_DETECTED /*131875*/:
                    WifiStateMachine.this.logd("handle WifiStateMachine: INVALID_LINK_DETECTED");
                    WifiStateMachine.this.triggerInvalidlinkNetworkMonitor();
                    break;
                case WifiStateMachine.SCE_REQUEST_UPDATE_DNS_SERVER /*131882*/:
                    WifiStateMachine.this.logd("handle WifiStateMachine: SCE_REQUEST_UPDATE_DNS_SERVER");
                    WifiStateMachine.this.sendUpdateDnsServersRequest(message, WifiStateMachine.this.mLinkProperties);
                    break;
                case WifiStateMachine.SCE_REQUEST_RENEW_DHCP /*131883*/:
                    WifiStateMachine.this.logd("handle WifiStateMachine: SCE_REQUEST_RENEW_DHCP");
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                    break;
                case WifiStateMachine.SCE_REQUEST_SET_STATIC_IP /*131884*/:
                    WifiStateMachine.this.logd("handle WifiStateMachine: SCE_REQUEST_SET_STATIC_IP");
                    WifiStateMachine.this.stopIpClient();
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.SCE_START_SET_STATIC_IP, message.obj, 1000);
                    break;
                case WifiStateMachine.SCE_START_SET_STATIC_IP /*131885*/:
                    WifiStateMachine.this.logd("handle WifiStateMachine: SCE_START_SET_STATIC_IP");
                    WifiStateMachine.this.handleStaticIpConfig(WifiStateMachine.this.mIpClient, WifiStateMachine.this.mWifiNative, (StaticIpConfiguration) message.obj);
                    break;
                case WifiStateMachine.SCE_REQUEST_REASSOC_WIFI /*131886*/:
                    WifiStateMachine.this.startSelfCureWifiReassoc();
                    break;
                case WifiStateMachine.SCE_REQUEST_RESET_WIFI /*131887*/:
                    WifiStateMachine.this.startSelfCureWifiReset();
                    break;
                case WifiStateMachine.CMD_SCE_HANDLE_IP_INVALID /*131895*/:
                    WifiStateMachine.this.startSelfCureReconnect();
                    WifiStateMachine.this.mIpClient.forceRemoveDhcpCache();
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.CMD_DISCONNECT, 500);
                    break;
                case WifiStateMachine.CMD_SCE_HANDLE_IP_NO_INTERNET /*131898*/:
                    WifiStateMachine.this.mIpClient.forceRemoveDhcpCache();
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                    WifiStateMachine.this.reportConnectionAttemptEnd(6, 1);
                    if (WifiStateMachine.this.mLastDriverRoamAttempt != 0) {
                        long lastRoam = WifiStateMachine.this.mClock.getWallClockMillis() - WifiStateMachine.this.mLastDriverRoamAttempt;
                        WifiStateMachine.this.mLastDriverRoamAttempt = 0;
                    }
                    if (WifiStateMachine.unexpectedDisconnectedReason(message.arg2)) {
                        WifiStateMachine.this.mWifiDiagnostics.captureBugReportData(5);
                    }
                    config = WifiStateMachine.this.getCurrentWifiConfiguration();
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine wifiStateMachine3 = WifiStateMachine.this;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("NETWORK_DISCONNECTION_EVENT in connected state BSSID=");
                        stringBuilder3.append(WifiStateMachine.this.mWifiInfo.getBSSID());
                        stringBuilder3.append(" RSSI=");
                        stringBuilder3.append(WifiStateMachine.this.mWifiInfo.getRssi());
                        stringBuilder3.append(" freq=");
                        stringBuilder3.append(WifiStateMachine.this.mWifiInfo.getFrequency());
                        stringBuilder3.append(" reason=");
                        stringBuilder3.append(message.arg2);
                        stringBuilder3.append(" Network Selection Status=");
                        if (config == null) {
                            str = "Unavailable";
                        } else {
                            str = config.getNetworkSelectionStatus().getNetworkStatusString();
                        }
                        stringBuilder3.append(str);
                        wifiStateMachine3.log(stringBuilder3.toString());
                        break;
                    }
                    break;
                case WifiMonitor.VOWIFI_DETECT_IRQ_STR_EVENT /*147520*/:
                    WifiStateMachine.this.logd("receive Vo WifiDetect event 1");
                    if (this.mSourceMessage != null) {
                        WifiStateMachine.this.logd("receive Vo WifiDetect event 2");
                        WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151576);
                        break;
                    }
                    break;
                case 151575:
                    WifiStateMachine.this.logd("start VoWifiDetect ");
                    this.mSourceMessage = Message.obtain(message);
                    break;
                default:
                    return false;
            }
            return true;
        }

        public void exit() {
            WifiStateMachine.this.logd("WifiStateMachine: Leaving Connected state");
            WifiStateMachine.this.processStatistics(1);
            WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(3);
            WifiStateMachine.this.mLastDriverRoamAttempt = 0;
            WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().connectedStateTransition(false);
            WifiStateMachine.this.notifyWlanState(WifiCommonUtils.STATE_DISCONNECTED);
        }
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            Message message2 = message;
            WifiStateMachine.this.logStateAndMessage(message2, this);
            int i = -1;
            boolean z = false;
            WifiStateMachine wifiStateMachine;
            StringBuilder stringBuilder;
            WifiStateMachine wifiStateMachine2;
            boolean disableOthers;
            StringBuilder stringBuilder2;
            switch (message2.what) {
                case 0:
                    Log.wtf(WifiStateMachine.TAG, "Error! empty message encountered");
                    break;
                case 69632:
                    if (((AsyncChannel) message2.obj) == WifiStateMachine.this.mWifiP2pChannel) {
                        if (message2.arg1 != 0) {
                            wifiStateMachine = WifiStateMachine.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("WifiP2pService connection failure, error=");
                            stringBuilder.append(message2.arg1);
                            wifiStateMachine.loge(stringBuilder.toString());
                            break;
                        }
                        WifiStateMachine.this.p2pSendMessage(69633);
                        if (WifiStateMachine.this.mOperationalMode == 1) {
                            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_ENABLE_P2P);
                            break;
                        }
                    }
                    WifiStateMachine.this.loge("got HALF_CONNECTED for unknown channel");
                    break;
                    break;
                case 69636:
                    if (message2.obj == WifiStateMachine.this.mWifiP2pChannel) {
                        wifiStateMachine = WifiStateMachine.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("WifiP2pService channel lost, message.arg1 =");
                        stringBuilder.append(message2.arg1);
                        wifiStateMachine.loge(stringBuilder.toString());
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_BLUETOOTH_ADAPTER_STATE_CHANGE /*131103*/:
                    wifiStateMachine2 = WifiStateMachine.this;
                    if (message2.arg1 != 0) {
                        z = true;
                    }
                    wifiStateMachine2.mBluetoothConnectionActive = z;
                    break;
                case WifiStateMachine.CMD_ADD_OR_UPDATE_NETWORK /*131124*/:
                    NetworkUpdateResult result = WifiStateMachine.this.mWifiConfigManager.addOrUpdateNetwork(message2.obj, message2.sendingUid);
                    if (!result.isSuccess()) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    }
                    WifiStateMachine.this.replyToMessage(message2, message2.what, result.getNetworkId());
                    break;
                case WifiStateMachine.CMD_REMOVE_NETWORK /*131125*/:
                    WifiStateMachine.this.deleteNetworkConfigAndSendReply(message2, false);
                    break;
                case WifiStateMachine.CMD_ENABLE_NETWORK /*131126*/:
                    if (message2.arg2 == 1) {
                        z = true;
                    }
                    disableOthers = z;
                    boolean ok = WifiStateMachine.this.mWifiConfigManager.enableNetwork(message2.arg1, disableOthers, message2.sendingUid);
                    if (!ok) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL;
                    }
                    wifiStateMachine = WifiStateMachine.this;
                    int i2 = message2.what;
                    if (ok) {
                        i = 1;
                    }
                    wifiStateMachine.replyToMessage(message2, i2, i);
                    break;
                case WifiStateMachine.CMD_GET_CONFIGURED_NETWORKS /*131131*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, (Object) WifiStateMachine.this.mWifiConfigManager.getSavedNetworks());
                    break;
                case WifiStateMachine.CMD_GET_SUPPORTED_FEATURES /*131133*/:
                    if (WifiStateMachine.this.mFeatureSet <= 0) {
                        WifiStateMachine.this.mFeatureSet = WifiStateMachine.this.mWifiNative.getSupportedFeatureSet(WifiStateMachine.this.mInterfaceName);
                        if (WifiStateMachine.DBG) {
                            String str = WifiStateMachine.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("CMD_GET_SUPPORTED_FEATURES: ");
                            stringBuilder2.append(WifiStateMachine.this.mFeatureSet);
                            Log.d(str, stringBuilder2.toString());
                        }
                    }
                    WifiStateMachine.this.replyToMessage(message2, message2.what, WifiStateMachine.this.mFeatureSet);
                    break;
                case WifiStateMachine.CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS /*131134*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, (Object) WifiStateMachine.this.mWifiConfigManager.getConfiguredNetworksWithPasswords());
                    break;
                case WifiStateMachine.CMD_GET_LINK_LAYER_STATS /*131135*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, null);
                    break;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE /*131144*/:
                case WifiStateMachine.CMD_UPDATE_WIFIPRO_CONFIGURATIONS /*131672*/:
                case WifiStateMachine.INVALID_LINK_DETECTED /*131875*/:
                    break;
                case WifiStateMachine.CMD_DISCONNECT /*131145*/:
                case WifiStateMachine.CMD_RECONNECT /*131146*/:
                case WifiStateMachine.CMD_REASSOCIATE /*131147*/:
                case WifiStateMachine.CMD_RSSI_POLL /*131155*/:
                case WifiStateMachine.CMD_TEST_NETWORK_DISCONNECT /*131161*/:
                case WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER /*131166*/:
                case WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER /*131168*/:
                case WifiStateMachine.CMD_DISABLE_EPHEMERAL_NETWORK /*131170*/:
                case WifiStateMachine.CMD_DISABLE_P2P_WATCHDOG_TIMER /*131184*/:
                case WifiStateMachine.CMD_ENABLE_P2P /*131203*/:
                case WifiStateMachine.CMD_DISABLE_P2P_RSP /*131205*/:
                case WifiStateMachine.CMD_TARGET_BSSID /*131213*/:
                case WifiStateMachine.CMD_RELOAD_TLS_AND_RECONNECT /*131214*/:
                case WifiStateMachine.CMD_START_CONNECT /*131215*/:
                case WifiStateMachine.CMD_UNWANTED_NETWORK /*131216*/:
                case WifiStateMachine.CMD_START_ROAM /*131217*/:
                case WifiStateMachine.CMD_ASSOCIATED_BSSID /*131219*/:
                case WifiStateMachine.CMD_WPS_PIN_RETRY /*131576*/:
                case WifiStateMachine.CMD_SET_DETECTMODE_CONF /*131772*/:
                case WifiStateMachine.CMD_SET_DETECT_PERIOD /*131773*/:
                case WifiMonitor.NETWORK_CONNECTION_EVENT /*147459*/:
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT /*147463*/:
                case WifiMonitor.WPS_OVERLAP_EVENT /*147466*/:
                case WifiMonitor.SUP_REQUEST_IDENTITY /*147471*/:
                case WifiMonitor.SUP_REQUEST_SIM_AUTH /*147472*/:
                case 147474:
                case WifiMonitor.WAPI_CERTIFICATION_FAILURE_EVENT /*147475*/:
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT /*147499*/:
                case WifiMonitor.VOWIFI_DETECT_IRQ_STR_EVENT /*147520*/:
                case WifiMonitor.WPS_START_OKC_EVENT /*147656*/:
                case WifiMonitor.EAP_ERRORCODE_REPORT_EVENT /*147956*/:
                case 151575:
                case 196611:
                case 196612:
                case 196614:
                case 196618:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE /*131149*/:
                    if (message2.arg1 != 1) {
                        WifiStateMachine.this.setSuspendOptimizations(2, true);
                        break;
                    }
                    WifiStateMachine.this.setSuspendOptimizations(2, false);
                    break;
                case WifiStateMachine.CMD_ENABLE_RSSI_POLL /*131154*/:
                    wifiStateMachine2 = WifiStateMachine.this;
                    if (message2.arg1 == 1) {
                        z = true;
                    }
                    wifiStateMachine2.mEnableRssiPolling = z;
                    break;
                case WifiStateMachine.CMD_SET_SUSPEND_OPT_ENABLED /*131158*/:
                    if (message2.arg1 != 1) {
                        WifiStateMachine.this.setSuspendOptimizations(4, false);
                        break;
                    }
                    if (message2.arg2 == 1) {
                        WifiStateMachine.this.mSuspendWakeLock.release();
                    }
                    WifiStateMachine.this.setSuspendOptimizations(4, true);
                    break;
                case WifiStateMachine.CMD_SCREEN_STATE_CHANGED /*131167*/:
                    wifiStateMachine2 = WifiStateMachine.this;
                    if (message2.arg1 != 0) {
                        z = true;
                    }
                    wifiStateMachine2.handleScreenStateChanged(z);
                    break;
                case WifiStateMachine.CMD_REMOVE_APP_CONFIGURATIONS /*131169*/:
                    WifiStateMachine.this.deferMessage(message2);
                    break;
                case WifiStateMachine.CMD_GET_MATCHING_CONFIG /*131171*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what);
                    break;
                case WifiStateMachine.CMD_RESET_SIM_NETWORKS /*131173*/:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message2);
                    break;
                case WifiStateMachine.CMD_QUERY_OSU_ICON /*131176*/:
                case WifiStateMachine.CMD_MATCH_PROVIDER_NETWORK /*131177*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what);
                    break;
                case WifiStateMachine.CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG /*131178*/:
                    if (WifiStateMachine.this.mPasspointManager.addOrUpdateProvider((PasspointConfiguration) message2.obj, message2.arg1)) {
                        i = 1;
                    }
                    WifiStateMachine.this.replyToMessage(message2, message2.what, i);
                    break;
                case WifiStateMachine.CMD_REMOVE_PASSPOINT_CONFIG /*131179*/:
                    if (WifiStateMachine.this.mPasspointManager.removeProvider((String) message2.obj)) {
                        i = 1;
                    }
                    WifiStateMachine.this.replyToMessage(message2, message2.what, i);
                    break;
                case WifiStateMachine.CMD_GET_PASSPOINT_CONFIGS /*131180*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, (Object) WifiStateMachine.this.mPasspointManager.getProviderConfigs());
                    break;
                case WifiStateMachine.CMD_GET_MATCHING_OSU_PROVIDERS /*131181*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, (Object) new ArrayList());
                    break;
                case WifiStateMachine.CMD_BOOT_COMPLETED /*131206*/:
                    WifiStateMachine.this.isBootCompleted = true;
                    WifiStateMachine.this.getAdditionalWifiServiceInterfaces();
                    if (!WifiStateMachine.this.mWifiConfigManager.loadFromStore()) {
                        Log.e(WifiStateMachine.TAG, "Failed to load from config store");
                    }
                    WifiStateMachine.this.maybeRegisterNetworkFactory();
                    break;
                case WifiStateMachine.CMD_INITIALIZE /*131207*/:
                    disableOthers = WifiStateMachine.this.mWifiNative.initialize();
                    WifiStateMachine.this.mPasspointManager.initializeProvisioner(WifiStateMachine.this.mWifiInjector.getWifiServiceHandlerThread().getLooper());
                    wifiStateMachine = WifiStateMachine.this;
                    int i3 = message2.what;
                    if (disableOthers) {
                        i = 1;
                    }
                    wifiStateMachine.replyToMessage(message2, i3, i);
                    break;
                case WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL /*131210*/:
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST /*131211*/:
                case WifiStateMachine.CMD_IP_REACHABILITY_LOST /*131221*/:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.CMD_UPDATE_LINKPROPERTIES /*131212*/:
                    WifiStateMachine.this.updateLinkProperties((LinkProperties) message2.obj);
                    break;
                case WifiStateMachine.CMD_REMOVE_USER_CONFIGURATIONS /*131224*/:
                    WifiStateMachine.this.deferMessage(message2);
                    break;
                case WifiStateMachine.CMD_START_IP_PACKET_OFFLOAD /*131232*/:
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(message2.arg1, -20);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_STOP_IP_PACKET_OFFLOAD /*131233*/:
                    if (WifiStateMachine.this.mNetworkAgent != null) {
                        WifiStateMachine.this.mNetworkAgent.onPacketKeepaliveEvent(message2.arg1, -20);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_START_RSSI_MONITORING_OFFLOAD /*131234*/:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD /*131235*/:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.CMD_GET_ALL_MATCHING_CONFIGS /*131240*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, (Object) new ArrayList());
                    break;
                case WifiStateMachine.CMD_INSTALL_PACKET_FILTER /*131274*/:
                    WifiStateMachine.this.mWifiNative.installPacketFilter(WifiStateMachine.this.mInterfaceName, (byte[]) message2.obj);
                    break;
                case WifiStateMachine.CMD_SET_FALLBACK_PACKET_FILTERING /*131275*/:
                    if (!((Boolean) message2.obj).booleanValue()) {
                        WifiStateMachine.this.mWifiNative.stopFilteringMulticastV4Packets(WifiStateMachine.this.mInterfaceName);
                        break;
                    }
                    WifiStateMachine.this.mWifiNative.startFilteringMulticastV4Packets(WifiStateMachine.this.mInterfaceName);
                    break;
                case WifiStateMachine.CMD_USER_SWITCH /*131277*/:
                    Set<Integer> removedNetworkIds = WifiStateMachine.this.mWifiConfigManager.handleUserSwitch(message2.arg1);
                    if (removedNetworkIds.contains(Integer.valueOf(WifiStateMachine.this.mTargetNetworkId)) || removedNetworkIds.contains(Integer.valueOf(WifiStateMachine.this.mLastNetworkId))) {
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        break;
                    }
                case WifiStateMachine.CMD_USER_UNLOCK /*131278*/:
                    WifiStateMachine.this.mWifiConfigManager.handleUserUnlock(message2.arg1);
                    break;
                case WifiStateMachine.CMD_USER_STOP /*131279*/:
                    WifiStateMachine.this.mWifiConfigManager.handleUserStop(message2.arg1);
                    break;
                case WifiStateMachine.CMD_READ_PACKET_FILTER /*131280*/:
                    WifiStateMachine.this.mIpClient.readPacketFilterComplete(WifiStateMachine.this.mWifiNative.readPacketFilter(WifiStateMachine.this.mInterfaceName));
                    break;
                case WifiStateMachine.CMD_DIAGS_CONNECT_TIMEOUT /*131324*/:
                    WifiStateMachine.this.mWifiDiagnostics.reportConnectionEvent(((Long) message2.obj).longValue(), (byte) 2);
                    break;
                case WifiStateMachine.CMD_START_SUBSCRIPTION_PROVISIONING /*131326*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, 0);
                    break;
                case WifiStateMachine.CMD_GET_CHANNEL_LIST_5G /*131572*/:
                    WifiStateMachine.this.replyToMessage(message2, message2.what, null);
                    break;
                case WifiStateMachine.CMD_PNO_PERIODIC_SCAN /*131575*/:
                    WifiStateMachine.this.deferMessage(message2);
                    break;
                case WifiStateMachine.CMD_GET_SUPPORT_VOWIFI_DETECT /*131774*/:
                    WifiStateMachine.this.processIsSupportVoWifiDetect(message2);
                    break;
                case WifiStateMachine.GOOD_LINK_DETECTED /*131874*/:
                    WifiStateMachine.this.log("GOOD_LINK_DETECTED, state = DefaultState");
                    WifiStateMachine.this.setWifiBackgroundStatus(false);
                    break;
                case WifiStateMachine.CMD_SCE_WIFI_OFF_TIMEOUT /*131888*/:
                case WifiStateMachine.CMD_SCE_WIFI_ON_TIMEOUT /*131889*/:
                case WifiStateMachine.CMD_SCE_WIFI_CONNECT_TIMEOUT /*131890*/:
                case WifiStateMachine.CMD_SCE_WIFI_REASSOC_TIMEOUT /*131891*/:
                case WifiStateMachine.CMD_SCE_WIFI_RECONNECT_TIMEOUT /*131896*/:
                case WifiStateMachine.WIFIPRO_SOFT_CONNECT_TIMEOUT /*131897*/:
                    wifiStateMachine2 = WifiStateMachine.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("wifi self cure timeout, message type = ");
                    stringBuilder2.append(message2.what);
                    wifiStateMachine2.log(stringBuilder2.toString());
                    WifiStateMachine.this.notifySelfCureComplete(false, message2.arg1);
                    break;
                case WifiStateMachine.CMD_SCE_STOP_SELF_CURE /*131892*/:
                    wifiStateMachine2 = WifiStateMachine.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("CMD_SCE_STOP_SELF_CURE, arg1 =");
                    stringBuilder2.append(message2.arg1);
                    wifiStateMachine2.log(stringBuilder2.toString());
                    WifiStateMachine.this.stopSelfCureWifi(message2.arg1);
                    if (message2.arg1 < 0) {
                        if (WifiStateMachine.this.getCurrentState() == WifiStateMachine.this.mDisconnectedState) {
                            WifiStateMachine.this.setNetworkDetailedState(DetailedState.DISCONNECTED);
                            WifiStateMachine.this.sendNetworkStateChangeBroadcast(null);
                            break;
                        }
                        WifiStateMachine.this.log("CMD_SCE_STOP_SELF_CURE, to disconnect because of wifi self cure failed.");
                        WifiStateMachine.this.removeMessages(WifiMonitor.NETWORK_CONNECTION_EVENT);
                        WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_DISCONNECT);
                        WifiStateMachine.this.handleNetworkDisconnect();
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_SCE_RESTORE /*131893*/:
                    if (WifiStateMachine.this.mNetworkAgent == null) {
                        WifiStateMachine.this.log("CMD_SCE_RESTORE, use networkAgent to sendNetworkInfo");
                        WifiStateMachine.this.setNetworkDetailedState(DetailedState.DISCONNECTED);
                        new WifiNetworkAgent(WifiStateMachine.this, WifiStateMachine.this.getHandler().getLooper(), WifiStateMachine.this.mContext, "WifiNetworkAgent", WifiStateMachine.this.mNetworkInfo, WifiStateMachine.this.mNetworkCapabilitiesFilter, WifiStateMachine.this.mLinkProperties, 100, WifiStateMachine.this.mNetworkMisc).sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
                        break;
                    }
                    WifiStateMachine.this.log("CMD_SCE_RESTORE, use mNetworkAgent to sendNetworkInfo");
                    WifiStateMachine.this.mNetworkAgent.sendNetworkInfo(WifiStateMachine.this.mNetworkInfo);
                    break;
                case WifiStateMachine.CMD_SCE_NOTIFY_WIFI_DISABLED /*131894*/:
                    WifiStateMachine.this.log("CMD_SCE_NOTIFY_WIFI_DISABLED, set WIFI_STATE_DISABLED");
                    break;
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED /*143371*/:
                    WifiStateMachine.this.mP2pConnected.set(message2.obj.isConnected());
                    break;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST /*143372*/:
                    wifiStateMachine2 = WifiStateMachine.this;
                    if (message2.arg1 == 1) {
                        z = true;
                    }
                    wifiStateMachine2.mTemporarilyDisconnectWifi = z;
                    WifiStateMachine.this.replyToMessage(message2, WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT /*147462*/:
                    if (message2.obj.state == SupplicantState.INTERFACE_DISABLED) {
                        Log.e(WifiStateMachine.TAG, "Detected drive hang , recover");
                        WifiStateMachine.this.mWifiInjector.getSelfRecovery().trigger(1);
                        break;
                    }
                    break;
                case WifiMonitor.EVENT_ANT_CORE_ROB /*147757*/:
                    WifiStateMachine.this.handleAntenaPreempted();
                    break;
                case 151553:
                    WifiStateMachine.this.replyToMessage(message2, 151554, 2);
                    break;
                case 151556:
                    WifiStateMachine.this.deleteNetworkConfigAndSendReply(message2, true);
                    break;
                case 151559:
                    WifiStateMachine.this.saveNetworkConfigAndSendReply(message2);
                    break;
                case 151562:
                    WifiStateMachine.this.replyToMessage(message2, 151564, 2);
                    break;
                case 151566:
                    WifiStateMachine.this.replyToMessage(message2, 151567, 2);
                    break;
                case 151569:
                    WifiStateMachine.this.replyToMessage(message2, 151570, 2);
                    break;
                case 151572:
                    WifiStateMachine.this.replyToMessage(message2, 151574, 2);
                    break;
                default:
                    wifiStateMachine2 = WifiStateMachine.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error! unhandled message");
                    stringBuilder2.append(message2);
                    wifiStateMachine2.loge(stringBuilder2.toString());
                    break;
            }
            return true;
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            Log.i(WifiStateMachine.TAG, "disconnectedstate enter");
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.log(getName());
            }
            if (WifiStateMachine.this.mTemporarilyDisconnectWifi) {
                WifiStateMachine.this.p2pSendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE);
                return;
            }
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" Enter DisconnectedState screenOn=");
                stringBuilder.append(WifiStateMachine.this.mScreenOn);
                wifiStateMachine.logd(stringBuilder.toString());
            }
            WifiStateMachine.this.handleDisconnectedInWifiPro();
            if (WifiStateMachine.this.mWifiRepeater != null) {
                WifiStateMachine.this.mWifiRepeater.handleWifiDisconnect();
            }
            WifiStateMachine.this.mIsAutoRoaming = false;
            WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(2);
            WifiStateMachine.this.mDisconnectedTimeStamp = WifiStateMachine.this.mClock.getWallClockMillis();
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_DISCONNECT /*131145*/:
                    WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 2);
                    WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                    return true;
                case WifiStateMachine.CMD_RECONNECT /*131146*/:
                case WifiStateMachine.CMD_REASSOCIATE /*131147*/:
                    if (WifiStateMachine.this.mTemporarilyDisconnectWifi) {
                        return true;
                    }
                    return false;
                case WifiStateMachine.CMD_SCREEN_STATE_CHANGED /*131167*/:
                    WifiStateMachine.this.handleScreenStateChanged(message.arg1 != 0);
                    return true;
                case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED /*143371*/:
                    WifiStateMachine.this.mP2pConnected.set(message.obj.isConnected());
                    return true;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT /*147462*/:
                    StateChangeResult stateChangeResult = message.obj;
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("SUPPLICANT_STATE_CHANGE_EVENT state=");
                        stringBuilder.append(stateChangeResult.state);
                        stringBuilder.append(" -> state= ");
                        stringBuilder.append(WifiInfo.getDetailedStateOf(stateChangeResult.state));
                        wifiStateMachine.logd(stringBuilder.toString());
                    }
                    WifiStateMachine.this.setNetworkDetailedState(WifiInfo.getDetailedStateOf(stateChangeResult.state));
                    return false;
                default:
                    return false;
            }
        }

        public void exit() {
            WifiStateMachine.this.mWifiConnectivityManager.handleConnectionStateChanged(3);
        }
    }

    class DisconnectingState extends State {
        DisconnectingState() {
        }

        public void enter() {
            WifiStateMachine wifiStateMachine;
            StringBuilder stringBuilder;
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                wifiStateMachine = WifiStateMachine.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append(" Enter DisconnectingState State screenOn=");
                stringBuilder.append(WifiStateMachine.this.mScreenOn);
                wifiStateMachine.logd(stringBuilder.toString());
            }
            wifiStateMachine = WifiStateMachine.this;
            wifiStateMachine.disconnectingWatchdogCount++;
            wifiStateMachine = WifiStateMachine.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Start Disconnecting Watchdog ");
            stringBuilder.append(WifiStateMachine.this.disconnectingWatchdogCount);
            wifiStateMachine.logd(stringBuilder.toString());
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER, WifiStateMachine.this.disconnectingWatchdogCount, 0), 5000);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE /*131144*/:
                    if (message.arg1 == 1) {
                        if (WifiStateMachine.this.hasDeferredMessagesForArg1(WifiStateMachine.CMD_SET_OPERATIONAL_MODE, 4)) {
                            WifiStateMachine.this.log("Has deferred DISABLED_MODE, deffer CONNECT_MODE");
                            WifiStateMachine.this.deferMessage(message);
                            break;
                        }
                    }
                    WifiStateMachine.this.deferMessage(message);
                    break;
                    break;
                case WifiStateMachine.CMD_DISCONNECT /*131145*/:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("Ignore CMD_DISCONNECT when already disconnecting.");
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_DISCONNECTING_WATCHDOG_TIMER /*131168*/:
                    if (WifiStateMachine.this.disconnectingWatchdogCount == message.arg1) {
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine.this.log("disconnecting watchdog! -> disconnect");
                        }
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                        break;
                    }
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT /*147462*/:
                    WifiStateMachine.this.deferMessage(message);
                    WifiStateMachine.this.handleNetworkDisconnect();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    private class HiddenScanListener implements ScanListener {
        private WifiConfiguration mConfig = null;
        private List<ScanResult> mScanResults = new ArrayList();
        private int mSendingUid = -1;

        HiddenScanListener(WifiConfiguration config, int uid) {
            this.mConfig = config;
            this.mSendingUid = uid;
        }

        private void quit() {
            this.mConfig = null;
            this.mSendingUid = -1;
            this.mScanResults.clear();
        }

        public void onResults(ScanData[] scanDatas) {
            if (this.mConfig == null || WifiStateMachine.this.mWifiConfigManager.getConfiguredNetwork(this.mConfig.configKey()) == null || this.mScanResults.size() == 0) {
                Log.d(WifiStateMachine.TAG, "HiddenScanListener: return since config removed.");
                return;
            }
            String ssid = NativeUtil.removeEnclosingQuotes(this.mConfig.SSID);
            int size = this.mScanResults.size();
            for (int i = 0; i < size; i++) {
                ScanResult result = (ScanResult) this.mScanResults.get(i);
                if (!(result == null || result.wifiSsid == null || TextUtils.isEmpty(result.wifiSsid.oriSsid) || TextUtils.isEmpty(result.SSID) || !result.SSID.equals(ssid))) {
                    if (WifiStateMachine.this.mactchResultAndConfigSecurity(result, this.mConfig)) {
                        this.mConfig.oriSsid = result.wifiSsid.oriSsid;
                        String str = WifiStateMachine.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("HiddenScanListener: find SSID=");
                        stringBuilder.append(ssid);
                        stringBuilder.append(" oriSsid=");
                        stringBuilder.append(this.mConfig.oriSsid);
                        Log.d(str, stringBuilder.toString());
                        WifiStateMachine.this.mWifiConfigManager.addOrUpdateNetwork(this.mConfig, this.mSendingUid);
                        WifiStateMachine.this.startConnectToUserSelectNetwork(this.mConfig.networkId, this.mSendingUid, "any");
                        quit();
                        return;
                    }
                    Log.d(WifiStateMachine.TAG, "ResultAndConfigSecurity not mactch");
                }
            }
            String str2 = WifiStateMachine.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("HiddenScanListener: can't find SSID=");
            stringBuilder2.append(ssid);
            Log.d(str2, stringBuilder2.toString());
            quit();
        }

        public void onFullResult(ScanResult scanResult) {
            this.mScanResults.add(scanResult);
        }

        public void onSuccess() {
        }

        public void onFailure(int i, String s) {
        }

        public void onPeriodChanged(int i) {
        }
    }

    class IpClientCallback extends Callback {
        IpClientCallback() {
        }

        public void onPreDhcpAction() {
            WifiStateMachine.this.sendMessage(196611);
        }

        public void onPostDhcpAction() {
            WifiStateMachine.this.sendMessage(196612);
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
            if (dhcpResults == null) {
                if (WifiStateMachine.this.mHwWifiCHRService != null) {
                    if (WifiStateMachine.this.mIpClient.getDhcpFlag() == 196609) {
                        WifiStateMachine.this.mHwWifiCHRService.updateDhcpState(4);
                    } else {
                        WifiStateMachine.this.mHwWifiCHRService.updateDhcpState(5);
                    }
                }
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IPV4_PROVISIONING_FAILURE);
                WifiStateMachine.this.mWifiInjector.getWifiLastResortWatchdog().noteConnectionFailureAndTriggerIfNeeded(WifiStateMachine.this.getTargetSsid(), WifiStateMachine.this.mTargetRoamBSSID, 3);
            } else if ("CMD_TRY_CACHED_IP".equals(dhcpResults.domains)) {
                WifiStateMachine.this.sendMessage(196618);
            } else {
                if (!(WifiStateMachine.this.mHwWifiCHRService == null || dhcpResults.ipAddress == null || WifiStateMachine.this.mWifiConfigManager == null || WifiStateMachine.this.mNetworkInfo == null)) {
                    WifiConfiguration currentConfig = WifiStateMachine.this.getCurrentWifiConfiguration();
                    boolean isUsingStaticIp = false;
                    if (currentConfig != null && currentConfig.getIpAssignment() == IpAssignment.STATIC) {
                        isUsingStaticIp = true;
                    }
                    if (isUsingStaticIp) {
                        WifiStateMachine.this.mHwWifiCHRService.updateDhcpState(9);
                    } else if (DetailedState.OBTAINING_IPADDR == WifiStateMachine.this.mNetworkInfo.getDetailedState()) {
                        if ("getCachedDhcpResultsForCurrentConfig".equals(dhcpResults.domains)) {
                            WifiStateMachine.this.mHwWifiCHRService.updateDhcpState(16);
                        } else {
                            WifiStateMachine.this.mHwWifiCHRService.updateDhcpState(2);
                        }
                    } else if (DetailedState.CONNECTED == WifiStateMachine.this.mNetworkInfo.getDetailedState()) {
                        WifiStateMachine.this.mHwWifiCHRService.updateDhcpState(3);
                    }
                }
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IPV4_PROVISIONING_SUCCESS, dhcpResults);
            }
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
            WifiStateMachine.this.mWifiMetrics.logStaEvent(7);
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_UPDATE_LINKPROPERTIES, newLp);
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL);
        }

        public void onProvisioningFailure(LinkProperties newLp) {
            WifiStateMachine.this.mWifiMetrics.logStaEvent(8);
            if (WifiStateMachine.this.mHwWifiCHRService != null) {
                WifiStateMachine.this.mHwWifiCHRService.uploadDhcpException(DhcpClient.mDhcpError);
            }
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IP_CONFIGURATION_LOST);
        }

        public void onLinkPropertiesChange(LinkProperties newLp) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_UPDATE_LINKPROPERTIES, newLp);
        }

        public void onReachabilityLost(String logMsg) {
            WifiStateMachine.this.mWifiMetrics.logStaEvent(9);
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_IP_REACHABILITY_LOST, logMsg);
        }

        public void installPacketFilter(byte[] filter) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_INSTALL_PACKET_FILTER, filter);
        }

        public void startReadPacketFilter() {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_READ_PACKET_FILTER);
        }

        public void setFallbackMulticastFilter(boolean enabled) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SET_FALLBACK_PACKET_FILTERING, Boolean.valueOf(enabled));
        }

        public void setNeighborDiscoveryOffload(boolean enabled) {
            WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_CONFIG_ND_OFFLOAD, enabled);
        }
    }

    class L2ConnectedState extends State {
        RssiEventHandler mRssiEventHandler = new RssiEventHandler();

        class RssiEventHandler implements WifiRssiEventHandler {
            RssiEventHandler() {
            }

            public void onRssiThresholdBreached(byte curRssi) {
                if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                    String str = WifiStateMachine.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onRssiThresholdBreach event. Cur Rssi = ");
                    stringBuilder.append(curRssi);
                    Log.e(str, stringBuilder.toString());
                }
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_RSSI_THRESHOLD_BREACHED, curRssi);
            }
        }

        L2ConnectedState() {
        }

        public void enter() {
            NetworkCapabilities access$5100;
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.log(getName());
            }
            WifiStateMachine.this.mRssiPollToken = WifiStateMachine.this.mRssiPollToken + 1;
            if (WifiStateMachine.this.mEnableRssiPolling) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0);
            }
            if (WifiStateMachine.this.mNetworkAgent != null) {
                WifiStateMachine.this.loge("Have NetworkAgent when entering L2Connected");
                WifiStateMachine.this.setNetworkDetailedState(DetailedState.DISCONNECTED);
            }
            WifiStateMachine.this.setNetworkDetailedState(DetailedState.CONNECTING);
            if (WifiStateMachine.this.mWifiInfo == null || WifiStateMachine.this.mWifiInfo.getSSID().equals("<unknown ssid>")) {
                access$5100 = WifiStateMachine.this.mNetworkCapabilitiesFilter;
            } else {
                access$5100 = new NetworkCapabilities(WifiStateMachine.this.mNetworkCapabilitiesFilter);
                access$5100.setSSID(WifiStateMachine.this.mWifiInfo.getSSID());
            }
            WifiStateMachine.this.mNetworkAgent = new WifiNetworkAgent(WifiStateMachine.this, WifiStateMachine.this.getHandler().getLooper(), WifiStateMachine.this.mContext, "WifiNetworkAgent", WifiStateMachine.this.mNetworkInfo, access$5100, WifiStateMachine.this.mLinkProperties, WifiStateMachine.this.reportWifiScoreDelayed() ? 99 : 60, WifiStateMachine.this.mNetworkMisc);
            WifiStateMachine.this.mWifiScoreReport.setLowScoreCount(0);
            WifiStateMachine.this.clearTargetBssid("L2ConnectedState");
            WifiStateMachine.this.mCountryCode.setReadyForChange(false);
            WifiStateMachine.this.mWifiMetrics.setWifiState(3);
        }

        public void exit() {
            WifiStateMachine.this.mIpClient.stop();
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                StringBuilder sb = new StringBuilder();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("leaving L2ConnectedState state nid=");
                stringBuilder.append(Integer.toString(WifiStateMachine.this.mLastNetworkId));
                sb.append(stringBuilder.toString());
                if (WifiStateMachine.this.mLastBssid != null) {
                    sb.append(" ");
                    sb.append(WifiStateMachine.this.mLastBssid);
                }
            }
            if (!(WifiStateMachine.this.mLastBssid == null && WifiStateMachine.this.mLastNetworkId == -1)) {
                WifiStateMachine.this.handleNetworkDisconnect();
            }
            WifiStateMachine.this.mCountryCode.setReadyForChange(true);
            WifiStateMachine.this.mWifiMetrics.setWifiState(2);
            WifiStateMachine.this.mWifiStateTracker.updateState(2);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            WifiConfiguration config;
            switch (message.what) {
                case WifiStateMachine.CMD_DISCONNECT /*131145*/:
                    WifiStateMachine.this.log("L2ConnectedState, case CMD_DISCONNECT, do disconnect");
                    WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 2);
                    WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    break;
                case WifiStateMachine.CMD_RECONNECT /*131146*/:
                    WifiStateMachine.this.log(" Ignore CMD_RECONNECT request because wifi is already connected");
                    break;
                case WifiStateMachine.CMD_ENABLE_RSSI_POLL /*131154*/:
                    WifiStateMachine.this.cleanWifiScore();
                    WifiStateMachine.this.mEnableRssiPolling = message.arg1 == 1;
                    WifiStateMachine.this.mRssiPollToken = WifiStateMachine.this.mRssiPollToken + 1;
                    if (WifiStateMachine.this.mEnableRssiPolling) {
                        WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0), (long) WifiStateMachine.this.mPollRssiIntervalMsecs);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_RSSI_POLL /*131155*/:
                    if (message.arg1 == WifiStateMachine.this.mRssiPollToken) {
                        WifiStateMachine.this.getWifiLinkLayerStats();
                        WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                        if (!WifiStateMachine.this.reportWifiScoreDelayed()) {
                            WifiStateMachine.this.mWifiScoreReport.calculateAndReportScore(WifiStateMachine.this.mWifiInfo, WifiStateMachine.this.mNetworkAgent, WifiStateMachine.this.mWifiMetrics);
                        }
                        if (WifiStateMachine.this.mWifiScoreReport.shouldCheckIpLayer()) {
                            WifiStateMachine.this.mIpClient.confirmConfiguration();
                            WifiStateMachine.this.mWifiScoreReport.noteIpCheck();
                        }
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_RSSI_POLL, WifiStateMachine.this.mRssiPollToken, 0), (long) WifiStateMachine.this.mPollRssiIntervalMsecs);
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine.this.sendRssiChangeBroadcast(WifiStateMachine.this.mWifiInfo.getRssi());
                            break;
                        }
                    }
                    break;
                case WifiStateMachine.CMD_RESET_SIM_NETWORKS /*131173*/:
                    if (message.arg1 == 0 && WifiStateMachine.this.mLastNetworkId != -1) {
                        config = WifiStateMachine.this.mWifiConfigManager.getConfiguredNetwork(WifiStateMachine.this.mLastNetworkId);
                        if (TelephonyUtil.isSimConfig(config)) {
                            WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 6);
                            WifiStateMachine.this.handleSimAbsent(config);
                            WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                        }
                    }
                    return false;
                case WifiStateMachine.CMD_IP_CONFIGURATION_SUCCESSFUL /*131210*/:
                    WifiStateMachine.this.log("L2ConnectedState, case CMD_IP_CONFIGURATION_SUCCESSFUL");
                    WifiStateMachine.this.handleSuccessfulIpConfiguration();
                    WifiStateMachine.this.reportConnectionAttemptEnd(1, 1);
                    if (WifiStateMachine.this.getCurrentWifiConfiguration() != null) {
                        if (WifiStateMachine.this.isHiLinkActive()) {
                            WifiStateMachine.this.setWifiBackgroundReason(6);
                        }
                        WifiStateMachine.this.notifyIpConfigCompleted();
                        if (!WifiStateMachine.this.ignoreEnterConnectedState()) {
                            if (!WifiStateMachine.this.isWifiProEvaluatingAP()) {
                                if (!WifiStateMachine.this.reportWifiScoreDelayed()) {
                                    WifiStateMachine.this.mWifiScoreReport.calculateAndReportScore(WifiStateMachine.this.mWifiInfo, WifiStateMachine.this.mNetworkAgent, WifiStateMachine.this.mWifiMetrics);
                                }
                                WifiStateMachine.this.sendConnectedState();
                                WifiStateMachine.this.transitionTo(WifiStateMachine.this.mConnectedState);
                                break;
                            }
                            WifiStateMachine.this.log("****WiFi's connected background, don't let Mobile Data down, keep dual networks up.");
                            WifiStateMachine.this.updateNetworkConcurrently();
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mConnectedState);
                            break;
                        }
                    }
                    WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    break;
                    break;
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST /*131211*/:
                    WifiStateMachine.this.log("L2ConnectedState, case CMD_IP_CONFIGURATION_LOST");
                    WifiStateMachine.this.getWifiLinkLayerStats();
                    if (!WifiStateMachine.this.notifyIpConfigLostAndFixedBySce(WifiStateMachine.this.getCurrentWifiConfiguration())) {
                        WifiStateMachine.this.handleIpConfigurationLost();
                        WifiStateMachine.this.reportConnectionAttemptEnd(10, 1);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                        break;
                    }
                    WifiStateMachine.this.log("L2ConnectedState, notifyIpConfigLostAndFixedBySce!!!!");
                    break;
                case WifiStateMachine.CMD_ASSOCIATED_BSSID /*131219*/:
                    if (((String) message.obj) != null) {
                        WifiStateMachine.this.mLastBssid = (String) message.obj;
                        if (WifiStateMachine.this.mLastBssid != null && (WifiStateMachine.this.mWifiInfo.getBSSID() == null || !WifiStateMachine.this.mLastBssid.equals(WifiStateMachine.this.mWifiInfo.getBSSID()))) {
                            WifiStateMachine.this.mWifiInfo.setBSSID(WifiStateMachine.this.mLastBssid);
                            config = WifiStateMachine.this.getCurrentWifiConfiguration();
                            if (config != null) {
                                ScanDetailCache scanDetailCache = WifiStateMachine.this.mWifiConfigManager.getScanDetailCacheForNetwork(config.networkId);
                                if (scanDetailCache != null) {
                                    ScanResult scanResult = scanDetailCache.getScanResult(WifiStateMachine.this.mLastBssid);
                                    if (scanResult != null) {
                                        WifiStateMachine.this.mWifiInfo.setFrequency(scanResult.frequency);
                                    }
                                }
                            }
                            if (!WifiStateMachine.this.isWifiSelfCuring()) {
                                if (WifiStateMachine.this.isWiFiProSwitchOnGoing() && WifiStateMachine.this.getWiFiProRoamingSSID() != null && WifiStateMachine.this.getCurrentState() == WifiStateMachine.this.mRoamingState) {
                                    WifiStateMachine.this.mWifiInfo.setSSID(WifiStateMachine.this.getWiFiProRoamingSSID());
                                }
                                WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                                break;
                            }
                            WifiStateMachine.this.logd("CMD_ASSOCIATED_BSSID, WifiSelfCuring, ignore associated bssid change message.");
                            break;
                        }
                    }
                    WifiStateMachine.this.logw("Associated command w/o BSSID");
                    break;
                case WifiStateMachine.CMD_IP_REACHABILITY_LOST /*131221*/:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled && message.obj != null) {
                        WifiStateMachine.this.log((String) message.obj);
                    }
                    if (!WifiStateMachine.this.mIpReachabilityDisconnectEnabled) {
                        WifiStateMachine.this.logd("CMD_IP_REACHABILITY_LOST but disconnect disabled -- ignore");
                        break;
                    }
                    WifiStateMachine.this.handleIpReachabilityLost();
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                    break;
                case WifiStateMachine.CMD_START_RSSI_MONITORING_OFFLOAD /*131234*/:
                case WifiStateMachine.CMD_RSSI_THRESHOLD_BREACHED /*131236*/:
                    WifiStateMachine.this.processRssiThreshold((byte) message.arg1, message.what, this.mRssiEventHandler);
                    break;
                case WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD /*131235*/:
                    WifiStateMachine.this.stopRssiMonitoringOffload();
                    break;
                case WifiStateMachine.CMD_IPV4_PROVISIONING_SUCCESS /*131272*/:
                    WifiStateMachine.this.handleIPv4Success((DhcpResults) message.obj);
                    WifiStateMachine.this.makeHwDefaultIPTable((DhcpResults) message.obj);
                    WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    break;
                case WifiStateMachine.CMD_IPV4_PROVISIONING_FAILURE /*131273*/:
                    WifiStateMachine.this.mWifiDiagnostics.captureBugReportData(4);
                    if (WifiStateMachine.DBG) {
                        WifiConfiguration config2 = WifiStateMachine.this.getCurrentWifiConfiguration();
                        WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("DHCP failure count=");
                        stringBuilder.append(-1);
                        wifiStateMachine.log(stringBuilder.toString());
                    }
                    WifiStateMachine.this.handleIPv4Failure();
                    break;
                case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST /*143372*/:
                    if (message.arg1 == 1) {
                        WifiStateMachine.this.log("L2ConnectedState, case WifiP2pService.DISCONNECT_WIFI_REQUEST, do disconnect");
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 5);
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.mTemporarilyDisconnectWifi = true;
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectingState);
                        break;
                    }
                    break;
                case WifiMonitor.NETWORK_CONNECTION_EVENT /*147459*/:
                    WifiStateMachine.this.mWifiInfo.setBSSID((String) message.obj);
                    WifiStateMachine.this.mLastNetworkId = message.arg1;
                    WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                    WifiStateMachine.this.mWifiInfo.setMacAddress(WifiStateMachine.this.mWifiNative.getMacAddress(WifiStateMachine.this.mInterfaceName));
                    if (!(WifiStateMachine.this.mLastBssid == null || WifiStateMachine.this.mLastBssid.equals(message.obj))) {
                        WifiStateMachine.this.mLastBssid = (String) message.obj;
                        WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    }
                    WifiStateMachine.this.checkSelfCureWifiResult(103);
                    WifiStateMachine.this.saveWpsOkcConfiguration(WifiStateMachine.this.mLastNetworkId, WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.notifyWifiRoamingCompleted(WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.notifyWlanChannelNumber(WifiCommonUtils.convertFrequencyToChannelNumber(WifiStateMachine.this.mWifiInfo.getFrequency()));
                    WifiStateMachine.this.setLastConnectConfig(WifiStateMachine.this.getCurrentWifiConfiguration());
                    if (WifiStateMachine.ENABLE_DHCP_AFTER_ROAM) {
                        WifiStateMachine.this.log("L2ConnectedState_NETWORK_CONNECTION_EVENT, go to mObtainingIpState");
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                        break;
                    }
                    break;
                case 151553:
                    if (WifiStateMachine.this.mWifiInfo.getNetworkId() == message.arg1) {
                        if (!WifiStateMachine.this.isWifiProEvaluatingAP()) {
                            WifiStateMachine.this.replyToMessage(message, 151555);
                            break;
                        }
                        WifiStateMachine.this.logd("==connection to same network==");
                        return false;
                    }
                    return false;
                case 151572:
                    RssiPacketCountInfo info = new RssiPacketCountInfo();
                    WifiStateMachine.this.fetchRssiLinkSpeedAndFrequencyNative();
                    info.rssi = WifiStateMachine.this.mWifiInfo.getRssi();
                    TxPacketCounters counters = WifiStateMachine.this.mWifiNative.getTxPacketCounters(WifiStateMachine.this.mInterfaceName);
                    if (counters == null) {
                        WifiStateMachine.this.replyToMessage(message, 151574, 0);
                        break;
                    }
                    info.txgood = counters.txSucceeded;
                    info.txbad = counters.txFailed;
                    WifiStateMachine.this.replyToMessage(message, 151573, (Object) info);
                    break;
                case 196611:
                    WifiStateMachine.this.handlePreDhcpSetup();
                    if (WifiStateMachine.this.mHwWifiCHRService != null) {
                        if (DetailedState.OBTAINING_IPADDR != WifiStateMachine.this.mNetworkInfo.getDetailedState()) {
                            if (DetailedState.CONNECTED == WifiStateMachine.this.mNetworkInfo.getDetailedState()) {
                                WifiStateMachine.this.mHwWifiCHRService.updateDhcpState(10);
                                break;
                            }
                        }
                        WifiStateMachine.this.mHwWifiCHRService.updateDhcpState(0);
                        break;
                    }
                    break;
                case 196612:
                    WifiStateMachine.this.handlePostDhcpSetup();
                    break;
                case 196614:
                    WifiStateMachine.this.mIpClient.completedPreDhcpAction();
                    break;
                case 196618:
                    DhcpResults dhcpResults = WifiStateMachine.this.getCachedDhcpResultsForCurrentConfig();
                    if (dhcpResults != null) {
                        WifiStateMachine.this.stopIpClient();
                        dhcpResults.domains = "getCachedDhcpResultsForCurrentConfig";
                        WifiStateMachine.this.mIpClient;
                        WifiStateMachine.this.mIpClient.startProvisioning(IpClient.buildProvisioningConfiguration().withStaticConfiguration(dhcpResults).withoutIpReachabilityMonitor().withApfCapabilities(WifiStateMachine.this.mWifiNative.getApfCapabilities(WifiStateMachine.this.mInterfaceName)).build());
                        break;
                    }
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    class ObtainingIpState extends State {
        ObtainingIpState() {
        }

        public void enter() {
            ProvisioningConfiguration prov;
            WifiConfiguration currentConfig = WifiStateMachine.this.getCurrentWifiConfiguration();
            boolean isUsingStaticIp = false;
            if (currentConfig != null && currentConfig.getIpAssignment() == IpAssignment.STATIC) {
                isUsingStaticIp = true;
            }
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                String key = null;
                if (currentConfig != null) {
                    key = currentConfig.configKey();
                }
                WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enter ObtainingIpState netId=");
                stringBuilder.append(Integer.toString(WifiStateMachine.this.mLastNetworkId));
                stringBuilder.append(" ");
                stringBuilder.append(key);
                stringBuilder.append("  roam=");
                stringBuilder.append(WifiStateMachine.this.mIsAutoRoaming);
                stringBuilder.append(" static=");
                stringBuilder.append(isUsingStaticIp);
                wifiStateMachine.log(stringBuilder.toString());
            }
            WifiStateMachine.this.setNetworkDetailedState(DetailedState.OBTAINING_IPADDR);
            WifiStateMachine.this.clearTargetBssid("ObtainingIpAddress");
            WifiStateMachine.this.stopIpClient();
            WifiStateMachine.this.mIpClient.setHttpProxy(WifiStateMachine.this.getProxyProperties());
            if (!TextUtils.isEmpty(WifiStateMachine.this.mTcpBufferSizes)) {
                WifiStateMachine.this.mIpClient.setTcpBufferSizes(WifiStateMachine.this.mTcpBufferSizes);
            }
            WifiStateMachine.this.tryUseStaticIpForFastConnecting(WifiStateMachine.this.mLastNetworkId);
            if (isUsingStaticIp) {
                ProvisioningConfiguration prov2 = IpClient.buildProvisioningConfiguration().withStaticConfiguration(currentConfig.getStaticIpConfiguration()).withoutIpReachabilityMonitor().withApfCapabilities(WifiStateMachine.this.mWifiNative.getApfCapabilities(WifiStateMachine.this.mInterfaceName)).withNetwork(WifiStateMachine.this.getCurrentNetwork()).withDisplayName(currentConfig.SSID).build();
                if (WifiStateMachine.this.mHwWifiCHRService != null) {
                    WifiStateMachine.this.mHwWifiCHRService.updateDhcpState(8);
                }
                prov = prov2;
            } else {
                prov = IpClient.buildProvisioningConfiguration().withPreDhcpAction().withoutIpReachabilityMonitor().withApfCapabilities(WifiStateMachine.this.mWifiNative.getApfCapabilities(WifiStateMachine.this.mInterfaceName)).withNetwork(WifiStateMachine.this.getCurrentNetwork()).withDisplayName(currentConfig != null ? currentConfig.SSID : "").withRandomMacAddress().build();
                WifiStateMachine.this.mIpClient.putPendingSSID(WifiStateMachine.this.mWifiInfo.getBSSID());
                WifiStateMachine.this.setForceDhcpDiscovery(WifiStateMachine.this.mIpClient);
            }
            WifiStateMachine.this.mIpClient.startProvisioning(prov);
            WifiStateMachine.this.getWifiLinkLayerStats();
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_SET_HIGH_PERF_MODE /*131149*/:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiStateMachine.CMD_START_CONNECT /*131215*/:
                case WifiStateMachine.CMD_START_ROAM /*131217*/:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    break;
                case WifiStateMachine.SCE_REQUEST_SET_STATIC_IP /*131884*/:
                    WifiStateMachine.this.logd("handle WifiStateMachine: SCE_REQUEST_SET_STATIC_IP.");
                    WifiStateMachine.this.stopIpClient();
                    WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.SCE_START_SET_STATIC_IP, message.obj, 1000);
                    break;
                case WifiStateMachine.SCE_START_SET_STATIC_IP /*131885*/:
                    WifiStateMachine.this.logd("handle WifiStateMachine: SCE_START_SET_STATIC_IP.");
                    WifiStateMachine.this.handleStaticIpConfig(WifiStateMachine.this.mIpClient, WifiStateMachine.this.mWifiNative, (StaticIpConfiguration) message.obj);
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                    WifiStateMachine.this.reportConnectionAttemptEnd(6, 1);
                    return false;
                case 151559:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DEFERRED;
                    WifiStateMachine.this.deferMessage(message);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    class RoamingState extends State {
        boolean mAssociated;

        RoamingState() {
        }

        public void enter() {
            WifiStateMachine wifiStateMachine;
            StringBuilder stringBuilder;
            if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                wifiStateMachine = WifiStateMachine.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("RoamingState Enter mScreenOn=");
                stringBuilder.append(WifiStateMachine.this.mScreenOn);
                wifiStateMachine.log(stringBuilder.toString());
            }
            WifiStateMachine.this.enterConnectedStateByMode();
            wifiStateMachine = WifiStateMachine.this;
            wifiStateMachine.roamWatchdogCount++;
            wifiStateMachine = WifiStateMachine.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Start Roam Watchdog ");
            stringBuilder.append(WifiStateMachine.this.roamWatchdogCount);
            wifiStateMachine.logd(stringBuilder.toString());
            WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.this.obtainMessage(WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER, WifiStateMachine.this.roamWatchdogCount, 0), 15000);
            this.mAssociated = false;
            WifiStateMachine.this.setWiFiProRoamingSSID(null);
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_ROAM_WATCHDOG_TIMER /*131166*/:
                    if (WifiStateMachine.this.roamWatchdogCount == message.arg1) {
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine.this.log("roaming watchdog! -> disconnect");
                        }
                        WifiStateMachine.this.mWifiMetrics.endConnectionEvent(9, 1);
                        WifiStateMachine.this.mRoamFailCount = WifiStateMachine.this.mRoamFailCount + 1;
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.mWifiMetrics.logStaEvent(15, 4);
                        WifiStateMachine.this.mWifiNative.disconnect(WifiStateMachine.this.mInterfaceName);
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_IP_CONFIGURATION_LOST /*131211*/:
                    if (WifiStateMachine.this.getCurrentWifiConfiguration() != null) {
                        WifiStateMachine.this.mWifiDiagnostics.captureBugReportData(3);
                    }
                    return false;
                case WifiStateMachine.CMD_UNWANTED_NETWORK /*131216*/:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("Roaming and CS doesnt want the network -> ignore");
                    }
                    return true;
                case WifiMonitor.NETWORK_CONNECTION_EVENT /*147459*/:
                    if (!this.mAssociated) {
                        WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                        break;
                    }
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("roaming and Network connection established");
                    }
                    WifiStateMachine.this.mLastNetworkId = message.arg1;
                    WifiStateMachine.this.mLastBssid = (String) message.obj;
                    WifiStateMachine.this.saveWpsOkcConfiguration(WifiStateMachine.this.mLastNetworkId, WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.mWifiInfo.setBSSID(WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.mWifiInfo.setNetworkId(WifiStateMachine.this.mLastNetworkId);
                    WifiStateMachine.this.mWifiConnectivityManager.trackBssid(WifiStateMachine.this.mLastBssid, true, message.arg2);
                    WifiStateMachine.this.sendNetworkStateChangeBroadcast(WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.reportConnectionAttemptEnd(1, 1);
                    WifiStateMachine.this.clearTargetBssid("RoamingCompleted");
                    WifiStateMachine.this.notifyWifiRoamingCompleted(WifiStateMachine.this.mLastBssid);
                    WifiStateMachine.this.setLastConnectConfig(WifiStateMachine.this.getCurrentWifiConfiguration());
                    if (!WifiStateMachine.ENABLE_DHCP_AFTER_ROAM) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mConnectedState);
                        break;
                    }
                    WifiStateMachine.this.log("RoamingState_NETWORK_CONNECTION_EVENT, go to mObtainingIpState");
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mObtainingIpState);
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                    String bssid = message.obj;
                    String target = "";
                    if (WifiStateMachine.this.mTargetRoamBSSID != null) {
                        target = WifiStateMachine.this.mTargetRoamBSSID;
                    }
                    WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("NETWORK_DISCONNECTION_EVENT in roaming state BSSID=");
                    stringBuilder.append(StringUtil.safeDisplayBssid(bssid));
                    stringBuilder.append(" target=");
                    stringBuilder.append(target);
                    wifiStateMachine.log(stringBuilder.toString());
                    if (message.arg2 == 15 || message.arg2 == 2) {
                        WifiStateMachine.this.handleDualbandHandoverFailed(3);
                    }
                    if (bssid != null && bssid.equals(WifiStateMachine.this.mTargetRoamBSSID)) {
                        WifiStateMachine.this.handleNetworkDisconnect();
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                        break;
                    }
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT /*147462*/:
                    StateChangeResult stateChangeResult = message.obj;
                    if (stateChangeResult.state == SupplicantState.DISCONNECTED || stateChangeResult.state == SupplicantState.INACTIVE || stateChangeResult.state == SupplicantState.INTERFACE_DISABLED) {
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine wifiStateMachine2 = WifiStateMachine.this;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("STATE_CHANGE_EVENT in roaming state ");
                            stringBuilder2.append(stateChangeResult.toString());
                            wifiStateMachine2.log(stringBuilder2.toString());
                        }
                        if (stateChangeResult.BSSID != null && stateChangeResult.BSSID.equals(WifiStateMachine.this.mTargetRoamBSSID)) {
                            WifiStateMachine.this.handleNetworkDisconnect();
                            WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                        }
                    }
                    if (stateChangeResult.state == SupplicantState.ASSOCIATED) {
                        this.mAssociated = true;
                        if (stateChangeResult.BSSID != null) {
                            WifiStateMachine.this.mTargetRoamBSSID = stateChangeResult.BSSID;
                        }
                        WifiStateMachine.this.notifyWifiRoamingStarted();
                        WifiStateMachine.this.setWiFiProRoamingSSID(stateChangeResult.wifiSsid);
                        break;
                    }
                    break;
                default:
                    return false;
            }
            return true;
        }

        public void exit() {
            WifiStateMachine.this.logd("WifiStateMachine: Leaving Roaming state");
            WifiStateMachine.this.setWiFiProRoamingSSID(null);
        }
    }

    private class UntrustedWifiNetworkFactory extends NetworkFactory {
        public UntrustedWifiNetworkFactory(Looper l, Context c, String tag, NetworkCapabilities f) {
            super(l, c, tag, f);
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            if (!networkRequest.networkCapabilities.hasCapability(14)) {
                synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                    if (WifiStateMachine.access$1904(WifiStateMachine.this) == 1 && WifiStateMachine.this.mWifiConnectivityManager != null) {
                        if (WifiStateMachine.this.mConnectionReqCount == 0) {
                            WifiStateMachine.this.mWifiConnectivityManager.enable(true);
                        }
                        WifiStateMachine.this.mWifiConnectivityManager.setUntrustedConnectionAllowed(true);
                    }
                }
            }
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            if (!networkRequest.networkCapabilities.hasCapability(14)) {
                synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                    if (WifiStateMachine.access$1906(WifiStateMachine.this) == 0 && WifiStateMachine.this.mWifiConnectivityManager != null) {
                        WifiStateMachine.this.mWifiConnectivityManager.setUntrustedConnectionAllowed(false);
                        if (WifiStateMachine.this.mConnectionReqCount == 0) {
                            WifiStateMachine.this.mWifiConnectivityManager.enable(false);
                        }
                    }
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mUntrustedReqCount ");
            stringBuilder.append(WifiStateMachine.this.mUntrustedReqCount);
            pw.println(stringBuilder.toString());
        }
    }

    private class WifiNetworkAgent extends HwNetworkAgent {
        private int mLastNetworkStatus = -1;
        final /* synthetic */ WifiStateMachine this$0;

        public WifiNetworkAgent(WifiStateMachine wifiStateMachine, Looper l, Context c, String TAG, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score, NetworkMisc misc) {
            this.this$0 = wifiStateMachine;
            super(l, c, TAG, ni, nc, lp, score, misc);
        }

        protected void unwanted() {
            if (this == this.this$0.mNetworkAgent) {
                if (this.this$0.mVerboseLoggingEnabled) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("WifiNetworkAgent -> Wifi unwanted score ");
                    stringBuilder.append(Integer.toString(this.this$0.mWifiInfo.score));
                    log(stringBuilder.toString());
                }
                this.this$0.unwantedNetwork(0);
            }
        }

        protected void networkStatus(int status, String redirectUrl) {
            if (this == this.this$0.mNetworkAgent && status != this.mLastNetworkStatus) {
                this.mLastNetworkStatus = status;
                StringBuilder stringBuilder;
                if (status == 2) {
                    if (this.this$0.mVerboseLoggingEnabled) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("WifiNetworkAgent -> Wifi networkStatus invalid, score=");
                        stringBuilder.append(Integer.toString(this.this$0.mWifiInfo.score));
                        log(stringBuilder.toString());
                    }
                    this.this$0.unwantedNetwork(1);
                } else if (status == 1) {
                    if (this.this$0.mVerboseLoggingEnabled) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("WifiNetworkAgent -> Wifi networkStatus valid, score= ");
                        stringBuilder.append(Integer.toString(this.this$0.mWifiInfo.score));
                        log(stringBuilder.toString());
                    }
                    this.this$0.mWifiMetrics.logStaEvent(14);
                    this.this$0.doNetworkStatus(status);
                } else if (status == 3) {
                    this.this$0.reportPortalNetworkStatus();
                } else if (status == 4) {
                    this.this$0.notifyWifiConnectedBackgroundReady();
                }
            }
        }

        protected void saveAcceptUnvalidated(boolean accept) {
            if (this == this.this$0.mNetworkAgent) {
                this.this$0.sendMessage(WifiStateMachine.CMD_ACCEPT_UNVALIDATED, accept);
            }
        }

        protected void startPacketKeepalive(Message msg) {
            this.this$0.sendMessage(WifiStateMachine.CMD_START_IP_PACKET_OFFLOAD, msg.arg1, msg.arg2, msg.obj);
        }

        protected void stopPacketKeepalive(Message msg) {
            this.this$0.sendMessage(WifiStateMachine.CMD_STOP_IP_PACKET_OFFLOAD, msg.arg1, msg.arg2, msg.obj);
        }

        protected void setSignalStrengthThresholds(int[] thresholds) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received signal strength thresholds: ");
            stringBuilder.append(Arrays.toString(thresholds));
            log(stringBuilder.toString());
            if (thresholds.length == 0) {
                this.this$0.sendMessage(WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD, this.this$0.mWifiInfo.getRssi());
                return;
            }
            int[] rssiVals = Arrays.copyOf(thresholds, thresholds.length + 2);
            rssiVals[rssiVals.length + WifiStateMachine.MESSAGE_HANDLING_STATUS_FAIL] = -128;
            rssiVals[rssiVals.length - 1] = 127;
            Arrays.sort(rssiVals);
            byte[] rssiRange = new byte[rssiVals.length];
            for (int i = 0; i < rssiVals.length; i++) {
                int val = rssiVals[i];
                if (val > 127 || val < -128) {
                    String str = WifiStateMachine.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Illegal value ");
                    stringBuilder2.append(val);
                    stringBuilder2.append(" for RSSI thresholds: ");
                    stringBuilder2.append(Arrays.toString(rssiVals));
                    Log.e(str, stringBuilder2.toString());
                    this.this$0.sendMessage(WifiStateMachine.CMD_STOP_RSSI_MONITORING_OFFLOAD, this.this$0.mWifiInfo.getRssi());
                    return;
                }
                rssiRange[i] = (byte) val;
            }
            this.this$0.mRssiRanges = rssiRange;
            this.this$0.sendMessage(WifiStateMachine.CMD_START_RSSI_MONITORING_OFFLOAD, this.this$0.mWifiInfo.getRssi());
        }

        protected void preventAutomaticReconnect() {
            if (this == this.this$0.mNetworkAgent) {
                this.this$0.unwantedNetwork(2);
            }
        }
    }

    private class WifiNetworkFactory extends NetworkFactory {
        public WifiNetworkFactory(Looper l, Context c, String TAG, NetworkCapabilities f) {
            super(l, c, TAG, f);
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                if (WifiStateMachine.access$1804(WifiStateMachine.this) == 1 && WifiStateMachine.this.mWifiConnectivityManager != null && WifiStateMachine.this.mUntrustedReqCount == 0) {
                    WifiStateMachine.this.mWifiConnectivityManager.enable(true);
                }
            }
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            synchronized (WifiStateMachine.this.mWifiReqCountLock) {
                if (WifiStateMachine.access$1806(WifiStateMachine.this) == 0 && WifiStateMachine.this.mWifiConnectivityManager != null && WifiStateMachine.this.mUntrustedReqCount == 0) {
                    WifiStateMachine.this.mWifiConnectivityManager.enable(false);
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mConnectionReqCount ");
            stringBuilder.append(WifiStateMachine.this.mConnectionReqCount);
            pw.println(stringBuilder.toString());
        }
    }

    class WpsRunningState extends State {
        private Message mSourceMessage;

        WpsRunningState() {
        }

        public void enter() {
            if (WifiStateMachine.DBG) {
                WifiStateMachine.this.log(getName());
            }
            this.mSourceMessage = Message.obtain(WifiStateMachine.this.getCurrentMessage());
        }

        public boolean processMessage(Message message) {
            WifiStateMachine.this.logStateAndMessage(message, this);
            switch (message.what) {
                case WifiStateMachine.CMD_ENABLE_NETWORK /*131126*/:
                case WifiStateMachine.CMD_RECONNECT /*131146*/:
                case 151553:
                    WifiStateMachine.this.log(" Ignore CMD_RECONNECT request because wps is running");
                    return true;
                case WifiStateMachine.CMD_SET_OPERATIONAL_MODE /*131144*/:
                    WifiStateMachine.this.mOperationalMode = message.arg1;
                    if (WifiStateMachine.this.mOperationalMode == 3) {
                        WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDefaultState);
                        break;
                    }
                    break;
                case WifiStateMachine.CMD_REASSOCIATE /*131147*/:
                    WifiStateMachine.this.deferMessage(message);
                    break;
                case WifiStateMachine.CMD_START_CONNECT /*131215*/:
                case WifiStateMachine.CMD_START_ROAM /*131217*/:
                    WifiStateMachine.this.messageHandlingStatus = WifiStateMachine.MESSAGE_HANDLING_STATUS_DISCARD;
                    return true;
                case WifiStateMachine.CMD_WPS_PIN_RETRY /*131576*/:
                    WpsResult wpsResult = message.obj;
                    if (!TextUtils.isEmpty(wpsResult.pin)) {
                        WifiStateMachine.this.mWifiNative.startWpsPinKeypad(WifiStateMachine.this.mInterfaceName, wpsResult.pin);
                        WifiStateMachine.this.sendMessageDelayed(WifiStateMachine.CMD_WPS_PIN_RETRY, wpsResult, 50000);
                        break;
                    }
                    break;
                case WifiMonitor.NETWORK_CONNECTION_EVENT /*147459*/:
                    WifiStateMachine.this.removeMessages(WifiStateMachine.CMD_WPS_PIN_RETRY);
                    Pair<Boolean, Integer> loadResult = loadNetworksFromSupplicantAfterWps();
                    boolean success = ((Boolean) loadResult.first).booleanValue();
                    int netId = ((Integer) loadResult.second).intValue();
                    if (success) {
                        message.arg1 = netId;
                        WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151565);
                    } else {
                        WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151564, 0);
                    }
                    this.mSourceMessage.recycle();
                    this.mSourceMessage = null;
                    WifiStateMachine.this.deferMessage(message);
                    WifiStateMachine.this.saveWpsNetIdInWifiPro(message.arg1);
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("Network connection lost");
                    }
                    WifiStateMachine.this.handleNetworkDisconnect();
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT /*147462*/:
                    return false;
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT /*147463*/:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("Ignore auth failure during WPS connection");
                        break;
                    }
                    break;
                case WifiMonitor.WPS_SUCCESS_EVENT /*147464*/:
                    break;
                case WifiMonitor.WPS_FAIL_EVENT /*147465*/:
                    if (message.arg1 == 0 && message.arg2 == 0) {
                        if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                            WifiStateMachine.this.log("Ignore unspecified fail event during WPS connection");
                            break;
                        }
                    }
                    WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151564, message.arg1);
                    this.mSourceMessage.recycle();
                    this.mSourceMessage = null;
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    break;
                    break;
                case WifiMonitor.WPS_OVERLAP_EVENT /*147466*/:
                    WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151564, 3);
                    this.mSourceMessage.recycle();
                    this.mSourceMessage = null;
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    break;
                case WifiMonitor.WPS_TIMEOUT_EVENT /*147467*/:
                    WifiStateMachine.this.removeMessages(WifiStateMachine.CMD_WPS_PIN_RETRY);
                    WifiStateMachine.this.replyToMessage(this.mSourceMessage, 151564, 7);
                    this.mSourceMessage.recycle();
                    this.mSourceMessage = null;
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    break;
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT /*147499*/:
                    if (WifiStateMachine.this.mVerboseLoggingEnabled) {
                        WifiStateMachine.this.log("Ignore Assoc reject event during WPS Connection");
                        break;
                    }
                    break;
                case 151562:
                    WifiStateMachine.this.replyToMessage(message, 151564, 1);
                    break;
                case 151566:
                    WifiStateMachine.this.removeMessages(WifiStateMachine.CMD_WPS_PIN_RETRY);
                    if (WifiStateMachine.this.mWifiNative.cancelWps(WifiStateMachine.this.mInterfaceName)) {
                        WifiStateMachine.this.replyToMessage(message, 151568);
                    } else {
                        WifiStateMachine.this.replyToMessage(message, 151567, 0);
                    }
                    WifiStateMachine.this.transitionTo(WifiStateMachine.this.mDisconnectedState);
                    break;
                default:
                    return false;
            }
            return true;
        }

        public void exit() {
            if (WifiStateMachine.this.mIsRandomMacCleared) {
                WifiStateMachine.this.setRandomMacOui();
                WifiStateMachine.this.mIsRandomMacCleared = false;
            }
        }

        private Pair<Boolean, Integer> loadNetworksFromSupplicantAfterWps() {
            Map<String, WifiConfiguration> configs = new HashMap();
            int netId = -1;
            int i = -1;
            if (WifiStateMachine.this.mWifiNative.migrateNetworksFromSupplicant(WifiStateMachine.this.mInterfaceName, configs, new SparseArray())) {
                for (Entry<String, WifiConfiguration> entry : configs.entrySet()) {
                    WifiConfiguration config = (WifiConfiguration) entry.getValue();
                    config.networkId = -1;
                    NetworkUpdateResult result = WifiStateMachine.this.mWifiConfigManager.addOrUpdateNetwork(config, this.mSourceMessage.sendingUid);
                    StringBuilder stringBuilder;
                    if (!result.isSuccess()) {
                        WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to add network after WPS: ");
                        stringBuilder.append(entry.getValue());
                        wifiStateMachine.loge(stringBuilder.toString());
                        return Pair.create(Boolean.valueOf(false), Integer.valueOf(-1));
                    } else if (WifiStateMachine.this.mWifiConfigManager.enableNetwork(result.getNetworkId(), true, this.mSourceMessage.sendingUid)) {
                        netId = result.getNetworkId();
                    } else {
                        String str = WifiStateMachine.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to enable network after WPS: ");
                        stringBuilder.append(entry.getValue());
                        Log.wtf(str, stringBuilder.toString());
                        return Pair.create(Boolean.valueOf(false), Integer.valueOf(-1));
                    }
                }
                Boolean valueOf = Boolean.valueOf(true);
                if (configs.size() == 1) {
                    i = netId;
                }
                return Pair.create(valueOf, Integer.valueOf(i));
            }
            WifiStateMachine.this.loge("Failed to load networks from wpa_supplicant after Wps");
            return Pair.create(Boolean.valueOf(false), Integer.valueOf(-1));
        }
    }

    class McastLockManagerFilterController implements FilterController {
        McastLockManagerFilterController() {
        }

        public void startFilteringMulticastPackets() {
            if (WifiStateMachine.this.mIpClient != null) {
                WifiStateMachine.this.mIpClient.setMulticastFilter(true);
            }
        }

        public void stopFilteringMulticastPackets() {
            if (WifiStateMachine.this.mIpClient != null) {
                WifiStateMachine.this.mIpClient.setMulticastFilter(false);
            }
        }
    }

    static /* synthetic */ int access$1804(WifiStateMachine x0) {
        int i = x0.mConnectionReqCount + 1;
        x0.mConnectionReqCount = i;
        return i;
    }

    static /* synthetic */ int access$1806(WifiStateMachine x0) {
        int i = x0.mConnectionReqCount - 1;
        x0.mConnectionReqCount = i;
        return i;
    }

    static /* synthetic */ int access$1904(WifiStateMachine x0) {
        int i = x0.mUntrustedReqCount + 1;
        x0.mUntrustedReqCount = i;
        return i;
    }

    static /* synthetic */ int access$1906(WifiStateMachine x0) {
        int i = x0.mUntrustedReqCount - 1;
        x0.mUntrustedReqCount = i;
        return i;
    }

    private HwMSSHandlerManager checkAndGetHwMSSHandlerManager() {
        if (this.mHwMssHandler == null) {
            this.mHwMssHandler = HwWifiServiceFactory.getHwMSSHandlerManager(this.mContext, this.mWifiNative, this.mWifiInfo);
        }
        return this.mHwMssHandler;
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    protected void loge(String s) {
        Log.e(getName(), s);
    }

    protected void logd(String s) {
        Log.d(getName(), s);
    }

    protected void log(String s) {
        Log.d(getName(), s);
    }

    public WifiScoreReport getWifiScoreReport() {
        return this.mWifiScoreReport;
    }

    private void processRssiThreshold(byte curRssi, int reason, WifiRssiEventHandler rssiHandler) {
        if (curRssi == Byte.MAX_VALUE || curRssi == Byte.MIN_VALUE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processRssiThreshold: Invalid rssi ");
            stringBuilder.append(curRssi);
            Log.wtf(str, stringBuilder.toString());
            return;
        }
        for (int i = 0; i < this.mRssiRanges.length; i++) {
            if (curRssi < this.mRssiRanges[i]) {
                byte maxRssi = this.mRssiRanges[i];
                byte minRssi = this.mRssiRanges[i - 1];
                this.mWifiInfo.setRssi(curRssi);
                updateCapabilities();
                int ret = startRssiMonitoringOffload(maxRssi, minRssi, rssiHandler);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Re-program RSSI thresholds for ");
                stringBuilder2.append(smToString(reason));
                stringBuilder2.append(": [");
                stringBuilder2.append(minRssi);
                stringBuilder2.append(", ");
                stringBuilder2.append(maxRssi);
                stringBuilder2.append("], curRssi=");
                stringBuilder2.append(curRssi);
                stringBuilder2.append(" ret=");
                stringBuilder2.append(ret);
                Log.d(str2, stringBuilder2.toString());
                break;
            }
        }
    }

    int getPollRssiIntervalMsecs() {
        return this.mPollRssiIntervalMsecs;
    }

    void setPollRssiIntervalMsecs(int newPollIntervalMsecs) {
        this.mPollRssiIntervalMsecs = newPollIntervalMsecs;
    }

    public boolean clearTargetBssid(String dbg) {
        WifiConfiguration config = this.mWifiConfigManager.getConfiguredNetwork(this.mTargetNetworkId);
        if (config == null) {
            return false;
        }
        String bssid = "any";
        if (config.BSSID != null) {
            bssid = config.BSSID;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("force BSSID to ");
            stringBuilder.append(bssid);
            stringBuilder.append("due to config");
            Log.d(str, stringBuilder.toString());
        }
        if (this.mVerboseLoggingEnabled) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(dbg);
            stringBuilder2.append(" clearTargetBssid ");
            stringBuilder2.append(bssid);
            stringBuilder2.append(" key=");
            stringBuilder2.append(config.configKey());
            logd(stringBuilder2.toString());
        }
        this.mTargetRoamBSSID = bssid;
        return this.mWifiNative.setConfiguredNetworkBSSID(this.mInterfaceName, "any");
    }

    private boolean setTargetBssid(WifiConfiguration config, String bssid) {
        if (config == null || bssid == null) {
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        if (config.BSSID != null) {
            bssid = config.BSSID;
            if (this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("force BSSID to ");
                stringBuilder.append(bssid);
                stringBuilder.append("due to config");
                Log.d(str, stringBuilder.toString());
            }
        }
        if (this.mVerboseLoggingEnabled) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setTargetBssid set to ");
            stringBuilder.append(bssid);
            stringBuilder.append(" key=");
            stringBuilder.append(config.configKey());
            Log.d(str, stringBuilder.toString());
        }
        this.mTargetRoamBSSID = bssid;
        config.getNetworkSelectionStatus().setNetworkSelectionBSSID(bssid);
        return true;
    }

    private TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = this.mWifiInjector.makeTelephonyManager();
        }
        return this.mTelephonyManager;
    }

    public WifiStateMachine(Context context, FrameworkFacade facade, Looper looper, UserManager userManager, WifiInjector wifiInjector, BackupManagerProxy backupManagerProxy, WifiCountryCode countryCode, WifiNative wifiNative, WrongPasswordNotifier wrongPasswordNotifier, SarManager sarManager) {
        Context context2 = context;
        super(TAG, looper);
        this.mWifiInjector = wifiInjector;
        this.mWifiMetrics = this.mWifiInjector.getWifiMetrics();
        this.mClock = wifiInjector.getClock();
        this.mPropertyService = wifiInjector.getPropertyService();
        this.mBuildProperties = wifiInjector.getBuildProperties();
        this.mContext = context2;
        this.mFacade = facade;
        this.mWifiNative = wifiNative;
        this.mBackupManagerProxy = backupManagerProxy;
        this.mWrongPasswordNotifier = wrongPasswordNotifier;
        this.mSarManager = sarManager;
        this.mNetworkInfo = new NetworkInfo(1, 0, NETWORKTYPE, "");
        this.mBatteryStats = Stub.asInterface(this.mFacade.getService("batterystats"));
        this.mWifiStateTracker = wifiInjector.getWifiStateTracker();
        this.mNwService = INetworkManagementService.Stub.asInterface(this.mFacade.getService("network_management"));
        this.mP2pSupported = this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
        this.mWifiPermissionsUtil = this.mWifiInjector.getWifiPermissionsUtil();
        this.mWifiConfigManager = this.mWifiInjector.getWifiConfigManager();
        this.mPasspointManager = this.mWifiInjector.getPasspointManager();
        this.mWifiMonitor = this.mWifiInjector.getWifiMonitor();
        this.mWifiDiagnostics = this.mWifiInjector.getWifiDiagnostics();
        this.mScanRequestProxy = this.mWifiInjector.getScanRequestProxy();
        this.mWifiPermissionsWrapper = this.mWifiInjector.getWifiPermissionsWrapper();
        this.mWifiInfo = new ExtendedWifiInfo();
        this.mSupplicantStateTracker = this.mFacade.makeSupplicantStateTracker(context2, this.mWifiConfigManager, getHandler());
        this.mLinkProperties = new LinkProperties();
        this.mMcastLockManagerFilterController = new McastLockManagerFilterController();
        if (WifiStateMachineHisiExt.hisiWifiEnabled()) {
            this.mWifiStateMachineHisiExt = new WifiStateMachineHisiExt(this.mContext, this.mWifiConfigManager, this.mWifiState, this.mWifiApState);
        }
        this.uploader = DataUploader.getInstance();
        this.mNetworkInfo.setIsAvailable(false);
        this.mLastBssid = null;
        this.mLastNetworkId = -1;
        this.mLastSignalLevel = -1;
        this.mPrimaryDeviceType = this.mContext.getResources().getString(17039849);
        this.mCountryCode = countryCode;
        this.mWifiScoreReport = new WifiScoreReport(this.mWifiInjector.getScoringParams(), this.mClock);
        this.mNetworkCapabilitiesFilter.addTransportType(1);
        this.mNetworkCapabilitiesFilter.addCapability(12);
        this.mNetworkCapabilitiesFilter.addCapability(11);
        this.mNetworkCapabilitiesFilter.addCapability(18);
        this.mNetworkCapabilitiesFilter.addCapability(20);
        this.mNetworkCapabilitiesFilter.addCapability(13);
        this.mNetworkCapabilitiesFilter.setLinkUpstreamBandwidthKbps(1048576);
        this.mNetworkCapabilitiesFilter.setLinkDownstreamBandwidthKbps(1048576);
        this.mDfltNetworkCapabilities = new NetworkCapabilities(this.mNetworkCapabilitiesFilter);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiStateMachine.VDBG) {
                    WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("receive action: ");
                    stringBuilder.append(action);
                    wifiStateMachine.log(stringBuilder.toString());
                }
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SCREEN_STATE_CHANGED, 1);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_SCREEN_STATE_CHANGED, 0);
                }
            }
        }, filter);
        this.mFacade.registerContentObserver(this.mContext, Global.getUriFor("wifi_suspend_optimizations_enabled"), false, new ContentObserver(getHandler()) {
            public void onChange(boolean selfChange) {
                AtomicBoolean access$300 = WifiStateMachine.this.mUserWantsSuspendOpt;
                boolean z = true;
                if (WifiStateMachine.this.mFacade.getIntegerSetting(WifiStateMachine.this.mContext, "wifi_suspend_optimizations_enabled", 1) != 1) {
                    z = false;
                }
                access$300.set(z);
            }
        });
        this.mFacade.registerContentObserver(this.mContext, Global.getUriFor("wifi_connected_mac_randomization_enabled"), false, new ContentObserver(getHandler()) {
            public void onChange(boolean selfChange) {
                WifiStateMachine.this.updateConnectedMacRandomizationSetting();
            }
        });
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("smart_network_switching"), false, new ContentObserver(getHandler()) {
            public void onChange(boolean selfChange) {
            }
        });
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                WifiStateMachine.this.sendMessage(WifiStateMachine.CMD_BOOT_COMPLETED);
            }
        }, new IntentFilter("android.intent.action.LOCKED_BOOT_COMPLETED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean z = false;
                WifiStateMachine.this.mIsRunning = false;
                WifiStateMachine wifiStateMachine = WifiStateMachine.this;
                if (getSendingUserId() == -1) {
                    z = true;
                }
                wifiStateMachine.mIsRealReboot = z;
                String str = WifiStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive: mIsRealReboot = ");
                stringBuilder.append(WifiStateMachine.this.mIsRealReboot);
                Log.d(str, stringBuilder.toString());
                if (WifiStateMachine.DBG) {
                    WifiStateMachine.this.log("shut down so update battery");
                }
                WifiStateMachine.this.updateBatteryWorkSource(null);
            }
        }, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"));
        this.mUserWantsSuspendOpt.set(this.mFacade.getIntegerSetting(this.mContext, "wifi_suspend_optimizations_enabled", 1) == 1);
        updateConnectedMacRandomizationSetting();
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, getName());
        this.mSuspendWakeLock = powerManager.newWakeLock(1, "WifiSuspend");
        this.mSuspendWakeLock.setReferenceCounted(false);
        this.mTcpBufferSizes = this.mContext.getResources().getString(17039851);
        addState(this.mDefaultState);
        addState(this.mConnectModeState, this.mDefaultState);
        addState(this.mL2ConnectedState, this.mConnectModeState);
        addState(this.mObtainingIpState, this.mL2ConnectedState);
        addState(this.mConnectedState, this.mL2ConnectedState);
        addState(this.mRoamingState, this.mL2ConnectedState);
        addState(this.mWpsRunningState, this.mConnectModeState);
        addState(this.mDisconnectingState, this.mConnectModeState);
        addState(this.mDisconnectedState, this.mConnectModeState);
        setInitialState(this.mDefaultState);
        if (!ActivityManager.isLowRamDeviceStatic()) {
            boolean z = SystemProperties.getBoolean("ro.config.hw_low_ram", false);
        }
        setLogRecSize(100);
        setLogOnlyTransitions(false);
        start();
        handleScreenStateChanged(powerManager.isInteractive());
        this.mHwWifiCHRService = HwWifiServiceFactory.getHwWifiCHRService();
        HwWifiServiceFactory.getHwWifiDevicePolicy().registerBroadcasts(this.mContext);
    }

    private void registerForWifiMonitorEvents() {
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_TARGET_BSSID, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_ASSOCIATED_BSSID, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.ANQP_DONE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.ASSOCIATION_REJECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.AUTHENTICATION_FAILURE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.GAS_QUERY_DONE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.GAS_QUERY_START_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.HS20_REMEDIATION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_CONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_DISCONNECTION_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.RX_HS20_ANQP_ICON_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WPS_SUCCESS_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WPS_FAIL_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WPS_OVERLAP_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WPS_TIMEOUT_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_REQUEST_IDENTITY, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUP_REQUEST_SIM_AUTH, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.ASSOCIATION_REJECTION_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.AUTHENTICATION_FAILURE_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_CONNECTION_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.NETWORK_DISCONNECTION_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_ASSOCIATED_BSSID, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, CMD_TARGET_BSSID, this.mWifiMetrics.getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.WPS_START_OKC_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.VOWIFI_DETECT_IRQ_STR_EVENT, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.EVENT_ANT_CORE_ROB, getHandler());
        this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiMonitor.EAP_ERRORCODE_REPORT_EVENT, getHandler());
    }

    private boolean mactchResultAndConfigSecurity(ScanResult scanResult, WifiConfiguration config) {
        boolean z = true;
        if (ScanResultUtil.isScanResultForWapiPskNetwork(scanResult)) {
            Log.d(TAG, "isScanResultForWapiPskNetwork");
            if (!(config.allowedKeyManagement.get(8) || config.allowedKeyManagement.get(10))) {
                z = false;
            }
            return z;
        } else if (ScanResultUtil.isScanResultForCertNetwork(scanResult)) {
            Log.d(TAG, "isScanResultForCertNetwork");
            if (!(config.allowedKeyManagement.get(9) || config.allowedKeyManagement.get(11))) {
                z = false;
            }
            return z;
        } else if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
            Log.d(TAG, "isScanResultForEapNetwork");
            if (!(config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3) || config.allowedKeyManagement.get(7))) {
                z = false;
            }
            return z;
        } else if (ScanResultUtil.isScanResultForPskNetwork(scanResult)) {
            Log.d(TAG, "isScanResultForPskNetwork");
            if (!(config.allowedKeyManagement.get(1) || config.allowedKeyManagement.get(6))) {
                z = false;
            }
            return z;
        } else if (ScanResultUtil.isScanResultForWepNetwork(scanResult)) {
            Log.d(TAG, "isScanResultForWepNetwork");
            if (config.wepKeys[0] == null) {
                z = false;
            }
            return z;
        } else {
            Log.d(TAG, "isScanResultForNone");
            if (config.wepKeys[0] != null) {
                z = false;
            }
            return z;
        }
    }

    public void setScreenOffMulticastFilter(boolean enabled) {
        if (this.mIpClient != null) {
            this.mIpClient.setScreenOffMulticastFilter(enabled);
        }
    }

    private void stopIpClient() {
        handlePostDhcpSetup();
        this.mIpClient.stop();
    }

    PendingIntent getPrivateBroadcast(String action, int requestCode) {
        Intent intent = new Intent(action, null);
        intent.addFlags(67108864);
        intent.setPackage("android");
        return this.mFacade.getBroadcast(this.mContext, requestCode, intent, 0);
    }

    void setSupplicantLogLevel() {
        this.mWifiNative.setSupplicantLogLevel(this.mVerboseLoggingEnabled);
    }

    public void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
            setLogRecSize(ActivityManager.isLowRamDeviceStatic() ? ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS : DEFAULT_POLL_RSSI_INTERVAL_MSECS);
        } else {
            this.mVerboseLoggingEnabled = false;
            setLogRecSize(100);
        }
        configureVerboseHalLogging(this.mVerboseLoggingEnabled);
        setSupplicantLogLevel();
        this.mCountryCode.enableVerboseLogging(verbose);
        this.mWifiScoreReport.enableVerboseLogging(this.mVerboseLoggingEnabled);
        this.mWifiDiagnostics.startLogging(this.mVerboseLoggingEnabled);
        this.mWifiMonitor.enableVerboseLogging(verbose);
        this.mWifiNative.enableVerboseLogging(verbose);
        this.mWifiConfigManager.enableVerboseLogging(verbose);
        this.mSupplicantStateTracker.enableVerboseLogging(verbose);
        this.mPasspointManager.enableVerboseLogging(verbose);
    }

    private void configureVerboseHalLogging(boolean enableVerbose) {
        if (!this.mBuildProperties.isUserBuild()) {
            this.mPropertyService.set(SYSTEM_PROPERTY_LOG_CONTROL_WIFIHAL, enableVerbose ? LOGD_LEVEL_VERBOSE : LOGD_LEVEL_DEBUG);
        }
    }

    public void clearANQPCache() {
    }

    public boolean setRandomMacOui() {
        String oui = this.mContext.getResources().getString(17039850);
        if (TextUtils.isEmpty(oui)) {
            oui = GOOGLE_OUI;
        }
        String[] ouiParts = oui.split("-");
        byte[] ouiBytes = new byte[]{(byte) (Integer.parseInt(ouiParts[0], 16) & Constants.BYTE_MASK), (byte) (Integer.parseInt(ouiParts[1], 16) & Constants.BYTE_MASK), (byte) (Integer.parseInt(ouiParts[2], 16) & Constants.BYTE_MASK)};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Setting OUI to ");
        stringBuilder.append(oui);
        logd(stringBuilder.toString());
        return this.mWifiNative.setScanningMacOui(this.mInterfaceName, ouiBytes);
    }

    public boolean clearRandomMacOui() {
        byte[] ouiBytes = new byte[]{(byte) 0, (byte) 0, (byte) 0};
        logd("Clear random OUI");
        return this.mWifiNative.setScanningMacOui(this.mInterfaceName, ouiBytes);
    }

    public void gameKOGAdjustSpeed(int mode) {
        this.mWifiNative.gameKOGAdjustSpeed(this.mWifiInfo.getFrequency(), mode);
    }

    public int setCmdToWifiChip(String iface, int mode, int type, int action, int param) {
        return this.mWifiNative.setCmdToWifiChip(iface, mode, type, action, param);
    }

    public void onMssSyncResultEvent(NativeMssResult mssstru) {
        if (checkAndGetHwMSSHandlerManager() != null) {
            this.mHwMssHandler.onMssDrvEvent(mssstru);
        }
    }

    private boolean connectToUserSelectNetwork(int netId, int uid, boolean forceReconnect) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("connectToUserSelectNetwork netId ");
        stringBuilder.append(netId);
        stringBuilder.append(", uid ");
        stringBuilder.append(uid);
        stringBuilder.append(", forceReconnect = ");
        stringBuilder.append(forceReconnect);
        logd(stringBuilder.toString());
        WifiConfiguration config = this.mWifiConfigManager.getConfiguredNetwork(netId);
        if (config == null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("connectToUserSelectNetwork Invalid network Id=");
            stringBuilder2.append(netId);
            loge(stringBuilder2.toString());
            return false;
        }
        if (HuaweiTelephonyConfigs.isChinaMobile()) {
            this.mWifiConfigManager.updatePriority(config, uid);
        }
        if (HwWifiServiceFactory.getHwWifiDevicePolicy().isWifiRestricted(config, true)) {
            return false;
        }
        StringBuilder stringBuilder3;
        if (!this.mWifiConfigManager.enableNetwork(netId, true, uid) || !this.mWifiConfigManager.updateLastConnectUid(netId, uid)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("connectToUserSelectNetwork Allowing uid ");
            stringBuilder3.append(uid);
            stringBuilder3.append(" with insufficient permissions to connect=");
            stringBuilder3.append(netId);
            logi(stringBuilder3.toString());
        } else if (this.mWifiPermissionsUtil.checkNetworkSettingsPermission(uid)) {
            this.mWifiConnectivityManager.setUserConnectChoice(netId);
        }
        if (forceReconnect || this.mWifiInfo.getNetworkId() != netId) {
            this.mWifiConnectivityManager.prepareForForcedConnection(netId);
            if (config.hiddenSSID && TextUtils.isEmpty(config.oriSsid)) {
                Log.d(TAG, "INTERCEPT1 the connect request since hidden and null oriSsid.");
                this.mScanRequestProxy.startScanForHiddenNetwork(new HiddenScanListener(config, uid), config);
            } else {
                startConnectToUserSelectNetwork(netId, uid, "any");
            }
        } else {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("connectToUserSelectNetwork already connecting/connected=");
            stringBuilder3.append(netId);
            logi(stringBuilder3.toString());
            notifyEnableSameNetworkId(netId);
        }
        return true;
    }

    public boolean isP2pConnected() {
        return this.mP2pConnected.get();
    }

    public Messenger getMessenger() {
        return new Messenger(getHandler());
    }

    public long getDisconnectedTimeMilli() {
        if (getCurrentState() != this.mDisconnectedState || this.mDisconnectedTimeStamp == 0) {
            return 0;
        }
        return this.mClock.getWallClockMillis() - this.mDisconnectedTimeStamp;
    }

    private boolean checkOrDeferScanAllowed(Message msg) {
        long now = this.mClock.getWallClockMillis();
        if (this.lastConnectAttemptTimestamp == 0 || now - this.lastConnectAttemptTimestamp >= 10000) {
            return true;
        }
        if (now - this.lastConnectAttemptTimestamp < 0) {
            logd("checkOrDeferScanAllowed time is jump!!!");
            this.lastConnectAttemptTimestamp = now;
        }
        sendMessageDelayed(Message.obtain(msg), 11000 - (now - this.lastConnectAttemptTimestamp));
        return false;
    }

    String reportOnTime() {
        long now = this.mClock.getWallClockMillis();
        StringBuilder sb = new StringBuilder();
        int on = this.mOnTime - this.mOnTimeLastReport;
        this.mOnTimeLastReport = this.mOnTime;
        int tx = this.mTxTime - this.mTxTimeLastReport;
        this.mTxTimeLastReport = this.mTxTime;
        int rx = this.mRxTime - this.mRxTimeLastReport;
        this.mRxTimeLastReport = this.mRxTime;
        int period = (int) (now - this.lastOntimeReportTimeStamp);
        this.lastOntimeReportTimeStamp = now;
        sb.append(String.format("[on:%d tx:%d rx:%d period:%d]", new Object[]{Integer.valueOf(on), Integer.valueOf(tx), Integer.valueOf(rx), Integer.valueOf(period)}));
        int on2 = this.mOnTime - this.mOnTimeScreenStateChange;
        on = (int) (now - this.lastScreenStateChangeTimeStamp);
        sb.append(String.format(" from screen [on:%d period:%d]", new Object[]{Integer.valueOf(on2), Integer.valueOf(on)}));
        return sb.toString();
    }

    WifiLinkLayerStats getWifiLinkLayerStats() {
        if (this.mInterfaceName == null) {
            loge("getWifiLinkLayerStats called without an interface");
            return null;
        }
        this.lastLinkLayerStatsUpdate = this.mClock.getWallClockMillis();
        WifiLinkLayerStats stats = this.mWifiNative.getWifiLinkLayerStats(this.mInterfaceName);
        if (stats != null) {
            this.mOnTime = stats.on_time;
            this.mTxTime = stats.tx_time;
            this.mRxTime = stats.rx_time;
            this.mRunningBeaconCount = stats.beacon_rx;
            this.mWifiInfo.updatePacketRates(stats, this.lastLinkLayerStatsUpdate);
        } else {
            long mTxPkts = this.mFacade.getTxPackets(this.mInterfaceName);
            this.mWifiInfo.updatePacketRates(mTxPkts, this.mFacade.getRxPackets(this.mInterfaceName), this.lastLinkLayerStatsUpdate);
        }
        return stats;
    }

    private byte[] getDstMacForKeepalive(KeepalivePacketData packetData) throws InvalidPacketException {
        try {
            return NativeUtil.macAddressToByteArray(macAddressFromRoute(RouteInfo.selectBestRoute(this.mLinkProperties.getRoutes(), packetData.dstAddress).getGateway().getHostAddress()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidPacketException(-21);
        }
    }

    private static int getEtherProtoForKeepalive(KeepalivePacketData packetData) throws InvalidPacketException {
        if (packetData.dstAddress instanceof Inet4Address) {
            return OsConstants.ETH_P_IP;
        }
        if (packetData.dstAddress instanceof Inet6Address) {
            return OsConstants.ETH_P_IPV6;
        }
        throw new InvalidPacketException(-21);
    }

    int startWifiIPPacketOffload(int slot, KeepalivePacketData packetData, int intervalSeconds) {
        InvalidPacketException e;
        int proto = 0;
        byte[] packet;
        try {
            packet = packetData.getPacket();
            try {
                byte[] dstMac = getDstMacForKeepalive(packetData);
                try {
                    int ret = this.mWifiNative.startSendingOffloadedPacket(this.mInterfaceName, slot, dstMac, packet, getEtherProtoForKeepalive(packetData), intervalSeconds * 1000);
                    if (ret == 0) {
                        return 0;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("startWifiIPPacketOffload(");
                    stringBuilder.append(slot);
                    stringBuilder.append(", ");
                    stringBuilder.append(intervalSeconds);
                    stringBuilder.append("): hardware error ");
                    stringBuilder.append(ret);
                    loge(stringBuilder.toString());
                    return -31;
                } catch (InvalidPacketException e2) {
                    e = e2;
                    byte[] bArr = dstMac;
                    return e.error;
                }
            } catch (InvalidPacketException e3) {
                e = e3;
                return e.error;
            }
        } catch (InvalidPacketException e4) {
            packet = null;
            e = e4;
            return e.error;
        }
    }

    int stopWifiIPPacketOffload(int slot) {
        int ret = this.mWifiNative.stopSendingOffloadedPacket(this.mInterfaceName, slot);
        if (ret == 0) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopWifiIPPacketOffload(");
        stringBuilder.append(slot);
        stringBuilder.append("): hardware error ");
        stringBuilder.append(ret);
        loge(stringBuilder.toString());
        return -31;
    }

    int startRssiMonitoringOffload(byte maxRssi, byte minRssi, WifiRssiEventHandler rssiHandler) {
        return this.mWifiNative.startRssiMonitoring(this.mInterfaceName, maxRssi, minRssi, rssiHandler);
    }

    int stopRssiMonitoringOffload() {
        return this.mWifiNative.stopRssiMonitoring(this.mInterfaceName);
    }

    public void setWifiStateForApiCalls(int newState) {
        String str;
        StringBuilder stringBuilder;
        switch (newState) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                if (this.mVerboseLoggingEnabled) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setting wifi state to: ");
                    stringBuilder.append(newState);
                    Log.d(str, stringBuilder.toString());
                }
                this.mWifiState.set(newState);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setWifiState: ");
                stringBuilder2.append(syncGetWifiStateByName());
                log(stringBuilder2.toString());
                return;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("attempted to set an invalid state: ");
                stringBuilder.append(newState);
                Log.d(str, stringBuilder.toString());
                return;
        }
    }

    public int syncGetWifiState() {
        return this.mWifiState.get();
    }

    public String syncGetWifiStateByName() {
        switch (this.mWifiState.get()) {
            case 0:
                return "disabling";
            case 1:
                return "disabled";
            case 2:
                return "enabling";
            case 3:
                return "enabled";
            case 4:
                return "unknown state";
            default:
                return "[invalid state]";
        }
    }

    public boolean isConnected() {
        return getCurrentState() == this.mConnectedState;
    }

    public boolean isDisconnected() {
        return getCurrentState() == this.mDisconnectedState;
    }

    public boolean isSupplicantTransientState() {
        SupplicantState supplicantState = this.mWifiInfo.getSupplicantState();
        String str;
        StringBuilder stringBuilder;
        if (supplicantState == SupplicantState.ASSOCIATING || supplicantState == SupplicantState.AUTHENTICATING || supplicantState == SupplicantState.FOUR_WAY_HANDSHAKE || supplicantState == SupplicantState.GROUP_HANDSHAKE) {
            if (this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Supplicant is under transient state: ");
                stringBuilder.append(supplicantState);
                Log.d(str, stringBuilder.toString());
            }
            return true;
        }
        if (this.mVerboseLoggingEnabled) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Supplicant is under steady state: ");
            stringBuilder.append(supplicantState);
            Log.d(str, stringBuilder.toString());
        }
        return false;
    }

    public WifiInfo syncRequestConnectionInfo(String callingPackage, int uid) {
        if (!isWifiSelfCuring()) {
            return new WifiInfo(this.mWifiInfo);
        }
        WifiInfo result = new WifiInfo(this.mWifiInfo);
        result.setNetworkId(getSelfCureNetworkId());
        if (result.getRssi() <= WifiMetrics.MIN_RSSI_DELTA) {
            result.setRssi(-70);
        }
        result.setSupplicantState(SupplicantState.COMPLETED);
        return result;
    }

    public WifiInfo getWifiInfo() {
        return this.mWifiInfo;
    }

    public DhcpResults syncGetDhcpResults() {
        DhcpResults dhcpResults;
        synchronized (this.mDhcpResultsLock) {
            dhcpResults = new DhcpResults(this.mDhcpResults);
        }
        return dhcpResults;
    }

    public void handleIfaceDestroyed() {
        handleNetworkDisconnect();
    }

    public void setOperationalMode(int mode, String ifaceName) {
        if (this.mVerboseLoggingEnabled) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setting operational mode to ");
            stringBuilder.append(String.valueOf(mode));
            stringBuilder.append(" for iface: ");
            stringBuilder.append(ifaceName);
            log(stringBuilder.toString());
        }
        this.mModeChange = true;
        if (mode != 1) {
            transitionTo(this.mDefaultState);
        } else if (ifaceName != null) {
            this.mInterfaceName = ifaceName;
            transitionTo(this.mDisconnectedState);
        } else {
            Log.e(TAG, "supposed to enter connect mode, but iface is null -> DefaultState");
            transitionTo(this.mDefaultState);
        }
        sendMessageAtFrontOfQueue(CMD_SET_OPERATIONAL_MODE);
    }

    public void takeBugReport(String bugTitle, String bugDetail) {
        this.mWifiDiagnostics.takeBugReport(bugTitle, bugDetail);
    }

    @VisibleForTesting
    protected int getOperationalModeForTest() {
        return this.mOperationalMode;
    }

    protected FilterController getMcastLockManagerFilterController() {
        return this.mMcastLockManagerFilterController;
    }

    public boolean syncQueryPasspointIcon(AsyncChannel channel, long bssid, String fileName) {
        Bundle bundle = new Bundle();
        bundle.putLong("BSSID", bssid);
        bundle.putString(EXTRA_OSU_ICON_QUERY_FILENAME, fileName);
        Message resultMsg = channel.sendMessageSynchronously(CMD_QUERY_OSU_ICON, bundle);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result == 1;
    }

    public int matchProviderWithCurrentNetwork(AsyncChannel channel, String fqdn) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_MATCH_PROVIDER_NETWORK, fqdn);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public void deauthenticateNetwork(AsyncChannel channel, long holdoff, boolean ess) {
    }

    public void disableEphemeralNetwork(String SSID) {
        if (SSID != null) {
            sendMessage(CMD_DISABLE_EPHEMERAL_NETWORK, SSID);
        }
    }

    public List<ScanDetail> getScanResultsListNoCopyUnsync() {
        return null;
    }

    public void disconnectCommand() {
        sendMessage(CMD_DISCONNECT);
    }

    public void disconnectCommand(int uid, int reason) {
        sendMessage(CMD_DISCONNECT, uid, reason);
    }

    public void reconnectCommand(WorkSource workSource) {
        sendMessage(CMD_RECONNECT, workSource);
    }

    public void reassociateCommand() {
        sendMessage(CMD_REASSOCIATE);
    }

    public void reloadTlsNetworksAndReconnect() {
        sendMessage(CMD_RELOAD_TLS_AND_RECONNECT);
    }

    public int syncAddOrUpdateNetwork(AsyncChannel channel, WifiConfiguration config) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_ADD_OR_UPDATE_NETWORK, config);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public List<WifiConfiguration> syncGetConfiguredNetworks(int uuid, AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_CONFIGURED_NETWORKS, uuid);
        if (resultMsg == null) {
            return null;
        }
        List<WifiConfiguration> result = resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public List<WifiConfiguration> syncGetPrivilegedConfiguredNetwork(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_PRIVILEGED_CONFIGURED_NETWORKS);
        List<WifiConfiguration> result = resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public WifiConfiguration syncGetMatchingWifiConfig(ScanResult scanResult, AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_MATCHING_CONFIG, scanResult);
        WifiConfiguration config = resultMsg.obj;
        resultMsg.recycle();
        return config;
    }

    List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult, AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_ALL_MATCHING_CONFIGS, scanResult);
        List<WifiConfiguration> configs = resultMsg.obj;
        resultMsg.recycle();
        return configs;
    }

    public List<OsuProvider> syncGetMatchingOsuProviders(ScanResult scanResult, AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_MATCHING_OSU_PROVIDERS, scanResult);
        List<OsuProvider> providers = resultMsg.obj;
        resultMsg.recycle();
        return providers;
    }

    public boolean syncAddOrUpdatePasspointConfig(AsyncChannel channel, PasspointConfiguration config, int uid) {
        boolean result = false;
        Message resultMsg = channel.sendMessageSynchronously(CMD_ADD_OR_UPDATE_PASSPOINT_CONFIG, uid, 0, config);
        if (resultMsg.arg1 == 1) {
            result = true;
        }
        resultMsg.recycle();
        return result;
    }

    public boolean syncRemovePasspointConfig(AsyncChannel channel, String fqdn) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_REMOVE_PASSPOINT_CONFIG, fqdn);
        boolean z = true;
        if (resultMsg.arg1 != 1) {
            z = false;
        }
        boolean result = z;
        resultMsg.recycle();
        return result;
    }

    public List<PasspointConfiguration> syncGetPasspointConfigs(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_PASSPOINT_CONFIGS);
        List<PasspointConfiguration> result = resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public boolean syncStartSubscriptionProvisioning(int callingUid, OsuProvider provider, IProvisioningCallback callback, AsyncChannel channel) {
        Message msg = Message.obtain();
        msg.what = CMD_START_SUBSCRIPTION_PROVISIONING;
        msg.arg1 = callingUid;
        msg.obj = callback;
        msg.getData().putParcelable(EXTRA_OSU_PROVIDER, provider);
        Message resultMsg = channel.sendMessageSynchronously(msg);
        boolean result = resultMsg.arg1 != 0;
        resultMsg.recycle();
        return result;
    }

    public int syncGetSupportedFeatures(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_SUPPORTED_FEATURES);
        int supportedFeatureSet = resultMsg.arg1;
        resultMsg.recycle();
        if (this.mPropertyService.getBoolean("config.disable_rtt", false)) {
            return supportedFeatureSet & -385;
        }
        return supportedFeatureSet;
    }

    public WifiLinkLayerStats syncGetLinkLayerStats(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_LINK_LAYER_STATS);
        WifiLinkLayerStats result = resultMsg.obj;
        resultMsg.recycle();
        return result;
    }

    public boolean syncRemoveNetwork(AsyncChannel channel, int networkId) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_REMOVE_NETWORK, networkId);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public boolean syncEnableNetwork(AsyncChannel channel, int netId, boolean disableOthers) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_ENABLE_NETWORK, netId, disableOthers);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    public boolean syncDisableNetwork(AsyncChannel channel, int netId) {
        Message resultMsg = channel.sendMessageSynchronously(151569, netId);
        boolean result = resultMsg.what != 151570;
        resultMsg.recycle();
        return result;
    }

    public void enableRssiPolling(boolean enabled) {
        sendMessage(CMD_ENABLE_RSSI_POLL, enabled, 0);
    }

    public void setHighPerfModeEnabled(boolean enable) {
        sendMessage(CMD_SET_HIGH_PERF_MODE, enable, 0);
    }

    public synchronized void resetSimAuthNetworks(boolean simPresent) {
        sendMessage(CMD_RESET_SIM_NETWORKS, simPresent);
    }

    public void notifyImsiAvailabe(boolean imsiAvailabe) {
        this.mIsImsiAvailable = imsiAvailabe;
    }

    public Network getCurrentNetwork() {
        if (this.mNetworkAgent != null) {
            return new Network(this.mNetworkAgent.netId);
        }
        return null;
    }

    public void enableTdls(String remoteMacAddress, boolean enable) {
        sendMessage(CMD_ENABLE_TDLS, enable, 0, remoteMacAddress);
    }

    public void sendBluetoothAdapterStateChange(int state) {
        sendMessage(CMD_BLUETOOTH_ADAPTER_STATE_CHANGE, state, 0);
    }

    public void removeAppConfigs(String packageName, int uid) {
        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = packageName;
        ai.uid = uid;
        sendMessage(CMD_REMOVE_APP_CONFIGURATIONS, ai);
    }

    public void removeUserConfigs(int userId) {
        sendMessage(CMD_REMOVE_USER_CONFIGURATIONS, userId);
    }

    public void updateBatteryWorkSource(WorkSource newSource) {
        synchronized (this.mRunningWifiUids) {
            if (newSource != null) {
                try {
                    this.mRunningWifiUids.set(newSource);
                } catch (RemoteException e) {
                }
            }
            if (this.mIsRunning) {
                if (!this.mReportedRunning) {
                    this.mBatteryStats.noteWifiRunning(this.mRunningWifiUids);
                    this.mLastRunningWifiUids.set(this.mRunningWifiUids);
                    this.mReportedRunning = true;
                } else if (!this.mLastRunningWifiUids.equals(this.mRunningWifiUids)) {
                    this.mBatteryStats.noteWifiRunningChanged(this.mLastRunningWifiUids, this.mRunningWifiUids);
                    this.mLastRunningWifiUids.set(this.mRunningWifiUids);
                }
            } else if (this.mReportedRunning) {
                this.mBatteryStats.noteWifiStopped(this.mLastRunningWifiUids);
                this.mLastRunningWifiUids.clear();
                this.mReportedRunning = false;
            }
            this.mWakeLock.setWorkSource(newSource);
            try {
            } catch (Throwable th) {
            }
        }
    }

    public void dumpIpClient(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mIpClient != null) {
            this.mIpClient.dump(fd, pw, args);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        this.mWifiInjector.dump(fd, pw, args);
        this.mSupplicantStateTracker.dump(fd, pw, args);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mLinkProperties ");
        stringBuilder.append(this.mLinkProperties);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mWifiInfo ");
        stringBuilder.append(this.mWifiInfo);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDhcpResults ");
        stringBuilder.append(this.mDhcpResults);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNetworkInfo ");
        stringBuilder.append(this.mNetworkInfo);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mLastSignalLevel ");
        stringBuilder.append(this.mLastSignalLevel);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mLastBssid ");
        stringBuilder.append(StringUtil.safeDisplayBssid(this.mLastBssid));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mLastNetworkId ");
        stringBuilder.append(this.mLastNetworkId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mOperationalMode ");
        stringBuilder.append(this.mOperationalMode);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mUserWantsSuspendOpt ");
        stringBuilder.append(this.mUserWantsSuspendOpt);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mSuspendOptNeedsDisabled ");
        stringBuilder.append(this.mSuspendOptNeedsDisabled);
        pw.println(stringBuilder.toString());
        this.mCountryCode.dump(fd, pw, args);
        if (this.mNetworkFactory != null) {
            this.mNetworkFactory.dump(fd, pw, args);
        } else {
            pw.println("mNetworkFactory is not initialized");
        }
        if (this.mUntrustedNetworkFactory != null) {
            this.mUntrustedNetworkFactory.dump(fd, pw, args);
        } else {
            pw.println("mUntrustedNetworkFactory is not initialized");
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Wlan Wake Reasons:");
        stringBuilder.append(this.mWifiNative.getWlanWakeReasonCount());
        pw.println(stringBuilder.toString());
        pw.println();
        this.mWifiConfigManager.dump(fd, pw, args);
        pw.println();
        this.mPasspointManager.dump(pw);
        pw.println();
        this.mWifiDiagnostics.captureBugReportData(7);
        this.mWifiDiagnostics.dump(fd, pw, args);
        dumpIpClient(fd, pw, args);
        if (this.mWifiConnectivityManager != null) {
            this.mWifiConnectivityManager.dump(fd, pw, args);
        } else {
            pw.println("mWifiConnectivityManager is not initialized");
        }
        this.mWifiInjector.getWakeupController().dump(fd, pw, args);
    }

    public void handleUserSwitch(int userId) {
        sendMessage(CMD_USER_SWITCH, userId);
    }

    public void handleUserUnlock(int userId) {
        sendMessage(CMD_USER_UNLOCK, userId);
    }

    public void handleUserStop(int userId) {
        sendMessage(CMD_USER_STOP, userId);
    }

    private void logStateAndMessage(Message message, State state) {
        this.messageHandlingStatus = 0;
        String currentStateTag = "";
        if (state == getCurrentState()) {
            currentStateTag = "$";
        }
        if (mLogMessages) {
            StringBuilder stringBuilder;
            switch (message.what) {
                case CMD_GET_CONFIGURED_NETWORKS /*131131*/:
                case CMD_GET_SUPPORTED_FEATURES /*131133*/:
                case CMD_GET_LINK_LAYER_STATS /*131135*/:
                case CMD_RSSI_POLL /*131155*/:
                case CMD_UPDATE_LINKPROPERTIES /*131212*/:
                case WifiMonitor.SCAN_RESULTS_EVENT /*147461*/:
                case 151572:
                    if (VDBG) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(currentStateTag);
                        stringBuilder.append(state.getClass().getSimpleName());
                        stringBuilder.append(" ");
                        stringBuilder.append(getLogRecString(message));
                        logd(stringBuilder.toString());
                        return;
                    }
                    return;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(currentStateTag);
                    stringBuilder.append(state.getClass().getSimpleName());
                    stringBuilder.append(" ");
                    stringBuilder.append(getLogRecString(message));
                    logd(stringBuilder.toString());
                    return;
            }
        }
    }

    protected boolean recordLogRec(Message msg) {
        if (msg.what != CMD_RSSI_POLL) {
            return true;
        }
        return this.mVerboseLoggingEnabled;
    }

    protected String getLogRecString(Message msg) {
        StringBuilder sb = new StringBuilder();
        if (this.mScreenOn) {
            sb.append("!");
        }
        if (this.messageHandlingStatus != 0) {
            sb.append("(");
            sb.append(this.messageHandlingStatus);
            sb.append(")");
        }
        sb.append(smToString(msg));
        if (msg.sendingUid > 0 && msg.sendingUid != 1010) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" uid=");
            stringBuilder.append(msg.sendingUid);
            sb.append(stringBuilder.toString());
        }
        long duration = this.mClock.getUptimeSinceBootMillis() - msg.getWhen();
        if (duration > 1000) {
            sb.append(" dur:");
            TimeUtils.formatDuration(duration, sb);
        }
        sb.append(" rt=");
        sb.append(this.mClock.getUptimeSinceBootMillis());
        sb.append("/");
        sb.append(this.mClock.getElapsedSinceBootMillis());
        WifiConfiguration config;
        WifiConfiguration curConfig;
        String key;
        StringBuilder stringBuilder2;
        switch (msg.what) {
            case CMD_ADD_OR_UPDATE_NETWORK /*131124*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (msg.obj != null) {
                    config = msg.obj;
                    sb.append(" ");
                    sb.append(config.configKey());
                    sb.append(" prio=");
                    sb.append(config.priority);
                    sb.append(" status=");
                    sb.append(config.status);
                    if (config.BSSID != null) {
                        sb.append(" ");
                        sb.append(config.BSSID);
                    }
                    curConfig = getCurrentWifiConfiguration();
                    if (curConfig != null) {
                        if (!curConfig.configKey().equals(config.configKey())) {
                            sb.append(" current=");
                            sb.append(curConfig.configKey());
                            sb.append(" prio=");
                            sb.append(curConfig.priority);
                            sb.append(" status=");
                            sb.append(curConfig.status);
                            break;
                        }
                        sb.append(" is current");
                        break;
                    }
                }
                break;
            case CMD_ENABLE_NETWORK /*131126*/:
            case 151569:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                key = this.mWifiConfigManager.getLastSelectedNetworkConfigKey();
                if (key != null) {
                    sb.append(" last=");
                    sb.append(key);
                }
                curConfig = this.mWifiConfigManager.getConfiguredNetwork(msg.arg1);
                if (curConfig != null && (key == null || !curConfig.configKey().equals(key))) {
                    sb.append(" target=");
                    sb.append(key);
                    break;
                }
            case CMD_GET_CONFIGURED_NETWORKS /*131131*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" num=");
                sb.append(this.mWifiConfigManager.getConfiguredNetworks().size());
                break;
            case CMD_RSSI_POLL /*131155*/:
            case CMD_UNWANTED_NETWORK /*131216*/:
            case 151572:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (!(this.mWifiInfo.getSSID() == null || this.mWifiInfo.getSSID() == null)) {
                    sb.append(" ");
                    sb.append(this.mWifiInfo.getSSID());
                }
                if (this.mWifiInfo.getBSSID() != null) {
                    sb.append(" ");
                    sb.append(this.mWifiInfo.getBSSID());
                }
                sb.append(" rssi=");
                sb.append(this.mWifiInfo.getRssi());
                sb.append(" f=");
                sb.append(this.mWifiInfo.getFrequency());
                sb.append(" sc=");
                sb.append(this.mWifiInfo.score);
                sb.append(" link=");
                sb.append(this.mWifiInfo.getLinkSpeed());
                sb.append(String.format(" tx=%.1f,", new Object[]{Double.valueOf(this.mWifiInfo.txSuccessRate)}));
                sb.append(String.format(" %.1f,", new Object[]{Double.valueOf(this.mWifiInfo.txRetriesRate)}));
                sb.append(String.format(" %.1f ", new Object[]{Double.valueOf(this.mWifiInfo.txBadRate)}));
                sb.append(String.format(" rx=%.1f", new Object[]{Double.valueOf(this.mWifiInfo.rxSuccessRate)}));
                sb.append(String.format(" bcn=%d", new Object[]{Integer.valueOf(this.mRunningBeaconCount)}));
                key = reportOnTime();
                if (key != null) {
                    sb.append(" ");
                    sb.append(key);
                }
                sb.append(String.format(" score=%d", new Object[]{Integer.valueOf(this.mWifiInfo.score)}));
                break;
            case CMD_ROAM_WATCHDOG_TIMER /*131166*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" cur=");
                sb.append(this.roamWatchdogCount);
                break;
            case CMD_DISCONNECTING_WATCHDOG_TIMER /*131168*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" cur=");
                sb.append(this.disconnectingWatchdogCount);
                break;
            case CMD_DISABLE_P2P_WATCHDOG_TIMER /*131184*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" cur=");
                sb.append(this.mDisableP2pWatchdogCount);
                break;
            case CMD_IP_CONFIGURATION_LOST /*131211*/:
                int count = -1;
                curConfig = getCurrentWifiConfiguration();
                if (curConfig != null) {
                    count = curConfig.getNetworkSelectionStatus().getDisableReasonCounter(4);
                }
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" failures: ");
                sb.append(Integer.toString(count));
                sb.append("/");
                sb.append(Integer.toString(this.mFacade.getIntegerSetting(this.mContext, "wifi_max_dhcp_retry_count", 0)));
                if (this.mWifiInfo.getBSSID() != null) {
                    sb.append(" ");
                    sb.append(this.mWifiInfo.getBSSID());
                }
                sb.append(String.format(" bcn=%d", new Object[]{Integer.valueOf(this.mRunningBeaconCount)}));
                break;
            case CMD_UPDATE_LINKPROPERTIES /*131212*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (this.mLinkProperties != null) {
                    sb.append(" ");
                    sb.append(getLinkPropertiesSummary(this.mLinkProperties));
                    break;
                }
                break;
            case CMD_TARGET_BSSID /*131213*/:
            case CMD_ASSOCIATED_BSSID /*131219*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (msg.obj != null) {
                    sb.append(" BSSID=");
                    sb.append(StringUtil.safeDisplayBssid((String) msg.obj));
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" Target=");
                    sb.append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=");
                sb.append(Boolean.toString(this.mIsAutoRoaming));
                break;
            case CMD_START_CONNECT /*131215*/:
            case 151553:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                config = this.mWifiConfigManager.getConfiguredNetwork(msg.arg1);
                if (config != null) {
                    sb.append(" ");
                    sb.append(config.configKey());
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" ");
                    sb.append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=");
                sb.append(Boolean.toString(this.mIsAutoRoaming));
                config = getCurrentWifiConfiguration();
                if (config != null) {
                    sb.append(config.configKey());
                    break;
                }
                break;
            case CMD_START_ROAM /*131217*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                ScanResult result = msg.obj;
                if (result != null) {
                    Long now = Long.valueOf(this.mClock.getWallClockMillis());
                    sb.append(" bssid=");
                    sb.append(result.BSSID);
                    sb.append(" rssi=");
                    sb.append(result.level);
                    sb.append(" freq=");
                    sb.append(result.frequency);
                    if (result.seen <= 0 || result.seen >= now.longValue()) {
                        sb.append(" !seen=");
                        sb.append(result.seen);
                    } else {
                        sb.append(" seen=");
                        sb.append(now.longValue() - result.seen);
                    }
                }
                if (this.mTargetRoamBSSID != null) {
                    sb.append(" ");
                    sb.append(this.mTargetRoamBSSID);
                }
                sb.append(" roam=");
                sb.append(Boolean.toString(this.mIsAutoRoaming));
                sb.append(" fail count=");
                sb.append(Integer.toString(this.mRoamFailCount));
                break;
            case CMD_IP_REACHABILITY_LOST /*131221*/:
                if (msg.obj != null) {
                    sb.append(" ");
                    sb.append((String) msg.obj);
                    break;
                }
                break;
            case CMD_START_RSSI_MONITORING_OFFLOAD /*131234*/:
            case CMD_STOP_RSSI_MONITORING_OFFLOAD /*131235*/:
            case CMD_RSSI_THRESHOLD_BREACHED /*131236*/:
                sb.append(" rssi=");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" thresholds=");
                sb.append(Arrays.toString(this.mRssiRanges));
                break;
            case CMD_IPV4_PROVISIONING_SUCCESS /*131272*/:
                sb.append(" ");
                if (msg.arg1 != 1) {
                    if (msg.arg1 != CMD_STATIC_IP_SUCCESS) {
                        sb.append(Integer.toString(msg.arg1));
                        break;
                    }
                    sb.append("STATIC_OK");
                    break;
                }
                sb.append("DHCP_OK");
                break;
            case CMD_IPV4_PROVISIONING_FAILURE /*131273*/:
                sb.append(" ");
                if (msg.arg1 != 2) {
                    if (msg.arg1 != CMD_STATIC_IP_FAILURE) {
                        sb.append(Integer.toString(msg.arg1));
                        break;
                    }
                    sb.append("STATIC_FAIL");
                    break;
                }
                sb.append("DHCP_FAIL");
                break;
            case CMD_INSTALL_PACKET_FILTER /*131274*/:
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" len=");
                stringBuilder2.append(((byte[]) msg.obj).length);
                sb.append(stringBuilder2.toString());
                break;
            case CMD_SET_FALLBACK_PACKET_FILTERING /*131275*/:
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" enabled=");
                stringBuilder2.append(((Boolean) msg.obj).booleanValue());
                sb.append(stringBuilder2.toString());
                break;
            case CMD_USER_SWITCH /*131277*/:
                sb.append(" userId=");
                sb.append(Integer.toString(msg.arg1));
                break;
            case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED /*143371*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (msg.obj != null) {
                    NetworkInfo info = msg.obj;
                    NetworkInfo.State state = info.getState();
                    DetailedState detailedState = info.getDetailedState();
                    if (state != null) {
                        sb.append(" st=");
                        sb.append(state);
                    }
                    if (detailedState != null) {
                        sb.append("/");
                        sb.append(detailedState);
                        break;
                    }
                }
                break;
            case WifiMonitor.NETWORK_CONNECTION_EVENT /*147459*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" ");
                sb.append(this.mLastBssid);
                sb.append(" nid=");
                sb.append(this.mLastNetworkId);
                config = getCurrentWifiConfiguration();
                if (config != null) {
                    sb.append(" ");
                    sb.append(config.configKey());
                }
                String key2 = this.mWifiConfigManager.getLastSelectedNetworkConfigKey();
                if (key2 != null) {
                    sb.append(" last=");
                    sb.append(key2);
                    break;
                }
                break;
            case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                if (msg.obj != null) {
                    sb.append(" ");
                    sb.append((String) msg.obj);
                }
                sb.append(" nid=");
                sb.append(msg.arg1);
                sb.append(" reason=");
                sb.append(msg.arg2);
                if (this.mLastBssid != null) {
                    sb.append(" lastbssid=");
                    sb.append(this.mLastBssid);
                }
                if (this.mWifiInfo.getFrequency() != -1) {
                    sb.append(" freq=");
                    sb.append(this.mWifiInfo.getFrequency());
                    sb.append(" rssi=");
                    sb.append(this.mWifiInfo.getRssi());
                    break;
                }
                break;
            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT /*147462*/:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                StateChangeResult stateChangeResult = msg.obj;
                if (stateChangeResult != null) {
                    sb.append(stateChangeResult.toString());
                    break;
                }
                break;
            case WifiMonitor.ASSOCIATION_REJECTION_EVENT /*147499*/:
                sb.append(" ");
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" timedOut=");
                stringBuilder2.append(Integer.toString(msg.arg1));
                sb.append(stringBuilder2.toString());
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                key = msg.obj;
                if (key != null && key.length() > 0) {
                    sb.append(" ");
                    sb.append(key);
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" blacklist=");
                stringBuilder3.append(Boolean.toString(this.didBlackListBSSID));
                sb.append(stringBuilder3.toString());
                break;
            case 151556:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                config = (WifiConfiguration) msg.obj;
                if (config != null) {
                    sb.append(" ");
                    sb.append(config.configKey());
                    sb.append(" nid=");
                    sb.append(config.networkId);
                    if (config.hiddenSSID) {
                        sb.append(" hidden");
                    }
                    if (config.preSharedKey != null) {
                        sb.append(" hasPSK");
                    }
                    if (config.ephemeral) {
                        sb.append(" ephemeral");
                    }
                    if (config.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    sb.append(" cuid=");
                    sb.append(config.creatorUid);
                    sb.append(" suid=");
                    sb.append(config.lastUpdateUid);
                    NetworkSelectionStatus netWorkSelectionStatus = config.getNetworkSelectionStatus();
                    sb.append(" ajst=");
                    sb.append(netWorkSelectionStatus.getNetworkStatusString());
                    break;
                }
                break;
            case 151559:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                config = msg.obj;
                if (config != null) {
                    sb.append(" ");
                    sb.append(config.configKey());
                    sb.append(" nid=");
                    sb.append(config.networkId);
                    if (config.hiddenSSID) {
                        sb.append(" hidden");
                    }
                    if (!(config.preSharedKey == null || config.preSharedKey.equals("*"))) {
                        sb.append(" hasPSK");
                    }
                    if (config.ephemeral) {
                        sb.append(" ephemeral");
                    }
                    if (config.selfAdded) {
                        sb.append(" selfAdded");
                    }
                    sb.append(" cuid=");
                    sb.append(config.creatorUid);
                    sb.append(" suid=");
                    sb.append(config.lastUpdateUid);
                    break;
                }
                break;
            case 196611:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                sb.append(" txpkts=");
                sb.append(this.mWifiInfo.txSuccess);
                sb.append(",");
                sb.append(this.mWifiInfo.txBad);
                sb.append(",");
                sb.append(this.mWifiInfo.txRetries);
                break;
            case 196612:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                if (msg.arg1 == 1) {
                    sb.append(" OK ");
                } else if (msg.arg1 == 2) {
                    sb.append(" FAIL ");
                }
                if (this.mLinkProperties != null) {
                    sb.append(" ");
                    sb.append(getLinkPropertiesSummary(this.mLinkProperties));
                    break;
                }
                break;
            default:
                sb.append(" ");
                sb.append(Integer.toString(msg.arg1));
                sb.append(" ");
                sb.append(Integer.toString(msg.arg2));
                break;
        }
        return sb.toString();
    }

    private void handleScreenStateChanged(boolean screenOn) {
        StringBuilder stringBuilder;
        this.mScreenOn = screenOn;
        if (this.mVerboseLoggingEnabled) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" handleScreenStateChanged Enter: screenOn=");
            stringBuilder.append(screenOn);
            stringBuilder.append(" mUserWantsSuspendOpt=");
            stringBuilder.append(this.mUserWantsSuspendOpt);
            stringBuilder.append(" state ");
            stringBuilder.append(getCurrentState().getName());
            stringBuilder.append(" suppState:");
            stringBuilder.append(this.mSupplicantStateTracker.getSupplicantStateName());
            logd(stringBuilder.toString());
        }
        enableRssiPolling(screenOn);
        if (this.mUserWantsSuspendOpt.get()) {
            int shouldReleaseWakeLock = 0;
            if (screenOn) {
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 0, 0);
            } else {
                if (isConnected()) {
                    this.mSuspendWakeLock.acquire(2000);
                    shouldReleaseWakeLock = 1;
                }
                sendMessage(CMD_SET_SUSPEND_OPT_ENABLED, 1, shouldReleaseWakeLock);
            }
        }
        if (this.mIsRunning) {
            getWifiLinkLayerStats();
            this.mOnTimeScreenStateChange = this.mOnTime;
            this.lastScreenStateChangeTimeStamp = this.lastLinkLayerStatsUpdate;
        }
        this.mWifiMetrics.setScreenState(screenOn);
        if (this.mWifiConnectivityManager != null) {
            long currenTime = this.mClock.getElapsedSinceBootMillis();
            if (screenOn && currenTime - this.mLastAllowSendHiLinkScanResultsBroadcastTime > 3000) {
                Log.d(TAG, "handleScreenStateChanged: allow send HiLink scan results broadcast.");
                this.mScanRequestProxy.mAllowSendHiLinkScanResultsBroadcast = true;
                this.mLastAllowSendHiLinkScanResultsBroadcastTime = currenTime;
                this.mScanRequestProxy.mSendHiLinkScanResultsBroadcastTries = 0;
            }
            this.mWifiConnectivityManager.handleScreenStateChanged(screenOn);
        }
        if (this.mVerboseLoggingEnabled) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleScreenStateChanged Exit: ");
            stringBuilder.append(screenOn);
            log(stringBuilder.toString());
        }
    }

    private void checkAndSetConnectivityInstance() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    private void setSuspendOptimizationsNative(int reason, boolean enabled) {
        StringBuilder stringBuilder;
        if (this.mVerboseLoggingEnabled) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setSuspendOptimizationsNative: ");
            stringBuilder.append(reason);
            stringBuilder.append(" ");
            stringBuilder.append(enabled);
            stringBuilder.append(" -want ");
            stringBuilder.append(this.mUserWantsSuspendOpt.get());
            stringBuilder.append(" stack:");
            stringBuilder.append(Thread.currentThread().getStackTrace()[2].getMethodName());
            stringBuilder.append(" - ");
            stringBuilder.append(Thread.currentThread().getStackTrace()[3].getMethodName());
            stringBuilder.append(" - ");
            stringBuilder.append(Thread.currentThread().getStackTrace()[4].getMethodName());
            stringBuilder.append(" - ");
            stringBuilder.append(Thread.currentThread().getStackTrace()[5].getMethodName());
            log(stringBuilder.toString());
        }
        if (enabled) {
            this.mSuspendOptNeedsDisabled &= ~reason;
            if (this.mSuspendOptNeedsDisabled == 0 && this.mUserWantsSuspendOpt.get()) {
                if (this.mVerboseLoggingEnabled) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setSuspendOptimizationsNative do it ");
                    stringBuilder.append(reason);
                    stringBuilder.append(" ");
                    stringBuilder.append(enabled);
                    stringBuilder.append(" stack:");
                    stringBuilder.append(Thread.currentThread().getStackTrace()[2].getMethodName());
                    stringBuilder.append(" - ");
                    stringBuilder.append(Thread.currentThread().getStackTrace()[3].getMethodName());
                    stringBuilder.append(" - ");
                    stringBuilder.append(Thread.currentThread().getStackTrace()[4].getMethodName());
                    stringBuilder.append(" - ");
                    stringBuilder.append(Thread.currentThread().getStackTrace()[5].getMethodName());
                    log(stringBuilder.toString());
                }
                this.mWifiNative.setSuspendOptimizations(this.mInterfaceName, true);
                return;
            }
            return;
        }
        this.mSuspendOptNeedsDisabled |= reason;
        this.mWifiNative.setSuspendOptimizations(this.mInterfaceName, false);
    }

    private void setSuspendOptimizations(int reason, boolean enabled) {
        StringBuilder stringBuilder;
        if (this.mVerboseLoggingEnabled) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setSuspendOptimizations: ");
            stringBuilder.append(reason);
            stringBuilder.append(" ");
            stringBuilder.append(enabled);
            log(stringBuilder.toString());
        }
        if (enabled) {
            this.mSuspendOptNeedsDisabled &= ~reason;
        } else {
            this.mSuspendOptNeedsDisabled |= reason;
        }
        if (this.mVerboseLoggingEnabled) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("mSuspendOptNeedsDisabled ");
            stringBuilder.append(this.mSuspendOptNeedsDisabled);
            log(stringBuilder.toString());
        }
    }

    public void setWifiStateForHw(int wifiState) {
        if (wifiState == 3) {
            broadcastWifiDriverChanged(1);
        } else if (wifiState == 1) {
            broadcastWifiDriverChanged(2);
            if (this.isBootCompleted && this.mWifiP2pServiceImpl != null) {
                this.mWifiP2pServiceImpl.setWifiRepeaterState(0);
            }
        }
    }

    public void setmSettingsStore(WifiSettingsStore settingsStore) {
        this.mWifiSettingStore = settingsStore;
    }

    public boolean attemptAutoConnect() {
        SupplicantState state = this.mWifiInfo.getSupplicantState();
        if (getCurrentState() != this.mRoamingState && getCurrentState() != this.mObtainingIpState && getCurrentState() != this.mDisconnectingState && state != SupplicantState.ASSOCIATING && state != SupplicantState.ASSOCIATED && state != SupplicantState.AUTHENTICATING && state != SupplicantState.FOUR_WAY_HANDSHAKE && state != SupplicantState.GROUP_HANDSHAKE) {
            return true;
        }
        Log.w(TAG, "attemptAutoConnect: false");
        return false;
    }

    public void setCHRConnectingSartTimestamp(long connectingStartTimestamp) {
        if (connectingStartTimestamp > 0) {
            this.mConnectingStartTimestamp = connectingStartTimestamp;
        }
    }

    private void fetchRssiLinkSpeedAndFrequencyNative() {
        if (SupplicantState.ASSOCIATED.compareTo(this.mWifiInfo.getSupplicantState()) > 0 || SupplicantState.COMPLETED.compareTo(this.mWifiInfo.getSupplicantState()) < 0) {
            loge("error state to fetch rssi");
            return;
        }
        SignalPollResult pollResult = this.mWifiNative.signalPoll(this.mInterfaceName);
        if (pollResult != null) {
            Integer newRssi = Integer.valueOf(pollResult.currentRssi);
            Integer newLinkSpeed = Integer.valueOf(pollResult.txBitrate);
            Integer newFrequency = Integer.valueOf(pollResult.associationFrequency);
            this.mWifiInfo.setNoise(pollResult.currentNoise);
            this.mWifiInfo.setSnr(pollResult.currentSnr);
            this.mWifiInfo.setChload(pollResult.currentChload);
            if (this.mVerboseLoggingEnabled) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fetchRssiLinkSpeedAndFrequencyNative rssi=");
                stringBuilder.append(newRssi);
                stringBuilder.append(" linkspeed=");
                stringBuilder.append(newLinkSpeed);
                stringBuilder.append(" freq=");
                stringBuilder.append(newFrequency);
                logd(stringBuilder.toString());
            }
            if (newRssi == null || newRssi.intValue() <= WifiMetrics.MIN_RSSI_DELTA || newRssi.intValue() >= ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS) {
                this.mWifiInfo.setRssi(WifiMetrics.MIN_RSSI_DELTA);
                updateCapabilities();
            } else {
                if (newRssi.intValue() > 5) {
                    newRssi = Integer.valueOf(newRssi.intValue() - 256);
                }
                this.mWifiInfo.setRssi(newRssi.intValue());
                if (checkAndGetHwMSSHandlerManager() != null) {
                    this.mHwMssHandler.mssSwitchCheck(newRssi.intValue());
                }
                if (isAllowedManualWifiPwrBoost() == 0) {
                    this.mWifiNative.pwrPercentBoostModeset(newRssi.intValue());
                }
                int newSignalLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(this.mWifiInfo.getFrequency(), newRssi.intValue());
                if (newSignalLevel != this.mLastSignalLevel) {
                    updateCapabilities();
                    sendRssiChangeBroadcast(newRssi.intValue());
                }
                this.mLastSignalLevel = newSignalLevel;
            }
            if (newLinkSpeed != null) {
                this.mWifiInfo.setLinkSpeed(newLinkSpeed.intValue());
            }
            if (newFrequency != null && newFrequency.intValue() > 0) {
                this.mWifiInfo.setFrequency(newFrequency.intValue());
                sendStaFrequency(newFrequency.intValue());
            }
            this.mWifiConfigManager.updateScanDetailCacheFromWifiInfo(this.mWifiInfo);
            if (!(newRssi == null || newLinkSpeed == null || newFrequency == null)) {
                this.mWifiMetrics.handlePollResult(this.mWifiInfo);
            }
        }
    }

    private void cleanWifiScore() {
        this.mWifiInfo.txBadRate = 0.0d;
        this.mWifiInfo.txSuccessRate = 0.0d;
        this.mWifiInfo.txRetriesRate = 0.0d;
        this.mWifiInfo.rxSuccessRate = 0.0d;
        this.mWifiScoreReport.reset();
    }

    private void updateLinkProperties(LinkProperties newLp) {
        StringBuilder stringBuilder;
        if (this.mVerboseLoggingEnabled) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Link configuration changed for netId: ");
            stringBuilder.append(this.mLastNetworkId);
            stringBuilder.append(" old: ");
            stringBuilder.append(this.mLinkProperties);
            stringBuilder.append(" new: ");
            stringBuilder.append(newLp);
            log(stringBuilder.toString());
        }
        this.mLinkProperties = newLp;
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
        if (getNetworkDetailedState() == DetailedState.CONNECTED) {
            sendLinkConfigurationChangedBroadcast();
        }
        if (this.mVerboseLoggingEnabled) {
            stringBuilder = new StringBuilder();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateLinkProperties nid: ");
            stringBuilder2.append(this.mLastNetworkId);
            stringBuilder.append(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" state: ");
            stringBuilder2.append(getNetworkDetailedState());
            stringBuilder.append(stringBuilder2.toString());
            if (this.mLinkProperties != null) {
                stringBuilder.append(" ");
                stringBuilder.append(getLinkPropertiesSummary(this.mLinkProperties));
            }
            logd(stringBuilder.toString());
        }
    }

    private void clearLinkProperties() {
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                this.mDhcpResults.clear();
            }
        }
        this.mLinkProperties.clear();
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
    }

    private String updateDefaultRouteMacAddress(int timeout) {
        String address = null;
        for (RouteInfo route : this.mLinkProperties.getRoutes()) {
            if (route != null && route.isDefaultRoute() && route.hasGateway()) {
                InetAddress gateway = route.getGateway();
                if (gateway instanceof Inet4Address) {
                    if (this.mVerboseLoggingEnabled) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("updateDefaultRouteMacAddress found Ipv4 default :");
                        stringBuilder.append(gateway.getHostAddress());
                        logd(stringBuilder.toString());
                    }
                    address = macAddressFromRoute(gateway.getHostAddress());
                    if (address == null && timeout > 0) {
                        try {
                            Thread.sleep((long) timeout);
                        } catch (InterruptedException e) {
                        }
                        address = macAddressFromRoute(gateway.getHostAddress());
                    }
                    if (address != null) {
                        this.mWifiConfigManager.setNetworkDefaultGwMacAddress(this.mLastNetworkId, address);
                    }
                }
            }
        }
        return address;
    }

    private void sendRssiChangeBroadcast(int newRssi) {
        try {
            this.mBatteryStats.noteWifiRssiChanged(newRssi);
        } catch (RemoteException e) {
        }
        Intent intent = new Intent("android.net.wifi.RSSI_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("newRssi", newRssi);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendNetworkStateChangeBroadcast(String bssid) {
        Intent intent = new Intent("android.net.wifi.STATE_CHANGE");
        intent.addFlags(67108864);
        NetworkInfo networkInfo = new NetworkInfo(this.mNetworkInfo);
        networkInfo.setExtraInfo(null);
        intent.putExtra("networkInfo", networkInfo);
        checkSelfCureWifiResult(102);
        if (!ignoreNetworkStateChange(this.mNetworkInfo)) {
            StringBuilder detailLog = new StringBuilder();
            detailLog.append("NetworkStateChange ");
            detailLog.append(this.mNetworkInfo.getState());
            detailLog.append("/");
            detailLog.append(this.mNetworkInfo.getDetailedState());
            Log.i(TAG, detailLog.toString());
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void sendLinkConfigurationChangedBroadcast() {
        Intent intent = new Intent("android.net.wifi.LINK_CONFIGURATION_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("linkProperties", new LinkProperties(this.mLinkProperties));
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void sendSupplicantConnectionChangedBroadcast(boolean connected) {
        Intent intent = new Intent("android.net.wifi.supplicant.CONNECTION_CHANGE");
        intent.addFlags(67108864);
        intent.putExtra("connected", connected);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean setNetworkDetailedState(DetailedState state) {
        boolean hidden = false;
        if (this.mIsAutoRoaming) {
            hidden = true;
        }
        if (this.mVerboseLoggingEnabled) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDetailed state, old =");
            stringBuilder.append(this.mNetworkInfo.getDetailedState());
            stringBuilder.append(" and new state=");
            stringBuilder.append(state);
            stringBuilder.append(" hidden=");
            stringBuilder.append(hidden);
            log(stringBuilder.toString());
        }
        if (hidden || state == this.mNetworkInfo.getDetailedState()) {
            return false;
        }
        this.mNetworkInfo.setDetailedState(state, null, null);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
        sendNetworkStateChangeBroadcast(null);
        return true;
    }

    private DetailedState getNetworkDetailedState() {
        return this.mNetworkInfo.getDetailedState();
    }

    private SupplicantState handleSupplicantStateChange(Message message) {
        StateChangeResult stateChangeResult = message.obj;
        SupplicantState state = stateChangeResult.state;
        this.mWifiInfo.setSupplicantState(state);
        if (SupplicantState.isConnecting(state)) {
            this.mWifiInfo.setNetworkId(stateChangeResult.networkId);
            this.mWifiInfo.setBSSID(stateChangeResult.BSSID);
            this.mWifiInfo.setSSID(stateChangeResult.wifiSsid);
        } else {
            this.mWifiInfo.setNetworkId(-1);
            this.mWifiInfo.setBSSID(null);
            this.mWifiInfo.setSSID(null);
        }
        updateCapabilities();
        if (SupplicantState.AUTHENTICATING == state || SupplicantState.ASSOCIATED == state) {
            fetchRssiLinkSpeedAndFrequencyNative();
        }
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config != null) {
            stateChangeResult.networkId = config.networkId;
            this.mWifiInfo.setEphemeral(config.ephemeral);
            ScanDetailCache scanDetailCache = this.mWifiConfigManager.getScanDetailCacheForNetwork(config.networkId);
            if (scanDetailCache != null) {
                ScanDetail scanDetail = scanDetailCache.getScanDetail(stateChangeResult.BSSID);
                if (scanDetail != null) {
                    this.mWifiInfo.setFrequency(scanDetail.getScanResult().frequency);
                    NetworkDetail networkDetail = scanDetail.getNetworkDetail();
                    if (networkDetail != null && networkDetail.getAnt() == Ant.ChargeablePublic) {
                        this.mWifiInfo.setMeteredHint(true);
                    }
                }
            }
        }
        if (SupplicantState.isConnecting(state)) {
            this.mWifiInfo.setNetworkId(stateChangeResult.networkId);
        } else {
            this.mWifiInfo.setNetworkId(-1);
        }
        this.mSupplicantStateTracker.sendMessage(Message.obtain(message));
        return state;
    }

    private void updateDefaultMtu() {
        if (this.mLinkProperties == null || this.mNwService == null) {
            loge("LinkProperties or NwService is null.");
            return;
        }
        String iface = this.mLinkProperties.getInterfaceName();
        if (!TextUtils.isEmpty(iface)) {
            if (DEFAULT_MTU == this.mLinkProperties.getMtu()) {
                log("MTU is same as the default: 1500");
                return;
            }
            this.mLinkProperties.setMtu(DEFAULT_MTU);
            StringBuilder stringBuilder;
            try {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Setting MTU size: ");
                stringBuilder.append(iface);
                stringBuilder.append(", ");
                stringBuilder.append(DEFAULT_MTU);
                log(stringBuilder.toString());
                this.mNwService.setMtu(iface, DEFAULT_MTU);
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("exception in setMtu()");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
            }
        }
    }

    protected void handleNetworkDisconnect() {
        if (DBG) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleNetworkDisconnect: Stopping DHCP and clearing IP stack:");
            stringBuilder.append(Thread.currentThread().getStackTrace()[2].getMethodName());
            stringBuilder.append(" - ");
            stringBuilder.append(Thread.currentThread().getStackTrace()[3].getMethodName());
            stringBuilder.append(" - ");
            stringBuilder.append(Thread.currentThread().getStackTrace()[4].getMethodName());
            stringBuilder.append(" - ");
            stringBuilder.append(Thread.currentThread().getStackTrace()[5].getMethodName());
            log(stringBuilder.toString());
        }
        stopRssiMonitoringOffload();
        clearTargetBssid("handleNetworkDisconnect");
        stopIpClient();
        this.mWifiScoreReport.reset();
        this.mWifiInfo.reset();
        this.mIsAutoRoaming = false;
        setNetworkDetailedState(DetailedState.DISCONNECTED);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
            this.mNetworkAgent = null;
        }
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (TelephonyUtil.isSimConfig(config) && TelephonyUtil.getSimIdentity(getTelephonyManager(), new TelephonyUtil(), config) == null) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("remove EAP-SIM/AKA/AKA' network ");
            stringBuilder2.append(this.mLastNetworkId);
            stringBuilder2.append(" from wpa_supplicant since identity was absent.");
            Log.d(str, stringBuilder2.toString());
            this.mWifiNative.removeAllNetworks(this.mInterfaceName);
        }
        updateDefaultMtu();
        clearLinkProperties();
        sendNetworkStateChangeBroadcast(this.mLastBssid);
        this.mLastBssid = null;
        registerDisconnected();
        this.mLastNetworkId = -1;
    }

    void handlePreDhcpSetup() {
        this.mWifiNative.setBluetoothCoexistenceMode(this.mInterfaceName, 1);
        setSuspendOptimizationsNative(1, false);
        this.mWifiNative.setPowerSave(this.mInterfaceName, false);
        getWifiLinkLayerStats();
        if (this.mWifiP2pChannel != null) {
            Message msg = new Message();
            msg.what = WifiP2pServiceImpl.BLOCK_DISCOVERY;
            msg.arg1 = 1;
            msg.arg2 = 196614;
            msg.obj = this;
            this.mWifiP2pChannel.sendMessage(msg);
            return;
        }
        sendMessage(196614);
    }

    void handlePostDhcpSetup() {
        setSuspendOptimizationsNative(1, true);
        this.mWifiNative.setPowerSave(this.mInterfaceName, true);
        p2pSendMessage(WifiP2pServiceImpl.BLOCK_DISCOVERY, 0);
        this.mWifiNative.setBluetoothCoexistenceMode(this.mInterfaceName, 2);
    }

    private void reportConnectionAttemptStart(WifiConfiguration config, String targetBSSID, int roamType) {
        this.mWifiMetrics.startConnectionEvent(config, targetBSSID, roamType);
        this.mDiagsConnectionStartMillis = this.mClock.getElapsedSinceBootMillis();
        this.mWifiDiagnostics.reportConnectionEvent(this.mDiagsConnectionStartMillis, (byte) 0);
        Long lDiagsConnectionStartMillis = Long.valueOf(this.mDiagsConnectionStartMillis);
        if (lDiagsConnectionStartMillis == null) {
            loge("reportConnectionAttemptStart : lDiagsConnectionStartMillis is null");
        }
        sendMessageDelayed(CMD_DIAGS_CONNECT_TIMEOUT, lDiagsConnectionStartMillis, 60000);
    }

    private void reportConnectionAttemptEnd(int level2FailureCode, int connectivityFailureCode) {
        this.mWifiMetrics.endConnectionEvent(level2FailureCode, connectivityFailureCode);
        this.mWifiConnectivityManager.handleConnectionAttemptEnded(level2FailureCode);
        if (level2FailureCode == 1) {
            this.mWifiDiagnostics.reportConnectionEvent(this.mDiagsConnectionStartMillis, (byte) 1);
        } else if (!(level2FailureCode == 5 || level2FailureCode == 8)) {
            this.mWifiDiagnostics.reportConnectionEvent(this.mDiagsConnectionStartMillis, (byte) 2);
        }
        this.mDiagsConnectionStartMillis = -1;
    }

    private void handleIPv4Success(DhcpResults dhcpResults) {
        Inet4Address addr;
        if (this.mVerboseLoggingEnabled) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleIPv4Success <");
            stringBuilder.append(dhcpResults.toString());
            stringBuilder.append(">");
            logd(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("link address ");
            stringBuilder.append(dhcpResults.ipAddress);
            logd(stringBuilder.toString());
        }
        synchronized (this.mDhcpResultsLock) {
            this.mDhcpResults = dhcpResults;
            addr = (Inet4Address) dhcpResults.ipAddress.getAddress();
        }
        if (this.mIsAutoRoaming && this.mWifiInfo.getIpAddress() != NetworkUtils.inetAddressToInt(addr)) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleIPv4Success, roaming and address changed");
            stringBuilder2.append(this.mWifiInfo);
            stringBuilder2.append(" got: ");
            stringBuilder2.append(addr);
            logd(stringBuilder2.toString());
        }
        this.mWifiInfo.setInetAddress(addr);
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config != null) {
            this.mWifiInfo.setEphemeral(config.ephemeral);
        }
        if (dhcpResults.hasMeteredHint() || hasMeteredHintForWi(addr)) {
            this.mWifiInfo.setMeteredHint(true);
        }
        updateCapabilities(config);
    }

    private void handleSuccessfulIpConfiguration() {
        this.mLastSignalLevel = -1;
        WifiConfiguration c = getCurrentWifiConfiguration();
        if (c != null) {
            c.getNetworkSelectionStatus().clearDisableReasonCounter(4);
            updateCapabilities(c);
        }
    }

    private void handleIPv4Failure() {
        this.mWifiDiagnostics.captureBugReportData(4);
        if (this.mVerboseLoggingEnabled) {
            int count = -1;
            WifiConfiguration config = getCurrentWifiConfiguration();
            if (config != null) {
                count = config.getNetworkSelectionStatus().getDisableReasonCounter(4);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DHCP failure count=");
            stringBuilder.append(count);
            log(stringBuilder.toString());
        }
        reportConnectionAttemptEnd(10, 2);
        synchronized (this.mDhcpResultsLock) {
            if (this.mDhcpResults != null) {
                this.mDhcpResults.clear();
            }
        }
        if (this.mVerboseLoggingEnabled) {
            logd("handleIPv4Failure");
        }
    }

    private void handleIpConfigurationLost() {
        this.mWifiInfo.setInetAddress(null);
        this.mWifiInfo.setMeteredHint(false);
        if (DBG) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleIpConfigurationLost: SSID = ");
            stringBuilder.append(this.mWifiInfo.getSSID());
            stringBuilder.append(", BSSID = ");
            stringBuilder.append(this.mWifiInfo.getBSSID());
            loge(stringBuilder.toString());
        }
        this.mWifiConfigManager.updateNetworkSelectionStatus(this.mLastNetworkId, 4);
        notifyWifiConnFailedInfo(this.mLastNetworkId, this.mWifiInfo.getBSSID(), this.mWifiInfo.getRssi(), 4, this.mWifiConnectivityManager);
        this.mWifiNative.disconnect(this.mInterfaceName);
    }

    private void handleIpReachabilityLost() {
        this.mWifiInfo.setInetAddress(null);
        this.mWifiInfo.setMeteredHint(false);
        this.mWifiNative.disconnect(this.mInterfaceName);
    }

    private String macAddressFromRoute(String ipAddress) {
        String macAddress = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/net/arp"));
            String line = reader.readLine();
            while (true) {
                String readLine = reader.readLine();
                line = readLine;
                if (readLine == null) {
                    break;
                }
                String[] tokens = line.split("[ ]+");
                if (tokens.length >= 6) {
                    String ip = tokens[null];
                    String mac = tokens[3];
                    if (ipAddress.equals(ip)) {
                        macAddress = mac;
                        break;
                    }
                }
            }
            if (macAddress == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Did not find remoteAddress {");
                stringBuilder.append(ipAddress);
                stringBuilder.append("} in /proc/net/arp");
                loge(stringBuilder.toString());
            }
            try {
                reader.close();
            } catch (IOException e) {
            }
        } catch (FileNotFoundException e2) {
            loge("Could not open /proc/net/arp to lookup mac address");
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e3) {
            loge("Could not read /proc/net/arp to lookup mac address");
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e4) {
                }
            }
        }
        return macAddress;
    }

    private boolean isPermanentWrongPasswordFailure(int networkId, int reasonCode) {
        if (reasonCode != 2) {
            return false;
        }
        WifiConfiguration network = this.mWifiConfigManager.getConfiguredNetwork(networkId);
        if (network == null || !network.getNetworkSelectionStatus().getHasEverConnected()) {
            return true;
        }
        return false;
    }

    void maybeRegisterNetworkFactory() {
        if (this.mNetworkFactory == null) {
            checkAndSetConnectivityInstance();
            if (this.mCm != null) {
                this.mNetworkFactory = new WifiNetworkFactory(getHandler().getLooper(), this.mContext, NETWORKTYPE, this.mNetworkCapabilitiesFilter);
                this.mNetworkFactory.setScoreFilter(59);
                this.mNetworkFactory.register();
                this.mUntrustedNetworkFactory = new UntrustedWifiNetworkFactory(getHandler().getLooper(), this.mContext, NETWORKTYPE_UNTRUSTED, this.mNetworkCapabilitiesFilter);
                this.mUntrustedNetworkFactory.setScoreFilter(Values.MAX_EXPID);
                this.mUntrustedNetworkFactory.register();
            }
        }
    }

    private void getAdditionalWifiServiceInterfaces() {
        if (this.mP2pSupported) {
            this.mWifiP2pServiceImpl = (WifiP2pServiceImpl) IWifiP2pManager.Stub.asInterface(this.mFacade.getService("wifip2p"));
            if (this.mWifiP2pServiceImpl != null && this.mWifiP2pChannel == null) {
                this.mWifiP2pChannel = new AsyncChannel();
                this.mWifiP2pChannel.connect(this.mContext, getHandler(), this.mWifiP2pServiceImpl.getP2pStateMachineMessenger());
                this.mWifiRepeater = this.mWifiP2pServiceImpl.getWifiRepeater();
            }
        }
    }

    private void configureRandomizedMacAddress(WifiConfiguration config) {
        if (config == null) {
            Log.e(TAG, "No config to change MAC address to");
            return;
        }
        MacAddress currentMac = MacAddress.fromString(this.mWifiNative.getMacAddress(this.mInterfaceName));
        MacAddress newMac = config.getOrCreateRandomizedMacAddress();
        this.mWifiConfigManager.setNetworkRandomizedMacAddress(config.networkId, newMac);
        if (!WifiConfiguration.isValidMacAddressForRandomization(newMac)) {
            Log.wtf(TAG, "Config generated an invalid MAC address");
        } else if (currentMac.equals(newMac)) {
            Log.d(TAG, "No changes in MAC address");
        } else {
            this.mWifiMetrics.logStaEvent(17, config);
            boolean setMacSuccess = this.mWifiNative.setMacAddress(this.mInterfaceName, newMac);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ConnectedMacRandomization SSID(");
            stringBuilder.append(config.getPrintableSsid());
            stringBuilder.append("). setMacAddress(");
            stringBuilder.append(newMac.toString());
            stringBuilder.append(") from ");
            stringBuilder.append(currentMac.toString());
            stringBuilder.append(" = ");
            stringBuilder.append(setMacSuccess);
            Log.d(str, stringBuilder.toString());
        }
    }

    private void updateConnectedMacRandomizationSetting() {
        boolean macRandomizationEnabled = true;
        if (this.mFacade.getIntegerSetting(this.mContext, "wifi_connected_mac_randomization_enabled", 0) != 1) {
            macRandomizationEnabled = false;
        }
        this.mEnableConnectedMacRandomization.set(macRandomizationEnabled);
        this.mWifiInfo.setEnableConnectedMacRandomization(macRandomizationEnabled);
        this.mWifiMetrics.setIsMacRandomizationOn(macRandomizationEnabled);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("EnableConnectedMacRandomization Setting changed to ");
        stringBuilder.append(macRandomizationEnabled);
        Log.d(str, stringBuilder.toString());
    }

    public boolean isConnectedMacRandomizationEnabled() {
        return this.mEnableConnectedMacRandomization.get();
    }

    public void failureDetected(int reason) {
        this.mWifiInjector.getSelfRecovery().trigger(2);
    }

    String smToString(Message message) {
        return smToString(message.what);
    }

    String smToString(int what) {
        String s = (String) sSmToString.get(what);
        if (s != null) {
            return s;
        }
        switch (what) {
            case WifiP2pServiceImpl.P2P_CONNECTION_CHANGED /*143371*/:
                s = "P2P_CONNECTION_CHANGED";
                break;
            case WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST /*143372*/:
                s = "WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST";
                break;
            case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE /*143373*/:
                s = "P2P.DISCONNECT_WIFI_RESPONSE";
                break;
            case WifiP2pServiceImpl.SET_MIRACAST_MODE /*143374*/:
                s = "P2P.SET_MIRACAST_MODE";
                break;
            case WifiP2pServiceImpl.BLOCK_DISCOVERY /*143375*/:
                s = "P2P.BLOCK_DISCOVERY";
                break;
            default:
                switch (what) {
                    case WifiMonitor.NETWORK_CONNECTION_EVENT /*147459*/:
                        s = "NETWORK_CONNECTION_EVENT";
                        break;
                    case WifiMonitor.NETWORK_DISCONNECTION_EVENT /*147460*/:
                        s = "NETWORK_DISCONNECTION_EVENT";
                        break;
                    default:
                        switch (what) {
                            case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT /*147462*/:
                                s = "SUPPLICANT_STATE_CHANGE_EVENT";
                                break;
                            case WifiMonitor.AUTHENTICATION_FAILURE_EVENT /*147463*/:
                                s = "AUTHENTICATION_FAILURE_EVENT";
                                break;
                            case WifiMonitor.WPS_SUCCESS_EVENT /*147464*/:
                                s = "WPS_SUCCESS_EVENT";
                                break;
                            case WifiMonitor.WPS_FAIL_EVENT /*147465*/:
                                s = "WPS_FAIL_EVENT";
                                break;
                            default:
                                switch (what) {
                                    case WifiMonitor.ASSOCIATION_REJECTION_EVENT /*147499*/:
                                        s = "ASSOCIATION_REJECTION_EVENT";
                                        break;
                                    case WifiMonitor.ANQP_DONE_EVENT /*147500*/:
                                        s = "WifiMonitor.ANQP_DONE_EVENT";
                                        break;
                                    default:
                                        switch (what) {
                                            case WifiMonitor.GAS_QUERY_START_EVENT /*147507*/:
                                                s = "WifiMonitor.GAS_QUERY_START_EVENT";
                                                break;
                                            case WifiMonitor.GAS_QUERY_DONE_EVENT /*147508*/:
                                                s = "WifiMonitor.GAS_QUERY_DONE_EVENT";
                                                break;
                                            case WifiMonitor.RX_HS20_ANQP_ICON_EVENT /*147509*/:
                                                s = "WifiMonitor.RX_HS20_ANQP_ICON_EVENT";
                                                break;
                                            default:
                                                switch (what) {
                                                    case 151562:
                                                        s = "START_WPS";
                                                        break;
                                                    case 151563:
                                                        s = "START_WPS_SUCCEEDED";
                                                        break;
                                                    case 151564:
                                                        s = "WPS_FAILED";
                                                        break;
                                                    case 151565:
                                                        s = "WPS_COMPLETED";
                                                        break;
                                                    case 151566:
                                                        s = "CANCEL_WPS";
                                                        break;
                                                    case 151567:
                                                        s = "CANCEL_WPS_FAILED";
                                                        break;
                                                    case 151568:
                                                        s = "CANCEL_WPS_SUCCEDED";
                                                        break;
                                                    case 151569:
                                                        s = "WifiManager.DISABLE_NETWORK";
                                                        break;
                                                    default:
                                                        switch (what) {
                                                            case 69632:
                                                                s = "AsyncChannel.CMD_CHANNEL_HALF_CONNECTED";
                                                                break;
                                                            case 69636:
                                                                s = "AsyncChannel.CMD_CHANNEL_DISCONNECTED";
                                                                break;
                                                            case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT /*143361*/:
                                                                s = "GROUP_CREATING_TIMED_OUT";
                                                                break;
                                                            case WifiMonitor.SUP_REQUEST_IDENTITY /*147471*/:
                                                                s = "SUP_REQUEST_IDENTITY";
                                                                break;
                                                            case WifiMonitor.HS20_REMEDIATION_EVENT /*147517*/:
                                                                s = "WifiMonitor.HS20_REMEDIATION_EVENT";
                                                                break;
                                                            case 151553:
                                                                s = "CONNECT_NETWORK";
                                                                break;
                                                            case 151556:
                                                                s = "FORGET_NETWORK";
                                                                break;
                                                            case 151559:
                                                                s = "SAVE_NETWORK";
                                                                break;
                                                            case 151572:
                                                                s = "RSSI_PKTCNT_FETCH";
                                                                break;
                                                            default:
                                                                StringBuilder stringBuilder = new StringBuilder();
                                                                stringBuilder.append("what:");
                                                                stringBuilder.append(Integer.toString(what));
                                                                s = stringBuilder.toString();
                                                                break;
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
        return s;
    }

    private void initializeWpsDetails() {
        StringBuilder stringBuilder;
        String detail = this.mPropertyService.get("ro.product.name", "");
        if (!this.mWifiNative.setDeviceName(this.mInterfaceName, detail)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to set device name ");
            stringBuilder.append(detail);
            loge(stringBuilder.toString());
        }
        detail = this.mPropertyService.get("ro.product.manufacturer", "");
        if (!this.mWifiNative.setManufacturer(this.mInterfaceName, detail)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to set manufacturer ");
            stringBuilder.append(detail);
            loge(stringBuilder.toString());
        }
        detail = this.mPropertyService.get("ro.product.model", "");
        if (!this.mWifiNative.setModelName(this.mInterfaceName, detail)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to set model name ");
            stringBuilder.append(detail);
            loge(stringBuilder.toString());
        }
        detail = this.mPropertyService.get("ro.product.model", "");
        if (!this.mWifiNative.setModelNumber(this.mInterfaceName, detail)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to set model number ");
            stringBuilder.append(detail);
            loge(stringBuilder.toString());
        }
        detail = this.mPropertyService.get("ro.serialno", "");
        if (!this.mWifiNative.setSerialNumber(this.mInterfaceName, detail)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to set serial number ");
            stringBuilder.append(detail);
            loge(stringBuilder.toString());
        }
        if (!this.mWifiNative.setConfigMethods(this.mInterfaceName, "physical_display virtual_push_button")) {
            loge("Failed to set WPS config methods");
        }
        if (!this.mWifiNative.setDeviceType(this.mInterfaceName, this.mPrimaryDeviceType)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to set primary device type ");
            stringBuilder.append(this.mPrimaryDeviceType);
            loge(stringBuilder.toString());
        }
    }

    private void setupClientMode() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setupClientMode() ifacename = ");
        stringBuilder.append(this.mInterfaceName);
        Log.d(str, stringBuilder.toString());
        this.mWifiStateTracker.updateState(0);
        if (this.mWifiConnectivityManager == null) {
            synchronized (this.mWifiReqCountLock) {
                this.mWifiConnectivityManager = this.mWifiInjector.makeWifiConnectivityManager(this.mWifiInfo, hasConnectionRequests());
                this.mWifiConnectivityManager.setUntrustedConnectionAllowed(this.mUntrustedReqCount > 0);
                this.mWifiConnectivityManager.handleScreenStateChanged(this.mScreenOn);
            }
        }
        this.mIpClient = this.mFacade.makeIpClient(this.mContext, this.mInterfaceName, new IpClientCallback());
        this.mIpClient.setMulticastFilter(true);
        registerForWifiMonitorEvents();
        this.mWifiInjector.getWifiLastResortWatchdog().clearAllFailureCounts();
        setSupplicantLogLevel();
        this.mSupplicantStateTracker.sendMessage(CMD_RESET_SUPPLICANT_STATE);
        this.mLastBssid = null;
        this.mLastNetworkId = -1;
        this.mLastSignalLevel = -1;
        this.mWifiInfo.setMacAddress(this.mWifiNative.getMacAddress(this.mInterfaceName));
        if (!this.mWifiConfigManager.migrateFromLegacyStore()) {
            Log.e(TAG, "Failed to migrate from legacy config store");
        }
        sendSupplicantConnectionChangedBroadcast(true);
        this.mWifiNative.setExternalSim(this.mInterfaceName, true);
        setRandomMacOui();
        initializeWpsDetails();
        this.mCountryCode.setReadyForChange(true);
        this.mWifiDiagnostics.startLogging(this.mVerboseLoggingEnabled);
        this.mIsRunning = true;
        updateBatteryWorkSource(null);
        this.mWifiNative.setBluetoothCoexistenceScanMode(this.mInterfaceName, this.mBluetoothConnectionActive);
        setNetworkDetailedState(DetailedState.DISCONNECTED);
        this.mWifiNative.stopFilteringMulticastV4Packets(this.mInterfaceName);
        this.mWifiNative.stopFilteringMulticastV6Packets(this.mInterfaceName);
        WifiNative wifiNative = this.mWifiNative;
        String str2 = this.mInterfaceName;
        boolean z = this.mSuspendOptNeedsDisabled == 0 && this.mUserWantsSuspendOpt.get();
        wifiNative.setSuspendOptimizations(str2, z);
        this.mWifiNative.setPowerSave(this.mInterfaceName, true);
        if (this.mP2pSupported) {
            p2pSendMessage(CMD_ENABLE_P2P);
        }
        this.mWifiNative.enableStaAutoReconnect(this.mInterfaceName, false);
        this.mWifiNative.setConcurrencyPriority(true);
        this.mWifiNative.initPrivFeatureCapability();
    }

    private void stopClientMode() {
        this.mWifiDiagnostics.stopLogging();
        if (this.mP2pSupported) {
            p2pSendMessage(CMD_DISABLE_P2P_REQ);
        }
        this.mIsRunning = false;
        updateBatteryWorkSource(null);
        if (this.mIpClient != null) {
            this.mIpClient.shutdown();
            this.mIpClient.awaitShutdown();
        }
        this.mNetworkInfo.setIsAvailable(false);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
        this.mCountryCode.setReadyForChange(false);
        this.mInterfaceName = null;
        sendSupplicantConnectionChangedBroadcast(false);
    }

    void registerConnected() {
        if (this.mLastNetworkId != -1) {
            this.mWifiConfigManager.updateNetworkAfterConnect(this.mLastNetworkId);
            this.mWifiScoreReport.reset();
            WifiConfiguration currentNetwork = getCurrentWifiConfiguration();
            if (currentNetwork != null && currentNetwork.isPasspoint()) {
                this.mPasspointManager.onPasspointNetworkConnected(currentNetwork.FQDN);
            }
        }
    }

    void registerDisconnected() {
        if (this.mLastNetworkId != -1) {
            this.mWifiConfigManager.updateNetworkAfterDisconnect(this.mLastNetworkId);
            this.mWifiConfigManager.removeAllEphemeralOrPasspointConfiguredNetworks();
        }
    }

    public WifiConfiguration getCurrentWifiConfiguration() {
        if (this.mLastNetworkId == -1) {
            return null;
        }
        return this.mWifiConfigManager.getConfiguredNetwork(this.mLastNetworkId);
    }

    ScanResult getCurrentScanResult() {
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config == null) {
            return null;
        }
        String BSSID = this.mWifiInfo.getBSSID();
        if (BSSID == null) {
            BSSID = this.mTargetRoamBSSID;
        }
        ScanDetailCache scanDetailCache = this.mWifiConfigManager.getScanDetailCacheForNetwork(config.networkId);
        if (scanDetailCache == null) {
            return null;
        }
        return scanDetailCache.getScanResult(BSSID);
    }

    String getCurrentBSSID() {
        return this.mLastBssid;
    }

    public void updateCapabilities() {
        updateCapabilities(getCurrentWifiConfiguration());
    }

    private void updateCapabilities(WifiConfiguration config) {
        if (this.mNetworkAgent != null) {
            NetworkCapabilities result = new NetworkCapabilities(this.mDfltNetworkCapabilities);
            if (this.mWifiInfo == null || this.mWifiInfo.isEphemeral()) {
                result.removeCapability(14);
            } else {
                result.addCapability(14);
            }
            if (this.mWifiInfo == null || WifiConfiguration.isMetered(config, this.mWifiInfo)) {
                result.removeCapability(11);
            } else {
                result.addCapability(11);
            }
            if (this.mWifiInfo == null || this.mWifiInfo.getRssi() == WifiMetrics.MIN_RSSI_DELTA) {
                result.setSignalStrength(Integer.MIN_VALUE);
            } else {
                result.setSignalStrength(this.mWifiInfo.getRssi());
            }
            if (this.mWifiInfo == null || this.mWifiInfo.getSSID().equals("<unknown ssid>")) {
                result.setSSID(null);
            } else {
                result.setSSID(this.mWifiInfo.getSSID());
            }
            this.mNetworkAgent.sendNetworkCapabilities(result);
        }
    }

    private ProxyInfo getProxyProperties() {
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (config == null) {
            return null;
        }
        return config.getHttpProxy();
    }

    private boolean isProviderOwnedNetwork(int networkId, String providerFqdn) {
        if (networkId == -1) {
            return false;
        }
        WifiConfiguration config = this.mWifiConfigManager.getConfiguredNetwork(networkId);
        if (config == null) {
            return false;
        }
        return TextUtils.equals(config.FQDN, providerFqdn);
    }

    private void handleEapAuthFailure(int networkId, int errorCode) {
        WifiConfiguration targetedNetwork = this.mWifiConfigManager.getConfiguredNetwork(this.mTargetNetworkId);
        if (targetedNetwork != null) {
            switch (targetedNetwork.enterpriseConfig.getEapMethod()) {
                case 4:
                case 5:
                case 6:
                    if (errorCode == 16385) {
                        getTelephonyManager().resetCarrierKeysForImsiEncryption();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    void unwantedNetwork(int reason) {
        sendMessage(CMD_UNWANTED_NETWORK, reason);
    }

    void doNetworkStatus(int status) {
        sendMessage(CMD_NETWORK_STATUS, status);
    }

    private String buildIdentity(int eapMethod, String imsi, String mccMnc) {
        if (imsi == null || imsi.isEmpty()) {
            return "";
        }
        String prefix;
        String mnc;
        String mcc;
        StringBuilder stringBuilder;
        if (eapMethod == 4) {
            prefix = "1";
        } else if (eapMethod == 5) {
            prefix = "0";
        } else if (eapMethod != 6) {
            return "";
        } else {
            prefix = "6";
        }
        if (mccMnc == null || mccMnc.isEmpty()) {
            String substring = imsi.substring(0, 3);
            mnc = imsi.substring(3, 6);
            mcc = substring;
        } else {
            mcc = mccMnc.substring(0, 3);
            mnc = mccMnc.substring(3);
            if (mnc.length() == 2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("0");
                stringBuilder.append(mnc);
                mnc = stringBuilder.toString();
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(imsi);
        stringBuilder.append("@wlan.mnc");
        stringBuilder.append(mnc);
        stringBuilder.append(".mcc");
        stringBuilder.append(mcc);
        stringBuilder.append(".3gppnetwork.org");
        return stringBuilder.toString();
    }

    @VisibleForTesting
    public boolean shouldEvaluateWhetherToSendExplicitlySelected(WifiConfiguration currentConfig) {
        boolean z = false;
        if (currentConfig == null) {
            Log.wtf(TAG, "Current WifiConfiguration is null, but IP provisioning just succeeded");
            return false;
        }
        long currentTimeMillis = this.mClock.getElapsedSinceBootMillis();
        if (this.mWifiConfigManager.getLastSelectedNetwork() == currentConfig.networkId && currentTimeMillis - this.mWifiConfigManager.getLastSelectedTimeStamp() < 30000) {
            z = true;
        }
        return z;
    }

    private void sendConnectedState() {
        WifiConfiguration config = getCurrentWifiConfiguration();
        if (shouldEvaluateWhetherToSendExplicitlySelected(config)) {
            StringBuilder stringBuilder;
            notifyNetworkUserConnect(true);
            if (this.mVerboseLoggingEnabled) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Network selected by UID ");
                stringBuilder.append(config.lastConnectUid);
                stringBuilder.append(" prompt=");
                stringBuilder.append(true);
                log(stringBuilder.toString());
            }
            if (1 != null) {
                if (this.mVerboseLoggingEnabled) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("explictlySelected acceptUnvalidated=");
                    stringBuilder.append(config.noInternetAccessExpected);
                    log(stringBuilder.toString());
                }
                if (this.mNetworkAgent != null) {
                    this.mNetworkAgent.explicitlySelected(config.noInternetAccessExpected);
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("duplexSelected connectToCellularAndWLAN=");
                    stringBuilder2.append(config.connectToCellularAndWLAN);
                    Log.d(str, stringBuilder2.toString());
                }
            }
        }
        if (!(this.mNetworkAgent == null || config == null)) {
            this.mNetworkAgent.duplexSelected(config.connectToCellularAndWLAN, config.noInternetAccessExpected);
        }
        setNetworkDetailedState(DetailedState.CONNECTED);
        sendNetworkStateChangeBroadcast(this.mLastBssid);
    }

    public void triggerUpdateAPInfo() {
        Log.d(TAG, "triggerUpdateAPInfo");
    }

    private void replyToMessage(Message msg, int what) {
        if (msg.replyTo != null) {
            this.mReplyChannel.replyToMessage(msg, obtainMessageWithWhatAndArg2(msg, what));
        }
    }

    private void replyToMessage(Message msg, int what, int arg1) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessageWithWhatAndArg2(msg, what);
            dstMsg.arg1 = arg1;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private void replyToMessage(Message msg, int what, Object obj) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessageWithWhatAndArg2(msg, what);
            dstMsg.obj = obj;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private Message obtainMessageWithWhatAndArg2(Message srcMsg, int what) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg2 = srcMsg.arg2;
        return msg;
    }

    private void broadcastWifiCredentialChanged(int wifiCredentialEventType, WifiConfiguration config) {
        if (config != null && config.preSharedKey != null) {
            Intent intent = new Intent("android.net.wifi.WIFI_CREDENTIAL_CHANGED");
            intent.putExtra(SupplicantBackupMigration.SUPPLICANT_KEY_SSID, config.SSID);
            intent.putExtra("et", wifiCredentialEventType);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, "android.permission.RECEIVE_WIFI_CREDENTIAL_CHANGE");
        }
    }

    private void broadcastWifiDriverChanged(int state) {
        if (this.isBootCompleted) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("broadcastWifiDriverChanged statte : ");
            stringBuilder.append(state);
            logd(stringBuilder.toString());
            Intent intent = new Intent(WIFI_DRIVER_CHANGE_ACTION);
            intent.putExtra(WIFI_DRIVER_STATE, state);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, WIFI_DRIVER_CHANGE_PERMISSION);
        }
    }

    void handleGsmAuthRequest(SimAuthRequestData requestData) {
        if (this.targetWificonfiguration == null || this.targetWificonfiguration.networkId == requestData.networkId) {
            logd("id matches targetWifiConfiguration");
            String response = TelephonyUtil.getGsmSimAuthResponse(requestData.data, getTelephonyManager());
            if (response == null) {
                this.mWifiNative.simAuthFailedResponse(this.mInterfaceName, requestData.networkId);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Supplicant Response -");
                stringBuilder.append(response);
                logv(stringBuilder.toString());
                this.mWifiNative.simAuthResponse(this.mInterfaceName, requestData.networkId, WifiNative.SIM_AUTH_RESP_TYPE_GSM_AUTH, response);
            }
            return;
        }
        logd("id does not match targetWifiConfiguration");
    }

    void handle3GAuthRequest(SimAuthRequestData requestData) {
        if (this.targetWificonfiguration == null || this.targetWificonfiguration.networkId == requestData.networkId) {
            logd("id matches targetWifiConfiguration");
            SimAuthResponseData response = TelephonyUtil.get3GAuthResponse(requestData, getTelephonyManager());
            if (response != null) {
                this.mWifiNative.simAuthResponse(this.mInterfaceName, requestData.networkId, response.type, response.response);
            } else {
                this.mWifiNative.umtsAuthFailedResponse(this.mInterfaceName, requestData.networkId);
            }
            return;
        }
        logd("id does not match targetWifiConfiguration");
    }

    public void startConnectToNetwork(int networkId, int uid, String bssid) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(CONNECT_FROM_USER, false);
        bundle.putString(BSSID_TO_CONNECT, bssid);
        sendMessage(CMD_START_CONNECT, networkId, uid, bundle);
    }

    public void startConnectToUserSelectNetwork(int networkId, int uid, String bssid) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(CONNECT_FROM_USER, true);
        bundle.putString(BSSID_TO_CONNECT, bssid);
        if (this.mNetworkAgent != null || hasConnectionRequests() || this.mNetworkFactory == null || this.mNetworkFactory.hasMessages(536576)) {
            sendMessage(CMD_START_CONNECT, networkId, uid, bundle);
            return;
        }
        Log.w(TAG, "delay connect request");
        sendMessageDelayed(CMD_START_CONNECT, networkId, uid, bundle, 50);
    }

    public void startRoamToNetwork(int networkId, ScanResult scanResult) {
        sendMessage(CMD_START_ROAM, networkId, 0, scanResult);
    }

    public void enableWifiConnectivityManager(boolean enabled) {
        sendMessage(CMD_ENABLE_WIFI_CONNECTIVITY_MANAGER, enabled);
    }

    static boolean unexpectedDisconnectedReason(int reason) {
        return reason == 2 || reason == 6 || reason == 7 || reason == 8 || reason == 9 || reason == 14 || reason == 15 || reason == 16 || reason == 18 || reason == 19 || reason == 23 || reason == 34;
    }

    private boolean disassociatedReason(int reason) {
        return reason == 2 || reason == 4 || reason == 5 || reason == 8 || reason == 34;
    }

    public int getWifiApTypeFromMpLink() {
        return 0;
    }

    public void updateWifiMetrics() {
        this.mWifiMetrics.updateSavedNetworks(this.mWifiConfigManager.getSavedNetworks());
        this.mPasspointManager.updateMetrics();
    }

    private boolean deleteNetworkConfigAndSendReply(Message message, boolean calledFromForget) {
        boolean success = this.mWifiConfigManager.removeNetwork(message.arg1, message.sendingUid);
        if (!success) {
            loge("Failed to remove network");
        } else if (this.mLastConnectConfig != null && this.mLastConnectConfig.networkId == message.arg1) {
            log("delete network, set mLastConnectConfig null");
            setLastConnectConfig(null);
        }
        if (calledFromForget) {
            if (success) {
                replyToMessage(message, 151558);
                broadcastWifiCredentialChanged(1, (WifiConfiguration) message.obj);
                return true;
            }
            replyToMessage(message, 151557, 0);
            return false;
        } else if (success) {
            replyToMessage(message, message.what, 1);
            return true;
        } else {
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_FAIL;
            replyToMessage(message, message.what, -1);
            return false;
        }
    }

    private NetworkUpdateResult saveNetworkConfigAndSendReply(Message message) {
        WifiConfiguration config = message.obj;
        if (config == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SAVE_NETWORK with null configuration ");
            stringBuilder.append(this.mSupplicantStateTracker.getSupplicantStateName());
            stringBuilder.append(" my state ");
            stringBuilder.append(getCurrentState().getName());
            loge(stringBuilder.toString());
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_FAIL;
            replyToMessage(message, 151560, 0);
            return new NetworkUpdateResult(-1);
        }
        NetworkUpdateResult result = this.mWifiConfigManager.addOrUpdateNetwork(config, message.sendingUid);
        if (!result.isSuccess()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SAVE_NETWORK adding/updating config=");
            stringBuilder2.append(config);
            stringBuilder2.append(" failed");
            loge(stringBuilder2.toString());
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_FAIL;
            replyToMessage(message, 151560, 0);
            return result;
        } else if (this.mWifiConfigManager.enableNetwork(result.getNetworkId(), false, message.sendingUid)) {
            broadcastWifiCredentialChanged(0, config);
            replyToMessage(message, 151561);
            return result;
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("SAVE_NETWORK enabling config=");
            stringBuilder3.append(config);
            stringBuilder3.append(" failed");
            loge(stringBuilder3.toString());
            this.messageHandlingStatus = MESSAGE_HANDLING_STATUS_FAIL;
            replyToMessage(message, 151560, 0);
            return new NetworkUpdateResult(-1);
        }
    }

    private static String getLinkPropertiesSummary(LinkProperties lp) {
        List<String> attributes = new ArrayList(6);
        if (lp.hasIPv4Address()) {
            attributes.add("v4");
        }
        if (lp.hasIPv4DefaultRoute()) {
            attributes.add("v4r");
        }
        if (lp.hasIPv4DnsServer()) {
            attributes.add("v4dns");
        }
        if (lp.hasGlobalIPv6Address()) {
            attributes.add("v6");
        }
        if (lp.hasIPv6DefaultRoute()) {
            attributes.add("v6r");
        }
        if (lp.hasIPv6DnsServer()) {
            attributes.add("v6dns");
        }
        return TextUtils.join(" ", attributes);
    }

    private String getTargetSsid() {
        WifiConfiguration currentConfig = this.mWifiConfigManager.getConfiguredNetwork(this.mTargetNetworkId);
        if (currentConfig != null) {
            return currentConfig.SSID;
        }
        return null;
    }

    private boolean p2pSendMessage(int what) {
        if (this.mWifiP2pChannel == null) {
            return false;
        }
        this.mWifiP2pChannel.sendMessage(what);
        return true;
    }

    private boolean p2pSendMessage(int what, int arg1) {
        if (this.mWifiP2pChannel == null) {
            return false;
        }
        this.mWifiP2pChannel.sendMessage(what, arg1);
        return true;
    }

    private boolean hasConnectionRequests() {
        return this.mConnectionReqCount > 0 || this.mUntrustedReqCount > 0;
    }

    public boolean getIpReachabilityDisconnectEnabled() {
        return this.mIpReachabilityDisconnectEnabled;
    }

    public void setIpReachabilityDisconnectEnabled(boolean enabled) {
        this.mIpReachabilityDisconnectEnabled = enabled;
    }

    public boolean syncInitialize(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_INITIALIZE);
        boolean result = resultMsg.arg1 != -1;
        resultMsg.recycle();
        return result;
    }

    void sendScanResultsAvailableBroadcast(boolean scanSucceeded) {
        Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
        intent.addFlags(67108864);
        intent.putExtra("resultsUpdated", scanSucceeded);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public boolean disableWifiFilter() {
        return this.mWifiNative.stopRxFilter(this.mInterfaceName);
    }

    public boolean enableWifiFilter() {
        return this.mWifiNative.startRxFilter(this.mInterfaceName);
    }

    public void notifyEnableSameNetworkId(int netId) {
    }

    public boolean reportWifiScoreDelayed() {
        return false;
    }

    public void saveConnectingNetwork(WifiConfiguration config, int netId, boolean autoJoin) {
    }

    public void notifyWifiConnFailedInfo(int netId, String bssid, int rssi, int reason, WifiConnectivityManager wcm) {
    }

    public void notifyNetworkUserConnect(boolean isUserConnect) {
    }

    public void initialFeatureSet(int featureSet) {
        this.mFeatureSet = featureSet;
    }

    public void saveLastNetIdForAp() {
        if (this.mWifiInfo != null) {
            this.mWifiInfo.setLastNetIdForAp(this.mWifiInfo.getNetworkId());
        }
    }

    public void clearLastNetIdForAp() {
        if (this.mWifiInfo != null) {
            this.mWifiInfo.setLastNetIdForAp(-1);
        }
    }

    private void setLastConnectConfig(WifiConfiguration config) {
        this.mLastConnectConfig = config;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set mLastConnectConfig, isPortalConnect:");
        stringBuilder.append(config != null ? Boolean.valueOf(config.isPortalConnect) : null);
        log(stringBuilder.toString());
    }

    public void updateLastPortalConnect(WifiConfiguration config) {
        if (this.mLastConnectConfig != null && config != null && config.networkId == this.mLastConnectConfig.networkId) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateLastPortalConnect: isPortalConnect-->");
            stringBuilder.append(config.isPortalConnect);
            log(stringBuilder.toString());
            this.mLastConnectConfig.isPortalConnect = config.isPortalConnect;
        }
    }

    public boolean isPortalConnectLast() {
        if (this.mLastConnectConfig != null) {
            return this.mLastConnectConfig.isPortalConnect;
        }
        return false;
    }
}
