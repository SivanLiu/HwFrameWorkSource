package com.android.internal.telephony.sip;

import android.os.SystemClock;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.Call.State;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.UUSInfo;

abstract class SipConnectionBase extends Connection {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SipConnBase";
    private static final boolean VDBG = false;
    private long mConnectTime;
    private long mConnectTimeReal;
    private long mCreateTime;
    private long mDisconnectTime;
    private long mDuration = -1;
    private long mHoldingStartTime;

    protected abstract Phone getPhone();

    SipConnectionBase(String dialString) {
        super(3);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SipConnectionBase: ctor dialString=");
        stringBuilder.append(SipPhone.hidePii(dialString));
        log(stringBuilder.toString());
        this.mPostDialString = PhoneNumberUtils.extractPostDialPortion(dialString);
        this.mCreateTime = System.currentTimeMillis();
    }

    protected void setState(State state) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setState: state=");
        stringBuilder.append(state);
        log(stringBuilder.toString());
        switch (state) {
            case ACTIVE:
                if (this.mConnectTime == 0) {
                    this.mConnectTimeReal = SystemClock.elapsedRealtime();
                    this.mConnectTime = System.currentTimeMillis();
                    return;
                }
                return;
            case DISCONNECTED:
                this.mDuration = getDurationMillis();
                this.mDisconnectTime = System.currentTimeMillis();
                return;
            case HOLDING:
                this.mHoldingStartTime = SystemClock.elapsedRealtime();
                return;
            default:
                return;
        }
    }

    public long getCreateTime() {
        return this.mCreateTime;
    }

    public long getConnectTime() {
        return this.mConnectTime;
    }

    public long getDisconnectTime() {
        return this.mDisconnectTime;
    }

    public long getDurationMillis() {
        if (this.mConnectTimeReal == 0) {
            return 0;
        }
        if (this.mDuration < 0) {
            return SystemClock.elapsedRealtime() - this.mConnectTimeReal;
        }
        return this.mDuration;
    }

    public long getHoldDurationMillis() {
        if (getState() != State.HOLDING) {
            return 0;
        }
        return SystemClock.elapsedRealtime() - this.mHoldingStartTime;
    }

    void setDisconnectCause(int cause) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDisconnectCause: prev=");
        stringBuilder.append(this.mCause);
        stringBuilder.append(" new=");
        stringBuilder.append(cause);
        log(stringBuilder.toString());
        this.mCause = cause;
    }

    public String getVendorDisconnectCause() {
        return null;
    }

    public void proceedAfterWaitChar() {
        log("proceedAfterWaitChar: ignore");
    }

    public void proceedAfterWildChar(String str) {
        log("proceedAfterWildChar: ignore");
    }

    public void cancelPostDial() {
        log("cancelPostDial: ignore");
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    public int getNumberPresentation() {
        return 1;
    }

    public UUSInfo getUUSInfo() {
        return null;
    }

    public int getPreciseDisconnectCause() {
        return 0;
    }

    public long getHoldingStartTime() {
        return this.mHoldingStartTime;
    }

    public long getConnectTimeReal() {
        return this.mConnectTimeReal;
    }

    public Connection getOrigConnection() {
        return null;
    }

    public boolean isMultiparty() {
        return false;
    }
}
