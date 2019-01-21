package com.android.server.hidata.mplink;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.HwInnerNetworkManagerImpl;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.telephony.ServiceState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.hidata.HwHidataJniAdapter;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.arbitration.IHiDataCHRCallBack;
import huawei.android.net.hwmplink.MpLinkCommonUtils;
import java.net.InetAddress;
import libcore.net.event.NetworkEventDispatcher;

public class HwMpLinkServiceImpl extends StateMachine implements IMpLinkStateObserverCallback {
    public static final int DEFAULT_SLOT_ID = 0;
    public static final int ILLEGAL_VALUE = -1;
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
    public static final int MPLINK_MSG_WIFIPRO_SWITCH_DISABLE = 210;
    public static final int MPLINK_MSG_WIFIPRO_SWITCH_ENABLE = 209;
    public static final int MPLINK_MSG_WIFI_CONNECTED = 213;
    public static final int MPLINK_MSG_WIFI_DISCONNECTED = 214;
    public static final int MPLINK_MSG_WIFI_VPN_CONNETED = 208;
    public static final int MPLINK_MSG_WIFI_VPN_DISCONNETED = 207;
    private static final int MPLK_SK_STRATEGY_CT = 1;
    private static final int MPLK_SK_STRATEGY_FURE = 4;
    private static final int MPLK_SK_STRATEGY_FUTE = 2;
    public static final int MSG_MPLINK_REQUEST_BIND_NETWORK = 2;
    public static final int MSG_MPLINK_REQUEST_UNBIND_NETWORK = 3;
    private static final int MSG_MPLINK_SETTINGS_STATE_CHANGE = 1;
    public static final int MSG_TEST_FOREGROUD_APP_LTE_HANDOVER_WIGI = 102;
    public static final int MSG_TEST_FOREGROUD_APP_WIFI_HANDOVER_LTE = 101;
    public static final int MSG_TEST_NET_COEXIST_LTE_PRIORITIZED = 104;
    public static final int MSG_TEST_NET_COEXIST_WIFI_PRIORITIZED = 103;
    private static final String TAG = "HiData_MpLinkServiceImpl";
    private static final int delayInMs = 30000;
    private static HwMpLinkServiceImpl mMpLinkServiceImpl;
    private final AlarmManager mAlarmManager;
    private ConnectivityManager mConnectivityManager = null;
    private Context mContext;
    private int mCurrentBindUid = -1;
    private int mCurrentNetworkStrategy = -1;
    private int mCurrentUnbindNetId = -1;
    private IHiDataCHRCallBack mHiDataCHRCallBack = null;
    private HwInnerNetworkManagerImpl mHwInnerNetworkManagerImpl;
    private HwMpLinkContentAware mHwMpLinkContentAware;
    private HwMpLinkDemoMode mHwMpLinkDemoMode;
    private HwMpLinkNetworkImpl mHwMpLinkNetworkImpl;
    private HwMpLinkTelephonyImpl mHwMpLinkTelephonyImpl;
    private HwMpLinkWifiImpl mHwMpLinkWifiImpl;
    private HwMplinkChrImpl mHwMplinkChrImpl = null;
    private HwMplinkStateObserver mHwMplinkStateObserver;
    private State mInitState = new InitState();
    private boolean mInternalMplinkEnable = false;
    private State mMpLinkBaseState = new MpLinkBaseState();
    private IMpLinkCallback mMpLinkCallback;
    private int mMpLinkConditionState = -1;
    private int mMpLinkNotifyControl = -1;
    private boolean mMpLinkSwitchEnable = false;
    private State mMpLinkedState = new MpLinkedState();
    private MplinkBindResultInfo mMplinkBindResultInfo = new MplinkBindResultInfo();
    private MplinkNetworkResultInfo mMplinkNri = new MplinkNetworkResultInfo();
    private PendingIntent mRequestCheckAlarmIntent = null;
    private boolean shouldIgnoreDefaultChange = false;

    class InitState extends State {
        InitState() {
        }

        public void enter() {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, "Enter InitState");
        }

        public void exit() {
            MpLinkCommonUtils.logI(HwMpLinkServiceImpl.TAG, "Exit InitState");
        }

