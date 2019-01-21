package com.android.internal.telephony.dataconnection;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MultiSimVariants;
import com.android.ims.HwImsManagerInner;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConfig;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import com.android.internal.telephony.fullnetwork.HwFullNetworkManager;
import com.android.internal.telephony.vsim.HwVSimUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

public class InCallDataStateMachine extends StateMachine {
    public static final String ACTION_HW_DSDS_MODE_STATE = "com.huawei.action.ACTION_HW_DSDS_MODE_STATE";
    private static final String ACTION_INCALL_SCREEN = "InCallScreenIsForegroundActivity";
    private static final boolean DISABLE_GW_PS_ATTACH = SystemProperties.getBoolean("ro.odm.disable_m1_gw_ps_attach", false);
    private static final int DSDS2 = 0;
    private static final int DSDS3 = 1;
    private static final String DSDS_KEY = "dsdsmode";
    private static final int DUAL_SIM_NUM = 2;
    private static final int EVENT_DATA_CONNECTED = 5;
    private static final int EVENT_DATA_DISCONNECTED = 4;
    private static final int EVENT_DSDS_MODE_CHANGE = 9;
    private static final int EVENT_INCALLUI_BACKGROUND = 7;
    private static final int EVENT_INCALL_DATA_SETTINGS_OFF = 1;
    private static final int EVENT_INCALL_DATA_SETTINGS_ON = 0;
    private static final int EVENT_USER_DISABLE_DATA = 6;
    private static final int EVENT_VOICE_CALL_ACTIVE = 8;
    private static final int EVENT_VOICE_CALL_ENDED = 3;
    private static final int EVENT_VOICE_CALL_STARTED = 2;
    private static final String LOG_TAG = "InCallDataSM";
    private static final int PHONE_ID_0 = 0;
    private static final int PHONE_ID_1 = 1;
    private static final boolean PROP_DEL_DEFAULT_LINK = SystemProperties.getBoolean("ro.config.del_default_link", false);
    private static final String SETTINGS_INCALL_DATA_SWITCH = "incall_data_switch";
    private static final int SWITCH_OFF = 0;
    private static final int SWITCH_ON = 1;
    private boolean isInCallUIForeground = false;
    private ActivatedSlaveState mActivatedSlaveState = new ActivatedSlaveState(this, null);
    private ActivatingSlaveState mActivatingSlaveState = new ActivatingSlaveState(this, null);
    private Context mContext;
    private DataEnablerObserver mDataEnablerObserver;
    private DeactivatingSlaveDataState mDeactivatingSlaveDataState = new DeactivatingSlaveDataState(this, null);
    private DefaultLinkDeletedState mDefaultLinkDeletedState = new DefaultLinkDeletedState(this, null);
    private int mDsdsMode = 0;
    private int mForegroundCallState = -1;
    private IdleState mIdleState = new IdleState(this, null);
    private int mInCallPhoneId = -1;
    private InCallScreenBroadcastReveiver mInCallScreenBroadcastReveiver;
    private MyPhoneStateListener[] mPhoneStateListener;
    private Phone[] mPhones = null;
    private PhoneStateListener mPreciseCallStateListener = new PhoneStateListener() {
        public void onPreciseCallStateChanged(PreciseCallState callState) {
            if (callState != null) {
                int foregroundCallState = callState.getForegroundCallState();
                int ringingCallState = callState.getRingingCallState();
                int backgroundCallState = callState.getBackgroundCallState();
                InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onPreciseCallStateChanged foregroundCallState=");
                stringBuilder.append(foregroundCallState);
                stringBuilder.append(",ringingCallState");
                stringBuilder.append(ringingCallState);
                stringBuilder.append(",backgroundCallState=");
                stringBuilder.append(backgroundCallState);
                inCallDataStateMachine.log(stringBuilder.toString());
                if (foregroundCallState != InCallDataStateMachine.this.mForegroundCallState) {
                    InCallDataStateMachine.this.mForegroundCallState = foregroundCallState;
                    if (1 == foregroundCallState) {
                        InCallDataStateMachine.this.sendMessage(InCallDataStateMachine.this.obtainMessage(8));
                    }
                }
            }
        }
    };
    private InCallDataSettingsChangeObserver mSettingsChangeObserver;
    private SlaveActiveState mSlaveActiveState = new SlaveActiveState(this, null);

    private class ActivatedSlaveState extends State {
        private ActivatedSlaveState() {
        }

        /* synthetic */ ActivatedSlaveState(InCallDataStateMachine x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
            InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ActivatedSlaveState enter notifyDataConnection disconnected phoneId = ");
            stringBuilder.append(default4GSlotId);
            inCallDataStateMachine.log(stringBuilder.toString());
            PhoneFactory.getPhone(default4GSlotId).notifyDataConnection("2GVoiceCallStarted", "default", DataState.DISCONNECTED);
            int defaultDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
            InCallDataStateMachine.this.reportSlaveActivedToChr((byte) defaultDataSubId, (byte) default4GSlotId, ((TelephonyManager) InCallDataStateMachine.this.mContext.getSystemService("phone")).getNetworkType(defaultDataSubId));
        }

        public boolean processMessage(Message msg) {
            InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ActivatedSlaveState: default msg.what=");
            stringBuilder.append(msg.what);
            inCallDataStateMachine.log(stringBuilder.toString());
            return false;
        }
    }

    private class ActivatingSlaveState extends State {
        private ActivatingSlaveState() {
        }

        /* synthetic */ ActivatingSlaveState(InCallDataStateMachine x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ActivatingSlaveState enter mInCallPhoneId is ");
            stringBuilder.append(InCallDataStateMachine.this.mInCallPhoneId);
            inCallDataStateMachine.log(stringBuilder.toString());
        }

