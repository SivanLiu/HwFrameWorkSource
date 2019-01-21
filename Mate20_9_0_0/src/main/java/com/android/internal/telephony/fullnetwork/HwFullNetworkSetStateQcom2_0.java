package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Global;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.HwSubscriptionManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;

public class HwFullNetworkSetStateQcom2_0 extends HwFullNetworkSetStateBase {
    private static final String LOG_TAG = "HwFullNetworkSetStateQcom2_0";
    private static final int MESSAGE_PENDING_DELAY = 500;
    private static final int RETRY_MAX_TIME = 20;
    private HwFullNetworkChipQcom mChipQcom;
    private int retryCount;
    private boolean updateUserDefaultFlag;

    HwFullNetworkSetStateQcom2_0(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        this.mChipQcom = null;
        this.updateUserDefaultFlag = false;
        this.retryCount = 0;
        this.mChipQcom = HwFullNetworkChipQcom.getInstance();
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
        int i = msg.what;
        if (i != HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_DONE) {
            switch (i) {
                case HwFullNetworkConstants.EVENT_SET_PREF_NETWORK_TIMEOUT /*2201*/:
                    logd("EVENT_SET_PREF_NETWORK_TIMEOUT");
                    this.mChipCommon.sendResponseToTarget(this.mChipCommon.mSet4GSlotCompleteMsg, 2);
                    this.mChipCommon.mSet4GSlotCompleteMsg = null;
                    this.mChipCommon.isSet4GSlotInProgress = false;
                    this.updateUserDefaultFlag = false;
                    this.retryCount = 20;
                    break;
                case HwFullNetworkConstants.MSG_RETRY_SET_DEFAULT_LTESLOT /*2202*/:
                    logd("MSG_RETRY_SET_DEFAULT_LTESLOT");
                    setPrefNetworkTypeAndStartTimer(msg.arg1);
                    break;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown msg:");
                    stringBuilder.append(msg.what);
                    logd(stringBuilder.toString());
                    break;
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Received EVENT_SET_MAIN_SLOT_DONE on index ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
        setMainSlotDone(msg, index.intValue());
    }

    private void setPrefNetworkTypeAndStartTimer(int slotId) {
        startSetPrefNetworkTimer();
        ProxyController.getInstance().retrySetRadioCapabilities();
    }

    public void setMainSlot(int slotId, Message response) {
        this.mChipCommon.expectedDDSsubId = slotId;
        StringBuilder stringBuilder;
        int needSetCount;
        if (!this.mChipQcom.isSetDefault4GSlotNeeded(slotId)) {
            loge("setDefault4GSlot: there is no need to set the lte slot");
            this.mChipCommon.isSet4GSlotInProgress = false;
            if (HwFullNetworkConfig.IS_CARD2_CDMA_SUPPORTED && this.mChipCommon.mSet4GSlotCompleteMsg == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("In auto set 4GSlot mode , makesure DDS slot same as 4G slot so set DDS to slot: ");
                stringBuilder.append(slotId);
                logd(stringBuilder.toString());
                HwTelephonyManagerInner.getDefault().setDefaultDataSlotId(slotId);
            }
            this.mChipQcom.setLteServiceAbility();
            stringBuilder = new StringBuilder();
            stringBuilder.append("set DDS to slot: ");
            stringBuilder.append(slotId);
            logd(stringBuilder.toString());
            HwTelephonyManagerInner.getDefault().setDefaultDataSlotId(slotId);
            needSetCount = PhoneFactory.onDataSubChange(0, null);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("needSetCount = ");
            stringBuilder2.append(needSetCount);
            stringBuilder2.append("; mNeedSetAllowData = ");
            stringBuilder2.append(this.mChipQcom.mNeedSetAllowData);
            logd(stringBuilder2.toString());
            if (needSetCount == 0 && this.mChipQcom.mNeedSetAllowData) {
                this.mChipQcom.mNeedSetAllowData = false;
                for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
                    PhoneFactory.resendDataAllowed(i);
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("setDefault4GSlot resend data allow with slot ");
                    stringBuilder3.append(i);
                    logd(stringBuilder3.toString());
                }
            }
            this.mChipCommon.sendResponseToTarget(this.mChipCommon.mSet4GSlotCompleteMsg, 0);
            this.mChipCommon.mSet4GSlotCompleteMsg = null;
            this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_TRANS_TO_DEFAULT).sendToTarget();
        } else if (this.mCis[slotId] == null || RadioState.RADIO_UNAVAILABLE != this.mCis[slotId].getRadioState()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setDefault4GSlot: target slot id is: ");
            stringBuilder.append(slotId);
            stringBuilder.append(" response:");
            stringBuilder.append(response);
            logd(stringBuilder.toString());
            sendHwSwitchSlotStartBroadcast();
            this.mChipCommon.mSet4GSlotCompleteMsg = response;
            this.mChipCommon.isSet4GSlotInProgress = true;
            this.mChipCommon.current4GSlotBackup = this.mChipCommon.getUserSwitchDualCardSlots();
            if (HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK) {
                sendSetRadioCapabilitySuccess(false);
            } else {
                needSetCount = this.mChipQcom.getExpectedMaxCapabilitySubId(slotId);
                if (-1 != needSetCount) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("setDefault4GSlot:setMaxRadioCapability, expectedMaxCapabilitySubId = ");
                    stringBuilder4.append(needSetCount);
                    logd(stringBuilder4.toString());
                    setRadioCapability(needSetCount);
                } else {
                    logd("setDefault4GSlot:don't setMaxRadioCapability, response message");
                    sendSetRadioCapabilitySuccess(false);
                }
            }
        } else {
            loge("setDefault4GSlot: radio is unavailable, return with failure");
            this.mChipCommon.sendResponseToTarget(response, 2);
            this.mChipCommon.mSet4GSlotCompleteMsg = null;
            this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_TRANS_TO_DEFAULT).sendToTarget();
        }
    }

    public void setMainSlotDone(Message response, int index) {
        if (hasMessages(HwFullNetworkConstants.EVENT_SET_PREF_NETWORK_TIMEOUT)) {
            removeMessages(HwFullNetworkConstants.EVENT_SET_PREF_NETWORK_TIMEOUT);
        }
        AsyncResult ar = response.obj;
        StringBuilder stringBuilder;
        if (ar == null || ar.exception != null) {
            this.mChipQcom.refreshCardState();
            loge("EVENT_SET_MAIN_SLOT_DONE failed ,response GENERIC_FAILURE");
            if (!this.mChipCommon.isSimInsertedArray[this.mChipCommon.default4GSlot]) {
                this.retryCount = 20;
                logd("current app destoryed, this error don't retry and set retryCount to max value");
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage: EVENT_SET_PREF_NETWORK_DONE failed for slot: ");
            stringBuilder.append(index);
            stringBuilder.append("with retryCount = ");
            stringBuilder.append(this.retryCount);
            loge(stringBuilder.toString());
            if (this.retryCount < 20) {
                this.retryCount++;
                sendMessageDelayed(obtainMessage(HwFullNetworkConstants.MSG_RETRY_SET_DEFAULT_LTESLOT, index, 0), 500);
                return;
            }
            this.mChipCommon.sendResponseToTarget(this.mChipCommon.mSet4GSlotCompleteMsg, 2);
            this.retryCount = 0;
        } else {
            HwTelephonyManagerInner.getDefault().updateCrurrentPhone(index);
            this.mChipCommon.sendResponseToTarget(this.mChipCommon.mSet4GSlotCompleteMsg, 0);
            if (this.mChipCommon.mSet4GSlotCompleteMsg == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("slience set network mode done, prepare to set DDS for slot ");
                stringBuilder.append(index);
                logd(stringBuilder.toString());
                HwTelephonyManagerInner.getDefault().setDefaultDataSlotId(index);
                if (PhoneFactory.onDataSubChange(0, null) == 0) {
                    for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
                        PhoneFactory.resendDataAllowed(i);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("EVENT_SET_PREF_NETWORK_DONE resend data allow with slot ");
                        stringBuilder2.append(i);
                        logd(stringBuilder2.toString());
                    }
                }
            }
            if (this.updateUserDefaultFlag) {
                Global.putInt(this.mContext.getContentResolver(), HwFullNetworkConstants.USER_DEFAULT_SUBSCRIPTION, index);
            }
            this.retryCount = 0;
        }
        this.mChipCommon.mSet4GSlotCompleteMsg = null;
        this.mChipCommon.isSet4GSlotInProgress = false;
        this.updateUserDefaultFlag = false;
        this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_TRANS_TO_DEFAULT).sendToTarget();
    }

    private void setRadioCapability(int ddsSubId) {
        ProxyController proxyController = ProxyController.getInstance();
        Phone[] phones = null;
        try {
            phones = PhoneFactory.getPhones();
        } catch (Exception ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPhones exception:");
            stringBuilder.append(ex.getMessage());
            logd(stringBuilder.toString());
        }
        if (SubscriptionManager.isValidSubscriptionId(ddsSubId) && phones != null) {
            RadioAccessFamily[] rafs = new RadioAccessFamily[phones.length];
            boolean atLeastOneMatch = false;
            for (int phoneId = 0; phoneId < phones.length; phoneId++) {
                int raf;
                int id = phones[phoneId].getSubId();
                if (id == ddsSubId) {
                    raf = proxyController.getMaxRafSupported();
                    atLeastOneMatch = true;
                } else {
                    raf = proxyController.getMinRafSupported();
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[setMaxRadioCapability] phoneId=");
                stringBuilder2.append(phoneId);
                stringBuilder2.append(" subId=");
                stringBuilder2.append(id);
                stringBuilder2.append(" raf=");
                stringBuilder2.append(raf);
                logd(stringBuilder2.toString());
                rafs[phoneId] = new RadioAccessFamily(phoneId, raf);
            }
            if (atLeastOneMatch) {
                proxyController.setRadioCapability(rafs);
                startSetPrefNetworkTimer();
                return;
            }
            logd("[setMaxRadioCapability] no valid subId's found - not updating.");
        }
    }

    private void sendSetRadioCapabilitySuccess(boolean needChangeNetworkTypeInDB) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendSetRadioCapabilitySuccess,needChangeNetworkTypeInDB:");
        stringBuilder.append(needChangeNetworkTypeInDB);
        logd(stringBuilder.toString());
        Message response = obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_DONE, Integer.valueOf(this.mChipCommon.expectedDDSsubId));
        AsyncResult.forMessage(response, null, null);
        response.sendToTarget();
        sendHwSwitchSlotDoneBroadcast(this.mChipCommon.expectedDDSsubId);
        int slotId = 0;
        while (true) {
            boolean z = true;
            if (slotId >= HwFullNetworkConstants.SIM_NUM) {
                break;
            }
            if (-1 != this.mChipQcom.mSetUiccSubscriptionResult[slotId]) {
                if (this.mChipQcom.mSetUiccSubscriptionResult[slotId] != 1) {
                    z = false;
                }
                boolean active = z;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sendSetRadioCapabilitySuccess,setSubscription: slotId = ");
                stringBuilder2.append(slotId);
                stringBuilder2.append(", activate = ");
                stringBuilder2.append(active);
                logd(stringBuilder2.toString());
                HwSubscriptionManager.getInstance().setSubscription(slotId, active, null);
            }
            slotId++;
        }
        if (HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK && !this.mChipCommon.isDualImsSwitchOpened()) {
            this.mChipQcom.mNeedSetLteServiceAbility = true;
            exchangeNetworkTypeInDB();
        } else if (needChangeNetworkTypeInDB) {
            exchangeNetworkTypeInDB();
        }
        this.mChipQcom.setLteServiceAbility();
    }

    protected void setRadioCapabilityDone(Intent intent) {
        sendSetRadioCapabilitySuccess(true);
    }

    protected void setRadioCapabilityFailed(Intent intent) {
        Message response = obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_DONE, Integer.valueOf(this.mChipCommon.expectedDDSsubId));
        AsyncResult.forMessage(response, null, new Exception());
        response.sendToTarget();
    }

    private void startSetPrefNetworkTimer() {
        Message message = obtainMessage(HwFullNetworkConstants.EVENT_SET_PREF_NETWORK_TIMEOUT);
        AsyncResult.forMessage(message, null, null);
        sendMessageDelayed(message, 60000);
        logd("startSetPrefNetworkTimer!");
    }

    private void exchangeNetworkTypeInDB() {
        int previousNetworkTypeSub0 = this.mChipQcom.getNetworkTypeFromDB(0);
        int previousNetworkTypeSub1 = this.mChipQcom.getNetworkTypeFromDB(1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("exchangeNetworkTypeInDB PREFERRED_NETWORK_MODE:");
        stringBuilder.append(previousNetworkTypeSub0);
        stringBuilder.append(",");
        stringBuilder.append(previousNetworkTypeSub1);
        stringBuilder.append("->");
        stringBuilder.append(previousNetworkTypeSub1);
        stringBuilder.append(",");
        stringBuilder.append(previousNetworkTypeSub0);
        logd(stringBuilder.toString());
        this.mChipQcom.setNetworkTypeToDB(0, previousNetworkTypeSub1);
        this.mChipQcom.setNetworkTypeToDB(1, previousNetworkTypeSub0);
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
