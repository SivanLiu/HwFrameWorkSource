package com.android.server.autofill.ui;

import android.os.IBinder;
import android.util.DebugUtils;
import android.view.autofill.IAutoFillManagerClient;

public final class PendingUi {
    public static final int STATE_CREATED = 1;
    public static final int STATE_FINISHED = 4;
    public static final int STATE_PENDING = 2;
    public final IAutoFillManagerClient client;
    public final int id;
    private int mState = 1;
    private final IBinder mToken;

    public PendingUi(IBinder token, int id, IAutoFillManagerClient client) {
        this.mToken = token;
        this.id = id;
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
        return "PendingUi: [token=" + this.mToken + ", id=" + this.id + ", state=" + DebugUtils.flagsToString(PendingUi.class, "STATE_", this.mState) + "]";
    }
}
