package com.android.server.locksettings;

import android.content.Context;
import android.os.Environment;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.server.locksettings.LockSettingsStorage.CredentialHash;
import java.io.File;

class HwLockSettingsStorage extends LockSettingsStorage {
    private static final String LOCK_EXTEND_PASSWORD_FILE = "gatekeeper.extendpassword.key";
    public static final String LOCK_PASSWORD_FILE2 = "password2.key";
    private static final String SYSTEM_DIRECTORY = "/system/";
    private static final String TABLE = "locksettings";
    private static final String TAG = "HwLockSettingsStorage";

    public HwLockSettingsStorage(Context context) {
        super(context);
    }

    @VisibleForTesting
    String getLockPasswordFilename(int userId) {
        return getLockCredentialFilePathForUser(userId, "password2.key");
    }

    public CredentialHash readCredentialHashEx(int userId) {
        CredentialHash passwordHash = readExPasswordHashIfExists(userId);
        if (passwordHash != null) {
            return passwordHash;
        }
        return CredentialHash.createEmptyHash();
    }

    private CredentialHash readExPasswordHashIfExists(int userId) {
        byte[] stored = readFile(getExLockPasswordFilename(userId));
        if (!ArrayUtils.isEmpty(stored)) {
            return CredentialHash.create(stored, 2);
        }
        Slog.i(TAG, "readPatternHash , cannot get any PasswordHash");
        return null;
    }

    public void writeCredentialHashEx(CredentialHash hash, int userId) {
        writeFile(getExLockPasswordFilename(userId), hash.hash);
    }

    private String getExLockPasswordFilename(int userId) {
        String dataSystemDirectory = new StringBuilder();
        dataSystemDirectory.append(Environment.getDataDirectory().getAbsolutePath());
        dataSystemDirectory.append(SYSTEM_DIRECTORY);
        dataSystemDirectory = dataSystemDirectory.toString();
        if (userId != 0) {
            return new File(Environment.getUserSystemDirectory(userId), LOCK_EXTEND_PASSWORD_FILE).getAbsolutePath();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dataSystemDirectory);
        stringBuilder.append(LOCK_EXTEND_PASSWORD_FILE);
        return stringBuilder.toString();
    }

    public void deleteExPasswordFile(int userId) {
        File file = new File(getExLockPasswordFilename(userId));
        if (file.exists() && !file.delete()) {
            Slog.e(TAG, "Error delet file ");
        }
    }

    public boolean hasSetPassword(int userId) {
        if (new File(getExLockPasswordFilename(userId)).exists()) {
            return true;
        }
        return false;
    }
}
