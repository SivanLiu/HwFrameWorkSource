package com.android.server.location;

import android.hardware.location.ContextHubTransaction;
import android.hardware.location.NanoAppState;
import java.util.List;
import java.util.concurrent.TimeUnit;

abstract class ContextHubServiceTransaction {
    private boolean mIsComplete = false;
    private final int mTransactionId;
    private final int mTransactionType;

    abstract int onTransact();

    ContextHubServiceTransaction(int id, int type) {
        this.mTransactionId = id;
        this.mTransactionType = type;
    }

    void onTransactionComplete(int result) {
    }

    void onQueryResponse(int result, List<NanoAppState> list) {
    }

    int getTransactionId() {
        return this.mTransactionId;
    }

    int getTransactionType() {
        return this.mTransactionType;
    }

    long getTimeout(TimeUnit unit) {
        return this.mTransactionType != 0 ? unit.convert(5, TimeUnit.SECONDS) : unit.convert(30, TimeUnit.SECONDS);
    }

    void setComplete() {
        this.mIsComplete = true;
    }

    boolean isComplete() {
        return this.mIsComplete;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ContextHubTransaction.typeToString(this.mTransactionType, true));
        stringBuilder.append(" transaction (ID = ");
        stringBuilder.append(this.mTransactionId);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
