package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.telephony.HwTelephonyManagerInner;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.SubscriptionController;

public abstract class HwFullNetworkCheckStateBase extends Handler {
    public int defaultMainSlot = 0;
    protected Handler mCheckStateHandler;
    public HwFullNetworkChipCommon mChipCommon = null;
    protected CommandsInterface[] mCis;
    public HwFullNetworkOperatorBase mOperatorBase = null;

    protected abstract boolean checkIfAllCardsReady(Message message);

    protected abstract void checkNetworkType();

    protected abstract int getDefaultMainSlot();

    protected abstract boolean judgeSetDefault4GSlotForCMCC(int i);

    protected abstract void logd(String str);

    protected abstract void loge(String str);

    public HwFullNetworkCheckStateBase(Context c, CommandsInterface[] ci, Handler h) {
        this.mCis = ci;
        this.mCheckStateHandler = h;
        this.mChipCommon = HwFullNetworkChipCommon.getInstance();
        this.mOperatorBase = HwFullNetworkOperatorFactory.getOperatorBase();
        logd("HwFullNetworkCheckStateBase constructor");
    }

    public boolean judgeDefaultMainSlotForMDM() {
        boolean isSub0Active = SubscriptionController.getInstance().getSubState(0) == 1;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("judgeDefaultMainSlotForMDM isSub0Active = ");
        stringBuilder.append(isSub0Active);
        logd(stringBuilder.toString());
        this.defaultMainSlot = this.mChipCommon.getUserSwitchDualCardSlots();
        StringBuilder stringBuilder2;
        if (HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(1, "disable-data") && this.mChipCommon.isCardPresent(0) && this.mChipCommon.isCardPresent(1) && isSub0Active) {
            this.defaultMainSlot = 0;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("disable-data  defaultMainSlot= ");
            stringBuilder2.append(this.defaultMainSlot);
            logd(stringBuilder2.toString());
            return true;
        } else if (!HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(1, "disable-sub") || !this.mChipCommon.isCardPresent(0)) {
            return false;
        } else {
            this.defaultMainSlot = 0;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("disable-sub  defaultMainSlot= ");
            stringBuilder2.append(this.defaultMainSlot);
            logd(stringBuilder2.toString());
            return true;
        }
    }
}
