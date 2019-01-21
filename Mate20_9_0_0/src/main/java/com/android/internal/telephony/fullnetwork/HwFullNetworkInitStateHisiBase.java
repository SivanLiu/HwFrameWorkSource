package com.android.internal.telephony.fullnetwork;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HwAESCryptoUtil;
import com.android.internal.telephony.HwDsdsController;
import com.android.internal.telephony.HwHotplugController;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants.HotplugState;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants.SubCarrierType;
import com.android.internal.telephony.uicc.HwIccUtils;
import com.android.internal.telephony.vsim.HwVSimUtils;
import java.util.Arrays;

public abstract class HwFullNetworkInitStateHisiBase extends HwFullNetworkInitStateBase {
    private static final String LOG_TAG = "HwFullNetworkInitStateHisiBase";
    protected HwFullNetworkChipHisi mChipHisi = HwFullNetworkChipHisi.getInstance();
    protected HwHotplugController mHotPlugController;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwFullNetworkInitStateHisiBase.this.loge("intent is null, return");
                return;
            }
            if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                HwFullNetworkInitStateHisiBase.this.processSimStateChanged(intent);
            }
        }
    };

    public HwFullNetworkInitStateHisiBase(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        initParams();
        if (HwHotplugController.IS_HOTSWAP_SUPPORT) {
            this.mHotPlugController = HwHotplugController.getInstance();
        }
        for (int i = 0; i < this.mCis.length; i++) {
            this.mCis[i].registerForSimHotPlug(this, HwFullNetworkConstants.EVENT_SIM_HOTPLUG, Integer.valueOf(i));
        }
        this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
    }

    private void initParams() {
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            this.mChipHisi.mGetUiccCardsStatusDone[i] = false;
            this.mChipHisi.mGetBalongSimSlotDone[i] = false;
            this.mChipHisi.mSwitchTypes[i] = -1;
            this.mChipHisi.mCardTypes[i] = -1;
            this.mChipHisi.mRadioOns[i] = false;
            mChipCommon.mIccIds[i] = null;
            mChipCommon.subCarrierTypeArray[i] = null;
            this.mChipHisi.mHotplugState[i] = HotplugState.STATE_PLUG_OUT;
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null) {
            loge("msg is null, return!");
            return;
        }
        Integer index = mChipCommon.getCiIndex(msg);
        if (index.intValue() < 0 || index.intValue() >= this.mCis.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid index : ");
            stringBuilder.append(index);
            stringBuilder.append(" received with event ");
            stringBuilder.append(msg.what);
            loge(stringBuilder.toString());
            return;
        }
        AsyncResult ar = null;
        if (msg.obj != null && (msg.obj instanceof AsyncResult)) {
            ar = msg.obj;
        }
        StringBuilder stringBuilder2;
        switch (msg.what) {
            case 1001:
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Received EVENT_ICC_STATUS_CHANGED on index ");
                stringBuilder2.append(index);
                logd(stringBuilder2.toString());
                onIccStatusChanged(index);
                break;
            case HwFullNetworkConstants.EVENT_QUERY_CARD_TYPE_DONE /*1005*/:
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Received EVENT_QUERY_CARD_TYPE_DONE on index ");
                stringBuilder2.append(index);
                logd(stringBuilder2.toString());
                if (ar != null && ar.exception == null) {
                    onQueryCardTypeDone(ar, index);
                    if (this.mHotPlugController != null) {
                        this.mHotPlugController.onHotPlugQueryCardTypeDone(ar, index);
                        break;
                    }
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Received EVENT_QUERY_CARD_TYPE_DONE got exception, ar  = ");
                stringBuilder2.append(ar);
                logd(stringBuilder2.toString());
                break;
                break;
            case HwFullNetworkConstants.EVENT_GET_BALONG_SIM_DONE /*1006*/:
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Received EVENT_GET_BALONG_SIM_DONE on index ");
                stringBuilder2.append(index);
                logd(stringBuilder2.toString());
                if (HwDsdsController.IS_DSDSPOWER_SUPPORT) {
                    HwDsdsController.getInstance().onGetBalongSimDone(ar, index);
                }
                onGetBalongSimDone(ar, index);
                break;
            case HwFullNetworkConstants.EVENT_SIM_HOTPLUG /*1008*/:
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Received EVENT_SIM_HOTPLUG on index ");
                stringBuilder2.append(index);
                logd(stringBuilder2.toString());
                onSimHotPlug(ar, index);
                break;
            case HwFullNetworkConstants.EVENT_GET_ICCID_DONE /*1009*/:
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Received EVENT_GET_ICCID_DONE on index ");
                stringBuilder2.append(index);
                logd(stringBuilder2.toString());
                onGetIccidDone(ar, index);
                break;
            case HwFullNetworkConstants.EVENT_GET_ICC_STATUS_DONE /*1012*/:
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Received EVENT_GET_ICC_STATUS_DONE on index ");
                stringBuilder2.append(index);
                logd(stringBuilder2.toString());
                onGetIccStatusDone(index);
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    public void onIccStatusChanged(Integer index) {
        if (HwFullNetworkConfig.IS_CMCC_4G_DSDX_ENABLE || HwFullNetworkConfig.IS_CT_4GSWITCH_DISABLE) {
            this.mCis[index.intValue()].getICCID(obtainMessage(HwFullNetworkConstants.EVENT_GET_ICCID_DONE, index));
        }
        this.mCis[index.intValue()].queryCardType(obtainMessage(HwFullNetworkConstants.EVENT_QUERY_CARD_TYPE_DONE, index));
        this.mCis[index.intValue()].getBalongSim(obtainMessage(HwFullNetworkConstants.EVENT_GET_BALONG_SIM_DONE, index));
        if (this.mHotPlugController != null) {
            this.mHotPlugController.onHotPlugIccStatusChanged(index);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x006c A:{Catch:{ all -> 0x005f }} */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x007e A:{Catch:{ all -> 0x005f }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void onQueryCardTypeDone(AsyncResult ar, Integer index) {
        int countGetCardTypeDone;
        int i = 0;
        if (ar != null) {
            try {
                if (ar.result != null) {
                    this.mChipHisi.mCardTypes[index.intValue()] = ((int[]) ar.result)[0];
                    this.mChipHisi.mSwitchTypes[index.intValue()] = ((int[]) ar.result)[0] & 15;
                    saveCardTypeProperties(((int[]) ar.result)[0], index.intValue());
                    HwVSimUtils.updateSimCardTypes(this.mChipHisi.mSwitchTypes);
                    if (!this.mChipHisi.mBroadcastDone && !HwHotplugController.IS_HOTSWAP_SUPPORT && HwFullNetworkConfig.IS_CHINA_TELECOM && isNoneCTcard()) {
                        this.mChipHisi.mBroadcastDone = true;
                        broadcastForHwCardManager(index.intValue());
                    }
                    countGetCardTypeDone = 0;
                    while (i < HwFullNetworkConstants.SIM_NUM) {
                        if (this.mChipHisi.mSwitchTypes[i] != -1) {
                            countGetCardTypeDone++;
                        }
                        i++;
                    }
                    if (countGetCardTypeDone == HwFullNetworkConstants.SIM_NUM) {
                        logd("onQueryCardTypeDone check main slot.");
                        this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, index).sendToTarget();
                    }
                }
            } finally {
            }
        }
        loge("onQueryCardTypeDone error.");
        countGetCardTypeDone = 0;
        while (i < HwFullNetworkConstants.SIM_NUM) {
        }
        if (countGetCardTypeDone == HwFullNetworkConstants.SIM_NUM) {
        }
    }

    public void onGetBalongSimDone(AsyncResult ar, Integer index) {
        StringBuilder stringBuilder;
        int i = 0;
        if (ar != null && ar.result != null && ((int[]) ar.result).length == 3) {
            int[] slots = ar.result;
            boolean isMainSlotOnVSim = false;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("slot result = ");
            stringBuilder2.append(Arrays.toString(slots));
            logd(stringBuilder2.toString());
            if (slots[0] == 0 && slots[1] == 1 && slots[2] == 2) {
                this.mChipHisi.mBalongSimSlot = 0;
                isMainSlotOnVSim = false;
            } else if (slots[0] == 1 && slots[1] == 0 && slots[2] == 2) {
                this.mChipHisi.mBalongSimSlot = 1;
                isMainSlotOnVSim = false;
            } else if (slots[0] == 2 && slots[1] == 1 && slots[2] == 0) {
                this.mChipHisi.mBalongSimSlot = 0;
                isMainSlotOnVSim = true;
            } else if (slots[0] == 2 && slots[1] == 0 && slots[2] == 1) {
                this.mChipHisi.mBalongSimSlot = 1;
                isMainSlotOnVSim = true;
            } else {
                loge("onGetBalongSimDone invalid slot result");
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("isMainSlotOnVSim = ");
            stringBuilder.append(isMainSlotOnVSim);
            logd(stringBuilder.toString());
            this.mChipHisi.mGetBalongSimSlotDone[index.intValue()] = true;
        } else if (ar == null || ar.result == null || ((int[]) ar.result).length != 2) {
            loge("onGetBalongSimDone error");
        } else {
            if (((int[]) ar.result)[0] + ((int[]) ar.result)[1] > 1) {
                this.mChipHisi.mBalongSimSlot = ((int[]) ar.result)[0] - 1;
            } else {
                this.mChipHisi.mBalongSimSlot = ((int[]) ar.result)[0];
            }
            this.mChipHisi.mGetBalongSimSlotDone[index.intValue()] = true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("mBalongSimSlot = ");
        stringBuilder.append(this.mChipHisi.mBalongSimSlot);
        logd(stringBuilder.toString());
        int countGetBalongSimSlotDone = 0;
        while (i < HwFullNetworkConstants.SIM_NUM) {
            if (this.mChipHisi.mGetBalongSimSlotDone[i]) {
                countGetBalongSimSlotDone++;
            }
            i++;
        }
        if (countGetBalongSimSlotDone == 1) {
            logd("onGetBalongSimDone check main slot.");
            this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, index).sendToTarget();
        }
    }

    public void onGetIccidDone(AsyncResult ar, Integer index) {
        if (ar == null || ar.exception != null) {
            if (ar != null && (ar.exception instanceof CommandException) && ((CommandException) ar.exception).getCommandError() == Error.RADIO_NOT_AVAILABLE) {
                logd("get iccid radio not available exception, Do Nothing.");
            } else {
                logd("get iccid exception, maybe card is absent. set iccid as \"\"");
                mChipCommon.mIccIds[index.intValue()] = "";
                this.mChipHisi.mFullIccIds[index.intValue()] = "";
            }
            this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, index).sendToTarget();
            return;
        }
        byte[] data = ar.result;
        int i = 0;
        String iccid = HwIccUtils.bcdIccidToString(data, 0, data.length);
        if (TextUtils.isEmpty(iccid) || 7 > iccid.length()) {
            logd("iccId is invalid, set it as \"\" ");
            mChipCommon.mIccIds[index.intValue()] = "";
        } else {
            mChipCommon.mIccIds[index.intValue()] = iccid.substring(0, 7);
        }
        this.mChipHisi.mFullIccIds[index.intValue()] = iccid;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("get iccid is ");
        stringBuilder.append(SubscriptionInfo.givePrintableIccid(mChipCommon.mIccIds[index.intValue()]));
        stringBuilder.append(" on index ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
        int countGetIccIdDone = 0;
        while (i < HwFullNetworkConstants.SIM_NUM) {
            if (mChipCommon.mIccIds[i] != null) {
                countGetIccIdDone++;
            }
            i++;
        }
        if (countGetIccIdDone == HwFullNetworkConstants.SIM_NUM) {
            logd("onGetIccidDone check main slot.");
            this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, index).sendToTarget();
        }
    }

    public void onSimHotPlug(AsyncResult ar, Integer index) {
        logd("onSimHotPlug");
        if (ar != null && ar.result != null && ((int[]) ar.result).length > 0) {
            if (HotplugState.STATE_PLUG_IN.ordinal() == ((int[]) ar.result)[0]) {
                if (this.mChipHisi.mHotplugState[index.intValue()] != HotplugState.STATE_PLUG_IN) {
                    this.mChipHisi.isHotPlugCompleted = true;
                    disposeCardStatus(index.intValue());
                }
            } else if (HotplugState.STATE_PLUG_OUT.ordinal() == ((int[]) ar.result)[0]) {
                HwVSimUtils.simHotPlugOut(index.intValue());
                this.mChipHisi.mHotplugState[index.intValue()] = HotplugState.STATE_PLUG_OUT;
            }
        }
    }

    public void disposeCardStatus(int slotID) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disposeCardStatus slotid = ");
        stringBuilder.append(slotID);
        logd(stringBuilder.toString());
        if (slotID >= 0 && slotID < HwFullNetworkConstants.SIM_NUM) {
            this.mChipHisi.mHotplugState[slotID] = HotplugState.STATE_PLUG_IN;
            this.mChipHisi.mSwitchTypes[slotID] = -1;
            this.mChipHisi.mGetUiccCardsStatusDone[slotID] = false;
            this.mChipHisi.mGetBalongSimSlotDone[slotID] = false;
            this.mChipHisi.mCardTypes[slotID] = -1;
            this.mChipHisi.mFullIccIds[slotID] = null;
            this.mChipHisi.mAllCardsReady = false;
            this.mChipHisi.mNvRestartRildDone = false;
            mChipCommon.mIccIds[slotID] = null;
            mChipCommon.subCarrierTypeArray[slotID] = null;
            this.mChipHisi.needFixMainSlotPosition = false;
            if (HwFullNetworkConfig.IS_CT_4GSWITCH_DISABLE) {
                saveIccidBySlot(slotID, "");
            }
            if (HwFullNetworkConfig.IS_HISI_DSDX) {
                logd("set mAutoSwitchDualCardsSlotDone to false");
                this.mChipHisi.mAutoSwitchDualCardsSlotDone = false;
                if (HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE && HwFullNetworkConfig.IS_VICE_WCDMA) {
                    Phone phone = PhoneFactory.getPhone(slotID);
                    if (phone != null) {
                        logd("disposeCardStatus: set network mode to NETWORK_MODE_GSM_UMTS");
                        phone.setPreferredNetworkType(3, null);
                    }
                }
            }
        }
    }

    private void saveCardTypeProperties(int cardTypeResult, int index) {
        int cardType = -1;
        int uiccOrIcc = (cardTypeResult & 240) >> 4;
        int appType = cardTypeResult & 15;
        switch (appType) {
            case 1:
                if (uiccOrIcc != 2) {
                    if (uiccOrIcc == 1) {
                        cardType = 10;
                        break;
                    }
                }
                cardType = 20;
                break;
                break;
            case 2:
                cardType = 30;
                break;
            case 3:
                if (uiccOrIcc != 2) {
                    if (uiccOrIcc == 1) {
                        cardType = 41;
                        break;
                    }
                }
                cardType = 43;
                break;
                break;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("uiccOrIcc :  ");
        stringBuilder.append(uiccOrIcc);
        stringBuilder.append(", appType : ");
        stringBuilder.append(appType);
        stringBuilder.append(", cardType : ");
        stringBuilder.append(cardType);
        logd(stringBuilder.toString());
        if (index == 0) {
            SystemProperties.set(HwFullNetworkConstants.CARD_TYPE_SIM1, String.valueOf(cardType));
        } else {
            SystemProperties.set(HwFullNetworkConstants.CARD_TYPE_SIM2, String.valueOf(cardType));
        }
    }

    private void saveIccidBySlot(int slot, String iccId) {
        logd("saveIccidBySlot");
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        String iccIdToSave = "";
        try {
            iccIdToSave = HwAESCryptoUtil.encrypt(HwFullNetworkConstants.MASTER_PASSWORD, iccId);
        } catch (Exception ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwAESCryptoUtil encrypt excepiton:");
            stringBuilder.append(ex.getMessage());
            logd(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("4G_AUTO_SWITCH_ICCID_SLOT");
        stringBuilder2.append(slot);
        editor.putString(stringBuilder2.toString(), iccIdToSave);
        editor.apply();
    }

    private boolean isNoneCTcard() {
        boolean z = false;
        if (HwFullNetworkConfig.IS_4G_SWITCH_SUPPORTED) {
            if (this.mChipHisi.mSwitchTypes[0] == 1 && this.mChipHisi.mSwitchTypes[1] == 1) {
                z = true;
            }
            return z;
        }
        if (this.mChipHisi.mSwitchTypes[mChipCommon.getUserSwitchDualCardSlots()] == 1) {
            z = true;
        }
        return z;
    }

    private void broadcastForHwCardManager(int sub) {
        Intent intent = new Intent("com.huawei.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        logd("[broadcastForHwCardManager]");
        intent.putExtra("popupDialog", "true");
        ActivityManager.broadcastStickyIntent(intent, 51, -1);
    }

    public void onGetIccCardStatusDone(AsyncResult ar, Integer index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetIccCardStatusDone on index ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
        if (ar.exception != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error getting ICC status. RIL_REQUEST_GET_ICC_STATUS should never return an error: ");
            stringBuilder.append(ar.exception);
            loge(stringBuilder.toString());
        } else if (mChipCommon.isValidIndex(index.intValue())) {
            this.mChipHisi.mGetUiccCardsStatusDone[index.intValue()] = true;
            if (this.mHotPlugController != null) {
                this.mHotPlugController.processNotifyPromptHotPlug(false);
            }
            sendMessage(obtainMessage(HwFullNetworkConstants.EVENT_GET_ICC_STATUS_DONE, ar));
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("onGetIccCardStatusDone: invalid index : ");
            stringBuilder.append(index);
            loge(stringBuilder.toString());
        }
    }

    private void onGetIccStatusDone(Integer index) {
        logd("onGetIccStatusDone check main slot.");
        this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, index).sendToTarget();
    }

    private void processSimStateChanged(Intent intent) {
        String simState = intent.getStringExtra("ss");
        int slotId = intent.getIntExtra("slot", -1);
        if ("IMSI".equals(simState) && mChipCommon.isValidIndex(slotId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processSimStateChanged for slot ");
            stringBuilder.append(slotId);
            logd(stringBuilder.toString());
            this.mChipHisi.refreshCardState();
            if (mChipCommon.subCarrierTypeArray[slotId] == null) {
                mChipCommon.judgeSubCarrierType();
            }
            SubCarrierType[] oldsubCarrierTypes = (SubCarrierType[]) mChipCommon.subCarrierTypeArray.clone();
            boolean diff = false;
            for (int sub = 0; sub < HwFullNetworkConstants.SIM_NUM; sub++) {
                mChipCommon.judgeSubCarrierTypeByMccMnc(sub);
                if (oldsubCarrierTypes[sub] != mChipCommon.subCarrierTypeArray[sub]) {
                    diff = true;
                }
            }
            if (diff && !this.mChipHisi.needFixMainSlotPosition) {
                loge("fix subCarrierType and recheck main slot.");
                this.mChipHisi.needFixMainSlotPosition = true;
                if (!mChipCommon.isSet4GSlotInProgress) {
                    this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, Integer.valueOf(0)).sendToTarget();
                }
            }
        }
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
