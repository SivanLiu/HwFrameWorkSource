package com.android.internal.telephony.uicc.euicc.apdu;

public class ApduException extends Exception {
    private final int mApduStatus;

    public ApduException(int apduStatus) {
        this.mApduStatus = apduStatus;
    }

    public ApduException(String message) {
        super(message);
        this.mApduStatus = 0;
    }

    public int getApduStatus() {
        return this.mApduStatus;
    }

    public String getStatusHex() {
        return Integer.toHexString(this.mApduStatus);
    }

    public String getMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.getMessage());
        stringBuilder.append(" (apduStatus=");
        stringBuilder.append(getStatusHex());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
