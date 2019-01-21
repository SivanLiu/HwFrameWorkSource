package com.android.internal.telephony.fullnetwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.CommandsInterface;

public abstract class HwFullNetworkSetStateBase extends Handler {
    public HwFullNetworkChipCommon mChipCommon = null;
    protected CommandsInterface[] mCis;
    protected Context mContext;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwFullNetworkSetStateBase.this.loge("intent is null, return");
                return;
            }
            if ("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE".equals(intent.getAction())) {
                HwFullNetworkSetStateBase.this.logd("received ACTION_SET_RADIO_CAPABILITY_DONE");
                HwFullNetworkSetStateBase.this.setRadioCapabilityDone(intent);
            } else if ("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED".equals(intent.getAction())) {
                HwFullNetworkSetStateBase.this.logd("received ACTION_SET_RADIO_CAPABILITY_FAILED");
                HwFullNetworkSetStateBase.this.setRadioCapabilityFailed(intent);
                HwFullNetworkSetStateBase.this.sendHwSwitchSlotFailedBroadcast();
            }
        }
    };
    protected Handler mStateHandler = null;

    protected abstract void logd(String str);

    protected abstract void loge(String str);

    public abstract void setMainSlot(int i, Message message);

    public abstract void setMainSlotDone(Message message, int i);

    protected abstract void setRadioCapabilityDone(Intent intent);

    protected abstract void setRadioCapabilityFailed(Intent intent);

    public HwFullNetworkSetStateBase(Context c, CommandsInterface[] ci, Handler h) {
        this.mContext = c;
        this.mCis = ci;
        this.mStateHandler = h;
        this.mChipCommon = HwFullNetworkChipCommon.getInstance();
        IntentFilter filter = new IntentFilter("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        filter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    public void startFastSwithSIMSlotTimer() {
        Message message = obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT_TIMEOUT);
        AsyncResult.forMessage(message, null, null);
        sendMessageDelayed(message, 60000);
        logd("startFastSwithSIMSlotTimer");
    }

    protected void sendHwSwitchSlotStartBroadcast() {
        Intent intent = new Intent("com.huawei.action.ACTION_HW_SWITCH_SLOT_DONE");
        intent.putExtra(HwFullNetworkConstants.HW_SWITCH_SLOT_STEP, 0);
        this.mContext.sendBroadcast(intent);
    }

    protected void sendHwSwitchSlotDoneBroadcast(int mainSlotId) {
        Intent intent = new Intent("com.huawei.action.ACTION_HW_SWITCH_SLOT_DONE");
        int i = 1;
        intent.putExtra(HwFullNetworkConstants.HW_SWITCH_SLOT_STEP, 1);
        int oldSlotId = this.mChipCommon.getUserSwitchDualCardSlots();
        this.mChipCommon.setUserSwitchDualCardSlots(mainSlotId);
        String str = HwFullNetworkConstants.IF_NEED_SET_RADIO_CAP;
        if (oldSlotId != mainSlotId) {
            i = 0;
        }
        intent.putExtra(str, i);
        this.mContext.sendBroadcast(intent);
    }

    protected void sendHwSwitchSlotFailedBroadcast() {
        Intent intent = new Intent("com.huawei.action.ACTION_HW_SWITCH_SLOT_DONE");
        intent.putExtra(HwFullNetworkConstants.HW_SWITCH_SLOT_STEP, -1);
        this.mContext.sendBroadcast(intent);
    }
}
