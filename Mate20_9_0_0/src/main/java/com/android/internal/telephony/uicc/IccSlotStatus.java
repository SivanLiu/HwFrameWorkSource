package com.android.internal.telephony.uicc;

import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;

public class IccSlotStatus {
    public String atr;
    public CardState cardState;
    public String iccid;
    public int logicalSlotIndex;
    public SlotState slotState;

    public enum SlotState {
        SLOTSTATE_INACTIVE,
        SLOTSTATE_ACTIVE
    }

    public void setCardState(int state) {
        switch (state) {
            case 0:
                this.cardState = CardState.CARDSTATE_ABSENT;
                return;
            case 1:
                this.cardState = CardState.CARDSTATE_PRESENT;
                return;
            case 2:
                this.cardState = CardState.CARDSTATE_ERROR;
                return;
            case 3:
                this.cardState = CardState.CARDSTATE_RESTRICTED;
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unrecognized RIL_CardState: ");
                stringBuilder.append(state);
                throw new RuntimeException(stringBuilder.toString());
        }
    }

    public void setSlotState(int state) {
        switch (state) {
            case 0:
                this.slotState = SlotState.SLOTSTATE_INACTIVE;
                return;
            case 1:
                this.slotState = SlotState.SLOTSTATE_ACTIVE;
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unrecognized RIL_SlotState: ");
                stringBuilder.append(state);
                throw new RuntimeException(stringBuilder.toString());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IccSlotStatus {");
        sb.append(this.cardState);
        sb.append(",");
        sb.append(this.slotState);
        sb.append(",");
        sb.append("logicalSlotIndex=");
        sb.append(this.logicalSlotIndex);
        sb.append(",");
        sb.append("atr=");
        sb.append(this.atr);
        sb.append(",iccid=");
        sb.append(SubscriptionInfo.givePrintableIccid(this.iccid));
        sb.append("}");
        return sb.toString();
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IccSlotStatus that = (IccSlotStatus) obj;
        if (!(this.cardState == that.cardState && this.slotState == that.slotState && this.logicalSlotIndex == that.logicalSlotIndex && TextUtils.equals(this.atr, that.atr) && TextUtils.equals(this.iccid, that.iccid))) {
            z = false;
        }
        return z;
    }
}
