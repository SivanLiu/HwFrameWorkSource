package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.AppOpsManager;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hdm.HwDeviceManager;
import android.hsm.HwSystemManager;
import android.net.DhcpInfo;
import android.net.DhcpResults;
import android.net.IpConfiguration.IpAssignment;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.HiSiWifiComm;
import android.net.wifi.ISoftApCallback;
import android.net.wifi.IWifiManager.Stub;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiDetectConfInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager.SoftApCallback;
import android.net.wifi.WifiSsid;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.util.Log;
import android.util.MutableInt;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.PowerProfile;
import com.android.internal.util.AsyncChannel;
import com.android.server.wifi.LocalOnlyHotspotRequestInfo.RequestingApplicationDeathCallback;
import com.android.server.wifi.hotspot2.PasspointProvider;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.util.WifiHandler;
import com.android.server.wifi.util.WifiPermissionsUtil;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WifiServiceImpl extends Stub {
    private static final int BACKGROUND_IMPORTANCE_CUTOFF = 125;
    private static final int BOTH_2G_AND_5G = 2;
    private static final boolean DBG = true;
    private static final long DEFAULT_SCAN_BACKGROUND_THROTTLE_INTERVAL_MS = 1800000;
    private static final int DEFAULT_VALUE = 0;
    private static final boolean IS_ATT;
    private static final boolean IS_VERIZON;
    private static final long LOG_SCAN_RESULTS_INTERVAL_MS = 3000;
    private static final int MAX_SLEEP_RETRY_TIMES = 10;
    private static final String NO_KNOW_NAME = "android";
    private static final int NUM_SOFT_AP_CALLBACKS_WARN_LIMIT = 10;
    private static final int NUM_SOFT_AP_CALLBACKS_WTF_LIMIT = 20;
    private static final int ONLY_2G = 1;
    private static final String POLICY_OPEN_HOTSPOT = "policy-open-hotspot";
    private static final int RUN_WITH_SCISSORS_TIMEOUT_MILLIS = 4000;
    private static final int SCANRESULTS_COUNT_MAX = 200;
    private static final int SCAN_PROXY_RESULTS_MAX_AGE_IN_MILLIS = 30000;
    private static final String SET_SSID_NAME = "set_hotspot_ssid_name";
    private static final String TAG = "WifiService";
    private static final String VALUE_DISABLE = "value_disable";
    private static final boolean VDBG = false;
    private static final int WAIT_SLEEP_TIME = 100;
    private static final Object mWifiLock = new Object();
    private final ActivityManager mActivityManager;
    private final AppOpsManager mAppOps;
    private ClientHandler mClientHandler;
    private final Clock mClock;
    private final Context mContext;
    private final WifiCountryCode mCountryCode;
    private final FrameworkFacade mFacade;
    private final FrameworkFacade mFrameworkFacade;
    private HiSiWifiComm mHiSiWifiComm;
    private IHwBehaviorCollectManager mHwBehaviorManager;
    private HwWifiCHRService mHwWifiCHRService;
    @GuardedBy("mLocalOnlyHotspotRequests")
    private final ConcurrentHashMap<String, Integer> mIfaceIpModes;
    boolean mInIdleMode;
    boolean mInLightIdleMode;
    private boolean mIsApDialogNeedShow;
    private boolean mIsDialogNeedShow;
    private boolean mIsP2pCloseDialogExist;
    private long mLastLogScanResultsTime;
    @GuardedBy("mLocalOnlyHotspotRequests")
    private WifiConfiguration mLocalOnlyHotspotConfig;
    @GuardedBy("mLocalOnlyHotspotRequests")
    private final HashMap<Integer, LocalOnlyHotspotRequestInfo> mLocalOnlyHotspotRequests;
    private WifiLog mLog;
    private final BroadcastReceiver mPackageOrUserReceiver;
    private final boolean mPermissionReviewRequired;
    private final PowerManager mPowerManager;
    PowerProfile mPowerProfile;
    private final BroadcastReceiver mReceiver;
    private final HashMap<Integer, ISoftApCallback> mRegisteredSoftApCallbacks;
    boolean mScanPending;
    final ScanRequestProxy mScanRequestProxy;
    final WifiSettingsStore mSettingsStore;
    private int mSoftApNumClients;
    private int mSoftApState;
    private WifiTrafficPoller mTrafficPoller;
    private final UserManager mUserManager;
    private boolean mVerboseLoggingEnabled;
    private WifiApConfigStore mWifiApConfigStore;
    private final WifiBackupRestore mWifiBackupRestore;
    private WifiController mWifiController;
    private final WifiInjector mWifiInjector;
    private final WifiLockManager mWifiLockManager;
    private final WifiMetrics mWifiMetrics;
    private final WifiMulticastLockManager mWifiMulticastLockManager;
    private WifiNative mWifiNative;
    private WifiPermissionsUtil mWifiPermissionsUtil;
    WifiServiceHisiExt mWifiServiceHisiExt = null;
    final WifiStateMachine mWifiStateMachine;
    private AsyncChannel mWifiStateMachineChannel;
    WifiStateMachineHandler mWifiStateMachineHandler;
    final WifiStateMachinePrime mWifiStateMachinePrime;
    private int scanRequestCounter;
    private DataUploader uploader;

    private final class SoftApCallbackImpl implements SoftApCallback {
        private SoftApCallbackImpl() {
        }

        /* synthetic */ SoftApCallbackImpl(WifiServiceImpl x0, AnonymousClass1 x1) {
            this();
        }

        public void onStateChanged(int state, int failureReason) {
            WifiServiceImpl.this.mSoftApState = state;
            Iterator<ISoftApCallback> iterator = WifiServiceImpl.this.mRegisteredSoftApCallbacks.values().iterator();
            while (iterator.hasNext()) {
                try {
                    ((ISoftApCallback) iterator.next()).onStateChanged(state, failureReason);
                } catch (RemoteException e) {
                    String str = WifiServiceImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onStateChanged: remote exception -- ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    iterator.remove();
                }
            }
        }

        public void onNumClientsChanged(int numClients) {
            WifiServiceImpl.this.mSoftApNumClients = numClients;
            Iterator<ISoftApCallback> iterator = WifiServiceImpl.this.mRegisteredSoftApCallbacks.values().iterator();
            while (iterator.hasNext()) {
                try {
                    ((ISoftApCallback) iterator.next()).onNumClientsChanged(numClients);
                } catch (RemoteException e) {
                    String str = WifiServiceImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onNumClientsChanged: remote exception -- ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    iterator.remove();
                }
            }
        }
    }

    class TdlsTask extends AsyncTask<TdlsTaskParams, Integer, Integer> {
        TdlsTask() {
        }

        protected Integer doInBackground(TdlsTaskParams... params) {
            TdlsTaskParams param = params[0];
            String remoteIpAddress = param.remoteIpAddress.trim();
            boolean enable = param.enable;
            String macAddress = null;
            BufferedReader reader = null;
            try {
                String readLine;
                reader = new BufferedReader(new FileReader("/proc/net/arp"));
                String line = reader.readLine();
                while (true) {
                    readLine = reader.readLine();
                    line = readLine;
                    if (readLine == null) {
                        break;
                    }
                    String[] tokens = line.split("[ ]+");
                    if (tokens.length >= 6) {
                        String ip = tokens[0];
                        String mac = tokens[3];
                        if (remoteIpAddress.equals(ip)) {
                            macAddress = mac;
                            break;
                        }
                    }
                }
                if (macAddress == null) {
                    readLine = WifiServiceImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Did not find remoteAddress {");
                    stringBuilder.append(remoteIpAddress);
                    stringBuilder.append("} in /proc/net/arp");
                    Slog.w(readLine, stringBuilder.toString());
                } else {
                    WifiServiceImpl.this.enableTdlsWithMacAddress(macAddress, enable);
                }
                try {
                    reader.close();
                } catch (IOException e) {
                }
            } catch (FileNotFoundException e2) {
                Slog.e(WifiServiceImpl.TAG, "Could not open /proc/net/arp to lookup mac address");
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e3) {
                Slog.e(WifiServiceImpl.TAG, "Could not read /proc/net/arp to lookup mac address");
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
            return Integer.valueOf(0);
        }
    }

    class TdlsTaskParams {
        public boolean enable;
        public String remoteIpAddress;

        TdlsTaskParams() {
        }
    }

    private class ClientHandler extends WifiHandler {
        ClientHandler(String tag, Looper looper) {
            super(tag, looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String str;
            StringBuilder stringBuilder;
            WifiConfiguration config;
            int networkId;
            String str2;
            StringBuilder stringBuilder2;
            switch (msg.what) {
                case 69632:
                    if (msg.arg1 == 0) {
                        Slog.d(WifiServiceImpl.TAG, "New client listening to asynchronous messages");
                        WifiServiceImpl.this.mTrafficPoller.addClient(msg.replyTo);
                        return;
                    }
                    str = WifiServiceImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Client connection failure, error=");
                    stringBuilder.append(msg.arg1);
                    Slog.e(str, stringBuilder.toString());
                    return;
                case 69633:
                    WifiServiceImpl.this.mFrameworkFacade.makeWifiAsyncChannel(WifiServiceImpl.TAG).connect(WifiServiceImpl.this.mContext, this, msg.replyTo);
                    return;
                case 69636:
                    if (msg.arg1 == 2) {
                        Slog.w(WifiServiceImpl.TAG, "Send failed, client connection lost");
                    } else {
                        str = WifiServiceImpl.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Client connection lost with reason: ");
                        stringBuilder.append(msg.arg1);
                        Slog.w(str, stringBuilder.toString());
                    }
                    WifiServiceImpl.this.mTrafficPoller.removeClient(msg.replyTo);
                    return;
                case 151553:
                    if (checkChangePermissionAndReplyIfNotAuthorized(msg, 151554)) {
                        config = (WifiConfiguration) msg.obj;
                        networkId = msg.arg1;
                        str2 = WifiServiceImpl.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("CONNECT  nid=");
                        stringBuilder2.append(Integer.toString(networkId));
                        stringBuilder2.append(" config=");
                        stringBuilder2.append(config);
                        stringBuilder2.append(" uid=");
                        stringBuilder2.append(msg.sendingUid);
                        stringBuilder2.append(" name=");
                        stringBuilder2.append(WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(msg.sendingUid));
                        Slog.d(str2, stringBuilder2.toString());
                        WifiServiceImpl.this.mWifiStateMachine.setCHRConnectingSartTimestamp(WifiServiceImpl.this.mClock.getElapsedSinceBootMillis());
                        if (WifiServiceImpl.this.mHwWifiCHRService != null) {
                            WifiServiceImpl.this.mHwWifiCHRService.updateApkChangewWifiStatus(10, WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(msg.sendingUid));
                        }
                        WifiServiceImpl.this.mWifiController.updateWMUserAction(WifiServiceImpl.this.mContext, "ACTION_SELECT_WIFINETWORK", WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(msg.sendingUid));
                        if (config != null && WifiServiceImpl.isValid(config)) {
                            if (WifiServiceImpl.this.mHwWifiCHRService != null) {
                                WifiServiceImpl.this.mHwWifiCHRService.updateConnectType("FIRST_CONNECT");
                            }
                            WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                            return;
                        } else if (config != null || networkId == -1) {
                            str2 = WifiServiceImpl.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("ClientHandler.handleMessage ignoring invalid msg=");
                            stringBuilder2.append(msg);
                            Slog.e(str2, stringBuilder2.toString());
                            replyFailed(msg, 151554, 8);
                            return;
                        } else {
                            if (WifiServiceImpl.this.mHwWifiCHRService != null) {
                                WifiServiceImpl.this.mHwWifiCHRService.updateConnectType("SELECT_CONNECT");
                            }
                            WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                            return;
                        }
                    }
                    return;
                case 151556:
                    if (checkChangePermissionAndReplyIfNotAuthorized(msg, 151557)) {
                        if (WifiServiceImpl.this.mHwWifiCHRService != null) {
                            WifiServiceImpl.this.mHwWifiCHRService.reportHwCHRAccessNetworkEventInfoList(2);
                            WifiServiceImpl.this.mHwWifiCHRService.updateApkChangewWifiStatus(11, WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(msg.sendingUid));
                        }
                        WifiServiceImpl.this.mWifiController.updateWMUserAction(WifiServiceImpl.this.mContext, "ACTION_FORGET_WIFINETWORK", WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(msg.sendingUid));
                        if (Boolean.valueOf(SystemProperties.getBoolean("ro.vendor.config.hw_vowifi", false)).booleanValue()) {
                            WifiServiceImpl.this.handleForgetNetwork(Message.obtain(msg));
                            return;
                        } else {
                            WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                            return;
                        }
                    }
                    return;
                case 151559:
                    if (checkChangePermissionAndReplyIfNotAuthorized(msg, 151560)) {
                        config = msg.obj;
                        networkId = msg.arg1;
                        str2 = WifiServiceImpl.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SAVE nid=");
                        stringBuilder2.append(Integer.toString(networkId));
                        stringBuilder2.append(" config=");
                        stringBuilder2.append(config);
                        stringBuilder2.append(" uid=");
                        stringBuilder2.append(msg.sendingUid);
                        stringBuilder2.append(" name=");
                        stringBuilder2.append(WifiServiceImpl.this.mContext.getPackageManager().getNameForUid(msg.sendingUid));
                        Slog.d(str2, stringBuilder2.toString());
                        if (config != null) {
                            WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                            return;
                        }
                        str2 = WifiServiceImpl.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ClientHandler.handleMessage ignoring invalid msg=");
                        stringBuilder2.append(msg);
                        Slog.e(str2, stringBuilder2.toString());
                        replyFailed(msg, 151560, 8);
                        return;
                    }
                    return;
                case 151562:
                    if (checkChangePermissionAndReplyIfNotAuthorized(msg, 151564)) {
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                        return;
                    }
                    return;
                case 151566:
                    if (checkChangePermissionAndReplyIfNotAuthorized(msg, 151567)) {
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                        return;
                    }
                    return;
                case 151569:
                    if (checkChangePermissionAndReplyIfNotAuthorized(msg, 151570)) {
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                        return;
                    }
                    return;
                case 151572:
                    if (checkChangePermissionAndReplyIfNotAuthorized(msg, 151574)) {
                        WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                        return;
                    }
                    return;
                case 151575:
                    WifiServiceImpl.this.mWifiStateMachine.sendMessage(Message.obtain(msg));
                    return;
                default:
                    str = WifiServiceImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ClientHandler.handleMessage ignoring msg=");
                    stringBuilder.append(msg);
                    Slog.d(str, stringBuilder.toString());
                    return;
            }
        }

        private boolean checkChangePermissionAndReplyIfNotAuthorized(Message msg, int replyWhat) {
            if (WifiServiceImpl.this.mWifiPermissionsUtil.checkChangePermission(msg.sendingUid)) {
                return true;
            }
            String str = WifiServiceImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ClientHandler.handleMessage ignoring unauthorized msg=");
            stringBuilder.append(msg);
            Slog.e(str, stringBuilder.toString());
            replyFailed(msg, replyWhat, 9);
            return false;
        }

        private void replyFailed(Message msg, int what, int why) {
            if (msg.replyTo != null) {
                Message reply = Message.obtain();
                reply.what = what;
                reply.arg1 = why;
                try {
                    msg.replyTo.send(reply);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public final class LocalOnlyRequestorCallback implements RequestingApplicationDeathCallback {
        public void onLocalOnlyHotspotRequestorDeath(LocalOnlyHotspotRequestInfo requestor) {
            WifiServiceImpl.this.unregisterCallingAppAndStopLocalOnlyHotspot(requestor);
        }
    }

    private class WifiStateMachineHandler extends WifiHandler {
        private AsyncChannel mWsmChannel;

        WifiStateMachineHandler(String tag, Looper looper, AsyncChannel asyncChannel) {
            super(tag, looper);
            this.mWsmChannel = asyncChannel;
            this.mWsmChannel.connect(WifiServiceImpl.this.mContext, this, WifiServiceImpl.this.mWifiStateMachine.getHandler());
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String str;
            StringBuilder stringBuilder;
            switch (msg.what) {
                case 69632:
                    if (msg.arg1 == 0) {
                        WifiServiceImpl.this.mWifiStateMachineChannel = this.mWsmChannel;
                        return;
                    }
                    str = WifiServiceImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("WifiStateMachine connection failure, error=");
                    stringBuilder.append(msg.arg1);
                    Slog.e(str, stringBuilder.toString());
                    WifiServiceImpl.this.mWifiStateMachineChannel = null;
                    return;
                case 69636:
                    str = WifiServiceImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("WifiStateMachine channel lost, msg.arg1 =");
                    stringBuilder.append(msg.arg1);
                    Slog.e(str, stringBuilder.toString());
                    WifiServiceImpl.this.mWifiStateMachineChannel = null;
                    this.mWsmChannel.connect(WifiServiceImpl.this.mContext, this, WifiServiceImpl.this.mWifiStateMachine.getHandler());
                    return;
                case WifiStateMachine.CMD_CHANGE_TO_STA_P2P_CONNECT /*131573*/:
                    Slog.e(WifiServiceImpl.TAG, "handleMessage CMD_CHANGE_TO_STA_P2P_CONNECT");
                    WifiServiceImpl.this.showP2pToStaDialog();
                    return;
                case WifiStateMachine.CMD_CHANGE_TO_AP_P2P_CONNECT /*131574*/:
                    Slog.e(WifiServiceImpl.TAG, "handleMessage CMD_CHANGE_TO_STA_AP_CONNECT");
                    Bundle data = msg.getData();
                    WifiConfiguration wifiConfig = (WifiConfiguration) data.getParcelable("wifiConfig");
                    WifiServiceImpl.this.showP2pToAPDialog(wifiConfig, data.getBoolean("isWifiApEnabled"));
                    return;
                default:
                    str = WifiServiceImpl.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("WifiStateMachineHandler.handleMessage ignoring msg=");
                    stringBuilder2.append(msg);
                    Slog.d(str, stringBuilder2.toString());
                    return;
            }
        }
    }

    static {
        boolean z = false;
        boolean z2 = "389".equals(SystemProperties.get("ro.config.hw_opta", "0")) && "840".equals(SystemProperties.get("ro.config.hw_optb", "0"));
        IS_VERIZON = z2;
        if ("07".equals(SystemProperties.get("ro.config.hw_opta", "0")) && "840".equals(SystemProperties.get("ro.config.hw_optb", "0"))) {
            z = true;
        }
        IS_ATT = z;
    }

    public WifiServiceImpl(Context context, WifiInjector wifiInjector, AsyncChannel asyncChannel) {
        boolean z = false;
        this.scanRequestCounter = 0;
        this.mIsP2pCloseDialogExist = false;
        this.mVerboseLoggingEnabled = false;
        this.mLocalOnlyHotspotConfig = null;
        this.mSoftApState = 11;
        this.mSoftApNumClients = 0;
        this.mLastLogScanResultsTime = 0;
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String action = intent.getAction();
                    if (action != null) {
                        String str = WifiServiceImpl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onReceive, action:");
                        stringBuilder.append(action);
                        Slog.d(str, stringBuilder.toString());
                        if (action.equals("android.intent.action.USER_PRESENT")) {
                            WifiServiceImpl.this.mWifiController.sendMessage(155660);
                        } else if (action.equals("android.intent.action.USER_REMOVED")) {
                            WifiServiceImpl.this.mWifiStateMachine.removeUserConfigs(intent.getIntExtra("android.intent.extra.user_handle", 0));
                        } else if (action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                            WifiServiceImpl.this.mWifiStateMachine.sendBluetoothAdapterStateChange(intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", 0));
                        } else if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                            WifiServiceImpl.this.mWifiController.sendMessage(155649, intent.getBooleanExtra("phoneinECMState", false), 0);
                        } else if (action.equals("android.intent.action.EMERGENCY_CALL_STATE_CHANGED")) {
                            WifiServiceImpl.this.mWifiController.sendMessage(155662, intent.getBooleanExtra("phoneInEmergencyCall", false), 0);
                        } else if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                            WifiServiceImpl.this.handleIdleModeChanged();
                        } else if (action.equals("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED")) {
                            WifiServiceImpl.this.handleLightIdleModeChanged();
                        } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                            int wifiApState = intent.getIntExtra("wifi_state", 14);
                            String str2 = WifiServiceImpl.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("wifiApState=");
                            stringBuilder2.append(wifiApState);
                            Slog.d(str2, stringBuilder2.toString());
                            if (wifiApState == 14) {
                                WifiServiceImpl.this.stopSoftAp();
                            }
                        } else {
                            WifiServiceImpl.this.onReceiveEx(context, intent);
                        }
                    }
                }
            }
        };
        this.mPackageOrUserReceiver = new BroadcastReceiver() {
            /* JADX WARNING: Removed duplicated region for block: B:12:0x0047  */
            /* JADX WARNING: Removed duplicated region for block: B:14:0x0071  */
            /* JADX WARNING: Removed duplicated region for block: B:13:0x0063  */
            /* JADX WARNING: Removed duplicated region for block: B:12:0x0047  */
            /* JADX WARNING: Removed duplicated region for block: B:14:0x0071  */
            /* JADX WARNING: Removed duplicated region for block: B:13:0x0063  */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(Context context, Intent intent) {
                int i;
                String action = intent.getAction();
                String str = WifiServiceImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive, action:");
                stringBuilder.append(action);
                Slog.d(str, stringBuilder.toString());
                str = intent.getAction();
                int hashCode = str.hashCode();
                if (hashCode != -2061058799) {
                    if (hashCode == 525384130 && str.equals("android.intent.action.PACKAGE_REMOVED")) {
                        i = 0;
                        switch (i) {
                            case 0:
                                if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                                    i = intent.getIntExtra("android.intent.extra.UID", -1);
                                    Uri uri = intent.getData();
                                    if (i != -1 && uri != null) {
                                        WifiServiceImpl.this.mWifiStateMachine.removeAppConfigs(uri.getSchemeSpecificPart(), i);
                                        break;
                                    }
                                    return;
                                }
                                return;
                                break;
                            case 1:
                                WifiServiceImpl.this.mWifiStateMachine.removeUserConfigs(intent.getIntExtra("android.intent.extra.user_handle", 0));
                                break;
                            default:
                                str = WifiServiceImpl.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("onReceive, action:");
                                stringBuilder.append(action);
                                stringBuilder.append(" no handle");
                                Slog.d(str, stringBuilder.toString());
                                break;
                        }
                    }
                } else if (str.equals("android.intent.action.USER_REMOVED")) {
                    i = 1;
                    switch (i) {
                        case 0:
                            break;
                        case 1:
                            break;
                        default:
                            break;
                    }
                }
                i = -1;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    default:
                        break;
                }
            }
        };
        this.mContext = context;
        this.mWifiInjector = wifiInjector;
        this.mClock = wifiInjector.getClock();
        this.mFacade = this.mWifiInjector.getFrameworkFacade();
        this.mWifiMetrics = this.mWifiInjector.getWifiMetrics();
        this.mTrafficPoller = this.mWifiInjector.getWifiTrafficPoller();
        this.mUserManager = this.mWifiInjector.getUserManager();
        this.mCountryCode = this.mWifiInjector.getWifiCountryCode();
        this.mHwWifiCHRService = HwWifiServiceFactory.getHwWifiCHRService();
        this.mWifiStateMachine = this.mWifiInjector.getWifiStateMachine();
        this.mWifiNative = this.mWifiInjector.getWifiNative();
        this.mWifiStateMachinePrime = this.mWifiInjector.getWifiStateMachinePrime();
        this.mWifiStateMachine.enableRssiPolling(true);
        this.mScanRequestProxy = this.mWifiInjector.getScanRequestProxy();
        this.mSettingsStore = this.mWifiInjector.getWifiSettingsStore();
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mWifiLockManager = this.mWifiInjector.getWifiLockManager();
        this.mWifiMulticastLockManager = this.mWifiInjector.getWifiMulticastLockManager();
        HandlerThread wifiServiceHandlerThread = this.mWifiInjector.getWifiServiceHandlerThread();
        this.mClientHandler = new ClientHandler(TAG, wifiServiceHandlerThread.getLooper());
        this.mWifiStateMachineHandler = new WifiStateMachineHandler(TAG, wifiServiceHandlerThread.getLooper(), asyncChannel);
        this.mWifiController = this.mWifiInjector.getWifiController();
        this.mWifiBackupRestore = this.mWifiInjector.getWifiBackupRestore();
        this.mWifiApConfigStore = this.mWifiInjector.getWifiApConfigStore();
        if (Build.PERMISSIONS_REVIEW_REQUIRED || context.getResources().getBoolean(17957000)) {
            z = true;
        }
        this.mPermissionReviewRequired = z;
        this.mWifiPermissionsUtil = this.mWifiInjector.getWifiPermissionsUtil();
        this.mLog = this.mWifiInjector.makeLog(TAG);
        this.mFrameworkFacade = wifiInjector.getFrameworkFacade();
        this.mIfaceIpModes = new ConcurrentHashMap();
        this.mLocalOnlyHotspotRequests = new HashMap();
        enableVerboseLoggingInternal(getVerboseLoggingLevel());
        this.mRegisteredSoftApCallbacks = new HashMap();
        this.mWifiInjector.getWifiStateMachinePrime().registerSoftApCallback(new SoftApCallbackImpl(this, null));
        this.mPowerProfile = this.mWifiInjector.getPowerProfile();
        if (WifiServiceHisiExt.hisiWifiEnabled()) {
            this.mWifiServiceHisiExt = new WifiServiceHisiExt(this.mContext);
            this.mHiSiWifiComm = new HiSiWifiComm(this.mContext);
            this.mWifiServiceHisiExt.mWifiStateMachineHisiExt = this.mWifiStateMachine.mWifiStateMachineHisiExt;
        }
        this.mWifiStateMachine.setmSettingsStore(this.mSettingsStore);
        this.uploader = DataUploader.getInstance();
        this.uploader.setContext(this.mContext);
    }

    @VisibleForTesting
    public void setWifiHandlerLogForTest(WifiLog log) {
        this.mClientHandler.setWifiLog(log);
    }

    public void checkAndStartWifi() {
        if (this.mFrameworkFacade.inStorageManagerCryptKeeperBounce()) {
            Log.d(TAG, "Device still encrypted. Need to restart SystemServer.  Do not start wifi.");
            return;
        }
        boolean wifiEnabled = this.mSettingsStore.isWifiToggleEnabled();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WifiService starting up with Wi-Fi ");
        stringBuilder.append(wifiEnabled ? "enabled" : "disabled");
        Slog.i(str, stringBuilder.toString());
        this.mWifiStateMachine.setWifiRepeaterStoped();
        registerForScanModeChange();
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (WifiServiceImpl.this.mdmForPolicyForceOpenWifi(false, false)) {
                    Slog.w(WifiServiceImpl.TAG, "mdm force open wifi, not allow airplane close wifi");
                    return;
                }
                if (WifiServiceImpl.this.mSettingsStore.handleAirplaneModeToggled()) {
                    if (Boolean.valueOf(SystemProperties.getBoolean("ro.vendor.config.hw_vowifi", false)).booleanValue()) {
                        WifiServiceImpl.this.handleAirplaneModeToggled();
                    } else {
                        WifiServiceImpl.this.mWifiController.sendMessage(155657);
                    }
                }
                if (WifiServiceImpl.this.mSettingsStore.isAirplaneModeOn()) {
                    Log.d(WifiServiceImpl.TAG, "resetting country code because Airplane mode is ON");
                    WifiServiceImpl.this.mCountryCode.airplaneModeEnabled();
                }
            }
        }, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String state = intent.getStringExtra("ss");
                if ("ABSENT".equals(state)) {
                    Log.d(WifiServiceImpl.TAG, "resetting networks because SIM was removed");
                    WifiServiceImpl.this.mWifiStateMachine.resetSimAuthNetworks(false);
                    WifiServiceImpl.this.mWifiStateMachine.notifyImsiAvailabe(false);
                } else if ("LOCKED".equals(state)) {
                    Log.d(WifiServiceImpl.TAG, "SIM is locked");
                    WifiServiceImpl.this.mWifiStateMachine.notifyImsiAvailabe(false);
                } else if ("IMSI".equals(state)) {
                    Log.d(WifiServiceImpl.TAG, "SIM is available");
                    WifiServiceImpl.this.mWifiStateMachine.notifyImsiAvailabe(true);
                } else if ("LOADED".equals(state)) {
                    Log.d(WifiServiceImpl.TAG, "resetting networks because SIM was loaded");
                    WifiServiceImpl.this.mWifiStateMachine.resetSimAuthNetworks(true);
                }
            }
        }, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                WifiServiceImpl.this.handleWifiApStateChange(intent.getIntExtra("wifi_state", 11), intent.getIntExtra("previous_wifi_state", 11), intent.getIntExtra("wifi_ap_error_code", -1), intent.getStringExtra("wifi_ap_interface_name"), intent.getIntExtra("wifi_ap_mode", -1));
            }
        }, new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"));
        if (!WifiServiceHisiExt.hisiWifiEnabled()) {
            registerForBroadcasts();
        } else if (!this.mWifiServiceHisiExt.mIsReceiverRegistered) {
            registerForBroadcasts();
            this.mWifiServiceHisiExt.mIsReceiverRegistered = true;
        }
        this.mInIdleMode = this.mPowerManager.isDeviceIdleMode();
        this.mInLightIdleMode = this.mPowerManager.isLightDeviceIdleMode();
        int waitRetry = 0;
        while (waitRetry < 10 && this.mWifiStateMachineChannel == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("wait connect to WifiStateMachineChannel sleep");
            stringBuilder2.append(waitRetry);
            Log.e(str2, stringBuilder2.toString());
            try {
                Thread.sleep(100);
                waitRetry++;
            } catch (InterruptedException e) {
                Log.e(TAG, "exception happened");
            }
        }
        if (!this.mWifiStateMachine.syncInitialize(this.mWifiStateMachineChannel)) {
            Log.wtf(TAG, "Failed to initialize WifiStateMachine");
        }
        this.mWifiController.start();
        HwWifiServiceFactory.getHwWifiServiceManager().createHwArpVerifier(this.mContext);
        if (wifiEnabled) {
            try {
                setWifiEnabled(this.mContext.getPackageName(), wifiEnabled);
            } catch (RemoteException e2) {
            }
        }
    }

    public void handleUserSwitch(int userId) {
        this.mWifiStateMachine.handleUserSwitch(userId);
    }

    public void handleUserUnlock(int userId) {
        this.mWifiStateMachine.handleUserUnlock(userId);
    }

    public void handleUserStop(int userId) {
        this.mWifiStateMachine.handleUserStop(userId);
    }

    /* JADX WARNING: Missing block: B:27:0x00c9, code skipped:
            if (r12 == null) goto L_0x00e0;
     */
    /* JADX WARNING: Missing block: B:30:0x00cf, code skipped:
            if (r12.length() == 0) goto L_0x00e0;
     */
    /* JADX WARNING: Missing block: B:31:0x00d1, code skipped:
            r11.mWifiPermissionsUtil.enforceCanAccessScanResults(r12, r0);
     */
    /* JADX WARNING: Missing block: B:34:0x00da, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:36:0x00dd, code skipped:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:39:0x00e6, code skipped:
            if (r11.mWifiStateMachine.allowWifiScanRequest(r2) == null) goto L_0x00f7;
     */
    /* JADX WARNING: Missing block: B:40:0x00e8, code skipped:
            android.util.Slog.d(TAG, "wifi_scan reject because the interval isn't arrived");
            sendFailedScanDirectionalBroadcast(r12);
     */
    /* JADX WARNING: Missing block: B:41:0x00f2, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:42:0x00f6, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:45:0x00fb, code skipped:
            if (startQuickttffScan(r12) == null) goto L_0x0102;
     */
    /* JADX WARNING: Missing block: B:46:0x00fd, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:47:0x0101, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:50:0x0106, code skipped:
            if (limitWifiScanRequest(r12) == null) goto L_0x0126;
     */
    /* JADX WARNING: Missing block: B:51:0x0108, code skipped:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("current scan request is refused ");
            r6.append(r12);
            android.util.Log.d(r5, r6.toString());
            sendFailedScanDirectionalBroadcast(r12);
     */
    /* JADX WARNING: Missing block: B:52:0x0121, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:53:0x0125, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:56:0x012a, code skipped:
            if (limitWifiScanInAbsoluteRest(r12) == null) goto L_0x014a;
     */
    /* JADX WARNING: Missing block: B:57:0x012c, code skipped:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("absolute rest, scan request is refused ");
            r6.append(r12);
            android.util.Log.d(r5, r6.toString());
            sendFailedScanDirectionalBroadcast(r12);
     */
    /* JADX WARNING: Missing block: B:58:0x0145, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:59:0x0149, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:62:0x014e, code skipped:
            if (restrictWifiScanRequest(r12) == null) goto L_0x016b;
     */
    /* JADX WARNING: Missing block: B:63:0x0150, code skipped:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("scan ctrl by PG, skip ");
            r6.append(r12);
            android.util.Slog.i(r5, r6.toString());
     */
    /* JADX WARNING: Missing block: B:64:0x0166, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:65:0x016a, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:68:0x016d, code skipped:
            if (r11.mHwWifiCHRService == null) goto L_0x0175;
     */
    /* JADX WARNING: Missing block: B:69:0x016f, code skipped:
            r11.mHwWifiCHRService.updateApkChangewWifiStatus(4, r12);
     */
    /* JADX WARNING: Missing block: B:70:0x0175, code skipped:
            r5 = new com.android.server.wifi.util.GeneralUtil.Mutable();
     */
    /* JADX WARNING: Missing block: B:71:0x018b, code skipped:
            if (r11.mWifiInjector.getWifiStateMachineHandler().runWithScissors(new com.android.server.wifi.-$$Lambda$WifiServiceImpl$71KWGZ9o3U1lf_2vP7tmY9cz4qQ(r11, r5, r0, r12), 4000) != false) goto L_0x019c;
     */
    /* JADX WARNING: Missing block: B:72:0x018d, code skipped:
            android.util.Log.e(TAG, "Failed to post runnable to start scan");
            sendFailedScanBroadcast();
     */
    /* JADX WARNING: Missing block: B:73:0x0197, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:74:0x019b, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:77:0x01a4, code skipped:
            if (((java.lang.Boolean) r5.value).booleanValue() != false) goto L_0x01b2;
     */
    /* JADX WARNING: Missing block: B:78:0x01a6, code skipped:
            android.util.Log.e(TAG, "Failed to start scan");
     */
    /* JADX WARNING: Missing block: B:79:0x01ad, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:80:0x01b1, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:81:0x01b2, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:82:0x01b6, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:84:?, code skipped:
            r6 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("Exception:");
            r7.append(r5.getMessage());
            android.util.Log.w(r6, r7.toString());
     */
    /* JADX WARNING: Missing block: B:85:0x01d2, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:86:0x01d6, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:88:?, code skipped:
            r6 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("SecurityException:");
            r7.append(r5.getMessage());
            android.util.Log.w(r6, r7.toString());
     */
    /* JADX WARNING: Missing block: B:89:0x01f2, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* JADX WARNING: Missing block: B:90:0x01f6, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:91:0x01f7, code skipped:
            android.os.Binder.restoreCallingIdentity(r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean startScan(String packageName) {
        sendBehavior(BehaviorId.WIFI_STARTSCAN);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startScan, pid:");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid:");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", appName:");
        stringBuilder.append(packageName);
        Slog.d(str, stringBuilder.toString());
        if (!this.mPowerManager.isScreenOn() && !"com.huawei.ca".equals(packageName) && !"com.huawei.parentcontrol".equals(packageName) && Binder.getCallingUid() != 1000 && !"com.huawei.hidisk".equals(packageName)) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Screen is off, ");
            stringBuilder2.append(packageName);
            stringBuilder2.append(" startScan is skipped.");
            Slog.i(str, stringBuilder2.toString());
            return false;
        } else if (enforceChangePermission(packageName) != 0) {
            return false;
        } else {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            long ident = Binder.clearCallingIdentity();
            this.mLog.info("startScan uid=%").c((long) callingUid).flush();
            if (limitForegroundWifiScanRequest(packageName, callingUid)) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("current foreground scan request is refused ");
                stringBuilder3.append(packageName);
                Log.d(str2, stringBuilder3.toString());
                sendFailedScanDirectionalBroadcast(packageName);
                return false;
            }
            synchronized (this) {
                if (this.mInIdleMode) {
                    sendFailedScanBroadcast();
                    this.mScanPending = true;
                    return false;
                }
            }
        }
    }

    private void sendFailedScanBroadcast() {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
            intent.addFlags(67108864);
            intent.putExtra("resultsUpdated", false);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private void sendFailedScanDirectionalBroadcast(String packageName) {
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

    public String getCurrentNetworkWpsNfcConfigurationToken() {
        enforceConnectivityInternalPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getCurrentNetworkWpsNfcConfigurationToken uid=%").c((long) Binder.getCallingUid()).flush();
        }
        return null;
    }

    void handleIdleModeChanged() {
        boolean doScan = false;
        synchronized (this) {
            boolean idle = this.mPowerManager.isDeviceIdleMode();
            if (this.mInIdleMode != idle) {
                this.mInIdleMode = idle;
                if (!idle && this.mScanPending) {
                    this.mScanPending = false;
                    doScan = true;
                }
                handleLightIdleModeChanged();
            }
        }
        if (doScan) {
            startScan(this.mContext.getOpPackageName());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x003f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void handleLightIdleModeChanged() {
        synchronized (this) {
            boolean combinedIdle;
            String str;
            StringBuilder stringBuilder;
            boolean lightIdle = this.mPowerManager.isLightDeviceIdleMode();
            boolean deepIdle = this.mPowerManager.isDeviceIdleMode();
            if (!lightIdle) {
                if (!deepIdle) {
                    combinedIdle = false;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("handleLightIdleModeChanged: lightIdle:");
                    stringBuilder.append(lightIdle);
                    stringBuilder.append(",deepIdle:");
                    stringBuilder.append(deepIdle);
                    stringBuilder.append(",combinedIdle:");
                    stringBuilder.append(combinedIdle);
                    Slog.d(str, stringBuilder.toString());
                    if (this.mInLightIdleMode != combinedIdle) {
                        this.mInLightIdleMode = combinedIdle;
                        setFilterEnable(combinedIdle);
                    }
                }
            }
            combinedIdle = true;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleLightIdleModeChanged: lightIdle:");
            stringBuilder.append(lightIdle);
            stringBuilder.append(",deepIdle:");
            stringBuilder.append(deepIdle);
            stringBuilder.append(",combinedIdle:");
            stringBuilder.append(combinedIdle);
            Slog.d(str, stringBuilder.toString());
            if (this.mInLightIdleMode != combinedIdle) {
            }
        }
    }

    private boolean checkNetworkSettingsPermission(int pid, int uid) {
        return this.mContext.checkPermission("android.permission.NETWORK_SETTINGS", pid, uid) == 0;
    }

    private void setFilterEnable(boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setFilterEnable:");
        stringBuilder.append(enable);
        Slog.d(str, stringBuilder.toString());
        this.mWifiNative.setFilterEnable(this.mWifiNative.getClientInterfaceName(), enable);
    }

    private void enforceNetworkSettingsPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", TAG);
    }

    private void enforceNetworkStackPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private int enforceChangePermission(String callingPackage) {
        if (checkNetworkSettingsPermission(Binder.getCallingPid(), Binder.getCallingUid())) {
            return 0;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
        return this.mAppOps.noteOp("android:change_wifi_state", Binder.getCallingUid(), callingPackage);
    }

    private void enforceLocationHardwarePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", "LocationHardware");
    }

    private void enforceReadCredentialPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_WIFI_CREDENTIAL", TAG);
    }

    private void enforceWorkSourcePermission() {
        this.mContext.enforceCallingPermission("android.permission.UPDATE_DEVICE_STATS", TAG);
    }

    private void enforceMulticastChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_MULTICAST_STATE", TAG);
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", "ConnectivityService");
    }

    protected String getAppName(int pID) {
        String processName = "";
        List<RunningAppProcessInfo> appProcessList = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == pID) {
                return appProcess.processName;
            }
        }
        return null;
    }

    private void setHisiWifiApEnabled(boolean enabled, WifiConfiguration wifiConfig, WifiController mWifiController) {
        this.mWifiServiceHisiExt.mWifiStateMachineHisiExt.setWifiApEnabled(enabled);
        if (wifiConfig == null || isValid(wifiConfig)) {
            mWifiController.obtainMessage(155658, enabled, 0, wifiConfig).sendToTarget();
        } else {
            Slog.e(TAG, "Invalid WifiConfiguration");
        }
    }

    private void showP2pToAPDialog(final WifiConfiguration wifiConfig, final boolean enabled) {
        if (this.mIsP2pCloseDialogExist) {
            Slog.d(TAG, "the dialog already exist don't show dialog again");
            return;
        }
        Slog.d(TAG, "showP2pToAPDialog enter");
        Resources r = Resources.getSystem();
        CheckBox checkBox = new CheckBox(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null)));
        checkBox.setChecked(false);
        checkBox.setText(r.getString(33685815));
        checkBox.setTextSize(14.0f);
        checkBox.setTextColor(-16777216);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WifiServiceImpl.this.mIsApDialogNeedShow = isChecked;
            }
        });
        AlertDialog dialog = new Builder(this.mContext, 33947691).setCancelable(false).setTitle(r.getString(33685813)).setMessage(r.getString(33685814)).setView(checkBox).setNegativeButton(r.getString(17039360), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                WifiServiceImpl.this.mIsP2pCloseDialogExist = false;
                Slog.d(WifiServiceImpl.TAG, "NegativeButton is click");
                WifiServiceImpl.this.setWifiApStateByManual(false);
            }
        }).setPositiveButton(r.getString(17039370), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                WifiServiceImpl.this.mIsP2pCloseDialogExist = false;
                Slog.d(WifiServiceImpl.TAG, "PositiveButton is click");
                if (WifiServiceImpl.this.mWifiServiceHisiExt.isAirplaneModeOn()) {
                    Slog.d(WifiServiceImpl.TAG, "Cann't start AP with airPlaneMode on");
                    return;
                }
                WifiServiceImpl.this.mHiSiWifiComm.changeShowDialogFlag("show_ap_dialog_flag", WifiServiceImpl.this.mIsApDialogNeedShow);
                WifiServiceImpl.this.setHisiWifiApEnabled(enabled, wifiConfig, WifiServiceImpl.this.mWifiController);
            }
        }).create();
        dialog.getWindow().setType(2014);
        dialog.show();
        this.mIsP2pCloseDialogExist = true;
        Slog.d(TAG, "dialog showed");
    }

    private void showP2pToStaDialog() {
        if (this.mIsP2pCloseDialogExist) {
            Slog.d(TAG, "the dialog already exist don't show dialog again");
            return;
        }
        Slog.d(TAG, "showP2pToStaDialog enter");
        Resources r = Resources.getSystem();
        CheckBox checkBox = new CheckBox(new ContextThemeWrapper(this.mContext, this.mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null)));
        checkBox.setChecked(false);
        checkBox.setText(r.getString(33685815));
        checkBox.setTextSize(14.0f);
        checkBox.setTextColor(-16777216);
        checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WifiServiceImpl.this.mIsDialogNeedShow = isChecked;
            }
        });
        AlertDialog dialog = new Builder(this.mContext, 33947691).setCancelable(false).setTitle(r.getString(33685811)).setMessage(r.getString(33685812)).setView(checkBox).setNegativeButton(r.getString(17039360), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                WifiServiceImpl.this.mIsP2pCloseDialogExist = false;
                Slog.d(WifiServiceImpl.TAG, "NegativeButton is click");
                WifiServiceImpl.this.mWifiServiceHisiExt.mWifiStateMachineHisiExt.sendWifiStateDisabledBroadcast();
            }
        }).setPositiveButton(r.getString(17039370), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                WifiServiceImpl.this.mIsP2pCloseDialogExist = false;
                Slog.d(WifiServiceImpl.TAG, "PositiveButton is click");
                if (WifiServiceImpl.this.mWifiServiceHisiExt.isWifiP2pEnabled() || !(1 == WifiServiceImpl.this.getWifiEnabledState() || WifiServiceImpl.this.getWifiEnabledState() == 0)) {
                    WifiServiceImpl.this.mHiSiWifiComm.changeShowDialogFlag("show_sta_dialog_flag", WifiServiceImpl.this.mIsDialogNeedShow);
                    WifiServiceImpl.this.setWifiStateByManual(true);
                    WifiServiceImpl.this.mWifiServiceHisiExt.setWifiP2pEnabled(3);
                    return;
                }
                Slog.d(WifiServiceImpl.TAG, "supplicant is closed ,enble wifi with start supplicant");
                try {
                    WifiServiceImpl.this.setWifiEnabled(WifiServiceImpl.this.mContext.getPackageName(), true);
                } catch (RemoteException e) {
                    Slog.d(WifiServiceImpl.TAG, "setWifiEnabled fail, RemoteException e");
                }
            }
        }).create();
        dialog.getWindow().setType(2014);
        dialog.show();
        this.mIsP2pCloseDialogExist = true;
        Slog.d(TAG, "dialog showed");
    }

    private void enforceLocationPermission(String pkgName, int uid) {
        this.mWifiPermissionsUtil.enforceLocationPermission(pkgName, uid);
    }

    private boolean checkWifiPermissionWhenPermissionReviewRequired() {
        boolean z = false;
        if (!this.mPermissionReviewRequired) {
            return false;
        }
        if (this.mContext.checkCallingPermission("android.permission.MANAGE_WIFI_WHEN_PERMISSION_REVIEW_REQUIRED") == 0) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Removed duplicated region for block: B:47:0x00e4 A:{Catch:{ all -> 0x0278 }} */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00fe A:{Catch:{ all -> 0x0278 }} */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x00f4 A:{Catch:{ all -> 0x0278 }} */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x0131 A:{Catch:{ all -> 0x0278 }} */
    /* JADX WARNING: Removed duplicated region for block: B:117:0x021e A:{SYNTHETIC, Splitter:B:117:0x021e} */
    /* JADX WARNING: Removed duplicated region for block: B:110:0x0200 A:{Catch:{ all -> 0x0278 }} */
    /* JADX WARNING: Missing block: B:61:0x0129, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:116:0x021d, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:145:0x0277, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean setWifiEnabled(String packageName, boolean enable) throws RemoteException {
        sendBehavior(BehaviorId.WIFI_SETWIFIENABLED);
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        if (HwDeviceManager.disallowOp(0)) {
            Slog.i(TAG, "Wifi has been restricted by MDM apk.");
            return false;
        } else if (mdmForPolicyForceOpenWifi(true, enable)) {
            Slog.w(TAG, "mdm force open wifi, not allow close wifi");
            return false;
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setWifiEnabled: ");
            stringBuilder.append(enable);
            stringBuilder.append(" pid=");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", uid=");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", package=");
            stringBuilder.append(packageName);
            stringBuilder.append(", Stack=");
            stringBuilder.append(NO_KNOW_NAME.equals(packageName) ? Log.getStackTraceString(new Throwable()) : null);
            Slog.d(str, stringBuilder.toString());
            this.mLog.info("setWifiEnabled package=% uid=% enable=%").c(packageName).c((long) Binder.getCallingUid()).c(enable).flush();
            boolean isFromSettings = checkNetworkSettingsPermission(Binder.getCallingPid(), Binder.getCallingUid());
            if (!this.mSettingsStore.isAirplaneModeOn() || isFromSettings) {
                boolean apEnabled;
                long ident;
                if (this.mSoftApState != 13) {
                    if (this.mSoftApState != 12) {
                        apEnabled = false;
                        if (apEnabled || isFromSettings) {
                            if (this.mHwWifiCHRService != null) {
                                if (enable) {
                                    this.mHwWifiCHRService.updateApkChangewWifiStatus(5, packageName);
                                } else {
                                    this.mHwWifiCHRService.updateApkChangewWifiStatus(1, packageName);
                                }
                            }
                            if (enable) {
                                this.mWifiController.updateWMUserAction(this.mContext, "ACTION_ENABLE_WIFI_FALSE", packageName);
                            } else {
                                this.mWifiController.updateWMUserAction(this.mContext, "ACTION_ENABLE_WIFI_TRUE", packageName);
                            }
                            if (this.mContext != null || HwSystemManager.allowOp(this.mContext, 2097152, enable)) {
                                if (WifiServiceHisiExt.hisiWifiEnabled()) {
                                    String str2 = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("setWifiEnabled, P2P enable flag is ");
                                    stringBuilder2.append(this.mWifiServiceHisiExt.isWifiP2pEnabled());
                                    Slog.d(str2, stringBuilder2.toString());
                                    SystemProperties.set("sys.open_wifi_pid", Integer.toString(Binder.getCallingPid()));
                                    if (this.mWifiServiceHisiExt.isWifiP2pEnabled() && enable) {
                                        int flag = this.mHiSiWifiComm.getSettingsGlobalIntValue("show_sta_dialog_flag");
                                        if (!this.mWifiServiceHisiExt.checkUseNotCoexistPermission()) {
                                            Slog.d(TAG, "the software have no some important permissions,return false.");
                                            return false;
                                        } else if (this.mHiSiWifiComm.isP2pConnect() && flag != 1) {
                                            Slog.e(TAG, "sendEmptyMessage CMD_CHANGE_TO_STA_P2P_CONNECT");
                                            this.mWifiStateMachineHandler.sendEmptyMessage(WifiStateMachine.CMD_CHANGE_TO_STA_P2P_CONNECT);
                                        } else if (this.mIsP2pCloseDialogExist) {
                                            Slog.d(TAG, "the p2p to AP dialog already exist cann't open wifi");
                                            return false;
                                        } else {
                                            setWifiStateByManual(true);
                                            this.mWifiServiceHisiExt.setWifiP2pEnabled(3);
                                        }
                                    } else if (this.mWifiServiceHisiExt.isWifiP2pEnabled() && !enable) {
                                        if (this.mWifiServiceHisiExt.checkUseNotCoexistPermission()) {
                                            this.mWifiServiceHisiExt.setWifiP2pEnabled(3);
                                        } else {
                                            Slog.d(TAG, "the software have no some important permissions.");
                                            return false;
                                        }
                                    }
                                }
                                if (Binder.getCallingUid() == 0 && "factory".equals(SystemProperties.get("ro.runmode", "normal")) && SystemProperties.getInt("wlan.wltest.status", 0) > 0) {
                                    Slog.e(TAG, "in wltest mode, dont allow to enable WiFi");
                                    return false;
                                }
                                ident = Binder.clearCallingIdentity();
                                if (this.mSettingsStore.handleWifiToggled(enable)) {
                                    Slog.d(TAG, "setWifiEnabled,Nothing to do if wifi cannot be toggled.");
                                    if (enable) {
                                        this.uploader.e(52, "{ACT:1,STATUS:failed,DETAIL:cannot be toggled}");
                                    } else {
                                        this.uploader.e(52, "{ACT:0,STATUS:failed,DETAIL:cannot be toggled}");
                                    }
                                } else {
                                    Binder.restoreCallingIdentity(ident);
                                    if (this.mPermissionReviewRequired) {
                                        int wiFiEnabledState = getWifiEnabledState();
                                        if (enable) {
                                            if ((wiFiEnabledState == 0 || wiFiEnabledState == 1) && startConsentUi(packageName, Binder.getCallingUid(), "android.net.wifi.action.REQUEST_ENABLE")) {
                                                return true;
                                            }
                                        } else if ((wiFiEnabledState == 2 || wiFiEnabledState == 3) && startConsentUi(packageName, Binder.getCallingUid(), "android.net.wifi.action.REQUEST_DISABLE")) {
                                            return true;
                                        }
                                    }
                                    if (this.mHwWifiCHRService != null) {
                                        this.mHwWifiCHRService.updateWifiTriggerState(enable);
                                    }
                                    if (Boolean.valueOf(SystemProperties.getBoolean("ro.vendor.config.hw_vowifi", false)).booleanValue()) {
                                        setWifiEnabledAfterVoWifiOff(enable);
                                    } else {
                                        this.mWifiController.sendMessage(155656);
                                    }
                                }
                            } else if (enable) {
                                this.uploader.e(52, "{ACT:1,STATUS:failed,DETAIL:permission deny}");
                            } else {
                                this.uploader.e(52, "{ACT:0,STATUS:failed,DETAIL:permission deny}");
                            }
                        } else {
                            this.mLog.info("setWifiEnabled SoftAp not disabled: only Settings can enable wifi").flush();
                            return false;
                        }
                    }
                }
                apEnabled = true;
                if (apEnabled) {
                }
                if (this.mHwWifiCHRService != null) {
                }
                if (enable) {
                }
                if (this.mContext != null) {
                }
                if (WifiServiceHisiExt.hisiWifiEnabled()) {
                }
                if (Binder.getCallingUid() == 0) {
                }
                ident = Binder.clearCallingIdentity();
                try {
                    if (this.mSettingsStore.handleWifiToggled(enable)) {
                    }
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                this.mLog.info("setWifiEnabled in Airplane mode: only Settings can enable wifi").flush();
                return false;
            }
        }
    }

    public void setWifiStateByManual(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (WifiServiceHisiExt.hisiWifiEnabled()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setWifiStateByManual:");
            stringBuilder.append(enable);
            stringBuilder.append(",mIsReceiverRegistered:");
            stringBuilder.append(this.mWifiServiceHisiExt.mIsReceiverRegistered);
            Slog.d(str, stringBuilder.toString());
            if (enable) {
                if (!this.mWifiServiceHisiExt.mIsReceiverRegistered) {
                    registerForBroadcasts();
                    this.mWifiServiceHisiExt.mIsReceiverRegistered = true;
                }
            } else if (this.mWifiServiceHisiExt.mIsReceiverRegistered) {
                this.mContext.unregisterReceiver(this.mReceiver);
                this.mContext.unregisterReceiver(this.mPackageOrUserReceiver);
                this.mWifiServiceHisiExt.mIsReceiverRegistered = false;
            }
            this.mWifiServiceHisiExt.mWifiStateMachineHisiExt.setWifiStateByManual(enable);
        }
    }

    public void setWifiApStateByManual(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (WifiServiceHisiExt.hisiWifiEnabled()) {
            this.mWifiServiceHisiExt.mWifiStateMachineHisiExt.setWifiApStateByManual(enable);
        }
    }

    public void setWifiEnableForP2p(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (WifiServiceHisiExt.hisiWifiEnabled()) {
            this.mWifiServiceHisiExt.mWifiStateMachineHisiExt.setWifiEnableForP2p(enable);
        }
    }

    public int getWifiEnabledState() {
        sendBehavior(BehaviorId.WIFI_GETWIFIENABLESTATE);
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getWifiEnabledState uid=%").c((long) Binder.getCallingUid()).flush();
        }
        return this.mWifiStateMachine.syncGetWifiState();
    }

    public int getWifiApEnabledState() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getWifiApEnabledState uid=%").c((long) Binder.getCallingUid()).flush();
        }
        MutableInt apState = new MutableInt(11);
        this.mClientHandler.runWithScissors(new -$$Lambda$WifiServiceImpl$Tk4v3H_jLeO4POzFwYzi9LRyPtE(this, apState), 4000);
        return apState.value;
    }

    public void updateInterfaceIpState(String ifaceName, int mode) {
        enforceNetworkStackPermission();
        this.mLog.info("updateInterfaceIpState uid=%").c((long) Binder.getCallingUid()).flush();
        this.mClientHandler.post(new -$$Lambda$WifiServiceImpl$UQ9JbF5sXBV77FhG4oE7wjNFgek(this, ifaceName, mode));
    }

    /* JADX WARNING: Missing block: B:28:0x009e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateInterfaceIpStateInternal(String ifaceName, int mode) {
        synchronized (this.mLocalOnlyHotspotRequests) {
            Integer previousMode = Integer.valueOf(-1);
            if (ifaceName != null) {
                previousMode = (Integer) this.mIfaceIpModes.put(ifaceName, Integer.valueOf(mode));
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateInterfaceIpState: ifaceName=");
            stringBuilder.append(ifaceName);
            stringBuilder.append(" mode=");
            stringBuilder.append(mode);
            stringBuilder.append(" previous mode= ");
            stringBuilder.append(previousMode);
            Slog.d(str, stringBuilder.toString());
            switch (mode) {
                case -1:
                    if (ifaceName == null) {
                        this.mIfaceIpModes.clear();
                        return;
                    }
                    break;
                case 0:
                    Slog.d(TAG, "IP mode config error - need to clean up");
                    if (this.mLocalOnlyHotspotRequests.isEmpty()) {
                        Slog.d(TAG, "no LOHS requests, stop softap");
                        stopSoftAp();
                    } else {
                        Slog.d(TAG, "we have LOHS requests, clean them up");
                        sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(2);
                    }
                    updateInterfaceIpStateInternal(null, -1);
                    break;
                case 1:
                    sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(3);
                    break;
                case 2:
                    if (!this.mLocalOnlyHotspotRequests.isEmpty()) {
                        sendHotspotStartedMessageToAllLOHSRequestInfoEntriesLocked();
                        break;
                    }
                    stopSoftAp();
                    updateInterfaceIpStateInternal(null, -1);
                    return;
                default:
                    this.mLog.warn("updateInterfaceIpStateInternal: unknown mode %").c((long) mode).flush();
                    break;
            }
        }
    }

    public boolean startSoftAp(WifiConfiguration wifiConfig) {
        boolean startSoftApInternal;
        enforceNetworkStackPermission();
        this.mLog.info("startSoftAp uid=%").c((long) Binder.getCallingUid()).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            if (!this.mLocalOnlyHotspotRequests.isEmpty()) {
                stopSoftApInternal();
            }
            startSoftApInternal = startSoftApInternal(wifiConfig, 1);
        }
        return startSoftApInternal;
    }

    private boolean startSoftApInternal(WifiConfiguration wifiConfig, int mode) {
        this.mLog.trace("startSoftApInternal uid=% mode=%").c((long) Binder.getCallingUid()).c((long) mode).flush();
        if (wifiConfig == null || WifiApConfigStore.validateApWifiConfiguration(wifiConfig)) {
            this.mWifiController.sendMessage(155658, 1, 0, new SoftApModeConfiguration(mode, wifiConfig));
            return true;
        }
        Slog.e(TAG, "Invalid WifiConfiguration");
        return false;
    }

    public boolean stopSoftAp() {
        boolean stopSoftApInternal;
        enforceNetworkStackPermission();
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopSoftAp: pid=");
        stringBuilder.append(pid);
        stringBuilder.append(", uid=");
        stringBuilder.append(uid);
        stringBuilder.append(", name=");
        stringBuilder.append(getAppName(pid));
        Slog.d(str, stringBuilder.toString());
        this.mLog.info("stopSoftAp uid=%").c((long) Binder.getCallingUid()).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            if (!this.mLocalOnlyHotspotRequests.isEmpty()) {
                this.mLog.trace("Call to stop Tethering while LOHS is active, Registered LOHS callers will be updated when softap stopped.").flush();
            }
            stopSoftApInternal = stopSoftApInternal();
        }
        return stopSoftApInternal;
    }

    private boolean stopSoftApInternal() {
        this.mLog.trace("stopSoftApInternal uid=%").c((long) Binder.getCallingUid()).flush();
        this.mWifiController.sendMessage(155658, 0, 0);
        return true;
    }

    public void registerSoftApCallback(final IBinder binder, ISoftApCallback callback, final int callbackIdentifier) {
        if (binder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (callback != null) {
            enforceNetworkSettingsPermission();
            if (this.mVerboseLoggingEnabled) {
                this.mLog.info("registerSoftApCallback uid=%").c((long) Binder.getCallingUid()).flush();
            }
            try {
                binder.linkToDeath(new DeathRecipient() {
                    public void binderDied() {
                        binder.unlinkToDeath(this, 0);
                        WifiServiceImpl.this.mClientHandler.post(new -$$Lambda$WifiServiceImpl$10$HF_QbnVn2k_uya8xeHeyxS3R9NY(this, callbackIdentifier));
                    }
                }, 0);
                this.mClientHandler.post(new -$$Lambda$WifiServiceImpl$Zd1sHIg7rJfJmwY_51xkiXQGMAI(this, callbackIdentifier, callback));
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error on linkToDeath - ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        } else {
            throw new IllegalArgumentException("Callback must not be null");
        }
    }

    public static /* synthetic */ void lambda$registerSoftApCallback$3(WifiServiceImpl wifiServiceImpl, int callbackIdentifier, ISoftApCallback callback) {
        wifiServiceImpl.mRegisteredSoftApCallbacks.put(Integer.valueOf(callbackIdentifier), callback);
        String str;
        StringBuilder stringBuilder;
        if (wifiServiceImpl.mRegisteredSoftApCallbacks.size() > 20) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Too many soft AP callbacks: ");
            stringBuilder.append(wifiServiceImpl.mRegisteredSoftApCallbacks.size());
            Log.wtf(str, stringBuilder.toString());
        } else if (wifiServiceImpl.mRegisteredSoftApCallbacks.size() > 10) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Too many soft AP callbacks: ");
            stringBuilder.append(wifiServiceImpl.mRegisteredSoftApCallbacks.size());
            Log.w(str, stringBuilder.toString());
        }
        try {
            callback.onStateChanged(wifiServiceImpl.mSoftApState, 0);
            callback.onNumClientsChanged(wifiServiceImpl.mSoftApNumClients);
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("registerSoftApCallback: remote exception -- ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
        }
    }

    public void unregisterSoftApCallback(int callbackIdentifier) {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("unregisterSoftApCallback uid=%").c((long) Binder.getCallingUid()).flush();
        }
        this.mClientHandler.post(new -$$Lambda$WifiServiceImpl$RmshU723eQairQK6HNmdtEWCoRA(this, callbackIdentifier));
    }

    private void handleWifiApStateChange(int currentState, int previousState, int errorCode, String ifaceName, int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleWifiApStateChange: currentState=");
        stringBuilder.append(currentState);
        stringBuilder.append(" previousState=");
        stringBuilder.append(previousState);
        stringBuilder.append(" errorCode= ");
        stringBuilder.append(errorCode);
        stringBuilder.append(" ifaceName=");
        stringBuilder.append(ifaceName);
        stringBuilder.append(" mode=");
        stringBuilder.append(mode);
        Slog.d(str, stringBuilder.toString());
        if (currentState == 14) {
            synchronized (this.mLocalOnlyHotspotRequests) {
                int errorToReport = 2;
                if (errorCode == 1) {
                    errorToReport = 1;
                }
                sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(errorToReport);
                updateInterfaceIpStateInternal(null, -1);
            }
        } else if (currentState == 10 || currentState == 11) {
            synchronized (this.mLocalOnlyHotspotRequests) {
                if (this.mIfaceIpModes.contains(Integer.valueOf(2))) {
                    sendHotspotStoppedMessageToAllLOHSRequestInfoEntriesLocked();
                } else {
                    sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(2);
                }
                updateInterfaceIpState(null, -1);
            }
        }
    }

    @GuardedBy("mLocalOnlyHotspotRequests")
    private void sendHotspotFailedMessageToAllLOHSRequestInfoEntriesLocked(int arg1) {
        for (LocalOnlyHotspotRequestInfo requestor : this.mLocalOnlyHotspotRequests.values()) {
            try {
                requestor.sendHotspotFailedMessage(arg1);
                requestor.unlinkDeathRecipient();
            } catch (RemoteException e) {
            }
        }
        this.mLocalOnlyHotspotRequests.clear();
    }

    @GuardedBy("mLocalOnlyHotspotRequests")
    private void sendHotspotStoppedMessageToAllLOHSRequestInfoEntriesLocked() {
        for (LocalOnlyHotspotRequestInfo requestor : this.mLocalOnlyHotspotRequests.values()) {
            try {
                requestor.sendHotspotStoppedMessage();
                requestor.unlinkDeathRecipient();
            } catch (RemoteException e) {
            }
        }
        this.mLocalOnlyHotspotRequests.clear();
    }

    @GuardedBy("mLocalOnlyHotspotRequests")
    private void sendHotspotStartedMessageToAllLOHSRequestInfoEntriesLocked() {
        for (LocalOnlyHotspotRequestInfo requestor : this.mLocalOnlyHotspotRequests.values()) {
            try {
                requestor.sendHotspotStartedMessage(this.mLocalOnlyHotspotConfig);
            } catch (RemoteException e) {
            }
        }
    }

    @VisibleForTesting
    void registerLOHSForTest(int pid, LocalOnlyHotspotRequestInfo request) {
        this.mLocalOnlyHotspotRequests.put(Integer.valueOf(pid), request);
    }

    public int startLocalOnlyHotspot(Messenger messenger, IBinder binder, String packageName) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (enforceChangePermission(packageName) != 0) {
            return 2;
        }
        enforceLocationPermission(packageName, uid);
        if (this.mSettingsStore.getLocationModeSetting(this.mContext) == 0) {
            throw new SecurityException("Location mode is not enabled.");
        } else if (this.mUserManager.hasUserRestriction("no_config_tethering")) {
            return 4;
        } else {
            try {
                if (!this.mFrameworkFacade.isAppForeground(uid)) {
                    return 3;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startLocalOnlyHotspot: pid=");
                stringBuilder.append(pid);
                stringBuilder.append(", uid=");
                stringBuilder.append(uid);
                stringBuilder.append(", packageName=");
                stringBuilder.append(packageName);
                Slog.d(str, stringBuilder.toString());
                this.mLog.info("startLocalOnlyHotspot uid=% pid=%").c((long) uid).c((long) pid).flush();
                synchronized (this.mLocalOnlyHotspotRequests) {
                    if (this.mIfaceIpModes.contains(Integer.valueOf(1))) {
                        this.mLog.info("Cannot start localOnlyHotspot when WiFi Tethering is active.").flush();
                        return 3;
                    } else if (((LocalOnlyHotspotRequestInfo) this.mLocalOnlyHotspotRequests.get(Integer.valueOf(pid))) == null) {
                        LocalOnlyHotspotRequestInfo request = new LocalOnlyHotspotRequestInfo(binder, messenger, new LocalOnlyRequestorCallback());
                        if (this.mIfaceIpModes.contains(Integer.valueOf(2))) {
                            try {
                                this.mLog.trace("LOHS already up, trigger onStarted callback").flush();
                                request.sendHotspotStartedMessage(this.mLocalOnlyHotspotConfig);
                            } catch (RemoteException e) {
                                return 2;
                            }
                        } else if (this.mLocalOnlyHotspotRequests.isEmpty()) {
                            this.mLocalOnlyHotspotConfig = WifiApConfigStore.generateLocalOnlyHotspotConfig(this.mContext);
                            startSoftApInternal(this.mLocalOnlyHotspotConfig, 2);
                        }
                        this.mLocalOnlyHotspotRequests.put(Integer.valueOf(pid), request);
                        return 0;
                    } else {
                        this.mLog.trace("caller already has an active request").flush();
                        throw new IllegalStateException("Caller already has an active LocalOnlyHotspot request");
                    }
                }
            } catch (RemoteException e2) {
                this.mLog.warn("RemoteException during isAppForeground when calling startLOHS").flush();
                return 3;
            }
        }
    }

    public void stopLocalOnlyHotspot() {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopLocalOnlyHotspot: pid=");
        stringBuilder.append(pid);
        stringBuilder.append(", uid=");
        stringBuilder.append(uid);
        stringBuilder.append(", name=");
        stringBuilder.append(getAppName(pid));
        Slog.d(str, stringBuilder.toString());
        this.mLog.info("stopLocalOnlyHotspot uid=% pid=%").c((long) uid).c((long) pid).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            LocalOnlyHotspotRequestInfo requestInfo = (LocalOnlyHotspotRequestInfo) this.mLocalOnlyHotspotRequests.get(Integer.valueOf(pid));
            if (requestInfo == null) {
                return;
            }
            requestInfo.unlinkDeathRecipient();
            unregisterCallingAppAndStopLocalOnlyHotspot(requestInfo);
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0054, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void unregisterCallingAppAndStopLocalOnlyHotspot(LocalOnlyHotspotRequestInfo request) {
        this.mLog.trace("unregisterCallingAppAndStopLocalOnlyHotspot pid=%").c((long) request.getPid()).flush();
        synchronized (this.mLocalOnlyHotspotRequests) {
            if (this.mLocalOnlyHotspotRequests.remove(Integer.valueOf(request.getPid())) == null) {
                this.mLog.trace("LocalOnlyHotspotRequestInfo not found to remove").flush();
            } else if (this.mLocalOnlyHotspotRequests.isEmpty()) {
                this.mLocalOnlyHotspotConfig = null;
                updateInterfaceIpStateInternal(null, -1);
                long identity = Binder.clearCallingIdentity();
                try {
                    stopSoftApInternal();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    public void startWatchLocalOnlyHotspot(Messenger messenger, IBinder binder) {
        enforceNetworkSettingsPermission();
        throw new UnsupportedOperationException("LocalOnlyHotspot is still in development");
    }

    public void stopWatchLocalOnlyHotspot() {
        enforceNetworkSettingsPermission();
        throw new UnsupportedOperationException("LocalOnlyHotspot is still in development");
    }

    public WifiConfiguration getWifiApConfiguration() {
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        StringBuilder stringBuilder;
        if (this.mWifiPermissionsUtil.checkConfigOverridePermission(uid)) {
            this.mLog.info("getWifiApConfiguration uid=%").c((long) uid).flush();
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getWifiApConfiguration, uid=");
            stringBuilder.append(uid);
            Log.d(str, stringBuilder.toString());
            return this.mWifiApConfigStore.getApConfiguration();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("App not allowed to read or update stored WiFi Ap config (uid = ");
        stringBuilder.append(uid);
        stringBuilder.append(")");
        throw new SecurityException(stringBuilder.toString());
    }

    public boolean setWifiApConfiguration(WifiConfiguration wifiConfig, String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        int uid = Binder.getCallingUid();
        StringBuilder stringBuilder;
        if (this.mWifiPermissionsUtil.checkConfigOverridePermission(uid)) {
            this.mLog.info("setWifiApConfiguration uid=%").c((long) uid).flush();
            if (wifiConfig == null) {
                return false;
            }
            if (WifiApConfigStore.validateApWifiConfiguration(wifiConfig)) {
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("apBand set to ");
                stringBuilder.append(wifiConfig.apBand);
                Log.d(str, stringBuilder.toString());
                this.mWifiApConfigStore.setApConfiguration(wifiConfig);
                return true;
            }
            Slog.e(TAG, "Invalid WifiConfiguration");
            return false;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("App not allowed to read or update stored WiFi AP config (uid = ");
        stringBuilder.append(uid);
        stringBuilder.append(")");
        throw new SecurityException(stringBuilder.toString());
    }

    public boolean isScanAlwaysAvailable() {
        sendBehavior(BehaviorId.WIFI_ISSACNALWAYSAVAILABLE);
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("isScanAlwaysAvailable uid=%").c((long) Binder.getCallingUid()).flush();
        }
        return this.mSettingsStore.isScanAlwaysAvailable();
    }

    public void disconnect(String packageName) {
        sendBehavior(BehaviorId.WIFI_DISCONNECT);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disconnect:  pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid=");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", name=");
        stringBuilder.append(getAppName(Binder.getCallingPid()));
        Slog.d(str, stringBuilder.toString());
        if (enforceChangePermission(packageName) == 0) {
            this.mLog.info("disconnect uid=%").c((long) Binder.getCallingUid()).flush();
            this.mWifiStateMachine.disconnectCommand();
            if (this.mHwWifiCHRService != null) {
                this.mHwWifiCHRService.updateApkChangewWifiStatus(8, getAppName(Binder.getCallingPid()));
            }
        }
    }

    public void reconnect(String packageName) {
        sendBehavior(BehaviorId.WIFI_RECONNECT);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("reconnect:  pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid=");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", name=");
        stringBuilder.append(getAppName(Binder.getCallingPid()));
        Slog.d(str, stringBuilder.toString());
        if (enforceChangePermission(packageName) == 0) {
            this.mLog.info("reconnect uid=%").c((long) Binder.getCallingUid()).flush();
            this.mWifiStateMachine.reconnectCommand(new WorkSource(Binder.getCallingUid()));
            if (this.mHwWifiCHRService != null) {
                this.mHwWifiCHRService.updateApkChangewWifiStatus(6, getAppName(Binder.getCallingPid()));
            }
        }
    }

    public void reassociate(String packageName) {
        sendBehavior(BehaviorId.WIFI_REASOCIATE);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("reassociate:  pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid=");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", name=");
        stringBuilder.append(getAppName(Binder.getCallingPid()));
        Slog.d(str, stringBuilder.toString());
        if (enforceChangePermission(packageName) == 0) {
            this.mLog.info("reassociate uid=%").c((long) Binder.getCallingUid()).flush();
            this.mWifiStateMachine.reassociateCommand();
            if (this.mHwWifiCHRService != null) {
                this.mHwWifiCHRService.updateApkChangewWifiStatus(7, getAppName(Binder.getCallingPid()));
            }
        }
    }

    public int getSupportedFeatures() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getSupportedFeatures uid=%").c((long) Binder.getCallingUid()).flush();
        }
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncGetSupportedFeatures(this.mWifiStateMachineChannel);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return 0;
    }

    public void requestActivityInfo(ResultReceiver result) {
        Bundle bundle = new Bundle();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("requestActivityInfo uid=%").c((long) Binder.getCallingUid()).flush();
        }
        bundle.putParcelable("controller_activity", reportActivityInfo());
        result.send(0, bundle);
    }

    public WifiActivityEnergyInfo reportActivityInfo() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("reportActivityInfo uid=%").c((long) Binder.getCallingUid()).flush();
        }
        if ((getSupportedFeatures() & 65536) == 0) {
            return null;
        }
        WifiActivityEnergyInfo energyInfo = null;
        WifiActivityEnergyInfo energyInfo2;
        if (this.mWifiStateMachineChannel != null) {
            WifiLinkLayerStats stats = this.mWifiStateMachine.syncGetLinkLayerStats(this.mWifiStateMachineChannel);
            if (stats != null) {
                long[] txTimePerLevel;
                long[] txTimePerLevel2;
                double rxIdleCurrent = this.mPowerProfile.getAveragePower("wifi.controller.idle");
                double rxCurrent = this.mPowerProfile.getAveragePower("wifi.controller.rx");
                double txCurrent = this.mPowerProfile.getAveragePower("wifi.controller.tx");
                double voltage = this.mPowerProfile.getAveragePower("wifi.controller.voltage") / 1000.0d;
                long rxIdleTime = (long) ((stats.on_time - stats.tx_time) - stats.rx_time);
                int i = 0;
                if (stats.tx_time_per_level != null) {
                    txTimePerLevel = new long[stats.tx_time_per_level.length];
                    while (i < txTimePerLevel.length) {
                        energyInfo2 = energyInfo;
                        txTimePerLevel[i] = (long) stats.tx_time_per_level[i];
                        i++;
                        energyInfo = energyInfo2;
                    }
                } else {
                    energyInfo2 = null;
                    txTimePerLevel = new long[0];
                }
                long[] txTimePerLevel3 = txTimePerLevel;
                long energyUsed = (long) ((((((double) stats.tx_time) * txCurrent) + (((double) stats.rx_time) * rxCurrent)) + (((double) rxIdleTime) * rxIdleCurrent)) * voltage);
                if (rxIdleTime < 0 || stats.on_time < 0 || stats.tx_time < 0 || stats.rx_time < 0 || stats.on_time_scan < 0 || energyUsed < 0) {
                    StringBuilder sb = new StringBuilder();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" rxIdleCur=");
                    stringBuilder.append(rxIdleCurrent);
                    sb.append(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" rxCur=");
                    stringBuilder.append(rxCurrent);
                    sb.append(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" txCur=");
                    stringBuilder.append(txCurrent);
                    sb.append(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" voltage=");
                    stringBuilder.append(voltage);
                    sb.append(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" on_time=");
                    stringBuilder.append(stats.on_time);
                    sb.append(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" tx_time=");
                    stringBuilder.append(stats.tx_time);
                    sb.append(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" tx_time_per_level=");
                    txTimePerLevel2 = txTimePerLevel3;
                    stringBuilder.append(Arrays.toString(txTimePerLevel2));
                    sb.append(stringBuilder.toString());
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" rx_time=");
                    stringBuilder2.append(stats.rx_time);
                    sb.append(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" rxIdleTime=");
                    stringBuilder2.append(rxIdleTime);
                    sb.append(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" scan_time=");
                    stringBuilder2.append(stats.on_time_scan);
                    sb.append(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" energy=");
                    stringBuilder2.append(energyUsed);
                    sb.append(stringBuilder2.toString());
                    String str = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" reportActivityInfo: ");
                    stringBuilder3.append(sb.toString());
                    Log.d(str, stringBuilder3.toString());
                } else {
                    double d = rxIdleCurrent;
                    txTimePerLevel2 = txTimePerLevel3;
                }
                energyInfo = new WifiActivityEnergyInfo(this.mClock.getElapsedSinceBootMillis(), 3, (long) stats.tx_time, txTimePerLevel2, (long) stats.rx_time, (long) stats.on_time_scan, rxIdleTime, energyUsed);
            } else {
                energyInfo2 = null;
                WifiLinkLayerStats wifiLinkLayerStats = stats;
            }
            if (energyInfo == null || !energyInfo.isValid()) {
                return null;
            }
            return energyInfo;
        }
        energyInfo2 = null;
        WifiActivityEnergyInfo wifiActivityEnergyInfo = null;
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return wifiActivityEnergyInfo;
    }

    public ParceledListSlice<WifiConfiguration> getConfiguredNetworks() {
        sendBehavior(BehaviorId.WIFI_GETCONFIGUREDNETWORKS);
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getConfiguredNetworks uid=%").c((long) Binder.getCallingUid()).flush();
        }
        if (this.mWifiStateMachineChannel != null) {
            List<WifiConfiguration> configs = this.mWifiStateMachine.syncGetConfiguredNetworks(Binder.getCallingUid(), this.mWifiStateMachineChannel);
            if (configs != null) {
                return new ParceledListSlice(configs);
            }
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public ParceledListSlice<WifiConfiguration> getPrivilegedConfiguredNetworks() {
        enforceReadCredentialPermission();
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getPrivilegedConfiguredNetworks uid=%").c((long) Binder.getCallingUid()).flush();
        }
        if (this.mWifiStateMachineChannel != null) {
            List<WifiConfiguration> configs = this.mWifiStateMachine.syncGetPrivilegedConfiguredNetwork(this.mWifiStateMachineChannel);
            if (configs != null) {
                return new ParceledListSlice(configs);
            }
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return null;
    }

    public WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getMatchingWifiConfig uid=%").c((long) Binder.getCallingUid()).flush();
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            return this.mWifiStateMachine.syncGetMatchingWifiConfig(scanResult, this.mWifiStateMachineChannel);
        }
        throw new UnsupportedOperationException("Passpoint not enabled");
    }

    public List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult) {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getMatchingPasspointConfigurations uid=%").c((long) Binder.getCallingUid()).flush();
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            return this.mWifiStateMachine.getAllMatchingWifiConfigs(scanResult, this.mWifiStateMachineChannel);
        }
        throw new UnsupportedOperationException("Passpoint not enabled");
    }

    public List<OsuProvider> getMatchingOsuProviders(ScanResult scanResult) {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getMatchingOsuProviders uid=%").c((long) Binder.getCallingUid()).flush();
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            return this.mWifiStateMachine.syncGetMatchingOsuProviders(scanResult, this.mWifiStateMachineChannel);
        }
        throw new UnsupportedOperationException("Passpoint not enabled");
    }

    public int addOrUpdateNetwork(WifiConfiguration config, String packageName) {
        sendBehavior(BehaviorId.WIFI_ADDORUPDATENETWORK);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addOrUpdateNetwork, pid:");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid:");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", config:");
        stringBuilder.append(config);
        stringBuilder.append(", name=");
        stringBuilder.append(getAppName(Binder.getCallingPid()));
        Slog.d(str, stringBuilder.toString());
        if (enforceChangePermission(packageName) != 0) {
            return -1;
        }
        this.mLog.info("addOrUpdateNetwork uid=%").c((long) Binder.getCallingUid()).flush();
        if (config.isPasspoint()) {
            PasspointConfiguration passpointConfig = PasspointProvider.convertFromWifiConfig(config);
            if (passpointConfig.getCredential() == null) {
                Slog.e(TAG, "Missing credential for Passpoint profile");
                return -1;
            }
            passpointConfig.getCredential().setCaCertificate(config.enterpriseConfig.getCaCertificate());
            passpointConfig.getCredential().setClientCertificateChain(config.enterpriseConfig.getClientCertificateChain());
            passpointConfig.getCredential().setClientPrivateKey(config.enterpriseConfig.getClientPrivateKey());
            if (addOrUpdatePasspointConfiguration(passpointConfig, packageName)) {
                return 0;
            }
            Slog.e(TAG, "Failed to add Passpoint profile");
            return -1;
        } else if (config != null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" uid = ");
            stringBuilder2.append(Integer.toString(Binder.getCallingUid()));
            stringBuilder2.append(" SSID ");
            stringBuilder2.append(config.SSID);
            stringBuilder2.append(" nid=");
            stringBuilder2.append(Integer.toString(config.networkId));
            Slog.i("addOrUpdateNetwork", stringBuilder2.toString());
            if (config.networkId == -1) {
                config.creatorUid = Binder.getCallingUid();
            } else {
                config.lastUpdateUid = Binder.getCallingUid();
            }
            this.mWifiController.updateWMUserAction(this.mContext, "ACTION_SELECT_WIFINETWORK", getAppName(Binder.getCallingPid()));
            if (this.mWifiStateMachineChannel != null) {
                return this.mWifiStateMachine.syncAddOrUpdateNetwork(this.mWifiStateMachineChannel, config);
            }
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return -1;
        } else {
            Slog.e(TAG, "bad network configuration");
            return -1;
        }
    }

    public static void verifyCert(X509Certificate caCert) throws GeneralSecurityException, IOException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        CertPathValidator validator = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
        CertPath path = factory.generateCertPath(Arrays.asList(new X509Certificate[]{caCert}));
        KeyStore ks = KeyStore.getInstance("AndroidCAStore");
        ks.load(null, null);
        PKIXParameters params = new PKIXParameters(ks);
        params.setRevocationEnabled(false);
        validator.validate(path, params);
    }

    public boolean removeNetwork(int netId, String packageName) {
        sendBehavior(BehaviorId.WIFI_REMOVENETWORK);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeNetwork, pid:");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid:");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", netId:");
        stringBuilder.append(netId);
        stringBuilder.append(", name=");
        stringBuilder.append(getAppName(Binder.getCallingPid()));
        Slog.d(str, stringBuilder.toString());
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        this.mLog.info("removeNetwork uid=%").c((long) Binder.getCallingUid()).flush();
        this.mWifiController.updateWMUserAction(this.mContext, "ACTION_REMOVE_WIFINETWORK", getAppName(Binder.getCallingPid()));
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncRemoveNetwork(this.mWifiStateMachineChannel, netId);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public boolean enableNetwork(int netId, boolean disableOthers, String packageName) {
        sendBehavior(BehaviorId.WIFI_ENABLENETWORK);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableNetwork, pid:");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid:");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", netId:");
        stringBuilder.append(netId);
        stringBuilder.append(", disableOthers:");
        stringBuilder.append(disableOthers);
        stringBuilder.append(", name=");
        stringBuilder.append(getAppName(Binder.getCallingPid()));
        Slog.d(str, stringBuilder.toString());
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        this.mLog.info("enableNetwork uid=% disableOthers=%").c((long) Binder.getCallingUid()).c(disableOthers).flush();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncEnableNetwork(this.mWifiStateMachineChannel, netId, disableOthers);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public boolean disableNetwork(int netId, String packageName) {
        sendBehavior(BehaviorId.WIFI_DISABLENETWORK);
        String appName = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disableNetwork, pid:");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid:");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", netId:");
        stringBuilder.append(netId);
        stringBuilder.append(", name=");
        stringBuilder.append(appName);
        Slog.d(str, stringBuilder.toString());
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        this.mHwWifiCHRService = HwWifiServiceFactory.getHwWifiCHRService();
        if (this.mHwWifiCHRService != null) {
            this.mHwWifiCHRService.updateApkChangewWifiStatus(2, appName);
        }
        this.mLog.info("disableNetwork uid=%").c((long) Binder.getCallingUid()).flush();
        if (this.mWifiStateMachineChannel != null) {
            return this.mWifiStateMachine.syncDisableNetwork(this.mWifiStateMachineChannel, netId);
        }
        Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
        return false;
    }

    public WifiInfo getConnectionInfo(String callingPackage) {
        WifiInfo result;
        boolean hideDefaultMacAddress;
        boolean hideBssidAndSsid;
        sendBehavior(BehaviorId.WIFI_GETCONNECTIONINFO);
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getConnectionInfo uid=%").c((long) uid).flush();
        }
        long ident = Binder.clearCallingIdentity();
        try {
            result = this.mWifiStateMachine.syncRequestConnectionInfo(callingPackage, uid);
            hideDefaultMacAddress = true;
            hideBssidAndSsid = true;
            if (this.mWifiInjector.getWifiPermissionsWrapper().getLocalMacAddressPermission(uid) == 0) {
                hideDefaultMacAddress = false;
            }
            this.mWifiPermissionsUtil.enforceCanAccessScanResults(callingPackage, uid);
            hideBssidAndSsid = false;
        } catch (RemoteException e) {
            Log.e(TAG, "Error checking receiver permission", e);
        } catch (SecurityException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enforceCanAccessScanResults: hiding ssid and bssid");
            stringBuilder.append(e2.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
        if (hideDefaultMacAddress) {
            result.setMacAddress("02:00:00:00:00:00");
        }
        if (hideBssidAndSsid) {
            result.setBSSID("02:00:00:00:00:00");
            result.setSSID(WifiSsid.createFromHex(null));
        }
        if (this.mVerboseLoggingEnabled && (hideBssidAndSsid || hideDefaultMacAddress)) {
            WifiLog wifiLog = this.mLog;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getConnectionInfo: hideBssidAndSSid=");
            stringBuilder2.append(hideBssidAndSsid);
            stringBuilder2.append(", hideDefaultMacAddress=");
            stringBuilder2.append(hideDefaultMacAddress);
            wifiLog.v(stringBuilder2.toString());
        }
        Binder.restoreCallingIdentity(ident);
        return result;
    }

    private ScanResult[] filterScanProxyResultsByAge(List<ScanResult> scanResultsList, List<ScanResult> CachedScanResultsList) {
        ScanResult[] filterScanProxyScanResults = (ScanResult[]) scanResultsList.stream().filter(new -$$Lambda$WifiServiceImpl$96A4u2hMi66fUsU9psWDkWSSwro(this.mClock.getElapsedSinceBootMillis())).toArray(-$$Lambda$WifiServiceImpl$BwofqEhGlhLBNFHuJX2d0T-lzyA.INSTANCE);
        if (filterScanProxyScanResults.length == 0) {
            return (ScanResult[]) CachedScanResultsList.toArray(new ScanResult[CachedScanResultsList.size()]);
        }
        return filterScanProxyScanResults;
    }

    static /* synthetic */ boolean lambda$filterScanProxyResultsByAge$5(long currentTimeInMillis, ScanResult ScanResult) {
        return currentTimeInMillis - (ScanResult.timestamp / 1000) < 30000;
    }

    public List<ScanResult> getScanResults(String callingPackage) {
        List<ScanResult> list;
        sendBehavior(BehaviorId.WIFI_GETSCANRESULTS);
        enforceAccessPermission();
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        if (this.mVerboseLoggingEnabled) {
            list = (long) uid;
            this.mLog.info("getScanResults uid=%").c((long) list).flush();
        }
        try {
            this.mWifiPermissionsUtil.enforceCanAccessScanResults(callingPackage, uid);
            List<ScanResult> scanResults = new ArrayList();
            list = this.mWifiInjector.getWifiStateMachineHandler().runWithScissors(new -$$Lambda$WifiServiceImpl$2ZawY3HKMGxYuJvvAb04rbHcj8k(this, scanResults), 4000);
            if (!list == true) {
                Log.e(TAG, "Failed to post runnable to fetch scan results");
            }
            List<ScanResult> scanResultsList = scanResults;
            if (1000 == uid) {
                scanResultsList = Arrays.asList(filterScanProxyResultsByAge(scanResults, this.mWifiInjector.getWifiScanner().getSingleScanResults()));
            }
            Collections.sort(scanResultsList, new Comparator<ScanResult>() {
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ScanResults exceed the max count. size = ");
                stringBuilder.append(scanResultsList.size());
                Log.d(str, stringBuilder.toString());
                scanResultsList = scanResultsList.subList(0, 200);
            }
            logScanResultsListRestrictively(callingPackage, scanResultsList);
            return scanResultsList;
        } catch (SecurityException e) {
            list = new ArrayList();
            return list;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void logScanResultsListRestrictively(String callingPackage, List<ScanResult> scanResultsList) {
        long currentLogTime = this.mClock.getElapsedSinceBootMillis();
        if ("com.android.settings".equals(callingPackage) && scanResultsList != null && currentLogTime - this.mLastLogScanResultsTime > 3000) {
            Set<String> ssids = new HashSet();
            StringBuilder sb = new StringBuilder();
            for (ScanResult scanResult : scanResultsList) {
                String ssid = scanResult.SSID;
                if (!ssids.contains(ssid)) {
                    ssids.add(ssid);
                    sb.append(ssid);
                    sb.append("|");
                    sb.append(scanResult.isHiLinkNetwork);
                    sb.append("|");
                    sb.append(scanResult.dot11vNetwork);
                    sb.append(" ");
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getScanResults: calling by ");
            stringBuilder.append(callingPackage);
            stringBuilder.append("   includes: ");
            stringBuilder.append(sb.toString());
            Log.d(str, stringBuilder.toString());
            this.mLastLogScanResultsTime = currentLogTime;
        }
    }

    public boolean addOrUpdatePasspointConfiguration(PasspointConfiguration config, String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        this.mLog.info("addorUpdatePasspointConfiguration uid=%").c((long) Binder.getCallingUid()).flush();
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            return this.mWifiStateMachine.syncAddOrUpdatePasspointConfig(this.mWifiStateMachineChannel, config, Binder.getCallingUid());
        }
        throw new UnsupportedOperationException("Passpoint not enabled");
    }

    public boolean removePasspointConfiguration(String fqdn, String packageName) {
        if (enforceChangePermission(packageName) != 0) {
            return false;
        }
        this.mLog.info("removePasspointConfiguration uid=%").c((long) Binder.getCallingUid()).flush();
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            return this.mWifiStateMachine.syncRemovePasspointConfig(this.mWifiStateMachineChannel, fqdn);
        }
        throw new UnsupportedOperationException("Passpoint not enabled");
    }

    public List<PasspointConfiguration> getPasspointConfigurations() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getPasspointConfigurations uid=%").c((long) Binder.getCallingUid()).flush();
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            return this.mWifiStateMachine.syncGetPasspointConfigs(this.mWifiStateMachineChannel);
        }
        throw new UnsupportedOperationException("Passpoint not enabled");
    }

    public void queryPasspointIcon(long bssid, String fileName) {
        enforceAccessPermission();
        this.mLog.info("queryPasspointIcon uid=%").c((long) Binder.getCallingUid()).flush();
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
            this.mWifiStateMachine.syncQueryPasspointIcon(this.mWifiStateMachineChannel, bssid, fileName);
            return;
        }
        throw new UnsupportedOperationException("Passpoint not enabled");
    }

    public int matchProviderWithCurrentNetwork(String fqdn) {
        this.mLog.info("matchProviderWithCurrentNetwork uid=%").c((long) Binder.getCallingUid()).flush();
        return this.mWifiStateMachine.matchProviderWithCurrentNetwork(this.mWifiStateMachineChannel, fqdn);
    }

    public void deauthenticateNetwork(long holdoff, boolean ess) {
        this.mLog.info("deauthenticateNetwork uid=%").c((long) Binder.getCallingUid()).flush();
        this.mWifiStateMachine.deauthenticateNetwork(this.mWifiStateMachineChannel, holdoff, ess);
    }

    public void setCountryCode(String countryCode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WifiService trying to set country code to ");
        stringBuilder.append(countryCode);
        Slog.i(str, stringBuilder.toString());
        enforceConnectivityInternalPermission();
        this.mLog.info("setCountryCode uid=%").c((long) Binder.getCallingUid()).flush();
        long token = Binder.clearCallingIdentity();
        this.mCountryCode.setCountryCode(countryCode);
        Binder.restoreCallingIdentity(token);
    }

    public String getCountryCode() {
        enforceConnectivityInternalPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getCountryCode uid=%").c((long) Binder.getCallingUid()).flush();
        }
        return this.mCountryCode.getCountryCode();
    }

    public boolean isDualBandSupported() {
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("isDualBandSupported uid=%").c((long) Binder.getCallingUid()).flush();
        }
        int value = SystemProperties.getInt("ro.config.hw_wifi_ap_band", 0);
        if (value == 1) {
            return false;
        }
        if (value == 2) {
            return true;
        }
        return this.mContext.getResources().getBoolean(17957074);
    }

    public boolean needs5GHzToAnyApBandConversion() {
        enforceNetworkSettingsPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("needs5GHzToAnyApBandConversion uid=%").c((long) Binder.getCallingUid()).flush();
        }
        return this.mContext.getResources().getBoolean(17957073);
    }

    @Deprecated
    public DhcpInfo getDhcpInfo() {
        sendBehavior(BehaviorId.WIFI_GETPHCPINFO);
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getDhcpInfo uid=%").c((long) Binder.getCallingUid()).flush();
        }
        DhcpResults dhcpResults = this.mWifiStateMachine.syncGetDhcpResults();
        DhcpInfo info = new DhcpInfo();
        if (dhcpResults.ipAddress != null && (dhcpResults.ipAddress.getAddress() instanceof Inet4Address)) {
            info.ipAddress = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResults.ipAddress.getAddress());
            info.netmask = NetworkUtils.prefixLengthToNetmaskInt(dhcpResults.ipAddress.getPrefixLength());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("netmask =");
            stringBuilder.append(info.netmask);
            Log.d("wifiserviceimpl", stringBuilder.toString());
        }
        if (dhcpResults.gateway != null && (dhcpResults.gateway instanceof Inet4Address)) {
            info.gateway = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResults.gateway);
        }
        int dnsFound = 0;
        Iterator it = dhcpResults.dnsServers.iterator();
        while (it.hasNext()) {
            InetAddress dns = (InetAddress) it.next();
            if (dns instanceof Inet4Address) {
                if (dnsFound == 0) {
                    info.dns1 = NetworkUtils.inetAddressToInt((Inet4Address) dns);
                } else {
                    info.dns2 = NetworkUtils.inetAddressToInt((Inet4Address) dns);
                }
                dnsFound++;
                if (dnsFound > 1) {
                    break;
                }
            }
        }
        Inet4Address serverAddress = dhcpResults.serverAddress;
        if (serverAddress != null) {
            info.serverAddress = NetworkUtils.inetAddressToInt(serverAddress);
        }
        info.leaseDuration = dhcpResults.leaseDuration;
        return info;
    }

    public void enableTdls(String remoteAddress, boolean enable) {
        if (remoteAddress != null) {
            this.mLog.info("enableTdls uid=% enable=%").c((long) Binder.getCallingUid()).c(enable).flush();
            TdlsTaskParams params = new TdlsTaskParams();
            params.remoteIpAddress = remoteAddress;
            params.enable = enable;
            new TdlsTask().execute(new TdlsTaskParams[]{params});
            return;
        }
        throw new IllegalArgumentException("remoteAddress cannot be null");
    }

    public void enableTdlsWithMacAddress(String remoteMacAddress, boolean enable) {
        this.mLog.info("enableTdlsWithMacAddress uid=% enable=%").c((long) Binder.getCallingUid()).c(enable).flush();
        if (remoteMacAddress != null) {
            this.mWifiStateMachine.enableTdls(remoteMacAddress, enable);
            return;
        }
        throw new IllegalArgumentException("remoteMacAddress cannot be null");
    }

    public Messenger getWifiServiceMessenger(String packageName) throws RemoteException {
        enforceAccessPermission();
        if (enforceChangePermission(packageName) == 0) {
            this.mLog.info("getWifiServiceMessenger uid=%").c((long) Binder.getCallingUid()).flush();
            return new Messenger(this.mClientHandler);
        }
        throw new SecurityException("Could not create wifi service messenger");
    }

    public void disableEphemeralNetwork(String SSID, String packageName) {
        enforceAccessPermission();
        if (enforceChangePermission(packageName) == 0) {
            this.mLog.info("disableEphemeralNetwork uid=%").c((long) Binder.getCallingUid()).flush();
            this.mWifiStateMachine.disableEphemeralNetwork(SSID);
        }
    }

    private boolean startConsentUi(String packageName, int callingUid, String intentAction) throws RemoteException {
        if (UserHandle.getAppId(callingUid) == 1000 || checkWifiPermissionWhenPermissionReviewRequired()) {
            return false;
        }
        try {
            if (this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 268435456, UserHandle.getUserId(callingUid)).uid == callingUid) {
                Intent intent = new Intent(intentAction);
                intent.addFlags(276824064);
                intent.putExtra("android.intent.extra.PACKAGE_NAME", packageName);
                this.mContext.startActivity(intent);
                return true;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" not in uid ");
            stringBuilder.append(callingUid);
            throw new SecurityException(stringBuilder.toString());
        } catch (NameNotFoundException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private void registerForScanModeChange() {
        this.mFrameworkFacade.registerContentObserver(this.mContext, Global.getUriFor("wifi_scan_always_enabled"), false, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                WifiServiceImpl.this.mSettingsStore.handleWifiScanAlwaysAvailableToggled();
                WifiServiceImpl.this.mWifiController.sendMessage(155655);
            }
        });
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        if (this.mContext.getResources().getBoolean(17957075)) {
            intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        }
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        intentFilter.addAction("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
        if (this.mContext.getResources().getBoolean(17957088)) {
            intentFilter.addAction("android.intent.action.EMERGENCY_CALL_STATE_CHANGED");
        }
        registerForBroadcastsEx(intentFilter);
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.PACKAGE_FULLY_REMOVED")) {
                    int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                    Uri uri = intent.getData();
                    if (uid != -1 && uri != null) {
                        String pkgName = uri.getSchemeSpecificPart();
                        WifiServiceImpl.this.mWifiStateMachine.removeAppConfigs(pkgName, uid);
                        WifiServiceImpl.this.mWifiStateMachineHandler.post(new -$$Lambda$WifiServiceImpl$15$bcYgGZKA_iNmZg53oTMy98qRxpY(this, pkgName, uid));
                    }
                }
            }
        }, intentFilter);
        this.mContext.registerReceiverAsUser(this.mPackageOrUserReceiver, UserHandle.ALL, intentFilter, null, null);
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new WifiShellCommand(this.mWifiStateMachine).exec(this, in, out, err, args, callback, resultReceiver);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Permission Denial: can't dump WifiService from from pid=");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", uid=");
            stringBuilder.append(Binder.getCallingUid());
            pw.println(stringBuilder.toString());
            return;
        }
        WifiScoreReport wifiScoreReport;
        if (args != null && args.length > 0 && WifiMetrics.PROTO_DUMP_ARG.equals(args[0])) {
            this.mWifiStateMachine.updateWifiMetrics();
            this.mWifiMetrics.dump(fd, pw, args);
        } else if (args != null && args.length > 0 && "ipclient".equals(args[0])) {
            String[] ipClientArgs = new String[(args.length - 1)];
            System.arraycopy(args, 1, ipClientArgs, 0, ipClientArgs.length);
            this.mWifiStateMachine.dumpIpClient(fd, pw, ipClientArgs);
        } else if (args == null || args.length <= 0 || !WifiScoreReport.DUMP_ARG.equals(args[0])) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Wi-Fi is ");
            stringBuilder2.append(this.mWifiStateMachine.syncGetWifiStateByName());
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Verbose logging is ");
            stringBuilder2.append(this.mVerboseLoggingEnabled ? "on" : "off");
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Stay-awake conditions: ");
            stringBuilder2.append(this.mFacade.getIntegerSetting(this.mContext, "stay_on_while_plugged_in", 0));
            pw.println(stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mInIdleMode ");
            stringBuilder.append(this.mInIdleMode);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mScanPending ");
            stringBuilder.append(this.mScanPending);
            pw.println(stringBuilder.toString());
            this.mWifiController.dump(fd, pw, args);
            this.mSettingsStore.dump(fd, pw, args);
            this.mTrafficPoller.dump(fd, pw, args);
            pw.println();
            pw.println("Locks held:");
            this.mWifiLockManager.dump(pw);
            pw.println();
            this.mWifiMulticastLockManager.dump(pw);
            pw.println();
            this.mWifiStateMachinePrime.dump(fd, pw, args);
            pw.println();
            this.mWifiStateMachine.dump(fd, pw, args);
            pw.println();
            this.mWifiStateMachine.updateWifiMetrics();
            this.mWifiMetrics.dump(fd, pw, args);
            pw.println();
            this.mWifiBackupRestore.dump(fd, pw, args);
            pw.println();
            stringBuilder = new StringBuilder();
            stringBuilder.append("ScoringParams: settings put global wifi_score_params ");
            stringBuilder.append(this.mWifiInjector.getScoringParams());
            pw.println(stringBuilder.toString());
            pw.println();
            wifiScoreReport = this.mWifiStateMachine.getWifiScoreReport();
            if (wifiScoreReport != null) {
                pw.println("WifiScoreReport:");
                wifiScoreReport.dump(fd, pw, args);
            }
            pw.println();
        } else {
            wifiScoreReport = this.mWifiStateMachine.getWifiScoreReport();
            if (wifiScoreReport != null) {
                wifiScoreReport.dump(fd, pw, args);
            }
        }
    }

    public boolean acquireWifiLock(IBinder binder, int lockMode, String tag, WorkSource ws) {
        this.mLog.info("acquireWifiLock uid=% lockMode=%").c((long) Binder.getCallingUid()).c((long) lockMode).flush();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("acquireWifiLock, pid:");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid:");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", binder:");
        stringBuilder.append(binder);
        stringBuilder.append(", lockMode:");
        stringBuilder.append(lockMode);
        stringBuilder.append(", tag:");
        stringBuilder.append(tag);
        stringBuilder.append(", ws:");
        stringBuilder.append(ws);
        Slog.d(str, stringBuilder.toString());
        if (this.mWifiLockManager.acquireWifiLock(lockMode, tag, binder, ws)) {
            return true;
        }
        return false;
    }

    public void updateWifiLockWorkSource(IBinder binder, WorkSource ws) {
        this.mLog.info("updateWifiLockWorkSource uid=%").c((long) Binder.getCallingUid()).flush();
        this.mWifiLockManager.updateWifiLockWorkSource(binder, ws);
    }

    public boolean releaseWifiLock(IBinder binder) {
        this.mLog.info("releaseWifiLock uid=%").c((long) Binder.getCallingUid()).flush();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("releaseWifiLock, pid:");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid:");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(", binder:");
        stringBuilder.append(binder);
        Slog.d(str, stringBuilder.toString());
        if (this.mWifiLockManager.releaseWifiLock(binder)) {
            return true;
        }
        return false;
    }

    public void initializeMulticastFiltering() {
        enforceMulticastChangePermission();
        this.mLog.info("initializeMulticastFiltering uid=%").c((long) Binder.getCallingUid()).flush();
        this.mWifiMulticastLockManager.initializeFiltering();
    }

    public void acquireMulticastLock(IBinder binder, String tag) {
        enforceMulticastChangePermission();
        this.mLog.info("acquireMulticastLock uid=%").c((long) Binder.getCallingUid()).flush();
        this.mWifiMulticastLockManager.acquireLock(binder, tag);
    }

    public void releaseMulticastLock() {
        enforceMulticastChangePermission();
        this.mLog.info("releaseMulticastLock uid=%").c((long) Binder.getCallingUid()).flush();
        this.mWifiMulticastLockManager.releaseLock();
    }

    public boolean isMulticastEnabled() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("isMulticastEnabled uid=%").c((long) Binder.getCallingUid()).flush();
        }
        return this.mWifiMulticastLockManager.isMulticastEnabled();
    }

    public void enableVerboseLogging(int verbose) {
        enforceAccessPermission();
        enforceNetworkSettingsPermission();
        this.mLog.info("enableVerboseLogging uid=% verbose=%").c((long) Binder.getCallingUid()).c((long) verbose).flush();
        this.mFacade.setIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", verbose);
        enableVerboseLoggingInternal(verbose);
    }

    void enableVerboseLoggingInternal(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
        this.mWifiStateMachine.enableVerboseLogging(verbose);
        this.mWifiLockManager.enableVerboseLogging(verbose);
        this.mWifiMulticastLockManager.enableVerboseLogging(verbose);
        this.mWifiInjector.enableVerboseLogging(verbose);
    }

    public int getVerboseLoggingLevel() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getVerboseLoggingLevel uid=%").c((long) Binder.getCallingUid()).flush();
        }
        return this.mFacade.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0);
    }

    public void factoryReset(String packageName) {
        enforceConnectivityInternalPermission();
        if (enforceChangePermission(packageName) == 0) {
            this.mLog.info("factoryReset uid=%").c((long) Binder.getCallingUid()).flush();
            if (!this.mUserManager.hasUserRestriction("no_network_reset")) {
                if (!this.mUserManager.hasUserRestriction("no_config_tethering")) {
                    stopSoftApInternal();
                    if (this.mContext != null && (IS_ATT || IS_VERIZON)) {
                        System.putInt(this.mContext.getContentResolver(), SET_SSID_NAME, 1);
                    }
                }
                if (!this.mUserManager.hasUserRestriction("no_config_wifi")) {
                    int i = 0;
                    while (i < 10) {
                        if (this.mWifiStateMachineChannel != null) {
                            List<WifiConfiguration> networks = this.mWifiStateMachine.syncGetConfiguredNetworks(Binder.getCallingUid(), this.mWifiStateMachineChannel);
                            if (networks != null) {
                                for (WifiConfiguration config : networks) {
                                    removeNetwork(config.networkId, packageName);
                                }
                            } else {
                                i++;
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static boolean logAndReturnFalse(String s) {
        Log.d(TAG, s);
        return false;
    }

    public static boolean isValid(WifiConfiguration config) {
        String validity = checkValidity(config);
        return validity == null || logAndReturnFalse(validity);
    }

    public static String checkValidity(WifiConfiguration config) {
        if (config.allowedKeyManagement == null) {
            return "allowed kmgmt";
        }
        if (config.allowedKeyManagement.cardinality() > 1) {
            if (config.allowedKeyManagement.get(10) && config.allowedKeyManagement.get(11)) {
                if (config.allowedKeyManagement.cardinality() != 4) {
                    return "include WAPI_PSK and WAPI_CERT but is still invalid for cardinality != 4";
                }
            } else if (config.allowedKeyManagement.cardinality() != 2) {
                return "invalid for cardinality != 2";
            }
            if (!config.allowedKeyManagement.get(2)) {
                return "not WPA_EAP";
            }
            if (!(config.allowedKeyManagement.get(3) || config.allowedKeyManagement.get(1))) {
                return "not PSK or 8021X";
            }
        }
        if (config.getIpAssignment() == IpAssignment.STATIC) {
            StaticIpConfiguration staticIpConf = config.getStaticIpConfiguration();
            if (staticIpConf == null) {
                return "null StaticIpConfiguration";
            }
            if (staticIpConf.ipAddress == null) {
                return "null static ip Address";
            }
        }
        return null;
    }

    public Network getCurrentNetwork() {
        enforceAccessPermission();
        if (this.mVerboseLoggingEnabled) {
            this.mLog.info("getCurrentNetwork uid=%").c((long) Binder.getCallingUid()).flush();
        }
        return this.mWifiStateMachine.getCurrentNetwork();
    }

    public static String toHexString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        sb.append(s);
        sb.append('\'');
        for (int n = 0; n < s.length(); n++) {
            sb.append(String.format(" %02x", new Object[]{Integer.valueOf(s.charAt(n) & Constants.SHORT_MASK)}));
        }
        return sb.toString();
    }

    public void enableWifiConnectivityManager(boolean enabled) {
        enforceConnectivityInternalPermission();
        this.mLog.info("enableWifiConnectivityManager uid=% enabled=%").c((long) Binder.getCallingUid()).c(enabled).flush();
        this.mWifiStateMachine.enableWifiConnectivityManager(enabled);
    }

    public byte[] retrieveBackupData() {
        enforceNetworkSettingsPermission();
        this.mLog.info("retrieveBackupData uid=%").c((long) Binder.getCallingUid()).flush();
        if (this.mWifiStateMachineChannel == null) {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return null;
        }
        Slog.d(TAG, "Retrieving backup data");
        byte[] backupData = this.mWifiBackupRestore.retrieveBackupDataFromConfigurations(this.mWifiStateMachine.syncGetPrivilegedConfiguredNetwork(this.mWifiStateMachineChannel));
        Slog.d(TAG, "Retrieved backup data");
        return backupData;
    }

    private void restoreNetworks(List<WifiConfiguration> configurations) {
        if (configurations == null) {
            Slog.e(TAG, "Backup data parse failed");
            return;
        }
        for (WifiConfiguration configuration : configurations) {
            int networkId = this.mWifiStateMachine.syncAddOrUpdateNetwork(this.mWifiStateMachineChannel, configuration);
            if (networkId == -1) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Restore network failed: ");
                stringBuilder.append(configuration.configKey());
                Slog.e(str, stringBuilder.toString());
            } else {
                this.mWifiStateMachine.syncEnableNetwork(this.mWifiStateMachineChannel, networkId, false);
            }
        }
    }

    public void restoreBackupData(byte[] data) {
        enforceNetworkSettingsPermission();
        this.mLog.info("restoreBackupData uid=%").c((long) Binder.getCallingUid()).flush();
        if (this.mWifiStateMachineChannel == null) {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return;
        }
        Slog.d(TAG, "Restoring backup data");
        restoreNetworks(this.mWifiBackupRestore.retrieveConfigurationsFromBackupData(data));
        Slog.d(TAG, "Restored backup data");
    }

    public void restoreSupplicantBackupData(byte[] supplicantData, byte[] ipConfigData) {
        enforceNetworkSettingsPermission();
        this.mLog.trace("restoreSupplicantBackupData uid=%").c((long) Binder.getCallingUid()).flush();
        if (this.mWifiStateMachineChannel == null) {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return;
        }
        Slog.d(TAG, "Restoring supplicant backup data");
        restoreNetworks(this.mWifiBackupRestore.retrieveConfigurationsFromSupplicantBackupData(supplicantData, ipConfigData));
        Slog.d(TAG, "Restored supplicant backup data");
    }

    public void startSubscriptionProvisioning(OsuProvider provider, IProvisioningCallback callback) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider must not be null");
        } else if (callback != null) {
            enforceNetworkSettingsPermission();
            if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint")) {
                int uid = Binder.getCallingUid();
                this.mLog.trace("startSubscriptionProvisioning uid=%").c((long) uid).flush();
                if (this.mWifiStateMachine.syncStartSubscriptionProvisioning(uid, provider, callback, this.mWifiStateMachineChannel)) {
                    this.mLog.trace("Subscription provisioning started with %").c(provider.toString()).flush();
                    return;
                }
                return;
            }
            throw new UnsupportedOperationException("Passpoint not enabled");
        } else {
            throw new IllegalArgumentException("Callback must not be null");
        }
    }

    protected void handleAirplaneModeToggled() {
        this.mWifiController.sendMessage(155657);
    }

    protected void onReceiveEx(Context context, Intent intent) {
    }

    protected void registerForBroadcastsEx(IntentFilter intentFilter) {
    }

    protected void setWifiEnabledAfterVoWifiOff(boolean enable) {
    }

    public boolean setVoWifiDetectMode(WifiDetectConfInfo info) {
        return false;
    }

    protected void handleForgetNetwork(Message msg) {
    }

    protected boolean mdmForPolicyForceOpenWifi(boolean showToast, boolean enable) {
        return false;
    }

    protected boolean startQuickttffScan(String packageName) {
        return false;
    }

    protected boolean restrictWifiScanRequest(String packageName) {
        return false;
    }

    protected boolean limitForegroundWifiScanRequest(String packageName, int uid) {
        return false;
    }

    protected boolean limitWifiScanRequest(String packageName) {
        return false;
    }

    protected boolean limitWifiScanInAbsoluteRest(String packageName) {
        return false;
    }

    protected String getPackageName(int pID) {
        List<RunningAppProcessInfo> appProcessList = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == pID && appProcess.pkgList != null && appProcess.pkgList.length > 0) {
                return appProcess.pkgList[0];
            }
        }
        return null;
    }

    private void sendBehavior(BehaviorId bid) {
        if (this.mHwBehaviorManager == null) {
            synchronized (mWifiLock) {
                if (this.mHwBehaviorManager == null) {
                    this.mHwBehaviorManager = HwFrameworkFactory.getHwBehaviorCollectManager();
                }
            }
        }
        if (this.mHwBehaviorManager != null) {
            try {
                this.mHwBehaviorManager.sendBehavior(Binder.getCallingUid(), Binder.getCallingPid(), bid);
                return;
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendBehavior:");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
                return;
            }
        }
        Log.w(TAG, "HwBehaviorCollectManager is null");
    }
}
