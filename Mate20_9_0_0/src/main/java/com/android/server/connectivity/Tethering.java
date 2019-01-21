package com.android.server.connectivity;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.usb.UsbManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkState;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.util.InterfaceSet;
import android.net.util.PrefixUtils;
import android.net.util.SharedLog;
import android.net.util.VersionedBroadcastListener;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.UserManagerInternal.UserRestrictionsListener;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.ConnectivityService;
import com.android.server.HwConnectivityManager;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.connectivity.tethering.IControlsTethering;
import com.android.server.connectivity.tethering.IPv6TetheringCoordinator;
import com.android.server.connectivity.tethering.OffloadController;
import com.android.server.connectivity.tethering.SimChangeListener;
import com.android.server.connectivity.tethering.TetherInterfaceStateMachine;
import com.android.server.connectivity.tethering.TetheringConfiguration;
import com.android.server.connectivity.tethering.TetheringDependencies;
import com.android.server.connectivity.tethering.TetheringInterfaceUtils;
import com.android.server.connectivity.tethering.UpstreamNetworkMonitor;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.BaseNetworkObserver;
import huawei.cust.HwCustUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Tethering extends BaseNetworkObserver {
    private static final boolean DBG = HWFLOW;
    protected static final String DISABLE_PROVISIONING_SYSPROP_KEY = "net.tethering.noprovisioning";
    private static final boolean HWDBG;
    protected static final boolean HWFLOW;
    private static boolean HWLOGW_E = true;
    private static final int MAX_SLEEP_RETRY_TIMES = 10;
    private static final int NOTIFICATION_TYPE_BLUETOOTH = 2;
    private static final int NOTIFICATION_TYPE_MULTIPLE = 4;
    private static final int NOTIFICATION_TYPE_NONE = -1;
    private static final int NOTIFICATION_TYPE_P2P = 3;
    private static final int NOTIFICATION_TYPE_USB = 1;
    private static final int NOTIFICATION_TYPE_WIFI = 0;
    private static final String TAG = "Tethering";
    private static final ComponentName TETHER_SERVICE = ComponentName.unflattenFromString(Resources.getSystem().getString(17039852));
    private static final boolean VDBG = HWFLOW;
    private static final int WAIT_SLEEP_TIME = 100;
    private static final Class[] messageClasses = new Class[]{Tethering.class, TetherMasterSM.class, TetherInterfaceStateMachine.class};
    private static final SparseArray<String> sMagicDecoderRing = MessageUtils.findMessageNames(messageClasses);
    private final String PROPERTY_BTHOTSPOT_ON = "sys.isbthotspoton";
    private final VersionedBroadcastListener mCarrierConfigChange;
    private volatile TetheringConfiguration mConfig;
    private final Context mContext;
    private InterfaceSet mCurrentUpstreamIfaceSet;
    HwCustTethering mCust = null;
    private final TetheringDependencies mDeps;
    private final HashSet<TetherInterfaceStateMachine> mForwardedDownstreams;
    private HwNotificationTethering mHwNotificationTethering;
    private int mLastNotificationId;
    private final SharedLog mLog = new SharedLog(TAG);
    private final Looper mLooper;
    private final INetworkManagementService mNMService;
    private int mNotificationType = -1;
    private final OffloadController mOffloadController;
    private final INetworkPolicyManager mPolicyManager;
    private final Object mPublicSync;
    private boolean mRndisEnabled;
    private final SimChangeListener mSimChange;
    private final BroadcastReceiver mStateReceiver;
    private final INetworkStatsService mStatsService;
    private final MockableSystemProperties mSystemProperties;
    private final StateMachine mTetherMasterSM;
    private final ArrayMap<String, TetherState> mTetherStates;
    private Builder mTetheredNotificationBuilder;
    private final UpstreamNetworkMonitor mUpstreamNetworkMonitor;
    private boolean mUsbTetherRequested;
    private boolean mWifiTetherRequested;

    private class StateReceiver extends BroadcastReceiver {
        private StateReceiver() {
        }

        /* synthetic */ StateReceiver(Tethering x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (Tethering.DBG) {
                    String str = Tethering.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("StateReceiver onReceive action:");
                    stringBuilder.append(action);
                    Log.d(str, stringBuilder.toString());
                }
                if (action.equals("android.hardware.usb.action.USB_STATE")) {
                    handleUsbAction(intent);
                } else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    handleConnectivityAction(intent);
                } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                    handleWifiApAction(intent);
                } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    Tethering.this.mLog.log("OBSERVED configuration changed");
                    Tethering.this.updateConfiguration();
                }
            }
        }

        private void handleConnectivityAction(Intent intent) {
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            if (networkInfo != null && networkInfo.getDetailedState() != DetailedState.FAILED) {
                if (Tethering.VDBG) {
                    String str = Tethering.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Tethering got CONNECTIVITY_ACTION: ");
                    stringBuilder.append(networkInfo.toString());
                    Log.d(str, stringBuilder.toString());
                }
                Tethering.this.mTetherMasterSM.sendMessage(327683);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:18:0x006d A:{Catch:{ all -> 0x0051 }} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void handleUsbAction(Intent intent) {
            boolean z = false;
            boolean usbConnected = intent.getBooleanExtra("connected", false);
            boolean usbConfigured = intent.getBooleanExtra("configured", false);
            boolean rndisEnabled = intent.getBooleanExtra("rndis", false);
            Tethering.this.mLog.log(String.format("USB bcast connected:%s configured:%s rndis:%s", new Object[]{Boolean.valueOf(usbConnected), Boolean.valueOf(usbConfigured), Boolean.valueOf(rndisEnabled)}));
            synchronized (Tethering.this.mPublicSync) {
                Tethering tethering;
                if (!usbConnected) {
                    try {
                        if (Tethering.this.mRndisEnabled) {
                            Tethering.this.tetherMatchingInterfaces(1, 1);
                            tethering = Tethering.this;
                            if (usbConfigured && rndisEnabled) {
                                z = true;
                            }
                            tethering.mRndisEnabled = z;
                            if (Tethering.DBG) {
                                String str = Tethering.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("StateReceiver onReceive action synchronized: usbConnected = ");
                                stringBuilder.append(usbConnected);
                                stringBuilder.append(", mRndisEnabled = ");
                                stringBuilder.append(Tethering.this.mRndisEnabled);
                                stringBuilder.append(", mUsbTetherRequested = ");
                                stringBuilder.append(Tethering.this.mUsbTetherRequested);
                                Log.d(str, stringBuilder.toString());
                            }
                        }
                    } finally {
                    }
                }
                if (usbConfigured && rndisEnabled) {
                    Tethering.this.tetherMatchingInterfaces(2, 1);
                }
                tethering = Tethering.this;
                z = true;
                tethering.mRndisEnabled = z;
                if (Tethering.DBG) {
                }
            }
        }

        private void handleWifiApAction(Intent intent) {
            int curState = intent.getIntExtra("wifi_state", 11);
            String ifname = intent.getStringExtra("wifi_ap_interface_name");
            int ipmode = intent.getIntExtra("wifi_ap_mode", -1);
            synchronized (Tethering.this.mPublicSync) {
                switch (curState) {
                    case 12:
                        break;
                    case 13:
                        Tethering.this.enableWifiIpServingLocked(ifname, ipmode);
                        break;
                    default:
                        Tethering.this.disableWifiIpServingLocked(ifname, curState);
                        break;
                }
            }
        }
    }

    class TetherMasterSM extends StateMachine {
        private static final int BASE_MASTER = 327680;
        static final int CMD_CLEAR_ERROR = 327686;
        static final int CMD_RETRY_UPSTREAM = 327684;
        static final int CMD_UPSTREAM_CHANGED = 327683;
        static final int EVENT_IFACE_SERVING_STATE_ACTIVE = 327681;
        static final int EVENT_IFACE_SERVING_STATE_INACTIVE = 327682;
        static final int EVENT_IFACE_UPDATE_LINKPROPERTIES = 327687;
        static final int EVENT_UPSTREAM_CALLBACK = 327685;
        private static final int UPSTREAM_SETTLE_TIME_MS = 10000;
        private final IPv6TetheringCoordinator mIPv6TetheringCoordinator;
        private final State mInitialState = new InitialState();
        private final ArrayList<TetherInterfaceStateMachine> mNotifyList;
        private final OffloadWrapper mOffload;
        private final State mSetDnsForwardersErrorState = new SetDnsForwardersErrorState();
        private final State mSetIpForwardingDisabledErrorState = new SetIpForwardingDisabledErrorState();
        private final State mSetIpForwardingEnabledErrorState = new SetIpForwardingEnabledErrorState();
        private final State mStartTetheringErrorState = new StartTetheringErrorState();
        private final State mStopTetheringErrorState = new StopTetheringErrorState();
        private final State mTetherModeAliveState = new TetherModeAliveState();

        class ErrorState extends State {
            private int mErrorNotification;

            ErrorState() {
            }

            public boolean processMessage(Message message) {
                if (Tethering.DBG) {
                    String str = Tethering.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ErrorState processMessage what=");
                    stringBuilder.append(message.what);
                    Log.d(str, stringBuilder.toString());
                }
                int i = message.what;
                if (i == TetherMasterSM.EVENT_IFACE_SERVING_STATE_ACTIVE) {
                    message.obj.sendMessage(this.mErrorNotification);
                    return true;
                } else if (i != TetherMasterSM.CMD_CLEAR_ERROR) {
                    return false;
                } else {
                    this.mErrorNotification = 0;
                    TetherMasterSM.this.transitionTo(TetherMasterSM.this.mInitialState);
                    return true;
                }
            }

            void notify(int msgType) {
                this.mErrorNotification = msgType;
                Iterator it = TetherMasterSM.this.mNotifyList.iterator();
                while (it.hasNext()) {
                    ((TetherInterfaceStateMachine) it.next()).sendMessage(msgType);
                }
            }
        }

        class InitialState extends State {
            InitialState() {
            }

            public boolean processMessage(Message message) {
                Tethering.this.logMessage(this, message.what);
                int i = message.what;
                if (i != TetherMasterSM.EVENT_IFACE_UPDATE_LINKPROPERTIES) {
                    TetherInterfaceStateMachine who;
                    String str;
                    StringBuilder stringBuilder;
                    switch (i) {
                        case TetherMasterSM.EVENT_IFACE_SERVING_STATE_ACTIVE /*327681*/:
                            who = (TetherInterfaceStateMachine) message.obj;
                            if (Tethering.VDBG) {
                                str = Tethering.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Tether Mode requested by ");
                                stringBuilder.append(who);
                                Log.d(str, stringBuilder.toString());
                            }
                            TetherMasterSM.this.handleInterfaceServingStateActive(message.arg1, who);
                            TetherMasterSM.this.transitionTo(TetherMasterSM.this.mTetherModeAliveState);
                            break;
                        case TetherMasterSM.EVENT_IFACE_SERVING_STATE_INACTIVE /*327682*/:
                            who = message.obj;
                            if (Tethering.VDBG) {
                                str = Tethering.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Tether Mode unrequested by ");
                                stringBuilder.append(who);
                                Log.d(str, stringBuilder.toString());
                            }
                            TetherMasterSM.this.handleInterfaceServingStateInactive(who);
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }
        }

        class OffloadWrapper {
            OffloadWrapper() {
            }

            public void start() {
                Tethering.this.mOffloadController.start();
                sendOffloadExemptPrefixes();
            }

            public void stop() {
                Tethering.this.mOffloadController.stop();
            }

            public void updateUpstreamNetworkState(NetworkState ns) {
                Tethering.this.mOffloadController.setUpstreamLinkProperties(ns != null ? ns.linkProperties : null);
            }

            public void updateDownstreamLinkProperties(LinkProperties newLp) {
                sendOffloadExemptPrefixes();
                Tethering.this.mOffloadController.notifyDownstreamLinkProperties(newLp);
            }

            public void excludeDownstreamInterface(String ifname) {
                sendOffloadExemptPrefixes();
                Tethering.this.mOffloadController.removeDownstreamInterface(ifname);
            }

            public void sendOffloadExemptPrefixes() {
                sendOffloadExemptPrefixes(Tethering.this.mUpstreamNetworkMonitor.getLocalPrefixes());
            }

            public void sendOffloadExemptPrefixes(Set<IpPrefix> localPrefixes) {
                PrefixUtils.addNonForwardablePrefixes(localPrefixes);
                localPrefixes.add(PrefixUtils.DEFAULT_WIFI_P2P_PREFIX);
                Iterator it = TetherMasterSM.this.mNotifyList.iterator();
                while (it.hasNext()) {
                    TetherInterfaceStateMachine tism = (TetherInterfaceStateMachine) it.next();
                    LinkProperties lp = tism.linkProperties();
                    switch (tism.servingMode()) {
                        case 0:
                        case 1:
                            continue;
                        case 2:
                            for (LinkAddress addr : lp.getAllLinkAddresses()) {
                                InetAddress ip = addr.getAddress();
                                if (!ip.isLinkLocalAddress()) {
                                    localPrefixes.add(PrefixUtils.ipAddressAsPrefix(ip));
                                }
                            }
                            break;
                        case 3:
                            localPrefixes.addAll(PrefixUtils.localPrefixesFrom(lp));
                            break;
                    }
                }
                Tethering.this.mOffloadController.setLocalPrefixes(localPrefixes);
            }
        }

        class TetherModeAliveState extends State {
            boolean mTryCell = true;
            boolean mUpstreamWanted = false;

            TetherModeAliveState() {
            }

            public void enter() {
                if (Tethering.DBG) {
                    Log.d(Tethering.TAG, "TetherModeAliveState enter");
                }
                if (TetherMasterSM.this.turnOnMasterTetherSettings()) {
                    Tethering.this.mSimChange.startListening();
                    Tethering.this.mUpstreamNetworkMonitor.start();
                    if (Tethering.this.upstreamWanted()) {
                        this.mUpstreamWanted = true;
                        TetherMasterSM.this.mOffload.start();
                        TetherMasterSM.this.chooseUpstreamType(true);
                        this.mTryCell = false;
                    }
                }
            }

            public void exit() {
                TetherMasterSM.this.mOffload.stop();
                Tethering.this.mUpstreamNetworkMonitor.stop();
                Tethering.this.mSimChange.stopListening();
                TetherMasterSM.this.notifyDownstreamsOfNewUpstreamIface(null);
                TetherMasterSM.this.handleNewUpstreamNetworkState(null);
            }

            private boolean updateUpstreamWanted() {
                boolean previousUpstreamWanted = this.mUpstreamWanted;
                this.mUpstreamWanted = Tethering.this.upstreamWanted();
                if (this.mUpstreamWanted != previousUpstreamWanted) {
                    if (this.mUpstreamWanted) {
                        TetherMasterSM.this.mOffload.start();
                    } else {
                        TetherMasterSM.this.mOffload.stop();
                    }
                }
                return previousUpstreamWanted;
            }

            public boolean processMessage(Message message) {
                Tethering.this.logMessage(this, message.what);
                TetherInterfaceStateMachine who;
                switch (message.what) {
                    case TetherMasterSM.EVENT_IFACE_SERVING_STATE_ACTIVE /*327681*/:
                        who = (TetherInterfaceStateMachine) message.obj;
                        if (Tethering.VDBG) {
                            String str = Tethering.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Tether Mode requested by ");
                            stringBuilder.append(who);
                            Log.d(str, stringBuilder.toString());
                        }
                        TetherMasterSM.this.handleInterfaceServingStateActive(message.arg1, who);
                        who.sendMessage(TetherInterfaceStateMachine.CMD_TETHER_CONNECTION_CHANGED, Tethering.this.mCurrentUpstreamIfaceSet);
                        if (updateUpstreamWanted() || !this.mUpstreamWanted) {
                            return true;
                        }
                        TetherMasterSM.this.chooseUpstreamType(true);
                        return true;
                    case TetherMasterSM.EVENT_IFACE_SERVING_STATE_INACTIVE /*327682*/:
                        String str2;
                        StringBuilder stringBuilder2;
                        who = message.obj;
                        if (Tethering.VDBG) {
                            str2 = Tethering.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Tether Mode unrequested by ");
                            stringBuilder2.append(who);
                            Log.d(str2, stringBuilder2.toString());
                        }
                        TetherMasterSM.this.handleInterfaceServingStateInactive(who);
                        if (TetherMasterSM.this.mNotifyList.isEmpty()) {
                            TetherMasterSM.this.turnOffMasterTetherSettings();
                            return true;
                        }
                        if (Tethering.DBG) {
                            str2 = Tethering.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("TetherModeAlive still has ");
                            stringBuilder2.append(TetherMasterSM.this.mNotifyList.size());
                            stringBuilder2.append(" live requests:");
                            Log.d(str2, stringBuilder2.toString());
                            Iterator it = TetherMasterSM.this.mNotifyList.iterator();
                            while (it.hasNext()) {
                                TetherInterfaceStateMachine o = (TetherInterfaceStateMachine) it.next();
                                String str3 = Tethering.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("  ");
                                stringBuilder3.append(o);
                                Log.d(str3, stringBuilder3.toString());
                            }
                        }
                        if (!updateUpstreamWanted() || this.mUpstreamWanted) {
                            return true;
                        }
                        Tethering.this.mUpstreamNetworkMonitor.releaseMobileNetworkRequest();
                        return true;
                    case TetherMasterSM.CMD_UPSTREAM_CHANGED /*327683*/:
                        updateUpstreamWanted();
                        if (!this.mUpstreamWanted) {
                            return true;
                        }
                        long iotDelayPropTimer = SystemProperties.getLong("persist.radio.telecom_apn_delay", 0);
                        String str4 = Tethering.TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("CMD_UPSTREAM_CHANGED, iotDelayPropTimer = ");
                        stringBuilder4.append(iotDelayPropTimer);
                        Log.d(str4, stringBuilder4.toString());
                        if (iotDelayPropTimer > 0) {
                            return true;
                        }
                        TetherMasterSM.this.chooseUpstreamType(true);
                        this.mTryCell = false;
                        return true;
                    case TetherMasterSM.CMD_RETRY_UPSTREAM /*327684*/:
                        updateUpstreamWanted();
                        if (!this.mUpstreamWanted) {
                            return true;
                        }
                        TetherMasterSM.this.chooseUpstreamType(this.mTryCell);
                        this.mTryCell ^= 1;
                        return true;
                    case TetherMasterSM.EVENT_UPSTREAM_CALLBACK /*327685*/:
                        updateUpstreamWanted();
                        if (!this.mUpstreamWanted) {
                            return true;
                        }
                        TetherMasterSM.this.handleUpstreamNetworkMonitorCallback(message.arg1, message.obj);
                        return true;
                    case TetherMasterSM.EVENT_IFACE_UPDATE_LINKPROPERTIES /*327687*/:
                        LinkProperties newLp = message.obj;
                        if (message.arg1 == 2) {
                            TetherMasterSM.this.mOffload.updateDownstreamLinkProperties(newLp);
                            return true;
                        }
                        TetherMasterSM.this.mOffload.excludeDownstreamInterface(newLp.getInterfaceName());
                        return true;
                    default:
                        return false;
                }
            }
        }

        class SetDnsForwardersErrorState extends ErrorState {
            SetDnsForwardersErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in setDnsForwarders");
                notify(TetherInterfaceStateMachine.CMD_SET_DNS_FORWARDERS_ERROR);
                try {
                    Tethering.this.mNMService.stopTethering();
                } catch (Exception e) {
                }
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                } catch (Exception e2) {
                }
            }
        }

        class SetIpForwardingDisabledErrorState extends ErrorState {
            SetIpForwardingDisabledErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in setIpForwardingDisabled");
                notify(TetherInterfaceStateMachine.CMD_IP_FORWARDING_DISABLE_ERROR);
            }
        }

        class SetIpForwardingEnabledErrorState extends ErrorState {
            SetIpForwardingEnabledErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in setIpForwardingEnabled");
                notify(TetherInterfaceStateMachine.CMD_IP_FORWARDING_ENABLE_ERROR);
            }
        }

        class StartTetheringErrorState extends ErrorState {
            StartTetheringErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in startTethering");
                notify(TetherInterfaceStateMachine.CMD_START_TETHERING_ERROR);
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                } catch (Exception e) {
                }
            }
        }

        class StopTetheringErrorState extends ErrorState {
            StopTetheringErrorState() {
                super();
            }

            public void enter() {
                Log.e(Tethering.TAG, "Error in stopTethering");
                notify(TetherInterfaceStateMachine.CMD_STOP_TETHERING_ERROR);
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                } catch (Exception e) {
                }
            }
        }

        TetherMasterSM(String name, Looper looper, TetheringDependencies deps) {
            super(name, looper);
            addState(this.mInitialState);
            addState(this.mTetherModeAliveState);
            addState(this.mSetIpForwardingEnabledErrorState);
            addState(this.mSetIpForwardingDisabledErrorState);
            addState(this.mStartTetheringErrorState);
            addState(this.mStopTetheringErrorState);
            addState(this.mSetDnsForwardersErrorState);
            this.mNotifyList = new ArrayList();
            this.mIPv6TetheringCoordinator = deps.getIPv6TetheringCoordinator(this.mNotifyList, Tethering.this.mLog);
            this.mOffload = new OffloadWrapper();
            setInitialState(this.mInitialState);
        }

        protected boolean turnOnMasterTetherSettings() {
            TetheringConfiguration cfg = Tethering.this.mConfig;
            try {
                Tethering.this.mNMService.setIpForwardingEnabled(true);
                try {
                    Tethering.this.mNMService.startTethering(cfg.dhcpRanges);
                } catch (Exception e) {
                    try {
                        Tethering.this.mNMService.stopTethering();
                        Tethering.this.mNMService.startTethering(cfg.dhcpRanges);
                    } catch (Exception ee) {
                        Tethering.this.mLog.e(ee);
                        transitionTo(this.mStartTetheringErrorState);
                        return false;
                    }
                }
                Tethering.this.mLog.log("SET master tether settings: ON");
                return true;
            } catch (Exception e2) {
                Tethering.this.mLog.e(e2);
                transitionTo(this.mSetIpForwardingEnabledErrorState);
                return false;
            }
        }

        protected boolean turnOffMasterTetherSettings() {
            try {
                Tethering.this.mNMService.stopTethering();
                try {
                    Tethering.this.mNMService.setIpForwardingEnabled(false);
                    transitionTo(this.mInitialState);
                    Tethering.this.mLog.log("SET master tether settings: OFF");
                    return true;
                } catch (Exception e) {
                    Tethering.this.mLog.e(e);
                    transitionTo(this.mSetIpForwardingDisabledErrorState);
                    return false;
                }
            } catch (Exception e2) {
                Tethering.this.mLog.e(e2);
                transitionTo(this.mStopTetheringErrorState);
                return false;
            }
        }

        protected void chooseUpstreamType(boolean tryCell) {
            Tethering.this.maybeUpdateConfiguration();
            NetworkState ns = Tethering.this.mUpstreamNetworkMonitor.selectPreferredUpstreamType(Tethering.this.mConfig.preferredUpstreamIfaceTypes);
            if (ns == null) {
                if (tryCell) {
                    Tethering.this.mUpstreamNetworkMonitor.registerMobileNetworkRequest();
                } else {
                    sendMessageDelayed(CMD_RETRY_UPSTREAM, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                }
            }
            Tethering.this.mUpstreamNetworkMonitor.setCurrentUpstream(ns != null ? ns.network : null);
            setUpstreamNetwork(ns);
        }

        protected void setUpstreamNetwork(NetworkState ns) {
            InterfaceSet ifaces = null;
            if (ns != null) {
                SharedLog access$600 = Tethering.this.mLog;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Looking for default routes on: ");
                stringBuilder.append(ns.linkProperties);
                access$600.i(stringBuilder.toString());
                ifaces = TetheringInterfaceUtils.getTetheringInterfaces(ns);
                access$600 = Tethering.this.mLog;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Found upstream interface(s): ");
                stringBuilder.append(ifaces);
                access$600.i(stringBuilder.toString());
            }
            if (ifaces != null) {
                setDnsForwarders(ns.network, ns.linkProperties);
            }
            notifyDownstreamsOfNewUpstreamIface(ifaces);
            if (ns != null && Tethering.this.pertainsToCurrentUpstream(ns)) {
                handleNewUpstreamNetworkState(ns);
            } else if (Tethering.this.mCurrentUpstreamIfaceSet == null) {
                handleNewUpstreamNetworkState(null);
            }
        }

        protected void setDnsForwarders(Network network, LinkProperties lp) {
            if (lp != null) {
                String[] dnsServers = Tethering.this.mConfig.defaultIPv4DNS;
                Collection<InetAddress> dnses = lp.getDnsServers();
                if (!(dnses == null || dnses.isEmpty())) {
                    dnsServers = NetworkUtils.makeStrings(dnses);
                }
                try {
                    Tethering.this.mNMService.setDnsForwarders(network, dnsServers);
                    Tethering.this.mLog.log(String.format("SET DNS forwarders: network=%s dnsServers=%s", new Object[]{network, Arrays.toString(dnsServers)}));
                } catch (Exception e) {
                    SharedLog access$600 = Tethering.this.mLog;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setting DNS forwarders failed, ");
                    stringBuilder.append(e);
                    access$600.e(stringBuilder.toString());
                    transitionTo(this.mSetDnsForwardersErrorState);
                }
            }
        }

        protected void notifyDownstreamsOfNewUpstreamIface(InterfaceSet ifaces) {
            Tethering.this.mCurrentUpstreamIfaceSet = ifaces;
            Iterator it = this.mNotifyList.iterator();
            while (it.hasNext()) {
                ((TetherInterfaceStateMachine) it.next()).sendMessage(TetherInterfaceStateMachine.CMD_TETHER_CONNECTION_CHANGED, ifaces);
            }
        }

        protected void handleNewUpstreamNetworkState(NetworkState ns) {
            this.mIPv6TetheringCoordinator.updateUpstreamNetworkState(ns);
            this.mOffload.updateUpstreamNetworkState(ns);
        }

        private void handleInterfaceServingStateActive(int mode, TetherInterfaceStateMachine who) {
            if (this.mNotifyList.indexOf(who) < 0) {
                this.mNotifyList.add(who);
                this.mIPv6TetheringCoordinator.addActiveDownstream(who, mode);
            }
            if (mode == 2) {
                Tethering.this.mForwardedDownstreams.add(who);
            } else {
                this.mOffload.excludeDownstreamInterface(who.interfaceName());
                Tethering.this.mForwardedDownstreams.remove(who);
            }
            if (who.interfaceType() == 0) {
                WifiManager mgr = Tethering.this.getWifiManager();
                String iface = who.interfaceName();
                switch (mode) {
                    case 2:
                        mgr.updateInterfaceIpState(iface, 1);
                        return;
                    case 3:
                        mgr.updateInterfaceIpState(iface, 2);
                        return;
                    default:
                        String str = Tethering.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown active serving mode: ");
                        stringBuilder.append(mode);
                        Log.wtf(str, stringBuilder.toString());
                        return;
                }
            }
        }

        private void handleInterfaceServingStateInactive(TetherInterfaceStateMachine who) {
            this.mNotifyList.remove(who);
            this.mIPv6TetheringCoordinator.removeActiveDownstream(who);
            this.mOffload.excludeDownstreamInterface(who.interfaceName());
            Tethering.this.mForwardedDownstreams.remove(who);
            if (who.interfaceType() == 0 && who.lastError() != 0) {
                Tethering.this.getWifiManager().updateInterfaceIpState(who.interfaceName(), 0);
            }
        }

        private void handleUpstreamNetworkMonitorCallback(int arg1, Object o) {
            if (arg1 == 10) {
                this.mOffload.sendOffloadExemptPrefixes((Set) o);
                return;
            }
            NetworkState ns = (NetworkState) o;
            if (ns == null || !Tethering.this.pertainsToCurrentUpstream(ns)) {
                if (Tethering.this.mCurrentUpstreamIfaceSet == null) {
                    chooseUpstreamType(false);
                }
                return;
            }
            switch (arg1) {
                case 1:
                    break;
                case 2:
                    handleNewUpstreamNetworkState(ns);
                    break;
                case 3:
                    chooseUpstreamType(false);
                    break;
                case 4:
                    handleNewUpstreamNetworkState(null);
                    break;
                default:
                    SharedLog access$600 = Tethering.this.mLog;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown arg1 value: ");
                    stringBuilder.append(arg1);
                    access$600.e(stringBuilder.toString());
                    break;
            }
        }
    }

    private static class TetherState {
        public int lastError = 0;
        public int lastState = 1;
        public final TetherInterfaceStateMachine stateMachine;

        public TetherState(TetherInterfaceStateMachine sm) {
            this.stateMachine = sm;
        }

        public boolean isCurrentlyServing() {
            switch (this.lastState) {
                case 2:
                case 3:
                    return true;
                default:
                    return false;
            }
        }
    }

    @VisibleForTesting
    protected static class TetheringUserRestrictionListener implements UserRestrictionsListener {
        private final Tethering mWrapper;

        public TetheringUserRestrictionListener(Tethering wrapper) {
            this.mWrapper = wrapper;
        }

        public void onUserRestrictionsChanged(int userId, Bundle newRestrictions, Bundle prevRestrictions) {
            boolean newlyDisallowed = newRestrictions.getBoolean("no_config_tethering");
            boolean isTetheringActiveOnDevice = true;
            if (newlyDisallowed != prevRestrictions.getBoolean("no_config_tethering")) {
                this.mWrapper.clearTetheredNotification();
                if (this.mWrapper.getTetheredIfaces().length == 0) {
                    isTetheringActiveOnDevice = false;
                }
                if (newlyDisallowed && isTetheringActiveOnDevice) {
                    this.mWrapper.showTetheredNotification(17303546, false);
                    this.mWrapper.untetherAll();
                }
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
        z = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDBG = z;
    }

    public Tethering(Context context, INetworkManagementService nmService, INetworkStatsService statsService, INetworkPolicyManager policyManager, Looper looper, MockableSystemProperties systemProperties, TetheringDependencies deps) {
        TetheringDependencies tetheringDependencies = deps;
        this.mLog.mark("constructed");
        this.mContext = context;
        this.mNMService = nmService;
        this.mStatsService = statsService;
        this.mPolicyManager = policyManager;
        this.mLooper = looper;
        this.mSystemProperties = systemProperties;
        this.mDeps = tetheringDependencies;
        this.mPublicSync = new Object();
        this.mTetherStates = new ArrayMap();
        this.mCust = (HwCustTethering) HwCustUtils.createObj(HwCustTethering.class, new Object[]{this.mContext});
        this.mTetherMasterSM = new TetherMasterSM("TetherMaster", this.mLooper, tetheringDependencies);
        this.mTetherMasterSM.start();
        Handler smHandler = this.mTetherMasterSM.getHandler();
        OffloadController offloadController = r10;
        OffloadController offloadController2 = new OffloadController(smHandler, this.mDeps.getOffloadHardwareInterface(smHandler, this.mLog), this.mContext.getContentResolver(), this.mNMService, this.mLog);
        this.mOffloadController = offloadController;
        this.mUpstreamNetworkMonitor = tetheringDependencies.getUpstreamNetworkMonitor(this.mContext, this.mTetherMasterSM, this.mLog, 327685);
        this.mForwardedDownstreams = new HashSet();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        VersionedBroadcastListener versionedBroadcastListener = r10;
        VersionedBroadcastListener versionedBroadcastListener2 = new VersionedBroadcastListener("CarrierConfigChangeListener", this.mContext, smHandler, filter, new -$$Lambda$Tethering$5JkghhOVq1MW7iK03DMZUSuLdFM(this));
        this.mCarrierConfigChange = versionedBroadcastListener;
        this.mSimChange = new SimChangeListener(this.mContext, smHandler, new -$$Lambda$Tethering$G9TtPVJE34-mHCiIrkFoFBxZRf8(this));
        this.mStateReceiver = new StateReceiver(this, null);
        updateConfiguration();
        startStateMachineUpdaters();
    }

    public static /* synthetic */ void lambda$new$0(Tethering tethering, Intent ignored) {
        tethering.mLog.log("OBSERVED carrier config change");
        tethering.updateConfiguration();
        tethering.reevaluateSimCardProvisioning();
    }

    private void startStateMachineUpdaters() {
        this.mCarrierConfigChange.startListening();
        Handler handler = this.mTetherMasterSM.getHandler();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        this.mContext.registerReceiver(this.mStateReceiver, filter, null, handler);
        if (this.mCust != null) {
            this.mCust.registerBroadcast(this.mPublicSync);
        }
        filter = new IntentFilter();
        filter.addAction("android.intent.action.MEDIA_SHARED");
        filter.addAction("android.intent.action.MEDIA_UNSHARED");
        filter.addDataScheme("file");
        this.mContext.registerReceiver(this.mStateReceiver, filter, null, handler);
        UserManagerInternal umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        if (umi != null) {
            umi.addUserRestrictionsListener(new TetheringUserRestrictionListener(this));
        }
        this.mHwNotificationTethering = HwServiceFactory.getHwNotificationTethering(this.mContext);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("com.android.server.connectivity.action.STOP_TETHERING".equals(intent.getAction())) {
                    Tethering.this.mHwNotificationTethering.stopTethering();
                    Tethering.this.clearTetheredNotification();
                }
            }
        }, new IntentFilter("com.android.server.connectivity.action.STOP_TETHERING"), "com.android.server.connectivity.permission.STOP_TETHERING", null);
    }

    private WifiManager getWifiManager() {
        return (WifiManager) this.mContext.getSystemService("wifi");
    }

    private void updateConfiguration() {
        this.mConfig = new TetheringConfiguration(this.mContext, this.mLog);
        this.mUpstreamNetworkMonitor.updateMobileRequiresDun(this.mConfig.isDunRequired);
    }

    private void maybeUpdateConfiguration() {
        if (TetheringConfiguration.checkDunRequired(this.mContext) != this.mConfig.dunCheck) {
            updateConfiguration();
        }
    }

    public void interfaceStatusChanged(String iface, boolean up) {
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("interfaceStatusChanged ");
            stringBuilder.append(iface);
            stringBuilder.append(", ");
            stringBuilder.append(up);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mPublicSync) {
            if (up) {
                try {
                    maybeTrackNewInterfaceLocked(iface);
                } catch (Throwable th) {
                }
            } else if (ifaceNameToType(iface) == 2) {
                stopTrackingInterfaceLocked(iface);
            } else if (VDBG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ignore interface down for ");
                stringBuilder2.append(iface);
                Log.d(str2, stringBuilder2.toString());
            }
        }
    }

    public void interfaceLinkStateChanged(String iface, boolean up) {
        interfaceStatusChanged(iface, up);
    }

    private int ifaceNameToType(String iface) {
        TetheringConfiguration cfg = this.mConfig;
        if (cfg.isWifi(iface)) {
            return 0;
        }
        if (cfg.isUsb(iface)) {
            return 1;
        }
        if (cfg.isBluetooth(iface)) {
            return 2;
        }
        if (HwServiceFactory.getHwConnectivityManager().isP2pTether(iface)) {
            return 3;
        }
        return -1;
    }

    public void interfaceAdded(String iface) {
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("interfaceAdded ");
            stringBuilder.append(iface);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mPublicSync) {
            maybeTrackNewInterfaceLocked(iface);
        }
    }

    public void interfaceRemoved(String iface) {
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("interfaceRemoved ");
            stringBuilder.append(iface);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mPublicSync) {
            stopTrackingInterfaceLocked(iface);
        }
    }

    public void startTethering(int type, ResultReceiver receiver, boolean showProvisioningUi) {
        if (isTetherProvisioningRequired()) {
            if (showProvisioningUi) {
                runUiTetherProvisioningAndEnable(type, receiver);
            } else {
                runSilentTetherProvisioningAndEnable(type, receiver);
            }
            return;
        }
        enableTetheringInternal(type, true, receiver);
    }

    public void stopTethering(int type) {
        enableTetheringInternal(type, false, null);
        if (isTetherProvisioningRequired()) {
            cancelTetherProvisioningRechecks(type);
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0063, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    protected boolean isTetherProvisioningRequired() {
        TetheringConfiguration cfg = this.mConfig;
        if (this.mSystemProperties.getBoolean(DISABLE_PROVISIONING_SYSPROP_KEY, false) || cfg.provisioningApp.length == 0 || carrierConfigAffirmsEntitlementCheckNotRequired()) {
            return false;
        }
        boolean z = true;
        if (cfg.provisioningApp.length == 2) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setClassName(cfg.provisioningApp[0], cfg.provisioningApp[1]);
            if (!(this.mContext == null || this.mContext.getPackageManager() == null || !this.mContext.getPackageManager().queryIntentActivities(intent, 65536).isEmpty())) {
                Log.e(TAG, "isTetherProvisioningRequired Provisioning app is configured, but not available.");
                return false;
            }
        }
        if (cfg.provisioningApp.length != 2) {
            z = false;
        }
        return z;
    }

    private boolean carrierConfigAffirmsEntitlementCheckNotRequired() {
        CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (configManager == null) {
            return false;
        }
        PersistableBundle carrierConfig = configManager.getConfig();
        if (carrierConfig == null) {
            return false;
        }
        return carrierConfig.getBoolean("require_entitlement_checks_bool") ^ 1;
    }

    private void enableTetheringInternal(int type, boolean enable, ResultReceiver receiver) {
        boolean isProvisioningRequired = enable && isTetherProvisioningRequired();
        int result;
        switch (type) {
            case 0:
                result = setWifiTethering(enable);
                if (isProvisioningRequired && result == 0) {
                    scheduleProvisioningRechecks(type);
                }
                sendTetherResult(receiver, result);
                return;
            case 1:
                result = setUsbTethering(enable);
                if (isProvisioningRequired && result == 0) {
                    scheduleProvisioningRechecks(type);
                }
                sendTetherResult(receiver, result);
                return;
            case 2:
                setBluetoothTethering(enable, receiver);
                return;
            case 3:
                if (VDBG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("type: ");
                    stringBuilder.append(type);
                    stringBuilder.append(" enable: ");
                    stringBuilder.append(enable);
                    Log.d(str, stringBuilder.toString());
                }
                if (!enable) {
                    HwServiceFactory.getHwConnectivityManager().stopP2pTether(this.mContext);
                    return;
                }
                return;
            default:
                Log.w(TAG, "Invalid tether type.");
                sendTetherResult(receiver, 1);
                return;
        }
    }

    private void sendTetherResult(ResultReceiver receiver, int result) {
        if (receiver != null) {
            receiver.send(result, null);
        }
    }

    private int setWifiTethering(boolean enable) {
        int rval = 5;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPublicSync) {
                this.mWifiTetherRequested = enable;
                WifiManager mgr = getWifiManager();
                if ((enable && mgr.startSoftAp(null)) || (!enable && mgr.stopSoftAp())) {
                    rval = 0;
                }
            }
            Binder.restoreCallingIdentity(ident);
            return rval;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void setBluetoothTethering(final boolean enable, final ResultReceiver receiver) {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Tried to enable bluetooth tethering with null or disabled adapter. null: ");
            stringBuilder.append(adapter == null);
            Log.w(str, stringBuilder.toString());
            sendTetherResult(receiver, 2);
            return;
        }
        adapter.getProfileProxy(this.mContext, new ServiceListener() {
            public void onServiceDisconnected(int profile) {
            }

            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                int result;
                ((BluetoothPan) proxy).setBluetoothTethering(enable);
                if (((BluetoothPan) proxy).isTetheringOn() == enable) {
                    result = 0;
                } else {
                    result = 5;
                }
                String str = Tethering.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setBluetoothTethering.ServiceListener.onServiceConnected called, enable : ");
                stringBuilder.append(enable);
                stringBuilder.append(", result : ");
                stringBuilder.append(result);
                Log.d(str, stringBuilder.toString());
                if (result == 0) {
                    SystemProperties.set("sys.isbthotspoton", enable ? "true" : "false");
                }
                Tethering.this.sendTetherResult(receiver, result);
                if (enable && Tethering.this.isTetherProvisioningRequired()) {
                    Tethering.this.scheduleProvisioningRechecks(2);
                }
                adapter.closeProfileProxy(5, proxy);
            }
        }, 5);
    }

    private void runUiTetherProvisioningAndEnable(int type, ResultReceiver receiver) {
        sendUiTetherProvisionIntent(type, getProxyReceiver(type, receiver));
    }

    private void sendUiTetherProvisionIntent(int type, ResultReceiver receiver) {
        Intent intent = new Intent("android.settings.TETHER_PROVISIONING_UI");
        intent.putExtra("extraAddTetherType", type);
        intent.putExtra("extraProvisionCallback", receiver);
        intent.addFlags(268435456);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private ResultReceiver getProxyReceiver(final int type, final ResultReceiver receiver) {
        ResultReceiver rr = new ResultReceiver(null) {
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 0) {
                    Tethering.this.enableTetheringInternal(type, true, receiver);
                } else {
                    Tethering.this.sendTetherResult(receiver, resultCode);
                }
            }
        };
        Parcel parcel = Parcel.obtain();
        rr.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ResultReceiver receiverForSending = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return receiverForSending;
    }

    private void scheduleProvisioningRechecks(int type) {
        Intent intent = new Intent();
        intent.putExtra("extraAddTetherType", type);
        intent.putExtra("extraSetAlarm", true);
        intent.setComponent(TETHER_SERVICE);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void runSilentTetherProvisioningAndEnable(int type, ResultReceiver receiver) {
        sendSilentTetherProvisionIntent(type, getProxyReceiver(type, receiver));
    }

    private void sendSilentTetherProvisionIntent(int type, ResultReceiver receiver) {
        Intent intent = new Intent();
        intent.putExtra("extraAddTetherType", type);
        intent.putExtra("extraRunProvision", true);
        intent.putExtra("extraProvisionCallback", receiver);
        intent.setComponent(TETHER_SERVICE);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void cancelTetherProvisioningRechecks(int type) {
        if (this.mDeps.isTetheringSupported()) {
            Intent intent = new Intent();
            intent.putExtra("extraRemTetherType", type);
            intent.setComponent(TETHER_SERVICE);
            long ident = Binder.clearCallingIdentity();
            try {
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void startProvisionIntent(int tetherType) {
        Intent startProvIntent = new Intent();
        startProvIntent.putExtra("extraAddTetherType", tetherType);
        startProvIntent.putExtra("extraRunProvision", true);
        startProvIntent.setComponent(TETHER_SERVICE);
        this.mContext.startServiceAsUser(startProvIntent, UserHandle.CURRENT);
    }

    public int tether(String iface) {
        return tether(iface, 2);
    }

    private int tether(String iface, int requestedState) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Tethering ");
            stringBuilder.append(iface);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mPublicSync) {
            TetherState tetherState = (TetherState) this.mTetherStates.get(iface);
            if (tetherState == null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Tried to Tether an unknown iface: ");
                stringBuilder2.append(iface);
                stringBuilder2.append(", ignoring");
                Log.e(str2, stringBuilder2.toString());
                return 1;
            } else if (tetherState.lastState != 1) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Tried to Tether an unavailable iface: ");
                stringBuilder3.append(iface);
                stringBuilder3.append(", ignoring");
                Log.e(str3, stringBuilder3.toString());
                return 4;
            } else {
                tetherState.stateMachine.sendMessage(TetherInterfaceStateMachine.CMD_TETHER_REQUESTED, requestedState);
                return 0;
            }
        }
    }

    public int untether(String iface) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Untethering ");
            stringBuilder.append(iface);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mPublicSync) {
            TetherState tetherState = (TetherState) this.mTetherStates.get(iface);
            String str2;
            StringBuilder stringBuilder2;
            if (tetherState == null) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Tried to Untether an unknown iface :");
                stringBuilder2.append(iface);
                stringBuilder2.append(", ignoring");
                Log.e(str2, stringBuilder2.toString());
                return 1;
            } else if (tetherState.isCurrentlyServing()) {
                tetherState.stateMachine.sendMessage(TetherInterfaceStateMachine.CMD_TETHER_UNREQUESTED);
                return 0;
            } else {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Tried to untether an inactive iface :");
                stringBuilder2.append(iface);
                stringBuilder2.append(", ignoring");
                Log.e(str2, stringBuilder2.toString());
                return 4;
            }
        }
    }

    public void untetherAll() {
        stopTethering(0);
        stopTethering(1);
        stopTethering(2);
    }

    public int getLastTetherError(String iface) {
        synchronized (this.mPublicSync) {
            TetherState tetherState = (TetherState) this.mTetherStates.get(iface);
            if (tetherState == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Tried to getLastTetherError on an unknown iface :");
                stringBuilder.append(iface);
                stringBuilder.append(", ignoring");
                Log.e(str, stringBuilder.toString());
                return 1;
            }
            int i = tetherState.lastError;
            return i;
        }
    }

    /* JADX WARNING: Missing block: B:66:0x0107, code skipped:
            r0 = new android.content.Intent("android.net.conn.TETHER_STATE_CHANGED");
            r0.addFlags(620756992);
            r0.putStringArrayListExtra("availableArray", r4);
            r0.putStringArrayListExtra("localOnlyArray", r6);
            r0.putStringArrayListExtra("tetherArray", r5);
            r0.putStringArrayListExtra("erroredArray", r7);
            r2 = android.os.SystemClock.elapsedRealtime();
            r13 = TAG;
            r14 = new java.lang.StringBuilder();
            r14.append("sendTetherStateChangedBroadcast: broadcastId= ");
            r14.append(r2);
            android.util.Log.d(r13, r14.toString());
            r0.putExtra("broadcastId", r2);
            r1.mContext.sendStickyBroadcastAsUser(r0, android.os.UserHandle.ALL);
     */
    /* JADX WARNING: Missing block: B:67:0x0152, code skipped:
            if (DBG == false) goto L_0x01a7;
     */
    /* JADX WARNING: Missing block: B:68:0x0154, code skipped:
            r13 = TAG;
            r15 = new java.lang.Object[8];
            r20 = r0;
            r15[1] = android.text.TextUtils.join(",", r4);
            r15[2] = "local_only";
            r15[3] = android.text.TextUtils.join(",", r6);
            r15[4] = "tether";
            r15[5] = android.text.TextUtils.join(",", r5);
            r15[6] = "error";
            r15[7] = android.text.TextUtils.join(",", r7);
            android.util.Log.d(r13, java.lang.String.format("sendTetherStateChangedBroadcast %s=[%s] %s=[%s] %s=[%s] %s=[%s]", r15));
     */
    /* JADX WARNING: Missing block: B:69:0x01a7, code skipped:
            r20 = r0;
     */
    /* JADX WARNING: Missing block: B:70:0x01a9, code skipped:
            r1.mHwNotificationTethering.setTetheringNumber(r8);
     */
    /* JADX WARNING: Missing block: B:71:0x01ae, code skipped:
            if (r9 != false) goto L_0x01bb;
     */
    /* JADX WARNING: Missing block: B:72:0x01b0, code skipped:
            if (r10 != false) goto L_0x01bb;
     */
    /* JADX WARNING: Missing block: B:73:0x01b2, code skipped:
            if (r11 != false) goto L_0x01bb;
     */
    /* JADX WARNING: Missing block: B:74:0x01b4, code skipped:
            if (r18 == false) goto L_0x01b7;
     */
    /* JADX WARNING: Missing block: B:75:0x01b7, code skipped:
            clearTetheredNotification();
     */
    /* JADX WARNING: Missing block: B:76:0x01bb, code skipped:
            r1.mNotificationType = r1.mHwNotificationTethering.getNotificationType(r8);
            showTetheredNotification(r1.mHwNotificationTethering.getNotificationIcon(r1.mNotificationType));
     */
    /* JADX WARNING: Missing block: B:77:0x01cf, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void sendTetherStateChangedBroadcast() {
        int waitRetry;
        boolean p2pTethered;
        Throwable th;
        int waitRetry2 = 0;
        while (true) {
            waitRetry = waitRetry2;
            if (waitRetry < 10 && this.mDeps == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sleep to wait ConnectivityManager init completely:");
                stringBuilder.append(waitRetry);
                Log.e(str, stringBuilder.toString());
                try {
                    Thread.sleep(100);
                    waitRetry2 = waitRetry + 1;
                } catch (InterruptedException ignore) {
                    InterruptedException interruptedException = ignore;
                    Log.e(TAG, "exception happened");
                    waitRetry2 = waitRetry;
                }
            }
        }
        if (this.mDeps == null) {
            Log.e(TAG, "start ConnectivityManager exception");
        } else if (this.mDeps.isTetheringSupported()) {
            ArrayList<String> availableList = new ArrayList();
            ArrayList<String> tetherList = new ArrayList();
            ArrayList<String> localOnlyList = new ArrayList();
            ArrayList<String> erroredList = new ArrayList();
            ArrayList<String> tetheringNumbers = new ArrayList();
            boolean usbTethered = false;
            TetheringConfiguration cfg = this.mConfig;
            synchronized (this.mPublicSync) {
                boolean p2pTethered2 = false;
                boolean bluetoothTethered = false;
                boolean wifiTethered = false;
                waitRetry2 = 0;
                while (waitRetry2 < this.mTetherStates.size()) {
                    int waitRetry3;
                    try {
                        TetherState tetherState = (TetherState) this.mTetherStates.valueAt(waitRetry2);
                        String iface = (String) this.mTetherStates.keyAt(waitRetry2);
                        waitRetry3 = waitRetry;
                        try {
                            if (tetherState.lastError != 0) {
                                try {
                                    erroredList.add(iface);
                                    p2pTethered = p2pTethered2;
                                } catch (Throwable th2) {
                                    th = th2;
                                    throw th;
                                }
                            }
                            p2pTethered = p2pTethered2;
                            if (tetherState.lastState) {
                                try {
                                    availableList.add(iface);
                                } catch (Throwable th3) {
                                    th = th3;
                                    p2pTethered2 = p2pTethered;
                                    throw th;
                                }
                            } else if (tetherState.lastState == 3) {
                                localOnlyList.add(iface);
                            } else if (tetherState.lastState == 2) {
                                if (cfg.isUsb(iface)) {
                                    usbTethered = true;
                                    tetheringNumbers.add("usb");
                                } else if (cfg.isWifi(iface)) {
                                    wifiTethered = true;
                                    tetheringNumbers.add("wifi");
                                } else if (cfg.isBluetooth(iface)) {
                                    bluetoothTethered = true;
                                    tetheringNumbers.add("bluetooth");
                                } else if (HwServiceFactory.getHwConnectivityManager().isP2pTether(iface)) {
                                    p2pTethered2 = true;
                                    tetheringNumbers.add("p2p");
                                    tetherList.add(iface);
                                    waitRetry2++;
                                    waitRetry = waitRetry3;
                                }
                                p2pTethered2 = p2pTethered;
                                tetherList.add(iface);
                                waitRetry2++;
                                waitRetry = waitRetry3;
                            }
                            p2pTethered2 = p2pTethered;
                            waitRetry2++;
                            waitRetry = waitRetry3;
                        } catch (Throwable th4) {
                            th = th4;
                            p2pTethered = p2pTethered2;
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        waitRetry3 = waitRetry;
                        p2pTethered = p2pTethered2;
                        throw th;
                    }
                }
                p2pTethered = p2pTethered2;
            }
        }
    }

    private void showTetheredNotification(int id) {
        showTetheredNotification(id, true);
    }

    @VisibleForTesting
    protected void showTetheredNotification(int id, boolean tetheringOn) {
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("showTetheredNotification icon:");
            stringBuilder.append(id);
            Log.d(str, stringBuilder.toString());
        }
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager != null) {
            int icon;
            switch (id) {
                case 15:
                    icon = 17303547;
                    break;
                case 16:
                    icon = 17303545;
                    break;
                default:
                    icon = 17303546;
                    break;
            }
            if (this.mLastNotificationId != 0) {
                if (this.mLastNotificationId == icon) {
                    this.mHwNotificationTethering.sendTetherNotification();
                    return;
                } else {
                    notificationManager.cancelAsUser(null, this.mLastNotificationId, UserHandle.ALL);
                    this.mLastNotificationId = 0;
                }
            }
            PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, this.mHwNotificationTethering.getNotificationIntent(this.mNotificationType), 0, null, UserHandle.CURRENT);
            PendingIntent pIntentCancel = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.server.connectivity.action.STOP_TETHERING"), 134217728);
            Resources r = Resources.getSystem();
            CharSequence title = this.mHwNotificationTethering.getNotificationTitle(this.mNotificationType);
            CharSequence message = r.getText(33685848);
            CharSequence action_text = this.mHwNotificationTethering.getNotificationActionText(this.mNotificationType);
            if (this.mTetheredNotificationBuilder == null) {
                this.mTetheredNotificationBuilder = new Builder(this.mContext, SystemNotificationChannels.NETWORK_STATUS);
                this.mTetheredNotificationBuilder.setWhen(0).setOngoing(false).setVisibility(1).setCategory("status").addAction(new Action(0, action_text, pIntentCancel));
            }
            this.mTetheredNotificationBuilder.setSmallIcon(getNotificationBitampIcon(id)).setContentTitle(title).setContentText(message).setContentIntent(pi);
            Notification notification = this.mTetheredNotificationBuilder.build();
            notification.icon = id;
            this.mLastNotificationId = id;
            this.mHwNotificationTethering.showTetheredNotification(this.mNotificationType, notification, pi);
        }
    }

    private Icon getNotificationBitampIcon(int resId) {
        Config config;
        Drawable drawable = this.mContext.getResources().getDrawable(resId);
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (drawable.getOpacity() != -1) {
            config = Config.ARGB_8888;
        } else {
            config = Config.RGB_565;
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return Icon.createWithBitmap(bitmap);
    }

    @VisibleForTesting
    protected void clearTetheredNotification() {
        if (DBG) {
            Log.d(TAG, "clearTetheredNotification");
        }
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager != null && this.mLastNotificationId != 0) {
            notificationManager.cancelAsUser(null, this.mLastNotificationId, UserHandle.ALL);
            this.mLastNotificationId = 0;
            this.mHwNotificationTethering.clearTetheredNotification();
        }
    }

    private void disableWifiIpServingLocked(String ifname, int apState) {
        String str;
        SharedLog sharedLog = this.mLog;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Canceling WiFi tethering request - AP_STATE=");
        stringBuilder.append(apState);
        sharedLog.log(stringBuilder.toString());
        int i = 0;
        this.mWifiTetherRequested = false;
        if (!TextUtils.isEmpty(ifname)) {
            TetherState ts = (TetherState) this.mTetherStates.get(ifname);
            if (ts != null) {
                ts.stateMachine.unwanted();
                return;
            }
        }
        while (i < this.mTetherStates.size()) {
            TetherInterfaceStateMachine tism = ((TetherState) this.mTetherStates.valueAt(i)).stateMachine;
            if (tism.interfaceType() == 0) {
                tism.unwanted();
                return;
            }
            i++;
        }
        sharedLog = this.mLog;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Error disabling Wi-Fi IP serving; ");
        if (TextUtils.isEmpty(ifname)) {
            str = "no interface name specified";
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("specified interface: ");
            stringBuilder2.append(ifname);
            str = stringBuilder2.toString();
        }
        stringBuilder.append(str);
        sharedLog.log(stringBuilder.toString());
    }

    private void enableWifiIpServingLocked(String ifname, int wifiIpMode) {
        int ipServingMode;
        switch (wifiIpMode) {
            case 1:
                ipServingMode = 2;
                break;
            case 2:
                ipServingMode = 3;
                break;
            default:
                SharedLog sharedLog = this.mLog;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot enable IP serving in unknown WiFi mode: ");
                stringBuilder.append(wifiIpMode);
                sharedLog.e(stringBuilder.toString());
                return;
        }
        if (TextUtils.isEmpty(ifname)) {
            this.mLog.e(String.format("Cannot enable IP serving in mode %s on missing interface name", new Object[]{Integer.valueOf(ipServingMode)}));
        } else {
            maybeTrackNewInterfaceLocked(ifname, 0);
            changeInterfaceState(ifname, ipServingMode);
        }
    }

    private void tetherMatchingInterfaces(int requestedState, int interfaceType) {
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("tetherMatchingInterfaces(");
            stringBuilder.append(requestedState);
            stringBuilder.append(", ");
            stringBuilder.append(interfaceType);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        try {
            String[] ifaces = this.mNMService.listInterfaces();
            String chosenIface = null;
            if (ifaces != null) {
                for (String iface : ifaces) {
                    if (ifaceNameToType(iface) == interfaceType) {
                        chosenIface = iface;
                        break;
                    }
                }
            }
            if (chosenIface == null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("could not find iface of type ");
                stringBuilder2.append(interfaceType);
                Log.e(str2, stringBuilder2.toString());
                return;
            }
            changeInterfaceState(chosenIface, requestedState);
        } catch (Exception e) {
            Log.e(TAG, "Error listing Interfaces", e);
        }
    }

    private void changeInterfaceState(String ifname, int requestedState) {
        int result;
        switch (requestedState) {
            case 0:
            case 1:
                result = untether(ifname);
                break;
            case 2:
            case 3:
                result = tether(ifname, requestedState);
                break;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown interface state: ");
                stringBuilder.append(requestedState);
                Log.wtf(str, stringBuilder.toString());
                return;
        }
        if (result != 0) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("unable start or stop tethering on iface ");
            stringBuilder2.append(ifname);
            Log.e(str2, stringBuilder2.toString());
        }
    }

    public TetheringConfiguration getTetheringConfiguration() {
        return this.mConfig;
    }

    public boolean hasTetherableConfiguration() {
        TetheringConfiguration cfg = this.mConfig;
        boolean hasDownstreamConfiguration = (cfg.tetherableUsbRegexs.length == 0 && cfg.tetherableWifiRegexs.length == 0 && cfg.tetherableBluetoothRegexs.length == 0) ? false : true;
        boolean hasUpstreamConfiguration = cfg.preferredUpstreamIfaceTypes.isEmpty() ^ true;
        if (hasDownstreamConfiguration && hasUpstreamConfiguration) {
            return true;
        }
        return false;
    }

    public String[] getTetherableUsbRegexs() {
        return copy(this.mConfig.tetherableUsbRegexs);
    }

    public String[] getTetherableWifiRegexs() {
        return copy(this.mConfig.tetherableWifiRegexs);
    }

    public String[] getTetherableBluetoothRegexs() {
        return copy(this.mConfig.tetherableBluetoothRegexs);
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:7:0x0032, B:12:0x003b, B:25:0x0063] */
    /* JADX WARNING: Missing block: B:17:0x0044, code skipped:
            android.os.Binder.restoreCallingIdentity(r4);
     */
    /* JADX WARNING: Missing block: B:35:0x0080, code skipped:
            android.os.Binder.restoreCallingIdentity(r4);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int setUsbTethering(boolean enable) {
        if (VDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setUsbTethering(");
            stringBuilder.append(enable);
            stringBuilder.append(")");
            Log.d(str, stringBuilder.toString());
        }
        UsbManager usbManager = (UsbManager) this.mContext.getSystemService("usb");
        synchronized (this.mPublicSync) {
            long ident;
            if (!enable) {
                ident = Binder.clearCallingIdentity();
                tetherMatchingInterfaces(1, 1);
                Binder.restoreCallingIdentity(ident);
                if (this.mRndisEnabled) {
                    usbManager.setCurrentFunction(null, false);
                }
                HwServiceFactory.getHwConnectivityManager().setUsbFunctionForTethering(this.mContext, usbManager, false);
                this.mUsbTetherRequested = false;
            } else if (this.mRndisEnabled) {
                ident = Binder.clearCallingIdentity();
                tetherMatchingInterfaces(2, 1);
                Binder.restoreCallingIdentity(ident);
            } else {
                this.mUsbTetherRequested = true;
                if (!HwServiceFactory.getHwConnectivityManager().setUsbFunctionForTethering(this.mContext, usbManager, true)) {
                    usbManager.setCurrentFunction("rndis", false);
                }
            }
        }
        return 0;
    }

    public String[] getTetheredIfaces() {
        ArrayList<String> list = new ArrayList();
        synchronized (this.mPublicSync) {
            for (int i = 0; i < this.mTetherStates.size(); i++) {
                if (((TetherState) this.mTetherStates.valueAt(i)).lastState == 2) {
                    list.add((String) this.mTetherStates.keyAt(i));
                }
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    public String[] getTetherableIfaces() {
        ArrayList<String> list = new ArrayList();
        synchronized (this.mPublicSync) {
            for (int i = 0; i < this.mTetherStates.size(); i++) {
                if (((TetherState) this.mTetherStates.valueAt(i)).lastState == 1) {
                    list.add((String) this.mTetherStates.keyAt(i));
                }
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    public String[] getTetheredDhcpRanges() {
        return this.mConfig.dhcpRanges;
    }

    public String[] getErroredIfaces() {
        ArrayList<String> list = new ArrayList();
        synchronized (this.mPublicSync) {
            for (int i = 0; i < this.mTetherStates.size(); i++) {
                if (((TetherState) this.mTetherStates.valueAt(i)).lastError != 0) {
                    list.add((String) this.mTetherStates.keyAt(i));
                }
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    private void logMessage(State state, int what) {
        SharedLog sharedLog = this.mLog;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(state.getName());
        stringBuilder.append(" got ");
        stringBuilder.append((String) sMagicDecoderRing.get(what, Integer.toString(what)));
        sharedLog.log(stringBuilder.toString());
    }

    private boolean upstreamWanted() {
        if (!this.mForwardedDownstreams.isEmpty()) {
            return true;
        }
        boolean z;
        synchronized (this.mPublicSync) {
            z = this.mWifiTetherRequested;
        }
        return z;
    }

    private boolean pertainsToCurrentUpstream(NetworkState ns) {
        if (!(ns == null || ns.linkProperties == null || this.mCurrentUpstreamIfaceSet == null)) {
            for (String ifname : ns.linkProperties.getAllInterfaceNames()) {
                if (this.mCurrentUpstreamIfaceSet.ifnames.contains(ifname)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void reevaluateSimCardProvisioning() {
        if (this.mConfig.hasMobileHotspotProvisionApp() && !carrierConfigAffirmsEntitlementCheckNotRequired()) {
            ArrayList<Integer> tethered = new ArrayList();
            synchronized (this.mPublicSync) {
                for (int i = 0; i < this.mTetherStates.size(); i++) {
                    if (((TetherState) this.mTetherStates.valueAt(i)).lastState == 2) {
                        int interfaceType = ifaceNameToType((String) this.mTetherStates.keyAt(i));
                        if (interfaceType != -1) {
                            tethered.add(Integer.valueOf(interfaceType));
                        }
                    }
                }
            }
            Iterator it = tethered.iterator();
            while (it.hasNext()) {
                startProvisionIntent(((Integer) it.next()).intValue());
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("Tethering:");
            pw.increaseIndent();
            pw.println("Configuration:");
            pw.increaseIndent();
            this.mConfig.dump(pw);
            pw.decreaseIndent();
            synchronized (this.mPublicSync) {
                pw.println("Tether state:");
                pw.increaseIndent();
                for (int i = 0; i < this.mTetherStates.size(); i++) {
                    String iface = (String) this.mTetherStates.keyAt(i);
                    TetherState tetherState = (TetherState) this.mTetherStates.valueAt(i);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(iface);
                    stringBuilder.append(" - ");
                    pw.print(stringBuilder.toString());
                    switch (tetherState.lastState) {
                        case 0:
                            pw.print("UnavailableState");
                            break;
                        case 1:
                            pw.print("AvailableState");
                            break;
                        case 2:
                            pw.print("TetheredState");
                            break;
                        case 3:
                            pw.print("LocalHotspotState");
                            break;
                        default:
                            pw.print("UnknownState");
                            break;
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" - lastError = ");
                    stringBuilder.append(tetherState.lastError);
                    pw.println(stringBuilder.toString());
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Upstream wanted: ");
                stringBuilder2.append(upstreamWanted());
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Current upstream interface(s): ");
                stringBuilder2.append(this.mCurrentUpstreamIfaceSet);
                pw.println(stringBuilder2.toString());
                pw.decreaseIndent();
            }
            pw.println("Hardware offload:");
            pw.increaseIndent();
            this.mOffloadController.dump(pw);
            pw.decreaseIndent();
            pw.println("Log:");
            pw.increaseIndent();
            if (argsContain(args, ConnectivityService.SHORT_ARG)) {
                pw.println("<log removed for brevity>");
            } else {
                this.mLog.dump(fd, pw, args);
            }
            pw.decreaseIndent();
            pw.decreaseIndent();
        }
    }

    private static boolean argsContain(String[] args, String target) {
        for (String arg : args) {
            if (target.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private IControlsTethering makeControlCallback(final String ifname) {
        return new IControlsTethering() {
            public void updateInterfaceState(TetherInterfaceStateMachine who, int state, int lastError) {
                Tethering.this.notifyInterfaceStateChange(ifname, who, state, lastError);
            }

            public void updateLinkProperties(TetherInterfaceStateMachine who, LinkProperties newLp) {
                Tethering.this.notifyLinkPropertiesChanged(ifname, who, newLp);
            }
        };
    }

    private void notifyInterfaceStateChange(String iface, TetherInterfaceStateMachine who, int state, int error) {
        int which;
        synchronized (this.mPublicSync) {
            TetherState tetherState = (TetherState) this.mTetherStates.get(iface);
            if (tetherState != null && tetherState.stateMachine.equals(who)) {
                tetherState.lastState = state;
                tetherState.lastError = error;
            } else if (DBG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("got notification from stale iface ");
                stringBuilder.append(iface);
                Log.d(str, stringBuilder.toString());
            }
        }
        SharedLog sharedLog = this.mLog;
        r2 = new Object[3];
        boolean z = true;
        r2[1] = Integer.valueOf(state);
        r2[2] = Integer.valueOf(error);
        sharedLog.log(String.format("OBSERVED iface=%s state=%s error=%s", r2));
        try {
            this.mPolicyManager.onTetheringChanged(iface, state == 2);
        } catch (RemoteException e) {
        }
        if (error == 5) {
            this.mTetherMasterSM.sendMessage(327686, who);
        }
        switch (state) {
            case 0:
            case 1:
                which = 327682;
                HwConnectivityManager hwConnectivityManager = HwServiceFactory.getHwConnectivityManager();
                if (1 != ifaceNameToType(iface)) {
                    z = false;
                }
                hwConnectivityManager.setTetheringProp(this, false, z, iface);
                Log.d(TAG, "setApIpv4AddressFixed false");
                HwServiceFactory.getHwConnectivityManager().setApIpv4AddressFixed(false);
                break;
            case 2:
            case 3:
                which = 327681;
                HwServiceFactory.getHwConnectivityManager().setTetheringProp(this, true, 1 == ifaceNameToType(iface), iface);
                break;
            default:
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unknown interface state: ");
                stringBuilder2.append(state);
                Log.wtf(str2, stringBuilder2.toString());
                return;
        }
        this.mTetherMasterSM.sendMessage(which, state, 0, who);
        sendTetherStateChangedBroadcast();
    }

    private void notifyLinkPropertiesChanged(String iface, TetherInterfaceStateMachine who, LinkProperties newLp) {
        synchronized (this.mPublicSync) {
            TetherState tetherState = (TetherState) this.mTetherStates.get(iface);
            if (tetherState == null || !tetherState.stateMachine.equals(who)) {
                SharedLog sharedLog = this.mLog;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("got notification from stale iface ");
                stringBuilder.append(iface);
                sharedLog.log(stringBuilder.toString());
                return;
            }
            int state = tetherState.lastState;
            this.mLog.log(String.format("OBSERVED LinkProperties update iface=%s state=%s lp=%s", new Object[]{iface, IControlsTethering.getStateString(state), newLp}));
            this.mTetherMasterSM.sendMessage(327687, state, 0, newLp);
        }
    }

    private void maybeTrackNewInterfaceLocked(String iface) {
        int interfaceType = ifaceNameToType(iface);
        if (interfaceType == -1) {
            SharedLog sharedLog = this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(iface);
            stringBuilder.append(" is not a tetherable iface, ignoring");
            sharedLog.log(stringBuilder.toString());
            return;
        }
        maybeTrackNewInterfaceLocked(iface, interfaceType);
    }

    private void maybeTrackNewInterfaceLocked(String iface, int interfaceType) {
        SharedLog sharedLog;
        StringBuilder stringBuilder;
        if (this.mTetherStates.containsKey(iface)) {
            sharedLog = this.mLog;
            stringBuilder = new StringBuilder();
            stringBuilder.append("active iface (");
            stringBuilder.append(iface);
            stringBuilder.append(") reported as added, ignoring");
            sharedLog.log(stringBuilder.toString());
            return;
        }
        sharedLog = this.mLog;
        stringBuilder = new StringBuilder();
        stringBuilder.append("adding TetheringInterfaceStateMachine for: ");
        stringBuilder.append(iface);
        sharedLog.log(stringBuilder.toString());
        TetherState tetherState = new TetherState(new TetherInterfaceStateMachine(iface, this.mLooper, interfaceType, this.mLog, this.mNMService, this.mStatsService, makeControlCallback(iface), this.mDeps));
        this.mTetherStates.put(iface, tetherState);
        tetherState.stateMachine.start();
    }

    private void stopTrackingInterfaceLocked(String iface) {
        TetherState tetherState = (TetherState) this.mTetherStates.get(iface);
        SharedLog sharedLog;
        StringBuilder stringBuilder;
        if (tetherState == null) {
            sharedLog = this.mLog;
            stringBuilder = new StringBuilder();
            stringBuilder.append("attempting to remove unknown iface (");
            stringBuilder.append(iface);
            stringBuilder.append("), ignoring");
            sharedLog.log(stringBuilder.toString());
            return;
        }
        tetherState.stateMachine.stop();
        sharedLog = this.mLog;
        stringBuilder = new StringBuilder();
        stringBuilder.append("removing TetheringInterfaceStateMachine for: ");
        stringBuilder.append(iface);
        sharedLog.log(stringBuilder.toString());
        this.mTetherStates.remove(iface);
    }

    private static String getIPv4DefaultRouteInterface(NetworkState ns) {
        if (ns == null) {
            return null;
        }
        return getInterfaceForDestination(ns.linkProperties, Inet4Address.ANY);
    }

    private static String getIPv6DefaultRouteInterface(NetworkState ns) {
        if (ns == null || ns.networkCapabilities == null || !ns.networkCapabilities.hasTransport(0)) {
            return null;
        }
        return getInterfaceForDestination(ns.linkProperties, Inet6Address.ANY);
    }

    private static String getInterfaceForDestination(LinkProperties lp, InetAddress dst) {
        RouteInfo ri;
        if (lp != null) {
            ri = RouteInfo.selectBestRoute(lp.getAllRoutes(), dst);
        } else {
            ri = null;
        }
        if (ri != null) {
            return ri.getInterface();
        }
        return null;
    }

    private static String[] copy(String[] strarray) {
        return (String[]) Arrays.copyOf(strarray, strarray.length);
    }
}
