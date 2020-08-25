package com.android.server.hidata.mplink;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.HwInnerNetworkManagerImpl;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.ServiceState;
import com.android.internal.telephony.IPhoneCallback;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.hidata.HwHidataJniAdapter;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.arbitration.IHiDataCHRCallBack;
import huawei.android.net.hwmplink.MpLinkCommonUtils;
import java.net.InetAddress;
import libcore.net.event.NetworkEventDispatcher;

public class HwMpLinkServiceImpl extends StateMachine implements IMpLinkStateObserverCallback {
    private static final String DATA_BANDWIDTH_FIELD = "ulbw";
    private static final String DATA_FREQUENCY_FIELD = "ulfreq";
    private static final String DEFAULT_CLASS_NAME = "HwMpLinkServiceImpl";
    private static final String DEFAULT_PACKAGE_FIELD = "android";
    public static final int DEFAULT_SLOT_ID = 0;
    private static final int DEFAULT_SOCKET_STRATEGY = 3;
    private static final int DEFAULT_VALUE = 2;
    private static final int DELAY_IN_MS = 30000;
    public static final int ILLEGAL_VALUE = -1;
    private static final boolean INTER_DISTURB_CHECK_FOR_ALL = SystemProperties.getBoolean("ro.config.check_disturb_always", false);
    private static final String MPLINK_CLOSE_TYPE_KEY = "mplink_close_type";
    public static final int MPLINK_MSG_AIDEVICE_MPLINK_CLOSE = 218;
    public static final int MPLINK_MSG_AIDEVICE_MPLINK_OPEN = 217;
    public static final int MPLINK_MSG_BASE = 200;
    public static final int MPLINK_MSG_DATA_ROAMING_OFF = 203;
    public static final int MPLINK_MSG_DATA_ROAMING_ON = 204;
    public static final int MPLINK_MSG_DATA_SUB_CHANGE = 230;
    public static final int MPLINK_MSG_DATA_SUITABLE_OFF = 202;
    public static final int MPLINK_MSG_DATA_SUITABLE_ON = 201;
    public static final int MPLINK_MSG_DEFAULT_NETWORK_CHANGE = 231;
    public static final int MPLINK_MSG_HIBRAIN_MPLINK_CLOSE = 212;
    public static final int MPLINK_MSG_HIBRAIN_MPLINK_OPEN = 211;
    public static final int MPLINK_MSG_MOBILE_DATA_AVAILABLE = 221;
    public static final int MPLINK_MSG_MOBILE_DATA_CONNECTED = 215;
    public static final int MPLINK_MSG_MOBILE_DATA_DISCONNECTED = 216;
    public static final int MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE = 220;
    public static final int MPLINK_MSG_MOBILE_DATA_SWITCH_OPEN = 219;
    public static final int MPLINK_MSG_MOBILE_SERVICE_IN = 205;
    public static final int MPLINK_MSG_MOBILE_SERVICE_OUT = 206;
    public static final int MPLINK_MSG_SET_UL_FREQ_REPORT_START = 1;
    public static final int MPLINK_MSG_SET_UL_FREQ_REPORT_STOP = 0;
    public static final int MPLINK_MSG_UPDTAE_UL_FREQ_INFO = 232;
    public static final int MPLINK_MSG_WIFIPRO_SWITCH_DISABLE = 210;
    public static final int MPLINK_MSG_WIFIPRO_SWITCH_ENABLE = 209;
    public static final int MPLINK_MSG_WIFI_CONNECTED = 213;
    public static final int MPLINK_MSG_WIFI_DISCONNECTED = 214;
    public static final int MPLINK_MSG_WIFI_VPN_CONNETED = 208;
    public static final int MPLINK_MSG_WIFI_VPN_DISCONNETED = 207;
    private static final String MPLINK_NETWORK_TYPE_KEY = "mplink_network_type";
    private static final int MPLK_SK_STRATEGY_CT = 1;
    private static final int MPLK_SK_STRATEGY_FURE = 4;
    private static final int MPLK_SK_STRATEGY_FUTE = 2;
    public static final int MSG_MPLINK_REQUEST_BIND_NETWORK = 2;
    public static final int MSG_MPLINK_REQUEST_UNBIND_NETWORK = 3;
    private static final int MSG_MPLINK_SETTINGS_STATE_CHANGE = 1;
    public static final int MSG_TEST_FOREGROUND_APP_LTE_HANDOVER_WIGI = 102;
    public static final int MSG_TEST_FOREGROUND_APP_WIFI_HANDOVER_LTE = 101;
    public static final int MSG_TEST_NET_COEXIST_LTE_PRIORITIZED = 104;
    public static final int MSG_TEST_NET_COEXIST_WIFI_PRIORITIZED = 103;
    private static final String REPORT_RATE_FIELD = "rat";
    private static final String TAG = "HiData_MpLinkServiceImpl";
    private static HwMpLinkServiceImpl mMpLinkServiceImpl;
    /* access modifiers changed from: private */
    public boolean isFrequencyUpdateOn = false;
    /* access modifiers changed from: private */
    public boolean isIgnoreDefaultChange = false;
    private boolean isInternalMpLinkEnable = false;
    private boolean isMpLinkSwitchEnable = false;
    private final AlarmManager mAlarmManager;
    private ConnectivityManager mConnectivityManager = null;
    private Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentBindUid = -1;
    private int mCurrentNetworkStrategy;
    private int mCurrentRequestBindNetWork = -1;
    private int mCurrentUnbindNetId = -1;
    private int mFreqReportSub = -1;
    private IHiDataCHRCallBack mHiDataChrCallBack = null;
    private HwInnerNetworkManagerImpl mHwInnerNetworkManagerImpl;
    /* access modifiers changed from: private */
    public HwMpLinkChrImpl mHwMpLinkChrImpl = null;
    /* access modifiers changed from: private */
    public HwMpLinkContentAware mHwMpLinkContentAware;
    private HwMpLinkDemoMode mHwMpLinkDemoMode;
    private HwMpLinkNetworkImpl mHwMpLinkNetworkImpl;
    private HwMpLinkStateObserver mHwMpLinkStateObserver;
    /* access modifiers changed from: private */
    public HwMpLinkTelephonyImpl mHwMpLinkTelephonyImpl;
    /* access modifiers changed from: private */
    public HwMpLinkWifiImpl mHwMpLinkWifiImpl;
    /* access modifiers changed from: private */
    public State mInitState = new InitState();
    /* access modifiers changed from: private */
    public State mMpLinkBaseState = new MpLinkBaseState();
    /* access modifiers changed from: private */
    public MpLinkBindResultInfo mMpLinkBindResultInfo = new MpLinkBindResultInfo();
    /* access modifiers changed from: private */
    public IMpLinkCallback mMpLinkCallback;
    private int mMpLinkConditionState = -1;
    private int mMpLinkNotifyControl = -1;
    private MpLinkNetworkResultInfo mMpLinkNri = new MpLinkNetworkResultInfo();
    /* access modifiers changed from: private */
    public State mMpLinkedState = new MpLinkedState();
    IPhoneCallback mMplinkIPhoneCallback = null;
    private PendingIntent mRequestCheckAlarmIntent = null;

