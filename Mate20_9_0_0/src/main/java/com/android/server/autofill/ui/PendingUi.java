package com.android.server.autofill.ui;

import android.os.IBinder;
import android.util.DebugUtils;
import android.view.autofill.IAutoFillManagerClient;

public final class PendingUi {
    public static final int STATE_CREATED = 1;
    public static final int STATE_FINISHED = 4;
    public static final int STATE_PENDING = 2;
    public final IAutoFillManagerClient client;
    private int mState = 1;
    private final IBinder mToken;
    public final int sessionId;

    public PendingUi(IBinder token, int sessionId, IAutoFillManagerClient client) {
        this.mToken = token;
        this.sessionId = sessionId;
        this.client = client;
    }

    public IBinder getToken() {
        return this.mToken;
    }

    public void setState(int state) {
        this.mState = state;
    }

    public int getState() {
        return this.mState;
    }

    public boolean matches(IBinder token) {
        return this.mToken.equals(token);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PendingUi: [token=");
        stringBuilder.append(this.mToken);
        stringBuilder.append(", sessionId=");
        stringBuilder.append(this.sessionId);
        stringBuilder.append(", state=");
        stringBuilder.append(DebugUtils.flagsToString(PendingUi.class, "STATE_", this.mState));
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
