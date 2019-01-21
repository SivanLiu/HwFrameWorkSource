package com.android.internal.telephony;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.AbstractSubscriptionInfoUpdater.SubscriptionInfoUpdaterReference;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccCardStatusUtils;
import com.android.internal.telephony.uicc.IccException;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.vsim.HwVSimUtils;
import java.util.List;

public class HwSubscriptionInfoUpdaterReference implements SubscriptionInfoUpdaterReference {
    private static final byte[] C2 = new byte[]{(byte) -89, (byte) 82, (byte) 3, (byte) 85, (byte) -88, (byte) -104, (byte) 57, (byte) -10, (byte) -103, (byte) 108, (byte) -88, (byte) 122, (byte) -38, (byte) -12, (byte) -55, (byte) -2};
    private static final int CARDTRAY_OUT_SLOT = 0;
    private static final boolean DBG = true;
    private static final int EVENT_ICC_CHANGED = 101;
    private static final int EVENT_QUERY_ICCID_DONE = 103;
    private static final String ICCID_STRING_FOR_NO_SIM = "";
    private static final String ICCID_STRING_FOR_NV = "DUMMY_NV_ID";
    public static final boolean IS_MODEM_CAPABILITY_SUPPORT = HwModemCapability.isCapabilitySupport(9);
    public static final boolean IS_QUICK_BROADCAST_STATUS = SystemProperties.getBoolean("ro.quick_broadcast_cardstatus", false);
    private static final boolean IS_SINGLE_CARD_TRAY = SystemProperties.getBoolean("persist.radio.single_card_tray", true);
    private static final String LOG_TAG = "HwSubscriptionInfoUpdaterReference";
    private static final String MASTER_PASSWORD = HwAESCryptoUtil.getKey(SubscriptionHelper.C1, C2, SubscriptionInfoUpdaterUtils.C3);
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final boolean VDBG = false;
    private static Context mContext = null;
    private static IccFileHandler[] mFh = new IccFileHandler[PROJECT_SIM_NUM];
    private static IccRecords[] mIccRecords = new IccRecords[PROJECT_SIM_NUM];
    private static UiccController mUiccController = null;
    private static CardState[] sCardState = new CardState[PROJECT_SIM_NUM];
    private static SubscriptionInfoUpdaterUtils subscriptionInfoUpdaterUtils = new SubscriptionInfoUpdaterUtils();
    private String[] internalOldIccId = new String[PROJECT_SIM_NUM];
    private boolean isNVSubAvailable = false;
    private boolean mChangeIccidDone = false;
    private CommandsInterface[] mCis;
    private Handler mHandler = null;
    private SubscriptionInfoUpdater mSubscriptionInfoUpdater;

    public HwSubscriptionInfoUpdaterReference(SubscriptionInfoUpdater subscriptionInfoUpdater) {
        this.mSubscriptionInfoUpdater = subscriptionInfoUpdater;
    }

