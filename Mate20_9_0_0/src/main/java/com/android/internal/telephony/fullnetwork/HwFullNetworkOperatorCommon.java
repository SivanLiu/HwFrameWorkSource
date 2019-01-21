package com.android.internal.telephony.fullnetwork;

import android.telephony.Rlog;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.UiccCard;

public class HwFullNetworkOperatorCommon implements HwFullNetworkOperatorBase {
    private static final String LOG_TAG = "HwFullNetworkOperatorCommon";
    private boolean isMainSlotFound;
    public HwFullNetworkChipCommon mChipCommon;
    public HwFullNetworkChipHisi mChipHisi;
    public HwFullNetworkChipQcom mChipQcom;

    public HwFullNetworkOperatorCommon() {
        this.mChipCommon = null;
        this.mChipHisi = null;
        this.mChipQcom = null;
        this.isMainSlotFound = false;
        this.mChipCommon = HwFullNetworkChipCommon.getInstance();
    }

    public int getDefaultMainSlot(boolean forceSwitch) {
        if (HwFullNetworkConfig.isHisiPlatform()) {
            this.mChipHisi = HwFullNetworkChipHisi.getInstance();
            return getDefaultMainSlotForHisi(forceSwitch);
        }
        this.mChipQcom = HwFullNetworkChipQcom.getInstance();
        return getDefaultMainSlotForQcom();
    }

    public int getDefaultMainSlotForHisi(boolean forceSwitch) {
        int temSub = this.mChipCommon.getUserSwitchDualCardSlots();
        UiccCard[] mUiccCards = this.mChipCommon.getUiccController().getUiccCards();
        boolean onlyCard2Present = false;
        boolean isCard1Present = mUiccCards[0].getCardState() == CardState.CARDSTATE_PRESENT || this.mChipHisi.mSwitchTypes[0] > 0;
        boolean isCard2Present = mUiccCards[1].getCardState() == CardState.CARDSTATE_PRESENT || this.mChipHisi.mSwitchTypes[1] > 0;
        boolean onlyCard1Present = isCard1Present && !isCard2Present;
        if (!isCard1Present && isCard2Present) {
            onlyCard2Present = true;
        }
        if (onlyCard1Present) {
            temSub = 0;
        } else if (onlyCard2Present) {
            temSub = 1;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isCard1Present = ");
        stringBuilder.append(isCard1Present);
        stringBuilder.append(", isCard2Present = ");
        stringBuilder.append(isCard2Present);
        stringBuilder.append(", defaultMainSlot ");
        stringBuilder.append(temSub);
        logd(stringBuilder.toString());
        return temSub;
    }

    public void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    public void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    public int getDefaultMainSlotForQcom() {
        int i;
        logd("getDeaultMainSlotForQcom start");
        int i2 = 0;
        this.isMainSlotFound = false;
        int numOfSimPresent = 0;
        for (i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            if (true == this.mChipCommon.isSimInsertedArray[i]) {
                numOfSimPresent++;
            }
        }
        if (numOfSimPresent == 0) {
            logd("no card inserted");
            i = this.mChipCommon.getUserSwitchDualCardSlots();
            this.isMainSlotFound = false;
            return i;
        } else if (1 == numOfSimPresent) {
            i = this.mChipQcom.judgeDefault4GSlotForSingleSim(0);
            this.isMainSlotFound = true;
            return i;
        } else if (HwFullNetworkConfig.IS_CARD2_CDMA_SUPPORTED || HwFullNetworkConfig.IS_QCRIL_CROSS_MAPPING || HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK) {
            this.isMainSlotFound = true;
            return 0;
        } else {
            i = 0;
            int indexOfCCard = 0;
            while (i2 < HwFullNetworkConstants.SIM_NUM) {
                if (true == this.mChipCommon.isSimInsertedArray[i2] && this.mChipCommon.subCarrierTypeArray[i2].isCCard()) {
                    i++;
                    indexOfCCard = i2;
                }
                i2++;
            }
            if (1 == i) {
                logd("there is only one CDMA card inserted, set it as the 4G slot");
                this.isMainSlotFound = true;
                i2 = indexOfCCard;
            } else {
                logd("there are multiple CDMA cards or U cards inserted, set the SUB_0 as the lte slot");
                this.isMainSlotFound = true;
                i2 = 0;
                if (i > 1) {
                    this.mChipQcom.updateUserDefaultFlag = true;
                }
            }
            return i2;
        }
    }

    public boolean isMainSlotFound() {
        return this.isMainSlotFound;
    }
}
