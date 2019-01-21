package com.android.internal.telephony;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimUtils;
import java.util.Arrays;

public class HwDsdsController extends Handler {
    private static final int DUAL_CARD_MODEM_MODE = 1;
    private static final int EVENT_GET_BALONG_SIM_DONE = 103;
    private static final int EVENT_HOTPLUG_CLOSE_MODEM_DONE = 301;
    private static final int EVENT_HOTPLUG_CLOSE_MODEM_TIMEOUT = 302;
    private static final int EVENT_HWDSDS_GET_ICC_STATUS_DONE = 201;
    private static final int EVENT_HWDSDS_RADIO_STATE_CHANGED = 202;
    private static final int EVENT_HWDSDS_SET_ACTIVEMODE_DONE = 203;
    private static final int EVENT_HWDSDS_SET_ACTIVEMODE_TIMEOUT = 204;
    private static final int EVENT_QUERY_CARD_TYPE_DONE = 102;
    private static final int EVENT_SWITCH_DUAL_CARD_SLOT = 101;
    private static final int EVENT_SWITCH_SIM_SLOT_CFG_DONE = 105;
    private static final int HOTPLUG_CLOSE_WAITING_TIME = 10000;
    private static final int HWDSDS_SET_ACTIVEMODE_WAITING_TIME = 5000;
    private static final int INVALID = -1;
    public static final boolean IS_DSDSPOWER_SUPPORT;
    private static final int SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final int SINGLE_CARD_MODEM_MODE = 0;
    private static final int SLOT1 = 1;
    private static final int SLOT2 = 2;
    private static final String TAG = "HwDsdsController";
    private static HwDsdsController mInstance;
    private static final Object mLock = new Object();
    private boolean isMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
    private int mActiveModemMode = SystemProperties.getInt("persist.radio.activemodem", -1);
    private boolean mAutoSetPowerupModemDone = false;
    private int mBalongSimSlot = 0;
    private CommandsInterface[] mCis;
    Context mContext;
    private boolean mDsdsCfgDone = false;
    private RegistrantList mIccDSDAutoModeSetRegistrants = new RegistrantList();
    private boolean mNeedNotify = false;
    private boolean mNeedWatingSlotSwitchDone = false;
    private int mNumOfCloseModemSuccess = 0;
    private boolean mProcessSetActiveModeForHotPlug = false;
    private boolean mSubinfoAutoUpdateDone = false;
    private int[] mSwitchTypes = new int[2];
    UiccCard[] mUiccCards = new UiccCard[SIM_NUM];
    private boolean mWatingSetActiveModeDone = false;

    static {
        boolean z = false;
        if (SystemProperties.getBoolean("ro.config.hw_dsdspowerup", false) && !SystemProperties.getBoolean("ro.config.fast_switch_simslot", false)) {
            z = true;
        }
        IS_DSDSPOWER_SUPPORT = z;
    }

    public boolean isProcessSetActiveModeForHotPlug() {
        return this.mProcessSetActiveModeForHotPlug;
    }

    public void setNeedNotify(boolean needNotify) {
        this.mNeedNotify = needNotify;
    }

    public void setNeedWatingSlotSwitchDone(boolean needWatingSlotSwitchDone) {
        this.mNeedWatingSlotSwitchDone = needWatingSlotSwitchDone;
    }

    public void setSubinfoAutoUpdateDone(boolean subinfoAutoUpdateDone) {
        this.mSubinfoAutoUpdateDone = subinfoAutoUpdateDone;
    }

