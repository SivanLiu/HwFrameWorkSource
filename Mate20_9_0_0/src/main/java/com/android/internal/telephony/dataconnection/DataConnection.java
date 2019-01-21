package com.android.internal.telephony.dataconnection;

import android.app.PendingIntent;
import android.content.Context;
import android.hardware.radio.V1_2.ScanIntervalRange;
import android.net.ConnectivityManager;
import android.net.KeepalivePacketData;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkMisc;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StringNetworkSpecifier;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Pair;
import android.util.SparseArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.AbstractPhoneInternalInterface;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CarrierSignalAgent;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwDataConnectionManager;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwTelephonyChrManager.Scenario;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.LinkCapacityEstimate;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.google.android.mms.pdu.CharacterSets;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class DataConnection extends StateMachine {
    static final int BASE = 262144;
    private static final int CMD_TO_STRING_COUNT = 26;
    private static final boolean DBG = true;
    private static final int EHRPD_MAX_RETRY = 2;
    static final int EVENT_BW_REFRESH_RESPONSE = 262158;
    static final int EVENT_CLEAR_LINK = 262168;
    static final int EVENT_CONNECT = 262144;
    static final int EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED = 262155;
    static final int EVENT_DATA_CONNECTION_OVERRIDE_CHANGED = 262161;
    static final int EVENT_DATA_CONNECTION_ROAM_OFF = 262157;
    static final int EVENT_DATA_CONNECTION_ROAM_ON = 262156;
    static final int EVENT_DATA_CONNECTION_VOICE_CALL_ENDED = 262160;
    static final int EVENT_DATA_CONNECTION_VOICE_CALL_STARTED = 262159;
    static final int EVENT_DATA_STATE_CHANGED = 262151;
    static final int EVENT_DEACTIVATE_DONE = 262147;
    static final int EVENT_DISCONNECT = 262148;
    static final int EVENT_DISCONNECT_ALL = 262150;
    static final int EVENT_KEEPALIVE_STARTED = 262163;
    static final int EVENT_KEEPALIVE_START_REQUEST = 262165;
    static final int EVENT_KEEPALIVE_STATUS = 262162;
    static final int EVENT_KEEPALIVE_STOPPED = 262164;
    static final int EVENT_KEEPALIVE_STOP_REQUEST = 262166;
    static final int EVENT_LINK_CAPACITY_CHANGED = 262167;
    static final int EVENT_LOST_CONNECTION = 262153;
    static final int EVENT_RESUME_LINK = 262169;
    static final int EVENT_RIL_CONNECTED = 262149;
    static final int EVENT_SETUP_DATA_CONNECTION_DONE = 262145;
    static final int EVENT_TEAR_DOWN_NOW = 262152;
    private static boolean HW_SET_EHRPD_DATA = SystemProperties.getBoolean("ro.config.hwpp_set_ehrpd_data", false);
    private static final String NETWORK_TYPE = "MOBILE";
    private static final String NULL_IP = "0.0.0.0";
    private static final String TCP_BUFFER_SIZES_1XRTT = "16384,32768,131072,4096,16384,102400";
    private static final String TCP_BUFFER_SIZES_EDGE = "4093,26280,70800,4096,16384,70800";
    private static final String TCP_BUFFER_SIZES_EHRPD = "131072,262144,1048576,4096,16384,524288";
    private static final String TCP_BUFFER_SIZES_EVDO = "4094,87380,262144,4096,16384,262144";
    private static final String TCP_BUFFER_SIZES_GPRS = "4092,8760,48000,4096,8760,48000";
    private static final String TCP_BUFFER_SIZES_HSDPA = "61167,367002,1101005,8738,52429,262114";
    private static final String TCP_BUFFER_SIZES_HSPA = "40778,244668,734003,16777,100663,301990";
    private static final String TCP_BUFFER_SIZES_HSPAP = "122334,734003,2202010,32040,192239,576717";
    private static final String TCP_BUFFER_SIZES_LTE = "524288,4194304,8388608,262144,524288,1048576";
    private static final String TCP_BUFFER_SIZES_UMTS = "58254,349525,1048576,58254,349525,1048576";
    private static final boolean VDBG = true;
    private static AtomicInteger mInstanceNumber = new AtomicInteger(0);
    private static String[] sCmdToString = new String[26];
    private ConnectionParams deferConnectParams = null;
    private boolean keepNetwork = false;
    private AsyncChannel mAc;
    private DcActivatingState mActivatingState = new DcActivatingState(this, null);
    private DcActiveState mActiveState = new DcActiveState(this, null);
    public HashMap<ApnContext, ConnectionParams> mApnContexts = null;
    private ApnSetting mApnSetting;
    public int mCid;
    private ConnectionParams mConnectionParams;
    private long mCreateTime;
    private int mDataRegState = KeepaliveStatus.INVALID_HANDLE;
    private DataServiceManager mDataServiceManager;
    private DcController mDcController;
    private DcFailCause mDcFailCause;
    private DcTesterFailBringUpAll mDcTesterFailBringUpAll;
    private DcTracker mDct = null;
    private DcDefaultState mDefaultState = new DcDefaultState(this, null);
    private DisconnectParams mDisconnectParams;
    private DcDisconnectionErrorCreatingConnection mDisconnectingErrorCreatingConnection = new DcDisconnectionErrorCreatingConnection(this, null);
    private DcDisconnectingState mDisconnectingState = new DcDisconnectingState(this, null);
    private int mEhrpdFailCount = 0;
    private HwCustDataConnection mHwCustDataConnection;
    private int mId;
    private DcInactiveState mInactiveState = new DcInactiveState(this, null);
    private DcFailCause mLastFailCause;
    private long mLastFailTime;
    private LinkProperties mLastLinkProperties = null;
    private LinkProperties mLinkProperties = new LinkProperties();
    private LocalLog mNetCapsLocalLog = new LocalLog(50);
    private DcNetworkAgent mNetworkAgent;
    private NetworkInfo mNetworkInfo;
    protected String[] mPcscfAddr;
    private Phone mPhone;
    PendingIntent mReconnectIntent = null;
    private boolean mRestrictedNetworkOverride = false;
    private int mRilRat = KeepaliveStatus.INVALID_HANDLE;
    private int mSubscriptionOverride;
    int mTag;
    private Object mUserData;
    private boolean misLastFailed = false;
    DisconnectParams toDisconnectParams = null;

    public static class ConnectionParams {
        ApnContext mApnContext;
        final int mConnectionGeneration;
        Message mOnCompletedMsg;
        int mProfileId;
        int mRilRat;
        int mTag;
        final boolean mUnmeteredUseOnly;
        boolean mdefered = false;

        ConnectionParams(ApnContext apnContext, int profileId, int rilRadioTechnology, boolean unmeteredUseOnly, Message onCompletedMsg, int connectionGeneration) {
            this.mApnContext = apnContext;
            this.mProfileId = profileId;
            this.mRilRat = rilRadioTechnology;
            this.mUnmeteredUseOnly = unmeteredUseOnly;
            this.mOnCompletedMsg = onCompletedMsg;
            this.mConnectionGeneration = connectionGeneration;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{mTag=");
            stringBuilder.append(this.mTag);
            stringBuilder.append(" mApnContext=");
            stringBuilder.append(this.mApnContext);
            stringBuilder.append(" mProfileId=");
            stringBuilder.append(this.mProfileId);
            stringBuilder.append(" mRat=");
            stringBuilder.append(this.mRilRat);
            stringBuilder.append(" mUnmeteredUseOnly=");
            stringBuilder.append(this.mUnmeteredUseOnly);
            stringBuilder.append(" mOnCompletedMsg=");
            stringBuilder.append(DataConnection.msgToString(this.mOnCompletedMsg));
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private class DcActivatingState extends State {
        private DcActivatingState() {
        }

        /* synthetic */ DcActivatingState(DataConnection x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            boolean canHandleType;
            int phoneId = DataConnection.this.mPhone.getPhoneId();
            int access$1100 = DataConnection.this.mId;
            long j = DataConnection.this.mApnSetting != null ? (long) DataConnection.this.mApnSetting.typesBitmap : 0;
            if (DataConnection.this.mApnSetting != null) {
                canHandleType = DataConnection.this.mApnSetting.canHandleType("default");
            } else {
                canHandleType = false;
            }
            StatsLog.write(75, 2, phoneId, access$1100, j, canHandleType);
            for (ApnContext apnContext : DataConnection.this.mApnContexts.keySet()) {
                if (apnContext.getState() == DctConstants.State.RETRYING) {
                    DataConnection.this.log("DcActivatingState: Set Retrying To Connecting!");
                    apnContext.setState(DctConstants.State.CONNECTING);
                }
            }
            DataConnection.this.deferConnectParams = null;
            DataConnection.this.toDisconnectParams = null;
        }

        public boolean processMessage(Message msg) {
            DataConnection dataConnection = DataConnection.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DcActivatingState: msg=");
            stringBuilder.append(DataConnection.msgToString(msg));
            dataConnection.log(stringBuilder.toString());
            int i = msg.what;
            boolean retVal = false;
            if (i != DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED) {
                ConnectionParams cp;
                switch (i) {
                    case InboundSmsTracker.DEST_PORT_FLAG_3GPP2 /*262144*/:
                        cp = (ConnectionParams) msg.obj;
                        cp.mdefered = true;
                        DataConnection.this.deferConnectParams = cp;
                        DataConnection.this.deferMessage(msg);
                        retVal = true;
                        break;
                    case DataConnection.EVENT_SETUP_DATA_CONNECTION_DONE /*262145*/:
                        DataConnection dataConnection2;
                        StringBuilder stringBuilder2;
                        cp = msg.obj;
                        DataCallResponse dataCallResponse = (DataCallResponse) msg.getData().getParcelable("data_call_response");
                        SetupResult result = DataConnection.this.onSetupConnectionCompleted(msg.arg1, dataCallResponse, cp);
                        if (!(result == SetupResult.ERROR_STALE || DataConnection.this.mConnectionParams == cp)) {
                            dataConnection2 = DataConnection.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("DcActivatingState: WEIRD mConnectionsParams:");
                            stringBuilder2.append(DataConnection.this.mConnectionParams);
                            stringBuilder2.append(" != cp:");
                            stringBuilder2.append(cp);
                            dataConnection2.loge(stringBuilder2.toString());
                        }
                        dataConnection2 = DataConnection.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("DcActivatingState onSetupConnectionCompleted result=");
                        stringBuilder2.append(result);
                        dataConnection2.log(stringBuilder2.toString());
                        if (cp.mApnContext != null) {
                            ApnContext apnContext = cp.mApnContext;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("onSetupConnectionCompleted result=");
                            stringBuilder2.append(result);
                            apnContext.requestLog(stringBuilder2.toString());
                        }
                        if (result != SetupResult.SUCCESS) {
                            VSimUtilsInner.checkMmsStop(DataConnection.this.mPhone.getPhoneId());
                        }
                        if (!(result == SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR && DataConnection.this.mRilRat == 13)) {
                            DataConnection.this.mEhrpdFailCount = 0;
                        }
                        switch (result) {
                            case SUCCESS:
                                DataConnection.this.mDcFailCause = DcFailCause.NONE;
                                DataConnection.this.transitionTo(DataConnection.this.mActiveState);
                                break;
                            case ERROR_RADIO_NOT_AVAILABLE:
                                DataConnection.this.mInactiveState.setEnterNotificationParams(cp, result.mFailCause);
                                DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                                break;
                            case ERROR_INVALID_ARG:
                                DataConnection.this.tearDownData(cp);
                                DataConnection.this.transitionTo(DataConnection.this.mDisconnectingErrorCreatingConnection);
                                break;
                            case ERROR_DATA_SERVICE_SPECIFIC_ERROR:
                                long delay = DataConnection.this.getSuggestedRetryDelay(dataCallResponse);
                                cp.mApnContext.setModemSuggestedDelay(delay);
                                String str = new StringBuilder();
                                str.append("DcActivatingState: ERROR_DATA_SERVICE_SPECIFIC_ERROR  delay=");
                                str.append(delay);
                                str.append(" result=");
                                str.append(result);
                                str.append(" result.isRestartRadioFail=");
                                str.append(result.mFailCause.isRestartRadioFail(DataConnection.this.mPhone.getContext(), DataConnection.this.mPhone.getSubId()));
                                str.append(" isPermanentFailure=");
                                str.append(DataConnection.this.mDct.isPermanentFailure(result.mFailCause));
                                str = str.toString();
                                DataConnection.this.log(str);
                                if (cp.mApnContext != null) {
                                    cp.mApnContext.requestLog(str);
                                }
                                DataConnection.this.mInactiveState.setEnterNotificationParams(cp, result.mFailCause);
                                DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                                if (DataConnection.HW_SET_EHRPD_DATA && DataConnection.this.mDct.isCTSimCard(DataConnection.this.mPhone.getPhoneId()) && DataConnection.this.mRilRat == 13 && !result.mFailCause.isRestartRadioFail(DataConnection.this.mPhone.getContext(), DataConnection.this.mPhone.getSubId())) {
                                    String apnContextType = cp.mApnContext.getApnType();
                                    if ("default".equals(apnContextType) || "mms".equals(apnContextType)) {
                                        if (DataConnection.this.mEhrpdFailCount < 2 && !result.mFailCause.isPermanentFailure(DataConnection.this.mPhone.getContext(), DataConnection.this.mPhone.getSubId())) {
                                            DataConnection.this.mEhrpdFailCount = DataConnection.this.mEhrpdFailCount + 1;
                                            break;
                                        }
                                        DataConnection.this.mPhone.mCi.setEhrpdByQMI(false);
                                        DataConnection.this.mEhrpdFailCount = 0;
                                        DataConnection.this.logd("ehrpd fail times reaches EHRPD_MAX_RETRY or permanent fail ,disable eHRPD.");
                                        break;
                                    }
                                }
                                break;
                            case ERROR_STALE:
                                DataConnection dataConnection3 = DataConnection.this;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("DcActivatingState: stale EVENT_SETUP_DATA_CONNECTION_DONE tag:");
                                stringBuilder3.append(cp.mTag);
                                stringBuilder3.append(" != mTag:");
                                stringBuilder3.append(DataConnection.this.mTag);
                                dataConnection3.loge(stringBuilder3.toString());
                                break;
                            default:
                                throw new RuntimeException("Unknown SetupResult, should not happen");
                        }
                        retVal = true;
                        break;
                    default:
                        dataConnection = DataConnection.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("DcActivatingState not handled msg.what=");
                        stringBuilder.append(DataConnection.this.getWhatToString(msg.what));
                        stringBuilder.append(" RefCount=");
                        stringBuilder.append(DataConnection.this.mApnContexts.size());
                        dataConnection.log(stringBuilder.toString());
                        break;
                }
            }
            DataConnection.this.deferMessage(msg);
            retVal = true;
            return retVal;
        }
    }

    private class DcActiveState extends State {
        private DcActiveState() {
        }

        /* synthetic */ DcActiveState(DataConnection x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            boolean canHandleType;
            DataConnection.this.log("DcActiveState: enter dc=*");
            int phoneId = DataConnection.this.mPhone.getPhoneId();
            int access$1100 = DataConnection.this.mId;
            long j = DataConnection.this.mApnSetting != null ? (long) DataConnection.this.mApnSetting.typesBitmap : 0;
            if (DataConnection.this.mApnSetting != null) {
                canHandleType = DataConnection.this.mApnSetting.canHandleType("default");
            } else {
                canHandleType = false;
            }
            StatsLog.write(75, 3, phoneId, access$1100, j, canHandleType);
            DataConnection.this.updateNetworkInfo();
            DataConnection.this.notifyAllOfConnected(PhoneInternalInterface.REASON_CONNECTED);
            if (DataConnection.this.mPhone.getCallTracker() != null) {
                DataConnection.this.mPhone.getCallTracker().registerForVoiceCallStarted(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_STARTED, null);
                DataConnection.this.mPhone.getCallTracker().registerForVoiceCallEnded(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_ENDED, null);
            }
            DataConnection.this.mDcController.addActiveDcByCid(DataConnection.this);
            DataConnection.this.mNetworkInfo.setDetailedState(DetailedState.CONNECTED, DataConnection.this.mNetworkInfo.getReason(), null);
            DataConnection.this.mNetworkInfo.setExtraInfo(DataConnection.this.mApnSetting.apn);
            DataConnection.this.updateTcpBufferSizes(DataConnection.this.mRilRat);
            NetworkMisc misc = new NetworkMisc();
            if (DataConnection.this.mPhone.getCarrierSignalAgent().hasRegisteredReceivers("com.android.internal.telephony.CARRIER_SIGNAL_REDIRECTED")) {
                misc.provisioningNotificationDisabled = true;
            }
            misc.subscriberId = DataConnection.this.mPhone.getSubscriberId();
            DataConnection.this.setNetworkRestriction();
            DataConnection dataConnection = DataConnection.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mRestrictedNetworkOverride = ");
            stringBuilder.append(DataConnection.this.mRestrictedNetworkOverride);
            dataConnection.log(stringBuilder.toString());
            DataConnection.this.mNetworkAgent = new DcNetworkAgent(DataConnection.this, DataConnection.this.getHandler().getLooper(), DataConnection.this.mPhone.getContext(), "DcNetworkAgent", DataConnection.this.mNetworkInfo, DataConnection.this.getNetworkCapabilities(), DataConnection.this.mLinkProperties, 50, misc);
            DataConnection.this.mPhone.mCi.registerForNattKeepaliveStatus(DataConnection.this.getHandler(), DataConnection.EVENT_KEEPALIVE_STATUS, null);
            DataConnection.this.mPhone.mCi.registerForLceInfo(DataConnection.this.getHandler(), DataConnection.EVENT_LINK_CAPACITY_CHANGED, null);
        }

        public void exit() {
            DataConnection dataConnection = DataConnection.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DcActiveState: exit dc=");
            stringBuilder.append(this);
            dataConnection.log(stringBuilder.toString());
            String reason = DataConnection.this.mNetworkInfo.getReason();
            if (DataConnection.this.mDcController.isExecutingCarrierChange()) {
                reason = PhoneInternalInterface.REASON_CARRIER_CHANGE;
            } else if (DataConnection.this.mDisconnectParams != null && DataConnection.this.mDisconnectParams.mReason != null) {
                reason = DataConnection.this.mDisconnectParams.mReason;
            } else if (DataConnection.this.mDcFailCause != null) {
                reason = DataConnection.this.mDcFailCause.toString();
            }
            if (DataConnection.this.mPhone.getCallTracker() != null) {
                DataConnection.this.mPhone.getCallTracker().unregisterForVoiceCallStarted(DataConnection.this.getHandler());
                DataConnection.this.mPhone.getCallTracker().unregisterForVoiceCallEnded(DataConnection.this.getHandler());
            }
            DataConnection.this.mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, reason, DataConnection.this.mNetworkInfo.getExtraInfo());
            DataConnection.this.mPhone.mCi.unregisterForNattKeepaliveStatus(DataConnection.this.getHandler());
            DataConnection.this.mPhone.mCi.unregisterForLceInfo(DataConnection.this.getHandler());
            if (DataConnection.this.mNetworkAgent != null) {
                DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                DataConnection.this.mNetworkAgent = null;
            }
            DataConnection.this.keepNetwork = false;
            DataConnection.this.mLastLinkProperties = null;
        }

        public boolean processMessage(Message msg) {
            boolean retVal;
            Message message = msg;
            int i = message.what;
            DataConnection dataConnection;
            StringBuilder stringBuilder;
            DisconnectParams dp;
            DataConnection dataConnection2;
            StringBuilder stringBuilder2;
            if (i == InboundSmsTracker.DEST_PORT_FLAG_3GPP2) {
                ConnectionParams cp = message.obj;
                DataConnection.this.mApnContexts.put(cp.mApnContext, cp);
                dataConnection = DataConnection.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DcActiveState: EVENT_CONNECT cp=");
                stringBuilder.append(cp);
                stringBuilder.append(" dc=");
                stringBuilder.append(DataConnection.this);
                dataConnection.log(stringBuilder.toString());
                if (DataConnection.this.mNetworkAgent != null) {
                    DataConnection.this.mNetworkAgent.sendRematchNetworkAndRequests(DataConnection.this.mNetworkInfo);
                }
                DataConnection.this.notifyConnectCompleted(cp, DcFailCause.NONE, false);
                retVal = true;
            } else if (i == DataConnection.EVENT_DISCONNECT) {
                dp = (DisconnectParams) message.obj;
                DataConnection.this.log("DcActiveState: EVENT_DISCONNECT dp=*");
                if (DataConnection.this.mApnContexts.containsKey(dp.mApnContext)) {
                    dataConnection = DataConnection.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DcActiveState msg.what=EVENT_DISCONNECT RefCount=");
                    stringBuilder.append(DataConnection.this.mApnContexts.size());
                    dataConnection.log(stringBuilder.toString());
                    if (DataConnection.this.mApnContexts.size() == 1) {
                        DataConnection.this.mApnContexts.clear();
                        DataConnection.this.mDisconnectParams = dp;
                        DataConnection.this.mConnectionParams = null;
                        dp.mTag = DataConnection.this.mTag;
                        DataConnection.this.tearDownData(dp);
                        DataConnection.this.transitionTo(DataConnection.this.mDisconnectingState);
                    } else {
                        DataConnection.this.mApnContexts.remove(dp.mApnContext);
                        DataConnection.this.notifyDisconnectCompleted(dp, false);
                    }
                } else {
                    dataConnection = DataConnection.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DcActiveState ERROR no such apnContext=");
                    stringBuilder.append(dp.mApnContext);
                    stringBuilder.append(" in this dc=");
                    stringBuilder.append(DataConnection.this);
                    dataConnection.log(stringBuilder.toString());
                    DataConnection.this.notifyDisconnectCompleted(dp, false);
                }
                retVal = true;
            } else if (i == DataConnection.EVENT_DISCONNECT_ALL) {
                dataConnection2 = DataConnection.this;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DcActiveState EVENT_DISCONNECT clearing apn contexts, dc=");
                stringBuilder2.append(DataConnection.this);
                dataConnection2.log(stringBuilder2.toString());
                dp = message.obj;
                DataConnection.this.mDisconnectParams = dp;
                DataConnection.this.mConnectionParams = null;
                dp.mTag = DataConnection.this.mTag;
                DataConnection.this.tearDownData(dp);
                DataConnection.this.transitionTo(DataConnection.this.mDisconnectingState);
                retVal = true;
            } else if (i != DataConnection.EVENT_LOST_CONNECTION) {
                boolean retVal2;
                AsyncResult ar;
                LinkCapacityEstimate lce;
                NetworkCapabilities nc;
                int slot;
                DataConnection dataConnection3;
                StringBuilder stringBuilder3;
                int slotId;
                StringBuilder stringBuilder4;
                switch (i) {
                    case DataConnection.EVENT_DATA_CONNECTION_ROAM_ON /*262156*/:
                    case DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF /*262157*/:
                    case DataConnection.EVENT_DATA_CONNECTION_OVERRIDE_CHANGED /*262161*/:
                        DataConnection.this.updateNetworkInfo();
                        if (DataConnection.this.mNetworkAgent != null) {
                            DataConnection.this.mNetworkAgent.sendNetworkCapabilities(DataConnection.this.getNetworkCapabilities());
                            DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                        }
                        retVal = true;
                        break;
                    case DataConnection.EVENT_BW_REFRESH_RESPONSE /*262158*/:
                        ar = (AsyncResult) message.obj;
                        if (ar.exception != null) {
                            dataConnection = DataConnection.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("EVENT_BW_REFRESH_RESPONSE: error ignoring, e=");
                            stringBuilder.append(ar.exception);
                            dataConnection.log(stringBuilder.toString());
                        } else {
                            lce = ar.result;
                            nc = DataConnection.this.getNetworkCapabilities();
                            if (DataConnection.this.mPhone.getLceStatus() == 1) {
                                nc.setLinkDownstreamBandwidthKbps(lce.downlinkCapacityKbps);
                                if (DataConnection.this.mNetworkAgent != null) {
                                    DataConnection.this.mNetworkAgent.sendNetworkCapabilities(nc);
                                }
                            }
                        }
                        retVal = true;
                        break;
                    case DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_STARTED /*262159*/:
                    case DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_ENDED /*262160*/:
                        DataConnection.this.updateNetworkInfo();
                        DataConnection.this.updateNetworkInfoSuspendState();
                        if (DataConnection.this.mNetworkAgent != null) {
                            DataConnection.this.mNetworkAgent.sendNetworkCapabilities(DataConnection.this.getNetworkCapabilities());
                            DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                        }
                        retVal = true;
                        break;
                    case DataConnection.EVENT_KEEPALIVE_STATUS /*262162*/:
                        ar = (AsyncResult) message.obj;
                        if (ar.exception != null) {
                            dataConnection = DataConnection.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("EVENT_KEEPALIVE_STATUS: error in keepalive, e=");
                            stringBuilder.append(ar.exception);
                            dataConnection.loge(stringBuilder.toString());
                        }
                        if (ar.result != null) {
                            DataConnection.this.mNetworkAgent.keepaliveTracker.handleKeepaliveStatus(ar.result);
                        }
                        retVal = true;
                        break;
                    case DataConnection.EVENT_KEEPALIVE_STARTED /*262163*/:
                        ar = (AsyncResult) message.obj;
                        slot = message.arg1;
                        if (ar.exception != null || ar.result == null) {
                            dataConnection3 = DataConnection.this;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("EVENT_KEEPALIVE_STARTED: error starting keepalive, e=");
                            stringBuilder3.append(ar.exception);
                            dataConnection3.loge(stringBuilder3.toString());
                            DataConnection.this.mNetworkAgent.onPacketKeepaliveEvent(slot, -31);
                        } else {
                            KeepaliveStatus ks = ar.result;
                            if (ks == null) {
                                DataConnection.this.loge("Null KeepaliveStatus received!");
                            } else {
                                DataConnection.this.mNetworkAgent.keepaliveTracker.handleKeepaliveStarted(slot, ks);
                            }
                        }
                        retVal = true;
                        break;
                    case DataConnection.EVENT_KEEPALIVE_STOPPED /*262164*/:
                        ar = message.obj;
                        slot = message.arg1;
                        slotId = message.arg2;
                        DataConnection dataConnection4;
                        if (ar.exception != null) {
                            dataConnection4 = DataConnection.this;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("EVENT_KEEPALIVE_STOPPED: error stopping keepalive for handle=");
                            stringBuilder4.append(slot);
                            stringBuilder4.append(" e=");
                            stringBuilder4.append(ar.exception);
                            dataConnection4.loge(stringBuilder4.toString());
                            DataConnection.this.mNetworkAgent.keepaliveTracker.handleKeepaliveStatus(new KeepaliveStatus(3));
                        } else {
                            dataConnection4 = DataConnection.this;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Keepalive Stop Requested for handle=");
                            stringBuilder5.append(slot);
                            dataConnection4.log(stringBuilder5.toString());
                            DataConnection.this.mNetworkAgent.keepaliveTracker.handleKeepaliveStatus(new KeepaliveStatus(slot, 1));
                        }
                        retVal2 = true;
                        break;
                    case DataConnection.EVENT_KEEPALIVE_START_REQUEST /*262165*/:
                        KeepalivePacketData pkt = message.obj;
                        slot = message.arg1;
                        slotId = message.arg2 * 1000;
                        if (DataConnection.this.mDataServiceManager.getTransportType() == 1) {
                            DataConnection.this.mPhone.mCi.startNattKeepalive(DataConnection.this.mCid, pkt, slotId, DataConnection.this.obtainMessage(DataConnection.EVENT_KEEPALIVE_STARTED, slot, 0, null));
                        } else if (DataConnection.this.mNetworkAgent != null) {
                            DataConnection.this.mNetworkAgent.onPacketKeepaliveEvent(message.arg1, -20);
                        }
                        retVal2 = true;
                        break;
                    case DataConnection.EVENT_KEEPALIVE_STOP_REQUEST /*262166*/:
                        i = message.arg1;
                        slot = DataConnection.this.mNetworkAgent.keepaliveTracker.getHandleForSlot(i);
                        if (slot >= 0) {
                            dataConnection3 = DataConnection.this;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Stopping keepalive with handle: ");
                            stringBuilder4.append(slot);
                            dataConnection3.logd(stringBuilder4.toString());
                            DataConnection.this.mPhone.mCi.stopNattKeepalive(slot, DataConnection.this.obtainMessage(DataConnection.EVENT_KEEPALIVE_STOPPED, slot, i, null));
                            retVal = true;
                            break;
                        }
                        dataConnection3 = DataConnection.this;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("No slot found for stopPacketKeepalive! ");
                        stringBuilder3.append(i);
                        dataConnection3.loge(stringBuilder3.toString());
                        retVal = true;
                        break;
                    case DataConnection.EVENT_LINK_CAPACITY_CHANGED /*262167*/:
                        ar = message.obj;
                        if (ar.exception != null) {
                            dataConnection = DataConnection.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("EVENT_LINK_CAPACITY_CHANGED e=");
                            stringBuilder.append(ar.exception);
                            dataConnection.loge(stringBuilder.toString());
                        } else {
                            lce = ar.result;
                            nc = DataConnection.this.getNetworkCapabilities();
                            if (lce.downlinkCapacityKbps != -1) {
                                nc.setLinkDownstreamBandwidthKbps(lce.downlinkCapacityKbps);
                            }
                            if (lce.uplinkCapacityKbps != -1) {
                                nc.setLinkUpstreamBandwidthKbps(lce.uplinkCapacityKbps);
                            }
                            if (DataConnection.this.mNetworkAgent != null) {
                                DataConnection.this.mNetworkAgent.sendNetworkCapabilities(nc);
                            }
                        }
                        retVal = true;
                        break;
                    case DataConnection.EVENT_CLEAR_LINK /*262168*/:
                        DataConnection.this.log("DcActiveState EVENT_CLEAR_LINK");
                        DataConnection.this.mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, DataConnection.this.mNetworkInfo.getReason(), DataConnection.this.mNetworkInfo.getExtraInfo());
                        if (DataConnection.this.mNetworkAgent != null) {
                            DataConnection.this.log("DcActiveState EVENT_CLEAR_LINK sendNetworkInfo");
                            DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                        }
                        DataConnection.this.keepNetwork = true;
                        DataConnection.this.mLastLinkProperties = DataConnection.this.mLinkProperties;
                        retVal = true;
                        break;
                    case DataConnection.EVENT_RESUME_LINK /*262169*/:
                        DataConnection.this.log("DcActiveState EVENT_RESUME_LINK");
                        DataConnection.this.mNetworkInfo.setDetailedState(DetailedState.CONNECTED, DataConnection.this.mNetworkInfo.getReason(), DataConnection.this.mNetworkInfo.getExtraInfo());
                        boolean shouldResumeLink = (DataConnection.this.mNetworkAgent == null || DataConnection.this.mLastLinkProperties == null) ? false : true;
                        if (shouldResumeLink) {
                            DataConnection.this.log("DcActiveState EVENT_RESUME_LINK sendNetworkInfo");
                            NetworkMisc misc = new NetworkMisc();
                            CarrierSignalAgent carrierSignalAgent = DataConnection.this.mPhone.getCarrierSignalAgent();
                            if (carrierSignalAgent.hasRegisteredReceivers("com.android.internal.telephony.CARRIER_SIGNAL_REDIRECTED")) {
                                misc.provisioningNotificationDisabled = true;
                            }
                            misc.subscriberId = DataConnection.this.mPhone.getSubscriberId();
                            DataConnection dataConnection5 = DataConnection.this;
                            DcNetworkAgent dcNetworkAgent = r8;
                            DcNetworkAgent dcNetworkAgent2 = new DcNetworkAgent(DataConnection.this, DataConnection.this.getHandler().getLooper(), DataConnection.this.mPhone.getContext(), "DcNetworkAgent", DataConnection.this.mNetworkInfo, DataConnection.this.getNetworkCapabilities(), DataConnection.this.mLinkProperties, 50, misc);
                            dataConnection5.mNetworkAgent = dcNetworkAgent;
                            DataConnection.this.mLastLinkProperties = null;
                            DataConnection.this.doChrCheckForResumeLink(DataConnection.this.mLastLinkProperties, DataConnection.this.mLinkProperties);
                        }
                        DataConnection.this.keepNetwork = false;
                        retVal = true;
                        break;
                    default:
                        dataConnection2 = DataConnection.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("DcActiveState not handled msg.what=");
                        stringBuilder2.append(DataConnection.this.getWhatToString(message.what));
                        dataConnection2.log(stringBuilder2.toString());
                        retVal = false;
                        break;
                }
                retVal = retVal2;
            } else {
                dataConnection2 = DataConnection.this;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DcActiveState EVENT_LOST_CONNECTION dc=");
                stringBuilder2.append(DataConnection.this);
                dataConnection2.log(stringBuilder2.toString());
                DataConnection.this.mInactiveState.setEnterNotificationParams(DcFailCause.LOST_CONNECTION);
                DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                retVal = true;
            }
            return retVal;
        }
    }

    private class DcDefaultState extends State {
        private DcDefaultState() {
        }

        /* synthetic */ DcDefaultState(DataConnection x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            DataConnection.this.log("DcDefaultState: enter");
            DataConnection.this.mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED, null);
            DataConnection.this.mPhone.getServiceStateTracker().registerForDataRoamingOn(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_ROAM_ON, null);
            DataConnection.this.mPhone.getServiceStateTracker().registerForDataRoamingOff(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF, null, true);
            DataConnection.this.mDcController.addDc(DataConnection.this);
        }

        public void exit() {
            DataConnection.this.log("DcDefaultState: exit");
            DataConnection.this.mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(DataConnection.this.getHandler());
            DataConnection.this.mPhone.getServiceStateTracker().unregisterForDataRoamingOn(DataConnection.this.getHandler());
            DataConnection.this.mPhone.getServiceStateTracker().unregisterForDataRoamingOff(DataConnection.this.getHandler());
            DataConnection.this.mDcController.removeDc(DataConnection.this);
            if (DataConnection.this.mAc != null) {
                DataConnection.this.mAc.disconnected();
                DataConnection.this.mAc = null;
            }
            DataConnection.this.mApnContexts = null;
            DataConnection.this.mReconnectIntent = null;
            DataConnection.this.mDct = null;
            DataConnection.this.mApnSetting = null;
            DataConnection.this.mPhone = null;
            DataConnection.this.mDataServiceManager = null;
            DataConnection.this.mLinkProperties = null;
            DataConnection.this.mLastFailCause = null;
            DataConnection.this.mUserData = null;
            DataConnection.this.mDcController = null;
            DataConnection.this.mDcTesterFailBringUpAll = null;
        }

        public boolean processMessage(Message msg) {
            DataConnection dataConnection = DataConnection.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DcDefault msg=");
            stringBuilder.append(DataConnection.this.getWhatToString(msg.what));
            stringBuilder.append(" RefCount=");
            stringBuilder.append(DataConnection.this.mApnContexts.size());
            dataConnection.log(stringBuilder.toString());
            boolean val;
            DataConnection dataConnection2;
            StringBuilder stringBuilder2;
            switch (msg.what) {
                case 69633:
                    if (DataConnection.this.mAc == null) {
                        DataConnection.this.mAc = new AsyncChannel();
                        DataConnection.this.mAc.connected(null, DataConnection.this.getHandler(), msg.replyTo);
                        DataConnection.this.log("DcDefaultState: FULL_CONNECTION reply connected");
                        DataConnection.this.mAc.replyToMessage(msg, 69634, 0, DataConnection.this.mId, "hi");
                        break;
                    }
                    dataConnection = DataConnection.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Disconnecting to previous connection mAc=");
                    stringBuilder.append(DataConnection.this.mAc);
                    dataConnection.log(stringBuilder.toString());
                    DataConnection.this.mAc.replyToMessage(msg, 69634, 3);
                    break;
                case 69636:
                    DataConnection.this.log("DcDefault: CMD_CHANNEL_DISCONNECTED before quiting call dump");
                    DataConnection.this.dumpToLog();
                    DataConnection.this.quit();
                    break;
                case InboundSmsTracker.DEST_PORT_FLAG_3GPP2 /*262144*/:
                    DataConnection.this.log("DcDefaultState: msg.what=EVENT_CONNECT, fail not expected");
                    DataConnection.this.notifyConnectCompleted(msg.obj, DcFailCause.UNKNOWN, false);
                    break;
                case DataConnection.EVENT_DISCONNECT /*262148*/:
                    dataConnection = DataConnection.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DcDefaultState deferring msg.what=EVENT_DISCONNECT RefCount=");
                    stringBuilder.append(DataConnection.this.mApnContexts.size());
                    dataConnection.log(stringBuilder.toString());
                    DataConnection.this.deferMessage(msg);
                    break;
                case DataConnection.EVENT_DISCONNECT_ALL /*262150*/:
                    dataConnection = DataConnection.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DcDefaultState deferring msg.what=EVENT_DISCONNECT_ALL RefCount=");
                    stringBuilder.append(DataConnection.this.mApnContexts.size());
                    dataConnection.log(stringBuilder.toString());
                    DataConnection.this.deferMessage(msg);
                    break;
                case DataConnection.EVENT_TEAR_DOWN_NOW /*262152*/:
                    DataConnection.this.log("DcDefaultState EVENT_TEAR_DOWN_NOW");
                    DataConnection.this.mDataServiceManager.deactivateDataCall(DataConnection.this.mCid, 1, null);
                    break;
                case DataConnection.EVENT_LOST_CONNECTION /*262153*/:
                    String s = new StringBuilder();
                    s.append("DcDefaultState ignore EVENT_LOST_CONNECTION tag=");
                    s.append(msg.arg1);
                    s.append(":mTag=");
                    s.append(DataConnection.this.mTag);
                    DataConnection.this.logAndAddLogRec(s.toString());
                    break;
                case DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED /*262155*/:
                    Pair<Integer, Integer> drsRatPair = msg.obj.result;
                    DataConnection.this.mDataRegState = ((Integer) drsRatPair.first).intValue();
                    if (DataConnection.this.mRilRat != ((Integer) drsRatPair.second).intValue()) {
                        DataConnection.this.updateTcpBufferSizes(((Integer) drsRatPair.second).intValue());
                    }
                    DataConnection.this.mRilRat = ((Integer) drsRatPair.second).intValue();
                    DataConnection dataConnection3 = DataConnection.this;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("DcDefaultState: EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED drs=");
                    stringBuilder3.append(DataConnection.this.mDataRegState);
                    stringBuilder3.append(" mRilRat=");
                    stringBuilder3.append(DataConnection.this.mRilRat);
                    dataConnection3.log(stringBuilder3.toString());
                    DataConnection.this.updateNetworkInfo();
                    DataConnection.this.updateNetworkInfoSuspendState();
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendNetworkCapabilities(DataConnection.this.getNetworkCapabilities());
                        DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                        DataConnection.this.mNetworkAgent.sendLinkProperties(DataConnection.this.mLinkProperties);
                        break;
                    }
                    break;
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_ON /*262156*/:
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF /*262157*/:
                case DataConnection.EVENT_DATA_CONNECTION_OVERRIDE_CHANGED /*262161*/:
                    DataConnection.this.updateNetworkInfo();
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendNetworkCapabilities(DataConnection.this.getNetworkCapabilities());
                        DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                        break;
                    }
                    break;
                case DataConnection.EVENT_KEEPALIVE_START_REQUEST /*262165*/:
                case DataConnection.EVENT_KEEPALIVE_STOP_REQUEST /*262166*/:
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.onPacketKeepaliveEvent(msg.arg1, -20);
                        break;
                    }
                    break;
                case 266240:
                    val = DataConnection.this.isInactive();
                    dataConnection2 = DataConnection.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REQ_IS_INACTIVE  isInactive=");
                    stringBuilder2.append(val);
                    dataConnection2.log(stringBuilder2.toString());
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_IS_INACTIVE, val);
                    break;
                case DcAsyncChannel.REQ_GET_CID /*266242*/:
                    int cid = DataConnection.this.getCid();
                    dataConnection2 = DataConnection.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REQ_GET_CID  cid=");
                    stringBuilder2.append(cid);
                    dataConnection2.log(stringBuilder2.toString());
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_GET_CID, cid);
                    break;
                case DcAsyncChannel.REQ_GET_APNSETTING /*266244*/:
                    ApnSetting apnSetting = DataConnection.this.getApnSetting();
                    dataConnection2 = DataConnection.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REQ_GET_APNSETTING  mApnSetting=");
                    stringBuilder2.append(apnSetting);
                    dataConnection2.log(stringBuilder2.toString());
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_GET_APNSETTING, apnSetting);
                    break;
                case DcAsyncChannel.REQ_GET_LINK_PROPERTIES /*266246*/:
                    LinkProperties lp = DataConnection.this.getCopyLinkProperties();
                    DataConnection.this.log("REQ_GET_LINK_PROPERTIES linkProperties");
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_GET_LINK_PROPERTIES, lp);
                    break;
                case DcAsyncChannel.REQ_SET_LINK_PROPERTIES_HTTP_PROXY /*266248*/:
                    ProxyInfo proxy = msg.obj;
                    dataConnection2 = DataConnection.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REQ_SET_LINK_PROPERTIES_HTTP_PROXY proxy=");
                    stringBuilder2.append(proxy);
                    dataConnection2.log(stringBuilder2.toString());
                    DataConnection.this.setLinkPropertiesHttpProxy(proxy);
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_SET_LINK_PROPERTIES_HTTP_PROXY);
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendLinkProperties(DataConnection.this.mLinkProperties);
                        break;
                    }
                    break;
                case DcAsyncChannel.REQ_GET_NETWORK_CAPABILITIES /*266250*/:
                    NetworkCapabilities nc = DataConnection.this.getNetworkCapabilities();
                    dataConnection2 = DataConnection.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REQ_GET_NETWORK_CAPABILITIES networkCapabilities");
                    stringBuilder2.append(nc);
                    dataConnection2.log(stringBuilder2.toString());
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_GET_NETWORK_CAPABILITIES, nc);
                    break;
                case DcAsyncChannel.REQ_RESET /*266252*/:
                    DataConnection.this.log("DcDefaultState: msg.what=REQ_RESET");
                    DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                    break;
                case DcAsyncChannel.REQ_CHECK_APNCONTEXT /*266254*/:
                    val = DataConnection.this.checkApnContext((ApnContext) msg.obj);
                    dataConnection2 = DataConnection.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REQ_CHECK_APNCONTEXT  checkApnContext=");
                    stringBuilder2.append(val);
                    dataConnection2.log(stringBuilder2.toString());
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_CHECK_APNCONTEXT, val);
                    break;
                default:
                    dataConnection = DataConnection.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DcDefaultState: shouldn't happen but ignore msg.what=");
                    stringBuilder.append(DataConnection.this.getWhatToString(msg.what));
                    dataConnection.log(stringBuilder.toString());
                    break;
            }
            return true;
        }
    }

    private class DcDisconnectingState extends State {
        private DcDisconnectingState() {
        }

        /* synthetic */ DcDisconnectingState(DataConnection x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            boolean canHandleType;
            int phoneId = DataConnection.this.mPhone.getPhoneId();
            int access$1100 = DataConnection.this.mId;
            long j = DataConnection.this.mApnSetting != null ? (long) DataConnection.this.mApnSetting.typesBitmap : 0;
            if (DataConnection.this.mApnSetting != null) {
                canHandleType = DataConnection.this.mApnSetting.canHandleType("default");
            } else {
                canHandleType = false;
            }
            StatsLog.write(75, 4, phoneId, access$1100, j, canHandleType);
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            DataConnection dataConnection;
            StringBuilder stringBuilder;
            if (i == InboundSmsTracker.DEST_PORT_FLAG_3GPP2) {
                dataConnection = DataConnection.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DcDisconnectingState msg.what=EVENT_CONNECT. Defer. RefCount = ");
                stringBuilder.append(DataConnection.this.mApnContexts.size());
                dataConnection.log(stringBuilder.toString());
                DataConnection.this.deferMessage(msg);
                return true;
            } else if (i != DataConnection.EVENT_DEACTIVATE_DONE) {
                dataConnection = DataConnection.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DcDisconnectingState not handled msg.what=");
                stringBuilder.append(DataConnection.this.getWhatToString(msg.what));
                dataConnection.log(stringBuilder.toString());
                return false;
            } else {
                DisconnectParams dp = msg.obj;
                String str = new StringBuilder();
                str.append("DcDisconnectingState msg.what=EVENT_DEACTIVATE_DONE RefCount=");
                str.append(DataConnection.this.mApnContexts.size());
                str = str.toString();
                DataConnection.this.log(str);
                if (dp.mApnContext != null) {
                    dp.mApnContext.requestLog(str);
                }
                if (DataConnection.this.mHwCustDataConnection != null) {
                    DataConnection.this.mHwCustDataConnection.clearInternetPcoValue(DataConnection.this.mApnSetting.profileId, DataConnection.this.mPhone);
                }
                if (dp.mTag == DataConnection.this.mTag) {
                    DataConnection.this.mInactiveState.setEnterNotificationParams(dp);
                    DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                } else {
                    DataConnection dataConnection2 = DataConnection.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("DcDisconnectState stale EVENT_DEACTIVATE_DONE dp.tag=");
                    stringBuilder2.append(dp.mTag);
                    stringBuilder2.append(" mTag=");
                    stringBuilder2.append(DataConnection.this.mTag);
                    dataConnection2.log(stringBuilder2.toString());
                }
                return true;
            }
        }
    }

    private class DcDisconnectionErrorCreatingConnection extends State {
        private DcDisconnectionErrorCreatingConnection() {
        }

        /* synthetic */ DcDisconnectionErrorCreatingConnection(DataConnection x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            boolean canHandleType;
            int phoneId = DataConnection.this.mPhone.getPhoneId();
            int access$1100 = DataConnection.this.mId;
            long j = DataConnection.this.mApnSetting != null ? (long) DataConnection.this.mApnSetting.typesBitmap : 0;
            if (DataConnection.this.mApnSetting != null) {
                canHandleType = DataConnection.this.mApnSetting.canHandleType("default");
            } else {
                canHandleType = false;
            }
            StatsLog.write(75, 5, phoneId, access$1100, j, canHandleType);
        }

        public boolean processMessage(Message msg) {
            if (msg.what != DataConnection.EVENT_DEACTIVATE_DONE) {
                DataConnection dataConnection = DataConnection.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DcDisconnectionErrorCreatingConnection not handled msg.what=");
                stringBuilder.append(DataConnection.this.getWhatToString(msg.what));
                dataConnection.log(stringBuilder.toString());
                return false;
            }
            ConnectionParams cp = msg.obj;
            if (cp.mTag == DataConnection.this.mTag) {
                String str = "DcDisconnectionErrorCreatingConnection msg.what=EVENT_DEACTIVATE_DONE";
                DataConnection.this.log(str);
                if (cp.mApnContext != null) {
                    cp.mApnContext.requestLog(str);
                }
                DataConnection.this.mInactiveState.setEnterNotificationParams(cp, DcFailCause.UNACCEPTABLE_NETWORK_PARAMETER);
                DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
            } else {
                DataConnection dataConnection2 = DataConnection.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DcDisconnectionErrorCreatingConnection stale EVENT_DEACTIVATE_DONE dp.tag=");
                stringBuilder2.append(cp.mTag);
                stringBuilder2.append(", mTag=");
                stringBuilder2.append(DataConnection.this.mTag);
                dataConnection2.log(stringBuilder2.toString());
            }
            return true;
        }
    }

    private class DcInactiveState extends State {
        private DcInactiveState() {
        }

        /* synthetic */ DcInactiveState(DataConnection x0, AnonymousClass1 x1) {
            this();
        }

        public void setEnterNotificationParams(ConnectionParams cp, DcFailCause cause) {
            DataConnection.this.log("DcInactiveState: setEnterNotificationParams cp,cause");
            DataConnection.this.mConnectionParams = cp;
            DataConnection.this.mDisconnectParams = null;
            DataConnection.this.mDcFailCause = cause;
        }

        public void setEnterNotificationParams(DisconnectParams dp) {
            DataConnection.this.log("DcInactiveState: setEnterNotificationParams dp");
            DataConnection.this.mConnectionParams = null;
            DataConnection.this.mDisconnectParams = dp;
            DataConnection.this.mDcFailCause = DcFailCause.NONE;
        }

        public void setEnterNotificationParams(DcFailCause cause) {
            DataConnection.this.mConnectionParams = null;
            DataConnection.this.mDisconnectParams = null;
            DataConnection.this.mDcFailCause = cause;
        }

        public void enter() {
            boolean canHandleType;
            DataConnection dataConnection = DataConnection.this;
            dataConnection.mTag++;
            dataConnection = DataConnection.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DcInactiveState: enter() mTag=");
            stringBuilder.append(DataConnection.this.mTag);
            dataConnection.log(stringBuilder.toString());
            int phoneId = DataConnection.this.mPhone.getPhoneId();
            int access$1100 = DataConnection.this.mId;
            long j = DataConnection.this.mApnSetting != null ? (long) DataConnection.this.mApnSetting.typesBitmap : 0;
            if (DataConnection.this.mApnSetting != null) {
                canHandleType = DataConnection.this.mApnSetting.canHandleType("default");
            } else {
                canHandleType = false;
            }
            StatsLog.write(75, 1, phoneId, access$1100, j, canHandleType);
            if (DataConnection.this.mConnectionParams != null) {
                dataConnection = DataConnection.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DcInactiveState: enter notifyConnectCompleted +ALL failCause=");
                stringBuilder.append(DataConnection.this.mDcFailCause);
                dataConnection.log(stringBuilder.toString());
                if (DataConnection.this.mDcFailCause != DcFailCause.NONE) {
                    DataConnection.this.misLastFailed = true;
                }
                DataConnection.this.notifyConnectCompleted(DataConnection.this.mConnectionParams, DataConnection.this.mDcFailCause, true);
            }
            if (DataConnection.this.mDisconnectParams != null) {
                dataConnection = DataConnection.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DcInactiveState: enter notifyDisconnectCompleted +ALL failCause=");
                stringBuilder.append(DataConnection.this.mDcFailCause);
                dataConnection.log(stringBuilder.toString());
                DataConnection.this.notifyDisconnectCompleted(DataConnection.this.mDisconnectParams, true);
            }
            if (DataConnection.this.mDisconnectParams == null && DataConnection.this.mConnectionParams == null && DataConnection.this.mDcFailCause != null) {
                dataConnection = DataConnection.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DcInactiveState: enter notifyAllDisconnectCompleted failCause=");
                stringBuilder.append(DataConnection.this.mDcFailCause);
                dataConnection.log(stringBuilder.toString());
                DataConnection.this.notifyAllDisconnectCompleted(DataConnection.this.mDcFailCause);
            }
            DataConnection.this.mDcController.removeActiveDcByCid(DataConnection.this);
            DataConnection.this.clearSettings();
        }

        public void exit() {
            DataConnection.this.misLastFailed = false;
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            boolean retVal = false;
            if (i == InboundSmsTracker.DEST_PORT_FLAG_3GPP2) {
                DataConnection.this.log("DcInactiveState: mag.what=EVENT_CONNECT");
                ConnectionParams cp = msg.obj;
                if (true == DataConnection.this.misLastFailed && true == cp.mdefered) {
                    DataConnection.this.log("DcInactiveState: msg.what=EVENT_CONNECT apnContext with defefed msg, not process ");
                    DataConnection.this.notifyConnectCompleted(cp, DcFailCause.UNKNOWN, false);
                } else if (DataConnection.this.initConnection(cp)) {
                    DataConnection.this.onConnect(DataConnection.this.mConnectionParams);
                    DataConnection.this.transitionTo(DataConnection.this.mActivatingState);
                } else {
                    DataConnection.this.log("DcInactiveState: msg.what=EVENT_CONNECT initConnection failed");
                    DataConnection.this.notifyConnectCompleted(cp, DcFailCause.UNACCEPTABLE_NETWORK_PARAMETER, false);
                }
                retVal = true;
            } else if (i == DataConnection.EVENT_DISCONNECT) {
                DataConnection.this.log("DcInactiveState: msg.what=EVENT_DISCONNECT");
                DataConnection.this.notifyDisconnectCompleted((DisconnectParams) msg.obj, false);
                retVal = true;
            } else if (i == DataConnection.EVENT_DISCONNECT_ALL) {
                DataConnection.this.log("DcInactiveState: msg.what=EVENT_DISCONNECT_ALL");
                DataConnection.this.notifyDisconnectCompleted((DisconnectParams) msg.obj, false);
                retVal = true;
            } else if (i != DcAsyncChannel.REQ_RESET) {
                DataConnection dataConnection = DataConnection.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DcInactiveState nothandled msg.what=");
                stringBuilder.append(DataConnection.this.getWhatToString(msg.what));
                dataConnection.log(stringBuilder.toString());
            } else {
                DataConnection.this.log("DcInactiveState: msg.what=RSP_RESET, ignore we're already reset");
                retVal = true;
            }
            return retVal;
        }
    }

    private class DcNetworkAgent extends NetworkAgent {
        public final DcKeepaliveTracker keepaliveTracker = new DcKeepaliveTracker(this, null);
        private NetworkCapabilities mNetworkCapabilities;
        final /* synthetic */ DataConnection this$0;

        private class DcKeepaliveTracker {
            private final SparseArray<KeepaliveRecord> mKeepalives;

            private class KeepaliveRecord {
                public int currentStatus;
                public int slotId;

                KeepaliveRecord(int slotId, int status) {
                    this.slotId = slotId;
                    this.currentStatus = status;
                }
            }

            private DcKeepaliveTracker() {
                this.mKeepalives = new SparseArray();
            }

            /* synthetic */ DcKeepaliveTracker(DcNetworkAgent x0, AnonymousClass1 x1) {
                this();
            }

            int getHandleForSlot(int slotId) {
                for (int i = 0; i < this.mKeepalives.size(); i++) {
                    if (((KeepaliveRecord) this.mKeepalives.valueAt(i)).slotId == slotId) {
                        return this.mKeepalives.keyAt(i);
                    }
                }
                return -1;
            }

            int keepaliveStatusErrorToPacketKeepaliveError(int error) {
                switch (error) {
                    case 0:
                        return 0;
                    case 1:
                        return -30;
                    default:
                        return -31;
                }
            }

            void handleKeepaliveStarted(int slot, KeepaliveStatus ks) {
                StringBuilder stringBuilder;
                switch (ks.statusCode) {
                    case 0:
                        DcNetworkAgent.this.onPacketKeepaliveEvent(slot, 0);
                        break;
                    case 1:
                        DcNetworkAgent.this.onPacketKeepaliveEvent(slot, keepaliveStatusErrorToPacketKeepaliveError(ks.errorCode));
                        return;
                    case 2:
                        break;
                    default:
                        DataConnection dataConnection = DcNetworkAgent.this.this$0;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid KeepaliveStatus Code: ");
                        stringBuilder.append(ks.statusCode);
                        dataConnection.loge(stringBuilder.toString());
                        return;
                }
                DcNetworkAgent dcNetworkAgent = DcNetworkAgent.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Adding keepalive handle=");
                stringBuilder.append(ks.sessionHandle);
                stringBuilder.append(" slot = ");
                stringBuilder.append(slot);
                dcNetworkAgent.log(stringBuilder.toString());
                this.mKeepalives.put(ks.sessionHandle, new KeepaliveRecord(slot, ks.statusCode));
            }

            void handleKeepaliveStatus(KeepaliveStatus ks) {
                KeepaliveRecord kr = (KeepaliveRecord) this.mKeepalives.get(ks.sessionHandle);
                StringBuilder stringBuilder;
                if (kr == null) {
                    DcNetworkAgent dcNetworkAgent = DcNetworkAgent.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Discarding keepalive event for different data connection:");
                    stringBuilder.append(ks);
                    dcNetworkAgent.log(stringBuilder.toString());
                    return;
                }
                DataConnection dataConnection;
                switch (kr.currentStatus) {
                    case 0:
                        switch (ks.statusCode) {
                            case 0:
                            case 2:
                                DcNetworkAgent.this.this$0.loge("Active Keepalive received invalid status!");
                                break;
                            case 1:
                                DcNetworkAgent.this.this$0.loge("Keepalive received stopped status!");
                                DcNetworkAgent.this.onPacketKeepaliveEvent(kr.slotId, 0);
                                kr.currentStatus = 1;
                                this.mKeepalives.remove(ks.sessionHandle);
                                break;
                            default:
                                dataConnection = DcNetworkAgent.this.this$0;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Invalid Keepalive Status received, ");
                                stringBuilder.append(ks.statusCode);
                                dataConnection.loge(stringBuilder.toString());
                                break;
                        }
                    case 1:
                        DcNetworkAgent.this.this$0.loge("Inactive Keepalive received status!");
                        DcNetworkAgent.this.onPacketKeepaliveEvent(kr.slotId, -31);
                        break;
                    case 2:
                        switch (ks.statusCode) {
                            case 0:
                                DcNetworkAgent.this.log("Pending Keepalive received active status!");
                                kr.currentStatus = 0;
                                DcNetworkAgent.this.onPacketKeepaliveEvent(kr.slotId, 0);
                                break;
                            case 1:
                                DcNetworkAgent.this.onPacketKeepaliveEvent(kr.slotId, keepaliveStatusErrorToPacketKeepaliveError(ks.errorCode));
                                kr.currentStatus = 1;
                                this.mKeepalives.remove(ks.sessionHandle);
                                break;
                            case 2:
                                DcNetworkAgent.this.this$0.loge("Invalid unsolicied Keepalive Pending Status!");
                                break;
                            default:
                                dataConnection = DcNetworkAgent.this.this$0;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Invalid Keepalive Status received, ");
                                stringBuilder.append(ks.statusCode);
                                dataConnection.loge(stringBuilder.toString());
                                break;
                        }
                    default:
                        dataConnection = DcNetworkAgent.this.this$0;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid Keepalive Status received, ");
                        stringBuilder.append(kr.currentStatus);
                        dataConnection.loge(stringBuilder.toString());
                        break;
                }
            }
        }

        public DcNetworkAgent(DataConnection dataConnection, Looper l, Context c, String TAG, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score, NetworkMisc misc) {
            NetworkCapabilities networkCapabilities = nc;
            DataConnection dataConnection2 = dataConnection;
            this.this$0 = dataConnection2;
            super(l, c, TAG, ni, networkCapabilities, lp, score, misc);
            LocalLog access$5600 = dataConnection2.mNetCapsLocalLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("New network agent created. capabilities=");
            stringBuilder.append(networkCapabilities);
            access$5600.log(stringBuilder.toString());
            this.mNetworkCapabilities = networkCapabilities;
        }

        protected void unwanted() {
            if (this.this$0.mNetworkAgent != this) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DcNetworkAgent: unwanted found mNetworkAgent=");
                stringBuilder.append(this.this$0.mNetworkAgent);
                stringBuilder.append(", which isn't me.  Aborting unwanted");
                log(stringBuilder.toString());
            } else if (this.this$0.mApnContexts != null) {
                if (this.this$0.keepNetwork) {
                    this.this$0.logi("DcNetworkAgent unwanted keepNetwork");
                    return;
                }
                for (ConnectionParams cp : this.this$0.mApnContexts.values()) {
                    ApnContext apnContext = cp.mApnContext;
                    Pair<ApnContext, Integer> pair = new Pair(apnContext, Integer.valueOf(cp.mConnectionGeneration));
                    if (!this.this$0.mDct.isDataNeededWithWifiAndBt()) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("DcNetworkAgent: [unwanted]: disconnect apnContext=");
                        stringBuilder2.append(apnContext);
                        stringBuilder2.append(". And no retry it after disconnected");
                        log(stringBuilder2.toString());
                        apnContext.setReason(AbstractPhoneInternalInterface.REASON_NO_RETRY_AFTER_DISCONNECT);
                    }
                    this.this$0.sendMessage(this.this$0.obtainMessage(DataConnection.EVENT_DISCONNECT, new DisconnectParams(apnContext, apnContext.getReason(), this.this$0.mDct.obtainMessage(270351, pair))));
                }
            }
        }

        protected void pollLceData() {
            if (this.this$0.mPhone.getLceStatus() == 1) {
                this.this$0.mPhone.mCi.pullLceData(this.this$0.obtainMessage(DataConnection.EVENT_BW_REFRESH_RESPONSE));
            }
        }

        protected void networkStatus(int status, String redirectUrl) {
            if (!TextUtils.isEmpty(redirectUrl)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("validation status: ");
                stringBuilder.append(status);
                stringBuilder.append(" with redirection URL: ");
                stringBuilder.append(redirectUrl);
                log(stringBuilder.toString());
                this.this$0.mDct.obtainMessage(270380, redirectUrl).sendToTarget();
            }
        }

        public void sendNetworkCapabilities(NetworkCapabilities networkCapabilities) {
            if (!networkCapabilities.equals(this.mNetworkCapabilities)) {
                String logStr = new StringBuilder();
                logStr.append("Changed from ");
                logStr.append(this.mNetworkCapabilities);
                logStr.append(" to ");
                logStr.append(networkCapabilities);
                logStr.append(", Data RAT=");
                logStr.append(this.this$0.mPhone.getServiceState().getRilDataRadioTechnology());
                logStr.append(", mApnSetting=");
                logStr.append(this.this$0.mApnSetting);
                logStr = logStr.toString();
                this.this$0.mNetCapsLocalLog.log(logStr);
                log(logStr);
                this.mNetworkCapabilities = networkCapabilities;
            }
            super.sendNetworkCapabilities(networkCapabilities);
        }

        protected void startPacketKeepalive(Message msg) {
            this.this$0.obtainMessage(DataConnection.EVENT_KEEPALIVE_START_REQUEST, msg.arg1, msg.arg2, msg.obj).sendToTarget();
        }

        protected void stopPacketKeepalive(Message msg) {
            this.this$0.obtainMessage(DataConnection.EVENT_KEEPALIVE_STOP_REQUEST, msg.arg1, msg.arg2, msg.obj).sendToTarget();
        }
    }

    public static class DisconnectParams {
        public ApnContext mApnContext;
        Message mOnCompletedMsg;
        String mReason;
        int mTag;

        DisconnectParams(ApnContext apnContext, String reason, Message onCompletedMsg) {
            this.mApnContext = apnContext;
            this.mReason = reason;
            this.mOnCompletedMsg = onCompletedMsg;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{mTag=");
            stringBuilder.append(this.mTag);
            stringBuilder.append(" mApnContext=");
            stringBuilder.append(this.mApnContext);
            stringBuilder.append(" mReason=");
            stringBuilder.append(this.mReason);
            stringBuilder.append(" mOnCompletedMsg=");
            stringBuilder.append(DataConnection.msgToString(this.mOnCompletedMsg));
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    public enum SetupResult {
        SUCCESS,
        ERROR_RADIO_NOT_AVAILABLE,
        ERROR_INVALID_ARG,
        ERROR_STALE,
        ERROR_DATA_SERVICE_SPECIFIC_ERROR;
        
        public DcFailCause mFailCause;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name());
            stringBuilder.append("  SetupResult.mFailCause=");
            stringBuilder.append(this.mFailCause);
            return stringBuilder.toString();
        }
    }

    public static class UpdateLinkPropertyResult {
        public LinkProperties newLp;
        public LinkProperties oldLp;
        public SetupResult setupResult = SetupResult.SUCCESS;

        public UpdateLinkPropertyResult(LinkProperties curLp) {
            this.oldLp = curLp;
            this.newLp = curLp;
        }
    }

    static {
        sCmdToString[0] = "EVENT_CONNECT";
        sCmdToString[1] = "EVENT_SETUP_DATA_CONNECTION_DONE";
        sCmdToString[3] = "EVENT_DEACTIVATE_DONE";
        sCmdToString[4] = "EVENT_DISCONNECT";
        sCmdToString[5] = "EVENT_RIL_CONNECTED";
        sCmdToString[6] = "EVENT_DISCONNECT_ALL";
        sCmdToString[7] = "EVENT_DATA_STATE_CHANGED";
        sCmdToString[8] = "EVENT_TEAR_DOWN_NOW";
        sCmdToString[9] = "EVENT_LOST_CONNECTION";
        sCmdToString[11] = "EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED";
        sCmdToString[12] = "EVENT_DATA_CONNECTION_ROAM_ON";
        sCmdToString[13] = "EVENT_DATA_CONNECTION_ROAM_OFF";
        sCmdToString[14] = "EVENT_BW_REFRESH_RESPONSE";
        sCmdToString[15] = "EVENT_DATA_CONNECTION_VOICE_CALL_STARTED";
        sCmdToString[16] = "EVENT_DATA_CONNECTION_VOICE_CALL_ENDED";
        sCmdToString[17] = "EVENT_DATA_CONNECTION_OVERRIDE_CHANGED";
        sCmdToString[18] = "EVENT_KEEPALIVE_STATUS";
        sCmdToString[19] = "EVENT_KEEPALIVE_STARTED";
        sCmdToString[20] = "EVENT_KEEPALIVE_STOPPED";
        sCmdToString[21] = "EVENT_KEEPALIVE_START_REQUEST";
        sCmdToString[22] = "EVENT_KEEPALIVE_STOP_REQUEST";
        sCmdToString[23] = "EVENT_LINK_CAPACITY_CHANGED";
        sCmdToString[24] = "EVENT_CLEAR_LINK";
        sCmdToString[25] = "EVENT_RESUME_LINK";
    }

    static String cmdToString(int cmd) {
        String value;
        cmd -= InboundSmsTracker.DEST_PORT_FLAG_3GPP2;
        if (cmd < 0 || cmd >= sCmdToString.length) {
            value = DcAsyncChannel.cmdToString(cmd + InboundSmsTracker.DEST_PORT_FLAG_3GPP2);
        } else {
            value = sCmdToString[cmd];
        }
        if (value != null) {
            return value;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0x");
        stringBuilder.append(Integer.toHexString(InboundSmsTracker.DEST_PORT_FLAG_3GPP2 + cmd));
        return stringBuilder.toString();
    }

    public static DataConnection makeDataConnection(Phone phone, int id, DcTracker dct, DataServiceManager dataServiceManager, DcTesterFailBringUpAll failBringUpAll, DcController dcc) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DC-");
        stringBuilder.append(mInstanceNumber.incrementAndGet());
        DataConnection dc = new DataConnection(phone, stringBuilder.toString(), id, dct, dataServiceManager, failBringUpAll, dcc);
        dc.start();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Made ");
        stringBuilder2.append(dc.getName());
        dc.log(stringBuilder2.toString());
        return dc;
    }

    void dispose() {
        log("dispose: call quiteNow()");
        quitNow();
    }

    LinkProperties getCopyLinkProperties() {
        return new LinkProperties(this.mLinkProperties);
    }

    boolean isInactive() {
        return getCurrentState() == this.mInactiveState;
    }

    boolean isDisconnecting() {
        return getCurrentState() == this.mDisconnectingState;
    }

    boolean isActive() {
        return getCurrentState() == this.mActiveState;
    }

    boolean isActivating() {
        return getCurrentState() == this.mActivatingState;
    }

    int getCid() {
        return this.mCid;
    }

    ApnSetting getApnSetting() {
        return this.mApnSetting;
    }

    boolean checkApnContext(ApnContext apnContext) {
        if (apnContext == null) {
            return false;
        }
        return this.mApnContexts.containsKey(apnContext);
    }

    void setLinkPropertiesHttpProxy(ProxyInfo proxy) {
        this.mLinkProperties.setHttpProxy(proxy);
    }

    public boolean isIpv4Connected() {
        for (InetAddress addr : this.mLinkProperties.getAddresses()) {
            if (addr instanceof Inet4Address) {
                Inet4Address i4addr = (Inet4Address) addr;
                if (!(i4addr.isAnyLocalAddress() || i4addr.isLinkLocalAddress() || i4addr.isLoopbackAddress() || i4addr.isMulticastAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isIpv6Connected() {
        for (InetAddress addr : this.mLinkProperties.getAddresses()) {
            if (addr instanceof Inet6Address) {
                Inet6Address i6addr = (Inet6Address) addr;
                if (!(i6addr.isAnyLocalAddress() || i6addr.isLinkLocalAddress() || i6addr.isLoopbackAddress() || i6addr.isMulticastAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    public UpdateLinkPropertyResult updateLinkProperty(DataCallResponse newState) {
        UpdateLinkPropertyResult result = new UpdateLinkPropertyResult(this.mLinkProperties);
        if (newState == null) {
            return result;
        }
        result.newLp = new LinkProperties();
        result.setupResult = setLinkProperties(newState, result.newLp);
        if (result.setupResult != SetupResult.SUCCESS) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateLinkProperty failed : ");
            stringBuilder.append(result.setupResult);
            log(stringBuilder.toString());
            return result;
        }
        result.newLp.setHttpProxy(this.mLinkProperties.getHttpProxy());
        checkSetMtu(this.mApnSetting, result.newLp);
        this.mLinkProperties = result.newLp;
        updateTcpBufferSizes(this.mRilRat);
        if (!result.oldLp.equals(result.newLp)) {
            log("updateLinkProperty old LP=*");
        }
        if (!(result.newLp.equals(result.oldLp) || this.mNetworkAgent == null)) {
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
        return result;
    }

    private void checkSetMtu(ApnSetting apn, LinkProperties lp) {
        if (lp != null && apn != null && lp != null) {
            if (this.mHwCustDataConnection != null && this.mHwCustDataConnection.setMtuIfNeeded(lp, this.mPhone)) {
                return;
            }
            StringBuilder stringBuilder;
            if (lp.getMtu() != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("MTU set by call response to: ");
                stringBuilder.append(lp.getMtu());
                log(stringBuilder.toString());
            } else if (apn != null && apn.mtu != 0) {
                lp.setMtu(apn.mtu);
                stringBuilder = new StringBuilder();
                stringBuilder.append("MTU set by APN to: ");
                stringBuilder.append(apn.mtu);
                log(stringBuilder.toString());
            } else if (this.mDct.isCTSimCard(this.mPhone.getPhoneId())) {
                log("MTU not set in CT Card");
            } else {
                int mtu = this.mPhone.getContext().getResources().getInteger(17694821);
                if (mtu != 0) {
                    lp.setMtu(mtu);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("MTU set by config resource to: ");
                    stringBuilder2.append(mtu);
                    log(stringBuilder2.toString());
                }
            }
        }
    }

    private DataConnection(Phone phone, String name, int id, DcTracker dct, DataServiceManager dataServiceManager, DcTesterFailBringUpAll failBringUpAll, DcController dcc) {
        super(name, dcc.getHandler());
        setLogRecSize(ScanIntervalRange.MAX);
        setLogOnlyTransitions(true);
        log("DataConnection created");
        this.mPhone = phone;
        this.mDct = dct;
        this.mDataServiceManager = dataServiceManager;
        this.mDcTesterFailBringUpAll = failBringUpAll;
        this.mDcController = dcc;
        this.mId = id;
        this.mCid = -1;
        ServiceState ss = this.mPhone.getServiceState();
        this.mRilRat = ss.getRilDataRadioTechnology();
        this.mDataRegState = this.mPhone.getServiceState().getDataRegState();
        int networkType = ss.getDataNetworkType();
        this.mNetworkInfo = new NetworkInfo(0, networkType, NETWORK_TYPE, TelephonyManager.getNetworkTypeName(networkType));
        this.mNetworkInfo.setRoaming(ss.getDataRoaming());
        this.mNetworkInfo.setIsAvailable(true);
        addState(this.mDefaultState);
        addState(this.mInactiveState, this.mDefaultState);
        addState(this.mActivatingState, this.mDefaultState);
        addState(this.mActiveState, this.mDefaultState);
        addState(this.mDisconnectingState, this.mDefaultState);
        addState(this.mDisconnectingErrorCreatingConnection, this.mDefaultState);
        setInitialState(this.mInactiveState);
        this.mApnContexts = new HashMap();
        this.mHwCustDataConnection = (HwCustDataConnection) HwCustUtils.createObj(HwCustDataConnection.class, new Object[0]);
    }

    private void onConnect(ConnectionParams cp) {
        ConnectionParams connectionParams = cp;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onConnect: carrier='");
        stringBuilder.append(this.mApnSetting.carrier);
        stringBuilder.append("' APN='");
        stringBuilder.append(this.mApnSetting.apn);
        stringBuilder.append("' proxy='");
        stringBuilder.append(this.mApnSetting.proxy);
        stringBuilder.append("' port='");
        stringBuilder.append(this.mApnSetting.port);
        stringBuilder.append("'");
        log(stringBuilder.toString());
        if (connectionParams.mApnContext != null) {
            connectionParams.mApnContext.requestLog("DataConnection.onConnect");
        }
        if (this.mDcTesterFailBringUpAll.getDcFailBringUp().mCounter > 0) {
            DataCallResponse dataCallResponse = new DataCallResponse(this.mDcTesterFailBringUpAll.getDcFailBringUp().mFailCause.getErrorCode(), this.mDcTesterFailBringUpAll.getDcFailBringUp().mSuggestedRetryTime, 0, 0, "", "", null, null, null, null, 0);
            Message msg = obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE, connectionParams);
            AsyncResult.forMessage(msg, dataCallResponse, null);
            sendMessage(msg);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onConnect: FailBringUpAll=");
            stringBuilder2.append(this.mDcTesterFailBringUpAll.getDcFailBringUp());
            stringBuilder2.append(" send error response=");
            stringBuilder2.append(dataCallResponse);
            log(stringBuilder2.toString());
            DcFailBringUp dcFailBringUp = this.mDcTesterFailBringUpAll.getDcFailBringUp();
            dcFailBringUp.mCounter--;
            return;
        }
        this.mCreateTime = -1;
        this.mLastFailTime = -1;
        this.mLastFailCause = DcFailCause.NONE;
        Message msg2 = obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE, connectionParams);
        msg2.obj = connectionParams;
        HwDataConnectionManager sHwDataConnectionManager = HwTelephonyFactory.getHwDataConnectionManager();
        if (sHwDataConnectionManager != null && sHwDataConnectionManager.getNamSwitcherForSoftbank()) {
            HashMap<String, String> userInfo = sHwDataConnectionManager.encryptApnInfoForSoftBank(this.mPhone, this.mApnSetting);
            if (userInfo != null) {
                String userKey = "username";
                String pswKey = "password";
                String username = (String) userInfo.get(userKey);
                String password = (String) userInfo.get(pswKey);
                log("onConnect: mApnSetting.user-mApnSetting.password handle finish");
                DataServiceManager dataServiceManager = this.mDataServiceManager;
                int i = connectionParams.mRilRat;
                int i2 = connectionParams.mProfileId;
                String str = this.mApnSetting.apn;
                String str2 = this.mApnSetting.protocol;
                int i3 = this.mApnSetting.authType;
                int i4 = this.mApnSetting.bearerBitmask == 0 ? 0 : ServiceState.bearerBitmapHasCdma(this.mApnSetting.bearerBitmask) ? 2 : 1;
                dataServiceManager.setupDataCall(i, new DataProfile(i2, str, str2, i3, username, password, i4, this.mApnSetting.maxConnsTime, this.mApnSetting.maxConns, this.mApnSetting.waitTime, this.mApnSetting.carrierEnabled, this.mApnSetting.typesBitmap, this.mApnSetting.roamingProtocol, this.mApnSetting.bearerBitmask, this.mApnSetting.mtu, this.mApnSetting.mvnoType, this.mApnSetting.mvnoMatchData, this.mApnSetting.modemCognitive), this.mPhone.getServiceState().getDataRoamingFromRegistration(), this.mPhone.getDataRoamingEnabled(), 1, null, msg2);
                return;
            }
        }
        Message msg3 = msg2;
        HwDataConnectionManager hwDataConnectionManager = sHwDataConnectionManager;
        ConnectionParams connectionParams2 = cp;
        DataProfile dp = DcTracker.createDataProfile(this.mApnSetting, connectionParams2.mProfileId);
        if (!(this.mHwCustDataConnection == null || !this.mHwCustDataConnection.whetherSetApnByCust(this.mPhone) || HwModemCapability.isCapabilitySupport(9))) {
            DataProfile dpCopy = dp;
            dp = new DataProfile(dpCopy.getProfileId(), "", dpCopy.getProtocol(), dpCopy.getAuthType(), dpCopy.getUserName(), dpCopy.getPassword(), dpCopy.getType(), dpCopy.getMaxConnsTime(), dpCopy.getMaxConns(), dpCopy.getWaitTime(), dpCopy.isEnabled(), dpCopy.getSupportedApnTypesBitmap(), dpCopy.getRoamingProtocol(), dpCopy.getBearerBitmap(), dpCopy.getMtu(), dpCopy.getMvnoType(), dpCopy.getMvnoMatchData(), dpCopy.isModemCognitive());
        }
        if (this.mHwCustDataConnection == null || !this.mHwCustDataConnection.isEmergencyApnSetting(this.mApnSetting)) {
            Message msg4 = msg3;
            boolean isModemRoaming = this.mPhone.getServiceState().getDataRoamingFromRegistration();
            boolean allowRoaming = this.mPhone.getDataRoamingEnabled() || (isModemRoaming && !this.mPhone.getServiceState().getDataRoaming());
            this.mDataServiceManager.setupDataCall(ServiceState.rilRadioTechnologyToAccessNetworkType(connectionParams2.mRilRat), dp, isModemRoaming, allowRoaming, 1, null, msg4);
            TelephonyMetrics.getInstance().writeSetupDataCall(this.mPhone.getPhoneId(), connectionParams2.mRilRat, dp.getProfileId(), dp.getApn(), dp.getProtocol());
            return;
        }
        this.mPhone.mCi.setupEIMEDataCall(msg3);
    }

    public void onSubscriptionOverride(int overrideMask, int overrideValue) {
        this.mSubscriptionOverride = (this.mSubscriptionOverride & (~overrideMask)) | (overrideValue & overrideMask);
        sendMessage(obtainMessage(EVENT_DATA_CONNECTION_OVERRIDE_CHANGED));
    }

    private void tearDownData(Object o) {
        int discReason = 1;
        ApnContext apnContext = null;
        if (o != null && (o instanceof DisconnectParams)) {
            DisconnectParams dp = (DisconnectParams) o;
            apnContext = dp.mApnContext;
            if (TextUtils.equals(dp.mReason, PhoneInternalInterface.REASON_RADIO_TURNED_OFF) || TextUtils.equals(dp.mReason, PhoneInternalInterface.REASON_PDP_RESET)) {
                discReason = 2;
            }
        }
        String str = new StringBuilder();
        str.append("tearDownData. mCid=");
        str.append(this.mCid);
        str.append(", reason=");
        str.append(discReason);
        str = str.toString();
        log(str);
        if (apnContext != null) {
            apnContext.requestLog(str);
        }
        boolean tearDownEimsData = false;
        if (!(apnContext == null || this.mHwCustDataConnection == null || !this.mHwCustDataConnection.isEmergencyApnSetting(apnContext.getApnSetting()))) {
            tearDownEimsData = true;
        }
        if (tearDownEimsData) {
            this.mPhone.mCi.deactivateEIMEDataCall(obtainMessage(EVENT_DEACTIVATE_DONE, this.mTag, 0, o));
        } else {
            this.mDataServiceManager.deactivateDataCall(this.mCid, discReason, obtainMessage(EVENT_DEACTIVATE_DONE, this.mTag, 0, o));
        }
        VSimUtilsInner.checkMmsStop(this.mPhone.getPhoneId());
    }

    private void notifyAllWithEvent(ApnContext alreadySent, int event, String reason) {
        this.mNetworkInfo.setDetailedState(this.mNetworkInfo.getDetailedState(), reason, this.mNetworkInfo.getExtraInfo());
        for (ConnectionParams cp : this.mApnContexts.values()) {
            ApnContext apnContext = cp.mApnContext;
            if (apnContext != alreadySent) {
                if (reason != null) {
                    apnContext.setReason(reason);
                }
                Message msg = this.mDct.obtainMessage(event, new Pair(apnContext, Integer.valueOf(cp.mConnectionGeneration)));
                AsyncResult.forMessage(msg);
                msg.sendToTarget();
            }
        }
    }

    private void notifyAllOfConnected(String reason) {
        notifyAllWithEvent(null, 270336, reason);
    }

    private void notifyAllOfDisconnectDcRetrying(String reason) {
        notifyAllWithEvent(null, 270370, reason);
    }

    private void notifyAllDisconnectCompleted(DcFailCause cause) {
        notifyAllWithEvent(null, 270351, cause.toString());
    }

    private void notifyConnectCompleted(ConnectionParams cp, DcFailCause cause, boolean sendAll) {
        ApnContext alreadySent = null;
        if (!(cp == null || cp.mOnCompletedMsg == null)) {
            Message connectionCompletedMsg = cp.mOnCompletedMsg;
            cp.mOnCompletedMsg = null;
            alreadySent = cp.mApnContext;
            long timeStamp = System.currentTimeMillis();
            connectionCompletedMsg.arg1 = this.mCid;
            if (cause == DcFailCause.NONE) {
                this.mCreateTime = timeStamp;
                AsyncResult.forMessage(connectionCompletedMsg);
            } else {
                this.mLastFailCause = cause;
                this.mLastFailTime = timeStamp;
                if (cause == null) {
                    cause = DcFailCause.UNKNOWN;
                }
                cp.mApnContext.setPdpFailCause(cause);
                AsyncResult.forMessage(connectionCompletedMsg, cause, new Throwable(cause.toString()));
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyConnectCompleted at ");
            stringBuilder.append(timeStamp);
            stringBuilder.append(" cause=");
            stringBuilder.append(cause);
            stringBuilder.append(" connectionCompletedMsg=");
            stringBuilder.append(msgToString(connectionCompletedMsg));
            log(stringBuilder.toString());
            connectionCompletedMsg.sendToTarget();
        }
        if (sendAll) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Send to all. ");
            stringBuilder2.append(alreadySent);
            stringBuilder2.append(" ");
            stringBuilder2.append(cause.toString());
            log(stringBuilder2.toString());
            notifyAllWithEvent(alreadySent, 270371, cause.toString());
        }
    }

    private void notifyDisconnectCompleted(DisconnectParams dp, boolean sendAll) {
        log("NotifyDisconnectCompleted");
        ApnContext alreadySent = null;
        String reason = null;
        if (!(dp == null || dp.mOnCompletedMsg == null)) {
            Message msg = dp.mOnCompletedMsg;
            dp.mOnCompletedMsg = null;
            if (msg.obj instanceof ApnContext) {
                alreadySent = msg.obj;
            }
            reason = dp.mReason;
            String str = "msg=%s msg.obj=%s";
            Object[] objArr = new Object[2];
            objArr[0] = msg.toString();
            objArr[1] = msg.obj instanceof String ? (String) msg.obj : "<no-reason>";
            log(String.format(str, objArr));
            AsyncResult.forMessage(msg);
            msg.sendToTarget();
        }
        if (sendAll) {
            if (reason == null) {
                reason = DcFailCause.UNKNOWN.toString();
            }
            notifyAllWithEvent(alreadySent, 270351, reason);
        }
        log("NotifyDisconnectCompleted DisconnectParams=*");
    }

    public int getDataConnectionId() {
        return this.mId;
    }

    private void clearSettings() {
        log("clearSettings");
        this.mCreateTime = -1;
        this.mLastFailTime = -1;
        this.mLastFailCause = DcFailCause.NONE;
        this.mCid = -1;
        this.mPcscfAddr = new String[5];
        this.mLinkProperties = new LinkProperties();
        this.mApnContexts.clear();
        this.mApnSetting = null;
        this.mDcFailCause = null;
        this.mEhrpdFailCount = 0;
    }

    private SetupResult onSetupConnectionCompleted(int resultCode, DataCallResponse response, ConnectionParams cp) {
        SetupResult result;
        if (cp.mTag != this.mTag) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSetupConnectionCompleted stale cp.tag=");
            stringBuilder.append(cp.mTag);
            stringBuilder.append(", mtag=");
            stringBuilder.append(this.mTag);
            log(stringBuilder.toString());
            return SetupResult.ERROR_STALE;
        } else if (resultCode == 4) {
            result = SetupResult.ERROR_RADIO_NOT_AVAILABLE;
            result.mFailCause = DcFailCause.RADIO_NOT_AVAILABLE;
            return result;
        } else if (response.getStatus() == 0) {
            log("onSetupConnectionCompleted received successful DataCallResponse");
            this.mCid = response.getCallId();
            this.mPcscfAddr = (String[]) response.getPcscfs().toArray(new String[response.getPcscfs().size()]);
            int i = 0;
            if (cp.mApnContext.getApnType().equals("internaldefault")) {
                cp.mApnContext.setEnabled(false);
            }
            result = updateLinkProperty(response).setupResult;
            if (cp.mApnContext.getApnType().equals("mms") || cp.mApnContext.getApnType().equals("ims") || response.getDnses() == null) {
                return result;
            }
            int dnsSize = response.getDnses().size();
            String[] dnses = new String[dnsSize];
            while (i < dnsSize) {
                dnses[i] = ((InetAddress) response.getDnses().get(i)).toString();
                i++;
            }
            HwTelephonyFactory.getHwDataServiceChrManager().SendIntentDNSfailure(dnses);
            return result;
        } else if (response.getStatus() == DcFailCause.RADIO_NOT_AVAILABLE.getErrorCode()) {
            result = SetupResult.ERROR_RADIO_NOT_AVAILABLE;
            result.mFailCause = DcFailCause.RADIO_NOT_AVAILABLE;
            return result;
        } else {
            result = SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR;
            result.mFailCause = DcFailCause.fromInt(response.getStatus());
            return result;
        }
    }

    private boolean isDnsOk(String[] domainNameServers) {
        if (!NULL_IP.equals(domainNameServers[0]) || !NULL_IP.equals(domainNameServers[1]) || this.mPhone.isDnsCheckDisabled() || (this.mApnSetting.types[0].equals("mms") && isIpAddress(this.mApnSetting.mmsProxy))) {
            return true;
        }
        log(String.format("isDnsOk: return false apn.types[0]=%s APN_TYPE_MMS=%s isIpAddress(%s)=%s", new Object[]{this.mApnSetting.types[0], "mms", this.mApnSetting.mmsProxy, Boolean.valueOf(isIpAddress(this.mApnSetting.mmsProxy))}));
        return false;
    }

    private void updateTcpBufferSizes(int rilRat) {
        String sizes = null;
        if (rilRat == 19) {
            rilRat = 14;
        }
        String ratName = ServiceState.rilRadioTechnologyToString(rilRat).toLowerCase(Locale.ROOT);
        if (rilRat == 7 || rilRat == 8 || rilRat == 12) {
            ratName = "evdo";
        }
        String[] configOverride = this.mPhone.getContext().getResources().getStringArray(17236020);
        for (String[] split : configOverride) {
            String[] split2 = split2.split(":");
            if (ratName.equals(split2[0]) && split2.length == 2) {
                sizes = split2[1];
                break;
            }
        }
        String tcpBufferSizePropName = "hw.net.tcp.buffersize.";
        if (sizes == null) {
            StringBuilder stringBuilder;
            switch (rilRat) {
                case 1:
                    sizes = TCP_BUFFER_SIZES_GPRS;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("gprs");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
                case 2:
                    sizes = TCP_BUFFER_SIZES_EDGE;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("edge");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
                case 3:
                case 17:
                    sizes = TCP_BUFFER_SIZES_UMTS;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("umts");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
                case 6:
                    sizes = TCP_BUFFER_SIZES_1XRTT;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("1xrtt");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
                case 7:
                case 8:
                case 12:
                    sizes = TCP_BUFFER_SIZES_EVDO;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("evdo");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
                case 9:
                    sizes = TCP_BUFFER_SIZES_HSDPA;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("hsdpa");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
                case 10:
                case 11:
                    sizes = TCP_BUFFER_SIZES_HSPA;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("hspa");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
                case 13:
                    sizes = TCP_BUFFER_SIZES_EHRPD;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("ehrpd");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
                case 14:
                case 19:
                    sizes = TCP_BUFFER_SIZES_LTE;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("lte");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
                case 15:
                    sizes = TCP_BUFFER_SIZES_HSPAP;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(tcpBufferSizePropName);
                    stringBuilder.append("hspap");
                    tcpBufferSizePropName = stringBuilder.toString();
                    break;
            }
        }
        StringBuilder stringBuilder2;
        try {
            String custTcpBuffer = SystemProperties.get(tcpBufferSizePropName);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("custTcpBuffer = ");
            stringBuilder2.append(custTcpBuffer);
            log(stringBuilder2.toString());
            if (custTcpBuffer != null && custTcpBuffer.length() > 0) {
                sizes = custTcpBuffer;
            }
            if (this.mPhone != null) {
                String hwTcpBuffer = (String) HwCfgFilePolicy.getValue(tcpBufferSizePropName, SubscriptionManager.getSlotIndex(this.mPhone.getPhoneId()), String.class);
                if (hwTcpBuffer != null && hwTcpBuffer.length() > 0) {
                    sizes = hwTcpBuffer;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("HwCfgFile:custTcpBuffer = ");
                    stringBuilder3.append(sizes);
                    log(stringBuilder3.toString());
                }
            }
        } catch (Exception e) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception: read custTcpBuffer error ");
            stringBuilder2.append(e.toString());
            log(stringBuilder2.toString());
        }
        this.mLinkProperties.setTcpBufferSizes(sizes);
    }

    private void setNetworkRestriction() {
        this.mRestrictedNetworkOverride = false;
        boolean noRestrictedRequests = true;
        for (ApnContext apnContext : this.mApnContexts.keySet()) {
            noRestrictedRequests &= apnContext.hasNoRestrictedRequests(true);
        }
        if (!noRestrictedRequests && this.mApnSetting.isMetered(this.mPhone)) {
            this.mRestrictedNetworkOverride = this.mDct.isDataEnabled() ^ 1;
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    NetworkCapabilities getNetworkCapabilities() {
        int size;
        NetworkCapabilities result = new NetworkCapabilities();
        boolean z = false;
        result.addTransportType(0);
        ArrayList<String> apnTypes = new ArrayList();
        for (ApnContext apnContext : this.mApnContexts.keySet()) {
            apnTypes.add(apnContext.getApnType());
        }
        boolean isBipNetwork = (this.mConnectionParams == null || this.mConnectionParams.mApnContext == null || this.mDct == null || !this.mDct.isBipApnType(this.mConnectionParams.mApnContext.getApnType())) ? false : true;
        if (this.mApnSetting != null) {
            ArrayList<ApnSetting> securedDunApns = this.mDct.fetchDunApns();
            String[] types = this.mApnSetting.types;
            if (enableCompatibleSimilarApnSettings()) {
                types = getCompatibleSimilarApnSettingsTypes(this.mApnSetting, this.mDct.getAllApnList());
            }
            int length = types.length;
            int i = 0;
            while (i < length) {
                String type = types[i];
                boolean shouldDropMeterApn = (this.mRestrictedNetworkOverride || this.mConnectionParams == null || !this.mConnectionParams.mUnmeteredUseOnly || !ApnSetting.isMeteredApnType(type, this.mPhone)) ? z : true;
                if (!shouldDropMeterApn) {
                    int hashCode = type.hashCode();
                    switch (hashCode) {
                        case 3023943:
                            if (type.equals("bip0")) {
                                hashCode = 10;
                                break;
                            }
                        case 3023944:
                            if (type.equals("bip1")) {
                                hashCode = 11;
                                break;
                            }
                        case 3023945:
                            if (type.equals("bip2")) {
                                hashCode = 12;
                                break;
                            }
                        case 3023946:
                            if (type.equals("bip3")) {
                                hashCode = 13;
                                break;
                            }
                        case 3023947:
                            if (type.equals("bip4")) {
                                hashCode = 14;
                                break;
                            }
                        case 3023948:
                            if (type.equals("bip5")) {
                                hashCode = 15;
                                break;
                            }
                        case 3023949:
                            if (type.equals("bip6")) {
                                hashCode = 16;
                                break;
                            }
                        default:
                            switch (hashCode) {
                                case -1490587420:
                                    if (type.equals("internaldefault")) {
                                        hashCode = 18;
                                        break;
                                    }
                                case 42:
                                    if (type.equals(CharacterSets.MIMENAME_ANY_CHARSET)) {
                                        hashCode = 0;
                                        break;
                                    }
                                case 3352:
                                    if (type.equals("ia")) {
                                        hashCode = 8;
                                        break;
                                    }
                                case 98292:
                                    if (type.equals("cbs")) {
                                        hashCode = 7;
                                        break;
                                    }
                                case 99837:
                                    if (type.equals("dun")) {
                                        hashCode = 4;
                                        break;
                                    }
                                case 104399:
                                    if (type.equals("ims")) {
                                        hashCode = 6;
                                        break;
                                    }
                                case 108243:
                                    if (type.equals("mms")) {
                                        hashCode = 2;
                                        break;
                                    }
                                case 3149046:
                                    if (type.equals("fota")) {
                                        hashCode = 5;
                                        break;
                                    }
                                case 3541982:
                                    if (type.equals("supl")) {
                                        hashCode = 3;
                                        break;
                                    }
                                case 3673178:
                                    if (type.equals("xcap")) {
                                        hashCode = 17;
                                        break;
                                    }
                                case 1544803905:
                                    if (type.equals("default")) {
                                        hashCode = 1;
                                        break;
                                    }
                                case 1629013393:
                                    if (type.equals("emergency")) {
                                        hashCode = 9;
                                        break;
                                    }
                            }
                            hashCode = -1;
                            break;
                    }
                    switch (hashCode) {
                        case 0:
                            result.addCapability(12);
                            result.addCapability(0);
                            if (DcTracker.CT_SUPL_FEATURE_ENABLE && !apnTypes.contains("supl") && this.mDct.isCTSimCard(this.mPhone.getSubId())) {
                                log("ct supl feature enabled and apncontex didn't contain supl, didn't add supl capability");
                            } else {
                                result.addCapability(1);
                            }
                            result.addCapability(3);
                            result.addCapability(4);
                            result.addCapability(5);
                            result.addCapability(7);
                            size = securedDunApns.size();
                            for (hashCode = 0; hashCode < size; hashCode++) {
                                if (this.mApnSetting.equals(securedDunApns.get(hashCode))) {
                                    result.addCapability(2);
                                    result.addCapability(23);
                                    result.addCapability(24);
                                    result.addCapability(25);
                                    result.addCapability(26);
                                    result.addCapability(27);
                                    result.addCapability(28);
                                    result.addCapability(29);
                                    result.addCapability(9);
                                    result.addCapability(30);
                                    break;
                                }
                            }
                            result.addCapability(23);
                            result.addCapability(24);
                            result.addCapability(25);
                            result.addCapability(26);
                            result.addCapability(27);
                            result.addCapability(28);
                            result.addCapability(29);
                            result.addCapability(9);
                            result.addCapability(30);
                            break;
                        case 1:
                            result.addCapability(12);
                            break;
                        case 2:
                            result.addCapability(0);
                            break;
                        case 3:
                            if (!DcTracker.CT_SUPL_FEATURE_ENABLE || apnTypes.contains("supl") || !this.mDct.isCTSimCard(this.mPhone.getSubId())) {
                                result.addCapability(1);
                                break;
                            }
                            log("ct supl feature enabled and apncontex didn't contain supl, didn't add supl capability");
                            break;
                            break;
                        case 4:
                            result.addCapability(2);
                            break;
                        case 5:
                            result.addCapability(3);
                            break;
                        case 6:
                            result.addCapability(4);
                            break;
                        case 7:
                            result.addCapability(5);
                            break;
                        case 8:
                            result.addCapability(7);
                            break;
                        case 9:
                            result.addCapability(10);
                            break;
                        case 10:
                            result.addCapability(23);
                            break;
                        case 11:
                            result.addCapability(24);
                            break;
                        case 12:
                            result.addCapability(25);
                            break;
                        case 13:
                            result.addCapability(26);
                            break;
                        case 14:
                            result.addCapability(27);
                            break;
                        case 15:
                            result.addCapability(28);
                            break;
                        case 16:
                            result.addCapability(29);
                            break;
                        case 17:
                            result.addCapability(9);
                            break;
                        case 18:
                            result.addCapability(30);
                            break;
                        default:
                            break;
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Dropped the metered ");
                stringBuilder.append(type);
                stringBuilder.append(" for the unmetered data call.");
                log(stringBuilder.toString());
                i++;
                z = false;
            }
            if ((this.mConnectionParams == null || !this.mConnectionParams.mUnmeteredUseOnly || this.mRestrictedNetworkOverride) && this.mApnSetting.isMetered(this.mPhone)) {
                result.removeCapability(11);
            } else {
                result.addCapability(11);
            }
            z = this.mHwCustDataConnection != null && this.mHwCustDataConnection.isNeedReMakeCapability();
            if (z) {
                result = this.mHwCustDataConnection.getNetworkCapabilities(types, result, this.mApnSetting, this.mDct);
            }
            if (!isBipNetwork) {
                result.maybeMarkCapabilitiesRestricted();
            }
        }
        boolean z2 = this.mRestrictedNetworkOverride && !isBipNetwork;
        if (z2) {
            result.removeCapability(13);
            result.removeCapability(2);
        }
        int up = 14;
        int down = 14;
        size = this.mRilRat;
        if (size != 19) {
            switch (size) {
                case 1:
                    up = 80;
                    down = 80;
                    break;
                case 2:
                    up = 59;
                    down = 236;
                    break;
                case 3:
                    up = 384;
                    down = 384;
                    break;
                case 4:
                case 5:
                    up = 14;
                    down = 14;
                    break;
                case 6:
                    up = 100;
                    down = 100;
                    break;
                case 7:
                    up = 153;
                    down = 2457;
                    break;
                case 8:
                    up = 1843;
                    down = 3174;
                    break;
                case 9:
                    up = 2048;
                    down = 14336;
                    break;
                case 10:
                    up = 5898;
                    down = 14336;
                    break;
                case 11:
                    up = 5898;
                    down = 14336;
                    break;
                case 12:
                    up = 1843;
                    down = 5017;
                    break;
                case 13:
                    up = 153;
                    down = 2516;
                    break;
                case 14:
                    up = 51200;
                    down = 102400;
                    break;
                case 15:
                    up = 11264;
                    down = 43008;
                    break;
            }
        }
        up = 51200;
        down = 102400;
        result.setLinkUpstreamBandwidthKbps(up);
        result.setLinkDownstreamBandwidthKbps(down);
        result.setNetworkSpecifier(new StringNetworkSpecifier(Integer.toString(this.mPhone.getSubId())));
        result.setCapability(18, this.mPhone.getServiceState().getDataRoaming() ^ 1);
        result.addCapability(20);
        if ((this.mSubscriptionOverride & 1) != 0) {
            result.addCapability(11);
        }
        if ((this.mSubscriptionOverride & 2) != 0) {
            result.removeCapability(20);
        }
        return result;
    }

    @VisibleForTesting
    public static boolean isIpAddress(String address) {
        if (address == null) {
            return false;
        }
        return InetAddress.isNumeric(address);
    }

    public boolean enableCompatibleSimilarApnSettings() {
        if (HuaweiTelephonyConfigs.isHisiPlatform() && this.mPhone != null) {
            boolean similarApnState = false;
            boolean hasHwCfgConfig = false;
            try {
                Boolean similarApnSign = (Boolean) HwCfgFilePolicy.getValue("compatible_apn_switch", SubscriptionManager.getSlotIndex(this.mPhone.getSubId()), Boolean.class);
                if (similarApnSign != null) {
                    hasHwCfgConfig = true;
                    similarApnState = similarApnSign.booleanValue();
                }
                StringBuilder stringBuilder;
                if (similarApnState) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwCfgFile:similarApnSign: ");
                    stringBuilder.append(similarApnSign);
                    log(stringBuilder.toString());
                    return true;
                }
                if (hasHwCfgConfig && !similarApnState) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwCfgFile:similarApnSign: ");
                    stringBuilder.append(similarApnSign);
                    log(stringBuilder.toString());
                    return false;
                }
                String plmnsConfig = System.getString(this.mPhone.getContext().getContentResolver(), "compatible_apn_plmn");
                if (TextUtils.isEmpty(plmnsConfig)) {
                    return false;
                }
                String operator = this.mDct.getCTOperator(this.mDct.getOperatorNumeric());
                String[] plmns = plmnsConfig.split(",");
                int length = plmns.length;
                int i = 0;
                while (i < length) {
                    String plmn = plmns[i];
                    if (TextUtils.isEmpty(plmn) || !plmn.equals(operator)) {
                        i++;
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("enableCompatibleSimilarApnSettings: ");
                        stringBuilder2.append(operator);
                        log(stringBuilder2.toString());
                        return true;
                    }
                }
            } catch (Exception e) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Exception: read compatible_apn_plmn error ");
                stringBuilder3.append(e.toString());
                log(stringBuilder3.toString());
            }
        }
        return false;
    }

    public String[] getCompatibleSimilarApnSettingsTypes(ApnSetting currentApnSetting, ArrayList<ApnSetting> allApnSettings) {
        ArrayList<String> resultTypes = new ArrayList();
        if (currentApnSetting == null) {
            return (String[]) resultTypes.toArray(new String[0]);
        }
        resultTypes.addAll(Arrays.asList(currentApnSetting.types));
        if (allApnSettings == null) {
            return (String[]) resultTypes.toArray(new String[0]);
        }
        Iterator it = allApnSettings.iterator();
        while (it.hasNext()) {
            ApnSetting apn = (ApnSetting) it.next();
            if (!currentApnSetting.equals(apn)) {
                if (apnSettingsSimilar(currentApnSetting, apn)) {
                    for (String type : apn.types) {
                        if (!resultTypes.contains(type)) {
                            resultTypes.add(type);
                        }
                    }
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCompatibleSimilarApnSettingsTypes: ");
        stringBuilder.append(resultTypes);
        log(stringBuilder.toString());
        return (String[]) resultTypes.toArray(new String[0]);
    }

    private boolean apnSettingsSimilar(ApnSetting first, ApnSetting second) {
        return Objects.equals(first.apn, second.apn) && ((first.authType == second.authType || -1 == first.authType || -1 == second.authType) && Objects.equals(first.user, second.user) && Objects.equals(first.password, second.password) && Objects.equals(first.proxy, second.proxy) && Objects.equals(first.port, second.port) && xorEqualsProtocol(first.protocol, second.protocol) && xorEqualsProtocol(first.roamingProtocol, second.roamingProtocol) && first.carrierEnabled == second.carrierEnabled && first.bearerBitmask == second.bearerBitmask && first.mtu == second.mtu && xorEquals(first.mmsc, second.mmsc) && xorEquals(first.mmsProxy, second.mmsProxy) && xorEquals(first.mmsPort, second.mmsPort));
    }

    private boolean xorEquals(String first, String second) {
        return Objects.equals(first, second) || TextUtils.isEmpty(first) || TextUtils.isEmpty(second);
    }

    private boolean xorEqualsProtocol(String first, String second) {
        return Objects.equals(first, second) || (("IPV4V6".equals(first) && ("IP".equals(second) || "IPV6".equals(second))) || (("IP".equals(first) && "IPV4V6".equals(second)) || ("IPV6".equals(first) && "IPV4V6".equals(second))));
    }

    private SetupResult setLinkProperties(DataCallResponse response, LinkProperties linkProperties) {
        SetupResult result;
        StringBuilder stringBuilder;
        String propertyPrefix = new StringBuilder();
        propertyPrefix.append("net.");
        propertyPrefix.append(response.getIfname());
        propertyPrefix.append(".");
        propertyPrefix = propertyPrefix.toString();
        String[] dnsServers = new String[2];
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(propertyPrefix);
        stringBuilder2.append("dns1");
        int i = 0;
        dnsServers[0] = SystemProperties.get(stringBuilder2.toString());
        dnsServers[1] = "8.8.8.8";
        boolean okToUseSystemPropertyDns = isDnsOk(dnsServers);
        linkProperties.clear();
        if (response.getStatus() == DcFailCause.NONE.getErrorCode()) {
            String dnsAddr;
            try {
                linkProperties.setInterfaceName(response.getIfname());
                if (response.getAddresses().size() > 0) {
                    for (LinkAddress la : response.getAddresses()) {
                        if (!la.getAddress().isAnyLocalAddress()) {
                            log("addr/pl=* ");
                            linkProperties.addLinkAddress(la);
                        }
                    }
                    if (response.getDnses().size() > 0) {
                        for (InetAddress dns : response.getDnses()) {
                            if (!dns.isAnyLocalAddress()) {
                                linkProperties.addDnsServer(dns);
                                if (dns instanceof Inet4Address) {
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append(propertyPrefix);
                                    stringBuilder3.append("dns1");
                                    SystemProperties.set(stringBuilder3.toString(), dns.getHostAddress());
                                }
                            }
                        }
                    } else if (okToUseSystemPropertyDns) {
                        int length = dnsServers.length;
                        while (i < length) {
                            dnsAddr = dnsServers[i].trim();
                            if (!dnsAddr.isEmpty()) {
                                InetAddress ia = NetworkUtils.numericToInetAddress(dnsAddr);
                                if (!ia.isAnyLocalAddress()) {
                                    linkProperties.addDnsServer(ia);
                                }
                            }
                            i++;
                        }
                    } else {
                        throw new UnknownHostException("Empty dns response and no system default dns");
                    }
                    for (InetAddress dns2 : response.getGateways()) {
                        linkProperties.addRoute(new RouteInfo(dns2));
                    }
                    linkProperties.setMtu(response.getMtu());
                    result = SetupResult.SUCCESS;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("no address for ifname=");
                    stringBuilder.append(response.getIfname());
                    throw new UnknownHostException(stringBuilder.toString());
                }
            } catch (IllegalArgumentException e) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Non-numeric dns addr=");
                stringBuilder4.append(dnsAddr);
                throw new UnknownHostException(stringBuilder4.toString());
            } catch (UnknownHostException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("setLinkProperties: UnknownHostException ");
                stringBuilder.append(e2);
                log(stringBuilder.toString());
                result = SetupResult.ERROR_INVALID_ARG;
            }
        } else {
            result = SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR;
        }
        if (result != SetupResult.SUCCESS) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setLinkProperties: error clearing LinkProperties status=");
            stringBuilder.append(response.getStatus());
            stringBuilder.append(" result=");
            stringBuilder.append(result);
            log(stringBuilder.toString());
            linkProperties.clear();
        }
        return result;
    }

    private boolean initConnection(ConnectionParams cp) {
        ApnContext apnContext = cp.mApnContext;
        boolean isBipUsingDefaultAPN = false;
        if (this.mApnSetting == null) {
            this.mApnSetting = apnContext.getApnSetting();
        }
        if (this.mApnSetting != null) {
            boolean z = this.mDct.isBipApnType(apnContext.getApnType()) && "default".equals(SystemProperties.get("gsm.bip.apn")) && this.mApnSetting.canHandleType("default");
            isBipUsingDefaultAPN = z;
        }
        StringBuilder stringBuilder;
        if (this.mApnSetting == null || !(this.mApnSetting.canHandleType(apnContext.getApnType()) || isBipUsingDefaultAPN)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("initConnection: incompatible apnSetting in ConnectionParams cp=");
            stringBuilder.append(cp);
            stringBuilder.append(" dc=");
            stringBuilder.append(this);
            log(stringBuilder.toString());
            return false;
        }
        this.mTag++;
        this.mConnectionParams = cp;
        this.mConnectionParams.mTag = this.mTag;
        this.mApnContexts.put(apnContext, cp);
        stringBuilder = new StringBuilder();
        stringBuilder.append("initConnection:  RefCount=");
        stringBuilder.append(this.mApnContexts.size());
        log(stringBuilder.toString());
        return true;
    }

    private void updateNetworkInfo() {
        ServiceState state = this.mPhone.getServiceState();
        int subtype = state.getDataNetworkType();
        this.mNetworkInfo.setSubtype(subtype, TelephonyManager.getNetworkTypeName(subtype));
        this.mNetworkInfo.setRoaming(state.getDataRoaming());
    }

    private void updateNetworkInfoSuspendState() {
        if (this.mNetworkAgent == null) {
            Rlog.e(getName(), "Setting suspend state without a NetworkAgent");
        }
        ServiceStateTracker sst = this.mPhone.getServiceStateTracker();
        if (sst.getCurrentDataConnectionState() != 0) {
            this.mNetworkInfo.setDetailedState(DetailedState.SUSPENDED, null, this.mNetworkInfo.getExtraInfo());
        } else {
            if (!sst.isConcurrentVoiceAndDataAllowed()) {
                CallTracker ct = this.mPhone.getCallTracker();
                if (!(ct == null || ct.getState() == PhoneConstants.State.IDLE)) {
                    this.mNetworkInfo.setDetailedState(DetailedState.SUSPENDED, null, this.mNetworkInfo.getExtraInfo());
                    return;
                }
            }
            this.mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, this.mNetworkInfo.getExtraInfo());
        }
    }

    private boolean isActivatingCancelSup(ConnectionParams deferConnectParams, Object disconnectParams) {
        if (!HuaweiTelephonyConfigs.isQcomPlatform()) {
            return false;
        }
        if (!(deferConnectParams == null || disconnectParams == null || !(disconnectParams instanceof DisconnectParams))) {
            ApnContext disconnectApnContext = ((DisconnectParams) disconnectParams).mApnContext;
            if (disconnectApnContext == null || !"supl".equals(disconnectApnContext.getApnType())) {
                logd("isActivatingCancelSup not disconnect supl");
                return false;
            }
            boolean connectingDefault = false;
            boolean connectingSupl = false;
            for (ApnContext apnContext : this.mApnContexts.keySet()) {
                if ("supl".equals(apnContext.getApnType())) {
                    connectingSupl = true;
                }
                if ("default".equals(apnContext.getApnType())) {
                    connectingDefault = true;
                }
            }
            ApnContext deferConnectApnContext = deferConnectParams.mApnContext;
            if (deferConnectApnContext != null) {
                if (connectingDefault && "supl".equals(deferConnectApnContext.getApnType())) {
                    logd("isActivatingCancelSup connectingDefault and defer supl");
                    return true;
                } else if (connectingSupl && "default".equals(deferConnectApnContext.getApnType())) {
                    logd("isActivatingCancelSup connectingSupl and defer default");
                    return true;
                }
            }
        }
        logd("isActivatingCancelSup end return false");
        return false;
    }

    void tearDownNow() {
        log("tearDownNow()");
        sendMessage(obtainMessage(EVENT_TEAR_DOWN_NOW));
    }

    private long getSuggestedRetryDelay(DataCallResponse response) {
        if (response.getSuggestedRetryTime() < 0) {
            log("No suggested retry delay.");
            return -2;
        } else if (response.getSuggestedRetryTime() != KeepaliveStatus.INVALID_HANDLE) {
            return (long) response.getSuggestedRetryTime();
        } else {
            log("Modem suggested not retrying.");
            return -1;
        }
    }

    protected String getWhatToString(int what) {
        return cmdToString(what);
    }

    private static String msgToString(Message msg) {
        if (msg == null) {
            return "null";
        }
        String retVal = new StringBuilder();
        retVal.append("{what=");
        retVal.append(cmdToString(msg.what));
        retVal.append(" when=");
        TimeUtils.formatDuration(msg.getWhen() - SystemClock.uptimeMillis(), retVal);
        if (msg.arg1 != 0) {
            retVal.append(" arg1=");
            retVal.append(msg.arg1);
        }
        if (msg.arg2 != 0) {
            retVal.append(" arg2=");
            retVal.append(msg.arg2);
        }
        if (msg.obj != null) {
            retVal.append(" obj=");
            retVal.append(msg.obj);
        }
        retVal.append(" target=");
        retVal.append(msg.getTarget());
        retVal.append(" replyTo=");
        retVal.append(msg.replyTo);
        retVal.append("}");
        return retVal.toString();
    }

    static void slog(String s) {
        Rlog.d("DC", s);
    }

    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    protected void logd(String s) {
        Rlog.d(getName(), s);
    }

    protected void logv(String s) {
        Rlog.v(getName(), s);
    }

    protected void logi(String s) {
        Rlog.i(getName(), s);
    }

    protected void logw(String s) {
        Rlog.w(getName(), s);
    }

    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    protected void loge(String s, Throwable e) {
        Rlog.e(getName(), s, e);
    }

    public String toStringSimple() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getName());
        stringBuilder.append(": State=");
        stringBuilder.append(getCurrentState().getName());
        stringBuilder.append(" mApnSetting=");
        stringBuilder.append(this.mApnSetting);
        stringBuilder.append(" RefCount=");
        stringBuilder.append(this.mApnContexts.size());
        stringBuilder.append(" mCid=");
        stringBuilder.append(this.mCid);
        stringBuilder.append(" mCreateTime=");
        stringBuilder.append(this.mCreateTime);
        stringBuilder.append(" mLastastFailTime=");
        stringBuilder.append(this.mLastFailTime);
        stringBuilder.append(" mLastFailCause=");
        stringBuilder.append(this.mLastFailCause);
        stringBuilder.append(" mTag=");
        stringBuilder.append(this.mTag);
        stringBuilder.append(" mLinkProperties=");
        stringBuilder.append(this.mLinkProperties);
        stringBuilder.append(" linkCapabilities=");
        stringBuilder.append(getNetworkCapabilities());
        stringBuilder.append(" mRestrictedNetworkOverride=");
        stringBuilder.append(this.mRestrictedNetworkOverride);
        return stringBuilder.toString();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append(toStringSimple());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private void dumpToLog() {
        dump(null, new PrintWriter(new StringWriter(0)) {
            public void println(String s) {
                DataConnection.this.logd(s);
            }

            public void flush() {
            }
        }, null);
    }

    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, " ");
        pw.print("DataConnection ");
        super.dump(fd, pw, args);
        pw.flush();
        pw.increaseIndent();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mApnContexts.size=");
        stringBuilder.append(this.mApnContexts.size());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mApnContexts=");
        stringBuilder.append(this.mApnContexts);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDataConnectionTracker=");
        stringBuilder.append(this.mDct);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mApnSetting=");
        stringBuilder.append(this.mApnSetting);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mTag=");
        stringBuilder.append(this.mTag);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCid=");
        stringBuilder.append(this.mCid);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mConnectionParams=");
        stringBuilder.append(this.mConnectionParams);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDisconnectParams=");
        stringBuilder.append(this.mDisconnectParams);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDcFailCause=");
        stringBuilder.append(this.mDcFailCause);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mPhone=");
        stringBuilder.append(this.mPhone);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mLinkProperties=");
        stringBuilder.append(this.mLinkProperties);
        pw.println(stringBuilder.toString());
        pw.flush();
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDataRegState=");
        stringBuilder.append(this.mDataRegState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mRilRat=");
        stringBuilder.append(this.mRilRat);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNetworkCapabilities=");
        stringBuilder.append(getNetworkCapabilities());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCreateTime=");
        stringBuilder.append(TimeUtils.logTimeOfDay(this.mCreateTime));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mLastFailTime=");
        stringBuilder.append(TimeUtils.logTimeOfDay(this.mLastFailTime));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mLastFailCause=");
        stringBuilder.append(this.mLastFailCause);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mUserData=");
        stringBuilder.append(this.mUserData);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mSubscriptionOverride=");
        stringBuilder.append(Integer.toHexString(this.mSubscriptionOverride));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mInstanceNumber=");
        stringBuilder.append(mInstanceNumber);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAc=");
        stringBuilder.append(this.mAc);
        pw.println(stringBuilder.toString());
        pw.println("Network capabilities changed history:");
        pw.increaseIndent();
        this.mNetCapsLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.println();
        pw.flush();
    }

    private void doChrCheckForResumeLink(LinkProperties lastLp, LinkProperties resumingLp) {
        byte equalsToResumingLP = (byte) 0;
        LinkProperties existedDcLp = ((ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity")).getLinkProperties(0);
        if (existedDcLp != null) {
            byte equalsToLastLP = existedDcLp.equals(lastLp) ? (byte) 1 : (byte) 0;
            if (existedDcLp.equals(resumingLp)) {
                equalsToResumingLP = (byte) 1;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CHR FAULT POINT in EVENT_RESUME_LINK, vaules = (");
            stringBuilder.append(equalsToLastLP);
            stringBuilder.append(",");
            stringBuilder.append(equalsToResumingLP);
            stringBuilder.append(")");
            logw(stringBuilder.toString());
            Bundle data = new Bundle();
            data.putString("EventScenario", Scenario.RESUME_LINK_FAULT);
            data.putInt("EventFailCause", 1002);
            data.putByte("DATACONN.RESUMELINKFAULT.equalsToLastLP", equalsToLastLP);
            data.putByte("DATACONN.RESUMELINKFAULT.equalsToResumingLP", equalsToResumingLP);
            HwTelephonyFactory.getHwTelephonyChrManager().sendTelephonyChrBroadcast(data);
        }
    }
}