    private HwMpLinkServiceImpl(Context context) {
        super(DEFAULT_CLASS_NAME);
        this.mContext = context;
        addState(this.mInitState);
        addState(this.mMpLinkBaseState, this.mInitState);
        addState(this.mMpLinkedState, this.mMpLinkBaseState);
        setInitialState(this.mInitState);
        start();
        if (MpLinkCommonUtils.isMpLinkTestMode()) {
            this.mHwMpLinkDemoMode = new HwMpLinkDemoMode(this.mContext, getHandler());
        }
        this.mHwMpLinkChrImpl = HwMpLinkChrImpl.getInstance(this.mContext);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mHwMpLinkContentAware = HwMpLinkContentAware.getInstance(context);
        this.mHwMpLinkContentAware.registerMpLinkHandler(getHandler());
        this.mHwMpLinkTelephonyImpl = new HwMpLinkTelephonyImpl(this.mContext, getHandler());
        this.mHwMpLinkWifiImpl = new HwMpLinkWifiImpl(this.mContext, getHandler());
        this.mHwMpLinkStateObserver = new HwMpLinkStateObserver(this.mContext, this);
        this.mHwMpLinkNetworkImpl = new HwMpLinkNetworkImpl(context);
        this.mHwInnerNetworkManagerImpl = HwFrameworkFactory.getHwInnerNetworkManager();
        this.mHwMpLinkWifiImpl.setCurrentWifiVpnState(this.mHwMpLinkStateObserver.getVpnConnectState());
        this.isMpLinkSwitchEnable = this.mHwMpLinkStateObserver.getMpLinkSwitchState();
        setMpLinkConditionDb(200);
        this.mHwMpLinkStateObserver.initSimulateHibrain();
        this.mMplinkIPhoneCallback = new IPhoneCallback.Stub() {
            /* class com.android.server.hidata.mplink.HwMpLinkServiceImpl.AnonymousClass1 */

            public void onCallback3(int param1, int param2, Bundle param3) {
                if (param3 == null || !HwMpLinkServiceImpl.this.isFrequencyUpdateOn) {
                    MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "mMplinkIPhoneCallback onCallback3 param3 is null or isFrequencyUpdateOn = %{public}s", new Object[]{String.valueOf(HwMpLinkServiceImpl.this.isFrequencyUpdateOn)});
                    return;
                }
                MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "mMplinkIPhoneCallback call back received", new Object[0]);
                HwMpLinkInterDisturbInfo disturbInfo = new HwMpLinkInterDisturbInfo();
                disturbInfo.mRat = param3.getInt(HwMpLinkServiceImpl.REPORT_RATE_FIELD);
                disturbInfo.mUlfreq = param3.getInt(HwMpLinkServiceImpl.DATA_FREQUENCY_FIELD);
                disturbInfo.mUlbw = param3.getInt(HwMpLinkServiceImpl.DATA_BANDWIDTH_FIELD);
                HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.upDataCellUlFreqInfo(disturbInfo);
                HwMpLinkServiceImpl.this.getHandler().sendMessage(HwMpLinkServiceImpl.this.obtainMessage(HwMpLinkServiceImpl.MPLINK_MSG_UPDTAE_UL_FREQ_INFO));
            }

            public void onCallback1(int parm) throws RemoteException {
            }

