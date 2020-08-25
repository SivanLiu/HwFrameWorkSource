package com.android.server.security.trustspace;

import android.util.Slog;
import com.android.server.LocalServices;

public class TrustSpaceControllerImpl implements ITrustSpaceController {
    private static final String TAG = "TrustSpaceControllerImpl";
    private TrustSpaceManagerInternal mTrustSpaceManagerInternal;

    public void initTrustSpace() {
        this.mTrustSpaceManagerInternal = (TrustSpaceManagerInternal) LocalServices.getService(TrustSpaceManagerInternal.class);
        TrustSpaceManagerInternal trustSpaceManagerInternal = this.mTrustSpaceManagerInternal;
        if (trustSpaceManagerInternal != null) {
            trustSpaceManagerInternal.initTrustSpace();
        } else {
            Slog.e(TAG, "TrustSpaceManagerInternal not find !");
        }
    }

    public boolean checkIntent(int type, String calleePackage, int callerUid, int callerPid, String callingPackage, int userId) {
        TrustSpaceManagerInternal trustSpaceManagerInternal = this.mTrustSpaceManagerInternal;
        if (trustSpaceManagerInternal != null) {
            return trustSpaceManagerInternal.checkIntent(type, calleePackage, callerUid, callerPid, callingPackage, userId);
        }
        return false;
    }

    public boolean isIntentProtectedApp(String pkg) {
        TrustSpaceManagerInternal trustSpaceManagerInternal = this.mTrustSpaceManagerInternal;
        if (trustSpaceManagerInternal != null) {
            return trustSpaceManagerInternal.isIntentProtectedApp(pkg);
        }
        return false;
    }
}
