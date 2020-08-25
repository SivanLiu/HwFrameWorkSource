package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hdm.HwDeviceManager;
import android.net.wifi.HwQoE.IHwQoECallback;
import android.net.wifi.IWifiActionListener;
import android.net.wifi.IWifiRepeaterConfirmListener;
import android.net.wifi.PPPOEConfig;
import android.net.wifi.PPPOEInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiDetectConfInfo;
import android.net.wifi.WifiDeviceConfig;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.rms.iaware.NetLocationStrategy;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.util.wifi.HwHiLog;
import android.util.wifi.HwHiSLog;
import android.widget.Toast;
import com.android.internal.util.AsyncChannel;
import com.android.server.HwServiceFactory;
import com.android.server.PPPOEStateMachine;
import com.android.server.hidata.HwHidataManager;
import com.android.server.hidata.arbitration.HwArbitrationManager;
import com.android.server.hidata.wavemapping.dataprovider.FrequentLocation;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifi.MSS.HwMSSHandler;
import com.android.server.wifi.dc.DCController;
import com.android.server.wifi.dc.DCUtils;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifi.wifipro.HwWifiProServiceManager;
import com.android.server.wifi.wifipro.HwWifiProServiceProxy;
import com.android.server.wifipro.WifiProCommonUtils;
import com.hisi.mapcon.IMapconService;
import com.hisi.mapcon.IMapconServiceCallback;
import com.huawei.utils.reflect.EasyInvokeFactory;
import com.mediatek.ims.impl.IMtkMapconService;
import com.mediatek.ims.impl.IMtkMapconServiceCallback;
import huawei.android.security.IHwBehaviorCollectManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwWifiService extends WifiServiceImpl {
    private static final String ACCESS_WIFI_FILTER_PERMISSION = "com.huawei.wifi.permission.ACCESS_FILTER";
    private static final String ACCESS_WIFI_PERMISSION = "huawei.permission.ACCESS_WIFI_SERVICE";
    private static final String[] ACCESS_WIFI_WHITELIST = {"com.huawei.smarthome"};
    private static final String ACTION_VOWIFI_STARTED = "com.hisi.vowifi.started";
    private static final String ACTION_VOWIFI_STARTED_MTK = "com.huawei.vowifi.started";
    private static final int BACKGROUND_IMPORTANCE_CUTOFF = 125;
    private static final int BOTH_2G_AND_5G = 2;
    private static final int CHIP_PLATFORM_HISI = 1;
    private static final int CHIP_PLATFORM_MTK = 2;
    private static final int CODE_BIND_MPLINK = 5002;
    private static final int CODE_CONFIRM_WIFI_REPEATER = 1022;
    private static final int CODE_CTRL_HW_WIFI_NETWORK = 1017;
    private static final int CODE_DISABLE_RX_FILTER = 3021;
    private static final int CODE_ENABLE_HILINK_HANDSHAKE = 2001;
    private static final int CODE_ENABLE_RX_FILTER = 3022;
    private static final int CODE_ENABLE_WIFICHIP_CHECK = 4021;
    private static final int CODE_EXTEND_WIFI_SCAN_PERIOD_FOR_P2P = 2006;
    private static final int CODE_GET_APLINKED_STA_LIST = 1005;
    private static final int CODE_GET_CONNECTION_RAW_PSK = 2002;
    private static final int CODE_GET_PPPOE_INFO_CONFIG = 1004;
    private static final int CODE_GET_RSDB_SUPPORTED_MODE = 2008;
    private static final int CODE_GET_SINGNAL_INFO = 1011;
    private static final int CODE_GET_SOFTAP_BANDWIDTH = 4012;
    private static final int CODE_GET_SOFTAP_CHANNEL_LIST = 1009;
    private static final int CODE_GET_VOWIFI_DETECT_MODE = 1013;
    private static final int CODE_GET_VOWIFI_DETECT_PERIOD = 1015;
    private static final int CODE_GET_WIFI_REPEATER_MODE = 1021;
    private static final int CODE_GET_WPA_SUPP_CONFIG = 1001;
    private static final int CODE_IS_BG_LIMIT_ALLOWED = 3008;
    private static final int CODE_IS_FEATURE_SUPPORTED = 4011;
    private static final int CODE_IS_IN_MPLINK_STATE = 5003;
    private static final int CODE_IS_SUPPORT_VOWIFI_DETECT = 1016;
    private static final int CODE_PROXY_WIFI_LOCK = 3009;
    private static final int CODE_REPORT_SPEED_RESULT = 4103;
    private static final int CODE_REQUEST_FRESH_WHITE_LIST = 2007;
    private static final int CODE_REQUEST_WIFI_ENABLE = 2004;
    private static final int CODE_RESTRICT_WIFI_SCAN = 4001;
    private static final int CODE_SET_FEM_TXPOWER = 4013;
    private static final int CODE_SET_PERFORMANCE_MODE = 4102;
    private static final int CODE_SET_SOFTAP_DISASSOCIATESTA = 1007;
    private static final int CODE_SET_SOFTAP_MACFILTER = 1006;
    private static final int CODE_SET_VOWIFI_DETECT_MODE = 1012;
    private static final int CODE_SET_VOWIFI_DETECT_PERIOD = 1014;
    private static final int CODE_SET_WIFI_ANTSET = 3007;
    private static final int CODE_SET_WIFI_AP_EVALUATE_ENABLED = 1010;
    private static final int CODE_SET_WIFI_MODE = 4111;
    private static final int CODE_SET_WIFI_TXPOWER = 2005;
    private static final int CODE_START_PPPOE_CONFIG = 1002;
    private static final int CODE_START_WIFI_KEEP_ALIVE = 3023;
    private static final int CODE_STOP_PPPOE_CONFIG = 1003;
    private static final int CODE_STOP_WIFI_KEEP_ALIVE = 3024;
    private static final int CODE_UPDATE_APP_EXPERIENCE_STATUS = 3006;
    private static final int CODE_UPDATE_APP_RUNNING_STATUS = 3005;
    private static final int CODE_UPDATE_LIMIT_SPEED_STATUS = 3025;
    private static final int CODE_UPDATE_WM_FREQ_LOC = 4002;
    private static final int CODE_USER_HANDOVER_WIFI = 1008;
    private static final int CODE_WIFI_DC_CONNECT = 4014;
    private static final int CODE_WIFI_DC_CONNECT_Z = 5004;
    private static final int CODE_WIFI_DC_DISCONNECT = 4015;
    private static final int CODE_WIFI_IS_DC_ACTIVE = 5005;
    private static final int CODE_WIFI_QOE_EVALUATE = 3003;
    private static final int CODE_WIFI_QOE_START_MONITOR = 3001;
    private static final int CODE_WIFI_QOE_STOP_MONITOR = 3002;
    private static final int CODE_WIFI_QOE_UPDATE_STATUS = 3004;
    private static final String CONF_FILE_NAME = "/system/etc/xml/wifi_factory_mac_whitelist.xml";
    private static final boolean DBG = true;
    private static final int DEFAULT_VALUE = 0;
    private static final String DESCRIPTOR = "android.net.wifi.IWifiManager";
    private static final int FACTORY_MAC_NO_CHECK_VERSION = -1;
    private static final int[] FREQUENCYS = {2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462, 2467, 2472};
    private static final String GNSS_LOCATION_FIX_STATUS = "GNSS_LOCATION_FIX_STATUS";
    private static final String KEY_APK_NAME = "Apk";
    private static final String KEY_BAND_CHANGE = "Band";
    private static final String KEY_BLACKLIST_CHANGE = "IsBlacklistChanged";
    private static final String KEY_CHANNEL_CHANGE = "Channel";
    private static final String KEY_PASSWORD_CHANGE = "IsPwdChanged";
    private static final String KEY_SECURITY_TYPE = "SecurityType";
    private static final String KEY_SSID_CHANGE = "IsSsidChanged";
    private static final long LOG_SCAN_RESULTS_INTERVAL_MS = 3000;
    private static final int MAPCON_SERVICE_SHUTDOWN_TIMEOUT = 5000;
    private static final int MSG_AIRPLANE_TOGGLED_MAPCON_TIMEOUT = 1;
    private static final int MSG_DISABLE_WIFI_MAPCON_TIMEOUT = 2;
    private static final int MSG_FORGET_NETWORK_MAPCON_TIMEOUT = 0;
    private static final int ONLY_2G = 1;
    private static final String OWE_TRANSITION_IN_CAPABILITY = "OWE_TRANSITION";
    private static final String PERMISSION_SET_WIFI_MODE_ASSISTANT = "com.huawei.permission.ASSOCIATE_ASSISTANT_SET_WIFI_MODE";
    private static final String PERMISSION_SET_WIFI_MODE_HWSHARE = "com.huawei.permission.HWSHARE_SET_WIFI_MODE";
    private static final String PG_AR_STATE_ACTION = "com.huawei.intent.action.PG_AR_STATE_ACTION";
    private static final String PG_RECEIVER_PERMISSION = "com.huawei.powergenie.receiverPermission";
    private static final String PMFR_IN_CAPABILITY = "PMFR";
    private static final String PPPOE_TAG = "PPPOEWIFIService";
    private static final String PROCESS_BD = "com.baidu.map.location";
    private static final String PROCESS_GD = "com.amap.android.ams";
    private static final String QTTFF_WIFI_SCAN_ENABLED = "qttff_wifi_scan_enabled";
    private static final int QTTFF_WIFI_SCAN_INTERVAL_MS = 5000;
    private static final int SCANRESULTS_COUNT_MAX = 200;
    private static final String SECURITY_NO_CHANGE = "NoChange";
    private static final int SERVER_CODE_CONTROL_HIDATA_OPTIMIZE = 3031;
    public static final boolean SHOULD_NETWORK_SHARING_INTEGRATION;
    private static final String TAG = "HwWifiService";
    private static final int TYPE_CHANNELS_BW160 = 1;
    private static final String VOWIFI_WIFI_DETECT_PERMISSION = "com.huawei.permission.VOWIFI_WIFI_DETECT";
    public static final int WHITE_LIST_TYPE_WIFI_SLEEP = 7;
    private static final int WIFISCANSTRATEGY_ALLOWABLE = 0;
    private static final int WIFISCANSTRATEGY_FORBIDDEN = -1;
    private static final int WIFI_MODE_ASSOCIATE_ASSISTANTE = 1002;
    private static final int WIFI_MODE_ASSOCIATE_ASSISTANTE_SUCC = 1003;
    private static final int WIFI_MODE_HWSHARE_LARGE_FILE = 1005;
    private static final int WIFI_MODE_HWSHARE_LARGE_FILE_SUCC = 1006;
    private static final int WIFI_MODE_SET_LISTEN_MODE = 1004;
    private static final String[] WIFI_MODE_WHITE_PACKAGE_LIST = {"com.huawei.associateassistant", "com.huawei.pcassistant", "com.huawei.android.instantshare"};
    private static final String XML_TAG_MAC_WHITELIST = "mac_whitelist";
    private static final String XML_TAG_PACKAGE = "package";
    private static final String XML_TAG_VERSION = "version_number";
    private static final String XML_TAG_VERSION_MAX = "version_max";
    private static final String XML_TAG_VERSION_MIN = "version_min";
    private static final Object mWifiLock = new Object();
    private static HashSet<String> restrictWifiScanPkgSet = new HashSet<>();
    /* access modifiers changed from: private */
    public static WifiServiceUtils wifiServiceUtils = EasyInvokeFactory.getInvokeUtils(WifiServiceUtils.class);
    private static WifiStateMachineUtils wifiStateMachineUtils = EasyInvokeFactory.getInvokeUtils(WifiStateMachineUtils.class);
    private final ServiceConnection conn;
    private boolean isPPPOE;
    private volatile boolean isRxFilterDisabled;
    /* access modifiers changed from: private */
    public long lastScanResultsAvailableTime;
    private final ActivityManager mActivityManager;
    private final IMapconServiceCallback mAirPlaneCallback;
    private final AppOpsManager mAppOps;
    private final IMapconServiceCallback mCallback;
    /* access modifiers changed from: private */
    public int mChipPlatform;
    /* access modifiers changed from: private */
    public final Clock mClock;
    /* access modifiers changed from: private */
    public Context mContext;
    Map<String, FactoryMacWhiteList> mFactoryMacWhiteListMap;
    private List<HwFilterLock> mFilterLockList;
    private final Object mFilterSynchronizeLock;
    private Message mForgetNetworkMsg;
    /* access modifiers changed from: private */
    public boolean mHasScanned;
    private IHwBehaviorCollectManager mHwBehaviorManager;
    private HwWifiCHRService mHwWifiChrService;
    private HwWifiProServiceManager mHwWifiProServiceManager;
    private HwWifiProServiceProxy mHwWifiProServiceProxy;
    /* access modifiers changed from: private */
    public boolean mIsAbsoluteRest;
    private long mLastLogScanResultsTime;
    private String mMacFilterRecord;
    /* access modifiers changed from: private */
    public Handler mMapconHandler;
    /* access modifiers changed from: private */
    public HandlerThread mMapconHandlerTread;
    /* access modifiers changed from: private */
    public IMapconService mMapconService;
    private final IMtkMapconServiceCallback mMtkAirPlaneCallback;
    private final IMtkMapconServiceCallback mMtkCallback;
    /* access modifiers changed from: private */
    public IMtkMapconService mMtkMapconService;
    private Handler mNetworkResetHandler;
    private PPPOEStateMachine mPPPOEStateMachine;
    /* access modifiers changed from: private */
    public int mPluggedType;
    private PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public boolean mVowifiServiceOn;
    private int mWifiMode;
    private final ArraySet<String> mWifiScanBlacklist;

    static {
        boolean z = true;
        if (!HwSoftApManager.shouldUseLiteUi() || !SystemProperties.getBoolean("ro.config.hw_wifibridge", false) || !SystemProperties.getBoolean("ro.feature.mobile_network_sharing_integration", true)) {
            z = false;
        }
        SHOULD_NETWORK_SHARING_INTEGRATION = z;
    }

    public HwWifiService(Context context, WifiInjector wifiInjector, AsyncChannel asyncChannel) {
        super(context, wifiInjector, asyncChannel);
        this.isPPPOE = SystemProperties.getInt("ro.config.pppoe_enable", 0) != 1 ? false : true;
        this.lastScanResultsAvailableTime = 0;
        this.mWifiScanBlacklist = new ArraySet<>();
        this.mIsAbsoluteRest = false;
        this.mHasScanned = false;
        this.mFactoryMacWhiteListMap = new HashMap();
        this.mHwWifiChrService = null;
        this.mMacFilterRecord = "";
        this.mWifiMode = 0;
        this.mMapconService = null;
        this.mMtkMapconService = null;
        this.mFilterLockList = new ArrayList();
        this.isRxFilterDisabled = false;
        this.mFilterSynchronizeLock = new Object();
        this.conn = new ServiceConnection() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass5 */

            public void onServiceDisconnected(ComponentName name) {
                HwHiLog.d(HwWifiService.TAG, false, "onServiceDisconnected,IMapconService", new Object[0]);
                IMapconService unused = HwWifiService.this.mMapconService = null;
                if (HwWifiService.this.mChipPlatform == 2) {
                    HwWifiService.this.onVoWifiCloseForServiceDisconnect();
                    IMtkMapconService unused2 = HwWifiService.this.mMtkMapconService = null;
                }
                boolean unused3 = HwWifiService.this.mVowifiServiceOn = false;
                HwWifiService.this.mMapconHandlerTread.quit();
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                HwHiLog.d(HwWifiService.TAG, false, "onServiceConnected,IMapconService", new Object[0]);
                HandlerThread unused = HwWifiService.this.mMapconHandlerTread = new HandlerThread("MapconHandler");
                HwHiLog.d(HwWifiService.TAG, false, "onServiceConnected,IMapconService and chip platform is %{public}d ", new Object[]{Integer.valueOf(HwWifiService.this.mChipPlatform)});
                if (HwWifiService.this.mChipPlatform == 1) {
                    IMapconService unused2 = HwWifiService.this.mMapconService = IMapconService.Stub.asInterface(service);
                } else if (HwWifiService.this.mChipPlatform == 2) {
                    IMtkMapconService unused3 = HwWifiService.this.mMtkMapconService = IMtkMapconService.Stub.asInterface(service);
                } else {
                    HwHiLog.d(HwWifiService.TAG, false, "onServiceConnected,chip platform is not hisi or mtk ", new Object[0]);
                    return;
                }
                HwWifiService.this.mMapconHandlerTread.start();
                HwWifiService hwWifiService = HwWifiService.this;
                Handler unused4 = hwWifiService.mMapconHandler = new Handler(hwWifiService.mMapconHandlerTread.getLooper()) {
                    /* class com.android.server.wifi.HwWifiService.AnonymousClass5.AnonymousClass1 */

                    public void handleMessage(Message msg) {
                        HwHiLog.d(HwWifiService.TAG, false, "handle TimeoutMessage,msg:%{public}d", new Object[]{Integer.valueOf(msg.what)});
                        WifiController controller = HwWifiService.wifiServiceUtils.getWifiController(HwWifiService.this);
                        int i = msg.what;
                        if (i == 0) {
                            HwWifiService.this.mClientModeImpl.sendMessage(Message.obtain((Message) msg.obj));
                        } else if (i != 1) {
                            if (i == 2 && controller != null) {
                                controller.sendMessage(155656);
                            }
                        } else if (controller != null) {
                            controller.sendMessage(155657);
                        }
                    }
                };
                boolean unused5 = HwWifiService.this.mVowifiServiceOn = true;
            }
        };
        this.mCallback = new IMapconServiceCallback.Stub() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass6 */

            public void onVoWifiCloseDone() {
                HwWifiService.this.onVoWifiCloseDoneForToggled();
            }
        };
        this.mMtkCallback = new IMtkMapconServiceCallback.Stub() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass7 */

            public void onVoWifiCloseDone() {
                HwWifiService.this.onVoWifiCloseDoneForToggled();
            }
        };
        this.mAirPlaneCallback = new IMapconServiceCallback.Stub() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass8 */

            public void onVoWifiCloseDone() {
                HwWifiService.this.onVoWifiCloseDoneForAirplaneToggled();
            }
        };
        this.mMtkAirPlaneCallback = new IMtkMapconServiceCallback.Stub() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass9 */

            public void onVoWifiCloseDone() {
                HwWifiService.this.onVoWifiCloseDoneForAirplaneToggled();
            }
        };
        this.mNetworkResetHandler = new Handler();
        this.mLastLogScanResultsTime = 0;
        this.mContext = context;
        parseFactoryMacWhiteListFile(this.mContext);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mHwWifiChrService = HwWifiServiceFactory.getHwWifiCHRService();
        if (this.isPPPOE) {
            this.mPPPOEStateMachine = new PPPOEStateMachine(this.mContext, PPPOE_TAG);
            this.mPPPOEStateMachine.start();
        }
        this.mHwWifiProServiceManager = HwWifiProServiceManager.createHwWifiProServiceManager(context);
        this.mClock = wifiInjector.getClock();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                if ("android.net.wifi.SCAN_RESULTS".equals(intent.getAction()) && intent.getBooleanExtra("resultsUpdated", false)) {
                    HwWifiService hwWifiService = HwWifiService.this;
                    long unused = hwWifiService.lastScanResultsAvailableTime = hwWifiService.mClock.getElapsedSinceBootMillis();
                }
            }
        }, filter);
        loadWifiScanBlacklist();
        BackgroundAppScanManager.getInstance().registerBlackListChangeListener(new BlacklistListener() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass2 */

            public void onBlacklistChange(List<String> list) {
                HwWifiService.this.updateWifiScanblacklist();
            }
        });
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass3 */

            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    boolean unused = HwWifiService.this.mIsAbsoluteRest = intent.getBooleanExtra("stationary", false);
                    HwHiLog.d(HwWifiService.TAG, false, "mIsAbsoluteRest = %{public}s", new Object[]{String.valueOf(HwWifiService.this.mIsAbsoluteRest)});
                    if (HwWifiService.this.mIsAbsoluteRest) {
                        boolean unused2 = HwWifiService.this.mHasScanned = false;
                    }
                }
            }
        }, new IntentFilter(PG_AR_STATE_ACTION), PG_RECEIVER_PERMISSION, null);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass4 */

            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    int unused = HwWifiService.this.mPluggedType = intent.getIntExtra("plugged", 0);
                    if (HwWifiService.this.mPluggedType == 0 && (HwWifiService.this.mClientModeImpl instanceof HwWifiStateMachine) && HwWifiService.this.mClientModeImpl.getChargingState()) {
                        int unused2 = HwWifiService.this.mPluggedType = 2;
                    }
                    HwHiLog.d(HwWifiService.TAG, false, "mPluggedType = %{public}d", new Object[]{Integer.valueOf(HwWifiService.this.mPluggedType)});
                }
            }
        }, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", "WifiService");
    }

    /* access modifiers changed from: protected */
    public boolean enforceStopScanSreenOff() {
        if (this.mPowerManager.isScreenOn() || "com.huawei.ca".equals(getAppName(Binder.getCallingPid()))) {
            return false;
        }
        HwHiSLog.i(TAG, false, "Screen is off, %{public}s startScan is skipped.", new Object[]{getAppName(Binder.getCallingPid())});
        return true;
    }

    private void restrictWifiScan(List<String> pkgs, Boolean restrict) {
        if (restrict.booleanValue()) {
            restrictWifiScanPkgSet.addAll(pkgs);
        } else if (pkgs == null) {
            restrictWifiScanPkgSet.clear();
        } else {
            restrictWifiScanPkgSet.removeAll(pkgs);
        }
    }

    /* access modifiers changed from: protected */
    public boolean restrictWifiScanRequest(String packageName) {
        if (restrictWifiScanPkgSet.contains(packageName)) {
            return true;
        }
        return false;
    }

    private boolean isInSetWifiModeWhiteList(String packageName, int mode) {
        String[] strArr = WIFI_MODE_WHITE_PACKAGE_LIST;
        int length = strArr.length;
        int i = 0;
        while (i < length) {
            if (!strArr[i].equals(packageName)) {
                i++;
            } else if (mode == 0) {
                return true;
            } else {
                switch (mode) {
                    case 1002:
                    case 1003:
                        if (!"com.huawei.associateassistant".equals(packageName)) {
                            return false;
                        }
                        enforceCallerPermission(PERMISSION_SET_WIFI_MODE_ASSISTANT);
                        return true;
                    case 1004:
                        if (!"com.huawei.pcassistant".equals(packageName)) {
                            return false;
                        }
                        enforceCallerPermission(PERMISSION_SET_WIFI_MODE_ASSISTANT);
                        return true;
                    case 1005:
                    case 1006:
                        if (!"com.huawei.android.instantshare".equals(packageName)) {
                            return false;
                        }
                        enforceCallerPermission(PERMISSION_SET_WIFI_MODE_HWSHARE);
                        return true;
                    default:
                        HwHiSLog.i(TAG, false, "%{public}d has no permission", new Object[]{Integer.valueOf(mode)});
                        return false;
                }
            }
        }
        HwHiSLog.i(TAG, false, "%{public}s not in white list to set wifi mode", new Object[]{packageName});
        return false;
    }

    private boolean dealWifiMode(String packageName, int mode) {
        if (!(this.mClientModeImpl instanceof HwWifiStateMachine)) {
            HwHiSLog.i(TAG, false, "mClientModeImpl is not a instanceof HwWifiStateMachine", new Object[0]);
            return false;
        }
        HwWifiStateMachine hwWifiStateMachine = this.mClientModeImpl;
        if (mode == 0) {
            return hwWifiStateMachine.setWifiMode(packageName, 0);
        }
        switch (mode) {
            case 1002:
                if (!hwWifiStateMachine.setWifiMode(packageName, 3)) {
                    return false;
                }
                hwWifiStateMachine.sendAssistantTimeoutMessage();
                return true;
            case 1003:
            case 1006:
                hwWifiStateMachine.removeAssistantTimeoutMessage();
                return true;
            case 1004:
                hwWifiStateMachine.setWifiP2pListenMode();
                return true;
            case 1005:
                if (!hwWifiStateMachine.setWifiMode(packageName, 7)) {
                    return false;
                }
                hwWifiStateMachine.sendAssistantTimeoutMessage();
                return true;
            default:
                return false;
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        PPPOEConfig _arg0;
        WifiDetectConfInfo _arg02;
        boolean z = false;
        boolean enableMp = false;
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        boolean z2 = false;
        boolean enablen = false;
        int result = 0;
        int i7 = 0;
        if (code == CODE_GET_WIFI_REPEATER_MODE) {
            int wifiRepeaterMode = -1;
            if (!checkSignMatchOrIsSystemApp()) {
                HwHiSLog.e(TAG, false, "CODE_GET_WIFI_REPEATER_MODE SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                reply.writeNoException();
                reply.writeInt(-1);
                return false;
            }
            data.enforceInterface(DESCRIPTOR);
            enforceAccessPermission();
            if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                wifiRepeaterMode = this.mClientModeImpl.getWifiRepeaterMode();
            }
            reply.writeNoException();
            reply.writeInt(wifiRepeaterMode);
            return true;
        } else if (code == CODE_CONFIRM_WIFI_REPEATER) {
            HwHiSLog.d(TAG, false, "CODE_CONFIRM_WIFI_REPEATER enter", new Object[0]);
            if (!checkSignMatchOrIsSystemApp()) {
                HwHiSLog.e(TAG, false, "CODE_CONFIRM_WIFI_REPEATER SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                reply.writeNoException();
                return false;
            }
            data.enforceInterface(DESCRIPTOR);
            enforceAccessPermission();
            int repeaterMode = data.readInt();
            IWifiRepeaterConfirmListener listener = IWifiRepeaterConfirmListener.Stub.asInterface(data.readStrongBinder());
            if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                this.mClientModeImpl.confirmWifiRepeater(repeaterMode, listener);
            }
            reply.writeNoException();
            return true;
        } else if (code != CODE_ENABLE_HILINK_HANDSHAKE) {
            if (code != CODE_GET_CONNECTION_RAW_PSK) {
                if (code != CODE_RESTRICT_WIFI_SCAN) {
                    if (code == CODE_UPDATE_WM_FREQ_LOC) {
                        boolean result2 = false;
                        if (!checkSignMatchOrIsSystemApp()) {
                            HwHiSLog.e(TAG, false, "WifiService  CODE_UPDATE_WM_FREQ_LOC SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                            reply.writeNoException();
                            reply.writeInt(0);
                            return false;
                        }
                        data.enforceInterface(DESCRIPTOR);
                        enforceAccessPermission();
                        int location = data.readInt();
                        int action = data.readInt();
                        FrequentLocation mFrequentLocation = FrequentLocation.getInstance();
                        if (mFrequentLocation != null) {
                            result2 = mFrequentLocation.updateWaveMapping(location, action);
                        }
                        reply.writeNoException();
                        if (result2) {
                            i7 = 1;
                        }
                        reply.writeInt(i7);
                        return true;
                    } else if (code == CODE_SET_PERFORMANCE_MODE) {
                        if (checkSignMatchOrIsSystemApp()) {
                            HwHiSLog.v(TAG, false, "CODE_SET_PERFORMANCE_MODE SIGNATURE_MATCH", new Object[0]);
                        } else if (isAllowAccessWifiService()) {
                            enforceCallerPermission(ACCESS_WIFI_PERMISSION);
                        } else {
                            HwHiSLog.d(TAG, false, "CODE_SET_PERFORMANCE_MODE SIGNATURE_NO_MATCH", new Object[0]);
                            reply.writeNoException();
                            reply.writeInt(0);
                            return false;
                        }
                        data.enforceInterface(DESCRIPTOR);
                        enforceAccessPermission();
                        int mode = data.readInt();
                        if (mode == 0 || mode == 1) {
                            result = 1;
                        }
                        reply.writeNoException();
                        reply.writeInt(result);
                        return true;
                    } else if (code != CODE_REPORT_SPEED_RESULT) {
                        switch (code) {
                            case CODE_GET_WPA_SUPP_CONFIG /*{ENCODED_INT: 1001}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService  CODE_GET_WPA_SUPP_CONFIG SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeString(null);
                                    return true;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                HwHiSLog.d(TAG, false, "WifiService  getWpaSuppConfig", new Object[0]);
                                String result3 = "";
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    result3 = this.mClientModeImpl.getWpaSuppConfig();
                                }
                                reply.writeNoException();
                                reply.writeString(result3);
                                return true;
                            case 1002:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService  CODE_START_PPPOE_CONFIG SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    return false;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                HwHiSLog.d(TAG, false, "WifiService  startPPPOE", new Object[0]);
                                if (!this.isPPPOE) {
                                    HwHiSLog.w(TAG, false, "the PPPOE function is closed.", new Object[0]);
                                    return false;
                                }
                                if (data.readInt() != 0) {
                                    _arg0 = (PPPOEConfig) PPPOEConfig.CREATOR.createFromParcel(data);
                                } else {
                                    _arg0 = null;
                                }
                                this.mPPPOEStateMachine.sendMessage(589825, _arg0);
                                reply.writeNoException();
                                return true;
                            case 1003:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService  CODE_STOP_PPPOE_CONFIG SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    return false;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                HwHiSLog.d(TAG, false, "WifiService  stopPPPOE", new Object[0]);
                                if (!this.isPPPOE) {
                                    HwHiSLog.w(TAG, false, "the PPPOE function is closed.", new Object[0]);
                                    return false;
                                }
                                this.mPPPOEStateMachine.sendMessage(589826);
                                reply.writeNoException();
                                return true;
                            case 1004:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService  CODE_GET_PPPOE_INFO_CONFIG SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeInt(0);
                                    reply.writeNoException();
                                    return true;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                HwHiSLog.d(TAG, false, "WifiService  get PPPOE info", new Object[0]);
                                if (!this.isPPPOE) {
                                    HwHiSLog.w(TAG, false, "the PPPOE function is closed.", new Object[0]);
                                    return false;
                                }
                                PPPOEInfo _result = this.mPPPOEStateMachine.getPPPOEInfo();
                                reply.writeNoException();
                                if (_result != null) {
                                    reply.writeInt(1);
                                    _result.writeToParcel(reply, 1);
                                } else {
                                    reply.writeInt(0);
                                }
                                reply.writeNoException();
                                return true;
                            case 1005:
                                HwHiSLog.d(TAG, false, "Receive CODE_GET_APLINKED_STA_LIST", new Object[0]);
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService  CODE_GET_APLINKED_STA_LIST SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeStringList(null);
                                    return true;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                List<String> result4 = null;
                                result4 = null;
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    HwWifiStateMachine hwWifiStateMachine = this.mClientModeImpl;
                                    if (isSoftApEnabled()) {
                                        result4 = hwWifiStateMachine.getApLinkedStaList();
                                    } else if (!SHOULD_NETWORK_SHARING_INTEGRATION || !hwWifiStateMachine.isWifiRepeaterStarted()) {
                                        HwHiSLog.w(TAG, false, "Receive CODE_GET_APLINKED_STA_LIST when softap state is not enabled", new Object[0]);
                                    } else {
                                        result4 = hwWifiStateMachine.getRepeaterLinkedClientList();
                                    }
                                }
                                if (result4 == null) {
                                    HwHiSLog.d(TAG, false, "getApLinkedStaList result = null", new Object[0]);
                                } else {
                                    HwHiSLog.d(TAG, false, "getApLinkedStaList result = %{private}s", new Object[]{result4.toString().replaceAll("\\.[\\d]{1,3}\\.[\\d]{1,3}\\.", ".*.*.").replaceAll(":[\\w]{1,}:[\\w]{1,}:", ":**:**:")});
                                }
                                reply.writeNoException();
                                reply.writeStringList(result4);
                                return true;
                            case 1006:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService  CODE_SET_SOFTAP_MACFILTER SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    return false;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                String macFilter = data.readString();
                                HwHiSLog.d(TAG, false, "Receive CODE_SET_SOFTAP_MACFILTER, macFilter:%{private}s", new Object[]{macFilter});
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    HwWifiStateMachine hwWifiStateMachine2 = this.mClientModeImpl;
                                    if (isSoftApEnabled()) {
                                        hwWifiStateMachine2.setSoftapMacFilter(macFilter);
                                        notifyBlacklistEvent(macFilter);
                                    } else if (!SHOULD_NETWORK_SHARING_INTEGRATION || !hwWifiStateMachine2.isWifiRepeaterStarted()) {
                                        HwHiSLog.w(TAG, false, "Receive CODE_SET_SOFTAP_MACFILTER when softap state is not enabled", new Object[0]);
                                    } else {
                                        hwWifiStateMachine2.setWifiRepeaterMacFilter(macFilter);
                                    }
                                }
                                reply.writeNoException();
                                return true;
                            case CODE_SET_SOFTAP_DISASSOCIATESTA /*{ENCODED_INT: 1007}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_SET_SOFTAP_DISASSOCIATESTA SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    return false;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                String mac = data.readString();
                                HwHiSLog.d(TAG, false, "Receive CODE_SET_SOFTAP_DISASSOCIATESTA, mac = %{private}s", new Object[]{mac});
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    HwWifiStateMachine hwWifiStateMachine3 = this.mClientModeImpl;
                                    if (isSoftApEnabled()) {
                                        hwWifiStateMachine3.setSoftapDisassociateSta(mac);
                                    } else if (!SHOULD_NETWORK_SHARING_INTEGRATION || !hwWifiStateMachine3.isWifiRepeaterStarted()) {
                                        HwHiSLog.w(TAG, false, "Receive CODE_SET_SOFTAP_DISASSOCIATESTA when softap state is not enabled", new Object[0]);
                                    } else {
                                        hwWifiStateMachine3.setWifiRepeaterDisassociateSta(mac);
                                    }
                                }
                                reply.writeNoException();
                                return true;
                            case CODE_USER_HANDOVER_WIFI /*{ENCODED_INT: 1008}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_USER_HANDOVER_WIFI SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    return false;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                HwHiSLog.d(TAG, false, "HwWifiService  userHandoverWiFi ", new Object[0]);
                                if (this.mHwWifiProServiceManager.isWifiProStateMachineStarted()) {
                                    this.mHwWifiProServiceManager.userHandoverWifi();
                                }
                                reply.writeNoException();
                                return true;
                            case CODE_GET_SOFTAP_CHANNEL_LIST /*{ENCODED_INT: 1009}*/:
                                HwHiSLog.d(TAG, false, "Receive CODE_GET_SOFTAP_CHANNEL_LIST", new Object[0]);
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_GET_SOFTAP_CHANNEL_LIST SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeIntArray(null);
                                    return true;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                int type = data.readInt();
                                int[] result5 = null;
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    HwWifiStateMachine mHwWifiStateMachine = this.mClientModeImpl;
                                    if (type == 1) {
                                        result5 = mHwWifiStateMachine.getSoftApWideBandWidthChannels();
                                    } else {
                                        result5 = mHwWifiStateMachine.getSoftApChannelListFor5G();
                                    }
                                }
                                reply.writeNoException();
                                reply.writeIntArray(result5);
                                return true;
                            case CODE_SET_WIFI_AP_EVALUATE_ENABLED /*{ENCODED_INT: 1010}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_SET_WIFI_AP_EVALUATE_ENABLED SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    return false;
                                }
                                HwHiSLog.d(TAG, false, "HwWifiService  SET_WIFI_AP_EVALUATE_ENABLED ", new Object[0]);
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                if (data.readInt() == 1) {
                                    enablen = true;
                                }
                                if (this.mHwWifiProServiceManager.isWifiProStateMachineStarted()) {
                                    this.mHwWifiProServiceManager.setWifiApEvaluateEnabled(enablen);
                                }
                                reply.writeNoException();
                                return true;
                            case CODE_GET_SINGNAL_INFO /*{ENCODED_INT: 1011}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_GET_SINGNAL_INFO SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeByteArray(null);
                                    return false;
                                }
                                HwHiSLog.d(TAG, false, "HwWifiService  FETCH_WIFI_SIGNAL_INFO_FOR_VOWIFI ", new Object[0]);
                                data.enforceInterface(DESCRIPTOR);
                                if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                                    HwHiSLog.d(TAG, false, "fetchWifiSignalInfoForVoWiFi(): permissin deny", new Object[0]);
                                    return false;
                                }
                                byte[] result6 = null;
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    result6 = this.mClientModeImpl.fetchWifiSignalInfoForVoWiFi();
                                }
                                reply.writeNoException();
                                reply.writeByteArray(result6);
                                return true;
                            case CODE_SET_VOWIFI_DETECT_MODE /*{ENCODED_INT: 1012}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_SET_VOWIFI_DETECT_MODE SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    return false;
                                }
                                HwHiSLog.d(TAG, false, "HwWifiService  SET_VOWIFI_DETECT_MODE ", new Object[0]);
                                data.enforceInterface(DESCRIPTOR);
                                if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                                    HwHiSLog.d(TAG, false, "setVoWifiDetectMode(): permissin deny", new Object[0]);
                                    return false;
                                }
                                if (data.readInt() != 0) {
                                    _arg02 = (WifiDetectConfInfo) WifiDetectConfInfo.CREATOR.createFromParcel(data);
                                } else {
                                    _arg02 = null;
                                }
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    this.mClientModeImpl.setVoWifiDetectMode(_arg02);
                                }
                                reply.writeNoException();
                                return true;
                            case CODE_GET_VOWIFI_DETECT_MODE /*{ENCODED_INT: 1013}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_GET_VOWIFI_DETECT_MODE SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeInt(0);
                                    return false;
                                }
                                HwHiSLog.d(TAG, false, "HwWifiService  GET_VOWIFI_DETECT_MODE ", new Object[0]);
                                data.enforceInterface(DESCRIPTOR);
                                WifiDetectConfInfo result7 = null;
                                if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                                    HwHiSLog.d(TAG, false, "getVoWifiDetectMode(): permissin deny", new Object[0]);
                                    return false;
                                }
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    result7 = this.mClientModeImpl.getVoWifiDetectMode();
                                }
                                reply.writeNoException();
                                if (result7 != null) {
                                    reply.writeInt(1);
                                    result7.writeToParcel(reply, 1);
                                } else {
                                    reply.writeInt(0);
                                }
                                return true;
                            case CODE_SET_VOWIFI_DETECT_PERIOD /*{ENCODED_INT: 1014}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_SET_VOWIFI_DETECT_PERIOD SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    return false;
                                }
                                HwHiSLog.d(TAG, false, "HwWifiService  SET_VOWIFI_DETECT_PERIOD ", new Object[0]);
                                data.enforceInterface(DESCRIPTOR);
                                if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                                    HwHiSLog.d(TAG, false, "setVoWifiDetectPeriod(): permissin deny", new Object[0]);
                                    return false;
                                }
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    this.mClientModeImpl.setVoWifiDetectPeriod(data.readInt());
                                }
                                reply.writeNoException();
                                return true;
                            case CODE_GET_VOWIFI_DETECT_PERIOD /*{ENCODED_INT: 1015}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_GET_VOWIFI_DETECT_PERIOD SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeInt(-1);
                                    return false;
                                }
                                HwHiSLog.d(TAG, false, "HwWifiService  GET_VOWIFI_DETECT_PERIOD ", new Object[0]);
                                data.enforceInterface(DESCRIPTOR);
                                if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                                    HwHiSLog.d(TAG, false, "getVoWifiDetectPeriod(): permissin deny", new Object[0]);
                                    return false;
                                }
                                int result8 = -1;
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    result8 = this.mClientModeImpl.getVoWifiDetectPeriod();
                                }
                                reply.writeNoException();
                                reply.writeInt(result8);
                                return true;
                            case CODE_IS_SUPPORT_VOWIFI_DETECT /*{ENCODED_INT: 1016}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService CODE_IS_SUPPORT_VOWIFI_DETECT SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    return false;
                                }
                                HwHiSLog.d(TAG, false, "HwWifiService  IS_SUPPORT_VOWIFI_DETECT ", new Object[0]);
                                data.enforceInterface(DESCRIPTOR);
                                if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                                    HwHiSLog.d(TAG, false, "isSupportVoWifiDetect(): permissin deny", new Object[0]);
                                    return false;
                                }
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    HwWifiStateMachine mHwWifiStateMachine2 = this.mClientModeImpl;
                                    if (wifiServiceUtils.getWifiStateMachineChannel(this) != null) {
                                        mHwWifiStateMachine2.syncGetSupportedVoWifiDetect(wifiServiceUtils.getWifiStateMachineChannel(this));
                                    } else {
                                        HwHiSLog.e(TAG, false, "Exception mWifiStateMachineChannel is not initialized", new Object[0]);
                                    }
                                }
                                reply.writeNoException();
                                reply.writeBooleanArray(new boolean[]{true});
                                return true;
                            case CODE_CTRL_HW_WIFI_NETWORK /*{ENCODED_INT: 1017}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    Slog.e(TAG, "WifiService CODE_CTRL_HW_WIFI_NETWORK SIGNATURE_NO_MATCH or not systemApp");
                                    reply.writeNoException();
                                    return false;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                String pkgName = data.readString();
                                int interfaceId = data.readInt();
                                Bundle bundle = data.readBundle();
                                Slog.d(TAG, "HwWifiService  CODE_CTRL_HW_WIFI_NETWORK");
                                this.mHwWifiProServiceProxy = HwWifiProServiceProxy.getHwWifiProServiceProxy(this.mContext);
                                Bundle result9 = new Bundle();
                                HwWifiProServiceProxy hwWifiProServiceProxy = this.mHwWifiProServiceProxy;
                                if (hwWifiProServiceProxy != null) {
                                    result9 = hwWifiProServiceProxy.ctrlHwWifiNetwork(pkgName, interfaceId, bundle);
                                }
                                reply.writeNoException();
                                reply.writeBundle(result9);
                                return true;
                            case SERVER_CODE_CONTROL_HIDATA_OPTIMIZE /*{ENCODED_INT: 3031}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiLog.e(TAG, false, "hidata control SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeBoolean(false);
                                    return true;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                String pkgName2 = data.readString();
                                int controlAction = data.readInt();
                                boolean isEnable = data.readBoolean();
                                boolean controlResult = false;
                                controlResult = false;
                                if (controlAction != 6060) {
                                    HwHidataManager hidataManager = HwHidataManager.getInstance();
                                    if (hidataManager != null) {
                                        controlResult = hidataManager.controlHidataOptimize(pkgName2, controlAction, isEnable);
                                    }
                                } else if (!DCUtils.isDcSupported()) {
                                    HwHiSLog.e(TAG, false, "Do not support DC, return", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeBoolean(false);
                                    return true;
                                } else {
                                    DCController dcController = DCController.getInstance();
                                    if (dcController != null) {
                                        controlResult = dcController.isDcDisconnectSuccess(pkgName2);
                                    }
                                }
                                reply.writeNoException();
                                reply.writeBoolean(controlResult);
                                return true;
                            case CODE_ENABLE_WIFICHIP_CHECK /*{ENCODED_INT: 4021}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiSLog.e(TAG, false, "WifiService  CODE_ENABLE_WIFICHIP_CHECKER SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    return false;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                if (data.readInt() != 0) {
                                    z2 = true;
                                }
                                Boolean isRestrict = Boolean.valueOf(z2);
                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                    this.mClientModeImpl.enbaleWifichipCheck(isRestrict.booleanValue());
                                }
                                return true;
                            case CODE_SET_WIFI_MODE /*{ENCODED_INT: 4111}*/:
                                if (!checkSignMatchOrIsSystemApp()) {
                                    HwHiLog.e(TAG, false, "CODE_SET_WIFI_MODE: SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeInt(0);
                                    return true;
                                }
                                data.enforceInterface(DESCRIPTOR);
                                enforceAccessPermission();
                                String packageName = data.readString();
                                int mode2 = data.readInt();
                                HwHiLog.i(TAG, false, "SET_WIFI_MODE is called packageName=%{public}s mode=%{public}d", new Object[]{packageName, Integer.valueOf(mode2)});
                                if (!isInSetWifiModeWhiteList(packageName, mode2)) {
                                    HwHiLog.e(TAG, false, "CODE_SET_WIFI_MODE: is not in white list", new Object[0]);
                                    reply.writeNoException();
                                    reply.writeInt(0);
                                    return true;
                                }
                                boolean dealWifiMode = dealWifiMode(packageName, mode2);
                                reply.writeNoException();
                                reply.writeInt(dealWifiMode ? 1 : 0);
                                return true;
                            default:
                                switch (code) {
                                    case CODE_REQUEST_WIFI_ENABLE /*{ENCODED_INT: 2004}*/:
                                        if (!checkSignMatchOrIsSystemApp()) {
                                            HwHiSLog.e(TAG, false, "WifiService REQUEST_WIFI_ENABLE SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                            return false;
                                        }
                                        data.enforceInterface(DESCRIPTOR);
                                        enforceAccessPermission();
                                        HwHiSLog.d(TAG, false, "HwWifiService REQUEST_WIFI_ENABLE", new Object[0]);
                                        return requestWifiEnable(data);
                                    case CODE_SET_WIFI_TXPOWER /*{ENCODED_INT: 2005}*/:
                                        if (!checkSignMatchOrIsSystemApp()) {
                                            HwHiSLog.e(TAG, false, "WifiService CODE_SET_WIFI_TXPOWER SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                            reply.writeNoException();
                                            reply.writeInt(-1);
                                            return true;
                                        }
                                        data.enforceInterface(DESCRIPTOR);
                                        enforceAccessPermission();
                                        int result10 = WifiInjector.getInstance().getWifiNative().mHwWifiNativeEx.setWifiTxPowerHw(data.readInt());
                                        reply.writeNoException();
                                        reply.writeInt(result10);
                                        return true;
                                    case CODE_EXTEND_WIFI_SCAN_PERIOD_FOR_P2P /*{ENCODED_INT: 2006}*/:
                                        if (!checkSignMatchOrIsSystemApp()) {
                                            HwHiSLog.e(TAG, false, "WifiService EXTEND_WIFI_SCAN_PERIOD_FOR_P2P SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                            return false;
                                        }
                                        data.enforceInterface(DESCRIPTOR);
                                        enforceAccessPermission();
                                        HwHiSLog.d(TAG, false, "HwWifiService  EXTEND_WIFI_SCAN_PERIOD_FOR_P2P", new Object[0]);
                                        return externWifiScanPeriodForP2p(data);
                                    case CODE_REQUEST_FRESH_WHITE_LIST /*{ENCODED_INT: 2007}*/:
                                        if (!checkSignMatchOrIsSystemApp()) {
                                            HwHiSLog.e(TAG, false, "WifiService CODE_REQUEST_FRESH_WHITE_LIST SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                            reply.writeNoException();
                                            return false;
                                        }
                                        data.enforceInterface(DESCRIPTOR);
                                        enforceAccessPermission();
                                        int type2 = data.readInt();
                                        List<String> packageWhiteList = new ArrayList<>();
                                        data.readStringList(packageWhiteList);
                                        if (type2 == 7) {
                                            HwQoEService qoeService_wifiSleep = HwQoEService.getInstance();
                                            if (qoeService_wifiSleep != null) {
                                                qoeService_wifiSleep.updateWifiSleepWhiteList(type2, packageWhiteList);
                                            }
                                        } else {
                                            BackgroundAppScanManager.getInstance().refreshPackageWhitelist(type2, packageWhiteList);
                                        }
                                        reply.writeNoException();
                                        return true;
                                    case CODE_GET_RSDB_SUPPORTED_MODE /*{ENCODED_INT: 2008}*/:
                                        if (!checkSignMatchOrIsSystemApp()) {
                                            HwHiSLog.e(TAG, false, "WifiService CODE_GET_RSDB_SUPPORTED_MODE SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                            reply.writeNoException();
                                            reply.writeBooleanArray(new boolean[]{false});
                                            return true;
                                        }
                                        data.enforceInterface(DESCRIPTOR);
                                        enforceAccessPermission();
                                        boolean result11 = false;
                                        result11 = false;
                                        if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                            HwWifiStateMachine mHwWifiStateMachine3 = this.mClientModeImpl;
                                            if (wifiServiceUtils.getWifiStateMachineChannel(this) != null) {
                                                result11 = mHwWifiStateMachine3.isRSDBSupported();
                                            } else {
                                                HwHiSLog.e(TAG, false, "Exception mWifiStateMachineChannel is not initialized", new Object[0]);
                                            }
                                        }
                                        reply.writeNoException();
                                        reply.writeBooleanArray(new boolean[]{result11});
                                        return true;
                                    default:
                                        switch (code) {
                                            case 3001:
                                                if (!checkSignMatchOrIsSystemApp()) {
                                                    HwHiSLog.e(TAG, false, "WifiService CODE_WIFI_QOE_START_MONITOR SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                    reply.writeNoException();
                                                    reply.writeInt(0);
                                                    return true;
                                                }
                                                data.enforceInterface(DESCRIPTOR);
                                                enforceAccessPermission();
                                                boolean result12 = false;
                                                int monitorType = data.readInt();
                                                int period = data.readInt();
                                                IHwQoECallback callback = IHwQoECallback.Stub.asInterface(data.readStrongBinder());
                                                HwQoEService mHwQoEService = HwQoEService.getInstance();
                                                if (mHwQoEService != null) {
                                                    result12 = mHwQoEService.registerHwQoEMonitor(monitorType, period, callback);
                                                }
                                                reply.writeNoException();
                                                if (result12) {
                                                    i6 = 1;
                                                }
                                                reply.writeInt(i6);
                                                return true;
                                            case CODE_WIFI_QOE_STOP_MONITOR /*{ENCODED_INT: 3002}*/:
                                                if (!checkSignMatchOrIsSystemApp()) {
                                                    HwHiSLog.e(TAG, false, "WifiService CODE_WIFI_QOE_START_MONITOR SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                    reply.writeNoException();
                                                    reply.writeInt(0);
                                                    return true;
                                                }
                                                data.enforceInterface(DESCRIPTOR);
                                                enforceAccessPermission();
                                                boolean result13 = false;
                                                int monitorType2 = data.readInt();
                                                HwQoEService mHwQoEService2 = HwQoEService.getInstance();
                                                if (mHwQoEService2 != null) {
                                                    result13 = mHwQoEService2.unRegisterHwQoEMonitor(monitorType2);
                                                }
                                                reply.writeNoException();
                                                if (result13) {
                                                    i5 = 1;
                                                }
                                                reply.writeInt(i5);
                                                return true;
                                            case CODE_WIFI_QOE_EVALUATE /*{ENCODED_INT: 3003}*/:
                                                if (!checkSignMatchOrIsSystemApp()) {
                                                    HwHiSLog.e(TAG, false, "WifiService CODE_WIFI_QOE_EVALUATE SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                    reply.writeNoException();
                                                    reply.writeInt(0);
                                                    return true;
                                                }
                                                data.enforceInterface(DESCRIPTOR);
                                                enforceAccessPermission();
                                                IHwQoECallback callback2 = IHwQoECallback.Stub.asInterface(data.readStrongBinder());
                                                boolean result14 = false;
                                                HwQoEService mHwQoEService3 = HwQoEService.getInstance();
                                                if (mHwQoEService3 != null) {
                                                    result14 = mHwQoEService3.evaluateNetworkQuality(callback2);
                                                }
                                                reply.writeNoException();
                                                if (result14) {
                                                    i4 = 1;
                                                }
                                                reply.writeInt(i4);
                                                return true;
                                            case CODE_WIFI_QOE_UPDATE_STATUS /*{ENCODED_INT: 3004}*/:
                                                if (!checkSignMatchOrIsSystemApp()) {
                                                    HwHiSLog.e(TAG, false, "WifiService CODE_WIFI_QOE_UPDATE_STATUS SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                    reply.writeNoException();
                                                    reply.writeInt(0);
                                                    return true;
                                                }
                                                data.enforceInterface(DESCRIPTOR);
                                                enforceAccessPermission();
                                                boolean result15 = false;
                                                int state = data.readInt();
                                                HwQoEService mHwQoEService4 = HwQoEService.getInstance();
                                                if (mHwQoEService4 != null) {
                                                    result15 = mHwQoEService4.updateVoWiFiState(state);
                                                }
                                                reply.writeNoException();
                                                if (result15) {
                                                    i3 = 1;
                                                }
                                                reply.writeInt(i3);
                                                return true;
                                            case CODE_UPDATE_APP_RUNNING_STATUS /*{ENCODED_INT: 3005}*/:
                                                if (!checkSignMatchOrIsSystemApp()) {
                                                    HwHiSLog.e(TAG, false, "WifiService CODE_UPDATE_APP_RUNNING_STATUS SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                    reply.writeNoException();
                                                    reply.writeInt(0);
                                                    return true;
                                                }
                                                data.enforceInterface(DESCRIPTOR);
                                                int uid_running = data.readInt();
                                                int type_running = data.readInt();
                                                int status_running = data.readInt();
                                                int scene_running = data.readInt();
                                                data.readInt();
                                                HwHiSLog.d(TAG, false, " updateAppRunningStatus  uid:%{public}d, type:%{public}d, status:%{public}d scene: %{public}d", new Object[]{Integer.valueOf(uid_running), Integer.valueOf(type_running), Integer.valueOf(status_running), Integer.valueOf(scene_running)});
                                                reply.writeNoException();
                                                reply.writeInt(1);
                                                return true;
                                            case CODE_UPDATE_APP_EXPERIENCE_STATUS /*{ENCODED_INT: 3006}*/:
                                                if (!checkSignMatchOrIsSystemApp()) {
                                                    HwHiSLog.e(TAG, false, "WifiService CODE_UPDATE_APP_EXPERIENCE_STATUS SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                    reply.writeNoException();
                                                    reply.writeInt(0);
                                                    return true;
                                                }
                                                data.enforceInterface(DESCRIPTOR);
                                                int uid_experience = data.readInt();
                                                int experience = data.readInt();
                                                long rtt_experience = data.readLong();
                                                data.readInt();
                                                HwHiSLog.d(TAG, false, "updateAppExperienceStatus  uid:%{public}d, experience:%{public}d, rtt:%{public}s", new Object[]{Integer.valueOf(uid_experience), Integer.valueOf(experience), String.valueOf(rtt_experience)});
                                                reply.writeNoException();
                                                reply.writeInt(1);
                                                return true;
                                            case CODE_SET_WIFI_ANTSET /*{ENCODED_INT: 3007}*/:
                                                if (!checkSignMatchOrIsSystemApp()) {
                                                    HwHiSLog.e(TAG, false, "WifiService CODE_SET_WIFI_ANTSET SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                    reply.writeNoException();
                                                    return false;
                                                }
                                                data.enforceInterface(DESCRIPTOR);
                                                enforceAccessPermission();
                                                String iface = data.readString();
                                                int mode3 = data.readInt();
                                                int op = data.readInt();
                                                HwMSSHandler mssHandler = HwMSSHandler.getInstance();
                                                if (mssHandler != null) {
                                                    mssHandler.setWifiAnt(iface, mode3, op);
                                                    HwHiSLog.d(TAG, false, "mssHandler hwSetWifiAnt", new Object[0]);
                                                }
                                                reply.writeNoException();
                                                return true;
                                            case CODE_IS_BG_LIMIT_ALLOWED /*{ENCODED_INT: 3008}*/:
                                                if (!checkSignMatchOrIsSystemApp()) {
                                                    HwHiSLog.e(TAG, false, "WifiService CODE_IS_BG_LIMIT_ALLOWED SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                    reply.writeNoException();
                                                    reply.writeBooleanArray(new boolean[]{false});
                                                    return true;
                                                }
                                                data.enforceInterface(DESCRIPTOR);
                                                int uid = data.readInt();
                                                boolean result16 = false;
                                                HwQoEService qoeService_check_bg_limit = HwQoEService.getInstance();
                                                if (qoeService_check_bg_limit != null) {
                                                    result16 = qoeService_check_bg_limit.isBgLimitAllowed(uid);
                                                }
                                                reply.writeNoException();
                                                reply.writeBooleanArray(new boolean[]{result16});
                                                return true;
                                            default:
                                                switch (code) {
                                                    case CODE_DISABLE_RX_FILTER /*{ENCODED_INT: 3021}*/:
                                                        data.enforceInterface(DESCRIPTOR);
                                                        if (this.mContext.checkCallingPermission(ACCESS_WIFI_FILTER_PERMISSION) != 0) {
                                                            HwHiSLog.d(TAG, false, "disableWifiFilter: No ACCESS_FILTER permission", new Object[0]);
                                                            return false;
                                                        } else if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
                                                            HwHiSLog.d(TAG, false, "you have no permission to call disableWifiFilter from uid:%{public}d", new Object[]{Integer.valueOf(Binder.getCallingUid())});
                                                            return false;
                                                        } else {
                                                            HwHiSLog.i(TAG, false, "call binder disableWifiFilter %{public}s", new Object[]{getAppName(Binder.getCallingPid())});
                                                            boolean result17 = disableWifiFilter(data.readStrongBinder());
                                                            reply.writeNoException();
                                                            reply.writeBooleanArray(new boolean[]{result17});
                                                            return true;
                                                        }
                                                    case CODE_ENABLE_RX_FILTER /*{ENCODED_INT: 3022}*/:
                                                        data.enforceInterface(DESCRIPTOR);
                                                        if (this.mContext.checkCallingPermission(ACCESS_WIFI_FILTER_PERMISSION) != 0) {
                                                            HwHiSLog.d(TAG, false, "enableWifiFilter: No ACCESS_FILTER permission", new Object[0]);
                                                            return false;
                                                        } else if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
                                                            HwHiSLog.d(TAG, false, "you have no permission to call enableWifiFilter from uid:%{public}d", new Object[]{Integer.valueOf(Binder.getCallingUid())});
                                                            return false;
                                                        } else {
                                                            HwHiSLog.i(TAG, false, "call binder enableWifiFilter %{public}s", new Object[]{getAppName(Binder.getCallingPid())});
                                                            boolean result18 = enableWifiFilter(data.readStrongBinder());
                                                            reply.writeNoException();
                                                            reply.writeBooleanArray(new boolean[]{result18});
                                                            return true;
                                                        }
                                                    case CODE_START_WIFI_KEEP_ALIVE /*{ENCODED_INT: 3023}*/:
                                                        if (!checkSignMatchOrIsSystemApp()) {
                                                            HwHiSLog.e(TAG, false, "WifiService CODE_START_WIFI_KEEP_ALIVE SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                            reply.writeNoException();
                                                            reply.writeInt(0);
                                                            return true;
                                                        }
                                                        data.enforceInterface(DESCRIPTOR);
                                                        enforceAccessPermission();
                                                        Message msg = (Message) Message.CREATOR.createFromParcel(data);
                                                        if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                                            this.mClientModeImpl.startPacketKeepalive(msg);
                                                        }
                                                        reply.writeNoException();
                                                        reply.writeInt(1);
                                                        return true;
                                                    case CODE_STOP_WIFI_KEEP_ALIVE /*{ENCODED_INT: 3024}*/:
                                                        if (!checkSignMatchOrIsSystemApp()) {
                                                            HwHiSLog.e(TAG, false, "WifiService CODE_START_WIFI_KEEP_ALIVE SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                            reply.writeNoException();
                                                            reply.writeInt(0);
                                                            return true;
                                                        }
                                                        data.enforceInterface(DESCRIPTOR);
                                                        enforceAccessPermission();
                                                        Message msg2 = (Message) Message.CREATOR.createFromParcel(data);
                                                        if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                                            this.mClientModeImpl.stopPacketKeepalive(msg2);
                                                        }
                                                        reply.writeNoException();
                                                        reply.writeInt(1);
                                                        return true;
                                                    case CODE_UPDATE_LIMIT_SPEED_STATUS /*{ENCODED_INT: 3025}*/:
                                                        data.enforceInterface(DESCRIPTOR);
                                                        int mode_running = data.readInt();
                                                        int reserve1_running = data.readInt();
                                                        int reserve2_running = data.readInt();
                                                        HwHiSLog.d(TAG, false, " updateLimitSpeedStatus mode: %{public}d, reserve1: %{public}d reserve2: %{public}d", new Object[]{Integer.valueOf(mode_running), Integer.valueOf(reserve1_running), Integer.valueOf(reserve2_running)});
                                                        boolean result19 = false;
                                                        HwQoEService qoeService_app_running = HwQoEService.getInstance();
                                                        if (qoeService_app_running != null) {
                                                            result19 = qoeService_app_running.updateLimitSpeedStatus(mode_running, reserve1_running, reserve2_running);
                                                        }
                                                        reply.writeNoException();
                                                        reply.writeBooleanArray(new boolean[]{result19});
                                                        return true;
                                                    default:
                                                        switch (code) {
                                                            case CODE_IS_FEATURE_SUPPORTED /*{ENCODED_INT: 4011}*/:
                                                                if (!checkSignMatchOrIsSystemApp()) {
                                                                    HwHiSLog.d(TAG, false, "CODE_IS_FEATURE_SUPPORTED SIGNATURE_NO_MATCH", new Object[0]);
                                                                    reply.writeNoException();
                                                                    reply.writeInt(0);
                                                                    return false;
                                                                }
                                                                data.enforceInterface(DESCRIPTOR);
                                                                enforceAccessPermission();
                                                                boolean isSupported = false;
                                                                int featrue = data.readInt();
                                                                int ifaceType = data.readInt();
                                                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                                                    isSupported = this.mClientModeImpl.isFeatureSupported(featrue, ifaceType);
                                                                }
                                                                reply.writeNoException();
                                                                if (isSupported) {
                                                                    i2 = 1;
                                                                }
                                                                reply.writeInt(i2);
                                                                return true;
                                                            case CODE_GET_SOFTAP_BANDWIDTH /*{ENCODED_INT: 4012}*/:
                                                                if (!checkSignMatchOrIsSystemApp()) {
                                                                    HwHiSLog.d(TAG, false, "CODE_IS_FEATURE_SUPPORTED SIGNATURE_NO_MATCH", new Object[0]);
                                                                    reply.writeNoException();
                                                                    reply.writeInt(0);
                                                                    return false;
                                                                }
                                                                data.enforceInterface(DESCRIPTOR);
                                                                enforceAccessPermission();
                                                                int result20 = 0;
                                                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                                                    result20 = this.mClientModeImpl.getApBandwidth();
                                                                }
                                                                reply.writeNoException();
                                                                reply.writeInt(result20);
                                                                return true;
                                                            case CODE_SET_FEM_TXPOWER /*{ENCODED_INT: 4013}*/:
                                                                if (!checkSignMatchOrIsSystemApp()) {
                                                                    HwHiSLog.d(TAG, false, "CODE_IS_FEATURE_SUPPORTED SIGNATURE_NO_MATCH", new Object[0]);
                                                                    reply.writeNoException();
                                                                    reply.writeInt(0);
                                                                    return false;
                                                                }
                                                                data.enforceInterface(DESCRIPTOR);
                                                                enforceAccessPermission();
                                                                boolean isSuccess = false;
                                                                int action2 = data.readInt();
                                                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                                                    isSuccess = this.mClientModeImpl.reduceTxPower(action2);
                                                                }
                                                                reply.writeNoException();
                                                                if (isSuccess) {
                                                                    i = 1;
                                                                }
                                                                reply.writeInt(i);
                                                                return true;
                                                            case CODE_WIFI_DC_CONNECT /*{ENCODED_INT: 4014}*/:
                                                                if (!checkSignMatchOrIsSystemApp()) {
                                                                    HwHiSLog.e(TAG, false, "WifiService CODE_WIFI_DC_CONNECT SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                                    return false;
                                                                }
                                                                HwHiSLog.i(TAG, false, "HwWifiService CODE_WIFI_DC_CONNECT", new Object[0]);
                                                                data.enforceInterface(DESCRIPTOR);
                                                                WifiConfiguration wifiConfig = null;
                                                                if (data.readInt() == 1) {
                                                                    wifiConfig = (WifiConfiguration) WifiConfiguration.CREATOR.createFromParcel(data);
                                                                }
                                                                IWifiActionListener actionListener = IWifiActionListener.Stub.asInterface(data.readStrongBinder());
                                                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                                                    this.mClientModeImpl.dcConnect(wifiConfig, actionListener);
                                                                }
                                                                reply.writeNoException();
                                                                return true;
                                                            case CODE_WIFI_DC_DISCONNECT /*{ENCODED_INT: 4015}*/:
                                                                if (!checkSignMatchOrIsSystemApp()) {
                                                                    HwHiSLog.e(TAG, false, "WifiService CODE_WIFI_DC_DISCONNECT SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                                    return false;
                                                                }
                                                                HwHiSLog.i(TAG, false, "HwWifiService CODE_WIFI_DC_DISCONNECT", new Object[0]);
                                                                data.enforceInterface(DESCRIPTOR);
                                                                boolean isDcDisconnect = false;
                                                                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                                                    isDcDisconnect = this.mClientModeImpl.dcDisconnect();
                                                                }
                                                                reply.writeNoException();
                                                                reply.writeBooleanArray(new boolean[]{isDcDisconnect});
                                                                return true;
                                                            default:
                                                                switch (code) {
                                                                    case CODE_BIND_MPLINK /*{ENCODED_INT: 5002}*/:
                                                                        if (!checkSignMatchOrIsSystemApp()) {
                                                                            HwHiLog.e(TAG, false, "bind mplink: SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                                            reply.writeNoException();
                                                                            reply.writeBoolean(false);
                                                                            return true;
                                                                        }
                                                                        data.enforceInterface(DESCRIPTOR);
                                                                        enforceAccessPermission();
                                                                        if (data.readInt() == 1) {
                                                                            enableMp = true;
                                                                        }
                                                                        String pkg = this.mContext.getPackageManager().getNameForUid(data.readInt());
                                                                        boolean result21 = false;
                                                                        HwHidataManager hidataManager2 = HwHidataManager.getInstance();
                                                                        if (hidataManager2 != null) {
                                                                            result21 = hidataManager2.controlHidataOptimize(pkg, 0, enableMp);
                                                                        }
                                                                        reply.writeNoException();
                                                                        reply.writeBoolean(result21);
                                                                        return true;
                                                                    case CODE_IS_IN_MPLINK_STATE /*{ENCODED_INT: 5003}*/:
                                                                        if (!checkSignMatchOrIsSystemApp()) {
                                                                            HwHiLog.e(TAG, false, "check mplink state: SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                                            reply.writeNoException();
                                                                            reply.writeBoolean(false);
                                                                            return true;
                                                                        }
                                                                        data.enforceInterface(DESCRIPTOR);
                                                                        enforceAccessPermission();
                                                                        int uid2 = data.readInt();
                                                                        boolean result22 = false;
                                                                        HwArbitrationManager hwArbitrationManager = HwArbitrationManager.getInstance();
                                                                        if (hwArbitrationManager != null) {
                                                                            result22 = hwArbitrationManager.isInMpLink(uid2);
                                                                        }
                                                                        reply.writeNoException();
                                                                        reply.writeBoolean(result22);
                                                                        return true;
                                                                    case CODE_WIFI_DC_CONNECT_Z /*{ENCODED_INT: 5004}*/:
                                                                        if (!checkSignMatchOrIsSystemApp()) {
                                                                            HwHiSLog.e(TAG, false, "connect dc: SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                                            reply.writeNoException();
                                                                            reply.writeBoolean(false);
                                                                            return false;
                                                                        }
                                                                        data.enforceInterface(DESCRIPTOR);
                                                                        enforceAccessPermission();
                                                                        WifiDeviceConfig deviceConfig = null;
                                                                        if (data.readInt() == 1) {
                                                                            deviceConfig = (WifiDeviceConfig) WifiDeviceConfig.CREATOR.createFromParcel(data);
                                                                        }
                                                                        WifiConfiguration wifiConfig2 = null;
                                                                        if (deviceConfig != null) {
                                                                            wifiConfig2 = deviceConfig.toWifiConfig();
                                                                        }
                                                                        if (deviceConfig == null || wifiConfig2 == null) {
                                                                            HwHiSLog.w(TAG, false, "wifi config is invalid", new Object[0]);
                                                                            reply.writeNoException();
                                                                            reply.writeBoolean(false);
                                                                            return false;
                                                                        }
                                                                        if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                                                                            this.mClientModeImpl.dcConnect(wifiConfig2, null);
                                                                        }
                                                                        reply.writeNoException();
                                                                        reply.writeBoolean(true);
                                                                        return true;
                                                                    case CODE_WIFI_IS_DC_ACTIVE /*{ENCODED_INT: 5005}*/:
                                                                        if (!checkSignMatchOrIsSystemApp()) {
                                                                            HwHiLog.e(TAG, false, "check dc state: SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                                                                            reply.writeNoException();
                                                                            reply.writeBoolean(false);
                                                                            return true;
                                                                        }
                                                                        data.enforceInterface(DESCRIPTOR);
                                                                        enforceAccessPermission();
                                                                        DCController dcController2 = DCController.getInstance();
                                                                        boolean result23 = false;
                                                                        if (dcController2 != null) {
                                                                            result23 = dcController2.isWifiDcActive();
                                                                        }
                                                                        reply.writeNoException();
                                                                        reply.writeBoolean(result23);
                                                                        return true;
                                                                    default:
                                                                        return HwWifiService.super.onTransact(code, data, reply, flags);
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                    } else {
                        if (checkSignMatchOrIsSystemApp()) {
                            HwHiSLog.v(TAG, false, "CODE_SET_PERFORMANCE_MODE sign match", new Object[0]);
                        } else if (isAllowAccessWifiService()) {
                            enforceCallerPermission(ACCESS_WIFI_PERMISSION);
                        } else {
                            HwHiSLog.d(TAG, false, "CODE_SET_PERFORMANCE_MODE SIGNATURE_NO_MATCH", new Object[0]);
                            reply.writeNoException();
                            reply.writeInt(0);
                            return false;
                        }
                        data.enforceInterface(DESCRIPTOR);
                        enforceAccessPermission();
                        boolean reportSpeedMeasureResult = HwWifiSpeedMeasure.reportSpeedMeasureResult(this.mHwWifiChrService, data.readString());
                        reply.writeNoException();
                        reply.writeInt(reportSpeedMeasureResult ? 1 : 0);
                        return true;
                    }
                } else if (!checkSignMatchOrIsSystemApp()) {
                    HwHiSLog.e(TAG, false, "WifiService  CODE_RESTRICT_WIFI_SCAN SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                    reply.writeNoException();
                    return false;
                } else {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    List<String> pkgs = new ArrayList<>();
                    data.readStringList(pkgs);
                    if (pkgs.size() == 0) {
                        pkgs = null;
                    }
                    if (data.readInt() != 0) {
                        z = true;
                    }
                    restrictWifiScan(pkgs, Boolean.valueOf(z));
                    return true;
                }
            } else if (!checkSignMatchOrIsSystemApp()) {
                HwHiSLog.e(TAG, false, "WifiService CODE_GET_CONNECTION_RAW_PSK SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
                reply.writeNoException();
                reply.writeString(null);
                return true;
            } else {
                data.enforceInterface(DESCRIPTOR);
                enforceAccessPermission();
                String _result2 = null;
                if (this.mClientModeImpl instanceof HwWifiStateMachine) {
                    _result2 = this.mClientModeImpl.getConnectionRawPsk();
                }
                reply.writeNoException();
                reply.writeString(_result2);
                return true;
            }
        } else if (!checkSignMatchOrIsSystemApp()) {
            HwHiSLog.e(TAG, false, "WifiService CODE_ENABLE_HILINK_HANDSHAKE SIGNATURE_NO_MATCH or not systemApp", new Object[0]);
            return false;
        } else {
            data.enforceInterface(DESCRIPTOR);
            enforceAccessPermission();
            boolean isHiLinkEnable = false;
            if (data.readInt() == 1) {
                isHiLinkEnable = true;
            }
            String bssid = data.readString();
            WifiConfiguration config = null;
            if (data.readInt() == 1) {
                config = (WifiConfiguration) WifiConfiguration.CREATOR.createFromParcel(data);
            }
            this.mClientModeImpl.enableHiLinkHandshake(isHiLinkEnable, bssid, config);
            return true;
        }
    }

    private boolean disableWifiFilter(IBinder token) {
        if (token == null) {
            return false;
        }
        synchronized (this.mFilterSynchronizeLock) {
            if (findFilterIndex(token) >= 0) {
                HwHiSLog.d(TAG, false, "attempted to add filterlock when already holding one", new Object[0]);
                return false;
            }
            HwFilterLock filterLock = new HwFilterLock(token);
            try {
                token.linkToDeath(filterLock, 0);
                this.mFilterLockList.add(filterLock);
                return updateWifiFilterState();
            } catch (RemoteException e) {
                HwHiSLog.d(TAG, false, "Filter lock is already dead.", new Object[0]);
                return false;
            }
        }
    }

    private boolean enableWifiFilter(IBinder token) {
        if (token == null) {
            return false;
        }
        synchronized (this.mFilterSynchronizeLock) {
            int index = findFilterIndex(token);
            if (index < 0) {
                HwHiSLog.d(TAG, false, "cannot find wifi filter", new Object[0]);
                return false;
            }
            HwFilterLock filterLock = this.mFilterLockList.get(index);
            this.mFilterLockList.remove(index);
            filterLock.mToken.unlinkToDeath(filterLock, 0);
            return updateWifiFilterState();
        }
    }

    private boolean updateWifiFilterState() {
        synchronized (this.mFilterSynchronizeLock) {
            if (this.mFilterLockList.size() == 0) {
                if (this.isRxFilterDisabled) {
                    HwHiSLog.d(TAG, false, "enableWifiFilter", new Object[0]);
                    this.isRxFilterDisabled = false;
                    return this.mClientModeImpl.enableWifiFilter();
                }
            } else if (!this.isRxFilterDisabled) {
                HwHiSLog.d(TAG, false, "disableWifiFilter", new Object[0]);
                this.isRxFilterDisabled = true;
                return this.mClientModeImpl.disableWifiFilter();
            }
            return true;
        }
    }

    /* access modifiers changed from: private */
    public void handleFilterLockDeath(HwFilterLock filterLock) {
        HwHiSLog.d(TAG, false, "handleFilterLockDeath: lock=%{public}d", new Object[]{Integer.valueOf(Objects.hashCode(filterLock.mToken))});
        synchronized (this.mFilterSynchronizeLock) {
            int index = findFilterIndex(filterLock.mToken);
            if (index < 0) {
                HwHiSLog.d(TAG, false, "cannot find wifi filter", new Object[0]);
                return;
            }
            this.mFilterLockList.remove(index);
            updateWifiFilterState();
        }
    }

    private int findFilterIndex(IBinder token) {
        synchronized (this.mFilterSynchronizeLock) {
            int count = this.mFilterLockList.size();
            for (int i = 0; i < count; i++) {
                if (this.mFilterLockList.get(i).mToken == token) {
                    return i;
                }
            }
            return -1;
        }
    }

    /* access modifiers changed from: private */
    public final class HwFilterLock implements IBinder.DeathRecipient {
        public final IBinder mToken;

        HwFilterLock(IBinder token) {
            this.mToken = token;
        }

        public void binderDied() {
            HwWifiService.this.handleFilterLockDeath(this);
        }
    }

    private boolean requestWifiEnable(Parcel data) {
        HwHiSLog.e(TAG, false, "HwWifiService REQUEST_WIFI_ENABLE is waived!!!", new Object[0]);
        return false;
    }

    private boolean externWifiScanPeriodForP2p(Parcel data) {
        boolean bExtend = data.readInt() == 1;
        int iTimes = data.readInt();
        WifiConnectivityManager wifiConnectivityManager = wifiStateMachineUtils.getWifiConnectivityManager(this.mClientModeImpl);
        if (wifiConnectivityManager == null || !(wifiConnectivityManager instanceof HwWifiConnectivityManager)) {
            HwHiSLog.d(TAG, false, "EXTEND_WIFI_SCAN_PERIOD_FOR_P2P: Exception wifiConnectivityManager is not initialized", new Object[0]);
            return false;
        }
        HwHiSLog.d(TAG, false, "HwWifiService  EXTEND_WIFI_SCAN_PERIOD_FOR_P2P: %{public}s, Times =%{public}d", new Object[]{String.valueOf(bExtend), Integer.valueOf(iTimes)});
        ((HwWifiConnectivityManager) wifiConnectivityManager).extendWifiScanPeriodForP2p(bExtend, iTimes);
        return true;
    }

    /* access modifiers changed from: private */
    public void onVoWifiCloseForServiceDisconnect() {
        HwHiLog.d(TAG, false, "onVoWifiCloseForServiceDisconnect: send and cancel delayed message", new Object[0]);
        if (this.mMapconHandler.hasMessages(0)) {
            this.mMapconHandler.removeMessages(0);
            this.mClientModeImpl.sendMessage(this.mForgetNetworkMsg);
        }
        WifiController controller = wifiServiceUtils.getWifiController(this);
        if (controller == null) {
            HwHiLog.d(TAG, false, "onVoWifiCloseForServiceDisconnect: get WifiController is NULL", new Object[0]);
            return;
        }
        if (this.mMapconHandler.hasMessages(1)) {
            this.mMapconHandler.removeMessages(1);
            controller.sendMessage(155657);
        }
        if (this.mMapconHandler.hasMessages(2)) {
            this.mMapconHandler.removeMessages(2);
            controller.sendMessage(155656);
        }
    }

    /* access modifiers changed from: private */
    public void onVoWifiCloseDoneForToggled() {
        HwHiLog.d(TAG, false, "onVoWifiCloseDone: cancel delayed message,send CMD_WIFI_TOGGLED", new Object[0]);
        if (this.mMapconHandler.hasMessages(2)) {
            this.mMapconHandler.removeMessages(2);
        }
        WifiController controller = wifiServiceUtils.getWifiController(this);
        if (controller != null) {
            controller.sendMessage(155656);
        }
    }

    /* access modifiers changed from: private */
    public void onVoWifiCloseDoneForAirplaneToggled() {
        HwHiLog.d(TAG, false, "onVoWifiCloseDone: cancel delayed message, send CMD_AIRPLANE_TOGGLED", new Object[0]);
        if (this.mMapconHandler.hasMessages(1)) {
            this.mMapconHandler.removeMessages(1);
        }
        WifiController controller = wifiServiceUtils.getWifiController(this);
        if (controller != null) {
            controller.sendMessage(155657);
        }
    }

    /* access modifiers changed from: private */
    public void onVoWifiCloseDoneForForgetNetwork() {
        HwHiLog.d(TAG, false, "onVoWifiCloseDone: cancel delayed message and send FORGET_NETWORK", new Object[0]);
        if (this.mMapconHandler.hasMessages(0)) {
            this.mMapconHandler.removeMessages(0);
        }
    }

    /* access modifiers changed from: protected */
    public void handleForgetNetwork(final Message msg) {
        WifiConfiguration currentWifiConfiguration = this.mClientModeImpl.getCurrentWifiConfiguration();
        HwHiLog.d(TAG, false, "handleForgetNetwork networkId = %{public}d", new Object[]{Integer.valueOf(msg.arg1)});
        if (!this.mVowifiServiceOn || currentWifiConfiguration == null || msg.arg1 != currentWifiConfiguration.networkId) {
            int currentNetId = -1;
            if (currentWifiConfiguration != null) {
                currentNetId = currentWifiConfiguration.networkId;
            }
            HwHiLog.d(TAG, false, "handleForgetNetwork current networkId = %{public}d", new Object[]{Integer.valueOf(currentNetId)});
            this.mClientModeImpl.sendMessage(Message.obtain(msg));
            return;
        }
        HwHiLog.d(TAG, false, "handleForgetNetwork enter.", new Object[0]);
        Handler handler = this.mMapconHandler;
        handler.sendMessageDelayed(handler.obtainMessage(0, msg), 5000);
        this.mForgetNetworkMsg = Message.obtain(msg);
        try {
            if (this.mMapconService != null) {
                this.mMapconService.notifyWifiOff(new IMapconServiceCallback.Stub() {
                    /* class com.android.server.wifi.HwWifiService.AnonymousClass10 */

                    public void onVoWifiCloseDone() {
                        HwWifiService.this.mClientModeImpl.sendMessage(Message.obtain(msg));
                        HwWifiService.this.onVoWifiCloseDoneForForgetNetwork();
                    }
                });
            } else if (this.mMtkMapconService != null) {
                this.mMtkMapconService.notifyWifiOff(new IMtkMapconServiceCallback.Stub() {
                    /* class com.android.server.wifi.HwWifiService.AnonymousClass11 */

                    public void onVoWifiCloseDone() {
                        HwWifiService.this.mClientModeImpl.sendMessage(Message.obtain(msg));
                        HwWifiService.this.onVoWifiCloseDoneForForgetNetwork();
                    }
                });
            } else {
                HwHiLog.d(TAG, false, "handleForgetNetwork: MapconService is null", new Object[0]);
            }
        } catch (RemoteException e) {
            HwHiLog.e(TAG, false, "handleForgetNetwork Exception", new Object[0]);
            this.mClientModeImpl.sendMessage(Message.obtain(msg));
            onVoWifiCloseDoneForForgetNetwork();
        }
    }

    /* access modifiers changed from: protected */
    public void handleAirplaneModeToggled() {
        WifiController controller = wifiServiceUtils.getWifiController(this);
        if (this.mVowifiServiceOn) {
            if (this.mSettingsStore.isAirplaneModeOn()) {
                HwHiLog.d(TAG, false, "handleAirplaneModeToggled, sendMessageDelayed", new Object[0]);
                Handler handler = this.mMapconHandler;
                handler.sendMessageDelayed(handler.obtainMessage(1), 5000);
                try {
                    HwHiLog.d(TAG, false, "airplane mode enter, notify MapconService to shutdown", new Object[0]);
                    if (this.mMapconService != null) {
                        this.mMapconService.notifyWifiOff(this.mAirPlaneCallback);
                    } else if (this.mMtkMapconService != null) {
                        this.mMtkMapconService.notifyWifiOff(this.mMtkAirPlaneCallback);
                    } else {
                        HwHiLog.d(TAG, false, "handleAirplaneModeToggled: MapconService is null", new Object[0]);
                    }
                } catch (RemoteException e) {
                    HwHiLog.e(TAG, false, "handleAirplaneModeToggled failed", new Object[0]);
                }
            } else if (controller != null) {
                controller.sendMessage(155657);
            }
        } else if (controller != null) {
            controller.sendMessage(155657);
        }
    }

    /* access modifiers changed from: protected */
    public void setWifiEnabledAfterVoWifiOff(boolean enable) {
        HwHiLog.d(TAG, false, "setWifiEnabled %{public}s", new Object[]{String.valueOf(enable)});
        if (WifiProCommonUtils.isWifiSelfCuring() || !this.mVowifiServiceOn || 3 != getWifiEnabledState() || this.mSettingsStore.isWifiToggleEnabled()) {
            WifiController controller = wifiServiceUtils.getWifiController(this);
            if (controller != null) {
                controller.sendMessage(155656);
                return;
            }
            return;
        }
        HwHiLog.d(TAG, false, "setWifiEnabled: sendMessageDelayed", new Object[0]);
        Handler handler = this.mMapconHandler;
        handler.sendMessageDelayed(handler.obtainMessage(2), 5000);
        try {
            HwHiLog.d(TAG, false, "setWifiEnabled enter, notify MapconService to shutdown", new Object[0]);
            if (this.mMapconService != null) {
                this.mMapconService.notifyWifiOff(this.mCallback);
            } else if (this.mMtkMapconService != null) {
                this.mMtkMapconService.notifyWifiOff(this.mMtkCallback);
            } else {
                HwHiLog.d(TAG, false, "notifyWifiOff fail", new Object[0]);
                return;
            }
        } catch (RemoteException e) {
            HwHiLog.d(TAG, false, "notifyWifiOff Exception", new Object[0]);
        }
        while (this.mMapconHandler.hasMessages(2)) {
            try {
                HwHiLog.d(TAG, false, "setWifiEnabled ++++", new Object[0]);
                Thread.sleep(5);
            } catch (InterruptedException e2) {
                HwHiLog.d(TAG, false, "%{public}s", new Object[]{e2.getMessage()});
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onReceiveEx(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            HwHiSLog.d(TAG, false, "onReceive, action:%{public}s", new Object[]{action});
            Boolean isVoWifiOn = Boolean.valueOf(SystemProperties.getBoolean("ro.vendor.config.hw_vowifi", false));
            if (isVoWifiOn.booleanValue() && ACTION_VOWIFI_STARTED.equals(action)) {
                this.mChipPlatform = 1;
                HwHiLog.d(TAG, false, "received broadcast ACTION_VOWIFI_STARTED, try to bind MapconService", new Object[0]);
                this.mContext.bindServiceAsUser(new Intent().setClassName("com.hisi.mapcon", "com.hisi.mapcon.MapconService"), this.conn, 1, UserHandle.OWNER);
            } else if (!isVoWifiOn.booleanValue() || !ACTION_VOWIFI_STARTED_MTK.equals(action)) {
                HwHiLog.d(TAG, false, "received wrong ACTION", new Object[0]);
            } else {
                this.mChipPlatform = 2;
                HwHiLog.d(TAG, false, "received broadcast ACTION_VOWIFI_STARTED_MTK, try to bind MtkMapconService", new Object[0]);
                this.mContext.bindServiceAsUser(new Intent().setClassName("com.mediatek.ims", "com.mediatek.ims.impl.MtkMapconService"), this.conn, 1, UserHandle.OWNER);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void registerForBroadcastsEx(IntentFilter intentFilter) {
        if (Boolean.valueOf(SystemProperties.getBoolean("ro.vendor.config.hw_vowifi", false)).booleanValue()) {
            intentFilter.addAction(ACTION_VOWIFI_STARTED);
            intentFilter.addAction(ACTION_VOWIFI_STARTED_MTK);
        }
    }

    /* access modifiers changed from: protected */
    public boolean mdmForPolicyForceOpenWifi(boolean showToast, boolean enable) {
        if (!HwDeviceManager.disallowOp(52) || enable) {
            return false;
        }
        if (!showToast) {
            return true;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass12 */

            public void run() {
                Toast.makeText(HwWifiService.this.mContext, HwWifiService.this.mContext.getString(33686052), 0).show();
            }
        });
        return true;
    }

    public void factoryReset(String packageName) {
        HwWifiService.super.factoryReset(packageName);
        if (SystemProperties.getBoolean("ro.config.hw_preset_ap", false)) {
            boolean isWifiScanningAlwaysAvailable = true;
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) != 1) {
                isWifiScanningAlwaysAvailable = false;
            }
            if (isWifiScanningAlwaysAvailable) {
                Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0);
            }
            setWifiEnabled(this.mContext.getPackageName(), false);
            this.mNetworkResetHandler.postDelayed(new Runnable() {
                /* class com.android.server.wifi.HwWifiService.AnonymousClass13 */

                public void run() {
                    boolean unused = HwWifiService.this.removeWpaSupplicantConf();
                    HwWifiService hwWifiService = HwWifiService.this;
                    hwWifiService.setWifiEnabled(hwWifiService.mContext.getPackageName(), true);
                }
            }, LOG_SCAN_RESULTS_INTERVAL_MS);
        }
    }

    /* access modifiers changed from: protected */
    public boolean startQuickttffScan(String packageName) {
        if (!"com.huawei.lbs".equals(packageName) || Settings.Global.getInt(this.mContext.getContentResolver(), QTTFF_WIFI_SCAN_ENABLED, 0) != 1) {
            return false;
        }
        HwHiSLog.d(TAG, false, "quickttff request  2.4G wifi scan", new Object[0]);
        if (this.lastScanResultsAvailableTime == 0 || this.mClock.getElapsedSinceBootMillis() - this.lastScanResultsAvailableTime >= 5000) {
            HwHiSLog.d(TAG, false, "Start 2.4G wifi scan.", new Object[0]);
            if (!WifiInjector.getInstance().getClientModeImplHandler().runWithScissors(new Runnable(packageName) {
                /* class com.android.server.wifi.$$Lambda$HwWifiService$bNOLyr8oakjuxLhdkXG9XdmZD4 */
                private final /* synthetic */ String f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    HwWifiService.this.lambda$startQuickttffScan$0$HwWifiService(this.f$1);
                }
            }, 0)) {
                HwHiLog.w(TAG, false, "Failed to post runnable to start scan in startQuickttffScan", new Object[0]);
                return false;
            }
        } else {
            HwHiSLog.d(TAG, false, "The scan results is fresh.", new Object[0]);
            Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
            intent.addFlags(67108864);
            intent.putExtra("resultsUpdated", true);
            intent.setPackage("com.huawei.lbs");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
        return true;
    }

    public /* synthetic */ void lambda$startQuickttffScan$0$HwWifiService(String packageName) {
        wifiStateMachineUtils.getScanRequestProxy(this.mClientModeImpl).startScanForSpecBand(Binder.getCallingUid(), packageName, 1);
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: protected */
    public boolean limitForegroundWifiScanRequest(String packageName, int uid) {
        long id = Binder.clearCallingIdentity();
        try {
            NetLocationStrategy wifiScanStrategy = HwServiceFactory.getNetLocationStrategy(packageName, uid, 1);
            Binder.restoreCallingIdentity(id);
            if (wifiScanStrategy == null) {
                HwHiSLog.e(TAG, false, "Get wifiScanStrategy from iAware is null.", new Object[0]);
                return false;
            }
            HwHiSLog.d(TAG, false, "Get wifiScanStrategy from iAware, WifiScanStrategy = %{public}s", new Object[]{wifiScanStrategy.toString()});
            if (wifiScanStrategy.getCycle() == -1) {
                return true;
            }
            if (wifiScanStrategy.getCycle() == 0) {
                return false;
            }
            if (wifiScanStrategy.getCycle() <= 0) {
                HwHiSLog.e(TAG, false, "Invalid wifiScanStrategy.", new Object[0]);
                return false;
            } else if (this.lastScanResultsAvailableTime > wifiScanStrategy.getTimeStamp()) {
                long msSinceLastScan = this.mClock.getElapsedSinceBootMillis() - this.lastScanResultsAvailableTime;
                if (msSinceLastScan <= wifiScanStrategy.getCycle()) {
                    return true;
                }
                HwHiSLog.d(TAG, false, "Last scan started %{public}s ms ago, cann't limit current scan request.", new Object[]{String.valueOf(msSinceLastScan)});
                return false;
            } else {
                HwHiSLog.d(TAG, false, "Cann't limit current scan request, lastScanResultsAvailableTime = %{public}s", new Object[]{String.valueOf(this.lastScanResultsAvailableTime)});
                return false;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(id);
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public boolean limitWifiScanRequest(String packageName) {
        if (this.mWifiScanBlacklist.contains(packageName)) {
            return isGnssLocationFix();
        }
        return false;
    }

    private boolean isGnssLocationFix() {
        boolean isGnssLocationFix = Settings.Global.getInt(this.mContext.getContentResolver(), GNSS_LOCATION_FIX_STATUS, 0) == 1;
        HwHiLog.d(TAG, false, "isGnssLocationFix =%{public}s", new Object[]{String.valueOf(isGnssLocationFix)});
        return isGnssLocationFix;
    }

    private void loadWifiScanBlacklist() {
        String[] blackList = this.mContext.getResources().getStringArray(33816591);
        this.mWifiScanBlacklist.clear();
        if (blackList != null) {
            this.mWifiScanBlacklist.addAll(Arrays.asList(blackList));
            HwHiLog.d(TAG, false, "mWifiScanBlacklist =%{public}s", new Object[]{this.mWifiScanBlacklist.toString()});
        }
    }

    /* access modifiers changed from: private */
    public void updateWifiScanblacklist() {
        this.mWifiScanBlacklist.clear();
        this.mWifiScanBlacklist.addAll(BackgroundAppScanManager.getInstance().getPackagBlackList());
    }

    /* access modifiers changed from: protected */
    public boolean limitWifiScanInAbsoluteRest(String packageName) {
        boolean requestFromBackground = isRequestFromBackground(packageName);
        HwHiLog.d(TAG, false, "mIsAbsoluteRest =%{public}s, mPluggedType =%{public}d, mHasScanned =%{public}s, requestFromBackground =%{public}s", new Object[]{String.valueOf(this.mIsAbsoluteRest), Integer.valueOf(this.mPluggedType), String.valueOf(this.mHasScanned), String.valueOf(requestFromBackground)});
        if (this.mIsAbsoluteRest && this.mPluggedType == 0 && requestFromBackground && this.mHasScanned) {
            return true;
        }
        this.mHasScanned = true;
        return false;
    }

    private boolean isRequestFromBackground(String packageName) {
        boolean z = false;
        if (Binder.getCallingUid() == 1000 || Binder.getCallingUid() == CODE_SET_WIFI_AP_EVALUATE_ENABLED || TextUtils.isEmpty(packageName) || PROCESS_BD.equals(packageName) || PROCESS_GD.equals(packageName)) {
            return false;
        }
        this.mAppOps.checkPackage(Binder.getCallingUid(), packageName);
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            if (this.mActivityManager.getPackageImportance(packageName) > 125) {
                z = true;
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /* access modifiers changed from: private */
    public boolean removeWpaSupplicantConf() {
        Object[] objArr;
        boolean ret = false;
        try {
            File conf = Environment.buildPath(Environment.getDataDirectory(), new String[]{"misc", "wifi", "wpa_supplicant.conf"});
            HwHiSLog.d(TAG, false, "conf path: %{public}s", new Object[]{conf.getPath()});
            if (conf.exists()) {
                ret = conf.delete();
            }
            objArr = new Object[]{String.valueOf(ret)};
        } catch (SecurityException e) {
            HwHiSLog.e(TAG, false, "delete conf error : %{public}s", new Object[]{e.getMessage()});
            objArr = new Object[]{String.valueOf(false)};
        } catch (Throwable th) {
            HwHiSLog.i(TAG, false, "delete conf result : %{public}s", new Object[]{String.valueOf(false)});
            throw th;
        }
        HwHiSLog.i(TAG, false, "delete conf result : %{public}s", objArr);
        return ret;
    }

    private boolean isSoftApEnabled() {
        return wifiServiceUtils.getSoftApState(this).intValue() == 13;
    }

    private boolean checkSignMatchOrIsSystemApp() {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        int matchResult = pm.checkSignatures(Binder.getCallingUid(), Process.myUid());
        if (matchResult == 0) {
            return true;
        }
        try {
            String pckName = getAppName(Binder.getCallingPid());
            if (pckName == null) {
                HwHiSLog.e(TAG, false, "pckName is null", new Object[0]);
                return false;
            }
            ApplicationInfo info = pm.getApplicationInfo(pckName, 0);
            if (info != null && (info.flags & 1) != 0) {
                return true;
            }
            HwHiSLog.d(TAG, false, "HwWifiService  checkSignMatchOrIsSystemAppMatch matchRe=%{public}d pckName=%{public}s", new Object[]{Integer.valueOf(matchResult), pckName});
            return false;
        } catch (Exception e) {
            HwHiSLog.e(TAG, false, "isSystemApp not found", new Object[0]);
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public void sendFailedScanDirectionalBroadcast(String packageName) {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
            intent.addFlags(67108864);
            intent.putExtra("resultsUpdated", false);
            intent.setPackage(packageName);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    /* access modifiers changed from: protected */
    public List<ScanResult> getFilterScanResults(String callingPackage, int uid, List<ScanResult> scanResults) {
        List<ScanResult> scanResultsList = scanResults;
        Iterator<ScanResult> it = scanResultsList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ScanResult scanResult = it.next();
            if (scanResult.capabilities.contains(OWE_TRANSITION_IN_CAPABILITY) && scanResult.capabilities.contains(PMFR_IN_CAPABILITY) && !TextUtils.isEmpty(scanResult.SSID)) {
                scanResult.SSID = "";
                break;
            }
        }
        Collections.sort(scanResultsList, new Comparator<ScanResult>() {
            /* class com.android.server.wifi.HwWifiService.AnonymousClass14 */

            public int compare(ScanResult o1, ScanResult o2) {
                if (o1.timestamp > o2.timestamp) {
                    return -1;
                }
                if (o1.timestamp < o2.timestamp) {
                    return 1;
                }
                return 0;
            }
        });
        if (scanResultsList.size() > 200) {
            HwHiLog.d(TAG, false, "ScanResults exceed the max count. size = %{public}d", new Object[]{Integer.valueOf(scanResultsList.size())});
            scanResultsList = scanResultsList.subList(0, 200);
        }
        logScanResultsListRestrictively(callingPackage, scanResultsList);
        return scanResultsList;
    }

    private void logScanResultsListRestrictively(String callingPackage, List<ScanResult> scanResultsList) {
        long currentLogTime = this.mClock.getElapsedSinceBootMillis();
        if ("com.android.settings".equals(callingPackage) && scanResultsList != null && currentLogTime - this.mLastLogScanResultsTime > LOG_SCAN_RESULTS_INTERVAL_MS) {
            Set<String> ssids = new HashSet<>();
            StringBuilder sb = new StringBuilder();
            for (ScanResult scanResult : scanResultsList) {
                String ssid = scanResult.SSID;
                if (!ssids.contains(ssid)) {
                    ssids.add(ssid);
                    sb.append(StringUtilEx.safeDisplaySsid(ssid));
                    sb.append("|");
                    sb.append(scanResult.isHiLinkNetwork);
                    sb.append("|");
                    sb.append(scanResult.dot11vNetwork);
                    sb.append(" ");
                }
            }
            HwHiLog.d(TAG, false, "getScanResults: calling by %{public}s  includes: %{public}s", new Object[]{callingPackage, sb.toString()});
            this.mLastLogScanResultsTime = currentLogTime;
        }
    }

    /* access modifiers changed from: protected */
    public boolean isScreenOff(String packageName) {
        HwHiSLog.d(TAG, false, "startScan, pid:%{public}d, uid:%{public}d, appName:%{public}s", new Object[]{Integer.valueOf(Binder.getCallingPid()), Integer.valueOf(Binder.getCallingUid()), packageName});
        if (this.mPowerManager.isScreenOn() || "com.huawei.ca".equals(packageName) || "com.huawei.parentcontrol".equals(packageName) || Binder.getCallingUid() == 1000 || "com.huawei.hidisk".equals(packageName)) {
            return false;
        }
        HwHiSLog.i(TAG, false, "Screen is off, %{public}s startScan is skipped.", new Object[]{packageName});
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean isWifiDualBandSupport() {
        int value = Settings.Global.getInt(this.mContext.getContentResolver(), "hw_wifi_ap_band", 0);
        if (value == 1) {
            return false;
        }
        if (value == 2) {
            return true;
        }
        return this.mContext.getResources().getBoolean(17891578);
    }

    /* access modifiers changed from: protected */
    public String getAppName(int pID) {
        List<ActivityManager.RunningAppProcessInfo> appProcessList = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == pID) {
                return appProcess.processName;
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public String getPackageName(int pID) {
        List<ActivityManager.RunningAppProcessInfo> appProcessList = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == pID && appProcess.pkgList != null && appProcess.pkgList.length > 0) {
                return appProcess.pkgList[0];
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public boolean isWifiScanRequestRefused(String packageName) {
        if (limitWifiScanRequest(packageName)) {
            HwHiLog.d(TAG, false, "current scan request is refused %{public}s", new Object[]{packageName});
            sendFailedScanDirectionalBroadcast(packageName);
            return true;
        } else if (limitWifiScanInAbsoluteRest(packageName)) {
            HwHiLog.d(TAG, false, "absolute rest, scan request is refused %{public}s", new Object[]{packageName});
            sendFailedScanDirectionalBroadcast(packageName);
            return true;
        } else if (!restrictWifiScanRequest(packageName)) {
            return false;
        } else {
            HwHiSLog.i(TAG, false, "scan ctrl by PG, skip %{public}s", new Object[]{packageName});
            return true;
        }
    }

    /* access modifiers changed from: protected */
    public void sendBehavior(IHwBehaviorCollectManager.BehaviorId bid) {
        synchronized (mWifiLock) {
            if (this.mHwBehaviorManager == null) {
                this.mHwBehaviorManager = HwFrameworkFactory.getHwBehaviorCollectManager();
            }
        }
        IHwBehaviorCollectManager iHwBehaviorCollectManager = this.mHwBehaviorManager;
        if (iHwBehaviorCollectManager != null) {
            try {
                iHwBehaviorCollectManager.sendBehavior(Binder.getCallingUid(), Binder.getCallingPid(), bid);
            } catch (Exception e) {
                HwHiLog.e(TAG, false, "sendBehavior fail", new Object[0]);
            }
        } else {
            HwHiLog.w(TAG, false, "HwBehaviorCollectManager is null", new Object[0]);
        }
    }

    private class FactoryMacWhiteList {
        protected String mPackageName;
        protected int mVersionMax;
        protected int mVersionMin;

        public FactoryMacWhiteList(String packageName, int versionMin, int versionMax) {
            this.mPackageName = packageName;
            this.mVersionMin = versionMin;
            this.mVersionMax = versionMax;
        }

        /* access modifiers changed from: protected */
        public int getVersionMin() {
            return this.mVersionMin;
        }

        /* access modifiers changed from: protected */
        public int getVersionMax() {
            return this.mVersionMax;
        }
    }

    /* access modifiers changed from: protected */
    public boolean isAppInFactoryMacWhiteList(int uid) {
        Map<String, FactoryMacWhiteList> map;
        String packageName = this.mContext.getPackageManager().getNameForUid(uid);
        if (TextUtils.isEmpty(packageName) || (map = this.mFactoryMacWhiteListMap) == null || map.size() == 0 || !this.mFactoryMacWhiteListMap.containsKey(packageName)) {
            return false;
        }
        int versionCode = -1;
        FactoryMacWhiteList factoryMacWhiteList = this.mFactoryMacWhiteListMap.get(packageName);
        if (factoryMacWhiteList != null) {
            try {
                ApplicationInfo appInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, 0);
                if (appInfo != null) {
                    versionCode = appInfo.versionCode;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "randommac, NameNotFoundException: " + e.getMessage());
            }
            if ((factoryMacWhiteList.getVersionMin() == -1 && factoryMacWhiteList.getVersionMax() == -1) || (versionCode >= factoryMacWhiteList.getVersionMin() && versionCode <= factoryMacWhiteList.getVersionMax())) {
                Log.d(TAG, "randommac, packageName:" + packageName + " is in white list, versionCode = " + versionCode);
                return true;
            }
        }
        return false;
    }

    private void parseFactoryMacWhiteListFile(Context context) {
        String packageName = null;
        int versionMin = -1;
        InputStream inputStream = null;
        try {
            XmlPullParser parser = Xml.newPullParser();
            File whitelistFile = new File(CONF_FILE_NAME);
            if (!whitelistFile.exists()) {
                Log.e(TAG, "wifi factory mac whitelist doesn't exist");
                return;
            }
            inputStream = new FileInputStream(whitelistFile);
            parser.setInput(inputStream, "UTF-8");
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType == 0) {
                    Log.d(TAG, "randommac, START_DOCUMENT");
                } else if (eventType != 2) {
                    if (eventType != 3) {
                    }
                } else if (XML_TAG_VERSION.equals(parser.getName())) {
                    Log.d(TAG, "randommac, wifi_factory_mac_whitelist VERSION = " + parser.nextText());
                } else if (XML_TAG_PACKAGE.equals(parser.getName())) {
                    packageName = parser.nextText();
                } else if (XML_TAG_VERSION_MIN.equals(parser.getName())) {
                    try {
                        versionMin = Integer.parseInt(parser.nextText());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "randommac, NumberFormatException");
                    }
                } else if (XML_TAG_VERSION_MAX.equals(parser.getName())) {
                    try {
                        int versionMax = Integer.parseInt(parser.nextText());
                        if (!TextUtils.isEmpty(packageName)) {
                            Log.d(TAG, "randommac, packageName: " + packageName + ", versionMin: " + versionMin + ", versionMax: " + versionMax);
                            this.mFactoryMacWhiteListMap.put(packageName, new FactoryMacWhiteList(packageName, versionMin, versionMax));
                        }
                    } catch (NumberFormatException e2) {
                        Log.e(TAG, "randommac, NumberFormatException");
                    }
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    Log.e(TAG, "randommac, IOException: close input stream error");
                }
            }
        } catch (IOException e4) {
            Log.e(TAG, "randommac, IOException: open file error");
        } catch (XmlPullParserException e5) {
            Log.e(TAG, "randommac, XmlPullParserException: prase file error");
        }
    }

    /* access modifiers changed from: protected */
    public void notifyWifiConfigChanged(WifiConfiguration newConfig, String packageName, boolean isBlacklistChanged) {
        WifiConfiguration oldConfig;
        if (this.mHwWifiChrService != null && newConfig != null && packageName != null && (oldConfig = getWifiApConfiguration()) != null) {
            int ssidTag = 0;
            if (oldConfig.SSID != null && !oldConfig.SSID.equals(newConfig.SSID)) {
                ssidTag = 1;
            }
            String securityType = SECURITY_NO_CHANGE;
            if (!(oldConfig.getAuthType() == newConfig.getAuthType() || WifiConfiguration.KeyMgmt.strings[newConfig.getAuthType()] == null)) {
                securityType = WifiConfiguration.KeyMgmt.strings[newConfig.getAuthType()];
            }
            int passwordTag = 0;
            if ((oldConfig.preSharedKey != null && !oldConfig.preSharedKey.equals(newConfig.preSharedKey)) || (oldConfig.preSharedKey == null && newConfig.preSharedKey != null)) {
                passwordTag = 1;
            }
            int apBand = -1;
            if (oldConfig.apBand != newConfig.apBand) {
                apBand = newConfig.apBand;
            }
            int apChannel = -1;
            if (oldConfig.apChannel != newConfig.apChannel) {
                apChannel = newConfig.apChannel;
            }
            int blacklistTag = 0;
            if (isBlacklistChanged) {
                blacklistTag = 1;
            }
            if (!SECURITY_NO_CHANGE.equals(securityType) || ssidTag != 0 || passwordTag != 0 || apBand != -1 || apChannel != -1 || blacklistTag != 0) {
                Bundle data = new Bundle();
                data.putString(KEY_APK_NAME, packageName);
                data.putString(KEY_SECURITY_TYPE, securityType);
                data.putInt(KEY_SSID_CHANGE, ssidTag);
                data.putInt(KEY_PASSWORD_CHANGE, passwordTag);
                data.putInt(KEY_BAND_CHANGE, apBand);
                data.putInt(KEY_CHANNEL_CHANGE, apChannel);
                data.putInt(KEY_BLACKLIST_CHANGE, blacklistTag);
                this.mHwWifiChrService.uploadDFTEvent(24, data);
            }
        }
    }

    private void notifyBlacklistEvent(String macFilter) {
        if (this.mHwWifiChrService != null && !TextUtils.isEmpty(this.mMacFilterRecord) && !TextUtils.isEmpty(macFilter) && !this.mMacFilterRecord.equals(macFilter)) {
            WifiConfiguration config = getWifiApConfiguration();
            String packageName = getAppName(Binder.getCallingPid());
            if (!(config == null || packageName == null)) {
                notifyWifiConfigChanged(config, packageName, true);
            }
        }
        this.mMacFilterRecord = macFilter;
    }

    private boolean isAllowAccessWifiService() {
        String packageName = getAppName(Binder.getCallingPid());
        for (String allowName : ACCESS_WIFI_WHITELIST) {
            if (allowName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void enforceCallerPermission(String permission) {
        this.mContext.enforceCallingOrSelfPermission(permission, "WifiService");
    }

    /* access modifiers changed from: protected */
    public Bundle parseZcaller(String caller) {
        if (TextUtils.isEmpty(caller)) {
            return Bundle.EMPTY;
        }
        String[] infos = caller.split(";");
        if (infos == null || infos.length != 3) {
            return Bundle.EMPTY;
        }
        Bundle bundle = new Bundle();
        int length = infos.length;
        int i = 0;
        while (i < length) {
            String[] kv = infos[i].split(":");
            if (kv == null || kv.length != 2) {
                return Bundle.EMPTY;
            }
            try {
                if ("uid".equals(kv[0])) {
                    bundle.putInt(kv[0], Integer.valueOf(kv[1]).intValue());
                } else if ("pid".equals(kv[0])) {
                    bundle.putInt(kv[0], Integer.valueOf(kv[1]).intValue());
                } else if (!"pkg".equals(kv[0]) || TextUtils.isEmpty(kv[1])) {
                    Log.d(TAG, "Not contain any valid info: " + caller);
                } else {
                    bundle.putString(kv[0], kv[1]);
                }
                i++;
            } catch (NumberFormatException e) {
                return Bundle.EMPTY;
            }
        }
        if (bundle.size() == 3) {
            return bundle;
        }
        return Bundle.EMPTY;
    }

    /* access modifiers changed from: protected */
    public void handleAirplaneNotSensitiveWifi() {
        if (this.mSettingsStore.isAirplaneModeOn()) {
            HwHiLog.i(TAG, false, "Airplane mode toggled, shutdown all modes except wifi", new Object[0]);
            this.mActiveModeWarden.stopSoftAPMode(-1);
        }
    }
}
