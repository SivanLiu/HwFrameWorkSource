package com.android.server.fsm;

import android.os.PowerManagerInternal;
import android.os.SystemClock;
import android.util.Slog;
import com.android.server.LocalServices;

public final class FingerPrintWakeupManager extends WakeupManager {
    private static final String TAG = "Fsm_FingerPrintWakeupManager";
    private boolean mFingerprintReady;
    private boolean mFoldScreenReady;
    private PowerManagerInternal mPowerManagerInternal;

    @Override // com.android.server.fsm.WakeupManager
    public void setFoldScreenReady(boolean foldScreenReady) {
        this.mFoldScreenReady = foldScreenReady;
    }

    @Override // com.android.server.fsm.WakeupManager
    public void setFingerprintReady(boolean fingerprintReady) {
        this.mFingerprintReady = fingerprintReady;
    }

    @Override // com.android.server.fsm.WakeupManager
    public void wakeup() {
        Slog.d("Fsm_FingerPrintWakeupManager", "Wakeup in FingerPrintWakeupManager");
        if (this.mFoldScreenReady && this.mFingerprintReady) {
            if (this.mPowerManagerInternal == null) {
                this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
            }
            this.mPowerManagerInternal.powerWakeup(SystemClock.uptimeMillis(), this.mReason, this.mDetails, this.mUid, this.mOpPackageName, this.mUid);
            this.mFoldScreenReady = false;
            this.mFingerprintReady = false;
        }
    }
}