        public void exit() {
            InCallDataStateMachine.this.log("ActivatingSlaveState exit");
        }

        public boolean processMessage(Message msg) {
            if (msg.what != 5) {
                InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ActivatingSlaveState: default msg.what=");
                stringBuilder.append(msg.what);
                inCallDataStateMachine.log(stringBuilder.toString());
                return false;
            }
            int dataPhoneId = Integer.valueOf(msg.arg1).intValue();
            InCallDataStateMachine inCallDataStateMachine2 = InCallDataStateMachine.this;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("ActivatingSlaveState processMessage EVENT_DATA_CONNECTED phoneId = ");
            stringBuilder2.append(dataPhoneId);
            inCallDataStateMachine2.log(stringBuilder2.toString());
            if (dataPhoneId != InCallDataStateMachine.this.mInCallPhoneId) {
                return true;
            }
            InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mActivatedSlaveState);
            return true;
        }
    }

    private class DataEnablerObserver extends ContentObserver {
        public DataEnablerObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            int retVal = Global.getInt(InCallDataStateMachine.this.mContext.getContentResolver(), "mobile_data", -1);
            InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DataEnablerObserver onChange retVal = ");
            stringBuilder.append(retVal);
            inCallDataStateMachine.log(stringBuilder.toString());
            if (retVal == 0) {
                InCallDataStateMachine.this.sendMessage(InCallDataStateMachine.this.obtainMessage(6));
            }
        }
    }

    private class DeactivatingSlaveDataState extends State {
        private DeactivatingSlaveDataState() {
        }

        /* synthetic */ DeactivatingSlaveDataState(InCallDataStateMachine x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            int defaultDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
            int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
            InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DeactivatingSlaveDataState enter defaultDataSubId = ");
            stringBuilder.append(defaultDataSubId);
            stringBuilder.append("main 4G slotId = ");
            stringBuilder.append(default4GSlotId);
            inCallDataStateMachine.log(stringBuilder.toString());
            if (defaultDataSubId != default4GSlotId) {
                SubscriptionController.getInstance().setDefaultDataSubId(default4GSlotId);
            } else {
                InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mIdleState);
            }
            if (defaultDataSubId >= 0 && defaultDataSubId < 2 && InCallDataStateMachine.this.mPhoneStateListener[defaultDataSubId].currentDataState != 2 && InCallDataStateMachine.this.mPhoneStateListener[defaultDataSubId].currentDataState != 3) {
                InCallDataStateMachine.this.log("DeactivatingSlaveDataState enter slave already diconnected");
                InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mIdleState);
            }
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                InCallDataStateMachine.this.log("DeactivatingSlaveDataState processMessage EVENT_INCALL_DATA_SETTINGS_ON");
                return true;
            } else if (i == 4) {
                i = Integer.valueOf(msg.arg1).intValue();
                InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DeactivatingSlaveDataState processMessage EVENT_DATA_DISCONNECTED ");
                stringBuilder.append(i);
                inCallDataStateMachine.log(stringBuilder.toString());
                int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                InCallDataStateMachine inCallDataStateMachine2 = InCallDataStateMachine.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("main 4G slotId = ");
                stringBuilder2.append(default4GSlotId);
                inCallDataStateMachine2.log(stringBuilder2.toString());
                if (i == default4GSlotId) {
                    return true;
                }
                InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mIdleState);
                return true;
            } else if (i != 7) {
                InCallDataStateMachine inCallDataStateMachine3 = InCallDataStateMachine.this;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("DeactivatingSlaveDataState: default msg.what=");
                stringBuilder3.append(msg.what);
                inCallDataStateMachine3.log(stringBuilder3.toString());
                return false;
            } else {
                InCallDataStateMachine.this.log("DeactivatingSlaveDataState processMessage EVENT_INCALLUI_BACKGROUND");
                return true;
            }
        }

        public void exit() {
            int defaultDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
            InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DeactivatingSlaveDataState exit defaultDataSubId = ");
            stringBuilder.append(defaultDataSubId);
            inCallDataStateMachine.log(stringBuilder.toString());
            TelephonyNetworkFactory activeNetworkFactory = PhoneFactory.getTelephonyNetworkFactory(defaultDataSubId);
            if (activeNetworkFactory != null) {
                activeNetworkFactory.resumeDefaultLink();
            }
            int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
            InCallDataStateMachine inCallDataStateMachine2 = InCallDataStateMachine.this;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DeactivatingSlaveDataState exit notifyDataConnection phoneId = ");
            stringBuilder2.append(default4GSlotId);
            inCallDataStateMachine2.log(stringBuilder2.toString());
            PhoneFactory.getPhone(default4GSlotId).notifyDataConnection("2GVoiceCallEnded", "default");
            if (Global.getInt(InCallDataStateMachine.this.mContext.getContentResolver(), "mobile_data", 0) == 1) {
                TelephonyNetworkFactory networkFactory = PhoneFactory.getTelephonyNetworkFactory(default4GSlotId);
                if (networkFactory != null) {
                    DcTracker dcTracker = networkFactory.getDcTracker();
                    if (dcTracker != null && SystemProperties.getBoolean("sys.defaultapn.enabled", true)) {
                        InCallDataStateMachine.this.log("DeactivatingSlaveDataState exit setUserDataEnabled true");
                        dcTracker.setEnabledPublic(0, true);
                    }
                }
            }
        }
    }

    private class DefaultLinkDeletedState extends State {
        private DefaultLinkDeletedState() {
        }

        /* synthetic */ DefaultLinkDeletedState(InCallDataStateMachine x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            InCallDataStateMachine.this.log("DefaultLinkDeletedState enter");
            int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
            TelephonyNetworkFactory networkFactory = PhoneFactory.getTelephonyNetworkFactory(default4GSlotId);
            if (networkFactory != null) {
                DcTracker dcTracker = networkFactory.getDcTracker();
                Phone default4GPhone = PhoneFactory.getPhone(default4GSlotId);
                InCallDataStateMachine.this.log("DefaultLinkDeletedState clearDefaultLink");
                if (dcTracker != null && default4GPhone != null) {
                    dcTracker.clearDefaultLink();
                    InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("DefaultLinkDeletedState notifyDataConnection disconnected phoneId = ");
                    stringBuilder.append(default4GSlotId);
                    inCallDataStateMachine.log(stringBuilder.toString());
                    default4GPhone.notifyDataConnection("2GVoiceCallStarted", "default", DataState.DISCONNECTED);
                }
            }
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    InCallDataStateMachine.this.log("DefaultLinkDeletedState processMessage EVENT_INCALL_DATA_SETTINGS_OFF");
                    return true;
                case 2:
                    InCallDataStateMachine.this.log("DefaultLinkDeletedState processMessage EVENT_VOICE_CALL_STARTED");
                    AsyncResult ar = msg.obj;
                    if (ar == null || !(ar.userObj instanceof Integer)) {
                        InCallDataStateMachine.this.logd("EVENT_VOICE_CALL_STARTED error ar");
                        return true;
                    }
                    InCallDataStateMachine.this.mInCallPhoneId = ((Integer) ar.userObj).intValue();
                    return true;
                case 3:
                    int callEndPhoneId = -1;
                    int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                    InCallDataStateMachine.this.log("DefaultLinkDeletedState processMessage EVENT_VOICE_CALL_ENDED");
                    AsyncResult ar2 = msg.obj;
                    if (ar2 == null || !(ar2.userObj instanceof Integer)) {
                        InCallDataStateMachine.this.logd("EVENT_VOICE_CALL_ENDED error ar");
                    } else {
                        callEndPhoneId = ((Integer) ar2.userObj).intValue();
                    }
                    if (InCallDataStateMachine.this.isPhoneStateIDLE()) {
                        InCallDataStateMachine.this.log("DefaultLinkDeletedState transitionTo IdleState");
                        InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mIdleState);
                        InCallDataStateMachine.this.mInCallPhoneId = -1;
                        InCallDataStateMachine.this.isInCallUIForeground = false;
                        return true;
                    } else if (callEndPhoneId < 0 || callEndPhoneId >= 2 || callEndPhoneId == default4GSlotId || !InCallDataStateMachine.this.is4GSlotCanActiveData()) {
                        return true;
                    } else {
                        InCallDataStateMachine.this.log("DefaultLinkDeletedState 4GSlotCanActiveData,transitionTo IdleState");
                        InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mIdleState);
                        return true;
                    }
                case 4:
                    InCallDataStateMachine.this.log("DefaultLinkDeletedState processMessage EVENT_DATA_DISCONNECTED");
                    return true;
                case 5:
                    InCallDataStateMachine.this.log("DefaultLinkDeletedState processMessage EVENT_DATA_CONNECTED");
                    return true;
                case 6:
                    InCallDataStateMachine.this.log("DefaultLinkDeletedState processMessage EVENT_USER_DISABLE_DATA,transitionTo(mIdleState)");
                    InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mIdleState);
                    return true;
                case 8:
                    InCallDataStateMachine.this.log("DefaultLinkDeletedState processMessage EVENT_VOICE_CALL_ACTIVE");
                    return true;
                default:
                    InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("DefaultLinkDeletedState: default msg.what=");
                    stringBuilder.append(msg.what);
                    inCallDataStateMachine.log(stringBuilder.toString());
                    return false;
            }
        }

        public void exit() {
            InCallDataStateMachine.this.log("DefaultLinkDeletedState exit()");
            InCallDataStateMachine.this.resumeDefaultLink();
        }
    }

    private class IdleState extends State {
        private IdleState() {
        }

        /* synthetic */ IdleState(InCallDataStateMachine x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            InCallDataStateMachine.this.log("IdleState enter");
            if (true == InCallDataStateMachine.this.isNeedEnterDefaultLinkDeletedState()) {
                InCallDataStateMachine.this.log("transitionTo mDefaultLinkDeletedState");
                InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mDefaultLinkDeletedState);
            }
        }

        public boolean processMessage(Message msg) {
            InCallDataStateMachine inCallDataStateMachine;
            StringBuilder stringBuilder;
            int default4GSlotId;
            InCallDataStateMachine inCallDataStateMachine2;
            StringBuilder stringBuilder2;
            switch (msg.what) {
                case 0:
                    inCallDataStateMachine = InCallDataStateMachine.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IdleState processMessage EVENT_INCALL_DATA_SETTINGS_ON isInCallUIForeground = ");
                    stringBuilder.append(InCallDataStateMachine.this.isInCallUIForeground);
                    inCallDataStateMachine.log(stringBuilder.toString());
                    if (InCallDataStateMachine.this.isInCallDataSwitchOn() && InCallDataStateMachine.this.isSlaveCanActiveData()) {
                        default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                        inCallDataStateMachine2 = InCallDataStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("call phoneId = ");
                        stringBuilder2.append(InCallDataStateMachine.this.mInCallPhoneId);
                        stringBuilder2.append("main 4G slotId = ");
                        stringBuilder2.append(default4GSlotId);
                        inCallDataStateMachine2.log(stringBuilder2.toString());
                        if (InCallDataStateMachine.this.mInCallPhoneId != default4GSlotId) {
                            InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mActivatingSlaveState);
                            inCallDataStateMachine2 = InCallDataStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("IdleState setDefaultDataSubId to ");
                            stringBuilder2.append(InCallDataStateMachine.this.mInCallPhoneId);
                            inCallDataStateMachine2.log(stringBuilder2.toString());
                            SubscriptionController.getInstance().setDefaultDataSubId(InCallDataStateMachine.this.mInCallPhoneId);
                            break;
                        }
                    }
                    break;
                case 2:
                    InCallDataStateMachine.this.log("IdleState processMessage EVENT_CALL_START");
                    AsyncResult ar = msg.obj;
                    if (ar != null && (ar.userObj instanceof Integer)) {
                        InCallDataStateMachine.this.mInCallPhoneId = ((Integer) ar.userObj).intValue();
                        if (InCallDataStateMachine.this.isInCallDataSwitchOn() && InCallDataStateMachine.this.isSlaveCanActiveData()) {
                            int default4GSlotId2 = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                            InCallDataStateMachine inCallDataStateMachine3 = InCallDataStateMachine.this;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("call phoneId = ");
                            stringBuilder3.append(InCallDataStateMachine.this.mInCallPhoneId);
                            stringBuilder3.append("main 4G slotId = ");
                            stringBuilder3.append(default4GSlotId2);
                            inCallDataStateMachine3.log(stringBuilder3.toString());
                            if (InCallDataStateMachine.this.mInCallPhoneId != default4GSlotId2) {
                                InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mActivatingSlaveState);
                                inCallDataStateMachine3 = InCallDataStateMachine.this;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("IdleState setDefaultDataSubId to ");
                                stringBuilder3.append(InCallDataStateMachine.this.mInCallPhoneId);
                                inCallDataStateMachine3.log(stringBuilder3.toString());
                                SubscriptionController.getInstance().setDefaultDataSubId(InCallDataStateMachine.this.mInCallPhoneId);
                                break;
                            }
                        }
                    }
                    InCallDataStateMachine.this.logd("EVENT_VOICE_CALL_STARTED error ar");
                    break;
                    break;
                case 3:
                    InCallDataStateMachine.this.log("IdleState processMessage EVENT_VOICE_CALL_ENDED");
                    if (InCallDataStateMachine.this.isPhoneStateIDLE()) {
                        InCallDataStateMachine.this.log("IdleState set mInCallPhoneId -1");
                        InCallDataStateMachine.this.mInCallPhoneId = -1;
                        InCallDataStateMachine.this.isInCallUIForeground = false;
                    }
                    if (InCallDataStateMachine.this.hasMessages(9)) {
                        InCallDataStateMachine.this.removeMessages(9);
                        break;
                    }
                    break;
                case 5:
                    default4GSlotId = Integer.valueOf(msg.arg1).intValue();
                    int default4GSlotId3 = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                    InCallDataStateMachine inCallDataStateMachine4 = InCallDataStateMachine.this;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("IdleState processMessage EVENT_DATA_CONNECTED phoneId = ");
                    stringBuilder4.append(default4GSlotId);
                    stringBuilder4.append(" default4GSlotId=");
                    stringBuilder4.append(default4GSlotId3);
                    inCallDataStateMachine4.log(stringBuilder4.toString());
                    if (true == InCallDataStateMachine.this.isNeedEnterDefaultLinkDeletedState() && default4GSlotId == default4GSlotId3) {
                        InCallDataStateMachine.this.log("transitionTo mDefaultLinkDeletedState");
                        InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mDefaultLinkDeletedState);
                        break;
                    }
                case 7:
                    InCallDataStateMachine.this.log("IdleState processMessage EVENT_INCALLUI_BACKGROUND");
                    if (InCallDataStateMachine.this.shouldShowDialog()) {
                        SystemProperties.set("persist.radio.incalldata", "true");
                        InCallDataStateMachine.this.showDialog();
                        break;
                    }
                    break;
                case 8:
                    InCallDataStateMachine.this.log("IdleState processMessage EVENT_VOICE_CALL_ACTIVE");
                    if (true == InCallDataStateMachine.this.isNeedEnterDefaultLinkDeletedState()) {
                        InCallDataStateMachine.this.log("transitionTo mDefaultLinkDeletedState");
                        InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mDefaultLinkDeletedState);
                        break;
                    }
                    break;
                case 9:
                    InCallDataStateMachine.this.log("IdleState processMessage EVENT_DSDS_MODE_CHANGE");
                    if (InCallDataStateMachine.this.isInCallDataSwitchOn() && InCallDataStateMachine.this.isSlaveCanActiveData()) {
                        default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                        if (InCallDataStateMachine.this.mInCallPhoneId != default4GSlotId) {
                            InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mActivatingSlaveState);
                            inCallDataStateMachine2 = InCallDataStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("call phoneId = ");
                            stringBuilder2.append(InCallDataStateMachine.this.mInCallPhoneId);
                            stringBuilder2.append("main 4G slotId = ");
                            stringBuilder2.append(default4GSlotId);
                            inCallDataStateMachine2.log(stringBuilder2.toString());
                            inCallDataStateMachine2 = InCallDataStateMachine.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Found DSDS 2.0 state, setDefaultDataSubId to ");
                            stringBuilder2.append(InCallDataStateMachine.this.mInCallPhoneId);
                            inCallDataStateMachine2.log(stringBuilder2.toString());
                            SubscriptionController.getInstance().setDefaultDataSubId(InCallDataStateMachine.this.mInCallPhoneId);
                            break;
                        }
                    }
                    break;
                default:
                    inCallDataStateMachine = InCallDataStateMachine.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IdleState: default msg.what=");
                    stringBuilder.append(msg.what);
                    inCallDataStateMachine.log(stringBuilder.toString());
                    break;
            }
            return true;
        }
    }

    private class InCallDataSettingsChangeObserver extends ContentObserver {
        public InCallDataSettingsChangeObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            if (InCallDataStateMachine.this.isInCallDataSwitchOn()) {
                InCallDataStateMachine.this.sendMessage(InCallDataStateMachine.this.obtainMessage(0));
            } else {
                InCallDataStateMachine.this.sendMessage(InCallDataStateMachine.this.obtainMessage(1));
            }
        }
    }

    class InCallScreenBroadcastReveiver extends BroadcastReceiver {
        InCallScreenBroadcastReveiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (InCallDataStateMachine.ACTION_INCALL_SCREEN.equals(action)) {
                    InCallDataStateMachine.this.isInCallUIForeground = intent.getBooleanExtra("IsForegroundActivity", true);
                    InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("InCallScreenBroadcastReveiver onReceive isInCallUIForeground = ");
                    stringBuilder.append(InCallDataStateMachine.this.isInCallUIForeground);
                    inCallDataStateMachine.log(stringBuilder.toString());
                    if (!InCallDataStateMachine.this.isInCallUIForeground) {
                        InCallDataStateMachine.this.sendMessage(InCallDataStateMachine.this.obtainMessage(7));
                    }
                } else if (InCallDataStateMachine.ACTION_HW_DSDS_MODE_STATE.equals(action) && !InCallDataStateMachine.PROP_DEL_DEFAULT_LINK) {
                    int newDsdsMode = intent.getIntExtra(InCallDataStateMachine.DSDS_KEY, 0);
                    InCallDataStateMachine inCallDataStateMachine2 = InCallDataStateMachine.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("BroadcastReveiver onReceive newDsdsMode = ");
                    stringBuilder2.append(newDsdsMode);
                    inCallDataStateMachine2.log(stringBuilder2.toString());
                    if (InCallDataStateMachine.this.mDsdsMode != newDsdsMode) {
                        InCallDataStateMachine.this.mDsdsMode = newDsdsMode;
                        InCallDataStateMachine.this.sendMessage(InCallDataStateMachine.this.obtainMessage(9));
                    }
                }
            }
        }
    }

    class MyPhoneStateListener extends PhoneStateListener {
        public int currentDataState = -1;
        private int mPhoneId;

        public MyPhoneStateListener(int phoneId) {
            super(Integer.valueOf(phoneId));
            this.mPhoneId = phoneId;
        }

        public void onDataConnectionStateChanged(int state) {
            InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDataConnectionStateChanged mPhoneId= ");
            stringBuilder.append(this.mPhoneId);
            stringBuilder.append("  state = ");
            stringBuilder.append(state);
            inCallDataStateMachine.log(stringBuilder.toString());
            this.currentDataState = state;
            if (state == 0) {
                InCallDataStateMachine.this.sendMessage(InCallDataStateMachine.this.obtainMessage(4, this.mPhoneId));
            } else if (2 == state || 3 == state) {
                InCallDataStateMachine.this.sendMessage(InCallDataStateMachine.this.obtainMessage(5, this.mPhoneId));
            }
        }
    }

    private class SlaveActiveState extends State {
        private SlaveActiveState() {
        }

        /* synthetic */ SlaveActiveState(InCallDataStateMachine x0, AnonymousClass1 x1) {
            this();
        }

        public boolean processMessage(Message msg) {
            int defaultDataSubId;
            switch (msg.what) {
                case 0:
                    InCallDataStateMachine.this.log("SlaveActiveState processMessage EVENT_INCALL_DATA_SETTINGS_ON");
                    return true;
                case 1:
                    InCallDataStateMachine.this.log("SlaveActiveState processMessage EVENT_INCALL_DATA_SETTINGS_OFF");
                    if (InCallDataStateMachine.this.isInCallDataSwitchOn()) {
                        return true;
                    }
                    defaultDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
                    int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                    InCallDataStateMachine inCallDataStateMachine = InCallDataStateMachine.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("defaultDataSubId = ");
                    stringBuilder.append(defaultDataSubId);
                    stringBuilder.append("main 4G slotId = ");
                    stringBuilder.append(default4GSlotId);
                    inCallDataStateMachine.log(stringBuilder.toString());
                    if (defaultDataSubId != default4GSlotId) {
                        InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mDeactivatingSlaveDataState);
                        return true;
                    }
                    InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mIdleState);
                    return true;
                case 3:
                    InCallDataStateMachine.this.log("SlaveActiveState processMessage EVENT_VOICE_CALL_ENDED");
                    defaultDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
                    int default4GSlotId2 = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                    if (!InCallDataStateMachine.this.isDataPhoneStateIDLE(defaultDataSubId)) {
                        return true;
                    }
                    InCallDataStateMachine inCallDataStateMachine2 = InCallDataStateMachine.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("defaultDataSubId = ");
                    stringBuilder2.append(defaultDataSubId);
                    stringBuilder2.append("main 4G slotId = ");
                    stringBuilder2.append(default4GSlotId2);
                    inCallDataStateMachine2.log(stringBuilder2.toString());
                    if (defaultDataSubId != default4GSlotId2) {
                        InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mDeactivatingSlaveDataState);
                    } else {
                        InCallDataStateMachine.this.transitionTo(InCallDataStateMachine.this.mIdleState);
                    }
                    InCallDataStateMachine.this.mInCallPhoneId = -1;
                    InCallDataStateMachine.this.isInCallUIForeground = false;
                    return true;
                case 6:
                    InCallDataStateMachine.this.log("SlaveActiveState processMessage EVENT_USER_DISABLE_DATA");
                    TelephonyNetworkFactory networkFactory = PhoneFactory.getTelephonyNetworkFactory(HwTelephonyManagerInner.getDefault().getDefault4GSlotId());
                    if (networkFactory == null) {
                        return true;
                    }
                    DcTracker dcTracker = networkFactory.getDcTracker();
                    if (dcTracker == null) {
                        return true;
                    }
                    dcTracker.setUserDataEnabled(false);
                    return true;
                case 7:
                    InCallDataStateMachine.this.log("SlaveActiveState processMessage EVENT_INCALLUI_BACKGROUND");
                    return true;
                case 9:
                    InCallDataStateMachine.this.log("SlaveActiveState drop msg EVENT_DSDS_MODE_CHANGE");
                    return true;
                default:
                    InCallDataStateMachine inCallDataStateMachine3 = InCallDataStateMachine.this;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("SlaveActiveState: default msg.what=");
                    stringBuilder3.append(msg.what);
                    inCallDataStateMachine3.log(stringBuilder3.toString());
                    return false;
            }
        }
    }

    public InCallDataStateMachine(Context context, Phone[] phones) {
        super(LOG_TAG, Looper.myLooper());
        int i = 0;
        this.mContext = context;
        boolean dualImsEnable = HwImsManagerInner.isDualImsAvailable();
        if (phones != null && phones.length == 2 && dualImsEnable) {
            this.mSettingsChangeObserver = new InCallDataSettingsChangeObserver(getHandler());
            this.mDataEnablerObserver = new DataEnablerObserver(getHandler());
            this.mInCallScreenBroadcastReveiver = new InCallScreenBroadcastReveiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_INCALL_SCREEN);
            filter.addAction(ACTION_HW_DSDS_MODE_STATE);
            context.registerReceiver(this.mInCallScreenBroadcastReveiver, filter);
            this.mPhones = new Phone[2];
            int i2 = 0;
            while (i2 < 2) {
                this.mPhones[i2] = phones[i2];
                if (phones[i2].getCallTracker() != null) {
                    phones[i2].getCallTracker().registerForVoiceCallEnded(getHandler(), 3, Integer.valueOf(i2));
                    phones[i2].getCallTracker().registerForVoiceCallStarted(getHandler(), 2, Integer.valueOf(i2));
                    if (!(phones[i2].getImsPhone() == null || phones[i2].getImsPhone().getCallTracker() == null)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("registerImsCallStates phoneId = ");
                        stringBuilder.append(i2);
                        log(stringBuilder.toString());
                        phones[i2].getImsPhone().getCallTracker().registerForVoiceCallEnded(getHandler(), 3, Integer.valueOf(i2));
                        phones[i2].getImsPhone().getCallTracker().registerForVoiceCallStarted(getHandler(), 2, Integer.valueOf(i2));
                    }
                }
                i2++;
            }
            TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            this.mPhoneStateListener = new MyPhoneStateListener[2];
            if (telephonyManager == null) {
                loge("SlotStateListener: mTelephonyManager is null, return!");
            } else {
                while (i < 2) {
                    this.mPhoneStateListener[i] = new MyPhoneStateListener(i);
                    telephonyManager.listen(this.mPhoneStateListener[i], 64);
                    i++;
                }
            }
            if (!(telephonyManager == null || this.mPreciseCallStateListener == null)) {
                telephonyManager.listen(this.mPreciseCallStateListener, 2048);
            }
            this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("mobile_data"), true, this.mDataEnablerObserver);
            this.mContext.getContentResolver().registerContentObserver(Global.getUriFor(SETTINGS_INCALL_DATA_SWITCH), true, this.mSettingsChangeObserver);
        }
        addState(this.mIdleState);
        addState(this.mSlaveActiveState, this.mIdleState);
        addState(this.mActivatingSlaveState, this.mSlaveActiveState);
        addState(this.mActivatedSlaveState, this.mSlaveActiveState);
        addState(this.mDeactivatingSlaveDataState, this.mIdleState);
        addState(this.mDefaultLinkDeletedState, this.mIdleState);
        setInitialState(this.mIdleState);
    }

    private boolean isInCallDataSwitchOn() {
        return Global.getInt(this.mContext.getContentResolver(), SETTINGS_INCALL_DATA_SWITCH, 0) == 1;
    }

    private boolean isSlaveCanActiveData() {
        if (HwVSimUtils.isVSimEnabled()) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSlaveCanActiveData mInCallPhoneId = ");
        stringBuilder.append(this.mInCallPhoneId);
        log(stringBuilder.toString());
        if (this.mInCallPhoneId < 0 || this.mInCallPhoneId >= 2) {
            return false;
        }
        if (PROP_DEL_DEFAULT_LINK || this.mDsdsMode != 1) {
            boolean isCTCard = false;
            TelephonyNetworkFactory callingNetworkFactory = PhoneFactory.getTelephonyNetworkFactory(this.mInCallPhoneId);
            if (callingNetworkFactory != null) {
                DcTracker dcTracker = callingNetworkFactory.getDcTracker();
                if (dcTracker != null) {
                    isCTCard = dcTracker.isCTSimCard(this.mInCallPhoneId);
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isSlaveCanActiveData isCTCard = ");
            stringBuilder2.append(isCTCard);
            log(stringBuilder2.toString());
            if ((isCTCard || DISABLE_GW_PS_ATTACH) && !HwTelephonyManagerInner.getDefault().isImsRegistered(this.mInCallPhoneId)) {
                log("isSlaveCanActiveData CT not switch data when is not volte ");
                return false;
            } else if (HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE && HwFullNetworkManager.getInstance().isCMCCHybird()) {
                log("TL version cmcc hybird, do not allowed switch data.");
                return false;
            } else {
                int networkType = ((TelephonyManager) this.mContext.getSystemService("phone")).getNetworkType(this.mInCallPhoneId);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("isSlaveCanActiveData networkType = ");
                stringBuilder3.append(networkType);
                log(stringBuilder3.toString());
                return canActiveDataByNetworkType(networkType);
            }
        }
        log("isSlaveCanActiveData found DSDS MODE 3.1, no need to active slave.");
        return false;
    }

    private boolean canActiveDataByNetworkType(int networkType) {
        if (networkType != 3) {
            if (networkType != 13) {
                if (networkType != 15) {
                    if (networkType != 19) {
                        if (networkType != 30) {
                            switch (networkType) {
                                case 8:
                                case 9:
                                case 10:
                                    break;
                                default:
                                    return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
        if (DISABLE_GW_PS_ATTACH) {
            return false;
        }
        return true;
    }

    private boolean shouldShowDialog() {
        boolean shouldDialog = false;
        boolean isUserDataOn = 1 == Global.getInt(this.mContext.getContentResolver(), "mobile_data", -1);
        boolean hasShowDialog = SystemProperties.getBoolean("persist.radio.incalldata", false);
        boolean mIsWifiConnected = false;
        int default4GSlot = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        ConnectivityManager mCm = (ConnectivityManager) PhoneFactory.getPhone(default4GSlot).getContext().getSystemService("connectivity");
        if (mCm != null) {
            NetworkInfo mWifiNetworkInfo = mCm.getNetworkInfo(1);
            if (mWifiNetworkInfo != null && mWifiNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
                mIsWifiConnected = true;
            }
        }
        boolean isOverDsds3 = this.mDsdsMode == 1 && !PROP_DEL_DEFAULT_LINK;
        if (!(isInCallDataSwitchOn() || !isSlaveCanActiveData() || !isUserDataOn || mIsWifiConnected || hasShowDialog || this.mInCallPhoneId == default4GSlot || isOverDsds3)) {
            shouldDialog = true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("shouldDialog is: ");
        stringBuilder.append(shouldDialog);
        stringBuilder.append(",isUserDataOn:");
        stringBuilder.append(isUserDataOn);
        stringBuilder.append(",mIsWifiConnected:");
        stringBuilder.append(mIsWifiConnected);
        stringBuilder.append(",hasShowDialog:");
        stringBuilder.append(hasShowDialog);
        stringBuilder.append(",mInCallPhoneId:");
        stringBuilder.append(this.mInCallPhoneId);
        stringBuilder.append(",default4GSlot");
        stringBuilder.append(default4GSlot);
        stringBuilder.append(", isOverDsds3:");
        stringBuilder.append(isOverDsds3);
        log(stringBuilder.toString());
        return shouldDialog;
    }

    private void showDialog() {
        String toastString = String.format(this.mContext.getResources().getString(33686162), new Object[]{Integer.valueOf(this.mInCallPhoneId + 1)});
        Builder builder = new Builder(this.mContext, 33947691);
        builder.setTitle(33686163);
        builder.setMessage(toastString);
        builder.setPositiveButton(17039871, new OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                Global.putInt(InCallDataStateMachine.this.mContext.getContentResolver(), InCallDataStateMachine.SETTINGS_INCALL_DATA_SWITCH, 1);
            }
        });
        builder.setNegativeButton(17039360, null);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_MODE_DONE);
        dialog.setCancelable(false);
        dialog.show();
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    public boolean isDeactivatingSlaveData() {
        return getCurrentState() == this.mDeactivatingSlaveDataState;
    }

    public boolean isSwitchingToSlave() {
        return getCurrentState() == this.mActivatingSlaveState;
    }

    public boolean isSlaveActive() {
        return getCurrentState() == this.mActivatingSlaveState || getCurrentState() == this.mActivatedSlaveState;
    }

    /* JADX WARNING: Missing block: B:13:0x00a4, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void registerImsCallStates(boolean enable, int i) {
        if (!(i < 0 || i >= 2 || this.mPhones == null || this.mPhones[i].getImsPhone() == null || this.mPhones[i].getImsPhone().getCallTracker() == null)) {
            if (enable) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("registerImsCallStates phoneId = ");
                stringBuilder.append(i);
                log(stringBuilder.toString());
                this.mPhones[i].getImsPhone().getCallTracker().registerForVoiceCallEnded(getHandler(), 3, Integer.valueOf(i));
                this.mPhones[i].getImsPhone().getCallTracker().registerForVoiceCallStarted(getHandler(), 2, Integer.valueOf(i));
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("unregisterImsCallStates phoneId = ");
                stringBuilder2.append(i);
                log(stringBuilder2.toString());
                this.mPhones[i].getImsPhone().getCallTracker().unregisterForVoiceCallEnded(getHandler());
                this.mPhones[i].getImsPhone().getCallTracker().unregisterForVoiceCallStarted(getHandler());
            }
        }
    }

    private boolean isPhoneStateIDLE() {
        boolean isIdle = true;
        int i = 0;
        while (i < this.mPhones.length) {
            try {
                if (this.mPhones[i].getCallTracker().getState() != PhoneConstants.State.IDLE || this.mPhones[i].getImsPhone().getCallTracker().getState() != PhoneConstants.State.IDLE) {
                    isIdle = false;
                }
                i++;
            } catch (NullPointerException npe) {
                log(npe.toString());
            } catch (Exception e) {
                log(e.toString());
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isPhoneStateIDLE isIdle = ");
        stringBuilder.append(isIdle);
        log(stringBuilder.toString());
        return isIdle;
    }

    private boolean isDataPhoneStateIDLE(int dataPhoneId) {
        if (dataPhoneId < 0 || dataPhoneId >= 2) {
            return true;
        }
        boolean isIdle = true;
        if (!((this.mPhones[dataPhoneId] == null || this.mPhones[dataPhoneId].getCallTracker() == null || PhoneConstants.State.IDLE == this.mPhones[dataPhoneId].getCallTracker().getState()) && (this.mPhones[dataPhoneId] == null || this.mPhones[dataPhoneId].getImsPhone() == null || this.mPhones[dataPhoneId].getImsPhone().getCallTracker() == null || PhoneConstants.State.IDLE == this.mPhones[dataPhoneId].getImsPhone().getCallTracker().getState()))) {
            isIdle = false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isDataPhoneStateIDLE isIdle = ");
        stringBuilder.append(isIdle);
        log(stringBuilder.toString());
        return isIdle;
    }

    private boolean isCallStateActive() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isCallStateActive mForegroundCallState = ");
        stringBuilder.append(this.mForegroundCallState);
        log(stringBuilder.toString());
        return 1 == this.mForegroundCallState;
    }

    private void resumeDefaultLink() {
        int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        TelephonyNetworkFactory networkFactory = PhoneFactory.getTelephonyNetworkFactory(default4GSlotId);
        Phone default4GPhone = PhoneFactory.getPhone(default4GSlotId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resumeDefaultLink default4GSlotId = ");
        stringBuilder.append(default4GSlotId);
        log(stringBuilder.toString());
        if (networkFactory != null && default4GPhone != null) {
            networkFactory.resumeDefaultLink();
            default4GPhone.notifyDataConnection("2GVoiceCallEnded", "default");
        }
    }

    private boolean isDefaultDataConnected() {
        TelephonyManager telephonyManager = null;
        if (this.mContext != null) {
            telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        if (telephonyManager == null || 2 != telephonyManager.getDataState()) {
            return false;
        }
        return true;
    }

    private boolean isNeedEnterDefaultLinkDeletedState() {
        int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        if (MultiSimVariants.DSDS != TelephonyManager.getDefault().getMultiSimConfiguration()) {
            log("isNeedEnterDefaultLinkDeletedState getMultiSimConfiguration=DSDA");
            return false;
        } else if (getCurrentState() != this.mIdleState || this.mInCallPhoneId < 0 || this.mInCallPhoneId >= 2 || this.mInCallPhoneId == default4GSlotId || true != isCallStateActive() || true != isDefaultDataConnected() || true != PROP_DEL_DEFAULT_LINK || (isInCallDataSwitchOn() && (true != isInCallDataSwitchOn() || isSlaveCanActiveData()))) {
            return false;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isNeedEnterDefaultLinkDeletedState true,mInCallPhoneId=");
            stringBuilder.append(this.mInCallPhoneId);
            stringBuilder.append(" default4GSlotId=");
            stringBuilder.append(default4GSlotId);
            log(stringBuilder.toString());
            return true;
        }
    }

    private boolean is4GSlotCanActiveData() {
        int default4GSlotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("is4GSlotCanActiveData default4GSlotId = ");
        stringBuilder.append(default4GSlotId);
        log(stringBuilder.toString());
        if (default4GSlotId < 0 || default4GSlotId >= 2) {
            return false;
        }
        boolean isCTCard = false;
        TelephonyNetworkFactory callingNetworkFactory = PhoneFactory.getTelephonyNetworkFactory(default4GSlotId);
        if (callingNetworkFactory != null) {
            DcTracker dcTracker = callingNetworkFactory.getDcTracker();
            if (dcTracker != null) {
                isCTCard = dcTracker.isCTSimCard(default4GSlotId);
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("is4GSlotCanActiveData isCTCard = ");
        stringBuilder2.append(isCTCard);
        log(stringBuilder2.toString());
        if (!isCTCard || HwTelephonyManagerInner.getDefault().isImsRegistered(default4GSlotId)) {
            int networkType = ((TelephonyManager) this.mContext.getSystemService("phone")).getNetworkType(default4GSlotId);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("is4GSlotCanActiveData networkType = ");
            stringBuilder3.append(networkType);
            log(stringBuilder3.toString());
            return canActiveDataByNetworkType(networkType);
        }
        log("is4GSlotCanActiveData CT can not active data when is not volte ");
        return false;
    }

    private void reportSlaveActivedToChr(byte defaultDataSubId, byte default4GSlotId, int networkType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ActiviedSlaveState report to CHR, networkType = ");
        stringBuilder.append(networkType);
        log(stringBuilder.toString());
        Bundle data = new Bundle();
        data.putString("EventScenario", "INCALLDATA");
        data.putInt("EventFailCause", 1001);
        data.putByte("DATACONN.INCALLDATA.InCallSubId", defaultDataSubId);
        data.putByte("DATACONN.INCALLDATA.default4gSubId", default4GSlotId);
        data.putInt("DATACONN.INCALLDATA.networkType", networkType);
        HwTelephonyFactory.getHwTelephonyChrManager().sendTelephonyChrBroadcast(data, defaultDataSubId);
    }
}
