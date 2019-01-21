package com.android.server.backup;

import android.accounts.AccountManagerInternal;
import android.app.backup.BlobBackupHelper;
import android.util.Slog;
import com.android.server.LocalServices;

public class AccountManagerBackupHelper extends BlobBackupHelper {
    private static final boolean DEBUG = false;
    private static final String KEY_ACCOUNT_ACCESS_GRANTS = "account_access_grants";
    private static final int STATE_VERSION = 1;
    private static final String TAG = "AccountsBackup";

    public AccountManagerBackupHelper() {
        super(1, new String[]{KEY_ACCOUNT_ACCESS_GRANTS});
    }

    protected byte[] getBackupPayload(String key) {
        AccountManagerInternal am = (AccountManagerInternal) LocalServices.getService(AccountManagerInternal.class);
        int i = -1;
        try {
            if (key.hashCode() == 1544100736) {
                if (key.equals(KEY_ACCOUNT_ACCESS_GRANTS)) {
                    i = 0;
                }
            }
            if (i == 0) {
                return am.backupAccountAccessPermissions(0);
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected backup key ");
            stringBuilder.append(key);
            Slog.w(str, stringBuilder.toString());
            return new byte[0];
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to store payload ");
            stringBuilder2.append(key);
            Slog.e(str2, stringBuilder2.toString());
        }
    }

    protected void applyRestoredPayload(String key, byte[] payload) {
        AccountManagerInternal am = (AccountManagerInternal) LocalServices.getService(AccountManagerInternal.class);
        int i = -1;
        try {
            if (key.hashCode() == 1544100736) {
                if (key.equals(KEY_ACCOUNT_ACCESS_GRANTS)) {
                    i = 0;
                }
            }
            if (i != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected restore key ");
                stringBuilder.append(key);
                Slog.w(str, stringBuilder.toString());
                return;
            }
            am.restoreAccountAccessPermissions(payload, 0);
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to restore key ");
            stringBuilder2.append(key);
            Slog.w(str2, stringBuilder2.toString());
        }
    }
}