        public boolean processMessage(Message message) {
            String str = HwMpLinkServiceImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("InitState,msg=");
            stringBuilder.append(message.what);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
            int i = message.what;
            if (i != 103) {
                if (i != HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_CONNECTED) {
                    switch (i) {
                        case 211:
                            if (HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.isWifiConnected()) {
                                if (!HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isMobileConnected()) {
                                    HwMpLinkServiceImpl.this.notifyNetCoexistFailed(9);
                                    break;
                                }
                                HwMpLinkServiceImpl.this.notifyNetCoexistFailed(15);
                                break;
                            }
                            HwMpLinkServiceImpl.this.notifyNetCoexistFailed(1);
                            break;
                        case HwMpLinkServiceImpl.MPLINK_MSG_HIBRAIN_MPLINK_CLOSE /*212*/:
                            HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.closeMobileDataIfOpened();
                            HwMpLinkServiceImpl.this.notifyNetCoexistFailed(true);
                            break;
                        case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_CONNECTED /*213*/:
                            break;
                        default:
                            return true;
                    }
                }
                HwMpLinkServiceImpl.this.transitionTo(HwMpLinkServiceImpl.this.mMpLinkBaseState);
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
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, "Enter MpLinkBaseState");
        }

        public void exit() {
            MpLinkCommonUtils.logI(HwMpLinkServiceImpl.TAG, "Exit MpLinkBaseState");
        }

