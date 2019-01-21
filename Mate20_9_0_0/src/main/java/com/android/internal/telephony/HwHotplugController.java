package com.android.internal.telephony;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConfig;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import com.android.internal.telephony.fullnetwork.HwFullNetworkManager;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.vsim.HwVSimUtils;
import huawei.cust.HwCustUtils;

public class HwHotplugController extends Handler {
    private static final String DUALCARD_CLASS = "com.huawei.android.dsdscardmanager.HWCardManagerActivity";
    private static final String DUALCARD_CLASS_TAB = "com.huawei.android.dsdscardmanager.HWCardManagerTabActivity";
    private static final String DUALCARD_PACKAGE = "com.huawei.android.dsdscardmanager";
    private static final int EVENT_HOTPLUG_GET_STATE = 0;
    private static final int EVENT_HOTPLUG_PROCESS_SIM1_TIMEOUT = 1;
    private static final int EVENT_HOTPLUG_PROCESS_SIM2_TIMEOUT = 2;
    private static final boolean IS_CHINA_TELECOM = HuaweiTelephonyConfigs.isChinaTelecom();
    private static final boolean IS_FULL_NETWORK_SUPPORTED = HwTelephonyFactory.getHwUiccManager().isFullNetworkSupported();
    public static final boolean IS_HOTSWAP_SUPPORT = HwTelephonyFactory.getHwUiccManager().isHotswapSupported();
    private static final int SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final int STATE_HOTPLUG_ADDED = 1;
    private static final int STATE_HOTPLUG_IDLE = 0;
    private static final int STATE_HOTPLUG_PLUGING = 3;
    private static final int STATE_HOTPLUG_QUERYING = 4;
    private static final int STATE_HOTPLUG_REMOVED = 2;
    private static final String TAG = "HwHotplugController";
    private static final int TIMEOUT_HOTPLUG_PROCESS = 15000;
    private static boolean isFactroyMode = "factory".equals(SystemProperties.get("ro.runmode", "normal"));
    private static HwHotplugController mInstance;
    private static final Object mLock = new Object();
    private CardState[] mCardStates = new CardState[2];
    private CommandsInterface[] mCis;
    private Context mContext;
    HwCustHotplugController mCustHotplugController = null;
    private AlertDialog mDialog;
    private int[] mHotPlugCardTypes = new int[2];
    private int[] mHotPlugStates = new int[2];
    private boolean mIsNotifyIccIdChange = false;
    private boolean[] mIsQueryingCardTypes = new boolean[2];
    private boolean[] mIsRestartRild = new boolean[2];
    private RadioState[] mLastRadioStates = new RadioState[2];
    private boolean mProccessHotPlugDone = true;

