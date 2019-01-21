package com.android.internal.telephony;

public class LastCallFailCause {
    public int causeCode;
    public String vendorCause;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append(" causeCode: ");
        stringBuilder.append(this.causeCode);
        stringBuilder.append(" vendorCause: ");
        stringBuilder.append(this.vendorCause);
        return stringBuilder.toString();
    }
}
