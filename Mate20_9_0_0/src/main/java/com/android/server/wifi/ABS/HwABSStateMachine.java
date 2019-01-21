package com.android.server.wifi.ABS;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings.System;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneCallback;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import com.android.ims.ImsManager;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.util.ScanResultUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HwABSStateMachine extends StateMachine {
    private static final long ABS_INTERVAL_TIME = 1800000;
    private static final long ABS_PUNISH_TIME = 60000;
    private static final long ABS_SCREEN_ON_TIME = 10000;
    private static final String ACTION_ABS_HANDOVER_TIMER = "android.net.wifi.abs_handover_timer";
    private static final int MAX_HANDOVER_TIME = 15;
    private static final long ONEDAYA_TIME = 86400000;
    private static final int SIM_CARD_STATE_MIMO = 2;
    private static final int SIM_CARD_STATE_SISO = 1;
    private static HwABSStateMachine mHwABSStateMachine = null;
    private int ABS_HANDOVER_TIMES = 0;
    private long ABS_LAST_HANDOVER_TIME = 0;
    private boolean ANTENNA_STATE_IN_CALL = false;
    private boolean ANTENNA_STATE_IN_CONNECT = false;
    private boolean ANTENNA_STATE_IN_PREEMPTED = false;
    private boolean ANTENNA_STATE_IN_SEARCH = false;
    private int MODEM_TUNERIC_ACTIVE = 1;
    private int MODEM_TUNERIC_IACTIVE = 0;
    private int RESENT_MODEM_TUNERIC_ACTIVE_TIMES = 0;
    private int RESENT_MODEM_TUNERIC_IACTIVE_TIMES = 0;
    private int RESTART_ABS_TIME = 300000;
    private boolean isPuaseHandover = false;
    private boolean isSwitching = false;
    private long mABSMIMOScreenOnStartTime = 0;
    private long mABSMIMOStartTime = 0;
    private long mABSSISOScreenOnStartTime = 0;
    private long mABSSISOStartTime = 0;
    private Map<String, APHandoverInfo> mAPHandoverInfoList = new HashMap();
    private PhoneCallback mActiveCallback = new PhoneCallback() {
        public void onPhoneCallback1(int parm) {
            HwABSStateMachine.this.sendMessage(33, parm);
        }
    };
    private int mAddBlackListReason = 0;
    private String mAssociateBSSID = null;
    private String mAssociateSSID = null;
    private Context mContext;
    private State mDefaultState = new DefaultState();
    private HwABSCHRManager mHwABSCHRManager;
    private HwABSDataBaseManager mHwABSDataBaseManager;
    private HwABSWiFiHandler mHwABSWiFiHandler;
    private HwABSWiFiScenario mHwABSWiFiScenario;
    private PhoneCallback mIactiveCallback = new PhoneCallback() {
        public void onPhoneCallback1(int parm) {
            HwABSStateMachine.this.sendMessage(35, parm);
        }
    };
    private boolean mIsInCallPunish = false;
    private boolean mIsSupportVoWIFI = false;
    private State mMimoState = new MimoState();
    private List<Integer> mModemStateList = new ArrayList();
    private State mSisoState = new SisoState();
    private int mSwitchEvent = 0;
    private int mSwitchType = 0;
    private TelephonyManager mTelephonyManager;
    private State mWiFiConnectedState = new WiFiConnectedState();
    private State mWiFiDisableState = new WiFiDisableState();
    private State mWiFiDisconnectedState = new WiFiDisconnectedState();
    private State mWiFiEnableState = new WiFiEnableState();
    private WifiManager mWifiManager;

    private static class APHandoverInfo {
        public long lastTime;
        public int mHandoverTimes;

        private APHandoverInfo() {
            this.mHandoverTimes = 0;
            this.lastTime = 0;
        }

        /* synthetic */ APHandoverInfo(AnonymousClass1 x0) {
            this();
        }
    }

    class DefaultState extends State {
        Bundle mData = null;
        int mSubId = -1;

        DefaultState() {
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 22) {
                HwABSStateMachine.this.handlePowerOffMessage();
                if (HwABSStateMachine.this.isModemStateInIdle()) {
                    HwABSStateMachine.this.resetCapablity(2);
                }
            } else if (i == 25) {
                HwABSStateMachine.this.mIsSupportVoWIFI = ImsManager.isWfcEnabledByPlatform(HwABSStateMachine.this.mContext);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DefaultState mIsSupportVoWIFI = ");
                stringBuilder.append(HwABSStateMachine.this.mIsSupportVoWIFI);
                HwABSUtils.logD(stringBuilder.toString());
            } else if (i != 103) {
                switch (i) {
                    case 1:
                        HwABSUtils.logD("DefaultState MSG_WIFI_CONNECTED");
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mWiFiConnectedState);
                        HwABSStateMachine.this.sendMessage(1);
                        break;
                    case 2:
                        HwABSUtils.logD("DefaultState MSG_WIFI_DISCONNECTED");
                        HwABSStateMachine.this.removeMessages(1);
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mWiFiDisconnectedState);
                        break;
                    case 3:
                        HwABSUtils.logD("DefaultState MSG_WIFI_ENABLED");
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mWiFiEnableState);
                        break;
                    case 4:
                        HwABSUtils.logD("DefaultState MSG_WIFI_DISABLE");
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mWiFiDisableState);
                        break;
                    default:
                        switch (i) {
                            case 7:
                                HwABSUtils.logD("DefaultState MSG_OUTGOING_CALL");
                                HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                                HwABSStateMachine.this.resetCapablity(1);
                                break;
                            case 8:
                                HwABSUtils.logD("DefaultState MSG_CALL_STATE_IDLE");
                                HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = false;
                                HwABSStateMachine.this.resetCapablity(2);
                                break;
                            case 9:
                                HwABSUtils.logD("DefaultState MSG_CALL_STATE_RINGING");
                                HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                                HwABSStateMachine.this.resetCapablity(1);
                                break;
                            default:
                                switch (i) {
                                    case 11:
                                    case 12:
                                        HwABSUtils.logD("DefaultState MSG_MODEM_ENTER_CONNECT_STATE");
                                        HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = true;
                                        HwABSStateMachine.this.resetCapablity(1);
                                        break;
                                    case 13:
                                        HwABSUtils.logD("DefaultState MSG_MODEM_EXIT_CONNECT_STATE");
                                        if (HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT) {
                                            HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = false;
                                            HwABSStateMachine.this.resetCapablity(2);
                                            break;
                                        }
                                        break;
                                    case 14:
                                        HwABSUtils.logD("DefaultState MSG_MODEM_ENTER_SEARCHING_STATE");
                                        HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = true;
                                        HwABSStateMachine.this.resetCapablity(1);
                                        this.mData = message.getData();
                                        this.mSubId = this.mData.getInt(HwABSUtils.SUB_ID);
                                        HwABSStateMachine.this.addModemState(this.mSubId);
                                        break;
                                    case 15:
                                        HwABSUtils.logD("DefaultState MSG_MODEM_EXIT_SEARCHING_STATE");
                                        this.mData = message.getData();
                                        this.mSubId = this.mData.getInt(HwABSUtils.SUB_ID);
                                        if (HwABSStateMachine.this.removeModemState(this.mSubId) == 0) {
                                            HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = false;
                                            HwABSStateMachine.this.resetCapablity(2);
                                            break;
                                        }
                                        break;
                                    case 16:
                                        HwABSUtils.logD("DefaultState MSG_WIFI_ANTENNA_PREEMPTED");
                                        break;
                                    default:
                                        StringBuilder stringBuilder2;
                                        switch (i) {
                                            case HwABSUtils.MSG_MODEM_TUNERIC_ACTIVE_RESULT /*33*/:
                                                i = message.arg1;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("DefaultState MSG_MODEM_TUNERIC_ACTIVE_RESULT active_result = ");
                                                stringBuilder2.append(i);
                                                stringBuilder2.append("  RESENT_MODEM_TUNERIC_ACTIVE_TIMES = ");
                                                stringBuilder2.append(HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES);
                                                HwABSUtils.logD(stringBuilder2.toString());
                                                if (i != 1) {
                                                    if (HwABSStateMachine.this.mWifiManager.isWifiEnabled() && HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES < 3) {
                                                        HwABSStateMachine.this.removeMessages(34);
                                                        HwABSStateMachine.this.sendMessageDelayed(34, 5000);
                                                        break;
                                                    }
                                                }
                                                HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES = 0;
                                                break;
                                            case HwABSUtils.MSG_RESEND_TUNERIC_ACTIVE_MSG /*34*/:
                                                HwABSUtils.logD("DefaultState MSG_RESEND_TUNERIC_ACTIVE_MSG");
                                                if (HwABSStateMachine.this.mWifiManager.isWifiEnabled()) {
                                                    HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_ACTIVE, HwABSStateMachine.this.mActiveCallback);
                                                    HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES = HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES + 1;
                                                    break;
                                                }
                                                break;
                                            case HwABSUtils.MSG_MODEM_TUNERIC_IACTIVE_RESULT /*35*/:
                                                i = message.arg1;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("DefaultState MSG_MODEM_TUNERIC_IACTIVE_RESULT iactive_result = ");
                                                stringBuilder2.append(i);
                                                stringBuilder2.append("  RESENT_MODEM_TUNERIC_IACTIVE_TIMES = ");
                                                stringBuilder2.append(HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES);
                                                HwABSUtils.logD(stringBuilder2.toString());
                                                if (i != 1) {
                                                    if (!HwABSStateMachine.this.mWifiManager.isWifiEnabled() && HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES < 3) {
                                                        HwABSStateMachine.this.removeMessages(34);
                                                        HwABSStateMachine.this.sendMessageDelayed(36, 5000);
                                                        break;
                                                    }
                                                }
                                                HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES = 0;
                                                break;
                                            case HwABSUtils.MSG_RESEND_TUNERIC_IACTIVE_MSG /*36*/:
                                                HwABSUtils.logD("DefaultState MSG_RESEND_TUNERIC_IACTIVE_MSG");
                                                if (!HwABSStateMachine.this.mWifiManager.isWifiEnabled()) {
                                                    HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_IACTIVE, HwABSStateMachine.this.mIactiveCallback);
                                                    HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES = HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES + 1;
                                                    break;
                                                }
                                                break;
                                            case HwABSUtils.MSG_BOOT_COMPLETED /*37*/:
                                                if (!HwABSStateMachine.this.mWifiManager.isWifiEnabled()) {
                                                    HwABSUtils.logD("DefaultState send MODEM_TUNERIC_IACTIVE_MSG");
                                                    HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_IACTIVE, HwABSStateMachine.this.mIactiveCallback);
                                                    HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES = 0;
                                                    break;
                                                }
                                                HwABSUtils.logD("DefaultState send MODEM_TUNERIC_ACTIVE_MSG");
                                                HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_ACTIVE, HwABSStateMachine.this.mActiveCallback);
                                                HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES = 0;
                                                HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(HwABSStateMachine.this.mHwABSWiFiHandler.getCurrentCapability());
                                                HwABSStateMachine.this.setBlackListBssid();
                                                break;
                                            case HwABSUtils.MSG_SEL_ENGINE_RESET_COMPLETED /*38*/:
                                                HwABSUtils.logD("DefaultState MSG_SEL_ENGINE_RESET_COMPLETED");
                                                HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mWiFiConnectedState);
                                                HwABSStateMachine.this.sendMessage(1);
                                                break;
                                        }
                                        break;
                                }
                        }
                }
            } else {
                HwABSUtils.logD("DefaultState CMD_WIFI_PAUSE_HANDOVER");
                HwABSStateMachine.this.isPuaseHandover = false;
            }
            return true;
        }
    }

    class MimoState extends State {
        private String mCurrentBSSID = null;
        private String mCurrentSSID = null;
        Bundle mData = null;
        int mSubId = -1;

        MimoState() {
        }

        public void enter() {
            HwABSUtils.logD("enter MimoState");
            HwABSStateMachine.this.setWiFiAntennaMonitor(true);
            HwABSStateMachine.this.mABSMIMOStartTime = System.currentTimeMillis();
            if (HwABSStateMachine.this.isScreenOn()) {
                HwABSStateMachine.this.mABSMIMOScreenOnStartTime = System.currentTimeMillis();
            }
            WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSSID() != null && wifiInfo.getBSSID() != null) {
                this.mCurrentSSID = HwABSCHRManager.getAPSSID(wifiInfo);
                this.mCurrentBSSID = wifiInfo.getBSSID();
            }
        }

        public boolean processMessage(Message message) {
            StringBuilder stringBuilder;
            switch (message.what) {
                case 1:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MimoState MSG_WIFI_CONNECTED mIsSupportVoWIFI = ");
                    stringBuilder.append(HwABSStateMachine.this.mIsSupportVoWIFI);
                    HwABSUtils.logE(stringBuilder.toString());
                    if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                        if (HwABSStateMachine.this.mIsSupportVoWIFI && HwABSStateMachine.this.mHwABSWiFiHandler.isHandoverTimeout()) {
                            HwABSUtils.logE("MimoState MSG_WIFI_CONNECTED handover timeout");
                            HwABSStateMachine.this.updateABSAssociateTimes(0, 1);
                        } else {
                            HwABSStateMachine.this.updateABSAssociateSuccess();
                        }
                        HwABSStateMachine.this.mHwABSWiFiHandler.setIsABSHandover(false);
                        break;
                    }
                    break;
                case 2:
                    HwABSUtils.logD("MimoState MSG_WIFI_DISCONNECTED");
                    if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                        HwABSStateMachine.this.mHwABSWiFiHandler.setIsABSHandover(false);
                        HwABSStateMachine.this.mHwABSCHRManager.uploadABSReassociateExeption();
                        HwABSStateMachine.this.updateABSAssociateTimes(0, 1);
                    }
                    HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mWiFiDisconnectedState);
                    break;
                case 5:
                    HwABSUtils.logE("MimoState MSG_SCREEN_ON");
                    HwABSStateMachine.this.mABSMIMOScreenOnStartTime = System.currentTimeMillis();
                    break;
                case 6:
                    HwABSUtils.logE("MimoState MSG_SCREEN_OFF");
                    if (HwABSStateMachine.this.mABSMIMOScreenOnStartTime != 0) {
                        HwABSStateMachine.this.mHwABSCHRManager.updateABSTime(this.mCurrentSSID, 0, 0, System.currentTimeMillis() - HwABSStateMachine.this.mABSMIMOScreenOnStartTime, 0);
                        HwABSStateMachine.this.mABSMIMOScreenOnStartTime = 0;
                        break;
                    }
                    break;
                case 7:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MimoState MSG_OUTGOING_CALL isAirModeOn =  ");
                    stringBuilder.append(HwABSStateMachine.this.isAirModeOn());
                    HwABSUtils.logE(stringBuilder.toString());
                    if (!HwABSStateMachine.this.isAirModeOn()) {
                        HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                        if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                            if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                                HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                                HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(7);
                                HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(6);
                                HwABSStateMachine.this.mHwABSWiFiHandler.hwABSHandover(1);
                            } else {
                                HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                                HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                            }
                            HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                            break;
                        }
                        HwABSStateMachine.this.mSwitchType = 7;
                        HwABSStateMachine.this.mSwitchEvent = 6;
                        HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                        break;
                    }
                    break;
                case 9:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MimoState MSG_CALL_STATE_RINGING isAirModeOn =  ");
                    stringBuilder.append(HwABSStateMachine.this.isAirModeOn());
                    HwABSUtils.logE(stringBuilder.toString());
                    if (!HwABSStateMachine.this.isAirModeOn()) {
                        HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                        if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                            if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                                HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                                HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(6);
                                HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(6);
                                HwABSStateMachine.this.mHwABSWiFiHandler.hwABSHandover(1);
                            } else {
                                HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                                HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                            }
                            HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                            break;
                        }
                        HwABSStateMachine.this.mSwitchType = 6;
                        HwABSStateMachine.this.mSwitchEvent = 6;
                        HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                        break;
                    }
                    break;
                case 11:
                    HwABSUtils.logE("MimoState MSG_MODEM_ENTER_CONNECT_STATE");
                    HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = true;
                    if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                            HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                            HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(1);
                            HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(1);
                            HwABSStateMachine.this.hwABSWiFiHandover(1);
                        } else {
                            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                            HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                        }
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                        break;
                    }
                    HwABSStateMachine.this.mSwitchType = 1;
                    HwABSStateMachine.this.mSwitchEvent = 1;
                    HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                    break;
                case 12:
                    HwABSUtils.logE("MimoState MSG_MODEM_ENTER_CONNECT_STATE");
                    HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = true;
                    if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                            HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                            HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(2);
                            HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(2);
                            HwABSStateMachine.this.hwABSWiFiHandover(1);
                        } else {
                            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                            HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                        }
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                        break;
                    }
                    HwABSStateMachine.this.mSwitchType = 2;
                    HwABSStateMachine.this.mSwitchEvent = 2;
                    HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                    break;
                case 13:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MimoState MSG_MODEM_EXIT_CONNECT_STATE ANTENNA_STATE_IN_CONNECT = ");
                    stringBuilder.append(HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT);
                    HwABSUtils.logE(stringBuilder.toString());
                    if (HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT) {
                        HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = false;
                        break;
                    }
                    break;
                case 14:
                    HwABSUtils.logE("MimoState MSG_MODEM_ENTER_SEARCHING_STATE");
                    HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = true;
                    this.mSubId = message.getData().getInt(HwABSUtils.SUB_ID);
                    HwABSStateMachine.this.addModemState(this.mSubId);
                    if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                            HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                            HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(3);
                            HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(3);
                            HwABSStateMachine.this.hwABSWiFiHandover(1);
                        } else {
                            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                            HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                        }
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                        break;
                    }
                    HwABSStateMachine.this.mSwitchType = 3;
                    HwABSStateMachine.this.mSwitchEvent = 3;
                    HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                    break;
                    break;
                case 15:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Mimo MSG_MODEM_EXIT_SEARCHING_STATE mModemStateList.size() == ");
                    stringBuilder.append(HwABSStateMachine.this.mModemStateList.size());
                    HwABSUtils.logE(stringBuilder.toString());
                    if (HwABSStateMachine.this.mModemStateList.size() != 0) {
                        this.mData = message.getData();
                        this.mSubId = this.mData.getInt(HwABSUtils.SUB_ID);
                        if (HwABSStateMachine.this.removeModemState(this.mSubId) == 0) {
                            HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = false;
                            break;
                        }
                    }
                    break;
                case 16:
                    HwABSUtils.logE("MimoState MSG_WIFI_ANTENNA_PREEMPTED");
                    HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED = true;
                    if (HwABSStateMachine.this.isScreenOn()) {
                        HwABSStateMachine.this.mSwitchType = 4;
                        HwABSStateMachine.this.mSwitchEvent = 4;
                    } else {
                        HwABSStateMachine.this.mSwitchType = 5;
                        HwABSStateMachine.this.mSwitchEvent = 5;
                    }
                    if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                            HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                            HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(HwABSStateMachine.this.mSwitchType);
                            HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(HwABSStateMachine.this.mSwitchEvent);
                            HwABSStateMachine.this.hwABSWiFiHandover(1);
                        } else {
                            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                            HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                        }
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                        break;
                    }
                    HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                    break;
                    break;
                case 23:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("MIMO MSG_DELAY_SWITCH ANTENNA_STATE_IN_CALL = ");
                    stringBuilder.append(HwABSStateMachine.this.ANTENNA_STATE_IN_CALL);
                    stringBuilder.append(" ANTENNA_STATE_IN_SEARCH = ");
                    stringBuilder.append(HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH);
                    stringBuilder.append(" ANTENNA_STATE_IN_CONNECT = ");
                    stringBuilder.append(HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT);
                    stringBuilder.append(" ANTENNA_STATE_IN_PREEMPTED = ");
                    stringBuilder.append(HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED);
                    HwABSUtils.logD(stringBuilder.toString());
                    if (HwABSStateMachine.this.ANTENNA_STATE_IN_CALL || HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH || HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT || HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("MIMO MSG_DELAY_SWITCH mSwitchType = ");
                        stringBuilder.append(HwABSStateMachine.this.mSwitchType);
                        stringBuilder.append(" mSwitchEvent = ");
                        stringBuilder.append(HwABSStateMachine.this.mSwitchEvent);
                        HwABSUtils.logD(stringBuilder.toString());
                        if (HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() && !HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                            if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                                HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                                HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(HwABSStateMachine.this.mSwitchType);
                                HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(HwABSStateMachine.this.mSwitchEvent);
                                HwABSStateMachine.this.hwABSWiFiHandover(1);
                            } else {
                                HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                                HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(1);
                            }
                            HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                            break;
                        }
                        HwABSStateMachine.this.sendMessageDelayed(23, 1000);
                        break;
                    }
                    break;
                case 24:
                    handleSuppliantComplete();
                    break;
                default:
                    return false;
            }
            return true;
        }

        public void exit() {
            HwABSUtils.logD("exit MimoState");
            HwABSStateMachine.this.removeMessages(23);
            long mimoScreenOnTime = 0;
            long mimoTime = System.currentTimeMillis() - HwABSStateMachine.this.mABSMIMOStartTime;
            if (HwABSStateMachine.this.mABSMIMOScreenOnStartTime != 0) {
                mimoScreenOnTime = System.currentTimeMillis() - HwABSStateMachine.this.mABSMIMOScreenOnStartTime;
            }
            long j = 0;
            HwABSStateMachine.this.mHwABSCHRManager.updateABSTime(this.mCurrentSSID, mimoTime, 0, mimoScreenOnTime, 0);
            HwABSStateMachine.this.mABSMIMOScreenOnStartTime = j;
            HwABSStateMachine.this.mABSMIMOStartTime = j;
        }

        private void handleSuppliantComplete() {
            WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getSSID() != null && wifiInfo.getBSSID() != null) {
                if (this.mCurrentBSSID.equals(wifiInfo.getBSSID()) || !this.mCurrentSSID.equals(HwABSCHRManager.getAPSSID(wifiInfo))) {
                    if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover() && this.mCurrentBSSID.equals(wifiInfo.getBSSID())) {
                        HwABSUtils.logD("mimo reassociate success");
                        HwABSWiFiHandler access$2200 = HwABSStateMachine.this.mHwABSWiFiHandler;
                        HwABSStateMachine.this.mHwABSWiFiHandler;
                        access$2200.setTargetBssid("any");
                        HwABSStateMachine.this.sendMessage(1);
                    }
                } else if (HwABSStateMachine.this.isApInDatabase(wifiInfo.getBSSID())) {
                    HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mMimoState);
                } else {
                    Message msg = new Message();
                    msg.what = 1;
                    HwABSStateMachine.this.deferMessage(msg);
                    HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mWiFiConnectedState);
                }
            }
        }
    }

    class SisoState extends State {
        private String mCurrentBSSID = null;
        private String mCurrentSSID = null;

        SisoState() {
        }

        public void enter() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enter SisoState ANTENNA_STATE_IN_PREEMPTED = ");
            stringBuilder.append(HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED);
            HwABSUtils.logD(stringBuilder.toString());
            HwABSStateMachine.this.setWiFiAntennaMonitor(true);
            if (!HwABSStateMachine.this.isScreenOn()) {
                HwABSStateMachine.this.mABSSISOScreenOnStartTime = System.currentTimeMillis();
            }
            HwABSStateMachine.this.mABSSISOStartTime = System.currentTimeMillis();
            WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
            if (!(wifiInfo == null || wifiInfo.getSSID() == null || wifiInfo.getBSSID() == null)) {
                this.mCurrentSSID = HwABSCHRManager.getAPSSID(wifiInfo);
                this.mCurrentBSSID = wifiInfo.getBSSID();
            }
            if (HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED && HwABSStateMachine.this.isModemStateInIdle()) {
                HwABSStateMachine.this.ANTENNA_STATE_IN_PREEMPTED = false;
                HwABSStateMachine.this.handoverToMIMO();
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 22) {
                HwABSStateMachine.this.handlePowerOffMessage();
                if (HwABSStateMachine.this.isModemStateInIdle()) {
                    HwABSStateMachine.this.handoverToMIMO();
                }
            } else if (i == 24) {
                WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
                if (!(wifiInfo == null || wifiInfo.getSSID() == null || wifiInfo.getBSSID() == null)) {
                    if (this.mCurrentBSSID.equals(wifiInfo.getBSSID()) || !this.mCurrentSSID.equals(HwABSCHRManager.getAPSSID(wifiInfo))) {
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover() && this.mCurrentBSSID.equals(wifiInfo.getBSSID())) {
                            HwABSUtils.logD("siso reassociate success");
                            HwABSWiFiHandler access$2200 = HwABSStateMachine.this.mHwABSWiFiHandler;
                            HwABSStateMachine.this.mHwABSWiFiHandler;
                            access$2200.setTargetBssid("any");
                            HwABSStateMachine.this.sendMessage(1);
                        }
                    } else if (HwABSStateMachine.this.isApInDatabase(wifiInfo.getBSSID())) {
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                    } else {
                        Message msg = new Message();
                        msg.what = 1;
                        HwABSStateMachine.this.deferMessage(msg);
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mWiFiConnectedState);
                    }
                }
            } else if (i == 101) {
                boolean isModemStateIdle = HwABSStateMachine.this.isModemStateInIdle();
                boolean isSIMCardInService = HwABSStateMachine.this.isSIMCardStatusIdle();
                boolean isInBlackList = HwABSStateMachine.this.isAPInBlackList();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SiSOState CMD_WIFI_SWITCH_MIMO isModemStateInIdle = ");
                stringBuilder.append(isModemStateIdle);
                stringBuilder.append(" isSIMCardInService = ");
                stringBuilder.append(isSIMCardInService);
                stringBuilder.append(" isInBlackList = ");
                stringBuilder.append(isInBlackList);
                HwABSUtils.logE(stringBuilder.toString());
                if (!isModemStateIdle || !isSIMCardInService || isInBlackList) {
                    HwABSUtils.logE("SiSOState CMD_WIFI_SWITCH_MIMO keep in SISO");
                } else if (!HwABSStateMachine.this.mHwABSWiFiScenario.isSupInCompleteState() || HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                    HwABSStateMachine.this.sendMessageDelayed(101, 1000);
                } else {
                    if (HwABSStateMachine.this.mHwABSWiFiHandler.isNeedHandover()) {
                        HwABSStateMachine.this.mHwABSCHRManager.initABSHandoverException(8);
                        HwABSStateMachine.this.mHwABSCHRManager.increaseEventStatistics(7);
                        HwABSStateMachine.this.updateABSAssociateTimes(1, 0);
                        HwABSStateMachine.this.hwABSWiFiHandover(2);
                    } else {
                        HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(2);
                        HwABSStateMachine.this.mHwABSWiFiHandler.setABSCurrentState(2);
                    }
                    HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mMimoState);
                }
            } else if (i != 103) {
                StringBuilder stringBuilder2;
                switch (i) {
                    case 1:
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SiSOState MSG_WIFI_CONNECTED mIsSupportVoWIFI = ");
                        stringBuilder2.append(HwABSStateMachine.this.mIsSupportVoWIFI);
                        HwABSUtils.logE(stringBuilder2.toString());
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                            if (HwABSStateMachine.this.mIsSupportVoWIFI && HwABSStateMachine.this.mHwABSWiFiHandler.isHandoverTimeout()) {
                                HwABSUtils.logE("SiSOState MSG_WIFI_CONNECTED handover time out");
                                HwABSStateMachine.this.updateABSAssociateTimes(0, 1);
                            } else {
                                HwABSStateMachine.this.updateABSAssociateSuccess();
                            }
                            HwABSStateMachine.this.mHwABSWiFiHandler.setIsABSHandover(false);
                            break;
                        }
                        break;
                    case 2:
                        HwABSUtils.logD("SiSOState MSG_WIFI_DISCONNECTED");
                        if (HwABSStateMachine.this.mHwABSWiFiHandler.getIsABSHandover()) {
                            HwABSStateMachine.this.mHwABSWiFiHandler.setIsABSHandover(false);
                            HwABSStateMachine.this.mHwABSCHRManager.uploadABSReassociateExeption();
                            HwABSStateMachine.this.updateABSAssociateTimes(0, 1);
                        }
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mWiFiDisconnectedState);
                        break;
                    default:
                        switch (i) {
                            case 5:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("SiSOState MSG_SCREEN_ON isModemStateInIdle = ");
                                stringBuilder2.append(HwABSStateMachine.this.isModemStateInIdle());
                                HwABSUtils.logE(stringBuilder2.toString());
                                if (HwABSStateMachine.this.isModemStateInIdle()) {
                                    if (HwABSStateMachine.this.isInPunishTime()) {
                                        long mOverPunishTime = HwABSStateMachine.this.getPunishTime() - (System.currentTimeMillis() - HwABSStateMachine.this.ABS_LAST_HANDOVER_TIME);
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("SiSOState MSG_SCREEN_ON inpunish time = ");
                                        stringBuilder2.append(mOverPunishTime);
                                        HwABSUtils.logE(stringBuilder2.toString());
                                        if (mOverPunishTime > HwABSStateMachine.ABS_SCREEN_ON_TIME) {
                                            HwABSStateMachine.this.sendHandoverToMIMOMsg(101, mOverPunishTime);
                                        } else {
                                            HwABSStateMachine.this.sendHandoverToMIMOMsg(101, HwABSStateMachine.ABS_SCREEN_ON_TIME);
                                        }
                                    } else {
                                        HwABSStateMachine.this.sendHandoverToMIMOMsg(101, HwABSStateMachine.ABS_SCREEN_ON_TIME);
                                    }
                                }
                                HwABSStateMachine.this.mABSSISOScreenOnStartTime = System.currentTimeMillis();
                                break;
                            case 6:
                                HwABSUtils.logE("SiSOState MSG_SCREEN_OFF");
                                HwABSStateMachine.this.removeMessages(101);
                                if (HwABSStateMachine.this.mABSSISOScreenOnStartTime != 0) {
                                    HwABSStateMachine.this.mHwABSCHRManager.updateABSTime(this.mCurrentSSID, 0, 0, 0, System.currentTimeMillis() - HwABSStateMachine.this.mABSSISOScreenOnStartTime);
                                    HwABSStateMachine.this.mABSSISOScreenOnStartTime = 0;
                                    break;
                                }
                                break;
                            case 7:
                            case 9:
                                HwABSUtils.logD("siso in or out call");
                                HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = true;
                                if (HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT) {
                                    HwABSStateMachine.this.resetABSHandoverTimes();
                                    break;
                                }
                                break;
                            case 8:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("SisoState MSG_ANTENNA_STATE_IDLE ANTENNA_STATE_IN_CALL = ");
                                stringBuilder2.append(HwABSStateMachine.this.ANTENNA_STATE_IN_CALL);
                                HwABSUtils.logE(stringBuilder2.toString());
                                if (HwABSStateMachine.this.ANTENNA_STATE_IN_CALL) {
                                    HwABSStateMachine.this.ANTENNA_STATE_IN_CALL = false;
                                    HwABSStateMachine.this.mIsInCallPunish = true;
                                    HwABSStateMachine.this.handoverToMIMO();
                                    break;
                                }
                                break;
                            default:
                                switch (i) {
                                    case 11:
                                    case 12:
                                        HwABSUtils.logE("SisoState MSG_MODEM_ENTER_CONNECT_STATE");
                                        HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = true;
                                        break;
                                    case 13:
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("SisoState MSG_MODEM_EXIT_CONNECT_STATE ANTENNA_STATE_IN_CONNECT = ");
                                        stringBuilder2.append(HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT);
                                        HwABSUtils.logE(stringBuilder2.toString());
                                        if (HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT) {
                                            HwABSStateMachine.this.ANTENNA_STATE_IN_CONNECT = false;
                                            HwABSStateMachine.this.handoverToMIMO();
                                            break;
                                        }
                                        break;
                                    case 14:
                                        HwABSUtils.logE("SisoState MSG_MODEM_ENTER_SEARCHING_STATE");
                                        HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = true;
                                        HwABSStateMachine.this.removeMessages(101);
                                        HwABSStateMachine.this.addModemState(message.getData().getInt(HwABSUtils.SUB_ID));
                                        break;
                                    case 15:
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("SisoState MSG_MODEM_EXIT_SEARCHING_STATE mModemStateList.size() ==");
                                        stringBuilder2.append(HwABSStateMachine.this.mModemStateList.size());
                                        HwABSUtils.logE(stringBuilder2.toString());
                                        if (HwABSStateMachine.this.mModemStateList.size() != 0) {
                                            Bundle mData = message.getData();
                                            int mSubId = mData.getInt(HwABSUtils.SUB_ID);
                                            int mResult = mData.getInt(HwABSUtils.RES);
                                            if (HwABSStateMachine.this.removeModemState(mSubId) == 0) {
                                                HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH = false;
                                            }
                                            if (!HwABSStateMachine.this.isHaveSIMCard(mSubId)) {
                                                if (!HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH && (mResult == 0 || mResult == 1)) {
                                                    HwABSStateMachine.this.handoverToMIMO();
                                                    break;
                                                }
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("SisoState keep stay in siso, have no sim card ANTENNA_STATE_IN_SEARCH = ");
                                                stringBuilder2.append(HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH);
                                                HwABSUtils.logE(stringBuilder2.toString());
                                                break;
                                            } else if (mResult == 0 && !HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH) {
                                                HwABSStateMachine.this.handoverToMIMO();
                                                break;
                                            } else {
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("SisoState keep stay in siso, have sim card ANTENNA_STATE_IN_SEARCH = ");
                                                stringBuilder2.append(HwABSStateMachine.this.ANTENNA_STATE_IN_SEARCH);
                                                HwABSUtils.logE(stringBuilder2.toString());
                                                break;
                                            }
                                        }
                                        break;
                                    default:
                                        return false;
                                }
                        }
                }
            } else {
                HwABSUtils.logD("SiSOState CMD_WIFI_PAUSE_HANDOVER");
                HwABSStateMachine.this.isPuaseHandover = false;
                HwABSStateMachine.this.handoverToMIMO();
            }
            return true;
        }

        public void exit() {
            HwABSUtils.logD("exit SisoState");
            long sisoScreenOnTime = 0;
            long sisoTime = System.currentTimeMillis() - HwABSStateMachine.this.mABSSISOStartTime;
            if (HwABSStateMachine.this.mABSSISOScreenOnStartTime != 0) {
                sisoScreenOnTime = System.currentTimeMillis() - HwABSStateMachine.this.mABSSISOScreenOnStartTime;
            }
            HwABSStateMachine.this.mHwABSCHRManager.updateABSTime(this.mCurrentSSID, 0, sisoTime, 0, sisoScreenOnTime);
            HwABSStateMachine.this.mABSSISOScreenOnStartTime = 0;
            HwABSStateMachine.this.mABSSISOStartTime = 0;
        }
    }

    class WiFiConnectedState extends State {
        private int mGetApMIMOCapabilityTimes = 0;

        WiFiConnectedState() {
        }

        public void enter() {
            HwABSUtils.logD("enter WiFiConnectedState");
            this.mGetApMIMOCapabilityTimes = 0;
        }

        /* JADX WARNING: Missing block: B:40:0x011a, code skipped:
            if (r4 == null) goto L_0x019f;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 1) {
                switch (i) {
                    case 17:
                        HwABSStateMachine.this.mHwABSWiFiHandler.hwABScheckLinked();
                        break;
                    case 18:
                        HwABSUtils.logE("WiFiConnectedState MSG_WIFI_CHECK_LINK_SUCCESS");
                        if (!HwABSStateMachine.this.isUsingMIMOCapability()) {
                            HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                            break;
                        }
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mMimoState);
                        break;
                    case 19:
                        HwABSUtils.logE("WiFiConnectedState MSG_WIFI_CHECK_LINK_FAILED");
                        WifiInfo wifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
                        if (wifiInfo != null && wifiInfo.getBSSID() != null) {
                            HwABSApInfoData hwABSApInfoData = HwABSStateMachine.this.mHwABSDataBaseManager.getApInfoByBssid(wifiInfo.getBSSID());
                            if (hwABSApInfoData != null) {
                                hwABSApInfoData.mSwitch_siso_type = 2;
                                HwABSStateMachine.this.mHwABSDataBaseManager.addOrUpdateApInfos(hwABSApInfoData);
                                HwABSStateMachine.this.hwABSWiFiHandover(1);
                                break;
                            }
                        }
                        HwABSUtils.logE("MSG_WIFI_CHECK_LINK_FAILED error ");
                        break;
                        break;
                    default:
                        return false;
                }
            }
            HwABSUtils.logE("WiFiConnectedState MSG_WIFI_CONNECTED");
            WifiInfo mWifiInfo = HwABSStateMachine.this.mWifiManager.getConnectionInfo();
            if (mWifiInfo == null || mWifiInfo.getBSSID() == null) {
                HwABSUtils.logE("WiFiConnectedState error ");
            } else {
                i = HwABSStateMachine.this.isAPSupportMIMOCapability(mWifiInfo.getBSSID());
                if (i == -1) {
                    HwABSUtils.logD("isAPSupportMIMOCapability mNetworkDetail == null");
                    if (HwABSStateMachine.this.mHwABSDataBaseManager.getApInfoByBssid(mWifiInfo.getBSSID()) == null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(" It is a hidden AP,delay get scan result mGetApMIMOCapabilityTimes = ");
                        stringBuilder.append(this.mGetApMIMOCapabilityTimes);
                        HwABSUtils.logE(stringBuilder.toString());
                        if (!HwABSStateMachine.this.hasMessages(1) && this.mGetApMIMOCapabilityTimes < 3) {
                            this.mGetApMIMOCapabilityTimes++;
                            HwABSStateMachine.this.sendMessageDelayed(1, 2000);
                        }
                    }
                } else if (i == 0) {
                    HwABSUtils.logE(" It is a siso AP");
                }
                if (!ScanResult.is24GHz(mWifiInfo.getFrequency()) || HwABSStateMachine.this.isMobileAP()) {
                    HwABSUtils.logE(" It is a 5G AP or moblie AP");
                    HwABSStateMachine.this.resetCapablity(2);
                } else {
                    HwABSApInfoData data = HwABSStateMachine.this.mHwABSDataBaseManager.getApInfoByBssid(mWifiInfo.getBSSID());
                    if (data == null) {
                        data = initApInfoData(mWifiInfo);
                    } else {
                        data.mLast_connect_time = System.currentTimeMillis();
                    }
                    HwABSStateMachine.this.mHwABSDataBaseManager.addOrUpdateApInfos(data);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("now capability = ");
                    stringBuilder2.append(HwABSStateMachine.this.mHwABSWiFiHandler.getCurrentCapability());
                    HwABSUtils.logD(stringBuilder2.toString());
                    if (data.mIn_black_List == 1 && HwABSStateMachine.this.mHwABSWiFiHandler.getCurrentCapability() == 2) {
                        HwABSUtils.logD("current AP is in blackList reset capability");
                        HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(1);
                        HwABSStateMachine.this.setBlackListBssid();
                    }
                    if (HwABSStateMachine.this.isUsingMIMOCapability()) {
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mMimoState);
                    } else {
                        HwABSStateMachine.this.transitionTo(HwABSStateMachine.this.mSisoState);
                    }
                }
            }
            return true;
        }

        public void exit() {
            HwABSUtils.logD("exit WiFiConnectedState");
        }

        private HwABSApInfoData initApInfoData(WifiInfo wifiInfo) {
            int authType = 0;
            WifiConfiguration sWifiConfiguration = getCurrntConfig(wifiInfo);
            if (sWifiConfiguration != null && sWifiConfiguration.allowedKeyManagement.cardinality() <= 1) {
                authType = sWifiConfiguration.getAuthType();
            }
            return new HwABSApInfoData(wifiInfo.getBSSID(), HwABSCHRManager.getAPSSID(wifiInfo), 2, 2, authType, 0, 0, 0, 0, System.currentTimeMillis());
        }

        private WifiConfiguration getCurrntConfig(WifiInfo wifiInfo) {
            List<WifiConfiguration> configNetworks = HwABSStateMachine.this.mWifiManager.getConfiguredNetworks();
            if (configNetworks == null || configNetworks.size() == 0) {
                return null;
            }
            for (WifiConfiguration nextConfig : configNetworks) {
                if (isValidConfig(nextConfig) && nextConfig.networkId == wifiInfo.getNetworkId()) {
                    return nextConfig;
                }
            }
            return null;
        }

        private boolean isValidConfig(WifiConfiguration config) {
            boolean z = false;
            if (config == null || config.SSID == null) {
                return false;
            }
            if (config.allowedKeyManagement.cardinality() <= 1) {
                z = true;
            }
            return z;
        }
    }

    class WiFiDisableState extends State {
        WiFiDisableState() {
        }

        public void enter() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enter WiFiDisableState ABS_HANDOVER_TIMES = ");
            stringBuilder.append(HwABSStateMachine.this.ABS_HANDOVER_TIMES);
            HwABSUtils.logD(stringBuilder.toString());
            if (HwABSStateMachine.this.isScreenOn()) {
                HwABSStateMachine.this.ABS_HANDOVER_TIMES = 0;
            }
            HwABSUtils.logD("WiFiDisableState send MODEM_TUNERIC_IACTIVE_MSG");
            HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_IACTIVE, HwABSStateMachine.this.mIactiveCallback);
            HwABSStateMachine.this.RESENT_MODEM_TUNERIC_IACTIVE_TIMES = 0;
        }

        public boolean processMessage(Message message) {
            StringBuilder stringBuilder;
            int i = message.what;
            if (i != 4) {
                switch (i) {
                    case 1:
                    case 2:
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("WiFiDisableState message.what = ");
                        stringBuilder.append(message.what);
                        HwABSUtils.logD(stringBuilder.toString());
                        return false;
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("WiFiDisableState handle message.what = ");
            stringBuilder.append(message.what);
            HwABSUtils.logD(stringBuilder.toString());
            return true;
        }

        public void exit() {
            HwABSUtils.logD("exit WiFiDisableState");
        }
    }

    static class WiFiDisconnectedState extends State {
        WiFiDisconnectedState() {
        }

        public void enter() {
            HwABSUtils.logD("enter WiFiDisconnectedState");
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 2) {
                HwABSUtils.logD("WiFiDisconnectedState MSG_WIFI_DISCONNECTED");
                return true;
            } else if (i != 4) {
                return false;
            } else {
                HwABSUtils.logD("WiFiDisconnectedState MSG_WIFI_DISABLE");
                return false;
            }
        }

        public void exit() {
            HwABSUtils.logD("exit WiFiDisconnectedState");
        }
    }

    class WiFiEnableState extends State {
        WiFiEnableState() {
        }

        public void enter() {
            HwABSUtils.logD("enter WiFiEnableState");
            HwABSStateMachine.this.mHwABSWiFiHandler.setAPCapability(HwABSStateMachine.this.mHwABSWiFiHandler.getCurrentCapability());
            HwABSUtils.logD("WiFiEnableState send MODEM_TUNERIC_ACTIVE_MSG");
            HwTelephonyManagerInner.getDefault().notifyCModemStatus(HwABSStateMachine.this.MODEM_TUNERIC_ACTIVE, HwABSStateMachine.this.mActiveCallback);
            HwABSStateMachine.this.RESENT_MODEM_TUNERIC_ACTIVE_TIMES = 0;
            HwABSStateMachine.this.setBlackListBssid();
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case 3:
                    HwABSUtils.logD("WiFiEnableState MSG_WIFI_ENABLED");
                    return true;
                case 4:
                    HwABSUtils.logD("WiFiDisconnectedState MSG_WIFI_DISABLE");
                    return false;
                default:
                    return false;
            }
        }

        public void exit() {
            HwABSUtils.logD("exit WiFiEnableState");
        }
    }

    public static HwABSStateMachine createHwABSStateMachine(Context context, WifiStateMachine wifiStateMachine) {
        if (mHwABSStateMachine == null) {
            mHwABSStateMachine = new HwABSStateMachine(context, wifiStateMachine);
        }
        return mHwABSStateMachine;
    }

    private HwABSStateMachine(Context context, WifiStateMachine wifiStateMachine) {
        super("HwABSStateMachine");
        this.mContext = context;
        this.mHwABSDataBaseManager = HwABSDataBaseManager.getInstance(context);
        this.mHwABSWiFiScenario = new HwABSWiFiScenario(context, getHandler());
        HwABSModemScenario hwABSModemScenario = new HwABSModemScenario(context, getHandler());
        this.mHwABSWiFiHandler = new HwABSWiFiHandler(context, getHandler(), wifiStateMachine);
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mHwABSCHRManager = HwABSCHRManager.getInstance(context);
        addState(this.mDefaultState);
        addState(this.mWiFiEnableState, this.mDefaultState);
        addState(this.mWiFiDisableState, this.mDefaultState);
        addState(this.mWiFiConnectedState, this.mWiFiEnableState);
        addState(this.mWiFiDisconnectedState, this.mWiFiEnableState);
        addState(this.mMimoState, this.mWiFiEnableState);
        addState(this.mSisoState, this.mWiFiEnableState);
        setInitialState(this.mDefaultState);
        start();
    }

    public void onStart() {
        this.mHwABSWiFiScenario.startMonitor();
    }

    public boolean isABSSwitching() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isABSSwitching isSwitching = ");
        stringBuilder.append(this.isSwitching);
        HwABSUtils.logE(stringBuilder.toString());
        return this.isSwitching;
    }

    private NetworkDetail getNetWorkDetail(String bssid) {
        NetworkDetail detail = null;
        for (ScanResult result : this.mWifiManager.getScanResults()) {
            if (result.BSSID.equals(bssid)) {
                detail = ScanResultUtil.toScanDetail(result).getNetworkDetail();
            }
        }
        return detail;
    }

    private boolean isUsingMIMOCapability() {
        if (this.mHwABSWiFiHandler.getCurrentCapability() == 2) {
            return true;
        }
        return false;
    }

    private int isAPSupportMIMOCapability(String bssid) {
        NetworkDetail mNetworkDetail = getNetWorkDetail(bssid);
        if (mNetworkDetail == null) {
            return -1;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isAPSupportMIMOCapability mNetworkDetail.getStream1() = ");
        stringBuilder.append(mNetworkDetail.getStream1());
        stringBuilder.append(" mNetworkDetail.getStream2() = ");
        stringBuilder.append(mNetworkDetail.getStream2());
        stringBuilder.append(" mNetworkDetail.getStream3() = ");
        stringBuilder.append(mNetworkDetail.getStream3());
        stringBuilder.append(" mNetworkDetail.getStream4() = ");
        stringBuilder.append(mNetworkDetail.getStream4());
        HwABSUtils.logD(stringBuilder.toString());
        if (((mNetworkDetail.getStream1() + mNetworkDetail.getStream2()) + mNetworkDetail.getStream3()) + mNetworkDetail.getStream4() >= 2) {
            return 1;
        }
        return 0;
    }

    private void hwABSWiFiHandover(int capability) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hwABSWiFiHandover capability = ");
        stringBuilder.append(capability);
        HwABSUtils.logD(stringBuilder.toString());
        if (capability == 1) {
            setPunishTime();
            updateABSHandoverTime();
        }
        this.mHwABSWiFiHandler.hwABSHandover(capability);
    }

    private void setPunishTime() {
        if (this.ABS_LAST_HANDOVER_TIME == 0 || System.currentTimeMillis() - this.ABS_LAST_HANDOVER_TIME > ABS_INTERVAL_TIME) {
            this.ABS_HANDOVER_TIMES = 1;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPunishTime reset times ABS_HANDOVER_TIMES = ");
            stringBuilder.append(this.ABS_HANDOVER_TIMES);
            HwABSUtils.logD(stringBuilder.toString());
        } else {
            this.ABS_HANDOVER_TIMES++;
            if (this.ABS_HANDOVER_TIMES == 10) {
                this.mHwABSCHRManager.increaseEventStatistics(8);
            } else if (this.ABS_HANDOVER_TIMES >= 10) {
                HwABSCHRStatistics record = this.mHwABSCHRManager.getStatisticsInfo();
                if (record != null && record.max_ping_pong_times < this.ABS_HANDOVER_TIMES) {
                    record.max_ping_pong_times = this.ABS_HANDOVER_TIMES;
                    this.mHwABSCHRManager.updateCHRInfo(record);
                }
            }
        }
        this.ABS_LAST_HANDOVER_TIME = System.currentTimeMillis();
    }

    private boolean isHaveSIMCard(int subID) {
        int cardState = this.mTelephonyManager.getSimState(subID);
        StringBuilder stringBuilder;
        if (cardState == 5) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("isHaveSIMCard subID = ");
            stringBuilder.append(subID);
            stringBuilder.append("  cardState = SIM_STATE_READY");
            HwABSUtils.logD(stringBuilder.toString());
            return true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("isHaveSIMCard subID = ");
        stringBuilder.append(subID);
        stringBuilder.append("  cardState = ");
        stringBuilder.append(cardState);
        HwABSUtils.logD(stringBuilder.toString());
        return false;
    }

    private boolean isSIMCardStatusIdle() {
        boolean isCardReady = false;
        int phoneNum = this.mTelephonyManager.getPhoneCount();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSIMCardStatusIdle phoneNum = ");
        stringBuilder.append(phoneNum);
        HwABSUtils.logD(stringBuilder.toString());
        if (phoneNum == 0) {
            return true;
        }
        for (int i = 0; i < phoneNum; i++) {
            if (this.mTelephonyManager.getSimState(i) == 5) {
                isCardReady = true;
                break;
            }
        }
        if (isCardReady) {
            return compareSIMStatusWithCardReady(phoneNum);
        }
        HwABSUtils.logD("isSIMCardStatusIdle return true");
        return true;
    }

    private boolean compareSIMStatusWithCardReady(int cardNum) {
        List<Integer> statusList = new ArrayList();
        if (cardNum == 0) {
            return true;
        }
        int subId;
        int voiceState = 0;
        for (subId = 0; subId < cardNum; subId++) {
            int cardState = this.mTelephonyManager.getSimState(subId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("compareSIMStatusWithCardReady subId = ");
            stringBuilder.append(subId);
            stringBuilder.append(" cardState = ");
            stringBuilder.append(cardState);
            HwABSUtils.logD(stringBuilder.toString());
            if (cardState != 5) {
                statusList.add(Integer.valueOf(2));
            } else {
                ServiceState serviceState = this.mTelephonyManager.getServiceStateForSubscriber(subId);
                if (serviceState == null) {
                    statusList.add(Integer.valueOf(1));
                } else {
                    voiceState = serviceState.getState();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("compareSIMStatusWithCardReady subId = ");
                    stringBuilder.append(subId);
                    stringBuilder.append(" voiceState = ");
                    stringBuilder.append(voiceState);
                    HwABSUtils.logD(stringBuilder.toString());
                    if (voiceState == 0 || voiceState == 3) {
                        statusList.add(Integer.valueOf(2));
                    } else {
                        statusList.add(Integer.valueOf(1));
                    }
                }
            }
        }
        for (subId = 0; subId < statusList.size(); subId++) {
            if (((Integer) statusList.get(subId)).intValue() != 2) {
                HwABSUtils.logD("compareSIMStatusWithCardReady return false");
                return false;
            }
        }
        HwABSUtils.logD("compareSIMStatusWithCardReady return true");
        return true;
    }

    private void setWiFiAntennaMonitor(boolean enable) {
        if (enable) {
            HwABSUtils.logD("setWiFiAntennaMonitor enable");
        } else {
            HwABSUtils.logD("setWiFiAntennaMonitor disable");
        }
    }

    private boolean isScreenOn() {
        if (((PowerManager) this.mContext.getSystemService("power")).isScreenOn()) {
            return true;
        }
        return false;
    }

    private void resetCapablity(int capablity) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resetCapablity capablity = ");
        stringBuilder.append(capablity);
        HwABSUtils.logD(stringBuilder.toString());
        if (capablity != 2) {
            this.mHwABSWiFiHandler.setAPCapability(capablity);
            this.mHwABSWiFiHandler.setABSCurrentState(capablity);
        } else if (isModemStateInIdle() && !isInPunishTime()) {
            this.mHwABSWiFiHandler.setAPCapability(capablity);
            this.mHwABSWiFiHandler.setABSCurrentState(capablity);
        }
    }

    private boolean isModemStateInIdle() {
        if (this.ANTENNA_STATE_IN_CALL || this.ANTENNA_STATE_IN_SEARCH || this.ANTENNA_STATE_IN_CONNECT || !isScreenOn() || this.isPuaseHandover) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isModemStateInIdle return false ANTENNA_STATE_IN_CALL = ");
            stringBuilder.append(this.ANTENNA_STATE_IN_CALL);
            stringBuilder.append("  ANTENNA_STATE_IN_SEARCH = ");
            stringBuilder.append(this.ANTENNA_STATE_IN_SEARCH);
            stringBuilder.append("  ANTENNA_STATE_IN_CONNECT = ");
            stringBuilder.append(this.ANTENNA_STATE_IN_CONNECT);
            stringBuilder.append(" isScreenOn() = ");
            stringBuilder.append(isScreenOn());
            stringBuilder.append(" isPuaseHandover = ");
            stringBuilder.append(this.isPuaseHandover);
            HwABSUtils.logD(stringBuilder.toString());
            return false;
        }
        HwABSUtils.logD("isModemStateInIdle return true");
        return true;
    }

    private void addModemState(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addModemState subId = ");
        stringBuilder.append(subId);
        HwABSUtils.logD(stringBuilder.toString());
        if (this.mModemStateList.size() == 0) {
            this.mModemStateList.add(Integer.valueOf(subId));
        } else {
            int i = 0;
            while (i < this.mModemStateList.size()) {
                if (((Integer) this.mModemStateList.get(i)).intValue() != subId) {
                    i++;
                } else {
                    return;
                }
            }
            this.mModemStateList.add(Integer.valueOf(subId));
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("addModemState size = ");
        stringBuilder.append(this.mModemStateList.size());
        HwABSUtils.logD(stringBuilder.toString());
    }

    private int removeModemState(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeModemState size = ");
        stringBuilder.append(this.mModemStateList.size());
        stringBuilder.append(" subId = ");
        stringBuilder.append(subId);
        HwABSUtils.logD(stringBuilder.toString());
        int i = 0;
        if (this.mModemStateList.size() == 0) {
            return 0;
        }
        int flag = -1;
        while (i < this.mModemStateList.size()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("removeModemState mModemStateList.get(i) = ");
            stringBuilder2.append(this.mModemStateList.get(i));
            stringBuilder2.append(" subId = ");
            stringBuilder2.append(subId);
            HwABSUtils.logD(stringBuilder2.toString());
            if (((Integer) this.mModemStateList.get(i)).intValue() == subId) {
                flag = i;
                break;
            }
            i++;
        }
        if (flag != -1) {
            this.mModemStateList.remove(flag);
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("removeModemState size = ");
        stringBuilder3.append(this.mModemStateList.size());
        HwABSUtils.logD(stringBuilder3.toString());
        return this.mModemStateList.size();
    }

    private boolean isInPunishTime() {
        long sPunishTim = getPunishTime();
        if (this.ABS_LAST_HANDOVER_TIME > System.currentTimeMillis()) {
            this.ABS_LAST_HANDOVER_TIME = System.currentTimeMillis();
        }
        if (System.currentTimeMillis() - this.ABS_LAST_HANDOVER_TIME > sPunishTim) {
            HwABSUtils.logD("isInPunishTime is in not in punish");
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isInPunishTime is in punish  sPunishTim =");
        stringBuilder.append((this.ABS_LAST_HANDOVER_TIME + sPunishTim) - System.currentTimeMillis());
        HwABSUtils.logD(stringBuilder.toString());
        return true;
    }

    private long getPunishTime() {
        long sPunishTim = ((long) (this.ABS_HANDOVER_TIMES * this.ABS_HANDOVER_TIMES)) * 60000;
        if (sPunishTim > ABS_INTERVAL_TIME) {
            return ABS_INTERVAL_TIME;
        }
        return sPunishTim;
    }

    private void handoverToMIMO() {
        HwABSUtils.logD("handoverToMIMO");
        if (isModemStateInIdle()) {
            if (hasMessages(101)) {
                removeMessages(101);
                HwABSUtils.logD("handoverToMIMO is already have message remove it");
            }
            if (isInPunishTime()) {
                long mOverPunishTime = getPunishTime() - (System.currentTimeMillis() - this.ABS_LAST_HANDOVER_TIME);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handoverToMIMO mOverPunishTime = ");
                stringBuilder.append(mOverPunishTime);
                stringBuilder.append(" mIsInCallPunish = ");
                stringBuilder.append(this.mIsInCallPunish);
                HwABSUtils.logD(stringBuilder.toString());
                if (!this.mIsInCallPunish || mOverPunishTime >= 60000) {
                    sendMessageDelayed(101, mOverPunishTime);
                } else {
                    HwABSUtils.logD("handoverToMIMO reset punish time  = 60000");
                    sendMessageDelayed(101, 60000);
                }
            } else if (this.mIsInCallPunish) {
                HwABSUtils.logD("handoverToMIMO mIsInCallPunish punish time  = 60000");
                sendMessageDelayed(101, 60000);
            } else {
                sendMessageDelayed(101, 2000);
            }
            this.mIsInCallPunish = false;
            return;
        }
        HwABSUtils.logD("handoverToMIMO is not in idle ignore it");
    }

    private void sendHandoverToMIMOMsg(int msg, long time) {
        if (hasMessages(msg)) {
            removeMessages(msg);
        }
        sendMessageDelayed(msg, time);
    }

    private boolean isAirModeOn() {
        boolean z = false;
        if (this.mContext == null) {
            return false;
        }
        if (System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
            z = true;
        }
        return z;
    }

    private List<Integer> getPowerOffSIMSubId() {
        List<Integer> subId = new ArrayList();
        int phoneNum = this.mTelephonyManager.getPhoneCount();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPowerOffSIMSubId phoneNum = ");
        stringBuilder.append(phoneNum);
        HwABSUtils.logD(stringBuilder.toString());
        if (phoneNum == 0) {
            return subId;
        }
        for (int i = 0; i < phoneNum; i++) {
            ServiceState serviceState = this.mTelephonyManager.getServiceStateForSubscriber(i);
            if (serviceState != null) {
                int voiceState = serviceState.getState();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getPowerOffSIMSubId subID = ");
                stringBuilder2.append(i);
                stringBuilder2.append(" voiceState = ");
                stringBuilder2.append(voiceState);
                HwABSUtils.logD(stringBuilder2.toString());
                if (voiceState == 3) {
                    subId.add(Integer.valueOf(i));
                }
            }
        }
        return subId;
    }

    private void handlePowerOffMessage() {
        if (this.ANTENNA_STATE_IN_SEARCH) {
            List<Integer> list = getPowerOffSIMSubId();
            if (list.size() != 0) {
                for (Integer i : list) {
                    removeModemState(i.intValue());
                }
                if (this.mModemStateList.size() == 0) {
                    this.ANTENNA_STATE_IN_SEARCH = false;
                }
            }
        }
        if (this.ANTENNA_STATE_IN_CONNECT) {
            this.ANTENNA_STATE_IN_CONNECT = false;
        }
    }

    private boolean isMobileAP() {
        if (this.mContext != null) {
            return HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(this.mContext);
        }
        return false;
    }

    private void updateABSAssociateTimes(int associateTimes, int associateFailedTimes) {
        String bssid;
        String ssid;
        if (associateTimes == 1) {
            WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
            if (mWifiInfo == null || mWifiInfo.getBSSID() == null || mWifiInfo.getSSID() == null) {
                HwABSUtils.logE("updateABSAssociateTimes mWifiInfo error");
                return;
            }
            bssid = mWifiInfo.getBSSID();
            ssid = HwABSCHRManager.getAPSSID(mWifiInfo);
            this.mAssociateSSID = ssid;
            this.mAssociateBSSID = bssid;
        } else {
            ssid = this.mAssociateSSID;
            bssid = this.mAssociateBSSID;
        }
        HwABSApInfoData hwABSApInfoData = this.mHwABSDataBaseManager.getApInfoByBssid(bssid);
        if (hwABSApInfoData != null) {
            int blackListStatus = hwABSApInfoData.mIn_black_List;
            hwABSApInfoData.mReassociate_times += associateTimes;
            hwABSApInfoData.mFailed_times += associateFailedTimes;
            if (associateFailedTimes != 0) {
                updateABSAssociateFailedEvent(hwABSApInfoData);
            }
            this.mHwABSDataBaseManager.addOrUpdateApInfos(hwABSApInfoData);
            if (blackListStatus == 0 && hwABSApInfoData.mIn_black_List == 1) {
                setBlackListBssid();
                uploadBlackListException(hwABSApInfoData);
            }
        } else {
            HwABSUtils.logE("updateABSAssociateTimes error!!");
        }
        this.mHwABSCHRManager.updateCHRAssociateTimes(ssid, associateTimes, associateFailedTimes);
    }

    private void updateABSAssociateFailedEvent(HwABSApInfoData data) {
        int continuousTimes;
        int highFailedRate;
        int lowFailedRate;
        int failedRate = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateABSAssociateFailedEvent mIsSupportVoWIFI = ");
        stringBuilder.append(this.mIsSupportVoWIFI);
        HwABSUtils.logE(stringBuilder.toString());
        if (this.mIsSupportVoWIFI) {
            continuousTimes = 2;
            highFailedRate = 5;
            lowFailedRate = 15;
        } else {
            continuousTimes = 3;
            highFailedRate = 10;
            lowFailedRate = 30;
        }
        data.mContinuous_failure_times++;
        if (data.mContinuous_failure_times >= continuousTimes) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateABSAssociateFailedEvent mContinuous_failure_times = ");
            stringBuilder.append(data.mContinuous_failure_times);
            HwABSUtils.logE(stringBuilder.toString());
            data.mIn_black_List = 1;
            this.mAddBlackListReason = 1;
            return;
        }
        if (data.mReassociate_times > 50) {
            failedRate = highFailedRate;
        } else if (data.mReassociate_times > 10) {
            failedRate = lowFailedRate;
        }
        int temp = (data.mFailed_times * 100) / data.mReassociate_times;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("updateABSAssociateFailedEvent temp = ");
        stringBuilder2.append(temp);
        stringBuilder2.append(" failedRate = ");
        stringBuilder2.append(failedRate);
        HwABSUtils.logE(stringBuilder2.toString());
        if (failedRate > 0 && temp > failedRate) {
            data.mIn_black_List = 1;
            this.mAddBlackListReason = 2;
        } else if (isHandoverTooMuch(data.mBssid)) {
            HwABSUtils.logE("updateABSAssociateFailedEvent isHandoverTooMach");
            data.mIn_black_List = 1;
            data.mSwitch_siso_type = 15;
            this.mAddBlackListReason = 3;
        }
    }

    private boolean isAPInBlackList() {
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        boolean z = false;
        if (mWifiInfo == null || mWifiInfo.getBSSID() == null) {
            HwABSUtils.logE("isAPInBlackList mWifiInfo error");
            return false;
        }
        HwABSApInfoData hwABSApInfoData = this.mHwABSDataBaseManager.getApInfoByBssid(mWifiInfo.getBSSID());
        if (hwABSApInfoData == null) {
            return false;
        }
        if (hwABSApInfoData.mIn_black_List == 1) {
            z = true;
        }
        return z;
    }

    private void updateABSAssociateSuccess() {
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (mWifiInfo == null || mWifiInfo.getBSSID() == null) {
            HwABSUtils.logE("updateABSAssociateSuccess mWifiInfo error");
            return;
        }
        HwABSApInfoData hwABSApInfoData = this.mHwABSDataBaseManager.getApInfoByBssid(mWifiInfo.getBSSID());
        if (hwABSApInfoData != null) {
            hwABSApInfoData.mContinuous_failure_times = 0;
            this.mHwABSDataBaseManager.addOrUpdateApInfos(hwABSApInfoData);
        }
    }

    public void setBlackListBssid() {
        StringBuilder blackList = new StringBuilder();
        List<HwABSApInfoData> lists = initBlackListDate();
        if (lists.size() != 0) {
            for (HwABSApInfoData data : lists) {
                blackList.append(data.mBssid);
                blackList.append(";");
            }
            this.mHwABSWiFiHandler.setABSBlackList(blackList.toString());
        }
    }

    private List<HwABSApInfoData> initBlackListDate() {
        List<HwABSApInfoData> lists = this.mHwABSDataBaseManager.getApInfoInBlackList();
        if (lists.size() <= 10) {
            return lists;
        }
        return seleteBlackApInfo(lists);
    }

    private List<HwABSApInfoData> seleteBlackApInfo(List<HwABSApInfoData> lists) {
        int size;
        List<HwABSApInfoData> result = new ArrayList();
        Collections.sort(lists);
        Collections.reverse(lists);
        if (lists.size() <= 10) {
            size = lists.size();
        } else {
            size = 10;
        }
        for (int i = 0; i < size; i++) {
            result.add((HwABSApInfoData) lists.get(i));
        }
        return result;
    }

    private boolean isApInDatabase(String bssid) {
        if (this.mHwABSDataBaseManager.getApInfoByBssid(bssid) != null) {
            return true;
        }
        return false;
    }

    private void uploadBlackListException(HwABSApInfoData data) {
        List<HwABSApInfoData> lists = this.mHwABSDataBaseManager.getAllApInfo();
        List<HwABSApInfoData> blacklists = this.mHwABSDataBaseManager.getApInfoInBlackList();
        HwABSCHRBlackListEvent event = new HwABSCHRBlackListEvent();
        event.mABSApSsid = data.mSsid;
        event.mABSApBssid = data.mBssid;
        event.mABSAddReason = this.mAddBlackListReason;
        event.mABSSuportVoWifi = this.mIsSupportVoWIFI;
        event.mABSSwitchTimes = data.mReassociate_times;
        event.mABSFailedTimes = data.mFailed_times;
        if (lists != null) {
            event.mABSTotalNum = lists.size();
        }
        if (blacklists != null) {
            event.mABSBlackListNum = blacklists.size();
        }
        this.mHwABSCHRManager.uploadBlackListException(event);
    }

    public void notifySelEngineEnableWiFi() {
        HwABSUtils.logD("notifySelEngineEnableWiFi");
        this.mHwABSWiFiHandler.setAPCapability(this.mHwABSWiFiHandler.getCurrentCapability());
    }

    public void notifySelEngineResetCompelete() {
        HwABSUtils.logD("notifySelEngineResetCompelete");
        sendMessage(38);
    }

    public void puaseABSHandover() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("puaseABSHandover, isPuaseHandover =");
        stringBuilder.append(this.isPuaseHandover);
        HwABSUtils.logD(stringBuilder.toString());
        if (!this.isPuaseHandover) {
            this.isPuaseHandover = true;
        } else if (hasMessages(103)) {
            removeMessages(103);
            HwABSUtils.logD("puaseABSHandover is already have message remove it");
        }
    }

    public void restartABSHandover() {
        if (this.isPuaseHandover && !hasMessages(103)) {
            HwABSUtils.logD("restartABSHandover send delay message ");
            sendMessageDelayed(103, (long) this.RESTART_ABS_TIME);
        }
    }

    private long getTimesMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(11, 0);
        cal.set(13, 0);
        cal.set(12, 0);
        cal.set(14, 0);
        return cal.getTimeInMillis();
    }

    private boolean isInOneDay(long now) {
        long startTime = getTimesMorning();
        long endTime = ONEDAYA_TIME + startTime;
        if (startTime > now || now > endTime) {
            return false;
        }
        return true;
    }

    private void updateABSHandoverTime() {
        long curTime = System.currentTimeMillis();
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        if (info == null || info.getBSSID() == null) {
            HwABSUtils.logE("updateABSHandoverTime error ");
        } else if (this.ANTENNA_STATE_IN_PREEMPTED || this.ANTENNA_STATE_IN_SEARCH || this.ANTENNA_STATE_IN_CONNECT) {
            APHandoverInfo curApInfo;
            if (this.mAPHandoverInfoList.containsKey(info.getBSSID())) {
                curApInfo = (APHandoverInfo) this.mAPHandoverInfoList.get(info.getBSSID());
                if (curApInfo != null) {
                    if (isInOneDay(curApInfo.lastTime)) {
                        curApInfo.mHandoverTimes++;
                        curApInfo.lastTime = curTime;
                    } else {
                        HwABSUtils.logE("updateABSHandoverTime not in one day");
                        curApInfo.mHandoverTimes = 1;
                        curApInfo.lastTime = curTime;
                        removeABSHandoverTimes();
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateABSHandoverTime curApInfo.mHandoverTimes = ");
                    stringBuilder.append(curApInfo.mHandoverTimes);
                    HwABSUtils.logE(stringBuilder.toString());
                    this.mAPHandoverInfoList.put(info.getBSSID(), curApInfo);
                    return;
                }
                HwABSUtils.logE("updateABSHandoverTime curApInfo == null");
                this.mAPHandoverInfoList.remove(info.getBSSID());
            }
            curApInfo = new APHandoverInfo();
            curApInfo.mHandoverTimes = 1;
            curApInfo.lastTime = curTime;
            this.mAPHandoverInfoList.put(info.getBSSID(), curApInfo);
        } else {
            HwABSUtils.logE("updateABSHandoverTime do not mach type ");
        }
    }

    private void resetABSHandoverTimes() {
        long curTime = System.currentTimeMillis();
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        if (info == null || info.getBSSID() == null) {
            HwABSUtils.logE("resetABSHandoverTimes error ");
            return;
        }
        if (this.mAPHandoverInfoList.containsKey(info.getBSSID())) {
            APHandoverInfo curApInfo = (APHandoverInfo) this.mAPHandoverInfoList.get(info.getBSSID());
            if (curApInfo != null && curApInfo.mHandoverTimes >= 1) {
                HwABSUtils.logE("resetABSHandoverTimes reset ");
                curApInfo.mHandoverTimes--;
                curApInfo.lastTime = curTime;
                this.mAPHandoverInfoList.put(info.getBSSID(), curApInfo);
            }
        }
    }

    private boolean isHandoverTooMuch(String bssid) {
        if (bssid != null && this.mAPHandoverInfoList.containsKey(bssid)) {
            APHandoverInfo curApInfo = (APHandoverInfo) this.mAPHandoverInfoList.get(bssid);
            if (curApInfo == null) {
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isHandoverTooMach mHandoverTimes = ");
            stringBuilder.append(curApInfo.mHandoverTimes);
            HwABSUtils.logE(stringBuilder.toString());
            if (curApInfo.mHandoverTimes >= 15) {
                return true;
            }
        }
        return false;
    }

    private void removeABSHandoverTimes() {
        String bssidKey;
        HwABSUtils.logE("removeABSHandoverTimes");
        List<String> strArray = new ArrayList();
        for (Entry entry : this.mAPHandoverInfoList.entrySet()) {
            bssidKey = (String) entry.getKey();
            if (!isInOneDay(((APHandoverInfo) entry.getValue()).lastTime)) {
                strArray.add(bssidKey);
            }
        }
        for (String bssidKey2 : strArray) {
            this.mAPHandoverInfoList.remove(bssidKey2);
        }
    }
}
