package com.android.internal.telephony.cdma;

public class HwCustMccSidLtmOff {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "HwCustMccSidLtmOff";
    public static final int LTM_OFF_INVALID = 100;
    public int LtmOffMax;
    public int LtmOffMin;
    public int Mcc;
    public int Sid;

    public HwCustMccSidLtmOff() {
        this.Mcc = -1;
        this.Sid = -1;
        this.LtmOffMin = 100;
        this.LtmOffMax = 100;
    }

    public HwCustMccSidLtmOff(int mcc, int sid, int ltmOffMin, int ltmOffMax) {
        this.Mcc = mcc;
        this.Sid = sid;
        this.LtmOffMin = ltmOffMin;
        this.LtmOffMax = ltmOffMax;
    }

    public HwCustMccSidLtmOff(HwCustMccSidLtmOff t) {
        copyFrom(t);
    }

    protected void copyFrom(HwCustMccSidLtmOff t) {
        this.Mcc = t.Mcc;
        this.Sid = t.Sid;
        this.LtmOffMin = t.LtmOffMin;
        this.LtmOffMax = t.LtmOffMax;
    }

    public int getMcc() {
        return this.Mcc;
    }

    public int getSid() {
        return this.Sid;
    }

    public int getLtmOffMin() {
        return this.LtmOffMin;
    }

    public int getLtmOffMax() {
        return this.LtmOffMax;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Mcc =");
        stringBuilder.append(this.Mcc);
        stringBuilder.append(", Sid = ");
        stringBuilder.append(this.Sid);
        stringBuilder.append(", LtmOffMin = ");
        stringBuilder.append(this.LtmOffMin);
        stringBuilder.append(", LtmOffMax = ");
        stringBuilder.append(this.LtmOffMax);
        return stringBuilder.toString();
    }
}