    public void subscriptionInfoInit(Handler handler, Context context, CommandsInterface[] ci) {
        this.mCis = (CommandsInterface[]) ci.clone();
        this.mHandler = handler;
        mContext = context;
        SubscriptionHelper.init(context, ci);
        mUiccController = UiccController.getInstance();
        mUiccController.registerForIccChanged(this.mHandler, EVENT_ICC_CHANGED, null);
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            sCardState[i] = CardState.CARDSTATE_ABSENT;
        }
        HwCardTrayInfo.make(ci);
    }

    public void handleMessageExtend(Message msg) {
        AsyncResult ar = msg.obj;
        int i = msg.what;
        Integer cardIndex;
        if (i == EVENT_ICC_CHANGED) {
            cardIndex = Integer.valueOf(0);
            if (ar.result != null) {
                updateIccAvailability(ar.result.intValue());
            } else {
                loge("Error: Invalid card index EVENT_ICC_CHANGED ");
            }
        } else if (i != EVENT_QUERY_ICCID_DONE) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown msg:");
            stringBuilder.append(msg.what);
            logd(stringBuilder.toString());
        } else {
            cardIndex = ar.userObj;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleMessage : <EVENT_QUERY_ICCID_DONE> SIM");
            stringBuilder2.append(cardIndex.intValue() + 1);
            logd(stringBuilder2.toString());
            if (ar.exception == null) {
                if (ar.result != null) {
                    String iccId;
                    if (ar.result instanceof byte[]) {
                        byte[] data = ar.result;
                        iccId = HwTelephonyFactory.getHwUiccManager().bcdIccidToString(data, 0, data.length);
                    } else {
                        try {
                            iccId = (String) ar.result;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[cardIndex.intValue()] = iccId;
                    if (subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[cardIndex.intValue()] != null && subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[cardIndex.intValue()].trim().length() == 0) {
                        String[] iccId2 = subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater);
                        int intValue = cardIndex.intValue();
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("emptyiccid");
                        stringBuilder3.append(cardIndex);
                        iccId2[intValue] = stringBuilder3.toString();
                    }
                    if (HwVSimUtils.needBlockUnReservedForVsim(cardIndex.intValue())) {
                        subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[cardIndex.intValue()] = ICCID_STRING_FOR_NO_SIM;
                        logd("the slot is unreserved for vsim,just set to no_sim");
                    }
                } else {
                    logd("Null ar");
                    subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[cardIndex.intValue()] = ICCID_STRING_FOR_NO_SIM;
                }
            } else if (((ar.exception instanceof CommandException) && (((CommandException) ar.exception).getCommandError() == Error.RADIO_NOT_AVAILABLE || ((CommandException) ar.exception).getCommandError() == Error.GENERIC_FAILURE)) || (ar.exception instanceof IccException)) {
                logd("Do Nothing.");
            } else {
                subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[cardIndex.intValue()] = ICCID_STRING_FOR_NO_SIM;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Query IccId fail: ");
                stringBuilder2.append(ar.exception);
                logd(stringBuilder2.toString());
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mIccId[");
            stringBuilder2.append(cardIndex);
            stringBuilder2.append("] = ");
            stringBuilder2.append(printIccid(subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[cardIndex.intValue()]));
            logd(stringBuilder2.toString());
            setNeedUpdateIfNeed(cardIndex.intValue(), subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[cardIndex.intValue()]);
            if (subscriptionInfoUpdaterUtils.isAllIccIdQueryDone(this.mSubscriptionInfoUpdater)) {
                SubscriptionInfoUpdater subscriptionInfoUpdater = this.mSubscriptionInfoUpdater;
                if (SubscriptionInfoUpdater.mNeedUpdate) {
                    subscriptionInfoUpdaterUtils.updateSubscriptionInfoByIccId(this.mSubscriptionInfoUpdater);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:61:0x0182, code skipped:
            if (com.android.internal.telephony.SubscriptionInfoUpdater.mNeedUpdate != false) goto L_0x0184;
     */
    /* JADX WARNING: Missing block: B:65:0x01a0, code skipped:
            if (subscriptionInfoUpdaterUtils.getIccId(r10.mSubscriptionInfoUpdater)[r11].equals(ICCID_STRING_FOR_NO_SIM) != false) goto L_0x01a2;
     */
    /* JADX WARNING: Missing block: B:66:0x01a2, code skipped:
            r5 = new java.lang.StringBuilder();
            r5.append("SIM");
            r5.append(r11 + 1);
            r5.append(" hot plug in");
            logd(r5.toString());
            subscriptionInfoUpdaterUtils.getIccId(r10.mSubscriptionInfoUpdater)[r11] = null;
            r5 = r10.mSubscriptionInfoUpdater;
            com.android.internal.telephony.SubscriptionInfoUpdater.mNeedUpdate = true;
            resetInternalOldIccId(r11);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateIccAvailability(int slotId) {
        if (mUiccController != null) {
            SubscriptionHelper subHelper = SubscriptionHelper.getInstance();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateIccAvailability: Enter, slotId ");
            stringBuilder.append(slotId);
            logd(stringBuilder.toString());
            String str = null;
            if (PROJECT_SIM_NUM <= 1 || subHelper.proceedToHandleIccEvent(slotId)) {
                StringBuilder stringBuilder2;
                CardState newState = CardState.CARDSTATE_ABSENT;
                UiccCard newCard = mUiccController.getUiccCard(slotId);
                boolean isNeedUpdateForUnReservedVSimSub = false;
                if (newCard != null) {
                    newState = newCard.getCardState();
                    if (!IccCardStatusUtils.isCardPresent(newState) && this.isNVSubAvailable) {
                        Rlog.i(LOG_TAG, "updateIccAvailability: Returning NV mode ");
                        return;
                    }
                }
                String str2 = LOG_TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateIccAvailability: newCard is null, slotId ");
                stringBuilder2.append(slotId);
                Rlog.i(str2, stringBuilder2.toString());
                boolean isNotTwoModemAndRadioNotAvailable = !HwVSimUtils.isPlatformTwoModems() || HwVSimUtils.isRadioAvailable(slotId);
                if (isNotTwoModemAndRadioNotAvailable) {
                    Rlog.i(LOG_TAG, "updateIccAvailability: not vsim pending sub");
                    return;
                }
                CardState oldState = sCardState[slotId];
                sCardState[slotId] = newState;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Slot[");
                stringBuilder2.append(slotId);
                stringBuilder2.append("]: New Card State = ");
                stringBuilder2.append(newState);
                stringBuilder2.append(" Old Card State = ");
                stringBuilder2.append(oldState);
                logd(stringBuilder2.toString());
                SubscriptionInfoUpdater subscriptionInfoUpdater;
                SubscriptionInfoUpdater subscriptionInfoUpdater2;
                String[] iccId;
                StringBuilder stringBuilder3;
                StringBuilder stringBuilder4;
                if (!IccCardStatusUtils.isCardPresent(newState)) {
                    SubscriptionInfoUpdater subscriptionInfoUpdater3;
                    if (!ICCID_STRING_FOR_NO_SIM.equals(subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId])) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SIM");
                        stringBuilder2.append(slotId + 1);
                        stringBuilder2.append(" hot plug out");
                        logd(stringBuilder2.toString());
                        subscriptionInfoUpdater3 = this.mSubscriptionInfoUpdater;
                        SubscriptionInfoUpdater.mNeedUpdate = true;
                        resetInternalOldIccId(slotId);
                    }
                    if (HwVSimUtils.needBlockUnReservedForVsim(slotId) && !IccCardStatusUtils.isCardPresent(newState) && IccCardStatusUtils.isCardPresent(oldState)) {
                        isNeedUpdateForUnReservedVSimSub = true;
                    }
                    if (isNeedUpdateForUnReservedVSimSub) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SIM");
                        stringBuilder2.append(slotId + 1);
                        stringBuilder2.append(" hot plug out when BlockUnReservedForVsim");
                        logd(stringBuilder2.toString());
                        subscriptionInfoUpdater3 = this.mSubscriptionInfoUpdater;
                        SubscriptionInfoUpdater.mNeedUpdate = true;
                    }
                    if (!IS_MODEM_CAPABILITY_SUPPORT) {
                        unRegisterForLoadIccID(slotId);
                        changeIccidForHotplug(slotId, sCardState);
                    }
                    mFh[slotId] = null;
                    subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] = ICCID_STRING_FOR_NO_SIM;
                    if (subscriptionInfoUpdaterUtils.isAllIccIdQueryDone(this.mSubscriptionInfoUpdater)) {
                        subscriptionInfoUpdater = this.mSubscriptionInfoUpdater;
                        if (SubscriptionInfoUpdater.mNeedUpdate) {
                            subscriptionInfoUpdaterUtils.updateSubscriptionInfoByIccId(this.mSubscriptionInfoUpdater);
                        }
                    }
                } else if (!IccCardStatusUtils.isCardPresent(oldState) && IccCardStatusUtils.isCardPresent(newState)) {
                    if (this.mChangeIccidDone && !HwVSimUtils.isPlatformRealTripple() && HwVSimUtils.isVSimOn()) {
                        subscriptionInfoUpdater2 = this.mSubscriptionInfoUpdater;
                    }
                    if (subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] != null) {
                    }
                    if (IS_MODEM_CAPABILITY_SUPPORT) {
                        queryIccId(slotId);
                    } else if (!IS_QUICK_BROADCAST_STATUS || this.mCis == null || this.mCis[slotId] == null) {
                        CharSequence iccId2;
                        changeIccidForHotplug(slotId, sCardState);
                        registerForLoadIccID(slotId);
                        iccId = subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater);
                        if (newCard != null) {
                            iccId2 = newCard.getIccId();
                        }
                        iccId[slotId] = iccId2;
                        if (!TextUtils.isEmpty(iccId2)) {
                            logd("need to update subscription after fligt mode on and off..");
                            if (HwVSimUtils.needBlockUnReservedForVsim(slotId)) {
                                subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] = ICCID_STRING_FOR_NO_SIM;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("the slot ");
                                stringBuilder3.append(slotId);
                                stringBuilder3.append(" is unreserved for vsim,just set to no_sim");
                                logd(stringBuilder3.toString());
                            }
                            setNeedUpdateIfNeed(slotId, subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId]);
                            if (subscriptionInfoUpdaterUtils.isAllIccIdQueryDone(this.mSubscriptionInfoUpdater)) {
                                subscriptionInfoUpdater = this.mSubscriptionInfoUpdater;
                                if (SubscriptionInfoUpdater.mNeedUpdate) {
                                    subscriptionInfoUpdaterUtils.updateSubscriptionInfoByIccId(this.mSubscriptionInfoUpdater);
                                }
                            }
                        }
                    } else {
                        changeIccidForHotplug(slotId, sCardState);
                        this.mCis[slotId].getICCID(this.mHandler.obtainMessage(EVENT_QUERY_ICCID_DONE, Integer.valueOf(slotId)));
                    }
                } else if (IccCardStatusUtils.isCardPresent(oldState) && IccCardStatusUtils.isCardPresent(newState) && TextUtils.isEmpty(subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId])) {
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("SIM");
                    stringBuilder4.append(slotId + 1);
                    stringBuilder4.append(" need to read iccid again in case of rild restart");
                    logd(stringBuilder4.toString());
                    subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] = null;
                    subscriptionInfoUpdater2 = this.mSubscriptionInfoUpdater;
                    SubscriptionInfoUpdater.mNeedUpdate = true;
                    resetInternalOldIccId(slotId);
                    if (IS_MODEM_CAPABILITY_SUPPORT) {
                        queryIccId(slotId);
                    } else if (!IS_QUICK_BROADCAST_STATUS || this.mCis == null || this.mCis[slotId] == null) {
                        registerForLoadIccID(slotId);
                        iccId = subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater);
                        if (newCard != null) {
                            str = newCard.getIccId();
                        }
                        iccId[slotId] = str;
                        if (subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] != null && subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId].trim().length() == 0) {
                            String[] iccId3 = subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater);
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("emptyiccid");
                            stringBuilder5.append(slotId);
                            iccId3[slotId] = stringBuilder5.toString();
                        }
                        if (HwVSimUtils.needBlockUnReservedForVsim(slotId)) {
                            subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] = ICCID_STRING_FOR_NO_SIM;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("the slot ");
                            stringBuilder3.append(slotId);
                            stringBuilder3.append(" is unreserved for vsim,just set to no_sim");
                            logd(stringBuilder3.toString());
                        }
                        setNeedUpdateIfNeed(slotId, subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId]);
                        if (subscriptionInfoUpdaterUtils.isAllIccIdQueryDone(this.mSubscriptionInfoUpdater)) {
                            subscriptionInfoUpdater = this.mSubscriptionInfoUpdater;
                            if (SubscriptionInfoUpdater.mNeedUpdate) {
                                subscriptionInfoUpdaterUtils.updateSubscriptionInfoByIccId(this.mSubscriptionInfoUpdater);
                            }
                        }
                    } else {
                        this.mCis[slotId].getICCID(this.mHandler.obtainMessage(EVENT_QUERY_ICCID_DONE, Integer.valueOf(slotId)));
                    }
                } else if (IccCardStatusUtils.isCardPresent(oldState) && IccCardStatusUtils.isCardPresent(newState) && !subHelper.isApmSIMNotPwdn() && subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] == null) {
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("SIM");
                    stringBuilder4.append(slotId + 1);
                    stringBuilder4.append(" powered up from APM ");
                    logd(stringBuilder4.toString());
                    mFh[slotId] = null;
                    subscriptionInfoUpdater = this.mSubscriptionInfoUpdater;
                    SubscriptionInfoUpdater.mNeedUpdate = true;
                    resetInternalOldIccId(slotId);
                    if (IS_MODEM_CAPABILITY_SUPPORT) {
                        queryIccId(slotId);
                    } else if (!IS_QUICK_BROADCAST_STATUS || this.mCis == null || this.mCis[slotId] == null) {
                        unRegisterForLoadIccID(slotId);
                        registerForLoadIccID(slotId);
                    } else {
                        this.mCis[slotId].getICCID(this.mHandler.obtainMessage(EVENT_QUERY_ICCID_DONE, Integer.valueOf(slotId)));
                    }
                } else if (IccCardStatusUtils.isCardPresent(oldState) && IccCardStatusUtils.isCardPresent(newState) && subHelper.needSubActivationAfterRefresh(slotId)) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("SIM");
                    stringBuilder3.append(slotId + 1);
                    stringBuilder3.append(" refresh happened, need sub activation");
                    logd(stringBuilder3.toString());
                    if (subscriptionInfoUpdaterUtils.isAllIccIdQueryDone(this.mSubscriptionInfoUpdater)) {
                        subscriptionInfoUpdaterUtils.updateSubscriptionInfoByIccId(this.mSubscriptionInfoUpdater);
                    }
                }
                return;
            }
            logd("updateIccAvailability: radio is OFF/unavailable, ignore ");
            if (!subHelper.isApmSIMNotPwdn()) {
                subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] = null;
            }
        }
    }

    private void changeIccidForHotplug(int slotId, CardState[] cardState) {
        if (IS_SINGLE_CARD_TRAY) {
            int i = 0;
            StringBuilder stringBuilder;
            int i2;
            if (IccCardStatusUtils.isCardPresent(cardState[slotId]) || !HwCardTrayInfo.getInstance().isCardTrayOut(0)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("changeIccidForHotplug  mChangeIccidDone= ");
                stringBuilder.append(this.mChangeIccidDone);
                logd(stringBuilder.toString());
                if (!this.mChangeIccidDone) {
                    while (true) {
                        i2 = i;
                        if (i2 < PROJECT_SIM_NUM) {
                            if (i2 != slotId) {
                                subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[i2] = null;
                                SubscriptionInfoUpdater subscriptionInfoUpdater = this.mSubscriptionInfoUpdater;
                                SubscriptionInfoUpdater.mNeedUpdate = true;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("set iccid null i =  ");
                                stringBuilder2.append(i2);
                                logd(stringBuilder2.toString());
                            }
                            i = i2 + 1;
                        } else {
                            this.mChangeIccidDone = true;
                            return;
                        }
                    }
                }
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("cardTray out set mChangeIccidDone = ");
            stringBuilder.append(this.mChangeIccidDone);
            logd(stringBuilder.toString());
            if (this.mChangeIccidDone) {
                this.mChangeIccidDone = false;
                i2 = 0;
                while (i2 < PROJECT_SIM_NUM) {
                    if (i2 != slotId && IccCardStatusUtils.isCardPresent(cardState[i2])) {
                        SubscriptionInfoUpdater subscriptionInfoUpdater2 = this.mSubscriptionInfoUpdater;
                        SubscriptionInfoUpdater.mNeedUpdate = false;
                        logd("cardTray out set first card mNeedUpdate to false");
                    }
                    i2++;
                }
                return;
            }
            return;
        }
        logd("changeIccidForHotplug don't for two card tray.");
    }

    public void queryIccId(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryIccId: slotid=");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        if (mFh[slotId] == null) {
            logd("Getting IccFileHandler");
            UiccCardApplication validApp = null;
            UiccCard uiccCard = mUiccController.getUiccCard(slotId);
            if (uiccCard != null) {
                int numApps = uiccCard.getNumApplications();
                for (int i = 0; i < numApps; i++) {
                    UiccCardApplication app = uiccCard.getApplicationIndex(i);
                    if (app != null && app.getType() != AppType.APPTYPE_UNKNOWN) {
                        validApp = app;
                        break;
                    }
                }
            }
            if (validApp != null) {
                mFh[slotId] = validApp.getIccFileHandler();
            }
        }
        if (mFh[slotId] != null) {
            String iccId = subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId];
            if (iccId == null) {
                logd("Querying IccId");
                mFh[slotId].loadEFTransparent(12258, this.mHandler.obtainMessage(EVENT_QUERY_ICCID_DONE, Integer.valueOf(slotId)));
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("NOT Querying IccId its already set sIccid[");
            stringBuilder2.append(slotId);
            stringBuilder2.append("]=");
            stringBuilder2.append(printIccid(iccId));
            logd(stringBuilder2.toString());
            return;
        }
        sCardState[slotId] = CardState.CARDSTATE_ABSENT;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mFh[");
        stringBuilder.append(slotId);
        stringBuilder.append("] is null, SIM not inserted");
        logd(stringBuilder.toString());
    }

    public void resetIccid(int slotId) {
        if (slotId < 0 || slotId >= subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater).length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("resetIccid: invaild slotid =");
            stringBuilder.append(slotId);
            logd(stringBuilder.toString());
            return;
        }
        logd("resetIccid: set iccid is null");
        subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] = null;
    }

    public void updateSubIdForNV(int slotId) {
        subscriptionInfoUpdaterUtils.getIccId(this.mSubscriptionInfoUpdater)[slotId] = ICCID_STRING_FOR_NV;
        SubscriptionInfoUpdater subscriptionInfoUpdater = this.mSubscriptionInfoUpdater;
        SubscriptionInfoUpdater.mNeedUpdate = true;
        logd("[updateSubIdForNV]+ Start");
        if (subscriptionInfoUpdaterUtils.isAllIccIdQueryDone(this.mSubscriptionInfoUpdater)) {
            logd("[updateSubIdForNV]+ updating");
            subscriptionInfoUpdaterUtils.updateSubscriptionInfoByIccId(this.mSubscriptionInfoUpdater);
            this.isNVSubAvailable = true;
        }
    }

    public void updateSubActivation(int[] simStatus, boolean isStackReadyEvent) {
        if (HuaweiTelephonyConfigs.isQcomPlatform()) {
            SubscriptionHelper.getInstance().updateNwMode();
        }
        SubscriptionHelper.getInstance().updateSubActivation(subscriptionInfoUpdaterUtils.getInsertSimState(this.mSubscriptionInfoUpdater), false);
    }

    public void broadcastSubinfoRecordUpdated(String[] iccId, String[] oldIccId, int nNewCardCount, int nSubCount, int nNewSimStatus) {
        logd("broadcastSubinfoRecordUpdated");
        if (HwDsdsController.IS_DSDSPOWER_SUPPORT) {
            logd("setSubinfoAutoUpdateDone true");
            HwDsdsController.getInstance().setSubinfoAutoUpdateDone(true);
        }
        int i = 0;
        boolean hasSimRemoved = false;
        for (int i2 = 0; i2 < PROJECT_SIM_NUM; i2++) {
            boolean z = (iccId[i2] == null || !iccId[i2].equals(ICCID_STRING_FOR_NO_SIM) || oldIccId[i2].equals(ICCID_STRING_FOR_NO_SIM)) ? false : true;
            hasSimRemoved = z;
            if (hasSimRemoved) {
                break;
            }
        }
        if (nNewCardCount != 0) {
            setUpdatedDataToNewCard(iccId, nSubCount, nNewSimStatus);
        } else if (hasSimRemoved) {
            while (i < PROJECT_SIM_NUM) {
                if (subscriptionInfoUpdaterUtils.getInsertSimState(this.mSubscriptionInfoUpdater)[i] == -3) {
                    logd("No new SIM detected and SIM repositioned");
                    setUpdatedData(3, nSubCount, nNewSimStatus);
                    break;
                }
                i++;
            }
            if (i == PROJECT_SIM_NUM) {
                logd("No new SIM detected and SIM removed");
                setUpdatedData(2, nSubCount, nNewSimStatus);
            }
        } else {
            while (i < PROJECT_SIM_NUM) {
                if (subscriptionInfoUpdaterUtils.getInsertSimState(this.mSubscriptionInfoUpdater)[i] == -3) {
                    logd("No new SIM detected and SIM repositioned");
                    setUpdatedData(3, nSubCount, nNewSimStatus);
                    break;
                }
                i++;
            }
            if (i == PROJECT_SIM_NUM) {
                logd("[updateSimInfoByIccId] All SIM inserted into the same slot");
                setUpdatedData(4, nSubCount, nNewSimStatus);
            }
        }
    }

    private void setUpdatedDataToNewCard(String[] iccId, int nSubCount, int nNewSimStatus) {
        if (HwVSimUtils.isVSimCauseCardReload() || HwVSimUtils.isVSimEnabled()) {
            logd("VSim is enabled or VSim caused card status change, skip");
            return;
        }
        logd("New SIM detected");
        if (isNewSimCardInserted(iccId).booleanValue()) {
            setUpdatedData(1, nSubCount, nNewSimStatus);
            return;
        }
        logd("Insert Same Sim");
        setUpdatedData(5, nSubCount, nNewSimStatus);
    }

    public Boolean isNewSimCardInserted(String[] sIccId) {
        StringBuilder stringBuilder;
        int i = 0;
        Boolean result = Boolean.valueOf(false);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        Editor editor = sp.edit();
        while (i < PROJECT_SIM_NUM) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("SP_SUBINFO_SLOT");
            stringBuilder2.append(i);
            String sIccIdOld = sp.getString(stringBuilder2.toString(), null);
            String iccIdInSP = ICCID_STRING_FOR_NO_SIM;
            if (!(sIccIdOld == null || ICCID_STRING_FOR_NO_SIM.equals(sIccIdOld))) {
                try {
                    iccIdInSP = HwAESCryptoUtil.decrypt(MASTER_PASSWORD, sIccIdOld);
                } catch (Exception ex) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwAESCryptoUtil decrypt excepiton:");
                    stringBuilder.append(ex.getMessage());
                    logd(stringBuilder.toString());
                }
            }
            if (!(sIccId[i] == null || sIccId[i].equals(ICCID_STRING_FOR_NO_SIM) || (sIccIdOld != null && sIccId[i].equals(iccIdInSP)))) {
                result = Boolean.valueOf(true);
                String iccidEncrypted = ICCID_STRING_FOR_NO_SIM;
                try {
                    iccidEncrypted = HwAESCryptoUtil.encrypt(MASTER_PASSWORD, sIccId[i]);
                } catch (Exception ex2) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("HwAESCryptoUtil encrypt excepiton:");
                    stringBuilder3.append(ex2.getMessage());
                    logd(stringBuilder3.toString());
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("SP_SUBINFO_SLOT");
                stringBuilder.append(i);
                editor.putString(stringBuilder.toString(), iccidEncrypted);
                editor.apply();
            }
            i++;
        }
        return result;
    }

    private static void setUpdatedData(int detectedType, int subCount, int newSimStatus) {
        Intent intent = new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        logd("[setUpdatedData]+ ");
        setIntentExtra(intent, detectedType, subCount, newSimStatus);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("broadcast intent ACTION_SUBINFO_RECORD_UPDATED : [");
        stringBuilder.append(detectedType);
        stringBuilder.append(", ");
        stringBuilder.append(subCount);
        stringBuilder.append(", ");
        stringBuilder.append(newSimStatus);
        stringBuilder.append("]");
        logd(stringBuilder.toString());
        ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
        intent = new Intent("com.huawei.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        setIntentExtra(intent, detectedType, subCount, newSimStatus);
        sendBroadcastForRecordUpdate(intent);
        logd("[setUpdatedData]- ");
    }

    private static void sendBroadcastForRecordUpdate(Intent intent) {
        if (mContext != null) {
            PackageManager pm = mContext.getPackageManager();
            if (pm != null) {
                int index = 0;
                List<ResolveInfo> Receivers = pm.queryBroadcastReceivers(intent, 0);
                if (!(Receivers == null || Receivers.isEmpty())) {
                    int size = Receivers.size();
                    while (index < size) {
                        Intent newIntent = new Intent(intent);
                        String packageName = getPackageName((ResolveInfo) Receivers.get(index));
                        if (packageName != null) {
                            newIntent.setPackage(packageName);
                            ActivityManagerNative.broadcastStickyIntent(newIntent, "android.permission.READ_PHONE_STATE", -1);
                        }
                        index++;
                    }
                }
            }
        }
    }

    private static String getPackageName(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        if (resolveInfo.serviceInfo != null) {
            return resolveInfo.serviceInfo.packageName;
        }
        if (resolveInfo.providerInfo != null) {
            return resolveInfo.providerInfo.packageName;
        }
        return null;
    }

    private static void setIntentExtra(Intent intent, int detectedType, int subCount, int newSimStatus) {
        if (detectedType == 1) {
            intent.putExtra("simDetectStatus", 1);
            intent.putExtra("simCount", subCount);
            intent.putExtra("newSIMSlot", newSimStatus);
        } else if (detectedType == 3) {
            intent.putExtra("simDetectStatus", 3);
            intent.putExtra("simCount", subCount);
        } else if (detectedType == 2) {
            intent.putExtra("simDetectStatus", 2);
            intent.putExtra("simCount", subCount);
        } else if (detectedType == 4) {
            intent.putExtra("simDetectStatus", 4);
        } else if (detectedType == 5) {
            intent.putExtra("simDetectStatus", 5);
        }
    }

    private String printIccid(String iccid) {
        if (iccid == null) {
            return "null";
        }
        if (iccid.length() < 6) {
            return "less than 6 digits";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(iccid.substring(0, 6));
        stringBuilder.append(new String(new char[(iccid.length() - 6)]).replace(0, '*'));
        return stringBuilder.toString();
    }

    private void resetInternalOldIccId(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resetInternalOldIccId slotId:");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        if (slotId >= 0 && slotId < PROJECT_SIM_NUM) {
            this.internalOldIccId[slotId] = null;
        }
    }

    public void setNeedUpdateIfNeed(int slotId, String currentIccId) {
        if (slotId >= 0 && slotId < PROJECT_SIM_NUM) {
            if (!(currentIccId == null || currentIccId.equals(this.internalOldIccId[slotId]))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("internalOldIccId[");
                stringBuilder.append(slotId);
                stringBuilder.append("]:");
                stringBuilder.append(printIccid(this.internalOldIccId[slotId]));
                stringBuilder.append(" currentIccId[");
                stringBuilder.append(slotId);
                stringBuilder.append("]:");
                stringBuilder.append(printIccid(currentIccId));
                stringBuilder.append(" set mNeedUpdate = true");
                logd(stringBuilder.toString());
                this.mSubscriptionInfoUpdater.setNeedUpdate(true);
            }
            this.internalOldIccId[slotId] = currentIccId;
        }
    }

    private void registerForLoadIccID(int slotId) {
        UiccCard uiccCard = mUiccController.getUiccCard(slotId);
        if (uiccCard != null) {
            UiccCardApplication validApp;
            UiccCardApplication app = uiccCard.getApplication(1);
            if (app != null) {
                validApp = app;
            } else {
                validApp = uiccCard.getApplication(2);
            }
            if (validApp != null) {
                IccRecords newIccRecords = validApp.getIccRecords();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SIM");
                stringBuilder.append(slotId + 1);
                stringBuilder.append(" new : ");
                logd(stringBuilder.toString());
                StringBuilder stringBuilder2;
                if (validApp.getState() == AppState.APPSTATE_PIN || validApp.getState() == AppState.APPSTATE_PUK) {
                    queryIccId(slotId);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("registerForLoadIccID query iccid SIM");
                    stringBuilder2.append(slotId + 1);
                    stringBuilder2.append(" for pin or puk");
                    logd(stringBuilder2.toString());
                } else if (newIccRecords != null && (newIccRecords instanceof RuimRecords) && PhoneFactory.getPhone(slotId).getPhoneType() == 1) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("registerForLoadIccID query iccid SIM");
                    stringBuilder2.append(slotId + 1);
                    stringBuilder2.append(" for single mode ruim card");
                    logd(stringBuilder2.toString());
                    queryIccId(slotId);
                } else if (newIccRecords != null && (mIccRecords[slotId] == null || newIccRecords != mIccRecords[slotId])) {
                    if (mIccRecords[slotId] != null) {
                        mIccRecords[slotId].unRegisterForLoadIccID(this.mHandler);
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("registerForLoadIccID SIM");
                    stringBuilder2.append(slotId + 1);
                    logd(stringBuilder2.toString());
                    mIccRecords[slotId] = newIccRecords;
                    mIccRecords[slotId].registerForLoadIccID(this.mHandler, EVENT_QUERY_ICCID_DONE, Integer.valueOf(slotId));
                }
            } else {
                logd("validApp is null");
            }
        }
    }

    private void unRegisterForLoadIccID(int slotId) {
        if (mIccRecords[slotId] != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unRegisterForLoadIccID SIM");
            stringBuilder.append(slotId + 1);
            logd(stringBuilder.toString());
            mIccRecords[slotId].unRegisterForLoadIccID(this.mHandler);
            mIccRecords[slotId] = null;
        }
    }

    private static void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
