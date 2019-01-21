package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MultiSimVariants;
import android.util.Log;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimUtils;

class SubscriptionHelper extends Handler {
    private static final String APM_SIM_NOT_PWDN_PROPERTY = "persist.radio.apm_sim_not_pwdn";
    public static final byte[] C1 = new byte[]{(byte) 98, (byte) 94, (byte) -52, (byte) 117, (byte) -82, (byte) 28, (byte) -44, (byte) 66, (byte) 28, (byte) 61, (byte) -110, (byte) -119, (byte) -75, (byte) 70, (byte) 2, (byte) 85};
    private static final int EVENT_RADIO_AVAILABLE = 5;
    private static final int EVENT_RADIO_ON = 4;
    private static final int EVENT_REFRESH = 2;
    private static final int EVENT_SET_UICC_SUBSCRIPTION_DONE = 1;
    private static final boolean IS_SETUICCSUB_BY_SLOT = SystemProperties.getBoolean("ro.config.setuiccsub_by_slot", false);
    private static final String LOG_TAG = "SubHelper";
    private static final int SUB_1 = 1;
    public static final int SUB_INIT_STATE = -1;
    public static final int SUB_SET_UICC_FAIL = -100;
    public static final int SUB_SIM_NOT_INSERTED = -99;
    private static final int SUB_SIM_REFRESH = -101;
    private static boolean mNwModeUpdated = false;
    private static final boolean sApmSIMNotPwdn;
    private static SubscriptionHelper sInstance;
    private static int sNumPhones;
    private static boolean sTriggerDds = false;
    private int INVALID_VALUE = -1;
    private CommandsInterface[] mCi;
    private Context mContext;
    private boolean[] mNeedResetSub;
    private int[] mNewSubState;
    private int[] mSubStatus;
    private final ContentObserver nwModeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfUpdate) {
            SubscriptionHelper.logd("NwMode Observer onChange hit !!!");
            if (SubscriptionHelper.mNwModeUpdated) {
                SubscriptionHelper.this.updateNwModesInSubIdTable(true);
            }
        }
    };

    static {
        boolean z = true;
        if (SystemProperties.getInt(APM_SIM_NOT_PWDN_PROPERTY, 0) != 1) {
            z = false;
        }
        sApmSIMNotPwdn = z;
    }

    public static SubscriptionHelper init(Context c, CommandsInterface[] ci) {
        SubscriptionHelper subscriptionHelper;
        synchronized (SubscriptionHelper.class) {
            if (sInstance == null) {
                sInstance = new SubscriptionHelper(c, ci);
            } else {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("init() called multiple times!  sInstance = ");
                stringBuilder.append(sInstance);
                Log.wtf(str, stringBuilder.toString());
            }
            subscriptionHelper = sInstance;
        }
        return subscriptionHelper;
    }

    public static SubscriptionHelper getInstance() {
        if (sInstance == null) {
            Log.wtf(LOG_TAG, "getInstance null");
        }
        return sInstance;
    }

    private SubscriptionHelper(Context c, CommandsInterface[] ci) {
        this.mContext = c;
        this.mCi = ci;
        sNumPhones = TelephonyManager.getDefault().getPhoneCount();
        this.mSubStatus = new int[sNumPhones];
        this.mNewSubState = new int[sNumPhones];
        this.mNeedResetSub = new boolean[sNumPhones];
        for (int i = 0; i < sNumPhones; i++) {
            this.mSubStatus[i] = -1;
            this.mNewSubState[i] = this.INVALID_VALUE;
            this.mNeedResetSub[i] = false;
            Integer index = Integer.valueOf(i);
            this.mCi[i].registerForIccRefresh(this, 2, index);
            this.mCi[i].registerForOn(this, 4, index);
            this.mCi[i].registerForAvailable(this, 5, index);
        }
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("preferred_network_mode"), false, this.nwModeObserver);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SubscriptionHelper init by Context, num phones = ");
        stringBuilder.append(sNumPhones);
        stringBuilder.append(" ApmSIMNotPwdn = ");
        stringBuilder.append(sApmSIMNotPwdn);
        logd(stringBuilder.toString());
    }

    private void updateNwModesInSubIdTable(boolean override) {
        SubscriptionController subCtrlr = SubscriptionController.getInstance();
        for (int i = 0; i < sNumPhones; i++) {
            int[] subIdList = subCtrlr.getSubId(i);
            if (subIdList != null && subIdList[0] >= 0) {
                int nwModeInDb;
                try {
                    nwModeInDb = TelephonyManager.getIntAtIndex(this.mContext.getContentResolver(), "preferred_network_mode", i);
                } catch (SettingNotFoundException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Settings Exception Reading Value At Index[");
                    stringBuilder.append(i);
                    stringBuilder.append("] Settings.Global.PREFERRED_NETWORK_MODE");
                    loge(stringBuilder.toString());
                    nwModeInDb = RILConstants.PREFERRED_NETWORK_MODE;
                }
                int nwModeinSubIdTable = subCtrlr.getNwMode(subIdList[0]);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateNwModesInSubIdTable: nwModeinSubIdTable: ");
                stringBuilder2.append(nwModeinSubIdTable);
                stringBuilder2.append(", nwModeInDb: ");
                stringBuilder2.append(nwModeInDb);
                logd(stringBuilder2.toString());
                if (override || nwModeinSubIdTable == -1) {
                    subCtrlr.setNwMode(subIdList[0], nwModeInDb);
                }
            }
        }
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                logd("EVENT_SET_UICC_SUBSCRIPTION_DONE");
                processSetUiccSubscriptionDone(msg);
                return;
            case 2:
                logd("EVENT_REFRESH");
                processSimRefresh((AsyncResult) msg.obj);
                return;
            case 4:
            case 5:
                Integer Index = msg.obj.userObj;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[EVENT_RADIO_ON or EVENT_RADIO_AVAILABLE]: Index");
                stringBuilder.append(Index);
                logd(stringBuilder.toString());
                if (Index.intValue() != this.INVALID_VALUE && this.mNewSubState[Index.intValue()] != this.INVALID_VALUE && true == this.mNeedResetSub[Index.intValue()]) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("[EVENT_RADIO_ON or EVENT_RADIO_AVAILABLE]: Need to reset UICC Subscription,Index = ");
                    stringBuilder.append(Index);
                    stringBuilder.append(";mNewSubState = ");
                    stringBuilder.append(this.mNewSubState[Index.intValue()]);
                    logd(stringBuilder.toString());
                    setUiccSubscription(Index.intValue(), this.mNewSubState[Index.intValue()]);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public boolean needSubActivationAfterRefresh(int slotId) {
        return sNumPhones > 1 && this.mSubStatus[slotId] == SUB_SIM_REFRESH;
    }

    public void updateSubActivation(int[] simStatus, boolean isStackReadyEvent) {
        boolean isPrimarySubFeatureEnable = SystemProperties.getBoolean("persist.radio.primarycard", false);
        SubscriptionController subCtrlr = SubscriptionController.getInstance();
        boolean z = true;
        if (isStackReadyEvent && !isPrimarySubFeatureEnable) {
            sTriggerDds = true;
        }
        boolean setUiccSent = false;
        int slotId = 0;
        while (slotId < sNumPhones) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("slot[");
            stringBuilder.append(slotId);
            stringBuilder.append("] simStatus = ");
            stringBuilder.append(simStatus[slotId]);
            logd(stringBuilder.toString());
            int[] subId = subCtrlr.getSubId(slotId);
            int i = simStatus[slotId];
            StringBuilder stringBuilder2;
            if (i != -99) {
                switch (i) {
                    case -3:
                    case HwVSimConstants.ERR_GET_VSIM_VER_NOT_SUPPORT /*-2*/:
                        if (!!HwVSimUtils.isVSimInProcess()) {
                            break;
                        }
                        logd("vsim caused sim load, skip it.");
                        break;
                    default:
                        switch (i) {
                            case 0:
                                if (!this.mNeedResetSub[slotId]) {
                                    if (!HwVSimUtils.getIsWaitingSwitchCdmaModeSide()) {
                                        if (!HwVSimUtils.getIsWaitingNvMatchUnsol()) {
                                            if (!HwVSimUtils.prohibitSubUpdateSimNoChange(slotId)) {
                                                i = this.mNewSubState[slotId] != this.INVALID_VALUE ? this.mNewSubState[slotId] : subCtrlr.getSubState(subId[0]);
                                                StringBuilder stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("slot[");
                                                stringBuilder3.append(slotId);
                                                stringBuilder3.append("], sim no change, subState should be ");
                                                stringBuilder3.append(i);
                                                logd(stringBuilder3.toString());
                                                if (i == 1) {
                                                    subCtrlr.activateSubId(subId[0]);
                                                } else {
                                                    subCtrlr.deactivateSubId(subId[0]);
                                                }
                                                setUiccSent = true;
                                                break;
                                            }
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("slot[");
                                            stringBuilder2.append(slotId);
                                            stringBuilder2.append("], sim no change, but vsim prohibit, skip it");
                                            logd(stringBuilder2.toString());
                                            setUiccSent = true;
                                            break;
                                        }
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("slot[");
                                        stringBuilder2.append(slotId);
                                        stringBuilder2.append("], sim no change, but isWaitingNvMatchUnsol, skip it");
                                        logd(stringBuilder2.toString());
                                        setUiccSent = true;
                                        break;
                                    }
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("slot[");
                                    stringBuilder2.append(slotId);
                                    stringBuilder2.append("], sim no change, but isWaitingSwitchCdmaModeSide, skip it");
                                    logd(stringBuilder2.toString());
                                    break;
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("slot[");
                                stringBuilder2.append(slotId);
                                stringBuilder2.append("], sim no change, but mNeedResetSub, skip it");
                                logd(stringBuilder2.toString());
                                setUiccSent = true;
                                continue;
                            case 1:
                            case 2:
                                break;
                            default:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(" slot [");
                                stringBuilder2.append(slotId);
                                stringBuilder2.append("], incorrect simStatus: ");
                                stringBuilder2.append(simStatus[slotId]);
                                loge(stringBuilder2.toString());
                                continue;
                        }
                        if (!HwVSimUtils.isVSimInProcess() && !HwVSimUtils.isVSimCauseCardReload()) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("slot[");
                            stringBuilder2.append(slotId);
                            stringBuilder2.append("] sim has changed, should activate it.");
                            logd(stringBuilder2.toString());
                            if (!HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(slotId, "disable-sub")) {
                                subCtrlr.activateSubId(subId[0]);
                                HwVSimUtils.setSubActived(subId[0]);
                                setUiccSent = true;
                                this.mNewSubState[slotId] = 1;
                                break;
                            }
                            setUiccSubscription(1, 0);
                            return;
                        }
                        logd("vsim caused sim load, skip it.");
                        break;
                }
            }
            this.mSubStatus[slotId] = simStatus[slotId];
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("slot[");
            stringBuilder2.append(slotId);
            stringBuilder2.append("] sim is not insert.");
            logd(stringBuilder2.toString());
            slotId++;
        }
        if (isAllSubsAvailable() && !setUiccSent) {
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Received all sim info, update user pref subs, triggerDds= ");
            stringBuilder4.append(sTriggerDds);
            logd(stringBuilder4.toString());
            if (!((HwVSimUtils.isVSimDsdsVersionOne() && HwVSimUtils.isVSimEnabled()) || HwVSimUtils.isVSimInProcess() || HwVSimUtils.isVSimCauseCardReload())) {
                z = false;
            }
            if (z) {
                logd("vsim skip updateUserPreferences");
            } else {
                HwTelephonyFactory.getHwUiccManager().updateUserPreferences(sTriggerDds);
            }
            sTriggerDds = false;
        }
    }

    public void updateNwMode() {
        updateNwModesInSubIdTable(false);
        mNwModeUpdated = true;
    }

    public void setUiccSubscription(int slotId, int subStatus) {
        int i = slotId;
        int i2 = subStatus;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setUiccSubscription: slotId:");
        stringBuilder.append(i);
        stringBuilder.append(", subStatus:");
        stringBuilder.append(i2);
        logd(stringBuilder.toString());
        boolean set3GPPDone = false;
        boolean set3GPP2Done = false;
        UiccCard uiccCard = UiccController.getInstance().getUiccCard(i);
        int i3 = 0;
        int i4 = 1;
        if (HwModemCapability.isCapabilitySupport(9)) {
            int i5 = 2;
            if (uiccCard == null || uiccCard.getNumApplications() == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("setUiccSubscription: slotId:");
                stringBuilder.append(i);
                stringBuilder.append(" card info not available");
                logd(stringBuilder.toString());
                PhoneFactory.getSubInfoRecordUpdater().resetIccid(i);
                Message msgSetUiccSubDone = Message.obtain(this, 1, i, i2);
                AsyncResult.forMessage(msgSetUiccSubDone, Boolean.valueOf(false), CommandException.fromRilErrno(2));
                msgSetUiccSubDone.sendToTarget();
            } else if (!IS_SETUICCSUB_BY_SLOT) {
                int numApplication = uiccCard.getNumApplications();
                while (true) {
                    int i6 = i3;
                    if (i6 >= numApplication) {
                        break;
                    }
                    int appType = uiccCard.getApplicationIndex(i6).getType().ordinal();
                    if (set3GPPDone) {
                        i5 = appType;
                    } else if (appType == i5 || appType == 1) {
                        this.mCi[i].setUiccSubscription(i, i6, i, i2, Message.obtain(this, 1, i, i2));
                        set3GPPDone = true;
                        if (!set3GPPDone && set3GPP2Done) {
                            break;
                        }
                        i3 = i6 + 1;
                        i5 = 2;
                    } else {
                        i5 = appType;
                    }
                    if (!set3GPP2Done && (appType == 4 || appType == 3)) {
                        this.mCi[i].setUiccSubscription(i, i6, i, i2, Message.obtain(this, 1, i, i2));
                        set3GPP2Done = true;
                    }
                    if (!set3GPPDone) {
                    }
                    i3 = i6 + 1;
                    i5 = 2;
                }
            } else {
                this.mCi[i].setUiccSubscription(i, 0, i, i2, Message.obtain(this, 1, i, i2));
            }
        } else {
            Message msgSetUiccSubDone2 = Message.obtain(this, 1, i, i2);
            if ((MultiSimVariants.DSDS == TelephonyManager.getDefault().getMultiSimConfiguration()) || SystemProperties.getBoolean("ro.hwpp.set_uicc_by_radiopower", false)) {
                this.mNewSubState[i] = this.INVALID_VALUE;
                this.mNeedResetSub[i] = false;
                PhoneFactory.getPhone(slotId).setRadioPower(i2 != 0, msgSetUiccSubDone2);
                if (i2 == 0) {
                    i4 = 0;
                }
                HwVSimUtils.updateSubState(i, i4);
            } else {
                this.mCi[i].setUiccSubscription(i, 0, i, i2, msgSetUiccSubDone2);
            }
        }
    }

    private void processSetUiccSubscriptionDone(Message msg) {
        SubscriptionController subCtrlr = SubscriptionController.getInstance();
        AsyncResult ar = msg.obj;
        int slotId = msg.arg1;
        int newSubState = msg.arg2;
        int[] subId = subCtrlr.getSubId(slotId);
        boolean isVSimSkipUpdateUserPref = true;
        if (ar.exception != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in SET_UICC_SUBSCRIPTION, slotId = ");
            stringBuilder.append(slotId);
            stringBuilder.append(" newSubState ");
            stringBuilder.append(newSubState);
            loge(stringBuilder.toString());
            this.mSubStatus[slotId] = -100;
            if ((ar.exception instanceof CommandException) && ((CommandException) ar.exception).getCommandError() == Error.RADIO_NOT_AVAILABLE) {
                this.mNewSubState[slotId] = newSubState;
                this.mNeedResetSub[slotId] = true;
                this.mSubStatus[slotId] = -1;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Store subinfo and set mNeedResetSub to true because of RADIO_NOT_AVAILABLE, mNeedResetSub[");
                stringBuilder.append(slotId);
                stringBuilder.append("]:");
                stringBuilder.append(this.mNeedResetSub[slotId]);
                logd(stringBuilder.toString());
            }
            broadcastSetUiccResult(slotId, newSubState, 1);
            return;
        }
        if (newSubState != subCtrlr.getSubState(subId[0])) {
            subCtrlr.setSubState(subId[0], newSubState);
        }
        broadcastSetUiccResult(slotId, newSubState, 0);
        this.mSubStatus[slotId] = newSubState;
        if (isAllSubsAvailable()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Received all subs, now update user preferred subs, slotid = ");
            stringBuilder2.append(slotId);
            stringBuilder2.append(" newSubState = ");
            stringBuilder2.append(newSubState);
            stringBuilder2.append(" sTriggerDds = ");
            stringBuilder2.append(sTriggerDds);
            logd(stringBuilder2.toString());
            if (!((HwVSimUtils.isVSimDsdsVersionOne() && HwVSimUtils.isVSimEnabled()) || HwVSimUtils.isVSimInProcess() || HwVSimUtils.isVSimCauseCardReload())) {
                isVSimSkipUpdateUserPref = false;
            }
            if (isVSimSkipUpdateUserPref) {
                logd("vsim skip updateUserPreferences");
            } else {
                HwTelephonyFactory.getHwUiccManager().updateUserPreferences(sTriggerDds);
            }
            if (sTriggerDds && HwModemCapability.isCapabilitySupport(9)) {
                HwTelephonyFactory.getHwUiccManager().updateDataSlot();
            }
            sTriggerDds = false;
        }
        this.mNewSubState[slotId] = this.INVALID_VALUE;
        this.mNeedResetSub[slotId] = false;
    }

    private void processSimRefresh(AsyncResult ar) {
        if (ar.exception != null || ar.result == null) {
            loge("processSimRefresh received without input");
            return;
        }
        Integer index = ar.userObj;
        IccRefreshResponse state = ar.result;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" Received SIM refresh, reset sub state ");
        stringBuilder.append(index);
        stringBuilder.append(" old sub state ");
        stringBuilder.append(this.mSubStatus[index.intValue()]);
        stringBuilder.append(" refreshResult = ");
        stringBuilder.append(state.refreshResult);
        logi(stringBuilder.toString());
        if (state.refreshResult == 2) {
            this.mSubStatus[index.intValue()] = SUB_SIM_REFRESH;
        }
    }

    private void broadcastSetUiccResult(int slotId, int newSubState, int result) {
        int[] subId = SubscriptionController.getInstance().getSubId(slotId);
        Intent intent = new Intent("com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT");
        intent.addFlags(16777216);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, slotId, subId[0]);
        intent.putExtra("operationResult", result);
        intent.putExtra("newSubState", newSubState);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isAllSubsAvailable() {
        boolean allSubsAvailable = true;
        for (int i = 0; i < sNumPhones; i++) {
            if (this.mSubStatus[i] == -1) {
                allSubsAvailable = false;
            }
        }
        return allSubsAvailable;
    }

    public boolean isRadioOn(int phoneId) {
        return this.mCi[phoneId].getRadioState().isOn();
    }

    public boolean isRadioAvailable(int phoneId) {
        return this.mCi[phoneId].getRadioState().isAvailable();
    }

    public boolean isApmSIMNotPwdn() {
        return sApmSIMNotPwdn;
    }

    public boolean proceedToHandleIccEvent(int slotId) {
        StringBuilder stringBuilder;
        int apmState = Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
        if (!sApmSIMNotPwdn && (!isRadioOn(slotId) || apmState == 1)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" proceedToHandleIccEvent, radio off/unavailable, slotId = ");
            stringBuilder.append(slotId);
            logi(stringBuilder.toString());
            this.mSubStatus[slotId] = -1;
        }
        if (apmState == 1 && !sApmSIMNotPwdn) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" proceedToHandleIccEvent, sApmSIMNotPwdn = ");
            stringBuilder.append(sApmSIMNotPwdn);
            logd(stringBuilder.toString());
            return false;
        } else if (isRadioAvailable(slotId)) {
            return true;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" proceedToHandleIccEvent, radio not available, slotId = ");
            stringBuilder.append(slotId);
            logi(stringBuilder.toString());
            if (!HwVSimUtils.isPlatformTwoModems() || HwVSimUtils.isRadioAvailable(slotId)) {
                return false;
            }
            logi("proceedToHandleIccEvent, vsim pending sub");
            return true;
        }
    }

    private static void logd(String message) {
        Rlog.d(LOG_TAG, message);
    }

    private void logi(String msg) {
        Rlog.i(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
