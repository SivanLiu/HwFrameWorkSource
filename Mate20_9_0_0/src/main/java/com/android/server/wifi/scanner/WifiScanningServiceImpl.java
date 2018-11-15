package com.android.server.wifi.scanner;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.IWifiScanner.Stub;
import android.net.wifi.ScanResult;
import android.net.wifi.ScanResult.InformationElement;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiScanner.ChannelSpec;
import android.net.wifi.WifiScanner.OperationResult;
import android.net.wifi.WifiScanner.ParcelableScanData;
import android.net.wifi.WifiScanner.ParcelableScanResults;
import android.net.wifi.WifiScanner.PnoSettings;
import android.net.wifi.WifiScanner.ScanData;
import android.os.Binder;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.Clock;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.WifiConnectivityHelper;
import com.android.server.wifi.WifiConnectivityManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiLog;
import com.android.server.wifi.WifiMetrics;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiNative.BucketSettings;
import com.android.server.wifi.WifiNative.HiddenNetwork;
import com.android.server.wifi.WifiNative.PnoEventHandler;
import com.android.server.wifi.WifiNative.PnoNetwork;
import com.android.server.wifi.WifiNative.ScanCapabilities;
import com.android.server.wifi.WifiNative.ScanEventHandler;
import com.android.server.wifi.WifiNative.ScanSettings;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.hotspot2.anqp.NAIRealmData;
import com.android.server.wifi.scanner.ChannelHelper.ChannelCollection;
import com.android.server.wifi.scanner.WifiScannerImpl.WifiScannerImplFactory;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.WifiHandler;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class WifiScanningServiceImpl extends Stub {
    private static final int BASE = 160000;
    private static final int CMD_DRIVER_LOADED = 160006;
    private static final int CMD_DRIVER_UNLOADED = 160007;
    private static final int CMD_FULL_SCAN_RESULTS = 160001;
    private static final int CMD_PNO_NETWORK_FOUND = 160011;
    private static final int CMD_PNO_SCAN_FAILED = 160012;
    private static final int CMD_SCAN_FAILED = 160010;
    private static final int CMD_SCAN_PAUSED = 160008;
    private static final int CMD_SCAN_RESTARTED = 160009;
    private static final int CMD_SCAN_RESULTS_AVAILABLE = 160000;
    private static final boolean DBG = false;
    private static final String TAG = "WifiScanningService";
    private static final int UNKNOWN_PID = -1;
    private static boolean mSendScanResultsBroadcast = false;
    private final AlarmManager mAlarmManager;
    private WifiBackgroundScanStateMachine mBackgroundScanStateMachine;
    private BackgroundScanScheduler mBackgroundScheduler;
    private final IBatteryStats mBatteryStats;
    private ChannelHelper mChannelHelper;
    private ClientHandler mClientHandler;
    private final ArrayMap<Messenger, ClientInfo> mClients;
    private final Clock mClock;
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private final LocalLog mLocalLog = new LocalLog(512);
    private WifiLog mLog;
    private final Looper mLooper;
    private WifiPnoScanStateMachine mPnoScanStateMachine;
    private ScanSettings mPreviousSchedule;
    private WifiScannerImpl mScannerImpl;
    private final WifiScannerImplFactory mScannerImplFactory;
    private final RequestList<Void> mSingleScanListeners = new RequestList(this, null);
    private WifiSingleScanStateMachine mSingleScanStateMachine;
    private final WifiMetrics mWifiMetrics;

    private abstract class ClientInfo {
        protected final Messenger mMessenger;
        private boolean mScanWorkReported = false;
        private final int mUid;
        private final WorkSource mWorkSource;

        public abstract void reportEvent(int i, int i2, int i3, Object obj);

        ClientInfo(int uid, Messenger messenger) {
            this.mUid = uid;
            this.mMessenger = messenger;
            this.mWorkSource = new WorkSource(uid);
        }

        public void register() {
            WifiScanningServiceImpl.this.mClients.put(this.mMessenger, this);
        }

        private void unregister() {
            WifiScanningServiceImpl.this.mClients.remove(this.mMessenger);
        }

        public void cleanup() {
            WifiScanningServiceImpl.this.mSingleScanListeners.removeAllForClient(this);
            WifiScanningServiceImpl.this.mSingleScanStateMachine.removeSingleScanRequests(this);
            WifiScanningServiceImpl.this.mBackgroundScanStateMachine.removeBackgroundScanSettings(this);
            unregister();
            WifiScanningServiceImpl wifiScanningServiceImpl = WifiScanningServiceImpl.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Successfully stopped all requests for client ");
            stringBuilder.append(this);
            wifiScanningServiceImpl.localLog(stringBuilder.toString());
        }

        public int getUid() {
            return this.mUid;
        }

        public void reportEvent(int what, int arg1, int arg2) {
            reportEvent(what, arg1, arg2, null);
        }

        private void reportBatchedScanStart() {
            if (this.mUid != 0) {
                try {
                    WifiScanningServiceImpl.this.mBatteryStats.noteWifiBatchedScanStartedFromSource(this.mWorkSource, getCsph());
                } catch (RemoteException e) {
                    WifiScanningServiceImpl wifiScanningServiceImpl = WifiScanningServiceImpl.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("failed to report scan work: ");
                    stringBuilder.append(e.toString());
                    wifiScanningServiceImpl.logw(stringBuilder.toString());
                }
            }
        }

        private void reportBatchedScanStop() {
            if (this.mUid != 0) {
                try {
                    WifiScanningServiceImpl.this.mBatteryStats.noteWifiBatchedScanStoppedFromSource(this.mWorkSource);
                } catch (RemoteException e) {
                    WifiScanningServiceImpl wifiScanningServiceImpl = WifiScanningServiceImpl.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("failed to cleanup scan work: ");
                    stringBuilder.append(e.toString());
                    wifiScanningServiceImpl.logw(stringBuilder.toString());
                }
            }
        }

        private int getCsph() {
            int totalScanDurationPerHour = 0;
            for (WifiScanner.ScanSettings settings : WifiScanningServiceImpl.this.mBackgroundScanStateMachine.getBackgroundScanSettings(this)) {
                totalScanDurationPerHour += WifiScanningServiceImpl.this.mChannelHelper.estimateScanDuration(settings) * (settings.periodInMs == 0 ? 1 : 3600000 / settings.periodInMs);
            }
            return totalScanDurationPerHour / ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS;
        }

        private void reportScanWorkUpdate() {
            if (this.mScanWorkReported) {
                reportBatchedScanStop();
                this.mScanWorkReported = false;
            }
            if (WifiScanningServiceImpl.this.mBackgroundScanStateMachine.getBackgroundScanSettings(this).isEmpty()) {
                reportBatchedScanStart();
                this.mScanWorkReported = true;
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ClientInfo[uid=");
            stringBuilder.append(this.mUid);
            stringBuilder.append(",");
            stringBuilder.append(this.mMessenger);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    private class RequestInfo<T> {
        final ClientInfo clientInfo;
        final int handlerId;
        final T settings;
        final WorkSource workSource;

        RequestInfo(ClientInfo clientInfo, int handlerId, WorkSource requestedWorkSource, T settings) {
            this.clientInfo = clientInfo;
            this.handlerId = handlerId;
            this.settings = settings;
            this.workSource = WifiScanningServiceImpl.this.computeWorkSource(clientInfo, requestedWorkSource);
        }

        void reportEvent(int what, int arg1, Object obj) {
            this.clientInfo.reportEvent(what, arg1, this.handlerId, obj);
        }
    }

    private class RequestList<T> extends ArrayList<RequestInfo<T>> {
        private RequestList() {
        }

        /* synthetic */ RequestList(WifiScanningServiceImpl x0, AnonymousClass1 x1) {
            this();
        }

        void addRequest(ClientInfo ci, int handler, WorkSource reqworkSource, T settings) {
            add(new RequestInfo(ci, handler, reqworkSource, settings));
        }

        T removeRequest(ClientInfo ci, int handlerId) {
            T removed = null;
            Iterator<RequestInfo<T>> iter = iterator();
            while (iter.hasNext()) {
                RequestInfo<T> entry = (RequestInfo) iter.next();
                if (entry.clientInfo == ci && entry.handlerId == handlerId) {
                    removed = entry.settings;
                    iter.remove();
                }
            }
            return removed;
        }

        Collection<T> getAllSettings() {
            ArrayList<T> settingsList = new ArrayList();
            Iterator<RequestInfo<T>> iter = iterator();
            while (iter.hasNext()) {
                settingsList.add(((RequestInfo) iter.next()).settings);
            }
            return settingsList;
        }

        Collection<T> getAllSettingsForClient(ClientInfo ci) {
            ArrayList<T> settingsList = new ArrayList();
            Iterator<RequestInfo<T>> iter = iterator();
            while (iter.hasNext()) {
                RequestInfo<T> entry = (RequestInfo) iter.next();
                if (entry.clientInfo == ci) {
                    settingsList.add(entry.settings);
                }
            }
            return settingsList;
        }

        void removeAllForClient(ClientInfo ci) {
            Iterator<RequestInfo<T>> iter = iterator();
            while (iter.hasNext()) {
                if (((RequestInfo) iter.next()).clientInfo == ci) {
                    iter.remove();
                }
            }
        }

        WorkSource createMergedWorkSource() {
            WorkSource mergedSource = new WorkSource();
            Iterator it = iterator();
            while (it.hasNext()) {
                mergedSource.add(((RequestInfo) it.next()).workSource);
            }
            return mergedSource;
        }
    }

    private class ClientHandler extends WifiHandler {
        ClientHandler(String tag, Looper looper) {
            super(tag, looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ExternalClientInfo client;
            WifiScanningServiceImpl wifiScanningServiceImpl;
            StringBuilder stringBuilder;
            switch (msg.what) {
                case 69633:
                    if (msg.replyTo == null) {
                        WifiScanningServiceImpl.this.logw("msg.replyTo is null");
                        return;
                    }
                    client = (ExternalClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                    if (client != null) {
                        WifiScanningServiceImpl wifiScanningServiceImpl2 = WifiScanningServiceImpl.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("duplicate client connection: ");
                        stringBuilder2.append(msg.sendingUid);
                        stringBuilder2.append(", messenger=");
                        stringBuilder2.append(msg.replyTo);
                        wifiScanningServiceImpl2.logw(stringBuilder2.toString());
                        client.mChannel.replyToMessage(msg, 69634, 3);
                        return;
                    }
                    AsyncChannel ac = WifiScanningServiceImpl.this.mFrameworkFacade.makeWifiAsyncChannel(WifiScanningServiceImpl.TAG);
                    ac.connected(WifiScanningServiceImpl.this.mContext, this, msg.replyTo);
                    client = new ExternalClientInfo(msg.sendingUid, msg.replyTo, ac);
                    client.register();
                    ac.replyToMessage(msg, 69634, 0);
                    WifiScanningServiceImpl wifiScanningServiceImpl3 = WifiScanningServiceImpl.this;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("client connected: ");
                    stringBuilder3.append(client);
                    wifiScanningServiceImpl3.localLog(stringBuilder3.toString());
                    return;
                case 69635:
                    client = (ExternalClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                    if (client != null) {
                        client.mChannel.disconnect();
                    }
                    return;
                case 69636:
                    client = (ExternalClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                    if (!(client == null || msg.arg1 == 2 || msg.arg1 == 3)) {
                        wifiScanningServiceImpl = WifiScanningServiceImpl.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("client disconnected: ");
                        stringBuilder.append(client);
                        stringBuilder.append(", reason: ");
                        stringBuilder.append(msg.arg1);
                        wifiScanningServiceImpl.localLog(stringBuilder.toString());
                        client.cleanup();
                    }
                    return;
                default:
                    try {
                        WifiScanningServiceImpl.this.enforceLocationHardwarePermission(msg.sendingUid);
                        if (msg.what == 159748) {
                            WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(Message.obtain(msg));
                            return;
                        } else if (msg.what == 159773) {
                            WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(Message.obtain(msg));
                            return;
                        } else {
                            ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                            if (ci == null) {
                                wifiScanningServiceImpl = WifiScanningServiceImpl.this;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Could not find client info for message ");
                                stringBuilder.append(msg.replyTo);
                                stringBuilder.append(", msg=");
                                stringBuilder.append(msg);
                                wifiScanningServiceImpl.loge(stringBuilder.toString());
                                WifiScanningServiceImpl.this.replyFailed(msg, -2, "Could not find listener");
                                return;
                            }
                            switch (msg.what) {
                                case 159746:
                                case 159747:
                                    WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(Message.obtain(msg));
                                    break;
                                case 159765:
                                    WifiScanningServiceImpl.this.localLog(WifiScanner.getScanKey(msg.arg2), "20", "start single scan!");
                                    WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(Message.obtain(msg));
                                    break;
                                case 159766:
                                    WifiScanningServiceImpl.this.localLog(WifiScanner.getScanKey(msg.arg2), "21", "stop single scan!");
                                    WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(Message.obtain(msg));
                                    break;
                                case 159768:
                                case 159769:
                                    WifiScanningServiceImpl.this.mPnoScanStateMachine.sendMessage(Message.obtain(msg));
                                    break;
                                case 159771:
                                    WifiScanningServiceImpl.this.logScanRequest("registerScanListener", ci, msg.arg2, null, null, null);
                                    WifiScanningServiceImpl.this.mSingleScanListeners.addRequest(ci, msg.arg2, null, null);
                                    WifiScanningServiceImpl.this.replySucceeded(msg);
                                    break;
                                case 159772:
                                    WifiScanningServiceImpl.this.logScanRequest("deregisterScanListener", ci, msg.arg2, null, null, null);
                                    WifiScanningServiceImpl.this.mSingleScanListeners.removeRequest(ci, msg.arg2);
                                    break;
                                default:
                                    WifiScanningServiceImpl.this.replyFailed(msg, -3, "Invalid request");
                                    break;
                            }
                            return;
                        }
                    } catch (SecurityException e) {
                        wifiScanningServiceImpl = WifiScanningServiceImpl.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("failed to authorize app: ");
                        stringBuilder.append(e);
                        wifiScanningServiceImpl.localLog(stringBuilder.toString());
                        WifiScanningServiceImpl.this.replyFailed(msg, -4, "Not authorized");
                        return;
                    }
            }
        }
    }

    private class ExternalClientInfo extends ClientInfo {
        private final AsyncChannel mChannel;
        private boolean mDisconnected = false;

        ExternalClientInfo(int uid, Messenger messenger, AsyncChannel c) {
            super(uid, messenger);
            this.mChannel = c;
        }

        public void reportEvent(int what, int arg1, int arg2, Object obj) {
            if (!this.mDisconnected) {
                this.mChannel.sendMessage(what, arg1, arg2, obj);
            }
        }

        public void cleanup() {
            this.mDisconnected = true;
            WifiScanningServiceImpl.this.mPnoScanStateMachine.removePnoSettings(this);
            super.cleanup();
        }
    }

    private class InternalClientInfo extends ClientInfo {
        private static final int INTERNAL_CLIENT_HANDLER = 0;

        InternalClientInfo(int requesterUid, Messenger messenger) {
            super(requesterUid, messenger);
        }

        public void reportEvent(int what, int arg1, int arg2, Object obj) {
            Message message = Message.obtain();
            message.what = what;
            message.arg1 = arg1;
            message.arg2 = arg2;
            message.obj = obj;
            try {
                this.mMessenger.send(message);
            } catch (RemoteException e) {
                WifiScanningServiceImpl wifiScanningServiceImpl = WifiScanningServiceImpl.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to send message: ");
                stringBuilder.append(what);
                wifiScanningServiceImpl.loge(stringBuilder.toString());
            }
        }

        public void sendRequestToClientHandler(int what, WifiScanner.ScanSettings settings, WorkSource workSource) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.arg2 = 0;
            if (settings != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("ScanSettings", settings);
                bundle.putParcelable("WorkSource", workSource);
                msg.obj = bundle;
            }
            msg.replyTo = this.mMessenger;
            msg.sendingUid = getUid();
            WifiScanningServiceImpl.this.mClientHandler.sendMessage(msg);
        }

        public void sendRequestToClientHandler(int what) {
            sendRequestToClientHandler(what, null, null);
        }

        public String toString() {
            return "InternalClientInfo[]";
        }
    }

    class WifiBackgroundScanStateMachine extends StateMachine implements ScanEventHandler {
        private final RequestList<WifiScanner.ScanSettings> mActiveBackgroundScans = new RequestList(WifiScanningServiceImpl.this, null);
        private final DefaultState mDefaultState = new DefaultState();
        private final PausedState mPausedState = new PausedState();
        private final StartedState mStartedState = new StartedState();

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
                WifiBackgroundScanStateMachine.this.mActiveBackgroundScans.clear();
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 159746:
                    case 159747:
                    case 159748:
                    case 159765:
                    case 159766:
                        WifiScanningServiceImpl.this.replyFailed(msg, -1, "not available");
                        break;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED /*160006*/:
                        WifiScanningServiceImpl.this.mScannerImpl = WifiScanningServiceImpl.this.mScannerImplFactory.create(WifiScanningServiceImpl.this.mContext, WifiScanningServiceImpl.this.mLooper, WifiScanningServiceImpl.this.mClock);
                        if (WifiScanningServiceImpl.this.mScannerImpl == null) {
                            WifiBackgroundScanStateMachine.this.loge("Failed to start bgscan scan state machine because scanner impl is null");
                            return true;
                        }
                        WifiScanningServiceImpl.this.mScannerImpl.setWifiScanLogger(WifiScanningServiceImpl.this.mLocalLog);
                        WifiScanningServiceImpl.this.mChannelHelper = WifiScanningServiceImpl.this.mScannerImpl.getChannelHelper();
                        WifiScanningServiceImpl.this.mBackgroundScheduler = new BackgroundScanScheduler(WifiScanningServiceImpl.this.mChannelHelper);
                        ScanCapabilities capabilities = new ScanCapabilities();
                        StringBuilder stringBuilder;
                        if (!WifiScanningServiceImpl.this.mScannerImpl.getScanCapabilities(capabilities)) {
                            WifiBackgroundScanStateMachine.this.loge("could not get scan capabilities");
                            return true;
                        } else if (capabilities.max_scan_buckets <= 0) {
                            WifiBackgroundScanStateMachine wifiBackgroundScanStateMachine = WifiBackgroundScanStateMachine.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("invalid max buckets in scan capabilities ");
                            stringBuilder.append(capabilities.max_scan_buckets);
                            wifiBackgroundScanStateMachine.loge(stringBuilder.toString());
                            return true;
                        } else {
                            WifiScanningServiceImpl.this.mBackgroundScheduler.setMaxBuckets(capabilities.max_scan_buckets);
                            WifiScanningServiceImpl.this.mBackgroundScheduler.setMaxApPerScan(capabilities.max_ap_cache_per_scan);
                            String str = WifiScanningServiceImpl.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("wifi driver loaded with scan capabilities: max buckets=");
                            stringBuilder.append(capabilities.max_scan_buckets);
                            Log.i(str, stringBuilder.toString());
                            WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mStartedState);
                            return true;
                        }
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED /*160007*/:
                        Log.i(WifiScanningServiceImpl.TAG, "wifi driver unloaded");
                        WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mDefaultState);
                        break;
                }
                return true;
            }
        }

        class PausedState extends State {
            PausedState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message msg) {
                if (msg.what != WifiScanningServiceImpl.CMD_SCAN_RESTARTED) {
                    WifiBackgroundScanStateMachine.this.deferMessage(msg);
                } else {
                    WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mStartedState);
                }
                return true;
            }
        }

        class StartedState extends State {
            StartedState() {
            }

            public void enter() {
            }

            public void exit() {
                WifiBackgroundScanStateMachine.this.sendBackgroundScanFailedToAllAndClear(-1, "Scan was interrupted");
                if (WifiScanningServiceImpl.this.mScannerImpl != null) {
                    WifiScanningServiceImpl.this.mScannerImpl.cleanup();
                }
            }

            public boolean processMessage(Message msg) {
                ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159746:
                        WifiScanningServiceImpl.this.mWifiMetrics.incrementBackgroundScanCount();
                        Bundle scanParams = msg.obj;
                        if (scanParams != null) {
                            scanParams.setDefusable(true);
                            if (!WifiBackgroundScanStateMachine.this.addBackgroundScanRequest(ci, msg.arg2, (WifiScanner.ScanSettings) scanParams.getParcelable("ScanSettings"), (WorkSource) scanParams.getParcelable("WorkSource"))) {
                                WifiScanningServiceImpl.this.replyFailed(msg, -3, "bad request");
                                break;
                            }
                            WifiScanningServiceImpl.this.replySucceeded(msg);
                            break;
                        }
                        WifiScanningServiceImpl.this.replyFailed(msg, -3, "params null");
                        return true;
                    case 159747:
                        WifiBackgroundScanStateMachine.this.removeBackgroundScanRequest(ci, msg.arg2);
                        break;
                    case 159748:
                        WifiBackgroundScanStateMachine.this.reportScanResults(WifiScanningServiceImpl.this.mScannerImpl.getLatestBatchedScanResults(true));
                        WifiScanningServiceImpl.this.replySucceeded(msg);
                        break;
                    case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS /*160000*/:
                        WifiBackgroundScanStateMachine.this.reportScanResults(WifiScanningServiceImpl.this.mScannerImpl.getLatestBatchedScanResults(true));
                        break;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS /*160001*/:
                        WifiBackgroundScanStateMachine.this.reportFullScanResult((ScanResult) msg.obj, msg.arg2);
                        break;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED /*160006*/:
                        Log.e(WifiScanningServiceImpl.TAG, "wifi driver loaded received while already loaded");
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED /*160007*/:
                        return false;
                    case WifiScanningServiceImpl.CMD_SCAN_PAUSED /*160008*/:
                        WifiBackgroundScanStateMachine.this.reportScanResults((ScanData[]) msg.obj);
                        WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mPausedState);
                        break;
                    case WifiScanningServiceImpl.CMD_SCAN_FAILED /*160010*/:
                        Log.e(WifiScanningServiceImpl.TAG, "WifiScanner background scan gave CMD_SCAN_FAILED");
                        WifiBackgroundScanStateMachine.this.sendBackgroundScanFailedToAllAndClear(-1, "Background Scan failed");
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        WifiBackgroundScanStateMachine(Looper looper) {
            super("WifiBackgroundScanStateMachine", looper);
            setLogRecSize(512);
            setLogOnlyTransitions(false);
            addState(this.mDefaultState);
            addState(this.mStartedState, this.mDefaultState);
            addState(this.mPausedState, this.mDefaultState);
            setInitialState(this.mDefaultState);
        }

        public Collection<WifiScanner.ScanSettings> getBackgroundScanSettings(ClientInfo ci) {
            return this.mActiveBackgroundScans.getAllSettingsForClient(ci);
        }

        public void removeBackgroundScanSettings(ClientInfo ci) {
            this.mActiveBackgroundScans.removeAllForClient(ci);
            updateSchedule();
        }

        public void onScanStatus(int event) {
            switch (event) {
                case 0:
                case 1:
                case 2:
                    sendMessage(WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS);
                    return;
                case 3:
                    sendMessage(WifiScanningServiceImpl.CMD_SCAN_FAILED);
                    return;
                default:
                    String str = WifiScanningServiceImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown scan status event: ");
                    stringBuilder.append(event);
                    Log.e(str, stringBuilder.toString());
                    return;
            }
        }

        public void onFullScanResult(ScanResult fullScanResult, int bucketsScanned) {
            sendMessage(WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS, 0, bucketsScanned, fullScanResult);
        }

        public void onScanPaused(ScanData[] scanData) {
            sendMessage(WifiScanningServiceImpl.CMD_SCAN_PAUSED, scanData);
        }

        public void onScanRestarted() {
            sendMessage(WifiScanningServiceImpl.CMD_SCAN_RESTARTED);
        }

        private boolean addBackgroundScanRequest(ClientInfo ci, int handler, WifiScanner.ScanSettings settings, WorkSource workSource) {
            StringBuilder stringBuilder;
            if (ci == null) {
                String str = WifiScanningServiceImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failing scan request ClientInfo not found ");
                stringBuilder.append(handler);
                Log.d(str, stringBuilder.toString());
                return false;
            } else if (settings.periodInMs < 1000) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failing scan request because periodInMs is ");
                stringBuilder2.append(settings.periodInMs);
                stringBuilder2.append(", min scan period is: ");
                stringBuilder2.append(1000);
                loge(stringBuilder2.toString());
                return false;
            } else if (settings.band == 0 && settings.channels == null) {
                loge("Channels was null with unspecified band");
                return false;
            } else if (settings.band == 0 && settings.channels.length == 0) {
                loge("No channels specified");
                return false;
            } else {
                int minSupportedPeriodMs = WifiScanningServiceImpl.this.mChannelHelper.estimateScanDuration(settings);
                if (settings.periodInMs < minSupportedPeriodMs) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failing scan request because minSupportedPeriodMs is ");
                    stringBuilder.append(minSupportedPeriodMs);
                    stringBuilder.append(" but the request wants ");
                    stringBuilder.append(settings.periodInMs);
                    loge(stringBuilder.toString());
                    return false;
                }
                if (!(settings.maxPeriodInMs == 0 || settings.maxPeriodInMs == settings.periodInMs)) {
                    if (settings.maxPeriodInMs < settings.periodInMs) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failing scan request because maxPeriodInMs is ");
                        stringBuilder.append(settings.maxPeriodInMs);
                        stringBuilder.append(" but less than periodInMs ");
                        stringBuilder.append(settings.periodInMs);
                        loge(stringBuilder.toString());
                        return false;
                    } else if (settings.maxPeriodInMs > 1024000) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failing scan request because maxSupportedPeriodMs is 1024000 but the request wants ");
                        stringBuilder.append(settings.maxPeriodInMs);
                        loge(stringBuilder.toString());
                        return false;
                    } else if (settings.stepCount < 1) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failing scan request because stepCount is ");
                        stringBuilder.append(settings.stepCount);
                        stringBuilder.append(" which is less than 1");
                        loge(stringBuilder.toString());
                        return false;
                    }
                }
                WifiScanningServiceImpl.this.logScanRequest("addBackgroundScanRequest", ci, handler, null, settings, null);
                this.mActiveBackgroundScans.addRequest(ci, handler, workSource, settings);
                if (updateSchedule()) {
                    return true;
                }
                this.mActiveBackgroundScans.removeRequest(ci, handler);
                WifiScanningServiceImpl.this.localLog("Failing scan request because failed to reset scan");
                return false;
            }
        }

        private boolean updateSchedule() {
            if (WifiScanningServiceImpl.this.mChannelHelper == null || WifiScanningServiceImpl.this.mBackgroundScheduler == null || WifiScanningServiceImpl.this.mScannerImpl == null) {
                loge("Failed to update schedule because WifiScanningService is not initialized");
                return false;
            }
            WifiScanningServiceImpl.this.mChannelHelper.updateChannels();
            WifiScanningServiceImpl.this.mBackgroundScheduler.updateSchedule(this.mActiveBackgroundScans.getAllSettings());
            ScanSettings schedule = WifiScanningServiceImpl.this.mBackgroundScheduler.getSchedule();
            if (ScanScheduleUtil.scheduleEquals(WifiScanningServiceImpl.this.mPreviousSchedule, schedule)) {
                Log.i("WifiScanLog", "schedule updated with no change");
                return true;
            }
            StringBuilder keys = new StringBuilder();
            Iterator it = this.mActiveBackgroundScans.iterator();
            while (it.hasNext()) {
                keys.append(WifiScanner.getScanKey(((RequestInfo) it.next()).handlerId));
            }
            schedule.handlerId = keys.toString();
            WifiScanningServiceImpl.this.mPreviousSchedule = schedule;
            StringBuilder stringBuilder;
            if (schedule.num_buckets == 0) {
                WifiScanningServiceImpl.this.mScannerImpl.stopBatchedScan();
                stringBuilder = new StringBuilder();
                stringBuilder.append(schedule.handlerId);
                stringBuilder.append("scan stopped");
                Log.i("WifiScanLog", stringBuilder.toString());
                return true;
            }
            WifiScanningServiceImpl wifiScanningServiceImpl = WifiScanningServiceImpl.this;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("starting scan: base period=");
            stringBuilder2.append(schedule.base_period_ms);
            stringBuilder2.append(", max ap per scan=");
            stringBuilder2.append(schedule.max_ap_per_scan);
            stringBuilder2.append(", batched scans=");
            stringBuilder2.append(schedule.report_threshold_num_scans);
            wifiScanningServiceImpl.localLog(stringBuilder2.toString());
            for (int b = 0; b < schedule.num_buckets; b++) {
                BucketSettings bucket = schedule.buckets[b];
                WifiScanningServiceImpl wifiScanningServiceImpl2 = WifiScanningServiceImpl.this;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("bucket ");
                stringBuilder3.append(bucket.bucket);
                stringBuilder3.append(" (");
                stringBuilder3.append(bucket.period_ms);
                stringBuilder3.append("ms)[");
                stringBuilder3.append(bucket.report_events);
                stringBuilder3.append("]: ");
                stringBuilder3.append(ChannelHelper.toString(bucket));
                wifiScanningServiceImpl2.localLog(stringBuilder3.toString());
            }
            if (WifiScanningServiceImpl.this.mScannerImpl.startBatchedScan(schedule, this)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(schedule.handlerId);
                stringBuilder.append("startBatchedScan success!");
                Log.i("WifiScanLog", stringBuilder.toString());
                return true;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(schedule.handlerId);
            stringBuilder.append("startBatchedScan failed!");
            Log.i("WifiScanLog", stringBuilder.toString());
            WifiScanningServiceImpl.this.mPreviousSchedule = null;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("error starting scan: base period=");
            stringBuilder4.append(schedule.base_period_ms);
            stringBuilder4.append(", max ap per scan=");
            stringBuilder4.append(schedule.max_ap_per_scan);
            stringBuilder4.append(", batched scans=");
            stringBuilder4.append(schedule.report_threshold_num_scans);
            loge(stringBuilder4.toString());
            for (int b2 = 0; b2 < schedule.num_buckets; b2++) {
                BucketSettings bucket2 = schedule.buckets[b2];
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("bucket ");
                stringBuilder2.append(bucket2.bucket);
                stringBuilder2.append(" (");
                stringBuilder2.append(bucket2.period_ms);
                stringBuilder2.append("ms)[");
                stringBuilder2.append(bucket2.report_events);
                stringBuilder2.append("]: ");
                stringBuilder2.append(ChannelHelper.toString(bucket2));
                loge(stringBuilder2.toString());
            }
            return false;
        }

        private void removeBackgroundScanRequest(ClientInfo ci, int handler) {
            if (ci != null) {
                WifiScanningServiceImpl.this.logScanRequest("removeBackgroundScanRequest", ci, handler, null, (WifiScanner.ScanSettings) this.mActiveBackgroundScans.removeRequest(ci, handler), null);
                updateSchedule();
            }
        }

        private void reportFullScanResult(ScanResult result, int bucketsScanned) {
            Iterator it = this.mActiveBackgroundScans.iterator();
            while (it.hasNext()) {
                RequestInfo<WifiScanner.ScanSettings> entry = (RequestInfo) it.next();
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                if (WifiScanningServiceImpl.this.mBackgroundScheduler.shouldReportFullScanResultForSettings(result, bucketsScanned, entry.settings)) {
                    ScanResult newResult = new ScanResult(result);
                    if (result.informationElements != null) {
                        newResult.informationElements = (InformationElement[]) result.informationElements.clone();
                    } else {
                        newResult.informationElements = null;
                    }
                    ci.reportEvent(159764, 0, handler, newResult);
                }
            }
        }

        private void reportScanResults(ScanData[] results) {
            if (results == null) {
                Log.d(WifiScanningServiceImpl.TAG, "The results is null, nothing to report.");
                return;
            }
            for (ScanData result : results) {
                if (!(result == null || result.getResults() == null)) {
                    if (result.getResults().length > 0) {
                        WifiScanningServiceImpl.this.mWifiMetrics.incrementNonEmptyScanResultCount();
                    } else {
                        WifiScanningServiceImpl.this.mWifiMetrics.incrementEmptyScanResultCount();
                    }
                }
            }
            Iterator it = this.mActiveBackgroundScans.iterator();
            while (it.hasNext()) {
                RequestInfo<WifiScanner.ScanSettings> entry = (RequestInfo) it.next();
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                ScanData[] resultsToDeliver = WifiScanningServiceImpl.this.mBackgroundScheduler.filterResultsForSettings(results, entry.settings);
                if (resultsToDeliver != null) {
                    WifiScanningServiceImpl.this.logCallback("backgroundScanResults", ci, handler, WifiScanningServiceImpl.describeForLog(resultsToDeliver));
                    ci.reportEvent(159749, 0, handler, new ParcelableScanData(resultsToDeliver));
                }
            }
        }

        private void sendBackgroundScanFailedToAllAndClear(int reason, String description) {
            Iterator it = this.mActiveBackgroundScans.iterator();
            while (it.hasNext()) {
                RequestInfo<WifiScanner.ScanSettings> entry = (RequestInfo) it.next();
                entry.clientInfo.reportEvent(159762, 0, entry.handlerId, new OperationResult(reason, description));
            }
            this.mActiveBackgroundScans.clear();
        }
    }

    class WifiPnoScanStateMachine extends StateMachine implements PnoEventHandler {
        private final RequestList<Pair<PnoSettings, WifiScanner.ScanSettings>> mActivePnoScans = new RequestList(WifiScanningServiceImpl.this, null);
        private final DefaultState mDefaultState = new DefaultState();
        private final HwPnoScanState mHwPnoScanState = new HwPnoScanState();
        private InternalClientInfo mInternalClientInfo;
        private final SingleScanState mSingleScanState = new SingleScanState();
        private final StartedState mStartedState = new StartedState();

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 159749:
                    case 159762:
                    case WifiScanningServiceImpl.CMD_PNO_NETWORK_FOUND /*160011*/:
                    case WifiScanningServiceImpl.CMD_PNO_SCAN_FAILED /*160012*/:
                        WifiPnoScanStateMachine wifiPnoScanStateMachine = WifiPnoScanStateMachine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unexpected message ");
                        stringBuilder.append(msg.what);
                        wifiPnoScanStateMachine.loge(stringBuilder.toString());
                        break;
                    case 159768:
                    case 159769:
                        WifiScanningServiceImpl.this.replyFailed(msg, -1, "not available");
                        break;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED /*160006*/:
                        if (WifiScanningServiceImpl.this.mScannerImpl != null) {
                            WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                            break;
                        }
                        WifiPnoScanStateMachine.this.loge("Failed to start pno scan state machine because scanner impl is null");
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED /*160007*/:
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mDefaultState);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        class HwPnoScanState extends State {
            HwPnoScanState() {
            }

            public void enter() {
            }

            public void exit() {
                WifiScanningServiceImpl.this.mScannerImpl.resetHwPnoList();
                WifiPnoScanStateMachine.this.removeInternalClient();
            }

            public boolean processMessage(Message msg) {
                ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159768:
                        Bundle pnoParams = msg.obj;
                        if (pnoParams != null) {
                            pnoParams.setDefusable(true);
                            if (!WifiPnoScanStateMachine.this.addHwPnoScanRequest(ci, msg.arg2, (WifiScanner.ScanSettings) pnoParams.getParcelable("ScanSettings"), (PnoSettings) pnoParams.getParcelable("PnoSettings"))) {
                                WifiScanningServiceImpl.this.replyFailed(msg, -3, "bad request");
                                WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                                break;
                            }
                            WifiScanningServiceImpl.this.replySucceeded(msg);
                            break;
                        }
                        WifiScanningServiceImpl.this.replyFailed(msg, -3, "params null");
                        return true;
                    case 159769:
                        WifiPnoScanStateMachine.this.removeHwPnoScanRequest(ci, msg.arg2);
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        break;
                    case WifiScanningServiceImpl.CMD_PNO_NETWORK_FOUND /*160011*/:
                        if (!WifiPnoScanStateMachine.this.isSingleScanNeeded(msg.obj)) {
                            WifiPnoScanStateMachine.this.reportPnoNetworkFound((ScanResult[]) msg.obj);
                            break;
                        }
                        WifiScanner.ScanSettings activeScanSettings = WifiPnoScanStateMachine.this.getScanSettings();
                        if (activeScanSettings != null) {
                            WifiPnoScanStateMachine.this.addSingleScanRequest(activeScanSettings);
                            WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mSingleScanState);
                            break;
                        }
                        WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "couldn't retrieve setting");
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        break;
                    case WifiScanningServiceImpl.CMD_PNO_SCAN_FAILED /*160012*/:
                        WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "pno scan failed");
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        class SingleScanState extends State {
            SingleScanState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message msg) {
                WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                int i = msg.what;
                if (i == 159749) {
                    ScanData[] scanDatas = msg.obj.getResults();
                    WifiPnoScanStateMachine.this.reportPnoNetworkFound(scanDatas[scanDatas.length - 1].getResults());
                    WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mHwPnoScanState);
                } else if (i != 159762) {
                    return false;
                } else {
                    WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "single scan failed");
                    WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                }
                return true;
            }
        }

        class StartedState extends State {
            StartedState() {
            }

            public void enter() {
            }

            public void exit() {
                WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "Scan was interrupted");
            }

            public boolean processMessage(Message msg) {
                WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                int i = msg.what;
                if (i == WifiScanningServiceImpl.CMD_DRIVER_LOADED) {
                    return true;
                }
                switch (i) {
                    case 159768:
                        Bundle pnoParams = msg.obj;
                        if (pnoParams != null) {
                            pnoParams.setDefusable(true);
                            if (!WifiScanningServiceImpl.this.mScannerImpl.isHwPnoSupported(((PnoSettings) pnoParams.getParcelable("PnoSettings")).isConnected)) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(WifiScanner.getScanKey(msg.arg2));
                                stringBuilder.append("isHwPnoSupported false");
                                Log.i("WifiScanLog", stringBuilder.toString());
                                WifiScanningServiceImpl.this.replyFailed(msg, -3, "not supported");
                                break;
                            }
                            WifiPnoScanStateMachine.this.deferMessage(msg);
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(WifiScanner.getScanKey(msg.arg2));
                            stringBuilder2.append("isHwPnoSupported true");
                            Log.i("WifiScanLog", stringBuilder2.toString());
                            WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mHwPnoScanState);
                            break;
                        }
                        WifiScanningServiceImpl.this.replyFailed(msg, -3, "params null");
                        return true;
                    case 159769:
                        WifiScanningServiceImpl.this.replyFailed(msg, -1, "no scan running");
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        WifiPnoScanStateMachine(Looper looper) {
            super("WifiPnoScanStateMachine", looper);
            setLogRecSize(256);
            setLogOnlyTransitions(false);
            addState(this.mDefaultState);
            addState(this.mStartedState, this.mDefaultState);
            addState(this.mHwPnoScanState, this.mStartedState);
            addState(this.mSingleScanState, this.mHwPnoScanState);
            setInitialState(this.mDefaultState);
        }

        public void removePnoSettings(ClientInfo ci) {
            this.mActivePnoScans.removeAllForClient(ci);
            transitionTo(this.mStartedState);
        }

        public void onPnoNetworkFound(ScanResult[] results) {
            sendMessage(WifiScanningServiceImpl.CMD_PNO_NETWORK_FOUND, 0, 0, results);
        }

        public void onPnoScanFailed() {
            sendMessage(WifiScanningServiceImpl.CMD_PNO_SCAN_FAILED, 0, 0, null);
        }

        private WifiNative.PnoSettings convertSettingsToPnoNative(WifiScanner.ScanSettings scanSettings, PnoSettings pnoSettings) {
            WifiNative.PnoSettings nativePnoSetting = new WifiNative.PnoSettings();
            nativePnoSetting.periodInMs = scanSettings.periodInMs;
            nativePnoSetting.min5GHzRssi = pnoSettings.min5GHzRssi;
            nativePnoSetting.min24GHzRssi = pnoSettings.min24GHzRssi;
            nativePnoSetting.initialScoreMax = pnoSettings.initialScoreMax;
            nativePnoSetting.currentConnectionBonus = pnoSettings.currentConnectionBonus;
            nativePnoSetting.sameNetworkBonus = pnoSettings.sameNetworkBonus;
            nativePnoSetting.secureBonus = pnoSettings.secureBonus;
            nativePnoSetting.band5GHzBonus = pnoSettings.band5GHzBonus;
            nativePnoSetting.isConnected = pnoSettings.isConnected;
            nativePnoSetting.networkList = new PnoNetwork[pnoSettings.networkList.length];
            for (int i = 0; i < pnoSettings.networkList.length; i++) {
                nativePnoSetting.networkList[i] = new PnoNetwork();
                nativePnoSetting.networkList[i].ssid = pnoSettings.networkList[i].ssid;
                nativePnoSetting.networkList[i].flags = pnoSettings.networkList[i].flags;
                nativePnoSetting.networkList[i].auth_bit_field = pnoSettings.networkList[i].authBitField;
            }
            return nativePnoSetting;
        }

        private WifiScanner.ScanSettings getScanSettings() {
            Iterator it = this.mActivePnoScans.getAllSettings().iterator();
            if (it.hasNext()) {
                return (WifiScanner.ScanSettings) ((Pair) it.next()).second;
            }
            return null;
        }

        private void removeInternalClient() {
            if (this.mInternalClientInfo != null) {
                this.mInternalClientInfo.cleanup();
                this.mInternalClientInfo = null;
                return;
            }
            Log.w(WifiScanningServiceImpl.TAG, "No Internal client for PNO");
        }

        private void addInternalClient(ClientInfo ci) {
            if (this.mInternalClientInfo == null) {
                this.mInternalClientInfo = new InternalClientInfo(ci.getUid(), new Messenger(getHandler()));
                this.mInternalClientInfo.register();
                return;
            }
            Log.w(WifiScanningServiceImpl.TAG, "Internal client for PNO already exists");
        }

        private void addPnoScanRequest(ClientInfo ci, int handler, WifiScanner.ScanSettings scanSettings, PnoSettings pnoSettings) {
            WifiScanningServiceImpl.this.localLog(WifiScanner.getScanKey(handler), "26", "addPnoScanRequest");
            this.mActivePnoScans.addRequest(ci, handler, WifiStateMachine.WIFI_WORK_SOURCE, Pair.create(pnoSettings, scanSettings));
            addInternalClient(ci);
        }

        private Pair<PnoSettings, WifiScanner.ScanSettings> removePnoScanRequest(ClientInfo ci, int handler) {
            return (Pair) this.mActivePnoScans.removeRequest(ci, handler);
        }

        private boolean addHwPnoScanRequest(ClientInfo ci, int handler, WifiScanner.ScanSettings scanSettings, PnoSettings pnoSettings) {
            StringBuilder stringBuilder;
            if (ci == null) {
                String str = WifiScanningServiceImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failing scan request ClientInfo not found ");
                stringBuilder.append(WifiScanner.getScanKey(handler));
                Log.d(str, stringBuilder.toString());
                return false;
            } else if (this.mActivePnoScans.isEmpty()) {
                if (WifiScanningServiceImpl.this.mScannerImpl.setHwPnoList(convertSettingsToPnoNative(scanSettings, pnoSettings), WifiScanningServiceImpl.this.mPnoScanStateMachine)) {
                    WifiScanningServiceImpl.this.logScanRequest("addHwPnoScanRequest", ci, handler, null, scanSettings, pnoSettings);
                    addPnoScanRequest(ci, handler, scanSettings, pnoSettings);
                    return true;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failing setHwPnoList");
                stringBuilder.append(WifiScanner.getScanKey(handler));
                loge(stringBuilder.toString());
                return false;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failing scan request because there is already an active scan");
                stringBuilder2.append(WifiScanner.getScanKey(handler));
                loge(stringBuilder2.toString());
                return false;
            }
        }

        private void removeHwPnoScanRequest(ClientInfo ci, int handler) {
            if (ci != null) {
                Pair<PnoSettings, WifiScanner.ScanSettings> settings = removePnoScanRequest(ci, handler);
                if (settings != null) {
                    WifiScanningServiceImpl.this.logScanRequest("removeHwPnoScanRequest", ci, handler, null, (WifiScanner.ScanSettings) settings.second, (PnoSettings) settings.first);
                    return;
                }
                Log.d(WifiScanningServiceImpl.TAG, "removeHwPnoScanRequest: settings is null");
            }
        }

        private void reportPnoNetworkFound(ScanResult[] results) {
            ParcelableScanResults parcelableScanResults = new ParcelableScanResults(results);
            Iterator it = this.mActivePnoScans.iterator();
            while (it.hasNext()) {
                RequestInfo<Pair<PnoSettings, WifiScanner.ScanSettings>> entry = (RequestInfo) it.next();
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                WifiScanningServiceImpl.this.logCallback("pnoNetworkFound", ci, handler, WifiScanningServiceImpl.describeForLog(results));
                ci.reportEvent(159770, 0, handler, parcelableScanResults);
            }
        }

        private void sendPnoScanFailedToAllAndClear(int reason, String description) {
            Iterator it = this.mActivePnoScans.iterator();
            while (it.hasNext()) {
                RequestInfo<Pair<PnoSettings, WifiScanner.ScanSettings>> entry = (RequestInfo) it.next();
                entry.clientInfo.reportEvent(159762, 0, entry.handlerId, new OperationResult(reason, description));
            }
            this.mActivePnoScans.clear();
        }

        private void addSingleScanRequest(WifiScanner.ScanSettings settings) {
            if (this.mInternalClientInfo != null) {
                this.mInternalClientInfo.sendRequestToClientHandler(159765, settings, WifiStateMachine.WIFI_WORK_SOURCE);
            }
        }

        private boolean isSingleScanNeeded(ScanResult[] scanResults) {
            for (ScanResult scanResult : scanResults) {
                if (scanResult.informationElements != null && scanResult.informationElements.length > 0) {
                    return false;
                }
            }
            return true;
        }
    }

    class WifiSingleScanStateMachine extends StateMachine implements ScanEventHandler {
        @VisibleForTesting
        public static final int CACHED_SCAN_RESULTS_MAX_AGE_IN_MILLIS = 180000;
        private ScanSettings mActiveScanSettings = null;
        private RequestList<WifiScanner.ScanSettings> mActiveScans = new RequestList(WifiScanningServiceImpl.this, null);
        private final List<ScanResult> mCachedScanResults = new ArrayList();
        private final DefaultState mDefaultState = new DefaultState();
        private final DriverStartedState mDriverStartedState = new DriverStartedState();
        private final IdleState mIdleState = new IdleState();
        private RequestList<WifiScanner.ScanSettings> mPendingScans = new RequestList(WifiScanningServiceImpl.this, null);
        private final ScanningState mScanningState = new ScanningState();

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
                WifiSingleScanStateMachine.this.mActiveScans.clear();
                WifiSingleScanStateMachine.this.mPendingScans.clear();
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 159765:
                    case 159766:
                        WifiScanningServiceImpl.this.replyFailed(msg, -1, "not available");
                        return true;
                    case 159773:
                        msg.obj = new ParcelableScanResults(filterCachedScanResultsByAge());
                        WifiScanningServiceImpl.this.replySucceeded(msg);
                        return true;
                    case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS /*160000*/:
                        return true;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS /*160001*/:
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED /*160006*/:
                        if (WifiScanningServiceImpl.this.mScannerImpl == null) {
                            WifiSingleScanStateMachine.this.loge("Failed to start single scan state machine because scanner impl is null");
                            return true;
                        }
                        WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mIdleState);
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED /*160007*/:
                        WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mDefaultState);
                        return true;
                    default:
                        return false;
                }
            }

            private ScanResult[] filterCachedScanResultsByAge() {
                ScanResult[] filterCachedScanResults = (ScanResult[]) WifiSingleScanStateMachine.this.mCachedScanResults.stream().filter(new -$$Lambda$WifiScanningServiceImpl$WifiSingleScanStateMachine$DefaultState$InbNEkwBcgp-s8u0tfPo_eYbuRI(WifiScanningServiceImpl.this.mClock.getElapsedSinceBootMillis())).toArray(-$$Lambda$WifiScanningServiceImpl$WifiSingleScanStateMachine$DefaultState$IadGqqQgFfoD3kqhYRHB92f1PGI.INSTANCE);
                return filterCachedScanResults.length == 0 ? (ScanResult[]) WifiSingleScanStateMachine.this.mCachedScanResults.toArray(new ScanResult[WifiSingleScanStateMachine.this.mCachedScanResults.size()]) : filterCachedScanResults;
            }

            static /* synthetic */ boolean lambda$filterCachedScanResultsByAge$0(long currentTimeInMillis, ScanResult scanResult) {
                return currentTimeInMillis - (scanResult.timestamp / 1000) < 180000;
            }
        }

        class DriverStartedState extends State {
            DriverStartedState() {
            }

            public void exit() {
                WifiSingleScanStateMachine.this.mCachedScanResults.clear();
                WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(2, WifiSingleScanStateMachine.this.mPendingScans.size());
                WifiSingleScanStateMachine.this.sendOpFailedToAllAndClear(WifiSingleScanStateMachine.this.mPendingScans, -1, "Scan was interrupted");
            }

            public boolean processMessage(Message msg) {
                ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                int i = msg.what;
                if (i == WifiScanningServiceImpl.CMD_DRIVER_LOADED) {
                    return true;
                }
                switch (i) {
                    case 159765:
                        WifiScanningServiceImpl.this.mWifiMetrics.incrementOneshotScanCount();
                        int handler = msg.arg2;
                        Bundle scanParams = msg.obj;
                        if (scanParams == null) {
                            if (ci != null) {
                                WifiScanningServiceImpl.this.logCallback("singleScanInvalidRequest", ci, handler, "null params");
                            }
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(WifiScanner.getScanKey(handler));
                            stringBuilder.append(" params null");
                            Log.w("WifiScanLog", stringBuilder.toString());
                            WifiScanningServiceImpl.this.replyFailed(msg, -3, "params null");
                            return true;
                        }
                        scanParams.setDefusable(true);
                        WifiScanner.ScanSettings scanSettings = (WifiScanner.ScanSettings) scanParams.getParcelable("ScanSettings");
                        WorkSource workSource = (WorkSource) scanParams.getParcelable("WorkSource");
                        if (WifiSingleScanStateMachine.this.validateScanRequest(ci, handler, scanSettings)) {
                            WifiScanningServiceImpl.this.logScanRequest("addSingleScanRequest", ci, handler, workSource, scanSettings, null);
                            WifiScanningServiceImpl.this.replySucceeded(msg);
                            if (WifiSingleScanStateMachine.this.getCurrentState() == WifiSingleScanStateMachine.this.mScanningState) {
                                if (WifiSingleScanStateMachine.this.activeScanSatisfies(scanSettings)) {
                                    WifiSingleScanStateMachine.this.mActiveScans.addRequest(ci, handler, workSource, scanSettings);
                                    WifiScanningServiceImpl.this.localLog(WifiScanner.getScanKey(handler), "22", "ScanningState add to mActiveScans");
                                } else {
                                    WifiSingleScanStateMachine.this.mPendingScans.addRequest(ci, handler, workSource, scanSettings);
                                    WifiScanningServiceImpl.this.localLog(WifiScanner.getScanKey(handler), "23", "ScanningState add to mPendingScans");
                                }
                                WifiScanningServiceImpl.this.mScannerImpl.logWifiScan("getCurrentState is ScanningState, do not start new scan");
                            } else {
                                WifiSingleScanStateMachine.this.mPendingScans.addRequest(ci, handler, workSource, scanSettings);
                                WifiScanningServiceImpl.this.localLog(WifiScanner.getScanKey(handler), "24", "add to mPendingScans, start new scan!");
                                WifiSingleScanStateMachine.this.tryToStartNewScan();
                            }
                        } else {
                            if (ci != null) {
                                WifiScanningServiceImpl.this.logCallback("singleScanInvalidRequest", ci, handler, "bad request");
                            }
                            WifiScanningServiceImpl.this.replyFailed(msg, -3, "bad request");
                            WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(3, 1);
                        }
                        return true;
                    case 159766:
                        WifiSingleScanStateMachine.this.removeSingleScanRequest(ci, msg.arg2);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class IdleState extends State {
            IdleState() {
            }

            public void enter() {
                WifiSingleScanStateMachine.this.tryToStartNewScan();
            }

            public boolean processMessage(Message msg) {
                return false;
            }
        }

        class ScanningState extends State {
            private WorkSource mScanWorkSource;

            ScanningState() {
            }

            public void enter() {
                this.mScanWorkSource = WifiSingleScanStateMachine.this.mActiveScans.createMergedWorkSource();
                try {
                    WifiScanningServiceImpl.this.mBatteryStats.noteWifiScanStartedFromSource(this.mScanWorkSource);
                } catch (RemoteException e) {
                    WifiSingleScanStateMachine.this.loge(e.toString());
                }
            }

            public void exit() {
                WifiSingleScanStateMachine.this.mActiveScanSettings = null;
                try {
                    WifiScanningServiceImpl.this.mBatteryStats.noteWifiScanStoppedFromSource(this.mScanWorkSource);
                } catch (RemoteException e) {
                    WifiSingleScanStateMachine.this.loge(e.toString());
                }
                WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(0, WifiSingleScanStateMachine.this.mActiveScans.size());
                WifiSingleScanStateMachine.this.sendOpFailedToAllAndClear(WifiSingleScanStateMachine.this.mActiveScans, -1, "Scan was interrupted");
            }

            public boolean processMessage(Message msg) {
                int i = msg.what;
                if (i != WifiScanningServiceImpl.CMD_SCAN_FAILED) {
                    switch (i) {
                        case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS /*160000*/:
                            WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(1, WifiSingleScanStateMachine.this.mActiveScans.size());
                            WifiSingleScanStateMachine.this.reportScanResults(WifiScanningServiceImpl.this.mScannerImpl.getLatestSingleScanResults());
                            WifiSingleScanStateMachine.this.mActiveScans.clear();
                            WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mIdleState);
                            return true;
                        case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS /*160001*/:
                            WifiSingleScanStateMachine.this.reportFullScanResult((ScanResult) msg.obj, msg.arg2);
                            return true;
                        default:
                            return false;
                    }
                }
                WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(0, WifiSingleScanStateMachine.this.mActiveScans.size());
                WifiSingleScanStateMachine.this.sendOpFailedToAllAndClear(WifiSingleScanStateMachine.this.mActiveScans, -1, "Scan failed");
                WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mIdleState);
                return true;
            }
        }

        WifiSingleScanStateMachine(Looper looper) {
            super("WifiSingleScanStateMachine", looper);
            setLogRecSize(128);
            setLogOnlyTransitions(false);
            addState(this.mDefaultState);
            addState(this.mDriverStartedState, this.mDefaultState);
            addState(this.mIdleState, this.mDriverStartedState);
            addState(this.mScanningState, this.mDriverStartedState);
            setInitialState(this.mDefaultState);
        }

        public void onScanStatus(int event) {
            switch (event) {
                case 0:
                case 1:
                case 2:
                    sendMessage(WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS);
                    return;
                case 3:
                    sendMessage(WifiScanningServiceImpl.CMD_SCAN_FAILED);
                    return;
                default:
                    String str = WifiScanningServiceImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown scan status event: ");
                    stringBuilder.append(event);
                    Log.e(str, stringBuilder.toString());
                    return;
            }
        }

        public void onFullScanResult(ScanResult fullScanResult, int bucketsScanned) {
            sendMessage(WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS, 0, bucketsScanned, fullScanResult);
        }

        public void onScanPaused(ScanData[] scanData) {
            Log.e(WifiScanningServiceImpl.TAG, "Got scan paused for single scan");
        }

        public void onScanRestarted() {
            Log.e(WifiScanningServiceImpl.TAG, "Got scan restarted for single scan");
        }

        boolean validateScanType(int type) {
            return type == 0 || type == 1 || type == 2;
        }

        boolean validateScanRequest(ClientInfo ci, int handler, WifiScanner.ScanSettings settings) {
            StringBuilder stringBuilder;
            String str;
            if (ci == null || settings == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(WifiScanner.getScanKey(handler));
                stringBuilder.append(" Failing single scan request ClientInfo not found ");
                stringBuilder.append(handler);
                stringBuilder.append(" ci is :");
                stringBuilder.append(ci);
                stringBuilder.append(" settings is :");
                stringBuilder.append(settings);
                Log.d("WifiScanLog", stringBuilder.toString());
                return false;
            } else if (settings.band == 0 && (settings.channels == null || settings.channels.length == 0)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(WifiScanner.getScanKey(handler));
                stringBuilder.append("Failing single scan because channel list was empty");
                Log.d("WifiScanLog", stringBuilder.toString());
                return false;
            } else if (validateScanType(settings.type)) {
                if (WifiScanningServiceImpl.this.mContext.checkPermission("android.permission.NETWORK_STACK", -1, ci.getUid()) == -1) {
                    if (!ArrayUtils.isEmpty(settings.hiddenNetworks)) {
                        str = WifiScanningServiceImpl.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failing single scan because app ");
                        stringBuilder.append(ci.getUid());
                        stringBuilder.append(" does not have permission to set hidden networks");
                        Log.e(str, stringBuilder.toString());
                        return false;
                    } else if (settings.type != 0) {
                        str = WifiScanningServiceImpl.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failing single scan because app ");
                        stringBuilder.append(ci.getUid());
                        stringBuilder.append(" does not have permission to set type");
                        Log.e(str, stringBuilder.toString());
                        return false;
                    }
                }
                return true;
            } else {
                str = WifiScanningServiceImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid scan type ");
                stringBuilder.append(settings.type);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }

        int getNativeScanType(int type) {
            switch (type) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                    return 2;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid scan type ");
                    stringBuilder.append(type);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        boolean activeScanTypeSatisfies(int requestScanType) {
            boolean z = true;
            switch (this.mActiveScanSettings.scanType) {
                case 0:
                case 1:
                    if (requestScanType == 2) {
                        z = false;
                    }
                    return z;
                case 2:
                    return true;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid scan type ");
                    stringBuilder.append(this.mActiveScanSettings.scanType);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        int mergeScanTypes(int existingScanType, int newScanType) {
            switch (existingScanType) {
                case 0:
                case 1:
                    return newScanType;
                case 2:
                    return existingScanType;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid scan type ");
                    stringBuilder.append(existingScanType);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        boolean activeScanSatisfies(WifiScanner.ScanSettings settings) {
            if (this.mActiveScanSettings == null || !activeScanTypeSatisfies(getNativeScanType(settings.type))) {
                return false;
            }
            BucketSettings activeBucket = this.mActiveScanSettings.buckets[0];
            ChannelCollection activeChannels = WifiScanningServiceImpl.this.mChannelHelper.createChannelCollection();
            activeChannels.addChannels(activeBucket);
            if (!activeChannels.containsSettings(settings)) {
                return false;
            }
            if ((settings.reportEvents & 2) != 0 && (activeBucket.report_events & 2) == 0) {
                return false;
            }
            if (!ArrayUtils.isEmpty(settings.hiddenNetworks)) {
                if (ArrayUtils.isEmpty(this.mActiveScanSettings.hiddenNetworks)) {
                    return false;
                }
                List<HiddenNetwork> activeHiddenNetworks = new ArrayList();
                for (HiddenNetwork hiddenNetwork : this.mActiveScanSettings.hiddenNetworks) {
                    activeHiddenNetworks.add(hiddenNetwork);
                }
                for (WifiScanner.ScanSettings.HiddenNetwork hiddenNetwork2 : settings.hiddenNetworks) {
                    HiddenNetwork nativeHiddenNetwork = new HiddenNetwork();
                    nativeHiddenNetwork.ssid = hiddenNetwork2.ssid;
                    if (!activeHiddenNetworks.contains(nativeHiddenNetwork)) {
                        return false;
                    }
                }
            }
            return true;
        }

        void removeSingleScanRequest(ClientInfo ci, int handler) {
            if (ci != null) {
                WifiScanningServiceImpl.this.logScanRequest("removeSingleScanRequest", ci, handler, null, null, null);
                this.mPendingScans.removeRequest(ci, handler);
                this.mActiveScans.removeRequest(ci, handler);
            }
        }

        void removeSingleScanRequests(ClientInfo ci) {
            if (ci != null) {
                WifiScanningServiceImpl.this.logScanRequest("removeSingleScanRequests", ci, -1, null, null, null);
                this.mPendingScans.removeAllForClient(ci);
                this.mActiveScans.removeAllForClient(ci);
            }
        }

        void tryToStartNewScan() {
            if (this.mPendingScans.size() != 0) {
                WifiScanningServiceImpl.this.mChannelHelper.updateChannels();
                ScanSettings settings = new ScanSettings();
                settings.num_buckets = 1;
                BucketSettings bucketSettings = new BucketSettings();
                bucketSettings.bucket = 0;
                bucketSettings.period_ms = 0;
                bucketSettings.report_events = 1;
                ChannelCollection channels = WifiScanningServiceImpl.this.mChannelHelper.createChannelCollection();
                List<HiddenNetwork> hiddenNetworkList = new ArrayList();
                StringBuilder keys = new StringBuilder();
                Iterator it = this.mPendingScans.iterator();
                while (it.hasNext()) {
                    RequestInfo<WifiScanner.ScanSettings> entry = (RequestInfo) it.next();
                    settings.scanType = mergeScanTypes(settings.scanType, getNativeScanType(((WifiScanner.ScanSettings) entry.settings).type));
                    keys.append(WifiScanner.getScanKey(entry.handlerId));
                    channels.addChannels((WifiScanner.ScanSettings) entry.settings);
                    if (((WifiScanner.ScanSettings) entry.settings).hiddenNetworks != null) {
                        for (WifiScanner.ScanSettings.HiddenNetwork hiddenNetwork : ((WifiScanner.ScanSettings) entry.settings).hiddenNetworks) {
                            HiddenNetwork hiddenNetwork2 = new HiddenNetwork();
                            hiddenNetwork2.ssid = hiddenNetwork.ssid;
                            hiddenNetworkList.add(hiddenNetwork2);
                        }
                    }
                    if ((((WifiScanner.ScanSettings) entry.settings).reportEvents & 2) != 0) {
                        bucketSettings.report_events |= 2;
                    }
                    if (((WifiScanner.ScanSettings) entry.settings).isHiddenSigleScan) {
                        settings.isHiddenSingleScan = true;
                        Log.d(WifiScanningServiceImpl.TAG, "tryToStartNewScan isHiddenSingleScan = true");
                    }
                }
                if (hiddenNetworkList.size() > 0) {
                    settings.hiddenNetworks = new HiddenNetwork[hiddenNetworkList.size()];
                    int numHiddenNetworks = 0;
                    for (HiddenNetwork hiddenNetwork3 : hiddenNetworkList) {
                        int numHiddenNetworks2 = numHiddenNetworks + 1;
                        settings.hiddenNetworks[numHiddenNetworks] = hiddenNetwork3;
                        numHiddenNetworks = numHiddenNetworks2;
                    }
                }
                channels.fillBucketSettings(bucketSettings, Values.MAX_EXPID);
                settings.buckets = new BucketSettings[]{bucketSettings};
                WifiScanningServiceImpl.this.localLog(keys.toString(), "25", "tryToStartNewScan in WifiScanningServiceImpl");
                settings.handlerId = keys.toString();
                if (WifiScanningServiceImpl.this.mScannerImpl == null || !WifiScanningServiceImpl.this.mScannerImpl.startSingleScan(settings, this)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(keys);
                    stringBuilder.append("start single scan failed ");
                    Log.w("WifiScanLog", stringBuilder.toString());
                    WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(0, this.mPendingScans.size());
                    sendOpFailedToAllAndClear(this.mPendingScans, -1, "Failed to start single scan");
                } else {
                    this.mActiveScanSettings = settings;
                    RequestList<WifiScanner.ScanSettings> tmp = this.mActiveScans;
                    this.mActiveScans = this.mPendingScans;
                    this.mPendingScans = tmp;
                    this.mPendingScans.clear();
                    transitionTo(this.mScanningState);
                }
            }
        }

        void sendOpFailedToAllAndClear(RequestList<?> clientHandlers, int reason, String description) {
            StringBuilder keys = new StringBuilder();
            Iterator it = clientHandlers.iterator();
            while (it.hasNext()) {
                RequestInfo<?> entry = (RequestInfo) it.next();
                ClientInfo clientInfo = entry.clientInfo;
                int i = entry.handlerId;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("reason=");
                stringBuilder.append(reason);
                stringBuilder.append(", ");
                stringBuilder.append(description);
                WifiScanningServiceImpl.this.logCallback("singleScanFailed", clientInfo, i, stringBuilder.toString());
                entry.reportEvent(159762, 0, new OperationResult(reason, description));
                keys.append(WifiScanner.getScanKey(entry.handlerId));
            }
            if (clientHandlers.size() > 0) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(keys);
                stringBuilder2.append("WifiSingleScanStateMachine sendOpFailedToAllAndClear scan failed des:");
                stringBuilder2.append(description);
                Log.w("WifiScanLog", stringBuilder2.toString());
            }
            clientHandlers.clear();
        }

        void reportFullScanResult(ScanResult result, int bucketsScanned) {
            Iterator it = this.mActiveScans.iterator();
            while (it.hasNext()) {
                RequestInfo<WifiScanner.ScanSettings> entry = (RequestInfo) it.next();
                if (ScanScheduleUtil.shouldReportFullScanResultForSettings(WifiScanningServiceImpl.this.mChannelHelper, result, bucketsScanned, (WifiScanner.ScanSettings) entry.settings, -1)) {
                    entry.reportEvent(159764, 0, result);
                }
            }
            it = WifiScanningServiceImpl.this.mSingleScanListeners.iterator();
            while (it.hasNext()) {
                ((RequestInfo) it.next()).reportEvent(159764, 0, result);
            }
        }

        void reportScanResults(ScanData results) {
            if (!(results == null || results.getResults() == null)) {
                if (results.getResults().length > 0) {
                    WifiScanningServiceImpl.this.mWifiMetrics.incrementNonEmptyScanResultCount();
                } else {
                    WifiScanningServiceImpl.this.mWifiMetrics.incrementEmptyScanResultCount();
                }
            }
            ScanData[] allResults = new ScanData[]{results};
            StringBuilder keys = new StringBuilder();
            Iterator it = this.mActiveScans.iterator();
            while (it.hasNext()) {
                RequestInfo<WifiScanner.ScanSettings> entry = (RequestInfo) it.next();
                ScanData[] resultsToDeliver = ScanScheduleUtil.filterResultsForSettings(WifiScanningServiceImpl.this.mChannelHelper, allResults, (WifiScanner.ScanSettings) entry.settings, -1);
                ParcelableScanData parcelableResultsToDeliver = new ParcelableScanData(resultsToDeliver);
                WifiScanningServiceImpl.this.logCallback("singleScanResults", entry.clientInfo, entry.handlerId, WifiScanningServiceImpl.describeForLog(resultsToDeliver));
                keys.append(WifiScanner.getScanKey(entry.handlerId));
                entry.reportEvent(159749, 0, parcelableResultsToDeliver);
                entry.reportEvent(159767, 0, null);
                if (!WifiScanningServiceImpl.mSendScanResultsBroadcast && shouldSendScanResultsBroadcast(entry, false)) {
                    WifiScanningServiceImpl.mSendScanResultsBroadcast = true;
                }
            }
            if (!WifiScanningServiceImpl.mSendScanResultsBroadcast) {
                it = this.mPendingScans.iterator();
                while (it.hasNext()) {
                    if (shouldSendScanResultsBroadcast((RequestInfo) it.next(), true)) {
                        WifiScanningServiceImpl.mSendScanResultsBroadcast = true;
                        break;
                    }
                }
            }
            ParcelableScanData parcelableAllResults = new ParcelableScanData(allResults);
            it = WifiScanningServiceImpl.this.mSingleScanListeners.iterator();
            while (it.hasNext()) {
                RequestInfo<Void> entry2 = (RequestInfo) it.next();
                WifiScanningServiceImpl.this.logCallback("singleScanResults", entry2.clientInfo, entry2.handlerId, WifiScanningServiceImpl.describeForLog(allResults));
                entry2.reportEvent(159774, 0, keys.toString());
                entry2.reportEvent(159749, 0, parcelableAllResults);
            }
            if (results != null) {
                this.mCachedScanResults.clear();
                this.mCachedScanResults.addAll(Arrays.asList(results.getResults()));
                WifiScanningServiceImpl.this.updateScanResultByWifiPro(this.mCachedScanResults);
            } else {
                Log.w(WifiScanningServiceImpl.TAG, "LatestSingleScanResult is null");
                Log.w(WifiScanningServiceImpl.TAG, "reportScanResults: not add scan results, not send broadcast");
            }
            WifiScanningServiceImpl.mSendScanResultsBroadcast = false;
        }

        List<ScanResult> getCachedScanResultsAsList() {
            return this.mCachedScanResults;
        }

        private boolean shouldSendScanResultsBroadcast(RequestInfo<WifiScanner.ScanSettings> requestInfo, boolean isPendingScans) {
            List<RunningTaskInfo> runningTaskInfos = ((ActivityManager) WifiScanningServiceImpl.this.mContext.getSystemService("activity")).getRunningTasks(1);
            if (!(runningTaskInfos == null || runningTaskInfos.isEmpty())) {
                ComponentName cn = ((RunningTaskInfo) runningTaskInfos.get(0)).topActivity;
                String WIFI_SETTINGS = "com.android.settings.Settings$WifiSettingsActivity";
                if (!(cn == null || cn.getClassName() == null || !cn.getClassName().startsWith("com.android.settings.Settings$WifiSettingsActivity"))) {
                    Log.d(WifiScanningServiceImpl.TAG, "shouldSendScanResultsBroadcast:true. WifiSettingsActivity");
                    return true;
                }
            }
            if (requestInfo.workSource != null) {
                for (int index = 0; index < requestInfo.workSource.size(); index++) {
                    if (1010 != requestInfo.workSource.get(index)) {
                        String str = WifiScanningServiceImpl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("shouldSendScanResultsBroadcast:true. Not only WIFI_UID scans. isPendingScans:");
                        stringBuilder.append(isPendingScans);
                        Log.d(str, stringBuilder.toString());
                        return true;
                    }
                }
            }
            if (isPendingScans || 7 != ((WifiScanner.ScanSettings) requestInfo.settings).band) {
                return false;
            }
            Log.d(WifiScanningServiceImpl.TAG, "shouldSendScanResultsBroadcast:true. Band is WIFI_BAND_BOTH_WITH_DFS");
            return true;
        }
    }

    private void localLog(String message) {
        this.mLocalLog.log(message);
    }

    private void logw(String message) {
        Log.w(TAG, message);
        this.mLocalLog.log(message);
    }

    private void loge(String message) {
        Log.e(TAG, message);
        this.mLocalLog.log(message);
    }

    public Messenger getMessenger() {
        if (this.mClientHandler != null) {
            this.mLog.trace("getMessenger() uid=%").c((long) Binder.getCallingUid()).flush();
            return new Messenger(this.mClientHandler);
        }
        loge("WifiScanningServiceImpl trying to get messenger w/o initialization");
        return null;
    }

    public Bundle getAvailableChannels(int band) {
        this.mChannelHelper.updateChannels();
        ChannelSpec[] channelSpecs = this.mChannelHelper.getAvailableScanChannels(band);
        ArrayList<Integer> list = new ArrayList(channelSpecs.length);
        for (ChannelSpec channelSpec : channelSpecs) {
            list.add(Integer.valueOf(channelSpec.frequency));
        }
        Bundle b = new Bundle();
        b.putIntegerArrayList("Channels", list);
        this.mLog.trace("getAvailableChannels uid=%").c((long) Binder.getCallingUid()).flush();
        return b;
    }

    private void enforceLocationHardwarePermission(int uid) {
        this.mContext.enforcePermission("android.permission.LOCATION_HARDWARE", -1, uid, "LocationHardware");
    }

    public WifiScanningServiceImpl(Context context, Looper looper, WifiScannerImplFactory scannerImplFactory, IBatteryStats batteryStats, WifiInjector wifiInjector) {
        this.mContext = context;
        this.mLooper = looper;
        this.mScannerImplFactory = scannerImplFactory;
        this.mBatteryStats = batteryStats;
        this.mClients = new ArrayMap();
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mWifiMetrics = wifiInjector.getWifiMetrics();
        this.mClock = wifiInjector.getClock();
        this.mLog = wifiInjector.makeLog(TAG);
        this.mFrameworkFacade = wifiInjector.getFrameworkFacade();
        this.mPreviousSchedule = null;
    }

    public void startService() {
        this.mBackgroundScanStateMachine = new WifiBackgroundScanStateMachine(this.mLooper);
        this.mSingleScanStateMachine = new WifiSingleScanStateMachine(this.mLooper);
        this.mPnoScanStateMachine = new WifiPnoScanStateMachine(this.mLooper);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra("scan_enabled", 1);
                if (state == 3) {
                    WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                    WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                    WifiScanningServiceImpl.this.mPnoScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                } else if (state == 1) {
                    WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                    WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                    WifiScanningServiceImpl.this.mPnoScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                }
            }
        }, new IntentFilter("wifi_scan_available"));
        this.mBackgroundScanStateMachine.start();
        this.mSingleScanStateMachine.start();
        this.mPnoScanStateMachine.start();
        this.mClientHandler = new ClientHandler(TAG, this.mLooper);
    }

    @VisibleForTesting
    public void setWifiHandlerLogForTest(WifiLog log) {
        this.mClientHandler.setWifiLog(log);
    }

    private WorkSource computeWorkSource(ClientInfo ci, WorkSource requestedWorkSource) {
        if (requestedWorkSource != null) {
            requestedWorkSource.clearNames();
            if (!requestedWorkSource.isEmpty()) {
                return requestedWorkSource;
            }
        }
        if (ci.getUid() > 0) {
            return new WorkSource(ci.getUid());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to compute workSource for client: ");
        stringBuilder.append(ci);
        stringBuilder.append(", requested: ");
        stringBuilder.append(requestedWorkSource);
        loge(stringBuilder.toString());
        return new WorkSource();
    }

    void replySucceeded(Message msg) {
        if (msg.replyTo != null) {
            Message reply = Message.obtain();
            reply.what = 159761;
            reply.arg2 = msg.arg2;
            if (msg.obj != null) {
                reply.obj = msg.obj;
            }
            try {
                msg.replyTo.send(reply);
                this.mLog.trace("replySucceeded recvdMessage=%").c((long) msg.what).flush();
            } catch (RemoteException e) {
            }
        }
    }

    void replyFailed(Message msg, int reason, String description) {
        if (msg.replyTo != null) {
            Message reply = Message.obtain();
            reply.what = 159762;
            reply.arg2 = msg.arg2;
            reply.obj = new OperationResult(reason, description);
            try {
                msg.replyTo.send(reply);
                this.mLog.trace("replyFailed recvdMessage=% reason=%").c((long) msg.what).c((long) reason).flush();
            } catch (RemoteException e) {
            }
        }
    }

    private static String toString(int uid, WifiScanner.ScanSettings settings) {
        StringBuilder sb = new StringBuilder();
        sb.append("ScanSettings[uid=");
        sb.append(uid);
        sb.append(", period=");
        sb.append(settings.periodInMs);
        sb.append(", report=");
        sb.append(settings.reportEvents);
        if (settings.reportEvents == 0 && settings.numBssidsPerScan > 0 && settings.maxScansToCache > 1) {
            sb.append(", batch=");
            sb.append(settings.maxScansToCache);
            sb.append(", numAP=");
            sb.append(settings.numBssidsPerScan);
        }
        sb.append(", ");
        sb.append(ChannelHelper.toString(settings));
        sb.append("]");
        return sb.toString();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Permission Denial: can't dump WifiScanner from from pid=");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", uid=");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(" without permission ");
            stringBuilder.append("android.permission.DUMP");
            pw.println(stringBuilder.toString());
            return;
        }
        pw.println("WifiScanningService - Log Begin ----");
        this.mLocalLog.dump(fd, pw, args);
        pw.println("WifiScanningService - Log End ----");
        pw.println();
        pw.println("clients:");
        for (ClientInfo client : this.mClients.values()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  ");
            stringBuilder2.append(client);
            pw.println(stringBuilder2.toString());
        }
        pw.println("listeners:");
        for (ClientInfo client2 : this.mClients.values()) {
            for (WifiScanner.ScanSettings settings : this.mBackgroundScanStateMachine.getBackgroundScanSettings(client2)) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("  ");
                stringBuilder3.append(toString(client2.mUid, settings));
                pw.println(stringBuilder3.toString());
            }
        }
        if (this.mBackgroundScheduler != null) {
            ScanSettings schedule = this.mBackgroundScheduler.getSchedule();
            if (schedule != null) {
                pw.println("schedule:");
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("  base period: ");
                stringBuilder4.append(schedule.base_period_ms);
                pw.println(stringBuilder4.toString());
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("  max ap per scan: ");
                stringBuilder4.append(schedule.max_ap_per_scan);
                pw.println(stringBuilder4.toString());
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("  batched scans: ");
                stringBuilder4.append(schedule.report_threshold_num_scans);
                pw.println(stringBuilder4.toString());
                pw.println("  buckets:");
                for (int b = 0; b < schedule.num_buckets; b++) {
                    BucketSettings bucket = schedule.buckets[b];
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("    bucket ");
                    stringBuilder5.append(bucket.bucket);
                    stringBuilder5.append(" (");
                    stringBuilder5.append(bucket.period_ms);
                    stringBuilder5.append("ms)[");
                    stringBuilder5.append(bucket.report_events);
                    stringBuilder5.append("]: ");
                    stringBuilder5.append(ChannelHelper.toString(bucket));
                    pw.println(stringBuilder5.toString());
                }
            }
        }
        if (this.mPnoScanStateMachine != null) {
            this.mPnoScanStateMachine.dump(fd, pw, args);
        }
        pw.println();
        if (this.mSingleScanStateMachine != null) {
            this.mSingleScanStateMachine.dump(fd, pw, args);
            pw.println();
            pw.println("Latest scan results:");
            ScanResultUtil.dumpScanResults(pw, this.mSingleScanStateMachine.getCachedScanResultsAsList(), this.mClock.getElapsedSinceBootMillis());
            pw.println();
        }
        if (this.mScannerImpl != null) {
            this.mScannerImpl.dump(fd, pw, args);
        }
    }

    void logScanRequest(String request, ClientInfo ci, int id, WorkSource workSource, WifiScanner.ScanSettings settings, PnoSettings pnoSettings) {
        StringBuilder sb = new StringBuilder();
        sb.append(request);
        sb.append(": ");
        sb.append(ci == null ? "ClientInfo[unknown]" : ci.toString());
        sb.append(",Id=");
        sb.append(id);
        if (workSource != null) {
            sb.append(",");
            sb.append(workSource);
        }
        if (settings != null) {
            sb.append(", ");
            describeTo(sb, settings);
        }
        if (pnoSettings != null) {
            sb.append(", ");
            describeTo(sb, pnoSettings);
        }
        localLog(sb.toString());
    }

    void logCallback(String callback, ClientInfo ci, int id, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append(callback);
        sb.append(": ");
        sb.append(ci == null ? "ClientInfo[unknown]" : ci.toString());
        sb.append(",Id=");
        sb.append(WifiScanner.getScanKey(id));
        if (extra != null) {
            sb.append(",");
            sb.append(extra);
        }
        localLog(sb.toString());
    }

    static String describeForLog(ScanData[] results) {
        StringBuilder sb = new StringBuilder();
        sb.append("results=");
        for (int i = 0; i < results.length; i++) {
            if (i > 0) {
                sb.append(NAIRealmData.NAI_REALM_STRING_SEPARATOR);
            }
            sb.append(results[i].getResults().length);
        }
        return sb.toString();
    }

    static String describeForLog(ScanResult[] results) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("results=");
        stringBuilder.append(results.length);
        return stringBuilder.toString();
    }

    static String getScanTypeString(int type) {
        switch (type) {
            case 0:
                return "LOW LATENCY";
            case 1:
                return "LOW POWER";
            case 2:
                return "HIGH ACCURACY";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid scan type ");
                stringBuilder.append(type);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    static String describeTo(StringBuilder sb, WifiScanner.ScanSettings scanSettings) {
        sb.append("ScanSettings { ");
        sb.append(" type:");
        sb.append(getScanTypeString(scanSettings.type));
        sb.append(" band:");
        sb.append(ChannelHelper.bandToString(scanSettings.band));
        sb.append(" period:");
        sb.append(scanSettings.periodInMs);
        sb.append(" reportEvents:");
        sb.append(scanSettings.reportEvents);
        sb.append(" numBssidsPerScan:");
        sb.append(scanSettings.numBssidsPerScan);
        sb.append(" maxScansToCache:");
        sb.append(scanSettings.maxScansToCache);
        sb.append(" channels:[ ");
        if (scanSettings.channels != null) {
            for (ChannelSpec channelSpec : scanSettings.channels) {
                sb.append(channelSpec.frequency);
                sb.append(" ");
            }
        }
        sb.append(" ] ");
        sb.append(" } ");
        return sb.toString();
    }

    static String describeTo(StringBuilder sb, PnoSettings pnoSettings) {
        sb.append("PnoSettings { ");
        sb.append(" min5GhzRssi:");
        sb.append(pnoSettings.min5GHzRssi);
        sb.append(" min24GhzRssi:");
        sb.append(pnoSettings.min24GHzRssi);
        sb.append(" initialScoreMax:");
        sb.append(pnoSettings.initialScoreMax);
        sb.append(" currentConnectionBonus:");
        sb.append(pnoSettings.currentConnectionBonus);
        sb.append(" sameNetworkBonus:");
        sb.append(pnoSettings.sameNetworkBonus);
        sb.append(" secureBonus:");
        sb.append(pnoSettings.secureBonus);
        sb.append(" band5GhzBonus:");
        sb.append(pnoSettings.band5GHzBonus);
        sb.append(" isConnected:");
        sb.append(pnoSettings.isConnected);
        sb.append(" networks:[ ");
        if (pnoSettings.networkList != null) {
            for (PnoSettings.PnoNetwork pnoNetwork : pnoSettings.networkList) {
                sb.append(pnoNetwork.ssid);
                sb.append(",");
            }
        }
        sb.append(" ] ");
        sb.append(" } ");
        return sb.toString();
    }

    public void updateScanResultByWifiPro(List<ScanResult> list) {
    }

    void localLog(String scanKey, String eventKey, String log) {
        localLog(scanKey, eventKey, log, null);
    }

    void localLog(String scanKey, String eventKey, String log, Object... params) {
        WifiConnectivityHelper.localLog(this.mLocalLog, scanKey, eventKey, log, params);
    }
}