    public static HwHotplugController make(Context context, CommandsInterface[] ci) {
        HwHotplugController hwHotplugController;
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new HwHotplugController(context, ci);
                hwHotplugController = mInstance;
            } else {
                throw new RuntimeException("HwHotplugController.make() should only be called once");
            }
        }
        return hwHotplugController;
    }

    public static HwHotplugController getInstance() {
        HwHotplugController hwHotplugController;
        synchronized (mLock) {
            if (mInstance != null) {
                hwHotplugController = mInstance;
            } else {
                throw new RuntimeException("HwHotPlugController.getInstance can't be called before make()");
            }
        }
        return hwHotplugController;
    }

    public HwHotplugController(Context c, CommandsInterface[] cis) {
        Rlog.d(TAG, "constructor init");
        this.mContext = c;
        this.mCis = cis;
        for (int i = 0; i < this.mCis.length; i++) {
            this.mHotPlugStates[i] = 0;
            this.mLastRadioStates[i] = RadioState.RADIO_UNAVAILABLE;
            this.mIsQueryingCardTypes[i] = false;
            this.mIsRestartRild[i] = false;
        }
        this.mCustHotplugController = (HwCustHotplugController) HwCustUtils.createObj(HwCustHotplugController.class, new Object[]{c});
    }

    public void initHotPlugCardState(UiccCard uc, IccCardStatus status, Integer index) {
        this.mCardStates[index.intValue()] = status.mCardState;
        RadioState radioState = this.mCis[index.intValue()].getRadioState();
        this.mLastRadioStates[index.intValue()] = radioState;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCardStates[");
        stringBuilder.append(index);
        stringBuilder.append("] : ");
        stringBuilder.append(radioState);
        stringBuilder.append(", mLastRadioStates[");
        stringBuilder.append(index);
        stringBuilder.append("] : ");
        stringBuilder.append(this.mLastRadioStates[index.intValue()]);
        Rlog.d(str, stringBuilder.toString());
    }

    public void updateHotPlugCardState(UiccCard uc, IccCardStatus status, Integer index) {
        CardState oldCardState = this.mCardStates[index.intValue()];
        this.mCardStates[index.intValue()] = status.mCardState;
        RadioState radioState = this.mCis[index.intValue()].getRadioState();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateHotPlugCardState SUB[");
        stringBuilder.append(index);
        stringBuilder.append("]: RadioState : ");
        stringBuilder.append(radioState);
        stringBuilder.append(", mLastRadioStates : ");
        stringBuilder.append(this.mLastRadioStates[index.intValue()]);
        Rlog.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("updateHotPlugCardState SUB[");
        stringBuilder.append(index);
        stringBuilder.append("]: Oldcard state : ");
        stringBuilder.append(oldCardState);
        stringBuilder.append(", Newcard state : ");
        stringBuilder.append(this.mCardStates[index.intValue()]);
        Rlog.d(str, stringBuilder.toString());
        if (index.intValue() == 0) {
            boolean z = true;
        } else {
            int otherIndex = 0;
        }
        if (oldCardState != CardState.CARDSTATE_ABSENT && this.mCardStates[index.intValue()] == CardState.CARDSTATE_ABSENT) {
            processHotPlugState(index.intValue(), false);
        } else if (oldCardState != CardState.CARDSTATE_ABSENT || this.mCardStates[index.intValue()] == CardState.CARDSTATE_ABSENT) {
            processNotHotPlugState(index.intValue());
        } else {
            processHotPlugState(index.intValue(), true);
        }
        this.mLastRadioStates[index.intValue()] = radioState;
    }

    public void onHotPlugIccStatusChanged(Integer index) {
        if (!this.mProccessHotPlugDone) {
            this.mIsQueryingCardTypes[index.intValue()] = true;
        }
    }

    public void onRestartRild() {
        if (!this.mProccessHotPlugDone) {
            for (int i = 0; i < this.mCis.length; i++) {
                this.mIsRestartRild[i] = true;
            }
        }
    }

    public void onHotplugIccIdChanged(String iccid, int slotId) {
        if (SIM_NUM != 1) {
            processMSimIccIdChange(iccid, slotId);
        }
    }

    public void onHotPlugQueryCardTypeDone(AsyncResult ar, Integer index) {
        int notReservedCard;
        String str;
        StringBuilder stringBuilder;
        if (!this.mProccessHotPlugDone) {
            this.mIsQueryingCardTypes[index.intValue()] = false;
        }
        if (HwVSimUtils.isVSimEnabled() && HwVSimUtils.isPlatformTwoModems()) {
            notReservedCard = index.intValue() == 0 ? 1 : 0;
            this.mIsQueryingCardTypes[notReservedCard] = false;
            this.mHotPlugCardTypes[notReservedCard] = 0;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onHotPlugQueryCardTypeDone SUB[");
            stringBuilder.append(notReservedCard);
            stringBuilder.append("] : change to no-sim.");
            Rlog.d(str, stringBuilder.toString());
        }
        if (ar != null && ar.result != null) {
            notReservedCard = this.mHotPlugCardTypes[index.intValue()];
            this.mHotPlugCardTypes[index.intValue()] = ((int[]) ar.result)[0] & 15;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onHotPlugQueryCardTypeDone SUB[");
            stringBuilder.append(index);
            stringBuilder.append("] :");
            stringBuilder.append(this.mHotPlugCardTypes[index.intValue()]);
            Rlog.d(str, stringBuilder.toString());
            if (IS_FULL_NETWORK_SUPPORTED) {
                onHotPlugQueryCardTypeDoneFullNetwork(notReservedCard, index);
                return;
            }
            if (IS_CHINA_TELECOM && index.intValue() == HwFullNetworkManager.getInstance().getUserSwitchDualCardSlots()) {
                onHotPlugQueryCardTypeDoneCDMA(notReservedCard, index);
            }
            if (SIM_NUM != 1) {
                processNotifyPromptHotPlug(false);
            }
        }
    }

    private void onHotPlugQueryCardTypeDoneCDMA(int oldHotPlugCardType, Integer index) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onHotPlugQueryCardTypeDoneCDMA SUB[");
        stringBuilder.append(index);
        stringBuilder.append("] : oldHotPlugCardType = ");
        stringBuilder.append(oldHotPlugCardType);
        stringBuilder.append(", mHotPlugCardTypes = ");
        stringBuilder.append(this.mHotPlugCardTypes[index.intValue()]);
        Rlog.d(str, stringBuilder.toString());
        if (oldHotPlugCardType == 0 && this.mHotPlugCardTypes[index.intValue()] == 1) {
            processHotPlugState(index.intValue(), true);
        } else if (oldHotPlugCardType == 1 && this.mHotPlugCardTypes[index.intValue()] == 0) {
            processHotPlugState(index.intValue(), false);
        } else if (oldHotPlugCardType == 0 && this.mHotPlugCardTypes[index.intValue()] == 0) {
            processNotHotPlugState(index.intValue());
        }
    }

    private void onHotPlugQueryCardTypeDoneFullNetwork(int oldHotPlugCardType, Integer index) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onHotPlugQueryCardTypeDoneFullNetwork SUB[");
        stringBuilder.append(index);
        stringBuilder.append("] : oldHotPlugCardType = ");
        stringBuilder.append(oldHotPlugCardType);
        stringBuilder.append(", mHotPlugCardTypes = ");
        stringBuilder.append(this.mHotPlugCardTypes[index.intValue()]);
        Rlog.d(str, stringBuilder.toString());
        if (this.mIsRestartRild[index.intValue()]) {
            this.mIsRestartRild[index.intValue()] = false;
        }
        str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mIsRestartRild[0] = ");
        stringBuilder2.append(this.mIsRestartRild[0]);
        stringBuilder2.append("; mIsRestartRild[");
        stringBuilder2.append(1);
        stringBuilder2.append("] = ");
        stringBuilder2.append(this.mIsRestartRild[1]);
        Rlog.d(str, stringBuilder2.toString());
        if (!this.mProccessHotPlugDone && !this.mIsRestartRild[0] && !this.mIsRestartRild[1]) {
            processNotifyPromptHotPlug(false);
        }
    }

    public void updateHotPlugMainSlotIccId(String iccid) {
        System.putString(this.mContext.getContentResolver(), "hotplug_mainslot_iccid", iccid);
    }

    private void processGetHotPlugState(AsyncResult ar, Integer index) {
        int what;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processGetHotPlugState : begin mHotPlugStates[");
        stringBuilder.append(index);
        stringBuilder.append("] = ");
        stringBuilder.append(this.mHotPlugStates[index.intValue()]);
        Rlog.d(str, stringBuilder.toString());
        if (index.intValue() == 0) {
            what = 2;
        } else {
            what = 1;
        }
        if (hasMessages(what)) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("processGetHotPlugState : has timeout message ");
            stringBuilder2.append(what);
            stringBuilder2.append(", remove it");
            Rlog.d(str2, stringBuilder2.toString());
            removeMessages(what);
        }
        if (this.mHotPlugStates[index.intValue()] == 4) {
            String str3;
            StringBuilder stringBuilder3;
            if (ar == null || ar.result == null) {
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("processGetHotPlugState : ar = ");
                stringBuilder3.append(ar);
                Rlog.d(str3, stringBuilder3.toString());
                this.mHotPlugStates[index.intValue()] = 0;
            } else if (((int[]) ar.result)[0] == 1) {
                this.mHotPlugStates[index.intValue()] = 3;
            } else {
                this.mHotPlugStates[index.intValue()] = 0;
            }
            str3 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("processGetHotPlugState : end mHotPlugStates[");
            stringBuilder3.append(index);
            stringBuilder3.append("] = ");
            stringBuilder3.append(this.mHotPlugStates[index.intValue()]);
            Rlog.d(str3, stringBuilder3.toString());
            processNotifyPromptHotPlug(false);
        }
    }

    private void processNotHotPlugState(int index) {
        if (SIM_NUM != 1) {
            processNotMSimHotPlugState(index);
        }
    }

    private void processNotMSimHotPlugState(int index) {
        if (!this.mProccessHotPlugDone && this.mHotPlugStates[index] == 3 && !this.mIsRestartRild[0] && !this.mIsRestartRild[1]) {
            this.mHotPlugStates[index] = 0;
            processNotifyPromptHotPlug(false);
        }
    }

    private void processHotPlugState(int index, boolean isAdded) {
        if (SIM_NUM != 1) {
            processMSimHotPlugState(index, isAdded);
        }
    }

    private void processMSimHotPlugState(int index, boolean isAdded) {
        int what;
        String str;
        StringBuilder stringBuilder;
        int otherIndex = 1;
        if (isAdded) {
            this.mHotPlugStates[index] = 1;
            if (this.mHotPlugCardTypes[index] == 0) {
                this.mIsQueryingCardTypes[index] = true;
            }
        } else {
            this.mHotPlugStates[index] = 2;
            this.mHotPlugCardTypes[index] = 0;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("processMSimHotPlugState : mHotPlugStates[0] = ");
        stringBuilder2.append(this.mHotPlugStates[0]);
        stringBuilder2.append(", mHotPlugStates[1] = ");
        stringBuilder2.append(this.mHotPlugStates[1]);
        Rlog.d(str2, stringBuilder2.toString());
        if (this.mProccessHotPlugDone) {
            Rlog.d(TAG, "processMSimHotPlug --------> begin");
            this.mProccessHotPlugDone = false;
        }
        if (index == 0) {
            what = 1;
        } else {
            what = 2;
        }
        if (hasMessages(what)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("processMSimHotPlugState : has timeout message ");
            stringBuilder.append(what);
            stringBuilder.append(", remove it");
            Rlog.d(str, stringBuilder.toString());
            removeMessages(what);
        }
        if (index != 0) {
            otherIndex = 0;
        }
        if (this.mHotPlugStates[otherIndex] == 0) {
            this.mHotPlugStates[otherIndex] = 4;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("processMSimHotPlugState : getSimHotPlugState mHotPlugStates[");
            stringBuilder.append(otherIndex);
            stringBuilder.append("] : ");
            stringBuilder.append(this.mHotPlugStates[otherIndex]);
            Rlog.d(str, stringBuilder.toString());
            this.mCis[otherIndex].getSimHotPlugState(obtainMessage(0, Integer.valueOf(otherIndex)));
            sendMessageDelayed(obtainMessage(what), 15000);
        }
    }

    public void processNotifyPromptHotPlug(boolean isTimeout) {
        if (this.mProccessHotPlugDone) {
            Rlog.d(TAG, "processNotifyPromptHotPlug : Hotplug process is complete, don't process notify.");
            return;
        }
        if (HwFullNetworkConfig.IS_HISI_DSDX) {
            if (HwFullNetworkManager.getInstance().getWaitingSwitchBalongSlot()) {
                Rlog.d(TAG, "processNotifyPromptHotPlug : Need waitingSwitchBalongSlot");
                return;
            } else if ("1".equals(SystemProperties.get("gsm.nvcfg.resetrild", "0"))) {
                Rlog.d(TAG, "processNotifyPromptHotPlug : Need wait nv restart rild");
                return;
            }
        }
        boolean needWaitNotify = this.mHotPlugStates[0] == 3 || this.mHotPlugStates[0] == 4 || this.mHotPlugStates[1] == 3 || this.mHotPlugStates[1] == 4 || this.mIsQueryingCardTypes[0] || this.mIsQueryingCardTypes[1];
        if (HwDsdsController.IS_DSDSPOWER_SUPPORT) {
            if (IS_FULL_NETWORK_SUPPORTED && (this.mIsRestartRild[0] || this.mIsRestartRild[1])) {
                Rlog.d(TAG, "processNotifyPromptHotPlug : Need restart rild");
                return;
            } else if (!needWaitNotify && ((this.mHotPlugStates[0] == 1 && this.mHotPlugCardTypes[0] != 0) || (this.mHotPlugStates[1] == 1 && this.mHotPlugCardTypes[1] != 0))) {
                if (HwFullNetworkConfig.IS_HISI_DSDX) {
                    UiccController tempUiccController = UiccController.getInstance();
                    if (tempUiccController == null || tempUiccController.getUiccCards() == null) {
                        Rlog.d(TAG, "haven't get all UiccCards done, please wait!");
                        return;
                    }
                    UiccCard[] uc = tempUiccController.getUiccCards();
                    for (int i = 0; i < uc.length; i++) {
                        if (uc[i] == null) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("UiccCard[");
                            stringBuilder.append(i);
                            stringBuilder.append("]is null");
                            Rlog.d(str, stringBuilder.toString());
                            return;
                        }
                    }
                }
                HwDsdsController.getInstance().setActiveModeForHotPlug();
            }
        }
        boolean isRemovedSUB1 = false;
        boolean isRemovedSUB2 = false;
        if (false) {
            isRemovedSUB1 = this.mHotPlugStates[0] == 2;
            isRemovedSUB2 = this.mHotPlugStates[1] == 2;
        } else {
            if (!needWaitNotify && this.mHotPlugStates[0] == 2) {
                this.mHotPlugStates[0] = 0;
            }
            if (!needWaitNotify && this.mHotPlugStates[1] == 2) {
                this.mHotPlugStates[1] = 0;
            }
        }
        boolean hasHotPluged = (this.mHotPlugStates[0] == 1 && this.mHotPlugCardTypes[0] != 0) || isRemovedSUB1 || ((this.mHotPlugStates[1] == 1 && this.mHotPlugCardTypes[1] != 0) || isRemovedSUB2);
        boolean needNotify = !needWaitNotify && hasHotPluged;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("processNotifyPromptHotPlug : mHotPlugStates[0] = ");
        stringBuilder2.append(this.mHotPlugStates[0]);
        stringBuilder2.append(", mHotPlugStates[1] = ");
        stringBuilder2.append(this.mHotPlugStates[1]);
        stringBuilder2.append(", mIsQueryingCardTypes[0] = ");
        stringBuilder2.append(this.mIsQueryingCardTypes[0]);
        stringBuilder2.append(", mIsQueryingCardTypes[1] = ");
        stringBuilder2.append(this.mIsQueryingCardTypes[1]);
        stringBuilder2.append(", needNotify = ");
        stringBuilder2.append(needNotify);
        stringBuilder2.append(", isTimeout = ");
        stringBuilder2.append(isTimeout);
        Rlog.d(str2, stringBuilder2.toString());
        if (needNotify || isTimeout) {
            this.mHotPlugStates[0] = 0;
            this.mHotPlugStates[1] = 0;
            if (HwDsdsController.IS_DSDSPOWER_SUPPORT && HwDsdsController.getInstance().isProcessSetActiveModeForHotPlug()) {
                Rlog.d(TAG, "processMSimHotPlug need wait ActiveMode done!");
                HwDsdsController.getInstance().setNeedNotify(true);
            } else {
                notifyMSimHotPlugPrompt();
            }
        }
        if (this.mHotPlugStates[0] == 0 && this.mHotPlugStates[1] == 0) {
            this.mProccessHotPlugDone = true;
            Rlog.d(TAG, "processMSimHotPlug --------> end");
        }
    }

    private void processMSimIccIdChange(String iccid, int slotId) {
        if (!TextUtils.isEmpty(iccid)) {
            int mainSlot = HwFullNetworkManager.getInstance().getUserSwitchDualCardSlots();
            int secSlot = mainSlot == 0 ? 1 : 0;
            String oldMainIccId = System.getString(this.mContext.getContentResolver(), "hotplug_mainslot_iccid");
            if (mainSlot == slotId && !iccid.equals(oldMainIccId)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("update main slot iccid change. mainSlot : ");
                stringBuilder.append(mainSlot);
                stringBuilder.append(", slotId : ");
                stringBuilder.append(slotId);
                Rlog.d(str, stringBuilder.toString());
                updateHotPlugMainSlotIccId(iccid);
                if (!TextUtils.isEmpty(oldMainIccId)) {
                    this.mIsNotifyIccIdChange = true;
                    notfiyHotPlugIccIdChange(mainSlot, secSlot);
                }
            }
        }
    }

    public void notifyMSimHotPlugPrompt() {
        int mainSlot = HwFullNetworkManager.getInstance().getUserSwitchDualCardSlots();
        int secSlot = mainSlot == 0 ? 1 : 0;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyMSimHotPlugPrompt : mainSlot = ");
        stringBuilder.append(mainSlot);
        stringBuilder.append(", secSlot = ");
        stringBuilder.append(secSlot);
        stringBuilder.append(", mHotPlugCardTypes[0] = ");
        stringBuilder.append(this.mHotPlugCardTypes[0]);
        stringBuilder.append(", mHotPlugCardTypes[1] = ");
        stringBuilder.append(this.mHotPlugCardTypes[1]);
        Rlog.d(str, stringBuilder.toString());
        if (HwVSimUtils.isVSimEnabled() || HwVSimUtils.isVSimCauseCardReload()) {
            Rlog.d(TAG, "vsim processHotPlug");
            HwVSimUtils.processHotPlug(this.mHotPlugCardTypes);
        } else if (IS_CHINA_TELECOM) {
            if (SystemProperties.getBoolean("persist.sys.dualcards", false)) {
                notifyMSimHotPlugPromptCDMA(mainSlot, secSlot);
            } else if (this.mHotPlugCardTypes[mainSlot] == 1) {
                broadcastForHwCardManager();
            }
        } else if (this.mHotPlugCardTypes[mainSlot] != 0 || this.mHotPlugCardTypes[secSlot] == 0 || HwFullNetworkConfig.IS_HISI_DSDX) {
            notfiyHotPlugIccIdChange(mainSlot, secSlot);
        } else {
            Rlog.d(TAG, "notifyMSimHotPlugPrompt : main card need switch.");
            showHotPlugDialog(33685804);
        }
    }

    private void broadcastForHwCardManager() {
        Rlog.d(TAG, "[broadcastForHwCardManager]");
        Intent intent = new Intent("com.huawei.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        intent.putExtra("popupDialog", "true");
        ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
    }

    private void notifyMSimHotPlugPromptCDMA(int mainSlot, int secSlot) {
        if (this.mHotPlugCardTypes[mainSlot] != 2 && this.mHotPlugCardTypes[mainSlot] != 3 && (this.mHotPlugCardTypes[secSlot] == 2 || this.mHotPlugCardTypes[secSlot] == 3)) {
            Rlog.d(TAG, "notifyMSimHotPlugPromptCDMA : cdma card need switch.");
            showHotPlugDialogCDMA(34013208);
        } else if (this.mHotPlugCardTypes[mainSlot] == 1 && this.mHotPlugCardTypes[secSlot] == 0) {
            Rlog.d(TAG, "notifyMSimHotPlugPromptCDMA : gsm card need switch.");
            showHotPlugDialogCDMA(34013209);
        } else {
            notfiyHotPlugIccIdChange(mainSlot, secSlot);
        }
    }

    private void notfiyHotPlugIccIdChange(int mainSlot, int secSlot) {
        if (!this.mIsNotifyIccIdChange) {
            return;
        }
        if (this.mHotPlugStates[0] == 0 && this.mHotPlugStates[1] == 0) {
            this.mIsNotifyIccIdChange = false;
            if (HwVSimUtils.isVSimEnabled() || HwVSimUtils.isVSimCauseCardReload()) {
                Rlog.d(TAG, "vsim is on, skip notify");
                return;
            } else if (IS_CHINA_TELECOM) {
                notifyHotPlugIccIdChangeCDMA(mainSlot, secSlot);
                return;
            } else {
                if (!(this.mHotPlugCardTypes[mainSlot] == 0 || this.mHotPlugCardTypes[secSlot] == 0 || HwFullNetworkConfig.IS_HISI_DSDX)) {
                    showHotPlugDialog(33685805);
                }
                return;
            }
        }
        Rlog.d(TAG, "The hotplug process is not complete, wait to noify iccid change");
    }

    private void notifyHotPlugIccIdChangeCDMA(int mainSlot, int secSlot) {
        if (this.mHotPlugCardTypes[mainSlot] != 2 && this.mHotPlugCardTypes[mainSlot] != 3) {
            return;
        }
        if ((this.mHotPlugCardTypes[secSlot] == 2 || this.mHotPlugCardTypes[secSlot] == 3) && !HwFullNetworkConfig.IS_HISI_DSDX) {
            showHotPlugDialog(33685805);
        }
    }

    private void showHotPlugDialog(int stringId) {
        try {
            if (!isAirplaneMode()) {
                if (isFactroyMode) {
                    Rlog.d(TAG, "showHotPlugDialog:don't show dialog in factory mode");
                    return;
                }
                if (this.mDialog != null) {
                    this.mDialog.dismiss();
                    this.mDialog = null;
                }
                Resources r = Resources.getSystem();
                String title = r.getString(33685797);
                String message = r.getString(stringId);
                if (this.mCustHotplugController != null) {
                    message = this.mCustHotplugController.change4GString(message);
                }
                this.mDialog = new Builder(this.mContext, 33947691).setTitle(title).setMessage(message).setPositiveButton(33685803, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent("android.intent.action.MAIN");
                        if (HwHotplugController.IS_CHINA_TELECOM) {
                            intent.setClassName(HwHotplugController.DUALCARD_PACKAGE, HwHotplugController.DUALCARD_CLASS_TAB);
                        } else {
                            intent.setClassName(HwHotplugController.DUALCARD_PACKAGE, HwHotplugController.DUALCARD_CLASS);
                        }
                        intent.addFlags(805306368);
                        Rlog.d(HwHotplugController.TAG, "start HWCardManagerActivity.");
                        HwHotplugController.this.mContext.startActivity(intent);
                    }
                }).setNegativeButton(17039360, null).setCancelable(false).create();
                this.mDialog.getWindow().setType(HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_MODE_DONE);
                this.mDialog.show();
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("showHotPlugDialog exception: ");
            stringBuilder.append(e);
            Rlog.e(str, stringBuilder.toString());
        }
    }

    private void showHotPlugDialogCDMA(int layoutId) {
        try {
            if (!isAirplaneMode()) {
                if (isFactroyMode) {
                    Rlog.d(TAG, "showHotPlugDialogCDMA:don't show dialog in factory mode");
                    return;
                }
                if (this.mDialog != null) {
                    this.mDialog.dismiss();
                    this.mDialog = null;
                }
                Resources r = Resources.getSystem();
                LayoutInflater inflater = (LayoutInflater) new ContextThemeWrapper(this.mContext, r.getIdentifier("androidhwext:style/Theme.Emui", null, null)).getSystemService("layout_inflater");
                String title = r.getString(33685797);
                this.mDialog = new Builder(this.mContext, 33947691).setTitle(title).setView(inflater.inflate(layoutId, null)).setPositiveButton(33685803, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent("android.intent.action.MAIN");
                        intent.setClassName(HwHotplugController.DUALCARD_PACKAGE, HwHotplugController.DUALCARD_CLASS_TAB);
                        intent.addFlags(805306368);
                        Rlog.d(HwHotplugController.TAG, "start HWCardManagerTabActivity.");
                        HwHotplugController.this.mContext.startActivity(intent);
                    }
                }).setNegativeButton(17039360, null).setCancelable(false).create();
                this.mDialog.getWindow().setType(HwFullNetworkConstants.EVENT_RESET_OOS_FLAG);
                this.mDialog.show();
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("showHotPlugDialogCDMA exception: ");
            stringBuilder.append(e);
            Rlog.e(str, stringBuilder.toString());
        }
    }

    public Integer getCiIndex(Message msg) {
        Integer index = Integer.valueOf(null);
        if (msg == null) {
            return index;
        }
        if (msg.obj != null && (msg.obj instanceof Integer)) {
            return msg.obj;
        }
        if (msg.obj == null || !(msg.obj instanceof AsyncResult)) {
            return index;
        }
        AsyncResult ar = msg.obj;
        if (ar.userObj == null || !(ar.userObj instanceof Integer)) {
            return index;
        }
        return ar.userObj;
    }

    public void handleMessage(Message msg) {
        Integer index = getCiIndex(msg);
        if (index.intValue() < 0 || index.intValue() >= this.mCis.length) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid index : ");
            stringBuilder.append(index);
            stringBuilder.append(" received with event ");
            stringBuilder.append(msg.what);
            Rlog.e(str, stringBuilder.toString());
            return;
        }
        switch (msg.what) {
            case 0:
                processGetHotPlugState((AsyncResult) msg.obj, index);
                break;
            case 1:
            case 2:
                processNotifyPromptHotPlug(true);
                break;
            default:
                Rlog.e(TAG, "xxxxx");
                break;
        }
    }

    private boolean isAirplaneMode() {
        return System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
    }
}
