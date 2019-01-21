package com.android.internal.telephony.dataconnection;

import android.content.Intent;
import android.telephony.Rlog;

public class DcFailBringUp {
    static final String ACTION_FAIL_BRINGUP = "action_fail_bringup";
    static final String COUNTER = "counter";
    private static final boolean DBG = true;
    static final int DEFAULT_COUNTER = 2;
    static final DcFailCause DEFAULT_FAIL_CAUSE = DcFailCause.ERROR_UNSPECIFIED;
    static final int DEFAULT_SUGGESTED_RETRY_TIME = -1;
    static final String FAIL_CAUSE = "fail_cause";
    static final String INTENT_BASE = DataConnection.class.getPackage().getName();
    private static final String LOG_TAG = "DcFailBringUp";
    static final String SUGGESTED_RETRY_TIME = "suggested_retry_time";
    int mCounter;
    DcFailCause mFailCause;
    int mSuggestedRetryTime;

    void saveParameters(Intent intent, String s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(s);
        stringBuilder.append(".saveParameters: action=");
        stringBuilder.append(intent.getAction());
        log(stringBuilder.toString());
        this.mCounter = intent.getIntExtra(COUNTER, 2);
        this.mFailCause = DcFailCause.fromInt(intent.getIntExtra(FAIL_CAUSE, DEFAULT_FAIL_CAUSE.getErrorCode()));
        this.mSuggestedRetryTime = intent.getIntExtra(SUGGESTED_RETRY_TIME, -1);
        stringBuilder = new StringBuilder();
        stringBuilder.append(s);
        stringBuilder.append(".saveParameters: ");
        stringBuilder.append(this);
        log(stringBuilder.toString());
    }

    public void saveParameters(int counter, int failCause, int suggestedRetryTime) {
        this.mCounter = counter;
        this.mFailCause = DcFailCause.fromInt(failCause);
        this.mSuggestedRetryTime = suggestedRetryTime;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{mCounter=");
        stringBuilder.append(this.mCounter);
        stringBuilder.append(" mFailCause=");
        stringBuilder.append(this.mFailCause);
        stringBuilder.append(" mSuggestedRetryTime=");
        stringBuilder.append(this.mSuggestedRetryTime);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private static void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
}
