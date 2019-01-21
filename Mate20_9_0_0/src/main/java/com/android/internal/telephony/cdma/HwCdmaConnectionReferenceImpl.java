package com.android.internal.telephony.cdma;

import android.os.SystemClock;
import android.telephony.Rlog;
import com.android.internal.telephony.AbstractGsmCdmaConnection.HwCdmaConnectionReference;
import com.android.internal.telephony.ConnectionUtils;
import com.android.internal.telephony.GsmCdmaConnection;

public class HwCdmaConnectionReferenceImpl implements HwCdmaConnectionReference {
    private static final String LOG_TAG = "HwCdmaConnectionReferenceImpl";
    boolean hasRevFWIM = false;
    boolean isEncryptCall = false;
    private GsmCdmaConnection mCdmaConnection;

    public HwCdmaConnectionReferenceImpl(GsmCdmaConnection connection) {
        this.mCdmaConnection = connection;
    }

    public void onLineControlInfo() {
        ConnectionUtils.setConnectTime(this.mCdmaConnection, System.currentTimeMillis());
        ConnectionUtils.setConnectTimeReal(this.mCdmaConnection, SystemClock.elapsedRealtime());
        ConnectionUtils.setDuration(this.mCdmaConnection, 0);
        this.hasRevFWIM = true;
        Rlog.d(LOG_TAG, "Reset call duration");
    }

    public boolean hasRevFWIM() {
        return this.hasRevFWIM;
    }

    public boolean isEncryptCall() {
        return this.isEncryptCall;
    }

    public void setEncryptCall(boolean isEncryptCall) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" isMT:");
        stringBuilder.append(this.mCdmaConnection.isIncoming());
        stringBuilder.append(" be set EncryptCall!!!");
        Rlog.d(str, stringBuilder.toString());
        this.isEncryptCall = isEncryptCall;
    }

    public boolean compareToNumber(String number) {
        return number != null && number.equals(getRemoteNumber());
    }

    private String getRemoteNumber() {
        if (this.mCdmaConnection.isIncoming()) {
            return this.mCdmaConnection.getAddress();
        }
        return this.mCdmaConnection.getOrigDialString();
    }
}