    private HwDsdsController(Context c, CommandsInterface[] ci) {
        logd("constructor init");
        this.mContext = c;
        this.mCis = ci;
        if (!IS_DSDSPOWER_SUPPORT || !this.isMultiSimEnabled) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mDSDSPowerup= ");
            stringBuilder.append(IS_DSDSPOWER_SUPPORT);
            stringBuilder.append(" ; isMultiSimEnabled = ");
            stringBuilder.append(this.isMultiSimEnabled);
            logd(stringBuilder.toString());
        }
    }

    public static HwDsdsController make(Context context, CommandsInterface[] ci) {
        HwDsdsController hwDsdsController;
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new HwDsdsController(context, ci);
                hwDsdsController = mInstance;
            } else {
                throw new RuntimeException("HwDsdsController.make() should only be called once");
            }
        }
        return hwDsdsController;
    }

    public static HwDsdsController getInstance() {
        HwDsdsController hwDsdsController;
        synchronized (mLock) {
            if (mInstance != null) {
                hwDsdsController = mInstance;
            } else {
                throw new RuntimeException("HwDsdsController.getInstance can't be called before make()");
            }
        }
        return hwDsdsController;
    }

    /* JADX WARNING: Missing block: B:29:0x0133, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        synchronized (mLock) {
            Integer index = getCiIndex(msg);
            if (index.intValue() >= 0) {
                if (index.intValue() < this.mCis.length) {
                    AsyncResult ar = msg.obj;
                    StringBuilder stringBuilder;
                    switch (msg.what) {
                        case EVENT_QUERY_CARD_TYPE_DONE /*102*/:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Received EVENT_QUERY_CARD_TYPE_DONE on index ");
                            stringBuilder.append(index);
                            logd(stringBuilder.toString());
                            onQueryCardTypeDone(ar, index);
                            break;
                        case EVENT_GET_BALONG_SIM_DONE /*103*/:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Received EVENT_GET_BALONG_SIM_DONE on index ");
                            stringBuilder.append(index);
                            logd(stringBuilder.toString());
                            onGetBalongSimDone(ar, index);
                            break;
                        case 201:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Received EVENT_HWDSDS_GET_ICC_STATUS_DONE on index ");
                            stringBuilder.append(index);
                            logd(stringBuilder.toString());
                            break;
                        case 202:
                            if (!(this.mAutoSetPowerupModemDone || this.mCis[index.intValue()].getRadioState() == RadioState.RADIO_UNAVAILABLE)) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Received EVENT_HWDSDS_RADIO_STATE_CHANGED on index ");
                                stringBuilder.append(index);
                                logd(stringBuilder.toString());
                                this.mCis[index.intValue()].queryCardType(obtainMessage(EVENT_QUERY_CARD_TYPE_DONE, index));
                                this.mCis[index.intValue()].getBalongSim(obtainMessage(EVENT_GET_BALONG_SIM_DONE, index));
                                this.mCis[index.intValue()].getIccCardStatus(obtainMessage(201, index));
                                break;
                            }
                        case 203:
                        case 204:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Received ");
                            stringBuilder.append(msg.what == 203 ? "EVENT_HWDSDS_SET_ACTIVEMODE_DONE" : "EVENT_HWDSDS_SET_ACTIVEMODE_TIMEOUT");
                            stringBuilder.append(" on index ");
                            stringBuilder.append(index);
                            logd(stringBuilder.toString());
                            handleActiveModeDoneOrTimeout(msg);
                            break;
                        case 301:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Received EVENT_HOTPLUG_CLOSE_MODEM_DONE on index ");
                            stringBuilder.append(index);
                            logd(stringBuilder.toString());
                            this.mNumOfCloseModemSuccess++;
                            if (this.mNumOfCloseModemSuccess == SIM_NUM) {
                                processCloseModemDone(302);
                                break;
                            }
                            break;
                        case 302:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Received EVENT_HOTPLUG_CLOSE_MODEM_TIMEOUT on index ");
                            stringBuilder.append(index);
                            logd(stringBuilder.toString());
                            processCloseModemDone(301);
                            break;
                        default:
                            break;
                    }
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Invalid index : ");
            stringBuilder2.append(index);
            stringBuilder2.append(" received with event ");
            stringBuilder2.append(msg.what);
            loge(stringBuilder2.toString());
        }
    }

    private void handleActiveModeDoneOrTimeout(Message msg) {
        AsyncResult ar_setmode = msg.obj;
        if (ar_setmode == null || ar_setmode.exception != null) {
            logd("Received EVENT_HWDSDS_SET_ACTIVEMODE_DONE  fail ");
            this.mActiveModemMode = -1;
        }
        if (this.mWatingSetActiveModeDone) {
            uiccHwdsdscancelTimeOut();
            logd("EVENT_HWDSDS_SET_ACTIVEMODE_DONE, need to power off and power on");
            setModemPowerDownForHotPlug();
            uiccHwdsdsGetIccStatusSend();
            this.mIccDSDAutoModeSetRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
            setAutoSetPowerupModemDone(true);
            setDsdsCfgDone(true);
            this.mWatingSetActiveModeDone = false;
        }
        if (this.mProcessSetActiveModeForHotPlug) {
            uiccHwdsdscancelTimeOut();
            setModemPowerDownForHotPlug();
        }
    }

    private Integer getCiIndex(Message msg) {
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

    public boolean isBalongSimSynced() {
        int currSlot = getUserSwitchDualCardSlots();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("currSlot  = ");
        stringBuilder.append(currSlot);
        stringBuilder.append(", mBalongSimSlot = ");
        stringBuilder.append(this.mBalongSimSlot);
        logd(stringBuilder.toString());
        return currSlot == this.mBalongSimSlot;
    }

    private int judgeBalongSimSlotFromResult(int[] slots) {
        if (slots[0] == 0 && slots[1] == 1 && slots[2] == 2) {
            return 0;
        }
        if (slots[0] == 1 && slots[1] == 0 && slots[2] == 2) {
            return 1;
        }
        if (slots[0] == 2 && slots[1] == 1 && slots[2] == 0) {
            return 0;
        }
        if (slots[0] == 2 && slots[1] == 0 && slots[2] == 1) {
            return 1;
        }
        return -1;
    }

    public void onGetBalongSimDone(AsyncResult ar, Integer index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetBalongSimDone. index = ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
        if (!(ar == null || ar.result == null)) {
            if (((int[]) ar.result).length == 2) {
                if (((int[]) ar.result)[0] + ((int[]) ar.result)[1] > 1) {
                    this.mBalongSimSlot = ((int[]) ar.result)[0] - 1;
                } else {
                    this.mBalongSimSlot = ((int[]) ar.result)[0];
                }
            } else if (((int[]) ar.result).length == 3) {
                int[] slots = ar.result;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("slot result = ");
                stringBuilder2.append(Arrays.toString(slots));
                logd(stringBuilder2.toString());
                this.mBalongSimSlot = judgeBalongSimSlotFromResult(slots);
            } else if (((int[]) ar.result).length == 1) {
                this.mBalongSimSlot = ((int[]) ar.result)[0] - 1;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("onGetBalongSimDone. GetBalongSim Failed ! ar.result.length:");
                stringBuilder.append(((int[]) ar.result).length);
                logd(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("mBalongSimSlot = ");
        stringBuilder.append(this.mBalongSimSlot);
        logd(stringBuilder.toString());
    }

    public void setBalongSimSlot(int slot) {
        this.mBalongSimSlot = slot;
    }

    public int getBalongSimSlot() {
        return this.mBalongSimSlot;
    }

    private void onQueryCardTypeDone(AsyncResult ar, Integer index) {
        logd("onQueryCardTypeDone");
        if (!(ar == null || ar.result == null)) {
            this.mSwitchTypes[index.intValue()] = ((int[]) ar.result)[0] & 15;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mSwitchTypes[");
        stringBuilder.append(index);
        stringBuilder.append("] = ");
        stringBuilder.append(this.mSwitchTypes[index.intValue()]);
        logd(stringBuilder.toString());
    }

    public void registerDSDSAutoModemSetChanged(Handler h, int what, Object obj) {
        synchronized (mLock) {
            this.mIccDSDAutoModeSetRegistrants.add(new Registrant(h, what, obj));
        }
    }

    public void unregisterDSDSAutoModemSetChanged(Handler h) {
        synchronized (mLock) {
            this.mIccDSDAutoModeSetRegistrants.remove(h);
        }
    }

    public int getUserSwitchDualCardSlots() {
        try {
            return System.getInt(this.mContext.getContentResolver(), "switch_dual_card_slots");
        } catch (SettingNotFoundException e) {
            loge("Settings Exception Reading Dual Sim Switch Dual Card Slots Values");
            return 0;
        }
    }

    private static void logd(String message) {
        Rlog.d(TAG, message);
    }

    private static void loge(String message) {
        Rlog.e(TAG, message);
    }

    public void setActiveModeForHotPlug() {
        if (HwVSimUtils.isVSimEnabled() && HwVSimUtils.isPlatformRealTripple()) {
            this.mProcessSetActiveModeForHotPlug = false;
            logd("vsim is enabled, just return!");
        } else if (this.mProcessSetActiveModeForHotPlug) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mProcessSetActiveModeForHotPlug = ");
            stringBuilder.append(this.mProcessSetActiveModeForHotPlug);
            stringBuilder.append(" , just return");
            logd(stringBuilder.toString());
        } else {
            this.mProcessSetActiveModeForHotPlug = true;
            this.mUiccCards = UiccController.getInstance().getUiccCards();
            uiccHwdsdsSetActiveModemMode();
        }
    }

    private void setModemPowerDownForHotPlug() {
        for (int i = 0; i < SIM_NUM; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setModemPowerDownForHotPlug PhoneFactory close modem i = ");
            stringBuilder.append(i);
            logd(stringBuilder.toString());
            PhoneFactory.getPhone(i).setRadioPower(false, obtainMessage(301, Integer.valueOf(i)));
        }
        removeMessages(302);
        sendMessageDelayed(obtainMessage(302), HwVSimConstants.VSIM_DISABLE_RETRY_TIMEOUT);
    }

    private void processCloseModemDone(int what) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processCloseModemDone what = ");
        stringBuilder.append(what);
        logd(stringBuilder.toString());
        if (hasMessages(what)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("remove ");
            stringBuilder.append(what);
            stringBuilder.append(" message!");
            logd(stringBuilder.toString());
            removeMessages(what);
        }
        this.mProcessSetActiveModeForHotPlug = false;
        this.mNumOfCloseModemSuccess = 0;
        setRadioPowerForHotPlug();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("processCloseModemDone mNeedNotify = ");
        stringBuilder2.append(this.mNeedNotify);
        logd(stringBuilder2.toString());
        if (this.mNeedNotify && HwHotplugController.IS_HOTSWAP_SUPPORT) {
            HwHotplugController.getInstance().notifyMSimHotPlugPrompt();
            this.mNeedNotify = false;
            logd("notifyMSimHotPlugPrompt done.");
        }
    }

    private void setRadioPowerForHotPlug() {
        boolean isCard1Present = false;
        boolean isCard2Present = false;
        if (System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) <= 0) {
            boolean z;
            for (UiccCard uc : this.mUiccCards) {
                if (this.mUiccCards.length < 2 || uc == null) {
                    logd("setRadioPowerForHotPlug fail");
                    return;
                }
            }
            boolean isCard1Inserted = false;
            boolean isCard2Inserted = false;
            if (this.mUiccCards[0] != null) {
                isCard1Present = this.mUiccCards[0].getCardState() == CardState.CARDSTATE_PRESENT;
                isCard1Inserted = isCard1Present;
                if (this.mSubinfoAutoUpdateDone) {
                    logd("setRadioPowerForHotPlug mSubinfoAutoUpdateDone, need to judge sub1 state");
                    z = isCard1Present && 1 == SubscriptionController.getInstance().getSubState(0);
                    isCard1Present = z;
                }
            } else {
                logd("setRadioPowerForHotPlug mUiccCards[0] == null");
            }
            if (this.mUiccCards[1] != null) {
                isCard2Present = this.mUiccCards[1].getCardState() == CardState.CARDSTATE_PRESENT;
                isCard2Inserted = isCard2Present;
                if (this.mSubinfoAutoUpdateDone) {
                    logd("setRadioPowerForHotPlug mSubinfoAutoUpdateDone, need to judge sub2 state");
                    z = isCard2Present && 1 == SubscriptionController.getInstance().getSubState(1);
                    isCard2Present = z;
                }
            } else {
                logd("setRadioPowerForHotPlug mUiccCards[1] == null");
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setRadioPowerForHotPlug isCard1Present=");
            stringBuilder.append(isCard1Present);
            stringBuilder.append(" ;isCard2Present=");
            stringBuilder.append(isCard2Present);
            logd(stringBuilder.toString());
            if (isCard1Present && !isCard2Present) {
                PhoneFactory.getPhone(0).setRadioPower(true);
                logd("setRadioPowerForHotPlug isCard1Present singel mode");
            } else if (isCard2Present && !isCard1Present) {
                PhoneFactory.getPhone(1).setRadioPower(true);
                logd("setRadioPowerForHotPlug isCard2Present singel mode");
            } else if (isCard2Present && isCard1Present) {
                PhoneFactory.getPhone(0).setRadioPower(true);
                PhoneFactory.getPhone(1).setRadioPower(true);
                logd("setRadioPowerForHotPlug dual card  mode");
            } else if (!(isCard2Present || isCard1Present)) {
                int slotId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                if (!this.mSubinfoAutoUpdateDone) {
                    PhoneFactory.getPhone(slotId).setRadioPower(true);
                    logd("setRadioPowerForHotPlug no card mode");
                } else if (isCard1Inserted || isCard2Inserted) {
                    logd("at least one card is inserted!");
                } else {
                    PhoneFactory.getPhone(slotId).setRadioPower(true);
                    logd("no cards in slots!");
                }
            }
            return;
        }
        logd("setRadioPowerForHotPlug in airplane mode");
    }

    public void custHwdsdsSetActiveModeIfNeeded(UiccCard[] uc) {
        if (this.mDsdsCfgDone || !this.isMultiSimEnabled) {
            logd(" don't need SetActiveMode");
            return;
        }
        logd("custHwdsdsSetActiveModeIfNeeded enter");
        if (uc == null || uc.length < 2) {
            logd("custSetActiveModeIfNeeded This Feature is not allowed here");
            return;
        }
        boolean mGetAllUiccCardsDone = true;
        for (int i = 0; i < uc.length; i++) {
            this.mUiccCards[i] = uc[i];
            if (uc[i] == null) {
                mGetAllUiccCardsDone = false;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mGetAllUiccCardsDone =");
        stringBuilder.append(mGetAllUiccCardsDone);
        logd(stringBuilder.toString());
        if (mGetAllUiccCardsDone && !this.mNeedWatingSlotSwitchDone && !this.mWatingSetActiveModeDone && (isBalongSimSynced() || (HwVSimUtils.isVSimEnabled() && HwVSimUtils.isPlatformRealTripple()))) {
            logd("custHwdsdsSetActiveModeIfNeeded!");
            this.mWatingSetActiveModeDone = true;
            uiccHwdsdsSetActiveModemMode();
        }
    }

    private void uiccHwdsdsSetActiveModemMode() {
        boolean isCard1Present = false;
        boolean isCard2Present = false;
        for (UiccCard uc : this.mUiccCards) {
            if (this.mUiccCards.length < 2 || uc == null) {
                logd("uiccsetActiveModemMode fail");
                if (this.mProcessSetActiveModeForHotPlug) {
                    this.mProcessSetActiveModeForHotPlug = false;
                }
                return;
            }
        }
        logd("setSubinfoAutoUpdateDone false");
        setSubinfoAutoUpdateDone(false);
        if (this.mUiccCards[0] != null) {
            isCard1Present = this.mUiccCards[0].getCardState() == CardState.CARDSTATE_PRESENT;
        } else {
            logd("uiccsetActiveModemMode mUiccCards[0] == null");
        }
        if (this.mUiccCards[1] != null) {
            isCard2Present = this.mUiccCards[1].getCardState() == CardState.CARDSTATE_PRESENT;
        } else {
            logd("uiccsetActiveModemMode mUiccCards[1] == null");
        }
        Message msg = obtainMessage(203);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("uiccsetActiveModemMode isCard1Present=");
        stringBuilder.append(isCard1Present);
        stringBuilder.append("isCard2Present=");
        stringBuilder.append(isCard2Present);
        logd(stringBuilder.toString());
        int i = -1;
        if (isCard1Present && !isCard2Present) {
            i = 0;
            logd("uiccsetActiveModemMode isCard1Present singel mode");
        } else if (isCard2Present && !isCard1Present) {
            i = 0;
            logd("uiccsetActiveModemMode isCard2Present singel mode");
        } else if (isCard2Present && isCard1Present) {
            i = 1;
            logd("uiccsetActiveModemMode dual card  mode");
        } else if (!(isCard2Present || isCard1Present)) {
            i = 1;
            logd("uiccsetActiveModemMode dual card  mode");
        }
        if (HwVSimUtils.isVSimEnabled() && HwVSimUtils.isPlatformRealTripple()) {
            i = 1;
        }
        StringBuilder stringBuilder2;
        if (this.mActiveModemMode != i) {
            this.mActiveModemMode = i;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("uiccsetActiveModemMode need set ActiveModemMode, mActiveModemMode = ");
            stringBuilder2.append(this.mActiveModemMode);
            logd(stringBuilder2.toString());
            this.mCis[0].setActiveModemMode(this.mActiveModemMode, msg);
            uiccHwdsdsStartTimeOut();
        } else if (this.mProcessSetActiveModeForHotPlug) {
            if (i == 0) {
                if (!isCard1Present) {
                    PhoneFactory.getPhone(0).setRadioPower(false, null);
                    logd("uiccsetActiveModemMode close card 1");
                } else if (!isCard2Present) {
                    PhoneFactory.getPhone(1).setRadioPower(false, null);
                    logd("uiccsetActiveModemMode close card 2");
                }
            }
            this.mProcessSetActiveModeForHotPlug = false;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mActiveModemMode = ");
            stringBuilder2.append(this.mActiveModemMode);
            stringBuilder2.append(";setActiveModemMode = ");
            stringBuilder2.append(i);
            stringBuilder2.append("; same active mode do nothing!!");
            logd(stringBuilder2.toString());
        } else {
            if (System.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) <= 0) {
                if (i == 0) {
                    if (isCard1Present) {
                        PhoneFactory.getPhone(0).setRadioPower(true, null);
                        PhoneFactory.getPhone(1).setRadioPower(false, null);
                        logd("uiccsetActiveModemMode, SINGLE_CARD_MODEM_MODE set card 1 power on");
                    } else if (isCard2Present) {
                        PhoneFactory.getPhone(1).setRadioPower(true, null);
                        PhoneFactory.getPhone(0).setRadioPower(false, null);
                        logd("uiccsetActiveModemMode, SINGLE_CARD_MODEM_MODE set card 2 power on");
                    }
                } else if (1 == i) {
                    if (isCard2Present && isCard1Present) {
                        PhoneFactory.getPhone(0).setRadioPower(true, null);
                        PhoneFactory.getPhone(1).setRadioPower(true, null);
                        logd("uiccsetActiveModemMode, DUAL_CARD_MODEM_MODE set dual card power on");
                    } else {
                        int mainSlot = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                        int anotherSlot = mainSlot == 0 ? 1 : 0;
                        PhoneFactory.getPhone(mainSlot).setRadioPower(true, null);
                        PhoneFactory.getPhone(anotherSlot).setRadioPower(false, null);
                        logd("uiccsetActiveModemMode, DUAL_CARD_MODEM_MODE all cards absent set card 1 power on");
                    }
                }
                setAutoSetPowerupModemDone(true);
                setDsdsCfgDone(true);
                this.mWatingSetActiveModeDone = false;
            } else {
                logd("uiccsetActiveModemMode, in airplane mode return");
                setAutoSetPowerupModemDone(true);
                setDsdsCfgDone(true);
                this.mWatingSetActiveModeDone = false;
            }
        }
    }

    private void uiccHwdsdscancelTimeOut() {
        removeMessages(204);
        logd("uiccHwdsdscancelTimeOut");
    }

    private void uiccHwdsdsStartTimeOut() {
        uiccHwdsdscancelTimeOut();
        sendMessageDelayed(obtainMessage(204), 5000);
        logd("uiccHwdsdsStartTimeOut");
    }

    public boolean uiccHwdsdsNeedSetActiveMode() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("uiccHwdsdsNeedSetActiveMode mAutoSetPowerupModemMode=");
        stringBuilder.append(this.mAutoSetPowerupModemDone);
        logd(stringBuilder.toString());
        return !this.mAutoSetPowerupModemDone && IS_DSDSPOWER_SUPPORT && this.isMultiSimEnabled;
    }

    public void uiccHwdsdsGetIccStatusSend() {
        int i = 0;
        for (UiccCard uc : this.mUiccCards) {
            if (this.mUiccCards.length < 2 || uc == null) {
                logd("uiccHwdsdsGetIccStatusSend fail");
                return;
            }
        }
        while (true) {
            int i2 = i;
            if (i2 < this.mCis.length) {
                logd("Received uiccHwdsdsGetIccStatusSend");
                this.mCis[i2].getIccCardStatus(obtainMessage(201, Integer.valueOf(i2)));
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    public void uiccHwdsdsUnregRadioStateEvent() {
        for (CommandsInterface unregisterForRadioStateChanged : this.mCis) {
            unregisterForRadioStateChanged.unregisterForRadioStateChanged(this);
            logd("unregisterForRadioStateChanged");
        }
    }

    private void setAutoSetPowerupModemDone(boolean isDone) {
        this.mAutoSetPowerupModemDone = isDone;
        if (this.mAutoSetPowerupModemDone) {
            HwVSimUtils.processAutoSetPowerupModemDone();
        }
    }

    public void setDsdsCfgDone(boolean isDone) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDsdsCfgDone, value = ");
        stringBuilder.append(isDone);
        logd(stringBuilder.toString());
        this.mDsdsCfgDone = isDone;
    }
}
