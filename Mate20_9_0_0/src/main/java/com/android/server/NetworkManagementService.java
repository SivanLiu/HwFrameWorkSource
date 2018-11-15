package com.android.server;

import android.app.ActivityManager;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetd;
import android.net.INetworkManagementEventObserver;
import android.net.ITetheringStatsProvider;
import android.net.InterfaceConfiguration;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.Network;
import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.util.NetdService;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkActivityListener;
import android.os.INetworkManagementService.Stub;
import android.os.PersistableBundle;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.Preconditions;
import com.android.server.HwServiceFactory.IHwNetworkManagermentService;
import com.android.server.NativeDaemonConnector.Command;
import com.android.server.Watchdog.Monitor;
import com.android.server.os.HwBootFail;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.voiceinteraction.DatabaseHelper.SoundModelContract;
import com.google.android.collect.Maps;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

public class NetworkManagementService extends Stub implements Monitor {
    static final int DAEMON_MSG_MOBILE_CONN_REAL_TIME_INFO = 1;
    private static final boolean DBG = Log.isLoggable(TAG, 3);
    public static final String LIMIT_GLOBAL_ALERT = "globalAlert";
    private static final int MAX_UID_RANGES_PER_COMMAND = 10;
    static final String NETD_SERVICE_NAME = "netd";
    private static final String NETD_TAG = "NetdConnector";
    public static final String PERMISSION_NETWORK = "NETWORK";
    public static final String PERMISSION_SYSTEM = "SYSTEM";
    static final String SOFT_AP_COMMAND = "softap";
    static final String SOFT_AP_COMMAND_SUCCESS = "Ok";
    private static final String TAG = "NetworkManagement";
    private static IHwNetworkManagermentService mNetworkMS;
    @GuardedBy("mQuotaLock")
    private HashMap<String, Long> mActiveAlerts;
    private HashMap<String, IdleTimerParams> mActiveIdleTimers;
    @GuardedBy("mQuotaLock")
    private HashMap<String, Long> mActiveQuotas;
    private volatile boolean mBandwidthControlEnabled;
    private IBatteryStats mBatteryStats;
    private CountDownLatch mConnectedSignal;
    private final NativeDaemonConnector mConnector;
    private final Context mContext;
    private final Handler mDaemonHandler;
    @GuardedBy("mQuotaLock")
    private volatile boolean mDataSaverMode;
    private final Handler mFgHandler;
    @GuardedBy("mRulesLock")
    final SparseBooleanArray mFirewallChainStates;
    private volatile boolean mFirewallEnabled;
    private final Object mIdleTimerLock;
    private int mLastPowerStateFromRadio;
    private int mLastPowerStateFromWifi;
    private int mLinkedStaCount;
    private boolean mMobileActivityFromRadio;
    protected INetd mNetdService;
    private boolean mNetworkActive;
    private final RemoteCallbackList<INetworkActivityListener> mNetworkActivityListeners;
    private final RemoteCallbackList<INetworkManagementEventObserver> mObservers;
    private final Object mQuotaLock;
    private final Object mRulesLock;
    private final SystemServices mServices;
    private final NetworkStatsFactory mStatsFactory;
    private volatile boolean mStrictEnabled;
    @GuardedBy("mTetheringStatsProviders")
    private final HashMap<ITetheringStatsProvider, String> mTetheringStatsProviders;
    private final Thread mThread;
    @GuardedBy("mRulesLock")
    private SparseBooleanArray mUidAllowOnMetered;
    @GuardedBy("mQuotaLock")
    private SparseIntArray mUidCleartextPolicy;
    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallDozableRules;
    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallPowerSaveRules;
    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallRules;
    @GuardedBy("mRulesLock")
    private SparseIntArray mUidFirewallStandbyRules;
    @GuardedBy("mRulesLock")
    private SparseBooleanArray mUidRejectOnMetered;
    private HwNativeDaemonConnector mhwNativeDaemonConnector;

    private static class IdleTimerParams {
        public int networkCount = 1;
        public final int timeout;
        public final int type;

        IdleTimerParams(int timeout, int type) {
            this.timeout = timeout;
            this.type = type;
        }
    }

    @VisibleForTesting
    class Injector {
        Injector() {
        }

        void setDataSaverMode(boolean dataSaverMode) {
            NetworkManagementService.this.mDataSaverMode = dataSaverMode;
        }

        void setFirewallChainState(int chain, boolean state) {
            NetworkManagementService.this.setFirewallChainState(chain, state);
        }

        void setFirewallRule(int chain, int uid, int rule) {
            synchronized (NetworkManagementService.this.mRulesLock) {
                NetworkManagementService.this.getUidFirewallRulesLR(chain).put(uid, rule);
            }
        }

        void setUidOnMeteredNetworkList(boolean blacklist, int uid, boolean enable) {
            synchronized (NetworkManagementService.this.mRulesLock) {
                if (blacklist) {
                    NetworkManagementService.this.mUidRejectOnMetered.put(uid, enable);
                } else {
                    NetworkManagementService.this.mUidAllowOnMetered.put(uid, enable);
                }
            }
        }

        void reset() {
            synchronized (NetworkManagementService.this.mRulesLock) {
                setDataSaverMode(false);
                for (int chain : new int[]{1, 2, 3}) {
                    setFirewallChainState(chain, false);
                    NetworkManagementService.this.getUidFirewallRulesLR(chain).clear();
                }
                NetworkManagementService.this.mUidAllowOnMetered.clear();
                NetworkManagementService.this.mUidRejectOnMetered.clear();
            }
        }
    }

    static class NetdResponseCode {
        public static final int ApLinkedStaListChangeHISI = 651;
        public static final int ApLinkedStaListChangeQCOM = 901;
        public static final int ApkDownloadUrlDetected = 810;
        public static final int BandwidthControl = 601;
        public static final int ClatdStatusResult = 223;
        public static final int DSCPChangeInfoReport = 662;
        public static final int DataSpeedSlowDetected = 660;
        public static final int DnsProxyQueryResult = 222;
        public static final int InterfaceAddressChange = 614;
        public static final int InterfaceChange = 600;
        public static final int InterfaceClassActivity = 613;
        public static final int InterfaceDnsServerInfo = 615;
        public static final int InterfaceGetCfgResult = 213;
        public static final int InterfaceListResult = 110;
        public static final int InterfaceRxCounterResult = 216;
        public static final int InterfaceTxCounterResult = 217;
        public static final int IpFwdStatusResult = 211;
        public static final int NbKsiReport = 671;
        public static final int NbVodReport = 670;
        public static final int QuotaCounterResult = 220;
        public static final int RouteChange = 616;
        public static final int SoftapStatusResult = 214;
        public static final int StrictCleartext = 617;
        public static final int TetherDnsFwdTgtListResult = 112;
        public static final int TetherInterfaceListResult = 111;
        public static final int TetherStatusResult = 210;
        public static final int TetheringStatsListResult = 114;
        public static final int TetheringStatsResult = 221;
        public static final int TtyListResult = 113;
        public static final int WebStatInfoReport = 661;

        NetdResponseCode() {
        }
    }

    private class NetdTetheringStatsProvider extends ITetheringStatsProvider.Stub {
        private NetdTetheringStatsProvider() {
        }

        /* synthetic */ NetdTetheringStatsProvider(NetworkManagementService x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x007b A:{Splitter: B:4:0x000e, ExcHandler: android.os.RemoteException (r0_3 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:17:0x007b, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:19:0x0084, code:
            throw new java.lang.IllegalStateException("problem parsing tethering stats: ", r0);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public NetworkStats getTetherStats(int how) {
            if (how != 1) {
                return new NetworkStats(SystemClock.elapsedRealtime(), 0);
            }
            try {
                PersistableBundle bundle = NetworkManagementService.this.mNetdService.tetherGetStats();
                NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), bundle.size());
                Entry entry = new Entry();
                for (String iface : bundle.keySet()) {
                    long[] statsArray = bundle.getLongArray(iface);
                    try {
                        entry.iface = iface;
                        entry.uid = -5;
                        entry.set = 0;
                        entry.tag = 0;
                        entry.rxBytes = statsArray[0];
                        entry.rxPackets = statsArray[1];
                        entry.txBytes = statsArray[2];
                        entry.txPackets = statsArray[3];
                        stats.combineValues(entry);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("invalid tethering stats for ");
                        stringBuilder.append(iface);
                        throw new IllegalStateException(stringBuilder.toString(), e);
                    }
                }
                return stats;
            } catch (Exception e2) {
            }
        }

        public void setInterfaceQuota(String iface, long quotaBytes) {
        }
    }

    @FunctionalInterface
    private interface NetworkManagementEventCallback {
        void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) throws RemoteException;
    }

    static class SystemServices {
        SystemServices() {
        }

        public IBinder getService(String name) {
            return ServiceManager.getService(name);
        }

        public void registerLocalService(NetworkManagementInternal nmi) {
            LocalServices.addService(NetworkManagementInternal.class, nmi);
        }

        public INetd getNetd() {
            return NetdService.get();
        }
    }

    @VisibleForTesting
    class LocalService extends NetworkManagementInternal {
        LocalService() {
        }

        public boolean isNetworkRestrictedForUid(int uid) {
            return NetworkManagementService.this.isNetworkRestrictedInternal(uid);
        }
    }

    private class NetdCallbackReceiver implements INativeDaemonConnectorCallbacks {
        private NetdCallbackReceiver() {
        }

        /* synthetic */ NetdCallbackReceiver(NetworkManagementService x0, AnonymousClass1 x1) {
            this();
        }

        public void onDaemonConnected() {
            Slog.i(NetworkManagementService.TAG, "onDaemonConnected()");
            if (NetworkManagementService.this.mConnectedSignal != null) {
                NetworkManagementService.this.mConnectedSignal.countDown();
                NetworkManagementService.this.mConnectedSignal = null;
                return;
            }
            NetworkManagementService.this.mFgHandler.post(new Runnable() {
                public void run() {
                    NetworkManagementService.this.connectNativeNetdService();
                    NetworkManagementService.this.prepareNativeDaemon();
                }
            });
        }

        public boolean onCheckHoldWakeLock(int code) {
            return code == NetdResponseCode.InterfaceClassActivity;
        }

