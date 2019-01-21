package com.android.internal.telephony.fullnetwork;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HwAESCryptoUtil;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.fullnetwork.HwFullNetworkChipCommon.HwFullNetworkChipInterface;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants.CommrilMode;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants.HotplugState;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.vsim.HwVSimUtils;

public class HwFullNetworkChipHisi implements HwFullNetworkChipInterface {
    private static final String LOG_TAG = "HwFullNetworkChipHisi";
    private static HwFullNetworkChipCommon mChipCommon;
    private static HwFullNetworkChipHisi mInstance;
    private static final Object mLock = new Object();
    int curSetDataAllowCount = 0;
    boolean isHotPlugCompleted = false;
    boolean isPreBootCompleted = false;
    boolean mAllCardsReady = false;
    boolean mAutoSwitchDualCardsSlotDone = false;
    int mBalongSimSlot = 0;
    boolean mBroadcastDone = false;
    int[] mCardTypes = new int[HwFullNetworkConstants.SIM_NUM];
    boolean mCommrilRestartRild = false;
    private Context mContext;
    String[] mFullIccIds = new String[HwFullNetworkConstants.SIM_NUM];
    boolean[] mGetBalongSimSlotDone = new boolean[HwFullNetworkConstants.SIM_NUM];
    boolean[] mGetUiccCardsStatusDone = new boolean[HwFullNetworkConstants.SIM_NUM];
    HotplugState[] mHotplugState = new HotplugState[HwFullNetworkConstants.SIM_NUM];
    private RegistrantList mIccChangedRegistrants = new RegistrantList();
    boolean mNvRestartRildDone = false;
    int[] mOldMainSwitchTypes = new int[HwFullNetworkConstants.SIM_NUM];
    boolean[] mRadioOn = new boolean[HwFullNetworkConstants.SIM_NUM];
    boolean[] mRadioOns = new boolean[HwFullNetworkConstants.SIM_NUM];
    Message mSetSdcsCompleteMsg = null;
    int[] mSwitchTypes = new int[HwFullNetworkConstants.SIM_NUM];
    boolean needFixMainSlotPosition = false;
    int needSetDataAllowCount = 0;

    private HwFullNetworkChipHisi(Context context, CommandsInterface[] ci) {
        logd("HwFullNetworkChipHisi constructor");
        this.mContext = context;
    }

