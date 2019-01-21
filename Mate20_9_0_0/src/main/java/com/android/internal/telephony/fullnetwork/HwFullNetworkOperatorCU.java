package com.android.internal.telephony.fullnetwork;

import android.telephony.Rlog;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants.SubType;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.UiccCard;

public class HwFullNetworkOperatorCU implements HwFullNetworkOperatorBase {
    private static final String LOG_TAG = "HwFullNetworkOperatorCU";
    private boolean isMainSlotFound;
    public HwFullNetworkChipCommon mChipCommon;
    public HwFullNetworkChipHisi mChipHisi;
    public HwFullNetworkChipQcom mChipQcom;

    public HwFullNetworkOperatorCU() {
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

    public int getDefaultMainSlotForQcom() {
        int i;
        logd("judgeDefault4GSlotForCU enter");
        this.isMainSlotFound = false;
        int default4GSlot = 0;
        int noSimCount = 0;
        int curSimCount = 0;
        for (i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            if (true == this.mChipCommon.isSimInsertedArray[i]) {
                curSimCount++;
                if (!this.isMainSlotFound) {
                    default4GSlot = i;
                }
                this.isMainSlotFound = true;
            } else {
                noSimCount++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("curSimCount =");
        stringBuilder.append(curSimCount);
        stringBuilder.append(", noSimCount = ");
        stringBuilder.append(noSimCount);
        logd(stringBuilder.toString());
        if (HwFullNetworkConstants.SIM_NUM != curSimCount + noSimCount || curSimCount == 0) {
            loge("cards status error or all cards absent.");
            this.isMainSlotFound = false;
            return 0;
        } else if (1 == curSimCount) {
            return default4GSlot;
        } else {
            initSubTypes();
            i = this.mChipQcom.getMainCardSubByPriority(SubType.CARRIER_PREFERRED);
            logd(this.mChipQcom.currentSubTypeMap.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("4G slot sub is ");
            stringBuilder2.append(i);
            logd(stringBuilder2.toString());
            if (i != 10) {
                switch (i) {
                    case 0:
                    case 1:
                        return i;
                    default:
                        this.isMainSlotFound = false;
                        return i;
                }
            }
            logd("The two cards inserted have the same priority!");
            return this.mChipCommon.getUserSwitchDualCardSlots();
        }
    }

    private void initSubTypes() {
        logd("in initSubTypes.");
        for (int index = 0; index < HwFullNetworkConstants.SIM_NUM; index++) {
            this.mChipQcom.currentSubTypeMap.put(Integer.valueOf(index), getSubTypeBySub(index));
        }
    }

    public SubType getSubTypeBySub(int sub) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("in getSubTypeBySub, sub = ");
        stringBuilder.append(sub);
        logd(stringBuilder.toString());
        SubType subType = SubType.ERROR;
        StringBuilder stringBuilder2;
        if (sub < 0 || sub >= HwFullNetworkConstants.SIM_NUM || !this.mChipCommon.isSimInsertedArray[sub]) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error, sub = ");
            stringBuilder2.append(sub);
            loge(stringBuilder2.toString());
            return SubType.ERROR;
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("subCarrierTypeArray[sub] = ");
        stringBuilder2.append(this.mChipCommon.subCarrierTypeArray[sub]);
        logd(stringBuilder2.toString());
        switch (this.mChipCommon.subCarrierTypeArray[sub]) {
            case CARRIER_CMCC_USIM:
                subType = SubType.CARRIER_PREFERRED;
                break;
            case CARRIER_CMCC_SIM:
                subType = SubType.CARRIER;
                break;
            case CARRIER_CU_USIM:
                subType = SubType.CARRIER_PREFERRED;
                break;
            case CARRIER_CU_SIM:
                subType = SubType.CARRIER;
                break;
            case CARRIER_FOREIGN_USIM:
            case CARRIER_FOREIGN_CSIM:
                subType = SubType.FOREIGN_CARRIER_PREFERRED;
                break;
            default:
                subType = SubType.FOREIGN_CARRIER;
                break;
        }
        return subType;
    }

    public boolean isMainSlotFound() {
        return this.isMainSlotFound;
    }

    public void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    public void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