            public void onCallback2(int parm1, int param2) throws RemoteException {
            }
        };
        MpLinkCommonUtils.logD(TAG, false, "HwMpLinkServiceImpl complete", new Object[0]);
    }

    public static HwMpLinkServiceImpl getInstance(Context context) {
        if (mMpLinkServiceImpl == null) {
            mMpLinkServiceImpl = new HwMpLinkServiceImpl(context);
        }
        return mMpLinkServiceImpl;
    }

    class InitState extends State {
        InitState() {
        }

        public void enter() {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "Enter InitState", new Object[0]);
        }

        public void exit() {
            MpLinkCommonUtils.logI(HwMpLinkServiceImpl.TAG, false, "Exit InitState", new Object[0]);
        }

        public boolean processMessage(Message message) {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "InitState,msg=%{public}d", new Object[]{Integer.valueOf(message.what)});
            int i = message.what;
            if (i != 103) {
                if (i != 215) {
                    switch (i) {
                        case 211:
                            if (HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.isWifiConnected()) {
                                if (HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isMobileConnected()) {
                                    HwMpLinkServiceImpl.this.notifyNetCoexistFailed(15);
                                    break;
                                } else {
                                    HwMpLinkServiceImpl.this.notifyNetCoexistFailed(9);
                                    break;
                                }
                            } else {
                                HwMpLinkServiceImpl.this.notifyNetCoexistFailed(1);
                                break;
                            }
                        case HwMpLinkServiceImpl.MPLINK_MSG_HIBRAIN_MPLINK_CLOSE /*{ENCODED_INT: 212}*/:
                            HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.closeMobileDataIfOpened();
                            HwMpLinkServiceImpl.this.notifyNetCoexistFailed(true);
                            break;
                        case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_CONNECTED /*{ENCODED_INT: 213}*/:
                            break;
                        default:
                            return true;
                    }
                }
                if (message.what == 213) {
                    HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.updateWifiLcfInfo(HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiFreq, HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiBandWidth);
                }
                HwMpLinkServiceImpl hwMpLinkServiceImpl = HwMpLinkServiceImpl.this;
                hwMpLinkServiceImpl.transitionTo(hwMpLinkServiceImpl.mMpLinkBaseState);
            } else if (message.arg1 == 0) {
                HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(true);
            } else if (message.arg1 == 1) {
                HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(false);
            }
            return true;
        }
    }

    class MpLinkBaseState extends State {
        MpLinkBaseState() {
        }

        public void enter() {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "Enter MpLinkBaseState", new Object[0]);
        }

        public void exit() {
            MpLinkCommonUtils.logI(HwMpLinkServiceImpl.TAG, false, "Exit MpLinkBaseState", new Object[0]);
        }

        public boolean processMessage(Message message) {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "MpLinkBaseState,msg=%{public}d", new Object[]{Integer.valueOf(message.what)});
            int i = message.what;
            if (i == 2) {
                requestBindNetwork(message);
            } else if (i == 3) {
                HwMpLinkServiceImpl.this.handleClearBindProcessToNetwork(0, message.arg2);
            } else if (i == 103) {
                judgeNetworkCoexist(message);
            } else if (i != 232) {
                switch (i) {
                    case 211:
                        HwMpLinkServiceImpl.this.openMpLinkNetworkCoexist();
                        break;
                    case HwMpLinkServiceImpl.MPLINK_MSG_HIBRAIN_MPLINK_CLOSE /*{ENCODED_INT: 212}*/:
                        HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.closeMobileDataIfOpened();
                        HwMpLinkServiceImpl.this.notifyNetCoexistFailed(true);
                        break;
                    case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_CONNECTED /*{ENCODED_INT: 213}*/:
                        updateWifiConnectedState();
                        break;
                    case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED /*{ENCODED_INT: 214}*/:
                        updateWifiDisconnectedState();
                        break;
                    case HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_CONNECTED /*{ENCODED_INT: 215}*/:
                        if (HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.isWifiConnected()) {
                            HwMpLinkServiceImpl.this.mHwMpLinkChrImpl.updateDualNetworkCnt();
                            HwMpLinkServiceImpl hwMpLinkServiceImpl = HwMpLinkServiceImpl.this;
                            hwMpLinkServiceImpl.transitionTo(hwMpLinkServiceImpl.mMpLinkedState);
                            break;
                        }
                        break;
                    case HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_DISCONNECTED /*{ENCODED_INT: 216}*/:
                        if (!HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.isWifiConnected()) {
                            HwMpLinkServiceImpl hwMpLinkServiceImpl2 = HwMpLinkServiceImpl.this;
                            hwMpLinkServiceImpl2.transitionTo(hwMpLinkServiceImpl2.mInitState);
                            break;
                        }
                        break;
                    case HwMpLinkServiceImpl.MPLINK_MSG_AIDEVICE_MPLINK_OPEN /*{ENCODED_INT: 217}*/:
                        HwMpLinkServiceImpl.this.mHwMpLinkChrImpl.updateAiDeviceOpenCnt(HwMpLinkServiceImpl.this.mHwMpLinkContentAware.getCurrentApName());
                        break;
                    default:
                        return true;
                }
            } else {
                updateFrequencyInfo(message);
            }
            return true;
        }

        private void updateWifiConnectedState() {
            HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.updateWifiLcfInfo(HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiFreq, HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiBandWidth);
            if (HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isMobileConnected()) {
                boolean unused = HwMpLinkServiceImpl.this.isIgnoreDefaultChange = true;
                HwMpLinkServiceImpl hwMpLinkServiceImpl = HwMpLinkServiceImpl.this;
                hwMpLinkServiceImpl.transitionTo(hwMpLinkServiceImpl.mMpLinkedState);
            }
        }

        private void updateWifiDisconnectedState() {
            HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.updateWifiLcfInfo(HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiFreq, HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiBandWidth);
            if (!HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isMobileConnected()) {
                HwMpLinkServiceImpl hwMpLinkServiceImpl = HwMpLinkServiceImpl.this;
                hwMpLinkServiceImpl.transitionTo(hwMpLinkServiceImpl.mInitState);
            }
        }

        private void judgeNetworkCoexist(Message message) {
            if (message.arg1 == 0) {
                HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(true);
            } else if (message.arg1 == 1) {
                HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(false);
            }
        }

        private void requestBindNetwork(Message message) {
            HwMpLinkServiceImpl.this.mMpLinkBindResultInfo.reset();
            HwMpLinkServiceImpl.this.mMpLinkBindResultInfo.setNetwork(message.arg1);
            HwMpLinkServiceImpl.this.mMpLinkBindResultInfo.setUid(message.arg2);
            HwMpLinkServiceImpl.this.mMpLinkBindResultInfo.setFailReason(101);
            HwMpLinkServiceImpl.this.mMpLinkBindResultInfo.setResult(4);
            if (HwMpLinkServiceImpl.this.mMpLinkCallback != null) {
                HwMpLinkServiceImpl.this.mMpLinkCallback.onBindProcessToNetworkResult(HwMpLinkServiceImpl.this.mMpLinkBindResultInfo);
            }
        }

        private void updateFrequencyInfo(Message message) {
            if (HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isFreqInterDisturbExist()) {
                MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "MpLinkBaseState,Freq Inter disturb between wifi and Cell", new Object[0]);
                HwMpLinkServiceImpl.this.notifyNetCoexistFailed(MpLinkNetworkResultInfo.messageToFailReason(message.what));
                HwMpLinkServiceImpl.this.mHwMpLinkChrImpl.updateInterDisturbHappenedTime();
                return;
            }
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "MpLinkBaseState,No Freq Interdisturb happened", new Object[0]);
            HwMpLinkServiceImpl.this.mHwMpLinkChrImpl.updateOpenMobileDataStamp();
            HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.mpLinkSetMobileData(true);
            HwMpLinkServiceImpl.this.mHwMpLinkChrImpl.updateNoInterDisturbHappenedTime();
        }
    }

    class MpLinkedState extends State {
        MpLinkedState() {
        }

        public void enter() {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "Enter MpLinkedState", new Object[0]);
            if (HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isMobileDataAvailable()) {
                HwMpLinkServiceImpl.this.notifyNetCoexistSuccess();
            }
        }

        public void exit() {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "Exit MpLinkedState", new Object[0]);
            int unused = HwMpLinkServiceImpl.this.mCurrentBindUid = -1;
        }

        /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
        public boolean processMessage(Message message) {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, false, "MpLinkedState,msg=%{public}d", new Object[]{Integer.valueOf(message.what)});
            int i = message.what;
            if (i == 2) {
                HwMpLinkServiceImpl.this.handleBindProcessToNetwork(message.arg1, message.arg2, (MpLinkQuickSwitchConfiguration) message.obj);
            } else if (i != 3) {
                if (!(i == 202 || i == 204 || i == 206 || i == 208)) {
                    if (i == 216) {
                        HwMpLinkServiceImpl.this.notifyNetCoexistFailed(9);
                        HwMpLinkServiceImpl.this.closeMpLinkNetworkCoexist();
                        HwMpLinkServiceImpl hwMpLinkServiceImpl = HwMpLinkServiceImpl.this;
                        hwMpLinkServiceImpl.transitionTo(hwMpLinkServiceImpl.mMpLinkBaseState);
                    } else if (i == 217) {
                        HwMpLinkServiceImpl.this.mHwMpLinkChrImpl.updateAiDeviceOpenCnt(HwMpLinkServiceImpl.this.mHwMpLinkContentAware.getCurrentApName());
                    } else if (i != 220) {
                        if (i != 221) {
                            switch (i) {
                                case 101:
                                case 102:
                                    HwMpLinkServiceImpl.this.handleBindProcessToNetwork(message.arg1, message.arg2, new MpLinkQuickSwitchConfiguration(3, 0));
                                    break;
                                case 103:
                                    judgeNetworkCoexist(message);
                                    break;
                                default:
                                    switch (i) {
                                        case 210:
                                        case HwMpLinkServiceImpl.MPLINK_MSG_HIBRAIN_MPLINK_CLOSE /*{ENCODED_INT: 212}*/:
                                            break;
                                        case 211:
                                            break;
                                        case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_CONNECTED /*{ENCODED_INT: 213}*/:
                                            updateWifiConnectedState();
                                            break;
                                        case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED /*{ENCODED_INT: 214}*/:
                                            updateWifiDisconnectedState();
                                            break;
                                        default:
                                            switch (i) {
                                                case HwMpLinkServiceImpl.MPLINK_MSG_DATA_SUB_CHANGE /*{ENCODED_INT: 230}*/:
                                                    break;
                                                case HwMpLinkServiceImpl.MPLINK_MSG_DEFAULT_NETWORK_CHANGE /*{ENCODED_INT: 231}*/:
                                                    updateDefaultNetworkChange();
                                                    break;
                                                case HwMpLinkServiceImpl.MPLINK_MSG_UPDTAE_UL_FREQ_INFO /*{ENCODED_INT: 232}*/:
                                                    updateFrequencyInfo();
                                                    break;
                                                default:
                                                    return true;
                                            }
                                    }
                            }
                        }
                        HwMpLinkServiceImpl.this.notifyNetCoexistSuccess();
                    } else {
                        HwMpLinkServiceImpl.this.mHwMpLinkChrImpl.updateCoexistMobileDataSwitchClosedCnt();
                    }
                }
                HwMpLinkServiceImpl.this.closeMpLinkNetworkCoexist();
            } else {
                HwMpLinkServiceImpl.this.handleClearBindProcessToNetwork(0, message.arg2);
            }
            return true;
        }

        private void updateWifiDisconnectedState() {
            HwMpLinkServiceImpl.this.notifyNetCoexistFailed(1);
            HwMpLinkServiceImpl.this.mHwMpLinkChrImpl.updateCoexistWifiSwitchClosedCnt();
            HwMpLinkServiceImpl.this.closeMpLinkNetworkCoexist();
            HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.updateWifiLcfInfo(HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiFreq, HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiBandWidth);
            HwMpLinkServiceImpl hwMpLinkServiceImpl = HwMpLinkServiceImpl.this;
            hwMpLinkServiceImpl.transitionTo(hwMpLinkServiceImpl.mMpLinkBaseState);
        }

        private void updateWifiConnectedState() {
            HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.updateWifiLcfInfo(HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiFreq, HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.mCurrentWifiBandWidth);
            HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.calculateInterDisturb();
            if (HwMpLinkServiceImpl.this.isFrequencyUpdateOn && HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isFreqInterDisturbExist()) {
                HwMpLinkServiceImpl.this.configInterDisturbDetectReport(0);
                HwMpLinkServiceImpl.this.closeMpLinkNetworkCoexist();
            }
        }

        private void updateDefaultNetworkChange() {
            if (!HwMpLinkServiceImpl.this.isIgnoreDefaultChange) {
                HwMpLinkServiceImpl.this.mHwMpLinkChrImpl.updateDefaultRouteChangeCnt();
                boolean unused = HwMpLinkServiceImpl.this.isIgnoreDefaultChange = false;
            }
        }

        private void updateFrequencyInfo() {
            if (HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isFreqInterDisturbExist()) {
                HwMpLinkServiceImpl.this.configInterDisturbDetectReport(0);
                HwMpLinkServiceImpl.this.closeMpLinkNetworkCoexist();
            }
        }

        private void judgeNetworkCoexist(Message message) {
            if (message.arg1 == 0) {
                HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(true);
            } else if (message.arg1 == 1) {
                HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(false);
            }
        }
    }

    private void getConnectivityManger() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    private void setMpLinkConditionDb(int type) {
        if (type >= 200 && type <= 220) {
            int value = 0;
            if (checkMpLinkSuitableBeOpen() == 0) {
                value = 1;
            }
            if (this.mMpLinkConditionState != value) {
                MpLinkCommonUtils.logD(TAG, false, "setMpLinkConditionDb value:%{public}d", new Object[]{Integer.valueOf(value)});
                Settings.System.putInt(this.mContext.getContentResolver(), "mplink_db_condition_value", value);
                this.mMpLinkConditionState = value;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void stopInterDisturbDetectReport() {
        MpLinkCommonUtils.logD(TAG, false, "stopInterDisturbDetectReport Enter: isFrequencyUpdateOn = %{public}s", new Object[]{String.valueOf(this.isFrequencyUpdateOn)});
        if (this.isFrequencyUpdateOn) {
            configInterDisturbDetectReportToRil(this.mFreqReportSub, 0);
            this.isFrequencyUpdateOn = false;
            this.mFreqReportSub = -1;
        }
    }

    public void configInterDisturbDetectReportToRil(int dataSub, int status) {
        if (status == 1) {
            this.isFrequencyUpdateOn = true;
        }
        boolean isSet = HwTelephonyManagerInner.getDefault().setUplinkFreqBandwidthReportState(dataSub, status, this.mMplinkIPhoneCallback);
        MpLinkCommonUtils.logD(TAG, false, "configInterDisturbDetectReportToRil Enter: dataSub = %{public}d, status = %{public}d, setUlFreqBandwidthReport ret = %{public}s", new Object[]{Integer.valueOf(dataSub), Integer.valueOf(status), String.valueOf(isSet)});
        if (!isSet) {
            this.isFrequencyUpdateOn = false;
            this.mHwMpLinkChrImpl.updateInterDisturbCheckFailedTime();
            notifyNetCoexistFailed(11);
        }
    }

    public void configInterDisturbDetectReport(int status) {
        MpLinkCommonUtils.logD(TAG, false, "configInterDisturbDetectReport Enter", new Object[0]);
        this.mFreqReportSub = this.mHwMpLinkTelephonyImpl.getDefaultDataSubId();
        configInterDisturbDetectReportToRil(this.mFreqReportSub, status);
    }

    private int checkMpLinkSuitableBeOpen() {
        int failReason = 0;
        if (!this.isMpLinkSwitchEnable) {
            failReason = 8;
        } else if (this.mHwMpLinkContentAware.isAiDevice()) {
            failReason = 0;
        } else if (this.mHwMpLinkTelephonyImpl.getCurrentDataRoamingState()) {
            failReason = 4;
        } else if (!this.isInternalMpLinkEnable) {
            failReason = 7;
        } else if (!this.mHwMpLinkTelephonyImpl.isMobileDataEnable()) {
            failReason = 2;
        } else if (this.mHwMpLinkWifiImpl.getCurrentWifiVpnState()) {
            failReason = 6;
        }
        MpLinkCommonUtils.logI(TAG, false, "checkMpLinkSuitableBeOpen ret:%{public}d", new Object[]{Integer.valueOf(failReason)});
        return failReason;
    }

    /* access modifiers changed from: private */
    public void notifyNetCoexistSuccess() {
        this.mMpLinkNri.reset();
        this.mMpLinkNri.setFailReason(0);
        this.mMpLinkNri.setResult(100);
        this.mHwMpLinkTelephonyImpl.setDealMobileDataRef(true);
        this.mHwMpLinkChrImpl.updateMobileDataConnectedStamp();
        if (!(this.mMpLinkCallback == null || this.mMpLinkNotifyControl == 1)) {
            MpLinkCommonUtils.logD(TAG, false, "network coexist successful", new Object[0]);
            this.mMpLinkCallback.onWiFiAndCellCoexistResult(this.mMpLinkNri);
            this.mMpLinkNotifyControl = 1;
        }
        recordRequestResult(this.mMpLinkNri);
    }

    /* access modifiers changed from: private */
    public void notifyNetCoexistFailed(boolean isNotify) {
        notifyNetCoexistFailed(0, isNotify);
    }

    /* access modifiers changed from: private */
    public void notifyNetCoexistFailed(int reason) {
        notifyNetCoexistFailed(reason, false);
    }

    private void notifyNetCoexistFailed(int reason, boolean isNotified) {
        this.mMpLinkNri.reset();
        this.mMpLinkNri.setFailReason(reason);
        this.mMpLinkNri.setResult(101);
        stopInterDisturbDetectReport();
        if (this.mMpLinkCallback != null) {
            if (this.mMpLinkNotifyControl != 0) {
                MpLinkCommonUtils.logD(TAG, false, "network coexist failed,reason:%{public}d", new Object[]{Integer.valueOf(reason)});
                this.mMpLinkCallback.onWiFiAndCellCoexistResult(this.mMpLinkNri);
                this.mMpLinkNotifyControl = 0;
            } else if (isNotified) {
                MpLinkCommonUtils.logD(TAG, false, "force notify network coexist failed,reason:%{public}d", new Object[]{Integer.valueOf(reason)});
                this.mMpLinkCallback.onWiFiAndCellCoexistResult(this.mMpLinkNri);
            }
        }
        recordRequestResult(this.mMpLinkNri);
    }

    /* access modifiers changed from: private */
    public void openMpLinkNetworkCoexist() {
        int reason = checkMpLinkSuitableBeOpen();
        if (reason != 0) {
            notifyNetCoexistFailed(reason);
        } else if (INTER_DISTURB_CHECK_FOR_ALL) {
            MpLinkCommonUtils.logD(TAG, false, "openMpLinkNetworkCoexist: Trigger Inter Disturb Detect", new Object[0]);
            configInterDisturbDetectReport(1);
            this.mHwMpLinkChrImpl.updateInterDisturbCheckTriggerTime();
        } else {
            MpLinkCommonUtils.logD(TAG, false, "openMpLinkNetworkCoexist: Not trigger Inter disturb Detect, Mp-Link trigger Open Mobile Data", new Object[0]);
            this.mHwMpLinkChrImpl.updateOpenMobileDataStamp();
            this.mHwMpLinkTelephonyImpl.mpLinkSetMobileData(true);
        }
    }

    /* access modifiers changed from: private */
    public void closeMpLinkNetworkCoexist() {
        MpLinkCommonUtils.logD(TAG, false, "request close mplink network coexist", new Object[0]);
        stopInterDisturbDetectReport();
        this.mHwMpLinkTelephonyImpl.mpLinkSetMobileData(false);
        this.isInternalMpLinkEnable = false;
    }

    public boolean isMpLinkConditionSatisfy() {
        boolean isSatisfied = false;
        if (checkMpLinkSuitableBeOpen() == 0) {
            isSatisfied = true;
        }
        MpLinkCommonUtils.logI(TAG, false, "isMpLinkConditionSatisfy return:%{public}s", new Object[]{String.valueOf(isSatisfied)});
        return isSatisfied;
    }

    public void notifyIpConfigCompleted() {
    }

    public void foregroundAppChanged(int uid) {
        if (this.mHwMpLinkContentAware.isWifiLanApp(uid)) {
            MpLinkCommonUtils.logD(TAG, false, "isWifiLanApp", new Object[0]);
        }
    }

    public void registerMpLinkCallback(IMpLinkCallback callback) {
        if (this.mMpLinkCallback == null) {
            this.mMpLinkCallback = callback;
        }
    }

    public void registerMpLinkChrCallback(IHiDataCHRCallBack callback) {
        if (this.mHiDataChrCallBack == null) {
            this.mHiDataChrCallBack = callback;
        }
    }

    public void registerRfInterferenceCallback(IRFInterferenceCallback callback) {
        MpLinkCommonUtils.logD(TAG, false, "registerRfInterferenceCallback", new Object[0]);
    }

    private void startRequestSuccessCheck(int requestType) {
        MpLinkCommonUtils.logD(TAG, false, "start a timer to check the operation result", new Object[0]);
        stopRequestSuccessCheck();
        Intent intent = new Intent("mplink_intent_check_request_success");
        intent.setPackage("android");
        intent.putExtra("mplink_intent_key_check_request", requestType);
        this.mRequestCheckAlarmIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728);
        this.mAlarmManager.setExact(3, SystemClock.elapsedRealtime() + HwArbitrationDEFS.DelayTimeMillisA, this.mRequestCheckAlarmIntent);
    }

    private void stopRequestSuccessCheck() {
        PendingIntent pendingIntent = this.mRequestCheckAlarmIntent;
        if (pendingIntent != null) {
            this.mAlarmManager.cancel(pendingIntent);
            this.mRequestCheckAlarmIntent = null;
            MpLinkCommonUtils.logD(TAG, false, "stop timer and release intent", new Object[0]);
        }
    }

    private void recordRequestResult(MpLinkNetworkResultInfo resultInfo) {
        PendingIntent pendingIntent = this.mRequestCheckAlarmIntent;
        if (pendingIntent != null) {
            int type = pendingIntent.getIntent().getIntExtra("mplink_intent_key_check_request", -1);
            int result = resultInfo.getResult();
            int reason = resultInfo.getFailReason();
            MpLinkCommonUtils.logD(TAG, false, "recordRequestResult:%{public}d,%{public}d,%{public}d", new Object[]{Integer.valueOf(type), Integer.valueOf(result), Integer.valueOf(reason)});
            if (type == 211) {
                if (result == 101) {
                    this.mHwMpLinkChrImpl.updateOpenFailCnt(reason);
                    stopRequestSuccessCheck();
                } else if (result == 100) {
                    this.mHwMpLinkChrImpl.updateOpenSuccCnt();
                    stopRequestSuccessCheck();
                }
            } else if (type == 212) {
                if (result == 101) {
                    this.mHwMpLinkChrImpl.updateCloseSuccCnt();
                    stopRequestSuccessCheck();
                } else if (result == 100) {
                    this.mHwMpLinkChrImpl.updateCloseFailCnt(reason);
                    stopRequestSuccessCheck();
                }
                this.mHwMpLinkChrImpl.sendDataToChr(this.mHiDataChrCallBack);
            }
        } else {
            MpLinkCommonUtils.logD(TAG, false, "mRequestCheckAlarmIntent is null, do nothing", new Object[0]);
        }
    }

    public void requestWiFiAndCellCoexist(boolean isCoexisted) {
        MpLinkCommonUtils.logD(TAG, false, "requestCoexist, coexist:%{public}s,internal:%{public}s,Control:%{public}d", new Object[]{String.valueOf(isCoexisted), String.valueOf(this.isInternalMpLinkEnable), Integer.valueOf(this.mMpLinkNotifyControl)});
        if (this.mMpLinkCallback == null) {
            MpLinkCommonUtils.logD(TAG, false, "callback is null", new Object[0]);
        } else if (this.isInternalMpLinkEnable != isCoexisted) {
            this.isInternalMpLinkEnable = isCoexisted;
            this.mMpLinkNotifyControl = -1;
            if (isCoexisted) {
                startRequestSuccessCheck(211);
                sendMessage(211);
                return;
            }
            startRequestSuccessCheck(MPLINK_MSG_HIBRAIN_MPLINK_CLOSE);
            sendMessage(MPLINK_MSG_HIBRAIN_MPLINK_CLOSE);
        } else {
            MpLinkCommonUtils.logD(TAG, false, "dup request", new Object[0]);
            if (!isCoexisted && !this.isInternalMpLinkEnable) {
                startRequestSuccessCheck(MPLINK_MSG_HIBRAIN_MPLINK_CLOSE);
                sendMessage(MPLINK_MSG_HIBRAIN_MPLINK_CLOSE);
            } else if (this.mRequestCheckAlarmIntent == null) {
                MpLinkCommonUtils.logD(TAG, false, "response for dup request", new Object[0]);
                this.mMpLinkCallback.onWiFiAndCellCoexistResult(this.mMpLinkNri);
            }
        }
    }

    public void updateMpLinkAiDevicesList(int type, String packageWhiteList) {
    }

    @Override // com.android.server.hidata.mplink.IMpLinkStateObserverCallback
    public void onMpLinkRequestTimeout(int requestType) {
        MpLinkCommonUtils.logD(TAG, false, "onMpLinkRequestTimeout, type:%{public}d", new Object[]{Integer.valueOf(requestType)});
        notifyNetCoexistFailed(10, true);
    }

    @Override // com.android.server.hidata.mplink.IMpLinkStateObserverCallback
    public void onTelephonyServiceStateChanged(ServiceState serviceState, int subId) {
        this.mHwMpLinkTelephonyImpl.handleTelephonyServiceStateChanged(serviceState, subId);
    }

    @Override // com.android.server.hidata.mplink.IMpLinkStateObserverCallback
    public void onTelephonyDefaultDataSubChanged(int newDataSub) {
        this.mHwMpLinkTelephonyImpl.handleDataSubChange(newDataSub);
    }

    @Override // com.android.server.hidata.mplink.IMpLinkStateObserverCallback
    public void onTelephonyDataConnectionChanged(String state, String iface, int subId) {
        this.mHwMpLinkTelephonyImpl.handleTelephonyDataConnectionChanged(state, iface, subId);
    }

    @Override // com.android.server.hidata.mplink.IMpLinkStateObserverCallback
    public void onMobileDataSwitchChange(boolean isEnabled) {
        this.mHwMpLinkTelephonyImpl.handleMobileDataSwitchChange(isEnabled);
    }

    @Override // com.android.server.hidata.mplink.IMpLinkStateObserverCallback
    public void onWifiNetworkStateChanged(NetworkInfo netInfo) {
        this.mHwMpLinkWifiImpl.handleWifiNetworkStateChanged(netInfo);
    }

    @Override // com.android.server.hidata.mplink.IMpLinkStateObserverCallback
    public void onVpnStateChange(boolean isVpnConnected) {
        this.mHwMpLinkWifiImpl.handleVpnStateChange(isVpnConnected);
    }

    @Override // com.android.server.hidata.mplink.IMpLinkStateObserverCallback
    public void onMpLinkSwitchChange(boolean isMpLinkEnabled) {
        if (this.isMpLinkSwitchEnable != isMpLinkEnabled) {
            this.isMpLinkSwitchEnable = isMpLinkEnabled;
            if (isMpLinkEnabled) {
                sendMessage(MPLINK_MSG_WIFIPRO_SWITCH_ENABLE);
            } else {
                sendMessage(210);
            }
        }
    }

    @Override // com.android.server.hidata.mplink.IMpLinkStateObserverCallback
    public void onSimulateHiBrainRequestForDemo(boolean isEnabled) {
        MpLinkCommonUtils.logD(TAG, false, "onSimulateHiBrainRequestForTest:%{public}s", new Object[]{String.valueOf(isEnabled)});
        requestWiFiAndCellCoexist(isEnabled);
    }

    public void requestBindProcessToNetwork(int network, int uid, int type) {
        MpLinkCommonUtils.logD(TAG, false, "bindProcessToNetwork network:%{public}d, uid:%{public}d", new Object[]{Integer.valueOf(network), Integer.valueOf(uid)});
        requestBindProcessToNetwork(network, uid, (MpLinkQuickSwitchConfiguration) null);
    }

    public void requestBindProcessToNetwork(int netId, int uid, MpLinkQuickSwitchConfiguration configuration) {
        if (configuration != null) {
            MpLinkCommonUtils.logD(TAG, false, "bindProcessToNetwork network:%{public}d, uid:%{public}d, configuration: %{public}s", new Object[]{Integer.valueOf(netId), Integer.valueOf(uid), configuration.toString()});
        } else {
            MpLinkCommonUtils.logD(TAG, false, "bindProcessToNetwork network:%{public}d, uid:%{public}d, configuration is null", new Object[]{Integer.valueOf(netId), Integer.valueOf(uid)});
        }
        sendMessage(2, netId, uid, configuration);
    }

    public void requestClearBindProcessToNetwork(int network, int uid) {
        MpLinkCommonUtils.logD(TAG, false, "clearBindProcessToNetwork network:%{public}d, uid:%{public}d", new Object[]{Integer.valueOf(network), Integer.valueOf(uid)});
        this.mCurrentUnbindNetId = network;
        sendMessage(3, 0, uid);
    }

    /* access modifiers changed from: private */
    public void handleBindProcessToNetwork(int network, int uid, MpLinkQuickSwitchConfiguration quickSwitchConfig) {
        this.mMpLinkBindResultInfo.reset();
        this.mMpLinkBindResultInfo.setNetwork(network);
        this.mMpLinkBindResultInfo.setUid(uid);
        if (quickSwitchConfig != null) {
            MpLinkCommonUtils.logD(TAG, false, "set bind type: " + quickSwitchConfig.getReason(), new Object[0]);
            this.mMpLinkBindResultInfo.setType(quickSwitchConfig.getReason());
        }
        this.mCurrentRequestBindNetWork = MpLinkCommonUtils.getNetworkType(this.mContext, network);
        int ret = bindProcessToNetwork(network, uid);
        if (ret == 0) {
            this.mCurrentBindUid = uid;
            InetAddress.clearDnsCache();
            NetworkEventDispatcher.getInstance().onNetworkConfigurationChanged();
            this.mMpLinkBindResultInfo.setResult(1);
            MpLinkCommonUtils.logD(TAG, false, "bind network successful !", new Object[0]);
            if (quickSwitchConfig == null) {
                MpLinkCommonUtils.logD(TAG, false, "quickSwitchConfig is null", new Object[0]);
                closeProcessSockets(0, uid);
                this.mHwMpLinkNetworkImpl.handleNetworkStrategy(0, uid);
            } else {
                MpLinkCommonUtils.logD(TAG, false, "quickSwitchConfig:%{public}s", new Object[]{quickSwitchConfig.toString()});
                this.mCurrentNetworkStrategy = quickSwitchConfig.getNetworkStrategy();
                handleSocketStrategy(quickSwitchConfig.getSocketStrategy(), uid);
                if (this.mHwMpLinkDemoMode != null) {
                    this.mCurrentNetworkStrategy = SystemProperties.getInt(MPLINK_NETWORK_TYPE_KEY, this.mCurrentNetworkStrategy);
                }
                MpLinkCommonUtils.logD(TAG, false, "network strategy:%{public}d, uid:%{public}d", new Object[]{Integer.valueOf(this.mCurrentNetworkStrategy), Integer.valueOf(uid)});
                this.mHwMpLinkNetworkImpl.handleNetworkStrategy(this.mCurrentNetworkStrategy, uid);
                if (this.mCurrentRequestBindNetWork == 0) {
                    this.mHwMpLinkChrImpl.updateMpLinkCellBindState(true, this.mHwMpLinkTelephonyImpl.getMobileIface());
                } else {
                    MpLinkCommonUtils.logD(TAG, false, "bind to wifi", new Object[0]);
                }
            }
            HwMpLinkDemoMode hwMpLinkDemoMode = this.mHwMpLinkDemoMode;
            if (hwMpLinkDemoMode != null) {
                hwMpLinkDemoMode.showToast("bind network successful !");
            }
        } else {
            MpLinkCommonUtils.logD(TAG, false, "bind network fail with err %{public}d", new Object[]{Integer.valueOf(ret)});
            this.mMpLinkBindResultInfo.setResult(2);
        }
        IMpLinkCallback iMpLinkCallback = this.mMpLinkCallback;
        if (iMpLinkCallback != null) {
            iMpLinkCallback.onBindProcessToNetworkResult(this.mMpLinkBindResultInfo);
        }
    }

    /* access modifiers changed from: private */
    public void handleClearBindProcessToNetwork(int network, int uid) {
        this.mMpLinkBindResultInfo.reset();
        this.mMpLinkBindResultInfo.setNetwork(this.mCurrentUnbindNetId);
        this.mMpLinkBindResultInfo.setUid(uid);
        int ret = bindProcessToNetwork(network, uid);
        if (ret == 0) {
            closeProcessSockets(0, uid);
            InetAddress.clearDnsCache();
            NetworkEventDispatcher.getInstance().onNetworkConfigurationChanged();
            this.mMpLinkBindResultInfo.setResult(3);
            this.mCurrentRequestBindNetWork = -1;
            MpLinkCommonUtils.logD(TAG, false, "unbind network successful !", new Object[0]);
            HwMpLinkDemoMode hwMpLinkDemoMode = this.mHwMpLinkDemoMode;
            if (hwMpLinkDemoMode != null) {
                hwMpLinkDemoMode.showToast("unbind network successful !");
            }
            if (this.mCurrentBindUid != -1) {
                this.mHwMpLinkChrImpl.updateMpLinkCellBindState(false, null);
            }
            this.mCurrentBindUid = -1;
        } else {
            MpLinkCommonUtils.logD(TAG, false, "unbind network fail with err %{public}d", new Object[]{Integer.valueOf(ret)});
            this.mMpLinkBindResultInfo.setResult(4);
        }
        IMpLinkCallback iMpLinkCallback = this.mMpLinkCallback;
        if (iMpLinkCallback != null) {
            iMpLinkCallback.onBindProcessToNetworkResult(this.mMpLinkBindResultInfo);
        }
    }

    private int bindProcessToNetwork(int network, int uid) {
        int reason = HwHidataJniAdapter.getInstance().bindUidProcessToNetwork(network, uid);
        MpLinkCommonUtils.logD(TAG, false, "bindProcessToNetwork network:%{public}d, uid:%{public}d, reason:%{public}d", new Object[]{Integer.valueOf(network), Integer.valueOf(uid), Integer.valueOf(reason)});
        if (reason != 0) {
            if (network == 0) {
                this.mHwMpLinkChrImpl.updateUnBindFailCnt(reason);
            } else {
                this.mHwMpLinkChrImpl.updateBindFailCnt(reason);
            }
        } else if (network == 0) {
            this.mHwMpLinkChrImpl.updateUnBindSuccCnt();
        } else {
            this.mHwMpLinkChrImpl.updateBindSuccCnt();
        }
        return reason;
    }

    private int resetProcessSockets(int network, int uid) {
        MpLinkCommonUtils.logD(TAG, false, "resetProcessSockets network:%{public}d, uid:%{public}d", new Object[]{Integer.valueOf(network), Integer.valueOf(uid)});
        return HwHidataJniAdapter.getInstance().resetProcessSockets(uid);
    }

    private int handleSocketStrategy(int strategy, int uid) {
        int tempStrategy = strategy;
        if (this.mHwMpLinkDemoMode != null) {
            tempStrategy = SystemProperties.getInt(MPLINK_CLOSE_TYPE_KEY, tempStrategy);
        }
        MpLinkCommonUtils.logD(TAG, false, "Socket strategy:%{public}d, uid:%{public}d", new Object[]{Integer.valueOf(tempStrategy), Integer.valueOf(uid)});
        int ret = 0;
        if ((tempStrategy & 1) != 0) {
            this.mHwInnerNetworkManagerImpl.closeSocketsForUid(uid);
        }
        if (!((tempStrategy & 2) == 0 && (tempStrategy & 4) == 0)) {
            ret = HwHidataJniAdapter.getInstance().handleSocketStrategy(tempStrategy, uid);
        }
        if (ret != 0) {
            this.mHwMpLinkChrImpl.updateCloseSocketFailCnt(ret);
        } else {
            this.mHwMpLinkChrImpl.updateCloseSocketSuccCnt();
        }
        return ret;
    }

    private int closeProcessSockets(int strategy, int uid) {
        MpLinkCommonUtils.logD(TAG, false, "closeProcessSockets strategy:%{public}d, uid:%{public}d", new Object[]{Integer.valueOf(strategy), Integer.valueOf(uid)});
        int ret = 0;
        if ((strategy & 1) != 0) {
            this.mHwInnerNetworkManagerImpl.closeSocketsForUid(uid);
        }
        if (!((strategy & 2) == 0 && (strategy & 4) == 0)) {
            ret = HwHidataJniAdapter.getInstance().handleSocketStrategy(strategy, uid);
        }
        if (ret != 0) {
            this.mHwMpLinkChrImpl.updateCloseSocketFailCnt(ret);
        } else {
            this.mHwMpLinkChrImpl.updateCloseSocketSuccCnt();
        }
        return ret;
    }

    public NetworkInfo getMpLinkNetworkInfo(NetworkInfo info, int uid) {
        if (!(this.mCurrentBindUid == -1 && this.mHwMpLinkDemoMode == null)) {
            MpLinkCommonUtils.logI(TAG, false, "uid = %{public}d, binduid = %{public}d, strategy: %{public}d", new Object[]{Integer.valueOf(uid), Integer.valueOf(this.mCurrentBindUid), Integer.valueOf(this.mCurrentNetworkStrategy)});
        }
        int i = this.mCurrentBindUid;
        if (i != -1 && uid == i && this.mCurrentNetworkStrategy == 1) {
            return this.mHwMpLinkNetworkImpl.createMobileNetworkInfo();
        }
        return info;
    }

    public boolean isAppBindedNetwork() {
        if (this.mCurrentBindUid == -1 || !MpLinkCommonUtils.isMpLinkEnabled(this.mContext)) {
            return false;
        }
        return true;
    }
}