    static HwFullNetworkChipHisi make(Context context, CommandsInterface[] ci) {
        HwFullNetworkChipHisi hwFullNetworkChipHisi;
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new HwFullNetworkChipHisi(context, ci);
                mChipCommon = HwFullNetworkChipCommon.getInstance();
                mChipCommon.setChipInterface(mInstance);
                hwFullNetworkChipHisi = mInstance;
            } else {
                throw new RuntimeException("HwFullNetworkChipHisi.make() should only be called once");
            }
        }
        return hwFullNetworkChipHisi;
    }

    static HwFullNetworkChipHisi getInstance() {
        HwFullNetworkChipHisi hwFullNetworkChipHisi;
        synchronized (mLock) {
            if (mInstance != null) {
                hwFullNetworkChipHisi = mInstance;
            } else {
                throw new RuntimeException("HwFullNetworkChipHisi.getInstance can't be called before make()");
            }
        }
        return hwFullNetworkChipHisi;
    }

    public void handleIccATR(String strATR, Integer index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleIccATR, ATR: [");
        stringBuilder.append(strATR);
        stringBuilder.append("], index:[");
        stringBuilder.append(index);
        stringBuilder.append("]");
        logd(stringBuilder.toString());
        if (strATR == null || strATR.isEmpty()) {
            strATR = "null";
        }
        if (strATR.length() > 66) {
            logd("strATR.length() greater than PROP_VALUE_MAX");
            strATR = strATR.substring(0, 66);
        }
        if (index.intValue() == 0) {
            SystemProperties.set("gsm.sim.hw_atr", strATR);
        } else {
            SystemProperties.set("gsm.sim.hw_atr1", strATR);
        }
    }

    public void onGetCdmaModeSideDone(AsyncResult ar, Integer index) {
        logd("onGetCdmaModeSideDone");
        int mCdmaModemSide = 0;
        CommrilMode currentCommrilModem = CommrilMode.NON_MODE;
        if (!(ar == null || ar.exception != null || ar.result == null)) {
            mCdmaModemSide = ((int[]) ar.result)[0];
        }
        if (mCdmaModemSide == 0) {
            currentCommrilModem = CommrilMode.HISI_CGUL_MODE;
        } else if (1 == mCdmaModemSide) {
            currentCommrilModem = CommrilMode.HISI_CG_MODE;
        } else if (2 == mCdmaModemSide) {
            currentCommrilModem = CommrilMode.HISI_VSIM_MODE;
        }
        SystemProperties.set("persist.radio.commril_mode", currentCommrilModem.toString());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onGetCdmaModeSideDone mCdmaModemSide = ");
        stringBuilder.append(mCdmaModemSide);
        stringBuilder.append(" set currentCommrilModem=");
        stringBuilder.append(currentCommrilModem);
        logd(stringBuilder.toString());
    }

    public boolean isSwitchDualCardSlotsEnabled() {
        boolean isValidSwitchType = false;
        if (mChipCommon.mUiccController == null || mChipCommon.mUiccController.getUiccCards() == null || mChipCommon.mUiccController.getUiccCards().length < 2) {
            loge("haven't get all UiccCards done, please wait!");
            return false;
        }
        for (UiccCard uc : mChipCommon.mUiccController.getUiccCards()) {
            if (uc == null) {
                loge("haven't get all UiccCards done, pls wait!");
                return false;
            }
        }
        if (!mChipCommon.isSwitchSlotEnabledForCMCC()) {
            logd("isSwitchSlotEnabledForCMCC: CMCC hybird and CMCC is not roaming return false");
            return false;
        } else if (HwFullNetworkConfig.IS_CT_4GSWITCH_DISABLE && mChipCommon.isCTHybird()) {
            return false;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mSwitchTypes[0] = ");
            stringBuilder.append(this.mSwitchTypes[0]);
            stringBuilder.append(", mSwitchTypes[1] = ");
            stringBuilder.append(this.mSwitchTypes[1]);
            logd(stringBuilder.toString());
            if (HwFullNetworkConfig.IS_CHINA_TELECOM) {
                boolean result = false;
                if ((this.mSwitchTypes[0] == 3 && this.mSwitchTypes[1] == 3) || ((this.mSwitchTypes[0] == 3 && this.mSwitchTypes[1] == 2) || ((this.mSwitchTypes[0] == 2 && this.mSwitchTypes[1] == 3) || ((this.mSwitchTypes[0] == 2 && this.mSwitchTypes[1] == 2) || (this.mSwitchTypes[0] == 1 && this.mSwitchTypes[1] == 1))))) {
                    isValidSwitchType = true;
                }
                if (isValidSwitchType) {
                    result = true;
                }
                return result;
            }
            refreshCardState();
            if ((this.mSwitchTypes[0] > 0 || mChipCommon.isSimInsertedArray[0]) && (this.mSwitchTypes[1] > 0 || mChipCommon.isSimInsertedArray[1])) {
                isValidSwitchType = true;
            }
            return isValidSwitchType;
        }
    }

    public void registerForIccChanged(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mIccChangedRegistrants.add(r);
            r.notifyRegistrant();
        }
    }

    public void unregisterForIccChanged(Handler h) {
        synchronized (mLock) {
            this.mIccChangedRegistrants.remove(h);
        }
    }

    public void setWaitingSwitchBalongSlot(boolean iSetResult) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setWaitingSwitchBalongSlot  iSetResult = ");
        stringBuilder.append(iSetResult);
        logd(stringBuilder.toString());
        mChipCommon.isSet4GSlotInProgress = iSetResult;
        SystemProperties.set("gsm.dualcards.switch", iSetResult ? "true" : "false");
        this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult(null, Integer.valueOf(0), null));
        this.mIccChangedRegistrants.notifyRegistrants(new AsyncResult(null, Integer.valueOf(1), null));
    }

    public boolean isBalongSimSynced() {
        return mChipCommon.getUserSwitchDualCardSlots() == this.mBalongSimSlot;
    }

    public boolean getWaitingSwitchBalongSlot() {
        return mChipCommon.isSet4GSlotInProgress;
    }

    public boolean anySimCardChanged() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mChipCommon.mContext);
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("4G_AUTO_SWITCH_ICCID_SLOT");
            stringBuilder.append(i);
            String oldIccId = sp.getString(stringBuilder.toString(), "");
            if (!"".equals(oldIccId)) {
                try {
                    oldIccId = HwAESCryptoUtil.decrypt(HwFullNetworkConstants.MASTER_PASSWORD, oldIccId);
                } catch (Exception ex) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("HwAESCryptoUtil decrypt excepiton:");
                    stringBuilder2.append(ex.getMessage());
                    logd(stringBuilder2.toString());
                }
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("anySimCardChanged oldIccId[");
            stringBuilder3.append(i);
            stringBuilder3.append("] = ");
            stringBuilder3.append(SubscriptionInfo.givePrintableIccid(oldIccId));
            logd(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("anySimCardChanged nowIccId[");
            stringBuilder3.append(i);
            stringBuilder3.append("] = ");
            stringBuilder3.append(SubscriptionInfo.givePrintableIccid(mChipCommon.mIccIds[i]));
            logd(stringBuilder3.toString());
            if (!oldIccId.equals(this.mFullIccIds[i])) {
                return true;
            }
        }
        return false;
    }

    public void disposeCardStatus(boolean resetSwitchDualCardsFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("disposeCardStatus. resetSwitchDualCardsFlag = ");
        stringBuilder.append(resetSwitchDualCardsFlag);
        logd(stringBuilder.toString());
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            this.mSwitchTypes[i] = -1;
            this.mGetUiccCardsStatusDone[i] = false;
            this.mGetBalongSimSlotDone[i] = false;
            this.mCardTypes[i] = -1;
            mChipCommon.mIccIds[i] = null;
            this.mFullIccIds[i] = null;
        }
        this.mAllCardsReady = false;
        if (HwFullNetworkConfig.IS_HISI_DSDX && resetSwitchDualCardsFlag) {
            logd("set mAutoSwitchDualCardsSlotDone to false");
            this.mAutoSwitchDualCardsSlotDone = false;
        }
    }

    public void setPrefNwForCmcc(Handler h) {
        logd("setPrefNwForCmcc enter.");
        if (!HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE || !HwFullNetworkConfig.IS_VICE_WCDMA) {
            return;
        }
        if (HwVSimUtils.isVSimEnabled() || HwVSimUtils.isVSimInProcess()) {
            logd("setPrefNwForCmcc: vsim on sub, not set pref nw for cmcc.");
            return;
        }
        int i = 0;
        for (int i2 = 0; i2 < HwFullNetworkConstants.SIM_NUM; i2++) {
            if (mChipCommon.mIccIds[i2] == null) {
                i = new StringBuilder();
                i.append("setPrefNwForCmcc: mIccIds[");
                i.append(i2);
                i.append("] is null");
                logd(i.toString());
                return;
            }
        }
        Phone[] phones = PhoneFactory.getPhones();
        while (i < HwFullNetworkConstants.SIM_NUM) {
            Phone phone = phones[i];
            if (phone == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setPrefNwForCmcc: phone ");
                stringBuilder.append(i);
                stringBuilder.append(" is null");
                loge(stringBuilder.toString());
            } else {
                int networkMode;
                int i3 = 8;
                int i4 = 9;
                int i5 = 4;
                if (mChipCommon.getUserSwitchDualCardSlots() == i) {
                    HwTelephonyManagerInner mHwTelephonyManager = HwTelephonyManagerInner.getDefault();
                    int ability = 1;
                    if (mHwTelephonyManager != null) {
                        ability = mHwTelephonyManager.getLteServiceAbility();
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("setPrefNwForCmcc: LteServiceAbility = ");
                        stringBuilder2.append(ability);
                        logd(stringBuilder2.toString());
                    }
                    if (HwFullNetworkConfig.IS_FAST_SWITCH_SIMSLOT || !mChipCommon.isCDMASimCard(i)) {
                        if (!(ability == 1 || mChipCommon.isCMCCCardBySlotId(i))) {
                            i4 = 3;
                        }
                        networkMode = i4;
                    } else {
                        if (ability != 1) {
                            i3 = 4;
                        }
                        networkMode = i3;
                    }
                } else if (!mChipCommon.isCMCCHybird() || mChipCommon.isCMCCCardBySlotId(i) || TelephonyManager.getDefault().isNetworkRoaming(mChipCommon.getCMCCCardSlotId())) {
                    networkMode = mChipCommon.isCDMASimCard(i) ? 4 : 3;
                    if (HwFullNetworkConfig.IS_DUAL_4G_SUPPORTED) {
                        if (mChipCommon.isCMCCCardBySlotId(i)) {
                            if (!mChipCommon.isDualImsSwitchOpened()) {
                                i4 = 3;
                            }
                            networkMode = i4;
                        } else if (mChipCommon.isDualImsSwitchOpened()) {
                            if (!mChipCommon.isCDMASimCard(i)) {
                                i3 = 9;
                            }
                            networkMode = i3;
                        } else {
                            if (!mChipCommon.isCDMASimCard(i)) {
                                i5 = 3;
                            }
                            networkMode = i5;
                        }
                    }
                } else {
                    if (!mChipCommon.isCDMASimCard(i)) {
                        i5 = 3;
                    }
                    networkMode = i5;
                }
                phone.setPreferredNetworkType(networkMode, h.obtainMessage(HwFullNetworkConstants.EVENT_CMCC_SET_NETWOR_DONE, i, networkMode));
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("setPrefNwForCmcc: i = ");
                stringBuilder3.append(i);
                stringBuilder3.append(", mode = ");
                stringBuilder3.append(networkMode);
                logd(stringBuilder3.toString());
            }
            i++;
        }
    }

    public void handleSetCmccPrefNetwork(Message msg) {
        int prefslot = msg.arg1;
        int setPrefMode = msg.arg2;
        AsyncResult ar = msg.obj;
        if (ar == null || ar.exception != null) {
            mChipCommon.needRetrySetPrefNetwork = true;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPrefNwForCmcc: Fail, slot ");
            stringBuilder.append(prefslot);
            stringBuilder.append(" network mode ");
            stringBuilder.append(setPrefMode);
            loge(stringBuilder.toString());
            return;
        }
        if (getNetworkTypeFromDB(prefslot) != setPrefMode) {
            setNetworkTypeToDB(prefslot, setPrefMode);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setPrefNwForCmcc: Success, slot ");
        stringBuilder2.append(prefslot);
        stringBuilder2.append(" network mode ");
        stringBuilder2.append(setPrefMode);
        logd(stringBuilder2.toString());
    }

    private int getNetworkTypeFromDB(int phoneId) {
        int networkType = -1;
        try {
            int networkType2;
            if (HwFullNetworkConfig.IS_DUAL_4G_SUPPORTED && HwFullNetworkConstants.SIM_NUM > 1) {
                ContentResolver contentResolver = mChipCommon.mContext.getContentResolver();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("preferred_network_mode");
                stringBuilder.append(phoneId);
                networkType2 = Global.getInt(contentResolver, stringBuilder.toString(), -1);
            } else if (phoneId != mChipCommon.getUserSwitchDualCardSlots() && 1 != HwFullNetworkConstants.SIM_NUM) {
                return networkType;
            } else {
                networkType2 = Global.getInt(mChipCommon.mContext.getContentResolver(), "preferred_network_mode", -1);
            }
            return networkType2;
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getNetworkTypeFromDB Exception = ");
            stringBuilder2.append(e);
            stringBuilder2.append(",phoneId");
            loge(stringBuilder2.toString());
            return networkType;
        }
    }

    private void setNetworkTypeToDB(int phoneId, int prefMode) {
        StringBuilder stringBuilder;
        try {
            int mainCardIndex = mChipCommon.getUserSwitchDualCardSlots();
            if (HwFullNetworkConfig.IS_DUAL_4G_SUPPORTED && HwFullNetworkConstants.SIM_NUM > 1) {
                ContentResolver contentResolver = mChipCommon.mContext.getContentResolver();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("preferred_network_mode");
                stringBuilder2.append(phoneId);
                Global.putInt(contentResolver, stringBuilder2.toString(), prefMode);
                if (phoneId == mainCardIndex) {
                    Global.putInt(mChipCommon.mContext.getContentResolver(), "preferred_network_mode", prefMode);
                }
            } else if (phoneId == mainCardIndex || 1 == HwFullNetworkConstants.SIM_NUM) {
                Global.putInt(mChipCommon.mContext.getContentResolver(), "preferred_network_mode", prefMode);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("setNetworkTypeToDB id = ");
            stringBuilder.append(phoneId);
            stringBuilder.append(", mode = ");
            stringBuilder.append(prefMode);
            stringBuilder.append(" to database success!");
            logd(stringBuilder.toString());
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setNetworkTypeToDB Exception = ");
            stringBuilder.append(e);
            stringBuilder.append(",phoneId:");
            stringBuilder.append(phoneId);
            stringBuilder.append(",prefMode:");
            stringBuilder.append(prefMode);
            loge(stringBuilder.toString());
        }
    }

    public boolean isSet4GDoneAfterSimInsert() {
        return this.mAutoSwitchDualCardsSlotDone;
    }

    public boolean isSettingDefaultData() {
        return this.needSetDataAllowCount > 0;
    }

    public int getSpecCardType(int slotId) {
        if (slotId < 0 || slotId >= HwFullNetworkConstants.SIM_NUM) {
            return -1;
        }
        return this.mCardTypes[slotId];
    }

    public void saveIccidsWhenAllCardsReady() {
        logd("saveIccidsWhenAllCardsReady");
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            String iccIdToSave = this.mFullIccIds[i];
            if ((iccIdToSave != null && !"".equals(iccIdToSave)) || HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE || HwFullNetworkConfig.IS_CT_4GSWITCH_DISABLE) {
                try {
                    iccIdToSave = HwAESCryptoUtil.encrypt(HwFullNetworkConstants.MASTER_PASSWORD, iccIdToSave);
                } catch (Exception ex) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("HwAESCryptoUtil decrypt excepiton:");
                    stringBuilder.append(ex.getMessage());
                    logd(stringBuilder.toString());
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("4G_AUTO_SWITCH_ICCID_SLOT");
                stringBuilder2.append(i);
                editor.putString(stringBuilder2.toString(), iccIdToSave);
                editor.apply();
            }
        }
    }

    public void refreshCardState() {
        for (int index = 0; index < HwFullNetworkConstants.SIM_NUM; index++) {
            mChipCommon.isSimInsertedArray[index] = mChipCommon.isCardPresent(index);
        }
    }

    public void setCommrilRestartRild(boolean bCommrilRestartRild) {
        if (this.mCommrilRestartRild != bCommrilRestartRild) {
            this.mCommrilRestartRild = bCommrilRestartRild;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setCommrilRestartRild = ");
            stringBuilder.append(bCommrilRestartRild);
            logd(stringBuilder.toString());
        }
    }

    public boolean isRestartRildProgress() {
        return this.mNvRestartRildDone;
    }

    public void resetUiccSubscriptionResultFlag(int slotId) {
    }

    public int getBalongSimSlot() {
        return this.mBalongSimSlot;
    }

    public String getFullIccid(int subId) {
        if (mChipCommon.isValidIndex(subId)) {
            return this.mFullIccIds[subId];
        }
        return null;
    }

    private void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
