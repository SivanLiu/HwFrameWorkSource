package com.android.server.locksettings;

import com.android.internal.widget.ILockSettings.Stub;

public abstract class AbsLockSettingsService extends Stub {
    protected int getOldCredentialType(int userId) {
        return -1;
    }

    protected int getPasswordStatus(int currentCredentialType, int oldCredentialType) {
        return 0;
    }

    protected void notifyPasswordStatusChanged(int userId, int status) {
    }

    protected void notifyModifyPwdForPrivSpacePwdProtect(String credential, String savedCredential, int userId) {
    }

    protected void notifyBigDataForPwdProtectFail(int userId) {
    }

    public boolean setExtendLockScreenPassword(String password, String phoneNumber, int userHandle) {
        return false;
    }

    public boolean clearExtendLockScreenPassword(String password, int userHandle) {
        return false;
    }

    public void handleUserClearLockForAnti(int userId) {
    }
}
