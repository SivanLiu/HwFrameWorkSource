package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.HwDsdsController;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.RadioCapability;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.vsim.HwVSimNvMatchController;
import com.android.internal.telephony.vsim.HwVSimUtils;

public class HwFullNetworkSetStateHisi2_0 extends HwFullNetworkSetStateBase {
    private static final String LOG_TAG = "HwFullNetworkSetStateHisi2_0";
    private static final int NEED_RESTART_RILD = 0;
    private HwFullNetworkChipHisi mChipHisi = null;
    private boolean needResartRild;

    HwFullNetworkSetStateHisi2_0(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        int i = 0;
        this.needResartRild = false;
        this.mChipHisi = HwFullNetworkChipHisi.getInstance();
        logd("HwFullNetworkSetStateHisi2_0 constructor");
        while (i < this.mCis.length) {
            Integer index = Integer.valueOf(i);
            this.mCis[i].registerForIccStatusChanged(this, HwFullNetworkConstants.EVENT_RESTART_RILD_FOR_NV, index);
            this.mCis[i].registerUnsolHwRestartRildStatus(this, HwFullNetworkConstants.EVENT_UNSOL_RESTART_RILD_STATUS, index);
            i++;
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null || msg.obj == null) {
            loge("msg or msg.obj is null, return!");
            return;
        }
        Integer index = this.mChipCommon.getCiIndex(msg);
        StringBuilder stringBuilder;
        if (index.intValue() < 0 || index.intValue() >= this.mCis.length) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid index : ");
            stringBuilder.append(index);
            stringBuilder.append(" received with event ");
            stringBuilder.append(msg.what);
            loge(stringBuilder.toString());
            return;
        }
        switch (msg.what) {
            case HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_DONE /*401*/:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Received EVENT_SET_MAIN_SLOT_DONE on index ");
                stringBuilder.append(index);
                logd(stringBuilder.toString());
                setMainSlotDone(msg, index.intValue());
                break;
            case HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_TIMEOUT /*402*/:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Received EVENT_SET_MAIN_SLOT_TIMEOUT on index ");
                stringBuilder.append(index);
                logd(stringBuilder.toString());
                setMainSlotTimeOut(msg, index.intValue());
                break;
            case HwFullNetworkConstants.EVENT_GET_CDMA_MODE_SIDE_DONE /*1007*/:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Received EVENT_GET_CDMA_MODE_SIDE_DONE on index ");
                stringBuilder.append(index);
                logd(stringBuilder.toString());
                this.mChipHisi.onGetCdmaModeSideDone(msg.obj, index);
                break;
            case HwFullNetworkConstants.EVENT_CMCC_SET_NETWOR_DONE /*1011*/:
                stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_CMCC_SET_NETWOR_DONE reveived for slot: ");
                stringBuilder.append(msg.arg1);
                logd(stringBuilder.toString());
                this.mChipHisi.handleSetCmccPrefNetwork(msg);
                break;
            case HwFullNetworkConstants.EVENT_RESTART_RILD_FOR_NV /*1013*/:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Received EVENT_RESTART_RILD_FOR_NV on index ");
                stringBuilder.append(index);
                logd(stringBuilder.toString());
                restartRildForNvcfg();
                break;
            case HwFullNetworkConstants.EVENT_SWITCH_SLOT_TYPE_DONE /*2205*/:
                logd("EVENT_SWITCH_SLOT_TYPE_DONE");
                break;
            case HwFullNetworkConstants.EVENT_UNSOL_RESTART_RILD_STATUS /*2206*/:
                logd("EVENT_UNSOL_RESTART_RILD_STATUS");
                onUnsolRestartRildStatus(msg);
                break;
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown msg:");
                stringBuilder.append(msg.what);
                logd(stringBuilder.toString());
                break;
        }
    }

    public void setMainSlot(int slotId, Message responseMsg) {
        this.mChipCommon.expectedDDSsubId = slotId;
        if (this.mCis[slotId] == null || RadioState.RADIO_UNAVAILABLE != this.mCis[slotId].getRadioState()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDefault4GSlot: target slot id is: ");
            stringBuilder.append(slotId);
            stringBuilder.append(" response:");
            stringBuilder.append(responseMsg);
            logd(stringBuilder.toString());
            sendHwSwitchSlotStartBroadcast();
            this.mChipCommon.mSet4GSlotCompleteMsg = responseMsg;
            this.mChipCommon.isSet4GSlotInProgress = true;
            this.mChipCommon.isSet4GSlotInSwitchProgress = true;
            this.mChipCommon.current4GSlotBackup = this.mChipCommon.getUserSwitchDualCardSlots();
            fastSwitchDualCardsSlot(slotId);
            return;
        }
        loge("setDefault4GSlot: radio is unavailable, return with failure");
        this.mChipHisi.mAutoSwitchDualCardsSlotDone = false;
        this.mChipCommon.sendResponseToTarget(responseMsg, 2);
        this.mChipCommon.mSet4GSlotCompleteMsg = null;
        this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_TRANS_TO_DEFAULT).sendToTarget();
    }

    private void fastSwitchDualCardsSlot(int expectedMainSlotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fastSwitchDualCardsSlot: expectedMainSlot=");
        stringBuilder.append(expectedMainSlotId);
        logd(stringBuilder.toString());
        int switchSlotType = 0;
        if (HwVSimUtils.isVSimEnabled() || HwVSimUtils.isVSimCauseCardReload() || HwVSimUtils.isSubActivationUpdate() || !HwVSimUtils.isAllowALSwitch()) {
            logd("vsim on sub");
            this.mChipHisi.setWaitingSwitchBalongSlot(false);
            this.mChipCommon.isSet4GSlotInSwitchProgress = false;
            this.mChipCommon.sendResponseToTarget(this.mChipCommon.mSet4GSlotCompleteMsg, 2);
            this.mChipCommon.mSet4GSlotCompleteMsg = null;
            this.mStateHandler.sendMessage(this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_TRANS_TO_DEFAULT));
            return;
        }
        SubscriptionController.getInstance().setDataSubId(expectedMainSlotId);
        stringBuilder = new StringBuilder();
        stringBuilder.append("fastSwitchDualCardsSlot:setDefaultDataSubId=");
        stringBuilder.append(expectedMainSlotId);
        stringBuilder.append(",only set database to expectedMainSlotId");
        logd(stringBuilder.toString());
        ProxyController proxyController = ProxyController.getInstance();
        int cdmaSimSlotId = getCdmaSimCardSlotId(expectedMainSlotId);
        if (isNeedSetRadioCapability(expectedMainSlotId, cdmaSimSlotId)) {
            if (this.mChipCommon.mSet4GSlotCompleteMsg != null) {
                switchSlotType = 1;
            }
            Message switchSoltTypeMsg = obtainMessage(HwFullNetworkConstants.EVENT_SWITCH_SLOT_TYPE_DONE);
            if (this.mCis[this.mChipCommon.current4GSlotBackup] != null) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("fastSwitchDualCardsSlot: sendSimChgTypeInfo type:");
                stringBuilder2.append(switchSlotType);
                logd(stringBuilder2.toString());
                this.mCis[this.mChipCommon.current4GSlotBackup].sendSimChgTypeInfo(switchSlotType, switchSoltTypeMsg);
            }
            if (!proxyController.setRadioCapability(expectedMainSlotId, cdmaSimSlotId)) {
                logd("fastSwitchDualCardsSlot: setRadioCapability fail ,response GENERIC_FAILURE");
                this.mChipCommon.sendResponseToTarget(obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_DONE, Integer.valueOf(expectedMainSlotId)), 2);
            }
            startFastSwithSIMSlotTimer();
            return;
        }
        logd("fastSwitchDualCardsSlot: don't need SetRadioCapability,response SUCCESS");
        this.mChipCommon.sendResponseToTarget(obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_DONE, Integer.valueOf(expectedMainSlotId)), 0);
    }

    private int getCdmaSimCardSlotId(int expectedMainSlotId) {
        HwTelephonyManagerInner mHwTelephonyManager = HwTelephonyManagerInner.getDefault();
        if (mHwTelephonyManager.isCDMASimCard(0) && mHwTelephonyManager.isCDMASimCard(1)) {
            return expectedMainSlotId;
        }
        if (mHwTelephonyManager.isCDMASimCard(0)) {
            return 0;
        }
        if (mHwTelephonyManager.isCDMASimCard(1)) {
            return 1;
        }
        return -1;
    }

    private boolean isNeedSetRadioCapability(int expectedMainSlotId, int cdmaSimSlotId) {
        Phone[] mPhones = PhoneFactory.getPhones();
        if (mPhones == null) {
            logd("isNeedSetRadioCapability: mPhones is null");
            return false;
        }
        RadioCapability expectedMainSlotRC;
        boolean same = true;
        if (SubscriptionManager.isValidSlotIndex(expectedMainSlotId) && mPhones[expectedMainSlotId] != null) {
            expectedMainSlotRC = mPhones[expectedMainSlotId].getRadioCapability();
            if (expectedMainSlotRC == null || "0".equals(expectedMainSlotRC.getLogicalModemUuid())) {
                logd("isNeedSetRadioCapability: expectedMainSlotId equals with LogicalModemUuid");
            } else {
                logd("isNeedSetRadioCapability: need switch LogicalModemUuid for expectedMainSlotId");
                same = false;
            }
        }
        if (SubscriptionManager.isValidSlotIndex(cdmaSimSlotId) && mPhones[cdmaSimSlotId] != null) {
            expectedMainSlotRC = mPhones[cdmaSimSlotId].getRadioCapability();
            int cdmaSimSlotRaf = 1;
            if (expectedMainSlotRC != null) {
                cdmaSimSlotRaf = expectedMainSlotRC.getRadioAccessFamily();
            }
            if (64 != (cdmaSimSlotRaf & 64)) {
                logd("isNeedSetRadioCapability: need add RAF_1xRTT for cdmaSimSlotRaf");
                same = false;
            } else {
                logd("isNeedSetRadioCapability: cdmaSimSlotRaf has RAF_1xRTT");
            }
        }
        if (same) {
            logd("isNeedSetRadioCapability: Already in requested configuration, nothing to do.");
            return false;
        }
        this.mChipHisi.mNvRestartRildDone = true;
        return true;
    }

    protected void setRadioCapabilityDone(Intent intent) {
        logd("reset mNvRestartRildDone");
        this.mChipHisi.mNvRestartRildDone = false;
        this.mChipCommon.sendResponseToTarget(obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_DONE, Integer.valueOf(intent.getIntExtra("intContent", 0))), 0);
    }

    protected void setRadioCapabilityFailed(Intent intent) {
        Message response = obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_DONE, Integer.valueOf(this.mChipCommon.expectedDDSsubId));
        this.mChipHisi.mAutoSwitchDualCardsSlotDone = false;
        this.mChipCommon.sendResponseToTarget(response, 2);
    }

    public void setMainSlotDone(Message response, int index) {
        if (hasMessages(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_TIMEOUT)) {
            removeMessages(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_TIMEOUT);
        }
        this.mCis[index].getCdmaModeSide(obtainMessage(HwFullNetworkConstants.EVENT_GET_CDMA_MODE_SIDE_DONE, Integer.valueOf(index)));
        AsyncResult ar = response.obj;
        if (ar == null || ar.exception != null) {
            loge("EVENT_FAST_SWITCH_SIM_SLOT_DONE failed ,response GENERIC_FAILURE");
            this.mChipCommon.sendResponseToTarget(this.mChipCommon.mSet4GSlotCompleteMsg, 2);
            this.mChipCommon.mSet4GSlotCompleteMsg = null;
            revertDefaultDataSubId();
            this.mChipCommon.isSet4GSlotInSwitchProgress = false;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EVENT_FAST_SWITCH_SIM_SLOT_DONE success for slot: ");
            stringBuilder.append(index);
            logd(stringBuilder.toString());
            if (HwFullNetworkConfig.IS_CMCC_4G_DSDX_ENABLE || HwFullNetworkConfig.IS_CT_4GSWITCH_DISABLE) {
                this.mChipHisi.saveIccidsWhenAllCardsReady();
            }
            HwTelephonyManagerInner.getDefault().updateCrurrentPhone(index);
            sendHwSwitchSlotDoneBroadcast(index);
            if ("0".equals(SystemProperties.get("gsm.nvcfg.rildrestarting", "0"))) {
                logd("send mSet4GSlotCompleteMsg");
                this.mChipCommon.sendResponseToTarget(this.mChipCommon.mSet4GSlotCompleteMsg, 0);
                this.mChipCommon.mSet4GSlotCompleteMsg = null;
                this.mChipHisi.setPrefNwForCmcc(this);
            } else {
                logd("waiting for rild restart");
                this.mChipCommon.needRetrySetPrefNetwork = true;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("set DDS for slot ");
            stringBuilder2.append(index);
            logd(stringBuilder2.toString());
            this.mChipCommon.isSet4GSlotInSwitchProgress = false;
            HwTelephonyManagerInner.getDefault().setDefaultDataSlotId(index);
            Global.putInt(this.mContext.getContentResolver(), HwFullNetworkConstants.USER_DEFAULT_SUBSCRIPTION, index);
        }
        this.mChipHisi.setWaitingSwitchBalongSlot(false);
        this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_TRANS_TO_DEFAULT).sendToTarget();
        if (HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(1, "disable-data")) {
            reCheckDefaultMainSlotForMDM();
        }
        restartRildForNvcfg();
        if (this.needResartRild) {
            restartRild();
        }
    }

    private void reCheckDefaultMainSlotForMDM() {
        this.mStateHandler.sendMessage(this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT_FOR_MDM, Integer.valueOf(0)));
    }

    private void setMainSlotTimeOut(Message msg, int index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Received EVENT_FAST_SWITCH_SIM_SLOT_TIMEOUT on index ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
        this.mChipCommon.sendResponseToTarget(this.mChipCommon.mSet4GSlotCompleteMsg, 2);
        this.mChipCommon.mSet4GSlotCompleteMsg = null;
        revertDefaultDataSubId();
        this.mChipHisi.setWaitingSwitchBalongSlot(false);
        this.mChipCommon.isSet4GSlotInSwitchProgress = false;
        this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_TRANS_TO_DEFAULT).sendToTarget();
        if (HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(1, "disable-data")) {
            reCheckDefaultMainSlotForMDM();
        }
    }

    private void revertDefaultDataSubId() {
        int mainSlot = this.mChipCommon.getUserSwitchDualCardSlots();
        SubscriptionController.getInstance().setDataSubId(mainSlot);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("revertDefaultDataSubId,setDefaultDataSubId=");
        stringBuilder.append(mainSlot);
        stringBuilder.append(",only set database to original");
        logd(stringBuilder.toString());
    }

    private void restartRildForNvcfg() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("restartRildForNvcfg, mNvRestartRildDone: ");
        stringBuilder.append(this.mChipHisi.mNvRestartRildDone);
        stringBuilder.append(", mAutoSwitchDualCardsSlotDone: ");
        stringBuilder.append(this.mChipHisi.mAutoSwitchDualCardsSlotDone);
        stringBuilder.append(", mSetSdcsCompleteMsg: ");
        stringBuilder.append(this.mChipHisi.mSetSdcsCompleteMsg);
        stringBuilder.append(", gsm.nvcfg.resetrild: ");
        stringBuilder.append(SystemProperties.get("gsm.nvcfg.resetrild", "0"));
        stringBuilder.append(", isSet4GSlotInProgress: ");
        stringBuilder.append(this.mChipCommon.isSet4GSlotInProgress);
        logd(stringBuilder.toString());
        if (!this.mChipHisi.mNvRestartRildDone && this.mChipHisi.mAutoSwitchDualCardsSlotDone && this.mChipHisi.mSetSdcsCompleteMsg == null && "1".equals(SystemProperties.get("gsm.nvcfg.resetrild", "0")) && !this.mChipCommon.isSet4GSlotInProgress) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("restartRildForNvcfg needSetDataAllowCount: ");
            stringBuilder.append(this.mChipHisi.needSetDataAllowCount);
            logd(stringBuilder.toString());
            if (this.mChipHisi.needSetDataAllowCount == 0) {
                logd("restartRildForNvcfg: call restartRild");
                if (HwVSimUtils.isVSimEnabled()) {
                    logd("restartRildForNvcfg: vsim is enabled, delay it.");
                    HwVSimNvMatchController.getInstance().storeIfNeedRestartRildForNvMatch(true);
                    HwVSimNvMatchController.getInstance().restartRildIfIdle();
                    return;
                }
                this.mChipHisi.mNvRestartRildDone = true;
                restartRild();
            }
        }
    }

    private void onUnsolRestartRildStatus(Message msg) {
        AsyncResult ar = msg.obj;
        int restartRildStatus = -1;
        if (ar == null || ar.exception != null || ar.result == null) {
            logd("onUnsolRestartRildStatus: err");
        } else {
            restartRildStatus = ((Integer) ar.result).intValue();
        }
        this.needResartRild = restartRildStatus == 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUnsolRestartRildStatus:restartRildStatus=");
        stringBuilder.append(restartRildStatus);
        stringBuilder.append(" needResartRild=");
        stringBuilder.append(this.needResartRild);
        logd(stringBuilder.toString());
        if (this.needResartRild && !this.mChipCommon.isSet4GSlotInSwitchProgress) {
            restartRild();
        }
    }

    private void restartRild() {
        logd("restartRild");
        this.needResartRild = false;
        try {
            this.mCis[0].restartRild(null);
            if (HwDsdsController.IS_DSDSPOWER_SUPPORT && !HwModemCapability.isCapabilitySupport(16)) {
                this.mCis[1].restartRild(null);
            }
        } catch (RuntimeException e) {
        }
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
