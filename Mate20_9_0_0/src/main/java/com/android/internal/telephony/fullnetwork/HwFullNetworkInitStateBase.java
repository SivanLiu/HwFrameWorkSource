package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.HwIccUtils;

public abstract class HwFullNetworkInitStateBase extends Handler {
    protected static HwFullNetworkChipCommon mChipCommon;
    protected static final Object mLock = new Object();
    protected CommandsInterface[] mCis;
    protected Context mContext;
    protected Handler mStateHandler;

    protected abstract void logd(String str);

    protected abstract void loge(String str);

    public abstract void onGetIccCardStatusDone(AsyncResult asyncResult, Integer num);

    public HwFullNetworkInitStateBase(Context c, CommandsInterface[] ci, Handler h) {
        this.mContext = c;
        this.mCis = ci;
        this.mStateHandler = h;
        mChipCommon = HwFullNetworkChipCommon.getInstance();
        int i = 0;
        for (int i2 = 0; i2 < HwFullNetworkConstants.SIM_NUM; i2++) {
            mChipCommon.isSimInsertedArray[i2] = false;
        }
        while (i < this.mCis.length) {
            Integer index = Integer.valueOf(i);
            this.mCis[i].registerForIccStatusChanged(this, 1001, index);
            this.mCis[i].registerForAvailable(this, 1001, index);
            i++;
        }
        initDefaultDBIfNeeded();
    }

    private void initDefaultDBIfNeeded() {
        try {
            System.getInt(this.mContext.getContentResolver(), "switch_dual_card_slots");
        } catch (SettingNotFoundException e) {
            logd("Settings Exception Reading Dual Sim Switch Dual Card Slots Values");
            System.putInt(this.mContext.getContentResolver(), "switch_dual_card_slots", 0);
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
        int i = msg.what;
        StringBuilder stringBuilder2;
        if (i == 1001) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Received EVENT_ICC_STATUS_CHANGED on index ");
            stringBuilder2.append(index);
            logd(stringBuilder2.toString());
            onIccStatusChanged(index);
        } else if (i == HwFullNetworkConstants.EVENT_GET_ICCID_DONE) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Received EVENT_GET_ICCID_DONE on index ");
            stringBuilder2.append(index);
            logd(stringBuilder2.toString());
            onGetIccidDone(ar, index);
        }
    }

    protected void onIccStatusChanged(Integer index) {
        if (HwFullNetworkConfig.IS_CMCC_4G_DSDX_ENABLE || HwFullNetworkConfig.IS_CT_4GSWITCH_DISABLE) {
            this.mCis[index.intValue()].getICCID(obtainMessage(HwFullNetworkConstants.EVENT_GET_ICCID_DONE, index));
        }
    }

    protected void onGetIccidDone(AsyncResult ar, Integer index) {
        if (ar == null || ar.exception != null) {
            logd("get iccid exception, maybe card is absent. set iccid as \"\"");
            mChipCommon.mIccIds[index.intValue()] = "";
            return;
        }
        byte[] data = ar.result;
        String iccid = HwIccUtils.bcdIccidToString(data, 0, data.length);
        if (TextUtils.isEmpty(iccid) || 7 > iccid.length()) {
            logd("iccId is invalid, set it as \"\" ");
            mChipCommon.mIccIds[index.intValue()] = "";
        } else {
            mChipCommon.mIccIds[index.intValue()] = iccid.substring(0, 7);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("get iccid is ");
        stringBuilder.append(SubscriptionInfo.givePrintableIccid(mChipCommon.mIccIds[index.intValue()]));
        stringBuilder.append(" on index ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
    }
}
