package com.android.server.wifi.aware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.wifi.V1_2.NanDataPathChannelInfo;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.aware.Characteristics;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.ConfigRequest.Builder;
import android.net.wifi.aware.IWifiAwareDiscoverySessionCallback;
import android.net.wifi.aware.IWifiAwareEventCallback;
import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.aware.WifiAwareShellCommand.DelegatedShellCommand;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiAwareStateManager implements DelegatedShellCommand {
    private static final byte[] ALL_ZERO_MAC = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private static final int COMMAND_TYPE_CONNECT = 100;
    private static final int COMMAND_TYPE_CREATE_ALL_DATA_PATH_INTERFACES = 112;
    private static final int COMMAND_TYPE_CREATE_DATA_PATH_INTERFACE = 114;
    private static final int COMMAND_TYPE_DELAYED_INITIALIZATION = 121;
    private static final int COMMAND_TYPE_DELETE_ALL_DATA_PATH_INTERFACES = 113;
    private static final int COMMAND_TYPE_DELETE_DATA_PATH_INTERFACE = 115;
    private static final int COMMAND_TYPE_DISABLE_USAGE = 109;
    private static final int COMMAND_TYPE_DISCONNECT = 101;
    private static final int COMMAND_TYPE_ENABLE_USAGE = 108;
    private static final int COMMAND_TYPE_END_DATA_PATH = 118;
    private static final int COMMAND_TYPE_ENQUEUE_SEND_MESSAGE = 107;
    private static final int COMMAND_TYPE_GET_AWARE = 122;
    private static final int COMMAND_TYPE_GET_CAPABILITIES = 111;
    private static final int COMMAND_TYPE_INITIATE_DATA_PATH_SETUP = 116;
    private static final int COMMAND_TYPE_PUBLISH = 103;
    private static final int COMMAND_TYPE_RECONFIGURE = 120;
    private static final int COMMAND_TYPE_RELEASE_AWARE = 123;
    private static final int COMMAND_TYPE_RESPOND_TO_DATA_PATH_SETUP_REQUEST = 117;
    private static final int COMMAND_TYPE_SUBSCRIBE = 105;
    private static final int COMMAND_TYPE_TERMINATE_SESSION = 102;
    private static final int COMMAND_TYPE_TRANSMIT_NEXT_MESSAGE = 119;
    private static final int COMMAND_TYPE_UPDATE_PUBLISH = 104;
    private static final int COMMAND_TYPE_UPDATE_SUBSCRIBE = 106;
    @VisibleForTesting
    public static final String HAL_COMMAND_TIMEOUT_TAG = "WifiAwareStateManager HAL Command Timeout";
    @VisibleForTesting
    public static final String HAL_DATA_PATH_CONFIRM_TIMEOUT_TAG = "WifiAwareStateManager HAL Data Path Confirm Timeout";
    @VisibleForTesting
    public static final String HAL_SEND_MESSAGE_TIMEOUT_TAG = "WifiAwareStateManager HAL Send Message Timeout";
    private static final String MESSAGE_BUNDLE_KEY_CALLING_PACKAGE = "calling_package";
    private static final String MESSAGE_BUNDLE_KEY_CHANNEL = "channel";
    private static final String MESSAGE_BUNDLE_KEY_CHANNEL_REQ_TYPE = "channel_request_type";
    private static final String MESSAGE_BUNDLE_KEY_CONFIG = "config";
    private static final String MESSAGE_BUNDLE_KEY_FILTER_DATA = "filter_data";
    private static final String MESSAGE_BUNDLE_KEY_INTERFACE_NAME = "interface_name";
    private static final String MESSAGE_BUNDLE_KEY_MAC_ADDRESS = "mac_address";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE = "message";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE_ARRIVAL_SEQ = "message_arrival_seq";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE_DATA = "message_data";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE_ID = "message_id";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE_PEER_ID = "message_peer_id";
    private static final String MESSAGE_BUNDLE_KEY_NDP_IDS = "ndp_ids";
    private static final String MESSAGE_BUNDLE_KEY_NOTIFY_IDENTITY_CHANGE = "notify_identity_chg";
    private static final String MESSAGE_BUNDLE_KEY_OOB = "out_of_band";
    private static final String MESSAGE_BUNDLE_KEY_PASSPHRASE = "passphrase";
    private static final String MESSAGE_BUNDLE_KEY_PEER_ID = "peer_id";
    private static final String MESSAGE_BUNDLE_KEY_PID = "pid";
    private static final String MESSAGE_BUNDLE_KEY_PMK = "pmk";
    private static final String MESSAGE_BUNDLE_KEY_REQ_INSTANCE_ID = "req_instance_id";
    private static final String MESSAGE_BUNDLE_KEY_RETRY_COUNT = "retry_count";
    private static final String MESSAGE_BUNDLE_KEY_SEND_MESSAGE_ENQUEUE_TIME = "message_queue_time";
    private static final String MESSAGE_BUNDLE_KEY_SENT_MESSAGE = "send_message";
    private static final String MESSAGE_BUNDLE_KEY_SESSION_ID = "session_id";
    private static final String MESSAGE_BUNDLE_KEY_SESSION_TYPE = "session_type";
    private static final String MESSAGE_BUNDLE_KEY_SSI_DATA = "ssi_data";
    private static final String MESSAGE_BUNDLE_KEY_STATUS_CODE = "status_code";
    private static final String MESSAGE_BUNDLE_KEY_SUCCESS_FLAG = "success_flag";
    private static final String MESSAGE_BUNDLE_KEY_UID = "uid";
    private static final String MESSAGE_RANGE_MM = "range_mm";
    private static final String MESSAGE_RANGING_INDICATION = "ranging_indication";
    private static final int MESSAGE_TYPE_COMMAND = 1;
    private static final int MESSAGE_TYPE_DATA_PATH_TIMEOUT = 6;
    private static final int MESSAGE_TYPE_NOTIFICATION = 3;
    private static final int MESSAGE_TYPE_RESPONSE = 2;
    private static final int MESSAGE_TYPE_RESPONSE_TIMEOUT = 4;
    private static final int MESSAGE_TYPE_SEND_MESSAGE_TIMEOUT = 5;
    private static final int NOTIFICATION_TYPE_AWARE_DOWN = 306;
    private static final int NOTIFICATION_TYPE_CLUSTER_CHANGE = 302;
    private static final int NOTIFICATION_TYPE_INTERFACE_CHANGE = 301;
    private static final int NOTIFICATION_TYPE_MATCH = 303;
    private static final int NOTIFICATION_TYPE_MESSAGE_RECEIVED = 305;
    private static final int NOTIFICATION_TYPE_ON_DATA_PATH_CONFIRM = 310;
    private static final int NOTIFICATION_TYPE_ON_DATA_PATH_END = 311;
    private static final int NOTIFICATION_TYPE_ON_DATA_PATH_REQUEST = 309;
    private static final int NOTIFICATION_TYPE_ON_DATA_PATH_SCHED_UPDATE = 312;
    private static final int NOTIFICATION_TYPE_ON_MESSAGE_SEND_FAIL = 308;
    private static final int NOTIFICATION_TYPE_ON_MESSAGE_SEND_SUCCESS = 307;
    private static final int NOTIFICATION_TYPE_SESSION_TERMINATED = 304;
    public static final String PARAM_ON_IDLE_DISABLE_AWARE = "on_idle_disable_aware";
    public static final int PARAM_ON_IDLE_DISABLE_AWARE_DEFAULT = 1;
    private static final int RESPONSE_TYPE_ON_CAPABILITIES_UPDATED = 206;
    private static final int RESPONSE_TYPE_ON_CONFIG_FAIL = 201;
    private static final int RESPONSE_TYPE_ON_CONFIG_SUCCESS = 200;
    private static final int RESPONSE_TYPE_ON_CREATE_INTERFACE = 207;
    private static final int RESPONSE_TYPE_ON_DELETE_INTERFACE = 208;
    private static final int RESPONSE_TYPE_ON_DISABLE = 213;
    private static final int RESPONSE_TYPE_ON_END_DATA_PATH = 212;
    private static final int RESPONSE_TYPE_ON_INITIATE_DATA_PATH_FAIL = 210;
    private static final int RESPONSE_TYPE_ON_INITIATE_DATA_PATH_SUCCESS = 209;
    private static final int RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_FAIL = 205;
    private static final int RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_SUCCESS = 204;
    private static final int RESPONSE_TYPE_ON_RESPOND_TO_DATA_PATH_SETUP_REQUEST = 211;
    private static final int RESPONSE_TYPE_ON_SESSION_CONFIG_FAIL = 203;
    private static final int RESPONSE_TYPE_ON_SESSION_CONFIG_SUCCESS = 202;
    private static final String TAG = "WifiAwareStateManager";
    private static final boolean VDBG = false;
    private static final boolean VVDBG = false;
    private static final SparseArray<String> sSmToString = MessageUtils.findMessageNames(new Class[]{WifiAwareStateManager.class}, new String[]{"MESSAGE_TYPE", "COMMAND_TYPE", "RESPONSE_TYPE", "NOTIFICATION_TYPE"});
    private WifiAwareMetrics mAwareMetrics;
    private volatile Capabilities mCapabilities;
    private volatile Characteristics mCharacteristics = null;
    private final SparseArray<WifiAwareClientState> mClients = new SparseArray();
    private Context mContext;
    private ConfigRequest mCurrentAwareConfiguration = null;
    private byte[] mCurrentDiscoveryInterfaceMac = ALL_ZERO_MAC;
    private boolean mCurrentIdentityNotification = false;
    public WifiAwareDataPathStateManager mDataPathMgr;
    boolean mDbg = false;
    private LocationManager mLocationManager;
    private PowerManager mPowerManager;
    private Map<String, Integer> mSettableParameters = new HashMap();
    private WifiAwareStateMachine mSm;
    private volatile boolean mUsageEnabled = false;
    private WifiAwareNativeApi mWifiAwareNativeApi;
    private WifiAwareNativeManager mWifiAwareNativeManager;
    private WifiManager mWifiManager;

    @VisibleForTesting
    class WifiAwareStateMachine extends StateMachine {
        private static final long AWARE_SEND_MESSAGE_TIMEOUT = 10000;
        private static final long AWARE_WAIT_FOR_DP_CONFIRM_TIMEOUT = 20000;
        private static final int TRANSACTION_ID_IGNORE = 0;
        private Message mCurrentCommand;
        private short mCurrentTransactionId = (short) 0;
        private final Map<WifiAwareNetworkSpecifier, WakeupMessage> mDataPathConfirmTimeoutMessages = new ArrayMap();
        private DefaultState mDefaultState = new DefaultState(this, null);
        private final Map<Short, Message> mFwQueuedSendMessages = new LinkedHashMap();
        private final SparseArray<Message> mHostQueuedSendMessages = new SparseArray();
        public int mNextSessionId = 1;
        private short mNextTransactionId = (short) 1;
        private int mSendArrivalSequenceCounter = 0;
        private WakeupMessage mSendMessageTimeoutMessage = new WakeupMessage(WifiAwareStateManager.this.mContext, getHandler(), WifiAwareStateManager.HAL_SEND_MESSAGE_TIMEOUT_TAG, 5);
        private boolean mSendQueueBlocked = false;
        private WaitForResponseState mWaitForResponseState = new WaitForResponseState(this, null);
        private WaitState mWaitState = new WaitState(this, null);

        private class DefaultState extends State {
            private DefaultState() {
            }

            /* synthetic */ DefaultState(WifiAwareStateMachine x0, AnonymousClass1 x1) {
                this();
            }

            public boolean processMessage(Message msg) {
                int i = msg.what;
                if (i != 3) {
                    switch (i) {
                        case 5:
                            WifiAwareStateMachine.this.processSendMessageTimeout();
                            return true;
                        case 6:
                            WifiAwareNetworkSpecifier networkSpecifier = msg.obj;
                            if (WifiAwareStateManager.this.mDbg) {
                                String str = WifiAwareStateManager.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("MESSAGE_TYPE_DATA_PATH_TIMEOUT: networkSpecifier=");
                                stringBuilder.append(networkSpecifier);
                                Log.v(str, stringBuilder.toString());
                            }
                            WifiAwareStateManager.this.mDataPathMgr.handleDataPathTimeout(networkSpecifier);
                            WifiAwareStateMachine.this.mDataPathConfirmTimeoutMessages.remove(networkSpecifier);
                            return true;
                        default:
                            String str2 = WifiAwareStateManager.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("DefaultState: should not get non-NOTIFICATION in this state: msg=");
                            stringBuilder2.append(msg);
                            Log.wtf(str2, stringBuilder2.toString());
                            return false;
                    }
                }
                WifiAwareStateMachine.this.processNotification(msg);
                return true;
            }
        }

        private class WaitForResponseState extends State {
            private static final long AWARE_COMMAND_TIMEOUT = 5000;
            private WakeupMessage mTimeoutMessage;

            private WaitForResponseState() {
            }

            /* synthetic */ WaitForResponseState(WifiAwareStateMachine x0, AnonymousClass1 x1) {
                this();
            }

            public void enter() {
                this.mTimeoutMessage = new WakeupMessage(WifiAwareStateManager.this.mContext, WifiAwareStateMachine.this.getHandler(), WifiAwareStateManager.HAL_COMMAND_TIMEOUT_TAG, 4, WifiAwareStateMachine.this.mCurrentCommand.arg1, WifiAwareStateMachine.this.mCurrentTransactionId);
                this.mTimeoutMessage.schedule(SystemClock.elapsedRealtime() + AWARE_COMMAND_TIMEOUT);
            }

            public void exit() {
                this.mTimeoutMessage.cancel();
            }

            public boolean processMessage(Message msg) {
                int i = msg.what;
                String str;
                StringBuilder stringBuilder;
                if (i != 4) {
                    switch (i) {
                        case 1:
                            WifiAwareStateMachine.this.deferMessage(msg);
                            return true;
                        case 2:
                            if (msg.arg2 == WifiAwareStateMachine.this.mCurrentTransactionId) {
                                WifiAwareStateMachine.this.processResponse(msg);
                                WifiAwareStateMachine.this.transitionTo(WifiAwareStateMachine.this.mWaitState);
                            } else {
                                str = WifiAwareStateManager.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("WaitForResponseState: processMessage: non-matching transaction ID on RESPONSE (a very late response) -- msg=");
                                stringBuilder.append(msg);
                                Log.w(str, stringBuilder.toString());
                            }
                            return true;
                        default:
                            return false;
                    }
                }
                if (msg.arg2 == WifiAwareStateMachine.this.mCurrentTransactionId) {
                    WifiAwareStateMachine.this.processTimeout(msg);
                    WifiAwareStateMachine.this.transitionTo(WifiAwareStateMachine.this.mWaitState);
                } else {
                    str = WifiAwareStateManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("WaitForResponseState: processMessage: non-matching transaction ID on RESPONSE_TIMEOUT (either a non-cancelled timeout or a race condition with cancel) -- msg=");
                    stringBuilder.append(msg);
                    Log.w(str, stringBuilder.toString());
                }
                return true;
            }
        }

        private class WaitState extends State {
            private WaitState() {
            }

            /* synthetic */ WaitState(WifiAwareStateMachine x0, AnonymousClass1 x1) {
                this();
            }

            public boolean processMessage(Message msg) {
                int i = msg.what;
                if (i != 4) {
                    switch (i) {
                        case 1:
                            if (WifiAwareStateMachine.this.processCommand(msg)) {
                                WifiAwareStateMachine.this.transitionTo(WifiAwareStateMachine.this.mWaitForResponseState);
                            }
                            return true;
                        case 2:
                            break;
                        default:
                            return false;
                    }
                }
                WifiAwareStateMachine.this.deferMessage(msg);
                return true;
            }
        }

        WifiAwareStateMachine(String name, Looper looper) {
            super(name, looper);
            addState(this.mDefaultState);
            addState(this.mWaitState, this.mDefaultState);
            addState(this.mWaitForResponseState, this.mDefaultState);
            setInitialState(this.mWaitState);
        }

        public void onAwareDownCleanupSendQueueState() {
            this.mSendQueueBlocked = false;
            this.mHostQueuedSendMessages.clear();
            this.mFwQueuedSendMessages.clear();
        }

        private void processNotification(Message msg) {
            Message message = msg;
            int pubSubId;
            short transactionId;
            StringBuilder stringBuilder;
            WifiAwareNetworkSpecifier networkSpecifier;
            switch (message.arg1) {
                case WifiAwareStateManager.NOTIFICATION_TYPE_INTERFACE_CHANGE /*301*/:
                    WifiAwareStateManager.this.onInterfaceAddressChangeLocal(message.obj);
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_CLUSTER_CHANGE /*302*/:
                    WifiAwareStateManager.this.onClusterChangeLocal(message.arg2, message.obj);
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_MATCH /*303*/:
                    WifiAwareStateManager.this.onMatchLocal(message.arg2, msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_REQ_INSTANCE_ID), msg.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), msg.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SSI_DATA), msg.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_FILTER_DATA), msg.getData().getInt(WifiAwareStateManager.MESSAGE_RANGING_INDICATION), msg.getData().getInt(WifiAwareStateManager.MESSAGE_RANGE_MM));
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_SESSION_TERMINATED /*304*/:
                    pubSubId = message.arg2;
                    int reason = ((Integer) message.obj).intValue();
                    WifiAwareStateManager.this.onSessionTerminatedLocal(pubSubId, msg.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_TYPE), reason);
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_MESSAGE_RECEIVED /*305*/:
                    WifiAwareStateManager.this.onMessageReceivedLocal(message.arg2, ((Integer) message.obj).intValue(), msg.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), msg.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_DATA));
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_AWARE_DOWN /*306*/:
                    pubSubId = message.arg2;
                    WifiAwareStateManager.this.onAwareDownLocal();
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_MESSAGE_SEND_SUCCESS /*307*/:
                    transactionId = (short) message.arg2;
                    Message queuedSendCommand = (Message) this.mFwQueuedSendMessages.get(Short.valueOf(transactionId));
                    if (queuedSendCommand == null) {
                        String str = WifiAwareStateManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("processNotification: NOTIFICATION_TYPE_ON_MESSAGE_SEND_SUCCESS: transactionId=");
                        stringBuilder.append(transactionId);
                        stringBuilder.append(" - no such queued send command (timed-out?)");
                        Log.w(str, stringBuilder.toString());
                    } else {
                        this.mFwQueuedSendMessages.remove(Short.valueOf(transactionId));
                        updateSendMessageTimeout();
                        WifiAwareStateManager.this.onMessageSendSuccessLocal(queuedSendCommand);
                    }
                    this.mSendQueueBlocked = false;
                    WifiAwareStateManager.this.transmitNextMessage();
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_MESSAGE_SEND_FAIL /*308*/:
                    transactionId = (short) message.arg2;
                    int reason2 = ((Integer) message.obj).intValue();
                    Message sentMessage = (Message) this.mFwQueuedSendMessages.get(Short.valueOf(transactionId));
                    if (sentMessage != null) {
                        this.mFwQueuedSendMessages.remove(Short.valueOf(transactionId));
                        updateSendMessageTimeout();
                        int retryCount = sentMessage.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_RETRY_COUNT);
                        if (retryCount <= 0 || reason2 != 9) {
                            WifiAwareStateManager.this.onMessageSendFailLocal(sentMessage, reason2);
                        } else {
                            sentMessage.getData().putInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_RETRY_COUNT, retryCount - 1);
                            this.mHostQueuedSendMessages.put(sentMessage.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_ARRIVAL_SEQ), sentMessage);
                        }
                        this.mSendQueueBlocked = false;
                        WifiAwareStateManager.this.transmitNextMessage();
                        break;
                    }
                    String str2 = WifiAwareStateManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("processNotification: NOTIFICATION_TYPE_ON_MESSAGE_SEND_FAIL: transactionId=");
                    stringBuilder.append(transactionId);
                    stringBuilder.append(" - no such queued send command (timed-out?)");
                    Log.w(str2, stringBuilder.toString());
                    break;
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_DATA_PATH_REQUEST /*309*/:
                    networkSpecifier = WifiAwareStateManager.this.mDataPathMgr.onDataPathRequest(message.arg2, msg.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), ((Integer) message.obj).intValue());
                    if (networkSpecifier != null) {
                        WakeupMessage wakeupMessage = new WakeupMessage(WifiAwareStateManager.this.mContext, getHandler(), WifiAwareStateManager.HAL_DATA_PATH_CONFIRM_TIMEOUT_TAG, 6, 0, 0, networkSpecifier);
                        this.mDataPathConfirmTimeoutMessages.put(networkSpecifier, wakeupMessage);
                        wakeupMessage.schedule(SystemClock.elapsedRealtime() + AWARE_WAIT_FOR_DP_CONFIRM_TIMEOUT);
                        break;
                    }
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_DATA_PATH_CONFIRM /*310*/:
                    networkSpecifier = WifiAwareStateManager.this.mDataPathMgr.onDataPathConfirm(message.arg2, msg.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), msg.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE), msg.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_DATA), (List) message.obj);
                    if (networkSpecifier != null) {
                        WakeupMessage timeout = (WakeupMessage) this.mDataPathConfirmTimeoutMessages.remove(networkSpecifier);
                        if (timeout != null) {
                            timeout.cancel();
                            break;
                        }
                    }
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_DATA_PATH_END /*311*/:
                    WifiAwareStateManager.this.mDataPathMgr.onDataPathEnd(message.arg2);
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_DATA_PATH_SCHED_UPDATE /*312*/:
                    WifiAwareStateManager.this.mDataPathMgr.onDataPathSchedUpdate(msg.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), msg.getData().getIntegerArrayList(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_NDP_IDS), (List) message.obj);
                    break;
                default:
                    String str3 = WifiAwareStateManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processNotification: this isn't a NOTIFICATION -- msg=");
                    stringBuilder2.append(message);
                    Log.wtf(str3, stringBuilder2.toString());
                    return;
            }
        }

        private boolean processCommand(Message msg) {
            boolean waitForResponse;
            Message message = msg;
            if (this.mCurrentCommand != null) {
                String str = WifiAwareStateManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processCommand: receiving a command (msg=");
                stringBuilder.append(message);
                stringBuilder.append(") but current (previous) command isn't null (prev_msg=");
                stringBuilder.append(this.mCurrentCommand);
                stringBuilder.append(")");
                Log.wtf(str, stringBuilder.toString());
                this.mCurrentCommand = null;
            }
            short s = this.mNextTransactionId;
            this.mNextTransactionId = (short) (s + 1);
            this.mCurrentTransactionId = s;
            Message sendMsg;
            Bundle data;
            int ndpId;
            switch (message.arg1) {
                case 100:
                    IWifiAwareEventCallback callback = message.obj;
                    ConfigRequest configRequest = (ConfigRequest) msg.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CONFIG);
                    waitForResponse = WifiAwareStateManager.this.connectLocal(this.mCurrentTransactionId, message.arg2, msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_UID), msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PID), msg.getData().getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CALLING_PACKAGE), callback, configRequest, msg.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_NOTIFY_IDENTITY_CHANGE));
                    break;
                case 101:
                    waitForResponse = WifiAwareStateManager.this.disconnectLocal(this.mCurrentTransactionId, message.arg2);
                    break;
                case 102:
                    WifiAwareStateManager.this.terminateSessionLocal(message.arg2, ((Integer) message.obj).intValue());
                    waitForResponse = false;
                    break;
                case 103:
                    PublishConfig publishConfig = (PublishConfig) msg.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CONFIG);
                    waitForResponse = WifiAwareStateManager.this.publishLocal(this.mCurrentTransactionId, message.arg2, publishConfig, message.obj);
                    break;
                case 104:
                    waitForResponse = WifiAwareStateManager.this.updatePublishLocal(this.mCurrentTransactionId, message.arg2, msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_ID), message.obj);
                    break;
                case 105:
                    SubscribeConfig subscribeConfig = (SubscribeConfig) msg.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CONFIG);
                    waitForResponse = WifiAwareStateManager.this.subscribeLocal(this.mCurrentTransactionId, message.arg2, subscribeConfig, message.obj);
                    break;
                case 106:
                    waitForResponse = WifiAwareStateManager.this.updateSubscribeLocal(this.mCurrentTransactionId, message.arg2, msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_ID), message.obj);
                    break;
                case 107:
                    sendMsg = obtainMessage(message.what);
                    sendMsg.copyFrom(message);
                    sendMsg.getData().putInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_ARRIVAL_SEQ, this.mSendArrivalSequenceCounter);
                    this.mHostQueuedSendMessages.put(this.mSendArrivalSequenceCounter, sendMsg);
                    this.mSendArrivalSequenceCounter++;
                    waitForResponse = false;
                    if (!this.mSendQueueBlocked) {
                        WifiAwareStateManager.this.transmitNextMessage();
                        break;
                    }
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_ENABLE_USAGE /*108*/:
                    WifiAwareStateManager.this.enableUsageLocal();
                    waitForResponse = false;
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_DISABLE_USAGE /*109*/:
                    waitForResponse = WifiAwareStateManager.this.disableUsageLocal(this.mCurrentTransactionId);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_GET_CAPABILITIES /*111*/:
                    if (WifiAwareStateManager.this.mCapabilities != null) {
                        waitForResponse = false;
                        break;
                    }
                    waitForResponse = WifiAwareStateManager.this.mWifiAwareNativeApi.getCapabilities(this.mCurrentTransactionId);
                    break;
                case 112:
                    WifiAwareStateManager.this.mDataPathMgr.createAllInterfaces();
                    waitForResponse = false;
                    break;
                case 113:
                    WifiAwareStateManager.this.mDataPathMgr.deleteAllInterfaces();
                    waitForResponse = false;
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_CREATE_DATA_PATH_INTERFACE /*114*/:
                    waitForResponse = WifiAwareStateManager.this.mWifiAwareNativeApi.createAwareNetworkInterface(this.mCurrentTransactionId, (String) message.obj);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_DELETE_DATA_PATH_INTERFACE /*115*/:
                    waitForResponse = WifiAwareStateManager.this.mWifiAwareNativeApi.deleteAwareNetworkInterface(this.mCurrentTransactionId, (String) message.obj);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_INITIATE_DATA_PATH_SETUP /*116*/:
                    data = msg.getData();
                    WifiAwareNetworkSpecifier networkSpecifier = message.obj;
                    waitForResponse = WifiAwareStateManager.this.initiateDataPathSetupLocal(this.mCurrentTransactionId, networkSpecifier, data.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PEER_ID), data.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CHANNEL_REQ_TYPE), data.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CHANNEL), data.getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), data.getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_INTERFACE_NAME), data.getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PMK), data.getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PASSPHRASE), data.getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_OOB));
                    if (waitForResponse) {
                        WakeupMessage timeout = new WakeupMessage(WifiAwareStateManager.this.mContext, getHandler(), WifiAwareStateManager.HAL_DATA_PATH_CONFIRM_TIMEOUT_TAG, 6, 0, 0, networkSpecifier);
                        this.mDataPathConfirmTimeoutMessages.put(networkSpecifier, timeout);
                        timeout.schedule(SystemClock.elapsedRealtime() + AWARE_WAIT_FOR_DP_CONFIRM_TIMEOUT);
                        break;
                    }
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_RESPOND_TO_DATA_PATH_SETUP_REQUEST /*117*/:
                    data = msg.getData();
                    ndpId = message.arg2;
                    waitForResponse = WifiAwareStateManager.this.respondToDataPathRequestLocal(this.mCurrentTransactionId, ((Boolean) message.obj).booleanValue(), ndpId, data.getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_INTERFACE_NAME), data.getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PMK), data.getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PASSPHRASE), data.getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_OOB));
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_END_DATA_PATH /*118*/:
                    waitForResponse = WifiAwareStateManager.this.endDataPathLocal(this.mCurrentTransactionId, message.arg2);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_TRANSMIT_NEXT_MESSAGE /*119*/:
                    if (!this.mSendQueueBlocked && this.mHostQueuedSendMessages.size() != 0) {
                        sendMsg = (Message) this.mHostQueuedSendMessages.valueAt(0);
                        this.mHostQueuedSendMessages.removeAt(0);
                        Bundle data2 = sendMsg.getData();
                        ndpId = sendMsg.arg2;
                        int sessionId = sendMsg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_ID);
                        int peerId = data2.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_PEER_ID);
                        byte[] message2 = data2.getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE);
                        int messageId = data2.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_ID);
                        msg.getData().putParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE, sendMsg);
                        waitForResponse = WifiAwareStateManager.this.sendFollowonMessageLocal(this.mCurrentTransactionId, ndpId, sessionId, peerId, message2, messageId);
                        break;
                    }
                    waitForResponse = false;
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_RECONFIGURE /*120*/:
                    waitForResponse = WifiAwareStateManager.this.reconfigureLocal(this.mCurrentTransactionId);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_DELAYED_INITIALIZATION /*121*/:
                    WifiAwareStateManager.this.mWifiAwareNativeManager.start(getHandler());
                    waitForResponse = false;
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_GET_AWARE /*122*/:
                    WifiAwareStateManager.this.mWifiAwareNativeManager.tryToGetAware();
                    waitForResponse = false;
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_RELEASE_AWARE /*123*/:
                    WifiAwareStateManager.this.mWifiAwareNativeManager.releaseAware();
                    waitForResponse = false;
                    break;
                default:
                    waitForResponse = false;
                    String str2 = WifiAwareStateManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processCommand: this isn't a COMMAND -- msg=");
                    stringBuilder2.append(message);
                    Log.wtf(str2, stringBuilder2.toString());
                    break;
            }
            if (waitForResponse) {
                this.mCurrentCommand = obtainMessage(message.what);
                this.mCurrentCommand.copyFrom(message);
            } else {
                this.mCurrentTransactionId = (short) 0;
            }
            return waitForResponse;
        }

        private void processResponse(Message msg) {
            String str;
            if (this.mCurrentCommand == null) {
                str = WifiAwareStateManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processResponse: no existing command stored!? msg=");
                stringBuilder.append(msg);
                Log.wtf(str, stringBuilder.toString());
                this.mCurrentTransactionId = (short) 0;
                return;
            }
            switch (msg.arg1) {
                case 200:
                    WifiAwareStateManager.this.onConfigCompletedLocal(this.mCurrentCommand);
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_CONFIG_FAIL /*201*/:
                    WifiAwareStateManager.this.onConfigFailedLocal(this.mCurrentCommand, ((Integer) msg.obj).intValue());
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_SESSION_CONFIG_SUCCESS /*202*/:
                    WifiAwareStateManager.this.onSessionConfigSuccessLocal(this.mCurrentCommand, ((Byte) msg.obj).byteValue(), msg.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_TYPE));
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_SESSION_CONFIG_FAIL /*203*/:
                    int reason = ((Integer) msg.obj).intValue();
                    WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, msg.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_TYPE), reason);
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_SUCCESS /*204*/:
                    Message sentMessage = (Message) this.mCurrentCommand.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE);
                    sentMessage.getData().putLong(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SEND_MESSAGE_ENQUEUE_TIME, SystemClock.elapsedRealtime());
                    this.mFwQueuedSendMessages.put(Short.valueOf(this.mCurrentTransactionId), sentMessage);
                    updateSendMessageTimeout();
                    if (!this.mSendQueueBlocked) {
                        WifiAwareStateManager.this.transmitNextMessage();
                        break;
                    }
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_FAIL /*205*/:
                    if (((Integer) msg.obj).intValue() != 11) {
                        WifiAwareStateManager.this.onMessageSendFailLocal((Message) this.mCurrentCommand.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE), 1);
                        if (!this.mSendQueueBlocked) {
                            WifiAwareStateManager.this.transmitNextMessage();
                            break;
                        }
                    }
                    Message sentMessage2 = (Message) this.mCurrentCommand.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE);
                    this.mHostQueuedSendMessages.put(sentMessage2.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_ARRIVAL_SEQ), sentMessage2);
                    this.mSendQueueBlocked = true;
                    break;
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_CAPABILITIES_UPDATED /*206*/:
                    WifiAwareStateManager.this.onCapabilitiesUpdatedResponseLocal((Capabilities) msg.obj);
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_CREATE_INTERFACE /*207*/:
                    WifiAwareStateManager.this.onCreateDataPathInterfaceResponseLocal(this.mCurrentCommand, msg.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE));
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_DELETE_INTERFACE /*208*/:
                    WifiAwareStateManager.this.onDeleteDataPathInterfaceResponseLocal(this.mCurrentCommand, msg.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE));
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_INITIATE_DATA_PATH_SUCCESS /*209*/:
                    WifiAwareStateManager.this.onInitiateDataPathResponseSuccessLocal(this.mCurrentCommand, ((Integer) msg.obj).intValue());
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_INITIATE_DATA_PATH_FAIL /*210*/:
                    WifiAwareStateManager.this.onInitiateDataPathResponseFailLocal(this.mCurrentCommand, ((Integer) msg.obj).intValue());
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_RESPOND_TO_DATA_PATH_SETUP_REQUEST /*211*/:
                    WifiAwareStateManager.this.onRespondToDataPathSetupRequestResponseLocal(this.mCurrentCommand, msg.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE));
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_END_DATA_PATH /*212*/:
                    WifiAwareStateManager.this.onEndPathEndResponseLocal(this.mCurrentCommand, msg.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), msg.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE));
                    break;
                case WifiAwareStateManager.RESPONSE_TYPE_ON_DISABLE /*213*/:
                    WifiAwareStateManager.this.onDisableResponseLocal(this.mCurrentCommand, ((Integer) msg.obj).intValue());
                    break;
                default:
                    str = WifiAwareStateManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processResponse: this isn't a RESPONSE -- msg=");
                    stringBuilder2.append(msg);
                    Log.wtf(str, stringBuilder2.toString());
                    this.mCurrentCommand = null;
                    this.mCurrentTransactionId = (short) 0;
                    return;
            }
            this.mCurrentCommand = null;
            this.mCurrentTransactionId = (short) 0;
        }

        private void processTimeout(Message msg) {
            String str;
            if (WifiAwareStateManager.this.mDbg) {
                str = WifiAwareStateManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processTimeout: msg=");
                stringBuilder.append(msg);
                Log.v(str, stringBuilder.toString());
            }
            StringBuilder stringBuilder2;
            if (this.mCurrentCommand == null) {
                str = WifiAwareStateManager.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("processTimeout: no existing command stored!? msg=");
                stringBuilder2.append(msg);
                Log.wtf(str, stringBuilder2.toString());
                this.mCurrentTransactionId = (short) 0;
                return;
            }
            switch (msg.arg1) {
                case 100:
                    WifiAwareStateManager.this.onConfigFailedLocal(this.mCurrentCommand, 1);
                    break;
                case 101:
                    WifiAwareStateManager.this.onConfigFailedLocal(this.mCurrentCommand, 1);
                    break;
                case 102:
                    Log.wtf(WifiAwareStateManager.TAG, "processTimeout: TERMINATE_SESSION - shouldn't be waiting!");
                    break;
                case 103:
                    WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, true, 1);
                    break;
                case 104:
                    WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, true, 1);
                    break;
                case 105:
                    WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, false, 1);
                    break;
                case 106:
                    WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, false, 1);
                    break;
                case 107:
                    Log.wtf(WifiAwareStateManager.TAG, "processTimeout: ENQUEUE_SEND_MESSAGE - shouldn't be waiting!");
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_ENABLE_USAGE /*108*/:
                    Log.wtf(WifiAwareStateManager.TAG, "processTimeout: ENABLE_USAGE - shouldn't be waiting!");
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_DISABLE_USAGE /*109*/:
                    Log.wtf(WifiAwareStateManager.TAG, "processTimeout: DISABLE_USAGE - shouldn't be waiting!");
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_GET_CAPABILITIES /*111*/:
                    Log.e(WifiAwareStateManager.TAG, "processTimeout: GET_CAPABILITIES timed-out - strange, will try again when next enabled!?");
                    break;
                case 112:
                    Log.wtf(WifiAwareStateManager.TAG, "processTimeout: CREATE_ALL_DATA_PATH_INTERFACES - shouldn't be waiting!");
                    break;
                case 113:
                    Log.wtf(WifiAwareStateManager.TAG, "processTimeout: DELETE_ALL_DATA_PATH_INTERFACES - shouldn't be waiting!");
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_CREATE_DATA_PATH_INTERFACE /*114*/:
                    WifiAwareStateManager.this.onCreateDataPathInterfaceResponseLocal(this.mCurrentCommand, false, 0);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_DELETE_DATA_PATH_INTERFACE /*115*/:
                    WifiAwareStateManager.this.onDeleteDataPathInterfaceResponseLocal(this.mCurrentCommand, false, 0);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_INITIATE_DATA_PATH_SETUP /*116*/:
                    WifiAwareStateManager.this.onInitiateDataPathResponseFailLocal(this.mCurrentCommand, 0);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_RESPOND_TO_DATA_PATH_SETUP_REQUEST /*117*/:
                    WifiAwareStateManager.this.onRespondToDataPathSetupRequestResponseLocal(this.mCurrentCommand, false, 0);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_END_DATA_PATH /*118*/:
                    WifiAwareStateManager.this.onEndPathEndResponseLocal(this.mCurrentCommand, false, 0);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_TRANSMIT_NEXT_MESSAGE /*119*/:
                    WifiAwareStateManager.this.onMessageSendFailLocal((Message) this.mCurrentCommand.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE), 1);
                    this.mSendQueueBlocked = false;
                    WifiAwareStateManager.this.transmitNextMessage();
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_RECONFIGURE /*120*/:
                    WifiAwareStateManager.this.onConfigFailedLocal(this.mCurrentCommand, 1);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_DELAYED_INITIALIZATION /*121*/:
                    Log.wtf(WifiAwareStateManager.TAG, "processTimeout: COMMAND_TYPE_DELAYED_INITIALIZATION - shouldn't be waiting!");
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_GET_AWARE /*122*/:
                    Log.wtf(WifiAwareStateManager.TAG, "processTimeout: COMMAND_TYPE_GET_AWARE - shouldn't be waiting!");
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_RELEASE_AWARE /*123*/:
                    Log.wtf(WifiAwareStateManager.TAG, "processTimeout: COMMAND_TYPE_RELEASE_AWARE - shouldn't be waiting!");
                    break;
                default:
                    str = WifiAwareStateManager.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processTimeout: this isn't a COMMAND -- msg=");
                    stringBuilder2.append(msg);
                    Log.wtf(str, stringBuilder2.toString());
                    break;
            }
            this.mCurrentCommand = null;
            this.mCurrentTransactionId = (short) 0;
        }

        private void updateSendMessageTimeout() {
            Iterator<Message> it = this.mFwQueuedSendMessages.values().iterator();
            if (it.hasNext()) {
                this.mSendMessageTimeoutMessage.schedule(((Message) it.next()).getData().getLong(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SEND_MESSAGE_ENQUEUE_TIME) + AWARE_SEND_MESSAGE_TIMEOUT);
                return;
            }
            this.mSendMessageTimeoutMessage.cancel();
        }

        private void processSendMessageTimeout() {
            if (WifiAwareStateManager.this.mDbg) {
                String str = WifiAwareStateManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processSendMessageTimeout: mHostQueuedSendMessages.size()=");
                stringBuilder.append(this.mHostQueuedSendMessages.size());
                stringBuilder.append(", mFwQueuedSendMessages.size()=");
                stringBuilder.append(this.mFwQueuedSendMessages.size());
                stringBuilder.append(", mSendQueueBlocked=");
                stringBuilder.append(this.mSendQueueBlocked);
                Log.v(str, stringBuilder.toString());
            }
            boolean first = true;
            long currentTime = SystemClock.elapsedRealtime();
            Iterator<Entry<Short, Message>> it = this.mFwQueuedSendMessages.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Short, Message> entry = (Entry) it.next();
                short transactionId = ((Short) entry.getKey()).shortValue();
                Message message = (Message) entry.getValue();
                long messageEnqueueTime = message.getData().getLong(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SEND_MESSAGE_ENQUEUE_TIME);
                if (!first && AWARE_SEND_MESSAGE_TIMEOUT + messageEnqueueTime > currentTime) {
                    break;
                }
                if (WifiAwareStateManager.this.mDbg) {
                    String str2 = WifiAwareStateManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processSendMessageTimeout: expiring - transactionId=");
                    stringBuilder2.append(transactionId);
                    stringBuilder2.append(", message=");
                    stringBuilder2.append(message);
                    stringBuilder2.append(", due to messageEnqueueTime=");
                    stringBuilder2.append(messageEnqueueTime);
                    stringBuilder2.append(", currentTime=");
                    stringBuilder2.append(currentTime);
                    Log.v(str2, stringBuilder2.toString());
                }
                WifiAwareStateManager.this.onMessageSendFailLocal(message, 1);
                it.remove();
                first = false;
            }
            updateSendMessageTimeout();
            this.mSendQueueBlocked = false;
            WifiAwareStateManager.this.transmitNextMessage();
        }

        protected String getLogRecString(Message msg) {
            StringBuilder sb = new StringBuilder(WifiAwareStateManager.messageToString(msg));
            if (msg.what == 1 && this.mCurrentTransactionId != (short) 0) {
                sb.append(" (Transaction ID=");
                sb.append(this.mCurrentTransactionId);
                sb.append(")");
            }
            return sb.toString();
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("WifiAwareStateMachine:");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  mNextTransactionId: ");
            stringBuilder.append(this.mNextTransactionId);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mNextSessionId: ");
            stringBuilder.append(this.mNextSessionId);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mCurrentCommand: ");
            stringBuilder.append(this.mCurrentCommand);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mCurrentTransaction: ");
            stringBuilder.append(this.mCurrentTransactionId);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mSendQueueBlocked: ");
            stringBuilder.append(this.mSendQueueBlocked);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mSendArrivalSequenceCounter: ");
            stringBuilder.append(this.mSendArrivalSequenceCounter);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mHostQueuedSendMessages: [");
            stringBuilder.append(this.mHostQueuedSendMessages);
            stringBuilder.append("]");
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mFwQueuedSendMessages: [");
            stringBuilder.append(this.mFwQueuedSendMessages);
            stringBuilder.append("]");
            pw.println(stringBuilder.toString());
            super.dump(fd, pw, args);
        }
    }

    public WifiAwareStateManager() {
        onReset();
    }

    public void setNative(WifiAwareNativeManager wifiAwareNativeManager, WifiAwareNativeApi wifiAwareNativeApi) {
        this.mWifiAwareNativeManager = wifiAwareNativeManager;
        this.mWifiAwareNativeApi = wifiAwareNativeApi;
    }

    /* JADX WARNING: Removed duplicated region for block: B:48:0x0177  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0141  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x009a  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0177  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0141  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x009a  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0177  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0141  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x009a  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0056  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0177  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0141  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x009a  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0056  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(ShellCommand parentShell) {
        boolean z;
        PrintWriter pw_err = parentShell.getErrPrintWriter();
        PrintWriter pw_out = parentShell.getOutPrintWriter();
        String subCmd = parentShell.getNextArgRequired();
        int hashCode = subCmd.hashCode();
        if (hashCode != -1212873217) {
            if (hashCode != 102230) {
                if (hashCode != 113762) {
                    if (hashCode == 1060304561 && subCmd.equals("allow_ndp_any")) {
                        z = true;
                        String name;
                        StringBuilder stringBuilder;
                        switch (z) {
                            case false:
                                name = parentShell.getNextArgRequired();
                                if (this.mSettableParameters.containsKey(name)) {
                                    String valueStr = parentShell.getNextArgRequired();
                                    try {
                                        this.mSettableParameters.put(name, Integer.valueOf(Integer.valueOf(valueStr).intValue()));
                                        return 0;
                                    } catch (NumberFormatException e) {
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Can't convert value to integer -- '");
                                        stringBuilder2.append(valueStr);
                                        stringBuilder2.append("'");
                                        pw_err.println(stringBuilder2.toString());
                                        return -1;
                                    }
                                }
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown parameter name -- '");
                                stringBuilder.append(name);
                                stringBuilder.append("'");
                                pw_err.println(stringBuilder.toString());
                                return -1;
                            case true:
                                name = parentShell.getNextArgRequired();
                                if (this.mSettableParameters.containsKey(name)) {
                                    pw_out.println(((Integer) this.mSettableParameters.get(name)).intValue());
                                    return 0;
                                }
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unknown parameter name -- '");
                                stringBuilder.append(name);
                                stringBuilder.append("'");
                                pw_err.println(stringBuilder.toString());
                                return -1;
                            case true:
                                JSONObject j = new JSONObject();
                                if (this.mCapabilities != null) {
                                    try {
                                        j.put("maxConcurrentAwareClusters", this.mCapabilities.maxConcurrentAwareClusters);
                                        j.put("maxPublishes", this.mCapabilities.maxPublishes);
                                        j.put("maxSubscribes", this.mCapabilities.maxSubscribes);
                                        j.put("maxServiceNameLen", this.mCapabilities.maxServiceNameLen);
                                        j.put("maxMatchFilterLen", this.mCapabilities.maxMatchFilterLen);
                                        j.put("maxTotalMatchFilterLen", this.mCapabilities.maxTotalMatchFilterLen);
                                        j.put("maxServiceSpecificInfoLen", this.mCapabilities.maxServiceSpecificInfoLen);
                                        j.put("maxExtendedServiceSpecificInfoLen", this.mCapabilities.maxExtendedServiceSpecificInfoLen);
                                        j.put("maxNdiInterfaces", this.mCapabilities.maxNdiInterfaces);
                                        j.put("maxNdpSessions", this.mCapabilities.maxNdpSessions);
                                        j.put("maxAppInfoLen", this.mCapabilities.maxAppInfoLen);
                                        j.put("maxQueuedTransmitMessages", this.mCapabilities.maxQueuedTransmitMessages);
                                        j.put("maxSubscribeInterfaceAddresses", this.mCapabilities.maxSubscribeInterfaceAddresses);
                                        j.put("supportedCipherSuites", this.mCapabilities.supportedCipherSuites);
                                    } catch (JSONException e2) {
                                        String str = TAG;
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("onCommand: get_capabilities e=");
                                        stringBuilder3.append(e2);
                                        Log.e(str, stringBuilder3.toString());
                                    }
                                }
                                pw_out.println(j.toString());
                                return 0;
                            case true:
                                name = parentShell.getNextArgRequired();
                                if (this.mDataPathMgr == null) {
                                    pw_err.println("Null Aware data-path manager - can't configure");
                                    return -1;
                                } else if (TextUtils.equals("true", name)) {
                                    this.mDataPathMgr.mAllowNdpResponderFromAnyOverride = true;
                                    break;
                                } else if (TextUtils.equals("false", name)) {
                                    this.mDataPathMgr.mAllowNdpResponderFromAnyOverride = false;
                                    break;
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Unknown configuration flag for 'allow_ndp_any' - true|false expected -- '");
                                    stringBuilder.append(name);
                                    stringBuilder.append("'");
                                    pw_err.println(stringBuilder.toString());
                                    return -1;
                                }
                        }
                        pw_err.println("Unknown 'wifiaware state_mgr <cmd>'");
                        return -1;
                    }
                } else if (subCmd.equals("set")) {
                    z = false;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                    }
                    pw_err.println("Unknown 'wifiaware state_mgr <cmd>'");
                    return -1;
                }
            } else if (subCmd.equals("get")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                }
                pw_err.println("Unknown 'wifiaware state_mgr <cmd>'");
                return -1;
            }
        } else if (subCmd.equals("get_capabilities")) {
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                case true:
                    break;
            }
            pw_err.println("Unknown 'wifiaware state_mgr <cmd>'");
            return -1;
        }
        z = true;
        switch (z) {
            case false:
                break;
            case true:
                break;
            case true:
                break;
            case true:
                break;
        }
        pw_err.println("Unknown 'wifiaware state_mgr <cmd>'");
        return -1;
    }

    public void onReset() {
        this.mSettableParameters.put(PARAM_ON_IDLE_DISABLE_AWARE, Integer.valueOf(1));
        if (this.mDataPathMgr != null) {
            this.mDataPathMgr.mAllowNdpResponderFromAnyOverride = false;
        }
    }

    public void onHelp(String command, ShellCommand parentShell) {
        PrintWriter pw = parentShell.getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  ");
        stringBuilder.append(command);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("    set <name> <value>: sets named parameter to value. Names: ");
        stringBuilder.append(this.mSettableParameters.keySet());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("    get <name>: gets named parameter value. Names: ");
        stringBuilder.append(this.mSettableParameters.keySet());
        pw.println(stringBuilder.toString());
        pw.println("    get_capabilities: prints out the capabilities as a JSON string");
        pw.println("    allow_ndp_any true|false: configure whether Responders can be specified to accept requests from ANY requestor (null peer spec)");
    }

    public void start(Context context, Looper looper, WifiAwareMetrics awareMetrics, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper permissionsWrapper) {
        Log.i(TAG, "start()");
        this.mContext = context;
        this.mAwareMetrics = awareMetrics;
        this.mSm = new WifiAwareStateMachine(TAG, looper);
        this.mSm.setDbg(false);
        this.mSm.start();
        this.mDataPathMgr = new WifiAwareDataPathStateManager(this);
        this.mDataPathMgr.start(this.mContext, this.mSm.getHandler().getLooper(), awareMetrics, wifiPermissionsUtil, permissionsWrapper);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON") || action.equals("android.intent.action.SCREEN_OFF")) {
                    WifiAwareStateManager.this.reconfigure();
                }
                if (!action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                    return;
                }
                if (((Integer) WifiAwareStateManager.this.mSettableParameters.get(WifiAwareStateManager.PARAM_ON_IDLE_DISABLE_AWARE)).intValue() == 0) {
                    WifiAwareStateManager.this.reconfigure();
                } else if (WifiAwareStateManager.this.mPowerManager.isDeviceIdleMode()) {
                    WifiAwareStateManager.this.disableUsage();
                } else {
                    WifiAwareStateManager.this.enableUsage();
                }
            }
        }, intentFilter);
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.location.MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (WifiAwareStateManager.this.mDbg) {
                    String str = WifiAwareStateManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onReceive: MODE_CHANGED_ACTION: intent=");
                    stringBuilder.append(intent);
                    Log.v(str, stringBuilder.toString());
                }
                if (WifiAwareStateManager.this.mLocationManager.isLocationEnabled()) {
                    WifiAwareStateManager.this.enableUsage();
                } else {
                    WifiAwareStateManager.this.disableUsage();
                }
            }
        }, intentFilter);
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getIntExtra("wifi_state", 4) == 3) {
                    WifiAwareStateManager.this.enableUsage();
                } else {
                    WifiAwareStateManager.this.disableUsage();
                }
            }
        }, intentFilter);
    }

    public void startLate() {
        delayedInitialization();
    }

    WifiAwareClientState getClient(int clientId) {
        return (WifiAwareClientState) this.mClients.get(clientId);
    }

    public Capabilities getCapabilities() {
        return this.mCapabilities;
    }

    public Characteristics getCharacteristics() {
        if (this.mCharacteristics == null && this.mCapabilities != null) {
            this.mCharacteristics = this.mCapabilities.toPublicCharacteristics();
        }
        return this.mCharacteristics;
    }

    public void requestMacAddresses(int uid, List<Integer> peerIds, IWifiAwareMacAddressProvider callback) {
        this.mSm.getHandler().post(new -$$Lambda$WifiAwareStateManager$k1e2sgI9ioQdd4UFKxciMG2eSr4(this, uid, peerIds, callback));
    }

    public static /* synthetic */ void lambda$requestMacAddresses$0(WifiAwareStateManager wifiAwareStateManager, int uid, List peerIds, IWifiAwareMacAddressProvider callback) {
        Map<Integer, byte[]> peerIdToMacMap = new HashMap();
        for (int i = 0; i < wifiAwareStateManager.mClients.size(); i++) {
            WifiAwareClientState client = (WifiAwareClientState) wifiAwareStateManager.mClients.valueAt(i);
            if (client.getUid() == uid) {
                SparseArray<WifiAwareDiscoverySessionState> sessions = client.getSessions();
                for (int j = 0; j < sessions.size(); j++) {
                    WifiAwareDiscoverySessionState session = (WifiAwareDiscoverySessionState) sessions.valueAt(j);
                    for (Integer peerId : peerIds) {
                        int peerId2 = peerId.intValue();
                        PeerInfo peerInfo = session.getPeerInfo(peerId2);
                        if (peerInfo != null) {
                            peerIdToMacMap.put(Integer.valueOf(peerId2), peerInfo.mMac);
                        }
                    }
                }
            }
        }
        try {
            callback.macAddress(peerIdToMacMap);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("requestMacAddress (sync): exception on callback -- ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    public void delayedInitialization() {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_DELAYED_INITIALIZATION;
        this.mSm.sendMessage(msg);
    }

    public void getAwareInterface() {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_GET_AWARE;
        this.mSm.sendMessage(msg);
    }

    public void releaseAwareInterface() {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_RELEASE_AWARE;
        this.mSm.sendMessage(msg);
    }

    public void connect(int clientId, int uid, int pid, String callingPackage, IWifiAwareEventCallback callback, ConfigRequest configRequest, boolean notifyOnIdentityChanged) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 100;
        msg.arg2 = clientId;
        msg.obj = callback;
        msg.getData().putParcelable(MESSAGE_BUNDLE_KEY_CONFIG, configRequest);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_UID, uid);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_PID, pid);
        msg.getData().putString(MESSAGE_BUNDLE_KEY_CALLING_PACKAGE, callingPackage);
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_NOTIFY_IDENTITY_CHANGE, notifyOnIdentityChanged);
        this.mSm.sendMessage(msg);
    }

    public void disconnect(int clientId) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 101;
        msg.arg2 = clientId;
        this.mSm.sendMessage(msg);
    }

    public void reconfigure() {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_RECONFIGURE;
        this.mSm.sendMessage(msg);
    }

    public void terminateSession(int clientId, int sessionId) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 102;
        msg.arg2 = clientId;
        msg.obj = Integer.valueOf(sessionId);
        this.mSm.sendMessage(msg);
    }

    public void publish(int clientId, PublishConfig publishConfig, IWifiAwareDiscoverySessionCallback callback) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 103;
        msg.arg2 = clientId;
        msg.obj = callback;
        msg.getData().putParcelable(MESSAGE_BUNDLE_KEY_CONFIG, publishConfig);
        this.mSm.sendMessage(msg);
    }

    public void updatePublish(int clientId, int sessionId, PublishConfig publishConfig) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 104;
        msg.arg2 = clientId;
        msg.obj = publishConfig;
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_SESSION_ID, sessionId);
        this.mSm.sendMessage(msg);
    }

    public void subscribe(int clientId, SubscribeConfig subscribeConfig, IWifiAwareDiscoverySessionCallback callback) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 105;
        msg.arg2 = clientId;
        msg.obj = callback;
        msg.getData().putParcelable(MESSAGE_BUNDLE_KEY_CONFIG, subscribeConfig);
        this.mSm.sendMessage(msg);
    }

    public void updateSubscribe(int clientId, int sessionId, SubscribeConfig subscribeConfig) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 106;
        msg.arg2 = clientId;
        msg.obj = subscribeConfig;
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_SESSION_ID, sessionId);
        this.mSm.sendMessage(msg);
    }

    public void sendMessage(int clientId, int sessionId, int peerId, byte[] message, int messageId, int retryCount) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 107;
        msg.arg2 = clientId;
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_SESSION_ID, sessionId);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_MESSAGE_PEER_ID, peerId);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_MESSAGE, message);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_MESSAGE_ID, messageId);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_RETRY_COUNT, retryCount);
        this.mSm.sendMessage(msg);
    }

    public void enableUsage() {
        if (((Integer) this.mSettableParameters.get(PARAM_ON_IDLE_DISABLE_AWARE)).intValue() != 0 && this.mPowerManager.isDeviceIdleMode()) {
            if (this.mDbg) {
                Log.d(TAG, "enableUsage(): while device is in IDLE mode - ignoring");
            }
        } else if (!this.mLocationManager.isLocationEnabled()) {
            if (this.mDbg) {
                Log.d(TAG, "enableUsage(): while location is disabled - ignoring");
            }
        } else if (this.mWifiManager.getWifiState() != 3) {
            if (this.mDbg) {
                Log.d(TAG, "enableUsage(): while Wi-Fi is disabled - ignoring");
            }
        } else {
            Message msg = this.mSm.obtainMessage(1);
            msg.arg1 = COMMAND_TYPE_ENABLE_USAGE;
            this.mSm.sendMessage(msg);
        }
    }

    public void disableUsage() {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_DISABLE_USAGE;
        this.mSm.sendMessage(msg);
    }

    public boolean isUsageEnabled() {
        return this.mUsageEnabled;
    }

    public void queryCapabilities() {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_GET_CAPABILITIES;
        this.mSm.sendMessage(msg);
    }

    public void createAllDataPathInterfaces() {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 112;
        this.mSm.sendMessage(msg);
    }

    public void deleteAllDataPathInterfaces() {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = 113;
        this.mSm.sendMessage(msg);
    }

    public void createDataPathInterface(String interfaceName) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_CREATE_DATA_PATH_INTERFACE;
        msg.obj = interfaceName;
        this.mSm.sendMessage(msg);
    }

    public void deleteDataPathInterface(String interfaceName) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_DELETE_DATA_PATH_INTERFACE;
        msg.obj = interfaceName;
        this.mSm.sendMessage(msg);
    }

    public void initiateDataPathSetup(WifiAwareNetworkSpecifier networkSpecifier, int peerId, int channelRequestType, int channel, byte[] peer, String interfaceName, byte[] pmk, String passphrase, boolean isOutOfBand) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_INITIATE_DATA_PATH_SETUP;
        msg.obj = networkSpecifier;
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_PEER_ID, peerId);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_CHANNEL_REQ_TYPE, channelRequestType);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_CHANNEL, channel);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, peer);
        msg.getData().putString(MESSAGE_BUNDLE_KEY_INTERFACE_NAME, interfaceName);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_PMK, pmk);
        msg.getData().putString(MESSAGE_BUNDLE_KEY_PASSPHRASE, passphrase);
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_OOB, isOutOfBand);
        this.mSm.sendMessage(msg);
    }

    public void respondToDataPathRequest(boolean accept, int ndpId, String interfaceName, byte[] pmk, String passphrase, boolean isOutOfBand) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_RESPOND_TO_DATA_PATH_SETUP_REQUEST;
        msg.arg2 = ndpId;
        msg.obj = Boolean.valueOf(accept);
        msg.getData().putString(MESSAGE_BUNDLE_KEY_INTERFACE_NAME, interfaceName);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_PMK, pmk);
        msg.getData().putString(MESSAGE_BUNDLE_KEY_PASSPHRASE, passphrase);
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_OOB, isOutOfBand);
        this.mSm.sendMessage(msg);
    }

    public void endDataPath(int ndpId) {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_END_DATA_PATH;
        msg.arg2 = ndpId;
        this.mSm.sendMessage(msg);
    }

    private void transmitNextMessage() {
        Message msg = this.mSm.obtainMessage(1);
        msg.arg1 = COMMAND_TYPE_TRANSMIT_NEXT_MESSAGE;
        this.mSm.sendMessage(msg);
    }

    public void onConfigSuccessResponse(short transactionId) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = 200;
        msg.arg2 = transactionId;
        this.mSm.sendMessage(msg);
    }

    public void onConfigFailedResponse(short transactionId, int reason) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_CONFIG_FAIL;
        msg.arg2 = transactionId;
        msg.obj = Integer.valueOf(reason);
        this.mSm.sendMessage(msg);
    }

    public void onDisableResponse(short transactionId, int reason) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_DISABLE;
        msg.arg2 = transactionId;
        msg.obj = Integer.valueOf(reason);
        this.mSm.sendMessage(msg);
    }

    public void onSessionConfigSuccessResponse(short transactionId, boolean isPublish, byte pubSubId) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_SESSION_CONFIG_SUCCESS;
        msg.arg2 = transactionId;
        msg.obj = Byte.valueOf(pubSubId);
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_SESSION_TYPE, isPublish);
        this.mSm.sendMessage(msg);
    }

    public void onSessionConfigFailResponse(short transactionId, boolean isPublish, int reason) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_SESSION_CONFIG_FAIL;
        msg.arg2 = transactionId;
        msg.obj = Integer.valueOf(reason);
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_SESSION_TYPE, isPublish);
        this.mSm.sendMessage(msg);
    }

    public void onMessageSendQueuedSuccessResponse(short transactionId) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_SUCCESS;
        msg.arg2 = transactionId;
        this.mSm.sendMessage(msg);
    }

    public void onMessageSendQueuedFailResponse(short transactionId, int reason) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_FAIL;
        msg.arg2 = transactionId;
        msg.obj = Integer.valueOf(reason);
        this.mSm.sendMessage(msg);
    }

    public void onCapabilitiesUpdateResponse(short transactionId, Capabilities capabilities) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_CAPABILITIES_UPDATED;
        msg.arg2 = transactionId;
        msg.obj = capabilities;
        this.mSm.sendMessage(msg);
    }

    public void onCreateDataPathInterfaceResponse(short transactionId, boolean success, int reasonOnFailure) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_CREATE_INTERFACE;
        msg.arg2 = transactionId;
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, success);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, reasonOnFailure);
        this.mSm.sendMessage(msg);
    }

    public void onDeleteDataPathInterfaceResponse(short transactionId, boolean success, int reasonOnFailure) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_DELETE_INTERFACE;
        msg.arg2 = transactionId;
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, success);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, reasonOnFailure);
        this.mSm.sendMessage(msg);
    }

    public void onInitiateDataPathResponseSuccess(short transactionId, int ndpId) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_INITIATE_DATA_PATH_SUCCESS;
        msg.arg2 = transactionId;
        msg.obj = Integer.valueOf(ndpId);
        this.mSm.sendMessage(msg);
    }

    public void onInitiateDataPathResponseFail(short transactionId, int reason) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_INITIATE_DATA_PATH_FAIL;
        msg.arg2 = transactionId;
        msg.obj = Integer.valueOf(reason);
        this.mSm.sendMessage(msg);
    }

    public void onRespondToDataPathSetupRequestResponse(short transactionId, boolean success, int reasonOnFailure) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_RESPOND_TO_DATA_PATH_SETUP_REQUEST;
        msg.arg2 = transactionId;
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, success);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, reasonOnFailure);
        this.mSm.sendMessage(msg);
    }

    public void onEndDataPathResponse(short transactionId, boolean success, int reasonOnFailure) {
        Message msg = this.mSm.obtainMessage(2);
        msg.arg1 = RESPONSE_TYPE_ON_END_DATA_PATH;
        msg.arg2 = transactionId;
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, success);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, reasonOnFailure);
        this.mSm.sendMessage(msg);
    }

    public void onInterfaceAddressChangeNotification(byte[] mac) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_INTERFACE_CHANGE;
        msg.obj = mac;
        this.mSm.sendMessage(msg);
    }

    public void onClusterChangeNotification(int flag, byte[] clusterId) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_CLUSTER_CHANGE;
        msg.arg2 = flag;
        msg.obj = clusterId;
        this.mSm.sendMessage(msg);
    }

    public void onMatchNotification(int pubSubId, int requestorInstanceId, byte[] peerMac, byte[] serviceSpecificInfo, byte[] matchFilter, int rangingIndication, int rangeMm) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_MATCH;
        msg.arg2 = pubSubId;
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_REQ_INSTANCE_ID, requestorInstanceId);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, peerMac);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_SSI_DATA, serviceSpecificInfo);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_FILTER_DATA, matchFilter);
        msg.getData().putInt(MESSAGE_RANGING_INDICATION, rangingIndication);
        msg.getData().putInt(MESSAGE_RANGE_MM, rangeMm);
        this.mSm.sendMessage(msg);
    }

    public void onSessionTerminatedNotification(int pubSubId, int reason, boolean isPublish) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_SESSION_TERMINATED;
        msg.arg2 = pubSubId;
        msg.obj = Integer.valueOf(reason);
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_SESSION_TYPE, isPublish);
        this.mSm.sendMessage(msg);
    }

    public void onMessageReceivedNotification(int pubSubId, int requestorInstanceId, byte[] peerMac, byte[] message) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_MESSAGE_RECEIVED;
        msg.arg2 = pubSubId;
        msg.obj = Integer.valueOf(requestorInstanceId);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, peerMac);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_MESSAGE_DATA, message);
        this.mSm.sendMessage(msg);
    }

    public void onAwareDownNotification(int reason) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_AWARE_DOWN;
        msg.arg2 = reason;
        this.mSm.sendMessage(msg);
    }

    public void onMessageSendSuccessNotification(short transactionId) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_ON_MESSAGE_SEND_SUCCESS;
        msg.arg2 = transactionId;
        this.mSm.sendMessage(msg);
    }

    public void onMessageSendFailNotification(short transactionId, int reason) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_ON_MESSAGE_SEND_FAIL;
        msg.arg2 = transactionId;
        msg.obj = Integer.valueOf(reason);
        this.mSm.sendMessage(msg);
    }

    public void onDataPathRequestNotification(int pubSubId, byte[] mac, int ndpId) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_ON_DATA_PATH_REQUEST;
        msg.arg2 = pubSubId;
        msg.obj = Integer.valueOf(ndpId);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, mac);
        this.mSm.sendMessage(msg);
    }

    public void onDataPathConfirmNotification(int ndpId, byte[] mac, boolean accept, int reason, byte[] message, List<NanDataPathChannelInfo> channelInfo) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_ON_DATA_PATH_CONFIRM;
        msg.arg2 = ndpId;
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, mac);
        msg.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, accept);
        msg.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, reason);
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_MESSAGE_DATA, message);
        msg.obj = channelInfo;
        this.mSm.sendMessage(msg);
    }

    public void onDataPathEndNotification(int ndpId) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_ON_DATA_PATH_END;
        msg.arg2 = ndpId;
        this.mSm.sendMessage(msg);
    }

    public void onDataPathScheduleUpdateNotification(byte[] peerMac, ArrayList<Integer> ndpIds, List<NanDataPathChannelInfo> channelInfo) {
        Message msg = this.mSm.obtainMessage(3);
        msg.arg1 = NOTIFICATION_TYPE_ON_DATA_PATH_SCHED_UPDATE;
        msg.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, peerMac);
        msg.getData().putIntegerArrayList(MESSAGE_BUNDLE_KEY_NDP_IDS, ndpIds);
        msg.obj = channelInfo;
        this.mSm.sendMessage(msg);
    }

    private void sendAwareStateChangedBroadcast(boolean enabled) {
        Intent intent = new Intent("android.net.wifi.aware.action.WIFI_AWARE_STATE_CHANGED");
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean connectLocal(short transactionId, int clientId, int uid, int pid, String callingPackage, IWifiAwareEventCallback callback, ConfigRequest configRequest, boolean notifyIdentityChange) {
        String str;
        int i = clientId;
        IWifiAwareEventCallback iWifiAwareEventCallback = callback;
        ConfigRequest configRequest2 = configRequest;
        boolean z = notifyIdentityChange;
        StringBuilder stringBuilder;
        if (this.mUsageEnabled) {
            String str2;
            if (this.mClients.get(i) != null) {
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("connectLocal: entry already exists for clientId=");
                stringBuilder.append(i);
                Log.e(str2, stringBuilder.toString());
            }
            ConfigRequest merged = mergeConfigRequests(configRequest2);
            if (merged == null) {
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("connectLocal: requested configRequest=");
                stringBuilder.append(configRequest2);
                stringBuilder.append(", incompatible with current configurations");
                Log.e(str2, stringBuilder.toString());
                try {
                    iWifiAwareEventCallback.onConnectFail(1);
                    this.mAwareMetrics.recordAttachStatus(1);
                } catch (RemoteException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("connectLocal onConnectFail(): RemoteException (FYI): ");
                    stringBuilder.append(e);
                    Log.w(str, stringBuilder.toString());
                }
                return false;
            }
            boolean z2;
            ConfigRequest merged2;
            int i2;
            if (this.mCurrentAwareConfiguration == null || !this.mCurrentAwareConfiguration.equals(merged)) {
                z2 = z;
                merged2 = merged;
                i2 = uid;
            } else if (this.mCurrentIdentityNotification || !z) {
                try {
                    iWifiAwareEventCallback.onConnectSuccess(i);
                } catch (RemoteException e2) {
                    RemoteException remoteException = e2;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("connectLocal onConnectSuccess(): RemoteException (FYI): ");
                    stringBuilder.append(e2);
                    Log.w(str, stringBuilder.toString());
                }
                z2 = z;
                WifiAwareClientState wifiAwareClientState = new WifiAwareClientState(this.mContext, i, uid, pid, callingPackage, iWifiAwareEventCallback, configRequest2, z, SystemClock.elapsedRealtime());
                wifiAwareClientState.mDbg = this.mDbg;
                wifiAwareClientState.onInterfaceAddressChange(this.mCurrentDiscoveryInterfaceMac);
                this.mClients.append(i, wifiAwareClientState);
                this.mAwareMetrics.recordAttachSession(uid, z2, this.mClients);
                return false;
            } else {
                z2 = z;
                merged2 = merged;
                i2 = uid;
            }
            boolean notificationRequired = doesAnyClientNeedIdentityChangeNotifications() || z2;
            if (this.mCurrentAwareConfiguration == null) {
                this.mWifiAwareNativeManager.tryToGetAware();
            }
            boolean success = this.mWifiAwareNativeApi.enableAndConfigure(transactionId, merged2, notificationRequired, this.mCurrentAwareConfiguration == null, this.mPowerManager.isInteractive(), this.mPowerManager.isDeviceIdleMode());
            if (!success) {
                try {
                    iWifiAwareEventCallback.onConnectFail(1);
                    this.mAwareMetrics.recordAttachStatus(1);
                } catch (RemoteException e22) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("connectLocal onConnectFail(): RemoteException (FYI):  ");
                    stringBuilder2.append(e22);
                    Log.w(str, stringBuilder2.toString());
                }
            }
            return success;
        }
        Log.w(TAG, "connect(): called with mUsageEnabled=false");
        try {
            iWifiAwareEventCallback.onConnectFail(1);
            this.mAwareMetrics.recordAttachStatus(1);
        } catch (RemoteException e222) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("connectLocal onConnectFail(): RemoteException (FYI): ");
            stringBuilder.append(e222);
            Log.w(str, stringBuilder.toString());
        }
        return false;
    }

    private boolean disconnectLocal(short transactionId, int clientId) {
        WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
        if (client == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("disconnectLocal: no entry for clientId=");
            stringBuilder.append(clientId);
            Log.e(str, stringBuilder.toString());
            return false;
        }
        this.mClients.delete(clientId);
        this.mAwareMetrics.recordAttachSessionDuration(client.getCreationTime());
        SparseArray<WifiAwareDiscoverySessionState> sessions = client.getSessions();
        for (int i = 0; i < sessions.size(); i++) {
            this.mAwareMetrics.recordDiscoverySessionDuration(((WifiAwareDiscoverySessionState) sessions.valueAt(i)).getCreationTime(), ((WifiAwareDiscoverySessionState) sessions.valueAt(i)).isPublishSession());
        }
        client.destroy();
        if (this.mClients.size() == 0) {
            this.mCurrentAwareConfiguration = null;
            deleteAllDataPathInterfaces();
            return this.mWifiAwareNativeApi.disable(transactionId);
        }
        ConfigRequest merged = mergeConfigRequests(null);
        if (merged == null) {
            Log.wtf(TAG, "disconnectLocal: got an incompatible merge on remaining configs!?");
            return false;
        }
        boolean notificationReqs = doesAnyClientNeedIdentityChangeNotifications();
        if (merged.equals(this.mCurrentAwareConfiguration) && this.mCurrentIdentityNotification == notificationReqs) {
            return false;
        }
        return this.mWifiAwareNativeApi.enableAndConfigure(transactionId, merged, notificationReqs, false, this.mPowerManager.isInteractive(), this.mPowerManager.isDeviceIdleMode());
    }

    private boolean reconfigureLocal(short transactionId) {
        if (this.mClients.size() == 0) {
            return false;
        }
        boolean notificationReqs = doesAnyClientNeedIdentityChangeNotifications();
        return this.mWifiAwareNativeApi.enableAndConfigure(transactionId, this.mCurrentAwareConfiguration, notificationReqs, false, this.mPowerManager.isInteractive(), this.mPowerManager.isDeviceIdleMode());
    }

    private void terminateSessionLocal(int clientId, int sessionId) {
        WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
        if (client == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("terminateSession: no client exists for clientId=");
            stringBuilder.append(clientId);
            Log.e(str, stringBuilder.toString());
            return;
        }
        WifiAwareDiscoverySessionState session = client.terminateSession(sessionId);
        if (session != null) {
            this.mAwareMetrics.recordDiscoverySessionDuration(session.getCreationTime(), session.isPublishSession());
        }
    }

    private boolean publishLocal(short transactionId, int clientId, PublishConfig publishConfig, IWifiAwareDiscoverySessionCallback callback) {
        WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
        if (client == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("publishLocal: no client exists for clientId=");
            stringBuilder.append(clientId);
            Log.e(str, stringBuilder.toString());
            return false;
        }
        boolean success = this.mWifiAwareNativeApi.publish(transactionId, (byte) 0, publishConfig);
        if (!success) {
            try {
                callback.onSessionConfigFail(1);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("publishLocal onSessionConfigFail(): RemoteException (FYI): ");
                stringBuilder2.append(e);
                Log.w(str2, stringBuilder2.toString());
            }
            this.mAwareMetrics.recordDiscoveryStatus(client.getUid(), 1, true);
        }
        return success;
    }

    private boolean updatePublishLocal(short transactionId, int clientId, int sessionId, PublishConfig publishConfig) {
        WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
        if (client == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updatePublishLocal: no client exists for clientId=");
            stringBuilder.append(clientId);
            Log.e(str, stringBuilder.toString());
            return false;
        }
        WifiAwareDiscoverySessionState session = client.getSession(sessionId);
        if (session == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updatePublishLocal: no session exists for clientId=");
            stringBuilder2.append(clientId);
            stringBuilder2.append(", sessionId=");
            stringBuilder2.append(sessionId);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
        boolean status = session.updatePublish(transactionId, publishConfig);
        if (!status) {
            this.mAwareMetrics.recordDiscoveryStatus(client.getUid(), 1, true);
        }
        return status;
    }

    private boolean subscribeLocal(short transactionId, int clientId, SubscribeConfig subscribeConfig, IWifiAwareDiscoverySessionCallback callback) {
        WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
        if (client == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("subscribeLocal: no client exists for clientId=");
            stringBuilder.append(clientId);
            Log.e(str, stringBuilder.toString());
            return false;
        }
        boolean success = this.mWifiAwareNativeApi.subscribe(transactionId, (byte) 0, subscribeConfig);
        if (!success) {
            try {
                callback.onSessionConfigFail(1);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("subscribeLocal onSessionConfigFail(): RemoteException (FYI): ");
                stringBuilder2.append(e);
                Log.w(str2, stringBuilder2.toString());
            }
            this.mAwareMetrics.recordDiscoveryStatus(client.getUid(), 1, false);
        }
        return success;
    }

    private boolean updateSubscribeLocal(short transactionId, int clientId, int sessionId, SubscribeConfig subscribeConfig) {
        WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
        if (client == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateSubscribeLocal: no client exists for clientId=");
            stringBuilder.append(clientId);
            Log.e(str, stringBuilder.toString());
            return false;
        }
        WifiAwareDiscoverySessionState session = client.getSession(sessionId);
        if (session == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateSubscribeLocal: no session exists for clientId=");
            stringBuilder2.append(clientId);
            stringBuilder2.append(", sessionId=");
            stringBuilder2.append(sessionId);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
        boolean status = session.updateSubscribe(transactionId, subscribeConfig);
        if (!status) {
            this.mAwareMetrics.recordDiscoveryStatus(client.getUid(), 1, false);
        }
        return status;
    }

    private boolean sendFollowonMessageLocal(short transactionId, int clientId, int sessionId, int peerId, byte[] message, int messageId) {
        WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
        if (client == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendFollowonMessageLocal: no client exists for clientId=");
            stringBuilder.append(clientId);
            Log.e(str, stringBuilder.toString());
            return false;
        }
        WifiAwareDiscoverySessionState session = client.getSession(sessionId);
        if (session != null) {
            return session.sendMessage(transactionId, peerId, message, messageId);
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("sendFollowonMessageLocal: no session exists for clientId=");
        stringBuilder2.append(clientId);
        stringBuilder2.append(", sessionId=");
        stringBuilder2.append(sessionId);
        Log.e(str2, stringBuilder2.toString());
        return false;
    }

    private void enableUsageLocal() {
        if (this.mCapabilities == null) {
            getAwareInterface();
            queryCapabilities();
            releaseAwareInterface();
        }
        if (!this.mUsageEnabled) {
            this.mUsageEnabled = true;
            sendAwareStateChangedBroadcast(true);
            this.mAwareMetrics.recordEnableUsage();
        }
    }

    private boolean disableUsageLocal(short transactionId) {
        if (!this.mUsageEnabled) {
            return false;
        }
        onAwareDownLocal();
        this.mUsageEnabled = false;
        boolean callDispatched = this.mWifiAwareNativeApi.disable(transactionId);
        sendAwareStateChangedBroadcast(false);
        this.mAwareMetrics.recordDisableUsage();
        return callDispatched;
    }

    private boolean initiateDataPathSetupLocal(short transactionId, WifiAwareNetworkSpecifier networkSpecifier, int peerId, int channelRequestType, int channel, byte[] peer, String interfaceName, byte[] pmk, String passphrase, boolean isOutOfBand) {
        boolean success = this.mWifiAwareNativeApi.initiateDataPath(transactionId, peerId, channelRequestType, channel, peer, interfaceName, pmk, passphrase, isOutOfBand, this.mCapabilities);
        if (success) {
            WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier = networkSpecifier;
        } else {
            this.mDataPathMgr.onDataPathInitiateFail(networkSpecifier, 1);
        }
        return success;
    }

    private boolean respondToDataPathRequestLocal(short transactionId, boolean accept, int ndpId, String interfaceName, byte[] pmk, String passphrase, boolean isOutOfBand) {
        boolean success = this.mWifiAwareNativeApi.respondToDataPathRequest(transactionId, accept, ndpId, interfaceName, pmk, passphrase, isOutOfBand, this.mCapabilities);
        if (success) {
            int i = ndpId;
        } else {
            this.mDataPathMgr.onRespondToDataPathRequest(ndpId, false, 1);
        }
        return success;
    }

    private boolean endDataPathLocal(short transactionId, int ndpId) {
        return this.mWifiAwareNativeApi.endDataPath(transactionId, ndpId);
    }

    private void onConfigCompletedLocal(Message completedCommand) {
        Message message = completedCommand;
        if (message.arg1 == 100) {
            Bundle data = completedCommand.getData();
            int clientId = message.arg2;
            IWifiAwareEventCallback callback = message.obj;
            ConfigRequest configRequest = (ConfigRequest) data.getParcelable(MESSAGE_BUNDLE_KEY_CONFIG);
            int uid = data.getInt(MESSAGE_BUNDLE_KEY_UID);
            int pid = data.getInt(MESSAGE_BUNDLE_KEY_PID);
            boolean z = data.getBoolean(MESSAGE_BUNDLE_KEY_NOTIFY_IDENTITY_CHANGE);
            String callingPackage = data.getString(MESSAGE_BUNDLE_KEY_CALLING_PACKAGE);
            boolean notifyIdentityChange = z;
            IWifiAwareEventCallback callback2 = callback;
            int uid2 = uid;
            WifiAwareClientState client = new WifiAwareClientState(this.mContext, clientId, uid, pid, callingPackage, callback, configRequest, z, SystemClock.elapsedRealtime());
            client.mDbg = this.mDbg;
            this.mClients.put(clientId, client);
            this.mAwareMetrics.recordAttachSession(uid2, notifyIdentityChange, this.mClients);
            try {
                callback2.onConnectSuccess(clientId);
            } catch (RemoteException e) {
                RemoteException remoteException = e;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onConfigCompletedLocal onConnectSuccess(): RemoteException (FYI): ");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
            }
            client.onInterfaceAddressChange(this.mCurrentDiscoveryInterfaceMac);
            message = completedCommand;
        } else {
            message = completedCommand;
            if (!(message.arg1 == 101 || message.arg1 == COMMAND_TYPE_RECONFIGURE)) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onConfigCompletedLocal: unexpected completedCommand=");
                stringBuilder2.append(message);
                Log.wtf(str2, stringBuilder2.toString());
                return;
            }
        }
        if (this.mCurrentAwareConfiguration == null) {
            createAllDataPathInterfaces();
        }
        this.mCurrentAwareConfiguration = mergeConfigRequests(null);
        if (this.mCurrentAwareConfiguration == null) {
            Log.wtf(TAG, "onConfigCompletedLocal: got a null merged configuration after config!?");
        }
        this.mCurrentIdentityNotification = doesAnyClientNeedIdentityChangeNotifications();
    }

    private void onConfigFailedLocal(Message failedCommand, int reason) {
        if (failedCommand.arg1 == 100) {
            try {
                failedCommand.obj.onConnectFail(reason);
                this.mAwareMetrics.recordAttachStatus(reason);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onConfigFailedLocal onConnectFail(): RemoteException (FYI): ");
                stringBuilder.append(e);
                Log.w(str, stringBuilder.toString());
            }
        } else if (!(failedCommand.arg1 == 101 || failedCommand.arg1 == COMMAND_TYPE_RECONFIGURE)) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onConfigFailedLocal: unexpected failedCommand=");
            stringBuilder2.append(failedCommand);
            Log.wtf(str2, stringBuilder2.toString());
        }
    }

    private void onDisableResponseLocal(Message command, int reason) {
        if (reason != 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDisableResponseLocal: FAILED!? command=");
            stringBuilder.append(command);
            stringBuilder.append(", reason=");
            stringBuilder.append(reason);
            Log.e(str, stringBuilder.toString());
        }
        this.mAwareMetrics.recordDisableAware();
    }

    private void onSessionConfigSuccessLocal(Message completedCommand, byte pubSubId, boolean isPublish) {
        Message message = completedCommand;
        int clientId;
        String str;
        StringBuilder stringBuilder;
        int i;
        if (message.arg1 == 103 || message.arg1 == 105) {
            clientId = message.arg2;
            IWifiAwareDiscoverySessionCallback callback = message.obj;
            WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
            if (client == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onSessionConfigSuccessLocal: no client exists for clientId=");
                stringBuilder.append(clientId);
                Log.e(str, stringBuilder.toString());
                return;
            }
            WifiAwareStateMachine wifiAwareStateMachine = this.mSm;
            i = wifiAwareStateMachine.mNextSessionId;
            wifiAwareStateMachine.mNextSessionId = i + 1;
            int sessionId = i;
            WifiAwareClientState client2;
            try {
                boolean isRangingEnabled;
                callback.onSessionStarted(sessionId);
                i = -1;
                int maxRange = -1;
                if (message.arg1 == 103) {
                    isRangingEnabled = ((PublishConfig) completedCommand.getData().getParcelable(MESSAGE_BUNDLE_KEY_CONFIG)).mEnableRanging;
                } else {
                    SubscribeConfig subscribeConfig = (SubscribeConfig) completedCommand.getData().getParcelable(MESSAGE_BUNDLE_KEY_CONFIG);
                    boolean z = subscribeConfig.mMinDistanceMmSet || subscribeConfig.mMaxDistanceMmSet;
                    isRangingEnabled = z;
                    if (subscribeConfig.mMinDistanceMmSet) {
                        i = subscribeConfig.mMinDistanceMm;
                    }
                    if (subscribeConfig.mMaxDistanceMmSet) {
                        maxRange = subscribeConfig.mMaxDistanceMm;
                    }
                }
                int minRange = i;
                int maxRange2 = maxRange;
                client2 = client;
                sessionId = new WifiAwareDiscoverySessionState(this.mWifiAwareNativeApi, sessionId, pubSubId, callback, isPublish, isRangingEnabled, SystemClock.elapsedRealtime());
                sessionId.mDbg = this.mDbg;
                client2.addSession(sessionId);
                if (isRangingEnabled) {
                    this.mAwareMetrics.recordDiscoverySessionWithRanging(client2.getUid(), message.arg1 != 103, minRange, maxRange2, this.mClients);
                } else {
                    this.mAwareMetrics.recordDiscoverySession(client2.getUid(), this.mClients);
                }
                this.mAwareMetrics.recordDiscoveryStatus(client2.getUid(), 0, message.arg1 == 103);
            } catch (RemoteException e) {
                int i2 = sessionId;
                IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback = callback;
                client2 = client;
                RemoteException remoteException = e;
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onSessionConfigSuccessLocal: onSessionStarted() RemoteException=");
                stringBuilder2.append(e);
                Log.e(str2, stringBuilder2.toString());
            }
        } else if (message.arg1 == 104 || message.arg1 == 106) {
            clientId = message.arg2;
            i = completedCommand.getData().getInt(MESSAGE_BUNDLE_KEY_SESSION_ID);
            WifiAwareClientState client3 = (WifiAwareClientState) this.mClients.get(clientId);
            if (client3 == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onSessionConfigSuccessLocal: no client exists for clientId=");
                stringBuilder.append(clientId);
                Log.e(str, stringBuilder.toString());
                return;
            }
            WifiAwareDiscoverySessionState session = client3.getSession(i);
            if (session == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onSessionConfigSuccessLocal: no session exists for clientId=");
                stringBuilder.append(clientId);
                stringBuilder.append(", sessionId=");
                stringBuilder.append(i);
                Log.e(str, stringBuilder.toString());
                return;
            }
            try {
                session.getCallback().onSessionConfigSuccess();
            } catch (RemoteException e2) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("onSessionConfigSuccessLocal: onSessionConfigSuccess() RemoteException=");
                stringBuilder3.append(e2);
                Log.e(str3, stringBuilder3.toString());
            }
            this.mAwareMetrics.recordDiscoveryStatus(client3.getUid(), 0, message.arg1 == 104);
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onSessionConfigSuccessLocal: unexpected completedCommand=");
            stringBuilder.append(message);
            Log.wtf(str, stringBuilder.toString());
        }
    }

    private void onSessionConfigFailLocal(Message failedCommand, boolean isPublish, int reason) {
        boolean z = false;
        int clientId;
        WifiAwareClientState client;
        String str;
        StringBuilder stringBuilder;
        if (failedCommand.arg1 == 103 || failedCommand.arg1 == 105) {
            clientId = failedCommand.arg2;
            IWifiAwareDiscoverySessionCallback callback = failedCommand.obj;
            client = (WifiAwareClientState) this.mClients.get(clientId);
            if (client == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onSessionConfigFailLocal: no client exists for clientId=");
                stringBuilder.append(clientId);
                Log.e(str, stringBuilder.toString());
                return;
            }
            try {
                callback.onSessionConfigFail(reason);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onSessionConfigFailLocal onSessionConfigFail(): RemoteException (FYI): ");
                stringBuilder2.append(e);
                Log.w(str2, stringBuilder2.toString());
            }
            WifiAwareMetrics wifiAwareMetrics = this.mAwareMetrics;
            int uid = client.getUid();
            if (failedCommand.arg1 == 103) {
                z = true;
            }
            wifiAwareMetrics.recordDiscoveryStatus(uid, reason, z);
        } else if (failedCommand.arg1 == 104 || failedCommand.arg1 == 106) {
            clientId = failedCommand.arg2;
            int sessionId = failedCommand.getData().getInt(MESSAGE_BUNDLE_KEY_SESSION_ID);
            client = (WifiAwareClientState) this.mClients.get(clientId);
            if (client == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onSessionConfigFailLocal: no client exists for clientId=");
                stringBuilder.append(clientId);
                Log.e(str, stringBuilder.toString());
                return;
            }
            WifiAwareDiscoverySessionState session = client.getSession(sessionId);
            if (session == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onSessionConfigFailLocal: no session exists for clientId=");
                stringBuilder.append(clientId);
                stringBuilder.append(", sessionId=");
                stringBuilder.append(sessionId);
                Log.e(str, stringBuilder.toString());
                return;
            }
            try {
                session.getCallback().onSessionConfigFail(reason);
            } catch (RemoteException e2) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("onSessionConfigFailLocal: onSessionConfigFail() RemoteException=");
                stringBuilder3.append(e2);
                Log.e(str3, stringBuilder3.toString());
            }
            WifiAwareMetrics wifiAwareMetrics2 = this.mAwareMetrics;
            int uid2 = client.getUid();
            if (failedCommand.arg1 == 104) {
                z = true;
            }
            wifiAwareMetrics2.recordDiscoveryStatus(uid2, reason, z);
            if (reason == 3) {
                client.removeSession(sessionId);
            }
        } else {
            String str4 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("onSessionConfigFailLocal: unexpected failedCommand=");
            stringBuilder4.append(failedCommand);
            Log.wtf(str4, stringBuilder4.toString());
        }
    }

    private void onMessageSendSuccessLocal(Message completedCommand) {
        int clientId = completedCommand.arg2;
        int sessionId = completedCommand.getData().getInt(MESSAGE_BUNDLE_KEY_SESSION_ID);
        int messageId = completedCommand.getData().getInt(MESSAGE_BUNDLE_KEY_MESSAGE_ID);
        WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
        if (client == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onMessageSendSuccessLocal: no client exists for clientId=");
            stringBuilder.append(clientId);
            Log.e(str, stringBuilder.toString());
            return;
        }
        WifiAwareDiscoverySessionState session = client.getSession(sessionId);
        if (session == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onMessageSendSuccessLocal: no session exists for clientId=");
            stringBuilder2.append(clientId);
            stringBuilder2.append(", sessionId=");
            stringBuilder2.append(sessionId);
            Log.e(str2, stringBuilder2.toString());
            return;
        }
        try {
            session.getCallback().onMessageSendSuccess(messageId);
        } catch (RemoteException e) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onMessageSendSuccessLocal: RemoteException (FYI): ");
            stringBuilder3.append(e);
            Log.w(str3, stringBuilder3.toString());
        }
    }

    private void onMessageSendFailLocal(Message failedCommand, int reason) {
        int clientId = failedCommand.arg2;
        int sessionId = failedCommand.getData().getInt(MESSAGE_BUNDLE_KEY_SESSION_ID);
        int messageId = failedCommand.getData().getInt(MESSAGE_BUNDLE_KEY_MESSAGE_ID);
        WifiAwareClientState client = (WifiAwareClientState) this.mClients.get(clientId);
        if (client == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onMessageSendFailLocal: no client exists for clientId=");
            stringBuilder.append(clientId);
            Log.e(str, stringBuilder.toString());
            return;
        }
        WifiAwareDiscoverySessionState session = client.getSession(sessionId);
        if (session == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onMessageSendFailLocal: no session exists for clientId=");
            stringBuilder2.append(clientId);
            stringBuilder2.append(", sessionId=");
            stringBuilder2.append(sessionId);
            Log.e(str2, stringBuilder2.toString());
            return;
        }
        try {
            session.getCallback().onMessageSendFail(messageId, reason);
        } catch (RemoteException e) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onMessageSendFailLocal: onMessageSendFail RemoteException=");
            stringBuilder3.append(e);
            Log.e(str3, stringBuilder3.toString());
        }
    }

    private void onCapabilitiesUpdatedResponseLocal(Capabilities capabilities) {
        this.mCapabilities = capabilities;
        this.mCharacteristics = null;
    }

    private void onCreateDataPathInterfaceResponseLocal(Message command, boolean success, int reasonOnFailure) {
        if (success) {
            this.mDataPathMgr.onInterfaceCreated((String) command.obj);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onCreateDataPathInterfaceResponseLocal: failed when trying to create interface ");
        stringBuilder.append(command.obj);
        stringBuilder.append(". Reason code=");
        stringBuilder.append(reasonOnFailure);
        Log.e(str, stringBuilder.toString());
    }

    private void onDeleteDataPathInterfaceResponseLocal(Message command, boolean success, int reasonOnFailure) {
        if (success) {
            this.mDataPathMgr.onInterfaceDeleted((String) command.obj);
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onDeleteDataPathInterfaceResponseLocal: failed when trying to delete interface ");
        stringBuilder.append(command.obj);
        stringBuilder.append(". Reason code=");
        stringBuilder.append(reasonOnFailure);
        Log.e(str, stringBuilder.toString());
    }

    private void onInitiateDataPathResponseSuccessLocal(Message command, int ndpId) {
        this.mDataPathMgr.onDataPathInitiateSuccess((WifiAwareNetworkSpecifier) command.obj, ndpId);
    }

    private void onInitiateDataPathResponseFailLocal(Message command, int reason) {
        this.mDataPathMgr.onDataPathInitiateFail((WifiAwareNetworkSpecifier) command.obj, reason);
    }

    private void onRespondToDataPathSetupRequestResponseLocal(Message command, boolean success, int reasonOnFailure) {
        this.mDataPathMgr.onRespondToDataPathRequest(command.arg2, success, reasonOnFailure);
    }

    private void onEndPathEndResponseLocal(Message command, boolean success, int reasonOnFailure) {
    }

    private void onInterfaceAddressChangeLocal(byte[] mac) {
        this.mCurrentDiscoveryInterfaceMac = mac;
        for (int i = 0; i < this.mClients.size(); i++) {
            ((WifiAwareClientState) this.mClients.valueAt(i)).onInterfaceAddressChange(mac);
        }
        this.mAwareMetrics.recordEnableAware();
    }

    private void onClusterChangeLocal(int flag, byte[] clusterId) {
        for (int i = 0; i < this.mClients.size(); i++) {
            ((WifiAwareClientState) this.mClients.valueAt(i)).onClusterChange(flag, clusterId, this.mCurrentDiscoveryInterfaceMac);
        }
        this.mAwareMetrics.recordEnableAware();
    }

    private void onMatchLocal(int pubSubId, int requestorInstanceId, byte[] peerMac, byte[] serviceSpecificInfo, byte[] matchFilter, int rangingIndication, int rangeMm) {
        Pair<WifiAwareClientState, WifiAwareDiscoverySessionState> data = getClientSessionForPubSubId(pubSubId);
        if (data == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onMatch: no session found for pubSubId=");
            stringBuilder.append(pubSubId);
            Log.e(str, stringBuilder.toString());
            return;
        }
        int i = pubSubId;
        if (((WifiAwareDiscoverySessionState) data.second).isRangingEnabled()) {
            this.mAwareMetrics.recordMatchIndicationForRangeEnabledSubscribe(rangingIndication != 0);
        }
        ((WifiAwareDiscoverySessionState) data.second).onMatch(requestorInstanceId, peerMac, serviceSpecificInfo, matchFilter, rangingIndication, rangeMm);
    }

    private void onSessionTerminatedLocal(int pubSubId, boolean isPublish, int reason) {
        Pair<WifiAwareClientState, WifiAwareDiscoverySessionState> data = getClientSessionForPubSubId(pubSubId);
        if (data == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSessionTerminatedLocal: no session found for pubSubId=");
            stringBuilder.append(pubSubId);
            Log.e(str, stringBuilder.toString());
            return;
        }
        try {
            ((WifiAwareDiscoverySessionState) data.second).getCallback().onSessionTerminated(reason);
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onSessionTerminatedLocal onSessionTerminated(): RemoteException (FYI): ");
            stringBuilder2.append(e);
            Log.w(str2, stringBuilder2.toString());
        }
        ((WifiAwareClientState) data.first).removeSession(((WifiAwareDiscoverySessionState) data.second).getSessionId());
        this.mAwareMetrics.recordDiscoverySessionDuration(((WifiAwareDiscoverySessionState) data.second).getCreationTime(), ((WifiAwareDiscoverySessionState) data.second).isPublishSession());
    }

    private void onMessageReceivedLocal(int pubSubId, int requestorInstanceId, byte[] peerMac, byte[] message) {
        Pair<WifiAwareClientState, WifiAwareDiscoverySessionState> data = getClientSessionForPubSubId(pubSubId);
        if (data == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onMessageReceivedLocal: no session found for pubSubId=");
            stringBuilder.append(pubSubId);
            Log.e(str, stringBuilder.toString());
            return;
        }
        ((WifiAwareDiscoverySessionState) data.second).onMessageReceived(requestorInstanceId, peerMac, message);
    }

    private void onAwareDownLocal() {
        if (this.mCurrentAwareConfiguration != null) {
            for (int i = 0; i < this.mClients.size(); i++) {
                this.mAwareMetrics.recordAttachSessionDuration(((WifiAwareClientState) this.mClients.valueAt(i)).getCreationTime());
                SparseArray<WifiAwareDiscoverySessionState> sessions = ((WifiAwareClientState) this.mClients.valueAt(i)).getSessions();
                for (int j = 0; j < sessions.size(); j++) {
                    this.mAwareMetrics.recordDiscoverySessionDuration(((WifiAwareDiscoverySessionState) sessions.valueAt(i)).getCreationTime(), ((WifiAwareDiscoverySessionState) sessions.valueAt(i)).isPublishSession());
                }
            }
            this.mAwareMetrics.recordDisableAware();
            this.mClients.clear();
            this.mCurrentAwareConfiguration = null;
            this.mSm.onAwareDownCleanupSendQueueState();
            this.mDataPathMgr.onAwareDownCleanupDataPaths();
            this.mCurrentDiscoveryInterfaceMac = ALL_ZERO_MAC;
            deleteAllDataPathInterfaces();
        }
    }

    private Pair<WifiAwareClientState, WifiAwareDiscoverySessionState> getClientSessionForPubSubId(int pubSubId) {
        for (int i = 0; i < this.mClients.size(); i++) {
            WifiAwareClientState client = (WifiAwareClientState) this.mClients.valueAt(i);
            WifiAwareDiscoverySessionState session = client.getAwareSessionStateForPubSubId(pubSubId);
            if (session != null) {
                return new Pair(client, session);
            }
        }
        return null;
    }

    private ConfigRequest mergeConfigRequests(ConfigRequest configRequest) {
        ConfigRequest configRequest2 = configRequest;
        ConfigRequest configRequest3 = null;
        if (this.mClients.size() == 0 && configRequest2 == null) {
            Log.e(TAG, "mergeConfigRequests: invalid state - called with 0 clients registered!");
            return null;
        }
        boolean support5gBand = false;
        int masterPreference = 0;
        boolean clusterIdValid = false;
        int clusterLow = 0;
        int clusterHigh = Constants.SHORT_MASK;
        int[] discoveryWindowInterval = new int[]{-1, -1};
        if (configRequest2 != null) {
            support5gBand = configRequest2.mSupport5gBand;
            masterPreference = configRequest2.mMasterPreference;
            clusterIdValid = true;
            clusterLow = configRequest2.mClusterLow;
            clusterHigh = configRequest2.mClusterHigh;
            discoveryWindowInterval = configRequest2.mDiscoveryWindowInterval;
        }
        int band = 0;
        boolean clusterIdValid2 = clusterIdValid;
        int masterPreference2 = masterPreference;
        boolean support5gBand2 = support5gBand;
        int i = 0;
        while (i < this.mClients.size()) {
            ConfigRequest cr = ((WifiAwareClientState) this.mClients.valueAt(i)).getConfigRequest();
            if (cr.mSupport5gBand) {
                support5gBand2 = true;
            }
            masterPreference2 = Math.max(masterPreference2, cr.mMasterPreference);
            if (!clusterIdValid2) {
                clusterIdValid2 = true;
                clusterLow = cr.mClusterLow;
                clusterHigh = cr.mClusterHigh;
            } else if (!(clusterLow == cr.mClusterLow && clusterHigh == cr.mClusterHigh)) {
                return configRequest3;
            }
            for (int band2 = 0; band2 <= 1; band2++) {
                if (discoveryWindowInterval[band2] == -1) {
                    discoveryWindowInterval[band2] = cr.mDiscoveryWindowInterval[band2];
                } else if (cr.mDiscoveryWindowInterval[band2] != -1) {
                    if (discoveryWindowInterval[band2] == 0) {
                        discoveryWindowInterval[band2] = cr.mDiscoveryWindowInterval[band2];
                    } else if (cr.mDiscoveryWindowInterval[band2] != 0) {
                        discoveryWindowInterval[band2] = Math.min(discoveryWindowInterval[band2], cr.mDiscoveryWindowInterval[band2]);
                    }
                }
            }
            i++;
            configRequest3 = null;
        }
        Builder builder = new Builder().setSupport5gBand(support5gBand2).setMasterPreference(masterPreference2).setClusterLow(clusterLow).setClusterHigh(clusterHigh);
        while (true) {
            int band3 = band;
            if (band3 > 1) {
                return builder.build();
            }
            if (discoveryWindowInterval[band3] != -1) {
                builder.setDiscoveryWindowInterval(band3, discoveryWindowInterval[band3]);
            }
            band = band3 + 1;
        }
    }

    private boolean doesAnyClientNeedIdentityChangeNotifications() {
        for (int i = 0; i < this.mClients.size(); i++) {
            if (((WifiAwareClientState) this.mClients.valueAt(i)).getNotifyIdentityChange()) {
                return true;
            }
        }
        return false;
    }

    private static String messageToString(Message msg) {
        StringBuilder sb = new StringBuilder();
        String s = (String) sSmToString.get(msg.what);
        if (s == null) {
            s = "<unknown>";
        }
        sb.append(s);
        sb.append("/");
        if (msg.what == 3 || msg.what == 1 || msg.what == 2) {
            s = (String) sSmToString.get(msg.arg1);
            if (s == null) {
                s = "<unknown>";
            }
            sb.append(s);
        }
        if (msg.what == 2 || msg.what == 4) {
            sb.append(" (Transaction ID=");
            sb.append(msg.arg2);
            sb.append(")");
        }
        return sb.toString();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("AwareStateManager:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mClients: [");
        stringBuilder.append(this.mClients);
        stringBuilder.append("]");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mUsageEnabled: ");
        stringBuilder.append(this.mUsageEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mCapabilities: [");
        stringBuilder.append(this.mCapabilities);
        stringBuilder.append("]");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mCurrentAwareConfiguration: ");
        stringBuilder.append(this.mCurrentAwareConfiguration);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mCurrentIdentityNotification: ");
        stringBuilder.append(this.mCurrentIdentityNotification);
        pw.println(stringBuilder.toString());
        for (int i = 0; i < this.mClients.size(); i++) {
            ((WifiAwareClientState) this.mClients.valueAt(i)).dump(fd, pw, args);
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mSettableParameters: ");
        stringBuilder.append(this.mSettableParameters);
        pw.println(stringBuilder.toString());
        this.mSm.dump(fd, pw, args);
        this.mDataPathMgr.dump(fd, pw, args);
        this.mWifiAwareNativeApi.dump(fd, pw, args);
        pw.println("mAwareMetrics:");
        this.mAwareMetrics.dump(fd, pw, args);
    }
}
