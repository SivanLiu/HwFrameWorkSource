package com.android.internal.telephony;

public class HwQualcommCsgNetworkInfo {
    public byte bIncludePcsDigit;
    public int iCSGId;
    public int iCSGListCat;
    public int iSignalStrength;
    public boolean isSelectedFail = false;
    public short mcc;
    public short mnc;
    public String sCSGName;

    public boolean isEmpty() {
        return this.mcc == (short) 0 && this.mnc == (short) 0 && this.bIncludePcsDigit == (byte) 0 && this.iCSGListCat == 0 && this.iCSGId == 0 && ((this.sCSGName == null || this.sCSGName.isEmpty()) && this.iSignalStrength == 0);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwQualcommCsgNetworkInfo: mcc: ");
        stringBuilder.append(this.mcc);
        stringBuilder.append(", mnc: ");
        stringBuilder.append(this.mnc);
        stringBuilder.append(", bIncludePcsDigit: ");
        stringBuilder.append(this.bIncludePcsDigit);
        stringBuilder.append(", iCSGListCat: ");
        stringBuilder.append(this.iCSGListCat);
        stringBuilder.append(", iCSGId: ");
        stringBuilder.append(this.iCSGId);
        stringBuilder.append(", sCSGName: ");
        stringBuilder.append(this.sCSGName);
        stringBuilder.append(", iSignalStrength: ");
        stringBuilder.append(this.iSignalStrength);
        stringBuilder.append(" ,isSelectedFail:");
        stringBuilder.append(this.isSelectedFail);
        return stringBuilder.toString();
    }
}
