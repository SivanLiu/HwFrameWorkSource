package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.State;
import com.android.server.wifi.ClientModeManager.Listener;

class WifiController extends AbsWifiController {
    private static final int BASE = 155648;
    static final int CMD_AIRPLANE_TOGGLED = 155657;
    static final int CMD_AP_START_FAILURE = 155661;
    static final int CMD_AP_STOPPED = 155663;
    static final int CMD_DEFERRED_TOGGLE = 155659;
    static final int CMD_EMERGENCY_CALL_STATE_CHANGED = 155662;
    static final int CMD_EMERGENCY_MODE_CHANGED = 155649;
    static final int CMD_RECOVERY_DISABLE_WIFI = 155667;
    static final int CMD_RECOVERY_RESTART_WIFI = 155665;
    private static final int CMD_RECOVERY_RESTART_WIFI_CONTINUE = 155666;
    static final int CMD_SCANNING_STOPPED = 155669;
    static final int CMD_SCAN_ALWAYS_MODE_CHANGED = 155655;
    static final int CMD_SET_AP = 155658;
    static final int CMD_STA_START_FAILURE = 155664;
    static final int CMD_STA_STOPPED = 155668;
    static final int CMD_USER_PRESENT = 155660;
    static final int CMD_WIFI_TOGGLED = 155656;
    private static final boolean DBG = HWFLOW;
    private static final long DEFAULT_REENABLE_DELAY_MS = 500;
    private static final long DEFER_MARGIN_MS = 5;
    protected static final boolean HWFLOW;
    private static final String TAG = "WifiController";
    private Listener mClientModeCallback;
    private Context mContext;
    private DefaultState mDefaultState;
    private DeviceActiveState mDeviceActiveState;
    private EcmState mEcmState;
    private FrameworkFacade mFacade;
    private boolean mFirstUserSignOnSeen = false;
    private HwWifiCHRService mHwWifiCHRService;
    NetworkInfo mNetworkInfo;
    private long mReEnableDelayMillis;
    private ScanOnlyModeManager.Listener mScanOnlyModeCallback;
    private final WifiSettingsStore mSettingsStore;
    private StaDisabledState mStaDisabledState;
    private StaDisabledWithScanState mStaDisabledWithScanState;
    private StaEnabledState mStaEnabledState;
    private final WorkSource mTmpWorkSource;
    private final WifiStateMachine mWifiStateMachine;
    private final Looper mWifiStateMachineLooper;
    private final WifiStateMachinePrime mWifiStateMachinePrime;

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" enter.\n");
                wifiController.log(stringBuilder.toString());
            }
            WifiController.this.createQoEEngineService(WifiController.this.mContext, WifiController.this.mWifiStateMachine);
            WifiController.this.createWifiProStateMachine(WifiController.this.mContext, WifiController.this.mWifiStateMachine.getMessenger());
        }

        public boolean processMessage(Message msg) {
            StringBuilder stringBuilder;
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" what=");
                stringBuilder.append(msg.what);
                wifiController.logd(stringBuilder.toString());
            }
            if (WifiController.this.processDefaultState(msg)) {
                return true;
            }
            int i = msg.what;
            if (i != WifiController.CMD_EMERGENCY_MODE_CHANGED) {
                switch (i) {
                    case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED /*155655*/:
                    case WifiController.CMD_WIFI_TOGGLED /*155656*/:
                    case WifiController.CMD_AP_START_FAILURE /*155661*/:
                    case WifiController.CMD_STA_START_FAILURE /*155664*/:
                    case WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE /*155666*/:
                    case WifiController.CMD_STA_STOPPED /*155668*/:
                    case WifiController.CMD_SCANNING_STOPPED /*155669*/:
                        break;
                    case WifiController.CMD_AIRPLANE_TOGGLED /*155657*/:
                        if (!WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                            WifiController.this.log("Airplane mode disabled, determine next state");
                            if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                                if (WifiController.this.checkScanOnlyModeAvailable()) {
                                    WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                                    break;
                                }
                            }
                            WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                            break;
                        }
                        WifiController.this.log("Airplane mode toggled, shutdown all modes");
                        WifiController.this.mWifiStateMachinePrime.shutdownWifi();
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                        break;
                        break;
                    case WifiController.CMD_SET_AP /*155658*/:
                        if (!WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                            if (msg.arg1 != 1) {
                                WifiController.this.mWifiStateMachinePrime.stopSoftAPMode();
                                break;
                            }
                            Object obj = msg.obj;
                            WifiController.this.mWifiStateMachinePrime.enterSoftAPMode((SoftApModeConfiguration) msg.obj);
                            break;
                        }
                        WifiController.this.log("drop softap requests when in airplane mode");
                        break;
                    case WifiController.CMD_DEFERRED_TOGGLE /*155659*/:
                        WifiController.this.log("DEFERRED_TOGGLE ignored due to state change");
                        break;
                    case WifiController.CMD_USER_PRESENT /*155660*/:
                        WifiController.this.mFirstUserSignOnSeen = true;
                        break;
                    case WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED /*155662*/:
                        break;
                    case WifiController.CMD_AP_STOPPED /*155663*/:
                        WifiController.this.log("SoftAp mode disabled, determine next state");
                        if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                            if (WifiController.this.checkScanOnlyModeAvailable()) {
                                WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                                break;
                            }
                        }
                        WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                        break;
                        break;
                    case WifiController.CMD_RECOVERY_RESTART_WIFI /*155665*/:
                        WifiController.this.deferMessage(WifiController.this.obtainMessage(WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE));
                        WifiController.this.mWifiStateMachinePrime.shutdownWifi();
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                        break;
                    case WifiController.CMD_RECOVERY_DISABLE_WIFI /*155667*/:
                        WifiController.this.log("Recovery has been throttled, disable wifi");
                        WifiController.this.mWifiStateMachinePrime.shutdownWifi();
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("WifiController.handleMessage ");
                        stringBuilder.append(msg.what);
                        throw new RuntimeException(stringBuilder.toString());
                }
            }
            boolean configWiFiDisableInECBM = WifiController.this.mFacade.getConfigWiFiDisableInECBM(WifiController.this.mContext);
            WifiController wifiController2 = WifiController.this;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("WifiController msg ");
            stringBuilder2.append(msg);
            stringBuilder2.append(" getConfigWiFiDisableInECBM ");
            stringBuilder2.append(configWiFiDisableInECBM);
            wifiController2.log(stringBuilder2.toString());
            if (msg.arg1 == 1 && configWiFiDisableInECBM) {
                WifiController.this.transitionTo(WifiController.this.mEcmState);
            }
            return true;
        }
    }

    class DeviceActiveState extends State {
        DeviceActiveState() {
        }

        public void enter() {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" enter.\n");
                wifiController.log(stringBuilder.toString());
            }
            WifiController.this.mWifiStateMachinePrime.enterClientMode();
            WifiController.this.mWifiStateMachine.setHighPerfModeEnabled(false);
        }

        public boolean processMessage(Message msg) {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(msg.toString());
                stringBuilder.append("\n");
                wifiController.log(stringBuilder.toString());
            }
            if (msg.what == WifiController.CMD_USER_PRESENT) {
                if (!WifiController.this.mFirstUserSignOnSeen) {
                    WifiController.this.mWifiStateMachine.reloadTlsNetworksAndReconnect();
                }
                WifiController.this.mFirstUserSignOnSeen = true;
                return true;
            } else if (msg.what != WifiController.CMD_RECOVERY_RESTART_WIFI) {
                return false;
            } else {
                String bugDetail;
                String bugTitle;
                if (msg.arg1 >= SelfRecovery.REASON_STRINGS.length || msg.arg1 < 0) {
                    bugDetail = "";
                    bugTitle = "Wi-Fi BugReport";
                } else {
                    bugDetail = SelfRecovery.REASON_STRINGS[msg.arg1];
                    bugTitle = new StringBuilder();
                    bugTitle.append("Wi-Fi BugReport: ");
                    bugTitle.append(bugDetail);
                    bugTitle = bugTitle.toString();
                }
                if (msg.arg1 != 0) {
                    new Handler(WifiController.this.mWifiStateMachineLooper).post(new -$$Lambda$WifiController$DeviceActiveState$j60bKc3b7Z47vZXsDHcRTlqzedE(this, bugTitle, bugDetail));
                }
                return false;
            }
        }
    }

    class EcmState extends State {
        private int mEcmEntryCount;

        EcmState() {
        }

        public void enter() {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append("\n");
                wifiController.log(stringBuilder.toString());
            }
            WifiController.this.mWifiStateMachinePrime.shutdownWifi();
            WifiController.this.mWifiStateMachine.clearANQPCache();
            this.mEcmEntryCount = 1;
        }

        /* JADX WARNING: Missing block: B:36:0x0089, code skipped:
            return true;
     */
        /* JADX WARNING: Missing block: B:37:0x008a, code skipped:
            return true;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(Message msg) {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(msg.toString());
                stringBuilder.append("\n");
                wifiController.log(stringBuilder.toString());
            }
            if (msg.what == WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED) {
                if (msg.arg1 == 1) {
                    this.mEcmEntryCount++;
                } else if (msg.arg1 == 0) {
                    decrementCountAndReturnToAppropriateState();
                }
                return true;
            } else if (msg.what == WifiController.CMD_EMERGENCY_MODE_CHANGED) {
                if (msg.arg1 == 1) {
                    this.mEcmEntryCount++;
                } else if (msg.arg1 == 0) {
                    decrementCountAndReturnToAppropriateState();
                }
                return true;
            } else if (msg.what == WifiController.CMD_RECOVERY_RESTART_WIFI || msg.what == WifiController.CMD_RECOVERY_DISABLE_WIFI || msg.what == WifiController.CMD_AP_STOPPED || msg.what == WifiController.CMD_SCANNING_STOPPED || msg.what == WifiController.CMD_STA_STOPPED || msg.what == WifiController.CMD_SET_AP) {
                return true;
            } else {
                return false;
            }
        }

        private void decrementCountAndReturnToAppropriateState() {
            boolean exitEcm = false;
            if (this.mEcmEntryCount == 0) {
                WifiController.this.loge("mEcmEntryCount is 0; exiting Ecm");
                exitEcm = true;
            } else {
                int i = this.mEcmEntryCount - 1;
                this.mEcmEntryCount = i;
                if (i == 0) {
                    exitEcm = true;
                }
            }
            if (!exitEcm) {
                return;
            }
            if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
            } else if (WifiController.this.checkScanOnlyModeAvailable()) {
                WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
            } else {
                WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
            }
        }
    }

    class StaDisabledState extends State {
        private int mDeferredEnableSerialNumber = 0;
        private long mDisabledTimestamp;
        private boolean mHaveDeferredEnable = false;

        StaDisabledState() {
        }

        public void enter() {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" enter.\n");
                wifiController.log(stringBuilder.toString());
            }
            WifiController.this.mWifiStateMachinePrime.disableWifi();
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = false;
            WifiController.this.mWifiStateMachine.clearANQPCache();
        }

        public boolean processMessage(Message msg) {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" what=");
                stringBuilder.append(msg.what);
                wifiController.logd(stringBuilder.toString());
            }
            switch (msg.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED /*155655*/:
                    if (WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                        break;
                    }
                    break;
                case WifiController.CMD_WIFI_TOGGLED /*155656*/:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (WifiController.this.checkScanOnlyModeAvailable() && WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                            break;
                        }
                    } else if (!doDeferEnable(msg)) {
                        WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                        break;
                    } else {
                        if (this.mHaveDeferredEnable) {
                            this.mDeferredEnableSerialNumber++;
                        }
                        this.mHaveDeferredEnable ^= 1;
                        break;
                    }
                case WifiController.CMD_SET_AP /*155658*/:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        WifiController.this.log("drop softap requests when in airplane mode");
                        break;
                    }
                    if (msg.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(0);
                    }
                    return false;
                case WifiController.CMD_DEFERRED_TOGGLE /*155659*/:
                    if (msg.arg1 == this.mDeferredEnableSerialNumber) {
                        WifiController.this.log("DEFERRED_TOGGLE handled");
                        WifiController.this.sendMessage((Message) msg.obj);
                        break;
                    }
                    WifiController.this.log("DEFERRED_TOGGLE ignored due to serial mismatch");
                    break;
                case WifiController.CMD_RECOVERY_RESTART_WIFI_CONTINUE /*155666*/:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (WifiController.this.checkScanOnlyModeAvailable()) {
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                            break;
                        }
                    }
                    WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                    break;
                    break;
                default:
                    return false;
            }
            return true;
        }

        private boolean doDeferEnable(Message msg) {
            long delaySoFar = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (delaySoFar >= WifiController.this.mReEnableDelayMillis) {
                return false;
            }
            WifiController wifiController = WifiController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WifiController msg ");
            stringBuilder.append(msg);
            stringBuilder.append(" deferred for ");
            stringBuilder.append(WifiController.this.mReEnableDelayMillis - delaySoFar);
            stringBuilder.append("ms");
            wifiController.log(stringBuilder.toString());
            Message deferredMsg = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
            deferredMsg.obj = Message.obtain(msg);
            int i = this.mDeferredEnableSerialNumber + 1;
            this.mDeferredEnableSerialNumber = i;
            deferredMsg.arg1 = i;
            WifiController.this.sendMessageDelayed(deferredMsg, (WifiController.this.mReEnableDelayMillis - delaySoFar) + WifiController.DEFER_MARGIN_MS);
            return true;
        }
    }

    class StaDisabledWithScanState extends State {
        private int mDeferredEnableSerialNumber = 0;
        private long mDisabledTimestamp;
        private boolean mHaveDeferredEnable = false;

        StaDisabledWithScanState() {
        }

        public void enter() {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" enter.\n");
                wifiController.log(stringBuilder.toString());
            }
            WifiController.this.mWifiStateMachinePrime.enterScanOnlyMode();
            this.mDisabledTimestamp = SystemClock.elapsedRealtime();
            this.mDeferredEnableSerialNumber++;
            this.mHaveDeferredEnable = false;
        }

        public boolean processMessage(Message msg) {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" what=");
                stringBuilder.append(msg.what);
                wifiController.logd(stringBuilder.toString());
            }
            switch (msg.what) {
                case WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED /*155655*/:
                    if (!WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController.this.log("StaDisabledWithScanState: scan no longer available");
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                        break;
                    }
                    break;
                case WifiController.CMD_WIFI_TOGGLED /*155656*/:
                    if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (!doDeferEnable(msg)) {
                            WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                            break;
                        }
                        if (this.mHaveDeferredEnable) {
                            this.mDeferredEnableSerialNumber++;
                        }
                        this.mHaveDeferredEnable ^= 1;
                        break;
                    }
                    break;
                case WifiController.CMD_SET_AP /*155658*/:
                    if (msg.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(0);
                    }
                    return false;
                case WifiController.CMD_DEFERRED_TOGGLE /*155659*/:
                    if (msg.arg1 == this.mDeferredEnableSerialNumber) {
                        WifiController.this.logd("DEFERRED_TOGGLE handled");
                        WifiController.this.sendMessage((Message) msg.obj);
                        break;
                    }
                    WifiController.this.log("DEFERRED_TOGGLE ignored due to serial mismatch");
                    break;
                case WifiController.CMD_AP_START_FAILURE /*155661*/:
                    break;
                case WifiController.CMD_AP_STOPPED /*155663*/:
                    if (WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        WifiController.this.transitionTo(WifiController.this.mDeviceActiveState);
                        break;
                    }
                    break;
                case WifiController.CMD_STA_START_FAILURE /*155664*/:
                    WifiController.this.logd("CMD_STA_START_FAILURE  transition to mStaDisabledState.");
                    WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                    break;
                case WifiController.CMD_SCANNING_STOPPED /*155669*/:
                    WifiController.this.log("WifiController: SCANNING_STOPPED when in scan mode -> StaDisabled");
                    WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                    break;
                default:
                    return false;
            }
            return true;
        }

        private boolean doDeferEnable(Message msg) {
            long delaySoFar = SystemClock.elapsedRealtime() - this.mDisabledTimestamp;
            if (delaySoFar >= WifiController.this.mReEnableDelayMillis) {
                return false;
            }
            WifiController wifiController = WifiController.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WifiController msg ");
            stringBuilder.append(msg);
            stringBuilder.append(" deferred for ");
            stringBuilder.append(WifiController.this.mReEnableDelayMillis - delaySoFar);
            stringBuilder.append("ms");
            wifiController.log(stringBuilder.toString());
            Message deferredMsg = WifiController.this.obtainMessage(WifiController.CMD_DEFERRED_TOGGLE);
            deferredMsg.obj = Message.obtain(msg);
            int i = this.mDeferredEnableSerialNumber + 1;
            this.mDeferredEnableSerialNumber = i;
            deferredMsg.arg1 = i;
            WifiController.this.sendMessageDelayed(deferredMsg, (WifiController.this.mReEnableDelayMillis - delaySoFar) + WifiController.DEFER_MARGIN_MS);
            return true;
        }
    }

    class StaEnabledState extends State {
        StaEnabledState() {
        }

        public void enter() {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" enter.\n");
                wifiController.log(stringBuilder.toString());
            }
            WifiController.this.log("StaEnabledState.enter()");
        }

        public boolean processMessage(Message msg) {
            if (WifiController.DBG) {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getName());
                stringBuilder.append(" what=");
                stringBuilder.append(msg.what);
                wifiController.logd(stringBuilder.toString());
            }
            if (WifiController.this.processStaEnabled(msg)) {
                return true;
            }
            switch (msg.what) {
                case WifiController.CMD_WIFI_TOGGLED /*155656*/:
                    if (!WifiController.this.mSettingsStore.isWifiToggleEnabled()) {
                        if (!WifiController.this.checkScanOnlyModeAvailable()) {
                            WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                            break;
                        }
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                        break;
                    }
                    break;
                case WifiController.CMD_AIRPLANE_TOGGLED /*155657*/:
                    if (WifiController.this.mSettingsStore.isAirplaneModeOn()) {
                        return false;
                    }
                    WifiController.this.log("airplane mode toggled - and airplane mode is off.  return handled");
                    return true;
                case WifiController.CMD_SET_AP /*155658*/:
                    if (msg.arg1 == 1) {
                        WifiController.this.mSettingsStore.setWifiSavedState(1);
                        if (WifiController.this.mWifiStateMachine != null) {
                            WifiController.this.mWifiStateMachine.saveLastNetIdForAp();
                        }
                    }
                    return false;
                case WifiController.CMD_AP_START_FAILURE /*155661*/:
                case WifiController.CMD_AP_STOPPED /*155663*/:
                    break;
                case WifiController.CMD_STA_START_FAILURE /*155664*/:
                    if (!WifiController.this.checkScanOnlyModeAvailable()) {
                        WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                        break;
                    }
                    WifiController.this.transitionTo(WifiController.this.mStaDisabledWithScanState);
                    break;
                case WifiController.CMD_STA_STOPPED /*155668*/:
                    WifiController.this.transitionTo(WifiController.this.mStaDisabledState);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    private class ClientModeCallback implements Listener {
        private ClientModeCallback() {
        }

        /* synthetic */ ClientModeCallback(WifiController x0, AnonymousClass1 x1) {
            this();
        }

        public void onStateChanged(int state) {
            if (state == 4) {
                WifiController.this.logd("ClientMode unexpected failure: state unknown");
                WifiController.this.sendMessage(WifiController.CMD_STA_START_FAILURE);
            } else if (state == 1) {
                WifiController.this.logd("ClientMode stopped");
                WifiController.this.sendMessage(WifiController.CMD_STA_STOPPED);
            } else if (state == 3) {
                WifiController.this.logd("client mode active");
            } else {
                WifiController wifiController = WifiController.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected state update: ");
                stringBuilder.append(state);
                wifiController.logd(stringBuilder.toString());
            }
        }
    }

    private class ScanOnlyCallback implements ScanOnlyModeManager.Listener {
        private ScanOnlyCallback() {
        }

        /* synthetic */ ScanOnlyCallback(WifiController x0, AnonymousClass1 x1) {
            this();
        }

        public void onStateChanged(int state) {
            if (state == 4) {
                Log.d(WifiController.TAG, "ScanOnlyMode unexpected failure: state unknown");
            } else if (state == 1) {
                Log.d(WifiController.TAG, "ScanOnlyMode stopped");
                WifiController.this.sendMessage(WifiController.CMD_SCANNING_STOPPED);
            } else if (state == 3) {
                Log.d(WifiController.TAG, "scan mode active");
            } else {
                String str = WifiController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected state update: ");
                stringBuilder.append(state);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    WifiController(Context context, WifiStateMachine wsm, Looper wifiStateMachineLooper, WifiSettingsStore wss, Looper wifiServiceLooper, FrameworkFacade f, WifiStateMachinePrime wsmp) {
        super(TAG, wifiServiceLooper);
        boolean isLocationModeActive = true;
        this.mNetworkInfo = new NetworkInfo(1, 0, "WIFI", "");
        this.mTmpWorkSource = new WorkSource();
        this.mDefaultState = new DefaultState();
        this.mStaEnabledState = new StaEnabledState();
        this.mStaDisabledState = new StaDisabledState();
        this.mStaDisabledWithScanState = new StaDisabledWithScanState();
        this.mDeviceActiveState = new DeviceActiveState();
        this.mEcmState = new EcmState();
        this.mScanOnlyModeCallback = new ScanOnlyCallback(this, null);
        this.mClientModeCallback = new ClientModeCallback(this, null);
        this.mFacade = f;
        this.mContext = context;
        this.mWifiStateMachine = wsm;
        this.mWifiStateMachineLooper = wifiStateMachineLooper;
        this.mWifiStateMachinePrime = wsmp;
        this.mSettingsStore = wss;
        this.mHwWifiCHRService = HwWifiServiceFactory.getHwWifiCHRService();
        addState(this.mDefaultState);
        addState(this.mStaDisabledState, this.mDefaultState);
        addState(this.mStaEnabledState, this.mDefaultState);
        addState(this.mDeviceActiveState, this.mStaEnabledState);
        addState(this.mStaDisabledWithScanState, this.mDefaultState);
        addState(this.mEcmState, this.mDefaultState);
        boolean isAirplaneModeOn = this.mSettingsStore.isAirplaneModeOn();
        boolean isWifiEnabled = this.mSettingsStore.isWifiToggleEnabled();
        boolean isScanningAlwaysAvailable = this.mSettingsStore.isScanAlwaysAvailable();
        if (this.mSettingsStore.getLocationModeSetting(this.mContext) != 0) {
            isLocationModeActive = false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isAirplaneModeOn = ");
        stringBuilder.append(isAirplaneModeOn);
        stringBuilder.append(", isWifiEnabled = ");
        stringBuilder.append(isWifiEnabled);
        stringBuilder.append(", isScanningAvailable = ");
        stringBuilder.append(isScanningAlwaysAvailable);
        stringBuilder.append(", isLocationModeActive = ");
        stringBuilder.append(isLocationModeActive);
        log(stringBuilder.toString());
        if (checkScanOnlyModeAvailable()) {
            setInitialState(this.mStaDisabledWithScanState);
        } else {
            setInitialState(this.mStaDisabledState);
        }
        setLogRecSize(100);
        setLogOnlyTransitions(false);
        this.mWifiStateMachinePrime.registerScanOnlyCallback(this.mScanOnlyModeCallback);
        this.mWifiStateMachinePrime.registerClientModeCallback(this.mClientModeCallback);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.location.MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiController.DBG) {
                    String str = WifiController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Received action: ");
                    stringBuilder.append(action);
                    Slog.d(str, stringBuilder.toString());
                }
                if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (networkInfo != null) {
                        WifiController.this.mNetworkInfo = networkInfo;
                    }
                } else if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                    int state = intent.getIntExtra("wifi_state", 14);
                    if (state == 14) {
                        Log.e(WifiController.TAG, "SoftAP start failed");
                        WifiController.this.sendMessage(WifiController.CMD_AP_START_FAILURE);
                    } else if (state == 11) {
                        WifiController.this.sendMessage(WifiController.CMD_AP_STOPPED);
                    }
                } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    if (intent.getIntExtra("wifi_state", 4) == 4) {
                        int realState = WifiController.this.mWifiStateMachine.syncGetWifiState();
                        WifiController wifiController = WifiController.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("WifiControllerWifi turn on failed, realState =");
                        stringBuilder2.append(realState);
                        wifiController.loge(stringBuilder2.toString());
                        if (realState == 4) {
                            WifiController.this.sendMessage(WifiController.CMD_STA_START_FAILURE);
                        }
                    }
                } else if (action.equals("android.location.MODE_CHANGED")) {
                    WifiController.this.sendMessage(WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED);
                }
            }
        }, new IntentFilter(filter));
        initializeAndRegisterForSettingsChange(wifiServiceLooper);
    }

    private void initializeAndRegisterForSettingsChange(Looper looper) {
        Handler handler = new Handler(looper);
        readWifiReEnableDelay();
    }

    private boolean checkScanOnlyModeAvailable() {
        if (this.mSettingsStore.getLocationModeSetting(this.mContext) == 0) {
            return false;
        }
        return this.mSettingsStore.isScanAlwaysAvailable();
    }

    private void readWifiReEnableDelay() {
        this.mReEnableDelayMillis = this.mFacade.getLongSetting(this.mContext, "wifi_reenable_delay", DEFAULT_REENABLE_DELAY_MS);
    }

    private void updateBatteryWorkSource() {
        this.mTmpWorkSource.clear();
        this.mWifiStateMachine.updateBatteryWorkSource(this.mTmpWorkSource);
    }

    private State getNextWifiState() {
        if (this.mSettingsStore.getWifiSavedState() == 1) {
            return this.mDeviceActiveState;
        }
        if (Global.getInt(this.mContext.getContentResolver(), "wifi_on", 0) == 1) {
            Log.e(TAG, "getWifiSavedState and Settings.Global.WIFI_ON are different!");
            this.mSettingsStore.setWifiSavedState(1);
            return this.mDeviceActiveState;
        } else if (checkScanOnlyModeAvailable()) {
            return this.mStaDisabledWithScanState;
        } else {
            return this.mStaDisabledState;
        }
    }
}
