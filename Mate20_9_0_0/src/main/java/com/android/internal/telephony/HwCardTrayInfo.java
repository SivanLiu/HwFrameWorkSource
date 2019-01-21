package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;

public class HwCardTrayInfo extends Handler {
    private static final int CARDTRAY_HOTPLUG_INFO_LEN = 4;
    private static final int CARDTRAY_OUT_SLOT = 0;
    private static final int EVENT_SIM_HOTPLUG = 10;
    private static final int PHONE_COUNT = TelephonyManager.getDefault().getPhoneCount();
    private static final String TAG = "HwCardTrayInfo";
    private static final Object mLock = new Object();
    private static HwCardTrayInfo sInstance;
    private HotplugState[] mHotplugState = new HotplugState[PHONE_COUNT];
    private boolean mQueryDone;

    private enum HotplugState {
        STATE_PLUG_OUT,
        STATE_PLUG_IN
    }

    public static HwCardTrayInfo make(CommandsInterface[] ci) {
        HwCardTrayInfo hwCardTrayInfo;
        synchronized (mLock) {
            if (sInstance == null) {
                sInstance = new HwCardTrayInfo(ci);
                hwCardTrayInfo = sInstance;
            } else {
                throw new RuntimeException("HwCardTrayInfo.make() should only be called once");
            }
        }
        return hwCardTrayInfo;
    }

    private HwCardTrayInfo(CommandsInterface[] ci) {
        int i = 0;
        this.mQueryDone = false;
        if (ci != null) {
            while (i < ci.length) {
                ci[i].registerForSimHotPlug(this, 10, Integer.valueOf(i));
                i++;
            }
        }
    }

    public static HwCardTrayInfo getInstance() {
        HwCardTrayInfo hwCardTrayInfo;
        synchronized (mLock) {
            if (sInstance != null) {
                hwCardTrayInfo = sInstance;
            } else {
                throw new RuntimeException("HwCardTrayInfo.getInstance can't be called before make()");
            }
        }
        return hwCardTrayInfo;
    }

    public void handleMessage(Message msg) {
        if (msg.what != 10) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown Event ");
            stringBuilder.append(msg.what);
            loge(stringBuilder.toString());
            return;
        }
        onSimHotPlug(msg);
    }

    private void onSimHotPlug(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar != null && ar.result != null && (ar.result instanceof int[]) && ((int[]) ar.result).length > 0) {
            Integer slotId = ar.userObj;
            StringBuilder stringBuilder;
            if (isValidIndex(slotId.intValue())) {
                if (HotplugState.STATE_PLUG_IN.ordinal() == ((int[]) ar.result)[0]) {
                    this.mHotplugState[slotId.intValue()] = HotplugState.STATE_PLUG_IN;
                } else if (HotplugState.STATE_PLUG_OUT.ordinal() == ((int[]) ar.result)[0]) {
                    this.mHotplugState[slotId.intValue()] = HotplugState.STATE_PLUG_OUT;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("onSimHotPlug, mHotplugState[");
                stringBuilder.append(slotId);
                stringBuilder.append("]:");
                stringBuilder.append(this.mHotplugState[slotId.intValue()]);
                log(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("onSimHotPlug, invalid slot:");
                stringBuilder.append(slotId);
                loge(stringBuilder.toString());
            }
        }
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < PHONE_COUNT;
    }

    private void setCardTrayHotplugState(byte[] cardTrayInfo) {
        if (cardTrayInfo != null && cardTrayInfo.length >= 4) {
            for (int i = 0; i < PHONE_COUNT; i++) {
                if (HotplugState.STATE_PLUG_IN.ordinal() == cardTrayInfo[(i * 2) + 1]) {
                    this.mHotplugState[i] = HotplugState.STATE_PLUG_IN;
                } else if (HotplugState.STATE_PLUG_OUT.ordinal() == cardTrayInfo[(i * 2) + 1]) {
                    this.mHotplugState[i] = HotplugState.STATE_PLUG_OUT;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setCardTrayHotplugState, mHotplugState[");
                stringBuilder.append(i);
                stringBuilder.append("]:");
                stringBuilder.append(this.mHotplugState[i]);
                log(stringBuilder.toString());
            }
        }
    }

    public boolean isCardTrayOut(int slotId) {
        boolean z = false;
        if (!this.mQueryDone) {
            log("isCardTrayOut, first query for card tray info.");
            byte[] cardTrayInfo = HwTelephonyManagerInner.getDefault().getCardTrayInfo();
            if (cardTrayInfo != null && cardTrayInfo.length >= 4) {
                this.mQueryDone = true;
                setCardTrayHotplugState(cardTrayInfo);
                if (isValidIndex(slotId)) {
                    if (cardTrayInfo[(slotId * 2) + 1] == (byte) 0) {
                        z = true;
                    }
                    return z;
                }
            }
        } else if (isValidIndex(slotId)) {
            if (this.mHotplugState[slotId].ordinal() == 0) {
                z = true;
            }
            return z;
        }
        return false;
    }

    private void log(String string) {
        Rlog.d(TAG, string);
    }

    private void loge(String string) {
        Rlog.e(TAG, string);
    }
}