        /* JADX WARNING: Removed duplicated region for block: B:79:0x0180  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean onEvent(int code, String raw, String[] cooked) {
            int i = code;
            String str = raw;
            String[] strArr = cooked;
            String errorMessage = String.format("Invalid event from daemon (%s)", new Object[]{str});
            if (i != 651) {
                if (i == 810) {
                    if (NetworkManagementService.mNetworkMS != null) {
                        NetworkManagementService.mNetworkMS.sendApkDownloadUrlBroadcast(strArr, str);
                    }
                    return true;
                } else if (i != 901) {
                    int i2 = 4;
                    switch (i) {
                        case 600:
                            if (strArr.length < 4 || !strArr[1].equals("Iface")) {
                                throw new IllegalStateException(errorMessage);
                            } else if (strArr[2].equals("added")) {
                                NetworkManagementService.this.notifyInterfaceAdded(strArr[3]);
                                return true;
                            } else if (strArr[2].equals("removed")) {
                                NetworkManagementService.this.notifyInterfaceRemoved(strArr[3]);
                                return true;
                            } else if (strArr[2].equals("changed") && strArr.length == 5) {
                                NetworkManagementService.this.notifyInterfaceStatusChanged(strArr[3], strArr[4].equals("up"));
                                return true;
                            } else if (strArr[2].equals("linkstate") && strArr.length == 5) {
                                NetworkManagementService.this.notifyInterfaceLinkStateChanged(strArr[3], strArr[4].equals("up"));
                                return true;
                            } else {
                                throw new IllegalStateException(errorMessage);
                            }
                        case NetdResponseCode.BandwidthControl /*601*/:
                            if (strArr.length < 5 || !strArr[1].equals("limit")) {
                                throw new IllegalStateException(errorMessage);
                            } else if (strArr[2].equals("alert")) {
                                NetworkManagementService.this.notifyLimitReached(strArr[3], strArr[4]);
                                return true;
                            } else {
                                throw new IllegalStateException(errorMessage);
                            }
                        default:
                            String iface;
                            switch (i) {
                                case NetdResponseCode.InterfaceClassActivity /*613*/:
                                    if (strArr.length < 4 || !strArr[1].equals("IfaceClass")) {
                                        throw new IllegalStateException(errorMessage);
                                    }
                                    long timestampNanos = 0;
                                    int processUid = -1;
                                    if (strArr.length >= 5) {
                                        try {
                                            timestampNanos = Long.parseLong(strArr[4]);
                                            if (strArr.length == 6) {
                                                processUid = Integer.parseInt(strArr[5]);
                                            }
                                        } catch (NumberFormatException e) {
                                        }
                                    } else {
                                        timestampNanos = SystemClock.elapsedRealtimeNanos();
                                    }
                                    NetworkManagementService.this.notifyInterfaceClassActivity(Integer.parseInt(strArr[3]), strArr[2].equals("active") ? 3 : 1, timestampNanos, processUid, false);
                                    return true;
                                case NetdResponseCode.InterfaceAddressChange /*614*/:
                                    if (strArr.length < 7 || !strArr[1].equals("Address")) {
                                        throw new IllegalStateException(errorMessage);
                                    }
                                    iface = strArr[4];
                                    try {
                                        LinkAddress address = new LinkAddress(strArr[3], Integer.parseInt(strArr[5]), Integer.parseInt(strArr[6]));
                                        if (strArr[2].equals("updated")) {
                                            NetworkManagementService.this.notifyAddressUpdated(iface, address);
                                        } else {
                                            NetworkManagementService.this.notifyAddressRemoved(iface, address);
                                        }
                                        return true;
                                    } catch (NumberFormatException e2) {
                                        throw new IllegalStateException(errorMessage, e2);
                                    } catch (IllegalArgumentException e3) {
                                        throw new IllegalStateException(errorMessage, e3);
                                    }
                                case NetdResponseCode.InterfaceDnsServerInfo /*615*/:
                                    if (strArr.length == 6 && strArr[1].equals("DnsInfo") && strArr[2].equals("servers")) {
                                        try {
                                            NetworkManagementService.this.notifyInterfaceDnsServerInfo(strArr[3], Long.parseLong(strArr[4]), strArr[5].split(","));
                                        } catch (NumberFormatException e4) {
                                            throw new IllegalStateException(errorMessage);
                                        }
                                    }
                                    return true;
                                case NetdResponseCode.RouteChange /*616*/:
                                    if (!strArr[1].equals("Route") || strArr.length < 6) {
                                        throw new IllegalStateException(errorMessage);
                                    }
                                    boolean valid = true;
                                    String dev = null;
                                    iface = null;
                                    while (true) {
                                        int i3 = i2;
                                        if (i3 + 1 < strArr.length && valid) {
                                            boolean valid2;
                                            if (strArr[i3].equals("dev")) {
                                                if (dev == null) {
                                                    dev = strArr[i3 + 1];
                                                    i2 = i3 + 2;
                                                } else {
                                                    valid2 = false;
                                                }
                                            } else if (!strArr[i3].equals("via")) {
                                                valid2 = false;
                                            } else if (iface == null) {
                                                iface = strArr[i3 + 1];
                                                i2 = i3 + 2;
                                            } else {
                                                valid2 = false;
                                            }
                                            valid = valid2;
                                            i2 = i3 + 2;
                                        } else if (valid) {
                                            InetAddress gateway = null;
                                            if (iface != null) {
                                                try {
                                                    gateway = InetAddress.parseNumericAddress(iface);
                                                } catch (IllegalArgumentException e5) {
                                                }
                                            }
                                            NetworkManagementService.this.notifyRouteChange(strArr[2], new RouteInfo(new IpPrefix(strArr[3]), gateway, dev));
                                            return true;
                                        }
                                    }
                                    if (valid) {
                                    }
                                    throw new IllegalStateException(errorMessage);
                                case NetdResponseCode.StrictCleartext /*617*/:
                                    try {
                                        ActivityManager.getService().notifyCleartextNetwork(Integer.parseInt(strArr[1]), HexDump.hexStringToByteArray(strArr[2]));
                                        break;
                                    } catch (RemoteException e6) {
                                        break;
                                    }
                                default:
                                    switch (i) {
                                        case NetdResponseCode.DataSpeedSlowDetected /*660*/:
                                            if (NetworkManagementService.mNetworkMS != null) {
                                                NetworkManagementService.mNetworkMS.sendDataSpeedSlowMessage(strArr, str);
                                            }
                                            return true;
                                        case NetdResponseCode.WebStatInfoReport /*661*/:
                                            if (NetworkManagementService.mNetworkMS != null) {
                                                NetworkManagementService.mNetworkMS.sendWebStatMessage(strArr, str);
                                            }
                                            return true;
                                        case NetdResponseCode.DSCPChangeInfoReport /*662*/:
                                            if (NetworkManagementService.mNetworkMS != null) {
                                                NetworkManagementService.mNetworkMS.sendDSCPChangeMessage(strArr, str);
                                            }
                                            return true;
                                        default:
                                            switch (i) {
                                                case NetdResponseCode.NbVodReport /*670*/:
                                                    Slog.e(NetworkManagementService.TAG, "NetdCallbackReceiver.onEvent NbVodReport");
                                                    if (strArr.length < 12 || !strArr[1].equals("vod")) {
                                                        throw new IllegalStateException(errorMessage);
                                                    }
                                                    try {
                                                        if (NetworkManagementService.mNetworkMS != null) {
                                                            NetworkManagementService.mNetworkMS.reportVodParams(Integer.parseInt(strArr[2]), Integer.parseInt(strArr[3]), Integer.parseInt(strArr[4]), Integer.parseInt(strArr[5]), Integer.parseInt(strArr[6]), Integer.parseInt(strArr[7]), Integer.parseInt(strArr[8]), Integer.parseInt(strArr[9]), Integer.parseInt(strArr[10]), Integer.parseInt(strArr[11]));
                                                        }
                                                        return true;
                                                    } catch (NumberFormatException e22) {
                                                        throw new IllegalStateException(errorMessage, e22);
                                                    }
                                                case NetdResponseCode.NbKsiReport /*671*/:
                                                    Slog.e(NetworkManagementService.TAG, "NetdCallbackReceiver.onEvent NbKsiReport");
                                                    if (strArr.length < 6 || !strArr[1].equals("ksi")) {
                                                        throw new IllegalStateException(errorMessage);
                                                    }
                                                    try {
                                                        if (NetworkManagementService.mNetworkMS != null) {
                                                            NetworkManagementService.mNetworkMS.reportKsiParams(Integer.parseInt(strArr[2]), Integer.parseInt(strArr[3]), Integer.parseInt(strArr[4]), Integer.parseInt(strArr[5]));
                                                        }
                                                        return true;
                                                    } catch (NumberFormatException e222) {
                                                        throw new IllegalStateException(errorMessage, e222);
                                                    }
                                            }
                                            break;
                                    }
                            }
                            return false;
                    }
                }
            }
            if (NetworkManagementService.mNetworkMS != null) {
                NetworkManagementService.mNetworkMS.handleApLinkedStaListChange(str, strArr);
            }
            return true;
        }
    }

    protected NetworkManagementService(Context context, String socket, SystemServices services) {
        this.mConnectedSignal = new CountDownLatch(1);
        this.mObservers = new RemoteCallbackList();
        this.mStatsFactory = new NetworkStatsFactory();
        this.mTetheringStatsProviders = Maps.newHashMap();
        this.mQuotaLock = new Object();
        this.mRulesLock = new Object();
        this.mActiveQuotas = Maps.newHashMap();
        this.mActiveAlerts = Maps.newHashMap();
        this.mUidRejectOnMetered = new SparseBooleanArray();
        this.mUidAllowOnMetered = new SparseBooleanArray();
        this.mUidCleartextPolicy = new SparseIntArray();
        this.mUidFirewallRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mIdleTimerLock = new Object();
        this.mActiveIdleTimers = Maps.newHashMap();
        this.mLinkedStaCount = 0;
        this.mMobileActivityFromRadio = false;
        this.mLastPowerStateFromRadio = 1;
        this.mLastPowerStateFromWifi = 1;
        this.mNetworkActivityListeners = new RemoteCallbackList();
        this.mContext = context;
        this.mServices = services;
        this.mFgHandler = new Handler(FgThread.get().getLooper());
        this.mhwNativeDaemonConnector = HwServiceFactory.getHwNativeDaemonConnector();
        this.mhwNativeDaemonConnector.setContext(this.mContext);
        this.mConnector = new NativeDaemonConnector(new NetdCallbackReceiver(this, null), socket, 10, NETD_TAG, 160, null, FgThread.get().getLooper());
        this.mConnector.setDebug(true);
        this.mThread = new Thread(this.mConnector, NETD_TAG);
        this.mDaemonHandler = new Handler(FgThread.get().getLooper());
        Watchdog.getInstance().addMonitor(this);
        this.mServices.registerLocalService(new LocalService());
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.put(new NetdTetheringStatsProvider(this, null), NETD_SERVICE_NAME);
        }
    }

    @VisibleForTesting
    NetworkManagementService() {
        this.mConnectedSignal = new CountDownLatch(1);
        this.mObservers = new RemoteCallbackList();
        this.mStatsFactory = new NetworkStatsFactory();
        this.mTetheringStatsProviders = Maps.newHashMap();
        this.mQuotaLock = new Object();
        this.mRulesLock = new Object();
        this.mActiveQuotas = Maps.newHashMap();
        this.mActiveAlerts = Maps.newHashMap();
        this.mUidRejectOnMetered = new SparseBooleanArray();
        this.mUidAllowOnMetered = new SparseBooleanArray();
        this.mUidCleartextPolicy = new SparseIntArray();
        this.mUidFirewallRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mIdleTimerLock = new Object();
        this.mActiveIdleTimers = Maps.newHashMap();
        this.mLinkedStaCount = 0;
        this.mMobileActivityFromRadio = false;
        this.mLastPowerStateFromRadio = 1;
        this.mLastPowerStateFromWifi = 1;
        this.mNetworkActivityListeners = new RemoteCallbackList();
        this.mConnector = null;
        this.mContext = null;
        this.mDaemonHandler = null;
        this.mFgHandler = null;
        this.mThread = null;
        this.mServices = null;
    }

    static NetworkManagementService create(Context context, String socket, SystemServices services) throws InterruptedException {
        NetworkManagementService service;
        mNetworkMS = HwServiceFactory.getHwNetworkManagermentService();
        if (mNetworkMS != null) {
            service = mNetworkMS.getInstance(context, socket, services);
            mNetworkMS.setNativeDaemonConnector(service, service.mConnector);
        } else {
            service = new NetworkManagementService(context, socket, services);
        }
        CountDownLatch connectedSignal = service.mConnectedSignal;
        if (DBG) {
            Slog.d(TAG, "Creating NetworkManagementService");
        }
        service.mThread.start();
        if (DBG) {
            Slog.d(TAG, "Awaiting socket connection");
        }
        connectedSignal.await();
        if (DBG) {
            Slog.d(TAG, "Connected");
        }
        if (DBG) {
            Slog.d(TAG, "Connecting native netd service");
        }
        service.connectNativeNetdService();
        if (DBG) {
            Slog.d(TAG, "Connected");
        }
        return service;
    }

    public static NetworkManagementService create(Context context) throws InterruptedException {
        return create(context, NETD_SERVICE_NAME, new SystemServices());
    }

    public void systemReady() {
        if (DBG) {
            long start = System.currentTimeMillis();
            prepareNativeDaemon();
            long delta = System.currentTimeMillis() - start;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Prepared in ");
            stringBuilder.append(delta);
            stringBuilder.append("ms");
            Slog.d(str, stringBuilder.toString());
            return;
        }
        prepareNativeDaemon();
    }

    private IBatteryStats getBatteryStats() {
        synchronized (this) {
            IBatteryStats iBatteryStats;
            if (this.mBatteryStats != null) {
                iBatteryStats = this.mBatteryStats;
                return iBatteryStats;
            }
            this.mBatteryStats = IBatteryStats.Stub.asInterface(this.mServices.getService("batterystats"));
            iBatteryStats = this.mBatteryStats;
            return iBatteryStats;
        }
    }

    public void registerObserver(INetworkManagementEventObserver observer) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.NETWORKMANAGEMENT_REGISTEROBSERVER);
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerObserver: pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid=");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", observer ");
        stringBuilder.append(observer);
        Slog.d(str, stringBuilder.toString());
        this.mObservers.register(observer, Integer.valueOf(Binder.getCallingPid()));
    }

    public void unregisterObserver(INetworkManagementEventObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregisterObserver: pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid=");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", observer ");
        stringBuilder.append(observer);
        Slog.d(str, stringBuilder.toString());
        this.mObservers.unregister(observer);
    }

    /* JADX WARNING: Removed duplicated region for block: B:7:0x001c A:{Splitter: B:2:0x0009, ExcHandler: android.os.RemoteException (e android.os.RemoteException)} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void invokeForAllObservers(NetworkManagementEventCallback eventCallback) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                eventCallback.sendCallback((INetworkManagementEventObserver) this.mObservers.getBroadcastItem(i));
            } catch (RemoteException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
            }
        }
        this.mObservers.finishBroadcast();
    }

    private void notifyInterfaceStatusChanged(String iface, boolean up) {
        invokeForAllObservers(new -$$Lambda$NetworkManagementService$fl14NirBlFUd6eJkGcL0QWd5-w0(iface, up));
    }

    private void notifyInterfaceLinkStateChanged(String iface, boolean up) {
        invokeForAllObservers(new -$$Lambda$NetworkManagementService$_L953cbquVj0BMBP1MZlSTm0Umg(iface, up));
    }

    private void notifyInterfaceAdded(String iface) {
        invokeForAllObservers(new -$$Lambda$NetworkManagementService$vX8dVVYxxv3YT9jQuN34bgGgRa8(iface));
    }

    private void notifyInterfaceRemoved(String iface) {
        this.mActiveAlerts.remove(iface);
        this.mActiveQuotas.remove(iface);
        invokeForAllObservers(new -$$Lambda$NetworkManagementService$FsR_UD5xfj4hgrwGdX74wq881Bk(iface));
    }

    private void notifyLimitReached(String limitName, String iface) {
        invokeForAllObservers(new -$$Lambda$NetworkManagementService$xer7k2RLU4mODjrkZqaX89S9gD8(limitName, iface));
    }

    private void notifyInterfaceClassActivity(int type, int powerState, long tsNanos, int uid, boolean fromRadio) {
        boolean isMobile = ConnectivityManager.isNetworkTypeMobile(type);
        boolean isActive = true;
        if (isMobile) {
            if (fromRadio) {
                this.mMobileActivityFromRadio = true;
            } else if (this.mMobileActivityFromRadio) {
                powerState = this.mLastPowerStateFromRadio;
            }
            if (this.mLastPowerStateFromRadio != powerState) {
                this.mLastPowerStateFromRadio = powerState;
                try {
                    getBatteryStats().noteMobileRadioPowerState(powerState, tsNanos, uid);
                } catch (RemoteException e) {
                }
            }
        }
        if (ConnectivityManager.isNetworkTypeWifi(type) && this.mLastPowerStateFromWifi != powerState) {
            this.mLastPowerStateFromWifi = powerState;
            try {
                getBatteryStats().noteWifiRadioPowerState(powerState, tsNanos, uid);
            } catch (RemoteException e2) {
            }
        }
        if (!(powerState == 2 || powerState == 3)) {
            isActive = false;
        }
        if (!(isMobile && !fromRadio && this.mMobileActivityFromRadio)) {
            invokeForAllObservers(new -$$Lambda$NetworkManagementService$D43p3Tqq7B3qaMs9AGb_3j0KZd0(type, isActive, tsNanos));
        }
        boolean report = false;
        synchronized (this.mIdleTimerLock) {
            if (this.mActiveIdleTimers.isEmpty()) {
                isActive = true;
            }
            if (this.mNetworkActive != isActive) {
                this.mNetworkActive = isActive;
                report = isActive;
            }
        }
        if (report) {
            reportNetworkActive();
        }
    }

    public void registerTetheringStatsProvider(ITetheringStatsProvider provider, String name) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        Preconditions.checkNotNull(provider);
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.put(provider, name);
        }
    }

    public void unregisterTetheringStatsProvider(ITetheringStatsProvider provider) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.remove(provider);
        }
    }

    public void tetherLimitReached(ITetheringStatsProvider provider) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        synchronized (this.mTetheringStatsProviders) {
            if (this.mTetheringStatsProviders.containsKey(provider)) {
                notifyLimitReached(LIMIT_GLOBAL_ALERT, null);
                return;
            }
        }
    }

    private void syncFirewallChainLocked(int chain, String name) {
        SparseIntArray rules;
        synchronized (this.mRulesLock) {
            SparseIntArray uidFirewallRules = getUidFirewallRulesLR(chain);
            rules = uidFirewallRules.clone();
            uidFirewallRules.clear();
        }
        if (rules.size() > 0) {
            if (DBG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Pushing ");
                stringBuilder.append(rules.size());
                stringBuilder.append(" active firewall ");
                stringBuilder.append(name);
                stringBuilder.append("UID rules");
                Slog.d(str, stringBuilder.toString());
            }
            for (int i = 0; i < rules.size(); i++) {
                setFirewallUidRuleLocked(chain, rules.keyAt(i), rules.valueAt(i));
            }
        }
    }

    private void connectNativeNetdService() {
        this.mNetdService = this.mServices.getNetd();
    }

    private void prepareNativeDaemon() {
        int i = 0;
        this.mBandwidthControlEnabled = false;
        boolean hasKernelSupport = new File("/proc/net/xt_qtaguid/ctrl").exists();
        synchronized (this.mQuotaLock) {
            String str;
            StringBuilder stringBuilder;
            HashMap<String, Long> activeQuotas;
            int i2;
            int i3;
            if (hasKernelSupport) {
                Slog.d(TAG, "enabling bandwidth control");
                try {
                    this.mConnector.execute("bandwidth", "enable");
                    this.mBandwidthControlEnabled = true;
                } catch (NativeDaemonConnectorException e) {
                    Log.wtf(TAG, "problem enabling bandwidth controls", e);
                }
            } else {
                Slog.i(TAG, "not enabling bandwidth control");
            }
            SystemProperties.set("net.qtaguid_enabled", this.mBandwidthControlEnabled ? "1" : "0");
            try {
                this.mConnector.execute("strict", "enable");
                this.mStrictEnabled = true;
            } catch (NativeDaemonConnectorException e2) {
                Log.wtf(TAG, "Failed strict enable", e2);
            }
            setDataSaverModeEnabled(this.mDataSaverMode);
            int size = this.mActiveQuotas.size();
            if (size > 0) {
                if (DBG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Pushing ");
                    stringBuilder.append(size);
                    stringBuilder.append(" active quota rules");
                    Slog.d(str, stringBuilder.toString());
                }
                activeQuotas = this.mActiveQuotas;
                this.mActiveQuotas = Maps.newHashMap();
                for (Map.Entry<String, Long> entry : activeQuotas.entrySet()) {
                    setInterfaceQuota((String) entry.getKey(), ((Long) entry.getValue()).longValue());
                }
            }
            size = this.mActiveAlerts.size();
            if (size > 0) {
                if (DBG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Pushing ");
                    stringBuilder.append(size);
                    stringBuilder.append(" active alert rules");
                    Slog.d(str, stringBuilder.toString());
                }
                activeQuotas = this.mActiveAlerts;
                this.mActiveAlerts = Maps.newHashMap();
                for (Map.Entry<String, Long> entry2 : activeQuotas.entrySet()) {
                    setInterfaceAlert((String) entry2.getKey(), ((Long) entry2.getValue()).longValue());
                }
            }
            SparseBooleanArray uidRejectOnQuota = null;
            SparseBooleanArray uidAcceptOnQuota = null;
            synchronized (this.mRulesLock) {
                String str2;
                StringBuilder stringBuilder2;
                size = this.mUidRejectOnMetered.size();
                if (size > 0) {
                    if (DBG) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Pushing ");
                        stringBuilder2.append(size);
                        stringBuilder2.append(" UIDs to metered blacklist rules");
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    uidRejectOnQuota = this.mUidRejectOnMetered;
                    this.mUidRejectOnMetered = new SparseBooleanArray();
                }
                size = this.mUidAllowOnMetered.size();
                if (size > 0) {
                    if (DBG) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Pushing ");
                        stringBuilder2.append(size);
                        stringBuilder2.append(" UIDs to metered whitelist rules");
                        Slog.d(str2, stringBuilder2.toString());
                    }
                    uidAcceptOnQuota = this.mUidAllowOnMetered;
                    this.mUidAllowOnMetered = new SparseBooleanArray();
                }
            }
            if (uidRejectOnQuota != null) {
                for (i2 = 0; i2 < uidRejectOnQuota.size(); i2++) {
                    setUidMeteredNetworkBlacklist(uidRejectOnQuota.keyAt(i2), uidRejectOnQuota.valueAt(i2));
                }
            }
            if (uidAcceptOnQuota != null) {
                for (i2 = 0; i2 < uidAcceptOnQuota.size(); i2++) {
                    setUidMeteredNetworkWhitelist(uidAcceptOnQuota.keyAt(i2), uidAcceptOnQuota.valueAt(i2));
                }
            }
            size = this.mUidCleartextPolicy.size();
            if (size > 0) {
                if (DBG) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Pushing ");
                    stringBuilder3.append(size);
                    stringBuilder3.append(" active UID cleartext policies");
                    Slog.d(str3, stringBuilder3.toString());
                }
                SparseIntArray local = this.mUidCleartextPolicy;
                this.mUidCleartextPolicy = new SparseIntArray();
                for (i3 = 0; i3 < local.size(); i3++) {
                    setUidCleartextNetworkPolicy(local.keyAt(i3), local.valueAt(i3));
                }
            }
            try {
                setFirewallEnabled(this.mFirewallEnabled);
            } catch (IllegalStateException e3) {
                Log.wtf(TAG, "Failed firewall enable", e3);
            }
            syncFirewallChainLocked(0, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            syncFirewallChainLocked(2, "standby ");
            syncFirewallChainLocked(1, "dozable ");
            syncFirewallChainLocked(3, "powersave ");
            int[] chains = new int[]{2, 1, 3};
            i3 = chains.length;
            while (i < i3) {
                int chain = chains[i];
                if (getFirewallChainState(chain)) {
                    setFirewallChainEnabled(chain, true);
                }
                i++;
            }
        }
        if (this.mBandwidthControlEnabled) {
            try {
                getBatteryStats().noteNetworkStatsEnabled();
            } catch (RemoteException e4) {
            }
        }
    }

    private void notifyAddressUpdated(String iface, LinkAddress address) {
        invokeForAllObservers(new -$$Lambda$NetworkManagementService$V2aaK7-IK-mKPVvhONFoyFWi4zM(iface, address));
    }

    private void notifyAddressRemoved(String iface, LinkAddress address) {
        invokeForAllObservers(new -$$Lambda$NetworkManagementService$iDseO-DhVR7T2LR6qxVJCC-3wfI(iface, address));
    }

    private void notifyInterfaceDnsServerInfo(String iface, long lifetime, String[] addresses) {
        invokeForAllObservers(new -$$Lambda$NetworkManagementService$8J1LB_n8vMkXxx2KS06P_lQCw6w(iface, lifetime, addresses));
    }

    private void notifyRouteChange(String action, RouteInfo route) {
        if (action.equals("updated")) {
            invokeForAllObservers(new -$$Lambda$NetworkManagementService$glaDh2pKbTpJLW8cwpYGiYd-sCA(route));
        } else {
            invokeForAllObservers(new -$$Lambda$NetworkManagementService$VhSl9D6THA_3jE0unleMmkHavJ0(route));
        }
    }

    public INetd getNetdService() throws RemoteException {
        CountDownLatch connectedSignal = this.mConnectedSignal;
        if (connectedSignal != null) {
            try {
                connectedSignal.await();
            } catch (InterruptedException e) {
            }
        }
        return this.mNetdService;
    }

    public String[] listInterfaces() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("interface", "list"), 110);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addUpstreamV6Interface(String iface) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", "NetworkManagementService");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addUpstreamInterface(");
        stringBuilder.append(iface);
        stringBuilder.append(")");
        Slog.d(str, stringBuilder.toString());
        try {
            Command cmd = new Command("tether", "interface", "add_upstream");
            cmd.appendArg(iface);
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw new RemoteException("Cannot add upstream interface");
        }
    }

    public void removeUpstreamV6Interface(String iface) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", "NetworkManagementService");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeUpstreamInterface(");
        stringBuilder.append(iface);
        stringBuilder.append(")");
        Slog.d(str, stringBuilder.toString());
        try {
            Command cmd = new Command("tether", "interface", "remove_upstream");
            cmd.appendArg(iface);
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw new RemoteException("Cannot remove upstream interface");
        }
    }

    public InterfaceConfiguration getInterfaceConfig(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            r2 = new Object[2];
            int prefixLength = 0;
            r2[0] = "getcfg";
            r2[1] = iface;
            NativeDaemonEvent event = this.mConnector.execute("interface", r2);
            event.checkCode(NetdResponseCode.InterfaceGetCfgResult);
            StringTokenizer st = new StringTokenizer(event.getMessage());
            try {
                InterfaceConfiguration cfg = new InterfaceConfiguration();
                cfg.setHardwareAddress(st.nextToken(" "));
                InetAddress addr = null;
                try {
                    addr = NetworkUtils.numericToInetAddress(st.nextToken());
                } catch (IllegalArgumentException iae) {
                    Slog.e(TAG, "Failed to parse ipaddr", iae);
                }
                try {
                    prefixLength = Integer.parseInt(st.nextToken());
                } catch (NumberFormatException nfe) {
                    Slog.e(TAG, "Failed to parse prefixLength", nfe);
                }
                cfg.setLinkAddress(new LinkAddress(addr, prefixLength));
                while (st.hasMoreTokens()) {
                    cfg.setFlag(st.nextToken());
                }
                return cfg;
            } catch (NoSuchElementException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid response from daemon: ");
                stringBuilder.append(event);
                throw new IllegalStateException(stringBuilder.toString());
            }
        } catch (NativeDaemonConnectorException e2) {
            throw e2.rethrowAsParcelableException();
        }
    }

    public void setInterfaceConfig(String iface, InterfaceConfiguration cfg) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        LinkAddress linkAddr = cfg.getLinkAddress();
        if (linkAddr == null || linkAddr.getAddress() == null) {
            throw new IllegalStateException("Null LinkAddress given");
        }
        Command cmd = new Command("interface", "setcfg", iface, linkAddr.getAddress().getHostAddress(), Integer.valueOf(linkAddr.getPrefixLength()));
        for (String flag : cfg.getFlags()) {
            cmd.appendArg(flag);
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setInterfaceDown(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        InterfaceConfiguration ifcg = getInterfaceConfig(iface);
        ifcg.setInterfaceDown();
        setInterfaceConfig(iface, ifcg);
    }

    public void setInterfaceUp(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        InterfaceConfiguration ifcg = getInterfaceConfig(iface);
        ifcg.setInterfaceUp();
        setInterfaceConfig(iface, ifcg);
    }

    public void setInterfaceIpv6PrivacyExtensions(String iface, boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            String str = "interface";
            Object[] objArr = new Object[3];
            objArr[0] = "ipv6privacyextensions";
            objArr[1] = iface;
            objArr[2] = enable ? "enable" : "disable";
            nativeDaemonConnector.execute(str, objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearInterfaceAddresses(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "clearaddrs", iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableIpv6(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "ipv6", iface, "enable");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIPv6AddrGenMode(String iface, int mode) throws ServiceSpecificException {
        try {
            this.mNetdService.setIPv6AddrGenMode(iface, mode);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public void disableIpv6(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("interface", "ipv6", iface, "disable");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addRoute(int netId, RouteInfo route) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.NETWORKMANAGEMENT_ADDROUTE);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(netId);
        modifyRoute("add", stringBuilder.toString(), route);
    }

    public void removeRoute(int netId, RouteInfo route) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(netId);
        modifyRoute("remove", stringBuilder.toString(), route);
    }

    private void modifyRoute(String action, String netId, RouteInfo route) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Command cmd = new Command("network", "route", action, netId);
        cmd.appendArg(route.getInterface());
        cmd.appendArg(route.getDestination().toString());
        int type = route.getType();
        if (type != 1) {
            if (type == 7) {
                cmd.appendArg("unreachable");
            } else if (type == 9) {
                cmd.appendArg("throw");
            }
        } else if (route.hasGateway()) {
            cmd.appendArg(route.getGateway().getHostAddress());
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    /* JADX WARNING: Missing block: B:19:0x003d, code:
            if (r0 == null) goto L_0x0040;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ArrayList<String> readRouteList(String filename) {
        FileInputStream fstream = null;
        ArrayList<String> list = new ArrayList();
        try {
            fstream = new FileInputStream(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(fstream)));
            while (true) {
                String readLine = br.readLine();
                String s = readLine;
                if (!(readLine == null || s.length() == 0)) {
                    list.add(s);
                }
                try {
                    fstream.close();
                    break;
                } catch (IOException e) {
                }
            }
        } catch (IOException e2) {
        } catch (Throwable th) {
            if (fstream != null) {
                try {
                    fstream.close();
                } catch (IOException e3) {
                }
            }
        }
        return list;
    }

    public void setMtu(String iface, int mtu) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("interface", "setmtu", iface, Integer.valueOf(mtu));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void shutdown() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SHUTDOWN", TAG);
        Slog.i(TAG, "Shutting down");
    }

    public boolean getIpForwardingEnabled() throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("ipfwd", "status");
            event.checkCode(NetdResponseCode.IpFwdStatusResult);
            return event.getMessage().endsWith("enabled");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setIpForwardingEnabled(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            String str = "ipfwd";
            Object[] objArr = new Object[2];
            objArr[0] = enable ? "enable" : "disable";
            objArr[1] = ConnectivityService.TETHERING_ARG;
            nativeDaemonConnector.execute(str, objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void startTethering(String[] dhcpRange) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] objArr = new Object[1];
        int i = 0;
        objArr[0] = "start";
        Command cmd = new Command("tether", objArr);
        int length = dhcpRange.length;
        while (i < length) {
            cmd.appendArg(dhcpRange[i]);
            i++;
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopTethering() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "stop");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isTetheringStarted() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("tether", "status");
            event.checkCode(NetdResponseCode.TetherStatusResult);
            return event.getMessage().endsWith("started");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void tetherInterface(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "interface", "add", iface);
            List<RouteInfo> routes = new ArrayList();
            routes.add(new RouteInfo(getInterfaceConfig(iface).getLinkAddress(), null, iface));
            addInterfaceToLocalNetwork(iface, routes);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void untetherInterface(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("tether", "interface", "remove", iface);
            removeInterfaceFromLocalNetwork(iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        } catch (Throwable th) {
            removeInterfaceFromLocalNetwork(iface);
        }
    }

    public String[] listTetheredInterfaces() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("tether", "interface", "list"), 111);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setDnsForwarders(Network network, String[] dns) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        int i = 0;
        int netId = network != null ? network.netId : 0;
        Command cmd = new Command("tether", "dns", "set", Integer.valueOf(netId));
        int length = dns.length;
        while (i < length) {
            cmd.appendArg(NetworkUtils.numericToInetAddress(dns[i]).getHostAddress());
            i++;
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public String[] getDnsForwarders() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("tether", "dns", "list"), 112);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    private List<InterfaceAddress> excludeLinkLocal(List<InterfaceAddress> addresses) {
        ArrayList<InterfaceAddress> filtered = new ArrayList(addresses.size());
        for (InterfaceAddress ia : addresses) {
            if (!ia.getAddress().isLinkLocalAddress()) {
                filtered.add(ia);
            }
        }
        return filtered;
    }

    private void modifyInterfaceForward(boolean add, String fromIface, String toIface) {
        String str = "ipfwd";
        Object[] objArr = new Object[3];
        objArr[0] = add ? "add" : "remove";
        objArr[1] = fromIface;
        objArr[2] = toIface;
        try {
            this.mConnector.execute(new Command(str, objArr));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void startInterfaceForwarding(String fromIface, String toIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        modifyInterfaceForward(true, fromIface, toIface);
    }

    public void stopInterfaceForwarding(String fromIface, String toIface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        modifyInterfaceForward(false, fromIface, toIface);
    }

    private void modifyNat(String action, String internalInterface, String externalInterface) throws SocketException {
        Command cmd = new Command("nat", action, internalInterface, externalInterface);
        NetworkInterface internalNetworkInterface = NetworkInterface.getByName(internalInterface);
        if (internalNetworkInterface == null) {
            cmd.appendArg("0");
        } else {
            List<InterfaceAddress> interfaceAddresses = excludeLinkLocal(internalNetworkInterface.getInterfaceAddresses());
            cmd.appendArg(Integer.valueOf(interfaceAddresses.size()));
            for (InterfaceAddress ia : interfaceAddresses) {
                InetAddress addr = NetworkUtils.getNetworkPart(ia.getAddress(), ia.getNetworkPrefixLength());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(addr.getHostAddress());
                stringBuilder.append(SliceAuthority.DELIMITER);
                stringBuilder.append(ia.getNetworkPrefixLength());
                cmd.appendArg(stringBuilder.toString());
            }
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void enableNat(String internalInterface, String externalInterface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            modifyNat("enable", internalInterface, externalInterface);
            if (HuaweiTelephonyConfigs.isQcomPlatform()) {
                NetPluginDelegate.natStarted(internalInterface, externalInterface);
            }
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }

    public void disableNat(String internalInterface, String externalInterface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            modifyNat("disable", internalInterface, externalInterface);
            if (HuaweiTelephonyConfigs.isQcomPlatform()) {
                NetPluginDelegate.natStopped(internalInterface, externalInterface);
            }
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }

    public String[] listTtys() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("list_ttys", new Object[0]), 113);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void attachPppd(String tty, String localAddr, String remoteAddr, String dns1Addr, String dns2Addr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("pppd", "attach", tty, NetworkUtils.numericToInetAddress(localAddr).getHostAddress(), NetworkUtils.numericToInetAddress(remoteAddr).getHostAddress(), NetworkUtils.numericToInetAddress(dns1Addr).getHostAddress(), NetworkUtils.numericToInetAddress(dns2Addr).getHostAddress());
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void detachPppd(String tty) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("pppd", "detach", tty);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addIdleTimer(String iface, int timeout, final int type) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (DBG) {
            Slog.d(TAG, "Adding idletimer");
        }
        synchronized (this.mIdleTimerLock) {
            IdleTimerParams params = (IdleTimerParams) this.mActiveIdleTimers.get(iface);
            if (params != null) {
                params.networkCount++;
                return;
            }
            try {
                this.mConnector.execute("idletimer", "add", iface, Integer.toString(timeout), Integer.toString(type));
                this.mActiveIdleTimers.put(iface, new IdleTimerParams(timeout, type));
                if (ConnectivityManager.isNetworkTypeMobile(type)) {
                    this.mNetworkActive = false;
                }
                this.mDaemonHandler.post(new Runnable() {
                    public void run() {
                        NetworkManagementService.this.notifyInterfaceClassActivity(type, 3, SystemClock.elapsedRealtimeNanos(), -1, false);
                    }
                });
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0067, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removeIdleTimer(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (DBG) {
            Slog.d(TAG, "Removing idletimer");
        }
        synchronized (this.mIdleTimerLock) {
            final IdleTimerParams params = (IdleTimerParams) this.mActiveIdleTimers.get(iface);
            if (params != null) {
                int i = params.networkCount - 1;
                params.networkCount = i;
                if (i <= 0) {
                    try {
                        this.mConnector.execute("idletimer", "remove", iface, Integer.toString(params.timeout), Integer.toString(params.type));
                        this.mActiveIdleTimers.remove(iface);
                        this.mDaemonHandler.post(new Runnable() {
                            public void run() {
                                NetworkManagementService.this.notifyInterfaceClassActivity(params.type, 1, SystemClock.elapsedRealtimeNanos(), -1, false);
                            }
                        });
                    } catch (NativeDaemonConnectorException e) {
                        throw e.rethrowAsParcelableException();
                    }
                }
            }
        }
    }

    public NetworkStats getNetworkStatsSummaryDev() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsSummaryDev();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public NetworkStats getNetworkStatsSummaryXt() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsSummaryXt();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public NetworkStats getNetworkStatsDetail() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsDetail(-1, null, -1, null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setInterfaceQuota(String iface, long quotaBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveQuotas.containsKey(iface)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("iface ");
                    stringBuilder.append(iface);
                    stringBuilder.append(" already has quota");
                    throw new IllegalStateException(stringBuilder.toString());
                }
                try {
                    this.mConnector.execute("bandwidth", "setiquota", iface, Long.valueOf(quotaBytes));
                    this.mActiveQuotas.put(iface, Long.valueOf(quotaBytes));
                    synchronized (this.mTetheringStatsProviders) {
                        for (ITetheringStatsProvider provider : this.mTetheringStatsProviders.keySet()) {
                            try {
                                provider.setInterfaceQuota(iface, quotaBytes);
                            } catch (RemoteException e) {
                                String str = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Problem setting tethering data limit on provider ");
                                stringBuilder2.append((String) this.mTetheringStatsProviders.get(provider));
                                stringBuilder2.append(": ");
                                stringBuilder2.append(e);
                                Log.e(str, stringBuilder2.toString());
                            }
                        }
                    }
                } catch (NativeDaemonConnectorException e2) {
                    throw e2.rethrowAsParcelableException();
                }
            }
        }
    }

    public void removeInterfaceQuota(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveQuotas.containsKey(iface)) {
                    this.mActiveQuotas.remove(iface);
                    this.mActiveAlerts.remove(iface);
                    try {
                        this.mConnector.execute("bandwidth", "removeiquota", iface);
                        synchronized (this.mTetheringStatsProviders) {
                            for (ITetheringStatsProvider provider : this.mTetheringStatsProviders.keySet()) {
                                try {
                                    provider.setInterfaceQuota(iface, -1);
                                } catch (RemoteException e) {
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Problem removing tethering data limit on provider ");
                                    stringBuilder.append((String) this.mTetheringStatsProviders.get(provider));
                                    stringBuilder.append(": ");
                                    stringBuilder.append(e);
                                    Log.e(str, stringBuilder.toString());
                                }
                            }
                        }
                        return;
                    } catch (NativeDaemonConnectorException e2) {
                        throw e2.rethrowAsParcelableException();
                    }
                }
            }
        }
    }

    public void setInterfaceAlert(String iface, long alertBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (!this.mBandwidthControlEnabled) {
            return;
        }
        if (this.mActiveQuotas.containsKey(iface)) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveAlerts.containsKey(iface)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("iface ");
                    stringBuilder.append(iface);
                    stringBuilder.append(" already has alert");
                    throw new IllegalStateException(stringBuilder.toString());
                }
                try {
                    this.mConnector.execute("bandwidth", "setinterfacealert", iface, Long.valueOf(alertBytes));
                    this.mActiveAlerts.put(iface, Long.valueOf(alertBytes));
                } catch (NativeDaemonConnectorException e) {
                    throw e.rethrowAsParcelableException();
                }
            }
            return;
        }
        throw new IllegalStateException("setting alert requires existing quota on iface");
    }

    public void removeInterfaceAlert(String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            synchronized (this.mQuotaLock) {
                if (this.mActiveAlerts.containsKey(iface)) {
                    try {
                        this.mConnector.execute("bandwidth", "removeinterfacealert", iface);
                        this.mActiveAlerts.remove(iface);
                        return;
                    } catch (NativeDaemonConnectorException e) {
                        throw e.rethrowAsParcelableException();
                    }
                }
            }
        }
    }

    public void setGlobalAlert(long alertBytes) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            try {
                this.mConnector.execute("bandwidth", "setglobalalert", Long.valueOf(alertBytes));
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    private void setUidOnMeteredNetworkList(int uid, boolean blacklist, boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (this.mBandwidthControlEnabled) {
            String chain = blacklist ? "naughtyapps" : "niceapps";
            String suffix = enable ? "add" : "remove";
            synchronized (this.mQuotaLock) {
                SparseBooleanArray quotaList;
                boolean oldEnable;
                synchronized (this.mRulesLock) {
                    quotaList = blacklist ? this.mUidRejectOnMetered : this.mUidAllowOnMetered;
                    oldEnable = quotaList.get(uid, false);
                }
                if (oldEnable == enable) {
                    return;
                }
                Trace.traceBegin(2097152, "inetd bandwidth");
                try {
                    Object[] objArr = new Object[2];
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(suffix);
                    stringBuilder.append(chain);
                    objArr[0] = stringBuilder.toString();
                    objArr[1] = Integer.valueOf(uid);
                    this.mConnector.execute("bandwidth", objArr);
                    synchronized (this.mRulesLock) {
                        if (enable) {
                            quotaList.put(uid, true);
                        } else {
                            quotaList.delete(uid);
                        }
                    }
                    Trace.traceEnd(2097152);
                } catch (NativeDaemonConnectorException e) {
                    try {
                        throw e.rethrowAsParcelableException();
                    } catch (Throwable th) {
                        Trace.traceEnd(2097152);
                    }
                }
            }
        }
    }

    public void setUidMeteredNetworkBlacklist(int uid, boolean enable) {
        setUidOnMeteredNetworkList(uid, true, enable);
    }

    public void setUidMeteredNetworkWhitelist(int uid, boolean enable) {
        setUidOnMeteredNetworkList(uid, false, enable);
    }

    public boolean setDataSaverModeEnabled(boolean enable) {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", TAG);
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDataSaverMode: ");
            stringBuilder.append(enable);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mQuotaLock) {
            if (this.mDataSaverMode == enable) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setDataSaverMode(): already ");
                stringBuilder2.append(this.mDataSaverMode);
                Log.w(str2, stringBuilder2.toString());
                return true;
            }
            Trace.traceBegin(2097152, "bandwidthEnableDataSaver");
            StringBuilder stringBuilder3;
            try {
                boolean changed = this.mNetdService.bandwidthEnableDataSaver(enable);
                if (changed) {
                    this.mDataSaverMode = enable;
                } else {
                    String str3 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("setDataSaverMode(");
                    stringBuilder3.append(enable);
                    stringBuilder3.append("): netd command silently failed");
                    Log.w(str3, stringBuilder3.toString());
                }
                Trace.traceEnd(2097152);
                return changed;
            } catch (RemoteException e) {
                try {
                    z = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("setDataSaverMode(");
                    stringBuilder3.append(enable);
                    stringBuilder3.append("): netd command failed");
                    Log.w(z, stringBuilder3.toString(), e);
                    z = false;
                } finally {
                    Trace.traceEnd(2097152);
                }
                return z;
            }
        }
    }

    public void setAllowOnlyVpnForUids(boolean add, UidRange[] uidRanges) throws ServiceSpecificException {
        StringBuilder stringBuilder;
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        try {
            this.mNetdService.networkRejectNonSecureVpn(add, uidRanges);
        } catch (ServiceSpecificException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setAllowOnlyVpnForUids(");
            stringBuilder.append(add);
            stringBuilder.append(", ");
            stringBuilder.append(Arrays.toString(uidRanges));
            stringBuilder.append("): netd command failed");
            Log.w(TAG, stringBuilder.toString(), e);
            throw e;
        } catch (RemoteException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setAllowOnlyVpnForUids(");
            stringBuilder.append(add);
            stringBuilder.append(", ");
            stringBuilder.append(Arrays.toString(uidRanges));
            stringBuilder.append("): netd command failed");
            Log.w(TAG, stringBuilder.toString(), e2);
            throw e2.rethrowAsRuntimeException();
        }
    }

    public int getNetdPid() {
        try {
            return this.mNetdService.getNetdPid();
        } catch (ServiceSpecificException e) {
            Log.w(TAG, "getNetdPid, SSE:", e);
            return -1;
        } catch (RemoteException e2) {
            Log.w(TAG, "getNetdPid RE", e2);
            return -1;
        }
    }

    private void applyUidCleartextNetworkPolicy(int uid, int policy) {
        String policyString;
        switch (policy) {
            case 0:
                policyString = "accept";
                break;
            case 1:
                policyString = "log";
                break;
            case 2:
                policyString = "reject";
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown policy ");
                stringBuilder.append(policy);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
        try {
            this.mConnector.execute("strict", "set_uid_cleartext_policy", Integer.valueOf(uid), policyString);
            this.mUidCleartextPolicy.put(uid, policy);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setUidCleartextNetworkPolicy(int uid, int policy) {
        if (Binder.getCallingUid() != uid) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        }
        synchronized (this.mQuotaLock) {
            int oldPolicy = this.mUidCleartextPolicy.get(uid, 0);
            if (oldPolicy == policy) {
            } else if (this.mStrictEnabled) {
                if (!(oldPolicy == 0 || policy == 0)) {
                    applyUidCleartextNetworkPolicy(uid, 0);
                }
                applyUidCleartextNetworkPolicy(uid, policy);
            } else {
                this.mUidCleartextPolicy.put(uid, policy);
            }
        }
    }

    public boolean isBandwidthControlEnabled() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return this.mBandwidthControlEnabled;
    }

    public NetworkStats getNetworkStatsUidDetail(int uid, String[] ifaces) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            return this.mStatsFactory.readNetworkStatsDetail(uid, ifaces, -1, null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public NetworkStats getNetworkStatsTethering(int how) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 1);
        synchronized (this.mTetheringStatsProviders) {
            for (ITetheringStatsProvider provider : this.mTetheringStatsProviders.keySet()) {
                try {
                    stats.combineAllValues(provider.getTetherStats(how));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Problem reading tethering stats from ");
                    stringBuilder.append((String) this.mTetheringStatsProviders.get(provider));
                    stringBuilder.append(": ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
            }
        }
        return stats;
    }

    public void setDnsConfigurationForNetwork(int netId, String[] servers, String[] domains, int[] params, String tlsHostname, String[] tlsServers) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mNetdService.setResolverConfiguration(netId, servers, domains, params, tlsHostname, tlsServers, new String[0]);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void addVpnUidRanges(int netId, UidRange[] ranges) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[13];
        int i = 0;
        argv[0] = SoundModelContract.KEY_USERS;
        argv[1] = "add";
        argv[2] = Integer.valueOf(netId);
        int argc = 3;
        while (i < ranges.length) {
            int argc2 = argc + 1;
            argv[argc] = ranges[i].toString();
            if (i == ranges.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 3;
                } catch (int argc3) {
                    throw argc3.rethrowAsParcelableException();
                }
            }
            argc3 = argc2;
            i++;
        }
    }

    public void removeVpnUidRanges(int netId, UidRange[] ranges) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[13];
        int i = 0;
        argv[0] = SoundModelContract.KEY_USERS;
        argv[1] = "remove";
        argv[2] = Integer.valueOf(netId);
        int argc = 3;
        while (i < ranges.length) {
            int argc2 = argc + 1;
            argv[argc] = ranges[i].toString();
            if (i == ranges.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 3;
                } catch (int argc3) {
                    throw argc3.rethrowAsParcelableException();
                }
            }
            argc3 = argc2;
            i++;
        }
    }

    public void setFirewallEnabled(boolean enabled) {
        enforceSystemUid();
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            String str = "firewall";
            Object[] objArr = new Object[2];
            objArr[0] = "enable";
            objArr[1] = enabled ? "whitelist" : "blacklist";
            nativeDaemonConnector.execute(str, objArr);
            this.mFirewallEnabled = enabled;
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isFirewallEnabled() {
        enforceSystemUid();
        return this.mFirewallEnabled;
    }

    public void setFirewallInterfaceRule(String iface, boolean allow) {
        enforceSystemUid();
        Preconditions.checkState(this.mFirewallEnabled);
        String rule = allow ? "allow" : "deny";
        try {
            this.mConnector.execute("firewall", "set_interface_rule", iface, rule);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:39:0x0085 A:{Splitter: B:37:0x007f, ExcHandler: android.os.RemoteException (r2_6 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x0085 A:{Splitter: B:37:0x007f, ExcHandler: android.os.RemoteException (r2_6 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:33:0x0070, code:
            r0 = r4;
     */
    /* JADX WARNING: Missing block: B:34:0x0072, code:
            if (r5 == r0.length) goto L_0x007b;
     */
    /* JADX WARNING: Missing block: B:35:0x0074, code:
            r0 = (android.net.UidRange[]) java.util.Arrays.copyOf(r0, r5);
     */
    /* JADX WARNING: Missing block: B:36:0x007b, code:
            r3 = r0;
            r1 = new int[0];
            r0 = r5;
     */
    /* JADX WARNING: Missing block: B:39:0x0085, code:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:40:0x0086, code:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Error closing sockets after enabling chain ");
            r5.append(r10);
            r5.append(": ");
            r5.append(r2);
            android.util.Slog.e(r4, r5.toString());
     */
    /* JADX WARNING: Missing block: B:56:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void closeSocketsForFirewallChainLocked(int chain, String chainName) {
        UidRange[] ranges;
        int[] exemptUids;
        int numUids = 0;
        int i = 0;
        if (getFirewallType(chain) == 0) {
            int[] exemptUids2;
            ranges = new UidRange[]{new UidRange(10000, HwBootFail.STAGE_BOOT_SUCCESS)};
            synchronized (this.mRulesLock) {
                SparseIntArray rules = getUidFirewallRulesLR(chain);
                exemptUids2 = new int[rules.size()];
                while (i < exemptUids2.length) {
                    if (rules.valueAt(i) == 1) {
                        exemptUids2[numUids] = rules.keyAt(i);
                        numUids++;
                    }
                    i++;
                }
            }
            exemptUids = exemptUids2;
            if (numUids != exemptUids.length) {
                exemptUids = Arrays.copyOf(exemptUids, numUids);
            }
        } else {
            synchronized (this.mRulesLock) {
                int numUids2;
                try {
                    SparseIntArray rules2 = getUidFirewallRulesLR(chain);
                    UidRange[] ranges2 = new UidRange[rules2.size()];
                    numUids2 = 0;
                    numUids = 0;
                    while (numUids < ranges2.length) {
                        try {
                            if (rules2.valueAt(numUids) == 2) {
                                int uid = rules2.keyAt(numUids);
                                ranges2[numUids2] = new UidRange(uid, uid);
                                numUids2++;
                            }
                            numUids++;
                        } catch (Throwable th) {
                            numUids = th;
                            throw numUids;
                        }
                    }
                } catch (Throwable th2) {
                    numUids2 = 0;
                    numUids = th2;
                    throw numUids;
                }
            }
        }
        try {
            this.mNetdService.socketDestroy(ranges, exemptUids);
        } catch (Exception e) {
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0016, code:
            if (r9 == false) goto L_0x001b;
     */
    /* JADX WARNING: Missing block: B:17:?, code:
            r1 = "enable_chain";
     */
    /* JADX WARNING: Missing block: B:18:0x001b, code:
            r1 = "disable_chain";
     */
    /* JADX WARNING: Missing block: B:19:0x001d, code:
            switch(r8) {
                case 1: goto L_0x002b;
                case 2: goto L_0x0027;
                case 3: goto L_0x0023;
                default: goto L_0x0020;
            };
     */
    /* JADX WARNING: Missing block: B:21:0x0023, code:
            r2 = "powersave";
     */
    /* JADX WARNING: Missing block: B:22:0x0027, code:
            r2 = "standby";
     */
    /* JADX WARNING: Missing block: B:23:0x002b, code:
            r2 = "dozable";
     */
    /* JADX WARNING: Missing block: B:25:?, code:
            r7.mConnector.execute("firewall", r1, r2);
     */
    /* JADX WARNING: Missing block: B:26:0x0040, code:
            if (r9 == false) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:29:0x0044, code:
            if (DBG == false) goto L_0x005c;
     */
    /* JADX WARNING: Missing block: B:30:0x0046, code:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("Closing sockets after enabling chain ");
            r4.append(r2);
            android.util.Slog.d(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:31:0x005c, code:
            closeSocketsForFirewallChainLocked(r8, r2);
     */
    /* JADX WARNING: Missing block: B:33:0x0060, code:
            return;
     */
    /* JADX WARNING: Missing block: B:34:0x0061, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:36:0x0066, code:
            throw r3.rethrowAsParcelableException();
     */
    /* JADX WARNING: Missing block: B:37:0x0067, code:
            r3 = new java.lang.StringBuilder();
            r3.append("Bad child chain: ");
            r3.append(r8);
     */
    /* JADX WARNING: Missing block: B:38:0x007b, code:
            throw new java.lang.IllegalArgumentException(r3.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setFirewallChainEnabled(int chain, boolean enable) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            synchronized (this.mRulesLock) {
                if (getFirewallChainState(chain) == enable) {
                    return;
                }
                setFirewallChainState(chain, enable);
            }
        }
    }

    private int getFirewallType(int chain) {
        switch (chain) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 0;
            default:
                return 1 ^ isFirewallEnabled();
        }
    }

    public void setFirewallUidRules(int chain, int[] uids, int[] rules) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            synchronized (this.mRulesLock) {
                int uid;
                SparseIntArray uidFirewallRules = getUidFirewallRulesLR(chain);
                SparseIntArray newRules = new SparseIntArray();
                for (int index = uids.length - 1; index >= 0; index--) {
                    uid = uids[index];
                    int rule = rules[index];
                    updateFirewallUidRuleLocked(chain, uid, rule);
                    newRules.put(uid, rule);
                }
                SparseIntArray rulesToRemove = new SparseIntArray();
                for (uid = uidFirewallRules.size() - 1; uid >= 0; uid--) {
                    int uid2 = uidFirewallRules.keyAt(uid);
                    if (newRules.indexOfKey(uid2) < 0) {
                        rulesToRemove.put(uid2, 0);
                    }
                }
                for (uid = rulesToRemove.size() - 1; uid >= 0; uid--) {
                    updateFirewallUidRuleLocked(chain, rulesToRemove.keyAt(uid), 0);
                }
            }
            switch (chain) {
                case 1:
                    this.mNetdService.firewallReplaceUidChain("fw_dozable", true, uids);
                    break;
                case 2:
                    this.mNetdService.firewallReplaceUidChain("fw_standby", false, uids);
                    break;
                case 3:
                    this.mNetdService.firewallReplaceUidChain("fw_powersave", true, uids);
                    break;
                default:
                    try {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setFirewallUidRules() called on invalid chain: ");
                        stringBuilder.append(chain);
                        Slog.d(str, stringBuilder.toString());
                        break;
                    } catch (RemoteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error flushing firewall chain ");
                        stringBuilder2.append(chain);
                        Slog.w(str2, stringBuilder2.toString(), e);
                        break;
                    }
            }
        }
    }

    public void setFirewallUidRule(int chain, int uid, int rule) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            setFirewallUidRuleLocked(chain, uid, rule);
        }
    }

    private void setFirewallUidRuleLocked(int chain, int uid, int rule) {
        if (updateFirewallUidRuleLocked(chain, uid, rule)) {
            try {
                this.mConnector.execute("firewall", "set_uid_rule", getFirewallChainName(chain), Integer.valueOf(uid), getFirewallRuleName(chain, rule));
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x004d, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean updateFirewallUidRuleLocked(int chain, int uid, int rule) {
        synchronized (this.mRulesLock) {
            String str;
            SparseIntArray uidFirewallRules = getUidFirewallRulesLR(chain);
            int oldUidFirewallRule = uidFirewallRules.get(uid, 0);
            if (DBG) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("oldRule = ");
                stringBuilder.append(oldUidFirewallRule);
                stringBuilder.append(", newRule=");
                stringBuilder.append(rule);
                stringBuilder.append(" for uid=");
                stringBuilder.append(uid);
                stringBuilder.append(" on chain ");
                stringBuilder.append(chain);
                Slog.d(str, stringBuilder.toString());
            }
            if (oldUidFirewallRule != rule) {
                String ruleName = getFirewallRuleName(chain, rule);
                str = getFirewallRuleName(chain, oldUidFirewallRule);
                if (rule == 0) {
                    uidFirewallRules.delete(uid);
                } else {
                    uidFirewallRules.put(uid, rule);
                }
                boolean equals = ruleName.equals(str) ^ 1;
                return equals;
            } else if (DBG) {
                Slog.d(TAG, "!!!!! Skipping change");
            }
        }
    }

    private String getFirewallRuleName(int chain, int rule) {
        if (getFirewallType(chain) == 0) {
            if (rule == 1) {
                return "allow";
            }
            return "deny";
        } else if (rule == 2) {
            return "deny";
        } else {
            return "allow";
        }
    }

    private SparseIntArray getUidFirewallRulesLR(int chain) {
        switch (chain) {
            case 0:
                return this.mUidFirewallRules;
            case 1:
                return this.mUidFirewallDozableRules;
            case 2:
                return this.mUidFirewallStandbyRules;
            case 3:
                return this.mUidFirewallPowerSaveRules;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown chain:");
                stringBuilder.append(chain);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public String getFirewallChainName(int chain) {
        switch (chain) {
            case 0:
                return "none";
            case 1:
                return "dozable";
            case 2:
                return "standby";
            case 3:
                return "powersave";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown chain:");
                stringBuilder.append(chain);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static void enforceSystemUid() {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only available to AID_SYSTEM");
        }
    }

    public void startClatd(String interfaceName) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("clatd", "start", interfaceName);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void stopClatd(String interfaceName) throws IllegalStateException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("clatd", "stop", interfaceName);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public boolean isClatdStarted(String interfaceName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonEvent event = this.mConnector.execute("clatd", "status", interfaceName);
            event.checkCode(NetdResponseCode.ClatdStatusResult);
            return event.getMessage().endsWith("started");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void registerNetworkActivityListener(INetworkActivityListener listener) {
        this.mNetworkActivityListeners.register(listener);
    }

    public void unregisterNetworkActivityListener(INetworkActivityListener listener) {
        this.mNetworkActivityListeners.unregister(listener);
    }

    public boolean isNetworkActive() {
        boolean z;
        synchronized (this.mNetworkActivityListeners) {
            z = this.mNetworkActive || this.mActiveIdleTimers.isEmpty();
        }
        return z;
    }

    /* JADX WARNING: Removed duplicated region for block: B:7:0x001c A:{Splitter: B:2:0x0009, ExcHandler: android.os.RemoteException (e android.os.RemoteException)} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void reportNetworkActive() {
        int length = this.mNetworkActivityListeners.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                ((INetworkActivityListener) this.mNetworkActivityListeners.getBroadcastItem(i)).onNetworkActive();
            } catch (RemoteException e) {
            } catch (Throwable th) {
                this.mNetworkActivityListeners.finishBroadcast();
            }
        }
        this.mNetworkActivityListeners.finishBroadcast();
    }

    public void monitor() {
        if (this.mConnector != null) {
            this.mConnector.monitor();
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("NetworkManagementService NativeDaemonConnector Log:");
            this.mConnector.dump(fd, pw, args);
            pw.println();
            pw.print("Bandwidth control enabled: ");
            pw.println(this.mBandwidthControlEnabled);
            pw.print("mMobileActivityFromRadio=");
            pw.print(this.mMobileActivityFromRadio);
            pw.print(" mLastPowerStateFromRadio=");
            pw.println(this.mLastPowerStateFromRadio);
            pw.print("mNetworkActive=");
            pw.println(this.mNetworkActive);
            synchronized (this.mQuotaLock) {
                pw.print("Active quota ifaces: ");
                pw.println(this.mActiveQuotas.toString());
                pw.print("Active alert ifaces: ");
                pw.println(this.mActiveAlerts.toString());
                pw.print("Data saver mode: ");
                pw.println(this.mDataSaverMode);
                synchronized (this.mRulesLock) {
                    dumpUidRuleOnQuotaLocked(pw, "blacklist", this.mUidRejectOnMetered);
                    dumpUidRuleOnQuotaLocked(pw, "whitelist", this.mUidAllowOnMetered);
                }
            }
            synchronized (this.mRulesLock) {
                dumpUidFirewallRule(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, this.mUidFirewallRules);
                pw.print("UID firewall standby chain enabled: ");
                pw.println(getFirewallChainState(2));
                dumpUidFirewallRule(pw, "standby", this.mUidFirewallStandbyRules);
                pw.print("UID firewall dozable chain enabled: ");
                pw.println(getFirewallChainState(1));
                dumpUidFirewallRule(pw, "dozable", this.mUidFirewallDozableRules);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UID firewall powersave chain enabled: ");
                stringBuilder.append(getFirewallChainState(3));
                pw.println(stringBuilder.toString());
                dumpUidFirewallRule(pw, "powersave", this.mUidFirewallPowerSaveRules);
            }
            synchronized (this.mIdleTimerLock) {
                pw.println("Idle timers:");
                for (Map.Entry<String, IdleTimerParams> ent : this.mActiveIdleTimers.entrySet()) {
                    pw.print("  ");
                    pw.print((String) ent.getKey());
                    pw.println(":");
                    IdleTimerParams params = (IdleTimerParams) ent.getValue();
                    pw.print("    timeout=");
                    pw.print(params.timeout);
                    pw.print(" type=");
                    pw.print(params.type);
                    pw.print(" networkCount=");
                    pw.println(params.networkCount);
                }
            }
            pw.print("Firewall enabled: ");
            pw.println(this.mFirewallEnabled);
            pw.print("Netd service status: ");
            if (this.mNetdService == null) {
                pw.println("disconnected");
            } else {
                try {
                    pw.println(this.mNetdService.isAlive() ? "alive" : "dead");
                } catch (RemoteException e) {
                    pw.println("unreachable");
                }
            }
        }
    }

    private void dumpUidRuleOnQuotaLocked(PrintWriter pw, String name, SparseBooleanArray list) {
        pw.print("UID bandwith control ");
        pw.print(name);
        pw.print(" rule: [");
        int size = list.size();
        for (int i = 0; i < size; i++) {
            pw.print(list.keyAt(i));
            if (i < size - 1) {
                pw.print(",");
            }
        }
        pw.println("]");
    }

    private void dumpUidFirewallRule(PrintWriter pw, String name, SparseIntArray rules) {
        pw.print("UID firewall ");
        pw.print(name);
        pw.print(" rule: [");
        int size = rules.size();
        for (int i = 0; i < size; i++) {
            pw.print(rules.keyAt(i));
            pw.print(":");
            pw.print(rules.valueAt(i));
            if (i < size - 1) {
                pw.print(",");
            }
        }
        pw.println("]");
    }

    public void createPhysicalNetwork(int netId, String permission) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (permission != null) {
            try {
                this.mConnector.execute("network", "create", Integer.valueOf(netId), permission);
                return;
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
        this.mConnector.execute("network", new Object[]{"create", Integer.valueOf(netId)});
    }

    public void createVirtualNetwork(int netId, boolean hasDNS, boolean secure) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            String str = "network";
            Object[] objArr = new Object[5];
            objArr[0] = "create";
            objArr[1] = Integer.valueOf(netId);
            objArr[2] = "vpn";
            objArr[3] = hasDNS ? "1" : "0";
            objArr[4] = secure ? "1" : "0";
            nativeDaemonConnector.execute(str, objArr);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void removeNetwork(int netId) {
        StringBuilder stringBuilder;
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
        try {
            this.mNetdService.networkDestroy(netId);
        } catch (ServiceSpecificException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeNetwork(");
            stringBuilder.append(netId);
            stringBuilder.append("): ");
            Log.w(TAG, stringBuilder.toString(), e);
            throw e;
        } catch (RemoteException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeNetwork(");
            stringBuilder.append(netId);
            stringBuilder.append("): ");
            Log.w(TAG, stringBuilder.toString(), e2);
            throw e2.rethrowAsRuntimeException();
        }
    }

    public void addInterfaceToNetwork(String iface, int netId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(netId);
        modifyInterfaceInNetwork("add", stringBuilder.toString(), iface);
    }

    public void removeInterfaceFromNetwork(String iface, int netId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(netId);
        modifyInterfaceInNetwork("remove", stringBuilder.toString(), iface);
    }

    private void modifyInterfaceInNetwork(String action, String netId, String iface) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "interface", action, netId, iface);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addLegacyRouteForNetId(int netId, RouteInfo routeInfo, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Command cmd = new Command("network", "route", "legacy", Integer.valueOf(uid), "add", Integer.valueOf(netId));
        LinkAddress la = routeInfo.getDestinationLinkAddress();
        cmd.appendArg(routeInfo.getInterface());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(la.getAddress().getHostAddress());
        stringBuilder.append(SliceAuthority.DELIMITER);
        stringBuilder.append(la.getPrefixLength());
        cmd.appendArg(stringBuilder.toString());
        if (routeInfo.hasGateway()) {
            cmd.appendArg(routeInfo.getGateway().getHostAddress());
        }
        try {
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setDefaultNetId(int netId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", HealthServiceWrapper.INSTANCE_VENDOR, "set", Integer.valueOf(netId));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearDefaultNetId() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", HealthServiceWrapper.INSTANCE_VENDOR, "clear");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setNetworkPermission(int netId, String permission) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (permission != null) {
            try {
                this.mConnector.execute("network", "permission", "network", "set", permission, Integer.valueOf(netId));
                return;
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
        this.mConnector.execute("network", new Object[]{"permission", "network", "clear", Integer.valueOf(netId)});
    }

    public void setPermission(String permission, int[] uids) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[14];
        int i = 0;
        argv[0] = "permission";
        argv[1] = "user";
        argv[2] = "set";
        argv[3] = permission;
        int argc = 4;
        while (i < uids.length) {
            int argc2 = argc + 1;
            argv[argc] = Integer.valueOf(uids[i]);
            if (i == uids.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 4;
                } catch (int argc3) {
                    throw argc3.rethrowAsParcelableException();
                }
            }
            argc3 = argc2;
            i++;
        }
    }

    public void clearPermission(int[] uids) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        Object[] argv = new Object[13];
        int i = 0;
        argv[0] = "permission";
        argv[1] = "user";
        argv[2] = "clear";
        int argc = 3;
        while (i < uids.length) {
            int argc2 = argc + 1;
            argv[argc] = Integer.valueOf(uids[i]);
            if (i == uids.length - 1 || argc2 == argv.length) {
                try {
                    this.mConnector.execute("network", Arrays.copyOf(argv, argc2));
                    argc = 3;
                } catch (int argc3) {
                    throw argc3.rethrowAsParcelableException();
                }
            }
            argc3 = argc2;
            i++;
        }
    }

    public void allowProtect(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "protect", "allow", Integer.valueOf(uid));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void denyProtect(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        try {
            this.mConnector.execute("network", "protect", "deny", Integer.valueOf(uid));
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void addInterfaceToLocalNetwork(String iface, List<RouteInfo> routes) {
        modifyInterfaceInNetwork("add", "local", iface);
        for (RouteInfo route : routes) {
            if (!route.isDefaultRoute()) {
                modifyRoute("add", "local", route);
            }
        }
    }

    public void removeInterfaceFromLocalNetwork(String iface) {
        modifyInterfaceInNetwork("remove", "local", iface);
    }

    public int removeRoutesFromLocalNetwork(List<RouteInfo> routes) {
        int failures = 0;
        for (RouteInfo route : routes) {
            try {
                modifyRoute("remove", "local", route);
            } catch (IllegalStateException e) {
                failures++;
            }
        }
        return failures;
    }

    public boolean isNetworkRestricted(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return isNetworkRestrictedInternal(uid);
    }

    /* JADX WARNING: Missing block: B:12:0x0033, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:21:0x0062, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:30:0x0092, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:37:0x00bb, code:
            return true;
     */
    /* JADX WARNING: Missing block: B:46:0x00e8, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isNetworkRestrictedInternal(int uid) {
        synchronized (this.mRulesLock) {
            String str;
            StringBuilder stringBuilder;
            if (getFirewallChainState(2) && this.mUidFirewallStandbyRules.get(uid) == 2) {
                if (DBG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Uid ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" restricted because of app standby mode");
                    Slog.d(str, stringBuilder.toString());
                }
            } else if (!getFirewallChainState(1) || this.mUidFirewallDozableRules.get(uid) == 1) {
                if (!getFirewallChainState(3) || this.mUidFirewallPowerSaveRules.get(uid) == 1) {
                    if (this.mUidRejectOnMetered.get(uid)) {
                        if (DBG) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Uid ");
                            stringBuilder.append(uid);
                            stringBuilder.append(" restricted because of no metered data in the background");
                            Slog.d(str, stringBuilder.toString());
                        }
                    } else if (!this.mDataSaverMode || this.mUidAllowOnMetered.get(uid)) {
                        return false;
                    } else if (DBG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Uid ");
                        stringBuilder.append(uid);
                        stringBuilder.append(" restricted because of data saver mode");
                        Slog.d(str, stringBuilder.toString());
                    }
                } else if (DBG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Uid ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" restricted because of power saver mode");
                    Slog.d(str, stringBuilder.toString());
                }
            } else if (DBG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Uid ");
                stringBuilder.append(uid);
                stringBuilder.append(" restricted because of device idle mode");
                Slog.d(str, stringBuilder.toString());
            }
        }
    }

    private void setFirewallChainState(int chain, boolean state) {
        synchronized (this.mRulesLock) {
            this.mFirewallChainStates.put(chain, state);
        }
    }

    private boolean getFirewallChainState(int chain) {
        boolean z;
        synchronized (this.mRulesLock) {
            z = this.mFirewallChainStates.get(chain);
        }
        return z;
    }

    @VisibleForTesting
    Injector getInjector() {
        return new Injector();
    }

    public String getFirewallRuleNameHw(int chain, int rule) {
        return getFirewallRuleName(chain, rule);
    }
}
