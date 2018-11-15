package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager.SoftApCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.WifiNative.InterfaceCallback;
import com.android.server.wifi.WifiNative.SoftApListener;
import huawei.android.app.admin.HwDevicePolicyManagerEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public class SoftApManager extends AbsSoftApManager implements ActiveModeManager {
    public static final String AP_LINKED_EVENT_KEY = "event_key";
    public static final String AP_LINKED_MAC_KEY = "mac_key";
    private static final int MIN_SOFT_AP_TIMEOUT_DELAY_MS = 600000;
    @VisibleForTesting
    public static final String SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG = "SoftApManager Soft AP Send Message Timeout";
    public static final String STA_JOIN_EVENT = "STA_JOIN";
    public static final String STA_LEAVE_EVENT = "STA_LEAVE";
    private static final String TAG = "SoftApManager";
    protected WifiConfiguration mApConfig;
    private String mApInterfaceName;
    private final SoftApCallback mCallback;
    private final Context mContext;
    private String mCountryCode;
    private final FrameworkFacade mFrameworkFacade;
    private boolean mIfaceIsUp;
    private final int mMode;
    private int mNumAssociatedStations = 0;
    private int mReportedBandwidth = -1;
    private int mReportedFrequency = -1;
    private final SoftApListener mSoftApListener = new SoftApListener() {
        public void onNumAssociatedStationsChanged(int numStations) {
            SoftApManager.this.mStateMachine.sendMessage(4, numStations);
        }

        public void onSoftApChannelSwitched(int frequency, int bandwidth) {
            SoftApManager.this.mStateMachine.sendMessage(9, frequency, bandwidth);
        }

        public void OnApLinkedStaJoin(String macAddress) {
            Bundle bundle = new Bundle();
            bundle.putString(SoftApManager.AP_LINKED_EVENT_KEY, SoftApManager.STA_JOIN_EVENT);
            bundle.putString(SoftApManager.AP_LINKED_MAC_KEY, macAddress);
            SoftApManager.this.notifyApLinkedStaListChange(bundle);
        }

        public void OnApLinkedStaLeave(String macAddress) {
            Bundle bundle = new Bundle();
            bundle.putString(SoftApManager.AP_LINKED_EVENT_KEY, SoftApManager.STA_LEAVE_EVENT);
            bundle.putString(SoftApManager.AP_LINKED_MAC_KEY, macAddress);
            SoftApManager.this.notifyApLinkedStaListChange(bundle);
        }
    };
    private final SoftApStateMachine mStateMachine;
    private boolean mTimeoutEnabled = false;
    private final WifiApConfigStore mWifiApConfigStore;
    private final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;

    private class SoftApStateMachine extends StateMachine {
        public static final int CMD_INTERFACE_DESTROYED = 7;
        public static final int CMD_INTERFACE_DOWN = 8;
        public static final int CMD_INTERFACE_STATUS_CHANGED = 3;
        public static final int CMD_NO_ASSOCIATED_STATIONS_TIMEOUT = 5;
        public static final int CMD_NUM_ASSOCIATED_STATIONS_CHANGED = 4;
        public static final int CMD_SOFT_AP_CHANNEL_SWITCHED = 9;
        public static final int CMD_START = 0;
        public static final int CMD_TIMEOUT_TOGGLE_CHANGED = 6;
        private final State mIdleState = new IdleState(this, null);
        private final State mStartedState = new StartedState(this, null);
        private final InterfaceCallback mWifiNativeInterfaceCallback = new InterfaceCallback() {
            public void onDestroyed(String ifaceName) {
                if (SoftApManager.this.mApInterfaceName != null && SoftApManager.this.mApInterfaceName.equals(ifaceName)) {
                    SoftApStateMachine.this.sendMessage(7);
                }
            }

            public void onUp(String ifaceName) {
                if (SoftApManager.this.mApInterfaceName != null && SoftApManager.this.mApInterfaceName.equals(ifaceName)) {
                    SoftApStateMachine.this.sendMessage(3, 1);
                }
            }

            public void onDown(String ifaceName) {
                if (SoftApManager.this.mApInterfaceName != null && SoftApManager.this.mApInterfaceName.equals(ifaceName)) {
                    SoftApStateMachine.this.sendMessage(3, 0);
                }
            }
        };

        private class IdleState extends State {
            private static final String POLICY_OPEN_HOTSPOT = "policy-open-hotspot";
            private static final String VALUE_DISABLE = "value_disable";

            private IdleState() {
            }

            /* synthetic */ IdleState(SoftApStateMachine x0, AnonymousClass1 x1) {
                this();
            }

            public void enter() {
                SoftApManager.this.mApInterfaceName = null;
                SoftApManager.this.mIfaceIsUp = false;
            }

            public boolean processMessage(Message message) {
                SoftApManager.this.logStateAndMessage(this, message);
                if (message.what == 0) {
                    if (checkOpenHotsoptPolicy((WifiConfiguration) message.obj)) {
                        SoftApManager.this.mApInterfaceName = SoftApManager.this.mWifiNative.setupInterfaceForSoftApMode(SoftApStateMachine.this.mWifiNativeInterfaceCallback);
                        if (TextUtils.isEmpty(SoftApManager.this.mApInterfaceName)) {
                            Log.e(SoftApManager.TAG, "setup failure when creating ap interface.");
                            SoftApManager.this.updateApState(14, 11, 0);
                            SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, 0);
                        } else {
                            SoftApManager.this.updateApState(12, 11, 0);
                            int result = SoftApManager.this.startSoftAp((WifiConfiguration) message.obj);
                            if (result != 0) {
                                int failureReason = 0;
                                if (result == 1) {
                                    failureReason = 1;
                                }
                                SoftApManager.this.updateApState(14, 12, failureReason);
                                SoftApManager.this.stopSoftAp();
                                String str = SoftApManager.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("SoftApStateMachine: IdleState: startSoftAp() returns FALSE! update ap state and reason= ");
                                stringBuilder.append(failureReason);
                                Log.w(str, stringBuilder.toString());
                                SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, failureReason);
                            } else {
                                Log.d(SoftApManager.TAG, "SoftApStateMachine: IdleState: startSoftAp() returns TRUE. update ap state and transition to StartedState");
                                SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mStartedState);
                            }
                        }
                    } else {
                        SoftApManager.this.updateApState(14, 12, 0);
                    }
                }
                return true;
            }

            private boolean checkOpenHotsoptPolicy(WifiConfiguration apConfig) {
                Bundle bundle = new HwDevicePolicyManagerEx().getPolicy(null, POLICY_OPEN_HOTSPOT);
                if (bundle == null || ((apConfig != null && apConfig.preSharedKey != null) || !bundle.getBoolean(VALUE_DISABLE))) {
                    return true;
                }
                Log.w(SoftApManager.TAG, "SoftApState: MDM deny start unsecure soft ap!");
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        Toast.makeText(SoftApManager.this.mContext, SoftApManager.this.mContext.getString(33685942), 0).show();
                    }
                });
                return false;
            }
        }

        private class StartedState extends State {
            private SoftApTimeoutEnabledSettingObserver mSettingObserver;
            private WakeupMessage mSoftApTimeoutMessage;
            private int mTimeoutDelay;

            private class SoftApTimeoutEnabledSettingObserver extends ContentObserver {
                SoftApTimeoutEnabledSettingObserver(Handler handler) {
                    super(handler);
                }

                public void register() {
                    SoftApManager.this.mFrameworkFacade.registerContentObserver(SoftApManager.this.mContext, Global.getUriFor("soft_ap_timeout_enabled"), true, this);
                    SoftApManager.this.mTimeoutEnabled = getValue();
                }

                public void unregister() {
                    SoftApManager.this.mFrameworkFacade.unregisterContentObserver(SoftApManager.this.mContext, this);
                }

                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    SoftApManager.this.mStateMachine.sendMessage(6, getValue());
                }

                private boolean getValue() {
                    boolean z = true;
                    if (SoftApManager.this.mFrameworkFacade.getIntegerSetting(SoftApManager.this.mContext, "soft_ap_timeout_enabled", 1) != 1) {
                        z = false;
                    }
                    return z;
                }
            }

            private StartedState() {
            }

            /* synthetic */ StartedState(SoftApStateMachine x0, AnonymousClass1 x1) {
                this();
            }

            private int getConfigSoftApTimeoutDelay() {
                int delay = SoftApManager.this.mContext.getResources().getInteger(17694911);
                if (delay < SoftApManager.MIN_SOFT_AP_TIMEOUT_DELAY_MS) {
                    delay = SoftApManager.MIN_SOFT_AP_TIMEOUT_DELAY_MS;
                    Log.w(SoftApManager.TAG, "Overriding timeout delay with minimum limit value");
                }
                String str = SoftApManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Timeout delay: ");
                stringBuilder.append(delay);
                Log.d(str, stringBuilder.toString());
                return delay;
            }

            private void scheduleTimeoutMessage() {
                if (SoftApManager.this.mTimeoutEnabled) {
                    this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + ((long) this.mTimeoutDelay));
                    Log.d(SoftApManager.TAG, "Timeout message scheduled");
                }
            }

            private void cancelTimeoutMessage() {
                this.mSoftApTimeoutMessage.cancel();
                Log.d(SoftApManager.TAG, "Timeout message canceled");
            }

            private void setNumAssociatedStations(int numStations) {
                if (SoftApManager.this.mNumAssociatedStations != numStations) {
                    SoftApManager.this.mNumAssociatedStations = numStations;
                    String str = SoftApManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Number of associated stations changed: ");
                    stringBuilder.append(SoftApManager.this.mNumAssociatedStations);
                    Log.d(str, stringBuilder.toString());
                    if (SoftApManager.this.mCallback != null) {
                        SoftApManager.this.mCallback.onNumClientsChanged(SoftApManager.this.mNumAssociatedStations);
                    } else {
                        Log.e(SoftApManager.TAG, "SoftApCallback is null. Dropping NumClientsChanged event.");
                    }
                    SoftApManager.this.mWifiMetrics.addSoftApNumAssociatedStationsChangedEvent(SoftApManager.this.mNumAssociatedStations, SoftApManager.this.mMode);
                    if (SoftApManager.this.mNumAssociatedStations == 0) {
                        scheduleTimeoutMessage();
                    } else {
                        cancelTimeoutMessage();
                    }
                }
            }

            private void onUpChanged(boolean isUp) {
                if (isUp != SoftApManager.this.mIfaceIsUp) {
                    SoftApManager.this.mIfaceIsUp = isUp;
                    if (isUp) {
                        Log.d(SoftApManager.TAG, "SoftAp is ready for use");
                        SoftApManager.this.updateApState(13, 12, 0);
                        SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(true, 0);
                        if (SoftApManager.this.mCallback != null) {
                            SoftApManager.this.mCallback.onNumClientsChanged(SoftApManager.this.mNumAssociatedStations);
                        }
                    } else {
                        SoftApStateMachine.this.sendMessage(8);
                    }
                    SoftApManager.this.mWifiMetrics.addSoftApUpChangedEvent(isUp, SoftApManager.this.mMode);
                }
            }

            public void enter() {
                SoftApManager.this.mIfaceIsUp = false;
                onUpChanged(SoftApManager.this.mWifiNative.isInterfaceUp(SoftApManager.this.mApInterfaceName));
                this.mTimeoutDelay = getConfigSoftApTimeoutDelay();
                Handler handler = SoftApManager.this.mStateMachine.getHandler();
                this.mSoftApTimeoutMessage = new WakeupMessage(SoftApManager.this.mContext, handler, SoftApManager.SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG, 5);
                this.mSettingObserver = new SoftApTimeoutEnabledSettingObserver(handler);
                SoftApTimeoutEnabledSettingObserver softApTimeoutEnabledSettingObserver = this.mSettingObserver;
                Log.d(SoftApManager.TAG, "Resetting num stations on start");
                SoftApManager.this.mNumAssociatedStations = 0;
                scheduleTimeoutMessage();
            }

            public void exit() {
                if (SoftApManager.this.mApInterfaceName != null) {
                    SoftApManager.this.stopSoftAp();
                }
                SoftApTimeoutEnabledSettingObserver softApTimeoutEnabledSettingObserver = this.mSettingObserver;
                Log.d(SoftApManager.TAG, "Resetting num stations on stop");
                SoftApManager.this.mNumAssociatedStations = 0;
                cancelTimeoutMessage();
                SoftApManager.this.mWifiMetrics.addSoftApUpChangedEvent(false, SoftApManager.this.mMode);
                SoftApManager.this.updateApState(11, 10, 0);
                SoftApManager.this.mApInterfaceName = null;
                SoftApManager.this.mIfaceIsUp = false;
                SoftApManager.this.mStateMachine.quitNow();
            }

            public boolean processMessage(Message message) {
                SoftApManager.this.logStateAndMessage(this, message);
                int i = message.what;
                if (i != 0) {
                    boolean z = false;
                    String str;
                    StringBuilder stringBuilder;
                    switch (i) {
                        case 3:
                            if (message.arg1 == 1) {
                                z = true;
                            }
                            onUpChanged(z);
                            break;
                        case 4:
                            if (message.arg1 >= 0) {
                                Log.d(SoftApManager.TAG, "Setting num stations on CMD_NUM_ASSOCIATED_STATIONS_CHANGED");
                                setNumAssociatedStations(message.arg1);
                                break;
                            }
                            str = SoftApManager.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid number of associated stations: ");
                            stringBuilder.append(message.arg1);
                            Log.e(str, stringBuilder.toString());
                            break;
                        case 5:
                            if (SoftApManager.this.mTimeoutEnabled) {
                                if (SoftApManager.this.mNumAssociatedStations == 0) {
                                    Log.i(SoftApManager.TAG, "Timeout message received. Stopping soft AP.");
                                    SoftApManager.this.updateApState(10, 13, 0);
                                    SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                                    break;
                                }
                                Log.wtf(SoftApManager.TAG, "Timeout message received but has clients. Dropping.");
                                break;
                            }
                            Log.wtf(SoftApManager.TAG, "Timeout message received while timeout is disabled. Dropping.");
                            break;
                        case 6:
                            if (message.arg1 == 1) {
                                z = true;
                            }
                            boolean isEnabled = z;
                            if (SoftApManager.this.mTimeoutEnabled != isEnabled) {
                                SoftApManager.this.mTimeoutEnabled = isEnabled;
                                if (!SoftApManager.this.mTimeoutEnabled) {
                                    cancelTimeoutMessage();
                                }
                                if (SoftApManager.this.mTimeoutEnabled && SoftApManager.this.mNumAssociatedStations == 0) {
                                    scheduleTimeoutMessage();
                                    break;
                                }
                            }
                            break;
                        case 7:
                            Log.d(SoftApManager.TAG, "Interface was cleanly destroyed.");
                            SoftApManager.this.updateApState(10, 13, 0);
                            SoftApManager.this.mApInterfaceName = null;
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                            break;
                        case 8:
                            Log.w(SoftApManager.TAG, "interface error, stop and report failure");
                            SoftApManager.this.updateApState(14, 13, 0);
                            SoftApManager.this.updateApState(10, 14, 0);
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                            break;
                        case 9:
                            SoftApManager.this.mReportedFrequency = message.arg1;
                            SoftApManager.this.mReportedBandwidth = message.arg2;
                            str = SoftApManager.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Channel switched. Frequency: ");
                            stringBuilder.append(SoftApManager.this.mReportedFrequency);
                            stringBuilder.append(" Bandwidth: ");
                            stringBuilder.append(SoftApManager.this.mReportedBandwidth);
                            Log.d(str, stringBuilder.toString());
                            SoftApManager.this.mWifiMetrics.addSoftApChannelSwitchedEvent(SoftApManager.this.mReportedFrequency, SoftApManager.this.mReportedBandwidth, SoftApManager.this.mMode);
                            int[] allowedChannels = new int[0];
                            if (SoftApManager.this.mApConfig.apBand == 0) {
                                allowedChannels = SoftApManager.this.mWifiNative.getChannelsForBand(1);
                            } else if (SoftApManager.this.mApConfig.apBand == 1) {
                                allowedChannels = SoftApManager.this.mWifiNative.getChannelsForBand(2);
                            } else if (SoftApManager.this.mApConfig.apBand == -1) {
                                allowedChannels = Stream.concat(Arrays.stream(SoftApManager.this.mWifiNative.getChannelsForBand(1)).boxed(), Arrays.stream(SoftApManager.this.mWifiNative.getChannelsForBand(2)).boxed()).mapToInt(-$$Lambda$SoftApManager$SoftApStateMachine$StartedState$gfCssnBJI7TKfXb_Jmv7raVYNkY.INSTANCE).toArray();
                            }
                            if (!ArrayUtils.contains(allowedChannels, SoftApManager.this.mReportedFrequency)) {
                                String str2 = SoftApManager.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Channel does not satisfy user band preference: ");
                                stringBuilder2.append(SoftApManager.this.mReportedFrequency);
                                Log.e(str2, stringBuilder2.toString());
                                SoftApManager.this.mWifiMetrics.incrementNumSoftApUserBandPreferenceUnsatisfied();
                                break;
                            }
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }
        }

        SoftApStateMachine(Looper looper) {
            super(SoftApManager.TAG, looper);
            addState(this.mIdleState);
            addState(this.mStartedState);
            setInitialState(this.mIdleState);
            start();
        }
    }

    public SoftApManager(Context context, Looper looper, FrameworkFacade framework, WifiNative wifiNative, String countryCode, SoftApCallback callback, WifiApConfigStore wifiApConfigStore, SoftApModeConfiguration apConfig, WifiMetrics wifiMetrics) {
        this.mContext = context;
        this.mFrameworkFacade = framework;
        this.mWifiNative = wifiNative;
        this.mCountryCode = countryCode;
        this.mCallback = callback;
        this.mWifiApConfigStore = wifiApConfigStore;
        this.mMode = apConfig.getTargetMode();
        WifiConfiguration config = apConfig.getWifiConfiguration();
        if (config == null) {
            this.mApConfig = this.mWifiApConfigStore.getApConfiguration();
        } else {
            this.mApConfig = config;
        }
        this.mWifiMetrics = wifiMetrics;
        this.mStateMachine = new SoftApStateMachine(looper);
    }

    public void setCountryCode(String countryCode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCountryCode: ");
        stringBuilder.append(countryCode);
        Log.d(str, stringBuilder.toString());
        this.mCountryCode = countryCode;
    }

    private void logStateAndMessage(State state, Message message) {
        String str;
        int i = message.what;
        if (i == 0) {
            str = "CMD_START";
        } else if (i != 3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("what:");
            stringBuilder.append(Integer.toString(message.what));
            str = stringBuilder.toString();
        } else {
            str = "CMD_INTERFACE_STATUS_CHANGED";
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(state.getClass().getSimpleName());
        stringBuilder2.append(": handle message: ");
        stringBuilder2.append(str);
        Log.d(str2, stringBuilder2.toString());
    }

    public void start() {
        this.mStateMachine.sendMessage(0, this.mApConfig);
    }

    public void stop() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" currentstate: ");
        stringBuilder.append(getCurrentStateName());
        Log.d(str, stringBuilder.toString());
        if (this.mApInterfaceName != null) {
            if (this.mIfaceIsUp) {
                updateApState(10, 13, 0);
            } else {
                updateApState(10, 12, 0);
            }
        }
        this.mStateMachine.quitNow();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("--Dump of SoftApManager--");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("current StateMachine mode: ");
        stringBuilder.append(getCurrentStateName());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mApInterfaceName: ");
        stringBuilder.append(this.mApInterfaceName);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mIfaceIsUp: ");
        stringBuilder.append(this.mIfaceIsUp);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mMode: ");
        stringBuilder.append(this.mMode);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCountryCode: ");
        stringBuilder.append(this.mCountryCode);
        pw.println(stringBuilder.toString());
        if (this.mApConfig != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("mApConfig.SSID: ");
            stringBuilder.append(this.mApConfig.SSID);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mApConfig.apBand: ");
            stringBuilder.append(this.mApConfig.apBand);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mApConfig.hiddenSSID: ");
            stringBuilder.append(this.mApConfig.hiddenSSID);
            pw.println(stringBuilder.toString());
        } else {
            pw.println("mApConfig: null");
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNumAssociatedStations: ");
        stringBuilder.append(this.mNumAssociatedStations);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mTimeoutEnabled: ");
        stringBuilder.append(this.mTimeoutEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mReportedFrequency: ");
        stringBuilder.append(this.mReportedFrequency);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mReportedBandwidth: ");
        stringBuilder.append(this.mReportedBandwidth);
        pw.println(stringBuilder.toString());
    }

    private String getCurrentStateName() {
        IState currentState = this.mStateMachine.getCurrentState();
        if (currentState != null) {
            return currentState.getName();
        }
        return "StateMachine not active";
    }

    protected void updateApState(int newState, int currentState, int reason) {
        this.mCallback.onStateChanged(newState, reason);
        Intent intent = new Intent("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("wifi_state", newState);
        intent.putExtra("previous_wifi_state", currentState);
        if (newState == 14) {
            intent.putExtra("wifi_ap_error_code", reason);
        }
        intent.putExtra("wifi_ap_interface_name", this.mApInterfaceName);
        intent.putExtra("wifi_ap_mode", this.mMode);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private int startSoftAp(WifiConfiguration config) {
        if (config == null || config.SSID == null) {
            Log.e(TAG, "Unable to start soft AP without valid configuration");
            return 2;
        }
        WifiConfiguration localConfig = new WifiConfiguration(config);
        int result = updateApChannelConfig(this.mWifiNative, this.mCountryCode, this.mWifiApConfigStore.getAllowed2GChannel(), localConfig);
        if (result != 0) {
            Log.e(TAG, "Failed to update AP band and channel");
            return result;
        } else if (this.mCountryCode == null || this.mWifiNative.setCountryCodeHal(this.mApInterfaceName, this.mCountryCode.toUpperCase(Locale.ROOT)) || config.apBand != 1) {
            if (localConfig.hiddenSSID) {
                Log.d(TAG, "SoftAP is a hidden network");
            }
            localConfig.apChannel = getApChannel(localConfig);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startSoftAp apChannel from ");
            stringBuilder.append(config.apChannel);
            stringBuilder.append(" to ");
            stringBuilder.append(localConfig.apChannel);
            Log.w(str, stringBuilder.toString());
            if (this.mWifiNative.startSoftAp(this.mApInterfaceName, localConfig, this.mSoftApListener)) {
                Log.d(TAG, "Soft AP is started");
                return 0;
            }
            Log.e(TAG, "Soft AP start failed");
            return 2;
        } else {
            Log.e(TAG, "Failed to set country code, required for setting up soft ap in 5GHz");
            return 2;
        }
    }

    private void stopSoftAp() {
        this.mWifiNative.teardownInterface(this.mApInterfaceName);
        Log.d(TAG, "Soft AP is stopped");
    }
}
