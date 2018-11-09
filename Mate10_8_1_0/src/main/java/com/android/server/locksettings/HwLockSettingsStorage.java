package com.android.server.locksettings;

import android.content.Context;

class HwLockSettingsStorage extends LockSettingsStorage {
    public static final String LOCK_PASSWORD_FILE2 = "password2.key";
    private static final String TABLE = "locksettings";
    private static final String TAG = "HwLockSettingsStorage";

    public HwLockSettingsStorage(Context context) {
        super(context);
    }

    String getLockPasswordFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, "password2.key");
    }
}
