package com.android.internal.telephony.fullnetwork;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.fullnetwork.HwFullNetworkChipCommon.HwFullNetworkChipInterface;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants.SubType;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class HwFullNetworkChipQcom implements HwFullNetworkChipInterface {
    private static final String LOG_TAG = "HwFullNetworkChipQcom";
    private static final String MAIN_CARD_INDEX = "main_card_id";
    private static final String NETWORK_MODE_2G_ONLY = "network_mode_2G_only";
    private static final String NETWORK_MODE_3G_PRE = "network_mode_3G_pre";
    private static final String NETWORK_MODE_4G_PRE = "network_mode_4G_pre";
    static final String PROP_MAIN_STACK = "persist.radio.msim.stackid_0";
    private static HwFullNetworkChipCommon mChipCommon;
    private static HwFullNetworkChipQcom mInstance;
    private static final Object mLock = new Object();
    Map<Integer, SubType> currentSubTypeMap = new HashMap();
    int is4GSlotReviewNeeded = 0;
    private Context mContext;
    int[] mModemPreferMode = new int[HwFullNetworkConstants.SIM_NUM];
    boolean mNeedSetAllowData = false;
    boolean mNeedSetLteServiceAbility = false;
    int mNumOfGetPrefNwModeSuccess = 0;
    int mPrimaryStackNetworkType = -1;
    int mPrimaryStackPhoneId = -1;
    int mSecondaryStackNetworkType = -1;
    int mSecondaryStackPhoneId = -1;
    int mSetPrimaryStackPrefMode = -1;
    int mSetSecondaryStackPrefMode = -1;
    int[] mSetUiccSubscriptionResult = new int[HwFullNetworkConstants.SIM_NUM];
    int mUserPref4GSlot = 0;
    int[] nwModeArray = new int[HwFullNetworkConstants.SIM_NUM];
    boolean updateUserDefaultFlag = false;

    private HwFullNetworkChipQcom(Context context, CommandsInterface[] ci) {
        this.mContext = context;
        logd("HwFullNetworkChipQcom constructor");
    }

    static HwFullNetworkChipQcom make(Context context, CommandsInterface[] ci) {
        HwFullNetworkChipQcom hwFullNetworkChipQcom;
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new HwFullNetworkChipQcom(context, ci);
                mChipCommon = HwFullNetworkChipCommon.getInstance();
                mChipCommon.setChipInterface(mInstance);
                hwFullNetworkChipQcom = mInstance;
            } else {
                throw new RuntimeException("HwFullNetworkChipQcom.make() should only be called once");
            }
        }
        return hwFullNetworkChipQcom;
    }

    static HwFullNetworkChipQcom getInstance() {
        HwFullNetworkChipQcom hwFullNetworkChipQcom;
        synchronized (mLock) {
            if (mInstance != null) {
                hwFullNetworkChipQcom = mInstance;
            } else {
                throw new RuntimeException("HwFullNetworkChipQcom.getInstance can't be called before make()");
            }
        }
        return hwFullNetworkChipQcom;
    }

    public int getMainCardSubByPriority(SubType targetType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("in getMainCardSubByPriority, input targetType = ");
        stringBuilder.append(targetType);
        logd(stringBuilder.toString());
        int count = 0;
        for (Entry<Integer, SubType> mapEntry : this.currentSubTypeMap.entrySet()) {
            if (mapEntry.getValue() == targetType) {
                count++;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("count = ");
        stringBuilder2.append(count);
        logd(stringBuilder2.toString());
        if (1 == count) {
            return getKeyFromMap(this.currentSubTypeMap, targetType).intValue();
        }
        if (1 < count) {
            logd("The priority is the same between two slots: return SAME_PRIORITY");
            return 10;
        } else if (targetType.ordinal() < SubType.ERROR.ordinal()) {
            return getMainCardSubByPriority(SubType.values()[targetType.ordinal() + 1]);
        } else {
            return -1;
        }
    }

    private Integer getKeyFromMap(Map<Integer, SubType> map, SubType type) {
        for (Entry<Integer, SubType> mapEntry : map.entrySet()) {
            if (mapEntry.getValue() == type) {
                return (Integer) mapEntry.getKey();
            }
        }
        return Integer.valueOf(-1);
    }

    public int judgeDefault4GSlotForSingleSim(int defaultMainSlot) {
        int i = 0;
        while (HwFullNetworkConstants.SIM_NUM > i && !mChipCommon.isSimInsertedArray[i]) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isSimInsertedArray[");
            stringBuilder.append(i);
            stringBuilder.append("] = ");
            stringBuilder.append(mChipCommon.isSimInsertedArray[i]);
            logd(stringBuilder.toString());
            i++;
        }
        if (HwFullNetworkConstants.SIM_NUM == i) {
            logd("there is no sim card inserted, error happen!!");
            return defaultMainSlot;
        }
        logd("there is only one card inserted, set it as the 4G slot");
        return i;
    }

    public boolean isSetDefault4GSlotNeeded(int lteSlotId) {
        boolean z = true;
        if (lteSlotId != mChipCommon.getUserSwitchDualCardSlots()) {
            logd("lte slot is not the same, return true");
            return true;
        } else if (HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK) {
            return false;
        } else {
            if (-1 == getExpectedMaxCapabilitySubId(lteSlotId)) {
                z = false;
            }
            boolean isSetDefault4GSlot = z;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isSetDefault4GSlotNeeded:");
            stringBuilder.append(isSetDefault4GSlot);
            logd(stringBuilder.toString());
            return isSetDefault4GSlot;
        }
    }

    public int getExpectedMaxCapabilitySubId(int ddsSubId) {
        int expectedMaxCapSubId = -1;
        int cdmaCardNums = 0;
        int cdmaSubId = -1;
        int i = 0;
        int CurrentMaxCapabilitySubId = SystemProperties.getInt(PROP_MAIN_STACK, 0);
        ProxyController.getInstance().syncRadioCapability(CurrentMaxCapabilitySubId);
        while (i < HwFullNetworkConstants.SIM_NUM) {
            if (mChipCommon.subCarrierTypeArray[i].isCCard()) {
                cdmaSubId = i;
                cdmaCardNums++;
            }
            i++;
        }
        if (1 == cdmaCardNums && CurrentMaxCapabilitySubId != cdmaSubId) {
            expectedMaxCapSubId = cdmaSubId;
        } else if (2 == cdmaCardNums && CurrentMaxCapabilitySubId != ddsSubId) {
            expectedMaxCapSubId = ddsSubId;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[getExpectedMaxCapabilitySubId] cdmaCardNums=");
        stringBuilder.append(cdmaCardNums);
        stringBuilder.append(" expectedMaxCapSubId=");
        stringBuilder.append(expectedMaxCapSubId);
        stringBuilder.append(" CurrentMaxCapabilitySubId=");
        stringBuilder.append(CurrentMaxCapabilitySubId);
        logd(stringBuilder.toString());
        return expectedMaxCapSubId;
    }

    void judgeNwMode(int lteSlotId) {
        int nwMode4GforCU;
        int nwMode4GforCMCC;
        int nwMode4GforCT;
        int otherNWModeInCMCC;
        boolean is4GAbilityOn = 1 == HwTelephonyManagerInner.getDefault().getLteServiceAbility();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("judgeNwMode: the LTE slot will be ");
        stringBuilder.append(lteSlotId);
        stringBuilder.append(" with the is4GAbilityOn = ");
        stringBuilder.append(is4GAbilityOn);
        logd(stringBuilder.toString());
        if (is4GAbilityOn) {
            nwMode4GforCU = 9;
            if (HwFullNetworkConfig.DEFAULT_NETWORK_MODE == 17) {
                nwMode4GforCMCC = 17;
            } else {
                nwMode4GforCMCC = 20;
            }
            nwMode4GforCT = 10;
            otherNWModeInCMCC = 9;
        } else {
            nwMode4GforCU = 3;
            if (HwFullNetworkConfig.DEFAULT_NETWORK_MODE == 17) {
                nwMode4GforCMCC = 16;
            } else {
                nwMode4GforCMCC = 18;
            }
            nwMode4GforCT = 7;
            otherNWModeInCMCC = 3;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("judgeNwMode: subCarrierTypeArray[");
        stringBuilder.append(lteSlotId);
        stringBuilder.append("] = ");
        stringBuilder.append(mChipCommon.subCarrierTypeArray[lteSlotId]);
        logd(stringBuilder.toString());
        if (true == HwFullNetworkConfig.IS_FULL_NETWORK_SUPPORTED) {
            judgeNwModeForFullNetwork(lteSlotId, nwMode4GforCU, nwMode4GforCMCC, nwMode4GforCT);
        } else if (true == HwFullNetworkConfig.IS_CMCC_CU_DSDX_ENABLE) {
            judgeNwModeForCMCC_CU(lteSlotId, nwMode4GforCMCC, nwMode4GforCU);
            judgePreNwModeSubIdAndListForCU(lteSlotId);
        } else if (true == HwFullNetworkConfig.IS_CMCC_4G_DSDX_ENABLE) {
            judgeNwModeForCMCC(lteSlotId, nwMode4GforCMCC, otherNWModeInCMCC);
            judgePreNwModeSubIdAndListForCMCC(lteSlotId);
        } else if (true == HwFullNetworkConfig.IS_CHINA_TELECOM) {
            judgeNwModeForCT(lteSlotId, nwMode4GforCT, nwMode4GforCU);
        } else {
            logd("judgeNwMode: do nothing.");
        }
    }

    private void judgeNwModeForCT(int lteSlotId, int CT_DefaultMode, int UMTS_DefaultMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("judgeNwModeForCT, lteSlotId = ");
        stringBuilder.append(lteSlotId);
        logd(stringBuilder.toString());
        this.nwModeArray[lteSlotId] = CT_DefaultMode;
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            if (i != lteSlotId) {
                this.nwModeArray[i] = 1;
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("judgeNwModeForCT prefer sub network mode is ");
        stringBuilder.append(this.nwModeArray[lteSlotId]);
        logd(stringBuilder.toString());
    }

    private void judgeNwModeForCMCC_CU(int lteSlotId, int TD_DefaultMode, int UMTS_DefaultMode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("judgeNwModeForCMCC_CU, lteSlotId = ");
        stringBuilder.append(lteSlotId);
        logd(stringBuilder.toString());
        if (mChipCommon.subCarrierTypeArray[lteSlotId].isCMCCCard()) {
            this.nwModeArray[lteSlotId] = TD_DefaultMode;
        } else {
            this.nwModeArray[lteSlotId] = UMTS_DefaultMode;
        }
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            if (i != lteSlotId) {
                this.nwModeArray[i] = 1;
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("judgeNwModeForCMCC_CU prefer sub network mode is ");
        stringBuilder.append(this.nwModeArray[lteSlotId]);
        logd(stringBuilder.toString());
    }

    private void putPreNWModeListToDBforCMCC() {
        logd("in putPreNWModeListToDBforCMCC");
        ContentResolver resolver = this.mContext.getContentResolver();
        int i = HwFullNetworkConfig.DEFAULT_NETWORK_MODE;
        if (i == 17) {
            System.putInt(resolver, NETWORK_MODE_4G_PRE, 17);
            System.putInt(resolver, NETWORK_MODE_3G_PRE, 16);
            System.putInt(resolver, NETWORK_MODE_2G_ONLY, 1);
        } else if (i != 20) {
            System.putInt(resolver, NETWORK_MODE_4G_PRE, -1);
            System.putInt(resolver, NETWORK_MODE_3G_PRE, -1);
            System.putInt(resolver, NETWORK_MODE_2G_ONLY, -1);
        } else {
            System.putInt(resolver, NETWORK_MODE_4G_PRE, 20);
            System.putInt(resolver, NETWORK_MODE_3G_PRE, 18);
            System.putInt(resolver, NETWORK_MODE_2G_ONLY, 1);
        }
    }

    public void judgePreNwModeSubIdAndListForCU(int lteSlotId) {
        logd("in judgePreNwModeSubIdAndListForCU ");
        ContentResolver resolver = this.mContext.getContentResolver();
        if (mChipCommon.subCarrierTypeArray[lteSlotId].isCCard()) {
            System.putInt(resolver, MAIN_CARD_INDEX, -1);
            System.putInt(resolver, NETWORK_MODE_4G_PRE, -1);
            System.putInt(resolver, NETWORK_MODE_3G_PRE, -1);
            System.putInt(resolver, NETWORK_MODE_2G_ONLY, -1);
        } else {
            System.putInt(resolver, MAIN_CARD_INDEX, lteSlotId);
            if (mChipCommon.subCarrierTypeArray[lteSlotId].isCMCCCard()) {
                putPreNWModeListToDBforCMCC();
            } else {
                System.putInt(resolver, NETWORK_MODE_4G_PRE, 9);
                System.putInt(resolver, NETWORK_MODE_3G_PRE, 3);
                System.putInt(resolver, NETWORK_MODE_2G_ONLY, 1);
            }
        }
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("main card index: ");
            stringBuilder.append(System.getInt(resolver, MAIN_CARD_INDEX));
            stringBuilder.append(" network mode 4G pre: ");
            stringBuilder.append(System.getInt(resolver, NETWORK_MODE_4G_PRE));
            stringBuilder.append(" network mode 3G pre: ");
            stringBuilder.append(System.getInt(resolver, NETWORK_MODE_3G_PRE));
            stringBuilder.append(" network mode 2G only: ");
            stringBuilder.append(System.getInt(resolver, NETWORK_MODE_2G_ONLY));
            logd(stringBuilder.toString());
        } catch (SettingNotFoundException e) {
            loge("Settings Exception Reading PreNwMode SubId AndList Values");
        }
    }

    public void judgeNwModeForCMCC(int lteSlotId, int nwMode4GforCMCC, int otherNWModeInCMCC) {
        if (mChipCommon.subCarrierTypeArray[lteSlotId].isCUCard() || mChipCommon.subCarrierTypeArray[lteSlotId].isCCard()) {
            this.nwModeArray[lteSlotId] = otherNWModeInCMCC;
        } else {
            this.nwModeArray[lteSlotId] = nwMode4GforCMCC;
        }
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            if (i != lteSlotId) {
                this.nwModeArray[i] = 1;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("judgeNwModeForCMCC, slotId = ");
            stringBuilder.append(i);
            stringBuilder.append("nwModeArray[i] = ");
            stringBuilder.append(this.nwModeArray[i]);
            logd(stringBuilder.toString());
        }
    }

    public void judgePreNwModeSubIdAndListForCMCC(int lteSlotId) {
        logd("in judgePreNwModeSubIdAndListForCMCC ");
        ContentResolver resolver = this.mContext.getContentResolver();
        if (mChipCommon.subCarrierTypeArray[lteSlotId].isCUCard() || mChipCommon.subCarrierTypeArray[lteSlotId].isCCard()) {
            System.putInt(resolver, MAIN_CARD_INDEX, -1);
            System.putInt(resolver, NETWORK_MODE_4G_PRE, -1);
            System.putInt(resolver, NETWORK_MODE_3G_PRE, -1);
            System.putInt(resolver, NETWORK_MODE_2G_ONLY, -1);
        } else {
            System.putInt(resolver, MAIN_CARD_INDEX, lteSlotId);
            putPreNWModeListToDBforCMCC();
        }
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("main card index: ");
            stringBuilder.append(System.getInt(resolver, MAIN_CARD_INDEX));
            stringBuilder.append(" network mode 4G pre: ");
            stringBuilder.append(System.getInt(resolver, NETWORK_MODE_4G_PRE));
            stringBuilder.append(" network mode 3G pre: ");
            stringBuilder.append(System.getInt(resolver, NETWORK_MODE_3G_PRE));
            stringBuilder.append(" network mode 2G only: ");
            stringBuilder.append(System.getInt(resolver, NETWORK_MODE_2G_ONLY));
            logd(stringBuilder.toString());
        } catch (SettingNotFoundException e) {
            loge("Settings Exception Reading PreNwMode SubId AndList Values");
        }
    }

    private void judgeNwModeForFullNetwork(int lteSlotId, int nwMode4GforCU, int nwMode4GforCMCC, int nwMode4GforCT) {
        logd("judgeNwModeForFullNetwork start");
        if (mChipCommon.subCarrierTypeArray[lteSlotId].isCCard()) {
            this.nwModeArray[lteSlotId] = nwMode4GforCT;
        } else if (mChipCommon.subCarrierTypeArray[lteSlotId].isCMCCCard()) {
            this.nwModeArray[lteSlotId] = nwMode4GforCMCC;
        } else {
            this.nwModeArray[lteSlotId] = nwMode4GforCU;
        }
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            if (i != lteSlotId) {
                if (!HwFullNetworkConfig.IS_CARD2_CDMA_SUPPORTED) {
                    this.nwModeArray[i] = 1;
                } else if (!mChipCommon.subCarrierTypeArray[i].isCCard() || mChipCommon.subCarrierTypeArray[lteSlotId].isCCard()) {
                    this.nwModeArray[i] = 3;
                } else {
                    this.nwModeArray[i] = 7;
                }
            }
        }
    }

    public void refreshCardState() {
        for (int index = 0; index < HwFullNetworkConstants.SIM_NUM; index++) {
            boolean z = true;
            boolean isSubActivated = SubscriptionController.getInstance().getSubState(index) == 1;
            boolean[] zArr = mChipCommon.isSimInsertedArray;
            if (!mChipCommon.isCardPresent(index) || !isSubActivated) {
                z = false;
            }
            zArr[index] = z;
        }
    }

    int getNetworkTypeFromDB(int phoneId) {
        try {
            return TelephonyManager.getIntAtIndex(mChipCommon.mContext.getContentResolver(), "preferred_network_mode", phoneId);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getNetworkTypeFromDB Exception = ");
            stringBuilder.append(e);
            stringBuilder.append(",phoneId");
            loge(stringBuilder.toString());
            return -1;
        }
    }

    void setNetworkTypeToDB(int phoneId, int prefMode) {
        try {
            TelephonyManager.putIntAtIndex(mChipCommon.mContext.getContentResolver(), "preferred_network_mode", phoneId, prefMode);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNetworkTypeToDB id = ");
            stringBuilder.append(phoneId);
            stringBuilder.append(", mode = ");
            stringBuilder.append(prefMode);
            stringBuilder.append(" to database success!");
            logd(stringBuilder.toString());
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setNetworkTypeToDB Exception = ");
            stringBuilder2.append(e);
            stringBuilder2.append(",phoneId:");
            stringBuilder2.append(phoneId);
            stringBuilder2.append(",prefMode:");
            stringBuilder2.append(prefMode);
            loge(stringBuilder2.toString());
        }
    }

    void setLteServiceAbility() {
        HwTelephonyManagerInner mHwTelephonyManager = HwTelephonyManagerInner.getDefault();
        if (mHwTelephonyManager != null && this.mNeedSetLteServiceAbility) {
            int ability = mHwTelephonyManager.getLteServiceAbility();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setLteServiceAbility:");
            stringBuilder.append(ability);
            logd(stringBuilder.toString());
            mHwTelephonyManager.setLteServiceAbility(ability);
            this.mNeedSetLteServiceAbility = false;
        }
    }

    public boolean isSwitchDualCardSlotsEnabled() {
        UiccController mUiccController = mChipCommon.getUiccController();
        boolean z = false;
        if (mUiccController == null || mUiccController.getUiccCards() == null || mUiccController.getUiccCards().length < 2) {
            loge("haven't get all UiccCards done, please wait!");
            return false;
        }
        for (UiccCard uc : mUiccController.getUiccCards()) {
            if (uc == null) {
                loge("haven't get all UiccCards done, pls wait!");
                return false;
            }
        }
        if (!mChipCommon.isSwitchSlotEnabledForCMCC()) {
            logd("isSwitchSlotEnabledForCMCC: CMCC hybird and CMCC is not roaming return false");
            return false;
        } else if (HwFullNetworkConfig.IS_CT_4GSWITCH_DISABLE && mChipCommon.isCTHybird()) {
            logd("isSwitchSlotEnabledForCT: CMCC hybird and CMCC is not roaming return false");
            return false;
        } else if (HwFullNetworkConfig.IS_CHINA_TELECOM) {
            return false;
        } else {
            refreshCardState();
            if (mChipCommon.isSimInsertedArray[0] && mChipCommon.isSimInsertedArray[1]) {
                z = true;
            }
            return z;
        }
    }

    public void resetUiccSubscriptionResultFlag(int slotId) {
        if (slotId >= 0 && slotId < HwFullNetworkConstants.SIM_NUM) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UiccSubscriptionResult:  slotId=");
            stringBuilder.append(slotId);
            stringBuilder.append("PreResult:");
            stringBuilder.append(this.mSetUiccSubscriptionResult[slotId]);
            logd(stringBuilder.toString());
            this.mSetUiccSubscriptionResult[slotId] = -1;
        }
    }

    public void registerForIccChanged(Handler h, int what, Object obj) {
    }

    public void unregisterForIccChanged(Handler h) {
    }

    public boolean getWaitingSwitchBalongSlot() {
        return false;
    }

    public boolean isSet4GDoneAfterSimInsert() {
        return false;
    }

    public boolean isSettingDefaultData() {
        return false;
    }

    public int getBalongSimSlot() {
        return 0;
    }

    public void setWaitingSwitchBalongSlot(boolean iSetResult) {
    }

    public int getSpecCardType(int slotId) {
        return -1;
    }

    public boolean isRestartRildProgress() {
        return false;
    }

    public String getFullIccid(int subId) {
        return null;
    }

    private void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
