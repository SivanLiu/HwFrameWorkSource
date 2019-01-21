package com.android.internal.telephony;

public class SmsResponse {
    String mAckPdu;
    public int mErrorCode;
    int mMessageRef;

    public SmsResponse(int messageRef, String ackPdu, int errorCode) {
        this.mMessageRef = messageRef;
        this.mAckPdu = ackPdu;
        this.mErrorCode = errorCode;
    }

    public String toString() {
        String ret = new StringBuilder();
        ret.append("{ mMessageRef = ");
        ret.append(this.mMessageRef);
        ret.append(", mErrorCode = ");
        ret.append(this.mErrorCode);
        ret.append(", mAckPdu = ");
        ret.append(this.mAckPdu);
        ret.append("}");
        return ret.toString();
    }
}
