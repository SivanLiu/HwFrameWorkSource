package com.android.server.fsm;

public class WakeupManager {
    protected String mDetails;
    protected String mOpPackageName;
    protected int mReason;
    protected int mUid;

    public void setFoldScreenReady(boolean isFoldScreenReady) {
    }

    public void setFingerprintReady(boolean isFingerprintReady) {
    }

    public void setWakeUpInfo(int uid, String opPackageName, int reason, String details) {
        this.mUid = uid;
        this.mOpPackageName = opPackageName;
        this.mReason = reason;
        this.mDetails = details;
    }

    public void wakeup() {
    }
}