        public boolean processMessage(Message message) {
            String str = HwMpLinkServiceImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MpLinkBaseState,msg=");
            stringBuilder.append(message.what);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
            int i = message.what;
            if (i != 103) {
                switch (i) {
                    case 2:
                        HwMpLinkServiceImpl.this.mMplinkBindResultInfo.reset();
                        HwMpLinkServiceImpl.this.mMplinkBindResultInfo.setNetwork(message.arg1);
                        HwMpLinkServiceImpl.this.mMplinkBindResultInfo.setUid(message.arg2);
                        HwMpLinkServiceImpl.this.mMplinkBindResultInfo.setFailReason(101);
                        HwMpLinkServiceImpl.this.mMplinkBindResultInfo.setResult(4);
                        if (HwMpLinkServiceImpl.this.mMpLinkCallback != null) {
                            HwMpLinkServiceImpl.this.mMpLinkCallback.onBindProcessToNetworkResult(HwMpLinkServiceImpl.this.mMplinkBindResultInfo);
                            break;
                        }
                        break;
                    case 3:
                        HwMpLinkServiceImpl.this.handleClearBindProcessToNetwork(0, message.arg2);
                        break;
                    default:
                        switch (i) {
                            case 211:
                                if (!HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.isWifiConnected()) {
                                    HwMpLinkServiceImpl.this.notifyNetCoexistFailed(1);
                                    break;
                                }
                                HwMpLinkServiceImpl.this.openMpLinkNetworkCoexist();
                                break;
                            case HwMpLinkServiceImpl.MPLINK_MSG_HIBRAIN_MPLINK_CLOSE /*212*/:
                                HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.closeMobileDataIfOpened();
                                HwMpLinkServiceImpl.this.notifyNetCoexistFailed(true);
                                break;
                            case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_CONNECTED /*213*/:
                                if (HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isMobileConnected()) {
                                    HwMpLinkServiceImpl.this.shouldIgnoreDefaultChange = true;
                                    HwMpLinkServiceImpl.this.transitionTo(HwMpLinkServiceImpl.this.mMpLinkedState);
                                    break;
                                }
                                break;
                            case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED /*214*/:
                                if (!HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.isMobileConnected()) {
                                    HwMpLinkServiceImpl.this.transitionTo(HwMpLinkServiceImpl.this.mInitState);
                                    break;
                                }
                                break;
                            case HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_CONNECTED /*215*/:
                                if (HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.isWifiConnected()) {
                                    HwMpLinkServiceImpl.this.mHwMplinkChrImpl.updataDualNetworkCnt();
                                    HwMpLinkServiceImpl.this.transitionTo(HwMpLinkServiceImpl.this.mMpLinkedState);
                                    break;
                                }
                                break;
                            case HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_DISCONNECTED /*216*/:
                                if (!HwMpLinkServiceImpl.this.mHwMpLinkWifiImpl.isWifiConnected()) {
                                    HwMpLinkServiceImpl.this.transitionTo(HwMpLinkServiceImpl.this.mInitState);
                                    break;
                                }
                                break;
                            case HwMpLinkServiceImpl.MPLINK_MSG_AIDEVICE_MPLINK_OPEN /*217*/:
                                HwMpLinkServiceImpl.this.mHwMplinkChrImpl.updateAiDeviceOpenCnt(HwMpLinkServiceImpl.this.mHwMpLinkContentAware.getCurrentApName());
                                break;
                            default:
                                return true;
                        }
                }
            } else if (message.arg1 == 0) {
                HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(true);
            } else if (message.arg1 == 1) {
                HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(false);
            }
            return true;
        }
    }

    class MpLinkedState extends State {
        MpLinkedState() {
        }

        public void enter() {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, "Enter MpLinkedState");
            if (HwMpLinkServiceImpl.this.mHwMpLinkTelephonyImpl.getMobileDataAvaiable()) {
                HwMpLinkServiceImpl.this.notifyMpLinkNetCoexistSuccessful();
            }
        }

        public void exit() {
            MpLinkCommonUtils.logD(HwMpLinkServiceImpl.TAG, "Exit MpLinkedState");
            HwMpLinkServiceImpl.this.mCurrentBindUid = -1;
        }

        public boolean processMessage(Message message) {
            String str = HwMpLinkServiceImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MpLinkedState,msg=");
            stringBuilder.append(message.what);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
            switch (message.what) {
                case 2:
                    HwMpLinkServiceImpl.this.handleBindProcessToNetwork(message.arg1, message.arg2, (MpLinkQuickSwitchConfiguration) message.obj);
                    break;
                case 3:
                    HwMpLinkServiceImpl.this.handleClearBindProcessToNetwork(0, message.arg2);
                    break;
                case 101:
                case 102:
                    HwMpLinkServiceImpl.this.handleBindProcessToNetwork(message.arg1, message.arg2, new MpLinkQuickSwitchConfiguration(3, 0));
                    break;
                case 103:
                    if (message.arg1 != 0) {
                        if (message.arg1 == 1) {
                            HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(false);
                            break;
                        }
                    }
                    HwMpLinkServiceImpl.this.requestWiFiAndCellCoexist(true);
                    break;
                    break;
                case 202:
                case 204:
                case 206:
                case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED /*208*/:
                case 210:
                case HwMpLinkServiceImpl.MPLINK_MSG_HIBRAIN_MPLINK_CLOSE /*212*/:
                case HwMpLinkServiceImpl.MPLINK_MSG_DATA_SUB_CHANGE /*230*/:
                    HwMpLinkServiceImpl.this.closeMpLinkNetworkCoexist();
                    break;
                case 211:
                    HwMpLinkServiceImpl.this.notifyMpLinkNetCoexistSuccessful();
                    break;
                case HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED /*214*/:
                    HwMpLinkServiceImpl.this.notifyNetCoexistFailed(1);
                    HwMpLinkServiceImpl.this.mHwMplinkChrImpl.updateCoexistWifiSwitchClosedCnt();
                    HwMpLinkServiceImpl.this.closeMpLinkNetworkCoexist();
                    HwMpLinkServiceImpl.this.transitionTo(HwMpLinkServiceImpl.this.mMpLinkBaseState);
                    break;
                case HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_DISCONNECTED /*216*/:
                    HwMpLinkServiceImpl.this.notifyNetCoexistFailed(9);
                    HwMpLinkServiceImpl.this.closeMpLinkNetworkCoexist();
                    HwMpLinkServiceImpl.this.transitionTo(HwMpLinkServiceImpl.this.mMpLinkBaseState);
                    break;
                case HwMpLinkServiceImpl.MPLINK_MSG_AIDEVICE_MPLINK_OPEN /*217*/:
                    HwMpLinkServiceImpl.this.mHwMplinkChrImpl.updateAiDeviceOpenCnt(HwMpLinkServiceImpl.this.mHwMpLinkContentAware.getCurrentApName());
                    break;
                case HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE /*220*/:
                    HwMpLinkServiceImpl.this.mHwMplinkChrImpl.updateCoexistMobileDataSwitchClosedCnt();
                    break;
                case HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_AVAILABLE /*221*/:
                    HwMpLinkServiceImpl.this.notifyMpLinkNetCoexistSuccessful();
                    break;
                case HwMpLinkServiceImpl.MPLINK_MSG_DEFAULT_NETWORK_CHANGE /*231*/:
                    if (!HwMpLinkServiceImpl.this.shouldIgnoreDefaultChange) {
                        HwMpLinkServiceImpl.this.mHwMplinkChrImpl.updateDefaultRouteChangeCnt();
                        HwMpLinkServiceImpl.this.shouldIgnoreDefaultChange = false;
                        break;
                    }
                    break;
                default:
                    return true;
            }
            return true;
        }
    }

    public static HwMpLinkServiceImpl getInstance(Context context) {
        if (mMpLinkServiceImpl == null) {
            mMpLinkServiceImpl = new HwMpLinkServiceImpl(context);
        }
        return mMpLinkServiceImpl;
    }

    private HwMpLinkServiceImpl(Context context) {
        super("HwMpLinkServiceImpl");
        this.mContext = context;
        addState(this.mInitState);
        addState(this.mMpLinkBaseState, this.mInitState);
        addState(this.mMpLinkedState, this.mMpLinkBaseState);
        setInitialState(this.mInitState);
        start();
        if (MpLinkCommonUtils.isMpLinkTestMode()) {
            this.mHwMpLinkDemoMode = new HwMpLinkDemoMode(this.mContext, getHandler());
        }
        this.mHwMplinkChrImpl = new HwMplinkChrImpl(this.mContext);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mHwMpLinkContentAware = HwMpLinkContentAware.getInstance(context);
        this.mHwMpLinkContentAware.regiterMpLinkHander(getHandler());
        this.mHwMpLinkTelephonyImpl = new HwMpLinkTelephonyImpl(this.mContext, getHandler());
        this.mHwMpLinkWifiImpl = new HwMpLinkWifiImpl(this.mContext, getHandler());
        this.mHwMplinkStateObserver = new HwMplinkStateObserver(this.mContext, this);
        this.mHwMpLinkNetworkImpl = new HwMpLinkNetworkImpl(context);
        this.mHwInnerNetworkManagerImpl = (HwInnerNetworkManagerImpl) HwFrameworkFactory.getHwInnerNetworkManager();
        this.mHwMpLinkWifiImpl.setCurrentWifiVpnState(this.mHwMplinkStateObserver.getVpnConnectState());
        this.mMpLinkSwitchEnable = this.mHwMplinkStateObserver.getMpLinkSwitchState();
        setMpLinkConditionDB(200);
        this.mHwMplinkStateObserver.initSimulateHibrain();
        MpLinkCommonUtils.logD(TAG, "HwMpLinkServiceImpl complete");
    }

    private void getConnectiviyManger() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    private void setMpLinkConditionDB(int type) {
        if (type >= 200 && type <= MPLINK_MSG_MOBILE_DATA_SWITCH_CLOSE) {
            int value = 0;
            if (checkMplinkSuitableBeOpen() == 0) {
                value = 1;
            }
            if (this.mMpLinkConditionState != value) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setMpLinkConditionDB value:");
                stringBuilder.append(value);
                MpLinkCommonUtils.logD(str, stringBuilder.toString());
                System.putInt(this.mContext.getContentResolver(), "mplink_db_condition_value", value);
                this.mMpLinkConditionState = value;
            }
        }
    }

    private int checkMplinkSuitableBeOpen() {
        int failReason = 0;
        if (!this.mMpLinkSwitchEnable) {
            failReason = 8;
        } else if (this.mHwMpLinkContentAware.isAiDevice()) {
            failReason = 0;
        } else if (this.mHwMpLinkTelephonyImpl.getCurrentServceState() != 0) {
            failReason = 5;
        } else if (!this.mHwMpLinkTelephonyImpl.getCurrentDataTechSuitable()) {
            failReason = 3;
        } else if (this.mHwMpLinkTelephonyImpl.getCurrentDataRoamingState()) {
            failReason = 4;
        } else if (!this.mInternalMplinkEnable) {
            failReason = 7;
        } else if (!this.mHwMpLinkTelephonyImpl.isMobileDataEnable()) {
            failReason = 2;
        } else if (this.mHwMpLinkWifiImpl.getCurrentWifiVpnState()) {
            failReason = 6;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkMplinkSuitableBeOpen ret:");
        stringBuilder.append(failReason);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        return failReason;
    }

    private void notifyMpLinkNetCoexistSuccessful() {
        this.mMplinkNri.reset();
        this.mMplinkNri.setFailReason(0);
        this.mMplinkNri.setResult(100);
        this.mHwMplinkChrImpl.updateMobileDataConnectedStamp();
        if (!(this.mMpLinkCallback == null || this.mMpLinkNotifyControl == 1)) {
            MpLinkCommonUtils.logD(TAG, "network coexist successful");
            this.mMpLinkCallback.onWiFiAndCellCoexistResult(this.mMplinkNri);
            this.mMpLinkNotifyControl = 1;
        }
        recordRequestResult(this.mMplinkNri);
    }

    private void notifyNetCoexistFailed(boolean forceNotify) {
        notifyNetCoexistFailed(0, forceNotify);
    }

    private void notifyNetCoexistFailed(int reason) {
        notifyNetCoexistFailed(reason, false);
    }

    private void notifyNetCoexistFailed(int reason, boolean forceNotify) {
        this.mMplinkNri.reset();
        this.mMplinkNri.setFailReason(reason);
        this.mMplinkNri.setResult(101);
        if (this.mMpLinkCallback != null) {
            String str;
            StringBuilder stringBuilder;
            if (this.mMpLinkNotifyControl != 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("network coexist failed,reason:");
                stringBuilder.append(reason);
                MpLinkCommonUtils.logD(str, stringBuilder.toString());
                this.mMpLinkCallback.onWiFiAndCellCoexistResult(this.mMplinkNri);
                this.mMpLinkNotifyControl = 0;
            } else if (forceNotify) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("force notify network coexist failed,reason:");
                stringBuilder.append(reason);
                MpLinkCommonUtils.logD(str, stringBuilder.toString());
                this.mMpLinkCallback.onWiFiAndCellCoexistResult(this.mMplinkNri);
            }
        }
        recordRequestResult(this.mMplinkNri);
    }

    private void openMpLinkNetworkCoexist() {
        int Reason = checkMplinkSuitableBeOpen();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("request start mplink network coexist: Reason:");
        stringBuilder.append(Reason);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        if (Reason != 0) {
            notifyNetCoexistFailed(Reason);
        } else if (this.mHwMpLinkTelephonyImpl.isMobileConnected()) {
            MpLinkCommonUtils.logD(TAG, "openMpLink mobile already connected");
        } else {
            this.mHwMplinkChrImpl.updateOpenMobileDataStamp();
            this.mHwMpLinkTelephonyImpl.mplinkSetMobileData(true);
        }
    }

    private void closeMpLinkNetworkCoexist() {
        MpLinkCommonUtils.logD(TAG, "request close mplink network coexist");
        this.mHwMpLinkTelephonyImpl.mplinkSetMobileData(false);
        this.mInternalMplinkEnable = false;
    }

    public boolean isMpLinkConditionSatisfy() {
        boolean can = false;
        if (checkMplinkSuitableBeOpen() == 0) {
            can = true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isMpLinkConditionSatisfy return:");
        stringBuilder.append(can);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        return can;
    }

    public void notifyIpConfigCompleted() {
    }

    public void foregroundAppChanged(int uid) {
        if (this.mHwMpLinkContentAware.isWifiLanApp(uid)) {
            MpLinkCommonUtils.logD(TAG, "isWifiLanApp");
        }
    }

    public void registMpLinkCallback(IMpLinkCallback callback) {
        if (this.mMpLinkCallback == null) {
            this.mMpLinkCallback = callback;
        }
    }

    public void registMpLinkCHRCallback(IHiDataCHRCallBack callback) {
        if (this.mHiDataCHRCallBack == null) {
            this.mHiDataCHRCallBack = callback;
        }
    }

    public void registRFInterferenceCallback(IRFInterferenceCallback callback) {
        MpLinkCommonUtils.logD(TAG, "registRFInterferenceCallback");
    }

    private void startRequestSuccessCheck(int requestType) {
        stopRequestSuccessCheck();
        Intent intent = new Intent("mplink_intent_check_request_success");
        intent.putExtra("mplink_intent_key_check_request", requestType);
        this.mRequestCheckAlarmIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728);
        this.mAlarmManager.setExact(3, SystemClock.elapsedRealtime() + HwArbitrationDEFS.DelayTimeMillisA, this.mRequestCheckAlarmIntent);
    }

    private void stopRequestSuccessCheck() {
        if (this.mRequestCheckAlarmIntent != null) {
            this.mAlarmManager.cancel(this.mRequestCheckAlarmIntent);
            this.mRequestCheckAlarmIntent = null;
        }
    }

    private void recordRequestResult(MplinkNetworkResultInfo resultInfo) {
        if (this.mRequestCheckAlarmIntent != null) {
            int type = this.mRequestCheckAlarmIntent.getIntent().getIntExtra("mplink_intent_key_check_request", -1);
            int result = resultInfo.getResult();
            int reason = resultInfo.getFailReason();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("recordRequestResult:");
            stringBuilder.append(type);
            stringBuilder.append(",");
            stringBuilder.append(result);
            stringBuilder.append(",");
            stringBuilder.append(reason);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
            if (type == 211) {
                if (result == 101) {
                    this.mHwMplinkChrImpl.updateOpenFailCnt(reason);
                    stopRequestSuccessCheck();
                } else if (result == 100) {
                    this.mHwMplinkChrImpl.updateOpenSuccCnt();
                    stopRequestSuccessCheck();
                }
            } else if (type == MPLINK_MSG_HIBRAIN_MPLINK_CLOSE) {
                if (result == 101) {
                    this.mHwMplinkChrImpl.updateCloseSuccCnt();
                    stopRequestSuccessCheck();
                } else if (result == 100) {
                    this.mHwMplinkChrImpl.updateCloseFailCnt(reason);
                    stopRequestSuccessCheck();
                }
                this.mHwMplinkChrImpl.sendDataToChr(this.mHiDataCHRCallBack);
            }
        }
    }

    public void requestWiFiAndCellCoexist(boolean coexist) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestCoexist, coexist:");
        stringBuilder.append(coexist);
        stringBuilder.append(", internal:");
        stringBuilder.append(this.mInternalMplinkEnable);
        stringBuilder.append(",Control:");
        stringBuilder.append(this.mMpLinkNotifyControl);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        if (this.mMpLinkCallback == null) {
            MpLinkCommonUtils.logD(TAG, "callback is null");
        } else if (this.mInternalMplinkEnable != coexist) {
            this.mInternalMplinkEnable = coexist;
            this.mMpLinkNotifyControl = -1;
            if (coexist) {
                sendMessage(211);
                startRequestSuccessCheck(211);
                return;
            }
            sendMessage(MPLINK_MSG_HIBRAIN_MPLINK_CLOSE);
            startRequestSuccessCheck(MPLINK_MSG_HIBRAIN_MPLINK_CLOSE);
        } else {
            MpLinkCommonUtils.logD(TAG, "dup request");
            if (!coexist && !this.mInternalMplinkEnable) {
                sendMessage(MPLINK_MSG_HIBRAIN_MPLINK_CLOSE);
                startRequestSuccessCheck(MPLINK_MSG_HIBRAIN_MPLINK_CLOSE);
            } else if (this.mRequestCheckAlarmIntent == null) {
                MpLinkCommonUtils.logD(TAG, "response for dup request");
                this.mMpLinkCallback.onWiFiAndCellCoexistResult(this.mMplinkNri);
            }
        }
    }

    public void updateMplinkAiDevicesList(int type, String packageWhiteList) {
    }

    public void onMpLinkRequestTimeout(int requestType) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onMpLinkRequestTimeout, type:");
        stringBuilder.append(requestType);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        notifyNetCoexistFailed(10, true);
    }

    public void onTelephonyServiceStateChanged(ServiceState serviceState, int subId) {
        this.mHwMpLinkTelephonyImpl.handleTelephonyServiceStateChanged(serviceState, subId);
    }

    public void onTelephonyDefaultDataSubChanged(int newDataSub) {
        this.mHwMpLinkTelephonyImpl.handleDataSubChange(newDataSub);
    }

    public void onTelephonyDataConnectionChanged(String state, String iface, int subId) {
        this.mHwMpLinkTelephonyImpl.handleTelephonyDataConnectionChanged(state, iface, subId);
    }

    public void onMobileDataSwitchChange(boolean enabled) {
        this.mHwMpLinkTelephonyImpl.handleMobileDataSwitchChange(enabled);
    }

    public void onWifiNetworkStateChanged(NetworkInfo netInfo) {
        this.mHwMpLinkWifiImpl.handleWifiNetworkStateChanged(netInfo);
    }

    public void onVpnStateChange(boolean vpnconnected) {
        this.mHwMpLinkWifiImpl.handleVpnStateChange(vpnconnected);
    }

    public void onMplinkSwitchChange(boolean mplinkSwitch) {
        if (this.mMpLinkSwitchEnable != mplinkSwitch) {
            this.mMpLinkSwitchEnable = mplinkSwitch;
            if (mplinkSwitch) {
                sendMessage(MPLINK_MSG_WIFIPRO_SWITCH_ENABLE);
            } else {
                sendMessage(210);
            }
        }
    }

    public void onSimulateHiBrainRequestForDemo(boolean enable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSimulateHiBrainRequestForTest:");
        stringBuilder.append(enable);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        requestWiFiAndCellCoexist(enable);
    }

    public void requestBindProcessToNetwork(int network, int uid, int type) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bindProcessToNetwork network:");
        stringBuilder.append(network);
        stringBuilder.append(", uid:");
        stringBuilder.append(uid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        requestBindProcessToNetwork(network, uid, null);
    }

    public void requestBindProcessToNetwork(int netid, int uid, MpLinkQuickSwitchConfiguration configuration) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bindProcessToNetwork network:");
        stringBuilder.append(netid);
        stringBuilder.append(", uid:");
        stringBuilder.append(uid);
        stringBuilder.append("configuration: ");
        stringBuilder.append(configuration);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        sendMessage(2, netid, uid, configuration);
    }

    public void requestClearBindProcessToNetwork(int network, int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("clearBindProcessToNetwork network:");
        stringBuilder.append(network);
        stringBuilder.append(", uid:");
        stringBuilder.append(uid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        this.mCurrentUnbindNetId = network;
        sendMessage(3, 0, uid);
    }

    private void handleBindProcessToNetwork(int network, int uid, MpLinkQuickSwitchConfiguration quickSwitchConfig) {
        this.mMplinkBindResultInfo.reset();
        this.mMplinkBindResultInfo.setNetwork(network);
        this.mMplinkBindResultInfo.setUid(uid);
        int ret = bindProcessToNetwork(network, uid);
        String str;
        if (ret == 0) {
            this.mCurrentBindUid = uid;
            InetAddress.clearDnsCache();
            NetworkEventDispatcher.getInstance().onNetworkConfigurationChanged();
            this.mMplinkBindResultInfo.setResult(1);
            MpLinkCommonUtils.logD(TAG, "bind network successful !");
            if (quickSwitchConfig == null) {
                MpLinkCommonUtils.logD(TAG, "quickSwitchConfig is null");
                closeProcessSockets(0, uid);
                this.mHwMpLinkNetworkImpl.handleNetworkStrategy(0, uid);
            } else {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("quickSwitchConfig:");
                stringBuilder.append(quickSwitchConfig.toString());
                MpLinkCommonUtils.logD(str, stringBuilder.toString());
                this.mCurrentNetworkStrategy = quickSwitchConfig.getNetworkStrategy();
                handleSocketStrategy(quickSwitchConfig.getSocketStrategy(), uid);
                if (this.mHwMpLinkDemoMode != null) {
                    this.mCurrentNetworkStrategy = SystemProperties.getInt("mplink_network_type", this.mCurrentNetworkStrategy);
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("network strategy:");
                stringBuilder.append(this.mCurrentNetworkStrategy);
                stringBuilder.append(", uid:");
                stringBuilder.append(uid);
                MpLinkCommonUtils.logD(str, stringBuilder.toString());
                this.mHwMpLinkNetworkImpl.handleNetworkStrategy(this.mCurrentNetworkStrategy, uid);
                if (MpLinkCommonUtils.getNetworkType(this.mContext, network) == 0) {
                    this.mHwMplinkChrImpl.updateMplinkCellBindState(true, this.mHwMpLinkTelephonyImpl.getMobileIface());
                } else {
                    MpLinkCommonUtils.logD(TAG, "bind to wifi");
                }
            }
            if (this.mHwMpLinkDemoMode != null) {
                this.mHwMpLinkDemoMode.showToast("bind network successful !");
            }
        } else {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("bind network fail with err ");
            stringBuilder2.append(ret);
            MpLinkCommonUtils.logD(str, stringBuilder2.toString());
            this.mMplinkBindResultInfo.setResult(2);
        }
        if (this.mMpLinkCallback != null) {
            this.mMpLinkCallback.onBindProcessToNetworkResult(this.mMplinkBindResultInfo);
        }
    }

    private void handleClearBindProcessToNetwork(int network, int uid) {
        this.mMplinkBindResultInfo.reset();
        this.mMplinkBindResultInfo.setNetwork(this.mCurrentUnbindNetId);
        this.mMplinkBindResultInfo.setUid(uid);
        int ret = bindProcessToNetwork(network, uid);
        if (ret == 0) {
            closeProcessSockets(0, uid);
            InetAddress.clearDnsCache();
            NetworkEventDispatcher.getInstance().onNetworkConfigurationChanged();
            this.mMplinkBindResultInfo.setResult(3);
            MpLinkCommonUtils.logD(TAG, "unbind network successful !");
            if (this.mHwMpLinkDemoMode != null) {
                this.mHwMpLinkDemoMode.showToast("unbind network successful !");
            }
            if (this.mCurrentBindUid != -1) {
                this.mHwMplinkChrImpl.updateMplinkCellBindState(false, null);
            }
            this.mCurrentBindUid = -1;
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unbind network fail with err ");
            stringBuilder.append(ret);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
            this.mMplinkBindResultInfo.setResult(4);
        }
        if (this.mMpLinkCallback != null) {
            this.mMpLinkCallback.onBindProcessToNetworkResult(this.mMplinkBindResultInfo);
        }
    }

    private int bindProcessToNetwork(int network, int uid) {
        int reason = HwHidataJniAdapter.getInstance().bindUidProcessToNetwork(network, uid);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bindProcessToNetwork network:");
        stringBuilder.append(network);
        stringBuilder.append(", uid:");
        stringBuilder.append(uid);
        stringBuilder.append(", reason:");
        stringBuilder.append(reason);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        if (reason != 0) {
            if (network == 0) {
                this.mHwMplinkChrImpl.updateUnBindFailCnt(reason);
            } else {
                this.mHwMplinkChrImpl.updateBindFailCnt(reason);
            }
        } else if (network == 0) {
            this.mHwMplinkChrImpl.updateUnBindSuccCnt();
        } else {
            this.mHwMplinkChrImpl.updateBindSuccCnt();
        }
        return reason;
    }

    private int resetProcessSockets(int network, int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resetProcessSockets network:");
        stringBuilder.append(network);
        stringBuilder.append(", uid:");
        stringBuilder.append(uid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        return HwHidataJniAdapter.getInstance().resetProcessSockets(uid);
    }

    private int handleSocketStrategy(int strategy, int uid) {
        if (this.mHwMpLinkDemoMode != null) {
            strategy = SystemProperties.getInt("mplink_close_type", strategy);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Socket strategy:");
        stringBuilder.append(strategy);
        stringBuilder.append(", uid:");
        stringBuilder.append(uid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        int ret = 0;
        if ((strategy & 1) != 0) {
            this.mHwInnerNetworkManagerImpl.closeSocketsForUid(uid);
        }
        if (!((strategy & 2) == 0 && (strategy & 4) == 0)) {
            ret = HwHidataJniAdapter.getInstance().handleSocketStrategy(strategy, uid);
        }
        if (ret != 0) {
            this.mHwMplinkChrImpl.updateCloseSocketFailCnt(ret);
        } else {
            this.mHwMplinkChrImpl.updateCloseSocketSuccCnt();
        }
        return ret;
    }

    private int closeProcessSockets(int strategy, int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("closeProcessSockets strategy:");
        stringBuilder.append(strategy);
        stringBuilder.append(", uid:");
        stringBuilder.append(uid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        int ret = 0;
        if ((strategy & 1) != 0) {
            this.mHwInnerNetworkManagerImpl.closeSocketsForUid(uid);
        }
        if (!((strategy & 2) == 0 && (strategy & 4) == 0)) {
            ret = HwHidataJniAdapter.getInstance().handleSocketStrategy(strategy, uid);
        }
        if (ret != 0) {
            this.mHwMplinkChrImpl.updateCloseSocketFailCnt(ret);
        } else {
            this.mHwMplinkChrImpl.updateCloseSocketSuccCnt();
        }
        return ret;
    }

    /* JADX WARNING: Missing block: B:14:0x004a, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public NetworkInfo getMpLinkNetworkInfo(NetworkInfo info, int uid) {
        if (!(this.mCurrentBindUid == -1 && this.mHwMpLinkDemoMode == null)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("uid = ");
            stringBuilder.append(uid);
            stringBuilder.append(", binduid = ");
            stringBuilder.append(this.mCurrentBindUid);
            stringBuilder.append(", strategy: ");
            stringBuilder.append(this.mCurrentNetworkStrategy);
            MpLinkCommonUtils.logI(str, stringBuilder.toString());
        }
        if (this.mCurrentBindUid != -1 && uid == this.mCurrentBindUid && this.mCurrentNetworkStrategy == 1) {
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
