package com.android.internal.telephony.fullnetwork;

import android.telephony.Rlog;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants.SubType;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.UiccCard;

public class HwFullNetworkOperatorCT implements HwFullNetworkOperatorBase {
    private static final String LOG_TAG = "HwFullNetworkOperatorCT";
    private boolean isMainSlotFound;
    public HwFullNetworkChipCommon mChipCommon;
    public HwFullNetworkChipHisi mChipHisi;
    public HwFullNetworkChipQcom mChipQcom;

    public HwFullNetworkOperatorCT() {
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
        StringBuilder stringBuilder;
        int temSub = this.mChipCommon.getUserSwitchDualCardSlots();
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            if (this.mChipCommon.mIccIds[i] == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("mIccIds[");
                stringBuilder.append(i);
                stringBuilder.append("] is null, and return");
                logd(stringBuilder.toString());
                return temSub;
            }
        }
        UiccCard[] mUiccCards = this.mChipCommon.mUiccController.getUiccCards();
        boolean isCard1Present = mUiccCards[0].getCardState() == CardState.CARDSTATE_PRESENT || this.mChipHisi.mSwitchTypes[0] > 0;
        boolean isCard2Present = mUiccCards[1].getCardState() == CardState.CARDSTATE_PRESENT || this.mChipHisi.mSwitchTypes[1] > 0;
        boolean isAnySimCardChanged = this.mChipHisi.anySimCardChanged() || this.mChipHisi.isPreBootCompleted || forceSwitch;
        if (this.mChipHisi.isPreBootCompleted) {
            logd("judgeDefault4GSlotForCT: reset isPreBootCompleted.");
            this.mChipHisi.isPreBootCompleted = false;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("judgeDefault4GSlotForCT isAnySimCardChanged = ");
        stringBuilder2.append(isAnySimCardChanged);
        logd(stringBuilder2.toString());
        if (isCard1Present && !isCard2Present) {
            temSub = 0;
        } else if (!isCard1Present && isCard2Present) {
            temSub = 1;
        } else if (isCard1Present && isCard2Present) {
            if (isAnySimCardChanged || !HwFullNetworkConfig.IS_HISI_DSDX) {
                boolean[] isCTCards = new boolean[HwFullNetworkConstants.SIM_NUM];
                int cardtype1 = (this.mChipHisi.mCardTypes[0] & 240) >> 4;
                int cardtype2 = (this.mChipHisi.mCardTypes[1] & 240) >> 4;
                for (int i2 = 0; i2 < HwFullNetworkConstants.SIM_NUM; i2++) {
                    isCTCards[i2] = this.mChipCommon.isCTCardBySlotId(i2);
                }
                if (isCTCards[0] && isCTCards[1]) {
                    if (cardtype1 == 2 && cardtype2 == 1) {
                        temSub = this.mChipHisi.isBalongSimSynced() ^ 1;
                    } else if (cardtype1 == 1 && cardtype2 == 2) {
                        temSub = this.mChipHisi.isBalongSimSynced();
                    }
                } else if (isCTCards[0]) {
                    temSub = 0;
                } else if (isCTCards[1]) {
                    temSub = 1;
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("cardtype1 = ");
                stringBuilder3.append(cardtype1);
                stringBuilder3.append(", cardtype2 = ");
                stringBuilder3.append(cardtype2);
                stringBuilder3.append(", isCTCards[SUB1] ");
                stringBuilder3.append(isCTCards[0]);
                stringBuilder3.append(", isCTCards[SUB2] ");
                stringBuilder3.append(isCTCards[1]);
                logd(stringBuilder3.toString());
            } else {
                logd("judgeDefaultSlotId4HisiCmcc all sim present but none sim change ");
                return temSub;
            }
        }
        stringBuilder = new StringBuilder();
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
        logd("judgeDefault4GSlotForCT enter");
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
            this.mChipQcom.currentSubTypeMap.put(Integer.valueOf(index), getSubTypeBySubForCT(index));
        }
    }

    public SubType getSubTypeBySubForCT(int sub) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("in getSubTypeBySubForCT, sub = ");
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
            case CARRIER_CT_CSIM:
                subType = SubType.CARRIER_PREFERRED;
                break;
            case CARRIER_CT_RUIM:
                subType = SubType.CARRIER;
                break;
            case CARRIER_FOREIGN_CSIM:
                subType = SubType.FOREIGN_CARRIER_PREFERRED;
                break;
            case CARRIER_FOREIGN_RUIM:
                subType = SubType.FOREIGN_CARRIER;
                break;
            default:
                subType = SubType.LOCAL_CARRIER;
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
