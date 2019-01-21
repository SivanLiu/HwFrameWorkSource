package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.AppOpsManager;
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
import android.net.wifi.PPPOEConfig;
import android.net.wifi.PPPOEInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiDetectConfInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.rms.iaware.NetLocationStrategy;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.widget.Toast;
import com.android.internal.util.AsyncChannel;
import com.android.server.HwServiceFactory;
import com.android.server.PPPOEStateMachine;
import com.android.server.hidata.wavemapping.dataprovider.FrequentLocation;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifi.MSS.HwMSSHandler;
import com.android.server.wifi.wifipro.WifiProStateMachine;
import com.android.server.wifipro.WifiProCommonUtils;
import com.hisi.mapcon.IMapconService;
import com.hisi.mapcon.IMapconService.Stub;
import com.hisi.mapcon.IMapconServiceCallback;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class HwWifiService extends WifiServiceImpl {
    private static final String ACCESS_WIFI_FILTER_PERMISSION = "com.huawei.wifi.permission.ACCESS_FILTER";
    private static final String ACTION_VOWIFI_STARTED = "com.hisi.vowifi.started";
    private static final int BACKGROUND_IMPORTANCE_CUTOFF = 125;
    private static final int CODE_DISABLE_RX_FILTER = 3021;
    private static final int CODE_ENABLE_HILINK_HANDSHAKE = 2001;
    private static final int CODE_ENABLE_RX_FILTER = 3022;
    private static final int CODE_EXTEND_WIFI_SCAN_PERIOD_FOR_P2P = 2006;
    private static final int CODE_GET_APLINKED_STA_LIST = 1005;
    private static final int CODE_GET_CONNECTION_RAW_PSK = 2002;
    private static final int CODE_GET_PPPOE_INFO_CONFIG = 1004;
    private static final int CODE_GET_RSDB_SUPPORTED_MODE = 2008;
    private static final int CODE_GET_SINGNAL_INFO = 1011;
    private static final int CODE_GET_SOFTAP_CHANNEL_LIST = 1009;
    private static final int CODE_GET_VOWIFI_DETECT_MODE = 1013;
    private static final int CODE_GET_VOWIFI_DETECT_PERIOD = 1015;
    private static final int CODE_GET_WPA_SUPP_CONFIG = 1001;
    private static final int CODE_IS_BG_LIMIT_ALLOWED = 3008;
    private static final int CODE_IS_SUPPORT_VOWIFI_DETECT = 1016;
    private static final int CODE_PROXY_WIFI_LOCK = 3009;
    private static final int CODE_REQUEST_FRESH_WHITE_LIST = 2007;
    private static final int CODE_REQUEST_WIFI_ENABLE = 2004;
    private static final int CODE_RESTRICT_WIFI_SCAN = 4001;
    private static final int CODE_SET_SOFTAP_DISASSOCIATESTA = 1007;
    private static final int CODE_SET_SOFTAP_MACFILTER = 1006;
    private static final int CODE_SET_VOWIFI_DETECT_MODE = 1012;
    private static final int CODE_SET_VOWIFI_DETECT_PERIOD = 1014;
    private static final int CODE_SET_WIFI_ANTSET = 3007;
    private static final int CODE_SET_WIFI_AP_EVALUATE_ENABLED = 1010;
    private static final int CODE_SET_WIFI_TXPOWER = 2005;
    private static final int CODE_START_PPPOE_CONFIG = 1002;
    private static final int CODE_START_WIFI_KEEP_ALIVE = 3023;
    private static final int CODE_STOP_PPPOE_CONFIG = 1003;
    private static final int CODE_STOP_WIFI_KEEP_ALIVE = 3024;
    private static final int CODE_UPDATE_APP_EXPERIENCE_STATUS = 3006;
    private static final int CODE_UPDATE_APP_RUNNING_STATUS = 3005;
    private static final int CODE_UPDATE_WM_FREQ_LOC = 4002;
    private static final int CODE_USER_HANDOVER_WIFI = 1008;
    private static final int CODE_WIFI_QOE_EVALUATE = 3003;
    private static final int CODE_WIFI_QOE_START_MONITOR = 3001;
    private static final int CODE_WIFI_QOE_STOP_MONITOR = 3002;
    private static final int CODE_WIFI_QOE_UPDATE_STATUS = 3004;
    private static final boolean DBG = true;
    private static final String DESCRIPTOR = "android.net.wifi.IWifiManager";
    private static final int[] FREQUENCYS = new int[]{2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462, 2467, 2472};
    private static final String GNSS_LOCATION_FIX_STATUS = "GNSS_LOCATION_FIX_STATUS";
    private static final int MAPCON_SERVICE_SHUTDOWN_TIMEOUT = 5000;
    private static final int MSG_AIRPLANE_TOGGLED_MAPCON_TIMEOUT = 1;
    private static final int MSG_DISABLE_WIFI_MAPCON_TIMEOUT = 2;
    private static final int MSG_FORGET_NETWORK_MAPCON_TIMEOUT = 0;
    private static final String PG_AR_STATE_ACTION = "com.huawei.intent.action.PG_AR_STATE_ACTION";
    private static final String PG_RECEIVER_PERMISSION = "com.huawei.powergenie.receiverPermission";
    private static final String PPPOE_TAG = "PPPOEWIFIService";
    private static final String PROCESS_BD = "com.baidu.map.location";
    private static final String PROCESS_GD = "com.amap.android.ams";
    private static final String QTTFF_WIFI_SCAN_ENABLED = "qttff_wifi_scan_enabled";
    private static final int QTTFF_WIFI_SCAN_INTERVAL_MS = 5000;
    private static final String TAG = "HwWifiService";
    private static final String VOWIFI_WIFI_DETECT_PERMISSION = "com.huawei.permission.VOWIFI_WIFI_DETECT";
    public static final int WHITE_LIST_TYPE_WIFI_SLEEP = 7;
    private static final int WIFISCANSTRATEGY_ALLOWABLE = 0;
    private static final int WIFISCANSTRATEGY_FORBIDDEN = -1;
    private static HashSet<String> restrictWifiScanPkgSet = new HashSet();
    private static WifiServiceUtils wifiServiceUtils = ((WifiServiceUtils) EasyInvokeFactory.getInvokeUtils(WifiServiceUtils.class));
    private static WifiStateMachineUtils wifiStateMachineUtils = ((WifiStateMachineUtils) EasyInvokeFactory.getInvokeUtils(WifiStateMachineUtils.class));
    private final ServiceConnection conn;
    private boolean isPPPOE;
    private volatile boolean isRxFilterDisabled;
    private long lastScanResultsAvailableTime;
    private final ActivityManager mActivityManager;
    private final IMapconServiceCallback mAirPlaneCallback;
    private final AppOpsManager mAppOps;
    private final IMapconServiceCallback mCallback;
    private final Clock mClock;
    private Context mContext;
    private List<HwFilterLock> mFilterLockList;
    private final Object mFilterSynchronizeLock;
    private boolean mHasScanned;
    private boolean mIsAbsoluteRest;
    private Handler mMapconHandler;
    private HandlerThread mMapconHandlerTread;
    private IMapconService mMapconService;
    private Handler mNetworkResetHandler;
    private PPPOEStateMachine mPPPOEStateMachine;
    private int mPluggedType;
    private PowerManager mPowerManager;
    private boolean mVowifiServiceOn;
    private WifiProStateMachine mWifiProStateMachine;
    private final ArraySet<String> mWifiScanBlacklist;

    private final class HwFilterLock implements DeathRecipient {
        public final IBinder mToken;

        HwFilterLock(IBinder token) {
            this.mToken = token;
        }

        public void binderDied() {
            HwWifiService.this.handleFilterLockDeath(this);
        }
    }

    public HwWifiService(Context context, WifiInjector wifiInjector, AsyncChannel asyncChannel) {
        super(context, wifiInjector, asyncChannel);
        boolean z = true;
        if (SystemProperties.getInt("ro.config.pppoe_enable", 0) != 1) {
            z = false;
        }
        this.isPPPOE = z;
        this.lastScanResultsAvailableTime = 0;
        this.mWifiScanBlacklist = new ArraySet();
        this.mIsAbsoluteRest = false;
        this.mHasScanned = false;
        this.mFilterLockList = new ArrayList();
        this.isRxFilterDisabled = false;
        this.mFilterSynchronizeLock = new Object();
        this.conn = new ServiceConnection() {
            public void onServiceDisconnected(ComponentName name) {
                Log.d(HwWifiService.TAG, "onServiceDisconnected,IMapconService");
                HwWifiService.this.mMapconService = null;
                HwWifiService.this.mVowifiServiceOn = false;
                HwWifiService.this.mMapconHandlerTread.quit();
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(HwWifiService.TAG, "onServiceConnected,IMapconService");
                HwWifiService.this.mMapconService = Stub.asInterface(service);
                HwWifiService.this.mMapconHandlerTread = new HandlerThread("MapconHandler");
                HwWifiService.this.mMapconHandlerTread.start();
                HwWifiService.this.mMapconHandler = new Handler(HwWifiService.this.mMapconHandlerTread.getLooper()) {
                    public void handleMessage(Message msg) {
                        String str = HwWifiService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("handle TimeoutMessage,msg:");
                        stringBuilder.append(msg.what);
                        Log.d(str, stringBuilder.toString());
                        WifiController controller = HwWifiService.wifiServiceUtils.getWifiController(HwWifiService.this);
                        switch (msg.what) {
                            case 0:
                                HwWifiService.this.mWifiStateMachine.sendMessage(Message.obtain(msg.obj));
                                return;
                            case 1:
                                if (controller != null) {
                                    controller.sendMessage(155657);
                                    return;
                                }
                                return;
                            case 2:
                                if (controller != null) {
                                    controller.sendMessage(155656);
                                    return;
                                }
                                return;
                            default:
                                return;
                        }
                    }
                };
                HwWifiService.this.mVowifiServiceOn = true;
            }
        };
        this.mCallback = new IMapconServiceCallback.Stub() {
            public void onVoWifiCloseDone() {
                Log.d(HwWifiService.TAG, "onVoWifiCloseDone: cancel delayed message,send CMD_WIFI_TOGGLED");
                if (HwWifiService.this.mMapconHandler.hasMessages(2)) {
                    HwWifiService.this.mMapconHandler.removeMessages(2);
                }
                WifiController controller = HwWifiService.wifiServiceUtils.getWifiController(HwWifiService.this);
                if (controller != null) {
                    controller.sendMessage(155656);
                }
            }
        };
        this.mAirPlaneCallback = new IMapconServiceCallback.Stub() {
            public void onVoWifiCloseDone() {
                Log.d(HwWifiService.TAG, "onVoWifiCloseDone: cancel delayed message, send CMD_AIRPLANE_TOGGLED");
                if (HwWifiService.this.mMapconHandler.hasMessages(1)) {
                    HwWifiService.this.mMapconHandler.removeMessages(1);
                }
                WifiController controller = HwWifiService.wifiServiceUtils.getWifiController(HwWifiService.this);
                if (controller != null) {
                    controller.sendMessage(155657);
                }
            }
        };
        this.mNetworkResetHandler = new Handler();
        this.mContext = context;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        if (this.isPPPOE) {
            this.mPPPOEStateMachine = new PPPOEStateMachine(this.mContext, PPPOE_TAG);
            this.mPPPOEStateMachine.start();
        }
        this.mClock = wifiInjector.getClock();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.net.wifi.SCAN_RESULTS".equals(intent.getAction()) && intent.getBooleanExtra("resultsUpdated", false)) {
                    HwWifiService.this.lastScanResultsAvailableTime = HwWifiService.this.mClock.getElapsedSinceBootMillis();
                }
            }
        }, filter);
        loadWifiScanBlacklist();
        BackgroundAppScanManager.getInstance().registerBlackListChangeListener(new BlacklistListener() {
            public void onBlacklistChange(List<String> list) {
                HwWifiService.this.updateWifiScanblacklist();
            }
        });
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    HwWifiService.this.mIsAbsoluteRest = intent.getBooleanExtra("stationary", false);
                    String str = HwWifiService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mIsAbsoluteRest =");
                    stringBuilder.append(HwWifiService.this.mIsAbsoluteRest);
                    Log.d(str, stringBuilder.toString());
                    if (HwWifiService.this.mIsAbsoluteRest) {
                        HwWifiService.this.mHasScanned = false;
                    }
                }
            }
        }, new IntentFilter(PG_AR_STATE_ACTION), PG_RECEIVER_PERMISSION, null);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    HwWifiService.this.mPluggedType = intent.getIntExtra("plugged", 0);
                    if (HwWifiService.this.mPluggedType == 0 && (HwWifiService.this.mWifiStateMachine instanceof HwWifiStateMachine) && HwWifiService.this.mWifiStateMachine.getChargingState()) {
                        HwWifiService.this.mPluggedType = 2;
                    }
                    String str = HwWifiService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mPluggedType =");
                    stringBuilder.append(HwWifiService.this.mPluggedType);
                    Log.d(str, stringBuilder.toString());
                }
            }
        }, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", "WifiService");
    }

    protected boolean enforceStopScanSreenOff() {
        if (this.mPowerManager.isScreenOn() || "com.huawei.ca".equals(getAppName(Binder.getCallingPid()))) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Screen is off, ");
        stringBuilder.append(getAppName(Binder.getCallingPid()));
        stringBuilder.append(" startScan is skipped.");
        Slog.i(str, stringBuilder.toString());
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

    protected boolean restrictWifiScanRequest(String packageName) {
        if (restrictWifiScanPkgSet.contains(packageName)) {
            return true;
        }
        return false;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        PPPOEConfig _arg0 = null;
        boolean z = false;
        String result;
        List<String> result2;
        HwWifiStateMachine mHwWifiStateMachine;
        String str;
        StringBuilder stringBuilder;
        boolean enablen;
        WifiDetectConfInfo _arg02;
        int result3;
        switch (code) {
            case CODE_GET_WPA_SUPP_CONFIG /*1001*/:
                if (checkSignMatchOrIsSystemApp()) {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    Slog.d(TAG, "WifiService  getWpaSuppConfig");
                    result = "";
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        result = this.mWifiStateMachine.getWpaSuppConfig();
                    }
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
                Slog.e(TAG, "WifiService  CODE_GET_WPA_SUPP_CONFIG SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                reply.writeString(null);
                return true;
            case CODE_START_PPPOE_CONFIG /*1002*/:
                if (checkSignMatchOrIsSystemApp()) {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    Slog.d(TAG, "WifiService  startPPPOE");
                    if (this.isPPPOE) {
                        if (data.readInt() != 0) {
                            _arg0 = (PPPOEConfig) PPPOEConfig.CREATOR.createFromParcel(data);
                        }
                        this.mPPPOEStateMachine.sendMessage(589825, _arg0);
                        reply.writeNoException();
                        return true;
                    }
                    Slog.w(TAG, "the PPPOE function is closed.");
                    return false;
                }
                Slog.e(TAG, "WifiService  CODE_START_PPPOE_CONFIG SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                return false;
            case CODE_STOP_PPPOE_CONFIG /*1003*/:
                if (checkSignMatchOrIsSystemApp()) {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    Slog.d(TAG, "WifiService  stopPPPOE");
                    if (this.isPPPOE) {
                        this.mPPPOEStateMachine.sendMessage(589826);
                        reply.writeNoException();
                        return true;
                    }
                    Slog.w(TAG, "the PPPOE function is closed.");
                    return false;
                }
                Slog.e(TAG, "WifiService  CODE_STOP_PPPOE_CONFIG SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                return false;
            case CODE_GET_PPPOE_INFO_CONFIG /*1004*/:
                if (checkSignMatchOrIsSystemApp()) {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    Slog.d(TAG, "WifiService  get PPPOE info");
                    if (this.isPPPOE) {
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
                    }
                    Slog.w(TAG, "the PPPOE function is closed.");
                    return false;
                }
                Slog.e(TAG, "WifiService  CODE_GET_PPPOE_INFO_CONFIG SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                reply.writeInt(0);
                reply.writeNoException();
                return true;
            case CODE_GET_APLINKED_STA_LIST /*1005*/:
                Slog.d(TAG, "Receive CODE_GET_APLINKED_STA_LIST");
                if (checkSignMatchOrIsSystemApp()) {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    result2 = null;
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        mHwWifiStateMachine = (HwWifiStateMachine) this.mWifiStateMachine;
                        if (isSoftApEnabled()) {
                            result2 = mHwWifiStateMachine.getApLinkedStaList();
                            String regularIP = "\\.[\\d]{1,3}\\.[\\d]{1,3}\\.";
                            String regularMAC = ":[\\w]{1,}:[\\w]{1,}:";
                            if (result2 == null) {
                                Slog.d(TAG, "getApLinkedStaList result = null");
                            } else {
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("getApLinkedStaList result = ");
                                stringBuilder2.append(result2.toString().replaceAll("\\.[\\d]{1,3}\\.[\\d]{1,3}\\.", ".*.*.").replaceAll(":[\\w]{1,}:[\\w]{1,}:", ":**:**:"));
                                Slog.d(str2, stringBuilder2.toString());
                            }
                        } else {
                            Slog.w(TAG, "Receive CODE_GET_APLINKED_STA_LIST when softap state is not enabled");
                        }
                    }
                    reply.writeNoException();
                    reply.writeStringList(result2);
                    return true;
                }
                Slog.e(TAG, "WifiService  CODE_GET_APLINKED_STA_LIST SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                reply.writeStringList(null);
                return true;
            case CODE_SET_SOFTAP_MACFILTER /*1006*/:
                if (checkSignMatchOrIsSystemApp()) {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    result = data.readString();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Receive CODE_SET_SOFTAP_MACFILTER, macFilter:");
                    stringBuilder.append(result);
                    Slog.d(str, stringBuilder.toString());
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        mHwWifiStateMachine = (HwWifiStateMachine) this.mWifiStateMachine;
                        if (isSoftApEnabled()) {
                            mHwWifiStateMachine.setSoftapMacFilter(result);
                        } else {
                            Slog.w(TAG, "Receive CODE_SET_SOFTAP_MACFILTER when softap state is not enabled");
                        }
                    }
                    reply.writeNoException();
                    return true;
                }
                Slog.e(TAG, "WifiService  CODE_SET_SOFTAP_MACFILTER SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                return false;
            case CODE_SET_SOFTAP_DISASSOCIATESTA /*1007*/:
                if (checkSignMatchOrIsSystemApp()) {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    result = data.readString();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Receive CODE_SET_SOFTAP_DISASSOCIATESTA, mac = ");
                    stringBuilder.append(result);
                    Slog.d(str, stringBuilder.toString());
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        mHwWifiStateMachine = (HwWifiStateMachine) this.mWifiStateMachine;
                        if (isSoftApEnabled()) {
                            mHwWifiStateMachine.setSoftapDisassociateSta(result);
                        } else {
                            Slog.w(TAG, "Receive CODE_SET_SOFTAP_DISASSOCIATESTA when softap state is not enabled");
                        }
                    }
                    reply.writeNoException();
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_SET_SOFTAP_DISASSOCIATESTA SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                return false;
            case CODE_USER_HANDOVER_WIFI /*1008*/:
                if (checkSignMatchOrIsSystemApp()) {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    Slog.d(TAG, "HwWifiService  userHandoverWiFi ");
                    this.mWifiProStateMachine = WifiProStateMachine.getWifiProStateMachineImpl();
                    if (this.mWifiProStateMachine != null) {
                        this.mWifiProStateMachine.userHandoverWifi();
                    }
                    reply.writeNoException();
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_USER_HANDOVER_WIFI SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                return false;
            case CODE_GET_SOFTAP_CHANNEL_LIST /*1009*/:
                Slog.d(TAG, "Receive CODE_GET_SOFTAP_CHANNEL_LIST");
                if (checkSignMatchOrIsSystemApp()) {
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    int[] _result2 = null;
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        _result2 = ((HwWifiStateMachine) this.mWifiStateMachine).getSoftApChannelListFor5G();
                    }
                    reply.writeNoException();
                    reply.writeIntArray(_result2);
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_GET_SOFTAP_CHANNEL_LIST SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                reply.writeIntArray(null);
                return true;
            case 1010:
                if (checkSignMatchOrIsSystemApp()) {
                    Slog.d(TAG, "HwWifiService  SET_WIFI_AP_EVALUATE_ENABLED ");
                    data.enforceInterface(DESCRIPTOR);
                    enforceAccessPermission();
                    if (data.readInt() == 1) {
                        z = true;
                    }
                    enablen = z;
                    this.mWifiProStateMachine = WifiProStateMachine.getWifiProStateMachineImpl();
                    if (this.mWifiProStateMachine != null) {
                        this.mWifiProStateMachine.setWifiApEvaluateEnabled(enablen);
                    }
                    reply.writeNoException();
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_SET_WIFI_AP_EVALUATE_ENABLED SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                return false;
            case CODE_GET_SINGNAL_INFO /*1011*/:
                if (checkSignMatchOrIsSystemApp()) {
                    Slog.d(TAG, "HwWifiService  FETCH_WIFI_SIGNAL_INFO_FOR_VOWIFI ");
                    data.enforceInterface(DESCRIPTOR);
                    if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                        Slog.d(TAG, "fetchWifiSignalInfoForVoWiFi(): permissin deny");
                        return false;
                    }
                    byte[] result4 = null;
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        result4 = ((HwWifiStateMachine) this.mWifiStateMachine).fetchWifiSignalInfoForVoWiFi();
                    }
                    reply.writeNoException();
                    reply.writeByteArray(result4);
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_GET_SINGNAL_INFO SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                reply.writeByteArray(null);
                return false;
            case CODE_SET_VOWIFI_DETECT_MODE /*1012*/:
                if (checkSignMatchOrIsSystemApp()) {
                    Slog.d(TAG, "HwWifiService  SET_VOWIFI_DETECT_MODE ");
                    data.enforceInterface(DESCRIPTOR);
                    if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                        Slog.d(TAG, "setVoWifiDetectMode(): permissin deny");
                        return false;
                    }
                    WifiDetectConfInfo _arg03;
                    if (data.readInt() != 0) {
                        _arg03 = (WifiDetectConfInfo) WifiDetectConfInfo.CREATOR.createFromParcel(data);
                    }
                    _arg02 = _arg03;
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        this.mWifiStateMachine.setVoWifiDetectMode(_arg02);
                    }
                    reply.writeNoException();
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_SET_VOWIFI_DETECT_MODE SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                return false;
            case CODE_GET_VOWIFI_DETECT_MODE /*1013*/:
                if (checkSignMatchOrIsSystemApp()) {
                    Slog.d(TAG, "HwWifiService  GET_VOWIFI_DETECT_MODE ");
                    data.enforceInterface(DESCRIPTOR);
                    _arg02 = null;
                    if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                        Slog.d(TAG, "getVoWifiDetectMode(): permissin deny");
                        return false;
                    }
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        _arg02 = ((HwWifiStateMachine) this.mWifiStateMachine).getVoWifiDetectMode();
                    }
                    reply.writeNoException();
                    if (_arg02 != null) {
                        reply.writeInt(1);
                        _arg02.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_GET_VOWIFI_DETECT_MODE SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                reply.writeInt(0);
                return false;
            case CODE_SET_VOWIFI_DETECT_PERIOD /*1014*/:
                if (checkSignMatchOrIsSystemApp()) {
                    Slog.d(TAG, "HwWifiService  SET_VOWIFI_DETECT_PERIOD ");
                    data.enforceInterface(DESCRIPTOR);
                    if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                        Slog.d(TAG, "setVoWifiDetectPeriod(): permissin deny");
                        return false;
                    }
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        this.mWifiStateMachine.setVoWifiDetectPeriod(data.readInt());
                    }
                    reply.writeNoException();
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_SET_VOWIFI_DETECT_PERIOD SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                return false;
            case CODE_GET_VOWIFI_DETECT_PERIOD /*1015*/:
                if (checkSignMatchOrIsSystemApp()) {
                    Slog.d(TAG, "HwWifiService  GET_VOWIFI_DETECT_PERIOD ");
                    data.enforceInterface(DESCRIPTOR);
                    if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                        Slog.d(TAG, "getVoWifiDetectPeriod(): permissin deny");
                        return false;
                    }
                    result3 = -1;
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        result3 = this.mWifiStateMachine.getVoWifiDetectPeriod();
                    }
                    reply.writeNoException();
                    reply.writeInt(result3);
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_GET_VOWIFI_DETECT_PERIOD SIGNATURE_NO_MATCH or not systemApp");
                reply.writeNoException();
                reply.writeInt(-1);
                return false;
            case CODE_IS_SUPPORT_VOWIFI_DETECT /*1016*/:
                if (checkSignMatchOrIsSystemApp()) {
                    Slog.d(TAG, "HwWifiService  IS_SUPPORT_VOWIFI_DETECT ");
                    data.enforceInterface(DESCRIPTOR);
                    if (this.mContext.checkCallingPermission(VOWIFI_WIFI_DETECT_PERMISSION) != 0) {
                        Slog.d(TAG, "isSupportVoWifiDetect(): permissin deny");
                        return false;
                    }
                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                        mHwWifiStateMachine = this.mWifiStateMachine;
                        if (wifiServiceUtils.getWifiStateMachineChannel(this) != null) {
                            enablen = mHwWifiStateMachine.syncGetSupportedVoWifiDetect(wifiServiceUtils.getWifiStateMachineChannel(this));
                        } else {
                            Slog.e(TAG, "Exception mWifiStateMachineChannel is not initialized");
                        }
                    }
                    reply.writeNoException();
                    reply.writeBooleanArray(new boolean[]{true});
                    return true;
                }
                Slog.e(TAG, "WifiService CODE_IS_SUPPORT_VOWIFI_DETECT SIGNATURE_NO_MATCH or not systemApp");
                return false;
            default:
                switch (code) {
                    case CODE_ENABLE_HILINK_HANDSHAKE /*2001*/:
                        if (checkSignMatchOrIsSystemApp()) {
                            data.enforceInterface(DESCRIPTOR);
                            enforceAccessPermission();
                            if (data.readInt() == 1) {
                                z = true;
                            }
                            this.mWifiStateMachine.enableHiLinkHandshake(z, data.readString());
                            return true;
                        }
                        Slog.e(TAG, "WifiService CODE_ENABLE_HILINK_HANDSHAKE SIGNATURE_NO_MATCH or not systemApp");
                        return false;
                    case CODE_GET_CONNECTION_RAW_PSK /*2002*/:
                        if (checkSignMatchOrIsSystemApp()) {
                            data.enforceInterface(DESCRIPTOR);
                            enforceAccessPermission();
                            result = null;
                            if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                                result = this.mWifiStateMachine.getConnectionRawPsk();
                            }
                            reply.writeNoException();
                            reply.writeString(result);
                            return true;
                        }
                        Slog.e(TAG, "WifiService CODE_GET_CONNECTION_RAW_PSK SIGNATURE_NO_MATCH or not systemApp");
                        reply.writeNoException();
                        reply.writeString(null);
                        return true;
                    default:
                        switch (code) {
                            case CODE_REQUEST_WIFI_ENABLE /*2004*/:
                                if (checkSignMatchOrIsSystemApp()) {
                                    data.enforceInterface(DESCRIPTOR);
                                    enforceAccessPermission();
                                    Slog.d(TAG, "HwWifiService REQUEST_WIFI_ENABLE");
                                    return requestWifiEnable(data);
                                }
                                Slog.e(TAG, "WifiService REQUEST_WIFI_ENABLE SIGNATURE_NO_MATCH or not systemApp");
                                return false;
                            case CODE_SET_WIFI_TXPOWER /*2005*/:
                                if (checkSignMatchOrIsSystemApp()) {
                                    data.enforceInterface(DESCRIPTOR);
                                    enforceAccessPermission();
                                    result3 = WifiInjector.getInstance().getWifiNative().setWifiTxPowerHw(data.readInt());
                                    reply.writeNoException();
                                    reply.writeInt(result3);
                                    return true;
                                }
                                Slog.e(TAG, "WifiService CODE_SET_WIFI_TXPOWER SIGNATURE_NO_MATCH or not systemApp");
                                reply.writeNoException();
                                reply.writeInt(-1);
                                return true;
                            case CODE_EXTEND_WIFI_SCAN_PERIOD_FOR_P2P /*2006*/:
                                if (checkSignMatchOrIsSystemApp()) {
                                    data.enforceInterface(DESCRIPTOR);
                                    enforceAccessPermission();
                                    Slog.d(TAG, "HwWifiService  EXTEND_WIFI_SCAN_PERIOD_FOR_P2P");
                                    return externWifiScanPeriodForP2p(data);
                                }
                                Slog.e(TAG, "WifiService EXTEND_WIFI_SCAN_PERIOD_FOR_P2P SIGNATURE_NO_MATCH or not systemApp");
                                return false;
                            case CODE_REQUEST_FRESH_WHITE_LIST /*2007*/:
                                if (checkSignMatchOrIsSystemApp()) {
                                    data.enforceInterface(DESCRIPTOR);
                                    enforceAccessPermission();
                                    result3 = data.readInt();
                                    List<String> packageWhiteList = new ArrayList();
                                    data.readStringList(packageWhiteList);
                                    if (result3 == 7) {
                                        HwQoEService qoeService_wifiSleep = HwQoEService.getInstance();
                                        if (qoeService_wifiSleep != null) {
                                            qoeService_wifiSleep.updateWifiSleepWhiteList(result3, packageWhiteList);
                                        }
                                    } else {
                                        BackgroundAppScanManager.getInstance().refreshPackageWhitelist(result3, packageWhiteList);
                                    }
                                    reply.writeNoException();
                                    return true;
                                }
                                Slog.e(TAG, "WifiService CODE_REQUEST_FRESH_WHITE_LIST SIGNATURE_NO_MATCH or not systemApp");
                                reply.writeNoException();
                                return false;
                            case CODE_GET_RSDB_SUPPORTED_MODE /*2008*/:
                                if (checkSignMatchOrIsSystemApp()) {
                                    data.enforceInterface(DESCRIPTOR);
                                    enforceAccessPermission();
                                    enablen = false;
                                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                                        mHwWifiStateMachine = this.mWifiStateMachine;
                                        if (wifiServiceUtils.getWifiStateMachineChannel(this) != null) {
                                            enablen = mHwWifiStateMachine.isRSDBSupported();
                                        } else {
                                            Slog.e(TAG, "Exception mWifiStateMachineChannel is not initialized");
                                        }
                                    }
                                    reply.writeNoException();
                                    reply.writeBooleanArray(new boolean[]{enablen});
                                    return true;
                                }
                                Slog.e(TAG, "WifiService CODE_GET_RSDB_SUPPORTED_MODE SIGNATURE_NO_MATCH or not systemApp");
                                reply.writeNoException();
                                reply.writeBooleanArray(new boolean[]{false});
                                return true;
                            default:
                                boolean result5;
                                int monitorType;
                                int period;
                                IHwQoECallback callback;
                                int i;
                                int monitorType2;
                                HwQoEService mHwQoEService;
                                String str3;
                                StringBuilder stringBuilder3;
                                switch (code) {
                                    case CODE_WIFI_QOE_START_MONITOR /*3001*/:
                                        if (checkSignMatchOrIsSystemApp()) {
                                            data.enforceInterface(DESCRIPTOR);
                                            enforceAccessPermission();
                                            result5 = false;
                                            monitorType = data.readInt();
                                            period = data.readInt();
                                            callback = IHwQoECallback.Stub.asInterface(data.readStrongBinder());
                                            HwQoEService mHwQoEService2 = HwQoEService.getInstance();
                                            if (mHwQoEService2 != null) {
                                                result5 = mHwQoEService2.registerHwQoEMonitor(monitorType, period, callback);
                                            }
                                            reply.writeNoException();
                                            if (result5) {
                                                i = 1;
                                            }
                                            reply.writeInt(i);
                                            return true;
                                        }
                                        Slog.e(TAG, "WifiService CODE_WIFI_QOE_START_MONITOR SIGNATURE_NO_MATCH or not systemApp");
                                        reply.writeNoException();
                                        reply.writeInt(0);
                                        return true;
                                    case CODE_WIFI_QOE_STOP_MONITOR /*3002*/:
                                        if (checkSignMatchOrIsSystemApp()) {
                                            data.enforceInterface(DESCRIPTOR);
                                            enforceAccessPermission();
                                            enablen = false;
                                            monitorType2 = data.readInt();
                                            mHwQoEService = HwQoEService.getInstance();
                                            if (mHwQoEService != null) {
                                                enablen = mHwQoEService.unRegisterHwQoEMonitor(monitorType2);
                                            }
                                            reply.writeNoException();
                                            if (enablen) {
                                                i = 1;
                                            }
                                            reply.writeInt(i);
                                            return true;
                                        }
                                        Slog.e(TAG, "WifiService CODE_WIFI_QOE_START_MONITOR SIGNATURE_NO_MATCH or not systemApp");
                                        reply.writeNoException();
                                        reply.writeInt(0);
                                        return true;
                                    case CODE_WIFI_QOE_EVALUATE /*3003*/:
                                        if (checkSignMatchOrIsSystemApp()) {
                                            data.enforceInterface(DESCRIPTOR);
                                            enforceAccessPermission();
                                            callback = IHwQoECallback.Stub.asInterface(data.readStrongBinder());
                                            result5 = false;
                                            mHwQoEService = HwQoEService.getInstance();
                                            if (mHwQoEService != null) {
                                                result5 = mHwQoEService.evaluateNetworkQuality(callback);
                                            }
                                            reply.writeNoException();
                                            if (result5) {
                                                i = 1;
                                            }
                                            reply.writeInt(i);
                                            return true;
                                        }
                                        Slog.e(TAG, "WifiService CODE_WIFI_QOE_EVALUATE SIGNATURE_NO_MATCH or not systemApp");
                                        reply.writeNoException();
                                        reply.writeInt(0);
                                        return true;
                                    case CODE_WIFI_QOE_UPDATE_STATUS /*3004*/:
                                        if (checkSignMatchOrIsSystemApp()) {
                                            data.enforceInterface(DESCRIPTOR);
                                            enforceAccessPermission();
                                            enablen = false;
                                            monitorType2 = data.readInt();
                                            mHwQoEService = HwQoEService.getInstance();
                                            if (mHwQoEService != null) {
                                                enablen = mHwQoEService.updateVOWIFIState(monitorType2);
                                            }
                                            reply.writeNoException();
                                            if (enablen) {
                                                i = 1;
                                            }
                                            reply.writeInt(i);
                                            return true;
                                        }
                                        Slog.e(TAG, "WifiService CODE_WIFI_QOE_UPDATE_STATUS SIGNATURE_NO_MATCH or not systemApp");
                                        reply.writeNoException();
                                        reply.writeInt(0);
                                        return true;
                                    case CODE_UPDATE_APP_RUNNING_STATUS /*3005*/:
                                        if (checkSignMatchOrIsSystemApp()) {
                                            data.enforceInterface(DESCRIPTOR);
                                            result3 = data.readInt();
                                            monitorType2 = data.readInt();
                                            i = data.readInt();
                                            monitorType = data.readInt();
                                            period = data.readInt();
                                            str3 = TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append(" updateAppRunningStatus  uid:");
                                            stringBuilder3.append(result3);
                                            stringBuilder3.append(", type:");
                                            stringBuilder3.append(monitorType2);
                                            stringBuilder3.append(",status:");
                                            stringBuilder3.append(i);
                                            stringBuilder3.append("scene: ");
                                            stringBuilder3.append(monitorType);
                                            Slog.d(str3, stringBuilder3.toString());
                                            reply.writeNoException();
                                            reply.writeInt(1);
                                            return true;
                                        }
                                        Slog.e(TAG, "WifiService CODE_UPDATE_APP_RUNNING_STATUS SIGNATURE_NO_MATCH or not systemApp");
                                        reply.writeNoException();
                                        reply.writeInt(0);
                                        return true;
                                    case CODE_UPDATE_APP_EXPERIENCE_STATUS /*3006*/:
                                        if (checkSignMatchOrIsSystemApp()) {
                                            data.enforceInterface(DESCRIPTOR);
                                            result3 = data.readInt();
                                            monitorType2 = data.readInt();
                                            long rtt_experience = data.readLong();
                                            i = data.readInt();
                                            str3 = TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("updateAppExperienceStatus  uid:");
                                            stringBuilder3.append(result3);
                                            stringBuilder3.append(", experience:");
                                            stringBuilder3.append(monitorType2);
                                            stringBuilder3.append(",rtt:");
                                            stringBuilder3.append(rtt_experience);
                                            Slog.d(str3, stringBuilder3.toString());
                                            reply.writeNoException();
                                            reply.writeInt(1);
                                            return true;
                                        }
                                        Slog.e(TAG, "WifiService CODE_UPDATE_APP_EXPERIENCE_STATUS SIGNATURE_NO_MATCH or not systemApp");
                                        reply.writeNoException();
                                        reply.writeInt(0);
                                        return true;
                                    case CODE_SET_WIFI_ANTSET /*3007*/:
                                        if (checkSignMatchOrIsSystemApp()) {
                                            data.enforceInterface(DESCRIPTOR);
                                            enforceAccessPermission();
                                            result = data.readString();
                                            monitorType2 = data.readInt();
                                            i = data.readInt();
                                            HwMSSHandler mssHandler = HwMSSHandler.getInstance();
                                            if (mssHandler != null) {
                                                mssHandler.setWifiAnt(result, monitorType2, i);
                                                Slog.d(TAG, "mssHandler hwSetWifiAnt");
                                            }
                                            reply.writeNoException();
                                            return true;
                                        }
                                        Slog.e(TAG, "WifiService CODE_SET_WIFI_ANTSET SIGNATURE_NO_MATCH or not systemApp");
                                        reply.writeNoException();
                                        return false;
                                    case CODE_IS_BG_LIMIT_ALLOWED /*3008*/:
                                        if (checkSignMatchOrIsSystemApp()) {
                                            data.enforceInterface(DESCRIPTOR);
                                            result3 = data.readInt();
                                            result5 = false;
                                            mHwQoEService = HwQoEService.getInstance();
                                            if (mHwQoEService != null) {
                                                result5 = mHwQoEService.isBgLimitAllowed(result3);
                                            }
                                            reply.writeNoException();
                                            reply.writeBooleanArray(new boolean[]{result5});
                                            return true;
                                        }
                                        Slog.e(TAG, "WifiService CODE_IS_BG_LIMIT_ALLOWED SIGNATURE_NO_MATCH or not systemApp");
                                        reply.writeNoException();
                                        reply.writeBooleanArray(new boolean[]{false});
                                        return true;
                                    default:
                                        StringBuilder stringBuilder4;
                                        Message msg;
                                        switch (code) {
                                            case CODE_DISABLE_RX_FILTER /*3021*/:
                                                data.enforceInterface(DESCRIPTOR);
                                                if (this.mContext.checkCallingPermission(ACCESS_WIFI_FILTER_PERMISSION) != 0) {
                                                    Slog.d(TAG, "disableWifiFilter: No ACCESS_FILTER permission");
                                                    return false;
                                                } else if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
                                                    result = TAG;
                                                    stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("you have no permission to call disableWifiFilter from uid:");
                                                    stringBuilder4.append(Binder.getCallingUid());
                                                    Slog.d(result, stringBuilder4.toString());
                                                    return false;
                                                } else {
                                                    result = TAG;
                                                    stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("call binder disableWifiFilter ");
                                                    stringBuilder4.append(getAppName(Binder.getCallingPid()));
                                                    Slog.i(result, stringBuilder4.toString());
                                                    result5 = disableWifiFilter(data.readStrongBinder());
                                                    reply.writeNoException();
                                                    reply.writeBooleanArray(new boolean[]{result5});
                                                    return true;
                                                }
                                            case CODE_ENABLE_RX_FILTER /*3022*/:
                                                data.enforceInterface(DESCRIPTOR);
                                                if (this.mContext.checkCallingPermission(ACCESS_WIFI_FILTER_PERMISSION) != 0) {
                                                    Slog.d(TAG, "enableWifiFilter: No ACCESS_FILTER permission");
                                                    return false;
                                                } else if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
                                                    result = TAG;
                                                    stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("you have no permission to call enableWifiFilter from uid:");
                                                    stringBuilder4.append(Binder.getCallingUid());
                                                    Slog.d(result, stringBuilder4.toString());
                                                    return false;
                                                } else {
                                                    result = TAG;
                                                    stringBuilder4 = new StringBuilder();
                                                    stringBuilder4.append("call binder enableWifiFilter ");
                                                    stringBuilder4.append(getAppName(Binder.getCallingPid()));
                                                    Slog.i(result, stringBuilder4.toString());
                                                    result5 = enableWifiFilter(data.readStrongBinder());
                                                    reply.writeNoException();
                                                    reply.writeBooleanArray(new boolean[]{result5});
                                                    return true;
                                                }
                                            case CODE_START_WIFI_KEEP_ALIVE /*3023*/:
                                                if (checkSignMatchOrIsSystemApp()) {
                                                    data.enforceInterface(DESCRIPTOR);
                                                    enforceAccessPermission();
                                                    msg = (Message) Message.CREATOR.createFromParcel(data);
                                                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                                                        ((HwWifiStateMachine) this.mWifiStateMachine).startPacketKeepalive(msg);
                                                    }
                                                    reply.writeNoException();
                                                    reply.writeInt(1);
                                                    return true;
                                                }
                                                Slog.e(TAG, "WifiService CODE_START_WIFI_KEEP_ALIVE SIGNATURE_NO_MATCH or not systemApp");
                                                reply.writeNoException();
                                                reply.writeInt(0);
                                                return true;
                                            case CODE_STOP_WIFI_KEEP_ALIVE /*3024*/:
                                                if (checkSignMatchOrIsSystemApp()) {
                                                    data.enforceInterface(DESCRIPTOR);
                                                    enforceAccessPermission();
                                                    msg = (Message) Message.CREATOR.createFromParcel(data);
                                                    if (this.mWifiStateMachine instanceof HwWifiStateMachine) {
                                                        this.mWifiStateMachine.stopPacketKeepalive(msg);
                                                    }
                                                    reply.writeNoException();
                                                    reply.writeInt(1);
                                                    return true;
                                                }
                                                Slog.e(TAG, "WifiService CODE_START_WIFI_KEEP_ALIVE SIGNATURE_NO_MATCH or not systemApp");
                                                reply.writeNoException();
                                                reply.writeInt(0);
                                                return true;
                                            default:
                                                switch (code) {
                                                    case CODE_RESTRICT_WIFI_SCAN /*4001*/:
                                                        if (checkSignMatchOrIsSystemApp()) {
                                                            data.enforceInterface(DESCRIPTOR);
                                                            enforceAccessPermission();
                                                            result2 = new ArrayList();
                                                            data.readStringList(result2);
                                                            if (result2.size() == 0) {
                                                                result2 = null;
                                                            }
                                                            if (data.readInt() != 0) {
                                                                z = true;
                                                            }
                                                            restrictWifiScan(result2, Boolean.valueOf(z));
                                                            return true;
                                                        }
                                                        Slog.e(TAG, "WifiService  CODE_RESTRICT_WIFI_SCAN SIGNATURE_NO_MATCH or not systemApp");
                                                        reply.writeNoException();
                                                        return false;
                                                    case CODE_UPDATE_WM_FREQ_LOC /*4002*/:
                                                        enablen = false;
                                                        if (checkSignMatchOrIsSystemApp()) {
                                                            data.enforceInterface(DESCRIPTOR);
                                                            enforceAccessPermission();
                                                            monitorType2 = data.readInt();
                                                            monitorType = data.readInt();
                                                            FrequentLocation mFrequentLocation = FrequentLocation.getInstance();
                                                            if (mFrequentLocation != null) {
                                                                enablen = mFrequentLocation.updateWaveMapping(monitorType2, monitorType);
                                                            }
                                                            reply.writeNoException();
                                                            if (enablen) {
                                                                i = 1;
                                                            }
                                                            reply.writeInt(i);
                                                            return true;
                                                        }
                                                        Slog.e(TAG, "WifiService  CODE_UPDATE_WM_FREQ_LOC SIGNATURE_NO_MATCH or not systemApp");
                                                        reply.writeNoException();
                                                        reply.writeInt(0);
                                                        return false;
                                                    default:
                                                        return super.onTransact(code, data, reply, flags);
                                                }
                                        }
                                }
                        }
                }
        }
    }

    private boolean disableWifiFilter(IBinder token) {
        if (token == null) {
            return false;
        }
        synchronized (this.mFilterSynchronizeLock) {
            if (findFilterIndex(token) >= 0) {
                Slog.d(TAG, "attempted to add filterlock when already holding one");
                return false;
            }
            HwFilterLock filterLock = new HwFilterLock(token);
            try {
                token.linkToDeath(filterLock, 0);
                this.mFilterLockList.add(filterLock);
                boolean updateWifiFilterState = updateWifiFilterState();
                return updateWifiFilterState;
            } catch (RemoteException e) {
                Slog.d(TAG, "Filter lock is already dead.");
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
                Slog.d(TAG, "cannot find wifi filter");
                return false;
            }
            HwFilterLock filterLock = (HwFilterLock) this.mFilterLockList.get(index);
            this.mFilterLockList.remove(index);
            filterLock.mToken.unlinkToDeath(filterLock, 0);
            boolean updateWifiFilterState = updateWifiFilterState();
            return updateWifiFilterState;
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0038, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean updateWifiFilterState() {
        synchronized (this.mFilterSynchronizeLock) {
            boolean enableWifiFilter;
            if (this.mFilterLockList.size() == 0) {
                if (this.isRxFilterDisabled) {
                    Slog.d(TAG, "enableWifiFilter");
                    this.isRxFilterDisabled = false;
                    enableWifiFilter = this.mWifiStateMachine.enableWifiFilter();
                    return enableWifiFilter;
                }
            } else if (!this.isRxFilterDisabled) {
                Slog.d(TAG, "disableWifiFilter");
                this.isRxFilterDisabled = true;
                enableWifiFilter = this.mWifiStateMachine.disableWifiFilter();
                return enableWifiFilter;
            }
        }
    }

    private void handleFilterLockDeath(HwFilterLock filterLock) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleFilterLockDeath: lock=");
        stringBuilder.append(Objects.hashCode(filterLock.mToken));
        Slog.d(str, stringBuilder.toString());
        synchronized (this.mFilterSynchronizeLock) {
            int index = findFilterIndex(filterLock.mToken);
            if (index < 0) {
                Slog.d(TAG, "cannot find wifi filter");
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
                if (((HwFilterLock) this.mFilterLockList.get(i)).mToken == token) {
                    return i;
                }
            }
            return -1;
        }
    }

    private boolean requestWifiEnable(Parcel data) {
        Slog.e(TAG, "HwWifiService REQUEST_WIFI_ENABLE is waived!!!");
        return false;
    }

    private boolean externWifiScanPeriodForP2p(Parcel data) {
        boolean bExtend = data.readInt() == 1;
        int iTimes = data.readInt();
        WifiConnectivityManager wifiConnectivityManager = wifiStateMachineUtils.getWifiConnectivityManager(this.mWifiStateMachine);
        if (wifiConnectivityManager == null || !(wifiConnectivityManager instanceof HwWifiConnectivityManager)) {
            Slog.d(TAG, "EXTEND_WIFI_SCAN_PERIOD_FOR_P2P: Exception wifiConnectivityManager is not initialized");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwWifiService  EXTEND_WIFI_SCAN_PERIOD_FOR_P2P: ");
        stringBuilder.append(bExtend);
        stringBuilder.append(", Times =");
        stringBuilder.append(iTimes);
        Slog.d(str, stringBuilder.toString());
        ((HwWifiConnectivityManager) wifiConnectivityManager).extendWifiScanPeriodForP2p(bExtend, iTimes);
        return true;
    }

    protected void handleForgetNetwork(final Message msg) {
        WifiConfiguration currentWifiConfiguration = this.mWifiStateMachine.getCurrentWifiConfiguration();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleForgetNetwork networkId = ");
        stringBuilder.append(msg.arg1);
        Log.d(str, stringBuilder.toString());
        if (this.mVowifiServiceOn && currentWifiConfiguration != null && msg.arg1 == currentWifiConfiguration.networkId) {
            Log.d(TAG, "handleForgetNetwork enter.");
            this.mMapconHandler.sendMessageDelayed(this.mMapconHandler.obtainMessage(0, msg), 5000);
            if (this.mMapconService != null) {
                try {
                    this.mMapconService.notifyWifiOff(new IMapconServiceCallback.Stub() {
                        public void onVoWifiCloseDone() {
                            Log.d(HwWifiService.TAG, "onVoWifiCloseDone: cancel delayed message and send FORGET_NETWORK");
                            HwWifiService.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                            if (HwWifiService.this.mMapconHandler.hasMessages(0)) {
                                HwWifiService.this.mMapconHandler.removeMessages(0);
                            }
                        }
                    });
                    return;
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception:", e);
                    this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                    if (this.mMapconHandler.hasMessages(0)) {
                        this.mMapconHandler.removeMessages(0);
                        return;
                    }
                    return;
                }
            }
            return;
        }
        int currentNetId = -1;
        if (currentWifiConfiguration != null) {
            currentNetId = currentWifiConfiguration.networkId;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("handleForgetNetwork current networkId = ");
        stringBuilder2.append(currentNetId);
        Log.d(str2, stringBuilder2.toString());
        this.mWifiStateMachine.sendMessage(Message.obtain(msg));
    }

    protected void handleAirplaneModeToggled() {
        WifiController controller = wifiServiceUtils.getWifiController(this);
        if (this.mVowifiServiceOn) {
            if (this.mSettingsStore.isAirplaneModeOn()) {
                Log.d(TAG, "handleAirplaneModeToggled, sendMessageDelayed");
                this.mMapconHandler.sendMessageDelayed(this.mMapconHandler.obtainMessage(1), 5000);
                if (this.mMapconService != null) {
                    try {
                        Log.d(TAG, "airplane mode enter, notify MapconService to shutdown");
                        this.mMapconService.notifyWifiOff(this.mAirPlaneCallback);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else if (controller != null) {
                controller.sendMessage(155657);
            }
        } else if (controller != null) {
            controller.sendMessage(155657);
        }
    }

    protected void setWifiEnabledAfterVoWifiOff(boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setWifiEnabled ");
        stringBuilder.append(enable);
        Log.d(str, stringBuilder.toString());
        if (WifiProCommonUtils.isWifiSelfCuring() || !this.mVowifiServiceOn || 3 != getWifiEnabledState() || this.mSettingsStore.isWifiToggleEnabled()) {
            WifiController controller = wifiServiceUtils.getWifiController(this);
            if (controller != null) {
                controller.sendMessage(155656);
                return;
            }
            return;
        }
        Log.d(TAG, "setWifiEnabled: sendMessageDelayed");
        this.mMapconHandler.sendMessageDelayed(this.mMapconHandler.obtainMessage(2), 5000);
        if (this.mMapconService != null) {
            try {
                Log.d(TAG, "setWifiEnabled enter, notify MapconService to shutdown");
                this.mMapconService.notifyWifiOff(this.mCallback);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("notifyWifiOff");
                stringBuilder2.append(e.toString());
                Log.d(str2, stringBuilder2.toString());
            }
        }
        while (this.mMapconHandler.hasMessages(2)) {
            try {
                Log.d(TAG, "setWifiEnabled ++++");
                Thread.sleep(5);
            } catch (InterruptedException e2) {
                Log.d(TAG, e2.toString());
            }
        }
    }

    protected void onReceiveEx(Context context, Intent intent) {
        String action = intent.getAction();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onReceive, action:");
        stringBuilder.append(action);
        Slog.d(str, stringBuilder.toString());
        if (Boolean.valueOf(SystemProperties.getBoolean("ro.vendor.config.hw_vowifi", false)).booleanValue() && ACTION_VOWIFI_STARTED.equals(action)) {
            Log.d(TAG, "received broadcast ACTION_VOWIFI_STARTED, try to bind MapconService");
            this.mContext.bindServiceAsUser(new Intent().setClassName("com.hisi.mapcon", "com.hisi.mapcon.MapconService"), this.conn, 1, UserHandle.OWNER);
        }
    }

    protected void registerForBroadcastsEx(IntentFilter intentFilter) {
        if (Boolean.valueOf(SystemProperties.getBoolean("ro.vendor.config.hw_vowifi", false)).booleanValue()) {
            intentFilter.addAction(ACTION_VOWIFI_STARTED);
        }
    }

    protected boolean mdmForPolicyForceOpenWifi(boolean showToast, boolean enable) {
        if (!HwDeviceManager.disallowOp(52) || enable) {
            return false;
        }
        if (showToast) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    Toast.makeText(HwWifiService.this.mContext, HwWifiService.this.mContext.getString(33686052), 0).show();
                }
            });
        }
        return true;
    }

    public void factoryReset(String packageName) {
        super.factoryReset(packageName);
        if (SystemProperties.getBoolean("ro.config.hw_preset_ap", false)) {
            boolean z = true;
            if (Global.getInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0) != 1) {
                z = false;
            }
            if (z) {
                Global.putInt(this.mContext.getContentResolver(), "wifi_scan_always_enabled", 0);
            }
            try {
                setWifiEnabled(this.mContext.getPackageName(), false);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setWifiEnabled false exception: ");
                stringBuilder.append(e.getMessage());
                Slog.e(str, stringBuilder.toString());
            }
            this.mNetworkResetHandler.postDelayed(new Runnable() {
                public void run() {
                    HwWifiService.this.removeWpaSupplicantConf();
                    try {
                        HwWifiService.this.setWifiEnabled(HwWifiService.this.mContext.getPackageName(), true);
                    } catch (RemoteException e) {
                        String str = HwWifiService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setWifiEnabled true exception: ");
                        stringBuilder.append(e.getMessage());
                        Slog.e(str, stringBuilder.toString());
                    }
                }
            }, 3000);
        }
    }

    protected boolean startQuickttffScan(String packageName) {
        if (!"com.huawei.lbs".equals(packageName) || Global.getInt(this.mContext.getContentResolver(), QTTFF_WIFI_SCAN_ENABLED, 0) != 1) {
            return false;
        }
        Slog.d(TAG, "quickttff request  2.4G wifi scan");
        if (this.lastScanResultsAvailableTime == 0 || this.mClock.getElapsedSinceBootMillis() - this.lastScanResultsAvailableTime >= 5000) {
            Slog.d(TAG, "Start 2.4G wifi scan.");
            if (!WifiInjector.getInstance().getWifiStateMachineHandler().runWithScissors(new -$$Lambda$HwWifiService$bNOLyr8oakjuxLhdkXG9XdmZD-4(this, packageName), 0)) {
                Log.w(TAG, "Failed to post runnable to start scan in startQuickttffScan");
                return false;
            }
        }
        Slog.d(TAG, "The scan results is fresh.");
        Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
        intent.addFlags(67108864);
        intent.putExtra("resultsUpdated", true);
        intent.setPackage("com.huawei.lbs");
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        return true;
    }

    protected boolean limitForegroundWifiScanRequest(String packageName, int uid) {
        long id = Binder.clearCallingIdentity();
        NetLocationStrategy wifiScanStrategy = null;
        try {
            boolean netLocationStrategy = HwServiceFactory.getNetLocationStrategy(packageName, uid, 1);
            wifiScanStrategy = netLocationStrategy;
            if (wifiScanStrategy == null) {
                Slog.e(TAG, "Get wifiScanStrategy from iAware is null.");
                return netLocationStrategy;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Get wifiScanStrategy from iAware, WifiScanStrategy = ");
            stringBuilder.append(wifiScanStrategy);
            Slog.d(str, stringBuilder.toString());
            if (wifiScanStrategy.getCycle() == -1) {
                return true;
            }
            if (wifiScanStrategy.getCycle() == 0) {
                return netLocationStrategy;
            }
            String str2;
            if (wifiScanStrategy.getCycle() <= 0) {
                Slog.e(TAG, "Invalid wifiScanStrategy.");
                return netLocationStrategy;
            } else if (this.lastScanResultsAvailableTime > wifiScanStrategy.getTimeStamp()) {
                long msSinceLastScan = this.mClock.getElapsedSinceBootMillis() - this.lastScanResultsAvailableTime;
                if (msSinceLastScan <= wifiScanStrategy.getCycle()) {
                    return true;
                }
                str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Last scan started ");
                stringBuilder2.append(msSinceLastScan);
                stringBuilder2.append("ms ago, cann't limit current scan request.");
                Slog.d(str2, stringBuilder2.toString());
                return netLocationStrategy;
            } else {
                str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Cann't limit current scan request, lastScanResultsAvailableTime = ");
                stringBuilder3.append(this.lastScanResultsAvailableTime);
                Slog.d(str2, stringBuilder3.toString());
                return netLocationStrategy;
            }
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    protected boolean limitWifiScanRequest(String packageName) {
        if (this.mWifiScanBlacklist.contains(packageName)) {
            return isGnssLocationFix();
        }
        return false;
    }

    private boolean isGnssLocationFix() {
        boolean z = true;
        if (Global.getInt(this.mContext.getContentResolver(), GNSS_LOCATION_FIX_STATUS, 0) != 1) {
            z = false;
        }
        boolean isGnssLocationFix = z;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isGnssLocationFix =");
        stringBuilder.append(isGnssLocationFix);
        Log.d(str, stringBuilder.toString());
        return isGnssLocationFix;
    }

    private void loadWifiScanBlacklist() {
        String[] blackList = this.mContext.getResources().getStringArray(33816590);
        this.mWifiScanBlacklist.clear();
        if (blackList != null) {
            this.mWifiScanBlacklist.addAll(Arrays.asList(blackList));
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mWifiScanBlacklist =");
            stringBuilder.append(this.mWifiScanBlacklist);
            Log.d(str, stringBuilder.toString());
        }
    }

    private void updateWifiScanblacklist() {
        this.mWifiScanBlacklist.clear();
        this.mWifiScanBlacklist.addAll(BackgroundAppScanManager.getInstance().getPackagBlackList());
    }

    protected boolean limitWifiScanInAbsoluteRest(String packageName) {
        boolean requestFromBackground = isRequestFromBackground(packageName);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mIsAbsoluteRest =");
        stringBuilder.append(this.mIsAbsoluteRest);
        stringBuilder.append(", mPluggedType =");
        stringBuilder.append(this.mPluggedType);
        stringBuilder.append(", mHasScanned =");
        stringBuilder.append(this.mHasScanned);
        stringBuilder.append(", requestFromBackground =");
        stringBuilder.append(requestFromBackground);
        Log.d(str, stringBuilder.toString());
        if (this.mIsAbsoluteRest && this.mPluggedType == 0 && requestFromBackground && this.mHasScanned) {
            return true;
        }
        this.mHasScanned = true;
        return false;
    }

    /* JADX WARNING: Missing block: B:22:0x004c, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isRequestFromBackground(String packageName) {
        boolean z = false;
        if (Binder.getCallingUid() == 1000 || Binder.getCallingUid() == 1010 || TextUtils.isEmpty(packageName) || PROCESS_BD.equals(packageName) || PROCESS_GD.equals(packageName)) {
            return false;
        }
        this.mAppOps.checkPackage(Binder.getCallingUid(), packageName);
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            if (this.mActivityManager.getPackageImportance(packageName) > 125) {
                z = true;
            }
            Binder.restoreCallingIdentity(callingIdentity);
            return z;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private boolean removeWpaSupplicantConf() {
        String str;
        StringBuilder stringBuilder;
        boolean ret = false;
        String str2;
        StringBuilder stringBuilder2;
        try {
            File conf = Environment.buildPath(Environment.getDataDirectory(), new String[]{"misc", "wifi", "wpa_supplicant.conf"});
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("conf path: ");
            stringBuilder2.append(conf.getPath());
            Slog.d(str2, stringBuilder2.toString());
            if (conf.exists()) {
                ret = conf.delete();
            }
            str = TAG;
            stringBuilder = new StringBuilder();
        } catch (SecurityException e) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("delete conf error : ");
            stringBuilder2.append(e.getMessage());
            Slog.e(str2, stringBuilder2.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
        } catch (Throwable th) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("delete conf result : ");
            stringBuilder.append(false);
            Slog.i(TAG, stringBuilder.toString());
        }
        stringBuilder.append("delete conf result : ");
        stringBuilder.append(ret);
        Slog.i(str, stringBuilder.toString());
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
        String pckName = "";
        StringBuilder stringBuilder;
        try {
            pckName = getAppName(Binder.getCallingPid());
            if (pckName == null) {
                Slog.e(TAG, "pckName is null");
                return false;
            }
            ApplicationInfo info = pm.getApplicationInfo(pckName, 0);
            if (info != null && (info.flags & 1) != 0) {
                return true;
            }
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("HwWifiService  checkSignMatchOrIsSystemAppMatch matchRe=");
            stringBuilder.append(matchResult);
            stringBuilder.append("pckName=");
            stringBuilder.append(pckName);
            Slog.d(str, stringBuilder.toString());
            return false;
        } catch (Exception ex) {
            String str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("isSystemApp not found app");
            stringBuilder.append(pckName);
            stringBuilder.append("exception=");
            stringBuilder.append(ex.toString());
            Slog.e(str2, stringBuilder.toString());
            return false;
        }
    }
}
